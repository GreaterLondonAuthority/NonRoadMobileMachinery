/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.servlet;

import com.pivotal.utils.Common;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Used to initialise parts of the application prior to anything else happening
 * Mostly used as a means of setting up the logging sub-system before anything
 * attempts to try and use it - referred to in the web.xml
 */
@Component
public class PreInitialisation implements ServletContextListener {

    /**
     * {@inheritDoc}
     *
     * Notification that the web application initialization process is starting.
     * All ServletContextListeners are notified of context initialization before
     * any filter or servlet in the web application is initialized.
     */
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        Initialisation.initLoggingEnv(servletContextEvent.getServletContext(), null);
        Common.setAplicationName(Common.getAplicationName());
    }

    /**
     * {@inheritDoc}
     *
     * Notification that the servlet context is about to be shut down.
     * All servlets and filters have been destroy()ed before any
     * ServletContextListeners are notified of context destruction.
     */
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
    }

}
