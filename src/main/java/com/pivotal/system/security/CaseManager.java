/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.security;

import com.pivotal.nrmm.service.notification.Notification;
import com.pivotal.system.data.cache.CacheEngine;
import com.pivotal.system.hibernate.entities.*;
import com.pivotal.system.hibernate.utils.AppDataSource;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.ClassUtils;
import com.pivotal.utils.Common;
import com.pivotal.utils.I18n;
import com.pivotal.utils.PivotalException;
import com.pivotal.utils.workflow.WorkflowHelper;
import com.pivotal.web.Constants;
import com.pivotal.web.controllers.AbstractAdminController;
import com.pivotal.web.controllers.SettingsController;
import com.pivotal.web.notifications.NotificationManager;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.postgresql.PGConnection;
import org.postgresql.largeobject.LargeObject;
import org.postgresql.largeobject.LargeObjectManager;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.*;

import static com.pivotal.system.hibernate.utils.HibernateUtils.selectEntities;
import static com.pivotal.utils.Common.isBlank;

/**
 * Provide a one stop shop for all thing associated with Cases
 * This class is the sole guardian of lists and case access etc
 */
public class CaseManager {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CaseManager.class);

    /**
     * Prevent instantiation
     */
    private CaseManager() {
    }

    /**
     * Returns a list of entities ordered by their name
     *
     * @param entityName Name of entity to get
     * @param allRecords If true then even unrequired records are returned
     * @param <E>   Generic entity
     *
     * @return List of PlanningAuthorityEntity objects
     */
    public static <E> List<E> getEntities(String entityName, boolean allRecords) {

        if (!isBlank(entityName)) {
            String query = "from " + entityName;
            if (!allRecords)
                query += " where disabled = false";
            query += " order by name";

            return selectEntities(query);
        }
        else
            return null;
    }

    /**
     * Handles the saving of user data into the database
     * Returns error information through the usual channels
     *
     * @param request Request causing this action
     * @param model   Model to send data back to
     * @param entity  User entity to update or add
     * @param result  Result of the action
     *
     * @return Boolean true if entity has saved
     */
    public static boolean updateDatabase(HttpServletRequest request, Model model, Object entity, BindingResult result) {

        return updateDatabase(request.getParameterMap(), model, entity, result, true);
    }

    /**
     * Handles the saving of user data into the database
     * Returns error information through the usual channels
     *
     * @param request Request causing this action
     * @param model   Model to send data back to
     * @param entity  User entity to update or add
     * @param result  Result of the action
     * @param showMessage   If true then save success message is output
     *
     * @return Boolean true if entity has saved
     */
    public static boolean updateDatabase(HttpServletRequest request, Model model, Object entity, BindingResult result, boolean showMessage) {

        return updateDatabase(request.getParameterMap(), model, entity, result, showMessage);
    }

    /**
     * Handles the saving of user data into the database
     * Returns error information through the usual channels
     *
     * @param params        parameters map
     * @param model         Model to send data back to
     * @param entity        User entity to update or add
     * @param result        Result of the action
     * @param showMessage   If true then save success message is output
     *
     * @return Boolean true if entity has saved
     */
    public static boolean updateDatabase(Map params, Model model, Object entity, BindingResult result, boolean showMessage) {

        logger.debug("Updating " + entity.getClass().getSimpleName() + " " + params);

        // Save the entity to the session so that we can get it back later

        boolean retValue = false;
        String message = null;
        Notification.NotificationLevel level=null;
        AbstractAdminController.EditStates editState = isBlank(entity)?AbstractAdminController.EditStates.ADDING:AbstractAdminController.EditStates.EDITING;

        // If no errors, save the record

        if (result==null || !result.hasErrors()) {
            String changes = HibernateUtils.serializeEntity(entity);
            if (HibernateUtils.save(model, entity, result)) {
                HibernateUtils.addChangeLog(entity, changes, editState.equals(AbstractAdminController.EditStates.ADDING));

                // Set next action

                model.addAttribute("CloseWindow", (!params.containsKey(AbstractAdminController.ATTRIBUTE_PINNED)));

                // Success notification

                message = I18n.getString("system.success.entity_saved");
                level = Notification.NotificationLevel.Info;
                retValue = true;
            }
        }

        // If we have errors

        if (result!=null && result.hasErrors()) {

            // Error notification

            message = I18n.getString("system.error.entity_not_saved");
            level = Notification.NotificationLevel.Error;
        }

        // Send the notification message

        if (showMessage && message != null && level != null) {
            NotificationManager.addNotification(message, level, Notification.NotificationGroup.Individual, Notification.NotificationType.Application, true);
        }

        model.addAttribute(AbstractAdminController.ATTRIBUTE_PINNED,params.containsKey(AbstractAdminController.ATTRIBUTE_PINNED));

        return retValue;
    }

    /**
     * Gets a report text object by its name
     *
     * @param reportName name of report text record
     *
     * @return ReportTextEntoty object
     */
    public static ReportTextEntity getReportText(String reportName) {

        if (!isBlank(reportName))
            return HibernateUtils.getEntity(ReportTextEntity.class, reportName);
        else
            return null;
    }

    /****************************************************************************
     *
     * Returns a string has all the MS "smart" characters replaced with their
     * normal ASCII equivalents
     *
     * @param sValue Value to be parsed
     *
     * @return Cleaned string
     *
     ***************************************************************************/
    public static String replaceSmartChars(String sValue) {
        String sReturn=sValue;
        if (!isBlank(sValue)) {
            sReturn=sReturn.replaceAll("\u0092|\u0091","'").replaceAll("\u0093|\u0094","\"").replaceAll("\u0096","-");
            sReturn=sReturn.replaceAll("\u2018|\u2019","'").replaceAll("\u202A|\u202B","\"").replaceAll("\u2013","-");

            sReturn=sReturn.replace((char)8220,'"').replace((char)8221,'"').replace((char)8222,'"');
            sReturn=sReturn.replace((char)8218,'\'').replace((char)8216,'\'').replace((char)8217,'\'');
            sReturn=sReturn.replace((char) 8211, '-').replace((char)8212,'-');
            sReturn=sReturn.replaceAll(String.valueOf((char) 8226), "\t&#8226;").replaceAll(String.valueOf((char)8230), "...");
        }
        return sReturn;
    }

    /**
     * Abbreviates names
     * e.g. Mr Alan Smith = AS
     *
     * @param name Name of person to abbreviate
     *
     * @return Abbreviated name
     */
    public static String abbreviate(String name) {
        String returnValue = name;

        if (!isBlank(name)) {
            returnValue = name.replaceAll("(?i)^(Ms|Mr|Mrs|Miss) ","").replaceAll("\\B[^ ]","").replaceAll(" ","");
        }

        return returnValue;
    }

    /**
     * Returns the text of the first template found with the specified type
     *
     * @param engine       Velocity engine to use
     * @param context      Context to use
     * @param templateName Template type to find
     *
     * @return String containing the report text
     *
     */
    public static String outputReportTemplate(VelocityEngine engine, Context context, String templateName) {

        String retValue = "";

        if (!isBlank(templateName)) {
            try {
                List<ReportTextEntity> reportTextList = selectEntities("From ReportTextEntity where lower(name) = ?", templateName.toLowerCase());

                if (!isBlank(reportTextList) && reportTextList.size() > 0 && !isBlank(reportTextList.get(0).getText())) {
                    StringWriter output = new StringWriter();
                    engine.evaluate(context, output, WorkflowHelper.class.getSimpleName(), reportTextList.get(0).getText());
                    retValue = output.toString();
                }
            }
            catch(Exception e) {
                logger.error("Error processing report template {} - {}", templateName, PivotalException.getErrorMessage(e));
            }
        }
        return retValue;
    }

    /**
     * Builds a deep link URL so users can open sites from anywhere
     *
     * @param siteId site to get link for
     *
     * @return String containing full link to case
     */
    public static String getDeepLink(Integer siteId) {
        return getDeepLink(siteId, null);
    }

    /**
     * Builds a deep link URL so users can open sites from anywhere
     *
     * @param siteId site to get link for
     * @param tab tab to display when link is shown
     *
     * @return String containing full link to case
     */
    public static String getDeepLink(Integer siteId, String tab) {

        return getDeepLink(HibernateUtils.getEntity(SiteEntity.class, siteId), null);
    }

    /**
     * Builds a deep link URL so users can open sites from anywhere
     *
     * @param siteEntity site to get link for
     *
     * @return String containing full link to case
     */
    public static String getDeepLink(SiteEntity siteEntity) {
        return getDeepLink(siteEntity, null);
    }

    /**
     * Builds a deep link URL so users can open sites from anywhere
     *
     * @param siteEntity site to get link for
     * @param tab         tab to display when link is shown
     *
     * @return String containing full link to site
     */
    public static String getDeepLink(SiteEntity siteEntity, String tab) {

        String retValue = "";
        logger.debug("Building deep link");

        if (!isBlank(siteEntity)) {
            retValue = Constants.getAppPath();
            if (isBlank(retValue)) retValue = "";

            retValue += "/deeplink/site/" + siteEntity.getId() + "/" + (isBlank(tab)?"":tab);

            logger.debug("Deeplink = " + retValue);
        }
        else
            logger.debug("Deep link not built as site entity is null");

        return retValue;
    }

    /**
     * Makes it easy to get values out of map even they don't exist
     *
     * @param params    map of params
     * @param name      key to get from map
     *
     * @return String containing value from map
     */
    public static String safeGet(Map<String, Object>params, String name) {
        return safeGet(params, name, null);
    }

    /**
     * Makes it easy to get values out of map even they don't exist
     *
     * @param params        map of params
     * @param name          key to get from map
     * @param defaultValue  default to return if name isn't in map
     *
     * @return String containing value from map
     */
    public static String safeGet(Map<String, Object>params, String name, String defaultValue) {
        String returnValue = defaultValue;

        if (params != null && params.containsKey(name) && !isBlank(params.get(name))) {
            Object value = params.get(name);
            if(value instanceof String[] && ((String[]) value).length > 0)
                returnValue = Common.join(((String[]) value), ",");
            else
                returnValue = value.toString();
        }

        return returnValue;
    }

    /**
     * Adds working days onto a date, taking into consideration weekends and holidays
     *
     * @param startDate date to start at
     * @param daysToAdd number of days to add positive or negative
     *
     * @return Date with days added
     */
    public static Date addWorkingDays(Date startDate, int daysToAdd) {

        Calendar retValue = Calendar.getInstance();
        if (!isBlank(startDate)) {
            retValue.setTime(startDate);
            int vector = daysToAdd > 0?1:-1;
            daysToAdd = Math.abs(daysToAdd);

            while (daysToAdd > 0) {

                retValue.add(Calendar.DAY_OF_MONTH, vector);

                if (retValue.get(Calendar.DAY_OF_WEEK) != 1 && retValue.get(Calendar.DAY_OF_WEEK) != 7 && !isHoliday(retValue.getTime()))
                    daysToAdd -=1;
            }
        }

        return retValue.getTime();
    }

    /**
     * Adds days onto a date, ignoring weekends and holidays
     * The resultant day cannot be on a weekend or on one of the
     * restricted holidays such as good friday, christmas, boxing day
     * or new years day
     *
     * @param startDate date to start at
     * @param daysToAdd number of days to add positive or negative
     *
     * @return Date with days added
     */
    public static Date addRestrictedDays(Date startDate, int daysToAdd) {

        Calendar retValue = Calendar.getInstance();
        if (!isBlank(startDate)) {
            retValue.setTime(startDate);

            retValue.add(Calendar.DAY_OF_MONTH, daysToAdd);

            // Now go in 'vector' direction until date is not a weekend or restricted holiday

            int vector = daysToAdd < 0?1:-1;

            while (retValue.get(Calendar.DAY_OF_WEEK) == 1 || retValue.get(Calendar.DAY_OF_WEEK) == 7 || isRestrictedHoliday(retValue.getTime()))
                retValue.add(Calendar.DAY_OF_MONTH, vector);
        }

        return retValue.getTime();
    }

    /**
     * Indicates if the specified date is in the list of holidays
     *
     * @param date date to check
     *
     * @return true if date is a holiday
     */
    public static boolean isHoliday(Date date) {

        final String FORMAT_TO_USE = "yyyyMMMdd";

        Map<String, Date>cachedHolidays = CacheEngine.get(HibernateUtils.SETTING_APP_HOLIDAYS);
        if (cachedHolidays == null) {
            cachedHolidays = new HashMap<>();
            String dates = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_APP_HOLIDAYS, "");

            if (!isBlank(dates)) {
                List<String>dateStringList = Common.splitToList(dates, "[\r\n]");
                for(String thisStringDate : dateStringList) {
                    Date thisDate = Common.parseDate(thisStringDate);
                    cachedHolidays.put(Common.formatDate(thisDate, FORMAT_TO_USE), thisDate);
                }
            }

            CacheEngine.put(HibernateUtils.SETTING_APP_HOLIDAYS, 600, cachedHolidays);
        }

        // Return a true or false if in list

        return cachedHolidays.containsKey(Common.dateFormat(date, FORMAT_TO_USE));
    }

    /**
     * Indicates if the specified date is in the list of holidays
     *
     * @param date date to check
     *
     * @return true if date is a holiday
     */
    public static boolean isRestrictedHoliday(Date date) {

        final String FORMAT_TO_USE = "yyyyMMMdd";

        Map<String, Date>cachedHolidays = CacheEngine.get(HibernateUtils.SETTING_APP_RESTRICTED_HOLIDAYS);
        if (cachedHolidays == null) {
            cachedHolidays = new HashMap<>();
            String dates = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_APP_RESTRICTED_HOLIDAYS, "");

            if (!isBlank(dates)) {
                List<String>dateStringList = Common.splitToList(dates, "[\r\n]");
                for(String thisStringDate : dateStringList) {
                    Date thisDate = Common.parseDate(thisStringDate);
                    cachedHolidays.put(Common.formatDate(thisDate, FORMAT_TO_USE), thisDate);
                }
            }

            CacheEngine.put(HibernateUtils.SETTING_APP_RESTRICTED_HOLIDAYS, 600, cachedHolidays);
        }

        // Return a true or false if in list

        return cachedHolidays.containsKey(Common.dateFormat(date, FORMAT_TO_USE));
    }


    /**
      * Adds a note for the specified record
      *
      * @param entity       Entity to add note to
      * @param tag          Text identifying the note to be removed
      */
    public static void removeNote(Object entity, String tag) {

        Integer id = ClassUtils.invokeMethod(entity, "getId");
        String refType = entity.getClass().getSimpleName();

        if (!Common.isBlank(id) && !Common.isBlank(refType) && !Common.isBlank(tag)) {
            HibernateUtils.delete(selectEntities("From NoteEntity where referenceId = ? and referenceType = ? and tag = ?", id, refType, tag));
            HibernateUtils.commit();
        }
    }

    /**
     * Returns a list of notes for the entity
     *
     * @param entity    Entity to get notes for
     *
     * @return List of notes
     */
    public static List<NoteEntity>getNote(Object entity) {

        return getNote(entity, null);

    }

    /**
     * Returns a list of notes for the entity
     *
     * @param entity    Entity to get notes for
     * @param tag       Tag to restrict the notes to
     *
     * @return List of notes
     */
    public static List<NoteEntity>getNote(Object entity, String tag) {

        List<NoteEntity>noteEntities = new ArrayList<>();
        if (!isBlank(entity)) {

            try {
                Map<String, Object> values = new HashMap<>();
                values.put("referenceId", ClassUtils.invokeMethod(entity, "getId"));
                values.put("referenceType", entity.getClass().getSimpleName());
                String query = "From NoteEntity where referenceId = :referenceId and referenceType = :referenceType";
                if (!isBlank(tag)) {
                    query += " and tag = :tag";
                    values.put("tag", tag);
                }

                noteEntities = HibernateUtils.selectEntities(query, values);
            }
            catch(Exception e) {
                logger.error("Unable to return note entities - {}", PivotalException.getErrorMessage(e));
            }
        }
        else
            logger.error("Unable to get note as the specified entity is null");

        return  noteEntities;

    }

    /**
      * Adds a note for the specified record
      *
      * @param entity       Entity to add note to
      * @param noteText     Note Text
      * @param folder       Folder to label note with
      */
    public static void addNote(Object entity, String noteText, String folder) {

        if (!isBlank(entity) && !isBlank(noteText)) {

            addNote((Integer)ClassUtils.invokeMethod(entity, "getId"),
                    entity.getClass().getSimpleName(),
                    noteText,
                    folder,
                    null,
                    null,
                    true,
                    null,
                    null,
                    NoteTypeEntity.NOTE_TYPE_GENERAL);
        }
        else
            logger.error("Unable to set note as there is a missing component [isBlank] entity:{} text:{}", isBlank(entity), isBlank(noteText));
    }

    /**
      * Adds a note for the specified record
      *
      * @param entity       Entity to add note to
      * @param noteText     Note Text
      * @param folder       Folder to label note with
      * @param actionDate   Date to use
      * @param roleName     Role to attach note to
      * @param userId       User to attach note to
      * @param tag          Tag used to specify some special function
      */
    public static void addNote(Object entity, String noteText, String folder, Date actionDate, String roleName, Integer userId, String tag) {

        if (!isBlank(entity) && !isBlank(noteText)) {

            addNote((Integer)ClassUtils.invokeMethod(entity, "getId"),
                    entity.getClass().getSimpleName(),
                    noteText,
                    folder,
                    actionDate,
                    roleName,
                    true,
                    userId,
                    tag,
                    NoteTypeEntity.NOTE_TYPE_GENERAL);
        }
        else
            logger.error("Unable to set note as there is a missing component [isBlank] entity:{} text:{}", isBlank(entity), isBlank(noteText));
    }

    /**
      * Adds a note for the specified record
      *
      * @param entity       Entity to add note to
      * @param noteText     Note Text
      * @param folder       Folder to label note with
      * @param actionDate   Date to use
      * @param roleName     Role to attach note to
      * @param linkedOnly    If true then user must be on case/meeting and have role to see note
      * @param userId       User to attach note to
      * @param tag          Tag used to specify some special function
      */
    public static void addNote(Object entity, String noteText, String folder, Date actionDate, String roleName, Boolean linkedOnly, Integer userId, String tag) {

        if (!isBlank(entity) && !isBlank(noteText)) {

            addNote((Integer)ClassUtils.invokeMethod(entity, "getId"),
                    entity.getClass().getSimpleName(),
                    noteText,
                    folder,
                    actionDate,
                    roleName,
                    linkedOnly,
                    userId,
                    tag,
                    NoteTypeEntity.NOTE_TYPE_GENERAL);
        }
        else
            logger.error("Unable to set note as there is a missing component [isBlank] entity:{} text:{}", isBlank(entity), isBlank(noteText));
    }

    /**
     * Creates a noteentity for the specified details
     *
     * @param referenceId   Referenced Id
     * @param referenceType Referenced Type
     * @param noteText      Text of note
     * @param folder        Folder for note (optional)
     * @param actionDate    Action date for note (optional)
     * @param roleName      Role to add note to
     * @param linkedOnly    If true then user must be on case/meeting and have role to see note
     * @param userId        User to add note to
     * @param tag           Tag to catagorise the note (optional)
     * @param noteTypeName  Type of note
     */
    public static void addNote(Integer referenceId,
                               String referenceType,
                               String noteText,
                               String folder,
                               Date actionDate,
                               String roleName,
                               Boolean linkedOnly,
                               Integer userId,
                               String tag,
                               String noteTypeName
    ) {

        if (!isBlank(referenceId) && !isBlank(referenceType) && !isBlank(noteText)) {

            try {

                boolean finished = false;
                String thisRoleName;

                List<RoleEntity>roleList = new ArrayList<>();
                if (!isBlank(roleName) && (roleName.contains(",") || roleName.startsWith("~"))) {
                    String roleQuery;
                    boolean negateQuery = false;
                    if (roleName.startsWith("~")) {
                        roleQuery = roleName.substring(1);
                        negateQuery = true;
                    }
                    else
                        roleQuery = roleName;

                    roleQuery = "'" + roleQuery.replace(",","','") + "'";
                    roleList = selectEntities(String.format("From RoleEntity where %s name in (%s)", (negateQuery?"not ":""), roleQuery));
                }
                else {
                    roleList.add(HibernateUtils.getEntity(RoleEntity.class, roleName));
                }

                // Get Current user or use admin if this is in a background task
                UserEntity currentUser = UserManager.getCurrentUser();
                if (currentUser == null)
                    currentUser = HibernateUtils.getEntity(UserEntity.class, UserEntity.DEFAULT_USER_NAME);
                for(RoleEntity roleEntity : roleList) {

                    NoteEntity noteEntity = HibernateUtils.getEntity(NoteEntity.class);

                    noteEntity.setReferenceId(referenceId);
                    noteEntity.setReferenceType(referenceType);
                    noteEntity.setContent(noteText.length() > 1000 ? noteText.substring(0, 1000) : noteText);
                    noteEntity.setFolder(isBlank(folder) ? "" : folder);
                    noteEntity.setType(HibernateUtils.getEntity(NoteTypeEntity.class, noteTypeName));
                    noteEntity.setTag(tag);
                    noteEntity.setRole(roleEntity);
                    noteEntity.setLinkedOnly(linkedOnly);
                    noteEntity.setUser(HibernateUtils.getEntity(UserEntity.class, userId));
                    if (!Common.isBlank(actionDate))
                        noteEntity.setActionDate(actionDate);
                    noteEntity.setViewed(false);
                    noteEntity.setAddedBy(currentUser);
                    noteEntity.setTimeAddedNow();

                    HibernateUtils.save(noteEntity);
//                    WorkflowHelper.progressNote(noteEntity);
                }
            }
            catch(Exception e) {
                logger.debug("Unable to set user note - {}", PivotalException.getErrorMessage(e));
            }
        }
        else
            logger.error("Unable to set user note as there is a missing component [isBlank] referenceId:{} referenceType:{} text:{}", isBlank(referenceId), isBlank(referenceType), isBlank(noteText), isBlank(noteText));
    }

    /**
     * Creates a noteentity for the specified details
     *
     * @param referenceId   Referenced Id
     * @param referenceType Referenced Type
     * @param noteText      Text of note
     * @param folder        Folder for note (optional)
     * @param userId        User to add note to
     * @param tag           Tag to catagorise the note (optional)
     * @param noteTypeName  Type of note
     */
    public static NoteEntity addNote(Integer referenceId,
                               String referenceType,
                               String noteText,
                               String folder,
                               Integer userId,
                               String tag,
                               String noteTypeName
    ) {

        if (!isBlank(referenceId) && !isBlank(referenceType) && !isBlank(noteText)) {

            try {

                // Get Current user or use admin if this is in a background task
                UserEntity currentUser = UserManager.getCurrentUser();
                if (currentUser == null)
                    currentUser = HibernateUtils.getEntity(UserEntity.class, UserEntity.DEFAULT_USER_NAME);

                NoteEntity noteEntity = HibernateUtils.getEntity(NoteEntity.class);

                noteEntity.setReferenceId(referenceId);
                noteEntity.setReferenceType(referenceType);
                noteEntity.setContent(noteText.length() > 1000 ? noteText.substring(0, 1000) : noteText);
                noteEntity.setFolder(isBlank(folder) ? "" : folder);
                noteEntity.setType(HibernateUtils.getEntity(NoteTypeEntity.class, noteTypeName));
                noteEntity.setTag(tag);
                noteEntity.setUser(HibernateUtils.getEntity(UserEntity.class, userId));
                noteEntity.setAddedBy(currentUser);
                noteEntity.setTimeAddedNow();

                HibernateUtils.save(noteEntity);
                return noteEntity;
            }
            catch(Exception e) {
                logger.debug("Unable to set user note - {}", PivotalException.getErrorMessage(e));
            }
        }
        else
            logger.error("Unable to set user note as there is a missing component [isBlank] referenceId:{} referenceType:{} text:{}", isBlank(referenceId), isBlank(referenceType), isBlank(noteText), isBlank(noteText));

        return null;
    }

    /**
     * Returns a timestamp for use in the databases
     *
     * @return timestamp for the current date/time
     */
    public static Timestamp getTimeStamp() {
        return getTimeStamp(null);
    }

    /**
     * Returns a timestamp for use in the databases
     *
     * @param date date to use to for timestamp
     *             if null then uses now
     *
     * @return time stamp
     */
    public static Timestamp getTimeStamp(Date date) {

        if (date == null)
            return new Timestamp(new Date().getTime());
        else
            return new Timestamp(date.getTime());
    }


    /**
     * Returns a timestamp for use in the databases
     *
     * @param seconds number o seconds to adjust timestamp by
     *
     * @return time stamp
     */
    public static Timestamp getTimeStampAdjusted(int seconds) {
        return getTimeStampAdjusted(null, seconds);
    }

    /**
     * Returns a timestamp for use in the databases
     *
     * @param date date to use to for timestamp
     *             if null then uses now
     * @param seconds number o seconds to adjust timestamp by
     *
     * @return time stamp
     */
    public static Timestamp getTimeStampAdjusted(Date date, int seconds) {

        Calendar objCalendar = Calendar.getInstance();
        if (date != null)
            objCalendar.setTime(date);

        objCalendar.add(Calendar.SECOND, seconds);

        return new Timestamp(objCalendar.getTimeInMillis());
    }

    /**
     * Returns the date of monday of the current week
     *
     * @return Date of the monday
     */
    public static Date getMonday() {
        return getMonday(0);
    }

    /**
     * Returns the date of monday of the current week
     * or adds 'offset' weeks to date
     *
     * @param offset number of weeks to offset the date
     *
     * @return Date of the monday
     */
    public static Date getMonday(int offset) {
        return getMonday(offset, null);
    }

    /**
     * Returns the date of monday of the current week
     * or adds 'offset' weeks to date
     *
     * @param offset number of weeks to offset the date
     * @param startDate date in week to get monday for
     *
     * @return Date of the monday
     */
    public static Date getMonday(int offset, Date startDate) {

        Calendar calendar = Calendar.getInstance();

        if (startDate != null)
            calendar.setTime(startDate);

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        if (offset != 0)
            calendar.add(Calendar.WEEK_OF_YEAR, offset);

        calendar.set(Calendar.AM_PM, Calendar.AM);
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime();
    }


    /**
     * Trims the specified strings from the beginning of the supplied string
     *
     * @param sValue    String to trim
     * @param sCharList   Regular expression list of characters to trim off front
     *
     * @return Trimmed string
     */
    public static String trimLeadRegEx(String sValue, String sCharList) {
        String sReturn = sValue;
        if (sCharList != null && sValue != null) {
            sReturn = sValue.replaceAll("^[" + sCharList + "]+","");
        }
        return sReturn;
    }

    /**
     * Sends notification to browser
     * Added to allow workflow to send messages
     *
     * @param message Message to Display
     */
    public static void sendNotification(String message) {

        NotificationManager.addNotification(message, Notification.NotificationLevel.Info, Notification.NotificationGroup.Individual, Notification.NotificationType.Application, true);
    }

    /**
     * Reads file from OID
     *
     * @param mediaId   Id of Media record
     *
     * @return File loaded with data
     */
    public static File getMediaFile(Integer mediaId) {

        File retValue = null;

        if (mediaId != null) {
            MediaEntity mediaEntity = HibernateUtils.getEntity(MediaEntity.class, mediaId);

            if (!isBlank(mediaEntity))
                retValue = getMediaFile(mediaEntity);
        }

        return retValue;

    }

    /**
     * Reads file from OID
     *
     * @param mediaEntity   Media entity record
     *
     * @return File loaded with data
     */
    public static File getMediaFile(MediaEntity mediaEntity) {

        File retValue = null;

        if (!isBlank(mediaEntity)) {
            try {

//                if (isBlank(mediaEntity.getBlobOid())) {
                if (false) {

                    // Get the old version

                    MediaFileEntity mediaFileEntity = HibernateUtils.getEntity(MediaFileEntity.class, mediaEntity.getId());

                    InputStream inputStream =  new ByteArrayInputStream(mediaFileEntity.getFile());

                    // Create a temporary file to read it into

                    retValue = Common.getTemporaryFile(mediaEntity.getExtension());

                    // Pipe from byte array stream to file

                    Common.pipeInputToOutputStream(inputStream, retValue);

                    // Close input

                    inputStream.close();


                }
                else {

                    // Get data source

                    AppDataSource dataSource = HibernateUtils.getDataSource();

                    // Make sure it is a PostgreSQL connection

                    if (dataSource.getConnection().isWrapperFor(PGConnection.class)) {
                        PGConnection pgConnection = dataSource.getConnection().unwrap(PGConnection.class);
                        ((Connection) pgConnection).setAutoCommit(false);

                        // Get Large Object Manager

                        LargeObjectManager largeObjectManager = pgConnection.getLargeObjectAPI();

//                        Long blobOid = mediaEntity.getBlobOid();
                        Long blobOid = null;

                        if (!isBlank(blobOid)) {

                            // Open object for reading

                            LargeObject largeObject = largeObjectManager.open(blobOid, LargeObjectManager.READ, true);

                            // Create a temporary file to read it into

                            retValue = Common.getTemporaryFile(mediaEntity.getExtension());

                            // Pipe from object stream to file

                            Common.pipeInputToOutputStream(largeObject.getInputStream(), retValue);

                            // Close

                            largeObject.close();
                        }
                    }
                    else {
                        logger.debug(I18n.getString("system.error.entity_incorrect_connection"));
                    }
                }
            }
            catch(Exception e){
                logger.debug(I18n.getString("system.error.entity_file_not_saved") + PivotalException.getErrorMessage(e));
            }
        }

        return retValue;
    }

    /**
     * Reads file from OID and returns the input stream
     * It's up to the caller to close the stream
     *
     * @param mediaEntity   Media entity record
     *
     * @return Inputstream from the database
     */
    public static InputStream getMediaInputStream(MediaEntity mediaEntity) {

        InputStream retValue = null;

        try {
            if (!isBlank(mediaEntity)) {
//                if (isBlank(mediaEntity.getBlobOid())) {
                if (false) {

                    // Get the old version

                    MediaFileEntity mediaFileEntity = HibernateUtils.getEntity(MediaFileEntity.class, mediaEntity.getId());

                    retValue =  new ByteArrayInputStream(mediaFileEntity.getFile());

                }
                else {

                    // Get data source

                    AppDataSource dataSource = HibernateUtils.getDataSource();

                    // Make sure it is a PostgreSQL connection

                    if (dataSource.getConnection().isWrapperFor(PGConnection.class)) {
                        PGConnection pgConnection = dataSource.getConnection().unwrap(PGConnection.class);
                        ((Connection) pgConnection).setAutoCommit(false);

                        // Get Large Object Manager

                        LargeObjectManager largeObjectManager = pgConnection.getLargeObjectAPI();

//                        Long blobOid = mediaEntity.getBlobOid();
                        Long blobOid = null;

                        if (!isBlank(blobOid)) {

                            // Open object for reading

                            LargeObject largeObject = largeObjectManager.open(blobOid, LargeObjectManager.READ, true);

                            retValue = largeObject.getInputStream();
                        }
                    }
                    else {
                        logger.debug(I18n.getString("system.error.entity_incorrect_connection"));
                    }
                }
            }
        }
        catch (Exception e) {
            logger.debug(I18n.getString("system.error.entity_file_not_saved") + PivotalException.getErrorMessage(e));
        }

        return retValue;
    }

    /**
     * Adds file data to media_file table
     *
     * @param mediaId   Media record blob is associated with
     * @param file      File to be uploaded
     *
     * @return True if all worked out ok
     */
    public static boolean addMediaFile(Integer mediaId, File file) {

        boolean retValue = false;

        if (mediaId != null) {
            MediaEntity mediaEntity = HibernateUtils.getEntity(MediaEntity.class, mediaId);

            if (!isBlank(mediaEntity))
                retValue = addMediaFile(mediaEntity, file);
        }

        return retValue;
    }

    /**
     * Adds file data to media_file table reading from the source media entity
     * Determines whether to do a server side copy or not
     *
     * @param mediaEntity       Media record blob is associated with
     * @param sourceMediaEntity Media entity with File to be uploaded
     *
     * @return True if all worked out ok
     */
    public static boolean addMediaFile(MediaEntity mediaEntity, MediaEntity sourceMediaEntity) {

        boolean retValue = false;
        try {
            if (!Common.isBlank(sourceMediaEntity)) {
//                if (Common.isBlank(sourceMediaEntity.getBlobOid()))
                if (true)
                    retValue = addMediaFile(mediaEntity, getMediaFile(sourceMediaEntity));
                else {
                    logger.debug("Trying server side copy of large file");
//                    String command = String.format("select copylo(%d);", sourceMediaEntity.getBlobOid());
//
//                    List<Object> loResults = HibernateUtils.selectSQLEntities(command);
//                    if (!Common.isBlank(loResults)) {
//                        Long newLoid = parseNumber(loResults.get(0).toString()).longValue();
//                        mediaEntity.setBlobOid(newLoid);
//                        if (!HibernateUtils.save(mediaEntity))
//                            logger.debug("Unable to copy document, save of entity failed");
//                    }
//                    else
//                        logger.debug("Unable to copy document as an invalid OID was returned");
                }
            }
        }
        catch (Exception e) {
            logger.debug("Unable to get file input stream {} " + PivotalException.getErrorMessage(e));
        }

        return retValue;
    }

    /**
     * Adds file data to media_file table
     *
     * @param mediaEntity   Media record blob is associated with
     * @param file      File to be uploaded
     *
     * @return True if all worked out ok
     */
    public static boolean addMediaFile(MediaEntity mediaEntity, File file) {

        boolean retValue = false;
        try {
            InputStream fileInputStream = new BufferedInputStream(new FileInputStream(file), 32 * 1024);
            retValue = addMediaFile(mediaEntity, fileInputStream);
            fileInputStream.close();
        }
        catch (Exception e) {
            logger.debug("Unable to get file input stream {} " + PivotalException.getErrorMessage(e));
        }

        return retValue;
    }

    /**
     * Adds file data to media table
     *
     * @param mediaId   Media record blob is associated with
     * @param file      File to be uploaded
     *
     * @return True if all worked out ok
     */
    public static boolean addMediaFile(Integer mediaId, MultipartFile file) {
        boolean retValue = false;
        try {
            MediaEntity mediaEntity = HibernateUtils.getEntity(MediaEntity.class, mediaId);

            if (!isBlank(mediaEntity)) {
                retValue = addMediaFile(mediaEntity, file);
            }
        }
        catch (Exception e) {
            logger.debug("Unable to get file input stream {} " + PivotalException.getErrorMessage(e));
        }

        return retValue;
    }

    /**
     * Adds file data to media_file table
     *
     * @param mediaEntity   Media record blob is associated with
     * @param file          File to be uploaded
     *
     * @return True if all worked out ok
     */
    public static boolean addMediaFile(MediaEntity mediaEntity, MultipartFile file) {

        boolean retValue = false;
        try {
            InputStream inputStream = file.getInputStream();
            retValue = addMediaFile(mediaEntity, inputStream);
            inputStream.close();
        }
        catch (Exception e) {
            logger.debug("Unable to get file input stream {} " + PivotalException.getErrorMessage(e));
        }

        return retValue;
    }

    /**
     * Adds file data to media table
     *
     * @param mediaEntity       Media record blob is associated with
     * @param fileInputStream   File Input Stream to be uploaded
     *
     * @return True if all worked out ok
     */
    public static boolean addMediaFile(MediaEntity mediaEntity, InputStream fileInputStream) {

        boolean retValue = false;

        if (mediaEntity.getFileSize() < 20971520) {

            // Store in media file byte array

            MediaFileEntity mediaFileEntity = HibernateUtils.getEntity(MediaFileEntity.class, mediaEntity.getId());

            if (mediaFileEntity != null) {
                try {
                    mediaFileEntity.setFile(IOUtils.toByteArray(fileInputStream));
                    retValue = HibernateUtils.save(mediaFileEntity);
                }
                catch (Exception e) {
                    logger.debug("Error loading fileInputStream into ByteArray " + PivotalException.getErrorMessage(e));
                }
            }
        }
        else {
            PGConnection pgConnection = null;
            try {

                // Get data source

                AppDataSource dataSource = HibernateUtils.getDataSource();

                // Make sure it is a postgreSQL connection

                if (dataSource.getConnection().isWrapperFor(PGConnection.class)) {
                    pgConnection = dataSource.getConnection().unwrap(PGConnection.class);

                    // All LargeObject methods need to be in a transaction

                    ((Connection) pgConnection).setAutoCommit(false);

                    // Get Large Object Manager

                    LargeObjectManager largeObjectManager = pgConnection.getLargeObjectAPI();

                    // Create OID to use
                    long oid = largeObjectManager.createLO(LargeObjectManager.READ | LargeObjectManager.WRITE);

                    // Open it for writing

                    LargeObject largeObject = largeObjectManager.open(oid, LargeObjectManager.WRITE);

                    // Pipe contents of uploaded file to object
                    Common.pipeInputToOutputStream(fileInputStream, largeObject.getOutputStream(), true, false);

                    // Close the object

                    largeObject.close();

                    // Update the database with the details

//                    mediaEntity.setBlobOid(oid);
                    retValue = HibernateUtils.save(mediaEntity);
                    ((Connection) pgConnection).commit();
                }
                else {
                    logger.debug(I18n.getString("system.error.entity_incorrect_connection"));
                }
            }
            catch(Exception e){
                logger.debug(I18n.getString("system.error.entity_file_not_saved") + PivotalException.getErrorMessage(e));
            }
            finally{
                if (pgConnection != null) {
                    try {
                        ((Connection) pgConnection).close();
                    }
                    catch (Exception e) {
                        logger.debug("Error closing PGConnection " + PivotalException.getErrorMessage(e));
                    }
                }
            }
        }

        return retValue;
    }

    /**
     * Returns list of notes for the specified user
     *
     * @param whereClause   NoteEntity Query
     * @param orderClause   Order to sort results
     * @param userId        User id to restrict results to
     *
     * @return List of note entities
     */
    public static List<NoteEntity>getUserNotes(String whereClause, String orderClause, Integer userId) {

        List<NoteEntity> noteEntityList = new ArrayList<>();

        if (!isBlank(userId)) {

            List<Object> noteIds = ViewSecurity.userNoteSearch(userId, null);

            if (!isBlank(noteIds)) {
                Map<String, Object> values = new HashMap<>();
                values.put("noteIds", noteIds);

                String query = "From NoteEntity where Id in (:noteIds)" + (isBlank(whereClause)?"":(" and " + whereClause)) + (isBlank(orderClause)?"":(" order by " + orderClause));

                noteEntityList = HibernateUtils.selectEntities(query, false, values);
            }
        }

        return noteEntityList;
    }

    /**
     * Swaps the boolean value
     * Handy for inline velocity
     *
     * @param value boolean value
     *
     * @return negated boolean value
     */
    public static boolean negate(Boolean value) {

        return !Common.isYes(value);
    }

    /**
     * Reads the holiday dates from the specified url and updates the settings
     * Designed to be called from an automated process so that the dates are
     * automatically kept up to date
     *
     * @return True if ok
     */
    public static boolean loadHolidayDates() {

        boolean result = false;

        try {
            String newDates = SettingsController.getHolidayDates();

            if (Common.isBlank(newDates))
                logger.debug("No Holiday dates found");
            else {
                SettingsEntity settingsEntity = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_APP_HOLIDAYS);
                settingsEntity.setValue(newDates);
                HibernateUtils.save(settingsEntity);
                CacheEngine.delete(HibernateUtils.SETTING_APP_HOLIDAYS);
            }
        }
        catch (Exception e) {
            logger.error("Error loading holiday dates {}", PivotalException.getErrorMessage(e));
        }

        return  result;
    }

    /**
     * Returns formated list of note tags to be ignored
     * @return String containing note ignore tags quoted and comma
     *                separated ready for use in sql
     */
    public static String getNoteIgnoreTagsSQL() {
        String ignoreTags = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_WIDGET_NOTE_TAG_IGNORE, "");
        if (!Common.isBlank(ignoreTags)) {
            ignoreTags = "'" + Common.join(Common.split(ignoreTags.replaceAll("'","''"),","),"','") + "'";
        }
        return ignoreTags;
    }

    /**
     * Adds a user to the site
     *
     * @param siteEntity Site to add user to
     * @param userEntity User to add to site
     * @param roleName Name of users role
     */
    public static void addSiteUser(SiteEntity siteEntity, UserEntity userEntity, String roleName) {

        if (!isBlank(userEntity) && !isBlank(userEntity.getId()) && !isBlank(roleName)) {
            RoleEntity roleEntity = HibernateUtils.getEntity(RoleEntity.class, roleName);
            if (roleEntity == null) {
                // Create role
                roleEntity = HibernateUtils.getEntity(RoleEntity.class);
                roleEntity.setName(roleName);
                roleEntity.setDescription(roleName);
                roleEntity.setDisabled(false);
                HibernateUtils.getCurrentSession().save(roleEntity);
                logger.debug("Creating new role - {}", roleName);
            }
            if (roleEntity != null) {
                SiteUsersEntity siteUsersEntity = HibernateUtils.getEntity(SiteUsersEntity.class);
                siteUsersEntity.setRole(roleEntity);
                siteUsersEntity.setUser(userEntity);
                SiteEntity newSiteEntity = HibernateUtils.getEntity(SiteEntity.class, siteEntity.getId());
                siteUsersEntity.setSite(newSiteEntity);
                newSiteEntity.getSiteUsers().add(siteUsersEntity);
                try {
                    if (HibernateUtils.save(siteUsersEntity))
                        logger.debug("Saved new siteUser - {}", siteUsersEntity);
                    else
                        logger.error("Error adding new siteUser - {}", siteUsersEntity);
                }
                catch(Exception e) {
                    logger.debug("Error adding site user record - {}", PivotalException.getErrorMessage(e));
                }
            }
            else
                logger.error("Unable to add user as role could not be loaded or created");
        }
    }
}
