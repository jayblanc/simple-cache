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
import org.jahia.features.cache.api.CacheResult;
import org.jahia.features.cache.api.CacheInvalidate;
import org.jahia.features.cache.api.CacheInvalidateAll;
import org.jahia.features.cache.api.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.annotation.Annotation;

/**
 * @author Jerome Blanchard
 */
public class CacheInterceptor implements InvocationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheInterceptor.class);

    private final Object target;
    private final CacheManager cacheManager;

    public CacheInterceptor(Object target, CacheManager cacheManager) {
        this.target = target;
        this.cacheManager = cacheManager;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        LOGGER.info("Invoking method: {}.{}", target.getClass().getName(), method.getName());

        // Check for cache invalidation annotations first
        CacheInvalidate invalidateAnn = getAnnotation(method, CacheInvalidate.class);
        CacheInvalidateAll invalidateAllAnn = getAnnotation(method, CacheInvalidateAll.class);

        // Handle cache invalidation
        if (invalidateAnn != null) {
            String cacheName = invalidateAnn.cacheName();
            LOGGER.info("Method requires cache invalidation for cache: {}", cacheName);
            try {
                Cache<Object> cache = cacheManager.getCache(cacheName, Object.class);
                String key = CacheKeyGenerator.generate(method, args, target);
                cache.delete(key);
                LOGGER.info("Cache entry with key: {} evicted from cache: {}", key, cacheName);
            } catch (Exception e) {
                LOGGER.warn("Failed to evict cache entry from cache: {}", cacheName, e);
            }
            return method.invoke(target, args);
        }

        if (invalidateAllAnn != null) {
            String cacheName = invalidateAllAnn.cacheName();
            LOGGER.info("Method requires cache invalidation of all entries for cache: {}", cacheName);
            try {
                Cache<Object> cache = cacheManager.getCache(cacheName, Object.class);
                cache.clear();
                LOGGER.info("All entries evicted from cache: {}", cacheName);
            } catch (Exception e) {
                LOGGER.warn("Failed to clear cache: {}", cacheName, e);
            }
            return method.invoke(target, args);
        }

        // Handle cache result
        CacheResult ann = getAnnotation(method, CacheResult.class);
        if (ann != null) {
            String cacheName = ann.cacheName();
            LOGGER.info("Methods requires cached result from cache with name: {}", cacheName);
            Cache<Object> cache;
            try {
                cache = cacheManager.getCache(cacheName, Object.class);
            } catch (Exception e) {
                LOGGER.info("Cache {} not found, creating it with default configuration.", cacheName);
                cache = cacheManager.createCache(cacheName, CacheConfig.create().build(), Object.class);
            }
            String key = CacheKeyGenerator.generate(method, args, target);
            Object value = cache.get(key);
            if (value != null) {
                LOGGER.info("Cache hit for key: {} in cache: {}", key, cacheName);
                return value;
            }
            LOGGER.info("Cache miss for key: {} in cache: {}. Caching result.", key, cacheName);
            value = method.invoke(target, args);

            if (value != null) {
                LOGGER.info("Caching value for key: {} in cache: {}", key, cacheName);
                cache.put(key, value);
            }
            return value;
        }
        return method.invoke(target, args);
    }

    private <T extends Annotation> T getAnnotation(Method method, Class<T> annotationClass) {
        T ann = method.getAnnotation(annotationClass);
        if (ann == null) {
            try {
                Method implMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
                ann = implMethod.getAnnotation(annotationClass);
            } catch (NoSuchMethodException e) {
                LOGGER.debug("Method {} not found in the implementation.", method.getName());
            }
        }
        return ann;
    }
}
