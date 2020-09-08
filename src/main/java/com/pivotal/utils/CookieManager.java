/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils;

import org.springframework.util.LinkedCaseInsensitiveMap;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.HttpCookie;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *  Provides a simple way of handling the cookie streams associated with UrlConnection
 */
public class CookieManager {

    private Map<String,HttpCookie> store = new LinkedCaseInsensitiveMap<>();
    private String domain;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CookieManager.class);
    private static final String SET_COOKIE = "Set-Cookie";
    private static final String COOKIE = "Cookie";
    private static final String DATE_FORMAT = "EEE, dd-MMM-yyyy HH:mm:ss z";
    private static final char DOT = '.';

    private static DateFormat dateFormat=new SimpleDateFormat(DATE_FORMAT);

    private Date referenceTime = new Date();

    /**
     * Creates an empty instance of the cookie manager
     *
     */
    public CookieManager() {

    }

    /**
     * Retrieves and stores cookies returned by the request
     *
     * @param request Request used to get the cookies
     */
    public CookieManager(HttpServletRequest request) {
        if (request!=null) {
            try {
                domain = getDomainFromHost(new URL(request.getRequestURL().toString()).getHost());
            }
            catch (Exception e) {

            }
            Cookie[] cookies = request.getCookies();
            if (cookies!=null) {
                for (Cookie cookie : cookies) {
                    try {
                        HttpCookie httpCookie = new HttpCookie(cookie.getName(), URLDecoder.decode(cookie.getValue(), "UTF-8"));
                        httpCookie.setComment(cookie.getComment());
                        httpCookie.setDomain(cookie.getDomain());
                        httpCookie.setPath(cookie.getPath());
                        httpCookie.setMaxAge(cookie.getMaxAge());
                        httpCookie.setSecure(cookie.getSecure());
                        httpCookie.setVersion(cookie.getVersion());
                        store.put(cookie.getName(), httpCookie);
                    }
                    catch (Exception e) {
                        logger.error("Problem deciphering cookies - %s", PivotalException.getErrorMessage(e));
                    }
                }
            }
        }
    }


    /**
     * Creates an empty cookie
     *
     * @param name Name of the cookie
     * @param value Value of the cookie
     * @return Cookie object
     */
    public static HttpCookie createCookie(String name, String value) {
        return new HttpCookie(name, value);
    }


    /**
     * Adds a cookie to the manager
     *
     * @param name Name of the cookie
     * @param value Value of the cookie
     */
    public void addCookie(String name, String value) {
        store.put(name,CookieManager.createCookie(name,value));
    }


    /**
     * Retrieves and stores cookies returned by the host on the other side
     * of the the open java.net.URLConnection.
     *
     * The connection MUST have been opened using the connect()
     * method or a IOException will be thrown.
     *
     * @param conn a java.net.URLConnection - must be open, or IOException will be thrown
     * @throws java.io.IOException Thrown if conn is not open.
     */
    public CookieManager(URLConnection conn) throws IOException {

        // let's determine the domain from where these cookies are being sent

        domain = getDomainFromHost(conn.getURL().getHost());

        // OK, now we are ready to get the cookies out of the URLConnection

        String headerName;
        for (int i = 1; (headerName = conn.getHeaderFieldKey(i)) != null; i++) {
            if (headerName.equalsIgnoreCase(SET_COOKIE)) {
                List<HttpCookie> list=HttpCookie.parse(conn.getHeaderField(i));
                if (!Common.isBlank(list)) {
                    for (HttpCookie cookie : list) {
                        store.put(cookie.getName(), cookie);
                    }
                }
            }
        }
    }


    /**
     * Prior to opening a URLConnection, calling this method will set all
     * unexpired cookies that match the path or sub-paths for the underlying URL
     *
     * The connection MUST NOT have been opened
     * method or an IOException will be thrown.
     *
     * @param conn a java.net.URLConnection - must NOT be open, or IOException will be thrown
     * @throws java.io.IOException Thrown if conn has already been opened.
     */
    public static void setCookies(URLConnection conn, CookieManager cookies) throws IOException {

        // We only want to add cookies from the list if the domains match

        URL url = conn.getURL();
        String path = url.getPath();
        String host = url.getHost();

        // Loop through all the cookies

        List<String> cookieValues = new ArrayList<>();
        for (HttpCookie cookie : cookies.store.values()) {

            // check cookie to ensure path matches  and cookie is not expired
            // if all is cool, add cookie to header string

            if ((cookie.getDomain()==null || HttpCookie.domainMatches(cookie.getDomain(), host)) &&
                Z_comparePaths(cookie.getPath(), path) && !cookie.hasExpired()) {
                cookieValues.add(cookie.toString());
            }
        }

        // If we have some valid cookies then apply them

        if (!Common.isBlank(cookieValues)) {
            try {
                conn.setRequestProperty(COOKIE, Common.join(cookieValues, "; "));
            }
            catch (java.lang.IllegalStateException ise) {
                throw new IOException("Illegal State! Cookies cannot be set on a URLConnection that is already connected. "
                        + "Only call setCookies(java.net.URLConnection) BEFORE calling java.net.URLConnection.connect().");
            }
        }
    }

    /**
     * Returns a string representation of stored cookies organized by domain
     *
     * @return String value of cookies
     */
    public String toString() {
        return store.toString();
    }

    /**
     * Returns the whole cookie value
     *
     * @param name Name of the cookie
     *
     * @return Cookie object
     */
    public HttpCookie get(String name) {
        if (!Common.isBlank(store))
            return store.get(name);
        else
            return null;
    }

    /**
     * Returns the date the cookie will expire
     *
     * @param name Name of the cookie
     *
     * @return Expiry date of the cookie
     */
    public Date getExpiry(String name) {
        Date returnValue=null;
        if (!Common.isBlank(store) &&
            store.get(name)!=null &&
            store.get(name).getMaxAge() > 0) {
            returnValue =  Common.addDate(referenceTime, Calendar.SECOND, store.get(name).getMaxAge());
        }
        return returnValue;
    }

    /**
     * Returns the whole cookie value set
     *
     * @param name Name of the cookie
     *
     * @return String value of the cookie
     */
    public String getValue(String name) {
        String returnValue=null;
        if (!Common.isBlank(store) && store.get(name)!=null)
            returnValue = store.get(name).getValue();
        return returnValue;
    }

    /**
     * Returns true if the value of the cookie can be equated to yes
     *
     * @param name Name of the cookie
     *
     * @return True if yes
     */
    public boolean isYes(String name) {
        boolean returnValue=false;
        if (!Common.isBlank(store) && store.get(name)!=null)
            returnValue = Common.isYes(store.get(name).getValue());
        return returnValue;
    }

    /**
     * Returns true if the value is in the pipe separated list of
     * cookie values
     *
     * @param name Name of the cookie
     * @param value Value to check for in the list
     *
     * @return True if the value is in the list
     */
    public boolean inList(String name, String value) {
        boolean returnValue=false;
        if (value!=null && !Common.isBlank(store) && store.get(name)!=null)
            returnValue = Common.splitToList(store.get(name).getValue().toLowerCase()," *\\| *").contains(value.toLowerCase());
        return returnValue;
    }

    /**
     * Returns true if the cookie has expired
     *
     * @param name Name of the cookie
     *
     * @return True if expired
     */
    public boolean hasExpired(String name) {
        return !Common.isBlank(store) &&
               store.get(name)!=null &&
               store.get(name).hasExpired();
    }

    /**
     * Returns the domain that this cookie belongs to
     *
     * @param host Host part of the cookie definition
     *
     * @return Domain of the cookie
     */
    private static String getDomainFromHost(String host) {
        if (host.indexOf(DOT) != host.lastIndexOf(DOT))
            return host.substring(host.indexOf(DOT) + 1);
        else
            return host;
    }

    /**
     * Compares the cookie paths to see if this cookie is part of this domain
     *
     * @param cookiePath Path of the cookie
     * @param targetPath Domain path
     *
     * @return True if the cookie is appropriate for this path
     */
    private static boolean Z_comparePaths(String cookiePath, String targetPath) {
        return cookiePath == null || "/".equals(cookiePath) || targetPath.regionMatches(0, cookiePath, 0, cookiePath.length());
    }


    public static void main(String[] args) {
        try {
            URL url = new URL("http://localhost:8080/pivotal");
            URLConnection conn = url.openConnection();
            conn.connect();
            CookieManager cm = new CookieManager(conn);
            System.out.println(cm);
            setCookies(url.openConnection(), cm);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

}
