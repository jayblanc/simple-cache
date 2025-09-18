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

/**
 * @author Jerome Blanchard
 */
public class CacheConfig {

    private final int maxEntries;
    private final int timeToLive;

    private CacheConfig(int maxEntries, int timeToLive) {
        this.maxEntries = maxEntries;
        this.timeToLive = timeToLive;
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public int getTimeToLive() {
        return timeToLive;
    }

    public static CacheConfigBuilder create() {
        return new CacheConfigBuilder();
    }

    public static class CacheConfigBuilder {
        private int maxEntries = 1000;
        private int timeToLive = 3600;

        public CacheConfigBuilder maxEntries(int maxEntries) {
            this.maxEntries = maxEntries;
            return this;
        }

        public CacheConfigBuilder timeToLive(int timeToLive) {
            this.timeToLive = timeToLive;
            return this;
        }

        public CacheConfig build() {
            return new CacheConfig(maxEntries, timeToLive);
        }
    }

}
