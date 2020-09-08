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
import com.pivotal.utils.PivotalException;
import org.springframework.ui.Model;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * Maps the change_log table to the ChangeLogEntity lass
 */
@Table(name = "change_log")
@Entity
public class ChangeLogEntity extends AbstractEntity implements Serializable {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ChangeLogEntity.class);
    private static final long serialVersionUID = 4550422780535557785L;

    /**
     * Types of changes that can occur to a record
     */
    public static enum ChangeTypes {
        ADDED,EDITED,DELETED;

        // Get the export format description
        public String getDescription() {
            if (equals(ADDED))
                return "Added";
            else if (equals(EDITED))
                return "Edited";
            else
                return "Deleted";
        }
    }

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

    private Timestamp timeAdded;

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

    private String changeType;

    /**
     * <p>Getter for the field <code>changeType</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "change_type", nullable = false, length = 100, precision = 0)
    @Basic
    public String getChangeType() {
        return changeType;
    }

    /**
     * <p>Setter for the field <code>changeType</code>.</p>
     *
     * @param changeType a {@link java.lang.String} object.
     */
    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    private String userFullName;

    /**
     * <p>Getter for the field <code>userFullName</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "user_full_name", nullable = false, length = 100, precision = 0)
    @Basic
    public String getUserFullName() {
        return userFullName;
    }

    /**
     * <p>Setter for the field <code>userFullName</code>.</p>
     *
     * @param userFullName a {@link java.lang.String} object.
     */
    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    private String tableAffected;

    /**
     * <p>Getter for the field <code>tableAffected</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "table_affected", nullable = false, length = 100, precision = 0)
    @Basic
    public String getTableAffected() {
        return tableAffected;
    }

    /**
     * <p>Setter for the field <code>tableAffected</code>.</p>
     *
     * @param tableAffected a {@link java.lang.String} object.
     */
    public void setTableAffected(String tableAffected) {
        this.tableAffected = tableAffected;
    }

    private Integer rowAffected;

    /**
     * <p>Getter for the field <code>rowAffected</code>.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    @Column(name = "row_affected", nullable = false, length = 10, precision = 0)
    @Basic
    public Integer getRowAffected() {
        return rowAffected;
    }

    /**
     * <p>Setter for the field <code>rowAffected</code>.</p>
     *
     * @param rowAffected a {@link java.lang.Integer} object.
     */
    public void setRowAffected(Integer rowAffected) {
        this.rowAffected = rowAffected;
    }

    private String parentRow;

    /**
     * <p>Getter for the field <code>parent_key</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "parent_row", nullable = true, length = 10, precision = 0)
    @Basic
    public String getParentRow() {
        return parentRow;
    }

    /**
     * <p>Setter for the field <code>parentRow</code>.</p>
     *
     * @param parentRow {@link java.lang.String} object.
     */
    public void setParentRow(String parentRow) {
        this.parentRow = parentRow;
    }

    private String previousValues;

    /**
     * <p>Getter for the field <code>previousValues</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "previous_values", length = 2147483647, precision = 0)
    @Basic
    public String getPreviousValues() {
        return previousValues;
    }

    /**
     * <p>Setter for the field <code>previousValues</code>.</p>
     *
     * @param previousValues a {@link java.lang.String} object.
     */
    public void setPreviousValues(String previousValues) {
        this.previousValues = previousValues;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ChangeLogEntity)) return false;
        final ChangeLogEntity other = (ChangeLogEntity) obj;
        return Objects.equals(this.changeType, other.changeType) &&
               Objects.equals(this.previousValues, other.previousValues) &&
               Objects.equals(this.rowAffected, other.rowAffected) &&
               Objects.equals(this.parentRow, other.parentRow) &&
               Objects.equals(this.tableAffected, other.tableAffected) &&
               Objects.equals(this.timeAdded, other.timeAdded) &&
               Objects.equals(this.userFullName, other.userFullName);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "ChangeLogEntity{" +
               "changeType='" + changeType + '\'' +
               ", id='" + id + '\'' +
               ", previousValues='" + previousValues + '\'' +
               ", rowAffected='" + rowAffected + '\'' +
               ", tableAffected='" + tableAffected + '\'' +
               ", timeAdded='" + timeAdded + '\'' +
               ", userFullName='" + userFullName + '\'' +
               '}';
    }

    /**
     * Adds a log message to the change log table
     *
     * @param log Log entity to save
     * @param model Model to log errors into
     */
    public static void addLogEntry(Model model, ChangeLogEntity log) {
        try {
            log.setId(null);
            HibernateUtils.save(model, log);
        }
        catch (Exception e) {
            logger.error("Problem saving change log entry - {}", PivotalException.getErrorMessage(e));
        }
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(changeType, previousValues, rowAffected, tableAffected, timeAdded, userFullName);
    }
}
