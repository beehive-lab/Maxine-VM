/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.alloc;

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.util.*;

/**
 * This class optimizes moves, particularly those that result from eliminating SSA form.
 * 
 * @author Thomas Wuerthinger
 */
public final class EdgeMoveOptimizer {

    // the class maintains a list with all lir-instruction-list of the
    // successors (predecessors) and the current index into the lir-lists
    List<List<LIRInstruction>> edgeInstructions;
    List<Integer> edgeInstructionsIdx;

    private EdgeMoveOptimizer() {
        edgeInstructions = new ArrayList<List<LIRInstruction>>(4);
        edgeInstructionsIdx = new ArrayList<Integer>(4);
    }

    public static void optimize(List<BlockBegin> code) {
        EdgeMoveOptimizer optimizer = new EdgeMoveOptimizer();

        // ignore the first block in the list (index 0 is not processed)
        for (int i = code.size() - 1; i >= 1; i--) {
            BlockBegin block = code.get(i);

            if (block.numberOfPreds() > 1 && !block.checkBlockFlag(BlockBegin.BlockFlag.ExceptionEntry)) {
                optimizer.optimizeMovesAtBlockEnd(block);
            }
            if (block.numberOfSux() == 2) {
                optimizer.optimizeMovesAtBlockBegin(block);
            }
        }
    }

    // clear all internal data structures
    void initInstructions() {
        edgeInstructions.clear();
        edgeInstructionsIdx.clear();
    }

    // append a lir-instruction-list and the index of the current operation in to the list
    void appendInstructions(List<LIRInstruction> instructions, int instructionsIdx) {
        edgeInstructions.add(instructions);
        edgeInstructionsIdx.add(instructionsIdx);
    }

    // return the current operation of the given edge (predecessor or successor)
    LIRInstruction instructionAt(int edge) {
        List<LIRInstruction> instructions = edgeInstructions.get(edge);
        int idx = edgeInstructionsIdx.get(edge);

        if (idx < instructions.size()) {
            return instructions.get(idx);
        } else {
            return null;
        }
    }

    // removes the current operation of the given edge (predecessor or successor)
    void removeCurInstruction(int edge, boolean decrementIndex) {
        List<LIRInstruction> instructions = edgeInstructions.get(edge);
        int idx = edgeInstructionsIdx.get(edge);
        instructions.remove(idx);

        if (decrementIndex) {
            edgeInstructionsIdx.set(edge, idx - 1);
        }
    }

    boolean operationsDifferent(LIRInstruction op1, LIRInstruction op2) {
        if (op1 == null || op2 == null) {
            // at least one block is already empty . no optimization possible
            return true;
        }

        if (op1.code == LIROpcode.Move && op2.code == LIROpcode.Move) {
            assert op1 instanceof LIROp1 : "move must be LIROp1";
            assert op2 instanceof LIROp1 : "move must be LIROp1";
            LIROp1 move1 = (LIROp1) op1;
            LIROp1 move2 = (LIROp1) op2;
            if (move1.info == move2.info && move1.inOpr() == move2.inOpr() && move1.resultOpr() == move2.resultOpr()) {
                // these moves are exactly equal and can be optimized
                return false;
            }

        }

        // no optimization possible
        return true;
    }

    void optimizeMovesAtBlockEnd(BlockBegin block) {
        Util.traceLinearScan(4, "optimizing moves at end of block B%d", block.blockID);

        if (block.isPredecessor(block)) {
            // currently we can't handle this correctly.
            return;
        }

        initInstructions();
        int numPreds = block.numberOfPreds();
        assert numPreds > 1 : "do not call otherwise";
        assert !block.checkBlockFlag(BlockBegin.BlockFlag.ExceptionEntry) : "exception handlers not allowed";

        // setup a list with the lir-instructions of all predecessors
        int i;
        for (i = 0; i < numPreds; i++) {
            BlockBegin pred = block.predAt(i);
            List<LIRInstruction> predInstructions = pred.lir().instructionsList();

            if (pred.numberOfSux() != 1) {
                // this can happen with switch-statements where multiple edges are between
                // the same blocks.
                return;
            }

            assert pred.numberOfSux() == 1 : "can handle only one successor";
            assert pred.suxAt(0) == block : "invalid control flow";
            assert predInstructions.get(predInstructions.size() - 1).code == LIROpcode.Branch : "block with successor must end with branch";
            assert predInstructions.get(predInstructions.size() - 1) instanceof LIRBranch : "branch must be LIROpBranch";
            assert ((LIRBranch) predInstructions.get(predInstructions.size() - 1)).cond() == LIRCondition.Always : "block must end with unconditional branch";

            if (predInstructions.get(predInstructions.size() - 1).info != null) {
                // can not optimize instructions when debug info is needed
                return;
            }

            // ignore the unconditional branch at the end of the block
            appendInstructions(predInstructions, predInstructions.size() - 2);
        }

        // process lir-instructions while all predecessors end with the same instruction
        while (true) {
            LIRInstruction op = instructionAt(0);
            for (i = 1; i < numPreds; i++) {
                if (operationsDifferent(op, instructionAt(i))) {
                    // these instructions are different and cannot be optimized .
                    // no further optimization possible
                    return;
                }
            }

            if (C1XOptions.TraceLinearScanLevel >= 4) {
                TTY.print("found instruction that is equal in all %d predecessors: ", numPreds);
                op.printOn(TTY.out);
            }

            // insert the instruction at the beginning of the current block
            block.lir().insertBefore(1, op);

            // delete the instruction at the end of all predecessors
            for (i = 0; i < numPreds; i++) {
                removeCurInstruction(i, true);
            }
        }
    }

    void optimizeMovesAtBlockBegin(BlockBegin block) {
        Util.traceLinearScan(4, "optimization moves at begin of block B%d", block.blockID);

        initInstructions();
        int numSux = block.numberOfSux();

        List<LIRInstruction> curInstructions = block.lir().instructionsList();

        assert numSux == 2 : "method should not be called otherwise";
        assert curInstructions.get(curInstructions.size() - 1).code == LIROpcode.Branch : "block with successor must end with branch";
        assert curInstructions.get(curInstructions.size() - 1) instanceof LIRBranch : "branch must be LIROpBranch";
        assert ((LIRBranch) curInstructions.get(curInstructions.size() - 1)).cond() == LIRCondition.Always : "block must end with unconditional branch";

        if (curInstructions.get(curInstructions.size() - 1).info != null) {
            // can no optimize instructions when debug info is needed
            return;
        }

        LIRInstruction branch = curInstructions.get(curInstructions.size() - 2);
        if (branch.info != null || (branch.code != LIROpcode.Branch && branch.code != LIROpcode.CondFloatBranch)) {
            // not a valid case for optimization
            // currently, only blocks that end with two branches (conditional branch followed
            // by unconditional branch) are optimized
            return;
        }

        // now it is guaranteed that the block ends with two branch instructions.
        // the instructions are inserted at the end of the block before these two branches
        int insertIdx = curInstructions.size() - 2;

        if (C1XOptions.DetailedAsserts) {
            int i;
            for (i = insertIdx - 1; i >= 0; i--) {
                LIRInstruction op = curInstructions.get(i);
                if ((op.code == LIROpcode.Branch || op.code == LIROpcode.CondFloatBranch) && ((LIRBranch) op).block() != null) {
                    assert false : "block with two successors can have only two branch instructions";
                }
            }
        }

        // setup a list with the lir-instructions of all successors
        for (int i = 0; i < numSux; i++) {
            BlockBegin sux = block.suxAt(i);
            List<LIRInstruction> suxInstructions = sux.lir().instructionsList();

            assert suxInstructions.get(0).code == LIROpcode.Label : "block must start with label";

            if (sux.numberOfPreds() != 1) {
                // this can happen with switch-statements where multiple edges are between
                // the same blocks.
                return;
            }
            assert sux.predAt(0) == block : "invalid control flow";
            assert !sux.checkBlockFlag(BlockBegin.BlockFlag.ExceptionEntry) : "exception handlers not allowed";

            // ignore the label at the beginning of the block
            appendInstructions(suxInstructions, 1);
        }

        // process lir-instructions while all successors begin with the same instruction
        while (true) {
            LIRInstruction op = instructionAt(0);
            for (int i = 1; i < numSux; i++) {
                if (operationsDifferent(op, instructionAt(i))) {
                    // these instructions are different and cannot be optimized .
                    // no further optimization possible
                    return;
                }
            }

            if (C1XOptions.TraceLinearScanLevel >= 4) {
                TTY.print("----- found instruction that is equal in all %d successors: ", numSux);
                op.printOn(TTY.out);
            }

            // insert instruction at end of current block
            block.lir().insertBefore(insertIdx, op);
            insertIdx++;

            // delete the instructions at the beginning of all successors
            for (int i = 0; i < numSux; i++) {
                removeCurInstruction(i, false);
            }
        }
    }
}
