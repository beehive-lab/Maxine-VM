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
package com.sun.max.vm.compiler.eir;

import static com.sun.max.vm.compiler.eir.EirLocationCategory.*;

import com.sun.max.collect.*;

/**
 * @author Bernd Mathiske
 */
public abstract class EirSwitch<EirInstructionVisitor_Type extends EirInstructionVisitor, EirTargetEmitter_Type extends EirTargetEmitter> extends
                EirInstruction<EirInstructionVisitor_Type, EirTargetEmitter_Type> {

    private final EirOperand _tag;

    public EirOperand tag() {
        return _tag;
    }

    private final EirOperand[] _matches;

    public EirOperand[] matches() {
        return _matches;
    }

    private final EirBlock[] _targets;

    public EirBlock[] targets() {
        return _targets;
    }

    private EirBlock _defaultTarget;

    public EirBlock defaultTarget() {
        return _defaultTarget;
    }

    public EirSwitch(EirBlock block, EirValue tag, EirValue[] matches, EirBlock[] targets, EirBlock defaultTarget) {
        super(block);
        assert matches.length >= 1;
        assert matches.length == targets.length;
        _tag = new EirOperand(this, EirOperand.Effect.USE, G);
        _tag.setEirValue(tag);
        _matches = new EirOperand[matches.length];
        for (int i = 0; i < matches.length; i++) {
            _matches[i] = new EirOperand(this, EirOperand.Effect.USE, I32_I64_L);
            _matches[i].setEirValue(matches[i]);
        }
        _targets = targets;
        for (EirBlock target : targets) {
            target.addPredecessor(block);
        }
        _defaultTarget = defaultTarget;
        _defaultTarget.addPredecessor(block);
    }

    @Override
    public void visitOperands(EirOperand.Procedure visitor) {
        visitor.run(_tag);
        for (EirOperand match : _matches) {
            visitor.run(match);
        }
    }

    @Override
    public void visitSuccessorBlocks(EirBlock.Procedure procedure) {
        super.visitSuccessorBlocks(procedure);
        if (_defaultTarget != null) {
            procedure.run(_defaultTarget);
        }
        for (EirBlock block : _targets) {
            procedure.run(block);
        }
    }

    @Override
    public void substituteSuccessorBlocks(Mapping<EirBlock, EirBlock> map) {
        super.substituteSuccessorBlocks(map);
        if (map.containsKey(_defaultTarget)) {
            _defaultTarget = map.get(_defaultTarget);
        }

        for (int i = 0; i < _targets.length; i++) {
            if (map.containsKey(_targets[i])) {
                _targets[i] = map.get(_targets[i]);
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder(super.toString());

        for (EirBlock b : _targets) {
            result.append("; " + b.toString());
        }

        result.append("; tag=" + tag());

        return result.toString();
    }

    @Override
    public EirBlock selectSuccessorBlock(PoolSet<EirBlock> eligibleBlocks) {
        if (eligibleBlocks.contains(_defaultTarget)) {
            return _defaultTarget;
        }
        for (EirBlock block : _targets) {
            if (eligibleBlocks.contains(block)) {
                return block;
            }
        }
        return null;
    }
}
