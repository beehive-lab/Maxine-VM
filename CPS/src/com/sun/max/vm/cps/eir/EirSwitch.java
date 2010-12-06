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
