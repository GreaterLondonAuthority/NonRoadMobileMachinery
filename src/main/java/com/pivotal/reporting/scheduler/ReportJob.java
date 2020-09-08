/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.reporting.scheduler;

import com.pivotal.monitoring.utils.ParameterValue;
import com.pivotal.reporting.publishing.Publisher;
import com.pivotal.reporting.publishing.PublisherFactory;
import com.pivotal.reporting.publishing.Recipient;
import com.pivotal.reporting.reports.Report;
import com.pivotal.reporting.reports.ReportFactory;
import com.pivotal.reporting.reports.RuntimeParameter;
import com.pivotal.system.hibernate.entities.LogEntity;
import com.pivotal.system.hibernate.entities.ScheduledTaskEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.monitoring.EventMonitor;
import com.pivotal.utils.Common;
import com.pivotal.utils.ExecutionResults;
import com.pivotal.utils.PivotalException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * This class is a running Report job that runs within it's own thread
 */
public class ReportJob extends Job {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ReportJob.class);


    /**
     *
     * Constructs a new entry of the task queue. It won't be associated with any task
     */
    protected ReportJob() {
        super();
    }

    /**
     **************************************************************************
     *
     * Constructs a new entry of the task queue
     *
     * @param objEntry The task entry to execute
     *
     **************************************************************************
     */
    public ReportJob(ScheduledTaskEntity objEntry) {
        super(objEntry);
    }

    /**
     **************************************************************************
     *
     * This method is called by the thread manager to begin the thread
     *
     * @return String representing the result of the execution
     *
     **************************************************************************
     */
    protected ExecutionResults runTask() {

        String error;
        Report report=null;

        try {
            // Get the task to run (in the current thread Hibernate session)
            scheduledTask = HibernateUtils.getEntity(ScheduledTaskEntity.class, scheduledTaskID);

            report = getReport();
            if (report != null)
                doPublish(report);
        }
        catch (Exception e) {
            error=PivotalException.getErrorMessage(e);
            runtimeErrors.add(error);
            logger.error("Problem running scheduled task {} - {}", scheduledTask.getName(), PivotalException.getStackTrace(e));
            LogEntity.addLogEntry(LogEntity.STATUS_SYSTEM_ERROR, scheduledTask, "Problem running scheduled task " + scheduledTask.getName() + " - " + error);
        }

        // Make sure we close the report and all it's resources

        logger.debug("Closing report {}", scheduledTask.getName());
        Common.close(report);
        EventMonitor.addEvent(EventMonitor.EVENT_REPORT, scheduledTask.getName(), new Date().getTime() - startDate.getTime());

        // Close the hibernate session associated with this job

        HibernateUtils.closeSession();

        return new ExecutionResults();
    }

    /**
     * Builds a Report Object from this job's task
     *
     * @return Report Object
     */
    protected Report getReport() {
        Report report = null;
        // Get a report to run if there is one
        if (scheduledTask != null && scheduledTask.getReport()!=null) {
            setStatusMessage("Opening report " + scheduledTask.getReport().toString());
            logger.debug("Opening report {}", scheduledTask.getReport().toString());
            report= ReportFactory.getReport(scheduledTask.getReport());

            // Sets the connection properties

            logger.debug("Setting connection properties {}", scheduledTask.getDatasource().getName());
            report.setDatasource(scheduledTask.getDatasource(), scheduledTask.getDatasource1(), scheduledTask.getDatasource2(), scheduledTask.getDatasource3(), scheduledTask.getDatasource4());
        }
        return report;
    }


    /**
     * Publish the Report.
     *
     * @param report Report object
     * @throws java.lang.Exception if any.
     */
    protected void doPublish(Report report) throws Exception {
        List<RuntimeParameter> params;
        String error;// Now get a publishing channel to use

        logger.debug("Getting publisher {}", scheduledTask.getName());
        Publisher publisher= PublisherFactory.getPublisher(scheduledTask, scheduledTask.getDistributionList());

        // Check to see if we have some recipients to work on

        if (!Common.isBlank(publisher.getRecipients())) {

            // We now need to loop through all the groups of recipients of the
            // publisher channel

            logger.debug("Looping through {} recipients for {}", publisher.getRecipients().size(), scheduledTask.getName());
            Collection<ParameterValue> paramColl = getReportParamaterValues();
            for (List<Recipient> recipients : publisher.getRecipients()) {
                try {
                    // Update the status and check if we have been told to stop

                    if (!running) throw new Exception("Report Job stopped prematurely by system");

                    // Now loop through all the parameters creating values to use
                    // in the report if there is a report

                    if (report!=null) {
                        params=null;
                        logger.debug("Parsing all the report parameters");
                        if (!Common.isBlank(paramColl)) {
                            params=new ArrayList<>();
                            for (ParameterValue parameter : paramColl) {
                                params.add(new RuntimeParameter(scheduledTask, parameter, recipients));
                            }
                        }

                        // Apply the parameters to the report

                        logger.debug("Executing report {}", scheduledTask.getName());
                        report.setParameters(params);
                    }

                    // Now we need to publish the report
                    // This will cause the report to be run

                    logger.debug("Publishing report to {}", scheduledTask.getDistributionList().getName());
                    publisher.publish(report, recipients, this);

                    // If this is a 'run once' report then set it's schedule type to 'never'

                    if (scheduledTask.getSchedType()!= null && scheduledTask.getSchedType()== ScheduledTaskEntity.SCHED_TYPE_ONCE) {
                        scheduledTask.setSchedType(ScheduledTaskEntity.SCHED_TYPE_NEVER);
                        HibernateUtils.save(scheduledTask);
                    }
                }
                catch (Exception e) {
                    error=PivotalException.getErrorMessage(e);
                    if (error!=null && error.contains("PivotalException:")) {
                        error=error.split("com.pivotal.utils.PivotalException:",2)[1];
                    }
                    runtimeErrors.add(error);
                    logger.error("Problem running scheduled task {}", scheduledTask.getName());
                    LogEntity.addLogEntry(LogEntity.STATUS_SYSTEM_ERROR, scheduledTask, "Problem running scheduled task " + scheduledTask.getName() + " - " + error);
                }
            }
        }
    }

    /**
     * Returns the report Parameter Value Collection.
     *
     * @return Parameter Value Collection
     * @throws java.lang.Exception if any.
     */
    protected Collection<ParameterValue> getReportParamaterValues() throws Exception {
        return scheduledTask.getParameters().getValues();
    }

}
