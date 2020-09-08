/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.servlet;

import com.pivotal.utils.Common;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.ResourceCacheImpl;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This is a resource cache provider that doesn't offer much more than
 * the standard except that we have better control over it
 */
public class VelocityResourceCache extends ResourceCacheImpl {

    public static final String RUNTIME_ENGINE_IDENTIFER = "runtime.engine.identifer";
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(VelocityResourceCache.class);
    private static Map<String,Map> cacheHandlers = new HashMap<>();
    private static VelocityCacheStats cacheStats = new VelocityCacheStats(cacheHandlers);

    @Override
    public void initialize(RuntimeServices rs) {
        super.initialize(rs);

        // Find out if this is one of the cached engines

        String engineID = rs.getString(RUNTIME_ENGINE_IDENTIFER);
        if (Common.isBlank(engineID)) engineID = "staticEngine-" + new Date().getTime();
        cacheHandlers.put(engineID, cache);
    }

    @Override
    public Resource get(Object key) {
        Resource res=super.get(key);
        if (res==null) {
            cacheStats.addMiss();
            logger.debug("Missed resource [" + key + ']');
        }
        else
            cacheStats.addHit();
        return res;
    }

    @Override
    public Resource put(Object key, Resource value) {
        cacheStats.addPut();
        logger.debug("Put resource [" + key + ']');
        return super.put(key, value);
    }

    @Override
    public Resource remove(Object key) {
        cacheStats.addDelete();
        logger.debug("Removed resource [" + key + ']');
        return super.remove(key);
    }

    @Override
    public Iterator enumerateKeys() {
        return super.enumerateKeys();
    }

    /**
     * Clears the cache
     */
    synchronized public static void clear() {
        logger.debug("Cleared cache");
        if (!Common.isBlank(cacheHandlers)) {
            for (Map map : cacheHandlers.values()) {
                map.clear();
            }
        }
    }

    /**
     * Clears the cache for the specific engine
     *
     * @param engine Engine to remove from the cache
     */
    synchronized public static void clear(VelocityEngine engine) {
        if (engine!=null) {
            logger.debug("Cleared cache for engine [" + engine + ']');
            String engineID = (String)engine.getProperty(RUNTIME_ENGINE_IDENTIFER);
            if (!Common.isBlank(cacheHandlers) && !Common.isBlank(engineID)) {
                Map map = cacheHandlers.get(engineID);
                if (!Common.isBlank(map)) {
                    map.clear();
                    cacheHandlers.remove(engineID);
                }
            }
        }
    }

    /**
     * Clears the cache stats
     */
    synchronized public static void clearStats() {
        cacheStats = new VelocityCacheStats(cacheHandlers);
    }

    /**
     * Returns the stats object
     *
     * @return Stats object
     */
    public static VelocityCacheStats getStats() {
        return cacheStats;
    }

    /**
     * Convenient storage for mechanism for the cache statistics
     */
    public static class VelocityCacheStats {
        int hits;
        int misses;
        int puts;
        int deletes;
        int size;
        Map<String,Map> cacheList;
        long lastGetMinute;
        long lastPutMinute;
        long lastDeleteMinute;
        long lastMissMinute;
        int hitsMinute;
        int putsMinute;
        int deletesMinute;
        int missesMinute;

        /**
         * Constructs a stats object using the given cache as a reference
         *
         * @param cacheList Cache on which to base stats
         */
        private VelocityCacheStats(Map<String,Map> cacheList) {
            this.cacheList=cacheList;
        }

        /**
         * Adds a hit to the counter
         */
        private void addHit() {
            hits++;
            long minute= new Date().getTime() / 60000;
            if (minute != lastGetMinute) {
                lastGetMinute = minute;
                hitsMinute = 0;
            }
            hitsMinute++;
        }

        /**
         * Adds a miss to the counter
         */
        private void addMiss() {
            misses++;
            long minute= new Date().getTime() / 60000;
            if (minute != lastMissMinute) {
                lastMissMinute = minute;
                missesMinute = 0;
            }
            missesMinute++;
        }

        /**
         * Adds a delete to the counter
         */
        private void addDelete() {
            deletes++;
            long minute= new Date().getTime() / 60000;
            if (minute != lastDeleteMinute) {
                lastDeleteMinute = minute;
                deletesMinute = 0;
            }
            deletesMinute++;
        }

        /**
         * Adds a put to the counter
         */
        private void addPut() {
            puts++;
            long minute= new Date().getTime() / 60000;
            if (minute != lastPutMinute) {
                lastPutMinute = minute;
                putsMinute = 0;
            }
            putsMinute++;
        }

        /**
         * Returns the number of hits on the cache that have been made since the last initialisation
         *
         * @return Number of hits
         */
        public int getHits() {
            return hits;
        }

        /**
         * Returns the number of misses on the cache that have been made since the last initialisation
         *
         * @return Number of misses
         */
        public int getMisses() {
            return misses;
        }

        /**
         * Returns the number of puts on the cache that have been made since the last initialisation
         *
         * @return Number of puts
         */
        public int getPuts() {
            return puts;
        }

        /**
         * Returns the number of deletes on the cache that have been made since the last initialisation
         *
         * @return Number of deletes
         */
        public int getDeletes() {
            return deletes;
        }

        /**
         * Returns the number of objects in the cache
         *
         * @return Number of objects in the cache
         */
        public int getSize() {
            int cnt=0;
            if (!Common.isBlank(cacheList)) {
                for (Map map : cacheList.values()) {
                    cnt+=map.size();
                }
            }
            return cnt;
        }

        /**
         * Returns the number of hits on the cache per second over the past 1 minutes
         *
         * @return Number of hits per second
         */
        public float getHitRate() {
            try {
                return hitsMinute / ((new Date().getTime() - new Date(lastGetMinute * 60000).getTime()) / 1000);
            }
            catch (Exception e) {
                return 0;
            }
        }

        /**
         * Returns the number of deletes on the cache per second over the past 1 minutes
         *
         * @return Number of deletes per second
         */
        public float getDeleteRate() {
            try {
                return deletesMinute / ((new Date().getTime() - new Date(lastDeleteMinute * 60000).getTime()) / 1000);
            }
            catch (Exception e) {
                return 0;
            }
        }

        /**
         * Returns the number of puts on the cache per second over the past 1 minutes
         *
         * @return Number of puts per second
         */
        public float getPutRate() {
            try {
                return putsMinute / ((new Date().getTime() - new Date(lastPutMinute * 60000).getTime()) / 1000);
            }
            catch (Exception e) {
                return 0;
            }
        }

        /**
         * Returns the number of misses on the cache per second over the past 1 minutes
         *
         * @return Number of misses per second
         */
        public float getMissRate() {
            try {
                return missesMinute / ((new Date().getTime() - new Date(lastMissMinute * 60000).getTime()) / 1000);
            }
            catch (Exception e) {
                return 0;
            }
        }
    }

}
