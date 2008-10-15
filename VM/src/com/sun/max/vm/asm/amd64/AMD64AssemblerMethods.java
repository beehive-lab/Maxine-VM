/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
    // Template#: 1, Serial#: 641
    public void addl(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0001((byte) 0x83, (byte) 0x00, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code addq  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code addq      [rbx + 18], 0x12}
     */
    // Template#: 2, Serial#: 713
    public void addq(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0002((byte) 0x83, (byte) 0x00, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code add       [rbx + 18], eax}
     */
    // Template#: 3, Serial#: 14
    public void add(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0003((byte) 0x01, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code add       [rbx + 18], rax}
     */
    // Template#: 4, Serial#: 23
    public void add(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0004((byte) 0x01, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code addl  }<i>disp8</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code addl      [rbx + 18], 0x12345678}
     */
    // Template#: 5, Serial#: 425
    public void addl(byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        assemble0005((byte) 0x81, (byte) 0x00, disp8, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code addq  }<i>disp8</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code addq      [rbx + 18], 0x12345678}
     */
    // Template#: 6, Serial#: 497
    public void addq(byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        assemble0006((byte) 0x81, (byte) 0x00, disp8, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code addl  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code addl      eax, 0x12}
     */
    // Template#: 7, Serial#: 673
    public void addl(AMD64GeneralRegister32 destination, byte imm8) {
        assemble0007((byte) 0x83, (byte) 0x00, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code add       eax, [rbx + 18]}
     */
    // Template#: 8, Serial#: 49
    public void add(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0008((byte) 0x03, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code add       eax, eax}
     */
    // Template#: 9, Serial#: 18
    public void add(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0009((byte) 0x01, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code add       eax, [L1: +305419896]}
     */
    // Template#: 10, Serial#: 48
    public void rip_add(AMD64GeneralRegister32 destination, int rel32) {
        assemble0010((byte) 0x03, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code addl  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code addl      eax, 0x12345678}
     */
    // Template#: 11, Serial#: 457
    public void addl(AMD64GeneralRegister32 destination, int imm32) {
        assemble0011((byte) 0x81, (byte) 0x00, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code add       eax, [rbx + 305419896]}
     */
    // Template#: 12, Serial#: 51
    public void add(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0012((byte) 0x03, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code addq  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code addq      rax, 0x12}
     */
    // Template#: 13, Serial#: 745
    public void addq(AMD64GeneralRegister64 destination, byte imm8) {
        assemble0013((byte) 0x83, (byte) 0x00, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code add       rax, [rbx + 18]}
     */
    // Template#: 14, Serial#: 57
    public void add(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0014((byte) 0x03, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code add       rax, rax}
     */
    // Template#: 15, Serial#: 27
    public void add(AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0015((byte) 0x01, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code add       rax, [L1: +305419896]}
     */
    // Template#: 16, Serial#: 56
    public void rip_add(AMD64GeneralRegister64 destination, int rel32) {
        assemble0016((byte) 0x03, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code addq  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code addq      rax, 0x12345678}
     */
    // Template#: 17, Serial#: 529
    public void addq(AMD64GeneralRegister64 destination, int imm32) {
        assemble0017((byte) 0x81, (byte) 0x00, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code add       rax, [rbx + 305419896]}
     */
    // Template#: 18, Serial#: 59
    public void add(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0018((byte) 0x03, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>imm32</i>
     * Example disassembly syntax: {@code add       eax, 0x12345678}
     */
    // Template#: 19, Serial#: 70
    public void add_EAX(int imm32) {
        assemble0019((byte) 0x05, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>imm32</i>
     * Example disassembly syntax: {@code add       rax, 0x12345678}
     */
    // Template#: 20, Serial#: 71
    public void add_RAX(int imm32) {
        assemble0020((byte) 0x05, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code addl  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code addl      [rbx + 305419896], 0x12}
     */
    // Template#: 21, Serial#: 657
    public void addl(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0021((byte) 0x83, (byte) 0x00, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code addq  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code addq      [rbx + 305419896], 0x12}
     */
    // Template#: 22, Serial#: 729
    public void addq(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0022((byte) 0x83, (byte) 0x00, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code add       [rbx + 305419896], eax}
     */
    // Template#: 23, Serial#: 16
    public void add(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0023((byte) 0x01, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code add       [rbx + 305419896], rax}
     */
    // Template#: 24, Serial#: 25
    public void add(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0024((byte) 0x01, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code addl  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code addl      [rbx + 305419896], 0x12345678}
     */
    // Template#: 25, Serial#: 441
    public void addl(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0025((byte) 0x81, (byte) 0x00, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code addq  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code addq      [rbx + 305419896], 0x12345678}
     */
    // Template#: 26, Serial#: 513
    public void addq(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0026((byte) 0x81, (byte) 0x00, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code addsd  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code addsd     xmm0, [rbx + 18]}
     */
    // Template#: 27, Serial#: 5505
    public void addsd(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0027((byte) 0x58, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code addsd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code addsd     xmm0, xmm0}
     */
    // Template#: 28, Serial#: 5509
    public void addsd(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0028((byte) 0x58, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code addsd  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code addsd     xmm0, [L1: +305419896]}
     */
    // Template#: 29, Serial#: 5504
    public void rip_addsd(AMD64XMMRegister destination, int rel32) {
        assemble0029((byte) 0x58, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code addsd  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code addsd     xmm0, [rbx + 305419896]}
     */
    // Template#: 30, Serial#: 5507
    public void addsd(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0030((byte) 0x58, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code addss  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code addss     xmm0, [rbx + 18]}
     */
    // Template#: 31, Serial#: 5568
    public void addss(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0031((byte) 0x58, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code addss  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code addss     xmm0, xmm0}
     */
    // Template#: 32, Serial#: 5572
    public void addss(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0032((byte) 0x58, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code addss  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code addss     xmm0, [L1: +305419896]}
     */
    // Template#: 33, Serial#: 5567
    public void rip_addss(AMD64XMMRegister destination, int rel32) {
        assemble0033((byte) 0x58, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code addss  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code addss     xmm0, [rbx + 305419896]}
     */
    // Template#: 34, Serial#: 5570
    public void addss(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0034((byte) 0x58, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code andl  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code andl      [rbx + 18], 0x12}
     */
    // Template#: 35, Serial#: 649
    public void andl(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0001((byte) 0x83, (byte) 0x04, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code andq  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code andq      [rbx + 18], 0x12}
     */
    // Template#: 36, Serial#: 721
    public void andq(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0002((byte) 0x83, (byte) 0x04, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code and       [rbx + 18], eax}
     */
    // Template#: 37, Serial#: 158
    public void and(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0003((byte) 0x21, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code and       [rbx + 18], rax}
     */
    // Template#: 38, Serial#: 167
    public void and(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0004((byte) 0x21, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code andl  }<i>disp8</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code andl      [rbx + 18], 0x12345678}
     */
    // Template#: 39, Serial#: 433
    public void andl(byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        assemble0005((byte) 0x81, (byte) 0x04, disp8, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code andq  }<i>disp8</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code andq      [rbx + 18], 0x12345678}
     */
    // Template#: 40, Serial#: 505
    public void andq(byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        assemble0006((byte) 0x81, (byte) 0x04, disp8, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code andl  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code andl      eax, 0x12}
     */
    // Template#: 41, Serial#: 677
    public void andl(AMD64GeneralRegister32 destination, byte imm8) {
        assemble0007((byte) 0x83, (byte) 0x04, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code and       eax, [rbx + 18]}
     */
    // Template#: 42, Serial#: 193
    public void and(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0008((byte) 0x23, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code and       eax, eax}
     */
    // Template#: 43, Serial#: 162
    public void and(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0009((byte) 0x21, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code and       eax, [L1: +305419896]}
     */
    // Template#: 44, Serial#: 192
    public void rip_and(AMD64GeneralRegister32 destination, int rel32) {
        assemble0010((byte) 0x23, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code andl  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code andl      eax, 0x12345678}
     */
    // Template#: 45, Serial#: 461
    public void andl(AMD64GeneralRegister32 destination, int imm32) {
        assemble0011((byte) 0x81, (byte) 0x04, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code and       eax, [rbx + 305419896]}
     */
    // Template#: 46, Serial#: 195
    public void and(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0012((byte) 0x23, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code andq  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code andq      rax, 0x12}
     */
    // Template#: 47, Serial#: 749
    public void andq(AMD64GeneralRegister64 destination, byte imm8) {
        assemble0013((byte) 0x83, (byte) 0x04, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code and       rax, [rbx + 18]}
     */
    // Template#: 48, Serial#: 201
    public void and(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0014((byte) 0x23, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code and       rax, rax}
     */
    // Template#: 49, Serial#: 171
    public void and(AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0015((byte) 0x21, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code and       rax, [L1: +305419896]}
     */
    // Template#: 50, Serial#: 200
    public void rip_and(AMD64GeneralRegister64 destination, int rel32) {
        assemble0016((byte) 0x23, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code andq  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code andq      rax, 0x12345678}
     */
    // Template#: 51, Serial#: 533
    public void andq(AMD64GeneralRegister64 destination, int imm32) {
        assemble0017((byte) 0x81, (byte) 0x04, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code and       rax, [rbx + 305419896]}
     */
    // Template#: 52, Serial#: 203
    public void and(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0018((byte) 0x23, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>imm32</i>
     * Example disassembly syntax: {@code and       eax, 0x12345678}
     */
    // Template#: 53, Serial#: 214
    public void and_EAX(int imm32) {
        assemble0019((byte) 0x25, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>imm32</i>
     * Example disassembly syntax: {@code and       rax, 0x12345678}
     */
    // Template#: 54, Serial#: 215
    public void and_RAX(int imm32) {
        assemble0020((byte) 0x25, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code andl  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code andl      [rbx + 305419896], 0x12}
     */
    // Template#: 55, Serial#: 665
    public void andl(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0021((byte) 0x83, (byte) 0x04, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code andq  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code andq      [rbx + 305419896], 0x12}
     */
    // Template#: 56, Serial#: 737
    public void andq(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0022((byte) 0x83, (byte) 0x04, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code and       [rbx + 305419896], eax}
     */
    // Template#: 57, Serial#: 160
    public void and(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0023((byte) 0x21, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code and       [rbx + 305419896], rax}
     */
    // Template#: 58, Serial#: 169
    public void and(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0024((byte) 0x21, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code andl  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code andl      [rbx + 305419896], 0x12345678}
     */
    // Template#: 59, Serial#: 449
    public void andl(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0025((byte) 0x81, (byte) 0x04, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code andq  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code andq      [rbx + 305419896], 0x12345678}
     */
    // Template#: 60, Serial#: 521
    public void andq(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0026((byte) 0x81, (byte) 0x04, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code call  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code call      [rbx + 18]}
     */
    // Template#: 61, Serial#: 3058
    public void call(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0035((byte) 0xFF, (byte) 0x02, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code call  }<i>destination</i>
     * Example disassembly syntax: {@code call      rax}
     */
    // Template#: 62, Serial#: 3070
    public void call(AMD64GeneralRegister64 destination) {
        assemble0036((byte) 0xFF, (byte) 0x02, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code call  }<i>rel32</i>
     * Example disassembly syntax: {@code call      L1: +305419896}
     */
    // Template#: 63, Serial#: 2957
    public void call(int rel32) {
        assemble0037((byte) 0xE8, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code call  }<i>rel32</i>
     * Example disassembly syntax: {@code call      [L1: +305419896]}
     */
    // Template#: 64, Serial#: 3049
    public void rip_call(int rel32) {
        assemble0038((byte) 0xFF, (byte) 0x02, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code call  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code call      [rbx + 305419896]}
     */
    // Template#: 65, Serial#: 3064
    public void call(int disp32, AMD64IndirectRegister64 destination) {
        assemble0039((byte) 0xFF, (byte) 0x02, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code cdq  }
     * Example disassembly syntax: {@code cdq     }
     */
    // Template#: 66, Serial#: 2466
    public void cdq() {
        assemble0040((byte) 0x99);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmova  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cmova     eax, [rbx + 18]}
     */
    // Template#: 67, Serial#: 3632
    public void cmova(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0041((byte) 0x47, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmova  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmova     eax, eax}
     */
    // Template#: 68, Serial#: 3636
    public void cmova(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0042((byte) 0x47, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmova  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cmova     eax, [L1: +305419896]}
     */
    // Template#: 69, Serial#: 3631
    public void rip_cmova(AMD64GeneralRegister32 destination, int rel32) {
        assemble0043((byte) 0x47, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmova  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cmova     eax, [rbx + 305419896]}
     */
    // Template#: 70, Serial#: 3634
    public void cmova(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0044((byte) 0x47, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovb  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovb     eax, [rbx + 18]}
     */
    // Template#: 71, Serial#: 3497
    public void cmovb(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0041((byte) 0x42, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovb  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovb     eax, eax}
     */
    // Template#: 72, Serial#: 3501
    public void cmovb(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0042((byte) 0x42, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovb  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cmovb     eax, [L1: +305419896]}
     */
    // Template#: 73, Serial#: 3496
    public void rip_cmovb(AMD64GeneralRegister32 destination, int rel32) {
        assemble0043((byte) 0x42, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovb  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovb     eax, [rbx + 305419896]}
     */
    // Template#: 74, Serial#: 3499
    public void cmovb(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0044((byte) 0x42, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovbe  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovbe    eax, [rbx + 18]}
     */
    // Template#: 75, Serial#: 3605
    public void cmovbe(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0041((byte) 0x46, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovbe  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovbe    eax, eax}
     */
    // Template#: 76, Serial#: 3609
    public void cmovbe(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0042((byte) 0x46, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovbe  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cmovbe    eax, [L1: +305419896]}
     */
    // Template#: 77, Serial#: 3604
    public void rip_cmovbe(AMD64GeneralRegister32 destination, int rel32) {
        assemble0043((byte) 0x46, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovbe  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovbe    eax, [rbx + 305419896]}
     */
    // Template#: 78, Serial#: 3607
    public void cmovbe(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0044((byte) 0x46, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmove  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cmove     eax, [rbx + 18]}
     */
    // Template#: 79, Serial#: 3551
    public void cmove(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0041((byte) 0x44, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmove  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmove     eax, eax}
     */
    // Template#: 80, Serial#: 3555
    public void cmove(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0042((byte) 0x44, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmove  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cmove     eax, [L1: +305419896]}
     */
    // Template#: 81, Serial#: 3550
    public void rip_cmove(AMD64GeneralRegister32 destination, int rel32) {
        assemble0043((byte) 0x44, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmove  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cmove     eax, [rbx + 305419896]}
     */
    // Template#: 82, Serial#: 3553
    public void cmove(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0044((byte) 0x44, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmove  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmove     rax, [rbx]}
     */
    // Template#: 83, Serial#: 3556
    public void cmove(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0046((byte) 0x44, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovg  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovg     eax, [rbx + 18]}
     */
    // Template#: 84, Serial#: 5334
    public void cmovg(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0041((byte) 0x4F, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovg  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovg     eax, eax}
     */
    // Template#: 85, Serial#: 5338
    public void cmovg(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0042((byte) 0x4F, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovg  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cmovg     eax, [L1: +305419896]}
     */
    // Template#: 86, Serial#: 5333
    public void rip_cmovg(AMD64GeneralRegister32 destination, int rel32) {
        assemble0043((byte) 0x4F, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovg  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovg     eax, [rbx + 305419896]}
     */
    // Template#: 87, Serial#: 5336
    public void cmovg(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0044((byte) 0x4F, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovg  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovg     rax, [rbx]}
     */
    // Template#: 88, Serial#: 5339
    public void cmovg(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0046((byte) 0x4F, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovge  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovge    eax, [rbx + 18]}
     */
    // Template#: 89, Serial#: 5280
    public void cmovge(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0041((byte) 0x4D, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovge  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovge    eax, eax}
     */
    // Template#: 90, Serial#: 5284
    public void cmovge(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0042((byte) 0x4D, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovge  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cmovge    eax, [L1: +305419896]}
     */
    // Template#: 91, Serial#: 5279
    public void rip_cmovge(AMD64GeneralRegister32 destination, int rel32) {
        assemble0043((byte) 0x4D, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovge  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovge    eax, [rbx + 305419896]}
     */
    // Template#: 92, Serial#: 5282
    public void cmovge(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0044((byte) 0x4D, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovge  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovge    rax, [rbx]}
     */
    // Template#: 93, Serial#: 5285
    public void cmovge(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0046((byte) 0x4D, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovl  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovl     eax, [rbx + 18]}
     */
    // Template#: 94, Serial#: 5253
    public void cmovl(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0041((byte) 0x4C, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovl  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovl     eax, eax}
     */
    // Template#: 95, Serial#: 5257
    public void cmovl(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0042((byte) 0x4C, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovl  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cmovl     eax, [L1: +305419896]}
     */
    // Template#: 96, Serial#: 5252
    public void rip_cmovl(AMD64GeneralRegister32 destination, int rel32) {
        assemble0043((byte) 0x4C, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovl  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovl     eax, [rbx + 305419896]}
     */
    // Template#: 97, Serial#: 5255
    public void cmovl(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0044((byte) 0x4C, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovl  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovl     rax, [rbx]}
     */
    // Template#: 98, Serial#: 5258
    public void cmovl(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0046((byte) 0x4C, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovle  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovle    eax, eax}
     */
    // Template#: 99, Serial#: 5311
    public void cmovle(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0042((byte) 0x4E, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovle  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cmovle    eax, [L1: +305419896]}
     */
    // Template#: 100, Serial#: 5306
    public void rip_cmovle(AMD64GeneralRegister32 destination, int rel32) {
        assemble0043((byte) 0x4E, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovle  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovle    rax, [rbx]}
     */
    // Template#: 101, Serial#: 5312
    public void cmovle(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0046((byte) 0x4E, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovne  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovne    rax, [rbx]}
     */
    // Template#: 102, Serial#: 3583
    public void cmovne(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0046((byte) 0x45, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovp  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovp     eax, [rbx + 18]}
     */
    // Template#: 103, Serial#: 5199
    public void cmovp(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0041((byte) 0x4A, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovp  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovp     eax, eax}
     */
    // Template#: 104, Serial#: 5203
    public void cmovp(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0042((byte) 0x4A, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovp  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cmovp     eax, [L1: +305419896]}
     */
    // Template#: 105, Serial#: 5198
    public void rip_cmovp(AMD64GeneralRegister32 destination, int rel32) {
        assemble0043((byte) 0x4A, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovp  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cmovp     eax, [rbx + 305419896]}
     */
    // Template#: 106, Serial#: 5201
    public void cmovp(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0044((byte) 0x4A, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpl  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code cmpl      [rbx + 18], 0x12}
     */
    // Template#: 107, Serial#: 655
    public void cmpl(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0001((byte) 0x83, (byte) 0x07, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpq  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code cmpq      [rbx + 18], 0x12}
     */
    // Template#: 108, Serial#: 727
    public void cmpq(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0002((byte) 0x83, (byte) 0x07, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmp       [rbx + 18], eax}
     */
    // Template#: 109, Serial#: 2214
    public void cmp(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0003((byte) 0x39, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmp       [rbx + 18], rax}
     */
    // Template#: 110, Serial#: 2223
    public void cmp(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0004((byte) 0x39, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpl  }<i>disp8</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code cmpl      [rbx + 18], 0x12345678}
     */
    // Template#: 111, Serial#: 439
    public void cmpl(byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        assemble0005((byte) 0x81, (byte) 0x07, disp8, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpq  }<i>disp8</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code cmpq      [rbx + 18], 0x12345678}
     */
    // Template#: 112, Serial#: 511
    public void cmpq(byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        assemble0006((byte) 0x81, (byte) 0x07, disp8, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpl  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code cmpl      eax, 0x12}
     */
    // Template#: 113, Serial#: 680
    public void cmpl(AMD64GeneralRegister32 destination, byte imm8) {
        assemble0007((byte) 0x83, (byte) 0x07, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cmp       eax, [rbx + 18]}
     */
    // Template#: 114, Serial#: 2249
    public void cmp(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0008((byte) 0x3B, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>destination</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code cmp       eax, rbx[rsi * 4]}
     */
    // Template#: 115, Serial#: 2246
    public void cmp(AMD64GeneralRegister32 destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0048((byte) 0x3B, destination, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmp       eax, eax}
     */
    // Template#: 116, Serial#: 2218
    public void cmp(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0009((byte) 0x39, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpl  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code cmpl      eax, 0x12345678}
     */
    // Template#: 117, Serial#: 464
    public void cmpl(AMD64GeneralRegister32 destination, int imm32) {
        assemble0011((byte) 0x81, (byte) 0x07, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cmp       eax, [L1: +305419896]}
     */
    // Template#: 118, Serial#: 2248
    public void rip_cmp(AMD64GeneralRegister32 destination, int rel32) {
        assemble0010((byte) 0x3B, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cmp       eax, [rbx + 305419896]}
     */
    // Template#: 119, Serial#: 2251
    public void cmp(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0012((byte) 0x3B, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpq  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code cmpq      rax, 0x12}
     */
    // Template#: 120, Serial#: 752
    public void cmpq(AMD64GeneralRegister64 destination, byte imm8) {
        assemble0013((byte) 0x83, (byte) 0x07, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cmp       rax, [rbx + 18]}
     */
    // Template#: 121, Serial#: 2257
    public void cmp(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0014((byte) 0x3B, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmp       rax, rax}
     */
    // Template#: 122, Serial#: 2227
    public void cmp(AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0015((byte) 0x39, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpq  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code cmpq      rax, 0x12345678}
     */
    // Template#: 123, Serial#: 536
    public void cmpq(AMD64GeneralRegister64 destination, int imm32) {
        assemble0017((byte) 0x81, (byte) 0x07, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cmp       rax, [L1: +305419896]}
     */
    // Template#: 124, Serial#: 2256
    public void rip_cmp(AMD64GeneralRegister64 destination, int rel32) {
        assemble0016((byte) 0x3B, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cmp       rax, [rbx + 305419896]}
     */
    // Template#: 125, Serial#: 2259
    public void cmp(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0018((byte) 0x3B, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>imm32</i>
     * Example disassembly syntax: {@code cmp       eax, 0x12345678}
     */
    // Template#: 126, Serial#: 2270
    public void cmp_EAX(int imm32) {
        assemble0019((byte) 0x3D, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>imm32</i>
     * Example disassembly syntax: {@code cmp       rax, 0x12345678}
     */
    // Template#: 127, Serial#: 2271
    public void cmp_RAX(int imm32) {
        assemble0020((byte) 0x3D, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpl  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code cmpl      [rbx + 305419896], 0x12}
     */
    // Template#: 128, Serial#: 671
    public void cmpl(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0021((byte) 0x83, (byte) 0x07, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpq  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code cmpq      [rbx + 305419896], 0x12}
     */
    // Template#: 129, Serial#: 743
    public void cmpq(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0022((byte) 0x83, (byte) 0x07, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmp       [rbx + 305419896], eax}
     */
    // Template#: 130, Serial#: 2216
    public void cmp(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0023((byte) 0x39, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmp       [rbx + 305419896], rax}
     */
    // Template#: 131, Serial#: 2225
    public void cmp(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0024((byte) 0x39, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpl  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code cmpl      [rbx + 305419896], 0x12345678}
     */
    // Template#: 132, Serial#: 455
    public void cmpl(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0025((byte) 0x81, (byte) 0x07, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpq  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code cmpq      [rbx + 305419896], 0x12345678}
     */
    // Template#: 133, Serial#: 527
    public void cmpq(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0026((byte) 0x81, (byte) 0x07, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpsd  }<i>destination</i>, <i>disp8</i>, <i>source</i>, <i>amd64xmmcomparison</i>
     * Example disassembly syntax: {@code cmpsd     xmm0, [rbx + 18], less_than_or_equal}
     */
    // Template#: 134, Serial#: 4446
    public void cmpsd(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source, AMD64XMMComparison amd64xmmcomparison) {
        assemble0049((byte) 0xC2, destination, disp8, source, amd64xmmcomparison);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpsd  }<i>destination</i>, <i>source</i>, <i>amd64xmmcomparison</i>
     * Example disassembly syntax: {@code cmpsd     xmm0, xmm0, less_than_or_equal}
     */
    // Template#: 135, Serial#: 4450
    public void cmpsd(AMD64XMMRegister destination, AMD64XMMRegister source, AMD64XMMComparison amd64xmmcomparison) {
        assemble0050((byte) 0xC2, destination, source, amd64xmmcomparison);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpsd  }<i>destination</i>, <i>disp32</i>, <i>source</i>, <i>amd64xmmcomparison</i>
     * Example disassembly syntax: {@code cmpsd     xmm0, [rbx + 305419896], less_than_or_equal}
     */
    // Template#: 136, Serial#: 4448
    public void cmpsd(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source, AMD64XMMComparison amd64xmmcomparison) {
        assemble0051((byte) 0xC2, destination, disp32, source, amd64xmmcomparison);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpsd  }<i>destination</i>, <i>rel32</i>, <i>amd64xmmcomparison</i>
     * Example disassembly syntax: {@code cmpsd     xmm0, [L1: +305419896], less_than_or_equal}
     */
    // Template#: 137, Serial#: 4445
    public void rip_cmpsd(AMD64XMMRegister destination, int rel32, AMD64XMMComparison amd64xmmcomparison) {
        assemble0052((byte) 0xC2, destination, rel32, amd64xmmcomparison);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpss  }<i>destination</i>, <i>disp8</i>, <i>source</i>, <i>amd64xmmcomparison</i>
     * Example disassembly syntax: {@code cmpss     xmm0, [rbx + 18], less_than_or_equal}
     */
    // Template#: 138, Serial#: 4455
    public void cmpss(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source, AMD64XMMComparison amd64xmmcomparison) {
        assemble0053((byte) 0xC2, destination, disp8, source, amd64xmmcomparison);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpss  }<i>destination</i>, <i>source</i>, <i>amd64xmmcomparison</i>
     * Example disassembly syntax: {@code cmpss     xmm0, xmm0, less_than_or_equal}
     */
    // Template#: 139, Serial#: 4459
    public void cmpss(AMD64XMMRegister destination, AMD64XMMRegister source, AMD64XMMComparison amd64xmmcomparison) {
        assemble0054((byte) 0xC2, destination, source, amd64xmmcomparison);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpss  }<i>destination</i>, <i>disp32</i>, <i>source</i>, <i>amd64xmmcomparison</i>
     * Example disassembly syntax: {@code cmpss     xmm0, [rbx + 305419896], less_than_or_equal}
     */
    // Template#: 140, Serial#: 4457
    public void cmpss(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source, AMD64XMMComparison amd64xmmcomparison) {
        assemble0055((byte) 0xC2, destination, disp32, source, amd64xmmcomparison);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpss  }<i>destination</i>, <i>rel32</i>, <i>amd64xmmcomparison</i>
     * Example disassembly syntax: {@code cmpss     xmm0, [L1: +305419896], less_than_or_equal}
     */
    // Template#: 141, Serial#: 4454
    public void rip_cmpss(AMD64XMMRegister destination, int rel32, AMD64XMMComparison amd64xmmcomparison) {
        assemble0056((byte) 0xC2, destination, rel32, amd64xmmcomparison);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpxchg  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmpxchg   [rbx + 18], eax}
     */
    // Template#: 142, Serial#: 4231
    public void cmpxchg(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0057((byte) 0xB1, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpxchg  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmpxchg   [rbx + 18], rax}
     */
    // Template#: 143, Serial#: 4240
    public void cmpxchg(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0058((byte) 0xB1, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpxchg  }<i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code cmpxchg   rbx[rsi * 4], eax}
     */
    // Template#: 144, Serial#: 4228
    public void cmpxchg(AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister32 source) {
        assemble0060((byte) 0xB1, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpxchg  }<i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code cmpxchg   rbx[rsi * 4], rax}
     */
    // Template#: 145, Serial#: 4237
    public void cmpxchg(AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister64 source) {
        assemble0062((byte) 0xB1, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpxchg  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmpxchg   [rbx], eax}
     */
    // Template#: 146, Serial#: 4227
    public void cmpxchg(AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0063((byte) 0xB1, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpxchg  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmpxchg   [rbx], rax}
     */
    // Template#: 147, Serial#: 4236
    public void cmpxchg(AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0064((byte) 0xB1, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpxchg  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmpxchg   [rbx + 305419896], eax}
     */
    // Template#: 148, Serial#: 4233
    public void cmpxchg(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0065((byte) 0xB1, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpxchg  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cmpxchg   [rbx + 305419896], rax}
     */
    // Template#: 149, Serial#: 4242
    public void cmpxchg(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0066((byte) 0xB1, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code comisd  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code comisd    xmm0, [rbx + 18]}
     */
    // Template#: 150, Serial#: 5028
    public void comisd(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0067((byte) 0x2F, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code comisd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code comisd    xmm0, xmm0}
     */
    // Template#: 151, Serial#: 5032
    public void comisd(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0068((byte) 0x2F, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code comisd  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code comisd    xmm0, [L1: +305419896]}
     */
    // Template#: 152, Serial#: 5027
    public void rip_comisd(AMD64XMMRegister destination, int rel32) {
        assemble0069((byte) 0x2F, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code comisd  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code comisd    xmm0, [rbx + 305419896]}
     */
    // Template#: 153, Serial#: 5030
    public void comisd(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0070((byte) 0x2F, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code comiss  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code comiss    xmm0, [rbx + 18]}
     */
    // Template#: 154, Serial#: 4958
    public void comiss(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0071((byte) 0x2F, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code comiss  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code comiss    xmm0, xmm0}
     */
    // Template#: 155, Serial#: 4962
    public void comiss(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0072((byte) 0x2F, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code comiss  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code comiss    xmm0, [L1: +305419896]}
     */
    // Template#: 156, Serial#: 4957
    public void rip_comiss(AMD64XMMRegister destination, int rel32) {
        assemble0073((byte) 0x2F, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code comiss  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code comiss    xmm0, [rbx + 305419896]}
     */
    // Template#: 157, Serial#: 4960
    public void comiss(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0074((byte) 0x2F, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cqo  }
     * Example disassembly syntax: {@code cqo     }
     */
    // Template#: 158, Serial#: 2467
    public void cqo() {
        assemble0075((byte) 0x99);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2si  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsd2si  eax, [rbx + 18]}
     */
    // Template#: 159, Serial#: 5073
    public void cvtsd2si(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0076((byte) 0x2D, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2si  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsd2si  eax, xmm0}
     */
    // Template#: 160, Serial#: 5077
    public void cvtsd2si(AMD64GeneralRegister32 destination, AMD64XMMRegister source) {
        assemble0077((byte) 0x2D, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2si  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvtsd2si  eax, [L1: +305419896]}
     */
    // Template#: 161, Serial#: 5072
    public void rip_cvtsd2si(AMD64GeneralRegister32 destination, int rel32) {
        assemble0078((byte) 0x2D, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2si  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsd2si  eax, [rbx + 305419896]}
     */
    // Template#: 162, Serial#: 5075
    public void cvtsd2si(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0079((byte) 0x2D, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2si  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsd2si  rax, [rbx + 18]}
     */
    // Template#: 163, Serial#: 5082
    public void cvtsd2si(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0080((byte) 0x2D, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2si  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsd2si  rax, xmm0}
     */
    // Template#: 164, Serial#: 5086
    public void cvtsd2si(AMD64GeneralRegister64 destination, AMD64XMMRegister source) {
        assemble0081((byte) 0x2D, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2si  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvtsd2si  rax, [L1: +305419896]}
     */
    // Template#: 165, Serial#: 5081
    public void rip_cvtsd2si(AMD64GeneralRegister64 destination, int rel32) {
        assemble0082((byte) 0x2D, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2si  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsd2si  rax, [rbx + 305419896]}
     */
    // Template#: 166, Serial#: 5084
    public void cvtsd2si(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0083((byte) 0x2D, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2ss  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsd2ss  xmm0, [rbx + 18]}
     */
    // Template#: 167, Serial#: 5523
    public void cvtsd2ss(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0027((byte) 0x5A, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2ss  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsd2ss  xmm0, xmm0}
     */
    // Template#: 168, Serial#: 5527
    public void cvtsd2ss(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0028((byte) 0x5A, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2ss  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvtsd2ss  xmm0, [L1: +305419896]}
     */
    // Template#: 169, Serial#: 5522
    public void rip_cvtsd2ss(AMD64XMMRegister destination, int rel32) {
        assemble0029((byte) 0x5A, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2ss  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsd2ss  xmm0, [rbx + 305419896]}
     */
    // Template#: 170, Serial#: 5525
    public void cvtsd2ss(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0030((byte) 0x5A, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2sdl  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsi2sdl  xmm0, [rbx + 18]}
     */
    // Template#: 171, Serial#: 5037
    public void cvtsi2sdl(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0027((byte) 0x2A, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2sdq  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsi2sdq  xmm0, [rbx + 18]}
     */
    // Template#: 172, Serial#: 5046
    public void cvtsi2sdq(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0084((byte) 0x2A, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2sdl  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsi2sdl  xmm0, eax}
     */
    // Template#: 173, Serial#: 5041
    public void cvtsi2sdl(AMD64XMMRegister destination, AMD64GeneralRegister32 source) {
        assemble0085((byte) 0x2A, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2sdq  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsi2sdq  xmm0, rax}
     */
    // Template#: 174, Serial#: 5050
    public void cvtsi2sdq(AMD64XMMRegister destination, AMD64GeneralRegister64 source) {
        assemble0086((byte) 0x2A, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2sdl  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvtsi2sdl  xmm0, [L1: +305419896]}
     */
    // Template#: 175, Serial#: 5036
    public void rip_cvtsi2sdl(AMD64XMMRegister destination, int rel32) {
        assemble0029((byte) 0x2A, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2sdq  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvtsi2sdq  xmm0, [L1: +305419896]}
     */
    // Template#: 176, Serial#: 5045
    public void rip_cvtsi2sdq(AMD64XMMRegister destination, int rel32) {
        assemble0087((byte) 0x2A, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2sdl  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsi2sdl  xmm0, [rbx + 305419896]}
     */
    // Template#: 177, Serial#: 5039
    public void cvtsi2sdl(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0030((byte) 0x2A, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2sdq  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsi2sdq  xmm0, [rbx + 305419896]}
     */
    // Template#: 178, Serial#: 5048
    public void cvtsi2sdq(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0088((byte) 0x2A, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2ssl  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsi2ssl  xmm0, [rbx + 18]}
     */
    // Template#: 179, Serial#: 5091
    public void cvtsi2ssl(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0031((byte) 0x2A, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2ssq  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsi2ssq  xmm0, [rbx + 18]}
     */
    // Template#: 180, Serial#: 5100
    public void cvtsi2ssq(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0089((byte) 0x2A, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2ssl  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsi2ssl  xmm0, eax}
     */
    // Template#: 181, Serial#: 5095
    public void cvtsi2ssl(AMD64XMMRegister destination, AMD64GeneralRegister32 source) {
        assemble0090((byte) 0x2A, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2ssq  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsi2ssq  xmm0, rax}
     */
    // Template#: 182, Serial#: 5104
    public void cvtsi2ssq(AMD64XMMRegister destination, AMD64GeneralRegister64 source) {
        assemble0091((byte) 0x2A, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2ssl  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvtsi2ssl  xmm0, [L1: +305419896]}
     */
    // Template#: 183, Serial#: 5090
    public void rip_cvtsi2ssl(AMD64XMMRegister destination, int rel32) {
        assemble0033((byte) 0x2A, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2ssq  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvtsi2ssq  xmm0, [L1: +305419896]}
     */
    // Template#: 184, Serial#: 5099
    public void rip_cvtsi2ssq(AMD64XMMRegister destination, int rel32) {
        assemble0092((byte) 0x2A, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2ssl  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsi2ssl  xmm0, [rbx + 305419896]}
     */
    // Template#: 185, Serial#: 5093
    public void cvtsi2ssl(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0034((byte) 0x2A, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2ssq  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtsi2ssq  xmm0, [rbx + 305419896]}
     */
    // Template#: 186, Serial#: 5102
    public void cvtsi2ssq(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0093((byte) 0x2A, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2sd  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtss2sd  xmm0, [rbx + 18]}
     */
    // Template#: 187, Serial#: 5586
    public void cvtss2sd(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0031((byte) 0x5A, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2sd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtss2sd  xmm0, xmm0}
     */
    // Template#: 188, Serial#: 5590
    public void cvtss2sd(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0032((byte) 0x5A, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2sd  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvtss2sd  xmm0, [L1: +305419896]}
     */
    // Template#: 189, Serial#: 5585
    public void rip_cvtss2sd(AMD64XMMRegister destination, int rel32) {
        assemble0033((byte) 0x5A, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2sd  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtss2sd  xmm0, [rbx + 305419896]}
     */
    // Template#: 190, Serial#: 5588
    public void cvtss2sd(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0034((byte) 0x5A, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2si  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtss2si  eax, [rbx + 18]}
     */
    // Template#: 191, Serial#: 5127
    public void cvtss2si(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0094((byte) 0x2D, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2si  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtss2si  eax, xmm0}
     */
    // Template#: 192, Serial#: 5131
    public void cvtss2si(AMD64GeneralRegister32 destination, AMD64XMMRegister source) {
        assemble0095((byte) 0x2D, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2si  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvtss2si  eax, [L1: +305419896]}
     */
    // Template#: 193, Serial#: 5126
    public void rip_cvtss2si(AMD64GeneralRegister32 destination, int rel32) {
        assemble0096((byte) 0x2D, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2si  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtss2si  eax, [rbx + 305419896]}
     */
    // Template#: 194, Serial#: 5129
    public void cvtss2si(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0097((byte) 0x2D, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2si  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtss2si  rax, [rbx + 18]}
     */
    // Template#: 195, Serial#: 5136
    public void cvtss2si(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0098((byte) 0x2D, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2si  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtss2si  rax, xmm0}
     */
    // Template#: 196, Serial#: 5140
    public void cvtss2si(AMD64GeneralRegister64 destination, AMD64XMMRegister source) {
        assemble0099((byte) 0x2D, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2si  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvtss2si  rax, [L1: +305419896]}
     */
    // Template#: 197, Serial#: 5135
    public void rip_cvtss2si(AMD64GeneralRegister64 destination, int rel32) {
        assemble0100((byte) 0x2D, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2si  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvtss2si  rax, [rbx + 305419896]}
     */
    // Template#: 198, Serial#: 5138
    public void cvtss2si(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0101((byte) 0x2D, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttsd2si  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvttsd2si  eax, xmm0}
     */
    // Template#: 199, Serial#: 5059
    public void cvttsd2si(AMD64GeneralRegister32 destination, AMD64XMMRegister source) {
        assemble0077((byte) 0x2C, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttsd2si  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvttsd2si  eax, [L1: +305419896]}
     */
    // Template#: 200, Serial#: 5054
    public void rip_cvttsd2si(AMD64GeneralRegister32 destination, int rel32) {
        assemble0078((byte) 0x2C, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttsd2si  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvttsd2si  eax, [rbx + 305419896]}
     */
    // Template#: 201, Serial#: 5057
    public void cvttsd2si(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0079((byte) 0x2C, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttsd2si  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cvttsd2si  rax, [rbx + 18]}
     */
    // Template#: 202, Serial#: 5064
    public void cvttsd2si(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0080((byte) 0x2C, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttsd2si  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvttsd2si  rax, xmm0}
     */
    // Template#: 203, Serial#: 5068
    public void cvttsd2si(AMD64GeneralRegister64 destination, AMD64XMMRegister source) {
        assemble0081((byte) 0x2C, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttsd2si  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvttsd2si  rax, [L1: +305419896]}
     */
    // Template#: 204, Serial#: 5063
    public void rip_cvttsd2si(AMD64GeneralRegister64 destination, int rel32) {
        assemble0082((byte) 0x2C, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttsd2si  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvttsd2si  rax, [rbx + 305419896]}
     */
    // Template#: 205, Serial#: 5066
    public void cvttsd2si(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0083((byte) 0x2C, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttss2si  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cvttss2si  eax, [rbx + 18]}
     */
    // Template#: 206, Serial#: 5109
    public void cvttss2si(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0094((byte) 0x2C, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttss2si  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvttss2si  eax, xmm0}
     */
    // Template#: 207, Serial#: 5113
    public void cvttss2si(AMD64GeneralRegister32 destination, AMD64XMMRegister source) {
        assemble0095((byte) 0x2C, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttss2si  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvttss2si  eax, [L1: +305419896]}
     */
    // Template#: 208, Serial#: 5108
    public void rip_cvttss2si(AMD64GeneralRegister32 destination, int rel32) {
        assemble0096((byte) 0x2C, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttss2si  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvttss2si  eax, [rbx + 305419896]}
     */
    // Template#: 209, Serial#: 5111
    public void cvttss2si(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0097((byte) 0x2C, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttss2si  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code cvttss2si  rax, [rbx + 18]}
     */
    // Template#: 210, Serial#: 5118
    public void cvttss2si(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0098((byte) 0x2C, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttss2si  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code cvttss2si  rax, xmm0}
     */
    // Template#: 211, Serial#: 5122
    public void cvttss2si(AMD64GeneralRegister64 destination, AMD64XMMRegister source) {
        assemble0099((byte) 0x2C, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttss2si  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code cvttss2si  rax, [L1: +305419896]}
     */
    // Template#: 212, Serial#: 5117
    public void rip_cvttss2si(AMD64GeneralRegister64 destination, int rel32) {
        assemble0100((byte) 0x2C, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttss2si  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code cvttss2si  rax, [rbx + 305419896]}
     */
    // Template#: 213, Serial#: 5120
    public void cvttss2si(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0101((byte) 0x2C, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code decq  }<i>destination</i>
     * Example disassembly syntax: {@code decq      rax}
     */
    // Template#: 214, Serial#: 3027
    public void decq(AMD64GeneralRegister64 destination) {
        assemble0102((byte) 0xFF, (byte) 0x01, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code divq  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code divq      [rbx + 18]}
     */
    // Template#: 215, Serial#: 1895
    public void divq(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0103((byte) 0xF7, (byte) 0x06, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code divq  }<i>destination</i>
     * Example disassembly syntax: {@code divq      rax}
     */
    // Template#: 216, Serial#: 1918
    public void divq(AMD64GeneralRegister64 destination) {
        assemble0102((byte) 0xF7, (byte) 0x06, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code divq  }<i>rel32</i>
     * Example disassembly syntax: {@code divq      [L1: +305419896]}
     */
    // Template#: 217, Serial#: 1880
    public void rip_divq(int rel32) {
        assemble0104((byte) 0xF7, (byte) 0x06, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code divq  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code divq      [rbx + 305419896]}
     */
    // Template#: 218, Serial#: 1909
    public void divq(int disp32, AMD64IndirectRegister64 destination) {
        assemble0105((byte) 0xF7, (byte) 0x06, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code divsd  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code divsd     xmm0, [rbx + 18]}
     */
    // Template#: 219, Serial#: 5550
    public void divsd(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0027((byte) 0x5E, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code divsd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code divsd     xmm0, xmm0}
     */
    // Template#: 220, Serial#: 5554
    public void divsd(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0028((byte) 0x5E, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code divsd  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code divsd     xmm0, [L1: +305419896]}
     */
    // Template#: 221, Serial#: 5549
    public void rip_divsd(AMD64XMMRegister destination, int rel32) {
        assemble0029((byte) 0x5E, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code divsd  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code divsd     xmm0, [rbx + 305419896]}
     */
    // Template#: 222, Serial#: 5552
    public void divsd(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0030((byte) 0x5E, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code divss  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code divss     xmm0, [rbx + 18]}
     */
    // Template#: 223, Serial#: 5622
    public void divss(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0031((byte) 0x5E, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code divss  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code divss     xmm0, xmm0}
     */
    // Template#: 224, Serial#: 5626
    public void divss(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0032((byte) 0x5E, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code divss  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code divss     xmm0, [L1: +305419896]}
     */
    // Template#: 225, Serial#: 5621
    public void rip_divss(AMD64XMMRegister destination, int rel32) {
        assemble0033((byte) 0x5E, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code divss  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code divss     xmm0, [rbx + 305419896]}
     */
    // Template#: 226, Serial#: 5624
    public void divss(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0034((byte) 0x5E, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code enter  }<i>imm16</i>, <i>imm8</i>
     * Example disassembly syntax: {@code enter     0x1234, 0x12}
     */
    // Template#: 227, Serial#: 2494
    public void enter(short imm16, byte imm8) {
        assemble0106((byte) 0xC8, imm16, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code idivl  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code idivl     [rbx + 18]}
     */
    // Template#: 228, Serial#: 1834
    public void idivl(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0035((byte) 0xF7, (byte) 0x07, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code idivq  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code idivq     [rbx + 18]}
     */
    // Template#: 229, Serial#: 1897
    public void idivq(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0103((byte) 0xF7, (byte) 0x07, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code idivl  }<i>destination</i>
     * Example disassembly syntax: {@code idivl     eax}
     */
    // Template#: 230, Serial#: 1856
    public void idivl(AMD64GeneralRegister32 destination) {
        assemble0107((byte) 0xF7, (byte) 0x07, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code idivq  }<i>destination</i>
     * Example disassembly syntax: {@code idivq     rax}
     */
    // Template#: 231, Serial#: 1919
    public void idivq(AMD64GeneralRegister64 destination) {
        assemble0102((byte) 0xF7, (byte) 0x07, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code idivl  }<i>rel32</i>
     * Example disassembly syntax: {@code idivl     [L1: +305419896]}
     */
    // Template#: 232, Serial#: 1821
    public void rip_idivl(int rel32) {
        assemble0038((byte) 0xF7, (byte) 0x07, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code idivq  }<i>rel32</i>
     * Example disassembly syntax: {@code idivq     [L1: +305419896]}
     */
    // Template#: 233, Serial#: 1884
    public void rip_idivq(int rel32) {
        assemble0104((byte) 0xF7, (byte) 0x07, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code idivl  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code idivl     [rbx + 305419896]}
     */
    // Template#: 234, Serial#: 1848
    public void idivl(int disp32, AMD64IndirectRegister64 destination) {
        assemble0039((byte) 0xF7, (byte) 0x07, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code idivq  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code idivq     [rbx + 305419896]}
     */
    // Template#: 235, Serial#: 1911
    public void idivq(int disp32, AMD64IndirectRegister64 destination) {
        assemble0105((byte) 0xF7, (byte) 0x07, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code imull  }<i>destination</i>
     * Example disassembly syntax: {@code imull     eax}
     */
    // Template#: 236, Serial#: 1854
    public void imull(AMD64GeneralRegister32 destination) {
        assemble0107((byte) 0xF7, (byte) 0x05, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code imul      eax, [rbx + 18]}
     */
    // Template#: 237, Serial#: 6104
    public void imul(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0041((byte) 0xAF, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code imul      eax, eax}
     */
    // Template#: 238, Serial#: 6108
    public void imul(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0042((byte) 0xAF, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>source</i>, <i>imm8</i>
     * Example disassembly syntax: {@code imul      eax, eax, 0x12}
     */
    // Template#: 239, Serial#: 2313
    public void imul(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source, byte imm8) {
        assemble0108((byte) 0x6B, destination, source, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>source</i>, <i>imm32</i>
     * Example disassembly syntax: {@code imul      eax, eax, 0x12345678}
     */
    // Template#: 240, Serial#: 2285
    public void imul(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source, int imm32) {
        assemble0109((byte) 0x69, destination, source, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code imul      eax, [L1: +305419896]}
     */
    // Template#: 241, Serial#: 6103
    public void rip_imul(AMD64GeneralRegister32 destination, int rel32) {
        assemble0043((byte) 0xAF, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code imul      eax, [rbx + 305419896]}
     */
    // Template#: 242, Serial#: 6106
    public void imul(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0044((byte) 0xAF, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code imulq  }<i>destination</i>
     * Example disassembly syntax: {@code imulq     rax}
     */
    // Template#: 243, Serial#: 1917
    public void imulq(AMD64GeneralRegister64 destination) {
        assemble0102((byte) 0xF7, (byte) 0x05, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code imul      rax, [rbx + 18]}
     */
    // Template#: 244, Serial#: 6113
    public void imul(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0045((byte) 0xAF, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code imul      rax, rax}
     */
    // Template#: 245, Serial#: 6117
    public void imul(AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0110((byte) 0xAF, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>source</i>, <i>imm8</i>
     * Example disassembly syntax: {@code imul      rax, rax, 0x12}
     */
    // Template#: 246, Serial#: 2322
    public void imul(AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source, byte imm8) {
        assemble0111((byte) 0x6B, destination, source, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>source</i>, <i>imm32</i>
     * Example disassembly syntax: {@code imul      rax, rax, 0x12345678}
     */
    // Template#: 247, Serial#: 2294
    public void imul(AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source, int imm32) {
        assemble0112((byte) 0x69, destination, source, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code imul      rax, [L1: +305419896]}
     */
    // Template#: 248, Serial#: 6112
    public void rip_imul(AMD64GeneralRegister64 destination, int rel32) {
        assemble0113((byte) 0xAF, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code imul      rax, [rbx + 305419896]}
     */
    // Template#: 249, Serial#: 6115
    public void imul(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0114((byte) 0xAF, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code int  }
     * Example disassembly syntax: {@code int       0x3}
     */
    // Template#: 250, Serial#: 2498
    public void int_3() {
        assemble0040((byte) 0xCC);
    }

    /**
     * Pseudo-external assembler syntax: {@code jb  }<i>rel8</i>
     * Example disassembly syntax: {@code jb        L1: +18}
     */
    // Template#: 251, Serial#: 315
    public void jb(byte rel8) {
        assemble0115((byte) 0x72, rel8);
    }

    /**
     * Pseudo-external assembler syntax: {@code jb  }<i>rel32</i>
     * Example disassembly syntax: {@code jb        L1: +305419896}
     */
    // Template#: 252, Serial#: 4056
    public void jb(int rel32) {
        assemble0116((byte) 0x82, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code jbe  }<i>rel8</i>
     * Example disassembly syntax: {@code jbe       L1: +18}
     */
    // Template#: 253, Serial#: 319
    public void jbe(byte rel8) {
        assemble0115((byte) 0x76, rel8);
    }

    /**
     * Pseudo-external assembler syntax: {@code jbe  }<i>rel32</i>
     * Example disassembly syntax: {@code jbe       L1: +305419896}
     */
    // Template#: 254, Serial#: 4060
    public void jbe(int rel32) {
        assemble0116((byte) 0x86, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code jl  }<i>rel8</i>
     * Example disassembly syntax: {@code jl        L1: +18}
     */
    // Template#: 255, Serial#: 2342
    public void jl(byte rel8) {
        assemble0115((byte) 0x7C, rel8);
    }

    /**
     * Pseudo-external assembler syntax: {@code jl  }<i>rel32</i>
     * Example disassembly syntax: {@code jl        L1: +305419896}
     */
    // Template#: 256, Serial#: 5897
    public void jl(int rel32) {
        assemble0116((byte) 0x8C, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code jle  }<i>rel8</i>
     * Example disassembly syntax: {@code jle       L1: +18}
     */
    // Template#: 257, Serial#: 2344
    public void jle(byte rel8) {
        assemble0115((byte) 0x7E, rel8);
    }

    /**
     * Pseudo-external assembler syntax: {@code jle  }<i>rel32</i>
     * Example disassembly syntax: {@code jle       L1: +305419896}
     */
    // Template#: 258, Serial#: 5899
    public void jle(int rel32) {
        assemble0116((byte) 0x8E, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code jmp  }<i>rel8</i>
     * Example disassembly syntax: {@code jmp       L1: +18}
     */
    // Template#: 259, Serial#: 2959
    public void jmp(byte rel8) {
        assemble0115((byte) 0xEB, rel8);
    }

    /**
     * Pseudo-external assembler syntax: {@code jmp  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code jmp       [rbx + 18]}
     */
    // Template#: 260, Serial#: 3060
    public void jmp(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0035((byte) 0xFF, (byte) 0x04, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code jmp  }<i>destination</i>
     * Example disassembly syntax: {@code jmp       rax}
     */
    // Template#: 261, Serial#: 3071
    public void jmp(AMD64GeneralRegister64 destination) {
        assemble0036((byte) 0xFF, (byte) 0x04, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code jmp  }<i>rel32</i>
     * Example disassembly syntax: {@code jmp       L1: +305419896}
     */
    // Template#: 262, Serial#: 2958
    public void jmp(int rel32) {
        assemble0037((byte) 0xE9, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code jmp  }<i>rel32</i>
     * Example disassembly syntax: {@code jmp       [L1: +305419896]}
     */
    // Template#: 263, Serial#: 3053
    public void rip_jmp(int rel32) {
        assemble0038((byte) 0xFF, (byte) 0x04, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code jmp  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code jmp       [rbx + 305419896]}
     */
    // Template#: 264, Serial#: 3066
    public void jmp(int disp32, AMD64IndirectRegister64 destination) {
        assemble0039((byte) 0xFF, (byte) 0x04, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnb  }<i>rel8</i>
     * Example disassembly syntax: {@code jnb       L1: +18}
     */
    // Template#: 265, Serial#: 316
    public void jnb(byte rel8) {
        assemble0115((byte) 0x73, rel8);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnb  }<i>rel32</i>
     * Example disassembly syntax: {@code jnb       L1: +305419896}
     */
    // Template#: 266, Serial#: 4057
    public void jnb(int rel32) {
        assemble0116((byte) 0x83, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnbe  }<i>rel8</i>
     * Example disassembly syntax: {@code jnbe      L1: +18}
     */
    // Template#: 267, Serial#: 320
    public void jnbe(byte rel8) {
        assemble0115((byte) 0x77, rel8);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnbe  }<i>rel32</i>
     * Example disassembly syntax: {@code jnbe      L1: +305419896}
     */
    // Template#: 268, Serial#: 4061
    public void jnbe(int rel32) {
        assemble0116((byte) 0x87, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnl  }<i>rel8</i>
     * Example disassembly syntax: {@code jnl       L1: +18}
     */
    // Template#: 269, Serial#: 2343
    public void jnl(byte rel8) {
        assemble0115((byte) 0x7D, rel8);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnl  }<i>rel32</i>
     * Example disassembly syntax: {@code jnl       L1: +305419896}
     */
    // Template#: 270, Serial#: 5898
    public void jnl(int rel32) {
        assemble0116((byte) 0x8D, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnle  }<i>rel8</i>
     * Example disassembly syntax: {@code jnle      L1: +18}
     */
    // Template#: 271, Serial#: 2345
    public void jnle(byte rel8) {
        assemble0115((byte) 0x7F, rel8);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnle  }<i>rel32</i>
     * Example disassembly syntax: {@code jnle      L1: +305419896}
     */
    // Template#: 272, Serial#: 5900
    public void jnle(int rel32) {
        assemble0116((byte) 0x8F, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnz  }<i>rel8</i>
     * Example disassembly syntax: {@code jnz       L1: +18}
     */
    // Template#: 273, Serial#: 318
    public void jnz(byte rel8) {
        assemble0115((byte) 0x75, rel8);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnz  }<i>rel32</i>
     * Example disassembly syntax: {@code jnz       L1: +305419896}
     */
    // Template#: 274, Serial#: 4059
    public void jnz(int rel32) {
        assemble0116((byte) 0x85, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code jz  }<i>rel8</i>
     * Example disassembly syntax: {@code jz        L1: +18}
     */
    // Template#: 275, Serial#: 317
    public void jz(byte rel8) {
        assemble0115((byte) 0x74, rel8);
    }

    /**
     * Pseudo-external assembler syntax: {@code jz  }<i>rel32</i>
     * Example disassembly syntax: {@code jz        L1: +305419896}
     */
    // Template#: 276, Serial#: 4058
    public void jz(int rel32) {
        assemble0116((byte) 0x84, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code lea  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code lea       rax, [rbx + 18]}
     */
    // Template#: 277, Serial#: 2435
    public void lea(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0014((byte) 0x8D, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code lea  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code lea       rax, [rbx]}
     */
    // Template#: 278, Serial#: 2431
    public void lea(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0117((byte) 0x8D, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code lea  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code lea       rax, [L1: +305419896]}
     */
    // Template#: 279, Serial#: 2434
    public void rip_lea(AMD64GeneralRegister64 destination, int rel32) {
        assemble0016((byte) 0x8D, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code lea  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code lea       rax, [rbx + 305419896]}
     */
    // Template#: 280, Serial#: 2437
    public void lea(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0018((byte) 0x8D, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code leave  }
     * Example disassembly syntax: {@code leave   }
     */
    // Template#: 281, Serial#: 2495
    public void leave() {
        assemble0040((byte) 0xC9);
    }

    /**
     * Pseudo-external assembler syntax: {@code lfence  }
     * Example disassembly syntax: {@code lfence  }
     */
    // Template#: 282, Serial#: 6097
    public void lfence() {
        assemble0118((byte) 0xAE, (byte) 0x05);
    }

    /**
     * Pseudo-external assembler syntax: {@code lock  }
     * Example disassembly syntax: {@code lock    }
     */
    // Template#: 283, Serial#: 1725
    public void lock() {
        assemble0040((byte) 0xF0);
    }

    /**
     * Pseudo-external assembler syntax: {@code mfence  }
     * Example disassembly syntax: {@code mfence  }
     */
    // Template#: 284, Serial#: 6098
    public void mfence() {
        assemble0118((byte) 0xAE, (byte) 0x06);
    }

    /**
     * Pseudo-external assembler syntax: {@code movb  }<i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>imm8</i>
     * Example disassembly syntax: {@code movb      rbx[rsi * 4 + 18], 0x12}
     */
    // Template#: 285, Serial#: 1177
    public void movb(byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, byte imm8) {
        assemble0119((byte) 0xC6, disp8, base, index, scale, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rbx[rsi * 4 + 18], ax}
     */
    // Template#: 286, Serial#: 2378
    public void mov(byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister16 source) {
        assemble0120((byte) 0x89, disp8, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rbx[rsi * 4 + 18], eax}
     */
    // Template#: 287, Serial#: 2360
    public void mov(byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister32 source) {
        assemble0121((byte) 0x89, disp8, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rbx[rsi * 4 + 18], rax}
     */
    // Template#: 288, Serial#: 2369
    public void mov(byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister64 source) {
        assemble0122((byte) 0x89, disp8, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rbx[rsi * 4 + 18], al}
     */
    // Template#: 289, Serial#: 2351
    public void mov(byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister8 source) {
        assemble0123((byte) 0x88, disp8, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movl  }<i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>imm32</i>
     * Example disassembly syntax: {@code movl      rbx[rsi * 4 + 18], 0x12345678}
     */
    // Template#: 290, Serial#: 1186
    public void movl(byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, int imm32) {
        assemble0124((byte) 0xC7, disp8, base, index, scale, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movw  }<i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>imm16</i>
     * Example disassembly syntax: {@code movw      rbx[rsi * 4 + 18], 0x1234}
     */
    // Template#: 291, Serial#: 1204
    public void movw(byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, short imm16) {
        assemble0125((byte) 0xC7, disp8, base, index, scale, imm16);
    }

    /**
     * Pseudo-external assembler syntax: {@code movb  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code movb      [rbx + 18], 0x12}
     */
    // Template#: 292, Serial#: 1176
    public void movb(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0126((byte) 0xC6, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [rbx + 18], ax}
     */
    // Template#: 293, Serial#: 2377
    public void mov(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister16 source) {
        assemble0127((byte) 0x89, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [rbx + 18], eax}
     */
    // Template#: 294, Serial#: 2359
    public void mov(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0003((byte) 0x89, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [rbx + 18], rax}
     */
    // Template#: 295, Serial#: 2368
    public void mov(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0004((byte) 0x89, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [rbx + 18], al}
     */
    // Template#: 296, Serial#: 2350
    public void mov(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister8 source) {
        assemble0128((byte) 0x88, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movl  }<i>disp8</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code movl      [rbx + 18], 0x12345678}
     */
    // Template#: 297, Serial#: 1185
    public void movl(byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        assemble0129((byte) 0xC7, disp8, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movw  }<i>disp8</i>, <i>destination</i>, <i>imm16</i>
     * Example disassembly syntax: {@code movw      [rbx + 18], 0x1234}
     */
    // Template#: 298, Serial#: 1203
    public void movw(byte disp8, AMD64IndirectRegister64 destination, short imm16) {
        assemble0130((byte) 0xC7, disp8, destination, imm16);
    }

    /**
     * Pseudo-external assembler syntax: {@code movb  }<i>base</i>, <i>index</i>, <i>scale</i>, <i>imm8</i>
     * Example disassembly syntax: {@code movb      rbx[rsi * 4], 0x12}
     */
    // Template#: 299, Serial#: 1173
    public void movb(AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, byte imm8) {
        assemble0131((byte) 0xC6, base, index, scale, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rbx[rsi * 4], ax}
     */
    // Template#: 300, Serial#: 2374
    public void mov(AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister16 source) {
        assemble0132((byte) 0x89, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rbx[rsi * 4], eax}
     */
    // Template#: 301, Serial#: 2356
    public void mov(AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister32 source) {
        assemble0133((byte) 0x89, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rbx[rsi * 4], rax}
     */
    // Template#: 302, Serial#: 2365
    public void mov(AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister64 source) {
        assemble0134((byte) 0x89, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rbx[rsi * 4], al}
     */
    // Template#: 303, Serial#: 2347
    public void mov(AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister8 source) {
        assemble0135((byte) 0x88, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movl  }<i>base</i>, <i>index</i>, <i>scale</i>, <i>imm32</i>
     * Example disassembly syntax: {@code movl      rbx[rsi * 4], 0x12345678}
     */
    // Template#: 304, Serial#: 1182
    public void movl(AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, int imm32) {
        assemble0136((byte) 0xC7, base, index, scale, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movw  }<i>base</i>, <i>index</i>, <i>scale</i>, <i>imm16</i>
     * Example disassembly syntax: {@code movw      rbx[rsi * 4], 0x1234}
     */
    // Template#: 305, Serial#: 1200
    public void movw(AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, short imm16) {
        assemble0137((byte) 0xC7, base, index, scale, imm16);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code mov       eax, rbx[rsi * 4 + 18]}
     */
    // Template#: 306, Serial#: 2395
    public void mov(AMD64GeneralRegister32 destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0047((byte) 0x8B, destination, disp8, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code mov       eax, rbx[rsi * 4]}
     */
    // Template#: 307, Serial#: 2391
    public void mov(AMD64GeneralRegister32 destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0048((byte) 0x8B, destination, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movl  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code movl      eax, 0x12345678}
     */
    // Template#: 308, Serial#: 1189
    public void movl(AMD64GeneralRegister32 destination, int imm32) {
        assemble0138((byte) 0xC7, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>register</i>, <i>imm32</i>
     * Example disassembly syntax: {@code mov       eax, 0x12345678}
     */
    // Template#: 309, Serial#: 2491
    public void mov(AMD64GeneralRegister32 register, int imm32) {
        assemble0139((byte) 0xB8, register, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       eax, [rbx + 305419896]}
     */
    // Template#: 310, Serial#: 2396
    public void mov(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0012((byte) 0x8B, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code mov       rax, rbx[rsi * 4 + 18]}
     */
    // Template#: 311, Serial#: 2403
    public void mov(AMD64GeneralRegister64 destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0140((byte) 0x8B, destination, disp8, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rax, [rbx + 18]}
     */
    // Template#: 312, Serial#: 2402
    public void mov(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0014((byte) 0x8B, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code mov       rax, rbx[rsi * 4]}
     */
    // Template#: 313, Serial#: 2399
    public void mov(AMD64GeneralRegister64 destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0141((byte) 0x8B, destination, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rax, rax}
     */
    // Template#: 314, Serial#: 2372
    public void mov(AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0015((byte) 0x89, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rax, [rbx]}
     */
    // Template#: 315, Serial#: 2398
    public void mov(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0117((byte) 0x8B, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movq  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code movq      rax, 0x12345678}
     */
    // Template#: 316, Serial#: 1198
    public void movq(AMD64GeneralRegister64 destination, int imm32) {
        assemble0142((byte) 0xC7, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code mov       rax, [L1: +305419896]}
     */
    // Template#: 317, Serial#: 2401
    public void rip_mov(AMD64GeneralRegister64 destination, int rel32) {
        assemble0016((byte) 0x8B, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code mov       rax, rbx[rsi * 4 + 305419896]}
     */
    // Template#: 318, Serial#: 2405
    public void mov(AMD64GeneralRegister64 destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0143((byte) 0x8B, destination, disp32, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rax, [rbx + 305419896]}
     */
    // Template#: 319, Serial#: 2404
    public void mov(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0018((byte) 0x8B, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>register</i>, <i>imm64</i>
     * Example disassembly syntax: {@code mov       rax, 0x123456789ABCDE}
     */
    // Template#: 320, Serial#: 2492
    public void mov(AMD64GeneralRegister64 register, long imm64) {
        assemble0144((byte) 0xB8, register, imm64);
    }

    /**
     * Pseudo-external assembler syntax: {@code movb  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code movb      [rbx], 0x12}
     */
    // Template#: 321, Serial#: 1172
    public void movb(AMD64IndirectRegister64 destination, byte imm8) {
        assemble0145((byte) 0xC6, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [rbx], ax}
     */
    // Template#: 322, Serial#: 2373
    public void mov(AMD64IndirectRegister64 destination, AMD64GeneralRegister16 source) {
        assemble0146((byte) 0x89, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [rbx], eax}
     */
    // Template#: 323, Serial#: 2355
    public void mov(AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0147((byte) 0x89, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [rbx], rax}
     */
    // Template#: 324, Serial#: 2364
    public void mov(AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0148((byte) 0x89, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [rbx], al}
     */
    // Template#: 325, Serial#: 2346
    public void mov(AMD64IndirectRegister64 destination, AMD64GeneralRegister8 source) {
        assemble0149((byte) 0x88, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movl  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code movl      [rbx], 0x12345678}
     */
    // Template#: 326, Serial#: 1181
    public void movl(AMD64IndirectRegister64 destination, int imm32) {
        assemble0150((byte) 0xC7, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movw  }<i>destination</i>, <i>imm16</i>
     * Example disassembly syntax: {@code movw      [rbx], 0x1234}
     */
    // Template#: 327, Serial#: 1199
    public void movw(AMD64IndirectRegister64 destination, short imm16) {
        assemble0151((byte) 0xC7, destination, imm16);
    }

    /**
     * Pseudo-external assembler syntax: {@code movb  }<i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>imm8</i>
     * Example disassembly syntax: {@code movb      rbx[rsi * 4 + 305419896], 0x12}
     */
    // Template#: 328, Serial#: 1179
    public void movb(int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, byte imm8) {
        assemble0152((byte) 0xC6, disp32, base, index, scale, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rbx[rsi * 4 + 305419896], ax}
     */
    // Template#: 329, Serial#: 2380
    public void mov(int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister16 source) {
        assemble0153((byte) 0x89, disp32, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rbx[rsi * 4 + 305419896], eax}
     */
    // Template#: 330, Serial#: 2362
    public void mov(int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister32 source) {
        assemble0154((byte) 0x89, disp32, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rbx[rsi * 4 + 305419896], rax}
     */
    // Template#: 331, Serial#: 2371
    public void mov(int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister64 source) {
        assemble0155((byte) 0x89, disp32, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       rbx[rsi * 4 + 305419896], al}
     */
    // Template#: 332, Serial#: 2353
    public void mov(int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister8 source) {
        assemble0156((byte) 0x88, disp32, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movl  }<i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>imm32</i>
     * Example disassembly syntax: {@code movl      rbx[rsi * 4 + 305419896], 0x12345678}
     */
    // Template#: 333, Serial#: 1188
    public void movl(int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, int imm32) {
        assemble0157((byte) 0xC7, disp32, base, index, scale, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movw  }<i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>imm16</i>
     * Example disassembly syntax: {@code movw      rbx[rsi * 4 + 305419896], 0x1234}
     */
    // Template#: 334, Serial#: 1206
    public void movw(int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, short imm16) {
        assemble0158((byte) 0xC7, disp32, base, index, scale, imm16);
    }

    /**
     * Pseudo-external assembler syntax: {@code movb  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code movb      [rbx + 305419896], 0x12}
     */
    // Template#: 335, Serial#: 1178
    public void movb(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0159((byte) 0xC6, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [rbx + 305419896], ax}
     */
    // Template#: 336, Serial#: 2379
    public void mov(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister16 source) {
        assemble0160((byte) 0x89, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [rbx + 305419896], eax}
     */
    // Template#: 337, Serial#: 2361
    public void mov(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0023((byte) 0x89, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [rbx + 305419896], rax}
     */
    // Template#: 338, Serial#: 2370
    public void mov(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0024((byte) 0x89, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [rbx + 305419896], al}
     */
    // Template#: 339, Serial#: 2352
    public void mov(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister8 source) {
        assemble0161((byte) 0x88, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movl  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code movl      [rbx + 305419896], 0x12345678}
     */
    // Template#: 340, Serial#: 1187
    public void movl(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0162((byte) 0xC7, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movq  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code movq      [rbx + 305419896], 0x12345678}
     */
    // Template#: 341, Serial#: 1196
    public void movq(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0163((byte) 0xC7, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movw  }<i>disp32</i>, <i>destination</i>, <i>imm16</i>
     * Example disassembly syntax: {@code movw      [rbx + 305419896], 0x1234}
     */
    // Template#: 342, Serial#: 1205
    public void movw(int disp32, AMD64IndirectRegister64 destination, short imm16) {
        assemble0164((byte) 0xC7, disp32, destination, imm16);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdl  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movdl     [rbx + 18], xmm0}
     */
    // Template#: 343, Serial#: 5837
    public void movdl(byte disp8, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        assemble0165((byte) 0x7E, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdq  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movdq     [rbx + 18], xmm0}
     */
    // Template#: 344, Serial#: 5846
    public void movdq(byte disp8, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        assemble0166((byte) 0x7E, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdl  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movdl     eax, xmm0}
     */
    // Template#: 345, Serial#: 5841
    public void movdl(AMD64GeneralRegister32 destination, AMD64XMMRegister source) {
        assemble0167((byte) 0x7E, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdq  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movdq     rax, xmm0}
     */
    // Template#: 346, Serial#: 5850
    public void movdq(AMD64GeneralRegister64 destination, AMD64XMMRegister source) {
        assemble0168((byte) 0x7E, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdl  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code movdl     xmm0, [rbx + 18]}
     */
    // Template#: 347, Serial#: 5757
    public void movdl(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0067((byte) 0x6E, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdl  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movdl     xmm0, eax}
     */
    // Template#: 348, Serial#: 5761
    public void movdl(AMD64XMMRegister destination, AMD64GeneralRegister32 source) {
        assemble0169((byte) 0x6E, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdq  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movdq     xmm0, rax}
     */
    // Template#: 349, Serial#: 5770
    public void movdq(AMD64XMMRegister destination, AMD64GeneralRegister64 source) {
        assemble0170((byte) 0x6E, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdl  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code movdl     xmm0, [rbx + 305419896]}
     */
    // Template#: 350, Serial#: 5759
    public void movdl(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0070((byte) 0x6E, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdl  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movdl     [rbx + 305419896], xmm0}
     */
    // Template#: 351, Serial#: 5839
    public void movdl(int disp32, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        assemble0171((byte) 0x7E, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdq  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movdq     [rbx + 305419896], xmm0}
     */
    // Template#: 352, Serial#: 5848
    public void movdq(int disp32, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        assemble0172((byte) 0x7E, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code movsd     rbx[rsi * 4 + 18], xmm0}
     */
    // Template#: 353, Serial#: 3384
    public void movsd(byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64XMMRegister source) {
        assemble0173((byte) 0x11, disp8, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movsd     [rbx + 18], xmm0}
     */
    // Template#: 354, Serial#: 3383
    public void movsd(byte disp8, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        assemble0174((byte) 0x11, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code movsd     rbx[rsi * 4], xmm0}
     */
    // Template#: 355, Serial#: 3380
    public void movsd(AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64XMMRegister source) {
        assemble0175((byte) 0x11, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movsd     [rbx], xmm0}
     */
    // Template#: 356, Serial#: 3379
    public void movsd(AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        assemble0176((byte) 0x11, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>destination</i>, <i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movsd     xmm0, rbx[rsi * 4 + 18]}
     */
    // Template#: 357, Serial#: 3375
    public void movsd(AMD64XMMRegister destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0177((byte) 0x10, destination, disp8, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code movsd     xmm0, [rbx + 18]}
     */
    // Template#: 358, Serial#: 3374
    public void movsd(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0027((byte) 0x10, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>destination</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movsd     xmm0, rbx[rsi * 4]}
     */
    // Template#: 359, Serial#: 3371
    public void movsd(AMD64XMMRegister destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0178((byte) 0x10, destination, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movsd     xmm0, [rbx]}
     */
    // Template#: 360, Serial#: 3370
    public void movsd(AMD64XMMRegister destination, AMD64IndirectRegister64 source) {
        assemble0179((byte) 0x10, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movsd     xmm0, xmm0}
     */
    // Template#: 361, Serial#: 3378
    public void movsd(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0028((byte) 0x10, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code movsd     xmm0, [L1: +305419896]}
     */
    // Template#: 362, Serial#: 3373
    public void rip_movsd(AMD64XMMRegister destination, int rel32) {
        assemble0029((byte) 0x10, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>destination</i>, <i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movsd     xmm0, rbx[rsi * 4 + 305419896]}
     */
    // Template#: 363, Serial#: 3377
    public void movsd(AMD64XMMRegister destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0180((byte) 0x10, destination, disp32, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code movsd     xmm0, [rbx + 305419896]}
     */
    // Template#: 364, Serial#: 3376
    public void movsd(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0030((byte) 0x10, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code movsd     rbx[rsi * 4 + 305419896], xmm0}
     */
    // Template#: 365, Serial#: 3386
    public void movsd(int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64XMMRegister source) {
        assemble0181((byte) 0x11, disp32, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movsd     [rbx + 305419896], xmm0}
     */
    // Template#: 366, Serial#: 3385
    public void movsd(int disp32, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        assemble0182((byte) 0x11, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code movss     rbx[rsi * 4 + 18], xmm0}
     */
    // Template#: 367, Serial#: 3410
    public void movss(byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64XMMRegister source) {
        assemble0183((byte) 0x11, disp8, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movss     [rbx + 18], xmm0}
     */
    // Template#: 368, Serial#: 3409
    public void movss(byte disp8, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        assemble0184((byte) 0x11, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code movss     rbx[rsi * 4], xmm0}
     */
    // Template#: 369, Serial#: 3406
    public void movss(AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64XMMRegister source) {
        assemble0185((byte) 0x11, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movss     [rbx], xmm0}
     */
    // Template#: 370, Serial#: 3405
    public void movss(AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        assemble0186((byte) 0x11, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>destination</i>, <i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movss     xmm0, rbx[rsi * 4 + 18]}
     */
    // Template#: 371, Serial#: 3401
    public void movss(AMD64XMMRegister destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0187((byte) 0x10, destination, disp8, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code movss     xmm0, [rbx + 18]}
     */
    // Template#: 372, Serial#: 3400
    public void movss(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0031((byte) 0x10, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>destination</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movss     xmm0, rbx[rsi * 4]}
     */
    // Template#: 373, Serial#: 3397
    public void movss(AMD64XMMRegister destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0188((byte) 0x10, destination, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movss     xmm0, [rbx]}
     */
    // Template#: 374, Serial#: 3396
    public void movss(AMD64XMMRegister destination, AMD64IndirectRegister64 source) {
        assemble0189((byte) 0x10, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movss     xmm0, xmm0}
     */
    // Template#: 375, Serial#: 3404
    public void movss(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0032((byte) 0x10, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code movss     xmm0, [L1: +305419896]}
     */
    // Template#: 376, Serial#: 3399
    public void rip_movss(AMD64XMMRegister destination, int rel32) {
        assemble0033((byte) 0x10, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>destination</i>, <i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movss     xmm0, rbx[rsi * 4 + 305419896]}
     */
    // Template#: 377, Serial#: 3403
    public void movss(AMD64XMMRegister destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0190((byte) 0x10, destination, disp32, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code movss     xmm0, [rbx + 305419896]}
     */
    // Template#: 378, Serial#: 3402
    public void movss(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0034((byte) 0x10, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>, <i>source</i>
     * Example disassembly syntax: {@code movss     rbx[rsi * 4 + 305419896], xmm0}
     */
    // Template#: 379, Serial#: 3412
    public void movss(int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64XMMRegister source) {
        assemble0191((byte) 0x11, disp32, base, index, scale, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movss     [rbx + 305419896], xmm0}
     */
    // Template#: 380, Serial#: 3411
    public void movss(int disp32, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        assemble0192((byte) 0x11, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsx  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code movsx     eax, [rbx + 18]}
     */
    // Template#: 381, Serial#: 6256
    public void movsxb(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0041((byte) 0xBE, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsx  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movsx     eax, al}
     */
    // Template#: 382, Serial#: 6260
    public void movsxb(AMD64GeneralRegister32 destination, AMD64GeneralRegister8 source) {
        assemble0193((byte) 0xBE, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsx  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code movsx     eax, [L1: +305419896]}
     */
    // Template#: 383, Serial#: 6255
    public void rip_movsxb(AMD64GeneralRegister32 destination, int rel32) {
        assemble0043((byte) 0xBE, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsx  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code movsx     eax, [rbx + 305419896]}
     */
    // Template#: 384, Serial#: 6258
    public void movsxb(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0044((byte) 0xBE, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsx  }<i>destination</i>, <i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movsx     rax, rbx[rsi * 4 + 18]}
     */
    // Template#: 385, Serial#: 6266
    public void movsxb(AMD64GeneralRegister64 destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0194((byte) 0xBE, destination, disp8, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsx  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code movsx     rax, [rbx + 18]}
     */
    // Template#: 386, Serial#: 6265
    public void movsxb(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0045((byte) 0xBE, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsx  }<i>destination</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movsx     rax, rbx[rsi * 4]}
     */
    // Template#: 387, Serial#: 6262
    public void movsxb(AMD64GeneralRegister64 destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0195((byte) 0xBE, destination, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsx  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movsx     rax, [rbx]}
     */
    // Template#: 388, Serial#: 6261
    public void movsxb(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0046((byte) 0xBE, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsx  }<i>destination</i>, <i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movsx     rax, rbx[rsi * 4 + 305419896]}
     */
    // Template#: 389, Serial#: 6268
    public void movsxb(AMD64GeneralRegister64 destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0196((byte) 0xBE, destination, disp32, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsx  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code movsx     rax, [rbx + 305419896]}
     */
    // Template#: 390, Serial#: 6267
    public void movsxb(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0114((byte) 0xBE, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxd  }<i>destination</i>, <i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movsxd    rax, rbx[rsi * 4 + 18]}
     */
    // Template#: 391, Serial#: 296
    public void movsxd(AMD64GeneralRegister64 destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0140((byte) 0x63, destination, disp8, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxd  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code movsxd    rax, [rbx + 18]}
     */
    // Template#: 392, Serial#: 295
    public void movsxd(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0014((byte) 0x63, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxd  }<i>destination</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movsxd    rax, rbx[rsi * 4]}
     */
    // Template#: 393, Serial#: 292
    public void movsxd(AMD64GeneralRegister64 destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0141((byte) 0x63, destination, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movsxd    rax, eax}
     */
    // Template#: 394, Serial#: 299
    public void movsxd(AMD64GeneralRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0197((byte) 0x63, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movsxd    rax, [rbx]}
     */
    // Template#: 395, Serial#: 291
    public void movsxd(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0117((byte) 0x63, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxd  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code movsxd    rax, [L1: +305419896]}
     */
    // Template#: 396, Serial#: 294
    public void rip_movsxd(AMD64GeneralRegister64 destination, int rel32) {
        assemble0016((byte) 0x63, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxd  }<i>destination</i>, <i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movsxd    rax, rbx[rsi * 4 + 305419896]}
     */
    // Template#: 397, Serial#: 298
    public void movsxd(AMD64GeneralRegister64 destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0143((byte) 0x63, destination, disp32, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxd  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code movsxd    rax, [rbx + 305419896]}
     */
    // Template#: 398, Serial#: 297
    public void movsxd(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0018((byte) 0x63, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxw  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code movsxw    eax, [rbx + 18]}
     */
    // Template#: 399, Serial#: 6283
    public void movsxw(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0041((byte) 0xBF, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxw  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movsxw    eax, ax}
     */
    // Template#: 400, Serial#: 6287
    public void movsxw(AMD64GeneralRegister32 destination, AMD64GeneralRegister16 source) {
        assemble0198((byte) 0xBF, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxw  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code movsxw    eax, [L1: +305419896]}
     */
    // Template#: 401, Serial#: 6282
    public void rip_movsxw(AMD64GeneralRegister32 destination, int rel32) {
        assemble0043((byte) 0xBF, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxw  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code movsxw    eax, [rbx + 305419896]}
     */
    // Template#: 402, Serial#: 6285
    public void movsxw(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0044((byte) 0xBF, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxw  }<i>destination</i>, <i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movsxw    rax, rbx[rsi * 4 + 18]}
     */
    // Template#: 403, Serial#: 6293
    public void movsxw(AMD64GeneralRegister64 destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0194((byte) 0xBF, destination, disp8, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxw  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code movsxw    rax, [rbx + 18]}
     */
    // Template#: 404, Serial#: 6292
    public void movsxw(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0045((byte) 0xBF, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxw  }<i>destination</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movsxw    rax, rbx[rsi * 4]}
     */
    // Template#: 405, Serial#: 6289
    public void movsxw(AMD64GeneralRegister64 destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0195((byte) 0xBF, destination, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxw  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movsxw    rax, [rbx]}
     */
    // Template#: 406, Serial#: 6288
    public void movsxw(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0046((byte) 0xBF, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxw  }<i>destination</i>, <i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movsxw    rax, rbx[rsi * 4 + 305419896]}
     */
    // Template#: 407, Serial#: 6295
    public void movsxw(AMD64GeneralRegister64 destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0196((byte) 0xBF, destination, disp32, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxw  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code movsxw    rax, [rbx + 305419896]}
     */
    // Template#: 408, Serial#: 6294
    public void movsxw(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0114((byte) 0xBF, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzx  }<i>destination</i>, <i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movzx     rax, rbx[rsi * 4 + 18]}
     */
    // Template#: 409, Serial#: 4295
    public void movzxb(AMD64GeneralRegister64 destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0194((byte) 0xB6, destination, disp8, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzx  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code movzx     rax, [rbx + 18]}
     */
    // Template#: 410, Serial#: 4294
    public void movzxb(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0045((byte) 0xB6, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzx  }<i>destination</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movzx     rax, rbx[rsi * 4]}
     */
    // Template#: 411, Serial#: 4291
    public void movzxb(AMD64GeneralRegister64 destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0195((byte) 0xB6, destination, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzx  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movzx     rax, [rbx]}
     */
    // Template#: 412, Serial#: 4290
    public void movzxb(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0046((byte) 0xB6, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzx  }<i>destination</i>, <i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movzx     rax, rbx[rsi * 4 + 305419896]}
     */
    // Template#: 413, Serial#: 4297
    public void movzxb(AMD64GeneralRegister64 destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0196((byte) 0xB6, destination, disp32, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzx  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code movzx     rax, [rbx + 305419896]}
     */
    // Template#: 414, Serial#: 4296
    public void movzxb(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0114((byte) 0xB6, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxd  }<i>destination</i>, <i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movzxd    rax, rbx[rsi * 4 + 18]}
     */
    // Template#: 415, Serial#: 305
    public void movzxd(AMD64GeneralRegister64 destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0199((byte) 0x63, destination, disp8, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxd  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code movzxd    rax, [rbx + 18]}
     */
    // Template#: 416, Serial#: 304
    public void movzxd(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0200((byte) 0x63, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxd  }<i>destination</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movzxd    rax, rbx[rsi * 4]}
     */
    // Template#: 417, Serial#: 301
    public void movzxd(AMD64GeneralRegister64 destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0201((byte) 0x63, destination, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movzxd    rax, eax}
     */
    // Template#: 418, Serial#: 308
    public void movzxd(AMD64GeneralRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0202((byte) 0x63, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movzxd    rax, [rbx]}
     */
    // Template#: 419, Serial#: 300
    public void movzxd(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0203((byte) 0x63, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxd  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code movzxd    rax, [L1: +305419896]}
     */
    // Template#: 420, Serial#: 303
    public void rip_movzxd(AMD64GeneralRegister64 destination, int rel32) {
        assemble0204((byte) 0x63, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxd  }<i>destination</i>, <i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movzxd    rax, rbx[rsi * 4 + 305419896]}
     */
    // Template#: 421, Serial#: 307
    public void movzxd(AMD64GeneralRegister64 destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0205((byte) 0x63, destination, disp32, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxd  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code movzxd    rax, [rbx + 305419896]}
     */
    // Template#: 422, Serial#: 306
    public void movzxd(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0206((byte) 0x63, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxw  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code movzxw    eax, [rbx + 18]}
     */
    // Template#: 423, Serial#: 4312
    public void movzxw(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0041((byte) 0xB7, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxw  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movzxw    eax, ax}
     */
    // Template#: 424, Serial#: 4316
    public void movzxw(AMD64GeneralRegister32 destination, AMD64GeneralRegister16 source) {
        assemble0198((byte) 0xB7, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxw  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code movzxw    eax, [L1: +305419896]}
     */
    // Template#: 425, Serial#: 4311
    public void rip_movzxw(AMD64GeneralRegister32 destination, int rel32) {
        assemble0043((byte) 0xB7, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxw  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code movzxw    eax, [rbx + 305419896]}
     */
    // Template#: 426, Serial#: 4314
    public void movzxw(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0044((byte) 0xB7, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxw  }<i>destination</i>, <i>disp8</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movzxw    rax, rbx[rsi * 4 + 18]}
     */
    // Template#: 427, Serial#: 4322
    public void movzxw(AMD64GeneralRegister64 destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0194((byte) 0xB7, destination, disp8, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxw  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code movzxw    rax, [rbx + 18]}
     */
    // Template#: 428, Serial#: 4321
    public void movzxw(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0045((byte) 0xB7, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxw  }<i>destination</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movzxw    rax, rbx[rsi * 4]}
     */
    // Template#: 429, Serial#: 4318
    public void movzxw(AMD64GeneralRegister64 destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0195((byte) 0xB7, destination, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxw  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code movzxw    rax, [rbx]}
     */
    // Template#: 430, Serial#: 4317
    public void movzxw(AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        assemble0046((byte) 0xB7, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxw  }<i>destination</i>, <i>disp32</i>, <i>base</i>, <i>index</i>, <i>scale</i>
     * Example disassembly syntax: {@code movzxw    rax, rbx[rsi * 4 + 305419896]}
     */
    // Template#: 431, Serial#: 4324
    public void movzxw(AMD64GeneralRegister64 destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        assemble0196((byte) 0xB7, destination, disp32, base, index, scale);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxw  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code movzxw    rax, [rbx + 305419896]}
     */
    // Template#: 432, Serial#: 4323
    public void movzxw(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0114((byte) 0xB7, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mulsd  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code mulsd     xmm0, [rbx + 18]}
     */
    // Template#: 433, Serial#: 5514
    public void mulsd(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0027((byte) 0x59, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mulsd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mulsd     xmm0, xmm0}
     */
    // Template#: 434, Serial#: 5518
    public void mulsd(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0028((byte) 0x59, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mulsd  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code mulsd     xmm0, [L1: +305419896]}
     */
    // Template#: 435, Serial#: 5513
    public void rip_mulsd(AMD64XMMRegister destination, int rel32) {
        assemble0029((byte) 0x59, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code mulsd  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code mulsd     xmm0, [rbx + 305419896]}
     */
    // Template#: 436, Serial#: 5516
    public void mulsd(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0030((byte) 0x59, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mulss  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code mulss     xmm0, [rbx + 18]}
     */
    // Template#: 437, Serial#: 5577
    public void mulss(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0031((byte) 0x59, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mulss  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code mulss     xmm0, xmm0}
     */
    // Template#: 438, Serial#: 5581
    public void mulss(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0032((byte) 0x59, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code mulss  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code mulss     xmm0, [L1: +305419896]}
     */
    // Template#: 439, Serial#: 5576
    public void rip_mulss(AMD64XMMRegister destination, int rel32) {
        assemble0033((byte) 0x59, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code mulss  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code mulss     xmm0, [rbx + 305419896]}
     */
    // Template#: 440, Serial#: 5579
    public void mulss(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0034((byte) 0x59, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code negq  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code negq      [rbx + 18]}
     */
    // Template#: 441, Serial#: 1889
    public void negq(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0103((byte) 0xF7, (byte) 0x03, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code negq  }<i>destination</i>
     * Example disassembly syntax: {@code negq      rax}
     */
    // Template#: 442, Serial#: 1915
    public void negq(AMD64GeneralRegister64 destination) {
        assemble0102((byte) 0xF7, (byte) 0x03, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code negq  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code negq      [rbx + 305419896]}
     */
    // Template#: 443, Serial#: 1903
    public void negq(int disp32, AMD64IndirectRegister64 destination) {
        assemble0105((byte) 0xF7, (byte) 0x03, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code nop  }
     * Example disassembly syntax: {@code nop     }
     */
    // Template#: 444, Serial#: 897
    public void nop() {
        assemble0040((byte) 0x90);
    }

    /**
     * Pseudo-external assembler syntax: {@code notq  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code notq      [rbx + 18]}
     */
    // Template#: 445, Serial#: 1887
    public void notq(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0103((byte) 0xF7, (byte) 0x02, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code notq  }<i>destination</i>
     * Example disassembly syntax: {@code notq      rax}
     */
    // Template#: 446, Serial#: 1914
    public void notq(AMD64GeneralRegister64 destination) {
        assemble0102((byte) 0xF7, (byte) 0x02, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code notq  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code notq      [rbx + 305419896]}
     */
    // Template#: 447, Serial#: 1901
    public void notq(int disp32, AMD64IndirectRegister64 destination) {
        assemble0105((byte) 0xF7, (byte) 0x02, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code orl  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code orl       [rbx + 18], 0x12}
     */
    // Template#: 448, Serial#: 643
    public void orl(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0001((byte) 0x83, (byte) 0x01, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code orq  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code orq       [rbx + 18], 0x12}
     */
    // Template#: 449, Serial#: 715
    public void orq(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0002((byte) 0x83, (byte) 0x01, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code or        [rbx + 18], eax}
     */
    // Template#: 450, Serial#: 1996
    public void or(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0003((byte) 0x09, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code or        [rbx + 18], rax}
     */
    // Template#: 451, Serial#: 2005
    public void or(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0004((byte) 0x09, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code orl  }<i>disp8</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code orl       [rbx + 18], 0x12345678}
     */
    // Template#: 452, Serial#: 427
    public void orl(byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        assemble0005((byte) 0x81, (byte) 0x01, disp8, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code orq  }<i>disp8</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code orq       [rbx + 18], 0x12345678}
     */
    // Template#: 453, Serial#: 499
    public void orq(byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        assemble0006((byte) 0x81, (byte) 0x01, disp8, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code orl  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code orl       eax, 0x12}
     */
    // Template#: 454, Serial#: 674
    public void orl(AMD64GeneralRegister32 destination, byte imm8) {
        assemble0007((byte) 0x83, (byte) 0x01, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code or        eax, [rbx + 18]}
     */
    // Template#: 455, Serial#: 2031
    public void or(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0008((byte) 0x0B, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code or        eax, eax}
     */
    // Template#: 456, Serial#: 2000
    public void or(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0009((byte) 0x09, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code orl  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code orl       eax, 0x12345678}
     */
    // Template#: 457, Serial#: 458
    public void orl(AMD64GeneralRegister32 destination, int imm32) {
        assemble0011((byte) 0x81, (byte) 0x01, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code or        eax, [L1: +305419896]}
     */
    // Template#: 458, Serial#: 2030
    public void rip_or(AMD64GeneralRegister32 destination, int rel32) {
        assemble0010((byte) 0x0B, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code or        eax, [rbx + 305419896]}
     */
    // Template#: 459, Serial#: 2033
    public void or(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0012((byte) 0x0B, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code orq  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code orq       rax, 0x12}
     */
    // Template#: 460, Serial#: 746
    public void orq(AMD64GeneralRegister64 destination, byte imm8) {
        assemble0013((byte) 0x83, (byte) 0x01, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code or        rax, [rbx + 18]}
     */
    // Template#: 461, Serial#: 2039
    public void or(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0014((byte) 0x0B, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code or        rax, rax}
     */
    // Template#: 462, Serial#: 2009
    public void or(AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0015((byte) 0x09, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code orq  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code orq       rax, 0x12345678}
     */
    // Template#: 463, Serial#: 530
    public void orq(AMD64GeneralRegister64 destination, int imm32) {
        assemble0017((byte) 0x81, (byte) 0x01, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code or        rax, [L1: +305419896]}
     */
    // Template#: 464, Serial#: 2038
    public void rip_or(AMD64GeneralRegister64 destination, int rel32) {
        assemble0016((byte) 0x0B, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code or        rax, [rbx + 305419896]}
     */
    // Template#: 465, Serial#: 2041
    public void or(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0018((byte) 0x0B, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>imm32</i>
     * Example disassembly syntax: {@code or        eax, 0x12345678}
     */
    // Template#: 466, Serial#: 2052
    public void or_EAX(int imm32) {
        assemble0019((byte) 0x0D, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>imm32</i>
     * Example disassembly syntax: {@code or        rax, 0x12345678}
     */
    // Template#: 467, Serial#: 2053
    public void or_RAX(int imm32) {
        assemble0020((byte) 0x0D, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code orl  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code orl       [rbx + 305419896], 0x12}
     */
    // Template#: 468, Serial#: 659
    public void orl(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0021((byte) 0x83, (byte) 0x01, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code orq  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code orq       [rbx + 305419896], 0x12}
     */
    // Template#: 469, Serial#: 731
    public void orq(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0022((byte) 0x83, (byte) 0x01, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code or        [rbx + 305419896], eax}
     */
    // Template#: 470, Serial#: 1998
    public void or(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0023((byte) 0x09, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code or        [rbx + 305419896], rax}
     */
    // Template#: 471, Serial#: 2007
    public void or(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0024((byte) 0x09, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code orl  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code orl       [rbx + 305419896], 0x12345678}
     */
    // Template#: 472, Serial#: 443
    public void orl(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0025((byte) 0x81, (byte) 0x01, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code orq  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code orq       [rbx + 305419896], 0x12345678}
     */
    // Template#: 473, Serial#: 515
    public void orq(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0026((byte) 0x81, (byte) 0x01, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code pop  }<i>register</i>
     * Example disassembly syntax: {@code pop       rax}
     */
    // Template#: 474, Serial#: 2273
    public void pop(AMD64GeneralRegister64 register) {
        assemble0207((byte) 0x58, register);
    }

    /**
     * Pseudo-external assembler syntax: {@code push  }<i>register</i>
     * Example disassembly syntax: {@code push      rax}
     */
    // Template#: 475, Serial#: 289
    public void push(AMD64GeneralRegister64 register) {
        assemble0207((byte) 0x50, register);
    }

    /**
     * Pseudo-external assembler syntax: {@code push  }<i>imm32</i>
     * Example disassembly syntax: {@code push      0x12345678}
     */
    // Template#: 476, Serial#: 2275
    public void push(int imm32) {
        assemble0019((byte) 0x68, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code push  }<i>rel32</i>
     * Example disassembly syntax: {@code push      [L1: +305419896]}
     */
    // Template#: 477, Serial#: 3057
    public void rip_push(int rel32) {
        assemble0038((byte) 0xFF, (byte) 0x06, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code ret  }
     * Example disassembly syntax: {@code ret     }
     */
    // Template#: 478, Serial#: 1171
    public void ret() {
        assemble0040((byte) 0xC3);
    }

    /**
     * Pseudo-external assembler syntax: {@code ret  }<i>imm16</i>
     * Example disassembly syntax: {@code ret       0x1234}
     */
    // Template#: 479, Serial#: 1170
    public void ret(short imm16) {
        assemble0208((byte) 0xC2, imm16);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarl  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code sarl      [rbx + 18], 0x1}
     */
    // Template#: 480, Serial#: 1311
    public void sarl___1(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0035((byte) 0xD1, (byte) 0x07, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarq  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code sarq      [rbx + 18], 0x1}
     */
    // Template#: 481, Serial#: 1374
    public void sarq___1(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0103((byte) 0xD1, (byte) 0x07, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarl  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code sarl      [rbx + 18], cl}
     */
    // Template#: 482, Serial#: 1563
    public void sarl___CL(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0035((byte) 0xD3, (byte) 0x07, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarq  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code sarq      [rbx + 18], cl}
     */
    // Template#: 483, Serial#: 1626
    public void sarq___CL(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0103((byte) 0xD3, (byte) 0x07, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarl  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code sarl      [rbx + 18], 0x12}
     */
    // Template#: 484, Serial#: 1021
    public void sarl(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0001((byte) 0xC1, (byte) 0x07, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarq  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code sarq      [rbx + 18], 0x12}
     */
    // Template#: 485, Serial#: 1084
    public void sarq(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0002((byte) 0xC1, (byte) 0x07, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarl  }<i>destination</i>
     * Example disassembly syntax: {@code sarl      eax, 0x1}
     */
    // Template#: 486, Serial#: 1333
    public void sarl___1(AMD64GeneralRegister32 destination) {
        assemble0107((byte) 0xD1, (byte) 0x07, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarl  }<i>destination</i>
     * Example disassembly syntax: {@code sarl      eax, cl}
     */
    // Template#: 487, Serial#: 1585
    public void sarl___CL(AMD64GeneralRegister32 destination) {
        assemble0107((byte) 0xD3, (byte) 0x07, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarl  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code sarl      eax, 0x12}
     */
    // Template#: 488, Serial#: 1043
    public void sarl(AMD64GeneralRegister32 destination, byte imm8) {
        assemble0007((byte) 0xC1, (byte) 0x07, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarq  }<i>destination</i>
     * Example disassembly syntax: {@code sarq      rax, 0x1}
     */
    // Template#: 489, Serial#: 1396
    public void sarq___1(AMD64GeneralRegister64 destination) {
        assemble0102((byte) 0xD1, (byte) 0x07, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarq  }<i>destination</i>
     * Example disassembly syntax: {@code sarq      rax, cl}
     */
    // Template#: 490, Serial#: 1648
    public void sarq___CL(AMD64GeneralRegister64 destination) {
        assemble0102((byte) 0xD3, (byte) 0x07, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarq  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code sarq      rax, 0x12}
     */
    // Template#: 491, Serial#: 1106
    public void sarq(AMD64GeneralRegister64 destination, byte imm8) {
        assemble0013((byte) 0xC1, (byte) 0x07, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarl  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code sarl      [rbx + 305419896], 0x1}
     */
    // Template#: 492, Serial#: 1325
    public void sarl___1(int disp32, AMD64IndirectRegister64 destination) {
        assemble0039((byte) 0xD1, (byte) 0x07, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarq  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code sarq      [rbx + 305419896], 0x1}
     */
    // Template#: 493, Serial#: 1388
    public void sarq___1(int disp32, AMD64IndirectRegister64 destination) {
        assemble0105((byte) 0xD1, (byte) 0x07, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarl  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code sarl      [rbx + 305419896], cl}
     */
    // Template#: 494, Serial#: 1577
    public void sarl___CL(int disp32, AMD64IndirectRegister64 destination) {
        assemble0039((byte) 0xD3, (byte) 0x07, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarq  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code sarq      [rbx + 305419896], cl}
     */
    // Template#: 495, Serial#: 1640
    public void sarq___CL(int disp32, AMD64IndirectRegister64 destination) {
        assemble0105((byte) 0xD3, (byte) 0x07, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarl  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code sarl      [rbx + 305419896], 0x12}
     */
    // Template#: 496, Serial#: 1035
    public void sarl(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0021((byte) 0xC1, (byte) 0x07, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarq  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code sarq      [rbx + 305419896], 0x12}
     */
    // Template#: 497, Serial#: 1098
    public void sarq(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0022((byte) 0xC1, (byte) 0x07, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code setb  }<i>destination</i>
     * Example disassembly syntax: {@code setb      al}
     */
    // Template#: 498, Serial#: 4088
    public void setb(AMD64GeneralRegister8 destination) {
        assemble0209((byte) 0x92, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code setbe  }<i>destination</i>
     * Example disassembly syntax: {@code setbe     al}
     */
    // Template#: 499, Serial#: 4124
    public void setbe(AMD64GeneralRegister8 destination) {
        assemble0209((byte) 0x96, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code setl  }<i>destination</i>
     * Example disassembly syntax: {@code setl      al}
     */
    // Template#: 500, Serial#: 5945
    public void setl(AMD64GeneralRegister8 destination) {
        assemble0209((byte) 0x9C, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code setnb  }<i>destination</i>
     * Example disassembly syntax: {@code setnb     al}
     */
    // Template#: 501, Serial#: 4097
    public void setnb(AMD64GeneralRegister8 destination) {
        assemble0209((byte) 0x93, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code setnbe  }<i>destination</i>
     * Example disassembly syntax: {@code setnbe    al}
     */
    // Template#: 502, Serial#: 4133
    public void setnbe(AMD64GeneralRegister8 destination) {
        assemble0209((byte) 0x97, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code setnl  }<i>destination</i>
     * Example disassembly syntax: {@code setnl     al}
     */
    // Template#: 503, Serial#: 5954
    public void setnl(AMD64GeneralRegister8 destination) {
        assemble0209((byte) 0x9D, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code setnle  }<i>destination</i>
     * Example disassembly syntax: {@code setnle    al}
     */
    // Template#: 504, Serial#: 5972
    public void setnle(AMD64GeneralRegister8 destination) {
        assemble0209((byte) 0x9F, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code setnp  }<i>destination</i>
     * Example disassembly syntax: {@code setnp     al}
     */
    // Template#: 505, Serial#: 5936
    public void setnp(AMD64GeneralRegister8 destination) {
        assemble0209((byte) 0x9B, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code setp  }<i>destination</i>
     * Example disassembly syntax: {@code setp      al}
     */
    // Template#: 506, Serial#: 5927
    public void setp(AMD64GeneralRegister8 destination) {
        assemble0209((byte) 0x9A, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code sfence  }
     * Example disassembly syntax: {@code sfence  }
     */
    // Template#: 507, Serial#: 6099
    public void sfence() {
        assemble0118((byte) 0xAE, (byte) 0x07);
    }

    /**
     * Pseudo-external assembler syntax: {@code shll  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code shll      [rbx + 18], 0x1}
     */
    // Template#: 508, Serial#: 1307
    public void shll___1(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0035((byte) 0xD1, (byte) 0x04, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlq  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code shlq      [rbx + 18], 0x1}
     */
    // Template#: 509, Serial#: 1370
    public void shlq___1(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0103((byte) 0xD1, (byte) 0x04, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shll  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code shll      [rbx + 18], cl}
     */
    // Template#: 510, Serial#: 1559
    public void shll___CL(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0035((byte) 0xD3, (byte) 0x04, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlq  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code shlq      [rbx + 18], cl}
     */
    // Template#: 511, Serial#: 1622
    public void shlq___CL(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0103((byte) 0xD3, (byte) 0x04, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shll  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shll      [rbx + 18], 0x12}
     */
    // Template#: 512, Serial#: 1017
    public void shll(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0001((byte) 0xC1, (byte) 0x04, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlq  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shlq      [rbx + 18], 0x12}
     */
    // Template#: 513, Serial#: 1080
    public void shlq(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0002((byte) 0xC1, (byte) 0x04, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code shll  }<i>destination</i>
     * Example disassembly syntax: {@code shll      eax, 0x1}
     */
    // Template#: 514, Serial#: 1331
    public void shll___1(AMD64GeneralRegister32 destination) {
        assemble0107((byte) 0xD1, (byte) 0x04, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shll  }<i>destination</i>
     * Example disassembly syntax: {@code shll      eax, cl}
     */
    // Template#: 515, Serial#: 1583
    public void shll___CL(AMD64GeneralRegister32 destination) {
        assemble0107((byte) 0xD3, (byte) 0x04, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shll  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shll      eax, 0x12}
     */
    // Template#: 516, Serial#: 1041
    public void shll(AMD64GeneralRegister32 destination, byte imm8) {
        assemble0007((byte) 0xC1, (byte) 0x04, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlq  }<i>destination</i>
     * Example disassembly syntax: {@code shlq      rax, 0x1}
     */
    // Template#: 517, Serial#: 1394
    public void shlq___1(AMD64GeneralRegister64 destination) {
        assemble0102((byte) 0xD1, (byte) 0x04, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlq  }<i>destination</i>
     * Example disassembly syntax: {@code shlq      rax, cl}
     */
    // Template#: 518, Serial#: 1646
    public void shlq___CL(AMD64GeneralRegister64 destination) {
        assemble0102((byte) 0xD3, (byte) 0x04, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlq  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shlq      rax, 0x12}
     */
    // Template#: 519, Serial#: 1104
    public void shlq(AMD64GeneralRegister64 destination, byte imm8) {
        assemble0013((byte) 0xC1, (byte) 0x04, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code shll  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code shll      [rbx + 305419896], 0x1}
     */
    // Template#: 520, Serial#: 1321
    public void shll___1(int disp32, AMD64IndirectRegister64 destination) {
        assemble0039((byte) 0xD1, (byte) 0x04, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlq  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code shlq      [rbx + 305419896], 0x1}
     */
    // Template#: 521, Serial#: 1384
    public void shlq___1(int disp32, AMD64IndirectRegister64 destination) {
        assemble0105((byte) 0xD1, (byte) 0x04, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shll  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code shll      [rbx + 305419896], cl}
     */
    // Template#: 522, Serial#: 1573
    public void shll___CL(int disp32, AMD64IndirectRegister64 destination) {
        assemble0039((byte) 0xD3, (byte) 0x04, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlq  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code shlq      [rbx + 305419896], cl}
     */
    // Template#: 523, Serial#: 1636
    public void shlq___CL(int disp32, AMD64IndirectRegister64 destination) {
        assemble0105((byte) 0xD3, (byte) 0x04, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shll  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shll      [rbx + 305419896], 0x12}
     */
    // Template#: 524, Serial#: 1031
    public void shll(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0021((byte) 0xC1, (byte) 0x04, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlq  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shlq      [rbx + 305419896], 0x12}
     */
    // Template#: 525, Serial#: 1094
    public void shlq(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0022((byte) 0xC1, (byte) 0x04, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrl  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code shrl      [rbx + 18], 0x1}
     */
    // Template#: 526, Serial#: 1309
    public void shrl___1(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0035((byte) 0xD1, (byte) 0x05, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrq  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code shrq      [rbx + 18], 0x1}
     */
    // Template#: 527, Serial#: 1372
    public void shrq___1(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0103((byte) 0xD1, (byte) 0x05, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrl  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code shrl      [rbx + 18], cl}
     */
    // Template#: 528, Serial#: 1561
    public void shrl___CL(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0035((byte) 0xD3, (byte) 0x05, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrq  }<i>disp8</i>, <i>destination</i>
     * Example disassembly syntax: {@code shrq      [rbx + 18], cl}
     */
    // Template#: 529, Serial#: 1624
    public void shrq___CL(byte disp8, AMD64IndirectRegister64 destination) {
        assemble0103((byte) 0xD3, (byte) 0x05, disp8, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrl  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shrl      [rbx + 18], 0x12}
     */
    // Template#: 530, Serial#: 1019
    public void shrl(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0001((byte) 0xC1, (byte) 0x05, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrq  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shrq      [rbx + 18], 0x12}
     */
    // Template#: 531, Serial#: 1082
    public void shrq(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0002((byte) 0xC1, (byte) 0x05, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrl  }<i>destination</i>
     * Example disassembly syntax: {@code shrl      eax, 0x1}
     */
    // Template#: 532, Serial#: 1332
    public void shrl___1(AMD64GeneralRegister32 destination) {
        assemble0107((byte) 0xD1, (byte) 0x05, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrl  }<i>destination</i>
     * Example disassembly syntax: {@code shrl      eax, cl}
     */
    // Template#: 533, Serial#: 1584
    public void shrl___CL(AMD64GeneralRegister32 destination) {
        assemble0107((byte) 0xD3, (byte) 0x05, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrl  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shrl      eax, 0x12}
     */
    // Template#: 534, Serial#: 1042
    public void shrl(AMD64GeneralRegister32 destination, byte imm8) {
        assemble0007((byte) 0xC1, (byte) 0x05, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrq  }<i>destination</i>
     * Example disassembly syntax: {@code shrq      rax, 0x1}
     */
    // Template#: 535, Serial#: 1395
    public void shrq___1(AMD64GeneralRegister64 destination) {
        assemble0102((byte) 0xD1, (byte) 0x05, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrq  }<i>destination</i>
     * Example disassembly syntax: {@code shrq      rax, cl}
     */
    // Template#: 536, Serial#: 1647
    public void shrq___CL(AMD64GeneralRegister64 destination) {
        assemble0102((byte) 0xD3, (byte) 0x05, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrq  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shrq      rax, 0x12}
     */
    // Template#: 537, Serial#: 1105
    public void shrq(AMD64GeneralRegister64 destination, byte imm8) {
        assemble0013((byte) 0xC1, (byte) 0x05, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrl  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code shrl      [rbx + 305419896], 0x1}
     */
    // Template#: 538, Serial#: 1323
    public void shrl___1(int disp32, AMD64IndirectRegister64 destination) {
        assemble0039((byte) 0xD1, (byte) 0x05, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrq  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code shrq      [rbx + 305419896], 0x1}
     */
    // Template#: 539, Serial#: 1386
    public void shrq___1(int disp32, AMD64IndirectRegister64 destination) {
        assemble0105((byte) 0xD1, (byte) 0x05, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrl  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code shrl      [rbx + 305419896], cl}
     */
    // Template#: 540, Serial#: 1575
    public void shrl___CL(int disp32, AMD64IndirectRegister64 destination) {
        assemble0039((byte) 0xD3, (byte) 0x05, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrq  }<i>disp32</i>, <i>destination</i>
     * Example disassembly syntax: {@code shrq      [rbx + 305419896], cl}
     */
    // Template#: 541, Serial#: 1638
    public void shrq___CL(int disp32, AMD64IndirectRegister64 destination) {
        assemble0105((byte) 0xD3, (byte) 0x05, disp32, destination);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrl  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shrl      [rbx + 305419896], 0x12}
     */
    // Template#: 542, Serial#: 1033
    public void shrl(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0021((byte) 0xC1, (byte) 0x05, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrq  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shrq      [rbx + 305419896], 0x12}
     */
    // Template#: 543, Serial#: 1096
    public void shrq(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0022((byte) 0xC1, (byte) 0x05, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code subl  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code subl      [rbx + 18], 0x12}
     */
    // Template#: 544, Serial#: 651
    public void subl(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0001((byte) 0x83, (byte) 0x05, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code subq  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code subq      [rbx + 18], 0x12}
     */
    // Template#: 545, Serial#: 723
    public void subq(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0002((byte) 0x83, (byte) 0x05, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code sub       [rbx + 18], eax}
     */
    // Template#: 546, Serial#: 2140
    public void sub(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0003((byte) 0x29, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code sub       [rbx + 18], rax}
     */
    // Template#: 547, Serial#: 2149
    public void sub(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0004((byte) 0x29, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code subl  }<i>disp8</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code subl      [rbx + 18], 0x12345678}
     */
    // Template#: 548, Serial#: 435
    public void subl(byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        assemble0005((byte) 0x81, (byte) 0x05, disp8, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code subq  }<i>disp8</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code subq      [rbx + 18], 0x12345678}
     */
    // Template#: 549, Serial#: 507
    public void subq(byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        assemble0006((byte) 0x81, (byte) 0x05, disp8, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code subl  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code subl      eax, 0x12}
     */
    // Template#: 550, Serial#: 678
    public void subl(AMD64GeneralRegister32 destination, byte imm8) {
        assemble0007((byte) 0x83, (byte) 0x05, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code sub       eax, [rbx + 18]}
     */
    // Template#: 551, Serial#: 2175
    public void sub(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0008((byte) 0x2B, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code sub       eax, eax}
     */
    // Template#: 552, Serial#: 2144
    public void sub(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0009((byte) 0x29, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code subl  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code subl      eax, 0x12345678}
     */
    // Template#: 553, Serial#: 462
    public void subl(AMD64GeneralRegister32 destination, int imm32) {
        assemble0011((byte) 0x81, (byte) 0x05, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code sub       eax, [L1: +305419896]}
     */
    // Template#: 554, Serial#: 2174
    public void rip_sub(AMD64GeneralRegister32 destination, int rel32) {
        assemble0010((byte) 0x2B, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code sub       eax, [rbx + 305419896]}
     */
    // Template#: 555, Serial#: 2177
    public void sub(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0012((byte) 0x2B, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code subq  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code subq      rax, 0x12}
     */
    // Template#: 556, Serial#: 750
    public void subq(AMD64GeneralRegister64 destination, byte imm8) {
        assemble0013((byte) 0x83, (byte) 0x05, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code sub       rax, [rbx + 18]}
     */
    // Template#: 557, Serial#: 2183
    public void sub(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0014((byte) 0x2B, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code sub       rax, rax}
     */
    // Template#: 558, Serial#: 2153
    public void sub(AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0015((byte) 0x29, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code subq  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code subq      rax, 0x12345678}
     */
    // Template#: 559, Serial#: 534
    public void subq(AMD64GeneralRegister64 destination, int imm32) {
        assemble0017((byte) 0x81, (byte) 0x05, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code sub       rax, [L1: +305419896]}
     */
    // Template#: 560, Serial#: 2182
    public void rip_sub(AMD64GeneralRegister64 destination, int rel32) {
        assemble0016((byte) 0x2B, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code sub       rax, [rbx + 305419896]}
     */
    // Template#: 561, Serial#: 2185
    public void sub(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0018((byte) 0x2B, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>imm32</i>
     * Example disassembly syntax: {@code sub       eax, 0x12345678}
     */
    // Template#: 562, Serial#: 2196
    public void sub_EAX(int imm32) {
        assemble0019((byte) 0x2D, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>imm32</i>
     * Example disassembly syntax: {@code sub       rax, 0x12345678}
     */
    // Template#: 563, Serial#: 2197
    public void sub_RAX(int imm32) {
        assemble0020((byte) 0x2D, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code subl  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code subl      [rbx + 305419896], 0x12}
     */
    // Template#: 564, Serial#: 667
    public void subl(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0021((byte) 0x83, (byte) 0x05, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code subq  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code subq      [rbx + 305419896], 0x12}
     */
    // Template#: 565, Serial#: 739
    public void subq(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0022((byte) 0x83, (byte) 0x05, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code sub       [rbx + 305419896], eax}
     */
    // Template#: 566, Serial#: 2142
    public void sub(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0023((byte) 0x29, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code sub       [rbx + 305419896], rax}
     */
    // Template#: 567, Serial#: 2151
    public void sub(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0024((byte) 0x29, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code subl  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code subl      [rbx + 305419896], 0x12345678}
     */
    // Template#: 568, Serial#: 451
    public void subl(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0025((byte) 0x81, (byte) 0x05, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code subq  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code subq      [rbx + 305419896], 0x12345678}
     */
    // Template#: 569, Serial#: 523
    public void subq(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0026((byte) 0x81, (byte) 0x05, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code subsd  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code subsd     xmm0, [rbx + 18]}
     */
    // Template#: 570, Serial#: 5532
    public void subsd(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0027((byte) 0x5C, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code subsd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code subsd     xmm0, xmm0}
     */
    // Template#: 571, Serial#: 5536
    public void subsd(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0028((byte) 0x5C, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code subsd  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code subsd     xmm0, [L1: +305419896]}
     */
    // Template#: 572, Serial#: 5531
    public void rip_subsd(AMD64XMMRegister destination, int rel32) {
        assemble0029((byte) 0x5C, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code subsd  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code subsd     xmm0, [rbx + 305419896]}
     */
    // Template#: 573, Serial#: 5534
    public void subsd(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0030((byte) 0x5C, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code subss  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code subss     xmm0, [rbx + 18]}
     */
    // Template#: 574, Serial#: 5604
    public void subss(AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0031((byte) 0x5C, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code subss  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code subss     xmm0, xmm0}
     */
    // Template#: 575, Serial#: 5608
    public void subss(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0032((byte) 0x5C, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code subss  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code subss     xmm0, [L1: +305419896]}
     */
    // Template#: 576, Serial#: 5603
    public void rip_subss(AMD64XMMRegister destination, int rel32) {
        assemble0033((byte) 0x5C, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code subss  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code subss     xmm0, [rbx + 305419896]}
     */
    // Template#: 577, Serial#: 5606
    public void subss(AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0034((byte) 0x5C, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code xchg  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code xchg      rax, rax}
     */
    // Template#: 578, Serial#: 887
    public void xchg(AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0015((byte) 0x87, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorl  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code xorl      [rbx + 18], 0x12}
     */
    // Template#: 579, Serial#: 653
    public void xorl(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0001((byte) 0x83, (byte) 0x06, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorq  }<i>disp8</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code xorq      [rbx + 18], 0x12}
     */
    // Template#: 580, Serial#: 725
    public void xorq(byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0002((byte) 0x83, (byte) 0x06, disp8, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code xor       [rbx + 18], eax}
     */
    // Template#: 581, Serial#: 230
    public void xor(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0003((byte) 0x31, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>disp8</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code xor       [rbx + 18], rax}
     */
    // Template#: 582, Serial#: 239
    public void xor(byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0004((byte) 0x31, disp8, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorl  }<i>disp8</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code xorl      [rbx + 18], 0x12345678}
     */
    // Template#: 583, Serial#: 437
    public void xorl(byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        assemble0005((byte) 0x81, (byte) 0x06, disp8, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorq  }<i>disp8</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code xorq      [rbx + 18], 0x12345678}
     */
    // Template#: 584, Serial#: 509
    public void xorq(byte disp8, AMD64IndirectRegister64 destination, int imm32) {
        assemble0006((byte) 0x81, (byte) 0x06, disp8, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorl  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code xorl      eax, 0x12}
     */
    // Template#: 585, Serial#: 679
    public void xorl(AMD64GeneralRegister32 destination, byte imm8) {
        assemble0007((byte) 0x83, (byte) 0x06, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code xor       eax, [rbx + 18]}
     */
    // Template#: 586, Serial#: 265
    public void xor(AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0008((byte) 0x33, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code xor       eax, eax}
     */
    // Template#: 587, Serial#: 234
    public void xor(AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
        assemble0009((byte) 0x31, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code xor       eax, [L1: +305419896]}
     */
    // Template#: 588, Serial#: 264
    public void rip_xor(AMD64GeneralRegister32 destination, int rel32) {
        assemble0010((byte) 0x33, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorl  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code xorl      eax, 0x12345678}
     */
    // Template#: 589, Serial#: 463
    public void xorl(AMD64GeneralRegister32 destination, int imm32) {
        assemble0011((byte) 0x81, (byte) 0x06, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code xor       eax, [rbx + 305419896]}
     */
    // Template#: 590, Serial#: 267
    public void xor(AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0012((byte) 0x33, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorq  }<i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code xorq      rax, 0x12}
     */
    // Template#: 591, Serial#: 751
    public void xorq(AMD64GeneralRegister64 destination, byte imm8) {
        assemble0013((byte) 0x83, (byte) 0x06, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>destination</i>, <i>disp8</i>, <i>source</i>
     * Example disassembly syntax: {@code xor       rax, [rbx + 18]}
     */
    // Template#: 592, Serial#: 273
    public void xor(AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
        assemble0014((byte) 0x33, destination, disp8, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code xor       rax, rax}
     */
    // Template#: 593, Serial#: 243
    public void xor(AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0015((byte) 0x31, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>destination</i>, <i>rel32</i>
     * Example disassembly syntax: {@code xor       rax, [L1: +305419896]}
     */
    // Template#: 594, Serial#: 272
    public void rip_xor(AMD64GeneralRegister64 destination, int rel32) {
        assemble0016((byte) 0x33, destination, rel32);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorq  }<i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code xorq      rax, 0x12345678}
     */
    // Template#: 595, Serial#: 535
    public void xorq(AMD64GeneralRegister64 destination, int imm32) {
        assemble0017((byte) 0x81, (byte) 0x06, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>destination</i>, <i>disp32</i>, <i>source</i>
     * Example disassembly syntax: {@code xor       rax, [rbx + 305419896]}
     */
    // Template#: 596, Serial#: 275
    public void xor(AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
        assemble0018((byte) 0x33, destination, disp32, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>imm32</i>
     * Example disassembly syntax: {@code xor       eax, 0x12345678}
     */
    // Template#: 597, Serial#: 286
    public void xor_EAX(int imm32) {
        assemble0019((byte) 0x35, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>imm32</i>
     * Example disassembly syntax: {@code xor       rax, 0x12345678}
     */
    // Template#: 598, Serial#: 287
    public void xor_RAX(int imm32) {
        assemble0020((byte) 0x35, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorl  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code xorl      [rbx + 305419896], 0x12}
     */
    // Template#: 599, Serial#: 669
    public void xorl(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0021((byte) 0x83, (byte) 0x06, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorq  }<i>disp32</i>, <i>destination</i>, <i>imm8</i>
     * Example disassembly syntax: {@code xorq      [rbx + 305419896], 0x12}
     */
    // Template#: 600, Serial#: 741
    public void xorq(int disp32, AMD64IndirectRegister64 destination, byte imm8) {
        assemble0022((byte) 0x83, (byte) 0x06, disp32, destination, imm8);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code xor       [rbx + 305419896], eax}
     */
    // Template#: 601, Serial#: 232
    public void xor(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        assemble0023((byte) 0x31, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>disp32</i>, <i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code xor       [rbx + 305419896], rax}
     */
    // Template#: 602, Serial#: 241
    public void xor(int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        assemble0024((byte) 0x31, disp32, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorl  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code xorl      [rbx + 305419896], 0x12345678}
     */
    // Template#: 603, Serial#: 453
    public void xorl(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0025((byte) 0x81, (byte) 0x06, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorq  }<i>disp32</i>, <i>destination</i>, <i>imm32</i>
     * Example disassembly syntax: {@code xorq      [rbx + 305419896], 0x12345678}
     */
    // Template#: 604, Serial#: 525
    public void xorq(int disp32, AMD64IndirectRegister64 destination, int imm32) {
        assemble0026((byte) 0x81, (byte) 0x06, disp32, destination, imm32);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorpd  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code xorpd     xmm0, xmm0}
     */
    // Template#: 605, Serial#: 3764
    public void xorpd(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0068((byte) 0x57, destination, source);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorps  }<i>destination</i>, <i>source</i>
     * Example disassembly syntax: {@code xorps     xmm0, xmm0}
     */
    // Template#: 606, Serial#: 3718
    public void xorps(AMD64XMMRegister destination, AMD64XMMRegister source) {
        assemble0072((byte) 0x57, destination, source);
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

    private void assemble0035(byte opcode1, byte modRmOpcode, byte disp8, AMD64IndirectRegister64 destination) {
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

    private void assemble0036(byte opcode1, byte modRmOpcode, AMD64GeneralRegister64 destination) {
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

    private void assemble0037(byte opcode1, int rel32) {
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

    private void assemble0038(byte opcode1, byte modRmOpcode, int rel32) {
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

    private void assemble0039(byte opcode1, byte modRmOpcode, int disp32, AMD64IndirectRegister64 destination) {
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

    private void assemble0040(byte opcode1) {
        emitByte(opcode1);
    }

    private void assemble0041(byte opcode2, AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
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

    private void assemble0042(byte opcode2, AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source) {
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

    private void assemble0043(byte opcode2, AMD64GeneralRegister32 destination, int rel32) {
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

    private void assemble0044(byte opcode2, AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
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

    private void assemble0045(byte opcode2, AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
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

    private void assemble0046(byte opcode2, AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        if (source == AMD64IndirectRegister64.RBP_INDIRECT || source == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0045(opcode2, destination, (byte) 0, source);
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

    private void assemble0047(byte opcode1, AMD64GeneralRegister32 destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
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

    private void assemble0048(byte opcode1, AMD64GeneralRegister32 destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0047(opcode1, destination, (byte) 0, base, index, scale);
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

    private void assemble0049(byte opcode2, AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source, AMD64XMMComparison amd64xmmcomparison) {
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

    private void assemble0050(byte opcode2, AMD64XMMRegister destination, AMD64XMMRegister source, AMD64XMMComparison amd64xmmcomparison) {
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

    private void assemble0051(byte opcode2, AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source, AMD64XMMComparison amd64xmmcomparison) {
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

    private void assemble0052(byte opcode2, AMD64XMMRegister destination, int rel32, AMD64XMMComparison amd64xmmcomparison) {
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

    private void assemble0053(byte opcode2, AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source, AMD64XMMComparison amd64xmmcomparison) {
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

    private void assemble0054(byte opcode2, AMD64XMMRegister destination, AMD64XMMRegister source, AMD64XMMComparison amd64xmmcomparison) {
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

    private void assemble0055(byte opcode2, AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source, AMD64XMMComparison amd64xmmcomparison) {
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

    private void assemble0056(byte opcode2, AMD64XMMRegister destination, int rel32, AMD64XMMComparison amd64xmmcomparison) {
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

    private void assemble0057(byte opcode2, byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
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

    private void assemble0058(byte opcode2, byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
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

    private void assemble0059(byte opcode2, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister32 source) {
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

    private void assemble0060(byte opcode2, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister32 source) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0059(opcode2, (byte) 0, base, index, scale, source);
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

    private void assemble0061(byte opcode2, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister64 source) {
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

    private void assemble0062(byte opcode2, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister64 source) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0061(opcode2, (byte) 0, base, index, scale, source);
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

    private void assemble0063(byte opcode2, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
        if (destination == AMD64IndirectRegister64.RBP_INDIRECT || destination == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0057(opcode2, (byte) 0, destination, source);
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

    private void assemble0064(byte opcode2, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
        if (destination == AMD64IndirectRegister64.RBP_INDIRECT || destination == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0058(opcode2, (byte) 0, destination, source);
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

    private void assemble0065(byte opcode2, int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
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

    private void assemble0066(byte opcode2, int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
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

    private void assemble0067(byte opcode2, AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
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

    private void assemble0068(byte opcode2, AMD64XMMRegister destination, AMD64XMMRegister source) {
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

    private void assemble0069(byte opcode2, AMD64XMMRegister destination, int rel32) {
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

    private void assemble0070(byte opcode2, AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
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

    private void assemble0071(byte opcode2, AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
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

    private void assemble0072(byte opcode2, AMD64XMMRegister destination, AMD64XMMRegister source) {
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

    private void assemble0073(byte opcode2, AMD64XMMRegister destination, int rel32) {
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

    private void assemble0074(byte opcode2, AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
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

    private void assemble0075(byte opcode1) {
        byte rex = (byte) 0x48;
        emitByte(rex);
        emitByte(opcode1);
    }

    private void assemble0076(byte opcode2, AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
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

    private void assemble0077(byte opcode2, AMD64GeneralRegister32 destination, AMD64XMMRegister source) {
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

    private void assemble0078(byte opcode2, AMD64GeneralRegister32 destination, int rel32) {
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

    private void assemble0079(byte opcode2, AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
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

    private void assemble0080(byte opcode2, AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
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

    private void assemble0081(byte opcode2, AMD64GeneralRegister64 destination, AMD64XMMRegister source) {
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

    private void assemble0082(byte opcode2, AMD64GeneralRegister64 destination, int rel32) {
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

    private void assemble0083(byte opcode2, AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
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

    private void assemble0084(byte opcode2, AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
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

    private void assemble0085(byte opcode2, AMD64XMMRegister destination, AMD64GeneralRegister32 source) {
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

    private void assemble0086(byte opcode2, AMD64XMMRegister destination, AMD64GeneralRegister64 source) {
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

    private void assemble0087(byte opcode2, AMD64XMMRegister destination, int rel32) {
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

    private void assemble0088(byte opcode2, AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
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

    private void assemble0089(byte opcode2, AMD64XMMRegister destination, byte disp8, AMD64IndirectRegister64 source) {
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

    private void assemble0090(byte opcode2, AMD64XMMRegister destination, AMD64GeneralRegister32 source) {
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

    private void assemble0091(byte opcode2, AMD64XMMRegister destination, AMD64GeneralRegister64 source) {
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

    private void assemble0092(byte opcode2, AMD64XMMRegister destination, int rel32) {
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

    private void assemble0093(byte opcode2, AMD64XMMRegister destination, int disp32, AMD64IndirectRegister64 source) {
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

    private void assemble0094(byte opcode2, AMD64GeneralRegister32 destination, byte disp8, AMD64IndirectRegister64 source) {
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

    private void assemble0095(byte opcode2, AMD64GeneralRegister32 destination, AMD64XMMRegister source) {
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

    private void assemble0096(byte opcode2, AMD64GeneralRegister32 destination, int rel32) {
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

    private void assemble0097(byte opcode2, AMD64GeneralRegister32 destination, int disp32, AMD64IndirectRegister64 source) {
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

    private void assemble0098(byte opcode2, AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
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

    private void assemble0099(byte opcode2, AMD64GeneralRegister64 destination, AMD64XMMRegister source) {
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

    private void assemble0100(byte opcode2, AMD64GeneralRegister64 destination, int rel32) {
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

    private void assemble0101(byte opcode2, AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
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

    private void assemble0102(byte opcode1, byte modRmOpcode, AMD64GeneralRegister64 destination) {
        byte rex = (byte) 0x48;
        rex |= (destination.value() & 8) >> 3; // rm field extension by REX.B bit
        emitByte(rex);
        emitByte(opcode1);
        byte modRMByte = (byte) ((3 << 6) | (modRmOpcode << 3)); // mod field, group opcode in reg field
        modRMByte |= (destination.value() & 7) << 0; // rm field
        emitByte(modRMByte);
    }

    private void assemble0103(byte opcode1, byte modRmOpcode, byte disp8, AMD64IndirectRegister64 destination) {
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

    private void assemble0104(byte opcode1, byte modRmOpcode, int rel32) {
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

    private void assemble0105(byte opcode1, byte modRmOpcode, int disp32, AMD64IndirectRegister64 destination) {
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

    private void assemble0106(byte opcode1, short imm16, byte imm8) {
        emitByte(opcode1);
        // appended:
        emitByte((byte) (imm16 & 0xff));
        imm16 >>= 8;
        emitByte((byte) (imm16 & 0xff));
        emitByte(imm8); // appended
    }

    private void assemble0107(byte opcode1, byte modRmOpcode, AMD64GeneralRegister32 destination) {
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

    private void assemble0108(byte opcode1, AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source, byte imm8) {
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

    private void assemble0109(byte opcode1, AMD64GeneralRegister32 destination, AMD64GeneralRegister32 source, int imm32) {
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

    private void assemble0110(byte opcode2, AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source) {
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

    private void assemble0111(byte opcode1, AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source, byte imm8) {
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

    private void assemble0112(byte opcode1, AMD64GeneralRegister64 destination, AMD64GeneralRegister64 source, int imm32) {
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

    private void assemble0113(byte opcode2, AMD64GeneralRegister64 destination, int rel32) {
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

    private void assemble0114(byte opcode2, AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
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

    private void assemble0115(byte opcode1, byte rel8) {
        emitByte(opcode1);
        emitByte(rel8); // appended
    }

    private void assemble0116(byte opcode2, int rel32) {
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

    private void assemble0117(byte opcode1, AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
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

    private void assemble0118(byte opcode2, byte modRmOpcode) {
        emitByte((byte) (0x0F)); // opcode1
        emitByte(opcode2);
        byte modRMByte = (byte) ((3 << 6) | (modRmOpcode << 3)); // mod field, group opcode in reg field
        emitByte(modRMByte);
    }

    private void assemble0119(byte opcode1, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, byte imm8) {
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

    private void assemble0120(byte opcode1, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister16 source) {
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

    private void assemble0121(byte opcode1, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister32 source) {
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

    private void assemble0122(byte opcode1, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister64 source) {
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

    private void assemble0123(byte opcode1, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister8 source) {
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

    private void assemble0124(byte opcode1, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, int imm32) {
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

    private void assemble0125(byte opcode1, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, short imm16) {
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

    private void assemble0126(byte opcode1, byte disp8, AMD64IndirectRegister64 destination, byte imm8) {
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

    private void assemble0127(byte opcode1, byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister16 source) {
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

    private void assemble0128(byte opcode1, byte disp8, AMD64IndirectRegister64 destination, AMD64GeneralRegister8 source) {
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

    private void assemble0129(byte opcode1, byte disp8, AMD64IndirectRegister64 destination, int imm32) {
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

    private void assemble0130(byte opcode1, byte disp8, AMD64IndirectRegister64 destination, short imm16) {
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

    private void assemble0131(byte opcode1, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, byte imm8) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0119(opcode1, (byte) 0, base, index, scale, imm8);
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

    private void assemble0132(byte opcode1, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister16 source) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0120(opcode1, (byte) 0, base, index, scale, source);
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

    private void assemble0133(byte opcode1, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister32 source) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0121(opcode1, (byte) 0, base, index, scale, source);
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

    private void assemble0134(byte opcode1, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister64 source) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0122(opcode1, (byte) 0, base, index, scale, source);
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

    private void assemble0135(byte opcode1, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister8 source) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0123(opcode1, (byte) 0, base, index, scale, source);
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

    private void assemble0136(byte opcode1, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, int imm32) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0124(opcode1, (byte) 0, base, index, scale, imm32);
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

    private void assemble0137(byte opcode1, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, short imm16) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0125(opcode1, (byte) 0, base, index, scale, imm16);
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

    private void assemble0138(byte opcode1, AMD64GeneralRegister32 destination, int imm32) {
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

    private void assemble0139(byte opcode1, AMD64GeneralRegister32 register, int imm32) {
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

    private void assemble0140(byte opcode1, AMD64GeneralRegister64 destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
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

    private void assemble0141(byte opcode1, AMD64GeneralRegister64 destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0140(opcode1, destination, (byte) 0, base, index, scale);
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

    private void assemble0142(byte opcode1, AMD64GeneralRegister64 destination, int imm32) {
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

    private void assemble0143(byte opcode1, AMD64GeneralRegister64 destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
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

    private void assemble0144(byte opcode1, AMD64GeneralRegister64 register, long imm64) {
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

    private void assemble0145(byte opcode1, AMD64IndirectRegister64 destination, byte imm8) {
        if (destination == AMD64IndirectRegister64.RBP_INDIRECT || destination == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0126(opcode1, (byte) 0, destination, imm8);
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

    private void assemble0146(byte opcode1, AMD64IndirectRegister64 destination, AMD64GeneralRegister16 source) {
        if (destination == AMD64IndirectRegister64.RBP_INDIRECT || destination == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0127(opcode1, (byte) 0, destination, source);
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

    private void assemble0147(byte opcode1, AMD64IndirectRegister64 destination, AMD64GeneralRegister32 source) {
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

    private void assemble0148(byte opcode1, AMD64IndirectRegister64 destination, AMD64GeneralRegister64 source) {
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

    private void assemble0149(byte opcode1, AMD64IndirectRegister64 destination, AMD64GeneralRegister8 source) {
        if (destination == AMD64IndirectRegister64.RBP_INDIRECT || destination == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0128(opcode1, (byte) 0, destination, source);
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

    private void assemble0150(byte opcode1, AMD64IndirectRegister64 destination, int imm32) {
        if (destination == AMD64IndirectRegister64.RBP_INDIRECT || destination == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0129(opcode1, (byte) 0, destination, imm32);
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

    private void assemble0151(byte opcode1, AMD64IndirectRegister64 destination, short imm16) {
        if (destination == AMD64IndirectRegister64.RBP_INDIRECT || destination == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0130(opcode1, (byte) 0, destination, imm16);
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

    private void assemble0152(byte opcode1, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, byte imm8) {
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

    private void assemble0153(byte opcode1, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister16 source) {
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

    private void assemble0154(byte opcode1, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister32 source) {
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

    private void assemble0155(byte opcode1, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister64 source) {
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

    private void assemble0156(byte opcode1, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64GeneralRegister8 source) {
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

    private void assemble0157(byte opcode1, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, int imm32) {
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

    private void assemble0158(byte opcode1, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, short imm16) {
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

    private void assemble0159(byte opcode1, int disp32, AMD64IndirectRegister64 destination, byte imm8) {
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

    private void assemble0160(byte opcode1, int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister16 source) {
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

    private void assemble0161(byte opcode1, int disp32, AMD64IndirectRegister64 destination, AMD64GeneralRegister8 source) {
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

    private void assemble0162(byte opcode1, int disp32, AMD64IndirectRegister64 destination, int imm32) {
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

    private void assemble0163(byte opcode1, int disp32, AMD64IndirectRegister64 destination, int imm32) {
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

    private void assemble0164(byte opcode1, int disp32, AMD64IndirectRegister64 destination, short imm16) {
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

    private void assemble0165(byte opcode2, byte disp8, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
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

    private void assemble0166(byte opcode2, byte disp8, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
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

    private void assemble0167(byte opcode2, AMD64GeneralRegister32 destination, AMD64XMMRegister source) {
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

    private void assemble0168(byte opcode2, AMD64GeneralRegister64 destination, AMD64XMMRegister source) {
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

    private void assemble0169(byte opcode2, AMD64XMMRegister destination, AMD64GeneralRegister32 source) {
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

    private void assemble0170(byte opcode2, AMD64XMMRegister destination, AMD64GeneralRegister64 source) {
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

    private void assemble0171(byte opcode2, int disp32, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
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

    private void assemble0172(byte opcode2, int disp32, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
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

    private void assemble0173(byte opcode2, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64XMMRegister source) {
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

    private void assemble0174(byte opcode2, byte disp8, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
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

    private void assemble0175(byte opcode2, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64XMMRegister source) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0173(opcode2, (byte) 0, base, index, scale, source);
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

    private void assemble0176(byte opcode2, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        if (destination == AMD64IndirectRegister64.RBP_INDIRECT || destination == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0174(opcode2, (byte) 0, destination, source);
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

    private void assemble0177(byte opcode2, AMD64XMMRegister destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
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

    private void assemble0178(byte opcode2, AMD64XMMRegister destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0177(opcode2, destination, (byte) 0, base, index, scale);
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

    private void assemble0179(byte opcode2, AMD64XMMRegister destination, AMD64IndirectRegister64 source) {
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

    private void assemble0180(byte opcode2, AMD64XMMRegister destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
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

    private void assemble0181(byte opcode2, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64XMMRegister source) {
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

    private void assemble0182(byte opcode2, int disp32, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
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

    private void assemble0183(byte opcode2, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64XMMRegister source) {
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

    private void assemble0184(byte opcode2, byte disp8, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
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

    private void assemble0185(byte opcode2, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64XMMRegister source) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0183(opcode2, (byte) 0, base, index, scale, source);
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

    private void assemble0186(byte opcode2, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
        if (destination == AMD64IndirectRegister64.RBP_INDIRECT || destination == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0184(opcode2, (byte) 0, destination, source);
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

    private void assemble0187(byte opcode2, AMD64XMMRegister destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
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

    private void assemble0188(byte opcode2, AMD64XMMRegister destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0187(opcode2, destination, (byte) 0, base, index, scale);
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

    private void assemble0189(byte opcode2, AMD64XMMRegister destination, AMD64IndirectRegister64 source) {
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

    private void assemble0190(byte opcode2, AMD64XMMRegister destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
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

    private void assemble0191(byte opcode2, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale, AMD64XMMRegister source) {
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

    private void assemble0192(byte opcode2, int disp32, AMD64IndirectRegister64 destination, AMD64XMMRegister source) {
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

    private void assemble0193(byte opcode2, AMD64GeneralRegister32 destination, AMD64GeneralRegister8 source) {
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

    private void assemble0194(byte opcode2, AMD64GeneralRegister64 destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
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

    private void assemble0195(byte opcode2, AMD64GeneralRegister64 destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0194(opcode2, destination, (byte) 0, base, index, scale);
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

    private void assemble0196(byte opcode2, AMD64GeneralRegister64 destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
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

    private void assemble0197(byte opcode1, AMD64GeneralRegister64 destination, AMD64GeneralRegister32 source) {
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

    private void assemble0198(byte opcode2, AMD64GeneralRegister32 destination, AMD64GeneralRegister16 source) {
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

    private void assemble0199(byte opcode1, AMD64GeneralRegister64 destination, byte disp8, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
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

    private void assemble0200(byte opcode1, AMD64GeneralRegister64 destination, byte disp8, AMD64IndirectRegister64 source) {
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

    private void assemble0201(byte opcode1, AMD64GeneralRegister64 destination, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
        if (base == AMD64BaseRegister64.RBP_BASE || base == AMD64BaseRegister64.R13_BASE) {
            assemble0199(opcode1, destination, (byte) 0, base, index, scale);
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

    private void assemble0202(byte opcode1, AMD64GeneralRegister64 destination, AMD64GeneralRegister32 source) {
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

    private void assemble0203(byte opcode1, AMD64GeneralRegister64 destination, AMD64IndirectRegister64 source) {
        if (source == AMD64IndirectRegister64.RBP_INDIRECT || source == AMD64IndirectRegister64.R13_INDIRECT) {
            assemble0200(opcode1, destination, (byte) 0, source);
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

    private void assemble0204(byte opcode1, AMD64GeneralRegister64 destination, int rel32) {
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

    private void assemble0205(byte opcode1, AMD64GeneralRegister64 destination, int disp32, AMD64BaseRegister64 base, AMD64IndexRegister64 index, Scale scale) {
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

    private void assemble0206(byte opcode1, AMD64GeneralRegister64 destination, int disp32, AMD64IndirectRegister64 source) {
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

    private void assemble0207(byte opcode1, AMD64GeneralRegister64 register) {
        byte rex = (byte) 0;
        if (register.value() >= 8) {
            rex |= (1 << 0) + 0x40; // opcode1 extension by REX.B bit
        }
        if (rex != (byte) 0) {
            emitByte(rex);
        }
        emitByte((byte) (opcode1 + (register.value()& 7))); // opcode1_rexb
    }

    private void assemble0208(byte opcode1, short imm16) {
        emitByte(opcode1);
        // appended:
        emitByte((byte) (imm16 & 0xff));
        imm16 >>= 8;
        emitByte((byte) (imm16 & 0xff));
    }

    private void assemble0209(byte opcode2, AMD64GeneralRegister8 destination) {
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
    // Template#: 1, Serial#: 48
    public void rip_add(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_add(destination, placeHolder);
        new rip_add_48(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code add       rax, [L1: +305419896]}
     */
    // Template#: 2, Serial#: 56
    public void rip_add(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_add(destination, placeHolder);
        new rip_add_56(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code addsd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code addsd     xmm0, [L1: +305419896]}
     */
    // Template#: 3, Serial#: 5504
    public void rip_addsd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_addsd(destination, placeHolder);
        new rip_addsd_5504(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code addss  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code addss     xmm0, [L1: +305419896]}
     */
    // Template#: 4, Serial#: 5567
    public void rip_addss(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_addss(destination, placeHolder);
        new rip_addss_5567(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code and       eax, [L1: +305419896]}
     */
    // Template#: 5, Serial#: 192
    public void rip_and(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_and(destination, placeHolder);
        new rip_and_192(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code and       rax, [L1: +305419896]}
     */
    // Template#: 6, Serial#: 200
    public void rip_and(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_and(destination, placeHolder);
        new rip_and_200(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code call  }<i>label</i>
     * Example disassembly syntax: {@code call      L1: +305419896}
     */
    // Template#: 7, Serial#: 2957
    public void call(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        call(placeHolder);
        new call_2957(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code call  }<i>label</i>
     * Example disassembly syntax: {@code call      [L1: +305419896]}
     */
    // Template#: 8, Serial#: 3049
    public void rip_call(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_call(placeHolder);
        new rip_call_3049(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmova  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmova     eax, [L1: +305419896]}
     */
    // Template#: 9, Serial#: 3631
    public void rip_cmova(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmova(destination, placeHolder);
        new rip_cmova_3631(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovb     eax, [L1: +305419896]}
     */
    // Template#: 10, Serial#: 3496
    public void rip_cmovb(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovb(destination, placeHolder);
        new rip_cmovb_3496(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovbe  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovbe    eax, [L1: +305419896]}
     */
    // Template#: 11, Serial#: 3604
    public void rip_cmovbe(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovbe(destination, placeHolder);
        new rip_cmovbe_3604(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmove  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmove     eax, [L1: +305419896]}
     */
    // Template#: 12, Serial#: 3550
    public void rip_cmove(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmove(destination, placeHolder);
        new rip_cmove_3550(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovg  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovg     eax, [L1: +305419896]}
     */
    // Template#: 13, Serial#: 5333
    public void rip_cmovg(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovg(destination, placeHolder);
        new rip_cmovg_5333(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovge  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovge    eax, [L1: +305419896]}
     */
    // Template#: 14, Serial#: 5279
    public void rip_cmovge(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovge(destination, placeHolder);
        new rip_cmovge_5279(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovl  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovl     eax, [L1: +305419896]}
     */
    // Template#: 15, Serial#: 5252
    public void rip_cmovl(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovl(destination, placeHolder);
        new rip_cmovl_5252(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovle  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovle    eax, [L1: +305419896]}
     */
    // Template#: 16, Serial#: 5306
    public void rip_cmovle(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovle(destination, placeHolder);
        new rip_cmovle_5306(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovp  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovp     eax, [L1: +305419896]}
     */
    // Template#: 17, Serial#: 5198
    public void rip_cmovp(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovp(destination, placeHolder);
        new rip_cmovp_5198(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmp       eax, [L1: +305419896]}
     */
    // Template#: 18, Serial#: 2248
    public void rip_cmp(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmp(destination, placeHolder);
        new rip_cmp_2248(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmp       rax, [L1: +305419896]}
     */
    // Template#: 19, Serial#: 2256
    public void rip_cmp(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmp(destination, placeHolder);
        new rip_cmp_2256(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpsd  }<i>destination</i>, <i>label</i>, <i>amd64xmmcomparison</i>
     * Example disassembly syntax: {@code cmpsd     xmm0, [L1: +305419896], less_than_or_equal}
     */
    // Template#: 20, Serial#: 4445
    public void rip_cmpsd(final AMD64XMMRegister destination, final Label label, final AMD64XMMComparison amd64xmmcomparison) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmpsd(destination, placeHolder, amd64xmmcomparison);
        new rip_cmpsd_4445(startPosition, currentPosition() - startPosition, destination, amd64xmmcomparison, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpss  }<i>destination</i>, <i>label</i>, <i>amd64xmmcomparison</i>
     * Example disassembly syntax: {@code cmpss     xmm0, [L1: +305419896], less_than_or_equal}
     */
    // Template#: 21, Serial#: 4454
    public void rip_cmpss(final AMD64XMMRegister destination, final Label label, final AMD64XMMComparison amd64xmmcomparison) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmpss(destination, placeHolder, amd64xmmcomparison);
        new rip_cmpss_4454(startPosition, currentPosition() - startPosition, destination, amd64xmmcomparison, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code comisd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code comisd    xmm0, [L1: +305419896]}
     */
    // Template#: 22, Serial#: 5027
    public void rip_comisd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_comisd(destination, placeHolder);
        new rip_comisd_5027(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code comiss  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code comiss    xmm0, [L1: +305419896]}
     */
    // Template#: 23, Serial#: 4957
    public void rip_comiss(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_comiss(destination, placeHolder);
        new rip_comiss_4957(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2si  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtsd2si  eax, [L1: +305419896]}
     */
    // Template#: 24, Serial#: 5072
    public void rip_cvtsd2si(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtsd2si(destination, placeHolder);
        new rip_cvtsd2si_5072(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2si  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtsd2si  rax, [L1: +305419896]}
     */
    // Template#: 25, Serial#: 5081
    public void rip_cvtsd2si(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtsd2si(destination, placeHolder);
        new rip_cvtsd2si_5081(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2ss  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtsd2ss  xmm0, [L1: +305419896]}
     */
    // Template#: 26, Serial#: 5522
    public void rip_cvtsd2ss(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtsd2ss(destination, placeHolder);
        new rip_cvtsd2ss_5522(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2sdl  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtsi2sdl  xmm0, [L1: +305419896]}
     */
    // Template#: 27, Serial#: 5036
    public void rip_cvtsi2sdl(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtsi2sdl(destination, placeHolder);
        new rip_cvtsi2sdl_5036(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2sdq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtsi2sdq  xmm0, [L1: +305419896]}
     */
    // Template#: 28, Serial#: 5045
    public void rip_cvtsi2sdq(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtsi2sdq(destination, placeHolder);
        new rip_cvtsi2sdq_5045(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2ssl  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtsi2ssl  xmm0, [L1: +305419896]}
     */
    // Template#: 29, Serial#: 5090
    public void rip_cvtsi2ssl(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtsi2ssl(destination, placeHolder);
        new rip_cvtsi2ssl_5090(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsi2ssq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtsi2ssq  xmm0, [L1: +305419896]}
     */
    // Template#: 30, Serial#: 5099
    public void rip_cvtsi2ssq(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtsi2ssq(destination, placeHolder);
        new rip_cvtsi2ssq_5099(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2sd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtss2sd  xmm0, [L1: +305419896]}
     */
    // Template#: 31, Serial#: 5585
    public void rip_cvtss2sd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtss2sd(destination, placeHolder);
        new rip_cvtss2sd_5585(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2si  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtss2si  eax, [L1: +305419896]}
     */
    // Template#: 32, Serial#: 5126
    public void rip_cvtss2si(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtss2si(destination, placeHolder);
        new rip_cvtss2si_5126(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtss2si  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtss2si  rax, [L1: +305419896]}
     */
    // Template#: 33, Serial#: 5135
    public void rip_cvtss2si(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtss2si(destination, placeHolder);
        new rip_cvtss2si_5135(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttsd2si  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvttsd2si  eax, [L1: +305419896]}
     */
    // Template#: 34, Serial#: 5054
    public void rip_cvttsd2si(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvttsd2si(destination, placeHolder);
        new rip_cvttsd2si_5054(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttsd2si  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvttsd2si  rax, [L1: +305419896]}
     */
    // Template#: 35, Serial#: 5063
    public void rip_cvttsd2si(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvttsd2si(destination, placeHolder);
        new rip_cvttsd2si_5063(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttss2si  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvttss2si  eax, [L1: +305419896]}
     */
    // Template#: 36, Serial#: 5108
    public void rip_cvttss2si(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvttss2si(destination, placeHolder);
        new rip_cvttss2si_5108(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttss2si  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvttss2si  rax, [L1: +305419896]}
     */
    // Template#: 37, Serial#: 5117
    public void rip_cvttss2si(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvttss2si(destination, placeHolder);
        new rip_cvttss2si_5117(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code divq  }<i>label</i>
     * Example disassembly syntax: {@code divq      [L1: +305419896]}
     */
    // Template#: 38, Serial#: 1880
    public void rip_divq(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_divq(placeHolder);
        new rip_divq_1880(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code divsd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code divsd     xmm0, [L1: +305419896]}
     */
    // Template#: 39, Serial#: 5549
    public void rip_divsd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_divsd(destination, placeHolder);
        new rip_divsd_5549(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code divss  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code divss     xmm0, [L1: +305419896]}
     */
    // Template#: 40, Serial#: 5621
    public void rip_divss(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_divss(destination, placeHolder);
        new rip_divss_5621(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code idivl  }<i>label</i>
     * Example disassembly syntax: {@code idivl     [L1: +305419896]}
     */
    // Template#: 41, Serial#: 1821
    public void rip_idivl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_idivl(placeHolder);
        new rip_idivl_1821(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code idivq  }<i>label</i>
     * Example disassembly syntax: {@code idivq     [L1: +305419896]}
     */
    // Template#: 42, Serial#: 1884
    public void rip_idivq(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_idivq(placeHolder);
        new rip_idivq_1884(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code imul      eax, [L1: +305419896]}
     */
    // Template#: 43, Serial#: 6103
    public void rip_imul(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_imul(destination, placeHolder);
        new rip_imul_6103(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code imul      rax, [L1: +305419896]}
     */
    // Template#: 44, Serial#: 6112
    public void rip_imul(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_imul(destination, placeHolder);
        new rip_imul_6112(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jb  }<i>label</i>
     * Example disassembly syntax: {@code jb        L1: +18}
     */
    // Template#: 45, Serial#: 315
    public void jb(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jb(placeHolder);
        new jb_315(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jbe  }<i>label</i>
     * Example disassembly syntax: {@code jbe       L1: +18}
     */
    // Template#: 46, Serial#: 319
    public void jbe(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jbe(placeHolder);
        new jbe_319(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jl  }<i>label</i>
     * Example disassembly syntax: {@code jl        L1: +18}
     */
    // Template#: 47, Serial#: 2342
    public void jl(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jl(placeHolder);
        new jl_2342(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jle  }<i>label</i>
     * Example disassembly syntax: {@code jle       L1: +18}
     */
    // Template#: 48, Serial#: 2344
    public void jle(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jle(placeHolder);
        new jle_2344(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jmp  }<i>label</i>
     * Example disassembly syntax: {@code jmp       L1: +18}
     */
    // Template#: 49, Serial#: 2959
    public void jmp(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jmp(placeHolder);
        new jmp_2959(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jmp  }<i>label</i>
     * Example disassembly syntax: {@code jmp       [L1: +305419896]}
     */
    // Template#: 50, Serial#: 3053
    public void rip_jmp(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_jmp(placeHolder);
        new rip_jmp_3053(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnb  }<i>label</i>
     * Example disassembly syntax: {@code jnb       L1: +18}
     */
    // Template#: 51, Serial#: 316
    public void jnb(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jnb(placeHolder);
        new jnb_316(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnbe  }<i>label</i>
     * Example disassembly syntax: {@code jnbe      L1: +18}
     */
    // Template#: 52, Serial#: 320
    public void jnbe(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jnbe(placeHolder);
        new jnbe_320(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnl  }<i>label</i>
     * Example disassembly syntax: {@code jnl       L1: +18}
     */
    // Template#: 53, Serial#: 2343
    public void jnl(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jnl(placeHolder);
        new jnl_2343(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnle  }<i>label</i>
     * Example disassembly syntax: {@code jnle      L1: +18}
     */
    // Template#: 54, Serial#: 2345
    public void jnle(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jnle(placeHolder);
        new jnle_2345(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnz  }<i>label</i>
     * Example disassembly syntax: {@code jnz       L1: +18}
     */
    // Template#: 55, Serial#: 318
    public void jnz(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jnz(placeHolder);
        new jnz_318(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jz  }<i>label</i>
     * Example disassembly syntax: {@code jz        L1: +18}
     */
    // Template#: 56, Serial#: 317
    public void jz(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jz(placeHolder);
        new jz_317(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code lea  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code lea       rax, [L1: +305419896]}
     */
    // Template#: 57, Serial#: 2434
    public void rip_lea(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_lea(destination, placeHolder);
        new rip_lea_2434(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code mov       rax, [L1: +305419896]}
     */
    // Template#: 58, Serial#: 2401
    public void rip_mov(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_mov(destination, placeHolder);
        new rip_mov_2401(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movsd     xmm0, [L1: +305419896]}
     */
    // Template#: 59, Serial#: 3373
    public void rip_movsd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movsd(destination, placeHolder);
        new rip_movsd_3373(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movss     xmm0, [L1: +305419896]}
     */
    // Template#: 60, Serial#: 3399
    public void rip_movss(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movss(destination, placeHolder);
        new rip_movss_3399(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsx  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movsx     eax, [L1: +305419896]}
     */
    // Template#: 61, Serial#: 6255
    public void rip_movsxb(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movsxb(destination, placeHolder);
        new rip_movsxb_6255(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movsxd    rax, [L1: +305419896]}
     */
    // Template#: 62, Serial#: 294
    public void rip_movsxd(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movsxd(destination, placeHolder);
        new rip_movsxd_294(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movsxw    eax, [L1: +305419896]}
     */
    // Template#: 63, Serial#: 6282
    public void rip_movsxw(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movsxw(destination, placeHolder);
        new rip_movsxw_6282(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movzxd    rax, [L1: +305419896]}
     */
    // Template#: 64, Serial#: 303
    public void rip_movzxd(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movzxd(destination, placeHolder);
        new rip_movzxd_303(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movzxw    eax, [L1: +305419896]}
     */
    // Template#: 65, Serial#: 4311
    public void rip_movzxw(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movzxw(destination, placeHolder);
        new rip_movzxw_4311(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mulsd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code mulsd     xmm0, [L1: +305419896]}
     */
    // Template#: 66, Serial#: 5513
    public void rip_mulsd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_mulsd(destination, placeHolder);
        new rip_mulsd_5513(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mulss  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code mulss     xmm0, [L1: +305419896]}
     */
    // Template#: 67, Serial#: 5576
    public void rip_mulss(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_mulss(destination, placeHolder);
        new rip_mulss_5576(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code or        eax, [L1: +305419896]}
     */
    // Template#: 68, Serial#: 2030
    public void rip_or(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_or(destination, placeHolder);
        new rip_or_2030(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code or        rax, [L1: +305419896]}
     */
    // Template#: 69, Serial#: 2038
    public void rip_or(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_or(destination, placeHolder);
        new rip_or_2038(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code push  }<i>label</i>
     * Example disassembly syntax: {@code push      [L1: +305419896]}
     */
    // Template#: 70, Serial#: 3057
    public void rip_push(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_push(placeHolder);
        new rip_push_3057(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code sub       eax, [L1: +305419896]}
     */
    // Template#: 71, Serial#: 2174
    public void rip_sub(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sub(destination, placeHolder);
        new rip_sub_2174(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code sub       rax, [L1: +305419896]}
     */
    // Template#: 72, Serial#: 2182
    public void rip_sub(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sub(destination, placeHolder);
        new rip_sub_2182(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code subsd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code subsd     xmm0, [L1: +305419896]}
     */
    // Template#: 73, Serial#: 5531
    public void rip_subsd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_subsd(destination, placeHolder);
        new rip_subsd_5531(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code subss  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code subss     xmm0, [L1: +305419896]}
     */
    // Template#: 74, Serial#: 5603
    public void rip_subss(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_subss(destination, placeHolder);
        new rip_subss_5603(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code xor       eax, [L1: +305419896]}
     */
    // Template#: 75, Serial#: 264
    public void rip_xor(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xor(destination, placeHolder);
        new rip_xor_264(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code xor       rax, [L1: +305419896]}
     */
    // Template#: 76, Serial#: 272
    public void rip_xor(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xor(destination, placeHolder);
        new rip_xor_272(startPosition, currentPosition() - startPosition, destination, label);
    }

    class rip_add_48 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_add_48(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_add(_destination, offsetAsInt());
        }
    }

    class rip_add_56 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_add_56(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_add(_destination, offsetAsInt());
        }
    }

    class rip_addsd_5504 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_addsd_5504(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_addsd(_destination, offsetAsInt());
        }
    }

    class rip_addss_5567 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_addss_5567(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_addss(_destination, offsetAsInt());
        }
    }

    class rip_and_192 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_and_192(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_and(_destination, offsetAsInt());
        }
    }

    class rip_and_200 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_and_200(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_and(_destination, offsetAsInt());
        }
    }

    class call_2957 extends InstructionWithOffset {
        call_2957(int startPosition, int endPosition, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            call(offsetAsInt());
        }
    }

    class rip_call_3049 extends InstructionWithOffset {
        rip_call_3049(int startPosition, int endPosition, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_call(offsetAsInt());
        }
    }

    class rip_cmova_3631 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmova_3631(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmova(_destination, offsetAsInt());
        }
    }

    class rip_cmovb_3496 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmovb_3496(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovb(_destination, offsetAsInt());
        }
    }

    class rip_cmovbe_3604 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmovbe_3604(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovbe(_destination, offsetAsInt());
        }
    }

    class rip_cmove_3550 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmove_3550(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmove(_destination, offsetAsInt());
        }
    }

    class rip_cmovg_5333 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmovg_5333(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovg(_destination, offsetAsInt());
        }
    }

    class rip_cmovge_5279 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmovge_5279(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovge(_destination, offsetAsInt());
        }
    }

    class rip_cmovl_5252 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmovl_5252(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovl(_destination, offsetAsInt());
        }
    }

    class rip_cmovle_5306 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmovle_5306(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovle(_destination, offsetAsInt());
        }
    }

    class rip_cmovp_5198 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmovp_5198(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovp(_destination, offsetAsInt());
        }
    }

    class rip_cmp_2248 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmp_2248(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmp(_destination, offsetAsInt());
        }
    }

    class rip_cmp_2256 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_cmp_2256(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmp(_destination, offsetAsInt());
        }
    }

    class rip_cmpsd_4445 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        private final AMD64XMMComparison _amd64xmmcomparison;
        rip_cmpsd_4445(int startPosition, int endPosition, AMD64XMMRegister destination, AMD64XMMComparison amd64xmmcomparison, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
            _amd64xmmcomparison = amd64xmmcomparison;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmpsd(_destination, offsetAsInt(), _amd64xmmcomparison);
        }
    }

    class rip_cmpss_4454 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        private final AMD64XMMComparison _amd64xmmcomparison;
        rip_cmpss_4454(int startPosition, int endPosition, AMD64XMMRegister destination, AMD64XMMComparison amd64xmmcomparison, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
            _amd64xmmcomparison = amd64xmmcomparison;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmpss(_destination, offsetAsInt(), _amd64xmmcomparison);
        }
    }

    class rip_comisd_5027 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_comisd_5027(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_comisd(_destination, offsetAsInt());
        }
    }

    class rip_comiss_4957 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_comiss_4957(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_comiss(_destination, offsetAsInt());
        }
    }

    class rip_cvtsd2si_5072 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cvtsd2si_5072(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtsd2si(_destination, offsetAsInt());
        }
    }

    class rip_cvtsd2si_5081 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_cvtsd2si_5081(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtsd2si(_destination, offsetAsInt());
        }
    }

    class rip_cvtsd2ss_5522 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_cvtsd2ss_5522(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtsd2ss(_destination, offsetAsInt());
        }
    }

    class rip_cvtsi2sdl_5036 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_cvtsi2sdl_5036(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtsi2sdl(_destination, offsetAsInt());
        }
    }

    class rip_cvtsi2sdq_5045 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_cvtsi2sdq_5045(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtsi2sdq(_destination, offsetAsInt());
        }
    }

    class rip_cvtsi2ssl_5090 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_cvtsi2ssl_5090(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtsi2ssl(_destination, offsetAsInt());
        }
    }

    class rip_cvtsi2ssq_5099 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_cvtsi2ssq_5099(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtsi2ssq(_destination, offsetAsInt());
        }
    }

    class rip_cvtss2sd_5585 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_cvtss2sd_5585(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtss2sd(_destination, offsetAsInt());
        }
    }

    class rip_cvtss2si_5126 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cvtss2si_5126(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtss2si(_destination, offsetAsInt());
        }
    }

    class rip_cvtss2si_5135 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_cvtss2si_5135(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtss2si(_destination, offsetAsInt());
        }
    }

    class rip_cvttsd2si_5054 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cvttsd2si_5054(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvttsd2si(_destination, offsetAsInt());
        }
    }

    class rip_cvttsd2si_5063 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_cvttsd2si_5063(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvttsd2si(_destination, offsetAsInt());
        }
    }

    class rip_cvttss2si_5108 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cvttss2si_5108(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvttss2si(_destination, offsetAsInt());
        }
    }

    class rip_cvttss2si_5117 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_cvttss2si_5117(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvttss2si(_destination, offsetAsInt());
        }
    }

    class rip_divq_1880 extends InstructionWithOffset {
        rip_divq_1880(int startPosition, int endPosition, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_divq(offsetAsInt());
        }
    }

    class rip_divsd_5549 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_divsd_5549(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_divsd(_destination, offsetAsInt());
        }
    }

    class rip_divss_5621 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_divss_5621(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_divss(_destination, offsetAsInt());
        }
    }

    class rip_idivl_1821 extends InstructionWithOffset {
        rip_idivl_1821(int startPosition, int endPosition, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_idivl(offsetAsInt());
        }
    }

    class rip_idivq_1884 extends InstructionWithOffset {
        rip_idivq_1884(int startPosition, int endPosition, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_idivq(offsetAsInt());
        }
    }

    class rip_imul_6103 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_imul_6103(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_imul(_destination, offsetAsInt());
        }
    }

    class rip_imul_6112 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_imul_6112(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_imul(_destination, offsetAsInt());
        }
    }

    class jb_315 extends InstructionWithOffset {
        jb_315(int startPosition, int endPosition, Label label) {
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

    class jbe_319 extends InstructionWithOffset {
        jbe_319(int startPosition, int endPosition, Label label) {
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

    class jl_2342 extends InstructionWithOffset {
        jl_2342(int startPosition, int endPosition, Label label) {
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

    class jle_2344 extends InstructionWithOffset {
        jle_2344(int startPosition, int endPosition, Label label) {
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

    class jmp_2959 extends InstructionWithOffset {
        jmp_2959(int startPosition, int endPosition, Label label) {
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

    class rip_jmp_3053 extends InstructionWithOffset {
        rip_jmp_3053(int startPosition, int endPosition, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_jmp(offsetAsInt());
        }
    }

    class jnb_316 extends InstructionWithOffset {
        jnb_316(int startPosition, int endPosition, Label label) {
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

    class jnbe_320 extends InstructionWithOffset {
        jnbe_320(int startPosition, int endPosition, Label label) {
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

    class jnl_2343 extends InstructionWithOffset {
        jnl_2343(int startPosition, int endPosition, Label label) {
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

    class jnle_2345 extends InstructionWithOffset {
        jnle_2345(int startPosition, int endPosition, Label label) {
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

    class jnz_318 extends InstructionWithOffset {
        jnz_318(int startPosition, int endPosition, Label label) {
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

    class jz_317 extends InstructionWithOffset {
        jz_317(int startPosition, int endPosition, Label label) {
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

    class rip_lea_2434 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_lea_2434(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_lea(_destination, offsetAsInt());
        }
    }

    class rip_mov_2401 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_mov_2401(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_mov(_destination, offsetAsInt());
        }
    }

    class rip_movsd_3373 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_movsd_3373(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movsd(_destination, offsetAsInt());
        }
    }

    class rip_movss_3399 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_movss_3399(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movss(_destination, offsetAsInt());
        }
    }

    class rip_movsxb_6255 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_movsxb_6255(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movsxb(_destination, offsetAsInt());
        }
    }

    class rip_movsxd_294 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_movsxd_294(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movsxd(_destination, offsetAsInt());
        }
    }

    class rip_movsxw_6282 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_movsxw_6282(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movsxw(_destination, offsetAsInt());
        }
    }

    class rip_movzxd_303 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_movzxd_303(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movzxd(_destination, offsetAsInt());
        }
    }

    class rip_movzxw_4311 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_movzxw_4311(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movzxw(_destination, offsetAsInt());
        }
    }

    class rip_mulsd_5513 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_mulsd_5513(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_mulsd(_destination, offsetAsInt());
        }
    }

    class rip_mulss_5576 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_mulss_5576(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_mulss(_destination, offsetAsInt());
        }
    }

    class rip_or_2030 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_or_2030(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_or(_destination, offsetAsInt());
        }
    }

    class rip_or_2038 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_or_2038(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_or(_destination, offsetAsInt());
        }
    }

    class rip_push_3057 extends InstructionWithOffset {
        rip_push_3057(int startPosition, int endPosition, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_push(offsetAsInt());
        }
    }

    class rip_sub_2174 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_sub_2174(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sub(_destination, offsetAsInt());
        }
    }

    class rip_sub_2182 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_sub_2182(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sub(_destination, offsetAsInt());
        }
    }

    class rip_subsd_5531 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_subsd_5531(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_subsd(_destination, offsetAsInt());
        }
    }

    class rip_subss_5603 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_subss_5603(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_subss(_destination, offsetAsInt());
        }
    }

    class rip_xor_264 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_xor_264(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xor(_destination, offsetAsInt());
        }
    }

    class rip_xor_272 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_xor_272(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64AssemblerMethods.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xor(_destination, offsetAsInt());
        }
    }

// END GENERATED LABEL ASSEMBLER METHODS
}
