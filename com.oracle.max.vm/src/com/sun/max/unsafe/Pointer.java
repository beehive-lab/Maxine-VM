/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.memory.Memory.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.oracle.graal.snippets.Snippet.Fold;
import com.oracle.max.cri.intrinsics.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * Pointers are addresses with extra methods to access memory.
 */
public final class Pointer extends Address implements Accessor {

    private static final int FLOAT_SIZE = 4;
    private static final int DOUBLE_SIZE = 8;

    @HOSTED_ONLY
    public Pointer(long value) {
        super(value);
    }

    public interface Procedure {
        void run(Pointer pointer);
    }

    public interface Predicate {
        boolean evaluate(Pointer pointer);
    }

    @INLINE
    public static Pointer zero() {
        return isHosted() ? ZERO : fromInt(0);
    }

    @INLINE
    public static Pointer fromUnsignedInt(int value) {
        if (isHosted()) {
            final long longValue = value;
            final long n = longValue & 0xffffffffL;
            return fromLong(n);
        }
        return Address.fromUnsignedInt(value).asPointer();
    }

    @INLINE
    public static Pointer fromInt(int value) {
        if (isHosted()) {
            return fromLong(value & INT_MASK);
        }
        return Address.fromInt(value).asPointer();
    }

    @INLINE
    public static Pointer fromLong(long value) {
        if (isHosted()) {
            if (value == 0) {
                return ZERO;
            }
            if (value == -1L) {
                return MAX;
            }
            return new Pointer(value);
        }
        return Address.fromLong(value).asPointer();
    }

    @Override
    @HOSTED_ONLY
    public String toString() {
        return "^" + toHexString();
    }

    @Override
    @INLINE
    public Pointer plus(int addend) {
        return super.plus(addend).asPointer();
    }

    @Override
    @INLINE
    public Pointer plus(long addend) {
        return super.plus(addend).asPointer();
    }

    @Override
    @INLINE
    public Pointer plus(Address addend) {
        return super.plus(addend).asPointer();
    }

    @Override
    @INLINE
    public Pointer plus(Offset addend) {
        return super.plus(addend).asPointer();
    }

    @Override
    @INLINE
    public Pointer plusWords(int nWords) {
        return  super.plusWords(nWords).asPointer();
    }

    @Override
    @INLINE
    public Pointer minus(Address subtrahend) {
        return super.minus(subtrahend).asPointer();
    }

    @Override
    @INLINE
    public Pointer minus(int subtrahend) {
        return super.minus(subtrahend).asPointer();
    }

    @Override
    @INLINE
    public Pointer minus(long subtrahend) {
        return super.minus(subtrahend).asPointer();
    }

    @Override
    @INLINE
    public Pointer minusWords(int nWords) {
        return super.minusWords(nWords).asPointer();
    }

    @Override
    @INLINE
    public Pointer minus(Offset subtrahend) {
        return super.minus(subtrahend).asPointer();
    }

    @Override
    @INLINE
    public Pointer times(Address factor) {
        return super.times(factor).asPointer();
    }

    @Override
    @INLINE
    public Pointer times(int factor) {
        return super.times(factor).asPointer();
    }

    @Override
    @INLINE
    public Pointer dividedBy(Address divisor) {
        return super.dividedBy(divisor).asPointer();
    }

    @Override
    @INLINE
    public Pointer dividedBy(int divisor) {
        return super.dividedBy(divisor).asPointer();
    }

    @Override
    @INLINE
    public Pointer remainder(Address divisor) {
        return super.remainder(divisor).asPointer();
    }

    @Override
    @INLINE
    public Pointer roundedUpBy(Address nBytes) {
        return super.roundedUpBy(nBytes).asPointer();
    }

    @Override
    @INLINE
    public Pointer roundedUpBy(int nBytes) {
        return super.roundedUpBy(nBytes).asPointer();
    }

    @Override
    @INLINE
    public Pointer roundedDownBy(int nBytes) {
        return super.roundedDownBy(nBytes).asPointer();
    }

    @Override
    @INLINE
    public Pointer wordAligned() {
        return super.wordAligned().asPointer();
    }

    @Override
    @INLINE
    public boolean isWordAligned() {
        return super.isWordAligned();
    }

    @Override
    @INLINE
    public Pointer bitSet(int index) {
        return super.bitSet(index).asPointer();
    }

    @Override
    @INLINE
    public Pointer bitClear(int index) {
        return super.bitClear(index).asPointer();
    }

    @Override
    @INLINE
    public Pointer and(Address operand) {
        return super.and(operand).asPointer();
    }

    @Override
    @INLINE
    public Pointer and(int operand) {
        return super.and(operand).asPointer();
    }

    @Override
    @INLINE
    public Pointer and(long operand) {
        return super.and(operand).asPointer();
    }

    @Override
    @INLINE
    public Pointer or(Address operand) {
        return super.or(operand).asPointer();
    }

    @Override
    @INLINE
    public Pointer or(int operand) {
        return super.or(operand).asPointer();
    }

    @Override
    @INLINE
    public Pointer or(long operand) {
        return super.or(operand).asPointer();
    }

    @Override
    @INLINE
    public Pointer not() {
        return super.not().asPointer();
    }

    @Override
    @INLINE
    public Pointer shiftedLeft(int nBits) {
        return super.shiftedLeft(nBits).asPointer();
    }

    @Override
    @INLINE
    public Pointer unsignedShiftedRight(int nBits) {
        return super.unsignedShiftedRight(nBits).asPointer();
    }

    @Fold
    private static boolean risc() {
        return Platform.platform().isa.category == ISA.Category.RISC;
    }

    @INTRINSIC(PREAD_OFF)
    public byte readByte(int offset) {
        return readByte(Offset.fromInt(offset));
    }

    @INTRINSIC(PREAD_OFF)
    public byte readByte(Offset offset) {
        return memory.get(address(offset));
    }

    @INTRINSIC(PREAD_IDX)
    public byte getByte(int displacement, int index) {
        return readByte(Offset.fromInt(index).plus(displacement));
    }

    @INLINE
    public byte getByte(int index) {
        return getByte(0, index);
    }

    @INLINE
    public byte getByte() {
        return getByte(0);
    }

    @INLINE
    public boolean readBoolean(Offset offset) {
        return UnsafeCast.asBoolean(readByte(offset));
    }

    @INLINE
    public boolean readBoolean(int offset) {
        return UnsafeCast.asBoolean(readByte(offset));
    }

    @INLINE
    public boolean getBoolean(int displacement, int index) {
        return UnsafeCast.asBoolean(getByte(displacement, index));
    }

    @INLINE
    public boolean getBoolean(int index) {
        return getBoolean(0, index);
    }

    @INLINE
    public boolean getBoolean() {
        return getBoolean(0);
    }

    @INTRINSIC(PREAD_OFF)
    public short readShort(int offset) {
        return readShort(Offset.fromInt(offset));
    }

    @INTRINSIC(PREAD_OFF)
    public short readShort(Offset offset) {
        return memory.getShort(address(offset));
    }

    @INTRINSIC(PREAD_IDX)
    public short getShort(int displacement, int index) {
        return readShort(Offset.fromInt(index).times(Shorts.SIZE).plus(displacement));
    }

    @INLINE
    public short getShort(int index) {
        return getShort(0, index);
    }

    @INLINE
    public short getShort() {
        return getShort(0);
    }

    @INTRINSIC(PREAD_OFF)
    public char readChar(int offset) {
        return readChar(Offset.fromInt(offset));
    }

    @INTRINSIC(PREAD_OFF)
    public char readChar(Offset offset) {
        return memory.getChar(address(offset));
    }

    @INTRINSIC(PREAD_IDX)
    public char getChar(int displacement, int index) {
        return readChar(Offset.fromInt(index).times(Chars.SIZE).plus(displacement));
    }

    @INLINE
    public char getChar(int index) {
        return getChar(0, index);
    }

    @INLINE
    public char getChar() {
        return getChar(0);
    }

    @INTRINSIC(PREAD_OFF)
    public int readInt(int offset) {
        return readInt(Offset.fromInt(offset));
    }

    @INTRINSIC(PREAD_OFF)
    public int readInt(Offset offset) {
        return memory.getInt(address(offset));
    }

    @INTRINSIC(PREAD_IDX)
    public int getInt(int displacement, int index) {
        return readInt(Offset.fromInt(index).times(Ints.SIZE).plus(displacement));
    }

    @INLINE
    public int getInt(int index) {
        return getInt(0, index);
    }

    @INLINE
    public int getInt() {
        return getInt(0);
    }

    @INTRINSIC(PREAD_OFF)
    public float readFloat(int offset) {
        return readFloat(Offset.fromInt(offset));
    }

    @INTRINSIC(PREAD_OFF)
    public float readFloat(Offset offset) {
        return memory.getFloat(address(offset));
    }

    @INTRINSIC(PREAD_IDX)
    public float getFloat(int displacement, int index) {
        return readFloat(Offset.fromInt(index).times(FLOAT_SIZE).plus(displacement));
    }

    @INLINE
    public float getFloat(int index) {
        return getFloat(0, index);
    }

    @INLINE
    public float getFloat() {
        return getFloat(0);
    }

    @INTRINSIC(PREAD_OFF)
    public long readLong(int offset) {
        return readLong(Offset.fromInt(offset));
    }

    @INTRINSIC(PREAD_OFF)
    public long readLong(Offset offset) {
        return memory.getLong(address(offset));
    }

    @INTRINSIC(PREAD_IDX)
    public long getLong(int displacement, int index) {
        return readLong(Offset.fromInt(index).times(Longs.SIZE).plus(displacement));
    }

    @INLINE
    public long getLong(int index) {
        return getLong(0, index);
    }

    @INLINE
    public long getLong() {
        return getLong(0);
    }

    @INTRINSIC(PREAD_OFF)
    public double readDouble(int offset) {
        return readDouble(Offset.fromInt(offset));
    }

    @INTRINSIC(PREAD_OFF)
    public double readDouble(Offset offset) {
        return memory.getDouble(address(offset));
    }

    @INTRINSIC(PREAD_IDX)
    public double getDouble(int displacement, int index) {
        return readDouble(Offset.fromInt(index).times(DOUBLE_SIZE).plus(displacement));
    }

    @INLINE
    public double getDouble(int index) {
        return getDouble(0, index);
    }

    @INLINE
    public double getDouble() {
        return getDouble(0);
    }

    @INTRINSIC(PREAD_OFF)
    public Word readWord(int offset) {
        return readWord(Offset.fromInt(offset));
    }

    @INTRINSIC(PREAD_OFF)
    public Word readWord(Offset offset) {
        if (Word.width() == 64) {
            return Address.fromLong(readLong(offset));
        }
        return Address.fromInt(readInt(offset));
    }

    @INTRINSIC(PREAD_IDX)
    public Word getWord(int displacement, int index) {
        return readWord(Offset.fromInt(index).times(Word.size()).plus(displacement));
    }

    @INLINE
    public Word getWord(int index) {
        return getWord(0, index);
    }

    @INLINE
    public Word getWord() {
        return getWord(0);
    }

    @INTRINSIC(PREAD_OFF)
    public Reference readReference(int offset) {
        return readReference(Offset.fromInt(offset));
    }

    @INTRINSIC(PREAD_OFF)
    public Reference readReference(Offset offset) {
        throw ProgramError.unexpected();
    }

    @INTRINSIC(PREAD_IDX)
    public Reference getReference(int displacement, int index) {
        return readReference(Offset.fromInt(index).times(Word.size()).plus(displacement));
    }

    @INLINE
    public Reference getReference(int index) {
        return getReference(0, index);
    }

    @INLINE
    public Reference getReference() {
        return getReference(0);
    }

    @INTRINSIC(PWRITE_OFF)
    public void writeByte(int offset, byte value) {
        writeByte(Offset.fromInt(offset), value);
    }

    @INTRINSIC(PWRITE_OFF)
    public void writeByte(Offset offset, byte value) {
        memory.put(address(offset), value);
    }

    @INTRINSIC(PWRITE_IDX)
    public void setByte(int displacement, int index, byte value) {
        writeByte(Offset.fromInt(index).plus(displacement), value);
    }

    @INLINE
    public void setByte(int index, byte value) {
        setByte(0, index, value);
    }

    @INLINE
    public void setByte(byte value) {
        setByte(0, value);
    }

    @INLINE
    public void writeBoolean(Offset offset, boolean value) {
        writeByte(offset, UnsafeCast.asByte(value));
    }

    @INLINE
    public void writeBoolean(int offset, boolean value) {
        writeByte(offset, UnsafeCast.asByte(value));
    }

    @INLINE
    public void setBoolean(int displacement, int index, boolean value) {
        setByte(displacement, index, UnsafeCast.asByte(value));
    }

    @INLINE
    public void setBoolean(int index, boolean value) {
        setBoolean(0, index, value);
    }

    @INLINE
    public void setBoolean(boolean value) {
        setBoolean(0, value);
    }

    @INTRINSIC(PWRITE_OFF)
    public void writeShort(int offset, short value) {
        writeShort(Offset.fromInt(offset), value);
    }

    @INTRINSIC(PWRITE_OFF)
    public void writeShort(Offset offset, short value) {
        memory.putShort(address(offset), value);
    }

    @INTRINSIC(PWRITE_IDX)
    public void setShort(int displacement, int index, short value) {
        writeShort(Offset.fromInt(index).times(Shorts.SIZE).plus(displacement), value);
    }

    @INLINE
    public void setShort(int index, short value) {
        setShort(0, index, value);
    }

    @INLINE
    public void setShort(short value) {
        setShort(0, value);
    }

    @INLINE
    public void writeChar(Offset offset, char value) {
        writeShort(offset, UnsafeCast.asShort(value));
    }

    @INLINE
    public void writeChar(int offset, char value) {
        writeShort(offset, UnsafeCast.asShort(value));
    }

    @INLINE
    public void setChar(int displacement, int index, char value) {
        setShort(displacement, index, UnsafeCast.asShort(value));
    }

    @INLINE
    public void setChar(int index, char value) {
        setChar(0, index, value);
    }

    @INLINE
    public void setChar(char value) {
        setChar(0, value);
    }

    @INTRINSIC(PWRITE_OFF)
    public void writeInt(int offset, int value) {
        writeInt(Offset.fromInt(offset), value);
    }

    @INTRINSIC(PWRITE_OFF)
    public void writeInt(Offset offset, int value) {
        memory.putInt(address(offset), value);
    }

    @INTRINSIC(PWRITE_IDX)
    public void setInt(int displacement, int index, int value) {
        writeInt(Offset.fromInt(index).times(Ints.SIZE).plus(displacement), value);
    }

    @INLINE
    public void setInt(int index, int value) {
        setInt(0, index, value);
    }

    @INLINE
    public void setInt(int value) {
        setInt(0, value);
    }

    @INTRINSIC(PWRITE_OFF)
    public void writeFloat(int offset, float value) {
        writeFloat(Offset.fromInt(offset), value);
    }

    @INTRINSIC(PWRITE_OFF)
    public void writeFloat(Offset offset, float value) {
        memory.putFloat(address(offset), value);
    }

    @INTRINSIC(PWRITE_IDX)
    public void setFloat(int displacement, int index, float value) {
        writeFloat(Offset.fromInt(index).times(FLOAT_SIZE).plus(displacement), value);
    }

    @INLINE
    public void setFloat(int index, float value) {
        setFloat(0, index, value);
    }

    @INLINE
    public void setFloat(float value) {
        setFloat(0, value);
    }

    @INTRINSIC(PWRITE_OFF)
    public void writeLong(int offset, long value) {
        writeLong(Offset.fromInt(offset), value);
    }

    @INTRINSIC(PWRITE_OFF)
    public void writeLong(Offset offset, long value) {
        memory.putLong(address(offset), value);
    }

    @INTRINSIC(PWRITE_IDX)
    public void setLong(int displacement, int index, long value) {
        writeLong(Offset.fromInt(index).times(Longs.SIZE).plus(displacement), value);
    }

    @INLINE
    public void setLong(int index, long value) {
        setLong(0, index, value);
    }

    @INLINE
    public void setLong(long value) {
        setLong(0, value);
    }

    @INTRINSIC(PWRITE_OFF)
    public void writeDouble(int offset, double value) {
        writeDouble(Offset.fromInt(offset), value);
    }

    @INTRINSIC(PWRITE_OFF)
    public void writeDouble(Offset offset, double value) {
        memory.putDouble(address(offset), value);
    }

    @INTRINSIC(PWRITE_IDX)
    public void setDouble(int displacement, int index, double value) {
        writeDouble(Offset.fromInt(index).times(DOUBLE_SIZE).plus(displacement), value);
    }

    @INLINE
    public void setDouble(int index, double value) {
        setDouble(0, index, value);
    }

    @INLINE
    public void setDouble(double value) {
        setDouble(0, value);
    }

    @INTRINSIC(PWRITE_OFF)
    public void writeWord(int offset, Word value) {
        writeWord(Offset.fromInt(offset), value);
    }

    @INTRINSIC(PWRITE_OFF)
    public void writeWord(Offset offset, Word value) {
        if (Word.width() == 64) {
            writeLong(offset, value.asOffset().toLong());
        } else {
            writeInt(offset, value.asOffset().toInt());
        }
    }

    @INTRINSIC(PWRITE_IDX)
    public void setWord(int displacement, int index, Word value) {
        writeWord(Offset.fromInt(index).times(Word.size()).plus(displacement), value);
    }

    @INLINE
    public void setWord(int index, Word value) {
        setWord(0, index, value);
    }

    @INLINE
    public void setWord(Word value) {
        setWord(0, value);
    }

    @INLINE
    public void writeObject(int offset, Object value) {
        writeReference(Offset.fromInt(offset), Reference.fromJava(value));
    }

    @INTRINSIC(PWRITE_OFF)
    public void writeReference(int offset, Reference value) {
        writeReference(Offset.fromInt(offset), value);
    }

    @INTRINSIC(PWRITE_OFF)
    public void writeReference(Offset offset, Reference value) {
        throw ProgramError.unexpected();
    }

    @INTRINSIC(PWRITE_IDX)
    public void setReference(int displacement, int index, Reference value) {
        writeReference(Offset.fromInt(index).times(Word.size()).plus(displacement), value);
    }

    @INLINE
    public void setReference(int index, Reference value) {
        setReference(0, index, value);
    }

    @INLINE
    public void setReference(Reference value) {
        setReference(0, value);
    }

    /**
     * @see Accessor#compareAndSwapInt(Offset, int, int)
     */
    @INTRINSIC(PCMPSWP)
    public native int compareAndSwapInt(int offset, int expectedValue, int newValue);

    /**
     * @see Accessor#compareAndSwapInt(Offset, int, int)
     */
    @INTRINSIC(PCMPSWP)
    public native int compareAndSwapInt(Offset offset, int expectedValue, int newValue);

    /**
     * @see Accessor#compareAndSwapInt(Offset, int, int)
     */
    @INTRINSIC(PCMPSWP)
    public native long compareAndSwapLong(int offset, long expectedValue, long newValue);

    /**
     * @see Accessor#compareAndSwapInt(Offset, int, int)
     */
    @INTRINSIC(PCMPSWP)
    public native long compareAndSwapLong(Offset offset, long expectedValue, long newValue);

    /**
     * @see Accessor#compareAndSwapInt(Offset, int, int)
     */
    @INLINE
    public Word compareAndSwapWord(int offset, Word expectedValue, Word newValue) {
        if (Word.width() == 64) {
            return fromLong(compareAndSwapLong(offset, expectedValue.asAddress().toLong(), newValue.asAddress().toLong()));
        }
        return fromInt(compareAndSwapInt(offset, expectedValue.asAddress().toInt(), newValue.asAddress().toInt()));
    }

    /**
     * @see Accessor#compareAndSwapInt(Offset, int, int)
     */
    @INLINE
    public Word compareAndSwapWord(Offset offset, Word expectedValue, Word newValue) {
        if (Word.width() == 64) {
            return fromLong(compareAndSwapLong(offset, expectedValue.asAddress().toLong(), newValue.asAddress().toLong()));
        }
        return fromInt(compareAndSwapInt(offset, expectedValue.asAddress().toInt(), newValue.asAddress().toInt()));
    }

    /**
     * @see Accessor#compareAndSwapInt(Offset, int, int)
     */
    @INTRINSIC(PCMPSWP)
    public native Reference compareAndSwapReference(int offset, Reference expectedValue, Reference newValue);

    /**
     * @see Accessor#compareAndSwapInt(Offset, int, int)
     */
    @INTRINSIC(PCMPSWP)
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
    @INLINE
    public void setBit(int bitIndex) {
        final int byteIndex = UnsignedMath.divide(bitIndex, Bytes.WIDTH);
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
    @INLINE
    public void setBits(int bitIndex, byte bits) {
        // If we do not mask off the leading bits after a conversion to int right here,
        // then the arithmetic operations below will convert implicitly to int and may insert sign bits.
        final int intBits = bits & 0xff;

        int byteIndex = UnsignedMath.divide(bitIndex, Bytes.WIDTH);
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

    @HOSTED_ONLY
    public void copyElements(int displacement, int srcIndex, Object dst, int dstIndex, int length) {
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

    @HOSTED_ONLY
    private static final Pointer ZERO = new Pointer(0);
    @HOSTED_ONLY
    private static final Pointer MAX = new Pointer(-1L);

    @HOSTED_ONLY
    private static final int HIGHEST_CACHED_VALUE = 1000000;

    @HOSTED_ONLY
    private static final Pointer[] cache = new Pointer[HIGHEST_CACHED_VALUE + 1];

    @HOSTED_ONLY
    public static Pointer from(long value) {
        if (value == 0) {
            return ZERO;
        }
        if (value >= 0 && value <= HIGHEST_CACHED_VALUE) {
            int cacheIndex = (int) value;
            Pointer ptr = cache[cacheIndex];
            if (ptr == null) {
                ptr = new Pointer(value);
                cache[cacheIndex] = ptr;
            }
            return ptr;
        }
        if (value == -1L) {
            return MAX;
        }
        return new Pointer(value);
    }

    @HOSTED_ONLY
    private int address(Offset offset) {
        assert (int) value == value;
        return (int) value + offset.toInt();
    }
}
