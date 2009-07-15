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

    public static final ValueType INT_TYPE = new ValueType(BasicType.Int);
    public static final ValueType LONG_TYPE = new ValueType(BasicType.Long);
    public static final ValueType FLOAT_TYPE = new ValueType(BasicType.Float);
    public static final ValueType DOUBLE_TYPE = new ValueType(BasicType.Double);
    public static final ValueType JSR_TYPE = new ValueType(BasicType.Jsr);
    public static final ValueType OBJECT_TYPE = new ValueType(BasicType.Object);
    public static final ValueType ILLEGAL_TYPE = new ValueType(BasicType.Illegal);
    public static final ValueType VOID_TYPE = new ValueType(BasicType.Void);

    public final BasicType basicType;

    /**
     * The base constructor for a value type accepts a basic type.
     * @param basicType the basic type
     */
    public ValueType(BasicType basicType) {
        this.basicType = basicType.stackType();
    }

    /**
     * Returns the size of this value type in slots.
     * @return the size of this value type in slots
     */
    public int size() {
        return basicType.sizeInSlots();
    }

    /**
     * Checks whether this value type is void.
     * @return <code>true</code> if this type is void
     */
    public final boolean isVoid() {
        return basicType == BasicType.Void;
    }

    /**
     * Checks whether this value type is int.
     * @return <code>true</code> if this type is int
     */
    public final boolean isInt() {
        return basicType == BasicType.Int;
    }

    /**
     * Checks whether this value type is long.
     * @return <code>true</code> if this type is long
     */
    public final boolean isLong() {
        return basicType == BasicType.Long;
    }

    /**
     * Checks whether this value type is float.
     * @return <code>true</code> if this type is float
     */
    public final boolean isFloat() {
        return basicType == BasicType.Float;
    }

    /**
     * Checks whether this value type is double.
     * @return <code>true</code> if this type is double
     */
    public final boolean isDouble() {
        return basicType == BasicType.Double;
    }

    /**
     * Checks if this is an {@code int}, {@code float}, {@code long} or {@code double} value type.
     */
    public final boolean isPrimitive() {
        return isInt() || isFloat() || isLong() || isDouble();
    }

    /**
     * Checks if this is {@linkplain ClassType class type}.
     */
    public boolean isClass() {
        return false;
    }

    /**
     * Checks whether this value type is an object type.
     * @return <code>true</code> if this type is an object
     */
    public final boolean isObject() {
        return basicType == BasicType.Object;
    }

    /**
     * Checks whether this value type is an address type.
     * @return <code>true</code> if this type is an address
     */
    public boolean isJsr() {
        return basicType == BasicType.Jsr;
    }

    /**
     * Checks whether this value type is illegal.
     * @return <code>true</code> if this type is illegal
     */
    public final boolean isIllegal() {
        return basicType == BasicType.Illegal;
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
        return basicType.sizeInSlots() == 1;
    }

    /**
     * Checks whether this type is represented by a double word (two words).
     * @return <code>true</code> if this type is represented by two words
     */
    public boolean isDoubleWord() {
        return basicType.sizeInSlots() == 2;
    }

    /**
     * Returns a single character for this type, e.g. int = 'i'.
     * @return the type character for this type
     */
    public char tchar() {
        return basicType.basicChar;
    }

    /**
     * Returns the name of this value type as a string.
     * @return the name of this value type
     */
    public String name() {
        return basicType.javaName;
    }

    /**
     * Performs the meet operation on this type and another type.
     * @param other the other value type
     * @return the result of the meet operation for these two types
     */
    public final ValueType meet(ValueType other) {
        if (other.basicType == this.basicType) {
            return base();
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
        return base(basicType);
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
    @Override
    public String toString() {
        return basicType.javaName;
    }

    /**
     * Gets the base value type for the specified value tag.
     * @param basicType the basic type
     * @return the value type
     */
    public static ValueType base(BasicType basicType) {
        switch (basicType) {
            // PERF: could be sped up with an array
            case Jsr: return JSR_TYPE;
            case Int: return INT_TYPE;
            case Float: return FLOAT_TYPE;
            case Long: return LONG_TYPE;
            case Double: return DOUBLE_TYPE;
            case Object: return OBJECT_TYPE;
            case Illegal: return ILLEGAL_TYPE;
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

}
