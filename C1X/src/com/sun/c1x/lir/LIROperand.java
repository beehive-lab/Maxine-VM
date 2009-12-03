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

/**
 * The <code>LIROperand</code> class represents an operand, either
 * a constant, an address calculation, a register, or a stack slot.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */

public class LIROperand {
    public final CiKind kind;
    public static final LIRLocation IllegalLocation = new LIRLocation(CiKind.Illegal, CiRegister.None);

    protected LIROperand(CiKind kind) {
        this.kind = kind;
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

    @Override
    public String toString() {
        if (isIllegal(this)) {
            return "illegal";
        }

        final StringBuffer out = new StringBuffer();
        out.append("[");
        if (isSingleStack()) {
            out.append("stack:").append(singleStackIndex());
        } else if (isDoubleStack()) {
            out.append("dblStack:").append(doubleStackIndex());
        } else if (isVariable()) {
            out.append("V").append(vregNumber());
        } else if (isSingleCpu()) {
            out.append(asRegister().name);
        } else if (isDoubleCpu()) {
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

    public boolean isRegister() {
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

    public boolean isVariable() {
        return false;
    }

    public boolean isFixedCpu() {
        return false;
    }

    public boolean isSingleCpu() {
        return false;
    }

    public boolean isDoubleCpu() {
        return false;
    }

    public boolean isSingleXmm() {
        return false;
    }

    public boolean isDoubleXmm() {
        return false;
    }

    public boolean isVirtualRegister() {
        return false;
    }

    public int stackIndex() {
        throw new Error(getClass().getSimpleName() + " does not have a stackIndex");
    }

    public int singleStackIndex() {
        throw new Error(getClass().getSimpleName() + " does not have a singleStackIndex");
    }

    public int doubleStackIndex() {
        throw new Error(getClass().getSimpleName() + " does not have a doubleStackIndex");
    }

    public int cpuRegNumber() {
        throw new Error(getClass().getSimpleName() + " does not have a cpuRegNumber");
    }

    public int cpuRegNumberLow() {
        throw new Error(getClass().getSimpleName() + " does not have a cpuRegNumberLow");
    }

    public int cpuRegNumberHigh() {
        throw new Error(getClass().getSimpleName() + " does not have a cpuRegNumberHigh");
    }

    public int vregNumber() {
        throw new Error(getClass().getSimpleName() + " does not have a vregNumber");
    }

    public CiRegister asRegister() {
        if (isIllegal(this)) {
            return CiRegister.None;
        }
        throw new Error(getClass().getSimpleName() + " cannot be a register");
    }

    public CiRegister asRegisterLow() {
        throw new Error(getClass().getSimpleName() + " cannot be a register");
    }

    public CiRegister asRegisterHigh() {
        throw new Error(getClass().getSimpleName() + " cannot be a register");
    }

    public CiRegister asPointerRegister(CiArchitecture architecture) {
        if (architecture.is64bit() && isDoubleCpu()) {
            assert asRegisterLow() == asRegisterHigh() : "should be a single register";
            return asRegisterLow();
        }
        return asRegister();
    }
}
