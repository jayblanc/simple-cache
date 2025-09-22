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

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jerome Blanchard
 */
public class InfinispanCacheTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(InfinispanCacheTest.class);
    private static InfinispanCacheProvider provider;

    @BeforeAll
    public static void setUp() {
        provider = new InfinispanCacheProvider();
    }

    @AfterAll
    public static void tearDown() {
        provider.deactivate();
    }

    @Test
    public void testInfinispanProviderLifecycle() {
        assertNull(provider.getEmbeddedCacheManager(), "Cache manager should be null before activation");

        LOGGER.info("Activating Infinispan Cache Provider...");
        provider.activate();
        EmbeddedCacheManager cacheManager = provider.getEmbeddedCacheManager();

        assertTrue(provider.isAvailable(), "Cache manager should be available after activation");
        assertEquals("infinispan", provider.getProviderName(), "Infinispan provider should have name 'infinispan'");
        assertEquals(10, provider.getPriority(), "Infinispan provider should have priority 10");

        assertNotNull(cacheManager, "Cache manager should not be null after activation");
        assertTrue(cacheManager.isCoordinator(), "Cache manager should be coordinator in a single node setup");
        assertTrue(cacheManager.isDefaultRunning(), "Cache manager default cache should be running after activation");

        try {
            Cache<Object, Object> cache = cacheManager.getCache();
            cache.put("key", "value");
            assertEquals(1 , cache.size(), "Infinispan cache should have one entry");
            assertEquals("value", cache.get("key"), "Infinispan cache should have the key entry");
        } catch (Exception e) {
            fail("Should be able to get a cache from the manager: " + e.getMessage());
        }

        LOGGER.info("Deactivating Infinispan Cache Provider...");
        provider.deactivate();

        cacheManager = provider.getEmbeddedCacheManager();
        assertFalse(cacheManager.isDefaultRunning(), "Cache manager should be stopped after deactivation");
    }

}
