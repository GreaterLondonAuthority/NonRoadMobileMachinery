/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers;

import com.pivotal.reporting.reports.Report;
import com.pivotal.reporting.reports.ReportFactory;
import com.pivotal.reporting.reports.RuntimeParameter;
import com.pivotal.system.hibernate.entities.DatasourceEntity;
import com.pivotal.system.hibernate.entities.ScheduledTaskEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.*;
import com.pivotal.utils.workflow.WorkflowHelper;
import com.pivotal.web.controllers.utils.Authorise;
import com.pivotal.web.controllers.utils.JsonResponse;
import com.pivotal.web.servlet.ServletHelper;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.*;

/**
 * <p>The {@code DashboardsController} is responsible for handling the creation and modification of dashboards by handling
 * the configuration of layouts/widgets and the relationship between them (for example, a dashboard has a layout which has
 * widgets and they all have configuration).</p>
 *
* @since 1.0
 */
@Authorise
@Controller
@RequestMapping("/dashboard")
public class DashboardController extends AbstractController {

    // Get access to the logger
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DashboardController.class);
    public static final String PAGE_NAME = "dashboard";
    /**
     * This is the entry path for displaying the dashboards page. The user is able to choose their dashboard to view
     * by passing along the dashboard identifier on the query string. This will be then saved to their user preferences,
     * meaning that it will be shown (by default) on their next visit to the dashboards page.
     *
     * @param model The model that will be injected into the view
     * @return The path to the template to use
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String getDashboardView(Model model) {

        JsonResponse workFlowResult = WorkflowHelper.executeWorkflow("CHOOSE_USER_DASHBOARD", model.asMap(), false);

        model.addAttribute("WorkflowResult", workFlowResult);

        return PAGE_NAME;
    }

    @RequestMapping(value = "/register/{type}", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String getRegister(Model model, @PathVariable(value = "type") String type) {

        model.addAttribute("RequestedPage", "register");
        model.addAttribute("RequestedType", type);

        return getDashboardView(model);
    }


    @RequestMapping(value = "/register/export", method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE)
    public void export(HttpServletResponse response
            , @RequestParam(value = "name") String name
            , @RequestParam(value = "task") String taskName
            , @RequestParam(value = "ext") String extension
            , @RequestParam(value = "where", required = false) String where
    ) {

        ScheduledTaskEntity taskEntity = HibernateUtils.getEntity(ScheduledTaskEntity.class, taskName);

        if (!Common.isBlank(taskEntity)) {
            Report report = ReportFactory.getReport(taskEntity.getReport());

            // Pass the parameters into the context

            List<RuntimeParameter> reportParams = new ArrayList<>();
            reportParams.add(new RuntimeParameter("whereClause", StringEscapeUtils.unescapeHtml(where)));
            report.setParameters(reportParams);

            try {
                report.setDatasource(HibernateUtils.getEntity(DatasourceEntity.class, DatasourceEntity.SYSTEM_DATASOURCE_INTERNAL), null, null, null, null);
            }
            catch (Exception e) {
                logger.debug("Error getting report datasource " + PivotalException.getErrorMessage(e));
            }

            String filename = Common.getTemporaryFilename(extension);
            report.export(filename, taskEntity.getExportFormat(), null);

            try {
                File result = new File(filename);
                if (result.exists()) {
                    response.setContentType(ServletHelper.getServletContext().getMimeType("test." + extension));
                    response.setHeader("Content-Disposition", "attachment; filename=\"" + name + "_export." + extension + '"');
                    response.setHeader("Content-Description", filename);
                    logger.debug("Sending file output " + name + "_export." + extension);
                    Common.pipeInputToOutputStream(result, response.getOutputStream());
                }
                else
                    logger.debug("Unable to create file when exporting advanced search results");
            }
            catch (Exception e) {
                logger.debug("Error exporting advanced search results " + PivotalException.getErrorMessage(e));
            }
        }
        else
            logger.debug("Unable to get task for name " + taskName);
    }
}
