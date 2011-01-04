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
package com.sun.max.vm.cps.eir;

import com.sun.max.collect.*;

/**
 * This pseudo-instruction (aka directive) directs the control flow of subsequent exceptions towards a given catch
 * block.
 *
 * @author Bernd Mathiske
 */
public class EirTry<EirInstructionVisitor_Type extends EirInstructionVisitor, EirTargetEmitter_Type extends EirTargetEmitter> extends EirInstruction<EirInstructionVisitor_Type, EirTargetEmitter_Type> {

    private EirBlock catchBlock;

    public EirBlock catchBlock() {
        return catchBlock;
    }

    public EirTry(EirBlock block, EirBlock catchBlock) {
        super(block);
        this.catchBlock = catchBlock;
    }

    @Override
    public String toString() {
        if (catchBlock == null) {
            return "try";
        }
        return "try -> #" + catchBlock.serial();
    }

    @Override
    public boolean isRedundant() {
        for (int i = index() - 1; i >= 0; i--) {
            final EirInstruction instruction = block().instructions().get(i);
            if (instruction instanceof EirCall) {
                return false;
            }
            if (instruction instanceof EirTry) {
                final EirTry other = (EirTry) instruction;
                return other.catchBlock == catchBlock;
            }
        }
        return false;
    }

    @Override
    public void visitSuccessorBlocks(EirBlock.Procedure procedure) {
        super.visitSuccessorBlocks(procedure);
        if (catchBlock != null) {
            procedure.run(catchBlock);
        }
    }

    @Override
    public void substituteSuccessorBlocks(Mapping<EirBlock, EirBlock> map) {
        super.substituteSuccessorBlocks(map);
        if (catchBlock != null && map.containsKey(catchBlock)) {
            catchBlock = map.get(catchBlock);
        }
    }

    @Override
    public void acceptVisitor(EirInstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void emit(EirTargetEmitter emitter) {
        emitter.addCatching(catchBlock);
    }

}
