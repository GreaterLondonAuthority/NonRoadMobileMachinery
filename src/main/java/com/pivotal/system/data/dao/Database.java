/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.data.dao;

import com.pivotal.reporting.reports.Report;
import com.pivotal.reporting.reports.sqldump.ExcelOutput;
import com.pivotal.reporting.reports.sqldump.SQLOutput;
import com.pivotal.reporting.reports.sqldump.TextOutput;
import com.pivotal.system.hibernate.entities.DatasourceEntity;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;
import com.pivotal.utils.VFSUtils;
import com.pivotal.web.Constants;
import com.pivotal.web.servlet.ServletHelper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.Selectors;
import org.springframework.util.LinkedCaseInsensitiveMap;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.sql.*;
import java.util.Date;
import java.util.*;

/**
 * This class provides a very simple DAO interface to JDBC databases
 * that are accessed from the Velocity Transformer context
 * This class is very 'procedural' in nature to make it easy to use
 * from non-NRMM experts within the Velocity environment
 */
public abstract class Database {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Database.class);
    private static final org.slf4j.Logger Perflogger = org.slf4j.LoggerFactory.getLogger("QueryPerformance");

    private static final int DEFAULT_MAXIMUM_RESULTS = 500000;
    private static final int DEFAULT_FETCH_SIZE = 30000;
    private static final String DEFAULT_DELIMITER = ",";
    /** Constant <code>HEADER_FLUSH_TIMEOUT=50</code> */
    public static final int HEADER_FLUSH_TIMEOUT = 50;

    String lastError;
    String lastWarning;
    Connection dbConnection;
    boolean useFutureConnection;
    int maximumResults = DEFAULT_MAXIMUM_RESULTS;
    boolean resultsTruncated;
    boolean resultsCached;
    long lastDuration;
    String name;
    boolean isInsideTranasaction;
    boolean autoCommitState;
    protected DatasourceEntity dataSrc;
    SQLException sqlException;

    Map<String, BatchDesc> batchDesc = new LinkedCaseInsensitiveMap<>();
    static Map<String, Set<String>> metData = new LinkedCaseInsensitiveMap<>();

    /**
     * Opens the connection and throws any exceptions
     * Should be overridden by implementations to do something
     * database type specific
     *
     * @throws java.lang.Exception - errors caused by opening
     */
    public void open() throws Exception {

        // Close the connection just in case we're open

        close();
    }

    /**
     * Closes the connection and swallows any errors
     */
    public void close() {
        try {
            logger.debug("Closing database connection");
            if (dbConnection != null && !dbConnection.isClosed())
                dbConnection.close();
        }
        catch (Throwable e) {
            logger.warn("Problem closing database - {}", PivotalException.getErrorMessage(e));
        }
        dbConnection = null;
        useFutureConnection = false;
    }

    /**
     * Returns the URL of the database connection
     *
     * @return url either from meta data or the data source
     * @throws java.sql.SQLException if any.
     */
    protected String obtainConnectionUrl() throws SQLException {
        String url;
        if (dataSrc == null) {
            getConnection();
            url = dbConnection.getMetaData().getURL();
        }
        else
            url = dataSrc.getDatabaseUrl();
        return url;
    }

    /**
     * Returns true if this connection is in error from the last operation
     *
     * @return True if an error exists
     */
    public boolean isInError() {
        return !Common.isBlank(lastError);
    }

    /**
     * Returns the error message from the most recent operation
     *
     * @return String last error
     */
    public String getLastError() {
        return lastError;
    }

    /**
     * Returns the time in milliseconds that the last query took to complete
     *
     * @return Time in milliseconds
     */
    public long getLastDuration() {
        return lastDuration;
    }

    /**
     * Adds a row for batching processing into the specified table using the map of
     * column values. The method takes care of type conversion into the native representation
     * If the Add was successful, it will return true, otherwise false and
     * the lastError will contain the error message
     * After a successful add, the supplied rowValues will be queued into the batch
     * statement. The method executeBatch() will synchronize changes with the db.
     * The method closeBatch() should be used to release batch related resources.
     *
     * There's a bit of seemingly unnecessary type conversion going on here whereby
     * the values bing passed in are likely to be in the correct type for the
     * database update - however, we can't be sure that it isn't simply passed
     * as a string so we convert the value to an unambiguous string first then
     * to the value second
     * This is all part of making sure the handling is as fault safe as possible
     *
     * @param table    Table to update
     * @param rowValues Map of column name/values
     * @return True if succeeded
     */
    public boolean addBatch(String table, Map<String, Object> rowValues) {

        // Check the parameters are OK

        if (checkParameters(table, rowValues)) {
            BatchDesc desc = null;
            try {

                // Create the BatchDesc object that handles all the operations related to batch statements.

                if (!batchDesc.containsKey(table) || batchDesc.get(table).isClosed()) {
                    desc = new BatchDesc(table, rowValues, dbConnection);
                    batchDesc.put(table, desc);
                    desc.isInTransaction = isInsideTranasaction;
                }
                else
                    desc = batchDesc.get(table);

                // Start a transaction

                if (!desc.isInTransaction) startTransactionIfNotStartedYet();
                desc.addBatch(rowValues);

            }
            catch (SQLException e) {
                setError("Problem adding database batch - " + Z_GetRowValues(rowValues) + " - " + PivotalException.getErrorMessage(e), e);
                if (desc != null && !desc.isInTransaction) rollbackTransaction();
            }
            catch (Exception e) {
                setError("Problem adding database batch - " + PivotalException.getErrorMessage(e));
                if (desc != null && !desc.isInTransaction) rollbackTransaction();
            }
        }

        if (isInError())
            logger.error("Problem adding database batch - {}", lastError);
        else
            logger.debug("Database batch added successfully to {}", table);

        return !isInError();
    }

    /**
     * Execute the current batch, inserting into the db the rows queued for the specified table.
     *
     * @param table the table that has rows queued.
     * @return True if succeeded
     */
    public boolean executeBatch(String table) {
        lastError = null;
        try {
            // Execute batch for active statements

            if (batchDesc.containsKey(table) && !batchDesc.get(table).isClosed()) {
                batchDesc.get(table).executeBatch();
                if (!batchDesc.get(table).isInTransaction) commitTransaction();
            }
            else {
                setError("There's no active batch statement associated with the table " + table);
            }

        }
        catch (SQLException e) {
            setError("Problem executing database batch - " + PivotalException.getErrorMessage(e), e);
            if (!batchDesc.get(table).isInTransaction) rollbackTransaction();
        }
        catch (Exception e) {
            setError("Problem executing database batch - " + PivotalException.getErrorMessage(e));
            if (!batchDesc.get(table).isInTransaction) rollbackTransaction();
        }

        return !isInError();
    }

    /**
     * Sets the last error
     *
     * @param error Error message
     */
    public void setError(String error) {
        setError(error, null);
    }

    /**
     * Sets the last error
     *
     * @param error        Error message
     * @param sqlException SQL exception that cause the problem
     */
    public void setError(String error, SQLException sqlException) {
        lastError = error;
        this.sqlException = sqlException;
        logger.error(lastError);
    }

    /**
     * Sets the last warning
     *
     * @param warning Error message
     */
    public void setWarning(String warning) {
        lastWarning = warning;
        logger.warn(lastWarning);
    }

    /**
     * Release the resources used for handling batch operations for the specified table.
     *
     * @param sTable the table used for batch statements
     */
    public void closeBatch(String sTable) {
        lastError = null;
        try {
            // Close active statements

            if (batchDesc.containsKey(sTable) && !batchDesc.get(sTable).isClosed()) {
                batchDesc.get(sTable).closeBatch();
                if (!batchDesc.get(sTable).isInTransaction) rollbackTransaction();
            }
            batchDesc.remove(sTable);

        }
        catch (Exception e) {
            setError("Problem closing database batch - " + PivotalException.getErrorMessage(e));
        }
    }

    /**
     * Adds a row to the specified table using the map of column values
     * The method takes care of type conversion into the native representation
     * If the Add was successful, it will return true, otherwise false and
     * the lastError will contain the error message
     * After a successful add, the supplied rowValues is updated with the newly
     * created row column values
     *
     * There's a bit of seemingly unnecessary type conversion going on here whereby
     * the values bing passed in are likely to be in the correct type for the
     * database update - however, we can't be sure that it isn't simply passed
     * as a string so we convert the value to an unambiguous string first then
     * to the value second
     * This is all part of making sure the handling is as fault safe as possible
     *
     * @param sTable    Table to update
     * @param rowValues Map of column name/values
     * @return True if succeeded
     */
    public boolean addRecord(String sTable, Map<String, Object> rowValues) {
        return addRecord(sTable, rowValues, true);
    }

    /**
     * Adds a row to the specified table using the map of column values
     * The method takes care of type conversion into the native representation
     * If the Add was successful, it will return true, otherwise false and
     * the lastError will contain the error message
     * After a successful add, the supplied rowValues is updated with the newly
     * created row column values
     *
     * There's a bit of seemingly unnecessary type conversion going on here whereby
     * the values bing passed in are likely to be in the correct type for the
     * database update - however, we can't be sure that it isn't simply passed
     * as a string so we convert the value to an unambiguous string first then
     * to the value second
     * This is all part of making sure the handling is as fault safe as possible
     *
     * @param table          Table to update
     * @param rowValues       Map of column name/values
     * @param updateRowValues True if the row values should be updated with the row
     * @return True if succeeded
     */
    public boolean addRecord(String table, Map<String, Object> rowValues, boolean updateRowValues) {

        // Check to see if we should auto commit

        boolean inTransaction = isInsideTranasaction;

        // Check the parameters are OK

        if (checkParameters(table, rowValues)) {
            if (!inTransaction) startTransactionIfNotStartedYet();
            BatchDesc batch = null;
            try {

                // Add the values to the tables

                batch = new BatchDesc(table, rowValues, dbConnection);
                batch.addBatch(rowValues);
                batch.executeBatch();
                if (!inTransaction) commitTransaction();

                // Get the ID of the record we just added

                if (updateRowValues) {
                    List keys = batch.getGeneratedKeys();
                    if (!Common.isBlank(batch.getKeyColumnName()) && !Common.isBlank(keys)) {
                        Long newKey = batch.getGeneratedKeys().get(0);

                        // Construct a select for this

                        rowValues.putAll(findFirst("select * from " + table + " where " + batch.getKeyColumnName() + '=' + newKey));
                    }
                    else
                        logger.debug("No generated keys from the recent addRecord");
                }
            }
            catch (SQLException e) {
                setError("Problem adding row to database - " + Z_GetRowValues(rowValues) + " - " + PivotalException.getErrorMessage(e), e);
                if (!inTransaction) rollbackTransaction();
            }
            catch (Exception e) {
                setError("Problem adding row to database - " + PivotalException.getErrorMessage(e));
                if (!inTransaction) rollbackTransaction();
            }
            finally {
                if (batch != null) batch.closeBatch();
            }
        }

        if (isInError())
            logger.error("Problem adding a record - {}", lastError);
        else
            logger.debug("Record added successfully to {}", table);

        return !isInError();
    }

    /**
     * Updates a row in the specified table using the map of column values
     * The method takes care of type conversion into the native representation
     * If the Add was successful, it will return true, otherwise false and
     * the lastError will contain the error message
     * After a successful add, the supplied rowValues is updated with the newly
     * created row column values
     *
     * There's a bit of seemingly unnecessary type conversion going on here whereby
     * the values bing passed in are likely to be in the correct type for the
     * database update - however, we can't be sure that it isn't simply passed
     * as a string so we convert the value to an unambiguous string first then
     * to the value second
     * This is all part of making sure the handling is as fault safe as possible
     *
     * @param sTable      Table to update
     * @param whereClause Clause to uniquely identify a row to update
     * @param rowValues   Map of column name/values
     * @return True if succeeded
     */
    public boolean updateRecord(String sTable, String whereClause, Map<String, Object> rowValues) {
        return updateRecord(sTable, whereClause, rowValues, true);
    }

    /**
     * Updates a row in the specified table using the map of column values
     * The method takes care of type conversion into the native representation
     * If the Add was successful, it will return true, otherwise false and
     * the lastError will contain the error message
     * After a successful add, the supplied rowValues is updated with the newly
     * created row column values
     *
     * There's a bit of seemingly unnecessary type conversion going on here whereby
     * the values bing passed in are likely to be in the correct type for the
     * database update - however, we can't be sure that it isn't simply passed
     * as a string so we convert the value to an unambiguous string first then
     * to the value second
     * This is all part of making sure the handling is as fault safe as possible
     *
     * @param sTable          Table to update
     * @param whereClause     Clause to uniquely identify a row to update
     * @param rowValues       Map of column name/values
     * @param updateRowValues True if the row values should be updated with the row
     * @return True if succeeded
     */
    public boolean updateRecord(String sTable, String whereClause, Map<String, Object> rowValues, boolean updateRowValues) {

        // Check to see if we should auto commit

        boolean inTransaction = isInsideTranasaction;

        // Check the parameters are OK

        if (checkParameters(sTable, rowValues)) {
            if (Common.isBlank(whereClause)) {
                setError("You must specify a where clause for your update");
            }
            else {
                if (!inTransaction) startTransactionIfNotStartedYet();
                PreparedStatement updatestmt = null;
                Statement stmt = null;
                ResultSet table = null;
                try {

                    // Add the values to the tables

                    TableMetaData meta = TableMetaData.getMetaData(dbConnection, sTable);

                    // We can't rely on being able to update a table using a results set so
                    // we will have to create a prepared statement to execute an UPDATE

                    List<String> columns = new ArrayList<>();
                    StringBuilder builder = new StringBuilder();
                    for (String column : rowValues.keySet()) {
                        if (builder.length() > 0) builder.append(',');
                        builder.append(column);
                        builder.append("=?");
                        columns.add(column);
                    }
                    builder.insert(0, "update " + sTable + " set ");
                    builder.append(" where ");
                    builder.append(whereClause);
                    updatestmt = dbConnection.prepareStatement(builder.toString());

                    // Add all the values to the results set

                    for (int column = 0; column < columns.size(); column++) {
                        String key = columns.get(column);
                        int columnType = meta.getColumnType(meta.findColumn(key));
                        if(Common.doStringsMatch(rowValues.get(key).toString(),"null")){
                            DataSourceUtils.setPreparedStatementValue(null, column + 1, columnType, updatestmt);
                        }
                        else {
                            DataSourceUtils.setPreparedStatementValue(rowValues.get(key), column + 1, columnType, updatestmt);
                        }
                    }

                    // Commit the row and retrieve the new record values

                    if (updatestmt.executeUpdate() > 0) {

                        // Retrieve the updated values

                        if (updateRowValues) {
                            stmt = dbConnection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                            table = stmt.executeQuery("select * from " + sTable + " where " + whereClause);
                            if (table.next())
                                getLastRowValues(table, meta, rowValues);
                            else
                                logger.error("Cannot get values after update to [{}] using [{}]", sTable, whereClause);
                        }
                    }
                    else {
                        logger.debug("No rows changed in update to [{}] using [{}]", sTable, whereClause);
                    }

                    // Commit the transaction

                    if (!inTransaction) commitTransaction();

                }
                catch (SQLException e) {
                    setError("Problem updating row in database - " + Z_GetRowValues(rowValues) + " - " + PivotalException.getErrorMessage(e), e);
                    if (!inTransaction) rollbackTransaction();
                }
                catch (Exception e) {
                    setError("Problem updating row in database - " + PivotalException.getErrorMessage(e));
                    if (!inTransaction) rollbackTransaction();
                }
                finally {
                    Common.close(table, stmt, updatestmt);
                }
            }
        }

        if (!isInError())
            logger.debug("Record updated successfully in {} for {}", sTable, whereClause);

        return !isInError();
    }

    /**
     * Executes the statement against the database within an implicit
     * transaction if one is not in operation
     *
     * @param sql SQL statement to execute
     * @return Returns true if OK
     */
    public boolean execute(String sql) {
        lastError = null;
        getConnection();
        Statement stmt = null;
        logger.debug("Executing statement [{}]", sql);
        try {
            stmt = dbConnection.createStatement();
            stmt.execute(sql);
        }
        catch (SQLException e) {
            setError("Problem executing statement [" + sql + "] on [" + name + "] - " + PivotalException.getErrorMessage(e), e);
        }
        finally {
            Common.close(stmt);
        }
        return !isInError();
    }

    /**
     * Runs the select statement against the database and returns the
     * first row as a map keyed on case insensitive column names
     *
     * @param sql               Select statement to execute
     * @param bypassCache       If it should bypass cache in case it exists
     * @return Map of column data keyed on the column name or
     *         an empty map if nothing found
     */
    public Map<String, Object> findFirst(String sql, boolean bypassCache) {
        List<Map<String, Object>> list = find(sql, bypassCache);
        if (Common.isBlank(list))
            return new HashMap<>();
        else
            return list.get(0);
    }

    /**
     * Runs the select statement against the database and returns the
     * first row as a map keyed on case insensitive column names
     *
     * @param sql Select statement to execute
     * @return Map of column data keyed on the column name or
     *         an empty map if nothing found
     */
    public Map<String, Object> findFirst(String sql) {
        return findFirst(sql, false);
    }

    /**
     * Runs the select statement against the database and returns a comma
     * separated list of the values in the first column
     *
     * @param sql         Select statement to execute
     * @param bypassCache If it should bypass cache in case it exists
     * @return String in the form "xx,yy,yy"
     */
    public String findList(String sql, boolean bypassCache) {
        return getInClauseFromResults(find(sql, bypassCache), null);
    }

    /**
     * Runs the select statement against the database and returns a comma
     * separated list of the values in the first column
     *
     * @param sql Select statement to execute
     * @return String in the form "xx,yy,yy"
     */
    public String findList(String sql) {
        return findList(sql, false);
    }

    /**
     * Runs the select statement against the database and returns a comma
     * separated list of the values in the specified column
     *
     * @param sql         Select statement to execute
     * @param columnName  Name of the column to get values for
     * @param bypassCache If it should bypass cache in case it exists
     * @return String in the form "xx,yy,yy"
     */
    public String findList(String sql, String columnName, boolean bypassCache) {
        return getInClauseFromResults(find(sql, bypassCache), columnName);
    }

    /**
     * Runs the select statement against the database and returns a comma
     * separated list of the values in the specified column
     *
     * @param sql        Select statement to execute
     * @param columnName Name of the column to get values for
     * @return String in the form "xx,yy,yy"
     */
    public String findList(String sql, String columnName) {
        return findList(sql, columnName, false);
    }

    /**
     * Runs the select statement against the database and returns the
     * results as a list of mapped rows where the map is keyed on
     * a case insensitive column names
     * This is more efficient than it sounds because it re-uses the
     * column names from a column name store
     *
     * @param sql         Select statement to execute
     * @param bypassCache If it should bypass cache in case it exists
     * @return List of rows of data keyed on the column name or
     *         an empty list if nothing found
     */
    public List<Map<String, Object>> find(String sql, boolean bypassCache) {
        return find(sql, bypassCache, (Object[])null);
    }

    /**
     * Runs the select statement against the database and returns the
     * results as a list of mapped rows where the map is keyed on
     * a case insensitive column names
     * This is more efficient than it sounds because it re-uses the
     * column names from a column name store
     * The parameters is an array of any positional SQL parameters within
     * the query
     *
     * @param sql               Select statement to execute
     * @param bypassCache       If it should bypass cache in case it exists
     * @param parameters        Array of optional parameters
     * @return List of rows of data keyed on the column name or
     *         an empty list if nothing found
     */
    public List<Map<String, Object>> find(String sql, boolean bypassCache, Object... parameters) {

        resultsTruncated = false;
        resultsCached = false;
        lastError = null;
        long startTime = new Date().getTime();
        List<Map<String, Object>> list;

        if (bypassCache || (list = getCachedQuery(sql)) == null) {

            logger.debug("Object is not cached or cache is disabled ");

            list = new ArrayList<>();

            // Only do something if we have a database connection

            getConnection();
            if (dbConnection != null) {
                ResultSet results = null;
                PreparedStatement stmt = null;
                logger.debug("Running statement [{}]", sql);
                try {

                    // Prepare the query

                    stmt = dbConnection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                    if (obtainConnectionUrl().contains("mysql"))
                        stmt.setFetchSize(Integer.MIN_VALUE);
                    else
                        stmt.setFetchSize(DEFAULT_FETCH_SIZE);

                    // Add on any parameters that might have been passed

                    if (!Common.isBlank(parameters)) {
                        int pos = 1;
                        for (Object obj : parameters) {
                            stmt.setObject(pos, obj);
                            pos++;
                        }
                    }

                    // Run the query

                    results = stmt.executeQuery();

                    // Log the query

                    if (Perflogger.isInfoEnabled()) {
                        long duration = new Date().getTime() - startTime;
                        Perflogger.info(duration + '\t' + sql);
                    }

                    // Get the results if there are any

                    if (results != null) {
                        ResultSetMetaData meta = results.getMetaData();
                        int columnCount = meta.getColumnCount();

                        // Construct a central store of column names
                        // Do it now as better for larger result sets and not much overhead for small ones

                        String[] columnNames = new String[columnCount + 1];
                        Set<String> columnNamesX = new HashSet<>();
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = meta.getColumnLabel(i);
                            if (columnName.equals(meta.getColumnName(i)))
                                columnName = columnName.toLowerCase();
                            if (columnNamesX.contains(columnName))
                                columnNames[i] = columnName + '_' + i;
                            else
                                columnNames[i] = columnName;
                            columnNamesX.add(columnName);
                        }

                        Object value;
                        Map<String, Object> row;

                        // Loop through all the results or until we reach the limit

                        while (results.next() && (maximumResults == 0 || list.size() < maximumResults)) {
                            row = new LinkedHashMap<>(columnCount);
                            for (int iCnt = 1; iCnt <= columnCount; iCnt++) {

                                // Get the value based on it's type

                                value = null;
                                try {
                                    if (meta.getColumnType(iCnt)==Types.CLOB) {
                                        Reader reader = results.getCharacterStream(iCnt);
                                        if (reader!=null) {
                                            value = IOUtils.toString(reader);
                                        }
                                    }
                                    else if (meta.getColumnType(iCnt)==Types.BLOB) {
                                        InputStream input = results.getBinaryStream(iCnt);
                                        if (input != null) {
                                            value = IOUtils.toByteArray(input);
                                        }
                                    }
                                    else {
                                        value = results.getObject(iCnt);
                                    }
                                }
                                catch (Throwable e) {
                                    logger.error("Problem getting database connection for [{}] - {}", getName(), PivotalException.getErrorMessage(e));
                                }

                                // Add the value to the row

                                row.put(columnNames[iCnt], value);
                            }
                            list.add(row);
                        }

                        // Show a warning if we have reached the limit

                        if (maximumResults > 0 && list.size() == maximumResults && !results.isAfterLast()) {
                            resultsTruncated = true;
                            logger.warn("The maximum number of results [{}] has been reached for [{}]", maximumResults, sql);
                        }
                    }

                    // Cache the result unless we're not allowed to

                    if (!bypassCache) {
                        putCachedQuery(sql, list);
                        list = DataSourceUtils.cloneResultsList(list);
                    }
                }
                catch (SQLException e) {
                    setError("SQL Problem running query [" + sql + "] - " + PivotalException.getErrorMessage(e), e);
                }
                catch (Exception e) {
                    setError("Problem running query [" + sql + "] - " + PivotalException.getErrorMessage(e));
                }
                finally {
                    Common.close(results, stmt);
                }
                logger.debug("Search returned {} results", results == null ? "no" : "some");
            }
            else {
                setError("Problem with connection for [" + sql + "] - Call made to find without a valid connection");
            }
        }

        // We're getting the values from the cache

        else {
            resultsCached = true;
            logger.debug("Object found in cache");

            // We need to make a copy of the rows into new Maps of a new List so that
            // if the user modifies the contents in some way, it doesn't pollute the
            // cache for everyone else

            if (!Common.isBlank(list)) {
                list = DataSourceUtils.cloneResultsList(list);
            }
        }
        lastDuration = new Date().getTime() - startTime;
        return list;
    }

    /**
     * Runs the select statement against the database and sends the
     * results direct to the output stream
     * It assumes that stream should be seen as an attachment so it
     * sets the headers correctly and can optionally include the header
     * row
     *
     * @param sql            Select statement to execute
     * @param response       Response stream to use to send back the attachment
     * @param attachmentName Name of the attachment to use
     * @param delimiter           Delimiter string (normally ,)
     * @param includeHeader  True if the header row should be included
     */
    public void findToCSV(String sql, HttpServletResponse response, String attachmentName, String delimiter, boolean includeHeader) {
        findToCSV(sql, response, attachmentName, delimiter, includeHeader, null, false);
    }

    /**
     * Runs the select statement against the database and sends the
     * results direct to the output stream
     * It assumes that stream should be seen as an attachment so it
     * sets the headers correctly and can optionally include the header
     * row
     *
     * @param sql            Select statement to execute
     * @param response       Response stream to use to send back the attachment
     * @param attachmentName Name of the attachment to use
     * @param delimiter           Delimiter string (normally ,)
     * @param includeHeader  True if the header row should be included
     * @param header         List of values to output as a header
     */
    public void findToCSV(String sql, HttpServletResponse response, String attachmentName, String delimiter, boolean includeHeader, List<Object> header) {
        findToCSV(sql, response, null, attachmentName, delimiter, includeHeader, null, false, header, null);
    }

    /**
     * Runs the select statement against the database and sends the
     * results direct to the output stream
     * It assumes that stream should be seen as an attachment so it
     * sets the headers correctly and can optionally include the header
     * row
     *
     * @param sql            Select statement to execute
     * @param response       Response stream to use to send back the attachment
     * @param attachmentName Name of the attachment to use
     * @param delimiter           Delimiter string (normally ,)
     * @param includeHeader  True if the header row should be included
     * @param header         List of values to output as a header
     * @param footer         List of values to output as a footer
     */
    public void findToCSV(String sql, HttpServletResponse response, String attachmentName, String delimiter, boolean includeHeader, List<Object> header, List<Object> footer) {
        findToCSV(sql, response, null, attachmentName, delimiter, includeHeader, null, false, header, footer);
    }

    /**
     * Runs the select statement against the database and sends the
     * results direct to the output stream
     * It assumes that stream should be seen as an attachment so it
     * sets the headers correctly and can optionally include the header
     * row
     *
     * @param sql                Select statement to execute
     * @param response           Response stream to use to send back the attachment
     * @param attachmentName     Name of the attachment to use
     * @param delimiter           Delimiter string (normally ,)
     * @param includeHeader      True if the header row should be included
     * @param conflateColumns    List of columns that can be conflated
     * @param conflatToSingleRow True if the conflation should create a single row
     */
    public void findToCSV(String sql, HttpServletResponse response, String attachmentName, String delimiter, boolean includeHeader, List<String> conflateColumns, boolean conflatToSingleRow) {
        findToCSV(sql, response, null, attachmentName, delimiter, includeHeader, conflateColumns, conflatToSingleRow, null, null);
    }

    /**
     * Runs the select statement against the database and sends the
     * results direct to the output stream
     * It assumes that stream should be seen as an attachment so it
     * sets the headers correctly and can optionally include the header
     * row
     *
     * @param sql                Select statement to execute
     * @param response           Response stream to use to send back the attachment
     * @param attachmentName     Name of the attachment to use
     * @param delimiter           Delimiter string (normally ,)
     * @param includeHeader      True if the header row should be included
     * @param header             List of values to output as a header
     * @param conflateColumns    List of columns that can be conflated
     * @param conflatToSingleRow True if the conflation should create a single row
     */
    public void findToCSV(String sql, HttpServletResponse response, String attachmentName, String delimiter, boolean includeHeader, List<Object> header, List<String> conflateColumns, boolean conflatToSingleRow) {
        findToCSV(sql, response, null, attachmentName, delimiter, includeHeader, conflateColumns, conflatToSingleRow, header, null);
    }

    /**
     * Runs the select statement against the database and sends the
     * results direct to the output stream
     * It assumes that stream should be seen as an attachment so it
     * sets the headers correctly and can optionally include the header
     * row
     *
     * @param sql                 Select statement to execute
     * @param response            Response stream to use to send back the attachment
     * @param request             Request stream to use
     * @param attachmentName      Name of the attachment to use
     * @param delimiter           Delimiter string (normally ,)
     * @param includeHeader       True if the header row should be included
     * @param conflateColumns     List of columns that can be conflated
     * @param conflateToSingleRow True if the conflation should create a single row
     * @param header              List of values to output as a header
     * @param footer              List of values to output as a footer
     */
    public void findToCSV(String sql, HttpServletResponse response, HttpServletRequest request, String attachmentName, String delimiter, boolean includeHeader, List<String> conflateColumns, boolean conflateToSingleRow, List<Object> header, List<Object> footer) {

        // If we have already been through

        Timer timer = null;
        ResponseFlush task = null;
        if (response == null) response = ServletHelper.getResponse();
        if (request == null) request = ServletHelper.getRequest();
        if (!response.containsHeader("Content-Disposition") || Common.isBlank(attachmentName)) {
            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + attachmentName + '"');
            response.setHeader("Content-Description", attachmentName);
            response.setContentLength(-1);

            // DownloadTokenValue will have been provided in the form submit via the hidden input field

            if (request != null && response != null) {
                String downloadCookie = request.getParameter(Constants.FILE_DOWNLOAD_TOKEN);
                if (!Common.isBlank(downloadCookie)) {
                    Cookie cookie;
                    cookie = new Cookie(Constants.FILE_DOWNLOAD_TOKEN, downloadCookie);
                    cookie.setMaxAge(-1);
                    cookie.setPath("/");
                    response.addCookie(cookie);
                }
            }

            // We need to create a timer here to flush out the header after 50 seconds so that the browser
            // doesn't time out waiting for some sort of response

            task = new ResponseFlush(response);
            timer = new Timer("HeaderFlush for " + attachmentName, true);
            timer.schedule(task, HEADER_FLUSH_TIMEOUT * 1000);
        }

        try {
            // Now send the actual content

            if (response!=null)
                findToCSV(sql, response.getOutputStream(), timer, task, delimiter, includeHeader, conflateColumns, conflateToSingleRow, header, footer);
        }
        catch (Exception e) {
            setError("Problem running query to CSV [" + sql + "] - " + PivotalException.getErrorMessage(e));
        }
    }

    /**
     * Runs the select statement against the database and sends the
     * results direct to the output stream
     * Optionally include the header row
     * It is the responsibility of the caller to close the output stream
     *
     * @param sql           Select statement to execute
     * @param output        Stream to send the output to
     * @param includeHeader True if the header row should be included
     * @param delimiter           Delimiter string (normally ,)
     */
    public void findToCSV(String sql, OutputStream output, String delimiter, boolean includeHeader) {
        findToCSV(sql, output, delimiter, includeHeader, null, false, null, null);
    }

    /**
     * Runs the select statement against the database and sends the
     * results direct to the output stream
     * Optionally include the header row
     * It is the responsibility of the caller to close the output stream
     *
     * @param sql           Select statement to execute
     * @param output        Stream to send the output to
     * @param delimiter           Delimiter string (normally ,)
     * @param includeHeader True if the header row should be included
     * @param header        List of values to output as a header
     */
    public void findToCSV(String sql, OutputStream output, String delimiter, boolean includeHeader, List<Object> header) {
        findToCSV(sql, output, delimiter, includeHeader, null, false, header, null);
    }

    /**
     * Runs the select statement against the database and sends the
     * results direct to the output stream
     * Optionally include the header row
     * It is the responsibility of the caller to close the output stream
     *
     * @param sql           Select statement to execute
     * @param output        Stream to send the output to
     * @param delimiter           Delimiter string (normally ,)
     * @param includeHeader True if the header row should be included
     * @param header        List of values to output as a header
     * @param footer        List of values to output as a footer
     */
    public void findToCSV(String sql, OutputStream output, String delimiter, boolean includeHeader, List<Object> header, List<Object> footer) {
        findToCSV(sql, output, delimiter, includeHeader, null, false, header, footer);
    }

    /**
     * Runs the select statement against the database and sends the
     * results direct to the output stream
     * Optionally include the header row
     * It is the responsibility of the caller to close the output stream
     *
     * @param sql                 Select statement to execute
     * @param output              Stream to send the output to
     * @param delimiter           Delimiter string (normally ,)
     * @param includeHeader       True if the header row should be included
     * @param conflateColumns     List of columns that can be conflated
     * @param conflateToSingleRow True if the conflation should create a single row
     * @param header              List of values to output as a header
     * @param footer              List of values to output as a footer
     */
    public void findToCSV(String sql, OutputStream output, String delimiter, boolean includeHeader, List<String> conflateColumns, boolean conflateToSingleRow, List<Object> header, List<Object> footer) {
        findToCSV(sql, output, null, null, delimiter, includeHeader, conflateColumns, conflateToSingleRow, header, footer);
    }

    /**
     * Runs the select statement against the database and sends the
     * results direct to the output stream
     * It assumes that stream should be seen as an attachment so it
     * sets the headers correctly and can optionally include the header
     * row
     *
     * @param sql            Select statement to execute
     * @param response       Response stream to use to send back the attachment
     * @param attachmentName Name of the attachment to use
     * @param includeHeader  True if the header row should be included
     */
    public void findToCSV(String sql, HttpServletResponse response, String attachmentName, boolean includeHeader) {
        findToCSV(sql, response, attachmentName, includeHeader, null, false);
    }

    /**
     * Runs the select statement against the database and sends the
     * results direct to the output stream
     * It assumes that stream should be seen as an attachment so it
     * sets the headers correctly and can optionally include the header
     * row
     *
     * @param sql            Select statement to execute
     * @param response       Response stream to use to send back the attachment
     * @param attachmentName Name of the attachment to use
     * @param includeHeader  True if the header row should be included
     * @param header         List of values to output as a header
     */
    public void findToCSV(String sql, HttpServletResponse response, String attachmentName, boolean includeHeader, List<Object> header) {
        findToCSV(sql, response, null, attachmentName, includeHeader, null, false, header, null);
    }

    /**
     * Runs the select statement against the database and sends the
     * results direct to the output stream
     * It assumes that stream should be seen as an attachment so it
     * sets the headers correctly and can optionally include the header
     * row
     *
     * @param sql            Select statement to execute
     * @param response       Response stream to use to send back the attachment
     * @param attachmentName Name of the attachment to use
     * @param includeHeader  True if the header row should be included
     * @param header         List of values to output as a header
     * @param footer         List of values to output as a footer
     */
    public void findToCSV(String sql, HttpServletResponse response, String attachmentName, boolean includeHeader, List<Object> header, List<Object> footer) {
        findToCSV(sql, response, null, attachmentName, includeHeader, null, false, header, footer);
    }

    /**
     * Runs the select statement against the database and sends the
     * results direct to the output stream
     * It assumes that stream should be seen as an attachment so it
     * sets the headers correctly and can optionally include the header
     * row
     *
     * @param sql                Select statement to execute
     * @param response           Response stream to use to send back the attachment
     * @param attachmentName     Name of the attachment to use
     * @param includeHeader      True if the header row should be included
     * @param conflateColumns    List of columns that can be conflated
     * @param conflatToSingleRow True if the conflation should create a single row
     */
    public void findToCSV(String sql, HttpServletResponse response, String attachmentName, boolean includeHeader, List<String> conflateColumns, boolean conflatToSingleRow) {
        findToCSV(sql, response, null, attachmentName, includeHeader, conflateColumns, conflatToSingleRow, null, null);
    }

    /**
     * Runs the select statement against the database and sends the
     * results direct to the output stream
     * It assumes that stream should be seen as an attachment so it
     * sets the headers correctly and can optionally include the header
     * row
     *
     * @param sql                Select statement to execute
     * @param response           Response stream to use to send back the attachment
     * @param attachmentName     Name of the attachment to use
     * @param includeHeader      True if the header row should be included
     * @param header             List of values to output as a header
     * @param conflateColumns    List of columns that can be conflated
     * @param conflatToSingleRow True if the conflation should create a single row
     */
    public void findToCSV(String sql, HttpServletResponse response, String attachmentName, boolean includeHeader, List<Object> header, List<String> conflateColumns, boolean conflatToSingleRow) {
        findToCSV(sql, response, null, attachmentName, includeHeader, conflateColumns, conflatToSingleRow, header, null);
    }

    /**
     * Runs the select statement against the database and sends the
     * results direct to the output stream
     * It assumes that stream should be seen as an attachment so it
     * sets the headers correctly and can optionally include the header
     * row
     *
     * @param sql                 Select statement to execute
     * @param response            Response stream to use to send back the attachment
     * @param request             Request stream to use
     * @param attachmentName      Name of the attachment to use
     * @param includeHeader       True if the header row should be included
     * @param conflateColumns     List of columns that can be conflated
     * @param conflateToSingleRow True if the conflation should create a single row
     * @param header              List of values to output as a header
     * @param footer              List of values to output as a footer
     */
    public void findToCSV(String sql, HttpServletResponse response, HttpServletRequest request, String attachmentName, boolean includeHeader, List<String> conflateColumns, boolean conflateToSingleRow, List<Object> header, List<Object> footer) {
        findToCSV(sql, response, request, attachmentName, DEFAULT_DELIMITER, includeHeader, conflateColumns, conflateToSingleRow, header, footer);
    }

    /**
     * Runs the select statement against the database and sends the
     * results direct to the output stream
     * Optionally include the header row
     * It is the responsibility of the caller to close the output stream
     *
     * @param sql           Select statement to execute
     * @param output        Stream to send the output to
     * @param includeHeader True if the header row should be included
     */
    public void findToCSV(String sql, OutputStream output, boolean includeHeader) {
        findToCSV(sql, output, includeHeader, null, false, null, null);
    }

    /**
     * Runs the select statement against the database and sends the
     * results direct to the output stream
     * Optionally include the header row
     * It is the responsibility of the caller to close the output stream
     *
     * @param sql           Select statement to execute
     * @param output        Stream to send the output to
     * @param includeHeader True if the header row should be included
     * @param header        List of values to output as a header
     */
    public void findToCSV(String sql, OutputStream output, boolean includeHeader, List<Object> header) {
        findToCSV(sql, output, includeHeader, null, false, header, null);
    }

    /**
     * Runs the select statement against the database and sends the
     * results direct to the output stream
     * Optionally include the header row
     * It is the responsibility of the caller to close the output stream
     *
     * @param sql           Select statement to execute
     * @param output        Stream to send the output to
     * @param includeHeader True if the header row should be included
     * @param header        List of values to output as a header
     * @param footer        List of values to output as a footer
     */
    public void findToCSV(String sql, OutputStream output, boolean includeHeader, List<Object> header, List<Object> footer) {
        findToCSV(sql, output, includeHeader, null, false, header, footer);
    }

    /**
     * Runs the select statement against the database and sends the
     * results direct to the output stream
     * Optionally include the header row
     * It is the responsibility of the caller to close the output stream
     *
     * @param sql                 Select statement to execute
     * @param output              Stream to send the output to
     * @param includeHeader       True if the header row should be included
     * @param conflateColumns     List of columns that can be conflated
     * @param conflateToSingleRow True if the conflation should create a single row
     * @param header              List of values to output as a header
     * @param footer              List of values to output as a footer
     */
    public void findToCSV(String sql, OutputStream output, boolean includeHeader, List<String> conflateColumns, boolean conflateToSingleRow, List<Object> header, List<Object> footer) {
        findToCSV(sql, output, null, null, DEFAULT_DELIMITER, includeHeader, conflateColumns, conflateToSingleRow, header, footer);
    }

    /**
     * Runs the select statement against the database and sends the
     * results direct to the output stream
     * Optionally include the header row
     * It will also optionally send a cookie down when the data is about to be sent to signal
     * that download is beginning
     * It is the responsibility of the caller to close the output stream
     *
     * @param sql                 Select statement to execute
     * @param output              Stream to send the output to
     * @param responseFlush       Timer used for triggering flushing the response stream
     * @param responseFlushTask   Task that flushes the response stream
     * @param delimiter           Delimiter string (normally ,)
     * @param includeHeader       True if the header row should be included
     * @param conflateColumns     List of columns that can be conflated
     * @param conflateToSingleRow True if the conflation should create a single row
     * @param header              List of values to output as a header
     * @param footer              List of values to output as a footer
     */
    private void findToCSV(String sql, OutputStream output, Timer responseFlush, TimerTask responseFlushTask, String delimiter, boolean includeHeader, List<String> conflateColumns, boolean conflateToSingleRow, List<Object> header, List<Object> footer) {

        resultsTruncated = false;
        lastError = null;
        long startTime = new Date().getTime();
        if (delimiter==null) delimiter = DEFAULT_DELIMITER;

        // Only do something if we have a database connection

        getConnection();
        if (dbConnection != null) {
            ResultSet results = null;
            Statement stmt = null;
            logger.debug("Running statement [{}]", sql);
            TextOutput csvOut = null;
            try {

                // Setup the statement - it's important we don't blow the JVM heap
                // so we need to make sure the fetch size is kept low

                stmt = dbConnection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                if (obtainConnectionUrl().contains("mysql"))
                    stmt.setFetchSize(Integer.MIN_VALUE);
                else
                    stmt.setFetchSize(DEFAULT_FETCH_SIZE);

                // Run the query

                results = stmt.executeQuery(sql);

                // Cancel the flush and send out the header

                if (responseFlush != null) {
                    responseFlush.cancel();
                    if (responseFlushTask!=null)
                        responseFlushTask.run();
                }

                // Output a header if we have one

                csvOut = new TextOutput(output, delimiter, !includeHeader);
                if (!Common.isBlank(header)) {
                    csvOut.newSection(header);
                    csvOut.flush();
                }

                // Get the results if there are any

                if (results != null) {
                    ResultSetMetaData meta = results.getMetaData();
                    int columnCount = meta.getColumnCount();

                    // Construct a central store of column names
                    // Do it now as better for larger result sets and not much overhead for small ones

                    String[] columnNames = new String[columnCount + 1];
                    Set<String> columnNamesX = new HashSet<>();

                    // Set up a list of non conflated columns if conflating to single row

                    List<String> nonConflatedColumns = null;

                    // Check if we need to create the non conflated list. If not then we cannot conflateToSingleRow

                    if (conflateToSingleRow && !Common.isBlank(conflateColumns))
                        nonConflatedColumns = new ArrayList<>();
                    else
                        conflateToSingleRow = false;

                    for (int i = 1; i <= columnCount; i++) {

                        String columnName = meta.getColumnLabel(i);
                        if (columnName.equals(meta.getColumnName(i)))
                            columnName = columnName.toLowerCase();
                        if (columnNamesX.contains(columnName))
                            columnNames[i] = columnName + '_' + i;
                        else
                            columnNames[i] = columnName;
                        columnNamesX.add(columnName);

                        if (conflateToSingleRow && !Common.isBlank(conflateColumns)) {

                            // Check to see if the column is not in the conflateColumn list.
                            // If it isn't then add to the nonConflatedColumns list

                            boolean columnNotConflated = true;
                            for (int j = 0; columnNotConflated && j < conflateColumns.size(); j++) {
                                if (conflateColumns.get(j).equalsIgnoreCase(columnNames[i]))
                                    columnNotConflated = false;
                            }
                            if (columnNotConflated)
                                nonConflatedColumns.add(columnNames[i]);
                        }
                    }

                    // Loop through all the results or until we reach the limit

                    Object value;
                    Map<String, Object> previousRow = null;
                    while (results.next()) {
                        Map<String, Object> row = new LinkedHashMap<>(columnCount);
                        for (int iCnt = 1; iCnt <= columnCount; iCnt++) {

                            // Get the value form the database

                            value = null;
                            try {
                                if (meta.getColumnType(iCnt)==Types.CLOB)
                                    value = IOUtils.toString(results.getCharacterStream(iCnt));
                                else if (meta.getColumnType(iCnt)==Types.BLOB)
                                    value = IOUtils.toByteArray(results.getBinaryStream(iCnt));
                                else
                                    value = results.getObject(iCnt);
                            }
                            catch (Throwable e) {
                                logger.error("Problem getting database connection for [{}] - {}", getName(), PivotalException.getErrorMessage(e));
                            }

                            // Add the value to the row

                            row.put(columnNames[iCnt], value);
                        }
                        if (conflateToSingleRow) {

                            // In this scenario conflateColumns indicates the column(s) that must be the same. Others are conflated to a single row
                            // Only really useful for String based data

                            boolean hasChanged = false;
                            if (previousRow == null) {

                                // First row in the results

                                if (results.isLast())
                                    csvOut.addRow(row);
                                else
                                    previousRow = row;
                            }
                            else {

                                // Check for conflated column changes

                                for (String column : conflateColumns) {
                                    Object oldval = previousRow.get(column);
                                    Object newval = row.get(column);
                                    if (oldval == null && newval != null)
                                        hasChanged = true;
                                    else if (oldval != null && newval == null)
                                        hasChanged = true;
                                    else if (oldval != null && newval != null && !oldval.equals(newval))
                                        hasChanged = true;
                                }
                                if (hasChanged) {

                                    // Changes between current and previous so output previous row. If row is the last in the list then also output that

                                    csvOut.addRow(previousRow);
                                    if (results.isLast())
                                        csvOut.addRow(row);
                                    else
                                        previousRow = row;
                                }
                                else {

                                    // No changes in the conflated columns so append the non conflated values to the previous row

                                    for (String column : nonConflatedColumns) {
                                        Object oldval = previousRow.get(column);
                                        Object newval = row.get(column);
                                        if (oldval == null && newval != null)
                                            previousRow.put(column, newval);
                                        else if (oldval != null && newval != null)
                                            previousRow.put(column, oldval.toString() + '\n' + newval.toString());
                                    }

                                    // If this is the last row then we need to output the previous row which by now includes the non conflated data

                                    if (results.isLast())
                                        csvOut.addRow(previousRow);
                                }
                            }
                        }
                        else {
                            // If we are conflating then we need to only output changed values
                            // in the columns that do the conflation

                            if (!Common.isBlank(conflateColumns)) {
                                Map<String, Object> rowx = new LinkedHashMap<>(row);
                                if (previousRow != null) {
                                    for (String column : conflateColumns) {
                                        if (row.containsKey(column)) {
                                            Object oldVal = previousRow.get(column);
                                            Object newVal = row.get(column);
                                            if (oldVal == null && newVal == null ||
                                                    oldVal != null && oldVal.equals(newVal) ||
                                                    newVal != null && newVal.equals(oldVal)) {
                                                row.put(column, null);
                                            }
                                        }
                                    }
                                }
                                previousRow = rowx;
                            }
                            csvOut.addRow(row);
                        }
                    }
                }

                // Output a footer if we have one

                if (!Common.isBlank(footer)) csvOut.addRow(footer);
            }
            catch (SQLException e) {
                setError("Problem running query [" + sql + "] - " + PivotalException.getErrorMessage(e), e);
            }
            catch (Exception e) {
                setError("Problem running query [" + sql + "] - " + PivotalException.getErrorMessage(e));
            }
            finally {
                Common.close(results, stmt);

                // Need to flush the buffer otherwise we may leave stuff hanging

                if (csvOut != null) csvOut.flush();

                // NOTE:- The next line of commented code is here to show what would at first site, be
                //        the right thing to do.
                //        We are not explicitly closing the CSV output here - it is assumed that the
                //        caller will do this - this allows us to append to the underlying stream
                //        if (csvOut!=null) csvOut.close();
            }
            logger.debug("Search returned {} results", results == null ? "no" : "some");
        }
        else {
            setError("Problem running query [" + sql + "] - Call made to find without a valid connection");
        }
        lastDuration = new Date().getTime() - startTime;
    }

    /**
     * Retrieve object from cache, using sql as key.
     *
     * Default implementation doesnt't use cache. Cache should be defined by subclasses.
     *
     * @param sql the sql to be looked up in the cache
     * @return the cached value if present, otherwise null
     */
    public List<Map<String, Object>> getCachedQuery(String sql) {
        return null;
    }

    /**
     * Adds the results of the query to the cache.
     *
     * Default implementation doesnt't use cache. Cache should be defined by subclasses.
     *
     * @param sql    the sql to be used as key
     * @param result records fetched from database
     */
    public void putCachedQuery(String sql, List<Map<String, Object>> result) {
    }

    /**
     * Runs the select statement against the database and returns the
     * results as a list of mapped rows where the map is keyed on
     * a case insensitive column names
     * This is more efficient than it sounds because it re-uses the
     * column names from a column name store
     *
     * @param sql Select statement to execute
     * @return List of rows of data keyed on the column name or
     *         an empty list if nothing found
     */
    public List<Map<String, Object>> find(String sql) {
        return find(sql, false);
    }

    /**
     * Begins a new transaction, committing any previous transaction that
     * may have been in place
     * Can be called multiple times to cause a commit/start operation rather
     * than explicitly calling start/commit
     *
     * @return True if the transaction started OK
     */
    public boolean startTransaction() {
        return startTransactionIfNotStartedYet();
    }

    /**
     * Makes sure we are within a transaction scope, and if
     * not, it will start a new transaction.
     *
     * @return True if the transaction continued/started OK
     */
    public boolean startTransactionIfNotStartedYet() {
        if (!isInsideTranasaction) {
            getConnection();
            try {
                isInsideTranasaction = true;
                autoCommitState = dbConnection.getAutoCommit();
                dbConnection.setAutoCommit(false);
            }
            catch (SQLException e) {
                setError("Problem starting a transaction - " + PivotalException.getErrorMessage(e), e);
            }
        }
        return !isInError();
    }

    /**
     * Commits any current transaction that may be in place
     * Has no effect if a transaction is not in place
     *
     * @return True if the transaction committed OK
     */
    public boolean commitTransaction() {
        lastError = null;
        getConnection();
        try {

            // Check that we are in a state to be able to do this

            if (dbConnection != null && !dbConnection.isClosed()) {
                dbConnection.commit();
                dbConnection.setAutoCommit(autoCommitState);
            }
            else {
                logger.debug("Cannot commit a transaction on a closed or invalid connection");
            }
        }
        catch (SQLException e) {
            setError("Problem committing a transaction - " + PivotalException.getErrorMessage(e), e);
        }
        isInsideTranasaction = false;
        return !isInError();
    }

    /**
     * Rolls back any transaction that may be in place
     * Has no effect if a transaction is not started or if
     * there have been no changes
     *
     * @return True if the transaction rolled back OK
     */
    public boolean rollbackTransaction() {
        boolean returnValue = true;
        getConnection();
        try {

            // Check that we are in a state to be able to do this

            if (dbConnection != null && !dbConnection.isClosed()) {
                dbConnection.rollback();
                dbConnection.setAutoCommit(autoCommitState);
            }
            else {
                logger.debug("Cannot commit a transaction on a closed or invalid connection");
            }
        }
        catch (SQLException e) {
            returnValue = false;
            logger.error("Problem rolling back a transaction - " + PivotalException.getErrorMessage(e), e);
        }
        isInsideTranasaction = false;
        return returnValue;
    }

    /**
     * Checks that the parameters are all OK and updates the lastError to
     * indicate if something is wrong
     *
     * @param table     Table to update
     * @param rowValues Map of column name/values
     * @return True if OK
     */
    private boolean checkParameters(String table, Map<String, Object> rowValues) {
        lastError = null;
        getConnection();

        // Check the easy stuff

        try {
            if (Common.isBlank(table))
                setError("You must specify a table name for the add");
            else if (Common.isBlank(rowValues))
                setError("You must provide some values for the row");
            else if (dbConnection == null)
                setError("The database is not connected");
            else if (dbConnection.isClosed())
                setError("The database connection is closed");
            else if (dbConnection.isReadOnly())
                setError("The database cannot be updated it is set to read-only");
            else {

                // Check the voracity of the table

                Set<String> set = metData.get(table);
                if (set == null) {
                    DatabaseMetaData meta = dbConnection.getMetaData();
                    String schema;
                    schema = null;
                    String tableLookup = table.toLowerCase();
                    if (table.contains(".")) {
                        schema = table.split("\\.")[0];
                        tableLookup = table.split("\\.")[1];
                    }

                    ResultSet columns = meta.getColumns(null, schema, tableLookup, null);
                    if (!columns.isBeforeFirst()) {
                        if (!columns.isClosed()) {
                            columns.close();
                        }
                        columns = meta.getColumns(null, schema==null?null:schema.toUpperCase(), tableLookup.toUpperCase(), null);
                    }
                    if (!columns.isBeforeFirst())
                        setError("The table [" + table + "] does not exist");
                    else {

                        // Get a list of all the columns

                        set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                        while (columns.next()) {
                            set.add(columns.getString("COLUMN_NAME").toLowerCase());
                        }
                        metData.put(table, set);
                    }
                }

                // Now check that all the columns exist

                if (Common.isBlank(set))
                    setError("The table [" + table + "] does not exist or does not contain any columns");
                else {
                    List<String> fieldsToRemove = null;
                    for (String column : rowValues.keySet()) {
                        if (set!=null && !set.contains(column)) {
                            if (Common.isBlank(lastError)) {
                                logger.debug("The Table [{}] does not contain column(s): {}", table, column);
                            }
                            else {
                                logger.debug("{} {}", lastError, column);
                            }
                            if (fieldsToRemove == null) fieldsToRemove = new ArrayList<>();
                            fieldsToRemove.add(column);
                        }
                    }
                    if (!Common.isBlank(fieldsToRemove)) {
                        for (String column : fieldsToRemove) {
                            rowValues.remove(column);
                        }
                    }
                }
            }
        }
        catch (SQLException e) {
            setError("Problem checking parameters - " + PivotalException.getErrorMessage(e), e);
        }
        catch (Exception e) {
            setError("Problem checking parameters - %s" + PivotalException.getErrorMessage(e));
        }
        return !isInError();
    }

    /**
     * Updates the last row values map with the data at the current row location
     *
     * @param table Results set to use
     * @param meta  Table metadata
     * @param row   Row to update (usually the source)
     * @throws SQLException Errors
     */
    private static void getLastRowValues(ResultSet table, ResultSetMetaData meta, Map<String, Object> row) throws SQLException {
        row.clear();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            Object value = table.getObject(i);

            // Add the row with the value

            if (value == null)
                row.put(meta.getColumnName(i), "");
            else
                row.put(meta.getColumnName(i), value);
        }
    }

    /**
     * Updates the last row values map with the data at the current row location
     *
     * @param table Results set to use
     * @param meta  Table metadata
     * @param row   Row to update (usually the source)
     * @throws SQLException Errors
     */
    private static void getLastRowValues(ResultSet table, TableMetaData meta, Map<String, Object> row) throws SQLException {
        row.clear();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            Object value = table.getObject(i);

            // Add the row with the value

            if (value == null)
                row.put(meta.getColumnName(i), "");
            else
                row.put(meta.getColumnName(i), value);
        }
    }

    /**
     * Returns the limt to the number of results to return from a call
     * to find
     *
     * @return The maximum number of results returned
     */
    public int getMaximumResults() {
        return maximumResults;
    }

    /**
     * Sets the maximum number of results that can be returned from a
     * call to find
     * A value of 0 means unlimited - very dangerous
     *
     * @param maximumResults Maximum number of rows to gather
     */
    public void setMaximumResults(int maximumResults) {
        this.maximumResults = maximumResults > -1 ? maximumResults : DEFAULT_MAXIMUM_RESULTS;
    }

    /**
     * Returns true if the last find command produced more than maximumResults
     * rows of data but the returned list was truncated
     *
     * @return True if the the results from the last find were truncated
     */
    public boolean isResultsTruncated() {
        return resultsTruncated;
    }

    /**
     * Returns true if the last find command returned data from the cache
     *
     * @return True if the the results are from the cache
     */
    public boolean isResultsCached() {
        return resultsCached;
    }

    /**
     * This method is a convenience function for retrieving a map of values from the
     * database, keyed on the key column
     * The nature of this function is such that it doesn't make sense to give it
     * a keyColumn that isn't unique - wouldn't be much of a lookup then....
     * This method never returns null
     *
     * @param table       Name of the table
     * @param keyColumn   Key column to provide the lookups for
     * @param dataColumns Comma separated list of columns to return
     * @return Map of values keyed on the keyColumn
     */
    public Map<String, Map<String, Object>> getLookup(String table, String keyColumn, String dataColumns) {
        return getLookup(table, keyColumn, dataColumns, false);
    }

    /**
     * This method is a convenience function for retrieving a map of values from the
     * database, keyed on the key column
     * The nature of this function is such that it doesn't make sense to give it
     * a keyColumn that isn't unique - wouldn't be much of a lookup then....
     * This method never returns null
     *
     * @param select    Select statement to use
     * @param keyColumn Key column to provide the lookups for
     * @return Map of values keyed on the keyColumn
     */
    public Map<String, Map<String, Object>> getLookup(String select, String keyColumn) {
        return getLookup(select, keyColumn, false);
    }

    /**
     * This method is a convenience function for retrieving a map of values from the
     * database, keyed on the key column
     * This function will create a composite key joined by commas
     * based on a list of keys that should ensure the uniqueness property
     *
     * @param table       Name of the table
     * @param keyColumns  List of keys to provide the lookups for
     * @param dataColumns Comma separated list of columns to return
     * @return Map of values keyed on the keyColumns joined by commas
     */
    public Map<String, Map<String, Object>> getLookup(String table, List<String> keyColumns, String dataColumns) {
        return getLookup(table, keyColumns, dataColumns, false);
    }

    /**
     * This method is a convenience function for retrieving a map of values from the
     * database, keyed on the key column
     * This function will create a composite key joined by commas
     * based on a list of keys that should ensure the uniqueness property
     *
     * @param select     Select statement to use
     * @param keyColumns List of keys to provide the lookups for
     * @return Map of values keyed on the keyColumns joined by commas
     */
    public Map<String, Map<String, Object>> getLookup(String select, List<String> keyColumns) {
        return getLookup(select, keyColumns, false);
    }

    /**
     * This method is a convenience function for retrieving a map of values from the
     * database, keyed on the key column
     * The nature of this function is such that it doesn't make sense to give it
     * a keyColumn that isn't unique - wouldn't be much of a lookup then....
     * This method never returns null
     *
     * @param table       Name of the table
     * @param keyColumn   Key column to provide the lookups for
     * @param dataColumns Comma separated list of columns to return
     * @param caseSensitive True if the keys are case sensitive (default false)
     * @return Map of values keyed on the keyColumn
     */
    public Map<String, Map<String, Object>> getLookup(String table, String keyColumn, String dataColumns, boolean caseSensitive) {
        List<String> keyColumns = new ArrayList<>();
        keyColumns.add(keyColumn);
        return getLookup(table, keyColumns, dataColumns, caseSensitive);
    }

    /**
     * This method is a convenience function for retrieving a map of values from the
     * database, keyed on the key column
     * The nature of this function is such that it doesn't make sense to give it
     * a keyColumn that isn't unique - wouldn't be much of a lookup then....
     * This method never returns null
     *
     * @param select    Select statement to use
     * @param keyColumn Key column to provide the lookups for
     * @param caseSensitive True if the keys are case sensitive (default false)
     * @return Map of values keyed on the keyColumn
     */
    public Map<String, Map<String, Object>> getLookup(String select, String keyColumn, boolean caseSensitive) {
        List<String> keyColumns = new ArrayList<>();
        keyColumns.add(keyColumn);
        return getLookup(select, keyColumns, caseSensitive);
    }

    /**
     * This method is a convenience function for retrieving a map of values from the
     * database, keyed on the key column
     * This function will create a composite key joined by commas
     * based on a list of keys that should ensure the uniqueness property
     *
     * @param table       Name of the table
     * @param keyColumns  List of keys to provide the lookups for
     * @param dataColumns Comma separated list of columns to return
     * @param caseSensitive True if the keys are case sensitive (default false)
     * @return Map of values keyed on the keyColumns joined by commas
     */
    public Map<String, Map<String, Object>> getLookup(String table, List<String> keyColumns, String dataColumns, boolean caseSensitive) {

        Map<String, Map<String, Object>> returnValue = new LinkedCaseInsensitiveMap<>();

        if (!Common.isBlank(keyColumns) && !Common.isBlank(table)) {

            // Construct a suitable SQL select

            returnValue = getLookup("select " + Common.join(keyColumns) + ',' + dataColumns + " from " + table + " group by " + Common.join(keyColumns) + ',' + dataColumns, keyColumns, caseSensitive);
        }
        return returnValue;
    }

    /**
     * This method is a convenience function for retrieving a map of values from the
     * database, keyed on the key column
     * This function will create a composite key joined by commas
     * based on a list of keys that should ensure the uniqueness property
     *
     * @param select     Select statement to use
     * @param keyColumns List of keys to provide the lookups for
     * @param caseSensitive True if the keys are case sensitive (default false)
     * @return Map of values keyed on the keyColumns joined by commas
     */
    public Map<String, Map<String, Object>> getLookup(String select, List<String> keyColumns, boolean caseSensitive) {

        Map<String, Map<String, Object>> returnValue = caseSensitive?new HashMap<String, Map<String, Object>>():new LinkedCaseInsensitiveMap<Map<String, Object>>();

        if (!Common.isBlank(keyColumns) && !Common.isBlank(select)) {

            // Construct a suitable SQL query

            List<Map<String, Object>> results = find(select, true);
            if (!Common.isBlank(results)) {

                // Turn the list into a map keyed on the key column value

                StringBuilder key;
                for (Map<String, Object> row : results) {

                    key = new StringBuilder();
                    for (String column : keyColumns) {
                        key.append(DataSourceUtils.getValueOf(row.get(column))).append(',');
                    }
                    returnValue.put(key.deleteCharAt(key.length() - 1).toString(), row);
                }
            }
        }
        return returnValue;
    }

    /**
     * This is a useful method for creating an IN clause from a list of results
     *
     * @param list       List of maps as would have produced by find/findFirst
     * @param columnName Name of the column to get values for
     * @return String in the form "xx,yy,yy"
     */
    public static String getInClauseFromResults(List<Map<String, Object>> list, String columnName) {

        String returnValue = "";
        if (Common.isBlank(list))
            logger.warn("The list for getInClauseFromResults is empty");
        else {

            // Set the name of the column if not specified

            if (Common.isBlank(columnName))
                columnName = list.get(0).keySet().toArray(new String[list.get(0).keySet().size()])[0];

            // Loop through all the rows getting the specific column values

            List<String> values = new ArrayList<>();
            for (Map<String, Object> row : list) {
                values.add(DataSourceUtils.getValueOf(row.get(columnName), true));
            }
            returnValue = Common.join(values);
        }
        return returnValue;
    }

    /**
     * Convenience method for returns the contents of a row values
     * map as a string
     *
     * @param rowValues Map of field values keyed on the column name
     * @return String of comma separated couplets e.g. column=value,column2=value2
     */
    private static String Z_GetRowValues(Map<String, Object> rowValues) {
        List<String> returnValue = new ArrayList<>();
        for (Map.Entry entry : rowValues.entrySet()) {
            returnValue.add((String) entry.getKey() + '=' + entry.getValue());
        }
        return Common.join(returnValue);
    }

    /**
     * Saves the result of the select statement to a VFS file
     *
     * @param select      Select statement to return rows
     * @param vfsFilename VFS file location to put results
     * @param format      Format of the output
     * @param username    Optional username to use
     * @param password    Optional password to use
     * @return True if the save worked OK
     */
    public boolean saveAs(String select, String vfsFilename, String format, String username, String password) {
        return saveAs(select, vfsFilename, format, username, password, null, false);
    }

    /**
     * Saves the result of the select statement to a VFS file
     *
     * @param select      Select statement to return rows
     * @param vfsFilename VFS file location to put results
     * @param format      Format of the output
     * @return True if the save worked OK
     */
    public boolean saveAs(String select, String vfsFilename, String format) {
        return saveAs(select, vfsFilename, format, null, null, null, false);
    }

    /**
     * Saves the result of the select statement to a VFS file
     *
     * @param select      Select statement to return rows
     * @param vfsFilename VFS file location to put results
     * @param format      Format of the output
     * @param compression Compression algorithm to use
     * @return True if the save worked OK
     */
    public boolean saveAs(String select, String vfsFilename, String format, String compression) {
        return saveAs(select, vfsFilename, format, null, null, compression, false);
    }

    /**
     * Saves the result of the select statement to a VFS file
     *
     * @param select        Select statement to return rows
     * @param vfsFilename   VFS file location to put results
     * @param format        Format of the output
     * @param username      Optional username to use
     * @param password      Optional password to use
     * @param compression   Compression algorithm to use
     * @param userDirIsRoot True if the landing directory is to be the root of the file
     * @return True if the save worked OK
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public boolean saveAs(String select, String vfsFilename, String format, String username, String password, String compression, boolean userDirIsRoot) {

        boolean returnValue = false;
        lastError = null;
        SQLOutput dumpFile = null;
        String tmpFile = null;
        FileObject dest;
        FileSystemManager fsManager = null;

        // Make sure that we have useful parameters

        if (Common.isBlank(select)) {
            setError("A select query must be specified");
        }
        else if (Common.isBlank(vfsFilename)) {
            setError("A VFS file destination must be specified");
        }
        else if (Common.isBlank(format)) {
            setError("A file export format must be specified");
        }
        else if (!Common.doStringsMatch(format, Report.ExportFormat.EXCEL2010.toString(),
                                        Report.ExportFormat.EXCEL97.toString(),
                                        Report.ExportFormat.CSV.toString(),
                                        Report.ExportFormat.TSV.toString())) {
            setError("The export format is not supported");
        }
        else {

            // Find the results

            try {
                getConnection();
                List<Map<String, Object>> results = find(select, true);
                if (Common.isBlank(lastError) && !Common.isBlank(results)) {
                    logger.debug("Found {} results", results.size());

                    // Create a temporary file to use

                    tmpFile = Common.getTemporaryFilename(Common.getFilenameExtension(vfsFilename));

                    // Send the results to the appropriate destination

                    dumpFile = Z_SelectDumpFile(tmpFile, Report.ExportFormat.getType(format), compression, true);
                    if (dumpFile != null) {
                        for (Map<String, Object> rowValues : results) {
                            dumpFile.addRow(rowValues);
                        }
                        dumpFile.close();
                    }
                    dumpFile = null;

                    // Connect to the server

                    fsManager = VFSUtils.getManager();
                    FileSystemOptions opts = VFSUtils.setUpConnection(username, password, userDirIsRoot);

                    // Copy the file

                    logger.debug("Attempting to copy [{}] to [{}]", tmpFile, vfsFilename);
                    FileObject file = fsManager.resolveFile(tmpFile);
                    dest = fsManager.resolveFile(vfsFilename, opts);
                    dest.copyFrom(file, Selectors.SELECT_ALL);
                    logger.debug("Copied file [{}] to [{}]", file.getName(), vfsFilename);
                    returnValue = true;
                }
            }
            catch (Exception e) {
                setError("Problem copying the VFS file [" + vfsFilename + "] - " + PivotalException.getErrorMessage(e));
            }
            finally {
                if (dumpFile != null) dumpFile.close();
                if (tmpFile != null && new File(tmpFile).exists()) new File(tmpFile).delete();

                // close connection after copying the file
                VFSUtils.closeManager(fsManager);
            }
        }

        return returnValue;
    }

    /**
     * Selects the appropriate dump file type to use
     *
     * @param filename    File to read from
     * @param format      Format to use
     * @param compression True if we need compression
     * @param noHeader    True if we want to omit the header
     * @return dump file to use
     */
    private SQLOutput Z_SelectDumpFile(String filename, Report.ExportFormat format, String compression, boolean noHeader) {

        SQLOutput dumpFile = null;
        if (format.equals(Report.ExportFormat.EXCEL97) || format.equals(Report.ExportFormat.EXCEL2010)) {
            dumpFile = new ExcelOutput(filename, compression, noHeader);
            logger.debug("Outputting to Excel file {}", filename);
        }
        else if (format.equals(Report.ExportFormat.CSV)) {
            dumpFile = new TextOutput(filename, ",", compression, noHeader);
            logger.debug("Outputting to comma delimited file {}", filename);
        }
        else if (format.equals(Report.ExportFormat.TSV)) {
            dumpFile = new TextOutput(filename, "\t", compression, noHeader);
            logger.debug("Outputting to tab delimited file {}", filename);
        }
        else
            setError("Unsupported export format " + format);

        return dumpFile;
    }

    /**
     * Clears any errors
     */
    public void clearErrors() {
        lastError = null;
        lastWarning = null;
    }

    /**
     * Gets a real connection from the pool if we have a future
     * and we don't already have a connection
     */
    private void getConnection() {
        if (dbConnection == null && useFutureConnection) {
            try {
                dbConnection = DataSourceUtils.getConnection(dataSrc);
            }
            catch (Exception e) {
                logger.error("Problem getting database connection for [{}] - {}", getName(), PivotalException.getErrorMessage(e));
            }
        }
    }

    /**
     * A timer that will flush the buffer on the response stream
     * This is useful if we want hold back sending the headers until
     * the last moment but cannot fall foul of the browser timeout
     */
    private static class ResponseFlush extends TimerTask {

        HttpServletResponse response;

        /**
         * Creates a flush timer that
         *
         * @param response Response to flush
         */
        ResponseFlush(HttpServletResponse response) {
            this.response = response;
        }

        /**
         * The action to be performed by this timer task.
         */
        @Override
        public void run() {
            if (response != null) {
                try {
                    response.flushBuffer();
                }
                catch (Exception e) {
                    logger.debug("Problem flushing the header - ", PivotalException.getErrorMessage(e));
                }
            }
        }
    }

    /**
     * Gets the name associated with this database
     *
     * @return Name of the source
     */
    public String getName() {
        return name;
    }
}
