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

import com.sun.c1x.C1XCompilation;
import com.sun.c1x.ci.CiMethod;
import com.sun.c1x.lir.FrameMap;
import com.sun.c1x.lir.LIROperand;
import com.sun.c1x.lir.LIROperandFactory;
import com.sun.c1x.target.Architecture;
import com.sun.c1x.target.Register;
import com.sun.c1x.util.Util;
import com.sun.c1x.value.BasicType;

/**
 *
 * @author Thomas Wuerthinger
 *
 */
public class X86FrameMap extends FrameMap {

    static final Register r15thread = X86.r15;

    static final LIROperand rsiOopOpr = asOopOpr(X86.rsi);
    static final LIROperand rdiOopOpr = asOopOpr(X86.rdi);
    static final LIROperand rbxOopOpr = asOopOpr(X86.rbx);
    static final LIROperand raxOopOpr = asOopOpr(X86.rax);
    static final LIROperand rdxOopOpr = asOopOpr(X86.rdx);
    static final LIROperand rcxOopOpr = asOopOpr(X86.rcx);
    static final LIROperand rsiOpr = asOpr(X86.rsi);
    static final LIROperand rdiOpr = asOpr(X86.rdi);
    static final LIROperand rbxOpr = asOpr(X86.rbx);
    static final LIROperand raxOpr = asOpr(X86.rax);
    static final LIROperand rdxOpr = asOpr(X86.rdx);
    static final LIROperand rcxOpr = asOpr(X86.rcx);

    private static final LIROperand rspOpr32 = asPointerOpr32(X86.rsp);
    private static final LIROperand rspOpr64 = asPointerOpr64(X86.rsp);


    @Override
    public Register stackRegister() {
        // TODO Retrieve this from target or runtime!
        return X86.rsp;
    }

    static LIROperand rspOpr(Architecture arch) {
        if (arch.is32bit()) {
            return rspOpr32;
        } else if (arch.is64bit()) {
            return rspOpr64;
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    static final LIROperand rbpOpr32 = asPointerOpr32(X86.rbp);
    static final LIROperand rbpOpr64 = asPointerOpr64(X86.rbp);

    // TODO: Check the consequences of not putting rsp / rbp at the end in 64 bit and not having cpureg2reg!

    // Only 64 bit oop operands
    static final LIROperand r8oopOpr = asOopOpr(X86.r8);
    static final LIROperand r9oopOpr = asOopOpr(X86.r9);
    static final LIROperand r11oopOpr = asOopOpr(X86.r11);
    static final LIROperand r12oopOpr = asOopOpr(X86.r12);
    static final LIROperand r13oopOpr = asOopOpr(X86.r13);
    static final LIROperand r14oopOpr = asOopOpr(X86.r14);


    private static final LIROperand long0Opr32 = LIROperandFactory.doubleLocation(BasicType.Long, X86.rax, X86.rdx);
    private static final LIROperand long0Opr64 = LIROperandFactory.doubleLocation(BasicType.Long, X86.rax, X86.rax);

    static LIROperand long0Opr(Architecture arch) {
        if (arch.is32bit()) {
            return long0Opr32;
        } else if (arch.is64bit()) {
            return long0Opr64;
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    private static final LIROperand long1Opr32 = LIROperandFactory.doubleLocation(BasicType.Long, X86.rbx, X86.rcx);
    private static final LIROperand long1Opr64 = LIROperandFactory.doubleLocation(BasicType.Long, X86.rbx, X86.rbx);
    static LIROperand long1Opr(Architecture arch) {
        if (arch.is32bit()) {
            return long1Opr32;
        } else if (arch.is64bit()) {
            return long1Opr64;
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    static final LIROperand xmm0floatOpr = LIROperandFactory.singleLocation(BasicType.Float, X86.xmm0);
    static final LIROperand xmm0doubleOpr = LIROperandFactory.doubleLocation(BasicType.Double, X86.xmm0, X86.xmm0);

    static LIROperand asOopOpr(Register reg) {
        return LIROperandFactory.singleLocation(BasicType.Object, reg);
    }

    private static LIROperand asPointerOpr32(Register reg) {
        return asOpr(reg);
    }

    private static LIROperand asLongOpr(Register reg) {
        return LIROperandFactory.doubleLocation(BasicType.Long, reg, reg);
    }

    private static LIROperand asPointerOpr64(Register reg) {
        return asLongOpr(reg);
    }

    static LIROperand asPointerOpr(Register reg, Architecture arch) {
        assert reg != null;
        if (arch.is32bit()) {
            return asPointerOpr32(reg);
        } else if (arch.is64bit()) {
            return asPointerOpr64(reg);
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    static LIROperand asOpr(Register reg) {
        return LIROperandFactory.singleLocation(BasicType.Int, reg);
    }

    public static Register rscratch1(Architecture arch) {
        return (arch.is32bit()) ? Register.noreg : X86.r10;
    }

    public X86FrameMap(C1XCompilation compilation, CiMethod method, int numberOfLocks, int maxStack) {
        super(compilation, method, numberOfLocks, maxStack);
    }

    @Override
    public boolean allocatableRegister(Register r) {
        // TODO: (tw) Remove this hack
        return r != X86.rsp && r != X86.rbp;
    }

}
