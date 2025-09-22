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

import org.jahia.features.cache.api.*;
import org.jahia.features.cache.api.CacheAlreadyExistsException;
import org.jahia.features.cache.api.CacheNotFoundException;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Jerome Blanchard
 */
@Component(service = CacheManager.class, immediate = true)
public class CacheManagerImpl implements CacheManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheManagerImpl.class);
    private final List<CacheProvider> providers = new CopyOnWriteArrayList<>();
    private CacheProvider activeProvider;
    private ConcurrentHashMap<String, Cache<?>> caches;

    public CacheManagerImpl() {
        LOGGER.info("Instantiating cache manager");
        caches = new ConcurrentHashMap<>();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "removeProvider")
    public void addProvider(CacheProvider provider) {
        LOGGER.info("Adding CacheProvider: {}", provider.getProviderName());
        providers.add(provider);
        providers.sort((p1, p2) -> Integer.compare(p2.getPriority(), p1.getPriority()));
        this.activeProvider = getBestAvailableProvider();
        this.rebuildCaches();
    }

    public void removeProvider(CacheProvider provider) {
        LOGGER.info("Removing CacheProvider: {}", provider.getProviderName());
        providers.remove(provider);
        this.activeProvider = getBestAvailableProvider();
        this.rebuildCaches();
    }

    @Activate
    public void activate() {
        LOGGER.info("Cache Manager activated");
    }

    @Deactivate
    public void deactivate() {
        LOGGER.info("Cache Manager deactivated");
        caches.clear();
    }

    @Override
    public String getCacheProviderName() {
        return (activeProvider != null) ? activeProvider.getProviderName() : "none";
    }

    @Override
    public List<String> listCacheNames() {
        return caches.keySet().stream().toList();
    }

    @Override
    public <T> Cache<T> createCache(String name, CacheConfig config, Class<T> type) throws CacheAlreadyExistsException {
        if (caches.containsKey(name)) {
            throw new CacheAlreadyExistsException("Cache " + name + " already exists");
        }
        LOGGER.info("Creating cache {} using provider: {}", name, activeProvider.getProviderName());
        Cache<T> cache = activeProvider.createCache(name, config, type);
        caches.put(name, cache);
        return cache;
    }

    @Override
    public <T> Cache<T> getCache(String name, Class<T> type) throws CacheNotFoundException {
        if (!caches.containsKey(name)) {
            throw new CacheNotFoundException("Cache " + name + " does not exist");
        }
        return (Cache<T>) caches.get(name);
    }

    @Override
    public void clearCache(String name) throws CacheNotFoundException {
        if (!caches.containsKey(name)) {
            throw new CacheNotFoundException("Cache " + name + " does not exist");
        }
        LOGGER.info("Clearing cache {}", name);
        caches.get(name).clear();
    }

    @Override
    public void clearAll() {
        LOGGER.info("Clearing all caches");
        caches.forEach((key, value) -> value.clear());
    }

    private CacheProvider getBestAvailableProvider() {
        return providers.stream()
                .filter(CacheProvider::isAvailable)
                .findFirst()
                .orElse(null);
    }

    private synchronized void rebuildCaches() {
        ConcurrentHashMap<String, Cache<?>> newCaches = new ConcurrentHashMap<>();
        caches.forEach((name, oldCache) -> {
            try {
                LOGGER.info("Recreating cache {} using provider: {}", name, activeProvider.getProviderName());
                Cache<?> newCache = activeProvider.createCache(name, oldCache.getConfig(), Object.class);
                newCaches.put(name, newCache);
            } catch (Exception e) {
                LOGGER.error("Failed to recreate cache {}", name, e);
            }
        });
        caches = newCaches;
    }

}
