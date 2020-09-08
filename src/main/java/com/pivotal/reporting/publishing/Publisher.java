/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *****************************************************************************
 *
 * LANGUAGE:             Java jdk 1.6
 *
 * PACKAGE NAME:         com.pivotal.publishing
 *
 * MODULE TYPE:          Java Class
 *
 * FILE NAME:            Publisher.java
 *
 *
 *****************************************************************************
 */
package com.pivotal.reporting.publishing;

import com.pivotal.reporting.reports.Report;
import com.pivotal.reporting.scheduler.Job;
import com.pivotal.system.data.dao.DataSourceUtils;
import com.pivotal.system.hibernate.entities.DatasourceEntity;
import com.pivotal.system.hibernate.entities.DistributionListEntity;
import com.pivotal.system.hibernate.entities.ScheduledTaskEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.Common;
import com.pivotal.utils.PgpUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;


/**
 * This is the base class for all Publishers within NRMM and must be
 * extended by any class that is used to publish data
 */
public abstract class Publisher {

    /** Constant <code>REPORT_CONTENT_PLACEHOLDER="$ReportContent"</code> */
    public static final String REPORT_CONTENT_PLACEHOLDER = "$ReportContent";

    List<List<Recipient>> recipientList;
    List<Recipient> bccList;
    List<Recipient> ccList;
    DistributionListEntity list;
    ScheduledTaskEntity task;
    Date startTime;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Publisher.class);

    /**
     * Creates an instance of the this publishing channel for
     * sending emails
     *
     * @param task Associated scheduled task
     * @param list Distribution list from which to get connection details
     */
    protected Publisher(ScheduledTaskEntity task, DistributionListEntity list) {

        // First thing to do is enumerate the list

        startTime=new Date();
        this.task=task;
        this.list=list;
        enumerate();
    }

    /**
     * Sends the data to the channel
     *
     * @param recipients List of recipients to publish to
     */
    public void publish(List<Recipient> recipients) {
        publish(recipients, null);
    }

    /**
     * Sends the data to the channel
     *
     * @param report Report to publish (can be null)
     * @param recipients List of recipients to publish to
     */
    public void publish(Report report, List<Recipient> recipients) {
        publish(report, recipients, null);
    }

    /**
     * Sends the data to the channel
     *
     * @param recipients List of recipients to publish to
     * @param job Report job (if any) that is running this publishing
     */
    abstract public void publish(List<Recipient> recipients, Job job);

    /**
     * Sends the data to the channel
     *
     * @param report Report to publish (can be null)
     * @param recipients List of recipients to publish to
     * @param job Report job (if any) that is running this publishing
     */
    abstract public void publish(Report report, List<Recipient> recipients, Job job);

    /**
     * Enumerates all the recipients into a list that can be cycled
     * through
     * It has to check to see if this is a query for recipients and
     * use that to find all the possible names/locations
     */
    protected void enumerate() {

        // Determine the list of recipients by looking at the content
        // If it looks like a select, then we need to get the values from
        // a database, otherwise the recipients are specified as ; separated
        // values

        logger.debug("Enumerating the list of recipients for list {}", list.toString());
        if (Common.isBlank(list.getContent()))
            logger.debug("The distribution list for [{}] is empty", list.getName());
        else {

            // Parse the values for system variables

            String content= HibernateUtils.parseSystemVariables(list.getContent().trim(), task);

            // Is it a select statement

            if (content.matches("(?is)^\\s*select\\s.+")) {
                logger.debug("List is a select statement {}", list.toString());

                // Determine which datasource to use

                DatasourceEntity source=task.getDatasource();
                if (list.getDatasource()!=null) source=list.getDatasource();
                logger.debug("Using datasource for recipients {}", source.toString());

                // Get the recipients from the database

                List<LinkedHashMap<String,String>> names= DataSourceUtils.getResults(source, content, HibernateUtils.getSystemSetting(HibernateUtils.SETTING_MAX_RECIPIENTS, HibernateUtils.SETTING_MAX_RECIPIENTS_DEFAULT));
                if (Common.isBlank(names))
                    logger.debug("The distribution list for [{}] is empty", list.getName());
                else {
                    recipientList =new ArrayList<>();
                    if (list.isForeach()) {
                        for (LinkedHashMap<String,String> name : names) {

                            // We need to allow for the fact the row values coming back from the database may
                            // actually contain lists of recipients

                            List<Recipient> tmp=new ArrayList<>();
                            List<String> list=new ArrayList<>(name.values());
                            List<String> subNames=Common.splitToList(list.get(0), " *[;,\r\n]+ *");

                            // If we only have a single address then that's fine

                            if (subNames.size()==1) {
                                Recipient recip=new Recipient(name);
                                tmp.add(recip);
                                logger.debug("Added ForEach recipient {} to list", recip.getName());
                            }
                            else {

                                // Add the groups of addresses

                                for (String subName : subNames) {
                                    if (!Common.isBlank(subName)) {
                                        Recipient recip;
                                        if (subName.contains("|"))
                                            recip=new Recipient(subName.split("\\|")[0],subName.split("\\|",2)[1],name);
                                        else
                                            recip=new Recipient(subName,null,name);
                                        tmp.add(recip);
                                        logger.debug("Added recipient {} to list", recip.getName());
                                    }
                                    else {
                                        logger.warn("Ignoring empty recipient from list [{}]", list.get(0));
                                    }
                                }
                            }
                            recipientList.add(tmp);
                        }
                    }
                    else {
                        List<Recipient> tmp=new ArrayList<>();
                        for (LinkedHashMap<String,String> name : names) {
                            if (!Common.isBlank(name)) {
                                Recipient recip=new Recipient(name);
                                tmp.add(recip);
                                logger.debug("Added recipient {} to list", recip.getName());
                            }
                            else {
                                logger.warn("Ignoring empty recipient from results list");
                            }
                        }
                        recipientList.add(tmp);
                    }
                }
            }

            // Must be a list separated by ';' characters

            else {
                logger.debug("List is strings separated by ; - {}", content);
                List<String> names=Common.splitToList(content, " *[;,\r\n]+ *");
                if (Common.isBlank(names))
                    logger.debug("The distribution list for [{}] is empty", list.getName());
                else {
                    recipientList =new ArrayList<>();
                    if (list.isForeach()) {
                        for (String name : names) {
                            if (!Common.isBlank(name)) {
                                List<Recipient> tmp=new ArrayList<>();
                                Recipient recip;
                                if (name.contains("|"))
                                    recip=new Recipient(name.split("\\|")[0],name.split("\\|",2)[1]);
                                else
                                    recip=new Recipient(name);
                                tmp.add(recip);
                                logger.debug("Added recipient {} to list", recip.getName());
                                recipientList.add(tmp);
                            }
                            else {
                                logger.warn("Ignoring empty recipient from supplied list");
                            }
                        }
                    }
                    else {
                        List<Recipient> tmp=new ArrayList<>();
                        for (String name : names) {
                            if (!Common.isBlank(name)) {
                                Recipient recip;
                                if (name.contains("|"))
                                    recip=new Recipient(name.split("\\|")[0],name.split("\\|",2)[1]);
                                else
                                    recip=new Recipient(name);
                                tmp.add(recip);
                                logger.debug("Added recipient {} to list", recip.getName());
                            }
                            else {
                                logger.warn("Ignoring empty recipient from supplied list");
                            }
                        }
                        recipientList.add(tmp);
                    }
                }
            }

            // If we have a CC list then sort that out too

            if (!Common.isBlank(list.getEmailCc())) {
                ccList=new ArrayList<>();
                for (String name : Common.splitToList(list.getEmailCc(), " *[;,\r\n]+ *")) {
                    Recipient recip;
                    if (name.contains("|"))
                        recip=new Recipient(name.split("\\|")[0],name.split("\\|",2)[1]);
                    else
                        recip=new Recipient(name);
                    ccList.add(recip);
                    logger.debug("Added cc recipient {} to list", recip.getName());
                }
            }

            // If we have a BCC list then sort that out too

            if (!Common.isBlank(list.getEmailBcc())) {
                bccList=new ArrayList<>();
                for (String name : Common.splitToList(list.getEmailBcc(), " *[;,\r\n]+ *")) {
                    Recipient recip;
                    if (name.contains("|"))
                        recip=new Recipient(name.split("\\|")[0],name.split("\\|",2)[1]);
                    else
                        recip=new Recipient(name);
                    bccList.add(recip);
                    logger.debug("Added bcc recipient {} to list", recip.getName());
                }
            }
        }
    }

    /**
     * Returns a list of all the discrete recipients lists
     * It may be that recipients are grouped into separate lists
     * or that there is only a single list or more likely a list
     * of lists containing a single recipient
     *
     * @return List of Lists of recipients
     */
    public List<List<Recipient>> getRecipients() {
        return recipientList;
    }


    /**
     * Convenience method for working out what the name of the report file should be
     * based on the output type and whether it is compressed or not
     *
     * @param filenameBody filename given by the user
     * @return Full specified filename
     */
    protected String createReportFilename(String filenameBody) {

        String filename=filenameBody;

        // If the user hasn't specified an extension explicitly, then add on one that is appropriate
        // to the type of the file

        String fileExt = Common.getFilenameExtension(filenameBody);
        if (task!=null && Common.isBlank(fileExt))
            filename += (Common.isBlank(filenameBody)?task.getReport().getName():"") + '.' + task.getOutputType().toLowerCase();

        // Here's a nice subtle little feature - if the file extension is "nfe" then the file extension is actually removed
        // This is so that the user can stop the system automatically applying an extension if they REALLY don't want one

        else if (Common.doStringsMatch(fileExt, "nfe"))
            filename = filenameBody.replaceAll("(?is)\\.nfe$", "");

        // If output is to be PGP encrypted, then add ".gpg" extension

        if (list!=null && list.isPgpEncryptedOutput())
            filename+=".gpg";

        // If the output is compressed, then add on a ".z" extension

        if (list!=null && Common.doStringsMatch(list.getCompression(),"gzip"))
            filename+=".gz";

        else if (list!=null && Common.doStringsMatch(list.getCompression(),"compress"))
            filename+=".Z";

        return filename;
    }


    /**
     * Encrypt given file with pgp.
     *
     * @param decryptedFile File to encrypt.
     * @return Path to the encrypted file.
     * @throws java.io.IOException             If unable to handle IO.
     * @throws java.security.NoSuchProviderException If unable to add bouncy castle provider.
     */
    protected String encryptOutput(String decryptedFile) throws IOException, NoSuchProviderException {
        String res = null;
        InputStream keyIn = null;
        FileOutputStream out = null;
        try {
            //Call encrypt file and reset tempFile Value
            res = Common.getTemporaryFilename("tmp");
            Security.addProvider(new BouncyCastleProvider());
            keyIn = new ByteArrayInputStream(task.getDistributionList().getPgpPublicKey());
            out = new FileOutputStream(res);
            logger.debug("Starting Encryption...");
            PgpUtils.encryptFile(out, decryptedFile, keyIn, true, true);
            logger.debug("Encryption Done.");
        }
        finally {
            Common.close(keyIn, out);
        }
        return res;
    }
}
