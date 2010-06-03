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
package com.sun.max.vm.cps.eir.sparc;

import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.eir.sparc.SPARCEirInstruction.*;

/**
 * Adapter for SPARCEirInstructionVisitor.
 * All visit methods do nothing.
 *
 * @author Laurent Daynes
 */
public class SPARCEirInstructionAdapter extends EirInstructionAdapter implements SPARCEirInstructionVisitor {

    public SPARCEirInstructionAdapter() {
    }

    public void visit(SPARCEirAssignment instruction) {
    }

    public void visit(SPARCEirLoad instruction) {
    }

    public void visit(SPARCEirStore instruction) {
    }

    public void visit(SPARCEirCompareAndSwap instruction) {
    }

    public void visit(ADD_I32 instruction) {
    }

    public void visit(ADD_I64 instruction) {
    }

    public void visit(AND_I32 instruction) {
    }

    public void visit(AND_I64 instruction) {
    }

    public void visit(BA instruction) {
    }

    public void visit(BRZ instruction) {
    }

    public void visit(BRNZ instruction) {
    }

    public void visit(BRLZ instruction) {
    }

    public void visit(BRLEZ instruction) {
    }

    public void visit(BRGEZ instruction) {
    }

    public void visit(BRGZ instruction) {
    }

    public void visit(BE instruction) {
    }

    public void visit(BNE instruction) {
    }

    public void visit(BL instruction) {
    }

    public void visit(BLE instruction) {
    }

    public void visit(BGE instruction) {
    }

    public void visit(BG instruction) {
    }

    public void visit(BLU instruction) {
    }

    public void visit(BLEU instruction) {
    }

    public void visit(BGEU instruction) {
    }

    public void visit(BGU instruction) {
    }

    public void visit(MOVNE instruction) {
    }

    public void visit(MOVE instruction) {
    }

    public void visit(MOVG instruction) {
    }

    public void visit(MOVCC instruction) {
    }

    public void visit(MOVL instruction) {
    }

    public void visit(MOVFL instruction) {
    }

    public void visit(MOVFG instruction) {
    }

    public void visit(MOVCS instruction) {
    }

    public void visit(MOVGU instruction) {
    }

    public void visit(MOVFE instruction) {
    }

    public void visit(MOVFNE instruction) {
    }

    public void visit(MOVLEU instruction) {
    }

    public void visit(CMP_I32 instruction) {
    }

    public void visit(CMP_I64 instruction) {
    }

    public void visit(DIV_I32 instruction) {
    }

    public void visit(DIV_I64 instruction) {
    }

    public void visit(FADD_S instruction) {
    }

    public void visit(FADD_D instruction) {
    }

    public void visit(FCMP_S  instruction) {
    }

    public void visit(FCMP_D  instruction) {
    }

    public void visit(FDIV_S instruction) {
    }

    public void visit(FDIV_D instruction) {
    }

    public void visit(FLAT_RETURN instruction) {
    }

    public void visit(FMUL_S instruction) {
    }

    public void visit(FMUL_D instruction) {
    }

    public void visit(FNEG_S instruction) {
    }

    public void visit(FNEG_D instruction) {
    }

    public void visit(FSUB_S instruction) {
    }

    public void visit(FSUB_D instruction) {
    }

    public void visit(FSTOD instruction) {
    }

    public void visit(FDTOS instruction) {
    }

    public void visit(FITOS instruction) {
    }

    public void visit(FITOD instruction) {
    }

    public void visit(FXTOS instruction) {
    }

    public void visit(FXTOD instruction) {
    }

    public void visit(FSTOI instruction) {
    }

    public void visit(FDTOI instruction) {
    }

    public void visit(FSTOX instruction) {
    }

    public void visit(FDTOX instruction) {
    }

    public void visit(FLUSHW instruction) {
    }

    public void visit(JMP_indirect instruction) {
    }

    public void visit(MEMBAR instruction) {
    }

    public void visit(MOV_I32 instruction) {
    }

    public void visit(MOV_I64 instruction) {
    }

    public void visit(MUL_I32 instruction) {
    }

    public void visit(MUL_I64 instruction) {
    }

    public void visit(NEG_I32 instruction) {
    }

    public void visit(NEG_I64 instruction) {
    }

    public void visit(NOT_I32 instruction) {
    }

    public void visit(NOT_I64 instruction) {
    }

    public void visit(OR_I32 instruction) {
    }

    public void visit(OR_I64 instruction) {
    }

    public void visit(RDPC instruction) {
    }

    public void visit(RET instruction) {
    }

    public void visit(SET_STACK_ADDRESS instruction) {
    }

    public void visit(STACK_ALLOCATE instruction) {
    }

    public void visit(SET_I32 instruction) {
    }

    public void visit(SLL_I32 instruction) {
    }

    public void visit(SLL_I64 instruction) {
    }

    public void visit(SRA_I32 instruction) {
    }

    public void visit(SRA_I64 instruction) {
    }

    public void visit(SRL_I32 instruction) {
    }

    public void visit(SRL_I64 instruction) {
    }

    public void visit(SUB_I32 instruction) {
    }

    public void visit(SUB_I64 instruction) {
    }

    public void visit(SWITCH_I32 instruction) {
    }

    public void visit(XOR_I32 instruction) {
    }

    public void visit(XOR_I64 instruction) {
    }

    public void visit(ZERO instruction) {
    }
}
