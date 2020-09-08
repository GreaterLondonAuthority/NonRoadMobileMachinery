/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.data.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.statistics.LiveCacheStatistics;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that handles all the communication with EHcache
 */
public class CacheAccessorEHCache extends CacheAccessor{

    private Cache cache;
    private long hitCount;
    private Date lastCount;
    private long missCount;
    private long putCount;
    private long deleteCount;

    /**
     * Creates an accessor to EHcache. Cache size and time to live are configurable
     *
     * @param maxSize max memory (mb) to use
     */
    public CacheAccessorEHCache(Integer maxSize) {

        //Create a Cache specifying its configuration.

        cache = new Cache(
          new CacheConfiguration()
              .name("nrmm-cache")
            .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU)
            .overflowToDisk(false)
            .timeToIdleSeconds(DEFAULT_TIME_TO_IDLE_SECONDS)
            .maxBytesLocalHeap(maxSize.longValue(),MemoryUnit.MEGABYTES));

        CacheManager.getInstance().addCache(cache);

        // Turn on the stats

        enableStatistics(true);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(String key) {
        if (cache==null)
            return null;
        else {
            Element element = cache.get(key);
            if (element != null)
                return (T) element.getObjectValue();
            else
                return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void put(String key, Integer timeToLive, Object obj) {
        if (cache!=null) {
            Element element = new Element(key, obj);
            element.setTimeToLive(timeToLive);
            cache.put(element);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown() {

        // We don't want to actually shutdown the cache manager because it is used
        // by Hibernate so we only want to close this private cache

        if (cache != null) {
            if (cache.getStatus().equals(Status.STATUS_ALIVE)) {
                cache.removeAll();
            }
            CacheManager.getInstance().removeCache(cache.getName());

            // Moved from Initialisation because it is EHCache specific so should be moved in here

            CacheManager.getInstance().shutdown();
            cache = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public List getKeys() {
        return cache.getKeys();
    }

    /** {@inheritDoc} */
    @Override
    public void delete(String key) {
        cache.remove(key);
    }

    /** {@inheritDoc} */
    @Override
    public Map<String,Object> getStatistics() {
        Map<String,Object> returnValue = null;
        if (cache!=null) {
            returnValue = new LinkedHashMap<>();
            returnValue.put("Settings", cache.toString());
            LiveCacheStatistics stats = cache.getLiveCacheStatistics();

            returnValue.put("Size", stats.getSize());
            returnValue.put("Name", stats.getCacheName());
            returnValue.put("Hits", stats.getCacheHitCount());
            returnValue.put("Misses", stats.getCacheMissCount());
            returnValue.put("Deletes", stats.getRemovedCount());
            returnValue.put("Puts", stats.getPutCount());

            returnValue.put("HitRate", getCountRate(hitCount, stats.getCacheHitCount()));
            returnValue.put("MissRate", getCountRate(missCount, stats.getCacheMissCount()));
            returnValue.put("PutRate", getCountRate(putCount, stats.getPutCount()));
            returnValue.put("DeleteRate", getCountRate(deleteCount, stats.getRemovedCount()));

            hitCount = stats.getCacheHitCount();
            missCount = stats.getCacheMissCount();
            putCount = stats.getPutCount();
            deleteCount = stats.getRemovedCount();
            lastCount = new Date();
        }
        return returnValue;
    }

    /** {@inheritDoc} */
    @Override
    public void enableStatistics(boolean value) {
        if (cache!=null) cache.setStatisticsEnabled(value);
    }

    /** {@inheritDoc} */
    @Override
    public void clearStatistics() {
        if (cache!=null) {
            cache.clearStatistics();
        }
    }

    /**
     * Works out the rough rate based on the number of hits since the last time we
     * measured it
     *
     * @param oldCount The old count
     * @param newCount The current count
     *
     * @return Count per second
     */
    private double getCountRate(long oldCount, long newCount) {

        double returnValue = 0.0;
        Date now = new Date();

        if (lastCount!=null && now.getTime()!=lastCount.getTime()) {
            returnValue = (newCount - oldCount) * 1000 / (now.getTime() - lastCount.getTime());
        }

        return returnValue;
    }
}
