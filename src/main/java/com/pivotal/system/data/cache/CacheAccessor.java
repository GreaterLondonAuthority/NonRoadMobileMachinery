/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.data.cache;

import java.util.List;
import java.util.Map;

/**
 * Base cache accessor class. It has all the methods to be implemented
 * by different cache implementations.
 *
 * This default implementation can be used as a no-op cache.
 */
public class CacheAccessor {

    // The maximum number of seconds that an object will remain in the cache if not accessed
    /** Constant <code>DEFAULT_TIME_TO_IDLE_SECONDS=600</code> */
    public static final int DEFAULT_TIME_TO_IDLE_SECONDS = 600;

    /**
     * Retrieve an object from the cache
     *
     * @param key the key used to look up the object
     * @return the object, if found, null otherwise
     * @param <T> a T object.
     */
    public <T> T get(String key) {
        return null;
    }

    /**
     * Put an object into the cache
     *
     * @param key the key to look up this object
     * @param timeToLive the expiration time of the cached object
     * @param obj the objecto to put into the cache
     */
    public void put(String key, Integer timeToLive, Object obj) {
    }

    /**
     * handle all the steps necessary to shutdown the cache accessor
     */
    public void shutdown() {
    }

    /**
     * Returns a list of the keys in the cache
     *
     * @return List of key objects
     */
    public List getKeys() {
        return null;
    }

    /**
     * Deletes the object with this key from the cache
     * Ignores the operation if the key doesn't exist
     *
     * @param key Key of the object to delete
     */
    public void delete(String key) {
    }

    /**
     * Returns a map of strings representing the current statistics of the cache
     *
     * @return Stats of the cache
     */
    public Map<String,Object> getStatistics() {
        return null;
    }

    /**
     * Enables/disables statistics
     *
     * @param value True to enable
     */
    public void enableStatistics(boolean value) {
    }

    /**
     * Clears the statistics
     */
    public void clearStatistics() {
    }
}
