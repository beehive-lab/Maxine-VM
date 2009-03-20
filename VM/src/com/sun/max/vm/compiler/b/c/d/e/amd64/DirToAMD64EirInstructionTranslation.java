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
package com.sun.max.vm.compiler.b.c.d.e.amd64;

import com.sun.max.program.*;
import com.sun.max.vm.compiler.dir.*;
import com.sun.max.vm.compiler.dir.eir.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.amd64.AMD64EirInstruction.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

public final class DirToAMD64EirInstructionTranslation extends DirToEirInstructionTranslation {

    public DirToAMD64EirInstructionTranslation(DirToEirMethodTranslation methodTranslation, EirBlock eirBlock) {
        super(methodTranslation, eirBlock);
    }

    @Override
    public void assignZero(Kind kind, EirValue variable) {
        addInstruction(new ZERO(eirBlock(), kind, variable));
    }

    @Override
    public DirToEirBuiltinTranslation createBuiltinTranslation(DirToEirInstructionTranslation dirToEirInstructionTranslation, DirJavaFrameDescriptor javaFrameDescriptor) {
        return new DirToAMD64EirBuiltinTranslation(this, javaFrameDescriptor);
    }

    private EirBlock translateConditionalBranch(DirSwitch dirSwitch, int index) {
        final EirValue tag = dirToEirValue(dirSwitch.tag());
        final EirValue match = dirToEirValue(dirSwitch.matches()[index]);
        final EirBlock targetBlock = dirToEirBlock(dirSwitch.targetBlocks()[index]);
        final EirBlock nextBlock = (index < (dirSwitch.targetBlocks().length - 1)) ? createEirBlock(IrBlock.Role.NORMAL) : dirToEirBlock(dirSwitch.defaultTargetBlock());

        final Kind kind = dirSwitch.comparisonKind();
        if (kind == Kind.INT) {
            addInstruction(new CMP_I32(eirBlock(), tag, match));
        } else {
            addInstruction(new CMP_I64(eirBlock(), tag, match));
        }
        switch (dirSwitch.valueComparator()) {
            case EQUAL:
                addInstruction(new JZ(eirBlock(), targetBlock, nextBlock));
                break;
            case NOT_EQUAL:
                addInstruction(new JNZ(eirBlock(), targetBlock, nextBlock));
                break;
            case UNSIGNED_LESS_THAN:
                addInstruction(new JB(eirBlock(), targetBlock, nextBlock));
                break;
            case UNSIGNED_LESS_EQUAL:
                addInstruction(new JBE(eirBlock(), targetBlock, nextBlock));
                break;
            case UNSIGNED_GREATER_EQUAL:
                addInstruction(new JAE(eirBlock(), targetBlock, nextBlock));
                break;
            case UNSIGNED_GREATER_THAN:
                addInstruction(new JA(eirBlock(), targetBlock, nextBlock));
                break;
            case LESS_THAN:
                addInstruction(new JL(eirBlock(), targetBlock, nextBlock));
                break;
            case LESS_EQUAL:
                addInstruction(new JLE(eirBlock(), targetBlock, nextBlock));
                break;
            case GREATER_EQUAL:
                addInstruction(new JGE(eirBlock(), targetBlock, nextBlock));
                break;
            case GREATER_THAN:
                addInstruction(new JG(eirBlock(), targetBlock, nextBlock));
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

        addInstruction(new SWITCH_I32(eirBlock(), tag, matches, targets, defaultTargetBlock));
    }

    public void visitSwitch(DirSwitch dirSwitch) {
        if (dirSwitch.matches().length <= 1 || dirSwitch.valueComparator() != ValueComparator.EQUAL) {
            translateConditionalBranch(dirSwitch, 0);
        } else {
            translateSwitch(dirSwitch);
        }
    }

    public void visitJump(DirJump dirJump) {
        addInstruction(new JMP_indirect(eirBlock(), dirToEirValue(dirJump.parameter())));
    }
}
