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
package com.sun.max.unsafe;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.reference.*;

/**
 * Pointers are addresses with extra methods to access memory.
 *
 * @author Bernd Mathiske
 */
public abstract class Pointer extends Address implements Accessor {

    protected Pointer() {
    }

    public interface Procedure {
        void run(Pointer pointer);
    }

    public interface Predicate {
        boolean evaluate(Pointer pointer);
    }

    @INLINE
    public static Pointer zero() {
        return fromInt(0);
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
    public final Pointer dividedBy(Address divisor) {
        return asAddress().dividedBy(divisor).asPointer();
    }

    @Override
    @INLINE
    public final Pointer dividedBy(int divisor) {
        return asAddress().dividedBy(divisor).asPointer();
    }

    @Override
    @INLINE
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
    public final Pointer aligned() {
        return asAddress().aligned().asPointer();
    }

    @Override
    @INLINE(override = true)
    public final boolean isAligned() {
        return asAddress().isAligned();
    }

    @Override
    @INLINE
    public final Pointer bitSet(int index) {
        return asAddress().bitSet(index).asPointer();
    }

    @Override
    @INLINE
    public final Pointer bitCleared(int index) {
        return asAddress().bitCleared(index).asPointer();
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

    @UNSAFE
    @FOLD
    private static boolean risc() {
        return Platform.hostOrTarget().processorKind().instructionSet().category() == InstructionSet.Category.RISC;
    }

    @BUILTIN(builtinClass = PointerLoadBuiltin.ReadByteAtLongOffset.class)
    protected native byte readByteAtLongOffset(long offset);

    @BUILTIN(builtinClass = PointerLoadBuiltin.ReadByteAtIntOffset.class)
    protected native byte readByteAtIntOffset(int offset);

    @INLINE(override = true)
    public byte readByte(int offset) {
        return readByteAtIntOffset(offset);
    }

    @INLINE(override = true)
    public final byte readByte(Offset offset) {
        if (Word.width() == WordWidth.BITS_64) {
            return readByteAtLongOffset(offset.toLong());
        }
        return readByteAtIntOffset(offset.toInt());
    }

    @BUILTIN(builtinClass = PointerLoadBuiltin.GetByte.class)
    private native byte builtinGetByte(int displacement, int index);

    @INLINE
    public final byte getByte(int displacement, int index) {
        if (risc() || Word.isBoxed()) {
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
        return UnsafeLoophole.byteToBoolean(readByte(offset));
    }

    @INLINE
    public final boolean readBoolean(int offset) {
        return UnsafeLoophole.byteToBoolean(readByte(offset));
    }

    @INLINE
    public final boolean getBoolean(int displacement, int index) {
        return UnsafeLoophole.byteToBoolean(getByte(displacement, index));
    }

    @INLINE
    public final boolean getBoolean(int index) {
        return getBoolean(0, index);
    }

    @INLINE
    public final boolean getBoolean() {
        return getBoolean(0);
    }


    @BUILTIN(builtinClass = PointerLoadBuiltin.ReadShortAtLongOffset.class)
    protected native short readShortAtLongOffset(long offset);

    @BUILTIN(builtinClass = PointerLoadBuiltin.ReadShortAtIntOffset.class)
    protected native short readShortAtIntOffset(int offset);

    @INLINE
    public final short readShort(int offset) {
        return readShortAtIntOffset(offset);
    }

    @INLINE
    public final short readShort(Offset offset) {
        if (Word.width() == WordWidth.BITS_64) {
            return readShortAtLongOffset(offset.toLong());
        }
        return readShortAtIntOffset(offset.toInt());
    }

    @BUILTIN(builtinClass = PointerLoadBuiltin.GetShort.class)
    private native short builtinGetShort(int displacement, int index);

    @INLINE
    public final short getShort(int displacement, int index) {
        if (risc() || Word.isBoxed()) {
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


    @BUILTIN(builtinClass = PointerLoadBuiltin.ReadCharAtLongOffset.class)
    protected native char readCharAtLongOffset(long offset);

    @BUILTIN(builtinClass = PointerLoadBuiltin.ReadCharAtIntOffset.class)
    protected native char readCharAtIntOffset(int offset);

    @INLINE
    public final char readChar(int offset) {
        return readCharAtIntOffset(offset);
    }

    @INLINE
    public final char readChar(Offset offset) {
        if (Word.width() == WordWidth.BITS_64) {
            return readCharAtLongOffset(offset.toLong());
        }
        return readCharAtIntOffset(offset.toInt());
    }

    @BUILTIN(builtinClass = PointerLoadBuiltin.GetChar.class)
    private native char builtinGetChar(int displacement, int index);

    @INLINE
    public final char getChar(int displacement, int index) {
        if (risc() || Word.isBoxed()) {
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


    @BUILTIN(builtinClass = PointerLoadBuiltin.ReadIntAtLongOffset.class)
    protected native int readIntAtLongOffset(long offset);

    @BUILTIN(builtinClass = PointerLoadBuiltin.ReadIntAtIntOffset.class)
    protected native int readIntAtIntOffset(int offset);

    @INLINE
    public final int readInt(int offset) {
        return readIntAtIntOffset(offset);
    }

    @INLINE
    public final int readInt(Offset offset) {
        if (Word.width() == WordWidth.BITS_64) {
            return readIntAtLongOffset(offset.toLong());
        }
        return readIntAtIntOffset(offset.toInt());
    }

    @BUILTIN(builtinClass = PointerLoadBuiltin.GetInt.class)
    private native int builtinGetInt(int displacement, int index);

    @INLINE
    public final int getInt(int displacement, int index) {
        if (risc() || Word.isBoxed()) {
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


    @BUILTIN(builtinClass = PointerLoadBuiltin.ReadFloatAtLongOffset.class)
    protected native float readFloatAtLongOffset(long offset);

    @BUILTIN(builtinClass = PointerLoadBuiltin.ReadFloatAtIntOffset.class)
    protected native float readFloatAtIntOffset(int offset);

    @INLINE
    public final float readFloat(int offset) {
        return readFloatAtIntOffset(offset);
    }

    @INLINE
    public final float readFloat(Offset offset) {
        if (Word.width() == WordWidth.BITS_64) {
            return readFloatAtLongOffset(offset.toLong());
        }
        return readFloatAtIntOffset(offset.toInt());
    }

    @BUILTIN(builtinClass = PointerLoadBuiltin.GetFloat.class)
    private native float builtinGetFloat(int displacement, int index);

    @INLINE
    public final float getFloat(int displacement, int index) {
        if (risc() || Word.isBoxed()) {
            return readFloat(Offset.fromInt(index).times(Floats.SIZE).plus(displacement));
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


    @BUILTIN(builtinClass = PointerLoadBuiltin.ReadLongAtLongOffset.class)
    protected native long readLongAtLongOffset(long offset);

    @BUILTIN(builtinClass = PointerLoadBuiltin.ReadLongAtIntOffset.class)
    protected native long readLongAtIntOffset(int offset);

    @INLINE
    public final long readLong(int offset) {
        return readLongAtIntOffset(offset);
    }

    @INLINE
    public final long readLong(Offset offset) {
        if (Word.width() == WordWidth.BITS_64) {
            return readLongAtLongOffset(offset.toLong());
        }
        return readLongAtIntOffset(offset.toInt());
    }

    @BUILTIN(builtinClass = PointerLoadBuiltin.GetLong.class)
    private native long builtinGetLong(int displacement, int index);

    @INLINE
    public final long getLong(int displacement, int index) {
        if (risc() || Word.isBoxed()) {
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


    @BUILTIN(builtinClass = PointerLoadBuiltin.ReadDoubleAtLongOffset.class)
    protected native double readDoubleAtLongOffset(long offset);

    @BUILTIN(builtinClass = PointerLoadBuiltin.ReadDoubleAtIntOffset.class)
    protected native double readDoubleAtIntOffset(int offset);

    @INLINE
    public final double readDouble(int offset) {
        return readDoubleAtIntOffset(offset);
    }

    @INLINE
    public final double readDouble(Offset offset) {
        if (Word.width() == WordWidth.BITS_64) {
            return readDoubleAtLongOffset(offset.toLong());
        }
        return readDoubleAtIntOffset(offset.toInt());
    }

    @BUILTIN(builtinClass = PointerLoadBuiltin.GetDouble.class)
    private native double builtinGetDouble(int displacement, int index);

    @INLINE
    public final double getDouble(int displacement, int index) {
        if (risc() || Word.isBoxed()) {
            return readDouble(Offset.fromInt(index).times(Doubles.SIZE).plus(displacement));
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


    @BUILTIN(builtinClass = PointerLoadBuiltin.ReadWordAtLongOffset.class)
    protected native Word readWordAtLongOffset(long offset);

    @BUILTIN(builtinClass = PointerLoadBuiltin.ReadWordAtIntOffset.class)
    protected native Word readWordAtIntOffset(int offset);

    @INLINE
    public final Word readWord(int offset) {
        return readWordAtIntOffset(offset);
    }

    @INLINE
    public final Word readWord(Offset offset) {
        if (Word.width() == WordWidth.BITS_64) {
            return readWordAtLongOffset(offset.toLong());
        }
        return readWordAtIntOffset(offset.toInt());
    }

    @BUILTIN(builtinClass = PointerLoadBuiltin.GetWord.class)
    private native Word builtinGetWord(int displacement, int index);

    @INLINE
    public final Word getWord(int displacement, int index) {
        if (risc() || Word.isBoxed()) {
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


    @INLINE
    public final Grip readGrip(Offset offset) {
        return readReference(offset).toGrip();
    }

    @INLINE
    public final Grip readGrip(int offset) {
        return readReference(offset).toGrip();
    }

    @INLINE
    public final Grip getGrip(int displacement, int index) {
        return getReference(displacement, index).toGrip();
    }

    @INLINE
    public final Grip getGrip(int index) {
        return getGrip(0, index);
    }

    @INLINE
    public final Grip getGrip() {
        return getGrip(0);
    }


    @BUILTIN(builtinClass = PointerLoadBuiltin.ReadReferenceAtLongOffset.class)
    protected native Reference readReferenceAtLongOffset(long offset);

    @BUILTIN(builtinClass = PointerLoadBuiltin.ReadReferenceAtIntOffset.class)
    protected native Reference readReferenceAtIntOffset(int offset);

    @INLINE
    public final Reference readReference(int offset) {
        return readReferenceAtIntOffset(offset);
    }

    @INLINE
    public final Reference readReference(Offset offset) {
        if (Word.width() == WordWidth.BITS_64) {
            return readReferenceAtLongOffset(offset.toLong());
        }
        return readReferenceAtIntOffset(offset.toInt());
    }

    @BUILTIN(builtinClass = PointerLoadBuiltin.GetReference.class)
    private native Reference builtinGetReference(int displacement, int index);

    @INLINE
    public final Reference getReference(int displacement, int index) {
        if (risc() || Word.isBoxed()) {
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


    @BUILTIN(builtinClass = PointerStoreBuiltin.WriteByteAtLongOffset.class)
    protected native void writeByteAtLongOffset(long offset, byte value);

    @BUILTIN(builtinClass = PointerStoreBuiltin.WriteByteAtIntOffset.class)
    protected native void writeByteAtIntOffset(int offset, byte value);

    @INLINE
    public final void writeByte(int offset, byte value) {
        writeByteAtIntOffset(offset, value);
    }

    @INLINE
    public final void writeByte(Offset offset, byte value) {
        if (Word.width() == WordWidth.BITS_64) {
            writeByteAtLongOffset(offset.toLong(), value);
        } else {
            writeByteAtIntOffset(offset.toInt(), value);
        }
    }

    @BUILTIN(builtinClass = PointerStoreBuiltin.SetByte.class)
    private native void builtinSetByte(int displacement, int index, byte value);

    @INLINE
    public final void setByte(int displacement, int index, byte value) {
        if (risc() || Word.isBoxed()) {
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
        writeByte(offset, UnsafeLoophole.booleanToByte(value));
    }

    @INLINE
    public final void writeBoolean(int offset, boolean value) {
        writeByte(offset, UnsafeLoophole.booleanToByte(value));
    }

    @INLINE
    public final void setBoolean(int displacement, int index, boolean value) {
        setByte(displacement, index, UnsafeLoophole.booleanToByte(value));
    }

    @INLINE
    public final void setBoolean(int index, boolean value) {
        setBoolean(0, index, value);
    }

    @INLINE
    public final void setBoolean(boolean value) {
        setBoolean(0, value);
    }


    @BUILTIN(builtinClass = PointerStoreBuiltin.WriteShortAtLongOffset.class)
    protected native void writeShortAtLongOffset(long offset, short value);

    @BUILTIN(builtinClass = PointerStoreBuiltin.WriteShortAtIntOffset.class)
    protected native void writeShortAtIntOffset(int offset, short value);

    @INLINE
    public final void writeShort(int offset, short value) {
        writeShortAtIntOffset(offset, value);
    }

    @INLINE
    public final void writeShort(Offset offset, short value) {
        if (Word.width() == WordWidth.BITS_64) {
            writeShortAtLongOffset(offset.toLong(), value);
        } else {
            writeShortAtIntOffset(offset.toInt(), value);
        }
    }

    @BUILTIN(builtinClass = PointerStoreBuiltin.SetShort.class)
    private native void builtinSetShort(int displacement, int index, short value);

    @INLINE
    public final void setShort(int displacement, int index, short value) {
        if (risc() || Word.isBoxed()) {
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
        writeShort(offset, UnsafeLoophole.charToShort(value));
    }

    @INLINE
    public final void writeChar(int offset, char value) {
        writeShort(offset, UnsafeLoophole.charToShort(value));
    }

    @INLINE
    public final void setChar(int displacement, int index, char value) {
        setShort(displacement, index, UnsafeLoophole.charToShort(value));
    }

    @INLINE
    public final void setChar(int index, char value) {
        setChar(0, index, value);
    }

    @INLINE
    public final void setChar(char value) {
        setChar(0, value);
    }


    @BUILTIN(builtinClass = PointerStoreBuiltin.WriteIntAtLongOffset.class)
    protected native void writeIntAtLongOffset(long offset, int value);

    @BUILTIN(builtinClass = PointerStoreBuiltin.WriteIntAtIntOffset.class)
    protected native void writeIntAtIntOffset(int offset, int value);

    @INLINE
    public final void writeInt(int offset, int value) {
        writeIntAtIntOffset(offset, value);
    }

    @INLINE
    public final void writeInt(Offset offset, int value) {
        if (Word.width() == WordWidth.BITS_64) {
            writeIntAtLongOffset(offset.toLong(), value);
        } else {
            writeIntAtIntOffset(offset.toInt(), value);
        }
    }

    @BUILTIN(builtinClass = PointerStoreBuiltin.SetInt.class)
    private native void builtinSetInt(int displacement, int index, int value);

    @INLINE
    public final void setInt(int displacement, int index, int value) {
        if (risc() || Word.isBoxed()) {
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


    @BUILTIN(builtinClass = PointerStoreBuiltin.WriteFloatAtLongOffset.class)
    protected native void writeFloatAtLongOffset(long offset, float value);

    @BUILTIN(builtinClass = PointerStoreBuiltin.WriteFloatAtIntOffset.class)
    protected native void writeFloatAtIntOffset(int offset, float value);

    @INLINE
    public final void writeFloat(int offset, float value) {
        writeFloatAtIntOffset(offset, value);
    }

    @INLINE
    public final void writeFloat(Offset offset, float value) {
        if (Word.width() == WordWidth.BITS_64) {
            writeFloatAtLongOffset(offset.toLong(), value);
        } else {
            writeFloatAtIntOffset(offset.toInt(), value);
        }
    }

    @BUILTIN(builtinClass = PointerStoreBuiltin.SetFloat.class)
    private native void builtinSetFloat(int displacement, int index, float value);

    @INLINE
    public final void setFloat(int displacement, int index, float value) {
        if (risc() || Word.isBoxed()) {
            writeFloat(Offset.fromInt(index).times(Floats.SIZE).plus(displacement), value);
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


    @BUILTIN(builtinClass = PointerStoreBuiltin.WriteLongAtLongOffset.class)
    protected native void writeLongAtLongOffset(long offset, long value);

    @BUILTIN(builtinClass = PointerStoreBuiltin.WriteLongAtIntOffset.class)
    protected native void writeLongAtIntOffset(int offset, long value);

    @INLINE
    public final void writeLong(int offset, long value) {
        writeLongAtIntOffset(offset, value);
    }

    @INLINE
    public final void writeLong(Offset offset, long value) {
        if (Word.width() == WordWidth.BITS_64) {
            writeLongAtLongOffset(offset.toLong(), value);
        } else {
            writeLongAtIntOffset(offset.toInt(), value);
        }
    }

    @BUILTIN(builtinClass = PointerStoreBuiltin.SetLong.class)
    private native void builtinSetLong(int displacement, int index, long value);

    @INLINE
    public final void setLong(int displacement, int index, long value) {
        if (risc() || Word.isBoxed()) {
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


    @BUILTIN(builtinClass = PointerStoreBuiltin.WriteDoubleAtLongOffset.class)
    protected native void writeDoubleAtLongOffset(long offset, double value);

    @BUILTIN(builtinClass = PointerStoreBuiltin.WriteDoubleAtIntOffset.class)
    protected native void writeDoubleAtIntOffset(int offset, double value);

    @INLINE
    public final void writeDouble(int offset, double value) {
        writeDoubleAtIntOffset(offset, value);
    }

    @INLINE
    public final void writeDouble(Offset offset, double value) {
        if (Word.width() == WordWidth.BITS_64) {
            writeDoubleAtLongOffset(offset.toLong(), value);
        } else {
            writeDoubleAtIntOffset(offset.toInt(), value);
        }
    }

    @BUILTIN(builtinClass = PointerStoreBuiltin.SetDouble.class)
    private native void builtinSetDouble(int displacement, int index, double value);

    @INLINE
    public final void setDouble(int displacement, int index, double value) {
        if (risc() || Word.isBoxed()) {
            writeDouble(Offset.fromInt(index).times(Doubles.SIZE).plus(displacement), value);
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


    @BUILTIN(builtinClass = PointerStoreBuiltin.WriteWordAtLongOffset.class)
    protected native void writeWordAtLongOffset(long offset, Word value);

    @BUILTIN(builtinClass = PointerStoreBuiltin.WriteWordAtIntOffset.class)
    protected native void writeWordAtIntOffset(int offset, Word value);

    @INLINE
    public final void writeWord(int offset, Word value) {
        writeWordAtIntOffset(offset, value);
    }

    @INLINE
    public final void writeWord(Offset offset, Word value) {
        if (Word.width() == WordWidth.BITS_64) {
            writeWordAtLongOffset(offset.toLong(), value);
        } else {
            writeWordAtIntOffset(offset.toInt(), value);
        }
    }

    @BUILTIN(builtinClass = PointerStoreBuiltin.SetWord.class)
    private native void builtinSetWord(int displacement, int index, Word value);

    @INLINE
    public final void setWord(int displacement, int index, Word value) {
        if (risc() || Word.isBoxed()) {
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


    @INLINE
    public final void writeGrip(Offset offset, Grip value) {
        writeReference(offset, value.toReference());
    }

    @INLINE
    public final void writeGrip(int offset, Grip value) {
        writeReference(offset, value.toReference());
    }

    @INLINE
    public final void setGrip(int displacement, int index, Grip value) {
        setReference(displacement, index, value.toReference());
    }

    @INLINE
    public final void setGrip(int index, Grip value) {
        setGrip(0, index, value);
    }

    @INLINE
    public final void setGrip(Grip value) {
        setGrip(0, value);
    }


    @BUILTIN(builtinClass = PointerStoreBuiltin.WriteReferenceAtLongOffset.class)
    protected native void writeReferenceAtLongOffset(long offset, Reference value);

    @BUILTIN(builtinClass = PointerStoreBuiltin.WriteReferenceAtIntOffset.class)
    protected native void writeReferenceAtIntOffset(int offset, Reference value);

    @INLINE
    public final void writeReference(int offset, Reference value) {
        writeReferenceAtIntOffset(offset, value);
    }

    @INLINE
    public final void writeReference(Offset offset, Reference value) {
        if (Word.width() == WordWidth.BITS_64) {
            writeReferenceAtLongOffset(offset.toLong(), value);
        } else {
            writeReferenceAtIntOffset(offset.toInt(), value);
        }
    }

    @BUILTIN(builtinClass = PointerStoreBuiltin.SetReference.class)
    private native void builtinSetReference(int displacement, int index, Reference value);

    @INLINE
    public final void setReference(int displacement, int index, Reference value) {
        if (risc() || Word.isBoxed()) {
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


    @BUILTIN(builtinClass = PointerAtomicBuiltin.CompareAndSwapIntAtLongOffset.class)
    protected native int compareAndSwapIntAtLongOffset(long offset, int suspectedValue, int newValue);

    @BUILTIN(builtinClass = PointerAtomicBuiltin.CompareAndSwapIntAtIntOffset.class)
    protected native int compareAndSwapIntAtIntOffset(int offset, int suspectedValue, int newValue);

    @INLINE
    public final int compareAndSwapInt(int offset, int suspectedValue, int newValue) {
        return compareAndSwapIntAtIntOffset(offset, suspectedValue, newValue);
    }

    @INLINE
    public final int compareAndSwapInt(int suspectedValue, int newValue) {
        return compareAndSwapInt(0, suspectedValue, newValue);
    }

    @INLINE
    public final int compareAndSwapInt(Offset offset, int suspectedValue, int newValue) {
        if (Word.width() == WordWidth.BITS_64) {
            return compareAndSwapIntAtLongOffset(offset.toLong(), suspectedValue, newValue);
        }
        return compareAndSwapIntAtIntOffset(offset.toInt(), suspectedValue, newValue);
    }


    @BUILTIN(builtinClass = PointerAtomicBuiltin.CompareAndSwapWordAtLongOffset.class)
    protected native Word compareAndSwapWordAtLongOffset(long offset, Word suspectedValue, Word newValue);

    @BUILTIN(builtinClass = PointerAtomicBuiltin.CompareAndSwapWordAtIntOffset.class)
    protected native Word compareAndSwapWordAtIntOffset(int offset, Word suspectedValue, Word newValue);

    @INLINE
    public final Word compareAndSwapWord(int offset, Word suspectedValue, Word newValue) {
        return compareAndSwapWordAtIntOffset(offset, suspectedValue, newValue);
    }

    @INLINE
    public final Word compareAndSwapWord(Word suspectedValue, Word newValue) {
        return compareAndSwapWord(0, suspectedValue, newValue);
    }

    @INLINE
    public final Word compareAndSwapWord(Offset offset, Word suspectedValue, Word newValue) {
        if (Word.width() == WordWidth.BITS_64) {
            return compareAndSwapWordAtLongOffset(offset.toLong(), suspectedValue, newValue);
        }
        return compareAndSwapWordAtIntOffset(offset.toInt(), suspectedValue, newValue);
    }


    @BUILTIN(builtinClass = PointerAtomicBuiltin.CompareAndSwapReferenceAtLongOffset.class)
    protected native Reference compareAndSwapReferenceAtLongOffset(long offset, Reference suspectedValue, Reference newValue);

    @BUILTIN(builtinClass = PointerAtomicBuiltin.CompareAndSwapReferenceAtIntOffset.class)
    protected native Reference compareAndSwapReferenceAtIntOffset(int offset, Reference suspectedValue, Reference newValue);

    @INLINE
    public final Reference compareAndSwapReference(int offset, Reference suspectedValue, Reference newValue) {
        return compareAndSwapReferenceAtIntOffset(offset, suspectedValue, newValue);
    }

    @INLINE
    public final Reference compareAndSwapReference(Reference suspectedValue, Reference newValue) {
        return compareAndSwapReference(0, suspectedValue, newValue);
    }

    @INLINE
    public final Reference compareAndSwapReference(Offset offset, Reference suspectedValue, Reference newValue) {
        if (Word.width() == WordWidth.BITS_64) {
            return compareAndSwapReferenceAtLongOffset(offset.toLong(), suspectedValue, newValue);
        }
        return compareAndSwapReferenceAtIntOffset(offset.toInt(), suspectedValue, newValue);
    }

    /**
     * Sets the bit at the given index, counting bits in the bytes starting from this pointer.
     *
     * ATTENTION: There is no protection against concurrent access to the affected byte.
     *
     * This method may read the affected byte first, then set the bit in it and then write the byte back.
     */
    public void setBit(int bitIndex) {
        final int byteIndex = bitIndex / Bytes.WIDTH;
        byte byteValue = getByte(byteIndex);
        byteValue |= 1 << (bitIndex % Bytes.WIDTH);
        setByte(byteIndex, byteValue);
    }

    /**
     * Sets the given bits starting from the given bit index, counting bits in the bytes starting from this pointer.
     * Every 1 in the given bits will result in a 1 in memory.
     * Every 0 in the given bits will leave the corresponding memory bit unchanged.
     *
     * ATTENTION: There is no protection against concurrent access to affected bytes.
     *
     * This method may read each affected byte first, then set some bits in it and then write the byte back.
     * There are either 1 or 2 affected bytes, depending on alignment of the bit index to bytes in memory.
     */
    public void setBits(int bitIndex, byte bits) {
        // If we do not mask off the leading bits after a conversion to int right here,
        // then the arithmetic operations below will convert implicitly to int and may insert sign bits.
        final int intBits = bits & 0xff;

        int byteIndex = bitIndex / Bytes.WIDTH;
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

}
