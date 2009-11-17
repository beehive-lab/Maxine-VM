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
package com.sun.max.vm.compiler.b.c.d.e.sparc;

import com.sun.max.program.*;
import com.sun.max.vm.compiler.dir.*;
import com.sun.max.vm.compiler.dir.eir.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.sparc.SPARCEirInstruction.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 *
 * @author Laurent Daynes
 */
public final class DirToSPARCEirInstructionTranslation extends DirToEirInstructionTranslation {

    public DirToSPARCEirInstructionTranslation(DirToEirMethodTranslation methodTranslation, EirBlock eirBlock) {
        super(methodTranslation, eirBlock);
    }

    @Override
    public DirToEirBuiltinTranslation createBuiltinTranslation(DirToEirInstructionTranslation dirToEirInstructionTranslation, DirJavaFrameDescriptor javaFrameDescriptor) {
        return new DirToSPARCEirBuiltinTranslation(this, javaFrameDescriptor);
    }

    @Override
    public void assignZero(Kind kind, EirValue variable) {
        if (kind == Kind.FLOAT || kind == Kind.DOUBLE) {
            assign(kind, variable, createEirConstant(kind.zeroValue()));
        } else {
            addInstruction(new ZERO(eirBlock(), kind, variable));
        }
    }

    private EirBlock translateCompareZeroAndBranch(DirSwitch dirSwitch, EirValue tag, EirBlock targetBlock,  EirBlock nextBlock) {
        switch (dirSwitch.valueComparator()) {
            case EQUAL:
                addInstruction(new BRZ(eirBlock(), targetBlock, nextBlock, tag));
                break;
            case NOT_EQUAL:
                addInstruction(new BRNZ(eirBlock(), targetBlock, nextBlock, tag));
                break;
            case LESS_THAN:
                addInstruction(new BRLZ(eirBlock(), targetBlock, nextBlock, tag));
                break;
            case LESS_EQUAL:
                addInstruction(new BRLEZ(eirBlock(), targetBlock, nextBlock, tag));
                break;
            case GREATER_EQUAL:
                addInstruction(new BRGEZ(eirBlock(), targetBlock, nextBlock, tag));
                break;
            case GREATER_THAN:
                addInstruction(new BRGZ(eirBlock(), targetBlock, nextBlock, tag));
                break;
            default:
                break;
        }
        return nextBlock;
    }

    private boolean canUseBranchOnRegister(DirValue match, ValueComparator comparator, Kind comparisonKind) {
        // Branch on register value examines the 64-bits of the register. So it must be used only when the value is 64-bits value.
        // It could be used for 32-bits value or lower, if and only if the higher-bits have been cleared before hand.
        // The caller should arrange for this to be the case and passed the appropriate comparisonKind in that case.
        return match.isZeroConstant() &&
            (comparator == ValueComparator.EQUAL || comparator == ValueComparator.NOT_EQUAL) &&
            (comparisonKind == Kind.LONG || comparisonKind == Kind.WORD);
    }

    private EirBlock translateConditionalBranch(DirSwitch dirSwitch, int index) {
        final EirValue tag = dirToEirValue(dirSwitch.tag());
        final EirBlock targetBlock = dirToEirBlock(dirSwitch.targetBlocks()[index]);
        final EirBlock nextBlock = (index < (dirSwitch.targetBlocks().length - 1)) ? createEirBlock(IrBlock.Role.NORMAL) : dirToEirBlock(dirSwitch.defaultTargetBlock());
        final Kind kind = dirSwitch.comparisonKind();
        final DirValue dirMatch = dirSwitch.matches()[index];

        if (canUseBranchOnRegister(dirMatch, dirSwitch.valueComparator(), kind)) {
            return translateCompareZeroAndBranch(dirSwitch, tag, targetBlock, nextBlock);
        }
        final EirValue match = dirToEirValue(dirSwitch.matches()[index]);
        if (kind == Kind.INT) {
            addInstruction(new CMP_I32(eirBlock(), tag, match));
        } else {
            addInstruction(new CMP_I64(eirBlock(), tag, match));
        }
        switch (dirSwitch.valueComparator()) {
            case EQUAL:
                addInstruction(new BE(eirBlock(), targetBlock, nextBlock, kind));
                break;
            case NOT_EQUAL:
                addInstruction(new BNE(eirBlock(), targetBlock, nextBlock, kind));
                break;
            case UNSIGNED_LESS_THAN:
                addInstruction(new BLU(eirBlock(), targetBlock, nextBlock, kind));
                break;
            case UNSIGNED_LESS_EQUAL:
                addInstruction(new BLEU(eirBlock(), targetBlock, nextBlock, kind));
                break;
            case UNSIGNED_GREATER_EQUAL:
                addInstruction(new BGEU(eirBlock(), targetBlock, nextBlock, kind));
                break;
            case UNSIGNED_GREATER_THAN:
                addInstruction(new BGU(eirBlock(), targetBlock, nextBlock, kind));
                break;
            case LESS_THAN:
                addInstruction(new BL(eirBlock(), targetBlock, nextBlock, kind));
                break;
            case LESS_EQUAL:
                addInstruction(new BLE(eirBlock(), targetBlock, nextBlock, kind));
                break;
            case GREATER_EQUAL:
                addInstruction(new BGE(eirBlock(), targetBlock, nextBlock, kind));
                break;
            case GREATER_THAN:
                addInstruction(new BG(eirBlock(), targetBlock, nextBlock, kind));
                break;
        }

        return nextBlock;
    }

    private void translateSwitch(DirSwitch dirSwitch) {
        if (!dirSwitch.comparisonKind().equals(Kind.INT)) {
            ProgramError.unexpected("only 32-bit keys are supported for SWITCH statements: " + dirSwitch.comparisonKind());
        }

        final EirValue tag = dirToEirValue(dirSwitch.tag());
        final EirBlock defaultTargetBlock = dirToEirBlock(dirSwitch.defaultTargetBlock());

        final EirValue[] matches = new EirValue[dirSwitch.matches().length];
        final EirBlock[] targets = new EirBlock[dirSwitch.matches().length];

        for (int i = 0; i < dirSwitch.matches().length; i++) {
            final DirValue dirMatch = dirSwitch.matches()[i];
            assert dirMatch.isConstant();
            matches[i] = dirToEirValue(dirMatch);
            targets[i] = dirToEirBlock(dirSwitch.targetBlocks()[i]);
        }
        // A scratch register.
        final EirVariable indexRegister = createEirVariable(Kind.INT);

        addInstruction(new SWITCH_I32(eirBlock(), tag, matches, targets, defaultTargetBlock, indexRegister));
    }

    public void visitSwitch(DirSwitch dirSwitch) {
        if (dirSwitch.matches().length <= 1 || dirSwitch.valueComparator() != ValueComparator.EQUAL) {
            translateConditionalBranch(dirSwitch, 0);
        } else {
            translateSwitch(dirSwitch);
        }
    }

    public void visitJump(DirJump dirJump) {
        FatalError.unimplemented();
    }
}
