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
package com.sun.c1x.ci;


/**
 * The <code>ConstType</code> class represents a value type for a constant on the stack
 * or in a local variable. This class implements the functionality provided by the
 * IntConstant, LongConstant, FloatConstant, etc classes in the C++ C1 compiler. This class
 * can represent either reference constants or primitive constants by boxing them.
 *
 * @author Ben L. Titzer
 */
public class CiConstant {

    public static final CiConstant NULL_OBJECT = new CiConstant(CiKind.Object, null);
    public static final CiConstant INT_MINUS_1 = forInt(-1);
    public static final CiConstant INT_0 = forInt(0);
    public static final CiConstant INT_1 = forInt(1);
    public static final CiConstant INT_2 = forInt(2);
    public static final CiConstant INT_3 = forInt(3);
    public static final CiConstant INT_4 = forInt(4);
    public static final CiConstant INT_5 = forInt(5);
    public static final CiConstant LONG_0 = forLong(0);
    public static final CiConstant LONG_1 = forLong(1);
    public static final CiConstant FLOAT_0 = forFloat(0);
    public static final CiConstant FLOAT_1 = forFloat(1);
    public static final CiConstant FLOAT_2 = forFloat(2);
    public static final CiConstant DOUBLE_0 = forDouble(0);
    public static final CiConstant DOUBLE_1 = forDouble(1);

    private final Object value;
    public final CiKind basicType;

    /**
     * Create a new constant type represented by the specified object reference or boxed
     * primitive.
     * @param type the type of this constant
     * @param value the value of this constant
     */
    public CiConstant(CiKind type, Object value) {
        this.basicType = type;
        this.value = value;
    }

    /**
     * Checks whether this constant is non-null.
     * @return {@code true} if this constant is a primitive, or an object constant that is not null
     */
    public boolean isNonNull() {
        return value != null;
    }

    /**
     * Converts this value type to a string.
     */
    @Override
    public String toString() {
        final String val = basicType.isObject() ? "object@" + System.identityHashCode(value) : value.toString();
        return basicType.javaName + " = " + val;
    }

    /**
     * Gets this constant's value as a string.
     *
     * @return this constant's value as a string
     */
    public String valueString() {
        return value.toString();
    }

    /**
     * Returns the value of this constant as a boxed Java value.
     * @return the value of this constant
     */
    public Object boxedValue() {
        return value;
    }

    public boolean equivalent(CiConstant other) {
        return other == this || valueEqual(other);
    }

    private boolean valueEqual(CiConstant cother) {
        // must have equivalent tags to be equal
        if (basicType != cother.basicType) {
            return false;
        }
        // use == for object references and .equals() for boxed types
        if (value == cother.value) {
            return true;
        } else if (!basicType.isObject() && value != null && value.equals(cother.value)) {
            return true;
        }
        return false;
    }

    /**
     * Converts this constant to a primitive int.
     * @return the int value of this constant
     */
    public int asInt() {
        if (basicType != CiKind.Object) {
            if (value instanceof Integer) {
                return (Integer) value;
            }
            if (value instanceof Byte) {
                return (Byte) value;
            }
            if (value instanceof Short) {
                return (Short) value;
            }
            if (value instanceof Character) {
                return (Character) value;
            }
            if (value instanceof Boolean) {
                return (Boolean) value ? 1 : 0; // note that we allow Boolean values to be used as ints
            }
        }
        throw new Error("Invalid constant");
    }

    /**
     * Converts this constant to a primitive long.
     * @return the long value of this constant
     */
    public long asLong() {
        if (basicType != CiKind.Object) {
            if (value instanceof Long) {
                return (Long) value;
            }
            if (value instanceof Integer) {
                return (Integer) value;
            }
            if (value instanceof Byte) {
                return (Byte) value;
            }
            if (value instanceof Short) {
                return (Short) value;
            }
            if (value instanceof Character) {
                return (Character) value;
            }
            if (value instanceof Boolean) {
                return (Boolean) value ? 1 : 0; // note that we allow Boolean values to be used as ints
            }
        }
        throw new Error("Invalid constant");
    }

    /**
     * Converts this constant to a primitive float.
     * @return the float value of this constant
     */
    public float asFloat() {
        if (basicType != CiKind.Object) {
            if (value instanceof Float) {
                return (Float) value;
            }
        }
        throw new Error("Invalid constant");
    }

    /**
     * Converts this constant to a primitive double.
     * @return the double value of this constant
     */
    public double asDouble() {
        if (basicType != CiKind.Object) {
            if (value instanceof Double) {
                return (Double) value;
            }
            if (value instanceof Float) {
                return (Float) value;
            }
        }
        throw new Error("Invalid constant");
    }

    /**
     * Converts this constant to the object reference it represents.
     * @return the object which this constant represents
     */
    public Object asObject() {
        if (basicType == CiKind.Object) {
            return value;
        }
        throw new Error("Invalid constant");
    }

    /**
     * Computes the hashcode of this constant.
     * @return a suitable hashcode for this constant
     */
    @Override
    public int hashCode() {
        if (basicType == CiKind.Object) {
            return System.identityHashCode(value);
        }
        return value.hashCode();
    }

    /**
     * Checks whether this constant equals another object. This is only
     * true if the other object is a constant and has the same value.
     * @param o the object to compare equality
     * @return <code>true</code> if this constant is equivalent to the specified object
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof CiConstant && valueEqual((CiConstant) o);
    }

    /**
     * Checks whether this constant is the default value for its type.
     * @return <code>true</code> if the value is the default value for its type; <code>false</code> otherwise
     */
    public boolean isDefaultValue() {
        switch (basicType) {
            case Int: return asInt() == 0;
            case Long: return asLong() == 0;
            case Float: return asFloat() == 0.0f; // TODO: be careful about -0.0
            case Double: return asDouble() == 0.0d; // TODO: be careful about -0.0
            case Object: return asObject() == null;
        }
        return false;
    }

    /**
     * Utility method to create a value type for a double constant.
     * @param d the double value for which to create the value type
     * @return a value type representing the double
     */
    public static CiConstant forDouble(double d) {
        return new CiConstant(CiKind.Double, d);
    }

    /**
     * Utility method to create a value type for a float constant.
     * @param f the float value for which to create the value type
     * @return a value type representing the float
     */
    public static CiConstant forFloat(float f) {
        return new CiConstant(CiKind.Float, f);
    }

    /**
     * Utility method to create a value type for an long constant.
     * @param i the long value for which to create the value type
     * @return a value type representing the long
     */
    public static CiConstant forLong(long i) {
        return new CiConstant(CiKind.Long, i);
    }

    /**
     * Utility method to create a value type for an integer constant.
     * @param i the integer value for which to create the value type
     * @return a value type representing the integer
     */
    public static CiConstant forInt(int i) {
        return new CiConstant(CiKind.Int, i);
    }

    /**
     * Utility method to create a value type for a byte constant.
     * @param i the byte value for which to create the value type
     * @return a value type representing the byte
     */
    public static CiConstant forByte(byte i) {
        return new CiConstant(CiKind.Byte, i);
    }

    /**
     * Utility method to create a value type for a boolean constant.
     * @param i the boolean value for which to create the value type
     * @return a value type representing the boolean
     */
    public static CiConstant forBoolean(boolean i) {
        return new CiConstant(CiKind.Boolean, i);
    }

    /**
     * Utility method to create a value type for a char constant.
     * @param i the char value for which to create the value type
     * @return a value type representing the char
     */
    public static CiConstant forChar(char i) {
        return new CiConstant(CiKind.Char, i);
    }

    /**
     * Utility method to create a value type for a short constant.
     * @param i the short value for which to create the value type
     * @return a value type representing the short
     */
    public static CiConstant forShort(short i) {
        return new CiConstant(CiKind.Short, i);
    }

    /**
     * Utility method to create a value type for an address (jsr/ret address) constant.
     * @param i the address value for which to create the value type
     * @return a value type representing the address
     */
    public static CiConstant forJsr(int i) {
        return new CiConstant(CiKind.Jsr, i);
    }

    /**
     * Utility method to create a value type for a word constant.
     * @param i the number representing the word's value, either an {@link Integer} or a {@link Long}
     * @return a value type representing the word
     */
    public static CiConstant forWord(Number i) {
        if (i instanceof Integer || i instanceof Long) {
            return new CiConstant(CiKind.Word, i); // only Integer and Long are allowed
        }
        throw new IllegalArgumentException("cannot create word ConstType for object of type " + i.getClass());
    }

    /**
     * Utility method to create a value type for an object constant.
     * @param o the object value for which to create the value type
     * @return a value type representing the object
     */
    public static CiConstant forObject(Object o) {
        if (o == null) {
            return NULL_OBJECT;
        }
        return new CiConstant(CiKind.Object, o);
    }

    /**
     * @param object
     * @return
     */
    public static CiConstant fromBoxedJavaValue(Object boxedJavaValue) {
        if (boxedJavaValue == null) {
            return CiConstant.NULL_OBJECT;
        }
        if (boxedJavaValue instanceof Byte) {
            final Byte box = (Byte) boxedJavaValue;
            return CiConstant.forByte(box.byteValue());
        }
        if (boxedJavaValue instanceof Boolean) {
            final Boolean box = (Boolean) boxedJavaValue;
            return CiConstant.forBoolean(box.booleanValue());
        }
        if (boxedJavaValue instanceof Short) {
            final Short box = (Short) boxedJavaValue;
            return CiConstant.forShort(box.shortValue());
        }
        if (boxedJavaValue instanceof Character) {
            final Character box = (Character) boxedJavaValue;
            return CiConstant.forChar(box.charValue());
        }
        if (boxedJavaValue instanceof Integer) {
            final Integer box = (Integer) boxedJavaValue;
            return CiConstant.forInt(box.intValue());
        }
        if (boxedJavaValue instanceof Float) {
            final Float box = (Float) boxedJavaValue;
            return CiConstant.forFloat(box.floatValue());
        }
        if (boxedJavaValue instanceof Long) {
            final Long box = (Long) boxedJavaValue;
            return CiConstant.forLong(box.longValue());
        }
        if (boxedJavaValue instanceof Double) {
            final Double box = (Double) boxedJavaValue;
            return CiConstant.forDouble(box.doubleValue());
        }
        return CiConstant.forObject(boxedJavaValue);
    }
}
