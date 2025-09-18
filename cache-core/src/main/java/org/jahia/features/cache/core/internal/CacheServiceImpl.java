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

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.jahia.features.cache.api.*;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jerome Blanchard
 */
@Component(
        service = CacheService.class,
        immediate = true
)
public class CacheServiceImpl implements CacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheServiceImpl.class);

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private FeaturesService featureService;

    private ConcurrentHashMap<String, Cache<?>> caches;

    public CacheServiceImpl() {
        LOGGER.info("Instantiating cache service");
        caches = new ConcurrentHashMap<>();
    }

    @Activate
    public void activate() {
        LOGGER.info("Cache service activated");
    }

    @Deactivate
    public void deactivate() {
        LOGGER.info("Cache service deactivated");
        caches.clear();
        ClusteredCache.shutdown();
    }

    @Override
    public List<String> list() {
        return caches.keySet().stream().toList();
    }

    @Override
    public <T> Cache<T> create(String name, CacheConfig config, Class<T> type) throws CacheAlreadyExistsException {
        if (caches.containsKey(name)) {
            throw new CacheAlreadyExistsException("Cache " + name + " already exists");
        }

        Cache<T> cache;
        if (isClusterFeatureAvailable()) {
            LOGGER.info("Creating clustered cache {} with Infinispan", name);
            cache = new ClusteredCache<>(name, config);
        } else {
            LOGGER.info("Creating in-memory cache {}", name);
            cache = new InMemoryCache<>(name, config);
        }

        caches.put(name, cache);
        LOGGER.info("Created cache {}", name);
        return cache;
    }

    @Override
    public <T> Cache<T> get(String name, Class<T> type) throws CacheNotFoundException {
        if (!caches.containsKey(name)) {
            throw new CacheNotFoundException("Cache " + name + " does not exist");
        }
        return (Cache<T>) caches.get(name);
    }

    @Override
    public void clear(String name) throws CacheNotFoundException {
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

    private boolean isClusterFeatureAvailable() {
        try {
            if (featureService != null) {
                for (Feature feature : featureService.listInstalledFeatures()) {
                    if (feature.getName().equals("infinispan")) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to check if cluster feature is available", e);
        }
        return false;
    }

}
