/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 *
 * This Class exposes any velocity related utilities.
 * When a velocity engine is needed we should get it with the getEngine() method available in VelocityUtils.
 * This method requests a new engine to a pool of VelocityEngine.
 * After this engine is no longer needed it should be returned to the pool and this must be done using the returnEngine(Engine) method available in this class as well.
 * The properties values for the pool have default values. However, they can be overridden in the spring servlet definition.
 * Using spring's @Autowired annotation we can set the values for these static properties.
 */
package com.pivotal.utils;

import com.pivotal.reporting.reports.sqldump.TextOutput;
import com.pivotal.system.data.cache.CacheEngineSafe;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.security.*;
import com.pivotal.utils.workflow.WorkflowHelper;
import com.pivotal.web.controllers.AbstractAdminController;
import com.pivotal.web.controllers.utils.MappingManager;
import com.pivotal.web.servlet.ServletHelper;
import com.pivotal.web.utils.ThemeManager;
import org.apache.commons.lang.*;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.generic.MathTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.apache.velocity.tools.generic.SortTool;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Provides a consistent place to get Velocity related resources
 */
@Component
public class VelocityUtils {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(VelocityUtils.class);

    /**
     * Constant <code>CACHE_ENGINE="Cache"</code>
     */
    public static final String CACHE_ENGINE = "Cache";
    /**
     * Constant <code>READINGS_MANAGER="ReadingsManager"</code>
     */
    public static final String READINGS_MANAGER = "ReadingsManager";
    /**
     * Constant <code>CONTEXT="Context"</code>
     */
    public static final String CONTEXT = "Context";
    /**
     * Constant <code>CAPTCHA_CONTEXT_NAME="Captcha"</code>
     */
    public static final String CAPTCHA_CONTEXT_NAME = "Captcha";
    /**
     * Constant <code>TEXT_OUTPUT="TextOutput"</code>
     */
    public static final String TEXT_OUTPUT = "TextOutput";
    /**
     * Constant <code>RUNTIME_ENGINE_IDENTIFIER="runtime.engine.identifer"</code>
     */
    public static final String RUNTIME_ENGINE_IDENTIFIER = "runtime.engine.identifier";
    /**
     * Constant <code>USER_MANAGER="UserManager"</code>
     */
    public static final String USER_MANAGER = "UserManager";
    /**
     * Constant <code>CASE_MANAGER="UserManager"</code>
     */
    public static final String CASE_MANAGER = "CaseManager";
    /**
     * Constant <code>WIZARD_MANAGER="UserManager"</code>
     */
    public static final String WIZARD_MANAGER = "WizardManager";
    /**
     * Constant <code>PRIVILEGES="Privileges"</code>
     */
    public static final String PRIVILEGES = "Privileges";
    /**
     * Constant <code>INTERNATIONALISATION="Internationalisation"</code>
     */
    public static final String INTERNATIONALISATION = "Internationalisation";

    private static VelocityEngine engine = makeEngine(true);
    private static Map<Class, Map<String, Object>> constantsCache;
    private static Map<String, Object> contextCache;

    /**
     * Prevent instantiation
     */
    private VelocityUtils() {
    }

    /**
     * Returns an initialised velocity engine
     *
     * @return Velocity engine
     */
    public static VelocityEngine getEngine() {
        return engine;
    }

    /**
     * Creates and initialises a velocity context
     *
     * @return Velocity context
     */
    public static Context getVelocityContext() {
        Context context = new VelocityContext();

        // Get all the objects from the context

        Map<String, Object> cacheContext = getVelocityContextMap();
        for (Map.Entry entry : cacheContext.entrySet()) {
            context.put((String) entry.getKey(), entry.getValue());
        }

        // Add the current user to the context

        context.put(UserManager.CURRENT_USER, UserManager.getCurrentUser());

        // Add their preferences
        if (UserManager.getCurrentUser() != null) {
            context.put(UserManager.CURRENT_USER_PREFERENCES, UserManager.getCurrentUser().getPreferences());
        }
        // Set the theme to use

        ThemeManager.setTheThemeToUse(context);

        // Set Interface

        String userInterface = null;
//        if (UserManager.isUserLoggedIn())
//            userInterface = UserManager.getCurrentUser().getUserInterface();

        if (Common.isBlank(userInterface))
            userInterface = UserManager.INTERFACE_STANDARD;

        addConstants(context, UserManager.class);

        context.put(UserManager.USER_INTERFACE, userInterface);

        return context;
    }

    /**
     * Will return the current context as {@link java.util.Map}
     *
     * @return The context contained within a {@link java.util.Map}
     */
    synchronized public static Map<String, Object> getVelocityContextMap() {

        // Only generate if the cache is empty
        if (contextCache == null) {
            contextCache = new HashMap<>();

            // Add ll the default stuff
            contextCache.put("TmpDir", Common.getTemporaryDirectory());
            contextCache.put("math", new MathTool());
            contextCache.put("number", new NumberTool());
            contextCache.put("sort", new SortTool());
            contextCache.put("LSQUARE_CHAR", '[');
            contextCache.put("RSQUARE_CHAR", ']');
            contextCache.put("LCURLY_CHAR", '{');
            contextCache.put("RCURLY_CHAR", '}');
            contextCache.put("DOT_CHAR", '.');
            contextCache.put("NEWLINE", '\n');
            contextCache.put("HASH", '#');
            contextCache.put("DOLLAR", '$');
            contextCache.put("DQUOTE", '"');
            contextCache.put("APPLICATION_INSTANCE_NAME", Common.getAplicationName());
            addConstants(contextCache, Calendar.class);
            addConstants(contextCache, HttpServletResponse.class);
            addConstants(contextCache, Pattern.class);
            addConstants(contextCache, Privileges.class);
            addConstants(contextCache, PrivilegeAccess.class);

            contextCache.put("XMLUtils", XMLUtils.class);
            contextCache.put("Utils", Common.class);
            contextCache.put("utils", Common.class);
            contextCache.put("HibernateUtils", HibernateUtils.class);
            contextCache.put("LookupHelper", LookupHelper.class);
            contextCache.put("WorkflowHelper", WorkflowHelper.class);
            contextCache.put("LDAP", Directory.class);
            contextCache.put("Pattern", Pattern.class);
            contextCache.put("WordUtils", WordUtils.class);
            contextCache.put("StringUtils", StringUtils.class);
            contextCache.put("StringEscapeUtils", StringEscapeUtils.class);
            contextCache.put("CharUtils", CharUtils.class);
            contextCache.put("RandomStringUtils", RandomStringUtils.class);
            contextCache.put("ArrayUtils", ArrayUtils.class);
            contextCache.put("ControllerUtils", AbstractAdminController.class);
            contextCache.put("JsonUtils", JsonMapper.class);
            contextCache.put("ServletHelper", ServletHelper.class);
            contextCache.put("MappingManager", MappingManager.class);
            contextCache.put(TEXT_OUTPUT, TextOutput.class);
            contextCache.put(CACHE_ENGINE, CacheEngineSafe.class);
            contextCache.put(CACHE_ENGINE, CacheEngineSafe.class);
            contextCache.put(CONTEXT, contextCache);
            contextCache.put(USER_MANAGER, UserManager.class);
            contextCache.put(CASE_MANAGER, CaseManager.class);
            contextCache.put(PRIVILEGES, Privileges.class);
            contextCache.put(INTERNATIONALISATION, I18n.class);

            // Get parameters specified in context.xml

            ServletContext servletContext = ServletHelper.getServletContext();
            Enumeration<String> servletParams = servletContext.getInitParameterNames();
            for(String paramName : Collections.list(servletParams))
                contextCache.put(paramName, servletContext.getInitParameter(paramName));

        }
        return contextCache;
    }

    /**
     * Adds constants and their values to the context using reflection
     *
     * @param model      Model to populate
     * @param fieldClass Class from which to read the constants
     */
    public static void addConstants(Model model, Class fieldClass) {

        Map<String, Object> fields = getConstants(fieldClass);

        if (!Common.isBlank(fields)) {
            for (Map.Entry entry : fields.entrySet()) {
                model.addAttribute((String) entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Adds constants and their values to the context using reflection
     *
     * @param context    Context to populate
     * @param fieldClass Class from which to read the constants
     */
    public static void addConstants(Map<String, Object> context, Class fieldClass) {

        Map<String, Object> fields = getConstants(fieldClass);

        if (!Common.isBlank(fields)) {
            for (Map.Entry entry : fields.entrySet()) {
                context.put((String) entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Adds constants and their values to the context using reflection
     *
     * @param context    Context to populate
     * @param fieldClass Class from which to read the constants
     */
    public static void addConstants(Context context, Class fieldClass) {

        Map<String, Object> fields = getConstants(fieldClass);

        if (!Common.isBlank(fields)) {
            for (Map.Entry entry : fields.entrySet()) {
                context.put((String) entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * gets constants and their values to the context using reflection
     *
     * @param fieldClass Class from which to read the constants
     *
     * @return Map of constants for the fieldClass
     */
    public static Map<String, Object> getConstants(Class fieldClass) {

        Map<String, Object> fields = null;
        if (!Common.isBlank(fieldClass)) {

            if (Common.isBlank(constantsCache))
                constantsCache = new HashMap<>();
            else
                fields = constantsCache.get(fieldClass);

            if (Common.isBlank(fields)) {

                    // Add on all the application specific values to the context
                    // This is all the public static fields that are in uppercase

                fields = new HashMap<>();
                List<Field> objFields = new ArrayList<>(Arrays.asList(fieldClass.getDeclaredFields()));
                for (Field objField : objFields) {
                    if (Modifier.isPublic(objField.getModifiers()) && Modifier.isStatic(objField.getModifiers()) && objField.getName().equals(objField.getName().toUpperCase()))
                        try {
                            fields.put(fieldClass.getSimpleName().toUpperCase() + '_' + objField.getName(), objField.get(objField.getClass()));
                        }
                        catch (Exception e) {
                            logger.error("Problem outputting static field values - {}", PivotalException.getErrorMessage(e));
                        }
                }
                constantsCache.put(fieldClass, fields);
            }
        }
        return fields;
    }

    /**
     * Will return an instance of the {@code VelocityEngine} already configured.
     *
     * @return The new {@code VelocityEngine} instance
     */
    public static VelocityEngine createEngine() {
        return makeEngine(false);
    }

    /**
     * Make an new Engine
     *
     * @param initialiseEngine Whether the engine is initialised
     * @return Velocity engine
     */
    private static VelocityEngine makeEngine(boolean initialiseEngine) {

        // Get an engine to use

        logger.debug("Creating a velocity engine");
        VelocityEngine engine = new VelocityEngine();

        // Now we need to find a suitable properties file to use from the classpath
        // This should always find something because even if the file is not present
        // in the file system there is one deep in the bowels of the Velocity jar file

        URL url = VelocityUtils.class.getClassLoader().getResource("velocity.properties");

        // Initialise the engine using the found properties

        if (url == null) {
            logger.debug("Initialising a velocity engine with default properties");
            if (initialiseEngine) engine.init();
        } else {
            String filePath = Common.decodeURL(url.getFile());
            String velocityProperties = new File(filePath).getAbsolutePath();
            InputStream inp = null;
            try {
                inp = new FileInputStream(velocityProperties);

                // Read the properties so that we can override them

                Properties props = new Properties();
                props.load(inp);

                // Identify the engine - seems a bit naff but is needed for the resource cache
                // to identify the engine when it is removed

                props.setProperty(RUNTIME_ENGINE_IDENTIFIER, engine.toString());

                // Override some of them - set the path to the templates/macros

                String path = velocityProperties.split("WEB-INF")[0];
                if (!Common.isBlank(path))
                    props.setProperty("file.resource.loader.path", path + "WEB-INF/templates");

                // Kill off the velocity logger

                props.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogChute");

                // Initialise

                if (initialiseEngine) {
                    logger.debug("Initialising a velocity engine with [{}] properties", velocityProperties);
                    engine.init(props);
                } else {
                    logger.debug("Creating a velocity engine with [{}] properties", velocityProperties);

                    // We still want the properties in the engine

                    for (String key : props.stringPropertyNames()) {
                        String value = props.getProperty(key);
                        engine.addProperty(key, value);
                    }
                }
            }
            catch (FileNotFoundException e) {
                logger.error("Failed to load properties file. Loading default properties. ", e);
                if (initialiseEngine) engine.init();
            }
            catch (IOException e) {
                logger.error("Failed to load properties file. Loading default properties. ", e);
                if (initialiseEngine) engine.init();
            }
            finally {
                Common.close(inp);
            }
        }
        return engine;
    }

}
