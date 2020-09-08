/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.servlet;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filter to add a cache control definition header on the response headers.
 * The instruction will be passed by the filter configuration in the web.xml
 * To make sure it works in old browsers, there's a default 'Now' plus 5 days 'Expires' header being passed along
 */
public class ResponseCachingFilter implements Filter {
    private final static String HEADER_GET_KEY = "Cache-Control";
    private final static String HEADER_PRAGMA = "Pragma";
    private final static String HEADER_EXPIRES = "Expires";

    private String cacheLifeTimeInstruction = null;

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        if (null != cacheLifeTimeInstruction) {
            ((HttpServletResponse) res).setHeader(HEADER_GET_KEY, cacheLifeTimeInstruction);
            ((HttpServletResponse) res).setHeader(HEADER_PRAGMA, null);
            final int CACHE_DURATION_IN_SECOND = 60 * 60 * 24 * 5; // 5 days
            final long CACHE_DURATION_IN_MS = CACHE_DURATION_IN_SECOND * 1000;
            long now = System.currentTimeMillis();
            ((HttpServletResponse) res).setDateHeader(HEADER_EXPIRES, now + CACHE_DURATION_IN_MS);
        }

        chain.doFilter(req, res);
    }

    public void init(FilterConfig config) throws ServletException {
        cacheLifeTimeInstruction = config.getInitParameter(HEADER_GET_KEY);
    }

    public void destroy() {
        cacheLifeTimeInstruction = null;
    }

}
