/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.hibernate.entities;

import com.pivotal.system.attributes.Attribute;
import com.pivotal.system.attributes.AttributesManager;

import javax.persistence.Transient;
import java.util.Date;
import java.util.Map;

/**
 * The parent of all entities - a very handy way of adding common
 * functionality to every entity
 */
public abstract class AbstractEntity {

    /**
     * Gets a list of any attributes that exist for this object
     * @return Map of attributes or null if none exist
     */
    @Transient
    public Map<String, Attribute> getAttributes() {
        return AttributesManager.getAttributes(this);
    }

    /**
     * Gets the named attribute for this object
     * @param name Name of the attribute to get
     * @return Status object or null if it doesn't
     */
    @Transient
    public Attribute getAttribute(String name) {
        return AttributesManager.getAttribute(this, name);
    }

    /**
     * Adds the specified attribute to this object
     * @param name Name of the attribute
     * @return The newly created attribute
     */
    @Transient
    public Attribute addAttribute(String name) {
        return AttributesManager.addAttribute(this, name);
    }

    /**
     * Adds the specified attribute to this object
     * @param name Name of the attribute
     * @param value Value of attribute
     * @param dateTime Date time to use
     * @return The newly created attribute
     */
    @Transient
    public Attribute addAttribute(String name, Double value, Date dateTime) {
        return AttributesManager.addAttribute(this, name, value, dateTime);
    }

    /**
     * Adds the specified attribute to this object
     * @param name Name of the attribute
     * @param description Description of the attribute
     * @param dateTime Date time to use
     * @return The newly created attribute
     */
    @Transient
    public Attribute addAttribute(String name, String description, Date dateTime) {
        return AttributesManager.addAttribute(this, name, description, null, dateTime);
    }

    /**
     * Adds the specified attribute to this object
     * @param name Name of the attribute
     * @param value Value of attribute
     * @return The newly created attribute
     */
    @Transient
    public Attribute addAttribute(String name, Double value) {
        return AttributesManager.addAttribute(this, name, null, value, null);
    }

    /**
     * Adds the specified attribute to this object
     * @param name Name of the attribute
     * @param description Description of the attribute
     * @param value Value of attribute
     * @return The newly created attribute
     */
    @Transient
    public Attribute addAttribute(String name, String description, Double value) {
        return AttributesManager.addAttribute(this, name, description, value, null);
    }

    /**
     * Adds the specified attribute to this object
     * @param name Name of the attribute
     * @param description Description of the attribute
     * @return The newly created attribute
     */
    @Transient
    public Attribute addAttribute(String name, String description) {
        return AttributesManager.addAttribute(this, name, description);
    }

    /**
     * Removes the specified attribute from this object
     * @param name Removes the attribute
     */
    @Transient
    public void removeAttribute(String name) {
        AttributesManager.removeAttribute(this, name);
    }

    /**
     * Clears all attributes for this object
     */
    @Transient
    public void clearAttributes() {
        AttributesManager.clear(this);
    }

    /**
     * Returns true if the attribute exists
     * @param name Name of the attribute to retrieve
     * @return True if attribute exists
     */
    @Transient
    public boolean attributeExists(String name) {
        return AttributesManager.attributeExists(this, name);
    }

    @Transient
    protected String safeValue(String value) {
        return value==null?"null":("'" + value + "'");
    }

    @Transient
    protected String safeValue(Integer value) {
        return value==null?"null":("'" + value + "'");
    }
}
