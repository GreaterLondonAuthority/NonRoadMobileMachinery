/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.monitoring.utils;

import com.pivotal.system.hibernate.entities.AbstractEntity;
import com.pivotal.system.hibernate.entities.ScheduledTaskEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.Common;
import com.pivotal.utils.DomHelper;
import com.pivotal.utils.PivotalException;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.WordUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class wraps a definition with one that includes the values
 * specified for each parameter in a settings XML
 * A Definition is a completely general purpose way of defining a suite
 * of parameters associated with an object of some sort
 * Some helper methods have been added to this Class that assume there
 * are parameters available called certain names - these are just
 * shorthand for the normal Getters to make life easier for the caller
 */
public class DefinitionSettings {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefinitionSettings.class);

    private Map<String,ParameterValue> parameterValuesMap = new LinkedCaseInsensitiveMap<>();
    private Definition definition;


    /**
     * Creates a transducer definition from the task and adds in the parameter values
     *
     * @param task The task to model
     * @throws java.lang.Exception if there is an issue with the XML
     */
    public DefinitionSettings(ScheduledTaskEntity task) throws Exception {
        this(task.getReport().getParameters(), task.getSettingsXML());
    }

    /**
     * Creates a definition and adds in the parameter values
     *
     * @param definition Definition description
     * @throws java.lang.Exception if any.
     */
    public DefinitionSettings(Definition definition) throws Exception {
        this(definition, null);
    }

    /**
     * Creates a definition and adds in the parameter values
     *
     * @param definition Definition description
     * @param settingsXML Values for the parameters
     * @throws java.lang.Exception if there is an issue with the XML
     */
    public DefinitionSettings(Definition definition, String settingsXML) throws Exception {

        this.definition = definition;
        if (definition!=null && !Common.isBlank(definition.getParametersMap())) {

            // If we only have a definition

            if (Common.isBlank(settingsXML)) {
                for (Parameter parameter : definition.getParametersMap().values()) {
                    parameterValuesMap.put(parameter.getName(), new ParameterValue(parameter));
                }
            }
            else {

                // This will handle the Test data move into H2 then need unescape

                settingsXML = StringEscapeUtils.unescapeJava(settingsXML);

                // We now need to parse the settings values from the transducer and
                // add them into parameters
                // This is tricky, because it involves managing the situation where a parameter
                // can have multiple instances for the same name - this represents a multi-value
                // parameter

                DomHelper domHelper = new DomHelper(settingsXML);

                // Loop through all the values adding them to the definition

                List<Element> parameterValues = domHelper.getElements("parameter");
                if (!Common.isBlank(parameterValues)) {
                    for (Element parameterValue : parameterValues) {
                        String name = parameterValue.getAttribute("name");

                        // Check for stupidity

                        if (Common.isBlank(name)) {
                            throw new PivotalException("A parameter is specified without a name", name);
                        }

                        // Check to see if we already have the parameter and if so, add the value to it

                        else if (parameterExists(name)) {
                            getValue(name).add(parameterValue);
                        }

                        // If we don't have the value yet but we do have the definition

                        else if (definition.parameterExists(name)) {
                            parameterValuesMap.put(name, new ParameterValue(definition.getParameter(name), parameterValue));
                        }

                        // We don't have a definition

                        else {
                            throw new PivotalException("There is a value specified in the settings for a non-existent parameter [%s]", name);
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the underlying definition of these settings
     *
     * @return Definition object
     */
    public Definition getDefinition() {
        return definition;
    }

    /**
     * Returns true if the parameter exists
     *
     * @param name Name of the parameter
     * @return True if exists
     */
    public boolean parameterExists(String name) {
        return parameterValuesMap.containsKey(name);
    }

    /**
     * Returns the read-only parameter value
     *
     * @param name Name of the parameter
     * @return Parameter
     */
    public ParameterValue getValue(String name) {
        return parameterValuesMap.get(name);
    }

    /**
     * Returns the read-only list of parameter values
     *
     * @return List of parameters
     */
    public List<ParameterValue> getValues() {
        return new ArrayList<>(parameterValuesMap.values());
    }

    /**
     * Returns the read-only map of parameter values
     *
     * @return Case insensitive map of parameters
     */
    @SuppressWarnings("unused")
    public Map<String, ParameterValue> getValuesMap() {
        Map<String, ParameterValue> returnValue = new LinkedCaseInsensitiveMap<>();
        returnValue.putAll(parameterValuesMap);
        return returnValue;
    }

    /**
     * Adds the contents of the specified definition values to this one
     * If the parameter already exists and the new value is not null, then the
     * existing value is overwritten otherwise it is simply added
     *
     * @param values Definition values to add
     * @return This object
     */
    public DefinitionSettings add(DefinitionSettings values) {
        if (values!=null) {
            for (ParameterValue parameter : values.getValues()) {
                if (parameterExists(parameter.getName())) {
                    if (!Common.isBlank(parameter.getString()))
                        getValue(parameter.getName()).set(parameter.getString());
                }
                else {
                    parameterValuesMap.put(parameter.getName(), parameter);
                }
            }
        }
        return this;
    }

    /**
     * Returns the Entity pointed to by the parameter value
     * or null if the value doesn't exist
     *
     * @param name Name of the parameter
     * @param <x> required type
     * @return String value or null if the parameter doesn't exist
     */
    public <x extends AbstractEntity> x getValueEntity(String name) {
        return getValueEntity(name, 0);
    }

    /**
     * Returns the Entity pointed to by the parameter value
     * or null if the value doesn't exist. If there is no value, but
     * there is a default in the definition, then that is returned
     *
     * @param name Name of the parameter
     * @param <x> required type
     * @return String value or null if the parameter doesn't exist
     * @param index a int.
     */
    @SuppressWarnings("unchecked")
    public <x extends AbstractEntity> x getValueEntity(String name, int index) {
        Object returnValue = null;
        String valueString = getValueString(name, index);
        if (!Common.isBlank(valueString)) {
            ParameterValue value = getValue(name);
            returnValue = HibernateUtils.getEntity(WordUtils.capitalize(value.getType()) + "Entity", Common.parseInt(valueString));
        }
        return (x)returnValue;
    }

    /**
     * Returns the String representation of the parameter value
     * or null if the value doesn't exist
     *
     * @param name Name of the parameter
     * @return String value or null if the parameter doesn't exist
     */
    public String getValueString(String name) {
        return getValueString(name, 0);
    }

    /**
     * Returns the String representation of the parameter value
     * or null if the value doesn't exist. If there is no value, but
     * there is a default in the definition, then that is returned
     *
     * @param name Name of the parameter
     * @return String value or null if the parameter doesn't exist
     * @param index a int.
     */
    public String getValueString(String name, int index) {
        String returnValue = null;
        if (!Common.isBlank(name)) {
            ParameterValue param = parameterValuesMap.get(name);
            if (param == null) {
                if (definition != null) {
                    Parameter tmp = definition.getParameter(name);
                    if (tmp == null)
                        logger.error("Unknown parameter [{}]", name);
                    else
                        returnValue = tmp.getDefaultValue();
                }
            }
            else if (param.getValue(index) != null) {
                returnValue = param.getValue(index).getValue();
            }
        }
        return returnValue;
    }

    /**
     * Loop through each of the parameters checking if it is correctly input
     *
     * @return Error result to update
     */
    public Map<String, String> validate() {
        Map<String, String> returnValue = new LinkedCaseInsensitiveMap<>();
        for (ParameterValue parameter : getValues()) {
            returnValue.putAll(parameter.validate());
        }
        return returnValue;
    }

    /**
     * Returns an XML string containing just the values portion of this
     *
     * @return XML string containing the values
     */
    public String getXml() {

        StringWriter output = new StringWriter();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            Element root = document.createElement("parameters");
            document.appendChild(root);
            for (ParameterValue parameter : getValues()) {
                List<Element> nodes = parameter.getXmlNodes(document, "parameter");
                if (!Common.isBlank(nodes)) {
                    for (Element node: nodes) {
                        if (!Common.isBlank(node.getTextContent()) ||
                            !Common.isBlank(node.getAttribute("value")) ||
                            !Common.isBlank(node.getChildNodes())) {
                            root.appendChild(node);
                        }
                    }
                }
            }

            // Get the XML

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.MEDIA_TYPE, "text/xml");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(output);
            transformer.transform(source, result);
        }
        catch (ParserConfigurationException | TransformerException e) {
            logger.error("Problem creating XML for settings - {}", PivotalException.getErrorMessage(e));
        }
        return output.toString();
    }

    /**
     * Sets the value of a parameter if it exists
     *
     * @param name Name of the parameter
     * @param value Value to assign
     */
    public void setParameterValue(String name, String value) {
        ParameterValue param = getValue(name);
        if (param!=null) {
            param.set(value);
        }
    }

    /**
     * Sets the value of a parameter if it exists.
     *
     * @param name Name of the parameter
     * @param value int Value to assign
     */
    public void setParameterValue(String name, int value) {
        setParameterValue(name, String.valueOf(value));
    }
}
