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
/*VCSID=ddf44d84-4e6a-4f14-b94f-5130ace94d1c*/

package com.sun.max.asm.amd64.complete;

import com.sun.max.asm.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.asm.x86.*;

public class AMD64LabelAssembler extends AMD64RawAssembler {

    public AMD64LabelAssembler(long startAddress) {
        super(startAddress);
    }

    public AMD64LabelAssembler() {
    }

// START GENERATED LABEL ASSEMBLER METHODS
    /**
     * Pseudo-external assembler syntax: {@code adc  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code adc       ax, [L1: +305419896]}
     */
    // Template#: 1, Serial#: 136
    public void rip_adc(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_adc(destination, placeHolder);
        new rip_adc_136(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code adc  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code adc       eax, [L1: +305419896]}
     */
    // Template#: 2, Serial#: 120
    public void rip_adc(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_adc(destination, placeHolder);
        new rip_adc_120(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code adc  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code adc       rax, [L1: +305419896]}
     */
    // Template#: 3, Serial#: 128
    public void rip_adc(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_adc(destination, placeHolder);
        new rip_adc_128(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code adc  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code adc       al, [L1: +305419896]}
     */
    // Template#: 4, Serial#: 112
    public void rip_adc(final AMD64GeneralRegister8 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_adc(destination, placeHolder);
        new rip_adc_112(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code adcb  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code adcb      [L1: +305419896], 0x12}
     */
    // Template#: 5, Serial#: 332
    public void rip_adcb(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_adcb(placeHolder, imm8);
        new rip_adcb_332(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code adcl  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code adcl      [L1: +305419896], 0x12}
     */
    // Template#: 6, Serial#: 620
    public void rip_adcl(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_adcl(placeHolder, imm8);
        new rip_adcl_620(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code adcq  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code adcq      [L1: +305419896], 0x12}
     */
    // Template#: 7, Serial#: 692
    public void rip_adcq(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_adcq(placeHolder, imm8);
        new rip_adcq_692(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code adcw  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code adcw      [L1: +305419896], 0x12}
     */
    // Template#: 8, Serial#: 764
    public void rip_adcw(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_adcw(placeHolder, imm8);
        new rip_adcw_764(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code adc  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code adc       [L1: +305419896], ax}
     */
    // Template#: 9, Serial#: 103
    public void rip_adc(final Label label, final AMD64GeneralRegister16 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_adc(placeHolder, source);
        new rip_adc_103(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code adc  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code adc       [L1: +305419896], eax}
     */
    // Template#: 10, Serial#: 85
    public void rip_adc(final Label label, final AMD64GeneralRegister32 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_adc(placeHolder, source);
        new rip_adc_85(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code adc  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code adc       [L1: +305419896], rax}
     */
    // Template#: 11, Serial#: 94
    public void rip_adc(final Label label, final AMD64GeneralRegister64 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_adc(placeHolder, source);
        new rip_adc_94(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code adc  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code adc       [L1: +305419896], al}
     */
    // Template#: 12, Serial#: 76
    public void rip_adc(final Label label, final AMD64GeneralRegister8 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_adc(placeHolder, source);
        new rip_adc_76(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code adcl  }<i>label</i>, <i>imm32</i>
     * Example disassembly syntax: {@code adcl      [L1: +305419896], 0x12345678}
     */
    // Template#: 13, Serial#: 404
    public void rip_adcl(final Label label, final int imm32) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_adcl(placeHolder, imm32);
        new rip_adcl_404(startPosition, currentPosition() - startPosition, imm32, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code adcq  }<i>label</i>, <i>imm32</i>
     * Example disassembly syntax: {@code adcq      [L1: +305419896], 0x12345678}
     */
    // Template#: 14, Serial#: 476
    public void rip_adcq(final Label label, final int imm32) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_adcq(placeHolder, imm32);
        new rip_adcq_476(startPosition, currentPosition() - startPosition, imm32, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code adcw  }<i>label</i>, <i>imm16</i>
     * Example disassembly syntax: {@code adcw      [L1: +305419896], 0x1234}
     */
    // Template#: 15, Serial#: 548
    public void rip_adcw(final Label label, final short imm16) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_adcw(placeHolder, imm16);
        new rip_adcw_548(startPosition, currentPosition() - startPosition, imm16, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code add       ax, [L1: +305419896]}
     */
    // Template#: 16, Serial#: 64
    public void rip_add(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_add(destination, placeHolder);
        new rip_add_64(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code add       eax, [L1: +305419896]}
     */
    // Template#: 17, Serial#: 48
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
    // Template#: 18, Serial#: 56
    public void rip_add(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_add(destination, placeHolder);
        new rip_add_56(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code add       al, [L1: +305419896]}
     */
    // Template#: 19, Serial#: 40
    public void rip_add(final AMD64GeneralRegister8 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_add(destination, placeHolder);
        new rip_add_40(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code addb  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code addb      [L1: +305419896], 0x12}
     */
    // Template#: 20, Serial#: 324
    public void rip_addb(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_addb(placeHolder, imm8);
        new rip_addb_324(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code addl  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code addl      [L1: +305419896], 0x12}
     */
    // Template#: 21, Serial#: 612
    public void rip_addl(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_addl(placeHolder, imm8);
        new rip_addl_612(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code addq  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code addq      [L1: +305419896], 0x12}
     */
    // Template#: 22, Serial#: 684
    public void rip_addq(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_addq(placeHolder, imm8);
        new rip_addq_684(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code addw  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code addw      [L1: +305419896], 0x12}
     */
    // Template#: 23, Serial#: 756
    public void rip_addw(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_addw(placeHolder, imm8);
        new rip_addw_756(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code add       [L1: +305419896], ax}
     */
    // Template#: 24, Serial#: 31
    public void rip_add(final Label label, final AMD64GeneralRegister16 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_add(placeHolder, source);
        new rip_add_31(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code add       [L1: +305419896], eax}
     */
    // Template#: 25, Serial#: 13
    public void rip_add(final Label label, final AMD64GeneralRegister32 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_add(placeHolder, source);
        new rip_add_13(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code add       [L1: +305419896], rax}
     */
    // Template#: 26, Serial#: 22
    public void rip_add(final Label label, final AMD64GeneralRegister64 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_add(placeHolder, source);
        new rip_add_22(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code add  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code add       [L1: +305419896], al}
     */
    // Template#: 27, Serial#: 4
    public void rip_add(final Label label, final AMD64GeneralRegister8 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_add(placeHolder, source);
        new rip_add_4(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code addl  }<i>label</i>, <i>imm32</i>
     * Example disassembly syntax: {@code addl      [L1: +305419896], 0x12345678}
     */
    // Template#: 28, Serial#: 396
    public void rip_addl(final Label label, final int imm32) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_addl(placeHolder, imm32);
        new rip_addl_396(startPosition, currentPosition() - startPosition, imm32, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code addq  }<i>label</i>, <i>imm32</i>
     * Example disassembly syntax: {@code addq      [L1: +305419896], 0x12345678}
     */
    // Template#: 29, Serial#: 468
    public void rip_addq(final Label label, final int imm32) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_addq(placeHolder, imm32);
        new rip_addq_468(startPosition, currentPosition() - startPosition, imm32, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code addw  }<i>label</i>, <i>imm16</i>
     * Example disassembly syntax: {@code addw      [L1: +305419896], 0x1234}
     */
    // Template#: 30, Serial#: 540
    public void rip_addw(final Label label, final short imm16) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_addw(placeHolder, imm16);
        new rip_addw_540(startPosition, currentPosition() - startPosition, imm16, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code addpd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code addpd     xmm0, [L1: +305419896]}
     */
    // Template#: 31, Serial#: 5432
    public void rip_addpd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_addpd(destination, placeHolder);
        new rip_addpd_5432(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code addps  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code addps     xmm0, [L1: +305419896]}
     */
    // Template#: 32, Serial#: 5360
    public void rip_addps(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_addps(destination, placeHolder);
        new rip_addps_5360(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code addsd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code addsd     xmm0, [L1: +305419896]}
     */
    // Template#: 33, Serial#: 5504
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
    // Template#: 34, Serial#: 5567
    public void rip_addss(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_addss(destination, placeHolder);
        new rip_addss_5567(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code addsubpd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code addsubpd  xmm0, [L1: +305419896]}
     */
    // Template#: 35, Serial#: 4509
    public void rip_addsubpd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_addsubpd(destination, placeHolder);
        new rip_addsubpd_4509(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code and       ax, [L1: +305419896]}
     */
    // Template#: 36, Serial#: 208
    public void rip_and(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_and(destination, placeHolder);
        new rip_and_208(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code and       eax, [L1: +305419896]}
     */
    // Template#: 37, Serial#: 192
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
    // Template#: 38, Serial#: 200
    public void rip_and(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_and(destination, placeHolder);
        new rip_and_200(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code and       al, [L1: +305419896]}
     */
    // Template#: 39, Serial#: 184
    public void rip_and(final AMD64GeneralRegister8 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_and(destination, placeHolder);
        new rip_and_184(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code andb  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code andb      [L1: +305419896], 0x12}
     */
    // Template#: 40, Serial#: 340
    public void rip_andb(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_andb(placeHolder, imm8);
        new rip_andb_340(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code andl  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code andl      [L1: +305419896], 0x12}
     */
    // Template#: 41, Serial#: 628
    public void rip_andl(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_andl(placeHolder, imm8);
        new rip_andl_628(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code andq  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code andq      [L1: +305419896], 0x12}
     */
    // Template#: 42, Serial#: 700
    public void rip_andq(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_andq(placeHolder, imm8);
        new rip_andq_700(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code andw  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code andw      [L1: +305419896], 0x12}
     */
    // Template#: 43, Serial#: 772
    public void rip_andw(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_andw(placeHolder, imm8);
        new rip_andw_772(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code and       [L1: +305419896], ax}
     */
    // Template#: 44, Serial#: 175
    public void rip_and(final Label label, final AMD64GeneralRegister16 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_and(placeHolder, source);
        new rip_and_175(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code and       [L1: +305419896], eax}
     */
    // Template#: 45, Serial#: 157
    public void rip_and(final Label label, final AMD64GeneralRegister32 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_and(placeHolder, source);
        new rip_and_157(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code and       [L1: +305419896], rax}
     */
    // Template#: 46, Serial#: 166
    public void rip_and(final Label label, final AMD64GeneralRegister64 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_and(placeHolder, source);
        new rip_and_166(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code and  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code and       [L1: +305419896], al}
     */
    // Template#: 47, Serial#: 148
    public void rip_and(final Label label, final AMD64GeneralRegister8 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_and(placeHolder, source);
        new rip_and_148(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code andl  }<i>label</i>, <i>imm32</i>
     * Example disassembly syntax: {@code andl      [L1: +305419896], 0x12345678}
     */
    // Template#: 48, Serial#: 412
    public void rip_andl(final Label label, final int imm32) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_andl(placeHolder, imm32);
        new rip_andl_412(startPosition, currentPosition() - startPosition, imm32, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code andq  }<i>label</i>, <i>imm32</i>
     * Example disassembly syntax: {@code andq      [L1: +305419896], 0x12345678}
     */
    // Template#: 49, Serial#: 484
    public void rip_andq(final Label label, final int imm32) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_andq(placeHolder, imm32);
        new rip_andq_484(startPosition, currentPosition() - startPosition, imm32, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code andw  }<i>label</i>, <i>imm16</i>
     * Example disassembly syntax: {@code andw      [L1: +305419896], 0x1234}
     */
    // Template#: 50, Serial#: 556
    public void rip_andw(final Label label, final short imm16) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_andw(placeHolder, imm16);
        new rip_andw_556(startPosition, currentPosition() - startPosition, imm16, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code andnpd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code andnpd    xmm0, [L1: +305419896]}
     */
    // Template#: 51, Serial#: 3741
    public void rip_andnpd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_andnpd(destination, placeHolder);
        new rip_andnpd_3741(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code andnps  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code andnps    xmm0, [L1: +305419896]}
     */
    // Template#: 52, Serial#: 3695
    public void rip_andnps(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_andnps(destination, placeHolder);
        new rip_andnps_3695(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code andpd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code andpd     xmm0, [L1: +305419896]}
     */
    // Template#: 53, Serial#: 3732
    public void rip_andpd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_andpd(destination, placeHolder);
        new rip_andpd_3732(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code andps  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code andps     xmm0, [L1: +305419896]}
     */
    // Template#: 54, Serial#: 3686
    public void rip_andps(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_andps(destination, placeHolder);
        new rip_andps_3686(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code bsf  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code bsf       ax, [L1: +305419896]}
     */
    // Template#: 55, Serial#: 6219
    public void rip_bsf(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_bsf(destination, placeHolder);
        new rip_bsf_6219(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code bsf  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code bsf       eax, [L1: +305419896]}
     */
    // Template#: 56, Serial#: 6201
    public void rip_bsf(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_bsf(destination, placeHolder);
        new rip_bsf_6201(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code bsf  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code bsf       rax, [L1: +305419896]}
     */
    // Template#: 57, Serial#: 6210
    public void rip_bsf(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_bsf(destination, placeHolder);
        new rip_bsf_6210(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code bsr  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code bsr       ax, [L1: +305419896]}
     */
    // Template#: 58, Serial#: 6246
    public void rip_bsr(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_bsr(destination, placeHolder);
        new rip_bsr_6246(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code bsr  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code bsr       eax, [L1: +305419896]}
     */
    // Template#: 59, Serial#: 6228
    public void rip_bsr(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_bsr(destination, placeHolder);
        new rip_bsr_6228(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code bsr  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code bsr       rax, [L1: +305419896]}
     */
    // Template#: 60, Serial#: 6237
    public void rip_bsr(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_bsr(destination, placeHolder);
        new rip_bsr_6237(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code bt  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code bt        [L1: +305419896], 0x12}
     */
    // Template#: 61, Serial#: 6130
    public void rip_bt(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_bt(placeHolder, imm8);
        new rip_bt_6130(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code bt  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code bt        [L1: +305419896], ax}
     */
    // Template#: 62, Serial#: 4158
    public void rip_bt(final Label label, final AMD64GeneralRegister16 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_bt(placeHolder, source);
        new rip_bt_4158(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code bt  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code bt        [L1: +305419896], eax}
     */
    // Template#: 63, Serial#: 4140
    public void rip_bt(final Label label, final AMD64GeneralRegister32 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_bt(placeHolder, source);
        new rip_bt_4140(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code bt  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code bt        [L1: +305419896], rax}
     */
    // Template#: 64, Serial#: 4149
    public void rip_bt(final Label label, final AMD64GeneralRegister64 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_bt(placeHolder, source);
        new rip_bt_4149(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code btc  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code btc       [L1: +305419896], 0x12}
     */
    // Template#: 65, Serial#: 6142
    public void rip_btc(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_btc(placeHolder, imm8);
        new rip_btc_6142(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code btc  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code btc       [L1: +305419896], ax}
     */
    // Template#: 66, Serial#: 6192
    public void rip_btc(final Label label, final AMD64GeneralRegister16 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_btc(placeHolder, source);
        new rip_btc_6192(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code btc  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code btc       [L1: +305419896], eax}
     */
    // Template#: 67, Serial#: 6174
    public void rip_btc(final Label label, final AMD64GeneralRegister32 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_btc(placeHolder, source);
        new rip_btc_6174(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code btc  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code btc       [L1: +305419896], rax}
     */
    // Template#: 68, Serial#: 6183
    public void rip_btc(final Label label, final AMD64GeneralRegister64 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_btc(placeHolder, source);
        new rip_btc_6183(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code btr  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code btr       [L1: +305419896], 0x12}
     */
    // Template#: 69, Serial#: 6138
    public void rip_btr(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_btr(placeHolder, imm8);
        new rip_btr_6138(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code btr  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code btr       [L1: +305419896], ax}
     */
    // Template#: 70, Serial#: 4275
    public void rip_btr(final Label label, final AMD64GeneralRegister16 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_btr(placeHolder, source);
        new rip_btr_4275(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code btr  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code btr       [L1: +305419896], eax}
     */
    // Template#: 71, Serial#: 4257
    public void rip_btr(final Label label, final AMD64GeneralRegister32 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_btr(placeHolder, source);
        new rip_btr_4257(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code btr  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code btr       [L1: +305419896], rax}
     */
    // Template#: 72, Serial#: 4266
    public void rip_btr(final Label label, final AMD64GeneralRegister64 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_btr(placeHolder, source);
        new rip_btr_4266(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code bts  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code bts       [L1: +305419896], 0x12}
     */
    // Template#: 73, Serial#: 6134
    public void rip_bts(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_bts(placeHolder, imm8);
        new rip_bts_6134(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code bts  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code bts       [L1: +305419896], ax}
     */
    // Template#: 74, Serial#: 5997
    public void rip_bts(final Label label, final AMD64GeneralRegister16 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_bts(placeHolder, source);
        new rip_bts_5997(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code bts  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code bts       [L1: +305419896], eax}
     */
    // Template#: 75, Serial#: 5979
    public void rip_bts(final Label label, final AMD64GeneralRegister32 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_bts(placeHolder, source);
        new rip_bts_5979(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code bts  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code bts       [L1: +305419896], rax}
     */
    // Template#: 76, Serial#: 5988
    public void rip_bts(final Label label, final AMD64GeneralRegister64 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_bts(placeHolder, source);
        new rip_bts_5988(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code call  }<i>label</i>
     * Example disassembly syntax: {@code call      L1: +305419896}
     */
    // Template#: 77, Serial#: 2957
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
    // Template#: 78, Serial#: 3049
    public void rip_call(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_call(placeHolder);
        new rip_call_3049(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code clflush  }<i>label</i>
     * Example disassembly syntax: {@code clflush   [L1: +305419896]}
     */
    // Template#: 79, Serial#: 6076
    public void rip_clflush(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_clflush(placeHolder);
        new rip_clflush_6076(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmova  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmova     ax, [L1: +305419896]}
     */
    // Template#: 80, Serial#: 3649
    public void rip_cmova(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmova(destination, placeHolder);
        new rip_cmova_3649(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmova  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmova     eax, [L1: +305419896]}
     */
    // Template#: 81, Serial#: 3631
    public void rip_cmova(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmova(destination, placeHolder);
        new rip_cmova_3631(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmova  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmova     rax, [L1: +305419896]}
     */
    // Template#: 82, Serial#: 3640
    public void rip_cmova(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmova(destination, placeHolder);
        new rip_cmova_3640(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovae  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovae    ax, [L1: +305419896]}
     */
    // Template#: 83, Serial#: 3541
    public void rip_cmovae(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovae(destination, placeHolder);
        new rip_cmovae_3541(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovae  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovae    eax, [L1: +305419896]}
     */
    // Template#: 84, Serial#: 3523
    public void rip_cmovae(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovae(destination, placeHolder);
        new rip_cmovae_3523(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovae  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovae    rax, [L1: +305419896]}
     */
    // Template#: 85, Serial#: 3532
    public void rip_cmovae(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovae(destination, placeHolder);
        new rip_cmovae_3532(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovb     ax, [L1: +305419896]}
     */
    // Template#: 86, Serial#: 3514
    public void rip_cmovb(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovb(destination, placeHolder);
        new rip_cmovb_3514(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovb     eax, [L1: +305419896]}
     */
    // Template#: 87, Serial#: 3496
    public void rip_cmovb(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovb(destination, placeHolder);
        new rip_cmovb_3496(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovb     rax, [L1: +305419896]}
     */
    // Template#: 88, Serial#: 3505
    public void rip_cmovb(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovb(destination, placeHolder);
        new rip_cmovb_3505(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovbe  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovbe    ax, [L1: +305419896]}
     */
    // Template#: 89, Serial#: 3622
    public void rip_cmovbe(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovbe(destination, placeHolder);
        new rip_cmovbe_3622(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovbe  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovbe    eax, [L1: +305419896]}
     */
    // Template#: 90, Serial#: 3604
    public void rip_cmovbe(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovbe(destination, placeHolder);
        new rip_cmovbe_3604(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovbe  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovbe    rax, [L1: +305419896]}
     */
    // Template#: 91, Serial#: 3613
    public void rip_cmovbe(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovbe(destination, placeHolder);
        new rip_cmovbe_3613(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmove  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmove     ax, [L1: +305419896]}
     */
    // Template#: 92, Serial#: 3568
    public void rip_cmove(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmove(destination, placeHolder);
        new rip_cmove_3568(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmove  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmove     eax, [L1: +305419896]}
     */
    // Template#: 93, Serial#: 3550
    public void rip_cmove(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmove(destination, placeHolder);
        new rip_cmove_3550(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmove  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmove     rax, [L1: +305419896]}
     */
    // Template#: 94, Serial#: 3559
    public void rip_cmove(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmove(destination, placeHolder);
        new rip_cmove_3559(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovg  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovg     ax, [L1: +305419896]}
     */
    // Template#: 95, Serial#: 5351
    public void rip_cmovg(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovg(destination, placeHolder);
        new rip_cmovg_5351(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovg  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovg     eax, [L1: +305419896]}
     */
    // Template#: 96, Serial#: 5333
    public void rip_cmovg(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovg(destination, placeHolder);
        new rip_cmovg_5333(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovg  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovg     rax, [L1: +305419896]}
     */
    // Template#: 97, Serial#: 5342
    public void rip_cmovg(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovg(destination, placeHolder);
        new rip_cmovg_5342(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovge  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovge    ax, [L1: +305419896]}
     */
    // Template#: 98, Serial#: 5297
    public void rip_cmovge(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovge(destination, placeHolder);
        new rip_cmovge_5297(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovge  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovge    eax, [L1: +305419896]}
     */
    // Template#: 99, Serial#: 5279
    public void rip_cmovge(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovge(destination, placeHolder);
        new rip_cmovge_5279(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovge  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovge    rax, [L1: +305419896]}
     */
    // Template#: 100, Serial#: 5288
    public void rip_cmovge(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovge(destination, placeHolder);
        new rip_cmovge_5288(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovl  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovl     ax, [L1: +305419896]}
     */
    // Template#: 101, Serial#: 5270
    public void rip_cmovl(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovl(destination, placeHolder);
        new rip_cmovl_5270(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovl  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovl     eax, [L1: +305419896]}
     */
    // Template#: 102, Serial#: 5252
    public void rip_cmovl(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovl(destination, placeHolder);
        new rip_cmovl_5252(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovl  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovl     rax, [L1: +305419896]}
     */
    // Template#: 103, Serial#: 5261
    public void rip_cmovl(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovl(destination, placeHolder);
        new rip_cmovl_5261(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovle  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovle    ax, [L1: +305419896]}
     */
    // Template#: 104, Serial#: 5324
    public void rip_cmovle(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovle(destination, placeHolder);
        new rip_cmovle_5324(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovle  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovle    eax, [L1: +305419896]}
     */
    // Template#: 105, Serial#: 5306
    public void rip_cmovle(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovle(destination, placeHolder);
        new rip_cmovle_5306(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovle  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovle    rax, [L1: +305419896]}
     */
    // Template#: 106, Serial#: 5315
    public void rip_cmovle(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovle(destination, placeHolder);
        new rip_cmovle_5315(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovne  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovne    ax, [L1: +305419896]}
     */
    // Template#: 107, Serial#: 3595
    public void rip_cmovne(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovne(destination, placeHolder);
        new rip_cmovne_3595(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovne  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovne    eax, [L1: +305419896]}
     */
    // Template#: 108, Serial#: 3577
    public void rip_cmovne(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovne(destination, placeHolder);
        new rip_cmovne_3577(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovne  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovne    rax, [L1: +305419896]}
     */
    // Template#: 109, Serial#: 3586
    public void rip_cmovne(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovne(destination, placeHolder);
        new rip_cmovne_3586(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovno  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovno    ax, [L1: +305419896]}
     */
    // Template#: 110, Serial#: 3487
    public void rip_cmovno(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovno(destination, placeHolder);
        new rip_cmovno_3487(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovno  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovno    eax, [L1: +305419896]}
     */
    // Template#: 111, Serial#: 3469
    public void rip_cmovno(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovno(destination, placeHolder);
        new rip_cmovno_3469(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovno  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovno    rax, [L1: +305419896]}
     */
    // Template#: 112, Serial#: 3478
    public void rip_cmovno(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovno(destination, placeHolder);
        new rip_cmovno_3478(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovnp  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovnp    ax, [L1: +305419896]}
     */
    // Template#: 113, Serial#: 5243
    public void rip_cmovnp(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovnp(destination, placeHolder);
        new rip_cmovnp_5243(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovnp  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovnp    eax, [L1: +305419896]}
     */
    // Template#: 114, Serial#: 5225
    public void rip_cmovnp(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovnp(destination, placeHolder);
        new rip_cmovnp_5225(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovnp  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovnp    rax, [L1: +305419896]}
     */
    // Template#: 115, Serial#: 5234
    public void rip_cmovnp(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovnp(destination, placeHolder);
        new rip_cmovnp_5234(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovns  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovns    ax, [L1: +305419896]}
     */
    // Template#: 116, Serial#: 5189
    public void rip_cmovns(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovns(destination, placeHolder);
        new rip_cmovns_5189(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovns  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovns    eax, [L1: +305419896]}
     */
    // Template#: 117, Serial#: 5171
    public void rip_cmovns(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovns(destination, placeHolder);
        new rip_cmovns_5171(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovns  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovns    rax, [L1: +305419896]}
     */
    // Template#: 118, Serial#: 5180
    public void rip_cmovns(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovns(destination, placeHolder);
        new rip_cmovns_5180(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovo  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovo     ax, [L1: +305419896]}
     */
    // Template#: 119, Serial#: 3460
    public void rip_cmovo(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovo(destination, placeHolder);
        new rip_cmovo_3460(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovo  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovo     eax, [L1: +305419896]}
     */
    // Template#: 120, Serial#: 3442
    public void rip_cmovo(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovo(destination, placeHolder);
        new rip_cmovo_3442(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovo  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovo     rax, [L1: +305419896]}
     */
    // Template#: 121, Serial#: 3451
    public void rip_cmovo(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovo(destination, placeHolder);
        new rip_cmovo_3451(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovp  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovp     ax, [L1: +305419896]}
     */
    // Template#: 122, Serial#: 5216
    public void rip_cmovp(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovp(destination, placeHolder);
        new rip_cmovp_5216(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovp  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovp     eax, [L1: +305419896]}
     */
    // Template#: 123, Serial#: 5198
    public void rip_cmovp(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovp(destination, placeHolder);
        new rip_cmovp_5198(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovp  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovp     rax, [L1: +305419896]}
     */
    // Template#: 124, Serial#: 5207
    public void rip_cmovp(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovp(destination, placeHolder);
        new rip_cmovp_5207(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovs  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovs     ax, [L1: +305419896]}
     */
    // Template#: 125, Serial#: 5162
    public void rip_cmovs(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovs(destination, placeHolder);
        new rip_cmovs_5162(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovs  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovs     eax, [L1: +305419896]}
     */
    // Template#: 126, Serial#: 5144
    public void rip_cmovs(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovs(destination, placeHolder);
        new rip_cmovs_5144(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmovs  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmovs     rax, [L1: +305419896]}
     */
    // Template#: 127, Serial#: 5153
    public void rip_cmovs(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmovs(destination, placeHolder);
        new rip_cmovs_5153(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmp       ax, [L1: +305419896]}
     */
    // Template#: 128, Serial#: 2264
    public void rip_cmp(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmp(destination, placeHolder);
        new rip_cmp_2264(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmp       eax, [L1: +305419896]}
     */
    // Template#: 129, Serial#: 2248
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
    // Template#: 130, Serial#: 2256
    public void rip_cmp(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmp(destination, placeHolder);
        new rip_cmp_2256(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cmp       al, [L1: +305419896]}
     */
    // Template#: 131, Serial#: 2240
    public void rip_cmp(final AMD64GeneralRegister8 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmp(destination, placeHolder);
        new rip_cmp_2240(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpb  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code cmpb      [L1: +305419896], 0x12}
     */
    // Template#: 132, Serial#: 352
    public void rip_cmpb(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmpb(placeHolder, imm8);
        new rip_cmpb_352(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpl  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code cmpl      [L1: +305419896], 0x12}
     */
    // Template#: 133, Serial#: 640
    public void rip_cmpl(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmpl(placeHolder, imm8);
        new rip_cmpl_640(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpq  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code cmpq      [L1: +305419896], 0x12}
     */
    // Template#: 134, Serial#: 712
    public void rip_cmpq(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmpq(placeHolder, imm8);
        new rip_cmpq_712(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpw  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code cmpw      [L1: +305419896], 0x12}
     */
    // Template#: 135, Serial#: 784
    public void rip_cmpw(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmpw(placeHolder, imm8);
        new rip_cmpw_784(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code cmp       [L1: +305419896], ax}
     */
    // Template#: 136, Serial#: 2231
    public void rip_cmp(final Label label, final AMD64GeneralRegister16 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmp(placeHolder, source);
        new rip_cmp_2231(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code cmp       [L1: +305419896], eax}
     */
    // Template#: 137, Serial#: 2213
    public void rip_cmp(final Label label, final AMD64GeneralRegister32 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmp(placeHolder, source);
        new rip_cmp_2213(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code cmp       [L1: +305419896], rax}
     */
    // Template#: 138, Serial#: 2222
    public void rip_cmp(final Label label, final AMD64GeneralRegister64 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmp(placeHolder, source);
        new rip_cmp_2222(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmp  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code cmp       [L1: +305419896], al}
     */
    // Template#: 139, Serial#: 2204
    public void rip_cmp(final Label label, final AMD64GeneralRegister8 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmp(placeHolder, source);
        new rip_cmp_2204(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpl  }<i>label</i>, <i>imm32</i>
     * Example disassembly syntax: {@code cmpl      [L1: +305419896], 0x12345678}
     */
    // Template#: 140, Serial#: 424
    public void rip_cmpl(final Label label, final int imm32) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmpl(placeHolder, imm32);
        new rip_cmpl_424(startPosition, currentPosition() - startPosition, imm32, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpq  }<i>label</i>, <i>imm32</i>
     * Example disassembly syntax: {@code cmpq      [L1: +305419896], 0x12345678}
     */
    // Template#: 141, Serial#: 496
    public void rip_cmpq(final Label label, final int imm32) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmpq(placeHolder, imm32);
        new rip_cmpq_496(startPosition, currentPosition() - startPosition, imm32, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpw  }<i>label</i>, <i>imm16</i>
     * Example disassembly syntax: {@code cmpw      [L1: +305419896], 0x1234}
     */
    // Template#: 142, Serial#: 568
    public void rip_cmpw(final Label label, final short imm16) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmpw(placeHolder, imm16);
        new rip_cmpw_568(startPosition, currentPosition() - startPosition, imm16, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmppd  }<i>destination</i>, <i>label</i>, <i>amd64xmmcomparison</i>
     * Example disassembly syntax: {@code cmppd     xmm0, [L1: +305419896], less_than_or_equal}
     */
    // Template#: 143, Serial#: 4417
    public void rip_cmppd(final AMD64XMMRegister destination, final Label label, final AMD64XMMComparison amd64xmmcomparison) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmppd(destination, placeHolder, amd64xmmcomparison);
        new rip_cmppd_4417(startPosition, currentPosition() - startPosition, destination, amd64xmmcomparison, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpps  }<i>destination</i>, <i>label</i>, <i>amd64xmmcomparison</i>
     * Example disassembly syntax: {@code cmpps     xmm0, [L1: +305419896], less_than_or_equal}
     */
    // Template#: 144, Serial#: 4365
    public void rip_cmpps(final AMD64XMMRegister destination, final Label label, final AMD64XMMComparison amd64xmmcomparison) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmpps(destination, placeHolder, amd64xmmcomparison);
        new rip_cmpps_4365(startPosition, currentPosition() - startPosition, destination, amd64xmmcomparison, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpsd  }<i>destination</i>, <i>label</i>, <i>amd64xmmcomparison</i>
     * Example disassembly syntax: {@code cmpsd     xmm0, [L1: +305419896], less_than_or_equal}
     */
    // Template#: 145, Serial#: 4445
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
    // Template#: 146, Serial#: 4454
    public void rip_cmpss(final AMD64XMMRegister destination, final Label label, final AMD64XMMComparison amd64xmmcomparison) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmpss(destination, placeHolder, amd64xmmcomparison);
        new rip_cmpss_4454(startPosition, currentPosition() - startPosition, destination, amd64xmmcomparison, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpxchg  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code cmpxchg   [L1: +305419896], ax}
     */
    // Template#: 147, Serial#: 4248
    public void rip_cmpxchg(final Label label, final AMD64GeneralRegister16 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmpxchg(placeHolder, source);
        new rip_cmpxchg_4248(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpxchg  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code cmpxchg   [L1: +305419896], eax}
     */
    // Template#: 148, Serial#: 4230
    public void rip_cmpxchg(final Label label, final AMD64GeneralRegister32 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmpxchg(placeHolder, source);
        new rip_cmpxchg_4230(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpxchg  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code cmpxchg   [L1: +305419896], rax}
     */
    // Template#: 149, Serial#: 4239
    public void rip_cmpxchg(final Label label, final AMD64GeneralRegister64 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmpxchg(placeHolder, source);
        new rip_cmpxchg_4239(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpxchg  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code cmpxchg   [L1: +305419896], al}
     */
    // Template#: 150, Serial#: 4221
    public void rip_cmpxchg(final Label label, final AMD64GeneralRegister8 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmpxchg(placeHolder, source);
        new rip_cmpxchg_4221(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cmpxchg16b  }<i>label</i>
     * Example disassembly syntax: {@code cmpxchg16b  [L1: +305419896]}
     */
    // Template#: 151, Serial#: 4409
    public void rip_cmpxchg16b(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cmpxchg16b(placeHolder);
        new rip_cmpxchg16b_4409(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code comisd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code comisd    xmm0, [L1: +305419896]}
     */
    // Template#: 152, Serial#: 5027
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
    // Template#: 153, Serial#: 4957
    public void rip_comiss(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_comiss(destination, placeHolder);
        new rip_comiss_4957(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtdq2pd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtdq2pd  xmm0, [L1: +305419896]}
     */
    // Template#: 154, Serial#: 4717
    public void rip_cvtdq2pd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtdq2pd(destination, placeHolder);
        new rip_cvtdq2pd_4717(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtdq2ps  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtdq2ps  xmm0, [L1: +305419896]}
     */
    // Template#: 155, Serial#: 5387
    public void rip_cvtdq2ps(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtdq2ps(destination, placeHolder);
        new rip_cvtdq2ps_5387(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtpd2dq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtpd2dq  xmm0, [L1: +305419896]}
     */
    // Template#: 156, Serial#: 4708
    public void rip_cvtpd2dq(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtpd2dq(destination, placeHolder);
        new rip_cvtpd2dq_4708(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtpd2pi  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtpd2pi  mm0, [L1: +305419896]}
     */
    // Template#: 157, Serial#: 5009
    public void rip_cvtpd2pi(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtpd2pi(destination, placeHolder);
        new rip_cvtpd2pi_5009(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtpd2ps  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtpd2ps  xmm0, [L1: +305419896]}
     */
    // Template#: 158, Serial#: 5450
    public void rip_cvtpd2ps(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtpd2ps(destination, placeHolder);
        new rip_cvtpd2ps_5450(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtpi2pd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtpi2pd  xmm0, [L1: +305419896]}
     */
    // Template#: 159, Serial#: 4983
    public void rip_cvtpi2pd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtpi2pd(destination, placeHolder);
        new rip_cvtpi2pd_4983(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtpi2ps  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtpi2ps  xmm0, [L1: +305419896]}
     */
    // Template#: 160, Serial#: 4913
    public void rip_cvtpi2ps(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtpi2ps(destination, placeHolder);
        new rip_cvtpi2ps_4913(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtps2dq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtps2dq  xmm0, [L1: +305419896]}
     */
    // Template#: 161, Serial#: 5459
    public void rip_cvtps2dq(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtps2dq(destination, placeHolder);
        new rip_cvtps2dq_5459(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtps2pd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtps2pd  xmm0, [L1: +305419896]}
     */
    // Template#: 162, Serial#: 5378
    public void rip_cvtps2pd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtps2pd(destination, placeHolder);
        new rip_cvtps2pd_5378(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtps2pi  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtps2pi  mm0, [L1: +305419896]}
     */
    // Template#: 163, Serial#: 4939
    public void rip_cvtps2pi(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtps2pi(destination, placeHolder);
        new rip_cvtps2pi_4939(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvtsd2si  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvtsd2si  eax, [L1: +305419896]}
     */
    // Template#: 164, Serial#: 5072
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
    // Template#: 165, Serial#: 5081
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
    // Template#: 166, Serial#: 5522
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
    // Template#: 167, Serial#: 5036
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
    // Template#: 168, Serial#: 5045
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
    // Template#: 169, Serial#: 5090
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
    // Template#: 170, Serial#: 5099
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
    // Template#: 171, Serial#: 5585
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
    // Template#: 172, Serial#: 5126
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
    // Template#: 173, Serial#: 5135
    public void rip_cvtss2si(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvtss2si(destination, placeHolder);
        new rip_cvtss2si_5135(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttpd2dq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvttpd2dq  xmm0, [L1: +305419896]}
     */
    // Template#: 174, Serial#: 4691
    public void rip_cvttpd2dq(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvttpd2dq(destination, placeHolder);
        new rip_cvttpd2dq_4691(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttpd2pi  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvttpd2pi  mm0, [L1: +305419896]}
     */
    // Template#: 175, Serial#: 5000
    public void rip_cvttpd2pi(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvttpd2pi(destination, placeHolder);
        new rip_cvttpd2pi_5000(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttps2dq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvttps2dq  xmm0, [L1: +305419896]}
     */
    // Template#: 176, Serial#: 5594
    public void rip_cvttps2dq(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvttps2dq(destination, placeHolder);
        new rip_cvttps2dq_5594(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttps2pi  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvttps2pi  mm0, [L1: +305419896]}
     */
    // Template#: 177, Serial#: 4930
    public void rip_cvttps2pi(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvttps2pi(destination, placeHolder);
        new rip_cvttps2pi_4930(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code cvttsd2si  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code cvttsd2si  eax, [L1: +305419896]}
     */
    // Template#: 178, Serial#: 5054
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
    // Template#: 179, Serial#: 5063
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
    // Template#: 180, Serial#: 5108
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
    // Template#: 181, Serial#: 5117
    public void rip_cvttss2si(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_cvttss2si(destination, placeHolder);
        new rip_cvttss2si_5117(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code decb  }<i>label</i>
     * Example disassembly syntax: {@code decb      [L1: +305419896]}
     */
    // Template#: 182, Serial#: 2981
    public void rip_decb(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_decb(placeHolder);
        new rip_decb_2981(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code decl  }<i>label</i>
     * Example disassembly syntax: {@code decl      [L1: +305419896]}
     */
    // Template#: 183, Serial#: 2999
    public void rip_decl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_decl(placeHolder);
        new rip_decl_2999(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code decq  }<i>label</i>
     * Example disassembly syntax: {@code decq      [L1: +305419896]}
     */
    // Template#: 184, Serial#: 3017
    public void rip_decq(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_decq(placeHolder);
        new rip_decq_3017(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code decw  }<i>label</i>
     * Example disassembly syntax: {@code decw      [L1: +305419896]}
     */
    // Template#: 185, Serial#: 3035
    public void rip_decw(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_decw(placeHolder);
        new rip_decw_3035(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code divb  }<i>label</i>
     * Example disassembly syntax: {@code divb      [L1: +305419896], al}
     */
    // Template#: 186, Serial#: 1754
    public void rip_divb___AL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_divb___AL(placeHolder);
        new rip_divb___AL_1754(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code divl  }<i>label</i>
     * Example disassembly syntax: {@code divl      [L1: +305419896]}
     */
    // Template#: 187, Serial#: 1817
    public void rip_divl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_divl(placeHolder);
        new rip_divl_1817(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code divq  }<i>label</i>
     * Example disassembly syntax: {@code divq      [L1: +305419896]}
     */
    // Template#: 188, Serial#: 1880
    public void rip_divq(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_divq(placeHolder);
        new rip_divq_1880(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code divw  }<i>label</i>
     * Example disassembly syntax: {@code divw      [L1: +305419896]}
     */
    // Template#: 189, Serial#: 1943
    public void rip_divw(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_divw(placeHolder);
        new rip_divw_1943(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code divpd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code divpd     xmm0, [L1: +305419896]}
     */
    // Template#: 190, Serial#: 5486
    public void rip_divpd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_divpd(destination, placeHolder);
        new rip_divpd_5486(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code divps  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code divps     xmm0, [L1: +305419896]}
     */
    // Template#: 191, Serial#: 5414
    public void rip_divps(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_divps(destination, placeHolder);
        new rip_divps_5414(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code divsd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code divsd     xmm0, [L1: +305419896]}
     */
    // Template#: 192, Serial#: 5549
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
    // Template#: 193, Serial#: 5621
    public void rip_divss(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_divss(destination, placeHolder);
        new rip_divss_5621(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fadds  }<i>label</i>
     * Example disassembly syntax: {@code fadds     [L1: +305419896]}
     */
    // Template#: 194, Serial#: 2504
    public void rip_fadds(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fadds(placeHolder);
        new rip_fadds_2504(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code faddl  }<i>label</i>
     * Example disassembly syntax: {@code faddl     [L1: +305419896]}
     */
    // Template#: 195, Serial#: 2728
    public void rip_faddl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_faddl(placeHolder);
        new rip_faddl_2728(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fbld  }<i>label</i>
     * Example disassembly syntax: {@code fbld      [L1: +305419896]}
     */
    // Template#: 196, Serial#: 2916
    public void rip_fbld(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fbld(placeHolder);
        new rip_fbld_2916(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fbstp  }<i>label</i>
     * Example disassembly syntax: {@code fbstp     [L1: +305419896]}
     */
    // Template#: 197, Serial#: 2924
    public void rip_fbstp(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fbstp(placeHolder);
        new rip_fbstp_2924(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fcoms  }<i>label</i>
     * Example disassembly syntax: {@code fcoms     [L1: +305419896]}
     */
    // Template#: 198, Serial#: 2512
    public void rip_fcoms(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fcoms(placeHolder);
        new rip_fcoms_2512(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fcoml  }<i>label</i>
     * Example disassembly syntax: {@code fcoml     [L1: +305419896]}
     */
    // Template#: 199, Serial#: 2736
    public void rip_fcoml(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fcoml(placeHolder);
        new rip_fcoml_2736(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fcomps  }<i>label</i>
     * Example disassembly syntax: {@code fcomps    [L1: +305419896]}
     */
    // Template#: 200, Serial#: 2516
    public void rip_fcomps(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fcomps(placeHolder);
        new rip_fcomps_2516(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fcompl  }<i>label</i>
     * Example disassembly syntax: {@code fcompl    [L1: +305419896]}
     */
    // Template#: 201, Serial#: 2740
    public void rip_fcompl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fcompl(placeHolder);
        new rip_fcompl_2740(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fdivs  }<i>label</i>
     * Example disassembly syntax: {@code fdivs     [L1: +305419896]}
     */
    // Template#: 202, Serial#: 2528
    public void rip_fdivs(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fdivs(placeHolder);
        new rip_fdivs_2528(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fdivl  }<i>label</i>
     * Example disassembly syntax: {@code fdivl     [L1: +305419896]}
     */
    // Template#: 203, Serial#: 2752
    public void rip_fdivl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fdivl(placeHolder);
        new rip_fdivl_2752(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fdivrs  }<i>label</i>
     * Example disassembly syntax: {@code fdivrs    [L1: +305419896]}
     */
    // Template#: 204, Serial#: 2532
    public void rip_fdivrs(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fdivrs(placeHolder);
        new rip_fdivrs_2532(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fdivrl  }<i>label</i>
     * Example disassembly syntax: {@code fdivrl    [L1: +305419896]}
     */
    // Template#: 205, Serial#: 2756
    public void rip_fdivrl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fdivrl(placeHolder);
        new rip_fdivrl_2756(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fiaddl  }<i>label</i>
     * Example disassembly syntax: {@code fiaddl    [L1: +305419896]}
     */
    // Template#: 206, Serial#: 2624
    public void rip_fiaddl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fiaddl(placeHolder);
        new rip_fiaddl_2624(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fiadds  }<i>label</i>
     * Example disassembly syntax: {@code fiadds    [L1: +305419896]}
     */
    // Template#: 207, Serial#: 2840
    public void rip_fiadds(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fiadds(placeHolder);
        new rip_fiadds_2840(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code ficoml  }<i>label</i>
     * Example disassembly syntax: {@code ficoml    [L1: +305419896]}
     */
    // Template#: 208, Serial#: 2632
    public void rip_ficoml(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_ficoml(placeHolder);
        new rip_ficoml_2632(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code ficoms  }<i>label</i>
     * Example disassembly syntax: {@code ficoms    [L1: +305419896]}
     */
    // Template#: 209, Serial#: 2848
    public void rip_ficoms(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_ficoms(placeHolder);
        new rip_ficoms_2848(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code ficompl  }<i>label</i>
     * Example disassembly syntax: {@code ficompl   [L1: +305419896]}
     */
    // Template#: 210, Serial#: 2636
    public void rip_ficompl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_ficompl(placeHolder);
        new rip_ficompl_2636(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code ficomps  }<i>label</i>
     * Example disassembly syntax: {@code ficomps   [L1: +305419896]}
     */
    // Template#: 211, Serial#: 2852
    public void rip_ficomps(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_ficomps(placeHolder);
        new rip_ficomps_2852(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fidivl  }<i>label</i>
     * Example disassembly syntax: {@code fidivl    [L1: +305419896]}
     */
    // Template#: 212, Serial#: 2648
    public void rip_fidivl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fidivl(placeHolder);
        new rip_fidivl_2648(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fidivs  }<i>label</i>
     * Example disassembly syntax: {@code fidivs    [L1: +305419896]}
     */
    // Template#: 213, Serial#: 2864
    public void rip_fidivs(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fidivs(placeHolder);
        new rip_fidivs_2864(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fidivrl  }<i>label</i>
     * Example disassembly syntax: {@code fidivrl   [L1: +305419896]}
     */
    // Template#: 214, Serial#: 2652
    public void rip_fidivrl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fidivrl(placeHolder);
        new rip_fidivrl_2652(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fidivrs  }<i>label</i>
     * Example disassembly syntax: {@code fidivrs   [L1: +305419896]}
     */
    // Template#: 215, Serial#: 2868
    public void rip_fidivrs(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fidivrs(placeHolder);
        new rip_fidivrs_2868(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fildl  }<i>label</i>
     * Example disassembly syntax: {@code fildl     [L1: +305419896]}
     */
    // Template#: 216, Serial#: 2688
    public void rip_fildl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fildl(placeHolder);
        new rip_fildl_2688(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code filds  }<i>label</i>
     * Example disassembly syntax: {@code filds     [L1: +305419896]}
     */
    // Template#: 217, Serial#: 2904
    public void rip_filds(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_filds(placeHolder);
        new rip_filds_2904(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fildq  }<i>label</i>
     * Example disassembly syntax: {@code fildq     [L1: +305419896]}
     */
    // Template#: 218, Serial#: 2920
    public void rip_fildq(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fildq(placeHolder);
        new rip_fildq_2920(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fimull  }<i>label</i>
     * Example disassembly syntax: {@code fimull    [L1: +305419896]}
     */
    // Template#: 219, Serial#: 2628
    public void rip_fimull(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fimull(placeHolder);
        new rip_fimull_2628(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fimuls  }<i>label</i>
     * Example disassembly syntax: {@code fimuls    [L1: +305419896]}
     */
    // Template#: 220, Serial#: 2844
    public void rip_fimuls(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fimuls(placeHolder);
        new rip_fimuls_2844(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fistl  }<i>label</i>
     * Example disassembly syntax: {@code fistl     [L1: +305419896]}
     */
    // Template#: 221, Serial#: 2692
    public void rip_fistl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fistl(placeHolder);
        new rip_fistl_2692(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fists  }<i>label</i>
     * Example disassembly syntax: {@code fists     [L1: +305419896]}
     */
    // Template#: 222, Serial#: 2908
    public void rip_fists(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fists(placeHolder);
        new rip_fists_2908(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fistpl  }<i>label</i>
     * Example disassembly syntax: {@code fistpl    [L1: +305419896]}
     */
    // Template#: 223, Serial#: 2696
    public void rip_fistpl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fistpl(placeHolder);
        new rip_fistpl_2696(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fistps  }<i>label</i>
     * Example disassembly syntax: {@code fistps    [L1: +305419896]}
     */
    // Template#: 224, Serial#: 2912
    public void rip_fistps(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fistps(placeHolder);
        new rip_fistps_2912(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fistpq  }<i>label</i>
     * Example disassembly syntax: {@code fistpq    [L1: +305419896]}
     */
    // Template#: 225, Serial#: 2928
    public void rip_fistpq(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fistpq(placeHolder);
        new rip_fistpq_2928(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fisubl  }<i>label</i>
     * Example disassembly syntax: {@code fisubl    [L1: +305419896]}
     */
    // Template#: 226, Serial#: 2640
    public void rip_fisubl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fisubl(placeHolder);
        new rip_fisubl_2640(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fisubs  }<i>label</i>
     * Example disassembly syntax: {@code fisubs    [L1: +305419896]}
     */
    // Template#: 227, Serial#: 2856
    public void rip_fisubs(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fisubs(placeHolder);
        new rip_fisubs_2856(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fisubrl  }<i>label</i>
     * Example disassembly syntax: {@code fisubrl   [L1: +305419896]}
     */
    // Template#: 228, Serial#: 2644
    public void rip_fisubrl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fisubrl(placeHolder);
        new rip_fisubrl_2644(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fisubrs  }<i>label</i>
     * Example disassembly syntax: {@code fisubrs   [L1: +305419896]}
     */
    // Template#: 229, Serial#: 2860
    public void rip_fisubrs(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fisubrs(placeHolder);
        new rip_fisubrs_2860(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code flds  }<i>label</i>
     * Example disassembly syntax: {@code flds      [L1: +305419896]}
     */
    // Template#: 230, Serial#: 2568
    public void rip_flds(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_flds(placeHolder);
        new rip_flds_2568(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fldt  }<i>label</i>
     * Example disassembly syntax: {@code fldt      [L1: +305419896]}
     */
    // Template#: 231, Serial#: 2700
    public void rip_fldt(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fldt(placeHolder);
        new rip_fldt_2700(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fldl  }<i>label</i>
     * Example disassembly syntax: {@code fldl      [L1: +305419896]}
     */
    // Template#: 232, Serial#: 2792
    public void rip_fldl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fldl(placeHolder);
        new rip_fldl_2792(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fldcw  }<i>label</i>
     * Example disassembly syntax: {@code fldcw     [L1: +305419896]}
     */
    // Template#: 233, Serial#: 2584
    public void rip_fldcw(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fldcw(placeHolder);
        new rip_fldcw_2584(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fldenv  }<i>label</i>
     * Example disassembly syntax: {@code fldenv    [L1: +305419896]}
     */
    // Template#: 234, Serial#: 2580
    public void rip_fldenv(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fldenv(placeHolder);
        new rip_fldenv_2580(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fmuls  }<i>label</i>
     * Example disassembly syntax: {@code fmuls     [L1: +305419896]}
     */
    // Template#: 235, Serial#: 2508
    public void rip_fmuls(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fmuls(placeHolder);
        new rip_fmuls_2508(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fmull  }<i>label</i>
     * Example disassembly syntax: {@code fmull     [L1: +305419896]}
     */
    // Template#: 236, Serial#: 2732
    public void rip_fmull(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fmull(placeHolder);
        new rip_fmull_2732(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code frstor  }<i>label</i>
     * Example disassembly syntax: {@code frstor    [L1: +305419896]}
     */
    // Template#: 237, Serial#: 2804
    public void rip_frstor(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_frstor(placeHolder);
        new rip_frstor_2804(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fsave  }<i>label</i>
     * Example disassembly syntax: {@code fsave     [L1: +305419896]}
     */
    // Template#: 238, Serial#: 2808
    public void rip_fsave(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fsave(placeHolder);
        new rip_fsave_2808(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fsts  }<i>label</i>
     * Example disassembly syntax: {@code fsts      [L1: +305419896]}
     */
    // Template#: 239, Serial#: 2572
    public void rip_fsts(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fsts(placeHolder);
        new rip_fsts_2572(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fstl  }<i>label</i>
     * Example disassembly syntax: {@code fstl      [L1: +305419896]}
     */
    // Template#: 240, Serial#: 2796
    public void rip_fstl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fstl(placeHolder);
        new rip_fstl_2796(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fstcw  }<i>label</i>
     * Example disassembly syntax: {@code fstcw     [L1: +305419896]}
     */
    // Template#: 241, Serial#: 2592
    public void rip_fstcw(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fstcw(placeHolder);
        new rip_fstcw_2592(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fstenv  }<i>label</i>
     * Example disassembly syntax: {@code fstenv    [L1: +305419896]}
     */
    // Template#: 242, Serial#: 2588
    public void rip_fstenv(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fstenv(placeHolder);
        new rip_fstenv_2588(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fstps  }<i>label</i>
     * Example disassembly syntax: {@code fstps     [L1: +305419896]}
     */
    // Template#: 243, Serial#: 2576
    public void rip_fstps(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fstps(placeHolder);
        new rip_fstps_2576(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fstpt  }<i>label</i>
     * Example disassembly syntax: {@code fstpt     [L1: +305419896]}
     */
    // Template#: 244, Serial#: 2704
    public void rip_fstpt(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fstpt(placeHolder);
        new rip_fstpt_2704(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fstpl  }<i>label</i>
     * Example disassembly syntax: {@code fstpl     [L1: +305419896]}
     */
    // Template#: 245, Serial#: 2800
    public void rip_fstpl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fstpl(placeHolder);
        new rip_fstpl_2800(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fstsw  }<i>label</i>
     * Example disassembly syntax: {@code fstsw     [L1: +305419896]}
     */
    // Template#: 246, Serial#: 2812
    public void rip_fstsw(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fstsw(placeHolder);
        new rip_fstsw_2812(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fsubs  }<i>label</i>
     * Example disassembly syntax: {@code fsubs     [L1: +305419896]}
     */
    // Template#: 247, Serial#: 2520
    public void rip_fsubs(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fsubs(placeHolder);
        new rip_fsubs_2520(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fsubl  }<i>label</i>
     * Example disassembly syntax: {@code fsubl     [L1: +305419896]}
     */
    // Template#: 248, Serial#: 2744
    public void rip_fsubl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fsubl(placeHolder);
        new rip_fsubl_2744(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fsubrs  }<i>label</i>
     * Example disassembly syntax: {@code fsubrs    [L1: +305419896]}
     */
    // Template#: 249, Serial#: 2524
    public void rip_fsubrs(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fsubrs(placeHolder);
        new rip_fsubrs_2524(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fsubrl  }<i>label</i>
     * Example disassembly syntax: {@code fsubrl    [L1: +305419896]}
     */
    // Template#: 250, Serial#: 2748
    public void rip_fsubrl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fsubrl(placeHolder);
        new rip_fsubrl_2748(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fxrstor  }<i>label</i>
     * Example disassembly syntax: {@code fxrstor   [L1: +305419896]}
     */
    // Template#: 251, Serial#: 6064
    public void rip_fxrstor(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fxrstor(placeHolder);
        new rip_fxrstor_6064(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code fxsave  }<i>label</i>
     * Example disassembly syntax: {@code fxsave    [L1: +305419896]}
     */
    // Template#: 252, Serial#: 6060
    public void rip_fxsave(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_fxsave(placeHolder);
        new rip_fxsave_6060(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code haddpd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code haddpd    xmm0, [L1: +305419896]}
     */
    // Template#: 253, Serial#: 5818
    public void rip_haddpd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_haddpd(destination, placeHolder);
        new rip_haddpd_5818(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code haddps  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code haddps    xmm0, [L1: +305419896]}
     */
    // Template#: 254, Serial#: 5862
    public void rip_haddps(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_haddps(destination, placeHolder);
        new rip_haddps_5862(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code hsubpd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code hsubpd    xmm0, [L1: +305419896]}
     */
    // Template#: 255, Serial#: 5827
    public void rip_hsubpd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_hsubpd(destination, placeHolder);
        new rip_hsubpd_5827(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code hsubps  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code hsubps    xmm0, [L1: +305419896]}
     */
    // Template#: 256, Serial#: 5871
    public void rip_hsubps(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_hsubps(destination, placeHolder);
        new rip_hsubps_5871(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code idivb  }<i>label</i>
     * Example disassembly syntax: {@code idivb     [L1: +305419896], al}
     */
    // Template#: 257, Serial#: 1758
    public void rip_idivb___AL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_idivb___AL(placeHolder);
        new rip_idivb___AL_1758(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code idivl  }<i>label</i>
     * Example disassembly syntax: {@code idivl     [L1: +305419896]}
     */
    // Template#: 258, Serial#: 1821
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
    // Template#: 259, Serial#: 1884
    public void rip_idivq(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_idivq(placeHolder);
        new rip_idivq_1884(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code idivw  }<i>label</i>
     * Example disassembly syntax: {@code idivw     [L1: +305419896]}
     */
    // Template#: 260, Serial#: 1947
    public void rip_idivw(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_idivw(placeHolder);
        new rip_idivw_1947(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code imul      ax, [L1: +305419896]}
     */
    // Template#: 261, Serial#: 6121
    public void rip_imul(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_imul(destination, placeHolder);
        new rip_imul_6121(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code imul      ax, [L1: +305419896], 0x12}
     */
    // Template#: 262, Serial#: 2326
    public void rip_imul(final AMD64GeneralRegister16 destination, final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_imul(destination, placeHolder, imm8);
        new rip_imul_2326(startPosition, currentPosition() - startPosition, destination, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>label</i>, <i>imm16</i>
     * Example disassembly syntax: {@code imul      ax, [L1: +305419896], 0x1234}
     */
    // Template#: 263, Serial#: 2298
    public void rip_imul(final AMD64GeneralRegister16 destination, final Label label, final short imm16) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_imul(destination, placeHolder, imm16);
        new rip_imul_2298(startPosition, currentPosition() - startPosition, destination, imm16, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code imul      eax, [L1: +305419896]}
     */
    // Template#: 264, Serial#: 6103
    public void rip_imul(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_imul(destination, placeHolder);
        new rip_imul_6103(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code imul      eax, [L1: +305419896], 0x12}
     */
    // Template#: 265, Serial#: 2308
    public void rip_imul(final AMD64GeneralRegister32 destination, final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_imul(destination, placeHolder, imm8);
        new rip_imul_2308(startPosition, currentPosition() - startPosition, destination, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>label</i>, <i>imm32</i>
     * Example disassembly syntax: {@code imul      eax, [L1: +305419896], 0x12345678}
     */
    // Template#: 266, Serial#: 2280
    public void rip_imul(final AMD64GeneralRegister32 destination, final Label label, final int imm32) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_imul(destination, placeHolder, imm32);
        new rip_imul_2280(startPosition, currentPosition() - startPosition, destination, imm32, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code imul      rax, [L1: +305419896]}
     */
    // Template#: 267, Serial#: 6112
    public void rip_imul(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_imul(destination, placeHolder);
        new rip_imul_6112(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code imul      rax, [L1: +305419896], 0x12}
     */
    // Template#: 268, Serial#: 2317
    public void rip_imul(final AMD64GeneralRegister64 destination, final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_imul(destination, placeHolder, imm8);
        new rip_imul_2317(startPosition, currentPosition() - startPosition, destination, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code imul  }<i>destination</i>, <i>label</i>, <i>imm32</i>
     * Example disassembly syntax: {@code imul      rax, [L1: +305419896], 0x12345678}
     */
    // Template#: 269, Serial#: 2289
    public void rip_imul(final AMD64GeneralRegister64 destination, final Label label, final int imm32) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_imul(destination, placeHolder, imm32);
        new rip_imul_2289(startPosition, currentPosition() - startPosition, destination, imm32, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code imulb  }<i>label</i>
     * Example disassembly syntax: {@code imulb     [L1: +305419896], al}
     */
    // Template#: 270, Serial#: 1750
    public void rip_imulb___AL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_imulb___AL(placeHolder);
        new rip_imulb___AL_1750(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code imull  }<i>label</i>
     * Example disassembly syntax: {@code imull     [L1: +305419896]}
     */
    // Template#: 271, Serial#: 1813
    public void rip_imull(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_imull(placeHolder);
        new rip_imull_1813(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code imulq  }<i>label</i>
     * Example disassembly syntax: {@code imulq     [L1: +305419896]}
     */
    // Template#: 272, Serial#: 1876
    public void rip_imulq(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_imulq(placeHolder);
        new rip_imulq_1876(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code imulw  }<i>label</i>
     * Example disassembly syntax: {@code imulw     [L1: +305419896]}
     */
    // Template#: 273, Serial#: 1939
    public void rip_imulw(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_imulw(placeHolder);
        new rip_imulw_1939(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code incb  }<i>label</i>
     * Example disassembly syntax: {@code incb      [L1: +305419896]}
     */
    // Template#: 274, Serial#: 2977
    public void rip_incb(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_incb(placeHolder);
        new rip_incb_2977(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code incl  }<i>label</i>
     * Example disassembly syntax: {@code incl      [L1: +305419896]}
     */
    // Template#: 275, Serial#: 2995
    public void rip_incl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_incl(placeHolder);
        new rip_incl_2995(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code incq  }<i>label</i>
     * Example disassembly syntax: {@code incq      [L1: +305419896]}
     */
    // Template#: 276, Serial#: 3013
    public void rip_incq(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_incq(placeHolder);
        new rip_incq_3013(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code incw  }<i>label</i>
     * Example disassembly syntax: {@code incw      [L1: +305419896]}
     */
    // Template#: 277, Serial#: 3031
    public void rip_incw(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_incw(placeHolder);
        new rip_incw_3031(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code invlpg  }<i>label</i>
     * Example disassembly syntax: {@code invlpg    [L1: +305419896]}
     */
    // Template#: 278, Serial#: 3159
    public void rip_invlpg(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_invlpg(placeHolder);
        new rip_invlpg_3159(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jb  }<i>label</i>
     * Example disassembly syntax: {@code jb        L1: +18}
     */
    // Template#: 279, Serial#: 315
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
    // Template#: 280, Serial#: 319
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
    // Template#: 281, Serial#: 2342
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
    // Template#: 282, Serial#: 2344
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
    // Template#: 283, Serial#: 2959
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
    // Template#: 284, Serial#: 3053
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
    // Template#: 285, Serial#: 316
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
    // Template#: 286, Serial#: 320
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
    // Template#: 287, Serial#: 2343
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
    // Template#: 288, Serial#: 2345
    public void jnle(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jnle(placeHolder);
        new jnle_2345(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jno  }<i>label</i>
     * Example disassembly syntax: {@code jno       L1: +18}
     */
    // Template#: 289, Serial#: 314
    public void jno(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jno(placeHolder);
        new jno_314(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnp  }<i>label</i>
     * Example disassembly syntax: {@code jnp       L1: +18}
     */
    // Template#: 290, Serial#: 2341
    public void jnp(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jnp(placeHolder);
        new jnp_2341(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jns  }<i>label</i>
     * Example disassembly syntax: {@code jns       L1: +18}
     */
    // Template#: 291, Serial#: 2339
    public void jns(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jns(placeHolder);
        new jns_2339(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jnz  }<i>label</i>
     * Example disassembly syntax: {@code jnz       L1: +18}
     */
    // Template#: 292, Serial#: 318
    public void jnz(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jnz(placeHolder);
        new jnz_318(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jo  }<i>label</i>
     * Example disassembly syntax: {@code jo        L1: +18}
     */
    // Template#: 293, Serial#: 313
    public void jo(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jo(placeHolder);
        new jo_313(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jp  }<i>label</i>
     * Example disassembly syntax: {@code jp        L1: +18}
     */
    // Template#: 294, Serial#: 2340
    public void jp(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jp(placeHolder);
        new jp_2340(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jrcxz  }<i>label</i>
     * Example disassembly syntax: {@code jrcxz     L1: +18}
     */
    // Template#: 295, Serial#: 1716
    public void jrcxz(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jrcxz(placeHolder);
        new jrcxz_1716(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code js  }<i>label</i>
     * Example disassembly syntax: {@code js        L1: +18}
     */
    // Template#: 296, Serial#: 2338
    public void js(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        js(placeHolder);
        new js_2338(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code jz  }<i>label</i>
     * Example disassembly syntax: {@code jz        L1: +18}
     */
    // Template#: 297, Serial#: 317
    public void jz(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        jz(placeHolder);
        new jz_317(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code lar  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code lar       ax, [L1: +305419896]}
     */
    // Template#: 298, Serial#: 3214
    public void rip_lar(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_lar(destination, placeHolder);
        new rip_lar_3214(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code lar  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code lar       eax, [L1: +305419896]}
     */
    // Template#: 299, Serial#: 3196
    public void rip_lar(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_lar(destination, placeHolder);
        new rip_lar_3196(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code lar  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code lar       rax, [L1: +305419896]}
     */
    // Template#: 300, Serial#: 3205
    public void rip_lar(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_lar(destination, placeHolder);
        new rip_lar_3205(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code lddqu  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code lddqu     xmm0, [L1: +305419896]}
     */
    // Template#: 301, Serial#: 4836
    public void rip_lddqu(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_lddqu(destination, placeHolder);
        new rip_lddqu_4836(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code ldmxcsr  }<i>label</i>
     * Example disassembly syntax: {@code ldmxcsr   [L1: +305419896]}
     */
    // Template#: 302, Serial#: 6068
    public void rip_ldmxcsr(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_ldmxcsr(placeHolder);
        new rip_ldmxcsr_6068(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code lea  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code lea       ax, [L1: +305419896]}
     */
    // Template#: 303, Serial#: 2442
    public void rip_lea(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_lea(destination, placeHolder);
        new rip_lea_2442(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code lea  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code lea       eax, [L1: +305419896]}
     */
    // Template#: 304, Serial#: 2426
    public void rip_lea(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_lea(destination, placeHolder);
        new rip_lea_2426(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code lea  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code lea       rax, [L1: +305419896]}
     */
    // Template#: 305, Serial#: 2434
    public void rip_lea(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_lea(destination, placeHolder);
        new rip_lea_2434(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code lgdt  }<i>label</i>
     * Example disassembly syntax: {@code lgdt      [L1: +305419896]}
     */
    // Template#: 306, Serial#: 3143
    public void rip_lgdt(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_lgdt(placeHolder);
        new rip_lgdt_3143(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code lidt  }<i>label</i>
     * Example disassembly syntax: {@code lidt      [L1: +305419896]}
     */
    // Template#: 307, Serial#: 3147
    public void rip_lidt(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_lidt(placeHolder);
        new rip_lidt_3147(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code lldt  }<i>label</i>
     * Example disassembly syntax: {@code lldt      [L1: +305419896]}
     */
    // Template#: 308, Serial#: 3085
    public void rip_lldt(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_lldt(placeHolder);
        new rip_lldt_3085(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code lmsw  }<i>label</i>
     * Example disassembly syntax: {@code lmsw      [L1: +305419896]}
     */
    // Template#: 309, Serial#: 3155
    public void rip_lmsw(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_lmsw(placeHolder);
        new rip_lmsw_3155(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code loop  }<i>label</i>
     * Example disassembly syntax: {@code loop      L1: +18}
     */
    // Template#: 310, Serial#: 1715
    public void loop(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        loop(placeHolder);
        new loop_1715(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code loope  }<i>label</i>
     * Example disassembly syntax: {@code loope     L1: +18}
     */
    // Template#: 311, Serial#: 1714
    public void loope(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        loope(placeHolder);
        new loope_1714(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code loopne  }<i>label</i>
     * Example disassembly syntax: {@code loopne    L1: +18}
     */
    // Template#: 312, Serial#: 1713
    public void loopne(final Label label) {
        final int startPosition = currentPosition();
        final byte placeHolder = 0;
        loopne(placeHolder);
        new loopne_1713(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code lsl  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code lsl       ax, [L1: +305419896]}
     */
    // Template#: 313, Serial#: 3241
    public void rip_lsl(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_lsl(destination, placeHolder);
        new rip_lsl_3241(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code lsl  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code lsl       eax, [L1: +305419896]}
     */
    // Template#: 314, Serial#: 3223
    public void rip_lsl(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_lsl(destination, placeHolder);
        new rip_lsl_3223(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code lsl  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code lsl       rax, [L1: +305419896]}
     */
    // Template#: 315, Serial#: 3232
    public void rip_lsl(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_lsl(destination, placeHolder);
        new rip_lsl_3232(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code ltr  }<i>label</i>
     * Example disassembly syntax: {@code ltr       [L1: +305419896]}
     */
    // Template#: 316, Serial#: 3089
    public void rip_ltr(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_ltr(placeHolder);
        new rip_ltr_3089(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code maxpd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code maxpd     xmm0, [L1: +305419896]}
     */
    // Template#: 317, Serial#: 5495
    public void rip_maxpd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_maxpd(destination, placeHolder);
        new rip_maxpd_5495(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code maxps  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code maxps     xmm0, [L1: +305419896]}
     */
    // Template#: 318, Serial#: 5423
    public void rip_maxps(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_maxps(destination, placeHolder);
        new rip_maxps_5423(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code maxsd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code maxsd     xmm0, [L1: +305419896]}
     */
    // Template#: 319, Serial#: 5558
    public void rip_maxsd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_maxsd(destination, placeHolder);
        new rip_maxsd_5558(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code maxss  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code maxss     xmm0, [L1: +305419896]}
     */
    // Template#: 320, Serial#: 5630
    public void rip_maxss(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_maxss(destination, placeHolder);
        new rip_maxss_5630(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code minpd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code minpd     xmm0, [L1: +305419896]}
     */
    // Template#: 321, Serial#: 5477
    public void rip_minpd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_minpd(destination, placeHolder);
        new rip_minpd_5477(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code minps  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code minps     xmm0, [L1: +305419896]}
     */
    // Template#: 322, Serial#: 5405
    public void rip_minps(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_minps(destination, placeHolder);
        new rip_minps_5405(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code minsd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code minsd     xmm0, [L1: +305419896]}
     */
    // Template#: 323, Serial#: 5540
    public void rip_minsd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_minsd(destination, placeHolder);
        new rip_minsd_5540(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code minss  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code minss     xmm0, [L1: +305419896]}
     */
    // Template#: 324, Serial#: 5612
    public void rip_minss(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_minss(destination, placeHolder);
        new rip_minss_5612(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code mov       ax, [L1: +305419896]}
     */
    // Template#: 325, Serial#: 2409
    public void rip_mov(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_mov(destination, placeHolder);
        new rip_mov_2409(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code mov       eax, [L1: +305419896]}
     */
    // Template#: 326, Serial#: 2393
    public void rip_mov(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_mov(destination, placeHolder);
        new rip_mov_2393(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code mov       rax, [L1: +305419896]}
     */
    // Template#: 327, Serial#: 2401
    public void rip_mov(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_mov(destination, placeHolder);
        new rip_mov_2401(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code mov       al, [L1: +305419896]}
     */
    // Template#: 328, Serial#: 2385
    public void rip_mov(final AMD64GeneralRegister8 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_mov(destination, placeHolder);
        new rip_mov_2385(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code mov       es, [L1: +305419896]}
     */
    // Template#: 329, Serial#: 2450
    public void rip_mov(final SegmentRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_mov(destination, placeHolder);
        new rip_mov_2450(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movb  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code movb      [L1: +305419896], 0x12}
     */
    // Template#: 330, Serial#: 1175
    public void rip_movb(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movb(placeHolder, imm8);
        new rip_movb_1175(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [L1: +305419896], ax}
     */
    // Template#: 331, Serial#: 2376
    public void rip_mov(final Label label, final AMD64GeneralRegister16 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_mov(placeHolder, source);
        new rip_mov_2376(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [L1: +305419896], eax}
     */
    // Template#: 332, Serial#: 2358
    public void rip_mov(final Label label, final AMD64GeneralRegister32 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_mov(placeHolder, source);
        new rip_mov_2358(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [L1: +305419896], rax}
     */
    // Template#: 333, Serial#: 2367
    public void rip_mov(final Label label, final AMD64GeneralRegister64 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_mov(placeHolder, source);
        new rip_mov_2367(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [L1: +305419896], al}
     */
    // Template#: 334, Serial#: 2349
    public void rip_mov(final Label label, final AMD64GeneralRegister8 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_mov(placeHolder, source);
        new rip_mov_2349(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code mov       [L1: +305419896], es}
     */
    // Template#: 335, Serial#: 2417
    public void rip_mov(final Label label, final SegmentRegister source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_mov(placeHolder, source);
        new rip_mov_2417(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movl  }<i>label</i>, <i>imm32</i>
     * Example disassembly syntax: {@code movl      [L1: +305419896], 0x12345678}
     */
    // Template#: 336, Serial#: 1184
    public void rip_movl(final Label label, final int imm32) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movl(placeHolder, imm32);
        new rip_movl_1184(startPosition, currentPosition() - startPosition, imm32, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movq  }<i>label</i>, <i>imm32</i>
     * Example disassembly syntax: {@code movq      [L1: +305419896], 0x12345678}
     */
    // Template#: 337, Serial#: 1193
    public void rip_movq(final Label label, final int imm32) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movq(placeHolder, imm32);
        new rip_movq_1193(startPosition, currentPosition() - startPosition, imm32, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movw  }<i>label</i>, <i>imm16</i>
     * Example disassembly syntax: {@code movw      [L1: +305419896], 0x1234}
     */
    // Template#: 338, Serial#: 1202
    public void rip_movw(final Label label, final short imm16) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movw(placeHolder, imm16);
        new rip_movw_1202(startPosition, currentPosition() - startPosition, imm16, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>label</i>
     * Example disassembly syntax: {@code mov       al, [0x123456789ABCDE]}
     */
    // Template#: 339, Serial#: 901
    public void m_mov_AL(final Label label) {
        final int startPosition = currentPosition();
        final long placeHolder = 0;
        m_mov_AL(placeHolder);
        new m_mov_AL_901(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>label</i>
     * Example disassembly syntax: {@code mov       eax, [0x123456789ABCDE]}
     */
    // Template#: 340, Serial#: 902
    public void m_mov_EAX(final Label label) {
        final int startPosition = currentPosition();
        final long placeHolder = 0;
        m_mov_EAX(placeHolder);
        new m_mov_EAX_902(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>label</i>
     * Example disassembly syntax: {@code mov       rax, [0x123456789ABCDE]}
     */
    // Template#: 341, Serial#: 903
    public void m_mov_RAX(final Label label) {
        final int startPosition = currentPosition();
        final long placeHolder = 0;
        m_mov_RAX(placeHolder);
        new m_mov_RAX_903(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>label</i>
     * Example disassembly syntax: {@code mov       ax, [0x123456789ABCDE]}
     */
    // Template#: 342, Serial#: 904
    public void m_mov_AX(final Label label) {
        final int startPosition = currentPosition();
        final long placeHolder = 0;
        m_mov_AX(placeHolder);
        new m_mov_AX_904(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>label</i>
     * Example disassembly syntax: {@code mov       [0x123456789ABCDE], al}
     */
    // Template#: 343, Serial#: 905
    public void m_mov___AL(final Label label) {
        final int startPosition = currentPosition();
        final long placeHolder = 0;
        m_mov___AL(placeHolder);
        new m_mov___AL_905(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>label</i>
     * Example disassembly syntax: {@code mov       [0x123456789ABCDE], eax}
     */
    // Template#: 344, Serial#: 906
    public void m_mov___EAX(final Label label) {
        final int startPosition = currentPosition();
        final long placeHolder = 0;
        m_mov___EAX(placeHolder);
        new m_mov___EAX_906(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>label</i>
     * Example disassembly syntax: {@code mov       [0x123456789ABCDE], rax}
     */
    // Template#: 345, Serial#: 907
    public void m_mov___RAX(final Label label) {
        final int startPosition = currentPosition();
        final long placeHolder = 0;
        m_mov___RAX(placeHolder);
        new m_mov___RAX_907(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mov  }<i>label</i>
     * Example disassembly syntax: {@code mov       [0x123456789ABCDE], ax}
     */
    // Template#: 346, Serial#: 908
    public void m_mov___AX(final Label label) {
        final int startPosition = currentPosition();
        final long placeHolder = 0;
        m_mov___AX(placeHolder);
        new m_mov___AX_908(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movapd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movapd    xmm0, [L1: +305419896]}
     */
    // Template#: 347, Serial#: 4966
    public void rip_movapd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movapd(destination, placeHolder);
        new rip_movapd_4966(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movapd  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code movapd    [L1: +305419896], xmm0}
     */
    // Template#: 348, Serial#: 4975
    public void rip_movapd(final Label label, final AMD64XMMRegister source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movapd(placeHolder, source);
        new rip_movapd_4975(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movaps  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movaps    xmm0, [L1: +305419896]}
     */
    // Template#: 349, Serial#: 4896
    public void rip_movaps(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movaps(destination, placeHolder);
        new rip_movaps_4896(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movaps  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code movaps    [L1: +305419896], xmm0}
     */
    // Template#: 350, Serial#: 4905
    public void rip_movaps(final Label label, final AMD64XMMRegister source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movaps(placeHolder, source);
        new rip_movaps_4905(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdl  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movdl     xmm0, [L1: +305419896]}
     */
    // Template#: 351, Serial#: 5756
    public void rip_movdl(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movdl(destination, placeHolder);
        new rip_movdl_5756(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movdq     xmm0, [L1: +305419896]}
     */
    // Template#: 352, Serial#: 5765
    public void rip_movdq(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movdq(destination, placeHolder);
        new rip_movdq_5765(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdl  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movdl     mm0, [L1: +305419896]}
     */
    // Template#: 353, Serial#: 5675
    public void rip_movdl(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movdl(destination, placeHolder);
        new rip_movdl_5675(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movdq     mm0, [L1: +305419896]}
     */
    // Template#: 354, Serial#: 5684
    public void rip_movdq(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movdq(destination, placeHolder);
        new rip_movdq_5684(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdl  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code movdl     [L1: +305419896], xmm0}
     */
    // Template#: 355, Serial#: 5836
    public void rip_movdl(final Label label, final AMD64XMMRegister source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movdl(placeHolder, source);
        new rip_movdl_5836(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdq  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code movdq     [L1: +305419896], xmm0}
     */
    // Template#: 356, Serial#: 5845
    public void rip_movdq(final Label label, final AMD64XMMRegister source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movdq(placeHolder, source);
        new rip_movdq_5845(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdl  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code movdl     [L1: +305419896], mm0}
     */
    // Template#: 357, Serial#: 5792
    public void rip_movdl(final Label label, final MMXRegister source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movdl(placeHolder, source);
        new rip_movdl_5792(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdq  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code movdq     [L1: +305419896], mm0}
     */
    // Template#: 358, Serial#: 5801
    public void rip_movdq(final Label label, final MMXRegister source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movdq(placeHolder, source);
        new rip_movdq_5801(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movddup  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movddup   xmm0, [L1: +305419896]}
     */
    // Template#: 359, Serial#: 3390
    public void rip_movddup(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movddup(destination, placeHolder);
        new rip_movddup_3390(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdqa  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movdqa    xmm0, [L1: +305419896]}
     */
    // Template#: 360, Serial#: 5774
    public void rip_movdqa(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movdqa(destination, placeHolder);
        new rip_movdqa_5774(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdqa  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code movdqa    [L1: +305419896], xmm0}
     */
    // Template#: 361, Serial#: 5854
    public void rip_movdqa(final Label label, final AMD64XMMRegister source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movdqa(placeHolder, source);
        new rip_movdqa_5854(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdqu  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movdqu    xmm0, [L1: +305419896]}
     */
    // Template#: 362, Serial#: 5783
    public void rip_movdqu(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movdqu(destination, placeHolder);
        new rip_movdqu_5783(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movdqu  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code movdqu    [L1: +305419896], xmm0}
     */
    // Template#: 363, Serial#: 5888
    public void rip_movdqu(final Label label, final AMD64XMMRegister source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movdqu(placeHolder, source);
        new rip_movdqu_5888(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movhpd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movhpd    xmm0, [L1: +305419896]}
     */
    // Template#: 364, Serial#: 3357
    public void rip_movhpd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movhpd(destination, placeHolder);
        new rip_movhpd_3357(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movhpd  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code movhpd    [L1: +305419896], xmm0}
     */
    // Template#: 365, Serial#: 3365
    public void rip_movhpd(final Label label, final AMD64XMMRegister source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movhpd(placeHolder, source);
        new rip_movhpd_3365(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movhps  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code movhps    [L1: +305419896], xmm0}
     */
    // Template#: 366, Serial#: 3298
    public void rip_movhps(final Label label, final AMD64XMMRegister source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movhps(placeHolder, source);
        new rip_movhps_3298(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movlpd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movlpd    xmm0, [L1: +305419896]}
     */
    // Template#: 367, Serial#: 3323
    public void rip_movlpd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movlpd(destination, placeHolder);
        new rip_movlpd_3323(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movlpd  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code movlpd    [L1: +305419896], xmm0}
     */
    // Template#: 368, Serial#: 3331
    public void rip_movlpd(final Label label, final AMD64XMMRegister source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movlpd(placeHolder, source);
        new rip_movlpd_3331(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movlps  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code movlps    [L1: +305419896], xmm0}
     */
    // Template#: 369, Serial#: 3271
    public void rip_movlps(final Label label, final AMD64XMMRegister source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movlps(placeHolder, source);
        new rip_movlps_3271(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movnti  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code movnti    [L1: +305419896], eax}
     */
    // Template#: 370, Serial#: 4374
    public void rip_movnti(final Label label, final AMD64GeneralRegister32 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movnti(placeHolder, source);
        new rip_movnti_4374(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movnti  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code movnti    [L1: +305419896], rax}
     */
    // Template#: 371, Serial#: 4382
    public void rip_movnti(final Label label, final AMD64GeneralRegister64 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movnti(placeHolder, source);
        new rip_movnti_4382(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movntpd  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code movntpd   [L1: +305419896], xmm0}
     */
    // Template#: 372, Serial#: 4992
    public void rip_movntpd(final Label label, final AMD64XMMRegister source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movntpd(placeHolder, source);
        new rip_movntpd_4992(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movntps  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code movntps   [L1: +305419896], xmm0}
     */
    // Template#: 373, Serial#: 4922
    public void rip_movntps(final Label label, final AMD64XMMRegister source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movntps(placeHolder, source);
        new rip_movntps_4922(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movntq  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code movntq    [L1: +305419896], mm0}
     */
    // Template#: 374, Serial#: 4629
    public void rip_movntq(final Label label, final MMXRegister source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movntq(placeHolder, source);
        new rip_movntq_4629(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movq      xmm0, [L1: +305419896]}
     */
    // Template#: 375, Serial#: 5880
    public void rip_movq(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movq(destination, placeHolder);
        new rip_movq_5880(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movq      mm0, [L1: +305419896]}
     */
    // Template#: 376, Serial#: 5693
    public void rip_movq(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movq(destination, placeHolder);
        new rip_movq_5693(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movq  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code movq      [L1: +305419896], xmm0}
     */
    // Template#: 377, Serial#: 4563
    public void rip_movq(final Label label, final AMD64XMMRegister source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movq(placeHolder, source);
        new rip_movq_4563(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movq  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code movq      [L1: +305419896], mm0}
     */
    // Template#: 378, Serial#: 5810
    public void rip_movq(final Label label, final MMXRegister source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movq(placeHolder, source);
        new rip_movq_5810(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movsd     xmm0, [L1: +305419896]}
     */
    // Template#: 379, Serial#: 3373
    public void rip_movsd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movsd(destination, placeHolder);
        new rip_movsd_3373(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsd  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code movsd     [L1: +305419896], xmm0}
     */
    // Template#: 380, Serial#: 3382
    public void rip_movsd(final Label label, final AMD64XMMRegister source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movsd(placeHolder, source);
        new rip_movsd_3382(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movshdup  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movshdup  xmm0, [L1: +305419896]}
     */
    // Template#: 381, Serial#: 3425
    public void rip_movshdup(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movshdup(destination, placeHolder);
        new rip_movshdup_3425(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsldup  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movsldup  xmm0, [L1: +305419896]}
     */
    // Template#: 382, Serial#: 3416
    public void rip_movsldup(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movsldup(destination, placeHolder);
        new rip_movsldup_3416(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movss     xmm0, [L1: +305419896]}
     */
    // Template#: 383, Serial#: 3399
    public void rip_movss(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movss(destination, placeHolder);
        new rip_movss_3399(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movss  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code movss     [L1: +305419896], xmm0}
     */
    // Template#: 384, Serial#: 3408
    public void rip_movss(final Label label, final AMD64XMMRegister source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movss(placeHolder, source);
        new rip_movss_3408(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsx  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movsx     ax, [L1: +305419896]}
     */
    // Template#: 385, Serial#: 6273
    public void rip_movsxb(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movsxb(destination, placeHolder);
        new rip_movsxb_6273(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsx  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movsx     eax, [L1: +305419896]}
     */
    // Template#: 386, Serial#: 6255
    public void rip_movsxb(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movsxb(destination, placeHolder);
        new rip_movsxb_6255(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsx  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movsx     rax, [L1: +305419896]}
     */
    // Template#: 387, Serial#: 6264
    public void rip_movsxb(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movsxb(destination, placeHolder);
        new rip_movsxb_6264(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movsxd    rax, [L1: +305419896]}
     */
    // Template#: 388, Serial#: 294
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
    // Template#: 389, Serial#: 6282
    public void rip_movsxw(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movsxw(destination, placeHolder);
        new rip_movsxw_6282(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movsxw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movsxw    rax, [L1: +305419896]}
     */
    // Template#: 390, Serial#: 6291
    public void rip_movsxw(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movsxw(destination, placeHolder);
        new rip_movsxw_6291(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movupd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movupd    xmm0, [L1: +305419896]}
     */
    // Template#: 391, Serial#: 3306
    public void rip_movupd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movupd(destination, placeHolder);
        new rip_movupd_3306(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movupd  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code movupd    [L1: +305419896], xmm0}
     */
    // Template#: 392, Serial#: 3315
    public void rip_movupd(final Label label, final AMD64XMMRegister source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movupd(placeHolder, source);
        new rip_movupd_3315(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movups  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movups    xmm0, [L1: +305419896]}
     */
    // Template#: 393, Serial#: 3253
    public void rip_movups(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movups(destination, placeHolder);
        new rip_movups_3253(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movups  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code movups    [L1: +305419896], xmm0}
     */
    // Template#: 394, Serial#: 3262
    public void rip_movups(final Label label, final AMD64XMMRegister source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movups(placeHolder, source);
        new rip_movups_3262(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzx  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movzx     ax, [L1: +305419896]}
     */
    // Template#: 395, Serial#: 4302
    public void rip_movzxb(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movzxb(destination, placeHolder);
        new rip_movzxb_4302(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzx  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movzx     eax, [L1: +305419896]}
     */
    // Template#: 396, Serial#: 4284
    public void rip_movzxb(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movzxb(destination, placeHolder);
        new rip_movzxb_4284(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzx  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movzx     rax, [L1: +305419896]}
     */
    // Template#: 397, Serial#: 4293
    public void rip_movzxb(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movzxb(destination, placeHolder);
        new rip_movzxb_4293(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movzxd    rax, [L1: +305419896]}
     */
    // Template#: 398, Serial#: 303
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
    // Template#: 399, Serial#: 4311
    public void rip_movzxw(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movzxw(destination, placeHolder);
        new rip_movzxw_4311(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code movzxw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code movzxw    rax, [L1: +305419896]}
     */
    // Template#: 400, Serial#: 4320
    public void rip_movzxw(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_movzxw(destination, placeHolder);
        new rip_movzxw_4320(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mulb  }<i>label</i>
     * Example disassembly syntax: {@code mulb      [L1: +305419896], al}
     */
    // Template#: 401, Serial#: 1746
    public void rip_mulb___AL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_mulb___AL(placeHolder);
        new rip_mulb___AL_1746(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mull  }<i>label</i>
     * Example disassembly syntax: {@code mull      [L1: +305419896]}
     */
    // Template#: 402, Serial#: 1809
    public void rip_mull(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_mull(placeHolder);
        new rip_mull_1809(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mulq  }<i>label</i>
     * Example disassembly syntax: {@code mulq      [L1: +305419896]}
     */
    // Template#: 403, Serial#: 1872
    public void rip_mulq(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_mulq(placeHolder);
        new rip_mulq_1872(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mulw  }<i>label</i>
     * Example disassembly syntax: {@code mulw      [L1: +305419896]}
     */
    // Template#: 404, Serial#: 1935
    public void rip_mulw(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_mulw(placeHolder);
        new rip_mulw_1935(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mulpd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code mulpd     xmm0, [L1: +305419896]}
     */
    // Template#: 405, Serial#: 5441
    public void rip_mulpd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_mulpd(destination, placeHolder);
        new rip_mulpd_5441(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mulps  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code mulps     xmm0, [L1: +305419896]}
     */
    // Template#: 406, Serial#: 5369
    public void rip_mulps(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_mulps(destination, placeHolder);
        new rip_mulps_5369(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mulsd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code mulsd     xmm0, [L1: +305419896]}
     */
    // Template#: 407, Serial#: 5513
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
    // Template#: 408, Serial#: 5576
    public void rip_mulss(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_mulss(destination, placeHolder);
        new rip_mulss_5576(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code mvntdq  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code mvntdq    [L1: +305419896], xmm0}
     */
    // Template#: 409, Serial#: 4700
    public void rip_mvntdq(final Label label, final AMD64XMMRegister source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_mvntdq(placeHolder, source);
        new rip_mvntdq_4700(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code negb  }<i>label</i>
     * Example disassembly syntax: {@code negb      [L1: +305419896]}
     */
    // Template#: 410, Serial#: 1742
    public void rip_negb(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_negb(placeHolder);
        new rip_negb_1742(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code negl  }<i>label</i>
     * Example disassembly syntax: {@code negl      [L1: +305419896]}
     */
    // Template#: 411, Serial#: 1805
    public void rip_negl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_negl(placeHolder);
        new rip_negl_1805(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code negq  }<i>label</i>
     * Example disassembly syntax: {@code negq      [L1: +305419896]}
     */
    // Template#: 412, Serial#: 1868
    public void rip_negq(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_negq(placeHolder);
        new rip_negq_1868(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code negw  }<i>label</i>
     * Example disassembly syntax: {@code negw      [L1: +305419896]}
     */
    // Template#: 413, Serial#: 1931
    public void rip_negw(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_negw(placeHolder);
        new rip_negw_1931(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code notb  }<i>label</i>
     * Example disassembly syntax: {@code notb      [L1: +305419896]}
     */
    // Template#: 414, Serial#: 1738
    public void rip_notb(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_notb(placeHolder);
        new rip_notb_1738(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code notl  }<i>label</i>
     * Example disassembly syntax: {@code notl      [L1: +305419896]}
     */
    // Template#: 415, Serial#: 1801
    public void rip_notl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_notl(placeHolder);
        new rip_notl_1801(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code notq  }<i>label</i>
     * Example disassembly syntax: {@code notq      [L1: +305419896]}
     */
    // Template#: 416, Serial#: 1864
    public void rip_notq(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_notq(placeHolder);
        new rip_notq_1864(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code notw  }<i>label</i>
     * Example disassembly syntax: {@code notw      [L1: +305419896]}
     */
    // Template#: 417, Serial#: 1927
    public void rip_notw(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_notw(placeHolder);
        new rip_notw_1927(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code or        ax, [L1: +305419896]}
     */
    // Template#: 418, Serial#: 2046
    public void rip_or(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_or(destination, placeHolder);
        new rip_or_2046(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code or        eax, [L1: +305419896]}
     */
    // Template#: 419, Serial#: 2030
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
    // Template#: 420, Serial#: 2038
    public void rip_or(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_or(destination, placeHolder);
        new rip_or_2038(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code or        al, [L1: +305419896]}
     */
    // Template#: 421, Serial#: 2022
    public void rip_or(final AMD64GeneralRegister8 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_or(destination, placeHolder);
        new rip_or_2022(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code orb  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code orb       [L1: +305419896], 0x12}
     */
    // Template#: 422, Serial#: 328
    public void rip_orb(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_orb(placeHolder, imm8);
        new rip_orb_328(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code orl  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code orl       [L1: +305419896], 0x12}
     */
    // Template#: 423, Serial#: 616
    public void rip_orl(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_orl(placeHolder, imm8);
        new rip_orl_616(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code orq  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code orq       [L1: +305419896], 0x12}
     */
    // Template#: 424, Serial#: 688
    public void rip_orq(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_orq(placeHolder, imm8);
        new rip_orq_688(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code orw  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code orw       [L1: +305419896], 0x12}
     */
    // Template#: 425, Serial#: 760
    public void rip_orw(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_orw(placeHolder, imm8);
        new rip_orw_760(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code or        [L1: +305419896], ax}
     */
    // Template#: 426, Serial#: 2013
    public void rip_or(final Label label, final AMD64GeneralRegister16 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_or(placeHolder, source);
        new rip_or_2013(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code or        [L1: +305419896], eax}
     */
    // Template#: 427, Serial#: 1995
    public void rip_or(final Label label, final AMD64GeneralRegister32 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_or(placeHolder, source);
        new rip_or_1995(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code or        [L1: +305419896], rax}
     */
    // Template#: 428, Serial#: 2004
    public void rip_or(final Label label, final AMD64GeneralRegister64 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_or(placeHolder, source);
        new rip_or_2004(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code or  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code or        [L1: +305419896], al}
     */
    // Template#: 429, Serial#: 1986
    public void rip_or(final Label label, final AMD64GeneralRegister8 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_or(placeHolder, source);
        new rip_or_1986(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code orl  }<i>label</i>, <i>imm32</i>
     * Example disassembly syntax: {@code orl       [L1: +305419896], 0x12345678}
     */
    // Template#: 430, Serial#: 400
    public void rip_orl(final Label label, final int imm32) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_orl(placeHolder, imm32);
        new rip_orl_400(startPosition, currentPosition() - startPosition, imm32, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code orq  }<i>label</i>, <i>imm32</i>
     * Example disassembly syntax: {@code orq       [L1: +305419896], 0x12345678}
     */
    // Template#: 431, Serial#: 472
    public void rip_orq(final Label label, final int imm32) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_orq(placeHolder, imm32);
        new rip_orq_472(startPosition, currentPosition() - startPosition, imm32, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code orw  }<i>label</i>, <i>imm16</i>
     * Example disassembly syntax: {@code orw       [L1: +305419896], 0x1234}
     */
    // Template#: 432, Serial#: 544
    public void rip_orw(final Label label, final short imm16) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_orw(placeHolder, imm16);
        new rip_orw_544(startPosition, currentPosition() - startPosition, imm16, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code orpd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code orpd      xmm0, [L1: +305419896]}
     */
    // Template#: 433, Serial#: 3750
    public void rip_orpd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_orpd(destination, placeHolder);
        new rip_orpd_3750(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code orps  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code orps      xmm0, [L1: +305419896]}
     */
    // Template#: 434, Serial#: 3704
    public void rip_orps(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_orps(destination, placeHolder);
        new rip_orps_3704(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code packssdw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code packssdw  xmm0, [L1: +305419896]}
     */
    // Template#: 435, Serial#: 5729
    public void rip_packssdw(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_packssdw(destination, placeHolder);
        new rip_packssdw_5729(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code packssdw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code packssdw  mm0, [L1: +305419896]}
     */
    // Template#: 436, Serial#: 5666
    public void rip_packssdw(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_packssdw(destination, placeHolder);
        new rip_packssdw_5666(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code packsswb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code packsswb  xmm0, [L1: +305419896]}
     */
    // Template#: 437, Serial#: 3903
    public void rip_packsswb(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_packsswb(destination, placeHolder);
        new rip_packsswb_3903(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code packsswb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code packsswb  mm0, [L1: +305419896]}
     */
    // Template#: 438, Serial#: 3831
    public void rip_packsswb(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_packsswb(destination, placeHolder);
        new rip_packsswb_3831(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code packuswb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code packuswb  xmm0, [L1: +305419896]}
     */
    // Template#: 439, Serial#: 3939
    public void rip_packuswb(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_packuswb(destination, placeHolder);
        new rip_packuswb_3939(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code packuswb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code packuswb  mm0, [L1: +305419896]}
     */
    // Template#: 440, Serial#: 3867
    public void rip_packuswb(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_packuswb(destination, placeHolder);
        new rip_packuswb_3867(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code paddb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code paddb     xmm0, [L1: +305419896]}
     */
    // Template#: 441, Serial#: 6689
    public void rip_paddb(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_paddb(destination, placeHolder);
        new rip_paddb_6689(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code paddb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code paddb     mm0, [L1: +305419896]}
     */
    // Template#: 442, Serial#: 6626
    public void rip_paddb(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_paddb(destination, placeHolder);
        new rip_paddb_6626(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code paddd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code paddd     xmm0, [L1: +305419896]}
     */
    // Template#: 443, Serial#: 6707
    public void rip_paddd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_paddd(destination, placeHolder);
        new rip_paddd_6707(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code paddd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code paddd     mm0, [L1: +305419896]}
     */
    // Template#: 444, Serial#: 6644
    public void rip_paddd(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_paddd(destination, placeHolder);
        new rip_paddd_6644(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code paddq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code paddq     xmm0, [L1: +305419896]}
     */
    // Template#: 445, Serial#: 4545
    public void rip_paddq(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_paddq(destination, placeHolder);
        new rip_paddq_4545(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code paddq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code paddq     mm0, [L1: +305419896]}
     */
    // Template#: 446, Serial#: 4490
    public void rip_paddq(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_paddq(destination, placeHolder);
        new rip_paddq_4490(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code paddsb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code paddsb    xmm0, [L1: +305419896]}
     */
    // Template#: 447, Serial#: 6554
    public void rip_paddsb(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_paddsb(destination, placeHolder);
        new rip_paddsb_6554(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code paddsb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code paddsb    mm0, [L1: +305419896]}
     */
    // Template#: 448, Serial#: 6482
    public void rip_paddsb(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_paddsb(destination, placeHolder);
        new rip_paddsb_6482(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code paddsw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code paddsw    xmm0, [L1: +305419896]}
     */
    // Template#: 449, Serial#: 6563
    public void rip_paddsw(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_paddsw(destination, placeHolder);
        new rip_paddsw_6563(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code paddsw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code paddsw    mm0, [L1: +305419896]}
     */
    // Template#: 450, Serial#: 6491
    public void rip_paddsw(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_paddsw(destination, placeHolder);
        new rip_paddsw_6491(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code paddusb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code paddusb   xmm0, [L1: +305419896]}
     */
    // Template#: 451, Serial#: 6410
    public void rip_paddusb(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_paddusb(destination, placeHolder);
        new rip_paddusb_6410(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code paddusb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code paddusb   mm0, [L1: +305419896]}
     */
    // Template#: 452, Serial#: 6338
    public void rip_paddusb(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_paddusb(destination, placeHolder);
        new rip_paddusb_6338(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code paddusw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code paddusw   xmm0, [L1: +305419896]}
     */
    // Template#: 453, Serial#: 6419
    public void rip_paddusw(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_paddusw(destination, placeHolder);
        new rip_paddusw_6419(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code paddusw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code paddusw   mm0, [L1: +305419896]}
     */
    // Template#: 454, Serial#: 6347
    public void rip_paddusw(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_paddusw(destination, placeHolder);
        new rip_paddusw_6347(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code paddw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code paddw     xmm0, [L1: +305419896]}
     */
    // Template#: 455, Serial#: 6698
    public void rip_paddw(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_paddw(destination, placeHolder);
        new rip_paddw_6698(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code paddw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code paddw     mm0, [L1: +305419896]}
     */
    // Template#: 456, Serial#: 6635
    public void rip_paddw(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_paddw(destination, placeHolder);
        new rip_paddw_6635(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pand  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pand      xmm0, [L1: +305419896]}
     */
    // Template#: 457, Serial#: 6401
    public void rip_pand(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pand(destination, placeHolder);
        new rip_pand_6401(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pand  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pand      mm0, [L1: +305419896]}
     */
    // Template#: 458, Serial#: 6329
    public void rip_pand(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pand(destination, placeHolder);
        new rip_pand_6329(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pandn  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pandn     xmm0, [L1: +305419896]}
     */
    // Template#: 459, Serial#: 6437
    public void rip_pandn(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pandn(destination, placeHolder);
        new rip_pandn_6437(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pandn  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pandn     mm0, [L1: +305419896]}
     */
    // Template#: 460, Serial#: 6365
    public void rip_pandn(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pandn(destination, placeHolder);
        new rip_pandn_6365(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pavgb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pavgb     xmm0, [L1: +305419896]}
     */
    // Template#: 461, Serial#: 4637
    public void rip_pavgb(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pavgb(destination, placeHolder);
        new rip_pavgb_4637(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pavgb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pavgb     mm0, [L1: +305419896]}
     */
    // Template#: 462, Serial#: 4575
    public void rip_pavgb(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pavgb(destination, placeHolder);
        new rip_pavgb_4575(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pavgw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pavgw     xmm0, [L1: +305419896]}
     */
    // Template#: 463, Serial#: 4664
    public void rip_pavgw(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pavgw(destination, placeHolder);
        new rip_pavgw_4664(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pavgw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pavgw     mm0, [L1: +305419896]}
     */
    // Template#: 464, Serial#: 4602
    public void rip_pavgw(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pavgw(destination, placeHolder);
        new rip_pavgw_4602(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pcmpeqb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pcmpeqb   xmm0, [L1: +305419896]}
     */
    // Template#: 465, Serial#: 4012
    public void rip_pcmpeqb(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pcmpeqb(destination, placeHolder);
        new rip_pcmpeqb_4012(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pcmpeqb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pcmpeqb   mm0, [L1: +305419896]}
     */
    // Template#: 466, Serial#: 3965
    public void rip_pcmpeqb(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pcmpeqb(destination, placeHolder);
        new rip_pcmpeqb_3965(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pcmpeqd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pcmpeqd   xmm0, [L1: +305419896]}
     */
    // Template#: 467, Serial#: 4030
    public void rip_pcmpeqd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pcmpeqd(destination, placeHolder);
        new rip_pcmpeqd_4030(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pcmpeqd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pcmpeqd   mm0, [L1: +305419896]}
     */
    // Template#: 468, Serial#: 3983
    public void rip_pcmpeqd(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pcmpeqd(destination, placeHolder);
        new rip_pcmpeqd_3983(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pcmpeqw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pcmpeqw   xmm0, [L1: +305419896]}
     */
    // Template#: 469, Serial#: 4021
    public void rip_pcmpeqw(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pcmpeqw(destination, placeHolder);
        new rip_pcmpeqw_4021(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pcmpeqw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pcmpeqw   mm0, [L1: +305419896]}
     */
    // Template#: 470, Serial#: 3974
    public void rip_pcmpeqw(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pcmpeqw(destination, placeHolder);
        new rip_pcmpeqw_3974(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pcmpgtb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pcmpgtb   xmm0, [L1: +305419896]}
     */
    // Template#: 471, Serial#: 3912
    public void rip_pcmpgtb(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pcmpgtb(destination, placeHolder);
        new rip_pcmpgtb_3912(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pcmpgtb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pcmpgtb   mm0, [L1: +305419896]}
     */
    // Template#: 472, Serial#: 3840
    public void rip_pcmpgtb(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pcmpgtb(destination, placeHolder);
        new rip_pcmpgtb_3840(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pcmpgtd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pcmpgtd   xmm0, [L1: +305419896]}
     */
    // Template#: 473, Serial#: 3930
    public void rip_pcmpgtd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pcmpgtd(destination, placeHolder);
        new rip_pcmpgtd_3930(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pcmpgtd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pcmpgtd   mm0, [L1: +305419896]}
     */
    // Template#: 474, Serial#: 3858
    public void rip_pcmpgtd(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pcmpgtd(destination, placeHolder);
        new rip_pcmpgtd_3858(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pcmpgtw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pcmpgtw   xmm0, [L1: +305419896]}
     */
    // Template#: 475, Serial#: 3921
    public void rip_pcmpgtw(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pcmpgtw(destination, placeHolder);
        new rip_pcmpgtw_3921(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pcmpgtw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pcmpgtw   mm0, [L1: +305419896]}
     */
    // Template#: 476, Serial#: 3849
    public void rip_pcmpgtw(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pcmpgtw(destination, placeHolder);
        new rip_pcmpgtw_3849(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pinsrw  }<i>destination</i>, <i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code pinsrw    xmm0, [L1: +305419896], 0x12}
     */
    // Template#: 477, Serial#: 4426
    public void rip_pinsrw(final AMD64XMMRegister destination, final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pinsrw(destination, placeHolder, imm8);
        new rip_pinsrw_4426(startPosition, currentPosition() - startPosition, destination, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pinsrw  }<i>destination</i>, <i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code pinsrw    mm0, [L1: +305419896], 0x12}
     */
    // Template#: 478, Serial#: 4390
    public void rip_pinsrw(final MMXRegister destination, final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pinsrw(destination, placeHolder, imm8);
        new rip_pinsrw_4390(startPosition, currentPosition() - startPosition, destination, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pmaddwd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pmaddwd   xmm0, [L1: +305419896]}
     */
    // Template#: 479, Serial#: 4817
    public void rip_pmaddwd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pmaddwd(destination, placeHolder);
        new rip_pmaddwd_4817(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pmaddwd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pmaddwd   mm0, [L1: +305419896]}
     */
    // Template#: 480, Serial#: 4762
    public void rip_pmaddwd(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pmaddwd(destination, placeHolder);
        new rip_pmaddwd_4762(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pmaxsw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pmaxsw    xmm0, [L1: +305419896]}
     */
    // Template#: 481, Serial#: 6572
    public void rip_pmaxsw(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pmaxsw(destination, placeHolder);
        new rip_pmaxsw_6572(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pmaxsw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pmaxsw    mm0, [L1: +305419896]}
     */
    // Template#: 482, Serial#: 6500
    public void rip_pmaxsw(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pmaxsw(destination, placeHolder);
        new rip_pmaxsw_6500(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pmaxub  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pmaxub    xmm0, [L1: +305419896]}
     */
    // Template#: 483, Serial#: 6428
    public void rip_pmaxub(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pmaxub(destination, placeHolder);
        new rip_pmaxub_6428(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pmaxub  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pmaxub    mm0, [L1: +305419896]}
     */
    // Template#: 484, Serial#: 6356
    public void rip_pmaxub(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pmaxub(destination, placeHolder);
        new rip_pmaxub_6356(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pminsw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pminsw    xmm0, [L1: +305419896]}
     */
    // Template#: 485, Serial#: 6536
    public void rip_pminsw(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pminsw(destination, placeHolder);
        new rip_pminsw_6536(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pminsw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pminsw    mm0, [L1: +305419896]}
     */
    // Template#: 486, Serial#: 6464
    public void rip_pminsw(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pminsw(destination, placeHolder);
        new rip_pminsw_6464(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pminub  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pminub    xmm0, [L1: +305419896]}
     */
    // Template#: 487, Serial#: 6392
    public void rip_pminub(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pminub(destination, placeHolder);
        new rip_pminub_6392(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pminub  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pminub    mm0, [L1: +305419896]}
     */
    // Template#: 488, Serial#: 6320
    public void rip_pminub(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pminub(destination, placeHolder);
        new rip_pminub_6320(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pmulhuw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pmulhuw   xmm0, [L1: +305419896]}
     */
    // Template#: 489, Serial#: 4673
    public void rip_pmulhuw(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pmulhuw(destination, placeHolder);
        new rip_pmulhuw_4673(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pmulhuw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pmulhuw   mm0, [L1: +305419896]}
     */
    // Template#: 490, Serial#: 4611
    public void rip_pmulhuw(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pmulhuw(destination, placeHolder);
        new rip_pmulhuw_4611(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pmulhw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pmulhw    xmm0, [L1: +305419896]}
     */
    // Template#: 491, Serial#: 4682
    public void rip_pmulhw(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pmulhw(destination, placeHolder);
        new rip_pmulhw_4682(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pmulhw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pmulhw    mm0, [L1: +305419896]}
     */
    // Template#: 492, Serial#: 4620
    public void rip_pmulhw(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pmulhw(destination, placeHolder);
        new rip_pmulhw_4620(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pmullw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pmullw    xmm0, [L1: +305419896]}
     */
    // Template#: 493, Serial#: 4554
    public void rip_pmullw(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pmullw(destination, placeHolder);
        new rip_pmullw_4554(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pmullw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pmullw    mm0, [L1: +305419896]}
     */
    // Template#: 494, Serial#: 4499
    public void rip_pmullw(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pmullw(destination, placeHolder);
        new rip_pmullw_4499(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pmuludq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pmuludq   xmm0, [L1: +305419896]}
     */
    // Template#: 495, Serial#: 4808
    public void rip_pmuludq(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pmuludq(destination, placeHolder);
        new rip_pmuludq_4808(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pmuludq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pmuludq   mm0, [L1: +305419896]}
     */
    // Template#: 496, Serial#: 4753
    public void rip_pmuludq(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pmuludq(destination, placeHolder);
        new rip_pmuludq_4753(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pop  }<i>label</i>
     * Example disassembly syntax: {@code pop       [L1: +305419896]}
     */
    // Template#: 497, Serial#: 2459
    public void rip_pop(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pop(placeHolder);
        new rip_pop_2459(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code por  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code por       xmm0, [L1: +305419896]}
     */
    // Template#: 498, Serial#: 6545
    public void rip_por(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_por(destination, placeHolder);
        new rip_por_6545(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code por  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code por       mm0, [L1: +305419896]}
     */
    // Template#: 499, Serial#: 6473
    public void rip_por(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_por(destination, placeHolder);
        new rip_por_6473(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code prefetch  }<i>label</i>
     * Example disassembly syntax: {@code prefetch  [L1: +305419896]}
     */
    // Template#: 500, Serial#: 4847
    public void rip_prefetch(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_prefetch(placeHolder);
        new rip_prefetch_4847(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code prefetchnta  }<i>label</i>
     * Example disassembly syntax: {@code prefetchnta  [L1: +305419896]}
     */
    // Template#: 501, Serial#: 4864
    public void rip_prefetchnta(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_prefetchnta(placeHolder);
        new rip_prefetchnta_4864(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code prefetcht0  }<i>label</i>
     * Example disassembly syntax: {@code prefetcht0  [L1: +305419896]}
     */
    // Template#: 502, Serial#: 4868
    public void rip_prefetcht0(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_prefetcht0(placeHolder);
        new rip_prefetcht0_4868(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code prefetcht1  }<i>label</i>
     * Example disassembly syntax: {@code prefetcht1  [L1: +305419896]}
     */
    // Template#: 503, Serial#: 4872
    public void rip_prefetcht1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_prefetcht1(placeHolder);
        new rip_prefetcht1_4872(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code prefetcht2  }<i>label</i>
     * Example disassembly syntax: {@code prefetcht2  [L1: +305419896]}
     */
    // Template#: 504, Serial#: 4876
    public void rip_prefetcht2(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_prefetcht2(placeHolder);
        new rip_prefetcht2_4876(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code prefetchw  }<i>label</i>
     * Example disassembly syntax: {@code prefetchw  [L1: +305419896]}
     */
    // Template#: 505, Serial#: 4851
    public void rip_prefetchw(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_prefetchw(placeHolder);
        new rip_prefetchw_4851(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psadbw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psadbw    xmm0, [L1: +305419896]}
     */
    // Template#: 506, Serial#: 4826
    public void rip_psadbw(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psadbw(destination, placeHolder);
        new rip_psadbw_4826(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psadbw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psadbw    mm0, [L1: +305419896]}
     */
    // Template#: 507, Serial#: 4771
    public void rip_psadbw(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psadbw(destination, placeHolder);
        new rip_psadbw_4771(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pshufd  }<i>destination</i>, <i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code pshufd    xmm0, [L1: +305419896], 0x12}
     */
    // Template#: 508, Serial#: 3993
    public void rip_pshufd(final AMD64XMMRegister destination, final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pshufd(destination, placeHolder, imm8);
        new rip_pshufd_3993(startPosition, currentPosition() - startPosition, destination, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pshufhw  }<i>destination</i>, <i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code pshufhw   xmm0, [L1: +305419896], 0x12}
     */
    // Template#: 509, Serial#: 4048
    public void rip_pshufhw(final AMD64XMMRegister destination, final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pshufhw(destination, placeHolder, imm8);
        new rip_pshufhw_4048(startPosition, currentPosition() - startPosition, destination, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pshuflw  }<i>destination</i>, <i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code pshuflw   xmm0, [L1: +305419896], 0x12}
     */
    // Template#: 510, Serial#: 4039
    public void rip_pshuflw(final AMD64XMMRegister destination, final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pshuflw(destination, placeHolder, imm8);
        new rip_pshuflw_4039(startPosition, currentPosition() - startPosition, destination, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pshufw  }<i>destination</i>, <i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code pshufw    mm0, [L1: +305419896], 0x12}
     */
    // Template#: 511, Serial#: 3948
    public void rip_pshufw(final MMXRegister destination, final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pshufw(destination, placeHolder, imm8);
        new rip_pshufw_3948(startPosition, currentPosition() - startPosition, destination, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pslld  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pslld     xmm0, [L1: +305419896]}
     */
    // Template#: 512, Serial#: 4790
    public void rip_pslld(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pslld(destination, placeHolder);
        new rip_pslld_4790(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pslld  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pslld     mm0, [L1: +305419896]}
     */
    // Template#: 513, Serial#: 4735
    public void rip_pslld(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pslld(destination, placeHolder);
        new rip_pslld_4735(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psllq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psllq     xmm0, [L1: +305419896]}
     */
    // Template#: 514, Serial#: 4799
    public void rip_psllq(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psllq(destination, placeHolder);
        new rip_psllq_4799(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psllq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psllq     mm0, [L1: +305419896]}
     */
    // Template#: 515, Serial#: 4744
    public void rip_psllq(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psllq(destination, placeHolder);
        new rip_psllq_4744(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psllw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psllw     xmm0, [L1: +305419896]}
     */
    // Template#: 516, Serial#: 4781
    public void rip_psllw(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psllw(destination, placeHolder);
        new rip_psllw_4781(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psllw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psllw     mm0, [L1: +305419896]}
     */
    // Template#: 517, Serial#: 4726
    public void rip_psllw(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psllw(destination, placeHolder);
        new rip_psllw_4726(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psrad  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psrad     xmm0, [L1: +305419896]}
     */
    // Template#: 518, Serial#: 4655
    public void rip_psrad(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psrad(destination, placeHolder);
        new rip_psrad_4655(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psrad  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psrad     mm0, [L1: +305419896]}
     */
    // Template#: 519, Serial#: 4593
    public void rip_psrad(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psrad(destination, placeHolder);
        new rip_psrad_4593(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psraw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psraw     xmm0, [L1: +305419896]}
     */
    // Template#: 520, Serial#: 4646
    public void rip_psraw(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psraw(destination, placeHolder);
        new rip_psraw_4646(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psraw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psraw     mm0, [L1: +305419896]}
     */
    // Template#: 521, Serial#: 4584
    public void rip_psraw(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psraw(destination, placeHolder);
        new rip_psraw_4584(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psrld  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psrld     xmm0, [L1: +305419896]}
     */
    // Template#: 522, Serial#: 4527
    public void rip_psrld(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psrld(destination, placeHolder);
        new rip_psrld_4527(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psrld  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psrld     mm0, [L1: +305419896]}
     */
    // Template#: 523, Serial#: 4472
    public void rip_psrld(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psrld(destination, placeHolder);
        new rip_psrld_4472(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psrlq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psrlq     xmm0, [L1: +305419896]}
     */
    // Template#: 524, Serial#: 4536
    public void rip_psrlq(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psrlq(destination, placeHolder);
        new rip_psrlq_4536(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psrlq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psrlq     mm0, [L1: +305419896]}
     */
    // Template#: 525, Serial#: 4481
    public void rip_psrlq(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psrlq(destination, placeHolder);
        new rip_psrlq_4481(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psrlw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psrlw     xmm0, [L1: +305419896]}
     */
    // Template#: 526, Serial#: 4518
    public void rip_psrlw(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psrlw(destination, placeHolder);
        new rip_psrlw_4518(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psrlw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psrlw     mm0, [L1: +305419896]}
     */
    // Template#: 527, Serial#: 4463
    public void rip_psrlw(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psrlw(destination, placeHolder);
        new rip_psrlw_4463(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psubb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psubb     xmm0, [L1: +305419896]}
     */
    // Template#: 528, Serial#: 6653
    public void rip_psubb(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psubb(destination, placeHolder);
        new rip_psubb_6653(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psubb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psubb     mm0, [L1: +305419896]}
     */
    // Template#: 529, Serial#: 6590
    public void rip_psubb(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psubb(destination, placeHolder);
        new rip_psubb_6590(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psubd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psubd     xmm0, [L1: +305419896]}
     */
    // Template#: 530, Serial#: 6671
    public void rip_psubd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psubd(destination, placeHolder);
        new rip_psubd_6671(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psubd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psubd     mm0, [L1: +305419896]}
     */
    // Template#: 531, Serial#: 6608
    public void rip_psubd(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psubd(destination, placeHolder);
        new rip_psubd_6608(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psubq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psubq     xmm0, [L1: +305419896]}
     */
    // Template#: 532, Serial#: 6680
    public void rip_psubq(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psubq(destination, placeHolder);
        new rip_psubq_6680(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psubq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psubq     mm0, [L1: +305419896]}
     */
    // Template#: 533, Serial#: 6617
    public void rip_psubq(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psubq(destination, placeHolder);
        new rip_psubq_6617(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psubsb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psubsb    xmm0, [L1: +305419896]}
     */
    // Template#: 534, Serial#: 6518
    public void rip_psubsb(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psubsb(destination, placeHolder);
        new rip_psubsb_6518(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psubsb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psubsb    mm0, [L1: +305419896]}
     */
    // Template#: 535, Serial#: 6446
    public void rip_psubsb(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psubsb(destination, placeHolder);
        new rip_psubsb_6446(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psubsw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psubsw    xmm0, [L1: +305419896]}
     */
    // Template#: 536, Serial#: 6527
    public void rip_psubsw(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psubsw(destination, placeHolder);
        new rip_psubsw_6527(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psubsw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psubsw    mm0, [L1: +305419896]}
     */
    // Template#: 537, Serial#: 6455
    public void rip_psubsw(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psubsw(destination, placeHolder);
        new rip_psubsw_6455(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psubusb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psubusb   xmm0, [L1: +305419896]}
     */
    // Template#: 538, Serial#: 6374
    public void rip_psubusb(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psubusb(destination, placeHolder);
        new rip_psubusb_6374(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psubusb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psubusb   mm0, [L1: +305419896]}
     */
    // Template#: 539, Serial#: 6302
    public void rip_psubusb(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psubusb(destination, placeHolder);
        new rip_psubusb_6302(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psubusw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psubusw   xmm0, [L1: +305419896]}
     */
    // Template#: 540, Serial#: 6383
    public void rip_psubusw(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psubusw(destination, placeHolder);
        new rip_psubusw_6383(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psubusw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psubusw   mm0, [L1: +305419896]}
     */
    // Template#: 541, Serial#: 6311
    public void rip_psubusw(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psubusw(destination, placeHolder);
        new rip_psubusw_6311(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psubw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psubw     xmm0, [L1: +305419896]}
     */
    // Template#: 542, Serial#: 6662
    public void rip_psubw(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psubw(destination, placeHolder);
        new rip_psubw_6662(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code psubw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code psubw     mm0, [L1: +305419896]}
     */
    // Template#: 543, Serial#: 6599
    public void rip_psubw(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_psubw(destination, placeHolder);
        new rip_psubw_6599(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code punpckhbw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code punpckhbw  xmm0, [L1: +305419896]}
     */
    // Template#: 544, Serial#: 5702
    public void rip_punpckhbw(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_punpckhbw(destination, placeHolder);
        new rip_punpckhbw_5702(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code punpckhbw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code punpckhbw  mm0, [L1: +305419896]}
     */
    // Template#: 545, Serial#: 5639
    public void rip_punpckhbw(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_punpckhbw(destination, placeHolder);
        new rip_punpckhbw_5639(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code punpckhdq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code punpckhdq  xmm0, [L1: +305419896]}
     */
    // Template#: 546, Serial#: 5720
    public void rip_punpckhdq(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_punpckhdq(destination, placeHolder);
        new rip_punpckhdq_5720(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code punpckhdq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code punpckhdq  mm0, [L1: +305419896]}
     */
    // Template#: 547, Serial#: 5657
    public void rip_punpckhdq(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_punpckhdq(destination, placeHolder);
        new rip_punpckhdq_5657(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code punpckhqdq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code punpckhqdq  xmm0, [L1: +305419896]}
     */
    // Template#: 548, Serial#: 5747
    public void rip_punpckhqdq(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_punpckhqdq(destination, placeHolder);
        new rip_punpckhqdq_5747(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code punpckhwd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code punpckhwd  xmm0, [L1: +305419896]}
     */
    // Template#: 549, Serial#: 5711
    public void rip_punpckhwd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_punpckhwd(destination, placeHolder);
        new rip_punpckhwd_5711(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code punpckhwd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code punpckhwd  mm0, [L1: +305419896]}
     */
    // Template#: 550, Serial#: 5648
    public void rip_punpckhwd(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_punpckhwd(destination, placeHolder);
        new rip_punpckhwd_5648(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code punpcklbw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code punpcklbw  xmm0, [L1: +305419896]}
     */
    // Template#: 551, Serial#: 3876
    public void rip_punpcklbw(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_punpcklbw(destination, placeHolder);
        new rip_punpcklbw_3876(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code punpcklbw  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code punpcklbw  mm0, [L1: +305419896]}
     */
    // Template#: 552, Serial#: 3804
    public void rip_punpcklbw(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_punpcklbw(destination, placeHolder);
        new rip_punpcklbw_3804(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code punpckldq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code punpckldq  xmm0, [L1: +305419896]}
     */
    // Template#: 553, Serial#: 3894
    public void rip_punpckldq(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_punpckldq(destination, placeHolder);
        new rip_punpckldq_3894(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code punpckldq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code punpckldq  mm0, [L1: +305419896]}
     */
    // Template#: 554, Serial#: 3822
    public void rip_punpckldq(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_punpckldq(destination, placeHolder);
        new rip_punpckldq_3822(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code punpcklqdq  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code punpcklqdq  xmm0, [L1: +305419896]}
     */
    // Template#: 555, Serial#: 5738
    public void rip_punpcklqdq(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_punpcklqdq(destination, placeHolder);
        new rip_punpcklqdq_5738(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code punpcklwd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code punpcklwd  xmm0, [L1: +305419896]}
     */
    // Template#: 556, Serial#: 3885
    public void rip_punpcklwd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_punpcklwd(destination, placeHolder);
        new rip_punpcklwd_3885(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code punpcklwd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code punpcklwd  mm0, [L1: +305419896]}
     */
    // Template#: 557, Serial#: 3813
    public void rip_punpcklwd(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_punpcklwd(destination, placeHolder);
        new rip_punpcklwd_3813(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code push  }<i>label</i>
     * Example disassembly syntax: {@code push      [L1: +305419896]}
     */
    // Template#: 558, Serial#: 3057
    public void rip_push(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_push(placeHolder);
        new rip_push_3057(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pxor  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pxor      xmm0, [L1: +305419896]}
     */
    // Template#: 559, Serial#: 6581
    public void rip_pxor(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pxor(destination, placeHolder);
        new rip_pxor_6581(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code pxor  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code pxor      mm0, [L1: +305419896]}
     */
    // Template#: 560, Serial#: 6509
    public void rip_pxor(final MMXRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_pxor(destination, placeHolder);
        new rip_pxor_6509(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rclb  }<i>label</i>
     * Example disassembly syntax: {@code rclb      [L1: +305419896], 0x1}
     */
    // Template#: 561, Serial#: 1219
    public void rip_rclb___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rclb___1(placeHolder);
        new rip_rclb___1_1219(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rcll  }<i>label</i>
     * Example disassembly syntax: {@code rcll      [L1: +305419896], 0x1}
     */
    // Template#: 562, Serial#: 1282
    public void rip_rcll___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rcll___1(placeHolder);
        new rip_rcll___1_1282(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rclq  }<i>label</i>
     * Example disassembly syntax: {@code rclq      [L1: +305419896], 0x1}
     */
    // Template#: 563, Serial#: 1345
    public void rip_rclq___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rclq___1(placeHolder);
        new rip_rclq___1_1345(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rclw  }<i>label</i>
     * Example disassembly syntax: {@code rclw      [L1: +305419896], 0x1}
     */
    // Template#: 564, Serial#: 1408
    public void rip_rclw___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rclw___1(placeHolder);
        new rip_rclw___1_1408(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rclb  }<i>label</i>
     * Example disassembly syntax: {@code rclb      [L1: +305419896], cl}
     */
    // Template#: 565, Serial#: 1471
    public void rip_rclb___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rclb___CL(placeHolder);
        new rip_rclb___CL_1471(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rcll  }<i>label</i>
     * Example disassembly syntax: {@code rcll      [L1: +305419896], cl}
     */
    // Template#: 566, Serial#: 1534
    public void rip_rcll___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rcll___CL(placeHolder);
        new rip_rcll___CL_1534(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rclq  }<i>label</i>
     * Example disassembly syntax: {@code rclq      [L1: +305419896], cl}
     */
    // Template#: 567, Serial#: 1597
    public void rip_rclq___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rclq___CL(placeHolder);
        new rip_rclq___CL_1597(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rclw  }<i>label</i>
     * Example disassembly syntax: {@code rclw      [L1: +305419896], cl}
     */
    // Template#: 568, Serial#: 1660
    public void rip_rclw___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rclw___CL(placeHolder);
        new rip_rclw___CL_1660(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rclb  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code rclb      [L1: +305419896], 0x12}
     */
    // Template#: 569, Serial#: 929
    public void rip_rclb(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rclb(placeHolder, imm8);
        new rip_rclb_929(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rcll  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code rcll      [L1: +305419896], 0x12}
     */
    // Template#: 570, Serial#: 992
    public void rip_rcll(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rcll(placeHolder, imm8);
        new rip_rcll_992(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rclq  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code rclq      [L1: +305419896], 0x12}
     */
    // Template#: 571, Serial#: 1055
    public void rip_rclq(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rclq(placeHolder, imm8);
        new rip_rclq_1055(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rclw  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code rclw      [L1: +305419896], 0x12}
     */
    // Template#: 572, Serial#: 1118
    public void rip_rclw(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rclw(placeHolder, imm8);
        new rip_rclw_1118(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rcpps  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code rcpps     xmm0, [L1: +305419896]}
     */
    // Template#: 573, Serial#: 3677
    public void rip_rcpps(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rcpps(destination, placeHolder);
        new rip_rcpps_3677(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rcpss  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code rcpss     xmm0, [L1: +305419896]}
     */
    // Template#: 574, Serial#: 3795
    public void rip_rcpss(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rcpss(destination, placeHolder);
        new rip_rcpss_3795(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rcrb  }<i>label</i>
     * Example disassembly syntax: {@code rcrb      [L1: +305419896], 0x1}
     */
    // Template#: 575, Serial#: 1223
    public void rip_rcrb___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rcrb___1(placeHolder);
        new rip_rcrb___1_1223(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rcrl  }<i>label</i>
     * Example disassembly syntax: {@code rcrl      [L1: +305419896], 0x1}
     */
    // Template#: 576, Serial#: 1286
    public void rip_rcrl___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rcrl___1(placeHolder);
        new rip_rcrl___1_1286(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rcrq  }<i>label</i>
     * Example disassembly syntax: {@code rcrq      [L1: +305419896], 0x1}
     */
    // Template#: 577, Serial#: 1349
    public void rip_rcrq___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rcrq___1(placeHolder);
        new rip_rcrq___1_1349(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rcrw  }<i>label</i>
     * Example disassembly syntax: {@code rcrw      [L1: +305419896], 0x1}
     */
    // Template#: 578, Serial#: 1412
    public void rip_rcrw___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rcrw___1(placeHolder);
        new rip_rcrw___1_1412(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rcrb  }<i>label</i>
     * Example disassembly syntax: {@code rcrb      [L1: +305419896], cl}
     */
    // Template#: 579, Serial#: 1475
    public void rip_rcrb___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rcrb___CL(placeHolder);
        new rip_rcrb___CL_1475(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rcrl  }<i>label</i>
     * Example disassembly syntax: {@code rcrl      [L1: +305419896], cl}
     */
    // Template#: 580, Serial#: 1538
    public void rip_rcrl___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rcrl___CL(placeHolder);
        new rip_rcrl___CL_1538(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rcrq  }<i>label</i>
     * Example disassembly syntax: {@code rcrq      [L1: +305419896], cl}
     */
    // Template#: 581, Serial#: 1601
    public void rip_rcrq___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rcrq___CL(placeHolder);
        new rip_rcrq___CL_1601(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rcrw  }<i>label</i>
     * Example disassembly syntax: {@code rcrw      [L1: +305419896], cl}
     */
    // Template#: 582, Serial#: 1664
    public void rip_rcrw___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rcrw___CL(placeHolder);
        new rip_rcrw___CL_1664(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rcrb  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code rcrb      [L1: +305419896], 0x12}
     */
    // Template#: 583, Serial#: 933
    public void rip_rcrb(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rcrb(placeHolder, imm8);
        new rip_rcrb_933(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rcrl  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code rcrl      [L1: +305419896], 0x12}
     */
    // Template#: 584, Serial#: 996
    public void rip_rcrl(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rcrl(placeHolder, imm8);
        new rip_rcrl_996(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rcrq  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code rcrq      [L1: +305419896], 0x12}
     */
    // Template#: 585, Serial#: 1059
    public void rip_rcrq(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rcrq(placeHolder, imm8);
        new rip_rcrq_1059(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rcrw  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code rcrw      [L1: +305419896], 0x12}
     */
    // Template#: 586, Serial#: 1122
    public void rip_rcrw(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rcrw(placeHolder, imm8);
        new rip_rcrw_1122(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rolb  }<i>label</i>
     * Example disassembly syntax: {@code rolb      [L1: +305419896], 0x1}
     */
    // Template#: 587, Serial#: 1211
    public void rip_rolb___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rolb___1(placeHolder);
        new rip_rolb___1_1211(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code roll  }<i>label</i>
     * Example disassembly syntax: {@code roll      [L1: +305419896], 0x1}
     */
    // Template#: 588, Serial#: 1274
    public void rip_roll___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_roll___1(placeHolder);
        new rip_roll___1_1274(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rolq  }<i>label</i>
     * Example disassembly syntax: {@code rolq      [L1: +305419896], 0x1}
     */
    // Template#: 589, Serial#: 1337
    public void rip_rolq___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rolq___1(placeHolder);
        new rip_rolq___1_1337(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rolw  }<i>label</i>
     * Example disassembly syntax: {@code rolw      [L1: +305419896], 0x1}
     */
    // Template#: 590, Serial#: 1400
    public void rip_rolw___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rolw___1(placeHolder);
        new rip_rolw___1_1400(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rolb  }<i>label</i>
     * Example disassembly syntax: {@code rolb      [L1: +305419896], cl}
     */
    // Template#: 591, Serial#: 1463
    public void rip_rolb___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rolb___CL(placeHolder);
        new rip_rolb___CL_1463(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code roll  }<i>label</i>
     * Example disassembly syntax: {@code roll      [L1: +305419896], cl}
     */
    // Template#: 592, Serial#: 1526
    public void rip_roll___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_roll___CL(placeHolder);
        new rip_roll___CL_1526(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rolq  }<i>label</i>
     * Example disassembly syntax: {@code rolq      [L1: +305419896], cl}
     */
    // Template#: 593, Serial#: 1589
    public void rip_rolq___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rolq___CL(placeHolder);
        new rip_rolq___CL_1589(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rolw  }<i>label</i>
     * Example disassembly syntax: {@code rolw      [L1: +305419896], cl}
     */
    // Template#: 594, Serial#: 1652
    public void rip_rolw___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rolw___CL(placeHolder);
        new rip_rolw___CL_1652(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rolb  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code rolb      [L1: +305419896], 0x12}
     */
    // Template#: 595, Serial#: 921
    public void rip_rolb(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rolb(placeHolder, imm8);
        new rip_rolb_921(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code roll  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code roll      [L1: +305419896], 0x12}
     */
    // Template#: 596, Serial#: 984
    public void rip_roll(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_roll(placeHolder, imm8);
        new rip_roll_984(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rolq  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code rolq      [L1: +305419896], 0x12}
     */
    // Template#: 597, Serial#: 1047
    public void rip_rolq(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rolq(placeHolder, imm8);
        new rip_rolq_1047(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rolw  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code rolw      [L1: +305419896], 0x12}
     */
    // Template#: 598, Serial#: 1110
    public void rip_rolw(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rolw(placeHolder, imm8);
        new rip_rolw_1110(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rorb  }<i>label</i>
     * Example disassembly syntax: {@code rorb      [L1: +305419896], 0x1}
     */
    // Template#: 599, Serial#: 1215
    public void rip_rorb___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rorb___1(placeHolder);
        new rip_rorb___1_1215(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rorl  }<i>label</i>
     * Example disassembly syntax: {@code rorl      [L1: +305419896], 0x1}
     */
    // Template#: 600, Serial#: 1278
    public void rip_rorl___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rorl___1(placeHolder);
        new rip_rorl___1_1278(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rorq  }<i>label</i>
     * Example disassembly syntax: {@code rorq      [L1: +305419896], 0x1}
     */
    // Template#: 601, Serial#: 1341
    public void rip_rorq___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rorq___1(placeHolder);
        new rip_rorq___1_1341(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rorw  }<i>label</i>
     * Example disassembly syntax: {@code rorw      [L1: +305419896], 0x1}
     */
    // Template#: 602, Serial#: 1404
    public void rip_rorw___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rorw___1(placeHolder);
        new rip_rorw___1_1404(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rorb  }<i>label</i>
     * Example disassembly syntax: {@code rorb      [L1: +305419896], cl}
     */
    // Template#: 603, Serial#: 1467
    public void rip_rorb___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rorb___CL(placeHolder);
        new rip_rorb___CL_1467(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rorl  }<i>label</i>
     * Example disassembly syntax: {@code rorl      [L1: +305419896], cl}
     */
    // Template#: 604, Serial#: 1530
    public void rip_rorl___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rorl___CL(placeHolder);
        new rip_rorl___CL_1530(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rorq  }<i>label</i>
     * Example disassembly syntax: {@code rorq      [L1: +305419896], cl}
     */
    // Template#: 605, Serial#: 1593
    public void rip_rorq___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rorq___CL(placeHolder);
        new rip_rorq___CL_1593(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rorw  }<i>label</i>
     * Example disassembly syntax: {@code rorw      [L1: +305419896], cl}
     */
    // Template#: 606, Serial#: 1656
    public void rip_rorw___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rorw___CL(placeHolder);
        new rip_rorw___CL_1656(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rorb  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code rorb      [L1: +305419896], 0x12}
     */
    // Template#: 607, Serial#: 925
    public void rip_rorb(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rorb(placeHolder, imm8);
        new rip_rorb_925(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rorl  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code rorl      [L1: +305419896], 0x12}
     */
    // Template#: 608, Serial#: 988
    public void rip_rorl(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rorl(placeHolder, imm8);
        new rip_rorl_988(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rorq  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code rorq      [L1: +305419896], 0x12}
     */
    // Template#: 609, Serial#: 1051
    public void rip_rorq(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rorq(placeHolder, imm8);
        new rip_rorq_1051(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rorw  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code rorw      [L1: +305419896], 0x12}
     */
    // Template#: 610, Serial#: 1114
    public void rip_rorw(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rorw(placeHolder, imm8);
        new rip_rorw_1114(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rsqrtps  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code rsqrtps   xmm0, [L1: +305419896]}
     */
    // Template#: 611, Serial#: 3668
    public void rip_rsqrtps(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rsqrtps(destination, placeHolder);
        new rip_rsqrtps_3668(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code rsqrtss  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code rsqrtss   xmm0, [L1: +305419896]}
     */
    // Template#: 612, Serial#: 3786
    public void rip_rsqrtss(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_rsqrtss(destination, placeHolder);
        new rip_rsqrtss_3786(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarb  }<i>label</i>
     * Example disassembly syntax: {@code sarb      [L1: +305419896], 0x1}
     */
    // Template#: 613, Serial#: 1235
    public void rip_sarb___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sarb___1(placeHolder);
        new rip_sarb___1_1235(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarl  }<i>label</i>
     * Example disassembly syntax: {@code sarl      [L1: +305419896], 0x1}
     */
    // Template#: 614, Serial#: 1298
    public void rip_sarl___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sarl___1(placeHolder);
        new rip_sarl___1_1298(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarq  }<i>label</i>
     * Example disassembly syntax: {@code sarq      [L1: +305419896], 0x1}
     */
    // Template#: 615, Serial#: 1361
    public void rip_sarq___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sarq___1(placeHolder);
        new rip_sarq___1_1361(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarw  }<i>label</i>
     * Example disassembly syntax: {@code sarw      [L1: +305419896], 0x1}
     */
    // Template#: 616, Serial#: 1424
    public void rip_sarw___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sarw___1(placeHolder);
        new rip_sarw___1_1424(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarb  }<i>label</i>
     * Example disassembly syntax: {@code sarb      [L1: +305419896], cl}
     */
    // Template#: 617, Serial#: 1487
    public void rip_sarb___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sarb___CL(placeHolder);
        new rip_sarb___CL_1487(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarl  }<i>label</i>
     * Example disassembly syntax: {@code sarl      [L1: +305419896], cl}
     */
    // Template#: 618, Serial#: 1550
    public void rip_sarl___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sarl___CL(placeHolder);
        new rip_sarl___CL_1550(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarq  }<i>label</i>
     * Example disassembly syntax: {@code sarq      [L1: +305419896], cl}
     */
    // Template#: 619, Serial#: 1613
    public void rip_sarq___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sarq___CL(placeHolder);
        new rip_sarq___CL_1613(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarw  }<i>label</i>
     * Example disassembly syntax: {@code sarw      [L1: +305419896], cl}
     */
    // Template#: 620, Serial#: 1676
    public void rip_sarw___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sarw___CL(placeHolder);
        new rip_sarw___CL_1676(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarb  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code sarb      [L1: +305419896], 0x12}
     */
    // Template#: 621, Serial#: 945
    public void rip_sarb(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sarb(placeHolder, imm8);
        new rip_sarb_945(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarl  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code sarl      [L1: +305419896], 0x12}
     */
    // Template#: 622, Serial#: 1008
    public void rip_sarl(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sarl(placeHolder, imm8);
        new rip_sarl_1008(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarq  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code sarq      [L1: +305419896], 0x12}
     */
    // Template#: 623, Serial#: 1071
    public void rip_sarq(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sarq(placeHolder, imm8);
        new rip_sarq_1071(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sarw  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code sarw      [L1: +305419896], 0x12}
     */
    // Template#: 624, Serial#: 1134
    public void rip_sarw(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sarw(placeHolder, imm8);
        new rip_sarw_1134(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sbb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code sbb       ax, [L1: +305419896]}
     */
    // Template#: 625, Serial#: 2118
    public void rip_sbb(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sbb(destination, placeHolder);
        new rip_sbb_2118(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sbb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code sbb       eax, [L1: +305419896]}
     */
    // Template#: 626, Serial#: 2102
    public void rip_sbb(final AMD64GeneralRegister32 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sbb(destination, placeHolder);
        new rip_sbb_2102(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sbb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code sbb       rax, [L1: +305419896]}
     */
    // Template#: 627, Serial#: 2110
    public void rip_sbb(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sbb(destination, placeHolder);
        new rip_sbb_2110(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sbb  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code sbb       al, [L1: +305419896]}
     */
    // Template#: 628, Serial#: 2094
    public void rip_sbb(final AMD64GeneralRegister8 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sbb(destination, placeHolder);
        new rip_sbb_2094(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sbbb  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code sbbb      [L1: +305419896], 0x12}
     */
    // Template#: 629, Serial#: 336
    public void rip_sbbb(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sbbb(placeHolder, imm8);
        new rip_sbbb_336(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sbbl  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code sbbl      [L1: +305419896], 0x12}
     */
    // Template#: 630, Serial#: 624
    public void rip_sbbl(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sbbl(placeHolder, imm8);
        new rip_sbbl_624(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sbbq  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code sbbq      [L1: +305419896], 0x12}
     */
    // Template#: 631, Serial#: 696
    public void rip_sbbq(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sbbq(placeHolder, imm8);
        new rip_sbbq_696(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sbbw  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code sbbw      [L1: +305419896], 0x12}
     */
    // Template#: 632, Serial#: 768
    public void rip_sbbw(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sbbw(placeHolder, imm8);
        new rip_sbbw_768(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sbb  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code sbb       [L1: +305419896], ax}
     */
    // Template#: 633, Serial#: 2085
    public void rip_sbb(final Label label, final AMD64GeneralRegister16 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sbb(placeHolder, source);
        new rip_sbb_2085(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sbb  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code sbb       [L1: +305419896], eax}
     */
    // Template#: 634, Serial#: 2067
    public void rip_sbb(final Label label, final AMD64GeneralRegister32 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sbb(placeHolder, source);
        new rip_sbb_2067(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sbb  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code sbb       [L1: +305419896], rax}
     */
    // Template#: 635, Serial#: 2076
    public void rip_sbb(final Label label, final AMD64GeneralRegister64 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sbb(placeHolder, source);
        new rip_sbb_2076(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sbb  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code sbb       [L1: +305419896], al}
     */
    // Template#: 636, Serial#: 2058
    public void rip_sbb(final Label label, final AMD64GeneralRegister8 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sbb(placeHolder, source);
        new rip_sbb_2058(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sbbl  }<i>label</i>, <i>imm32</i>
     * Example disassembly syntax: {@code sbbl      [L1: +305419896], 0x12345678}
     */
    // Template#: 637, Serial#: 408
    public void rip_sbbl(final Label label, final int imm32) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sbbl(placeHolder, imm32);
        new rip_sbbl_408(startPosition, currentPosition() - startPosition, imm32, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sbbq  }<i>label</i>, <i>imm32</i>
     * Example disassembly syntax: {@code sbbq      [L1: +305419896], 0x12345678}
     */
    // Template#: 638, Serial#: 480
    public void rip_sbbq(final Label label, final int imm32) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sbbq(placeHolder, imm32);
        new rip_sbbq_480(startPosition, currentPosition() - startPosition, imm32, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sbbw  }<i>label</i>, <i>imm16</i>
     * Example disassembly syntax: {@code sbbw      [L1: +305419896], 0x1234}
     */
    // Template#: 639, Serial#: 552
    public void rip_sbbw(final Label label, final short imm16) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sbbw(placeHolder, imm16);
        new rip_sbbw_552(startPosition, currentPosition() - startPosition, imm16, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code setb  }<i>label</i>
     * Example disassembly syntax: {@code setb      [L1: +305419896]}
     */
    // Template#: 640, Serial#: 4083
    public void rip_setb(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_setb(placeHolder);
        new rip_setb_4083(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code setbe  }<i>label</i>
     * Example disassembly syntax: {@code setbe     [L1: +305419896]}
     */
    // Template#: 641, Serial#: 4119
    public void rip_setbe(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_setbe(placeHolder);
        new rip_setbe_4119(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code setl  }<i>label</i>
     * Example disassembly syntax: {@code setl      [L1: +305419896]}
     */
    // Template#: 642, Serial#: 5940
    public void rip_setl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_setl(placeHolder);
        new rip_setl_5940(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code setle  }<i>label</i>
     * Example disassembly syntax: {@code setle     [L1: +305419896]}
     */
    // Template#: 643, Serial#: 5958
    public void rip_setle(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_setle(placeHolder);
        new rip_setle_5958(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code setnb  }<i>label</i>
     * Example disassembly syntax: {@code setnb     [L1: +305419896]}
     */
    // Template#: 644, Serial#: 4092
    public void rip_setnb(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_setnb(placeHolder);
        new rip_setnb_4092(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code setnbe  }<i>label</i>
     * Example disassembly syntax: {@code setnbe    [L1: +305419896]}
     */
    // Template#: 645, Serial#: 4128
    public void rip_setnbe(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_setnbe(placeHolder);
        new rip_setnbe_4128(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code setnl  }<i>label</i>
     * Example disassembly syntax: {@code setnl     [L1: +305419896]}
     */
    // Template#: 646, Serial#: 5949
    public void rip_setnl(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_setnl(placeHolder);
        new rip_setnl_5949(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code setnle  }<i>label</i>
     * Example disassembly syntax: {@code setnle    [L1: +305419896]}
     */
    // Template#: 647, Serial#: 5967
    public void rip_setnle(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_setnle(placeHolder);
        new rip_setnle_5967(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code setno  }<i>label</i>
     * Example disassembly syntax: {@code setno     [L1: +305419896]}
     */
    // Template#: 648, Serial#: 4074
    public void rip_setno(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_setno(placeHolder);
        new rip_setno_4074(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code setnp  }<i>label</i>
     * Example disassembly syntax: {@code setnp     [L1: +305419896]}
     */
    // Template#: 649, Serial#: 5931
    public void rip_setnp(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_setnp(placeHolder);
        new rip_setnp_5931(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code setns  }<i>label</i>
     * Example disassembly syntax: {@code setns     [L1: +305419896]}
     */
    // Template#: 650, Serial#: 5913
    public void rip_setns(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_setns(placeHolder);
        new rip_setns_5913(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code setnz  }<i>label</i>
     * Example disassembly syntax: {@code setnz     [L1: +305419896]}
     */
    // Template#: 651, Serial#: 4110
    public void rip_setnz(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_setnz(placeHolder);
        new rip_setnz_4110(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code seto  }<i>label</i>
     * Example disassembly syntax: {@code seto      [L1: +305419896]}
     */
    // Template#: 652, Serial#: 4065
    public void rip_seto(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_seto(placeHolder);
        new rip_seto_4065(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code setp  }<i>label</i>
     * Example disassembly syntax: {@code setp      [L1: +305419896]}
     */
    // Template#: 653, Serial#: 5922
    public void rip_setp(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_setp(placeHolder);
        new rip_setp_5922(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sets  }<i>label</i>
     * Example disassembly syntax: {@code sets      [L1: +305419896]}
     */
    // Template#: 654, Serial#: 5904
    public void rip_sets(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sets(placeHolder);
        new rip_sets_5904(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code setz  }<i>label</i>
     * Example disassembly syntax: {@code setz      [L1: +305419896]}
     */
    // Template#: 655, Serial#: 4101
    public void rip_setz(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_setz(placeHolder);
        new rip_setz_4101(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sgdt  }<i>label</i>
     * Example disassembly syntax: {@code sgdt      [L1: +305419896]}
     */
    // Template#: 656, Serial#: 3135
    public void rip_sgdt(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sgdt(placeHolder);
        new rip_sgdt_3135(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlb  }<i>label</i>
     * Example disassembly syntax: {@code shlb      [L1: +305419896], 0x1}
     */
    // Template#: 657, Serial#: 1227
    public void rip_shlb___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shlb___1(placeHolder);
        new rip_shlb___1_1227(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shll  }<i>label</i>
     * Example disassembly syntax: {@code shll      [L1: +305419896], 0x1}
     */
    // Template#: 658, Serial#: 1290
    public void rip_shll___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shll___1(placeHolder);
        new rip_shll___1_1290(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlq  }<i>label</i>
     * Example disassembly syntax: {@code shlq      [L1: +305419896], 0x1}
     */
    // Template#: 659, Serial#: 1353
    public void rip_shlq___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shlq___1(placeHolder);
        new rip_shlq___1_1353(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlw  }<i>label</i>
     * Example disassembly syntax: {@code shlw      [L1: +305419896], 0x1}
     */
    // Template#: 660, Serial#: 1416
    public void rip_shlw___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shlw___1(placeHolder);
        new rip_shlw___1_1416(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlb  }<i>label</i>
     * Example disassembly syntax: {@code shlb      [L1: +305419896], cl}
     */
    // Template#: 661, Serial#: 1479
    public void rip_shlb___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shlb___CL(placeHolder);
        new rip_shlb___CL_1479(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shll  }<i>label</i>
     * Example disassembly syntax: {@code shll      [L1: +305419896], cl}
     */
    // Template#: 662, Serial#: 1542
    public void rip_shll___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shll___CL(placeHolder);
        new rip_shll___CL_1542(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlq  }<i>label</i>
     * Example disassembly syntax: {@code shlq      [L1: +305419896], cl}
     */
    // Template#: 663, Serial#: 1605
    public void rip_shlq___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shlq___CL(placeHolder);
        new rip_shlq___CL_1605(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlw  }<i>label</i>
     * Example disassembly syntax: {@code shlw      [L1: +305419896], cl}
     */
    // Template#: 664, Serial#: 1668
    public void rip_shlw___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shlw___CL(placeHolder);
        new rip_shlw___CL_1668(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlb  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shlb      [L1: +305419896], 0x12}
     */
    // Template#: 665, Serial#: 937
    public void rip_shlb(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shlb(placeHolder, imm8);
        new rip_shlb_937(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shll  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shll      [L1: +305419896], 0x12}
     */
    // Template#: 666, Serial#: 1000
    public void rip_shll(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shll(placeHolder, imm8);
        new rip_shll_1000(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlq  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shlq      [L1: +305419896], 0x12}
     */
    // Template#: 667, Serial#: 1063
    public void rip_shlq(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shlq(placeHolder, imm8);
        new rip_shlq_1063(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shlw  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shlw      [L1: +305419896], 0x12}
     */
    // Template#: 668, Serial#: 1126
    public void rip_shlw(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shlw(placeHolder, imm8);
        new rip_shlw_1126(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shld  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code shld      [L1: +305419896], ax, cl}
     */
    // Template#: 669, Serial#: 4212
    public void rip_shld_CL(final Label label, final AMD64GeneralRegister16 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shld_CL(placeHolder, source);
        new rip_shld_CL_4212(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shld  }<i>label</i>, <i>source</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shld      [L1: +305419896], ax, 0x12}
     */
    // Template#: 670, Serial#: 4185
    public void rip_shld(final Label label, final AMD64GeneralRegister16 source, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shld(placeHolder, source, imm8);
        new rip_shld_4185(startPosition, currentPosition() - startPosition, source, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shld  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code shld      [L1: +305419896], eax, cl}
     */
    // Template#: 671, Serial#: 4194
    public void rip_shld_CL(final Label label, final AMD64GeneralRegister32 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shld_CL(placeHolder, source);
        new rip_shld_CL_4194(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shld  }<i>label</i>, <i>source</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shld      [L1: +305419896], eax, 0x12}
     */
    // Template#: 672, Serial#: 4167
    public void rip_shld(final Label label, final AMD64GeneralRegister32 source, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shld(placeHolder, source, imm8);
        new rip_shld_4167(startPosition, currentPosition() - startPosition, source, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shld  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code shld      [L1: +305419896], rax, cl}
     */
    // Template#: 673, Serial#: 4203
    public void rip_shld_CL(final Label label, final AMD64GeneralRegister64 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shld_CL(placeHolder, source);
        new rip_shld_CL_4203(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shld  }<i>label</i>, <i>source</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shld      [L1: +305419896], rax, 0x12}
     */
    // Template#: 674, Serial#: 4176
    public void rip_shld(final Label label, final AMD64GeneralRegister64 source, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shld(placeHolder, source, imm8);
        new rip_shld_4176(startPosition, currentPosition() - startPosition, source, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrb  }<i>label</i>
     * Example disassembly syntax: {@code shrb      [L1: +305419896], 0x1}
     */
    // Template#: 675, Serial#: 1231
    public void rip_shrb___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shrb___1(placeHolder);
        new rip_shrb___1_1231(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrl  }<i>label</i>
     * Example disassembly syntax: {@code shrl      [L1: +305419896], 0x1}
     */
    // Template#: 676, Serial#: 1294
    public void rip_shrl___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shrl___1(placeHolder);
        new rip_shrl___1_1294(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrq  }<i>label</i>
     * Example disassembly syntax: {@code shrq      [L1: +305419896], 0x1}
     */
    // Template#: 677, Serial#: 1357
    public void rip_shrq___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shrq___1(placeHolder);
        new rip_shrq___1_1357(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrw  }<i>label</i>
     * Example disassembly syntax: {@code shrw      [L1: +305419896], 0x1}
     */
    // Template#: 678, Serial#: 1420
    public void rip_shrw___1(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shrw___1(placeHolder);
        new rip_shrw___1_1420(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrb  }<i>label</i>
     * Example disassembly syntax: {@code shrb      [L1: +305419896], cl}
     */
    // Template#: 679, Serial#: 1483
    public void rip_shrb___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shrb___CL(placeHolder);
        new rip_shrb___CL_1483(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrl  }<i>label</i>
     * Example disassembly syntax: {@code shrl      [L1: +305419896], cl}
     */
    // Template#: 680, Serial#: 1546
    public void rip_shrl___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shrl___CL(placeHolder);
        new rip_shrl___CL_1546(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrq  }<i>label</i>
     * Example disassembly syntax: {@code shrq      [L1: +305419896], cl}
     */
    // Template#: 681, Serial#: 1609
    public void rip_shrq___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shrq___CL(placeHolder);
        new rip_shrq___CL_1609(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrw  }<i>label</i>
     * Example disassembly syntax: {@code shrw      [L1: +305419896], cl}
     */
    // Template#: 682, Serial#: 1672
    public void rip_shrw___CL(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shrw___CL(placeHolder);
        new rip_shrw___CL_1672(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrb  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shrb      [L1: +305419896], 0x12}
     */
    // Template#: 683, Serial#: 941
    public void rip_shrb(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shrb(placeHolder, imm8);
        new rip_shrb_941(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrl  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shrl      [L1: +305419896], 0x12}
     */
    // Template#: 684, Serial#: 1004
    public void rip_shrl(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shrl(placeHolder, imm8);
        new rip_shrl_1004(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrq  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shrq      [L1: +305419896], 0x12}
     */
    // Template#: 685, Serial#: 1067
    public void rip_shrq(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shrq(placeHolder, imm8);
        new rip_shrq_1067(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrw  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shrw      [L1: +305419896], 0x12}
     */
    // Template#: 686, Serial#: 1130
    public void rip_shrw(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shrw(placeHolder, imm8);
        new rip_shrw_1130(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrd  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code shrd      [L1: +305419896], ax, cl}
     */
    // Template#: 687, Serial#: 6051
    public void rip_shrd_CL(final Label label, final AMD64GeneralRegister16 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shrd_CL(placeHolder, source);
        new rip_shrd_CL_6051(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrd  }<i>label</i>, <i>source</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shrd      [L1: +305419896], ax, 0x12}
     */
    // Template#: 688, Serial#: 6024
    public void rip_shrd(final Label label, final AMD64GeneralRegister16 source, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shrd(placeHolder, source, imm8);
        new rip_shrd_6024(startPosition, currentPosition() - startPosition, source, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrd  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code shrd      [L1: +305419896], eax, cl}
     */
    // Template#: 689, Serial#: 6033
    public void rip_shrd_CL(final Label label, final AMD64GeneralRegister32 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shrd_CL(placeHolder, source);
        new rip_shrd_CL_6033(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrd  }<i>label</i>, <i>source</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shrd      [L1: +305419896], eax, 0x12}
     */
    // Template#: 690, Serial#: 6006
    public void rip_shrd(final Label label, final AMD64GeneralRegister32 source, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shrd(placeHolder, source, imm8);
        new rip_shrd_6006(startPosition, currentPosition() - startPosition, source, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrd  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code shrd      [L1: +305419896], rax, cl}
     */
    // Template#: 691, Serial#: 6042
    public void rip_shrd_CL(final Label label, final AMD64GeneralRegister64 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shrd_CL(placeHolder, source);
        new rip_shrd_CL_6042(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shrd  }<i>label</i>, <i>source</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shrd      [L1: +305419896], rax, 0x12}
     */
    // Template#: 692, Serial#: 6015
    public void rip_shrd(final Label label, final AMD64GeneralRegister64 source, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shrd(placeHolder, source, imm8);
        new rip_shrd_6015(startPosition, currentPosition() - startPosition, source, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shufpd  }<i>destination</i>, <i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shufpd    xmm0, [L1: +305419896], 0x12}
     */
    // Template#: 693, Serial#: 4436
    public void rip_shufpd(final AMD64XMMRegister destination, final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shufpd(destination, placeHolder, imm8);
        new rip_shufpd_4436(startPosition, currentPosition() - startPosition, destination, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code shufps  }<i>destination</i>, <i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code shufps    xmm0, [L1: +305419896], 0x12}
     */
    // Template#: 694, Serial#: 4400
    public void rip_shufps(final AMD64XMMRegister destination, final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_shufps(destination, placeHolder, imm8);
        new rip_shufps_4400(startPosition, currentPosition() - startPosition, destination, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sidt  }<i>label</i>
     * Example disassembly syntax: {@code sidt      [L1: +305419896]}
     */
    // Template#: 695, Serial#: 3139
    public void rip_sidt(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sidt(placeHolder);
        new rip_sidt_3139(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sldt  }<i>label</i>
     * Example disassembly syntax: {@code sldt      [L1: +305419896]}
     */
    // Template#: 696, Serial#: 3077
    public void rip_sldt(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sldt(placeHolder);
        new rip_sldt_3077(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code smsw  }<i>label</i>
     * Example disassembly syntax: {@code smsw      [L1: +305419896]}
     */
    // Template#: 697, Serial#: 3151
    public void rip_smsw(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_smsw(placeHolder);
        new rip_smsw_3151(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sqrtpd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code sqrtpd    xmm0, [L1: +305419896]}
     */
    // Template#: 698, Serial#: 3723
    public void rip_sqrtpd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sqrtpd(destination, placeHolder);
        new rip_sqrtpd_3723(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sqrtps  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code sqrtps    xmm0, [L1: +305419896]}
     */
    // Template#: 699, Serial#: 3659
    public void rip_sqrtps(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sqrtps(destination, placeHolder);
        new rip_sqrtps_3659(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sqrtsd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code sqrtsd    xmm0, [L1: +305419896]}
     */
    // Template#: 700, Serial#: 3768
    public void rip_sqrtsd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sqrtsd(destination, placeHolder);
        new rip_sqrtsd_3768(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sqrtss  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code sqrtss    xmm0, [L1: +305419896]}
     */
    // Template#: 701, Serial#: 3777
    public void rip_sqrtss(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sqrtss(destination, placeHolder);
        new rip_sqrtss_3777(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code stmxcsr  }<i>label</i>
     * Example disassembly syntax: {@code stmxcsr   [L1: +305419896]}
     */
    // Template#: 702, Serial#: 6072
    public void rip_stmxcsr(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_stmxcsr(placeHolder);
        new rip_stmxcsr_6072(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code str  }<i>label</i>
     * Example disassembly syntax: {@code str       [L1: +305419896]}
     */
    // Template#: 703, Serial#: 3081
    public void rip_str(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_str(placeHolder);
        new rip_str_3081(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code sub       ax, [L1: +305419896]}
     */
    // Template#: 704, Serial#: 2190
    public void rip_sub(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sub(destination, placeHolder);
        new rip_sub_2190(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code sub       eax, [L1: +305419896]}
     */
    // Template#: 705, Serial#: 2174
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
    // Template#: 706, Serial#: 2182
    public void rip_sub(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sub(destination, placeHolder);
        new rip_sub_2182(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code sub       al, [L1: +305419896]}
     */
    // Template#: 707, Serial#: 2166
    public void rip_sub(final AMD64GeneralRegister8 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sub(destination, placeHolder);
        new rip_sub_2166(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code subb  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code subb      [L1: +305419896], 0x12}
     */
    // Template#: 708, Serial#: 344
    public void rip_subb(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_subb(placeHolder, imm8);
        new rip_subb_344(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code subl  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code subl      [L1: +305419896], 0x12}
     */
    // Template#: 709, Serial#: 632
    public void rip_subl(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_subl(placeHolder, imm8);
        new rip_subl_632(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code subq  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code subq      [L1: +305419896], 0x12}
     */
    // Template#: 710, Serial#: 704
    public void rip_subq(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_subq(placeHolder, imm8);
        new rip_subq_704(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code subw  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code subw      [L1: +305419896], 0x12}
     */
    // Template#: 711, Serial#: 776
    public void rip_subw(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_subw(placeHolder, imm8);
        new rip_subw_776(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code sub       [L1: +305419896], ax}
     */
    // Template#: 712, Serial#: 2157
    public void rip_sub(final Label label, final AMD64GeneralRegister16 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sub(placeHolder, source);
        new rip_sub_2157(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code sub       [L1: +305419896], eax}
     */
    // Template#: 713, Serial#: 2139
    public void rip_sub(final Label label, final AMD64GeneralRegister32 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sub(placeHolder, source);
        new rip_sub_2139(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code sub       [L1: +305419896], rax}
     */
    // Template#: 714, Serial#: 2148
    public void rip_sub(final Label label, final AMD64GeneralRegister64 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sub(placeHolder, source);
        new rip_sub_2148(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code sub  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code sub       [L1: +305419896], al}
     */
    // Template#: 715, Serial#: 2130
    public void rip_sub(final Label label, final AMD64GeneralRegister8 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_sub(placeHolder, source);
        new rip_sub_2130(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code subl  }<i>label</i>, <i>imm32</i>
     * Example disassembly syntax: {@code subl      [L1: +305419896], 0x12345678}
     */
    // Template#: 716, Serial#: 416
    public void rip_subl(final Label label, final int imm32) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_subl(placeHolder, imm32);
        new rip_subl_416(startPosition, currentPosition() - startPosition, imm32, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code subq  }<i>label</i>, <i>imm32</i>
     * Example disassembly syntax: {@code subq      [L1: +305419896], 0x12345678}
     */
    // Template#: 717, Serial#: 488
    public void rip_subq(final Label label, final int imm32) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_subq(placeHolder, imm32);
        new rip_subq_488(startPosition, currentPosition() - startPosition, imm32, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code subw  }<i>label</i>, <i>imm16</i>
     * Example disassembly syntax: {@code subw      [L1: +305419896], 0x1234}
     */
    // Template#: 718, Serial#: 560
    public void rip_subw(final Label label, final short imm16) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_subw(placeHolder, imm16);
        new rip_subw_560(startPosition, currentPosition() - startPosition, imm16, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code subpd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code subpd     xmm0, [L1: +305419896]}
     */
    // Template#: 719, Serial#: 5468
    public void rip_subpd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_subpd(destination, placeHolder);
        new rip_subpd_5468(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code subps  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code subps     xmm0, [L1: +305419896]}
     */
    // Template#: 720, Serial#: 5396
    public void rip_subps(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_subps(destination, placeHolder);
        new rip_subps_5396(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code subsd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code subsd     xmm0, [L1: +305419896]}
     */
    // Template#: 721, Serial#: 5531
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
    // Template#: 722, Serial#: 5603
    public void rip_subss(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_subss(destination, placeHolder);
        new rip_subss_5603(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code testb  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code testb     [L1: +305419896], 0x12}
     */
    // Template#: 723, Serial#: 1734
    public void rip_testb(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_testb(placeHolder, imm8);
        new rip_testb_1734(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code test  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code test      [L1: +305419896], ax}
     */
    // Template#: 724, Serial#: 855
    public void rip_test(final Label label, final AMD64GeneralRegister16 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_test(placeHolder, source);
        new rip_test_855(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code test  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code test      [L1: +305419896], eax}
     */
    // Template#: 725, Serial#: 837
    public void rip_test(final Label label, final AMD64GeneralRegister32 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_test(placeHolder, source);
        new rip_test_837(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code test  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code test      [L1: +305419896], rax}
     */
    // Template#: 726, Serial#: 846
    public void rip_test(final Label label, final AMD64GeneralRegister64 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_test(placeHolder, source);
        new rip_test_846(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code test  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code test      [L1: +305419896], al}
     */
    // Template#: 727, Serial#: 828
    public void rip_test(final Label label, final AMD64GeneralRegister8 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_test(placeHolder, source);
        new rip_test_828(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code testl  }<i>label</i>, <i>imm32</i>
     * Example disassembly syntax: {@code testl     [L1: +305419896], 0x12345678}
     */
    // Template#: 728, Serial#: 1797
    public void rip_testl(final Label label, final int imm32) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_testl(placeHolder, imm32);
        new rip_testl_1797(startPosition, currentPosition() - startPosition, imm32, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code testq  }<i>label</i>, <i>imm32</i>
     * Example disassembly syntax: {@code testq     [L1: +305419896], 0x12345678}
     */
    // Template#: 729, Serial#: 1860
    public void rip_testq(final Label label, final int imm32) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_testq(placeHolder, imm32);
        new rip_testq_1860(startPosition, currentPosition() - startPosition, imm32, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code testw  }<i>label</i>, <i>imm16</i>
     * Example disassembly syntax: {@code testw     [L1: +305419896], 0x1234}
     */
    // Template#: 730, Serial#: 1923
    public void rip_testw(final Label label, final short imm16) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_testw(placeHolder, imm16);
        new rip_testw_1923(startPosition, currentPosition() - startPosition, imm16, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code ucomisd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code ucomisd   xmm0, [L1: +305419896]}
     */
    // Template#: 731, Serial#: 5018
    public void rip_ucomisd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_ucomisd(destination, placeHolder);
        new rip_ucomisd_5018(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code ucomiss  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code ucomiss   xmm0, [L1: +305419896]}
     */
    // Template#: 732, Serial#: 4948
    public void rip_ucomiss(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_ucomiss(destination, placeHolder);
        new rip_ucomiss_4948(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code unpckhpd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code unpckhpd  xmm0, [L1: +305419896]}
     */
    // Template#: 733, Serial#: 3348
    public void rip_unpckhpd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_unpckhpd(destination, placeHolder);
        new rip_unpckhpd_3348(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code unpckhps  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code unpckhps  xmm0, [L1: +305419896]}
     */
    // Template#: 734, Serial#: 3288
    public void rip_unpckhps(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_unpckhps(destination, placeHolder);
        new rip_unpckhps_3288(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code unpcklpd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code unpcklpd  xmm0, [L1: +305419896]}
     */
    // Template#: 735, Serial#: 3339
    public void rip_unpcklpd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_unpcklpd(destination, placeHolder);
        new rip_unpcklpd_3339(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code unpcklps  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code unpcklps  xmm0, [L1: +305419896]}
     */
    // Template#: 736, Serial#: 3279
    public void rip_unpcklps(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_unpcklps(destination, placeHolder);
        new rip_unpcklps_3279(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code verr  }<i>label</i>
     * Example disassembly syntax: {@code verr      [L1: +305419896]}
     */
    // Template#: 737, Serial#: 3093
    public void rip_verr(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_verr(placeHolder);
        new rip_verr_3093(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code verw  }<i>label</i>
     * Example disassembly syntax: {@code verw      [L1: +305419896]}
     */
    // Template#: 738, Serial#: 3097
    public void rip_verw(final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_verw(placeHolder);
        new rip_verw_3097(startPosition, currentPosition() - startPosition, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xadd  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code xadd      [L1: +305419896], ax}
     */
    // Template#: 739, Serial#: 4356
    public void rip_xadd(final Label label, final AMD64GeneralRegister16 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xadd(placeHolder, source);
        new rip_xadd_4356(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xadd  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code xadd      [L1: +305419896], eax}
     */
    // Template#: 740, Serial#: 4338
    public void rip_xadd(final Label label, final AMD64GeneralRegister32 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xadd(placeHolder, source);
        new rip_xadd_4338(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xadd  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code xadd      [L1: +305419896], rax}
     */
    // Template#: 741, Serial#: 4347
    public void rip_xadd(final Label label, final AMD64GeneralRegister64 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xadd(placeHolder, source);
        new rip_xadd_4347(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xadd  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code xadd      [L1: +305419896], al}
     */
    // Template#: 742, Serial#: 4329
    public void rip_xadd(final Label label, final AMD64GeneralRegister8 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xadd(placeHolder, source);
        new rip_xadd_4329(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xchg  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code xchg      [L1: +305419896], ax}
     */
    // Template#: 743, Serial#: 891
    public void rip_xchg(final Label label, final AMD64GeneralRegister16 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xchg(placeHolder, source);
        new rip_xchg_891(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xchg  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code xchg      [L1: +305419896], eax}
     */
    // Template#: 744, Serial#: 873
    public void rip_xchg(final Label label, final AMD64GeneralRegister32 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xchg(placeHolder, source);
        new rip_xchg_873(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xchg  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code xchg      [L1: +305419896], rax}
     */
    // Template#: 745, Serial#: 882
    public void rip_xchg(final Label label, final AMD64GeneralRegister64 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xchg(placeHolder, source);
        new rip_xchg_882(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xchg  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code xchg      [L1: +305419896], al}
     */
    // Template#: 746, Serial#: 864
    public void rip_xchg(final Label label, final AMD64GeneralRegister8 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xchg(placeHolder, source);
        new rip_xchg_864(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code xor       ax, [L1: +305419896]}
     */
    // Template#: 747, Serial#: 280
    public void rip_xor(final AMD64GeneralRegister16 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xor(destination, placeHolder);
        new rip_xor_280(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code xor       eax, [L1: +305419896]}
     */
    // Template#: 748, Serial#: 264
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
    // Template#: 749, Serial#: 272
    public void rip_xor(final AMD64GeneralRegister64 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xor(destination, placeHolder);
        new rip_xor_272(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code xor       al, [L1: +305419896]}
     */
    // Template#: 750, Serial#: 256
    public void rip_xor(final AMD64GeneralRegister8 destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xor(destination, placeHolder);
        new rip_xor_256(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorb  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code xorb      [L1: +305419896], 0x12}
     */
    // Template#: 751, Serial#: 348
    public void rip_xorb(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xorb(placeHolder, imm8);
        new rip_xorb_348(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorl  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code xorl      [L1: +305419896], 0x12}
     */
    // Template#: 752, Serial#: 636
    public void rip_xorl(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xorl(placeHolder, imm8);
        new rip_xorl_636(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorq  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code xorq      [L1: +305419896], 0x12}
     */
    // Template#: 753, Serial#: 708
    public void rip_xorq(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xorq(placeHolder, imm8);
        new rip_xorq_708(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorw  }<i>label</i>, <i>imm8</i>
     * Example disassembly syntax: {@code xorw      [L1: +305419896], 0x12}
     */
    // Template#: 754, Serial#: 780
    public void rip_xorw(final Label label, final byte imm8) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xorw(placeHolder, imm8);
        new rip_xorw_780(startPosition, currentPosition() - startPosition, imm8, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code xor       [L1: +305419896], ax}
     */
    // Template#: 755, Serial#: 247
    public void rip_xor(final Label label, final AMD64GeneralRegister16 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xor(placeHolder, source);
        new rip_xor_247(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code xor       [L1: +305419896], eax}
     */
    // Template#: 756, Serial#: 229
    public void rip_xor(final Label label, final AMD64GeneralRegister32 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xor(placeHolder, source);
        new rip_xor_229(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code xor       [L1: +305419896], rax}
     */
    // Template#: 757, Serial#: 238
    public void rip_xor(final Label label, final AMD64GeneralRegister64 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xor(placeHolder, source);
        new rip_xor_238(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xor  }<i>label</i>, <i>source</i>
     * Example disassembly syntax: {@code xor       [L1: +305419896], al}
     */
    // Template#: 758, Serial#: 220
    public void rip_xor(final Label label, final AMD64GeneralRegister8 source) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xor(placeHolder, source);
        new rip_xor_220(startPosition, currentPosition() - startPosition, source, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorl  }<i>label</i>, <i>imm32</i>
     * Example disassembly syntax: {@code xorl      [L1: +305419896], 0x12345678}
     */
    // Template#: 759, Serial#: 420
    public void rip_xorl(final Label label, final int imm32) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xorl(placeHolder, imm32);
        new rip_xorl_420(startPosition, currentPosition() - startPosition, imm32, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorq  }<i>label</i>, <i>imm32</i>
     * Example disassembly syntax: {@code xorq      [L1: +305419896], 0x12345678}
     */
    // Template#: 760, Serial#: 492
    public void rip_xorq(final Label label, final int imm32) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xorq(placeHolder, imm32);
        new rip_xorq_492(startPosition, currentPosition() - startPosition, imm32, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorw  }<i>label</i>, <i>imm16</i>
     * Example disassembly syntax: {@code xorw      [L1: +305419896], 0x1234}
     */
    // Template#: 761, Serial#: 564
    public void rip_xorw(final Label label, final short imm16) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xorw(placeHolder, imm16);
        new rip_xorw_564(startPosition, currentPosition() - startPosition, imm16, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorpd  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code xorpd     xmm0, [L1: +305419896]}
     */
    // Template#: 762, Serial#: 3759
    public void rip_xorpd(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xorpd(destination, placeHolder);
        new rip_xorpd_3759(startPosition, currentPosition() - startPosition, destination, label);
    }

    /**
     * Pseudo-external assembler syntax: {@code xorps  }<i>destination</i>, <i>label</i>
     * Example disassembly syntax: {@code xorps     xmm0, [L1: +305419896]}
     */
    // Template#: 763, Serial#: 3713
    public void rip_xorps(final AMD64XMMRegister destination, final Label label) {
        final int startPosition = currentPosition();
        final int placeHolder = 0;
        rip_xorps(destination, placeHolder);
        new rip_xorps_3713(startPosition, currentPosition() - startPosition, destination, label);
    }

    class rip_adc_136 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_adc_136(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_adc(_destination, offsetAsInt());
        }
    }

    class rip_adc_120 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_adc_120(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_adc(_destination, offsetAsInt());
        }
    }

    class rip_adc_128 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_adc_128(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_adc(_destination, offsetAsInt());
        }
    }

    class rip_adc_112 extends InstructionWithOffset {
        private final AMD64GeneralRegister8 _destination;
        rip_adc_112(int startPosition, int endPosition, AMD64GeneralRegister8 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_adc(_destination, offsetAsInt());
        }
    }

    class rip_adcb_332 extends InstructionWithOffset {
        private final byte _imm8;
        rip_adcb_332(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_adcb(offsetAsInt(), _imm8);
        }
    }

    class rip_adcl_620 extends InstructionWithOffset {
        private final byte _imm8;
        rip_adcl_620(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_adcl(offsetAsInt(), _imm8);
        }
    }

    class rip_adcq_692 extends InstructionWithOffset {
        private final byte _imm8;
        rip_adcq_692(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_adcq(offsetAsInt(), _imm8);
        }
    }

    class rip_adcw_764 extends InstructionWithOffset {
        private final byte _imm8;
        rip_adcw_764(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_adcw(offsetAsInt(), _imm8);
        }
    }

    class rip_adc_103 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _source;
        rip_adc_103(int startPosition, int endPosition, AMD64GeneralRegister16 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_adc(offsetAsInt(), _source);
        }
    }

    class rip_adc_85 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _source;
        rip_adc_85(int startPosition, int endPosition, AMD64GeneralRegister32 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_adc(offsetAsInt(), _source);
        }
    }

    class rip_adc_94 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _source;
        rip_adc_94(int startPosition, int endPosition, AMD64GeneralRegister64 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_adc(offsetAsInt(), _source);
        }
    }

    class rip_adc_76 extends InstructionWithOffset {
        private final AMD64GeneralRegister8 _source;
        rip_adc_76(int startPosition, int endPosition, AMD64GeneralRegister8 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_adc(offsetAsInt(), _source);
        }
    }

    class rip_adcl_404 extends InstructionWithOffset {
        private final int _imm32;
        rip_adcl_404(int startPosition, int endPosition, int imm32, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm32 = imm32;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_adcl(offsetAsInt(), _imm32);
        }
    }

    class rip_adcq_476 extends InstructionWithOffset {
        private final int _imm32;
        rip_adcq_476(int startPosition, int endPosition, int imm32, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm32 = imm32;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_adcq(offsetAsInt(), _imm32);
        }
    }

    class rip_adcw_548 extends InstructionWithOffset {
        private final short _imm16;
        rip_adcw_548(int startPosition, int endPosition, short imm16, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm16 = imm16;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_adcw(offsetAsInt(), _imm16);
        }
    }

    class rip_add_64 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_add_64(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_add(_destination, offsetAsInt());
        }
    }

    class rip_add_48 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_add_48(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_add(_destination, offsetAsInt());
        }
    }

    class rip_add_40 extends InstructionWithOffset {
        private final AMD64GeneralRegister8 _destination;
        rip_add_40(int startPosition, int endPosition, AMD64GeneralRegister8 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_add(_destination, offsetAsInt());
        }
    }

    class rip_addb_324 extends InstructionWithOffset {
        private final byte _imm8;
        rip_addb_324(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_addb(offsetAsInt(), _imm8);
        }
    }

    class rip_addl_612 extends InstructionWithOffset {
        private final byte _imm8;
        rip_addl_612(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_addl(offsetAsInt(), _imm8);
        }
    }

    class rip_addq_684 extends InstructionWithOffset {
        private final byte _imm8;
        rip_addq_684(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_addq(offsetAsInt(), _imm8);
        }
    }

    class rip_addw_756 extends InstructionWithOffset {
        private final byte _imm8;
        rip_addw_756(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_addw(offsetAsInt(), _imm8);
        }
    }

    class rip_add_31 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _source;
        rip_add_31(int startPosition, int endPosition, AMD64GeneralRegister16 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_add(offsetAsInt(), _source);
        }
    }

    class rip_add_13 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _source;
        rip_add_13(int startPosition, int endPosition, AMD64GeneralRegister32 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_add(offsetAsInt(), _source);
        }
    }

    class rip_add_22 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _source;
        rip_add_22(int startPosition, int endPosition, AMD64GeneralRegister64 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_add(offsetAsInt(), _source);
        }
    }

    class rip_add_4 extends InstructionWithOffset {
        private final AMD64GeneralRegister8 _source;
        rip_add_4(int startPosition, int endPosition, AMD64GeneralRegister8 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_add(offsetAsInt(), _source);
        }
    }

    class rip_addl_396 extends InstructionWithOffset {
        private final int _imm32;
        rip_addl_396(int startPosition, int endPosition, int imm32, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm32 = imm32;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_addl(offsetAsInt(), _imm32);
        }
    }

    class rip_addq_468 extends InstructionWithOffset {
        private final int _imm32;
        rip_addq_468(int startPosition, int endPosition, int imm32, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm32 = imm32;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_addq(offsetAsInt(), _imm32);
        }
    }

    class rip_addw_540 extends InstructionWithOffset {
        private final short _imm16;
        rip_addw_540(int startPosition, int endPosition, short imm16, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm16 = imm16;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_addw(offsetAsInt(), _imm16);
        }
    }

    class rip_addpd_5432 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_addpd_5432(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_addpd(_destination, offsetAsInt());
        }
    }

    class rip_addps_5360 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_addps_5360(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_addps(_destination, offsetAsInt());
        }
    }

    class rip_addsd_5504 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_addsd_5504(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_addss(_destination, offsetAsInt());
        }
    }

    class rip_addsubpd_4509 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_addsubpd_4509(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_addsubpd(_destination, offsetAsInt());
        }
    }

    class rip_and_208 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_and_208(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_and(_destination, offsetAsInt());
        }
    }

    class rip_and_192 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_and_192(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_and(_destination, offsetAsInt());
        }
    }

    class rip_and_184 extends InstructionWithOffset {
        private final AMD64GeneralRegister8 _destination;
        rip_and_184(int startPosition, int endPosition, AMD64GeneralRegister8 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_and(_destination, offsetAsInt());
        }
    }

    class rip_andb_340 extends InstructionWithOffset {
        private final byte _imm8;
        rip_andb_340(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_andb(offsetAsInt(), _imm8);
        }
    }

    class rip_andl_628 extends InstructionWithOffset {
        private final byte _imm8;
        rip_andl_628(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_andl(offsetAsInt(), _imm8);
        }
    }

    class rip_andq_700 extends InstructionWithOffset {
        private final byte _imm8;
        rip_andq_700(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_andq(offsetAsInt(), _imm8);
        }
    }

    class rip_andw_772 extends InstructionWithOffset {
        private final byte _imm8;
        rip_andw_772(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_andw(offsetAsInt(), _imm8);
        }
    }

    class rip_and_175 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _source;
        rip_and_175(int startPosition, int endPosition, AMD64GeneralRegister16 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_and(offsetAsInt(), _source);
        }
    }

    class rip_and_157 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _source;
        rip_and_157(int startPosition, int endPosition, AMD64GeneralRegister32 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_and(offsetAsInt(), _source);
        }
    }

    class rip_and_166 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _source;
        rip_and_166(int startPosition, int endPosition, AMD64GeneralRegister64 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_and(offsetAsInt(), _source);
        }
    }

    class rip_and_148 extends InstructionWithOffset {
        private final AMD64GeneralRegister8 _source;
        rip_and_148(int startPosition, int endPosition, AMD64GeneralRegister8 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_and(offsetAsInt(), _source);
        }
    }

    class rip_andl_412 extends InstructionWithOffset {
        private final int _imm32;
        rip_andl_412(int startPosition, int endPosition, int imm32, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm32 = imm32;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_andl(offsetAsInt(), _imm32);
        }
    }

    class rip_andq_484 extends InstructionWithOffset {
        private final int _imm32;
        rip_andq_484(int startPosition, int endPosition, int imm32, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm32 = imm32;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_andq(offsetAsInt(), _imm32);
        }
    }

    class rip_andw_556 extends InstructionWithOffset {
        private final short _imm16;
        rip_andw_556(int startPosition, int endPosition, short imm16, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm16 = imm16;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_andw(offsetAsInt(), _imm16);
        }
    }

    class rip_andnpd_3741 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_andnpd_3741(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_andnpd(_destination, offsetAsInt());
        }
    }

    class rip_andnps_3695 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_andnps_3695(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_andnps(_destination, offsetAsInt());
        }
    }

    class rip_andpd_3732 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_andpd_3732(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_andpd(_destination, offsetAsInt());
        }
    }

    class rip_andps_3686 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_andps_3686(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_andps(_destination, offsetAsInt());
        }
    }

    class rip_bsf_6219 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_bsf_6219(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_bsf(_destination, offsetAsInt());
        }
    }

    class rip_bsf_6201 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_bsf_6201(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_bsf(_destination, offsetAsInt());
        }
    }

    class rip_bsf_6210 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_bsf_6210(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_bsf(_destination, offsetAsInt());
        }
    }

    class rip_bsr_6246 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_bsr_6246(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_bsr(_destination, offsetAsInt());
        }
    }

    class rip_bsr_6228 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_bsr_6228(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_bsr(_destination, offsetAsInt());
        }
    }

    class rip_bsr_6237 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_bsr_6237(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_bsr(_destination, offsetAsInt());
        }
    }

    class rip_bt_6130 extends InstructionWithOffset {
        private final byte _imm8;
        rip_bt_6130(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_bt(offsetAsInt(), _imm8);
        }
    }

    class rip_bt_4158 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _source;
        rip_bt_4158(int startPosition, int endPosition, AMD64GeneralRegister16 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_bt(offsetAsInt(), _source);
        }
    }

    class rip_bt_4140 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _source;
        rip_bt_4140(int startPosition, int endPosition, AMD64GeneralRegister32 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_bt(offsetAsInt(), _source);
        }
    }

    class rip_bt_4149 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _source;
        rip_bt_4149(int startPosition, int endPosition, AMD64GeneralRegister64 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_bt(offsetAsInt(), _source);
        }
    }

    class rip_btc_6142 extends InstructionWithOffset {
        private final byte _imm8;
        rip_btc_6142(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_btc(offsetAsInt(), _imm8);
        }
    }

    class rip_btc_6192 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _source;
        rip_btc_6192(int startPosition, int endPosition, AMD64GeneralRegister16 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_btc(offsetAsInt(), _source);
        }
    }

    class rip_btc_6174 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _source;
        rip_btc_6174(int startPosition, int endPosition, AMD64GeneralRegister32 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_btc(offsetAsInt(), _source);
        }
    }

    class rip_btc_6183 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _source;
        rip_btc_6183(int startPosition, int endPosition, AMD64GeneralRegister64 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_btc(offsetAsInt(), _source);
        }
    }

    class rip_btr_6138 extends InstructionWithOffset {
        private final byte _imm8;
        rip_btr_6138(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_btr(offsetAsInt(), _imm8);
        }
    }

    class rip_btr_4275 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _source;
        rip_btr_4275(int startPosition, int endPosition, AMD64GeneralRegister16 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_btr(offsetAsInt(), _source);
        }
    }

    class rip_btr_4257 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _source;
        rip_btr_4257(int startPosition, int endPosition, AMD64GeneralRegister32 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_btr(offsetAsInt(), _source);
        }
    }

    class rip_btr_4266 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _source;
        rip_btr_4266(int startPosition, int endPosition, AMD64GeneralRegister64 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_btr(offsetAsInt(), _source);
        }
    }

    class rip_bts_6134 extends InstructionWithOffset {
        private final byte _imm8;
        rip_bts_6134(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_bts(offsetAsInt(), _imm8);
        }
    }

    class rip_bts_5997 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _source;
        rip_bts_5997(int startPosition, int endPosition, AMD64GeneralRegister16 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_bts(offsetAsInt(), _source);
        }
    }

    class rip_bts_5979 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _source;
        rip_bts_5979(int startPosition, int endPosition, AMD64GeneralRegister32 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_bts(offsetAsInt(), _source);
        }
    }

    class rip_bts_5988 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _source;
        rip_bts_5988(int startPosition, int endPosition, AMD64GeneralRegister64 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_bts(offsetAsInt(), _source);
        }
    }

    class call_2957 extends InstructionWithOffset {
        call_2957(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            call(offsetAsInt());
        }
    }

    class rip_call_3049 extends InstructionWithOffset {
        rip_call_3049(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_call(offsetAsInt());
        }
    }

    class rip_clflush_6076 extends InstructionWithOffset {
        rip_clflush_6076(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_clflush(offsetAsInt());
        }
    }

    class rip_cmova_3649 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_cmova_3649(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmova(_destination, offsetAsInt());
        }
    }

    class rip_cmova_3631 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmova_3631(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmova(_destination, offsetAsInt());
        }
    }

    class rip_cmova_3640 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_cmova_3640(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmova(_destination, offsetAsInt());
        }
    }

    class rip_cmovae_3541 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_cmovae_3541(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovae(_destination, offsetAsInt());
        }
    }

    class rip_cmovae_3523 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmovae_3523(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovae(_destination, offsetAsInt());
        }
    }

    class rip_cmovae_3532 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_cmovae_3532(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovae(_destination, offsetAsInt());
        }
    }

    class rip_cmovb_3514 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_cmovb_3514(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovb(_destination, offsetAsInt());
        }
    }

    class rip_cmovb_3496 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmovb_3496(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovb(_destination, offsetAsInt());
        }
    }

    class rip_cmovb_3505 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_cmovb_3505(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovb(_destination, offsetAsInt());
        }
    }

    class rip_cmovbe_3622 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_cmovbe_3622(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovbe(_destination, offsetAsInt());
        }
    }

    class rip_cmovbe_3604 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmovbe_3604(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovbe(_destination, offsetAsInt());
        }
    }

    class rip_cmovbe_3613 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_cmovbe_3613(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovbe(_destination, offsetAsInt());
        }
    }

    class rip_cmove_3568 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_cmove_3568(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmove(_destination, offsetAsInt());
        }
    }

    class rip_cmove_3550 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmove_3550(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmove(_destination, offsetAsInt());
        }
    }

    class rip_cmove_3559 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_cmove_3559(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmove(_destination, offsetAsInt());
        }
    }

    class rip_cmovg_5351 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_cmovg_5351(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovg(_destination, offsetAsInt());
        }
    }

    class rip_cmovg_5333 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmovg_5333(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovg(_destination, offsetAsInt());
        }
    }

    class rip_cmovg_5342 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_cmovg_5342(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovg(_destination, offsetAsInt());
        }
    }

    class rip_cmovge_5297 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_cmovge_5297(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovge(_destination, offsetAsInt());
        }
    }

    class rip_cmovge_5279 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmovge_5279(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovge(_destination, offsetAsInt());
        }
    }

    class rip_cmovge_5288 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_cmovge_5288(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovge(_destination, offsetAsInt());
        }
    }

    class rip_cmovl_5270 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_cmovl_5270(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovl(_destination, offsetAsInt());
        }
    }

    class rip_cmovl_5252 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmovl_5252(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovl(_destination, offsetAsInt());
        }
    }

    class rip_cmovl_5261 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_cmovl_5261(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovl(_destination, offsetAsInt());
        }
    }

    class rip_cmovle_5324 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_cmovle_5324(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovle(_destination, offsetAsInt());
        }
    }

    class rip_cmovle_5306 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmovle_5306(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovle(_destination, offsetAsInt());
        }
    }

    class rip_cmovle_5315 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_cmovle_5315(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovle(_destination, offsetAsInt());
        }
    }

    class rip_cmovne_3595 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_cmovne_3595(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovne(_destination, offsetAsInt());
        }
    }

    class rip_cmovne_3577 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmovne_3577(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovne(_destination, offsetAsInt());
        }
    }

    class rip_cmovne_3586 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_cmovne_3586(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovne(_destination, offsetAsInt());
        }
    }

    class rip_cmovno_3487 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_cmovno_3487(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovno(_destination, offsetAsInt());
        }
    }

    class rip_cmovno_3469 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmovno_3469(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovno(_destination, offsetAsInt());
        }
    }

    class rip_cmovno_3478 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_cmovno_3478(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovno(_destination, offsetAsInt());
        }
    }

    class rip_cmovnp_5243 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_cmovnp_5243(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovnp(_destination, offsetAsInt());
        }
    }

    class rip_cmovnp_5225 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmovnp_5225(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovnp(_destination, offsetAsInt());
        }
    }

    class rip_cmovnp_5234 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_cmovnp_5234(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovnp(_destination, offsetAsInt());
        }
    }

    class rip_cmovns_5189 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_cmovns_5189(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovns(_destination, offsetAsInt());
        }
    }

    class rip_cmovns_5171 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmovns_5171(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovns(_destination, offsetAsInt());
        }
    }

    class rip_cmovns_5180 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_cmovns_5180(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovns(_destination, offsetAsInt());
        }
    }

    class rip_cmovo_3460 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_cmovo_3460(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovo(_destination, offsetAsInt());
        }
    }

    class rip_cmovo_3442 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmovo_3442(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovo(_destination, offsetAsInt());
        }
    }

    class rip_cmovo_3451 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_cmovo_3451(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovo(_destination, offsetAsInt());
        }
    }

    class rip_cmovp_5216 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_cmovp_5216(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovp(_destination, offsetAsInt());
        }
    }

    class rip_cmovp_5198 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmovp_5198(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovp(_destination, offsetAsInt());
        }
    }

    class rip_cmovp_5207 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_cmovp_5207(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovp(_destination, offsetAsInt());
        }
    }

    class rip_cmovs_5162 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_cmovs_5162(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovs(_destination, offsetAsInt());
        }
    }

    class rip_cmovs_5144 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmovs_5144(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovs(_destination, offsetAsInt());
        }
    }

    class rip_cmovs_5153 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_cmovs_5153(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmovs(_destination, offsetAsInt());
        }
    }

    class rip_cmp_2264 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_cmp_2264(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmp(_destination, offsetAsInt());
        }
    }

    class rip_cmp_2248 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cmp_2248(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmp(_destination, offsetAsInt());
        }
    }

    class rip_cmp_2240 extends InstructionWithOffset {
        private final AMD64GeneralRegister8 _destination;
        rip_cmp_2240(int startPosition, int endPosition, AMD64GeneralRegister8 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmp(_destination, offsetAsInt());
        }
    }

    class rip_cmpb_352 extends InstructionWithOffset {
        private final byte _imm8;
        rip_cmpb_352(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmpb(offsetAsInt(), _imm8);
        }
    }

    class rip_cmpl_640 extends InstructionWithOffset {
        private final byte _imm8;
        rip_cmpl_640(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmpl(offsetAsInt(), _imm8);
        }
    }

    class rip_cmpq_712 extends InstructionWithOffset {
        private final byte _imm8;
        rip_cmpq_712(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmpq(offsetAsInt(), _imm8);
        }
    }

    class rip_cmpw_784 extends InstructionWithOffset {
        private final byte _imm8;
        rip_cmpw_784(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmpw(offsetAsInt(), _imm8);
        }
    }

    class rip_cmp_2231 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _source;
        rip_cmp_2231(int startPosition, int endPosition, AMD64GeneralRegister16 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmp(offsetAsInt(), _source);
        }
    }

    class rip_cmp_2213 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _source;
        rip_cmp_2213(int startPosition, int endPosition, AMD64GeneralRegister32 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmp(offsetAsInt(), _source);
        }
    }

    class rip_cmp_2222 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _source;
        rip_cmp_2222(int startPosition, int endPosition, AMD64GeneralRegister64 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmp(offsetAsInt(), _source);
        }
    }

    class rip_cmp_2204 extends InstructionWithOffset {
        private final AMD64GeneralRegister8 _source;
        rip_cmp_2204(int startPosition, int endPosition, AMD64GeneralRegister8 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmp(offsetAsInt(), _source);
        }
    }

    class rip_cmpl_424 extends InstructionWithOffset {
        private final int _imm32;
        rip_cmpl_424(int startPosition, int endPosition, int imm32, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm32 = imm32;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmpl(offsetAsInt(), _imm32);
        }
    }

    class rip_cmpq_496 extends InstructionWithOffset {
        private final int _imm32;
        rip_cmpq_496(int startPosition, int endPosition, int imm32, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm32 = imm32;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmpq(offsetAsInt(), _imm32);
        }
    }

    class rip_cmpw_568 extends InstructionWithOffset {
        private final short _imm16;
        rip_cmpw_568(int startPosition, int endPosition, short imm16, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm16 = imm16;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmpw(offsetAsInt(), _imm16);
        }
    }

    class rip_cmppd_4417 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        private final AMD64XMMComparison _amd64xmmcomparison;
        rip_cmppd_4417(int startPosition, int endPosition, AMD64XMMRegister destination, AMD64XMMComparison amd64xmmcomparison, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
            _amd64xmmcomparison = amd64xmmcomparison;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmppd(_destination, offsetAsInt(), _amd64xmmcomparison);
        }
    }

    class rip_cmpps_4365 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        private final AMD64XMMComparison _amd64xmmcomparison;
        rip_cmpps_4365(int startPosition, int endPosition, AMD64XMMRegister destination, AMD64XMMComparison amd64xmmcomparison, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
            _amd64xmmcomparison = amd64xmmcomparison;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmpps(_destination, offsetAsInt(), _amd64xmmcomparison);
        }
    }

    class rip_cmpsd_4445 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        private final AMD64XMMComparison _amd64xmmcomparison;
        rip_cmpsd_4445(int startPosition, int endPosition, AMD64XMMRegister destination, AMD64XMMComparison amd64xmmcomparison, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
            _amd64xmmcomparison = amd64xmmcomparison;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmpss(_destination, offsetAsInt(), _amd64xmmcomparison);
        }
    }

    class rip_cmpxchg_4248 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _source;
        rip_cmpxchg_4248(int startPosition, int endPosition, AMD64GeneralRegister16 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmpxchg(offsetAsInt(), _source);
        }
    }

    class rip_cmpxchg_4230 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _source;
        rip_cmpxchg_4230(int startPosition, int endPosition, AMD64GeneralRegister32 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmpxchg(offsetAsInt(), _source);
        }
    }

    class rip_cmpxchg_4239 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _source;
        rip_cmpxchg_4239(int startPosition, int endPosition, AMD64GeneralRegister64 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmpxchg(offsetAsInt(), _source);
        }
    }

    class rip_cmpxchg_4221 extends InstructionWithOffset {
        private final AMD64GeneralRegister8 _source;
        rip_cmpxchg_4221(int startPosition, int endPosition, AMD64GeneralRegister8 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmpxchg(offsetAsInt(), _source);
        }
    }

    class rip_cmpxchg16b_4409 extends InstructionWithOffset {
        rip_cmpxchg16b_4409(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cmpxchg16b(offsetAsInt());
        }
    }

    class rip_comisd_5027 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_comisd_5027(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_comiss(_destination, offsetAsInt());
        }
    }

    class rip_cvtdq2pd_4717 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_cvtdq2pd_4717(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtdq2pd(_destination, offsetAsInt());
        }
    }

    class rip_cvtdq2ps_5387 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_cvtdq2ps_5387(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtdq2ps(_destination, offsetAsInt());
        }
    }

    class rip_cvtpd2dq_4708 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_cvtpd2dq_4708(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtpd2dq(_destination, offsetAsInt());
        }
    }

    class rip_cvtpd2pi_5009 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_cvtpd2pi_5009(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtpd2pi(_destination, offsetAsInt());
        }
    }

    class rip_cvtpd2ps_5450 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_cvtpd2ps_5450(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtpd2ps(_destination, offsetAsInt());
        }
    }

    class rip_cvtpi2pd_4983 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_cvtpi2pd_4983(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtpi2pd(_destination, offsetAsInt());
        }
    }

    class rip_cvtpi2ps_4913 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_cvtpi2ps_4913(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtpi2ps(_destination, offsetAsInt());
        }
    }

    class rip_cvtps2dq_5459 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_cvtps2dq_5459(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtps2dq(_destination, offsetAsInt());
        }
    }

    class rip_cvtps2pd_5378 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_cvtps2pd_5378(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtps2pd(_destination, offsetAsInt());
        }
    }

    class rip_cvtps2pi_4939 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_cvtps2pi_4939(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtps2pi(_destination, offsetAsInt());
        }
    }

    class rip_cvtsd2si_5072 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cvtsd2si_5072(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvtss2si(_destination, offsetAsInt());
        }
    }

    class rip_cvttpd2dq_4691 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_cvttpd2dq_4691(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvttpd2dq(_destination, offsetAsInt());
        }
    }

    class rip_cvttpd2pi_5000 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_cvttpd2pi_5000(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvttpd2pi(_destination, offsetAsInt());
        }
    }

    class rip_cvttps2dq_5594 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_cvttps2dq_5594(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvttps2dq(_destination, offsetAsInt());
        }
    }

    class rip_cvttps2pi_4930 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_cvttps2pi_4930(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvttps2pi(_destination, offsetAsInt());
        }
    }

    class rip_cvttsd2si_5054 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_cvttsd2si_5054(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_cvttss2si(_destination, offsetAsInt());
        }
    }

    class rip_decb_2981 extends InstructionWithOffset {
        rip_decb_2981(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_decb(offsetAsInt());
        }
    }

    class rip_decl_2999 extends InstructionWithOffset {
        rip_decl_2999(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_decl(offsetAsInt());
        }
    }

    class rip_decq_3017 extends InstructionWithOffset {
        rip_decq_3017(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_decq(offsetAsInt());
        }
    }

    class rip_decw_3035 extends InstructionWithOffset {
        rip_decw_3035(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_decw(offsetAsInt());
        }
    }

    class rip_divb___AL_1754 extends InstructionWithOffset {
        rip_divb___AL_1754(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_divb___AL(offsetAsInt());
        }
    }

    class rip_divl_1817 extends InstructionWithOffset {
        rip_divl_1817(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_divl(offsetAsInt());
        }
    }

    class rip_divq_1880 extends InstructionWithOffset {
        rip_divq_1880(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_divq(offsetAsInt());
        }
    }

    class rip_divw_1943 extends InstructionWithOffset {
        rip_divw_1943(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_divw(offsetAsInt());
        }
    }

    class rip_divpd_5486 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_divpd_5486(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_divpd(_destination, offsetAsInt());
        }
    }

    class rip_divps_5414 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_divps_5414(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_divps(_destination, offsetAsInt());
        }
    }

    class rip_divsd_5549 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_divsd_5549(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_divss(_destination, offsetAsInt());
        }
    }

    class rip_fadds_2504 extends InstructionWithOffset {
        rip_fadds_2504(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fadds(offsetAsInt());
        }
    }

    class rip_faddl_2728 extends InstructionWithOffset {
        rip_faddl_2728(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_faddl(offsetAsInt());
        }
    }

    class rip_fbld_2916 extends InstructionWithOffset {
        rip_fbld_2916(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fbld(offsetAsInt());
        }
    }

    class rip_fbstp_2924 extends InstructionWithOffset {
        rip_fbstp_2924(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fbstp(offsetAsInt());
        }
    }

    class rip_fcoms_2512 extends InstructionWithOffset {
        rip_fcoms_2512(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fcoms(offsetAsInt());
        }
    }

    class rip_fcoml_2736 extends InstructionWithOffset {
        rip_fcoml_2736(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fcoml(offsetAsInt());
        }
    }

    class rip_fcomps_2516 extends InstructionWithOffset {
        rip_fcomps_2516(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fcomps(offsetAsInt());
        }
    }

    class rip_fcompl_2740 extends InstructionWithOffset {
        rip_fcompl_2740(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fcompl(offsetAsInt());
        }
    }

    class rip_fdivs_2528 extends InstructionWithOffset {
        rip_fdivs_2528(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fdivs(offsetAsInt());
        }
    }

    class rip_fdivl_2752 extends InstructionWithOffset {
        rip_fdivl_2752(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fdivl(offsetAsInt());
        }
    }

    class rip_fdivrs_2532 extends InstructionWithOffset {
        rip_fdivrs_2532(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fdivrs(offsetAsInt());
        }
    }

    class rip_fdivrl_2756 extends InstructionWithOffset {
        rip_fdivrl_2756(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fdivrl(offsetAsInt());
        }
    }

    class rip_fiaddl_2624 extends InstructionWithOffset {
        rip_fiaddl_2624(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fiaddl(offsetAsInt());
        }
    }

    class rip_fiadds_2840 extends InstructionWithOffset {
        rip_fiadds_2840(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fiadds(offsetAsInt());
        }
    }

    class rip_ficoml_2632 extends InstructionWithOffset {
        rip_ficoml_2632(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_ficoml(offsetAsInt());
        }
    }

    class rip_ficoms_2848 extends InstructionWithOffset {
        rip_ficoms_2848(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_ficoms(offsetAsInt());
        }
    }

    class rip_ficompl_2636 extends InstructionWithOffset {
        rip_ficompl_2636(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_ficompl(offsetAsInt());
        }
    }

    class rip_ficomps_2852 extends InstructionWithOffset {
        rip_ficomps_2852(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_ficomps(offsetAsInt());
        }
    }

    class rip_fidivl_2648 extends InstructionWithOffset {
        rip_fidivl_2648(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fidivl(offsetAsInt());
        }
    }

    class rip_fidivs_2864 extends InstructionWithOffset {
        rip_fidivs_2864(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fidivs(offsetAsInt());
        }
    }

    class rip_fidivrl_2652 extends InstructionWithOffset {
        rip_fidivrl_2652(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fidivrl(offsetAsInt());
        }
    }

    class rip_fidivrs_2868 extends InstructionWithOffset {
        rip_fidivrs_2868(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fidivrs(offsetAsInt());
        }
    }

    class rip_fildl_2688 extends InstructionWithOffset {
        rip_fildl_2688(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fildl(offsetAsInt());
        }
    }

    class rip_filds_2904 extends InstructionWithOffset {
        rip_filds_2904(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_filds(offsetAsInt());
        }
    }

    class rip_fildq_2920 extends InstructionWithOffset {
        rip_fildq_2920(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fildq(offsetAsInt());
        }
    }

    class rip_fimull_2628 extends InstructionWithOffset {
        rip_fimull_2628(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fimull(offsetAsInt());
        }
    }

    class rip_fimuls_2844 extends InstructionWithOffset {
        rip_fimuls_2844(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fimuls(offsetAsInt());
        }
    }

    class rip_fistl_2692 extends InstructionWithOffset {
        rip_fistl_2692(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fistl(offsetAsInt());
        }
    }

    class rip_fists_2908 extends InstructionWithOffset {
        rip_fists_2908(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fists(offsetAsInt());
        }
    }

    class rip_fistpl_2696 extends InstructionWithOffset {
        rip_fistpl_2696(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fistpl(offsetAsInt());
        }
    }

    class rip_fistps_2912 extends InstructionWithOffset {
        rip_fistps_2912(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fistps(offsetAsInt());
        }
    }

    class rip_fistpq_2928 extends InstructionWithOffset {
        rip_fistpq_2928(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fistpq(offsetAsInt());
        }
    }

    class rip_fisubl_2640 extends InstructionWithOffset {
        rip_fisubl_2640(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fisubl(offsetAsInt());
        }
    }

    class rip_fisubs_2856 extends InstructionWithOffset {
        rip_fisubs_2856(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fisubs(offsetAsInt());
        }
    }

    class rip_fisubrl_2644 extends InstructionWithOffset {
        rip_fisubrl_2644(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fisubrl(offsetAsInt());
        }
    }

    class rip_fisubrs_2860 extends InstructionWithOffset {
        rip_fisubrs_2860(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fisubrs(offsetAsInt());
        }
    }

    class rip_flds_2568 extends InstructionWithOffset {
        rip_flds_2568(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_flds(offsetAsInt());
        }
    }

    class rip_fldt_2700 extends InstructionWithOffset {
        rip_fldt_2700(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fldt(offsetAsInt());
        }
    }

    class rip_fldl_2792 extends InstructionWithOffset {
        rip_fldl_2792(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fldl(offsetAsInt());
        }
    }

    class rip_fldcw_2584 extends InstructionWithOffset {
        rip_fldcw_2584(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fldcw(offsetAsInt());
        }
    }

    class rip_fldenv_2580 extends InstructionWithOffset {
        rip_fldenv_2580(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fldenv(offsetAsInt());
        }
    }

    class rip_fmuls_2508 extends InstructionWithOffset {
        rip_fmuls_2508(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fmuls(offsetAsInt());
        }
    }

    class rip_fmull_2732 extends InstructionWithOffset {
        rip_fmull_2732(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fmull(offsetAsInt());
        }
    }

    class rip_frstor_2804 extends InstructionWithOffset {
        rip_frstor_2804(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_frstor(offsetAsInt());
        }
    }

    class rip_fsave_2808 extends InstructionWithOffset {
        rip_fsave_2808(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fsave(offsetAsInt());
        }
    }

    class rip_fsts_2572 extends InstructionWithOffset {
        rip_fsts_2572(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fsts(offsetAsInt());
        }
    }

    class rip_fstl_2796 extends InstructionWithOffset {
        rip_fstl_2796(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fstl(offsetAsInt());
        }
    }

    class rip_fstcw_2592 extends InstructionWithOffset {
        rip_fstcw_2592(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fstcw(offsetAsInt());
        }
    }

    class rip_fstenv_2588 extends InstructionWithOffset {
        rip_fstenv_2588(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fstenv(offsetAsInt());
        }
    }

    class rip_fstps_2576 extends InstructionWithOffset {
        rip_fstps_2576(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fstps(offsetAsInt());
        }
    }

    class rip_fstpt_2704 extends InstructionWithOffset {
        rip_fstpt_2704(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fstpt(offsetAsInt());
        }
    }

    class rip_fstpl_2800 extends InstructionWithOffset {
        rip_fstpl_2800(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fstpl(offsetAsInt());
        }
    }

    class rip_fstsw_2812 extends InstructionWithOffset {
        rip_fstsw_2812(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fstsw(offsetAsInt());
        }
    }

    class rip_fsubs_2520 extends InstructionWithOffset {
        rip_fsubs_2520(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fsubs(offsetAsInt());
        }
    }

    class rip_fsubl_2744 extends InstructionWithOffset {
        rip_fsubl_2744(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fsubl(offsetAsInt());
        }
    }

    class rip_fsubrs_2524 extends InstructionWithOffset {
        rip_fsubrs_2524(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fsubrs(offsetAsInt());
        }
    }

    class rip_fsubrl_2748 extends InstructionWithOffset {
        rip_fsubrl_2748(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fsubrl(offsetAsInt());
        }
    }

    class rip_fxrstor_6064 extends InstructionWithOffset {
        rip_fxrstor_6064(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fxrstor(offsetAsInt());
        }
    }

    class rip_fxsave_6060 extends InstructionWithOffset {
        rip_fxsave_6060(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_fxsave(offsetAsInt());
        }
    }

    class rip_haddpd_5818 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_haddpd_5818(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_haddpd(_destination, offsetAsInt());
        }
    }

    class rip_haddps_5862 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_haddps_5862(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_haddps(_destination, offsetAsInt());
        }
    }

    class rip_hsubpd_5827 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_hsubpd_5827(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_hsubpd(_destination, offsetAsInt());
        }
    }

    class rip_hsubps_5871 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_hsubps_5871(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_hsubps(_destination, offsetAsInt());
        }
    }

    class rip_idivb___AL_1758 extends InstructionWithOffset {
        rip_idivb___AL_1758(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_idivb___AL(offsetAsInt());
        }
    }

    class rip_idivl_1821 extends InstructionWithOffset {
        rip_idivl_1821(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_idivl(offsetAsInt());
        }
    }

    class rip_idivq_1884 extends InstructionWithOffset {
        rip_idivq_1884(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_idivq(offsetAsInt());
        }
    }

    class rip_idivw_1947 extends InstructionWithOffset {
        rip_idivw_1947(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_idivw(offsetAsInt());
        }
    }

    class rip_imul_6121 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_imul_6121(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_imul(_destination, offsetAsInt());
        }
    }

    class rip_imul_2326 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        private final byte _imm8;
        rip_imul_2326(int startPosition, int endPosition, AMD64GeneralRegister16 destination, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_imul(_destination, offsetAsInt(), _imm8);
        }
    }

    class rip_imul_2298 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        private final short _imm16;
        rip_imul_2298(int startPosition, int endPosition, AMD64GeneralRegister16 destination, short imm16, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
            _imm16 = imm16;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_imul(_destination, offsetAsInt(), _imm16);
        }
    }

    class rip_imul_6103 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_imul_6103(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_imul(_destination, offsetAsInt());
        }
    }

    class rip_imul_2308 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        private final byte _imm8;
        rip_imul_2308(int startPosition, int endPosition, AMD64GeneralRegister32 destination, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_imul(_destination, offsetAsInt(), _imm8);
        }
    }

    class rip_imul_2280 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        private final int _imm32;
        rip_imul_2280(int startPosition, int endPosition, AMD64GeneralRegister32 destination, int imm32, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
            _imm32 = imm32;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_imul(_destination, offsetAsInt(), _imm32);
        }
    }

    class rip_imul_6112 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_imul_6112(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_imul(_destination, offsetAsInt());
        }
    }

    class rip_imul_2317 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        private final byte _imm8;
        rip_imul_2317(int startPosition, int endPosition, AMD64GeneralRegister64 destination, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_imul(_destination, offsetAsInt(), _imm8);
        }
    }

    class rip_imul_2289 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        private final int _imm32;
        rip_imul_2289(int startPosition, int endPosition, AMD64GeneralRegister64 destination, int imm32, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
            _imm32 = imm32;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_imul(_destination, offsetAsInt(), _imm32);
        }
    }

    class rip_imulb___AL_1750 extends InstructionWithOffset {
        rip_imulb___AL_1750(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_imulb___AL(offsetAsInt());
        }
    }

    class rip_imull_1813 extends InstructionWithOffset {
        rip_imull_1813(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_imull(offsetAsInt());
        }
    }

    class rip_imulq_1876 extends InstructionWithOffset {
        rip_imulq_1876(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_imulq(offsetAsInt());
        }
    }

    class rip_imulw_1939 extends InstructionWithOffset {
        rip_imulw_1939(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_imulw(offsetAsInt());
        }
    }

    class rip_incb_2977 extends InstructionWithOffset {
        rip_incb_2977(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_incb(offsetAsInt());
        }
    }

    class rip_incl_2995 extends InstructionWithOffset {
        rip_incl_2995(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_incl(offsetAsInt());
        }
    }

    class rip_incq_3013 extends InstructionWithOffset {
        rip_incq_3013(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_incq(offsetAsInt());
        }
    }

    class rip_incw_3031 extends InstructionWithOffset {
        rip_incw_3031(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_incw(offsetAsInt());
        }
    }

    class rip_invlpg_3159 extends InstructionWithOffset {
        rip_invlpg_3159(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_invlpg(offsetAsInt());
        }
    }

    class jb_315 extends InstructionWithOffset {
        jb_315(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 1 | 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 1 | 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 1 | 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 1 | 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 1 | 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_jmp(offsetAsInt());
        }
    }

    class jnb_316 extends InstructionWithOffset {
        jnb_316(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 1 | 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 1 | 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 1 | 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 1 | 4);
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

    class jno_314 extends InstructionWithOffset {
        jno_314(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 1 | 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            final int labelSize = labelSize();
            if (labelSize == 1) {
                jno(offsetAsByte());
            } else if (labelSize == 4) {
                jno(offsetAsInt());
            } else {
                throw new AssemblyException("Unexpected label width: " + labelSize);
            }
        }
    }

    class jnp_2341 extends InstructionWithOffset {
        jnp_2341(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 1 | 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            final int labelSize = labelSize();
            if (labelSize == 1) {
                jnp(offsetAsByte());
            } else if (labelSize == 4) {
                jnp(offsetAsInt());
            } else {
                throw new AssemblyException("Unexpected label width: " + labelSize);
            }
        }
    }

    class jns_2339 extends InstructionWithOffset {
        jns_2339(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 1 | 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            final int labelSize = labelSize();
            if (labelSize == 1) {
                jns(offsetAsByte());
            } else if (labelSize == 4) {
                jns(offsetAsInt());
            } else {
                throw new AssemblyException("Unexpected label width: " + labelSize);
            }
        }
    }

    class jnz_318 extends InstructionWithOffset {
        jnz_318(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 1 | 4);
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

    class jo_313 extends InstructionWithOffset {
        jo_313(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 1 | 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            final int labelSize = labelSize();
            if (labelSize == 1) {
                jo(offsetAsByte());
            } else if (labelSize == 4) {
                jo(offsetAsInt());
            } else {
                throw new AssemblyException("Unexpected label width: " + labelSize);
            }
        }
    }

    class jp_2340 extends InstructionWithOffset {
        jp_2340(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 1 | 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            final int labelSize = labelSize();
            if (labelSize == 1) {
                jp(offsetAsByte());
            } else if (labelSize == 4) {
                jp(offsetAsInt());
            } else {
                throw new AssemblyException("Unexpected label width: " + labelSize);
            }
        }
    }

    class jrcxz_1716 extends InstructionWithOffset {
        jrcxz_1716(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 1);
        }
        @Override
        protected void assemble() throws AssemblyException {
            jrcxz(offsetAsByte());
        }
    }

    class js_2338 extends InstructionWithOffset {
        js_2338(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 1 | 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            final int labelSize = labelSize();
            if (labelSize == 1) {
                js(offsetAsByte());
            } else if (labelSize == 4) {
                js(offsetAsInt());
            } else {
                throw new AssemblyException("Unexpected label width: " + labelSize);
            }
        }
    }

    class jz_317 extends InstructionWithOffset {
        jz_317(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 1 | 4);
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

    class rip_lar_3214 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_lar_3214(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_lar(_destination, offsetAsInt());
        }
    }

    class rip_lar_3196 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_lar_3196(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_lar(_destination, offsetAsInt());
        }
    }

    class rip_lar_3205 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_lar_3205(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_lar(_destination, offsetAsInt());
        }
    }

    class rip_lddqu_4836 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_lddqu_4836(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_lddqu(_destination, offsetAsInt());
        }
    }

    class rip_ldmxcsr_6068 extends InstructionWithOffset {
        rip_ldmxcsr_6068(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_ldmxcsr(offsetAsInt());
        }
    }

    class rip_lea_2442 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_lea_2442(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_lea(_destination, offsetAsInt());
        }
    }

    class rip_lea_2426 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_lea_2426(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_lea(_destination, offsetAsInt());
        }
    }

    class rip_lea_2434 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_lea_2434(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_lea(_destination, offsetAsInt());
        }
    }

    class rip_lgdt_3143 extends InstructionWithOffset {
        rip_lgdt_3143(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_lgdt(offsetAsInt());
        }
    }

    class rip_lidt_3147 extends InstructionWithOffset {
        rip_lidt_3147(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_lidt(offsetAsInt());
        }
    }

    class rip_lldt_3085 extends InstructionWithOffset {
        rip_lldt_3085(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_lldt(offsetAsInt());
        }
    }

    class rip_lmsw_3155 extends InstructionWithOffset {
        rip_lmsw_3155(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_lmsw(offsetAsInt());
        }
    }

    class loop_1715 extends InstructionWithOffset {
        loop_1715(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 1);
        }
        @Override
        protected void assemble() throws AssemblyException {
            loop(offsetAsByte());
        }
    }

    class loope_1714 extends InstructionWithOffset {
        loope_1714(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 1);
        }
        @Override
        protected void assemble() throws AssemblyException {
            loope(offsetAsByte());
        }
    }

    class loopne_1713 extends InstructionWithOffset {
        loopne_1713(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 1);
        }
        @Override
        protected void assemble() throws AssemblyException {
            loopne(offsetAsByte());
        }
    }

    class rip_lsl_3241 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_lsl_3241(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_lsl(_destination, offsetAsInt());
        }
    }

    class rip_lsl_3223 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_lsl_3223(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_lsl(_destination, offsetAsInt());
        }
    }

    class rip_lsl_3232 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_lsl_3232(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_lsl(_destination, offsetAsInt());
        }
    }

    class rip_ltr_3089 extends InstructionWithOffset {
        rip_ltr_3089(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_ltr(offsetAsInt());
        }
    }

    class rip_maxpd_5495 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_maxpd_5495(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_maxpd(_destination, offsetAsInt());
        }
    }

    class rip_maxps_5423 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_maxps_5423(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_maxps(_destination, offsetAsInt());
        }
    }

    class rip_maxsd_5558 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_maxsd_5558(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_maxsd(_destination, offsetAsInt());
        }
    }

    class rip_maxss_5630 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_maxss_5630(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_maxss(_destination, offsetAsInt());
        }
    }

    class rip_minpd_5477 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_minpd_5477(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_minpd(_destination, offsetAsInt());
        }
    }

    class rip_minps_5405 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_minps_5405(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_minps(_destination, offsetAsInt());
        }
    }

    class rip_minsd_5540 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_minsd_5540(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_minsd(_destination, offsetAsInt());
        }
    }

    class rip_minss_5612 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_minss_5612(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_minss(_destination, offsetAsInt());
        }
    }

    class rip_mov_2409 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_mov_2409(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_mov(_destination, offsetAsInt());
        }
    }

    class rip_mov_2393 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_mov_2393(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_mov(_destination, offsetAsInt());
        }
    }

    class rip_mov_2401 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_mov_2401(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_mov(_destination, offsetAsInt());
        }
    }

    class rip_mov_2385 extends InstructionWithOffset {
        private final AMD64GeneralRegister8 _destination;
        rip_mov_2385(int startPosition, int endPosition, AMD64GeneralRegister8 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_mov(_destination, offsetAsInt());
        }
    }

    class rip_mov_2450 extends InstructionWithOffset {
        private final SegmentRegister _destination;
        rip_mov_2450(int startPosition, int endPosition, SegmentRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_mov(_destination, offsetAsInt());
        }
    }

    class rip_movb_1175 extends InstructionWithOffset {
        private final byte _imm8;
        rip_movb_1175(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movb(offsetAsInt(), _imm8);
        }
    }

    class rip_mov_2376 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _source;
        rip_mov_2376(int startPosition, int endPosition, AMD64GeneralRegister16 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_mov(offsetAsInt(), _source);
        }
    }

    class rip_mov_2358 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _source;
        rip_mov_2358(int startPosition, int endPosition, AMD64GeneralRegister32 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_mov(offsetAsInt(), _source);
        }
    }

    class rip_mov_2367 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _source;
        rip_mov_2367(int startPosition, int endPosition, AMD64GeneralRegister64 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_mov(offsetAsInt(), _source);
        }
    }

    class rip_mov_2349 extends InstructionWithOffset {
        private final AMD64GeneralRegister8 _source;
        rip_mov_2349(int startPosition, int endPosition, AMD64GeneralRegister8 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_mov(offsetAsInt(), _source);
        }
    }

    class rip_mov_2417 extends InstructionWithOffset {
        private final SegmentRegister _source;
        rip_mov_2417(int startPosition, int endPosition, SegmentRegister source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_mov(offsetAsInt(), _source);
        }
    }

    class rip_movl_1184 extends InstructionWithOffset {
        private final int _imm32;
        rip_movl_1184(int startPosition, int endPosition, int imm32, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm32 = imm32;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movl(offsetAsInt(), _imm32);
        }
    }

    class rip_movq_1193 extends InstructionWithOffset {
        private final int _imm32;
        rip_movq_1193(int startPosition, int endPosition, int imm32, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm32 = imm32;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movq(offsetAsInt(), _imm32);
        }
    }

    class rip_movw_1202 extends InstructionWithOffset {
        private final short _imm16;
        rip_movw_1202(int startPosition, int endPosition, short imm16, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm16 = imm16;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movw(offsetAsInt(), _imm16);
        }
    }

    class m_mov_AL_901 extends InstructionWithAddress {
        m_mov_AL_901(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label);
        }
        @Override
        protected void assemble() throws AssemblyException {
            m_mov_AL(addressAsLong());
        }
    }

    class m_mov_EAX_902 extends InstructionWithAddress {
        m_mov_EAX_902(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label);
        }
        @Override
        protected void assemble() throws AssemblyException {
            m_mov_EAX(addressAsLong());
        }
    }

    class m_mov_RAX_903 extends InstructionWithAddress {
        m_mov_RAX_903(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label);
        }
        @Override
        protected void assemble() throws AssemblyException {
            m_mov_RAX(addressAsLong());
        }
    }

    class m_mov_AX_904 extends InstructionWithAddress {
        m_mov_AX_904(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label);
        }
        @Override
        protected void assemble() throws AssemblyException {
            m_mov_AX(addressAsLong());
        }
    }

    class m_mov___AL_905 extends InstructionWithAddress {
        m_mov___AL_905(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label);
        }
        @Override
        protected void assemble() throws AssemblyException {
            m_mov___AL(addressAsLong());
        }
    }

    class m_mov___EAX_906 extends InstructionWithAddress {
        m_mov___EAX_906(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label);
        }
        @Override
        protected void assemble() throws AssemblyException {
            m_mov___EAX(addressAsLong());
        }
    }

    class m_mov___RAX_907 extends InstructionWithAddress {
        m_mov___RAX_907(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label);
        }
        @Override
        protected void assemble() throws AssemblyException {
            m_mov___RAX(addressAsLong());
        }
    }

    class m_mov___AX_908 extends InstructionWithAddress {
        m_mov___AX_908(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label);
        }
        @Override
        protected void assemble() throws AssemblyException {
            m_mov___AX(addressAsLong());
        }
    }

    class rip_movapd_4966 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_movapd_4966(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movapd(_destination, offsetAsInt());
        }
    }

    class rip_movapd_4975 extends InstructionWithOffset {
        private final AMD64XMMRegister _source;
        rip_movapd_4975(int startPosition, int endPosition, AMD64XMMRegister source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movapd(offsetAsInt(), _source);
        }
    }

    class rip_movaps_4896 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_movaps_4896(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movaps(_destination, offsetAsInt());
        }
    }

    class rip_movaps_4905 extends InstructionWithOffset {
        private final AMD64XMMRegister _source;
        rip_movaps_4905(int startPosition, int endPosition, AMD64XMMRegister source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movaps(offsetAsInt(), _source);
        }
    }

    class rip_movdl_5756 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_movdl_5756(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movdl(_destination, offsetAsInt());
        }
    }

    class rip_movdq_5765 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_movdq_5765(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movdq(_destination, offsetAsInt());
        }
    }

    class rip_movdl_5675 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_movdl_5675(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movdl(_destination, offsetAsInt());
        }
    }

    class rip_movdq_5684 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_movdq_5684(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movdq(_destination, offsetAsInt());
        }
    }

    class rip_movdl_5836 extends InstructionWithOffset {
        private final AMD64XMMRegister _source;
        rip_movdl_5836(int startPosition, int endPosition, AMD64XMMRegister source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movdl(offsetAsInt(), _source);
        }
    }

    class rip_movdq_5845 extends InstructionWithOffset {
        private final AMD64XMMRegister _source;
        rip_movdq_5845(int startPosition, int endPosition, AMD64XMMRegister source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movdq(offsetAsInt(), _source);
        }
    }

    class rip_movdl_5792 extends InstructionWithOffset {
        private final MMXRegister _source;
        rip_movdl_5792(int startPosition, int endPosition, MMXRegister source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movdl(offsetAsInt(), _source);
        }
    }

    class rip_movdq_5801 extends InstructionWithOffset {
        private final MMXRegister _source;
        rip_movdq_5801(int startPosition, int endPosition, MMXRegister source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movdq(offsetAsInt(), _source);
        }
    }

    class rip_movddup_3390 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_movddup_3390(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movddup(_destination, offsetAsInt());
        }
    }

    class rip_movdqa_5774 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_movdqa_5774(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movdqa(_destination, offsetAsInt());
        }
    }

    class rip_movdqa_5854 extends InstructionWithOffset {
        private final AMD64XMMRegister _source;
        rip_movdqa_5854(int startPosition, int endPosition, AMD64XMMRegister source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movdqa(offsetAsInt(), _source);
        }
    }

    class rip_movdqu_5783 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_movdqu_5783(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movdqu(_destination, offsetAsInt());
        }
    }

    class rip_movdqu_5888 extends InstructionWithOffset {
        private final AMD64XMMRegister _source;
        rip_movdqu_5888(int startPosition, int endPosition, AMD64XMMRegister source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movdqu(offsetAsInt(), _source);
        }
    }

    class rip_movhpd_3357 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_movhpd_3357(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movhpd(_destination, offsetAsInt());
        }
    }

    class rip_movhpd_3365 extends InstructionWithOffset {
        private final AMD64XMMRegister _source;
        rip_movhpd_3365(int startPosition, int endPosition, AMD64XMMRegister source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movhpd(offsetAsInt(), _source);
        }
    }

    class rip_movhps_3298 extends InstructionWithOffset {
        private final AMD64XMMRegister _source;
        rip_movhps_3298(int startPosition, int endPosition, AMD64XMMRegister source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movhps(offsetAsInt(), _source);
        }
    }

    class rip_movlpd_3323 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_movlpd_3323(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movlpd(_destination, offsetAsInt());
        }
    }

    class rip_movlpd_3331 extends InstructionWithOffset {
        private final AMD64XMMRegister _source;
        rip_movlpd_3331(int startPosition, int endPosition, AMD64XMMRegister source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movlpd(offsetAsInt(), _source);
        }
    }

    class rip_movlps_3271 extends InstructionWithOffset {
        private final AMD64XMMRegister _source;
        rip_movlps_3271(int startPosition, int endPosition, AMD64XMMRegister source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movlps(offsetAsInt(), _source);
        }
    }

    class rip_movnti_4374 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _source;
        rip_movnti_4374(int startPosition, int endPosition, AMD64GeneralRegister32 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movnti(offsetAsInt(), _source);
        }
    }

    class rip_movnti_4382 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _source;
        rip_movnti_4382(int startPosition, int endPosition, AMD64GeneralRegister64 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movnti(offsetAsInt(), _source);
        }
    }

    class rip_movntpd_4992 extends InstructionWithOffset {
        private final AMD64XMMRegister _source;
        rip_movntpd_4992(int startPosition, int endPosition, AMD64XMMRegister source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movntpd(offsetAsInt(), _source);
        }
    }

    class rip_movntps_4922 extends InstructionWithOffset {
        private final AMD64XMMRegister _source;
        rip_movntps_4922(int startPosition, int endPosition, AMD64XMMRegister source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movntps(offsetAsInt(), _source);
        }
    }

    class rip_movntq_4629 extends InstructionWithOffset {
        private final MMXRegister _source;
        rip_movntq_4629(int startPosition, int endPosition, MMXRegister source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movntq(offsetAsInt(), _source);
        }
    }

    class rip_movq_5880 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_movq_5880(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movq(_destination, offsetAsInt());
        }
    }

    class rip_movq_5693 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_movq_5693(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movq(_destination, offsetAsInt());
        }
    }

    class rip_movq_4563 extends InstructionWithOffset {
        private final AMD64XMMRegister _source;
        rip_movq_4563(int startPosition, int endPosition, AMD64XMMRegister source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movq(offsetAsInt(), _source);
        }
    }

    class rip_movq_5810 extends InstructionWithOffset {
        private final MMXRegister _source;
        rip_movq_5810(int startPosition, int endPosition, MMXRegister source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movq(offsetAsInt(), _source);
        }
    }

    class rip_movsd_3373 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_movsd_3373(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movsd(_destination, offsetAsInt());
        }
    }

    class rip_movsd_3382 extends InstructionWithOffset {
        private final AMD64XMMRegister _source;
        rip_movsd_3382(int startPosition, int endPosition, AMD64XMMRegister source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movsd(offsetAsInt(), _source);
        }
    }

    class rip_movshdup_3425 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_movshdup_3425(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movshdup(_destination, offsetAsInt());
        }
    }

    class rip_movsldup_3416 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_movsldup_3416(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movsldup(_destination, offsetAsInt());
        }
    }

    class rip_movss_3399 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_movss_3399(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movss(_destination, offsetAsInt());
        }
    }

    class rip_movss_3408 extends InstructionWithOffset {
        private final AMD64XMMRegister _source;
        rip_movss_3408(int startPosition, int endPosition, AMD64XMMRegister source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movss(offsetAsInt(), _source);
        }
    }

    class rip_movsxb_6273 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_movsxb_6273(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movsxb(_destination, offsetAsInt());
        }
    }

    class rip_movsxb_6255 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_movsxb_6255(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movsxb(_destination, offsetAsInt());
        }
    }

    class rip_movsxb_6264 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_movsxb_6264(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movsxw(_destination, offsetAsInt());
        }
    }

    class rip_movsxw_6291 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_movsxw_6291(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movsxw(_destination, offsetAsInt());
        }
    }

    class rip_movupd_3306 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_movupd_3306(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movupd(_destination, offsetAsInt());
        }
    }

    class rip_movupd_3315 extends InstructionWithOffset {
        private final AMD64XMMRegister _source;
        rip_movupd_3315(int startPosition, int endPosition, AMD64XMMRegister source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movupd(offsetAsInt(), _source);
        }
    }

    class rip_movups_3253 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_movups_3253(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movups(_destination, offsetAsInt());
        }
    }

    class rip_movups_3262 extends InstructionWithOffset {
        private final AMD64XMMRegister _source;
        rip_movups_3262(int startPosition, int endPosition, AMD64XMMRegister source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movups(offsetAsInt(), _source);
        }
    }

    class rip_movzxb_4302 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_movzxb_4302(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movzxb(_destination, offsetAsInt());
        }
    }

    class rip_movzxb_4284 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_movzxb_4284(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movzxb(_destination, offsetAsInt());
        }
    }

    class rip_movzxb_4293 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_movzxb_4293(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movzxb(_destination, offsetAsInt());
        }
    }

    class rip_movzxd_303 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_movzxd_303(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movzxw(_destination, offsetAsInt());
        }
    }

    class rip_movzxw_4320 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_movzxw_4320(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_movzxw(_destination, offsetAsInt());
        }
    }

    class rip_mulb___AL_1746 extends InstructionWithOffset {
        rip_mulb___AL_1746(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_mulb___AL(offsetAsInt());
        }
    }

    class rip_mull_1809 extends InstructionWithOffset {
        rip_mull_1809(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_mull(offsetAsInt());
        }
    }

    class rip_mulq_1872 extends InstructionWithOffset {
        rip_mulq_1872(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_mulq(offsetAsInt());
        }
    }

    class rip_mulw_1935 extends InstructionWithOffset {
        rip_mulw_1935(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_mulw(offsetAsInt());
        }
    }

    class rip_mulpd_5441 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_mulpd_5441(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_mulpd(_destination, offsetAsInt());
        }
    }

    class rip_mulps_5369 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_mulps_5369(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_mulps(_destination, offsetAsInt());
        }
    }

    class rip_mulsd_5513 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_mulsd_5513(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_mulss(_destination, offsetAsInt());
        }
    }

    class rip_mvntdq_4700 extends InstructionWithOffset {
        private final AMD64XMMRegister _source;
        rip_mvntdq_4700(int startPosition, int endPosition, AMD64XMMRegister source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_mvntdq(offsetAsInt(), _source);
        }
    }

    class rip_negb_1742 extends InstructionWithOffset {
        rip_negb_1742(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_negb(offsetAsInt());
        }
    }

    class rip_negl_1805 extends InstructionWithOffset {
        rip_negl_1805(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_negl(offsetAsInt());
        }
    }

    class rip_negq_1868 extends InstructionWithOffset {
        rip_negq_1868(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_negq(offsetAsInt());
        }
    }

    class rip_negw_1931 extends InstructionWithOffset {
        rip_negw_1931(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_negw(offsetAsInt());
        }
    }

    class rip_notb_1738 extends InstructionWithOffset {
        rip_notb_1738(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_notb(offsetAsInt());
        }
    }

    class rip_notl_1801 extends InstructionWithOffset {
        rip_notl_1801(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_notl(offsetAsInt());
        }
    }

    class rip_notq_1864 extends InstructionWithOffset {
        rip_notq_1864(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_notq(offsetAsInt());
        }
    }

    class rip_notw_1927 extends InstructionWithOffset {
        rip_notw_1927(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_notw(offsetAsInt());
        }
    }

    class rip_or_2046 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_or_2046(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_or(_destination, offsetAsInt());
        }
    }

    class rip_or_2030 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_or_2030(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_or(_destination, offsetAsInt());
        }
    }

    class rip_or_2022 extends InstructionWithOffset {
        private final AMD64GeneralRegister8 _destination;
        rip_or_2022(int startPosition, int endPosition, AMD64GeneralRegister8 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_or(_destination, offsetAsInt());
        }
    }

    class rip_orb_328 extends InstructionWithOffset {
        private final byte _imm8;
        rip_orb_328(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_orb(offsetAsInt(), _imm8);
        }
    }

    class rip_orl_616 extends InstructionWithOffset {
        private final byte _imm8;
        rip_orl_616(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_orl(offsetAsInt(), _imm8);
        }
    }

    class rip_orq_688 extends InstructionWithOffset {
        private final byte _imm8;
        rip_orq_688(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_orq(offsetAsInt(), _imm8);
        }
    }

    class rip_orw_760 extends InstructionWithOffset {
        private final byte _imm8;
        rip_orw_760(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_orw(offsetAsInt(), _imm8);
        }
    }

    class rip_or_2013 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _source;
        rip_or_2013(int startPosition, int endPosition, AMD64GeneralRegister16 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_or(offsetAsInt(), _source);
        }
    }

    class rip_or_1995 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _source;
        rip_or_1995(int startPosition, int endPosition, AMD64GeneralRegister32 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_or(offsetAsInt(), _source);
        }
    }

    class rip_or_2004 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _source;
        rip_or_2004(int startPosition, int endPosition, AMD64GeneralRegister64 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_or(offsetAsInt(), _source);
        }
    }

    class rip_or_1986 extends InstructionWithOffset {
        private final AMD64GeneralRegister8 _source;
        rip_or_1986(int startPosition, int endPosition, AMD64GeneralRegister8 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_or(offsetAsInt(), _source);
        }
    }

    class rip_orl_400 extends InstructionWithOffset {
        private final int _imm32;
        rip_orl_400(int startPosition, int endPosition, int imm32, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm32 = imm32;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_orl(offsetAsInt(), _imm32);
        }
    }

    class rip_orq_472 extends InstructionWithOffset {
        private final int _imm32;
        rip_orq_472(int startPosition, int endPosition, int imm32, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm32 = imm32;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_orq(offsetAsInt(), _imm32);
        }
    }

    class rip_orw_544 extends InstructionWithOffset {
        private final short _imm16;
        rip_orw_544(int startPosition, int endPosition, short imm16, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm16 = imm16;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_orw(offsetAsInt(), _imm16);
        }
    }

    class rip_orpd_3750 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_orpd_3750(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_orpd(_destination, offsetAsInt());
        }
    }

    class rip_orps_3704 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_orps_3704(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_orps(_destination, offsetAsInt());
        }
    }

    class rip_packssdw_5729 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_packssdw_5729(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_packssdw(_destination, offsetAsInt());
        }
    }

    class rip_packssdw_5666 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_packssdw_5666(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_packssdw(_destination, offsetAsInt());
        }
    }

    class rip_packsswb_3903 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_packsswb_3903(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_packsswb(_destination, offsetAsInt());
        }
    }

    class rip_packsswb_3831 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_packsswb_3831(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_packsswb(_destination, offsetAsInt());
        }
    }

    class rip_packuswb_3939 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_packuswb_3939(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_packuswb(_destination, offsetAsInt());
        }
    }

    class rip_packuswb_3867 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_packuswb_3867(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_packuswb(_destination, offsetAsInt());
        }
    }

    class rip_paddb_6689 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_paddb_6689(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_paddb(_destination, offsetAsInt());
        }
    }

    class rip_paddb_6626 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_paddb_6626(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_paddb(_destination, offsetAsInt());
        }
    }

    class rip_paddd_6707 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_paddd_6707(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_paddd(_destination, offsetAsInt());
        }
    }

    class rip_paddd_6644 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_paddd_6644(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_paddd(_destination, offsetAsInt());
        }
    }

    class rip_paddq_4545 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_paddq_4545(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_paddq(_destination, offsetAsInt());
        }
    }

    class rip_paddq_4490 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_paddq_4490(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_paddq(_destination, offsetAsInt());
        }
    }

    class rip_paddsb_6554 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_paddsb_6554(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_paddsb(_destination, offsetAsInt());
        }
    }

    class rip_paddsb_6482 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_paddsb_6482(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_paddsb(_destination, offsetAsInt());
        }
    }

    class rip_paddsw_6563 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_paddsw_6563(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_paddsw(_destination, offsetAsInt());
        }
    }

    class rip_paddsw_6491 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_paddsw_6491(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_paddsw(_destination, offsetAsInt());
        }
    }

    class rip_paddusb_6410 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_paddusb_6410(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_paddusb(_destination, offsetAsInt());
        }
    }

    class rip_paddusb_6338 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_paddusb_6338(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_paddusb(_destination, offsetAsInt());
        }
    }

    class rip_paddusw_6419 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_paddusw_6419(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_paddusw(_destination, offsetAsInt());
        }
    }

    class rip_paddusw_6347 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_paddusw_6347(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_paddusw(_destination, offsetAsInt());
        }
    }

    class rip_paddw_6698 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_paddw_6698(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_paddw(_destination, offsetAsInt());
        }
    }

    class rip_paddw_6635 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_paddw_6635(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_paddw(_destination, offsetAsInt());
        }
    }

    class rip_pand_6401 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_pand_6401(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pand(_destination, offsetAsInt());
        }
    }

    class rip_pand_6329 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_pand_6329(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pand(_destination, offsetAsInt());
        }
    }

    class rip_pandn_6437 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_pandn_6437(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pandn(_destination, offsetAsInt());
        }
    }

    class rip_pandn_6365 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_pandn_6365(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pandn(_destination, offsetAsInt());
        }
    }

    class rip_pavgb_4637 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_pavgb_4637(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pavgb(_destination, offsetAsInt());
        }
    }

    class rip_pavgb_4575 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_pavgb_4575(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pavgb(_destination, offsetAsInt());
        }
    }

    class rip_pavgw_4664 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_pavgw_4664(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pavgw(_destination, offsetAsInt());
        }
    }

    class rip_pavgw_4602 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_pavgw_4602(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pavgw(_destination, offsetAsInt());
        }
    }

    class rip_pcmpeqb_4012 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_pcmpeqb_4012(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pcmpeqb(_destination, offsetAsInt());
        }
    }

    class rip_pcmpeqb_3965 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_pcmpeqb_3965(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pcmpeqb(_destination, offsetAsInt());
        }
    }

    class rip_pcmpeqd_4030 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_pcmpeqd_4030(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pcmpeqd(_destination, offsetAsInt());
        }
    }

    class rip_pcmpeqd_3983 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_pcmpeqd_3983(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pcmpeqd(_destination, offsetAsInt());
        }
    }

    class rip_pcmpeqw_4021 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_pcmpeqw_4021(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pcmpeqw(_destination, offsetAsInt());
        }
    }

    class rip_pcmpeqw_3974 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_pcmpeqw_3974(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pcmpeqw(_destination, offsetAsInt());
        }
    }

    class rip_pcmpgtb_3912 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_pcmpgtb_3912(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pcmpgtb(_destination, offsetAsInt());
        }
    }

    class rip_pcmpgtb_3840 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_pcmpgtb_3840(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pcmpgtb(_destination, offsetAsInt());
        }
    }

    class rip_pcmpgtd_3930 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_pcmpgtd_3930(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pcmpgtd(_destination, offsetAsInt());
        }
    }

    class rip_pcmpgtd_3858 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_pcmpgtd_3858(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pcmpgtd(_destination, offsetAsInt());
        }
    }

    class rip_pcmpgtw_3921 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_pcmpgtw_3921(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pcmpgtw(_destination, offsetAsInt());
        }
    }

    class rip_pcmpgtw_3849 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_pcmpgtw_3849(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pcmpgtw(_destination, offsetAsInt());
        }
    }

    class rip_pinsrw_4426 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        private final byte _imm8;
        rip_pinsrw_4426(int startPosition, int endPosition, AMD64XMMRegister destination, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pinsrw(_destination, offsetAsInt(), _imm8);
        }
    }

    class rip_pinsrw_4390 extends InstructionWithOffset {
        private final MMXRegister _destination;
        private final byte _imm8;
        rip_pinsrw_4390(int startPosition, int endPosition, MMXRegister destination, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pinsrw(_destination, offsetAsInt(), _imm8);
        }
    }

    class rip_pmaddwd_4817 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_pmaddwd_4817(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pmaddwd(_destination, offsetAsInt());
        }
    }

    class rip_pmaddwd_4762 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_pmaddwd_4762(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pmaddwd(_destination, offsetAsInt());
        }
    }

    class rip_pmaxsw_6572 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_pmaxsw_6572(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pmaxsw(_destination, offsetAsInt());
        }
    }

    class rip_pmaxsw_6500 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_pmaxsw_6500(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pmaxsw(_destination, offsetAsInt());
        }
    }

    class rip_pmaxub_6428 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_pmaxub_6428(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pmaxub(_destination, offsetAsInt());
        }
    }

    class rip_pmaxub_6356 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_pmaxub_6356(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pmaxub(_destination, offsetAsInt());
        }
    }

    class rip_pminsw_6536 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_pminsw_6536(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pminsw(_destination, offsetAsInt());
        }
    }

    class rip_pminsw_6464 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_pminsw_6464(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pminsw(_destination, offsetAsInt());
        }
    }

    class rip_pminub_6392 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_pminub_6392(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pminub(_destination, offsetAsInt());
        }
    }

    class rip_pminub_6320 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_pminub_6320(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pminub(_destination, offsetAsInt());
        }
    }

    class rip_pmulhuw_4673 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_pmulhuw_4673(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pmulhuw(_destination, offsetAsInt());
        }
    }

    class rip_pmulhuw_4611 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_pmulhuw_4611(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pmulhuw(_destination, offsetAsInt());
        }
    }

    class rip_pmulhw_4682 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_pmulhw_4682(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pmulhw(_destination, offsetAsInt());
        }
    }

    class rip_pmulhw_4620 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_pmulhw_4620(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pmulhw(_destination, offsetAsInt());
        }
    }

    class rip_pmullw_4554 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_pmullw_4554(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pmullw(_destination, offsetAsInt());
        }
    }

    class rip_pmullw_4499 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_pmullw_4499(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pmullw(_destination, offsetAsInt());
        }
    }

    class rip_pmuludq_4808 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_pmuludq_4808(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pmuludq(_destination, offsetAsInt());
        }
    }

    class rip_pmuludq_4753 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_pmuludq_4753(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pmuludq(_destination, offsetAsInt());
        }
    }

    class rip_pop_2459 extends InstructionWithOffset {
        rip_pop_2459(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pop(offsetAsInt());
        }
    }

    class rip_por_6545 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_por_6545(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_por(_destination, offsetAsInt());
        }
    }

    class rip_por_6473 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_por_6473(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_por(_destination, offsetAsInt());
        }
    }

    class rip_prefetch_4847 extends InstructionWithOffset {
        rip_prefetch_4847(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_prefetch(offsetAsInt());
        }
    }

    class rip_prefetchnta_4864 extends InstructionWithOffset {
        rip_prefetchnta_4864(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_prefetchnta(offsetAsInt());
        }
    }

    class rip_prefetcht0_4868 extends InstructionWithOffset {
        rip_prefetcht0_4868(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_prefetcht0(offsetAsInt());
        }
    }

    class rip_prefetcht1_4872 extends InstructionWithOffset {
        rip_prefetcht1_4872(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_prefetcht1(offsetAsInt());
        }
    }

    class rip_prefetcht2_4876 extends InstructionWithOffset {
        rip_prefetcht2_4876(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_prefetcht2(offsetAsInt());
        }
    }

    class rip_prefetchw_4851 extends InstructionWithOffset {
        rip_prefetchw_4851(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_prefetchw(offsetAsInt());
        }
    }

    class rip_psadbw_4826 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_psadbw_4826(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psadbw(_destination, offsetAsInt());
        }
    }

    class rip_psadbw_4771 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_psadbw_4771(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psadbw(_destination, offsetAsInt());
        }
    }

    class rip_pshufd_3993 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        private final byte _imm8;
        rip_pshufd_3993(int startPosition, int endPosition, AMD64XMMRegister destination, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pshufd(_destination, offsetAsInt(), _imm8);
        }
    }

    class rip_pshufhw_4048 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        private final byte _imm8;
        rip_pshufhw_4048(int startPosition, int endPosition, AMD64XMMRegister destination, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pshufhw(_destination, offsetAsInt(), _imm8);
        }
    }

    class rip_pshuflw_4039 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        private final byte _imm8;
        rip_pshuflw_4039(int startPosition, int endPosition, AMD64XMMRegister destination, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pshuflw(_destination, offsetAsInt(), _imm8);
        }
    }

    class rip_pshufw_3948 extends InstructionWithOffset {
        private final MMXRegister _destination;
        private final byte _imm8;
        rip_pshufw_3948(int startPosition, int endPosition, MMXRegister destination, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pshufw(_destination, offsetAsInt(), _imm8);
        }
    }

    class rip_pslld_4790 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_pslld_4790(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pslld(_destination, offsetAsInt());
        }
    }

    class rip_pslld_4735 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_pslld_4735(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pslld(_destination, offsetAsInt());
        }
    }

    class rip_psllq_4799 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_psllq_4799(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psllq(_destination, offsetAsInt());
        }
    }

    class rip_psllq_4744 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_psllq_4744(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psllq(_destination, offsetAsInt());
        }
    }

    class rip_psllw_4781 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_psllw_4781(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psllw(_destination, offsetAsInt());
        }
    }

    class rip_psllw_4726 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_psllw_4726(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psllw(_destination, offsetAsInt());
        }
    }

    class rip_psrad_4655 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_psrad_4655(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psrad(_destination, offsetAsInt());
        }
    }

    class rip_psrad_4593 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_psrad_4593(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psrad(_destination, offsetAsInt());
        }
    }

    class rip_psraw_4646 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_psraw_4646(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psraw(_destination, offsetAsInt());
        }
    }

    class rip_psraw_4584 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_psraw_4584(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psraw(_destination, offsetAsInt());
        }
    }

    class rip_psrld_4527 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_psrld_4527(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psrld(_destination, offsetAsInt());
        }
    }

    class rip_psrld_4472 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_psrld_4472(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psrld(_destination, offsetAsInt());
        }
    }

    class rip_psrlq_4536 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_psrlq_4536(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psrlq(_destination, offsetAsInt());
        }
    }

    class rip_psrlq_4481 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_psrlq_4481(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psrlq(_destination, offsetAsInt());
        }
    }

    class rip_psrlw_4518 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_psrlw_4518(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psrlw(_destination, offsetAsInt());
        }
    }

    class rip_psrlw_4463 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_psrlw_4463(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psrlw(_destination, offsetAsInt());
        }
    }

    class rip_psubb_6653 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_psubb_6653(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psubb(_destination, offsetAsInt());
        }
    }

    class rip_psubb_6590 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_psubb_6590(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psubb(_destination, offsetAsInt());
        }
    }

    class rip_psubd_6671 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_psubd_6671(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psubd(_destination, offsetAsInt());
        }
    }

    class rip_psubd_6608 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_psubd_6608(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psubd(_destination, offsetAsInt());
        }
    }

    class rip_psubq_6680 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_psubq_6680(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psubq(_destination, offsetAsInt());
        }
    }

    class rip_psubq_6617 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_psubq_6617(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psubq(_destination, offsetAsInt());
        }
    }

    class rip_psubsb_6518 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_psubsb_6518(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psubsb(_destination, offsetAsInt());
        }
    }

    class rip_psubsb_6446 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_psubsb_6446(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psubsb(_destination, offsetAsInt());
        }
    }

    class rip_psubsw_6527 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_psubsw_6527(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psubsw(_destination, offsetAsInt());
        }
    }

    class rip_psubsw_6455 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_psubsw_6455(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psubsw(_destination, offsetAsInt());
        }
    }

    class rip_psubusb_6374 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_psubusb_6374(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psubusb(_destination, offsetAsInt());
        }
    }

    class rip_psubusb_6302 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_psubusb_6302(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psubusb(_destination, offsetAsInt());
        }
    }

    class rip_psubusw_6383 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_psubusw_6383(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psubusw(_destination, offsetAsInt());
        }
    }

    class rip_psubusw_6311 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_psubusw_6311(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psubusw(_destination, offsetAsInt());
        }
    }

    class rip_psubw_6662 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_psubw_6662(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psubw(_destination, offsetAsInt());
        }
    }

    class rip_psubw_6599 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_psubw_6599(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_psubw(_destination, offsetAsInt());
        }
    }

    class rip_punpckhbw_5702 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_punpckhbw_5702(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_punpckhbw(_destination, offsetAsInt());
        }
    }

    class rip_punpckhbw_5639 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_punpckhbw_5639(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_punpckhbw(_destination, offsetAsInt());
        }
    }

    class rip_punpckhdq_5720 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_punpckhdq_5720(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_punpckhdq(_destination, offsetAsInt());
        }
    }

    class rip_punpckhdq_5657 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_punpckhdq_5657(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_punpckhdq(_destination, offsetAsInt());
        }
    }

    class rip_punpckhqdq_5747 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_punpckhqdq_5747(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_punpckhqdq(_destination, offsetAsInt());
        }
    }

    class rip_punpckhwd_5711 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_punpckhwd_5711(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_punpckhwd(_destination, offsetAsInt());
        }
    }

    class rip_punpckhwd_5648 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_punpckhwd_5648(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_punpckhwd(_destination, offsetAsInt());
        }
    }

    class rip_punpcklbw_3876 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_punpcklbw_3876(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_punpcklbw(_destination, offsetAsInt());
        }
    }

    class rip_punpcklbw_3804 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_punpcklbw_3804(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_punpcklbw(_destination, offsetAsInt());
        }
    }

    class rip_punpckldq_3894 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_punpckldq_3894(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_punpckldq(_destination, offsetAsInt());
        }
    }

    class rip_punpckldq_3822 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_punpckldq_3822(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_punpckldq(_destination, offsetAsInt());
        }
    }

    class rip_punpcklqdq_5738 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_punpcklqdq_5738(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_punpcklqdq(_destination, offsetAsInt());
        }
    }

    class rip_punpcklwd_3885 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_punpcklwd_3885(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_punpcklwd(_destination, offsetAsInt());
        }
    }

    class rip_punpcklwd_3813 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_punpcklwd_3813(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_punpcklwd(_destination, offsetAsInt());
        }
    }

    class rip_push_3057 extends InstructionWithOffset {
        rip_push_3057(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_push(offsetAsInt());
        }
    }

    class rip_pxor_6581 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_pxor_6581(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pxor(_destination, offsetAsInt());
        }
    }

    class rip_pxor_6509 extends InstructionWithOffset {
        private final MMXRegister _destination;
        rip_pxor_6509(int startPosition, int endPosition, MMXRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_pxor(_destination, offsetAsInt());
        }
    }

    class rip_rclb___1_1219 extends InstructionWithOffset {
        rip_rclb___1_1219(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rclb___1(offsetAsInt());
        }
    }

    class rip_rcll___1_1282 extends InstructionWithOffset {
        rip_rcll___1_1282(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rcll___1(offsetAsInt());
        }
    }

    class rip_rclq___1_1345 extends InstructionWithOffset {
        rip_rclq___1_1345(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rclq___1(offsetAsInt());
        }
    }

    class rip_rclw___1_1408 extends InstructionWithOffset {
        rip_rclw___1_1408(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rclw___1(offsetAsInt());
        }
    }

    class rip_rclb___CL_1471 extends InstructionWithOffset {
        rip_rclb___CL_1471(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rclb___CL(offsetAsInt());
        }
    }

    class rip_rcll___CL_1534 extends InstructionWithOffset {
        rip_rcll___CL_1534(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rcll___CL(offsetAsInt());
        }
    }

    class rip_rclq___CL_1597 extends InstructionWithOffset {
        rip_rclq___CL_1597(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rclq___CL(offsetAsInt());
        }
    }

    class rip_rclw___CL_1660 extends InstructionWithOffset {
        rip_rclw___CL_1660(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rclw___CL(offsetAsInt());
        }
    }

    class rip_rclb_929 extends InstructionWithOffset {
        private final byte _imm8;
        rip_rclb_929(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rclb(offsetAsInt(), _imm8);
        }
    }

    class rip_rcll_992 extends InstructionWithOffset {
        private final byte _imm8;
        rip_rcll_992(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rcll(offsetAsInt(), _imm8);
        }
    }

    class rip_rclq_1055 extends InstructionWithOffset {
        private final byte _imm8;
        rip_rclq_1055(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rclq(offsetAsInt(), _imm8);
        }
    }

    class rip_rclw_1118 extends InstructionWithOffset {
        private final byte _imm8;
        rip_rclw_1118(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rclw(offsetAsInt(), _imm8);
        }
    }

    class rip_rcpps_3677 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_rcpps_3677(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rcpps(_destination, offsetAsInt());
        }
    }

    class rip_rcpss_3795 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_rcpss_3795(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rcpss(_destination, offsetAsInt());
        }
    }

    class rip_rcrb___1_1223 extends InstructionWithOffset {
        rip_rcrb___1_1223(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rcrb___1(offsetAsInt());
        }
    }

    class rip_rcrl___1_1286 extends InstructionWithOffset {
        rip_rcrl___1_1286(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rcrl___1(offsetAsInt());
        }
    }

    class rip_rcrq___1_1349 extends InstructionWithOffset {
        rip_rcrq___1_1349(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rcrq___1(offsetAsInt());
        }
    }

    class rip_rcrw___1_1412 extends InstructionWithOffset {
        rip_rcrw___1_1412(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rcrw___1(offsetAsInt());
        }
    }

    class rip_rcrb___CL_1475 extends InstructionWithOffset {
        rip_rcrb___CL_1475(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rcrb___CL(offsetAsInt());
        }
    }

    class rip_rcrl___CL_1538 extends InstructionWithOffset {
        rip_rcrl___CL_1538(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rcrl___CL(offsetAsInt());
        }
    }

    class rip_rcrq___CL_1601 extends InstructionWithOffset {
        rip_rcrq___CL_1601(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rcrq___CL(offsetAsInt());
        }
    }

    class rip_rcrw___CL_1664 extends InstructionWithOffset {
        rip_rcrw___CL_1664(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rcrw___CL(offsetAsInt());
        }
    }

    class rip_rcrb_933 extends InstructionWithOffset {
        private final byte _imm8;
        rip_rcrb_933(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rcrb(offsetAsInt(), _imm8);
        }
    }

    class rip_rcrl_996 extends InstructionWithOffset {
        private final byte _imm8;
        rip_rcrl_996(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rcrl(offsetAsInt(), _imm8);
        }
    }

    class rip_rcrq_1059 extends InstructionWithOffset {
        private final byte _imm8;
        rip_rcrq_1059(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rcrq(offsetAsInt(), _imm8);
        }
    }

    class rip_rcrw_1122 extends InstructionWithOffset {
        private final byte _imm8;
        rip_rcrw_1122(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rcrw(offsetAsInt(), _imm8);
        }
    }

    class rip_rolb___1_1211 extends InstructionWithOffset {
        rip_rolb___1_1211(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rolb___1(offsetAsInt());
        }
    }

    class rip_roll___1_1274 extends InstructionWithOffset {
        rip_roll___1_1274(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_roll___1(offsetAsInt());
        }
    }

    class rip_rolq___1_1337 extends InstructionWithOffset {
        rip_rolq___1_1337(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rolq___1(offsetAsInt());
        }
    }

    class rip_rolw___1_1400 extends InstructionWithOffset {
        rip_rolw___1_1400(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rolw___1(offsetAsInt());
        }
    }

    class rip_rolb___CL_1463 extends InstructionWithOffset {
        rip_rolb___CL_1463(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rolb___CL(offsetAsInt());
        }
    }

    class rip_roll___CL_1526 extends InstructionWithOffset {
        rip_roll___CL_1526(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_roll___CL(offsetAsInt());
        }
    }

    class rip_rolq___CL_1589 extends InstructionWithOffset {
        rip_rolq___CL_1589(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rolq___CL(offsetAsInt());
        }
    }

    class rip_rolw___CL_1652 extends InstructionWithOffset {
        rip_rolw___CL_1652(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rolw___CL(offsetAsInt());
        }
    }

    class rip_rolb_921 extends InstructionWithOffset {
        private final byte _imm8;
        rip_rolb_921(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rolb(offsetAsInt(), _imm8);
        }
    }

    class rip_roll_984 extends InstructionWithOffset {
        private final byte _imm8;
        rip_roll_984(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_roll(offsetAsInt(), _imm8);
        }
    }

    class rip_rolq_1047 extends InstructionWithOffset {
        private final byte _imm8;
        rip_rolq_1047(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rolq(offsetAsInt(), _imm8);
        }
    }

    class rip_rolw_1110 extends InstructionWithOffset {
        private final byte _imm8;
        rip_rolw_1110(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rolw(offsetAsInt(), _imm8);
        }
    }

    class rip_rorb___1_1215 extends InstructionWithOffset {
        rip_rorb___1_1215(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rorb___1(offsetAsInt());
        }
    }

    class rip_rorl___1_1278 extends InstructionWithOffset {
        rip_rorl___1_1278(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rorl___1(offsetAsInt());
        }
    }

    class rip_rorq___1_1341 extends InstructionWithOffset {
        rip_rorq___1_1341(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rorq___1(offsetAsInt());
        }
    }

    class rip_rorw___1_1404 extends InstructionWithOffset {
        rip_rorw___1_1404(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rorw___1(offsetAsInt());
        }
    }

    class rip_rorb___CL_1467 extends InstructionWithOffset {
        rip_rorb___CL_1467(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rorb___CL(offsetAsInt());
        }
    }

    class rip_rorl___CL_1530 extends InstructionWithOffset {
        rip_rorl___CL_1530(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rorl___CL(offsetAsInt());
        }
    }

    class rip_rorq___CL_1593 extends InstructionWithOffset {
        rip_rorq___CL_1593(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rorq___CL(offsetAsInt());
        }
    }

    class rip_rorw___CL_1656 extends InstructionWithOffset {
        rip_rorw___CL_1656(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rorw___CL(offsetAsInt());
        }
    }

    class rip_rorb_925 extends InstructionWithOffset {
        private final byte _imm8;
        rip_rorb_925(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rorb(offsetAsInt(), _imm8);
        }
    }

    class rip_rorl_988 extends InstructionWithOffset {
        private final byte _imm8;
        rip_rorl_988(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rorl(offsetAsInt(), _imm8);
        }
    }

    class rip_rorq_1051 extends InstructionWithOffset {
        private final byte _imm8;
        rip_rorq_1051(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rorq(offsetAsInt(), _imm8);
        }
    }

    class rip_rorw_1114 extends InstructionWithOffset {
        private final byte _imm8;
        rip_rorw_1114(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rorw(offsetAsInt(), _imm8);
        }
    }

    class rip_rsqrtps_3668 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_rsqrtps_3668(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rsqrtps(_destination, offsetAsInt());
        }
    }

    class rip_rsqrtss_3786 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_rsqrtss_3786(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_rsqrtss(_destination, offsetAsInt());
        }
    }

    class rip_sarb___1_1235 extends InstructionWithOffset {
        rip_sarb___1_1235(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sarb___1(offsetAsInt());
        }
    }

    class rip_sarl___1_1298 extends InstructionWithOffset {
        rip_sarl___1_1298(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sarl___1(offsetAsInt());
        }
    }

    class rip_sarq___1_1361 extends InstructionWithOffset {
        rip_sarq___1_1361(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sarq___1(offsetAsInt());
        }
    }

    class rip_sarw___1_1424 extends InstructionWithOffset {
        rip_sarw___1_1424(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sarw___1(offsetAsInt());
        }
    }

    class rip_sarb___CL_1487 extends InstructionWithOffset {
        rip_sarb___CL_1487(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sarb___CL(offsetAsInt());
        }
    }

    class rip_sarl___CL_1550 extends InstructionWithOffset {
        rip_sarl___CL_1550(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sarl___CL(offsetAsInt());
        }
    }

    class rip_sarq___CL_1613 extends InstructionWithOffset {
        rip_sarq___CL_1613(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sarq___CL(offsetAsInt());
        }
    }

    class rip_sarw___CL_1676 extends InstructionWithOffset {
        rip_sarw___CL_1676(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sarw___CL(offsetAsInt());
        }
    }

    class rip_sarb_945 extends InstructionWithOffset {
        private final byte _imm8;
        rip_sarb_945(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sarb(offsetAsInt(), _imm8);
        }
    }

    class rip_sarl_1008 extends InstructionWithOffset {
        private final byte _imm8;
        rip_sarl_1008(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sarl(offsetAsInt(), _imm8);
        }
    }

    class rip_sarq_1071 extends InstructionWithOffset {
        private final byte _imm8;
        rip_sarq_1071(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sarq(offsetAsInt(), _imm8);
        }
    }

    class rip_sarw_1134 extends InstructionWithOffset {
        private final byte _imm8;
        rip_sarw_1134(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sarw(offsetAsInt(), _imm8);
        }
    }

    class rip_sbb_2118 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_sbb_2118(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sbb(_destination, offsetAsInt());
        }
    }

    class rip_sbb_2102 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_sbb_2102(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sbb(_destination, offsetAsInt());
        }
    }

    class rip_sbb_2110 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _destination;
        rip_sbb_2110(int startPosition, int endPosition, AMD64GeneralRegister64 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sbb(_destination, offsetAsInt());
        }
    }

    class rip_sbb_2094 extends InstructionWithOffset {
        private final AMD64GeneralRegister8 _destination;
        rip_sbb_2094(int startPosition, int endPosition, AMD64GeneralRegister8 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sbb(_destination, offsetAsInt());
        }
    }

    class rip_sbbb_336 extends InstructionWithOffset {
        private final byte _imm8;
        rip_sbbb_336(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sbbb(offsetAsInt(), _imm8);
        }
    }

    class rip_sbbl_624 extends InstructionWithOffset {
        private final byte _imm8;
        rip_sbbl_624(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sbbl(offsetAsInt(), _imm8);
        }
    }

    class rip_sbbq_696 extends InstructionWithOffset {
        private final byte _imm8;
        rip_sbbq_696(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sbbq(offsetAsInt(), _imm8);
        }
    }

    class rip_sbbw_768 extends InstructionWithOffset {
        private final byte _imm8;
        rip_sbbw_768(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sbbw(offsetAsInt(), _imm8);
        }
    }

    class rip_sbb_2085 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _source;
        rip_sbb_2085(int startPosition, int endPosition, AMD64GeneralRegister16 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sbb(offsetAsInt(), _source);
        }
    }

    class rip_sbb_2067 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _source;
        rip_sbb_2067(int startPosition, int endPosition, AMD64GeneralRegister32 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sbb(offsetAsInt(), _source);
        }
    }

    class rip_sbb_2076 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _source;
        rip_sbb_2076(int startPosition, int endPosition, AMD64GeneralRegister64 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sbb(offsetAsInt(), _source);
        }
    }

    class rip_sbb_2058 extends InstructionWithOffset {
        private final AMD64GeneralRegister8 _source;
        rip_sbb_2058(int startPosition, int endPosition, AMD64GeneralRegister8 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sbb(offsetAsInt(), _source);
        }
    }

    class rip_sbbl_408 extends InstructionWithOffset {
        private final int _imm32;
        rip_sbbl_408(int startPosition, int endPosition, int imm32, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm32 = imm32;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sbbl(offsetAsInt(), _imm32);
        }
    }

    class rip_sbbq_480 extends InstructionWithOffset {
        private final int _imm32;
        rip_sbbq_480(int startPosition, int endPosition, int imm32, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm32 = imm32;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sbbq(offsetAsInt(), _imm32);
        }
    }

    class rip_sbbw_552 extends InstructionWithOffset {
        private final short _imm16;
        rip_sbbw_552(int startPosition, int endPosition, short imm16, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm16 = imm16;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sbbw(offsetAsInt(), _imm16);
        }
    }

    class rip_setb_4083 extends InstructionWithOffset {
        rip_setb_4083(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_setb(offsetAsInt());
        }
    }

    class rip_setbe_4119 extends InstructionWithOffset {
        rip_setbe_4119(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_setbe(offsetAsInt());
        }
    }

    class rip_setl_5940 extends InstructionWithOffset {
        rip_setl_5940(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_setl(offsetAsInt());
        }
    }

    class rip_setle_5958 extends InstructionWithOffset {
        rip_setle_5958(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_setle(offsetAsInt());
        }
    }

    class rip_setnb_4092 extends InstructionWithOffset {
        rip_setnb_4092(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_setnb(offsetAsInt());
        }
    }

    class rip_setnbe_4128 extends InstructionWithOffset {
        rip_setnbe_4128(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_setnbe(offsetAsInt());
        }
    }

    class rip_setnl_5949 extends InstructionWithOffset {
        rip_setnl_5949(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_setnl(offsetAsInt());
        }
    }

    class rip_setnle_5967 extends InstructionWithOffset {
        rip_setnle_5967(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_setnle(offsetAsInt());
        }
    }

    class rip_setno_4074 extends InstructionWithOffset {
        rip_setno_4074(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_setno(offsetAsInt());
        }
    }

    class rip_setnp_5931 extends InstructionWithOffset {
        rip_setnp_5931(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_setnp(offsetAsInt());
        }
    }

    class rip_setns_5913 extends InstructionWithOffset {
        rip_setns_5913(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_setns(offsetAsInt());
        }
    }

    class rip_setnz_4110 extends InstructionWithOffset {
        rip_setnz_4110(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_setnz(offsetAsInt());
        }
    }

    class rip_seto_4065 extends InstructionWithOffset {
        rip_seto_4065(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_seto(offsetAsInt());
        }
    }

    class rip_setp_5922 extends InstructionWithOffset {
        rip_setp_5922(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_setp(offsetAsInt());
        }
    }

    class rip_sets_5904 extends InstructionWithOffset {
        rip_sets_5904(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sets(offsetAsInt());
        }
    }

    class rip_setz_4101 extends InstructionWithOffset {
        rip_setz_4101(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_setz(offsetAsInt());
        }
    }

    class rip_sgdt_3135 extends InstructionWithOffset {
        rip_sgdt_3135(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sgdt(offsetAsInt());
        }
    }

    class rip_shlb___1_1227 extends InstructionWithOffset {
        rip_shlb___1_1227(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shlb___1(offsetAsInt());
        }
    }

    class rip_shll___1_1290 extends InstructionWithOffset {
        rip_shll___1_1290(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shll___1(offsetAsInt());
        }
    }

    class rip_shlq___1_1353 extends InstructionWithOffset {
        rip_shlq___1_1353(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shlq___1(offsetAsInt());
        }
    }

    class rip_shlw___1_1416 extends InstructionWithOffset {
        rip_shlw___1_1416(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shlw___1(offsetAsInt());
        }
    }

    class rip_shlb___CL_1479 extends InstructionWithOffset {
        rip_shlb___CL_1479(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shlb___CL(offsetAsInt());
        }
    }

    class rip_shll___CL_1542 extends InstructionWithOffset {
        rip_shll___CL_1542(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shll___CL(offsetAsInt());
        }
    }

    class rip_shlq___CL_1605 extends InstructionWithOffset {
        rip_shlq___CL_1605(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shlq___CL(offsetAsInt());
        }
    }

    class rip_shlw___CL_1668 extends InstructionWithOffset {
        rip_shlw___CL_1668(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shlw___CL(offsetAsInt());
        }
    }

    class rip_shlb_937 extends InstructionWithOffset {
        private final byte _imm8;
        rip_shlb_937(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shlb(offsetAsInt(), _imm8);
        }
    }

    class rip_shll_1000 extends InstructionWithOffset {
        private final byte _imm8;
        rip_shll_1000(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shll(offsetAsInt(), _imm8);
        }
    }

    class rip_shlq_1063 extends InstructionWithOffset {
        private final byte _imm8;
        rip_shlq_1063(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shlq(offsetAsInt(), _imm8);
        }
    }

    class rip_shlw_1126 extends InstructionWithOffset {
        private final byte _imm8;
        rip_shlw_1126(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shlw(offsetAsInt(), _imm8);
        }
    }

    class rip_shld_CL_4212 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _source;
        rip_shld_CL_4212(int startPosition, int endPosition, AMD64GeneralRegister16 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shld_CL(offsetAsInt(), _source);
        }
    }

    class rip_shld_4185 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _source;
        private final byte _imm8;
        rip_shld_4185(int startPosition, int endPosition, AMD64GeneralRegister16 source, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shld(offsetAsInt(), _source, _imm8);
        }
    }

    class rip_shld_CL_4194 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _source;
        rip_shld_CL_4194(int startPosition, int endPosition, AMD64GeneralRegister32 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shld_CL(offsetAsInt(), _source);
        }
    }

    class rip_shld_4167 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _source;
        private final byte _imm8;
        rip_shld_4167(int startPosition, int endPosition, AMD64GeneralRegister32 source, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shld(offsetAsInt(), _source, _imm8);
        }
    }

    class rip_shld_CL_4203 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _source;
        rip_shld_CL_4203(int startPosition, int endPosition, AMD64GeneralRegister64 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shld_CL(offsetAsInt(), _source);
        }
    }

    class rip_shld_4176 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _source;
        private final byte _imm8;
        rip_shld_4176(int startPosition, int endPosition, AMD64GeneralRegister64 source, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shld(offsetAsInt(), _source, _imm8);
        }
    }

    class rip_shrb___1_1231 extends InstructionWithOffset {
        rip_shrb___1_1231(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shrb___1(offsetAsInt());
        }
    }

    class rip_shrl___1_1294 extends InstructionWithOffset {
        rip_shrl___1_1294(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shrl___1(offsetAsInt());
        }
    }

    class rip_shrq___1_1357 extends InstructionWithOffset {
        rip_shrq___1_1357(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shrq___1(offsetAsInt());
        }
    }

    class rip_shrw___1_1420 extends InstructionWithOffset {
        rip_shrw___1_1420(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shrw___1(offsetAsInt());
        }
    }

    class rip_shrb___CL_1483 extends InstructionWithOffset {
        rip_shrb___CL_1483(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shrb___CL(offsetAsInt());
        }
    }

    class rip_shrl___CL_1546 extends InstructionWithOffset {
        rip_shrl___CL_1546(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shrl___CL(offsetAsInt());
        }
    }

    class rip_shrq___CL_1609 extends InstructionWithOffset {
        rip_shrq___CL_1609(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shrq___CL(offsetAsInt());
        }
    }

    class rip_shrw___CL_1672 extends InstructionWithOffset {
        rip_shrw___CL_1672(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shrw___CL(offsetAsInt());
        }
    }

    class rip_shrb_941 extends InstructionWithOffset {
        private final byte _imm8;
        rip_shrb_941(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shrb(offsetAsInt(), _imm8);
        }
    }

    class rip_shrl_1004 extends InstructionWithOffset {
        private final byte _imm8;
        rip_shrl_1004(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shrl(offsetAsInt(), _imm8);
        }
    }

    class rip_shrq_1067 extends InstructionWithOffset {
        private final byte _imm8;
        rip_shrq_1067(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shrq(offsetAsInt(), _imm8);
        }
    }

    class rip_shrw_1130 extends InstructionWithOffset {
        private final byte _imm8;
        rip_shrw_1130(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shrw(offsetAsInt(), _imm8);
        }
    }

    class rip_shrd_CL_6051 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _source;
        rip_shrd_CL_6051(int startPosition, int endPosition, AMD64GeneralRegister16 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shrd_CL(offsetAsInt(), _source);
        }
    }

    class rip_shrd_6024 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _source;
        private final byte _imm8;
        rip_shrd_6024(int startPosition, int endPosition, AMD64GeneralRegister16 source, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shrd(offsetAsInt(), _source, _imm8);
        }
    }

    class rip_shrd_CL_6033 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _source;
        rip_shrd_CL_6033(int startPosition, int endPosition, AMD64GeneralRegister32 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shrd_CL(offsetAsInt(), _source);
        }
    }

    class rip_shrd_6006 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _source;
        private final byte _imm8;
        rip_shrd_6006(int startPosition, int endPosition, AMD64GeneralRegister32 source, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shrd(offsetAsInt(), _source, _imm8);
        }
    }

    class rip_shrd_CL_6042 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _source;
        rip_shrd_CL_6042(int startPosition, int endPosition, AMD64GeneralRegister64 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shrd_CL(offsetAsInt(), _source);
        }
    }

    class rip_shrd_6015 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _source;
        private final byte _imm8;
        rip_shrd_6015(int startPosition, int endPosition, AMD64GeneralRegister64 source, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shrd(offsetAsInt(), _source, _imm8);
        }
    }

    class rip_shufpd_4436 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        private final byte _imm8;
        rip_shufpd_4436(int startPosition, int endPosition, AMD64XMMRegister destination, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shufpd(_destination, offsetAsInt(), _imm8);
        }
    }

    class rip_shufps_4400 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        private final byte _imm8;
        rip_shufps_4400(int startPosition, int endPosition, AMD64XMMRegister destination, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_shufps(_destination, offsetAsInt(), _imm8);
        }
    }

    class rip_sidt_3139 extends InstructionWithOffset {
        rip_sidt_3139(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sidt(offsetAsInt());
        }
    }

    class rip_sldt_3077 extends InstructionWithOffset {
        rip_sldt_3077(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sldt(offsetAsInt());
        }
    }

    class rip_smsw_3151 extends InstructionWithOffset {
        rip_smsw_3151(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_smsw(offsetAsInt());
        }
    }

    class rip_sqrtpd_3723 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_sqrtpd_3723(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sqrtpd(_destination, offsetAsInt());
        }
    }

    class rip_sqrtps_3659 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_sqrtps_3659(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sqrtps(_destination, offsetAsInt());
        }
    }

    class rip_sqrtsd_3768 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_sqrtsd_3768(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sqrtsd(_destination, offsetAsInt());
        }
    }

    class rip_sqrtss_3777 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_sqrtss_3777(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sqrtss(_destination, offsetAsInt());
        }
    }

    class rip_stmxcsr_6072 extends InstructionWithOffset {
        rip_stmxcsr_6072(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_stmxcsr(offsetAsInt());
        }
    }

    class rip_str_3081 extends InstructionWithOffset {
        rip_str_3081(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_str(offsetAsInt());
        }
    }

    class rip_sub_2190 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_sub_2190(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sub(_destination, offsetAsInt());
        }
    }

    class rip_sub_2174 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_sub_2174(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sub(_destination, offsetAsInt());
        }
    }

    class rip_sub_2166 extends InstructionWithOffset {
        private final AMD64GeneralRegister8 _destination;
        rip_sub_2166(int startPosition, int endPosition, AMD64GeneralRegister8 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sub(_destination, offsetAsInt());
        }
    }

    class rip_subb_344 extends InstructionWithOffset {
        private final byte _imm8;
        rip_subb_344(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_subb(offsetAsInt(), _imm8);
        }
    }

    class rip_subl_632 extends InstructionWithOffset {
        private final byte _imm8;
        rip_subl_632(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_subl(offsetAsInt(), _imm8);
        }
    }

    class rip_subq_704 extends InstructionWithOffset {
        private final byte _imm8;
        rip_subq_704(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_subq(offsetAsInt(), _imm8);
        }
    }

    class rip_subw_776 extends InstructionWithOffset {
        private final byte _imm8;
        rip_subw_776(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_subw(offsetAsInt(), _imm8);
        }
    }

    class rip_sub_2157 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _source;
        rip_sub_2157(int startPosition, int endPosition, AMD64GeneralRegister16 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sub(offsetAsInt(), _source);
        }
    }

    class rip_sub_2139 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _source;
        rip_sub_2139(int startPosition, int endPosition, AMD64GeneralRegister32 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sub(offsetAsInt(), _source);
        }
    }

    class rip_sub_2148 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _source;
        rip_sub_2148(int startPosition, int endPosition, AMD64GeneralRegister64 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sub(offsetAsInt(), _source);
        }
    }

    class rip_sub_2130 extends InstructionWithOffset {
        private final AMD64GeneralRegister8 _source;
        rip_sub_2130(int startPosition, int endPosition, AMD64GeneralRegister8 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_sub(offsetAsInt(), _source);
        }
    }

    class rip_subl_416 extends InstructionWithOffset {
        private final int _imm32;
        rip_subl_416(int startPosition, int endPosition, int imm32, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm32 = imm32;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_subl(offsetAsInt(), _imm32);
        }
    }

    class rip_subq_488 extends InstructionWithOffset {
        private final int _imm32;
        rip_subq_488(int startPosition, int endPosition, int imm32, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm32 = imm32;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_subq(offsetAsInt(), _imm32);
        }
    }

    class rip_subw_560 extends InstructionWithOffset {
        private final short _imm16;
        rip_subw_560(int startPosition, int endPosition, short imm16, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm16 = imm16;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_subw(offsetAsInt(), _imm16);
        }
    }

    class rip_subpd_5468 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_subpd_5468(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_subpd(_destination, offsetAsInt());
        }
    }

    class rip_subps_5396 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_subps_5396(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_subps(_destination, offsetAsInt());
        }
    }

    class rip_subsd_5531 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_subsd_5531(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_subss(_destination, offsetAsInt());
        }
    }

    class rip_testb_1734 extends InstructionWithOffset {
        private final byte _imm8;
        rip_testb_1734(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_testb(offsetAsInt(), _imm8);
        }
    }

    class rip_test_855 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _source;
        rip_test_855(int startPosition, int endPosition, AMD64GeneralRegister16 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_test(offsetAsInt(), _source);
        }
    }

    class rip_test_837 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _source;
        rip_test_837(int startPosition, int endPosition, AMD64GeneralRegister32 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_test(offsetAsInt(), _source);
        }
    }

    class rip_test_846 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _source;
        rip_test_846(int startPosition, int endPosition, AMD64GeneralRegister64 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_test(offsetAsInt(), _source);
        }
    }

    class rip_test_828 extends InstructionWithOffset {
        private final AMD64GeneralRegister8 _source;
        rip_test_828(int startPosition, int endPosition, AMD64GeneralRegister8 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_test(offsetAsInt(), _source);
        }
    }

    class rip_testl_1797 extends InstructionWithOffset {
        private final int _imm32;
        rip_testl_1797(int startPosition, int endPosition, int imm32, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm32 = imm32;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_testl(offsetAsInt(), _imm32);
        }
    }

    class rip_testq_1860 extends InstructionWithOffset {
        private final int _imm32;
        rip_testq_1860(int startPosition, int endPosition, int imm32, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm32 = imm32;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_testq(offsetAsInt(), _imm32);
        }
    }

    class rip_testw_1923 extends InstructionWithOffset {
        private final short _imm16;
        rip_testw_1923(int startPosition, int endPosition, short imm16, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm16 = imm16;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_testw(offsetAsInt(), _imm16);
        }
    }

    class rip_ucomisd_5018 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_ucomisd_5018(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_ucomisd(_destination, offsetAsInt());
        }
    }

    class rip_ucomiss_4948 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_ucomiss_4948(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_ucomiss(_destination, offsetAsInt());
        }
    }

    class rip_unpckhpd_3348 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_unpckhpd_3348(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_unpckhpd(_destination, offsetAsInt());
        }
    }

    class rip_unpckhps_3288 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_unpckhps_3288(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_unpckhps(_destination, offsetAsInt());
        }
    }

    class rip_unpcklpd_3339 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_unpcklpd_3339(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_unpcklpd(_destination, offsetAsInt());
        }
    }

    class rip_unpcklps_3279 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_unpcklps_3279(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_unpcklps(_destination, offsetAsInt());
        }
    }

    class rip_verr_3093 extends InstructionWithOffset {
        rip_verr_3093(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_verr(offsetAsInt());
        }
    }

    class rip_verw_3097 extends InstructionWithOffset {
        rip_verw_3097(int startPosition, int endPosition, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_verw(offsetAsInt());
        }
    }

    class rip_xadd_4356 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _source;
        rip_xadd_4356(int startPosition, int endPosition, AMD64GeneralRegister16 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xadd(offsetAsInt(), _source);
        }
    }

    class rip_xadd_4338 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _source;
        rip_xadd_4338(int startPosition, int endPosition, AMD64GeneralRegister32 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xadd(offsetAsInt(), _source);
        }
    }

    class rip_xadd_4347 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _source;
        rip_xadd_4347(int startPosition, int endPosition, AMD64GeneralRegister64 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xadd(offsetAsInt(), _source);
        }
    }

    class rip_xadd_4329 extends InstructionWithOffset {
        private final AMD64GeneralRegister8 _source;
        rip_xadd_4329(int startPosition, int endPosition, AMD64GeneralRegister8 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xadd(offsetAsInt(), _source);
        }
    }

    class rip_xchg_891 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _source;
        rip_xchg_891(int startPosition, int endPosition, AMD64GeneralRegister16 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xchg(offsetAsInt(), _source);
        }
    }

    class rip_xchg_873 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _source;
        rip_xchg_873(int startPosition, int endPosition, AMD64GeneralRegister32 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xchg(offsetAsInt(), _source);
        }
    }

    class rip_xchg_882 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _source;
        rip_xchg_882(int startPosition, int endPosition, AMD64GeneralRegister64 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xchg(offsetAsInt(), _source);
        }
    }

    class rip_xchg_864 extends InstructionWithOffset {
        private final AMD64GeneralRegister8 _source;
        rip_xchg_864(int startPosition, int endPosition, AMD64GeneralRegister8 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xchg(offsetAsInt(), _source);
        }
    }

    class rip_xor_280 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _destination;
        rip_xor_280(int startPosition, int endPosition, AMD64GeneralRegister16 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xor(_destination, offsetAsInt());
        }
    }

    class rip_xor_264 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _destination;
        rip_xor_264(int startPosition, int endPosition, AMD64GeneralRegister32 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
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
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xor(_destination, offsetAsInt());
        }
    }

    class rip_xor_256 extends InstructionWithOffset {
        private final AMD64GeneralRegister8 _destination;
        rip_xor_256(int startPosition, int endPosition, AMD64GeneralRegister8 destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xor(_destination, offsetAsInt());
        }
    }

    class rip_xorb_348 extends InstructionWithOffset {
        private final byte _imm8;
        rip_xorb_348(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xorb(offsetAsInt(), _imm8);
        }
    }

    class rip_xorl_636 extends InstructionWithOffset {
        private final byte _imm8;
        rip_xorl_636(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xorl(offsetAsInt(), _imm8);
        }
    }

    class rip_xorq_708 extends InstructionWithOffset {
        private final byte _imm8;
        rip_xorq_708(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xorq(offsetAsInt(), _imm8);
        }
    }

    class rip_xorw_780 extends InstructionWithOffset {
        private final byte _imm8;
        rip_xorw_780(int startPosition, int endPosition, byte imm8, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm8 = imm8;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xorw(offsetAsInt(), _imm8);
        }
    }

    class rip_xor_247 extends InstructionWithOffset {
        private final AMD64GeneralRegister16 _source;
        rip_xor_247(int startPosition, int endPosition, AMD64GeneralRegister16 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xor(offsetAsInt(), _source);
        }
    }

    class rip_xor_229 extends InstructionWithOffset {
        private final AMD64GeneralRegister32 _source;
        rip_xor_229(int startPosition, int endPosition, AMD64GeneralRegister32 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xor(offsetAsInt(), _source);
        }
    }

    class rip_xor_238 extends InstructionWithOffset {
        private final AMD64GeneralRegister64 _source;
        rip_xor_238(int startPosition, int endPosition, AMD64GeneralRegister64 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xor(offsetAsInt(), _source);
        }
    }

    class rip_xor_220 extends InstructionWithOffset {
        private final AMD64GeneralRegister8 _source;
        rip_xor_220(int startPosition, int endPosition, AMD64GeneralRegister8 source, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _source = source;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xor(offsetAsInt(), _source);
        }
    }

    class rip_xorl_420 extends InstructionWithOffset {
        private final int _imm32;
        rip_xorl_420(int startPosition, int endPosition, int imm32, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm32 = imm32;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xorl(offsetAsInt(), _imm32);
        }
    }

    class rip_xorq_492 extends InstructionWithOffset {
        private final int _imm32;
        rip_xorq_492(int startPosition, int endPosition, int imm32, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm32 = imm32;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xorq(offsetAsInt(), _imm32);
        }
    }

    class rip_xorw_564 extends InstructionWithOffset {
        private final short _imm16;
        rip_xorw_564(int startPosition, int endPosition, short imm16, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _imm16 = imm16;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xorw(offsetAsInt(), _imm16);
        }
    }

    class rip_xorpd_3759 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_xorpd_3759(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xorpd(_destination, offsetAsInt());
        }
    }

    class rip_xorps_3713 extends InstructionWithOffset {
        private final AMD64XMMRegister _destination;
        rip_xorps_3713(int startPosition, int endPosition, AMD64XMMRegister destination, Label label) {
            super(AMD64LabelAssembler.this, startPosition, currentPosition(), label, 4);
            _destination = destination;
        }
        @Override
        protected void assemble() throws AssemblyException {
            rip_xorps(_destination, offsetAsInt());
        }
    }

// END GENERATED LABEL ASSEMBLER METHODS
}
