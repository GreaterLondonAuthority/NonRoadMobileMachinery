/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.servlet;

import com.pivotal.api.PoolBroker;
import com.pivotal.reporting.scheduler.ScheduleMonitor;
import com.pivotal.system.data.cache.CacheAccessorFactory;
import com.pivotal.system.hibernate.annotations.InitialValue;
import com.pivotal.system.hibernate.entities.AbstractEntity;
import com.pivotal.system.hibernate.entities.DatasourceEntity;
import com.pivotal.system.hibernate.entities.LogEntity;
import com.pivotal.system.hibernate.entities.ScheduledTaskEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.monitoring.EmailMonitor;
import com.pivotal.system.monitoring.EventMonitor;
import com.pivotal.system.monitoring.jmx.*;
import com.pivotal.utils.*;
import com.pivotal.web.Constants;
import com.pivotal.web.notifications.NotificationManager;
import com.pivotal.web.utils.ThemeManager;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.stereotype.Component;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.io.*;
import java.lang.reflect.Field;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Manifest;

/**
 * Used to capture context and session events - referred to in the web.xml
 */
@Component
public class Initialisation implements ServletContextListener, HttpSessionListener {
    private static final int MAX_ARCHETYPE_INIT_SANITY_COUNT = 1000;
    private static org.slf4j.Logger logger;
    private static String logPropertiesFilename;
    private static File logDir;

    // The host provider for the application. We need this statically as we will be coming back to it if we need to restart
    // the services.
//    protected static HostComponentProvider hostComponentProvider;

    // Access to the servlet context (when we do a restart)
    protected static ServletContextEvent servletContextEvent;

    // Cache of all local sessions
    private static final Map<String, HttpSession> sessionCache = new ConcurrentHashMap<>();

    static {
        logger = null;
        logPropertiesFilename = null;
        logDir = null;
    }

//    /**
//     * Provides access to the supplied bean
//     */
//    @Autowired
//    void setProvider(HostComponentProvider hostComponentProvider) {
//        Initialisation.hostComponentProvider = hostComponentProvider;
//    }

    /**
     * Notification that the web application initialization process is starting. All ServletContextListeners are notified of context initialization before any
     * filter or servlet in the web application is initialized.
     *
     * @param servletContextEvent Context event
     */
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        Initialisation.servletContextEvent = servletContextEvent;

        // Attempt to autowire this class using the current application context

//        autowire(servletContextEvent.getServletContext());

        // Get the application version from the manifest file, if it exists

        initApplicationVersion(servletContextEvent);

        // Initialise the application

        startup();
    }

    /**
     * Will start the application server services. Can be called from other places than just the initialisation
     */
    public static void startup() {
        startup(false, false);
    }

    /**
     * Will start the application server services. Can be called from other places than just the initialisation
     *
     * @param isNewDatabase True if the archetypes must eb reloaded
     * @param isRestart     True if this is a restart and services are already running
     */
    public static void startup(boolean isNewDatabase, boolean isRestart) {
        try {
            // Set the identity of the instance

            ServletHelper.setAppIdentity(servletContextEvent.getServletContext());
            logger.info("{} instance initialisation started with identity {} version {}", Common.getAplicationName(), ServletHelper.getAppIdentity(), Constants.APPLICATION_VERSION);

            // Install the mime types to use

            installDefaultMimeTypes(servletContextEvent.getServletContext());

            // Create the US translations

            I18n.createUsTranslations();

            // Setup our app database

            logger.info("Checking validity of {} database", Common.getAplicationName());
            boolean newDatabase = HibernateUtils.setupAppDatabase(servletContextEvent) || isNewDatabase;

            // Initialise hibernate

            logger.info("Initialising hibernate");
            HibernateUtils.initHibernate(servletContextEvent, null);

            // Initialise all the database assets

            logger.info("Initialising archetypes");
            initialiseArchetypes(newDatabase);

            // Initialise all the themes

            logger.info("Initialising themes");
            ThemeManager.initThemes();

            // Only continue if this isn't a restart request

            if (!isRestart) {
                startServices();
            }
            logger.debug("{} initialisation finished", ServletHelper.getAppIdentity());
        }
        catch (Exception e) {

            // The context failed to initialise so show the error and shutdown

            logger.error(PivotalException.getErrorMessage(e));
            throw new RuntimeException("Catastrophic error - cannot continue - " + PivotalException.getErrorMessage(e));
        }
    }

    /**
     * Starts all the services and monitors
     */
    public static void startServices() {
        try {

            // Register the JMX MBeans

            logger.info("Registering JMX interface");
            registerJmxBeans();

            // We need to clear out any old locked tasks from the scheduler for this
            // instance of app

            logger.info("Clearing old locked tasks");
            ScheduledTaskEntity.clearScheduledTaskStatus();

            // Clean up any old temporary files

            logger.info("Clearing up temporary files");
            clearTemporaryFiles();

            // Start the eventMonitor

            logger.info("Starting Event Monitor");
            EventMonitor.init("Event Monitor", HibernateUtils.getSystemSetting(HibernateUtils.SETTING_MONITOR_PERIOD, HibernateUtils.SETTING_MONITOR_PERIOD_DEFAULT), HibernateUtils.getSystemSetting(HibernateUtils.SETTING_MONITOR_PERIOD_DEAD, HibernateUtils.SETTING_MONITOR_PERIOD_DEAD_DEFAULT)).startMonitor();

            // Start the emailMonitor

            logger.info("Starting Email Monitor");
            EmailMonitor.init("Email Monitor", HibernateUtils.getSystemSetting(HibernateUtils.SETTING_MONITOR_PERIOD, HibernateUtils.SETTING_MONITOR_PERIOD_DEFAULT), HibernateUtils.getSystemSetting(HibernateUtils.SETTING_MONITOR_PERIOD_DEAD, HibernateUtils.SETTING_MONITOR_PERIOD_DEAD_DEFAULT)).startMonitor();

            // Start the notifications manager

            logger.info("Starting Notifications Manager");
            NotificationManager.init("Notifications Manager").startMonitor();

            // Start the scheduler

            logger.info("Starting Scheduler");
            ScheduleMonitor.init("Scheduler").startMonitor();

        }
        catch (Exception e) {

            // The context failed to initialise so show the error and shutdown

            logger.error(PivotalException.getErrorMessage(e));
            throw new RuntimeException("Catastrophic error - cannot continue - " + PivotalException.getErrorMessage(e));
        }
    }

    /**
     * Attempts to set the application version by getting it from the Manifest file
     *
     * @param servletContextEvent Servlet context
     */
    protected void initApplicationVersion(ServletContextEvent servletContextEvent) {
        File manifestFile = new File(servletContextEvent.getServletContext().getRealPath(Constants.MANIFEST_FILE));
        if (manifestFile.exists()) {
            try {
                ServletContext application = servletContextEvent.getServletContext();
                Manifest manifest = new Manifest(application.getResourceAsStream(Constants.MANIFEST_FILE));
                Constants.APPLICATION_VERSION = manifest.getMainAttributes().getValue("Implementation-Version");
                Constants.APPLICATION_BUILD_DATE = new Date(manifestFile.lastModified());
            }
            catch (Exception e) {
                logger.debug("Problem trying to set the version number from the manifest - {}", PivotalException.getErrorMessage(e));
            }
        }
    }

    /**
     * Attempt to register the JMX providers
     */
    private static void registerJmxBeans() {
        Configuration.registerMBean();
        Templating.registerMBean();
        Tasks.registerMBean();
        JDBCPool.registerMBean();
        Performance.registerMBean();
    }

    /**
     * Looks for any app specific temporary files and deletes them
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void clearTemporaryFiles() {
        List<File> files = Common.listFiles(Common.getTemporaryDirectory(), ServletHelper.getAppIdentity() + ".+");
        if (!Common.isBlank(files)) {
            for (File file : files) {
                if (file.exists()) file.delete();
            }
        }
    }

    /**
     * Will shutdown the application server services.
     */
    public static void shutdown() {
        shutdown(true);
    }

    /**
     * Will shutdown the application server services.
     *
     * @param deregisterJDBCDrivers True if the JDBC drivers should be de-registered from the driver manager
     */
    public static void shutdown(boolean deregisterJDBCDrivers) {

        // Stop the schedule

        Date startTime = new Date();
        logger.info("Stopping {} {}", Common.getAplicationName(), ServletHelper.getAppIdentity());

        logger.info("Stopping Scheduler");
        ScheduleMonitor.shutdown();

        logger.info("Stopping Event monitor");
        EventMonitor.shutdown();

        logger.info("Stopping Notifications Manager");
        NotificationManager.shutdown();

        logger.info("Closing Hibernate sessions");
        if (HibernateUtils.isInitialised()) {
            LogEntity.addLogEntry(LogEntity.STATUS_SERVER_STOPPED, "Server [" + ServletHelper.getAppIdentity() + "] stopped", Common.getTimeDifference(startTime));
            HibernateUtils.close();
        }

        logger.info("Closing connections pools");
        PoolBroker.shutdown();

        logger.info("Shutting down cache");
        CacheAccessorFactory.shutdown();

        if (deregisterJDBCDrivers) {
            logger.info("De-registering JDBC drivers");
            shutdownJDBCDrivers();
        }

        // Remove the mapping proxy settings

//        SettingsController.removeMappingProxyConfig();


        logger.info("Application shutdown complete");
    }

    /**
     * Notification that the servlet context is about to be shut down. All servlets and filters have been destroy()ed before any ServletContextListeners are
     * notified of context destruction.
     *
     * @param servletContextEvent Context event
     */
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

        // Shutdown

        shutdown();
    }

    /**
     * De-register all the active drivers to prevent a memory leak
     */
    private static void shutdownJDBCDrivers() {

        // This manually de-registers JDBC driver, which prevents Tomcat 7 from complaining about memory leaks wrt this class

        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            try {
                DriverManager.deregisterDriver(driver);
                logger.debug("De-registering jdbc driver: {}", driver);
            }
            catch (SQLException e) {
                logger.error("Error de-registering driver {}", e, driver);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * Notification that a session was created
     */
    public void sessionCreated(HttpSessionEvent httpSessionEvent) {
        sessionCache.put(httpSessionEvent.getSession().getId(), httpSessionEvent.getSession());
    }

    /**
     * {@inheritDoc}
     *
     * Notification that a session was destroyed
     */
    public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
        sessionCache.remove(httpSessionEvent.getSession().getId());
    }

    /**
     * Returns a map of all the currently tracked sessions
     *
     * @return Map of sessions keyed on their ID
     */
    public static Map<String, HttpSession> getSessionMap() {
        return new HashMap<>(sessionCache);
    }

    /**
     * Convenient way of retrieving the log directory property value
     *
     * @param context Http servlet context
     * @return Name of the system property e.g. app-bell.logdir
     */
    public static String getLogDirProperty(ServletContext context) {
        if (context == null) return getLogDirProperty((String) null);
        else return getLogDirProperty(context.getContextPath());
    }

    /**
     * Convenient way of retrieving the log directory property value
     *
     * @param request Http request
     * @return Name of the system property e.g. app-bell.logdir
     */
    @SuppressWarnings("unused")
    public static String getLogDirProperty(HttpServletRequest request) {
        if (request == null || request.getSession() == null) return getLogDirProperty((String) null);
        else return getLogDirProperty(request.getSession().getServletContext());
    }

    /**
     * Convenient way of retrieving the log directory property value
     *
     * @param servletPath Servlet path e.g. /bell
     * @return Name of the system property e.g. app-bell.logdir
     */
    private static String getLogDirProperty(String servletPath) {

        // We mustn't use the Common methods in here because this will
        // load the static logger before it has been potentially initialised

        if (servletPath == null || servletPath.isEmpty()) return "app.logdir";
        else return "app-" + servletPath.replaceAll("^/", "") + ".logdir";
    }

    /**
     * Reads the contents of the file and returns them as a string
     *
     * @param sFilename File to read
     * @return Contents of the file
     */
    private static String readTextFile(String sFilename) {
        String sReturn = null;
        StringBuilder sTmp = new StringBuilder();

        try (BufferedReader objIn = new BufferedReader(new InputStreamReader(new FileInputStream(sFilename), "UTF-8"))) {
            while ((sReturn = objIn.readLine()) != null) {
                if (sTmp.length() > 0) sTmp.append('\n');
                sTmp.append(sReturn);
            }
            sReturn = sTmp.toString();
        }
        catch (IOException e) {
            System.out.println("Problem reading file " + PivotalException.getErrorMessage(e));
        }
        return sReturn;
    }

    /**
     * Writes a string to the specified file
     *
     * @param sFilename File to write to
     * @param sValue    String t write to the file
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void writeTextFile(String sFilename, String sValue) {
        File objFile = new File(sFilename);
        if (objFile.exists()) objFile.delete();
        try (Writer objOut = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(objFile), "UTF-8"))) {
            objOut.write(sValue);
        }
        catch (Exception e) {
            System.out.println("Problem writing file " + PivotalException.getErrorMessage(e));
        }
    }

    /**
     * Initialises the logging environment using a combination of system settings and default values
     *
     * @param context   Servlet context
     * @param logConfig Configuration properties
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void initLoggingEnv(ServletContext context, String logConfig) {

        // Get the default stuff
        // Try and use the Catalina base folder if it exists

        String warning = null;
        String baseDir = System.getProperty(Constants.CATALINA_BASE_SETTING);
        if (baseDir != null && !baseDir.isEmpty())
            logDir = new File(baseDir + '/' + Constants.DEFAULT_CATALINA_LOG_DIR);

        if (!logDir.exists() || !logDir.isDirectory())
            logDir = new File(context.getRealPath(Constants.DEFAULT_LOG_DIR));
        logPropertiesFilename = context.getRealPath(Constants.LOG4J_PROPERTIES_FILE);

        // Check if the file has been overridden with a system property that points to
        // a parent folder containing child folders for each instance of App

        String logPath = System.getProperty(Constants.APP_DEFAULT_LOGDIR);
        if (logPath != null && !logPath.isEmpty()) {
            File tmp = new File(logPath);
            if (!tmp.exists())
                warning = String.format("The log folder %s specified in the startup parameter %s does not exist - the default %s will be used", logPath, Constants.APP_DEFAULT_LOGDIR, logDir);
            else if (!tmp.canWrite())
                warning = String.format("The log folder %s is not writable - the default %s will be used", logPath, logDir);
            else {
                if (context.getContextPath().isEmpty()) logPath += "/root";
                else logPath += context.getContextPath();
                logDir = new File(logPath);

                // Check to see if we have an instance specific version of the log config

                logPropertiesFilename = logPath + '/' + Constants.LOG4J_PROPERTIES_FILE.replaceFirst("^.+/", "");
            }
        }
        if (!logDir.exists()) logDir.mkdirs();

        // Read the config text if we haven't been sent it
        // NOTE :- We have to use the private versions of the readTextFile and writeTextFile so that
        //         we don't inadvertently instantiate a static logger before we have configured it

        if (logConfig == null) {
            if (new File(logPropertiesFilename).exists()) logConfig = readTextFile(logPropertiesFilename);
            if (logConfig == null || logConfig.trim().isEmpty()) {
                logConfig = readTextFile(context.getRealPath(context.getContextPath()) + Constants.LOG4J_PROPERTIES_FILE);
                writeTextFile(logPropertiesFilename, logConfig);
            }
        }

        // Configure the Log4j environment

        System.setProperty(getLogDirProperty(context), logDir.getAbsolutePath() + '/');
        logConfig = logConfig.replaceAll("\\$\\{.*app\\.logdir\\}", "\\${" + getLogDirProperty(context) + '}');
        writeTextFile(logPropertiesFilename, logConfig);

        // Write the config out and reset the logging engine

        BasicConfigurator.resetConfiguration();
        PropertyConfigurator.configure(logPropertiesFilename);
        logger = org.slf4j.LoggerFactory.getLogger(Initialisation.class);

        // Tell the user if there has been a problem

        if (warning != null) logger.warn(warning);
    }

    /**
     * Returns the logging directory
     *
     * @return File object
     */
    public static File getLogDir() {
        return logDir;
    }

    /**
     * Returns the log file properties path name
     *
     * @return Fully qualified path name
     */
    public static String getLogPropertiesFilename() {
        return logPropertiesFilename;
    }

    /**
     * Installs the Mime types from a static file
     *
     * NOTE:- This configures the mime types for the whole JVM, not just for this web application
     *
     * @param objServlet Servlet context to tie it all into
     */
    private static void installDefaultMimeTypes(ServletContext objServlet) {
        File objMimeTypeFile = new File(ServletHelper.getRealPath(objServlet, Constants.MIME_TYPES_FILE));
        if (objMimeTypeFile.exists()) {
            try {
                MimetypesFileTypeMap objTmp = new MimetypesFileTypeMap(objMimeTypeFile.getAbsolutePath());
                FileTypeMap.setDefaultFileTypeMap(objTmp);
                if (logger != null) logger.info("Loaded mime types from file - {}", objMimeTypeFile.getAbsolutePath());
            }
            catch (Exception e) {
                if (logger != null)
                    logger.error("Cannot install default Mime Types from file - {}", PivotalException.getErrorMessage(e));
            }
        }
    }

    /**
     * Used to initialise the system with the default values provided by gthe vendor There are a lot of system setup parameters
     * that need to be seeded into the database and other system wide properties.
     * These are handled ostensibly by the owning entity which is meant to carry a number of static fields that are
     * expressed as JSON properties annotated with @InitialValue.
     * Each of these are exercised to create/update entities
     * An annotated field may have a dependency on another class in which case, this method keeps looping
     * round until all the dependencies have been satisfied - it also allows for the situation whereby
     * fields inside the same class may have different dependencies or non at all.
     *
     * @param newDatabase True if this method is acting on a completely empty database
     */
    public static void initialiseArchetypes(boolean newDatabase) {

        // Set of classes and the fields within them that have been initialised

        Set<Class> createdEntities = new HashSet<>();
        Set<String> createdFields = new HashSet<>();
        int sanityCount = 0;

        try {

            // Get a list of all the classes (Using AbstractEntity as it should always be there)

            List<Class> classes = Common.sortListObjects(ClassUtils.getClasses(AbstractEntity.class.getPackage().getName()), "getSimpleName");

            // Loop whilst we still have entities to create

            if (!Common.isBlank(classes)) {
                boolean entitesToCreate = true;
                while (entitesToCreate) {

                    // Check to see if we have been round this loop way too many times

                    sanityCount++;
                    if (sanityCount > MAX_ARCHETYPE_INIT_SANITY_COUNT) {
                        throw new PivotalException("The archetype initialisation is stuck in a loop and will not complete");
                    }
                    entitesToCreate = false;
                    for (Class clazz : classes) {

                        // If we haven't already initialised this class fully

                        if (!createdEntities.contains(clazz)) {

                            boolean fullyCreated = true;
                            List<Field> fields = ClassUtils.getFields(clazz, InitialValue.class);

                            for (Field field : fields) {
                                InitialValue initialValue = field.getAnnotation(InitialValue.class);

                                // Check if this can be applied to this database

                                if (!initialValue.onlyNewDatabase() || newDatabase) {

                                    // Make sure we haven't already created it

                                    String fieldName = clazz.getName() + '.' + field.getName();
                                    if (!createdFields.contains(fieldName)) {

                                        // If this field depends on another class then make sure it exists

                                        Class[] depends = initialValue.depends();
                                        if (Common.isBlank(depends) || createdEntities.containsAll(Arrays.asList(depends))) {
                                            try {
                                                // Get any prepends and appends

                                                Map<String, Object> constants = getStringObjectMap(initialValue.constants());
                                                Map<String, Object> prepends = getStringObjectMap(initialValue.prepends());
                                                Map<String, Object> appends = getStringObjectMap(initialValue.appends());
                                                Map<String, Object> defaults = getStringObjectMap(initialValue.defaults());

                                                // Get the value of the field

                                                Object fieldValue = field.get(field.getClass());
                                                Object entity;

                                                // If it's a string, then turn it into an array

                                                Map<String, Object> propertyValues;
                                                if (fieldValue instanceof String) {
                                                    fieldValue = new String[]{(String) fieldValue};
                                                }

                                                // If it's a string array, then it's an array of maps

                                                if (fieldValue instanceof String[]) {
                                                    for (String value : (String[]) fieldValue) {
                                                        propertyValues = JsonMapper.parseJson(value);
                                                        adjustMapWithAppendsAndPrepends(propertyValues, constants, defaults, prepends, appends);
                                                        entity = HibernateUtils.createEntity(clazz, propertyValues);
                                                        if (entity != null) {
                                                            logger.debug("Created/updated {}", entity);
                                                        }
                                                        else {
                                                            logger.error("Cannot create [{}] using {}", clazz.getSimpleName(), value);
                                                        }
                                                    }
                                                }

                                                // Ummmm, that's not good
                                                // Log it and carry on

                                                else {
                                                    logger.error("The initialisation field [{}.{}] is neither a String or an array of Strings so cannot be used", clazz.getSimpleName(), field.getName());
                                                }
                                            }
                                            catch (Exception e) {
                                                logger.error("Problem initialising [{}] - {}", clazz.getSimpleName(), PivotalException.getErrorMessage(e));
                                            }

                                            // Add the field to the list of ones that have been actioned

                                            createdFields.add(fieldName);
                                            logger.debug("Created field for {} - {}", clazz.getSimpleName(), field.getName());
                                        }
                                        else {
                                            logger.debug("Field not created for {} - {} waiting for {}", clazz.getSimpleName(), field.getName(), getStringFromArray(initialValue.depends()));
                                            fullyCreated = false;
                                            entitesToCreate = true;
                                        }
                                    }
                                    else {
                                        logger.debug("Field skipped, already created for {} - {}", clazz.getSimpleName(), field.getName());
                                    }
                                }
                            }

                            // If all the fields have been applied successfully

                            if (fullyCreated) {
                                if (!Common.isBlank(fields))
                                    logger.debug("Nothing to create for {}", clazz.getSimpleName());
                                else
                                    logger.debug("Created all types for {}", clazz.getSimpleName());
                                createdEntities.add(clazz);
                            }
                            else {
                                logger.debug("Not fully created {}", clazz.getSimpleName());
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            logger.error("Problem getting list of classes - {}", PivotalException.getErrorMessage(e));
        }

        // Create the data source archetype

        DatasourceEntity.createSystemDatasource();
    }

    /**
     * Retrieves the values of the JSON string converted to a Map
     *
     * @param values JSON map string
     * @return Map of values (never null)
     */
//    @NotNull
    public static Map<String, Object> getStringObjectMap(String values) {
        Map<String, Object> returnValue = new HashMap<>();
        if (!Common.isBlank(values)) returnValue = JsonMapper.parseJson(values);
        return returnValue;
    }

    /**
     * Decorates the values with any prepend and append text available
     *
     * @param values    Values to decorate
     * @param constants Values to prepend
     * @param defaults  Values to prepend
     * @param prepends  Values to prepend
     * @param appends   Values to append
     */
    private static void adjustMapWithAppendsAndPrepends(Map<String, Object> values, Map<String, Object> constants, Map<String, Object> defaults, Map<String, Object> prepends, Map<String, Object> appends) {
        if (!Common.isBlank(values)) {

            // Add the defaults for any missing values

            if (!Common.isBlank(defaults)) {
                for (String key : defaults.keySet()) {
                    if (!values.containsKey(key)) {
                        values.put(key, defaults.get(key));
                    }
                }
            }

            // Apply any live substitutions

            substituteConstants(values, values);

            // Prepend the values

            if (!Common.isBlank(prepends)) {
                for (String key : prepends.keySet()) {
                    if (values.containsKey(key)) {
                        values.put(key, prepends.get(key) + (String) values.get(key));
                    }
                }
            }

            // Append the values

            if (!Common.isBlank(appends)) {
                for (String key : appends.keySet()) {
                    if (values.containsKey(key)) {
                        values.put(key, (String) values.get(key) + appends.get(key));
                    }
                }
            }

            // Apply any constants substitutions - we do it twice in case the constants
            // contain references to other constants

            substituteConstants(values, constants);
            substituteConstants(values, constants);
        }
    }

    /**
     * Substitutes values from the constants map into the values map
     *
     * @param values       Values to decorate
     * @param replacements Values to use as replacements
     */
    private static void substituteConstants(Map<String, Object> values, Map<String, Object> replacements) {
        if (!Common.isBlank(values) && !Common.isBlank(replacements)) {

            // Apply any substitutions

            for (String key : values.keySet()) {
                for (String replacementKey : replacements.keySet()) {
                    try {
                        String pattern = String.format("(?is)#%s#", replacementKey);
                        values.put(key, ((String) values.get(key)).replaceAll(pattern, (String) replacements.get(replacementKey)));
                    }
                    catch (Exception e) {
                        logger.debug("Don't really care");
                    }
                }
            }
        }
    }

    /**
     * Convenient way yo get a string representation of an array of classes
     *
     * @param classes Array of classes
     * @return String of their simple names
     */
    private static String getStringFromArray(Class[] classes) {
        String returnValue = null;
        if (!Common.isBlank(classes)) {
            for (Class clazz : classes) {
                if (returnValue != null) {
                    returnValue += ", " + clazz.getSimpleName();
                }
                else {
                    returnValue = clazz.getSimpleName();
                }
            }
        }
        return returnValue;
    }

}
