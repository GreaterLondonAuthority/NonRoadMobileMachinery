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
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;

/**
 * <p>MediaEntity class.</p>
 */
@Table(name = "media")
@Entity
public class MediaFileEntity extends AbstractEntity implements Serializable, Cloneable{

    public static final String TYPE_CODE_ICON = "ICON";
    public static final String TYPE_CODE_DASHBOARD = "DASHBOARD";
    public static final String TYPE_CODE_DASHBOARD_PROPERTY = "DASHBOARDPROPERTY";
    public static final String TYPE_CODE_DOCUMENT_LIBRARY = "DOCUMENTLIBRARY";
    public static final String TYPE_CODE_DOCUMENT = "DOCUMENT";
    public static final String TYPE_CODE_MEDIA_THEME_TYPE = "MEDIATHEMETYPE";
    public static final String TYPE_CODE_REPORT_OUTPUT_TYPE = "REPORTOUTPUTTYPE";
    public static final String TYPE_CODE_USER_SIGNATURE = "USERSIGNATURE";

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MediaFileEntity.class);
    private static final long serialVersionUID = -1915301587741334140L;

    private Integer id;

    /**
     * <p>Getter for the field <code>id</code>.</p>
     *
     * @return a {@link Integer} object.
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
     * @param id a {@link Integer} object.
     */
    public void setId(Integer id) {
        logger.debug("Setting id {}", id);
        this.id = id;
    }

    private String name;

    /**
     * <p>Getter for the field <code>name</code>.</p>
     *
     * @return a {@link String} object.
     */
    @Column(name = "name", nullable = false, length = 100, precision = 0)
    @Basic
    public String getName() {
        return name;
    }

    /**
     * <p>Setter for the field <code>name</code>.</p>
     *
     * @param name a {@link String} object.
     */
    public void setName(String name) {
        if (name != null && name.length() > 100)
            name = name.substring(0,100);
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
     * @return a {@link String} object.
     */
    @Column(name = "description", length = 65535, precision = 0)
    @Basic
    public String getDescription() {
        return description;
    }

    /**
     * <p>Setter for the field <code>description</code>.</p>
     *
     * @param description a {@link String} object.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    private String type;

    /**
     * <p>Getter for the field <code>type</code>.</p>
     *
     * @return a {@link String} object.
     */
    @Column(name = "type", length = 100, precision = 0)
    @Basic
    public String getType() {
        return type;
    }

    /**
     * <p>Setter for the field <code>type</code>.</p>
     *
     * @param type a {@link String} object.
     */
    public void setType(String type) {
        this.type = type;
    }

    private String extension;

    /**
     * <p>Getter for the field <code>extension</code>.</p>
     *
     * @return a {@link String} object.
     */
    @Column(name = "extension", length = 10, precision = 0)
    @Basic
    public String getExtension() {
        return extension;
    }

    /**
     * <p>Setter for the field <code>extension</code>.</p>
     *
     * @param extension a {@link String} object.
     */
    public void setExtension(String extension) {
        this.extension = extension;
    }


    private byte[] file;
    /**
     * <p>Getter for the field <code>file</code>.</p>
     *
     * @return an array of byte.
     */
    @Column(name = "file")
    public byte[] getFile() {
        return file;
    }


    /**
     * <p>Setter for the field <code>file</code>.</p>
     *
     * @param file an array of byte.
     */
    public void setFile(byte[] file) {
        this.file = file;
        if (file!=null) setFileSize(file.length);
    }

    /**
     * Returns a stream for the file byte array
     * @return Input stream that doesn't need closing
     */
    @Transient
    public InputStream getFileStream() {
        return new ByteArrayInputStream(getFile());
    }

    private String filename;

    /**
     * <p>Getter for the field <code>filename</code>.</p>
     *
     * @return a {@link String} object.
     */
    @Column(name = "filename", length = 65535, precision = 0)
    @Basic
    public String getFilename() {
        return filename;
    }

    /**
     * <p>Setter for the field <code>filename</code>.</p>
     *
     * @param filename a {@link String} object.
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    private Integer fileSize;

    /**
     * <p>Getter for the field <code>fileSize</code>.</p>
     *
     * @return a {@link Integer} object.
     */
    @Column(name = "file_size", nullable = false, length = 10, precision = 0)
    public Integer getFileSize() {
        return fileSize;
    }


    /**
     * <p>Setter for the field <code>fileSize</code>.</p>
     *
     * @param fileSize a {@link Integer} object.
     */
    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * <p>Getter for the field <code>fileSize</code>.</p>
     *
     * @return a {@link Integer} object.
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

    public MediaFileEntity clone() {

        MediaFileEntity obj = new MediaFileEntity();

        obj.setTimeModified(this.timeModified);
        obj.setTimeAdded(this.timeAdded);
        obj.setDescription(this.description);
        obj.setName(this.name);
        obj.setExtension(this.extension);
//        obj.setMediaFile(this.mediaFile);
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
        if (!(obj instanceof MediaFileEntity)) return false;
        final MediaFileEntity other = (MediaFileEntity) obj;
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
