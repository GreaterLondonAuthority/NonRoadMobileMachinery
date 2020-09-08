/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.email;

import com.google.common.collect.Lists;
import com.pivotal.nrmm.service.notification.Notification;
import com.pivotal.reporting.publishing.Recipient;
import com.pivotal.system.hibernate.entities.MediaEntity;
import com.pivotal.system.hibernate.entities.ReportTextEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.monitoring.EmailMonitor;
import com.pivotal.system.monitoring.Monitor;
import com.pivotal.system.security.CaseManager;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;
import com.pivotal.utils.VelocityUtils;
import com.pivotal.utils.workflow.WorkflowHelper;
import com.pivotal.web.controllers.utils.JsonResponse;
import com.pivotal.web.notifications.NotificationManager;
import com.pivotal.web.servlet.ServletHelper;
import org.apache.velocity.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.InitialContext;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.pivotal.system.security.CaseManager.getMediaFile;
import static com.pivotal.utils.Common.*;

/**
 *
 */
public class EmailManager extends Monitor {

    private static final Logger logger = LoggerFactory.getLogger(EmailManager.class);

    @Override
    public void runTask() {

    }

//    /**
//     * Sends emails
//     *
//     * @param recipients list of recipients for the email
//     * @param subject    email subject line
//     * @param inMessage  email message body
//     * @param parent     object to save the email output to
//     * @return true if email sent
//     */
//    private static boolean sendEmail(List<Recipient> recipients, String subject, String inMessage, Object parent) {
//        return sendEmail(recipients, null, null, subject, inMessage, null, null, parent, false);
//    }

//    /**
//     * Sends emails
//     *
//     * @param emailAddress     email address to send email to
//     * @param emailDescription description of email address
//     * @param subject          email subject line
//     * @param inMessage        email message body
//     * @param fromAddress      From email address
//     * @param parent           object to save the email output to
//     * @return true if email sent
//     */
//    public static boolean sendEmail(String emailAddress, String emailDescription, String subject, String inMessage, String fromAddress, Object parent) {
//
//        List<Recipient> recipients = new ArrayList<>();
//
//        String[] addresses = emailAddress.split(" *[,;\r\n]+ *");
//        String[] descriptions = emailDescription.split(" *[,;\r\n]+ *");
//        String thisDesc;
//        for (int index = 0; index < addresses.length; index++) {
//            if (index < descriptions.length)
//                thisDesc = descriptions[index];
//            else
//                thisDesc = addresses[index];
//
//            recipients.add(new Recipient(addresses[index], thisDesc));
//        }
//
//        return sendEmail(recipients, null, null, subject, inMessage, null, fromAddress, parent, false);
//    }
//
//    /**
//     * Sends emails
//     *
//     * @param toList      list of recipients for the email
//     * @param ccList      cc list of recipients for the email
//     * @param bccList     bcc list of recipients for the email
//     * @param subject     email subject line
//     * @param inMessage   email message body
//     * @param mediaIds    Comma separated list of media Ids
//     * @param fromAddress Return email address for the email
//     * @param parent      object to save the email output to
//     * @param landscape   If true then email is saved to the parent in landscape
//     * @return true if email sent
//     */
//    public static boolean sendEmail(List<Recipient> toList, List<Recipient> ccList, List<Recipient> bccList, String subject, String inMessage, String fromAddress, List<Integer> mediaIds, Object parent, boolean landscape) {
//
//        Email email = new Email(toList, ccList, bccList, subject, inMessage, fromAddress, mediaIds);
//        email.setParent(parent);
//        email.setLandscape(landscape);
//
//        return sendEmail(email);
//    }

    /**
     * Helper to get email object
     *
     * @return Empty Email object
     */
    public static Email startEmail() {

        return new Email();
    }

    /**
     * Sends email now
     *
     * @param email email object to be sent
     *
     * @return true if email has been sent
     */
    public static boolean sendEmail(Email email) {
        return sendEmail(email, null);
    }

    /**
     * Sends email now
     *
     * @param email email object to be sent
     * @param passedSession Session to use
     *
     * @return true if email has been sent
     */
    public static boolean sendEmailSession(Email email, Session passedSession) {

        boolean retValue = false;
        String error;

        if (email != null) {

            if (!isBlank(email.getReportName())) {
                // User has specified report to generate the message with
                logger.debug("Generating email message with report (session) " + email.getReportName());
                email.setMessage(email.getReportName());
            }

            logger.debug("Sending email " + (Common.isBlank(email.getMessage()) ? "<blank message>" : email.getMessage()));

            try {
                boolean publishingEnabled = false;
                boolean publishingSimulated = false;
                String simulationAddress = null;
                if (HibernateUtils.getSystemSetting(HibernateUtils.SETTING_PUBLISHING_ENABLED, HibernateUtils.SETTING_PUBLISHING_ENABLED_DEFAULT))
                    publishingEnabled = true;
                else {
                    simulationAddress = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_DEFAULT_EMAIL_PUBLISHER_ADDRESS, "");
                    if (!Common.isBlank(simulationAddress)) {
                        publishingEnabled = true;
                        publishingSimulated = true;
                    }
                }

                if (publishingEnabled) {

                    if (!Common.isBlank(email.getToList()) || !Common.isBlank(email.getCcList()) || !Common.isBlank(email.getBccList())) {

                        // See if we can use the sender that we were passed, otherwise make our own

//                        JavaMailSenderImpl sender;
//                        if (passedSender == null || passedSender.getSession() != null)
//                            sender = connectEmailServer();
//                        else
//                            sender = passedSender;

                        if (passedSession != null) {

                            // See if we need to use a different from address

                            String emailFrom = setEmailFrom(email.getFromAddress());

                            passedSession.getProperties().setProperty("mail.smtp.from", emailFrom);
//                            passedSession.getProperties().setProperty("mail.smtp.user", emailFrom);

                            // Create a message and use the first recipient as the main destination

                            MimeMessage message = new MimeMessage(passedSession);
                            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                            // Create toList internet addresses
                            List<InternetAddress> toInternetAddrs = null;
                            List<String> displayToList = new ArrayList<>();
                            if (!Common.isBlank(email.getToList())) {
                                toInternetAddrs = getInternetAddressesFromRecipients(email.getToList());
                                displayToList = getDisplayAddresses(email.getToList());
                                if (!publishingSimulated)
                                    helper.setTo(toInternetAddrs.toArray(new InternetAddress[0]));
                                else if (!isBlank(simulationAddress))
                                    helper.setTo(simulationAddress);
                            }

                            // Create ccList internet addresses
                            List<InternetAddress> ccInternetAddrs = null;
                            List<String> displayCcList = new ArrayList<>();
                            if (!Common.isBlank(email.getCcList())) {
                                ccInternetAddrs = getInternetAddressesFromRecipients(email.getCcList());
                                displayCcList = getDisplayAddresses(email.getCcList());
                                if (!publishingSimulated)
                                    helper.setCc(ccInternetAddrs.toArray(new InternetAddress[0]));
                            }

                            // Create bccList internet addresses
                            List<InternetAddress> bccInternetAddrs = null;
                            List<String> displayBccList = new ArrayList<>();
                            if (!Common.isBlank(email.getBccList())) {
                                bccInternetAddrs = getInternetAddressesFromRecipients(email.getBccList());
                                displayBccList = getDisplayAddresses(email.getBccList());
                                if (!publishingSimulated)
                                    helper.setBcc(bccInternetAddrs.toArray(new InternetAddress[0]));
                            }

                            helper.setSubject(email.getSubject());
                            helper.setFrom(emailFrom);
                            helper.setSentDate(new Date());
                            helper.setValidateAddresses(false);

                            // Process the inline images and Set the body of the message

                            processInlineImages(email.getMessage(), helper);

                            // Load in media attachments if any

                            List<String> attachmentDescriptions = new ArrayList<>();
                            if (!Common.isBlank(email.getMediaIds())) {
                                List<MediaEntity> mediaEntities;

                                // Loop through and get entities individually to retain order they were added

                                for (Integer mediaId : email.getMediaIds()) {
                                    mediaEntities = HibernateUtils.selectEntities("From MediaEntity where id = " + mediaId, true);
                                    if (!Common.isBlank(mediaEntities)) {

                                        File tmpFile = null;
                                        try {
                                            tmpFile = CaseManager.getMediaFile(mediaEntities.get(0));
                                            helper.addAttachment(mediaEntities.get(0).getFilename(), tmpFile);
                                            attachmentDescriptions.add(mediaEntities.get(0).getFilename());
                                        }
                                        catch (Exception e) {
                                            error = String.format("Error adding media attachment to email - %s", PivotalException.getErrorMessage(e));
                                            logger.error(error);
                                            NotificationManager.addNotification(error, Notification.NotificationLevel.Error);
                                        }
                                        finally {
                                            Common.addFileForDeletion(tmpFile);
                                        }
                                    }
                                }
                            }

                            // Load in attached files, if any
                            if (!Common.isBlank(email.getAttachments())) {

                                for(Email.Attachment attachment : email.getAttachments()) {
                                    File tmpFile;
                                    try {
                                        tmpFile = new File(attachment.getFilename());
                                        helper.addAttachment(attachment.getDescriptiveFilename(), tmpFile);
                                        attachmentDescriptions.add(attachment.getDescriptiveFilename());
                                    }
                                    catch (Exception e) {
                                        error = String.format("Error adding file attachment to email - %s", PivotalException.getErrorMessage(e));
                                        logger.error(error);
                                        NotificationManager.addNotification(error, Notification.NotificationLevel.Error);
                                    }
                                    finally {
//                                        Common.addFileForDeletion(tmpFile);
                                    }
                                }
                            }

                            String destinationTo = join(displayToList, "; ");
                            if (publishingSimulated)
                                logger.debug("Publishing turned off - sending to {} - Sending message to {} with subject [{}]", simulationAddress, destinationTo, message.getSubject());
                            else {
                                String destinationCc = join(displayCcList, "; ");
                                String destinationBcc = join(displayBccList, "; ");
                                logger.debug("Sending message to " + destinationTo + (Common.isBlank(destinationCc) ? "" : " cc:" + destinationCc) + (Common.isBlank(destinationBcc) ? "" : " bcc:" + destinationBcc) + " with subject [" + message.getSubject() + ']');
                            }

                            // Send the message

                            Transport.send(message);

                            logger.debug("Message sent " + destinationTo);

                            retValue = true;
                        }
                        else {
                            error = "Email not sent as the application could connect to the mail server";
                            logger.debug(error);
                            NotificationManager.addNotification(error, Notification.NotificationLevel.Error);
                        }
                    }
                    else {
                        error = "Email not sent as no recipients were specified";
                        logger.debug(error);
                        NotificationManager.addNotification(error, Notification.NotificationLevel.Error);
                    }
                }
                else {
                    error = "Email not sent as publishing not enabled or default publisher address not specified";
                    logger.debug(error);
                    NotificationManager.addNotification(error, Notification.NotificationLevel.Error);
                }
            }
            catch (Exception e) {
                error = String.format("Problem sending email - %s", PivotalException.getErrorMessage(e));
                logger.error(error);
                NotificationManager.addNotification(error, Notification.NotificationLevel.Error);
            }
        }
        else {
            error = "Unable to send email as details are blank";
            logger.error(error);
            NotificationManager.addNotification(error, Notification.NotificationLevel.Error);
        }

        return retValue;


    }

    /**
     * Sends email now
     *
     * @param email email object to be sent
     * @param passedSender Sender to use
     *
     * @return true if email has been sent
     */
    public static boolean sendEmail(Email email, JavaMailSenderImpl passedSender) {

        boolean retValue = false;
        String error;
        boolean emailOk = false;

        if (email != null) {

            if (!isBlank(email.getReportName())) {
                // User has specified report to generate the message with
                logger.debug("Generating email message with report " + email.getReportName());
                ReportTextEntity reportTextEntity = HibernateUtils.getEntity(ReportTextEntity.class, email.getReportName());
                if (!isBlank(reportTextEntity.getText())) {

                    String reportText = reportTextEntity.getText().replaceAll("\\%24","\\$");
                    JsonResponse reportResult = WorkflowHelper.executeScript(reportText, email.getReportName(), email.getReportSettings(), true);
                    if (reportResult.getInError())
                        logger.error("Error running report for email {} - {}", email.getReportName(), reportResult.getError());
                    else if (isBlank(reportResult.getInformation()))
                        logger.error("Error running report for email {} - No content was returned", email.getReportName());
                    else {
                        email.setMessage(reportResult.getInformation().trim());
                        emailOk = true;
                    }
                }
                logger.debug("Generated text for email");
            }
            else
                emailOk = true;


            if (emailOk) {
                logger.debug("Sending email " + (Common.isBlank(email.getMessage()) ? "<blank message>" : email.getMessage()));
                try {
                    boolean publishingEnabled = false;
                    boolean publishingSimulated = false;
                    String simulationAddress = null;
                    if (HibernateUtils.getSystemSetting(HibernateUtils.SETTING_PUBLISHING_ENABLED, HibernateUtils.SETTING_PUBLISHING_ENABLED_DEFAULT))
                        publishingEnabled = true;
                    else {
                        simulationAddress = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_DEFAULT_EMAIL_PUBLISHER_ADDRESS, "");
                        if (!Common.isBlank(simulationAddress)) {
                            publishingEnabled = true;
                            publishingSimulated = true;
                        }
                    }

                    if (publishingEnabled) {

                        if (!Common.isBlank(email.getToList()) || !Common.isBlank(email.getCcList()) || !Common.isBlank(email.getBccList())) {

                            // See if we can use the sender that we were passed, otherwise make our own

                            JavaMailSenderImpl sender;
                            if (passedSender == null || passedSender.getSession() != null)
                                sender = connectEmailServer();
                            else
                                sender = passedSender;

                            if (sender.getSession() != null) {

                                // See if we need to use a different from address

                                String emailFrom = setEmailFrom(email.getFromAddress());

                                sender.getJavaMailProperties().setProperty("mail.smtp.from", emailFrom);
                                sender.getJavaMailProperties().setProperty("mail.smtp.user", emailFrom);

                                // Create a message and use the first recipient as the main destination

                                MimeMessage message = sender.createMimeMessage();
                                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                                // Create toList internet addresses
                                List<InternetAddress> toInternetAddrs = null;
                                List<String> displayToList = new ArrayList<>();
                                if (!Common.isBlank(email.getToList())) {
                                    toInternetAddrs = getInternetAddressesFromRecipients(email.getToList());
                                    displayToList = getDisplayAddresses(email.getToList());
                                    if (!publishingSimulated)
                                        helper.setTo(toInternetAddrs.toArray(new InternetAddress[0]));
                                    else if (!isBlank(simulationAddress))
                                        helper.setTo(simulationAddress);
                                }

                                // Create ccList internet addresses
                                List<InternetAddress> ccInternetAddrs = null;
                                List<String> displayCcList = new ArrayList<>();
                                if (!Common.isBlank(email.getCcList())) {
                                    ccInternetAddrs = getInternetAddressesFromRecipients(email.getCcList());
                                    displayCcList = getDisplayAddresses(email.getCcList());
                                    if (!publishingSimulated)
                                        helper.setCc(ccInternetAddrs.toArray(new InternetAddress[0]));
                                }

                                // Create bccList internet addresses
                                List<InternetAddress> bccInternetAddrs = null;
                                List<String> displayBccList = new ArrayList<>();
                                if (!Common.isBlank(email.getBccList())) {
                                    bccInternetAddrs = getInternetAddressesFromRecipients(email.getBccList());
                                    displayBccList = getDisplayAddresses(email.getBccList());
                                    if (!publishingSimulated)
                                        helper.setBcc(bccInternetAddrs.toArray(new InternetAddress[0]));
                                }

                                helper.setSubject(email.getSubject());
                                helper.setFrom(emailFrom);
                                helper.setSentDate(new Date());
                                helper.setValidateAddresses(false);

                                // Process the inline images and Set the body of the message

                                processInlineImages(email.getMessage(), helper);

                                // Load in media attachments if any

                                List<String> attachmentDescriptions = new ArrayList<>();
                                if (!Common.isBlank(email.getMediaIds())) {
                                    List<MediaEntity> mediaEntities;

                                    // Loop through and get entities individually to retain order they were added

                                    for (Integer mediaId : email.getMediaIds()) {
                                        mediaEntities = HibernateUtils.selectEntities("From MediaEntity where id = " + mediaId, true);
                                        if (!Common.isBlank(mediaEntities)) {

                                            File tmpFile = null;
                                            try {
                                                tmpFile = CaseManager.getMediaFile(mediaEntities.get(0));
                                                helper.addAttachment(mediaEntities.get(0).getFilename(), tmpFile);
                                                attachmentDescriptions.add(mediaEntities.get(0).getFilename());
                                            }
                                            catch (Exception e) {
                                                error = String.format("Error adding media attachment to email - %s", PivotalException.getErrorMessage(e));
                                                logger.error(error);
                                                NotificationManager.addNotification(error, Notification.NotificationLevel.Error);
                                            }
                                            finally {
                                                Common.addFileForDeletion(tmpFile);
                                            }
                                        }
                                    }
                                }

                                // Load in attached files, if any
                                if (!Common.isBlank(email.getAttachments())) {

                                    for (Email.Attachment attachment : email.getAttachments()) {
                                        File tmpFile;
                                        try {
                                            tmpFile = new File(attachment.getFilename());
                                            helper.addAttachment(attachment.getDescriptiveFilename(), tmpFile);
                                            attachmentDescriptions.add(attachment.getDescriptiveFilename());
                                        }
                                        catch (Exception e) {
                                            error = String.format("Error adding file attachment to email - %s", PivotalException.getErrorMessage(e));
                                            logger.error(error);
                                            NotificationManager.addNotification(error, Notification.NotificationLevel.Error);
                                        }
                                        finally {
                                            //                                        Common.addFileForDeletion(tmpFile);
                                        }
                                    }
                                }

                                String destinationTo = join(displayToList, "; ");
                                if (publishingSimulated)
                                    logger.debug("Publishing turned off - sending to {} - Sending message to {} with subject [{}]", simulationAddress, destinationTo, message.getSubject());
                                else {
                                    String destinationCc = join(displayCcList, "; ");
                                    String destinationBcc = join(displayBccList, "; ");
                                    logger.debug("Sending message to " + destinationTo + (Common.isBlank(destinationCc) ? "" : " cc:" + destinationCc) + (Common.isBlank(destinationBcc) ? "" : " bcc:" + destinationBcc) + " with subject [" + message.getSubject() + ']');
                                }

                                // Send the message

                                sender.send(message);
                                retValue = true;

                            }
                            else {
                                error = "Email not sent as the application could connect to the mail server";
                                logger.debug(error);
                                NotificationManager.addNotification(error, Notification.NotificationLevel.Error);
                            }
                        }
                        else {
                            error = "Email not sent as no recipients were specified";
                            logger.debug(error);
                            NotificationManager.addNotification(error, Notification.NotificationLevel.Error);
                        }
                    }
                    else {
                        error = "Email not sent as publishing not enabled or default publisher address not specified";
                        logger.debug(error);
                        NotificationManager.addNotification(error, Notification.NotificationLevel.Error);
                    }
                }
                catch (Exception e) {
                    error = String.format("Problem sending email - %s", PivotalException.getErrorMessage(e));
                    logger.error(error);
                    NotificationManager.addNotification(error, Notification.NotificationLevel.Error);
                }
            }
        }
        else {
            error = "Unable to send email as details are blank";
            logger.error(error);
            NotificationManager.addNotification(error, Notification.NotificationLevel.Error);
        }

        return retValue;
    }

    /**
     * Adds email to queue
     *
     * @param email email object to be queued
     */
    public static void queueEmail(Email email) {

        EmailMonitor.addEmail(email);
    }

    /**
     * Connects to the email server as defined in the settings
     *
     * @return email sender Object
     */
    public static JavaMailSenderImpl connectEmailServer() {

        JavaMailSenderImpl sender = new JavaMailSenderImpl();

        String emailHost = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_EMAIL_SERVER_HOST, "");
        String emailUserName = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_EMAIL_SERVER_USERNAME, "");
        String emailPassword = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_EMAIL_SERVER_PASSWORD, "");
        String emailServerPort = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_EMAIL_SERVER_PORT, "");
        boolean isSSL = isYes(HibernateUtils.getSystemSetting(HibernateUtils.SETTING_EMAIL_SERVER_SSL, "false"));
        boolean isDebug = isYes(HibernateUtils.getSystemSetting(HibernateUtils.SETTING_EMAIL_SERVER_DEBUG, "false"));
        String emailFrom = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_EMAIL_SERVER_FROM, "");

        if (!Common.isBlank(emailHost)) {

            sender.setHost(emailHost);
            if (!Common.isBlank(emailUserName)) sender.setUsername(emailUserName);
            if (!Common.isBlank(emailPassword)) sender.setPassword(emailPassword);

            Properties mailProps = new Properties();
            if (Common.isBlank(emailUserName) && Common.isBlank(emailPassword))
                mailProps.setProperty("mail.smtp.auth", "false");
            else
                mailProps.put("mail.smtp.auth", "true");

            mailProps.put("mail.debug", isDebug ? "true" : "false");
            mailProps.put("mail.smtp.starttls.enable", isSSL ? "true" : "false");
            mailProps.put("mail.smtp.ssl.protocols", "TLSv1.2");
            mailProps.put("mail.smtp.EnableSSL.enable", isSSL ? "true" : "false");
            mailProps.put("mail.smtp.host", emailHost);
            sender.setProtocol(JavaMailSenderImpl.DEFAULT_PROTOCOL + (isSSL ? "s" : ""));

            if (!Common.isBlank(emailServerPort)) {
                mailProps.put("mail.smtp.port", emailServerPort);
                sender.setPort(Integer.valueOf(emailServerPort));
            }

            if (!Common.isBlank(emailFrom)) {
                mailProps.put("mail.smtp.from", emailFrom);
                mailProps.put("mail.smtp.user", emailFrom);
            }

            //Load the properties

            sender.setJavaMailProperties(mailProps);
            logger.debug("Connecting to email server {}", emailHost);
        }
        else
            logger.error("Unable to connect to email server as host not specified");

        return sender;
    }

    private static String setEmailFrom(String fromAddress) {

        String addressToUse;
        if (Common.isBlank(fromAddress))
            addressToUse = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_EMAIL_SERVER_FROM, "");
        else
            addressToUse = fromAddress;

        return addressToUse;
    }

    /**
     * Helper to convert a list of recipients into a list of Internet addresses.
     *
     * @param recipients List of recipients
     * @return List of InternetAddress.
     * @throws UnsupportedEncodingException Exception
     */
    private static List<InternetAddress> getInternetAddressesFromRecipients(List<?> recipients) throws UnsupportedEncodingException {
        List<InternetAddress> internetAddresses = Lists.newArrayList();

        // Make sure we actually have something on the given list
        if (!Common.isBlank(recipients)) {

            for (Object recipientObject : recipients) {
                if (recipientObject instanceof Recipient) {
                    Recipient recipient = (Recipient) (recipientObject);
                    if (!Common.isBlank(recipient.getName()))
                        internetAddresses.add(new InternetAddress(recipient.getName(), recipient.getDescriptiveName()));
                }
            }
        }
        return internetAddresses;
    }

    /**
     * Helper to convert a list of recipients into a list of display addresses.
     *
     * @param recipients List of recipients
     * @return List of display address strings Name &lt;Email Address&gt;
     */
    private static List<String> getDisplayAddresses(List<Recipient> recipients) {

        List<String> addresses = new ArrayList<>();

        // Make sure we actually have something on the given list
        if (!Common.isBlank(recipients)) {
            for (Recipient thisRecipient : recipients) {
                addresses.add(thisRecipient.getDescription());
            }
        }
        return addresses;
    }

    /**
     * Converts images in HTML to inline images
     * assumes the image link is back into the database in the format
     * http://server/nrmm/image/1037
     * Only looks at last two parts of the url
     * It then modifies the message content and adds it to the email
     * The text must be modified and added to the email before the images
     * are loaded in (seemed to be the only way to get it working with inline images)
     *
     * @param message HTML message to be updated
     * @param helper  MimeMessage helper to use to add the inline images
     */
    public static void processInlineImages(String message, MimeMessageHelper helper) {

        String convertedMessage = message;
        Map<String, File> imageList = new HashMap<>();

        if (!Common.isBlank(message)) {

            Pattern pattern = Pattern.compile("<img\\s[^>]*?src\\s*=\\s*['\\\"]([^'\\\"]*?)['\\\"][^>]*?>", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(message);
            StringBuffer stringBuffer = new StringBuffer();
            while (matcher.find()) {
                try {

                    if (matcher.groupCount() > 0) {
                        List<String> parts = Common.splitToList(matcher.group(1), "/");
                        if (parts.size() > 2) {
                            String contentId = String.format("%s_%s", parts.get(parts.size() - 2), parts.get(parts.size() - 1));
                            if (!imageList.containsKey(contentId)) {
                                File mediaFile = getMediaFile(HibernateUtils.getEntity(MediaEntity.class, Common.parseInt(parts.get(parts.size() - 1))));
                                if (!Common.isBlank(mediaFile)) {
                                    imageList.put(contentId, mediaFile);
                                    // Load into String Buffer
                                    matcher.appendReplacement(stringBuffer, matcher.group().replace(matcher.group(1), String.format("cid:%s", contentId)));
                                }
                            }
                        }
                    }
                }
                catch (Exception e) {
                    logger.error("Unable to add inline image, {}", PivotalException.getErrorMessage(e));
                }
            }
            matcher.appendTail(stringBuffer);

            convertedMessage = stringBuffer.toString();
        }

        try {
            helper.setText(Common.getBodyTextFromHtml(convertedMessage), convertedMessage);

            // Add images after adding text (seems to only work if done in this order)
            for (String contentId : imageList.keySet())
                helper.addInline(contentId, imageList.get(contentId));

        }
        catch (Exception e) {
            logger.error("Unable to set email text, {}", PivotalException.getErrorMessage(e));
        }
    }

    public static boolean isPublishingEnabled() {

        return isYes(HibernateUtils.getSystemSetting(HibernateUtils.SETTING_PUBLISHING_ENABLED, HibernateUtils.SETTING_PUBLISHING_ENABLED_DEFAULT));
    }

    public static boolean isPublishingSimulated() {

        return !isBlank(HibernateUtils.getSystemSetting(HibernateUtils.SETTING_DEFAULT_EMAIL_PUBLISHER_ADDRESS, ""));
    }

    /**
     * Generates the report text
     *
     * @param passedContextVariables  Map of context values to be used when rendering the template
     * @param passedText              Text of the report
     *
     * @return text of report
     */
    private static String generateReportText(Map<String, Object>passedContextVariables, String passedText) {

        String returnValue = "";

        Map<String, Object> contextVariables = ServletHelper.getGenericObjects(ServletHelper.getRequest(), ServletHelper.getResponse(), true);

        if(!isBlank(passedContextVariables))
            contextVariables.putAll(passedContextVariables);

        contextVariables.put("PathSeparator", File.pathSeparator);
        Context velocityContext = VelocityUtils.getVelocityContext();
        for (Object contextKey : velocityContext.getKeys())
            contextVariables.put(contextKey.toString(), velocityContext.get(contextKey.toString()));

        if (!isBlank(passedText)) {

            // Make sure all image tags are closed

            String text = passedText.replaceAll("(<img[^>]+)(?<!/)>", "$1></img>");

            // Make sure the initialise.inc is called

            if (!text.contains("initialise.inc"))
                text = "#macroPageHead(\"\")\r\n" + text;

            text = replaceSmartChars(text);

            contextVariables.put("CaseManager", CaseManager.class);
            returnValue = HibernateUtils.parseTemplate(text, contextVariables);
        }

        return returnValue.replaceAll("^[\r\n]+","");
    }
    /**
    * Gets the Email Session
    *
    * @return javamail session
    *
    * @throws java.lang.Exception Error
    */
   public static javax.mail.Session getEmailSession() {

       javax.mail.Session session = null;

       if (true) {
           try {
               javax.naming.Context initCtx = new InitialContext();
               javax.naming.Context envCtx = (javax.naming.Context) initCtx.lookup("java:comp/env");
               session = (javax.mail.Session) envCtx.lookup(HibernateUtils.MAIL_SETTINGS);
           }
           catch (Exception e) {
               logger.debug("Error getting session {}", PivotalException.getErrorMessage(e));
           }
           catch(Throwable t) {
               logger.debug("Error getting session {}", PivotalException.getErrorMessage(t));
           }
//           finally {
//               Common.close(initCtx);
//           }
       }
       else {
           try {
               Properties props = new Properties();
               Authenticator authenticator = new Authenticator() {
                   protected PasswordAuthentication getPasswordAuthentication() {
                       return new PasswordAuthentication("nrmm@pivotalmail.co.uk", "nrmm99");
                   }
               };

               props.put("mail.smtp.host", "smtp.pivotalmail.co.uk");
               props.put("mail.smtp.auth", "true");
               props.put("mail.smtp.user", "nrmm@pivotalmail.co.uk");
               props.put("mail.smtp.password", "nrmm99");
               props.put("mail.smtp.starttls.enable", "true");
               props.put("mail.transport.protocol", "smtps");

               logger.debug("Props = {}" + props);
               session = Session.getInstance(props, authenticator);
           }
           catch (Exception e) {
               logger.debug("Error getting session {}", PivotalException.getErrorMessage(e));
           }
           finally {
               logger.debug("Here we are");
           }
       }
        return session;
   }
}
