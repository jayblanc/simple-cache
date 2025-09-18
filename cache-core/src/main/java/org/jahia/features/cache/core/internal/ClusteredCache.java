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
package org.jahia.features.cache.core.internal;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jahia.features.cache.api.Cache;
import org.jahia.features.cache.api.CacheConfig;
import org.jahia.features.cache.api.CacheEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Implémentation du cache basée sur Infinispan pour le support du clustering
 *
 * @author Jerome Blanchard
 */
public class ClusteredCache<T> implements Cache<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusteredCache.class);
    private static EmbeddedCacheManager cacheManager;

    private final String cacheName;
    private final CacheConfig cacheConfig;
    private final org.infinispan.Cache<String, ClusteredEntry<T>> infinispanCache;

    public ClusteredCache(String cacheName, CacheConfig cacheConfig) {
        this.cacheName = cacheName;
        this.cacheConfig = cacheConfig;

        if (cacheManager == null) {
            cacheManager = new DefaultCacheManager();
        }

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.clustering().cacheMode(org.infinispan.configuration.cache.CacheMode.DIST_SYNC);
        builder.expiration()
               .maxIdle(-1, TimeUnit.SECONDS)
               .lifespan(cacheConfig.getTimeToLive(), TimeUnit.SECONDS);
        cacheManager.defineConfiguration(cacheName, builder.build());

        this.infinispanCache = cacheManager.getCache(cacheName);
        LOGGER.info("Created clustered cache: {}", cacheName);
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
        ClusteredEntry<T> entry = infinispanCache.get(key);
        if (entry != null) {
            entry.touch();
            infinispanCache.put(key, entry); // Mise à jour du timestamp accessed
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
        ClusteredEntry<T> newEntry = new ClusteredEntry<>(key, value);
        ClusteredEntry<T> oldEntry = infinispanCache.put(key, newEntry);
        return oldEntry;
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

    /**
     * Arrête proprement le cache manager Infinispan
     */
    public static void shutdown() {
        if (cacheManager != null) {
            cacheManager.stop();
            cacheManager = null;
        }
    }

    /**
     * Implémentation d'entrée de cache pour Infinispan qui est Serializable
     */
    private static class ClusteredEntry<T> implements CacheEntry<T>, java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private final String key;
        private final T value;
        private final long created;
        private long accessed;

        public ClusteredEntry(String key, T value) {
            this.key = key;
            this.value = value;
            this.created = System.currentTimeMillis();
            this.accessed = this.created;
        }

        @Override
        public String key() { return key; }

        @Override
        public T value() { return value; }

        @Override
        public long created() { return created; }

        @Override
        public long accessed() { return accessed; }

        public void touch() { accessed = System.currentTimeMillis(); }

        @Override
        public String toString() {
            return "ClusteredEntry{" + "key='" + key + '\'' + ", class=" + value.getClass().getName() + ", created=" + created + ", accessed=" + accessed + '}';
        }
    }
}
