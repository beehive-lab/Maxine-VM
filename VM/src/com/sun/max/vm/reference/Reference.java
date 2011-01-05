/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.reference;

import static com.sun.max.vm.VMConfiguration.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.layout.*;

/**
 * @author Bernd Mathiske
 */
public abstract class Reference implements Accessor {

    protected Reference() {
    }

    @UNSAFE
    @FOLD
    private static ReferenceScheme referenceScheme() {
        return vmConfig().referenceScheme();
    }

    @INLINE
    public static Reference fromJava(Object object) {
        return referenceScheme().fromJava(object);
    }

    @INLINE
    public final Object toJava() {
        return referenceScheme().toJava(this);
    }

    @INLINE
    public static Reference fromOrigin(Pointer origin) {
        return referenceScheme().fromOrigin(origin);
    }

    @INLINE
    public final Pointer toOrigin() {
        return referenceScheme().toOrigin(this);
    }

    @INLINE
    public final Reference readHubReference() {
        return Layout.readHubReference(this);
    }

    @INLINE
    public final boolean isZero() {
        return referenceScheme().isZero(this);
    }

    @INLINE
    public static Reference zero() {
        return referenceScheme().zero();
    }

    @INLINE
    public final boolean isAllOnes() {
        return referenceScheme().isAllOnes(this);
    }

    @INLINE(override = true)
    public boolean equals(Reference other) {
        return other == this;
    }

    @INLINE
    public final boolean isMarked() {
        return referenceScheme().isMarked(this);
    }

    @INLINE
    public final Reference marked() {
        return referenceScheme().marked(this);
    }

    @INLINE
    public final Reference unmarked() {
        return referenceScheme().unmarked(this);
    }

    @INLINE
    public final byte readByte(Offset offset) {
        return referenceScheme().readByte(this, offset);
    }

    @INLINE
    public final byte readByte(int offset) {
        return referenceScheme().readByte(this, offset);
    }

    @INLINE
    public final byte getByte(int displacement, int index) {
        return referenceScheme().getByte(this, displacement, index);
    }

    @INLINE
    public final boolean readBoolean(Offset offset) {
        return referenceScheme().readBoolean(this, offset);
    }

    @INLINE
    public final boolean readBoolean(int offset) {
        return referenceScheme().readBoolean(this, offset);
    }

    @INLINE
    public final boolean getBoolean(int displacement, int index) {
        return referenceScheme().getBoolean(this, displacement, index);
    }

    @INLINE
    public final short readShort(Offset offset) {
        return referenceScheme().readShort(this, offset);
    }

    @INLINE
    public final short readShort(int offset) {
        return referenceScheme().readShort(this, offset);
    }

    @INLINE
    public final short getShort(int displacement, int index) {
        return referenceScheme().getShort(this, displacement, index);
    }

    @INLINE
    public final char readChar(Offset offset) {
        return referenceScheme().readChar(this, offset);
    }

    @INLINE
    public final char readChar(int offset) {
        return referenceScheme().readChar(this, offset);
    }

    @INLINE
    public final char getChar(int displacement, int index) {
        return referenceScheme().getChar(this, displacement, index);
    }

    @INLINE
    public final int readInt(Offset offset) {
        return referenceScheme().readInt(this, offset);
    }

    @INLINE
    public final int readInt(int offset) {
        return referenceScheme().readInt(this, offset);
    }

    @INLINE
    public final int getInt(int displacement, int index) {
        return referenceScheme().getInt(this, displacement, index);
    }

    @INLINE
    public final float readFloat(Offset offset) {
        return referenceScheme().readFloat(this, offset);
    }

    @INLINE
    public final float readFloat(int offset) {
        return referenceScheme().readFloat(this, offset);
    }

    @INLINE
    public final float getFloat(int displacement, int index) {
        return referenceScheme().getFloat(this, displacement, index);
    }

    @INLINE
    public final long readLong(Offset offset) {
        return referenceScheme().readLong(this, offset);
    }

    @INLINE
    public final long readLong(int offset) {
        return referenceScheme().readLong(this, offset);
    }

    @INLINE
    public final long getLong(int displacement, int index) {
        return referenceScheme().getLong(this, displacement, index);
    }

    @INLINE
    public final double readDouble(Offset offset) {
        return referenceScheme().readDouble(this, offset);
    }

    @INLINE
    public final double readDouble(int offset) {
        return referenceScheme().readDouble(this, offset);
    }

    @INLINE
    public final double getDouble(int displacement, int index) {
        return referenceScheme().getDouble(this, displacement, index);
    }

    @INLINE
    public final Word readWord(Offset offset) {
        return referenceScheme().readWord(this, offset);
    }

    @INLINE
    public final Word readWord(int offset) {
        return referenceScheme().readWord(this, offset);
    }

    @INLINE
    public final Word getWord(int displacement, int index) {
        return referenceScheme().getWord(this, displacement, index);
    }

    @INLINE
    public final Reference readReference(Offset offset) {
        return referenceScheme().readReference(this, offset);
    }

    @INLINE
    public final Reference readReference(int offset) {
        return referenceScheme().readReference(this, offset);
    }

    @INLINE
    public final Reference getReference(int displacement, int index) {
        return referenceScheme().getReference(this, displacement, index);
    }

    @INLINE
    public final void writeByte(Offset offset, byte value) {
        referenceScheme().writeByte(this, offset, value);
    }

    @INLINE
    public final void writeByte(int offset, byte value) {
        referenceScheme().writeByte(this, offset, value);
    }

    @INLINE
    public final void setByte(int displacement, int index, byte value) {
        referenceScheme().setByte(this, displacement, index, value);
    }

    @INLINE
    public final void writeBoolean(Offset offset, boolean value) {
        referenceScheme().writeBoolean(this, offset, value);
    }

    @INLINE
    public final void writeBoolean(int offset, boolean value) {
        referenceScheme().writeBoolean(this, offset, value);
    }

    @INLINE
    public final void setBoolean(int displacement, int index, boolean value) {
        referenceScheme().setBoolean(this, displacement, index, value);
    }

    @INLINE
    public final void writeShort(Offset offset, short value) {
        referenceScheme().writeShort(this, offset, value);
    }

    @INLINE
    public final void writeShort(int offset, short value) {
        referenceScheme().writeShort(this, offset, value);
    }

    @INLINE
    public final void setShort(int displacement, int index, short value) {
        referenceScheme().setShort(this, displacement, index, value);
    }

    @INLINE
    public final void writeChar(Offset offset, char value) {
        referenceScheme().writeChar(this, offset, value);
    }

    @INLINE
    public final void writeChar(int offset, char value) {
        referenceScheme().writeChar(this, offset, value);
    }

    @INLINE
    public final void setChar(int displacement, int index, char value) {
        referenceScheme().setChar(this, displacement, index, value);
    }

    @INLINE
    public final void writeInt(Offset offset, int value) {
        referenceScheme().writeInt(this, offset, value);
    }

    @INLINE
    public final void writeInt(int offset, int value) {
        referenceScheme().writeInt(this, offset, value);
    }

    @INLINE
    public final void setInt(int displacement, int index, int value) {
        referenceScheme().setInt(this, displacement, index, value);
    }

    @INLINE
    public final void writeFloat(Offset offset, float value) {
        referenceScheme().writeFloat(this, offset, value);
    }

    @INLINE
    public final void writeFloat(int offset, float value) {
        referenceScheme().writeFloat(this, offset, value);
    }

    @INLINE
    public final void setFloat(int displacement, int index, float value) {
        referenceScheme().setFloat(this, displacement, index, value);
    }

    @INLINE
    public final void writeLong(Offset offset, long value) {
        referenceScheme().writeLong(this, offset, value);
    }

    @INLINE
    public final void writeLong(int offset, long value) {
        referenceScheme().writeLong(this, offset, value);
    }

    @INLINE
    public final void setLong(int displacement, int index, long value) {
        referenceScheme().setLong(this, displacement, index, value);
    }

    @INLINE
    public final void writeDouble(Offset offset, double value) {
        referenceScheme().writeDouble(this, offset, value);
    }

    @INLINE
    public final void writeDouble(int offset, double value) {
        referenceScheme().writeDouble(this, offset, value);
    }

    @INLINE
    public final void setDouble(int displacement, int index, double value) {
        referenceScheme().setDouble(this, displacement, index, value);
    }

    @INLINE
    public final void writeWord(Offset offset, Word value) {
        referenceScheme().writeWord(this, offset, value);
    }

    @INLINE
    public final void writeWord(int offset, Word value) {
        referenceScheme().writeWord(this, offset, value);
    }

    @INLINE
    public final void setWord(int displacement, int index, Word value) {
        referenceScheme().setWord(this, displacement, index, value);
    }

    @INLINE
    public final void writeReference(Offset offset, Reference value) {
        referenceScheme().writeReference(this, offset, value);
    }

    @INLINE
    public final void writeReference(int offset, Reference value) {
        referenceScheme().writeReference(this, offset, value);
    }

    @INLINE
    public final void setReference(int displacement, int index, Reference value) {
        referenceScheme().setReference(this, displacement, index, value);
    }

    /**
     * Atomically compares the contents of a memory location to a given value and, if they are the same, modifies the
     * contents of that memory location to a given new value.
     *
     * @param offset the offset from this reference of the memory location to be tested and potentially updated
     * @param expectedValue the value that must currently exist in the memory location for the update to occur
     * @param newValue the value to which the memory is updated if its current value is {@code expectedValue}
     * @return the value of the memory location before this call; if it is equal to {@code expectedValue}, then the
     *         update occurred, otherwise the update did not occur (assuming {@code expectedValue != newValue})
     */
    @INLINE
    public final int compareAndSwapInt(Offset offset, int expectedValue, int newValue) {
        return referenceScheme().compareAndSwapInt(this, offset, expectedValue, newValue);
    }

    /**
     * @see #compareAndSwapInt(Offset, int, int)
     */
    @INLINE
    public final int compareAndSwapInt(int offset, int expectedValue, int newValue) {
        return referenceScheme().compareAndSwapInt(this, offset, expectedValue, newValue);
    }

    /**
     * @see #compareAndSwapInt(Offset, int, int)
     */
    @INLINE
    public final Word compareAndSwapWord(Offset offset, Word expectedValue, Word newValue) {
        return referenceScheme().compareAndSwapWord(this, offset, expectedValue, newValue);
    }

    /**
     * @see #compareAndSwapInt(Offset, int, int)
     */
    @INLINE
    public final Word compareAndSwapWord(int offset, Word expectedValue, Word newValue) {
        return referenceScheme().compareAndSwapWord(this, offset, expectedValue, newValue);
    }

    /**
     * @see #compareAndSwapInt(Offset, int, int)
     */
    @INLINE
    public final Reference compareAndSwapReference(Offset offset, Reference expectedValue, Reference newValue) {
        return referenceScheme().compareAndSwapReference(this, offset, expectedValue, newValue);
    }

    /**
     * @see #compareAndSwapInt(Offset, int, int)
     */
    @INLINE
    public final Reference compareAndSwapReference(int offset, Reference expectedValue, Reference newValue) {
        return referenceScheme().compareAndSwapReference(this, offset, expectedValue, newValue);
    }

    @INLINE
    public final void copyElements(int displacement, int srcIndex, Object dst, int dstIndex, int length) {
        referenceScheme().copyElements(displacement, this, displacement, dst, dstIndex, length);
    }
}
