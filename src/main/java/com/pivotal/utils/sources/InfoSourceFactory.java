/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils.sources;

import com.pivotal.utils.ClassUtils;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * Manages the creation of information sources
 */
public class InfoSourceFactory {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(InfoSourceFactory.class);

    /**
     * Prevent instantiation
     */
    private InfoSourceFactory() {}

    /**
     * Returns a suitable info source class to use
     * It uses reflection to determine which class to use by looking for all
     * classes in this package which look like the one to use
     *
     * @param infosourceEntity Associated scheduled task
     *
     * @return Returns a suitable information source
     */
    public static InfoSource getInfoSource(InfosourceEntity infosourceEntity) {

        InfoSource infosource =null;

        // Get a list of classes in this package

        try {
            Class<?> infosourceClass=null;

            // Find the class that has the correct name

            String target = infosourceEntity.getType() + "InfoSource";
            for (Class pubClass : ClassUtils.getClasses(InfoSource.class.getPackage().getName())) {
                if (Common.doStringsMatch(pubClass.getSimpleName(),target) &&
                    !pubClass.isInterface() &&
                    !Modifier.isAbstract(pubClass.getModifiers()) &&
                    (pubClass.getSuperclass().equals(InfoSource.class) || (pubClass.getSuperclass().getSuperclass()!= null && pubClass.getSuperclass().getSuperclass().equals(InfoSource.class)))) {
                    infosourceClass=pubClass;
                    logger.debug("Found cInfoSource class %s", infosourceClass.getSimpleName());
                    break;
                }
            }

            // If we've got a suitable class to use then construct it

            if (infosourceClass!=null) {
                Class[] paramTypes = {InfosourceEntity.class};
                Object[] paramValues = {infosourceEntity};
                Constructor objCon=infosourceClass.getConstructor(paramTypes);
                infosource = (InfoSource)objCon.newInstance(paramValues);
            }
        }
        catch (Exception e) {
            throw new PivotalException("Cannot enumerate the information source [" + infosourceEntity.getType() + ']', e);
        }

        if (infosource ==null)
            throw new PivotalException("Unknown information source type [" + infosourceEntity.getType() + ']');

        return infosource;
    }

}
