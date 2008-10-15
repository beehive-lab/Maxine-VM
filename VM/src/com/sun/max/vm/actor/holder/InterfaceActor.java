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

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.type.*;

/**
 * Internal representations of Java interfaces.
 * 
 * @author Bernd Mathiske
 */
public final class InterfaceActor extends ClassActor {

    public boolean isAnnotation() {
        return (flags() & ACC_ANNOTATION) != 0;
    }

    private final ConstantPool _constantPool;

    @Override
    public ConstantPool constantPool() {
        return _constantPool;
    }

    InterfaceActor(ConstantPool constantPool,
                   ClassLoader classLoader,
                   Utf8Constant name,
                   char majorVersion,
                   char minorVersion,
                   int flags,
                   InterfaceActor[] interfaceActors,
                   FieldActor[] fieldActors,
                   MethodActor[] methodActors,
                   Utf8Constant genericSignature,
                   byte[] runtimeVisibleAnnotationsBytes,
                   String sourceFileName,
                   TypeDescriptor[] innerClasses,
                   TypeDescriptor outerClass,
                   EnclosingMethodInfo enclosingMethodInfo) {
        super(Kind.REFERENCE,
              Layout.tupleLayout(),
              classLoader,
              name,
              majorVersion,
              minorVersion,
              flags | ACC_INTERFACE,
              JavaTypeDescriptor.getDescriptorForWellFormedTupleName(name.toString()),
              ClassRegistry.javaLangObjectActor(),
              NO_COMPONENT_CLASS_ACTOR,
              interfaceActors,
              fieldActors,
              methodActors,
              genericSignature,
              runtimeVisibleAnnotationsBytes,
              sourceFileName,
              innerClasses,
              outerClass,
              enclosingMethodInfo);
        _constantPool = constantPool;
        constantPool.setHolder(this);
        for (int i = 0; i < localInterfaceMethodActors().length; i++) {
            localInterfaceMethodActors()[i].setIIndexInInterface(i + 1);
        }
    }

    public static final InterfaceActor[] NONE = {};
}
