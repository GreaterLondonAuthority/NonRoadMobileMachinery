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
import com.pivotal.reporting.reports.Report;
import com.pivotal.reporting.reports.ReportFactory;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.*;
import java.io.File;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Objects;

/**
 * Maps the report table to the ReportBlobEntity class
 */
@Table(name = "report")
@Entity
public class ReportEntity extends AbstractEntity implements Serializable {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ReportEntity.class);
    private static final long serialVersionUID = -4571873434638120607L;

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

    private String name;

    /**
     * <p>Getter for the field <code>name</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "name", nullable = false, length = 100, precision = 0)
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

    private byte[] file;

    /**
     * <p>Getter for the field <code>file</code>.</p>
     *
     * @return an array of byte.
     */
    @Column(name = "file", length = 2147483647, precision = 0)
    @Basic(fetch = FetchType.LAZY)
    public byte[] getFile() {
        return file;
    }

    /**
     * <p>getFileString.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Transient
    public String getFileString() {
        if (!Common.isBlank(file))
            try {
                return new String(file, "UTF-8");
            }
            catch (Exception e) {
                logger.error("Cannot transform file to string - {}", PivotalException.getErrorMessage(e));
                return null;
            }
        else
            return null;
    }

    /**
     * <p>setFileString.</p>
     *
     * @param value a {@link java.lang.String} object.
     */
    @Transient
    public void setFileString(String value) {
        if (!Common.isBlank(value))
            try {
                file = value.getBytes("UTF-8");
            }
            catch (Exception e) {
                logger.error("Cannot transform string to byte array - {}", PivotalException.getErrorMessage(e));
            }
        else
            file=null;
    }

    /**
     * <p>setFileBinary.</p>
     *
     * @param file a {@link java.io.File} object.
     */
    @SuppressWarnings("unused")
    @Transient
    public void setFileBinary(File file) {
        try {
            setFile(Common.readBinaryFile(file));
        }
        catch (Exception e) {
            logger.error("Cannot transform string to byte array - {}", PivotalException.getErrorMessage(e));
        }
    }

    /**
     * <p>Setter for the field <code>file</code>.</p>
     *
     * @param file an array of byte.
     */
    public void setFile(byte[] file) {
        this.file = file;
    }

    @NotBlank(message = "You must select the type of this report")
    private String type;

    /**
     * <p>Getter for the field <code>type</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "type", nullable = false, length = 20, precision = 0)
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

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ReportEntity)) return false;
        final ReportEntity other = (ReportEntity) obj;
        return Objects.equals(this.description, other.description) &&
               Objects.equals(this.getFile(), other.getFile()) &&
               Objects.equals(this.internal, other.internal) &&
               Objects.equals(this.timeModified, other.timeModified) &&
               Objects.equals(this.name, other.name) &&
               Objects.equals(this.type, other.type);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "ReportEntity{" +
               "description='" + description + '\'' +
               ", id='" + id + '\'' +
               ", internal='" + internal + '\'' +
               ", lastModified='" + timeModified + '\'' +
               ", name='" + name + '\'' +
               ", type='" + type + '\'' +
               '}';
    }


    private Collection<ScheduledTaskEntity> scheduledtasks;

    /**
     * <p>getScheduledTasks.</p>
     *
     * @return a {@link java.util.Collection} object.
     */
    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL)
    public Collection<ScheduledTaskEntity> getScheduledTasks() {
        return scheduledtasks;
    }

    /**
     * <p>setScheduledTasks.</p>
     *
     * @param scheduledtasks a {@link java.util.Collection} object.
     */
    public void setScheduledTasks(Collection<ScheduledTaskEntity> scheduledtasks) {
        this.scheduledtasks = scheduledtasks;
    }

    private Timestamp timeModified;

    /**
     * <p>Getter for the field <code>lastModified</code>.</p>
     *
     * @return a {@link java.sql.Timestamp} object.
     */
    @Column(name = "time_modified", length = 19, precision = 0)
    @Basic
    public Timestamp getTimeModified() {
        return timeModified;
    }

    /**
     * <p>Setter for the field <code>lastModified</code>.</p>
     *
     * @param lastModified a {@link java.sql.Timestamp} object.
     */
    public void setTimeModified(Timestamp lastModified) {
        this.timeModified = lastModified;
    }

    /**
     * Returns the definition of the parameters in the report
     *
     * @return Definition of the parameters
     */
    @Transient
    public Definition getParameters() {
        Report report= ReportFactory.getReport(this);
        Definition returnValue = report.getReportParameters();
        report.close();
        return returnValue;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(description, getFile(), internal, timeModified, name, type);
    }
}
