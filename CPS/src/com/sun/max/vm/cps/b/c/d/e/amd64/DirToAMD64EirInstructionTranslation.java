/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.b.c.d.e.amd64;

import com.sun.max.program.*;
import com.sun.max.vm.cps.dir.*;
import com.sun.max.vm.cps.dir.eir.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.eir.amd64.AMD64EirInstruction.*;
import com.sun.max.vm.cps.ir.*;
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
