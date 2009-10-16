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
 * The <code>LIROperand</code> class represents an operand, either
 * a constant, an address calculation, a register, or a stack slot.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */

public class LIROperand {
    public final CiKind kind;

    protected LIROperand(CiKind basicType) {
        this.kind = basicType;
    }

    public final boolean isIllegal() {
        return this == LIROperandFactory.IllegalLocation;
    }

    public boolean isRegister() {
        return false;
    }

    public boolean isVirtual() {
        return isVirtualCpu();
    }

    public boolean isConstant() {
        return this instanceof LIRConstant;
    }

    public boolean isAddress() {
        return this instanceof LIRAddress;
    }

    public boolean isLocation() {
        return !(this instanceof LIRLocation);
    }

    @Override
    public String toString() {
        if (isIllegal()) {
            return "illegal";
        }

        final StringBuffer out = new StringBuffer();
        out.append("[");
        if (isLocation()) {
            //out.append(valueToString());
        } else if (isSingleStack()) {
            out.append("stack:").append(singleStackIx());
        } else if (isDoubleStack()) {
            out.append("dblStack:").append(doubleStackIx());
        } else if (isVirtual()) {
            out.append("R").append(vregNumber());
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
            out.append(String.format("|%c", this.kind.typeChar));
        }
        out.append("]");
        return out.toString();
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

    public boolean isSingleXmm() {
        return false;
    }

    public boolean isDoubleXmm() {
        return false;
    }

    public boolean isVirtualRegister() {
        return false;
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

    public CiRegister asRegister() {
        if (this == LIROperandFactory.IllegalLocation) {
            return CiRegister.None;
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
}
