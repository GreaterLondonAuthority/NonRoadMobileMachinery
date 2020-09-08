/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.hibernate.entities;

import com.pivotal.monitoring.utils.Definition;
import com.pivotal.monitoring.utils.DefinitionSettings;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

/**
 * Maps the distribution_list table to the DistributionListEntity lass
 */
@Table(name = "distribution_list")
@Entity
public class DistributionListEntity extends AbstractEntity implements Serializable {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DistributionListEntity.class);
    private static final long serialVersionUID = -8128473845901500797L;

    private static final String smsDefinitionXML = "<parameters>\n" +
            "   <parameter name='method' label='distributionlistentity.sms.method' description='distributionlistentity.sms.method.description' type='string' required='true' default='get'>\n" +
            "       <choices>\n" +
            "           <choice name='distributionlistentity.sms.method.get' value='get' />\n" +
            "           <choice name='distributionlistentity.sms.method.post' value='post' />\n" +
            "       </choices>\n" +
            "   </parameter>" +
            "   <parameter name='post_params' label='reporting.distributionlistentity.edit.sms.parameters.post_params' description='reporting.distributionlistentity.edit.sms.parameters.post_params.description' type='any' required='true' multiple='true'>\n" +
            "       <subparameters>\n" +
            "           <subparameter name='parameter' label='reporting.distributionlistentity.edit.sms.parameters.post_params.parameter' description='reporting.distributionlistentity.edit.sms.parameters.post_params.parameter.description' type='string'/>\n" +
            "           <subparameter name='value' label='reporting.distributionlistentity.edit.sms.parameters.post_params.value' description='reporting.distributionlistentity.edit.sms.parameters.post_params.value.description' type='string'/>\n" +
            "       </subparameters>\n" +
            "   </parameter>" +
            "   <parameter name='headers' label='reporting.distributionlistentity.edit.sms.parameters.headers' description='reporting.distributionlistentity.edit.sms.parameters.headers.description' type='any' required='true' multiple='true'>\n" +
            "       <subparameters>\n" +
            "           <subparameter name='header' label='reporting.distributionlistentity.edit.sms.parameters.headers.header' description='reporting.distributionlistentity.edit.sms.parameters.headers.header.description' type='string'/>\n" +
            "           <subparameter name='value' label='reporting.distributionlistentity.edit.sms.parameters.headers.value' description='reporting.distributionlistentity.edit.sms.parameters.headers.value.description' type='string'/>\n" +
            "       </subparameters>\n" +
            "   </parameter>" +
            "</parameters>";


    private Integer id;

    /**
     * <p>Getter for the field <code>id</code>.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    @Column(name = "id", nullable = false, length = 10, precision = 0)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer getId() {
        return id;
    }

    /**
     * <p>Setter for the field <code>id</code>.</p>
     *
     * @param id a {@link java.lang.Integer} object.
     */
    public void setId(Integer id) {
        this.id = id;
    }

    @NotBlank(message = "You must specify a name")
    private String name;

    /**
     * <p>Getter for the field <code>name</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "name", unique = true, nullable = false, length = 100, precision = 0)
    @Basic
    public String getName() {
        return name;
    }

    /**
     * <p>Setter for the field <code>name</code>.</p>
     *
     * @param name a {@link java.lang.String} object.
     */
    public void setName(String name) {
        this.name = name;
    }

    private String description;

    /**
     * <p>Getter for the field <code>description</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "description", length = 255, precision = 0)
    @Basic
    public String getDescription() {
        return description;
    }

    /**
     * <p>Setter for the field <code>description</code>.</p>
     *
     * @param description a {@link java.lang.String} object.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    @NotBlank(message = "You must specify a type")
    private String type;

    /**
     * <p>Getter for the field <code>type</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "type_id", nullable = false, length = 20, precision = 0)
    @Basic
    public String getType() {
        return type;
    }

    /**
     * <p>Setter for the field <code>type</code>.</p>
     *
     * @param type a {@link java.lang.String} object.
     */
    public void setType(String type) {
        this.type = type;
    }

    private String settingsXML;

    /**
     * <p>Getter for the field <code>settingsXML</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "settings", nullable = false, length = 65535, precision = 0)
    @Basic
    public String getSettingsXML() {
        return settingsXML;
    }

    /**
     * <p>Setter for the field <code>settingsXML</code>.</p>
     *
     * @param settingsXML a {@link java.lang.String} object.
     */
    public void setSettingsXML(String settingsXML) {
        this.settingsXML = settingsXML;
    }

    private boolean foreach;

    /**
     * <p>isForeach.</p>
     *
     * @return a boolean.
     */
    @Column(name = "foreach", length = 0, precision = 0)
    @Basic
    public boolean isForeach() {
        return foreach;
    }

    /**
     * <p>Setter for the field <code>foreach</code>.</p>
     *
     * @param foreach a boolean.
     */
    public void setForeach(boolean foreach) {
        this.foreach = foreach;
    }

    private boolean userDirIsRoot;

    /**
     * <p>isUserDirIsRoot.</p>
     *
     * @return a boolean.
     */
    @Column(name = "user_dir_is_root", length = 0, precision = 0)
    @Basic
    public boolean isUserDirIsRoot() {
        return userDirIsRoot;
    }

    /**
     * <p>Setter for the field <code>userDirIsRoot</code>.</p>
     *
     * @param user_dir_is_root a boolean.
     */
    public void setUserDirIsRoot(boolean user_dir_is_root) {
        this.userDirIsRoot = user_dir_is_root;
    }

    @NotBlank(message = "You must create a list of recipients or locations")
    private String content;

    /**
     * <p>Getter for the field <code>content</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "content", nullable = false, length = 65535, precision = 0)
    @Basic
    public String getContent() {
        return content;
    }

    /**
     * <p>Setter for the field <code>content</code>.</p>
     *
     * @param content a {@link java.lang.String} object.
     */
    public void setContent(String content) {
        this.content = content;
    }

    private String secondaryContent;

    /**
     * <p>Getter for the field <code>secondaryContent</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "secondary_content", length = 65535, precision = 0)
    @Basic
    public String getSecondaryContent() {
        return secondaryContent;
    }

    /**
     * <p>Setter for the field <code>secondaryContent</code>.</p>
     *
     * @param secondarycontent a {@link java.lang.String} object.
     */
    public void setSecondaryContent(String secondarycontent) {
        this.secondaryContent = secondarycontent;
    }

    private String emailHost;

    /**
     * <p>Getter for the field <code>emailHost</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "email_host", length = 255, precision = 0)
    @Basic
    public String getEmailHost() {
        return emailHost;
    }

    /**
     * <p>Setter for the field <code>emailHost</code>.</p>
     *
     * @param emailHost a {@link java.lang.String} object.
     */
    public void setEmailHost(String emailHost) {
        this.emailHost = emailHost;
    }

    private String emailFrom;

    /**
     * <p>Getter for the field <code>emailFrom</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "email_from", length = 255, precision = 0)
    @Basic
    public String getEmailFrom() {
        return emailFrom;
    }

    /**
     * <p>Setter for the field <code>emailFrom</code>.</p>
     *
     * @param emailFrom a {@link java.lang.String} object.
     */
    public void setEmailFrom(String emailFrom) {
        this.emailFrom = emailFrom;
    }

    private String emailBcc;

    /**
     * <p>Getter for the field <code>emailBcc</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "email_bcc", length = 255, precision = 0)
    @Basic
    public String getEmailBcc() {
        return emailBcc;
    }

    /**
     * <p>Setter for the field <code>emailBcc</code>.</p>
     *
     * @param emailBcc a {@link java.lang.String} object.
     */
    public void setEmailBcc(String emailBcc) {
        this.emailBcc = emailBcc;
    }

    private String emailCc;

    /**
     * <p>Getter for the field <code>emailCc</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "email_cc", length = 255, precision = 0)
    @Basic
    public String getEmailCc() {
        return emailCc;
    }

    /**
     * <p>Setter for the field <code>emailCc</code>.</p>
     *
     * @param emailCc a {@link java.lang.String} object.
     */
    public void setEmailCc(String emailCc) {
        this.emailCc = emailCc;
    }

    private String emailSensitivity;

    /**
     * <p>Getter for the field <code>emailSensitivity</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "email_sensitivity", length = 100, precision = 0)
    @Basic
    public String getEmailSensitivity() {
        return emailSensitivity;
    }

    /**
     * <p>Setter for the field <code>emailSensitivity</code>.</p>
     *
     * @param emailSensitivity a {@link java.lang.String} object.
     */
    public void setEmailSensitivity(String emailSensitivity) {
        this.emailSensitivity = emailSensitivity;
    }

    private Integer emailPriority;

    /**
     * <p>Getter for the field <code>emailPriority</code>.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    @Column(name = "email_priority", length = 10, precision = 0)
    @Basic
    public Integer getEmailPriority() {
        return emailPriority;
    }

    /**
     * <p>Setter for the field <code>emailPriority</code>.</p>
     *
     * @param emailPriority a {@link java.lang.Integer} object.
     */
    public void setEmailPriority(Integer emailPriority) {
        this.emailPriority = emailPriority;
    }

    private String emailImportance;

    /**
     * <p>Getter for the field <code>emailImportance</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "email_importance", length = 100, precision = 0)
    @Basic
    public String getEmailImportance() {
        return emailImportance;
    }

    /**
     * <p>Setter for the field <code>emailImportance</code>.</p>
     *
     * @param emailImportance a {@link java.lang.String} object.
     */
    public void setEmailImportance(String emailImportance) {
        this.emailImportance = emailImportance;
    }

    private String username;

    /**
     * <p>Getter for the field <code>username</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "username", length = 255, precision = 0)
    @Basic
    public String getUsername() {
        return username;
    }

    /**
     * <p>Setter for the field <code>username</code>.</p>
     *
     * @param username a {@link java.lang.String} object.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    private String password;

    /**
     * <p>Getter for the field <code>password</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "password", length = 255, precision = 0)
    @Basic
    public String getPassword() {
        return password;
    }

    /**
     * <p>Setter for the field <code>password</code>.</p>
     *
     * @param password a {@link java.lang.String} object.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    private String secondaryUsername;

    /**
     * <p>Getter for the field <code>secondaryUsername</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "secondary_username", length = 255, precision = 0)
    @Basic
    public String getSecondaryUsername() {
        return secondaryUsername;
    }

    /**
     * <p>Setter for the field <code>secondaryUsername</code>.</p>
     *
     * @param secondaryusername a {@link java.lang.String} object.
     */
    public void setSecondaryUsername(String secondaryusername) {
        this.secondaryUsername = secondaryusername;
    }

    private String secondaryPassword;

    /**
     * <p>Getter for the field <code>secondaryPassword</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "secondary_password", length = 255, precision = 0)
    @Basic
    public String getSecondaryPassword() {
        return secondaryPassword;
    }

    /**
     * <p>Setter for the field <code>secondaryPassword</code>.</p>
     *
     * @param secondarypassword a {@link java.lang.String} object.
     */
    public void setSecondaryPassword(String secondarypassword) {
        this.secondaryPassword = secondarypassword;
    }

    private String emailSubject;

    /**
     * <p>Getter for the field <code>emailSubject</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "email_subject", length = 255, precision = 0)
    @Basic
    public String getEmailSubject() {
        return emailSubject;
    }

    /**
     * <p>Setter for the field <code>emailSubject</code>.</p>
     *
     * @param emailSubject a {@link java.lang.String} object.
     */
    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
    }

    private String emailBody;

    /**
     * <p>Getter for the field <code>emailBody</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "email_body", length = 65535, precision = 0)
    @Basic
    public String getEmailBody() {
        return emailBody;
    }

    /**
     * <p>Setter for the field <code>emailBody</code>.</p>
     *
     * @param emailBody a {@link java.lang.String} object.
     */
    public void setEmailBody(String emailBody) {
        this.emailBody = emailBody;
    }

    private String emailAttachmentName;

    /**
     * <p>Getter for the field <code>emailAttachmentName</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "email_attachment_name", length = 255, precision = 0)
    @Basic
    public String getEmailAttachmentName() {
        return emailAttachmentName;
    }

    /**
     * <p>Setter for the field <code>emailAttachmentName</code>.</p>
     *
     * @param emailAttachmentName a {@link java.lang.String} object.
     */
    public void setEmailAttachmentName(String emailAttachmentName) {
        this.emailAttachmentName = emailAttachmentName;
    }

    private String compression;

    /**
     * <p>Getter for the field <code>compression</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "compression", length = 20, precision = 0)
    @Basic
    public String getCompression() {
        return compression;
    }

    /**
     * <p>Setter for the field <code>compression</code>.</p>
     *
     * @param compression a {@link java.lang.String} object.
     */
    public void setCompression(String compression) {
        this.compression = compression;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DistributionListEntity)) return false;
        final DistributionListEntity other = (DistributionListEntity) obj;
        return Objects.equals(this.id, other.id) &&
                Objects.equals(this.compression, other.compression) &&
                Objects.equals(this.content, other.content) &&
                Objects.equals(this.getDatasource(), other.getDatasource()) &&
                Objects.equals(this.description, other.description) &&
                Objects.equals(this.emailAttachmentName, other.emailAttachmentName) &&
                Objects.equals(this.emailBcc, other.emailBcc) &&
                Objects.equals(this.emailBody, other.emailBody) &&
                Objects.equals(this.emailCc, other.emailCc) &&
                Objects.equals(this.emailFrom, other.emailFrom) &&
                Objects.equals(this.emailHost, other.emailHost) &&
                Objects.equals(this.emailImportance, other.emailImportance) &&
                Objects.equals(this.emailPriority, other.emailPriority) &&
                Objects.equals(this.emailSensitivity, other.emailSensitivity) &&
                Objects.equals(this.emailSubject, other.emailSubject) &&
                Objects.equals(this.foreach, other.foreach) &&
                Objects.equals(this.internal, other.internal) &&
                Objects.equals(this.name, other.name) &&
                Objects.equals(this.password, other.password) &&
                Objects.equals(this.pgpEncryptedOutput, other.pgpEncryptedOutput) &&
                Objects.equals(this.getPgpPublicKey(), other.getPgpPublicKey()) &&
                Objects.equals(this.secondaryContent, other.secondaryContent) &&
                Objects.equals(this.secondaryPassword, other.secondaryPassword) &&
                Objects.equals(this.secondaryUsername, other.secondaryUsername) &&
                Objects.equals(this.getSshKey(), other.getSshKey()) &&
                Objects.equals(this.sshPassphrase, other.sshPassphrase) &&
                Objects.equals(this.sshUserAuthentication, other.sshUserAuthentication) &&
                Objects.equals(this.type, other.type) &&
                Objects.equals(this.userDirIsRoot, other.userDirIsRoot) &&
                Objects.equals(this.username, other.username);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "DistributionListEntity{" +
                "compression='" + compression + '\'' +
                ", content='" + content + '\'' +
                ", datasource=" + getDatasource() +
                ", description='" + description + '\'' +
                ", emailAttachmentName='" + emailAttachmentName + '\'' +
                ", emailBcc='" + emailBcc + '\'' +
                ", emailBody='" + emailBody + '\'' +
                ", emailCc='" + emailCc + '\'' +
                ", emailFrom='" + emailFrom + '\'' +
                ", emailHost='" + emailHost + '\'' +
                ", emailImportance='" + emailImportance + '\'' +
                ", emailPriority='" + emailPriority + '\'' +
                ", emailSensitivity='" + emailSensitivity + '\'' +
                ", emailSubject='" + emailSubject + '\'' +
                ", foreach='" + foreach + '\'' +
                ", id='" + id + '\'' +
                ", internal='" + internal + '\'' +
                ", name='" + name + '\'' +
                ", password='" + password + '\'' +
                ", pgpEncryptedOutput='" + pgpEncryptedOutput + '\'' +
                ", pgpPublicKey=" + getPgpPublicKey() +
                ", secondaryContent='" + secondaryContent + '\'' +
                ", secondaryPassword='" + secondaryPassword + '\'' +
                ", secondaryUsername='" + secondaryUsername + '\'' +
                ", sshPassphrase='" + sshPassphrase + '\'' +
                ", sshUserAuthentication='" + sshUserAuthentication + '\'' +
                ", type='" + type + '\'' +
                ", userDirIsRoot='" + userDirIsRoot + '\'' +
                ", username='" + username + '\'' +
                '}';
    }

    private Collection<ScheduledTaskEntity> errorScheduledTasks;

    /**
     * <p>Getter for the field <code>errorScheduledTasks</code>.</p>
     *
     * @return a {@link java.util.Collection} object.
     */
    @OneToMany(mappedBy = "errorDistributionList", cascade = CascadeType.ALL)
    public Collection<ScheduledTaskEntity> getErrorScheduledTasks() {
        return errorScheduledTasks;
    }

    /**
     * <p>Setter for the field <code>errorScheduledTasks</code>.</p>
     *
     * @param errorScheduledTasks a {@link java.util.Collection} object.
     */
    public void setErrorScheduledTasks(Collection<ScheduledTaskEntity> errorScheduledTasks) {
        this.errorScheduledTasks = errorScheduledTasks;
    }

    private Collection<ScheduledTaskEntity> scheduledTasks;

    /**
     * <p>Getter for the field <code>scheduledTasks</code>.</p>
     *
     * @return a {@link java.util.Collection} object.
     */
    @OneToMany(mappedBy = "distributionList", cascade = CascadeType.ALL)
    public Collection<ScheduledTaskEntity> getScheduledTasks() {
        return scheduledTasks;
    }

    /**
     * <p>Setter for the field <code>scheduledTasks</code>.</p>
     *
     * @param scheduledTasks a {@link java.util.Collection} object.
     */
    public void setScheduledTasks(Collection<ScheduledTaskEntity> scheduledTasks) {
        this.scheduledTasks = scheduledTasks;
    }

    private DatasourceEntity datasource;

    /**
     * <p>Getter for the field <code>datasource</code>.</p>
     *
     * @return a {@link DatasourceEntity} object.
     */
    @ManyToOne
    public
    @JoinColumn(name = "datasource_id", referencedColumnName = "id")
    DatasourceEntity getDatasource() {
        return datasource;
    }

    /**
     * <p>Setter for the field <code>datasource</code>.</p>
     *
     * @param datasource a {@link DatasourceEntity} object.
     */
    public void setDatasource(DatasourceEntity datasource) {
        this.datasource = datasource;
    }

    private boolean internal;

    /**
     * <p>isInternal.</p>
     *
     * @return a boolean.
     */
    @Column(name = "internal", length = 0, precision = 0)
    @Basic
    public boolean isInternal() {
        return internal;
    }

    /**
     * <p>Setter for the field <code>internal</code>.</p>
     *
     * @param internal a boolean.
     */
    public void setInternal(boolean internal) {
        this.internal = internal;
    }


    private boolean pgpEncryptedOutput;

    /**
     * <p>isPgpEncryptedOutput.</p>
     *
     * @return a boolean.
     */
    @Column(name = "pgp_encrypted_output", length = 0, precision = 0)
    @Basic
    public boolean isPgpEncryptedOutput() {
        return pgpEncryptedOutput;
    }

    /**
     * <p>Setter for the field <code>pgpEncryptedOutput</code>.</p>
     *
     * @param pgpEncryptedOutput a boolean.
     */
    public void setPgpEncryptedOutput(boolean pgpEncryptedOutput) {
        this.pgpEncryptedOutput = pgpEncryptedOutput;
    }


    private byte[] pgpPublicKey;

    /**
     * <p>Getter for the field <code>pgpPublicKey</code>.</p>
     *
     * @return an array of byte.
     */
    @Column(name = "pgp_public_key", length = 2147483647, precision = 0)
    @Basic(fetch = FetchType.LAZY)
    public byte[] getPgpPublicKey() {
        return pgpPublicKey;
    }

    /**
     * <p>Setter for the field <code>pgpPublicKey</code>.</p>
     *
     * @param pgpPublicKey an array of byte.
     */
    public void setPgpPublicKey(byte[] pgpPublicKey) {
        this.pgpPublicKey = pgpPublicKey;
    }

    private boolean sshUserAuthentication;

    /**
     * <p>isSshUserAuthentication.</p>
     *
     * @return a boolean.
     */
    @Column(name = "ssh_user_authentication", length = 1, precision = 0)
    @Basic
    public boolean isSshUserAuthentication() {
        return sshUserAuthentication;
    }

    /**
     * <p>Setter for the field <code>sshUserAuthentication</code>.</p>
     *
     * @param sshUserAuthentication a boolean.
     */
    public void setSshUserAuthentication(boolean sshUserAuthentication) {
        this.sshUserAuthentication = sshUserAuthentication;
    }

    private byte[] sshKey;

    /**
     * <p>Getter for the field <code>sshKey</code>.</p>
     *
     * @return an array of byte.
     */
    @Column(name = "ssh_key", length = 2147483647, precision = 0)
    @Basic(fetch = FetchType.LAZY)
    public byte[] getSshKey() {
        return sshKey;
    }

    /**
     * <p>Setter for the field <code>sshKey</code>.</p>
     *
     * @param sshKey an array of byte.
     */
    public void setSshKey(byte[] sshKey) {
        this.sshKey = sshKey;
    }

    private String sshPassphrase;

    /**
     * <p>Getter for the field <code>sshPassphrase</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "ssh_passphrase", length = 100, precision = 0)
    @Basic
    public String getSshPassphrase() {
        return sshPassphrase;
    }

    /**
     * <p>Setter for the field <code>sshPassphrase</code>.</p>
     *
     * @param sshPassphrase a {@link java.lang.String} object.
     */
    public void setSshPassphrase(String sshPassphrase) {
        this.sshPassphrase = sshPassphrase;
    }

    private Collection<ScheduledTaskEntity> notifyScheduledTasksBy;

    /**
     * <p>getNotifyScheduledTasks.</p>
     *
     * @return a {@link java.util.Collection} object.
     */
    @OneToMany(mappedBy = "notifyDistributionList", cascade = CascadeType.ALL)
    public Collection<ScheduledTaskEntity> getNotifyScheduledTasks() {
        return notifyScheduledTasksBy;
    }

    /**
     * <p>setNotifyScheduledTasks.</p>
     *
     * @param notifyScheduledTasksBy a {@link java.util.Collection} object.
     */
    public void setNotifyScheduledTasks(Collection<ScheduledTaskEntity> notifyScheduledTasksBy) {
        this.notifyScheduledTasksBy = notifyScheduledTasksBy;
    }

    /**
     * <p>getDefinition.</p>
     *
     * @return a {@link com.pivotal.monitoring.utils.Definition} object.
     *
     * @throws Exception if an exception occurs parsing the xml
     */
    @Transient
    public String getDefinitionXML() throws Exception {
        return getDefinitionXML(getType());
    }

    /**
     * Get the definition XML for a given distribution list type, or null if there is none for that type
     *
     * @param type type name
     *
     * @return definition xml
     */
    public static String getDefinitionXML(String type) {
        if (Common.doStringsMatch(type, "sms")) {
            return smsDefinitionXML;
        }
        else {
            return null;
        }
    }


    /**
     * <p>getDefinition.</p>
     *
     * @return a {@link com.pivotal.monitoring.utils.Definition} object.
     *
     * @throws Exception if an exception occurs parsing the xml
     */
    @Transient
    public Definition getDefinition() throws Exception {
        return new Definition(this);
    }

    private DefinitionSettings values;

    /**
     * Returns the definition values for this object
     *
     * @return DefinitionSettings object
     */
    @Transient
    public DefinitionSettings getSettings() {
        try {
            if (values == null && settingsXML != null) {
                values = new DefinitionSettings(new Definition(this), getSettingsXML());
            }
        }
        catch (Exception e) {
            logger.error("Problem parsing settings - {}", PivotalException.getErrorMessage(e));
        }
        return values;
    }

    /**
     * Returns the definition values for this object
     *
     * @param type Type name
     *
     * @return DefinitionSettings object
     */
    @Transient
    static public DefinitionSettings getSettings(String type) {
        try {
            Definition definition = new Definition(getDefinitionXML(type));
            definition.setName(type);
            return new DefinitionSettings(definition);
        }
        catch (Exception e) {
            logger.error("Problem parsing settings - {}", PivotalException.getErrorMessage(e));
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(compression, content, getDatasource(), description, emailAttachmentName, emailBcc, emailBody, emailCc, emailFrom, emailHost, emailImportance, emailPriority, emailSensitivity, emailSubject, foreach, internal, name, password, pgpEncryptedOutput, getPgpPublicKey(), secondaryContent, secondaryPassword, secondaryUsername, getSshKey(), sshPassphrase, sshUserAuthentication, type, userDirIsRoot, username);
    }
}
