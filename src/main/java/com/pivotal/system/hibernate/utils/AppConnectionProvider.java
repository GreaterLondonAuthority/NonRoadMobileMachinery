/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.hibernate.utils;

import com.pivotal.utils.PivotalException;
import org.hibernate.HibernateException;
import org.hibernate.connection.DatasourceConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is a connection provider that overrides the standard JNDI version
 * to provide the connections so that we can access the data source explicitly
 * and force a hard-close
 */
public class AppConnectionProvider extends DatasourceConnectionProvider {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AppConnectionProvider.class);

    AppDataSource targetSource;
    static long opens = 0;
    static long closes = 0;
    static Map<Long, StatsBlock> borrowers = new ConcurrentHashMap<>();

    @Override
    public void configure(Properties props) throws HibernateException {
        try {
            // Now instantiate our lazy provider to override it

            targetSource = HibernateUtils.getDataSource();
            setDataSource(targetSource);
        }
        catch (Exception e) {
            logger.error("Cannot configure datasource - {}", PivotalException.getErrorMessage(e));
        }
    }

    @Override
    public void close() {
        super.close();
        targetSource.close(true);
    }

    @Override
    public Connection getConnection() throws SQLException {
        opens++;
        Long id = Thread.currentThread().getId();
        if (borrowers.containsKey(id)) {
            borrowers.get(id).count++;
        }
        else {
            borrowers.put(id, new StatsBlock(Thread.currentThread()));
        }
        return super.getConnection();
    }

    @Override
    public void closeConnection(Connection conn) throws SQLException {
        closes++;
        Long id = Thread.currentThread().getId();
        if (borrowers.containsKey(id)) {
            borrowers.get(id).count--;
            if (borrowers.get(id).count<=0) {
                borrowers.remove(id);
            }
        }
        else {
            logger.debug("Connection being returned but no borrower exists");
        }
        super.closeConnection(conn);
    }

    /**
     * Returns the number of opens there have been
     * @return Number of opens
     */
    public static long getOpens() {
        return opens;
    }

    /**
     * Returns the number of closes there have been
     * @return Number of closes
     */
    public static long getCloses() {
        return closes;
    }

    /**
     * The list of current borrowers
     * @return Map of borrowers
     */
    public static Map<Long, StatsBlock> getBorrowers() {
        return new TreeMap<>(borrowers);
    }

    /**
     * Clears the cache of stats
     */
    public static void clearStats() {
        opens = 0;
        closes = 0;
        borrowers.clear();
    }

    /**
     * A simple holder class for the borrower information
     */
    public class StatsBlock {
        Long id;
        String name;
        Long count=1L;

        /**
         * Create a borrower for the given thread
         * @param thread Thread to derive the information from
         */
        private StatsBlock(Thread thread) {
            this.id = thread.getId();
            this.name = thread.getName();
        }

        /**
         * Name of the thread/borrower
         * @return Name of the thread
         */
        public String getName() {
            return name;
        }

        /**
         * Number of connections this borrower has
         * @return Numebr of connections
         */
        public Long getCount() {
            return count;
        }

        /**
         * ID of the thread/borrower
         * @return ID of the thread
         */
        public Long getId() {
            return id;
        }
    }
}
