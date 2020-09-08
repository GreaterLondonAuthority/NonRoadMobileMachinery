/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers.utils;

import com.pivotal.system.data.dao.DataSourceUtils;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class MappingManager {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MappingManager.class);

    /**
     * Prevent instantiation
     */
    private MappingManager() {
    }

    /**
     * Connects to the mapping database defined in the context
     *
     * @param status status object to return status in
     *
     * @return DataSource
     *
     */
    private static DataSource connectMappingDatabase(JsonResponse status) {

        DataSource returnValue = null;
        javax.naming.Context initCtx = null;
        if (status == null)
            status = new JsonResponse();

        try {
            initCtx = new InitialContext();
            returnValue = (DataSource) initCtx.lookup("java:/comp/env/jdbc/Mapping");
            logger.info("Loaded mapping database connection settings");
        }
        catch (Exception e) {
            status.setError("Error loading MAP datasource %s", PivotalException.getErrorMessage(e));
            logger.debug(status.getError());
        }
        finally {
            Common.close(initCtx);
        }

        return returnValue;
    }

    /**
     * Runs query against mapping data
     *
     * @param query qiery to be run
     *
     * @return the data
     */
    public static JsonResponse getMappingData(String query) {

        JsonResponse returnValue = new JsonResponse();

        // Build the query

        logger.debug("Building query");

        // Execute query

        try {
            DataSource mappingDatasource = connectMappingDatabase(returnValue);

            if (!returnValue.getInError() && mappingDatasource != null) {
                logger.debug("Running mapping query [{}]", query);
                ResultSet resultSet = DataSourceUtils.executeQuery(mappingDatasource.getConnection(), query);

                List data = new ArrayList<Map<String, String>>();
                while (!resultSet.isClosed() && resultSet.next()) {
                    // Loop through columns until error
                    boolean finished = false;
                    Map<String, String>dataRow = new HashMap<>();
                    int fieldIndex = 1;
                    while (!finished) {
                        try {
                            dataRow.put(resultSet.getMetaData().getColumnName(fieldIndex), resultSet.getString(fieldIndex));
                            fieldIndex +=1;
                        }
                        catch(Exception e) {
                            finished = true;
                        }
                    }
                    data.add(dataRow);
                }
                returnValue.getData().put("RecordSet",data);
            }
        }
        catch(Exception e) {
            returnValue.setError("Error running mapping query %s - %s", query, PivotalException.getErrorMessage(e));
            logger.error(returnValue.getError());
        }

        return returnValue;
    }

    /**
     * Returns a list of available KML files
     *
     */
    public static List<String> getMappingFileList() {

        return Common.splitToList(HibernateUtils.getMappingFileList(),",");

    }
}
