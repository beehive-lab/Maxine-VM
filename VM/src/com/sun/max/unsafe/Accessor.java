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
 * @author Paul Caprioli
 */
public interface Accessor {

    /**
     * Tests whether this is zero.
     * @return true if all bits are zero.
     */
    boolean isZero();

    /**
     * Reads a byte at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @return the byte value
     */
    byte readByte(Offset offset);

    /**
     * Reads a byte at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @return the byte value
     */
    byte readByte(int offset);

    /**
     * Gets a byte at index plus displacement from this location.
     * @param displacement signed displacement in bytes
     * @param index signed index measured in bytes
     * @return the byte value
     */
    byte getByte(int displacement, int index);

    /**
     * Writes a byte at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @param value the data to be written
     */
    void writeByte(Offset offset, byte value);

    /**
     * Writes a byte at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @param value the data to be written
     */
    void writeByte(int offset, byte value);

    /**
     * Writes a byte at index plus displacement from this location.
     * @param displacement signed displacement in bytes
     * @param index signed index measured in bytes
     * @param value the data to be written
     */
    void setByte(int displacement, int index, byte value);

    /**
     * Reads a boolean at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @return the boolean value
     */
    boolean readBoolean(Offset offset);

    /**
     * Reads a boolean at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @return the boolean value
     */
    boolean readBoolean(int offset);

    /**
     * Gets a boolean at index plus displacement from this location.
     * @param displacement signed displacement in bytes
     * @param index signed index measured in bytes
     * @return the boolean value
     */
    boolean getBoolean(int displacement, int index);

    /**
     * Writes a boolean at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @param value the data to be written
     */
    void writeBoolean(Offset offset, boolean value);

    /**
     * Writes a boolean at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @param value the data to be written
     */
    void writeBoolean(int offset, boolean value);

    /**
     * Writes a boolean at index plus displacement from this location.
     * @param displacement signed displacement in bytes
     * @param index signed index measured in bytes
     * @param value the data to be written
     */
    void setBoolean(int displacement, int index, boolean value);

    /**
     * Reads a short at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @return the short value
     */
    short readShort(Offset offset);

    /**
     * Reads a short at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @return the short value
     */
    short readShort(int offset);

    /**
     * Gets a short at the scaled index plus displacement from this location.
     * @param displacement signed displacement in bytes
     * @param index signed index measured in shorts
     * @return the short value
     */
    short getShort(int displacement, int index);

    /**
     * Writes a short at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @param value the data to be written
     */
    void writeShort(Offset offset, short value);

    /**
     * Writes a short at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @param value the data to be written
     */
    void writeShort(int offset, short value);

    /**
     * Writes a short at index plus displacement from this location.
     * @param displacement signed displacement in bytes
     * @param index signed index measured in shorts
     * @param value the data to be written
     */
    void setShort(int displacement, int index, short value);

    /**
     * Reads a character at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @return the character value
     */
    char readChar(Offset offset);

    /**
     * Reads a character at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @return the character value
     */
    char readChar(int offset);

    /**
     * Gets a character at the scaled index plus displacement from this location.
     * @param displacement signed displacement in bytes
     * @param index signed index measured in chars
     * @return the character value
     */
    char getChar(int displacement, int index);

    /**
     * Writes a character at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @param value the data to be written
     */
    void writeChar(Offset offset, char value);

    /**
     * Writes a character at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @param value the data to be written
     */
    void writeChar(int offset, char value);

    /**
     * Writes a character at index plus displacement from this location.
     * @param displacement signed displacement in bytes
     * @param index signed index measured in characters
     * @param value the data to be written
     */
    void setChar(int displacement, int index, char value);

    /**
     * Reads an integer at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @return the integer value
     */
    int readInt(Offset offset);

    /**
     * Reads an integer at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @return the integer value
     */
    int readInt(int offset);

    /**
     * Gets an integer at the scaled index plus displacement from this location.
     * @param displacement signed displacement in bytes
     * @param index signed index measured in ints
     * @return the integer value
     */
    int getInt(int displacement, int index);

    /**
     * Writes an integer at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @param value the data to be written
     */
    void writeInt(Offset offset, int value);
    /**
     * Writes an integer at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @param value the data to be written
     */
    void writeInt(int offset, int value);

    /**
     * Writes an integer at index plus displacement from this location.
     * @param displacement signed displacement in bytes
     * @param index signed index measured in ints
     * @param value the data to be written
     */
    void setInt(int displacement, int index, int value);

    /**
     * Reads a float at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @return the float value
     */
    float readFloat(Offset offset);

    /**
     * Reads a float at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @return the float value
     */
    float readFloat(int offset);

    /**
     * Gets a float at the scaled index plus displacement from this location.
     * @param displacement signed displacement in bytes
     * @param index signed index measured in floats
     * @return the float value
     */
    float getFloat(int displacement, int index);

    /**
     * Writes a float at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @param value the data to be written
     */
    void writeFloat(Offset offset, float value);

    /**
     * Writes a float at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @param value the data to be written
     */
    void writeFloat(int offset, float value);

    /**
     * Writes a float at index plus displacement from this location.
     * @param displacement signed displacement in bytes
     * @param index signed index measured in floats
     * @param value the data to be written
     */
    void setFloat(int displacement, int index, float value);

    /**
     * Reads a long at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @return the long value
     */
    long readLong(Offset offset);

    /**
     * Reads a long at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @return the long value
     */
    long readLong(int offset);

    /**
     * Gets a long at the scaled index plus displacement from this location.
     * @param displacement signed displacement in bytes
     * @param index signed index measured in longs
     * @return the long value
     */
    long getLong(int displacement, int index);

    /**
     * Writes a long at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @param value the data to be written
     */
    void writeLong(Offset offset, long value);

    /**
     * Writes a long at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @param value the data to be written
     */
    void writeLong(int offset, long value);

    /**
     * Writes a long at index plus displacement from this location.
     * @param displacement signed displacement in bytes
     * @param index signed index measured in longs
     * @param value the data to be written
     */
    void setLong(int displacement, int index, long value);

    /**
     * Reads a double at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @return the double value
     */
    double readDouble(Offset offset);

    /**
     * Reads a double at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @return the double value
     */
    double readDouble(int offset);

    /**
     * Gets a double at the scaled index plus displacement from this location.
     * @param displacement signed displacement in bytes
     * @param index signed index measured in doubles
     * @return the double value
     */
    double getDouble(int displacement, int index);

    /**
     * Writes a double at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @param value the data to be written
     */
    void writeDouble(Offset offset, double value);

    /**
     * Writes a double at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @param value the data to be written
     */
    void writeDouble(int offset, double value);

    /**
     * Writes a double at index plus displacement from this location.
     * @param displacement signed displacement in bytes
     * @param index signed index measured in doubles
     * @param value the data to be written
     */
    void setDouble(int displacement, int index, double value);

    /**
     * Reads a Word at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @return the Word value
     */
    Word readWord(Offset offset);

    /**
     * Reads a Word at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @return the Word value
     */
    Word readWord(int offset);

    /**
     * Gets a Word at the scaled index plus displacement from this location.
     * @param displacement signed displacement in bytes
     * @param index signed index measured in Words
     * @return the Word value
     */
    Word getWord(int displacement, int index);

    /**
     * Writes a Word at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @param value the data to be written
     */
    void writeWord(Offset offset, Word value);

    /**
     * Writes a Word at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @param value the data to be written
     */
    void writeWord(int offset, Word value);

    /**
     * Writes a Word at index plus displacement from this location.
     * @param displacement signed displacement in bytes
     * @param index signed index measured in Words
     * @param value the data to be written
     */
    void setWord(int displacement, int index, Word value);

    /**
     * Reads a Reference at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @return the Reference value
     */
    Reference readReference(Offset offset);

    /**
     * Reads a Reference at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @return the Reference value
     */
    Reference readReference(int offset);

    /**
     * Gets a Reference at the scaled index plus displacement from this location.
     * @param displacement signed displacement in bytes
     * @param index signed index measured in References
     * @return the Reference value
     */
    Reference getReference(int displacement, int index);

    /**
     * Writes a Reference at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @param value the data to be written
     */
    void writeReference(Offset offset, Reference value);

    /**
     * Writes a Reference at an offset from this location.
     * @param offset the signed offset in bytes from this
     * @param value the data to be written
     */
    void writeReference(int offset, Reference value);

    /**
     * Writes a Reference at index plus displacement from this location.
     * @param displacement signed displacement in bytes
     * @param index signed index measured in References
     * @param value the data to be written
     */
    void setReference(int displacement, int index, Reference value);

    /**
     * Atomic compare and swap.
     *
     * Compares an expected value with the actual value in a location denoted by this accessor and a given offset.
     * Iff they are same, {@code newValue} is placed into the location and the {@code expectedValue} is returned.
     * Otherwise, the actual value is returned.
     * All of the above is performed in one atomic hardware transaction.
     *
     * @param offset offset from accessor origin
     * @param expectedValue if this value is currently in the accessor location, perform the swap
     * @param newValue the new value to put into the accessor location
     * @return either {@code expectedValue} or the actual value
     */
    int compareAndSwapInt(Offset offset, int expectedValue, int newValue);

    /**
     * @see #compareAndSwapInt(Offset, int, int)
     */
    int compareAndSwapInt(int offset, int expectedValue, int newValue);

    /**
     * @see #compareAndSwapInt(Offset, int, int)
     */
    Word compareAndSwapWord(Offset offset, Word expectedValue, Word newValue);

    /**
     * @see #compareAndSwapInt(Offset, int, int)
     */
    Word compareAndSwapWord(int offset, Word expectedValue, Word newValue);

    /**
     * @see #compareAndSwapInt(Offset, int, int)
     */
    Reference compareAndSwapReference(Offset offset, Reference expectedValue, Reference newValue);

    /**
     * @see #compareAndSwapInt(Offset, int, int)
     */
    Reference compareAndSwapReference(int offset, Reference expectedValue, Reference newValue);

    /**
     * Copies elements from this array into a given array.
     *
     * @param displacement signed displacement in bytes
     * @param srcIndex the index in this array from which to copy
     * @param dst the array into which the elements will be read
     * @param dstIndex the index in {@code dst} to which to copy
     * @param length the number of elements to copy
     */
    void copyElements(int displacement, int srcIndex, Object dst, int dstIndex, int length);
}

