/*
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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * We need to override ASM's default behaviour in {@link #getCommonSuperClass(String, String)}
 * so that it doesn't load classes (which it was doing on the wrong {@link ClassLoader} anyway...)
 */
final class OSGiFriendlyClassWriter extends ClassWriter {
    private static final String OBJECT_INTERNAL_NAME = "java/lang/Object";
    private static final String PACKAGE_WIRING = "osgi.wiring.package";
    private static final String CLASS_EXTENSION = ".class";

    private final BundleWiring initialWiring;

    OSGiFriendlyClassWriter(int flags, BundleWiring initialWiring) {
        super(flags);
        this.initialWiring = initialWiring;
    }

    /**
     * We provide an implementation that doesn't cause class loads to occur. It works
     * by following the {@link BundleWiring} objects all the way back to {@link Object},
     * linking each super class's {@code osgi.wiring.package} requirement to a bundle
     * with a matching capability.
     *
     * precondition: classA and classB are not equal. (checked before this method is called)
     */
    @Override
    @Nonnull
    protected String getCommonSuperClass(String classA, String classB) {
        //If either is Object, then Object must be the answer
        if (OBJECT_INTERNAL_NAME.equals(classA) || OBJECT_INTERNAL_NAME.equals(classB)) {
            return OBJECT_INTERNAL_NAME;
        }

        final Deque<String> listA = getSuperClasses(classA);
        final Deque<String> listB = getSuperClasses(classB);

        String commonSuperClass = OBJECT_INTERNAL_NAME;
        int count = Math.min(listA.size(), listB.size());
        while (count > 0) {
            final String superClassA = listA.removeFirst();
            final String superClassB = listB.removeFirst();
            if (!superClassA.equals(superClassB)) {
                break;
            }
            commonSuperClass = superClassA;
            --count;
        }
        return commonSuperClass;
    }

    @Nonnull
    private Deque<String> getSuperClasses(String className) {
        final Deque<String> superClasses = new LinkedList<>();
        BundleWiring bundleWiring = initialWiring;
        String packageName = "";
        for (;;) {
            superClasses.addFirst(className);
            if (OBJECT_INTERNAL_NAME.equals(className)) {
                break;
            }

            final String nextPackageName = getPackageName(className);
            if (!packageName.equals(nextPackageName)) {
                // Either the package has changed, or we don't know that it
                // hasn't changed. Check whether this bundle wiring supports
                // our package or must be switched for one that does.
                packageName = nextPackageName;
                bundleWiring = getBundleWiringFor(bundleWiring, packageName);
            }

            final String superClassName = extractSuperClass(bundleWiring, className);
            if (superClassName == null) {
                // Only java.lang.Object should have a null super class.
                throw new TypeNotPresentException(className, null);
            }
            className = superClassName;
        }
        return superClasses;
    }

    @Nullable
    private String extractSuperClass(BundleWiring bundleWiring, String className) {
        final ClassLoader cl = bundleWiring == null ? ClassLoader.getPlatformClassLoader() : bundleWiring.getClassLoader();
        final InputStream is = cl.getResourceAsStream(className + CLASS_EXTENSION);
        if (is == null) {
            return null;
        }

        try (is) {
            return new ClassReader(is).getSuperName();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private BundleWiring getBundleWiringFor(@Nonnull BundleWiring bundleWiring, String packageName) {
        final List<BundleWire> requiredWires = bundleWiring.getRequiredWires(PACKAGE_WIRING);
        for (BundleWire requiredWire : requiredWires) {
            final BundleCapability capability = requiredWire.getCapability();
            if (capability != null) {
                final Map<String, ?> attributes = capability.getAttributes();
                final Object wirePackage = attributes.get(PACKAGE_WIRING);
                if (packageName.equals(wirePackage)) {
                    return requiredWire.getProviderWiring();
                }
            }
        }
        // We haven't gone anywhere, so keep this wiring.
        return bundleWiring;
    }

    @Nonnull
    private static String getPackageName(@Nonnull String className) {
        int idx = className.lastIndexOf('/');
        if (idx < 0) {
            throw new IllegalArgumentException("Invalid class: " + className);
        }
        return className.substring(0, idx).replace('/', '.');
    }
}
