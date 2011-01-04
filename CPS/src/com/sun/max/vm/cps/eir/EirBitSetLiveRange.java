/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.cps.collect.*;
import com.sun.max.vm.cps.eir.allocate.*;
import com.sun.max.vm.runtime.*;

/**
 * @author Bernd Mathiske
 */
public final class EirBitSetLiveRange extends EirLiveRange {

    public EirBitSetLiveRange(EirVariable variable) {
        super(variable);
    }

    private LinkedIdentityHashSet<EirBlock> coverageBlocks = new LinkedIdentityHashSet<EirBlock>();
    private LinkedIdentityHashMap<EirBlock, BitSet> blockToCoverage = new LinkedIdentityHashMap<EirBlock, BitSet>();

    private BitSet makeCoverage(EirBlock block) {
        BitSet coverage = blockToCoverage.get(block);
        if (coverage == null) {
            coverage = new BitSet();
            blockToCoverage.put(block, coverage);
            coverageBlocks.add(block);
        }
        return coverage;
    }

    @Override
    public boolean contains(EirPosition position) {
        final BitSet coverage = blockToCoverage.get(position.block());
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
        return coverage.get(block.instructions().size());
    }

    private void expandLiveRangeToBlock(EirBlock block) {
        final BitSet coverage = makeCoverage(block);

        if (!isCoverageExtendingBeyondBlock(coverage, block)) {
            final int length = block.instructions().size();
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
            for (EirBlock block : r.coverageBlocks) {
                makeCoverage(block).or(r.blockToCoverage.get(block));
            }
        } else {
            FatalError.unimplemented();
        }
    }

    @Override
    public void forAllLiveInstructions(EirInstruction.Procedure procedure) {
        for (EirBlock block : coverageBlocks) {
            final List<EirInstruction> instructions = block.instructions();
            final BitSet coverage = blockToCoverage.get(block);
            for (int i = coverage.nextSetBit(0); 0 <= i && i < instructions.size(); i = coverage.nextSetBit(i + 1)) {
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
            if (coverageBlocks.size() != r.coverageBlocks.size() || blockToCoverage.size() != r.blockToCoverage.size()) {
                traceLiveRange(r);
                return false;
            }
            for (EirBlock block : coverageBlocks) {
                if (!r.coverageBlocks.contains(block) || !blockToCoverage.get(block).equals(r.blockToCoverage.get(block))) {
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
        return blockToCoverage.hashCode();
    }

    @Override
    public String toString() {
        String s = "";
        String blockDelimiter = "";
        for (EirBlock block : coverageBlocks) {
            s += blockDelimiter + "#" + block.serial() + "(";
            String rangeDelimiter = "";
            final BitSet coverage = blockToCoverage.get(block);
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
        for (EirBlock block : coverageBlocks) {
            final List<EirInstruction> instructions = block.instructions();
            final BitSet coverage = blockToCoverage.get(block);
            for (int i = coverage.nextSetBit(0); 0 <= i && i < instructions.size(); i = coverage.nextSetBit(i + 1)) {
                final EirInstruction<?, ?> instruction = instructions.get(i);
                procedure.run(instruction);
            }
        }
    }
}
