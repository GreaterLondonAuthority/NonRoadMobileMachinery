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
import com.pivotal.system.data.cache.CacheEngine;
import com.pivotal.system.hibernate.entities.DatasourceEntity;

import java.util.List;
import java.util.Map;

/**
 * Extends the standard Database class to cater for opening
 * a datasource entity database
 */
public class DatabaseApp extends Database {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DatabaseApp.class);

    /**
     * Constructs a wrapper for an NRMM datasource
     *
     * @param dataSrc    NRMM entity
     */
    public DatabaseApp(DatasourceEntity dataSrc) {
        this.dataSrc = dataSrc;
        if (dataSrc != null) name = dataSrc.getName();
    }

    /**
     * Opens the connection and throws any exceptions
     *
     * @throws java.lang.Exception if any.
     */
    public void open() throws Exception {
        super.open();

        // Attempt to open the database described by the database entity
        // We will try for a future connection first but if that fails, we will
        // get a normal connection

        useFutureConnection = PoolBroker.getInstance().isPooled(dataSrc);
        if (!useFutureConnection)
            dbConnection = DataSourceUtils.getConnection(dataSrc);
    }

    /**
     * {@inheritDoc}
     *
     * Retrieve the return of the query from the cache
     * if cache is enabled in this datasource
     */
    public List<Map<String, Object>> getCachedQuery(String sql) {
        if (dataSrc.isUseCache()) {
            logger.debug("Retrieving from cache: {} {}", sql, CacheEngine.getCacheKey(dataSrc.getId(), sql));
            return CacheEngine.get(CacheEngine.getCacheKey(dataSrc.getId(), sql));
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * Put the return of the query into the cache
     * if cache is enabled in this datasource
     */
    public void putCachedQuery(String sql, List<Map<String, Object>> result) {
        if (dataSrc.isUseCache()) {
            logger.debug("Adding to cache: {}", CacheEngine.getCacheKey(dataSrc.getId(), sql));
            CacheEngine.put(CacheEngine.getCacheKey(dataSrc.getId(), sql), dataSrc.getCacheTimeout(), result);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Executes the statement against the database within an implicit
     * transaction if one is not in operation
     * Clears the cache if the update was successful
     */
    public boolean execute(String sql) {
        boolean returnValue = super.execute(sql);

        // Clear the cache if it is engaged

        if (returnValue && dataSrc.isUseCache()) {
            CacheEngine.clear(dataSrc.getId());
        }
        return returnValue;
    }

    /**
     * {@inheritDoc}
     *
     * Execute the current batch, inserting into the db the rows queued for the specified table.
     * Clears the cache if the update was successful
     */
    public boolean executeBatch(String table) {
        boolean returnValue = super.executeBatch(table);

        // Clear the cache if it is engaged

        if (returnValue && dataSrc.isUseCache()) {
            CacheEngine.clear(dataSrc.getId());
        }
        return returnValue;
    }

    /**
     * Returns the underlying data source that this connection is based on
     *
     * @return Datasource
     */
    public DatasourceEntity getDataSrc() {
        return dataSrc;
    }
}
