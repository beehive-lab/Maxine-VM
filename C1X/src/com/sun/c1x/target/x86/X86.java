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

import com.sun.c1x.ci.*;
import com.sun.c1x.ci.CiRegister.*;

/**
 * This class represents the X86 architecture.
 *
 * @author Thomas Wuerthinger
 */
public class X86 extends CiArchitecture {

    // Registers for 32 bit and 64 bit architecture
    public static final CiRegister rax = new CiRegister(64, 1, 0, "rax", RegisterFlag.CPU, RegisterFlag.Byte);
    public static final CiRegister rcx = new CiRegister(64, 2, 1, "rcx", RegisterFlag.CPU, RegisterFlag.Byte);
    public static final CiRegister rdx = new CiRegister(64, 3, 2, "rdx", RegisterFlag.CPU, RegisterFlag.Byte);
    public static final CiRegister rbx = new CiRegister(64, 4, 3, "rbx", RegisterFlag.CPU, RegisterFlag.Byte);
    public static final CiRegister rsp = new CiRegister(64, 5, 4, "rsp", RegisterFlag.CPU);
    public static final CiRegister rbp = new CiRegister(64, 6, 5, "rbp", RegisterFlag.CPU);
    public static final CiRegister rsi = new CiRegister(64, 7, 6, "rsi", RegisterFlag.CPU, RegisterFlag.Byte); // (tw) check if byte flag is correct
    public static final CiRegister rdi = new CiRegister(64, 8, 7, "rdi", RegisterFlag.CPU, RegisterFlag.Byte); // (tw) check if byte flag is correct
    public static final CiRegister[] cpuRegisters = {rax, rcx, rdx, rbx, rsp, rbp, rsi, rdi};

    // CPU registers only on 64 bit architecture
    public static final CiRegister r8 = new CiRegister(64, 9, 8, "r8", RegisterFlag.CPU);
    public static final CiRegister r9 = new CiRegister(64, 10, 9, "r9", RegisterFlag.CPU);
    public static final CiRegister r10 = new CiRegister(64, 11, 10, "r10", RegisterFlag.CPU);
    public static final CiRegister r11 = new CiRegister(64, 12, 11, "r11", RegisterFlag.CPU);
    public static final CiRegister r12 = new CiRegister(64, 13, 12, "r12", RegisterFlag.CPU);
    public static final CiRegister r13 = new CiRegister(64, 14, 13, "r13", RegisterFlag.CPU);
    public static final CiRegister r14 = new CiRegister(64, 15, 14, "r14", RegisterFlag.CPU);
    public static final CiRegister r15 = new CiRegister(64, 16, 15, "r15", RegisterFlag.CPU);
    public static final CiRegister[] cpuRegisters64 = {rax, rcx, rdx, rbx, rsp, rbp, rsi, rdi, r8, r9, r10, r11, r12, r13, r14, r15};

    // XMM registers
    public static final CiRegister xmm0 = new CiRegister(128, 17, 0, "xmm0", RegisterFlag.XMM);
    public static final CiRegister xmm1 = new CiRegister(128, 18, 1, "xmm1", RegisterFlag.XMM);
    public static final CiRegister xmm2 = new CiRegister(128, 19, 2, "xmm2", RegisterFlag.XMM);
    public static final CiRegister xmm3 = new CiRegister(128, 20, 3, "xmm3", RegisterFlag.XMM);
    public static final CiRegister xmm4 = new CiRegister(128, 21, 4, "xmm4", RegisterFlag.XMM);
    public static final CiRegister xmm5 = new CiRegister(128, 22, 5, "xmm5", RegisterFlag.XMM);
    public static final CiRegister xmm6 = new CiRegister(128, 23, 6, "xmm6", RegisterFlag.XMM);
    public static final CiRegister xmm7 = new CiRegister(128, 24, 7, "xmm7", RegisterFlag.XMM);
    public static final CiRegister[] xmmRegisters = {xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7};

    // XMM registers only on 64 bit architecture
    public static final CiRegister xmm8 = new CiRegister(128, 25, 8, "xmm8", RegisterFlag.XMM);
    public static final CiRegister xmm9 = new CiRegister(128, 26, 9, "xmm9", RegisterFlag.XMM);
    public static final CiRegister xmm10 = new CiRegister(128, 27, 10, "xmm10", RegisterFlag.XMM);
    public static final CiRegister xmm11 = new CiRegister(128, 28, 11, "xmm11", RegisterFlag.XMM);
    public static final CiRegister xmm12 = new CiRegister(128, 29, 12, "xmm12", RegisterFlag.XMM);
    public static final CiRegister xmm13 = new CiRegister(128, 30, 13, "xmm13", RegisterFlag.XMM);
    public static final CiRegister xmm14 = new CiRegister(128, 31, 14, "xmm14", RegisterFlag.XMM);
    public static final CiRegister xmm15 = new CiRegister(128, 32, 15, "xmm15", RegisterFlag.XMM);
    public static final CiRegister[] xmmRegisters64 = {xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7, xmm8, xmm9, xmm10, xmm11, xmm12, xmm13, xmm14, xmm15};

    public static final CiRegister[] allRegisters =   {rax, rcx, rdx, rbx, rsp, rbp, rsi, rdi, xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7};
    public static final CiRegister[] allRegisters64 = {rax, rcx, rdx, rbx, rsp, rbp, rsi, rdi, r8, r9, r10, r11, r12, r13, r14, r15, xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7, xmm8, xmm9, xmm10, xmm11, xmm12, xmm13, xmm14, xmm15};

    public X86(String name, int wordSize, CiRegister[] registers, int framePadding) {
        super(name, wordSize, "x86", ByteOrder.LittleEndian, registers, 1, wordSize);
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
