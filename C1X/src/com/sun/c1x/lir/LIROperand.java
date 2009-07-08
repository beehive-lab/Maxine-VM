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

import com.sun.c1x.target.*;
import com.sun.c1x.target.sparc.*;
import com.sun.c1x.target.x86.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;

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
    public static final LIROperand ILLEGAL = new LIRConstant(new ConstType(BasicType.Illegal, 0, false));

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

    public int lowerRegisterHalf() {
        throw Util.shouldNotReachHere();
    }

    public int higherRegisterHalf() {
        throw Util.shouldNotReachHere();
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

    boolean isSameRegister(LIROperand opr) {
        throw Util.shouldNotReachHere();
    }

    public boolean isRegister() {
        return isCpuRegister() || isFpuRegister();
    }

    public boolean isVirtual() {
        return isVirtualCpu() || isVirtualFpu();
    }

    public boolean isConstant() {
        return this instanceof LIRConstant;
    }

    public boolean isAddress() {
        return this instanceof LIRAddress;
    }

    public void print(LogStream out) {
        // TODO to be completed later
    }

    boolean isFloatKind() {
        return basicType == BasicType.Float || basicType == BasicType.Double;
    }

    boolean isOop() {
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

    public boolean isFpuRegister() {
        return false;
    }

    public boolean isVirtualFpu() {
        return false;
    }

    public boolean isFixedFpu() {
        return false;
    }

    public boolean isSingleFpu() {
        return false;
    }

    public boolean isDoubleFpu() {
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

    public int fpuRegnr() {
        throw Util.shouldNotReachHere();
    }

    public int fpuRegnrLo() {
        throw Util.shouldNotReachHere();
    }

    public int fpuRegnrHi() {
        throw Util.shouldNotReachHere();
    }

    public int xmmRegnr() {
        throw Util.shouldNotReachHere();
    }

    public int xmmRegnrLo() {
        throw Util.shouldNotReachHere();
    }

    public int xmmRegnrHi() {
        throw Util.shouldNotReachHere();
    }

    public int vregNumber() {
        throw Util.shouldNotReachHere();
    }

    public LIRConstant asConstantPtr() {
        return (LIRConstant) this;
    }

    public LIRAddress asAddressPtr() {
        return (LIRAddress) this;
    }

    public Register asRegister() {
        return FrameMap.cpuRnr2Reg(cpuRegnr());
    }

    public Register asRegisterLo() {
        return FrameMap.cpuRnr2Reg(cpuRegnrLo());
    }

    public Register asRegisterHi() {
        return FrameMap.cpuRnr2Reg(cpuRegnrHi());
    }

    public Register asPointerRegister(Architecture architecture) {
        if (architecture.is64bit() && isDoubleCpu()) {
            assert asRegisterLo() == asRegisterHi() : "should be a single register";
            return asRegisterLo();
        }
        return asRegister();
    }

    // X86 specific

    public XMMRegister asXmmFloatReg() {
        return FrameMap.nr2XmmReg(xmmRegnr());
    }

    public XMMRegister asXmmDoubleReg() {
        assert xmmRegnrLo() == xmmRegnrHi() : "assumed in calculation";
        return FrameMap.nr2XmmReg(xmmRegnrLo());
    }

    // for compatibility with RInfo
    public int fpu() {
        return lowerRegisterHalf();
    }

    // SPARC specific
    public FloatRegister asFloatReg() {
        return FrameMap.nr2FloatReg(fpuRegnr());
    }

    public FloatRegister asDoubleReg() {
        return FrameMap.nr2FloatReg(fpuRegnrHi());
    }

    public int asInt() {
        return asConstantPtr().asInt();
    }

    public long asLong() {
        return asConstantPtr().asLong();
    }

    public float asJfloat() {
        return asConstantPtr().asFloat();
    }

    public double asJdouble() {
        return asConstantPtr().asJdouble();
    }

    public Object asJobject() {
        return asConstantPtr().asObject();
    }

    boolean isOopPointer() {
        return type() == BasicType.Object;
    }

    boolean isFloat() {
        BasicType t = type();
        return (t == BasicType.Float) || (t == BasicType.Double);
    }

    public LIRConstant asConstant() {
        if (this instanceof LIRConstant) {
            return (LIRConstant) this;
        }
        return null;
    }

    public LIRAddress asAddress() {
        if (this instanceof LIRAddress) {
            return (LIRAddress) this;
        }
        return null;
    }

    public void printValueOn(LogStream out) {
        throw Util.unimplemented();
    }
}
