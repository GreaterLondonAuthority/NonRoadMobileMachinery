/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers.utils;

import com.pivotal.system.security.Preferences;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.ClassUtils;
import com.pivotal.utils.Common;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pivotal.utils.PivotalException;
import com.pivotal.web.servlet.ServletHelper;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.Query;

import java.util.*;

/**
 * Class to hold the list management properties
 */
public class GridResults {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GridResults.class);

    /** Constant <code>PARAM_CLEAR="clear"</code> */
    public static final String PARAM_CLEAR = "clear";
    /** Constant <code>PARAM_PAGE_SIZE="pageSize"</code> */
    public static final String PARAM_PAGE_SIZE = "pageSize";
    /** Constant <code>PARAM_PAGE="page"</code> */
    public static final String PARAM_PAGE = "page";
    /** Constant <code>PARAM_PAGE_CLASS_ID="classId"</code> */
    public static final String PARAM_PAGE_CLASS_ID = "classId";

    private Class entityClass;

    private GridFieldList fields;
    private int page = 1;
    private int pageSize = 100;
    private Preferences<Object> preferences;
    private int numberOfRows;

    private GridSortCriteria sortCriteria;
    private GridFilterCriteria filterCriteria;

    private boolean storePage =false;
    private boolean storePageSize =false;
    private boolean clearSettings=false;

    private List<Map<String, Object>> results;


    /**
     * Create all the properties using the current page
     *
     * @param entityClass Entity to model
     * @param preferences Preferences object to use
     * @throws Error if the entity cannot be interrogated
     * @throws java.lang.Exception if any.
     */
    public GridResults(Class entityClass, Preferences<Object> preferences) throws Exception {
        this(entityClass,preferences,null);
    }

    /**
     * Create all the properties using the current page
     *
     * @param entityClass Entity to model
     * @param preferences Preferences object to use
     * @param fixedCriteria a {@link java.lang.String} object.
     * @param entityId Id of entity
     *
     * @throws Error if the entity cannot be interrogated
     * @throws java.lang.Exception if any.
     */
    public GridResults(Class entityClass, Preferences<Object> preferences, String fixedCriteria, int entityId) throws Exception {
        this(entityClass, preferences, fixedCriteria);

        // Make sure we only keep the page number for the same entity
        // So that the page number is reset if we are showing a new entity
        String newEntityKey = entityClass.getName() + '_' + entityId;
        if (preferences.containsKey(GridResults.PARAM_PAGE_CLASS_ID)) {
            if (!newEntityKey.equals(preferences.get(GridResults.PARAM_PAGE_CLASS_ID))) {
                preferences.put(GridResults.PARAM_PAGE_CLASS_ID, newEntityKey);
                preferences.put(GridResults.PARAM_PAGE, 1);
                page = 1;
            }
        }
        else {
            preferences.put(GridResults.PARAM_PAGE_CLASS_ID, newEntityKey);
            page = 1;
        }
    }

    /**
     * Create all the properties using the current page
     *
     * @param entityClass Entity to model
     * @param preferences Preferences object to use
     * @param fixedCriteria a {@link java.lang.String} object.
     *
     * @throws Error if the entity cannot be interrogated
     * @throws java.lang.Exception if any.
     */
    public GridResults(Class entityClass, Preferences<Object> preferences, String fixedCriteria) throws Exception {

        // Check for stupidity

        if (entityClass==null)
            throw new PivotalException("Entity class is null");

        // Get a list of the default fields for the table if
        // none have been provided either in the preferences or on the URL

        this.entityClass = entityClass;
        this.preferences = preferences;

        // Determine what has been sent to us

        clearSettings = ServletHelper.parameterExists(PARAM_CLEAR);
        if (clearSettings) {
            fields = new GridFieldList(entityClass, null);
            pageSize = preferences.getDefaults().get(PARAM_PAGE_SIZE, pageSize);
        }
        else {
            storePage = ServletHelper.parameterExists(PARAM_PAGE);
            storePageSize = ServletHelper.parameterExists(PARAM_PAGE_SIZE);

            // Get the useful information from the query string and preferences

            page = ServletHelper.getParameter(PARAM_PAGE, preferences.get(PARAM_PAGE, page));
            pageSize = ServletHelper.getParameter(PARAM_PAGE_SIZE, preferences.get(PARAM_PAGE_SIZE, pageSize));

            fields = new GridFieldList(entityClass, preferences);
            filterCriteria = new GridFilterCriteria(entityClass, preferences,fixedCriteria);
            sortCriteria = new GridSortCriteria(preferences);

            // If we have been sent a sortClause or a search whereClause then reset the page

            if ((sortCriteria.isDirty() || filterCriteria.isDirty()) && !storePage) {
                page = 1;
                storePage = true;
            }
            if (page<1) page=1;
        }
    }

    /**
     * Constructs an HQL query using the values stored in the object
     *
     * @return HQL query
     */
    public String getQuery() {
        return getQuery(null);
    }

    /**
     * Constructs an HQL query using the values stored in the object
     *
     * @param extraFilter Extra filter to apply to the grid data
     *
     * @return HQL query
     */
    public String getQuery(String extraFilter) {

        String whereClause = filterCriteria.getWhereClause();

        if (!Common.isBlank(extraFilter))
            whereClause += (Common.isBlank(whereClause)?" where ":" and ") + extraFilter;

        return "from " + entityClass.getSimpleName() + whereClause + sortCriteria.getSortClause();
    }

    /**
     * Calculates the number of rows that would be returned for the given query
     *
     * @return Number of rows for the query
     */
    private int calculateRowCount() {
        String hql = "select count(*) from " + entityClass.getSimpleName() + filterCriteria.getWhereClause();

        Query query = HibernateUtils.createQuery(hql);
        filterCriteria.addCriteria(query);
        query.setCacheable(false);
        return ((Long)query.iterate().next()).intValue();
    }

    /**
     * Runs the query with all the parameters and sets the resulting list
     * This runs the query and then reduces the resulting object list into a list
     * of maps containing the results only for the specified fields
     */
    public void executeQuery() {
        executeQuery(null);
    }

    /**
     * Runs the query with all the parameters and sets the resulting list
     * This runs the query and then reduces the resulting object list into a list
     * of maps containing the results only for the specified fields
     *
     * @param extraFilter Extra filter to apply to query
     *
     */
    public void executeQuery(String extraFilter) {

        // Get the number of rows

        results = new ArrayList<>();
        numberOfRows = calculateRowCount();
        if (numberOfRows > 0 ) {

            // Construct the query and get the results - this is going to produce
            // a list of Entities

            Query query = HibernateUtils.createQuery(getQuery(extraFilter));
            if (page > 1) query.setFirstResult((page - 1) * pageSize);
            query.setMaxResults(pageSize);
            query.setCacheable(true);
            filterCriteria.addCriteria(query);
            @SuppressWarnings("unchecked")
            List<Object> localResults = query.list();

            // Copy the data into a list of maps of the data so that the JSON is
            // kept under some sort of control and the field names match
            // We're doing it this way rather than using an explicit Select so that
            // we don't invoke the dreaded "implicit inner join"

            // TODO The implicit join is still created for any sort that references
            // TODO a child entity property - need to manually create the queries
            // TODO so that we can manage the join ourselves but to be able to do this
            // TODO we are going to need to get involved in the Hibernate annotations
            // TODO so that we know how entities are related to each other

            if (localResults!=null) {
                int rowNumber=1;
                boolean hasInternalProperty = ClassUtils.propertyExists(entityClass, "internal");
                for (Object row : localResults) {
                    Map<String, Object> tmp = new LinkedHashMap<>();
                    boolean idInFieldList = false;
                    boolean internalInFieldList = false;
                    for (GridFieldList.FieldDescription field : fields.getFieldList()) {
                        Object value = field.getValue(row);

                        // Don't add null values - improves the JSON size

                        if (value!=null) {

                            // Check for Date variants (Date/Timestamps)

                            if (Date.class.isAssignableFrom(value.getClass()))
                                tmp.put(field.getKendoName(), Common.formatDate((Date)value, "yyyy-MM-dd HH:mm:ss"));
                            else
                                tmp.put(field.getKendoName(), value);
                        }

                        // Check if the user has included the ID and/or the Internal flag in the display

                        idInFieldList = field.getBaseName().equalsIgnoreCase("id");
                        internalInFieldList = field.getBaseName().equalsIgnoreCase("internal");
                    }

                    // Always add a row number, ID and internal flag - these are used
                    // by the grid to determine how to display the rows so must be present

                    tmp.put("rowNumber", rowNumber + ((page - 1) * pageSize));
                    if (!idInFieldList) {
                        tmp.put("id", ClassUtils.getPropertyValue(row, "id"));
                    }
                    if (!internalInFieldList && hasInternalProperty) {
                        tmp.put("internal", ClassUtils.getPropertyValue(row, "internal"));
                    }
                    else {
                        tmp.put("internal", false);
                    }
                    results.add(tmp);
                    rowNumber++;
                }
            }
        }
    }

    /**
     * Returns the number of rows found for the query once it has been executed
     *
     * @return Number of rows in the result - Null id execute hasn't been run
     */
    public int getTotalNumberOfRows() {
        return numberOfRows;
    }

    /**
     * Returns the number of pages found for the query once it has been executed
     *
     * @return Number of pages in the result - Null id execute hasn't been run
     */
    public int getTotalNumberOfPages() {
        return (numberOfRows / pageSize) + (numberOfRows % pageSize==0?0:1);
    }

    /**
     * Returns the page length used in the results
     *
     * @return Number of rows in a page
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Page to start at (1 based)
     *
     * @return The first page to display
     */
    public int getPage() {
        return page;
    }

    /**
     * Returns the current results (executeQuery) must have been called prior to this
     *
     * @return List of maps of searchField values
     */
    public List<Map<String, Object>> getData() {
        return results;
    }

    /**
     * Gets a list of useful properties that describe the fields
     *
     * @return Map of searchField information maps
     */
    @JsonIgnore
    public GridFieldList getFieldList() {
        return fields;
    }

    /**
     * Conditionally saves the settings into the preferences if they have changed
     */
    public void savePreferences() {
        if (clearSettings) {
            filterCriteria.clear();
            sortCriteria.clear();
            fields.clear();
            preferences.getSession().remove(PARAM_PAGE);
        }
        else {
            if (storePageSize) preferences.put(PARAM_PAGE_SIZE, pageSize);
            if (storePage) {
                if (page <1)
                    preferences.getSession().remove(PARAM_PAGE);
                else
                    preferences.getSession().put(PARAM_PAGE, page);
            }
        }
        filterCriteria.save();
        sortCriteria.save();
        fields.save();
    }

    /**
     * Returns the filter clause currently in use
     *
     * @return Filter clause object
     */
    public GridFilterCriteria getFilterClause() {
        return filterCriteria;
    }

    /**
     * Returns the sort clause currently in use
     *
     * @return Filter clause object
     */
    public GridSortCriteria getSortClause() {
        return sortCriteria;
    }
}
