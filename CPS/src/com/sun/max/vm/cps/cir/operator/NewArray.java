/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.cir.operator;

import com.sun.max.lang.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.cps.b.c.*;
import com.sun.max.vm.cps.cir.operator.JavaOperator.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.type.*;

public class NewArray extends JavaResolvableOperator<ArrayClassActor> {

    private final Kind primitiveElementKind;

    public NewArray(int atype) {
        super(CALL_STOP | NEGATIVE_ARRAY_SIZE_CHECK, null, 0, Kind.REFERENCE);
        primitiveElementKind = Kind.fromNewArrayTag(atype);
        actor = primitiveElementKind.arrayClassActor();
    }

    public NewArray(ConstantPool constantPool, int index) {
        super(CALL_STOP | NEGATIVE_ARRAY_SIZE_CHECK, constantPool, index, Kind.REFERENCE);
        primitiveElementKind = null;
    }

    /**
     * Gets the primitive {@linkplain ClassActor#elementClassActor() element kind} of the array created by this
     * operator. If this operator creates a reference array, then {@code null} is returned.
     */
    public Kind primitiveElementKind() {
        return primitiveElementKind;
    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitHCirOperator(this);
    }

    @Override
    public void acceptVisitor(HCirOperatorVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        if (primitiveElementKind != null) {
            return "New" + Strings.capitalizeFirst(primitiveElementKind.name.string, true) + "Array";
        }
        return super.toString();
    }

    /**
     * Overrides resolution for non-primitive array creation so that the resolved type is the array type,
     * not the component type denoted by the constant pool entry.
     */
    @Override
    public void resolve() {
        if (primitiveElementKind == null) {
            super.resolve();
            actor = ArrayClassActor.forComponentClassActor(actor);
        }
    }

    private static final Kind[] parameterKinds = {Kind.INT};

    @Override
    public Kind[] parameterKinds() {
        return parameterKinds;
    }
}
