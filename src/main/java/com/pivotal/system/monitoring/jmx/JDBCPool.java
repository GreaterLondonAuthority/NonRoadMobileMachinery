/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

package com.pivotal.system.monitoring.jmx;

import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.hibernate.utils.AppDataSource;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * Provides the main JMX MBean implementation for read-only performance
 */
public class JDBCPool implements JDBCPoolMBean {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JDBCPool.class);

    private AppDataSource pool;

    /**
     * Initialise the class
     */
    public JDBCPool() {
        try {
            pool = HibernateUtils.getDataSource();
        }
        catch (Exception e) {
            logger.error("Problem registering JMX MBean - {}", PivotalException.getErrorMessage(e));
        }
    }

    /**
     * Registers a new MBean with the JMX infrastructure
     */
    public static void registerMBean() {

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName name = JMXUtils.getObjectName("ConnectionPools", Common.getAplicationName());
            mbs.registerMBean(new JDBCPool(), name);
        }
        catch (Exception e) {
            logger.debug("Problem registering JMX MBean - {}", PivotalException.getErrorMessage(e));
        }
    }

    /**
     * Returns the number of JDBC connections that are currently being used
     * by the system
     *
     * @return Number of active pool connections
     */
    public int getPoolActiveCount() {
        return pool.getNumActive();
    }

    /**
     * Returns the number of idle JDBC connections available in the pool
     *
     * @return Number of idle connections
     */
    public int getPoolIdleCount() {
        return pool.getNumIdle();
    }

    /**
     * Returns the maximum number of active JDBC connections allowed in the pool
     *
     * @return Max active
     */
    public int getPoolMaxActive() {
        return pool.getMaxActive();
    }

    /**
     * Returns true if the JDBC pool is getting low on resources
     *
     * @return True if the pool performance is poor
     */
    public boolean isPoolPerformancePoor() {
        return pool.getNumActive() / pool.getMaxActive() > 0.7;
    }

    /**
     * Returns true if the JDBC pool is critically low on free engines
     *
     * @return True if the pool performance is critical
     */
    public boolean isPoolPerformanceCritical() {
        return pool.getNumActive() / pool.getMaxActive() > 0.9;
    }

}
