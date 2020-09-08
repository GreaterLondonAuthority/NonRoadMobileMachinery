/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web;

import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;
import com.pivotal.utils.browser.Browser;
import com.pivotal.web.servlet.ServletHelper;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.Http11AprProtocol;
import org.apache.coyote.http11.Http11NioProtocol;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.servlet.http.HttpServletRequest;
import java.util.Calendar;
import java.util.Date;

/**
 * Useful constants for the application
 */
public class Constants {
    /**
     * Standard logger
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Constants.class);

    //** Name of the application
    public static final String APPLICATION_NAME = "APP";
    public static final String IN_IDE = "inide";
    public static final String INTERNAL_REQUEST_USER_ID = "user-id";
    public static final String INTERNAL_REQUEST_TOKEN = "token";

    //** Version number of the application - note, this is not final because it is changed at runtime
    public static String APPLICATION_VERSION = "0.10";

    //** Build date of the application - note, this is not final because it is changed at runtime
    public static Date APPLICATION_BUILD_DATE = new Date();

    //** Default location for the log files
    public static final String DEFAULT_LOG_DIR = "/WEB-INF/logs";

    //** Catalina base directory system setting name
    public static final String CATALINA_BASE_SETTING = "catalina.base";

    //** Alternative location for the log files
    public static final String DEFAULT_CATALINA_LOG_DIR = "/logs";

    //** Logging properties file
    public static final String LOG4J_PROPERTIES_FILE = "/WEB-INF/classes/log4j.properties";

    //** Manifest file
    public static final String MANIFEST_FILE = "/META-INF/MANIFEST.MF";

    //** App email definition template file
    public static final String SAVE_EMAIL_TEMPLATE_FILE = "/WEB-INF/database/reports/email/save_template.vm";

    //** App email definition template file
    public static final String SAVE_EMAIL_FOLDER = "Emails";

    //** App db definition file
    public static final String APP_SQL_FILE = "/WEB-INF/database/app.sql";

    //** App test data file
    public static final String APP_SQL_TEST_DATA_FILE = "/WEB-INF/database/app_test_data.sql";

    //** App default settingst data file
    public static final String APP_SQL_SETTINGS_DATA_FILE = "/WEB-INF/database/app_settings_data.sql";

    //** App XSD settings definition
    public static final String APP_XSD_FILE = "/WEB-INF/xsd/app.xsd";

    //** Mime types file
    public static final String MIME_TYPES_FILE = "/WEB-INF/mime.types";

    //** Template used for formatting task error notification messages
    public static final String ERROR_NOTIFICATION_TEMPLATE = "/WEB-INF/templates/task/notifications/error.vm";

    //** Template used for formatting task completion notification messages
    public static final String COMPLETION_NOTIFICATION_TEMPLATE = "/WEB-INF/templates/task/notifications/completion.vm";

    //** Name of the token passed by the intelligent download mechanism
    public static final String FILE_DOWNLOAD_TOKEN = "fileDownloadToken";

    //** Name of the system parameter that holds the default log directory
    public static final String APP_DEFAULT_LOGDIR = "app.default.logdir";

    //** Directory that will contain scripts that override the ones contained within the database
    public static final String SCRIPT_OVERRIDE_DIRECTORY = "overrideDir";

    //** Address used as the default address to access local servlet services
    private static final String LOCALHOST_ADDRESS = "http://localhost";

    //** Port used as the default to access local servlet services
    private static final String LOCALHOST_PORT = ":8080";

    private static int lastKnownPort = -1;
    private static String localAddress;

    /**
     * Returns true if the inIDE switch is set on the command line
     *
     * @return True if running inside an IDE
     */
    public static boolean inIde() {
        return System.getProperty(Constants.IN_IDE) != null;
    }

    /**
     * Calculates the server address with context but will use the overridden value
     * if it has been set by the user
     *
     * @return Server address and context e.g. http://xxx.com:8080/test
     */
    public static String getAppPath() {
        String value = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_PUBLISHING_SERVER, getServerContextAddress());
        return value.replaceAll("[ /]+$", "");
    }

    /**
     * Calculates the server address with context. If something fails, it will return the
     * default LOCALHOST_ADDRESS constant
     *
     * @return Server address and context e.g. http://xxx.com:8080/test
     */
    public static String getServerContextAddress() {
        String address = null;

        // See if a request has been made

        HttpServletRequest request = ServletHelper.getRequest();
        if (request != null) {

            // This call is associated with an http request so use the information from that request

            try {
                // Store the last known good port the application is running from

                lastKnownPort = request.getLocalPort();

                // This check req when you reports exe then request local name need to be handled

                if (Common.doStringsMatch("0:0:0:0:0:0:0:1", request.getLocalName())) {
                    address = String.format("%s:%s", LOCALHOST_ADDRESS, lastKnownPort);
                }
                else {
                    address = String.format("http://%s:%s", request.getLocalName(), lastKnownPort);
                }
            }
            catch (Exception e) {
                logger.error("Error attempting to find out request information : {}", PivotalException.getErrorMessage(e));
            }
        }
        else {
            // No request associated with the thread so use lastKnownPort if possible

            if (lastKnownPort != -1) {

                // No request for the operation but another request has been processed by the app so use the port that came in on

                address = String.format("%s:%s", LOCALHOST_ADDRESS, lastKnownPort);
            }
            else {
                // No request come through as yet so try to look the port up from the container

                address = getLocalAddress();
            }
        }
        // If all has failed return a best guess.....

        if (Common.isBlank(address)) {
            address = LOCALHOST_ADDRESS + LOCALHOST_PORT;
        }

        // Add on the servlet context if we have it

        if (ServletHelper.getServletContext() != null) {
            address += ServletHelper.getServletContext().getContextPath();
        }
        return address;
    }

    /**
     * This is a useful method for getting round the restriction that Servlets are
     * not allowed to know much about their container
     * This method tries to find the container address using management beans and then
     * interrogates the network interfaces to get the right one
     * It only works if the container has registered itself with MBX
     *
     * @return Server address and port if known
     */
    synchronized public static String getLocalAddress() {
        if (localAddress == null) {
            try {
                MBeanServer mBeanServer = MBeanServerFactory.findMBeanServer(null).get(0);
                ObjectName name = new ObjectName("Catalina", "type", "Server");
                Server server = (Server)mBeanServer.getAttribute(name, "managedResource");
                Service[] services = server.findServices();
                for (Service service : services) {
                    for (Connector connector : service.findConnectors()) {
                        ProtocolHandler protocolHandler = connector.getProtocolHandler();
//                        if (protocolHandler instanceof Http11Protocol || protocolHandler instanceof Http11AprProtocol || protocolHandler instanceof Http11NioProtocol) {
                        if (protocolHandler instanceof Http11AprProtocol || protocolHandler instanceof Http11NioProtocol) {
                            localAddress = String.format("%s:%s", LOCALHOST_ADDRESS, connector.getPort());
                        }
                    }
                }
            }
            catch (Exception e) {
                logger.error("Error attempting to find out application port : {}", PivotalException.getErrorMessage(e));
            }
        }
        return localAddress;
    }

    /**
     * Returns a token to use for the loopback call
     *
     * @return A simple time based token
     */
    public static String getLoopbackToken() {

        // Get a timestamp with no seconds/milliseconds

        Calendar tmp = Calendar.getInstance();
        tmp.set(Calendar.MILLISECOND, 0);

        // Create a token based on an MD5 string

        return Common.getMD5String(tmp.getTime() + "");
    }

    /**
     * Checks to make sure that this is a valid loopback request
     *
     * @param request Request being processed
     *
     * @return True if this is a loopback request
     */
    public static boolean isLoopbackRequest(HttpServletRequest request) {
        boolean returnValue = false;
        if (request != null && request.getHeader(Browser.INTERNAL_REQUEST_HEADER) != null) {
            String token = request.getHeader(Constants.INTERNAL_REQUEST_TOKEN);

            // Look back no more than 10 seconds

            Date date = new Date();
            for (int i = 0; i < 10 && !returnValue; i++) {
                Calendar tmp = Calendar.getInstance();
                tmp.setTime(Common.addDate(date, Calendar.SECOND, -i));
                tmp.set(Calendar.MILLISECOND, 0);
                returnValue = Common.doStringsMatch(token, Common.getMD5String(tmp.getTime() + ""));
            }
        }
        return returnValue;
    }
}
