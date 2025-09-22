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
package org.jahia.features.cache.api;

import java.beans.Transient;
import java.io.Serial;
import java.io.Serializable;

/**
 * Cache entry implementation suitable for all cache systems.
 * This class is serializable and tracks creation and access timestamps.
 *
 * @author Jerome Blanchard
 */
public class CacheEntry<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String key;
    private final T value;
    private final long created;
    private long accessed;

    /**
     * Creates a new cache entry with the given key and value.
     * Sets creation and access timestamps to current time.
     *
     * @param key the cache key
     * @param value the cached value
     */
    public CacheEntry(String key, T value) {
        this.key = key;
        this.value = value;
        this.created = System.currentTimeMillis();
        this.accessed = this.created;
    }

    /**
     * Returns the cache key.
     *
     * @return the key
     */
    public String key() {
        return key;
    }

    /**
     * Returns the cached value.
     *
     * @return the value
     */
    public T value() {
        return value;
    }

    /**
     * Returns the creation timestamp.
     *
     * @return creation time in milliseconds
     */
    public long created() {
        return created;
    }

    /**
     * Returns the last access timestamp.
     *
     * @return last access time in milliseconds
     */
    public long accessed() {
        return accessed;
    }

    /**
     * Updates the access timestamp to the current time.
     * This method should be called when the entry is accessed.
     */
    @Transient
    public void touch() {
        accessed = System.currentTimeMillis();
    }

    @Override
    @Transient
    public String toString() {
        return "CacheEntry{" +
               "key='" + key + '\'' +
               ", class=" + (value != null ? value.getClass().getName() : "null") +
               ", created=" + created +
               ", accessed=" + accessed +
               '}';
    }
}
