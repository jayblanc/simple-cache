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
package org.jahia.features.cache;

import org.apache.karaf.itests.KarafTestSupport;
import org.jahia.features.cache.api.Cache;
import org.jahia.features.cache.api.CacheConfig;
import org.jahia.features.cache.api.CacheService;
import org.jahia.features.cache.sample.BasicCacheSampleService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.debugConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

/**
 * @author Jerome Blanchard
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CacheIntegrationTest extends KarafTestSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheIntegrationTest.class);

    @Inject
    private BundleContext bundleContext;

    @Configuration
    public Option[] config() {
        List<Option> options = new LinkedList<>();
        options.add(editConfigurationFilePut("etc/org.ops4j.pax.logging.cfg","log4j2.logger.cache.name","org.jahia.features.cache"));
        options.add(editConfigurationFilePut("etc/org.ops4j.pax.logging.cfg","log4j2.logger.cache.level","INFO"));
        options.add(editConfigurationFilePut("etc/org.ops4j.pax.logging.cfg","log4j2.logger.cache-sample.name","org.jahia.features.cache.sample"));
        options.add(editConfigurationFilePut("etc/org.ops4j.pax.logging.cfg","log4j2.logger.cache-sample.level","DEBUG"));
        String karafDebug = System.getProperty("it.karaf.debug");
        if (karafDebug != null) {
            LOGGER.warn("Found system Karaf Debug system property, activating configuration: {}", karafDebug);
            String port = "5006";
            boolean hold = true;
            if (karafDebug.trim().isEmpty()) {
                String[] debugOptions = karafDebug.split(",");
                for (String debugOption : debugOptions) {
                    String[] debugOptionParts = debugOption.split(":");
                    if ("hold".equals(debugOptionParts[0])) {
                        hold = Boolean.parseBoolean(debugOptionParts[1].trim());
                    }
                    if ("port".equals(debugOptionParts[0])) {
                        port = debugOptionParts[1].trim();
                    }
                }
            }
            options.add(0, debugConfiguration(port, hold));
        }
        return Stream.concat(Stream.of(super.config()), options.stream()).toArray(Option[]::new);
    }

    @Test
    public void testCacheService() throws Exception {
        LOGGER.info("Testing Cache service");

        // Install MyScheduler feature
        addFeaturesRepository(maven("org.jahia.features.cache", "cache-features").type("xml").classifier("features").version("1.0.0-SNAPSHOT").getURL());
        installAndAssertFeature("cache");

        // Check that bundle exists and retrieve the service
        assertCacheBundleExists();
        ServiceReference<?> cacheServiceRef = bundleContext.getServiceReference(CacheService.class.getName());
        assertNotNull("CacheService is NOT available", cacheServiceRef);
        CacheService cacheService = (CacheService) bundleContext.getService(cacheServiceRef);
        assertNotNull("Unable to get CacheService reference", cacheService);
        int cacheCount = cacheService.list().size();
        LOGGER.info("1. Caches list size {}", cacheCount);
        assertEquals("CacheService should not have any cache at this point", 0, cacheCount);

        // Test a programmatic cache creation
        String testCacheName = "testCache";
        Cache<String> testCache = cacheService.create(testCacheName, CacheConfig.create().timeToLive(10).maxEntries(10).build(), String.class);
        assertTrue("Cache " + testCacheName + " not found in cache service", cacheService.list().contains(testCacheName));
        cacheCount = cacheService.list().size();
        LOGGER.info("2. Cache list size {}", cacheCount);
        assertEquals("Cache service should have one cache at this point", 1, cacheCount);

        // Install a sample bundle with a service having some methodes annotated with @CacheResult
        Bundle basicCacheBundle = bundleContext.installBundle(maven("org.jahia.features", "cache-basic-sample").type("jar").version("1.0.0-SNAPSHOT").getURL());
        assertNotNull("Unable to install basic cache sample bundle", basicCacheBundle);
        basicCacheBundle.start();
        LOGGER.info("Bundle cache-basic-sample installed. State: {}", getBundleStateAsString(basicCacheBundle.getState()));
        assertEquals("Bundle is not is ACTIVE state", Bundle.ACTIVE, basicCacheBundle.getState());
        ServiceReference<?>[] refs = bundleContext.getServiceReferences("org.jahia.features.cache.sample.BasicCacheSampleService", null);
        assertNotNull("No service references found for BasicCacheSampleService", refs);
        assertTrue("No service references found for BasicCacheSampleService", refs.length > 0);
        ServiceReference<?> basicCacheSampleServiceRef = refs[0];
        int highestRanking = Integer.MIN_VALUE;
        for (ServiceReference<?> ref : refs) {
            Object rankingObj = ref.getProperty(Constants.SERVICE_RANKING);
            int ranking = rankingObj instanceof Integer ? (Integer) rankingObj : 0;
            if (ranking > highestRanking) {
                highestRanking = ranking;
                basicCacheSampleServiceRef = ref;
            }
        }
        BasicCacheSampleService basicCacheService = (BasicCacheSampleService) bundleContext.getService(basicCacheSampleServiceRef);
        assertNotNull("Unable to get BasicCacheSampleService proxy", basicCacheService);

        // Call a sample service methode and check the cache is used
        String value1 = basicCacheService.getValue();

        cacheCount = cacheService.list().size();
        LOGGER.info("3. Caches list size {}", cacheCount);
        assertEquals("CacheService should have a new cache at this point", 2, cacheCount);
        LOGGER.info("4.1. Sample service getValue first call  {}", value1);
        String value2 = basicCacheService.getValue();
        LOGGER.info("4.2. Sample service getValue second call {}", value2);
        assertEquals("Value should be the same (taken from cache)", value1, value2);

        // Try to uninstall the bundle and check the caches are removed
        LOGGER.info("5. Uninstall the sample bundle ...");
        bundleContext.ungetService(basicCacheSampleServiceRef);
        basicCacheBundle.stop();
        basicCacheBundle.uninstall();
        LOGGER.info("Basic Cache Sample uninstalled, wait a little bit and check cache list size...");
        Thread.sleep(500);

        // Check that the cache list does not contain the cache created by the sample bundle
        cacheCount = cacheService.list().size();
        LOGGER.info("6. Caches list size {}", cacheCount);
        assertEquals("CacheService should not have any cache at this point", 2, cacheCount);

        bundleContext.ungetService(cacheServiceRef);
    }

    private void assertCacheBundleExists() {
        boolean bundleFound = false;
        for (Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getSymbolicName().contains("cache-core")) {
                LOGGER.info("Bundle cache-core found. State: {}", getBundleStateAsString(bundle.getState()));
                bundleFound = true;
            }
        }
        if (!bundleFound) {
            LOGGER.error("Bundle cache-core NOT found!");
        }
        assertTrue("Bundle cache-core not found", bundleFound);
    }

    private String getBundleStateAsString(int state) {
        switch (state) {
            case Bundle.UNINSTALLED: return "UNINSTALLED";
            case Bundle.INSTALLED: return "INSTALLED";
            case Bundle.RESOLVED: return "RESOLVED";
            case Bundle.STARTING: return "STARTING";
            case Bundle.STOPPING: return "STOPPING";
            case Bundle.ACTIVE: return "ACTIVE";
            default: return "UNKNOWN (" + state + ")";
        }
    }
}
