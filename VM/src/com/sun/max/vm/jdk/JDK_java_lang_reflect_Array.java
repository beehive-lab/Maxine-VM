/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.jdk;

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.compiler.snippet.CreateArraySnippet.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Native method implementations for {@link java.lang.reflect.Array java.lang.reflect.Array},
 * which provides reflection support for accessing array elements and creating new arrays.
 * Substitutions for this class use Maxine's internal reflection mechanisms instead of the
 * JDK's reflection mechanisms.
 *
 * @author Bernd Mathiske
 */
@METHOD_SUBSTITUTIONS(Array.class)
final class JDK_java_lang_reflect_Array {

    private JDK_java_lang_reflect_Array() {
    }

    /**
     * Checks that the specified object is an array and throws {@code IllegalArgumentException} if not.
     * @param array the object to check
     * @throws IllegalArgumentException if the specified object is not an array
     */
    private static void checkArray(Object array) throws IllegalArgumentException {
        if (array == null) {
            throw new NullPointerException();
        }
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Checks that the argument is an array and that the index falls within the bounds.
     * @param array the object to check
     * @param index the index to check
     */
    private static void checkIndex(Object array, int index) {
        checkArray(array);
        ArrayAccess.checkIndex(array, index);
    }

    /**
     * Gets the length of the specified array.
     * @see java.lang.reflect.Array#getLength(Object)
     * @param array the array object for which to get the length
     * @return the length of the array
     * @throws IllegalArgumentException if the specified object is not an array
     */
    @SUBSTITUTE
    public static int getLength(Object array) throws IllegalArgumentException {
        checkArray(array);
        return ArrayAccess.readArrayLength(array);
    }

    /**
     * Get the value of an array element at the specified index.
     * @see java.lang.reflect.Array#get(Object, int)
     * @param array the array object
     * @param index the index into the array
     * @return a boxed representation of the value of the array element
     * @throws IllegalArgumentException if the object is not an array
     * @throws ArrayIndexOutOfBoundsException if the specified index is out of bounds
     */
    @SUBSTITUTE
    public static Object get(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkIndex(array, index);
        final Kind elementKind = ObjectAccess.readClassActor(array).componentClassActor().kind;
        return elementKind.getValue(array, index).asBoxedJavaValue();
    }

    /**
     * Get the boolean value of an array element at the specified index.
     * @see java.lang.reflect.Array#getBoolean(Object, int)
     * @param array the array object
     * @param index the index into the array
     * @return the boolean value of the element
     * @throws IllegalArgumentException if the object is not an array
     * @throws ArrayIndexOutOfBoundsException if the specified index is out of bounds
     */
    @SUBSTITUTE
    public static boolean getBoolean(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkIndex(array, index);
        final Kind elementKind = ObjectAccess.readClassActor(array).componentClassActor().kind;
        final Value value = elementKind.getValue(array, index);
        return value.toBoolean();
    }

    /**
     * Get the byte value of an array element at the specified index.
     * @see java.lang.reflect.Array#getByte(Object, int)
     * @param array the array object
     * @param index the index into the array
     * @return the byte value of the element
     * @throws IllegalArgumentException if the object is not an array
     * @throws ArrayIndexOutOfBoundsException if the specified index is out of bounds
     */
    @SUBSTITUTE
    public static byte getByte(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkIndex(array, index);
        final Kind elementKind = ObjectAccess.readClassActor(array).componentClassActor().kind;
        final Value value = elementKind.getValue(array, index);
        return value.toByte();
    }

    /**
     * Get the char value of an array element at the specified index.
     * @see java.lang.reflect.Array#getChar(Object, int)
     * @param array the array object
     * @param index the index into the array
     * @return the char value of the element
     * @throws IllegalArgumentException if the object is not an array
     * @throws ArrayIndexOutOfBoundsException if the specified index is out of bounds
     */
    @SUBSTITUTE
    public static char getChar(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkIndex(array, index);
        final Kind elementKind = ObjectAccess.readClassActor(array).componentClassActor().kind;
        final Value value = elementKind.getValue(array, index);
        return value.toChar();
    }

    /**
     * Get the short value of an array element at the specified index.
     * @see java.lang.reflect.Array#getShort(Object, int)
     * @param array the array object
     * @param index the index into the array
     * @return the boolean value of the element
     * @throws IllegalArgumentException if the object is not an array
     * @throws ArrayIndexOutOfBoundsException if the specified index is out of bounds
     */
    @SUBSTITUTE
    public static short getShort(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkIndex(array, index);
        final Kind elementKind = ObjectAccess.readClassActor(array).componentClassActor().kind;
        final Value value = elementKind.getValue(array, index);
        return value.toShort();
    }

    /**
     * Get the int value of an array element at the specified index.
     * @see java.lang.reflect.Array#getInt(Object, int)
     * @param array the array object
     * @param index the index into the array
     * @return the int value of the element
     * @throws IllegalArgumentException if the object is not an array
     * @throws ArrayIndexOutOfBoundsException if the specified index is out of bounds
     */
    @SUBSTITUTE
    public static int getInt(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkIndex(array, index);
        final Kind elementKind = ObjectAccess.readClassActor(array).componentClassActor().kind;
        final Value value = elementKind.getValue(array, index);
        return value.toInt();
    }

    /**
     * Get the long value of an array element at the specified index.
     * @see java.lang.reflect.Array#getLong(Object, int)
     * @param array the array object
     * @param index the index into the array
     * @return the long value of the element
     * @throws IllegalArgumentException if the object is not an array
     * @throws ArrayIndexOutOfBoundsException if the specified index is out of bounds
     */
    @SUBSTITUTE
    public static long getLong(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkIndex(array, index);
        final Kind elementKind = ObjectAccess.readClassActor(array).componentClassActor().kind;
        final Value value = elementKind.getValue(array, index);
        return value.toLong();
    }

    /**
     * Get the float value of an array element at the specified index.
     * @see java.lang.reflect.Array#getFloat(Object, int)
     * @param array the array object
     * @param index the index into the array
     * @return the float value of the element
     * @throws IllegalArgumentException if the object is not an array
     * @throws ArrayIndexOutOfBoundsException if the specified index is out of bounds
     */
    @SUBSTITUTE
    public static float getFloat(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkIndex(array, index);
        final Kind elementKind = ObjectAccess.readClassActor(array).componentClassActor().kind;
        final Value value = elementKind.getValue(array, index);
        return value.toFloat();
    }

    /**
     * Get the double value of an array element at the specified index.
     * @see java.lang.reflect.Array#getDouble(Object, int)
     * @param array the array object
     * @param index the index into the array
     * @return the double value of the element
     * @throws IllegalArgumentException if the object is not an array
     * @throws ArrayIndexOutOfBoundsException if the specified index is out of bounds
     */
    @SUBSTITUTE
    public static double getDouble(Object array, int index) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkIndex(array, index);
        final Kind elementKind = ObjectAccess.readClassActor(array).componentClassActor().kind;
        final Value value = elementKind.getValue(array, index);
        return value.toDouble();
    }

    /**
     * Sets the element of the array at the specified index to the specified value.
     * @see java.lang.reflect.Array#set(Object, int, Object)
     * @param array the array object
     * @param index the index into the array
     * @param value the value to assign to the element of the array
     * @throws IllegalArgumentException if the object is not an array
     * @throws ArrayIndexOutOfBoundsException if the specified index is out of bounds
     */
    @SUBSTITUTE
    public static void set(Object array, int index, Object value) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkIndex(array, index);
        final Kind elementKind = ObjectAccess.readClassActor(array).componentClassActor().kind;
        if (elementKind.isReference) {
            ArrayAccess.setObject(array, index, value);
        } else {
            elementKind.setErasedValue(array, index, elementKind.convert(Value.fromBoxedJavaValue(value)));
        }
    }

    /**
     * Set the boolean value of an array element at the specified index.
     * @see java.lang.reflect.Array#setBoolean(Object, int, boolean)
     * @param array the array object
     * @param index the index into the array
     * @param value the value to assign
     * @throws IllegalArgumentException if the object is not an array
     * @throws ArrayIndexOutOfBoundsException if the specified index is out of bounds
     */
    @SUBSTITUTE
    public static void setBoolean(Object array, int index, boolean value) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkIndex(array, index);
        final Kind elementKind = ObjectAccess.readClassActor(array).componentClassActor().kind;
        elementKind.setErasedValue(array, index, elementKind.convert(BooleanValue.from(value)));
    }

    /**
     * Set the byte value of an array element at the specified index.
     * @see java.lang.reflect.Array#setByte(Object, int, byte)
     * @param array the array object
     * @param index the index into the array
     * @param value the value to assign
     * @throws IllegalArgumentException if the object is not an array
     * @throws ArrayIndexOutOfBoundsException if the specified index is out of bounds
     */
    @SUBSTITUTE
    public static void setByte(Object array, int index, byte value) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkIndex(array, index);
        final Kind elementKind = ObjectAccess.readClassActor(array).componentClassActor().kind;
        elementKind.setErasedValue(array, index, elementKind.convert(ByteValue.from(value)));
    }

    /**
     * Set the char value of an array element at the specified index.
     * @see java.lang.reflect.Array#setChar(Object, int, char)
     * @param array the array object
     * @param index the index into the array
     * @param value the value to assign
     * @throws IllegalArgumentException if the object is not an array
     * @throws ArrayIndexOutOfBoundsException if the specified index is out of bounds
     */
    @SUBSTITUTE
    public static void setChar(Object array, int index, char value) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkIndex(array, index);
        final Kind elementKind = ObjectAccess.readClassActor(array).componentClassActor().kind;
        elementKind.setErasedValue(array, index, elementKind.convert(CharValue.from(value)));
    }

    /**
     * Set the short value of an array element at the specified index.
     * @see java.lang.reflect.Array#setShort(Object, int, short)
     * @param array the array object
     * @param index the index into the array
     * @param value the value to assign
     * @throws IllegalArgumentException if the object is not an array
     * @throws ArrayIndexOutOfBoundsException if the specified index is out of bounds
     */
    @SUBSTITUTE
    public static void setShort(Object array, int index, short value) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkIndex(array, index);
        final Kind elementKind = ObjectAccess.readClassActor(array).componentClassActor().kind;
        elementKind.setErasedValue(array, index, elementKind.convert(ShortValue.from(value)));
    }

    /**
     * Set the int value of an array element at the specified index.
     * @see java.lang.reflect.Array#setInt(Object, int, int)
     * @param array the array object
     * @param index the index into the array
     * @param value the value to assign
     * @throws IllegalArgumentException if the object is not an array
     * @throws ArrayIndexOutOfBoundsException if the specified index is out of bounds
     */
    @SUBSTITUTE
    public static void setInt(Object array, int index, int value) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkIndex(array, index);
        final Kind elementKind = ObjectAccess.readClassActor(array).componentClassActor().kind;
        elementKind.setErasedValue(array, index, elementKind.convert(IntValue.from(value)));
    }

    /**
     * Set the long value of an array element at the specified index.
     * @see java.lang.reflect.Array#setLong(Object, int, long)
     * @param array the array object
     * @param index the index into the array
     * @param value the value to assign
     * @throws IllegalArgumentException if the object is not an array
     * @throws ArrayIndexOutOfBoundsException if the specified index is out of bounds
     */
    @SUBSTITUTE
    public static void setLong(Object array, int index, long value) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkIndex(array, index);
        final Kind elementKind = ObjectAccess.readClassActor(array).componentClassActor().kind;
        elementKind.setErasedValue(array, index, elementKind.convert(LongValue.from(value)));
    }

    /**
     * Set the float value of an array element at the specified index.
     * @see java.lang.reflect.Array#setFloat(Object, int, float)
     * @param array the array object
     * @param index the index into the array
     * @param value the value to assign
     * @throws IllegalArgumentException if the object is not an array
     * @throws ArrayIndexOutOfBoundsException if the specified index is out of bounds
     */
    @SUBSTITUTE
    public static void setFloat(Object array, int index, float value) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkIndex(array, index);
        final Kind elementKind = ObjectAccess.readClassActor(array).componentClassActor().kind;
        elementKind.setErasedValue(array, index, elementKind.convert(FloatValue.from(value)));
    }

    /**
     * Set the double value of an array element at the specified index.
     * @see java.lang.reflect.Array#setDouble(Object, int, double)
     * @param array the array object
     * @param index the index into the array
     * @param value the value to assign
     * @throws IllegalArgumentException if the object is not an array
     * @throws ArrayIndexOutOfBoundsException if the specified index is out of bounds
     */
    @SUBSTITUTE
    public static void setDouble(Object array, int index, double value) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        checkIndex(array, index);
        final Kind elementKind = ObjectAccess.readClassActor(array).componentClassActor().kind;
        elementKind.setErasedValue(array, index, elementKind.convert(DoubleValue.from(value)));
    }

    /**
     * Allocates a new array with the specified component class and the specified length.
     * @see java.lang.reflect.Array#newArray(Class, int)
     * @param javaComponentClass the component class
     * @param length the length of the new array
     * @return a new array with the specified component class and length
     * @throws IllegalArgumentException if the component type is {@code void}
     * @throws NegativeArraySizeException if the supplied length was negative
     */
    @SUBSTITUTE
    private static Object newArray(Class javaComponentClass, int length) throws NegativeArraySizeException {
        if (javaComponentClass == null) {
            throw new NullPointerException();
        }
        if (javaComponentClass == Void.TYPE) {
            throw new IllegalArgumentException();
        }
        if (length < 0) {
            throw new NegativeArraySizeException();
        }
        final ArrayClassActor arrayClassActor = ArrayClassActor.forComponentClassActor(ClassActor.fromJava(javaComponentClass));
        return Heap.createArray(arrayClassActor.dynamicHub(), length);
    }

    /**
     * Allocates a new multi-dimensional array with the specified component class and dimensions.
     *
     * @param javaComponentClass the component type of the innermost dimension
     * @param dimensions an array denoting the size of the inner arrays
     * @return a new multi-dimensional array
     * @throws IllegalArgumentException if the dimension array has length {@code 0} or if the component type is {@code void}
     * @throws NegativeArraySizeException if one of the dimensions is negative
     */
    @SUBSTITUTE
    private static Object multiNewArray(Class javaComponentClass, int[] dimensions) throws IllegalArgumentException, NegativeArraySizeException {
        if (javaComponentClass == null) {
            throw new NullPointerException();
        }
        if (dimensions.length == 0 || javaComponentClass == Void.TYPE) {
            throw new IllegalArgumentException();
        }
        for (int i = 0; i < dimensions.length; i++) {
            if (dimensions[i] < 0) {
                throw new NegativeArraySizeException();
            }
        }
        // get the ultimate outer array type
        ArrayClassActor arrayClassActor = ArrayClassActor.forComponentClassActor(ClassActor.fromJava(javaComponentClass));
        for (int i = 1; i < dimensions.length; i++) {
            arrayClassActor = ArrayClassActor.forComponentClassActor(arrayClassActor);
        }
        return CreateMultiReferenceArray.createMultiReferenceArray(arrayClassActor, dimensions);
    }

}
