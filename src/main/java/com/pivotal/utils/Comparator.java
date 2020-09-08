/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * Class used to compare complex objects and collections of results maps
 * Typically used for a grid - a Collection (List, Map etc) that contains
 * field value maps as their items e.g. Row maps from a BASIS Slave results set
 * When comparing fields that are numeric it assumes the values are
 * real (Double precision).  In the case of strings, the comparison is
 * done case-insensitive.
 *
 **/
public class Comparator implements java.util.Comparator<Object> {

    static final org.slf4j.Logger mobjLogger = org.slf4j.LoggerFactory.getLogger(Comparator.class);

    /** What sort of objects the comparator is used to sort */

    protected static enum ComparatorUsage {
        MAP, METHOD, STRING
    }

    protected ComparatorUsage type;
    protected Class sortClass;
    protected List<SortConfig> sortFields=new ArrayList<>();

    /**
     *
     * Class to hold the sorting characteristics
     *
     **/
    public static class SortConfig {
        public boolean Ascending=true;
        public Method MethodName;
        public boolean StringComparison=true;
        public String FieldName;
    }

    /**
     *
     * Creates a simple case-insensitive string comparator
     *
     **/
    public Comparator() {
        this(null, null, ComparatorUsage.STRING, false);
    }

    /**
     *
     * Creates a comparator for comparing using a method of the contained objects
     *
     * @param objClass The class to which the method relates
     * @param sMethod Name of the method to call to get comparison value
     *
     **/
    public Comparator(Class objClass, String sMethod) {
        this(objClass, sMethod, ComparatorUsage.METHOD, false);
    }

    /**
     *
     * Creates a comparator for comparing using a field of the contained map
     * It expects the comparable values to be strings
     *
     * @param sField Field to do the comparison on
     *
     **/
    public Comparator(String sField) {
        this(null, sField, ComparatorUsage.MAP, true);
    }

    /**
     *
     * Creates a comparator for comparing using a field of the contained map
     * It expects the values to strings
     *
     * @param sField Field to do the comparison on
     * @param bNumeric True if the comparison should be numeric
     *
     **/
    public Comparator(String sField, boolean bNumeric) {
        this(null, sField, ComparatorUsage.MAP, !bNumeric);
    }

    /**
     *
     * Creates a comparator for comparing both strings and numbers
     *
     * @param objClass The class to which the method relates
     * @param sField Field to do the comparison on
     * @param eType The type of obect to compare
     * @param bStringComparison True if the comparison should be String (only used for Maps)
     *
     **/
    public Comparator(Class<?> objClass, String sField, ComparatorUsage eType, boolean bStringComparison) {

        // Save the constituents

        type =eType;
        if (sField==null) {

            // Determine the type of sorting we are going to do

            SortConfig objTmp=new SortConfig();
            objTmp.StringComparison=true;
            sortFields.add(objTmp);
        }
        else {

            // Break the sort fields up

            String[] asFields=sField.split(" *, *");
            for (String sFieldName : asFields) {
                SortConfig objTmp=new SortConfig();
                objTmp.Ascending=!sFieldName.trim().matches("^.*(/| )desc(end)?$");
                objTmp.FieldName=sFieldName.replaceAll("(/| ).*","").trim();

                // Determine the type of sorting we are going to do

                if (eType==ComparatorUsage.MAP)
                    objTmp.StringComparison = bStringComparison;

                else if (objClass!=null) {
                    try {objTmp.MethodName=objClass.getMethod(sFieldName);}
                    catch (Exception e) {
                        mobjLogger.error("The sorting method name does not exist - " + sFieldName);
                    }
                }
                sortFields.add(objTmp);
            }
        }
    }

    /**
     *
     * Compares the two obects based on whether they are strings or numbers
     * Each object is in actual fact a Map of field values
     *
     * @param objRow1 - Map of fields
     * @param objRow2 - Map of fields
     *
     * @return int
     *
     **/
    public int compare(Object objRow1, Object objRow2) {

        // Loop round all the sort fields

        int iReturn=0;
        for (SortConfig objSort : sortFields) {
            if (iReturn==0) iReturn= compare(objRow1, objRow2, objSort);
        }
        return iReturn;
    }

    /**
     *
     * Compares the two obects based on whether they are strings or numbers
     * Each object is in actual fact a Map of field values or are objects
     * that exhibit the specified method
     *
     * @param objRow1 - Map of fields
     * @param objRow2 - Map of fields
     * @param objSort Sort characteristics
     *
     * @return int
     *
     **/
    private int compare(Object objRow1, Object objRow2, SortConfig objSort) {

        String sValue1,sValue2;
        Boolean bValue1,bValue2;
        int iReturn=0;

        // Determine the type objects that we are comparing
        // Simple field value maps

        try {
            if (type == ComparatorUsage.MAP) {
                sValue1=((Map)objRow1).get(objSort.FieldName).toString();
                sValue2=((Map)objRow2).get(objSort.FieldName).toString();

                // If this is a string comparison

                if (objSort.StringComparison &&
                    !((Map)objRow1).get(objSort.FieldName).getClass().getSuperclass().equals(Number.class) &&
                    !((Map)objRow2).get(objSort.FieldName).getClass().getSuperclass().equals(Number.class)) {
                    iReturn=objSort.Ascending?sValue1.compareToIgnoreCase(sValue2):sValue2.compareToIgnoreCase(sValue1);
                }

                // It is a numeric comparison so use doubles to do the work

                else {
                    Double rValue1=Double.parseDouble('0' + sValue1);
                    Double rValue2=Double.parseDouble('0' + sValue2);
                    if (objSort.Ascending)
                        iReturn=rValue1 < rValue2 ? -1 : rValue1.equals(rValue2) ? 0 : 1;
                    else
                        iReturn=rValue2 < rValue1 ? -1 : rValue2.equals(rValue1) ? 0 : 1;
                }
            }

            // More complex entity methods

            else if (type == ComparatorUsage.METHOD) {
                if (objSort.MethodName!=null) {
                    if (objSort.MethodName.getGenericReturnType().equals(String.class)) {
                        sValue1=(String)objSort.MethodName.invoke(objRow1);
                        sValue2=(String)objSort.MethodName.invoke(objRow2);
                        iReturn=objSort.Ascending?sValue1.compareToIgnoreCase(sValue2):sValue2.compareToIgnoreCase(sValue1);
                    }
                    else if (objSort.MethodName.getGenericReturnType().equals(boolean.class)) {
                        bValue1=(Boolean)objSort.MethodName.invoke(objRow1);
                        bValue2=(Boolean)objSort.MethodName.invoke(objRow2);
                        if (bValue1 && !bValue2)
                            iReturn=objSort.Ascending?1:-1;
                        else if (!bValue1 && bValue2)
                            iReturn=objSort.Ascending?-1:1;
                    }
                    else if (objSort.MethodName.getGenericReturnType().equals(long.class)) {
                        Long lValue1=(Long)objSort.MethodName.invoke(objRow1);
                        Long lValue2=(Long)objSort.MethodName.invoke(objRow2);
                        if (objSort.Ascending)
                            iReturn=lValue1 < lValue2 ? -1 : lValue1.equals(lValue2) ? 0 : 1;
                        else
                            iReturn=lValue2 < lValue1 ? -1 : lValue2.equals(lValue1) ? 0 : 1;
                    }
                    else if (objSort.MethodName.getGenericReturnType().equals(int.class)) {
                        Integer lValue1=(Integer)objSort.MethodName.invoke(objRow1);
                        Integer lValue2=(Integer)objSort.MethodName.invoke(objRow2);
                        if (objSort.Ascending)
                            iReturn=lValue1 < lValue2 ? -1 : lValue1.equals(lValue2) ? 0 : 1;
                        else
                            iReturn=lValue2 < lValue1 ? -1 : lValue2.equals(lValue1) ? 0 : 1;
                    }
                }
            }

            // Simple toString methods

            else if (type == ComparatorUsage.STRING) {
                return objRow1.toString().compareToIgnoreCase(objRow2.toString());
            }
        }
        catch (Exception e) {
            mobjLogger.debug("Problem comparing values - " + e.getMessage());
        }
        return iReturn;
    }

}
