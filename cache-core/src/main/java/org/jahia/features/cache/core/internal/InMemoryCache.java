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

import org.jahia.features.cache.api.Cache;
import org.jahia.features.cache.api.CacheConfig;
import org.jahia.features.cache.api.CacheEntry;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Jerome Blanchard
 */
public class InMemoryCache<T> implements Cache<T> {

    private final String cacheName;
    private final CacheConfig cacheConfig;
    private final Map<String, Entry<T>> entries;

    public InMemoryCache(String cacheName, CacheConfig cacheConfig) {
        this.cacheName = cacheName;
        this.cacheConfig = cacheConfig;
        this.entries = new LinkedHashMap<>(16, 0.75f, true);
    }

    @Override
    public String getName() {
        return cacheName;
    }

    @Override
    public CacheConfig getConfig() {
        return cacheConfig;
    }

    @Override
    public synchronized CacheEntry<T> getEntry(String key) {
        Entry<T> entry = entries.get(key);
        if (entry != null) {
            if (System.currentTimeMillis() - entry.created() < getConfig().getTimeToLive() * 1000L) {
                entry.touch();
                return entry;
            } else {
                entries.remove(key);
            }
        }
        return null;
    }

    @Override
    public T get(String key) {
        CacheEntry<T> entry = getEntry(key);
        return (entry !=null) ? entry.value() : null;
    }

    @Override
    public synchronized CacheEntry<T> put(String key, T value) {
        Entry<T> entry = new Entry<>(key, value);
        CacheEntry<T> old = entries.put(entry.key(), entry);
        if (getConfig().getMaxEntries() > 0 && entries.size() >= (getConfig().getMaxEntries() + 1)) {
            Entry<T> eldest = entries.values().iterator().next();
            entries.remove(eldest.key());
        }
        return old;
    }

    @Override
    public synchronized void clear() {
        entries.clear();
    }

    @Override
    public synchronized CacheEntry<T> delete(String key) {
        return entries.remove(key);
    }

    @Override
    public int size() {
        return entries.size();
    }

    private static class Entry<T> implements CacheEntry<T> {
        private final String key;
        private final T value;
        private final long created;
        private long accessed;

        public Entry(String key, T value) {
            this.key = key;
            this.value = value;
            this.created = System.currentTimeMillis();
            this.accessed = this.created;
        }

        @Override
        public String key() { return key; }

        @Override
        public T value() { return value; }

        @Override
        public long created() { return created; }

        @Override
        public long accessed() { return accessed; }

        public void touch() { accessed = System.currentTimeMillis(); }

        @Override
        public String toString() {
            return "Entry{" + "key='" + key + '\'' + ", class=" + value.getClass().getName() + ", created=" + created + ", accessed=" + accessed + '}';
        }
    }

}
