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
import com.pivotal.utils.*;
import com.pivotal.web.Constants;
import org.python.core.PyException;
import org.python.util.PythonInterpreter;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This class implements a report type that creates HTML and Text
 * (primarily, but does also support other formats too) from
 * a python script
 *
 * For more information about the syntax and capabilities of python http://www.jython.org/
 */
public class PythonReport extends Report {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PythonReport.class);

    private String reportText;

    /**
     * Opens the report, parses it and determines the list of parameters
     * declared within it
     *
     * @param report Report object to interrogate
     * @throws java.io.UnsupportedEncodingException If the bytes cannot be converted to UTF-8
     */
    public PythonReport(ReportEntity report) throws UnsupportedEncodingException {

        // Get a local copy of the report script so that it doesn't
        // change in between parsing and executing

        this.report=report;
        reportText=getScript(report);

        // Create a helper object to get the parameters

        ReportHelper reportHelper=new ReportHelper();

        // Parse the script to get the number of parameters etc

        getParameters(reportHelper);

        // Get any report parameters that were declared in the script

        reportParams=reportHelper.getParameters();

        // Find the number of data sources used by the script by parsing it carefully

        for (int i=1; i<4; i++) {
            if (Pattern.compile("(?ms)\\Source" + i + "\\.[a-zA-Z]+").matcher(reportText).find())
                numberOfRequiredDatasources++;
        }
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
     *
     * Returns a list of all the supported export types
     *
     * @return List of types
     */
    public List<ExportFormat> getSupportedExportTypes() {
        return Lists.newArrayList(ExportFormat.HTML, ExportFormat.TEXT);
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
    protected void exportReport(String filename, ExportFormat format, Job job, String compression, Recipient recipient, ExecutionResults executionResults) {

        // Parse the report into a text string

        String output = runReport(job, recipient, executionResults);

        // Select the output format we want to work with - default to HTML

        if (format.equals(ExportFormat.TEXT)) {
            Common.writeTextFile(filename, Common.getCleanHtml(output), compression);
            logger.debug("Outputting to text file {}", filename);
        }

        // Use default for everything else

        else  {
            if (!format.equals(ExportFormat.HTML))
                logger.debug("Using default output format HTML");
            Common.writeTextFile(filename, output, compression);
            logger.debug("Outputting to and HTML file {}", filename);
        }
    }

    /**
     * This method runs the report by parsing to with velocity to produce
     * the string output
     *
     * @param job Schedule job
     * @param recipient Recipient if known
     * @param executionResults Results from run
     *
     * @return Parsed report
     */
    private String runReport(Job job, Recipient recipient, ExecutionResults executionResults) {

        // Parse the report into a text string

        String returnValue=null;
        Writer output=new StringWriter();
        ReportHelper helper=new ReportHelper(dataSourceEntity);
        PythonInterpreter engine;
        try {
            // Now get a destination database

            openDatasources();
            logger.debug("Running report {} using database {}", report==null?"[embedded]":report.getName(), dataSourceEntity.getName());

            // Initialise the velocity environment

            job.setStatusMessage("Initialising python");
            engine=new PythonInterpreter();
            logger.debug("Created python engine");

            // Now add the useful stuff to the context

            logger.debug("Creating velocity context");

            // Now add the transformer objects

            engine.set("Recipient", recipient);
            engine.set("Source", dataSource);
            engine.set("Source1", dataSource1);
            engine.set("Source2", dataSource2);
            engine.set("Source3", dataSource3);
            engine.set("Source4", dataSource4);
            engine.set("ReportHelper", helper);
            engine.set("HibernateUtils", HibernateUtils.class);
            engine.set("AppPath", Constants.getLocalAddress());
            engine.set("Job", job);
            engine.set("Utils", Common.class);
            engine.set("XMLUtils", XMLUtils.class);
            engine.set("LDAP", Directory.class);
            engine.set("ExecutionResults", executionResults);
            engine.set("InRunMode", true);
            engine.setOut(output);

            // Add a logger that is categorised by this class name and the report name

            if (report!=null)
                engine.set("Logger", org.slf4j.LoggerFactory.getLogger(PythonReport.class.getName() + '.' + report.getName().replaceAll("\\s","")));
            else
                engine.set("Logger", logger);

            // Add the runtime parameters to the context if there are any

            if (!Common.isBlank(runtimeParams)) {
                for (RuntimeParameter param : runtimeParams) {
                    engine.set(param.getName(), param.getValue());
                }
            }

            // Run the report

            job.setStatusMessage("Evaluating");
            engine.exec(reportText);

        }
        catch (PyException e) {
            String error="Problem running report - " + PivotalException.getErrorMessage(e);
            String backTrace= PythonUtils.getPythonBackTrace(e, reportText);
            if (!Common.isBlank(backTrace))
                error+='\n' + backTrace;
            logger.error(error);
            throw new PivotalException(e);
        }
        catch (Exception e) {
            logger.error("Problem running report - {}", PivotalException.getErrorMessage(e));
            throw new PivotalException(e);
        }

        // Cleanup our connections etc

        finally {
            closeDatasources();
            if (output!=null) {
                try {
                    output.close();
                    returnValue=output.toString();
                }
                catch (Exception e) {
                    logger.error("Problem closing writer - {}", PivotalException.getErrorMessage(e));
                }
            }
        }
        return returnValue;
    }

    /**
     * Parses the command script and populates the transformer parameters
     * This method will throw an exception if it encounters an issue
     * with the parsing i.e. incorrect syntax etc
     *
     * @param reportHelper Report helper object to use
     *
     */
    private void getParameters(ReportHelper reportHelper)  {

        // Initialise the velocity environment

        Writer output=new StringWriter();
        PythonInterpreter engine=new PythonInterpreter();
        logger.debug("Created python engine");

        // Now add the transformer objects

        engine.set("ReportHelper", reportHelper);
        engine.set("InRunMode", false);
        engine.setOut(output);

        // Carry out the transformation using the script provided

        try {
            engine.exec(getDeclareParameterStatements());
        }
        catch (IOException e) {
            String error="Problem parsing report parameters\n  - " + PivotalException.getErrorMessage(e);
            logger.error(error);
            throw new PivotalException(error);
        }
    }

}
