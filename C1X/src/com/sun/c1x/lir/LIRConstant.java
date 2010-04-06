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

import com.sun.c1x.ci.*;
import com.sun.c1x.util.*;

/**
 * The {@code LIRConstant} class definition.
 *
 * @author Marcelo Cintra
 */
public final class LIRConstant extends LIROperand {

    public final CiConstant value;

    /**
     * Create a LIRConstant from a ConstType object.
     * @param value the value
     */
    public LIRConstant(CiConstant value) {
        super(value.kind);
        this.value = value;
    }

    /**
     * Converts this constant to an int.
     *
     * @return the int value of the constant, if it is an int
     */
    public int asInt() {
        return value.asInt();
    }

    public int asChar() {
        return value.asInt();
    }

    public int asShort() {
        return value.asInt();
    }

    /**
     * Converts this constant to an boolean.
     *
     * @return the boolean value of the constant, if it is an boolean
     */
    public boolean asBoolean() {
        return value.asBoolean();
    }

    /**
     * Converts this constant to a long.
     *
     * @return the long value of the constant, if it is a long
     */
    public long asLong() {
        assertType(this, CiKind.Long);
        return value.asLong();
    }

    /**
     * Converts this constant to a word.
     *
     * @return the long value of the constant, if it is a long
     */
    public long asWord() {
        assertType(this, CiKind.Word);
        return value.asLong();
    }

    /**
     * Converts this constant to a Float.
     *
     * @return the float value of the constant, if it is a float
     */
    public float asFloat() {
        assertType(this, CiKind.Float);
        return value.asFloat();
    }

    /**
     * Converts this constant to a Double.
     *
     * @return the double value of the constant, if it is a double
     */
    public double asDouble() {
        assertType(this, CiKind.Double);
        return value.asDouble();
    }

    /**
     * Gets the object referred to by this constant, if it is an object reference.
     *
     * @return a reference to the object if this is a reference to an object
     */
    public Object asObject() {
        assertType(this, CiKind.Object);
        return value.asObject();
    }

    /**
     * Gets the jsr referred to by this constant.
     *
     * @return the jsr
     */
    public int asJsr() {
        assertType(this, CiKind.Jsr);
        return value.asJsr();
    }

    /**
     * Gets the low order 32 bits of this constant; The constant type must be long.
     *
     * @return the lower order 32 bits of this constant
     */
    public int asIntLo() {
        assertType(this, CiKind.Long);
        return (int) value.asLong();
    }

    /**
     * Gets the high order 32 bits of a long constant; The constant must be of type long.
     *
     * @return the high order 32 bits of this constant
     */
    public int asIntHi() {
        assertType(this, CiKind.Long);
        return (int) (value.asLong() >> 32);
    }

    /**
     * Asserts that a given constant c if of type t.
     *
     * @return the reference to the input constant if succeeded.
     */
    public static LIRConstant assertType(LIRConstant c, CiKind t) {
        assert c.kind == t || (c.kind.isWord() && (t.isLong() || t.isInt())) : "constant has wrong type: " + c + ", should be " + t;
        return c;
    }

    /**
     * Converts a float constant into an int constant.
     *
     * @return the int value of the constant.
     */
    public int asIntBits() {
        switch (kind) {
            case Boolean:
            case Byte:
            case Short:
            case Char:
            case Int:
                return this.asInt();
            case Float:
                return Float.floatToIntBits(this.asFloat());
            default:
                throw Util.shouldNotReachHere();
        }
    }

    /**
     * Get the low order 32 bits of a double constant as an int.
     *
     * @return the int value of the low order 32 bits of a double constant
     */
    public int asIntLoBits() {
        switch (kind) {
            case Boolean:
            case Byte:
            case Short:
            case Char:
            case Int:
                return this.asInt();
            case Float:
                return Float.floatToIntBits(this.asFloat());
            case Double:
                return (int) Double.doubleToLongBits(this.asDouble());
            case Long:
                return (int) this.asLong();
            default:
                throw Util.shouldNotReachHere();
        }
    }

    /**
     * Get the high order 32 bits of a double constant as an int.
     *
     * @return the int value of the high order 32 bits of a double constant
     */
    public int asIntHiBits() {
        switch (kind) {
            case Boolean:
            case Byte:
            case Short:
            case Char:
            case Int:
            case Float:
                return 0;
            case Double:
                return (int) (Double.doubleToLongBits(this.asDouble()) >> 32);
            case Long:
                return (int) (this.asLong() >> 32);
            default:
                throw Util.shouldNotReachHere();
        }
    }

    /**
     * Converts a double constant to a long constant.
     *
     * @return the long value of the constant, if it is a double constant.
     */
    public long asLongBits() {
        switch (kind) {
            case Boolean:
            case Byte:
            case Short:
            case Char:
            case Int:
                return asInt();
            case Float:
                return Float.floatToIntBits(this.asFloat());
            case Double:
                return Double.doubleToLongBits(this.asDouble());
            case Long:
                return asLong();
            case Word:
                return asWord();
            default:
                throw Util.shouldNotReachHere();
        }
    }

    @Override
    public String toString() {
        switch (kind) {
            case Boolean:
                return String.format("boolean:%b", asBoolean());
            case Char:
                return String.format("char:%d", asChar());
            case Short:
                return String.format("short:%d", asShort());
            case Int:
                return String.format("int:%d", asInt());
            case Long:
                return String.format("lng:%d", asLong());
            case Float:
                return String.format("flt:%f", this.asFloat());
            case Double:
                return String.format("dbl:%f", this.asDouble());
            case Object:
            {
                Object o = this.asObject();
                if (o == null) {
                    return "obj:null";
                } else {
                    return String.format("obj:%s:%d", o.getClass().getName(), o.hashCode());
                }
            }
            case Jsr:
                return String.format("jsr:%d", this.asJsr());
            case Word:
                return String.format("word:%d", this.asWord());
            default:
                return String.format("%3d:0x%x", kind, this.asDouble());
        }
    }
}
