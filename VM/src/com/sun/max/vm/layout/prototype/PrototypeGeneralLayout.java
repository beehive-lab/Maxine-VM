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
package com.sun.max.vm.layout.prototype;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.*;
import com.sun.max.vm.layout.SpecificLayout.*;
import com.sun.max.vm.reference.*;

/**
 * @author Bernd Mathiske
 */
public class PrototypeGeneralLayout extends AbstractLayout  implements GeneralLayout {

    public boolean isTupleLayout() {
        return false;
    }

    public boolean isHybridLayout() {
        return false;
    }

    public boolean isArrayLayout() {
        return false;
    }

    public boolean isReferenceArrayLayout() {
        return false;
    }

    private final GripScheme gripScheme;

    public PrototypeGeneralLayout(GripScheme gripScheme) {
        this.gripScheme = gripScheme;
    }

    public GripScheme gripScheme() {
        return gripScheme;
    }

    public Pointer cellToOrigin(Pointer cell) {
        throw ProgramError.unexpected();
    }

    public Pointer originToCell(Pointer origin) {
        throw ProgramError.unexpected();
    }

    public boolean isArray(Accessor accessor) {
        final Reference reference = (Reference) accessor;
        return reference.toJava().getClass().isArray();
    }

    public boolean isHybrid(Accessor accessor) {
        final Reference reference = (Reference) accessor;
        return Hub.class.isAssignableFrom(reference.toJava().getClass());
    }

    public boolean isTuple(Accessor accessor) {
        final Reference reference = (Reference) accessor;
        return !isArray(reference) && !isHybrid(reference);
    }

    @INLINE
    public final Layout.Category category(Accessor accessor) {
        if (isArray(accessor)) {
            return Layout.Category.ARRAY;
        }
        if (isHybrid(accessor)) {
            return Layout.Category.HYBRID;
        }
        return Layout.Category.TUPLE;
    }

    public SpecificLayout specificLayout(Accessor accessor) {
        throw ProgramError.unexpected();
    }

    public Size size(Accessor accessor) {
        return specificLayout(accessor).size(accessor);
    }

    public Reference readHubReference(Accessor accessor) {
        return Reference.fromJava(readReferenceClassActor(accessor));
    }

    public Word readHubReferenceAsWord(Accessor accessor) {
        throw ProgramError.unexpected();
    }

    public ReferenceClassActor readReferenceClassActor(Accessor accessor) {
        final Reference reference = (Reference) accessor;
        return (ReferenceClassActor) ClassActor.fromJava(reference.toJava().getClass());
    }

    public void writeHubReference(Accessor accessor, Reference referenceClassReference) {
        ProgramError.unexpected();
    }

    public Word readMisc(Accessor accessor) {
        throw ProgramError.unexpected();
    }

    public final void writeMisc(Accessor accessor, Word value) {
        ProgramError.unexpected();
    }

    public final Word compareAndSwapMisc(Accessor accessor, Word expectedValue, Word newValue) {
        throw ProgramError.unexpected();
    }

    public Grip forwarded(Grip reference) {
        throw ProgramError.unexpected();
    }

    public Grip readForwardGrip(Accessor accessor) {
        throw ProgramError.unexpected();
    }

    public Grip readForwardGripValue(Accessor accessor) {
        throw ProgramError.unexpected();
    }

    public void writeForwardGrip(Accessor accessor, Grip forwardGrip) {
        ProgramError.unexpected();
    }

    public Grip compareAndSwapForwardGrip(Accessor accessor, Grip suspectedGrip, Grip forwardGrip) {
        throw ProgramError.unexpected();
    }

    public void visitObjectCell(Object object, ObjectCellVisitor visitor) {
        ProgramError.unexpected();
    }

    public Offset getOffsetFromOrigin(HeaderField headerField) {
        throw ProgramError.unexpected();
    }

    public int getHubReferenceOffsetInCell() {
        throw ProgramError.unexpected();
    }

    public boolean equals(Pointer origin1, Pointer origin2) {
        throw ProgramError.unexpected();
    }
}
