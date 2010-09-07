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
package com.sun.c1x.target.amd64;

import static com.sun.cri.ci.CiKind.*;
import static com.sun.cri.ci.CiRegister.RegisterFlag.*;

import com.sun.cri.ci.*;
import com.sun.cri.ci.CiRegister.*;

/**
 * Represents the AMD64 architecture.
 *
 * @author Thomas Wuerthinger
 */
public class AMD64 extends CiArchitecture {

    // General purpose CPU registers
    public static final CiRegister rax = new CiRegister(0, 0, "rax", CPU, RegisterFlag.Byte);
    public static final CiRegister rcx = new CiRegister(1, 1, "rcx", CPU, RegisterFlag.Byte);
    public static final CiRegister rdx = new CiRegister(2, 2, "rdx", CPU, RegisterFlag.Byte);
    public static final CiRegister rbx = new CiRegister(3, 3, "rbx", CPU, RegisterFlag.Byte);
    public static final CiRegister rsp = new CiRegister(4, 4, "rsp", CPU);
    public static final CiRegister rbp = new CiRegister(5, 5, "rbp", CPU);
    public static final CiRegister rsi = new CiRegister(6, 6, "rsi", CPU, RegisterFlag.Byte); // (tw) check if byte flag is correct
    public static final CiRegister rdi = new CiRegister(7, 7, "rdi", CPU, RegisterFlag.Byte); // (tw) check if byte flag is correct

    public static final CiRegister r8 = new CiRegister(8, 8, "r8", CPU);
    public static final CiRegister r9 = new CiRegister(9, 9, "r9", CPU);
    public static final CiRegister r10 = new CiRegister(10, 10, "r10", CPU);
    public static final CiRegister r11 = new CiRegister(11, 11, "r11", CPU);
    public static final CiRegister r12 = new CiRegister(12, 12, "r12", CPU);
    public static final CiRegister r13 = new CiRegister(13, 13, "r13", CPU);
    public static final CiRegister r14 = new CiRegister(14, 14, "r14", CPU, NonZero);
    public static final CiRegister r15 = new CiRegister(15, 15, "r15", CPU);
    public static final CiRegister[] cpuRegisters = {rax, rcx, rdx, rbx, rsp, rbp, rsi, rdi, r8, r9, r10, r11, r12, r13, r14, r15};

    // XMM registers
    public static final CiRegister xmm0 = new CiRegister(16, 0, "xmm0", FPU);
    public static final CiRegister xmm1 = new CiRegister(17, 1, "xmm1", FPU);
    public static final CiRegister xmm2 = new CiRegister(18, 2, "xmm2", FPU);
    public static final CiRegister xmm3 = new CiRegister(19, 3, "xmm3", FPU);
    public static final CiRegister xmm4 = new CiRegister(20, 4, "xmm4", FPU);
    public static final CiRegister xmm5 = new CiRegister(21, 5, "xmm5", FPU);
    public static final CiRegister xmm6 = new CiRegister(22, 6, "xmm6", FPU);
    public static final CiRegister xmm7 = new CiRegister(23, 7, "xmm7", FPU);

    public static final CiRegister xmm8 = new CiRegister(24, 8, "xmm8", FPU);
    public static final CiRegister xmm9 = new CiRegister(25, 9, "xmm9", FPU);
    public static final CiRegister xmm10 = new CiRegister(26, 10, "xmm10", FPU);
    public static final CiRegister xmm11 = new CiRegister(27, 11, "xmm11", FPU);
    public static final CiRegister xmm12 = new CiRegister(28, 12, "xmm12", FPU);
    public static final CiRegister xmm13 = new CiRegister(29, 13, "xmm13", FPU);
    public static final CiRegister xmm14 = new CiRegister(30, 14, "xmm14", FPU);
    public static final CiRegister xmm15 = new CiRegister(31, 15, "xmm15", FPU);

    public static final CiRegister[] xmmRegisters = {
        xmm0, xmm1, xmm2,  xmm3,  xmm4,  xmm5,  xmm6,  xmm7,
        xmm8, xmm9, xmm10, xmm11, xmm12, xmm13, xmm14, xmm15
    };

    public static final CiRegister[] allRegisters = {
        rax,  rcx,  rdx,   rbx,   rsp,   rbp,   rsi,   rdi,
        r8,   r9,   r10,   r11,   r12,   r13,   r14,   r15,
        xmm0, xmm1, xmm2,  xmm3,  xmm4,  xmm5,  xmm6,  xmm7,
        xmm8, xmm9, xmm10, xmm11, xmm12, xmm13, xmm14, xmm15
    };

    public static final CiRegisterValue RSP = rsp.asValue(Word);

    public AMD64() {
        super("AMD64", 8, "x86", ByteOrder.LittleEndian, allRegisters, 1, 8);
    }

    @Override
    public boolean isX86() {
        return true;
    }

    @Override
    public boolean twoOperandMode() {
        return true;
    }

}
