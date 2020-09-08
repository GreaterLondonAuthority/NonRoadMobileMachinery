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
import org.apache.commons.vfs2.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the publishing of information to file systems
 *
 * If the recipient's descriptive name is equals to "empty", an empty file will be published.
 * The descriptive name is what comes after the pipe (|) when specifying the recipient list.
 */
public class VFSPublisher extends Publisher {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(VFSPublisher.class);

    // If the recipient's descriptive name is equals to "empty", an empty file will be published.
    private static final String CREATE_EMPTY_FILE_PARAM = "empty";
    private Integer timeout = null;
    private String proxyServer = null;

    /**
     * Creates an instance of the this publishing channel for
     * sending emails
     *
     * @param task Associated scheduled task
     * @param list Distribution list from which to get connection details
     */
    @SuppressWarnings("unused")
    public VFSPublisher(ScheduledTaskEntity task, DistributionListEntity list) {
        super(task,list);
    }

    /**
     * Sends the data to the channel
     *
     * @param recipients List of recipients to publish to
     * @param job Report job (if any) that is running this publishing
     */
    public void publish(List<Recipient> recipients, Job job) {
        publish(null, recipients);
    }

    /**
     * {@inheritDoc}
     *
     * Sends the data to the channel
     *
     * If the recipient's descriptive name is equals to "empty", an empty file will be published.
     * The descriptive name is what comes after the pipe (|) when specifying the recipient list.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void publish(Report report, List<Recipient> recipients, Job job) {

        // Firstly, we need to run the report and export it to a local temporary file

        List<String> errors=new ArrayList<>();
        String tmpFile= Common.getTemporaryFilename(task.getOutputType());
        String destination=null;
        boolean publishingEnabled=false;
        boolean publishingSimulated=false;
        String toAddress=null;
        ExecutionResults executionResults=new ExecutionResults();
        File tmpKey=null;

        try {

            // Use the first recipient as the main destination

            Recipient mainRecip=recipients.get(0);
            logger.debug("Exporting report to {}", tmpFile);
            if (job!=null) job.setStatusMessage("Exporting to " + tmpFile);
            report.export(tmpFile, Report.ExportFormat.getType(task.getOutputType()), job, task.getDistributionList()==null?null:task.getDistributionList().getCompression(), mainRecip, executionResults);

            // Encrypt file if necessary and reset tmpFile

            if (task.getDistributionList() != null && task.getDistributionList().isPgpEncryptedOutput() && !Common.isBlank(task.getDistributionList().getPgpPublicKey())){
                String tmp;
                if ((tmp = encryptOutput(tmpFile))!=null)
                    tmpFile = tmp;
            }

            // Create a source file object

            FileSystemManager fsManager = null;
            FileObject destFile;

            try {
                fsManager = VFSUtils.getManager();
                FileObject srcFile = fsManager.resolveFile(tmpFile);

                // Loop round all the recipients delivering the report to the destination

                for (Recipient recip : recipients) {

                    // Parse for variables within the filename

                    destination = createReportFilename(HibernateUtils.parseVariables(recip.getName(), task, recip));
                    if (job!=null) job.setStatusMessage("Copying to " + destination);

                    // Setup security just in case we need it

                    if (list.isSshUserAuthentication() && !Common.isBlank(list.getSshKey())) {
                        tmpKey = new File(Common.getTemporaryFilename());
                        Common.writeTextFile(tmpKey, new String(list.getSshKey(), "UTF-8"));
                    }
                    FileSystemOptions opts = VFSUtils.setUpConnection(list.getUsername(), list.getPassword(), list.isUserDirIsRoot(), tmpKey, timeout, proxyServer);

                    // Create the destination to send this file to

                    destFile = fsManager.resolveFile(destination, opts);

                    // Check if we are allowed to actually publish this

                    if (HibernateUtils.getSystemSetting(HibernateUtils.SETTING_PUBLISHING_ENABLED, HibernateUtils.SETTING_PUBLISHING_ENABLED_DEFAULT)) {
                        writeFileContent(destFile, srcFile, recip.getDescriptiveName());
                        publishingEnabled=true;
                    }
                    else {
                        toAddress = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_DEFAULT_VFS_PUBLISHER_ADDRESS, HibernateUtils.SETTING_DEFAULT_VFS_PUBLISHER_ADDRESS_DEFAULT);
                        if (!Common.isBlank(toAddress)) {
                            destFile = fsManager.resolveFile(toAddress + '.' + task.getOutputType().toLowerCase(), opts);
                            writeFileContent(destFile, srcFile, recip.getDescriptiveName());
                            publishingEnabled=true;
                            publishingSimulated=true;
                        }
                    }

                    if (publishingEnabled) {
                        if (publishingSimulated)
                            logger.debug("Publishing turned off - writing file to {}", destination);
                        else
                            logger.debug("Writing file to {}", destination);
                    }
                    else {
                        logger.debug("Publishing turned off - not writing file");
                    }
                }
            }
            catch (Exception e) {
                logger.error("Problem publishing report to {} - {}", destination, PivotalException.getErrorMessage(e));
                errors.add(PivotalException.getErrorMessage(e));
            }
            finally {
                // close connection
                VFSUtils.closeManager(fsManager);
            }
        }
        catch (Exception e) {
            logger.error("Problem exporting report to {} - {}", tmpFile, PivotalException.getErrorMessage(e));
            errors.add(PivotalException.getErrorMessage(e));
        }

        // Tidy up after ourselves

        File tmp=new File(tmpFile);
        if (tmp.exists()) tmp.delete();
        if (tmpKey!=null && tmpKey.exists()) tmpKey.delete();

        // Add a log entry to say we have completed the job

        String rider="";
        if (publishingEnabled) {
            if (publishingSimulated) rider=" PUBLISHING OFF - " + toAddress;
        }
        else
            rider=" PUBLISHING OFF";
        LogEntity.addLogEntry(LogEntity.STATUS_REPORT_PUBLISHED + rider, getClass().getSimpleName(), startTime, task, report, recipients);

        // If we have some errors then we need to do something about that

        if (!Common.isBlank(errors)) throw new PivotalException(Common.join(errors, "\n"));
    }

    /**
     * This method decides whether to create an empty file
     * or to copy the content from srcFile into the destFile
     *
     * @param destFile the file to be written
     * @param srcFile the file to be copied from
     * @param descriptiveName if it has the value "empty" the file created will be empty
     * @throws FileSystemException if unable to write the file
     */
    private static void writeFileContent(FileObject destFile, FileObject srcFile, String descriptiveName) throws FileSystemException {

        // if the descriptive name is "empty" it means that an empty file should be created
        if (CREATE_EMPTY_FILE_PARAM.equalsIgnoreCase(descriptiveName)) {
            if (destFile.exists()) {
                destFile.delete();
            }
            destFile.createFile();
        }
        else {
            destFile.copyFrom(srcFile, Selectors.SELECT_ALL);
        }
    }

    /**
     * Get the current timeout value
     *
     * @return Timeout value - can be null for default
     */
    public Integer getTimeout() {
        return timeout;
    }

    /**
     * Sets the timeout to use for the connection
     *
     * @param timeout Time out in millieseconds
     */
    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    /**
     * Get the proxy server in "server:port" mode
     *
     * @return Server to use
     */
    @SuppressWarnings("unused")
    public String getProxyServer() {
        return proxyServer;
    }

    /**
     * Sets the proxy server in "server:port" format
     *
     * @param proxyServer Proxy name
     */
    @SuppressWarnings("unused")
    public void setProxyServer(String proxyServer) {
        this.proxyServer = proxyServer;
    }
}
