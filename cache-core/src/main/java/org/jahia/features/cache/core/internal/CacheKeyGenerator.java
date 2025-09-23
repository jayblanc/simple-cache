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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jahia.features.cache.api.CacheKey;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * @author Jerome Blanchard
 */
public class CacheKeyGenerator {

    private static final ObjectMapper mapper = new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private CacheKeyGenerator() {}

    public static String generate(Method method, Object[] args) {
        try {
            String signature = method.getDeclaringClass().getName() + "." +
                    method.getName() + "(" +
                    String.join(",",
                            Arrays.stream(method.getParameterTypes())
                                    .map(Class::getName)
                                    .toArray(String[]::new)
                    ) + ")";

            Object[] keyParams = getKeyParameters(method, args);

            String paramsJson = mapper.writeValueAsString(keyParams);
            String rawKey = signature + ":" + paramsJson;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Cache key generation failed", e);
        }
    }

    /**
     * Determines which parameters to use for cache key generation.
     * If no parameter is annotated with @CacheKey, all parameters are used.
     * If at least one parameter is annotated with @CacheKey, only annotated parameters are used.
     *
     * @param method the method for which to generate the key
     * @param args the method arguments
     * @return the parameters to use for cache key generation
     */
    private static Object[] getKeyParameters(Method method, Object[] args) {
        if (args == null || args.length == 0) {
            return new Object[0];
        }

        Parameter[] parameters = method.getParameters();
        List<Object> keyParams = new ArrayList<>();
        boolean hasCacheKeyAnnotation = false;

        // Check if at least one parameter has the @CacheKey annotation
        for (Parameter parameter : parameters) {
            if (parameter.isAnnotationPresent(CacheKey.class)) {
                hasCacheKeyAnnotation = true;
                break;
            }
        }

        // If no @CacheKey annotation is present, use all parameters
        if (!hasCacheKeyAnnotation) {
            return args;
        }

        // If at least one @CacheKey annotation is present, use only annotated parameters
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(CacheKey.class)) {
                keyParams.add(args[i]);
            }
        }

        return keyParams.toArray();
    }
}
