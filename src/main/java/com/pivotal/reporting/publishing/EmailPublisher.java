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
import com.pivotal.utils.VFSUtils;
import com.pivotal.web.email.Email;
import com.pivotal.web.email.EmailManager;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.Selectors;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.util.*;

/**
 * Manages the publishing of information to email recipients
 */
public class EmailPublisher extends Publisher {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EmailPublisher.class);

    /**
     * Creates an instance of the this publishing channel for
     * sending emails
     *
     * @param task Associated scheduled task
     * @param list Distribution list from which to get connection details
     */
    public EmailPublisher(ScheduledTaskEntity task, DistributionListEntity list) {
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

    public void publish(Report report, List<Recipient> recipients, Job job) {


        List<String> errors=new ArrayList<>();
        String tmpFile=Common.getTemporaryFilename(task.getOutputType());
        String toAddress=null;
        Map<String, Object> extras=new HashMap<>();
        ExecutionResults executionResults=new ExecutionResults();
        boolean deleteFile = true;

        // Firstly, we need to run the report and export it to a local temporary file

        try {

            // Use the first recipient as the main destination

            Recipient mainRecip=recipients.get(0);
            toAddress = mainRecip.getDescriptiveName();

            if (job!=null) job.setStatusMessage("Exporting report to " + tmpFile);
            logger.debug("Exporting report to {}", tmpFile);
            report.export(tmpFile, Report.ExportFormat.getType(task.getOutputType()), job, task.getDistributionList()==null?null:task.getDistributionList().getCompression(), mainRecip, executionResults);
            extras.put("ExecutionResults", executionResults);

            try {

                Email email = new Email();

                if (task.getDistributionList() != null && !Common.isBlank(task.getDistributionList().getEmailSensitivity())) {
                    email.setSensitivity(task.getDistributionList().getEmailSensitivity());
                }

                // Add on the priority if it has been specified

                if (task.getDistributionList() != null && task.getDistributionList().getEmailPriority() != null) {
                    email.setPriority(task.getDistributionList().getEmailPriority() + "");
                }

                // Add on the importance if it has been specified

                if (task.getDistributionList() != null && task.getDistributionList().getEmailImportance() != null) {
                    email.setImportance(task.getDistributionList().getEmailImportance() + "");
                }

                // Encrypt file if necessary and reset tmpFile

                if(task.getDistributionList() != null && task.getDistributionList().isPgpEncryptedOutput() && !Common.isBlank(task.getDistributionList().getPgpPublicKey())){
                    String tmp;
                    if((tmp = encryptOutput(tmpFile))!=null)
                        tmpFile = tmp;
                }

                // Now we need to check to see if there is a VFS location where the report
                // should be copied to

                if (!Common.isBlank(list.getSecondaryContent())) {
                    String destinationDir=createReportFilename(HibernateUtils.parseVariables(list.getSecondaryContent(), task, mainRecip));
                    if (job!=null) job.setStatusMessage("Copying to " + destinationDir);
                    copyfile(tmpFile, destinationDir, list.getSecondaryUsername(), list.getSecondaryPassword());
                    extras.put("DestinationFilename", Common.getFilename(destinationDir));
                    extras.put("DestinationFilenamePath", destinationDir);
                }

                // Attach the report if we have one and we have specified a filename template

                String filename=null;
                if (!Common.isBlank(list.getEmailAttachmentName())) {
                    filename = createReportFilename(HibernateUtils.parseVariables(list.getEmailAttachmentName(), task, mainRecip, extras));
                    FileSystemResource file = new FileSystemResource(new File(tmpFile));
                    email.addAttachment(filename, file.getPath());
                    extras.put("AttachmentFilename", Common.getFilename(filename));
                    deleteFile = false;
                }

                // Set the subject and from (the from address is very important for spam)
                String emailFrom;
                if (!Common.isBlank(list.getEmailFrom()))
                    emailFrom = list.getEmailFrom();
                else
                    emailFrom = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_EMAIL_SERVER_FROM, "");

                email.setSubject(HibernateUtils.parseVariables(list.getEmailSubject(), task, mainRecip, extras));
                email.setFromAddress(HibernateUtils.parseVariables(emailFrom, task, mainRecip, extras));

                // Set the body of the message - this can include the actual content
                // of the report itself but we need to manually check for this to save
                // memory

                String body=HibernateUtils.parseVariables(list.getEmailBody(), task, mainRecip, extras);
                if (tmpFile!=null && !Common.isBlank(list.getEmailBody()) && list.getEmailBody().contains(REPORT_CONTENT_PLACEHOLDER))
                    body=body.replace(REPORT_CONTENT_PLACEHOLDER, Common.readTextFile(tmpFile));

                email.setMessage(body);
                email.setToList(recipients);
                email.setCcList(ccList);
                email.setBccList(bccList);

                EmailManager.queueEmail(email);
            }
            catch (Exception e) {
                logger.error("Problem sending report to [{}] using {} - {}", toAddress, list.toString(), PivotalException.getErrorMessage(e));
                errors.add(PivotalException.getErrorMessage(e));
            }
        }
        catch (Exception e) {
            logger.error("Problem exporting report to {} - {}", tmpFile, PivotalException.getErrorMessage(e));
            errors.add(PivotalException.getErrorMessage(e));
        }

        // Tidy up after ourselves

        File tmp=new File(tmpFile);
        if (tmp.exists()) tmp.delete();

        // Add a log entry to say we have started the job

        String rider="";
        if (EmailManager.isPublishingEnabled())
            if (EmailManager.isPublishingSimulated()) rider=" PUBLISHING OFF - " + toAddress;
        else
            rider=" PUBLISHING OFF";

        LogEntity.addLogEntry(LogEntity.STATUS_REPORT_PUBLISHED + rider, getClass().getSimpleName(), startTime, task, report, recipients);
        startTime=new Date();

        // If we have some errors then we need to do something about that

        if (!Common.isBlank(errors)) throw new PivotalException(Common.join(errors, "\n"));
    }


    /**
     * Copies a file from the source folder to the destination
     * If it encounters any errors, it will throw an exception
     *
     * @param srcFilename Source filename to copy
     * @param destFilename Destination VFS location
     * @param username Optional username to use
     * @param password Optional password to use
     *
     * @throws Exception Any errors with the copy process
     */
    private void copyfile(String srcFilename, String destFilename, String username, String password) throws Exception {

        boolean publishingEnabled=false;
        boolean publishingSimulated=false;
        String toAddress;
        FileSystemManager fsManager = null;
        FileObject destFile;

        try {

            fsManager = VFSUtils.getManager();
            FileObject srcFile = fsManager.resolveFile(srcFilename);

            // Setup security just in case we need it

            FileSystemOptions opts = VFSUtils.setUpConnection(list.getUsername(), list.getPassword(), list.isUserDirIsRoot());

            // Create the destination to send this file to

            destFile = fsManager.resolveFile(destFilename, opts);

            // Check if we are allowed to actually publish this

            if (HibernateUtils.getSystemSetting(HibernateUtils.SETTING_PUBLISHING_ENABLED, HibernateUtils.SETTING_PUBLISHING_ENABLED_DEFAULT)) {
                destFile.copyFrom(srcFile, Selectors.SELECT_ALL);
                publishingEnabled=true;
            }
            else {
                toAddress = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_DEFAULT_VFS_PUBLISHER_ADDRESS, HibernateUtils.SETTING_DEFAULT_VFS_PUBLISHER_ADDRESS_DEFAULT);
                if (!Common.isBlank(toAddress)) {
                    destFile = fsManager.resolveFile(toAddress + '.' + task.getOutputType().toLowerCase(), opts);
                    destFile.copyFrom(srcFile, Selectors.SELECT_ALL);
                    publishingEnabled=true;
                    publishingSimulated=true;
                }
            }
        }
        finally {
            // close connection to the destination folder
            VFSUtils.closeManager(fsManager);
        }

        if (publishingEnabled) {
            if (publishingSimulated)
                logger.debug("Publishing turned off - writing file to {}", destFilename);
            else
                logger.debug("Writing file to {}", destFilename);
        }
        else {
            logger.debug("Publishing turned off - not writing file");
        }
    }

}
