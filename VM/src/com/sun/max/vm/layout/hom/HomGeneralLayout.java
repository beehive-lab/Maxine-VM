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
package com.sun.max.vm.layout.hom;

import com.sun.max.annotate.*;
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
 * @author Bernd Mathiske
 * @author Doug Simon
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

    private final GripScheme _gripScheme;

    /**
     * The offset of the hub pointer.
     */
    final int _hubOffset;

    /**
     * The offset of the extras (i.e. monitor and hashcode info).
     */
    final int _miscOffset;

    /**
     * The offset of the array length.
     */
    protected final int _arrayLengthOffset;

    public HomGeneralLayout(GripScheme gripScheme) {
        _gripScheme = gripScheme;
        _miscOffset = 0 - Word.size();
        _hubOffset = _miscOffset - Word.size();
        _arrayLengthOffset = _hubOffset - Word.size();
    }

    public GripScheme gripScheme() {
        return _gripScheme;
    }

    @INLINE(override = true)
    public Pointer cellToOrigin(Pointer cell) {
        return cell.isBitSet(0) ? cell.plus(-_arrayLengthOffset) : cell.plus(-_miscOffset);
    }

    @ACCESSOR(Pointer.class)
    @INLINE(override = true)
    public Pointer originToCell(Pointer origin) {
        return isArray(origin) ? origin.plus(_arrayLengthOffset) : origin.plus(_hubOffset);
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

    public Layout.Category category(Accessor accessor) {
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

    @INLINE(override = true)
    public Size size(Accessor accessor) {
        return specificLayout(accessor).size(accessor);
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
    public Grip readForwardGrip(Accessor accessor) {
        final Grip forwardGrip = accessor.readGrip(_hubOffset);
        if (forwardGrip.isMarked()) {
            return forwardGrip.unmarked();
        }
        // no forward reference has been stored
        return _gripScheme.zero();
    }

    @INLINE
    public Grip readForwardGripValue(Accessor accessor) {
        final Grip forwardGrip = accessor.readGrip(_hubOffset);
        if (forwardGrip.isMarked()) {
            return forwardGrip.unmarked();
        }
        // no forward reference has been stored
        return _gripScheme.zero();
    }

    @INLINE
    public void writeForwardGrip(Accessor accessor, Grip forwardGrip) {
        accessor.writeGrip(_hubOffset, forwardGrip.marked());
    }

    @INLINE
    public final Grip compareAndSwapForwardGrip(Accessor accessor, Grip suspectedGrip, Grip forwardGrip) {
        return UnsafeLoophole.referenceToGrip(accessor.compareAndSwapReference(_hubOffset, UnsafeLoophole.gripToReference(suspectedGrip), UnsafeLoophole.gripToReference(forwardGrip.marked())));
    }

    @PROTOTYPE_ONLY
    public void visitHeader(ObjectCellVisitor visitor, Object object) {
        final Hub hub = HostObjectAccess.readHub(object);
        final int origin = hub.specificLayout().isTupleLayout() ? -_miscOffset : -_arrayLengthOffset;
        visitor.visitHeaderField(origin + _hubOffset, "hub", JavaTypeDescriptor.forJavaClass(hub.getClass()), ReferenceValue.from(hub));
        visitor.visitHeaderField(origin + _miscOffset, "misc", JavaTypeDescriptor.WORD, new WordValue(gripScheme().vmConfiguration().monitorScheme().createMisc(object)));
    }

    protected Value readHeaderValue(ObjectMirror mirror, int offset) {
        if (offset == _hubOffset) {
            return mirror.readHub();
        } else if (offset == _arrayLengthOffset) {
            return mirror.readArrayLength();
        } else if (offset == _miscOffset) {
            return mirror.readMisc();
        }
        return null;
    }

    protected boolean writeHeaderValue(ObjectMirror mirror, int offset, Value value) {
        if (offset == _hubOffset) {
            mirror.writeHub(value);
        } else if (offset == _arrayLengthOffset) {
            mirror.writeArrayLength(value);
        } else if (offset == _miscOffset) {
            mirror.writeMisc(value);
        } else {
            return false;
        }
        return true;
    }


}
