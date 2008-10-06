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
/*VCSID=9a4959f9-4cec-44bb-830c-87b2cf01c23c*/
package com.sun.max.unsafe;

import com.sun.max.vm.grip.*;
import com.sun.max.vm.reference.*;

/**
 * ATTENTION: DO NOT USE THIS INTERFACE UNLESS YOU KNOW EXACTLY WHAT YOU ARE DOING!!!
 *
 * Common interface for pointers and for object references.
 *
 * The object layout implementation is written ONCE against this type
 * (instead of twice, once against Pointer and once against Reference).
 *
 * @author Bernd Mathiske
 */
public interface Accessor {

    boolean isZero();

    byte readByte(Offset offset);
    byte readByte(int offset);
    byte getByte(int displacement, int index);

    void writeByte(Offset offset, byte value);
    void writeByte(int offset, byte value);
    void setByte(int displacement, int index, byte value);

    boolean readBoolean(Offset offset);
    boolean readBoolean(int offset);
    boolean getBoolean(int displacement, int index);

    void writeBoolean(Offset offset, boolean value);
    void writeBoolean(int offset, boolean value);
    void setBoolean(int displacement, int index, boolean value);

    short readShort(Offset offset);
    short readShort(int offset);
    short getShort(int displacement, int index);

    void writeShort(Offset offset, short value);
    void writeShort(int offset, short value);
    void setShort(int displacement, int index, short value);

    char readChar(Offset offset);
    char readChar(int offset);
    char getChar(int displacement, int index);

    void writeChar(Offset offset, char value);
    void writeChar(int offset, char value);
    void setChar(int displacement, int index, char value);

    int readInt(Offset offset);
    int readInt(int offset);
    int getInt(int displacement, int index);

    void writeInt(Offset offset, int value);
    void writeInt(int offset, int value);
    void setInt(int displacement, int index, int value);

    float readFloat(Offset offset);
    float readFloat(int offset);
    float getFloat(int displacement, int index);

    void writeFloat(Offset offset, float value);
    void writeFloat(int offset, float value);
    void setFloat(int displacement, int index, float value);

    long readLong(Offset offset);
    long readLong(int offset);
    long getLong(int displacement, int index);

    void writeLong(Offset offset, long value);
    void writeLong(int offset, long value);
    void setLong(int displacement, int index, long value);

    double readDouble(Offset offset);
    double readDouble(int offset);
    double getDouble(int displacement, int index);

    void writeDouble(Offset offset, double value);
    void writeDouble(int offset, double value);
    void setDouble(int displacement, int index, double value);

    Word readWord(Offset offset);
    Word readWord(int offset);
    Word getWord(int displacement, int index);

    void writeWord(Offset offset, Word value);
    void writeWord(int offset, Word value);
    void setWord(int displacement, int index, Word value);

    Grip readGrip(Offset offset);
    Grip readGrip(int offset);
    Grip getGrip(int displacement, int index);

    void writeGrip(Offset offset, Grip value);
    void writeGrip(int offset, Grip value);
    void setGrip(int displacement, int index, Grip value);

    Reference readReference(Offset offset);
    Reference readReference(int offset);
    Reference getReference(int displacement, int index);

    void writeReference(Offset offset, Reference value);
    void writeReference(int offset, Reference value);
    void setReference(int displacement, int index, Reference value);

    /**
     * Atomic compare and swap.
     *
     * Compare a suspected value with the value in the location given by this accessor and an offset.
     * Iff they are same, place a new value into the location and return the suspected value.
     * Do all of the above in one atomic hardware transaction.
     *
     * Iff the suspected and actual value are different, return the value from memory.
     *
     * @param offset offset from accessor origin
     * @param suspectedValue if this value is in the accessor location, perform the swap
     * @param newValue the new value to put into the accessor location
     * @return either the suspected value or the unused new value
     */
    int compareAndSwapInt(Offset offset, int suspectedValue, int newValue);
    int compareAndSwapInt(int offset, int suspectedValue, int newValue);

    Word compareAndSwapWord(Offset offset, Word suspectedValue, Word newValue);
    Word compareAndSwapWord(int offset, Word suspectedValue, Word newValue);

    Reference compareAndSwapReference(Offset offset, Reference suspectedValue, Reference newValue);
    Reference compareAndSwapReference(int offset, Reference suspectedValue, Reference newValue);
}

