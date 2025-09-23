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
package org.jahia.features.cache.sample;

import org.jahia.features.cache.api.CacheInvalidate;
import org.jahia.features.cache.api.CacheInvalidateAll;
import org.jahia.features.cache.api.CacheKey;
import org.jahia.features.cache.api.CacheResult;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jerome Blanchard
 */
@Component(service = ValueService.class, immediate = true)
public class ValueServiceImpl implements ValueService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValueServiceImpl.class);
    private static final String CACHE_NAME = "sample-basic-cache";

    private final Map<String, String> keyValueStore = new HashMap<>();

    @Override
    @CacheResult(cacheName = CACHE_NAME)
    public String getValue(@CacheKey String key) {
        LOGGER.info("Getting value for key: {}", key);
        String storedValue = keyValueStore.getOrDefault(key, "not found");
        return "Value for key '" + key + "' at " + System.currentTimeMillis() + " is: " + storedValue;
    }

    @Override
    @CacheInvalidate(cacheName = CACHE_NAME)
    public void updateValue(@CacheKey String key, String value) {
        LOGGER.info("Updating value for key '{}' to '{}'", key, value);
        keyValueStore.put(key, value);
    }

    @Override
    @CacheInvalidateAll(cacheName = CACHE_NAME)
    public void clearAllValues() {
        LOGGER.info("Clearing all values from store");
        keyValueStore.clear();
    }
}
