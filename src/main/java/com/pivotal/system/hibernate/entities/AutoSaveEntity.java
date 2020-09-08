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
 * <p>NoteEntity class.</p>
 */
@Table(name = "auto_save")
@Entity
public class AutoSaveEntity extends AbstractEntity implements Serializable {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AutoSaveEntity.class);
    private static final long serialVersionUID = 6687270923912239189L;

    public final static String RESTORE_PARAMETER = "autosaverestore";
    private Integer referenceId;
    private String referenceType;
    private Timestamp timeAdded;
    private String savedValues;
    private UserEntity user;
    private Integer id;

    /**
     * <p>Getter for the field <code>id</code>.</p>
     *
     * @return a {@link Integer} object.
     */
    @Column(name = "id", nullable = false, length = 10)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer getId() {
        return id;
    }

    /**
     * <p>Setter for the field <code>id</code>.</p>
     *
     * @param id a {@link Integer} object.
     */
    public void setId(Integer id) {
        logger.debug("Setting id {}", (Common.isBlank(id)?"":String.valueOf(id)));
        this.id = id;
    }

    @ManyToOne
    @JoinColumn(name = "users_id", referencedColumnName = "id", nullable = false)
    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    /**
     * <p>Getter for the field <code>referenceId</code>.</p>
     *
     * @return a {@link Integer} object.
     */
    @Column(name = "reference_id", nullable = false, length = 10)
    @Basic
    public Integer getReferenceId() {
        return referenceId;
    }

    /**
     * <p>Setter for the field <code>referenceId</code>.</p>
     *
     * @param referenceId a {@link Integer} object.
     */
    public void setReferenceId(Integer referenceId) {
        this.referenceId = referenceId;
    }

    /**
     * <p>Getter for the field <code>referenceType</code>.</p>
     *
     * @return a {@link String} object.
     */
    @Column(name = "reference_type", nullable = false, length = 40)
    @Basic
    public String getReferenceType() {
        return referenceType;
    }

    /**
     * <p>Setter for the field <code>referenceType</code>.</p>
     *
     * @param referenceType a {@link String} object.
     */
    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    /**
     * <p>Getter for the field <code>savedValues</code>.</p>
     *
     * @return a {@link String} object.
     */
    @Column(name = "saved_values", nullable = false, length = 1000)
    @Basic
    public String getSavedValues() {
        return savedValues;
    }

    /**
     * <p>Setter for the field <code>savedValues</code>.</p>
     *
     * @param savedValues a {@link String} object.
     */
    public void setSavedValues(String savedValues) {
        this.savedValues = savedValues;
    }

    /**
     * <p>Getter for the field <code>timeAdded</code>.</p>
     *
     * @return a {@link Timestamp} object.
     */
    @Column(name = "time_added", nullable = true, length = 19)
    @Basic
    public Timestamp getTimeAdded() {
        return timeAdded;
    }

    /**
     * <p>Setter for the field <code>timeAdded</code>.</p>
     *
     * @param timeAdded a {@link Timestamp} object.
     */
    public void setTimeAdded(Timestamp timeAdded) {
        this.timeAdded = timeAdded;
    }

    @Transient
    public void setTimeAddedNow() {
        setTimeAdded(new Timestamp(new Date().getTime()));
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof AutoSaveEntity)) return false;
        final AutoSaveEntity other = (AutoSaveEntity) obj;
        return Objects.equals(this.savedValues, other.savedValues) &&
               Objects.equals(this.referenceId, other.referenceId) &&
               Objects.equals(this.referenceType, other.referenceType) &&
               Objects.equals(this.timeAdded, other.timeAdded) &&
               Objects.equals(this.user, other.user);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "AutoSaveEntity{" +
               "savedValues='" + savedValues + '\'' +
               ", id='" + id + '\'' +
               ", referenceId='" + referenceId + '\'' +
               ", referenceType='" + referenceType + '\'' +
               ", timeAdded='" + timeAdded + '\'' +
               ", userId='" + user + '\'' +
               ", savedValues='" + savedValues + '\'' +
               '}';
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(savedValues, referenceId, referenceType, timeAdded, user, savedValues);
    }
}
