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

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.*;
import com.sun.max.vm.layout.SpecificLayout.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Origin, Header, Mixed.
 *
 * Header words in tuples: hub, misc Header words in arrays: hub, misc, length
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

    public boolean isArrayLayout() {
        return false;
    }

    public boolean isReferenceArrayLayout() {
        return false;
    }

    private final GripScheme _gripScheme;

    /**
     * The offset of the hub pointer.
     */
    final int _hubOffset = 0;

    /**
     * The offset of the extras (such as monitor and hashCode info).
     */
    final int _miscOffset;

    final int _arrayLengthOffset;

    public OhmGeneralLayout(GripScheme gripScheme) {
        _gripScheme = gripScheme;
        _miscOffset = _hubOffset + Word.size();
        _arrayLengthOffset = _miscOffset + Word.size();
    }

    @INLINE
    public final GripScheme gripScheme() {
        return _gripScheme;
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
            return Offset.fromInt(_hubOffset);
        } else if (headerField == HeaderField.MISC) {
            return Offset.fromInt(_miscOffset);
        }
        throw new IllegalArgumentException(getClass().getSimpleName() + " does not know about header field: " + headerField);
    }

    @INLINE
    private Hub getHub(Accessor accessor) {
        return UnsafeLoophole.cast(readHubReference(accessor).toJava());
    }

    @INLINE
    public final Layout.Category category(Accessor accessor) {
        final Hub hub = getHub(accessor);
        return hub.layoutCategory();
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
        return getHub(accessor).specificLayout();
    }

    @INLINE
    public final Size size(Accessor accessor) {
        final Hub hub = getHub(accessor);
        switch (hub.layoutCategory()) {
            case TUPLE:
                return Layout.tupleLayout().specificSize(accessor);
            case ARRAY:
                return Layout.arrayHeaderLayout().getArraySize(hub.elementKind(), Layout.arrayHeaderLayout().readLength(accessor));
            case HYBRID:
                return Layout.hybridLayout().specificSize(accessor);
        }
        ProgramError.unknownCase();
        return Size.zero();
    }

    @INLINE
    public final Reference readHubReference(Accessor accessor) {
        return accessor.readReference(_hubOffset);
    }

    @INLINE
    public final Word readHubReferenceAsWord(Accessor accessor) {
        return accessor.readWord(_hubOffset);
    }

    @INLINE
    public final void writeHubReference(Accessor accessor, Reference referenceClassReference) {
        accessor.writeReference(_hubOffset, referenceClassReference);
    }

    @INLINE
    public final Word readMisc(Accessor accessor) {
        return accessor.readWord(_miscOffset);
    }

    @INLINE
    public final void writeMisc(Accessor accessor, Word value) {
        accessor.writeWord(_miscOffset, value);
    }

    @INLINE
    public final Word compareAndSwapMisc(Accessor accessor, Word suspectedValue, Word newValue) {
        return accessor.compareAndSwapWord(_miscOffset, suspectedValue, newValue);
    }

    @INLINE
    public final Grip forwarded(Grip grip) {
        if (grip.isMarked()) {
            return grip.readGrip(_hubOffset).unmarked();
        }
        return grip;
    }

    @INLINE
    public final Grip readForwardGrip(Accessor accessor) {
        final Grip forwardGrip = accessor.readGrip(_hubOffset);
        if (forwardGrip.isMarked()) {
            return forwardGrip.unmarked();
        }

        // no forward reference has been stored
        return _gripScheme.zero();
    }

    @INLINE
    public final Grip readForwardGripValue(Accessor accessor) {
        final Grip forwardGrip = accessor.readGrip(_hubOffset);
        if (forwardGrip.isMarked()) {
            return forwardGrip.unmarked();
        }
        // no forward reference has been stored
        //return the value (instead of zero) to be used in CAS
        return forwardGrip;
    }

    @INLINE
    public final void writeForwardGrip(Accessor accessor, Grip forwardGrip) {
        accessor.writeGrip(_hubOffset, forwardGrip.marked());
    }

    @INLINE
    public final Grip compareAndSwapForwardGrip(Accessor accessor, Grip suspectedGrip, Grip forwardGrip) {
        return UnsafeLoophole.wordToGrip(accessor.compareAndSwapWord(_hubOffset, UnsafeLoophole.gripToWord(suspectedGrip), UnsafeLoophole.gripToWord(forwardGrip.marked())));
    }

    public void visitHeader(ObjectCellVisitor visitor, Object object) {
        final Hub hub = HostObjectAccess.readHub(object);
        visitor.visitHeaderField(_hubOffset, "hub", JavaTypeDescriptor.forJavaClass(hub.getClass()), ReferenceValue.from(hub));
        visitor.visitHeaderField(_miscOffset, "misc", JavaTypeDescriptor.WORD, new WordValue(gripScheme().vmConfiguration().monitorScheme().createMisc(object)));
    }

    public int getHubReferenceOffsetInCell() {
        return _hubOffset;
    }

    protected Value readHeaderValue(ObjectMirror mirror, int offset) {
        if (offset == _hubOffset) {
            return mirror.readHub();
        } else if (offset == _miscOffset) {
            return mirror.readMisc();
        }
        return null;
    }

    protected boolean writeHeaderValue(ObjectMirror mirror, int offset, Value value) {
        if (offset == _hubOffset) {
            mirror.writeHub(value);
        } else if (offset == _miscOffset) {
            mirror.writeMisc(value);
        } else {
            return false;
        }
        return true;
    }

}
