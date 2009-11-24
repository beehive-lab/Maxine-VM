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
package com.sun.max.vm.compiler.eir.sparc;

import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.sparc.SPARCEirInstruction.*;

/**
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public interface SPARCEirInstructionVisitor extends EirInstructionVisitor {
    void visit(SPARCEirLoad instruction);
    void visit(SPARCEirStore instruction);
    void visit(SPARCEirCompareAndSwap instruction);

    void visit(ADD_I32 instruction);
    void visit(ADD_I64 instruction);
    void visit(AND_I32 instruction);
    void visit(AND_I64 instruction);

    void visit(MOVNE instruction);
    void visit(MOVE instruction);
    void visit(MOVG instruction);
    void visit(MOVCC instruction);
    void visit(MOVL instruction);
    void visit(MOVCS instruction);
    void visit(MOVLEU instruction);
    void visit(MOVGU instruction);
    void visit(MOVFL instruction);
    void visit(MOVFG instruction);
    void visit(MOVFE instruction);
    void visit(MOVFNE instruction);

    void visit(CMP_I32 instruction);
    void visit(CMP_I64 instruction);
    void visit(DIV_I32 instruction);
    void visit(DIV_I64 instruction);

    void visit(BA instruction);
    void visit(BRZ instruction);
    void visit(BRNZ instruction);
    void visit(BRLZ instruction);
    void visit(BRLEZ instruction);
    void visit(BRGEZ instruction);
    void visit(BRGZ instruction);

    void visit(BE instruction);
    void visit(BNE instruction);
    void visit(BL instruction);
    void visit(BLE instruction);
    void visit(BGE instruction);
    void visit(BG instruction);
    void visit(BLU instruction);
    void visit(BLEU instruction);
    void visit(BGEU instruction);
    void visit(BGU instruction);

    void visit(FLAT_RETURN instruction);

    void visit(FADD_S instruction);
    void visit(FADD_D instruction);
    void visit(FCMP_S instruction);
    void visit(FCMP_D instruction);
    void visit(FDIV_S instruction);
    void visit(FDIV_D instruction);
    void visit(FMUL_S instruction);
    void visit(FMUL_D instruction);
    void visit(FNEG_S instruction);
    void visit(FNEG_D instruction);
    void visit(FSUB_S instruction);
    void visit(FSUB_D instruction);
    void visit(FDTOS instruction);
    void visit(FSTOD instruction);
    void visit(FITOS instruction);
    void visit(FITOD instruction);
    void visit(FXTOS instruction);
    void visit(FXTOD instruction);
    void visit(FSTOI instruction);
    void visit(FDTOI instruction);
    void visit(FSTOX instruction);
    void visit(FDTOX instruction);

    void visit(FLUSHW instruction);

    void visit(JMP_indirect instruction);
    void visit(MEMBAR instruction);
    void visit(MOV_I32 instruction);
    void visit(MOV_I64 instruction);
    void visit(MUL_I32 instruction);
    void visit(MUL_I64 instruction);

    void visit(NEG_I32 instruction);
    void visit(NEG_I64 instruction);
    void visit(NOT_I32 instruction);
    void visit(NOT_I64 instruction);
    void visit(OR_I32 instruction);
    void visit(OR_I64 instruction);

    void visit(RDPC instruction);
    void visit(RET instruction);

    void visit(SET_I32 instruction);
    void visit(SET_STACK_ADDRESS instruction);
    void visit(STACK_ALLOCATE instruction);
    void visit(SLL_I32 instruction);
    void visit(SLL_I64 instruction);
    void visit(SRA_I32 instruction);
    void visit(SRA_I64 instruction);
    void visit(SRL_I32 instruction);
    void visit(SRL_I64 instruction);
    void visit(SUB_I32 instruction);
    void visit(SUB_I64 instruction);
    void visit(SWITCH_I32 instruction);
    void visit(XOR_I32 instruction);
    void visit(XOR_I64 instruction);
    void visit(ZERO instruction);
}
