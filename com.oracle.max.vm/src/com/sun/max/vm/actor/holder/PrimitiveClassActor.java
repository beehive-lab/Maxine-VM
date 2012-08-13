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

import java.lang.annotation.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;

/**
 * Internal representations of "primitive classes" such as 'int', 'char', etc.
 */
public final class PrimitiveClassActor extends ClassActor {

    @HOSTED_ONLY
    public PrimitiveClassActor(Kind kind) {
        super(kind,
              NO_SPECIFIC_LAYOUT,
              HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER,
              kind.name,
              NO_MAJOR_VERSION,
              NO_MINOR_VERSION,
              ACC_PUBLIC | ACC_FINAL,
              kind.typeDescriptor(),
              NO_SUPER_CLASS_ACTOR,
              NO_COMPONENT_CLASS_ACTOR,
              InterfaceActor.NONE,
              FieldActor.NONE,
              MethodActor.NONE,
              NO_GENERIC_SIGNATURE,
              NO_RUNTIME_VISIBLE_ANNOTATION_BYTES,
              NO_SOURCE_FILE_NAME,
              NO_INNER_CLASSES,
              NO_OUTER_CLASS,
              NO_ENCLOSING_METHOD_INFO);
    }

    public TupleClassActor toWrapperClassActor() {
        return (TupleClassActor) ClassActor.fromJava(kind.boxedClass);
    }

    @Override
    public boolean isPrimitiveClassActor() {
        return true;
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return null;
    }
}
