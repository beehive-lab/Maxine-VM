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
package com.sun.c1x.lir;

import com.sun.c1x.util.*;
import com.sun.c1x.value.*;


/**
 * The <code>LIRConstant</code> class definition.
 *
 * @author Marcelo Cintra
 */
public class LIRConstant extends LIROperandPtr {

    ConstType value;

    /**
     * Create a new integer constant.
     *
     * @param i integer value for the new constant
     */
    public LIRConstant(int i) {
        value = ConstType.forInt(i);
    }

    /**
     * Create a new long constant.
     *
     * @param l long value for the new constant
     */
    public LIRConstant(long l) {
        value = ConstType.forLong(l);
    }

    /**
     * Create a new float constant.
     *
     * @param f float value for the new constant
     */
    public LIRConstant(float f) {
        value = ConstType.forFloat(f);
    }

    /**
     * Create a new double constant.
     *
     * @param d double value for the new constant
     */
    public LIRConstant(double d) {
        value = ConstType.forDouble(d);
    }

    /**
     * Create a new object reference constant.
     *
     * @param o object reference for the new constant
     */
    public LIRConstant(Object o) {
        value = ConstType.forObject(o);
    }

    /**
     * Gets the basic type of this constant, which can be used to convert it
     * to the appropriate primitive value or object.
     *
     * @return the basic type of this constant
     */
    @Override
    public BasicType type() {
        return value.basicType();
    }

    /**
     * Returns the reference for this constant.
     *
     * @return the reference(this) for this constant
     */
    @Override
    public LIRConstant asConstant() {
        return this;
    }

    /**
     * Converts this constant to an int.
     *
     * @return the int value of the constant, if it is an int
     */
    public int asInt() {
        assertType(this, BasicType.Int);
        return value.asInt();
    }

    /**
     * Converts this constant to a long.
     *
     * @return the long value of the constant, if it is a long
     */
    public long asLong() {
        assertType(this, BasicType.Long);
        return value.asLong();
    }

    /**
     * Converts this constant to a Float.
     *
     * @return the float value of the constant, if it is a float
     */
    public float asFloat() {
        assertType(this, BasicType.Float);
        return value.asFloat();
    }

    /**
     * Converts this constant to a Double.
     *
     * @return the double value of the constant, if it is a double
     */
    public double asDouble() {
        assertType(this, BasicType.Double);
        return value.asDouble();
    }

    /**
     * Gets the object referred to by this constant, if it is an object reference.
     *
     * @return a reference to the object if this is a reference to an object
     */
    public Object asObject() {
        assertType(this, BasicType.Object);
        return value.asObject();
    }

    /**
     * Gets the low order 32 bits of this constant; The constant type must be long.
     *
     * @return the lower order 32 bits of this constant
     */
    public int asIntLo() {
        assertType(this, BasicType.Long);
        return (int) value.asLong();
    }

    /**
     * Gets the high order 32 bits of a long constant; The constant must be of type long.
     *
     * @return the high order 32 bits of this constant
     */
    public int asIntHi() {
        assertType(this, BasicType.Long);
        return (int) value.asLong() >> 32;
    }

    /**
     * Asserts that a given constant c if of type t.
     *
     * @return the reference to the input constant if succeeded.
     */
    public static LIRConstant assertType(LIRConstant c, BasicType t) {
        assert c.type() == t : "type check failed on LIRConstant.java";
        return c;
    }

    /**
     * Asserts that a given constant c if of type t1 or t2.
     *
     * @return the reference to the input constant if succeeded.
     */
    public static LIRConstant assertType(LIRConstant c, BasicType t1, BasicType t2) {
        assert c.type() == t1 || c.type() == t2 : "type check failed on LIRConstant.java";
        return c;
    }

    /**
     * Converts a float constant to an int constant.
     *
     * @return the int value of the constant.
     */
    public int asIntBits() {
        assertType(this, BasicType.Float, BasicType.Int);
        return value.asInt();
    }

    /**
     * Get the low order 32 bits of a double constant as an int.
     *
     * @return the int value of the low order 32 bits of a double constant
     */
    public int asIntLoBits() {
        if (type() == BasicType.Double) {
            return (int) (long) value.asDouble();
        } else {
            return value.asInt();
        }
    }

    /**
     * Get the high order 32 bits of a double constant as an int.
     *
     * @return the int value of the high order 32 bits of a double constant
     */
    public int asIntHiBits() {
        if (type() == BasicType.Double) {
            return (int) ((long) value.asDouble() >> 32);
        } else {
            return asIntHi();
        }
    }

    /**
     * Converts a double constant to a long constant.
     *
     * @return the long value of the constant, if it is a double constant.
     */
    public long asLongBits() {
      if (type() == BasicType.Double) {
        return (long) value.asDouble();
      } else {
        return asLong();
      }
    }

    /**
     * Checks if the float constant has the value 0.0f.
     *
     * @return true if the constant is zero, false otherwise.
     */
    public boolean isFloat0() {
        return asFloat() == 0.0f;
      }

    /**
     * Checks if the float constant has the value 1.0f.
     *
     * @return true if the constant is 1.0, false otherwise.
     */
    public boolean isFloat1() {
        return asFloat() == 1.0f;
      }

    /**
     * Checks if the double constant has the value 0.0.
     *
     * @return true if the constant is zero, false otherwise.
     */
    public boolean isDouble0() {
        return asDouble() == 0.0;
    }

    /**
     * Checks if the double constant has the value 1.0.
     *
     * @return true if the constant is zero, false otherwise.
     */
    public boolean isDouble1() {
        return asDouble() == 1.0;
      }

    /**
     * Prints the constant value to the LogStream.
     *
     * @param out the ouput LogStream
     */
    @Override
    public void printValueOn(LogStream out) {
        switch(type()) {
            case Int:
                out.print("int:" + asInt());
                break;
            case Long:
                out.print("long:" + asLong());
                break;
            case Float:
                out.print("float:" + asFloat());
                break;
            case Double:
                out.print("double:" + asDouble());
                break;
            case Object:
                out.print("obectj:0x" + asObject());
                break;
            default:
                out.print(type() + ":" + asDouble());
        }
    }
}
