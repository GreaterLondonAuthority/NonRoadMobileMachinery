/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

package com.pivotal.system.monitoring.jmx;

import com.pivotal.reporting.scheduler.Job;
import com.pivotal.reporting.scheduler.ScheduleMonitor;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * Provides the main JMX MBean implementation for read-only performance
 */
public class Tasks implements TasksMBean {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Tasks.class);

    /**
     * Registers a new MBean with the JMX infrastructure
     */
    public static void registerMBean() {

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName name = JMXUtils.getObjectName("Tasks");
            mbs.registerMBean(new Tasks(), name);
        }
        catch (Exception e) {
            logger.debug("Problem registering JMX MBean - {}", PivotalException.getErrorMessage(e));
        }
    }

    /**
     * Returns the number of tasks configured in the system
     *
     * @return Number of tasks
     */
    public int getNumberOfTasks() {
        List<BigInteger> values = HibernateUtils.selectSQLEntities("select count(*) from scheduled_task");
        return values.get(0).intValue();
    }

    /**
     * Returns the number of currently running tasks
     *
     * @return Number of running tasks
     */
    public int getNumberOfRunningTasks() {
        Map<Integer,Job> jobs= ScheduleMonitor.getRunningTaskList();
        return Common.isBlank(jobs)?0:jobs.size();
    }

}
