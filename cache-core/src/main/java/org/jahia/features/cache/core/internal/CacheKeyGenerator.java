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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Generates cache keys based on method signatures and parameters.
 * Supports @CacheKey annotations to control which parameters are included.
 *
 * @author Jerome Blanchard
 */
public class CacheKeyGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheKeyGenerator.class);

    /**
     * Generates a cache key for the given method and arguments.
     * If @CacheKey annotations are present, only annotated parameters are used.
     * Otherwise, all parameters are included in the key.
     * If no parameters are present, the key is based on the class name.
     *
     * @param method the method being cached
     * @param args the method arguments
     * @param target the target object (for checking implementation annotations)
     * @return a unique cache key
     */
    public static String generate(Method method, Object[] args, Object target) {
        List<Object> keyComponents = new ArrayList<>();
        keyComponents.add(method.getDeclaringClass().getName());

        Parameter[] parameters = method.getParameters();
        Method implMethod = null;

        // Try to get the implementation method to check for @CacheKey annotations
        if (target != null) {
            try {
                implMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException e) {
                LOGGER.debug("Implementation method not found for {}", method.getName());
            }
        }

        boolean hasAnnotatedParams = false;

        // Check if any parameters have @CacheKey annotation (either on interface or implementation)
        for (int i = 0; i < parameters.length; i++) {
            if (hasParameterCacheKeyAnnotation(parameters[i], implMethod, i)) {
                hasAnnotatedParams = true;
                break;
            }
        }

        // If we have annotated parameters, only include those in the key
        if (hasAnnotatedParams) {
            for (int i = 0; i < parameters.length; i++) {
                if (hasParameterCacheKeyAnnotation(parameters[i], implMethod, i)) {
                    if (args != null && i < args.length) {
                        keyComponents.add(args[i]);
                    }
                }
            }
        } else {
            // Include all parameters
            if (args != null) {
                keyComponents.addAll(Arrays.asList(args));
            }
        }

        return generateHashKey(keyComponents);
    }

    /**
     * Backwards compatibility method that doesn't check implementation annotations
     */
    public static String generate(Method method, Object[] args) {
        return generate(method, args, null);
    }

    /**
     * Checks if a parameter has a specific annotation, looking at both interface and implementation
     */
    private static boolean hasParameterCacheKeyAnnotation(Parameter interfaceParam, Method implMethod, int paramIndex) {
        // Check interface parameter first
        if (interfaceParam.isAnnotationPresent(CacheKey.class)) {
            return true;
        }

        // Check implementation parameter if available
        if (implMethod != null) {
            Parameter[] implParams = implMethod.getParameters();
            if (paramIndex < implParams.length) {
                return implParams[paramIndex].isAnnotationPresent(CacheKey.class);
            }
        }

        return false;
    }

    /**
     * Generates a hash-based key from the components
     */
    private static String generateHashKey(List<Object> components) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();

            for (Object component : components) {
                if (component != null) {
                    sb.append(component).append("|");
                } else {
                    sb.append("null|");
                }
            }

            byte[] hash = md.digest(sb.toString().getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.warn("SHA-256 algorithm not available, falling back to simple hash", e);
            return String.valueOf(components.hashCode());
        }
    }
}
