package org.jahia.features.cache.core.internal;

import org.jahia.features.cache.api.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CacheServiceImplTest {
    @Test
    void testMultipleCacheManagement() throws Exception {
        CacheServiceImpl service = new CacheServiceImpl();
        CacheConfig config1 = CacheConfig.create().timeToLive(10).maxEntries(10).build();
        CacheConfig config2 = CacheConfig.create().timeToLive(10).maxEntries(10).build();
        Cache<String> cache1 = service.create("cache1", config1, String.class);
        Cache<String> cache2 = service.create("cache2", config2, String.class);
        cache1.put("k1", "v1");
        cache2.put("k2", "v2");
        assertEquals("v1", cache1.get("k1"));
        assertEquals("v2", cache2.get("k2"));
        assertNull(cache1.get("k2"));
        assertNull(cache2.get("k1"));
        List<String> names = service.list();
        assertTrue(names.contains("cache1"));
        assertTrue(names.contains("cache2"));
        assertEquals(2, names.size());
    }

    @Test
    void testClearAndClearAll() throws Exception {
        CacheServiceImpl service = new CacheServiceImpl();
        CacheConfig config = CacheConfig.create().timeToLive(10).maxEntries(10).build();
        Cache<String> cacheA = service.create("cacheA", config, String.class);
        Cache<String> cacheB = service.create("cacheB", config, String.class);
        cacheA.put("a", "1");
        cacheB.put("b", "2");
        service.clear("cacheA");
        assertNull(cacheA.get("a"));
        service.clearAll();
        assertNull(cacheB.get("b"));
    }

    @Test
    void testExceptionHandling() throws Exception {
        CacheServiceImpl service = new CacheServiceImpl();
        CacheConfig config = CacheConfig.create().timeToLive(10).maxEntries(10).build();
        service.create("cacheX", config, String.class);
        assertThrows(CacheAlreadyExistsException.class, () -> service.create("cacheX", config, String.class));
        assertThrows(CacheNotFoundException.class, () -> service.get("notfound", String.class));
        assertThrows(CacheNotFoundException.class, () -> service.clear("notfound"));
    }
}
