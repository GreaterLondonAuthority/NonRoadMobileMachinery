/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.servlet;

import org.apache.catalina.servlets.DefaultServlet;

import javax.servlet.ServletException;

/**
 * This is simply a wrapper around the default servlet and allows
 * us to set values that otherwise would have to defined in the
 * Tomcat installed web.xml
 */
public class StaticServlet extends DefaultServlet {

    @Override
    public void init() throws ServletException {
        super.init();

        // Override the defaults that are set in the Tomcat installation web.xml

        output = 1024 * 30;
        sendfileSize = 1024 * 30;
        fileEncoding = "UTF-8";
    }
}
