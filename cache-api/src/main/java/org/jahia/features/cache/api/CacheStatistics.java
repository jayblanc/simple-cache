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
public class CacheStatistics {

    private final long totalHits;
    private final long totalMisses;
    private final long size;

    public CacheStatistics() {
        this.totalHits = 0;
        this.totalMisses = 0;
        this.size = 0;
    }

    public long getTotalHits() {
        return totalHits;
    }

    public long getTotalMisses() {
        return totalMisses;
    }

    public long getSize() {
        return size;
    }

}
