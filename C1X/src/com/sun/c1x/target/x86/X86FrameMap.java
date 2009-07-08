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
package com.sun.c1x.target.x86;

import com.sun.c1x.lir.*;
import com.sun.c1x.target.*;
import com.sun.c1x.util.*;

/**
 *
 * @author Thomas Wuerthinger
 *
 */
public class X86FrameMap {

    static final Register r15thread = X86Register.r15;

    static final LIROperand rsiOopOpr = asOopOpr(X86Register.rsi);
    static final LIROperand rdiOopOpr = asOopOpr(X86Register.rdi);
    static final LIROperand rbxOopOpr = asOopOpr(X86Register.rbx);
    static final LIROperand raxOopOpr = asOopOpr(X86Register.rax);
    static final LIROperand rdxOopOpr = asOopOpr(X86Register.rdx);
    static final LIROperand rcxOopOpr = asOopOpr(X86Register.rcx);
    static final LIROperand rsiOpr = asOpr(X86Register.rsi);
    static final LIROperand rdiOpr = asOpr(X86Register.rdi);
    static final LIROperand rbxOpr = asOpr(X86Register.rbx);
    static final LIROperand raxOpr = asOpr(X86Register.rax);
    static final LIROperand rdxOpr = asOpr(X86Register.rdx);
    static final LIROperand rcxOpr = asOpr(X86Register.rcx);

    private static final LIROperand rspOpr32 = asPointerOpr32(X86Register.rsp);
    private static final LIROperand rspOpr64 = asPointerOpr64(X86Register.rsp);

    static final LIROperand rspOpr(Architecture arch) {
        if (arch.is32bit()) {
            return rspOpr32;
        } else if (arch.is64bit()) {
            return rspOpr64;
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    static final LIROperand rbpOpr32 = asPointerOpr32(X86Register.rbp);
    static final LIROperand rbpOpr64 = asPointerOpr64(X86Register.rbp);

    // TODO: Check the consequences of not putting rsp / rbp at the end in 64 bit and not having cpureg2reg!

    // Only 64 bit oop operands
    static final LIROperand r8oopOpr = asOopOpr(X86Register.r8);
    static final LIROperand r9oopOpr = asOopOpr(X86Register.r9);
    static final LIROperand r11oopOpr = asOopOpr(X86Register.r11);
    static final LIROperand r12oopOpr = asOopOpr(X86Register.r12);
    static final LIROperand r13oopOpr = asOopOpr(X86Register.r13);
    static final LIROperand r14oopOpr = asOopOpr(X86Register.r14);


    private static final LIROperand long0Opr32 = LIROperandFactory.doubleCpu(X86Register.rax.number, X86Register.rdx.number);
    private static final LIROperand long0Opr64 = LIROperandFactory.doubleCpu(X86Register.rax.number, X86Register.rax.number);

    static final LIROperand long0Opr(Architecture arch) {
        if (arch.is32bit()) {
            return long0Opr32;
        } else if (arch.is64bit()) {
            return long0Opr64;
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    private static final LIROperand long1Opr32 = LIROperandFactory.doubleCpu(X86Register.rbx.number, X86Register.rcx.number);
    private static final LIROperand long1Opr64 = LIROperandFactory.doubleCpu(X86Register.rbx.number, X86Register.rbx.number);
    static final LIROperand long1Opr(Architecture arch) {
        if (arch.is32bit()) {
            return long1Opr32;
        } else if (arch.is64bit()) {
            return long1Opr64;
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    static final LIROperand fpu0FloatOpr   = LIROperandFactory.singleFpu(X86Register.fpu0.number);
    static final LIROperand fpu0DoubleOpr   = LIROperandFactory.doubleFpu(X86Register.fpu0.number);

    static final LIROperand xmm0floatOpr = LIROperandFactory.singleXmmX86(X86Register.xmm0.number);
    static final LIROperand xmm0doubleOpr = LIROperandFactory.doubleXmmX86(X86Register.xmm0.number);

    static LIROperand asOopOpr(Register reg) {
        return LIROperandFactory.singleCpuOop(reg.number);
    }

    private static LIROperand asPointerOpr32(Register reg) {
        return asOpr(reg);
    }

    private static LIROperand asLongOpr(Register reg) {
        return LIROperandFactory.doubleCpu(reg.number, reg.number);
    }

    private static LIROperand asPointerOpr64(Register reg) {
        return asLongOpr(reg);
    }

    static final LIROperand asPointerOpr(Register reg, Architecture arch) {
        if (arch.is32bit()) {
            return asPointerOpr32(reg);
        } else if (arch.is64bit()) {
            return asPointerOpr64(reg);
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    static LIROperand asOpr(Register reg) {
        return LIROperandFactory.singleCpu(reg.number);
    }




}
