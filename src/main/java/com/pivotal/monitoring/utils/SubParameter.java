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
import com.pivotal.utils.DomHelper;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An immutable class that holds a parameter definition and value
 */
public class SubParameter {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefinitionSettings.class);

    private String name;
    private String label;
    private String description;
    private String type;
    private String subType;
    private String defaultValue;
    private boolean required;
    private String scope;
    private boolean multiple;
    private boolean multiselect;
    private int maxLength;
    private int minLength;
    private double maxValue;
    private double minValue;
    private String pattern;
    private Map<String, Choice> choicesMap = new LinkedCaseInsensitiveMap<>();

    /**
     * Creates a skeleton parameter
     *
     * @param name Name of the parameter
     */
    protected SubParameter(String name) {
        this.name = name;
        this.label = name;
    }

    /**
     * Constructs a parameter using the supplied values
     *
     * @param name Name of the parameter
     * @param label Display label
     * @param description Description of the parameter
     * @param type Type of the parameter
     * @param defaultValue Default value
     * @param required True if it is required
     */
    protected SubParameter(String name, String label, String description, String type, String defaultValue, boolean required) {
        this.name = name;
        this.label = label;
        this.description = description;
        this.type = type;
        this.defaultValue = defaultValue;
        this.required = required;
    }

    /**
     * Creates a parameter class representing the definition
     *
     * @param domHelper XML reference
     * @param element Parameter node
     */
    protected SubParameter(DomHelper domHelper, Element element) {
        name = element.getAttribute("name");
        label = element.getAttribute("label");
        if (Common.isBlank(label)) label=name;
        description = element.getAttribute("description");
        type = element.getAttribute("type");
        subType = element.getAttribute("subtype");
        defaultValue = element.getAttribute("default");
        required = Common.isYes(element.getAttribute("required"));
        scope = element.getAttribute("scope");
        multiple = Common.isYes(element.getAttribute("multiple"));
        multiselect = Common.isYes(element.getAttribute("multiselect"));
        maxLength = Common.parseInt(element.getAttribute("maxlength"));
        minLength = Common.parseInt(element.getAttribute("minlength"));
        maxValue = Common.parseDouble(element.getAttribute("maxvalue"));
        minValue = Common.parseDouble(element.getAttribute("minvalue"));
        pattern = element.getAttribute("pattern");
        List<Element> elements = domHelper.getElements("choice", element);
        if (!Common.isBlank(elements)) {
            for (Element choice : elements) {
                Choice tmp = new Choice(choice);
                choicesMap.put(tmp.getName(), tmp);
            }
        }
        logger.debug("Created new Parameter - {}", this);
    }

    /**
     * Constructs a Parameter using the one passed to us
     *
     * @param parameter Parameter to copy
     */
    protected SubParameter(SubParameter parameter) {
        if (parameter!=null) {
            name = parameter.getName();
            label = parameter.getLabel();
            description = parameter.getDescription();
            type = parameter.getType();
            subType = parameter.getSubType();
            defaultValue = parameter.getDefaultValue();
            required = parameter.isRequired();
            scope = parameter.getScope();
            multiple = parameter.isMultiple();
            multiselect = parameter.isMultiselect();
            maxLength = parameter.getMaxLength();
            minLength = parameter.getMinLength();
            maxValue = parameter.getMaxValue();
            minValue = parameter.getMinValue();
            pattern = parameter.getPattern();
            choicesMap.putAll(parameter.getChoicesMap());
        }
    }

    /**
     * Adds a choice object to the parameter
     *
     * @param name Name of the choice
     * @param value Value of the choice (can be null)
     * @return The newly create Choice object
     */
    public Choice addChoice(String name, String value) {
        Choice choice = new Choice(name, value);
        choicesMap.put(name, choice);
        return choice;
    }

    /**
     * Returns the unique name of the parameter
     *
     * @return Name of the parameter
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the display label for the parameter - if blank, the name is used
     *
     * @return Label to use for display
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns a description of the parameter
     *
     * @return Description of null if it doesn't exist
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the type nam of the parameter e.g. string, numeric etc.
     *
     * @return Returns the type name
     */
    public String getType() {
        return type;
    }

    /**
     * Returns true if the type of this parameter is any of the specified ones
     *
     * @param type One or more types to match
     * @return True if type matches
     */
    public boolean isType(String... type) {
        return Common.doStringsMatch(this.type, type);
    }

    /**
     * Returns the subtype name of the parameter
     * This is the comma separated list of types that are allowed for this parameter
     *
     * @return Returns the sub type name
     */
    public String getSubType() {
        return subType;
    }

    /**
     * Returns true if the subtype of this parameter is any of the specified ones
     *
     * @param subType One or more subtypes to match
     * @return True if subtype matches
     */
    @SuppressWarnings("unused")
    public boolean isSubType(String... subType) {
        return Common.doStringsMatch(this.subType, subType);
    }

    /**
     * Returns true if the scope of this parameter is any of the specified ones
     *
     * @param scope One or more scopes to match
     * @return True if scopes matches
     */
    @SuppressWarnings("unused")
    public boolean isScope(String... scope) {
        return Common.doStringsMatch(this.scope, scope);
    }

    /**
     * Returns a String representation of the default value
     * If a list of choices is available, this relates to the value field
     *
     * @return Any default value
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Returns true if a value for this parameter is mandatory
     *
     * @return True if required
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Returns the scope of this parameter
     *
     * @return Scope name
     */
    public String getScope() {
        return scope;
    }

    /**
     * Returns true multiple values are allowed for this parameter
     *
     * @return True if multiple
     */
    public boolean isMultiple() {
        return multiple;
    }

    /**
     * Returns true if multiselect values are allowed for this parameter
     *
     * @return True if multiselect
     */
    public boolean isMultiselect() {
        return multiselect;
    }

    /**
     * Returns the maximum number of characters that can be saved
     * Only appropriate for a String type
     *
     * @return Maximum String length
     */
    public int getMaxLength() {
        return maxLength;
    }

    /**
     * Returns the minimum number of characters that can be saved
     * Only appropriate for a String type
     *
     * @return Minimum String length
     */
    public int getMinLength() {
        return minLength;
    }

    /**
     * Returns the maximum value of this parameter as a String e.g. date or number
     * Only appropriate for non String types
     *
     * @return Maximum value expressed as a double
     */
    public double getMaxValue() {
        return maxValue;
    }

    /**
     * Returns the minimum value of this parameter as a String e.g. date or number
     * Only appropriate for non String types
     *
     * @return Minimum value expressed as a double
     */
    public double getMinValue() {
        return minValue;
    }

    /**
     * Returns the validation pattern for the parameter value
     * This is expressed as a Regular Expression
     *
     * @return Validation pattern
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * List of possible choices that the user must choose from
     *
     * @return Option list of values
     */
    public List<Choice> getChoices() {
        return new ArrayList<>(choicesMap.values());
    }

    /**
     * List of possible choices that the user must choose from in a form
     * suitable for displaying in a select box
     *
     * @return Option list of values
     */
    @SuppressWarnings("unused")
    public List<Map<String,String>> getChoicesDisplay() {
        List<Map<String,String>> returnValue = null;
        if (!Common.isBlank(choicesMap)) {
            returnValue = new ArrayList<>();
            for (Choice choice : choicesMap.values()) {
                returnValue.add(Common.getMapFromPairs("text", choice.name, "value", choice.value + ""));
            }
        }
        return returnValue;
    }

    /**
     * Returns true if this parameter is a selection list
     *
     * @return True if choices available
     */
    public boolean hasChoices() {
        return !Common.isBlank(choicesMap);
    }

    /**
     * Map of possible choices that the uer must choose from
     *
     * @return Optional case insensitive map of values
     */
    public Map<String, Choice> getChoicesMap() {
        return choicesMap;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        String returnValue =  "Parameter{" +
                "name='" + name + '\'' +
                ", label='" + label + '\'' +
                ", description='" + description + '\'' +
                ", type='" + type + '\'' +
                ", subtype='" + subType + '\'' +
                ", defaultValue='" + defaultValue + '\'' +
                ", required=" + required +
                ", scope=" + scope +
                ", multiple=" + multiple +
                ", multiselect=" + multiselect +
                ", maxLength=" + maxLength +
                ", minLength=" + minLength +
                ", maxValue='" + maxValue + '\'' +
                ", minValue='" + minValue + '\'' +
                ", pattern='" + pattern + '\'';
        if (!Common.isBlank(choicesMap)) {
            returnValue+="Choices{";
            boolean first=true;
            for (Choice choice : choicesMap.values()) {
                if (!first) returnValue+=", ";
                returnValue+=choice;
                first = false;
            }
            returnValue+="}";
        }
        returnValue+='}';
        return returnValue;
    }

    /**
     * Creates an XML node for this parameter using the tag name supplied
     *
     * @param document Document to anchor this node to
     * @param tagName The tag name to use
     * @return XML Node
     */
    public Element getXmlNode(Document document, String tagName) {
        Element node = document.createElement(tagName);
        node.setAttribute("name", getName());
        node.setAttribute("type", getType());
        if (isRequired()) node.setAttribute("required", "true");
        if (!Common.isBlank(getScope())) node.setAttribute("scope", getScope());
        if (isMultiple()) node.setAttribute("multiple", "true");
        if (isMultiselect()) node.setAttribute("multiselect", "true");
        if (!Common.isBlank(getSubType())) node.setAttribute("subtype", getSubType());
        if (!Common.isBlank(getLabel())) node.setAttribute("label", getLabel());
        if (!Common.isBlank(getDescription())) node.setAttribute("description", getDescription());
        if (!Common.isBlank(getDefaultValue())) node.setAttribute("default", getDefaultValue());
        if (!Common.isBlank(getPattern())) node.setAttribute("pattern", getPattern());
        if (!(getMinValue()==getMaxValue() && getMinValue()==0)) {
            node.setAttribute("minvalue", getMinValue() + "");
            node.setAttribute("maxvalue", getMaxValue() + "");
        }
        if (Common.doStringsMatch(getType(), "string)") && !(getMinLength()==getMaxLength() && getMinLength()==0)) {
            node.setAttribute("minlength", getMinLength() + "");
            node.setAttribute("maxlength", getMaxLength() + "");
        }

        // Add on the choices if we have any

        if (!Common.isBlank(choicesMap)) {
            Element choicesNode = document.createElement("choices");
            for (SubParameter.Choice choice : choicesMap.values()) {
                choicesNode.appendChild(choice.getXmlNode(document));
            }
            node.appendChild(choicesNode);
        }

        return node;
    }

    /**
     * An immutable class that hold a user input choice
     */
    public class Choice {
        private String name;
        private Object value;
        private String description;

        /**
         * Create a choice element from the XML attributes
         * @param element DOM element
         */
        Choice(Element element) {
            name = element.getAttribute("name");
            value = element.getAttribute("value");
            description = element.getAttribute("description");
        }

        /**
         * Creates a new Choice object using the name and value
         * @param name Name of the choice
         * @param value Value of the choice (if empty, uses the name)
         */
        Choice(String name, String value) {
            this(name, value, null);
        }

        /**
         * Creates a new Choice object using the name and value
         * @param name Name of the choice
         * @param value Value of the choice (if empty, uses the name)
         * @param description Description of the choice
         */
        Choice(String name, String value, String description) {
            this.name = name;
            this.value = Common.isBlank(value)?name:value;
            this.description = description;
        }

        /**
         * Returns the name of the choice
         *
         * @return Name
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the description of the choice
         *
         * @return Description
         */
        public String getDescription() {
            return description;
        }

        /**
         * Returns the String representation of the value
         *
         * @return Value expressed as a String
         */
        public Object getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "Choice{" +
                    "name='" + name + '\'' +
                    ", value=" + value +
                    ", description=" + description +
                    '}';
        }

        /**
         * Creates an XML node from the choice properties
         * @param document Document to anchor the node to
         * @return XML Node
         */
        public Node getXmlNode(Document document) {
            Element choiceNode = document.createElement("choice");
            choiceNode.setAttribute("name", getName());
            choiceNode.setAttribute("value", getValue().toString());
            choiceNode.setAttribute("description", getDescription());
            return choiceNode;
        }
    }

}
