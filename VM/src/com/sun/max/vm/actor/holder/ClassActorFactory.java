/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
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
        ClassRegistry.put(interfaceActor);
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
        ClassRegistry.put(classActor);
        return classActor;
    }

    @INLINE
    private static boolean isHybrid(final TypeDescriptor typeDescriptor, ClassActor superClassActor) {
        return MaxineVM.isHosted() && JavaTypeDescriptor.isAssignableFrom(JavaTypeDescriptor.HYBRID, typeDescriptor, superClassActor);
    }
}
