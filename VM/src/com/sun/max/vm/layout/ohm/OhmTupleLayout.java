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
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public final class OhmTupleLayout extends OhmGeneralLayout implements TupleLayout {

    public Layout.Category category() {
        return Layout.Category.TUPLE;
    }

    @Override
    public boolean isTupleLayout() {
        return true;
    }

    @INLINE
    public Size specificSize(Accessor accessor) {
        final Hub hub = UnsafeLoophole.cast(readHubReference(accessor).toJava());
        return hub.tupleSize;
    }

    public OhmTupleLayout(GripScheme gripScheme) {
        super(gripScheme);
    }

    private final int headerSize = 2 * Word.size();

    @INLINE
    public int headerSize() {
        return headerSize;
    }

    public HeaderField[] headerFields() {
        return new HeaderField[] {HeaderField.HUB, HeaderField.MISC};
    }

    @INLINE
    public int getFieldOffsetInCell(FieldActor fieldActor) {
        return fieldActor.offset();
    }

    private static final int INVALID_OFFSET = -1;

    private static boolean setInvalidOffsets(FieldActor[] fieldActors) {
        for (FieldActor fieldActor : fieldActors) {
            fieldActor.setOffset(INVALID_OFFSET);
        }
        return true;
    }

    private static boolean hasValidOffsets(FieldActor[] fieldActors) {
        for (FieldActor fieldActor : fieldActors) {
            if (fieldActor.offset() == INVALID_OFFSET) {
                return false;
            }
        }
        return true;
    }

    private int fillAlignmentGap(FieldActor[] fieldActors, int offset, int nAlignmentBytes) {
        final int nBytesToFill = nAlignmentBytes - (offset % nAlignmentBytes);
        assert nBytesToFill > 0;
        int scale = nAlignmentBytes;
        int currentOffset = offset;
        while (scale >= 1) {
            for (FieldActor fieldActor : fieldActors) {
                if (scale > nBytesToFill) {
                    break;
                }
                if (fieldActor.offset() == INVALID_OFFSET && fieldActor.kind.width.numberOfBytes == scale) {
                    fieldActor.setOffset(currentOffset);
                    currentOffset += scale;
                    assert nBytesToFill >= 0;
                    if (nBytesToFill == 0) {
                        assert currentOffset % nAlignmentBytes == 0;
                        return currentOffset;
                    }
                }
            }
            scale >>= 1;
        }
        return Ints.roundUp(currentOffset, nAlignmentBytes);
    }


    Size layoutFields(ClassActor superClassActor, FieldActor[] fieldActors, int headerSize) {
        setInvalidOffsets(fieldActors);
        final int nAlignmentBytes = Word.size();
        int offset = (superClassActor == null || superClassActor.typeDescriptor == JavaTypeDescriptor.HYBRID) ? headerSize : superClassActor.dynamicTupleSize().toInt();
        if (Size.fromInt(offset).dividedBy(nAlignmentBytes).toInt() != 0) {
            offset = fillAlignmentGap(fieldActors, offset, nAlignmentBytes);
        }
        for (int scale = 8; scale >= 1; scale >>= 1) {
            for (FieldActor fieldActor : fieldActors) {
                if (fieldActor.offset() == INVALID_OFFSET && fieldActor.kind.width.numberOfBytes == scale) {
                    fieldActor.setOffset(offset);
                    offset += scale;
                }
            }
        }
        assert hasValidOffsets(fieldActors);
        offset = Ints.roundUp(offset, nAlignmentBytes);
        return Size.fromInt(offset);
    }

    public Size layoutFields(ClassActor superClassActor, FieldActor[] fieldActors) {
        return layoutFields(superClassActor, fieldActors, headerSize());
    }

    @PROTOTYPE_ONLY
    private void visitFields(ObjectCellVisitor visitor, Object tuple, FieldActor[] fieldActors) {
        for (FieldActor fieldActor : fieldActors) {
            final Value value = HostTupleAccess.readValue(tuple, fieldActor);
            visitor.visitField(getFieldOffsetInCell(fieldActor), fieldActor.name, fieldActor.descriptor(), value);
        }
    }

    @PROTOTYPE_ONLY
    void visitFields(ObjectCellVisitor visitor, Object tuple) {
        final Hub hub = HostObjectAccess.readHub(tuple);
        ClassActor classActor = hub.classActor;
        if (hub instanceof StaticHub) {
            visitFields(visitor, tuple, classActor.localStaticFieldActors());
        } else {
            do {
                visitFields(visitor, tuple, classActor.localInstanceFieldActors());
                classActor = classActor.superClassActor;
            } while (classActor != null);
        }
    }

    @PROTOTYPE_ONLY
    public void visitObjectCell(Object tuple, ObjectCellVisitor visitor) {
        visitHeader(visitor, tuple);
        visitFields(visitor, tuple);
    }

    @PROTOTYPE_ONLY
    public Value readValue(Kind kind, ObjectMirror mirror, int offset) {
        final Value value = readHeaderValue(mirror, offset);
        if (value != null) {
            return value;
        }
        return mirror.readField(offset);
    }

    @PROTOTYPE_ONLY
    public void writeValue(Kind kind, ObjectMirror mirror, int offset, Value value) {
        if (writeHeaderValue(mirror, offset, value)) {
            return;
        }
        mirror.writeField(offset, value);
    }

}
