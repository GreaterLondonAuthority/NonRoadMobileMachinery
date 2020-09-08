/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

package com.pivotal.web.controllers;

import com.pivotal.monitoring.utils.Definition;
import com.pivotal.monitoring.utils.DefinitionSettings;
import com.pivotal.monitoring.utils.Parameter;
import com.pivotal.nrmm.service.notification.Notification;
import com.pivotal.reporting.reports.sqldump.TextOutput;
import com.pivotal.system.data.DataChangeEvent;
import com.pivotal.system.hibernate.entities.AutoSaveEntity;
import com.pivotal.system.hibernate.entities.ChangeLogEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.security.UserManager;
import com.pivotal.utils.*;
import com.pivotal.web.controllers.utils.GridFieldList;
import com.pivotal.web.controllers.utils.GridResults;
import com.pivotal.web.controllers.utils.JsonResponse;
import com.pivotal.web.notifications.NotificationManager;
import com.pivotal.web.servlet.ServletHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.context.Context;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.support.DefaultDataBinderFactory;
import org.springframework.web.bind.support.WebRequestDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.pivotal.utils.Common.isBlank;


/**
 * This class is a very useful wrapper for Admin controllers that handles
 * most of the list and edit screen functionality
 * It will use the request mapping of the controller to determine the
 * namespace to use for defaults and personal preferences
 */
@Controller
public abstract class AbstractAdminController extends AbstractController implements ApplicationEventPublisherAware, HandlerExceptionResolver {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AbstractAdminController.class);
    /**
     * Constant <code>ATTRIBUTE_PINNED="pinned"</code>
     */
    public static final String ATTRIBUTE_PINNED = "pinned";
    public static final String CLEAR_AFTER_UPDATE = "clearafterupdate";

    public static final String NEXT_ENTITY = "next";
    public static final String PREVIOUS_ENTITY = "previous";
    /**
     * Constant <code>EDIT_STATE="EditState"</code>
     */
    public static final String EDIT_STATE = "EditState";

    // The publisher for any events
    private ApplicationEventPublisher publisher;


    /**
     * Set the publisher for any events we define. This uses Spring Application Event mechanism
     *
     * @param publisher the event publisher
     */
    public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Autowired
    private ConversionService conversionService;

    /**
     * Convert a Calendar unit (e.g. <code>Calendar.SECOND</code>, <code>Calendar.MINUTE</code>, etc...) into a string
     * (e.g. seconds, minutes), looking it up in the I18n class.
     *
     * @param unit   Calendar unit.
     * @param plural Should the return value be pluralised ("seconds" instead of "second")?
     * @return The string
     */
    public static String getDateUnit(int unit, boolean plural) {

        String key;

        if (unit == Calendar.MILLISECOND) {
            key = "date.units.millisecond";
        }
        else if (unit == Calendar.SECOND) {
            key = "date.units.second";
        }
        else if (unit == Calendar.MINUTE) {
            key = "date.units.minute";
        }
        else if (unit == Calendar.HOUR || unit == Calendar.HOUR_OF_DAY) {
            key = "date.units.hour";
        }
        else if (unit == Calendar.DATE) {
            key = "date.units.day";
        }
        else {
            return "";
        }

        if (plural) {
            key += "s";
        }

        return I18n.getString(key);
    }

    /**
     * <p>Gets a formatted {@link java.lang.String} date from a given {@link java.sql.Timestamp} and a date format.</p>
     * <p>If timestamp is null, the current timestamp will be used. If the format is null, "dd-MM-yyy" will be used.</p>
     *
     * @param timestamp the given {@link java.sql.Timestamp}
     * @param format    the {@link java.lang.String} date format
     * @return {@link java.lang.String} representing the date
     */
    public static String getDateAsString(Timestamp timestamp, String format) {

        if (Common.isBlank(format)) {
            format = "dd-MM-yyyy";
        }

        if (Common.isBlank(timestamp)) {
            timestamp = new Timestamp(System.currentTimeMillis());
        }

        DateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(new Date(timestamp.getTime()));
    }

    /**
     * Return a nicely formatted of how long ago a given date is, in days, hours, minutes and seconds.
     *
     * @param startDate First date to compare. Must be in the past.
     * @param units     Maximum number of units to display - i.e. how much detail to show. Set to 0 to show all units.
     * @return The string
     */
    public static String diffDateWords(Date startDate, int units) {
        return diffDateWords(startDate, new Date(), units);
    }

    /**
     * Return a nicely formatted string of the difference between two dates, in days, hours, minutes and seconds.
     *
     * @param startDate First date to compare. Must be before endDate.
     * @param endDate   Second date to compare. Must be after startDate.
     * @param units     Maximum number of units to display - i.e. how much detail to show. Set to 0 to show all units.
     * @return The string
     */
    public static String diffDateWords(Date startDate, Date endDate, int units) {

        StringBuilder result = new StringBuilder();

        // The different date units to consider.
        List<Integer> unitTypes = new ArrayList<>();
        unitTypes.add(Calendar.DATE);
        unitTypes.add(Calendar.HOUR);
        unitTypes.add(Calendar.MINUTE);
        unitTypes.add(Calendar.SECOND);


        boolean firstEntry = true;
        int unitsShown = 0;
        long diff;

        for (int unitType : unitTypes) {
            if ((diff = Common.diffDate(startDate, endDate, unitType)) > 0) {

                // If it's not the first word, separate it with a comma and a space.
                if (!firstEntry) {
                    result.append(", ");
                }
                else {
                    firstEntry = false;
                }

                // Find the right word to use for this date unit and append it to the string.
                result.append(String.format("%d %s", diff, getDateUnit(unitType, diff != 1)));

                // And subtract what we've just put on the string from the endDate;
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(endDate);
                calendar.add(unitType, (int)-diff);
                endDate = calendar.getTime();


            }

            // Don't output more units than we're asked for.
            if (!firstEntry) {
                unitsShown++;
            }
            if (units > 0 && unitsShown >= units) {
                break;
            }
        }

        // If we haven't put out any text, it was less than a second.
        if (unitsShown == 0) {
            return I18n.getString("date.less_than_a_second");
        }
        else {

            return result.toString();
        }
    }

    /**
     * List of the edit states that a record can be in
     */
    public enum EditStates {
        VIEWING, ADDING, COPYING, EDITING, DELETING;

        /**
         * Returns true if the state is any of the ones supplied
         *
         * @param types Array of types to check
         * @return Trye if any of the types match
         */
        public boolean is(String... types) {
            boolean returnValue = false;
            if (!Common.isBlank(types)) {
                for (int i = 0; i < types.length && !returnValue; i++) {
                    returnValue = Common.doStringsMatch(types[i], this.name());
                }
            }
            return returnValue;
        }
    }

    /**
     * Manages the sending of data to the grid
     * Takes care of any sorting and filtering that might be in place
     *
     * @return Grid results object
     */
    @RequestMapping(value = "/grid", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public GridResults getGridData() {

        // Get a list of the components we need from the query string etc.

        GridResults gridResults = null;
        try {
            gridResults = new GridResults(getEntityClass(), UserManager.getCurrentUser().getPreferences(getNamespace()), ServletHelper.getParameter("extrafilter"));
            gridResults.executeQuery();
            gridResults.savePreferences();
        }
        catch (Exception e) {
            //TODO : Why do we keep getting error - data integrity
            logger.error("Problem getting grid results - {}", PivotalException.getErrorMessage(e));
        }
        return gridResults;
    }

    /**
     * Shows the grid for the specified entity and namespace
     *
     * @param session Session to store temporary entities
     * @param request Request causing this action
     * @param model   Model add attributes to
     * @return The template to use
     */
    @RequestMapping(value = {"", "/", "/default"}, method = RequestMethod.GET)
    public String showGrid(HttpSession session, HttpServletRequest request, Model model) {

        // Get a list of the components we need from the query string etc.

        try {
            String namespace = getNamespace();
            logger.debug("Namespace = {}, {}", namespace, request.getQueryString());
            GridResults config = new GridResults(getEntityClass(), UserManager.getCurrentUser().getPreferences(namespace));
            config.savePreferences();
            model.addAttribute("Prefs", UserManager.getCurrentUser().getPreferences(getNamespace()));
            model.addAttribute("Config", config);

            Table table = getEntityClass().getAnnotation(Table.class);
            String tableName = table != null ? table.name() : "";

            model.addAttribute("EntityTableName", tableName);
        }
        catch (Exception e) {
            logger.error("Problem showing grid - {}", PivotalException.getErrorMessage(e));
        }
        return getRootTemplate();
    }

    /**
     * Shows the grid for the specified entity and namespace
     *
     * @param response Response for this action
     */
    /**
     *
     * @param request Request of this action
     * @param response Response for this action
     * @param name Name of file to output
     */
    @RequestMapping(value = "/download",  method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE)
    public void downloadGridCSV(HttpServletRequest request, HttpServletResponse response
                                  ,@RequestParam(value = "name") String name) {

        String extension = "csv";

        // Get a list of the components we need from the query string etc.
        name = Common.cleanUserData(name);
        try {
            String namespace = getNamespace();
            logger.error("Namespace = {}, {}", namespace, request.getQueryString());
            GridResults config = new GridResults(getEntityClass(), UserManager.getCurrentUser().getPreferences(namespace));

            String filename = Common.getTemporaryFilename(extension);
            String extraFilter = ServletHelper.getParameter("extraFilter");
            String nameToUse = name;
            if ("lookups".equalsIgnoreCase(name) && !isBlank(extraFilter)) {
                // table name is lookups the look to see if extra filter has a type value
                // and use that as the file name
                // eg type='euStage' or type=euStage and ...
                String dataType = extraFilter.replaceAll(".*type=[']?","").replaceAll("[^a-zA-Z].*","");
                if (!isBlank(dataType)) nameToUse = dataType;
            }
            nameToUse += "_export." + extension;
            config.executeQuery(extraFilter);
            List<Map<String, Object>> results = config.getData();
            if (results != null && !isBlank(results)) {
                TextOutput textOutput = new TextOutput(filename, null, false);
                // Build list of all field names
                // this is used to ensure all fields have a value
                List<String>fieldNames = new ArrayList<>();
                for(GridFieldList.FieldDescription fieldDescription : config.getFieldList().getFieldList())
                    fieldNames.add(fieldDescription.getName().replaceAll("\\.", "_"));

                for(Map<String, Object> row : results) {

                    Map<String, Object>newRow = new LinkedHashMap<>();

                    // Rather than put all the results into the output we need to
                    // ensure each row has all the selected fields
                    for(String fieldName : fieldNames)
                        if (row.containsKey(fieldName))
                            newRow.put(fieldName, row.get(fieldName));
                        else
                            newRow.put(fieldName, "");

                    // Output to file
                    textOutput.addRow(newRow);
                }

                textOutput.close();
            }

            try {
                File result = new File(filename);
                if (result.exists()) {
                    response.setContentType(ServletHelper.getServletContext().getMimeType("test." + extension));
                    response.setHeader("Content-Disposition", "attachment; filename=\"" + nameToUse + '"');
                    response.setHeader("Content-Description", nameToUse);
                    logger.debug("Sending file output " + nameToUse);
                    Common.pipeInputToOutputStream(result, response.getOutputStream());
                }
                else
                    logger.debug("Unable to create file when exporting advanced search results");
            }
            catch (Exception e) {
                logger.error("Problem showing grid - {}", PivotalException.getErrorMessage(e));
            }
        }
        catch (Exception e) {
            logger.error("Problem showing grid - {}", PivotalException.getErrorMessage(e));
        }
    }

    /**
     * Catch all for saving settings sent from the grid
     *
     * @return A thankyou!
     */
    @RequestMapping(value = "/settings", method = RequestMethod.GET)
    @ResponseBody
    public String saveGridSettings() {

        // Get a list of the components we need from the query string etc.

        try {
            GridResults props = new GridResults(getEntityClass(), UserManager.getCurrentUser().getPreferences(getNamespace()));
            props.savePreferences();
        }
        catch (Exception e) {
            logger.error("Problem getting grid settings - {}", PivotalException.getErrorMessage(e));
        }
        return "thanks";
    }

    /**
     * Returns the entity to use as the source of the list data
     *
     * @return Entity e.g. UserEntity
     */
    public abstract Class<?> getEntityClass();

    /**
     * Provides the template to use to service the request
     *
     * @return Defaults to the root page but can be overridden
     */
    public String getRootTemplate() {
        return ServletHelper.getRequestInfo().getPageName();
    }

    /**
     * Removes the specified element from the system
     * Expects that this is called via an AJAX JSON call
     *
     * @param model Model to use
     * @param id ID of the user to delete
     * @return a {@link com.pivotal.web.controllers.utils.JsonResponse} object.
     */
    @RequestMapping(value = "/delete", method = RequestMethod.GET)
    @ResponseBody
    public JsonResponse delete(Model model, @RequestParam("id") Integer id) {
        JsonResponse returnValue = new JsonResponse();
        try {
            Object entity = HibernateUtils.getEntity(getEntityClass(), id);
            if (entity == null) returnValue.setWarning(I18n.getString("system.error.entity_not_exists"));

                // Check with the owner

            else if (beforeDelete(entity, id, returnValue)) {
                String changes = HibernateUtils.serializeEntity(entity);
                HibernateUtils.delete(entity);

                // Call the override method

                afterDelete(entity, id);

                // Add a change log entry

                HibernateUtils.addChangeLog(entity, changes, ChangeLogEntity.ChangeTypes.DELETED, model);

                publisher.publishEvent(new DataChangeEvent("delete", entity));

                if (!Common.isBlank(ServletHelper.getErrorsAsString(model)))
                    returnValue.setError(I18n.getString("system.error.exception", id, ServletHelper.getErrorsAsString(model)));
            }
        }
        catch (Exception e) {
            returnValue.setError(I18n.getString("system.error.exception", id, PivotalException.getErrorMessage(e)));
            logger.error(returnValue.getError());
        }

        // Create notification and allow the caller to add to it

        NotificationMessage message;
        if (returnValue.getInError())
            message = new NotificationMessage(I18n.getString("system.error.entity_not_deleted") + '\n' + returnValue.getError(), Notification.NotificationLevel.Error, EditStates.DELETING, false);
        else if (!Common.isBlank(returnValue.getWarning()))
            message = new NotificationMessage(I18n.getString("system.warning.entity_not_deleted") + '\n' + returnValue.getError(), Notification.NotificationLevel.Warning, EditStates.DELETING, false);
        else
            message = new NotificationMessage(I18n.getString("system.success.entity_deleted"), Notification.NotificationLevel.Info, EditStates.DELETING, true);
        addNotificationMessage(message);
        message.activateNotification();

        return returnValue;
    }

    /**
     * Gets the user from the database
     *
     * @param model   Model to send data back to
     * @param id      Optional ID of the role to edit
     * @param session a {@link javax.servlet.http.HttpSession} object.
     * @return a {@link java.lang.String} object.
     */
    @RequestMapping(value = "/edit", method = RequestMethod.GET)
    public String edit(HttpSession session, Model model, @RequestParam(value = "id", required = false) Integer id) {

        // Where do we get the entity from - try the session first if this is an add

        Object entity;
        if (ServletHelper.parameterExists(AutoSaveEntity.RESTORE_PARAMETER)) {
            entity = AutoSaveController.restoreAutoSave(ServletHelper.getParameter(AutoSaveEntity.RESTORE_PARAMETER), id, getEntityClass());
        }
        else if (id == null) {
            entity = HibernateUtils.getEntity(getEntityClass());
        }
        else {
            entity = HibernateUtils.getEntity(getEntityClass(), id);
        }

        // If this is a request for a copy, then remove the entity from the persistence
        // and reset it's ID and clear all the unique values

        if (ServletHelper.parameterExists("copy")) {
            HibernateUtils.evict(entity);
            id = null;

            // Set all the unique columns to null

            BeanWrapperImpl beanWrapper = new BeanWrapperImpl(entity.getClass());
            for (PropertyDescriptor prop : beanWrapper.getPropertyDescriptors()) {
                if (prop.getReadMethod() != null) {
                    Column column = prop.getReadMethod().getAnnotation(Column.class);
                    if (column != null && (column.unique() || prop.getReadMethod().getAnnotation(Id.class) != null)) {
                        ClassUtils.setPropertyValue(entity, prop.getName(), null);
                    }
                }
            }
        }

        // Populate the model with all the required display attributes

        populateModel(model, id, entity);
        return getRootTemplate();
    }

    /**
     * Adds all the entity related attributes to the model
     *
     * @param model  Model to populate
     * @param id     ID of the entity being edited
     * @param entity Entity being edited
     */
    private void populateModel(Model model, Integer id, Object entity) {
        model.addAttribute(EDIT_STATE, getEditState(id));
        model.addAttribute(getEntityClass().getSimpleName(), entity);
        model.addAttribute(getEntityClass().getSimpleName().toLowerCase(), entity);
        model.addAttribute("Entity", entity);
        model.addAttribute(ATTRIBUTE_PINNED, ServletHelper.parameterExists(ATTRIBUTE_PINNED));
        model.addAttribute("NextEntity", ServletHelper.getParameter(NEXT_ENTITY));
        model.addAttribute("PreviousEntity", ServletHelper.getParameter(PREVIOUS_ENTITY));
        // Add any controller specific attributes to the model

        addAttributesToModel(model, entity, id, getEditState(id));
    }

    /**
     * Returns the edit state based on the presence of the actual parameters
     * set to us
     *
     * @param id ID of the entity being edited
     * @return Edit state
     */
    private EditStates getEditState(Integer id) {
        EditStates editState;
        if (ServletHelper.parameterExists("copy")) editState = EditStates.COPYING;
        else if (ServletHelper.parameterExists("view")) editState = EditStates.VIEWING;
        else if (id == null) editState = EditStates.ADDING;
        else editState = EditStates.EDITING;
        return editState;
    }

    /**
     * Handles the saving of user data into the database
     * Returns error information through the usual channels i.e. the binding
     * Note:- this method would normally use a @ParamAttribute and @Valid annotation
     * but they tie this to a particular entity type so we unwrap all that stuff
     * and do it all manually
     *
     * @param session    Session to store temporary entities
     * @param request    Request causing this action
     * @param webRequest The native request object from Spring
     * @param model      Model to send data back to
     * @param id         a {@link java.lang.Integer} object.
     * @return a {@link java.lang.String} object.
     */
    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    public String save(HttpSession session, HttpServletRequest request, NativeWebRequest webRequest, Model model, @RequestParam(value = "id", required = false) Integer id) {

        BindingResult result;
        String returnValue = getRootTemplate();

        // Get the entity from the model

        Object entity;
        String name = getEntityClass().getSimpleName().toLowerCase();
        String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + name;
        if (id != null) entity = HibernateUtils.getEntity(getEntityClass(), id);
        else entity = HibernateUtils.getEntity(getEntityClass());
        try {

            // Create a binder to handle the setting of the properties

            WebDataBinder binder = new DefaultDataBinderFactory(null).createBinder(webRequest, entity, name);
            if (binder.getTarget() == null) logger.error("Cannot find a suitable target for  [{}]", name);
            else {

                // Set all the properties and validate the bean

                binder.setConversionService(conversionService);
                ((WebRequestDataBinder) binder).bind(webRequest);

                // Allow the values to be subverted etc.

                beforeValidation(session, request, entity);

                // Now validate the bean

                LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
                validator.afterPropertiesSet();
                binder.setValidator(validator);
                binder.validate(entity);
                result = binder.getBindingResult();

                // Add resolved attribute and BindingResult at the end of the model

                model.addAttribute(bindingResultKey, result);
                model.addAttribute(name, entity);

                // Carry out the normal save method

                returnValue = updateDatabase(session, request, model, entity, result);
                model.addAttribute("Entity", entity);

            }
        }
        catch (Exception e) {
            logger.error("Problem binding web request to object [{}] - {}", name, PivotalException.getErrorMessage(e));
        }

        // Populate the model with all the required display attributes

        populateModel(model, id, entity);

        // clear the autosave object

        AutoSaveController.autoSaveRemove(ServletHelper.getRequestInfo().getSubPageName(), id);

        return returnValue;
    }

    /**
     * This method is expected to be overridden by extenders that want to augment/adjust
     * the entity values before the entity os validated
     *
     * @param session Session to store temporary entities
     * @param request Request causing this action
     * @param entity  User entity to update or add
     */
    protected void beforeValidation(HttpSession session, HttpServletRequest request, Object entity) {
    }

    /**
     * Handles the saving of user data into the database
     * Returns error information through the usual channels
     *
     * @param session Session to store temporary entities
     * @param request Request causing this action
     * @param model   Model to send data back to
     * @param entity  User entity to update or add
     * @param result  Result of the action
     */
    private String updateDatabase(HttpSession session, HttpServletRequest request, Model model, Object entity, BindingResult result) {

        // Save the entity to the session so that we can get it back later

        String returnValue = getRootTemplate();
        Integer id = ClassUtils.getPropertyValue(entity, "Id");

        // If this entity has a settings property then we will need to
        // make a settings XML block to be saved

        convertSettingsXML(request, entity, "settings");

        // Similarly for an address XML block

        convertSettingsXML(request, entity, "address");

        // Check for problems

        EditStates editState = getEditState(id);

        // Call the owner to do any entity specific handling

        beforeSave(session, request, model, entity, id, editState, result);

        // Check to see if the user is trying to create a record with the same name as a constant

        try {
            Method method = entity.getClass().getMethod("getName");
            if (method != null) {
                String newName = (String)(method.invoke(entity));
                if (!Common.isBlank(newName) && isBlank(id)) {
                    Map<String, Object> entityFields = VelocityUtils.getConstants(entity.getClass());
                    if (!Common.isBlank(entityFields)) {
                        for (Object constant : entityFields.values()) {
                            if (newName.equals(I18n.translate((String) constant))) {
                                addError(result, "name", "system.error.entity_duplicate");
                                break;
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            logger.debug("Nothing to worry about");
        }

        // If no errors, save the record

        NotificationMessage message = null;
        if (!result.hasErrors()) {
            String changes = HibernateUtils.serializeEntity(entity);
            if (HibernateUtils.save(model, entity, result)) {
                HibernateUtils.addChangeLog(entity, changes, editState.equals(EditStates.ADDING));

                // Remove the session version and redirect if needed

                if (ServletHelper.parameterExists("noresponse")) returnValue = null;
                else if (request.getParameterMap().containsKey(ATTRIBUTE_PINNED))
                    if (request.getParameterMap().containsKey(CLEAR_AFTER_UPDATE))
                        returnValue = String.format("redirect:%s/edit?%s", getRequestMapping(), ATTRIBUTE_PINNED);
                    else
                        returnValue = String.format("redirect:%s/edit?copy&%s&id=%d", getRequestMapping(), ATTRIBUTE_PINNED, ClassUtils.getPropertyValue(entity, "id"));
                else {
                    boolean closeWindow = true;
                    try {
                        closeWindow = request.getParameterMap().containsKey("closewindow") && Common.isYes(request.getParameterMap().get("closewindow")[0]);
                    }
                    catch(Exception e) {
                        closeWindow = true;
                    }
                    model.addAttribute("CloseWindow", closeWindow);
                    returnValue = getRootTemplate();
                }

                // Success notification

                message = new NotificationMessage(I18n.getString("system.success.entity_saved"), Notification.NotificationLevel.Info, editState, true);

                // Call the override method

                afterSave(entity, id);
                // And send notification of the data change
                publisher.publishEvent(new DataChangeEvent("update", entity));
            }
        }

        // If we have errors

        if (result.hasErrors()) {
            model.addAttribute(ATTRIBUTE_PINNED, request.getParameterMap().containsKey(ATTRIBUTE_PINNED));

            // Add any controller specific attributes to the model

            addAttributesToModel(model, entity, id, editState);

            // Error notification

            message = new NotificationMessage(I18n.getString("system.error.entity_not_saved"), Notification.NotificationLevel.Error, editState, false);
        }

        // Adjust the notification message

        if (message != null) {
            addNotificationMessage(message);
            message.activateNotification();
        }
        return returnValue;
    }

    /**
     * Checks to see if this entity sports a settings/definition XML property
     * If it does, it reads the settings from the request and maps them to the definition
     * using reflection to find all the right property values
     *
     * @param request Request to interrogate
     * @param entity  Entity to update
     * @param property  Property
     */
    protected void convertSettingsXML(HttpServletRequest request, Object entity, String property) {

        // Check to see if the entity sports a settings input

        if (ClassUtils.propertyExists(getEntityClass(), property + "XML")) {

            // Clear the settings value

            ClassUtils.invokeMethod(entity, String.format("set%sXML", StringUtils.capitalize(property)), "");

            // Build a new settings entity

            DefinitionSettings values = ClassUtils.invokeMethod(entity, String.format("get%s", StringUtils.capitalize(property)));
            if (values != null) {
                Definition definition = values.getDefinition();

                // we have to try and defeat the forms mechanism that doesn't send checkbox
                // values if they are not ticked

                for (Parameter param : definition.getParameters()) {
                    if (param.isType("boolean") && request.getParameterMap().containsKey("settings-values._" + param.getName())) {
                        values.setParameterValue(param.getName(), "false");
                    }
                }

                // Loop through all the parameters sent to us

                for (String name : request.getParameterMap().keySet()) {

                    // Look for a parameter name

                    if (name.matches("(?i)settings-values\\.[a-z_0-9]+")) {
                        String parameterName = name.split("\\.")[1];

                        // If this is a multiple type parameter, then add all the values

                        if (!definition.parameterExists(parameterName)) {
                            logger.debug("A value was sent for a non existent parameter [{}]", parameterName);
                        }
                        else {
                            if (definition.getParameter(parameterName).isMultiple()) {
                                for (String value : request.getParameterValues(name)) {
                                    values.getValue(parameterName).add(Common.isBlank(value) ? null : value);
                                }
                            }
                            else {
                                String tmp = request.getParameter(name);
                                values.setParameterValue(parameterName, Common.isBlank(tmp) ? null : tmp);
                            }
                        }
                    }

                    // Look for a subparameter name

                    else if (name.matches("(?i)settings-values\\.[a-z_0-9]+\\.[a-z_0-9]+")) {
                        String parameterName = name.split("\\.")[1];
                        String subParameterName = name.split("\\.")[2];

                        // If this is a multiple type parameter, then add all the values for the specific subparameter

                        if (!definition.parameterExists(parameterName)) {
                            logger.debug("A value was sent for a non existent parameter[{}]", parameterName);
                        }
                        else if (!definition.getParameter(parameterName).subParameterExists(subParameterName)) {
                            logger.debug("A value was sent for a non existent subparameter[{}] of parameter [{}]", subParameterName, parameterName);
                        }
                        else {
                            if (definition.getParameter(parameterName).isMultiple()) {
                                int index = 0;
                                for (String value : request.getParameterValues(name)) {

                                    // Add the parameter if it doesn't exist

                                    if (index >= values.getValue(parameterName).getCount()) {
                                        values.getValue(parameterName).add(null);
                                    }

                                    // Set the subvalue

                                    values.getValue(parameterName).getSubValue(index, subParameterName).setValue(value);
                                    index++;
                                }
                            }
                            else {
                                values.setParameterValue(parameterName, request.getParameter(name));
                            }
                        }
                    }
                }
                ClassUtils.invokeMethod(entity, String.format("set%sXML", StringUtils.capitalize(property)), values.getXml());
            }
        }
    }

    /**
     * Called after an entity has been posted and is expected to be overridden
     * to add entity specific checking
     *
     * @param session   Session to store temporary entities
     * @param request   Request causing this action
     * @param model     Model to update with errors etc
     * @param entity    Entity to validate
     * @param id        Id of the entity
     * @param editState State of the entity
     * @param result    Binding result to use to check errors
     */
    protected void beforeSave(HttpSession session, HttpServletRequest request, Model model, Object entity, Integer id, EditStates editState, BindingResult result) {
    }

    /**
     * Adds any controller specific objects to the model
     * It should be overridden for this to happen
     *
     * @param model     Model to add to
     * @param entity    Entity currently under edit
     * @param id        The ID of the entity under edit
     * @param editState The type of edit
     */
    protected void addAttributesToModel(Model model, Object entity, Integer id, EditStates editState) {
    }

    /**
     * This method doesn't do anything but is intended to be overridden by sub-classes if they want
     * to add something to the notification message before it is sent
     *
     * @param message Message to add to
     */
    protected void addNotificationMessage(NotificationMessage message) {
    }

    /**
     * This method is called whenever a record is about to be deleted
     * It should be overridden if entity specific checks need to be run
     * e.g. stop a user deleting themselves from the users table
     *
     * @param entity       Entity to be removed
     * @param id           ID of the entity (saves invoking the method)
     * @param jsonResponse The response object to send messages back via
     * @return True if the record can be deleted
     */
    protected boolean beforeDelete(Object entity, Integer id, JsonResponse jsonResponse) {
        return true;
    }

    /**
     * This method is called whenever a record has been successfully saved
     *
     * @param entity Entity saved
     * @param id     ID of the entity (saves invoking the method)
     */
    protected void afterSave(Object entity, Integer id) {
    }

    /**
     * This method is called whenever a record has been successfully deleted
     *
     * @param entity Entity removed
     * @param id     ID of the entity (saves invoking the method)
     */
    protected void afterDelete(Object entity, Integer id) {
    }

    /**
     * Handy way of adding an error to the binding result
     *
     * @param result         Binding result to add to
     * @param field          Name of the field that the error belongs to
     * @param defaultMessage The I18N token to translate
     */
    public void addError(BindingResult result, String field, String defaultMessage) {
        if (result != null) {
            result.addError(new FieldError(getEntityClass().getSimpleName().toLowerCase(), field, I18n.getString(defaultMessage)));
        }
    }

    /**
     * Handy way of adding an error to the binding result
     *
     * @param result         Binding result to add to
     * @param field          Name of the field that the error belongs to
     * @param defaultMessage The I18N token to translate
     * @param msgParams      Parameters to pass to the translation
     */
    protected void addError(BindingResult result, String field, String defaultMessage, Object... msgParams) {
        if (result != null) {
            result.addError(new FieldError(getEntityClass().getSimpleName().toLowerCase(), field, I18n.getString(defaultMessage, msgParams)));
        }
    }

    /**
     * Handy way of adding an error to the binding result
     *
     * @param result         Binding result to add to
     * @param field          Name of the field that the error belongs to
     * @param rejectedValue  Value that has been rejected
     * @param defaultMessage The I18N token to translate
     */
    protected void addError(BindingResult result, String field, Object rejectedValue, String defaultMessage) {
        if (result != null) {
            result.addError(new FieldError(getEntityClass().getSimpleName().toLowerCase(), field, rejectedValue, false, null, null, I18n.getString(defaultMessage)));
        }
    }

    /**
     * Handy way of adding an error to the binding result
     *
     * @param result         Binding result to add to
     * @param field          Name of the field that the error belongs to
     * @param rejectedValue  Value that has been rejected
     * @param defaultMessage The I18N token to translate
     * @param msgParams      Parameters to pass to the translation
     */
    protected void addError(BindingResult result, String field, Object rejectedValue, String defaultMessage, Object... msgParams) {
        if (result != null) {
            result.addError(new FieldError(getEntityClass().getSimpleName().toLowerCase(), field, rejectedValue, false, null, null, I18n.getString(defaultMessage, msgParams)));
        }
    }

    /**
     * This class provides a means for callers to be able to add to the messages that
     * are sent to the user on save/delete events
     */
    protected class NotificationMessage {
        private String message;
        private Notification.NotificationLevel level;
        private EditStates editState;
        private boolean successful;

        /**
         * Creates a notification message that can be added to or overridden by an owner
         *
         * @param message Message to display
         */
        private NotificationMessage(String message, Notification.NotificationLevel level, EditStates editState, boolean successful) {
            this.message = message;
            this.level = level;
            this.editState = editState;
            this.successful = successful;
        }

        /**
         * This method adds the supplied message to the one that will be displayed
         *
         * @param message Message to add
         */
        protected void addMessage(String message) {
            if (!Common.isBlank(message)) {
                if (Common.isBlank(this.message)) this.message = message;
                else this.message += '\n' + message;
            }
        }

        /**
         * Returns the edit state
         *
         * @return Edit state
         */
        protected EditStates getEditState() {
            return editState;
        }

        /**
         * Returns true if the edit/delete action had been successful
         *
         * @return True if the action worked OK
         */
        protected boolean isSuccessful() {
            return successful;
        }

        /**
         * Sends the notification (if there is one) to the user
         */
        private void activateNotification() {
            if (!Common.isBlank(message)) {
                NotificationManager.addNotification(message, level, Notification.NotificationGroup.Individual, Notification.NotificationType.Application, true);
            }
        }
    }

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

        Map<Object, Object> model = new HashMap<Object, Object>();
        String errorMessage = "";

        errorMessage = PivotalException.getErrorMessage(ex);
        if (!isBlank(errorMessage)) {
            model.put("errors", errorMessage);
        }

        String pathToUse = ServletHelper.getPathInfo(request);

        if (pathToUse.endsWith("/"))
            pathToUse = pathToUse.substring(0, pathToUse.length() - 1);

        pathToUse += "_error";

        if (pathToUse.startsWith("/"))
            pathToUse = pathToUse.substring(1);

        model.putAll(ServletHelper.getGenericObjects(request, response, false));

        Context velocityContext = VelocityUtils.getVelocityContext();
        for (Object contextKey : velocityContext.getKeys())
            model.put(contextKey, velocityContext.get(contextKey.toString()));

        ModelAndView modelAndView = new ModelAndView(pathToUse, (Map) model);

        return modelAndView;
    }
}
