/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils.sources;

import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;
import org.apache.commons.lang.StringUtils;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Implements a mail info source
 */
public class EmailInfoSource extends InfoSource {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EmailInfoSource.class);

    Store store;
    Session session;
    String folder;
    String protocol;
    String host;
    int port=-1;

    /**
     * Creates an instance of the this information source for reading
     *
     * @param infosourceEntity Database entity for this source
     */
    public EmailInfoSource(InfosourceEntity infosourceEntity) {
        super(infosourceEntity);
    }

    /**
     * Opens the actual source
     * Implementors should make sure that they cache this list so that there
     * is a consistent set of entities to work between open/close calls
     *
     * @throws PivotalException Errors if the source can't be opened
     */
    @Override
    public void open() throws PivotalException {

        logger.debug("Opening VFS information source " + infosourceEntity);

        try {

            // Determine if a port has been specified on the host name

            if (Common.isBlank(folder)) folder="INBOX";
            protocol=infosourceEntity.getEmailType();
            if (Common.isBlank(protocol)) protocol="imap";
            host=infosourceEntity.getServer();
            if (host.indexOf(':')>0 && StringUtils.isNumeric(host.split(":")[1])) {
                port = Common.parseInt(host.split(":")[1]);
                host = host.split(":")[0];
            }

            // Get a mail session and setup some defaults

            Properties objProps = System.getProperties();
            objProps.setProperty("mail.mime.address.strict", "false");
            objProps.setProperty("mail.mime.decodetext.strict", "false");
            session = Session.getInstance(objProps, null);

            // Turn on debugging of the connection if we are logging to debug level

            //if (logger.isDebugEnabled()) session.setDebug(true);

            // Connect to the store

            store = session.getStore(protocol.toLowerCase());
            logger.debug("Connecting to host");
            store.connect(host, port, infosourceEntity.getUsername(), infosourceEntity.getPassword());

            // Wait for the connection

            boolean bBreak=false;
            long lTimeout = System.currentTimeMillis() + 10000;
            while (!store.isConnected() && !bBreak) {
                if (System.currentTimeMillis() > lTimeout) {
                    logger.debug("Connection timed out");
                    bBreak=true;
                }
                else
                    Thread.sleep(1000);
            }

            // Throw an error if we're not connected but get the error from the connection
            // attempt

            if (!store.isConnected())
                throw new PivotalException("A problem has occurred attempting to connect to a mail server\nusing the " + infosourceEntity + " account\n\nCheck that the connection parameters and username/password re-correct and try again\nFor more information, check the application log");
            else {

                // Open the folder

                Folder emailFolder = store.getDefaultFolder().getFolder(folder);
                emailFolder.open(Folder.READ_ONLY);

                // Copy all the messages down to local storage so that we can work on them here

                Message[] messages = emailFolder.getMessages();
                if (!Common.isBlank(messages)) {
                    logger.debug("Found " + messages.length + " messages to download");
                    for (int i=0; i<messages.length; i++) {
                        logger.debug("Downloading " + i + " of " + messages.length + " messages");
                        items.add(new InfosourceItem((MimeMessage)messages[i], infosourceEntity));
                    }
                }
                else
                    logger.debug("The folder is empty");
            }
        }
        catch (Exception e) {
            throw new PivotalException("Error connecting to server - " + PivotalException.getErrorMessage(e));
        }
        finally {
            Common.close(store);
        }
    }

    /**
     * Closes the source - doesn't do anything if the source
     * is not open
     */
    @Override
    public void close() {
        logger.debug("Closing Email [" + infosourceEntity + ']');

        // Clean off any local files we have used

        if (!Common.isBlank(items)) {
            for (InfoSourceItem item : items) {
                item.cleanLocalTempFiles();
            }
        }
    }

    /**
     * Implementation of am email item
     */
    protected class InfosourceItem extends InfoSourceItem {

        MimeMessage message;
        Address[] to;
        Address[] cc;
        Address[] from;
        String messageID;
        int messageNumber;

        /**
         * Creates a file object to use
         *
         * @param message Email message to create
         * @param infosourceEntity The item's infosource entity
         *
         * @throws MessagingException Messaging problems
         * @throws IOException IO Errors
         */
        InfosourceItem(MimeMessage message, InfosourceEntity infosourceEntity) throws MessagingException,IOException {


            this.infosourceEntity = infosourceEntity;
            // Enumerate the properties of the file
            this.message=message;
            properties.put("subject", message.getSubject());
            properties.put("body", Common.getBodyText(message));
            if (message.getSentDate() != null) {
                properties.put("sentdate", message.getSentDate().getTime() + "");
            }
            if (message.getReceivedDate() != null) {
                properties.put("receiveddate", message.getReceivedDate().getTime() + "");
            }
            properties.put("messageno", message.getMessageNumber() + "");
            properties.put("unread", message.isSet(Flags.Flag.SEEN)?"false":"true");

            // Now get some of the more complex stuff

            name=message.getSubject();
            lastModified=message.getSentDate();
            size=message.getSize();
            extension="eml";
            to=Common.getRecipients(message, Message.RecipientType.TO);
            cc=Common.getRecipients(message, Message.RecipientType.CC);
            from=message.getFrom();
            messageID=Common.getMessageID(message);
            messageNumber=message.getMessageNumber();
            logger.debug("Retrieved message [" + message.getSubject() + ']');

            // Download the file to a local copy for processing later

            File tmpFile=new File(Common.getTemporaryFilename("eml"));
            logger.debug("Downloading message to local storage " + tmpFile);
            Common.pipeInputToOutputStream(message.getInputStream(), new FileOutputStream(tmpFile));
            localTempFiles.add(tmpFile);

            // Now we need to get all the attachments from the message and
            // add them to the list

            if (!Common.isBlank(message.getContentType()) && message.getContentType().toLowerCase().startsWith("multipart/mixed")) {

                // Get the parts from the message

                Multipart parts = (Multipart)message.getContent();
                logger.debug("Contains " + parts.getCount() + "parts, type is " + message.getContentType());

                // Loop through them - we're only interested in the ones that have a filename associated
                // with them to indicate that they are in fact attachments

                for (int iPart=0; iPart<parts.getCount(); iPart++) {
                    BodyPart part = parts.getBodyPart(iPart);
                    String filename=null;
                    if (part.getFileName()!=null)
                        filename=part.getFileName();
                    else if (part.getContentType().toLowerCase().startsWith("message/rfc822"))
                        filename=((Message)part.getContent()).getSubject() + ".msg";

                    // Create a local file of the attachment

                    if (!Common.isBlank(filename)) {
                        logger.debug("Found attachment in part " + iPart + " [" + filename + ']');
                        tmpFile=new File(Common.getTemporaryFilename("eml"));
                        Common.pipeInputToOutputStream(part.getInputStream(), new FileOutputStream(tmpFile));
                        localTempFiles.add(tmpFile);

                        // Add the attachment to the file list

                        files.add(new InfoSourceFile(tmpFile, properties,filename,
                                                     filename.replaceAll("^[^\\\\]*\\\\",""),filename.replaceAll("^[^.]*\\.",""),
                                                     tmpFile.length(), message.getReceivedDate(),infosourceEntity));
                    }
                }
            }
            else
                logger.debug("No attachments, type is " + message.getContentType());
        }

        /**
         * This method is responsible for deleting this object from wherever
         * it is stored
         *
         * @return Returns true if the item was successfully deleted
         */
        @Override
        public boolean delete() {
            try {
                // Attempt to connect to the store and open the source folder

                store.connect(host, port, infosourceEntity.getUsername(), infosourceEntity.getPassword());
                Folder srcFolder=store.getDefaultFolder().getFolder(folder);
                srcFolder.open(Folder.READ_WRITE);

                // Get the message

                MimeMessage message=Common.getMessageFromFolder(srcFolder, messageID, messageNumber);

                // Have we got the message

                if (message==null)
                    logger.warn("Message for ID:" + messageID + " number:" + messageNumber + " no longer exists");

                // We're only interested in those that are not already marked for deletion

                else if (!message.getFlags().contains(Flags.Flag.DELETED)) {

                    // Check to see if we can move the message

                    logger.debug("Attempting to move message [" + messageID + "] to deleted folder");
                    Folder deletedFolder = store.getFolder("Deleted Items");

                    // If the folder doesn't exist and we're on IMAP then try and create it

                    if (!deletedFolder.exists() && Common.doStringsMatch(protocol,"imap")) {
                        try {
                            deletedFolder.create(Folder.HOLDS_FOLDERS + Folder.HOLDS_MESSAGES);
                        }
                        catch (MessagingException e) {
                            logger.error("Message will be deleted because cannot create 'Deleted Items' folder to move it to - " + PivotalException.getErrorMessage(e));
                        }
                    }

                    // Move the message to the deleted folder if we can

                    if (deletedFolder.exists()) {
                        deletedFolder.open(Folder.READ_WRITE);
                        deletedFolder.appendMessages(new Message[] {message});
                        logger.debug("Message [" + messageID + "] moved to deleted folder successfully");
                    }

                    // Remove the message whatever happens

                    logger.debug("Deleting message [" + messageID + ']');
                    message.setFlag(Flags.Flag.DELETED, true);
                    srcFolder.close(true);
                }
            }
            catch (Exception e) {
                lastError="Problem deleting the email message [" + messageID + "] - " + PivotalException.getErrorMessage(e);
                logger.error(lastError);
            }
            finally {
                Common.close(store);
            }

            // Clean off any local files we have used

            cleanLocalTempFiles();
            return !isInError();
        }

        /**
         * Returns array of TO addresses
         *
         * @return Array of addresses
         */
        public Address[] getTo() {
            return to;
        }

        /**
         * Returns array of the CC addresses
         *
         * @return Array of addresses
         */
        public Address[] getCc() {
            return cc;
        }

        /**
         * Returns an array of all the from addresses - usually a single element array
         *
         * @return Array of addresses
         */
        public Address[] getFrom() {
            return from;
        }

        /**
         * Returns the unique identifier for this message
         *
         * @return String UID
         */
        protected String getMessageID() {
            return messageID;
        }
    }
}
