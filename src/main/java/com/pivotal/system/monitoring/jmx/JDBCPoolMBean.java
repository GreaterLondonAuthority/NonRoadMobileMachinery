/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.monitoring.jmx;

public interface JDBCPoolMBean {

    // NRMM JDBC Connection Pool

    /**
     * Returns the number of JDBC connections that are currently being used
     * by the system
     *
     * @return Number of active pool connections
     */
    int getPoolActiveCount();

    /**
     * Returns the number of idle JDBC connections available in the pool
     *
     * @return Number of idle connections
     */
    int getPoolIdleCount();

    /**
     * Returns the maximum number of active JDBC connections allowed in the pool
     *
     * @return Max active
     */
    int getPoolMaxActive();

    /**
     * Returns true if the JDBC pool is getting low on resources
     *
     * @return True if the pool performance is poor
     */
    boolean isPoolPerformancePoor();

    /**
     * Returns true if the JDBC pool is critically low on free engines
     *
     * @return True if the pool performance is critical
     */
    boolean isPoolPerformanceCritical();

}
