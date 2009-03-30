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
package com.sun.max.tele.debug;

import java.nio.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;

/**
 * @author Bernd Mathiske
 */
public abstract class DataAccessAdapter implements DataAccess {

    private final WordWidth _wordWidth;

    protected DataAccessAdapter(WordWidth wordWidth) {
        _wordWidth = wordWidth;
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
        return readBoolean(address.plus(displacement).plus(index * Booleans.SIZE));
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
        return UnsafeLoophole.shortToChar(readShort(address));
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
        return UnsafeLoophole.intToFloat(readInt(address));
    }

    public float readFloat(Address address, Offset offset) {
        return readFloat(address.plus(offset));
    }

    public float readFloat(Address address, int offset) {
        return readFloat(address.plus(offset));
    }

    public float getFloat(Address address, int displacement, int index) {
        return readFloat(address.plus(displacement).plus(index * Floats.SIZE));
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
        return UnsafeLoophole.longToDouble(readLong(address));
    }

    public double readDouble(Address address, Offset offset) {
        return readDouble(address.plus(offset));
    }

    public double readDouble(Address address, int offset) {
        return readDouble(address.plus(offset));
    }

    public double getDouble(Address address, int displacement, int index) {
        return readDouble(address.plus(displacement).plus(index * Doubles.SIZE));
    }

    public Word readWord(Address address) {
        switch (_wordWidth) {
            case BITS_32:
                return Offset.fromInt(readInt(address));
            case BITS_64:
                return Offset.fromLong(readLong(address));
            default:
                throw ProgramError.unexpected();
        }
    }

    public Word readWord(Address address, Offset offset) {
        return readWord(address.plus(offset));
    }

    public Word readWord(Address address, int offset) {
        return readWord(address.plus(offset));
    }

    public Word getWord(Address address, int displacement, int index) {
        return readWord(address.plus(displacement).plus(index * _wordWidth.numberOfBytes()));
    }

    @Override
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
        writeBoolean(address.plus(displacement).plus(index * Booleans.SIZE), value);
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
        writeShort(address, UnsafeLoophole.charToShort(value));
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
        writeInt(address, UnsafeLoophole.floatToInt(value));
    }

    public void writeFloat(Address address, Offset offset, float value) {
        writeFloat(address.plus(offset), value);
    }

    public void writeFloat(Address address, int offset, float value) {
        writeFloat(address.plus(offset), value);
    }

    public void setFloat(Address address, int displacement, int index, float value) {
        writeFloat(address.plus(displacement).plus(index * Floats.SIZE), value);
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
        writeLong(address, UnsafeLoophole.doubleToLong(value));
    }

    public void writeDouble(Address address, Offset offset, double value) {
        writeDouble(address.plus(offset), value);
    }

    public void writeDouble(Address address, int offset, double value) {
        writeDouble(address.plus(offset), value);
    }

    public void setDouble(Address address, int displacement, int index, double value) {
        writeDouble(address.plus(displacement).plus(index * Doubles.SIZE), value);
    }

    public void writeWord(Address address, Word value) {
        switch (_wordWidth) {
            case BITS_32:
                writeInt(address, value.asOffset().toInt());
                break;
            case BITS_64:
                writeLong(address, value.asOffset().toLong());
                break;
            default:
                ProgramError.unexpected();
        }
    }

    public void writeWord(Address address, Offset offset, Word value) {
        writeWord(address.plus(offset), value);
    }

    public void writeWord(Address address, int offset, Word value) {
        writeWord(address.plus(offset), value);
    }

    public void setWord(Address address, int displacement, int index, Word value) {
        writeWord(address.plus(displacement).plus(index * _wordWidth.numberOfBytes()), value);
    }

}

