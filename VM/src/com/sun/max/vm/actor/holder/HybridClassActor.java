/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.type.*;

/**
 * Class actors for hybrid objects (currently only hubs).
 *
 * @author Bernd Mathiske
 */
public class HybridClassActor extends ReferenceClassActor {

    @INSPECTED
    private final ConstantPool constantPool;

    HybridClassActor(ConstantPool constantPool,
                     ClassLoader classLoader,
                     Utf8Constant name,
                     char majorVersion,
                     char minorVersion,
                     int flags,
                     ClassActor superClassActor,
                     InterfaceActor[] interfaceActors,
                     FieldActor[] fieldActors,
                     MethodActor[] methodActors) {
        super(Kind.REFERENCE,
              Layout.hybridLayout(),
              classLoader,
              name,
              majorVersion,
              minorVersion,
              flags,
              JavaTypeDescriptor.getDescriptorForWellFormedTupleName(name.toString()),
              superClassActor,
              NO_COMPONENT_CLASS_ACTOR,
              interfaceActors,
              fieldActors,
              methodActors,
              NO_GENERIC_SIGNATURE,
              NO_RUNTIME_VISIBLE_ANNOTATION_BYTES,
              NO_SOURCE_FILE_NAME,
              NO_INNER_CLASSES,
              NO_OUTER_CLASS,
              NO_ENCLOSING_METHOD_INFO);
        this.constantPool = constantPool;
        constantPool.setHolder(this);
    }

    @Override
    public final ConstantPool constantPool() {
        return constantPool;
    }

    @Override
    public void recordUniqueConcreteSubtype() {
        UniqueConcreteSubtypeTable.recordClassActor(this);
    }

    @Override
    protected Size layoutFields(SpecificLayout specificLayout) {
        final HybridLayout hybridLayout = (HybridLayout) specificLayout;
        return hybridLayout.layoutFields(superClassActor, localInstanceFieldActors());
    }
}
