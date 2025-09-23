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
package org.jahia.features.cache.infinispan;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jahia.features.cache.api.Cache;
import org.jahia.features.cache.api.CacheConfig;
import org.jahia.features.cache.api.CacheEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Infinispan-based clustered cache implementation
 *
 * @author Jerome Blanchard
 */
public class InfinispanCache<T> implements Cache<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InfinispanCache.class);

    private final String cacheName;
    private final CacheConfig cacheConfig;
    private final org.infinispan.Cache<String, CacheEntry<T>> infinispanCache;

    public InfinispanCache(EmbeddedCacheManager cacheManager, String cacheName, CacheConfig cacheConfig) {
        this.cacheName = cacheName;
        this.cacheConfig = cacheConfig;
        if (!cacheManager.cacheExists(cacheName)) {
            ConfigurationBuilder builder = new ConfigurationBuilder();
            builder.clustering().cacheMode(CacheMode.DIST_SYNC);
            if (cacheConfig.getMaxEntries() > 0) {
                builder.memory().maxCount(cacheConfig.getMaxEntries()).whenFull(EvictionStrategy.REMOVE);
            }
            if (cacheConfig.getTimeToLive() > 0) {
                builder.expiration().maxIdle(-1, TimeUnit.SECONDS).lifespan(cacheConfig.getTimeToLive(), TimeUnit.SECONDS);
            }
            cacheManager.defineConfiguration(cacheName, builder.build());
        }
        this.infinispanCache = cacheManager.getCache(cacheName);
        LOGGER.info("Created infinispan cache: {}", cacheName);
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
        CacheEntry<T> entry = infinispanCache.get(key);
        if (entry != null) {
            entry.touch();
            infinispanCache.put(key, entry); // Update accessed timestamp
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
        return infinispanCache.put(key, entry);
    }

    @Override
    public CacheEntry<T> delete(String key) {
        return infinispanCache.remove(key);
    }

    @Override
    public int size() {
        return infinispanCache.size();
    }

    @Override
    public void clear() {
        infinispanCache.clear();
    }
}
