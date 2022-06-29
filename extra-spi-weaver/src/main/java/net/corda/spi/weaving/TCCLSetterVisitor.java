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

import org.apache.aries.spifly.WeavingData;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

/**
 * This class implements an ASM ClassVisitor which puts the appropriate ThreadContextClassloader
 * calls around applicable method invocations. It does the actual bytecode weaving.
 */
class TCCLSetterVisitor extends ClassVisitor {
    private static final Type CLASSLOADER_TYPE = Type.getType(ClassLoader.class);
    private static final Type CLASS_TYPE = Type.getType(Class.class);
    private static final Type STRING_TYPE = Type.getType(String.class);
    private static final Type UTIL_CLASS = Type.getType(Util.class);

    static final String NEW_FACTORY = "newFactory";
    static final String NEW_INSTANCE = "newInstance";

    private static final Set<String> FACTORY_TYPES = Set.of(
        XMLInputFactory.class.getName(),
        XMLOutputFactory.class.getName(),
        XMLEventFactory.class.getName()
    );
    private static final Set<String> FACTORY_NAMES = Set.of(
        NEW_INSTANCE,
        NEW_FACTORY
    );

    private final Type targetClass;
    private final Set<WeavingData> weavingData;
    private final Set<String> extraImports;

    // This field is true when the class was woven
    private boolean woven = false;

    TCCLSetterVisitor(ClassVisitor cv, @Nonnull String className, Set<WeavingData> weavingData) {
        super(ASM9, cv);
        this.targetClass = Type.getObjectType(className.replace('.', '/'));
        this.weavingData = weavingData;
        this.extraImports = new LinkedHashSet<>();
    }

    Set<String> getExtraImports() {
        return extraImports;
    }

    boolean isWoven() {
        return woven;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        return (mv == null) ? null : new TCCLSetterMethodVisitor(api, mv, access, name, desc);
    }

    private class TCCLSetterMethodVisitor extends GeneratorAdapter {
        TCCLSetterMethodVisitor(int api, MethodVisitor mv, int access, String name, String descriptor) {
            super(api, mv, access, name, descriptor);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (opcode != INVOKESTATIC) {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
                return;
            }

            final WeavingData weavingData = findWeavingData(owner, name, desc);
            if (weavingData == null) {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
                return;
            }

            final String serviceClassName = weavingData.getClassName();
            final String serviceMethodName = weavingData.getMethodName();
            final String[] serviceArgClasses = weavingData.getArgClasses();

            // XMLFactory.newInstance(..) and XMLFactory.newFactory(..)
            if (FACTORY_TYPES.contains(serviceClassName) && FACTORY_NAMES.contains(serviceMethodName) && serviceArgClasses != null) {
                visitLdcInsn(targetClass);
                invokeStatic(UTIL_CLASS, new Method(
                    "new" + serviceClassName.substring(serviceClassName.lastIndexOf('.') + 1) + serviceMethodName.substring(3),
                    Type.getReturnType(desc),
                    serviceArgClasses.length == 0
                        ? new Type[] { CLASS_TYPE }
                        : new Type[] { STRING_TYPE, CLASSLOADER_TYPE, CLASS_TYPE }
                ));

                extraImports.add(Util.class.getPackageName());
                woven = true;
            }
        }

        @Nullable
        private WeavingData findWeavingData(String owner, String methodName, String methodDesc) {
            final Type[] argTypes = Type.getArgumentTypes(methodDesc);
            String[] argClassNames = new String[argTypes.length];
            for (int i = 0; i < argTypes.length; i++) {
                argClassNames[i] = argTypes[i].getClassName();
            }

            final String internalOwner = owner.replace('/', '.');
            for (WeavingData wd : weavingData) {
                if (wd.getClassName().equals(internalOwner) &&
                    wd.getMethodName().equals(methodName) &&
                    (wd.getArgClasses() == null || Arrays.equals(argClassNames, wd.getArgClasses()))) {
                    return wd;
                }
            }
            return null;
        }
    }
}
