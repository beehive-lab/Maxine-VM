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
 * @author Doug Simon
 */
public class HomArrayLayout extends HomGeneralLayout implements ArrayLayout {
    public final int headerSize;
    public final Kind elementKind;

    HomArrayLayout(Kind elementKind) {
        headerSize = -arrayLengthOffset;
        this.elementKind = elementKind;
    }

    @INLINE
    public final int headerSize() {
        return headerSize;
    }

    public HeaderField[] headerFields() {
        return new HeaderField[] {HeaderField.LENGTH, HeaderField.HUB, HeaderField.MISC};
    }

    @INLINE
    public final Size getArraySize(Kind kind, int length) {
        return Size.fromInt(kind.width.numberOfBytes).times(length).plus(headerSize).wordAligned();
    }

    @Override
    @INLINE
    public final Pointer cellToOrigin(Pointer cell) {
        return cell.plus(headerSize);
    }

    @Override
    @INLINE
    public final Pointer originToCell(Pointer origin) {
        return origin.minus(headerSize);
    }

    @Override
    public boolean isArrayLayout() {
        return true;
    }

    @Override
    public Offset getOffsetFromOrigin(HeaderField headerField) {
        if (headerField == HeaderField.LENGTH) {
            return Offset.fromInt(arrayLengthOffset);
        }
        return super.getOffsetFromOrigin(headerField);
    }

    @INLINE
    static final Word lengthToWord(int length) {
        return Address.fromInt(length).shiftedLeft(1).or(1);
    }

    @INLINE
    static int wordToLength(Word word) {
        return word.asAddress().unsignedShiftedRight(1).toInt();
    }

    public int arrayLengthOffset() {
        return arrayLengthOffset;
    }

    @INLINE
    public final int readLength(Accessor accessor) {
        return wordToLength(accessor.readWord(arrayLengthOffset));
    }

    @INLINE
    public final void writeLength(Accessor accessor, int length) {
        accessor.writeWord(arrayLengthOffset, lengthToWord(length));
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
    public final Offset getElementOffsetFromOrigin(int index) {
        // Converting to 'Offset' before multiplication to avoid overflow:
        return Offset.fromInt(index).times(elementSize());
    }

    @INLINE
    public final Offset getElementOffsetInCell(int index) {
        return getElementOffsetFromOrigin(index).plus(headerSize);
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
        final int origin = headerSize;
        visitor.visitHeaderField(origin + arrayLengthOffset, "length", JavaTypeDescriptor.WORD, new WordValue(lengthToWord(ArrayAccess.readArrayLength(array))));
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

    public int getHubReferenceOffsetInCell() {
        final int origin = headerSize;
        return origin + hubOffset;
    }

    @HOSTED_ONLY
    public Value readValue(Kind kind, ObjectMirror mirror, int offset) {
        final Value value = readHeaderValue(mirror, offset);
        if (value != null) {
            return value;
        }
        assert kind.isPrimitiveOfSameSizeAs(elementKind) : "kind: " + kind + ", elementKind: " + elementKind;
        assert offset % kind.width.numberOfBytes == 0;
        final int index = offset / kind.width.numberOfBytes;
        return mirror.readElement(kind, index);
    }

    @HOSTED_ONLY
    public void writeValue(Kind kind, ObjectMirror mirror, int offset, Value value) {
        if (writeHeaderValue(mirror, offset, value)) {
            return;
        }
        assert kind.isPrimitiveOfSameSizeAs(value.kind());
        assert offset % elementSize() == 0;
        assert kind.isPrimitiveOfSameSizeAs(elementKind);
        final int index = offset / elementSize();
        mirror.writeElement(elementKind, index, value);
    }

    public void copyElements(Accessor src, int srcIndex, Object dst, int dstIndex, int length) {
        src.copyElements(0, srcIndex, dst, dstIndex, length);
    }

    @INLINE public final boolean   getBoolean(Accessor accessor, int index) { return accessor.getBoolean(0, index); }
    @INLINE public final byte      getByte(Accessor accessor, int index) { return accessor.getByte(0, index); }
    @INLINE public final char      getChar(Accessor accessor, int index) { return accessor.getChar(0, index); }
    @INLINE public final short     getShort(Accessor accessor, int index) { return accessor.getShort(0, index);  }
    @INLINE public final int       getInt(Accessor accessor, int index) { return accessor.getInt(0, index); }
    @INLINE public final float     getFloat(Accessor accessor, int index) { return accessor.getFloat(0, index); }
    @INLINE public final long      getLong(Accessor accessor, int index) { return accessor.getLong(0, index); }
    @INLINE public final double    getDouble(Accessor accessor, int index) { return accessor.getDouble(0, index); }
    @INLINE public final Word      getWord(Accessor accessor, int index) { return accessor.getWord(0, index); }
    @INLINE public final Reference getReference(Accessor accessor, int index) { return accessor.getReference(0, index); }

    @INLINE public final void setBoolean(Accessor accessor, int index, boolean value) { accessor.setBoolean(0, index, value); }
    @INLINE public final void setByte(Accessor accessor, int index, byte value) {  accessor.setByte(0, index, value); }
    @INLINE public final void setChar(Accessor accessor, int index, char value) { accessor.setChar(0, index, value); }
    @INLINE public final void setShort(Accessor accessor, int index, short value) { accessor.setShort(0, index, value); }
    @INLINE public final void setInt(Accessor accessor, int index, int value) { accessor.setInt(0, index, value); }
    @INLINE public final void setFloat(Accessor accessor, int index, float value) { accessor.setFloat(0, index, value); }
    @INLINE public final void setLong(Accessor accessor, int index, long value) { accessor.setLong(0, index, value); }
    @INLINE public final void setDouble(Accessor accessor, int index, double value) { accessor.setDouble(0, index, value); }
    @INLINE public final void setWord(Accessor accessor, int index, Word value) { accessor.setWord(0, index, value); }
    @INLINE public final void setReference(Accessor accessor, int index, Reference element) { accessor.setReference(0, index, element); }
}
