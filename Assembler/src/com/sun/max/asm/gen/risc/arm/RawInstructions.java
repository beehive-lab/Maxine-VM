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
/*VCSID=7efc54dd-ab63-40f0-b5f2-bb1b3aac12e1*/

package com.sun.max.asm.gen.risc.arm;

import static com.sun.max.asm.gen.Expression.Static.*;
import static com.sun.max.asm.gen.InstructionConstraint.Static.*;
import static com.sun.max.asm.gen.risc.arm.ARMFields.*;

import java.lang.reflect.*;

import com.sun.max.asm.*;
import com.sun.max.asm.arm.*;
import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.risc.*;
import com.sun.max.collect.*;

/**
 * Definitions of ARM instructions.
 * 
 * @author Sumeet Panchal
 */

public final class RawInstructions extends ARMInstructionDescriptionCreator {
    final Immediate32Argument _zero = new Immediate32Argument(0);

    RawInstructions(ARMTemplateCreator templateCreator) {
        super(templateCreator);

        generateBranch();
        generateDataProcessing();
        generateMultiply();
        generateMiscellaneousArithmetic();
        generateStatusRegisterAccess();
        generateLoadAndStore();
        generateLoadAndStoreMultiple();
        generateSemaphore();
        generateExceptionGenerating();
        generateCoprocessor();
    }

    public RiscInstructionDescription createInlineBytesInstructionDescription() {
        return createInstructionDescription(new ArraySequence<Object>(new Object[]{".byte", _byte3, ", ", _byte2, ", ", _byte1, ", ", _byte0}));
    }

    private void generateCoprocessor() {
        // TODO Auto-generated method stub

    }

    private void generateExceptionGenerating() {
        setCurrentArchitectureManualSection("4.1.7");
        define("bkpt", bits_31_28(14), bits_27_20(18), bits_7_4(7), _immediate2, immed_19_8(and(rightShift(_immediate2, 4), 0xfff)), immed_3_0(and(_immediate2, 0xf)));
        setCurrentArchitectureManualSection("4.1.50");
        define("swi", _cond, bits_27_24(15), _immed_24);
    }

    private void generateSemaphore() {
        setCurrentArchitectureManualSection("4.1.51");
        define("swp", _cond, _Rd, _Rm, ", [", _Rn, "]", bits_27_20(16), sbz_11_8(0), bits_7_4(9), ne(_Rd, 15), ne(_Rm, 15), ne(_Rn, 15), ne(_Rn, _Rd), ne(_Rn, _Rm));
        setCurrentArchitectureManualSection("4.1.52");
        define("swpb", _cond, _Rd, _Rm, ", [", _Rn, "]", bits_27_20(20), sbz_11_8(0), bits_7_4(9), ne(_Rd, 15), ne(_Rm, 15), ne(_Rn, 15), ne(_Rn, _Rd), ne(_Rn, _Rm));
    }

    private void generateLoadAndStoreMultiple() {
        // TODO Auto-generated method stub

    }

    private void generateLoadAndStore() {
        // TODO Auto-generated method stub
        defineLoadAndStoreForAddressingModesExceptPostIndexed("4.1.20", "ldr", b(0), l(1));
        defineLoadAndStoreForPostIndexedAddressingModes("4.1.20", "ldr", b(0), l(1), w(0));

        defineLoadAndStoreForAddressingModesExceptPostIndexed("4.1.44", "str", b(0), l(0));
        defineLoadAndStoreForPostIndexedAddressingModes("4.1.44", "str", b(0), l(0), w(0));

//        defineLoadAndStoreForAddressingModesExceptPostIndexed("4.1.21", "ldrb", b(1), l(1));
//        defineLoadAndStoreForPostIndexedAddressingModes("4.1.21", "ldrb", b(1), l(1), w(0));

//        defineLoadAndStoreForAddressingModesExceptPostIndexed("4.1.45", "strb", b(1), l(0));
//        defineLoadAndStoreForPostIndexedAddressingModes("4.1.45", "strb", b(1), l(0), w(0));

//        defineLoadAndStoreForPostIndexedAddressingModes("4.1.22", "ldrbt", b(1), l(1), w(1));

    }

    private void generateStatusRegisterAccess() {
     // TODO Auto-generated method stub
        setCurrentArchitectureManualSection("4.1.31");
        define("mrscpsr", _cond, bit_27(0), bit_26(0), bit_25(0), bit_24(1), bit_23(0), r(0), bit_21(0), bit_20(0), sbo_19_16(15), _Rd, sbz_11_0(0), ", cpsr").setExternalName("mrs");
        define("mrsspsr", _cond, bit_27(0), bit_26(0), bit_25(0), bit_24(1), bit_23(0), r(1), bit_21(0), bit_20(0), sbo_19_16(15), _Rd, sbz_11_0(0), ", spsr").setExternalName("mrs");
        //setCurrentArchitectureManualSection("4.1.32");
        //define("msr");
    }

    private void generateMiscellaneousArithmetic() {
        setCurrentArchitectureManualSection("4.1.12");
        define("clz", _cond, bits_27_20(22), sbo_19_16(15), _Rd, sbo_11_8(15), bits_7_4(1), _Rm, ne(_Rd, 15), ne(_Rm, 15));
    }

    private void generateMultiply() {
        setCurrentArchitectureManualSection("4.1.28");
        define("mla", _cond, bits_27_21(1), _s, _Rd2, _Rm, _Rs, _Rn2, bits_7_4(9), ne(_Rd2, _Rm), ne(_Rd2, 15), ne(_Rm, 15), ne(_Rs, 15), ne(_Rn2, 15));
        setCurrentArchitectureManualSection("4.1.33");
        define("mul", _cond, bits_27_21(0), _s, _Rd2, _Rm, _Rs, sbz_15_12(0), bits_7_4(9), ne(_Rd2, _Rm), ne(_Rd2, 15), ne(_Rm, 15), ne(_Rs, 15));
        setCurrentArchitectureManualSection("4.1.39");
        define("smlal", _cond, bits_27_21(7), _s, _RdLo, _RdHi, _Rm, _Rs, bits_7_4(9), ne(_RdLo, _RdHi), ne(_RdLo, _Rm), ne(_RdHi, _Rm), ne(_RdHi, 15), ne(_RdLo, 15), ne(_Rm, 15), ne(_Rs, 15));
        setCurrentArchitectureManualSection("4.1.40");
        define("smull", _cond, bits_27_21(6), _s, _RdLo, _RdHi, _Rm, _Rs, bits_7_4(9), ne(_RdLo, _RdHi), ne(_RdLo, _Rm), ne(_RdHi, _Rm), ne(_RdHi, 15), ne(_RdLo, 15), ne(_Rm, 15), ne(_Rs, 15));
        setCurrentArchitectureManualSection("4.1.55");
        define("umlal", _cond, bits_27_21(5), _s, _RdLo, _RdHi, _Rm, _Rs, bits_7_4(9), ne(_RdLo, _RdHi), ne(_RdLo, _Rm), ne(_RdHi, _Rm), ne(_RdHi, 15), ne(_RdLo, 15), ne(_Rm, 15), ne(_Rs, 15));
        setCurrentArchitectureManualSection("4.1.56");
        define("umull", _cond, bits_27_21(4), _s, _RdLo, _RdHi, _Rm, _Rs, bits_7_4(9), ne(_RdLo, _RdHi), ne(_RdLo, _Rm), ne(_RdHi, _Rm), ne(_RdHi, 15), ne(_RdLo, 15), ne(_Rm, 15), ne(_Rs, 15));
    }

    private void generateDataProcessing() {
        final Method validImmedMethod = getPredicateMethod(ARMImmediates.class, "isValidImmediate", int.class);
        final InstructionConstraint immedConstraint = new TestOnlyInstructionConstraint(makePredicate(validImmedMethod, _immediate));

        defineDataProcessingForAllAddressingModes("4.1.2", "adc", _s, _Rn, _Rd, 5, immedConstraint);
        defineDataProcessingForAllAddressingModes("4.1.3", "add", _s, _Rn, _Rd, 4, immedConstraint);
        defineDataProcessingForAllAddressingModes("4.1.4", "and", _s, _Rn, _Rd, 0, immedConstraint);
        defineDataProcessingForAllAddressingModes("4.1.6", "bic", _s, _Rn, _Rd, 14, immedConstraint);
        defineDataProcessingForAllAddressingModes("4.1.13", "cmn", s(1), _Rn, sbz_15_12(0), 11, immedConstraint);
        defineDataProcessingForAllAddressingModes("4.1.14", "cmp", s(1), _Rn, sbz_15_12(0), 10, immedConstraint);
        defineDataProcessingForAllAddressingModes("4.1.15", "eor", _s, _Rn, _Rd, 1, immedConstraint);
        defineDataProcessingForAllAddressingModes("4.1.29", "mov", _s, sbz_19_16(0), _Rd, 13, immedConstraint);
        defineDataProcessingForAllAddressingModes("4.1.34", "mvn", _s, sbz_19_16(0), _Rd, 15, immedConstraint);
        defineDataProcessingForAllAddressingModes("4.1.35", "orr", _s, _Rn, _Rd, 12, immedConstraint);
        defineDataProcessingForAllAddressingModes("4.1.36", "rsb", _s, _Rn, _Rd, 3, immedConstraint);
        defineDataProcessingForAllAddressingModes("4.1.37", "rsc", _s, _Rn, _Rd, 7, immedConstraint);
        defineDataProcessingForAllAddressingModes("4.1.38", "sbc", _s, _Rn, _Rd, 6, immedConstraint);
        defineDataProcessingForAllAddressingModes("4.1.49", "sub", _s, _Rn, _Rd, 2, immedConstraint);
        defineDataProcessingForAllAddressingModes("4.1.53", "teq", s(1), _Rn, sbz_15_12(0), 9, immedConstraint);
        defineDataProcessingForAllAddressingModes("4.1.54", "tst", s(1), _Rn, sbz_15_12(0), 8, immedConstraint);
    }

    private void generateBranch() {
        // TODO Auto-generated method stub

    }

    /**
     * Generates instruction descriptions for all 11 addressing modes of data processing instructions.
     * 
     * @param mnemonic
     *            assembly instruction mnemonic
     * @param s
     *            option field which decides whether status registers are updated or not
     * @param source
     *            first source operand
     * @param dest
     *            destination operand
     * @param opcode
     *            numeric opcode of the assembly instruction
     * @param constraints
     *            constraints on the instruction parameters
     */
    public void defineDataProcessingForAllAddressingModes(String section, String mnemonic, Object s, Object source, Object dest, int opcode, InstructionConstraint... constraints) {

        setCurrentArchitectureManualSection(section);

        define(mnemonic, _cond, bits_27_26(0), i(1), opcode(opcode), s, dest, source, ", #", _immediate, constraints, shifter_operand(encodeShifterOperand(_immediate)));
        define(mnemonic, _cond, bits_27_26(0), i(1), opcode(opcode), s, dest, source, ", #", _immed_8, ", ", _rotate_amount, even(_rotate_amount), rotate_imm(div(_rotate_amount, 2)));
        define(mnemonic, _cond, bits_27_26(0), i(0), opcode(opcode), s, dest, source, _Rm, bits_11_7(0), bits_6_4(0));

        define(mnemonic + "lsl", _cond, bits_27_26(0), i(0), opcode(opcode), s, dest, source, _Rm, ", lsl #", _shift_imm, bits_6_4(0)).setExternalName(mnemonic);
        define(mnemonic + "lsr", _cond, bits_27_26(0), i(0), opcode(opcode), s, dest, source, _Rm, ", lsr #", _shift_imm2, shift_imm(mod(_shift_imm2, 32)), bits_6_4(2)).setExternalName(mnemonic);
        define(mnemonic + "asr", _cond, bits_27_26(0), i(0), opcode(opcode), s, dest, source, _Rm, ", asr #", _shift_imm2, shift_imm(mod(_shift_imm2, 32)), bits_6_4(4)).setExternalName(mnemonic);
        define(mnemonic + "ror", _cond, bits_27_26(0), i(0), opcode(opcode), s, dest, source, _Rm, ", ror #", _shift_imm.withExcludedExternalTestArguments(_zero), bits_6_4(6)).setExternalName(mnemonic);

        define(mnemonic + "lsl", _cond, bits_27_26(0), i(0), opcode(opcode), s, dest, source, _Rm, ", lsl ", _Rs, bits_7_4(1)).setExternalName(mnemonic);
        define(mnemonic + "lsr", _cond, bits_27_26(0), i(0), opcode(opcode), s, dest, source, _Rm, ", lsr ", _Rs, bits_7_4(3)).setExternalName(mnemonic);
        define(mnemonic + "asr", _cond, bits_27_26(0), i(0), opcode(opcode), s, dest, source, _Rm, ", asr ", _Rs, bits_7_4(5)).setExternalName(mnemonic);
        define(mnemonic + "ror", _cond, bits_27_26(0), i(0), opcode(opcode), s, dest, source, _Rm, ", ror ", _Rs, bits_7_4(7)).setExternalName(mnemonic);

        define(mnemonic + "rrx", _cond, bits_27_26(0), i(0), opcode(opcode), s, dest, source, _Rm, ", rrx ", bits_11_4(6)).setExternalName(mnemonic);
    }

    /**
     * Generates instruction descriptions for first 6 out of 9 addressing modes for load and store instructions.
     * @param section
     * @param mnemonic
     * @param b
     * @param l
     */
    public void defineLoadAndStoreForAddressingModesExceptPostIndexed(String section, String mnemonic, Object b, Object l) {
        setCurrentArchitectureManualSection(section);

        //Addressing mode 1
        define(mnemonic + "add", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(0), p(1), u(1), b, w(0), l, ", #+", _offset_12, "]").setExternalName(mnemonic);
        define(mnemonic + "sub", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(0), p(1), u(0), b, w(0), l, ", #-", _offset_12.withExcludedExternalTestArguments(_zero), "]").setExternalName(mnemonic);

        //Addressing mode 2
        define(mnemonic + "add", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(1), p(1), u(1), b, w(0), l, ", +", _Rm, "]", bits_11_4(0), ne(_Rm, 15)).setExternalName(mnemonic);
        define(mnemonic + "sub", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(1), p(1), u(0), b, w(0), l, ", -", _Rm, "]", bits_11_4(0), ne(_Rm, 15)).setExternalName(mnemonic);

        //Addressing mode 3
        define(mnemonic + "addlsl", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(1), p(1), u(1), b, w(0), l, ", +", _Rm, ", lsl #", _shift_imm, "]", shift(0), bit_4(0), ne(_Rm, 15)).setExternalName(mnemonic);
        define(mnemonic + "sublsl", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(1), p(1), u(0), b, w(0), l, ", -", _Rm, ", lsl #", _shift_imm.withExcludedExternalTestArguments(_zero), "]", shift(0), bit_4(0), ne(_Rm, 15)).setExternalName(mnemonic);
        define(mnemonic + "addlsr", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(1), p(1), u(1), b, w(0), l, ", +", _Rm, ", lsr #", _shift_imm2, "]", shift_imm(mod(_shift_imm2, 32)), shift(1), bit_4(0), ne(_Rm, 15)).setExternalName(mnemonic);
        define(mnemonic + "sublsr", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(1), p(1), u(0), b, w(0), l, ", -", _Rm, ", lsr #", _shift_imm2, "]", shift_imm(mod(_shift_imm2, 32)), shift(1), bit_4(0), ne(_Rm, 15)).setExternalName(mnemonic);
        define(mnemonic + "addasr", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(1), p(1), u(1), b, w(0), l, ", +", _Rm, ", asr #", _shift_imm2, "]", shift_imm(mod(_shift_imm2, 32)), shift(2), bit_4(0), ne(_Rm, 15)).setExternalName(mnemonic);
        define(mnemonic + "subasr", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(1), p(1), u(0), b, w(0), l, ", -", _Rm, ", asr #", _shift_imm2, "]", shift_imm(mod(_shift_imm2, 32)), shift(2), bit_4(0), ne(_Rm, 15)).setExternalName(mnemonic);
        define(mnemonic + "addror", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(1), p(1), u(1), b, w(0), l, ", +", _Rm, ", ror #", _shift_imm.withExcludedExternalTestArguments(_zero), "]",
                shift(3), bit_4(0), ne(_Rm, 15)).setExternalName(mnemonic);
        define(mnemonic + "subror", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(1), p(1), u(0), b, w(0), l, ", -", _Rm, ", ror #", _shift_imm.withExcludedExternalTestArguments(_zero), "]",
                shift(3), bit_4(0), ne(_Rm, 15)).setExternalName(mnemonic);
        define(mnemonic + "addrrx", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(1), p(1), u(1), b, w(0), l, ", +", _Rm, ", rrx", "]", shift_imm(0), shift(3), bit_4(0), ne(_Rm, 15)).setExternalName(mnemonic);
        define(mnemonic + "subrrx", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(1), p(1), u(0), b, w(0), l, ", -", _Rm, ", rrx", "]", shift_imm(0), shift(3), bit_4(0), ne(_Rm, 15)).setExternalName(mnemonic);

        //Addressing mode 4
        define(mnemonic + "addw", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(0), p(1), u(1), b, w(1), l, ", #+", _offset_12, "]", "!", ne(_Rd, _Rn), ne(_Rn, 15)).setExternalName(mnemonic);
        define(mnemonic + "subw", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(0), p(1), u(0), b, w(1), l, ", #-", _offset_12.withExcludedExternalTestArguments(_zero), "]", "!", ne(_Rd, _Rn), ne(_Rn, 15)).setExternalName(mnemonic);

        //Addressing mode 5
        define(mnemonic + "addw", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(1), p(1), u(1), b, w(1), l, ", +", _Rm, "]", bits_11_4(0), "!", ne(_Rd, _Rn), ne(_Rm, _Rn), ne(_Rn, 15), ne(_Rm, 15)).setExternalName(mnemonic);
        define(mnemonic + "subw", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(1), p(1), u(0), b, w(1), l, ", -", _Rm, "]", bits_11_4(0), "!", ne(_Rd, _Rn), ne(_Rm, _Rn), ne(_Rn, 15), ne(_Rm, 15)).setExternalName(mnemonic);

        //Addressing mode 6
        define(mnemonic + "addlslw", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(1), p(1), u(1), b, w(1), l, ", +", _Rm, ", lsl #", _shift_imm, "]", "!", shift(0), bit_4(0), ne(_Rd, _Rn),
                ne(_Rm, _Rn), ne(_Rn, 15), ne(_Rm, 15)).setExternalName(mnemonic);
        define(mnemonic + "sublslw", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(1), p(1), u(0), b, w(1), l, ", -", _Rm, ", lsl #", _shift_imm.withExcludedExternalTestArguments(_zero), "]", "!", shift(0), bit_4(0), ne(_Rd, _Rn),
                ne(_Rm, _Rn), ne(_Rn, 15), ne(_Rm, 15)).setExternalName(mnemonic);
        define(mnemonic + "addlsrw", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(1), p(1), u(1), b, w(1), l, ", +", _Rm, ", lsr #", _shift_imm2, "]", "!", shift_imm(mod(_shift_imm2, 32)),
                shift(1), bit_4(0), ne(_Rd, _Rn), ne(_Rm, _Rn), ne(_Rn, 15), ne(_Rm, 15)).setExternalName(mnemonic);
        define(mnemonic + "sublsrw", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(1), p(1), u(0), b, w(1), l, ", -", _Rm, ", lsr #", _shift_imm2, "]", "!", shift_imm(mod(_shift_imm2, 32)),
                shift(1), bit_4(0), ne(_Rd, _Rn), ne(_Rm, _Rn), ne(_Rn, 15), ne(_Rm, 15)).setExternalName(mnemonic);
        define(mnemonic + "addasrw", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(1), p(1), u(1), b, w(1), l, ", +", _Rm, ", asr #", _shift_imm2, "]", "!", shift_imm(mod(_shift_imm2, 32)),
                shift(2), bit_4(0), ne(_Rd, _Rn), ne(_Rm, _Rn), ne(_Rn, 15), ne(_Rm, 15)).setExternalName(mnemonic);
        define(mnemonic + "subasrw", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(1), p(1), u(0), b, w(1), l, ", -", _Rm, ", asr #", _shift_imm2, "]", "!", shift_imm(mod(_shift_imm2, 32)),
                shift(2), bit_4(0), ne(_Rd, _Rn), ne(_Rm, _Rn), ne(_Rn, 15), ne(_Rm, 15)).setExternalName(mnemonic);
        define(mnemonic + "addrorw", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(1), p(1), u(1), b, w(1), l, ", +", _Rm, ", ror #", _shift_imm.withExcludedExternalTestArguments(_zero), "]", "!",
                shift(3), bit_4(0), ne(_Rd, _Rn), ne(_Rm, _Rn), ne(_Rn, 15), ne(_Rm, 15)).setExternalName(mnemonic);
        define(mnemonic + "subrorw", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(1), p(1), u(0), b, w(1), l, ", -", _Rm, ", ror #", _shift_imm.withExcludedExternalTestArguments(_zero), "]", "!",
                shift(3), bit_4(0), ne(_Rd, _Rn), ne(_Rm, _Rn), ne(_Rn, 15), ne(_Rm, 15)).setExternalName(mnemonic);
        define(mnemonic + "addrrxw", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(1), p(1), u(1), b, w(1), l, ", +", _Rm, ", rrx", "]", "!", shift_imm(0), shift(3), bit_4(0),
                ne(_Rd, _Rn), ne(_Rm, _Rn), ne(_Rn, 15), ne(_Rm, 15)).setExternalName(mnemonic);
        define(mnemonic + "subrrxw", _cond, bits_27_26(1), _Rd, ", [", _Rn, i(1), p(1), u(0), b, w(1), l, ", -", _Rm, ", rrx", "]", "!", shift_imm(0), shift(3), bit_4(0),
                ne(_Rd, _Rn), ne(_Rm, _Rn), ne(_Rn, 15), ne(_Rm, 15)).setExternalName(mnemonic);

    }

    /**
     * Generates instruction descriptions for last 3 post indexed addressing modes for load and store instructions.
     * @param section
     * @param mnemonic
     * @param b
     * @param l
     */
    public void defineLoadAndStoreForPostIndexedAddressingModes(String section, String mnemonic, Object b, Object l, Object w) {
        //Addressing mode 7
        define(mnemonic + "add" + "post", _cond, bits_27_26(1), _Rd, ", [", _Rn, "]", i(0), p(0), u(1), b, w, l, ", #+", _offset_12, ne(_Rn, 15)).setExternalName(mnemonic);
        define(mnemonic + "sub" + "post", _cond, bits_27_26(1), _Rd, ", [", _Rn, "]", i(0), p(0), u(0), b, w, l, ", #-", _offset_12.withExcludedExternalTestArguments(_zero), ne(_Rn, 15)).setExternalName(mnemonic);

        //Addressing mode 8
        define(mnemonic + "add" + "post", _cond, bits_27_26(1), _Rd, ", [", _Rn, "]", i(1), p(0), u(1), b, w, l, ", +", _Rm, bits_11_4(0), ne(_Rm, _Rn), ne(_Rn, 15), ne(_Rm, 15)).setExternalName(mnemonic);
        define(mnemonic + "sub" + "post", _cond, bits_27_26(1), _Rd, ", [", _Rn, "]", i(1), p(0), u(0), b, w, l, ", -", _Rm, bits_11_4(0), ne(_Rm, _Rn), ne(_Rn, 15), ne(_Rm, 15)).setExternalName(mnemonic);

        //Addressing mode 9
        define(mnemonic + "addlsl" + "post", _cond, bits_27_26(1), _Rd, ", [", _Rn, "]", i(1), p(0), u(1), b, w, l, ", +", _Rm, ", lsl #", _shift_imm, shift(0), bit_4(0), ne(_Rm, 15), ne(_Rn, 15), ne(_Rm, _Rn)).setExternalName(mnemonic);
        define(mnemonic + "sublsl" + "post", _cond, bits_27_26(1), _Rd, ", [", _Rn, "]", i(1), p(0), u(0), b, w, l, ", -", _Rm, ", lsl #", _shift_imm.withExcludedExternalTestArguments(_zero),
                shift(0), bit_4(0), ne(_Rm, 15), ne(_Rn, 15), ne(_Rm, _Rn)).setExternalName(mnemonic);
        define(mnemonic + "addlsr" + "post", _cond, bits_27_26(1), _Rd, ", [", _Rn, "]", i(1), p(0), u(1), b, w, l, ", +", _Rm, ", lsr #", _shift_imm2, shift_imm(mod(_shift_imm2, 32)),
                shift(1), bit_4(0), ne(_Rm, 15), ne(_Rn, 15), ne(_Rm, _Rn)).setExternalName(mnemonic);
        define(mnemonic + "sublsr" + "post", _cond, bits_27_26(1), _Rd, ", [", _Rn, "]", i(1), p(0), u(0), b, w, l, ", -", _Rm, ", lsr #", _shift_imm2, shift_imm(mod(_shift_imm2, 32)),
                shift(1), bit_4(0), ne(_Rm, 15), ne(_Rn, 15), ne(_Rm, _Rn)).setExternalName(mnemonic);
        define(mnemonic + "addasr" + "post", _cond, bits_27_26(1), _Rd, ", [", _Rn, "]", i(1), p(0), u(1), b, w, l, ", +", _Rm, ", asr #", _shift_imm2, shift_imm(mod(_shift_imm2, 32)),
            shift(2), bit_4(0), ne(_Rm, 15), ne(_Rn, 15), ne(_Rm, _Rn)).setExternalName(mnemonic);
        define(mnemonic + "subasr" + "post", _cond, bits_27_26(1), _Rd, ", [", _Rn, "]", i(1), p(0), u(0), b, w, l, ", -", _Rm, ", asr #", _shift_imm2, shift_imm(mod(_shift_imm2, 32)),
            shift(2), bit_4(0), ne(_Rm, 15), ne(_Rn, 15), ne(_Rm, _Rn)).setExternalName(mnemonic);
        define(mnemonic + "addror" + "post", _cond, bits_27_26(1), _Rd, ", [", _Rn, "]", i(1), p(0), u(1), b, w, l, ", +", _Rm, ", ror #", _shift_imm.withExcludedExternalTestArguments(_zero),
                shift(3), bit_4(0), ne(_Rm, 15), ne(_Rn, 15), ne(_Rm, _Rn)).setExternalName(mnemonic);
        define(mnemonic + "subror" + "post", _cond, bits_27_26(1), _Rd, ", [", _Rn, "]", i(1), p(0), u(0), b, w, l, ", -", _Rm, ", ror #", _shift_imm.withExcludedExternalTestArguments(_zero),
                shift(3), bit_4(0), ne(_Rm, 15), ne(_Rn, 15), ne(_Rm, _Rn)).setExternalName(mnemonic);
        define(mnemonic + "addrrx" + "post", _cond, bits_27_26(1), _Rd, ", [", _Rn, "]", i(1), p(0), u(1), b, w, l, ", +", _Rm, ", rrx", shift_imm(0), shift(3), bit_4(0), ne(_Rm, 15), ne(_Rn, 15), ne(_Rm, _Rn)).setExternalName(mnemonic);
        define(mnemonic + "subrrx" + "post", _cond, bits_27_26(1), _Rd, ", [", _Rn, "]", i(1), p(0), u(0), b, w, l, ", -", _Rm, ", rrx", shift_imm(0), shift(3), bit_4(0), ne(_Rm, 15), ne(_Rn, 15), ne(_Rm, _Rn)).setExternalName(mnemonic);

    }

    /**
     * Encodes the given 32 bit immediate value as an 8 bit immediate value and a 4 bit rotate value.
     * 
     * @param term
     *            the 32 bit immediate value
     */
    public static Expression encodeShifterOperand(final Object term) {
        return new Expression() {
            public long evaluate(Template template, IndexedSequence<Argument> arguments) {
                return ARMImmediates.calculateShifter((int) Static.evaluateTerm(term, template, arguments));
            }

            public String valueString() {
                return ARMImmediates.class.getSimpleName() + "." + "calculateShifter(" + Static.termValueString(term) + ")";
            }
        };
    }

}
