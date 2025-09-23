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

import org.jahia.features.cache.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Proxy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for cache invalidation functionality
 * @author Jerome Blanchard
 */
public class CacheInterceptorTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache<Object> cacheInterface;

    @Mock
    private Cache<Object> cacheBean;

    private TestServiceAnnotatedInterface testServiceAnnotatedInterface;
    private TestServiceAnnotatedInterface proxyTestServiceAnnotatedInterface;

    private TestServiceAnnotatedBean testServiceAnnotatedBean;
    private TestServiceAnnotatedBean proxyTestServiceAnnotatedBean;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        testServiceAnnotatedInterface = new TestServiceAnnotatedInterfaceImpl();
        testServiceAnnotatedBean = new TestServiceAnnotatedBeanImpl();

        when(cacheManager.getCache(eq("test-cache-interface"), eq(Object.class))).thenReturn(cacheInterface);
        when(cacheManager.getCache(eq("test-cache-bean"), eq(Object.class))).thenReturn(cacheBean);

        proxyTestServiceAnnotatedInterface = (TestServiceAnnotatedInterface) Proxy.newProxyInstance(
            TestServiceAnnotatedInterface.class.getClassLoader(),
            new Class[]{ TestServiceAnnotatedInterface.class},
            new CacheInterceptor(testServiceAnnotatedInterface, cacheManager)
        );
        proxyTestServiceAnnotatedBean = (TestServiceAnnotatedBean) Proxy.newProxyInstance(
            TestServiceAnnotatedBean.class.getClassLoader(),
            new Class[]{ TestServiceAnnotatedBean.class},
            new CacheInterceptor(testServiceAnnotatedBean, cacheManager)
        );
    }

    @Test
    void testCacheInvalidate() throws Exception {
        // Test that @CacheInvalidate calls evict with the correct key when annotation is on interface
        proxyTestServiceAnnotatedInterface.invalidateValue("test-key");
        String expectedGeneratedKeyInterface = CacheKeyGenerator.generate(
            TestServiceAnnotatedInterface.class.getMethod("invalidateValue", String.class),
            new Object[]{"test-key"}, testServiceAnnotatedInterface
        );

        verify(cacheManager).getCache("test-cache-interface", Object.class);
        verify(cacheInterface).delete(eq(expectedGeneratedKeyInterface));

        // Test that @CacheInvalidate calls evict with the correct key when annotation is on bean implementation
        proxyTestServiceAnnotatedBean.invalidateValue("test-key");
        String expectedGeneratedKeyBean = CacheKeyGenerator.generate(
            TestServiceAnnotatedBean.class.getMethod("invalidateValue", String.class),
            new Object[]{"test-key"}, testServiceAnnotatedBean
        );

        verify(cacheManager).getCache("test-cache-bean", Object.class);
        verify(cacheBean).delete(eq(expectedGeneratedKeyBean));
    }

    @Test
    void testCacheInvalidateAll() throws Exception {
        // Test that @CacheInvalidateAll calls clear on the cache
        proxyTestServiceAnnotatedInterface.clearAllValues();

        verify(cacheManager).getCache("test-cache-interface", Object.class);
        verify(cacheInterface).clear();

        proxyTestServiceAnnotatedBean.clearAllValues();

        verify(cacheManager).getCache("test-cache-bean", Object.class);
        verify(cacheBean).clear();
    }

    @Test
    void testCacheResult() throws Exception {
        when(cacheInterface.get(any(String.class))).thenReturn(null);
        when(cacheBean.get(any(String.class))).thenReturn(null);

        String resultInterface = proxyTestServiceAnnotatedInterface.getValue("test-key");
        String expectedGeneratedKeyInterface = CacheKeyGenerator.generate(
            TestServiceAnnotatedInterface.class.getMethod("getValue", String.class),
            new Object[]{"test-key"}, testServiceAnnotatedInterface
        );

        verify(cacheManager).getCache("test-cache-interface", Object.class);
        verify(cacheInterface).get(eq(expectedGeneratedKeyInterface));
        verify(cacheInterface).put(eq(expectedGeneratedKeyInterface), eq(resultInterface));

        String resultBean = proxyTestServiceAnnotatedBean.getValue("test-key");
        String expectedGeneratedKeyBean = CacheKeyGenerator.generate(
            TestServiceAnnotatedBean.class.getMethod("getValue", String.class),
            new Object[]{"test-key"}, testServiceAnnotatedBean
        );

        verify(cacheManager).getCache("test-cache-bean", Object.class);
        verify(cacheBean).get(eq(expectedGeneratedKeyBean));
        verify(cacheBean).put(eq(expectedGeneratedKeyBean), eq(resultBean));
    }

    public interface TestServiceAnnotatedInterface {
        @CacheResult(cacheName = "test-cache-interface")
        String getValue(String key);

        @CacheInvalidate(cacheName = "test-cache-interface")
        void invalidateValue(String key);

        @CacheInvalidateAll(cacheName = "test-cache-interface")
        void clearAllValues();
    }

    public static class TestServiceAnnotatedInterfaceImpl implements TestServiceAnnotatedInterface {
        @Override
        public String getValue(String key) {
            return "value-for-" + key;
        }

        @Override
        public void invalidateValue(String key) {
        }

        @Override
        public void clearAllValues() {
        }
    }

    public interface TestServiceAnnotatedBean {
        String getValue(String key);

        void invalidateValue(String key);

        void clearAllValues();
    }

    public static class TestServiceAnnotatedBeanImpl implements TestServiceAnnotatedBean {
        @Override
        @CacheResult(cacheName = "test-cache-bean")
        public String getValue(String key) {
            return "value-for-" + key;
        }

        @Override
        @CacheInvalidate(cacheName = "test-cache-bean")
        public void invalidateValue(String key) {
        }

        @Override
        @CacheInvalidateAll(cacheName = "test-cache-bean")
        public void clearAllValues() {
        }
    }
}
