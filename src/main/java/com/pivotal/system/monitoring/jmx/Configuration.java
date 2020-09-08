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
import com.pivotal.utils.PivotalException;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * Provides the main JMX MBean implementation for read-only configuration
 */
public class Configuration implements ConfigurationMBean {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Configuration.class);

    /**
     * Registers a new MBean with the JMX infrastructure
     */
    public static void registerMBean() {

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName name = JMXUtils.getObjectName("Configuration");
            mbs.registerMBean(new Configuration(), name);
        }
        catch (Exception e) {
            logger.debug("Problem registering JMX MBean - {}", PivotalException.getErrorMessage(e));
        }
    }

    /**
     * Returns true if the scheduler is enabled
     *
     * @return True if enabled
     */
    public boolean isSchedulerEnabled() {
        return HibernateUtils.getSystemSetting(HibernateUtils.SETTING_SCHEDULING_ENABLED, HibernateUtils.SETTING_SCHEDULING_ENABLED_DEFAULT);
    }

    /**
     * Returns true if the publisher is enabled
     *
     * @return True if the publisher is enabled
     */
    public boolean isPublisherEnabled() {
        return HibernateUtils.getSystemSetting(HibernateUtils.SETTING_PUBLISHING_ENABLED, HibernateUtils.SETTING_PUBLISHING_ENABLED_DEFAULT);
    }

}
