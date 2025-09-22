/*
 * Copyright (C) 2002-2025 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.features.cache.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.replicatedmap.ReplicatedMap;
import org.jahia.features.cache.api.Cache;
import org.jahia.features.cache.api.CacheConfig;
import org.jahia.features.cache.api.CacheEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hazelcast-based clustered cache implementation
 *
 * @author Jerome Blanchard
 */
public class HazelcastCache<T> implements Cache<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HazelcastCache.class);

    private final String cacheName;
    private final CacheConfig cacheConfig;
    private final ReplicatedMap<String, CacheEntry<T>> hazelcastMap;

    public HazelcastCache(HazelcastInstance hazelcastInstance, String cacheName, CacheConfig cacheConfig) {
        this.cacheName = cacheName;
        this.cacheConfig = cacheConfig;
        this.hazelcastMap = hazelcastInstance.getReplicatedMap(cacheName);
        LOGGER.info("Created hazelcast cache: {}", cacheName);
    }

    @Override
    public String getName() {
        return cacheName;
    }

    @Override
    public CacheConfig getConfig() {
        return cacheConfig;
    }

    @Override
    public CacheEntry<T> getEntry(String key) {
        CacheEntry<T> entry = hazelcastMap.get(key);
        if (entry != null) {
            entry.touch();
            hazelcastMap.put(key, entry); // Mise à jour du timestamp accessed
            return entry;
        }
        return null;
    }

    @Override
    public T get(String key) {
        CacheEntry<T> entry = getEntry(key);
        return (entry != null) ? entry.value() : null;
    }

    @Override
    public CacheEntry<T> put(String key, T value) {
        CacheEntry<T> entry = new CacheEntry<>(key, value);
        return hazelcastMap.put(key, entry);
    }

    @Override
    public CacheEntry<T> delete(String key) {
        return hazelcastMap.remove(key);
    }

    @Override
    public int size() {
        return hazelcastMap.size();
    }

    @Override
    public void clear() {
        hazelcastMap.clear();
    }
}
