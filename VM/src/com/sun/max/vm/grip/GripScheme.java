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
/*VCSID=51672dd9-adea-4461-8650-6518ffb536d0*/

package com.sun.max.vm.grip;

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.reference.*;

/**
 * A "grip" is a runtime value of type 'java.lang.Object'.
 * It can be stored in fields and array elements of other objects.
 * The GC uses grips instead of references to avoid barrier code.
 * 
 * @see ReferenceScheme
 * 
 * @author Matthew Seidl
 * @author Bernd Mathiske
 */
public interface GripScheme extends VMScheme {

    DataModel dataModel();

    /**
     * @return whether any given grip to the identical object must always remain identical, even throughout GCs
     */
    boolean isConstant();

    Grip fromJava(Object object);

    Object toJava(Grip grip);

    Grip fromReference(Reference reference);

    Grip fromWord(Word word);

    Word toWord(Grip grip);

    /**
     * Create a new reference using the origin pointer to a newly created object.
     * 
     * @param origin the origin pointer of an object
     * @return a reference for the given object
     */
    Grip fromOrigin(Pointer origin);

    Pointer toOrigin(Grip grip);

    /**
     * Make use of a new origin.
     * Either change the old grip to refer to a new origin
     * or create a completely new grip that uses the new origin.
     * 
     * @param grip the old grip
     * @param origin the new origin pointer
     * @return the changed grip, potentially a new one
     */
    Grip updateGrip(Grip grip, Pointer origin);

    /**
     * @return the "zero" grip that represents 'null'.
     */
    Grip zero();

    boolean isZero(Grip grip);

    boolean isAllOnes(Grip grip);

    boolean equals(Grip grip1, Grip grip2);

    boolean isMarked(Grip grip);

    Grip marked(Grip grip);

    Grip unmarked(Grip grip);

    byte readByte(Grip grip, Offset offset);
    byte readByte(Grip grip, int offset);
    byte getByte(Grip grip, int displacement, int index);

    boolean readBoolean(Grip grip, Offset offset);
    boolean readBoolean(Grip grip, int offset);
    boolean getBoolean(Grip grip, int displacement, int index);

    short readShort(Grip grip, Offset offset);
    short readShort(Grip grip, int offset);
    short getShort(Grip grip, int displacement, int index);

    char readChar(Grip grip, Offset offset);
    char readChar(Grip grip, int offset);
    char getChar(Grip grip, int displacement, int index);

    int readInt(Grip grip, Offset offset);
    int readInt(Grip grip, int offset);
    int getInt(Grip grip, int displacement, int index);

    float readFloat(Grip grip, Offset offset);
    float readFloat(Grip grip, int offset);
    float getFloat(Grip grip, int displacement, int index);

    long readLong(Grip grip, Offset offset);
    long readLong(Grip grip, int offset);
    long getLong(Grip grip, int displacement, int index);

    double readDouble(Grip grip, Offset offset);
    double readDouble(Grip refgriperence, int offset);
    double getDouble(Grip grip, int displacement, int index);

    Word readWord(Grip grip, Offset offset);
    Word readWord(Grip grip, int offset);
    Word getWord(Grip grip, int displacement, int index);

    Grip readGrip(Grip grip, Offset offset);
    Grip readGrip(Grip grip, int offset);
    Grip getGrip(Grip grip, int displacement, int index);

    void writeByte(Grip grip, Offset offset, byte value);
    void writeByte(Grip grip, int offset, byte value);
    void setByte(Grip grip, int displacement, int index, byte value);

    void writeBoolean(Grip grip, Offset offset, boolean value);
    void writeBoolean(Grip grip, int offset, boolean value);
    void setBoolean(Grip grip, int displacement, int index, boolean value);

    void writeShort(Grip grip, Offset offset, short value);
    void writeShort(Grip grip, int offset, short value);
    void setShort(Grip grip, int displacement, int index, short value);

    void writeChar(Grip grip, Offset offset, char value);
    void writeChar(Grip grip, int offset, char value);
    void setChar(Grip grip, int displacement, int index, char value);

    void writeInt(Grip grip, Offset offset, int value);
    void writeInt(Grip grip, int offset, int value);
    void setInt(Grip grip, int displacement, int index, int value);

    void writeFloat(Grip grip, Offset offset, float value);
    void writeFloat(Grip grip, int offset, float value);
    void setFloat(Grip grip, int displacement, int index, float value);

    void writeLong(Grip grip, Offset offset, long value);
    void writeLong(Grip grip, int offset, long value);
    void setLong(Grip grip, int displacement, int index, long value);

    void writeDouble(Grip grip, Offset offset, double value);
    void writeDouble(Grip grip, int offset, double value);
    void setDouble(Grip grip, int displacement, int index, double value);

    void writeWord(Grip grip, Offset offset, Word value);
    void writeWord(Grip grip, int offset, Word value);
    void setWord(Grip grip, int displacement, int index, Word value);

    void writeGrip(Grip grip, Offset offset, Grip value);
    void writeGrip(Grip grip, int offset, Grip value);
    void setGrip(Grip grip, int displacement, int index, Grip value);

    int compareAndSwapInt(Grip grip, Offset offset, int suspectedValue, int newValue);
    int compareAndSwapInt(Grip grip, int offset, int suspectedValue, int newValue);

    Word compareAndSwapWord(Grip grip, Offset offset, Word suspectedValue, Word newValue);
    Word compareAndSwapWord(Grip grip, int offset, Word suspectedValue, Word newValue);

    Reference compareAndSwapReference(Grip grip, Offset offset, Reference suspectedValue, Reference newValue);
    Reference compareAndSwapReference(Grip grip, int offset, Reference suspectedValue, Reference newValue);

    /**
     * @param reference prototype value data for the reference before relocation by the starter program
     * @return the bytes to be written to the prototype, in cpu-specific ordering
     */
    byte[] createPrototypeGrip(Address address);

    /**
     * @return the bytes of a null reference to be written to the prototype, in cpu-specific ordering
     */
    byte[] createPrototypeNullGrip();
}
