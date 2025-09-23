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
 * @author Jerome Blanchard
 */
public class CacheEntry<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String key;
    private final T value;
    private final long created;
    private long accessed;

    public CacheEntry(String key, T value) {
        this.key = key;
        this.value = value;
        this.created = System.currentTimeMillis();
        this.accessed = this.created;
    }

    public String key() {
        return key;
    }

    public T value() {
        return value;
    }

    public long created() {
        return created;
    }

    public long accessed() {
        return accessed;
    }

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
