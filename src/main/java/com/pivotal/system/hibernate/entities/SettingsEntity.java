/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.hibernate.entities;

import com.pivotal.system.hibernate.annotations.InitialValue;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.security.UserManager;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * Maps the settings table to the SettingsEntity class
 */
@Table(name = "settings")
@Entity
public class SettingsEntity extends AbstractEntity implements Serializable {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SettingsEntity.class);
    private static final long serialVersionUID = -1864257038913768575L;

    @InitialValue(onlyNewDatabase = true)
    private static final String[] INIT = {
        String.format("{name:'%s',value:'%s'}",HibernateUtils.SETTING_AUTHENTICATION_TYPE,UserManager.SIMPLE_AUTHENTICATION)
    };

    private String name;

    /**
     * <p>Getter for the field <code>name</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "name", nullable = false, length = 100, precision = 0)
    @Id
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

    private String value;

    /**
     * <p>Getter for the field <code>value</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "value", length = 255, precision = 0)
    @Basic
    public String getValue() {
        return value;
    }

    /**
     * <p>Setter for the field <code>value</code>.</p>
     *
     * @param value a {@link java.lang.String} object.
     */
    public void setValue(String value) {
        this.value = value;
    }

    private String valueText;

    /**
     * <p>Getter for the field <code>valueText</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "value_text", length = 65535, precision = 0)
    @Basic
    public String getValueText() {
        return valueText;
    }

    /**
     * <p>Setter for the field <code>valueText</code>.</p>
     *
     * @param valueText a {@link java.lang.String} object.
     */
    public void setValueText(String valueText) {
        this.valueText = valueText;
    }

    private Integer valueNumeric;

    /**
     * <p>Getter for the field <code>valueNumeric</code>.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    @Column(name = "value_numeric", length = 10, precision = 0)
    @Basic
    public Integer getValueNumeric() {
        return valueNumeric;
    }

    /**
     * <p>Setter for the field <code>valueNumeric</code>.</p>
     *
     * @param valueNumeric a {@link java.lang.Integer} object.
     */
    public void setValueNumeric(Integer valueNumeric) {
        this.valueNumeric = valueNumeric;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SettingsEntity)) return false;
        final SettingsEntity other = (SettingsEntity) obj;
        return Objects.equals(this.description, other.description) &&
               Objects.equals(this.name, other.name) &&
               Objects.equals(this.value, other.value) &&
               Objects.equals(this.valueNumeric, other.valueNumeric) &&
               Objects.equals(this.valueText, other.valueText);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "SettingsEntity{" +
               "description='" + description + '\'' +
               ", name='" + name + '\'' +
               ", value='" + value + '\'' +
               ", valueNumeric='" + valueNumeric + '\'' +
               ", valueText='" + valueText + '\'' +
               '}';
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(description, name, value, valueNumeric, valueText);
    }
}
