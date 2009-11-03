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
package com.sun.c1x.ir;

import com.sun.c1x.ci.*;

/**
 * The <code>Constant</code> instruction represents a constant such as an integer value,
 * long, float, object reference, address, etc.
 *
 * @author Ben L. Titzer
 */
public class Constant extends Instruction {

    public final CiConstant value;

    /**
     * Constructs a new instruction representing the specified constant.
     * @param value the constant
     */
    public Constant(CiConstant value) {
        super(value.basicType.stackType());
        this.value = value;
        initFlag(Value.Flag.NonNull, value.isNonNull());
    }

    /**
     * Implements half of the visitor pattern for this instruction.
     * @param v the visitor to accept
     */
    @Override
    public void accept(ValueVisitor v) {
        v.visitConstant(this);
    }

    /**
     * Utility method to create an instruction for a double constant.
     * @param d the double value for which to create the instruction
     * @return an instruction representing the double
     */
    public static Constant forDouble(double d) {
        return new Constant(CiConstant.forDouble(d));
    }

    /**
     * Utility method to create an instruction for a float constant.
     * @param f the float value for which to create the instruction
     * @return an instruction representing the float
     */
    public static Constant forFloat(float f) {
        return new Constant(CiConstant.forFloat(f));
    }

    /**
     * Utility method to create an instruction for an long constant.
     * @param i the long value for which to create the instruction
     * @return an instruction representing the long
     */
    public static Constant forLong(long i) {
        return new Constant(CiConstant.forLong(i));
    }

    /**
     * Utility method to create an instruction for an integer constant.
     * @param i the integer value for which to create the instruction
     * @return an instruction representing the integer
     */
    public static Constant forInt(int i) {
        return new Constant(CiConstant.forInt(i));
    }

    /**
     * Utility method to create an instruction for a boolean constant.
     * @param i the boolean value for which to create the instruction
     * @return an instruction representing the boolean
     */
    public static Constant forBoolean(boolean i) {
        return new Constant(CiConstant.forBoolean(i));
    }

    /**
     * Utility method to create an instruction for an address (jsr/ret address) constant.
     * @param i the address value for which to create the instruction
     * @return an instruction representing the address
     */
    public static Constant forJsr(int i) {
        return new Constant(CiConstant.forJsr(i));
    }

    /**
     * Utility method to create an instruction for an object constant.
     * @param o the object value for which to create the instruction
     * @return an instruction representing the object
     */
    public static Constant forObject(Object o) {
        return new Constant(CiConstant.forObject(o));
    }

    @Override
    public String toString() {
        return super.toString() + "(" + value + ")";
    }

    @Override
    public int valueNumber() {
        return 0x50000000 | value.hashCode();
    }

    @Override
    public boolean valueEqual(Instruction i) {
        return i instanceof Constant && ((Constant) i).value.equivalent(this.value);
    }

}
