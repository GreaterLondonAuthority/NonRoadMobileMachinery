package com.pivotal.reporting.reports;

import com.pivotal.monitoring.utils.Definition;
import com.pivotal.reporting.publishing.Recipient;
import com.pivotal.reporting.reports.sqldump.ExcelOutput;
import com.pivotal.reporting.reports.sqldump.SQLOutput;
import com.pivotal.reporting.reports.sqldump.TextOutput;
import com.pivotal.reporting.scheduler.Job;
import com.pivotal.system.data.dao.Database;
import com.pivotal.system.hibernate.entities.ReportEntity;
import com.pivotal.utils.*;
import com.google.common.collect.Lists;
import com.pivotal.utils.VelocityUtils;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * This class implements a report type that creates Excel spreadsheets
 * (primarily, but does also support other formats too) from
 * a collection of SQL statements expressed within a text file
 * The file is parsed by velocity to collect the list of parameters and then
 * parsed again at runtime to replace the parameters within the SQL statements
 * In the case of Excel, each SQL statement that produces data will
 * create a worksheet within the spreadsheet and each column is translated
 * to the correct excel column data type
 * There is a "special" parameter name called "delimiter" which is used in
 * favour of the normal comma/tab characters if it is specified
 *
 * An example of a SQLDump report;
 *
 * <pre>
 *
 *     ## Declare some parameters
 *
 *     $ReportHelper.declareParameter("Start", "Start Date", "The start date of this report", $utils.dateFormat(Utils.Now, "yyyy-MM-dd"), false, small_text)
 *     $ReportHelper.declareParameter("GroupSize", "Minimum Group Size", "Only include groups with a minimum of this number of records within them", "10", true, small_text)
 *
 *     ## Specify the caret character as our delimiter
 *
 *     $ReportHelper.declareParameter("Delimiter", "Delimiter Character", "Delimiter character to use for TSV/CSV type outputs", "^", false, "small_text")
 *
 *     ## Specify the single quote character as our quoting character (leave empty for no quoting)
 *
 *     $ReportHelper.declareParameter("Quote", "Quote Character", "Quote character to use for TSV/CSV type outputs", "'", false, "small_text")
 *
 *     ## Specify a line break as the line ending string (Carriage Return &amp; Line Feed is the default)
 *
 *     $ReportHelper.declareParameter("LineEnd", "Line Ending", "Line ending to use for TSV/CSV type outputs", "\n", false, "small_text")
 *
 *     ## Specify if each worksheet should show a header row
 *
 *     $ReportHelper.declareParameter("Header", "Show Header", "If true, then show a header row for each worksheet", "1", false, "boolean")
 *
 *     ## Statements to run - this shows how to use the parameter values
 *     ## and also shows how to set the name of the worksheets
 *
 *     #set ($StartDate=$utils.dateFormat($utils.parseDate($Start), "yyyy-MM-dd"))
 *
 *     newsheet "Counts";  ## this line tells NRMM to create a new worksheet called Counts
 *
 *     select count(*) as MessageCount, store_id from process where t_time&gt;='$StartDate'
 *          group by store_id having MessageCount &gt;= $GroupSize;
 *
 *     ## Lets create a worksheet per customer promise
 *     ## This illustrates how to use the $ReportHelper object to look at the data to determine
 *     ## what to actually report on
 *     ## It is important to note that $ReportHelper only uses the primary datasource
 *
 *     use datasource1; ## Use the secondary data source for future queries at runtime
 *                      ## This does not affect the use of $ReportHelper which always uses the
 *                      ## primary datasource
 *
 *     #foreach ($Row in $ReportHelper.find("select id,name from customer_promise"))
 *
 *         newsheet "$Row.name";  ## Create a new sheet called the name of the promise
 *
 *         select * from process where customer_promise_id=$Row.id and t_time &gt; '$StartDate';
 *     #end
 *
 * </pre>
 *
 * For more information about the syntax and capabilities of velocity see http://velocity.apache.org/engine/releases/velocity-1.7
 */
public class SQLDumpReport extends Report {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SQLDumpReport.class);

    private String sqlCommand;
    private Recipient recipient;


    /**
     * Opens the report, parses it and determines the list of parameters
     * declared within it
     *
     * @param report Report object to interrogate
     * @throws java.io.UnsupportedEncodingException If the bytes cannot be converted to UTF-8
     */
    public SQLDumpReport(ReportEntity report) throws UnsupportedEncodingException {

        // Get a local copy of the report script so that it doesn't
        // change in between parsing and executing

        this.report=report;
        sqlCommand=getScript(report);

        // Create a helper object to get the parameters

        ReportHelper reportHelper=new ReportHelper();

        // Parse the script to get the number of parameters etc

        parseCommand(reportHelper);

        // Get any report parameters that were declared in the script

        reportParams=reportHelper.getParameters();
    }

    /**
     * Close the report
     * Nothing to do here for this type of report
     */
    public void close() {
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

        // Store the runtime parameters just in case we want them later

        runtimeParams = parameters;

    }

    /**
     * {@inheritDoc}
     *
     * This is the business end of the system where we run each of the
     * SQL statements against the database, collect all the data and
     * write it out to the output type
     */
    protected void exportReport(String filename, ExportFormat format, Job job, String compression, Recipient recipient, ExecutionResults executionResults) {

        this.recipient = recipient;
        SQLOutput dumpFile;
        List<String> sqlCommands;

        // Determine if we should not output the header

        boolean noHeader= getParameterValue("Header") != null && !Common.isYes((String)getParameterValue("Header"));

        // Select the output format we want to work with

        dumpFile = selectDumpFile(filename, format, compression, noHeader);

        // Parse the command and convert the output into a list of possible
        // SQL commands

        sqlCommands = getCommnds();

        try {
            // Open a connection to the database(s)

            openDatasources();

            // Set the flags to manage the worksheet creation

            boolean sectionCreated=false;
            boolean createNewSheets=true;

            // Loop round each of the SQL statements

            Database database= dataSource;
            for (String command : sqlCommands) {

                // Look for a new sheet

                if (command.matches("(?ims)newsheet\\s+\"[^\"]+\"")) {
                    logger.debug("Adding a section {}", command);
                    dumpFile.newSection(command.split("\"")[1]);
                    sectionCreated=true;
                    createNewSheets=true;
                }

                // Look for a section divider

                else if (command.matches("(?ims)suppressnewsheets")) {
                    logger.debug("Turned off new sheets");
                    createNewSheets=false;
                }

                // look for value update

                else if (command.matches("(?ims)setvalue .+=.+")) {
                    // These values are available in the report/dist list using $ExecutionResults.getValue()
                    logger.debug("Setting value " + command);
                    String[] keyValue = Common.getItem(command, "setvalue ", 1).split("=");
                    if (keyValue.length == 2)
                        executionResults.setValue(keyValue[0].trim(), keyValue[1].trim());
                }
                // Execute a datasource change

                else if (command.matches("(?ims)use\\s+(datasource|database)[1-4]?\\s*")) {
                    String source=command.replaceAll("[^1-4]","").trim();
                    switch (source) {
                        case "1":
                            database = dataSource1;
                            break;
                        case "2":
                            database = dataSource2;
                            break;
                        case "3":
                            database = dataSource3;
                            break;
                        case "4":
                            database = dataSource4;
                            break;
                        default:
                            database = dataSource;
                            break;
                    }
                    if (database==null) {
                        logger.error("Changed database to a null data source [{}]", source);
                        throw new Exception("Problem running command [" + command + "] - data source unknown");
                    }
                    else
                        logger.debug("Changed database to {}", database.getClass().getSimpleName());
                }

                // Execute a select command

                else if (command.matches("(?ims)select\\s.*")) {

                    // TODO We need to do this in a better way so that we don't exhaust memory for very
                    // TODO large results sets

                    logger.debug("Executing select command {}", command);
                    List<Map<String,Object>> results= database.find(command);
                    if (!database.isInError()) {

                        // Create a new section if we don't have one

                        if (!sectionCreated) dumpFile.newSection();

                        // Save the values to the output

                        logger.debug("Found {} results", results.size());
                        for (Map<String,Object>rowValues : results)
                            dumpFile.addRow(rowValues);

                        // We have to be careful here because Excel only supports 347 tabs and it is easy to
                        // exhaust them so we have a mechanism to turn off the generation of new sheets

                        if (createNewSheets) sectionCreated=true;
                    }
                }

                // Execute a non-query command

                else {
                    logger.debug("Executing command {}", command);
                    database.execute(command);
                }

                // Check for any errors

                if (database.isInError()) {
                    throw new Exception("Problem running command [" + command + "] - " + database.getLastError());
                }
            }
        }
        catch (Exception e) {
            throw new PivotalException(e);
        }

        finally {

            // Save the file and close everything

            dumpFile.close();
            dataSource.close();
            closeDatasources();
        }
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
     * Return the list of runtime parameters
     *
     * @return List of runtime parameters
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
        return Lists.newArrayList(ExportFormat.EXCEL97, ExportFormat.EXCEL2010, ExportFormat.CSV, ExportFormat.TSV);
    }

    /**
     * Parses the command script and returns the output
     * This method will throw an exception if it encounters and issue
     * with the parsing i.e. incorrect syntax etc
     * This version of the the method makes available to the context
     * a database connection
     *
     * @param parameters List of runtime parameters to use
     *
     * @return String output of parsing
     */
    private String parseCommand(List<RuntimeParameter> parameters) {

        String returnValue=null;
        ReportHelper reportHelper=new ReportHelper(dataSourceEntity);
        try {
            reportHelper.open();
            returnValue= parseCommand(reportHelper, parameters);
        }
        catch (Exception e) {
            throw new PivotalException(e);
        }
        finally {
            reportHelper.close();
        }
        return returnValue;
    }

    /**
     * Parses the command script and returns the output
     * This method will throw an exception if it encounters and issue
     * with the parsing i.e. incorrect syntax etc
     *
     * @param reportHelper Report helper object to use
     *
     * @return String output of parsing
     */
    private String parseCommand(ReportHelper reportHelper) {
        return parseCommand(reportHelper, null);
    }

    /**
     * Parses the command script and returns the output
     * This method will throw an exception if it encounters and issue
     * with the parsing i.e. incorrect syntax etc
     *
     * @param reportHelper Report helper object to use
     * @param parameters List of runtime parameters to use
     *
     * @return String output of parsing
     */
    private String parseCommand(ReportHelper reportHelper, List<RuntimeParameter> parameters) {
        Writer output=new StringWriter();
        VelocityEngine engine;
        try {

            // Get a velocity engine to use

            engine= VelocityUtils.getEngine();

            // Now add the useful stuff to the context

            logger.debug("Creating velocity context");
            Context context= VelocityUtils.getVelocityContext();

            // Parse the SQL statements with the ReportHelper object to allow
            // it to collect all the possible parameters

            context.put("ReportHelper", reportHelper);
            context.put("Recipient", recipient);
            context.put("Out", System.out);

            // Add the runtime parameters to the context if there are any

            if (!Common.isBlank(parameters)) {
                for (RuntimeParameter param : parameters) {
                    context.put(param.getName(), param.getValue());
                }
            }

            // Add a logger that is categorised by this class name and the report name

            if (report!=null)
                context.put("Logger", org.slf4j.LoggerFactory.getLogger(VelocityReport.class.getName() + '.' + report.getName().replaceAll("\\s","")));
            else
                context.put("Logger", logger);

            // Run the evaluation of the script

            engine.evaluate(context, output, getClass().getSimpleName(), sqlCommand);

            // Find the number of data sources used by the script by parsing it carefully

            for (int i=1; i<4; i++) {
                if (Pattern.compile("(?ims)use\\s+datasource" + i + "\\s*;").matcher(sqlCommand).find())
                    numberOfRequiredDatasources++;
            }
        }
        catch (Exception e) {
            String error="Problem parsing recipient variables\n    " + sqlCommand + " - " + PivotalException.getErrorMessage(e);
            logger.error(error);
            throw new PivotalException(error);
        }
        return output.toString().trim();
    }

    /**
     * Creates a dump file to use based on the parameters and export type chosen
     * If the output type is not supported then an error will be thrown
     *
     * @param filename Filename to save to
     * @param format Format to export
     * @param compression Compression algorithm to use
     * @param noHeader True if there is to be no headers
     *
     * @return SQLOutput Dump file object to use
     */
    private SQLOutput selectDumpFile(String filename, ExportFormat format, String compression, boolean noHeader) {
        SQLOutput dumpFile;
        if (format.equals(ExportFormat.EXCEL97) || format.equals(ExportFormat.EXCEL2010)) {
            dumpFile=new ExcelOutput(filename, compression, noHeader);
            logger.debug("Outputting to Excel file {}", filename);
        }
        else if (format.equals(ExportFormat.CSV) || format.equals(ExportFormat.TSV)) {
            dumpFile=new TextOutput(filename, (String)getParameterValue("Delimiter"),
                                    (String)getParameterValue("Quote"), (String)getParameterValue("Escape"),
                                    (String)getParameterValue("LineEnd"), compression, noHeader);
            logger.debug("Outputting to delimited file {}", filename);
        }
        else
            throw new PivotalException("Unsupported export format " + format);

        return dumpFile;
    }

    /**
     * Returns a list of all the possible commands to run
     *
     * @return List of strings
     */
    private List<String> getCommnds() {

        // Parse the command and convert the output into a list of possible
        // SQL commands

        List<String> sqlCommands;
        String commands = parseCommand(runtimeParams);
        logger.debug("Parsed SQL into {}", commands);

        // All SQL commands are terminated by ; and must start on a new line

        if (!Common.isBlank(commands)) {
            sqlCommands=new ArrayList<>();
            List<String> localCommands=Common.splitToList(commands, "\\s*;\\t* *\\r?\\n\\s*");

            // Clean up the commands, removing empty ones

            if (!Common.isBlank(localCommands)) {
                for (String command : localCommands) {
                    if (!Common.isBlank(command)) {
                        logger.debug("Created command {}", command);
                        sqlCommands.add(command.replaceAll("[\\n\\r\\t ]+"," "));
                    }
                }
            }
        }
        else
            throw new PivotalException("There are now SQL commands to run");

        return sqlCommands;
    }

}
