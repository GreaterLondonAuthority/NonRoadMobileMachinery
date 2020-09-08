/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.monitoring.utils;

import com.pivotal.utils.Common;
import com.pivotal.utils.I18n;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * An immutable class that holds a sub-parameter value
 */
public class SubParameterValue extends SubParameter {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefinitionSettings.class);
    private String value;

    /**
     * Creates an empty parameter value using the definition
     *
     * @param parameter Parameter definition
     * @param value a {@link java.lang.String} object.
     */
    public SubParameterValue(SubParameter parameter, String value) {
        super(parameter);
        setValue(value);
    }

    /**
     * Creates an empty parameter value using the definition
     *
     * @param parameter Parameter definition
     * @param value a {@link org.w3c.dom.Element} object.
     */
    protected SubParameterValue(SubParameter parameter, Element value) {
        super(parameter);
        setValue(value);
    }

    /**
     * Sets the value of the parameter
     *
     * @param value Value of the parameter
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Returns the String representation of the parameter value
     * or the default if the value is null
     *
     * @return String value
     */
    public String getString() {
        return value==null?getDefaultValue():value;
    }

    /**
     * Returns the Integer value of the parameter value
     *
     * @return int value
     */
    public int getInteger() {
        return Common.parseInt(getString());
    }

    /**
     * Returns the Double value of the parameter value
     *
     * @return double value
     */
    public double getDouble() {
        return Common.parseDouble(getString());
    }

    /**
     * Returns the Date value of the parameter value
     *
     * @return date value
     */
    public Date getDate() {
        return Common.parseDate(getString());
    }

    /**
     * Returns the Date and Time value of the parameter value
     *
     * @return date value
     */
    public Date getDateTime() {
        return Common.parseDateTime(getString());
    }

    /**
     * Returns the Time value of the parameter value
     *
     * @return date value
     */
    public Date getTime() {
        return Common.parseTime(getString());
    }

    /**
     * Sets the value using the XML representation
     * This allows for the deciphering of subparameters
     *
     * @param value Value of the parameter
     */
    public void setValue(Element value) {
        if (value!=null) {
            this.value = value.getAttribute("value");
            if (Common.isBlank(this.value)) {
                this.value = value.getTextContent();
            }
        }
    }

    /**
     * Validates the value and adds a field error if there is a problem
     *
     * @return Map of errors to update
     */
    public Map<String, String> validate() {
        Map<String, String> result = new HashMap<>();

        if (isRequired() && Common.isBlank(getString()))
            result.put(getName(), I18n.getString("system.validation.error.required"));

        else if (!Common.isBlank(getString())) {
            if (isType("numeric") && !getString().matches("[0-9]+(\\.[0-9]*)?"))
                result.put(getName(), I18n.getString("system.validation.error.not_numeric"));

            else if (isType("numeric") && getMaxValue()!=getMinValue() && getMaxValue()!=0 && (getInteger()<getMinValue() || getInteger()>getMaxValue()))
                result.put(getName(), I18n.getString("system.validation.error.out_of_range", getMinValue(), getMaxValue()));

            else if (isType("string") && getMaxLength()!=getMinLength() && getMaxLength()!=0 && (getString().length()<getMinLength() || getString().length()>getMaxValue()))
                result.put(getName(), I18n.getString("system.validation.error.length_out_of_range", getMinLength(), getMaxLength()));

            else if (isType("string") && !Common.isBlank(getPattern()) && !getString().matches(getPattern()))
                result.put(getName(), I18n.getString("system.validation.error.bad_pattern", getPattern()));

            else if (hasChoices() && !getChoicesMap().containsKey(getString()))
                result.put(getName(), I18n.getString("system.validation.error.bad_choice"));
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return super.toString() +
                ", value='" + value + '\'';
    }

    /**
     * Creates an XML node for this parameter using the tag name supplied
     *
     * @param document Document to anchor this node to
     * @return XML Node
     */
    public Element getXmlNode(Document document) {

        Element node = document.createElement("subparameter");
        node.setAttribute("name", getName());

        // If the parameter value is pretty long, then put it into the content

        if (!Common.isBlank(value)) {
            if (value.length() < 100)
                node.setAttribute("value", value);
            else
                node.setTextContent(value);
        }
        return node;
    }

}
