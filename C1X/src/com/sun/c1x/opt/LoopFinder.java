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
package com.sun.c1x.opt;

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.util.*;

/**
 * This class walks the control flow graph and identifies loops.
 *
 * @author Marcelo Cintra
 */
public class LoopFinder {

    private int maxBlockId; // the highest blockId of a block
    private int numLoops; // total number of loops

    private BitMap visitedBlocks; // used for recursive processing of blocks
    private BitMap activeBlocks; // used for recursive processing of blocks
    private List<BlockBegin> loopEndBlocks; // list of all loop end blocks collected during countEdges
    private BitMap2D loopMap; // two-dimensional bit set: a bit is set if a block is contained in a loop
    private List<BlockBegin> workList; // temporary list (used in markLoops and computeOrder)
    List<Loop> loopList;

    public LoopFinder(int maxBlockId, BlockBegin startBlock) {
        this.maxBlockId = maxBlockId;
        visitedBlocks = new BitMap(maxBlockId);
        activeBlocks = new BitMap(maxBlockId);
        loopEndBlocks = new ArrayList<BlockBegin>(8);
        workList = new ArrayList<BlockBegin>(8);
        loopList = new ArrayList<Loop>();

        initVisited();
        countEdges(startBlock, null);

        if (numLoops > 0) {
            markLoops();
            clearNonNaturalLoops(startBlock);
            assignLoopDepth(startBlock);
            if (C1XOptions.PrintLoopList) {
                printLoops(startBlock.stateBefore().scope().method);
            }
        }

        // cleanup flags to avoid assertion errors when computing linear scan ordering
        startBlock.iterateAnyOrder(new BlockClosure() {
            public void apply(BlockBegin block) {
                block.setLoopIndex(-1);
                block.setLoopDepth(0);
            }
        }, false);
    }

    private void printLoops(RiMethod method) {
        System.out.println("Compiling " + method);
        for (Loop loop : loopList) {
            System.out.println(loop);
        }
    }

    public List<Loop> getLoopList() {
        return loopList;
    }

    // Traverse the CFG:
    // * count total number of blocks
    // * count all incoming edges and backward incoming edges
    // * number loop header blocks
    // * create a list with all loop end blocks
    private void countEdges(BlockBegin cur, BlockBegin parent) {
        if (isActive(cur)) {
            assert isVisited(cur) : "block must be visited when block is active";
            assert parent != null : "must have parent";

            cur.setBlockFlag(BlockBegin.BlockFlag.LinearScanLoopHeader);
            cur.setBlockFlag(BlockBegin.BlockFlag.BackwardBranchTarget);

            parent.setBlockFlag(BlockBegin.BlockFlag.LinearScanLoopEnd);

            loopEndBlocks.add(parent);
            return;
        }

        if (isVisited(cur)) {
            return;
        }

        setVisited(cur);
        setActive(cur);

        // recursive call for all successors
        int i;
        for (i = cur.numberOfSux() - 1; i >= 0; i--) {
            countEdges(cur.suxAt(i), cur);
        }
        for (i = cur.numberOfExceptionHandlers() - 1; i >= 0; i--) {
            countEdges(cur.exceptionHandlerAt(i), cur);
        }

        clearActive(cur);

        // Each loop has a unique number.
        // When multiple loops are nested, assignLoopDepth assumes that the
        // innermost loop has the lowest number. This is guaranteed by setting
        // the loop number after the recursive calls for the successors above
        // have returned.
        if (cur.checkBlockFlag(BlockBegin.BlockFlag.LinearScanLoopHeader)) {
            cur.setLoopIndex(numLoops);
            numLoops++;
        }
    }

    private void markLoops() {
        loopMap = new BitMap2D(numLoops, maxBlockId);
        loopMap.clear();

        for (int i = loopEndBlocks.size() - 1; i >= 0; i--) {
            BlockBegin loopEnd = loopEndBlocks.get(i);
            BlockBegin loopStart = loopEnd.suxAt(0);
            int loopIdx = loopStart.loopIndex();
            ArrayList<BlockBegin> loopBody = new ArrayList<BlockBegin>();

            assert loopEnd.checkBlockFlag(BlockBegin.BlockFlag.LinearScanLoopEnd) : "loop end flag must be set";
            assert loopStart.checkBlockFlag(BlockBegin.BlockFlag.LinearScanLoopHeader) : "loop header flag must be set";
            assert loopIdx >= 0 && loopIdx < numLoops : "loop index not set";
            assert workList.isEmpty() : "work list must be empty before processing";

            // add the end-block of the loop to the working list
            workList.add(loopEnd);
            loopBody.add(loopEnd);
            setBlockInLoop(loopIdx, loopEnd);
            do {
                BlockBegin cur = workList.remove(workList.size() - 1);

                assert isBlockInLoop(loopIdx, cur) : "bit in loop map must be set when block is in work list";

                // recursive processing of all predecessors ends when start block of loop is reached
                if (cur != loopStart && !cur.checkBlockFlag(BlockBegin.BlockFlag.OsrEntry)) {
                    for (int j = cur.numberOfPreds() - 1; j >= 0; j--) {
                        BlockBegin pred = cur.predAt(j);

                        if (!isBlockInLoop(loopIdx, pred)) {
                            // this predecessor has not been processed yet, so add it to work list
                            // Util.traceLinearScan(3, "    pushing B%d", pred.blockID);
                            workList.add(pred);
                            loopBody.add(pred);
                            setBlockInLoop(loopIdx, pred);
                        }
                    }
                }
            } while (!workList.isEmpty());
            Loop loop = new Loop(loopStart, loopEnd, loopBody);
            loopList.add(loop);
        }
    }

    // check for non-natural loops (loops where the loop header does not dominate
    // all other loop blocks = loops with multiple entries).
    // such loops are ignored
    private void clearNonNaturalLoops(BlockBegin startBlock) {
        for (int i = numLoops - 1; i >= 0; i--) {
            if (isBlockInLoop(i, startBlock)) {
                // loop i contains the entry block of the method.
                // this is not a natural loop, so ignore it
                for (int blockId = maxBlockId - 1; blockId >= 0; blockId--) {
                    clearBlockInLoop(i, blockId);
                }
            }
        }
    }

    private void assignLoopDepth(BlockBegin startBlock) {
        initVisited();

        assert workList.isEmpty() : "work list must be empty before processing";
        workList.add(startBlock);

        do {
            BlockBegin cur = workList.remove(workList.size() - 1);

            if (!isVisited(cur)) {
                setVisited(cur);

                // compute loop-depth and loop-index for the block
                int i;
                int loopDepth = 0;
                int minLoopIdx = -1;
                for (i = numLoops - 1; i >= 0; i--) {
                    if (isBlockInLoop(i, cur)) {
                        loopDepth++;
                        minLoopIdx = i;
                    }
                }
                cur.setLoopDepth(loopDepth);
                cur.setLoopIndex(minLoopIdx);

                // append all unvisited successors to work list
                for (i = cur.numberOfSux() - 1; i >= 0; i--) {
                    workList.add(cur.suxAt(i));
                }
                for (i = cur.numberOfExceptionHandlers() - 1; i >= 0; i--) {
                    workList.add(cur.exceptionHandlerAt(i));
                }
            }
        } while (!workList.isEmpty());
    }

    private void initVisited() {
        activeBlocks.clearAll();
        visitedBlocks.clearAll();
    }

    private boolean isVisited(BlockBegin b) {
        return visitedBlocks.get(b.blockID);
    }

    private boolean isActive(BlockBegin b) {
        return activeBlocks.get(b.blockID);
    }

    private void setVisited(BlockBegin b) {
        assert !isVisited(b) : "already set";
        visitedBlocks.set(b.blockID);
    }

    private void setActive(BlockBegin b) {
        assert !isActive(b) : "already set";
        activeBlocks.set(b.blockID);
    }

    private void clearActive(BlockBegin b) {
        assert isActive(b) : "not already";
        activeBlocks.clear(b.blockID);
    }

    private boolean isBlockInLoop(int loopIdx, BlockBegin b) {
        return loopMap.at(loopIdx, b.blockID);
    }

    private void setBlockInLoop(int loopIdx, BlockBegin b) {
        loopMap.setBit(loopIdx, b.blockID);
    }

    private void clearBlockInLoop(int loopIdx, int blockId) {
        loopMap.clearBit(loopIdx, blockId);
    }
}
