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
/*VCSID=e74ecb84-ca6a-4129-a3b6-b9f3408cec81*/
package com.sun.max.tele.value;

import com.sun.max.lang.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.value.*;

/**
 * Boxed representations of object references (and null).
 * We cannot use {@link ObjectReferenceValue},
 * because it wraps an Object, not a reference for bootstrapping reasons.
 * Instead we need to wrap a Reference here,
 * since there is no related Object in our address space.
 *
 * NOTE: (mlvdv) Although the name of this class suggests that it is
 * part of the {@link TeleObject} class hierarchy that is used to
 * model the state of heap objects in the tele VM, it is in fact
 * not part of that hierarchy.  This represents the clash of two
 * models for doing things that have not yet been resolved.
 * The function of a {@link TeleReferenceValue} is very much
 * like that of a {@link TeleObject}.
 *
 * @see Reference
 *
 * @author Bernd Mathiske
 * @author Athul Acharya
 */
public final class TeleReferenceValue extends ReferenceValue {

    private final TeleVM teleVM;
    public final TeleReference reference;

    public static TeleReferenceValue from(TeleVM teleVM, Reference reference) {
        return new TeleReferenceValue(teleVM, reference);
    }

    private TeleReferenceValue(TeleVM teleVM, Reference reference) {
        this.teleVM = teleVM;
        this.reference = (TeleReference) reference;
    }

    @Override
    public boolean isZero() {
        return reference.isZero();
    }

    @Override
    public boolean isAllOnes() {
        return reference.isAllOnes();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            // common case of reference equality
            return true;
        }
        if (!(other instanceof TeleReferenceValue)) {
            return false;
        }
        final TeleReferenceValue teleReferenceValue = (TeleReferenceValue) other;
        return reference.equals(teleReferenceValue.reference);
    }

    @Override
    protected int compareSameKind(ReferenceValue other) {
        // TODO: It seems impossible to find a way to deterministically order the identities of two objects.
        //       So, Value should not implement Comparable!
        throw TeleError.unexpected("trying to compare reference values");
    }

    @Override
    public int hashCode() {
        return reference.hashCode();
    }

    @Override
    public String toString() {
        if (reference.isZero()) {
            return "null";
        }
        return reference.toString();
    }

    @Override
    public Object asBoxedJavaValue() {
        if (reference.isLocal()) {
            return reference.toJava();
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public Object unboxObject() {
        if (reference.isLocal()) {
            return reference.toJava();
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public Reference asReference() {
        return reference;
    }

    @Override
    public Object asObject() {
        if (reference.isLocal()) {
            return reference.toJava();
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public Word toWord() {
        return teleVM.referenceScheme().toOrigin(reference);
    }

    @Override
    public WordWidth signedEffectiveWidth() {
        return Word.widthValue();
    }

    @Override
    public WordWidth unsignedEffectiveWidth() {
        return Word.widthValue();
    }

    @Override
    public byte[] toBytes(DataModel dataModel) {
        return dataModel.toBytes(reference.toOrigin());
    }

    @Override
    public ClassActor getClassActor() {
        if (reference.isLocal()) {
            return ClassActor.fromJava(reference.toJava().getClass());
        }
        return teleVM.classRegistry().makeClassActorForTypeOf(reference);
    }
}
