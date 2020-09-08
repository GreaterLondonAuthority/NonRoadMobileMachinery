/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

package com.pivotal.system.monitoring.jmx;

public interface TasksMBean {

    /**
     * Returns the number of tasks configured in the system
     *
     * @return Number of tasks
     */
    int getNumberOfTasks();

    /**
     * Returns the number of currently running tasks
     *
     * @return Number of running tasks
     */
    int getNumberOfRunningTasks();

}
