/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.servlet;

import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.*;
import com.pivotal.web.Constants;
import com.pivotal.web.controllers.AbstractController;
import org.apache.velocity.context.Context;
import org.springframework.ui.Model;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.view.AbstractTemplateView;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * A class that contains some general purpose Servlet related methods
 */
public class ServletHelperStateless extends HandlerInterceptorAdapter {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ServletHelperStateless.class);
    /**
     * Constant <code>FORM_ERRORS="FormErrors"</code>
     */
    public static final String FORM_ERRORS = "FormErrors";

    private static ThreadLocal<HttpServletRequest> threadRequest = new ThreadLocal<>();
    private static ThreadLocal<HttpServletResponse> threadResponse = new ThreadLocal<>();
    private static ServletContext context;

    private ServletHelperStateless() {
    }

    /**
     * Set the identity of the instance to the host name plus the context
     * path - if the hostname cannot be identified then use the IP address
     *
     * @param servletContext Servlet context to get unique name from
     */
    public static void setAppIdentity(ServletContext servletContext) {
        ServletHelperStateless.context = servletContext;

        // Allow the application name to be overridden from the default translations file

        String name= I18n.getString("default.application.name", true);
        if (name==null) {
            name = Constants.APPLICATION_NAME;
        }

        // Add on the context to the name just in case we're running multiple NRMM servers
        // on the same server

        name += context.getContextPath().replaceAll("(?is)[^a-z0-9]+", "-");

        Common.setAplicationName(name);
    }

    /**
     * Returns the servlet context that the servlet is running within
     *
     * @return Servlet context in use
     */
    public static ServletContext getServletContext() {
        return context;
    }

    /**
     * Returns the real path of the relative path
     *
     * @param path a <code>String</code> specifying a virtual path
     * @return Real path or null if context is null
     */
    public static String getRealPath(String path) {
        return getRealPath(context, path);
    }

    /**
     * Returns the real path of the relative path
     * This method takes care of the transition from Tomcat 7 to 8 whereby if the path
     * didn't exist, it now returns a null rather thn a full path
     * It also takes care of "relative" paths - Tomcat 8 doesn't treat these the same
     * as Tomcat 7 whereby they were based on the context root
     *
     * @param context Servlet context to use
     * @param path    a <code>String</code> specifying a virtual path
     * @return Real path or null if context is null
     */
    public static String getRealPath(ServletContext context, String path) {
        if (context == null) return null;
        else if (Common.isBlank(path)) return context.getRealPath("/");
        else {
            if (!path.startsWith("/")) {
                path = '/' + path;
            }
            String returnValue = context.getRealPath(path);
            if (returnValue == null) {
                returnValue = context.getRealPath("/") + path;
            }
            return returnValue;
        }
    }

    /**
     * Returns a unique identity for this instance of NRMM
     * This method takes care of the transition from Tomcat 7 to 8 whereby if the path
     * didn't exist, it now returns a null rather thn a full path
     *
     * @return String
     */
    public static String getAppIdentity() {
        return Common.getAplicationName();
    }

    /**
     * Safe way to send an error response to the browser
     *
     * @param errorCode Error code to send
     */
    public static void sendError(int errorCode) {
        sendError(getResponse(), errorCode, null);
    }

    /**
     * Safe way to send an error response to the browser
     *
     * @param errorCode Error code to send
     * @param error     Optional error message
     */
    public static void sendError(int errorCode, String error) {
        sendError(getResponse(), errorCode, error);
    }

    /**
     * Safe way to send an error response to the browser
     *
     * @param response  Response object to use
     * @param errorCode Error code to send
     * @param error     Optional error message
     */
    public static void sendError(HttpServletResponse response, int errorCode, String error) {
        if (response == null) {
            response = getResponse();
        }
        if (response != null) {
            try {
                if (Common.isBlank(error)) response.sendError(errorCode);
                else response.sendError(errorCode, error);
            }
            catch (Exception e) {
                logger.debug("Cannot send error - {}", PivotalException.getErrorMessage(e));
            }
        }
    }

    /**
     * Updates the passed model with the error
     *
     * @param model Model to update
     * @param error Error to add to the model
     * @return Returned error string
     */
    @SuppressWarnings("unchecked")
    public static String addError(Model model, String error) {
        if (model != null) {
            List<String> errors = (List<String>) model.asMap().get(FORM_ERRORS);
            if (Common.isBlank(errors)) errors = new ArrayList<>();
            errors.add(error);
            model.addAttribute(FORM_ERRORS, errors);
        }
        return error;
    }

    /**
     * Updates the passed model with the error
     *
     * @param model     Model to update
     * @param error     Error to add to the model
     * @param variables Variables to put into message
     * @return Returned error string
     */
    public static String addError(Model model, String error, Object... variables) {
        if (!Common.isBlank(error) && !Common.isBlank(variables))
            return addError(model, String.format(error, variables));
        else return addError(model, error);
    }

    /**
     * Returns true/false to indicate if the model contains any errors
     *
     * @param model Model to update
     * @return True if there are any errors
     */
    public static boolean hasErrors(Model model) {
        boolean returnValue = false;
        if (model != null) {
            returnValue = !Common.isBlank((Collection) model.asMap().get(FORM_ERRORS));
        }
        return returnValue;
    }

    /**
     * Returns true/false to indicate if the model contains any errors
     *
     * @param model Model to update
     * @return True if there are any errors
     */
    public static String getErrorsAsString(Model model) {
        String returnValue = null;
        if (hasErrors(model)) {
            returnValue = Common.join((Collection) model.asMap().get(FORM_ERRORS), "\n");

        }
        return returnValue;
    }

    /**
     * Checks to see if the field exists in the class
     *
     * @param classObject Class to inspect
     * @param name        Name of the field to look for
     * @return True/false
     */
    public static boolean fieldExists(Class classObject, String name) {
        boolean returnValue = false;
        try {
            classObject.getDeclaredField(name);
            returnValue = true;
        }
        catch (Exception e) {
            logger.debug("Field doesn't exist [{}]", name);
        }
        return returnValue;
    }

    /**
     * Checks to see if the field exists in the class
     *
     * @param modelAndView to put the attributes into
     * @param classObject  Class to inspect
     * @param setting      Name of the setting
     */
    public static void addSettingValue(Map<String, Object> modelAndView, Class classObject, String setting) {
        if (setting.matches("SETTING_.+") && !setting.matches("^SETTING_.+_DEFAULT")) {
            String defaultName = setting + "_DEFAULT";

            // Get the value of the setting field

            try {
                Field settingField = classObject.getDeclaredField(setting);
                String settingName = (String) settingField.get(settingField);
                if (fieldExists(classObject, defaultName)) {
                    Field tmp = classObject.getDeclaredField(defaultName);

                    // Get the correct setting

                    if (tmp.getType().equals(String.class))
                        modelAndView.put(settingName, HibernateUtils.getSystemSetting(settingName, (String) tmp.get(tmp)));

                    else if (tmp.getType().equals(Integer.class) || tmp.getType().equals(int.class))
                        modelAndView.put(settingName, HibernateUtils.getSystemSetting(settingName, (Integer) tmp.get(tmp)));

                    else if (tmp.getType().equals(Boolean.class) || tmp.getType().equals(boolean.class))
                        modelAndView.put(settingName, HibernateUtils.getSystemSetting(settingName, (Boolean) tmp.get(tmp)));

                    else if (tmp.getType().equals(StringBuilder.class))
                        modelAndView.put(settingName, HibernateUtils.getSystemSettingText(settingName, null));
                }
                else modelAndView.put(settingName, HibernateUtils.getSystemSetting(settingName, ""));
            }
            catch (Exception e) {
                logger.warn("Cannot replace/add setting attribute [{}] - {}", setting, PivotalException.getErrorMessage(e));
            }
        }
    }

    /**
     * Adds constants and their values to the context using reflection
     *
     * @param modelAndView Context to populate
     * @param fieldClass   Class from which to read the constants
     */
    public static void addConstants(Map<String, Object> modelAndView, Class fieldClass) {

        // Add on all the application specific values to the context
        // This is all the public static fields that are in uppercase

        List<Field> objFields = new ArrayList<>(Arrays.asList(fieldClass.getDeclaredFields()));
        for (Field objField : objFields) {
            if (Modifier.isPublic(objField.getModifiers()) && Modifier.isStatic(objField.getModifiers()) && objField.getName().equals(objField.getName().toUpperCase()))
                try {
                    modelAndView.put(objField.getName(), objField.get(objField.getClass()));

                    // If this is a Hibernate constant then get the values from the settings table

                    if (Common.doStringsMatch(fieldClass.getName(), HibernateUtils.class.getName()))
                        addSettingValue(modelAndView, fieldClass, objField.getName());
                }
                catch (Exception e) {
                    logger.error("Problem outputting static field values - {}", PivotalException.getErrorMessage(e));
                }
        }
    }

    /**
     * Creates a map of all the useful context values that we may have
     *
     * @param request   Web request
     * @param response  Web response
     * @param addStatic True if the NRMM settings constants etc should be added to the context
     * @return Map of context objects
     */
    public static Map<String, Object> getGenericObjects(HttpServletRequest request, HttpServletResponse response, boolean addStatic) {

        Map<String, Object> returnValue = new HashMap<>();

        // Add on all the application specific values to the context
        // This is all the public static fields that are in uppercase

        if (addStatic) {
            addConstants(returnValue, HibernateUtils.class);
            addConstants(returnValue, Constants.class);

            // Get a default velocity context

            Context tmp = VelocityUtils.getVelocityContext();
            for (Object key : tmp.getKeys())
                returnValue.put((String) key, tmp.get((String) key));
        }

        // Add on the request object

        returnValue.put("Request", request);
        returnValue.put("Response", response);
        returnValue.put("Flash", FlashSessionManager.class);
        returnValue.put("JsonMapper", JsonMapper.class);
        returnValue.put("Integer", Integer.class);
        returnValue.put("Math", Math.class);

        // Add the content manager (this is the integration point for plugins)

        //PMW returnValue.put("ContentManager",  PluginManager.getInstance().getContentManager());

        // Add the different enum types for choosing which libraries you want to include (CSS + JS)

        returnValue.put("INC", IncludeTypes.getIncludesAsMap());

        // Add on the session object

        if (request != null) {
//PMW            returnValue.put("Session", request.getSession(false));

            returnValue.put("AppPath", request.getContextPath());
        }

        // Add a map of the cookie values from the request

        if (request != null) {
            returnValue.put("Cookies", new CookieManager(request));

            // Add on some useful features of the request object

            returnValue.put("IsPostRequest", Common.doStringsMatch("post", request.getMethod()));

            // Add in all the parameters

            for (Object name : request.getParameterMap().keySet()) {
                String paramValue = request.getParameter(name.toString());
                if (!Common.isBlank(paramValue)) {
                    returnValue.put("REQ_" + name.toString().toLowerCase() + "_SQL", paramValue.replaceAll("'", "''"));
                }
                returnValue.put("REQ_" + name.toString().toLowerCase(), paramValue);
            }

            // Add in all the session attributes

//PMW             if (request.getSession() != null) {
//                HttpSession session = request.getSession();
//                Enumeration attributes = session.getAttributeNames();
//                while (attributes.hasMoreElements()) {
//                    String name = (String) attributes.nextElement();
//                    returnValue.put("SESS_" + name.toLowerCase(), session.getAttribute(name));
//                }
//            }
        }

        return returnValue;
    }

    /**
     * Returns true if the request is to the edit page
     *
     * @param request Request object
     * @return True if editing
     */
    public static boolean isEditing(HttpServletRequest request) {
        PathMatcher path = new AntPathMatcher();
        return path.matchStart("/*/edit", ServletHelperStateless.getPathInfo(request));
    }

    /**
     * Returns The first file that matches the pattern within the
     * context folder
     *
     * @param request Request object to get context from
     * @param folder  Folder to look inside
     * @param pattern File pattern
     * @return Translated path to the asset
     */
    public static String getAsset(HttpServletRequest request, String folder, String pattern) {
        String returnValue = null;
//PMW        if (request != null && request.getSession(false) != null) {
//            List<File> files = Common.listFiles(getRealPath(request.getSession().getServletContext(), folder), pattern, false, false);
//            if (!Common.isBlank(files))
//                returnValue = request.getContextPath() + folder + File.separator + files.get(0).getName();
//            else
//                logger.error("Cannot find asset [{}] in folder [{}]", pattern, getRealPath(request.getSession().getServletContext(), folder));
//        }
        return returnValue;
    }

    /**
     * Returns The first file that matches the pattern within the
     * context folder
     *
     * @param folder  Folder to look inside
     * @param pattern File pattern
     * @return Translated path to the asset
     */
    public static String getAsset(String folder, String pattern) {
        return getAsset(getRequest(), folder, pattern);
    }

    /**
     * {@inheritDoc}
     *
     * Spring MVC Interceptor
     * Updates the passed model with default values for the current session
     * and context.  Returns the updated model for chaining
     */
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        super.postHandle(request, response, handler, modelAndView);

        // We need to check to see if we need to actually do something
        // If we don't have a model/view to work on then the response must have been
        // handled - if we are redirecting then again, it's all been sorted for us via
        // the super call above

        if (modelAndView != null && modelAndView.getViewName() != null) {

            // If we are redirecting clear the model otherwise the model attributes
            // end up on the query string

            if (modelAndView.getViewName().matches("(?i)redirect *:.+")) {
                modelAndView.getModel().clear();
            }
            else {

                // Indicate to the view handler what the current namespace is

                request.setAttribute(AbstractController.NAMESPACE_ATTRIBUTE, AbstractController.getNamespace(((HandlerMethod) handler).getBean().getClass()));

                // Add on some normal goodies for all pages and set the
                // content type if it hasn't been specified already

                modelAndView.getModel().putAll(getGenericObjects(request, response, true));
                modelAndView.addObject("Context", modelAndView.getModel());
                if (Common.isBlank(response.getContentType())) response.setContentType("text/html;charset=UTF-8");

                // Add on our own version of the springMacroRequestContext

                modelAndView.addObject(AbstractTemplateView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE, new SpringRequestContext(request, response, getServletContext(), modelAndView.getModel()));
            }
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        super.afterCompletion(request, response, handler, ex);
        HibernateUtils.closeSession();
    }

    /**
     * Returns the full path of the application as known by the calling user
     *
     * @return Path e.g. http://localhost:8080/NRMM
     */
    public static String getFullAppPath() {
        final HttpServletRequest request = ServletHelperStateless.getRequest();
        String url = request.getRequestURL().toString();

        // SS-451
        // getRequestURI() returns everything after the protocol and name e.g. http://www.NRMM.com/[] which will
        // include the servlet path (this means in my case '/NRMM' gets cut off).
        // getPathInfo() returns the path after the servlet path (so http://www.NRMM.com/NRMM/[] or if you or on
        // root then it will be http://www.NRMM.com/[]).

        return url.substring(0, url.length() - (request.getPathInfo() != null ? request.getPathInfo().length() : 0));
    }

    /**
     * Creates a holder for the request information
     *
     * @return Request info
     */
    public static RequestInfo getRequestInfo() {
        return new RequestInfo(getRequest());
    }

    /**
     * Creates a holder for the request information
     *
     * @param request Request object to interrogate
     * @return Request info
     */
    public static RequestInfo getRequestInfo(HttpServletRequest request) {
        return new RequestInfo(request);
    }

    /**
     * Saves the request and response objects in local thread storage
     *
     * @param request  HTTP Request object
     * @param response HTTP Response object
     */
    public static void setThreadVariables(HttpServletRequest request, HttpServletResponse response) {
        threadRequest.set(request);
        threadResponse.set(response);
    }

    /**
     * Returns the thread stored request object
     *
     * @return HTTP Request object
     */
    public static HttpServletRequest getRequest() {
        return threadRequest.get();
    }

    /**
     * Returns the thread stored request object
     *
     * @return HTTP Response object
     */
    public static HttpServletResponse getResponse() {
        return threadResponse.get();
    }

    /**
     * Returns the thread stored session object
     *
     * @return HTTP Session object
     */
    public static HttpSession getSession() {
//PMW        HttpServletRequest request = getRequest();
//        return request == null ? null : request.getSession();
        return null;
    }

    /**
     * Simple way of getting a parameter value
     *
     * @param name Name of the parameter
     * @return Value
     */
    public static String getParameter(String name) {
        return getParameter(name, (String) null);
    }

    /**
     * Simple way of getting a parameter value or a default if it doesn't exist
     *
     * @param name         Name of the parameter
     * @param defaultValue Value to use if parameter doesn't exist
     * @return Value or default
     */
    public static String getParameter(String name, String defaultValue) {
        String returnValue = defaultValue;
        HttpServletRequest request = getRequest();
        if (request != null && !Common.isBlank(name)) {
            if (request.getParameterMap().containsKey(name)) returnValue = request.getParameter(name);
            else if (request.getParameterMap().containsKey(name.toLowerCase()))
                returnValue = request.getParameter(name.toLowerCase());
        }
        return returnValue;
    }

    /**
     * Simple way of getting a parameter value or a default if it doesn't exist
     *
     * @param name         Name of the parameter
     * @param defaultValue Value to use if parameter doesn't exist
     * @return Value or default
     */
    public static int getParameter(String name, int defaultValue) {
        String returnValue = getParameter(name, defaultValue + "");
        return Common.parseInt(returnValue);
    }

    /**
     * Simple way of getting a parameter value or a default if it doesn't exist
     *
     * @param name         Name of the parameter
     * @param defaultValue Value to use if parameter doesn't exist
     * @return Value or default
     */
    public static long getParameter(String name, long defaultValue) {
        String returnValue = getParameter(name, defaultValue + "");
        return Common.parseLong(returnValue);
    }

    /**
     * Simple way of getting a parameter value or a default if it doesn't exist
     *
     * @param name         Name of the parameter
     * @param defaultValue Value to use if parameter doesn't exist
     * @return Value or default
     */
    public static double getParameter(String name, double defaultValue) {
        String returnValue = getParameter(name, defaultValue + "");
        return Common.parseDouble(returnValue);
    }

    /**
     * Simple way of getting a parameter value or a default if it doesn't exist
     *
     * @param name         Name of the parameter
     * @param defaultValue Value to use if parameter doesn't exist
     * @return Value or default
     */
    public static Date getParameter(String name, Date defaultValue) {
        String returnValue = getParameter(name, defaultValue + "");
        return Common.parseDate(returnValue);
    }

    /**
     * Simple way of getting a parameter value or a default if it doesn't exist
     *
     * @param name         Name of the parameter
     * @param defaultValue Value to use if parameter doesn't exist
     * @return Value or default
     */
    public static boolean getParameter(String name, boolean defaultValue) {
        String returnValue = getParameter(name, defaultValue + "");
        return Common.parseBoolean(returnValue);
    }

    /**
     * Simple way of determining if the parameter exists
     *
     * @param name Name of the parameter
     * @return True if the parameter exists
     */
    public static boolean parameterExists(String name) {
        HttpServletRequest request = getRequest();
        return request != null && !Common.isBlank(name) && request.getParameterMap().containsKey(name);
    }

    /**
     * Simple way of getting a parameter value or a default if it doesn't exist
     *
     * @param request      Request to check
     * @param name         Name of the parameter
     * @param defaultValue Value to use if parameter doesn't exist
     * @return Value or default
     */
    public static String getParameter(HttpServletRequest request, String name, String defaultValue) {
        String returnValue;
        if (request != null && !Common.isBlank(name) && request.getParameterMap().containsKey(name)) {
            returnValue = request.getParameter(name);
        }
        else returnValue = defaultValue;

        return returnValue;
    }

    /**
     * Simple way of determining if the parameter exists
     *
     * @param request Request to check
     * @param name    Name of the parameter
     * @return True if the parameter exists
     */
    public static boolean parameterExists(HttpServletRequest request, String name) {
        return request != null && !Common.isBlank(name) && request.getParameterMap().containsKey(name);
    }

    /**
     * Cleans up thread local values so that this thread can be re-used with
     * cross pollination
     */
    public static void cleanUpThreadLocals() {
        threadRequest.remove();
        threadResponse.remove();
    }
    /**
     * Safe way to get path info if it is null
     *
     * @return pathinfo or blank string if it is null
     *
     */
    public static String getPathInfo() {
        return getPathInfo(getRequest());
    }

    /**
     * Safe way to get path info if it is null
     *
     * @param request object to use
     *
     * @return pathinfo or blank string if it is null
     *
     */
    public static String getPathInfo(HttpServletRequest request) {
        String returnValue = "";

        if (request != null && request.getPathInfo() != null)
            returnValue = request.getPathInfo();

        return returnValue;
    }
}
