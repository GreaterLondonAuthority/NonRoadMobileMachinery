/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

package com.pivotal.system.monitoring.jmx;

import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;
import com.pivotal.web.servlet.ServletHelper;

import javax.management.ObjectName;
import java.util.Hashtable;

/**
 * Provides some general purpose NRMM JMX utilities
 */
public class JMXUtils {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JMXUtils.class);

    /**
     * Prevent instantiation
     */
    private JMXUtils() {
    }

    /**
     * Creates a suitable NRMM JMX entry name
     *
     * @param type Name of the top level container
     * @return Object name
     */
    public static ObjectName getObjectName(String type) {
        return getObjectName(type, null);
    }

    /**
     * Creates a suitable NRMM JMX entry name
     *
     * @param type Name of the top level container
     * @param subType Secondary container name
     * @return Object name
     */
    public static ObjectName getObjectName(String type, String subType) {
        ObjectName name = null;
        Hashtable<String,String> properties = new Hashtable<>();
        if (Common.isBlank(subType)) {
            properties.put("type", "General");
            properties.put("item", type);
        }
        else {
            properties.put("type", type);
            properties.put("item", subType);
        }
        try {
            name = new ObjectName(String.format("%s-%s", Common.getAplicationName(), ServletHelper.getAppIdentity()), properties);
        }
        catch (Exception e) {
            logger.error("Problem constructing JMX object {}", PivotalException.getErrorMessage(e));
        }
        return name;
    }

}
