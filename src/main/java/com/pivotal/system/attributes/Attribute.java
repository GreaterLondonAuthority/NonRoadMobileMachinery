/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.attributes;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>AttributeEntity class.</p>
 */
public class Attribute implements Serializable {

    private static final long serialVersionUID = 477192698038407335L;
    private Integer referenceId;
    private String referenceType;
    private String name;
    private String description;
    private java.util.Date dateTime;
    private Double value;
    private Map<String,Object> details = new HashMap<>();
    private Boolean hidden;

    /**
     * <p>Adds a detail to this Attribute.</p>
     *
     * @param name   Detail Name to store
     * @param detail Object
     */
    public void setDetail(String name, Object detail) {
        details.put(name, detail);
    }

    /**
     * <p>Gets a stored detail from this attribute</p>
     *
     * @param name Detail Name to fetch
     *
     * @return Object
     */
    public Object getDetail(String name) {
        return details.get(name);
    }

    /**
     * <p>Clears all the attribute's details</p>
     */
    public void clearDetails() {
        details.clear();
    }

    /**
     * <p>Removes a stored detail from this attribute</p>
     *
     * @param name Detail Name to remove
     *
     * @return The removed detail
     */
    public Object removeDetail(String name) {
        return details.remove(name);
    }

    /**
     * <p>Getter for the field <code>hidden</code>.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public boolean isHidden() {
        return hidden != null ? hidden : false;
    }

    /**
     * <p>Setter for the field <code>hidden</code>.</p>
     *
     * @param hidden a {@link java.lang.Boolean}.
     */
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * <p>Getter for the field <code>referenceId</code>.</p>
     *
     * @return a {@link Integer} object.
     */
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
     * <p>Getter for the field <code>name</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
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

    /**
     * <p>Getter for the field <code>description</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
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

    /**
     * <p>Getter for the field <code>dateTime</code>.</p>
     *
     * @return a {@link java.sql.Date} object.
     */
    public Date getDateTime() {
        return dateTime;
    }

    /**
     * <p>Setter for the field <code>sourceDate</code>.</p>
     *
     * @param dateTime a {@link java.sql.Date} object.
     */
    public void setDateTime(Date dateTime) {
        this.dateTime = dateTime;
    }

    /**
     * <p>Getter for the field <code>value</code>.</p>
     *
     * @return a {@link java.lang.Double} object.
     */
    public Double getValue() {
        return value;
    }

    /**
     * <p>Setter for the field <code>value</code>.</p>
     *
     * @param value a {@link java.lang.Double} object.
     */
    public void setValue(Double value) {
        this.value = value;
    }

    /** {@inheritDoc} */
    public String toString() {
        return "Attribute{" +
               "description='" + description + '\'' +
               ", name='" + name + '\'' +
               ", referenceId='" + referenceId + '\'' +
               ", referenceType='" + referenceType + '\'' +
               ", value='" + value + '\'' +
               ", dateTime='" + dateTime + '\'' +
               '}';
    }
}
