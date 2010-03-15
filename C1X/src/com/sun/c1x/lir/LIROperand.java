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
import com.sun.c1x.ci.CiRegister.*;
import com.sun.c1x.ir.Value;
import com.sun.c1x.C1XMetrics;

/**
 * An LIR operand is either a {@linkplain #isConstant(LIROperand) constant}, an {@linkplain #isAddress(LIROperand) address},
 * a {@linkplain #isRegister() register}, a {@linkplain #isVariable() variable} or a {@linkplain #isStack() stack slot}.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 * @author Doug Simon
 */
public class LIROperand {
    public final CiKind kind;
    public static final LIRLocation IllegalLocation = new LIRLocation(CiKind.Illegal, CiRegister.None);

    protected LIROperand(CiKind kind) {
        this.kind = kind;
    }

    @Override
    public String toString() {
        if (isIllegal(this)) {
            return "illegal";
        }

        final StringBuilder out = new StringBuilder();
        out.append("[");
        if (isSingleStack()) {
            out.append("stack:").append(singleStackIndex());
        } else if (isDoubleStack()) {
            out.append("dblStack:").append(doubleStackIndex());
        } else if (isVariable()) {
            out.append("V").append(variableNumber());
        } else if (isSingleRegister()) {
            out.append(asRegister().name);
        } else if (isDoubleRegister()) {
            out.append(asRegisterHigh().name);
            out.append(asRegisterLow().name);
        } else if (isSingleXmm()) {
            out.append(asRegister().name);
        } else if (isDoubleXmm()) {
            out.append(asRegister().name);
        } else {
            out.append("Unknown Operand");
        }
        if (isLegal(this)) {
            out.append(String.format("|%c", this.kind.typeChar));
        }
        out.append("]");
        return out.toString();
    }

    public boolean isVariableOrRegister() {
        return false;
    }

    public boolean isStack() {
        return false;
    }

    public boolean isSingleStack() {
        return false;
    }

    public boolean isDoubleStack() {
        return false;
    }

    /**
     * Determines if this is a variable that has yet to be allocated.
     */
    public boolean isVariable() {
        return false;
    }

    /**
     * Determines if this is a physical register.
     */
    public boolean isRegister() {
        return false;
    }

    /**
     * Determines if this is a general purpose register that can hold a 32-bit value.
     */
    public boolean isSingleRegister() {
        return false;
    }

    /**
     * Determines if this is a general purpose register that can hold a 64-bit value.
     */
    public boolean isDoubleRegister() {
        return false;
    }

    /**
     * Determines if this is an {@linkplain RegisterFlag#XMM XMM} register that can hold a 32-bit value.
     */
    public boolean isSingleXmm() {
        return false;
    }

    /**
     * Determines if this is an {@linkplain RegisterFlag#XMM XMM} register that can hold a 64-bit value.
     */
    public boolean isDoubleXmm() {
        return false;
    }

    protected Error illegalOperation(String operation) {
        throw new InternalError("Cannot call " + operation + " on " + this);
    }

    public int stackIndex() {
        throw illegalOperation("stackIndex()");
    }

    public int singleStackIndex() {
        throw illegalOperation("singleStackIndex()");
    }

    public int doubleStackIndex() {
        throw illegalOperation("doubleStackIndex()");
    }

    public int registerNumber() {
        throw illegalOperation("registerNumber()");
    }

    public int registerNumberLow() {
        throw illegalOperation("registerNumberLow()");
    }

    public int registerNumberHigh() {
        throw illegalOperation("registerNumberHigh()");
    }

    public int variableNumber() {
        throw illegalOperation("variableNumber()");
    }

    public CiRegister asRegister() {
        if (isIllegal(this)) {
            return CiRegister.None;
        }
        throw illegalOperation("asRegister()");
    }

    public CiRegister asRegisterLow() {
        throw illegalOperation("asRegisterLow()");
    }

    public CiRegister asRegisterHigh() {
        throw illegalOperation("asRegisterHigh()");
    }

    public CiRegister asPointerRegister(CiArchitecture architecture) {
        return asRegister();
    }

    /**
     * Creates a new LIR {@linkplain LIRLocation#isRegister() register} operand.
     *
     * @param reg the register
     * @param kind the kind of the register
     * @return a LIR register operand
     */
    public static LIRLocation forRegister(CiKind kind, CiRegister reg) {
        return new LIRLocation(kind, reg);
    }

    /**
     * Creates a new LIR {@linkplain LIRLocation#isRegister() register}-pair operand.
     *
     * @param reg1 the first register
     * @param reg2 the second register
     * @param kind the kind of the register
     * @return a LIR register-pair operand
     */
    public static LIRLocation forRegisters(CiKind kind, CiRegister reg1, CiRegister reg2) {
        return new LIRLocation(kind, reg1, reg2);
    }

    /**
     * Creates a new LIR {@linkplain LIRLocation#isVariable() variable} operand.
     *
     * @param index the index of the variable
     * @param kind the kind of the variable
     * @return a LIR variable operand
     */
    public static LIRLocation forVariable(int index, CiKind kind) {
        C1XMetrics.LIRVariables++;
        return new LIRLocation(kind, index);
    }

    /**
     * Creates a new LIR {@linkplain LIRLocation#isStack() stack slot} operand.
     *
     * @param index the index of the stack slot
     * @param kind the kind of the stack slot
     * @return a LIR stack slot operand
     */
    public static LIRLocation forStack(int index, CiKind type) {
        assert index >= 0;
        return new LIRLocation(type, -index - 1);
    }

    /**
     * Creates a new LIR {@linkplain LIRLocation#isConstant(LIROperand) constant} int operand.
     *
     * @param i an int constant
     * @return a LIR constant int operand
     */
    public static LIRConstant forInt(int i) {
        return new LIRConstant(CiConstant.forInt(i));
    }

    /**
     * Creates a new LIR {@linkplain LIRLocation#isConstant(LIROperand) constant} long operand.
     *
     * @param l a long constant
     * @return a LIR constant long operand
     */
    public static LIROperand forLong(long l) {
        return new LIRConstant(CiConstant.forLong(l));
    }

    /**
     * Creates a new LIR {@linkplain LIRLocation#isConstant(LIROperand) constant} float operand.
     *
     * @param f a float constant
     * @return a LIR constant float operand
     */
    public static LIROperand forFloat(float f) {
        return new LIRConstant(CiConstant.forFloat(f));
    }

    /**
     * Creates a new LIR {@linkplain LIRLocation#isConstant(LIROperand) constant} double operand.
     *
     * @param d a double constant
     * @return a LIR constant double operand
     */
    public static LIROperand forDouble(double d) {
        return new LIRConstant(CiConstant.forDouble(d));
    }

    /**
     * Creates a new LIR {@linkplain LIRLocation#isConstant(LIROperand) constant} object operand.
     *
     * @param o an object constant
     * @return a LIR constant object operand
     */
    public static LIROperand forObject(Object o) {
        return new LIRConstant(CiConstant.forObject(o));
    }

    /**
     * Creates a new LIR {@linkplain LIRLocation#isConstant(LIROperand) constant} operand
     * for a given value.
     *
     * @param value a {@linkplain Value#isConstant() constant} value
     * @return a LIR constant for {@code value}
     */
    public static LIROperand forConstant(Value value) {
        return new LIRConstant(value.asConstant());
    }

    /**
     * Creates a new LIR {@linkplain LIRLocation#isAddress(LIROperand) address} operand
     * for an address specified as a base register plus some displacement.
     *
     * @param register the base register for the address calculation
     * @param disp the displacement for the address calculation
     * @param kind the kind of the value located at the address
     * @return a LIR address operand
     */
    public static LIROperand forAddress(LIRLocation register, int disp, CiKind kind) {
        return new LIRAddress(register, disp, kind);
    }

    public static LIROperand forAddress(CiRegister rsp, int disp, CiKind kind) {
        return forAddress(new LIRLocation(CiKind.Int, rsp), disp, kind);
    }

    public static LIROperand forConstant(CiConstant value) {
        return new LIRConstant(value);
    }

    public static LIROperand forScratch(CiKind type, CiTarget target) {
        return forRegister(type, target.scratchRegister);
    }

    public static boolean isIllegal(LIROperand operand) {
        return operand == IllegalLocation;
    }

    public static boolean isLegal(LIROperand operand) {
        return operand != null && operand != IllegalLocation;
    }

    public static boolean isConstant(LIROperand operand) {
        return operand instanceof LIRConstant;
    }

    public static boolean isAddress(LIROperand operand) {
        return operand instanceof LIRAddress;
    }

    public static boolean isLocation(LIROperand operand) {
        return operand instanceof LIRLocation;
    }

}
