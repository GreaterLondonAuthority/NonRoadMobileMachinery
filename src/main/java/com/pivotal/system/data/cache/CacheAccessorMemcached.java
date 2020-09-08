/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.data.cache;

import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles communication with memcached server
 */
public class CacheAccessorMemcached extends CacheAccessor {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CacheAccessorMemcached.class);

    private MemcachedClient client;

    private Set<String> cacheKeys = new HashSet<>();

    /**
     * Create a new accessor to memcached servers.
     *
     * @param serverList a list of memcached servers to be used in AddrUtil.getAddresses() function.
     * @throws java.io.IOException if unable to create accessor object
     */
    public CacheAccessorMemcached(String serverList) throws IOException {
        if (Common.isBlank(serverList))
            throw new PivotalException("Cannot instantiate a memcached client when no servers have been defined");
        client = new MemcachedClient(AddrUtil.getAddresses(serverList));
        cacheKeys = new HashSet<>();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(String key) {
        return (T)client.get(key);
    }

    /** {@inheritDoc} */
    @Override
    public void put(String key, Integer timeToLive, Object obj) {
        client.set(key, timeToLive, obj);
        cacheKeys.add(key);
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown() {
        client.shutdown();
    }

    /** {@inheritDoc} */
    @Override
    public List getKeys() {
        return new ArrayList<>(cacheKeys);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(String key) {
        client.delete(key);
        cacheKeys.remove(key);
    }

}
