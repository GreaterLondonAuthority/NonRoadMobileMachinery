/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.hibernate.entities;

import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.Common;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * <p>NoteEntity class.</p>
 */
@Table(name = "note")
@Entity
public class NoteEntity extends AbstractEntity implements Serializable {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NoteEntity.class);
    private static final long serialVersionUID = 6687270923912239189L;


    private Integer referenceId;
    private String referenceType;
    private String content;
    private String plainContent;
    private Timestamp timeAdded;
    private String tag;
    private UserEntity user;
    private UserEntity addedBy;
    private NoteTypeEntity type;
    private RoleEntity role;
    private Integer id;
    private boolean viewed;
    private Timestamp actionDate;
    private Boolean linkedOnly;

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
        logger.debug("Setting id " + (Common.isBlank(id)?"":String.valueOf(id)));
        this.id = id;
    }

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = true)
    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    @ManyToOne
    @JoinColumn(name = "added_by", referencedColumnName = "id", nullable = false)
    public UserEntity getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(UserEntity addedBy) {
        this.addedBy = addedBy;
    }

    @ManyToOne
    @JoinColumn(name = "role_id", referencedColumnName = "id", nullable = true)
    public RoleEntity getRole() {
        return role;
    }

    public void setRole(RoleEntity role) {
        this.role = role;
    }

    /**
     * <p>Getter for the field <code>referenceId</code>.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    @Column(name = "reference_id", nullable = false, length = 10, precision = 0)
    @Basic
    public Integer getReferenceId() {
        return referenceId;
    }

    /**
     * <p>Setter for the field <code>referenceId</code>.</p>
     *
     * @param referenceId a {@link java.lang.Integer} object.
     */
    public void setReferenceId(Integer referenceId) {
        this.referenceId = referenceId;
    }

    /**
     * <p>Getter for the field <code>referenceType</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "reference_type", nullable = false, length = 40, precision = 0)
    @Basic
    public String getReferenceType() {
        return referenceType;
    }

    /**
     * <p>Setter for the field <code>referenceType</code>.</p>
     *
     * @param referenceType a {@link java.lang.String} object.
     */
    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    /**
     * <p>Getter for the field <code>linked_only</code>.</p>
     *
     * @return a {boolean}.
     */
    @Column(name = "linked_only", length = 0)
    @Basic
    public Boolean getLinkedOnly() {
        return linkedOnly;
    }

    public void setLinkedOnly(Boolean linkedOnly) {
        this.linkedOnly = Common.isYes(linkedOnly);
    }

    /**
     * <p>Getter for the field <code>content</code>.</p>
     * Returns the first 100 characters
     *
     * @return a {@link java.lang.String} object.
     */
    @Transient
    public String getContentStart() {

        String retValue = Common.getBodyTextFromHtml(content);

        if (!Common.isBlank(retValue)) {

            if (retValue.length() > 100)
                retValue = retValue.substring(0,97) + "...";
        }

        return retValue;
    }

    /**
     * <p>Getter for the field <code>content</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "content", nullable = false)
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
        this.plainContent = Common.getBodyTextFromHtml(content);
    }

    /**
     * <p>Getter for the field <code>plainContent</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "plain_content", nullable = false)
    @Basic
    public String getPlainContent() {
        return plainContent;
    }

    /**
     * <p>Setter for the field <code>plainContent</code>.</p>
     *
     * @param plainContent a {@link java.lang.String} object.
     */
    public void setPlainContent(String plainContent) {
        this.plainContent = plainContent;
    }

    /**
     * <p>Getter for the field <code>timeAdded</code>.</p>
     *
     * @return a {@link java.sql.Timestamp} object.
     */
    @Column(name = "time_added", nullable = false, length = 19, precision = 0)
    @Basic
    public Timestamp getTimeAdded() {
        return timeAdded;
    }

    /**
     * <p>Setter for the field <code>timeAdded</code>.</p>
     *
     * @param timeAdded a {@link java.sql.Timestamp} object.
     */
    public void setTimeAdded(Timestamp timeAdded) {
        this.timeAdded = timeAdded;
    }
    private String folder;

    @Transient
    public void setTimeAddedNow() {
        setTimeAdded(new Timestamp(new Date().getTime()));
    }

    @ManyToOne
    @JoinColumn(name = "type_id", referencedColumnName = "id", nullable = false)
    public NoteTypeEntity getType() {
        return type;
    }

    public void setType(NoteTypeEntity type) {
        this.type = type;
    }

    /**
     * <p>Getter for the field <code>folder</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "folder", length = 250, precision = 0)
    @Basic
    public String getFolder() {
        return folder;
    }

    /**
     * <p>Setter for the field <code>folder</code>.</p>
     *
     * @param folder a {@link java.lang.String} object.
     */
    public void setFolder(String folder) {
        this.folder = folder;
    }

    /**
     * <p>Getter for the field <code>tag</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "tag", nullable = true, length = 100, precision = 0)
    @Basic
    public String getTag() {
        return tag;
    }

    /**
     * <p>Setter for the field <code>tag</code>.</p>
     *
     * @param tag a {@link java.lang.String} object.
     */
    public void setTag(String tag) {
        this.tag = tag;
    }

    @Transient
    public static List<NoteEntity> getNotes(String referenceType, Integer referenceId, String sort) {

        return HibernateUtils.selectEntities("from NoteEntity where referenceType = ? and referenceId = ? order by " + (Common.isBlank(sort) ? "timeAdded" : sort), referenceType, referenceId);
    }

    /**
     * Returns the referenced siteentity, null if not a site or no site
     *
     * @return SiteEntity Object
     */
    @Transient
    public SiteEntity getSite() {
        SiteEntity retValue = null;

        if (referenceType.equals(SiteEntity.class.getSimpleName()))
            retValue = (SiteEntity)getReference();

        return  retValue;
    }

    /**
     * Returns the referenced object
     *
     * @return Object
     */
    @Transient
    public Object getReference() {

        Object retValue = null;
        if (!Common.isBlank(referenceType) && !Common.isBlank(referenceId))
            retValue = HibernateUtils.getEntity(referenceType, referenceId);

        return retValue;
    }

    /**
     * <p>Getter for the field <code>viewed</code>.</p>
     *
     * @return a {boolean}.
     */
    @Column(name = "viewed", length = 0, precision = 0)
    @Basic
    public boolean isViewed() {
        return viewed;
    }

    /**
     * <p>Setter for the field <code>viewed</code>.</p>
     *
     * @param viewed a {boolean}.
     */
    public void setViewed(boolean viewed) {
        this.viewed = viewed;
    }

    /**
     * <p>Getter for the field <code>action_date</code>.</p>
     *
     * @return a {@link java.sql.Timestamp} object.
     */
    @Basic
    @Column(name = "action_date", nullable = true, length = 19, precision = 0)
    public Timestamp getActionDate() {
        return actionDate;
    }

    /**
     * <p>Setter for the field <code>action_date</code>.</p>
     *
     * @param actionDate a {@link java.sql.Timestamp} object.
     */
    public void setActionDate(Timestamp actionDate) {
        this.actionDate = actionDate;
    }

    public void setActionDate(Calendar actionDate) {
        this.actionDate = new java.sql.Timestamp(actionDate.getTimeInMillis());
    }

    public void setActionDate(Date actionDate) {
        this.actionDate = new java.sql.Timestamp(actionDate.getTime());
    }

    public void setActionDate(String actionDate) {
        this.actionDate = new java.sql.Timestamp(Common.parseDate(actionDate).getTime());
    }

    public void setActionDate() {
        this.actionDate = new Timestamp(new Date().getTime());
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof NoteEntity)) return false;
        final NoteEntity other = (NoteEntity) obj;
        return Objects.equals(this.content, other.content) &&
               Objects.equals(this.referenceId, other.referenceId) &&
               Objects.equals(this.referenceType, other.referenceType) &&
               Objects.equals(this.timeAdded, other.timeAdded) &&
               Objects.equals(this.addedBy, other.addedBy) &&
               Objects.equals(this.tag, other.tag) &&
               Objects.equals(this.role, other.role) &&
               Objects.equals(this.linkedOnly, other.linkedOnly) &&
               Objects.equals(this.actionDate, other.actionDate) &&
               Objects.equals(this.viewed, other.viewed) &&
               Objects.equals(this.folder, other.folder) &&
               Objects.equals(this.user, other.user);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "NotesEntity{" +
               "content='" + content + '\'' +
               ", id='" + id + '\'' +
               ", referenceId='" + referenceId + '\'' +
               ", referenceType='" + referenceType + '\'' +
               ", timeAdded='" + timeAdded + '\'' +
               ", addedBy='" + addedBy + '\'' +
               ", tag='" + tag + '\'' +
               ", role='" + role + '\'' +
               ", linkedOnly='" + linkedOnly + '\'' +
               ", actionDate='" + actionDate + '\'' +
               ", viewed='" + viewed + '\'' +
               ", folder='" + folder + '\'' +
               ", user='" + user + '\'' +
               '}';
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(content, referenceId, referenceType, timeAdded, addedBy, tag, role, linkedOnly, actionDate, viewed, folder, user);
    }
}
