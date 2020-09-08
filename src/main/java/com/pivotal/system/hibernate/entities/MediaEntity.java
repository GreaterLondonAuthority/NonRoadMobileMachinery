/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.hibernate.entities;

import com.pivotal.utils.Common;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;

/**
 * <p>MediaEntity class.</p>
 */
@Table(name = "media")
@Entity
public class MediaEntity extends AbstractEntity implements Serializable {

    public static final String TYPE_CODE_ICON = "ICON";
    public static final String TYPE_CODE_DASHBOARD = "DASHBOARD";
    public static final String TYPE_CODE_DASHBOARD_PROPERTY = "DASHBOARDPROPERTY";
    public static final String TYPE_CODE_DOCUMENT_LIBRARY = "DOCUMENTLIBRARY";
    public static final String TYPE_CODE_DOCUMENT = "DOCUMENT";
    public static final String TYPE_CODE_MEDIA_THEME_TYPE = "MEDIATHEMETYPE";
    public static final String TYPE_CODE_REPORT_OUTPUT_TYPE = "REPORTOUTPUTTYPE";
    public static final String TYPE_CODE_USER_SIGNATURE = "USERSIGNATURE";
    public static final String TYPE_MEETING_FILE = "MEETINGFILE";
    public static final String TYPE_MEETING_REPORT_FILE = "MEETINGREPORTFILE";
    public static final String TYPE_CASE_FILE = "CASEFILE";
    public static final String TYPE_DRAFT_EMAIL = "DRAFTEMAIL";
    public static final String TYPE_CASE_REPORT_FILE = "CASEREPORTFILE";
    public static final String TYPE_REPORTTEXT_FILE = "REPORTTEXTFILE";


    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MediaEntity.class);
    private static final long serialVersionUID = -1915301587741334140L;

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
        logger.debug("Getting id {}", id);
        return id;
    }

    /**
     * <p>Setter for the field <code>id</code>.</p>
     *
     * @param id a {@link java.lang.Integer} object.
     */
    public void setId(Integer id) {
        logger.debug("Setting id to {}", id);
        this.id = id;
    }

    private String name;

    /**
     * <p>Getter for the field <code>name</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "name", nullable = false, length = 500, precision = 0)
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

    private String description;

    /**
     * <p>Getter for the field <code>description</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "description", length = 65535, precision = 0)
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

    private String type;

    /**
     * <p>Getter for the field <code>type</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "type", length = 100, precision = 0)
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

    private String extension;

    /**
     * <p>Getter for the field <code>extension</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "extension", length = 10, precision = 0)
    @Basic
    public String getExtension() {
        return extension;
    }

    /**
     * <p>Setter for the field <code>extension</code>.</p>
     *
     * @param extension a {@link java.lang.String} object.
     */
    public void setExtension(String extension) {
        this.extension = extension;
    }

    private String filename;

    /**
     * <p>Getter for the field <code>filename</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "filename", length = 65535, precision = 0)
    @Basic
    public String getFilename() {
        return filename;
    }

    /**
     * <p>Setter for the field <code>filename</code>.</p>
     *
     * @param filename a {@link java.lang.String} object.
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    private Integer fileSize;

    /**
     * <p>Getter for the field <code>fileSize</code>.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    @Column(name = "file_size", nullable = false, length = 10, precision = 0)
    public Integer getFileSize() {
        return fileSize;
    }


    /**
     * <p>Setter for the field <code>fileSize</code>.</p>
     *
     * @param fileSize a {@link java.lang.Integer} object.
     */
    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }

//    private Long bloboid;
//
//    /**
//     * <p>Getter for the field <code>blobOid</code>.</p>
//     *
//     * @return a {@link java.lang.Long} object.
//     */
//    @Column(name = "bloboid", nullable = true, length = 10, precision = 0)
//    public Long getBlobOid() {
//        return bloboid;
//    }
//
//    /**
//     * <p>Setter for the field <code>blobOid</code>.</p>
//     *
//     * @param bloboid a {@link java.lang.Long} object.
//     */
//    public void setBlobOid(Long bloboid) {
//        this.bloboid = bloboid;
//    }

    /**
     * <p>Getter for the field <code>fileSize</code>.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    @Transient
    public String getFileSizeDisplay() {

        if (fileSize > 0) {
            String unit = "B";
            Float fileSizeDisplay = fileSize.floatValue();

            if (fileSizeDisplay > 1024) {
                unit = "K";
                fileSizeDisplay = fileSize.floatValue() / 1024;
            }

            if (fileSizeDisplay > 1024) {
                unit = "M";
                fileSizeDisplay = fileSizeDisplay / 1024;
            }

            if (fileSizeDisplay > 1024) {
                unit = "G";
                fileSizeDisplay = fileSizeDisplay / 1024;
            }

            return String.valueOf(Common.formatNumber(fileSizeDisplay, "#.00")) + unit;
        }
        else
            return "0";
    }

    private Timestamp timeAdded;

    @Basic
    @Column(name = "time_added", nullable = true, length = 19, precision = 0)
    public Timestamp getTimeAdded() {
        return timeAdded;
    }

    public void setTimeAdded(Timestamp timeAdded) {
        this.timeAdded = timeAdded;
    }

    @Transient
    public void setTimeAddedNow() {
        setTimeAdded(new Timestamp(new Date().getTime()));
    }

    private Timestamp timeModified;

    @Basic
    @Column(name = "time_modified", nullable = true, length = 19, precision = 0)
    public Timestamp getTimeModified() {
        return timeModified;
    }

    public void setTimeModified(Timestamp timeModified) {
        this.timeModified = timeModified;
    }

    @Transient
    public void setTimeModifiedNow() {
        setTimeModified(new Timestamp(new Date().getTime()));
    }

    public MediaEntity clone() {

        MediaEntity obj = new MediaEntity();

        obj.setTimeModified(this.timeModified);
        obj.setTimeAdded(this.timeAdded);
        obj.setDescription(this.description);
        obj.setName(this.name);
        obj.setExtension(this.extension);
        obj.setFilename(this.filename);
        obj.setFileSize(this.fileSize);
        obj.setInternal(this.internal);
        obj.setType(this.type);

        return obj;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MediaEntity)) return false;
        final MediaEntity other = (MediaEntity) obj;
        return Objects.equals(this.id, other.id) &&
               Objects.equals(this.filename, other.filename) &&
               Objects.equals(this.fileSize, other.fileSize) &&
               Objects.equals(this.extension, other.extension) &&
               Objects.equals(this.timeAdded, other.timeAdded) &&
               Objects.equals(this.timeModified, other.timeModified) &&
               Objects.equals(this.description, other.description);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "MediaEntity{" +
               "id='" + id + '\'' +
               ", filename='" + filename + '\'' +
               ", fileSize='" + fileSize + '\'' +
               ", extension=" + extension +
               ", description='" + description + '\'' +
               ", timeAdded='" + timeAdded + '\'' +
               ", timeModified='" + timeModified + '\'' +
               '}';
    }

}
