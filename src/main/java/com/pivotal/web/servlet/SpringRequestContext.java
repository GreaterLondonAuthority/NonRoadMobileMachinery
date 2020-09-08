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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.servlet.support.RequestContext;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * This class is a wrapper around the Spring RequestContext
 * This allows us to manage and react a little less harshly to issues
 * with the binding
 */
public class SpringRequestContext extends RequestContext {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SpringRequestContext.class);

    /**
     * Create a new RequestContext for the given request, using the given model attributes for Errors retrieval. <p>This
     * works with all View implementations. It will typically be used by View implementations. <p>If a ServletContext is
     * specified, the RequestContext will also work with a root WebApplicationContext (outside a DispatcherServlet).
     *
     * @param request        current HTTP request
     * @param response       current HTTP response
     * @param servletContext the servlet context of the web application (can be {@code null}; necessary for
     *                       fallback to root WebApplicationContext)
     * @param model          the model attributes for the current view (can be {@code null}, using the request attributes
     *                       for Errors retrieval)
     * @see org.springframework.web.context.WebApplicationContext
     * @see org.springframework.web.servlet.DispatcherServlet
     */
    public SpringRequestContext(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext, Map<String, Object> model) {
        super(request, response, servletContext, model);
    }

    /**
     * {@inheritDoc}
     *
     * Create a BindStatus for the given bind object, using the "defaultHtmlEscape" setting.
     */
    public BindStatus getBindStatus(String path, boolean htmlEscape) {
        try {
            return new SpringBindStatus(this, path, htmlEscape);
        }
        catch (Exception e) {
            logger.debug("Path isn't bound [{}]", path);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * Create a BindStatus for the given bind object, using the "defaultHtmlEscape" setting.
     */
    public BindStatus getBindStatus(String path) {
        try {
            return new SpringBindStatus(this, path, isDefaultHtmlEscape());
        }
        catch (Exception e) {
            logger.debug("Path isn't bound [{}]", path);
            return null;
        }
    }

    /**
     * Returns true if the path equates to a bound property or entity
     *
     * @param path the bean and property path for which values and errors will be resolved (e.g. "person.age" or "person")
     * @return a boolean.
     */
    public boolean isBound(String path) {
        if (path==null)
            return false;
        else if (path.contains("."))
            return getBindStatus(path)!=null;
        else
            return getErrors(path)!=null;
    }

    /**
     * Returns true if there are errors for the entity
     *
     * @param path Path to the entity e.g. "userentity"
     * @return True if there are errors for the pecified path
     */
    public boolean hasErrors(String path) {
        Errors errors = getErrors(path);
        return errors!=null && errors.hasErrors();
    }

    /**
     * Returns a list of errors which are not bound to any particular field
     *
     * @return List of error strings
     * @param path a {@link java.lang.String} object.
     */
    public List<ObjectError> getGlobalErrors(String path) {
        List<ObjectError> returnValue = null;
        if (!Common.isBlank(path)) {
            path = Common.getItem(path, "\\.", 0).toLowerCase();
            Errors errors = getErrors(path);
            if (errors!=null)
                returnValue =  errors.getGlobalErrors();
        }
        return returnValue;
}
}
