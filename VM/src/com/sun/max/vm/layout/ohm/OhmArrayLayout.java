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

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.HeaderField;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public class OhmArrayLayout extends OhmGeneralLayout implements ArrayLayout {

    /**
     * The cell offset of the word in the header containing the array length.
     */
    public final int lengthOffset;

    public final int headerSize;

    public final Kind elementKind;

    @INLINE
    public final int headerSize() {
        return headerSize;
    }

    public HeaderField[] headerFields() {
        return new HeaderField[] {HeaderField.HUB, HeaderField.MISC, HeaderField.LENGTH};
    }

    OhmArrayLayout(Kind elementKind) {
        lengthOffset = miscOffset + Word.size();
        headerSize = lengthOffset + Word.size();
        this.elementKind = elementKind;
    }

    public boolean isArrayLayout() {
        return true;
    }

    @INLINE
    public final Size getArraySize(Kind kind, int length) {
        return Size.fromInt(kind.width.numberOfBytes).times(length).plus(headerSize).wordAligned();
    }

    @Override
    public Offset getOffsetFromOrigin(HeaderField headerField) {
        if (headerField == HeaderField.LENGTH) {
            return Offset.fromInt(lengthOffset);
        }
        return super.getOffsetFromOrigin(headerField);
    }

    public int arrayLengthOffset() {
        return lengthOffset;
    }

    @INLINE
    public final int readLength(Accessor accessor) {
        return accessor.readInt(lengthOffset);
    }

    @INLINE
    public final void writeLength(Accessor accessor, int length) {
        accessor.writeInt(lengthOffset, length);
    }

    @INLINE
    public final Kind elementKind() {
        return elementKind;
    }

    public Layout.Category category() {
        return Layout.Category.ARRAY;
    }

    @Override
    public final boolean isReferenceArrayLayout() {
        final Kind rawKind = elementKind;
        return rawKind.isReference;
    }

    @INLINE
    public final int elementSize() {
        return elementKind().width.numberOfBytes;
    }

    @INLINE
    protected final int originDisplacement() {
        return headerSize();
    }

    @INLINE
    public final Offset getElementOffsetFromOrigin(int index) {
        return getElementOffsetInCell(index);
    }

    @INLINE
    public final Offset getElementOffsetInCell(int index) {
        // Converting to 'Offset' before multiplication to avoid overflow:
        return Offset.fromInt(index).times(elementSize()).plus(headerSize());
    }

    @INLINE
    public final Size getArraySize(int length) {
        return getElementOffsetInCell(length).aligned().asSize();
    }

    @INLINE
    public final Size specificSize(Accessor accessor) {
        return getArraySize(readLength(accessor));
    }

    @HOSTED_ONLY
    @Override
    public void visitHeader(ObjectCellVisitor visitor, Object array) {
        super.visitHeader(visitor, array);
        visitor.visitHeaderField(lengthOffset, "length", JavaTypeDescriptor.INT, IntValue.from(ArrayAccess.readArrayLength(array)));
    }

    @HOSTED_ONLY
    private void visitElements(ObjectCellVisitor visitor, Object array) {
        final int length = Array.getLength(array);
        final Hub hub = ObjectAccess.readHub(array);
        final Kind elementKind = hub.classActor.componentClassActor().kind;
        if (elementKind.isReference) {
            for (int i = 0; i < length; i++) {
                final Object object = Array.get(array, i);
                visitor.visitElement(getElementOffsetInCell(i).toInt(), i, ReferenceValue.from(object));
            }
        } else {
            for (int i = 0; i < length; i++) {
                final Object boxedJavaValue = Array.get(array, i);
                final Value value = elementKind.asValue(boxedJavaValue);
                visitor.visitElement(getElementOffsetInCell(i).toInt(), i, value);
            }
        }
    }

    @HOSTED_ONLY
    public void visitObjectCell(Object array, ObjectCellVisitor visitor) {
        visitHeader(visitor, array);
        visitElements(visitor, array);
    }

    @HOSTED_ONLY
    public Value readValue(Kind kind, ObjectMirror mirror, int offset) {
        if (offset == lengthOffset) {
            return IntValue.from(mirror.readArrayLength());
        }
        final Value value = readHeaderValue(mirror, offset);
        if (value != null) {
            return value;
        }
        assert kind.isPrimitiveOfSameSizeAs(elementKind);
        final int index = (offset - headerSize()) / kind.width.numberOfBytes;
        return mirror.readElement(kind, index);
    }

    @HOSTED_ONLY
    public void writeValue(Kind kind, ObjectMirror mirror, int offset, Value value) {
        assert kind.isPrimitiveOfSameSizeAs(value.kind());
        if (offset == lengthOffset) {
            mirror.writeArrayLength(value);
            return;
        }
        if (writeHeaderValue(mirror, offset, value)) {
            return;
        }
        assert kind.isPrimitiveOfSameSizeAs(elementKind);
        final int index = (offset - headerSize()) / elementSize();
        mirror.writeElement(kind, index, value);
    }

    public void copyElements(Accessor src, int srcIndex, Object dst, int dstIndex, int length) {
        src.copyElements(originDisplacement(), srcIndex, dst, dstIndex, length);
    }

    @INLINE public final boolean   getBoolean(Accessor accessor, int index) { return accessor.getBoolean(originDisplacement(), index); }
    @INLINE public final byte      getByte(Accessor accessor, int index) { return accessor.getByte(originDisplacement(), index); }
    @INLINE public final char      getChar(Accessor accessor, int index) { return accessor.getChar(originDisplacement(), index); }
    @INLINE public final short     getShort(Accessor accessor, int index) { return accessor.getShort(originDisplacement(), index);  }
    @INLINE public final int       getInt(Accessor accessor, int index) { return accessor.getInt(originDisplacement(), index); }
    @INLINE public final float     getFloat(Accessor accessor, int index) { return accessor.getFloat(originDisplacement(), index); }
    @INLINE public final long      getLong(Accessor accessor, int index) { return accessor.getLong(originDisplacement(), index); }
    @INLINE public final double    getDouble(Accessor accessor, int index) { return accessor.getDouble(originDisplacement(), index); }
    @INLINE public final Word      getWord(Accessor accessor, int index) { return accessor.getWord(originDisplacement(), index); }
    @INLINE public final Reference getReference(Accessor accessor, int index) { return accessor.getReference(originDisplacement(), index); }

    @INLINE public final void setBoolean(Accessor accessor, int index, boolean value) { accessor.setBoolean(originDisplacement(), index, value); }
    @INLINE public final void setByte(Accessor accessor, int index, byte value) {  accessor.setByte(originDisplacement(), index, value); }
    @INLINE public final void setChar(Accessor accessor, int index, char value) { accessor.setChar(originDisplacement(), index, value); }
    @INLINE public final void setShort(Accessor accessor, int index, short value) { accessor.setShort(originDisplacement(), index, value); }
    @INLINE public final void setInt(Accessor accessor, int index, int value) { accessor.setInt(originDisplacement(), index, value); }
    @INLINE public final void setFloat(Accessor accessor, int index, float value) { accessor.setFloat(originDisplacement(), index, value); }
    @INLINE public final void setLong(Accessor accessor, int index, long value) { accessor.setLong(originDisplacement(), index, value); }
    @INLINE public final void setDouble(Accessor accessor, int index, double value) { accessor.setDouble(originDisplacement(), index, value); }
    @INLINE public final void setWord(Accessor accessor, int index, Word value) { accessor.setWord(originDisplacement(), index, value); }
    @INLINE public final void setReference(Accessor accessor, int index, Reference element) { accessor.setReference(originDisplacement(), index, element); }
}
