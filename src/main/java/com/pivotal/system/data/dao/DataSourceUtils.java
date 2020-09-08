/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.data.dao;


import com.pivotal.api.PoolBroker;
import com.pivotal.system.hibernate.entities.DatasourceEntity;
import com.pivotal.system.hibernate.utils.AppDataSource;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.*;
import com.pivotal.web.Constants;
import com.pivotal.web.servlet.Initialisation;
import com.pivotal.web.servlet.ServletHelper;
import org.springframework.util.LinkedCaseInsensitiveMap;

import javax.sql.DataSource;
import java.io.*;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.pivotal.utils.Common.readTextFile;

/**
 * Provides useful data source related utilities
 */
public class DataSourceUtils {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DataSourceUtils.class);
    public static final int LOGIN_TIMEOUT_SECS = 30;
    private static final String LOOKUP_PREFIX = "<LOOKUP|";
    private static final String LOOKUP_SUFFIX = "|>";
    private static final String LOOKUP_DIVIDER = "|";
    private static final String LOOKUP_ROW_IDENTIFIER = "LOOKUP_ROW_IDENTIFIER_NAME";

    /**
     * Returns a connection to use for the given datasource entity.
     * If the datasource entity is set to user pools, the connection will be taken from a pool instead of being manually created.
     *
     * @param datasource Source details to use
     *
     * @return Connection object
     *
     * @throws SQLException If it cannot connect to the database
     */
    public static Connection getConnection(DatasourceEntity datasource) throws SQLException {
        Connection objDB;
        logger.debug("Getting a connection for datasource [{}]", datasource.getName());
        if (datasource.isUseConnectionPool()) {
            objDB = PoolBroker.getInstance().getConnection(datasource);
        }
        else {
            objDB = createNewConnection(datasource.getDatabaseUrl(), datasource.getDriver(), datasource.getUsername(), datasource.getPassword());
        }
        logger.debug("Connection fetched for [{}]", datasource.getName());
        return objDB;
    }

    /**
     * Returns a connection to use for the given connection attributes
     *
     * @param url      JDBC Connection string
     * @param driver   Driver class to use
     * @param username Username
     * @param password Password
     *
     * @return Connection object
     *
     * @throws SQLException If it cannot connect to the database
     */
    public static Connection createNewConnection(String url, String driver, String username, String password) throws SQLException {
        return createNewConnection(url, driver, username, password, LOGIN_TIMEOUT_SECS);
    }

    /**
     * Returns a connection to use for the given connection attributes
     *
     * @param url      JDBC Connection string
     * @param driver   Driver class to use
     * @param username Username
     * @param password Password
     * @param timeout  Login timeout in seconds
     *
     * @return Connection object
     *
     * @throws SQLException If it cannot connect to the database
     */
    public static Connection createNewConnection(String url, String driver, String username, String password, int timeout) throws SQLException {
        Connection objDB;
        logger.debug("Creating connection with parameters, class:{} URL:{} Username:{} Password:*", driver, url, username);
        try {
            Class.forName(driver);
            logger.debug("Database driver loaded OK - attempting connection");
            DriverManager.setLoginTimeout(timeout);
            objDB = DriverManager.getConnection(url, username, password);
            try {
                objDB.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            }
            catch (Exception e) {
                logger.debug("Problem setting the default transaction isolation level to read committed - {}", PivotalException.getErrorMessage(e));
            }
        }
        catch (ClassNotFoundException e) {
            throw new PivotalException("Cannot load driver [" + driver + "] " + PivotalException.getErrorMessage(e));
        }
        logger.debug("Connection Successfully Created");
        return objDB;
    }

    /**
     * Executes a query on the database and return a results set if the
     * query has vales, otherwise it returns null
     *
     * @param db  Database connection to use
     * @param sql SQL Select statement
     *
     * @throws SQLException if command fails
     */
    public static void executeCommand(Connection db, String sql) throws SQLException {

        ResultSet results = null;
        Statement statement = null;
        logger.debug("Running statement [{}]", sql);
        try {
            statement = db.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            if (statement.execute(sql)) {
                results = statement.getResultSet();
            }
        }
        catch (Exception e) {
            String tmp = sql;
            if (sql.length() > 200) tmp = new String(tmp.substring(0, 200)) + "....";
            logger.debug("Problem running command [{}] - {}", tmp, PivotalException.getErrorMessage(e));
            throw e;
        }
        finally {
            Common.close(statement);
        }
        logger.debug("Statement returned {} results", results == null ? "no" : "some");
    }


    /**
     * Executes a query on the database and return a results set if the
     * query has vales, otherwise it returns null
     *
     * @param db  Database connection to use
     * @param sql SQL Select statement
     *
     * @return ResultSet or null if nothing found
     * @throws SQLException if command fails
     */
    public static ResultSet executeQuery(Connection db, String sql) throws SQLException {

        ResultSet results = null;
        Statement statement;
        logger.debug("Running statement [{}]", sql);
        try {
            statement = db.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            if (statement.execute(sql)) {
                results = statement.getResultSet();
            }
        }
        catch (SQLException e) {
            String tmp = sql;
            if (sql.length() > 200) tmp = new String(tmp.substring(0, 200)) + "....";
            logger.debug("Problem running query [{}] - {}", tmp, PivotalException.getErrorMessage(e));
            throw e;
        }
        logger.debug("Statement returned {} results", results == null ? "no" : "some");
        return results;
    }

    /**
     * Executes a update query or any DML query on the database and returns an update count
     *
     * @param db   Database connection to use
     * @param sSQL SQL Select statement
     *
     * @return int  Returns the update count
     *
     * @throws java.sql.SQLException If problem running the command
     */
    public static int executeUpdateQuery(Connection db, String sSQL) throws SQLException {

        Statement statement = null;
        int updateCount = 0;
        logger.debug("Running statement [{}]", sSQL);
        try {
            statement = db.createStatement();
            statement.setEscapeProcessing(false);
            updateCount = statement.executeUpdate(sSQL);
        }
        catch (SQLException e) {
            String tmp = sSQL;
            if (sSQL.length() > 500) tmp = new String(tmp.substring(0, 500)) + "....";
            logger.error("Problem running update query [{}] - {}", tmp, PivotalException.getErrorMessage(e));
            throw e;
        }
        finally {
            Common.close(statement);
        }
        logger.debug("Update Statement returned {} update counts", updateCount == 0 ? "no" : updateCount);
        return updateCount;
    }


    /**
     * Executes a prepared statement query on the database and return a results set if the
     * query has vales, otherwise it returns null
     *
     * @param db     Database connection to use
     * @param sSQL   SQL Select statement
     * @param params Map of parameter values
     */
    public static void executeCommand(Connection db, String sSQL, Map<Integer, Object> params) {

        ResultSet results = null;
        PreparedStatement statement = null;
        logger.debug("Running statement [{}]", sSQL);
        try {
            statement = db.prepareStatement(sSQL, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            for (int paramIndex : params.keySet()) {
                statement.setObject(paramIndex, params.get(paramIndex));
            }

            if (statement.execute(sSQL))
                results = statement.getResultSet();
        }
        catch (Exception e) {
            throw new PivotalException("Problem executing query [" + sSQL + "] - %s", PivotalException.getErrorMessage(e));
        }
        finally {
            Common.close(statement);
        }
        logger.debug("Search returned {} results", results == null ? "no" : "some");
    }

    /**
     * This method executes the query on the specified datasource and
     * returns the results as a list of column values (as strings)
     * This routine makes use of it's own database connection that it
     * cleans up after it has completed
     *
     * @param datasource Source details to use
     * @param sql        SQL Select statement
     * @param max        Maximum number of results to return
     *
     * @return List of lists of strings
     */
    public static List<LinkedHashMap<String, String>> getResults(DatasourceEntity datasource, String sql, int max) {
        List<LinkedHashMap<String, String>> list = null;
        Connection db = null;
        PreparedStatement ps = null;
        ResultSet results = null;
        logger.debug("Running statement [{}]", sql);
        try {

            // Get a connection to work with

            db = getConnection(datasource);

            // Run the query
            ps = db.prepareStatement(sql);

            results = ps.executeQuery();

            // Get the results if there are any

            if (results != null) {
                ResultSetMetaData meta = results.getMetaData();
                list = new ArrayList<>();
                while (results.next() && (list.size() < max || max < 1)) {
                    LinkedHashMap<String, String> row = new LinkedHashMap<>();
                    for (int iCnt = 1; iCnt <= meta.getColumnCount(); iCnt++) {
                        Object value = results.getObject(iCnt);
                        String label = meta.getColumnLabel(iCnt);
                        if (Common.isBlank(label))
                            label = meta.getColumnName(iCnt);
                        else if (Common.doStringsMatch(label, "?column?"))
                            label = "column" + iCnt;
                        row.put(label, value == null ? "" : value.toString());
                    }
                    list.add(row);
                }

                // Show a warning if we have reached the limit

                if (list.size() == max && !results.isLast() && max > 0)
                    logger.warn("The maximum number of results [{}] has been reached for [{}] on data source [{}]", max, sql, datasource.getName());
            }
        }
        catch (Exception e) {
            throw new PivotalException("Problem getting results [%s] - %s", sql, PivotalException.getErrorMessage(e));
        }
        finally {
            try {
                if (!Common.isBlank(results) && !results.isClosed()) {
                    results.close();
                }
            }
            catch (SQLException e) {
                logger.error("Could not close Result Set", e);
            }
            try {
                if (!Common.isBlank(ps) && !ps.isClosed()) {
                    ps.close();
                }
            }
            catch (SQLException e) {
                logger.error("Could not close Prepared Statement", e);
            }
            Common.close(db);
        }
        logger.debug("Search returned {} results", results == null ? "no" : "some");
        return list;
    }

    /**
     * Finds the column within the table that matches the column name
     *
     * @param meta   Table metadata
     * @param column Column name
     *
     * @return Column ID
     *
     * @throws java.sql.SQLException Errors
     */
    public static int findColumn(ResultSetMetaData meta, String column) throws SQLException {
        int returnValue = -1;
        for (int i = 1; i <= meta.getColumnCount() && returnValue == -1; i++) {
            if (meta.getColumnName(i).equals(column))
                returnValue = i;
        }
        return returnValue;
    }


    /**
     * This is a useful function that returns an object in it's
     * unambiguous string form
     *
     * @param value Object value
     *
     * @return String version
     */
    public static String getValueOf(Object value) {
        return getValueOf(value, false);
    }

    /**
     * This is a useful function that returns an object in it's
     * unambiguous string form
     *
     * @param value      Object value
     * @param withQuotes True if string type values should be inclosed in single quotes
     *
     * @return String version
     */
    public static String getValueOf(Object value, boolean withQuotes) {
        return getValueOf(value, withQuotes, null);
    }

    /**
     * This is a useful function that returns an object in it's
     * unambiguous string form
     *
     * @param value         Object value
     * @param withQuotes    True if string type values should be enclosed in single quotes
     * @param numericFormat Optional format for floating point numbers
     *
     * @return String version
     */
    public static String getValueOf(Object value, boolean withQuotes, String numericFormat) {

        String returnValue;

        // If its nul then just return it

        if (value == null)
            returnValue = null;

            // If it's a timestamp

        else if (value instanceof Timestamp)
            returnValue = value.toString();

            // If it's a date then return the non-ambiguous form

        else if (value instanceof java.sql.Time)
            returnValue = withQuotes ? Common.dateFormat((Date) value, "'HH:mm:ss'") : Common.dateFormat((Date) value, "HH:mm:ss");

            // If it's a date then return the non-ambiguous form

        else if (value instanceof Date)
            returnValue = withQuotes ? Common.dateFormat((Date) value, "'yyyy-MM-dd HH:mm:ss'") : Common.dateFormat((Date) value, "yyyy-MM-dd HH:mm:ss");

            // If it's a whole number type

        else if (value instanceof Integer ||
                value instanceof Long ||
                value instanceof Short ||
                value instanceof Byte)
            returnValue = value.toString();

            // If it's a numeric type and not a whole number then it's
            // likely some sort of real number

        else if (value instanceof Number)
            returnValue = Common.formatNumber((Number) value, Common.isBlank(numericFormat) ? "0.0#######" : numericFormat);

            // If it's boolean then a 0 or 1 will suffice

        else if (value instanceof Boolean)
            returnValue = (Boolean) value ? "1" : "0";

            // If it's a byte array then return its length

        else if (value instanceof byte[])
            returnValue = "" + ((byte[]) value).length;

            // Everything else is treated as a string

        else
            returnValue = withQuotes ? '\'' + (String) value + '\'' : (String) value;

        return returnValue;
    }

    /**
     * This is a useful function that creates a copy of the specified object
     *
     * @param value Object value
     *
     * @return Copy of the object
     */
    public static Object cloneValueOf(Object value) {

        Object returnValue;

        // If its nul then just return it

        if (value == null)
            returnValue = null;

            // If it's a String

        else if (value instanceof Integer)
            returnValue = value;

            // If it's a String

        else if (value instanceof String)
            returnValue = value;

            // If it's a date then return the non-ambiguous form

        else if (value instanceof java.sql.Date)
            returnValue = ((java.sql.Date) value).clone();

        else if (value instanceof java.sql.Time)
            returnValue = ((java.sql.Time) value).clone();

            // If it's a timestamp

        else if (value instanceof Timestamp)
            returnValue = ((Timestamp) value).clone();

        else if (value instanceof Date)
            returnValue = ((Date) value).clone();

            // If it's a byte array

        else if (value instanceof byte[])
            returnValue = Arrays.copyOf((byte[]) value, ((byte[]) value).length);

            // Everything else is treated as an immutable type

        else
            returnValue = value;

        return returnValue;
    }

    /**
     * This helper method receives an Object and inserts into a PreparedStatement,
     * according to the type of the column.
     * It needs the position of the field in the statement and the respective type.
     *
     * @param vo        The Object to be set into the PreparedStatement
     * @param columnPos The position of the Object in the PreparedStatement
     * @param type      The SQL type of the Object
     * @param stmt      The PreparedStatement to be updated
     *
     * @throws java.sql.SQLException When unable to set the value correctly
     */
    public static void setPreparedStatementValue(Object vo, int columnPos, int type, PreparedStatement stmt) throws SQLException {
        updateStmtOrTable(vo, columnPos, type, stmt, null);
    }

    /**
     * This helper method receives an Object and inserts into an updatable ResultSet,
     * according to the type of the column.
     * It needs the position of the field in the ResultSet and the respective type.
     * It assumes the ResultSet is updatable and the cursor is in the InsertRow position.
     *
     * @param vo        The Object to be set into the PreparedStatement
     * @param columnPos The position of the Object in the PreparedStatement
     * @param type      The SQL type of the Object
     * @param table     The ResultSet to be updated
     *
     * @throws java.sql.SQLException When unable to set the value correctly
     */
    public static void setResultSetValue(Object vo, int columnPos, int type, ResultSet table) throws SQLException {
        updateStmtOrTable(vo, columnPos, type, null, table);
    }

    /**
     * This helper method receives an Object and inserts it into either an updatable ResultSet
     * or a Prepared Statement.
     * It needs the position of the field in the statement (or ResultSet) and the respective type.
     * It assumes the ResultSet is updatable and the cursor is in the InsertRow position.
     *
     * @param vo        The Object to be set into either PreparedStatement or Result Set
     * @param columnPos The position of the Object in either PreparedStatement or Result Set
     * @param type      The SQL type of the Object
     * @param stmt      The PreparedStatement to be updated
     * @param table     The ResultSet to be updated
     *
     * @throws SQLException When unable to set the value correctly
     */
    private static void updateStmtOrTable(Object vo, int columnPos, int type, PreparedStatement stmt, ResultSet table) throws SQLException {

        //Don't run getValueOf for Blob cases.
        //String value= type != Types.LONGVARBINARY ? getValueOf(vo) : "";
        String value = getValueOf(vo);

        // We treat empty strings as NULL whatever the destination type

        if (vo instanceof String && Common.isBlank(value)) value = null;

        if (value == null) {
            if (stmt != null) stmt.setNull(columnPos, type);
            if (table != null) table.updateNull(columnPos);
        }
        else {
            switch (type) {
                case Types.BOOLEAN:
                case Types.BIT:
                    boolean boolValue = Common.isYes(value);
                    if (stmt != null) stmt.setBoolean(columnPos, boolValue);
                    if (table != null) table.updateBoolean(columnPos, boolValue);
                    break;

                case Types.DATE:
                    if (Common.parseDate(value) == null) {
                        if (stmt != null) stmt.setString(columnPos, value);
                        if (table != null) table.updateString(columnPos, value);
                    }
                    else {
                        java.sql.Date dateValue = new java.sql.Date(Common.parseDate(value).getTime());
                        if (stmt != null) stmt.setDate(columnPos, dateValue);
                        if (table != null) table.updateDate(columnPos, dateValue);
                    }
                    break;

                case Types.TIMESTAMP:
                    // Parse the data once and reuse the value
                    Date parseDate = Common.parseDateTime(value);
                    if (parseDate != null) {
                        Timestamp timestampValue = new Timestamp(parseDate.getTime());
                        if (stmt != null) stmt.setTimestamp(columnPos, timestampValue);
                        if (table != null)
                            table.updateTimestamp(columnPos, new Timestamp(Common.parseDateTime(value).getTime()));
                    }
                    else {
                        // Parse the data once and reuse the value
                        parseDate = Common.parseDate(value);
                        if (parseDate != null) {
                            java.sql.Date dateValue = new java.sql.Date(parseDate.getTime());
                            if (stmt != null) stmt.setDate(columnPos, dateValue);
                            if (table != null) table.updateDate(columnPos, dateValue);
                        }
                        else {
                            if (stmt != null) stmt.setString(columnPos, value);
                            if (table != null) table.updateString(columnPos, value);
                        }
                    }
                    break;

                case Types.DOUBLE:
                case Types.FLOAT:
                    Double doubleValue = Common.parseDouble(value);
                    if (stmt != null) stmt.setDouble(columnPos, doubleValue);
                    if (table != null) table.updateDouble(columnPos, doubleValue);
                    break;

                case Types.SMALLINT:
                    short shortValue = Common.parseShort(value);
                    if (stmt != null) stmt.setShort(columnPos, shortValue);
                    if (table != null) table.updateShort(columnPos, shortValue);
                    break;
                case Types.INTEGER:
                case Types.NUMERIC:
                    int intValue = Common.parseInt(value);
                    if (stmt != null) stmt.setInt(columnPos, intValue);
                    if (table != null) table.updateInt(columnPos, intValue);
                    break;
                case Types.LONGVARBINARY:
                    int size = Common.parseInt(value);
                    //Use the default update
                    if (vo instanceof String) {
                        if (stmt != null) stmt.setString(columnPos, value);
                        if (table != null) table.updateString(columnPos, value);
                    }
                    else if (vo instanceof byte[]) {
                        byte[] byteArrayValue = (byte[]) vo;
                        InputStream lobStream = new ByteArrayInputStream(byteArrayValue);
                        if (stmt != null) stmt.setBlob(columnPos, lobStream, size);
                        if (table != null) table.updateBinaryStream(columnPos, lobStream, size);
                    }
                    break;
                default:
                    if (stmt != null) stmt.setString(columnPos, value);
                    if (table != null) table.updateString(columnPos, value);
            }
        }
    }

    /**
     * This method dumps out to the writer the values for the current results set row
     * The format is the same as that provided by MySQLDump except for BLOBs
     * that are turned into hex value to avoid encoding problems.
     *
     * @param results Results set to read
     * @param table   Table to dump
     * @param out     Writer to send the data to
     *
     * @throws java.lang.Exception Errors if there is a problem
     */
    private static void dumpRow(ResultSet results, ResultSetMetaData meta, String table, Map<String, String> columns, Map<String, String> columnDefs, OutputStreamWriter out) throws Exception {

        // Check we haven't been sent rubbish

        if (results != null && !results.isAfterLast()) {

            // Now the values

            String value;
            Date dateValue;
            char currentChar;
            for (int col = 1; col <= meta.getColumnCount(); col++) {
                String colName = meta.getColumnName(col);
                String colDef = columnDefs.get(colName);
                switch (meta.getColumnType(col)) {

                    // Look for the character strings

                    case Types.CHAR:
                    case Types.NCHAR:
                        value = results.getString(col);
                        if (value == null) {
                            if (colDef == null) {
                                columns.remove(colName);
                            }
                        }
                        else
                            columns.put(colName, '\'' + value.replaceAll("\\\\", "\\\\\\\\").replaceAll("'", "\\\\'") + '\'');
                        break;

                    case Types.DATE:
                        dateValue = results.getDate(col);
                        if (dateValue == null) {
                            if (colDef == null) {
                                columns.remove(colName);
                            }
                        }
                        else
                            columns.put(colName, '\'' + Common.dateFormat(dateValue, "yyyy-MM-dd") + '\'');
                        break;

                    case Types.BOOLEAN:
                    case Types.BIT:
                        if (results.getBoolean(col) == Common.parseBoolean(colDef))
                            columns.remove(colName);
                        else
                            columns.put(colName, results.getBoolean(col) ? "true" : "false");
                        break;

                    case Types.TIME:
                        dateValue = results.getTime(col);
                        if (dateValue == null) {
                            if (colDef == null) {
                                columns.remove(colName);
                            }
                        }
                        else
                            columns.put(colName, '\'' + Common.dateFormat(dateValue, "HH:mm:ss") + '\'');
                        break;

                    case Types.TIMESTAMP:
                        dateValue = results.getTimestamp(col);
                        if (dateValue == null) {
                            if (colDef == null) {
                                columns.remove(colName);
                            }
                        }
                        else
                            columns.put(colName, '\'' + Common.dateFormat(dateValue, "yyyy-MM-dd HH:mm:ss") + '\'');
                        break;

                    // BLOBs need special handling

                    case Types.NVARCHAR:
                    case Types.VARCHAR:
                    case Types.LONGNVARCHAR:
                    case Types.LONGVARCHAR:
                    case Types.CLOB:
                        String valueString = results.getString(col);
                        if (valueString == null) {
                            if (colDef == null) {
                                columns.remove(colName);
                            }
                        }
                        else {
                            StringBuilder tmp = new StringBuilder();
                            tmp.append("'");

                            // Handle the special characters to create a binary string

                            for (int pos = 0; pos < valueString.length(); pos++) {
                                currentChar = valueString.charAt(pos);
                                switch (currentChar) {
                                    case 0:
                                        tmp.append("\\0");
                                        break;

                                    case '\n':
                                        tmp.append("\\n");
                                        break;

                                    case '\r':
                                        tmp.append("\\r");
                                        break;

                                    case 26:
                                        tmp.append("\\Z");
                                        break;

                                    case '\\':
                                    case '\'':
                                        tmp.append("\\");

                                    default:
                                        tmp.append(currentChar);
                                }
                            }
                            tmp.append("'");
                            columns.put(colName, tmp.toString());
                        }
                        break;

                    case Types.LONGVARBINARY:
                    case Types.VARBINARY:
                    case Types.BINARY:
                    case Types.BLOB:
                        byte[] valueBytes = results.getBytes(col);
                        if (valueBytes == null)
                            columns.remove(colName);
                        else {
                            // Convert binary values to Hex so we won't have problems with character encoding

                            StringBuilder tmp = new StringBuilder();
                            tmp.append("X'");
                            for (byte byteChar : valueBytes) {
                                int unsigned = byteChar;

                                // by default, for byte type, values from 128 to 255 are seen as negative values

                                if (unsigned < 0) {
                                    unsigned = 256 + byteChar;
                                }

                                // convert char to hex

                                tmp.append(Common.padLeft(Integer.toHexString(unsigned), "0", 2));
                            }
                            tmp.append("'");
                            columns.put(colName, tmp.toString());
                        }

                        break;

                    // Assume everything else is numeric

                    default:
                        value = results.getString(col);
                        if (value == null) {
                            if (colDef == null) {
                                columns.remove(colName);
                            }
                        }
                        else if (colDef != null && Common.parseFloat(value) == Common.parseFloat(colDef)) {
                            columns.remove(colName);
                        }
                        else {
                            if (value.matches(".+0000000000[0-9]$")) {
                                value = Common.parseDouble(value.replaceAll("0000000000[0-9]$", "")).toString();
                            }
                            else if (value.matches(".+09999999999999$")) {
                                value = Common.parseDouble(value.replaceAll("09999999999999$", "")).toString();
                            }
                            columns.put(colName, value);
                        }
                        break;
                }
            }
            // Output the columns that have values

            if (!Common.isBlank(columns)) {
                out.write("insert into " + table + " (");
                out.write(Common.join(columns.keySet(), ","));
                out.write(") values (");
                out.write(Common.join(columns.values(), ","));
                out.write(");\n");
            }
        }
    }

    /**
     * Dumps the rows for this table as INSERT statements
     *
     * @param connection    Connection to use
     * @param table         Table to dump
     * @param out           Destination for the insert statements
     * @param deleteAllRows True if the table is to be cleared before loading
     * @param progress      Progress indicator
     *
     * @throws java.lang.Exception Errors if there is a problem
     */
    public static void dumpTable(Connection connection, String table, OutputStreamWriter out, boolean deleteAllRows, Progress progress) throws Exception {

        // Make sure we're valid

        if (connection != null && !connection.isClosed() && !Common.isBlank(table)) {

            logger.debug("Dumping data from table " + table);

            // Find all the rows

            progress.setMessage(table);
            Statement statement = null;
            try {
                statement = connection.createStatement();
                ResultSet results = statement.executeQuery(String.format("select * from %s", table));

                // Get a list of the columns

                ResultSetMetaData meta = results.getMetaData();
                Map<String, String> columns = new LinkedHashMap<>();
                for (int col = 1; col <= meta.getColumnCount(); col++) {
                    columns.put(meta.getColumnName(col), "null");
                }

                // Get a list of the column defaults

                Map<String, String> columnDefs = new LinkedHashMap<>();
                for (int col = 1; col <= meta.getColumnCount(); col++) {
                    ResultSet rs = connection.getMetaData().getColumns(null, null, table, meta.getColumnName(col));
                    if (rs.next()) {
                        columnDefs.put(meta.getColumnName(col), rs.getString("COLUMN_DEF"));
                    }
                    rs.close();
                }

                // Loop through all the rows

                boolean first = true;
                if (deleteAllRows) {
                    out.write("\n/* Data for the table " + table + " */\n");
                    out.write("truncate table " + table + ";\n");
                    first = false;
                }

                // Loop through all the rows

                while (results.next()) {
                    if (first) {
                        out.write("\n/* Data for the table " + table + " */\n");
                        first = false;
                    }
                    progress.setCount(progress.getCount() + 1);
                    dumpRow(results, meta, table, columns, columnDefs, out);
                }
            }
            finally {
                Common.close(statement);
            }
        }
        Common.sleep(100);
    }

    /**
     * Dumps the database to the given file
     *
     * @param connection           Database connection to use
     * @param filename             File to receive the INSERT statements
     * @param dropForeignKeychecks True if foreign key checking should be dropped
     * @param deleteAllRows        True if the table is to be cleared before loading
     * @param info                 Additional information to include in the file
     * @param progress             Progress indicator
     *
     * @throws java.lang.Exception Errors if there is a problem
     */
    public static void dumpDatabase(Connection connection, String filename, boolean dropForeignKeychecks, boolean deleteAllRows, String info, Progress progress) throws Exception {

        // Check for goofiness

        if (!Common.isBlank(filename)) {

            // Get a file writer to use

            OutputStreamWriter out = null;
            try {

                // Create the file output stream to use

                out = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(filename)), "UTF-8");

                // Dump the database to the file

                dumpDatabase(connection, out, dropForeignKeychecks, deleteAllRows, info, progress);
            }
            finally {
                Common.close(out);
            }
        }
    }

    /**
     * Dumps the database to the given file
     *
     * @param connection           Database connection to use
     * @param out                  The output stream to use
     * @param dropForeignKeychecks True if foreign key checking should be dropped
     * @param deleteAllRows        True if the table is to be cleared before loading
     * @param info                 Additional information to include in the file
     * @param progress             Progress indicator
     *
     * @throws java.lang.Exception Errors if there is a problem
     */
    public static void dumpDatabase(Connection connection, OutputStreamWriter out, boolean dropForeignKeychecks, boolean deleteAllRows, String info, Progress progress) throws Exception {
        dumpDatabase(connection, out, dropForeignKeychecks, deleteAllRows, info, progress, null);
    }

    /**
     * Dumps the database to the given file
     *
     * @param connection           Database connection to use
     * @param out                  The output stream to use
     * @param dropForeignKeychecks True if foreign key checking should be dropped
     * @param deleteAllRows        True if the table is to be cleared before loading
     * @param info                 Additional information to include in the file
     * @param progress             Progress indicator
     * @param excludedTables       List of excluded tables
     *
     * @throws java.lang.Exception Errors if there is a problem
     */
    public static void dumpDatabase(Connection connection, OutputStreamWriter out, boolean dropForeignKeychecks, boolean deleteAllRows, String info, Progress progress, List<String> excludedTables) throws Exception {

        // Check for goofiness

        if (out != null) {

            // If we have some information to put out

            if (!Common.isBlank(info)) {
                out.write(info + '\n');
            }
            out.write("/*! set sql_safe_updates=0 */;\n");

            // If we don't want to check keys

            if (dropForeignKeychecks) {
                out.write("set foreign_key_checks=0;\n");
            }

            // Output schema if postgres

            AppDataSource dsInfo = HibernateUtils.getDataSource();
            if (dsInfo.isPostgreSQL()) {
                out.write("set schema '" + dsInfo.getSchema() + "';\n");
            }

            // Get a list of all the tables and the scope of the problem

            Set<String> tablesToExclude = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            if (!Common.isBlank(excludedTables)) {
                tablesToExclude.addAll(excludedTables);
            }
            String[] types = {"TABLE"};
            List<String> tablesToExport = new ArrayList<>();


            tablesToExport = getTables(tablesToExclude, progress);

            // Dump each of the tables

            for (String table : tablesToExport) {
                dumpTable(connection, table, out, deleteAllRows, progress);
            }

            // If we don't want to check keys

            if (dropForeignKeychecks) {
                out.write("\nset foreign_key_checks=1;\n");
            }
        }
    }

    /**
     * Dumps the current NRMM database to the given file
     *
     * @param filename             File to receive the INSERT statements
     * @param dropForeignKeychecks True if foreign key checking should be dropped
     * @param deleteAllRows        True if the table is to be cleared before loading
     * @param info                 Additional information to include in the file
     * @param progress             Optional progress indicator
     * @param tablesToIgnore       List of tables to ignore
     * @param compress             True if the file should be compressed
     *
     * @throws java.lang.Exception Errors if there is a problem
     */
    public static void dumpDatabase(File filename, boolean dropForeignKeychecks, boolean deleteAllRows, String info, Progress progress, List<String> tablesToIgnore, boolean compress) throws Exception {

        // Check for goofiness

        if (filename != null) {

            // Get an outputstream to use

            OutputStreamWriter out = null;
            try {

                // Create the file output stream to use

                if (compress) {
                    out = new OutputStreamWriter(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(filename))), "UTF-8");
                }
                else {
                    out = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(filename)), "UTF-8");
                }

                // Dump the database to the file

                dumpDatabase(out, dropForeignKeychecks, deleteAllRows, info, progress, tablesToIgnore);
            }
            finally {
                Common.close(out);
            }
        }
    }

    /**
     * Dumps the current NRMM database to the given stream
     *
     * @param out                  Output stream to send contect to
     * @param dropForeignKeychecks True if foreign key checking should be dropped
     * @param deleteAllRows        True if the table is to be cleared before loading
     * @param info                 Additional information to include in the file
     * @param progress             Optional progress indicator
     * @param tablesToIgnore       Lst of tables to ignore
     *
     * @throws java.lang.Exception Errors if there is a problem
     */
    public static void dumpDatabase(OutputStreamWriter out, boolean dropForeignKeychecks, boolean deleteAllRows, String info, Progress progress, List<String> tablesToIgnore) throws Exception {

        Connection connection = null;
        try {
            // Get the NRMM database connection to use

            DataSource ds = HibernateUtils.getDataSource();
            if (ds != null) {

                // Dump the database

                connection = ds.getConnection();
                dumpDatabase(connection, out, dropForeignKeychecks, deleteAllRows, info, progress, tablesToIgnore);
            }
        }
        finally {
            Common.close(connection);
        }
    }

    /**
     * Returns a list of all the table names in the schema
     *
     * @return List of table names
     */
    public static List<String> getTables() {
        return getTables(null, null);
    }

    /**
     * Returns a list of all the table names in the schema
     *
     * @param tablesToExclude Set of tables to not dump
     *
     * @return List of table names
     */
    public static List<String> getTables(Set<String> tablesToExclude) {
        return getTables(tablesToExclude, null);
    }

    /**
     * Returns a list of all the table names in the schema
     *
     * @param tablesToExclude Set of tables to not dump
     * @param progress Progress object to be updated with number of actions
     *                 Must be instantiated
     *
     * @return List of table names
     */
    public static List<String> getTables(Set<String> tablesToExclude, Progress progress) {

        List<String> returnValue = new ArrayList<>();
        Connection connection = null;
        try {
            // Get the NRMM database connection to use

            DataSource ds = HibernateUtils.getDataSource();
            ResultSet tables = getTableSet(ds, ds.getConnection());

            if (tables != null) {
                // Loop through the tables and add them to the list if not excluded

                String thisTable;
                while (tables.next()) {
                    thisTable = tables.getString(3);

                    if (Common.isBlank(tablesToExclude) || !tablesToExclude.contains(thisTable)) {

                        if (progress != null) {
                            if (connection == null)
                                connection = ds.getConnection();

                            ResultSet results = executeQuery(connection, "select count(*) from " + thisTable);
                            if (!results.isClosed() && results.next()) {
                                progress.setTotal(progress.getTotal() + results.getInt(1));
                            }
                            else {
                                Common.close(results);
                            }
                        }
                        returnValue.add(thisTable);
                    }
                }
            }
        }
        catch (Exception e) {
            logger.error("Cannot get table list - {}", PivotalException.getErrorMessage(e));
        }
        finally {
            Common.close(connection);
        }
        return returnValue;
    }


    /**
     * Returns a Resultset of all the table names in the schema
     *
     * @param ds Datasource to use
     * @param connection connection to Use
     *
     * @return List of table names
     */
    public static ResultSet getTableSet(DataSource ds, Connection connection) {

        ResultSet tables=null;
        try {
            // Get the NRMM database connection to use

            if (ds != null) {
                String[] types = {"TABLE"};
                if (((AppDataSource) ds).isPostgreSQL())
                    tables = connection.getMetaData().getTables(null, ((AppDataSource)ds).getSchema(), "%", types);
                else
                    tables = connection.getMetaData().getTables(null, null, "%", types);

            }
        }
        catch (Exception e) {
            logger.error("Cannot get table list - {}", PivotalException.getErrorMessage(e));
        }
        return tables;
    }

    /**
     * Reloads the database from the given file
     *
     * @param filename File to receive the INSERT statements
     * @param progress Progress object to use
     * @param contentType Type of file being processed
     *
     * @throws java.lang.Exception Errors if there is a problem
     */
    public static void reloadDatabase(File filename, Progress progress, String contentType) throws Exception {
        // Check for goofiness
        AppDataSource ds = HibernateUtils.getDataSource();
        if (!Common.isBlank(filename) && filename.length() > 0 && ds != null) {

            // Shutdown the application - this will release any connections we have
            // to the database and prevent any from being created

            try {
                progress.setTotal(10);
                progress.setCount(1);
                Initialisation.shutdown(false);

                // Get a connection to the database

                Connection connection = null;
                try {
                    // Get a new connection
                    connection = DriverManager.getConnection(ds.getUrl(), ds.getUsername(), ds.getPassword());

                    // Clear the database
                    logger.info("Clearing {} database", Common.getAplicationName());
                    clearDatabase(ds, connection, progress, "user_status", "patch");

                    if (Common.doStringsMatch(contentType, "application/x-gzip", "application/gzip")) {
                        logger.info("Reloading {} database from gzip file [{}]", Common.getAplicationName(), filename);
                        progress.setTotal((int) filename.length());
                        processSingleFile(new GZIPInputStream(new FileInputStream(filename)), connection, progress);
                    }
                    else {
                        // Reload the database
                        logger.info("Reloading {} database from [{}]", Common.getAplicationName(), filename);
                        progress.setTotal((int) filename.length());
                        processSingleFile(new FileInputStream(filename), connection, progress);
                    }
                }
                catch (Exception e) {
                    logger.error("Problem executing command - {}", PivotalException.getErrorMessage(e));
                }
                finally {
                    if (connection != null) {
                        connection.close();
                    }
                    // Clear the hibernate cache
                    HibernateUtils.clearCache();

                    // Start the application again
                    Initialisation.startup();
                }
            }
            catch (Exception e) {
                logger.error("Problem shutting down the application - {}", PivotalException.getErrorMessage(e));
            }
        }
        progress.setFinished(true);
    }

    /**
     * Reload the database from the file specified
     *
     * @param is         Stream to read file from
     * @param connection Database connection
     * @param progress   Progress to show
     *
     * @throws Exception
     */
    private static void processSingleFile(InputStream is, Connection connection, Progress progress) throws Exception {
        Reader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(is, "UTF-8"), 32 * 1024);
            reloadDatabase(connection, in, progress);
            logger.info("Reloading {} database complete", Common.getAplicationName());
        }
        finally {
            Common.close(in, connection);
        }
    }


    /**
     * Loads the database using the statements in the reader
     *
     * @param db       Connection to an updatable database
     * @param in       Input reader containing commands
     * @param progress Progress object to use
     *
     * @throws java.lang.Exception Errors if there is a problem
     */
    public static void reloadDatabase(Connection db, Reader in, Progress progress) throws Exception {

        // Check for goofiness

        if (in != null) {

            // Turn off the referential integrity

            boolean autoCommit = db.getAutoCommit();
            db.setAutoCommit(false);
            AppDataSource ds = HibernateUtils.getDataSource();
            setReferentialIntegrity(ds, db, false, false, progress);

            // We need to loop through all the bytes in the stream checking
            // to see when we have a complete command

            String commandString;
            StringBuilder command = new StringBuilder();
            boolean inQuotes = false;
            boolean inEscape = false;
            boolean addCharToBuffer = true;
            int value;
            int commands = 0;

            if (ds.isPostgreSQL())
                executeCommand(db, "set schema '" + ds.getSchema() + "';");

            while ((value = in.read()) > -1) {

                // Check for an EOL and then check the command

                progress.setCount(progress.getCount() + 1);
                char charValue = (char) value;
                if (charValue == '\n') {

                    // Discard the command buffer if it is empty (just blank lines)
                    // or it is a comment - make sure we allow the MySQL special commands

                    commandString = command.toString().trim();
                    if (Common.isBlank(commandString) || commandString.matches("(?ism)\\s*/\\*.*\\*/\\s*")) {
                        command = new StringBuilder();
                        addCharToBuffer = false;
                        inQuotes = false;
                        inEscape = false;
                    }
                }

                // Check for special characters
                // If we see a quote then it could be the start of string or and escaped quote

                if (charValue == '\'') {
                    if (!inEscape)
                        inQuotes = !inQuotes;
                    else
                        inEscape = false;
                }

                // If we see a slash, then it could be just a slash or if it is inside
                // a string then it must signify the beginning of an escape

                else if (charValue == '\\') {
                    if (inQuotes) {
                        inEscape = !inEscape;
                    }
                }

                // Look for the command terminator

                else if (charValue == ';') {
                    inEscape = false;
                    commandString = command.toString().trim();
                    if (!inQuotes && !Common.isBlank(commandString) && !commandString.matches("(?ism)\\s*/\\*.*")) {

                        // We will need to filter the command if the DB is H2

                        if (HibernateUtils.getDataSource().isH2()) {
                            commandString = getCleanH2Command(commandString);
                        }
                        else if (HibernateUtils.getDataSource().isPostgreSQL()) {
                            commandString = getCleanPostgreSQLCommand(commandString);
                        }

                        // Fix the user table for legacy data

                        commandString = commandString.replaceFirst("(?mis)^insert\\s+into\\s+user\\s+", "insert into users ");
                        commandString = commandString.replaceFirst("(?mis)^delete\\s+from\\s+user[\\s;]?", "delete from users ");
                        commandString = commandString.replaceFirst("(?mis)^update\\s+user\\s+", "update users ");
                        commandString = commandString.replaceFirst("(?mis)^truncate\\s+table\\s+user[;]?$", "truncate table users");
                        if (!Common.isBlank(commandString)) {

                            // We've got ourselves a command so now we need to deal with it

                            commands++;
                            try {
                                executeUpdateQuery(db, commandString);
                            }
                            catch (SQLException e) {
                                logger.error(PivotalException.getErrorMessage(e));
                            }
                        }

                        // Reset for the next command

                        command = new StringBuilder();
                        addCharToBuffer = false;
                        inQuotes = false;
                        inEscape = false;
                        progress.setMessage("Loaded " + commands + " rows");
                    }
                    else if (Common.isBlank(commandString)) {

                        // If the command is blank start a new buffer

                        command = new StringBuilder();
                        addCharToBuffer = false;
                    }
                }

                // We can't be in an escape sequence

                else
                    inEscape = false;

                // If we should add the character to the buffer

                if (addCharToBuffer) command.append(charValue);
                addCharToBuffer = true;
            }

            // Turn on the referential integrity

            setReferentialIntegrity(HibernateUtils.getDataSource(), db, true, false, progress);
            db.setAutoCommit(autoCommit);
        }
    }

    /**
     * Turns the command into one suitable for execution against H2
     *
     * @param commandString SQL command
     *
     * @return Santised H2 version
     */
    private static String getCleanH2Command(String commandString) {

        if (!Common.isBlank(commandString)) {
            try {
                // There is no alternative to safe updates so we need to remove these

                commandString = commandString.replaceFirst("(?is) *set *sql_safe_updates *= *\\d *;? *$", "");

                // Fix some H2 issues as per
                // http://matthewcasperson.blogspot.co.uk/2013/07/exporting-from-mysql-to-h2.html

                commandString = commandString.replaceAll("\\\\'", "''").replaceAll("0x([A-F0-9]*)", "$1").replaceAll("((\\\\r)|(\\\\n))+", "\n");
                commandString = commandString.replaceAll("\\\\\"", "\\\"");

                // Change any unhex functions to the equivalent

                if (commandString.matches("(?mis).+unhex\\('.+")) {
                    commandString = commandString.replaceAll("(?mis)(.+)unhex\\(([^\\)]+)\\)(.+)", "$1X$2$3");
                }

                // We need to switch out the foreign key checks with the H2 alternatives

                if (commandString.matches("(?mis).+foreign_key_checks.+")) {
                    commandString = commandString.replaceFirst("(?is) *set +foreign_key_checks *= *(false|true|0|1) *;? *$", "SET REFERENTIAL_INTEGRITY $3");
                }
            }
            catch (Exception e) {
                logger.debug("Problem sanitising H2 command - {}", PivotalException.getErrorMessage(e));
            }
        }

        return commandString;
    }

    /**
     * Turns the command into one suitable for execution against PostgreSQL
     *
     * @param commandString SQL command
     *
     * @return Santised PostgreSQL version
     */
    private static String getCleanPostgreSQLCommand(String commandString) {

        if (!Common.isBlank(commandString)) {
            try {
                // There is no alternative to safe updates so we need to remove these

                commandString = commandString.replaceFirst("(?is) *set *sql_safe_updates *= *\\d *;? *$", "");

                // Change any unhex or X functions to the equivalent

                if (commandString.matches("(?mis).+unhex\\('.+")) {
                    commandString = commandString.replaceAll("(?mis)(.+)unhex\\(([^\\)]+)\\)(.+)", "$1E'\\\\\\\\x$2'$3");
                }
                if (commandString.matches("(?mis).+[\\s,=\\(]X'[^']+'[\\s,\\);].*")) {
                    commandString = commandString.replaceAll("(?mis)(.+[\\s,=\\(])X'([^\\']+)'([\\s,\\);].*)", "$1E'\\\\\\\\x$2'$3");
                }

                // We need to switch out the foreign key checks with the PostgreSQL alternatives

                if (commandString.matches("(?mis).+foreign_key_checks.+")) {
                    commandString = commandString.replaceFirst("(?is) *set +foreign_key_checks *= *(false|true|0|1) *;? *$", "SET CONSTRAINTS ALL DEFERRED");
                }

                // Change the quoting

                commandString = commandString.replaceAll("\\\\'", "''");

                // Change the newlines

                commandString = commandString.replaceAll("(\\\\r)?\\\\n", "\n");

                // Remove any truncate commands - we just can't tolerate them mid stream

                commandString = commandString.replaceAll("(?mis)truncate table .+", "");
            }
            catch (Exception e) {
                logger.debug("Problem sanitising PostgreSQL command - {}", PivotalException.getErrorMessage(e));
            }
        }

        return commandString;
    }

    /**
     * Dumps the current NRMM database to the given file
     *
     * @param progress Progress indicator
     *
     * @throws java.lang.Exception Errors if there is a problem
     */
    public static void clearDatabase(Progress progress) throws Exception {

        // Get a connection to the database

        AppDataSource ds = HibernateUtils.getDataSource();
        if (ds != null) {

            // Shutdown the application

            try {
                progress.setTotal(10);
                progress.setCount(1);
                Initialisation.shutdown(false);

                // Get a new connection and clear the database

                Connection connection = null;
                try {
                    connection = DriverManager.getConnection(ds.getUrl(), ds.getUsername(), ds.getPassword());
                    logger.info("Clearing {} database", Common.getAplicationName());
                    clearDatabase(ds, connection, progress, "patch");
                    logger.info("Clearing {} database complete", Common.getAplicationName());
                }
                catch (Exception e) {
                    logger.error("Problem getting a connection - {}", PivotalException.getErrorMessage(e));
                }
                finally {
                    Common.close(connection);
                }

                // Clear the hibernate cache

                HibernateUtils.clearCache();

                // Start the application again

                Initialisation.startup(true, false);
            }
            catch (Exception e) {
                logger.error("Problem shutting down the application - {}", PivotalException.getErrorMessage(e));
            }
        }
    }

    /**
     * Applies any test data that the system may have been supplied with
     *
     * @param progress Progress indicator
     *
     * @throws java.lang.Exception Errors if there is a problem
     */
    public static void applyTestDataToDatabase(Progress progress) throws Exception {

        // Get a connection to the database

        AppDataSource ds = HibernateUtils.getDataSource();
        if (ds != null) {

            // Shutdown the application

            try {
                progress.setTotal(10);
                progress.setCount(1);
                Initialisation.shutdown(false);

                // Get a new connection and clear the database

                Connection connection = null;
                Statement statement = null;
                String commandCheck = null;
                boolean autoCommit = true;
                try {

                    // Clear the database first

                    connection = DriverManager.getConnection(ds.getUrl(), ds.getUsername(), ds.getPassword());
                    autoCommit = connection.getAutoCommit();
                    logger.info("Clearing {} database", Common.getAplicationName());
                    clearDatabase(ds, connection, progress, "user_status","patch");
                    logger.info("Applying test data to {}", Common.getAplicationName());

                    // Start the application again

                    Initialisation.startup(true, true);

                    // Database is empty except for archetypes, so now fill it up with test data

//                    String[] commandList = Common.readTextFile(ServletHelper.getRealPath(Constants.NRMM_SQL_TEST_DATA_FILE)).split("(?ims)(;\\s+)|(\\*/;*\\s*)");
                    String[] commandList = readTextFile(ServletHelper.getRealPath(Constants.APP_SQL_TEST_DATA_FILE)).split("(?ims)(;$)|(\\*/;*\\s*)");
                    progress.setTotal(progress.getTotal() + commandList.length);
                    statement = connection.createStatement();
                    connection.setAutoCommit(false);
                    setReferentialIntegrity(ds, connection, false, false, progress);
                    if (ds.isMySQL()) {
                        statement.execute(String.format("use %s", ds.getDatabase()));
                        statement.close();
                        statement = connection.createStatement();
                    }

                    // Add each command to the buffer

                    for (String command : commandList) {
                        if (!Common.isBlank(command) && !command.trim().startsWith("/*")) {
                            if (ds.isH2()) {
                                command = getCleanH2Command(command);
                            }
                            else if (ds.isPostgreSQL()) {
                                command = getCleanPostgreSQLCommand(command);
                            }

                            commandCheck = command;
                            statement.execute(command);
                            statement.close();
                            statement = connection.createStatement();
                        }
                        progress.setCount(progress.getCount() + 1);
                    }
                    logger.info("Applied test data to {} successfully", Common.getAplicationName());
                }
                catch (Exception e) {
                    logger.error("Problem with statement {}\n{}", commandCheck, PivotalException.getErrorMessage(e));
                }
                finally {
                    if (statement != null) {
                        connection.commit();
                        setReferentialIntegrity(ds, connection, true, false, progress);
                        connection.setAutoCommit(autoCommit);
                    }
                    Common.close(statement, connection);
                }

                // Clear the hibernate cache

                HibernateUtils.clearCache();

                // Start the services

                Initialisation.startServices();
            }
            catch (Exception e) {
                logger.error("Problem shutting down the application - {}", PivotalException.getErrorMessage(e));
            }
        }
    }

    /**
     * Applies any settings data that the system may have been supplied with
     *
     * @param progress Progress indicator
     *
     * @throws java.lang.Exception Errors if there is a problem
     */
    public static void applySettingsDataToDatabase(Progress progress) throws Exception {

        // Get a connection to the database

        AppDataSource ds = HibernateUtils.getDataSource();
        if (ds != null) {

            try {
                progress.setTotal(10);
                progress.setCount(1);

                // Get a new connection and load the data

                Connection connection = null;
                Statement statement = null;
                String commandCheck = null;
                boolean autoCommit = true;
                try {

                    // Connect to the database

                    connection = DriverManager.getConnection(ds.getUrl(), ds.getUsername(), ds.getPassword());
                    autoCommit = connection.getAutoCommit();
                    logger.info("Applying settings data to {}", Common.getAplicationName());

                    // load it up with settings data

                    String[] commandList = readTextFile(ServletHelper.getRealPath(Constants.APP_SQL_SETTINGS_DATA_FILE)).split("(?ims)(;$)|(\\*/;*\\s*)");
                    progress.setTotal(progress.getTotal() + commandList.length);
                    statement = connection.createStatement();
                    connection.setAutoCommit(true);
                    setReferentialIntegrity(ds, connection, false, false, progress);
                    if (ds.isMySQL()) {
                        statement.execute(String.format("use %s", ds.getDatabase()));
                        statement.close();
                        statement = connection.createStatement();
                    }

                    // Add each command to the buffer

                    for (String command : commandList) {
                        if (!Common.isBlank(command) && !command.trim().startsWith("/*")) {
                            if (ds.isH2()) {
                                command = getCleanH2Command(command);
                            }
                            else if (ds.isPostgreSQL()) {
                                command = getCleanPostgreSQLCommand(command);
                            }

                            // Process each command separately
                            try {
                                commandCheck = command;
                                statement.execute(command);
                                statement.close();
                            }
                            catch(Exception e) {
                                logger.error("Problem with statement {}\n{}", commandCheck, PivotalException.getErrorMessage(e));
                            }
                            statement = connection.createStatement();
                        }
                        progress.setCount(progress.getCount() + 1);
                    }
                    logger.info("Applied settings data to {} successfully", Common.getAplicationName());
                }
                catch (Exception e) {
                    logger.error("Problem with statement {}\n{}", commandCheck, PivotalException.getErrorMessage(e));
                }
                finally {
                    if (statement != null) {
                        setReferentialIntegrity(ds, connection, true, false, progress);
                        connection.setAutoCommit(autoCommit);
                    }
                    Common.close(statement, connection);
                }

                // Clear the hibernate cache

                HibernateUtils.clearCache();
            }
            catch (Exception e) {
                logger.error("Problem controlling the application - {}", PivotalException.getErrorMessage(e));
            }
        }
    }

    /**
     * Truncates the database in the ArrayList
     *
     * @param connection Connection to a database
     * @param tableNames List of table names
     *
     * @throws java.lang.Exception Errors
     */
    public static void clearDatabaseTables(Connection connection, List<String> tableNames) throws Exception {

        try {
            List<String>tables = getTables();

            if (!tableNames.isEmpty()) {
                executeCommand(connection, "set foreign_key_checks=0");

                // Loop through all tables

                for(String table : tables) {

                    // Check if the table is listed and truncate

                    if (tableNames.contains(table)) {
                        executeCommand(connection, "truncate table " + table);
                    }
                }
                executeCommand(connection, "set foreign_key_checks=1");
            }
        }
        catch(Exception e) {
            throw new PivotalException(e);
        }
    }

    /**
     * Clears the database by truncating all tables
     *
     * @param ds         Datasource used to get connection
     * @param connection Database connection to use
     * @param progress   Progress indicator
     *
     * @throws java.lang.Exception Errors if there is a problem
     */
    public static void clearDatabase(AppDataSource ds, Connection connection, Progress progress) throws Exception {
        clearDatabase(ds, connection, progress, (String[]) null);
    }

    /**
     * Clears the database by truncating all tables
     *
     * @param ds         Datasource used to get connection
     * @param connection Database connection to use
     * @param progress   Progress indicator
     * @param excludedTables Tables not to be included when clearing
     *
     * @throws java.lang.Exception Errors if there is a problem
     */
    public static void clearDatabase(AppDataSource ds, Connection connection, Progress progress, String... excludedTables) throws Exception {

        // Get the exclusions into an easy list

        Map<String, String> exclusions = new LinkedCaseInsensitiveMap<>();
        if (!Common.isBlank(excludedTables)) {
            for (String table : excludedTables) {
                exclusions.put(table, table);
            }
        }

        // Get the ID exclusions

        Map<String, String> idExclusions = new LinkedCaseInsensitiveMap<>();

        // Get a list of all the tables and the scope of the problem

        List<String> tables = new ArrayList<>();
        String database = null;

        ResultSet tableRes = getTableSet(ds, connection);

        while (tableRes != null && tableRes.next()) {
            if (!exclusions.containsKey(tableRes.getString(3))) {
                progress.setTotal(progress.getTotal() + 1);
                if (database == null) {
                    database = tableRes.getString(1);
                }
                tables.add(tableRes.getString(3));

                // Check to see if this table has an ID primary key

                ResultSet colRes = connection.getMetaData().getPrimaryKeys(tableRes.getString(1), tableRes.getString(2), tableRes.getString(3));
                if (!colRes.next() || !Common.doStringsMatch(colRes.getString("COLUMN_NAME"), "id")) {
                    idExclusions.put(tableRes.getString(3), tableRes.getString(3));
                }
                Common.close(colRes);
            }
        }
        if (tableRes != null)
            tableRes.close();

        // Truncate each table

        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        setReferentialIntegrity(ds, connection, false, true, progress);
        if (ds.isPostgreSQL())
            executeCommand(connection, "set schema '" + ds.getSchema() + "';");

        for (String table : tables) {
            logger.info("Truncating {}", table);
            progress.setMessage("Truncating " + table);
            try {
                executeCommand(connection, "truncate table " + table);

                // H2 doesn't reset the auto-increment columns on truncate

                if (ds.isH2()) {
                    executeCommand(connection, String.format("ALTER TABLE %s ALTER COLUMN id RESTART WITH 1", table));
                }

                // PostgreSQL is similar - there is a really rubbish thing here in that most databases
                // will catch an error and give you an option to ignore it, but not PostgreSQL. Consequently,
                // we have to catch all the tables that don't have an ID column

                else if (ds.isPostgreSQL() && !idExclusions.containsKey(table)) {
                    executeCommand(connection, String.format("SELECT setval(pg_get_serial_sequence('%s', 'id'), coalesce(max(id),0) + 1, false) FROM %s", table, table));
                }
            }
            catch (SQLException e) {

                // Ignore those due to tables that don't have an ID column

                if (!PivotalException.getErrorMessage(e).contains("not found") && !PivotalException.getErrorMessage(e).contains("does not exist")) {
                    logger.error("Problem truncating [{}] - {}", table, PivotalException.getErrorMessage(e));
                }
            }
            progress.setCount(progress.getCount() + 1);
        }

        // Turn back on referential integrity

        connection.commit();
        setReferentialIntegrity(ds, connection, true, true, progress);
        connection.setAutoCommit(autoCommit);
    }

    /**
     * Turns on/off referential integrity checks for the specific database type
     *
     * @param ds             Data source
     * @param connection     Connection to use
     * @param on             True if referential integrity should be turned on
     * @param useConstraints Used in PostgreSQL connections to drop/re-apply FK constraints
     * @param progress       Container to record progress of the operation
     *
     * @throws SQLException If there is an error
     */
    public static void setReferentialIntegrity(AppDataSource ds, Connection connection, boolean on, boolean useConstraints, Progress progress) throws SQLException {

        // Easy on MySQL

        if (ds.isMySQL()) {
            executeCommand(connection, "set names 'utf8';");
            executeCommand(connection, String.format("set foreign_key_checks=%s;", on ? "1" : "0"));
        }

        // Easy on H2

        else if (ds.isH2()) {
            executeCommand(connection, String.format("SET REFERENTIAL_INTEGRITY %s;", on ? "TRUE" : "FALSE"));
        }

        // Not so easy on PostgreSQL

        else if (ds.isPostgreSQL()) {

            // Re-apply all the constraints that we saved previously

            if (on) {
                if (useConstraints) {
                    String commands = connection.getClientInfo().getProperty("xx_constraints");
                    if (!Common.isBlank(commands)) {
                        for (String command : commands.split(";\n")) {
                            executeCommand(connection, command);
                        }
                    }
                    connection.getClientInfo().setProperty("xx_constraints", "");
                }
                // Need to update all of the sequences so that the last_value is consistent with the MAX(value) of the SERIAL column
                updatePostgresqlSequenceValues(connection, progress);
            }

            // Get a list of all the constraints and then drop them all

            else {
                executeCommand(connection, "SET CONSTRAINTS ALL DEFERRED;");
                if (useConstraints) {
                    String tmp = "SELECT 'ALTER TABLE '||nspname||'.'||relname||' ADD CONSTRAINT '||conname||' '|| pg_get_constraintdef(pg_constraint.oid)||';'\n" +
                            "FROM pg_constraint\n" +
                            "INNER JOIN pg_class ON conrelid=pg_class.oid\n" +
                            "INNER JOIN pg_namespace ON pg_namespace.oid=pg_class.relnamespace\n" +
                            "WHERE contype='f'";
                    PreparedStatement ps = null;
                    ResultSet results = null;
                    List<String> commands = new ArrayList<>();
                    try {
                        ps = connection.prepareStatement(tmp);
                        results = ps.executeQuery();
                        if (!Common.isBlank(results)) {
                            while (results.next()) {
                                commands.add(results.getString(1));
                            }
                        }
                    }
                    finally {
                        if (!Common.isBlank(results) && !results.isClosed()) {
                            results.close();
                        }
                        if (!Common.isBlank(ps) && !ps.isClosed()) {
                            ps.close();
                        }
                    }
                    connection.getClientInfo().setProperty("xx_constraints", Common.join(commands, "\n"));

                    // Drop all the constraints

                    tmp = "SELECT 'ALTER TABLE '||nspname||'.'||relname||' DROP CONSTRAINT '||conname||';'\n" +
                            "FROM pg_constraint \n" +
                            "INNER JOIN pg_class ON conrelid=pg_class.oid \n" +
                            "INNER JOIN pg_namespace ON pg_namespace.oid=pg_class.relnamespace\n" +
                            "WHERE contype='f'";
                    ps = null;
                    try {
                        ps = connection.prepareStatement(tmp);
                        results = ps.executeQuery();
                        if (!Common.isBlank(results)) {
                            while (results.next()) {
                                executeCommand(connection, results.getString(1));
                            }
                        }
                    }
                    finally {
                        if (!Common.isBlank(results) && !results.isClosed()) {
                            results.close();
                        }
                        if (!Common.isBlank(ps) && !ps.isClosed()) {
                            ps.close();
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates a deep copy of the results set
     *
     * @param list Results set to copy
     *
     * @return Clone of the results set
     */
    public static List<Map<String, Object>> cloneResultsList(List<Map<String, Object>> list) {

        List<Map<String, Object>> clone = new ArrayList<>();
        for (Map<String, Object> row : list) {
            Map<String, Object> newRow = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                newRow.put(entry.getKey(), cloneValueOf(entry.getValue()));
            }
            clone.add(newRow);
        }
        return clone;
    }

    /**
     * Update the Postgresql sequences with the correct values
     *
     * @param connection Connection to the database
     * @param progress   Container holding the current progress of the operation
     */
    private static void updatePostgresqlSequenceValues(Connection connection, Progress progress) {
        List<SequenceInformation> sequences = new ArrayList<>();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            // Get a list of all sequences inn the schema
            logger.info("Obtaining list of known sequences");
            ps = connection.prepareStatement("SELECT sequence_name FROM information_schema.sequences WHERE sequence_catalog = ?");
            ps.setString(1, connection.getCatalog());
            rs = ps.executeQuery();
            // Store each in the list
            if (!Common.isBlank(rs)) {
                while (rs.next()) {
                    SequenceInformation thisOne = new SequenceInformation();
                    thisOne.setSequenceName(rs.getString("sequence_name"));
                    sequences.add(thisOne);
                }
            }
            // Cleanup
            rs.close();
            ps.close();
            // Now for each sequence found
            for (SequenceInformation sequence : sequences) {
                logger.info("Processing sequence : {}", sequence.getSequenceName());
                // See if its an id sequence
                if (sequence.getSequenceName().endsWith("_id_seq")) {
                    // Now see if theres a table associated with the sequence (if so the format is <table_name>_id_seq
                    String tableName = sequence.getSequenceName().substring(0, sequence.getSequenceName().length() - 7);
                    ps = connection.prepareStatement("SELECT column_name AS column_name, COUNT(*) AS record_count FROM information_schema.columns WHERE table_catalog = ? AND table_name = ? AND column_default = ? GROUP BY column_name");
                    ps.setString(1, connection.getCatalog());
                    ps.setString(2, tableName);
                    ps.setString(3, String.format("nextval('%s'::regclass)", sequence.getSequenceName()));
                    rs = ps.executeQuery();
                    // Loop over each one found and store the values
                    while (rs.next()) {
                        logger.info("Processing sequence {} : Found tableName {}", sequence.getSequenceName(), tableName);
                        String columnName = rs.getString("column_name");
                        sequence.setTableName(tableName);
                        sequence.setColumnName(columnName);
                    }
                    rs.close();
                    ps.close();
                }
            }
            // Now for each sequence find the max value and set the sequence accordingly. Could be done above, but this is cleaner as only 1PS + 1RS is open at any point etc
            for (SequenceInformation sequence : sequences) {
                if (Common.isBlank(sequence.getTableName())) {
                    logger.info("Processing sequence {} : No table found - ignored", sequence.getSequenceName());
                }
                else {
                    logger.info("Processing sequence {} : Table name {}, Sequence key attribute {}", sequence.getSequenceName(), sequence.getTableName(), sequence.getColumnName());
                    ps = connection.prepareStatement(String.format("SELECT MAX(%s) AS max_id FROM %s", sequence.getColumnName(), sequence.getTableName()));
                    rs = ps.executeQuery();
                    while (rs.next()) {
                        int maxValue = rs.getInt("max_id");
                        // Increment maxValue as we are setting this to be the next number returned by the sequence
                        maxValue++;
                        logger.info("Processing sequence {} : Max value {}", sequence.getSequenceName(), maxValue);
                        executeQuery(connection, String.format("SELECT SETVAL('%s', %s, false)", sequence.getSequenceName(), maxValue));
                    }
                    rs.close();
                    ps.close();
                }
            }
            progress.setCount(progress.getCount() + 1);
        }
        catch (SQLException e) {
            logger.error("Error processing Postgresql Sequences", e);
            progress.setError("Error during post-processing of sequence values");
        }
        finally {
            try {
                if (!Common.isBlank(rs) && !rs.isClosed()) {
                    rs.close();
                }
            }
            catch (SQLException e) {
                logger.error("Error Closing the result set", e);
            }
            try {
                if (!Common.isBlank(ps) && !ps.isClosed()) {
                    ps.close();
                }
            }
            catch (SQLException e) {
                logger.error("Error closing the Prepared Statement", e);
            }
        }
    }

    /**
     * Gets the list of column names from the specified table
     * Returns a map of info about the column. The column name will always
     * be in the 'Field' property
     *
     * @param tableName Table to get columns of
     *
     * @return List of Maps
     */
    public static List<Map<String, Object>> getColumns(String tableName) {

        List<Map<String, Object>> columns = new ArrayList<Map<String, Object>>();

        Database db = new DatabaseHibernate();
        try {

            if (HibernateUtils.getDataSource().isPostgreSQL()) {

                columns = db.find(String.format("select column_name as Field,* from information_schema.columns where table_schema = '%s' and table_name = '%s'", HibernateUtils.getDataSource().getSchema(), tableName));

                for(Map<String, Object>thisColumn : columns)
                    thisColumn.put("Field", thisColumn.get("field"));
            }
            else {

                // Use the MySQL format as default

                columns = db.find("SHOW COLUMNS FROM user_status");
            }
        }
        catch(Exception e) {
            logger.debug(String.format("Error getting column details for %s. %s", tableName, PivotalException.getErrorMessage(e)));
        }
        finally {
            try {
                db.close();
            }
            catch(Exception e1) {
                logger.debug("Error closing db when getting column details for {}. {}", tableName, PivotalException.getErrorMessage(e1));
            }
        }

        return columns;
    }

    /**
     * Dumps the current NRMM workflow to the given file
     *
     * @param filename             File to receive the INSERT statements
     * @param settings             What to dump
     * @param progress             Optional progress indicator
     *
     * @throws java.lang.Exception Errors if there is a problem
     */
    public static void dumpWorkflow(File filename, String settings, Progress progress) throws Exception {

        // Check for goofiness

        if (filename != null) {

            List<Map<String,Object>>data=null;

            // Get an outputstream to use

            OutputStreamWriter out = null;
            try {

                // Create the file output stream to use

                out = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(filename)), "UTF-8");

                // Get the NRMM database connection to use

                DataSource ds = HibernateUtils.getDataSource();
                if (ds != null) {

                    // Dump the database

                    Connection connection = ds.getConnection();
                    List<String> workflowItems = Common.splitToList(settings, "[\r\n]");
                    progress.setTotal(workflowItems.size());
                    for (String item : workflowItems) {
                        progress.setMessage(item);
                        progress.setCount(progress.getCount() + 1);
                        data = outputWorkflowItem(connection, item, data);
                    }
                }
                if (!Common.isBlank(data))
                    out.write(JsonMapper.serializeItem(data));
            }
            finally {
                Common.close(out);
            }
        }
    }

    /**
     * Outputs the data specified by item to the out stream
     *
     * @param connection    Database connection
     * @param item          What to output
     * @param data          List of maps to write to
     *
     */
    private static List<Map<String, Object>> outputWorkflowItem(Connection connection, String item, List<Map<String, Object>>data) {

        if (!Common.isBlank(item)) {

            try {
                if (item.contains(".")) {
                    //TODO may need to output specific rows
                }
                else {
                    // Output whole table

                    // Run query
                    ResultSet resultSet = executeQuery(connection, String.format("select * from %s", item));

                    if (resultSet != null) {

                        if (data==null)
                             data = new ArrayList<>();

                        ResultSetMetaData meta = resultSet.getMetaData();

                        // Loop through data
                        Integer thisId;
                        String fieldName;
                        while (resultSet.next()) {
                            Map<String, Object>row = new HashMap<>();
                            thisId = null;
                            for (int col = 1; col <= meta.getColumnCount(); col++) {
                                fieldName = meta.getColumnName(col);
                                if ("id".equalsIgnoreCase(fieldName))
                                    thisId = (Integer)resultSet.getObject(col);
                                else if(fieldName.endsWith("_id"))
                                    row.put(fieldName, createLookupString(item, fieldName, resultSet.getObject(col)));
                                else
                                    row.put(fieldName, resultSet.getObject(col));
                            }
                            if (thisId != null) {
                                // Add identifier to row and add row to data list
                                row.put(LOOKUP_ROW_IDENTIFIER, item + "_" + thisId);
                                data.add(row);
                            }
                        }
                    }
                }
            }
            catch(Exception e) {
                logger.error("Error dumping {}, {}", item, PivotalException.getErrorMessage(e));
                throw new PivotalException(e);
            }
        }

        return data;
    }

    /**
     * Reloads the workflow from the given file
     *
     * @param filename      File to receive the INSERT statements
     * @param progress      Progress object to use
     * @param contentType   File content type
     *
     * @throws java.lang.Exception Errors if there is a problem
     */
    public static void loadWorkflow(File filename, Progress progress, String contentType) throws Exception {

        // Check for goofiness
        AppDataSource ds = HibernateUtils.getDataSource();
        if (!Common.isBlank(filename) && filename.length() > 0 && ds != null) {

            String data = Common.readTextFile(filename);

            if (!Common.isBlank(data)) {

                List dataMap = JsonMapper.deserializeItem(data, List.class);

                progress.setTotal(dataMap.size());
                progress.setCount(0);
                Map<String, Object>row;
                String rowKey;
                for(Object rowObject : dataMap) {

                    progress.setCount(progress.getCount() + 1);

                    row = null;
                    try {
                        row = (Map<String, Object>)rowObject;
                    }
                    catch(Exception e) {
                        row = null;
                        logger.error("Unable to copy row to map {}", PivotalException.getErrorMessage(e));
                    }

                    if (!Common.isBlank(row)) {
                        if (!row.containsKey(LOOKUP_ROW_IDENTIFIER)) {
                            logger.error("Row does not contain identifier");
                        }
                        else {
                            rowKey = (String) row.get(LOOKUP_ROW_IDENTIFIER);

                            // must have a name as that is what we are using to update records

                            if (row.containsKey("name") || rowKey.startsWith("search_field")) {
                                String rowTable;

                                if (rowKey.contains("_"))
                                    rowTable = rowKey.substring(0, rowKey.lastIndexOf("_"));
                                else
                                    rowTable = rowKey;

                                if (!Common.isBlank(rowTable)) {
                                    try {
                                        Class thisClass = HibernateUtils.getEntityClassByTable(rowTable);

                                        // Get current entity

                                        Object currentEntity;

                                        if (rowKey.startsWith("search_field")) {
                                            currentEntity = HibernateUtils.selectFirstEntity("from SearchFieldEntity where tableName = ? and fieldName = ? and displayName = ?", row.get("table_name"), row.get("field_name"), row.get("display_name"));
                                        }
                                        else
                                            currentEntity = HibernateUtils.getEntity(thisClass, (String) row.get("name"));

                                        Integer currentId = ClassUtils.invokeMethod(currentEntity, "getId");

                                        // If present get id

                                        if (currentId != null)
                                            row.put("id", currentId);

                                        // Process any lookups in a row
                                        for (String fieldName : row.keySet()) {
                                            if (row.get(fieldName) instanceof String && ((String) row.get(fieldName)).startsWith(LOOKUP_PREFIX)) {
                                                row.put(fieldName, getLookupEntity((String) row.get(fieldName)));
                                            }
                                        }

                                        Map entityRow = new HashMap<String, Object>();
                                        for(String fieldKey : row.keySet()) {
                                            String propertyName = HibernateUtils.getPropertyFromFieldName(thisClass, fieldKey);
                                            if (!Common.isBlank(propertyName))
                                                entityRow.put(propertyName, row.get(fieldKey));
                                        }

                                        // Create new entity
                                        Object newEntity = HibernateUtils.createEntity(thisClass, entityRow);
                                        if (newEntity != null) {
                                            logger.debug("Updating {} from {}", row.get("name"), rowTable);
                                            if (!HibernateUtils.save(newEntity))
                                                logger.error("Error updating record {}:{}", rowTable, currentId);
                                        }
                                        else
                                            logger.error("Error creating record {}:{}", rowTable, currentId);
                                    }
                                    catch (Exception e) {
                                        logger.error("Unable to get entity for " + rowKey);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        progress.setFinished(true);
    }

    /**
     * Builds a lookup string so we can look items up rather than use the id
     * when dumping the workflow
     *
     * @param fieldName     Field name
     * @param fieldValue    Current field value
     *
     * @return Lookup string for use when loading the workflow
     */
    private static Object createLookupString(String tableName, String fieldName, Object fieldValue) {

        Object returnValue=fieldValue;

        if (!Common.isBlank(tableName) && !Common.isBlank(fieldName) && !Common.isBlank(fieldValue) && fieldValue instanceof Integer) {

            try {
                logger.debug("Looking for {}.{}", tableName, fieldName);

                // Translate the tableName and fieldName to the lookup table

                String fieldSpec = String.format("%s.%s", tableName, fieldName).toLowerCase();
                String lookupTable = null;

                if ("role.type_id".equals(fieldSpec))
                    lookupTable = "role_type";
                else if ("report_text.type_id".equals(fieldSpec))
                    lookupTable = "report_text_type";
                else if ("report_text.parent_id".equals(fieldSpec))
                    lookupTable = "report_text";

                if (lookupTable != null) {

                    Object entity = HibernateUtils.getEntity(HibernateUtils.getEntityClassByTable(lookupTable), (Integer) fieldValue);

                    String name = ClassUtils.invokeMethod(entity, "getName");

                    returnValue = String.format("%s%s%s%s%s",LOOKUP_PREFIX, lookupTable, LOOKUP_DIVIDER, name, LOOKUP_SUFFIX);
                }
            }
            catch (Exception e) {
                logger.error("Unable to get current entity or name for {}.{} - {}", tableName, fieldName, PivotalException.getErrorMessage(e));
            }
        }

        return returnValue;
    }

    /**
     * Gets id from workflow lookup, expects the string in the format
     * <LOOKUP|table_name|row name|>
     *
     * @param lookup Lookup settings
     * @return Lookup entity
     */
    private static Object getLookupEntity(String lookup) {

        Object returnValue = null;

        if (lookup.startsWith(LOOKUP_PREFIX)) {

            // Dissect lookup
            String[] breakDown = lookup.split("\\|");
            if (breakDown.length == 4) {
                String tableName = breakDown[1];
                String rowName = breakDown[2];

                if (!Common.isBlank(tableName) && !Common.isBlank(rowName)) {
                    try {
                        returnValue = HibernateUtils.getEntity(HibernateUtils.getEntityClassByTable(tableName).getSimpleName(), rowName);
                    }
                    catch(Exception e) {
                        logger.debug("Error performing lookup for {}, {}", lookup, PivotalException.getErrorMessage(e));
                    }
                }
            }
        }
        logger.debug("Workflow Lookup {}", lookup);

        return returnValue;
    }
}
