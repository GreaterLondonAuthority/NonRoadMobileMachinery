/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers;

import com.pivotal.system.data.cache.CacheAccessorFactory;
import com.pivotal.system.data.cache.CacheEngine;
import com.pivotal.system.data.dao.DataSourceUtils;
import com.pivotal.system.data.dao.Database;
import com.pivotal.system.data.dao.DatabaseHibernate;
import com.pivotal.system.hibernate.entities.ChangeLogEntity;
import com.pivotal.system.hibernate.entities.LogEntity;
import com.pivotal.system.hibernate.entities.SettingsEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.security.UserManager;
import com.pivotal.utils.*;
import com.pivotal.web.Constants;
import com.pivotal.web.controllers.utils.Authorise;
import com.pivotal.web.controllers.utils.JsonResponse;
import com.pivotal.web.servlet.ServletHelper;
import com.pivotal.web.utils.ThemeManager;
import org.hibernate.Session;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Handles all the settings
 */
@Authorise
@Controller
@RequestMapping(value = {"/settings", "/admin/settings"})
public class SettingsController extends AbstractController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SettingsController.class);

    /**
     * Returns the form for entering settings values
     *
     * @param request Web request
     * @param model   Context to fill
     * @return a {@link java.lang.String} object.
     */
    @RequestMapping(value = {"/", "/**"}, method = RequestMethod.GET)
    public String getForm(HttpServletRequest request, Model model) {

        // Work out the themes

        model.addAttribute("Themes", ThemeManager.getThemes());

        // Get a list of the available tables

        List<String> tables = DataSourceUtils.getTables();
        List<String> tablesDefault = new ArrayList<>();
        tablesDefault.addAll(tables);
        tablesDefault.remove("log");
        tablesDefault.remove("user_status");
        model.addAttribute("Tables", tables);
        model.addAttribute("TablesDefault", tablesDefault);

        // Use the correct view

        if (request.getRequestURI().contains("/admin"))
            return "admin";
        else
            return request.getRequestURI();
    }

    /**
     * Handles storing the settings
     *
     * @param request Web request
     * @param model   Context to fill
     *
     * @return dummy string;
     */
    @RequestMapping(method = RequestMethod.POST)
    public String post(HttpServletRequest request, Model model) {

        Session session = HibernateUtils.getCurrentSession();
        try {

            // Get a list of the values before the change

            List<SettingsEntity> settings = HibernateUtils.selectEntities("from SettingsEntity ");
            List<String> values = new ArrayList<>();
            if (!Common.isBlank(settings)) {
                for (SettingsEntity setting : settings) {
                    values.add('@' + setting.getName() + ": value=" + setting.getValue() + ", valueNumeric=" + setting.getValueNumeric() + ", valueText=" + setting.getValueText());
                }
            }

            // Get a list of all the fields in the class that can possibly be
            // used for settings storage

            for (Map.Entry field : ClassUtils.getFields(HibernateUtils.class, "^SETTING_.+(?<!_DEFAULT)").entrySet()) {

                // If we haven't been sent the value, then reset property to default value
                String fieldValue = (String) field.getValue();
                String value = Common.join(request.getParameterValues(fieldValue),",");
                if (value != null) {
                    value = value.replaceAll(",visible","");
                    // Try and get the type of the  setting and then save the value sent to us

                    SettingsEntity setting = HibernateUtils.getSystemSetting(fieldValue);

                    updateSettingEntity(field, value, setting);
                    session.saveOrUpdate(setting);
                }
                else {
                    //Nothing was sent to us, reset setting to default value and save the value

                    String blankSetting = request.getParameter('_' + fieldValue);
                    SettingsEntity setting = HibernateUtils.getSystemSetting(fieldValue);
                    if (!Common.isBlank(blankSetting) && blankSetting.equalsIgnoreCase("visible")) {
                        updateSettingEntity(field, "false", setting);
                        session.saveOrUpdate(setting);
                    }
                    else if (request.getParameterMap().containsKey('_' + fieldValue)) {
                        updateSettingEntity(field, blankSetting, setting);
                        session.saveOrUpdate(setting);
                    }
                }

                // Clear out cache value

                CacheEngine.delete(fieldValue);
            }

            // Write out the proxy settings to a temp file

//            writeMappingProxyConfig();

            // Update the cache settings

            CacheAccessorFactory.updateSettings();

            // Add a change log entry

            HibernateUtils.addChangeLog("Settings", Common.join(values, "\n"), Common.isBlank(values) ? ChangeLogEntity.ChangeTypes.ADDED : ChangeLogEntity.ChangeTypes.EDITED);
        }
        catch (org.hibernate.exception.ConstraintViolationException e) {
            logger.error(ServletHelper.addError(model, e.getCause().getMessage().replaceAll("^[^:]+:", "")));
        }
        catch (Exception e) {
            logger.error(ServletHelper.addError(model, "Problem saving settings - %s", PivotalException.getErrorMessage(e)));
        }
        return "admin";
    }

    /**
     * update the SettingsEntity object value with the provided value or set it to default if no value is provided
     *
     * @param field   The setting field map entry
     * @param value   the value send to us
     * @param setting the SettingEntity object to be updated.
     */
    private void updateSettingEntity(Map.Entry field, String value, SettingsEntity setting) {
        Map<String, Object> defaults = ClassUtils.getFields(HibernateUtils.class, field.getKey() + "_DEFAULT");
        if (Common.isBlank(defaults))
                setting.setValue(value);
        else {
            Object defaultValue = defaults.values().toArray()[0];
            if (Boolean.class.isAssignableFrom(defaultValue.getClass()))
                setting.setValueNumeric(Common.isYes(value) ? 1 : 0);
            else if (Number.class.isAssignableFrom(defaultValue.getClass()))
                setting.setValueNumeric(Common.parseInt(value));
            else
                setting.setValue(value);
        }
    }

    /**
     * Starts a background database job
     * We can't use Jackson to do the JSON conversion here because the file upload will
     * cause a "save this file" action on IE so the data has to be returned as content
     * type text
     *
     * @param request Web request
     * @param action  Action to perform
     * @param tables  List of table names to limit the action to
     * @return a {@link java.lang.String} object.
     */
    @RequestMapping(value = "/job/start/{action}", method = RequestMethod.POST)
    @ResponseBody
    public String startJob(HttpServletRequest request, @PathVariable("action") String action, @RequestParam(value = "tables", required = false) String tables) {

        // Ok, we need to start a new dump job and return an ID for the
        // thread to the caller so that that they can test progress

        DatabaseJob job = new DatabaseJob(request, action, tables);
        request.getSession().setAttribute(job.getJobName(), job);
        job.start();
        return String.format("{\"id\":\"%s\"}", job.getJobName());
    }

    /**
     * Returns the status of a running job
     *
     * @param request Web request
     * @param id      a {@link java.lang.String} object.
     * @return a {@link com.pivotal.web.controllers.utils.JsonResponse} object.
     */
    @RequestMapping(value = "/job/check", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public JsonResponse checkJobProgress(HttpServletRequest request, @RequestParam(value = "id") String id) {
        JsonResponse returnValue = new JsonResponse();

        // Make sure we have a job identifier

        DatabaseJob job = (DatabaseJob) request.getSession().getAttribute(id);
        if (job == null) {
            returnValue.setError("The job does not exist");
            returnValue.setCompleted(true);
        }
        else {
            Progress progress = job.getProgress();
            returnValue.setCount(progress.getPercent());
            returnValue.setInformation(progress.getMessage());
            returnValue.setCompleted(progress.isFinished());
        }
        return returnValue;
    }

    /**
     * Kills a running job
     *
     * @param request Web request
     * @param id      a {@link java.lang.String} object.
     * @return a {@link com.pivotal.web.controllers.utils.JsonResponse} object.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @RequestMapping(value = "/job/kill", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public JsonResponse killJob(HttpServletRequest request, @RequestParam(value = "id") String id) {
        JsonResponse returnValue = new JsonResponse();

        // Make sure we have a job identifier

        DatabaseJob job = (DatabaseJob) request.getSession().getAttribute(id);
        if (job == null)
            returnValue.setError("The job does not exist");
        else {
            if (job.isAlive()) Common.stopThread(job);
            if (job.getFile().exists()) job.getFile().delete();
            request.getSession().removeAttribute(job.getJobName());
        }
        return returnValue;
    }

    /**
     * Downloads the result of a dump job
     *
     * @param request  Web request
     * @param response Response stream to use
     * @param id       a {@link java.lang.String} object.
     * @return a {@link com.pivotal.web.controllers.utils.JsonResponse} object.
     */
    @RequestMapping(value = "/job/download", method = RequestMethod.GET)
    @ResponseBody
    public JsonResponse downloadDatabaseDump(HttpServletRequest request, HttpServletResponse response, @RequestParam(value = "id") String id) {

        DatabaseJob job = (DatabaseJob) request.getSession().getAttribute(id);
        if (job == null) throw new PivotalException("The job does not exist");
        if (!job.getProgress().isFinished())
            throw new PivotalException("The job has not finished yet");

        String fileExt = ServletHelper.getParameter("fileext","sql");
        String filenameStart = Common.getAplicationName() + '-' + ServletHelper.getParameter("filename",Constants.APPLICATION_VERSION);

        // Set the disposition

        String filename = filenameStart + "." + fileExt + (job.compress ? ".gz" : "");
        ServletContext context = request.getSession().getServletContext();
        response.setContentType(context.getMimeType(filename));
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + '"');
        response.setHeader("Content-Description", filename);

        // Send the file direct from the job

        try {
            Common.pipeInputToOutputStream(job.getFileStream(), response.getOutputStream());
        }
        catch (Exception e) {
            logger.error("Problem dumping database - {}", PivotalException.getErrorMessage(e));
        }
        return null;
    }

    /**
     * Truncates the log table
     *
     * @param request Web request
     * @return a {@link com.pivotal.web.controllers.utils.JsonResponse} object.
     */
    @RequestMapping(value = "/logs/truncate", method = RequestMethod.GET)
    @ResponseBody
    public JsonResponse truncateLogs(HttpServletRequest request) {
        JsonResponse returnValue = new JsonResponse();

        Date now = new Date();
        logger.info("Truncated log table");
        Database db = new DatabaseHibernate();
        db.execute("truncate table log");
        if (db.isInError())
            returnValue.setError(db.getLastError());
        else
            LogEntity.addLogEntry(LogEntity.STATUS_TRUNCATED_LOGS, "Truncated the logs", now, request);
        db.close();

        return returnValue;
    }

    /**
     * A simple thread object used to run the database action in the background
     */
    static class DatabaseJob extends Thread implements Serializable {

        private static final long serialVersionUID = 4175079433795297626L;
        private Progress progress = new Progress();
        private String tables;
        private String name;
        private File tmpFile;
        private String username = String.format("%s (%s)", UserManager.getCurrentUser().getName(), UserManager.getCurrentUser().getEmail());
        private String jobType;
        private String contentType;
        private boolean compress;

        /**
         * Creates a background job to carry out the required action on the database
         *
         * @param request Request object used to initiate the action
         * @param jobType The type of action to carry out
         */
        DatabaseJob(HttpServletRequest request, String jobType) {
            this(request, jobType, null);
        }

        /**
         * Creates a background job to carry out the required action on the database
         *
         * @param request Request object used to initiate the action
         * @param jobType The type of action to carry out
         * @param tables  List of tables to limit job to
         */
        DatabaseJob(HttpServletRequest request, String jobType, String tables) {

            // Create a temporary file to use

            compress = ServletHelper.parameterExists(request, "compress");
            String fileExt = ServletHelper.getParameter("fileext","sql");
            this.tables = tables;

            // If this is a reload job then we need to copy the uploaded file

            if (Common.doStringsMatch(jobType, "reload") || Common.doStringsMatch(jobType, "loadworkflow")) {
                MultipartFile filePart = ((MultipartRequest) request).getFile("file");
                contentType = filePart.getContentType();
                logger.info("Uploaded a file of type [{}]", contentType);
                if (Common.doStringsMatch(contentType, "application/x-gzip", "application/gzip")) {
                    tmpFile = new File(Common.getTemporaryFilename(fileExt + ".gz"));
                }
                else {
                    tmpFile = new File(Common.getTemporaryFilename(fileExt));
                }
                try {
                    filePart.transferTo(tmpFile);
                }
                catch (Exception e) {
                    logger.error("Problem uploading SQL file - {}", PivotalException.getErrorMessage(e));
                }
            }
            else {
                tmpFile = new File(Common.getTemporaryFilename(fileExt + (compress ? ".gz" : "")));
            }
            name = jobType + new Date().getTime();
            this.jobType = jobType;
        }

        /**
         * Actually run the database action with progress
         */
        public void run() {
            try {
                if (Common.doStringsMatch(jobType, "download")) {
                    String header = String.format("/*\nDumped %s database to %s\nDate: %s\nBy: %s\nFrom: %s\n*/\n",
                            Common.getAplicationName(),
                            tmpFile.getName(),
                            Common.dateFormat(new Date(), "EEEE, dd MMM yyyy HH:mm:ss"),
                            username,
                            ServletHelper.getAppIdentity());

                    // Work out the tables to ignore

                    List<String> tablesToIgnore = Common.splitToList("log,user_status", ",");
                    if (!Common.isBlank(tables)) {
                        tablesToIgnore = DataSourceUtils.getTables();
                        for (String table : Common.splitToList(tables, ",")) {
                            tablesToIgnore.remove(table);
                        }
                    }
                    DataSourceUtils.dumpDatabase(tmpFile, true, false, header, progress, tablesToIgnore, compress);
                }
                else if (Common.doStringsMatch(jobType, "reload")) {
                    DataSourceUtils.reloadDatabase(tmpFile, progress, contentType);
                    clearStats();
                }
                else if (Common.doStringsMatch(jobType, "clear")) {
                    DataSourceUtils.clearDatabase(progress);
                    clearStats();
                }
                else if (Common.doStringsMatch(jobType, "applytestdata")) {
                    DataSourceUtils.applyTestDataToDatabase(progress);
                    clearStats();
                }
                else if (Common.doStringsMatch(jobType, "applysettingsdata")) {
                    DataSourceUtils.applySettingsDataToDatabase(progress);
                    clearStats();
                }
                else if (Common.doStringsMatch(jobType, "dumpworkflow")) {
                    DataSourceUtils.dumpWorkflow(tmpFile,  HibernateUtils.getSystemSetting(HibernateUtils.SETTING_BACKUP_WORKFLOW_SETTINGS, ""), progress);
                    clearStats();
                }
                else if (Common.doStringsMatch(jobType, "loadworkflow")) {
                    DataSourceUtils.loadWorkflow(tmpFile, progress, contentType);
                    clearStats();
                }
            }
            catch (Exception e) {
                logger.error(progress.getMessage());
                progress.setMessage("Problem running " + Common.getAplicationName() + " dump/reload - " + PivotalException.getErrorMessage(e));
            }
            progress.setFinished(true);
        }

        /**
         * Clears the data caches and re-indexes the database
         */
        private static void clearStats() {
            HibernateUtils.clearCache();
        }

        /**
         * Returns a stream for the download file
         *
         * @return File input stream
         *
         * @throws Exception Errors if the stream cannot be opened
         */
        InputStream getFileStream() throws Exception {
            if (progress.isFinished()) {
                tmpFile.deleteOnExit();
                return new BufferedInputStream(new FileInputStream(tmpFile));
            }
            else
                throw new PivotalException("Dump has not yet completed");
        }

        /**
         * Returns a File for the download file
         *
         * @return File input stream
         */
        File getFile() {
            return tmpFile;
        }

        /**
         * Returns the progress object
         *
         * @return Progress object
         */
        Progress getProgress() {
            return progress;
        }

        /**
         * Returns the name of this job
         *
         * @return Unique name of this job
         */
        String getJobName() {
            return name;
        }
    }

    /**
     * Returns the full path of the mapping proxy.config file
     */
    private static String getMappingProxyConfigFilename() {

        return Common.getTemporaryDirectory() + "/proxy.config";

    }

    /**
     * Returns Json containing the list of dates
     *
     * @return JSON containing result
     */
    @RequestMapping(value = {"/loadholidays"}, method = RequestMethod.GET)
    @ResponseBody
    public JsonResponse getHolidays() {

        JsonResponse result = new JsonResponse();

        try {
            String dates = getHolidayDates();
            result.setInformation(dates);
        }
        catch(PivotalException e) {
            result.setError(PivotalException.getErrorMessage(e));
        }

        return result;
    }

    /**
     * Returns a \n delimited list of dates
     *
     * @return delimited string of holiday dates
     *
     * @throws PivotalException if problem
     */
    public static String getHolidayDates() throws PivotalException {

        List<String>dates = new ArrayList<>();
        String error=null;

        logger.debug("Starting reading of holidays");

        String url = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_APP_HOLIDAY_URL, "");

        if (Common.isBlank(url)) {
            error = I18n.getString("system.setting.app.holiday.error.no_url");
        }
        else {
            try {
                HttpUtils.HttpContent data = Common.getUrl(url, 30000);

                if (Common.isBlank(data)) {
                    error = I18n.getString("system.setting.app.holiday.error.no_data") + " " + url;
                }
                else if(!data.getContent().startsWith("BEGIN:VCALENDAR")) {
                    error = I18n.getString("system.setting.app.holiday.error.invalid_data") + " " + url;
                }
                else {

                    // Process the ICS file we are expecting from gov.uk
                    // Dissect the information and retrieve the dates

                    String[] lines = data.getContent().split("[\r\n]+");
                    for (String thisLine : lines) {
                        if (thisLine.startsWith("DTSTART;VALUE=DATE:")) {
                            String thisDate = thisLine.substring(19);
                            if (!Common.isBlank(thisDate)) {
                                logger.debug("Loading " + thisDate);
                                dates.add(Common.formatDate(thisDate, "dd MMMM yyyy"));
                            }
                        }
                    }
                }
            }
            catch(Exception e) {
                error = "Error getting dates data " + PivotalException.getErrorMessage(e);
            }
        }

        if (!Common.isBlank(error))
            throw new PivotalException(error);

        logger.debug("Finished reading of holidays");

        return Common.join(dates, "\n");
    }

    /**
     * Store the workflow backup settings
     * @param settings settings to store
     * @return JSON containing result
     */
    @RequestMapping(value = {"/saveworkflowsettings"}, method = RequestMethod.POST)
    @ResponseBody
    public JsonResponse saveWorkflowSettings(@RequestParam("settings") String settings) {

        JsonResponse result = new JsonResponse();

        try {
            SettingsEntity settingsEntity = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_BACKUP_WORKFLOW_SETTINGS);
            settingsEntity.setValue(settings);
            HibernateUtils.save(settingsEntity);
        }
        catch(PivotalException e) {
            result.setError(PivotalException.getErrorMessage(e));
        }

        return result;
    }
}
