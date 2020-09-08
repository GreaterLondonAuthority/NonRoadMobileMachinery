/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.data.cache;

/**
 * This class is a wrapper around the standard CacheEngine that only allows
 * access to certain features of the cache for safety e.g. users can't clear
 * the cache
 */
public class CacheEngineSafe {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CacheEngineSafe.class);

    /**
     * Prevent instantiation
     */
    private CacheEngineSafe() {}

       /**
        * Retrieves an object from the cache. If object is not present or error
        * it will simply return null.
        *
        * @param key the key to look up the object
        * @return either the object from the cache if found or null
        */
       public static Object get(String key) {
           return CacheEngine.get(key);
       }

       /**
        * Puts an object into the cache. In case of error nothing will be thrown.
        *
        * @param key the key to store the object into the cache
        * @param timeToLive the expiration time of the cached object (seconds)
        * @param obj the object to store into the cache
        */
       public static void put(String key, Integer timeToLive, Object obj) {
        CacheEngine.put(key, timeToLive, obj);
       }

    /**
     * Deletes the object with the give key from the cache
     *
     * @param key Name of the object to delete
     */
    public static void delete(String key) {
        CacheEngine.delete(key);
    }
}
