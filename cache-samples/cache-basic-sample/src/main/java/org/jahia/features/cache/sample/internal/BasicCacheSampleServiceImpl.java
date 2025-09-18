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
package org.jahia.features.cache.sample.internal;

import org.jahia.features.cache.sample.BasicCacheSampleService;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jerome Blanchard
 */
@Component(service = BasicCacheSampleService.class, immediate = true)
public class BasicCacheSampleServiceImpl implements BasicCacheSampleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicCacheSampleServiceImpl.class);

    private String value = "default";

    @Override
    public String getValue() {
        LOGGER.info("Getting value");
        return "Value at " + System.currentTimeMillis() + " is: " + value;
    }

    @Override
    public void setValue(String value) {
        LOGGER.info("Setting value to {}", value);
        this.value = value;
    }

}
