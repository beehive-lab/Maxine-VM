/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.layout.hom;

import static com.sun.max.vm.VMConfiguration.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.HeaderField;
import com.sun.max.vm.layout.SpecificLayout.ObjectCellVisitor;
import com.sun.max.vm.layout.SpecificLayout.ObjectMirror;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Header, Origin, Mixed.
 *
 * Tuple header words:         hub, misc
 * Array header words: length, hub, misc
 *
 * The low bit in the misc word can potentially sometimes be non-zero.
 * The low bit in the hub word is always zero.
 *
 * When scanning cells and encountering the first word of a cell,
 * one can tell whether a tuple or an array follows by testing the low bit:
 * - if it's zero, this is the beginning of a tuple cell,
 * - if it's one, this is the beginning of an array cell.
 *
 * To support this, array length is always encoded in the length word
 * by shifting 1 to the left and then adding 1.
 *
 * See the package level documentation for a more detailed description.
 */
public class HomGeneralLayout extends AbstractLayout implements GeneralLayout {

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

    /**
     * The offset of the hub pointer.
     */
    final int hubOffset;

    /**
     * The offset of the extras (i.e. monitor and hashcode info).
     */
    final int miscOffset;

    /**
     * The offset of the array length.
     */
    protected final int arrayLengthOffset;

    public HomGeneralLayout() {
        this.miscOffset = 0 - Word.size();
        this.hubOffset = miscOffset - Word.size();
        this.arrayLengthOffset = hubOffset - Word.size();
    }

    @INLINE
    public Pointer cellToOrigin(Pointer cell) {
        return cell.readWord(0).asAddress().isBitSet(0) ? cell.plus(-arrayLengthOffset) : cell.plus(-hubOffset);
    }

    @ACCESSOR(Pointer.class)
    @INLINE
    public Pointer originToCell(Pointer origin) {
        return !isTuple(origin) ? origin.plus(arrayLengthOffset) : origin.plus(hubOffset);
    }

    public Offset getOffsetFromOrigin(HeaderField headerField) {
        if (headerField == HeaderField.HUB) {
            return Offset.fromInt(hubOffset);
        } else if (headerField == HeaderField.MISC) {
            return Offset.fromInt(miscOffset);
        }
        throw new IllegalArgumentException(getClass().getSimpleName() + " does not know about header field: " + headerField);
    }

    @INLINE
    private Hub getHub(Accessor accessor) {
        return UnsafeCast.asHub(readHubReference(accessor).toJava());
    }

    public Layout.Category category(Accessor accessor) {
        final Hub hub = getHub(accessor);
        return hub.layoutCategory;
    }

    @INLINE
    public final boolean isArray(Accessor accessor) {
        return specificLayout(accessor).isArrayLayout();
    }

    @INLINE
    public final boolean isTuple(Accessor accessor) {
        return specificLayout(accessor).isTupleLayout();
    }

    @INLINE
    public final boolean isHybrid(Accessor accessor) {
        return specificLayout(accessor).isHybridLayout();
    }

    @INLINE
    public final SpecificLayout specificLayout(Accessor accessor) {
        return getHub(accessor).specificLayout;
    }

    @INLINE
    public final Size size(Accessor accessor) {
        final Hub hub = getHub(accessor);
        switch (hub.layoutCategory) {
            case TUPLE:
                return Layout.tupleLayout().specificSize(accessor);
            case ARRAY:
                return Layout.arrayLayout().getArraySize(hub.classActor.componentClassActor().kind, Layout.arrayLayout().readLength(accessor));
            case HYBRID:
                return Layout.hybridLayout().specificSize(accessor);
        }
        throw ProgramError.unknownCase();
    }

    @INLINE
    public final Reference readHubReference(Accessor accessor) {
        return accessor.readReference(hubOffset);
    }

    @INLINE
    public final Word readHubReferenceAsWord(Accessor accessor) {
        return accessor.readWord(hubOffset);
    }

    @INLINE
    public final void writeHubReference(Accessor accessor, Reference hub) {
        accessor.writeReference(hubOffset, hub);
    }

    @INLINE
    public final Word readMisc(Accessor accessor) {
        return accessor.readWord(miscOffset);
    }

    @INLINE
    public final void writeMisc(Accessor accessor, Word value) {
        accessor.writeWord(miscOffset, value);
    }

    @INLINE
    public final Word compareAndSwapMisc(Accessor accessor, Word expectedValue, Word newValue) {
        return accessor.compareAndSwapWord(miscOffset, expectedValue, newValue);
    }

    @INLINE
    public final Reference forwarded(Reference ref) {
        if (ref.isMarked()) {
            return ref.readReference(hubOffset).unmarked();
        }
        return ref;
    }

    @INLINE
    public final Reference readForwardRef(Accessor accessor) {
        final Reference forwardRef = accessor.readReference(hubOffset);
        if (forwardRef.isMarked()) {
            return forwardRef.unmarked();
        }
        // no forward reference has been stored
        return Reference.zero();
    }

    @INLINE
    public final Reference readForwardRefValue(Accessor accessor) {
        final Reference forwardRef = accessor.readReference(hubOffset);
        if (forwardRef.isMarked()) {
            return forwardRef.unmarked();
        }
        // no forward reference has been stored
        return Reference.zero();
    }

    @INLINE
    public final void writeForwardRef(Accessor accessor, Reference forwardRef) {
        accessor.writeReference(hubOffset, forwardRef.marked());
    }

    @INLINE
    public final Reference compareAndSwapForwardRef(Accessor accessor, Reference suspectedRef, Reference forwardRef) {
        return accessor.compareAndSwapReference(hubOffset, suspectedRef, forwardRef.marked());
    }

    @HOSTED_ONLY
    public void visitHeader(ObjectCellVisitor visitor, Object object) {
        final Hub hub = ObjectAccess.readHub(object);
        final int origin = hub.specificLayout.headerSize();
        visitor.visitHeaderField(origin + hubOffset, "hub", JavaTypeDescriptor.forJavaClass(hub.getClass()), ReferenceValue.from(hub));
        visitor.visitHeaderField(origin + miscOffset, "misc", JavaTypeDescriptor.WORD, new WordValue(vmConfig().monitorScheme().createMisc(object)));
    }

    @HOSTED_ONLY
    protected Value readHeaderValue(ObjectMirror mirror, int offset) {
        if (offset == hubOffset) {
            return mirror.readHub();
        } else if (offset == arrayLengthOffset) {
            return WordValue.from(HomArrayLayout.lengthToWord(mirror.readArrayLength()));
        } else if (offset == miscOffset) {
            return mirror.readMisc();
        }
        return null;
    }

    @HOSTED_ONLY
    protected boolean writeHeaderValue(ObjectMirror mirror, int offset, Value value) {
        if (offset == hubOffset) {
            mirror.writeHub(value);
        } else if (offset == arrayLengthOffset) {
            mirror.writeArrayLength(value);
        } else if (offset == miscOffset) {
            mirror.writeMisc(value);
        } else {
            return false;
        }
        return true;
    }
}
