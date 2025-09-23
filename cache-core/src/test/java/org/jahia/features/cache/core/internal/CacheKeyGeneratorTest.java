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

import org.jahia.features.cache.api.CacheKey;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for CacheKeyGenerator
 *
 * @author Jerome Blanchard
 */
public class CacheKeyGeneratorTest {

    public void methodWithoutParameter1() {}

    public void methodWithoutParameter2() {}

    public void methodWithoutAnnotation(String param1, String param2) {}

    public void methodWithPartialAnnotation(@CacheKey String param1, String param2) {}

    public void methodWithAllAnnotated(@CacheKey String param1, @CacheKey String param2) {}

    public void methodWithMixedAnnotations(@CacheKey String param1, String param2, @CacheKey int param3, boolean param4) {}

    @Test
    public void testGenerateKeyWithoutParameter() throws Exception {
        Method method1 = CacheKeyGeneratorTest.class.getMethod("methodWithoutParameter1");
        Method method2 = CacheKeyGeneratorTest.class.getMethod("methodWithoutParameter2");
        Object[] args = {};

        String key1 = CacheKeyGenerator.generate(method1, args);
        String key2 = CacheKeyGenerator.generate(method2, args);

        // Keys must be identical for different methods without parameters
        assertEquals(key1, key2);
    }

    @Test
    public void testGenerateKeyWithoutAnnotations() throws Exception {
        Method method = CacheKeyGeneratorTest.class.getMethod("methodWithoutAnnotation", String.class, String.class);
        Object[] args = {"value1", "value2"};

        String key1 = CacheKeyGenerator.generate(method, args);
        String key2 = CacheKeyGenerator.generate(method, args);

        // Keys must be identical for the same parameters
        assertEquals(key1, key2);

        // Changing a parameter must produce a different key
        Object[] differentArgs = {"value1", "different"};
        String key3 = CacheKeyGenerator.generate(method, differentArgs);
        assertNotEquals(key1, key3);
    }

    @Test
    public void testGenerateKeyWithPartialAnnotation() throws Exception {
        Method method = CacheKeyGeneratorTest.class.getMethod("methodWithPartialAnnotation", String.class, String.class);
        Object[] args = {"annotated", "notAnnotated"};

        String key1 = CacheKeyGenerator.generate(method, args);

        // Changing the non-annotated parameter should not affect the key
        Object[] argsWithDifferentNonAnnotated = {"annotated", "differentNotAnnotated"};
        String key2 = CacheKeyGenerator.generate(method, argsWithDifferentNonAnnotated);
        assertEquals(key1, key2);

        // Changing the annotated parameter should affect the key
        Object[] argsWithDifferentAnnotated = {"differentAnnotated", "notAnnotated"};
        String key3 = CacheKeyGenerator.generate(method, argsWithDifferentAnnotated);
        assertNotEquals(key1, key3);
    }

    @Test
    public void testGenerateKeyWithAllAnnotated() throws Exception {
        Method method = CacheKeyGeneratorTest.class.getMethod("methodWithAllAnnotated", String.class, String.class);
        Object[] args = {"value1", "value2"};

        String key1 = CacheKeyGenerator.generate(method, args);

        // Changing any parameter should affect the key
        Object[] argsWithDifferentFirst = {"different", "value2"};
        String key2 = CacheKeyGenerator.generate(method, argsWithDifferentFirst);
        assertNotEquals(key1, key2);

        Object[] argsWithDifferentSecond = {"value1", "different"};
        String key3 = CacheKeyGenerator.generate(method, argsWithDifferentSecond);
        assertNotEquals(key1, key3);
    }

    @Test
    public void testGenerateKeyWithMixedAnnotations() throws Exception {
        Method method = CacheKeyGeneratorTest.class.getMethod("methodWithMixedAnnotations", String.class, String.class, int.class, boolean.class);
        Object[] args = {"annotated1", "notAnnotated", 42, false};

        String key1 = CacheKeyGenerator.generate(method, args);

        // Changing non-annotated parameters should not affect the key
        Object[] argsWithDifferentNonAnnotated = {"annotated1", "differentNotAnnotated", 42, true};
        String key2 = CacheKeyGenerator.generate(method, argsWithDifferentNonAnnotated);
        assertEquals(key1, key2);

        // Changing the first annotated parameter should affect the key
        Object[] argsWithDifferentFirst = {"differentAnnotated", "notAnnotated", 42, false};
        String key3 = CacheKeyGenerator.generate(method, argsWithDifferentFirst);
        assertNotEquals(key1, key3);

        // Changing the third annotated parameter should affect the key
        Object[] argsWithDifferentThird = {"annotated1", "notAnnotated", 99, false};
        String key4 = CacheKeyGenerator.generate(method, argsWithDifferentThird);
        assertNotEquals(key1, key4);
    }

    @Test
    public void testGenerateKeyWithNullArgs() throws Exception {
        Method method = CacheKeyGeneratorTest.class.getMethod("methodWithoutAnnotation", String.class, String.class);

        String key1 = CacheKeyGenerator.generate(method, null);
        String key2 = CacheKeyGenerator.generate(method, new Object[0]);

        // Both should produce valid keys
        assertNotNull(key1);
        assertNotNull(key2);
        assertEquals(key1, key2);
    }

    @Test
    public void testGenerateKeyDeterministic() throws Exception {
        Method method = CacheKeyGeneratorTest.class.getMethod("methodWithPartialAnnotation", String.class, String.class);
        Object[] args = {"test", "ignored"};

        // Generating the same key multiple times should give the same result
        String key1 = CacheKeyGenerator.generate(method, args);
        String key2 = CacheKeyGenerator.generate(method, args);
        String key3 = CacheKeyGenerator.generate(method, args);

        assertEquals(key1, key2);
        assertEquals(key2, key3);
    }
}
