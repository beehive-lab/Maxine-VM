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

import com.sun.c1x.target.Architecture;
import com.sun.c1x.target.Backend;
import com.sun.c1x.target.Register;
import com.sun.c1x.target.Target;
import com.sun.c1x.target.Register.*;

/**
 *
 * @author Thomas Wuerthinger
 *
 */
public class X86 extends Architecture {

    // Registers for 32 bit and 64 bit architecture
    public static final Register rax = new Register(1, 0, "rax", RegisterFlag.CPU, RegisterFlag.Byte);
    public static final Register rcx = new Register(2, 1, "rcx", RegisterFlag.CPU, RegisterFlag.Byte);
    public static final Register rdx = new Register(3, 2, "rdx", RegisterFlag.CPU, RegisterFlag.Byte);
    public static final Register rbx = new Register(4, 3, "rbx", RegisterFlag.CPU, RegisterFlag.Byte);
    public static final Register rsp = new Register(5, 4, "rsp", RegisterFlag.CPU);
    public static final Register rbp = new Register(6, 5, "rbp", RegisterFlag.CPU);
    public static final Register rsi = new Register(7, 6, "rsi", RegisterFlag.CPU);
    public static final Register rdi = new Register(8, 7, "rdi", RegisterFlag.CPU);
    public static final Register[] cpuRegisters = {rax, rcx, rdx, rbx, rsp, rbp, rsi, rdi};

    // CPU registers only on 64 bit architecture
    public static final Register r8 = new Register(9, 8, "r8", RegisterFlag.CPU);
    public static final Register r9 = new Register(10, 9, "r9", RegisterFlag.CPU);
    public static final Register r10 = new Register(11, 10, "r10", RegisterFlag.CPU);
    public static final Register r11 = new Register(12, 11, "r11", RegisterFlag.CPU);
    public static final Register r12 = new Register(13, 12, "r12", RegisterFlag.CPU);
    public static final Register r13 = new Register(14, 13, "r13", RegisterFlag.CPU);
    public static final Register r14 = new Register(15, 14, "r14", RegisterFlag.CPU);
    public static final Register r15 = new Register(16, 15, "r15", RegisterFlag.CPU);
    public static final Register[] cpuRegisters64 = {X86.rax, X86.rcx, X86.rdx, X86.rbx, X86.rsp, X86.rbp, X86.rsi, X86.rdi, r8, r9, r10, r11, r12, r13, r14, r15};

    // XMM registers
    public static final Register xmm0 = new Register(17, 0, "xmm0", RegisterFlag.XMM);
    public static final Register xmm1 = new Register(18, 1, "xmm1", RegisterFlag.XMM);
    public static final Register xmm2 = new Register(19, 2, "xmm2", RegisterFlag.XMM);
    public static final Register xmm3 = new Register(20, 3, "xmm3", RegisterFlag.XMM);
    public static final Register xmm4 = new Register(21, 4, "xmm4", RegisterFlag.XMM);
    public static final Register xmm5 = new Register(22, 5, "xmm5", RegisterFlag.XMM);
    public static final Register xmm6 = new Register(23, 6, "xmm6", RegisterFlag.XMM);
    public static final Register xmm7 = new Register(24, 7, "xmm7", RegisterFlag.XMM);
    public static final Register[] xmmRegisters = {xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7};

    // XMM registers only on 64 bit architecture
    public static final Register xmm8 = new Register(25, 8, "xmm8", RegisterFlag.XMM);
    public static final Register xmm9 = new Register(26, 9, "xmm9", RegisterFlag.XMM);
    public static final Register xmm10 = new Register(27, 10, "xmm10", RegisterFlag.XMM);
    public static final Register xmm11 = new Register(28, 11, "xmm11", RegisterFlag.XMM);
    public static final Register xmm12 = new Register(29, 12, "xmm12", RegisterFlag.XMM);
    public static final Register xmm13 = new Register(30, 13, "xmm13", RegisterFlag.XMM);
    public static final Register xmm14 = new Register(31, 14, "xmm14", RegisterFlag.XMM);
    public static final Register xmm15 = new Register(32, 15, "xmm15", RegisterFlag.XMM);
    public static final Register[] xmmRegisters64 = {xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7, xmm8, xmm9, xmm10, xmm11, xmm12, xmm13, xmm14, xmm15};

    public static final Register[] allRegisters =   {rax, rcx, rdx, rbx, rsp, rbp, rsi, rdi, xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7};
    public static final Register[] allRegisters64 = {rax, rcx, rdx, rbx, rsp, rbp, rsi, rdi, r8, r9, r10, r11, r12, r13, r14, r15, xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7, xmm8, xmm9, xmm10, xmm11, xmm12, xmm13, xmm14, xmm15};

    public X86(String name, int wordSize, Register[] registers, int framePadding, int nativeMoveConstInstructionSize) {
        super(name, wordSize, "x86", BitOrdering.LittleEndian, registers, framePadding, 1, nativeMoveConstInstructionSize);
    }

    @Override
    public Backend getBackend(Target target) {
        return new X86Backend(target);
    }
}
