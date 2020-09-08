/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils;

import com.pivotal.system.hibernate.entities.LookupsEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Helper for using lookups
 */
public class LookupHelper {

    private static final Logger logger = LoggerFactory.getLogger(LookupHelper.class);

    /**
     * Gets lookup record using type and name
     *
     * @param type Type of lookup to get
     * @param name Name of lookup to get
     *
     * @return LookupsEntity
     */
    public static LookupsEntity getLookupByName(String type, String name) {

        return HibernateUtils.selectFirstEntity("From LookupsEntity where lower(type) = lower(?) and lower(name) = lower(?)", type, name);
    }

    /**
     * Gets lookup records using type
     *
     * @param type Type of lookup to get
     *
     * @return List of LookupsEntities
     */
    public static List<LookupsEntity> getLookupByType(String type) {

        return HibernateUtils.selectEntities("From LookupsEntity where lower(type) = lower(?)", type);
    }

    /**
     * Gets lookup record using type and tag
     *
     * @param type Type of lookup to get
     * @param tag Tag of lookup to get
     *
     * @return LookupsEntity
     */
    public static LookupsEntity getLookupByTag(String type, String tag) {

        return HibernateUtils.selectFirstEntity("From LookupsEntity where lower(type) = lower(?) and lower(tag) = lower(?)", type, tag);
    }

    /**
     * Gets lookup tag using type and name
     *
     * @param type Type of lookup to get
     * @param name Tag of lookup to get
     *
     * @return tag string or "" if not found
     */
    public static String getLookupTag(String type, String name) {

        LookupsEntity lookupsEntity = getLookupByName(type, name);

        if (lookupsEntity != null && !Common.isBlank(lookupsEntity.getTag()))
            return lookupsEntity.getTag();
        else
            return "";
    }
}
