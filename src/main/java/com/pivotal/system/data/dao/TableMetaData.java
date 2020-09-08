/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.data.dao;

import com.pivotal.system.data.cache.CacheEngine;
import com.pivotal.utils.Common;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Storage are for Table meta data
 */
public class TableMetaData {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TableMetaData.class);
    private static final int CACHE_AGE = 1200;

    int columnCount;
    List<ColumnMetaData> columns=new ArrayList<>();

    /**
     * Constructs a meta data object for the given table and database connection
     *
     * @param db Database connection to use
     * @param tableName Name of the table
     * @throws SQLException Exception if it fails
     */
    TableMetaData(Connection db, String tableName) throws SQLException {
        if (db!=null && !Common.isBlank(tableName)) {
            Statement stmt=null;
            ResultSet table=null;
            try {
                stmt = db.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
                try {
                    table = stmt.executeQuery("select * from " + tableName.toLowerCase() + " where 0=1");
                }
                catch (SQLException e) {
                    table = stmt.executeQuery("select * from " + tableName.toUpperCase() + " where 0=1");
                }
                init(table.getMetaData());
            }
            finally {
                Common.close(table, stmt);
            }

        }
    }

    /**
     * Constructs a meta data object for the given table and database connection
     * It checks the cache first and will return one from there first
     *
     * @param db Database connection to use
     * @param tableName Name of the table
     * @throws java.sql.SQLException Exception if it fails
     * @return a {@link TableMetaData} object.
     */
    public static TableMetaData getMetaData(Connection db, String tableName) throws SQLException {
        TableMetaData returnValue = null;
        if (db!=null && !Common.isBlank(tableName)) {

            // Check if we have this in the cache already

            String cacheKey = db.getMetaData().getURL() + '~' + tableName;
            returnValue = CacheEngine.get(cacheKey);
            if (returnValue==null) {
                logger.debug("Constructing metadata for {}", tableName);
                returnValue = new TableMetaData(db, tableName);
                CacheEngine.put(cacheKey, CACHE_AGE, returnValue);
            }
            else
                logger.debug("Retrieved the metadata from cache for {}", tableName);
        }
        return returnValue;
    }

    /**
     * Constructs a meta data object based on the meta results
     * @param meta Meta results
     * @throws SQLException Exception of broken
     */
    private void init(ResultSetMetaData meta) throws SQLException {
        columnCount = meta.getColumnCount();
        columns.add(null);
        for (int i=1; i<=columnCount; i++)
            columns.add(new ColumnMetaData(meta, i));
    }

    /**
     * <p>Getter for the field <code>columnCount</code>.</p>
     *
     * @return a int.
     * @throws java.sql.SQLException if any.
     */
    public int getColumnCount() throws SQLException {
        return columnCount;
    }

    /**
     * <p>isAutoIncrement.</p>
     *
     * @param i a int.
     * @return a boolean.
     * @throws java.sql.SQLException if any.
     */
    public boolean isAutoIncrement(int i) throws SQLException {
        return columns.get(i).isAutoIncrement;
    }

    /**
     * <p>isCaseSensitive.</p>
     *
     * @param i a int.
     * @return a boolean.
     * @throws java.sql.SQLException if any.
     */
    public boolean isCaseSensitive(int i) throws SQLException {
        return columns.get(i).isCaseSensitive;
    }

    /**
     * <p>isSearchable.</p>
     *
     * @param i a int.
     * @return a boolean.
     * @throws java.sql.SQLException if any.
     */
    public boolean isSearchable(int i) throws SQLException {
        return columns.get(i).isSearchable;
    }

    /**
     * <p>isCurrency.</p>
     *
     * @param i a int.
     * @return a boolean.
     * @throws java.sql.SQLException if any.
     */
    public boolean isCurrency(int i) throws SQLException {
        return columns.get(i).isCurrency;
    }

    /**
     * <p>isNullable.</p>
     *
     * @param i a int.
     * @return a int.
     * @throws java.sql.SQLException if any.
     */
    public int isNullable(int i) throws SQLException {
        return columns.get(i).isNullable;
    }

    /**
     * <p>isSigned.</p>
     *
     * @param i a int.
     * @return a boolean.
     * @throws java.sql.SQLException if any.
     */
    public boolean isSigned(int i) throws SQLException {
        return columns.get(i).isSigned;
    }

    /**
     * <p>getColumnDisplaySize.</p>
     *
     * @param i a int.
     * @return a int.
     * @throws java.sql.SQLException if any.
     */
    public int getColumnDisplaySize(int i) throws SQLException {
        return columns.get(i).columnDisplaySize;
    }

    /**
     * <p>getColumnLabel.</p>
     *
     * @param i a int.
     * @return a {@link java.lang.String} object.
     * @throws java.sql.SQLException if any.
     */
    public String getColumnLabel(int i) throws SQLException {
        return columns.get(i).columnLabel;
    }

    /**
     * <p>getColumnName.</p>
     *
     * @param i a int.
     * @return a {@link java.lang.String} object.
     * @throws java.sql.SQLException if any.
     */
    public String getColumnName(int i) throws SQLException {
        return columns.get(i).columnName;
    }

    /**
     * <p>getSchemaName.</p>
     *
     * @param i a int.
     * @return a {@link java.lang.String} object.
     * @throws java.sql.SQLException if any.
     */
    public String getSchemaName(int i) throws SQLException {
        return columns.get(i).schemaName;
    }

    /**
     * <p>getPrecision.</p>
     *
     * @param i a int.
     * @return a int.
     * @throws java.sql.SQLException if any.
     */
    public int getPrecision(int i) throws SQLException {
        return columns.get(i).precision;
    }

    /**
     * <p>getScale.</p>
     *
     * @param i a int.
     * @return a int.
     * @throws java.sql.SQLException if any.
     */
    public int getScale(int i) throws SQLException {
        return columns.get(i).scale;
    }

    /**
     * <p>getTableName.</p>
     *
     * @param i a int.
     * @return a {@link java.lang.String} object.
     * @throws java.sql.SQLException if any.
     */
    public String getTableName(int i) throws SQLException {
        return columns.get(i).tableName;
    }

    /**
     * <p>getCatalogName.</p>
     *
     * @param i a int.
     * @return a {@link java.lang.String} object.
     * @throws java.sql.SQLException if any.
     */
    public String getCatalogName(int i) throws SQLException {
        return columns.get(i).catalogName;
    }

    /**
     * <p>getColumnType.</p>
     *
     * @param i a int.
     * @return a int.
     * @throws java.sql.SQLException if any.
     */
    public int getColumnType(int i) throws SQLException {
        return columns.get(i).columnType;
    }

    /**
     * <p>getColumnTypeName.</p>
     *
     * @param i a int.
     * @return a {@link java.lang.String} object.
     * @throws java.sql.SQLException if any.
     */
    public String getColumnTypeName(int i) throws SQLException {
        return columns.get(i).columnTypeName;
    }

    /**
     * <p>isReadOnly.</p>
     *
     * @param i a int.
     * @return a boolean.
     * @throws java.sql.SQLException if any.
     */
    public boolean isReadOnly(int i) throws SQLException {
        return columns.get(i).isReadOnly;
    }

    /**
     * <p>isWritable.</p>
     *
     * @param i a int.
     * @return a boolean.
     * @throws java.sql.SQLException if any.
     */
    public boolean isWritable(int i) throws SQLException {
        return columns.get(i).isWritable;
    }

    /**
     * <p>isDefinitelyWritable.</p>
     *
     * @param i a int.
     * @return a boolean.
     * @throws java.sql.SQLException if any.
     */
    public boolean isDefinitelyWritable(int i) throws SQLException {
        return columns.get(i).isDefinitelyWritable;
    }

    /**
     * <p>getColumnClassName.</p>
     *
     * @param i a int.
     * @return a {@link java.lang.String} object.
     * @throws java.sql.SQLException if any.
     */
    public String getColumnClassName(int i) throws SQLException {
        return columns.get(i).columnClassName;
    }

    /**
     * Finds the column within the table that matches the column name
     *
     * @param column Column name
     * @return Column ID
     * @throws java.sql.SQLException Errors
     */
    public int findColumn(String column) throws SQLException {
        int returnValue=-1;
        for (int i=1; i<=columnCount && returnValue==-1; i++) {
            if (Common.doStringsMatch(getColumnName(i),column))
                returnValue=i;
        }
        return returnValue;
    }


    /**
     * Class to store the meta data for a column
     */
    private static class ColumnMetaData {
        boolean isAutoIncrement;
        boolean isCaseSensitive;
        boolean isSearchable;
        boolean isCurrency;
        int isNullable;
        boolean isSigned;
        int columnDisplaySize;
        String columnLabel;
        String columnName;
        String schemaName;
        int precision;
        int scale;
        String tableName;
        String catalogName;
        int columnType;
        String columnTypeName;
        boolean isReadOnly;
        boolean isWritable;
        boolean isDefinitelyWritable;
        String columnClassName;

        /**
         * Construct a meta column object for the given column and meta results set
         *
         * @param meta Results set to use to get the meta data
         * @param i Column to get the data for
         * @throws SQLException Exception if there is a problem
         */
        private ColumnMetaData(ResultSetMetaData meta, int i) throws SQLException {
            isAutoIncrement = meta.isAutoIncrement(i);
            isCaseSensitive = meta.isCaseSensitive(i);
            isSearchable = meta.isSearchable(i);
            isCurrency = meta.isCurrency(i);
            isNullable = meta.isNullable(i);
            isSigned = meta.isSigned(i);
            columnDisplaySize = meta.getColumnDisplaySize(i);
            columnLabel = meta.getColumnLabel(i);
            columnName = meta.getColumnName(i);
            schemaName = meta.getSchemaName(i);
            precision = meta.getPrecision(i);
            scale = meta.getScale(i);
            tableName = meta.getTableName(i);
            catalogName = meta.getCatalogName(i);
            columnType = meta.getColumnType(i);
            columnTypeName = meta.getColumnTypeName(i);
            isReadOnly = meta.isReadOnly(i);
            isWritable = meta.isWritable(i);
            isDefinitelyWritable = meta.isDefinitelyWritable(i);
            columnClassName = meta.getColumnClassName(i);
        }
    }

}
