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

/**
 *
 * @author Thomas Wuerthinger
 * @author Marcelo Cintra
 *
 */
public final class X86Register extends Register {

    // Invalid register
    public static final X86Register noreg = new X86Register(-1, "noreg");

    // Registers for 32 bit and 64 bit architecture
    public static final X86Register rax = new X86Register(1, "rax");
    public static final X86Register rcx = new X86Register(2, "rcx");
    public static final X86Register rdx = new X86Register(3, "rdx");
    public static final X86Register rbx = new X86Register(4, "rbx");
    public static final X86Register rsp = new X86Register(5, "rsp");
    public static final X86Register rbp = new X86Register(6, "rbp");
    public static final X86Register rsi = new X86Register(7, "rsi");
    public static final X86Register rdi = new X86Register(8, "rdi");

    // CPU registers only on 64 bit architecture
    public static final X86Register r8 = new X86Register(9, "r8");
    public static final X86Register r9 = new X86Register(10, "r9");
    public static final X86Register r10 = new X86Register(11, "r10");
    public static final X86Register r11 = new X86Register(12, "r11");
    public static final X86Register r12 = new X86Register(13, "r12");
    public static final X86Register r13 = new X86Register(14, "r13");
    public static final X86Register r14 = new X86Register(15, "r14");
    public static final X86Register r15 = new X86Register(16, "r15");

    // Floating point registers
    public static final X86Register fpu0 = new X86Register(17, "fpu0");
    public static final X86Register fpu1 = new X86Register(18, "fpu1");
    public static final X86Register fpu2 = new X86Register(19, "fpu2");
    public static final X86Register fpu3 = new X86Register(20, "fpu3");
    public static final X86Register fpu4 = new X86Register(21, "fpu4");
    public static final X86Register fpu5 = new X86Register(22, "fpu5");
    public static final X86Register fpu6 = new X86Register(23, "fpu6");
    public static final X86Register fpu7 = new X86Register(24, "fpu7");

    // XMM registers
    public static final X86Register xmm0 = new X86Register(25, "xmm0");
    public static final X86Register xmm1 = new X86Register(26, "xmm1");
    public static final X86Register xmm2 = new X86Register(27, "xmm2");
    public static final X86Register xmm3 = new X86Register(28, "xmm3");
    public static final X86Register xmm4 = new X86Register(29, "xmm4");
    public static final X86Register xmm5 = new X86Register(30, "xmm5");
    public static final X86Register xmm6 = new X86Register(31, "xmm6");
    public static final X86Register xmm7 = new X86Register(32, "xmm7");

    // XMM registers only on 64 bit architecture
    public static final X86Register xmm8 = new X86Register(33, "xmm8");
    public static final X86Register xmm9 = new X86Register(34, "xmm9");
    public static final X86Register xmm10 = new X86Register(35, "xmm10");
    public static final X86Register xmm11 = new X86Register(36, "xmm11");
    public static final X86Register xmm12 = new X86Register(37, "xmm12");
    public static final X86Register xmm13 = new X86Register(38, "xmm13");
    public static final X86Register xmm14 = new X86Register(39, "xmm14");
    public static final X86Register xmm15 = new X86Register(40, "xmm15");

    private X86Register(int number, String name) {
        super(number, name);
    }

    public int encoding() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean hasByteRegister() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isMMX() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isXMM() {
        // TODO Auto-generated method stub
        return false;
    }

    public static X86Register fromEncoding(int i) {
        // TODO Auto-generated method stub
        return null;
    }
}
