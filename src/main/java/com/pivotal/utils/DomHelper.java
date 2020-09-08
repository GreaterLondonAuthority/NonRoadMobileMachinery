/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils;

import com.googlecode.htmlcompressor.compressor.XmlCompressor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.*;

/**
 * This class wraps an XML snippet and allows users to get values from
 * it using simple function calls
 */
public class DomHelper {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DomHelper.class);
    private Document document;
    private NamespaceContext namespaceContext;

    /**
     * Creates a Document from the passed XML snippet
     *
     * @param input XML snippet
     *
     * @throws Exception if it can't parse the snippet
     */
    public DomHelper(InputSource input) throws Exception {

        // Create a document

        logger.debug("Parsing XML input stream");
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setValidating(false);
        docBuilderFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        document = docBuilder.parse(input);
    }

    /**
     * Creates a Document from the passed XML snippet
     *
     * @param input XML snippet
     *
     * @throws Exception if it can't parse the snippet
     */
    public DomHelper(InputStream input) throws Exception {
        this(new InputSource(input));
    }

    /**
     * Creates a Document from the passed XML snippet
     *
     * @param documentXml XML snippet
     *
     * @throws Exception if it can't parse the snippet
     */
    public DomHelper(String documentXml) throws Exception {
        this(new InputSource(new StringReader(documentXml)));
    }

    /**
     * Creates a Document from the passed XML snippet
     *
     * @param xmlFile File to read XML from
     *
     * @throws Exception if it can't parse the snippet
     */
    public DomHelper(File xmlFile) throws Exception {

        // Create a document

        logger.debug("Parsing XML file...");
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setValidating(false);
        docBuilderFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        InputStream in=null;
        try {
            in = new FileInputStream(xmlFile);
            document = docBuilder.parse(new InputSource(in));
        }
        finally {
            Common.close(in);
        }
    }

    /**
     * Returns the string value of the attribute of the element
     *
     * @param elementName Tag to get attributes for
     * @param attributeName Attribute to retrieve
     *
     * @return String value or null
     */
    public String getAttributeValue(String elementName, String attributeName) {
        String returnValue=null;
        List<Node> tmp=getNodes(elementName);
        if (!Common.isBlank(tmp)) {
            returnValue=((Element)tmp.get(0)).getAttribute(attributeName);
        }
        return returnValue;
    }

    /**
     * Returns the string value of the attribute of the element
     *
     * @param elementName Tag to get attributes for
     * @param attributeName Attribute to retrieve
     *
     * @return String value or null
     */
    public Date getAttributeDateValue(String elementName, String attributeName) {
        Date returnValue=null;
        try {
            DatatypeFactory dataFactory = DatatypeFactory.newInstance();
            String tmp=getAttributeValue(elementName, attributeName);
            if (!Common.isBlank(tmp)) {
                returnValue = dataFactory.newXMLGregorianCalendar(tmp).toGregorianCalendar().getTime();
            }
        }
        catch (Exception e) {
            logger.error("Cannot get date attribute value for [" + elementName + "] attribute [" + attributeName + "] - " + PivotalException.getErrorMessage(e));
        }
        return returnValue;
    }

    /**
     * Returns the integer value of the element attribute
     *
     * @param elementName Name of the element to get
     * @param attributeName Attribute to retrieve
     *
     * @return int value or 0 if it doesn't exist
     */
    public int getAttributeIntegerValue(String elementName, String attributeName) {
        String tmp=getAttributeValue(elementName, attributeName);
        if (!Common.isBlank(tmp))
            return Common.parseInt(tmp);
        else
            return 0;
    }

    /**
     * Returns the long value of the element attribute
     *
     * @param elementName Name of the element to get
     * @param attributeName Attribute to retrieve
     *
     * @return long value or 0 if it doesn't exist
     */
    public long getAttributeLongValue(String elementName, String attributeName) {
        String tmp=getAttributeValue(elementName, attributeName);
        if (!Common.isBlank(tmp))
            return Common.parseLong(tmp);
        else
            return 0;
    }

    /**
     * Returns the long value of the element attrinute
     *
     * @param elementName Name of the element to get
     * @param attributeName Attribute to retrieve
     *
     * @return long value or 0 if it doesn't exist
     */
    public double getAttributeDoubleValue(String elementName, String attributeName) {
        String tmp=getAttributeValue(elementName, attributeName);
        if (!Common.isBlank(tmp))
            return Common.parseDouble(tmp);
        else
            return 0;
    }

    /**
     * Retrieves the first element value as a string
     *
     * @param elementName Name of the tag
     *
     * @return String version of the value
     */
    public String getValue(String elementName) {
        String returnValue=null;
        try {
            List<String> tmp=getValues(elementName);
            if (!Common.isBlank(tmp)) {
                returnValue = tmp.get(0);
            }
        }
        catch (Exception e) {
            logger.error("Cannot get string node value for [" + elementName + "] - " + PivotalException.getErrorMessage(e));
        }
        return returnValue;
    }

    /**
     * Retrieves the first element value as a date
     *
     * @param elementName Name of the tag
     *
     * @return Date version of the value
     */
    public Date getDateValue(String elementName) {
        Date returnValue=null;
        try {
            DatatypeFactory dataFactory = DatatypeFactory.newInstance();
            String tmp=getValue(elementName);
            if (!Common.isBlank(tmp)) {
                returnValue = dataFactory.newXMLGregorianCalendar(tmp).toGregorianCalendar().getTime();
            }
        }
        catch (Exception e) {
            logger.error("Cannot get date node value for [" + elementName + "] - " + PivotalException.getErrorMessage(e));
        }
        return returnValue;
    }

    /**
     * Returns the integer value of the element
     *
     * @param elementName Name of the element to get
     *
     * @return int value or 0 if it doesn't exist
     */
    public int getIntegerValue(String elementName) {
        String tmp=getValue(elementName);
        if (!Common.isBlank(tmp))
            return Common.parseInt(tmp);
        else
            return 0;
    }

    /**
     * Returns the long value of the element
     *
     * @param elementName Name of the element to get
     *
     * @return long value or 0 if it doesn't exist
     */
    public long getLongValue(String elementName) {
        String tmp=getValue(elementName);
        if (!Common.isBlank(tmp))
            return Common.parseLong(tmp);
        else
            return 0;
    }

    /**
     * Returns the long value of the element
     *
     * @param elementName Name of the element to get
     *
     * @return long value or 0 if it doesn't exist
     */
    public double getDoubleValue(String elementName) {
        String tmp=getValue(elementName);
        if (!Common.isBlank(tmp))
            return Common.parseDouble(tmp);
        else
            return 0;
    }

    /**
     * Retrieves a list of element values for the given element name
     *
     * @param elementName Name of the tag
     *
     * @return List of Element string values
     */
    public List<String> getValues(String elementName) {
        List<String> returnValue=null;
        List<Node> tmp=getNodes(elementName);
        if (!Common.isBlank(tmp)) {
            returnValue=new ArrayList<>();
            for (Node node : tmp) {
                returnValue.add(node.getTextContent());
            }
        }
        return returnValue;
    }

    /**
     * Retrieves a list of nodes for the given element name
     *
     * @param elementName Name of the tag
     *
     * @return List of Node objects
     */
    public List<Node> getNodes(String elementName) {
        List<Node> returnValue=null;
        if (!Common.isBlank(elementName) && !elementName.startsWith("/")) elementName = "//" + elementName;
        try {
            NodeList nodes=findNodes(elementName);
            if (nodes!=null && nodes.getLength()>0) {
                returnValue=new ArrayList<>();
                for (int i=0; i<nodes.getLength(); i++) {
                    returnValue.add(nodes.item(i));
                }
            }
        }
        catch (Exception e) {
            logger.error("Cannot get date node values for [" + elementName + "] - " + PivotalException.getErrorMessage(e));
        }
        return returnValue;
    }

    /**
     * Retrieves a list of nodes for the given element name
     *
     * @param elementName Name of the tag
     *
     * @return List of Node objects
     */
    public List<Element> getElements(String elementName) {
        return getElements(elementName, null,false);
    }

    /**
     * Retrieves a list of nodes for the given element name
     * @param elementName Name of the tag
     * @param element Element to work from
     * @return List of Node objects
     */
    public List<Element> getElements(String elementName, Element element) {
        return getElements(elementName, element, false);
    }

    /**
     * Retrieves a list of nodes for the given element name
     *
     * @param elementName Name of the tag
     * @param onlyImmediate Return only the immediate children
     *
     * @return List of Node objects
     */
    public List<Element> getElements(String elementName, boolean onlyImmediate) {
        return getElements(elementName, null, onlyImmediate);
    }
    /**
     * Retrieves a list of nodes for the given element name
     *
     * @param elementName Name of the tag
     * @param element Element to work from
     * @param onlyImmediate Return only the immediate children
     *
     * @return List of Node objects
     */
    public List<Element> getElements(String elementName, Element element, boolean onlyImmediate) {
        List<Element> returnValue=null;
        try {
            NodeList nodes;
            if (element == null)
                nodes=document.getElementsByTagName(elementName);
            else
                nodes=element.getElementsByTagName(elementName);
            if (nodes!=null && nodes.getLength()>0) {
                returnValue=new ArrayList<>();
                for (int i=0; i<nodes.getLength(); i++) {
                    Element child = ((Element)nodes.item(i));
                    if(onlyImmediate && ((element ==null && child.getParentNode() == null) || !child.getParentNode().equals(element)))
                        continue;
                    returnValue.add(child);
                }
            }
        }
        catch (Exception e) {
            logger.error("Cannot get date element values for [" + elementName + "] - " + PivotalException.getErrorMessage(e));
        }
        return returnValue;
    }


    /**
     * Returns the text value of the immediate child text node
     *
     * @param node Node to start from
     * @return Text value or empty if nothing found
     */
    protected String getNodeValue( Node node ) {
        NodeList childNodes = node.getChildNodes();
        for (int x = 0; x < childNodes.getLength(); x++ ) {
            Node data = childNodes.item(x);
            if ( data.getNodeType() == Node.TEXT_NODE )
                return data.getNodeValue().trim();
        }
        return "";
    }

    protected String getNodeValue(String tagName, NodeList nodes ) {
        for ( int x = 0; x < nodes.getLength(); x++ ) {
            Node node = nodes.item(x);
            if (node.getNodeName().equalsIgnoreCase(tagName)) {
                NodeList childNodes = node.getChildNodes();
                for (int y = 0; y < childNodes.getLength(); y++ ) {
                    Node data = childNodes.item(y);
                    if ( data.getNodeType() == Node.TEXT_NODE )
                        return data.getNodeValue().trim();
                }
            }
        }
        return "";
    }



    /**
     * Returns a list with all the records present in the XML. Each node with the given name is processed as an individual record.
     * All other nodes are processed as common information to all records.
     * The column names follow a convention. Common nodes will be named S_X_Y_?... where X,Y, etc will be the "Coordinates" from the root node.
     * Records inside the the given node name (specific record information) be named N_X_Y_?... where X,Y, etc will be the relative "Coordinates" from the the given node (record node).
     * @param recordNodeName Node Name for records.
     * @return List of Maps. Each element in the list is a record.
     */
    public List<Map<String,String>> getRecordList(String recordNodeName) {
        logger.debug("Getting Record List");
        List<Map<String,String>> res = new ArrayList<>();

        //Common Nodes processor
        Map<String,String> common = processCommonNodes(recordNodeName);

        for (Element child : getElements(recordNodeName)) {
            //a child element to process
            try {
                Map<String, String> rec = processNodes(child, 1, new ArrayList<Integer>(), "N", null);
                rec.putAll(common);
                res.add(rec);
            } catch (Exception e) {//If one record fails it shouldn't stop processing. Log it and move on to the next one
                logger.error("Failed to process Node. Skipping" + e.getMessage(), e);
            }
        }

        logger.debug("Record List Complete. Result is " + res.toString());
        return res;
    }

    /**
     * Processes the nodes in recursion. The output will be a list with all the data under the given node.
     * @param currNd Current Node Name
     * @param level starting level
     * @param breadCrumb List to store the "coordinates" (bread crumb) of the current node.
     * @param prefix Prefix to use in the column name.
     * @param excludeKey Exclude node names from processing (Alpha Beta)
     * @return list with all the data under the given node excluding any descendants under the excluded node name.
     */
    private Map<String,String> processNodes(Node currNd , int level, ArrayList<Integer> breadCrumb , String prefix, String excludeKey) {

        Map<String,String> res = new HashMap<>();

        int tmp = currNd.getChildNodes().getLength();
        if(currNd.getNodeName().equals(excludeKey == null ? "" : excludeKey)){
            logger.debug("Skipping Node: " + excludeKey);
        }
        //if leaf node add to result. If it has only one node (the text) ahead or none at all (empty node), then it's leaf
        else if( tmp == 1 || tmp==0 && !Common.doStringsMatch(currNd.getNodeName(), "#text")) {
            logger.debug("Node " + " - " + level + " - " + breadCrumb + " - " + currNd.getNodeName() + " : " + getNodeValue(currNd));
            res.put(getNodeSpecialName(prefix,breadCrumb), getNodeValue(currNd));
        }
        else{
            NodeList nodes = currNd.getChildNodes();
            for(int i=0; i<nodes.getLength(); i++){
                Node node = nodes.item(i);

                if(node instanceof Element){
                    //a child element to process
                    Element child = (Element) node;
                    processBreadCrumb(breadCrumb,level);
                    res.putAll(processNodes(child,level+1,breadCrumb,prefix,excludeKey));
                }
            }
        }
        return res;
    }

    /**
     * Holds the logic for the bread crumb.
     * @param breadCrumb List of breadcrumb positions
     * @param level Level to add
     */
    private static void processBreadCrumb(ArrayList<Integer> breadCrumb, int level) {
        //Logic for breadcrum
        if(breadCrumb.size() < level){
            breadCrumb.add(1);
        }
        else{
            breadCrumb.set(level-1, breadCrumb.get(level-1)+1);
            //Clear old values
            while(breadCrumb.size() > level){
                breadCrumb.remove(breadCrumb.size()-1);
            }
        }
    }

    /**
     * Gets the proper name for node given a prefix and a bread crumb  list
     * @param prefix Prefix to prepend label with
     * @param breadCrum Depth of breadcrum to show
     * @return Label constructed
     */
    private String getNodeSpecialName(String prefix, List<Integer> breadCrum) {
        StringBuilder res = new StringBuilder(prefix);

        for (Integer aBreadCrum : breadCrum) {
            res.append('_');
            res.append(aBreadCrum.toString());
        }

        return res.toString();
    }

    /**
     * Builds a map with all the data not specific to the records.
     *
     * @param recordNodeName Node name that represents the record and that will be excluded from the result map
     *
     * @return Map of nodes
     */
    private Map<String,String> processCommonNodes(String recordNodeName) {
        logger.debug("Getting Common info Map");
        Map<String, String> res = new HashMap<>();

        Map<String,String> other = processNodes(document.getDocumentElement(),1,new ArrayList<Integer>(),"S",recordNodeName);
        res.putAll(other);

        logger.debug("Common info Map is " + res);
        return res;
    }


    /**
     * Save the XML document to the specified file
     *
     * @param outFile File to save to
     *
     * @throws TransformerException If there is a problem saving the file
     */
    public void save(String outFile) throws TransformerException {

        // Use a Transformer for output

        if (!Common.isBlank(outFile)) {
            save(new File(outFile));
        }
    }

    /**
     * Save the XML document to the specified file
     *
     * @param outFile File to save to
     *
     * @throws TransformerException If there is a problem saving the file
     */
    public void save(File outFile) throws TransformerException {

        // Use a Transformer for output

        if (outFile != null) {
            XmlCompressor comp=new XmlCompressor();
            comp.setRemoveComments(true);
            comp.setRemoveIntertagSpaces(true);
            Common.writeTextFile(outFile, comp.compress(getString()));
        }
    }

    /**
     * Save the XML document as a string
     *
     * @param omitXmlHeader True if the XML header should be omitted from the output
     *
     * @return Compressed version of the XML
     *
     * @throws TransformerException If there is a problem saving the file
     */
    public String save(boolean omitXmlHeader) throws TransformerException {

        // Use a Transformer for output

        XmlCompressor comp=new XmlCompressor();
        comp.setRemoveComments(true);
        comp.setRemoveIntertagSpaces(true);

        Writer output=new StringWriter();
        save(output, omitXmlHeader);
        return comp.compress(output.toString());
    }

    /**
     * Save the XML document to the specified writer
     *
     * @param output Destination to save to
     *
     * @throws TransformerException If there is a problem saving the file
     */
    public void save(Writer output) throws TransformerException {
        save(output, true);
    }

    /**
     * Save the XML document to the specified writer
     *
     * @param output Destination to save to\
     * @param omitXmlHeader True if the XML header should be omitted from the output
     *
     * @throws TransformerException If there is a problem saving the file
     */
    public void save(Writer output, boolean omitXmlHeader) throws TransformerException {
        save(output, omitXmlHeader, false);
    }

    /**
     * Save the XML document to the specified writer
     *
     * @param output Destination to save to
     * @param omitXmlHeader True if the XML header should be omitted from the output
     * @param  pretty True if the output should be pretty
     *
     * @throws TransformerException If there is a problem saving the file
     */
    public void save(Writer output, boolean omitXmlHeader, boolean pretty) throws TransformerException {

        // Use a Transformer for output

        if (output != null) {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, pretty?"yes":"no");
            if (pretty) {
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            }
            transformer.setOutputProperty(OutputKeys.MEDIA_TYPE, "text/xml");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXmlHeader?"yes":"no");
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(output);
            transformer.transform(source, result);
        }
    }

    /**
     * Returns the string value of the DOM
     *
     * @return String version of the document
     *
     * @throws TransformerException Exceptions
     */
    public String getString() throws TransformerException {
        Writer output=new StringWriter();
        save(output);
        return output.toString();
    }

    /**
     * Returns the string value of the DOM prettified with whitespace and
     * indentation
     *
     * @return String version of the document
     *
     * @throws TransformerException Exceptions
     */
    public String getPrettyString() throws TransformerException {
        Writer output=new StringWriter();
        save(output, true, true);
        return output.toString().replace("\r", "");
    }

    /**
     * Returns a nodelist using the xpath specified
     *
     * @param xpathQuery XPath query
     *
     * @return Nodelist or null if there is an issue with the query or document
     *
     * @throws XPathExpressionException If the XPath is badly formed
     */
    public NodeList findNodes(String xpathQuery) throws XPathExpressionException {
        return findNodes(xpathQuery, null, null);
    }

    /**
     * Returns a nodelist using the xpath specified
     *
     * @param xpathQuery XPath query
     *
     * @return Nodelist or null if there is an issue with the query or document
     *
     * @throws XPathExpressionException If the XPath is badly formed
     */
    public NodeList findNodes(String xpathQuery, NamespaceContext namespaceContext) throws XPathExpressionException {
        return findNodes(xpathQuery, null, namespaceContext);
    }

    /**
     * Returns a nodelist using the xpath specified
     *
     * @param xpathQuery XPath query
     * @param startNode Starting point, can be null
     *
     * @return Nodelist or null if there is an issue with the query or document
     *
     * @throws XPathExpressionException If the XPath is badly formed
     */
    public NodeList findNodes(String xpathQuery, Node startNode) throws XPathExpressionException {
        return findNodes(xpathQuery, startNode, null);
    }

    /**
     * Returns a nodelist using the xpath specified
     *
     * @param xpathQuery XPath query
     * @param startNode Starting point, can be null
     * @param namespaceContext Namespace to add, can be null
     *
     * @return Nodelist or null if there is an issue with the query or document
     *
     *
     * @throws XPathExpressionException If the XPath is badly formed
     */
    public NodeList findNodes(String xpathQuery, Node startNode, NamespaceContext namespaceContext) throws XPathExpressionException {
        if (Common.isBlank(xpathQuery))
            return null;
        else {
            XPath xPath= XPathFactory.newInstance().newXPath();

            // Set the name space context if we have one

            if (namespaceContext!=null)
                xPath.setNamespaceContext(namespaceContext);
            else if (this.namespaceContext!=null)
                xPath.setNamespaceContext(this.namespaceContext);

            // Potentially start from a known node

            if (startNode==null)
                return (NodeList) xPath.evaluate(xpathQuery, document, XPathConstants.NODESET);
            else
                return (NodeList) xPath.evaluate(xpathQuery, startNode, XPathConstants.NODESET);
        }
    }

    /**
     * Returns a node using the xpath specified
     *
     * @param xpathQuery XPath query
     *
     * @return Node or null if there is an issue with the query or document
     *
     * @throws XPathExpressionException If the XPath is badly formed
     */
    public Node findNode(String xpathQuery) throws XPathExpressionException {
        return findNode(xpathQuery, null, null);
    }

    /**
     * Returns a node using the xpath specified
     *
     * @param xpathQuery XPath query
     * @param startNode Starting point, can be null
     *
     * @return Node or null if there is an issue with the query or document
     *
     * @throws XPathExpressionException If the XPath is badly formed
     */
    public Node findNode(String xpathQuery, Node startNode) throws XPathExpressionException {
        return findNode(xpathQuery, startNode, null);
    }

    /**
     * Returns a node using the xpath specified
     *
     * @param xpathQuery XPath query
     * @param namespaceContext Namespace to use, can be null
     *
     * @return Node or null if there is an issue with the query or document
     *
     * @throws XPathExpressionException If the XPath is badly formed
     */
    public Node findNode(String xpathQuery, NamespaceContext namespaceContext) throws XPathExpressionException {
        return findNode(xpathQuery, null, namespaceContext);
    }

    /**
     * Returns a node using the xpath specified
     *
     * @param xpathQuery XPath query
     * @param startNode Starting point, can be null
     * @param namespaceContext Namespace to use, can be null
     *
     * @return Node or null if there is an issue with the query or document
     *
     * @throws XPathExpressionException If the XPath is badly formed
     */
    public Node findNode(String xpathQuery, Node startNode, NamespaceContext namespaceContext) throws XPathExpressionException {
        if (Common.isBlank(xpathQuery))
            return null;
        else {
            XPath xPath= XPathFactory.newInstance().newXPath();
            if (namespaceContext!=null) xPath.setNamespaceContext(namespaceContext);
            if (startNode==null)
                return (Node) xPath.evaluate(xpathQuery, document, XPathConstants.NODE);
            else
                return (Node) xPath.evaluate(xpathQuery, startNode, XPathConstants.NODE);
        }
    }

    /**
     * Returns the underlying document
     *
     * @return Document object
     */
    public Document getDocument() {
        return document;
    }


    /**
     * Validates th XML using the XSD file
     *
     * @param xsdFilename XSD filename to use
     * @param xmlString XML to validate
     *
     * @return XMLValidationResult result
     */
    public static XMLValidationResult validateXML(String xsdFilename, String xmlString) {

        XMLValidationResult returnValue = new XMLValidationResult();

        if (!Common.isBlank(xsdFilename)) {
            File xsdFile = new File(xsdFilename);

            // Clean up the service definition and take care of some obvious but otherwise
            // non-problematic errors

            String xmlText = xmlString.trim();

            // Get the schema and objects

            try {
                SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema schema = schemaFactory.newSchema(xsdFile);
                Validator validator = schema.newValidator();

                // Validate the XML against the schema

                validator.validate(new StreamSource(new StringReader(xmlText)));
            }
            catch (SAXParseException e) {
                returnValue.error = "Problem parsing XML, line=" + e.getLineNumber() + ", column=" + e.getColumnNumber() + "] - " + PivotalException.getErrorMessage(e);
                if (e.getSystemId() != null && e.getSystemId().matches("(?i).*xsd$") && e.getLineNumber() > 0) {
                    String xsd = Common.readTextFile(xsdFile);
                    returnValue.error += '\n' + xsd.split("\n")[e.getLineNumber() - 1];
                }
                else if (e.getLineNumber()>-1)
                    returnValue.error += '\n' + xmlText.split("\n")[e.getLineNumber() - 1];
                returnValue.error += "\n\n" + xmlText;
            }
            catch (Exception e) {
                returnValue.error = "Problem validating XML - " + PivotalException.getErrorMessage(e);
            }
        }

        return returnValue;
    }

    /**
     * Sets an optional search context
     * @param namespaceContext Namespace context to use
     */
    public void setNamespaceContext(NamespaceContext namespaceContext) {
        this.namespaceContext = namespaceContext;
    }
}


