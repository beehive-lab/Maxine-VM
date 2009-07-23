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
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * This class implements a facade for the {@link Layout Layout} class. The
 * Layout class requires the use of {@link Reference Reference} objects, as opposed
 * to regular Java objects, and this class implements a layer around the Layout
 * API.
 *
 * @author Bernd Mathiske
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
        if (SpecialBuiltin.unsignedIntGreaterEqual(index, readArrayLength(array))) {
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
        if (value != null) {
            final ClassActor arrayClassActor = ObjectAccess.readClassActor(array);
            if (!arrayClassActor.componentClassActor().isNonNullInstance(value)) {
                Throw.arrayStoreException(array, value);
            }
        }
    }

    public static Pointer elementPointer(byte[] array, int index) {
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
        if (MaxineVM.isPrototyping()) {
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
        if (MaxineVM.isPrototyping()) {
            final byte[] byteArray = (byte[]) array;
            byteArray[index] = value;
            return;
        }
        Layout.setByte(Reference.fromJava(array), index, value);
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
        if (MaxineVM.isPrototyping()) {
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
        if (MaxineVM.isPrototyping()) {
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
        if (MaxineVM.isPrototyping()) {
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
        if (MaxineVM.isPrototyping()) {
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
        if (MaxineVM.isPrototyping()) {
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
        if (MaxineVM.isPrototyping()) {
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
        if (MaxineVM.isPrototyping()) {
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
        if (MaxineVM.isPrototyping()) {
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
        if (MaxineVM.isPrototyping()) {
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
        if (MaxineVM.isPrototyping()) {
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
        if (MaxineVM.isPrototyping()) {
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
        if (MaxineVM.isPrototyping()) {
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
        if (MaxineVM.isPrototyping()) {
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
        if (MaxineVM.isPrototyping()) {
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
        if (MaxineVM.isPrototyping()) {
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
        if (MaxineVM.isPrototyping()) {
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
        if (MaxineVM.isPrototyping()) {
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
        if (MaxineVM.isPrototyping()) {
            final Object[] objectArray = (Object[]) array;
            objectArray[index] = value;
            return;
        }
        Layout.setReference(Reference.fromJava(array), index, Reference.fromJava(value));
    }

}
