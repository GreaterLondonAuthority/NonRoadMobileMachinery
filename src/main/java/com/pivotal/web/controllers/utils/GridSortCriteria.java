/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pivotal.system.security.Preferences;
import com.pivotal.utils.Common;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pivotal.web.servlet.ServletHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * A class to make handling of the search criteria from a KendoUI grid easier
 */
public class GridSortCriteria {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GridSortCriteria.class);

    private static final String PREF_SORT = "sort-criteria";

    private static final String PARAM_CLEAR_SORT = "clearsort";
    private static final String PARAM_SORT_FIELD = "sort[%d][field]";
    private static final String PARAM_SORT_DIRECTION = "sort[%d][dir]";

    private Map<String, FieldSort> fields = new HashMap<>();
    private boolean dirty=false;
    private transient Preferences<Object> preferences;

    /**
     * Construct a filter clause from the request or the user preferences if a filter
     * is not defined on the querystring
     *
     * @param preferences Preferences object to use
     */
    public GridSortCriteria(Preferences<Object> preferences) {

        this.preferences = preferences;
        boolean clear = ServletHelper.parameterExists(PARAM_CLEAR_SORT);
        dirty = clear;

        // Check to see if we have something

        if (!clear) {
            if (ServletHelper.parameterExists(String.format(PARAM_SORT_FIELD, 0))) {

                // If we have something in the parameters then use it

                int i=0;
                dirty = true;
                String fieldName;
                while (ServletHelper.parameterExists(String.format(PARAM_SORT_FIELD, i))) {
                    fieldName = ServletHelper.getParameter(String.format(PARAM_SORT_FIELD, i)).replace('_','.');
                    fields.put(fieldName, new FieldSort(fieldName, ServletHelper.getParameter(String.format(PARAM_SORT_DIRECTION, i), "asc")));
                    i++;
                }
            }

            // See if we can get it from the preferences

            else {
                try {
                    // New way
                    Map<String, FieldSort> tmp = (Map<String, FieldSort>) preferences.get(PREF_SORT);
                    if (tmp != null) {
                        fields = tmp;
                    }
                }
                catch(Exception e) {

                    // Old way
                    // TODO : remove this once the users have all converted over to the new way ie gone into the screens. - after 1 June 2018?
                    GridSortCriteria tmp = (GridSortCriteria) preferences.get(PREF_SORT);
                    if (tmp!=null) {
                        fields=tmp.fields;
                    }
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder returnValue = new StringBuilder();
        for(FieldSort fieldSort : fields.values())
            returnValue.append(fieldSort.getField()).append(" ").append(fieldSort.getDirection());

        return returnValue.toString();
    }

    /**
     * Returns true if the sort clause needs saving
     *
     * @return True if data needs saving
     */
    @JsonIgnore
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Clears the criteria
     */
    public void clear() {
        if (!Common.isBlank(fields)) {
            fields.clear();
            dirty = true;
        }
    }

    /**
     * Returns a sort clause
     *
     * @return Sort clause
     */
    public String getSortClause() {
        StringBuilder returnValue = new StringBuilder();
        if (!Common.isBlank(fields)) {
            returnValue.append(" order by ");
            boolean firstTime=true;
            for(FieldSort fieldSort : fields.values()) {
                if (!firstTime) returnValue.append(",");
                firstTime=false;
                returnValue.append(fieldSort.getField().replace('_', '.')).append(" ").append(fieldSort.getDirection());
            }
        }
        return returnValue.toString();
    }

    /**
     * Returns true if the sort clause has some criteria
     *
     * @return True if criteria present
     */
    public boolean hasCriteria() {
        return fields.size()>0;
    }


    /**
     * Saves the values or clears them depending on what the command was from the browser
     * Sort settings are stored in the database
     */
    public void save() {
        if (isDirty()) {
            if (Common.isBlank(fields))
                preferences.remove(PREF_SORT);
            else
                preferences.put(PREF_SORT, fields);
        }
    }

    /**
     * Returns a list of field sort blocks
     *
     * @return Field sort objects
     */
    public Map<String, FieldSort> getFieldSort() {
        return fields;
    }

    /**
     * Sets the sort field to the specified field
     *
     * @param newField      Field to sort by
     * @param newDirection  Direction to sort
     *
     */
    public void setSortField(String newField, String newDirection) {
        fields.put(newField, new FieldSort(newField, newDirection));
        dirty = true;
    }

    /**
     * A container for a field sort block
     */
    public class FieldSort {
        private String field;
        private String direction;

        /**
         * Creates a sort block for the given field and direction
         * @param field Field this belongs to
         * @param direction Sort direction
         */
        public FieldSort(String field, String direction) {
            this.field = field;
            this.direction = direction;
        }

        /**
         * Name of the field
         * @return Field name
         */
        public String getField() {
            return field;
        }

        /**
         * Direction name
         * @return Name of the direction
         */
        public String getDirection() {
            return direction;
        }

        @Override
        public String toString() {
            return "field='" + field + '\'' +
                    ", direction='" + direction + '\'';
        }
    }
}
