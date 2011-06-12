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

import com.sun.max.unsafe.*;

/**
 * Reading/writing bytes and other primitive data kinds from/to a source/destination that can be identified by an {@link Address}.
 * For each kind, methods support direct addressing, offset addressing, and indexed addressing for arrays.
 */
public interface DataAccess extends DataIO {

    void readFully(Address address, ByteBuffer buffer);
    void readFully(Address address, byte[] bytes);
    byte[] readFully(Address address, int length);

    byte readByte(Address address);
    byte readByte(Address address, Offset offset);
    byte readByte(Address address, int offset);
    byte getByte(Address address, int displacement, int index);

    boolean readBoolean(Address address);
    boolean readBoolean(Address address, Offset offset);
    boolean readBoolean(Address address, int offset);
    boolean getBoolean(Address address, int displacement, int index);

    short readShort(Address address);
    short readShort(Address address, Offset offset);
    short readShort(Address address, int offset);
    short getShort(Address address, int displacement, int index);

    char readChar(Address address);
    char readChar(Address address, Offset offset);
    char readChar(Address address, int offset);
    char getChar(Address address, int displacement, int index);

    int readInt(Address address);
    int readInt(Address address, Offset offset);
    int readInt(Address address, int offset);
    int getInt(Address address, int displacement, int index);

    float readFloat(Address address);
    float readFloat(Address address, Offset offset);
    float readFloat(Address address, int offset);
    float getFloat(Address address, int displacement, int index);

    long readLong(Address address);
    long readLong(Address address, Offset offset);
    long readLong(Address address, int offset);
    long getLong(Address address, int displacement, int index);

    double readDouble(Address address);
    double readDouble(Address address, Offset offset);
    double readDouble(Address address, int offset);
    double getDouble(Address address, int displacement, int index);

    Word readWord(Address address);
    Word readWord(Address address, Offset offset);
    Word readWord(Address address, int offset);
    Word getWord(Address address, int displacement, int index);

    void writeBytes(Address address, byte[] bytes);
    void writeBuffer(Address address, ByteBuffer buffer);

    void writeByte(Address address, byte value);
    void writeByte(Address address, Offset offset, byte value);
    void writeByte(Address address, int offset, byte value);
    void setByte(Address address, int displacement, int index, byte value);

    void writeBoolean(Address address, boolean value);
    void writeBoolean(Address address, Offset offset, boolean value);
    void writeBoolean(Address address, int offset, boolean value);
    void setBoolean(Address address, int displacement, int index, boolean value);

    void writeShort(Address address, short value);
    void writeShort(Address address, Offset offset, short value);
    void writeShort(Address address, int offset, short value);
    void setShort(Address address, int displacement, int index, short value);

    void writeChar(Address address, char value);
    void writeChar(Address address, Offset offset, char value);
    void writeChar(Address address, int offset, char value);
    void setChar(Address address, int displacement, int index, char value);

    void writeInt(Address address, int value);
    void writeInt(Address address, Offset offset, int value);
    void writeInt(Address address, int offset, int value);
    void setInt(Address address, int displacement, int index, int value);

    void writeFloat(Address address, float value);
    void writeFloat(Address address, Offset offset, float value);
    void writeFloat(Address address, int offset, float value);
    void setFloat(Address address, int displacement, int index, float value);

    void writeLong(Address address, long value);
    void writeLong(Address address, Offset offset, long value);
    void writeLong(Address address, int offset, long value);
    void setLong(Address address, int displacement, int index, long value);

    void writeDouble(Address address, double value);
    void writeDouble(Address address, Offset offset, double value);
    void writeDouble(Address address, int offset, double value);
    void setDouble(Address address, int displacement, int index, double value);

    void writeWord(Address address, Word value);
    void writeWord(Address address, Offset offset, Word value);
    void writeWord(Address address, int offset, Word value);
    void setWord(Address address, int displacement, int index, Word value);

    void copyElements(Address address, int displacement, int srcIndex, Object dst, int dstIndex, int length);
}
