/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.value;

import com.sun.c1x.util.Util;

/**
 * The <code>ValueType</code> class represents the type of a value in the IR.
 * These types correspond to the types of local variables and stack slots
 * in Java bytecode; thus there are no types for bytes, booleans, shorts, or
 * chars, because these cannot be distinguished by Java bytecode operations.
 *
 * @author Ben L. Titzer
 */
public class ValueType {

    public static final ValueType INT_TYPE = new ValueType(ValueTag.INT_TAG, 1);
    public static final ValueType LONG_TYPE = new ValueType(ValueTag.LONG_TAG, 2);
    public static final ValueType FLOAT_TYPE = new ValueType(ValueTag.FLOAT_TAG, 1);
    public static final ValueType DOUBLE_TYPE = new ValueType(ValueTag.DOUBLE_TAG, 2);
    public static final ValueType JSR_TYPE = new ValueType(ValueTag.JSR_TAG, 1);
    public static final ValueType OBJECT_TYPE = new ValueType(ValueTag.OBJECT_TAG, 1);
    public static final ValueType ILLEGAL_TYPE = new ValueType(ValueTag.ILLEGAL_TAG, 1);
    public static final ValueType VOID_TYPE = new ValueType(ValueTag.VOID_TAG, 0);

    private final int _size;
    private final byte _tag;

    /**
     * The base constructor for a value type accepts a type tag and a size (in words)
     * of this object.
     * @param tag the type tag for this value type
     * @param size the size in words
     */
    public ValueType(byte tag, int size) {
        _tag = tag;
        _size = size;
    }

    /**
     * Returns the size of this value type in words.
     * @return the size of this value type in words
     */
    public int size() {
        return _size;
    }

    /**
     * Returns the type tag of this value type.
     * @return the type tag of this value type
     */
    public byte tag() {
        return _tag;
    }

    /**
     * Checks whether this value type is void.
     * @return <code>true</code> if this type is void
     */
    public final boolean isVoid() {
        return _tag == ValueTag.VOID_TAG;
    }

    /**
     * Checks whether this value type is int.
     * @return <code>true</code> if this type is int
     */
    public final boolean isInt() {
        return _tag == ValueTag.INT_TAG;
    }

    /**
     * Checks whether this value type is long.
     * @return <code>true</code> if this type is long
     */
    public final boolean isLong() {
        return _tag == ValueTag.LONG_TAG;
    }

    /**
     * Checks whether this value type is float.
     * @return <code>true</code> if this type is float
     */
    public final boolean isFloat() {
        return _tag == ValueTag.FLOAT_TAG;
    }

    /**
     * Checks whether this value type is double.
     * @return <code>true</code> if this type is double
     */
    public final boolean isDouble() {
        return _tag == ValueTag.DOUBLE_TAG;
    }

    /**
     * Checks if this is an {@code int}, {@code float}, {@code long} or {@code double} value type.
     */
    public final boolean isPrimitive() {
        return isInt() || isFloat() || isLong() || isDouble();
    }

    /**
     * Checks whether this value type is an object type.
     * @return <code>true</code> if this type is an object
     */
    public final boolean isObject() {
        return _tag == ValueTag.OBJECT_TAG;
    }

    /**
     * Checks whether this value type is an address type.
     * @return <code>true</code> if this type is an address
     */
    public boolean isAddress() {
        return _tag == ValueTag.JSR_TAG;
    }

    /**
     * Checks whether this value type is illegal.
     * @return <code>true</code> if this type is illegal
     */
    public final boolean isIllegal() {
        return _tag == ValueTag.ILLEGAL_TAG;
    }

    /**
     * Converts this value type to a constant, if it is a constant.
     * @return a reference to the constant representing this value type, if it is a constant;
     * <code>null</code> otherwise
     */
    public ConstType asConstant() {
        if (this instanceof ConstType) {
            return (ConstType) this;
        }
        return null;
    }

    /**
     * Checks whether this type is represented by a single word.
     * @return true if this type is represented by a single word
     */
    public boolean isSingleWord() {
        return _size == 1;
    }

    /**
     * Checks whether this type is represented by a double word (two words).
     * @return <code>true</code> if this type is represented by two words
     */
    public boolean isDoubleWord() {
        return _size == 2;
    }

    /**
     * Returns a single character for this type, e.g. int = 'i'.
     * @return the type character for this type
     */
    public char tchar() {
        return ValueTag.tagChar(_tag);
    }

    /**
     * Returns the name of this value type as a string.
     * @return the name of this value type
     */
    public String name() {
        return ValueTag.tagName(_tag);
    }

    /**
     * Performs the meet operation on this type and another type.
     * @param other the other value type
     * @return the result of the meet operation for these two types
     */
    public final ValueType meet(ValueType other) {
        if (other._tag == this._tag) {
            return base(_tag);
        }
        return ILLEGAL_TYPE;
    }

    /**
     * Gets the value type for this type, ignoring whether this type is a constant or not.
     * @return the base value type for this value type
     */
    public final ValueType base() {
        if (getClass() == ValueType.class) {
            return this;
        }
        return base(_tag);
    }

    /**
     * Performs the merge operation on this type and another type.
     * @param other the other value type
     * @return the result of the merge operation for these two types
     */
    public ValueType merge(ValueType other) {
        if (other._tag == this._tag && other.getClass() == ValueType.class) {
            return this;
        }
        return ILLEGAL_TYPE;
    }

    /**
     * Checks whether this value type is a constant.
     * @return true if this value type is a constant.
     */
    public boolean isConstant() {
        return false;
    }

    /**
     * Converts this value type to a string.
     */
    public String toString() {
        return ValueTag.tagName(_tag);
    }

    /**
     * Gets the base value type for the specified value tag.
     * @param tag the value tag
     * @return the value type
     */
    public static ValueType base(byte tag) {
        switch (tag) {
            case ValueTag.JSR_TAG: return JSR_TYPE;
            case ValueTag.INT_TAG: return INT_TYPE;
            case ValueTag.FLOAT_TAG: return FLOAT_TYPE;
            case ValueTag.LONG_TAG: return LONG_TYPE;
            case ValueTag.DOUBLE_TAG: return DOUBLE_TYPE;
            case ValueTag.OBJECT_TAG: return OBJECT_TYPE;
            case ValueTag.ILLEGAL_TAG: return ILLEGAL_TYPE;
        }
        throw Util.shouldNotReachHere();
    }

    /**
     * Gets the appropriate value type for a basic type.
     * @param basicType the basic type
     * @return the appropriate value type for the basic type
     */
    public static ValueType fromBasicType(BasicType basicType) {
        switch (basicType) {
            case Void: return VOID_TYPE;
            case Byte: return INT_TYPE;
            case Char: return INT_TYPE;
            case Short: return INT_TYPE;
            case Boolean: return INT_TYPE;
            case Int: return INT_TYPE;
            case Long: return LONG_TYPE;
            case Float: return FLOAT_TYPE;
            case Double: return DOUBLE_TYPE;
            case Object: return OBJECT_TYPE;
            case Jsr: return JSR_TYPE;
        }
        return ILLEGAL_TYPE;
    }

    /**
     * Gets the basic type for this ValueType.
     * @return the basic type
     */
    public BasicType basicType() {
        switch (_tag) {
            case ValueTag.INT_TAG: return BasicType.Int;
            case ValueTag.LONG_TAG: return BasicType.Long;
            case ValueTag.FLOAT_TAG: return BasicType.Float;
            case ValueTag.DOUBLE_TAG: return BasicType.Double;
            case ValueTag.OBJECT_TAG: return BasicType.Object;
            case ValueTag.JSR_TAG: return BasicType.Jsr;
            case ValueTag.VOID_TAG: return BasicType.Void;
        }
        return BasicType.Illegal;
    }
}
