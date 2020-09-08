/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.monitoring.jmx;

public interface ConfigurationMBean {

    /**
     * Returns true if the scheduler is enabled
     *
     * @return True if enabled
     */
    boolean isSchedulerEnabled();

    /**
     * Returns true if the publisher is enabled
     *
     * @return True if the publisher is enabled
     */
    boolean isPublisherEnabled();

}
