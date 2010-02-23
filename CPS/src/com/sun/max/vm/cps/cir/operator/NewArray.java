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
