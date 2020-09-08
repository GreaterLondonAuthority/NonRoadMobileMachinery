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
import com.pivotal.utils.PivotalException;

/**
 * This class serves as a singleton provider of the cache accessor object.
 * It also checks current settings. In case of change it creates a new object.
 */
public class CacheAccessorFactory {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CacheAccessorFactory.class);

    private static String cacheEngine;
    private static String memcachedServer;
    private static Integer cacheSize;

    private static CacheAccessor cache;

    static {
        cache = null;
    }

    /**
     * Prevent instantiation
     */
    private CacheAccessorFactory() {}

    /**
     * Returns true if the cache is initialised
     *
     * @return a boolean.
     */
    public static boolean isInitialised() {
        return cache!=null;
    }

    /**
     * Return a singleton instance based on the current settings.
     * If settings are changed, a new instance will be created. Then use it carefully.
     *
     * @return The cache implementation
     */
    public static CacheAccessor getInstance() {
        if (cache == null) {
            createNewInstance();
        }
        return cache;
    }

    /**
     * Create a new CacheAccessor instance.
     * If there's an active instance, it will be shutdown.
     *
     */
    private synchronized static void createNewInstance() {
        if (cache != null) {
            try {
                cache.shutdown();
            }
            catch (Exception e) {}
        }

        try {
            // Get the settings locally

            Z_updateSettings();

            // create CacheAccessor instance based on the current settings

            if ("memcached".equalsIgnoreCase(cacheEngine)) {
                cache = new CacheAccessorMemcached(memcachedServer);
            }
            else if ("ehcache".equalsIgnoreCase(cacheEngine)) {
                cache = new CacheAccessorEHCache(cacheSize);
            }
            else
                cache = new CacheAccessor();
        }
        catch (Exception e) {
            logger.warn("Unable to initiate cache engine - {}", PivotalException.getErrorMessage(e));
            cache = new CacheAccessor();
        }
    }

    /**
     * Get the most recent settings.
     */
    public static void updateSettings() {
        String tmpCacheEngine = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_CACHE_ENGINE, HibernateUtils.SETTING_CACHE_ENGINE_DEFAULT);
        String tmpMemcachedServer = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_CACHE_MEMCACHED_SERVERS, HibernateUtils.SETTING_CACHE_MEMCACHED_SERVERS_DEFAULT);
        Integer tmpCacheSize = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_CACHE_MAX_SIZE, HibernateUtils.SETTING_CACHE_MAX_SIZE_DEFAULT);

        boolean hasChangedConfig = !Common.doStringsMatch(tmpCacheEngine,cacheEngine) ||
                                   !Common.doStringsMatch(tmpMemcachedServer,memcachedServer) ||
                                   !tmpCacheSize.equals(cacheSize);

        // If the settings have changed then update the cache

        if (hasChangedConfig) {
            cacheEngine = tmpCacheEngine;
            memcachedServer = tmpMemcachedServer;
            cacheSize = tmpCacheSize;
            createNewInstance();
        }
    }

    /**
     * Get the most recent settings.
     */
    private static void Z_updateSettings() {
        cacheEngine = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_CACHE_ENGINE, HibernateUtils.SETTING_CACHE_ENGINE_DEFAULT);
        memcachedServer = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_CACHE_MEMCACHED_SERVERS, HibernateUtils.SETTING_CACHE_MEMCACHED_SERVERS_DEFAULT);
        cacheSize = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_CACHE_MAX_SIZE, HibernateUtils.SETTING_CACHE_MAX_SIZE_DEFAULT);
    }

    /**
     * This will shutdown the cache manager correctly by shutting down the current cache manager and clearing the local instance.
     */
    public static void shutdown() {
        if(isInitialised()) {
            cache.shutdown();

            // Clear the cache so that it is refreshed on the restart

            cache = null;
        }
    }
}
