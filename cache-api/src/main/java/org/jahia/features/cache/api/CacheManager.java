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
package org.jahia.features.cache.api;

import java.util.List;

/**
 * @author Jerome Blanchard
 */
public interface CacheManager {

    String getCacheProviderName();

    List<String> listCacheNames();

    <T> Cache<T> createCache(String name, CacheConfig config, Class<T> type) throws CacheAlreadyExistsException;

    <T> Cache<T> getCache(String name, Class<T> type) throws CacheNotFoundException;

    void clearCache(String name) throws CacheNotFoundException;

    void clearAll();

}
