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
/*VCSID=3008c5ca-20d7-4d60-8e47-23fc74b83dd3*/
package com.sun.max.vm.compiler.eir.ia32;

import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.ia32.IA32EirInstruction.*;

/**
 * @author Bernd Mathiske
 */
public interface IA32EirInstructionVisitor extends EirInstructionVisitor {
    void visit(IA32EirLoad instruction);
    void visit(IA32EirStore instruction);
    void visit(IA32EirCompareAndSwap instruction);

    void visit(ADD instruction);
    void visit(ADDSD instruction);
    void visit(ADDSS instruction);
    void visit(AND instruction);
    void visit(CDQ instruction);
    void visit(CMOVA_I32 instruction);
    void visit(CMOVB_I32 instruction);
    void visit(CMOVE_I32 instruction);
    void visit(CMOVG_I32 instruction);
    void visit(CMOVGE_I32 instruction);
    void visit(CMOVL_I32 instruction);
    void visit(CMOVLE_I32 instruction);
    void visit(CMOVP_I32 instruction);
    void visit(CMP_I32 instruction);
    void visit(CMP_I64 instruction);
    void visit(CMPSD instruction);
    void visit(CMPSS instruction);
    void visit(COMISD instruction);
    void visit(COMISS instruction);
    void visit(CVTSD2SI_I32 instruction);
    void visit(CVTSD2SI_I64 instruction);
    void visit(CVTSD2SS instruction);
    void visit(CVTSI2SD instruction);
    void visit(CVTSI2SS_I32 instruction);
    void visit(CVTSS2SD instruction);
    void visit(CVTSS2SI instruction);
    void visit(DIV_I64 instruction);
    void visit(DIVSD instruction);
    void visit(DIVSS instruction);
    void visit(IDIV instruction);
    void visit(IMUL instruction);
    void visit(JMP instruction);
    void visit(JMP_indirect instruction);
    void visit(JA instruction);
    void visit(JAE instruction);
    void visit(JB instruction);
    void visit(JBE instruction);
    void visit(JG instruction);
    void visit(JGE instruction);
    void visit(JL instruction);
    void visit(JLE instruction);
    void visit(JNZ instruction);
    void visit(JZ instruction);
    void visit(LEA_PC instruction);
    void visit(LEA_STACK_ADDRESS instruction);
    void visit(LFENCE instruction);
    void visit(MFENCE instruction);
    void visit(MOVD instruction);
    void visit(MOVSD instruction);
    void visit(MOVSX_I8 instruction);
    void visit(MOVSX_I16 instruction);
    void visit(MOVZX_I16 instruction);
    void visit(MULSD instruction);
    void visit(MULSS instruction);
    void visit(NEG instruction);
    void visit(NOT instruction);
    void visit(OR instruction);
    void visit(PADDQ instruction);
    void visit(PAND instruction);
    void visit(PANDN instruction);
    void visit(PCMPEQD instruction);
    void visit(POR instruction);
    void visit(PSLLQ instruction);
    void visit(PSRLQ instruction);
    void visit(PSUBQ instruction);
    void visit(PXOR instruction);
    void visit(RET instruction);
    void visit(SAL instruction);
    void visit(SAR instruction);
    void visit(SFENCE instruction);
    void visit(SHR instruction);
    void visit(STORE_HIGH instruction);
    void visit(STORE_LOW instruction);
    void visit(SUB instruction);
    void visit(SUBSD instruction);
    void visit(SUBSS instruction);
    void visit(SWITCH_I32 instruction);
    void visit(XOR instruction);
    void visit(XORPD instruction);
    void visit(XORPS instruction);
}
