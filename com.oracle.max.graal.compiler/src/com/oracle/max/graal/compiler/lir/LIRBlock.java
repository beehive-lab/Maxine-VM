/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.compiler.lir;

import java.util.*;

import com.oracle.max.asm.*;
import com.oracle.max.criutils.*;
import com.oracle.max.graal.compiler.alloc.*;
import com.oracle.max.graal.compiler.schedule.*;
import com.oracle.max.graal.compiler.util.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.java.*;

/**
 * The {@code LIRBlock} class definition.
 */
public final class LIRBlock {

    private final Block block;

    private final int blockID;
    private final int loopIndex;
    private final int loopDepth;

    public final Label label;
    private List<LIRInstruction> lir;
    private FrameState lastState;

    private final List<Node> instructions;
    private List<LIRBlock> predecessors;
    private List<LIRBlock> successors;
    private LIRDebugInfo debugInfo;
    private boolean align;

    private final Node first;
    private final Node last;

    private int linearScanNumber;
    private final boolean linearScanLoopEnd;
    private final boolean linearScanLoopHeader;


    /**
     * Bit map specifying which {@linkplain OperandPool operands} are live upon entry to this block.
     * These are values used in this block or any of its successors where such value are not defined
     * in this block.
     * The bit index of an operand is its {@linkplain OperandPool#operandNumber(com.sun.cri.ci.CiValue) operand number}.
     */
    public BitMap liveIn;

    /**
     * Bit map specifying which {@linkplain OperandPool operands} are live upon exit from this block.
     * These are values used in a successor block that are either defined in this block or were live
     * upon entry to this block.
     * The bit index of an operand is its {@linkplain OperandPool#operandNumber(com.sun.cri.ci.CiValue) operand number}.
     */
    public BitMap liveOut;

    /**
     * Bit map specifying which {@linkplain OperandPool operands} are used (before being defined) in this block.
     * That is, these are the values that are live upon entry to the block.
     * The bit index of an operand is its {@linkplain OperandPool#operandNumber(com.sun.cri.ci.CiValue) operand number}.
     */
    public BitMap liveGen;

    /**
     * Bit map specifying which {@linkplain OperandPool operands} are defined/overwritten in this block.
     * The bit index of an operand is its {@linkplain OperandPool#operandNumber(com.sun.cri.ci.CiValue) operand number}.
     */
    public BitMap liveKill;

    private int firstLirInstructionID;
    private int lastLirInstructionID;


    public LIRDebugInfo debugInfo() {
        return this.debugInfo;
    }

    public LIRBlock(Block block) {
        this.block = block;

        blockID = block.blockID();
        loopIndex = block.loopIndex();
        loopDepth = block.loopDepth();

        label = new Label();
        linearScanNumber = blockID;
        instructions = block.getInstructions();
        predecessors = new ArrayList<LIRBlock>(block.getPredecessors().size());
        successors = new ArrayList<LIRBlock>(block.getSuccessors().size());


        linearScanLoopEnd = block.isLoopEnd();
        linearScanLoopHeader = block.isLoopHeader();

        first = block.firstNode();
        last = block.lastNode();
    }

    public Block schedulerBlock() {
        return block;
    }

    public List<Node> getInstructions() {
        return instructions;
    }

    public int firstLirInstructionId() {
        return firstLirInstructionID;
    }

    public boolean align() {
        return align;
    }

    public void setAlign(boolean b) {
        align = b;
    }

    public void setFirstLirInstructionId(int firstLirInstructionId) {
        this.firstLirInstructionID = firstLirInstructionId;
    }

    public int lastLirInstructionId() {
        return lastLirInstructionID;
    }

    public void setLastLirInstructionId(int lastLirInstructionId) {
        this.lastLirInstructionID = lastLirInstructionId;
    }

    public List<LIRInstruction> lir() {
        return lir;
    }

    public void setLir(List<LIRInstruction> lir) {
        this.lir = lir;
    }

    public void printWithoutPhis(LogStream out) {
        out.println("LIR Block " + blockID());
    }

    public int blockID() {
        return blockID;
    }

    public int numberOfPreds() {
        return predecessors.size();
    }

    public int numberOfSux() {
        return successors.size();
    }

    public boolean isPredecessor(LIRBlock block) {
        return predecessors.contains(block);
    }

    public LIRBlock predAt(int i) {
        return predecessors.get(i);
    }

    public LIRBlock suxAt(int i) {
        return successors.get(i);
    }

    public List<LIRBlock> blockSuccessors() {
        return successors;
    }

    @Override
    public String toString() {
        return "B" + blockID();
    }

    public List<LIRBlock> blockPredecessors() {
        return predecessors;
    }

    public int loopDepth() {
        return loopDepth;
    }

    public int loopIndex() {
        return loopIndex;
    }

    public Label label() {
        return label;
    }

    public void setLinearScanNumber(int v) {
        linearScanNumber = v;
    }

    public int linearScanNumber() {
        return linearScanNumber;
    }

    public boolean isLinearScanLoopEnd() {
        return linearScanLoopEnd;
    }

    public boolean isLinearScanLoopHeader() {
        return linearScanLoopHeader;
    }

    public void replaceWith(LIRBlock other) {
        for (LIRBlock pred : predecessors) {
            Util.replaceAllInList(this, other, pred.successors);
        }
        for (int i = 0; i < other.predecessors.size(); ++i) {
            if (other.predecessors.get(i) == this) {
                other.predecessors.remove(i);
                other.predecessors.addAll(i, this.predecessors);
            }
        }
        successors.clear();
        predecessors.clear();
    }

    public void setLastState(FrameState fs) {
        lastState = fs;
    }

    public FrameState lastState() {
        return lastState;
    }

    public Node firstInstruction() {
        return first;
    }


    public Node lastInstruction() {
        return last;
    }

    public boolean endsWithJump() {
        if (lir.size() == 0) {
            return false;
        }
        LIRInstruction lirInstruction = lir.get(lir.size() - 1);
        if (lirInstruction instanceof LIRXirInstruction) {
            LIRXirInstruction lirXirInstruction = (LIRXirInstruction) lirInstruction;
            return (lirXirInstruction.falseSuccessor() != null) && (lirXirInstruction.trueSuccessor() != null);
        }
        return lirInstruction instanceof LIRBranch;
    }

    public boolean isExceptionEntry() {
        return firstInstruction() instanceof ExceptionObjectNode;
    }
}
