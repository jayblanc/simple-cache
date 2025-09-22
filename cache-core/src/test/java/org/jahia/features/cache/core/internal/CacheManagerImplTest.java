package org.jahia.features.cache.core.internal;

import org.jahia.features.cache.api.*;
import org.jahia.features.cache.api.CacheAlreadyExistsException;
import org.jahia.features.cache.api.CacheNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CacheManagerImplTest {

    private CacheManager manager;

    @BeforeEach
    void setup() {
        this.manager = new CacheManagerImpl();
        DefaultCacheProvider provider = new DefaultCacheProvider();
        ((CacheManagerImpl) this.manager).addProvider(provider);
    }

    @Test
    void testMultipleCacheManagement() throws Exception {
        CacheConfig config1 = CacheConfig.create().timeToLive(10).maxEntries(10).build();
        CacheConfig config2 = CacheConfig.create().timeToLive(10).maxEntries(10).build();
        Cache<String> cache1 = manager.createCache("cache1", config1, String.class);
        Cache<String> cache2 = manager.createCache("cache2", config2, String.class);
        cache1.put("k1", "v1");
        cache2.put("k2", "v2");
        assertEquals("v1", cache1.get("k1"));
        assertEquals("v2", cache2.get("k2"));
        assertNull(cache1.get("k2"));
        assertNull(cache2.get("k1"));
        List<String> names = manager.listCacheNames();
        assertTrue(names.contains("cache1"));
        assertTrue(names.contains("cache2"));
        assertEquals(2, names.size());
    }

    @Test
    void testClearAndClearAll() throws Exception {
        CacheConfig config = CacheConfig.create().timeToLive(10).maxEntries(10).build();
        Cache<String> cacheA = manager.createCache("cacheA", config, String.class);
        Cache<String> cacheB = manager.createCache("cacheB", config, String.class);
        cacheA.put("a", "1");
        cacheB.put("b", "2");
        manager.clearCache("cacheA");
        assertNull(cacheA.get("a"));
        manager.clearAll();
        assertNull(cacheB.get("b"));
    }

    @Test
    void testExceptionHandling() throws Exception {
        CacheConfig config = CacheConfig.create().timeToLive(10).maxEntries(10).build();
        manager.createCache("cacheX", config, String.class);
        assertThrows(CacheAlreadyExistsException.class, () -> manager.createCache("cacheX", config, String.class));
        assertThrows(CacheNotFoundException.class, () -> manager.getCache("notfound", String.class));
        assertThrows(CacheNotFoundException.class, () -> manager.clearCache("notfound"));
    }
}
