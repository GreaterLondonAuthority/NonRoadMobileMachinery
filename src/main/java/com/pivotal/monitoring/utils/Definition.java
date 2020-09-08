/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.monitoring.utils;

import com.pivotal.system.hibernate.entities.DistributionListEntity;
import com.pivotal.utils.*;
import com.pivotal.web.servlet.ServletHelper;
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
 * Holds a definition of a settings object normally by parsing the XML definition
 * as stored in the database
 */
public class Definition {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Definition.class);

    private Map<String,Parameter> parametersMap = new LinkedCaseInsensitiveMap<>();
    private int id;
    private String name = null;
    private Class source;

    /**
     * Empty definition object
     */
    public Definition() {
    }


    /**
     * Creates a Template definition from the Distribution List Entity
     * The definition XML is validated to make sure it complies with the XSD
     *
     * @param distributionList the Distribution List
     * @throws Exception java.lang.Exception if there is an issue with the XML
     */
    public Definition(DistributionListEntity distributionList) throws Exception {

        // Get the XML if there is any - if there isn't then it must be parameter-less

        if (distributionList ==null)
            logger.error("Cannot get definition for a null type");
        else {
            init(distributionList.getDefinitionXML());
            source = distributionList.getClass();
            name = distributionList.getType();
        }
    }

    /**
     * Creates a Collector definition from the XML definition
     * The definition XML is validated to make sure it complies with the XSD
     *
     * @param definitionXML XML to parse
     * @throws java.lang.Exception java.lang.Exception if there is an issue with the XML
     */
    public Definition(String definitionXML) throws Exception {
        init(definitionXML);
    }

    /**
     * Creates a Collector definition from the XML definition
     * The definition XML is validated to make sure it complies with the XSD
     *
     * @param definitionXML XML to parse
     *
     * @throws Exception Exception if there is an issue with the XML
     */
    protected void init(String definitionXML) throws Exception {

        // Get the XML if there is any - if there isn't then it must be parameter-less

        if (!Common.isBlank(definitionXML)) {

            // Check the definition

            XMLValidationResult result = DomHelper.validateXML(ServletHelper.getRealPath(com.pivotal.web.Constants.APP_XSD_FILE), definitionXML);
            if (result.isInError()) {
                throw new PivotalException("The XML is not valid - " + result.getError());
            }

            // Get a document form the XML

            DomHelper domHelper = new DomHelper(definitionXML);

            // Get a list of the parameters

            List<Element> elements = domHelper.getElements("parameter");
            if (!Common.isBlank(elements)) {
                for (Element element : elements) {
                    Parameter parameter = new Parameter(domHelper, element);
                    parametersMap.put(parameter.getName(), parameter);
                }
            }
        }
    }

    /**
     * Returns the read-only parameter
     *
     * @param name Name of the parameter
     * @return Parameter
     */
    public Parameter getParameter(String name) {
        if (!Common.isBlank(parametersMap))
            return parametersMap.get(name);
        else
            return null;
    }

    /**
     * Returns the read-only list of parameters
     *
     * @return List of parameters
     */
    public List<Parameter> getParameters() {
        return new ArrayList<>(parametersMap.values());
    }

    /**
     * Returns the read-only map of parameters
     *
     * @return Case insensitive map of parameters
     */
    public Map<String, Parameter> getParametersMap() {
        Map<String, Parameter> returnValue = new LinkedCaseInsensitiveMap<>();
        returnValue.putAll(parametersMap);
        return returnValue;
    }

    /**
     * Returns true if the parameter exists
     *
     * @param name Name of the parameter
     * @return True if exists
     */
    public boolean parameterExists(String name) {
        return parametersMap.containsKey(name);
    }

    /**
     * Add a parameter to the definition
     *
     * @param name Name of the parameter
     * @param label Display label
     * @param description Description of the parameter
     * @param type Type of the parameter
     * @param defaultValue Default value
     * @param required True if it is required
     * @return Newly created parameter
     */
    public Parameter addParameter(String name, String label, String description, String type, String defaultValue, boolean required) {
        return addParameter(name, label, description, type, defaultValue, required, null);
    }

    /**
     * Add a parameter to the definition
     *
     * @param name Name of the parameter
     * @param label Display label
     * @param description Description of the parameter
     * @param type Type of the parameter
     * @param defaultValue Default value
     * @param required True if it is required
     * @param choices String of choices in name:[value],name:[value] format
     * @return Newly created parameter
     */
    public Parameter addParameter(String name, String label, String description, String type, String defaultValue, boolean required, String choices) {
        Parameter param = new Parameter(name, label, description, type, defaultValue, required);
        if (!Common.isBlank(choices)) {
            String[] rows = Common.splitQuotedStrings(choices);
            for (String row : rows) {
                param.addChoice(Common.getItem(row, " *: *", 0), Common.getItem(row, " *: *", 1));
            }
        }
        parametersMap.put(name, param);
        return param;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        String returnValue = "Definition{";
        if (!Common.isBlank(parametersMap)) {
            boolean first = true;
            for (Parameter parameter : parametersMap.values()) {
                if (!first) returnValue += ", ";
                returnValue += parameter;
                first = false;
            }
        }
        returnValue += '}';
        return returnValue;
    }

    /**
     * Returns an XML string containing just the configuration
     *
     * @return XML configuration
     */
    public String getXml() {

        StringWriter output = new StringWriter();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            Element root = document.createElement("parameters");
            document.appendChild(root);
            for (Parameter parameter : getParameters()) {
                root.appendChild(parameter.getXmlNode(document, "parameter"));
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
            logger.error("Problem creating XML - {}", PivotalException.getErrorMessage(e));
        }
        return output.toString();
    }

    /**
     * Returns the ID of te source of this definition if known
     *
     * @return ID of source (0 if not known)
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the name of te source of this definition if known
     *
     * @return Name of source (null if not known)
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the source of this definition
     * @param name Name of source
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the class of the source if it is known
     *
     * @return Source class or null if not derived from an object
     */
    public Class getSource() {
        return source;
    }

    /**
     * Returns true if there are no parameters declared in this definition
     *
     * @return True if empty
     */
    public boolean isEmpty() {
        return Common.isBlank(getParameters());
    }

}
