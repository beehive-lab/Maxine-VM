/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
/*VCSID=4db5fb46-e2e9-4aa7-81a5-2a0e13ac9265*/
package com.sun.max.vm.reference.direct;

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.VMConfiguration.*;
import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.oracle.graal.replacements.Snippet.Fold;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.reference.*;

/**
 * References are direct pointers in this scheme.
 */
public final class DirectReferenceScheme extends AbstractVMScheme implements ReferenceScheme {

    @HOSTED_ONLY
    public DirectReferenceScheme() {
    }

    @INLINE
    public Reference fromOrigin(Pointer origin) {
        return toReference(origin);
    }

    @INTRINSIC(UNSAFE_CAST)
    private static native Reference toReference(Pointer origin);

    @Fold
    private static HeapScheme heapScheme() {
        return vmConfig().heapScheme();
    }

    @INLINE
    public Reference fromJava(Object object) {
        return toReference(object);
    }

    @INTRINSIC(UNSAFE_CAST)
    private static native Reference toReference(Object origin);

    @INLINE
    public Object toJava(Reference reference) {
        return reference;
    }

    @INLINE
    public Pointer toOrigin(Reference ref) {
        return toWord(ref).asPointer();
    }

    @INTRINSIC(UNSAFE_CAST)
    private static native Word toWord(Reference ref);

    @INLINE
    public byte readByte(Reference ref, Offset offset) {
        return toOrigin(ref).readByte(offset);
    }

    @INLINE
    public byte readByte(Reference ref, int offset) {
        return toOrigin(ref).readByte(offset);
    }

    @INLINE
    public byte getByte(Reference ref, int displacement, int index) {
        return toOrigin(ref).getByte(displacement, index);
    }

    @INLINE
    public boolean readBoolean(Reference ref, Offset offset) {
        return toOrigin(ref).readBoolean(offset);
    }

    @INLINE
    public boolean readBoolean(Reference ref, int offset) {
        return toOrigin(ref).readBoolean(offset);
    }

    @INLINE
    public boolean getBoolean(Reference ref, int displacement, int index) {
        return toOrigin(ref).getBoolean(displacement, index);
    }

    @INLINE
    public short readShort(Reference ref, Offset offset) {
        return toOrigin(ref).readShort(offset);
    }

    @INLINE
    public short readShort(Reference ref, int offset) {
        return toOrigin(ref).readShort(offset);
    }

    @INLINE
    public short getShort(Reference ref, int displacement, int index) {
        return toOrigin(ref).getShort(displacement, index);
    }

    @INLINE
    public char readChar(Reference ref, Offset offset) {
        return toOrigin(ref).readChar(offset);
    }

    @INLINE
    public char readChar(Reference ref, int offset) {
        return toOrigin(ref).readChar(offset);
    }

    @INLINE
    public char getChar(Reference ref, int displacement, int index) {
        return toOrigin(ref).getChar(displacement, index);
    }

    @INLINE
    public int readInt(Reference ref, Offset offset) {
        return toOrigin(ref).readInt(offset);
    }

    @INLINE
    public int readInt(Reference ref, int offset) {
        return toOrigin(ref).readInt(offset);
    }

    @INLINE
    public int getInt(Reference ref, int displacement, int index) {
        return toOrigin(ref).getInt(displacement, index);
    }

    @INLINE
    public float readFloat(Reference ref, Offset offset) {
        return toOrigin(ref).readFloat(offset);
    }

    @INLINE
    public float readFloat(Reference ref, int offset) {
        return toOrigin(ref).readFloat(offset);
    }

    @INLINE
    public float getFloat(Reference ref, int displacement, int index) {
        return toOrigin(ref).getFloat(displacement, index);
    }

    @INLINE
    public long readLong(Reference ref, Offset offset) {
        return toOrigin(ref).readLong(offset);
    }

    @INLINE
    public long readLong(Reference ref, int offset) {
        return toOrigin(ref).readLong(offset);
    }

    @INLINE
    public long getLong(Reference ref, int displacement, int index) {
        return toOrigin(ref).getLong(displacement, index);
    }

    @INLINE
    public double readDouble(Reference ref, Offset offset) {
        return toOrigin(ref).readDouble(offset);
    }

    @INLINE
    public double readDouble(Reference ref, int offset) {
        return toOrigin(ref).readDouble(offset);
    }

    @INLINE
    public double getDouble(Reference ref, int displacement, int index) {
        return toOrigin(ref).getDouble(displacement, index);
    }

    @INLINE
    public Word readWord(Reference ref, Offset offset) {
        return toOrigin(ref).readWord(offset);
    }

    @INLINE
    public Word readWord(Reference ref, int offset) {
        return toOrigin(ref).readWord(offset);
    }

    @INLINE
    public Word getWord(Reference ref, int displacement, int index) {
        return toOrigin(ref).getWord(displacement, index);
    }

    @INLINE
    public Reference readReference(Reference ref, Offset offset) {
        return toOrigin(ref).readReference(offset);
    }

    @INLINE
    public Reference readReference(Reference ref, int offset) {
        return toOrigin(ref).readReference(offset);
    }

    @INLINE
    public Reference getReference(Reference ref, int displacement, int index) {
        return toOrigin(ref).getReference(displacement, index);
    }

    @INLINE
    public void writeByte(Reference ref, Offset offset, byte value) {
        toOrigin(ref).writeByte(offset, value);
    }

    @INLINE
    public void writeByte(Reference ref, int offset, byte value) {
        toOrigin(ref).writeByte(offset, value);
    }

    @INLINE
    public void setByte(Reference ref, int displacement, int index, byte value) {
        toOrigin(ref).setByte(displacement, index, value);
    }

    @INLINE
    public void writeBoolean(Reference ref, Offset offset, boolean value) {
        toOrigin(ref).writeBoolean(offset, value);
    }

    @INLINE
    public void writeBoolean(Reference ref, int offset, boolean value) {
        toOrigin(ref).writeBoolean(offset, value);
    }

    @INLINE
    public void setBoolean(Reference ref, int displacement, int index, boolean value) {
        toOrigin(ref).setBoolean(displacement, index, value);
    }

    @INLINE
    public void writeShort(Reference ref, Offset offset, short value) {
        toOrigin(ref).writeShort(offset, value);
    }

    @INLINE
    public void writeShort(Reference ref, int offset, short value) {
        toOrigin(ref).writeShort(offset, value);
    }

    @INLINE
    public void setShort(Reference ref, int displacement, int index, short value) {
        toOrigin(ref).setShort(displacement, index, value);
    }

    @INLINE
    public void writeChar(Reference ref, Offset offset, char value) {
        toOrigin(ref).writeChar(offset, value);
    }

    @INLINE
    public void writeChar(Reference ref, int offset, char value) {
        toOrigin(ref).writeChar(offset, value);
    }

    @INLINE
    public void setChar(Reference ref, int displacement, int index, char value) {
        toOrigin(ref).setChar(displacement, index, value);
    }

    @INLINE
    public void writeInt(Reference ref, Offset offset, int value) {
        toOrigin(ref).writeInt(offset, value);
    }

    @INLINE
    public void writeInt(Reference ref, int offset, int value) {
        toOrigin(ref).writeInt(offset, value);
    }

    @INLINE
    public void setInt(Reference ref, int displacement, int index, int value) {
        toOrigin(ref).setInt(displacement, index, value);
    }

    @INLINE
    public void writeFloat(Reference ref, Offset offset, float value) {
        toOrigin(ref).writeFloat(offset, value);
    }

    @INLINE
    public void writeFloat(Reference ref, int offset, float value) {
        toOrigin(ref).writeFloat(offset, value);
    }

    @INLINE
    public void setFloat(Reference ref, int displacement, int index, float value) {
        toOrigin(ref).setFloat(displacement, index, value);
    }

    @INLINE
    public void writeLong(Reference ref, Offset offset, long value) {
        toOrigin(ref).writeLong(offset, value);
    }

    @INLINE
    public void writeLong(Reference ref, int offset, long value) {
        toOrigin(ref).writeLong(offset, value);
    }

    @INLINE
    public void setLong(Reference ref, int displacement, int index, long value) {
        toOrigin(ref).setLong(displacement, index, value);
    }

    @INLINE
    public void writeDouble(Reference ref, Offset offset, double value) {
        toOrigin(ref).writeDouble(offset, value);
    }

    @INLINE
    public void writeDouble(Reference ref, int offset, double value) {
        toOrigin(ref).writeDouble(offset, value);
    }

    @INLINE
    public void setDouble(Reference ref, int displacement, int index, double value) {
        toOrigin(ref).setDouble(displacement, index, value);
    }

    @INLINE
    public void writeWord(Reference ref, Offset offset, Word value) {
        toOrigin(ref).writeWord(offset, value);
    }

    @INLINE
    public void writeWord(Reference ref, int offset, Word value) {
        toOrigin(ref).writeWord(offset, value);
    }

    @INLINE
    public void setWord(Reference ref, int displacement, int index, Word value) {
        toOrigin(ref).setWord(displacement, index, value);
    }

    @INLINE
    public void writeReference(Reference ref, Offset offset, Reference value) {
        heapScheme().preWriteBarrier(ref, offset, value);
        toOrigin(ref).writeReference(offset, value);
        heapScheme().postWriteBarrier(ref, offset, value);
    }

    @INLINE
    public void writeReference(Reference ref, int offset, Reference value) {
        heapScheme().preWriteBarrier(ref, Offset.fromInt(offset), value);
        toOrigin(ref).writeReference(offset, value);
        heapScheme().postWriteBarrier(ref, Offset.fromInt(offset), value);
    }

    @INLINE
    public void setReference(Reference ref, int displacement, int index, Reference value) {
        heapScheme().preWriteBarrier(ref, displacement, index, value);
        toOrigin(ref).setReference(displacement, index, value);
        heapScheme().postWriteBarrier(ref, displacement, index, value);
    }

    @INLINE
    public int compareAndSwapInt(Reference ref, Offset offset, int expectedValue, int newValue) {
        return toOrigin(ref).compareAndSwapInt(offset, expectedValue, newValue);
    }

    @INLINE
    public int compareAndSwapInt(Reference ref, int offset, int expectedValue, int newValue) {
        return toOrigin(ref).compareAndSwapInt(offset, expectedValue, newValue);
    }

    @INLINE
    public Word compareAndSwapWord(Reference ref, Offset offset, Word expectedValue, Word newValue) {
        return toOrigin(ref).compareAndSwapWord(offset, expectedValue, newValue);
    }

    @INLINE
    public Word compareAndSwapWord(Reference ref, int offset, Word expectedValue, Word newValue) {
        return toOrigin(ref).compareAndSwapWord(offset, expectedValue, newValue);
    }

    @INLINE
    public Reference compareAndSwapReference(Reference ref, Offset offset, Reference expectedValue, Reference newValue) {
        heapScheme().preWriteBarrier(ref, offset, newValue);
        final Reference result = toOrigin(ref).compareAndSwapReference(offset, expectedValue, newValue);
        heapScheme().postWriteBarrier(ref, offset, newValue);
        return result;
    }

    @INLINE
    public Reference compareAndSwapReference(Reference ref, int offset, Reference expectedValue, Reference newValue) {
        heapScheme().preWriteBarrier(ref, Offset.fromInt(offset), newValue);
        final Reference result = toOrigin(ref).compareAndSwapReference(offset, expectedValue, newValue);
        heapScheme().postWriteBarrier(ref,  Offset.fromInt(offset), newValue);
        return result;
    }

    @HOSTED_ONLY
    public void copyElements(int displacement, Reference src, int srcIndex, Object dst, int dstIndex, int length) {
        toOrigin(src).copyElements(displacement, srcIndex, dst, dstIndex, length);
    }

    @HOSTED_ONLY
    public byte[] asBytes(Pointer origin) {
        return platform().dataModel.toBytes(origin);
    }

    @HOSTED_ONLY
    public byte[] nullAsBytes() {
        return platform().dataModel.toBytes(Word.zero());
    }

    @Override
    public Reference zero() {
        return fromOrigin(Pointer.zero());
    }

    @Override
    public boolean isZero(Reference ref) {
        return toOrigin(ref).isZero();
    }

    @Override
    public boolean isAllOnes(Reference ref) {
        return toOrigin(ref).isAllOnes();
    }

    @Override
    public boolean isMarked(Reference ref) {
        return toOrigin(ref).isBitSet(0);
    }

    @Override
    public boolean isTagged(Reference ref) {
        return toOrigin(ref).isBitSet(0);
    }

    @Override
    public Reference marked(Reference ref) {
        final Pointer origin = toOrigin(ref).bitSet(0);
        return fromOrigin(origin);
    }

    @Override
    public Reference unmarked(Reference ref) {
        final Pointer origin = toOrigin(ref).bitClear(0);
        return fromOrigin(origin);
    }
}
