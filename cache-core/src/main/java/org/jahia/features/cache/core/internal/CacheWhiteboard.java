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

import org.jahia.features.cache.api.CacheManager;
import org.jahia.features.cache.api.CacheResult;
import org.osgi.framework.*;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jerome Blanchard
 */
@Component(immediate = true)
public class CacheWhiteboard {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheWhiteboard.class);

    private final Map<Object, ServiceRegistration<?>> registrations = new ConcurrentHashMap<>();
    private ServiceTracker<Object, Object> serviceTracker;
    private BundleContext context;
    @Reference
    private CacheManager cacheManager;

    @Activate
    public void activate(BundleContext context) {
        this.context = context;
        this.startServiceTracker();
    }

    @Deactivate
    public void deactivate() {
        if (serviceTracker != null) {
            serviceTracker.close();
        }
        registrations.values().forEach(ServiceRegistration::unregister);
        registrations.clear();
    }

    private void startServiceTracker () {
        try {
            LOGGER.info("Starting cache whiteboard service tracker");
            String filterString = "(&(" + Constants.OBJECTCLASS + "=*)" +
                    "(!(" + Constants.OBJECTCLASS + "=" + CacheWhiteboard.class.getName() + "))" +
                    "(!(cache.proxy=true)))";
            Filter filter = context.createFilter(filterString);
            if ( filter != null ) {
                serviceTracker = new ServiceTracker<>(context, filter, new ServiceTrackerCustomizer<Object, Object>() {
                    @Override public Object addingService(org.osgi.framework.ServiceReference<Object> reference) {
                        Object service = context.getService(reference);
                        if (Proxy.isProxyClass(service.getClass())) {
                            LOGGER.debug("Service {} is already a proxy, skipping.", service.getClass().getName());
                            return service;
                        }
                        Map<String, Object> props = new Hashtable<>();
                        for (String key : reference.getPropertyKeys()) {
                            props.put(key, reference.getProperty(key));
                        }
                        if (hasCacheAnnotations(service)) {
                            LOGGER.info("Creating caching proxy for service: {}", service.getClass().getName());
                            Object proxy = createProxy(service);
                            Dictionary<String, Object> proxyProps = new Hashtable<>(props);
                            proxyProps.put(Constants.SERVICE_RANKING, (int) props.getOrDefault(Constants.SERVICE_RANKING, 0) + 1);
                            proxyProps.put("cache.proxy", Boolean.TRUE);
                            String[] ifaces = Arrays.stream(service.getClass().getInterfaces()).map(Class::getName).toArray(String[]::new);
                            ServiceRegistration<?> reg = context.registerService(ifaces, proxy, proxyProps);
                            LOGGER.info("Caching proxy registered: {} with properties: {}", proxy.getClass().getName(), props);
                            registrations.put(service, reg);
                        }
                        return service;
                    }

                    @Override public void modifiedService(org.osgi.framework.ServiceReference<Object> reference, Object service) {
                        // nothing to do
                    }

                    @Override public void removedService(org.osgi.framework.ServiceReference<Object> reference, Object service) {
                        ServiceRegistration<?> reg = registrations.remove(service);
                        // TODO cleanup caches associated with this service
                        if (reg != null) {
                            reg.unregister();
                            LOGGER.info("Unregistering caching proxy for service: {}", service.getClass().getName());
                        }
                        context.ungetService(reference);
                    }
                });
                serviceTracker.open();
            }
        } catch (InvalidSyntaxException e) {
            LOGGER.error("Failed to create service tracker filter", e);
        }
    }

    private boolean hasCacheAnnotations(Object svc) {
        for (Method m : svc.getClass().getMethods()) {
            if (m.isAnnotationPresent(CacheResult.class)) {
                return true;
            }
        }
        for (Class<?> iface : svc.getClass().getInterfaces()) {
            for (Method m : iface.getMethods()) {
                if (m.isAnnotationPresent(CacheResult.class)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Object createProxy(Object target) {
        return Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                target.getClass().getInterfaces(),
                new CacheInterceptor(target, cacheManager));
    }
}
