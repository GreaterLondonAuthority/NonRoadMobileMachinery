/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.data.cache;

import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.Common;
import com.pivotal.web.servlet.ServletHelper;

import java.util.List;

/**
 * Class created to be the only interaction between the client and the cache.
 * It exposes just three methods - get,put and clear - and handle internally all other steps
 */
public class CacheEngine {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CacheEngine.class);

    /**
     * Prevent instantiation
     */
    private CacheEngine() {}

    /**
     * Retrieves an object from the cache. If object is not present or error
     * it will simply return null.
     *
     * @param key the key to look up the object
     * @return either the object from the cache if found or null
     * @param <T> a T object.
     */
    public static <T> T get(String key) {
        T ret = null;
        try {
            ret = CacheAccessorFactory.getInstance().get(key);
        } catch (Exception e) {
            logger.warn("Unable to retrieve object ["+key+"] from cache", e);
        }
        return ret;
    }

    /**
     * Puts an object into the cache. In case of error nothing will be thrown.
     *
     * @param key the key to store the object into the cache
     * @param timeToLive the expiration time of the cached object (seconds)
     * @param obj the object to store into the cache
     */
    public static void put(String key, Integer timeToLive, Object obj) {

        // if time to live is not defined in the datasource, use default configuration
        if (timeToLive == null) {
            timeToLive = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_CACHE_EXPIRATION, HibernateUtils.SETTING_CACHE_EXPIRATION_DEFAULT);
        }

        try {
            CacheAccessorFactory.getInstance().put(key, timeToLive, obj);
        } catch (Exception e) {
            logger.warn("Unable to put object ["+key+"] into cache", e);
        }
    }

    /**
     * Clears the cache of all objects that use this datasource on the current NRMM instance
     *
     * @param dataSourceId ID of the datasource
     */
    public static void clear(int dataSourceId) {
        String keyLead = ServletHelper.getAppIdentity() + '|' + dataSourceId + '|';

        try {
            CacheAccessor cache=CacheAccessorFactory.getInstance();
            List keys=cache.getKeys();
            if (!Common.isBlank(keys)) {
                for (Object key : keys) {
                    if (key.getClass().getName().equals(String.class.getName()) &&
                        ((String)key).startsWith(keyLead))
                        cache.delete((String)key);
                }
              }
        }
        catch (Exception e) {
            logger.warn("Unable to clear cache for ["+keyLead+']', e);
        }
    }

    /**
     * Creates a general purpose key using a combination of the NRMM instance name,
     * the datasource ID and a part of the SQL query
     *
     * Format is: NRMM ID | datasource id | md5(query) | beginning of the query
     *
     * @param dataSourceId ID of the datasource4
     * @param sql SQL query
     * @return String
     */
    public static String getCacheKey(int dataSourceId, String sql) {
        String key = ServletHelper.getAppIdentity() + '|' + dataSourceId + '|' + Common.getMD5String(sql) + '|';

        //add part of the query as key (keys cannot have spaces)
        sql = sql.replaceAll("\\s", "");

        //add the first 50 chars of the query into the key
        if (!Common.isBlank(sql)) {
            key += new String(sql.substring(0, sql.length() > 50 ? 50 : sql.length()));
        }

        return key;
    }

    /**
     * Deletes the object with the give key from the cache
     *
     * @param key Name of the object to delete
     */
    public static void delete(String key) {
        CacheAccessorFactory.getInstance().delete(key);
    }
}
