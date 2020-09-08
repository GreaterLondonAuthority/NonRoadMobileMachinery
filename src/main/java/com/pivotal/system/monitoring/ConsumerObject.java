/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

package com.pivotal.system.monitoring;

import java.util.Date;

/**
 * This is a simple wrapper around an object that will be used
 * in an execution thread
 */
public class ConsumerObject {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ConsumerObject.class);

    private String name;
    private Object object;
    private Date startTime = new Date();

    /**
     * Creates a new execution object to be used as a payload later
     * @param name Unique name of this task to carry out
     * @param object The object itself
     */
    public ConsumerObject(String name, Object object) {
        this.name = name;
        this.object = object;
    }

    /**
     * Get the name of this object
     * @return The name given at creation
     */
    public String getName() {
        return name;
    }

    /**
     * The object payload
     * @return Payload
     */
    public Object getObject() {
        return object;
    }

    /**
     * The creation timestamp of this instance
     * @return Timestamp
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * Returns the name prefixed by it's class name
     * @return Ful name of the object
     */
    public String getFullName() {
        return String.format("%s (%s)", object.getClass().getSimpleName(), name);
    }
}
