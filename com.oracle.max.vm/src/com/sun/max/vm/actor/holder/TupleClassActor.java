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
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.type.*;

/**
 * Internal representations of classes, which have instances that are tuples.
 *
 * A "tuple" is an object with fields, as opposed to an array, which has only elements.
 */
public class TupleClassActor extends ReferenceClassActor {

    @INSPECTED
    private final ConstantPool constantPool;


    protected TupleClassActor(Kind kind,
                              ConstantPool constantPool,
                              ClassLoader classLoader,
                              Utf8Constant name,
                              char majorVersion,
                              char minorVersion,
                              int flags,
                              ClassActor superClassActor,
                              InterfaceActor[] interfaceActors,
                              FieldActor[] fieldActors,
                              MethodActor[] methodActors,
                              Utf8Constant genericSignature,
                              byte[] runtimeVisibleAnnotationsBytes,
                              String sourceFileName,
                              TypeDescriptor[] innerClasses,
                              TypeDescriptor outerClass, EnclosingMethodInfo enclosingMethodInfo) {
        super(kind,
              Layout.tupleLayout(),
              classLoader,
              name,
              majorVersion,
              minorVersion,
              flags,
              JavaTypeDescriptor.getDescriptorForWellFormedTupleName(name.toString()),
              getSuperClassActor(superClassActor, name),
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
    }

    @Override
    public final ConstantPool constantPool() {
        return constantPool;
    }

    /**
     * Severs the {@link Word} types from the standard Java class hierarchy by making the superClassActor of {@link Word} be null.
     */
    @INLINE
    private static ClassActor getSuperClassActor(ClassActor superClassActor, Utf8Constant name) {
        if (MaxineVM.isHosted()) {
            if (JavaTypeDescriptor.WORD == JavaTypeDescriptor.getDescriptorForJavaString(name.toString())) {
                return null;
            }
        }
        return superClassActor;
    }

    @Override
    protected Size layoutFields(SpecificLayout specificLayout) {
        final TupleLayout tupleLayout = (TupleLayout) specificLayout;
        return tupleLayout.layoutFields(superClassActor, localInstanceFieldActors());
    }

    public static TupleClassActor fromJava(final Class javaClass) {
        return (TupleClassActor) ClassActor.fromJava(javaClass);
    }
}
