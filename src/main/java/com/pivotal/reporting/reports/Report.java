/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.reporting.reports;

import com.pivotal.monitoring.utils.Definition;
import com.pivotal.reporting.publishing.Recipient;
import com.pivotal.reporting.scheduler.Job;
import com.pivotal.system.data.dao.Database;
import com.pivotal.system.data.dao.DatabaseHibernate;
import com.pivotal.system.data.dao.DatabaseApp;
import com.pivotal.system.hibernate.entities.DatasourceEntity;
import com.pivotal.system.hibernate.entities.ReportEntity;
import com.pivotal.utils.Common;
import com.pivotal.utils.ExecutionResults;
import com.pivotal.web.Constants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class for defining a report
 */
public abstract class Report {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Report.class);
    private static final String DECLARE_STATEMENT_REGEXP = "\\s*\\$?ReportHelper\\.declareParameter.*\\s*";

    protected DatasourceEntity dataSourceEntity;
    protected DatasourceEntity dataSourceEntity1;
    protected DatasourceEntity dataSourceEntity2;
    protected DatasourceEntity dataSourceEntity3;
    protected DatasourceEntity dataSourceEntity4;
    protected Database dataSource;
    protected Database dataSource1;
    protected Database dataSource2;
    protected Database dataSource3;
    protected Database dataSource4;
    protected ReportEntity report;
    protected Definition reportParams;
    protected List<RuntimeParameter> runtimeParams=new ArrayList<>();
    protected int numberOfRequiredDatasources=1;
    protected boolean overrideInOperation;

    /**
     * Opens all the datasources required by the report
     *
     * @throws java.lang.Exception If one of the datasources can't be opened
     */
    protected void openDatasources() throws Exception {

        // Close and reset all the datasources

        closeDatasources();

        // We must always have a primary data source, all the rest are optional

        dataSource=new DatabaseHibernate();

        // Open a connection to the database(s)

        logger.debug("Opening source {}", dataSource);
        dataSource.open();
        dataSource.setMaximumResults(0);
        if (dataSourceEntity1 !=null) {
            logger.debug("Opening source {}", dataSourceEntity1);
            dataSource1=new DatabaseApp(dataSourceEntity1);
            dataSource1.open();
            dataSource1.setMaximumResults(0);
        }
        if (dataSourceEntity2 !=null) {
            logger.debug("Opening source {}", dataSourceEntity2);
            dataSource2=new DatabaseApp(dataSourceEntity2);
            dataSource2.open();
            dataSource2.setMaximumResults(0);
        }
        if (dataSourceEntity3 !=null) {
            logger.debug("Opening source {}", dataSourceEntity3);
            dataSource3=new DatabaseApp(dataSourceEntity3);
            dataSource3.open();
            dataSource3.setMaximumResults(0);
        }
        if (dataSourceEntity4 !=null) {
            logger.debug("Opening source {}", dataSourceEntity4);
            dataSource4=new DatabaseApp(dataSourceEntity4);
            dataSource4.open();
            dataSource4.setMaximumResults(0);
        }
    }

    /**
     * Closes all the datasources used by the report
     */
    protected void closeDatasources() {
        Common.close(dataSource, dataSource1, dataSource2, dataSource3, dataSource4);
        dataSource=null;
        dataSource1=null;
        dataSource2=null;
        dataSource3=null;
        dataSource4=null;
    }

    /** List of possible output formats **/
    public enum ExportFormat {
        EXCEL97,EXCEL2010,WORD,XML,JSON,RPT,PRINTER,TSV,CSV,HTML,TEXT;

        // The following methods are to allow Velocity to access these enumerations
        public static ExportFormat getEXCEL97() {return EXCEL97;}
        public static ExportFormat getEXCEL2010() {return EXCEL2010;}
        public static ExportFormat getWORD() {return WORD;}
        public static ExportFormat getXML() {return XML;}
        public static ExportFormat getJSON() {return JSON;}
        public static ExportFormat getRPT() {return RPT;}
        public static ExportFormat getPRINTER() {return PRINTER;}
        public static ExportFormat getTSV() {return TSV;}
        public static ExportFormat getCSV() {return CSV;}
        public static ExportFormat getHTML() {return HTML;}
        public static ExportFormat getTEXT() {return TEXT;}

        // Get the export format for the type string
        public static ExportFormat getType(String type) {
            if (Common.doStringsMatch(EXCEL97.toString(), type))
                return EXCEL97;
            else if (Common.doStringsMatch(EXCEL2010.toString(), type))
                return EXCEL2010;
            else if (Common.doStringsMatch(WORD.toString(), type))
                return WORD;
            else if (Common.doStringsMatch(XML.toString(), type))
                return XML;
            else if (Common.doStringsMatch(JSON.toString(), type))
                return JSON;
            else if (Common.doStringsMatch(RPT.toString(), type))
                return RPT;
            else if (Common.doStringsMatch(PRINTER.toString(), type))
                return PRINTER;
            else if (Common.doStringsMatch(TSV.toString(), type))
                return TSV;
            else if (Common.doStringsMatch(CSV.toString(), type))
                return CSV;
            else if (Common.doStringsMatch(HTML.toString(), type))
                return HTML;
            else
                return TEXT;
        }

        // Get the export format description
        public String getDescription() {
            return "admin.scheduled_task.output.type." + toString();
        }

        public String toString() {
            if (equals(EXCEL97))
                return "xls";
            else if (equals(EXCEL2010))
                return "xlsx";
            else if (equals(WORD))
                return "rtf";
            else if (equals(XML))
                return "xml";
            else if (equals(JSON))
                return "json";
            else if (equals(RPT))
                return "rpt";
            else if (equals(PRINTER))
                return "printer";
            else if (equals(TSV))
                return "tsv";
            else if (equals(CSV))
                return "csv";
            else if (equals(HTML))
                return "html";
            else if (equals(TEXT))
                return "txt";
            else
                return "pdf";
        }

        public String gettext() {
            return getDescription();
        }

        public String getvalue() {
            return toString();
        }

        public boolean is(ExportFormat...types) {
            boolean returnValue = false;
            if (!Common.isBlank(types)) {
                for (ExportFormat type : types) {
                    if (equals(type)) returnValue = true;
                }
            }
            return returnValue;
        }
    }

    /** List of valid data types */
    public enum ColumnType {
        BOOLEAN,INTEGER,DOUBLE,STRING,TEXT,BLOB,DATE;

        // The following methods are to allow Velocity to access these enumerations
        public static ColumnType getBOOLEAN() {return BOOLEAN;}
        public static ColumnType getINTEGER() {return INTEGER;}
        public static ColumnType getDOUBLE() {return DOUBLE;}
        public static ColumnType getSTRING() {return STRING;}
        public static ColumnType getTEXT() {return TEXT;}
        public static ColumnType getBLOB() {return BLOB;}
        public static ColumnType getDATE() {return DATE;}
    }


    /**
     *
     * Closes and disposes of the resources used
     * The dispose has the effect of actually disconnecting from the data source
     * whereas the close() simply garbage collects the report memory
     */
    public abstract void close();

    /**
     * Stores a reference to the datasource to use at runtime
     * This datasource will be the one used to exercise the SQL
     * statements against
     *
     * @param dataSource Connection properties
     * @param dataSource1 Alternative connection properties
     * @param dataSource2 Alternative connection properties
     * @param dataSource3 Alternative connection properties
     * @param dataSource4 Alternative connection properties
     */
    public void setDatasource(DatasourceEntity dataSource, DatasourceEntity dataSource1, DatasourceEntity dataSource2, DatasourceEntity dataSource3, DatasourceEntity dataSource4) {

        // Save a reference to this data source for usage later

        dataSourceEntity=dataSource;
        dataSourceEntity1=dataSource1;
        dataSourceEntity2=dataSource2;
        dataSourceEntity3=dataSource3;
        dataSourceEntity4=dataSource4;
    }

    /**
     *
     * This method applies the parameters to the report
     *
     * @param parameters List of parameter values
     */
    public abstract void setParameters(List<RuntimeParameter> parameters);

    /**
     *
     * This method exports the report to the Response stream with a flag to
     * indicate if it should be sent as an attachment
     *
     * @param filename Filename to save to
     * @param format Format to export
     * @param compression Compression algorithm to use
     * @param executionResults Results from the execution
     */
    public void export(String filename, ExportFormat format, String compression, ExecutionResults executionResults) {
        exportReport(filename, format, null, compression, null, executionResults);
    }

    /**
     *
     * This method exports the report to the Response stream with a flag to
     * indicate if it should be sent as an attachment
     *
     * @param filename Filename to save to
     * @param format Format to export
     * @param job Job that this report may be associated with (can be null)
     * @param executionResults Results from the execution
     */
    public void export(String filename, ExportFormat format, Job job, ExecutionResults executionResults) {

        // This is for future development - we can place code here that sets
        // the scene for running the report i.e. adding classes etc

        exportReport(filename, format, job, null, null, executionResults);
    }

    /**
     *
     * This method exports the report to the Response stream with a flag to
     * indicate if it should be sent as an attachment
     *
     * @param filename Filename to save to
     * @param format Format to export
     * @param executionResults Results from the execution
     */
    public void export(String filename, ExportFormat format, ExecutionResults executionResults) {

        // This is for future development - we can place code here that sets
        // the scene for running the report i.e. adding classes etc

        exportReport(filename, format, null, null, null, executionResults);
    }

    /**
     *
     * This method exports the report to the Response stream with a flag to
     * indicate if it should be sent as an attachment
     *
     * @param filename Filename to save to
     * @param format Format to export
     * @param job Job that this report may be associated with (can be null)
     * @param compression Compression algorithm to use
     * @param recipient Recipient if known
     * @param executionResults Results from the execution
     */
    public void export(String filename, ExportFormat format, Job job, String compression, Recipient recipient, ExecutionResults executionResults) {

        // This is for future development - we can place code here that sets
        // the scene for running the report i.e. adding classes etc

        exportReport(filename, format, job, compression, recipient, executionResults);
    }

    /**
     *
     * This method exports the report to the Response stream with a flag to
     * indicate if it should be sent as an attachment
     *
     * @param filename Filename to save to
     * @param format Format to export
     * @param job Job that this report may be associated with (can be null)
     * @param compression Compression algorithm to use
     * @param recipient Recipient if known
     * @param executionResults Results from the execution
     */
    protected abstract void exportReport(String filename, ExportFormat format, Job job, String compression, Recipient recipient, ExecutionResults executionResults);

    /**
     *
     * Returns the list of parameters embedded in the report as a Definition
     *
     * @return Definition of parameters
     */
    public abstract Definition getReportParameters();

    /**
     *
     * Returns the list of parameters used to produce the report
     * in the report
     *
     * @return List of parameters
     */
    public abstract List<RuntimeParameter> getParameters();

    /**
     * Convenience method for returning a particular runtime parameter
     *
     * @param name Name of the parameter to get
     * @return A runtime parameter
     */
    public RuntimeParameter getParameter(String name) {
        RuntimeParameter returnValue=null;
        if (!Common.isBlank(getReportParameters()) && !Common.isBlank(getParameters()) && getReportParameters().parameterExists(name)) {
            for (RuntimeParameter param : getParameters()) {
                if (!Common.isBlank(param.getName()) && param.getName().equalsIgnoreCase(name)) {
                    returnValue=param;
                    break;
                }
            }
        }
        return returnValue;
    }

    /**
     * Convenience method for returning the value of a particular runtime parameter
     *
     * @param name Name of the parameter to get
     * @return A runtime parameter
     */
    public Object getParameterValue(String name) {
        RuntimeParameter value=getParameter(name);
        if (value!=null)
            return value.getValue();
        else
            return null;
    }

    /**
     *
     * Returns a list of all the supported export types
     *
     * @return List of types
     */
    public abstract List<ExportFormat> getSupportedExportTypes();

    /**
     *
     * Returns the number of data sources used by this report
     *
     * @return Number of datasources used
     */
    public int getRequiredDatasources() {
        return numberOfRequiredDatasources;
    }

    /**
     * Analyses the report script and return just Declare Parameter statements.
     * It is used to show custom parameters on scheduled tasks page for this report.
     *
     * @return Script containing just Declare Parameter statements
     * @throws java.io.IOException When unable to open the script
     */
    protected String getDeclareParameterStatements() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String row : getScript(report).split("\n")) {
            if (row.matches(DECLARE_STATEMENT_REGEXP)) {
                builder.append(row).append('\n');
            }
        }
        return builder.toString();
    }

       /**
        * This method decides whether to load the script uploaded into the database
        * or open the script file configured in the file system
        *
        * @return A String representing the Script
        */
        public String getScript() {
            return getScript(report, this);
        }

       /**
        * This method decides whether to load the script uploaded into the database
        * or open the script file configured in the file system
        *
        * @param report Report entity to get the script from
        * @return A String representing the Script
        */
       public static String getScript(ReportEntity report) {
        return getScript(report, null);
    }

    /**
     * This method decides whether to load the script uploaded into the database
     * or open the script file configured in the file system
     *
     * @param report Report entity to get the script from
     * @param reportObj Optional report object to update
     * @return A String representing the Script
     */
    public static String getScript(ReportEntity report, Report reportObj) {

        // See if we want to override this file content from the java_opts
        // We would only do this in development of course

        String script = null;
        if (reportObj != null) reportObj.overrideInOperation = false;
        File localFile = getOverridingFile(report);
        if (localFile != null) {
            script = Common.readTextFile(localFile);
            logger.warn("Overriding script file for [{}] with [{}]", report.getName(), localFile.getAbsolutePath());
            if (reportObj != null) reportObj.overrideInOperation = true;
        }

        // If we don't have a file to use

        if (script == null)
            script = report.getFileString();

           return script;
       }

    /**
     * Returns the possible local filename for the specified report if it has
     * been overridden
     *
     * @param report Report entity to check
     * @return Report file if present otherwise null
     */
    public static File getOverridingFile(ReportEntity report) {
        File returnValue=null;
        String overrideDirectory=System.getProperties().getProperty(Constants.SCRIPT_OVERRIDE_DIRECTORY);
        if (!Common.isBlank(overrideDirectory) && new File(overrideDirectory).exists()) {
            List<File> files=Common.listFiles(overrideDirectory, report.getName() + "\\.[a-zA-Z0-9]+", false, false);
            if (!Common.isBlank(files)) {
                returnValue = files.get(0);
            }
        }
        return returnValue;
    }
}
