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
package com.sun.max.tele.data;

import java.nio.*;

import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;

/**
 * Implementation of {@link DataAccess} by direct main memory access.
 */
public final class MemoryDataAccess implements DataAccess {

    private MemoryDataAccess() {
    }

    public static final MemoryDataAccess MEMORY_DATA_ACCESS = new MemoryDataAccess();

    public void readFully(Address address, ByteBuffer buffer) {
        DataIO.Static.readFully(this, address, buffer);
    }

    public byte[] readFully(Address address, int length) {
        return DataIO.Static.readFully(this, address, length);
    }

    public void readFully(Address address, byte[] buffer) {
        readFully(address, ByteBuffer.wrap(buffer));
    }

    public int read(Address address, ByteBuffer buffer, int offset, int length) {
        throw FatalError.unimplemented();
    }

    public int write(ByteBuffer buffer, int offset, int length, Address toAddress) throws DataIOError {
        throw FatalError.unimplemented();
    }

    public void writeBuffer(Address address, ByteBuffer buffer) {
        throw FatalError.unimplemented();
    }

    public byte readByte(Address address) {
        return address.asPointer().readByte(0);
    }

    public byte readByte(Address address, Offset offset) {
        return address.asPointer().readByte(offset);
    }

    public byte readByte(Address address, int offset) {
        return address.asPointer().readByte(offset);
    }

    public byte getByte(Address address, int displacement, int index) {
        return address.asPointer().getByte(displacement, index);
    }

    public boolean readBoolean(Address address) {
        return address.asPointer().readBoolean(0);
    }

    public boolean readBoolean(Address address, Offset offset) {
        return address.asPointer().readBoolean(offset);
    }

    public boolean readBoolean(Address address, int offset) {
        return address.asPointer().readBoolean(offset);
    }

    public boolean getBoolean(Address address, int displacement, int index) {
        return address.asPointer().getBoolean(displacement, index);
    }

    public short readShort(Address address) {
        return address.asPointer().readShort(0);
    }

    public short readShort(Address address, Offset offset) {
        return address.asPointer().readShort(offset);
    }

    public short readShort(Address address, int offset) {
        return address.asPointer().readShort(offset);
    }

    public short getShort(Address address, int displacement, int index) {
        return address.asPointer().getShort(displacement, index);
    }

    public char readChar(Address address) {
        return address.asPointer().readChar(0);
    }

    public char readChar(Address address, Offset offset) {
        return address.asPointer().readChar(offset);
    }

    public char readChar(Address address, int offset) {
        return address.asPointer().readChar(offset);
    }

    public char getChar(Address address, int displacement, int index) {
        return address.asPointer().getChar(displacement, index);
    }

    public int readInt(Address address) {
        return address.asPointer().readInt(0);
    }

    public int readInt(Address address, Offset offset) {
        return address.asPointer().readInt(offset);
    }

    public int readInt(Address address, int offset) {
        return address.asPointer().readInt(offset);
    }

    public int getInt(Address address, int displacement, int index) {
        return address.asPointer().getInt(displacement, index);
    }

    public float readFloat(Address address) {
        return address.asPointer().readFloat(0);
    }

    public float readFloat(Address address, Offset offset) {
        return address.asPointer().readFloat(offset);
    }

    public float readFloat(Address address, int offset) {
        return address.asPointer().readFloat(offset);
    }

    public float getFloat(Address address, int displacement, int index) {
        return address.asPointer().getFloat(displacement, index);
    }

    public long readLong(Address address) {
        return address.asPointer().readLong(0);
    }

    public long readLong(Address address, Offset offset) {
        return address.asPointer().readLong(offset);
    }

    public long readLong(Address address, int offset) {
        return address.asPointer().readLong(offset);
    }

    public long getLong(Address address, int displacement, int index) {
        return address.asPointer().getLong(displacement, index);
    }

    public double readDouble(Address address) {
        return address.asPointer().readDouble(0);
    }

    public double readDouble(Address address, Offset offset) {
        return address.asPointer().readDouble(offset);
    }

    public double readDouble(Address address, int offset) {
        return address.asPointer().readDouble(offset);
    }

    public double getDouble(Address address, int displacement, int index) {
        return address.asPointer().getDouble(displacement, index);
    }

    public Word readWord(Address address) {
        return address.asPointer().readWord(0);
    }

    public Word readWord(Address address, Offset offset) {
        return address.asPointer().readWord(offset);
    }

    public Word readWord(Address address, int offset) {
        return address.asPointer().readWord(offset);
    }

    public Word getWord(Address address, int displacement, int index) {
        return address.asPointer().getWord(displacement, index);
    }

    public void writeBytes(Address address, byte[] bytes) {
        Memory.writeBytes(bytes, address.asPointer());
    }

    public void writeByte(Address address, byte value) {
        address.asPointer().writeByte(0, value);
    }

    public void writeByte(Address address, Offset offset, byte value) {
        address.asPointer().writeByte(offset, value);
    }

    public void writeByte(Address address, int offset, byte value) {
        address.asPointer().writeByte(offset, value);
    }

    public void setByte(Address address, int displacement, int index, byte value) {
        address.asPointer().setByte(displacement, index, value);
    }

    public void writeBoolean(Address address, boolean value) {
        address.asPointer().writeBoolean(0, value);
    }

    public void writeBoolean(Address address, Offset offset, boolean value) {
        address.asPointer().writeBoolean(offset, value);
    }

    public void writeBoolean(Address address, int offset, boolean value) {
        address.asPointer().writeBoolean(offset, value);
    }

    public void setBoolean(Address address, int displacement, int index, boolean value) {
        address.asPointer().setBoolean(displacement, index, value);
    }

    public void writeShort(Address address, short value) {
        address.asPointer().writeShort(0, value);
    }

    public void writeShort(Address address, Offset offset, short value) {
        address.asPointer().writeShort(offset, value);
    }

    public void writeShort(Address address, int offset, short value) {
        address.asPointer().writeShort(offset, value);
    }

    public void setShort(Address address, int displacement, int index, short value) {
        address.asPointer().setShort(displacement, index, value);
    }

    public void writeChar(Address address, char value) {
        address.asPointer().writeChar(0, value);
    }

    public void writeChar(Address address, Offset offset, char value) {
        address.asPointer().writeChar(offset, value);
    }

    public void writeChar(Address address, int offset, char value) {
        address.asPointer().writeChar(offset, value);
    }

    public void setChar(Address address, int displacement, int index, char value) {
        address.asPointer().setChar(displacement, index, value);
    }

    public void writeInt(Address address, int value) {
        address.asPointer().writeInt(0, value);
    }

    public void writeInt(Address address, Offset offset, int value) {
        address.asPointer().writeInt(offset, value);
    }

    public void writeInt(Address address, int offset, int value) {
        address.asPointer().writeInt(offset, value);
    }

    public void setInt(Address address, int displacement, int index, int value) {
        address.asPointer().setInt(displacement, index, value);
    }

    public void writeFloat(Address address, float value) {
        address.asPointer().writeFloat(0, value);
    }

    public void writeFloat(Address address, Offset offset, float value) {
        address.asPointer().writeFloat(offset, value);
    }

    public void writeFloat(Address address, int offset, float value) {
        address.asPointer().writeFloat(offset, value);
    }

    public void setFloat(Address address, int displacement, int index, float value) {
        address.asPointer().setFloat(displacement, index, value);
    }

    public void writeLong(Address address, long value) {
        address.asPointer().writeLong(0, value);
    }

    public void writeLong(Address address, Offset offset, long value) {
        address.asPointer().writeLong(offset, value);
    }

    public void writeLong(Address address, int offset, long value) {
        address.asPointer().writeLong(offset, value);
    }

    public void setLong(Address address, int displacement, int index, long value) {
        address.asPointer().setLong(displacement, index, value);
    }

    public void writeDouble(Address address, double value) {
        address.asPointer().writeDouble(0, value);
    }

    public void writeDouble(Address address, Offset offset, double value) {
        address.asPointer().writeDouble(offset, value);
    }

    public void writeDouble(Address address, int offset, double value) {
        address.asPointer().writeDouble(offset, value);
    }

    public void setDouble(Address address, int displacement, int index, double value) {
        address.asPointer().setDouble(displacement, index, value);
    }

    public void writeWord(Address address, Word value) {
        address.asPointer().writeWord(0, value);
    }

    public void writeWord(Address address, Offset offset, Word value) {
        address.asPointer().writeWord(offset, value);
    }

    public void writeWord(Address address, int offset, Word value) {
        address.asPointer().writeWord(offset, value);
    }

    public void setWord(Address address, int displacement, int index, Word value) {
        address.asPointer().setWord(displacement, index, value);
    }

    @Override
    public void copyElements(Address address, int displacement, int srcIndex, Object dst, int dstIndex, int length) {
        address.asPointer().copyElements(displacement, srcIndex, dst, dstIndex, length);
    }
}
