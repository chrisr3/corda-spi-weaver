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

import org.apache.aries.spifly.MultiDelegationClassloader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWiring;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import java.io.IOException;
import java.net.URL;
import java.security.AccessControlException;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.security.AccessController.doPrivileged;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import static org.osgi.framework.ServicePermission.GET;

/**
 * Methods used from ASM-generated code.
 */
public final class Util {
    private static final Logger logger = DynamicExtraWeavingActivator.logger;

    public static XMLInputFactory newXMLInputFactoryFactory(String factoryId, ClassLoader specifiedClassLoader, Class<?> caller) {
        return createXMLFactory(factoryId, specifiedClassLoader, caller, XMLInputFactory.class.getName(), XMLInputFactory::newFactory);
    }

    public static XMLInputFactory newXMLInputFactoryFactory(Class<?> caller) {
        return createXMLFactory(caller, XMLInputFactory.class.getName(), XMLInputFactory::newFactory);
    }

    @SuppressWarnings("deprecation")
    public static XMLInputFactory newXMLInputFactoryInstance(String factoryId, ClassLoader specifiedClassLoader, Class<?> caller) {
        return createXMLFactory(factoryId, specifiedClassLoader, caller, XMLInputFactory.class.getName(), XMLInputFactory::newInstance);
    }

    public static XMLInputFactory newXMLInputFactoryInstance(Class<?> caller) {
        return createXMLFactory(caller, XMLInputFactory.class.getName(), XMLInputFactory::newInstance);
    }

    public static XMLOutputFactory newXMLOutputFactoryFactory(String factoryId, ClassLoader specifiedClassLoader, Class<?> caller) {
        return createXMLFactory(factoryId, specifiedClassLoader, caller, XMLOutputFactory.class.getName(), XMLOutputFactory::newFactory);
    }

    public static XMLOutputFactory newXMLOutputFactoryFactory(Class<?> caller) {
        return createXMLFactory(caller, XMLOutputFactory.class.getName(), XMLOutputFactory::newFactory);
    }

    @SuppressWarnings("deprecation")
    public static XMLInputFactory newXMLOutputFactoryInstance(String factoryId, ClassLoader specifiedClassLoader, Class<?> caller) {
        return createXMLFactory(factoryId, specifiedClassLoader, caller, XMLOutputFactory.class.getName(), XMLOutputFactory::newInstance);
    }

    public static XMLOutputFactory newXMLOutputFactoryInstance(Class<?> caller) {
        return createXMLFactory(caller, XMLOutputFactory.class.getName(), XMLOutputFactory::newInstance);
    }

    public static XMLEventFactory newXMLEventFactoryFactory(String factoryId, ClassLoader specifiedClassLoader, Class<?> caller) {
        return createXMLFactory(factoryId, specifiedClassLoader, caller, XMLEventFactory.class.getName(), XMLEventFactory::newFactory);
    }

    public static XMLEventFactory newXMLEventFactoryFactory(Class<?> caller) {
        return createXMLFactory(caller, XMLEventFactory.class.getName(), XMLEventFactory::newFactory);
    }

    @SuppressWarnings("deprecation")
    public static XMLEventFactory newXMLEventFactoryInstance(String factoryId, ClassLoader specifiedClassLoader, Class<?> caller) {
        return createXMLFactory(factoryId, specifiedClassLoader, caller, XMLEventFactory.class.getName(), XMLEventFactory::newInstance);
    }

    public static XMLEventFactory newXMLEventFactoryInstance(Class<?> caller) {
        return createXMLFactory(caller, XMLEventFactory.class.getName(), XMLEventFactory::newInstance);
    }

    private static <X> X createXMLFactory(Class<?> caller, String factoryClassName, Supplier<X> factory) {
        final ClassLoader bundleLoader = doPrivileged((PrivilegedAction<? extends ClassLoader>)caller::getClassLoader);
        if (!(bundleLoader instanceof BundleReference)) {
            logger.log(FINE, "Classloader of consuming bundle doesn't implement BundleReference: {0}", bundleLoader);
            return factory.get();
        }

        final ClassLoader bundleClassLoader = findContextClassloader(
            ((BundleReference)bundleLoader).getBundle(), factoryClassName, factoryClassName
        );

        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(bundleClassLoader);
        try {
            return factory.get();
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    private static <X> X createXMLFactory(
        String factoryId,
        ClassLoader specifiedClassLoader,
        Class<?> caller,
        String factoryClassName,
        BiFunction<String, ClassLoader, X> factory
    ) {
        final ClassLoader bundleLoader = doPrivileged((PrivilegedAction<? extends ClassLoader>)caller::getClassLoader);
        if (!(bundleLoader instanceof BundleReference)) {
            logger.log(FINE, "Classloader of consuming bundle doesn't implement BundleReference: {0}", bundleLoader);
            return factory.apply(factoryId, specifiedClassLoader);
        }

        final ClassLoader bundleClassLoader = findContextClassloader(
            ((BundleReference)bundleLoader).getBundle(), factoryClassName, factoryId
        );
        return factory.apply(factoryId, bundleClassLoader == null ?
            specifiedClassLoader : new WrapperCL(specifiedClassLoader, bundleClassLoader)
        );
    }

    private static ClassLoader findContextClassloader(Bundle consumerBundle, String className, String requestedClass) {
        Collection<Bundle> bundles;
        try {
            bundles = new HashSet<>(getServiceBundles(consumerBundle.getBundleContext(), className));
            if (!className.equals(requestedClass)) {
                bundles.addAll(getServiceBundles(consumerBundle.getBundleContext(), requestedClass));
            }
        } catch (Exception e) {
            logger.log(SEVERE, e, () -> "Failed to query " + className + " services for " + consumerBundle);
            return null;
        }
        logger.log(FINE, "Found bundles providing {0}: {1}", new Object[] { className, bundles });

        switch (bundles.size()) {
        case 0:
            return null;
        case 1:
            Bundle bundle = bundles.iterator().next();
            return getBundleClassLoader(bundle);
        default:
            List<ClassLoader> loaders = new ArrayList<>();
            for (Bundle b : bundles) {
                loaders.add(getBundleClassLoader(b));
            }
            return new MultiDelegationClassloader(loaders.toArray(new ClassLoader[0]));
        }
    }

    private static Set<Bundle> getServiceBundles(BundleContext context, String serviceType) throws InvalidSyntaxException {
        final Set<Bundle> bundles = new HashSet<>();

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            try {
                sm.checkPermission(new ServicePermission(serviceType, GET));
            } catch (AccessControlException ace) {
                // access denied
                logger.log(FINE, "No permission to obtain services of type: {0}", serviceType);
                return bundles;
            }
        }

        ServiceReference<?>[] references = context.getServiceReferences(serviceType, null);
        if (references != null) {
            for (ServiceReference<?> reference : references) {
                bundles.add(reference.getBundle());
            }
        }
        return bundles;
    }

    private static ClassLoader getBundleClassLoader(final Bundle bundle) {
        return doPrivileged((PrivilegedAction<ClassLoader>) () -> bundle.adapt(BundleWiring.class).getClassLoader());
    }

    private static class WrapperCL extends ClassLoader {
        private final ClassLoader bundleClassloader;

        public WrapperCL(ClassLoader specifiedClassLoader, ClassLoader bundleClassloader) {
            super(specifiedClassLoader);
            this.bundleClassloader = bundleClassloader;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            return bundleClassloader.loadClass(name);
        }

        @Override
        protected URL findResource(String name) {
            return bundleClassloader.getResource(name);
        }

        @Override
        protected Enumeration<URL> findResources(String name) throws IOException {
            return bundleClassloader.getResources(name);
        }
    }
}
