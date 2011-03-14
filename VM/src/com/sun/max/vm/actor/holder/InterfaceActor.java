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

    private final ConstantPool constantPool;

    @Override
    public ConstantPool constantPool() {
        return constantPool;
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
              ClassRegistry.OBJECT,
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
        this.constantPool = constantPool;
        constantPool.setHolder(this);
        for (int i = 0; i < localInterfaceMethodActors().length; i++) {
            localInterfaceMethodActors()[i].setIIndexInInterface(i + 1);
        }
    }

    public static final InterfaceActor[] NONE = {};
}
