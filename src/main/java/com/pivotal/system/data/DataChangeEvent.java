/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.data;

import org.springframework.context.ApplicationEvent;

/**
 * Class used to describe an application data change event.
 * Used within the Spring Framework to route data change events to any other
 * components listening for this type of event
 */
public class DataChangeEvent extends ApplicationEvent {

    // The operation undertaken on the entity
    private String operation = null;

    public DataChangeEvent(String operation, Object entity) {
        super(entity);
        this.operation = operation;
    }
}
