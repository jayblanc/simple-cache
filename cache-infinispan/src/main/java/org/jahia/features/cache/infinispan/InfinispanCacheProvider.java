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
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jahia.features.cache.api.Cache;
import org.jahia.features.cache.api.CacheConfig;
import org.jahia.features.cache.api.CacheProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Jerome Blanchard
 */
@Component(service = {InfinispanCacheProvider.class, CacheProvider.class}, immediate = true)
public class InfinispanCacheProvider implements CacheProvider {

    public static final String PROVIDER_NAME = "infinispan";
    public static final int PROVIDER_PRIORITY = 10;
    public static final String CACHE_MANAGER_NAME = "infinispan-cache-manager";
    public static final String DEFAULT_CACHE = "default";

    private static final Logger LOGGER = LoggerFactory.getLogger(InfinispanCacheProvider.class);
    private EmbeddedCacheManager cacheManager;

    @Activate
    public void activate() {
        LOGGER.info("Infinispan cache provider activated");
        if (cacheManager == null) {
            GlobalConfigurationBuilder global = GlobalConfigurationBuilder.defaultClusteredBuilder();
            global.cacheManagerName(CACHE_MANAGER_NAME);
            global.defaultCacheName(DEFAULT_CACHE);
            cacheManager = new DefaultCacheManager(global.build());
            ConfigurationBuilder builder = new ConfigurationBuilder();
            builder.clustering().cacheMode(CacheMode.DIST_SYNC);
            builder.memory().maxCount(1000).whenFull(EvictionStrategy.REMOVE);
            cacheManager.defineConfiguration(DEFAULT_CACHE, builder.build());
        }
        cacheManager.start();
        cacheManager.getCache().start();
    }

    @Deactivate
    public void deactivate() {
        LOGGER.info("Infinispan cache provider deactivated");
        if (cacheManager != null) {
            try {
                cacheManager.close();
            } catch (IOException e) {
                LOGGER.error("Error closing Infinispan cache manager", e);
            }
        }
    }

    protected EmbeddedCacheManager getEmbeddedCacheManager() {
        return cacheManager;
    }

    @Override
    public <T> Cache<T> createCache(String name, CacheConfig config, Class<T> type) {
        return new InfinispanCache<>(cacheManager, name, config);
    }

    @Override
    public boolean isAvailable() {
        return cacheManager != null && cacheManager.getStatus().allowInvocations();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override public int getPriority() {
        return PROVIDER_PRIORITY;
    }
}
