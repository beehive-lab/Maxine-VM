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

import static com.sun.max.vm.cps.eir.EirLocationCategory.*;

import com.sun.max.collect.*;

/**
 * @author Bernd Mathiske
 */
public abstract class EirSwitch<EirInstructionVisitor_Type extends EirInstructionVisitor, EirTargetEmitter_Type extends EirTargetEmitter> extends
                EirInstruction<EirInstructionVisitor_Type, EirTargetEmitter_Type> {

    private final EirOperand tag;

    public EirOperand tag() {
        return tag;
    }

    private final EirOperand[] matches;

    public EirOperand[] matches() {
        return matches;
    }

    public final EirBlock[] targets;

    private EirBlock defaultTarget;

    public EirBlock defaultTarget() {
        return defaultTarget;
    }

    public EirSwitch(EirBlock block, EirValue tag, EirValue[] matches, EirBlock[] targets, EirBlock defaultTarget) {
        super(block);
        assert matches.length >= 1;
        assert matches.length == targets.length;
        this.tag = new EirOperand(this, EirOperand.Effect.USE, G);
        this.tag.setEirValue(tag);
        this.matches = new EirOperand[matches.length];
        for (int i = 0; i < matches.length; i++) {
            this.matches[i] = new EirOperand(this, EirOperand.Effect.USE, I32_I64_L);
            this.matches[i].setEirValue(matches[i]);
        }
        this.targets = targets;
        for (EirBlock target : targets) {
            target.addPredecessor(block);
        }
        this.defaultTarget = defaultTarget;
        this.defaultTarget.addPredecessor(block);
    }

    @Override
    public void visitOperands(EirOperand.Procedure visitor) {
        visitor.run(tag);
        for (EirOperand match : matches) {
            visitor.run(match);
        }
    }

    @Override
    public void visitSuccessorBlocks(EirBlock.Procedure procedure) {
        super.visitSuccessorBlocks(procedure);
        if (defaultTarget != null) {
            procedure.run(defaultTarget);
        }
        for (EirBlock block : targets) {
            procedure.run(block);
        }
    }

    @Override
    public void substituteSuccessorBlocks(Mapping<EirBlock, EirBlock> map) {
        super.substituteSuccessorBlocks(map);
        if (map.containsKey(defaultTarget)) {
            defaultTarget = map.get(defaultTarget);
        }

        for (int i = 0; i < targets.length; i++) {
            if (map.containsKey(targets[i])) {
                targets[i] = map.get(targets[i]);
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder(super.toString());

        for (EirBlock b : targets) {
            result.append("; " + b.toString());
        }

        result.append("; tag=" + tag());

        return result.toString();
    }

    @Override
    public EirBlock selectSuccessorBlock(PoolSet<EirBlock> eligibleBlocks) {
        if (eligibleBlocks.contains(defaultTarget)) {
            return defaultTarget;
        }
        for (EirBlock block : targets) {
            if (eligibleBlocks.contains(block)) {
                return block;
            }
        }
        return null;
    }
}
