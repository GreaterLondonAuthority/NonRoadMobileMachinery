/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.data.dao;

import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;
import org.springframework.util.LinkedCaseInsensitiveMap;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * Provides a single entry point for batch related operations
 */
public class BatchDesc {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BatchDesc.class);

    private List<String> columnOrder = new ArrayList<>();
    private HashMap<String, Integer> columnTypes = new LinkedCaseInsensitiveMap<>();
    private PreparedStatement stmt;
    private String keyColumnName;
    protected boolean isInTransaction;

    /**
     * Creates the BatchDesc object and execute the initial processing.
     * It already creates the PreparedStatemnt for insertion.
     *
     * @param tableName The table that is going to be updated
     * @param rowValues Map of column name/values
     * @param con Database connection for creating PreparedStatement and find out table's columns
     */
    public BatchDesc(String tableName, Map<String, Object> rowValues, Connection con) {
        try {

            // Find out the type of the columns of the database

            TableMetaData resultSetMetaData = TableMetaData.getMetaData(con, tableName.toLowerCase());
            for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                columnTypes.put(resultSetMetaData.getColumnName(i), resultSetMetaData.getColumnType(i));
                if (resultSetMetaData.isAutoIncrement(i))
                    keyColumnName = resultSetMetaData.getColumnName(i);
            }

            for (String column : rowValues.keySet()) {
                if (columnTypes.containsKey(column)) {
                    columnOrder.add(column);
                }
            }

            // Creates a prepared statement based on the table description and in the provided row values

            boolean isPostgreSQL = con.getMetaData().getConnection().getClass().getName().contains("postgres");
            stmt = con.prepareStatement(createInsertQuery(tableName), con.getMetaData().supportsGetGeneratedKeys()&&!isPostgreSQL?Statement.RETURN_GENERATED_KEYS:Statement.NO_GENERATED_KEYS);
        } catch (SQLException e) {
            logger.error(PivotalException.getErrorMessage(e));
            throw new PivotalException(e);
        }
    }

    /**
     * Add values of the mapping &lt;Database Column Name, Value%gt; as a batch into the
     * current PreparedStatement
     *
     * @param rowValues a map having the following meaning: &lt;Database Column Name, Value&gt;
     * @throws java.sql.SQLException if add batch is not successful.
     */
    public void addBatch(Map<String, Object> rowValues) throws SQLException {
        for (int i = 0; i < columnOrder.size(); i++) {
            Object value = rowValues.get(columnOrder.get(i));
            DataSourceUtils.setPreparedStatementValue(value, i+1,columnTypes.get(columnOrder.get(i)), stmt);
        }
        stmt.addBatch();
    }

    /**
     * Execute current batch. It means it will synchronize the records queued in the
     * PreparedStatement into the database.
     *
     * @throws java.sql.SQLException if execute is not successful.
     */
    public void executeBatch() throws SQLException {
        stmt.executeBatch();
    }

    /**
     * Returns the generated keys from the last batch operation
     *
     * @return List of generated IDs or null if not capable
     */
    public List<Long> getGeneratedKeys() {

        List<Long> returnValue = null;

        // Check the obvious

        if (stmt != null) {

            try {

                // If we have generated keys then great

                if (stmt.getConnection().getMetaData().supportsGetGeneratedKeys()) {
                    ResultSet keys=stmt.getGeneratedKeys();
                    while (keys.next()) {
                        if (returnValue==null) returnValue = new ArrayList<>();
                        returnValue.add(keys.getLong(1));
                    }
                }

                // Now try something different - going to be database vendor specific unfortunately
                // MySQL,Vertica

                else if (stmt.getConnection().getMetaData().getURL().contains("mysql") ||
                         stmt.getConnection().getMetaData().getURL().contains("vertica")) {
                    ResultSet keys=stmt.executeQuery("select last_insert_id()");
                    while (keys.next()) {
                        if (returnValue==null) returnValue = new ArrayList<>();
                        returnValue.add(keys.getLong(1));
                    }
                }

                // SQLite

                else if (stmt.getConnection().getMetaData().getURL().contains("sqlite")) {
                    ResultSet keys=stmt.executeQuery("select last_insert_rowid()");
                    while (keys.next()) {
                        if (returnValue==null) returnValue = new ArrayList<>();
                        returnValue.add(keys.getLong(1));
                    }
                }

                // SQLserver/Sybase

                else if (stmt.getConnection().getMetaData().getURL().contains("sqlserver") ||
                         stmt.getConnection().getMetaData().getURL().contains("sybase")) {
                    ResultSet keys=null;
                    try {
                        keys=stmt.executeQuery("select @@IDENTITY");
                        while (keys.next()) {
                            if (returnValue==null) returnValue = new ArrayList<>();
                            returnValue.add(keys.getLong(1));
                        }
                    }
                    finally {
                        Common.close(keys);
                    }
                }
            }
            catch (SQLException e) {
                logger.debug("Problem getting generated keys for addRecord - {}", PivotalException.getErrorMessage(e));
            }
        }

        return returnValue;
    }

    /**
     * Close current batch. It means it will close the PreparedStatement.
     */
    public void closeBatch() {
        Common.close(stmt);
    }

    /**
     * Check whether the current batch is closed or null.
     *
     * @return true if it's closed
     */
    public boolean isClosed() {
        try {
            return stmt == null || stmt.isClosed();
        }
        catch (Throwable e) {
            return false;
        }
    }

    /**
     * Returns the name of the auto generating key column if there is one
     *
     * @return Name of the auto-increment column
     */
    public String getKeyColumnName() {
        return keyColumnName;
    }

    /**
     * Create the insert query based on the table name and its columns.
     *
     * @param tableName the name of the table to be used in the insert query
     *
     * @return the string representing the insert query for PreparedStatement
     */
    private String createInsertQuery(String tableName) {
        StringBuilder builder = new StringBuilder();
        if (!Common.isBlank(columnOrder)) {
            builder.append("INSERT INTO ");
            builder.append(tableName);
            builder.append(" (");
            builder.append(Common.join(columnOrder));
            builder.append(") values (?");
            for (int i = 1; i < columnOrder.size(); i++) {
                builder.append(",?");
            }
            builder.append(')');
        }
        return builder.toString();
    }


}
