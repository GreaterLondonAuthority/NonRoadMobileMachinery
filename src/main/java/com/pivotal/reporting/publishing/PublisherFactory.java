/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.reporting.publishing;

import com.pivotal.system.hibernate.entities.DistributionListEntity;
import com.pivotal.system.hibernate.entities.ScheduledTaskEntity;
import com.pivotal.utils.ClassUtils;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * Determines how a file should be published
 */
public class PublisherFactory {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PublisherFactory.class);

    /**
     * Prevent instantiation
     */
    private PublisherFactory() {}

    /**
     * Returns a suitable publisher channel class to use
     * It uses reflection to determine which class to use by looking for all
     * classes in this package which look like the one to use
     *
     * @param task Associated scheduled task
     * @param list Distribution list
     * @return Returns a suitable publisher channel
     */
    public static Publisher getPublisher(ScheduledTaskEntity task, DistributionListEntity list) {

        Publisher publisher=null;

        // Get a list of classes in this package

        Class<?> publishingClass=null;
        try {

            // Find the class that has the correct name

            String target = list.getType() + "Publisher";
            for (Class pubClass : ClassUtils.getClasses(Publisher.class.getPackage().getName())) {
                if (Common.doStringsMatch(pubClass.getSimpleName(),target) &&
                    !pubClass.isInterface() &&
                    !Modifier.isAbstract(pubClass.getModifiers()) &&
                    (pubClass.getSuperclass().equals(Publisher.class) || pubClass.getSuperclass().getSuperclass()!= null && pubClass.getSuperclass().getSuperclass().equals(Publisher.class))) {
                    publishingClass=pubClass;
                    break;
                }
            }

            // If we've got a suitable class to use then construct it

            if (publishingClass!=null) {
                Class[] paramTypes = {ScheduledTaskEntity.class, DistributionListEntity.class};
                Object[] paramValues = {task, list};
                Constructor objCon=publishingClass.getConstructor(paramTypes);
                publisher=(Publisher)objCon.newInstance(paramValues);
            }
        }
        catch (Exception e) {
            if (publishingClass==null)
                throw new PivotalException("Cannot publish [" + list.getType() + "] for distribution list [" + list.getName() + ']', e);
            else
                throw new PivotalException(PivotalException.getErrorMessage(e));
        }

        if (publisher==null)
            throw new PivotalException("Unknown publishing channel type [" + list.getType() + "] for distribution list [" + list.getName() + ']');

        return publisher;
    }

}
