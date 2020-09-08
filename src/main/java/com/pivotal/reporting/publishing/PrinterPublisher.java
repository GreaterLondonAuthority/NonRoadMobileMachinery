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
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.Common;
import com.pivotal.utils.ExecutionResults;
import com.pivotal.utils.PivotalException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Manages the publishing of information to printers
 */
public class PrinterPublisher extends Publisher {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PrinterPublisher.class);

    /**
     * Creates an instance of the this publishing channel for
     * sending emails
     *
     * @param task Associated scheduled task
     * @param list Distribution list from which to get connection details
     */
    public PrinterPublisher(ScheduledTaskEntity task, DistributionListEntity list) {
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
    public void publish(Report report, List<Recipient> recipients, Job job) {

        // Firstly, we need to run the report and export it to a local temporary file

        List<String> errors=new ArrayList<>();
        String destination=null;
        boolean publishingEnabled=false;
        boolean publishingSimulated=false;
        String printer=null;
        ExecutionResults executionResults=new ExecutionResults();

        try {
            // Loop round all the recipients delivering the report to the destination

            for (Recipient recip : recipients) {

                // Parse for variables

                destination= HibernateUtils.parseVariables(recip.getName(), task, recip);
                if (job!=null) job.setStatusMessage("Printing to " + destination);

                // Check if we are allowed to actually publish this

                if (HibernateUtils.getSystemSetting(HibernateUtils.SETTING_PUBLISHING_ENABLED, HibernateUtils.SETTING_PUBLISHING_ENABLED_DEFAULT)) {
                    report.export(destination, Report.ExportFormat.PRINTER, job, executionResults);
                    publishingEnabled=true;
                }
                else {
                    printer = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_DEFAULT_PRINTER_PUBLISHER_ADDRESS, HibernateUtils.SETTING_DEFAULT_PRINTER_PUBLISHER_ADDRESS_DEFAULT);
                    if (!Common.isBlank(printer)) {
                        report.export(printer, Report.ExportFormat.PRINTER, job, executionResults);
                        publishingEnabled=true;
                        publishingSimulated=true;
                    }
                }

                if (publishingEnabled) {
                    if (publishingSimulated)
                        logger.debug("Publishing turned off - printing report to {}", printer);
                    else
                        logger.debug("Printing report to {}", destination);
                }
                else {
                    logger.debug("Publishing turned off - not printing report");
                }
            }
        }
        catch (Exception e) {
            logger.error("Problem publishing report to {} - {}", destination, PivotalException.getErrorMessage(e));
            errors.add(PivotalException.getErrorMessage(e));
        }

        // Add a log entry to say we have started the job

        String rider="";
        if (publishingEnabled) {
            if (publishingSimulated) rider=" PUBLISHING OFF - " + printer;
        }
        else
            rider=" PUBLISHING OFF";
        LogEntity.addLogEntry(LogEntity.STATUS_REPORT_PUBLISHED + rider, getClass().getSimpleName(), startTime, task, report, recipients);
        startTime =new Date();

        // If we have some errors then we need to do something about that

        if (!Common.isBlank(errors)) throw new PivotalException(Common.join(errors, "\n"));
    }
}
