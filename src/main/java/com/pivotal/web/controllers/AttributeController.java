/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers;

import com.pivotal.system.attributes.Attribute;
import com.pivotal.utils.Common;
import com.pivotal.utils.I18n;
import com.pivotal.web.controllers.utils.Authorise;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

/**
 * Provides access to the Attributes held within the system. These are typically alerts relating to Rooms, Sites and Transducers
 */
@Authorise(edit = {})
@Controller
@RequestMapping(value = "/attribute")
public class AttributeController extends AbstractController {
    // Standard logger
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AttributeController.class);

    /**
     * Basic constructor
     */
    public AttributeController() {
        super();
    }

    /**
     * Process the attributes supplied, turning them into a map of attributes ready for processing within the UI
     *
     * @param retval        The service response
     * @param entityName    The name of the entity (room name, site name, transducer name etc)
     * @param attributeList The list of attributes
     * @param showAll       Whether to include all attributes or only those with a severity rating
     */
    private void processAttributes(List<Map<String, Object>> retval, String entityName, Map<String, Attribute> attributeList, boolean showAll) {
        // General sanity check before looping over each item
        if (!Common.isBlank(attributeList) && !Common.isBlank(attributeList.values())) {
            for (Attribute attr : attributeList.values()) {
                // Only include those that have a severity
                if (showAll) {
                    // Construct the map for this item
                    Map<String, Object> thisAttr = new HashMap<>();
                    // Add in the entries
                    thisAttr.put("refId", attr.getReferenceId());
                    thisAttr.put("refType", attr.getReferenceType());
                    thisAttr.put("name", attr.getName());
                    thisAttr.put("datetime", attr.getDateTime());
                    thisAttr.put("time", I18n.diffDateWords(attr.getDateTime()));
                    thisAttr.put("description", String.format("%s : %s", entityName, attr.getDescription()));
                    // And add it to the list of attributes
                    retval.add(thisAttr);
                }
            }
        }
    }

    /**
     * Check the list of entity types (room / site / transducer) to see if there are any applicable alerts to display
     *
     * @param siteIdList       Comma separated list of site ids
     * @param roomIdList       Comma separated list of room ids
     * @param transducerIdList Comma separated list of transducer ids
     * @param showAll          Include attributes without severity indication or just those with severity
     * @return a list of processed attribute objects ready for display
     */
    @RequestMapping(value = "/alert", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Object getAlertInformation(@RequestParam(value = "sites", required = false, defaultValue = "") String siteIdList, @RequestParam(value = "rooms", required = false, defaultValue = "") String roomIdList, @RequestParam(value = "transducers", required = false, defaultValue = "") String transducerIdList, @RequestParam(value = "showall", required = false, defaultValue = "false") boolean showAll) {
        List<Map<String, Object>> retval = new ArrayList<>();
        // Process the data requested

        // And sort the information before responding
        Collections.sort(retval, new AttributeComparator());
        return retval;
    }

    /**
     * Comparator used to sort the attribute maps according to rules based on the name and the date time of the incident
     */
    public class AttributeComparator implements Comparator<Map<String, Object>> {
        @Override
        public int compare(Map<String, Object> o1, Map<String, Object> o2) {
            // Check for null / empty objects first
            if (Common.isBlank(o1) && Common.isBlank(o2)) {
                return 0;
            }
            if (Common.isBlank(o1)) {
                return 1;
            }
            if (Common.isBlank(o2)) {
                return -1;
            }
            // Now check the names of the objects
            String o1Name = (String) o1.get("name");
            String o2Name = (String) o2.get("name");
            if (Common.isBlank(o1Name) && Common.isBlank(o2Name)) {
                return 0;
            }
            if (Common.isBlank(o1Name)) {
                return 1;
            }
            if (Common.isBlank(o2Name)) {
                return -1;
            }
            int retval = o1Name.compareTo(o2Name);
            if (retval == 0) {
                // Same entity so check the date times
                Date o1DateTime = (Date) o1.get("datetime");
                Date o2DateTime = (Date) o2.get("datetime");
                if (!Common.isBlank(o1DateTime) || !Common.isBlank(o2DateTime)) {
                    if (Common.isBlank(o1DateTime)) {
                        retval = 1;
                    } else {
                        if (Common.isBlank(o2DateTime)) {
                            retval = -1;
                        } else {
                            // Reverse order for dates
                            retval = o2DateTime.compareTo(o1DateTime);
                        }
                    }
                }
            }
            return retval;
        }
    }
}
