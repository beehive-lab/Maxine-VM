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
public class LIRConstant extends LIROperand {

    public final ConstType value;

    /**
     * Create a LIRConstant from a ConstType object.
     * @param value the value
     */
    public LIRConstant(ConstType value) {
        super(value.basicType);
        this.value = value;
    }

    /**
     * Gets the basic type of this constant, which can be used to convert it
     * to the appropriate primitive value or object.
     *
     * @return the basic type of this constant
     */
    @Override
    public BasicType type() {
        return value.basicType;
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
    @Override
    public int asInt() {
        assertType(this, BasicType.Int);
        return value.asInt();
    }

    /**
     * Converts this constant to a long.
     *
     * @return the long value of the constant, if it is a long
     */
    @Override
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
        assert c.type() == t : "constant has wrong type";
        return c;
    }

    /**
     * Converts a float constant into an int constant.
     *
     * @return the int value of the constant.
     */
    public int asIntBits() {
        if (this.isFloat()) {
            return Float.floatToIntBits(this.asFloat());
        } else if (this.type() == BasicType.Int) {
            return this.asInt();
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    /**
     * Get the low order 32 bits of a double constant as an int.
     *
     * @return the int value of the low order 32 bits of a double constant
     */
    public int asIntLoBits() {
        // TODO: floats, longs
        if (value.isDouble()) {
            return (int) Double.doubleToLongBits(value.asDouble());
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
        // TODO: floats, longs
        if (value.isDouble()) {
            return (int) (Double.doubleToLongBits(value.asDouble()) >> 32);
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
        // TODO: floats, longs
      if (value.isDouble()) {
        return Double.doubleToLongBits(value.asDouble());
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

    public boolean isOneFloat() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isZeroFloat() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isZeroDouble() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isOneDouble() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public float asJfloat() {
        return this.asFloat();
    }

    @Override
    public double asJdouble() {
        return this.asDouble();
    }

    @Override
    public Object asJobject() {
        return this.asObject();
    }

    @Override
    public String valueToString() {
        switch (type()) {
            case Int:
                return String.format("int:%d", asInt());
            case Long:
                return String.format("lng:%d", asLong());
            case Float:
                return String.format("flt:%f", asJfloat());
            case Double:
                return String.format("dbl:%f", asJdouble());
            case Object:
                return String.format("obj:%s", asJobject());
            default:
                return String.format("%3d:0x%x", type(), asJdouble());
        }
    }
}
