/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.filter;

import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;
import com.pivotal.web.servlet.ServletHelper;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <p>This intercepts the request and response objects and stores them within
 * the {@link com.pivotal.web.servlet.ServletHelper} for the application.</p>
 * <p>They had to be moved from the {@link com.pivotal.web.servlet.Dispatcher}
 * because that is only for spring mvc requests and the plugins do not go through
 * this dispatcher meaning that any classes that depend on the request within
 * the {@link com.pivotal.web.servlet.Dispatcher} will not work correctly. By
 * collecting the variables within the filter we are guaranteed to collect them for all
 * requests.</p>
 *
*/
@SuppressWarnings("unused")
public class RequestFilter implements Filter {

    // Get access to the logger
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RequestFilter.class);

    private String encoding;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        encoding = filterConfig.getInitParameter("encoding");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws MaxUploadSizeExceededException,IOException, ServletException {
        try {
            ServletHelper.setThreadVariables((HttpServletRequest) request, (HttpServletResponse) response);

            // Set the encoding

            if (this.encoding != null && request.getCharacterEncoding() == null) {
                request.setCharacterEncoding(this.encoding);
                response.setCharacterEncoding(this.encoding);
            }
            filterChain.doFilter(request, response);

            // Check for where the character encoding has been reset

            if (logger.isDebugEnabled()) {
                if (Common.doStringsMatch(response.getCharacterEncoding(), "iso-8859-1")) {
                    try {
                        logger.debug("Sending {} response to {}", response.getCharacterEncoding(), ((HttpServletRequest) request).getRequestURL());
                    }
                    catch (Exception e) {
                        logger.debug("Probably not an HttpServletRequest");
                    }
                }
            }
        }
        catch(Exception e) {
            logger.error(PivotalException.getErrorMessage(e));
        }
        finally {
            HibernateUtils.closeSession();
            ServletHelper.cleanUpThreadLocals();
        }
    }

    @Override
    public void destroy() {

        // Just clean up if the filter is destroyed

        ServletHelper.cleanUpThreadLocals();
    }

}
