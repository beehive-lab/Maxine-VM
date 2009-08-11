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
package com.sun.max.vm.reference;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.grip.*;

/**
 * Reference-based object access methods for mutator use.
 *
 * A "reference" is a runtime value of type 'java.lang.Object'.
 * It can be stored in fields and array elements of other objects.
 * The mutator refers to objects and parts thereof by using references.
 *
 * A reference is almost the same as a "grip".
 * The difference is that the former's access operations may incur barriers.
 *
 * @see Grip
 *
 * @author Bernd Mathiske
 */
public interface ReferenceScheme extends VMScheme {

    Reference fromGrip(Grip grip);

    Reference fromJava(Object object);

    Object toJava(Reference reference);

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
     * @param suspectedValue the value that must currently exist in the memory location for the update to occur
     * @param newValue the value to which the memory is updated if its current value is {@code suspectedValue}
     * @return the value of the memory location before this call; if it is equal to {@code suspectedValue}, then the
     *         update occurred, otherwise the update did not occur (assuming {@code suspectedValue != newValue})
     */
    int compareAndSwapInt(Reference reference, Offset offset, int suspectedValue, int newValue);

    /**
     * @see #compareAndSwapInt(Reference, Offset, int, int)
     */
    int compareAndSwapInt(Reference reference, int offset, int suspectedValue, int newValue);

    /**
     * @see #compareAndSwapInt(Reference, Offset, int, int)
     */
    Word compareAndSwapWord(Reference reference, Offset offset, Word suspectedValue, Word newValue);

    /**
     * @see #compareAndSwapInt(Reference, Offset, int, int)
     */
    Word compareAndSwapWord(Reference reference, int offset, Word suspectedValue, Word newValue);

    Reference compareAndSwapReference(Reference reference, Offset offset, Reference suspectedValue, Reference newValue);
    Reference compareAndSwapReference(Reference reference, int offset, Reference suspectedValue, Reference newValue);
}
