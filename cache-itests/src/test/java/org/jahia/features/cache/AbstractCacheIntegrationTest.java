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

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.itests.KarafTestSupport;
import org.jahia.features.cache.api.CacheConfig;
import org.jahia.features.cache.api.CacheManager;
import org.jahia.features.cache.sample.ValueService;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.debugConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

/**
 * Base class for cache integration tests that provides common test scenarios.
 *
 * @author Jerome Blanchard
 */
public abstract class AbstractCacheIntegrationTest extends KarafTestSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCacheIntegrationTest.class);

    @Inject
    protected BundleContext bundleContext;

    @Inject
    protected FeaturesService featuresService;

    /**
     * Returns the feature name to install for this specific cache provider.
     */
    protected abstract String getFeatureName();

    /**
     * Returns the expected provider name for this cache implementation.
     */
    protected abstract String getExpectedProviderName();

    protected org.ops4j.pax.exam.Option[] createBaseConfig() {
        List<org.ops4j.pax.exam.Option> options = new LinkedList<>();
        options.add(editConfigurationFilePut("etc/org.ops4j.pax.logging.cfg","log4j2.logger.cache.name","org.jahia.features.cache"));
        options.add(editConfigurationFilePut("etc/org.ops4j.pax.logging.cfg","log4j2.logger.cache.level","INFO"));
        options.add(editConfigurationFilePut("etc/org.ops4j.pax.logging.cfg","log4j2.logger.cache-core.name","org.jahia.features.cache.core.internal"));
        options.add(editConfigurationFilePut("etc/org.ops4j.pax.logging.cfg","log4j2.logger.cache-core.level","DEBUG"));
        options.add(editConfigurationFilePut("etc/org.ops4j.pax.logging.cfg","log4j2.logger.cache-sample.name","org.jahia.features.cache.sample"));
        options.add(editConfigurationFilePut("etc/org.ops4j.pax.logging.cfg","log4j2.logger.cache-sample.level","DEBUG"));

        String karafDebug = System.getProperty("it.karaf.debug");
        if (karafDebug != null) {
            LOGGER.warn("Found system Karaf Debug system property, activating configuration: {}", karafDebug);
            String port = "5006";
            boolean hold = true;
            if (!karafDebug.trim().isEmpty()) {
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
        return Stream.concat(Stream.of(super.config()), options.stream()).toArray(org.ops4j.pax.exam.Option[]::new);
    }

    @Test
    public void testCacheService() throws Exception {
        LOGGER.info("Testing Cache service with provider: {}", getExpectedProviderName());
        Feature[] featuresBefore = featuresService.listInstalledFeatures();

        if (getFeatureName() != null) {
            addFeaturesRepository(
                    maven("org.jahia.features.cache", "cache-features")
                            .type("xml")
                            .classifier("features")
                            .version("1.0.0-SNAPSHOT")
                            .getURL());
            installAndAssertFeature(getFeatureName());
        }

        // Check that bundle exists and retrieve the service
        assertCacheBundleExists();
        ServiceReference<?> cacheServiceRef = bundleContext.getServiceReference(CacheManager.class.getName());
        assertNotNull("CacheService is NOT available", cacheServiceRef);
        CacheManager cacheManager = (CacheManager) bundleContext.getService(cacheServiceRef);
        assertNotNull("Unable to get CacheService reference", cacheManager);
        cacheManager.clearAll();

        // Test provider name
        assertEquals("Wrong cache provider name", getExpectedProviderName(), cacheManager.getCacheProviderName());
        LOGGER.info("Cache provider name verified: {}", cacheManager.getCacheProviderName());

        int cacheCount = cacheManager.listCacheNames().size();
        LOGGER.info("1. Initial caches list size {}", cacheCount);
        assertEquals("CacheService should not have any cache at this point", 0, cacheCount);

        // Test a programmatic cache creation
        String testCacheName = "testCache";
        LOGGER.info("2. Create a cache {} using programing API", testCacheName);
        cacheManager.createCache(testCacheName, CacheConfig.create().timeToLive(10).maxEntries(10).build(), String.class);
        assertTrue("Cache " + testCacheName + " not found in cache service", cacheManager.listCacheNames().contains(testCacheName));
        cacheCount = cacheManager.listCacheNames().size();
        LOGGER.info("2.1. After create cache list size {} (should be 1)", cacheCount);
        assertEquals("Cache service should have one cache at this point", 1, cacheCount);

        // Install a sample bundle with a service having some methods annotated with @CacheResult
        LOGGER.info("3. Install the sample bundle with a service using @CacheResult annotation ...");
        Bundle basicCacheBundle = bundleContext.installBundle(maven("org.jahia.features.cache", "cache-basic-sample").type("jar").version("1.0.0-SNAPSHOT").getURL());
        assertNotNull("Unable to install basic cache sample bundle", basicCacheBundle);
        basicCacheBundle.start();
        LOGGER.info("3.1. Bundle cache-basic-sample installed. State: {}", getBundleStateAsString(basicCacheBundle.getState()));
        assertEquals("Bundle is not in ACTIVE state", Bundle.ACTIVE, basicCacheBundle.getState());
        ServiceReference<?>[] refs = bundleContext.getServiceReferences("org.jahia.features.cache.sample.ValueService", null);
        assertNotNull("No service references found for ValueService", refs);
        LOGGER.info("3.2. Looking for ValueService references to check if proxy one is the highest ranking one...");
        ServiceReference<?>[] allRefs = bundleContext.getAllServiceReferences(null, null);
        if (allRefs != null) {
            for (ServiceReference<?> ref : allRefs) {
                String[] classes = (String[]) ref.getProperty(Constants.OBJECTCLASS);
                for (String clazz : classes) {
                    if (clazz.contains("ValueService")) {
                        LOGGER.info("3.3. Found ValueService: {} with ranking: {}", clazz, ref.getProperty(Constants.SERVICE_RANKING));
                    }
                }
            }
        }
        ServiceReference<?> valueServiceRef = refs[0];
        int highestRanking = Integer.MIN_VALUE;
        for (ServiceReference<?> ref : refs) {
            Object rankingObj = ref.getProperty(Constants.SERVICE_RANKING);
            int ranking = rankingObj instanceof Integer ? (Integer) rankingObj : 0;
            if (ranking > highestRanking) {
                highestRanking = ranking;
                valueServiceRef = ref;
            }
        }
        ValueService valueService = (ValueService) bundleContext.getService(valueServiceRef);
        assertNotNull("Unable to get ValueService proxy", valueService);

        // Test the use of service methods to check that the cache proxy is working as expected
        LOGGER.info("4. Call the sample service method getValue() and check the cache is used as expected...");
        String value1 = valueService.getValue("myKey");
        cacheCount = cacheManager.listCacheNames().size();
        LOGGER.info("4.1. After first method call caches list size {} (should be 2)", cacheCount);
        assertEquals("CacheService should have a new cache at this point", 2, cacheCount);
        LOGGER.info("4.2. Sample service getValue first call value:  {}", value1);
        String value2 = valueService.getValue("myKey");
        LOGGER.info("4.3. Sample service getValue second call value: {} (should be the same as first call)", value2);
        assertEquals("Value should be the same (taken from cache)", value1, value2);
        LOGGER.info("4.4. Update Sample service value to:  plop");
        valueService.updateValue("myKey", "plop");
        String value3 = valueService.getValue("myKey");
        LOGGER.info("4.5. Sample service getValue third call value: {} (should have been evicted by update)", value3);
        assertTrue("Value should contains plop", value3.contains("plop"));
        assertNotEquals("Value should not be the same (invalidated)", value1, value3);
        LOGGER.info("4.6. Clear all values");
        valueService.clearAllValues();
        String value4 = valueService.getValue("myKey");
        LOGGER.info("4.7. Sample service getValue fourth call value: {} (should have been purge)", value3);
        assertFalse("Value should NO MORE contains plop", value4.contains("plop"));
        assertNotEquals("Value should not be the same (purged)", value3, value4);
        assertNotEquals("Value should not be the same (purged)", value1, value4);

        // Uninstall the sample bundle and check that the cache has been purged (not implemented yet)
        LOGGER.info("5. Uninstall the sample bundle ...");
        bundleContext.ungetService(valueServiceRef);
        basicCacheBundle.stop();
        basicCacheBundle.uninstall();
        LOGGER.info("5.1. Basic Cache Sample uninstalled, wait a little bit and check cache list size...");
        Thread.sleep(500);
        cacheCount = cacheManager.listCacheNames().size();
        LOGGER.info("5.2. After uninstall bundle caches list size {} (should be one but not implemented yet)", cacheCount);
        assertEquals("CacheService should have two caches at this point because purge of cache name is not implemented yet", 2, cacheCount);
        bundleContext.ungetService(cacheServiceRef);

        // Uninstall the feature.
        LOGGER.info("6. Uninstall the feature");
        uninstallNewFeatures(Set.of(featuresBefore));
    }

    protected void assertCacheBundleExists() {
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

    protected String getBundleStateAsString(int state) {
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
