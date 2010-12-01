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
package com.sun.max.vm.layout.ohm;

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
 * Origin, Header, Mixed.
 *
 * Header words in tuples: hub, misc
 * Header words in arrays: hub, misc, length
 *
 * See the package level documentation for a more detailed description.
 *
 * @author Bernd Mathiske
 */
public class OhmGeneralLayout extends AbstractLayout implements GeneralLayout {

    public boolean isTupleLayout() {
        return false;
    }

    public boolean isHybridLayout() {
        return false;
    }

    public boolean isReferenceArrayLayout() {
        return false;
    }

    /**
     * The offset of the hub pointer.
     */
    final int hubOffset = 0;

    /**
     * The offset of the extras (such as monitor and hashCode info).
     */
    final int miscOffset;

    final int arrayLengthOffset;

    public OhmGeneralLayout() {
        this.miscOffset = hubOffset + Word.size();
        this.arrayLengthOffset = miscOffset + Word.size();
    }

    @INLINE
    public final Pointer cellToOrigin(Pointer cell) {
        return cell;
    }

    @INLINE
    public final Pointer originToCell(Pointer origin) {
        return origin;
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

    @INLINE
    public final Layout.Category category(Accessor accessor) {
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
        ProgramError.unknownCase();
        return Size.zero();
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
        //return the value (instead of zero) to be used in CAS
        return forwardRef;
    }

    @INLINE
    public final void writeForwardRef(Accessor accessor, Reference forwardRef) {
        accessor.writeReference(hubOffset, forwardRef.marked());
    }

    @INLINE
    public final Reference compareAndSwapForwardRef(Accessor accessor, Reference suspectedRef, Reference forwardRef) {
        return Reference.fromOrigin(accessor.compareAndSwapWord(hubOffset, suspectedRef.toOrigin(), forwardRef.marked().toOrigin()).asPointer());
    }

    @HOSTED_ONLY
    public void visitHeader(ObjectCellVisitor visitor, Object object) {
        final Hub hub = ObjectAccess.readHub(object);
        visitor.visitHeaderField(hubOffset, "hub", JavaTypeDescriptor.forJavaClass(hub.getClass()), ReferenceValue.from(hub));
        visitor.visitHeaderField(miscOffset, "misc", JavaTypeDescriptor.WORD, new WordValue(vmConfig().monitorScheme().createMisc(object)));
    }

    public int getHubReferenceOffsetInCell() {
        return hubOffset;
    }

    @HOSTED_ONLY
    protected Value readHeaderValue(ObjectMirror mirror, int offset) {
        if (offset == hubOffset) {
            return mirror.readHub();
        } else if (offset == miscOffset) {
            return mirror.readMisc();
        }
        return null;
    }

    @HOSTED_ONLY
    protected boolean writeHeaderValue(ObjectMirror mirror, int offset, Value value) {
        if (offset == hubOffset) {
            mirror.writeHub(value);
        } else if (offset == miscOffset) {
            mirror.writeMisc(value);
        } else {
            return false;
        }
        return true;
    }

}
