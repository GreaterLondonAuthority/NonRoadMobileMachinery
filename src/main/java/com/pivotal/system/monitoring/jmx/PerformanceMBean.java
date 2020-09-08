/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.monitoring.jmx;

public interface PerformanceMBean {

    /**
     * Returns true if the server is starting to struggle - this is measured in terms
     * of the load average for the server exceeding the value of 4
     *
     * @return True if the server is in a bit of trouble
     */
    boolean isUnderPressure();

    /**
     * Returns true if the server is really up against it and without
     * remedial action will crash
     *
     * @return True if the server is about to go down
     */
    boolean isCritical();

}
