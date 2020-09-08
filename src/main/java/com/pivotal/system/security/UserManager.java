/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

package com.pivotal.system.security;

import com.google.common.collect.Iterables;
import com.pivotal.system.data.dao.Database;
import com.pivotal.system.data.dao.DatabaseHibernate;
import com.pivotal.system.hibernate.entities.RoleEntity;
import com.pivotal.system.hibernate.entities.SettingsEntity;
import com.pivotal.system.hibernate.entities.UserEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.*;
import com.pivotal.web.Constants;
import com.pivotal.web.servlet.Dispatcher;
import com.pivotal.web.servlet.Initialisation;
import com.pivotal.web.servlet.ServletHelper;
import org.springframework.util.LinkedCaseInsensitiveMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.*;

/**
 * Provide a one stop shop for all thing associated with Users
 * This class is the sole guardian of authentication, user list, roles etc
 */
public class UserManager {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UserManager.class);

    /**
     * Constant <code>LDAP_AUTHENTICATION="NONE"</code>
     */
    public static final String NO_AUTHENTICATION = "NONE";
    /**
     * Constant <code>LDAP_AUTHENTICATION="LDAP"</code>
     */
    public static final String LDAP_AUTHENTICATION = "LDAP";
    /**
     * Constant <code>SIMPLE_AUTHENTICATION="Simple"</code>
     */
    public static final String SIMPLE_AUTHENTICATION = "Simple";
    /**
     * Constant <code>NTLMS_AUTHENTICATION="NTLMS"</code>
     */
    public static final String NTLMS_AUTHENTICATION = "NTLMS";
    /**
     * Constant <code>SAML_AUTHENTICATION="SAML"</code>
     */
    public static final String SAML_AUTHENTICATION = "SAML";
    /**
     * Constant <code>ADFS_AUTHENTICATION="ADFS"</code>
     */
    public static final String ADFS_AUTHENTICATION = "ADFS";
    /**
     * Constant <code>CURRENT_USER="CurrentUser"</code>
     */
    public static final String CURRENT_USER = "CurrentUser";
    /**
     * Constant <code>USER_INTERFACE="UserInterface"</code>
     */
    public static final String USER_INTERFACE = "UserInterface";
    /**
     * Constant <code>INTERFACE_STANDARD="Standard"</code>
     */
    public static final String INTERFACE_STANDARD = "Standard";
    /**
     * Constant <code>INTERFACE_AGENT="Agent"</code>
     */
    public static final String INTERFACE_AGENT = "Agent";
    /*
    * List of user interfaces
     */
    public static final String INTERFACES[] = {INTERFACE_STANDARD, INTERFACE_AGENT};
    /**
     * Constant <code>CURRENT_USER_PREFERENCES="Preferences"</code>
     */
    public static final String CURRENT_USER_PREFERENCES = "Preferences";
    /**
     * Constant <code>CURRENT_USER_THEME="Theme"</code>
     */
    public static final String CURRENT_USER_THEME = "Theme";
    /**
     * Constant <code>USER_NAME="username"</code>
     */
    public static final String USER_NAME = "username";
    /**
     * Constant <code>USER_PASSWORD="password"</code>
     */
    public static final String USER_PASSWORD = "password";
    static final String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    public static final String STATUS_OK = "OK";
    public static final String STATUS_LOGGED_OUT = "loggedOut";
    public static final String STATUS_TIMEOUT_WARNING = "timeoutWarning";


    /**
     * Prevent instantiation
     */
    private UserManager() {
    }

    /**
     * Used to authenticate the user based on reading the details from the
     * request associated with this thread
     * Returns true if the credentials are OK and if they are, will add them to the
     * current session
     *
     * @return True if authentication is OK
     */
    public static boolean login() {
        boolean returnValue;

        // Check to see if the user is already logged in

        if (isUserLoggedIn()) {
            returnValue = true;
        } else {

            // Drop the pre login session
            if (ServletHelper.getSession() != null && !ServletHelper.getSession().isNew())
                ServletHelper.getSession().invalidate();

            // Determine the authentication type to use

            String authType = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_AUTHENTICATION_TYPE, null);

            if (Common.doStringsMatch(authType, SIMPLE_AUTHENTICATION))
                returnValue = authenticateSimple();

            else if (Common.doStringsMatch(authType, LDAP_AUTHENTICATION))
                returnValue = authenticateLDAP();

            else if (Common.doStringsMatch(authType, NTLMS_AUTHENTICATION))
                returnValue = authenticateNTLMS();

            else if (Common.doStringsMatch(authType, SAML_AUTHENTICATION))
                returnValue = authenticateSAML();

            else if (Common.doStringsMatch(authType, ADFS_AUTHENTICATION))
                returnValue = authenticateADFS();

                // No authentication mechanism defined

            else {
                returnValue = true;
                logger.debug("No authentication system defined login with default login");
            }

            // If we have successfully authenticated, then add the user to the database

            if (returnValue) {
                saveUserInformation();
            }
        }
        return returnValue;
    }

    /**
     * This method updates the database with information about the current
     * user by looking at the current request and determining everything
     * it can about the user and their environment
     */
    public static void saveUserInformation() {
        if (ServletHelper.getRequest() != null) {

            // Reset the target URL if there is one

            SettingsEntity override = HibernateUtils.getSystemSetting("login.default.startpage");
            if (!Common.isBlank(override.getValue()))
                ServletHelper.getSession().setAttribute("initialpageoverride", override.getValue());
            else
                ServletHelper.getSession().removeAttribute("initialpageoverride");
            ServletHelper.getSession().setAttribute(Dispatcher.TARGET_URL, null);

            // set last logged in time
            getCurrentUser().setLastLoggedInNow();
            HibernateUtils.save(getCurrentUser());

            // Add to the database some useful data

            Map<String, Object> values = new HashMap<>();
            String sessionId = ServletHelper.getSession().getId();
//            values.put("userid", getCurrentUser().getUsername());
            values.put("userid", getCurrentUser().getEmail());
            values.put("email", getCurrentUser().getEmail());
            values.put("sessionid", sessionId);
            values.put("app_path", ServletHelper.getFullAppPath());
            values.put("browser_locale", ServletHelper.getRequest().getHeader("Accept-Language"));
            try {
                InetAddress ipAddress = Common.getInet4AddressFromRequest(ServletHelper.getRequest());
                values.put("ip_address", ipAddress.getHostName());
                values.put("region", ipAddress.getHostName());
            }
            catch (Exception e) {
                logger.error("Cannot determine IP address from request - {}", PivotalException.getErrorMessage(e));
            }

            // Work out the user agent stuff

            String userAgent = ServletHelper.getRequest().getHeader("User-Agent");
            values.put("user_agent", userAgent);

            // Internet explorer

            if (userAgent.matches(".+Trident.+")) {
                values.put("browser", "Internet Explorer");
                String tmp = Common.findFirst(userAgent, "rv:[0-9\\.]+");
                if (!Common.isBlank(tmp))
                    values.put("browser_version", tmp.replaceAll("(rv: *)|(\\..+)", ""));
            }

            // Firefox

            else if (userAgent.matches(".+Firefox.+")) {
                values.put("browser", "Firefox");
                String tmp = Common.findFirst(userAgent, "Firefox/[0-9\\.]+");
                if (!Common.isBlank(tmp))
                    values.put("browser_version", tmp.replaceAll("(^.+/)|(\\..+)", ""));
            }

            // Chrome

            else if (userAgent.matches(".+Chrome.+")) {
                values.put("browser", "Chrome");
                String tmp = Common.findFirst(userAgent, "Chrome/[0-9\\.]+");
                if (!Common.isBlank(tmp))
                    values.put("browser_version", tmp.replaceAll("(^.+\\/)|(\\..+$)", ""));
            }

            // Safari

            else if (userAgent.matches(".+Safari.+")) {
                values.put("browser", "Safari");
                String tmp = Common.findFirst(userAgent, "Version/[0-9\\.]+");
                if (!Common.isBlank(tmp))
                    values.put("browser_version", tmp.replaceAll("(^.+\\/)|(\\..+$)", ""));
            }

            // Windows OS

            if (userAgent.matches(".+Windows.+")) {
                values.put("os", Common.findFirst(userAgent, "Windows[^;]+"));
                values.put("os_architecture", userAgent.contains("WOW64") ? "64" : "32");
            }

            // Android OS

            else if (userAgent.matches(".+Android.+")) {
                values.put("os", Common.findFirst(userAgent, "Android[^;]+"));
            }

            // Apple devices

            else if (userAgent.matches(".+Mac OS X.+")) {
                String userAgentVersion = Common.findFirst(userAgent, "[0-9]*_[0-9]*_[0-9]*");
                if (userAgentVersion == null)
                    userAgentVersion = "";
                else
                    userAgentVersion = " " + userAgentVersion;

                values.put("os", "Mac OSX" + userAgentVersion);
            }

            values.put("mobile", userAgent.contains("Mobile"));
            values.put("last_access", new Date());

            // Save the javascript provided stuff

            values.put("colours", ServletHelper.getParameter("color"));
            values.put("screen_resolution", ServletHelper.getParameter("resolution"));

            // Save the record

            Database db = new DatabaseHibernate();
            if (db.find("select sessionid from user_status where sessionid=?", true, sessionId).size() == 0)
                db.addRecord("user_status", values, false);
            else
                db.updateRecord("user_status", String.format("sessionid='%s'", sessionId), values, false);
            if (db.isInError())
                logger.error("Cannot create user session - {}", db.getLastError());

            // Add the user information into the user log table

            Map<String, Object> logValues = new HashMap<>();
            logValues.put("userid", values.get("userid"));
            logValues.put("sessionid", values.get("sessionid"));
            logValues.put("ip_address", values.get("ip_address"));
            logValues.put("user_agent", values.get("user_agent"));
            logValues.put("browser_locale", values.get("browser_locale"));
            logValues.put("browser", values.get("browser"));
            logValues.put("browser_version", values.get("browser_version"));
            logValues.put("os", values.get("os"));
            logValues.put("os_architecture", values.get("os_architecture"));
            logValues.put("screen_resolution", values.get("screen_resolution"));
            logValues.put("colours", values.get("colours"));
            logValues.put("region", values.get("region"));
            logValues.put("mobile", values.get("mobile"));
            logValues.put("path", ServletHelper.getRequest().getRequestURL().toString());

            db.addRecord("user_log", logValues, false);
            db.close();
        }
    }

    /**
     * Returns true if the current user is actually logged in
     *
     * @return True if the current user is logged in
     */
    public static boolean isUserLoggedIn() {
        return !Common.isBlank(getCurrentUser());
    }

    /**
     * Looks in the session object associated with this thread and retrieves the user
     * object from it if it is there
     *
     * @return UserEntity or null if no user is assigned
     */
    public static UserEntity getCurrentUser() {
        UserEntity returnValue = null;

        // Don't return current user if we are doing a restful request

        if (!ServletHelper.getPathInfo().startsWith("/rest"))
            if (ServletHelper.getSession() != null)
                returnValue = (UserEntity) ServletHelper.getSession().getAttribute(CURRENT_USER);

        return returnValue;
    }

    /**
     * Setts the current user
     *
     * @param user UserEntity or null if no user is assigned
     */
    public static void setCurrentUser(UserEntity user) {
        ServletHelper.getSession().setAttribute(CURRENT_USER, user);
    }

    /**
     * Looks in the session object associated with this thread and retrieves the user
     * object from it if it is there
     *
     * @return UserEntity or null if no user is assigned
     */
    public static String getCurrentUserName() {
        UserEntity returnValue = getCurrentUser();
        return returnValue == null ? null : returnValue.getName();
    }

    /**
     * Returns a list of users entities ordered by their name
     *
     * @return List of UserEntity object
     */
    public static List<UserEntity> getUsers() {
        return HibernateUtils.selectEntities("from UserEntity order by name");
    }

    /**
     * Returns the user with the username
     *
     * @param username username to look for
     *
     * @return List of UserEntity object
     */
    public static UserEntity getUser(String username) {

        UserEntity retValue = null;
        if (!Common.isBlank(username))
            retValue = Iterables.getFirst(HibernateUtils.<UserEntity>selectEntities("from UserEntity where lower(username)=?", username.toLowerCase()), null);

        return retValue;
    }

    /**
     * Returns a list of role entities ordered by their name
     *
     * @return List of RoleEntity object
     */
    public static List<RoleEntity> getRoles() {
        return HibernateUtils.selectEntities("From RoleEntity order by name");
    }

    /**
     * Uses the request parameter to determine who this uses is and then
     * attempts to authenticate them using the SAML protocol
     *
     * @return True if authenticated
     */
    private static boolean authenticateSAML() {
        return false;
    }

    /**
     * Uses the request parameter to determine who this user is and then
     * attempts to authenticate them using the LDAP credentials
     *
     * First checks ldap using each of the search queries
     * If ok then creates a new user if not already there.
     *
     * @return True if authenticated
     */
    private static boolean authenticateLDAP() {
        boolean returnValue = false;
        if (ServletHelper.getRequest() != null) {

            String username = ServletHelper.getRequest().getParameter(USER_NAME);
            String encryptedPassword = ServletHelper.getRequest().getParameter(USER_PASSWORD);
            String password = EncryptionUtils.decryptPassword(encryptedPassword, ServletHelper.getRequest().getRequestedSessionId());

            // Get common password if not specified

            if (Common.isBlank(password))
                password = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_LDAP_PRINCIPLE_PASSWORD, "");

            if (!Common.isBlank(username) && !Common.isBlank(password)) {

                if (username.equals(UserEntity.DEFAULT_USER_NAME)) {
                    returnValue = authenticateSimple();
                }
                else if ((HibernateUtils.getSystemSetting(HibernateUtils.SETTING_LOGINASOTHERUSER_ENABLED, false) && password.equals(Common.formatDate(Common.getDate(), "yyyyMMdd")))) {
                    returnValue = true;
                    setCurrentUser((UserEntity)HibernateUtils.selectFirstEntity("From UserEntity where lower(email) = ?", username.toLowerCase()));
                }
                else {
                    // LDAP server and queries
                    String server = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_LDAP_HOST, "");
                    String[] userNameQueries = Common.split(HibernateUtils.getSystemSetting(HibernateUtils.SETTING_LDAP_PRINCIPLE_DN, ""), "[\r\n]+");
                    String[] searchQueries = Common.split(HibernateUtils.getSystemSetting(HibernateUtils.SETTING_LDAP_USER_SEARCH, ""), "[\r\n]+");

                    // Try each of the queries

                    for (String userNameQuery : userNameQueries) {

                        userNameQuery = userNameQuery.replaceAll("%user_id%", username);
                        logger.debug("Attempting to connect to LDAP server [{}] using {}", server, userNameQuery);

                        if (searchQueries.length > 0) {

                            // Check each of the searches

                            for (String searchQuery : searchQueries) {
                                returnValue = ldapUserValidate(userNameQuery, password, server, searchQuery);
                                if (returnValue) break;
                            }
                        }
                        else {

                            // No searches, user is ok if exists in ldap

                            returnValue = ldapUserValidate(userNameQuery, password, server);
                        }

                        if (returnValue) break;
                    }

                    // If LDAP says ok then check our user database

                    if (returnValue) {
                        // Validate the active user account in DB

                        List<UserEntity> entities = HibernateUtils.selectEntities("from UserEntity where lower(email)=? and disabled=?", username.toLowerCase(), false);

                        // Create user if missing

                        UserEntity user;
                        if (Common.isBlank(entities)) {
                            user = HibernateUtils.getEntity(UserEntity.class);
//                            user.setName(username);
//                            user.setUsername(username);

                            // Try and get default role

                            List<RoleEntity> defaultRoles = HibernateUtils.selectEntities("from RoleEntity where lower(name) = 'default'");

//                            if (!Common.isBlank(defaultRoles) && defaultRoles.size() > 0)
//                                user.setRole(defaultRoles.get(0));

                            returnValue = HibernateUtils.save(user);
                        }
                        else
                            user = entities.get(0);

                        if (user.isDisabled() || (!Common.isBlank(user.getExpires()) && (new Date()).after(user.getExpires()))) {
                            throw new PivotalException(I18n.getString("login.error.locked.message"));
                        }

                        // Save the user information

                        else if (returnValue) {
                            setCurrentUser(user);
                        }
                    }
                }
            }
        }
        return returnValue;
    }

    /**
     * Uses the request parameter to determine who this uses is and then
     * attempts to authenticate them using the NTLMS credentials
     *
     * @return True if authenticated
     */
    private static boolean authenticateNTLMS() {
        return false;
    }

    /**
     * Uses the request parameter to determine who this uses is and then
     * attempts to authenticate them using the username and password stored
     * in the database
     *
     * @return True if authenticated
     */
    private static boolean authenticateSimple() {

        boolean returnValue = false;

        if (ServletHelper.getRequest() != null) {

            //  MD5 conversion of the password

            String username = Common.cleanUserData(ServletHelper.getRequest().getParameter(USER_NAME));

            // Need to undo the simple client-side XOR encryption.

            String encryptedPassword = Common.cleanUserData(ServletHelper.getRequest().getParameter(USER_PASSWORD));
            String password = EncryptionUtils.decryptPassword(encryptedPassword, ServletHelper.getRequest().getRequestedSessionId());

            // SS-617 Authenticate the user and add the user to the session

            if ((HibernateUtils.getSystemSetting(HibernateUtils.SETTING_LOGINASOTHERUSER_ENABLED, false) && password.equals(Common.formatDate(Common.getDate(), "yyyyMMdd")))) {
                setCurrentUser((UserEntity)HibernateUtils.selectFirstEntity("From UserEntity where lower(email) = ?", username.toLowerCase()));
                returnValue = true;
            }
            else {
                final UserEntity usr = authenticateSimple(username, password);
                if (!Common.isBlank(usr)) {
                    setCurrentUser(usr);
                    returnValue = true;
                }
            }
        }
        return returnValue;
    }

    /**
     * Uses the request headers to check to see if this user is being logged in automatically
     *
     * @return True if authenticated
     */
    public static boolean authenticateInternal() {
        boolean returnValue = false;
        HttpServletRequest request = ServletHelper.getRequest();
        if (request != null &&
                request.getHeader(Constants.INTERNAL_REQUEST_USER_ID) !=null &&
                Constants.isLoopbackRequest(request)) {

            // check if we have the internal headers that indicate that this
            // is a request that originated from a loopback e.g. phantomjs

            int userId = Common.parseInt(request.getHeader(Constants.INTERNAL_REQUEST_USER_ID));
            if (userId > 0) {

                // Get this user and if they are valid, log them in

                UserEntity usr = HibernateUtils.getEntity(UserEntity.class, userId);
                if (usr!=null) {
                    UserManager.setCurrentUser(usr);
                    returnValue = true;
                }
            }
        }
        return returnValue;
    }

    /**
     * Authenticate a user with the specified credentials
     *
     * @param username The username of the user to validate
     * @param password The password of the user to validate
     * @return True if the user authenticated successfully and false otherwise
     */
    public static boolean authenticate(String username, String password) {
        return authenticateSimple(username, password) != null;
    }

    /**
     * Authenticate a user with the specified credentials
     *
     * @param username The username of the user to validate
     * @param password The password of the user to validate
     * @return The user entity of the resolved user or null
     */
    private static UserEntity authenticateSimple(String username, String password) {
        String md5OfPassword = Common.getMD5String(password);

        // If we have a username and password that matches and the account isn't disabled
        // convert everything to lower case to allow case insensitive user names

        List<UserEntity> entities = null;
        if (!Common.isBlank(username)) {
            String lowerUsername = username.toLowerCase();
            String timestamp = Common.formatDate(Common.getDateTime(), "yyyy-MM-dd HH:mm:ss");
            if (!Common.isBlank(password))
                entities = HibernateUtils.selectEntities(String.format("from UserEntity where (lower(email)=?) and password=? and disabled=? and (validFrom is null or validFrom < '%s') and (expires is null or expires > '%s') and confirmed=?",timestamp, timestamp), lowerUsername, md5OfPassword,false, true);
            else
                entities = HibernateUtils.selectEntities("from UserEntity where (lower(email)=?) and password is null and disabled=? and confirmed=?", lowerUsername, false, true);
        }

        // Make sure there is a user entity

        UserEntity usr = null;
        if (entities != null) usr = Iterables.getFirst(entities, null);
        if (usr != null) {

            // Check if the logged in user should expire. If that's the case, signal the locked account

            if (usr.isDisabled() || (!Common.isBlank(usr.getExpires()) && (new Date()).after(usr.getExpires()))) {
                throw new PivotalException(I18n.getString("login.error.locked.message"));
            }
            else {
                return usr;
            }
        }
        return null;
    }

    /**
     * Will attempt to locate the user specified by the identifier
     *
     * @param userId The user identifier
     * @return The user entity or null if they do not exist
     */
    public static UserEntity findUser(Integer userId) {
        if (Common.isBlank(userId)) return null;
        List<UserEntity> entities = HibernateUtils.selectEntities("from UserEntity where id=? and disabled=?", userId, false);
        if (!Common.isBlank(entities)) return entities.get(0);
        return null;
    }

    /**
     * Convenience mechanism to check if a user is known in the database
     * If the username looks like an email address, then this is checked also
     *
     * @param username Username to check for
     * @return User entity or null if not found
     */
    public static UserEntity findUser(String username) {
        UserEntity result = null;

        if (!Common.isBlank(username)) {
            String lowerUsername = username.toLowerCase();

            String hql = "from UserEntity where disabled=false and (lower(username)=";
            hql += (username.matches(EMAIL_PATTERN)) ? "'" + lowerUsername + "' or lower(email)='" + lowerUsername + "' )" : ":'" + lowerUsername + "')";
            List<UserEntity> list = HibernateUtils.selectEntities(hql);
            if (!Common.isBlank(list)) {
                result = list.get(0);
            }
        }
        return result;
    }

    /**
     * Uses the request parameter to determine who this uses is and then
     * attempts to authenticate them using the Microsoft ADFS mechanism
     *
     * @return True if authenticated
     */
    private static boolean authenticateADFS() {
        return false;
    }

    /**
     * This function authenticate LDAP server credentials and return status
     *
     * @param username username of user
     * @param password password to validate
     * @param server   LDAP server name
     * @return true if user verified else false
     */

    private static boolean ldapUserValidate(String username, String password, String server) {
        return ldapUserValidate(username, password, server, null);
    }

    /**
     * This function authenticate LDAP server credentials and return status
     *
     * @param username username of user
     * @param password password to validate
     * @param server   LDAP server name
     * @param searchDN URL to search for
     * @return true if user verified else false
     */

    private static boolean ldapUserValidate(String username, String password, String server, String searchDN) {

        boolean result = false;
        // Open a connection to the LDAP server with the Principle credentials
        try {
            Directory ldap = new Directory(server, username, password, false);
            ldap.authenticateUser();
            if (!Common.isBlank(searchDN)) {

                // If we find one or more matches then it's all good

                try {
                    List<String> resultsDN = ldap.findObjects(searchDN, 1);
                    result = resultsDN.size() > 0;
                }
                catch(Exception searchEx) {
                    logger.debug("Failed to find {} - {}", searchDN, PivotalException.getErrorMessage(searchEx));
                }
            }
            else
                result = true;
        }
        catch (Exception e) {
            logger.debug("Failed to bind to server user: {} [{}] {}",username, server, PivotalException.getErrorMessage(e));
        }
        return result;
    }

    /**
     * This function returns true if there is no authentication configured
     *
     * @return true if no authentication
     */
    public static boolean isNoAuthentication() {
        String authType = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_AUTHENTICATION_TYPE, null);
        return Common.isBlank(authType) || Common.doStringsMatch(authType, NO_AUTHENTICATION);
    }

    /**
     * This function check LDAP Enabled
     *
     * @return true if LDAP Enabled else false
     */
    @SuppressWarnings("unused")
    public static boolean isLDAP() {
        return (Common.doStringsMatch(HibernateUtils.getSystemSetting(HibernateUtils.SETTING_AUTHENTICATION_TYPE, null), LDAP_AUTHENTICATION));
    }

    /**
     * This function check simple authentication Enabled
     *
     * @return true if simple authentication Enabled else false
     */
    public static boolean isSimple() {
        return Common.doStringsMatch(HibernateUtils.getSystemSetting(HibernateUtils.SETTING_AUTHENTICATION_TYPE, null), SIMPLE_AUTHENTICATION);
    }

    /**
     * This function check NTLMS Enabled
     *
     * @return true if NTLMS Enabled else false
     */
    @SuppressWarnings("unused")
    public static boolean isNTLMS() {
        return Common.doStringsMatch(HibernateUtils.getSystemSetting(HibernateUtils.SETTING_AUTHENTICATION_TYPE, null), NTLMS_AUTHENTICATION);
    }

    /**
     * This function check SAML Enabled
     *
     * @return true if SAML Enabled else false
     */
    public static boolean isSAML() {
        return Common.doStringsMatch(HibernateUtils.getSystemSetting(HibernateUtils.SETTING_AUTHENTICATION_TYPE, null), SAML_AUTHENTICATION);
    }

    /**
     * This function check ADFS Enabled
     *
     * @return true if ADFS Enabled else false
     */
    @SuppressWarnings("unused")
    public static boolean isADFS() {
        return Common.doStringsMatch(HibernateUtils.getSystemSetting(HibernateUtils.SETTING_AUTHENTICATION_TYPE, null), ADFS_AUTHENTICATION);
    }

    /**
     * Retrieves the internationalised  Authentication Type
     *
     * @return type of Authentication
     */
    public static String getAuthenticationType() {
        return HibernateUtils.getSystemSetting(HibernateUtils.SETTING_AUTHENTICATION_TYPE, null);
    }

    /**
     * This function will return Anonymous user if not there create it by default
     * with default Anonymous role
     *
     * @return userEntity object populated with Anonymous user
     */
    public static UserEntity getAnonymousUser() {
        UserEntity user;

        // verify User already exist

        List<UserEntity> entities = HibernateUtils.selectEntities("from UserEntity where lower(email)=?", UserEntity.DEFAULT_USER_NAME.toLowerCase());
        if (!Common.isBlank(entities))
            user = entities.get(0);

        else {
            // Load  the administration role

           // Create Anonymous user

            user = new UserEntity();
//            user.setName(UserEntity.DEFAULT_USER_NAME);
//            user.setUsername(UserEntity.DEFAULT_USER_NAME.toLowerCase());
            user.setPassword(Common.getMD5String(UserEntity.DEFAULT_USER_NAME.toLowerCase()));
//            if (role != null) user.setRole(role);
            HibernateUtils.save(user);
        }
        return user;
    }

    /**
     * Update the current user object in the session if the being passed
     * refers to the same user
     *
     * @param user User to add
     */
    public static void updateUserInSession(UserEntity user) {
        if (ServletHelper.getSession() != null) {
            UserEntity current = (UserEntity) ServletHelper.getSession().getAttribute(CURRENT_USER);
            if (current.getId().equals(user.getId())) setCurrentUser(user);
        }
    }

    /**
     * Logs a user out from the system by clearing out their entry in the
     * database and updating the activity
     */
    public static void logout() {
        if (ServletHelper.getSession() != null) {
            logout(null, ServletHelper.getSession().getId());
            ServletHelper.getSession().invalidate();
        }
    }

    /**
     * Logs a user out from the system by clearing out their entry in the
     * database and updating the activity
     *
     * @param appPath   URI of the user session
     * @param sessionId Session ID associated with this logout
     */
    public static void logout(String appPath, String sessionId) {
        Database db = new DatabaseHibernate();
        db.startTransaction();
        String URL = ((ServletHelper.getRequest() == null) ? "Housekeeping" : ServletHelper.getRequest().getRequestURL().toString());
        db.execute("INSERT into user_log (userid, sessionid, ip_address, user_agent, browser_locale, browser, browser_version, os, os_architecture, screen_resolution, colours, region, mobile, locale, path)\n" +
                "SELECT userid, sessionid, ip_address, user_agent, browser_locale, browser, browser_version, os, os_architecture, screen_resolution, colours, region, mobile, locale, '" + URL + "' as path   FROM user_status where sessionid='" + sessionId + "';");
        db.execute("delete from user_status where sessionid='" + sessionId + "'");
        db.commitTransaction();
        db.close();

        // If we are logging out our own session then clear it, otherwise send
        // a call to the server to log them out

        if (ServletHelper.getSession() != null && Common.doStringsMatch(ServletHelper.getSession().getId(), sessionId)) {
            ServletHelper.getSession().invalidate();
        }
        else if (appPath != null) {
            try {
                CookieManager cookieManager = new CookieManager();
                cookieManager.addCookie("JSESSIONID", sessionId);
                Common.getUrl(appPath + "/logout", 2000, null, null, cookieManager);
            }
            catch (Exception e) {
                logger.error("Failed to send logout to the users session - {}", PivotalException.getErrorMessage(e));
            }
        }
    }

    /**
     * Updates the user information so that we can tell when we got the last heartbeat
     *
     * @param keepAlive if true the last access is updated each heartbeat
     *
     * @return Logged in Status
     */
    public static String heartbeat(boolean keepAlive) {

        String returnValue = STATUS_OK;

        if (ServletHelper.getSession() != null && HibernateUtils.getSessionFactory() != null) {
            Database db = null;
            try {

                db = new DatabaseHibernate();

                // check to see if there is a user_status record, if not then heartbeat fails

                Map<String, Object> check = db.findFirst("select last_access from user_status where sessionid='" + ServletHelper.getSession().getId() + "'");
                if (check.size() > 0) {

                    // We have record so update last_heartbeat

                    Map<String, Object> tmp = new HashMap<>();
                    tmp.put("last_heartbeat", new Date());
                    if (keepAlive) {
                        logger.debug("Setting user session to not expire");
                        tmp.put("last_access", new Date());
                    }

                    db.updateRecord("user_status", String.format("sessionid='%s'", ServletHelper.getSession().getId()), tmp, false);

                    if (!keepAlive && check.containsKey("last_access") && check.get("last_access") instanceof Timestamp) {
                        // Check last_access time
                        // See if any sessions are going to log out in the next 5 minutes (unless the session timeout less than 5 minutes)
                        Integer sessionTimeoutWarning = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_SESSION_TIMEOUT_WARNING, HibernateUtils.SETTING_SESSION_TIMEOUT_WARNING_DEFAULT);
                        if (sessionTimeoutWarning > 4) {
                            logger.debug("Checking for session entries in user_status that are older than {} minutes", sessionTimeoutWarning);

                            Timestamp lastAccess = (Timestamp)check.get("last_access");

                            if (Common.getTimeDifference(lastAccess) > (sessionTimeoutWarning * 60000))
                                 returnValue = STATUS_TIMEOUT_WARNING;
                        }
                    }
                }
                else {
                    logger.debug("User session not found - logging out");
                    returnValue = STATUS_LOGGED_OUT;
                }
            }
            catch (Exception e) {
                logger.error("Cannot update the user status - {}", PivotalException.getErrorMessage(e));
            }
            finally {

                if (db != null) {
                    if (db.isInError())
                        logger.error("Error updating user_status - {}", db.getLastError());

                    db.close();
                }
            }
        }

         return returnValue;
    }

    /**
     * Check is current user have any of it's have Admin right then return true
     *
     * @return is Admin true else false
     */
    public static boolean isCurrentUserAdministrator() {
        boolean result = false;
//        for (RoleEntity roleEntity : getCurrentUser().getRolesMap().values()) {
//            if (roleEntity.isAdministrator()) {
//                result = true;
//            }
//        }
        return result;
    }

    /**
     * Used to display all Users if admin all users
     *
     * @return list of users with keys
     */
    public static List<Map<String, String>> getUsersAllDisplay() {
        List<Map<String, String>> result = new ArrayList<>();

        for (UserEntity user : getUsers()) {
            Map<String, String> temp = new HashMap<>();
            temp.put("text", user.getName());
            temp.put("value", String.valueOf(user.getId()));

            // id current user is  Administrator then show all

//            if (isCurrentUserAdministrator()) {
//                result.add(temp);
//            }
//            else {
//                if (!role.isAdministrator()) {
                    result.add(temp);
//                }
//            }
        }
        return result;
    }

    /**
     * Used to display Roles for current User Based if admin all roles
     *
     * @return list of  roles with keys
     */
    public static List<Map<String, String>> getRolesAllDisplay() {
        List<Map<String, String>> result = new ArrayList<>();

        for (RoleEntity role : getRoles()) {
            Map<String, String> temp = new HashMap<>();
            temp.put("text", role.getName());
            temp.put("value", String.valueOf(role.getId()));

            // id current user is  Administrator then show all

            if (isCurrentUserAdministrator()) {
                result.add(temp);
            }
            else {
                if (!Common.isYes(role.isAdministrator())) {
                    result.add(temp);
                }
            }
        }
        return result;
    }

    /**
     * This will check if current user is expired then invalidate it's session
     */

    public static void checkCurrentUserExpires() {
        HttpSession session = ServletHelper.getSession();
        if (!Common.isBlank(session)) {
            UserEntity usr = getCurrentUser();
            Map<String, HttpSession> sessions = Initialisation.getSessionMap();
            if (!Common.isBlank(usr) && (usr.isDisabled() || (!Common.isBlank(usr.getExpires()) && (new Date()).after(usr.getExpires())))) {
                UserManager.logout(null, session.getId());
                sessions.get(session.getId()).invalidate();
            }
        }
    }

    /**
     * Builds a map of privilege access objects
     *
     * @param privilegeIds              Comma separated list of privilege ids
     * @param privilegeAccessSettings   Comma separated list of access for each privilege
     *
     * @return Map containing Privilege access objects keyed by privilege name
     */
    public static Map<String, PrivilegeAccess>getPrivilegeAccessMap(String privilegeIds, String privilegeAccessSettings) {

        Map<String, PrivilegeAccess> returnValue = new LinkedCaseInsensitiveMap<>();

        if (!Common.isBlank(privilegeIds)) {
            List<String> privilegeList = Common.splitToList(privilegeIds, " *, *");
            List<String> accessList = Common.splitToList(privilegeAccessSettings, " *, *");
            String privilegeId;
            String privilegeAccess;
            Privileges privilege;
            for (int index = 0; index < privilegeList.size(); index++) {
                privilegeId = privilegeList.get(index);
                if (!Common.isBlank(privilegeId)) {
                    if (accessList != null && index>=0 && index<accessList.size()) {
                        privilegeAccess = accessList.get(index);
                        privilege = Privileges.get(Common.parseInt(privilegeId));
                        if (!Common.isBlank(privilege)) {
                            returnValue.put(privilege.name(), new PrivilegeAccess(privilege, privilegeAccess));
                        }
                    }
                }
            }
        }

        return returnValue;
    }

    /**
     * Builds a map of privileges keyed by name
     *
     * @param privilegeIds comma separated list of privilege ids
     *
     * @return Map of privileges
     */
    public static Map<String, Privileges> getPrivilegeMap(String privilegeIds) {

        Map<String, Privileges> privilegesMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        // Add privileges

        if (!Common.isBlank(privilegeIds)) {
            for (String privilegeId : Common.splitToList(privilegeIds," *, *")) {
                if (!Common.isBlank(privilegeId)  ) {
                    Privileges privilege = Privileges.get(Common.parseInt(privilegeId));
                    if(!Common.isBlank(privilege)) {
                        privilegesMap.put(privilege.name(), privilege);
                    }
                }
            }
        }

        return privilegesMap;
    }

    /**
     * Get privilege settings from html form
     * Expect parameters to contain access_&lt;#PrivId&gt;
     * Each access is either NONE, READ or WRITE and is abbreviated to n,r or w
     *
     * @param request form submission request object
     *
     * @return map keyed by privilege id with values of access settings
     */
    public static Map<String, String> extractPrivilegesFromForm(HttpServletRequest request) {

        Map<String, String>retValue = new HashMap<>();

        String privId;
        String privAccess;
        for (String key : request.getParameterMap().keySet()) {
            if (key.startsWith("access")) {
                privId = Common.getItem(key, "_", 1);
                privAccess = request.getParameter("access_" + privId);
                if (!Common.isBlank(privAccess)) {
                    privAccess = privAccess.substring(0, 1);

                    if (!Common.isBlank(privId) && !"n".equalsIgnoreCase(privAccess)) {
                        retValue.put(privId, privAccess);
                    }
                }
            }
        }

        return retValue;
    }

     /**
    * Returns a list of roles given the comma separated list of role IDs
    *
    * @param roleIds Comma separated list of role IDs
    * @param translate True if the names should be translated
    * @return List of Role names
    */
    public static String getRoleNames(String roleIds, boolean translate) {
       String returnValue = null;
       if (!Common.isBlank(roleIds)) {
           List<Object> tmp = HibernateUtils.selectEntities(String.format("select name from RoleEntity where id in (%s)", roleIds));
           if (!Common.isBlank(tmp)) {
               if (translate) {
                   List<String> names = new ArrayList<>();
                   for (Object name : tmp) {
                       names.add(I18n.getString((String) name));
                   }
                   returnValue = Common.join(names, ", ");
               }
               else
                   returnValue = Common.join((Collection)tmp);
           }
       }
       return returnValue;
    }
}
