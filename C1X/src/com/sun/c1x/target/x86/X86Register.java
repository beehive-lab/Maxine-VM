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

import com.sun.c1x.target.*;
import com.sun.c1x.target.Register.*;

/**
 *
 * @author Thomas Wuerthinger
 * @author Marcelo Cintra
 *
 */
public final class X86Register {

    // Registers for 32 bit and 64 bit architecture
    public static final Register rax = new Register(1, "rax", RegisterFlag.CPU);
    public static final Register rcx = new Register(2, "rcx", RegisterFlag.CPU, RegisterFlag.Byte);
    public static final Register rdx = new Register(3, "rdx", RegisterFlag.CPU, RegisterFlag.Byte);
    public static final Register rbx = new Register(4, "rbx", RegisterFlag.CPU, RegisterFlag.Byte);
    public static final Register rsp = new Register(5, "rsp", RegisterFlag.CPU, RegisterFlag.Byte);
    public static final Register rbp = new Register(6, "rbp", RegisterFlag.CPU);
    public static final Register rsi = new Register(7, "rsi", RegisterFlag.CPU);
    public static final Register rdi = new Register(8, "rdi", RegisterFlag.CPU);

    public static final Register[] cpuRegisters = new Register[]{rax, rcx, rdx, rbx, rsp, rbp, rsi, rdi};

    // CPU registers only on 64 bit architecture
    public static final Register r8 = new Register(9, "r8", RegisterFlag.CPU);
    public static final Register r9 = new Register(10, "r9", RegisterFlag.CPU);
    public static final Register r10 = new Register(11, "r10", RegisterFlag.CPU);
    public static final Register r11 = new Register(12, "r11", RegisterFlag.CPU);
    public static final Register r12 = new Register(13, "r12", RegisterFlag.CPU);
    public static final Register r13 = new Register(14, "r13", RegisterFlag.CPU);
    public static final Register r14 = new Register(15, "r14", RegisterFlag.CPU);
    public static final Register r15 = new Register(16, "r15", RegisterFlag.CPU);
    public static final Register[] cpuRegisters64 = new Register[]{rax, rcx, rdx, rbx, rsp, rbp, rsi, rdi, r8, r9, r10, r11, r12, r13, r14, r15};

    // Floating point registers
    public static final Register fpu0 = new Register(17, "fpu0", RegisterFlag.FPU);
    public static final Register fpu1 = new Register(18, "fpu1", RegisterFlag.FPU);
    public static final Register fpu2 = new Register(19, "fpu2", RegisterFlag.FPU);
    public static final Register fpu3 = new Register(20, "fpu3", RegisterFlag.FPU);
    public static final Register fpu4 = new Register(21, "fpu4", RegisterFlag.FPU);
    public static final Register fpu5 = new Register(22, "fpu5", RegisterFlag.FPU);
    public static final Register fpu6 = new Register(23, "fpu6", RegisterFlag.FPU);
    public static final Register fpu7 = new Register(24, "fpu7", RegisterFlag.FPU);
    public static final Register[] fpuRegisters = new Register[]{fpu0, fpu1, fpu2, fpu3, fpu4, fpu5, fpu6, fpu7};

    // XMM registers
    public static final Register xmm0 = new Register(25, "xmm0", RegisterFlag.XMM);
    public static final Register xmm1 = new Register(26, "xmm1", RegisterFlag.XMM);
    public static final Register xmm2 = new Register(27, "xmm2", RegisterFlag.XMM);
    public static final Register xmm3 = new Register(28, "xmm3", RegisterFlag.XMM);
    public static final Register xmm4 = new Register(29, "xmm4", RegisterFlag.XMM);
    public static final Register xmm5 = new Register(30, "xmm5", RegisterFlag.XMM);
    public static final Register xmm6 = new Register(31, "xmm6", RegisterFlag.XMM);
    public static final Register xmm7 = new Register(32, "xmm7", RegisterFlag.XMM);
    public static final Register[] xmmRegisters = new Register[]{xmm0, xmm1, xmm2, xmm3, xmm4, xmm6, xmm7};

    // XMM registers only on 64 bit architecture
    public static final Register xmm8 = new Register(33, "xmm8", RegisterFlag.XMM);
    public static final Register xmm9 = new Register(34, "xmm9", RegisterFlag.XMM);
    public static final Register xmm10 = new Register(35, "xmm10", RegisterFlag.XMM);
    public static final Register xmm11 = new Register(36, "xmm11", RegisterFlag.XMM);
    public static final Register xmm12 = new Register(37, "xmm12", RegisterFlag.XMM);
    public static final Register xmm13 = new Register(38, "xmm13", RegisterFlag.XMM);
    public static final Register xmm14 = new Register(39, "xmm14", RegisterFlag.XMM);
    public static final Register xmm15 = new Register(40, "xmm15", RegisterFlag.XMM);
    public static final Register[] xmmRegisters64 = new Register[]{xmm0, xmm1, xmm2, xmm3, xmm4, xmm6, xmm7, xmm8, xmm9, xmm10, xmm11, xmm12, xmm13, xmm14};
    public static final Register[] allRegisters = new Register[]{rax, rcx, rdx, rbx, rsp, rbp, rsi, rdi, fpu0, fpu1, fpu2, fpu3, fpu4, fpu5, fpu6, fpu7, xmm0, xmm1, xmm2, xmm3, xmm4, xmm6, xmm7};
    public static final Register[] allRegisters64 = new Register[] {rax, rcx, rdx, rbx, rsp, rbp, rsi, rdi, r8, r9, r10, r11, r12, r13, r14, r15, fpu0, fpu1, fpu2, fpu3, fpu4, fpu5, fpu6,
                    fpu7, xmm0, xmm1, xmm2, xmm3, xmm4, xmm6, xmm7, xmm8, xmm9, xmm10, xmm11, xmm12, xmm13, xmm14};


    public int encoding() {
        // TODO Auto-generated method stub
        return 0;
    }

    public static Register fromEncoding(int i) {
        // TODO Auto-generated method stub
        return null;
    }
}
