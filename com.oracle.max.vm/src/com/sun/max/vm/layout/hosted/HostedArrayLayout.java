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
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.HeaderField;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
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
        throw ProgramError.unexpected();
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
        throw ProgramError.unexpected();
    }

    public void copyElements(Accessor src, int srcIndex, Object dst, int dstIndex, int length) {
        throw ProgramError.unexpected();
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
