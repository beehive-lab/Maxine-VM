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
package com.sun.cri.ci;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;


/**
 * This class represents a boxed value, such as integer, floating point number, or object reference,
 * within the compiler and across the compiler/runtime barrier.
 *
 * @author Ben L. Titzer
 */
public final class CiConstant extends CiValue {

    /**
     * Cache for non-Object boxed constants.
     */
    private static final Map<Object, CiConstant>[] cache;
    
    @SuppressWarnings("unchecked")
    private static <T> T suppressCastWarning(Object object) {
        return (T) object;
    }

    static {
        final Object array = Array.newInstance(Map.class, CiKind.VALUES.length);
        cache = suppressCastWarning(array);
        for (CiKind kind : CiKind.VALUES) {
            if (!kind.isVoid() & !kind.isObject()) {
                cache[kind.ordinal()] = new HashMap<Object, CiConstant>();
            }
        }
    }
    
    /**
     * 
     * @param out
     */
    public static void dumpCacheStats(PrintStream out) {
        out.println("CiConstant cache contents:");
        for (CiKind kind : CiKind.VALUES) {
            Map<Object, CiConstant> map = cache[kind.ordinal()];
            if (map != null) {
                out.printf("  %8s: %d%n", kind, map.size());
            }
        }
    }
    
    public static final CiConstant NULL_OBJECT = get(CiKind.Object, null);
    public static final CiConstant ZERO = get(CiKind.Word, 0L);
    public static final CiConstant INT_MINUS_1 = get(CiKind.Int, -1);
    public static final CiConstant INT_0 = get(CiKind.Int, 0);
    public static final CiConstant INT_1 = get(CiKind.Int, 1);
    public static final CiConstant INT_2 = get(CiKind.Int, 2);
    public static final CiConstant INT_3 = get(CiKind.Int, 3);
    public static final CiConstant INT_4 = get(CiKind.Int, 4);
    public static final CiConstant INT_5 = get(CiKind.Int, 5);
    public static final CiConstant LONG_0 = get(CiKind.Long, 0L);
    public static final CiConstant LONG_1 = get(CiKind.Long, 1L);
    public static final CiConstant FLOAT_0 = get(CiKind.Float, 0.0F);
    public static final CiConstant FLOAT_1 = get(CiKind.Float, 1.0F);
    public static final CiConstant FLOAT_2 = get(CiKind.Float, 2.0F);
    public static final CiConstant DOUBLE_0 = get(CiKind.Double, 0.0D);
    public static final CiConstant DOUBLE_1 = get(CiKind.Double, 1.0D);

    private final Object value;

    /**
     * Create a new constant represented by the specified object reference or boxed
     * primitive.
     * @param kind the type of this constant
     * @param value the value of this constant
     */
    private CiConstant(CiKind kind, Object value) {
        super(kind);
        this.value = value;
    }

    /**
     * Gets a boxed value for a given value of a given kind. For non-object kinds,
     * this operation tries to find a cached copy of the boxed value.
     * 
     * @param kind the kind of {@code value}
     * @param value the value to box
     * @return a boxed copy of {@code value}
     */
    public static CiConstant get(CiKind kind, Object value) {
        if (kind.isObject()) {
            return new CiConstant(kind, value);
        }
        Map<Object, CiConstant> map = cache[kind.ordinal()];
        CiConstant constant = map.get(value);
        if (constant == null) {
            synchronized (map) {
                constant = map.get(value);
                if (constant == null) {
                    constant = new CiConstant(kind, value);
                    map.put(value, constant);
                }
            }
        }
        return constant;
    }
    
    /**
     * Checks whether this constant is non-null.
     * @return {@code true} if this constant is a primitive, or an object constant that is not null
     */
    public boolean isNonNull() {
        return value != null;
    }

    /**
     * Checks whether this constant is null.
     * @return {@code true} if this constant is the null constant
     */
    public boolean isNull() {
        return kind == CiKind.Object && value == null;
    }

    @Override
    public String name() {
        return "const[" + kind.format(value) + "]";
    }

    /**
     * Gets this constant's value as a string.
     *
     * @return this constant's value as a string
     */
    public String valueString() {
        return (value == null) ? "null" : value.toString();
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

    private boolean valueEqual(CiConstant other) {
        // must have equivalent tags to be equal
        if (kind != other.kind) {
            return false;
        }
        // use == for object references and .equals() for boxed types
        if (value == other.value) {
            return true;
        } else if (!kind.isObject() && value != null && value.equals(other.value)) {
            return true;
        }
        return false;
    }
    
    private static boolean valuesEqual(Object o1, Object o2, CiKind kind) {
        // use == for object references and .equals() for boxed types
        if (o1 == o2) {
            return true;
        } else if (!kind.isObject() && o1.equals(o2)) {
            return true;
        }
        return false;
    }

    /**
     * Converts this constant to a primitive int.
     * @return the int value of this constant
     */
    public int asInt() {
        if (kind != CiKind.Object) {
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
        throw new Error("Constant is not int: " + this);
    }

    /**
     * Converts this constant to a primitive boolean.
     * @return the boolean value of this constant
     */
    public boolean asBoolean() {
    	if (kind == CiKind.Boolean) {
    		return (Boolean) value;
    	}

        throw new Error("Constant is not boolean: " + this);
    }

    /**
     * Converts this constant to a primitive long.
     * @return the long value of this constant
     */
    public long asLong() {
        if (kind != CiKind.Object) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            if (value instanceof Character) {
                return (Character) value;
            }
            if (value instanceof Boolean) {
                return (Boolean) value ? 1 : 0; // note that we allow Boolean values to be used as ints
            }
        }
        throw new Error("Constant is not long: " + this);
    }

    /**
     * Converts this constant to a primitive float.
     * @return the float value of this constant
     */
    public float asFloat() {
        if (kind != CiKind.Object) {
            if (value instanceof Float) {
                return (Float) value;
            }
        }
        throw new Error("Constant is not float: " + this);
    }

    /**
     * Converts this constant to a primitive double.
     * @return the double value of this constant
     */
    public double asDouble() {
        if (kind != CiKind.Object) {
            if (value instanceof Double) {
                return (Double) value;
            }
            if (value instanceof Float) {
                return (Float) value;
            }
        }
        throw new Error("Constant is not double: " + this);
    }

    /**
     * Converts this constant to the object reference it represents.
     * @return the object which this constant represents
     */
    public Object asObject() {
        if (kind == CiKind.Object) {
            return value;
        }
        throw new Error("Constant is not object: " + this);
    }

    /**
     * Converts this constant to the jsr reference it represents.
     * @return the object which this constant represents
     */
    public int asJsr() {
        if (kind == CiKind.Jsr) {
            return (Integer) value;
        }
        throw new Error("Constant is not jsr: " + this);
    }

    /**
     * Computes the hashcode of this constant.
     * @return a suitable hashcode for this constant
     */
    @Override
    public int hashCode() {
        if (kind == CiKind.Object) {
            return System.identityHashCode(value);
        }
        return value.hashCode();
    }

    /**
     * Checks whether this constant equals another object. This is only
     * true if the other object is a constant and has the same value.
     * @param o the object to compare equality
     * @return {@code true} if this constant is equivalent to the specified object
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof CiConstant && valueEqual((CiConstant) o);
    }

    /**
     * Checks whether this constant is the default value for its type.
     * @return {@code true} if the value is the default value for its type; {@code false} otherwise
     */
    public boolean isDefaultValue() {
        switch (kind) {
            case Int: return asInt() == 0;
            case Long: return asLong() == 0;
            case Float: return asFloat() == 0.0f; // TODO: be careful about -0.0
            case Double: return asDouble() == 0.0d; // TODO: be careful about -0.0
            case Object: return asObject() == null;
        }
        return false;
    }

    /**
     * Creates a boxed double constant.
     * @param d the double value to box
     * @return a boxed copy of {@code value}
     */
    public static CiConstant forDouble(double d) {
        return get(CiKind.Double, d);
    }

    /**
     * Creates a boxed float constant.
     * @param f the float value to box
     * @return a boxed copy of {@code value}
     */
    public static CiConstant forFloat(float f) {
        return get(CiKind.Float, f);
    }

    /**
     * Creates a boxed long constant.
     * @param i the long value to box
     * @return a boxed copy of {@code value}
     */
    public static CiConstant forLong(long i) {
        return i == 0 ? LONG_0 : i == 1 ? LONG_1 : get(CiKind.Long, i);
    }

    /**
     * Creates a boxed integer constant.
     * @param i the integer value to box
     * @return a boxed copy of {@code value}
     */
    public static CiConstant forInt(int i) {
        switch (i) {
            case -1 : return INT_MINUS_1;
            case 0  : return INT_0;
            case 1  : return INT_1;
            case 2  : return INT_2;
            case 3  : return INT_3;
            case 4  : return INT_4;
            case 5  : return INT_5;
        }
        return get(CiKind.Int, i);
    }

    /**
     * Creates a boxed byte constant.
     * @param i the byte value to box
     * @return a boxed copy of {@code value}
     */
    public static CiConstant forByte(byte i) {
        return get(CiKind.Byte, i);
    }

    /**
     * Creates a boxed boolean constant.
     * @param i the boolean value to box
     * @return a boxed copy of {@code value}
     */
    public static CiConstant forBoolean(boolean i) {
        return get(CiKind.Boolean, i);
    }

    /**
     * Creates a boxed char constant.
     * @param i the char value to box
     * @return a boxed copy of {@code value}
     */
    public static CiConstant forChar(char i) {
        return get(CiKind.Char, i);
    }

    /**
     * Creates a boxed short constant.
     * @param i the short value to box
     * @return a boxed copy of {@code value}
     */
    public static CiConstant forShort(short i) {
        return get(CiKind.Short, i);
    }

    /**
     * Creates a boxed address (jsr/ret address) constant.
     * @param i the address value to box
     * @return a boxed copy of {@code value}
     */
    public static CiConstant forJsr(int i) {
        return get(CiKind.Jsr, i);
    }

    /**
     * Creates a boxed word constant.
     * @param i the number representing the word's value, either an {@link Integer} or a {@link Long}
     * @return a boxed copy of {@code value}
     */
    public static CiConstant forWord(Number i) {
        if (i instanceof Integer || i instanceof Long) {
            return get(CiKind.Word, i); // only Integer and Long are allowed
        }
        throw new IllegalArgumentException("cannot create word constant for object of type " + i.getClass());
    }

    /**
     * Creates a boxed object constant.
     * @param o the object value to box
     * @return a boxed copy of {@code value}
     */
    public static CiConstant forObject(Object o) {
        if (o == null) {
            return NULL_OBJECT;
        }
        return get(CiKind.Object, o);
    }
}
