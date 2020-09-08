/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers;

import com.pivotal.system.hibernate.entities.LogEntity;
import com.pivotal.system.hibernate.entities.MediaEntity;
import com.pivotal.system.hibernate.entities.ReportEntity;
import com.pivotal.system.hibernate.entities.ScheduledTaskEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.security.CaseManager;
import com.pivotal.utils.Common;
import com.pivotal.utils.ExecutionResults;
import com.pivotal.utils.I18n;
import com.pivotal.utils.PivotalException;
import com.pivotal.web.controllers.utils.Authorise;
import com.pivotal.web.servlet.ServletHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.sql.Timestamp;
import java.util.*;

/**
 * Handles requests for storing and managing reports
 */
@Authorise
@Controller
@RequestMapping(value = {"/admin/report"})
public class ReportController extends AbstractAdminController {

    /**
     * The the type in the media table for report outputs
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ReportController.class);

    /** {@inheritDoc} */
    @Override
    public Class getEntityClass() {
        return ReportEntity.class;
    }

    /**
     * Return a saved report from the media table
     *
     * @param response Response to send the string back to
     * @param id       Id of the media entity
     */
    @RequestMapping(value = "saved/{id}", method = RequestMethod.GET)
    public void savedReport(HttpServletResponse response, @PathVariable int id) {

        MediaEntity entity = HibernateUtils.getEntity(MediaEntity.class, id);
        if (entity != null) {
            if (entity.getType().equals(MediaEntity.TYPE_CODE_REPORT_OUTPUT_TYPE)) {
                try {
                    Common.pipeInputToOutputStream(CaseManager.getMediaFile(entity), response.getOutputStream());
                }
                catch (Exception e) {
                    logger.error("Problem sending definition - {}", PivotalException.getErrorMessage(e));
                }
            }
        }
    }

    /**
     * Returns the definition for the specified report
     *
     * @param response Response to send the string back to
     * @param id ID of the report
     */
    @RequestMapping(value = "content", method = RequestMethod.GET)
    public void editReport(HttpServletResponse response, @RequestParam(value = "id") int id) {

        ReportEntity report = HibernateUtils.getEntity(ReportEntity.class, id);
        response.setContentType("text/html; charset=utf-8");
        try {
            if (report!=null) {
                response.getWriter().print(report.getFileString());
            }
            else {
                response.getWriter().print("");
            }
        }
        catch (Exception e) {
            logger.error("Problem sending definition - {}", PivotalException.getErrorMessage(e));
        }
    }

    /**
     * Returns the definition for the specified report
     *
     * @param model Model to use to carry the report info
     * @param id ID of the report
     * @param readOnlyDef Should the definition be displayed in read only mode?
     * @return a {@link java.lang.String} object.
     */
    @RequestMapping(value = "definition", method = RequestMethod.GET)
    public String editReportDefinition(Model model, @RequestParam(value = "id") int id, @RequestParam(value = "read_only", required = false) Boolean readOnlyDef) {
        ReportEntity report = HibernateUtils.getEntity(ReportEntity.class, id);
        model.addAttribute("ReadOnly", readOnlyDef != null ? readOnlyDef : false);
        if (report!=null) {
            model.addAttribute("Content", report.getFileString());
            model.addAttribute("ContentId", report.getId());
            model.addAttribute("ContentType", report.getType());
        }
        return "/media/editor";
    }

    /**
     * Returns the definition for the specified report using the task ID
     *
     * @param model Model to use to carry the report info
     * @param taskId ID of the task
     * @return a {@link java.lang.String} object.
     */
    @RequestMapping(value = "taskreport", method = RequestMethod.GET)
    public String editTaskReport(Model model, @RequestParam(value = "id") int taskId) {
        ScheduledTaskEntity task = HibernateUtils.getEntity(ScheduledTaskEntity.class, taskId);
        if (task!=null) {
            model.addAttribute("Content", task.getReport().getFileString());
            model.addAttribute("ContentId", task.getReport().getId());
            model.addAttribute("ContentType", task.getReport().getType());
        }
        return "/media/editor";
    }

    /** {@inheritDoc} */
    @Override
    protected void beforeValidation(HttpSession session, HttpServletRequest request, Object entity) {
        ((ReportEntity) entity).setTimeModified(new Timestamp(new Date().getTime()));
    }

    /**
     * Load report ModalWindow to show log of the task
     *
     * @param model Context to fill
     * @param id    id of the task
     * @return log table path
     */
    @RequestMapping(value = "/log")
    public String getReportLog(Model model, @RequestParam(value = "id") Integer id) {
        model.addAttribute("id", id);
        model.addAttribute("RefreshPeriod", 60 * 1000);
        return "admin/report-table-log";
    }

    /**
     * Returns a list of all log entries for reports
     * @param model Context to fill
     * @param id    id of the Log
     * @return log table path
     */
    @RequestMapping(value = "/log/data", method = RequestMethod.GET)
    @ResponseBody
    public List<Map<String, Object>> getReportLogData(Model model, @RequestParam(value = "id") Integer id) {

        List<LogEntity> reports = HibernateUtils.selectEntities("from LogEntity where task_id = '" + id + "' and (status='ReportPublished' or status='SystemError') order by date_added desc");
        List<Map<String, Object>> results = new ArrayList<>();

        for (LogEntity log : reports) {
            Map<String, Object> result = new HashMap<>();

            result.put("id", log.getId());
            result.put("addDate", Common.formatDate(log.getDateAdded(), "yyyy-MM-dd HH:mm:ss"));
            result.put("reportName", I18n.getString(log.getReportName()));
            result.put("taskName", log.getTaskName());
            result.put("status", log.getStatus());
            result.put("recipents", log.getRecipients());
            result.put("dutation", log.getDuration());
            result.put("view", "");

            results.add(result);
        }
        return results;
    }

    /**
     * Run the task and create create a pdf file to download to the server
     *
     * @param request  The current request
     * @param response The response object for this request
     * @param id       The identifier of the dashboard for the current user
     */
    @ResponseBody
    @RequestMapping(value = "/launch", method = RequestMethod.GET)
    public void viewReport(HttpServletRequest request, HttpServletResponse response, @RequestParam("id") Integer id) {

        ExecutionResults results=new ExecutionResults(request);
        ScheduledTaskEntity taskToRun = HibernateUtils.getEntity(ScheduledTaskEntity.class, id);
        try {

            // Check to see if there is anything to do

            if (taskToRun==null) {
                logger.error("A call has been made for an unknown task [{}]", id);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, String.format("Unknown task [%d]", id));
            }
            else {

                // Get a map of all the useful parameters

                logger.debug("Adding useful values to the context");
                Map<String, Object> contextParams= ServletHelper.getGenericObjects(request, response, false);

                // Try and prevent caching - particularly in IE

                response.setHeader("Pragma","No-cache");
                response.setHeader("Cache-Control","no-cache");
                response.setDateHeader("Expires", 0);

                // Run the task

//                WebServiceUtils.runTask(taskToRun, contextParams, results);

                // If we have committed the response, then skip the rest because the likelihood
                // is that we have redirected or sent our own stuff

                if (!response.isCommitted()) {
                    ServletContext context = request.getSession().getServletContext();
                    response.setContentType(context.getMimeType(results.getWebserviceReportContentFile().getName()));
                    Common.pipeInputToOutputStream(results.getWebserviceReportContentFile(), response.getOutputStream());
                }
            }
        }
        catch (Exception e) {
            logger.error("Problem sending results to the browser [{}] - {}", id, PivotalException.getErrorMessage(e));
        }

        // Remove any temporary file associated with this request

        finally {
            if (!Common.isBlank(results.getWebserviceReportContentFile())) {
                results.getWebserviceReportContentFile().delete();
            }
        }
    }
}
