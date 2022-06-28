/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package net.corda.spi.weaving;

import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import org.apache.aries.spifly.ArgRestrictions;
import org.apache.aries.spifly.BundleDescriptor;
import org.apache.aries.spifly.ConsumerRestriction;
import org.apache.aries.spifly.MethodRestriction;
import org.apache.aries.spifly.WeavingData;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static net.corda.spi.weaving.TCCLSetterVisitor.NEW_FACTORY;
import static net.corda.spi.weaving.TCCLSetterVisitor.NEW_INSTANCE;
import static org.osgi.framework.Constants.FILTER_DIRECTIVE;
import static org.osgi.framework.Constants.VERSION_ATTRIBUTE;
import static org.osgi.service.serviceloader.ServiceLoaderNamespace.SERVICELOADER_NAMESPACE;

final class ConsumerHeaderProcessor {
    @Nonnull
    static Set<WeavingData> processRequireCapabilityHeader(String consumerHeader) throws InvalidSyntaxException {
        Set<WeavingData> weavingData = new HashSet<>();

        Parameters requirements = OSGiHeader.parseHeader(consumerHeader);
        Entry<String, ? extends Map<String, String>> extenderRequirement = findRequirement(requirements, "osgi.extender", "osgi.serviceloader.processor");

        if (extenderRequirement != null) {
            Collection<Entry<String, ? extends Map<String, String>>> serviceLoaderRequirements = findAllMetadata(requirements, SERVICELOADER_NAMESPACE);
            List<BundleDescriptor> allowedBundles = new ArrayList<>();
            for (Entry<String, ? extends Map<String, String>> req : serviceLoaderRequirements) {
                String slFilterString = req.getValue().get(FILTER_DIRECTIVE + ':');
                if (slFilterString != null) {
                    Filter slFilter = FrameworkUtil.createFilter(slFilterString);
                    allowedBundles.add(new BundleDescriptor(slFilter));
                }
            }

            {
                ArgRestrictions ar = new ArgRestrictions();
                ar.addRestriction(0, String.class.getName());
                ar.addRestriction(1, ClassLoader.class.getName());

                // XMLInputFactory.newFactory(String, ClassLoader)
                // XMLOutputFactory.newFactory(String, ClassLoader)
                // XMLEventFactory.newFactory(String, ClassLoader)
                MethodRestriction newFactoryRestrictions = new MethodRestriction(NEW_FACTORY, ar);
                weavingData.add(createWeavingData(XMLInputFactory.class.getName(), NEW_FACTORY, newFactoryRestrictions, allowedBundles));
                weavingData.add(createWeavingData(XMLOutputFactory.class.getName(), NEW_FACTORY, newFactoryRestrictions, allowedBundles));
                weavingData.add(createWeavingData(XMLEventFactory.class.getName(), NEW_FACTORY, newFactoryRestrictions, allowedBundles));

                // XMLInputFactory.newInstance(String, ClassLoader)
                // XMLOutputFactory.newInstance(String, ClassLoader)
                // XMLEventFactory.newInstance(String, ClassLoader)
                MethodRestriction newInstanceRestrictions = new MethodRestriction(NEW_INSTANCE, ar);
                weavingData.add(createWeavingData(XMLInputFactory.class.getName(), NEW_INSTANCE, newInstanceRestrictions, allowedBundles));
                weavingData.add(createWeavingData(XMLOutputFactory.class.getName(), NEW_INSTANCE, newInstanceRestrictions, allowedBundles));
                weavingData.add(createWeavingData(XMLEventFactory.class.getName(), NEW_INSTANCE, newInstanceRestrictions, allowedBundles));
            }

            {
                ArgRestrictions ar = new ArgRestrictions();

                // XMLInputFactory.newFactory()
                // XMLOutputFactory.newFactory()
                // XMLEventFactory.newFactory()
                MethodRestriction newFactoryRestrictions = new MethodRestriction(NEW_FACTORY, ar);
                weavingData.add(createWeavingData(XMLInputFactory.class.getName(), NEW_FACTORY, newFactoryRestrictions, allowedBundles));
                weavingData.add(createWeavingData(XMLOutputFactory.class.getName(), NEW_FACTORY, newFactoryRestrictions, allowedBundles));
                weavingData.add(createWeavingData(XMLEventFactory.class.getName(), NEW_FACTORY, newFactoryRestrictions, allowedBundles));

                // XMLInputFactory.newInstance()
                // XMLOutputFactory.newInstance()
                // XMLEventFactory.newInstance()
                MethodRestriction newInstanceRestrictions = new MethodRestriction(NEW_INSTANCE, ar);
                weavingData.add(createWeavingData(XMLInputFactory.class.getName(), NEW_INSTANCE, newInstanceRestrictions, allowedBundles));
                weavingData.add(createWeavingData(XMLOutputFactory.class.getName(), NEW_INSTANCE, newInstanceRestrictions, allowedBundles));
                weavingData.add(createWeavingData(XMLEventFactory.class.getName(), NEW_INSTANCE, newInstanceRestrictions, allowedBundles));
            }
        }

        return weavingData;
    }

    @Nonnull
    private static WeavingData createWeavingData(
        String className,
        String methodName,
        MethodRestriction methodRestriction,
        @Nonnull List<BundleDescriptor> allowedBundles
    ) {
        ConsumerRestriction restriction = new ConsumerRestriction(className, methodRestriction);

        Set<ConsumerRestriction> restrictions = new HashSet<>();
        restrictions.add(restriction);

        String[] argClasses = restriction.getMethodRestriction(methodName).getArgClasses();

        return new WeavingData(className, methodName, argClasses, restrictions, allowedBundles.isEmpty() ? null : allowedBundles);
    }

    @Nullable
    static Entry<String, ? extends Map<String, String>> findCapability(@Nonnull Parameters capabilities, String namespace, String spiName) {
        for (Entry<String, ? extends Map<String, String>> cap : capabilities.entrySet()) {
            String key = removeDuplicateMarker(cap.getKey());
            if (namespace.equals(key)) {
                if (spiName.equals(cap.getValue().get(namespace))) {
                    return cap;
                }
            }
        }
        return null;
    }

    @Nullable
    static Entry<String, ? extends Map<String, String>> findRequirement(@Nonnull Parameters requirements, String namespace, String type) throws InvalidSyntaxException {
        Dictionary<String, Object> nsAttr = new Hashtable<>();
        nsAttr.put(namespace, type);
        nsAttr.put(VERSION_ATTRIBUTE, "1.0.0");

        for (Entry<String, ? extends Map<String, String>> req : requirements.entrySet()) {
            String key = removeDuplicateMarker(req.getKey());
            if (namespace.equals(key)) {
                String filterString = req.getValue().get(FILTER_DIRECTIVE + ':');
                if (filterString != null) {
                    Filter filter = FrameworkUtil.createFilter(filterString);
                    if (filter.match(nsAttr)) {
                        return req;
                    }
                }
            }
        }
        return null;
    }

    @Nonnull
    static Collection<Entry<String, ? extends Map<String, String>>> findAllMetadata(@Nonnull Parameters requirementsOrCapabilities, String namespace) {
        List<Entry<String, ? extends Map<String, String>>> reqsCaps = new ArrayList<>();
        for (Entry<String, ? extends Map<String, String>> reqCap : requirementsOrCapabilities.entrySet()) {
            String key = removeDuplicateMarker(reqCap.getKey());
            if (namespace.equals(key)) {
                reqsCaps.add(reqCap);
            }
        }
        return reqsCaps;
    }

    @Nonnull
    static String removeDuplicateMarker(@Nonnull String key) {
        int i = key.length() - 1;
        while (i >= 0 && key.charAt(i) == '~') {
            --i;
        }
        return key.substring(0, i + 1);
    }
}
