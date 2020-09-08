/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 * MODULE NAME:          com.pivotal.web.servlet
 *
 * MODULE TYPE:          Java Class
 *
 * FILE NAME:            Dispatcher.java
 *
 *****************************************************************************
 */
package com.pivotal.web.servlet;

import com.pivotal.system.hibernate.entities.UserEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.monitoring.EventMonitor;
import com.pivotal.system.security.UserManager;
import com.pivotal.utils.Common;
import com.pivotal.utils.HttpUtils;
import com.pivotal.utils.I18n;
import com.pivotal.utils.PivotalException;
import com.pivotal.utils.browser.Browser;
import com.pivotal.web.Constants;
import com.pivotal.web.controllers.AbstractAdminController;
import com.pivotal.web.controllers.utils.Authorise;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

/**
 * Main entry point for all requests
 */
public class Dispatcher extends DispatcherServlet {

    // Header set by jQuery when doing AJAX requests:
    private static final String JQUERY_HEADER_NAME = "x-requested-with";
    private static final String JQUERY_HEADER_VALUE = "XMLHttpRequest";

    static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Dispatcher.class);

    /** Constant <code>REQUEST_START_TIME="RequestStartTime"</code> */
    public static final String REQUEST_START_TIME = "RequestStartTime";
    /** Constant <code>TARGET_URL="Target"</code> */
    public static final String TARGET_URL = "Target";
    private static final long serialVersionUID = 3535082542736659353L;

    /**
     * {@inheritDoc}
     *
     * This method simply intercepts all requests to the system
     * We clear the hibernate cache for this thread so that there isn't any
     * chance of old objects from previous requests polluting our data
     */
    @Override
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {

        // Get the name of the thread and store the objects in the thread

        String name=Thread.currentThread().getName().split(" *- *")[0];

        // Create a hibernate session to use right at the start

        Date startTime = new Date();
        request.setAttribute(REQUEST_START_TIME, startTime);
        HibernateUtils.getCurrentSession();

        // For robustness we need to make sure the rest is in a try/catch block
        // to insure that no rogue uncaught exception causes a Hibernate session leak

        try {

            // Get the page name and sub page name from the URI

            RequestInfo reqInfo=ServletHelper.getRequestInfo(request);

            if (!Common.isBlank(request.getParameter("autologin"))) {
                UserManager.setCurrentUser(HibernateUtils.getEntity(UserEntity.class, "admin"));
            }

            // Set the current username so that we can collect this elsewhere during the
            // operation of the thread
            if (UserManager.getCurrentUserName() == null)
                Thread.currentThread().setName(name + " - " + ServletHelper.getPathInfo(request) + " - " + HttpUtils.getAddressFromRequest(request).toLowerCase());
            else
                Thread.currentThread().setName(name + " - " + UserManager.getCurrentUserName() + " - " + HttpUtils.getAddressFromRequest(request).toLowerCase());

            // Now check that the user is either
            // logging out
            // running a restful service
            // trying login
            // running an action
            // already logged in
            if (!Common.isBlank(ServletHelper.getPathInfo(request)) && (Common.doStringsMatch(request.getPathInfo(), "/logout") || request.getPathInfo().startsWith("/action") || request.getPathInfo().startsWith("/rest") || request.getPathInfo().startsWith("/image") || reqInfo.isPageName("login") || UserManager.isUserLoggedIn())) {
                super.doDispatch(request, response);
            }

            // Not logged in, so redirect to the login page

            else if (!UserManager.isUserLoggedIn()) {
                if (!reqInfo.isPageName("login") || reqInfo.isSubPageName("heartbeat")) {

                    // Store this URL if it's ok to - i.e. it's not a request for JSON etc.
                    if (!Common.doStringsMatch(request.getHeader(JQUERY_HEADER_NAME), JQUERY_HEADER_VALUE)) {
                        request.getSession().setAttribute(TARGET_URL, request.getRequestURI().substring(request.getContextPath().length()) + (request.getQueryString()==null?"":("?" + request.getQueryString())));
                    }
                    response.sendRedirect(reqInfo.getAppPath() + "/login");
                }
            }

            // Log the access attempt and send an error

            else {
                logger.error("User [{}] attempted to access [{}] from [{}]", UserManager.getCurrentUserName(), ServletHelper.getPathInfo(request), request.getRemoteHost());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, I18n.translate("global.error.unauthorised.message"));
            }
        }

        // If the client goes away during a response, that's fine, just catch it so that
        // the logs don't fill up

        catch (ClientAbortException e) {
            logger.debug("The client has cut us off short!");
        }

        // Any other exception is a major issue

        catch (Exception e) {
            logger.error("Error in Dispatcher : ", e);
            HandlerExecutionChain mappedHandler = getHandler(request);
            if (mappedHandler != null && mappedHandler.getHandler() != null) {
                HandlerMethod method = (HandlerMethod) mappedHandler.getHandler();
                logger.error("Something horrible happened in {}.{} servicing {}?{} and was not caught - {}", method.getBeanType().getSimpleName(), method.getMethod().getName(),
                                                                                                          request.getRequestURI(), request.getQueryString(),
                                                                                                          PivotalException.getErrorMessage(e, PivotalException.getStackTrace(e)));
            }
            else {
                logger.error("Something horrible happened servicing {} and was not caught - {}", request.getRequestURI(), PivotalException.getErrorMessage(e, PivotalException.getStackTrace(e)));
            }
        }
        finally {

            // Reset the thread name and cleanup thread local variables

            Thread.currentThread().setName(name);
        }

        // Add a monitoring timer event to the queue

        EventMonitor.addEvent(EventMonitor.EVENT_DISPATCHER, EventMonitor.EVENT_TYPE_DISPATCHER_REQUEST, new Date().getTime() - startTime.getTime());

        // Close the session if it hasn't already been

        HibernateUtils.closeSession();
    }

    /**
     * This method is used to check to see if the user is allowed access to the
     * resource that they are attempting to get to.
     * It does this by finding the method that will be used to handle the request
     * and checks its Authorise annotation.  If there isn't one, it then checks
     * the Class level annotation.  If there isn't one here. then only Administrators
     * are allowed access.  In fact, the whole thing is bypassed if the current
     * user is an administrator.
     * @return True if the user is authorised to get the resource
     */
    private boolean isUserAuthorised(HttpServletRequest request) {

        // Are we already logged in or internally authenticated

//        boolean returnValue = (UserManager.isUserLoggedIn() && UserManager.getCurrentUser().isAdministrator()) || UserManager.authenticateInternal();
        boolean returnValue = UserManager.isUserLoggedIn() || UserManager.authenticateInternal();

        // If we can bypass the checking

        if (!returnValue) {

            // Security may not be turned on and we may not be logged in
            // If we don't have any authentication configured then let the user through

            if (!UserManager.isUserLoggedIn() && UserManager.isNoAuthentication()) {
                UserEntity user = UserManager.getAnonymousUser();
                UserManager.setCurrentUser(user);
                UserManager.saveUserInformation();
                returnValue = true;
            }
            else {

                returnValue = true;
                // Get the SpringMVC handler method from it's map - if there isn't one then
                // the system will barf up anyway

                try {
                    HandlerExecutionChain mappedHandler = getHandler(request);
                    if (mappedHandler != null && mappedHandler.getHandler() != null) {
                        HandlerMethod method = (HandlerMethod) mappedHandler.getHandler();

                        // Get any annotation on the method and if that doesn't exist, get
                        // it from the Class

                        Authorise anno = method.getMethodAnnotation(Authorise.class);
                        if (anno == null) {
                            anno = method.getBeanType().getAnnotation(Authorise.class);
                        }

                        // If we have an annotation then lets see if it allows us access

                        if (anno != null) {
                            if (Constants.inIde() && anno.inIde()) {
                                returnValue = true;
                            }

                            // Check to see if they are not logged in but are allowed access to this resource
                            // This is either because the resource allows this access, or the internal browser
                            // is attempting to get access

                            else if (!UserManager.isUserLoggedIn()) {
                                returnValue = anno.notLoggedIn() ||
                                        (request.getRequestURL().toString().startsWith(Constants.getLocalAddress()) &&
                                                !Common.isBlank(request.getHeader(Browser.INTERNAL_REQUEST_HEADER)));
                            }
                            else if (Common.isBlank(anno.view()) && Common.isBlank(anno.edit())) {
                                returnValue = true;
                            }

                            // If the user has edit access then they can do what they want
                            // otherwise it's a little more complicated

                            else {

                                // If the user has edit access

//                                if (!Common.isBlank(anno.edit())) {
//                                    returnValue = UserManager.getCurrentUser().hasPrivilege(anno.edit());
//                                }

                                // If the edit access isn't allowed we need to check to see what the
                                // user is trying to do - if they are trying to edit something, then
                                // no deal, otherwise they may have access through the 'view' values

                                if (!returnValue && !Common.isBlank(anno.edit())) {

                                    // Are we attempting to edit something

                                    boolean editing = false;
                                    RequestInfo info = ServletHelper.getRequestInfo(request);
                                    if (AbstractAdminController.class.isAssignableFrom(method.getBean().getClass())) {
                                        editing = Common.doStringsMatch(method.getMethod().getName(), "delete", "edit", "settings");
                                    }
                                    else if (!info.isGet()) {
                                        // TODO this will almost certainly need refining
                                        editing = true;
                                    }

                                    // If we're not editing then check the 'view' values

//                                    if (!editing) {
//                                        returnValue = UserManager.getCurrentUser().hasPrivilege(anno.view());
//                                    }
                                }
                            }
                        }
                    }
                }
                catch (Exception e) {
                    if ("HEAD".equalsIgnoreCase(request.getMethod()))
                        logger.debug("Problem getting Spring request handler - {}", PivotalException.getErrorMessage(e));
                    else
                        logger.error("Problem getting Spring request handler - {}", PivotalException.getErrorMessage(e));
                }
            }
        }

        return returnValue;
    }
}
