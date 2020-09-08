/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils.browser;

import com.pivotal.utils.Common;
import com.pivotal.utils.Logger;
import com.pivotal.utils.PivotalException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
* This class describes the configuration for a Browser to use to operate
*/
public class Configuration {

    private boolean canGoBack = false;
    private boolean canGoForward = true;
    private Map<String, Cookie> cookies;
    private Map<String,String> customHeaders;
    private String libraryPath;
    private boolean navigationLocked = false;
    private String title;
    private String url;
    private HttpServletRequest request;
    private int timeout = 20000;
    private int resourceTimeout = 20000;
    private int settleTimeout = 3000;
    private String defaultCookieDomain = "localhost";

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Configuration.class);

    /**
     * Public constructor
     */
    public Configuration() {
    }

    /**
     * Constructor that copies the cookies etc. from the request object
     * @param request Request to use
     */
    public Configuration(HttpServletRequest request) {
        this();
        this.request = request;
        addCookies(request);
    }

    /**
     * Gets the time in millieseconds that PhantomJS will wait for resources to load
     * @return Time in millieseconds
     */
    public int getResourceTimeout() {
        return resourceTimeout;
    }

    /**
     * Sets the time in millieseconds that PhantomJS will wait for resources to load
     * @param resourceTimeout Time in millieseconds
     */
    public void setResourceTimeout(int resourceTimeout) {
        this.resourceTimeout = resourceTimeout;
    }

    private String getSettings() {

        return "    page.settings = {\n" +
                "      javascriptEnabled: true,\n" +
                "      loadImages: true,\n" +
                "      localToRemoteUrlAccessEnabled: true,\n" +
                "      XSSAuditingEnabled: false,\n" +
                "      webSecurityEnabled: false,\n" +
                "      resourceTimeout: " + getResourceTimeout() + "\n" +
                "    };\n";
    }

    /**
     * Returns the URL to open
     * @return URL to open in full e.g. http://google.com
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the URL to open
     * @param url URL to open in full e.g. http://google.com
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Returns the root of the request or URL to pin any relative paths
     * @return String of root e.g. http://localhost:8080/app
     */
    public String getUrlHost() {
        return getUrlHost(false);
    }

    /**
     * Returns the root of the request or URL to pin any relative paths
     * @param withPath True if the returned string should contain the path
     * @return String of root e.g. http://localhost:8080/app
     */
    public String getUrlHost(boolean withPath) {
        String returnValue = "";
        if (request!=null) {
            if (withPath) {
                returnValue = request.getRequestURL().toString();
            }
            else {
                returnValue += request.getRequestURL().toString().split("/")[0] + "//" + request.getServerName();
                if (request.getServerPort()>0) {
                    returnValue += ":" + request.getServerPort();
                }
            }
        }
        else if (!Common.isBlank(url)) {
            try {
                URL urlObj = new URL(url);
                returnValue += urlObj.getProtocol() + "://" + urlObj.getHost();
                if (urlObj.getPort()>0) {
                    returnValue += ":" + urlObj.getPort();
                }
                if (withPath) {
                    returnValue += urlObj.getPath();
                }
            }
            catch (MalformedURLException e) {
                logger.error("Problem parsing %s - %s", url, PivotalException.getErrorMessage(e));
            }
        }
        return returnValue;
    }

    /**
     * Convenient way of adding cookies from a request to the configuration
     * @param request Request to copy cookies from
     */
    public void addCookies(HttpServletRequest request) {
        if (request!=null && !Common.isBlank(request.getCookies())) {
            defaultCookieDomain = request.getServerName();
            for (Cookie cookie: request.getCookies()) {
                addCookie(cookie);
            }
        }
    }

    /**
     * Adds a Cookie to the request
     * @param cookie Cookie to add
     */
    public void addCookie(Cookie cookie) {
        if (cookie!=null) {
            if (Common.isBlank(cookies)) cookies = new HashMap<>();
            cookies.put(cookie.getName(), cookie);
        }
    }

    /**
     * Adds a header to be appended to the request
     * @param name Name of the header
     * @param value Value of the headers
     */
    public void addCustomHeader(String name, String value) {
        if (!Common.isBlank(name)) {
            if (Common.isBlank(customHeaders)) customHeaders = new HashMap<>();
            customHeaders.put(name, value);
        }
    }

    /**
     * Returns the custom headers map
     * @return Map of custom headers
     */
    public Map<String, String> getCustomHeaders() {
        return customHeaders;
    }

    /**
     * True if back button is enabled
     * @return boolean
     */
    public boolean isCanGoBack() {
        return canGoBack;
    }

    /**
     * Set whether the browser can be navigated backwards
     * @param canGoBack boolean
     */
    public void setCanGoBack(boolean canGoBack) {
        this.canGoBack = canGoBack;
    }

    /**
     * True if the forward button is enabled
     * @return boolean
     */
    public boolean isCanGoForward() {
        return canGoForward;
    }

    /**
     * Set whether the browser can be navigated forwards
     * @param canGoForward boolean
     */
    public void setCanGoForward(boolean canGoForward) {
        this.canGoForward = canGoForward;
    }

    /**
     * Returns the library path to determine javascript/css resources
     * @return Library path
     */
    public String getLibraryPath() {
        return libraryPath;
    }

    /**
     * Set the library path used to determine javascript/css resources
     * @param libraryPath Library path
     */
    public void setLibraryPath(String libraryPath) {
        this.libraryPath = libraryPath;
    }

    /**
     * Returns true if navigation is allowed
     * @return True if navigation allowed
     */
    public boolean isNavigationLocked() {
        return navigationLocked;
    }

    /**
     * Sets whether the browser can be navigated from the target URL
     * @param navigationLocked True if navigation allowed
     */
    public void setNavigationLocked(boolean navigationLocked) {
        this.navigationLocked = navigationLocked;
    }

    /**
     * Gets the page title
     * @return Page title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets ths page title
     * @param title Page title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns a JSON representation of the headers
     * @return JSON version of the headers
     */
    public String getCustomHeadersJSON() {
        StringBuilder command = new StringBuilder();
        if (!Common.isBlank(customHeaders)) {
            command.append("    page.customHeaders = {\n");
            boolean first = true;
            for (Map.Entry entry : customHeaders.entrySet()) {
                if (!first) command.append(",\n");
                command.append("      '").append(entry.getKey()).append("':'").append(entry.getValue()).append("'");
                first = false;
            }
            command.append("\n    };\n");
        }
        return command.length()==0?null:command.toString();
    }

    /**
     * Returns a map of the cookies keyed on the name
     * @return Map of cookies
     */
    public Map<String, Cookie> getCookies() {
        return cookies;
    }

    /**
     * Returns a JSON representation of the cookies
     * @return JSON version of the cookies
     */
    public String getCookiesJSON() {
        StringBuilder command = new StringBuilder();
        if (!Common.isBlank(cookies)) {
            for (Cookie cookie : cookies.values()) {
                command.append("phantom.addCookie({\n");
                command.append("  'name':'").append(cookie.getName()).append("',\n");
                command.append("  'value':'").append(cookie.getValue().replaceAll("'","''")).append("',\n");
                if (cookie.getDomain()==null) {
                    command.append("  'domain':'").append(defaultCookieDomain).append("',\n");
                }
                else {
                    command.append("  'domain':'").append(cookie.getDomain()).append("',\n");
                }
                if (cookie.getPath()!=null) {
                    command.append("  'path':'").append(cookie.getPath()).append("',\n");
                }
                if (cookie.getMaxAge()>-1) {
                    command.append("  'expires':").append(cookie.getMaxAge()).append(",\n");
                }
                command.append("  'httponly':").append(cookie.isHttpOnly() ? "true" : "false").append(",\n");
                command.append("  'secure':").append(cookie.getSecure() ? "true" : "false").append("\n");
                command.append("});\n");
            }
        }
        return command.length()==0?null:command.toString();
    }

    /**
     * Gets the execution timeout - this is the time in milliseconds that the system
     * will wait for the PhantonJS executable to run before it forcibly kills it
     * @return Time in milliseconds
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Sets the execution timeout - this is the time in milliseconds that the system
     * will wait for the PhantonJS executable to run before it forcibly kills it
     * @param timeout Time in milliseconds
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * This is the time (milliseconds) to wait for a page to fully load and render before it is closed
     * @return Settling timeout in milliseconds
     */
    public int getSettleTimeout() {
        return settleTimeout;
    }

    /**
     * Sets the time (milliseconds) that the browser will wait before closing to allow
     * the page to fully render
     * @param settleTimeout Time in milliseconds
     */
    public void setSettleTimeout(int settleTimeout) {
        this.settleTimeout = settleTimeout;
    }

    /**
     * Returns a JSON representation of the configuration
     * @param format Export format to use
     * @return Configuration
     */
    public String getJSON(ExportFormat format) {
        StringBuilder command = new StringBuilder();
        command.append("function getNewPage() {\n");
        command.append("    var page = require('webpage').create();\n");
        command.append(String.format("    page.canGoBack = %s;\n", isCanGoBack()?"true":"false"));
        command.append(String.format("    page.canGoForward = %s;\n", isCanGoForward()?"true":"false"));
        command.append(String.format("    page.navigationLocked = %s;\n", isNavigationLocked()?"true":"false"));
        command.append("    page.onConsoleMessage = function(msg) {\n" +
                       "        console.log(msg);\n" +
                       "    };\n");

        // Attach the standard settings

        command.append(getSettings());

        // Add a title if there is one

        if (!Common.isBlank(getTitle())) {
            command.append(String.format("    page.title = '%s';\n", getTitle().replaceAll("'", "''")));
        }

        // Add a library path if we have one

        if (!Common.isBlank(getLibraryPath())) {
            command.append(String.format("    page.libraryPath = '%s';\n", getLibraryPath()));
        }

        // Add any custom headers

        if (!Common.isBlank(getCustomHeaders())) {
            command.append(getCustomHeadersJSON());
        }

        // If we have a clip rectangle specified, then we will apply it
        // to everything exported from the browser.  This isn't strictly great
        // but will do until we decide to allow a mechanism for clip rectangles
        // per export

        if (format.getClipRect()!=null)
            command.append(format.getClipRect().getJSON());

        // Complete the function definition

        command.append("    return page;\n");
        command.append("}\n");
        command.append("var page = getNewPage();\n\n");

        // The default page may have an export definition

        if(!Common.isBlank(format)) {
            command.append(format.getJSON());
        }

        // Create a file system to use

        command.append("var fs = require('fs');\n");

        // Add on any cookies to the phantom container if we have any

        if (!Common.isBlank(getCookies())) {
            command.append(getCookiesJSON());
        }

        // Add on a universal error trap

        command.append("phantom.onError = function(msg, trace) {\n" +
                "    var msgStack = ['PHANTOM ERROR: ' + msg];\n" +
                "    if (trace && trace.length) {\n" +
                "        msgStack.push('TRACE:');\n" +
                "        trace.forEach(function(t) {\n" +
                "            msgStack.push(' -> ' + (t.file || t.sourceURL) + ': ' + t.line + (t.function ? ' (in function ' + t.function +')' : ''));\n" +
                "        });\n" +
                "    }\n" +
                "    console.error(msgStack.join('\\n'));\n" +
                "    phantom.exit(1);\n" +
                "};\n");

        // Add on the font sizer

        command.append("function setFontSize(fontSize) {\n" +
                "    document.body.style.fontSize = fontSize;\n" +
                "};\n");

        // Add the serialise tags function

        command.append("function serializeTags(tagName) {\n" +
                "    var returnValues = [];\n" +
                "    var serializer = new XMLSerializer();\n" +
                "    var elements = document.getElementsByTagName(tagName);\n" +
                "    if (elements.length>0) {\n" +
                "        for (var i=0; i<elements.length; i++) {\n" +
                "            var image = new Object();\n" +
                "            image.content = serializer.serializeToString(elements[i]);\n" +
                "            image.width = elements[i].offsetWidth;\n" +
                "            image.height = elements[i].offsetHeight;\n" +
                "            image.fontSize = window.getComputedStyle(elements[i]).getPropertyValue('font-size');\n" +
                "            returnValues.push(image);\n" +
                "        }\n" +
                "    }\n" +
                "    return returnValues;\n" +
                "}\n");

        // Serialise SVG tags

        command.append("function serializeSVG(tagName, fileBody, imageType) {\n" +
                "    var returnValues = [];\n" +
                "    var serializer = new XMLSerializer();\n" +
                "    var elements = document.getElementsByTagName(tagName);\n" +
                "    if (elements.length>0) {\n" +
                "        for (var i=0; i<elements.length; i++) { \n" +
                "            var image = new Object();\n" +
                "            image.content = serializer.serializeToString(elements[i]);\n" +
                "            image.fileName = fileBody + '-' + i + '.' + imageType;\n" +
                "            image.width = elements[i].offsetWidth;\n" +
                "            image.height = elements[i].offsetHeight;\n" +
                "            image.src = '<img width=\\'image.width\\' height=\\'image.height\\' src=\\'file:////' + image.fileName.replace(/ /g,'%20').replace(/\\\\/g,'/') + '\\'>';\n" +
                "            image.fontSize = window.getComputedStyle(elements[i]).getPropertyValue('font-size');\n" +
                "            returnValues.push(image);\n" +
                "        }\n" +
                "    }\n" +
                "    return returnValues;\n" +
                "}\n");

        // Serialise an arbitrary element

        command.append("\nfunction serializeId(elementID) {\n" +
                "    var serializer = new XMLSerializer();\n" +
                "    var element = document.getElementById(elementID);\n" +
                "    return serializer.serializeToString(element);\n" +
                "}\n");

        // Create the javascript to do the extraction

        command.append("function renderNextImage(imageStack) {\n" +
                "    if (imageStack.length==0) {\n" +
                "        phantom.exit();\n" +
                "    }\n" +
                "    else {\n" +
                "        var nextImage = imageStack.pop();\n" +
                "        var page = getNewPage();\n" +
                "        if (nextImage.width) {page.viewportSize = {width:nextImage.width, height:nextImage.height}}\n" +
                "        if (nextImage.fontSize) {page.evaluate(setFontSize, nextImage.fontSize)}\n" +
                "        page.onLoadFinished = function(status) {\n" +
                "            page.render(nextImage.fileName);\n" +
                "            renderNextImage(imageStack);\n" +
                "        }\n" +
                "        page.content = nextImage.content;\n" +
                "    }\n" +
                "}\n");

        return command.toString();
    }
}
