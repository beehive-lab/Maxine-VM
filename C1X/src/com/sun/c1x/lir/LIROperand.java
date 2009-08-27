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

import com.sun.c1x.asm.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.util.*;

/**
 * The <code>LIROperand</code> class represents an operand, either
 * a constant, an address calculation, a register, or a stack slot.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */

public abstract class LIROperand {

    /**
     * The illegal operand singleton instance.
     */
    public static final LIROperand ILLEGAL = new LIRIllegal();

    private static final class LIRIllegal extends LIROperand {
        private LIRIllegal() {
            super(CiKind.Illegal);
        }
        @Override
        public String toString() {
            return "illegal";
        }
    }

    /**
     * The basic type of this operand.
     */
    public final CiKind basicType;

    protected LIROperand(CiKind basicType) {
        this.basicType = basicType;
    }

    public boolean isValid() {
        return this != ILLEGAL;
    }

    public boolean isIllegal() {
        return this == ILLEGAL;
    }

    public static LIROperand illegalOpr() {
        return ILLEGAL;
    }

    public CiKind type() {
        return basicType;
    }

    // checks whether types are same
    boolean isSameType(LIROperand opr) {
        return basicType == opr.basicType;
    }

    public boolean isSameRegister(LIROperand opr) {
        throw Util.shouldNotReachHere();
    }

    public boolean isRegister() {
        return isCpuRegister() || isXmmRegister();
    }

    public boolean isVirtual() {
        return isVirtualCpu();
    }

    public boolean isConstant() {
        return this instanceof LIRConstant && !isIllegal();
    }

    public boolean isAddress() {
        return this instanceof LIRAddress;
    }

    public void print(LogStream out) {
        out.print(this.toString());
    }

    public String valueToString() {
        throw Util.shouldNotReachHere();
    }

    @Override
    public String toString() {

        if (isIllegal()) {
            return "";
        }

        final StringBuffer out = new StringBuffer();
        out.append("[");
        if (isPointer()) {
            out.append(valueToString());
        } else if (isSingleStack()) {
            out.append("stack:" + singleStackIx());
        } else if (isDoubleStack()) {
            out.append("dblStack:" + doubleStackIx());
        } else if (isVirtual()) {
            out.append("R" + vregNumber());
        } else if (isSingleCpu()) {
            out.append(asRegister().name);
        } else if (isDoubleCpu()) {
            out.append(asRegisterHi().name);
            out.append(asRegisterLo().name);
        } else if (isSingleXmm()) {
            out.append(asRegister().name);
        } else if (isDoubleXmm()) {
            out.append(asRegister().name);
        } else if (isIllegal()) {
            out.append("-");
        } else {
            out.append("Unknown Operand");
        }
        if (!isIllegal()) {
            out.append(String.format("|%c", this.type().basicChar));
        }
        out.append("]");
        return out.toString();
    }

    public boolean isPointer() {
        // TODO to be removed
        return !(this instanceof LIRLocation);
    }

    public boolean isFloatKind() {
        return basicType == CiKind.Float || basicType == CiKind.Double;
    }

    public boolean isOop() {
        return basicType == CiKind.Object;
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

    public boolean isCpuRegister() {
        return false;
    }

    public boolean isVirtualCpu() {
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

    public boolean isXmmRegister() {
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

    public boolean isOopRegister() {
        return false;
    }

    public boolean isLastUse() {
        return false;
    }

    public boolean isFpuStackOffset() {
        return false;
    }

    public LIROperand makeLastUse() {
        throw new Error(getClass().getSimpleName() + " does not have a makeLastUse");
    }

    public LIROperand makeFpuStackOffset() {
        throw new Error(getClass().getSimpleName() + " does not have a makeFpuStackOffset");
    }

    public int stackIx() {
        throw new Error(getClass().getSimpleName() + " does not have a stackIx");
    }

    public int singleStackIx() {
        throw new Error(getClass().getSimpleName() + " does not have a singleStackIx");
    }

    public int doubleStackIx() {
        throw new Error(getClass().getSimpleName() + " does not have a doubleStackIx");
    }

    public int cpuRegnr() {
        throw new Error(getClass().getSimpleName() + " does not have a cpuRegnr");
    }

    public int cpuRegnrLo() {
        throw new Error(getClass().getSimpleName() + " does not have a cpuRegnrLo");
    }

    public int cpuRegnrHi() {
        throw new Error(getClass().getSimpleName() + " does not have a cpuRegnrHi");
    }

    public int vregNumber() {
        throw new Error(getClass().getSimpleName() + " does not have a vregNumber");
    }

    public LIRConstant asConstantPtr() {
        if (this instanceof LIRConstant) {
            return (LIRConstant) this;
        } else {
            return null;
        }
    }

    public LIRAddress asAddressPtr() {
        if (this instanceof LIRAddress) {
            return (LIRAddress) this;
        } else {
            return null;
        }
    }

    public CiRegister asRegister() {
        if (this == LIROperand.ILLEGAL) {
            return CiRegister.noreg;
        }

        throw Util.shouldNotReachHere();
    }

    public CiRegister asRegisterLo() {
        throw Util.shouldNotReachHere();
    }

    public CiRegister asRegisterHi() {
        throw Util.shouldNotReachHere();
    }

    public CiRegister asPointerRegister(CiArchitecture architecture) {
        if (architecture.is64bit() && isDoubleCpu()) {
            assert asRegisterLo() == asRegisterHi() : "should be a single register";
            return asRegisterLo();
        }
        return asRegister();
    }

    public int asInt() {
        throw Util.unimplemented();
    }

    public long asLong() {
        throw Util.unimplemented();
    }

    boolean isOopPointer() {
        return type() == CiKind.Object;
    }

    boolean isFloat() {
        CiKind t = type();
        return (t == CiKind.Float) || (t == CiKind.Double);
    }

    public LIRAddress asAddress() {
        if (this instanceof LIRAddress) {
            return (LIRAddress) this;
        }
        return null;
    }

    public CiKind typeRegister() {
        assert this.isRegister();

        if (type() == CiKind.Boolean || type() == CiKind.Char || type() == CiKind.Byte) {
            return CiKind.Int;
        }

        return type();
    }

    public void assignPhysicalRegister(LIROperand colorLirOpr) {
        Util.shouldNotReachHere();
    }

    public RegisterOrConstant asRegisterOrConstant() {
        if (isRegister()) {
            return new RegisterOrConstant(asRegister());
        } else if (this.isConstant()) {
            final LIRConstant c = (LIRConstant) this;
            if (c.value.basicType == CiKind.Int) {
                return new RegisterOrConstant(c.value.asInt());
            } else if (c.value.basicType == CiKind.Object) {
                return new RegisterOrConstant(c.value.asObject());
            } else {
                throw Util.shouldNotReachHere();
            }
        } else {
            throw Util.shouldNotReachHere();
        }
    }
}
