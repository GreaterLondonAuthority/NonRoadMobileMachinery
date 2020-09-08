/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.monitoring.jmx;

public interface TemplatingMBean {

    /**
     * Returns the number of cached objects in the Velocity templates cache
     *
     * @return Number of objects
     */
    int getVelocityCacheObjectCount();

    /**
     * Returns the number of successful hits/second on the Velocity template cache
     *
     * @return Hits per second
     */
    double getVelocityCacheHitRate();

    /**
     * Returns the number of unsuccessful hits/second on the Velocity template cache
     *
     * @return Misses per second
     */
    double getVelocityCacheMissRate();

    /**
     * Returns true if the Velocity cache miss rate is causing concern
     *
     * @return True if Cache performance is poor
     */
    boolean isVelocityCachePerformancePoor();

    /**
     * Returns true if the Velocity cache miss rate is critical
     *
     * @return True if the cache performance is critical
     */
    boolean isVelocityCachePerformanceCritical();


}
