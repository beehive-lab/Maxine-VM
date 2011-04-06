/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.actor.holder;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * The centralized location in which all {@link ClassActor}s are created. This enables invariants
 * such as registration of only fully initialized (non-static) ClassActors with the {@link ClassRegistry}
 * to be maintained in one place.
 *
 * @author Doug Simon
 */
public final class ClassActorFactory {

    private ClassActorFactory() {
    }

    /**
     * Creates the actor for the type representing an 1-dimensional array of a given {@linkplain ClassActor#componentClassActor() component type}.
     */
    public static ArrayClassActor createArrayClassActor(ClassActor componentClassActor) {
        return ClassRegistry.put(new ArrayClassActor(componentClassActor));
    }

    /**
     * Creates a ClassActor for an interface.
     */
    public static InterfaceActor createInterfaceActor(
                    ConstantPool constantPool,
                    ClassLoader classLoader,
                    Utf8Constant name,
                    char majorVersion,
                    char minorVersion,
                    int flags,
                    InterfaceActor[] interfaceActors,
                    final FieldActor[] fieldActors,
                    final MethodActor[] methodActors,
                    Utf8Constant genericSignature,
                    byte[] runtimeVisibleAnnotationsBytes,
                    String sourceFileName,
                    TypeDescriptor[] innerClasses,
                    TypeDescriptor outerClass,
                    EnclosingMethodInfo enclosingMethodInfo) {
        final InterfaceActor interfaceActor = new InterfaceActor(
                        constantPool,
                        classLoader,
                        name,
                        majorVersion,
                        minorVersion,
                        flags,
                        interfaceActors,
                        fieldActors,
                        methodActors,
                        genericSignature,
                        runtimeVisibleAnnotationsBytes,
                        sourceFileName,
                        innerClasses,
                        outerClass,
                        enclosingMethodInfo);
        return interfaceActor;
    }

    /**
     * Creates a ClassActor for tuple instances.
     */
    public static ClassActor createTupleOrHybridClassActor(
                    ConstantPool constantPool,
                    ClassLoader classLoader,
                    Utf8Constant name,
                    char majorVersion,
                    char minorVersion,
                    int flags,
                    ClassActor superClassActor,
                    final InterfaceActor[] interfaceActors,
                    FieldActor[] fieldActors,
                    MethodActor[] methodActors,
                    Utf8Constant genericSignature,
                    byte[] runtimeVisibleAnnotationsBytes,
                    String sourceFileName,
                    TypeDescriptor[] innerClasses,
                    TypeDescriptor outerClass,
                    EnclosingMethodInfo enclosingMethodInfo) {
        final TypeDescriptor typeDescriptor = JavaTypeDescriptor.getDescriptorForJavaString(name.toString());
        final ClassActor classActor = isHybrid(typeDescriptor, superClassActor) ?
            new HybridClassActor(
                            constantPool,
                            classLoader,
                            name,
                            majorVersion,
                            minorVersion,
                            flags,
                            superClassActor,
                            interfaceActors,
                            fieldActors,
                            methodActors) :
            new TupleClassActor(
                            typeDescriptor.toKind(),
                            constantPool,
                            classLoader,
                            name,
                            majorVersion,
                            minorVersion,
                            flags,
                            superClassActor,
                            interfaceActors,
                            fieldActors,
                            methodActors,
                            genericSignature,
                            runtimeVisibleAnnotationsBytes,
                            sourceFileName,
                            innerClasses,
                            outerClass, enclosingMethodInfo);
        return classActor;
    }

    @INLINE
    private static boolean isHybrid(final TypeDescriptor typeDescriptor, ClassActor superClassActor) {
        return MaxineVM.isHosted() && JavaTypeDescriptor.isAssignableFrom(JavaTypeDescriptor.HYBRID, typeDescriptor, superClassActor);
    }
}
