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
import com.pivotal.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Objects;

/**
 * <p>ActionEntity class.</p>
 */
@Entity
@Table(name = "action")
public class ActionEntity extends AbstractEntity implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(ActionEntity.class);
    private static final long serialVersionUID = 6687270923912239189L;

    private Integer id;
    private Timestamp expiry;
    private ActionTypeEntity type;
    private String settings;
    private String tag;
    private UserEntity user;
    private String guid;
    private boolean used;

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
        logger.debug("Setting id " + (Common.isBlank(id)?"":String.valueOf(id)));
        this.id = id;
    }

    /**
     * <p>Getter for the field <code>expiry</code>.</p>
     *
     * @return a {@link Timestamp} object.
     */
    @Basic
    @Column(name = "expiry", length = 19)
    public Timestamp getExpiry() {
        return expiry;
    }

    /**
     * <p>Setter for the field <code>expiry</code>.</p>
     *
     * @param expiry a {@link Timestamp} object.
     */
    public void setExpiry(Timestamp expiry) {
        this.expiry = expiry;
    }

    /**
     * <p>Getter for the field <code>guid</code>.</p>
     *
     * @return a {@link String} object.
     */
    @Column(name = "guid", nullable = false, length = 100)
    @Basic
    public String getGuid() {
        return guid;
    }

    /**
     * <p>Setter for the field <code>Guid</code>.</p>
     *
     * @param guid a {@link String} object.
     */
    public void setGuid(String guid) {
        this.guid = guid;
    }

    /**
     * <p>Getter for the field <code>used</code>.</p>
     *
     * @return a {boolean}.
     */
    @Column(name = "used", length = 0)
    @Basic
    public Boolean getUsed() {
        return used;
    }

    public void setUsed(Boolean used) {
        this.used = Common.isYes(used);
    }

    /**
     * <p>Getter for the field <code>settings</code>.</p>
     *
     * @return a {@link String} object.
     */
    @Column(name = "settings", nullable = false)
    @Basic
    public String getSettings() {
        return settings;
    }

    /**
     * <p>Setter for the field <code>settinghs</code>.</p>
     *
     * @param settings a {@link String} object.
     */
    public void setSettings(String settings) {
        this.settings = settings;
    }

    @Transient
    public void setSettings(Map<String, String>settings) {
        setSettings(JsonMapper.serializeItem(settings));
    }

    @Transient
    public Map<String, String> getSettingsMap() {

        return JsonMapper.deserializeIntoMap(settings, String.class, String.class);
    }

    @ManyToOne
    @JoinColumn(name = "action_type_id", referencedColumnName = "id", nullable = false)
    public ActionTypeEntity getType() {
        return type;
    }

    public void setType(ActionTypeEntity type) {
        this.type = type;
    }

    @Column(name = "tag", nullable = false)
    @Basic
    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ActionEntity)) return false;
        final ActionEntity other = (ActionEntity) obj;
        return Objects.equals(this.expiry, other.expiry) &&
               Objects.equals(this.type, other.type) &&
               Objects.equals(this.guid, other.guid) &&
               Objects.equals(this.settings, other.settings) &&
               Objects.equals(this.tag, other.tag) &&
               Objects.equals(this.user, other.user) &&
               Objects.equals(this.used, other.used);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "ActionEntity{" +
               ", id='" + id + '\'' +
               ", type='" + type + '\'' +
               ", guid='" + guid + '\'' +
               ", settings='" + settings + '\'' +
               ", tag='" + tag + '\'' +
               ", user='" + user + '\'' +
               ", used='" + used + '\'' +
               '}';
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(expiry, type, guid, settings, tag, user, used);
    }
}
