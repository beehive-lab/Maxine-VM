/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.asm.amd64;

import com.sun.max.asm.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.asm.x86.*;

/**
 * The AMD64 assembler methods required by the VM and defined in {@link AMD64Assembler}.
 *
 * @see AMD64AssemblerSpecification
 * 
 * @author Doug Simon
 */
public class AMD64AssemblerMethods extends AbstractAMD64Assembler {

    public AMD64AssemblerMethods(long startAddress) {
        super(startAddress);
    }

    public AMD64AssemblerMethods() {
    }

    @Override
    protected void emitPadding(int numberOfBytes) throws AssemblyException {
        for (int i = 0; i < numberOfBytes; i++) {
            nop();
        }
    }

// START GENERATED RAW ASSEMBLER METHODS
    /**
     * Pseudo-external assembler syntax: {@code addl  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code addl      [rbx + 18], 0x12}
     */
    // Template#: 1, Serial#: 961
    public void addl(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0001((byte) 0x83, (byte) 0x00, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code addq  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code addq      [rbx + 18], 0x12}
     */
    // Template#: 2, Serial#: 1033
    public void addq(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0002((byte) 0x83, (byte) 0x00, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code add       [rbx + 18], eax}
     */
    // Template#: 3, Serial#: 32
    public void add(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0003((byte) 0x01, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code add       [rbx + 18], rax}
     */
    // Template#: 4, Serial#: 41
    public void add(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0004((byte) 0x01, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code addl  }<i>disp8</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code addl      [rbx + 18], 0x12345678}
     */
    // Template#: 5, Serial#: 745
    public void addl(byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        assemble0005((byte) 0x81, (byte) 0x00, disp8, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code addq  }<i>disp8</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code addq      [rbx + 18], 0x12345678}
     */
    // Template#: 6, Serial#: 817
    public void addq(byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        assemble0006((byte) 0x81, (byte) 0x00, disp8, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code addl  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code addl      eax, 0x12}
     */
    // Template#: 7, Serial#: 993
    public void addl(AMD64GeneralRegister32 destination, byte imm8) {
        assemble0007((byte) 0x83, (byte) 0x00, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code add       eax, [rbx + 18]}
     */
    // Template#: 8, Serial#: 86
    public void add(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0008((byte) 0x03, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code add       eax, eax}
     */
    // Template#: 9, Serial#: 36
    public void add(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0009((byte) 0x01, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code add       eax, [L1: +305419896]}
     */
    // Template#: 10, Serial#: 85
    public void rip_add(AMD64GeneralRegister32 destination, int rel32) {
        assemble0010((byte) 0x03, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code addl  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code addl      eax, 0x12345678}
     */
    // Template#: 11, Serial#: 777
    public void addl(AMD64GeneralRegister32 destination, int imm32) {
        assemble0011((byte) 0x81, (byte) 0x00, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code add       eax, [rbx + 305419896]}
     */
    // Template#: 12, Serial#: 88
    public void add(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0012((byte) 0x03, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code addq  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code addq      rax, 0x12}
     */
    // Template#: 13, Serial#: 1065
    public void addq(AMD64GeneralRegister64 destination, byte imm8) {
        assemble0013((byte) 0x83, (byte) 0x00, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code add       rax, [rbx + 18]}
     */
    // Template#: 14, Serial#: 95
    public void add(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0014((byte) 0x03, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code add       rax, rax}
     */
    // Template#: 15, Serial#: 45
    public void add(AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0015((byte) 0x01, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code add       rax, [L1: +305419896]}
     */
    // Template#: 16, Serial#: 94
    public void rip_add(AMD64GeneralRegister64 destination, int rel32) {
        assemble0016((byte) 0x03, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code addq  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code addq      rax, 0x12345678}
     */
    // Template#: 17, Serial#: 849
    public void addq(AMD64GeneralRegister64 destination, int imm32) {
        assemble0017((byte) 0x81, (byte) 0x00, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code add       rax, [rbx + 305419896]}
     */
    // Template#: 18, Serial#: 97
    public void add(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0018((byte) 0x03, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>imm32</i>
     * Example disassembly syntax: {@code add       eax, 0x12345678}
     */
    // Template#: 19, Serial#: 112
    public void add_EAX(int imm32) {
        assemble0019((byte) 0x05, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>imm32</i>
     * Example disassembly syntax: {@code add       rax, 0x12345678}
     */
    // Template#: 20, Serial#: 113
    public void add_RAX(int imm32) {
        assemble0020((byte) 0x05, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code addl  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code addl      [rbx + 305419896], 0x12}
     */
    // Template#: 21, Serial#: 977
    public void addl(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0021((byte) 0x83, (byte) 0x00, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code addq  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code addq      [rbx + 305419896], 0x12}
     */
    // Template#: 22, Serial#: 1049
    public void addq(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0022((byte) 0x83, (byte) 0x00, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code add       [rbx + 305419896], eax}
     */
    // Template#: 23, Serial#: 34
    public void add(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0023((byte) 0x01, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code add       [rbx + 305419896], rax}
     */
    // Template#: 24, Serial#: 43
    public void add(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0024((byte) 0x01, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code addl  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code addl      [rbx + 305419896], 0x12345678}
     */
    // Template#: 25, Serial#: 761
    public void addl(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0025((byte) 0x81, (byte) 0x00, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code addq  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code addq      [rbx + 305419896], 0x12345678}
     */
    // Template#: 26, Serial#: 833
    public void addq(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0026((byte) 0x81, (byte) 0x00, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code addsd  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code addsd     xmm0, [rbx + 18]}
     */
    // Template#: 27, Serial#: 10252
    public void addsd(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0027((byte) 0x58, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code addsd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code addsd     xmm0, xmm0}
     */
    // Template#: 28, Serial#: 10256
    public void addsd(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0028((byte) 0x58, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code addsd  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code addsd     xmm0, [L1: +305419896]}
     */
    // Template#: 29, Serial#: 10251
    public void rip_addsd(AMD64XMMRegister destination, int rel32) {
        assemble0029((byte) 0x58, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code addsd  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code addsd     xmm0, [rbx + 305419896]}
     */
    // Template#: 30, Serial#: 10254
    public void addsd(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0030((byte) 0x58, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code addss  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code addss     xmm0, [rbx + 18]}
     */
    // Template#: 31, Serial#: 10378
    public void addss(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0031((byte) 0x58, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code addss  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code addss     xmm0, xmm0}
     */
    // Template#: 32, Serial#: 10382
    public void addss(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0032((byte) 0x58, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code addss  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code addss     xmm0, [L1: +305419896]}
     */
    // Template#: 33, Serial#: 10377
    public void rip_addss(AMD64XMMRegister destination, int rel32) {
        assemble0033((byte) 0x58, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code addss  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code addss     xmm0, [rbx + 305419896]}
     */
    // Template#: 34, Serial#: 10380
    public void addss(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0034((byte) 0x58, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code andl  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code andl      [rbx + 18], 0x12}
     */
    // Template#: 35, Serial#: 969
    public void andl(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0001((byte) 0x83, (byte) 0x04, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code andq  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code andq      [rbx + 18], 0x12}
     */
    // Template#: 36, Serial#: 1041
    public void andq(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0002((byte) 0x83, (byte) 0x04, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code and       [rbx + 18], eax}
     */
    // Template#: 37, Serial#: 260
    public void and(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0003((byte) 0x21, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code and       [rbx + 18], rax}
     */
    // Template#: 38, Serial#: 269
    public void and(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0004((byte) 0x21, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code andl  }<i>disp8</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code andl      [rbx + 18], 0x12345678}
     */
    // Template#: 39, Serial#: 753
    public void andl(byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        assemble0005((byte) 0x81, (byte) 0x04, disp8, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code andq  }<i>disp8</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code andq      [rbx + 18], 0x12345678}
     */
    // Template#: 40, Serial#: 825
    public void andq(byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        assemble0006((byte) 0x81, (byte) 0x04, disp8, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code andl  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code andl      eax, 0x12}
     */
    // Template#: 41, Serial#: 997
    public void andl(AMD64GeneralRegister32 destination, byte imm8) {
        assemble0007((byte) 0x83, (byte) 0x04, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code and       eax, [rbx + 18]}
     */
    // Template#: 42, Serial#: 314
    public void and(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0008((byte) 0x23, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code and       eax, eax}
     */
    // Template#: 43, Serial#: 264
    public void and(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0009((byte) 0x21, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code and       eax, [L1: +305419896]}
     */
    // Template#: 44, Serial#: 313
    public void rip_and(AMD64GeneralRegister32 destination, int rel32) {
        assemble0010((byte) 0x23, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code andl  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code andl      eax, 0x12345678}
     */
    // Template#: 45, Serial#: 781
    public void andl(AMD64GeneralRegister32 destination, int imm32) {
        assemble0011((byte) 0x81, (byte) 0x04, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code and       eax, [rbx + 305419896]}
     */
    // Template#: 46, Serial#: 316
    public void and(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0012((byte) 0x23, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code andq  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code andq      rax, 0x12}
     */
    // Template#: 47, Serial#: 1069
    public void andq(AMD64GeneralRegister64 destination, byte imm8) {
        assemble0013((byte) 0x83, (byte) 0x04, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code and       rax, [rbx + 18]}
     */
    // Template#: 48, Serial#: 323
    public void and(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0014((byte) 0x23, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code and       rax, rax}
     */
    // Template#: 49, Serial#: 273
    public void and(AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0015((byte) 0x21, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code and       rax, [L1: +305419896]}
     */
    // Template#: 50, Serial#: 322
    public void rip_and(AMD64GeneralRegister64 destination, int rel32) {
        assemble0016((byte) 0x23, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code andq  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code andq      rax, 0x12345678}
     */
    // Template#: 51, Serial#: 853
    public void andq(AMD64GeneralRegister64 destination, int imm32) {
        assemble0017((byte) 0x81, (byte) 0x04, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code and       rax, [rbx + 305419896]}
     */
    // Template#: 52, Serial#: 325
    public void and(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0018((byte) 0x23, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>imm32</i>
     * Example disassembly syntax: {@code and       eax, 0x12345678}
     */
    // Template#: 53, Serial#: 340
    public void and_EAX(int imm32) {
        assemble0019((byte) 0x25, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>imm32</i>
     * Example disassembly syntax: {@code and       rax, 0x12345678}
     */
    // Template#: 54, Serial#: 341
    public void and_RAX(int imm32) {
        assemble0020((byte) 0x25, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code andl  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code andl      [rbx + 305419896], 0x12}
     */
    // Template#: 55, Serial#: 985
    public void andl(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0021((byte) 0x83, (byte) 0x04, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code andq  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code andq      [rbx + 305419896], 0x12}
     */
    // Template#: 56, Serial#: 1057
    public void andq(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0022((byte) 0x83, (byte) 0x04, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code and       [rbx + 305419896], eax}
     */
    // Template#: 57, Serial#: 262
    public void and(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0023((byte) 0x21, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code and       [rbx + 305419896], rax}
     */
    // Template#: 58, Serial#: 271
    public void and(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0024((byte) 0x21, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code andl  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code andl      [rbx + 305419896], 0x12345678}
     */
    // Template#: 59, Serial#: 769
    public void andl(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0025((byte) 0x81, (byte) 0x04, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code andq  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code andq      [rbx + 305419896], 0x12345678}
     */
    // Template#: 60, Serial#: 841
    public void andq(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0026((byte) 0x81, (byte) 0x04, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code bsf  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code bsf       eax, [rbx + 18]}
     */
    // Template#: 61, Serial#: 11629
    public void bsf(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0035((byte) 0xBC, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code bsf  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code bsf       eax, eax}
     */
    // Template#: 62, Serial#: 11633
    public void bsf(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0036((byte) 0xBC, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code bsf  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code bsf       eax, [rbx]}
     */
    // Template#: 63, Serial#: 11625
    public void bsf(AMD64GeneralRegister32 destination, AMD64IndirectRegister64 source) {
        assemble0037((byte) 0xBC, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code bsf  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code bsf       eax, [rbx + 305419896]}
     */
    // Template#: 64, Serial#: 11631
    public void bsf(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0038((byte) 0xBC, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code bsf  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code bsf       rax, [rbx + 18]}
     */
    // Template#: 65, Serial#: 11638
    public void bsf(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0039((byte) 0xBC, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code bsf  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code bsf       rax, rax}
     */
    // Template#: 66, Serial#: 11642
    public void bsf(AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0040((byte) 0xBC, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code bsf  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code bsf       rax, [rbx]}
     */
    // Template#: 67, Serial#: 11634
    public void bsf(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0041((byte) 0xBC, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code bsf  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code bsf       rax, [rbx + 305419896]}
     */
    // Template#: 68, Serial#: 11640
    public void bsf(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0042((byte) 0xBC, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code bsr  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code bsr       eax, [rbx + 18]}
     */
    // Template#: 69, Serial#: 11656
    public void bsr(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0035((byte) 0xBD, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code bsr  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code bsr       eax, eax}
     */
    // Template#: 70, Serial#: 11660
    public void bsr(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0036((byte) 0xBD, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code bsr  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code bsr       eax, [rbx]}
     */
    // Template#: 71, Serial#: 11652
    public void bsr(AMD64GeneralRegister32 destination, AMD64IndirectRegister64 source) {
        assemble0037((byte) 0xBD, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code bsr  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code bsr       eax, [rbx + 305419896]}
     */
    // Template#: 72, Serial#: 11658
    public void bsr(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0038((byte) 0xBD, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code bsr  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code bsr       rax, [rbx + 18]}
     */
    // Template#: 73, Serial#: 11665
    public void bsr(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0039((byte) 0xBD, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code bsr  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code bsr       rax, rax}
     */
    // Template#: 74, Serial#: 11669
    public void bsr(AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0040((byte) 0xBD, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code bsr  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code bsr       rax, [rbx]}
     */
    // Template#: 75, Serial#: 11661
    public void bsr(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0041((byte) 0xBD, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code bsr  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code bsr       rax, [rbx + 305419896]}
     */
    // Template#: 76, Serial#: 11667
    public void bsr(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0042((byte) 0xBD, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code call  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code call      [rbx + 18]}
     */
    // Template#: 77, Serial#: 5441
    public void call(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0043((byte) 0xFF, (byte) 0x02, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code call  }<i>destination</i>
     * Example disassembly syntax: {@code call      rax}
     */
    // Template#: 78, Serial#: 5453
    public void call(AMD64GeneralRegister64 destination) {
        assemble0044((byte) 0xFF, (byte) 0x02, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code call  }<i>rel32</i>
     * Example disassembly syntax: {@code call      L1: +305419896}
     */
    // Template#: 79, Serial#: 5288
    public void call(int rel32) {
        assemble0045((byte) 0xE8, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code call  }<i>rel32</i>
     * Example disassembly syntax: {@code call      [L1: +305419896]}
     */
    // Template#: 80, Serial#: 5432
    public void rip_call(int rel32) {
        assemble0046((byte) 0xFF, (byte) 0x02, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code call  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code call      [rbx + 305419896]}
     */
    // Template#: 81, Serial#: 5447
    public void call(int disp32, AMD64IndirectRegister64 destination) {
        assemble0047((byte) 0xFF, (byte) 0x02, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code cdq  }
     * Example disassembly syntax: {@code cdq     }
     */
    // Template#: 82, Serial#: 3859
    public void cdq() {
        assemble0048((byte) 0x99);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmova  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cmova     eax, [rbx + 18]}
     */
    // Template#: 83, Serial#: 6558
    public void cmova(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0035((byte) 0x47, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmova  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmova     eax, eax}
     */
    // Template#: 84, Serial#: 6562
    public void cmova(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0036((byte) 0x47, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmova  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cmova     eax, [L1: +305419896]}
     */
    // Template#: 85, Serial#: 6557
    public void rip_cmova(AMD64GeneralRegister32 destination, int rel32) {
        assemble0049((byte) 0x47, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmova  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cmova     eax, [rbx + 305419896]}
     */
    // Template#: 86, Serial#: 6560
    public void cmova(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0038((byte) 0x47, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovae  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovae    eax, [rbx + 18]}
     */
    // Template#: 87, Serial#: 6450
    public void cmovae(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0035((byte) 0x43, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovae  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovae    eax, eax}
     */
    // Template#: 88, Serial#: 6454
    public void cmovae(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0036((byte) 0x43, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovae  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cmovae    eax, [L1: +305419896]}
     */
    // Template#: 89, Serial#: 6449
    public void rip_cmovae(AMD64GeneralRegister32 destination, int rel32) {
        assemble0049((byte) 0x43, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovae  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovae    eax, [rbx + 305419896]}
     */
    // Template#: 90, Serial#: 6452
    public void cmovae(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0038((byte) 0x43, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovb  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovb     eax, [rbx + 18]}
     */
    // Template#: 91, Serial#: 6423
    public void cmovb(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0035((byte) 0x42, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovb  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovb     eax, eax}
     */
    // Template#: 92, Serial#: 6427
    public void cmovb(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0036((byte) 0x42, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovb  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cmovb     eax, [L1: +305419896]}
     */
    // Template#: 93, Serial#: 6422
    public void rip_cmovb(AMD64GeneralRegister32 destination, int rel32) {
        assemble0049((byte) 0x42, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovb  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovb     eax, [rbx + 305419896]}
     */
    // Template#: 94, Serial#: 6425
    public void cmovb(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0038((byte) 0x42, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovbe  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovbe    eax, [rbx + 18]}
     */
    // Template#: 95, Serial#: 6531
    public void cmovbe(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0035((byte) 0x46, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovbe  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovbe    eax, eax}
     */
    // Template#: 96, Serial#: 6535
    public void cmovbe(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0036((byte) 0x46, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovbe  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cmovbe    eax, [L1: +305419896]}
     */
    // Template#: 97, Serial#: 6530
    public void rip_cmovbe(AMD64GeneralRegister32 destination, int rel32) {
        assemble0049((byte) 0x46, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovbe  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovbe    eax, [rbx + 305419896]}
     */
    // Template#: 98, Serial#: 6533
    public void cmovbe(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0038((byte) 0x46, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmove  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cmove     eax, [rbx + 18]}
     */
    // Template#: 99, Serial#: 6477
    public void cmove(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0035((byte) 0x44, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmove  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmove     eax, eax}
     */
    // Template#: 100, Serial#: 6481
    public void cmove(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0036((byte) 0x44, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmove  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cmove     eax, [L1: +305419896]}
     */
    // Template#: 101, Serial#: 6476
    public void rip_cmove(AMD64GeneralRegister32 destination, int rel32) {
        assemble0049((byte) 0x44, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmove  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cmove     eax, [rbx + 305419896]}
     */
    // Template#: 102, Serial#: 6479
    public void cmove(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0038((byte) 0x44, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmove  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmove     rax, [rbx]}
     */
    // Template#: 103, Serial#: 6482
    public void cmove(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0041((byte) 0x44, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovg  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovg     eax, [rbx + 18]}
     */
    // Template#: 104, Serial#: 9937
    public void cmovg(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0035((byte) 0x4F, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovg  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovg     eax, eax}
     */
    // Template#: 105, Serial#: 9941
    public void cmovg(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0036((byte) 0x4F, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovg  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cmovg     eax, [L1: +305419896]}
     */
    // Template#: 106, Serial#: 9936
    public void rip_cmovg(AMD64GeneralRegister32 destination, int rel32) {
        assemble0049((byte) 0x4F, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovg  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovg     eax, [rbx + 305419896]}
     */
    // Template#: 107, Serial#: 9939
    public void cmovg(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0038((byte) 0x4F, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovg  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovg     rax, [rbx]}
     */
    // Template#: 108, Serial#: 9942
    public void cmovg(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0041((byte) 0x4F, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovge  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovge    eax, [rbx + 18]}
     */
    // Template#: 109, Serial#: 9883
    public void cmovge(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0035((byte) 0x4D, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovge  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovge    eax, eax}
     */
    // Template#: 110, Serial#: 9887
    public void cmovge(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0036((byte) 0x4D, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovge  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cmovge    eax, [L1: +305419896]}
     */
    // Template#: 111, Serial#: 9882
    public void rip_cmovge(AMD64GeneralRegister32 destination, int rel32) {
        assemble0049((byte) 0x4D, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovge  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovge    eax, [rbx + 305419896]}
     */
    // Template#: 112, Serial#: 9885
    public void cmovge(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0038((byte) 0x4D, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovge  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovge    rax, [rbx]}
     */
    // Template#: 113, Serial#: 9888
    public void cmovge(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0041((byte) 0x4D, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovl  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovl     eax, [rbx + 18]}
     */
    // Template#: 114, Serial#: 9856
    public void cmovl(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0035((byte) 0x4C, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovl  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovl     eax, eax}
     */
    // Template#: 115, Serial#: 9860
    public void cmovl(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0036((byte) 0x4C, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovl  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cmovl     eax, [L1: +305419896]}
     */
    // Template#: 116, Serial#: 9855
    public void rip_cmovl(AMD64GeneralRegister32 destination, int rel32) {
        assemble0049((byte) 0x4C, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovl  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovl     eax, [rbx + 305419896]}
     */
    // Template#: 117, Serial#: 9858
    public void cmovl(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0038((byte) 0x4C, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovl  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovl     rax, [rbx]}
     */
    // Template#: 118, Serial#: 9861
    public void cmovl(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0041((byte) 0x4C, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovle  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovle    eax, eax}
     */
    // Template#: 119, Serial#: 9914
    public void cmovle(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0036((byte) 0x4E, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovle  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cmovle    eax, [L1: +305419896]}
     */
    // Template#: 120, Serial#: 9909
    public void rip_cmovle(AMD64GeneralRegister32 destination, int rel32) {
        assemble0049((byte) 0x4E, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovle  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovle    rax, [rbx]}
     */
    // Template#: 121, Serial#: 9915
    public void cmovle(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0041((byte) 0x4E, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovne  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovne    rax, [rbx]}
     */
    // Template#: 122, Serial#: 6509
    public void cmovne(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0041((byte) 0x45, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovp  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovp     eax, [rbx + 18]}
     */
    // Template#: 123, Serial#: 9802
    public void cmovp(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0035((byte) 0x4A, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovp  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovp     eax, eax}
     */
    // Template#: 124, Serial#: 9806
    public void cmovp(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0036((byte) 0x4A, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovp  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cmovp     eax, [L1: +305419896]}
     */
    // Template#: 125, Serial#: 9801
    public void rip_cmovp(AMD64GeneralRegister32 destination, int rel32) {
        assemble0049((byte) 0x4A, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovp  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovp     eax, [rbx + 305419896]}
     */
    // Template#: 126, Serial#: 9804
    public void cmovp(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0038((byte) 0x4A, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpl  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code cmpl      [rbx + 18], 0x12}
     */
    // Template#: 127, Serial#: 975
    public void cmpl(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0001((byte) 0x83, (byte) 0x07, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpq  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code cmpq      [rbx + 18], 0x12}
     */
    // Template#: 128, Serial#: 1047
    public void cmpq(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0002((byte) 0x83, (byte) 0x07, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmp       [rbx + 18], eax}
     */
    // Template#: 129, Serial#: 3492
    public void cmp(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0003((byte) 0x39, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmp       [rbx + 18], rax}
     */
    // Template#: 130, Serial#: 3501
    public void cmp(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0004((byte) 0x39, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpl  }<i>disp8</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code cmpl      [rbx + 18], 0x12345678}
     */
    // Template#: 131, Serial#: 759
    public void cmpl(byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        assemble0005((byte) 0x81, (byte) 0x07, disp8, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpq  }<i>disp8</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code cmpq      [rbx + 18], 0x12345678}
     */
    // Template#: 132, Serial#: 831
    public void cmpq(byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        assemble0006((byte) 0x81, (byte) 0x07, disp8, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpl  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code cmpl      eax, 0x12}
     */
    // Template#: 133, Serial#: 1000
    public void cmpl(AMD64GeneralRegister32 destination, byte imm8) {
        assemble0007((byte) 0x83, (byte) 0x07, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cmp       eax, [rbx + 18]}
     */
    // Template#: 134, Serial#: 3546
    public void cmp(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0008((byte) 0x3B, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>destination</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code cmp       eax, rbx[rsi * 4]}
     */
    // Template#: 135, Serial#: 3543
    public void cmp(AMD64GeneralRegister32 destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0051((byte) 0x3B, destination, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmp       eax, eax}
     */
    // Template#: 136, Serial#: 3496
    public void cmp(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0009((byte) 0x39, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpl  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code cmpl      eax, 0x12345678}
     */
    // Template#: 137, Serial#: 784
    public void cmpl(AMD64GeneralRegister32 destination, int imm32) {
        assemble0011((byte) 0x81, (byte) 0x07, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cmp       eax, [L1: +305419896]}
     */
    // Template#: 138, Serial#: 3545
    public void rip_cmp(AMD64GeneralRegister32 destination, int rel32) {
        assemble0010((byte) 0x3B, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cmp       eax, [rbx + 305419896]}
     */
    // Template#: 139, Serial#: 3548
    public void cmp(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0012((byte) 0x3B, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpq  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code cmpq      rax, 0x12}
     */
    // Template#: 140, Serial#: 1072
    public void cmpq(AMD64GeneralRegister64 destination, byte imm8) {
        assemble0013((byte) 0x83, (byte) 0x07, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cmp       rax, [rbx + 18]}
     */
    // Template#: 141, Serial#: 3555
    public void cmp(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0014((byte) 0x3B, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmp       rax, rax}
     */
    // Template#: 142, Serial#: 3505
    public void cmp(AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0015((byte) 0x39, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpq  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code cmpq      rax, 0x12345678}
     */
    // Template#: 143, Serial#: 856
    public void cmpq(AMD64GeneralRegister64 destination, int imm32) {
        assemble0017((byte) 0x81, (byte) 0x07, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cmp       rax, [L1: +305419896]}
     */
    // Template#: 144, Serial#: 3554
    public void rip_cmp(AMD64GeneralRegister64 destination, int rel32) {
        assemble0016((byte) 0x3B, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cmp       rax, [rbx + 305419896]}
     */
    // Template#: 145, Serial#: 3557
    public void cmp(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0018((byte) 0x3B, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>imm32</i>
     * Example disassembly syntax: {@code cmp       eax, 0x12345678}
     */
    // Template#: 146, Serial#: 3572
    public void cmp_EAX(int imm32) {
        assemble0019((byte) 0x3D, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>imm32</i>
     * Example disassembly syntax: {@code cmp       rax, 0x12345678}
     */
    // Template#: 147, Serial#: 3573
    public void cmp_RAX(int imm32) {
        assemble0020((byte) 0x3D, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpl  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code cmpl      [rbx + 305419896], 0x12}
     */
    // Template#: 148, Serial#: 991
    public void cmpl(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0021((byte) 0x83, (byte) 0x07, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpq  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code cmpq      [rbx + 305419896], 0x12}
     */
    // Template#: 149, Serial#: 1063
    public void cmpq(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0022((byte) 0x83, (byte) 0x07, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmp       [rbx + 305419896], eax}
     */
    // Template#: 150, Serial#: 3494
    public void cmp(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0023((byte) 0x39, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmp       [rbx + 305419896], rax}
     */
    // Template#: 151, Serial#: 3503
    public void cmp(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0024((byte) 0x39, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpl  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code cmpl      [rbx + 305419896], 0x12345678}
     */
    // Template#: 152, Serial#: 775
    public void cmpl(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0025((byte) 0x81, (byte) 0x07, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpq  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code cmpq      [rbx + 305419896], 0x12345678}
     */
    // Template#: 153, Serial#: 847
    public void cmpq(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0026((byte) 0x81, (byte) 0x07, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpsd  }<i>destination</i>, <i>disp8</i>, <i>source</i>, <i>amd64xmmcomparison</i>
     * Example disassembly syntax: {@code cmpsd     xmm0, [rbx + 18], less_than_or_equal}
     */
    // Template#: 154, Serial#: 8140
    public void cmpsd(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source, AMD64XMMComparison amd64xmmcomparison) {
        assemble0052((byte) 0xC2, destination, disp8, source, amd64xmmcomparison);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpsd  }<i>destination</i>, <i>source</i>, <i>amd64xmmcomparison</i>
     * Example disassembly syntax: {@code cmpsd     xmm0, xmm0, less_than_or_equal}
     */
    // Template#: 155, Serial#: 8144
    public void cmpsd(AMD64XMMRegister destination, AMD64XMMRegister source, AMD64XMMComparison amd64xmmcomparison) {
        assemble0053((byte) 0xC2, destination, source, amd64xmmcomparison);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpsd  }<i>destination</i>, <i>disp32</i>, <i>source</i>, <i>amd64xmmcomparison</i>
     * Example disassembly syntax: {@code cmpsd     xmm0, [rbx + 305419896], less_than_or_equal}
     */
    // Template#: 156, Serial#: 8142
    public void cmpsd(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source, AMD64XMMComparison amd64xmmcomparison) {
        assemble0054((byte) 0xC2, destination, disp32, source, amd64xmmcomparison);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpsd  }<i>destination</i>, <i>rel32</i>, <i>amd64xmmcomparison</i>
     * Example disassembly syntax: {@code cmpsd     xmm0, [L1: +305419896], less_than_or_equal}
     */
    // Template#: 157, Serial#: 8139
    public void rip_cmpsd(AMD64XMMRegister destination, int rel32, AMD64XMMComparison amd64xmmcomparison) {
        assemble0055((byte) 0xC2, destination, rel32, amd64xmmcomparison);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpss  }<i>destination</i>, <i>disp8</i>, <i>source</i>, <i>amd64xmmcomparison</i>
     * Example disassembly syntax: {@code cmpss     xmm0, [rbx + 18], less_than_or_equal}
     */
    // Template#: 158, Serial#: 8158
    public void cmpss(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source, AMD64XMMComparison amd64xmmcomparison) {
        assemble0056((byte) 0xC2, destination, disp8, source, amd64xmmcomparison);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpss  }<i>destination</i>, <i>source</i>, <i>amd64xmmcomparison</i>
     * Example disassembly syntax: {@code cmpss     xmm0, xmm0, less_than_or_equal}
     */
    // Template#: 159, Serial#: 8162
    public void cmpss(AMD64XMMRegister destination, AMD64XMMRegister source, AMD64XMMComparison amd64xmmcomparison) {
        assemble0057((byte) 0xC2, destination, source, amd64xmmcomparison);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpss  }<i>destination</i>, <i>disp32</i>, <i>source</i>, <i>amd64xmmcomparison</i>
     * Example disassembly syntax: {@code cmpss     xmm0, [rbx + 305419896], less_than_or_equal}
     */
    // Template#: 160, Serial#: 8160
    public void cmpss(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source, AMD64XMMComparison amd64xmmcomparison) {
        assemble0058((byte) 0xC2, destination, disp32, source, amd64xmmcomparison);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpss  }<i>destination</i>, <i>rel32</i>, <i>amd64xmmcomparison</i>
     * Example disassembly syntax: {@code cmpss     xmm0, [L1: +305419896], less_than_or_equal}
     */
    // Template#: 161, Serial#: 8157
    public void rip_cmpss(AMD64XMMRegister destination, int rel32, AMD64XMMComparison amd64xmmcomparison) {
        assemble0059((byte) 0xC2, destination, rel32, amd64xmmcomparison);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpxchg  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmpxchg   [rbx + 18], eax}
     */
    // Template#: 162, Serial#: 7851
    public void cmpxchg(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0060((byte) 0xB1, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpxchg  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmpxchg   [rbx + 18], rax}
     */
    // Template#: 163, Serial#: 7860
    public void cmpxchg(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0061((byte) 0xB1, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpxchg  }<i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code cmpxchg   rbx[rsi * 4], eax}
     */
    // Template#: 164, Serial#: 7848
    public void cmpxchg(AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister32 source) {
        assemble0063((byte) 0xB1, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpxchg  }<i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code cmpxchg   rbx[rsi * 4], rax}
     */
    // Template#: 165, Serial#: 7857
    public void cmpxchg(AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister64 source) {
        assemble0065((byte) 0xB1, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpxchg  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmpxchg   [rbx], eax}
     */
    // Template#: 166, Serial#: 7847
    public void cmpxchg(AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0066((byte) 0xB1, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpxchg  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmpxchg   [rbx], rax}
     */
    // Template#: 167, Serial#: 7856
    public void cmpxchg(AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0067((byte) 0xB1, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpxchg  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmpxchg   [rbx + 305419896], eax}
     */
    // Template#: 168, Serial#: 7853
    public void cmpxchg(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0068((byte) 0xB1, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpxchg  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmpxchg   [rbx + 305419896], rax}
     */
    // Template#: 169, Serial#: 7862
    public void cmpxchg(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0069((byte) 0xB1, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code comisd  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code comisd    xmm0, [rbx + 18]}
     */
    // Template#: 170, Serial#: 9622
    public void comisd(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0070((byte) 0x2F, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code comisd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code comisd    xmm0, xmm0}
     */
    // Template#: 171, Serial#: 9626
    public void comisd(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0071((byte) 0x2F, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code comisd  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code comisd    xmm0, [L1: +305419896]}
     */
    // Template#: 172, Serial#: 9621
    public void rip_comisd(AMD64XMMRegister destination, int rel32) {
        assemble0072((byte) 0x2F, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code comisd  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code comisd    xmm0, [rbx + 305419896]}
     */
    // Template#: 173, Serial#: 9624
    public void comisd(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0073((byte) 0x2F, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code comiss  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code comiss    xmm0, [rbx + 18]}
     */
    // Template#: 174, Serial#: 9463
    public void comiss(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0074((byte) 0x2F, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code comiss  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code comiss    xmm0, xmm0}
     */
    // Template#: 175, Serial#: 9467
    public void comiss(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0075((byte) 0x2F, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code comiss  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code comiss    xmm0, [L1: +305419896]}
     */
    // Template#: 176, Serial#: 9462
    public void rip_comiss(AMD64XMMRegister destination, int rel32) {
        assemble0076((byte) 0x2F, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code comiss  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code comiss    xmm0, [rbx + 305419896]}
     */
    // Template#: 177, Serial#: 9465
    public void comiss(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0077((byte) 0x2F, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cqo  }
     * Example disassembly syntax: {@code cqo     }
     */
    // Template#: 178, Serial#: 3860
    public void cqo() {
        assemble0078((byte) 0x99);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2si  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsd2si  eax, [rbx + 18]}
     */
    // Template#: 179, Serial#: 9676
    public void cvtsd2si(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0079((byte) 0x2D, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2si  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsd2si  eax, xmm0}
     */
    // Template#: 180, Serial#: 9680
    public void cvtsd2si(AMD64GeneralRegister32 destination, AMD64XMMRegister source) {
        assemble0080((byte) 0x2D, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2si  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvtsd2si  eax, [L1: +305419896]}
     */
    // Template#: 181, Serial#: 9675
    public void rip_cvtsd2si(AMD64GeneralRegister32 destination, int rel32) {
        assemble0081((byte) 0x2D, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2si  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsd2si  eax, [rbx + 305419896]}
     */
    // Template#: 182, Serial#: 9678
    public void cvtsd2si(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0082((byte) 0x2D, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2si  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsd2si  rax, [rbx + 18]}
     */
    // Template#: 183, Serial#: 9685
    public void cvtsd2si(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0083((byte) 0x2D, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2si  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsd2si  rax, xmm0}
     */
    // Template#: 184, Serial#: 9689
    public void cvtsd2si(AMD64GeneralRegister64 destination, AMD64XMMRegister source) {
        assemble0084((byte) 0x2D, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2si  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvtsd2si  rax, [L1: +305419896]}
     */
    // Template#: 185, Serial#: 9684
    public void rip_cvtsd2si(AMD64GeneralRegister64 destination, int rel32) {
        assemble0085((byte) 0x2D, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2si  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsd2si  rax, [rbx + 305419896]}
     */
    // Template#: 186, Serial#: 9687
    public void cvtsd2si(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0086((byte) 0x2D, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2ss  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsd2ss  xmm0, [rbx + 18]}
     */
    // Template#: 187, Serial#: 10288
    public void cvtsd2ss(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0027((byte) 0x5A, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2ss  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsd2ss  xmm0, xmm0}
     */
    // Template#: 188, Serial#: 10292
    public void cvtsd2ss(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0028((byte) 0x5A, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2ss  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvtsd2ss  xmm0, [L1: +305419896]}
     */
    // Template#: 189, Serial#: 10287
    public void rip_cvtsd2ss(AMD64XMMRegister destination, int rel32) {
        assemble0029((byte) 0x5A, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2ss  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsd2ss  xmm0, [rbx + 305419896]}
     */
    // Template#: 190, Serial#: 10290
    public void cvtsd2ss(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0030((byte) 0x5A, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2sdl  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsi2sdl  xmm0, [rbx + 18]}
     */
    // Template#: 191, Serial#: 9640
    public void cvtsi2sdl(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0027((byte) 0x2A, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2sdq  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsi2sdq  xmm0, [rbx + 18]}
     */
    // Template#: 192, Serial#: 9649
    public void cvtsi2sdq(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0087((byte) 0x2A, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2sdl  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsi2sdl  xmm0, eax}
     */
    // Template#: 193, Serial#: 9644
    public void cvtsi2sdl(AMD64XMMRegister destination, AMD64GeneralRegister32 source) {
        assemble0088((byte) 0x2A, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2sdq  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsi2sdq  xmm0, rax}
     */
    // Template#: 194, Serial#: 9653
    public void cvtsi2sdq(AMD64XMMRegister destination, AMD64GeneralRegister64 source) {
        assemble0089((byte) 0x2A, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2sdl  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvtsi2sdl  xmm0, [L1: +305419896]}
     */
    // Template#: 195, Serial#: 9639
    public void rip_cvtsi2sdl(AMD64XMMRegister destination, int rel32) {
        assemble0029((byte) 0x2A, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2sdq  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvtsi2sdq  xmm0, [L1: +305419896]}
     */
    // Template#: 196, Serial#: 9648
    public void rip_cvtsi2sdq(AMD64XMMRegister destination, int rel32) {
        assemble0090((byte) 0x2A, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2sdl  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsi2sdl  xmm0, [rbx + 305419896]}
     */
    // Template#: 197, Serial#: 9642
    public void cvtsi2sdl(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0030((byte) 0x2A, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2sdq  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsi2sdq  xmm0, [rbx + 305419896]}
     */
    // Template#: 198, Serial#: 9651
    public void cvtsi2sdq(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0091((byte) 0x2A, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2ssl  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsi2ssl  xmm0, [rbx + 18]}
     */
    // Template#: 199, Serial#: 9694
    public void cvtsi2ssl(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0031((byte) 0x2A, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2ssq  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsi2ssq  xmm0, [rbx + 18]}
     */
    // Template#: 200, Serial#: 9703
    public void cvtsi2ssq(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0092((byte) 0x2A, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2ssl  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsi2ssl  xmm0, eax}
     */
    // Template#: 201, Serial#: 9698
    public void cvtsi2ssl(AMD64XMMRegister destination, AMD64GeneralRegister32 source) {
        assemble0093((byte) 0x2A, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2ssq  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsi2ssq  xmm0, rax}
     */
    // Template#: 202, Serial#: 9707
    public void cvtsi2ssq(AMD64XMMRegister destination, AMD64GeneralRegister64 source) {
        assemble0094((byte) 0x2A, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2ssl  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvtsi2ssl  xmm0, [L1: +305419896]}
     */
    // Template#: 203, Serial#: 9693
    public void rip_cvtsi2ssl(AMD64XMMRegister destination, int rel32) {
        assemble0033((byte) 0x2A, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2ssq  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvtsi2ssq  xmm0, [L1: +305419896]}
     */
    // Template#: 204, Serial#: 9702
    public void rip_cvtsi2ssq(AMD64XMMRegister destination, int rel32) {
        assemble0095((byte) 0x2A, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2ssl  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsi2ssl  xmm0, [rbx + 305419896]}
     */
    // Template#: 205, Serial#: 9696
    public void cvtsi2ssl(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0034((byte) 0x2A, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2ssq  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsi2ssq  xmm0, [rbx + 305419896]}
     */
    // Template#: 206, Serial#: 9705
    public void cvtsi2ssq(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0096((byte) 0x2A, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2sd  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtss2sd  xmm0, [rbx + 18]}
     */
    // Template#: 207, Serial#: 10414
    public void cvtss2sd(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0031((byte) 0x5A, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2sd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtss2sd  xmm0, xmm0}
     */
    // Template#: 208, Serial#: 10418
    public void cvtss2sd(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0032((byte) 0x5A, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2sd  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvtss2sd  xmm0, [L1: +305419896]}
     */
    // Template#: 209, Serial#: 10413
    public void rip_cvtss2sd(AMD64XMMRegister destination, int rel32) {
        assemble0033((byte) 0x5A, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2sd  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtss2sd  xmm0, [rbx + 305419896]}
     */
    // Template#: 210, Serial#: 10416
    public void cvtss2sd(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0034((byte) 0x5A, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2si  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtss2si  eax, [rbx + 18]}
     */
    // Template#: 211, Serial#: 9730
    public void cvtss2si(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0097((byte) 0x2D, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2si  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtss2si  eax, xmm0}
     */
    // Template#: 212, Serial#: 9734
    public void cvtss2si(AMD64GeneralRegister32 destination, AMD64XMMRegister source) {
        assemble0098((byte) 0x2D, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2si  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvtss2si  eax, [L1: +305419896]}
     */
    // Template#: 213, Serial#: 9729
    public void rip_cvtss2si(AMD64GeneralRegister32 destination, int rel32) {
        assemble0099((byte) 0x2D, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2si  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtss2si  eax, [rbx + 305419896]}
     */
    // Template#: 214, Serial#: 9732
    public void cvtss2si(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0100((byte) 0x2D, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2si  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtss2si  rax, [rbx + 18]}
     */
    // Template#: 215, Serial#: 9739
    public void cvtss2si(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0101((byte) 0x2D, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2si  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtss2si  rax, xmm0}
     */
    // Template#: 216, Serial#: 9743
    public void cvtss2si(AMD64GeneralRegister64 destination, AMD64XMMRegister source) {
        assemble0102((byte) 0x2D, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2si  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvtss2si  rax, [L1: +305419896]}
     */
    // Template#: 217, Serial#: 9738
    public void rip_cvtss2si(AMD64GeneralRegister64 destination, int rel32) {
        assemble0103((byte) 0x2D, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2si  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtss2si  rax, [rbx + 305419896]}
     */
    // Template#: 218, Serial#: 9741
    public void cvtss2si(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0104((byte) 0x2D, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttsd2si  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvttsd2si  eax, xmm0}
     */
    // Template#: 219, Serial#: 9662
    public void cvttsd2si(AMD64GeneralRegister32 destination, AMD64XMMRegister source) {
        assemble0080((byte) 0x2C, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttsd2si  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvttsd2si  eax, [L1: +305419896]}
     */
    // Template#: 220, Serial#: 9657
    public void rip_cvttsd2si(AMD64GeneralRegister32 destination, int rel32) {
        assemble0081((byte) 0x2C, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttsd2si  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvttsd2si  eax, [rbx + 305419896]}
     */
    // Template#: 221, Serial#: 9660
    public void cvttsd2si(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0082((byte) 0x2C, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttsd2si  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cvttsd2si  rax, [rbx + 18]}
     */
    // Template#: 222, Serial#: 9667
    public void cvttsd2si(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0083((byte) 0x2C, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttsd2si  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvttsd2si  rax, xmm0}
     */
    // Template#: 223, Serial#: 9671
    public void cvttsd2si(AMD64GeneralRegister64 destination, AMD64XMMRegister source) {
        assemble0084((byte) 0x2C, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttsd2si  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvttsd2si  rax, [L1: +305419896]}
     */
    // Template#: 224, Serial#: 9666
    public void rip_cvttsd2si(AMD64GeneralRegister64 destination, int rel32) {
        assemble0085((byte) 0x2C, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttsd2si  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvttsd2si  rax, [rbx + 305419896]}
     */
    // Template#: 225, Serial#: 9669
    public void cvttsd2si(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0086((byte) 0x2C, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttss2si  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cvttss2si  eax, [rbx + 18]}
     */
    // Template#: 226, Serial#: 9712
    public void cvttss2si(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0097((byte) 0x2C, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttss2si  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvttss2si  eax, xmm0}
     */
    // Template#: 227, Serial#: 9716
    public void cvttss2si(AMD64GeneralRegister32 destination, AMD64XMMRegister source) {
        assemble0098((byte) 0x2C, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttss2si  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvttss2si  eax, [L1: +305419896]}
     */
    // Template#: 228, Serial#: 9711
    public void rip_cvttss2si(AMD64GeneralRegister32 destination, int rel32) {
        assemble0099((byte) 0x2C, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttss2si  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvttss2si  eax, [rbx + 305419896]}
     */
    // Template#: 229, Serial#: 9714
    public void cvttss2si(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0100((byte) 0x2C, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttss2si  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cvttss2si  rax, [rbx + 18]}
     */
    // Template#: 230, Serial#: 9721
    public void cvttss2si(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0101((byte) 0x2C, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttss2si  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvttss2si  rax, xmm0}
     */
    // Template#: 231, Serial#: 9725
    public void cvttss2si(AMD64GeneralRegister64 destination, AMD64XMMRegister source) {
        assemble0102((byte) 0x2C, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttss2si  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvttss2si  rax, [L1: +305419896]}
     */
    // Template#: 232, Serial#: 9720
    public void rip_cvttss2si(AMD64GeneralRegister64 destination, int rel32) {
        assemble0103((byte) 0x2C, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttss2si  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvttss2si  rax, [rbx + 305419896]}
     */
    // Template#: 233, Serial#: 9723
    public void cvttss2si(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0104((byte) 0x2C, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code decq  }<i>destination</i>
     * Example disassembly syntax: {@code decq      rax}
     */
    // Template#: 234, Serial#: 5410
    public void decq(AMD64GeneralRegister64 destination) {
        assemble0105((byte) 0xFF, (byte) 0x01, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code divq  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code divq      [rbx + 18]}
     */
    // Template#: 235, Serial#: 3013
    public void divq(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0106((byte) 0xF7, (byte) 0x06, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code divq  }<i>destination</i>
     * Example disassembly syntax: {@code divq      rax}
     */
    // Template#: 236, Serial#: 3039
    public void divq(AMD64GeneralRegister64 destination) {
        assemble0105((byte) 0xF7, (byte) 0x06, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code divq  }<i>rel32</i>
     * Example disassembly syntax: {@code divq      [L1: +305419896]}
     */
    // Template#: 237, Serial#: 2996
    public void rip_divq(int rel32) {
        assemble0107((byte) 0xF7, (byte) 0x06, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code divq  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code divq      [rbx + 305419896]}
     */
    // Template#: 238, Serial#: 3029
    public void divq(int disp32, AMD64IndirectRegister64 destination) {
        assemble0108((byte) 0xF7, (byte) 0x06, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code divsd  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code divsd     xmm0, [rbx + 18]}
     */
    // Template#: 239, Serial#: 10342
    public void divsd(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0027((byte) 0x5E, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code divsd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code divsd     xmm0, xmm0}
     */
    // Template#: 240, Serial#: 10346
    public void divsd(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0028((byte) 0x5E, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code divsd  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code divsd     xmm0, [L1: +305419896]}
     */
    // Template#: 241, Serial#: 10341
    public void rip_divsd(AMD64XMMRegister destination, int rel32) {
        assemble0029((byte) 0x5E, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code divsd  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code divsd     xmm0, [rbx + 305419896]}
     */
    // Template#: 242, Serial#: 10344
    public void divsd(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0030((byte) 0x5E, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code divss  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code divss     xmm0, [rbx + 18]}
     */
    // Template#: 243, Serial#: 10486
    public void divss(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0031((byte) 0x5E, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code divss  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code divss     xmm0, xmm0}
     */
    // Template#: 244, Serial#: 10490
    public void divss(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0032((byte) 0x5E, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code divss  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code divss     xmm0, [L1: +305419896]}
     */
    // Template#: 245, Serial#: 10485
    public void rip_divss(AMD64XMMRegister destination, int rel32) {
        assemble0033((byte) 0x5E, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code divss  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code divss     xmm0, [rbx + 305419896]}
     */
    // Template#: 246, Serial#: 10488
    public void divss(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0034((byte) 0x5E, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code enter  }<i>imm16</i>, <i>imm8</i>
     * Example disassembly syntax: {@code enter     0x1234, 0x12}
     */
    // Template#: 247, Serial#: 3901
    public void enter(short imm16, byte imm8) {
        assemble0109((byte) 0xC8, imm16, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code idivl  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code idivl     [rbx + 18]}
     */
    // Template#: 248, Serial#: 2943
    public void idivl(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0043((byte) 0xF7, (byte) 0x07, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code idivq  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code idivq     [rbx + 18]}
     */
    // Template#: 249, Serial#: 3015
    public void idivq(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0106((byte) 0xF7, (byte) 0x07, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code idivl  }<i>destination</i>
     * Example disassembly syntax: {@code idivl     eax}
     */
    // Template#: 250, Serial#: 2968
    public void idivl(AMD64GeneralRegister32 destination) {
        assemble0110((byte) 0xF7, (byte) 0x07, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code idivq  }<i>destination</i>
     * Example disassembly syntax: {@code idivq     rax}
     */
    // Template#: 251, Serial#: 3040
    public void idivq(AMD64GeneralRegister64 destination) {
        assemble0105((byte) 0xF7, (byte) 0x07, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code idivl  }<i>rel32</i>
     * Example disassembly syntax: {@code idivl     [L1: +305419896]}
     */
    // Template#: 252, Serial#: 2928
    public void rip_idivl(int rel32) {
        assemble0046((byte) 0xF7, (byte) 0x07, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code idivq  }<i>rel32</i>
     * Example disassembly syntax: {@code idivq     [L1: +305419896]}
     */
    // Template#: 253, Serial#: 3000
    public void rip_idivq(int rel32) {
        assemble0107((byte) 0xF7, (byte) 0x07, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code idivl  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code idivl     [rbx + 305419896]}
     */
    // Template#: 254, Serial#: 2959
    public void idivl(int disp32, AMD64IndirectRegister64 destination) {
        assemble0047((byte) 0xF7, (byte) 0x07, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code idivq  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code idivq     [rbx + 305419896]}
     */
    // Template#: 255, Serial#: 3031
    public void idivq(int disp32, AMD64IndirectRegister64 destination) {
        assemble0108((byte) 0xF7, (byte) 0x07, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code imull  }<i>destination</i>
     * Example disassembly syntax: {@code imull     eax}
     */
    // Template#: 256, Serial#: 2966
    public void imull(AMD64GeneralRegister32 destination) {
        assemble0110((byte) 0xF7, (byte) 0x05, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code imul      eax, [rbx + 18]}
     */
    // Template#: 257, Serial#: 11467
    public void imul(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0035((byte) 0xAF, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code imul      eax, eax}
     */
    // Template#: 258, Serial#: 11471
    public void imul(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0036((byte) 0xAF, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>source</i>, <i>imm8</i>
     * Example disassembly syntax: {@code imul      eax, eax, 0x12}
     */
    // Template#: 259, Serial#: 3616
    public void imul(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source, byte imm8) {
        assemble0111((byte) 0x6B, destination, source, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>source</i>, <i>imm32</i>
     * Example disassembly syntax: {@code imul      eax, eax, 0x12345678}
     */
    // Template#: 260, Serial#: 3587
    public void imul(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source, int imm32) {
        assemble0112((byte) 0x69, destination, source, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code imul      eax, [L1: +305419896]}
     */
    // Template#: 261, Serial#: 11466
    public void rip_imul(AMD64GeneralRegister32 destination, int rel32) {
        assemble0049((byte) 0xAF, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code imul      eax, [rbx + 305419896]}
     */
    // Template#: 262, Serial#: 11469
    public void imul(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0038((byte) 0xAF, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code imulq  }<i>destination</i>
     * Example disassembly syntax: {@code imulq     rax}
     */
    // Template#: 263, Serial#: 3038
    public void imulq(AMD64GeneralRegister64 destination) {
        assemble0105((byte) 0xF7, (byte) 0x05, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code imul      rax, [rbx + 18]}
     */
    // Template#: 264, Serial#: 11476
    public void imul(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0039((byte) 0xAF, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code imul      rax, rax}
     */
    // Template#: 265, Serial#: 11480
    public void imul(AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0040((byte) 0xAF, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>source</i>, <i>imm8</i>
     * Example disassembly syntax: {@code imul      rax, rax, 0x12}
     */
    // Template#: 266, Serial#: 3625
    public void imul(AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source, byte imm8) {
        assemble0113((byte) 0x6B, destination, source, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>source</i>, <i>imm32</i>
     * Example disassembly syntax: {@code imul      rax, rax, 0x12345678}
     */
    // Template#: 267, Serial#: 3596
    public void imul(AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source, int imm32) {
        assemble0114((byte) 0x69, destination, source, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code imul      rax, [L1: +305419896]}
     */
    // Template#: 268, Serial#: 11475
    public void rip_imul(AMD64GeneralRegister64 destination, int rel32) {
        assemble0115((byte) 0xAF, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code imul      rax, [rbx + 305419896]}
     */
    // Template#: 269, Serial#: 11478
    public void imul(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0042((byte) 0xAF, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code int  }
     * Example disassembly syntax: {@code int       0x3}
     */
    // Template#: 270, Serial#: 3911
    public void int_3() {
        assemble0048((byte) 0xCC);
    }

    /**
     * Pseudo-external assembler syntax: {@code jb  }<i>rel8</i>
     * Example disassembly syntax: {@code jb        L1: +18}
     */
    // Template#: 271, Serial#: 491
    public void jb(byte rel8) {
        assemble0116((byte) 0x72, rel8);
    }

    /**
     * Pseudo-external assembler syntax: {@code jb  }<i>rel32</i>
     * Example disassembly syntax: {@code jb        L1: +305419896}
     */
    // Template#: 272, Serial#: 7510
    public void jb(int rel32) {
        assemble0117((byte) 0x82, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code jbe  }<i>rel8</i>
     * Example disassembly syntax: {@code jbe       L1: +18}
     */
    // Template#: 273, Serial#: 495
    public void jbe(byte rel8) {
        assemble0116((byte) 0x76, rel8);
    }

    /**
     * Pseudo-external assembler syntax: {@code jbe  }<i>rel32</i>
     * Example disassembly syntax: {@code jbe       L1: +305419896}
     */
    // Template#: 274, Serial#: 7514
    public void jbe(int rel32) {
        assemble0117((byte) 0x86, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code jl  }<i>rel8</i>
     * Example disassembly syntax: {@code jl        L1: +18}
     */
    // Template#: 275, Serial#: 3649
    public void jl(byte rel8) {
        assemble0116((byte) 0x7C, rel8);
    }

    /**
     * Pseudo-external assembler syntax: {@code jl  }<i>rel32</i>
     * Example disassembly syntax: {@code jl        L1: +305419896}
     */
    // Template#: 276, Serial#: 11026
    public void jl(int rel32) {
        assemble0117((byte) 0x8C, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code jle  }<i>rel8</i>
     * Example disassembly syntax: {@code jle       L1: +18}
     */
    // Template#: 277, Serial#: 3651
    public void jle(byte rel8) {
        assemble0116((byte) 0x7E, rel8);
    }

    /**
     * Pseudo-external assembler syntax: {@code jle  }<i>rel32</i>
     * Example disassembly syntax: {@code jle       L1: +305419896}
     */
    // Template#: 278, Serial#: 11028
    public void jle(int rel32) {
        assemble0117((byte) 0x8E, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code jmp  }<i>rel8</i>
     * Example disassembly syntax: {@code jmp       L1: +18}
     */
    // Template#: 279, Serial#: 5290
    public void jmp(byte rel8) {
        assemble0116((byte) 0xEB, rel8);
    }

    /**
     * Pseudo-external assembler syntax: {@code jmp  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code jmp       [rbx + 18]}
     */
    // Template#: 280, Serial#: 5443
    public void jmp(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0043((byte) 0xFF, (byte) 0x04, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code jmp  }<i>destination</i>
     * Example disassembly syntax: {@code jmp       rax}
     */
    // Template#: 281, Serial#: 5454
    public void jmp(AMD64GeneralRegister64 destination) {
        assemble0044((byte) 0xFF, (byte) 0x04, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code jmp  }<i>rel32</i>
     * Example disassembly syntax: {@code jmp       L1: +305419896}
     */
    // Template#: 282, Serial#: 5289
    public void jmp(int rel32) {
        assemble0045((byte) 0xE9, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code jmp  }<i>rel32</i>
     * Example disassembly syntax: {@code jmp       [L1: +305419896]}
     */
    // Template#: 283, Serial#: 5436
    public void rip_jmp(int rel32) {
        assemble0046((byte) 0xFF, (byte) 0x04, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code jmp  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code jmp       [rbx + 305419896]}
     */
    // Template#: 284, Serial#: 5449
    public void jmp(int disp32, AMD64IndirectRegister64 destination) {
        assemble0047((byte) 0xFF, (byte) 0x04, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnb  }<i>rel8</i>
     * Example disassembly syntax: {@code jnb       L1: +18}
     */
    // Template#: 285, Serial#: 492
    public void jnb(byte rel8) {
        assemble0116((byte) 0x73, rel8);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnb  }<i>rel32</i>
     * Example disassembly syntax: {@code jnb       L1: +305419896}
     */
    // Template#: 286, Serial#: 7511
    public void jnb(int rel32) {
        assemble0117((byte) 0x83, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnbe  }<i>rel8</i>
     * Example disassembly syntax: {@code jnbe      L1: +18}
     */
    // Template#: 287, Serial#: 496
    public void jnbe(byte rel8) {
        assemble0116((byte) 0x77, rel8);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnbe  }<i>rel32</i>
     * Example disassembly syntax: {@code jnbe      L1: +305419896}
     */
    // Template#: 288, Serial#: 7515
    public void jnbe(int rel32) {
        assemble0117((byte) 0x87, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnl  }<i>rel8</i>
     * Example disassembly syntax: {@code jnl       L1: +18}
     */
    // Template#: 289, Serial#: 3650
    public void jnl(byte rel8) {
        assemble0116((byte) 0x7D, rel8);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnl  }<i>rel32</i>
     * Example disassembly syntax: {@code jnl       L1: +305419896}
     */
    // Template#: 290, Serial#: 11027
    public void jnl(int rel32) {
        assemble0117((byte) 0x8D, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnle  }<i>rel8</i>
     * Example disassembly syntax: {@code jnle      L1: +18}
     */
    // Template#: 291, Serial#: 3652
    public void jnle(byte rel8) {
        assemble0116((byte) 0x7F, rel8);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnle  }<i>rel32</i>
     * Example disassembly syntax: {@code jnle      L1: +305419896}
     */
    // Template#: 292, Serial#: 11029
    public void jnle(int rel32) {
        assemble0117((byte) 0x8F, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnz  }<i>rel8</i>
     * Example disassembly syntax: {@code jnz       L1: +18}
     */
    // Template#: 293, Serial#: 494
    public void jnz(byte rel8) {
        assemble0116((byte) 0x75, rel8);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnz  }<i>rel32</i>
     * Example disassembly syntax: {@code jnz       L1: +305419896}
     */
    // Template#: 294, Serial#: 7513
    public void jnz(int rel32) {
        assemble0117((byte) 0x85, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code jz  }<i>rel8</i>
     * Example disassembly syntax: {@code jz        L1: +18}
     */
    // Template#: 295, Serial#: 493
    public void jz(byte rel8) {
        assemble0116((byte) 0x74, rel8);
    }

    /**
     * Pseudo-external assembler syntax: {@code jz  }<i>rel32</i>
     * Example disassembly syntax: {@code jz        L1: +305419896}
     */
    // Template#: 296, Serial#: 7512
    public void jz(int rel32) {
        assemble0117((byte) 0x84, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code lea  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code lea       rax, [rbx + 18]}
     */
    // Template#: 297, Serial#: 3800
    public void lea(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0014((byte) 0x8D, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code lea  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code lea       rax, [rbx]}
     */
    // Template#: 298, Serial#: 3796
    public void lea(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0118((byte) 0x8D, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code lea  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code lea       rax, [L1: +305419896]}
     */
    // Template#: 299, Serial#: 3799
    public void rip_lea(AMD64GeneralRegister64 destination, int rel32) {
        assemble0016((byte) 0x8D, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code lea  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code lea       rax, [rbx + 305419896]}
     */
    // Template#: 300, Serial#: 3802
    public void lea(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0018((byte) 0x8D, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code leave  }
     * Example disassembly syntax: {@code leave   }
     */
    // Template#: 301, Serial#: 3903
    public void leave() {
        assemble0048((byte) 0xC9);
    }

    /**
     * Pseudo-external assembler syntax: {@code lfence  }
     * Example disassembly syntax: {@code lfence  }
     */
    // Template#: 302, Serial#: 11454
    public void lfence() {
        assemble0119((byte) 0xAE, (byte) 0x05);
    }

    /**
     * Pseudo-external assembler syntax: {@code lock  }
     * Example disassembly syntax: {@code lock    }
     */
    // Template#: 303, Serial#: 2663
    public void lock() {
        assemble0048((byte) 0xF0);
    }

    /**
     * Pseudo-external assembler syntax: {@code mfence  }
     * Example disassembly syntax: {@code mfence  }
     */
    // Template#: 304, Serial#: 11455
    public void mfence() {
        assemble0119((byte) 0xAE, (byte) 0x06);
    }

    /**
     * Pseudo-external assembler syntax: {@code movb  }<i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>imm8</i>
     * Example disassembly syntax: {@code movb      rbx[rsi * 4 + 18], 0x12}
     */
    // Template#: 305, Serial#: 1727
    public void movb(byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, byte imm8) {
        assemble0120((byte) 0xC6, disp8, base, index, scale, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rbx[rsi * 4 + 18], ax}
     */
    // Template#: 306, Serial#: 3703
    public void mov(byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister16 source) {
        assemble0121((byte) 0x89, disp8, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rbx[rsi * 4 + 18], eax}
     */
    // Template#: 307, Serial#: 3685
    public void mov(byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister32 source) {
        assemble0122((byte) 0x89, disp8, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rbx[rsi * 4 + 18], rax}
     */
    // Template#: 308, Serial#: 3694
    public void mov(byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister64 source) {
        assemble0123((byte) 0x89, disp8, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rbx[rsi * 4 + 18], al}
     */
    // Template#: 309, Serial#: 3658
    public void mov(byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister8 source) {
        assemble0124((byte) 0x88, disp8, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movl  }<i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>imm32</i>
     * Example disassembly syntax: {@code movl      rbx[rsi * 4 + 18], 0x12345678}
     */
    // Template#: 310, Serial#: 1754
    public void movl(byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, int imm32) {
        assemble0125((byte) 0xC7, disp8, base, index, scale, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movw  }<i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>imm16</i>
     * Example disassembly syntax: {@code movw      rbx[rsi * 4 + 18], 0x1234}
     */
    // Template#: 311, Serial#: 1772
    public void movw(byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, short imm16) {
        assemble0126((byte) 0xC7, disp8, base, index, scale, imm16);
    }

    /**
     * Pseudo-external assembler syntax: {@code movb  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code movb      [rbx + 18], 0x12}
     */
    // Template#: 312, Serial#: 1726
    public void movb(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0127((byte) 0xC6, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [rbx + 18], ax}
     */
    // Template#: 313, Serial#: 3702
    public void mov(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister16 source) {
        assemble0128((byte) 0x89, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [rbx + 18], eax}
     */
    // Template#: 314, Serial#: 3684
    public void mov(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0003((byte) 0x89, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [rbx + 18], rax}
     */
    // Template#: 315, Serial#: 3693
    public void mov(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0004((byte) 0x89, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [rbx + 18], al}
     */
    // Template#: 316, Serial#: 3657
    public void mov(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister8 source) {
        assemble0129((byte) 0x88, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movl  }<i>disp8</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code movl      [rbx + 18], 0x12345678}
     */
    // Template#: 317, Serial#: 1753
    public void movl(byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        assemble0130((byte) 0xC7, disp8, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movw  }<i>disp8</i>, <i>destination</i>, <i>imm16</i>
     * Example disassembly syntax: {@code movw      [rbx + 18], 0x1234}
     */
    // Template#: 318, Serial#: 1771
    public void movw(byte disp8, AMD64IndirectRegister64 destination, short imm16) {
        assemble0131((byte) 0xC7, disp8, destination, imm16);
    }

    /**
     * Pseudo-external assembler syntax: {@code movb  }<i>base</i>, <i>index</i>, <i>scale</i>, <i>imm8</i>
     * Example disassembly syntax: {@code movb      rbx[rsi * 4], 0x12}
     */
    // Template#: 319, Serial#: 1723
    public void movb(AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, byte imm8) {
        assemble0132((byte) 0xC6, base, index, scale, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rbx[rsi * 4], ax}
     */
    // Template#: 320, Serial#: 3699
    public void mov(AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister16 source) {
        assemble0133((byte) 0x89, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rbx[rsi * 4], eax}
     */
    // Template#: 321, Serial#: 3681
    public void mov(AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister32 source) {
        assemble0134((byte) 0x89, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rbx[rsi * 4], rax}
     */
    // Template#: 322, Serial#: 3690
    public void mov(AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister64 source) {
        assemble0135((byte) 0x89, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rbx[rsi * 4], al}
     */
    // Template#: 323, Serial#: 3654
    public void mov(AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister8 source) {
        assemble0136((byte) 0x88, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movl  }<i>base</i>, <i>index</i>, <i>scale</i>, <i>imm32</i>
     * Example disassembly syntax: {@code movl      rbx[rsi * 4], 0x12345678}
     */
    // Template#: 324, Serial#: 1750
    public void movl(AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, int imm32) {
        assemble0137((byte) 0xC7, base, index, scale, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movw  }<i>base</i>, <i>index</i>, <i>scale</i>, <i>imm16</i>
     * Example disassembly syntax: {@code movw      rbx[rsi * 4], 0x1234}
     */
    // Template#: 325, Serial#: 1768
    public void movw(AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, short imm16) {
        assemble0138((byte) 0xC7, base, index, scale, imm16);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code mov       eax, rbx[rsi * 4 + 18]}
     */
    // Template#: 326, Serial#: 3739
    public void mov(AMD64GeneralRegister32 destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0050((byte) 0x8B, destination, disp8, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code mov       eax, rbx[rsi * 4]}
     */
    // Template#: 327, Serial#: 3735
    public void mov(AMD64GeneralRegister32 destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0051((byte) 0x8B, destination, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movl  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code movl      eax, 0x12345678}
     */
    // Template#: 328, Serial#: 1757
    public void movl(AMD64GeneralRegister32 destination, int imm32) {
        assemble0139((byte) 0xC7, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>register</i>, <i>imm32</i>
     * Example disassembly syntax: {@code mov       eax, 0x12345678}
     */
    // Template#: 329, Serial#: 3898
    public void mov(AMD64GeneralRegister32 register, int imm32) {
        assemble0140((byte) 0xB8, register, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       eax, [rbx + 305419896]}
     */
    // Template#: 330, Serial#: 3740
    public void mov(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0012((byte) 0x8B, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code mov       rax, rbx[rsi * 4 + 18]}
     */
    // Template#: 331, Serial#: 3748
    public void mov(AMD64GeneralRegister64 destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0141((byte) 0x8B, destination, disp8, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rax, [rbx + 18]}
     */
    // Template#: 332, Serial#: 3747
    public void mov(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0014((byte) 0x8B, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code mov       rax, rbx[rsi * 4]}
     */
    // Template#: 333, Serial#: 3744
    public void mov(AMD64GeneralRegister64 destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0142((byte) 0x8B, destination, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rax, rax}
     */
    // Template#: 334, Serial#: 3697
    public void mov(AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0015((byte) 0x89, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rax, [rbx]}
     */
    // Template#: 335, Serial#: 3743
    public void mov(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0118((byte) 0x8B, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movq  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code movq      rax, 0x12345678}
     */
    // Template#: 336, Serial#: 1766
    public void movq(AMD64GeneralRegister64 destination, int imm32) {
        assemble0143((byte) 0xC7, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code mov       rax, [L1: +305419896]}
     */
    // Template#: 337, Serial#: 3746
    public void rip_mov(AMD64GeneralRegister64 destination, int rel32) {
        assemble0016((byte) 0x8B, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code mov       rax, rbx[rsi * 4 + 305419896]}
     */
    // Template#: 338, Serial#: 3750
    public void mov(AMD64GeneralRegister64 destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0144((byte) 0x8B, destination, disp32, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rax, [rbx + 305419896]}
     */
    // Template#: 339, Serial#: 3749
    public void mov(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0018((byte) 0x8B, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>register</i>, <i>imm64</i>
     * Example disassembly syntax: {@code mov       rax, 0x123456789ABCDE}
     */
    // Template#: 340, Serial#: 3899
    public void mov(AMD64GeneralRegister64 register, long imm64) {
        assemble0145((byte) 0xB8, register, imm64);
    }

    /**
     * Pseudo-external assembler syntax: {@code movb  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code movb      [rbx], 0x12}
     */
    // Template#: 341, Serial#: 1722
    public void movb(AMD64IndirectRegister64 destination, byte imm8) {
        assemble0146((byte) 0xC6, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [rbx], ax}
     */
    // Template#: 342, Serial#: 3698
    public void mov(AMD64IndirectRegister64 destination, AMD64GeneralRegister16 source) {
        assemble0147((byte) 0x89, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [rbx], eax}
     */
    // Template#: 343, Serial#: 3680
    public void mov(AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0148((byte) 0x89, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [rbx], rax}
     */
    // Template#: 344, Serial#: 3689
    public void mov(AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0149((byte) 0x89, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [rbx], al}
     */
    // Template#: 345, Serial#: 3653
    public void mov(AMD64IndirectRegister64 destination, AMD64GeneralRegister8 source) {
        assemble0150((byte) 0x88, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movl  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code movl      [rbx], 0x12345678}
     */
    // Template#: 346, Serial#: 1749
    public void movl(AMD64IndirectRegister64 destination, int imm32) {
        assemble0151((byte) 0xC7, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movw  }<i>destination</i>, <i>imm16</i>
     * Example disassembly syntax: {@code movw      [rbx], 0x1234}
     */
    // Template#: 347, Serial#: 1767
    public void movw(AMD64IndirectRegister64 destination, short imm16) {
        assemble0152((byte) 0xC7, destination, imm16);
    }

    /**
     * Pseudo-external assembler syntax: {@code movb  }<i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>imm8</i>
     * Example disassembly syntax: {@code movb      rbx[rsi * 4 + 305419896], 0x12}
     */
    // Template#: 348, Serial#: 1729
    public void movb(int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, byte imm8) {
        assemble0153((byte) 0xC6, disp32, base, index, scale, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rbx[rsi * 4 + 305419896], ax}
     */
    // Template#: 349, Serial#: 3705
    public void mov(int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister16 source) {
        assemble0154((byte) 0x89, disp32, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rbx[rsi * 4 + 305419896], eax}
     */
    // Template#: 350, Serial#: 3687
    public void mov(int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister32 source) {
        assemble0155((byte) 0x89, disp32, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rbx[rsi * 4 + 305419896], rax}
     */
    // Template#: 351, Serial#: 3696
    public void mov(int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister64 source) {
        assemble0156((byte) 0x89, disp32, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rbx[rsi * 4 + 305419896], al}
     */
    // Template#: 352, Serial#: 3660
    public void mov(int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister8 source) {
        assemble0157((byte) 0x88, disp32, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movl  }<i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>imm32</i>
     * Example disassembly syntax: {@code movl      rbx[rsi * 4 + 305419896], 0x12345678}
     */
    // Template#: 353, Serial#: 1756
    public void movl(int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, int imm32) {
        assemble0158((byte) 0xC7, disp32, base, index, scale, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movw  }<i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>imm16</i>
     * Example disassembly syntax: {@code movw      rbx[rsi * 4 + 305419896], 0x1234}
     */
    // Template#: 354, Serial#: 1774
    public void movw(int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, short imm16) {
        assemble0159((byte) 0xC7, disp32, base, index, scale, imm16);
    }

    /**
     * Pseudo-external assembler syntax: {@code movb  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code movb      [rbx + 305419896], 0x12}
     */
    // Template#: 355, Serial#: 1728
    public void movb(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0160((byte) 0xC6, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [rbx + 305419896], ax}
     */
    // Template#: 356, Serial#: 3704
    public void mov(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister16 source) {
        assemble0161((byte) 0x89, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [rbx + 305419896], eax}
     */
    // Template#: 357, Serial#: 3686
    public void mov(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0023((byte) 0x89, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [rbx + 305419896], rax}
     */
    // Template#: 358, Serial#: 3695
    public void mov(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0024((byte) 0x89, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [rbx + 305419896], al}
     */
    // Template#: 359, Serial#: 3659
    public void mov(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister8 source) {
        assemble0162((byte) 0x88, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movl  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code movl      [rbx + 305419896], 0x12345678}
     */
    // Template#: 360, Serial#: 1755
    public void movl(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0163((byte) 0xC7, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movq  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code movq      [rbx + 305419896], 0x12345678}
     */
    // Template#: 361, Serial#: 1764
    public void movq(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0164((byte) 0xC7, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movw  }<i>disp32</i>, <i>destination</i>, <i>imm16</i>
     * Example disassembly syntax: {@code movw      [rbx + 305419896], 0x1234}
     */
    // Template#: 362, Serial#: 1773
    public void movw(int disp32, AMD64IndirectRegister64 destination, short imm16) {
        assemble0165((byte) 0xC7, disp32, destination, imm16);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdl  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movdl     [rbx + 18], xmm0}
     */
    // Template#: 363, Serial#: 10918
    public void movdl(byte disp8, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        assemble0166((byte) 0x7E, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdq  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movdq     [rbx + 18], xmm0}
     */
    // Template#: 364, Serial#: 10927
    public void movdq(byte disp8, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        assemble0167((byte) 0x7E, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdl  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movdl     eax, xmm0}
     */
    // Template#: 365, Serial#: 10922
    public void movdl(AMD64GeneralRegister32 destination, AMD64XMMRegister source) {
        assemble0168((byte) 0x7E, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdq  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movdq     rax, xmm0}
     */
    // Template#: 366, Serial#: 10931
    public void movdq(AMD64GeneralRegister64 destination, AMD64XMMRegister source) {
        assemble0169((byte) 0x7E, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdl  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code movdl     xmm0, [rbx + 18]}
     */
    // Template#: 367, Serial#: 10783
    public void movdl(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0070((byte) 0x6E, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdl  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movdl     xmm0, eax}
     */
    // Template#: 368, Serial#: 10787
    public void movdl(AMD64XMMRegister destination, AMD64GeneralRegister32 source) {
        assemble0170((byte) 0x6E, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdq  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movdq     xmm0, rax}
     */
    // Template#: 369, Serial#: 10796
    public void movdq(AMD64XMMRegister destination, AMD64GeneralRegister64 source) {
        assemble0171((byte) 0x6E, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdl  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code movdl     xmm0, [rbx + 305419896]}
     */
    // Template#: 370, Serial#: 10785
    public void movdl(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0073((byte) 0x6E, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdq  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code movdq     xmm0, [rbx + 305419896]}
     */
    // Template#: 371, Serial#: 10794
    public void movdq(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0172((byte) 0x6E, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdl  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movdl     [rbx + 305419896], xmm0}
     */
    // Template#: 372, Serial#: 10920
    public void movdl(int disp32, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        assemble0173((byte) 0x7E, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdq  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movdq     [rbx + 305419896], xmm0}
     */
    // Template#: 373, Serial#: 10929
    public void movdq(int disp32, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        assemble0174((byte) 0x7E, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code movsd     rbx[rsi * 4 + 18], xmm0}
     */
    // Template#: 374, Serial#: 6220
    public void movsd(byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64XMMRegister source) {
        assemble0175((byte) 0x11, disp8, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movsd     [rbx + 18], xmm0}
     */
    // Template#: 375, Serial#: 6219
    public void movsd(byte disp8, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        assemble0176((byte) 0x11, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code movsd     rbx[rsi * 4], xmm0}
     */
    // Template#: 376, Serial#: 6216
    public void movsd(AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64XMMRegister source) {
        assemble0177((byte) 0x11, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movsd     [rbx], xmm0}
     */
    // Template#: 377, Serial#: 6215
    public void movsd(AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        assemble0178((byte) 0x11, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>destination</i>, <i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movsd     xmm0, rbx[rsi * 4 + 18]}
     */
    // Template#: 378, Serial#: 6184
    public void movsd(AMD64XMMRegister destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0179((byte) 0x10, destination, disp8, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code movsd     xmm0, [rbx + 18]}
     */
    // Template#: 379, Serial#: 6183
    public void movsd(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0027((byte) 0x10, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>destination</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movsd     xmm0, rbx[rsi * 4]}
     */
    // Template#: 380, Serial#: 6180
    public void movsd(AMD64XMMRegister destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0180((byte) 0x10, destination, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movsd     xmm0, [rbx]}
     */
    // Template#: 381, Serial#: 6179
    public void movsd(AMD64XMMRegister destination, AMD64IndirectRegister64 source) {
        assemble0181((byte) 0x10, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movsd     xmm0, xmm0}
     */
    // Template#: 382, Serial#: 6187
    public void movsd(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0028((byte) 0x10, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code movsd     xmm0, [L1: +305419896]}
     */
    // Template#: 383, Serial#: 6182
    public void rip_movsd(AMD64XMMRegister destination, int rel32) {
        assemble0029((byte) 0x10, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>destination</i>, <i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movsd     xmm0, rbx[rsi * 4 + 305419896]}
     */
    // Template#: 384, Serial#: 6186
    public void movsd(AMD64XMMRegister destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0182((byte) 0x10, destination, disp32, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code movsd     xmm0, [rbx + 305419896]}
     */
    // Template#: 385, Serial#: 6185
    public void movsd(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0030((byte) 0x10, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code movsd     rbx[rsi * 4 + 305419896], xmm0}
     */
    // Template#: 386, Serial#: 6222
    public void movsd(int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64XMMRegister source) {
        assemble0183((byte) 0x11, disp32, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movsd     [rbx + 305419896], xmm0}
     */
    // Template#: 387, Serial#: 6221
    public void movsd(int disp32, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        assemble0184((byte) 0x11, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code movss     rbx[rsi * 4 + 18], xmm0}
     */
    // Template#: 388, Serial#: 6292
    public void movss(byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64XMMRegister source) {
        assemble0185((byte) 0x11, disp8, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movss     [rbx + 18], xmm0}
     */
    // Template#: 389, Serial#: 6291
    public void movss(byte disp8, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        assemble0186((byte) 0x11, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code movss     rbx[rsi * 4], xmm0}
     */
    // Template#: 390, Serial#: 6288
    public void movss(AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64XMMRegister source) {
        assemble0187((byte) 0x11, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movss     [rbx], xmm0}
     */
    // Template#: 391, Serial#: 6287
    public void movss(AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        assemble0188((byte) 0x11, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>destination</i>, <i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movss     xmm0, rbx[rsi * 4 + 18]}
     */
    // Template#: 392, Serial#: 6256
    public void movss(AMD64XMMRegister destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0189((byte) 0x10, destination, disp8, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code movss     xmm0, [rbx + 18]}
     */
    // Template#: 393, Serial#: 6255
    public void movss(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0031((byte) 0x10, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>destination</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movss     xmm0, rbx[rsi * 4]}
     */
    // Template#: 394, Serial#: 6252
    public void movss(AMD64XMMRegister destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0190((byte) 0x10, destination, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movss     xmm0, [rbx]}
     */
    // Template#: 395, Serial#: 6251
    public void movss(AMD64XMMRegister destination, AMD64IndirectRegister64 source) {
        assemble0191((byte) 0x10, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movss     xmm0, xmm0}
     */
    // Template#: 396, Serial#: 6259
    public void movss(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0032((byte) 0x10, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code movss     xmm0, [L1: +305419896]}
     */
    // Template#: 397, Serial#: 6254
    public void rip_movss(AMD64XMMRegister destination, int rel32) {
        assemble0033((byte) 0x10, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>destination</i>, <i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movss     xmm0, rbx[rsi * 4 + 305419896]}
     */
    // Template#: 398, Serial#: 6258
    public void movss(AMD64XMMRegister destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0192((byte) 0x10, destination, disp32, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code movss     xmm0, [rbx + 305419896]}
     */
    // Template#: 399, Serial#: 6257
    public void movss(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0034((byte) 0x10, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code movss     rbx[rsi * 4 + 305419896], xmm0}
     */
    // Template#: 400, Serial#: 6294
    public void movss(int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64XMMRegister source) {
        assemble0193((byte) 0x11, disp32, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movss     [rbx + 305419896], xmm0}
     */
    // Template#: 401, Serial#: 6293
    public void movss(int disp32, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        assemble0194((byte) 0x11, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsx  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code movsx     eax, [rbx + 18]}
     */
    // Template#: 402, Serial#: 11683
    public void movsxb(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0035((byte) 0xBE, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsx  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movsx     eax, al}
     */
    // Template#: 403, Serial#: 11687
    public void movsxb(AMD64GeneralRegister32 destination, AMD64GeneralRegister8 source) {
        assemble0195((byte) 0xBE, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsx  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code movsx     eax, [L1: +305419896]}
     */
    // Template#: 404, Serial#: 11682
    public void rip_movsxb(AMD64GeneralRegister32 destination, int rel32) {
        assemble0049((byte) 0xBE, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsx  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code movsx     eax, [rbx + 305419896]}
     */
    // Template#: 405, Serial#: 11685
    public void movsxb(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0038((byte) 0xBE, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsx  }<i>destination</i>, <i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movsx     rax, rbx[rsi * 4 + 18]}
     */
    // Template#: 406, Serial#: 11693
    public void movsxb(AMD64GeneralRegister64 destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0196((byte) 0xBE, destination, disp8, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsx  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code movsx     rax, [rbx + 18]}
     */
    // Template#: 407, Serial#: 11692
    public void movsxb(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0039((byte) 0xBE, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsx  }<i>destination</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movsx     rax, rbx[rsi * 4]}
     */
    // Template#: 408, Serial#: 11689
    public void movsxb(AMD64GeneralRegister64 destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0197((byte) 0xBE, destination, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsx  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movsx     rax, [rbx]}
     */
    // Template#: 409, Serial#: 11688
    public void movsxb(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0041((byte) 0xBE, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsx  }<i>destination</i>, <i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movsx     rax, rbx[rsi * 4 + 305419896]}
     */
    // Template#: 410, Serial#: 11695
    public void movsxb(AMD64GeneralRegister64 destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0198((byte) 0xBE, destination, disp32, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsx  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code movsx     rax, [rbx + 305419896]}
     */
    // Template#: 411, Serial#: 11694
    public void movsxb(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0042((byte) 0xBE, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxd  }<i>destination</i>, <i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movsxd    rax, rbx[rsi * 4 + 18]}
     */
    // Template#: 412, Serial#: 464
    public void movsxd(AMD64GeneralRegister64 destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0141((byte) 0x63, destination, disp8, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxd  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code movsxd    rax, [rbx + 18]}
     */
    // Template#: 413, Serial#: 463
    public void movsxd(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0014((byte) 0x63, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxd  }<i>destination</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movsxd    rax, rbx[rsi * 4]}
     */
    // Template#: 414, Serial#: 460
    public void movsxd(AMD64GeneralRegister64 destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0142((byte) 0x63, destination, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movsxd    rax, eax}
     */
    // Template#: 415, Serial#: 467
    public void movsxd(AMD64GeneralRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0199((byte) 0x63, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movsxd    rax, [rbx]}
     */
    // Template#: 416, Serial#: 459
    public void movsxd(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0118((byte) 0x63, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxd  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code movsxd    rax, [L1: +305419896]}
     */
    // Template#: 417, Serial#: 462
    public void rip_movsxd(AMD64GeneralRegister64 destination, int rel32) {
        assemble0016((byte) 0x63, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxd  }<i>destination</i>, <i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movsxd    rax, rbx[rsi * 4 + 305419896]}
     */
    // Template#: 418, Serial#: 466
    public void movsxd(AMD64GeneralRegister64 destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0144((byte) 0x63, destination, disp32, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxd  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code movsxd    rax, [rbx + 305419896]}
     */
    // Template#: 419, Serial#: 465
    public void movsxd(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0018((byte) 0x63, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxw  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code movsxw    eax, [rbx + 18]}
     */
    // Template#: 420, Serial#: 11710
    public void movsxw(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0035((byte) 0xBF, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxw  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movsxw    eax, ax}
     */
    // Template#: 421, Serial#: 11714
    public void movsxw(AMD64GeneralRegister32 destination, AMD64GeneralRegister16 source) {
        assemble0200((byte) 0xBF, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxw  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code movsxw    eax, [L1: +305419896]}
     */
    // Template#: 422, Serial#: 11709
    public void rip_movsxw(AMD64GeneralRegister32 destination, int rel32) {
        assemble0049((byte) 0xBF, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxw  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code movsxw    eax, [rbx + 305419896]}
     */
    // Template#: 423, Serial#: 11712
    public void movsxw(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0038((byte) 0xBF, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxw  }<i>destination</i>, <i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movsxw    rax, rbx[rsi * 4 + 18]}
     */
    // Template#: 424, Serial#: 11720
    public void movsxw(AMD64GeneralRegister64 destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0196((byte) 0xBF, destination, disp8, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxw  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code movsxw    rax, [rbx + 18]}
     */
    // Template#: 425, Serial#: 11719
    public void movsxw(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0039((byte) 0xBF, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxw  }<i>destination</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movsxw    rax, rbx[rsi * 4]}
     */
    // Template#: 426, Serial#: 11716
    public void movsxw(AMD64GeneralRegister64 destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0197((byte) 0xBF, destination, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxw  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movsxw    rax, [rbx]}
     */
    // Template#: 427, Serial#: 11715
    public void movsxw(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0041((byte) 0xBF, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxw  }<i>destination</i>, <i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movsxw    rax, rbx[rsi * 4 + 305419896]}
     */
    // Template#: 428, Serial#: 11722
    public void movsxw(AMD64GeneralRegister64 destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0198((byte) 0xBF, destination, disp32, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxw  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code movsxw    rax, [rbx + 305419896]}
     */
    // Template#: 429, Serial#: 11721
    public void movsxw(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0042((byte) 0xBF, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzx  }<i>destination</i>, <i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movzx     rax, rbx[rsi * 4 + 18]}
     */
    // Template#: 430, Serial#: 7915
    public void movzxb(AMD64GeneralRegister64 destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0196((byte) 0xB6, destination, disp8, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzx  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code movzx     rax, [rbx + 18]}
     */
    // Template#: 431, Serial#: 7914
    public void movzxb(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0039((byte) 0xB6, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzx  }<i>destination</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movzx     rax, rbx[rsi * 4]}
     */
    // Template#: 432, Serial#: 7911
    public void movzxb(AMD64GeneralRegister64 destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0197((byte) 0xB6, destination, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzx  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movzx     rax, [rbx]}
     */
    // Template#: 433, Serial#: 7910
    public void movzxb(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0041((byte) 0xB6, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzx  }<i>destination</i>, <i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movzx     rax, rbx[rsi * 4 + 305419896]}
     */
    // Template#: 434, Serial#: 7917
    public void movzxb(AMD64GeneralRegister64 destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0198((byte) 0xB6, destination, disp32, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzx  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code movzx     rax, [rbx + 305419896]}
     */
    // Template#: 435, Serial#: 7916
    public void movzxb(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0042((byte) 0xB6, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxd  }<i>destination</i>, <i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movzxd    rax, rbx[rsi * 4 + 18]}
     */
    // Template#: 436, Serial#: 473
    public void movzxd(AMD64GeneralRegister64 destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0201((byte) 0x63, destination, disp8, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxd  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code movzxd    rax, [rbx + 18]}
     */
    // Template#: 437, Serial#: 472
    public void movzxd(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0202((byte) 0x63, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxd  }<i>destination</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movzxd    rax, rbx[rsi * 4]}
     */
    // Template#: 438, Serial#: 469
    public void movzxd(AMD64GeneralRegister64 destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0203((byte) 0x63, destination, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movzxd    rax, eax}
     */
    // Template#: 439, Serial#: 476
    public void movzxd(AMD64GeneralRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0204((byte) 0x63, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movzxd    rax, [rbx]}
     */
    // Template#: 440, Serial#: 468
    public void movzxd(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0205((byte) 0x63, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxd  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code movzxd    rax, [L1: +305419896]}
     */
    // Template#: 441, Serial#: 471
    public void rip_movzxd(AMD64GeneralRegister64 destination, int rel32) {
        assemble0206((byte) 0x63, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxd  }<i>destination</i>, <i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movzxd    rax, rbx[rsi * 4 + 305419896]}
     */
    // Template#: 442, Serial#: 475
    public void movzxd(AMD64GeneralRegister64 destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0207((byte) 0x63, destination, disp32, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxd  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code movzxd    rax, [rbx + 305419896]}
     */
    // Template#: 443, Serial#: 474
    public void movzxd(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0208((byte) 0x63, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxw  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code movzxw    eax, [rbx + 18]}
     */
    // Template#: 444, Serial#: 7932
    public void movzxw(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0035((byte) 0xB7, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxw  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movzxw    eax, ax}
     */
    // Template#: 445, Serial#: 7936
    public void movzxw(AMD64GeneralRegister32 destination, AMD64GeneralRegister16 source) {
        assemble0200((byte) 0xB7, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxw  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code movzxw    eax, [L1: +305419896]}
     */
    // Template#: 446, Serial#: 7931
    public void rip_movzxw(AMD64GeneralRegister32 destination, int rel32) {
        assemble0049((byte) 0xB7, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxw  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code movzxw    eax, [rbx + 305419896]}
     */
    // Template#: 447, Serial#: 7934
    public void movzxw(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0038((byte) 0xB7, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxw  }<i>destination</i>, <i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movzxw    rax, rbx[rsi * 4 + 18]}
     */
    // Template#: 448, Serial#: 7942
    public void movzxw(AMD64GeneralRegister64 destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0196((byte) 0xB7, destination, disp8, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxw  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code movzxw    rax, [rbx + 18]}
     */
    // Template#: 449, Serial#: 7941
    public void movzxw(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0039((byte) 0xB7, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxw  }<i>destination</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movzxw    rax, rbx[rsi * 4]}
     */
    // Template#: 450, Serial#: 7938
    public void movzxw(AMD64GeneralRegister64 destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0197((byte) 0xB7, destination, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxw  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movzxw    rax, [rbx]}
     */
    // Template#: 451, Serial#: 7937
    public void movzxw(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0041((byte) 0xB7, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxw  }<i>destination</i>, <i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movzxw    rax, rbx[rsi * 4 + 305419896]}
     */
    // Template#: 452, Serial#: 7944
    public void movzxw(AMD64GeneralRegister64 destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0198((byte) 0xB7, destination, disp32, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxw  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code movzxw    rax, [rbx + 305419896]}
     */
    // Template#: 453, Serial#: 7943
    public void movzxw(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0042((byte) 0xB7, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mulsd  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code mulsd     xmm0, [rbx + 18]}
     */
    // Template#: 454, Serial#: 10270
    public void mulsd(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0027((byte) 0x59, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mulsd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mulsd     xmm0, xmm0}
     */
    // Template#: 455, Serial#: 10274
    public void mulsd(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0028((byte) 0x59, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mulsd  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code mulsd     xmm0, [L1: +305419896]}
     */
    // Template#: 456, Serial#: 10269
    public void rip_mulsd(AMD64XMMRegister destination, int rel32) {
        assemble0029((byte) 0x59, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code mulsd  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code mulsd     xmm0, [rbx + 305419896]}
     */
    // Template#: 457, Serial#: 10272
    public void mulsd(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0030((byte) 0x59, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mulss  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code mulss     xmm0, [rbx + 18]}
     */
    // Template#: 458, Serial#: 10396
    public void mulss(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0031((byte) 0x59, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mulss  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mulss     xmm0, xmm0}
     */
    // Template#: 459, Serial#: 10400
    public void mulss(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0032((byte) 0x59, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mulss  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code mulss     xmm0, [L1: +305419896]}
     */
    // Template#: 460, Serial#: 10395
    public void rip_mulss(AMD64XMMRegister destination, int rel32) {
        assemble0033((byte) 0x59, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code mulss  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code mulss     xmm0, [rbx + 305419896]}
     */
    // Template#: 461, Serial#: 10398
    public void mulss(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0034((byte) 0x59, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code negq  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code negq      [rbx + 18]}
     */
    // Template#: 462, Serial#: 3007
    public void negq(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0106((byte) 0xF7, (byte) 0x03, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code negq  }<i>destination</i>
     * Example disassembly syntax: {@code negq      rax}
     */
    // Template#: 463, Serial#: 3036
    public void negq(AMD64GeneralRegister64 destination) {
        assemble0105((byte) 0xF7, (byte) 0x03, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code negq  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code negq      [rbx + 305419896]}
     */
    // Template#: 464, Serial#: 3023
    public void negq(int disp32, AMD64IndirectRegister64 destination) {
        assemble0108((byte) 0xF7, (byte) 0x03, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code nop  }
     * Example disassembly syntax: {@code nop     }
     */
    // Template#: 465, Serial#: 1253
    public void nop() {
        assemble0048((byte) 0x90);
    }

    /**
     * Pseudo-external assembler syntax: {@code notq  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code notq      [rbx + 18]}
     */
    // Template#: 466, Serial#: 3005
    public void notq(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0106((byte) 0xF7, (byte) 0x02, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code notq  }<i>destination</i>
     * Example disassembly syntax: {@code notq      rax}
     */
    // Template#: 467, Serial#: 3035
    public void notq(AMD64GeneralRegister64 destination) {
        assemble0105((byte) 0xF7, (byte) 0x02, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code notq  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code notq      [rbx + 305419896]}
     */
    // Template#: 468, Serial#: 3021
    public void notq(int disp32, AMD64IndirectRegister64 destination) {
        assemble0108((byte) 0xF7, (byte) 0x02, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code orl  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code orl       [rbx + 18], 0x12}
     */
    // Template#: 469, Serial#: 963
    public void orl(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0001((byte) 0x83, (byte) 0x01, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code orq  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code orq       [rbx + 18], 0x12}
     */
    // Template#: 470, Serial#: 1035
    public void orq(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0002((byte) 0x83, (byte) 0x01, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code or        [rbx + 18], eax}
     */
    // Template#: 471, Serial#: 3144
    public void or(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0003((byte) 0x09, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code or        [rbx + 18], rax}
     */
    // Template#: 472, Serial#: 3153
    public void or(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0004((byte) 0x09, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code orl  }<i>disp8</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code orl       [rbx + 18], 0x12345678}
     */
    // Template#: 473, Serial#: 747
    public void orl(byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        assemble0005((byte) 0x81, (byte) 0x01, disp8, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code orq  }<i>disp8</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code orq       [rbx + 18], 0x12345678}
     */
    // Template#: 474, Serial#: 819
    public void orq(byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        assemble0006((byte) 0x81, (byte) 0x01, disp8, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code orl  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code orl       eax, 0x12}
     */
    // Template#: 475, Serial#: 994
    public void orl(AMD64GeneralRegister32 destination, byte imm8) {
        assemble0007((byte) 0x83, (byte) 0x01, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code or        eax, [rbx + 18]}
     */
    // Template#: 476, Serial#: 3198
    public void or(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0008((byte) 0x0B, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code or        eax, eax}
     */
    // Template#: 477, Serial#: 3148
    public void or(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0009((byte) 0x09, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code orl  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code orl       eax, 0x12345678}
     */
    // Template#: 478, Serial#: 778
    public void orl(AMD64GeneralRegister32 destination, int imm32) {
        assemble0011((byte) 0x81, (byte) 0x01, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code or        eax, [L1: +305419896]}
     */
    // Template#: 479, Serial#: 3197
    public void rip_or(AMD64GeneralRegister32 destination, int rel32) {
        assemble0010((byte) 0x0B, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code or        eax, [rbx + 305419896]}
     */
    // Template#: 480, Serial#: 3200
    public void or(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0012((byte) 0x0B, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code orq  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code orq       rax, 0x12}
     */
    // Template#: 481, Serial#: 1066
    public void orq(AMD64GeneralRegister64 destination, byte imm8) {
        assemble0013((byte) 0x83, (byte) 0x01, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code or        rax, [rbx + 18]}
     */
    // Template#: 482, Serial#: 3207
    public void or(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0014((byte) 0x0B, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code or        rax, rax}
     */
    // Template#: 483, Serial#: 3157
    public void or(AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0015((byte) 0x09, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code orq  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code orq       rax, 0x12345678}
     */
    // Template#: 484, Serial#: 850
    public void orq(AMD64GeneralRegister64 destination, int imm32) {
        assemble0017((byte) 0x81, (byte) 0x01, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code or        rax, [L1: +305419896]}
     */
    // Template#: 485, Serial#: 3206
    public void rip_or(AMD64GeneralRegister64 destination, int rel32) {
        assemble0016((byte) 0x0B, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code or        rax, [rbx + 305419896]}
     */
    // Template#: 486, Serial#: 3209
    public void or(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0018((byte) 0x0B, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>imm32</i>
     * Example disassembly syntax: {@code or        eax, 0x12345678}
     */
    // Template#: 487, Serial#: 3224
    public void or_EAX(int imm32) {
        assemble0019((byte) 0x0D, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>imm32</i>
     * Example disassembly syntax: {@code or        rax, 0x12345678}
     */
    // Template#: 488, Serial#: 3225
    public void or_RAX(int imm32) {
        assemble0020((byte) 0x0D, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code orl  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code orl       [rbx + 305419896], 0x12}
     */
    // Template#: 489, Serial#: 979
    public void orl(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0021((byte) 0x83, (byte) 0x01, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code orq  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code orq       [rbx + 305419896], 0x12}
     */
    // Template#: 490, Serial#: 1051
    public void orq(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0022((byte) 0x83, (byte) 0x01, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code or        [rbx + 305419896], eax}
     */
    // Template#: 491, Serial#: 3146
    public void or(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0023((byte) 0x09, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code or        [rbx + 305419896], rax}
     */
    // Template#: 492, Serial#: 3155
    public void or(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0024((byte) 0x09, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code orl  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code orl       [rbx + 305419896], 0x12345678}
     */
    // Template#: 493, Serial#: 763
    public void orl(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0025((byte) 0x81, (byte) 0x01, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code orq  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code orq       [rbx + 305419896], 0x12345678}
     */
    // Template#: 494, Serial#: 835
    public void orq(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0026((byte) 0x81, (byte) 0x01, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code pop  }<i>register</i>
     * Example disassembly syntax: {@code pop       rax}
     */
    // Template#: 495, Serial#: 3575
    public void pop(AMD64GeneralRegister64 register) {
        assemble0209((byte) 0x58, register);
    }

    /**
     * Pseudo-external assembler syntax: {@code popfq  }
     * Example disassembly syntax: {@code popfq   }
     */
    // Template#: 496, Serial#: 3866
    public void popfq() {
        assemble0048((byte) 0x9D);
    }

    /**
     * Pseudo-external assembler syntax: {@code push  }<i>register</i>
     * Example disassembly syntax: {@code push      rax}
     */
    // Template#: 497, Serial#: 457
    public void push(AMD64GeneralRegister64 register) {
        assemble0209((byte) 0x50, register);
    }

    /**
     * Pseudo-external assembler syntax: {@code push  }<i>imm32</i>
     * Example disassembly syntax: {@code push      0x12345678}
     */
    // Template#: 498, Serial#: 3577
    public void push(int imm32) {
        assemble0019((byte) 0x68, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code push  }<i>rel32</i>
     * Example disassembly syntax: {@code push      [L1: +305419896]}
     */
    // Template#: 499, Serial#: 5440
    public void rip_push(int rel32) {
        assemble0046((byte) 0xFF, (byte) 0x06, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code pushfq  }
     * Example disassembly syntax: {@code pushfq  }
     */
    // Template#: 500, Serial#: 3864
    public void pushfq() {
        assemble0048((byte) 0x9C);
    }

    /**
     * Pseudo-external assembler syntax: {@code repe  }
     * Example disassembly syntax: {@code repe    }
     */
    // Template#: 501, Serial#: 2672
    public void repe() {
        assemble0048((byte) 0xF3);
    }

    /**
     * Pseudo-external assembler syntax: {@code ret  }
     * Example disassembly syntax: {@code ret     }
     */
    // Template#: 502, Serial#: 1720
    public void ret() {
        assemble0048((byte) 0xC3);
    }

    /**
     * Pseudo-external assembler syntax: {@code ret  }<i>imm16</i>
     * Example disassembly syntax: {@code ret       0x1234}
     */
    // Template#: 503, Serial#: 1718
    public void ret(short imm16) {
        assemble0210((byte) 0xC2, imm16);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarl  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code sarl      [rbx + 18], 0x1}
     */
    // Template#: 504, Serial#: 2038
    public void sarl___1(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0043((byte) 0xD1, (byte) 0x07, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarq  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code sarq      [rbx + 18], 0x1}
     */
    // Template#: 505, Serial#: 2110
    public void sarq___1(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0106((byte) 0xD1, (byte) 0x07, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarl  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code sarl      [rbx + 18], cl}
     */
    // Template#: 506, Serial#: 2470
    public void sarl___CL(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0043((byte) 0xD3, (byte) 0x07, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarq  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code sarq      [rbx + 18], cl}
     */
    // Template#: 507, Serial#: 2542
    public void sarq___CL(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0106((byte) 0xD3, (byte) 0x07, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarl  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code sarl      [rbx + 18], 0x12}
     */
    // Template#: 508, Serial#: 1548
    public void sarl(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0001((byte) 0xC1, (byte) 0x07, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarq  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code sarq      [rbx + 18], 0x12}
     */
    // Template#: 509, Serial#: 1620
    public void sarq(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0002((byte) 0xC1, (byte) 0x07, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarl  }<i>destination</i>
     * Example disassembly syntax: {@code sarl      eax, 0x1}
     */
    // Template#: 510, Serial#: 2063
    public void sarl___1(AMD64GeneralRegister32 destination) {
        assemble0110((byte) 0xD1, (byte) 0x07, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarl  }<i>destination</i>
     * Example disassembly syntax: {@code sarl      eax, cl}
     */
    // Template#: 511, Serial#: 2495
    public void sarl___CL(AMD64GeneralRegister32 destination) {
        assemble0110((byte) 0xD3, (byte) 0x07, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarl  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code sarl      eax, 0x12}
     */
    // Template#: 512, Serial#: 1573
    public void sarl(AMD64GeneralRegister32 destination, byte imm8) {
        assemble0007((byte) 0xC1, (byte) 0x07, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarq  }<i>destination</i>
     * Example disassembly syntax: {@code sarq      rax, 0x1}
     */
    // Template#: 513, Serial#: 2135
    public void sarq___1(AMD64GeneralRegister64 destination) {
        assemble0105((byte) 0xD1, (byte) 0x07, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarq  }<i>destination</i>
     * Example disassembly syntax: {@code sarq      rax, cl}
     */
    // Template#: 514, Serial#: 2567
    public void sarq___CL(AMD64GeneralRegister64 destination) {
        assemble0105((byte) 0xD3, (byte) 0x07, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarq  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code sarq      rax, 0x12}
     */
    // Template#: 515, Serial#: 1645
    public void sarq(AMD64GeneralRegister64 destination, byte imm8) {
        assemble0013((byte) 0xC1, (byte) 0x07, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarl  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code sarl      [rbx + 305419896], 0x1}
     */
    // Template#: 516, Serial#: 2054
    public void sarl___1(int disp32, AMD64IndirectRegister64 destination) {
        assemble0047((byte) 0xD1, (byte) 0x07, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarq  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code sarq      [rbx + 305419896], 0x1}
     */
    // Template#: 517, Serial#: 2126
    public void sarq___1(int disp32, AMD64IndirectRegister64 destination) {
        assemble0108((byte) 0xD1, (byte) 0x07, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarl  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code sarl      [rbx + 305419896], cl}
     */
    // Template#: 518, Serial#: 2486
    public void sarl___CL(int disp32, AMD64IndirectRegister64 destination) {
        assemble0047((byte) 0xD3, (byte) 0x07, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarq  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code sarq      [rbx + 305419896], cl}
     */
    // Template#: 519, Serial#: 2558
    public void sarq___CL(int disp32, AMD64IndirectRegister64 destination) {
        assemble0108((byte) 0xD3, (byte) 0x07, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarl  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code sarl      [rbx + 305419896], 0x12}
     */
    // Template#: 520, Serial#: 1564
    public void sarl(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0021((byte) 0xC1, (byte) 0x07, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarq  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code sarq      [rbx + 305419896], 0x12}
     */
    // Template#: 521, Serial#: 1636
    public void sarq(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0022((byte) 0xC1, (byte) 0x07, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code setb  }<i>destination</i>
     * Example disassembly syntax: {@code setb      al}
     */
    // Template#: 522, Serial#: 7578
    public void setb(AMD64GeneralRegister8 destination) {
        assemble0211((byte) 0x92, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code setbe  }<i>destination</i>
     * Example disassembly syntax: {@code setbe     al}
     */
    // Template#: 523, Serial#: 7686
    public void setbe(AMD64GeneralRegister8 destination) {
        assemble0211((byte) 0x96, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code setl  }<i>destination</i>
     * Example disassembly syntax: {@code setl      al}
     */
    // Template#: 524, Serial#: 11146
    public void setl(AMD64GeneralRegister8 destination) {
        assemble0211((byte) 0x9C, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code setnb  }<i>destination</i>
     * Example disassembly syntax: {@code setnb     al}
     */
    // Template#: 525, Serial#: 7605
    public void setnb(AMD64GeneralRegister8 destination) {
        assemble0211((byte) 0x93, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code setnbe  }<i>destination</i>
     * Example disassembly syntax: {@code setnbe    al}
     */
    // Template#: 526, Serial#: 7713
    public void setnbe(AMD64GeneralRegister8 destination) {
        assemble0211((byte) 0x97, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code setnl  }<i>destination</i>
     * Example disassembly syntax: {@code setnl     al}
     */
    // Template#: 527, Serial#: 11173
    public void setnl(AMD64GeneralRegister8 destination) {
        assemble0211((byte) 0x9D, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code setnle  }<i>destination</i>
     * Example disassembly syntax: {@code setnle    al}
     */
    // Template#: 528, Serial#: 11227
    public void setnle(AMD64GeneralRegister8 destination) {
        assemble0211((byte) 0x9F, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code setnp  }<i>destination</i>
     * Example disassembly syntax: {@code setnp     al}
     */
    // Template#: 529, Serial#: 11119
    public void setnp(AMD64GeneralRegister8 destination) {
        assemble0211((byte) 0x9B, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code setp  }<i>destination</i>
     * Example disassembly syntax: {@code setp      al}
     */
    // Template#: 530, Serial#: 11092
    public void setp(AMD64GeneralRegister8 destination) {
        assemble0211((byte) 0x9A, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code sfence  }
     * Example disassembly syntax: {@code sfence  }
     */
    // Template#: 531, Serial#: 11456
    public void sfence() {
        assemble0119((byte) 0xAE, (byte) 0x07);
    }

    /**
     * Pseudo-external assembler syntax: {@code shll  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code shll      [rbx + 18], 0x1}
     */
    // Template#: 532, Serial#: 2032
    public void shll___1(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0043((byte) 0xD1, (byte) 0x04, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlq  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code shlq      [rbx + 18], 0x1}
     */
    // Template#: 533, Serial#: 2104
    public void shlq___1(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0106((byte) 0xD1, (byte) 0x04, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shll  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code shll      [rbx + 18], cl}
     */
    // Template#: 534, Serial#: 2464
    public void shll___CL(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0043((byte) 0xD3, (byte) 0x04, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlq  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code shlq      [rbx + 18], cl}
     */
    // Template#: 535, Serial#: 2536
    public void shlq___CL(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0106((byte) 0xD3, (byte) 0x04, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shll  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shll      [rbx + 18], 0x12}
     */
    // Template#: 536, Serial#: 1542
    public void shll(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0001((byte) 0xC1, (byte) 0x04, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlq  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shlq      [rbx + 18], 0x12}
     */
    // Template#: 537, Serial#: 1614
    public void shlq(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0002((byte) 0xC1, (byte) 0x04, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code shll  }<i>destination</i>
     * Example disassembly syntax: {@code shll      eax, 0x1}
     */
    // Template#: 538, Serial#: 2060
    public void shll___1(AMD64GeneralRegister32 destination) {
        assemble0110((byte) 0xD1, (byte) 0x04, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shll  }<i>destination</i>
     * Example disassembly syntax: {@code shll      eax, cl}
     */
    // Template#: 539, Serial#: 2492
    public void shll___CL(AMD64GeneralRegister32 destination) {
        assemble0110((byte) 0xD3, (byte) 0x04, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shll  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shll      eax, 0x12}
     */
    // Template#: 540, Serial#: 1570
    public void shll(AMD64GeneralRegister32 destination, byte imm8) {
        assemble0007((byte) 0xC1, (byte) 0x04, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlq  }<i>destination</i>
     * Example disassembly syntax: {@code shlq      rax, 0x1}
     */
    // Template#: 541, Serial#: 2132
    public void shlq___1(AMD64GeneralRegister64 destination) {
        assemble0105((byte) 0xD1, (byte) 0x04, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlq  }<i>destination</i>
     * Example disassembly syntax: {@code shlq      rax, cl}
     */
    // Template#: 542, Serial#: 2564
    public void shlq___CL(AMD64GeneralRegister64 destination) {
        assemble0105((byte) 0xD3, (byte) 0x04, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlq  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shlq      rax, 0x12}
     */
    // Template#: 543, Serial#: 1642
    public void shlq(AMD64GeneralRegister64 destination, byte imm8) {
        assemble0013((byte) 0xC1, (byte) 0x04, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code shll  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code shll      [rbx + 305419896], 0x1}
     */
    // Template#: 544, Serial#: 2048
    public void shll___1(int disp32, AMD64IndirectRegister64 destination) {
        assemble0047((byte) 0xD1, (byte) 0x04, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlq  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code shlq      [rbx + 305419896], 0x1}
     */
    // Template#: 545, Serial#: 2120
    public void shlq___1(int disp32, AMD64IndirectRegister64 destination) {
        assemble0108((byte) 0xD1, (byte) 0x04, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shll  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code shll      [rbx + 305419896], cl}
     */
    // Template#: 546, Serial#: 2480
    public void shll___CL(int disp32, AMD64IndirectRegister64 destination) {
        assemble0047((byte) 0xD3, (byte) 0x04, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlq  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code shlq      [rbx + 305419896], cl}
     */
    // Template#: 547, Serial#: 2552
    public void shlq___CL(int disp32, AMD64IndirectRegister64 destination) {
        assemble0108((byte) 0xD3, (byte) 0x04, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shll  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shll      [rbx + 305419896], 0x12}
     */
    // Template#: 548, Serial#: 1558
    public void shll(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0021((byte) 0xC1, (byte) 0x04, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlq  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shlq      [rbx + 305419896], 0x12}
     */
    // Template#: 549, Serial#: 1630
    public void shlq(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0022((byte) 0xC1, (byte) 0x04, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrl  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code shrl      [rbx + 18], 0x1}
     */
    // Template#: 550, Serial#: 2034
    public void shrl___1(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0043((byte) 0xD1, (byte) 0x05, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrq  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code shrq      [rbx + 18], 0x1}
     */
    // Template#: 551, Serial#: 2106
    public void shrq___1(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0106((byte) 0xD1, (byte) 0x05, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrl  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code shrl      [rbx + 18], cl}
     */
    // Template#: 552, Serial#: 2466
    public void shrl___CL(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0043((byte) 0xD3, (byte) 0x05, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrq  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code shrq      [rbx + 18], cl}
     */
    // Template#: 553, Serial#: 2538
    public void shrq___CL(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0106((byte) 0xD3, (byte) 0x05, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrl  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shrl      [rbx + 18], 0x12}
     */
    // Template#: 554, Serial#: 1544
    public void shrl(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0001((byte) 0xC1, (byte) 0x05, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrq  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shrq      [rbx + 18], 0x12}
     */
    // Template#: 555, Serial#: 1616
    public void shrq(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0002((byte) 0xC1, (byte) 0x05, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrl  }<i>destination</i>
     * Example disassembly syntax: {@code shrl      eax, 0x1}
     */
    // Template#: 556, Serial#: 2061
    public void shrl___1(AMD64GeneralRegister32 destination) {
        assemble0110((byte) 0xD1, (byte) 0x05, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrl  }<i>destination</i>
     * Example disassembly syntax: {@code shrl      eax, cl}
     */
    // Template#: 557, Serial#: 2493
    public void shrl___CL(AMD64GeneralRegister32 destination) {
        assemble0110((byte) 0xD3, (byte) 0x05, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrl  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shrl      eax, 0x12}
     */
    // Template#: 558, Serial#: 1571
    public void shrl(AMD64GeneralRegister32 destination, byte imm8) {
        assemble0007((byte) 0xC1, (byte) 0x05, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrq  }<i>destination</i>
     * Example disassembly syntax: {@code shrq      rax, 0x1}
     */
    // Template#: 559, Serial#: 2133
    public void shrq___1(AMD64GeneralRegister64 destination) {
        assemble0105((byte) 0xD1, (byte) 0x05, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrq  }<i>destination</i>
     * Example disassembly syntax: {@code shrq      rax, cl}
     */
    // Template#: 560, Serial#: 2565
    public void shrq___CL(AMD64GeneralRegister64 destination) {
        assemble0105((byte) 0xD3, (byte) 0x05, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrq  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shrq      rax, 0x12}
     */
    // Template#: 561, Serial#: 1643
    public void shrq(AMD64GeneralRegister64 destination, byte imm8) {
        assemble0013((byte) 0xC1, (byte) 0x05, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrl  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code shrl      [rbx + 305419896], 0x1}
     */
    // Template#: 562, Serial#: 2050
    public void shrl___1(int disp32, AMD64IndirectRegister64 destination) {
        assemble0047((byte) 0xD1, (byte) 0x05, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrq  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code shrq      [rbx + 305419896], 0x1}
     */
    // Template#: 563, Serial#: 2122
    public void shrq___1(int disp32, AMD64IndirectRegister64 destination) {
        assemble0108((byte) 0xD1, (byte) 0x05, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrl  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code shrl      [rbx + 305419896], cl}
     */
    // Template#: 564, Serial#: 2482
    public void shrl___CL(int disp32, AMD64IndirectRegister64 destination) {
        assemble0047((byte) 0xD3, (byte) 0x05, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrq  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code shrq      [rbx + 305419896], cl}
     */
    // Template#: 565, Serial#: 2554
    public void shrq___CL(int disp32, AMD64IndirectRegister64 destination) {
        assemble0108((byte) 0xD3, (byte) 0x05, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrl  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shrl      [rbx + 305419896], 0x12}
     */
    // Template#: 566, Serial#: 1560
    public void shrl(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0021((byte) 0xC1, (byte) 0x05, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrq  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shrq      [rbx + 305419896], 0x12}
     */
    // Template#: 567, Serial#: 1632
    public void shrq(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0022((byte) 0xC1, (byte) 0x05, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code subl  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code subl      [rbx + 18], 0x12}
     */
    // Template#: 568, Serial#: 971
    public void subl(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0001((byte) 0x83, (byte) 0x05, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code subq  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code subq      [rbx + 18], 0x12}
     */
    // Template#: 569, Serial#: 1043
    public void subq(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0002((byte) 0x83, (byte) 0x05, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code sub       [rbx + 18], eax}
     */
    // Template#: 570, Serial#: 3372
    public void sub(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0003((byte) 0x29, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code sub       [rbx + 18], rax}
     */
    // Template#: 571, Serial#: 3381
    public void sub(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0004((byte) 0x29, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code subl  }<i>disp8</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code subl      [rbx + 18], 0x12345678}
     */
    // Template#: 572, Serial#: 755
    public void subl(byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        assemble0005((byte) 0x81, (byte) 0x05, disp8, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code subq  }<i>disp8</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code subq      [rbx + 18], 0x12345678}
     */
    // Template#: 573, Serial#: 827
    public void subq(byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        assemble0006((byte) 0x81, (byte) 0x05, disp8, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code subl  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code subl      eax, 0x12}
     */
    // Template#: 574, Serial#: 998
    public void subl(AMD64GeneralRegister32 destination, byte imm8) {
        assemble0007((byte) 0x83, (byte) 0x05, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code sub       eax, [rbx + 18]}
     */
    // Template#: 575, Serial#: 3426
    public void sub(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0008((byte) 0x2B, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code sub       eax, eax}
     */
    // Template#: 576, Serial#: 3376
    public void sub(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0009((byte) 0x29, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code subl  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code subl      eax, 0x12345678}
     */
    // Template#: 577, Serial#: 782
    public void subl(AMD64GeneralRegister32 destination, int imm32) {
        assemble0011((byte) 0x81, (byte) 0x05, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code sub       eax, [L1: +305419896]}
     */
    // Template#: 578, Serial#: 3425
    public void rip_sub(AMD64GeneralRegister32 destination, int rel32) {
        assemble0010((byte) 0x2B, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code sub       eax, [rbx + 305419896]}
     */
    // Template#: 579, Serial#: 3428
    public void sub(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0012((byte) 0x2B, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code subq  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code subq      rax, 0x12}
     */
    // Template#: 580, Serial#: 1070
    public void subq(AMD64GeneralRegister64 destination, byte imm8) {
        assemble0013((byte) 0x83, (byte) 0x05, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code sub       rax, [rbx + 18]}
     */
    // Template#: 581, Serial#: 3435
    public void sub(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0014((byte) 0x2B, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code sub       rax, rax}
     */
    // Template#: 582, Serial#: 3385
    public void sub(AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0015((byte) 0x29, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code subq  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code subq      rax, 0x12345678}
     */
    // Template#: 583, Serial#: 854
    public void subq(AMD64GeneralRegister64 destination, int imm32) {
        assemble0017((byte) 0x81, (byte) 0x05, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code sub       rax, [L1: +305419896]}
     */
    // Template#: 584, Serial#: 3434
    public void rip_sub(AMD64GeneralRegister64 destination, int rel32) {
        assemble0016((byte) 0x2B, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code sub       rax, [rbx + 305419896]}
     */
    // Template#: 585, Serial#: 3437
    public void sub(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0018((byte) 0x2B, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>imm32</i>
     * Example disassembly syntax: {@code sub       eax, 0x12345678}
     */
    // Template#: 586, Serial#: 3452
    public void sub_EAX(int imm32) {
        assemble0019((byte) 0x2D, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>imm32</i>
     * Example disassembly syntax: {@code sub       rax, 0x12345678}
     */
    // Template#: 587, Serial#: 3453
    public void sub_RAX(int imm32) {
        assemble0020((byte) 0x2D, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code subl  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code subl      [rbx + 305419896], 0x12}
     */
    // Template#: 588, Serial#: 987
    public void subl(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0021((byte) 0x83, (byte) 0x05, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code subq  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code subq      [rbx + 305419896], 0x12}
     */
    // Template#: 589, Serial#: 1059
    public void subq(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0022((byte) 0x83, (byte) 0x05, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code sub       [rbx + 305419896], eax}
     */
    // Template#: 590, Serial#: 3374
    public void sub(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0023((byte) 0x29, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code sub       [rbx + 305419896], rax}
     */
    // Template#: 591, Serial#: 3383
    public void sub(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0024((byte) 0x29, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code subl  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code subl      [rbx + 305419896], 0x12345678}
     */
    // Template#: 592, Serial#: 771
    public void subl(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0025((byte) 0x81, (byte) 0x05, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code subq  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code subq      [rbx + 305419896], 0x12345678}
     */
    // Template#: 593, Serial#: 843
    public void subq(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0026((byte) 0x81, (byte) 0x05, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code subsd  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code subsd     xmm0, [rbx + 18]}
     */
    // Template#: 594, Serial#: 10306
    public void subsd(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0027((byte) 0x5C, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code subsd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code subsd     xmm0, xmm0}
     */
    // Template#: 595, Serial#: 10310
    public void subsd(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0028((byte) 0x5C, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code subsd  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code subsd     xmm0, [L1: +305419896]}
     */
    // Template#: 596, Serial#: 10305
    public void rip_subsd(AMD64XMMRegister destination, int rel32) {
        assemble0029((byte) 0x5C, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code subsd  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code subsd     xmm0, [rbx + 305419896]}
     */
    // Template#: 597, Serial#: 10308
    public void subsd(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0030((byte) 0x5C, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code subss  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code subss     xmm0, [rbx + 18]}
     */
    // Template#: 598, Serial#: 10450
    public void subss(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0031((byte) 0x5C, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code subss  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code subss     xmm0, xmm0}
     */
    // Template#: 599, Serial#: 10454
    public void subss(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0032((byte) 0x5C, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code subss  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code subss     xmm0, [L1: +305419896]}
     */
    // Template#: 600, Serial#: 10449
    public void rip_subss(AMD64XMMRegister destination, int rel32) {
        assemble0033((byte) 0x5C, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code subss  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code subss     xmm0, [rbx + 305419896]}
     */
    // Template#: 601, Serial#: 10452
    public void subss(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0034((byte) 0x5C, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code xchg  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code xchg      rax, rax}
     */
    // Template#: 602, Serial#: 1243
    public void xchg(AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0015((byte) 0x87, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorl  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code xorl      [rbx + 18], 0x12}
     */
    // Template#: 603, Serial#: 973
    public void xorl(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0001((byte) 0x83, (byte) 0x06, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorq  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code xorq      [rbx + 18], 0x12}
     */
    // Template#: 604, Serial#: 1045
    public void xorq(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0002((byte) 0x83, (byte) 0x06, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code xor       [rbx + 18], eax}
     */
    // Template#: 605, Serial#: 374
    public void xor(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0003((byte) 0x31, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code xor       [rbx + 18], rax}
     */
    // Template#: 606, Serial#: 383
    public void xor(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0004((byte) 0x31, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorl  }<i>disp8</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code xorl      [rbx + 18], 0x12345678}
     */
    // Template#: 607, Serial#: 757
    public void xorl(byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        assemble0005((byte) 0x81, (byte) 0x06, disp8, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorq  }<i>disp8</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code xorq      [rbx + 18], 0x12345678}
     */
    // Template#: 608, Serial#: 829
    public void xorq(byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        assemble0006((byte) 0x81, (byte) 0x06, disp8, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorl  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code xorl      eax, 0x12}
     */
    // Template#: 609, Serial#: 999
    public void xorl(AMD64GeneralRegister32 destination, byte imm8) {
        assemble0007((byte) 0x83, (byte) 0x06, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code xor       eax, [rbx + 18]}
     */
    // Template#: 610, Serial#: 428
    public void xor(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0008((byte) 0x33, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code xor       eax, eax}
     */
    // Template#: 611, Serial#: 378
    public void xor(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0009((byte) 0x31, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code xor       eax, [L1: +305419896]}
     */
    // Template#: 612, Serial#: 427
    public void rip_xor(AMD64GeneralRegister32 destination, int rel32) {
        assemble0010((byte) 0x33, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorl  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code xorl      eax, 0x12345678}
     */
    // Template#: 613, Serial#: 783
    public void xorl(AMD64GeneralRegister32 destination, int imm32) {
        assemble0011((byte) 0x81, (byte) 0x06, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code xor       eax, [rbx + 305419896]}
     */
    // Template#: 614, Serial#: 430
    public void xor(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0012((byte) 0x33, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorq  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code xorq      rax, 0x12}
     */
    // Template#: 615, Serial#: 1071
    public void xorq(AMD64GeneralRegister64 destination, byte imm8) {
        assemble0013((byte) 0x83, (byte) 0x06, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code xor       rax, [rbx + 18]}
     */
    // Template#: 616, Serial#: 437
    public void xor(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0014((byte) 0x33, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code xor       rax, rax}
     */
    // Template#: 617, Serial#: 387
    public void xor(AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0015((byte) 0x31, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code xor       rax, [L1: +305419896]}
     */
    // Template#: 618, Serial#: 436
    public void rip_xor(AMD64GeneralRegister64 destination, int rel32) {
        assemble0016((byte) 0x33, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorq  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code xorq      rax, 0x12345678}
     */
    // Template#: 619, Serial#: 855
    public void xorq(AMD64GeneralRegister64 destination, int imm32) {
        assemble0017((byte) 0x81, (byte) 0x06, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code xor       rax, [rbx + 305419896]}
     */
    // Template#: 620, Serial#: 439
    public void xor(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0018((byte) 0x33, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>imm32</i>
     * Example disassembly syntax: {@code xor       eax, 0x12345678}
     */
    // Template#: 621, Serial#: 454
    public void xor_EAX(int imm32) {
        assemble0019((byte) 0x35, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>imm32</i>
     * Example disassembly syntax: {@code xor       rax, 0x12345678}
     */
    // Template#: 622, Serial#: 455
    public void xor_RAX(int imm32) {
        assemble0020((byte) 0x35, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorl  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code xorl      [rbx + 305419896], 0x12}
     */
    // Template#: 623, Serial#: 989
    public void xorl(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0021((byte) 0x83, (byte) 0x06, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorq  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code xorq      [rbx + 305419896], 0x12}
     */
    // Template#: 624, Serial#: 1061
    public void xorq(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0022((byte) 0x83, (byte) 0x06, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code xor       [rbx + 305419896], eax}
     */
    // Template#: 625, Serial#: 376
    public void xor(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0023((byte) 0x31, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code xor       [rbx + 305419896], rax}
     */
    // Template#: 626, Serial#: 385
    public void xor(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0024((byte) 0x31, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorl  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code xorl      [rbx + 305419896], 0x12345678}
     */
    // Template#: 627, Serial#: 773
    public void xorl(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0025((byte) 0x81, (byte) 0x06, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorq  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code xorq      [rbx + 305419896], 0x12345678}
     */
    // Template#: 628, Serial#: 845
    public void xorq(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0026((byte) 0x81, (byte) 0x06, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorpd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code xorpd     xmm0, xmm0}
     */
    // Template#: 629, Serial#: 6793
    public void xorpd(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0071((byte) 0x57, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorps  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code xorps     xmm0, xmm0}
     */
    // Template#: 630, Serial#: 6700
    public void xorps(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0075((byte) 0x57, destination, source);
    }

    private void assemble0001(byte opcode1, byte modRmOpcode, byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((1 << 6) | (modRmOpcode << 3)); // mod field, group opcode in reg field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
        emitByte(imm8); // appended
    }

    private void assemble0002(byte opcode1, byte modRmOpcode, byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((1 << 6) | (modRmOpcode << 3)); // mod field, group opcode in reg field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
        emitByte(imm8); // appended
    }

    private void assemble0003(byte opcode1, byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0004(byte opcode1, byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 3; // rm field extension by REX.B bit
        rex |= (source.value() & 8) >> 1; // mod field extension by REX.R bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0005(byte opcode1, byte modRmOpcode, byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((1 << 6) | (modRmOpcode << 3)); // mod field, group opcode in reg field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
        // appended:
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
    }

    private void assemble0006(byte opcode1, byte modRmOpcode, byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((1 << 6) | (modRmOpcode << 3)); // mod field, group opcode in reg field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
        // appended:
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
    }

    private void assemble0007(byte opcode1, byte modRmOpcode, AMD64GeneralRegister32 destination, byte imm8) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((3 << 6) | (modRmOpcode << 3)); // mod field, group opcode in reg field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        emitByte(imm8); // appended
    }

    private void assemble0008(byte opcode1, AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0009(byte opcode1, AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
    }

    private void assemble0010(byte opcode1, AMD64GeneralRegister32 destination, int rel32) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 5 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        // appended:
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
    }

    private void assemble0011(byte opcode1, byte modRmOpcode, AMD64GeneralRegister32 destination, int imm32) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((3 << 6) | (modRmOpcode << 3)); // mod field, group opcode in reg field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        // appended:
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
    }

    private void assemble0012(byte opcode1, AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0013(byte opcode1, byte modRmOpcode, AMD64GeneralRegister64 destination, byte imm8) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((3 << 6) | (modRmOpcode << 3)); // mod field, group opcode in reg field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        emitByte(imm8); // appended
    }

    private void assemble0014(byte opcode1, AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (source.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0015(byte opcode1, AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 3; // rm field extension by REX.B bit
        rex |= (source.value() & 8) >> 1; // mod field extension by REX.R bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
    }

    private void assemble0016(byte opcode1, AMD64GeneralRegister64 destination, int rel32) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 5 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        // appended:
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
    }

    private void assemble0017(byte opcode1, byte modRmOpcode, AMD64GeneralRegister64 destination, int imm32) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((3 << 6) | (modRmOpcode << 3)); // mod field, group opcode in reg field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        // appended:
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
    }

    private void assemble0018(byte opcode1, AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (source.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0019(byte opcode1, int imm32) {
        emitByte(opcode1);
        // appended:
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
    }

    private void assemble0020(byte opcode1, int imm32) {
        byte rex = (byte) 0x48;
        emitByte(rex);
        emitByte(opcode1);
        // appended:
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
    }

    private void assemble0021(byte opcode1, byte modRmOpcode, int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((2 << 6) | (modRmOpcode << 3)); // mod field, group opcode in reg field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        emitByte(imm8); // appended
    }

    private void assemble0022(byte opcode1, byte modRmOpcode, int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((2 << 6) | (modRmOpcode << 3)); // mod field, group opcode in reg field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        emitByte(imm8); // appended
    }

    private void assemble0023(byte opcode1, int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0024(byte opcode1, int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 3; // rm field extension by REX.B bit
        rex |= (source.value() & 8) >> 1; // mod field extension by REX.R bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0025(byte opcode1, byte modRmOpcode, int disp32, AMD64IndirectRegister64 destination, int imm32) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((2 << 6) | (modRmOpcode << 3)); // mod field, group opcode in reg field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        // appended:
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
    }

    private void assemble0026(byte opcode1, byte modRmOpcode, int disp32, AMD64IndirectRegister64 destination, int imm32) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((2 << 6) | (modRmOpcode << 3)); // mod field, group opcode in reg field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        // appended:
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
    }

    private void assemble0027(byte opcode2, AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0028(byte opcode2, AMD64XMMRegister destination, AMD64XMMRegister source) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
    }

    private void assemble0029(byte opcode2, AMD64XMMRegister destination, int rel32) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 5 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        // appended:
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
    }

    private void assemble0030(byte opcode2, AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0031(byte opcode2, AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0032(byte opcode2, AMD64XMMRegister destination, AMD64XMMRegister source) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
    }

    private void assemble0033(byte opcode2, AMD64XMMRegister destination, int rel32) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 5 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        // appended:
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
    }

    private void assemble0034(byte opcode2, AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0035(byte opcode2, AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0036(byte opcode2, AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
    }

    private void assemble0037(byte opcode2, AMD64GeneralRegister32 destination, AMD64IndirectRegister64 source) {
        if (source == AMD64IndirectRegister64.RBP_INDIRECT || source == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0035(opcode2, destination, (byte) 0, source);
            return;
        }
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
    }

    private void assemble0038(byte opcode2, AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0039(byte opcode2, AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (source.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0040(byte opcode2, AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (source.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
    }

    private void assemble0041(byte opcode2, AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        if (source == AMD64IndirectRegister64.RBP_INDIRECT || source == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0039(opcode2, destination, (byte) 0, source);
            return;
        }
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (source.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
    }

    private void assemble0042(byte opcode2, AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (source.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0043(byte opcode1, byte modRmOpcode, byte disp8, AMD64IndirectRegister64 destination) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((1 << 6) | (modRmOpcode << 3)); // mod field, group opcode in reg field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0044(byte opcode1, byte modRmOpcode, AMD64GeneralRegister64 destination) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((3 << 6) | (modRmOpcode << 3)); // mod field, group opcode in reg field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
    }

    private void assemble0045(byte opcode1, int rel32) {
        emitByte(opcode1);
        // appended:
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
    }

    private void assemble0046(byte opcode1, byte modRmOpcode, int rel32) {
        emitByte(opcode1);
        byte modRMByte = (byte) ((0 << 6) | (modRmOpcode << 3)); // mod field, group opcode in reg field
        modRMByte |= 5 << 0; // rm field
        emitByte(modRMByte);
        // appended:
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
    }

    private void assemble0047(byte opcode1, byte modRmOpcode, int disp32, AMD64IndirectRegister64 destination) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((2 << 6) | (modRmOpcode << 3)); // mod field, group opcode in reg field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0048(byte opcode1) {
        emitByte(opcode1);
    }

    private void assemble0049(byte opcode2, AMD64GeneralRegister32 destination, int rel32) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 5 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        // appended:
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
    }

    private void assemble0050(byte opcode1, AMD64GeneralRegister32 destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        emitByte(disp8); // appended
    }

    private void assemble0051(byte opcode1, AMD64GeneralRegister32 destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0050(opcode1, destination, (byte) 0, base, index, scale);
            return;
        }
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
    }

    private void assemble0052(byte opcode2, AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source, AMD64XMMComparison amd64xmmcomparison) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
        emitByte((byte) amd64xmmcomparison.value()); // appended
    }

    private void assemble0053(byte opcode2, AMD64XMMRegister destination, AMD64XMMRegister source, AMD64XMMComparison amd64xmmcomparison) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        emitByte((byte) amd64xmmcomparison.value()); // appended
    }

    private void assemble0054(byte opcode2, AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source, AMD64XMMComparison amd64xmmcomparison) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        emitByte((byte) amd64xmmcomparison.value()); // appended
    }

    private void assemble0055(byte opcode2, AMD64XMMRegister destination, int rel32, AMD64XMMComparison amd64xmmcomparison) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 5 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        // appended:
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        emitByte((byte) amd64xmmcomparison.value()); // appended
    }

    private void assemble0056(byte opcode2, AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source, AMD64XMMComparison amd64xmmcomparison) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
        emitByte((byte) amd64xmmcomparison.value()); // appended
    }

    private void assemble0057(byte opcode2, AMD64XMMRegister destination, AMD64XMMRegister source, AMD64XMMComparison amd64xmmcomparison) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        emitByte((byte) amd64xmmcomparison.value()); // appended
    }

    private void assemble0058(byte opcode2, AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source, AMD64XMMComparison amd64xmmcomparison) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        emitByte((byte) amd64xmmcomparison.value()); // appended
    }

    private void assemble0059(byte opcode2, AMD64XMMRegister destination, int rel32, AMD64XMMComparison amd64xmmcomparison) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 5 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        // appended:
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        emitByte((byte) amd64xmmcomparison.value()); // appended
    }

    private void assemble0060(byte opcode2, byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0061(byte opcode2, byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 3; // rm field extension by REX.B bit
        rex |= (source.value() & 8) >> 1; // mod field extension by REX.R bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0062(byte opcode2, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister32 source) {
        byte rex = (byte) 0;
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        emitByte(disp8); // appended
    }

    private void assemble0063(byte opcode2, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister32 source) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0062(opcode2, (byte) 0, base, index, scale, source);
            return;
        }
        byte rex = (byte) 0;
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
    }

    private void assemble0064(byte opcode2, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister64 source) {
        byte rex = (byte) 0x48;
        rex |= (base.value() & 8) >> 3; // SIB base field extension by REX.B bit
        rex |= (index.value() & 8) >> 2; // SIB index field extension by REX.X bit
        rex |= (source.value() & 8) >> 1; // mod field extension by REX.R bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        emitByte(disp8); // appended
    }

    private void assemble0065(byte opcode2, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister64 source) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0064(opcode2, (byte) 0, base, index, scale, source);
            return;
        }
        byte rex = (byte) 0x48;
        rex |= (base.value() & 8) >> 3; // SIB base field extension by REX.B bit
        rex |= (index.value() & 8) >> 2; // SIB index field extension by REX.X bit
        rex |= (source.value() & 8) >> 1; // mod field extension by REX.R bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
    }

    private void assemble0066(byte opcode2, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        if (destination == AMD64IndirectRegister64.RBP_INDIRECT || destination == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0060(opcode2, (byte) 0, destination, source);
            return;
        }
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
    }

    private void assemble0067(byte opcode2, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        if (destination == AMD64IndirectRegister64.RBP_INDIRECT || destination == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0061(opcode2, (byte) 0, destination, source);
            return;
        }
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 3; // rm field extension by REX.B bit
        rex |= (source.value() & 8) >> 1; // mod field extension by REX.R bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
    }

    private void assemble0068(byte opcode2, int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0069(byte opcode2, int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 3; // rm field extension by REX.B bit
        rex |= (source.value() & 8) >> 1; // mod field extension by REX.R bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0070(byte opcode2, AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        emitByte(((byte) 0x66)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0071(byte opcode2, AMD64XMMRegister destination, AMD64XMMRegister source) {
        emitByte(((byte) 0x66)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
    }

    private void assemble0072(byte opcode2, AMD64XMMRegister destination, int rel32) {
        emitByte(((byte) 0x66)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 5 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        // appended:
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
    }

    private void assemble0073(byte opcode2, AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        emitByte(((byte) 0x66)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0074(byte opcode2, AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0075(byte opcode2, AMD64XMMRegister destination, AMD64XMMRegister source) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
    }

    private void assemble0076(byte opcode2, AMD64XMMRegister destination, int rel32) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 5 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        // appended:
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
    }

    private void assemble0077(byte opcode2, AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0078(byte opcode1) {
        byte rex = (byte) 0x48;
        emitByte(rex);
        emitByte(opcode1);
    }

    private void assemble0079(byte opcode2, AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0080(byte opcode2, AMD64GeneralRegister32 destination, AMD64XMMRegister source) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
    }

    private void assemble0081(byte opcode2, AMD64GeneralRegister32 destination, int rel32) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 5 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        // appended:
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
    }

    private void assemble0082(byte opcode2, AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0083(byte opcode2, AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (source.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0084(byte opcode2, AMD64GeneralRegister64 destination, AMD64XMMRegister source) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (source.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
    }

    private void assemble0085(byte opcode2, AMD64GeneralRegister64 destination, int rel32) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 5 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        // appended:
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
    }

    private void assemble0086(byte opcode2, AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (source.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0087(byte opcode2, AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (source.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0088(byte opcode2, AMD64XMMRegister destination, AMD64GeneralRegister32 source) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
    }

    private void assemble0089(byte opcode2, AMD64XMMRegister destination, AMD64GeneralRegister64 source) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (source.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
    }

    private void assemble0090(byte opcode2, AMD64XMMRegister destination, int rel32) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 5 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        // appended:
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
    }

    private void assemble0091(byte opcode2, AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (source.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0092(byte opcode2, AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (source.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0093(byte opcode2, AMD64XMMRegister destination, AMD64GeneralRegister32 source) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
    }

    private void assemble0094(byte opcode2, AMD64XMMRegister destination, AMD64GeneralRegister64 source) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (source.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
    }

    private void assemble0095(byte opcode2, AMD64XMMRegister destination, int rel32) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 5 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        // appended:
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
    }

    private void assemble0096(byte opcode2, AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (source.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0097(byte opcode2, AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0098(byte opcode2, AMD64GeneralRegister32 destination, AMD64XMMRegister source) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
    }

    private void assemble0099(byte opcode2, AMD64GeneralRegister32 destination, int rel32) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 5 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        // appended:
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
    }

    private void assemble0100(byte opcode2, AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0101(byte opcode2, AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (source.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0102(byte opcode2, AMD64GeneralRegister64 destination, AMD64XMMRegister source) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (source.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
    }

    private void assemble0103(byte opcode2, AMD64GeneralRegister64 destination, int rel32) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 5 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        // appended:
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
    }

    private void assemble0104(byte opcode2, AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (source.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0105(byte opcode1, byte modRmOpcode, AMD64GeneralRegister64 destination) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((3 << 6) | (modRmOpcode << 3)); // mod field, group opcode in reg field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
    }

    private void assemble0106(byte opcode1, byte modRmOpcode, byte disp8, AMD64IndirectRegister64 destination) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((1 << 6) | (modRmOpcode << 3)); // mod field, group opcode in reg field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0107(byte opcode1, byte modRmOpcode, int rel32) {
        byte rex = (byte) 0x48;
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((0 << 6) | (modRmOpcode << 3)); // mod field, group opcode in reg field
        modRMByte |= 5 << 0; // rm field
        emitByte(modRMByte);
        // appended:
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
    }

    private void assemble0108(byte opcode1, byte modRmOpcode, int disp32, AMD64IndirectRegister64 destination) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((2 << 6) | (modRmOpcode << 3)); // mod field, group opcode in reg field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0109(byte opcode1, short imm16, byte imm8) {
        emitByte(opcode1);
        // appended:
        emitByte((byte) (imm16 & 0xff));
        imm16 >>= 8;
        emitByte((byte) (imm16 & 0xff));
        emitByte(imm8); // appended
    }

    private void assemble0110(byte opcode1, byte modRmOpcode, AMD64GeneralRegister32 destination) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((3 << 6) | (modRmOpcode << 3)); // mod field, group opcode in reg field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
    }

    private void assemble0111(byte opcode1, AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source, byte imm8) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        emitByte(imm8); // appended
    }

    private void assemble0112(byte opcode1, AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source, int imm32) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        // appended:
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
    }

    private void assemble0113(byte opcode1, AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source, byte imm8) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (source.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        emitByte(imm8); // appended
    }

    private void assemble0114(byte opcode1, AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source, int imm32) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (source.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        // appended:
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
    }

    private void assemble0115(byte opcode2, AMD64GeneralRegister64 destination, int rel32) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 5 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        // appended:
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
    }

    private void assemble0116(byte opcode1, byte rel8) {
        emitByte(opcode1);
        emitByte(rel8); // appended
    }

    private void assemble0117(byte opcode2, int rel32) {
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        // appended:
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
    }

    private void assemble0118(byte opcode1, AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        if (source == AMD64IndirectRegister64.RBP_INDIRECT || source == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0014(opcode1, destination, (byte) 0, source);
            return;
        }
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (source.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
    }

    private void assemble0119(byte opcode2, byte modRmOpcode) {
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((3 << 6) | (modRmOpcode << 3)); // mod field, group opcode in reg field
        emitByte(modRMByte);
    }

    private void assemble0120(byte opcode1, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, byte imm8) {
        byte rex = (byte) 0;
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        emitByte(disp8); // appended
        emitByte(imm8); // appended
    }

    private void assemble0121(byte opcode1, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister16 source) {
        emitByte(((byte) 0x66)); // operand size prefix
        byte rex = (byte) 0;
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        emitByte(disp8); // appended
    }

    private void assemble0122(byte opcode1, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister32 source) {
        byte rex = (byte) 0;
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        emitByte(disp8); // appended
    }

    private void assemble0123(byte opcode1, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister64 source) {
        byte rex = (byte) 0x48;
        rex |= (base.value() & 8) >> 3; // SIB base field extension by REX.B bit
        rex |= (index.value() & 8) >> 2; // SIB index field extension by REX.X bit
        rex |= (source.value() & 8) >> 1; // mod field extension by REX.R bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        emitByte(disp8); // appended
    }

    private void assemble0124(byte opcode1, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister8 source) {
        byte rex = (byte) 0;
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (source.requiresRexPrefix()) {
            rex |= 0x40;
            if (source.value() >= 8) {
                rex |= 1 << 2; // mod field extension by REX.R bit
            }
        }
        if (rex != (byte) 0) {
            if (source.isHighByte()) {
                throw new IllegalArgumentException("Cannot encode " + source.name() + " in the presence of a REX prefix");
            }
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        emitByte(disp8); // appended
    }

    private void assemble0125(byte opcode1, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, int imm32) {
        byte rex = (byte) 0;
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        emitByte(disp8); // appended
        // appended:
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
    }

    private void assemble0126(byte opcode1, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, short imm16) {
        emitByte(((byte) 0x66)); // operand size prefix
        byte rex = (byte) 0;
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        emitByte(disp8); // appended
        // appended:
        emitByte((byte) (imm16 & 0xff));
        imm16 >>= 8;
        emitByte((byte) (imm16 & 0xff));
    }

    private void assemble0127(byte opcode1, byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
        emitByte(imm8); // appended
    }

    private void assemble0128(byte opcode1, byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister16 source) {
        emitByte(((byte) 0x66)); // operand size prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0129(byte opcode1, byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister8 source) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (source.requiresRexPrefix()) {
            rex |= 0x40;
            if (source.value() >= 8) {
                rex |= 1 << 2; // mod field extension by REX.R bit
            }
        }
        if (rex != (byte) 0) {
            if (source.isHighByte()) {
                throw new IllegalArgumentException("Cannot encode " + source.name() + " in the presence of a REX prefix");
            }
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0130(byte opcode1, byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
        // appended:
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
    }

    private void assemble0131(byte opcode1, byte disp8, AMD64IndirectRegister64 destination, short imm16) {
        emitByte(((byte) 0x66)); // operand size prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
        // appended:
        emitByte((byte) (imm16 & 0xff));
        imm16 >>= 8;
        emitByte((byte) (imm16 & 0xff));
    }

    private void assemble0132(byte opcode1, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, byte imm8) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0120(opcode1, (byte) 0, base, index, scale, imm8);
            return;
        }
        byte rex = (byte) 0;
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        emitByte(imm8); // appended
    }

    private void assemble0133(byte opcode1, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister16 source) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0121(opcode1, (byte) 0, base, index, scale, source);
            return;
        }
        emitByte(((byte) 0x66)); // operand size prefix
        byte rex = (byte) 0;
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
    }

    private void assemble0134(byte opcode1, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister32 source) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0122(opcode1, (byte) 0, base, index, scale, source);
            return;
        }
        byte rex = (byte) 0;
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
    }

    private void assemble0135(byte opcode1, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister64 source) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0123(opcode1, (byte) 0, base, index, scale, source);
            return;
        }
        byte rex = (byte) 0x48;
        rex |= (base.value() & 8) >> 3; // SIB base field extension by REX.B bit
        rex |= (index.value() & 8) >> 2; // SIB index field extension by REX.X bit
        rex |= (source.value() & 8) >> 1; // mod field extension by REX.R bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
    }

    private void assemble0136(byte opcode1, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister8 source) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0124(opcode1, (byte) 0, base, index, scale, source);
            return;
        }
        byte rex = (byte) 0;
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (source.requiresRexPrefix()) {
            rex |= 0x40;
            if (source.value() >= 8) {
                rex |= 1 << 2; // mod field extension by REX.R bit
            }
        }
        if (rex != (byte) 0) {
            if (source.isHighByte()) {
                throw new IllegalArgumentException("Cannot encode " + source.name() + " in the presence of a REX prefix");
            }
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
    }

    private void assemble0137(byte opcode1, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, int imm32) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0125(opcode1, (byte) 0, base, index, scale, imm32);
            return;
        }
        byte rex = (byte) 0;
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        // appended:
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
    }

    private void assemble0138(byte opcode1, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, short imm16) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0126(opcode1, (byte) 0, base, index, scale, imm16);
            return;
        }
        emitByte(((byte) 0x66)); // operand size prefix
        byte rex = (byte) 0;
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        // appended:
        emitByte((byte) (imm16 & 0xff));
        imm16 >>= 8;
        emitByte((byte) (imm16 & 0xff));
    }

    private void assemble0139(byte opcode1, AMD64GeneralRegister32 destination, int imm32) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        // appended:
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
    }

    private void assemble0140(byte opcode1, AMD64GeneralRegister32 register, int imm32) {
        byte rex = (byte) 0;
        if (register.value() >= 8) {
            rex |= (1 << 0) + 0x40; // opcode1 extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (opcode1 + (register.value()& 7))); // opcode1_rexb
        // appended:
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
    }

    private void assemble0141(byte opcode1, AMD64GeneralRegister64 destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (base.value() & 8) >> 3; // SIB base field extension by REX.B bit
        rex |= (index.value() & 8) >> 2; // SIB index field extension by REX.X bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        emitByte(disp8); // appended
    }

    private void assemble0142(byte opcode1, AMD64GeneralRegister64 destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0141(opcode1, destination, (byte) 0, base, index, scale);
            return;
        }
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (base.value() & 8) >> 3; // SIB base field extension by REX.B bit
        rex |= (index.value() & 8) >> 2; // SIB index field extension by REX.X bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
    }

    private void assemble0143(byte opcode1, AMD64GeneralRegister64 destination, int imm32) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        // appended:
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
    }

    private void assemble0144(byte opcode1, AMD64GeneralRegister64 destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (base.value() & 8) >> 3; // SIB base field extension by REX.B bit
        rex |= (index.value() & 8) >> 2; // SIB index field extension by REX.X bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0145(byte opcode1, AMD64GeneralRegister64 register, long imm64) {
        byte rex = (byte) 0x48;
        rex |= (register.value() & 8) >> 3; // opcode1 extension by REX.B bit
        emitByte(rex);
        emitByte((byte) (opcode1 + (register.value()& 7))); // opcode1_rexb
        // appended:
        emitByte((byte) (imm64 & 0xff));
        imm64 >>= 8;
        emitByte((byte) (imm64 & 0xff));
        imm64 >>= 8;
        emitByte((byte) (imm64 & 0xff));
        imm64 >>= 8;
        emitByte((byte) (imm64 & 0xff));
        imm64 >>= 8;
        emitByte((byte) (imm64 & 0xff));
        imm64 >>= 8;
        emitByte((byte) (imm64 & 0xff));
        imm64 >>= 8;
        emitByte((byte) (imm64 & 0xff));
        imm64 >>= 8;
        emitByte((byte) (imm64 & 0xff));
    }

    private void assemble0146(byte opcode1, AMD64IndirectRegister64 destination, byte imm8) {
        if (destination == AMD64IndirectRegister64.RBP_INDIRECT || destination == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0127(opcode1, (byte) 0, destination, imm8);
            return;
        }
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(imm8); // appended
    }

    private void assemble0147(byte opcode1, AMD64IndirectRegister64 destination, AMD64GeneralRegister16 source) {
        if (destination == AMD64IndirectRegister64.RBP_INDIRECT || destination == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0128(opcode1, (byte) 0, destination, source);
            return;
        }
        emitByte(((byte) 0x66)); // operand size prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
    }

    private void assemble0148(byte opcode1, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        if (destination == AMD64IndirectRegister64.RBP_INDIRECT || destination == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0003(opcode1, (byte) 0, destination, source);
            return;
        }
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
    }

    private void assemble0149(byte opcode1, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        if (destination == AMD64IndirectRegister64.RBP_INDIRECT || destination == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0004(opcode1, (byte) 0, destination, source);
            return;
        }
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 3; // rm field extension by REX.B bit
        rex |= (source.value() & 8) >> 1; // mod field extension by REX.R bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
    }

    private void assemble0150(byte opcode1, AMD64IndirectRegister64 destination, AMD64GeneralRegister8 source) {
        if (destination == AMD64IndirectRegister64.RBP_INDIRECT || destination == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0129(opcode1, (byte) 0, destination, source);
            return;
        }
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (source.requiresRexPrefix()) {
            rex |= 0x40;
            if (source.value() >= 8) {
                rex |= 1 << 2; // mod field extension by REX.R bit
            }
        }
        if (rex != (byte) 0) {
            if (source.isHighByte()) {
                throw new IllegalArgumentException("Cannot encode " + source.name() + " in the presence of a REX prefix");
            }
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
    }

    private void assemble0151(byte opcode1, AMD64IndirectRegister64 destination, int imm32) {
        if (destination == AMD64IndirectRegister64.RBP_INDIRECT || destination == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0130(opcode1, (byte) 0, destination, imm32);
            return;
        }
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
    }

    private void assemble0152(byte opcode1, AMD64IndirectRegister64 destination, short imm16) {
        if (destination == AMD64IndirectRegister64.RBP_INDIRECT || destination == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0131(opcode1, (byte) 0, destination, imm16);
            return;
        }
        emitByte(((byte) 0x66)); // operand size prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (imm16 & 0xff));
        imm16 >>= 8;
        emitByte((byte) (imm16 & 0xff));
    }

    private void assemble0153(byte opcode1, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, byte imm8) {
        byte rex = (byte) 0;
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        emitByte(imm8); // appended
    }

    private void assemble0154(byte opcode1, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister16 source) {
        emitByte(((byte) 0x66)); // operand size prefix
        byte rex = (byte) 0;
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0155(byte opcode1, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister32 source) {
        byte rex = (byte) 0;
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0156(byte opcode1, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister64 source) {
        byte rex = (byte) 0x48;
        rex |= (base.value() & 8) >> 3; // SIB base field extension by REX.B bit
        rex |= (index.value() & 8) >> 2; // SIB index field extension by REX.X bit
        rex |= (source.value() & 8) >> 1; // mod field extension by REX.R bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0157(byte opcode1, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister8 source) {
        byte rex = (byte) 0;
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (source.requiresRexPrefix()) {
            rex |= 0x40;
            if (source.value() >= 8) {
                rex |= 1 << 2; // mod field extension by REX.R bit
            }
        }
        if (rex != (byte) 0) {
            if (source.isHighByte()) {
                throw new IllegalArgumentException("Cannot encode " + source.name() + " in the presence of a REX prefix");
            }
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0158(byte opcode1, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, int imm32) {
        byte rex = (byte) 0;
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        // appended:
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
    }

    private void assemble0159(byte opcode1, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, short imm16) {
        emitByte(((byte) 0x66)); // operand size prefix
        byte rex = (byte) 0;
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        // appended:
        emitByte((byte) (imm16 & 0xff));
        imm16 >>= 8;
        emitByte((byte) (imm16 & 0xff));
    }

    private void assemble0160(byte opcode1, int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        emitByte(imm8); // appended
    }

    private void assemble0161(byte opcode1, int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister16 source) {
        emitByte(((byte) 0x66)); // operand size prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0162(byte opcode1, int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister8 source) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (source.requiresRexPrefix()) {
            rex |= 0x40;
            if (source.value() >= 8) {
                rex |= 1 << 2; // mod field extension by REX.R bit
            }
        }
        if (rex != (byte) 0) {
            if (source.isHighByte()) {
                throw new IllegalArgumentException("Cannot encode " + source.name() + " in the presence of a REX prefix");
            }
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0163(byte opcode1, int disp32, AMD64IndirectRegister64 destination, int imm32) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        // appended:
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
    }

    private void assemble0164(byte opcode1, int disp32, AMD64IndirectRegister64 destination, int imm32) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        // appended:
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
        imm32 >>= 8;
        emitByte((byte) (imm32 & 0xff));
    }

    private void assemble0165(byte opcode1, int disp32, AMD64IndirectRegister64 destination, short imm16) {
        emitByte(((byte) 0x66)); // operand size prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        // appended:
        emitByte((byte) (imm16 & 0xff));
        imm16 >>= 8;
        emitByte((byte) (imm16 & 0xff));
    }

    private void assemble0166(byte opcode2, byte disp8, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        emitByte(((byte) 0x66)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0167(byte opcode2, byte disp8, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        emitByte(((byte) 0x66)); // instruction selection prefix
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 3; // rm field extension by REX.B bit
        rex |= (source.value() & 8) >> 1; // mod field extension by REX.R bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0168(byte opcode2, AMD64GeneralRegister32 destination, AMD64XMMRegister source) {
        emitByte(((byte) 0x66)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
    }

    private void assemble0169(byte opcode2, AMD64GeneralRegister64 destination, AMD64XMMRegister source) {
        emitByte(((byte) 0x66)); // instruction selection prefix
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 3; // rm field extension by REX.B bit
        rex |= (source.value() & 8) >> 1; // mod field extension by REX.R bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
    }

    private void assemble0170(byte opcode2, AMD64XMMRegister destination, AMD64GeneralRegister32 source) {
        emitByte(((byte) 0x66)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
    }

    private void assemble0171(byte opcode2, AMD64XMMRegister destination, AMD64GeneralRegister64 source) {
        emitByte(((byte) 0x66)); // instruction selection prefix
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (source.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
    }

    private void assemble0172(byte opcode2, AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        emitByte(((byte) 0x66)); // instruction selection prefix
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (source.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0173(byte opcode2, int disp32, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        emitByte(((byte) 0x66)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0174(byte opcode2, int disp32, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        emitByte(((byte) 0x66)); // instruction selection prefix
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 3; // rm field extension by REX.B bit
        rex |= (source.value() & 8) >> 1; // mod field extension by REX.R bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0175(byte opcode2, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64XMMRegister source) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0;
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        emitByte(disp8); // appended
    }

    private void assemble0176(byte opcode2, byte disp8, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0177(byte opcode2, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64XMMRegister source) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0175(opcode2, (byte) 0, base, index, scale, source);
            return;
        }
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0;
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
    }

    private void assemble0178(byte opcode2, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        if (destination == AMD64IndirectRegister64.RBP_INDIRECT || destination == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0176(opcode2, (byte) 0, destination, source);
            return;
        }
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
    }

    private void assemble0179(byte opcode2, AMD64XMMRegister destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        emitByte(disp8); // appended
    }

    private void assemble0180(byte opcode2, AMD64XMMRegister destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0179(opcode2, destination, (byte) 0, base, index, scale);
            return;
        }
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
    }

    private void assemble0181(byte opcode2, AMD64XMMRegister destination, AMD64IndirectRegister64 source) {
        if (source == AMD64IndirectRegister64.RBP_INDIRECT || source == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0027(opcode2, destination, (byte) 0, source);
            return;
        }
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
    }

    private void assemble0182(byte opcode2, AMD64XMMRegister destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0183(byte opcode2, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64XMMRegister source) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0;
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0184(byte opcode2, int disp32, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        emitByte(((byte) 0xF2)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0185(byte opcode2, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64XMMRegister source) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0;
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        emitByte(disp8); // appended
    }

    private void assemble0186(byte opcode2, byte disp8, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0187(byte opcode2, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64XMMRegister source) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0185(opcode2, (byte) 0, base, index, scale, source);
            return;
        }
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0;
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
    }

    private void assemble0188(byte opcode2, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        if (destination == AMD64IndirectRegister64.RBP_INDIRECT || destination == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0186(opcode2, (byte) 0, destination, source);
            return;
        }
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
    }

    private void assemble0189(byte opcode2, AMD64XMMRegister destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        emitByte(disp8); // appended
    }

    private void assemble0190(byte opcode2, AMD64XMMRegister destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0189(opcode2, destination, (byte) 0, base, index, scale);
            return;
        }
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
    }

    private void assemble0191(byte opcode2, AMD64XMMRegister destination, AMD64IndirectRegister64 source) {
        if (source == AMD64IndirectRegister64.RBP_INDIRECT || source == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0031(opcode2, destination, (byte) 0, source);
            return;
        }
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
    }

    private void assemble0192(byte opcode2, AMD64XMMRegister destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0193(byte opcode2, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64XMMRegister source) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0;
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0194(byte opcode2, int disp32, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        emitByte(((byte) 0xF3)); // instruction selection prefix
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        modRMByte |= (source.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        if (destination == AMD64IndirectRegister64.RSP_INDIRECT || destination == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0195(byte opcode2, AMD64GeneralRegister32 destination, AMD64GeneralRegister8 source) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.requiresRexPrefix()) {
            rex |= 0x40;
            if (source.value() >= 8) {
                rex |= 1 << 0; // rm field extension by REX.B bit
            }
        }
        if (rex != (byte) 0) {
            if (source.isHighByte()) {
                throw new IllegalArgumentException("Cannot encode " + source.name() + " in the presence of a REX prefix");
            }
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
    }

    private void assemble0196(byte opcode2, AMD64GeneralRegister64 destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (base.value() & 8) >> 3; // SIB base field extension by REX.B bit
        rex |= (index.value() & 8) >> 2; // SIB index field extension by REX.X bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        emitByte(disp8); // appended
    }

    private void assemble0197(byte opcode2, AMD64GeneralRegister64 destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0196(opcode2, destination, (byte) 0, base, index, scale);
            return;
        }
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (base.value() & 8) >> 3; // SIB base field extension by REX.B bit
        rex |= (index.value() & 8) >> 2; // SIB index field extension by REX.X bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
    }

    private void assemble0198(byte opcode2, AMD64GeneralRegister64 destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (base.value() & 8) >> 3; // SIB base field extension by REX.B bit
        rex |= (index.value() & 8) >> 2; // SIB index field extension by REX.X bit
        emitByte(rex);
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0199(byte opcode1, AMD64GeneralRegister64 destination, AMD64GeneralRegister32 source) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 1; // mod field extension by REX.R bit
        rex |= (source.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
    }

    private void assemble0200(byte opcode2, AMD64GeneralRegister32 destination, AMD64GeneralRegister16 source) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
    }

    private void assemble0201(byte opcode1, AMD64GeneralRegister64 destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        emitByte(disp8); // appended
    }

    private void assemble0202(byte opcode1, AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((1 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        emitByte(disp8); // appended
    }

    private void assemble0203(byte opcode1, AMD64GeneralRegister64 destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0201(opcode1, destination, (byte) 0, base, index, scale);
            return;
        }
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
    }

    private void assemble0204(byte opcode1, AMD64GeneralRegister64 destination, AMD64GeneralRegister32 source) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
    }

    private void assemble0205(byte opcode1, AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        if (source == AMD64IndirectRegister64.RBP_INDIRECT || source == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0202(opcode1, destination, (byte) 0, source);
            return;
        }
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
    }

    private void assemble0206(byte opcode1, AMD64GeneralRegister64 destination, int rel32) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((0 << 6)); // mod field
        modRMByte |= 5 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        // appended:
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
        rel32 >>= 8;
        emitByte((byte) (rel32 & 0xff));
    }

    private void assemble0207(byte opcode1, AMD64GeneralRegister64 destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (base.value() >= 8) {
            rex |= (1 << 0) + 0x40; // SIB base field extension by REX.B bit
        }
        if (index.value() >= 8) {
            rex |= (1 << 1) + 0x40; // SIB index field extension by REX.X bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= 4 << 0; // rm field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        emitByte(modRMByte);
        byte sibByte = (byte) 0;
        sibByte |= (base.value() & 7) << 0; // base field
        sibByte |= (index.value() & 7) << 3; // index field
        sibByte |= scale.value() << 6; // scale field
        emitByte(sibByte);
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0208(byte opcode1, AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        byte rex = (byte) 0;
        if (destination.value() >= 8) {
            rex |= (1 << 2) + 0x40; // mod field extension by REX.R bit
        }
        if (source.value() >= 8) {
            rex |= (1 << 0) + 0x40; // rm field extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte(opcode1);
        byte modRMByte = (byte) ((2 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 3; // reg field
        modRMByte |= (source.value() & 7) << 0; // rm field
        emitByte(modRMByte);
        if (source == AMD64IndirectRegister64.RSP_INDIRECT || source == AMD64IndirectRegister64.R12_INDIRECT) {
            emitByte(((byte) 0x24)); // SIB byte
        }
        // appended:
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
        disp32 >>= 8;
        emitByte((byte) (disp32 & 0xff));
    }

    private void assemble0209(byte opcode1, AMD64GeneralRegister64 register) {
        byte rex = (byte) 0;
        if (register.value() >= 8) {
            rex |= (1 << 0) + 0x40; // opcode1 extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (opcode1 + (register.value()& 7))); // opcode1_rexb
    }

    private void assemble0210(byte opcode1, short imm16) {
        emitByte(opcode1);
        // appended:
        emitByte((byte) (imm16 & 0xff));
        imm16 >>= 8;
        emitByte((byte) (imm16 & 0xff));
    }

    private void assemble0211(byte opcode2, AMD64GeneralRegister8 destination) {
        byte rex = (byte) 0;
        if (destination.requiresRexPrefix()) {
            rex |= 0x40;
            if (destination.value() >= 8) {
                rex |= 1 << 0; // rm field extension by REX.B bit
            }
        }
        if (rex != (byte) 0) {
            if (destination.isHighByte()) {
                throw new IllegalArgumentException("Cannot encode " + destination.name() + " in the presence of a REX prefix");
            }
            emitByte(rex);
        }
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((3 << 6)); // mod field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
    }

// END GENERATED RAW ASSEMBLER METHODS

// START GENERATED LABEL ASSEMBLER METHODS
    /**
     * Pseudo-external assembler syntax: {@code add  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code add       eax, [L1: +305419896]}
     */
    // Template#: 1, Serial#: 85
    public void rip_add(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_add(destination, placeHolder);
        new rip_add_85(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code add       rax, [L1: +305419896]}
     */
    // Template#: 2, Serial#: 94
    public void rip_add(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_add(destination, placeHolder);
        new rip_add_94(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code addsd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code addsd     xmm0, [L1: +305419896]}
     */
    // Template#: 3, Serial#: 10251
    public void rip_addsd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_addsd(destination, placeHolder);
        new rip_addsd_10251(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code addss  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code addss     xmm0, [L1: +305419896]}
     */
    // Template#: 4, Serial#: 10377
    public void rip_addss(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_addss(destination, placeHolder);
        new rip_addss_10377(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code and       eax, [L1: +305419896]}
     */
    // Template#: 5, Serial#: 313
    public void rip_and(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_and(destination, placeHolder);
        new rip_and_313(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code and       rax, [L1: +305419896]}
     */
    // Template#: 6, Serial#: 322
    public void rip_and(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_and(destination, placeHolder);
        new rip_and_322(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code call  }<i>label</i>
     * Example disassembly syntax: {@code call      L1: +305419896}
     */
    // Template#: 7, Serial#: 5288
    public void call(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        call(placeHolder);
        new call_5288(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code call  }<i>label</i>
     * Example disassembly syntax: {@code call      [L1: +305419896]}
     */
    // Template#: 8, Serial#: 5432
    public void rip_call(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_call(placeHolder);
        new rip_call_5432(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmova  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmova     eax, [L1: +305419896]}
     */
    // Template#: 9, Serial#: 6557
    public void rip_cmova(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmova(destination, placeHolder);
        new rip_cmova_6557(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovae  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovae    eax, [L1: +305419896]}
     */
    // Template#: 10, Serial#: 6449
    public void rip_cmovae(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovae(destination, placeHolder);
        new rip_cmovae_6449(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovb     eax, [L1: +305419896]}
     */
    // Template#: 11, Serial#: 6422
    public void rip_cmovb(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovb(destination, placeHolder);
        new rip_cmovb_6422(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovbe  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovbe    eax, [L1: +305419896]}
     */
    // Template#: 12, Serial#: 6530
    public void rip_cmovbe(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovbe(destination, placeHolder);
        new rip_cmovbe_6530(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmove  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmove     eax, [L1: +305419896]}
     */
    // Template#: 13, Serial#: 6476
    public void rip_cmove(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmove(destination, placeHolder);
        new rip_cmove_6476(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovg  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovg     eax, [L1: +305419896]}
     */
    // Template#: 14, Serial#: 9936
    public void rip_cmovg(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovg(destination, placeHolder);
        new rip_cmovg_9936(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovge  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovge    eax, [L1: +305419896]}
     */
    // Template#: 15, Serial#: 9882
    public void rip_cmovge(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovge(destination, placeHolder);
        new rip_cmovge_9882(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovl  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovl     eax, [L1: +305419896]}
     */
    // Template#: 16, Serial#: 9855
    public void rip_cmovl(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovl(destination, placeHolder);
        new rip_cmovl_9855(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovle  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovle    eax, [L1: +305419896]}
     */
    // Template#: 17, Serial#: 9909
    public void rip_cmovle(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovle(destination, placeHolder);
        new rip_cmovle_9909(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovp  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovp     eax, [L1: +305419896]}
     */
    // Template#: 18, Serial#: 9801
    public void rip_cmovp(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovp(destination, placeHolder);
        new rip_cmovp_9801(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmp       eax, [L1: +305419896]}
     */
    // Template#: 19, Serial#: 3545
    public void rip_cmp(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmp(destination, placeHolder);
        new rip_cmp_3545(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmp       rax, [L1: +305419896]}
     */
    // Template#: 20, Serial#: 3554
    public void rip_cmp(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmp(destination, placeHolder);
        new rip_cmp_3554(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpsd  }<i>destination</i>, <i>label</i>, <i>amd64xmmcomparison</i>
     * Example disassembly syntax: {@code cmpsd     xmm0, [L1: +305419896], less_than_or_equal}
     */
    // Template#: 21, Serial#: 8139
    public void rip_cmpsd(final AMD64XMMRegister destination, final Label label, final AMD64XMMComparison amd64xmmcomparison) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmpsd(destination, placeHolder, amd64xmmcomparison);
        new rip_cmpsd_8139(startPosition, currentPosition() - startPosition, destination, amd64xmmcomparison, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpss  }<i>destination</i>, <i>label</i>, <i>amd64xmmcomparison</i>
     * Example disassembly syntax: {@code cmpss     xmm0, [L1: +305419896], less_than_or_equal}
     */
    // Template#: 22, Serial#: 8157
    public void rip_cmpss(final AMD64XMMRegister destination, final Label label, final AMD64XMMComparison amd64xmmcomparison) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmpss(destination, placeHolder, amd64xmmcomparison);
        new rip_cmpss_8157(startPosition, currentPosition() - startPosition, destination, amd64xmmcomparison, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code comisd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code comisd    xmm0, [L1: +305419896]}
     */
    // Template#: 23, Serial#: 9621
    public void rip_comisd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_comisd(destination, placeHolder);
        new rip_comisd_9621(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code comiss  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code comiss    xmm0, [L1: +305419896]}
     */
    // Template#: 24, Serial#: 9462
    public void rip_comiss(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_comiss(destination, placeHolder);
        new rip_comiss_9462(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2si  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtsd2si  eax, [L1: +305419896]}
     */
    // Template#: 25, Serial#: 9675
    public void rip_cvtsd2si(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtsd2si(destination, placeHolder);
        new rip_cvtsd2si_9675(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2si  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtsd2si  rax, [L1: +305419896]}
     */
    // Template#: 26, Serial#: 9684
    public void rip_cvtsd2si(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtsd2si(destination, placeHolder);
        new rip_cvtsd2si_9684(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2ss  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtsd2ss  xmm0, [L1: +305419896]}
     */
    // Template#: 27, Serial#: 10287
    public void rip_cvtsd2ss(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtsd2ss(destination, placeHolder);
        new rip_cvtsd2ss_10287(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2sdl  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtsi2sdl  xmm0, [L1: +305419896]}
     */
    // Template#: 28, Serial#: 9639
    public void rip_cvtsi2sdl(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtsi2sdl(destination, placeHolder);
        new rip_cvtsi2sdl_9639(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2sdq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtsi2sdq  xmm0, [L1: +305419896]}
     */
    // Template#: 29, Serial#: 9648
    public void rip_cvtsi2sdq(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtsi2sdq(destination, placeHolder);
        new rip_cvtsi2sdq_9648(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2ssl  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtsi2ssl  xmm0, [L1: +305419896]}
     */
    // Template#: 30, Serial#: 9693
    public void rip_cvtsi2ssl(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtsi2ssl(destination, placeHolder);
        new rip_cvtsi2ssl_9693(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2ssq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtsi2ssq  xmm0, [L1: +305419896]}
     */
    // Template#: 31, Serial#: 9702
    public void rip_cvtsi2ssq(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtsi2ssq(destination, placeHolder);
        new rip_cvtsi2ssq_9702(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2sd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtss2sd  xmm0, [L1: +305419896]}
     */
    // Template#: 32, Serial#: 10413
    public void rip_cvtss2sd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtss2sd(destination, placeHolder);
        new rip_cvtss2sd_10413(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2si  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtss2si  eax, [L1: +305419896]}
     */
    // Template#: 33, Serial#: 9729
    public void rip_cvtss2si(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtss2si(destination, placeHolder);
        new rip_cvtss2si_9729(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2si  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtss2si  rax, [L1: +305419896]}
     */
    // Template#: 34, Serial#: 9738
    public void rip_cvtss2si(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtss2si(destination, placeHolder);
        new rip_cvtss2si_9738(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttsd2si  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvttsd2si  eax, [L1: +305419896]}
     */
    // Template#: 35, Serial#: 9657
    public void rip_cvttsd2si(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvttsd2si(destination, placeHolder);
        new rip_cvttsd2si_9657(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttsd2si  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvttsd2si  rax, [L1: +305419896]}
     */
    // Template#: 36, Serial#: 9666
    public void rip_cvttsd2si(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvttsd2si(destination, placeHolder);
        new rip_cvttsd2si_9666(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttss2si  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvttss2si  eax, [L1: +305419896]}
     */
    // Template#: 37, Serial#: 9711
    public void rip_cvttss2si(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvttss2si(destination, placeHolder);
        new rip_cvttss2si_9711(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttss2si  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvttss2si  rax, [L1: +305419896]}
     */
    // Template#: 38, Serial#: 9720
    public void rip_cvttss2si(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvttss2si(destination, placeHolder);
        new rip_cvttss2si_9720(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code divq  }<i>label</i>
     * Example disassembly syntax: {@code divq      [L1: +305419896]}
     */
    // Template#: 39, Serial#: 2996
    public void rip_divq(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_divq(placeHolder);
        new rip_divq_2996(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code divsd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code divsd     xmm0, [L1: +305419896]}
     */
    // Template#: 40, Serial#: 10341
    public void rip_divsd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_divsd(destination, placeHolder);
        new rip_divsd_10341(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code divss  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code divss     xmm0, [L1: +305419896]}
     */
    // Template#: 41, Serial#: 10485
    public void rip_divss(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_divss(destination, placeHolder);
        new rip_divss_10485(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code idivl  }<i>label</i>
     * Example disassembly syntax: {@code idivl     [L1: +305419896]}
     */
    // Template#: 42, Serial#: 2928
    public void rip_idivl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_idivl(placeHolder);
        new rip_idivl_2928(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code idivq  }<i>label</i>
     * Example disassembly syntax: {@code idivq     [L1: +305419896]}
     */
    // Template#: 43, Serial#: 3000
    public void rip_idivq(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_idivq(placeHolder);
        new rip_idivq_3000(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code imul      eax, [L1: +305419896]}
     */
    // Template#: 44, Serial#: 11466
    public void rip_imul(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_imul(destination, placeHolder);
        new rip_imul_11466(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code imul      rax, [L1: +305419896]}
     */
    // Template#: 45, Serial#: 11475
    public void rip_imul(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_imul(destination, placeHolder);
        new rip_imul_11475(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jb  }<i>label</i>
     * Example disassembly syntax: {@code jb        L1: +18}
     */
    // Template#: 46, Serial#: 491
    public void jb(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jb(placeHolder);
        new jb_491(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jbe  }<i>label</i>
     * Example disassembly syntax: {@code jbe       L1: +18}
     */
    // Template#: 47, Serial#: 495
    public void jbe(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jbe(placeHolder);
        new jbe_495(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jl  }<i>label</i>
     * Example disassembly syntax: {@code jl        L1: +18}
     */
    // Template#: 48, Serial#: 3649
    public void jl(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jl(placeHolder);
        new jl_3649(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jle  }<i>label</i>
     * Example disassembly syntax: {@code jle       L1: +18}
     */
    // Template#: 49, Serial#: 3651
    public void jle(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jle(placeHolder);
        new jle_3651(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jmp  }<i>label</i>
     * Example disassembly syntax: {@code jmp       L1: +18}
     */
    // Template#: 50, Serial#: 5290
    public void jmp(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jmp(placeHolder);
        new jmp_5290(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jmp  }<i>label</i>
     * Example disassembly syntax: {@code jmp       [L1: +305419896]}
     */
    // Template#: 51, Serial#: 5436
    public void rip_jmp(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_jmp(placeHolder);
        new rip_jmp_5436(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnb  }<i>label</i>
     * Example disassembly syntax: {@code jnb       L1: +18}
     */
    // Template#: 52, Serial#: 492
    public void jnb(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jnb(placeHolder);
        new jnb_492(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnbe  }<i>label</i>
     * Example disassembly syntax: {@code jnbe      L1: +18}
     */
    // Template#: 53, Serial#: 496
    public void jnbe(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jnbe(placeHolder);
        new jnbe_496(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnl  }<i>label</i>
     * Example disassembly syntax: {@code jnl       L1: +18}
     */
    // Template#: 54, Serial#: 3650
    public void jnl(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jnl(placeHolder);
        new jnl_3650(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnle  }<i>label</i>
     * Example disassembly syntax: {@code jnle      L1: +18}
     */
    // Template#: 55, Serial#: 3652
    public void jnle(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jnle(placeHolder);
        new jnle_3652(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnz  }<i>label</i>
     * Example disassembly syntax: {@code jnz       L1: +18}
     */
    // Template#: 56, Serial#: 494
    public void jnz(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jnz(placeHolder);
        new jnz_494(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jz  }<i>label</i>
     * Example disassembly syntax: {@code jz        L1: +18}
     */
    // Template#: 57, Serial#: 493
    public void jz(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jz(placeHolder);
        new jz_493(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code lea  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code lea       rax, [L1: +305419896]}
     */
    // Template#: 58, Serial#: 3799
    public void rip_lea(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_lea(destination, placeHolder);
        new rip_lea_3799(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code mov       rax, [L1: +305419896]}
     */
    // Template#: 59, Serial#: 3746
    public void rip_mov(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_mov(destination, placeHolder);
        new rip_mov_3746(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movsd     xmm0, [L1: +305419896]}
     */
    // Template#: 60, Serial#: 6182
    public void rip_movsd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movsd(destination, placeHolder);
        new rip_movsd_6182(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movss     xmm0, [L1: +305419896]}
     */
    // Template#: 61, Serial#: 6254
    public void rip_movss(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movss(destination, placeHolder);
        new rip_movss_6254(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsx  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movsx     eax, [L1: +305419896]}
     */
    // Template#: 62, Serial#: 11682
    public void rip_movsxb(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movsxb(destination, placeHolder);
        new rip_movsxb_11682(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movsxd    rax, [L1: +305419896]}
     */
    // Template#: 63, Serial#: 462
    public void rip_movsxd(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movsxd(destination, placeHolder);
        new rip_movsxd_462(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movsxw    eax, [L1: +305419896]}
     */
    // Template#: 64, Serial#: 11709
    public void rip_movsxw(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movsxw(destination, placeHolder);
        new rip_movsxw_11709(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movzxd    rax, [L1: +305419896]}
     */
    // Template#: 65, Serial#: 471
    public void rip_movzxd(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movzxd(destination, placeHolder);
        new rip_movzxd_471(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movzxw    eax, [L1: +305419896]}
     */
    // Template#: 66, Serial#: 7931
    public void rip_movzxw(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movzxw(destination, placeHolder);
        new rip_movzxw_7931(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mulsd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code mulsd     xmm0, [L1: +305419896]}
     */
    // Template#: 67, Serial#: 10269
    public void rip_mulsd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_mulsd(destination, placeHolder);
        new rip_mulsd_10269(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mulss  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code mulss     xmm0, [L1: +305419896]}
     */
    // Template#: 68, Serial#: 10395
    public void rip_mulss(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_mulss(destination, placeHolder);
        new rip_mulss_10395(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code or        eax, [L1: +305419896]}
     */
    // Template#: 69, Serial#: 3197
    public void rip_or(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_or(destination, placeHolder);
        new rip_or_3197(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code or        rax, [L1: +305419896]}
     */
    // Template#: 70, Serial#: 3206
    public void rip_or(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_or(destination, placeHolder);
        new rip_or_3206(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code push  }<i>label</i>
     * Example disassembly syntax: {@code push      [L1: +305419896]}
     */
    // Template#: 71, Serial#: 5440
    public void rip_push(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_push(placeHolder);
        new rip_push_5440(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code sub       eax, [L1: +305419896]}
     */
    // Template#: 72, Serial#: 3425
    public void rip_sub(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sub(destination, placeHolder);
        new rip_sub_3425(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code sub       rax, [L1: +305419896]}
     */
    // Template#: 73, Serial#: 3434
    public void rip_sub(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sub(destination, placeHolder);
        new rip_sub_3434(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code subsd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code subsd     xmm0, [L1: +305419896]}
     */
    // Template#: 74, Serial#: 10305
    public void rip_subsd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_subsd(destination, placeHolder);
        new rip_subsd_10305(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code subss  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code subss     xmm0, [L1: +305419896]}
     */
    // Template#: 75, Serial#: 10449
    public void rip_subss(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_subss(destination, placeHolder);
        new rip_subss_10449(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code xor       eax, [L1: +305419896]}
     */
    // Template#: 76, Serial#: 427
    public void rip_xor(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xor(destination, placeHolder);
        new rip_xor_427(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code xor       rax, [L1: +305419896]}
     */
    // Template#: 77, Serial#: 436
    public void rip_xor(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xor(destination, placeHolder);
        new rip_xor_436(startPosition, currentPosition() - startPosition, destination, label);
    }

    class rip_add_85 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 destination;
        rip_add_85(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_add(destination, offsetAsInt());
        }
    }

    class rip_add_94 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 destination;
        rip_add_94(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_add(destination, offsetAsInt());
        }
    }

    class rip_addsd_10251 extends InstructionWithOffset {
        private final AMD64XMMRegister destination;
        rip_addsd_10251(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_addsd(destination, offsetAsInt());
        }
    }

    class rip_addss_10377 extends InstructionWithOffset {
        private final AMD64XMMRegister destination;
        rip_addss_10377(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_addss(destination, offsetAsInt());
        }
    }

    class rip_and_313 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 destination;
        rip_and_313(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_and(destination, offsetAsInt());
        }
    }

    class rip_and_322 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 destination;
        rip_and_322(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_and(destination, offsetAsInt());
        }
    }

    class call_5288 extends InstructionWithOffset {
        call_5288(int startPosition, int endPosition, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            call(offsetAsInt());
        }
    }

    class rip_call_5432 extends InstructionWithOffset {
        rip_call_5432(int startPosition, int endPosition, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_call(offsetAsInt());
        }
    }

    class rip_cmova_6557 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 destination;
        rip_cmova_6557(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmova(destination, offsetAsInt());
        }
    }

    class rip_cmovae_6449 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 destination;
        rip_cmovae_6449(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovae(destination, offsetAsInt());
        }
    }

    class rip_cmovb_6422 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 destination;
        rip_cmovb_6422(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovb(destination, offsetAsInt());
        }
    }

    class rip_cmovbe_6530 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 destination;
        rip_cmovbe_6530(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovbe(destination, offsetAsInt());
        }
    }

    class rip_cmove_6476 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 destination;
        rip_cmove_6476(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmove(destination, offsetAsInt());
        }
    }

    class rip_cmovg_9936 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 destination;
        rip_cmovg_9936(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovg(destination, offsetAsInt());
        }
    }

    class rip_cmovge_9882 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 destination;
        rip_cmovge_9882(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovge(destination, offsetAsInt());
        }
    }

    class rip_cmovl_9855 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 destination;
        rip_cmovl_9855(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovl(destination, offsetAsInt());
        }
    }

    class rip_cmovle_9909 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 destination;
        rip_cmovle_9909(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovle(destination, offsetAsInt());
        }
    }

    class rip_cmovp_9801 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 destination;
        rip_cmovp_9801(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovp(destination, offsetAsInt());
        }
    }

    class rip_cmp_3545 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 destination;
        rip_cmp_3545(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmp(destination, offsetAsInt());
        }
    }

    class rip_cmp_3554 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 destination;
        rip_cmp_3554(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmp(destination, offsetAsInt());
        }
    }

    class rip_cmpsd_8139 extends InstructionWithOffset {
        private final AMD64XMMRegister destination;
        private final AMD64XMMComparison amd64xmmcomparison;
        rip_cmpsd_8139(int startPosition, int endPosition, AMD64XMMRegister destination, AMD64XMMComparison amd64xmmcomparison, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
            this.amd64xmmcomparison = amd64xmmcomparison;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmpsd(destination, offsetAsInt(), amd64xmmcomparison);
        }
    }

    class rip_cmpss_8157 extends InstructionWithOffset {
        private final AMD64XMMRegister destination;
        private final AMD64XMMComparison amd64xmmcomparison;
        rip_cmpss_8157(int startPosition, int endPosition, AMD64XMMRegister destination, AMD64XMMComparison amd64xmmcomparison, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
            this.amd64xmmcomparison = amd64xmmcomparison;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmpss(destination, offsetAsInt(), amd64xmmcomparison);
        }
    }

    class rip_comisd_9621 extends InstructionWithOffset {
        private final AMD64XMMRegister destination;
        rip_comisd_9621(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_comisd(destination, offsetAsInt());
        }
    }

    class rip_comiss_9462 extends InstructionWithOffset {
        private final AMD64XMMRegister destination;
        rip_comiss_9462(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_comiss(destination, offsetAsInt());
        }
    }

    class rip_cvtsd2si_9675 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 destination;
        rip_cvtsd2si_9675(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtsd2si(destination, offsetAsInt());
        }
    }

    class rip_cvtsd2si_9684 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 destination;
        rip_cvtsd2si_9684(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtsd2si(destination, offsetAsInt());
        }
    }

    class rip_cvtsd2ss_10287 extends InstructionWithOffset {
        private final AMD64XMMRegister destination;
        rip_cvtsd2ss_10287(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtsd2ss(destination, offsetAsInt());
        }
    }

    class rip_cvtsi2sdl_9639 extends InstructionWithOffset {
        private final AMD64XMMRegister destination;
        rip_cvtsi2sdl_9639(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtsi2sdl(destination, offsetAsInt());
        }
    }

    class rip_cvtsi2sdq_9648 extends InstructionWithOffset {
        private final AMD64XMMRegister destination;
        rip_cvtsi2sdq_9648(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtsi2sdq(destination, offsetAsInt());
        }
    }

    class rip_cvtsi2ssl_9693 extends InstructionWithOffset {
        private final AMD64XMMRegister destination;
        rip_cvtsi2ssl_9693(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtsi2ssl(destination, offsetAsInt());
        }
    }

    class rip_cvtsi2ssq_9702 extends InstructionWithOffset {
        private final AMD64XMMRegister destination;
        rip_cvtsi2ssq_9702(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtsi2ssq(destination, offsetAsInt());
        }
    }

    class rip_cvtss2sd_10413 extends InstructionWithOffset {
        private final AMD64XMMRegister destination;
        rip_cvtss2sd_10413(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtss2sd(destination, offsetAsInt());
        }
    }

    class rip_cvtss2si_9729 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 destination;
        rip_cvtss2si_9729(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtss2si(destination, offsetAsInt());
        }
    }

    class rip_cvtss2si_9738 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 destination;
        rip_cvtss2si_9738(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtss2si(destination, offsetAsInt());
        }
    }

    class rip_cvttsd2si_9657 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 destination;
        rip_cvttsd2si_9657(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvttsd2si(destination, offsetAsInt());
        }
    }

    class rip_cvttsd2si_9666 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 destination;
        rip_cvttsd2si_9666(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvttsd2si(destination, offsetAsInt());
        }
    }

    class rip_cvttss2si_9711 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 destination;
        rip_cvttss2si_9711(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvttss2si(destination, offsetAsInt());
        }
    }

    class rip_cvttss2si_9720 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 destination;
        rip_cvttss2si_9720(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvttss2si(destination, offsetAsInt());
        }
    }

    class rip_divq_2996 extends InstructionWithOffset {
        rip_divq_2996(int startPosition, int endPosition, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_divq(offsetAsInt());
        }
    }

    class rip_divsd_10341 extends InstructionWithOffset {
        private final AMD64XMMRegister destination;
        rip_divsd_10341(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_divsd(destination, offsetAsInt());
        }
    }

    class rip_divss_10485 extends InstructionWithOffset {
        private final AMD64XMMRegister destination;
        rip_divss_10485(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_divss(destination, offsetAsInt());
        }
    }

    class rip_idivl_2928 extends InstructionWithOffset {
        rip_idivl_2928(int startPosition, int endPosition, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_idivl(offsetAsInt());
        }
    }

    class rip_idivq_3000 extends InstructionWithOffset {
        rip_idivq_3000(int startPosition, int endPosition, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_idivq(offsetAsInt());
        }
    }

    class rip_imul_11466 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 destination;
        rip_imul_11466(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_imul(destination, offsetAsInt());
        }
    }

    class rip_imul_11475 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 destination;
        rip_imul_11475(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_imul(destination, offsetAsInt());
        }
    }

    class jb_491 extends InstructionWithOffset {
        jb_491(int startPosition, int endPosition, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 1 | 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            final int labelSize = labelSize();
            if (labelSize == 1) {
                jb(offsetAsByte());
            } else if (labelSize == 4) {
                jb(offsetAsInt());
            } else {
                throw new AssemblyException("Unexpected label width: " + labelSize);
            }
        }
    }

    class jbe_495 extends InstructionWithOffset {
        jbe_495(int startPosition, int endPosition, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 1 | 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            final int labelSize = labelSize();
            if (labelSize == 1) {
                jbe(offsetAsByte());
            } else if (labelSize == 4) {
                jbe(offsetAsInt());
            } else {
                throw new AssemblyException("Unexpected label width: " + labelSize);
            }
        }
    }

    class jl_3649 extends InstructionWithOffset {
        jl_3649(int startPosition, int endPosition, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 1 | 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            final int labelSize = labelSize();
            if (labelSize == 1) {
                jl(offsetAsByte());
            } else if (labelSize == 4) {
                jl(offsetAsInt());
            } else {
                throw new AssemblyException("Unexpected label width: " + labelSize);
            }
        }
    }

    class jle_3651 extends InstructionWithOffset {
        jle_3651(int startPosition, int endPosition, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 1 | 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            final int labelSize = labelSize();
            if (labelSize == 1) {
                jle(offsetAsByte());
            } else if (labelSize == 4) {
                jle(offsetAsInt());
            } else {
                throw new AssemblyException("Unexpected label width: " + labelSize);
            }
        }
    }

    class jmp_5290 extends InstructionWithOffset {
        jmp_5290(int startPosition, int endPosition, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 1 | 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            final int labelSize = labelSize();
            if (labelSize == 1) {
                jmp(offsetAsByte());
            } else if (labelSize == 4) {
                jmp(offsetAsInt());
            } else {
                throw new AssemblyException("Unexpected label width: " + labelSize);
            }
        }
    }

    class rip_jmp_5436 extends InstructionWithOffset {
        rip_jmp_5436(int startPosition, int endPosition, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_jmp(offsetAsInt());
        }
    }

    class jnb_492 extends InstructionWithOffset {
        jnb_492(int startPosition, int endPosition, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 1 | 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            final int labelSize = labelSize();
            if (labelSize == 1) {
                jnb(offsetAsByte());
            } else if (labelSize == 4) {
                jnb(offsetAsInt());
            } else {
                throw new AssemblyException("Unexpected label width: " + labelSize);
            }
        }
    }

    class jnbe_496 extends InstructionWithOffset {
        jnbe_496(int startPosition, int endPosition, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 1 | 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            final int labelSize = labelSize();
            if (labelSize == 1) {
                jnbe(offsetAsByte());
            } else if (labelSize == 4) {
                jnbe(offsetAsInt());
            } else {
                throw new AssemblyException("Unexpected label width: " + labelSize);
            }
        }
    }

    class jnl_3650 extends InstructionWithOffset {
        jnl_3650(int startPosition, int endPosition, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 1 | 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            final int labelSize = labelSize();
            if (labelSize == 1) {
                jnl(offsetAsByte());
            } else if (labelSize == 4) {
                jnl(offsetAsInt());
            } else {
                throw new AssemblyException("Unexpected label width: " + labelSize);
            }
        }
    }

    class jnle_3652 extends InstructionWithOffset {
        jnle_3652(int startPosition, int endPosition, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 1 | 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            final int labelSize = labelSize();
            if (labelSize == 1) {
                jnle(offsetAsByte());
            } else if (labelSize == 4) {
                jnle(offsetAsInt());
            } else {
                throw new AssemblyException("Unexpected label width: " + labelSize);
            }
        }
    }

    class jnz_494 extends InstructionWithOffset {
        jnz_494(int startPosition, int endPosition, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 1 | 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            final int labelSize = labelSize();
            if (labelSize == 1) {
                jnz(offsetAsByte());
            } else if (labelSize == 4) {
                jnz(offsetAsInt());
            } else {
                throw new AssemblyException("Unexpected label width: " + labelSize);
            }
        }
    }

    class jz_493 extends InstructionWithOffset {
        jz_493(int startPosition, int endPosition, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 1 | 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            final int labelSize = labelSize();
            if (labelSize == 1) {
                jz(offsetAsByte());
            } else if (labelSize == 4) {
                jz(offsetAsInt());
            } else {
                throw new AssemblyException("Unexpected label width: " + labelSize);
            }
        }
    }

    class rip_lea_3799 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 destination;
        rip_lea_3799(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_lea(destination, offsetAsInt());
        }
    }

    class rip_mov_3746 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 destination;
        rip_mov_3746(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_mov(destination, offsetAsInt());
        }
    }

    class rip_movsd_6182 extends InstructionWithOffset {
        private final AMD64XMMRegister destination;
        rip_movsd_6182(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movsd(destination, offsetAsInt());
        }
    }

    class rip_movss_6254 extends InstructionWithOffset {
        private final AMD64XMMRegister destination;
        rip_movss_6254(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movss(destination, offsetAsInt());
        }
    }

    class rip_movsxb_11682 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 destination;
        rip_movsxb_11682(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movsxb(destination, offsetAsInt());
        }
    }

    class rip_movsxd_462 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 destination;
        rip_movsxd_462(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movsxd(destination, offsetAsInt());
        }
    }

    class rip_movsxw_11709 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 destination;
        rip_movsxw_11709(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movsxw(destination, offsetAsInt());
        }
    }

    class rip_movzxd_471 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 destination;
        rip_movzxd_471(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movzxd(destination, offsetAsInt());
        }
    }

    class rip_movzxw_7931 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 destination;
        rip_movzxw_7931(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movzxw(destination, offsetAsInt());
        }
    }

    class rip_mulsd_10269 extends InstructionWithOffset {
        private final AMD64XMMRegister destination;
        rip_mulsd_10269(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_mulsd(destination, offsetAsInt());
        }
    }

    class rip_mulss_10395 extends InstructionWithOffset {
        private final AMD64XMMRegister destination;
        rip_mulss_10395(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_mulss(destination, offsetAsInt());
        }
    }

    class rip_or_3197 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 destination;
        rip_or_3197(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_or(destination, offsetAsInt());
        }
    }

    class rip_or_3206 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 destination;
        rip_or_3206(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_or(destination, offsetAsInt());
        }
    }

    class rip_push_5440 extends InstructionWithOffset {
        rip_push_5440(int startPosition, int endPosition, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_push(offsetAsInt());
        }
    }

    class rip_sub_3425 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 destination;
        rip_sub_3425(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sub(destination, offsetAsInt());
        }
    }

    class rip_sub_3434 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 destination;
        rip_sub_3434(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sub(destination, offsetAsInt());
        }
    }

    class rip_subsd_10305 extends InstructionWithOffset {
        private final AMD64XMMRegister destination;
        rip_subsd_10305(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_subsd(destination, offsetAsInt());
        }
    }

    class rip_subss_10449 extends InstructionWithOffset {
        private final AMD64XMMRegister destination;
        rip_subss_10449(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_subss(destination, offsetAsInt());
        }
    }

    class rip_xor_427 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 destination;
        rip_xor_427(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xor(destination, offsetAsInt());
        }
    }

    class rip_xor_436 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 destination;
        rip_xor_436(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            this.destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xor(destination, offsetAsInt());
        }
    }

// END GENERATED LABEL ASSEMBLER METHODS
}
