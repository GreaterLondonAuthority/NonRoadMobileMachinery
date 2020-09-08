/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

package com.pivotal.system.monitoring.jmx;

import com.pivotal.utils.PivotalException;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * Provides the main JMX MBean implementation for read-only configuration
 */
public class Performance implements PerformanceMBean {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Performance.class);
    private static final int CRITICAL_LOAD_AVERAGE = 8;
    private static final int PRESSURE_LOAD_AVERAGE = 4;

    /**
     * Registers a new MBean with the JMX infrastructure
     */
    public static void registerMBean() {

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName name = JMXUtils.getObjectName("Performance");
            mbs.registerMBean(new Performance(), name);
        }
        catch (Exception e) {
            logger.debug("Problem registering JMX MBean - {}", PivotalException.getErrorMessage(e));
        }
    }


    /**
     * Returns true if the server is starting to struggle - this is measured in terms
     * of the load average for the server exceeding the value of 4
     *
     * @return True if the server is in a bit of trouble
     */
    public boolean isUnderPressure() {
        return ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage() > PRESSURE_LOAD_AVERAGE;
    }

    /**
     * Returns true if the server is really up against it and without
     * remedial action will crash
     * In this situation, it will be amazing if the server even responds
     *
     * @return True if the server is about to go down
     */
    public boolean isCritical() {
        return ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage() > CRITICAL_LOAD_AVERAGE;
    }


}
