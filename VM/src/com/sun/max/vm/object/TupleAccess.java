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
package com.sun.max.vm.object;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;

/**
 * This class implements a facade for tuple class (i.e. non-array and non-hybrid objects)
 * references that allows access to object fields with object parameters instead of {@code Reference}
 * parameters.
 *
 * @author Bernd Mathiske
 */
public final class TupleAccess {

    private TupleAccess() {
    }

    /**
     * Reads a byte from the specified object at the specified offset.
     * @param tuple the object to read the value from
     * @param offset the offset from the origin of the object
     * @return the value at the specified index from the object
     */
    @INLINE
    public static byte readByte(Object tuple, Offset offset) {
        return Reference.fromJava(tuple).readByte(offset);
    }

    /**
     * Reads a byte from the specified object at the specified offset.
     * @param tuple the object to read the value from
     * @param offset the offset from the origin of the object
     * @return the value at the specified index from the object
     */
    @INLINE
    public static byte readByte(Object tuple, int offset) {
        return Reference.fromJava(tuple).readByte(offset);
    }

    /**
     * Write a byte into the specified object at the specified offset.
     * @param tuple the object to write the value into
     * @param offset the offset from the origin of the object
     * @param value the value to write into the object
     */
    @INLINE
    public static void writeByte(Object tuple, Offset offset, byte value) {
        Reference.fromJava(tuple).writeByte(offset, value);
    }

    /**
     * Write a byte into the specified object at the specified offset.
     * @param tuple the object to write the value into
     * @param offset the offset from the origin of the object
     * @param value the value to write into the object
     */
    @INLINE
    public static void writeByte(Object tuple, int offset, byte value) {
        Reference.fromJava(tuple).writeByte(offset, value);
    }

    /**
     * Reads a boolean from the specified object at the specified offset.
     * @param tuple the object to read the value from
     * @param offset the offset from the origin of the object
     * @return the value at the specified index from the object
     */
    @INLINE
    public static boolean readBoolean(Object tuple, Offset offset) {
        return Reference.fromJava(tuple).readBoolean(offset);
    }

    /**
     * Reads a boolean from the specified object at the specified offset.
     * @param tuple the object to read the value from
     * @param offset the offset from the origin of the object
     * @return the value at the specified index from the object
     */
    @INLINE
    public static boolean readBoolean(Object tuple, int offset) {
        return Reference.fromJava(tuple).readBoolean(offset);
    }

    /**
     * Write a boolean into the specified object at the specified offset.
     * @param tuple the object to write the value into
     * @param offset the offset from the origin of the object
     * @param value the value to write into the object
     */
    @INLINE
    public static void writeBoolean(Object tuple, Offset offset, boolean value) {
        Reference.fromJava(tuple).writeBoolean(offset, value);
    }

    /**
     * Write a boolean into the specified object at the specified offset.
     * @param tuple the object to write the value into
     * @param offset the offset from the origin of the object
     * @param value the value to write into the object
     */
    @INLINE
    public static void writeBoolean(Object tuple, int offset, boolean value) {
        Reference.fromJava(tuple).writeBoolean(offset, value);
    }

    /**
     * Reads a short from the specified object at the specified offset.
     * @param tuple the object to read the value from
     * @param offset the offset from the origin of the object
     * @return the value at the specified index from the object
     */
    @INLINE
    public static short readShort(Object tuple, Offset offset) {
        return Reference.fromJava(tuple).readShort(offset);
    }

    /**
     * Reads a short from the specified object at the specified offset.
     * @param tuple the object to read the value from
     * @param offset the offset from the origin of the object
     * @return the value at the specified index from the object
     */
    @INLINE
    public static short readShort(Object tuple, int offset) {
        return Reference.fromJava(tuple).readShort(offset);
    }

    /**
     * Write a short into the specified object at the specified offset.
     * @param tuple the object to write the value into
     * @param offset the offset from the origin of the object
     * @param value the value to write into the object
     */
    @INLINE
    public static void writeShort(Object tuple, Offset offset, short value) {
        Reference.fromJava(tuple).writeShort(offset, value);
    }

    /**
     * Write a short into the specified object at the specified offset.
     * @param tuple the object to write the value into
     * @param offset the offset from the origin of the object
     * @param value the value to write into the object
     */
    @INLINE
    public static void writeShort(Object tuple, int offset, short value) {
        Reference.fromJava(tuple).writeShort(offset, value);
    }

    /**
     * Reads a char from the specified object at the specified offset.
     * @param tuple the object to read the value from
     * @param offset the offset from the origin of the object
     * @return the value at the specified index from the object
     */
    @INLINE
    public static char readChar(Object tuple, Offset offset) {
        return Reference.fromJava(tuple).readChar(offset);
    }

    /**
     * Reads a char from the specified object at the specified offset.
     * @param tuple the object to read the value from
     * @param offset the offset from the origin of the object
     * @return the value at the specified index from the object
     */
    @INLINE
    public static char readChar(Object tuple, int offset) {
        return Reference.fromJava(tuple).readChar(offset);
    }

    /**
     * Write a char into the specified object at the specified offset.
     * @param tuple the object to write the value into
     * @param offset the offset from the origin of the object
     * @param value the value to write into the object
     */
    @INLINE
    public static void writeChar(Object tuple, Offset offset, char value) {
        Reference.fromJava(tuple).writeChar(offset, value);
    }

    /**
     * Write a char into the specified object at the specified offset.
     * @param tuple the object to write the value into
     * @param offset the offset from the origin of the object
     * @param value the value to write into the object
     */
    @INLINE
    public static void writeChar(Object tuple, int offset, char value) {
        Reference.fromJava(tuple).writeChar(offset, value);
    }

    /**
     * Reads an int from the specified object at the specified offset.
     * @param tuple the object to read the value from
     * @param offset the offset from the origin of the object
     * @return the value at the specified index from the object
     */
    @INLINE
    public static int readInt(Object tuple, Offset offset) {
        return Reference.fromJava(tuple).readInt(offset);
    }

    /**
     * Reads an int from the specified object at the specified offset.
     * @param tuple the object to read the value from
     * @param offset the offset from the origin of the object
     * @return the value at the specified index from the object
     */
    @INLINE
    public static int readInt(Object tuple, int offset) {
        return Reference.fromJava(tuple).readInt(offset);
    }

    /**
     * Write an int into the specified object at the specified offset.
     * @param tuple the object to write the value into
     * @param offset the offset from the origin of the object
     * @param value the value to write into the object
     */
    @INLINE
    public static void writeInt(Object tuple, Offset offset, int value) {
        Reference.fromJava(tuple).writeInt(offset, value);
    }

    /**
     * Write an int into the specified object at the specified offset.
     *
     * @param tuple the object to write the value into
     * @param offset the offset from the origin of the object
     * @param value the value to write into the object
     */
    @INLINE
    public static void writeInt(Object tuple, int offset, int value) {
        Reference.fromJava(tuple).writeInt(offset, value);
    }

    /**
     * Reads a float from the specified object at the specified offset.
     * @param tuple the object to read the value from
     * @param offset the offset from the origin of the object
     * @return the value at the specified index from the object
     */
    @INLINE
    public static float readFloat(Object tuple, Offset offset) {
        return Reference.fromJava(tuple).readFloat(offset);
    }

    /**
     * Reads a float from the specified object at the specified offset.
     * @param tuple the object to read the value from
     * @param offset the offset from the origin of the object
     * @return the value at the specified index from the object
     */
    @INLINE
    public static float readFloat(Object tuple, int offset) {
        return Reference.fromJava(tuple).readFloat(offset);
    }

    /**
     * Write a float into the specified object at the specified offset.
     * @param tuple the object to write the value into
     * @param offset the offset from the origin of the object
     * @param value the value to write into the object
     */
    @INLINE
    public static void writeFloat(Object tuple, Offset offset, float value) {
        Reference.fromJava(tuple).writeFloat(offset, value);
    }

    /**
     * Write a float into the specified object at the specified offset.
     * @param tuple the object to write the value into
     * @param offset the offset from the origin of the object
     * @param value the value to write into the object
     */
    @INLINE
    public static void writeFloat(Object tuple, int offset, float value) {
        Reference.fromJava(tuple).writeFloat(offset, value);
    }

    /**
     * Reads a long from the specified object at the specified offset.
     * @param tuple the object to read the value from
     * @param offset the offset from the origin of the object
     * @return the value at the specified index from the object
     */
    @INLINE
    public static long readLong(Object tuple, Offset offset) {
        return Reference.fromJava(tuple).readLong(offset);
    }

    /**
     * Reads a long from the specified object at the specified offset.
     * @param tuple the object to read the value from
     * @param offset the offset from the origin of the object
     * @return the value at the specified index from the object
     */
    @INLINE
    public static long readLong(Object tuple, int offset) {
        return Reference.fromJava(tuple).readLong(offset);
    }

    /**
     * Write a long into the specified object at the specified offset.
     * @param tuple the object to write the value into
     * @param offset the offset from the origin of the object
     * @param value the value to write into the object
     */
    @INLINE
    public static void writeLong(Object tuple, Offset offset, long value) {
        Reference.fromJava(tuple).writeLong(offset, value);
    }

    /**
     * Write a long into the specified object at the specified offset.
     * @param tuple the object to write the value into
     * @param offset the offset from the origin of the object
     * @param value the value to write into the object
     */
    @INLINE
    public static void writeLong(Object tuple, int offset, long value) {
        Reference.fromJava(tuple).writeLong(offset, value);
    }

    /**
     * Reads a double from the specified object at the specified offset.
     * @param tuple the object to read the value from
     * @param offset the offset from the origin of the object
     * @return the value at the specified index from the object
     */
    @INLINE
    public static double readDouble(Object tuple, Offset offset) {
        return Reference.fromJava(tuple).readDouble(offset);
    }

    /**
     * Reads a double from the specified object at the specified offset.
     * @param tuple the object to read the value from
     * @param offset the offset from the origin of the object
     * @return the value at the specified index from the object
     */
    @INLINE
    public static double readDouble(Object tuple, int offset) {
        return Reference.fromJava(tuple).readDouble(offset);
    }

    /**
     * Write a double into the specified object at the specified offset.
     * @param tuple the object to write the value into
     * @param offset the offset from the origin of the object
     * @param value the value to write into the object
     */
    @INLINE
    public static void writeDouble(Object tuple, Offset offset, double value) {
        Reference.fromJava(tuple).writeDouble(offset, value);
    }

    /**
     * Write a double into the specified object at the specified offset.
     * @param tuple the object to write the value into
     * @param offset the offset from the origin of the object
     * @param value the value to write into the object
     */
    @INLINE
    public static void writeDouble(Object tuple, int offset, double value) {
        Reference.fromJava(tuple).writeDouble(offset, value);
    }

    /**
     * Reads a word from the specified object at the specified offset.
     * @param tuple the object to read the value from
     * @param offset the offset from the origin of the object
     * @return the value at the specified index from the object
     */
    @INLINE
    public static Word readWord(Object tuple, Offset offset) {
        return Reference.fromJava(tuple).readWord(offset);
    }

    /**
     * Reads a word from the specified object at the specified offset.
     * @param tuple the object to read the value from
     * @param offset the offset from the origin of the object
     * @return the value at the specified index from the object
     */
    @INLINE
    public static Word readWord(Object tuple, int offset) {
        return Reference.fromJava(tuple).readWord(offset);
    }

    /**
     * Write a word into the specified object at the specified offset.
     * @param tuple the object to write the value into
     * @param offset the offset from the origin of the object
     * @param value the value to write into the object
     */
    @INLINE
    public static void writeWord(Object tuple, Offset offset, Word value) {
        Reference.fromJava(tuple).writeWord(offset, value);
    }

    /**
     * Write a word into the specified object at the specified offset.
     * @param tuple the object to write the value into
     * @param offset the offset from the origin of the object
     * @param value the value to write into the object
     */
    @INLINE
    public static void writeWord(Object tuple, int offset, Word value) {
        Reference.fromJava(tuple).writeWord(offset, value);
    }

    /**
     * Reads a reference from the specified object at the specified offset.
     * @param tuple the object to read the value from
     * @param offset the offset from the origin of the object
     * @return the value at the specified index from the object
     */
    @INLINE
    public static Object readObject(Object tuple, Offset offset) {
        return Reference.fromJava(tuple).readReference(offset).toJava();
    }

    /**
     * Reads a reference from the specified object at the specified offset.
     * @param tuple the object to read the value from
     * @param offset the offset from the origin of the object
     * @return the value at the specified index from the object
     */
    @INLINE
    public static Object readObject(Object tuple, int offset) {
        return Reference.fromJava(tuple).readReference(offset).toJava();
    }

    /**
     * Writes a reference into the specified object at the specified offset.
     * @param tuple the object to write the value into
     * @param offset the offset from the origin of the object
     * @param value the value to write into the object
     */
    @INLINE
    public static void writeObject(Object tuple, Offset offset, Object value) {
        Reference.fromJava(tuple).writeReference(offset, Reference.fromJava(value));
    }

    /**
     * Writes a reference into the specified object at the specified offset.
     * @param tuple the object to write the value into
     * @param offset the offset from the origin of the object
     * @param value the value to write into the object
     */
    @INLINE
    public static void writeObject(Object tuple, int offset, Object value) {
        Reference.fromJava(tuple).writeReference(offset, Reference.fromJava(value));
    }

    /**
     * Writes a reference into the specified object at the specified offset.
     *
     * This non-inline version is required for the JIT templates. These templates
     * cannot contain reference literals while the compiled version of {@link #writeObject(Object, int, Object)}
     * might depending on any write barrier configured for the VM.
     *
     * @param tuple the object to write the value into
     * @param offset the offset from the origin of the object
     * @param value the value to write into the object
     */
    @NEVER_INLINE
    public static void noninlineWriteObject(Object tuple, int offset, Object value) {
        writeObject(tuple, offset, value);
    }
}
