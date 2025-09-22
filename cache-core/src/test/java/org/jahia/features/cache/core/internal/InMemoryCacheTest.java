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

import org.jahia.features.cache.api.CacheConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jerome Blanchard
 */
public class InMemoryCacheTest {

    @Test
    void testPutAndGet() {
        CacheConfig config = CacheConfig.create().timeToLive(1).maxEntries(2).build();
        InMemoryCache<String> cache = new InMemoryCache<>("testCache", config);
        cache.put("key1", "value1");
        String value = cache.get("key1");
        assertNotNull(value);
        assertEquals("value1", value);
    }

    @Test
    void testDelete() {
        CacheConfig config = CacheConfig.create().timeToLive(1).maxEntries(2).build();
        InMemoryCache<String> cache = new InMemoryCache<>("testCache", config);
        cache.put("key1", "value1");
        cache.delete("key1");
        assertNull(cache.get("key1"));
    }

    @Test
    void testClear() {
        CacheConfig config = CacheConfig.create().timeToLive(1).maxEntries(2).build();
        InMemoryCache<String> cache = new InMemoryCache<>("testCache", config);
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.clear();
        assertEquals(0, cache.size());
    }

    @Test
    void testMaxEntriesEviction() {
        CacheConfig config = CacheConfig.create().timeToLive(100).maxEntries(2).build();
        InMemoryCache<String> cache = new InMemoryCache<>("testCache", config);
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3"); // Should evict key1
        assertNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));
        assertNotNull(cache.get("key3"));
    }

    @Test
    void testTTLExpiration() throws InterruptedException {
        CacheConfig config = CacheConfig.create().timeToLive(1).maxEntries(50).build();
        InMemoryCache<String> cache = new InMemoryCache<>("testCache", config);
        cache.put("key1", "value1");
        Thread.sleep(1100); // Wait for TTL to expire
        assertNull(cache.get("key1"));
    }
}
