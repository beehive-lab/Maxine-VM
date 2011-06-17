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

import static com.sun.max.vm.MaxineVM.*;

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * This class implements a facade for the {@link Layout Layout} class. The
 * Layout class requires the use of {@link Reference Reference} objects, as opposed
 * to regular Java objects, and this class implements a layer around the Layout
 * API.
 */
public final class ArrayAccess {

    private ArrayAccess() {
    }

    /**
     * Reads the length of the specified array.
     *
     * @param array the array object
     * @return the length of the array
     */
    @INLINE
    public static int readArrayLength(Object array) {
        if (isHosted()) {
            if (array.getClass().isArray()) {
                return Array.getLength(array);
            }
            if (array instanceof Hybrid) {
                final Hybrid hybrid = (Hybrid) array;
                return hybrid.length();
            }
            throw FatalError.unexpected("Cannot get array length of " + array.getClass().getName() + " instance");
        }
        return Layout.readArrayLength(Reference.fromJava(array));
    }

    /**
     * An inlined method to check an array index against the bounds of the array.
     *
     * @param array the array object
     * @param index the index into the array
     */
    @INLINE
    public static void checkIndex(Object array, int index) {
        // note that we must read the array length first (implicit null check has precedence over bounds check)
        if (Intrinsics.aboveEqual(index, readArrayLength(array))) {
            Throw.arrayIndexOutOfBoundsException(array, index);
        }
    }

    /**
     * Checks that the specified object reference can be assigned to the given object array.
     * This implements the same check as the {@code aastore} JVM bytecode.
     *
     * @param array the array object
     * @param value the object reference to write into the specified array
     */
    @INLINE
    public static void checkSetObject(Object array, Object value) {
        if (isHosted()) {
            final Class arrayClass = array.getClass();
            final Class componentType = arrayClass.getComponentType();
            if (value != null) {
                if (!componentType.isInstance(value)) {
                    Throw.arrayStoreException(array, value);
                }
            } else {
                if (Word.class.isAssignableFrom(componentType)) {
                    Throw.arrayStoreException(array, value);
                }
            }
        } else {
            if (value != null) {
                final ClassActor arrayClassActor = ObjectAccess.readClassActor(array);
                if (!arrayClassActor.componentClassActor().isNonNullInstance(value)) {
                    Throw.arrayStoreException(array, value);
                }
            }
        }
    }

    public static Pointer elementPointer(byte[] array, int index) {
        return Reference.fromJava(array).toOrigin().plus(Layout.byteArrayLayout().getElementOffsetFromOrigin(index));
    }

    @INLINE
    public static Pointer elementPointer(int[] array, int index) {
        return Reference.fromJava(array).toOrigin().plus(Layout.byteArrayLayout().getElementOffsetFromOrigin(index));
    }

    /**
     * Gets a byte from the specified array at the specified index.
     *
     * @param array the array object
     * @param index the index into the array
     * @return the value at the specified index in the specified array
     */
    @INLINE
    public static byte getByte(Object array, int index) {
        if (isHosted()) {
            if (array instanceof boolean[]) {
                final boolean[] booleanArray = (boolean[]) array;
                return booleanArray[index] ? (byte) 1 : (byte) 0;
            }
            assert array instanceof byte[];
            final byte[] byteArray = (byte[]) array;
            return byteArray[index];
        }
        return Layout.getByte(Reference.fromJava(array), index);
    }

    /**
     * Sets a byte in the specified array at the specified index.
     *
     * @param array the array object
     * @param index the index into the array
     * @param value the value to write into the array at the specified index
     */
    @INLINE
    public static void setByte(Object array, int index, byte value) {
        if (isHosted()) {
            if (array instanceof boolean[]) {
                final boolean[] booleanArray = (boolean[]) array;
                booleanArray[index] = value != 0;
            } else {
                final byte[] byteArray = (byte[]) array;
                byteArray[index] = value;
            }
        } else {
            Layout.setByte(Reference.fromJava(array), index, value);
        }
    }

    /**
     * Gets a boolean from the specified array at the specified index.
     *
     * @param array the array object
     * @param index the index into the array
     * @return the value at the specified index in the specified array
     */
    @INLINE
    public static boolean getBoolean(Object array, int index) {
        if (isHosted()) {
            final boolean[] booleanArray = (boolean[]) array;
            return booleanArray[index];
        }
        return Layout.getBoolean(Reference.fromJava(array), index);
    }

    /**
     * Sets a boolean in the specified array at the specified index.
     *
     * @param array the array object
     * @param index the index into the array
     * @param value the value to write into the array at the specified index
     */
    @INLINE
    public static void setBoolean(Object array, int index, boolean value) {
        if (isHosted()) {
            final boolean[] booleanArray = (boolean[]) array;
            booleanArray[index] = value;
            return;
        }
        Layout.setBoolean(Reference.fromJava(array), index, value);
    }

    /**
     * Gets a short from the specified array at the specified index.
     *
     * @param array the array object
     * @param index the index into the array
     * @return the value at the specified index in the specified array
     */
    @INLINE
    public static short getShort(Object array, int index) {
        if (isHosted()) {
            final short[] shortArray = (short[]) array;
            return shortArray[index];
        }
        return Layout.getShort(Reference.fromJava(array), index);
    }

    /**
     * Sets a short in the specified array at the specified index.
     *
     * @param array the array object
     * @param index the index into the array
     * @param value the value to write into the array at the specified index
     */
    @INLINE
    public static void setShort(Object array, int index, short value) {
        if (isHosted()) {
            final short[] shortArray = (short[]) array;
            shortArray[index] = value;
            return;
        }
        Layout.setShort(Reference.fromJava(array), index, value);
    }

    /**
     * Gets a char from the specified array at the specified index.
     *
     * @param array the array object
     * @param index the index into the array
     * @return the value at the specified index in the specified array
     */
    @INLINE
    public static char getChar(Object array, int index) {
        if (isHosted()) {
            final char[] charArray = (char[]) array;
            return charArray[index];
        }
        return Layout.getChar(Reference.fromJava(array), index);
    }

    /**
     * Sets a char in the specified array at the specified index.
     *
     * @param array the array object
     * @param index the index into the array
     * @param value the value to write into the array at the specified index
     */
    @INLINE
    public static void setChar(Object array, int index, char value) {
        if (isHosted()) {
            final char[] charArray = (char[]) array;
            charArray[index] = value;
            return;
        }
        Layout.setChar(Reference.fromJava(array), index, value);
    }

    /**
     * Gets an int from the specified array at the specified index.
     *
     * @param array the array object
     * @param index the index into the array
     * @return the value at the specified index in the specified array
     */
    @INLINE
    public static int getInt(Object array, int index) {
        if (isHosted()) {
            final int[] intArray = (int[]) array;
            return intArray[index];
        }
        return Layout.getInt(Reference.fromJava(array), index);
    }

    /**
     * Sets an int in the specified array at the specified index.
     *
     * @param array the array object
     * @param index the index into the array
     * @param value the value to write into the array at the specified index
     */
    @INLINE
    public static void setInt(Object array, int index, int value) {
        if (isHosted()) {
            final int[] intArray = (int[]) array;
            intArray[index] = value;
            return;
        }
        Layout.setInt(Reference.fromJava(array), index, value);
    }

    /**
     * Gets a float from the specified array at the specified index.
     *
     * @param array the array object
     * @param index the index into the array
     * @return the value at the specified index in the specified array
     */
    @INLINE
    public static float getFloat(Object array, int index) {
        if (isHosted()) {
            final float[] floatArray = (float[]) array;
            return floatArray[index];
        }
        return Layout.getFloat(Reference.fromJava(array), index);
    }

    /**
     * Sets a float in the specified array at the specified index.
     *
     * @param array the array object
     * @param index the index into the array
     * @param value the value to write into the array at the specified index
     */
    @INLINE
    public static void setFloat(Object array, int index, float value) {
        if (isHosted()) {
            final float[] floatArray = (float[]) array;
            floatArray[index] = value;
            return;
        }
        Layout.setFloat(Reference.fromJava(array), index, value);
    }

    /**
     * Gets a long from the specified array at the specified index.
     *
     * @param array the array object
     * @param index the index into the array
     * @return the value at the specified index in the specified array
     */
    @INLINE
    public static long getLong(Object array, int index) {
        if (isHosted()) {
            final long[] longArray = (long[]) array;
            return longArray[index];
        }
        return Layout.getLong(Reference.fromJava(array), index);
    }

    /**
     * Sets a long in the specified array at the specified index.
     *
     * @param array the array object
     * @param index the index into the array
     * @param value the value to write into the array at the specified index
     */
    @INLINE
    public static void setLong(Object array, int index, long value) {
        if (isHosted()) {
            final long[] longArray = (long[]) array;
            longArray[index] = value;
            return;
        }
        Layout.setLong(Reference.fromJava(array), index, value);
    }

    /**
     * Gets a double from the specified array at the specified index.
     *
     * @param array the array object
     * @param index the index into the array
     * @return the value at the specified index in the specified array
     */
    @INLINE
    public static double getDouble(Object array, int index) {
        if (isHosted()) {
            final double[] doubleArray = (double[]) array;
            return doubleArray[index];
        }
        return Layout.getDouble(Reference.fromJava(array), index);
    }

    /**
     * Sets a double in the specified array at the specified index.
     *
     * @param array the array object
     * @param index the index into the array
     * @param value the value to write into the array at the specified index
     */
    @INLINE
    public static void setDouble(Object array, int index, double value) {
        if (isHosted()) {
            final double[] doubleArray = (double[]) array;
            doubleArray[index] = value;
            return;
        }
        Layout.setDouble(Reference.fromJava(array), index, value);
    }

    /**
     * Gets a word from the specified array at the specified index.
     *
     * @param array the array object
     * @param index the index into the array
     * @return the value at the specified index in the specified array
     */
    @INLINE
    public static Word getWord(Object array, int index) {
        if (isHosted()) {
            final Word[] wordArray = (Word[]) array;
            return WordArray.get(wordArray, index);
        }
        return Layout.getWord(Reference.fromJava(array), index);
    }

    /**
     * Sets a word in the specified array at the specified index.
     *
     * @param array the array object
     * @param index the index into the array
     * @param value the value to write into the array at the specified index
     */
    @INLINE
    public static void setWord(Object array, int index, Word value) {
        if (isHosted()) {
            final Word[] wordArray = (Word[]) array;
            WordArray.set(wordArray, index, value);
            return;
        }
        Layout.setWord(Reference.fromJava(array), index, value);
    }

    /**
     * Gets a reference from the specified array at the specified index.
     *
     * @param array the array object
     * @param index the index into the array
     * @return the value at the specified index in the specified array
     */
    @INLINE
    public static Object getObject(Object array, int index) {
        if (isHosted()) {
            final Object[] objectArray = (Object[]) array;
            return objectArray[index];
        }
        return Layout.getReference(Reference.fromJava(array), index).toJava();
    }

    /**
     * Sets a reference in the specified array at the specified index.
     *
     * @param array the array object
     * @param index the index into the array
     * @param value the value to write into the array at the specified index
     */
    @INLINE
    public static void setObject(Object array, int index, Object value) {
        if (isHosted()) {
            final Object[] objectArray = (Object[]) array;
            objectArray[index] = value;
            return;
        }
        Layout.setReference(Reference.fromJava(array), index, Reference.fromJava(value));
    }

}
