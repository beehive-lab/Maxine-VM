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
package com.sun.max.unsafe;

import static com.sun.cri.bytecode.Bytecodes.*;
import static com.sun.max.vm.MaxineVM.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.lang.Bytes;
import com.sun.max.platform.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * Pointers are addresses with extra methods to access memory.
 */
public abstract class Pointer extends Address implements Accessor {

    private static final int FLOAT_SIZE = 4;
    private static final int DOUBLE_SIZE = 8;

    protected Pointer() {
    }

    public interface Procedure {
        void run(Pointer pointer);
    }

    public interface Predicate {
        boolean evaluate(Pointer pointer);
    }

    @INLINE
    @INTRINSIC(WCONST_0)
    public static Pointer zero() {
        return isHosted() ? BoxedPointer.ZERO : fromInt(0);
    }

    @INLINE
    public static Pointer fromUnsignedInt(int value) {
        return Address.fromUnsignedInt(value).asPointer();
    }

    @INLINE
    public static Pointer fromInt(int value) {
        return Address.fromInt(value).asPointer();
    }

    @INLINE
    public static Pointer fromLong(long value) {
        return Address.fromLong(value).asPointer();
    }

    @Override
    public final String toString() {
        return "^" + toHexString();
    }

    @Override
    @INLINE
    public final Pointer plus(int addend) {
        return asAddress().plus(addend).asPointer();
    }

    @Override
    @INLINE
    public final Pointer plus(long addend) {
        return asAddress().plus(addend).asPointer();
    }

    @Override
    @INLINE
    public final Pointer plus(Address addend) {
        return asAddress().plus(addend).asPointer();
    }

    @Override
    @INLINE
    public final Pointer plus(Offset addend) {
        return asAddress().plus(addend).asPointer();
    }

    @INLINE
    public final Pointer plusWords(int nWords) {
        return plus(nWords * Word.size());
    }

    @Override
    @INLINE
    public final Pointer minus(Address subtrahend) {
        return asAddress().minus(subtrahend).asPointer();
    }

    @Override
    @INLINE
    public final Pointer minus(int subtrahend) {
        return asAddress().minus(subtrahend).asPointer();
    }

    @Override
    @INLINE
    public final Pointer minus(long subtrahend) {
        return asAddress().minus(subtrahend).asPointer();
    }

    @INLINE
    public final Pointer minusWords(int nWords) {
        return minus(nWords * Word.size());
    }

    @Override
    @INLINE
    public final Pointer minus(Offset subtrahend) {
        return asAddress().minus(subtrahend).asPointer();
    }

    @Override
    @INLINE
    public final Pointer times(Address factor) {
        return asAddress().times(factor).asPointer();
    }

    @Override
    @INLINE
    public final Pointer times(int factor) {
        return asAddress().times(factor).asPointer();
    }

    @Override
    @INLINE
    @INTRINSIC(WDIV)
    public final Pointer dividedBy(Address divisor) {
        return asAddress().dividedBy(divisor).asPointer();
    }

    @Override
    @INLINE
    @INTRINSIC(WDIVI)
    public final Pointer dividedBy(int divisor) {
        return asAddress().dividedBy(divisor).asPointer();
    }

    @Override
    @INLINE
    @INTRINSIC(WREM)
    public final Pointer remainder(Address divisor) {
        return asAddress().remainder(divisor).asPointer();
    }

    @Override
    @INLINE
    public final Pointer roundedUpBy(Address nBytes) {
        return asAddress().roundedUpBy(nBytes).asPointer();
    }

    @Override
    @INLINE
    public final Pointer roundedUpBy(int nBytes) {
        return asAddress().roundedUpBy(nBytes).asPointer();
    }

    @Override
    @INLINE
    public final Pointer roundedDownBy(int nBytes) {
        return asAddress().roundedDownBy(nBytes).asPointer();
    }

    @Override
    @INLINE
    public final Pointer wordAligned() {
        return asAddress().wordAligned().asPointer();
    }

    @Override
    @INLINE(override = true)
    public final boolean isWordAligned() {
        return asAddress().isWordAligned();
    }

    @Override
    @INLINE
    public final Pointer bitSet(int index) {
        return asAddress().bitSet(index).asPointer();
    }

    @Override
    @INLINE
    public final Pointer bitClear(int index) {
        return asAddress().bitClear(index).asPointer();
    }

    @Override
    @INLINE
    public final Pointer and(Address operand) {
        return asAddress().and(operand).asPointer();
    }

    @Override
    @INLINE
    public final Pointer and(int operand) {
        return asAddress().and(operand).asPointer();
    }

    @Override
    @INLINE
    public final Pointer and(long operand) {
        return asAddress().and(operand).asPointer();
    }

    @Override
    @INLINE
    public final Pointer or(Address operand) {
        return asAddress().or(operand).asPointer();
    }

    @Override
    @INLINE
    public final Pointer or(int operand) {
        return asAddress().or(operand).asPointer();
    }

    @Override
    @INLINE
    public final Pointer or(long operand) {
        return asAddress().or(operand).asPointer();
    }

    @Override
    @INLINE
    public final Pointer not() {
        return asAddress().not().asPointer();
    }

    @Override
    @INLINE
    public final Pointer shiftedLeft(int nBits) {
        return asAddress().shiftedLeft(nBits).asPointer();
    }

    @Override
    @INLINE
    public final Pointer unsignedShiftedRight(int nBits) {
        return asAddress().unsignedShiftedRight(nBits).asPointer();
    }

    @FOLD
    private static boolean risc() {
        return Platform.platform().isa.category == ISA.Category.RISC;
    }

    @INTRINSIC(PREAD_BYTE_I)
    public byte readByte(int offset) {
        return readByte(Offset.fromInt(offset));
    }

    @INTRINSIC(PREAD_BYTE)
    public abstract byte readByte(Offset offset);

    @INTRINSIC(PGET_BYTE)
    private native byte builtinGetByte(int displacement, int index);

    @INLINE
    public final byte getByte(int displacement, int index) {
        if (risc() || isHosted()) {
            return readByte(Offset.fromInt(index).plus(displacement));
        }
        return builtinGetByte(displacement, index);
    }

    @INLINE
    public final byte getByte(int index) {
        return getByte(0, index);
    }

    @INLINE
    public final byte getByte() {
        return getByte(0);
    }

    @INLINE
    public final boolean readBoolean(Offset offset) {
        return UnsafeCast.asBoolean(readByte(offset));
    }

    @INLINE
    public final boolean readBoolean(int offset) {
        return UnsafeCast.asBoolean(readByte(offset));
    }

    @INLINE
    public final boolean getBoolean(int displacement, int index) {
        return UnsafeCast.asBoolean(getByte(displacement, index));
    }

    @INLINE
    public final boolean getBoolean(int index) {
        return getBoolean(0, index);
    }

    @INLINE
    public final boolean getBoolean() {
        return getBoolean(0);
    }

    @INTRINSIC(PREAD_SHORT_I)
    public final short readShort(int offset) {
        return readShort(Offset.fromInt(offset));
    }

    @INTRINSIC(PREAD_SHORT)
    public abstract short readShort(Offset offset);

    @INTRINSIC(PGET_SHORT)
    private native short builtinGetShort(int displacement, int index);

    @INLINE
    public final short getShort(int displacement, int index) {
        if (risc() || isHosted()) {
            return readShort(Offset.fromInt(index).times(Shorts.SIZE).plus(displacement));
        }
        return builtinGetShort(displacement, index);
    }

    @INLINE
    public final short getShort(int index) {
        return getShort(0, index);
    }

    @INLINE
    public final short getShort() {
        return getShort(0);
    }

    @INTRINSIC(PREAD_CHAR_I)
    public final char readChar(int offset) {
        return readChar(Offset.fromInt(offset));
    }

    @INTRINSIC(PREAD_CHAR)
    public abstract char readChar(Offset offset);

    @INTRINSIC(PGET_CHAR)
    private native char builtinGetChar(int displacement, int index);

    @INLINE
    public final char getChar(int displacement, int index) {
        if (risc() || isHosted()) {
            return readChar(Offset.fromInt(index).times(Chars.SIZE).plus(displacement));
        }
        return builtinGetChar(displacement, index);
    }

    @INLINE
    public final char getChar(int index) {
        return getChar(0, index);
    }

    @INLINE
    public final char getChar() {
        return getChar(0);
    }

    @INTRINSIC(PREAD_INT_I)
    public final int readInt(int offset) {
        return readInt(Offset.fromInt(offset));
    }

    @INTRINSIC(PREAD_INT)
    public abstract int readInt(Offset offset);

    @INTRINSIC(PGET_INT)
    private native int builtinGetInt(int displacement, int index);

    @INLINE
    public final int getInt(int displacement, int index) {
        if (risc() || isHosted()) {
            return readInt(Offset.fromInt(index).times(Ints.SIZE).plus(displacement));
        }
        return builtinGetInt(displacement, index);
    }

    @INLINE
    public final int getInt(int index) {
        return getInt(0, index);
    }

    @INLINE
    public final int getInt() {
        return getInt(0);
    }

    @INTRINSIC(PREAD_FLOAT_I)
    public final float readFloat(int offset) {
        return readFloat(Offset.fromInt(offset));
    }

    @INTRINSIC(PREAD_FLOAT)
    public abstract float readFloat(Offset offset);

    @INTRINSIC(PGET_FLOAT)
    private native float builtinGetFloat(int displacement, int index);

    @INLINE
    public final float getFloat(int displacement, int index) {
        if (risc() || isHosted()) {
            return readFloat(Offset.fromInt(index).times(FLOAT_SIZE).plus(displacement));
        }
        return builtinGetFloat(displacement, index);
    }

    @INLINE
    public final float getFloat(int index) {
        return getFloat(0, index);
    }

    @INLINE
    public final float getFloat() {
        return getFloat(0);
    }

    @INTRINSIC(PREAD_LONG_I)
    public final long readLong(int offset) {
        return readLong(Offset.fromInt(offset));
    }

    @INTRINSIC(PREAD_LONG)
    public abstract long readLong(Offset offset);

    @INTRINSIC(PGET_LONG)
    private native long builtinGetLong(int displacement, int index);

    @INLINE
    public final long getLong(int displacement, int index) {
        if (risc() || isHosted()) {
            return readLong(Offset.fromInt(index).times(Longs.SIZE).plus(displacement));
        }
        return builtinGetLong(displacement, index);
    }

    @INLINE
    public final long getLong(int index) {
        return getLong(0, index);
    }

    @INLINE
    public final long getLong() {
        return getLong(0);
    }

    @INTRINSIC(PREAD_DOUBLE_I)
    public final double readDouble(int offset) {
        return readDouble(Offset.fromInt(offset));
    }

    @INTRINSIC(PREAD_DOUBLE)
    public abstract double readDouble(Offset offset);

    @INTRINSIC(PGET_DOUBLE)
    private native double builtinGetDouble(int displacement, int index);

    @INLINE
    public final double getDouble(int displacement, int index) {
        if (risc() || isHosted()) {
            return readDouble(Offset.fromInt(index).times(DOUBLE_SIZE).plus(displacement));
        }
        return builtinGetDouble(displacement, index);
    }

    @INLINE
    public final double getDouble(int index) {
        return getDouble(0, index);
    }

    @INLINE
    public final double getDouble() {
        return getDouble(0);
    }

    @INTRINSIC(PREAD_WORD_I)
    public final Word readWord(int offset) {
        return readWord(Offset.fromInt(offset));
    }

    @INTRINSIC(PREAD_WORD)
    public abstract Word readWord(Offset offset);

    @INTRINSIC(PGET_WORD)
    private native Word builtinGetWord(int displacement, int index);

    @INLINE
    public final Word getWord(int displacement, int index) {
        if (risc() || isHosted()) {
            return readWord(Offset.fromInt(index).times(Word.size()).plus(displacement));
        }
        return builtinGetWord(displacement, index);
    }

    @INLINE
    public final Word getWord(int index) {
        return getWord(0, index);
    }

    @INLINE
    public final Word getWord() {
        return getWord(0);
    }

    @INTRINSIC(PREAD_REFERENCE_I)
    public final Reference readReference(int offset) {
        return readReference(Offset.fromInt(offset));
    }

    @INTRINSIC(PREAD_REFERENCE)
    public abstract Reference readReference(Offset offset);

    @INTRINSIC(PGET_REFERENCE)
    private native Reference builtinGetReference(int displacement, int index);

    @INLINE
    public final Reference getReference(int displacement, int index) {
        if (risc() || isHosted()) {
            return readReference(Offset.fromInt(index).times(Word.size()).plus(displacement));
        }
        return builtinGetReference(displacement, index);
    }

    @INLINE
    public final Reference getReference(int index) {
        return getReference(0, index);
    }

    @INLINE
    public final Reference getReference() {
        return getReference(0);
    }

    @INTRINSIC(PWRITE_BYTE_I)
    public final void writeByte(int offset, byte value) {
        writeByte(Offset.fromInt(offset), value);
    }

    @INTRINSIC(PWRITE_BYTE)
    public abstract void writeByte(Offset offset, byte value);

    @INTRINSIC(PSET_BYTE)
    private native void builtinSetByte(int displacement, int index, byte value);

    @INLINE
    public final void setByte(int displacement, int index, byte value) {
        if (risc() || isHosted()) {
            writeByte(Offset.fromInt(index).plus(displacement), value);
        } else {
            builtinSetByte(displacement, index, value);
        }
    }

    @INLINE
    public final void setByte(int index, byte value) {
        setByte(0, index, value);
    }

    @INLINE
    public final void setByte(byte value) {
        setByte(0, value);
    }

    @INLINE
    public final void writeBoolean(Offset offset, boolean value) {
        writeByte(offset, UnsafeCast.asByte(value));
    }

    @INLINE
    public final void writeBoolean(int offset, boolean value) {
        writeByte(offset, UnsafeCast.asByte(value));
    }

    @INLINE
    public final void setBoolean(int displacement, int index, boolean value) {
        setByte(displacement, index, UnsafeCast.asByte(value));
    }

    @INLINE
    public final void setBoolean(int index, boolean value) {
        setBoolean(0, index, value);
    }

    @INLINE
    public final void setBoolean(boolean value) {
        setBoolean(0, value);
    }

    @INTRINSIC(PWRITE_SHORT_I)
    public final void writeShort(int offset, short value) {
        writeShort(Offset.fromInt(offset), value);
    }

    @INTRINSIC(PWRITE_SHORT)
    public abstract void writeShort(Offset offset, short value);

    @INTRINSIC(PSET_SHORT)
    private native void builtinSetShort(int displacement, int index, short value);

    @INLINE
    public final void setShort(int displacement, int index, short value) {
        if (risc() || isHosted()) {
            writeShort(Offset.fromInt(index).times(Shorts.SIZE).plus(displacement), value);
        } else {
            builtinSetShort(displacement, index, value);
        }
    }

    @INLINE
    public final void setShort(int index, short value) {
        setShort(0, index, value);
    }

    @INLINE
    public final void setShort(short value) {
        setShort(0, value);
    }

    @INLINE
    public final void writeChar(Offset offset, char value) {
        writeShort(offset, UnsafeCast.asShort(value));
    }

    @INLINE
    public final void writeChar(int offset, char value) {
        writeShort(offset, UnsafeCast.asShort(value));
    }

    @INLINE
    public final void setChar(int displacement, int index, char value) {
        setShort(displacement, index, UnsafeCast.asShort(value));
    }

    @INLINE
    public final void setChar(int index, char value) {
        setChar(0, index, value);
    }

    @INLINE
    public final void setChar(char value) {
        setChar(0, value);
    }

    @INTRINSIC(PWRITE_INT_I)
    public final void writeInt(int offset, int value) {
        writeInt(Offset.fromInt(offset), value);
    }

    @INTRINSIC(PWRITE_INT)
    public abstract void writeInt(Offset offset, int value);

    @INTRINSIC(PSET_INT)
    private native void builtinSetInt(int displacement, int index, int value);

    @INLINE
    public final void setInt(int displacement, int index, int value) {
        if (risc() || isHosted()) {
            writeInt(Offset.fromInt(index).times(Ints.SIZE).plus(displacement), value);
        } else {
            builtinSetInt(displacement, index, value);
        }
    }

    @INLINE
    public final void setInt(int index, int value) {
        setInt(0, index, value);
    }

    @INLINE
    public final void setInt(int value) {
        setInt(0, value);
    }

    @INTRINSIC(PWRITE_FLOAT_I)
    public final void writeFloat(int offset, float value) {
        writeFloat(Offset.fromInt(offset), value);
    }

    @INTRINSIC(PWRITE_FLOAT)
    public abstract void writeFloat(Offset offset, float value);

    @INTRINSIC(PSET_FLOAT)
    private native void builtinSetFloat(int displacement, int index, float value);

    @INLINE
    public final void setFloat(int displacement, int index, float value) {
        if (risc() || isHosted()) {
            writeFloat(Offset.fromInt(index).times(FLOAT_SIZE).plus(displacement), value);
        } else {
            builtinSetFloat(displacement, index, value);
        }
    }

    @INLINE
    public final void setFloat(int index, float value) {
        setFloat(0, index, value);
    }

    @INLINE
    public final void setFloat(float value) {
        setFloat(0, value);
    }

    @INTRINSIC(PWRITE_LONG_I)
    public final void writeLong(int offset, long value) {
        writeLong(Offset.fromInt(offset), value);
    }

    @INTRINSIC(PWRITE_LONG)
    public abstract void writeLong(Offset offset, long value);

    @INTRINSIC(PSET_LONG)
    private native void builtinSetLong(int displacement, int index, long value);

    @INLINE
    public final void setLong(int displacement, int index, long value) {
        if (risc() || isHosted()) {
            writeLong(Offset.fromInt(index).times(Longs.SIZE).plus(displacement), value);
        } else {
            builtinSetLong(displacement, index, value);
        }
    }

    @INLINE
    public final void setLong(int index, long value) {
        setLong(0, index, value);
    }

    @INLINE
    public final void setLong(long value) {
        setLong(0, value);
    }

    @INTRINSIC(PWRITE_DOUBLE_I)
    public final void writeDouble(int offset, double value) {
        writeDouble(Offset.fromInt(offset), value);
    }

    @INTRINSIC(PWRITE_DOUBLE)
    public abstract void writeDouble(Offset offset, double value);

    @INTRINSIC(PSET_DOUBLE)
    private native void builtinSetDouble(int displacement, int index, double value);

    @INLINE
    public final void setDouble(int displacement, int index, double value) {
        if (risc() || isHosted()) {
            writeDouble(Offset.fromInt(index).times(DOUBLE_SIZE).plus(displacement), value);
        } else {
            builtinSetDouble(displacement, index, value);
        }
    }

    @INLINE
    public final void setDouble(int index, double value) {
        setDouble(0, index, value);
    }

    @INLINE
    public final void setDouble(double value) {
        setDouble(0, value);
    }

    @INTRINSIC(PWRITE_WORD_I)
    public final void writeWord(int offset, Word value) {
        writeWord(Offset.fromInt(offset), value);
    }

    @INTRINSIC(PWRITE_WORD)
    public abstract void writeWord(Offset offset, Word value);

    @INTRINSIC(PSET_WORD)
    private native void builtinSetWord(int displacement, int index, Word value);

    @INLINE
    public final void setWord(int displacement, int index, Word value) {
        if (risc() || isHosted()) {
            writeWord(Offset.fromInt(index).times(Word.size()).plus(displacement), value);
        } else {
            builtinSetWord(displacement, index, value);
        }
    }

    @INLINE
    public final void setWord(int index, Word value) {
        setWord(0, index, value);
    }

    @INLINE
    public final void setWord(Word value) {
        setWord(0, value);
    }

    @INTRINSIC(PWRITE_REFERENCE_I)
    public final void writeReference(int offset, Reference value) {
        writeReference(Offset.fromInt(offset), value);
    }

    @INTRINSIC(PWRITE_REFERENCE)
    public abstract void writeReference(Offset offset, Reference value);

    @INTRINSIC(PSET_REFERENCE)
    private native void builtinSetReference(int displacement, int index, Reference value);

    @INLINE
    public final void setReference(int displacement, int index, Reference value) {
        if (risc() || isHosted()) {
            writeReference(Offset.fromInt(index).times(Word.size()).plus(displacement), value);
        } else {
            builtinSetReference(displacement, index, value);
        }
    }

    @INLINE
    public final void setReference(int index, Reference value) {
        setReference(0, index, value);
    }

    @INLINE
    public final void setReference(Reference value) {
        setReference(0, value);
    }

    /**
     * @see Accessor#compareAndSwapInt(Offset, int, int)
     */
    @INTRINSIC(PCMPSWP_INT_I)
    public native int compareAndSwapInt(int offset, int expectedValue, int newValue);

    /**
     * @see Accessor#compareAndSwapInt(Offset, int, int)
     */
    @INTRINSIC(PCMPSWP_INT)
    public native int compareAndSwapInt(Offset offset, int expectedValue, int newValue);

    /**
     * @see Accessor#compareAndSwapInt(Offset, int, int)
     */
    @INTRINSIC(PCMPSWP_WORD_I)
    public native Word compareAndSwapWord(int offset, Word expectedValue, Word newValue);

    /**
     * @see Accessor#compareAndSwapInt(Offset, int, int)
     */
    @INTRINSIC(PCMPSWP_WORD)
    public native Word compareAndSwapWord(Offset offset, Word expectedValue, Word newValue);

    /**
     * @see Accessor#compareAndSwapInt(Offset, int, int)
     */
    @INTRINSIC(PCMPSWP_REFERENCE_I)
    public native Reference compareAndSwapReference(int offset, Reference expectedValue, Reference newValue);

    /**
     * @see Accessor#compareAndSwapInt(Offset, int, int)
     */
    @INTRINSIC(PCMPSWP_REFERENCE)
    public native Reference compareAndSwapReference(Offset offset, Reference expectedValue, Reference newValue);

    /**
     * Sets a bit in the bit map whose base is denoted by the value of this pointer.
     *
     * ATTENTION: There is no protection against concurrent access to the affected byte.
     *
     * This method may read the affected byte first, then set the bit in it and then write the byte back.
     *
     * @param bitIndex the index of the bit to set
     */
    public void setBit(int bitIndex) {
        final int byteIndex = Unsigned.idiv(bitIndex, Bytes.WIDTH);
        byte byteValue = getByte(byteIndex);
        byteValue |= 1 << (bitIndex % Bytes.WIDTH);
        setByte(byteIndex, byteValue);
    }

    /**
     * Modifies up to 8 bits in the bit map whose base is denoted by the value of this pointer
     * by OR'ing in a given 8-bit mask.
     *
     * ATTENTION: There is no protection against concurrent access to affected bytes.
     *
     * This method may read each affected byte first, then set some bits in it and then write the byte back.
     * There are either 1 or 2 affected bytes, depending on alignment of the bit index to bytes in memory.
     *
     * @param bitIndex the index of the first bit to set
     * @param bits a mask of 8 bits OR'ed with the 8 bits in this bit map starting at {@code bitIndex}
     */
    public void setBits(int bitIndex, byte bits) {
        // If we do not mask off the leading bits after a conversion to int right here,
        // then the arithmetic operations below will convert implicitly to int and may insert sign bits.
        final int intBits = bits & 0xff;

        int byteIndex = Unsigned.idiv(bitIndex, Bytes.WIDTH);
        final int rest = bitIndex % Bytes.WIDTH;
        byte byteValue = getByte(byteIndex);
        byteValue |= intBits << rest;
        setByte(byteIndex, byteValue);
        if (rest > 0) {
            byteIndex++;
            byteValue = getByte(byteIndex);
            byteValue |= intBits >>> (Bytes.WIDTH - rest);
            setByte(byteIndex, byteValue);
        }
    }

    public final void copyElements(int displacement, int srcIndex, Object dst, int dstIndex, int length) {
        Kind kind = Kind.fromJava(dst.getClass().getComponentType());
        switch (kind.asEnum) {
            case BOOLEAN: {
                boolean[] arr = (boolean[]) dst;
                for (int i = 0; i < length; ++i) {
                    arr[dstIndex + i] = getBoolean(displacement, srcIndex + i);
                }
                break;
            }
            case BYTE: {
                byte[] arr = (byte[]) dst;
                for (int i = 0; i < length; ++i) {
                    arr[dstIndex + i] = getByte(displacement, srcIndex + i);
                }
                break;
            }
            case CHAR: {
                char[] arr = (char[]) dst;
                for (int i = 0; i < length; ++i) {
                    arr[dstIndex + i] = getChar(displacement, srcIndex + i);
                }
                break;
            }
            case SHORT: {
                short[] arr = (short[]) dst;
                for (int i = 0; i < length; ++i) {
                    arr[dstIndex + i] = getShort(displacement, srcIndex + i);
                }
                break;
            }
            case INT: {
                int[] arr = (int[]) dst;
                for (int i = 0; i < length; ++i) {
                    arr[dstIndex + i] = getInt(displacement, srcIndex + i);
                }
                break;
            }
            case FLOAT: {
                float[] arr = (float[]) dst;
                for (int i = 0; i < length; ++i) {
                    arr[dstIndex + i] = getFloat(displacement, srcIndex + i);
                }
                break;
            }
            case LONG: {
                long[] arr = (long[]) dst;
                for (int i = 0; i < length; ++i) {
                    arr[dstIndex + i] = getLong(displacement, srcIndex + i);
                }
                break;
            }
            case DOUBLE: {
                double[] arr = (double[]) dst;
                for (int i = 0; i < length; ++i) {
                    arr[dstIndex + i] = getDouble(displacement, srcIndex + i);
                }
                break;
            }
            case REFERENCE: {
                Reference[] arr = (Reference[]) dst;
                for (int i = 0; i < length; ++i) {
                    arr[dstIndex + i] = getReference(displacement, srcIndex + i);
                }
                break;
            }
            case WORD: {
                Word[] arr = (Word[]) dst;
                for (int i = 0; i < length; ++i) {
                    WordArray.set(arr, dstIndex + i, getWord(displacement, srcIndex + i));
                }
                break;
            }
            default:
                throw FatalError.unexpected("invalid type");
        }
    }
}
