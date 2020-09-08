/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.reporting.publishing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.pivotal.utils.Common.isBlank;

/**
 * Holder for general purpose recipient names
 * In the case of an email address, the name will be the full
 * address e.g. pwallace@pivotal-solutions.co.uk and descriptiveName if it exists, will
 * be Steve O'Hara
 * In the case of VFS recipients the name will be the full URI of the
 * destination file e.g. ftp://pivotal-solutions.co.uk/something.pdf and the descriptiveName
 * will be null
 */
public class Recipient implements java.io.Serializable {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Recipient.class);

    private String name;
    private String descriptiveName;
    private LinkedHashMap<String,String> values;

    /**
     * Constructs a recipient object
     *
     * @param name Full name of the recipient e.g. pwallace@pivotal-solutions.co.uk
     */
    protected Recipient(String name) {
        this(name, null);
    }

    /**
     * Constructs a recipient object
     *
     * @param name Full name of the recipient e.g. pwallace@pivotal-solutions.co.uk
     * @param descriptiveName Descriptive name of the recipient e.g. paul wallace
     */
    public Recipient(String name, String descriptiveName) {
        setNames(name, descriptiveName);
        values = new LinkedHashMap<>();
        values.put("name",this.name);
        values.put("description",this.descriptiveName);
    }

    /**
     * Constructs a recipient object
     *
     * @param name Full name of the recipient e.g. pwallace@pivotal-solutions.co.uk
     * @param descriptiveName Descriptive name of the recipient e.g. paul wallace
     */
    private void setNames(String name, String descriptiveName) {
        if (!isBlank(name) && name.contains("<") && isBlank(descriptiveName)) {
            this.name = name.split("[<>]")[1].trim();
            this.descriptiveName = name.split("<")[0].trim();
        }
        else {
            this.name = isBlank(name) ? "????" : name;
            this.descriptiveName = isBlank(descriptiveName) ? name : descriptiveName;
        }
    }

    /**
     * Constructs a recipient object
     *
     * @param name Full name of the recipient e.g. pwallace@pivotal-solutions.co.uk
     * @param descriptiveName Descriptive name of the recipient e.g. Paul Wallace
     * @param values Map of ancillary values from table
     */
    protected Recipient(String name, String descriptiveName, LinkedHashMap<String, String> values) {
        this(name, descriptiveName);
        if (!isBlank(values))
            this.values.putAll(values);
    }

    /**
     * Constructs a recipient using the returned values from a map
     * The address of the person is expected to be the first value in the
     * map and the name is the second - in the case of email addresses, the
     * xxx@fddsdfd.com is the first value and "Steve O'Hara" is the second
     *
     * @param values Map of column values
     */
    public Recipient(LinkedHashMap<String, String> values) {
        this.values = values;
        if (!isBlank(values)) {
            List<String> list=new ArrayList<>(values.values());
            String name=list.get(0).replaceAll("[,;\n\r\t ]+$","");
            String descriptiveName;
            if (list.size()>1)
                descriptiveName=list.get(1);
            else
                descriptiveName = null;
            setNames(name, descriptiveName);
        }
    }

    /**
     * Returns the full name of the recipient e.g. pwallace@pivotal-solutions.co.uk
     *
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the descriptive name of the recipient e.g. Steve O'Hara
     * or null if it doesn't exist
     *
     * @return String
     */
    public String getDescriptiveName() {
        return descriptiveName;
    }

    public String getDescription() {
        return this.descriptiveName + " <" + this.name + '>';
    }

    /**
     * Returns the map of ancillary column values for this recipient
     *
     * @return Map
     */
    public Map<String, String> getValues() {
        return values;
    }
}
