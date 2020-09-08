/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

package com.pivotal.web.controllers;

import com.pivotal.system.hibernate.entities.LookupsEntity;
import com.pivotal.system.hibernate.entities.MediaEntity;
import com.pivotal.system.hibernate.entities.SiteEntity;
import com.pivotal.system.hibernate.entities.UserEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.security.CaseManager;
import com.pivotal.system.security.Preferences;
import com.pivotal.system.security.UserManager;
import com.pivotal.utils.*;
import com.pivotal.web.controllers.utils.Authorise;
import com.pivotal.web.controllers.utils.JsonResponse;
import com.pivotal.web.servlet.ServletHelper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.pivotal.system.hibernate.utils.HibernateUtils.serializeRowJSON;
import static com.pivotal.system.security.CaseManager.safeGet;
import static com.pivotal.utils.Common.isBlank;
import static com.pivotal.utils.Common.parseInt;

//import org.zefer.pd4ml.PD4Constants;
//import org.zefer.pd4ml.PD4ML;

/**
 * Handles requests for storing and managing preferences
 * For the most part, all the requests made to this controller will be
 * from an AJAX source and will not expect a return value
 */
@Authorise
@Controller
public abstract class AbstractController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AbstractController.class);
    /** Constant <code>NAMESPACE_ATTRIBUTE="namespace"</code> */
    public static final String NAMESPACE_ATTRIBUTE = "namespace";
    /** Constant <code>ADMIN_PAGE_TEMPLATE="admin"</code> */
    public static final String ADMIN_PAGE_TEMPLATE = "admin";

    static final String HIDDEN_LOCKED_ID = "~|HIDDEN-LOCKED|~";

    /**
     * Saves all the values sent to it in the current users preferences storage
     * This works irrespective of whether the request is a POST or a GET
     * It will return an empty response to all requests because it assumes that it
     * is being called in a "fire and forget" scenario
     *
     * @param request  Request from client
     * @param response Response to send
     */
    @RequestMapping(value = {"/preferences/save", "/preferences/session/save", "/prefs/save", "/prefs/session/save"})
    public void savePreferences(HttpServletRequest request, HttpServletResponse response) {

        // Get the current uer profile

        UserEntity user = UserManager.getCurrentUser();
        if (user != null) {
            Preferences<Object> preferences = user.getPreferences(getNamespace());
            for (Object key : request.getParameterMap().keySet()) {
                if (!Common.doStringsMatch((String) key, "_")) {
                    String value = Common.join(request.getParameterValues((String) key));
                    if (request.getRequestURI().contains("/session"))
                        preferences.getSession().put((String) key, value);
                    else preferences.put((String) key, value);
                }
            }
        }
        ServletHelper.sendError(HttpServletResponse.SC_OK, "Thankyou");
    }

    /**
     * Removes preferences for the current user
     * This works irrespective of whether the request is a POST or a GET
     * It will return an empty response to all requests because it assumes that it
     * is being called in a "fire and forget" scenario
     *
     * @param request  Request from client
     * @param response Response to send
     */
    @RequestMapping(value = {"/preferences/remove", "/preferences/session/remove", "/prefs/remove", "/prefs/session/remove"})
    public void removePreferences(HttpServletRequest request, HttpServletResponse response) {

        // Get the current uer profile

        UserEntity user = UserManager.getCurrentUser();
        if (user != null && !Common.isBlank(request.getParameterMap())) {
            Preferences preferences = user.getPreferences(getNamespace());
            for (Object key : request.getParameterMap().keySet()) {
                if (!Common.doStringsMatch((String) key, "_")) {
                    if (request.getRequestURI().contains("/session")) preferences.getSession().remove(key);
                    else preferences.remove(key);
                }
            }
        }
        ServletHelper.sendError(HttpServletResponse.SC_OK, "Thankyou");
    }

    /**
     * Clears preferences for the current user
     * This works irrespective of whether the request is a POST or a GET
     * It will return an empty response to all requests because it assumes that it
     * is being called in a "fire and forget" scenario
     *
     * @param request  Request from client
     * @param response Response to send
     */
    @RequestMapping(value = {"/preferences/clear", "/preferences/sesion/clear", "/prefs/clear", "/prefs/session/clear"})
    public void clearPreferences(HttpServletRequest request, HttpServletResponse response) {

        // Get the current uer profile

        UserEntity user = UserManager.getCurrentUser();
        if (user != null) {
            if (request.getRequestURI().contains("/session"))
                user.getPreferences(getNamespace()).getSession().clearPage();
            else user.getPreferences(getNamespace()).clearPage();
        }
        ServletHelper.sendError(HttpServletResponse.SC_OK, "Thankyou");
    }

    /**
     * Returns the namespace to use for the preferences
     * This uses reflection to get the capture rule for the controller but it is expected
     * that users will override this method if they want to use a different namespace
     *
     * @return Namespace e.g. admin.user.grid
     */
    public String getNamespace() {
        String tmp = getRequestMapping();
        if (!Common.isBlank(tmp)) {
            return (tmp.startsWith("/") ? tmp.substring(1) : tmp).replace('/', '.');
        }
        else {
            logger.error("No request mapping declared for [{}]", this.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * Returns the namespace to use for the preferences
     * This uses reflection to get the capture rule for the controller but it is expected
     * that users will override this method if they want to use a different namespace
     *
     * @param clazz The class to interrogate
     * @return Namespace e.g. admin.user.grid
     */
    public static String getNamespace(Class clazz) {
        String tmp = getRequestMapping(clazz);
        if (!Common.isBlank(tmp)) {
            return (tmp.startsWith("/") ? tmp.substring(1) : tmp).replace('/', '.');
        }
        else {
            logger.debug("No namespace declared for [{}]", clazz.getSimpleName());
            return null;
        }
    }

    /**
     * Returns the page request mapping for the class
     * This uses reflection to get the capture rule for the controller but it is expected
     * that users will override this method if they want to use a different namespace
     *
     * @return Request mappingN e.g. /admin/user
     */
    public String getRequestMapping() {
        return getRequestMapping(this.getClass());
    }

    /**
     * Returns the page request mapping for the class
     * This uses reflection to get the capture rule for the controller but it is expected
     * that users will override this method if they want to use a different namespace
     *
     * @param clazz The class to interrogate
     * @return Request mappingN e.g. /admin/user
     */
    public static String getRequestMapping(Class<?> clazz) {
        Annotation anno = clazz.getAnnotation(RequestMapping.class);
        if (anno != null) {
            String[] rules = ((RequestMapping) anno).value();
            if (rules != null) return rules[0];
            else {
                logger.error("No request mapping declared for [{}]", clazz.getSimpleName());
                return null;
            }
        }
        else return null;
    }

    /**
     * Will add the value under the current namespace {@link #getRequestMapping()}.
     *
     * @param key   The key to store the property
     * @param value The actual value to save
     * @param <T> a T object.
     */
    protected <T> void putPreference(String key, T value) {

        // Store the value using the key under this class namespace

        putPreference(key, value, this.getClass());
    }

    /**
     * Will return the value under the current namespace {@link #getRequestMapping()}.
     *
     * @param key The key for the property key
     * @param <T> a T object.
     * @return a T object.
     */
    @SuppressWarnings("unchecked")
    protected <T> T getPreference(String key) {

        // Get the preference using this class namespace

        return getPreference(key, this.getClass());
    }

    /**
     * Will add the value under the current namespace of the class provided.
     *
     * @param key   The key to store the property
     * @param value The actual value to save
     * @param clazz The class to interrogate
     * @param <T> a T object.
     */
    protected <T> void putPreference(String key, T value, Class<?> clazz) {

        // Get the current user preferences

        final Preferences<T> preferences = getPreferences(clazz);
        if (preferences != null) {
            preferences.put(key, value);
        }
    }

    /**
     * Will return the value under the current namespace of the class provided.
     *
     * @param key   The key for the property key
     * @param clazz The class to interrogate
     * @param <T> a T object.
     * @return a T object.
     */
    @SuppressWarnings("unchecked")
    protected <T> T getPreference(String key, Class<?> clazz) {

        // Get the current user preferences

        final Preferences<T> preferences = getPreferences(clazz);
        if (preferences != null) {
            return preferences.get(key);
        }
        return null;
    }

    /**
     * Returns the preferences for the current controller namespace {@link #getNamespace()}
     *
     * @param <T> The value type of the preferences
     * @return The current preferences object
     */
    protected <T> Preferences<T> getPreferences() {
        return getPreferences(this.getClass());
    }

    /**
     * Returns the preferences for the namespace of the class {@link #getNamespace(Class)}
     *
     * @param <T> The value type of the preferences
     * @return The current preferences object
     * @param clazz a {@link java.lang.Class} object.
     */
    protected <T> Preferences<T> getPreferences(Class<?> clazz) {
        UserEntity user = UserManager.getCurrentUser();
        if (user != null) {
            return user.getPreferences(getNamespace(clazz));
        }
        return null;
    }

    /**
     * Send back an HTTP error response without worrying about throwing an error
     *
     * @param scInternalServerError Error Code
     * @param errorMessage          Error message
     */
    public void sendError(int scInternalServerError, String errorMessage) {
        ServletHelper.sendError(scInternalServerError, errorMessage);
    }

    /**
     * Creates a list of the entities suitable for displaying in a dropdown from select
     *
     * @param clazz Class to get the entities for
     * @return List of mapped ids to names
     */
    public static List<Map<String,String>> getSelectDisplayList(Class clazz) {
        if (clazz==null)
            return null;
        else
            return getSelectDisplayList(clazz.getSimpleName(), null, "");
    }

    /**
     * Creates a list of the entities suitable for displaying in a dropdown from select
     *
     * @param className Class to get the entities for
     * @return List of mapped ids to names
     */
    public static List<Map<String,String>> getSelectDisplayList(String className) {
        return getSelectDisplayList(className, null, "");
    }


    /**
     * Creates a list of the entities suitable for displaying in a dropdown from select
     *
     * @param className Class to get the entities for
     * @param basket Comma separated list of IDs to limit the list to
     * @param extraFilter Extra filter to apply in the entity selection
     * @return List of mapped ids to names
     */
    public static List<Map<String,String>> getSelectDisplayList(String className, String basket, String extraFilter) {
        Map<String, Object>params=new HashMap<>();

        params.put("ExtraFilter", extraFilter);

        return getSelectDisplayList(className, basket, params);
    }

    /**
     * Creates a list of the entities suitable for displaying in a dropdown from select
     *
     * @param className Class to get the entities for
     * @param basket Comma separated list of IDs to limit the list to
     * @param params Map of possible options to be used to get the data
     *
     * @return List of mapped ids to names
     */
    public static List<Map<String,String>> getSelectDisplayList(String className, String basket, Map<String, Object>params) {

        List<Map<String,String>> returnValue = new ArrayList<>();
        if (!Common.isBlank(className)) {
            List<Object[]> objects;
            try {

                String extraFilter = CaseManager.safeGet(params, "ExtraFilter");
                String sortClause = CaseManager.safeGet(params, "Sort", "");
                String joinClause = CaseManager.safeGet(params, "Join","");
                boolean showEmptyItem = Common.isYes(CaseManager.safeGet(params, "ShowEmptyItem"));

//                if (showEmptyItem)
//                    returnValue.add(Common.getMapFromPairs("text", "", "value", ""));

                // Figure out if there is a description property

                Class clazz = Class.forName(UserEntity.class.getPackage().getName() + "." + className);
                boolean hasDescription = ClassUtils.propertyExists(clazz, "description");
                String additionalDescription = hasDescription?", searchEntity.description":"";

                // Make sure there is a name field, if not try title

                String displayListModifier = CaseManager.safeGet(params,"displaylistmodifier");
                List<String> nameField = getDisplayListFields(clazz, displayListModifier);

                // Build text string

                String textSearch = "";
                for (int fieldCount=0; fieldCount<nameField.size(); fieldCount++) {
                    if (!Common.isBlank(nameField.get(fieldCount)))
                        textSearch += (Common.isBlank(textSearch)?"":", ") + nameField.get(fieldCount) + " as text_" + (fieldCount + 1);
                }

                String searchEntities = className + " as searchEntity";

                // See if we need to do a left join rather than an inner join (which may exclude some records)
                if (clazz == SiteEntity.class) {
                    searchEntities += " left join searchEntity.caseStage cs";
                }
                else if (clazz == com.pivotal.system.hibernate.entities.UserEntity.class) {
                    searchEntities += " left join searchEntity.company co";
                }

                // Do we have a basket to limit the search to

                String query;
                if (Common.isBlank(basket) && Common.isBlank(extraFilter))
                    query = String.format("select %s, searchEntity.id as value%s from %s %s", textSearch, additionalDescription, searchEntities, joinClause);

                else if (!Common.isBlank(basket) && Common.isBlank(extraFilter)){
                    String Ids = (Common.isBlank(basket) ? "" : ",") + basket;
                    query = String.format("select %s, searchEntity.id as value%s from %s %s where searchEntity.id in (-1%s)", textSearch, additionalDescription, searchEntities, joinClause, Ids);
                }

                else if (Common.isBlank(basket) && !Common.isBlank(extraFilter)){
                    query = String.format("select %s, searchEntity.id as value%s from %s %s where %s", textSearch, additionalDescription, searchEntities, joinClause, extraFilter);
                }

                else {
                    String Ids = (Common.isBlank(basket) ? "" : ",") + basket;
                    query = String.format("select %s, searchEntity.id as value%s from %s %s where searchEntity.id in (-1%s) and %s", textSearch, additionalDescription, searchEntities, joinClause, Ids, extraFilter);
                }

                if (!Common.isBlank(sortClause))
                    query += " order by " + sortClause;

                objects = HibernateUtils.selectEntities(query);

                // Create a return list of map values

                if (!Common.isBlank(objects)) {
                    String description;
                    String key;
                    String textValue;
                    int textExtent;
                    for (Object[] object : objects) {

                        // Get components of return list

                        if (hasDescription && object.length > 2) {
                            description = Common.isBlank(object[object.length - 1])?"":String.valueOf(object[object.length - 1]);
                            key = String.valueOf(object[object.length - 2]);
                            textExtent = object.length - 3;
                        }
                        else {
                            description = "";
                            key = String.valueOf(object[object.length - 1]);
                            textExtent = object.length - 2;
                        }

                        // Build up list of fields to be displayed as text

                        textValue = "";
                        Object thisValue;
                        String testValue;
                        for(int index=0;index<=textExtent;index++) {
                            thisValue = object[index];
                            if (!Common.isBlank(thisValue)) {
                                if (object[index] instanceof java.sql.Timestamp)
                                    textValue += Common.dateFormat((java.sql.Timestamp) (thisValue), "yyyy-MMM-dd HH:mm");
                                else {
                                    testValue = I18n.translate(String.valueOf(thisValue), true);
                                    if (testValue != null)
                                        textValue += testValue;
                                    else
                                        textValue += thisValue;
                                }
                            }
                        }

                        if (Common.isBlank(description))
                            returnValue.add(Common.getMapFromPairs("text", textValue, "value", key));
                        else
                            returnValue.add(Common.getMapFromPairs("text", textValue, "value", key, "description", description));

                    }

                    if (Common.isBlank(sortClause))
                        returnValue = Common.sortList(returnValue, "text");
                }
            }
            catch (Exception e) {
                logger.debug("The class [{}] doesn't exit", className);
            }
        }
        return returnValue;
    }

    /**
     * Creates a list of the entities suitable for displaying in a dropdown from select
     *
     * @param className Class to get the entities for
     * @param basket Comma separated list of IDs to limit the list to
     * @return List of mapped ids to names as a JSON string
     */
    public static String getSelectDisplayListJson(String className, String basket) {
        return JsonMapper.serializeItem(getSelectDisplayList(className, basket, ""));
    }


    /**
     * Creates a list of the entities suitable for displaying in a dropdown from select
     *
     * @param className Class to get the entities for
     * @param basket Comma separated list of IDs to limit the list to
     * @param extraFilter Extra filter to apply in the entity selection
     * @return List of mapped ids to names as a JSON string
     */
    public static String getSelectDisplayListJson(String className, String basket, String extraFilter) {
        return JsonMapper.serializeItem(getSelectDisplayList(className, basket, extraFilter));
    }

    /**
     * Creates a list of the entities suitable for displaying in a dropdown from select
     *
     * @param className Class to get the entities for
     * @param basket Comma separated list of IDs to limit the list to
     * @param params Map of possible options to be used to get the data
     * @return List of mapped ids to names as a JSON string
     */
    public static String getSelectDisplayListJson(String className, String basket, Map<String, Object>params) {
        return JsonMapper.serializeItem(getSelectDisplayList(className, basket, params));
    }

    /**
     * Define list of fields to display when selecting entities
     *
     * @param clazz class being displayed
     * @param displayListModifier context identifier to allow specific displays for the same type
     *
     * @return List of field values to display in the drop down
     */
    public static List<String>getDisplayListFields(Class clazz, String displayListModifier) {

        List<String>fields = new ArrayList<>();

        if (clazz == SiteEntity.class && Common.isBlank(displayListModifier)) {
            fields.add("'['");
            fields.add("s.name");
            fields.add("'] '");
            fields.add("searchEntity.name");
        }
        else if (clazz == com.pivotal.system.hibernate.entities.UserEntity.class && Common.isBlank(displayListModifier)) {
            fields.add("searchEntity.name");
            fields.add("' ['");
            fields.add("co.name");
            fields.add("']'");
        }
        else {
            if (ClassUtils.propertyExists(clazz, "name"))
                fields.add("searchEntity.name");
            else if (ClassUtils.propertyExists(clazz, "title"))
                fields.add("searchEntity.title");
        }
        return fields;
    }

    /**
     * Generate the template name used by the autosave
     *
     * @return String to identify this template
     */
    public static String getAutoSaveTemplate() {
        String returnValue = ServletHelper.getRequestInfo().getPageName();
        if (!Common.isBlank(ServletHelper.getRequestInfo().getActionPageName()))
            returnValue += "_" + ServletHelper.getRequestInfo().getActionPageName();

        return returnValue;
    }

    /**
     * Manages the sending of data to select/drop down form controls
     *
     * @param entityName - Class Name of entity to get
     * @param params     - parameter map
     *
     * @return JSON with data
     */
    @RequestMapping(value = "/display/{EntityName}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    @ResponseBody
    public String getDisplay(@PathVariable(value = "EntityName") String entityName,
                             @RequestParam Map<String, Object> params) {
        // Gets default select display data
        return getSelectDisplayListJson(entityName, CaseManager.safeGet(params, "basket"), params);
    }

    /**
     * Returns a value, settings is an array of values
     *      pos 1 = id
     *      pos 2 = tableName
     *      pos 3 = fieldName
     *
     *      pos 2 and 3 can be repeated with the returnValue fed back in as id
     *      to allow searches for linked records.
     *
     * @param settings         - settings to be used to get value
     *
     * @return JSON with data
     */
    @RequestMapping(value = "/get", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    @ResponseBody
    public String getFieldValue(@RequestParam (value = "settings") String settings) {

        String returnValue = "";

        List<String> values = Common.splitToList(settings,",");

        if (values.size() > 2) {
            int index = 0;
            String fieldName;
            String tableName;
            String id = "";
            List<String> result;
            while (index < values.size()) {

                if (index == 0) {
                    id = values.get(0);
                    tableName = values.get(1);
                    fieldName = values.get(2);
                    index = 3;
                }
                else {
                    id = returnValue;
                    tableName = values.get(index);
                    fieldName = values.get(index + 1);
                    index += 2;
                }

                // If we have id then get value

                returnValue = "";
                if (!Common.isBlank(id)) {
                    result = HibernateUtils.selectSQLEntities(String.format("select %s from %s where id = %s", fieldName, tableName, id));
                    if (result != null && result.size() > 0)
                        returnValue = String.valueOf(result.get(0));
                    else {
                        returnValue  = "";
                        id = "";
                    }
                }

                // If we have no id then short circuit the loop

                if (Common.isBlank(id))
                    index = values.size();
            }
        }

        return returnValue;
    }

    /**
     * Returns the field value
     *
     * @param id         - id of entity
     *
     * @return JSON with data
     */
    @RequestMapping(value = "/get/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    @ResponseBody
    public String getRecord(@PathVariable(value = "id") String id) {

        // Correct Table name
        String tableName = getNamespace();
        if (tableName.equalsIgnoreCase("case"))
            tableName = "cases";
        else if(tableName.startsWith("admin."))
            tableName = tableName.substring(6);

        return serializeRowJSON(id, tableName);
    }

    /**
     * Uploads files to temporary files and returns details for later saving
     *
     * @param files files to upload
     * @return Details of uploaded files
     */
    @RequestMapping(value = {"/upload"}, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public JsonResponse uploadFiles(@RequestParam(value = "files", required = false) List<MultipartFile> files) {

        JsonResponse returnValue = new JsonResponse();

        String output = "";
        try {
            Map<String, Object> thisFileDetails;
            for (MultipartFile thisFile : files) {
                // Save file to temp folder
                File tempFile = Common.getTemporaryFile();
                Common.pipeInputToOutputStream(thisFile.getInputStream(), tempFile);

                thisFileDetails = new HashMap<>();
                thisFileDetails.put("Size", thisFile.getSize());
                thisFileDetails.put("OriginalFilename", thisFile.getOriginalFilename());
                thisFileDetails.put("TempFilename", tempFile.getAbsolutePath());
                output += (Common.isBlank(output)?"":",") + JsonMapper.serializeItem(thisFileDetails);

                Integer retentionTime = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_SESSION_UPLOAD_FILE_RETENTION, HibernateUtils.SETTING_SESSION_UPLOAD_FILE_RETENTION_DEFAULT);
                Common.addFileForDeletion(tempFile, retentionTime * 60);
            }
            returnValue.putDataItem("fileDetail", output);
        }
        catch(Exception e) {
            returnValue.clearDataEntry();
            returnValue.setError(PivotalException.getErrorMessage(e));
        }

        return returnValue;
    }

    /**
     * Uploads files to temporary files and returns details for later saving
     *
     * @param params files to remove
     * @return Json containing errors if any
     */
    @RequestMapping(value = {"/removeupload"}, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public JsonResponse removeFiles(@RequestParam Map<String, Object> params) {

        JsonResponse returnValue = new JsonResponse();

        if (params.containsKey("fileList")) {

            List<String>files = Common.splitToList(params.get("fileList").toString(), ",");

            if (!Common.isBlank(files)) {
                List<Object> delObjects = new ArrayList<>();
                try {
                    for (String file : files) {
                        String entityName;
                        Integer entityId;
                        if (file.contains(":")) {
                            entityName = Common.getItem(file, ":", 0);
                            entityId = Common.parseInt(Common.getItem(file, ":", 1));
                        }
                        else {
                            entityName = "MediaEntity";
                            entityId = Common.parseInt(file);
                        }
                        Object entity = null;
                        if (!isBlank(entityId) && entityId > 0) {
                            logger.debug("Deleting {} - {}", entityName, entityId);
                            entity = HibernateUtils.getEntity(entityName, entityId);
                            if (entity != null)
                                delObjects.add(entity);
                        }
                        // See if we need to or can get media

                        if (!(entity instanceof MediaEntity)) {
                            MediaEntity mediaEntity = ClassUtils.invokeMethod(entity, "getMedia");
                            if (mediaEntity != null && mediaEntity.getId() != null) {
                                delObjects.add(mediaEntity);
                                logger.debug("Deleting MediaEntity - {}", entityId);
                            }
                        }
                    }

                    // Do delete
                    if (delObjects.size() > 0) {
                        HibernateUtils.delete(delObjects);
                        HibernateUtils.commit();
                    }
                }
                catch (Exception e) {
                    returnValue.setError(PivotalException.getErrorMessage(e));
                }
            }
        }
        return returnValue;
    }
    /**
     * Returns true if the item should be updated and isn't a field the user doesn't have access to
     *
     * @param params    Map of parameters
     * @param valueName Key of value to check
     * @return true if ok to update
     */
    boolean updateValue(Map<String, Object> params, String valueName) {
        return updateValue(params, valueName, false);
    }

    /**
     * Returns true if the item should be updated and isn't a field the user doesn't have access to
     *
     * @param params    Map of parameters
     * @param valueName Key of value to check
     * @return true if ok to update
     */
    boolean updateValue(Map<String, Object> params, String valueName, boolean nullAllowed) {

        // Return true if it is in params and it's not equal to the hidden locked token

        boolean returnValue;

        if (nullAllowed)
            returnValue = params.containsKey(valueName) && !HIDDEN_LOCKED_ID.equals(params.get(valueName));
        else
            returnValue = params.containsKey(valueName) && !isBlank(params.get(valueName)) && !HIDDEN_LOCKED_ID.equals(params.get(valueName));

        if (!returnValue && !valueName.startsWith("_"))
            returnValue = updateValue(params, "_" + valueName, nullAllowed);

        return returnValue;
    }

    /**
     * If selectedValue exists then it uses that, otherwise it
     * creates a new one and then uses that
     *
     * @param selectedValue Value returned from the form
     * @param params        Form paramerters
     * @param fieldName     Field name
     *
     * @return LookupsEntity or null
     */
    LookupsEntity getLookup(LookupsEntity selectedValue, Map<String,Object>params, String fieldName, String lookupType) {

        if (selectedValue != null)
            return selectedValue;

        else if(!isBlank(params)) {

            String value = safeGet(params, fieldName);
            String valueInput = safeGet(params, fieldName + "_input");

            if (!isBlank(valueInput)) {
                LookupsEntity lookupsEntity = HibernateUtils.getEntity(LookupsEntity.class);
                lookupsEntity.setName(valueInput);
                lookupsEntity.setDescription(valueInput);
                lookupsEntity.setType(lookupType);

                if (HibernateUtils.save(lookupsEntity))
                    return lookupsEntity;
                else
                    logger.error("Unable to create new lookup {}", valueInput);
            }
        }
        return null;
    }
    /**
     * If Value exists then it uses that, otherwise it
     * creates a new one and then uses that
     *
     * @param params        Form parameters
     * @param fieldName     Field name
     *
     * @return LookupsEntity or null
     */
    LookupsEntity getLookup(Map<String,Object>params, String fieldName, String lookupType) {

        if(!isBlank(params) && !isBlank(fieldName) && params.containsKey(fieldName) && !isBlank(lookupType)) {

            String value = safeGet(params, fieldName);
            LookupsEntity lookupsEntity = HibernateUtils.getEntity(LookupsEntity.class, parseInt(value));

            if (lookupsEntity != null && lookupsEntity.getId() != null) {
                return lookupsEntity;
            }
            else {
                String valueInput = safeGet(params, fieldName + "_input");
                if (!isBlank(valueInput)) {
                    lookupsEntity = HibernateUtils.getEntity(LookupsEntity.class);
                    lookupsEntity.setName(valueInput);
                    lookupsEntity.setDescription(valueInput);
                    lookupsEntity.setType(lookupType);
                }

                if (HibernateUtils.save(lookupsEntity))
                    return lookupsEntity;
                else
                    logger.error("Unable to create new lookup {}", valueInput);
            }
        }
        return null;
    }
}
