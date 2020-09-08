/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.email;

import com.pivotal.reporting.publishing.Recipient;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.pivotal.utils.Common.isBlank;
import static com.pivotal.utils.Common.split;

/**
 * Email Object
 */
public class Email implements java.io.Serializable {

    private static final Logger logger = LoggerFactory.getLogger(Email.class);

    private List<Recipient> toList;
    private List<Recipient> ccList = null;
    private List<Recipient> bccList = null;
    private String subject;
    private String message;
    private String fromAddress;
    private List<Integer> mediaIds = null;
    private List<Attachment> attachments = null;
    private String sensitivity = null;
    private String priority = null;
    private String importance = null;
    private Integer distListId = null;
    private Integer parentId = null;
    private String parentType = null;
    private boolean landscape = false;
    private String reportName = null;
    private Map<String, Object>reportSettings = new HashMap<>();

    /**
     * Minimum instantiator for the class
     *
     * Used so it can be loaded one property at a time
     */
    public Email() {
        logger.debug("Initialised email");
    }

    /**
     * Full instantiator for the class
     *
     * @param toList List of To recipients
     * @param ccList List of CC recipients
     * @param bccList List of BCC recipients
     * @param subject Subject of email
     * @param message Email message
     * @param fromAddress From Address
     * @param mediaIds List of media ids to add as attachments
     */
    public Email(List<Recipient>toList, List<Recipient>ccList, List<Recipient>bccList, String subject, String message, String fromAddress, List<Integer> mediaIds) {
        this.toList = toList;
        this.ccList = ccList;
        this.bccList = bccList;
        this.subject = subject;
        this.message = message;
        this.fromAddress = fromAddress;
        this.mediaIds = mediaIds;
    }

    /**
     * Small instantiator for the class
     *
     * @param toEmail Email addresses to send email to; separated by ;
     * @param subject Subject of email
     * @param message Email message
     */
    public Email(String toEmail, String subject, String message) {
        List<Recipient>toList = new ArrayList<>();
        if (!isBlank(toEmail)) {
            for(String address : split(toEmail,";"))
                toList.add(new Recipient(address, address));
        }
        this.toList = toList;
        this.subject = subject;
        this.message = message;
        this.fromAddress = getDefaultFromAddress();
        logger.debug("Creating email for {}", toEmail);
    }

    public List<Recipient> getToList() {
        return toList;
    }

    public void setToList(List<Recipient> toList) {
        this.toList = toList;
    }

    public List<Recipient> getCcList() {
        return ccList;
    }

    public void setCcList(List<Recipient> ccList) {
        this.ccList = ccList;
    }

    public List<Recipient> getBccList() {
        return bccList;
    }

    public void setBccList(List<Recipient> bccList) {
        this.bccList = bccList;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Integer> getMediaIds() {
        return mediaIds;
    }

    public void setMediaIds(List<Integer> mediaIds) {
        this.mediaIds = mediaIds;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public Integer getDistListId() {
        return distListId;
    }

    public void setDistListId(Integer distListId) {
        this.distListId = distListId;
    }

    public String getSensitivity() {
        return sensitivity;
    }

    public void setSensitivity(String sensitivity) {
        this.sensitivity = sensitivity;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getImportance() {
        return importance;
    }

    public void setImportance(String importance) {
        this.importance = importance;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParent(Object parent) {

        boolean done = false;
        if (parent != null) {
            try {
                this.parentType = parent.getClass().getName();
                this.parentId = ClassUtils.invokeMethod(parent, "getId");
                done = true;
            }
            catch(Exception e) {
                logger.error("Unable to getId for email parent {}", e.getMessage());
            }
        }
        if(!done) {
            this.parentId = null;
            this.parentType = null;
        }
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public Map<String, Object> getReportSettings() {
        return reportSettings;
    }

    public void setReportSettings(Map<String, Object> reportSettings) {
        this.reportSettings = reportSettings;
    }

    public void addReportSetting(String name, Object value) {
        if(name != null) {
            this.reportSettings.put(name, value);
        }
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public String getParentType() {
        return parentType;
    }

    public void setParentType(String parentType) {
        this.parentType = parentType;
    }

    public boolean isLandscape() {
        return landscape;
    }

    public void setLandscape(boolean landscape) {
        this.landscape = landscape;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    public List<Attachment> addAttachment(String descriptiveFilename, String filename) {
        if (isBlank(this.attachments))
            attachments = new ArrayList<>();

        if (!isBlank(filename)) {
            attachments.add(new Attachment(descriptiveFilename, filename));
        }

        return this.attachments;
    }

    /**
     * Adds a recipient to list
     *
     * @param email Full email of the recipient e.g. pwallace@pivotal-solutions.co.uk
     * @param descriptiveName Descriptive name of the recipient e.g. paul wallace
     */
    public void addTo(String email, String descriptiveName) {
        if (toList == null)
            toList = new ArrayList<>();

        toList.add(new Recipient(email, descriptiveName));
    }

    /**
     * Adds a recipient to list
     *
     * @param email Full email of the recipient e.g. pwallace@pivotal-solutions.co.uk
     * @param descriptiveName Descriptive name of the recipient e.g. paul wallace
     */
    public void addCc(String email, String descriptiveName) {
        if (ccList == null)
            ccList = new ArrayList<>();

        ccList.add(new Recipient(email, descriptiveName));
    }

    /**
     * Adds a recipient to list
     *
     * @param email Full email of the recipient e.g. pwallace@pivotal-solutions.co.uk
     * @param descriptiveName Descriptive name of the recipient e.g. paul wallace
     */
    public void addBcc(String email, String descriptiveName) {

        if (bccList == null)
            bccList = new ArrayList<>();

        bccList.add(new Recipient(email, descriptiveName));
    }

    private static String getDefaultFromAddress() {
        return HibernateUtils.getSystemSetting(HibernateUtils.SETTING_EMAIL_SERVER_FROM, "");
    }

    public static class Attachment implements java.io.Serializable {
        private String descriptiveFilename;
        private String filename;

        Attachment(String descriptiveFilename, String filename) {
            this.descriptiveFilename = isBlank(descriptiveFilename)?filename:descriptiveFilename;
            this.filename = filename;
        }

        public String getDescriptiveFilename() {
            return descriptiveFilename;
        }

        public void setDescriptiveFilename(String descriptiveFilename) {
            this.descriptiveFilename = descriptiveFilename;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }
    }
}
