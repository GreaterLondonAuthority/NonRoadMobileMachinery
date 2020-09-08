/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.reporting.publishing;

import com.pivotal.reporting.reports.Report;
import com.pivotal.reporting.scheduler.Job;
import com.pivotal.system.hibernate.entities.DistributionListEntity;
import com.pivotal.system.hibernate.entities.LogEntity;
import com.pivotal.system.hibernate.entities.ScheduledTaskEntity;
import com.pivotal.utils.Common;
import com.pivotal.utils.ExecutionResults;
import com.pivotal.utils.PivotalException;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Manages the publishing of information to nowhere
 * More accurately, it acts as a publishing sink that simply swallows everything you
 * throw at it and never complains and never actually delivers anything
 */
public class DeadEndPublisher extends Publisher {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DeadEndPublisher.class);

    /**
     * Creates an instance of the this publishing channel for
     * sending emails
     *
     * @param task Associated scheduled task
     * @param list Distribution list from which to get connection details
     */
    public DeadEndPublisher(ScheduledTaskEntity task, DistributionListEntity list) {
        super(task,list);
    }

    /**
     * Sends the data to the channel
     *
     * @param recipients List of recipients to publish to
     * @param job Report job (if any) that is running this publishing
     */
    public void publish(List<Recipient> recipients, Job job) {
        publish(null, recipients, job);
    }

    /**
     * {@inheritDoc}
     *
     * Sends the data to the channel
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void publish(Report report, List<Recipient> recipients, Job job) {

        // We need to run the report and export it to a local temporary file

        List<String> errors=new ArrayList<>();
        String destination=null;
        ExecutionResults executionResults=new ExecutionResults();

        try {
            destination=Common.getTemporaryFilename();
            logger.debug("Exporting report to {}", destination);

            // Check if we are allowed to actually publish this

            if (job!=null) job.setStatusMessage("Publishing to " + destination);
            report.export(destination, Report.ExportFormat.getType(task.getOutputType()), job, executionResults);
        }
        catch (Exception e) {
            logger.error("Problem publishing report to {} - {}", destination, PivotalException.getErrorMessage(e));
            errors.add(PivotalException.getErrorMessage(e));
        }
        finally {
            if (!Common.isBlank(destination))
                new File(destination).delete();
        }

        // Add a log entry to say we have completed the job

        LogEntity.addLogEntry(LogEntity.STATUS_REPORT_PUBLISHED, getClass().getSimpleName(), startTime, task, report, recipients);
        startTime=new Date();

        // If we have some errors then we need to do something about that

        if (!Common.isBlank(errors)) throw new PivotalException(Common.join(errors, "\n"));
    }
}
