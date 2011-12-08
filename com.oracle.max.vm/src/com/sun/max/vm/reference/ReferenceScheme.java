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
package com.sun.max.vm.reference;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;

/**
 * Reference-based object access methods for mutator use.
 *
 * @see Reference
 */
public interface ReferenceScheme extends VMScheme {

    Reference fromOrigin(Pointer origin);

    Reference fromJava(Object object);

    Object toJava(Reference reference);
    Pointer toOrigin(Reference reference);

    /**
     * @return the "zero" ref that represents 'null'.
     */
    Reference zero();

    boolean isZero(Reference ref);

    boolean isAllOnes(Reference ref);

    boolean isMarked(Reference ref);

    boolean isTagged(Reference ref);

    Reference marked(Reference ref);

    Reference unmarked(Reference ref);

    byte readByte(Reference reference, Offset offset);
    byte readByte(Reference reference, int offset);
    byte getByte(Reference reference, int displacement, int index);

    boolean readBoolean(Reference reference, Offset offset);
    boolean readBoolean(Reference reference, int offset);
    boolean getBoolean(Reference reference, int displacement, int index);

    short readShort(Reference reference, Offset offset);
    short readShort(Reference reference, int offset);
    short getShort(Reference reference, int displacement, int index);

    char readChar(Reference reference, Offset offset);
    char readChar(Reference reference, int offset);
    char getChar(Reference reference, int displacement, int index);

    int readInt(Reference reference, Offset offset);
    int readInt(Reference reference, int offset);
    int getInt(Reference reference, int displacement, int index);

    float readFloat(Reference reference, Offset offset);
    float readFloat(Reference reference, int offset);
    float getFloat(Reference reference, int displacement, int index);

    long readLong(Reference reference, Offset offset);
    long readLong(Reference reference, int offset);
    long getLong(Reference reference, int displacement, int index);

    double readDouble(Reference reference, Offset offset);
    double readDouble(Reference reference, int offset);
    double getDouble(Reference reference, int displacement, int index);

    Word readWord(Reference reference, Offset offset);
    Word readWord(Reference reference, int offset);
    Word getWord(Reference reference, int displacement, int index);

    Reference readReference(Reference reference, Offset offset);
    Reference readReference(Reference reference, int offset);
    Reference getReference(Reference reference, int displacement, int index);

    void writeByte(Reference reference, Offset offset, byte value);
    void writeByte(Reference reference, int offset, byte value);
    void setByte(Reference reference, int displacement, int index, byte value);

    void writeBoolean(Reference reference, Offset offset, boolean value);
    void writeBoolean(Reference reference, int offset, boolean value);
    void setBoolean(Reference reference, int displacement, int index, boolean value);

    void writeShort(Reference reference, Offset offset, short value);
    void writeShort(Reference reference, int offset, short value);
    void setShort(Reference reference, int displacement, int index, short value);

    void writeChar(Reference reference, Offset offset, char value);
    void writeChar(Reference reference, int offset, char value);
    void setChar(Reference reference, int displacement, int index, char value);

    void writeInt(Reference reference, Offset offset, int value);
    void writeInt(Reference reference, int offset, int value);
    void setInt(Reference reference, int displacement, int index, int value);

    void writeFloat(Reference reference, Offset offset, float value);
    void writeFloat(Reference reference, int offset, float value);
    void setFloat(Reference reference, int displacement, int index, float value);

    void writeLong(Reference reference, Offset offset, long value);
    void writeLong(Reference reference, int offset, long value);
    void setLong(Reference reference, int displacement, int index, long value);

    void writeDouble(Reference reference, Offset offset, double value);
    void writeDouble(Reference reference, int offset, double value);
    void setDouble(Reference reference, int displacement, int index, double value);

    void writeWord(Reference reference, Offset offset, Word value);
    void writeWord(Reference reference, int offset, Word value);
    void setWord(Reference reference, int displacement, int index, Word value);

    void writeReference(Reference reference, Offset offset, Reference value);
    void writeReference(Reference reference, int offset, Reference value);
    void setReference(Reference reference, int displacement, int index, Reference value);

    /**
     * Atomically compares the contents of the memory location addressed by adding {@code offset} to {@code reference}
     * to a given value and, if they are the same, modifies the contents of that memory location to a given new value.
     *
     * @param reference the base of the memory location
     * @param offset the offset of the memory location
     * @param expectedValue the value that must currently exist in the memory location for the update to occur
     * @param newValue the value to which the memory is updated if its current value is {@code expectedValue}
     * @return the value of the memory location before this call; if it is equal to {@code expectedValue}, then the
     *         update occurred, otherwise the update did not occur (assuming {@code expectedValue != newValue})
     */
    int compareAndSwapInt(Reference reference, Offset offset, int expectedValue, int newValue);

    /**
     * @see #compareAndSwapInt(Reference, Offset, int, int)
     */
    int compareAndSwapInt(Reference reference, int offset, int expectedValue, int newValue);

    /**
     * @see #compareAndSwapInt(Reference, Offset, int, int)
     */
    Word compareAndSwapWord(Reference reference, Offset offset, Word expectedValue, Word newValue);

    /**
     * @see #compareAndSwapInt(Reference, Offset, int, int)
     */
    Word compareAndSwapWord(Reference reference, int offset, Word expectedValue, Word newValue);

    Reference compareAndSwapReference(Reference reference, Offset offset, Reference expectedValue, Reference newValue);
    Reference compareAndSwapReference(Reference reference, int offset, Reference expectedValue, Reference newValue);

    void copyElements(int displacement, Reference src, int srcIndex, Object dst, int dstIndex, int length);

    /**
     * Gets the byte pattern for a reference to be written into the boot image.
     *
     * @param origin the origin of the reference to be written
     */
    @HOSTED_ONLY
    byte[] asBytes(Pointer origin);

    /**
     * Gets the byte pattern for a null reference to be written into the boot image.
     */
    @HOSTED_ONLY
    byte[] nullAsBytes();


}
