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
import com.pivotal.web.servlet.VelocityResourceCache;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * Provides the main JMX MBean implementation for read-only performance
 */
public class Templating implements TemplatingMBean {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Templating.class);

    private VelocityResourceCache.VelocityCacheStats cache;

    /**
     * Registers a new MBean with the JMX infrastructure
     */
    public static void registerMBean() {

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName name = JMXUtils.getObjectName("Templating");
            mbs.registerMBean(new Templating(), name);
        }
        catch (Exception e) {
            logger.debug("Problem registering JMX MBean - {}", PivotalException.getErrorMessage(e));
        }
    }

    /**
     * Returns the number of cached objects in the Velocity templates cache
     *
     * @return Number of objects
     */
    public int getVelocityCacheObjectCount() {
        init();
        return cache.getSize();
    }

    /**
     * Returns the number of successful hits/second on the Velocity template cache
     *
     * @return Hits per second
     */
    public double getVelocityCacheHitRate() {
        init();
        return cache.getHitRate();
    }

    /**
     * Returns the number of unsuccessful hits/second on the Velocity template cache
     *
     * @return Misses per second
     */
    public double getVelocityCacheMissRate() {
        init();
        return cache.getMissRate();
    }

    /**
     * Returns true if the Velocity cache miss rate is causing concern
     *
     * @return True if Cache performance is poor
     */
    public boolean isVelocityCachePerformancePoor() {
        init();
        return cache.getMisses() / cache.getHits() > 0.2;
    }

    /**
     * Returns true if the Velocity cache miss rate is critical
     *
     * @return True if the cache performance is critical
     */
    public boolean isVelocityCachePerformanceCritical() {
        init();
        return cache.getMisses() / cache.getHits() > 0.5;
    }

    /**
     * Initialises the resources
     */
    private void init() {
        if (cache==null) {
            cache = VelocityResourceCache.getStats();
        }
    }
}
