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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An immutable class that holds a parameter definition and value
 */
public class Parameter extends SubParameter {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefinitionSettings.class);

    private Map<String, SubParameter> subParametersMap = new LinkedCaseInsensitiveMap<>();

    /**
     * Creates a skeleton parameter
     *
     * @param name Name of the parameter
     */
    public Parameter(String name) {
        super(name);
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
    public Parameter(String name, String label, String description, String type, String defaultValue, boolean required) {
        super(name, label, description, type, defaultValue, required);
    }

    /**
     * Creates a parameter class representing the definition
     *
     * @param domHelper XML reference
     * @param element Parameter node
     */
    public Parameter(DomHelper domHelper, Element element) {
        super(domHelper, element);
        List<Element> elements = domHelper.getElements("subparameter", element);
        if (!Common.isBlank(elements)) {
            for (Element subParameter : elements) {
                SubParameter sub = new SubParameter(domHelper, subParameter);
                subParametersMap.put(sub.getName(), sub);
            }
        }
        logger.debug("Created new Parameter - {}", this);
    }

    /**
     * Constructs a Parameter using the one passed to us
     *
     * @param parameter Parameter to copy
     */
    public Parameter(SubParameter parameter) {
        super(parameter);
    }

    /**
     * Constructs a Parameter using the one passed to us
     *
     * @param parameter Parameter to copy
     */
    public Parameter(Parameter parameter) {
        super(parameter);
        if (parameter!=null && parameter.getSubParametersMap()!=null) {
            subParametersMap.putAll(parameter.getSubParametersMap());
        }
    }

    /**
     * Returns the read-only list of subparameter
     *
     * @return List of subparameter
     */
    public List<SubParameter> getSubParameters() {
        return new ArrayList<>(subParametersMap.values());
    }

    /**
     * Returns the read-only map of subparameter
     *
     * @return Case insensitive map of subparameter
     */
    public Map<String, SubParameter> getSubParametersMap() {
        Map<String, SubParameter> returnValue = new LinkedCaseInsensitiveMap<>();
        returnValue.putAll(subParametersMap);
        return returnValue;
    }

    /**
     * Returns the subparameter using it's name or null if it doesn't exist
     *
     * @param name Name of the subparameter
     * @return SubParameter
     */
    public SubParameter getSubParameter(String name) {
        return subParametersMap.get(name);
    }

    /**
     * Returns true if this Parameter contains subparameter definitions
     *
     * @return True if subparameters are defined
     */
    public boolean hasSubParameters() {
        return !Common.isBlank(subParametersMap);
    }

    /**
     * Returns true if the subparameter exists
     *
     * @param name Name of the subparameter
     * @return True if exists
     */
    public boolean subParameterExists(String name) {
        return subParametersMap.containsKey(name);
    }

    /**
     * {@inheritDoc}
     *
     * Creates an XML node for this parameter using the tag name supplied
     */
    public Element getXmlNode(Document document, String tagName) {
        Element node = super.getXmlNode(document, tagName);

        // Add on the subparameters if we have any

        if (!Common.isBlank(subParametersMap)) {
            Element choicesNode = document.createElement("subparameters");
            for (SubParameter parameter : subParametersMap.values()) {
                choicesNode.appendChild(parameter.getXmlNode(document, "subparameter"));
            }
            node.appendChild(choicesNode);
        }
        return node;
    }
}
