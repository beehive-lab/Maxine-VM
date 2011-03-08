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
package com.sun.max.vm.cps.eir.amd64;

import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.eir.amd64.AMD64EirInstruction.*;

public class AMD64EirInstructionAdapter extends EirInstructionAdapter implements AMD64EirInstructionVisitor {

    public AMD64EirInstructionAdapter() {
    }

    public void visitLocalControlTransfer(AMD64EirLocalControlTransfer localControlTransfer) {
        visitInstruction(localControlTransfer);
    }

    public void visitConditionalBranch(AMD64EirConditionalBranch conditionalBranch) {
        visitLocalControlTransfer(conditionalBranch);
    }

    public void visitOperation(AMD64EirOperation operation) {
        visitInstruction(operation);
    }

    public void visitUnaryOperation(AMD64EirUnaryOperation unaryOperation) {
        visitOperation(unaryOperation);
    }

    public void visitBinaryOperation(AMD64EirBinaryOperation binaryOperation) {
        visitUnaryOperation(binaryOperation);
    }

    public void visitPointerOperation(AMD64EirPointerOperation pointerOperation) {
        visitBinaryOperation(pointerOperation);
    }

    public void visitArithmetic(AMD64EirBinaryOperation.Arithmetic arithmetic) {
        visitBinaryOperation(arithmetic);
    }

    public void visitGeneral(AMD64EirBinaryOperation.Arithmetic.General general) {
        visitArithmetic(general);
    }

    public void visitRA(AMD64EirBinaryOperation.Arithmetic.General.RA ra) {
        visitGeneral(ra);
    }

    public void visitShift(AMD64EirBinaryOperation.Arithmetic.Shift shift) {
        visitArithmetic(shift);
    }

    public void visitXMM(AMD64EirBinaryOperation.Arithmetic.XMM xmm) {
        visitArithmetic(xmm);
    }

    public void visitXMM128(AMD64EirBinaryOperation.Arithmetic.XMM128 xmm) {
        visitArithmetic(xmm);
    }

    public void visitMove(AMD64EirBinaryOperation.Move move) {
        visitBinaryOperation(move);
    }

    public void visitGeneralToGeneral(AMD64EirBinaryOperation.Move.GeneralToGeneral generalToGeneral) {
        visitMove(generalToGeneral);
    }

    public void visitGeneralToXMM(AMD64EirBinaryOperation.Move.GeneralToXMM generalToXMM) {
        visitMove(generalToXMM);
    }

    public void visitXMMToGeneral(AMD64EirBinaryOperation.Move.XMMToGeneral xmmToGeneral) {
        visitMove(xmmToGeneral);
    }

    public void visitXMMToXMM(AMD64EirBinaryOperation.Move.XMMToXMM xmmToXMM) {
        visitMove(xmmToXMM);
    }

    public void visitDivision(AMD64EirDivision division) {
        visitUnaryOperation(division);
    }

    /*
     * The actual instructions:
     */

    public void visit(AMD64EirLoad instruction) {
        visitPointerOperation(instruction);
    }

    public void visit(AMD64EirStore instruction) {
        visitPointerOperation(instruction);
    }

    public void visit(AMD64EirCompareAndSwap instruction) {
        visitPointerOperation(instruction);
    }

    public void visit(ADD_I32 instruction) {
        visitRA(instruction);
    }

    public void visit(ADD_I64 instruction) {
        visitRA(instruction);
    }

    public void visit(ADDSD instruction) {
        visitXMM(instruction);
    }

    public void visit(ADDSS instruction) {
        visitXMM(instruction);
    }

    public void visit(AND_I32 instruction) {
        visitRA(instruction);
    }

    public void visit(AND_I64 instruction) {
        visitRA(instruction);
    }

    public void visit(BSF_I64 instruction) {
        visitArithmetic(instruction);
    }

    public void visit(BSR_I64 instruction) {
        visitArithmetic(instruction);
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

    public void visit(CMOVAE_I32 instruction) {
        visitMove(instruction);
    }

    public void visit(CMOVL_I32 instruction) {
        visitMove(instruction);
    }

    public void visit(CMOVBE_I32 instruction) {
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

    public void visit(CQO instruction) {
        visitMove(instruction);
    }

    public void visit(CVTTSD2SI_I32 instruction) {
        visitXMMToGeneral(instruction);
    }

    public void visit(CVTTSD2SI_I64 instruction) {
        visitXMMToGeneral(instruction);
    }

    public void visit(CVTSD2SS instruction) {
        visitXMMToXMM(instruction);
    }

    public void visit(CVTSI2SD_I32 instruction) {
        visitGeneralToXMM(instruction);
    }

    public void visit(CVTSI2SD_I64 instruction) {
        visitGeneralToXMM(instruction);
    }

    public void visit(CVTSI2SS_I32 instruction) {
        visitGeneralToXMM(instruction);
    }

    public void visit(CVTSI2SS_I64 instruction) {
        visitGeneralToXMM(instruction);
    }

    public void visit(CVTSS2SD instruction) {
        visitXMMToXMM(instruction);
    }

    public void visit(CVTTSS2SI_I32 instruction) {
        visitXMMToGeneral(instruction);
    }

    public void visit(CVTTSS2SI_I64 instruction) {
        visitXMMToGeneral(instruction);
    }

    public void visit(DEC_I64 instruction) {
        visitInstruction(instruction);
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

    public void visit(IDIV_I32 instruction) {
        visitDivision(instruction);
    }

    public void visit(IDIV_I64 instruction) {
        visitDivision(instruction);
    }

    public void visit(IMUL_I32 instruction) {
        visitArithmetic(instruction);
    }

    public void visit(IMUL_I64 instruction) {
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

    public void visit(LEA_STACK_ADDRESS instruction) {
        visitBinaryOperation(instruction);
    }

    public void visit(STACK_ALLOCATE instruction) {
        visitUnaryOperation(instruction);
    }

    public void visit(LFENCE instruction) {
        visitOperation(instruction);
    }

    public void visit(MFENCE instruction) {
        visitOperation(instruction);
    }

    public void visit(PAUSE instruction) {
        visitOperation(instruction);
    }

    public void visit(MOVD_I32_F32 instruction) {
        visitMove(instruction);
    }

    public void visit(MOVD_I64_F64 instruction) {
        visitMove(instruction);
    }

    public void visit(MOVD_F32_I32 instruction) {
        visitMove(instruction);
    }

    public void visit(MOVD_F64_I64 instruction) {
        visitMove(instruction);
    }

    public void visit(MOVSX_I8 instruction) {
        visitMove(instruction);
    }

    public void visit(MOVSX_I16 instruction) {
        visitMove(instruction);
    }

    public void visit(MOVSXD instruction) {
        visitMove(instruction);
    }

    public void visit(MOVZX_I16 instruction) {
        visitMove(instruction);
    }

    public void visit(MOVZXD instruction) {
        visitMove(instruction);
    }

    public void visit(MULSD instruction) {
        visitXMM(instruction);
    }

    public void visit(MULSS instruction) {
        visitXMM(instruction);
    }

    public void visit(NEG_I32 instruction) {
        visitUnaryOperation(instruction);
    }

    public void visit(NEG_I64 instruction) {
        visitUnaryOperation(instruction);
    }

    public void visit(NOT_I32 instruction) {
        visitUnaryOperation(instruction);
    }

    public void visit(NOT_I64 instruction) {
        visitUnaryOperation(instruction);
    }

    public void visit(OR_I32 instruction) {
        visitRA(instruction);
    }

    public void visit(OR_I64 instruction) {
        visitRA(instruction);
    }

    public void visit(POP instruction) {
        visitInstruction(instruction);
    }

    public void visit(PUSH instruction) {
        visitInstruction(instruction);
    }

    public void visit(RET instruction) {
        visitInstruction(instruction);
    }

    public void visit(SETB instruction) {
        visitInstruction(instruction);
    }

    public void visit(SETBE instruction) {
        visitInstruction(instruction);
    }

    public void visit(SETL instruction) {
        visitInstruction(instruction);
    }

    public void visit(SETNB instruction) {
        visitInstruction(instruction);
    }

    public void visit(SETNBE instruction) {
        visitInstruction(instruction);
    }

    public void visit(SETNLE instruction) {
        visitInstruction(instruction);
    }

    public void visit(SETNP instruction) {
        visitInstruction(instruction);
    }

    public void visit(SETP instruction) {
        visitInstruction(instruction);
    }

    public void visit(SAL_I32 instruction) {
        visitShift(instruction);
    }

    public void visit(SAL_I64 instruction) {
        visitShift(instruction);
    }

    public void visit(SAR_I32 instruction) {
        visitShift(instruction);
    }

    public void visit(SAR_I64 instruction) {
        visitShift(instruction);
    }

    public void visit(SFENCE instruction) {
        visitOperation(instruction);
    }

    public void visit(SHR_I32 instruction) {
        visitShift(instruction);
    }

    public void visit(SHR_I64 instruction) {
        visitShift(instruction);
    }

    public void visit(SUB_I32 instruction) {
        visitRA(instruction);
    }

    public void visit(SUB_I64 instruction) {
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

    public void visit(XCHG instruction) {
        visitBinaryOperation(instruction);
    }

    public void visit(XOR_I32 instruction) {
        visitRA(instruction);
    }

    public void visit(XOR_I64 instruction) {
        visitRA(instruction);
    }

    public void visit(XORPD instruction) {
        visitXMM128(instruction);
    }

    public void visit(XORPS instruction) {
        visitXMM128(instruction);
    }

    public void visit(ZERO instruction) {
        visitInstruction(instruction);
    }

}
