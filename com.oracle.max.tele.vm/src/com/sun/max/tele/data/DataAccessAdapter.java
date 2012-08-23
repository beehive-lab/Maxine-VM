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
package com.sun.max.tele.data;

import java.nio.*;

import com.sun.max.lang.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.type.*;

/**
 * An adapter for reading/writing bytes and other primitive data kinds from/to a source/destination
 * that can be identified by an {@link Address}.  Subclasses implement only the most basic
 * read/write operations.
 */
public abstract class DataAccessAdapter implements DataAccess {

    private static final int BOOLEAN_SIZE = 1;
    private static final int FLOAT_SIZE = 4;
    private static final int DOUBLE_SIZE = 8;

    private final WordWidth wordWidth;
    public final ByteOrder byteOrder;

    protected DataAccessAdapter(WordWidth wordWidth, ByteOrder byteOrder) {
        this.wordWidth = wordWidth;
        this.byteOrder = byteOrder;
    }

    public void readFully(Address address, ByteBuffer buffer) {
        DataIO.Static.readFully(this, address, buffer);
    }

    public byte[] readFully(Address address, int length) {
        return DataIO.Static.readFully(this, address, length);
    }

    public void readFully(Address address, byte[] buffer) {
        DataIO.Static.readFully(this, address, ByteBuffer.wrap(buffer));
    }

    public byte readByte(Address address, Offset offset) {
        return readByte(address.plus(offset));
    }

    public byte readByte(Address address, int offset) {
        return readByte(address.plus(offset));
    }

    public byte getByte(Address address, int displacement, int index) {
        return readByte(address.plus(displacement).plus(index * Bytes.SIZE));
    }

    public boolean readBoolean(Address address) {
        return readByte(address) != (byte) 0;
    }

    public boolean readBoolean(Address address, Offset offset) {
        return readBoolean(address.plus(offset));
    }

    public boolean readBoolean(Address address, int offset) {
        return readBoolean(address.plus(offset));
    }

    public boolean getBoolean(Address address, int displacement, int index) {
        return readBoolean(address.plus(displacement).plus(index * BOOLEAN_SIZE));
    }

    public short readShort(Address address, Offset offset) {
        return readShort(address.plus(offset));
    }

    public short readShort(Address address, int offset) {
        return readShort(address.plus(offset));
    }

    public short getShort(Address address, int displacement, int index) {
        return readShort(address.plus(displacement).plus(index * Shorts.SIZE));
    }

    public char readChar(Address address) {
        return UnsafeCast.asChar(readShort(address));
    }

    public char readChar(Address address, Offset offset) {
        return readChar(address.plus(offset));
    }

    public char readChar(Address address, int offset) {
        return readChar(address.plus(offset));
    }

    public char getChar(Address address, int displacement, int index) {
        return readChar(address.plus(displacement).plus(index * Chars.SIZE));
    }

    public int readInt(Address address, Offset offset) {
        return readInt(address.plus(offset));
    }

    public int readInt(Address address, int offset) {
        return readInt(address.plus(offset));
    }

    public int getInt(Address address, int displacement, int index) {
        return readInt(address.plus(displacement).plus(index * Ints.SIZE));
    }

    public float readFloat(Address address) {
        return Float.intBitsToFloat(readInt(address));
    }

    public float readFloat(Address address, Offset offset) {
        return readFloat(address.plus(offset));
    }

    public float readFloat(Address address, int offset) {
        return readFloat(address.plus(offset));
    }

    public float getFloat(Address address, int displacement, int index) {
        return readFloat(address.plus(displacement).plus(index * FLOAT_SIZE));
    }

    public long readLong(Address address, Offset offset) {
        return readLong(address.plus(offset));
    }

    public long readLong(Address address, int offset) {
        return readLong(address.plus(offset));
    }

    public long getLong(Address address, int displacement, int index) {
        return readLong(address.plus(displacement).plus(index * Longs.SIZE));
    }

    public double readDouble(Address address) {
        return Double.longBitsToDouble(readLong(address));
    }

    public double readDouble(Address address, Offset offset) {
        return readDouble(address.plus(offset));
    }

    public double readDouble(Address address, int offset) {
        return readDouble(address.plus(offset));
    }

    public double getDouble(Address address, int displacement, int index) {
        return readDouble(address.plus(displacement).plus(index * DOUBLE_SIZE));
    }

    public Word readWord(Address address) {
        switch (wordWidth) {
            case BITS_32:
                return Offset.fromInt(readInt(address));
            case BITS_64:
                return Offset.fromLong(readLong(address));
            default:
                throw TeleError.unexpected();
        }
    }

    public Word readWord(Address address, Offset offset) {
        return readWord(address.plus(offset));
    }

    public Word readWord(Address address, int offset) {
        return readWord(address.plus(offset));
    }

    public Word getWord(Address address, int displacement, int index) {
        return readWord(address.plus(displacement).plus(index * wordWidth.numberOfBytes));
    }

    public void writeBuffer(Address address, ByteBuffer buffer) {
        final int length = buffer.capacity();
        final int bytesWritten = write(buffer, 0, length, address);
        if (bytesWritten < 0) {
            throw new DataIOError(address);
        }
        if (bytesWritten != length) {
            throw new DataIOError(address, (length - bytesWritten) + " of " + length + " bytes unwritten");
        }
    }

    public void writeBytes(Address address, byte[] bytes) {
        writeBuffer(address, ByteBuffer.wrap(bytes));
    }

    public void writeByte(Address address, Offset offset, byte value) {
        writeByte(address.plus(offset), value);
    }

    public void writeByte(Address address, int offset, byte value) {
        writeByte(address.plus(offset), value);
    }

    public void setByte(Address address, int displacement, int index, byte value) {
        writeByte(address.plus(displacement).plus(index * Bytes.SIZE), value);
    }

    public void writeBoolean(Address address, boolean value) {
        writeByte(address, value ? (byte) 1 : (byte) 0);
    }

    public void writeBoolean(Address address, Offset offset, boolean value) {
        writeBoolean(address.plus(offset), value);
    }

    public void writeBoolean(Address address, int offset, boolean value) {
        writeBoolean(address.plus(offset), value);
    }

    public void setBoolean(Address address, int displacement, int index, boolean value) {
        writeBoolean(address.plus(displacement).plus(index * BOOLEAN_SIZE), value);
    }

    public void writeShort(Address address, Offset offset, short value) {
        writeShort(address.plus(offset), value);
    }

    public void writeShort(Address address, int offset, short value) {
        writeShort(address.plus(offset), value);
    }

    public void setShort(Address address, int displacement, int index, short value) {
        writeShort(address.plus(displacement).plus(index * Shorts.SIZE), value);
    }

    public void writeChar(Address address, char value) {
        writeShort(address, UnsafeCast.asShort(value));
    }

    public void writeChar(Address address, Offset offset, char value) {
        writeChar(address.plus(offset), value);
    }

    public void writeChar(Address address, int offset, char value) {
        writeChar(address.plus(offset), value);
    }

    public void setChar(Address address, int displacement, int index, char value) {
        writeChar(address.plus(displacement).plus(index * Chars.SIZE), value);
    }

    public void writeInt(Address address, Offset offset, int value) {
        writeInt(address.plus(offset), value);
    }

    public void writeInt(Address address, int offset, int value) {
        writeInt(address.plus(offset), value);
    }

    public void setInt(Address address, int displacement, int index, int value) {
        writeInt(address.plus(displacement).plus(index * Ints.SIZE), value);
    }

    public void writeFloat(Address address, float value) {
        writeInt(address, Float.floatToRawIntBits(value));
    }

    public void writeFloat(Address address, Offset offset, float value) {
        writeFloat(address.plus(offset), value);
    }

    public void writeFloat(Address address, int offset, float value) {
        writeFloat(address.plus(offset), value);
    }

    public void setFloat(Address address, int displacement, int index, float value) {
        writeFloat(address.plus(displacement).plus(index * FLOAT_SIZE), value);
    }

    public void writeLong(Address address, Offset offset, long value) {
        writeLong(address.plus(offset), value);
    }

    public void writeLong(Address address, int offset, long value) {
        writeLong(address.plus(offset), value);
    }

    public void setLong(Address address, int displacement, int index, long value) {
        writeLong(address.plus(displacement).plus(index * Longs.SIZE), value);
    }

    public void writeDouble(Address address, double value) {
        writeLong(address, Double.doubleToRawLongBits(value));
    }

    public void writeDouble(Address address, Offset offset, double value) {
        writeDouble(address.plus(offset), value);
    }

    public void writeDouble(Address address, int offset, double value) {
        writeDouble(address.plus(offset), value);
    }

    public void setDouble(Address address, int displacement, int index, double value) {
        writeDouble(address.plus(displacement).plus(index * DOUBLE_SIZE), value);
    }

    public void writeWord(Address address, Word value) {
        switch (wordWidth) {
            case BITS_32:
                writeInt(address, value.asOffset().toInt());
                break;
            case BITS_64:
                writeLong(address, value.asOffset().toLong());
                break;
            default:
                throw TeleError.unexpected();
        }
    }

    public void writeWord(Address address, Offset offset, Word value) {
        writeWord(address.plus(offset), value);
    }

    public void writeWord(Address address, int offset, Word value) {
        writeWord(address.plus(offset), value);
    }

    public void setWord(Address address, int displacement, int index, Word value) {
        writeWord(address.plus(displacement).plus(index * wordWidth.numberOfBytes), value);
    }

    // Checkstyle: stop
    public synchronized void copyElements(Address address, int displacement, int srcIndex, Object dst, int dstIndex, int length) {
        Kind kind = Kind.fromJava(dst.getClass().getComponentType());
        int size = length * kind.width.numberOfBytes;
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        Static.readFully(this, address.plus(displacement), byteBuffer);
        byteBuffer.order(byteOrder);
        byteBuffer.position(0);
        switch (kind.asEnum) {
            case BOOLEAN: {
                boolean[] arr = (boolean[]) dst;
                for (int i = 0; i < length; ++i) {
                    arr[dstIndex + i] = byteBuffer.get(i) != 0;
                }
                break;
            }
            case BYTE:     byteBuffer.get((byte[]) dst, dstIndex, length);  break;
            case CHAR:     byteBuffer.asCharBuffer().get((char[]) dst, dstIndex, length);  break;
            case SHORT:    byteBuffer.asShortBuffer().get((short[]) dst, dstIndex, length); break;
            case INT:      byteBuffer.asIntBuffer().get((int[]) dst, dstIndex, length); break;
            case FLOAT:    byteBuffer.asFloatBuffer().get((float[]) dst, dstIndex, length); break;
            case LONG:     byteBuffer.asLongBuffer().get((long[]) dst, dstIndex, length); break;
            case DOUBLE:   byteBuffer.asDoubleBuffer().get((double[]) dst, dstIndex, length); break;
            case WORD: {
                if (wordWidth == WordWidth.BITS_32) {
                    IntBuffer intBuffer = byteBuffer.asIntBuffer();
                    Word[] arr = (Word[]) dst;
                    for (int i = 0; i < length; ++i) {
                        WordArray.set(arr, dstIndex + i, Offset.fromInt(intBuffer.get()));
                    }
                } else if (wordWidth == WordWidth.BITS_64) {
                    LongBuffer longBuffer = byteBuffer.asLongBuffer();
                    Word[] arr = (Word[]) dst;
                    for (int i = 0; i < length; ++i) {
                        WordArray.set(arr, dstIndex + i, Offset.fromLong(longBuffer.get()));
                    }
                } else {
                    throw TeleError.unexpected();
                }
                break;
            }
            default:
                throw TeleError.unexpected("invalid type");
        }
    }
    // Checkstyle: resume

}

