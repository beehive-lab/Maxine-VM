/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
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
 * Internal representations of non-primitive classes whose instance values are object references.
 * 
 * @author Bernd Mathiske
 */
public abstract class ReferenceClassActor extends ClassActor {

    @Override
    public final boolean isReferenceClassActor() {
        return true;
    }

    protected ReferenceClassActor(Kind kind,
                                  SpecificLayout specificLayout,
                                  ClassLoader classLoader,
                                  Utf8Constant name,
                                  char majorVersion,
                                  char minorVersion,
                                  int flags,
                                  TypeDescriptor typeDescriptor,
                                  ClassActor superClassActor,
                                  ClassActor componentClassActor,
                                  InterfaceActor[] interfaceActors,
                                  FieldActor[] fieldActors,
                                  MethodActor[] methodActors,
                                  Utf8Constant genericSignature,
                                  byte[] runtimeVisibleAnnotationsBytes,
                                  String sourceFileName,
                                  TypeDescriptor[] innerClasses,
                                  TypeDescriptor outerClass,
                                  EnclosingMethodInfo enclosingMethodInfo) {
        super(kind,
              specificLayout,
              classLoader,
              name,
              majorVersion,
              minorVersion,
              flags,
              typeDescriptor,
              superClassActor,
              componentClassActor,
              interfaceActors,
              fieldActors,
              methodActors,
              genericSignature,
              runtimeVisibleAnnotationsBytes,
              sourceFileName,
              innerClasses,
              outerClass,
              enclosingMethodInfo);
    }

}
