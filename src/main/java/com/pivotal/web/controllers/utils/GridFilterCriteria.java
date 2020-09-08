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
import com.pivotal.utils.ClassUtils;
import com.pivotal.utils.Common;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pivotal.utils.PivotalException;
import com.pivotal.web.servlet.ServletHelper;
import org.hibernate.Query;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

/**
 * A class to make handling of the search criteria from a KendoUI grid easier
 */
public class GridFilterCriteria {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GridFilterCriteria.class);

    private static final String PREF_SEARCH = "search-criteria";
    private static final String PREF_SEARCH_FIELDS = "search-criteria-fields";
    private static final String PREF_SEARCH_CONNECTOR = "search-criteria-connectors";

    private static final String PARAM_CLEAR_SEARCH = "clearsearch";
    private static final String PARAM_SEARCH_FIELD = "filter[filters][%d][field]";
    private static final String PARAM_SEARCH_CRITERIA = "filter[filters][%d][value]";
    private static final String PARAM_SEARCH_OPERATOR = "filter[filters][%d][operator]";
    private static final String PARAM_SEARCH_CONNECTOR = "filter[logic]";

    private List<FieldCriteria> fields = new ArrayList<>();
    private String connector = "and";
    private String fixedFilter = null;
    private boolean dirty=false;
    private Class entityClass;
    private transient Preferences<Object> preferences;

    /**
     * Construct a filter clause from the request or the user preferences if a filter
     * is not defined on the querystring
     *
     * @param entityClass Entity to model
     * @param preferences Preferences object to use
     */
    public GridFilterCriteria(Class entityClass, Preferences<Object> preferences) {
        this(entityClass, preferences, null);
    }

    /**
     * Construct a filter clause from the request or the user preferences if a filter
     * is not defined on the querystring
     *
     * @param entityClass Entity to model
     * @param preferences Preferences object to use
     * @param fixedFilter Permanent filter condition to use
     */
    public GridFilterCriteria(Class entityClass, Preferences<Object> preferences, String fixedFilter) {
        this.fixedFilter = fixedFilter;
        this.entityClass = entityClass;
        this.preferences = preferences;
        boolean clear = ServletHelper.parameterExists(PARAM_CLEAR_SEARCH);
        dirty = clear;

        // Check to see if we have something

        if (!clear) {
            if (ServletHelper.parameterExists(String.format(PARAM_SEARCH_FIELD, 0)) &&
                ServletHelper.parameterExists(String.format(PARAM_SEARCH_CRITERIA, 0))) {

                // If we have something in the parameters then use it

                int i=0;
                dirty = true;
                while (ServletHelper.parameterExists(String.format(PARAM_SEARCH_FIELD, i)) &&
                       ServletHelper.parameterExists(String.format(PARAM_SEARCH_CRITERIA, i))) {
                    fields.add(new FieldCriteria(ServletHelper.getParameter(String.format(PARAM_SEARCH_FIELD, i)),
                                                 ServletHelper.getParameter(String.format(PARAM_SEARCH_OPERATOR, i), "="),
                                                 ServletHelper.getParameter(String.format(PARAM_SEARCH_CRITERIA, i))));

                    i++;
                }
                connector = ServletHelper.getParameter(PARAM_SEARCH_CONNECTOR, "and");
            }

            // See if we can get it from the preferences

            else {

                // SS-835 this will handle the situation when user had only one col filter and clicked clear filter on col .In that situation
                // filter become empty string  then preferences should not applied.
                // preferences filter only applied when page refreshes at the time filter Parameter is null

                if (ServletHelper.getParameter("filter") == null) {
                    try {
                        Map<String, Object> tmp = (Map<String, Object>)preferences.getSession().get(PREF_SEARCH);
                        if (tmp != null) {
                            fields = (List<FieldCriteria>)tmp.get(PREF_SEARCH_FIELDS);
                            connector = (String)tmp.get(PREF_SEARCH_CONNECTOR);
                        }
                    }
                    catch(Exception e) {
                        logger.error("Error getting GridFilterCriteria {}", PivotalException.getErrorMessage(e));
                    }
                }
                else{
                    if (Common.doStringsMatch(ServletHelper.getParameter("filter"), "")) {
                        Object tmp = preferences.getSession().get(PREF_SEARCH);
                        if (tmp != null) {
                            preferences.getSession().remove(PREF_SEARCH);
                        }
                    }
                }
            }
        }
    }

    /**
     * This is for Jackson to pick up the nicely formatted version of the criteria
     *
     * @return Nicely formatted version of the search criteria
     */
    public String getDisplayString() {
        return toString();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder returnValue = new StringBuilder();
        //Add the fixed filter condition
        if(!Common.isBlank(fixedFilter)){
            returnValue.append("( ").append(fixedFilter).append(" )");

            // If there is more to come, add the and/ or

            if (!Common.isBlank(fields)) {
                returnValue.append(" ").append(connector).append(" ");
            }
        }
        for (FieldCriteria field : fields) {
            if (returnValue.length()>0) returnValue.append(connector).append(" ");
            returnValue.append(field.field).append(" ").append(field.operator).append(" ").append(field.criteria).append(" ");
        }

        return returnValue.toString();
    }

    /**
     * Returns true if the search clause needs saving
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
            connector = "and";
            dirty = true;
        }
    }

    /**
     * Returns a where clause with placeholders for the criteria
     *
     * @return Where clause
     */
    public String getWhereClause() {
        StringBuilder returnValue = new StringBuilder();
        StringBuilder whereClause = new StringBuilder();
        whereClause.append(" where ");

        // Add the fixed filter condition

        if (!Common.isBlank(fixedFilter)){
            whereClause.append("( ");
            whereClause.append(fixedFilter);
            whereClause.append(" )");

            // If there is more to come, add the AND

            if (!Common.isBlank(fields)) {
                whereClause.append(" ").append(connector).append(" ");
            }
        }
        if (!Common.isBlank(fields)) {
            PropertyDescriptor prop;
            String lowerPrefix;
            String lowerSuffix;

            for (FieldCriteria field : fields) {
                if (returnValue.length()>0) returnValue.append(connector).append(" ");
                prop = ClassUtils.getPropertyDescriptor(entityClass, field.field);
                if (prop.getPropertyType().equals(String.class)) {
                    lowerPrefix = "lower(";
                    lowerSuffix = ")";
                }
                else {
                    lowerPrefix = "";
                    lowerSuffix = "";
                }

                returnValue.append(lowerPrefix).append(field.field).append(lowerSuffix).append(" ").append(lookupOperator(field.operator)).append(" ? ");
            }
        }

        // If we have a fixed filter or a some filtered fields, insert the where clause

        if (!Common.isBlank(fixedFilter) || !Common.isBlank(fields)) {
            returnValue.insert(0, whereClause.toString());
        }
        return returnValue.toString();
    }

    /**
     * Returns true if the where clause has some criteria
     *
     * @return True if criteria present
     */
    public boolean hasCriteria() {
        return fields.size()>0;
    }

    /**
     * Add criteria to the query object
     *
     * @param query Query object to update
     * @return Query object updated
     */
    public Query addCriteria(Query query) {

        int i=0;
        if (!Common.isBlank(fields)) {
            for (FieldCriteria field : fields) {

                // Add the value as the correct object type

                PropertyDescriptor prop = ClassUtils.getPropertyDescriptor(entityClass, field.field);
                if (prop==null || prop.getPropertyType().equals(String.class)) {
                    query.setParameter(i, adjustCriteria(field.operator,field.criteria.toLowerCase()).replace('*', '%'));
                }
                else {
                    Class propType = prop.getPropertyType();
                    if (propType.equals(Integer.class) || propType.equals(int.class)) {
                        query.setParameter(i, Common.parseInt(field.criteria));
                    }
                    else if (propType.equals(Long.class) || propType.equals(long.class)) {
                        query.setParameter(i, Common.parseLong(field.criteria));
                    }
                    else if (propType.equals(Double.class) || propType.equals(double.class)) {
                        query.setParameter(i, Common.parseDouble(field.criteria));
                    }
                    else if (propType.equals(Short.class) || propType.equals(short.class)) {
                        query.setParameter(i, Common.parseShort(field.criteria));
                    }
                    else if (propType.equals(Boolean.class) || propType.equals(boolean.class)) {
                        query.setParameter(i, Common.parseBoolean(field.criteria));
                    }
                    else if (propType.equals(Date.class)) {
                        query.setParameter(i, Common.parseDate(field.criteria));
                    }
                    else if (propType.equals(BigDecimal.class)) {
                        query.setParameter(i, new BigDecimal(field.criteria));
                    }
                    else {
                        logger.warn("Using default class conversion for [{}] type [{}]", prop.getName(), propType.getSimpleName());
                        query.setParameter(i, field.criteria);
                    }
                }
                i++;
            }
        }
        return query;
    }

    /**
     * Saves the values or clears them depending on what the command was from the browser
     * Filter criteria are stored in the Session
     */
    public void save() {
        if (isDirty()) {
            if (Common.isBlank(fields))
                preferences.getSession().remove(PREF_SEARCH);
            else {
                Map<String, Object>settings = new HashMap<>();
                settings.put(PREF_SEARCH_FIELDS, fields);
                settings.put(PREF_SEARCH_CONNECTOR, connector);
                preferences.getSession().put(PREF_SEARCH, settings);
            }
        }
    }

    /**
     * Returns the non-Kendo version of the filter operator
     *
     * @param operator Kendo operator
     * @return HQL equivalent
     */
    private String lookupOperator(String operator) {
        if (Common.doStringsMatch(operator, "eq"))
            return "=";
        else if (Common.doStringsMatch(operator, "neq"))
            return "<>";
        else if (Common.doStringsMatch(operator, "lt"))
            return "<";
        else if (Common.doStringsMatch(operator, "lte"))
            return "<=";
        else if (Common.doStringsMatch(operator, "gt"))
            return ">";
        else if (Common.doStringsMatch(operator, "gte"))
            return ">=";
        else if (Common.doStringsMatch(operator, "startswith", "contains", "endswith"))
            return "like";
        else if (Common.doStringsMatch(operator, "doesnotcontain"))
            return "not like";
        else
            return operator;
    }

    /**
     * Returns the non-Kendo version of the criteria
     *
     * @param operator Kendo operator
     * @param criteria Criteria to adjust
     * @return HQL equivalent
     */
    private String adjustCriteria(String operator, String criteria) {
        if (Common.doStringsMatch(operator, "startswith"))
            return criteria + '%';
        else if (Common.doStringsMatch(operator, "contains", "doesnotcontain"))
            return '%' + criteria + '%';
        else if (Common.doStringsMatch(operator, "endswith"))
            return '%' + criteria;
        else
            return criteria;
    }

    /**
     * Returns the list of fields in the clause
     *
     * @return List of filter fields
     */
    public List<FieldCriteria> getFieldCriteria() {
        return fields;
    }

    /**
     * Returns the connector used in the query
     *
     * @return Connector e.g. AND
     */
    public String getConnector() {
        return connector;
    }

    /**
     * A container for a field criteria block
     */
    public class FieldCriteria implements Serializable {
        private static final long serialVersionUID = -579730154108691287L;
        private String field;
        private String operator;
        private String criteria;

        /**
         * Creates a criteria block for the given field, operator and criteria
         * @param field Field this belongs to
         * @param operator Operator to use
         * @param criteria Search criteria
         */
        public FieldCriteria(String field, String operator, String criteria) {
            this.field = field==null?null:field.replace('_', '.');
            this.operator = operator;
            this.criteria = criteria;
        }

        /**
         * Name of the field
         * @return Field name
         */
        public String getField() {
            return field;
        }

        /**
         * Operator name
         * @return Name of the operator
         */
        public String getOperator() {
            return operator;
        }

        /**
         * Search criteria for the field
         * @return Criteria
         */
        public String getCriteria() {
            return criteria;
        }

        @Override
        public String toString() {
            return "field='" + field + '\'' +
                    ", operator='" + operator + '\'' +
                    ", criteria='" + criteria + '\'';
        }
    }
}
