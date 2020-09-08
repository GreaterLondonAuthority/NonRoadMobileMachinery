/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.reporting.reports;

import com.google.common.collect.Lists;
import com.pivotal.monitoring.utils.Definition;
import com.pivotal.reporting.publishing.Recipient;
import com.pivotal.reporting.scheduler.Job;
import com.pivotal.system.hibernate.entities.ReportEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.security.CaseManager;
import com.pivotal.system.security.UserManager;
import com.pivotal.utils.*;
import com.pivotal.utils.browser.Browser;
import com.pivotal.utils.browser.Configuration;
import com.pivotal.utils.workflow.WorkflowHelper;
import com.pivotal.web.Constants;
import com.pivotal.web.controllers.utils.JsonResponse;
import com.pivotal.web.email.EmailManager;
import com.pivotal.web.servlet.ServletHelper;
import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.slf4j.LoggerFactory;
import org.zefer.pd4ml.PD4Constants;
import org.zefer.pd4ml.PD4ML;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This class implements a report type that creates HTML and Text
 * (primarily, but does also support other formats too) from
 * a velocity template
 * The file is parsed by velocity to collect the list of parameters and then
 * parsed again at runtime to replace the parameters within the script
 * which is then run to produce the output
 * The velocity context is augmented with useful objects to allow databases
 * to be interrogated etc - see Transformer for more details
 *
 * An example of a Velocity report;
 *
 * <pre>
 *
 *     ## Declare some parameters
 *
 *     $ReportHelper.declareParameter("Start", "Start Date", "The start date of this report", $utils.dateFormat(Utils.Now, "yyyy-MM-dd"), false)
 *     $ReportHelper.declareParameter("GroupSize", "Minimum Group Size", "Only include groups with a minimum of this number of records within them", "10", true)
 *
 *     &lt;html&gt;
 *         &lt;body&gt;
 *             &lt;b&gt;Stores with Over $GroupSize Messages after $Start&lt;/b&gt;
 *
 *             #set ($DataList = $DB.find("select count(*) as MessageCount, store_id from process where t_time&gt;='$StartDate' group by store_id having MessageCount &gt;= $GroupSize;"))
 *             $Logger.info("Found $DataList.size() stores to work on")
 *             &lt;table&gt;
 *                 &lt;tr&gt;
 *                     &lt;th&gt;Store ID&lt;/th&gt;
 *                     &lt;th&gt;Count&lt;/th&gt;
 *                 &lt;/tr&gt;
 *                 #foreach ($Store in $DataList)
 *                     &lt;tr&gt;
 *                         &lt;td&gt;$Store.store_id&lt;/td&gt;
 *                         &lt;td&gt;$Store.MessageCount&lt;/td&gt;
 *                     &lt;/tr&gt;
 *                 #end
 *             &lt;/table&gt;
 *         &lt;/body&gt;
 *     &lt;/html&gt;
 *
 * </pre>
 *
 * For more information about the syntax and capabilities of velocity http://velocity.apache.org/engine/releases/velocity-1.7
 */
public class VelocityReport extends Report {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(VelocityReport.class);

    private String reportText;
    private Template template;

    /**
     * Opens the report, parses it and determines the list of parameters
     * declared within it
     *
     * @param report Report object to interrogate
     */
    public VelocityReport(ReportEntity report) {

        // Save the report entity for later

        this.report = report;

        // Get a local copy of the report script so that it doesn't
        // change in between parsing and executing

        initReport(getScript(report, this));

        // Try and use the cache to get the template first

        VelocityEngine engine;
        try {
            engine = VelocityUtils.getEngine();
            if(report.getId()!=null) {
                template = engine.getTemplate("/database/report:" + report.getId());
            }
        } catch (Exception e) {
            logger.warn("Cannot retrieve template using resource loader - will continue with StringTemplate");
        }
    }

    /**
     * Opens the report, parses it and determines the list of parameters
     * declared within it
     *
     * @param reportText Report text to use
     */
    public VelocityReport(String reportText) {
        initReport(reportText);
    }

    /**
     * Opens the report, parses it and determines the list of parameters
     * declared within it
     *
     * @param reportText Report text to use
     */
    private void initReport(String reportText) {

        // Get a local copy of the report script so that it doesn't
        // change in between parsing and executing

        this.reportText = reportText;

        // Create a helper object to get the parameters

        ReportHelper reportHelper = new ReportHelper();

        // Parse the script to get the parameters

        getParameters(reportHelper);

        // Get any report parameters that were declared in the script

        reportParams = reportHelper.getParameters();
    }

    /**
     * {@inheritDoc}
     *
     * This method is called at runtime to provide the values to be used
     * within this instance of the report
     * All we do is put the values into a velocity context and parse
     * the report command to create clean SQL that we will then run
     */
    public void setParameters(List<RuntimeParameter> parameters) {

        // Store the runtime parameters

        runtimeParams = parameters;
    }

    /**
     * Return the map of report parameters
     *
     * @return Map of parameters keyed on the parameter name
     */
    public Definition getReportParameters() {
        return reportParams;
    }

    /**
     * Return the map of runtime parameters
     *
     * @return Map of runtime parameters keyed on the parameter name
     */
    public List<RuntimeParameter> getParameters() {
        return runtimeParams;
    }

    /**
     * Returns a list of all the supported export types
     *
     * @return List of types
     */
    public List<ExportFormat> getSupportedExportTypes() {
        return Lists.newArrayList(ExportFormat.PDF,ExportFormat.HTML,ExportFormat.TEXT,ExportFormat.XML,ExportFormat.JSON);
    }

    /**
     * Closes and disposes of the resources used
     * The dispose has the effect of actually disconnecting from the data source
     * whereas the close() simply garbage collects the report memory
     */
    public void close() {
    }

    /**
     * {@inheritDoc}
     *
     * This method exports the report to the Response stream with a flag to
     * indicate if it should be sent as an attachment
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    protected void exportReport(String filename, ExportFormat format, Job job, String compression, Recipient recipient, ExecutionResults executionResults) {

        // Parse the report into a text string

        String output = runReport(job, recipient, executionResults);

        // Select the output format we want to work with - default to HTML

        if (format.equals(ExportFormat.TEXT)) {
            Common.writeTextFile(filename, Common.getCleanHtml(output), compression);
            logger.debug("Outputting to text file {}", filename);
        }

        // If this is PDF then we need to do some groovy stuff with the
        // PhantomJS and PD4ML libraries

        else if (format.equals(ExportFormat.PDF)) {

            // Copy the HTML to a temporary file

            File tmpHtml = Common.getTemporaryFile("html");
            Common.writeTextFile(tmpHtml, output);

            // Run the file through the browser to exercise it using a special
            // URL that allows us to get the report via our server

            Configuration config = new Configuration();
            config.setUrl(Constants.getLocalAddress() +  ServletHelper.getServletContext().getContextPath() + "/media/stream/tmp?f=" + Common.encodeURL(tmpHtml.getName()));
            com.pivotal.utils.browser.ExportFormat exportFormat = new com.pivotal.utils.browser.ExportFormat();
            exportFormat.setFormat(com.pivotal.utils.browser.ExportFormat.Format.GIF);

            // Add headers to tell NRMM that we are logged in

            if(UserManager.getCurrentUser()!= null) {
                config.addCustomHeader(Constants.INTERNAL_REQUEST_USER_ID, UserManager.getCurrentUser().getId() + "");
            }
            config.addCustomHeader(Constants.INTERNAL_REQUEST_TOKEN, Constants.getLoopbackToken());

            // Run the export

            try {
                List<File> files = Browser.exportHtml(config, exportFormat);
                if (Common.isBlank(files))
                    executionResults.setError("Cannot browse the URL");
                else {

                    // Setup the PD4ML library

                    PD4ML pd4ml = new PD4ML();
                    pd4ml.setHtmlWidth(1300);
                    pd4ml.generateOutlines(true);
                    if (logger.isDebugEnabled()) {
                        pd4ml.enableDebugInfo();
                    }
                    pd4ml.setPageSize(PD4Constants.A4);
                    pd4ml.setPageInsetsMM(new Insets(10, 10, 10, 10));
                    pd4ml.addStyle("BODY {margin: 0}", true);

                    // The first file in the list contains the HTML, the rest are the SVG images

                    String tmpText = Common.readTextFile(files.get(0));
                    try (OutputStream out = new FileOutputStream(filename)) {
                        pd4ml.render(new StringReader(tmpText), out, new URL(Constants.getLocalAddress()));
                        logger.debug("PDF Export Completed successfully");
                    }
                    catch (Exception e) {
                        logger.error("Cannot output from PD4ML - {}", PivotalException.getErrorMessage(e));
                    }
                }
            }
            catch (Exception e) {
                logger.error("Cannot export from Browser - {}", PivotalException.getErrorMessage(e));
            }

            // Clean up the temporary files

            if (tmpHtml.exists())
                tmpHtml.delete();
        }

        // Use default for everything else

        else {
            Common.writeTextFile(filename, output, compression);
            logger.debug("Outputting to {} file {}", format.toString(), filename);
        }
    }

    /**
     * This method runs the report by parsing to with velocity to produce
     * the string output
     *
     * @param job              Schedule job
     * @param recipient        Recipient if known
     * @param executionResults Execution messages object
     * @return Parsed report
     */
    private String runReport(Job job, Recipient recipient, ExecutionResults executionResults) {

        // Parse the report into a text string

        String returnValue = null;
        ReportHelper helper = new ReportHelper(dataSourceEntity);
        Writer output = new StringWriter();
        VelocityEngine engine;
        try {
            // Now get a destination database
            // Open a connection to the database(s)

            openDatasources();
            logger.debug("Running report {} using database {}", report == null ? "[embedded]" : report.getName(), dataSourceEntity.getName());

            // Now get the transformation object to use
            // If we have specified a file to use, then use that otherwise use
            // the script contained in the record

            // Initialise the velocity environment

            logger.debug("Initialising the velocity engine");
            engine = VelocityUtils.getEngine();

            // Now add the useful stuff to the context

            logger.debug("Creating velocity context");
            Context context = VelocityUtils.getVelocityContext();

            // Now add the useful objects

            context.put("Recipient", recipient);
            context.put("Source", dataSource);
            context.put("Source1", dataSource1);
            context.put("Source2", dataSource2);
            context.put("Source3", dataSource3);
            context.put("Source4", dataSource4);
            context.put("ReportHelper", helper);
            context.put("HibernateUtils", HibernateUtils.class);
            context.put("AppPath",Constants.getLocalAddress());
            context.put("Utils", Common.class);
            context.put("I18n", I18n.class);
            context.put("Constants", Constants.class);
            context.put("Out", System.out);
            context.put("Job", job);
            context.put("ExecutionResults", executionResults);
            context.put("InRunMode", true);
            context.put("CaseManager", CaseManager.class);
            context.put("WorkflowResult", new JsonResponse());
            context.put("Engine", engine);
            context.put("engine", engine);
            context.put("LookupHelper", LookupHelper.class);
            context.put("WorkflowHelper", WorkflowHelper.class);
            context.put("EmailManager", EmailManager.class);
            context.put("Context", context);
            context.put("context", context);

            // Add a logger that is categorised by this class name and the report name

            if (report != null) {
                context.put("Logger", LoggerFactory.getLogger(VelocityReport.class.getName() + '.' + report.getName().replaceAll("\\s", "")));
                context.put("logger", LoggerFactory.getLogger(VelocityReport.class.getName() + '.' + report.getName().replaceAll("\\s", "")));
            }
            else {
                context.put("Logger", logger);
                context.put("logger", logger);
            }

            // Add the runtime parameters to the context if there are any

            if (!Common.isBlank(runtimeParams)) {
                for (RuntimeParameter param : runtimeParams) {
                    context.put(param.getName(), param.getValue());
                }
            }

            // Carry out the transformation using the script provided
            // If we have a cached template, then use that otherwise do it manually

//            if (template != null && !overrideInOperation)
//                template.merge(context, output);
//            else
                engine.evaluate(context, output, getClass().getSimpleName(), reportText);

            // See if there are any progression instructions

//            WorkflowHelper.processOutput(context, output);

        } catch (Throwable e) {
            logger.error("Problem running report - {}", PivotalException.getErrorMessage(e));
            throw new PivotalException(e);
        }

        // Cleanup our connections etc

        finally {
            closeDatasources();
            try {
                output.close();
                returnValue = output.toString();
            } catch (Exception e) {
                logger.error("Problem closing writer - {}", PivotalException.getErrorMessage(e));
            }
        }
        return returnValue;
    }

    /**
     * Parses the command script and populates the report parameters
     * This method will throw an exception if it encounters an issue
     * with the parsing i.e. incorrect syntax etc
     *
     * @param reportHelper Report helper object to use
     * @return String output of parsing
     */
    private String getParameters(ReportHelper reportHelper) {
        Writer output = new StringWriter();
        VelocityEngine engine;
        try {

            // We only need to check to see if parameters are being set from the report if they contain
            // the parameter declaration method

            if (reportText.contains("$ReportHelper.declareParameter")) {

                // Get a velocity engine to use

                engine = VelocityUtils.getEngine();

                // Now add the useful stuff to the context

                logger.debug("Creating velocity context");
                Context context = VelocityUtils.getVelocityContext();

                // Parse the SQL statements with the ReportHelper object to allow
                // it to collect all the possible parameters

                context.put("ReportHelper", reportHelper);
                context.put("InRunMode", false);

                // Run the evaluation of the script - this will exercise any embedded calls to
                // the ReportHelper that declare parameters

                engine.evaluate(context, output, getClass().getSimpleName(), reportText);
            }

            // Find the number of data sources used by the script by parsing it carefully

            for (int i = 1; i < 4; i++) {
                if (Pattern.compile("(?ms)\\$Source" + i + "[^a-zA-Z0-9]").matcher(reportText).find())
                    numberOfRequiredDatasources++;
            }
        } catch (Exception e) {
            String error = "Problem parsing recipient variables\n    " + reportText + " - " + PivotalException.getErrorMessage(e);
            logger.error(error);
            throw new PivotalException(error);
        }
        return output.toString().trim();
    }

}
