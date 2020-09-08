/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.attributes;

import com.pivotal.utils.ClassUtils;
import com.pivotal.utils.Common;
import org.springframework.util.LinkedCaseInsensitiveMap;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is the manager of all the transient attributes associated with objects<p>
 * Attributes can be associated with anything that can be identified by a type, id and name.<p>
 * Each attribute is uniquely identified by these three values<p>
 * All attributes are entirely transitory and live in memory only
 */
public class AttributesManager implements Serializable {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AttributesManager.class);

    private static Map<String, Map<String, Attribute>> attributesCache = new ConcurrentHashMap<>();
    private static Map<String, Map<Integer, Map<String, Attribute>>> attributesByTypeCache = new ConcurrentHashMap<>();

    /**
     * Convenience method for getting the attribute value for the given identity
     * @param referenceType Type of the referenced object
     * @param referenceId ID of the referenced object
     * @param name Name of the attribute to retrieve
     * @return attribute object or null if not found
     */
    public static Attribute getAttribute(String referenceType, int referenceId, String name) {
        Attribute returnValue = null;
        if (Common.isBlank(referenceType)) {
            logger.error("Reference type is blank");
        }
        else if (Common.isBlank(name)) {
            logger.error("Name for type:{} and id:{} is blank", referenceType, referenceId);
        }
        else {
            Map<String, Attribute> list = attributesCache.get(getKey(referenceType, referenceId));
            if (!Common.isBlank(list)) {
                returnValue = list.get(name);
            }
        }
        return returnValue;
    }

    /**
     * Convenience method for getting a list of attributes for a given reference
     * @param referenceType Type of the referenced object
     * @param referenceId ID of the referenced object
     * @return List of attribute objects or null if none found
     */
    public static Map<String, Attribute> getAttributes(String referenceType, int referenceId) {
        Map<String, Attribute> returnValue = null;
        if (Common.isBlank(referenceType)) {
            logger.error("Reference type is blank");
        }
        else {
            Map<String, Attribute> tmp = attributesCache.get(getKey(referenceType, referenceId));
            if (!Common.isBlank(tmp)) {
                returnValue = new LinkedCaseInsensitiveMap<>();
                returnValue.putAll(tmp);
            }
        }
        return returnValue;
    }

    /**
     * Convenience method for getting the attributes associated with an instance
     * that supports an Id property
     * @param entityReference Object to look up
     * @return List of attribute objects or null if none found
     */
    public static Map<String, Attribute> getAttributes(Object entityReference) {
        Map<String, Attribute> returnValue = null;
        if (Common.isBlank(entityReference)) {
            logger.error("Reference is blank");
        }
        else {
            returnValue = getAttributes(getType(entityReference), getId(entityReference));
        }
        return returnValue;
    }

    /**
     * Convenience method for getting the attributes associated with a reference type
     * @param referenceType Type of the referenced object
     * @return List of attribute objects or null if none found
     */
    public static Map<Integer, Map<String, Attribute>> getAttributesByReferenceType(String referenceType) {
        Map<Integer, Map<String, Attribute>> returnValue = null;
        if (Common.isBlank(referenceType)) {
            logger.error("Reference is blank");
        }
        else {
            Map<Integer, Map<String, Attribute>> tmp = attributesByTypeCache.get(referenceType.toLowerCase());
            if (!Common.isBlank(tmp)) {
                returnValue = new HashMap<>();
                returnValue.putAll(tmp);
            }
        }
        return returnValue;
    }

    /**
     * Convenience method that gets the named attribute object for the given entity reference
     * @param entityReference Object to look up
     * @param name Name of the attribute to retrieve
     * @return attribute object or null if not found
     */
    public static Attribute getAttribute(Object entityReference, String name) {
        Attribute returnValue = null;
        if (Common.isBlank(entityReference)) {
            logger.error("Reference is blank");
        }
        else if (Common.isBlank(name)) {
            logger.error("Name for type:{} is blank", getType(entityReference));
        }
        else {
            Map<String, Attribute> list = attributesCache.get(getKey(entityReference));
            if (!Common.isBlank(list)) {
                returnValue = list.get(name);
            }
        }
        return returnValue;
    }

    /**
     * Adds or updates an existing attribute entity
     * @param entityReference Object to look up
     * @param name Name of the attribute to retrieve
     * @param dateTime Date time to use
     * @return New or update attribute entity
     */
    public static Attribute addAttribute(Object entityReference, String name, Date dateTime) {
        return addAttribute(entityReference, name, null, null, dateTime);
    }

    /**
     * Adds or updates an existing attribute entity
     * @param entityReference Object to look up
     * @param name Name of the attribute to retrieve
     * @return New or update attribute entity
     */
    public static Attribute addAttribute(Object entityReference, String name) {
        return addAttribute(entityReference, name, null, null, null);
    }

    /**
     * Adds or updates an existing attribute entity
     * @param entityReference Object to look up
     * @param name Name of the attribute to retrieve
     * @param description Description to add to attribute
     * @return New or update attribute entity
     */
    public static Attribute addAttribute(Object entityReference, String name, String description) {
        return addAttribute(entityReference, name, description, null, null);
    }

    /**
     * Adds or updates an existing attribute entity
     * @param entityReference Object to look up
     * @param name Name of the attribute to retrieve
     * @param description Description to add to attribute
     * @param dateTime Date time to use
     * @return New or update attribute entity
     */
    public static Attribute addAttribute(Object entityReference, String name, String description, Date dateTime) {
        return addAttribute(entityReference, name, description, null, dateTime);
    }

    /**
     * Adds or updates an existing attribute entity
     * @param entityReference Object to look up
     * @param name Name of the attribute to retrieve
     * @param description Description to add to attribute
     * @param value Value of attribute
     * @return New or update attribute entity
     */
    public static Attribute addAttribute(Object entityReference, String name, String description, Double value) {
        return addAttribute(entityReference, name, description, value, null);
    }

    /**
     * Adds or updates an existing attribute entity
     * @param entityReference Object to look up
     * @param name Name of the attribute to retrieve
     * @param description Description to add to attribute
     * @param value Value of attribute
     * @param dateTime Date time to use
     * @return New or update attribute entity
     */
    public static Attribute addAttribute(Object entityReference, String name, String description, Double value, Date dateTime) {
        Attribute returnValue = null;
        if (entityReference!=null) {
            returnValue = addAttribute(getType(entityReference), getId(entityReference), name, description, value, dateTime);
        }
        return returnValue;
    }

    /**
     * Adds or updates an existing attribute entity
     * @param entityReference Object to look up
     * @param name Name of the attribute to retrieve
     * @param value Value of attribute
     * @param dateTime Date time to use
     * @return New or update attribute entity
     */
    public static Attribute addAttribute(Object entityReference, String name, Double value, Date dateTime) {
        return addAttribute(entityReference, name, null, value, dateTime);
    }

    /**
     * Adds or updates an existing attribute entity
     * @param referenceType Type of the referenced object
     * @param referenceId ID of the referenced object
     * @param name Name of the attribute to retrieve
     * @param description Description to add to attribute
     * @param value Value of attribute
     * @param dateTime Date time to use
     * @return New or update attribute entity
     */
    synchronized public static Attribute addAttribute(String referenceType, int referenceId, String name, String description, Double value, Date dateTime) {
        Attribute returnValue = null;
        if (!Common.isBlank(referenceType) && !Common.isBlank(name)) {

            // Get the list of existing attributes for this unique object

            Map<String, Attribute> attributes = attributesCache.get(getKey(referenceType, referenceId));

            // If there are no attributes, then create them and add the list to the cache

            if (attributes==null) {
                attributes = new LinkedCaseInsensitiveMap<>();
                attributesCache.put(getKey(referenceType, referenceId), attributes);
            }

            // Get the list of existing IDs for this type

            Map<Integer, Map<String, Attribute>> attributeIds = attributesByTypeCache.get(referenceType.toLowerCase());

            // If there are no attributes, then create them and add the list to the cache

            if (attributeIds==null) {
                attributeIds = new HashMap<>();
                attributesByTypeCache.put(referenceType.toLowerCase(), attributeIds);
            }

            // Get the list of existing attributes for this ID

            Map<String, Attribute> attributesByType = attributeIds.get(referenceId);

            // If there are no attributes, then create them and add the list to the cache

            if (attributesByType==null) {
                attributeIds.put(referenceId, attributes);
            }

            // Get the existing attribute if we're updating it

            returnValue = attributes.get(name);
            if (returnValue==null) {
                returnValue = new Attribute();
                returnValue.setReferenceType(referenceType);
                returnValue.setReferenceId(referenceId);
                returnValue.setName(name);
                attributes.put(name, returnValue);
            }

            // Add on the changed stuff

            returnValue.setDescription(description);
            returnValue.setValue(value);
            returnValue.setDateTime(dateTime==null?new Date():dateTime);
        }
        return returnValue;
    }

    /**
     * Removes the attribute entity if it exists
     * @param entityReference Object to look up
     * @param name Name of the attribute to retrieve
     */
    public static void removeAttribute(Object entityReference, String name) {
        if (!Common.isBlank(entityReference)) {
            removeAttribute(getType(entityReference), getId(entityReference), name);
        }
    }

    /**
     * Removes the attribute entity if it exists
     * @param referenceType Type of the referenced object
     * @param referenceId ID of the referenced object
     * @param name Name of the attribute to retrieve
     */
    synchronized public static void removeAttribute(String referenceType, int referenceId, String name) {
        if (!Common.isBlank(referenceType) && !Common.isBlank(name)) {
            Map<String, Attribute> attributes = attributesCache.get(getKey(referenceType, referenceId));
            if (!Common.isBlank(attributes)) {
                attributes.remove(name);
            }
        }
    }

    /**
     * Returns true if the attribute exists
     * @param entityReference Object to look up
     * @param name Name of the attribute to retrieve
     * @return True if attribute exists
     */
    public static boolean attributeExists(Object entityReference, String name) {
        return getAttribute(entityReference, name)!=null;
    }

    /**
     * Returns true if the attribute exists
     * @param referenceType Type of the referenced object
     * @param referenceId ID of the referenced object
     * @param name Name of the attribute to retrieve
     * @return True if attribute exists
     */
    public static boolean exists(String referenceType, int referenceId, String name) {
        return getAttribute(referenceType, referenceId, name)!=null;
    }

    /**
     * Removes all attributes associated with the reference
     * @param referenceType Type of the referenced object
     * @param referenceId ID of the referenced object
     */
    synchronized public static void clear(String referenceType, int referenceId) {
        String key = getKey(referenceType, referenceId);
        if (key!=null) {
            attributesCache.remove(key);
            Map<Integer, Map<String, Attribute>> tmp = attributesByTypeCache.get(referenceType.toLowerCase());
            if (!Common.isBlank(tmp)) {
                tmp.remove(referenceId);
            }
        }
    }

    /**
     * Removes all attributes associated with the entity
     * that supports an Id property
     * @param entityReference Object to look up
     */
    public static void clear(Object entityReference) {
        if (entityReference!=null) {
            clear(getType(entityReference), getId(entityReference));
        }
    }

    /**
     * Constructs the key to the map of attribute names
     * @param entityReference Object to look up
     * @return Key as a unique string for this type and ID
     */
    private static String getKey(Object entityReference) {
        if (!Common.isBlank(entityReference)) {
            return getKey(getType(entityReference), getId(entityReference));
        }
        else {
            return null;
        }
    }

    /**
     * Constructs the key to the map of attribute names
     * @param referenceType Type of the referenced object
     * @param referenceId ID of the referenced object
     * @return Key as a unique string for this type and ID
     */
    private static String getKey(String referenceType, int referenceId) {
        if (!Common.isBlank(referenceType)) {
            return String.format("%s (id:%d", referenceType, referenceId).toLowerCase();
        }
        else {
            return null;
        }
    }

    /**
     * Safe way of getting the ID property
     * @param entityReference Object to use
     * @return The ID property value of -1 if unknown
     */
    private static int getId(Object entityReference) {
        if (entityReference==null) {
            return -1;
        }
        else {
            Object tmp = ClassUtils.getPropertyValue(entityReference, "Id");
            if (tmp != null) {
                return (int) tmp;
            }
            else {
                return -1;
            }
        }
    }

    /**
     * Safe way of getting the reference type from an object
     * @param entityReference Object to use
     * @return The reference type or a string "null"
     */
    private static String getType(Object entityReference) {
        if (entityReference==null) {
            return "null";
        }
        else {
            return entityReference.getClass().getSimpleName();
        }
    }

}
