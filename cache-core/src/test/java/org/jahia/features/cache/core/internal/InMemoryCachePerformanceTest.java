package org.jahia.features.cache.core.internal;

import org.jahia.features.cache.api.CacheConfig;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class InMemoryCachePerformanceTest {

    private static final Logger LOGGER = Logger.getLogger(InMemoryCachePerformanceTest.class.getName());

    private static final int THREAD_COUNT = 50;
    private static final int OPERATIONS_PER_THREAD = 10000;
    private static final int BANDWIDTH_OPERATIONS = 100000;

    private final CacheConfig config = CacheConfig.create().timeToLive(10).maxEntries(10000).build();

    @Test
    void testConcurrentPerformance() throws InterruptedException {
        InMemoryCache<String> cache = new InMemoryCache<>("perfCache", config);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        AtomicInteger ops = new AtomicInteger();
        long start = System.nanoTime();
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                    String key = Thread.currentThread().getId() + "-" + j;
                    cache.put(key, "value" + j);
                    cache.get(key);
                    cache.delete(key);
                    ops.addAndGet(3);
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.MINUTES);
        long end = System.nanoTime();
        double durationSec = (end - start) / 1_000_000_000.0;
        double opsPerSec = ops.get() / durationSec;
        LOGGER.info("[Concurrent] Total operations: " + ops.get() + ", Duration: " + durationSec + "s, Ops/sec: " + opsPerSec);
    }

    @Test
    void testCacheBandwidth() {
        InMemoryCache<String> cache = new InMemoryCache<>("bandwidthCache", config);
        long start = System.nanoTime();
        for (int i = 0; i < BANDWIDTH_OPERATIONS; i++) {
            String key = "key" + i;
            cache.put(key, "value" + i);
            cache.get(key);
        }
        long end = System.nanoTime();
        double durationSec = (end - start) / 1_000_000_000.0;
        double opsPerSec = (2.0 * BANDWIDTH_OPERATIONS) / durationSec;
        LOGGER.info("[Bandwidth] Total operations: " + (2 * BANDWIDTH_OPERATIONS) + ", Duration: " + durationSec + "s, Ops/sec: " + opsPerSec);
    }
}

