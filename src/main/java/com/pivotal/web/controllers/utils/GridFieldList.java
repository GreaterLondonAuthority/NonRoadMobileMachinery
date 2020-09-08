/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers.utils;

import com.pivotal.system.hibernate.entities.*;
import com.pivotal.system.security.Preferences;
import com.pivotal.system.security.UserManager;
import com.pivotal.utils.Comparator;
import com.pivotal.utils.*;
import com.pivotal.web.Constants;
import com.pivotal.web.servlet.ServletHelper;
import org.springframework.util.LinkedCaseInsensitiveMap;

import javax.persistence.Transient;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

/**
 * A useful container for handling field lists used by the KendoUI grid
 * Columns definitions are expected to be a comma separated list of property
 * names and optionally a width in pixels e.g. "address:100,name:50,media:50:downloadimage"
 */
public class GridFieldList {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GridFieldList.class);
    private static final String PARAM_FIELDS = "fields";
    private static Map<String, Map<String, FieldDescription>> propertyCache = new LinkedCaseInsensitiveMap<>();

    private Class entityClass;
    private Map<String, FieldDescription> fieldList;
    private Preferences<Object> preferences;
    private boolean dirty;
    private Set<String> exclusions = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private String defaultFields;

    /**
     * Create a field list using the default values for this entity
     *
     * @param entityClass Entity to model
     * @param preferences Preferences object to use
     */
    public GridFieldList(Class entityClass, Preferences<Object> preferences) {
        this(entityClass, preferences, ServletHelper.getParameter(PARAM_FIELDS));
        dirty = ServletHelper.parameterExists(PARAM_FIELDS);
    }

    /**
     * Create a field list using the supplied fields
     *
     * @param entityClass Entity to model
     * @param preferences Preferences object to use
     * @param fields Field definitions to use
     */
    public GridFieldList(Class entityClass, Preferences<Object> preferences, String fields) {

        // Get the field list either from what has been passed or
        // from the preferences and if not there, the defaults in the
        // translation properties

        this.entityClass = entityClass;
        this.preferences = preferences;
        String excString = I18n.translate(preferences.getNamespace() + ".column.exclusions", true);
        if (excString!=null) exclusions.addAll(Common.splitToList(excString));
        defaultFields = I18n.translate(preferences.getNamespace() + ".column.defaults", true);
        if (fields==null) fields = preferences.get(PARAM_FIELDS, defaultFields);

        // If we don't have any fields at all, then fall back to using
        // all the properties of the entity

        if (Common.isBlank(fields))
            fieldList = getAllBeanFields(true);

        // Split the field list up into their constituent parts
        // The fields will be expressed in xxx:yy,xxx2:yyy2 etc

        else
            fieldList = convertFieldList(fields);
    }

    /**
     * Retrieves a default list of fields to use for the current entity
     * If a searchField is itself an Entity, then it's ID is returned
     *
     * @param onlyDisplayFields True if the list should only contain displayable columns
     * @return Comma separated list of property names
     */
    private Map<String, FieldDescription> getAllBeanFields(boolean onlyDisplayFields) {

        // Build the list of properties if we haven't got it from the cache

        Map<String, FieldDescription> tmp = propertyCache.get(entityClass.getName());
        if (tmp==null || Constants.inIde()) {
            tmp = new LinkedCaseInsensitiveMap<>();
            try {
                BeanInfo bean = Introspector.getBeanInfo(entityClass);
                for (PropertyDescriptor prop : bean.getPropertyDescriptors()) {
                    if (prop.getReadMethod()!=null && prop.getWriteMethod()!=null) {
                        tmp.put(prop.getName(), new FieldDescription(entityClass, prop, exclusions));
                    }
                }
                logger.debug("Default fields [{}]", Common.join(tmp.keySet()));
                propertyCache.put(entityClass.getName(), tmp);
            }
            catch (Exception e) {
                logger.error("Problem reading properties of [{}] - {}", entityClass.getSimpleName(), PivotalException.getErrorMessage(e));
            }
        }

        // Filter if required

        if (onlyDisplayFields) {
            for (FieldDescription field : new ArrayList<>(tmp.values())) {
                if (field.excluded)
                    tmp.remove(field.name);
            }
        }

        return tmp;
    }

    /**
     * Returns the fields as a list of descriptions
     *
     * @return List of field descriptions
     */
    public List<FieldDescription> getFieldList() {
        return new ArrayList<>(fieldList.values());
    }

    /**
     * Returns a field lst definition string in the form of
     * "field:width,fieldX:widthX"
     *
     * @return Field definition
     */
    public String getFields() {
        StringBuilder returnValue = new StringBuilder();
        if (fieldList !=null) {
            for (FieldDescription field : fieldList.values()) {
                returnValue.append((returnValue.length()>0) ? "," : "").append(field.name).append(":").append(field.width).append(Common.isBlank(field.extra)?"":":").append(Common.isBlank(field.extra) ? "" : field.extra);
            }
        }
        return returnValue.toString();
    }

    /**
     * Returns true if the field is present in the column list
     *
     * @param field Field name to check for
     * @return True if present
     */
    public boolean hasField(String field) {
        return field!=null && fieldList != null && fieldList.containsKey(field);
    }

    /**
     * Returns a List of field descriptions in an order that makes sense to Kendo
     * It's important that the fields are ordered with the visible ones first
     * (in their correct display order) followed by all the other available
     * fields
     *
     * @return List of field descriptions
     */
    public List<FieldDescription> getColumns() {

        // Start with the display columns

        List<FieldDescription> returnValue = new ArrayList<>(fieldList.values());
        Set<String> alreadyGot = new HashSet<>(fieldList.keySet());

        // Now do the same for all the hidden fields

        List<FieldDescription> tmp = new ArrayList<>();
        for (FieldDescription field : getAllBeanFields(true).values()) {
            if (!alreadyGot.contains(field.name)) {
                field.setHidden(true);
                tmp.add(field);
                alreadyGot.add(field.name);
            }
        }

        // Now again for any fields in the default list that we haven't got (this will
        // be the xxx.yyy variety)

        String fullDefaultFields = defaultFields;

        // Add extras that aren't in the standard list - maybe from child relations
        String extraFields = I18n.translate(preferences.getNamespace() + ".column.extras", true);
        if (!Common.isBlank(extraFields))
            fullDefaultFields += (Common.isBlank(fullDefaultFields)?"":",") + extraFields;

        if (!Common.isBlank(fullDefaultFields)) {
            Map<String, FieldDescription> allFields = getAllBeanFields(false);
            for (String column : Common.splitToList(fullDefaultFields)) {
                String columnName = Common.getItem(column, ":", 0);
                if (!alreadyGot.contains(columnName) && !allFields.containsKey(columnName)) {
                    try {
                        FieldDescription field = new FieldDescription(entityClass, column, exclusions);
                        field.setHidden(true);
                        tmp.add(field);
                        alreadyGot.add(field.name);
                    }
                    catch (Exception e) {
                        // Handled elsewhere
                    }
                }
            }
        }

        // Sort the hidden fields based on title

        Collections.sort(tmp, new Comparator(FieldDescription.class, "getTitle"));
        returnValue.addAll(tmp);


        // Make sure any extras that are specified in the default list are added to the fields

        if (!Common.isBlank(defaultFields) && returnValue.size() > 0) {

            // Build map of defaults that have extras

            Map<String, String> extraMap = new HashMap<>();
            for (String column : Common.splitToList(defaultFields))
                if (!Common.isBlank(Common.getItem(column, ":", 2)))
                    extraMap.put(Common.getItem(column, ":", 0), Common.getItem(column, ":", 2));

            if (extraMap.size() > 0) {
                for(FieldDescription field : returnValue) {
                    if (Common.isBlank(field.extra) && extraMap.containsKey(field.name)) {
                        field.extra = extraMap.get(field.name);
                    }
                }
            }
        }

        return returnValue;
    }

    /**
     * Clears the field lists
     */
    public void clear() {
        fieldList = new LinkedCaseInsensitiveMap<>();
        dirty = true;
    }

    /**
     * Save the field list into persistent storage if it has changed
     * Field settings are stored in the database
     */
    public void save() {
        if (dirty) {
            if (Common.isBlank(fieldList))
                preferences.remove(PARAM_FIELDS);
            else
                preferences.put(PARAM_FIELDS, getFields());
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getFields();
    }

    /**
     * Converts a comma separated list of field definitions into a Map
     * of field descriptions
     * @param fieldNames List of field names and widths
     * @return Map of field descriptions
     */
    private Map<String, FieldDescription> convertFieldList(String fieldNames) {
        Map<String, FieldDescription> fieldList = new LinkedCaseInsensitiveMap<>();
        for (String field : fieldNames.replaceAll("_",".").split(" *, *")) {
            try {
                FieldDescription desc = new FieldDescription(entityClass, field, exclusions);
                fieldList.put(desc.name, desc);
            }
            catch (Exception e) {
                logger.error("Cannot determine the properties of the field [{}.{}] - {}", entityClass.getSimpleName(), field, PivotalException.getErrorMessage(e));
            }
        }
        return fieldList;
    }

    /**
     * A container for a field description block
     */
    public static class FieldDescription {
        private Class entityClass;
        private Class parentClass;
        private String name;
        private String baseName;
        private Class clazz;
        private String kendoType;
        private int width;
        private boolean isSortable;
        private boolean excluded;
        private boolean hidden;
        private String title;
        private boolean isEntity;
        private boolean isMedia;
        private boolean isImage;
        private boolean isTransient;
        private String extra;

        /**
         * Creates a description block for the given field
         * @param entityClass Class that is the top parent of this property
         * @param field Field name and optionally width
         * @param exclusions Set of fields to exclude from display
         */
        private FieldDescription(Class entityClass, String field, Set<String> exclusions) throws Exception {
            if (Common.isBlank(field))
                throw new PivotalException("Cannot create property for an empty field name");

            name = Common.getItem(field, " *:", 0);
            width = Common.parseInt(Common.getItem(field, ": *", 1));
            extra = Common.getItem(field, ": *", 2);
            if (width<1) width = 100;

            // Get the properties

            init(entityClass, name, ClassUtils.getPropertyDescriptor(entityClass, name), exclusions);
        }

        /**
         * Creates a description block for the given field
         * @param prop Properties of the field
         * @param exclusions Set of fields to exclude from display
         */
        private FieldDescription(Class entityClass, PropertyDescriptor prop, Set<String> exclusions) {

            // Check for stupidity

            if (Common.isBlank(prop))
                throw new PivotalException("Cannot create property for an empty field property");

            // Save the properties

            width = 100;
            init(entityClass, prop.getName(), prop, exclusions);
        }

        /**
         * Initialises a description block for the given field
         * @param name Name of the field
         * @param prop Properties of the field
         * @param exclusions Set of fields to exclude from display
         */
        private void init(Class entityClass, String name, PropertyDescriptor prop, Set<String> exclusions) {

            // Sort the easy stuff

            this.entityClass = entityClass;
            hidden = false;
            baseName = prop.getName();
            this.name = name;
            parentClass = prop.getReadMethod().getDeclaringClass();
            clazz = prop.getPropertyType();
            excluded = exclusions!=null && exclusions.contains(name);
            title = I18n.translate(entityClass.getSimpleName().toLowerCase() + '.' + name);
            isTransient = prop.getReadMethod().getAnnotation(Transient.class)!=null;

            // Figure out the rest of it

            determineCharacteristics();
        }

        /**
         * Name of the field
         * @return Field name
         */
        public String getName() {
            return name;
        }

        /**
         * Name of the field in Kendo format (dots replaced with underscores)
         * @return Field name
         */
        public String getKendoName() {
            return name.replace('.', '_');
        }

        /**
         * Returns the translated title of the field
         * @return Translated title
         */
        public String getTitle() {
            return title;
        }

        /**
         * Return true if this column can be included in a sort
         *
         * @return True if a user can sort on this column
         */
        public boolean isSortable() {
            return isSortable;
        }

        /**
         * Return the Kendo column type for the given class type
         *
         * @return Kendo type
         */
        public String getType() {
            return kendoType;
        }

        /**
         * Returns the width of the column
         * @return Width allocated to the column
         */
        public int getWidth() {
            return width;
        }

        /**
         * Returns true if the column is excluded from the display
         * @return True if excluded
         */
        public boolean isExcluded() {
            return excluded;
        }

        /**
         * Returns true if the field is hidden
         * @return True if hidden
         */
        public boolean isHidden() {
            return hidden;
        }

        /**
         * Set the hidden status of the field
         * @param hidden Hide the field from the grid
         */
        public void setHidden(boolean hidden) {
            this.hidden = hidden;
        }

        /**
         * Returns true if this field refers to one of our entities
         * @return True if this field is of type xxxEntity
         */
        public boolean isEntity() {
            return isEntity;
        }

        /**
         * Returns the base name of the field
         * @return Base name of the field
         */
        public String getBaseName() {
            return baseName;
        }

        /**
         * Returns the class of the entity that sourced this field
         * @return Class of the source
         */
        public Class getEntityClass() {
            return entityClass;
        }

        /**
         * Returns the class of the entity that owns this field
         * @return Class of the owner
         */
        @SuppressWarnings("unused")
        public Class getParentClass() {
            return parentClass;
        }

        /**
         * Returns the class of the field
         * @return Class of the field
         */
        @SuppressWarnings("unused")
        public Class getClazz() {
            return clazz;
        }

        /**
         * Return true if this field is a media type
         * @return True if media
         */
        public boolean isMedia() {
            return isMedia;
        }

        /**
         * Returns true if the field is an image
         * @return True if it is an image
         */
        public boolean isImage() {
            return isImage;
        }

        /**
         * Returns the 'extra' part of the field definition i.e. name:width:extra
         * @return Extra portion of the field
         */
        public String getExtra() {
            return extra;
        }

        /**
         * Checks the type of the property and it's definition to get all the
         * ancilliary information
         */
        private void determineCharacteristics() {

            // Work out if the field is sortable

            isSortable = !isTransient && !"nosort".equalsIgnoreCase(extra) && (
                         clazz.equals(String.class) ||
                         clazz.equals(Integer.class) || clazz.equals(Long.class) ||
                         clazz.equals(Double.class) || clazz.equals(Short.class) ||
                         clazz.equals(int.class) || clazz.equals(long.class) ||
                         clazz.equals(double.class) || clazz.equals(short.class) ||
                         clazz.equals(Boolean.class) || clazz.equals(boolean.class) ||
                         clazz.equals(Date.class) || clazz.equals(Timestamp.class));

            // Now get the Kendo type

            if (clazz.equals(Integer.class) || clazz.equals(Long.class) ||
                clazz.equals(Double.class) || clazz.equals(Short.class) ||
                clazz.equals(int.class) || clazz.equals(long.class) ||
                clazz.equals(double.class) || clazz.equals(short.class)) {
                kendoType = "number";
            }
            else if (clazz.equals(BigDecimal.class)) {
                kendoType = "currency";
            }
            else if (clazz.equals(Boolean.class) || clazz.equals(boolean.class)) {
                kendoType = "boolean";
            }
            else if (clazz.equals(Date.class) || clazz.equals(Timestamp.class)) {
                kendoType = "date";
            }
            else if (clazz.equals(MediaEntity.class)) {
                kendoType = "media";
                isMedia = true;
            }
            else {
                kendoType = "string";
            }

            // Check to see if the field returns one of our entities

            isEntity = clazz.getSimpleName().endsWith("Entity");

        }

        @Override
        public String toString() {
            return "parentClass=" + parentClass +
                    ", name='" + name + '\'' +
                    ", clazz=" + clazz +
                    ", kendoType='" + kendoType + '\'' +
                    ", width=" + width +
                    ", isSortable=" + isSortable +
                    ", excluded=" + excluded +
                    ", hidden=" + hidden +
                    ", isEntity=" + isEntity +
                    ", title='" + title + '\'';
        }

        /**
         * Uses bean activation to go and get the value of this field
         * from the given entity
         *
         * @param entity The entity to interrogate
         *
         * @return Value object
         */
        public Object getValue(Object entity) {

            // Get the field value

            Object returnValue = ClassUtils.getPropertyValue(entity, name);

            // If this is a media item, then return the ID

            String regExp = "[a-z0-9_]{3,}\\.[a-z0-9_]{3,}+\\.[a-z0-9_]{3,}.+";
            if (returnValue!=null) {
                if (isMedia) {

                    // Check to see if this is an image

                    String extension = ClassUtils.getPropertyValue(returnValue, "extension");
                    isImage = Common.doStringsMatch(extension, "jpeg", "jpg", "png", "gif");

                    // If this isn't an image, negate the ID to indicate that we need to download it

                    returnValue = ClassUtils.getPropertyValue(returnValue, "id");
                    if (!isImage && returnValue!=null) returnValue = -(Integer)returnValue;
                }

                // Check for the special case where this is pointing at something interesting

                else if (Common.doStringsMatch("roles", name))
                    returnValue= UserManager.getRoleNames((String) returnValue, true);

                // Return the string value if this is an entity reference

                else if (isEntity)
                    returnValue=returnValue.toString();

                // Do any translation that might be needed

                else if (entity instanceof ReportTextEntity && "layout".equalsIgnoreCase(name) && ((String)returnValue).matches(regExp)) {
                    returnValue = I18n.translate((String) returnValue);
                }
                else if (!(entity instanceof LogEntity) && (returnValue instanceof String) && name!=null &&
                         name.matches("(?is)(.*(.+\\.)?[nN]ame)|((.+\\.)?description)|((.+\\.)?label)|((.+\\.)?category)|((.+\\.)?status)|((.+\\.)?severity)") &&
                        ((String)returnValue).matches(regExp)) {
                    returnValue = I18n.translate((String) returnValue);
                }
            }

            return returnValue;
        }
    }


}
