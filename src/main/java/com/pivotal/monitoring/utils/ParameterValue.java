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
import com.pivotal.utils.PivotalException;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.*;

/**
 * An immutable class that holds a parameter definition and value
 */
public class ParameterValue extends Parameter {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefinitionSettings.class);

    private List<Value> values = new ArrayList<>();

    /**
     * Creates an empty parameter value using the definition
     *
     * @param parameter Parameter definition
     */
    public ParameterValue(Parameter parameter) {
        super(parameter);
    }

    /**
     * Creates an empty parameter value using the definition
     *
     * @param parameter Parameter definition
     * @param value a {@link org.w3c.dom.Element} object.
     */
    protected ParameterValue(SubParameter parameter, Element value) {
        super(parameter);
        set(value);
    }

    /**
     * Creates an empty parameter value using the definition
     *
     * @param parameter Parameter definition
     * @param value a {@link org.w3c.dom.Element} object.
     */
    protected ParameterValue(Parameter parameter, Element value) {
        super(parameter);
        set(value);
    }

    /**
     * This returns a Value object for the specified index or null
     * if the index is out of range
     * @return Value object
     */
    private Value getValue() {
        return getValue(0);
    }

    /**
     * This returns a Value object for the specified index or null
     * if the index is out of range
     *
     * @param index Index of the value to get
     * @return Value object
     */
    protected Value getValue(int index) {
        return Common.isBlank(values)||index<0||index>=values.size()?null:values.get(index);
    }

    /**
     * This returns a Value object for the specified index or null
     * if the index is out of range
     *
     * @return Value object
     */
    public String getString() {
        return getString(0);
    }

    /**
     * This returns a Value object for the specified index or null
     * if the index is out of range
     *
     * @param index Index of the value to get
     * @return Value object
     */
    public String getString(int index) {
        Value tmp = getValue(index);
        return tmp==null?null:tmp.getValue();
    }

    /**
     * Returns the Integer value of the parameter value safely
     *
     * @return int value or 0 if the parameter doesn't exist
     */
    public int getInteger() {
        return Common.parseInt(getString());
    }

    /**
     * Returns the Double value of the parameter value safely
     *
     * @return double value or 0.0 if the parameter doesn't exist
     */
    public double getDouble() {
        return Common.parseDouble(getString());
    }

    /**
     * Returns the Date value of the parameter value safely
     *
     * @return date value or null if the parameter doesn't exist
     */
    public Date getDate() {
        return Common.parseDate(getString());
    }

    /**
     * Returns the Date and Time value of the parameter value safely
     *
     * @return date value or null if the parameter doesn't exist
     */
    public Date getDateTime() {
        return Common.parseDateTime(getString());
    }

    /**
     * Returns the Time value of the parameter value safely
     *
     * @return date value or null if the parameter doesn't exist
     */
    public Date getTime() {
        return Common.parseTime(getString());
    }

    /**
     * Returns the Integer value of the parameter value
     *
     * @param index Index of the value to get
     * @return int value
     */
    public int getInteger(int index) {
        return Common.parseInt(getString(index));
    }

    /**
     * Returns the Double value of the parameter value
     *
     * @param index Index of the value to get
     * @return double value
     */
    public double getDouble(int index) {
        return Common.parseDouble(getString(index));
    }

    /**
     * Returns the Date value of the parameter value
     *
     * @param index Index of the value to get
     * @return date value
     */
    public Date getDate(int index) {
        return Common.parseDate(getString(index));
    }

    /**
     * Returns the Date and Time value of the parameter value
     *
     * @param index Index of the value to get
     * @return date value
     */
    public Date getDateTime(int index) {
        return Common.parseDateTime(getString(index));
    }

    /**
     * Returns the Time value of the parameter value
     *
     * @param index Index of the value to get
     * @return date value
     */
    public Date getTime(int index) {
        return Common.parseTime(getString(index));
    }

    /**
     * Sets the value of the parameter
     *
     * @param value Value of the parameter
     */
    public void set(String value) {
        values.clear();
        values.add(new Value(this, value));
    }

    /**
     * Sets the value of the parameter
     *
     * @param index Index of the parameter to get
     * @param value Value of the parameter
     */
    public void set(int index, String value) {
        if (index==values.size())
            values.add(new Value(this, value));
        else {
            Value tmp = getValue(index);
            if (tmp!=null) {
                tmp.value = value;
            }
        }
    }

    /**
     * Sets the value using the XML representation
     * This allows for the deciphering of subparameters
     *
     * @param value Value of the parameter
     */
    protected void set(Element value) {
        values.clear();
        values.add(new Value(this, value));
    }

    /**
     * Adds a new value to the parameter at the end of the list
     *
     * @param value Value of the parameter
     */
    public void add(String value) {
        values.add(new Value(this, value));
    }

    /**
     * Adds this value to the list of values using the XML representation
     * This allows for the deciphering of subparameters
     *
     * @param value Values to add
     */
    protected void add(Element value) {
        values.add(new Value(this, value));
    }

    /**
     * Adds the subvalue as per the name
     *
     * @param name Name of the subvalue
     * @param value Value to add
     * @return a {@link SubParameterValue} object.
     */
    public SubParameterValue setSubValue(String name, String value) {
        return setSubValue(0, name, value);
    }

    /**
     * Sets the subvalue as per the name and index
     *
     * @param index Paramater value to set this
     * @param name Name of the subvalue
     * @param value Value to add
     * @return The update subvalue
     */
    public SubParameterValue setSubValue(int index, String name, String value) {
        SubParameterValue returnValue = null;
        Value tmp = getValue(index);
        if (tmp!=null) {
            if (subParameterExists(name)) {
                returnValue = new SubParameterValue(getSubParameter(name), value);
                tmp.subValues.put(name, returnValue);
            }
            else {
                logger.warn("Attempt set a value to a non-existent subvalue [{}]", name);
            }
        }
        return returnValue;
    }

    /**
     * Returns the subparameter using it's name or null if it doesn't exist
     *
     * @param name Name of the subparameter
     * @return SubParameter
     */
    public SubParameterValue getSubValue(String name) {
        return getSubValue(0, name);
    }

    /**
     * Returns the subparameter using it's name or null if it doesn't exist
     *
     * @param index Index of the parameter to get the subvalue from
     * @param name Name of the subparameter
     * @return SubParameter
     */
    public SubParameterValue getSubValue(int index, String name) {
        Value tmp = getValue(index);
        return tmp==null?null:tmp.subValues.get(name);
    }

    /**
     * Returns the read-only list of subparameter
     *
     * @return List of subparameter
     */
    public List<SubParameterValue> getSubValues() {
        return getSubValues(0);
    }

    /**
     * Returns the read-only list of subparameter
     *
     * @param index Index of the value to get
     * @return List of subparameter
     */
    public List<SubParameterValue> getSubValues(int index) {
        Value tmp = getValue(index);
        return tmp==null?null:new ArrayList<>(tmp.subValues.values());
    }

    /**
     * Returns the read-only map of subparameter
     *
     * @return Case insensitive map of subparameter
     */
    public Map<String, SubParameterValue> getSubValuesMap() {
        return getSubValuesMap(0);
    }

    /**
     * Returns the read-only map of subparameter
     *
     * @param index Index of the value to get
     * @return Case insensitive map of subparameter
     */
    public Map<String, SubParameterValue> getSubValuesMap(int index) {
        Map<String, SubParameterValue> returnValue=null;
        Value tmp = getValue(index);
        if (tmp!=null) {
            returnValue = new LinkedCaseInsensitiveMap<>();
            returnValue.putAll(tmp.subValues);
        }
        return returnValue;
    }

    /**
     * Returns the number of values there are for this parameter
     *
     * @return Number of entries
     */
    public int getCount() {
        return Common.isBlank(values)?0:values.size();
    }

    /**
     * Validates the value and adds a field error if there is a problem
     *
     * @return Map of errors to update
     */
    public Map<String, String> validate() {
        Map<String, String> result = new HashMap<>();
        int intValue = Common.parseInt(getString());

        if (isRequired() && Common.isBlank(getValue()))
            result.put(getName(), I18n.getString("system.validation.error.required"));

        else if (!Common.isBlank(getValue())) {
            if (isType("numeric") && !getString().matches("[0-9]+(\\.[0-9]*)?"))
                result.put(getName(), I18n.getString("system.validation.error.not_numeric"));

            else if (isType("numeric") && getMaxValue()!=getMinValue() && getMaxValue()!=0 && (intValue<getMinValue() || intValue>getMaxValue()))
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
                ", value='" + Common.join(values,",") + '\'';
    }

    /**
     * Creates an XML node for this parameter using the tag name supplied
     *
     * @param document Document to anchor this node to
     * @param tagName The tag name to use
     * @return XML Node
     */
    public List<Element> getXmlNodes(Document document, String tagName) {

        List<Element> returnValue = new ArrayList<>();
        for (Value value : values) {
            Element node = document.createElement(tagName);
            node.setAttribute("name", getName());

            // If the parameter value is pretty long, then put it into the content

            if (!Common.isBlank(value.value)) {
                if (value.value.length() < 100)
                    node.setAttribute("value", value.value);
                else
                    node.setTextContent(value.value);
            }


            // Add on the subparameters if we have any

            if (!Common.isBlank(value.subValues)) {
                Element subNode = document.createElement("subparameters");
                for (SubParameterValue parameter : value.subValues.values()) {
                    subNode.appendChild(parameter.getXmlNode(document));
                }
                node.appendChild(subNode);
            }
            returnValue.add(node);
        }
        return returnValue;
    }

    /**
     * Holding class for a parameter value that may contain subparameter values
     */
    protected class Value {
        String value;
        Map<String, SubParameterValue> subValues = new LinkedCaseInsensitiveMap<>();

        /**
         * Create a value object for this parameter
         * @param value Values to add
         */
        private Value(Parameter parameter, String value) {
            this.value = value;
            if (hasSubParameters()) {
                for (SubParameter sub : parameter.getSubParameters()) {
                    subValues.put(sub.getName(), new SubParameterValue(sub, sub.getDefaultValue()));
                }
            }
        }

        /**
         * Create a value object for this parameter
         * @param parameter The parameter that this value belongs to
         * @param value Values to add
         */
        private Value(Parameter parameter, Element value) {
            this.value = value.getAttribute("value");
            if (Common.isBlank(this.value)) {
                this.value = value.getTextContent();
                if (this.value!=null) this.value = this.value.trim();
            }

            // Add any subparameters

            NodeList nodes= value.getElementsByTagName("subparameter");
            if (!Common.isBlank(nodes)) {
                this.value = null;  // Cant have a value and subparameters
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element element = ((Element) nodes.item(i));
                    String name = element.getAttribute("name");
                    if (Common.isBlank(name)) {
                        throw new PivotalException("A subparamater has been specified without a name");
                    }
                    else if (!parameter.subParameterExists(name)) {
                        throw new PivotalException("A subparamater [%s] has been specified in the data but no definition exists", name);
                    }
                    SubParameterValue paramValue = new SubParameterValue(parameter.getSubParameter(name), element);
                    subValues.put(paramValue.getName(), paramValue);
                }
            }
        }

        /**
         * Returns the value of this parameter
         * @return String value
         */
        protected String getValue() {
            return value;
        }
    }

}
