/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.json.XML;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

/**
 * Provides some general purpose XML builder tools primarily for use
 * in Velocity, hence the rather odd naming convention of capitalised
 * first letter and no leader
 */
public class XMLUtils {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(XMLUtils.class);

    /**
     * Prevents instantiation
     */
    private XMLUtils() {
    }

    /**
     * Creates a tag with the given content and no attributes
     * If the content is null then a truncated tag is created e.g. <gggg/>
     *
     * @param name    Name of the tag to generate
     * @param content Content of the tag (can be null)
     * @return Rendered output
     */
    public static String Tag(String name, String content) {
        return Tag(name, content, (String[]) null);
    }

    /**
     * Creates a tag with the given content
     * If the content is null then a truncated tag is created e.g. <gggg/>
     * If attributes are supplied, they are added to the tag but only if the attribute value is non-null
     *
     * @param name       Name of the tag to generate
     * @param content    Content of the tag (can be null)
     * @param attributes Array of name value pairs
     * @return Rendered output
     */
    public static String Tag(String name, String content, String... attributes) {
        return Tag(name, content, Common.getMapFromPairs(attributes));
    }

    /**
     * Creates a tag with the given content
     * If the content is null then a truncated tag is created e.g. <gggg/>
     * If attributes are supplied, they are added to the tag but only if the attribute value is non-null
     *
     * @param name       Name of the tag to generate
     * @param content    Content of the tag (can be null)
     * @param attributes Map of attribute values (can be null)
     * @return Rendered output
     */
    public static String Tag(String name, String content, Map<String, String> attributes) {
        StringBuilder returnValue = new StringBuilder();

        // Check for stupidity

        if (!Common.isBlank(name)) {
            returnValue.append(StartTag(name, content == null, attributes));

            // Check to see if the content has to be wrapped in a CDATA section

            if (content != null) {
                if (StringUtils.containsAny(content, "\t\n\r\f"))
                    returnValue.append("<![CDATA[" + content.replaceAll("]]>", "]]]]><![CDATA[>") + "]]>");
                else
                    returnValue.append(StringEscapeUtils.escapeXml(content));
                returnValue.append("</" + name + '>');
            }
        }
        return returnValue.toString();
    }

    /**
     * Creates a starting tag with the given name
     * If attributes are supplied, they are added to the tag but only if the attribute value is non-null
     *
     * @param name       Name of the tag to generate
     * @param attributes Map of attribute values (can be null)
     * @return Rendered output
     */
    public static String StartTag(String name, String... attributes) {
        return StartTag(name, Common.getMapFromPairs(attributes));
    }

    /**
     * Creates a starting tag with the given name
     * If attributes are supplied, they are added to the tag but only if the attribute value is non-null
     *
     * @param name       Name of the tag to generate
     * @param attributes Map of attribute values (can be null)
     * @return Rendered output
     */
    public static String StartTag(String name, Map<String, String> attributes) {
        return StartTag(name, false, attributes);
    }

    /**
     * Creates a starting tag with the given name
     * If attributes are supplied, they are added to the tag but only if the attribute value is non-null
     *
     * @param name       Name of the tag to generate
     * @param closed     True if the element is to be closed
     * @param attributes Map of attribute values (can be null)
     * @return Rendered output
     */
    public static String StartTag(String name, boolean closed, String... attributes) {
        return StartTag(name, closed, Common.getMapFromPairs(attributes));
    }

    /**
     * Creates a starting tag with the given name
     * If attributes are supplied, they are added to the tag but only if the attribute value is non-null
     *
     * @param name       Name of the tag to generate
     * @param attributes Map of attribute values (can be null)
     * @param closed     True if the element is to be closed
     * @return Rendered output
     */
    public static String StartTag(String name, boolean closed, Map<String, String> attributes) {
        StringBuilder returnValue = new StringBuilder();

        // Check for stupidity

        if (!Common.isBlank(name)) {
            returnValue.append('<' + name.trim());
            if (!Common.isBlank(attributes)) {

                // Loop through all the attributes adding them to the tag

                for (Map.Entry entry : attributes.entrySet()) {
                    if (entry.getValue() != null)
                        returnValue.append(" " + entry.getKey() + "=\"" + entry.getValue() + '"');
                }
            }

            // If we truncate the tag

            returnValue.append(closed ? "/>" : ">");
        }
        return returnValue.toString();
    }

    /**
     * Creates a closing tag with the given name
     *
     * @param name Name of the tag to generate
     * @return Rendered output
     */
    public static String EndTag(String name) {
        return Common.isBlank(name) ? "" : "</" + name.trim() + '>';
    }

    /**
     * Transforms an XML string using an XSLT string and returns the results
     * as a string
     *
     * @param xml An XML string
     * @param xslt An XSLT script
     *
     * @return Transformed string
     */
    public static String transform(String xml, String xslt) {

        // Check for stupidity

        StringWriter returnValue = new StringWriter();
        if (Common.isBlank(xml))
            logger.error("The XML string is empty");
        else if (Common.isBlank(xslt))
            logger.error("The XSLT transformation string is empty");
        else {

            // Get a factory to work with

            TransformerFactory factory = TransformerFactory.newInstance();

            // Convert string into a stream source and transform it

            Source xsltSource = new StreamSource(new StringReader(xslt));
            try {
                Transformer transformer = factory.newTransformer(xsltSource);
                Source xmlSource = new StreamSource(new StringReader(xml));
                transformer.transform(xmlSource, new StreamResult(returnValue));
            }
            catch (Exception e) {
                logger.error("Problem transforming XML - " + new PivotalException(e));
            }
        }
        return returnValue.toString();
    }

    /**
     * Transforms an XML string using an XSLT string and returns the results
     * as a string
     *
     * @param xml An HttpContent object as per Common.getUrl
     * @param xslt An XSLT script
     *
     * @return Transformed string
     */
    public static String transform(Common.HttpContent xml, String xslt) {

        // Check for stupidity

        StringWriter returnValue = new StringWriter();
        if (Common.isBlank(xml))
            logger.error("The XML source is null");
        else if (Common.isBlank(xml.getContent()))
            logger.error("The XML source content is empty");
        else if (Common.isBlank(xslt))
            logger.error("The XSLT transformation string is empty");
        else {

            // Get a factory to work with

            TransformerFactory factory = TransformerFactory.newInstance();

            // Convert string into a stream source and transform it

            Source xsltSource = new StreamSource(new StringReader(xslt));
            try {
                Transformer transformer = factory.newTransformer(xsltSource);
                Source xmlSource = new StreamSource(new StringReader(xml.getContent()));
                transformer.transform(xmlSource, new StreamResult(returnValue));
            }
            catch (Exception e) {
                logger.error("Problem transforming XML - " + new PivotalException(e));
            }
        }
        return returnValue.toString();
    }

    /**
     * Transforms an XML string using an XSLT string and returns the results
     * as a string
     *
     * @param xml An HttpContent object as per Common.getUrl
     * @param xslt An XSLT script
     *
     * @return Transformed string
     */
    public static String transform(DomHelper xml, String xslt) {

        // Check for stupidity

        StringWriter returnValue = new StringWriter();
        if (Common.isBlank(xml))
            logger.error("The XML source is null");
        else if (Common.isBlank(xslt))
            logger.error("The XSLT transformation string is empty");
        else {

            // Get a factory to work with

            TransformerFactory factory = TransformerFactory.newInstance();

            // Convert DomHelper into a DOM source and transform it

            Source xsltSource = new StreamSource(new StringReader(xslt));
            try {
                Transformer transformer = factory.newTransformer(xsltSource);
                Source xmlSource = new DOMSource(xml.getDocument());
                transformer.transform(xmlSource, new StreamResult(returnValue));
            }
            catch (Exception e) {
                logger.error("Problem transforming XML - " + new PivotalException(e));
            }
        }
        return returnValue.toString();
    }

    /**
     * Converts the XML into JSON notation
     *
     * @param xml XML string
     * @return JSON output
     */
    public static String toJSON(String xml) {
        String returnValue = xml;
        try {
            returnValue = XML.toJSONObject(returnValue).toString(2);
        }
        catch (Exception e) {
            logger.error("Problem transforming XML - " + new PivotalException(e));
        }
        return returnValue;
    }

}
