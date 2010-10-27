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
package com.sun.max.vm.layout.hosted;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.HeaderField;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public class HostedArrayLayout extends HostedGeneralLayout implements ArrayLayout {

    public final Kind elementKind;

    public HostedArrayLayout(Kind elementKind) {
        this.elementKind = elementKind;
    }

    @Override
    public boolean isArrayLayout() {
        return true;
    }

    @INLINE
    public final Size getArraySize(Kind kind, int length) {
        throw ProgramError.unexpected("cannot compute array size in prototype layout");
    }

    @INLINE
    public final int headerSize() {
        throw ProgramError.unexpected();
    }

    public HeaderField[] headerFields() {
        throw ProgramError.unexpected();
    }

    public Kind getElementKind(Accessor accessor) {
        return ObjectAccess.readHub(accessor).classActor.componentClassActor().kind;
    }

    public int arrayLengthOffset() {
        throw ProgramError.unexpected("cannot get array length offset in prototype layout");
    }

    public int readLength(Accessor accessor) {
        final Reference reference = (Reference) accessor;
        return ArrayAccess.readArrayLength(reference.toJava());
    }

    public void writeLength(Accessor accessor, int length) {
        ProgramError.unexpected();
    }

    @INLINE
    public final int cellDataOffset() {
        throw ProgramError.unexpected();
    }

    public Kind elementKind() {
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

    public Offset getElementOffsetFromOrigin(int index) {
        throw ProgramError.unexpected("cannot compute cell offset in prototype layout");
    }

    public Offset getElementOffsetInCell(int index) {
        throw ProgramError.unexpected("cannot compute cell offset in prototype layout");
    }

    public int getElementSize() {
        return elementKind().width.numberOfBytes;
    }

    public Size getHeaderSize() {
        throw ProgramError.unexpected();
    }

    public Offset getElementOffset(int index) {
        return Offset.fromInt(index * getElementSize());
    }

    public Size getArraySize(int length) {
        throw ProgramError.unexpected();
    }

    public Size specificSize(Accessor accessor) {
        throw ProgramError.unexpected();
    }

    public Value readValue(Kind kind, ObjectMirror mirror, int offset) {
        throw ProgramError.unexpected();
    }

    public void writeValue(Kind kind, ObjectMirror mirror, int offset, Value value) {
        assert kind == value.kind();
        ProgramError.unexpected();
    }

    public void copyElements(Accessor src, int srcIndex, Object dst, int dstIndex, int length) {
        ProgramError.unexpected();
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
