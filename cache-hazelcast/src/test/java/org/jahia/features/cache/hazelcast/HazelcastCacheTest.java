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

import org.jahia.features.cache.api.Cache;
import org.jahia.features.cache.api.CacheConfig;
import org.jahia.features.cache.api.CacheEntry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jerome Blanchard
 */
public class HazelcastCacheTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(HazelcastCacheTest.class);
    private static HazelcastCacheProvider provider;

    @BeforeAll
    public static void setUp() {
        provider = new HazelcastCacheProvider();
    }

    @AfterAll
    public static void tearDown() {
        if (provider != null) {
            provider.deactivate();
        }
    }

    @Test
    public void testHazelcastProviderLifecycle() {
        assertNull(provider.getHazelcastInstance(), "Cache manager should be null before activation");
        assertFalse(provider.isAvailable(), "Provider should not be available before activation");

        LOGGER.info("Activating Hazelcast Cache Provider...");
        provider.activate();

        assertTrue(provider.isAvailable(), "Provider should be available after activation");
        assertNotNull(provider.getHazelcastInstance(), "Cache manager should NOT be null after activation");
        assertTrue(provider.isAvailable(), "Provider should be available after activation");
        assertEquals("hazelcast", provider.getProviderName(), "Hazelcast provider should have name 'hazelcast'");
        assertEquals(10, provider.getPriority(), "Hazelcast provider should have priority 10");

        // Test cache creation and operations
        CacheConfig config = CacheConfig.create().maxEntries(100).timeToLive(300).build();
        Cache<String> cache = provider.createCache("test-cache", config, String.class);

        assertNotNull(cache, "Cache should be created successfully");
        assertEquals("test-cache", cache.getName(), "Cache should have correct name");
        assertEquals(config, cache.getConfig(), "Cache should have correct config");

        // Test cache operations
        assertEquals(0, cache.size(), "Cache should be empty initially");

        CacheEntry<String> putResult = cache.put("key1", "value1");
        assertNull(putResult, "Previous value should not exist");
        assertEquals(1, cache.size(), "Cache should have one entry after put");

        String value = cache.get("key1");
        assertEquals("value1", value, "Cache should return correct value");

        CacheEntry<String> entry = cache.getEntry("key1");
        assertNotNull(entry, "Cache entry should not be null");
        assertEquals("key1", entry.key(), "Entry should have correct key");
        assertEquals("value1", entry.value(), "Entry should have correct value");
        assertTrue(entry.created() > 0, "Entry should have creation timestamp");
        assertTrue(entry.accessed() > 0, "Entry should have access timestamp");

        CacheEntry<String> deleteResult = cache.delete("key1");
        assertNotNull(deleteResult, "Delete should return the removed entry");
        assertEquals(0, cache.size(), "Cache should be empty after delete");

        // Test with multiple entries
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");
        assertEquals(3, cache.size(), "Cache should have three entries");

        cache.clear();
        assertEquals(0, cache.size(), "Cache should be empty after clear");

        LOGGER.info("Deactivating Hazelcast Cache Provider...");
        provider.deactivate();

        assertFalse(provider.isAvailable(), "Provider should not be available after deactivation");
    }

}
