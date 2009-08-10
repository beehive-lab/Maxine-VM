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

import com.sun.c1x.ir.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.util.*;

/**
 *
 * @author Thomas Wuerthinger
 *
 */
public final class ControlFlowOptimizer {

    List<BlockBegin> originalPreds;

    private static final int ShortLoopSize = 5;

    private ControlFlowOptimizer() {
        originalPreds = new ArrayList<BlockBegin>(4);
    }

    public static void optimize(List<BlockBegin> code) {
        ControlFlowOptimizer optimizer = new ControlFlowOptimizer();

        // push the OSR entry block to the end so that we're not jumping over it.
        BlockBegin osrEntry = ((Base) code.get(0).end()).osrEntry();
        if (osrEntry != null) {
            int index = osrEntry.linearScanNumber();
            assert code.get(index) == osrEntry : "wrong index";
            code.remove(index);
            code.add(osrEntry);
        }

        optimizer.reorderShortLoops(code);
        optimizer.deleteEmptyBlocks(code);
        optimizer.deleteUnnecessaryJumps(code);
        optimizer.deleteJumpsToReturn(code);
    }

    void reorderShortLoop(List<BlockBegin> code, BlockBegin headerBlock, int headerIdx) {
        int i = headerIdx + 1;
        int maxEnd = Math.min(headerIdx + ShortLoopSize, code.size());
        while (i < maxEnd && code.get(i).loopDepth() >= headerBlock.loopDepth()) {
            i++;
        }

        if (i == code.size() || code.get(i).loopDepth() < headerBlock.loopDepth()) {
            int endIdx = i - 1;
            BlockBegin endBlock = code.get(endIdx);

            if (endBlock.numberOfSux() == 1 && endBlock.suxAt(0) == headerBlock) {
                // short loop from headerIdx to endIdx found . reorder blocks such that
                // the headerBlock is the last block instead of the first block of the loop
                Util.traceLinearScan(1, "Reordering short loop: length %d, header B%d, end B%d", endIdx - headerIdx + 1, headerBlock.blockID, endBlock.blockID);

                for (int j = headerIdx; j < endIdx; j++) {
                    code.set(j, code.get(j + 1));
                }
                code.set(endIdx, headerBlock);

                // correct the flags so that any loop alignment occurs in the right place.
                assert code.get(endIdx).checkBlockFlag(BlockBegin.BlockFlag.BackwardBranchTarget) : "must be backward branch target";
                code.get(endIdx).clearBlockFlag(BlockBegin.BlockFlag.BackwardBranchTarget);
                code.get(headerIdx).setBlockFlag(BlockBegin.BlockFlag.BackwardBranchTarget);
            }
        }
    }

    void reorderShortLoops(List<BlockBegin> code) {
        for (int i = code.size() - 1; i >= 0; i--) {
            BlockBegin block = code.get(i);

            if (block.checkBlockFlag(BlockBegin.BlockFlag.LinearScanLoopHeader)) {
                reorderShortLoop(code, block, i);
            }
        }

        assert verify(code);
    }

    // only blocks with exactly one successor can be deleted. Such blocks
    // must always end with an unconditional branch to this successor
    boolean canDeleteBlock(BlockBegin block) {
        if (block.numberOfSux() != 1 || block.numberOfExceptionHandlers() != 0 || block.isEntryBlock()) {
            return false;
        }

        List<LIRInstruction> instructions = block.lir().instructionsList();

        assert instructions.size() >= 2 : "block must have label and branch";
        assert instructions.get(0).code() == LIROpcode.Label : "first instruction must always be a label";
        assert instructions.get(instructions.size() - 1) instanceof LIRBranch : "last instrcution must always be a branch";
        assert ((LIRBranch) instructions.get(instructions.size() - 1)).cond() == LIRCondition.Always : "branch must be unconditional";
        assert ((LIRBranch) instructions.get(instructions.size() - 1)).block() == block.suxAt(0) : "branch target must be the successor";

        // block must have exactly one successor

        return instructions.size() == 2 && instructions.get(instructions.size() - 1).info() == null;
        }

    // substitute branch targets in all branch-instructions of this blocks
    void substituteBranchTarget(BlockBegin block, BlockBegin targetFrom, BlockBegin targetTo) {
        Util.traceLinearScan(3, "Deleting empty block: substituting from B%d to B%d inside B%d", targetFrom.blockID, targetTo.blockID, block.blockID);

        List<LIRInstruction> instructions = block.lir().instructionsList();

        assert instructions.get(0).code() == LIROpcode.Label : "first instruction must always be a label";
        for (int i = instructions.size() - 1; i >= 1; i--) {
            LIRInstruction op = instructions.get(i);

            if (op.code() == LIROpcode.Branch || op.code() == LIROpcode.CondFloatBranch) {
                assert op instanceof LIRBranch : "branch must be of type LIRBranch";
                LIRBranch branch = (LIRBranch) op;

                if (branch.block() == targetFrom) {
                    branch.changeBlock(targetTo);
                }
                if (branch.ublock() == targetFrom) {
                    branch.changeUblock(targetTo);
                }
            }
        }
    }

    void deleteEmptyBlocks(List<BlockBegin> code) {
        int oldPos = 0;
        int newPos = 0;
        int numBlocks = code.size();

        while (oldPos < numBlocks) {
            BlockBegin block = code.get(oldPos);

            if (canDeleteBlock(block)) {
                BlockBegin newTarget = block.suxAt(0);

                // propagate backward branch target flag for correct code alignment
                if (block.checkBlockFlag(BlockBegin.BlockFlag.BackwardBranchTarget)) {
                    newTarget.setBlockFlag(BlockBegin.BlockFlag.BackwardBranchTarget);
                }

                // collect a list with all predecessors that contains each predecessor only once
                // the predecessors of cur are changed during the substitution, so a copy of the
                // predecessor list is necessary
                int j;
                originalPreds.clear();
                for (j = block.numberOfPreds() - 1; j >= 0; j--) {
                    BlockBegin pred = block.predAt(j);
                    if (!originalPreds.contains(pred)) {
                        originalPreds.add(pred);
                    }
                }

                for (j = originalPreds.size() - 1; j >= 0; j--) {
                    BlockBegin pred = originalPreds.get(j);
                    substituteBranchTarget(pred, block, newTarget);
                    pred.end().substituteSuccessor(block, newTarget);
                }
            } else {
                // adjust position of this block in the block list if blocks before
                // have been deleted
                if (newPos != oldPos) {
                    code.set(newPos, code.get(oldPos));
                }
                newPos++;
            }
            oldPos++;
        }
        Util.truncate(code, newPos);

        assert verify(code);
    }

    void deleteUnnecessaryJumps(List<BlockBegin> code) {
        // skip the last block because there a branch is always necessary
        for (int i = code.size() - 2; i >= 0; i--) {
            BlockBegin block = code.get(i);
            List<LIRInstruction> instructions = block.lir().instructionsList();

            LIRInstruction lastOp = instructions.get(instructions.size() - 1);
            if (lastOp.code() == LIROpcode.Branch) {
                assert lastOp instanceof LIRBranch : "branch must be of type LIRBranch";
                LIRBranch lastBranch = (LIRBranch) lastOp;

                assert lastBranch.block() != null : "last branch must always have a block as target";
                assert lastBranch.label() == lastBranch.block().label() : "must be equal";

                if (lastBranch.info() == null) {
                    if (lastBranch.block() == code.get(i + 1)) {

                        Util.traceLinearScan(3, "Deleting unconditional branch at end of block B%d", block.blockID);

                        // delete last branch instruction
                        Util.truncate(instructions, instructions.size() - 1);

                    } else {
                        LIRInstruction prevOp = instructions.get(instructions.size() - 2);
                        if (prevOp.code() == LIROpcode.Branch || prevOp.code() == LIROpcode.CondFloatBranch) {
                            assert prevOp instanceof LIRBranch : "branch must be of type LIRBranch";
                            LIRBranch prevBranch = (LIRBranch) prevOp;

                            if (prevBranch.block() == code.get(i + 1) && prevBranch.info() == null) {

                                Util.traceLinearScan(3, "Negating conditional branch and deleting unconditional branch at end of block B%d", block.blockID);

                                // eliminate a conditional branch to the immediate successor
                                prevBranch.changeBlock(lastBranch.block());
                                prevBranch.negateCondition();
                                Util.truncate(instructions, instructions.size() - 1);
                            }
                        }
                    }
                }
            }
        }

        assert verify(code);
    }

    void deleteJumpsToReturn(List<BlockBegin> code) {
        for (int i = code.size() - 1; i >= 0; i--) {
            BlockBegin block = code.get(i);
            List<LIRInstruction> curInstructions = block.lir().instructionsList();
            LIRInstruction curLastOp = curInstructions.get(curInstructions.size() - 1);

            assert curInstructions.get(0).code() == LIROpcode.Label : "first instruction must always be a label";
            if (curInstructions.size() == 2 && curLastOp.code() == LIROpcode.Return) {
                // the block contains only a label and a return
                // if a predecessor ends with an unconditional jump to this block, then the jump
                // can be replaced with a return instruction
                //
                // Note: the original block with only a return statement cannot be deleted completely
                // because the predecessors might have other (conditional) jumps to this block.
                // this may lead to unnecesary return instructions in the final code

                assert curLastOp.info() == null : "return instructions do not have debug information";

                assert curLastOp instanceof LIROp1 : "return must be LIROp1";
                LIROperand returnOpr = ((LIROp1) curLastOp).inOpr();

                for (int j = block.numberOfPreds() - 1; j >= 0; j--) {
                    BlockBegin pred = block.predAt(j);
                    List<LIRInstruction> predInstructions = pred.lir().instructionsList();
                    LIRInstruction predLastOp = predInstructions.get(predInstructions.size() - 1);

                    if (predLastOp.code() == LIROpcode.Branch) {
                        assert predLastOp instanceof LIRBranch : "branch must be LIRBranch";
                        LIRBranch predLastBranch = (LIRBranch) predLastOp;

                        if (predLastBranch.block() == block && predLastBranch.cond() == LIRCondition.Always && predLastBranch.info() == null) {
                            // replace the jump to a return with a direct return
                            // Note: currently the edge between the blocks is not deleted
                            predInstructions.set(predInstructions.size() - 1, new LIROp1(LIROpcode.Return, returnOpr));
                        }
                    }
                }
            }
        }
    }

    boolean verify(List<BlockBegin> code) {
        for (int i = 0; i < code.size(); i++) {
            BlockBegin block = code.get(i);
            List<LIRInstruction> instructions = block.lir().instructionsList();

            int j;
            for (j = 0; j < instructions.size(); j++) {
                LIRInstruction instr = instructions.get(j);

                if (instr instanceof LIRBranch) {

                    LIRBranch opBranch = (LIRBranch) instr;

                    assert opBranch.block() == null || code.contains(opBranch.block()) : "branch target not valid";
                    assert opBranch.ublock() == null || code.contains(opBranch.ublock()) : "branch target not valid";
                }
            }

            for (j = 0; j < block.numberOfSux() - 1; j++) {
                BlockBegin sux = block.suxAt(j);
                assert code.contains(sux) : "successor not valid";
            }

            for (j = 0; j < block.numberOfPreds() - 1; j++) {
                BlockBegin pred = block.predAt(j);
                assert code.contains(pred) : "successor not valid";
            }
        }

        return true;
    }

}
