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

import com.sun.c1x.debug.LogStream;
import com.sun.c1x.target.Architecture;
import com.sun.c1x.target.Register;
import com.sun.c1x.util.Util;
import com.sun.c1x.value.BasicType;
import com.sun.c1x.value.ConstType;

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
    public static final LIROperand ILLEGAL = new LIRConstant(new ConstType(BasicType.Illegal, 0));

    /**
     * The basic type of this operand.
     */
    public final BasicType basicType;

    protected LIROperand(BasicType basicType) {
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

    public BasicType type() {
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
        return basicType == BasicType.Float || basicType == BasicType.Double;
    }

    public boolean isOop() {
        return basicType == BasicType.Object;
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
        throw Util.shouldNotReachHere();
    }

    public LIROperand makeFpuStackOffset() {
        throw Util.shouldNotReachHere();
    }

    public int singleStackIx() {
        throw Util.shouldNotReachHere();
    }

    public int doubleStackIx() {
        throw Util.shouldNotReachHere();
    }

    public int cpuRegnr() {
        throw Util.shouldNotReachHere();
    }

    public int cpuRegnrLo() {
        throw Util.shouldNotReachHere();
    }

    public int cpuRegnrHi() {
        throw Util.shouldNotReachHere();
    }

    public int vregNumber() {
        throw Util.shouldNotReachHere();
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

    public Register asRegister() {
        throw Util.shouldNotReachHere();
    }

    public Register asRegisterLo() {
        throw Util.shouldNotReachHere();
    }

    public Register asRegisterHi() {
        throw Util.shouldNotReachHere();
    }

    public Register asPointerRegister(Architecture architecture) {
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
        return type() == BasicType.Object;
    }

    boolean isFloat() {
        BasicType t = type();
        return (t == BasicType.Float) || (t == BasicType.Double);
    }

    public LIRAddress asAddress() {
        if (this instanceof LIRAddress) {
            return (LIRAddress) this;
        }
        return null;
    }

    public BasicType typeRegister() {
        assert this.isRegister();
        return type();
    }

    public void assignPhysicalRegister(LIROperand colorLirOpr) {
        Util.shouldNotReachHere();
        // TODO Auto-generated method stub
    }
}
