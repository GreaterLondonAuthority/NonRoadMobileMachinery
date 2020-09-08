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

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Creates a holder for the request information associated with the request
 */
public class RequestInfo {

    private String pageName ="";
    private String subPageName ="";
    private String actionPageName ="";
    private String subActionPageName ="";
    private String appPath ="";
    private boolean isPost;
    private boolean isGet;

    /**
     * Creates a holder for the request information
     *
     * @param request Request object to interrogate
     */
    public RequestInfo(HttpServletRequest request) {
        appPath =request.getContextPath();
        String requestURI = request.getRequestURI();
        String appURI = requestURI.substring(appPath.length());

//        List<String> Pages= Common.splitToList(request.getRequestURI(), "/");
        List<String> Pages= Common.splitToList(appURI, "/");
//        if ("".equalsIgnoreCase(appPath)) {
            if (Pages.size()>1)
                pageName =Pages.get(1);
            if (Pages.size()>2)
                subPageName =Pages.get(2);
            if (Pages.size()>3)
                actionPageName =Pages.get(3);
            if (Pages.size()>4)
                subActionPageName =Pages.get(4);
//        }
//        else {
//            if (Pages.size()>2) {
//                pageName =Pages.get(2);
//                if (Pages.size()>3)
//                    subPageName =Pages.get(3);
//                if (Pages.size()>4)
//                    actionPageName =Pages.get(4);
//                if (Pages.size()>5)
//                    subActionPageName =Pages.get(5);
//            }
//        }
        isPost=Common.doStringsMatch(request.getMethod(), "post");
        isGet=Common.doStringsMatch(request.getMethod(), "get");
    }

    /**
     * Returns the page name
     *
     * @return Page name of the request
     */
    public String getPageName() {
        return pageName;
    }

    /**
     * Returns the subpage name
     *
     * @return Secondary page name of the request
     */
    public String getSubPageName() {
        return subPageName;
    }

    /**
     * Returns the action name
     *
     * @return Secondary page name of the request
     */
    public String getActionPageName() {
        return actionPageName;
    }

    /**
     * Returns the subaction name
     *
     * @return Secondary page name of the request
     */
    public String getSubActionPageName() {
        return subActionPageName;
    }

    /**
     * Returns the context path
     *
     * @return Context path of the request
     */
    public String getAppPath() {
        return appPath;
    }

    /**
     * Returns true if the page name matches the request page name
     *
     * @param pageName Name to check for
     *
     * @return True if there is a match
     */
    public boolean isPageName(String pageName) {
        return  Common.doStringsMatch(pageName, this.pageName);
    }

    /**
     * Returns true if any of the page names match the request page name
     *
     * @param pageNames Names to check for
     *
     * @return True if there is a match
     */
    public boolean isPageName(String... pageNames) {
        return  Common.doStringsMatch(pageName, pageNames);
    }

    /**
     * Returns true if the sub page name matches the request sub page name
     *
     * @param subPageName Name to check for
     *
     * @return True if there is a match
     */
    public boolean isSubPageName(String subPageName) {
        return Common.doStringsMatch(subPageName, this.subPageName);
    }

    /**
     * Returns true if any of the subpage names match the request page name
     *
     * @param subPageNames Names to check for
     *
     * @return True if there is a match
     */
    public boolean isSubPageName(String... subPageNames) {
        return Common.doStringsMatch(subPageName, subPageNames);
    }

    /**
     * Returns true if the action page name matches the request sub page name
     *
     * @param actionPageName Name to check for
     *
     * @return True if there is a match
     */
    public boolean isActionPageName(String actionPageName) {
        return Common.doStringsMatch(actionPageName, this.actionPageName);
    }

    /**
     * Returns true if any of the action page names match the request page name
     *
     * @param actionPageNames Names to check for
     *
     * @return True if there is a match
     */
    public boolean isActionPageName(String... actionPageNames) {
        return Common.doStringsMatch(actionPageName, actionPageNames);
    }

    /**
     * Returns true if the sub action page name matches the request sub page name
     *
     * @param subActionPageName Name to check for
     *
     * @return True if there is a match
     */
    public boolean isSubActionPageName(String subActionPageName) {
        return Common.doStringsMatch(subActionPageName, this.subActionPageName);
    }

    /**
     * Returns true if any of the subactionpage names match the request page name
     *
     * @param subActionPageNames Names to check for
     *
     * @return True if there is a match
     */
    public boolean isSubActionPageName(String... subActionPageNames) {
        return Common.doStringsMatch(subActionPageName, subActionPageNames);
    }

    /**
     * Returns true if the request is a post type
     *
     * @return True if post
     */
    public boolean isPost() {
        return isPost;
    }

    /**
     * Returns true if the request is a get type
     *
     * @return True if get
     */
    public boolean isGet() {
        return isGet;
    }
}
