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
/*VCSID=191091df-101a-4548-956c-6e923d63ecc7*/
package com.sun.max.asm.arm.example;

import com.sun.max.asm.arm.*;

/**
 *
 * @author Doug Simon
 */
public interface ExampleARMAssemblerSpecification {

    public static class Generator {
        public static void main(String[] args) throws Exception {
            final String[] programArguments = {
                "-i=" + ExampleARMAssemblerSpecification.class.getName(),
                "-r=" + ExampleARMAssembler.class.getName(),
                "-l=" + ExampleARMAssembler.class.getName()
            };
            // Using reflection prevents a literal reference to a class in the assembler generator framework.
            Class.forName("com.sun.max.asm.gen.risc.arm.ARMAssemblerGenerator").
                getMethod("main", String[].class).invoke(null, (Object) programArguments);
        }
    }

    // Checkstyle: stop

    void adc(final ConditionCode cond, final SBit s, final GPR Rd, final GPR Rn, final int immed_8, final int rotate_amount);
    void adclsl(final ConditionCode cond, final SBit s, final GPR Rd, final GPR Rn, final GPR Rm, final int shift_imm);
    void add(final ConditionCode cond, final SBit s, final GPR Rd, final GPR Rn, final int immed_8, final int rotate_amount);
    void addror(final ConditionCode cond, final SBit s, final GPR Rd, final GPR Rn, final GPR Rm, final int shift_imm);
    void biclsr(final ConditionCode cond, final SBit s, final GPR Rd, final GPR Rn, final GPR Rm, final GPR Rs);
    void cmnasr(final ConditionCode cond, final GPR Rn, final GPR Rm, final GPR Rs);
    void cmnror(final ConditionCode cond, final GPR Rn, final GPR Rm, final GPR Rs);
    void cmpasr(final ConditionCode cond, final GPR Rn, final GPR Rm, final int shift_imm);
    void eorlsr(final ConditionCode cond, final SBit s, final GPR Rd, final GPR Rn, final GPR Rm, final GPR Rs);
    void movror(final ConditionCode cond, final SBit s, final GPR Rd, final GPR Rm, final int shift_imm);
    void mvnror(final ConditionCode cond, final SBit s, final GPR Rd, final GPR Rm, final GPR Rs);
    void orrlsl(final ConditionCode cond, final SBit s, final GPR Rd, final GPR Rn, final GPR Rm, final int shift_imm);
    void rsb(final ConditionCode cond, final SBit s, final GPR Rd, final GPR Rn, final int immed_8, final int rotate_amount);
    void rsblsl(final ConditionCode cond, final SBit s, final GPR Rd, final GPR Rn, final GPR Rm, final int shift_imm);
    void rsc(final ConditionCode cond, final SBit s, final GPR Rd, final GPR Rn, final GPR Rm);
    void rsclsr(final ConditionCode cond, final SBit s, final GPR Rd, final GPR Rn, final GPR Rm, final GPR Rs);
    void sbcror(final ConditionCode cond, final SBit s, final GPR Rd, final GPR Rn, final GPR Rm, final GPR Rs);
    void sub(final ConditionCode cond, final SBit s, final GPR Rd, final GPR Rn, final int immed_8, final int rotate_amount);
    void tst(final ConditionCode cond, final GPR Rn, final int immediate);
    void tstlsr(final ConditionCode cond, final GPR Rn, final GPR Rm, final int shift_imm);
    void smlal(final ConditionCode cond, final SBit s, final GPR RdLo, final GPR RdHi, final GPR Rm, final GPR Rs);
    void umull(final ConditionCode cond, final SBit s, final GPR RdLo, final GPR RdHi, final GPR Rm, final GPR Rs);
    void ldradd(final ConditionCode cond, final GPR Rd, final GPR Rn, final GPR Rm);
    void ldrsubw(final ConditionCode cond, final GPR Rd, final GPR Rn, final int offset_12);
    void ldraddrorpost(final ConditionCode cond, final GPR Rd, final GPR Rn, final GPR Rm, final int shift_imm);
    void strsubasr(final ConditionCode cond, final GPR Rd, final GPR Rn, final GPR Rm, final int shift_imm);
    void strsubrorw(final ConditionCode cond, final GPR Rd, final GPR Rn, final GPR Rm, final int shift_imm);
    void straddpost(final ConditionCode cond, final GPR Rd, final GPR Rn, final int offset_12);
    void swi(final ConditionCode cond, final int immed_24);

    // Checkstyle: resume

}
