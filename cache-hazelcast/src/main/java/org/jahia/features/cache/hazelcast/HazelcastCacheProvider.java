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

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.jahia.features.cache.api.Cache;
import org.jahia.features.cache.api.CacheConfig;
import org.jahia.features.cache.api.CacheProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jerome Blanchard
 */
@Component(service = {HazelcastCacheProvider.class, CacheProvider.class}, immediate = true)
public class HazelcastCacheProvider implements CacheProvider {

    public static final String PROVIDER_NAME = "hazelcast";
    public static final int PROVIDER_PRIORITY = 10;

    private static final Logger LOGGER = LoggerFactory.getLogger(HazelcastCacheProvider.class);

    private static HazelcastInstance hazelcastInstance;

    @Activate
    public void activate() {
        LOGGER.info("Hazelcast cache service activated");
        if (hazelcastInstance == null) {
            Config config = new Config();
            config.setClusterName("hazelcast-cache-cluster");
            hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        }
    }

    @Deactivate
    public void deactivate() {
        LOGGER.info("Hazelcast cache service deactivated");
        if (hazelcastInstance != null) {
            hazelcastInstance.shutdown();
            hazelcastInstance = null;
        }
    }

    public HazelcastInstance getHazelcastInstance() {
        return hazelcastInstance;
    }

    @Override public <T> Cache<T> createCache(String name, CacheConfig config, Class<T> type) {
        if (hazelcastInstance == null) {
            throw new IllegalStateException("Hazelcast instance is not available");
        }
        return new HazelcastCache<>(hazelcastInstance, name, config);
    }

    @Override public boolean isAvailable() {
        return hazelcastInstance != null && hazelcastInstance.getLifecycleService().isRunning();
    }

    @Override public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override public int getPriority() {
        return PROVIDER_PRIORITY;
    }
}
