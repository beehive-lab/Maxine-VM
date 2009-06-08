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

import com.sun.c1x.C1XOptions;

/**
 * The <code>ConstType</code> class represents a value type for a constant on the stack
 * or in a local variable. This class implements the functionality provided by the
 * IntConstant, LongConstant, FloatConstant, etc classes in the C++ C1 compiler. This class
 * can represent either reference constants or primitive constants by boxing them.
 *
 * @author Ben L. Titzer
 */
public class ConstType extends ValueType {

    public static final ConstType NULL_OBJECT = new ConstType(ValueTag.OBJECT_TAG, 1, null, true);
    public static final ConstType INT_MINUS_1 = ConstType.forInt(-1);
    public static final ConstType INT_0 = ConstType.forInt(0);
    public static final ConstType INT_1 = ConstType.forInt(1);
    public static final ConstType INT_2 = ConstType.forInt(2);
    public static final ConstType INT_3 = ConstType.forInt(3);
    public static final ConstType INT_4 = ConstType.forInt(4);
    public static final ConstType INT_5 = ConstType.forInt(5);
    public static final ConstType LONG_0 = ConstType.forLong(0);
    public static final ConstType LONG_1 = ConstType.forLong(1);
    public static final ConstType FLOAT_0 = ConstType.forFloat(0);
    public static final ConstType FLOAT_1 = ConstType.forFloat(1);
    public static final ConstType FLOAT_2 = ConstType.forFloat(2);
    public static final ConstType DOUBLE_0 = ConstType.forDouble(0);
    public static final ConstType DOUBLE_1 = ConstType.forDouble(1);

    private final Object _value;
    private final boolean _isObject;

    /**
     * Create a new constant type represented by the specified object reference or boxed
     * primitive.
     * @param tag the type tag of this constant
     * @param size the size of this constant
     * @param value the value of this constant
     * @param isObject true if this constant is an object reference; false otherwise
     */
    public ConstType(byte tag, int size, Object value, boolean isObject) {
        super(tag, size);
        _value = value;
        _isObject = isObject;
    }

    /**
     * Instances of this class are constants.
     * @return true
     */
    @Override
    public boolean isConstant() {
        return true;
    }

    /**
     * Modified merge operations for constants. Merge of two identical constants
     * will return the first. Merge of two constants of the same type will return
     * the common (nonconstant) value type. Meet of anything else results in the
     * illegal value type.
     * @param other the other value type to merge with
     * @return the value type representing the meet operation
     */
    public ValueType merge(ValueType other) {
        if (tag() != other.tag()) {
            return ILLEGAL_TYPE;
        }
        if (C1XOptions.MergeEquivalentConstants && equivalent(other)) {
            return this;
        }
        return new ValueType(tag(), size());
    }

    public boolean equivalent(ValueType other) {
        if (other == this) {
            return true;
        }
        if (other instanceof ConstType) {
            ConstType cother = (ConstType) other;
            // must have equivalent tags to be equal
            if (tag() != cother.tag()) {
                return false;
            }
            // use == for object references and .equals() for boxed types
            if (_value == cother._value) {
                return true;
            } else if (!(_isObject || cother._isObject) && _value != null && _value.equals(cother._value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Converts this constant to a primitive int.
     * @return the int value of this constant
     */
    public int asInt() {
        if (!_isObject) {
            if (_value instanceof Integer) {
                return (Integer) _value;
            }
            if (_value instanceof Byte) {
                return (Byte) _value;
            }
            if (_value instanceof Short) {
                return (Short) _value;
            }
            if (_value instanceof Character) {
                return (Character) _value;
            }
        }
        throw new Error("Invalid constant");
    }

    /**
     * Converts this constant to a primitive long.
     * @return the long value of this constant
     */
    public long asLong() {
        if (!_isObject) {
            if (_value instanceof Long) {
                return (Long) _value;
            }
            if (_value instanceof Integer) {
                return (Integer) _value;
            }
            if (_value instanceof Byte) {
                return (Byte) _value;
            }
            if (_value instanceof Short) {
                return (Short) _value;
            }
            if (_value instanceof Character) {
                return (Character) _value;
            }
        }
        throw new Error("Invalid constant");
    }

    /**
     * Converts this constant to a primitive float.
     * @return the float value of this constant
     */
    public float asFloat() {
        if (!_isObject) {
            if (_value instanceof Float) {
                return (Float) _value;
            }
        }
        throw new Error("Invalid constant");
    }

    /**
     * Converts this constant to a primitive double.
     * @return the double value of this constant
     */
    public double asDouble() {
        if (!_isObject) {
            if (_value instanceof Double) {
                return (Double) _value;
            }
            if (_value instanceof Float) {
                return (Float) _value;
            }
        }
        throw new Error("Invalid constant");
    }

    /**
     * Converts this constant to the object reference it represents.
     * @return the object which this constant represents
     */
    public Object asObject() {
        if (_isObject) {
            return _value;
        }
        throw new Error("Invalid constant");
    }

    /**
     * Computes the hashcode of this constant.
     * @return a suitable hashcode for this constant
     */
    public int hashCode() {
        if (_isObject) {
            return System.identityHashCode(_value);
        }
        return _value.hashCode();
    }

    /**
     * Checks whether this constant equals another object. This is only
     * true if the other object is a constant and has the same value.
     * @param o the object to compare equality
     * @return <code>true</code> if this constant is equivalent to the specified object
     */
    public boolean equals(Object o) {
        return o instanceof ConstType && equivalent((ConstType) o);
    }

    /**
     * Utility method to create a value type for an object constant.
     * @param o the object value for which to create the value type
     * @return a value type representing the object
     */
    public static ConstType forObject(Object o) {
        if (o == null) {
            return NULL_OBJECT;
        }
        return new ConstType(ValueTag.OBJECT_TAG, 1, o, true);
    }

    /**
     * Checks whether this constant is the default value for its type.
     * @return <code>true</code> if the value is the default value for its type; <code>false</code> otherwise
     */
    public boolean isDefaultValue() {
        switch (tag()) {
            case ValueTag.INT_TAG: return asInt() == 0;
            case ValueTag.LONG_TAG: return asLong() == 0;
            case ValueTag.FLOAT_TAG: return asFloat() == 0.0f; // TODO: be careful about -0.0
            case ValueTag.DOUBLE_TAG: return asDouble() == 0.0d; // TODO: be careful about -0.0
            case ValueTag.OBJECT_TAG: return asObject() == null;
        }
        return false;
    }

    /**
     * Utility method to create a value type for a double constant.
     * @param d the double value for which to create the value type
     * @return a value type representing the double
     */
    public static ConstType forDouble(double d) {
        return new ConstType(ValueTag.DOUBLE_TAG, 2, d, false);
    }

    /**
     * Utility method to create a value type for a float constant.
     * @param f the float value for which to create the value type
     * @return a value type representing the float
     */
    public static ConstType forFloat(float f) {
        return new ConstType(ValueTag.FLOAT_TAG, 1, f, false);
    }

    /**
     * Utility method to create a value type for an long constant.
     * @param i the long value for which to create the value type
     * @return a value type representing the long
     */
    public static ConstType forLong(long i) {
        return new ConstType(ValueTag.LONG_TAG, 2, i, false);
    }

    /**
     * Utility method to create a value type for an integer constant.
     * @param i the integer value for which to create the value type
     * @return a value type representing the integer
     */
    public static ConstType forInt(int i) {
        return new ConstType(ValueTag.INT_TAG, 1, i, false);
    }

    /**
     * Utility method to create a value type for a byte constant.
     * @param i the byte value for which to create the value type
     * @return a value type representing the byte
     */
    public static ConstType forByte(byte i) {
        return new ConstType(ValueTag.INT_TAG, 1, i, false);
    }

    /**
     * Utility method to create a value type for a boolean constant.
     * @param i the boolean value for which to create the value type
     * @return a value type representing the boolean
     */
    public static ConstType forBoolean(boolean i) {
        return new ConstType(ValueTag.INT_TAG, 1, i, false);
    }

    /**
     * Utility method to create a value type for a char constant.
     * @param i the char value for which to create the value type
     * @return a value type representing the char
     */
    public static ConstType forChar(char i) {
        return new ConstType(ValueTag.INT_TAG, 1, i, false);
    }

    /**
     * Utility method to create a value type for a short constant.
     * @param i the short value for which to create the value type
     * @return a value type representing the short
     */
    public static ConstType forShort(short i) {
        return new ConstType(ValueTag.INT_TAG, 1, i, false);
    }

    /**
     * Utility method to create a value type for an address (jsr/ret address) constant.
     * @param i the address value for which to create the value type
     * @return a value type representing the address
     */
    public static ConstType forAddress(int i) {
        return new ConstType(ValueTag.ADDRESS_TAG, 1, i, false);
    }

}
