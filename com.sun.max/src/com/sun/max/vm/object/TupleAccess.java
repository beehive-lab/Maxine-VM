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
package com.sun.max.vm.object;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;

/**
 * This class implements a facade for tuple class (i.e. non-array and non-hybrid objects)
 * references that allows access to object fields with object parameters instead of {@code Reference}
 * parameters.
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
     * @param tuple the object to write the value into
     * @param offset the offset from the origin of the object
     * @param value the value to write into the object
     */
    @NEVER_INLINE
    public static void noninlineWriteObject(Object tuple, int offset, Object value) {
        writeObject(tuple, offset, value);
    }
}
