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
package com.sun.max.vm.layout.hosted;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.HeaderField;
import com.sun.max.vm.layout.SpecificLayout.ObjectCellVisitor;
import com.sun.max.vm.reference.*;

/**
 */
public class HostedGeneralLayout extends AbstractLayout  implements GeneralLayout {

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

    public void writeHubReference(Accessor accessor, Reference hub) {
        throw ProgramError.unexpected();
    }

    public Word readMisc(Accessor accessor) {
        throw ProgramError.unexpected();
    }

    public final void writeMisc(Accessor accessor, Word value) {
        throw ProgramError.unexpected();
    }

    public final Word compareAndSwapMisc(Accessor accessor, Word expectedValue, Word newValue) {
        throw ProgramError.unexpected();
    }

    public Reference forwarded(Reference reference) {
        throw ProgramError.unexpected();
    }

    public Reference readForwardRef(Accessor accessor) {
        throw ProgramError.unexpected();
    }

    public Reference readForwardRefValue(Accessor accessor) {
        throw ProgramError.unexpected();
    }

    public void writeForwardRef(Accessor accessor, Reference forwardRef) {
        throw ProgramError.unexpected();
    }

    public Reference compareAndSwapForwardRef(Accessor accessor, Reference suspectedRef, Reference forwardRef) {
        throw ProgramError.unexpected();
    }

    public void visitObjectCell(Object object, ObjectCellVisitor visitor) {
        throw ProgramError.unexpected();
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
