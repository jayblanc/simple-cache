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

import org.jahia.features.cache.api.Cache;
import org.jahia.features.cache.api.CacheConfig;
import org.jahia.features.cache.api.CacheProvider;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jerome Blanchard
 */
@Component(service = {DefaultCacheProvider.class, CacheProvider.class}, immediate = true)
public class DefaultCacheProvider implements CacheProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCacheProvider.class);

    @Override
    public <T> Cache<T> createCache(String name, CacheConfig config, Class<T> type) {
        LOGGER.info("Creating default cache: {}", name);
        return new InMemoryCache<T>(name, config);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getProviderName() {
        return DEFAULT_PROVIDER_NAME;
    }

}
