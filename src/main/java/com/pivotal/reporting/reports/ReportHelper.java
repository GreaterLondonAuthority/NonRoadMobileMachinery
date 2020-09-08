/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

package com.pivotal.reporting.reports;

import com.pivotal.monitoring.utils.Definition;
import com.pivotal.system.data.dao.DatabaseApp;
import com.pivotal.system.hibernate.entities.DatasourceEntity;
import com.pivotal.utils.Common;

/**
 * This class is used by reports as a convenient way of declaring parameters
 * It extends the DatabaseApp object which turns it into a full blown
 * SQL tool for updating querying etc.
 */
public class ReportHelper extends DatabaseApp {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ReportHelper.class);

    private Definition parameters=new Definition();

    /**
     * Constructs a report helper object that wraps a database
     */
    public ReportHelper() {
        super(null);
    }

    /**
     * Constructs a report helper object that wraps a database
     *
     * @param dataSrc NRMM entity
     */
    public ReportHelper(DatasourceEntity dataSrc) {
        super(dataSrc);
    }

    /**
     *
     * Creates a parameter object using the specified arguments
     *
     * @param sName Name of the parameter
     * @param sLabel Column header
     * @param sDescription Comment about it
     */
    public void declareParameter(String sName, String sLabel, String sDescription) {
        declareParameter(sName, sLabel, sDescription, null, true, "string");
    }

   /**
    *
    * Creates a parameter object using the specified arguments
    *
    * @param sName Name of the parameter
    * @param sLabel Column header
    * @param sDescription Comment about it
    * @param sDefaultValue Default value
    * @param bIsOptional True if the user must select from options
    */
   public void declareParameter(String sName, String sLabel, String sDescription, String sDefaultValue, boolean bIsOptional) {
       declareParameter(sName, sLabel, sDescription, sDefaultValue, bIsOptional, "string");
   }

   /**
    *
    * Creates a parameter object using the specified arguments
    *
    * @param sName Name of the parameter
    * @param sLabel Column header
    * @param sDescription Comment about it
    * @param sDefaultValue Default value
    * @param bIsOptional True if the user must select from options
    * @param displayType Type type of display to use for this parameter prompt
    */
   public void declareParameter(String sName, String sLabel, String sDescription, String sDefaultValue, boolean bIsOptional, String displayType) {
       if (Common.doStringsMatch(displayType, "drop_down"))
           parameters.addParameter(sName, sLabel, sDescription, displayType, null, !bIsOptional, sDefaultValue);
       else
           parameters.addParameter(sName, sLabel, sDescription, displayType, sDefaultValue, !bIsOptional);
   }


    /**
     * Returns the map of parameters declared within the report
     *
     * @return Map of parameters keyed on the name and ordered by insertion
     */
    public Definition getParameters() {
        return parameters;
    }

}
