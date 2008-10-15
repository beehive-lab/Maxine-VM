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

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.eir.allocate.*;

/**
 * @author Bernd Mathiske
 */
public final class EirBitSetLiveRange extends EirLiveRange {

    public EirBitSetLiveRange(EirVariable variable) {
        super(variable);
    }

    private GrowableDeterministicSet<EirBlock> _coverageBlocks = new LinkedIdentityHashSet<EirBlock>();
    private DeterministicMap<EirBlock, BitSet> _blockToCoverage = new LinkedIdentityHashMap<EirBlock, BitSet>();

    private BitSet makeCoverage(EirBlock block) {
        BitSet coverage = _blockToCoverage.get(block);
        if (coverage == null) {
            coverage = new BitSet();
            _blockToCoverage.put(block, coverage);
            _coverageBlocks.add(block);
        }
        return coverage;
    }

    @Override
    public boolean contains(EirPosition position) {
        final BitSet coverage = _blockToCoverage.get(position.block());
        if (coverage == null) {
            return false;
        }
        return coverage.get(position.index());
    }

    @Override
    public void recordDefinition(EirOperand operand) {
        final BitSet coverage = makeCoverage(operand.instruction().block());
        coverage.set(operand.instruction().index());
    }

    private boolean isCoverageExtendingBeyondBlock(BitSet coverage, EirBlock block) {
        return coverage.get(block.instructions().length());
    }

    private void expandLiveRangeToBlock(EirBlock block) {
        final BitSet coverage = makeCoverage(block);

        if (!isCoverageExtendingBeyondBlock(coverage, block)) {
            final int length = block.instructions().length();
            coverage.set(length);
            expandLiveRange(coverage, block, length);
        }
    }

    private void expandLiveRangeToPredecessors(EirBlock block) {
        for (EirBlock predecessor : block.predecessors()) {
            expandLiveRangeToBlock(predecessor);
        }
    }

    private void expandLiveRange(BitSet coverage, EirBlock block, int useIndex) {
        if (useIndex == 0) {
            expandLiveRangeToPredecessors(block);
            return;
        }
        final int bit = coverage.nextSetBit(0);
        if (bit < 0 || bit >= useIndex) {
            coverage.set(0, useIndex);
            expandLiveRangeToPredecessors(block);
        } else {
            for (int i = useIndex - 1; i >= 0; i--) {
                if (coverage.get(i)) {
                    coverage.set(i, useIndex);
                    return;
                }
            }
            ProgramError.unexpected();
        }
    }

    @Override
    public void recordUse(EirOperand operand) {
        final EirBlock block = operand.instruction().block();
        final int index = operand.instruction().index();
        if (index == 0) {
            expandLiveRangeToPredecessors(block);
            return;
        }
        final BitSet coverage = makeCoverage(block);
        if (!coverage.get(index - 1)) {
            expandLiveRange(coverage, block, index);
        }
    }

    @Override
    public void add(EirLiveRange other) {
        if (other instanceof EirBitSetLiveRange) {
            final EirBitSetLiveRange r = (EirBitSetLiveRange) other;
            for (EirBlock block : r._coverageBlocks) {
                makeCoverage(block).or(r._blockToCoverage.get(block));
            }
        } else {
            Problem.unimplemented();
        }
    }

    @Override
    public void forAllLiveInstructions(EirInstruction.Procedure procedure) {
        for (EirBlock block : _coverageBlocks) {
            final IndexedSequence<EirInstruction> instructions = block.instructions();
            final BitSet coverage = _blockToCoverage.get(block);
            for (int i = coverage.nextSetBit(0); 0 <= i && i < instructions.length(); i = coverage.nextSetBit(i + 1)) {
                final EirInstruction<?, ?> instruction = instructions.get(i);
                procedure.run(instruction);
            }
        }
    }

    @Override
    public void compute() {
        for (EirOperand operand : variable().operands()) {
            operand.recordDefinition();
        }
        for (EirOperand operand : variable().operands()) {
            operand.recordUse();
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof EirBitSetLiveRange) {
            final EirBitSetLiveRange r = (EirBitSetLiveRange) other;
            if (_coverageBlocks.length() != r._coverageBlocks.length() || _blockToCoverage.length() != r._blockToCoverage.length()) {
                traceLiveRange(r);
                return false;
            }
            for (EirBlock block : _coverageBlocks) {
                if (!r._coverageBlocks.contains(block) || !_blockToCoverage.get(block).equals(r._blockToCoverage.get(block))) {
                    traceLiveRange(r);
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private void traceLiveRange(EirBitSetLiveRange r) {
        if (Trace.hasLevel(1)) {
            Trace.line(1, "liveRange of " + variable() + ": " + this);
            Trace.line(1, "liveRange of " + r.variable() + ": " + r);
        }
    }

    @Override
    public int hashCode() {
        return _blockToCoverage.hashCode();
    }

    @Override
    public String toString() {
        String s = "";
        String blockDelimiter = "";
        for (EirBlock block : _coverageBlocks) {
            s += blockDelimiter + "#" + block.serial() + "(";
            String rangeDelimiter = "";
            final BitSet coverage = _blockToCoverage.get(block);
            int i = 0;
            while (i <= coverage.size()) {
                if (coverage.get(i)) {
                    s += rangeDelimiter;
                    final int end = coverage.nextClearBit(i);
                    if (end == i + 1) {
                        s += i;
                    } else {
                        s += i + "-" + end;
                    }
                    rangeDelimiter = ", ";
                    i = end + 1;
                } else {
                    i++;
                }
            }
            s += ")";
            blockDelimiter = ", ";
        }
        return s;
    }

    @Override
    public void visitInstructions(EirInstruction.Procedure procedure) {
        for (EirBlock block : _coverageBlocks) {
            final IndexedSequence<EirInstruction> instructions = block.instructions();
            final BitSet coverage = _blockToCoverage.get(block);
            for (int i = coverage.nextSetBit(0); 0 <= i && i < instructions.length(); i = coverage.nextSetBit(i + 1)) {
                final EirInstruction<?, ?> instruction = instructions.get(i);
                procedure.run(instruction);
            }
        }
    }
}
