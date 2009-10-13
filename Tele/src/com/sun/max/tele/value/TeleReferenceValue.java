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
/*VCSID=e74ecb84-ca6a-4129-a3b6-b9f3408cec81*/
package com.sun.max.tele.value;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.grip.*;
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
        throw ProgramError.unexpected("trying to compare reference values");
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
    public Grip asGrip() {
        return reference.grip();
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
        return teleVM.vmConfiguration().gripScheme().toOrigin(reference.grip());
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
        return teleVM.makeClassActorForTypeOf(reference);
    }
}
