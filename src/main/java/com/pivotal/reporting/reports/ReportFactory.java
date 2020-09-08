/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.reporting.reports;

import com.pivotal.system.hibernate.entities.ReportEntity;
import com.pivotal.utils.ClassUtils;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * Used to return an appropriate Report class for the given ReportEntity
 */
public class ReportFactory {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ReportFactory.class);
    private static Map<String, Class> classCache;

    /**
     * Factory class that looks at the content of the ReportEntity
     * and determines what type of report object to return
     *
     * @param report Entity describing the report type
     * @return Report class
     */
    public static Report getReport(ReportEntity report) {

        Report reportObject=null;

        // Get a list of classes in this package

        try {
            Class<?> reportClass=null;

            // Find the class that has the correct name

            String target = report.getType() + "Report";
            if (classCache==null)
                classCache = new HashMap<>();
            else
                reportClass = classCache.get(target);
            if (reportClass==null) {
                for (Class pubClass : ClassUtils.getClasses(Report.class.getPackage().getName())) {
                    if (Common.doStringsMatch(pubClass.getSimpleName(),target) && !pubClass.isInterface() && !Modifier.isAbstract(pubClass.getModifiers())) {
                        reportClass=pubClass;
                        break;
                    }
                }
                classCache.put(target, reportClass);
            }

            // If we've got a suitable class to use then construct it

            if (reportClass!=null) {
                Class[] paramTypes = {ReportEntity.class};
                Object[] paramValues = {report};
                Constructor objCon=reportClass.getConstructor(paramTypes);
                reportObject =(Report)objCon.newInstance(paramValues);
            }
        }
        catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof PivotalException)
                throw (PivotalException)e.getTargetException();
            else
                throw new PivotalException("Cannot enumerate the report classes [" + report.getType() + "] - %s", PivotalException.getErrorMessage(e));
        }
        catch (Exception e) {
            throw new PivotalException("Cannot enumerate the report classes [" + report.getType() + "] - %s", PivotalException.getErrorMessage(e));
        }

        if (reportObject ==null)
            throw new PivotalException("The report [" + report.getType() + "] is not supported by " + Common.getAplicationName());

        return reportObject;
    }

}
