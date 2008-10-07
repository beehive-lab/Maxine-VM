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
package com.sun.max.vm.compiler.eir.ia32;

import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.ia32.IA32EirInstruction.*;

public class IA32EirInstructionAdapter extends EirInstructionAdapter implements IA32EirInstructionVisitor {

    public IA32EirInstructionAdapter() {
    }

    public void visitLocalControlTransfer(IA32EirLocalControlTransfer localControlTransfer) {
        visitInstruction(localControlTransfer);
    }

    public void visitConditionalBranch(IA32EirConditionalBranch conditionalBranch) {
        visitLocalControlTransfer(conditionalBranch);
    }

    public void visitOperation(IA32EirOperation operation) {
        visitInstruction(operation);
    }

    public void visitUnaryOperation(IA32EirUnaryOperation unaryOperation) {
        visitOperation(unaryOperation);
    }

    public void visitBinaryOperation(IA32EirBinaryOperation binaryOperation) {
        visitUnaryOperation(binaryOperation);
    }

    public void visitPointerOperation(IA32EirPointerOperation pointerOperation) {
        visitBinaryOperation(pointerOperation);
    }

    public void visitArithmetic(IA32EirBinaryOperation.Arithmetic arithmetic) {
        visitBinaryOperation(arithmetic);
    }

    public void visitGeneral(IA32EirBinaryOperation.Arithmetic.General general) {
        visitArithmetic(general);
    }

    public void visitRA(IA32EirBinaryOperation.Arithmetic.General.RA ra) {
        visitGeneral(ra);
    }

    public void visitShiftGeneral(IA32EirBinaryOperation.Arithmetic.ShiftGeneral shift) {
        visitArithmetic(shift);
    }

    public void visitShiftXMM(IA32EirBinaryOperation.Arithmetic.ShiftXMM shift) {
        visitArithmetic(shift);
    }

    public void visitXMM(IA32EirBinaryOperation.Arithmetic.XMM xmm) {
        visitArithmetic(xmm);
    }

    public void visitXMM128(IA32EirBinaryOperation.Arithmetic.XMM128 xmm) {
        visitArithmetic(xmm);
    }

    public void visitMove(IA32EirBinaryOperation.Move move) {
        visitBinaryOperation(move);
    }

    public void visitGeneralToGeneral(IA32EirBinaryOperation.Move.GeneralToGeneral generalToGeneral) {
        visitMove(generalToGeneral);
    }

    public void visitGeneralToXMM(IA32EirBinaryOperation.Move.GeneralToXMM generalToXMM) {
        visitMove(generalToXMM);
    }

    public void visitXMMToGeneral(IA32EirBinaryOperation.Move.XMMToGeneral xmmToGeneral) {
        visitMove(xmmToGeneral);
    }

    public void visitXMMToXMM(IA32EirBinaryOperation.Move.XMMToXMM xmmToXMM) {
        visitMove(xmmToXMM);
    }

    public void visitDivision(IA32EirDivision division) {
        visitUnaryOperation(division);
    }

    /*
     * The actual instructions:
     */

    public void visit(IA32EirLoad instruction) {
        visitPointerOperation(instruction);
    }

    public void visit(IA32EirStore instruction) {
        visitPointerOperation(instruction);
    }

    public void visit(IA32EirCompareAndSwap instruction) {
        visitPointerOperation(instruction);
    }

    public void visit(ADD instruction) {
        visitRA(instruction);
    }

    public void visit(ADDSD instruction) {
        visitXMM(instruction);
    }

    public void visit(ADDSS instruction) {
        visitXMM(instruction);
    }

    public void visit(AND instruction) {
        visitRA(instruction);
    }

    public void visit(CDQ instruction) {
        visitMove(instruction);
    }

    public void visit(CMOVA_I32 instruction) {
        visitMove(instruction);
    }

    public void visit(CMOVB_I32 instruction) {
        visitMove(instruction);
    }

    public void visit(CMOVE_I32 instruction) {
        visitMove(instruction);
    }

    public void visit(CMOVG_I32 instruction) {
        visitMove(instruction);
    }

    public void visit(CMOVGE_I32 instruction) {
        visitMove(instruction);
    }

    public void visit(CMOVL_I32 instruction) {
        visitMove(instruction);
    }

    public void visit(CMOVLE_I32 instruction) {
        visitMove(instruction);
    }

    public void visit(CMOVP_I32 instruction) {
        visitMove(instruction);
    }

    public void visit(CMP_I32 instruction) {
        visitRA(instruction);
    }

    public void visit(CMP_I64 instruction) {
        visitRA(instruction);
    }

    public void visit(CMPSD instruction) {
        visitXMM(instruction);
    }

    public void visit(CMPSS instruction) {
        visitXMM(instruction);
    }

    public void visit(COMISD instruction) {
        visitXMM(instruction);
    }

    public void visit(COMISS instruction) {
        visitXMM(instruction);
    }

    public void visit(CVTSD2SI_I32 instruction) {
        visitXMMToGeneral(instruction);
    }

    public void visit(CVTSD2SI_I64 instruction) {
        visitXMMToGeneral(instruction);
    }

    public void visit(CVTSD2SS instruction) {
        visitXMMToXMM(instruction);
    }

    public void visit(CVTSI2SD instruction) {
        visitGeneralToXMM(instruction);
    }

    public void visit(CVTSI2SS_I32 instruction) {
        visitGeneralToXMM(instruction);
    }

    public void visit(CVTSS2SD instruction) {
        visitXMMToXMM(instruction);
    }

    public void visit(CVTSS2SI instruction) {
        visitXMMToGeneral(instruction);
    }

    public void visit(DIV_I64 instruction) {
        visitDivision(instruction);
    }

    public void visit(DIVSD instruction) {
        visitXMM(instruction);
    }

    public void visit(DIVSS instruction) {
        visitXMM(instruction);
    }

    public void visit(IDIV instruction) {
        visitDivision(instruction);
    }

    public void visit(IMUL instruction) {
        visitArithmetic(instruction);
    }

    public void visit(JMP instruction) {
        visitLocalControlTransfer(instruction);
    }

    public void visit(JMP_indirect instruction) {
        visitInstruction(instruction);
    }

    public void visit(JA instruction) {
        visitConditionalBranch(instruction);
    }

    public void visit(JAE instruction) {
        visitConditionalBranch(instruction);
    }

    public void visit(JB instruction) {
        visitConditionalBranch(instruction);
    }

    public void visit(JBE instruction) {
        visitConditionalBranch(instruction);
    }

    public void visit(JG instruction) {
        visitConditionalBranch(instruction);
    }

    public void visit(JGE instruction) {
        visitConditionalBranch(instruction);
    }

    public void visit(JL instruction) {
        visitConditionalBranch(instruction);
    }

    public void visit(JLE instruction) {
        visitConditionalBranch(instruction);
    }

    public void visit(JNZ instruction) {
        visitConditionalBranch(instruction);
    }

    public void visit(JZ instruction) {
        visitConditionalBranch(instruction);
    }

    public void visit(LEA_PC instruction) {
        visitUnaryOperation(instruction);
    }

    public void visit(LEA_STACK_ADDRESS instruction) {
        visitBinaryOperation(instruction);
    }

    public void visit(LFENCE instruction) {
        visitOperation(instruction);
    }

    public void visit(MFENCE instruction) {
        visitOperation(instruction);
    }

    public void visit(MOVD instruction) {
        visitMove(instruction);
    }

    public void visit(MOVSD instruction) {
        visitXMMToXMM(instruction);
    }

    public void visit(MOVSX_I8 instruction) {
        visitMove(instruction);
    }

    public void visit(MOVSX_I16 instruction) {
        visitMove(instruction);
    }

    public void visit(MOVZX_I16 instruction) {
        visitMove(instruction);
    }

    public void visit(MULSD instruction) {
        visitXMM(instruction);
    }

    public void visit(MULSS instruction) {
        visitXMM(instruction);
    }

    public void visit(NEG instruction) {
        visitUnaryOperation(instruction);
    }

    public void visit(NOT instruction) {
        visitUnaryOperation(instruction);
    }

    public void visit(OR instruction) {
        visitRA(instruction);
    }

    public void visit(PADDQ instruction) {
        visitXMM128(instruction);
    }

    public void visit(PAND instruction) {
        visitXMM128(instruction);
    }

    public void visit(PANDN instruction) {
        visitXMM128(instruction);
    }

    public void visit(PCMPEQD instruction) {
        visitBinaryOperation(instruction);
    }

    public void visit(POR instruction) {
        visitXMM128(instruction);
    }

    public void visit(PSLLQ instruction) {
        visitShiftXMM(instruction);
    }

    public void visit(PSRLQ instruction) {
        visitShiftXMM(instruction);
    }

    public void visit(PSUBQ instruction) {
        visitXMM128(instruction);
    }

    public void visit(PXOR instruction) {
        visitXMM128(instruction);
    }

    public void visit(RET instruction) {
        visitInstruction(instruction);
    }

    public void visit(SAL instruction) {
        visitShiftGeneral(instruction);
    }

    public void visit(SAR instruction) {
        visitShiftGeneral(instruction);
    }

    public void visit(SFENCE instruction) {
        visitOperation(instruction);
    }

    public void visit(SHR instruction) {
        visitShiftGeneral(instruction);
    }

    public void visit(STORE_HIGH instruction) {
        visitBinaryOperation(instruction);
    }

    public void visit(STORE_LOW instruction) {
        visitBinaryOperation(instruction);
    }

    public void visit(SUB instruction) {
        visitRA(instruction);
    }

    public void visit(SUBSD instruction) {
        visitXMM(instruction);
    }

    public void visit(SUBSS instruction) {
        visitXMM(instruction);
    }

    public void visit(SWITCH_I32 instruction) {
        visitInstruction(instruction);
    }

    public void visit(XOR instruction) {
        visitRA(instruction);
    }

    public void visit(XORPD instruction) {
        visitXMM128(instruction);
    }

    public void visit(XORPS instruction) {
        visitXMM128(instruction);
    }

}
