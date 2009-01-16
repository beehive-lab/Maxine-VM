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
package com.sun.max.vm.value;

import java.io.*;

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.hotpath.state.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 * A wrapped/boxed Java value.
 *
 * The value 'null' is is represented by 'ReferenceValue.NULL'.
 *
 * In contrast, plain unboxed 'null' stands for "no Value, not even null".
 *
 * There are 3 mechanisms for unboxing/converting between values of different kinds:
 *
 * 1. The "as<Kind>" methods fail always if (fromKind != toKind).
 * 2. The "unbox<Kind>" methods is the same except for:
 *    - all kinds that are represented as ints on the operand stack are interchangeable
 *      e.g. a byte can be unboxed as an int and vice versa
 *    - word values can be unboxed as ints iff Word.width() == 32
 *    - word values can be unboxed as longs iff Word.width() == 64
 * 3. The "to<Kind>" method always succeed where the Java language allows an explicit cast between the two kinds
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class Value<Value_Type extends Value<Value_Type>> implements Classifiable, Comparable<Value> {

    protected Value() {
    }

    public boolean isCategory1() {
        return kind().isCategory1();
    }

    public boolean isCategory2() {
        return kind().isCategory2();
    }

    public abstract Kind<Value_Type> kind();

    public abstract boolean isZero();

    public abstract boolean isAllOnes();

    @Override
    public abstract boolean equals(Object other);

    /**
     * Performs an comparison between this value and another given value of the same {@linkplain #kind() kind} as this
     * value. This precondition is established by {@link #compareTo(Value)} which is the only method that should call
     * this method.
     *
     * The semantics of comparison with respect to signed versus unsigned comparison matches the Java language semantics
     * for the comparison operators. For example, comparing two {@code char} values makes an unsigned comparison where
     * as comparing two {@code int} values is a signed comparison.
     *
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than
     *         the specified object.
     * @throws IllegalArgumentException if the semantics of comparison for this value's kind are undefined
     */
    protected abstract int compareSameKind(Value_Type other);

    /**
     * Compares this value with another value.
     *
     * If the {@linkplain #kind() kind} of this value is different from the kind of {@code other}, then returned value
     * is negative/positive if the {@linkplain KindEnum#ordinal() ordinal} of this value's kind is lower/greater than
     * the ordinal of {@code other}'s kind.
     *
     * If this value's kind is the same as {@code other}'s kind, then the result is determined by
     * {@link #compareSameKind(Value)}.
     *
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than
     *         the specified object.
     * @throws IllegalArgumentException if the semantics of comparison for this value's kind are undefined
     */
    public int compareTo(Value other) {
        final int kindCompare = kind().asEnum().ordinal() - other.kind().asEnum().ordinal();
        if (kindCompare != 0) {
            return kindCompare;
        }
        return compareSameKind(kind().valueClass().cast(other));
    }

    /**
     * Performs an unsigned comparison between this value and another given value of the same {@linkplain #kind() kind}
     * as this value. This precondition is established by {@link #compareTo(Value)} which is the only method that should
     * call this method.
     *
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than
     *         the specified object.
     * @throws IllegalArgumentException if the semantics of unsigned comparison for this value's kind are undefined
     */
    protected int unsignedCompareSameKind(Value_Type other) {
        throw new IllegalArgumentException("Cannot perform unsigned comparison between values of kind " + kind());
    }

    /**
     * Performs an unsigned comparison between this value and another given value.
     *
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than
     *         the specified object.
     * @throws IllegalArgumentException if {@code other}'s {@linkplain #kind() kind} is not the same as this value's
     *             kind or the semantics of unsigned comparison for this value's kind are undefined
     */
    public int unsignedCompareTo(Value other) {
        if (kind() != other.kind()) {
            throw new IllegalArgumentException("Cannot perform unsigned comparison between values of different kinds");
        }
        return unsignedCompareSameKind(kind().valueClass().cast(other));
    }

    /**
     * Must not be used to obtain a 'WordValue' from a 'Word',
     * because this would create reference tracking ambiguity wrt. the parameter 'boxedJavaValue'!
     *
     * If you really need one, our canonical "boxed Java value type" for 'Word' is 'WordValue'.
     */
    public static Value fromBoxedJavaValue(Object boxedJavaValue) {
        if (boxedJavaValue == null) {
            return ReferenceValue.NULL;
        }
        if (boxedJavaValue instanceof Byte) {
            final Byte box = (Byte) boxedJavaValue;
            return ByteValue.from(box.byteValue());
        }
        if (boxedJavaValue instanceof Boolean) {
            final Boolean box = (Boolean) boxedJavaValue;
            return BooleanValue.from(box.booleanValue());
        }
        if (boxedJavaValue instanceof Short) {
            final Short box = (Short) boxedJavaValue;
            return ShortValue.from(box.shortValue());
        }
        if (boxedJavaValue instanceof Character) {
            final Character box = (Character) boxedJavaValue;
            return CharValue.from(box.charValue());
        }
        if (boxedJavaValue instanceof Integer) {
            final Integer box = (Integer) boxedJavaValue;
            return IntValue.from(box.intValue());
        }
        if (boxedJavaValue instanceof Float) {
            final Float box = (Float) boxedJavaValue;
            return FloatValue.from(box.floatValue());
        }
        if (boxedJavaValue instanceof Long) {
            final Long box = (Long) boxedJavaValue;
            return LongValue.from(box.longValue());
        }
        if (boxedJavaValue instanceof Double) {
            final Double box = (Double) boxedJavaValue;
            return DoubleValue.from(box.doubleValue());
        }
        if (boxedJavaValue instanceof WordValue) {
            return (WordValue) boxedJavaValue;
        }
        return ReferenceValue.from(boxedJavaValue);
    }

    /**
     * See {@link #fromBoxedJavaValue(Object)}.
     */
    public static Value[] fromBoxedJavaValues(Object... boxedJavaValues) {
        final Value[] values = new Value[boxedJavaValues.length];
        for (int i = 0; i < boxedJavaValues.length; ++i) {
            values[i] = fromBoxedJavaValue(boxedJavaValues[i]);
        }
        return values;
    }

    /**
     * Converts a (sub)array of {@code Value}s to corresponding boxed Java values.
     *
     * @param offset index in {@code values} at which to start converting
     * @param length the number of values to convert
     * @param values the values to be converted
     * @return the elements in {@code values} at indexes {@code [offset .. (offset+length)]} inclusive converted to
     *         their corresponding {@linkplain #asBoxedJavaValue() boxed Java values}
     */
    public static Object[] asBoxedJavaValues(int offset, int length, Value... values) {
        final Object[] result = new Object[length];
        for (int i = 0; i != length; ++i) {
            result[i] = values[i + offset].asBoxedJavaValue();
        }
        return result;
    }

    /**
     * Gets this value converted to a boxed Java value.
     */
    public abstract Object asBoxedJavaValue();

    private IllegalArgumentException illegalConversion(String expected) {
        return new IllegalArgumentException("expected " + expected + ", got " + kind() + "[" + this + "]");
    }

    /**
     * @return the byte boxed by this
     * @throws IllegalArgumentException if this is not a ByteValue
     */
    public byte asByte() {
        throw illegalConversion("byte");
    }

    /**
     * @return my boxed value converted to a byte
     */
    public byte toByte() {
        throw new IllegalArgumentException();
    }

    /**
     * @return my boxed value interpreted in an unsigned manner converted to a byte
     */
    public byte unsignedToByte() {
        throw new IllegalArgumentException();
    }

    /**
     * @return the boolean boxed by this
     * @throws IllegalArgumentException if this is not a BooleanValue
     */
    public boolean asBoolean() {
        throw illegalConversion("boolean");
    }

    /**
     * @return my boxed value converted to a boolean
     */
    public boolean toBoolean() {
        throw new IllegalArgumentException();
    }

    /**
     * @return the short boxed by this
     * @throws IllegalArgumentException if this is not a ShortValue
     */
    public short asShort() {
        throw illegalConversion("short");
    }

    /**
     * @return my boxed value converted to a short
     */
    public short toShort() {
        throw new IllegalArgumentException();
    }

    /**
     * @return my boxed value interpreted in an unsigned manner converted to a short
     */
    public short unsignedToShort() {
        throw new IllegalArgumentException();
    }

    /**
     * @return the char boxed by this
     * @throws IllegalArgumentException if this is not a CharValue
     */
    public char asChar() {
        throw illegalConversion("char");
    }

    /**
     * @return my boxed value converted to a char
     */
    public char toChar() {
        throw new IllegalArgumentException();
    }

    /**
     * @return the int boxed by this
     * @throws IllegalArgumentException if this is not a IntValue
     */
    public int asInt() {
        throw illegalConversion("int");
    }

    /**
     * @return my boxed value converted to an int
     */
    public int toInt() {
        throw new IllegalArgumentException();
    }

    /**
     * @return my boxed value interpreted in an unsigned manner converted to an int
     */
    public int unsignedToInt() {
        throw new IllegalArgumentException();
    }

    /**
     * @return the float boxed by this
     * @throws IllegalArgumentException if this is not a FloatValue
     */
    public float asFloat() {
        throw illegalConversion("float");
    }

    /**
     * @return my boxed value converted to a float
     */
    public float toFloat() {
        throw new IllegalArgumentException();
    }

    /**
     * @return the long boxed by this
     * @throws IllegalArgumentException if this is not a LongValue
     */
    public long asLong() {
        throw illegalConversion("long");
    }

    /**
     * @return my boxed value converted to a long
     */
    public long toLong() {
        throw new IllegalArgumentException();
    }

    /**
     * @return the double boxed by this
     * @throws IllegalArgumentException if this is not a DoubleValue
     */
    public double asDouble() {
        throw illegalConversion("double");
    }

    /**
     * @return my boxed value converted to a double
     */
    public double toDouble() {
        throw new IllegalArgumentException();
    }

    /**
     * @return the Word boxed by this
     * @throws IllegalArgumentException if this is not a WordValue
     */
    public Word asWord() {
        throw illegalConversion("word");
    }

    /**
     * @return my boxed value converted to a Word
     */
    public Word toWord() {
        throw new IllegalArgumentException();
    }

    /**
     * @return the Grip boxed by this
     * @throws IllegalArgumentException if this is not a GripValue or a ReferenceValue
     */
    public Grip asGrip() {
        throw illegalConversion("grip");
    }

    /**
     * @return the Reference boxed by this
     * @throws IllegalArgumentException if this is not a ReferenceValue or a GripValue
     */
    public Reference asReference() {
        throw illegalConversion("reference");
    }

    /**
     * @return the object represented by the Reference boxed by this
     * @throws IllegalArgumentException if this is not a ReferenceValue
     */
    public Object asObject() {
        throw illegalConversion("object");
    }


    public boolean unboxBoolean() {
        throw illegalConversion("boolean");
    }

    public byte unboxByte() {
        throw illegalConversion("byte");
    }

    public char unboxChar() {
        throw illegalConversion("char");
    }

    public short unboxShort() {
        throw illegalConversion("short");
    }

    public int unboxInt() {
        throw illegalConversion("int");
    }

    public float unboxFloat() {
        throw illegalConversion("float");
    }

    public long unboxLong() {
        throw illegalConversion("long");
    }

    public double unboxDouble() {
        throw illegalConversion("double");
    }

    public Object unboxObject() {
        throw illegalConversion("object");
    }

    public Word unboxWord() {
        throw illegalConversion("word");
    }


    public abstract WordWidth signedEffectiveWidth();

    public abstract WordWidth unsignedEffectiveWidth();

    public abstract byte[] toBytes(DataModel dataModel);

    public abstract void write(DataOutput stream) throws IOException;
}
