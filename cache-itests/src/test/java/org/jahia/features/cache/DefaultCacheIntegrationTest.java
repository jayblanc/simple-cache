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

import org.jahia.features.cache.api.CacheProvider;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

/**
 * Integration test for default cache implementation (in-memory).
 *
 * @author Jerome Blanchard
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class DefaultCacheIntegrationTest extends AbstractCacheIntegrationTest {

    @Override
    protected String getFeatureName() {
        return "cache";
    }

    @Override
    protected String getExpectedProviderName() {
        return CacheProvider.DEFAULT_PROVIDER_NAME;
    }

    @Configuration
    public Option[] config() {
        return createBaseConfig();
    }
}
