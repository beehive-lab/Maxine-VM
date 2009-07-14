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
package com.sun.c1x.ir;

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.util.*;

/**
 * @author Thomas Wuerthinger
 *
 */
public class ComputeLinearScanOrder {

    private int maxBlockId; // the highest blockId of a block
    private int numBlocks; // total number of blocks (smaller than maxBlockId)
    private int numLoops; // total number of loops
    private boolean iterativeDominators; // method requires iterative computation of dominatiors

    List<BlockBegin> linearScanOrder; // the resulting list of blocks in correct order

    BitMap visitedBlocks; // used for recursive processing of blocks
    BitMap activeBlocks; // used for recursive processing of blocks
    BitMap dominatorBlocks; // temproary BitMap used for computation of dominator
    int[] forwardBranches; // number of incoming forward branches for each block
    List<BlockBegin> loopEndBlocks; // list of all loop end blocks collected during countEdges
    BitMap2D loopMap; // two-dimensional bit set: a bit is set if a block is contained in a loop
    List<BlockBegin> workList; // temporary list (used in markLoops and computeOrder)

    // accessors for visitedBlocks and activeBlocks
    void initVisited() {
        activeBlocks.clearAll();
        visitedBlocks.clearAll();
    }

    boolean isVisited(BlockBegin b) {
        return visitedBlocks.get(b.blockID());
    }

    boolean isActive(BlockBegin b) {
        return activeBlocks.get(b.blockID());
    }

    void setVisited(BlockBegin b) {
        assert !isVisited(b) : "already set";
        visitedBlocks.set(b.blockID());
    }

    void setActive(BlockBegin b) {
        assert !isActive(b) : "already set";
        activeBlocks.set(b.blockID());
    }

    void clearActive(BlockBegin b) {
        assert isActive(b) : "not already";
        activeBlocks.clear(b.blockID());
    }

    // accessors for forwardBranches
    void incForwardBranches(BlockBegin b) {
        forwardBranches[b.blockID()]++;
    }

    int decForwardBranches(BlockBegin b) {
        return --forwardBranches[b.blockID()];
    }

    // accessors for loopMap
    boolean isBlockInLoop(int loopIdx, BlockBegin b) {
        return loopMap.at(loopIdx, b.blockID());
    }

    void setBlockInLoop(int loopIdx, BlockBegin b) {
        loopMap.setBit(loopIdx, b.blockID());
    }

    void clearBlockInLoop(int loopIdx, int blockId) {
        loopMap.clearBit(loopIdx, blockId);
    }

    // accessors for final result
    List<BlockBegin> linearScanOrder() {
        return linearScanOrder;
    }

    int numLoops() {
        return numLoops;
    }

    ComputeLinearScanOrder(C1XCompilation compilation, BlockBegin startBlock) {

        maxBlockId = compilation.numberOfBlocks();
        visitedBlocks = new BitMap(maxBlockId);
        activeBlocks = new BitMap(maxBlockId);
        dominatorBlocks = new BitMap(maxBlockId);
        forwardBranches = new int[maxBlockId];
        loopEndBlocks = new ArrayList<BlockBegin>(8);
        workList = new ArrayList<BlockBegin>(8);
        Util.traceLinearScan(2, " computing linear-scan block order");

        initVisited();
        countEdges(startBlock, null);

        if (numLoops > 0) {
            markLoops();
            clearNonNaturalLoops(startBlock);
            assignLoopDepth(startBlock);
        }

        computeOrder(startBlock);
        computeDominators();

        printBlocks();
        assert verify();
    }

    // Traverse the CFG:
    // * count total number of blocks
    // * count all incoming edges and backward incoming edges
    // * number loop header blocks
    // * create a list with all loop end blocks
    void countEdges(BlockBegin cur, BlockBegin parent) {
        Util.traceLinearScan(3, "Enter countEdges for block B%d coming from B%d", cur.blockID(), parent != null ? parent.blockID() : -1);
        assert cur.dominator() == null : "dominator already initialized";

        if (isActive(cur)) {
            Util.traceLinearScan(3, "backward branch");
            assert isVisited(cur) : "block must be visisted when block is active";
            assert parent != null : "must have parent";

            cur.setBlockFlag(BlockBegin.BlockFlag.LinearScanLoopHeader);
            cur.setBlockFlag(BlockBegin.BlockFlag.BackwardBranchTarget);

            parent.setBlockFlag(BlockBegin.BlockFlag.LinearScanLoopEnd);

            // When a loop header is also the start of an exception handler, then the backward branch is
            // an exception edge. Because such edges are usually critical edges which cannot be split, the
            // loop must be excluded here from processing.
            if (cur.checkBlockFlag(BlockBegin.BlockFlag.ExceptionEntry)) {
                // Make sure that dominators are correct in this weird situation
                iterativeDominators = true;
                return;
            }
            assert parent.numberOfSux() == 1 && parent.suxAt(0) == cur : "loop end blocks must have one successor (critical edges are split)";

            loopEndBlocks.add(parent);
            return;
        }

        // increment number of incoming forward branches
        incForwardBranches(cur);

        if (isVisited(cur)) {
            Util.traceLinearScan(3, "block already visited");
            return;
        }

        numBlocks++;
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
            assert cur.loopIndex() == -1 : "cannot set loop-index twice";
            Util.traceLinearScan(3, "Block B%d is loop header of loop %d", cur.blockID(), numLoops);

            cur.setLoopIndex(numLoops);
            numLoops++;
        }

        Util.traceLinearScan(3, "Finished countEdges for block B%d", cur.blockID());
    }

    void markLoops() {
        Util.traceLinearScan(3, "----- marking loops");

        loopMap = new BitMap2D(numLoops, maxBlockId);
        loopMap.clear();

        for (int i = loopEndBlocks.size() - 1; i >= 0; i--) {
            BlockBegin loopEnd = loopEndBlocks.get(i);
            BlockBegin loopStart = loopEnd.suxAt(0);
            int loopIdx = loopStart.loopIndex();

            Util.traceLinearScan(3, "Processing loop from B%d to B%d (loop %d):", loopStart.blockID(), loopEnd.blockID(), loopIdx);
            assert loopEnd.checkBlockFlag(BlockBegin.BlockFlag.LinearScanLoopEnd) : "loop end flag must be set";
            assert loopEnd.numberOfSux() == 1 : "incorrect number of successors";
            assert loopStart.checkBlockFlag(BlockBegin.BlockFlag.LinearScanLoopHeader) : "loop header flag must be set";
            assert loopIdx >= 0 && loopIdx < numLoops : "loop index not set";
            assert workList.isEmpty() : "work list must be empty before processing";

            // add the end-block of the loop to the working list
            workList.add(loopEnd);
            setBlockInLoop(loopIdx, loopEnd);
            do {
                BlockBegin cur = workList.remove(workList.size() - 1);

                Util.traceLinearScan(3, "    processing B%d", cur.blockID());
                assert isBlockInLoop(loopIdx, cur) : "bit in loop map must be set when block is in work list";

                // recursive processing of all predecessors ends when start block of loop is reached
                if (cur != loopStart && !cur.checkBlockFlag(BlockBegin.BlockFlag.OsrEntry)) {
                    for (int j = cur.numberOfPreds() - 1; j >= 0; j--) {
                        BlockBegin pred = cur.predAt(j);

                        if (!isBlockInLoop(loopIdx, pred)) {
                            // this predecessor has not been processed yet, so add it to work list
                            Util.traceLinearScan(3, "    pushing B%d", pred.blockID());
                            workList.add(pred);
                            setBlockInLoop(loopIdx, pred);
                        }
                    }
                }
            } while (!workList.isEmpty());
        }
    }

    // check for non-natural loops (loops where the loop header does not dominate
    // all other loop blocks = loops with mulitple entries).
    // such loops are ignored
    void clearNonNaturalLoops(BlockBegin startBlock) {
        for (int i = numLoops - 1; i >= 0; i--) {
            if (isBlockInLoop(i, startBlock)) {
                // loop i contains the entry block of the method
                // . this is not a natural loop, so ignore it
                Util.traceLinearScan(2, "Loop %d is non-natural, so it is ignored", i);

                for (int blockId = maxBlockId - 1; blockId >= 0; blockId--) {
                    clearBlockInLoop(i, blockId);
                }
                iterativeDominators = true;
            }
        }
    }

    void assignLoopDepth(BlockBegin startBlock) {
        Util.traceLinearScan(3, "----- computing loop-depth and weight");
        initVisited();

        assert workList.isEmpty() : "work list must be empty before processing";
        workList.add(startBlock);

        do {
            BlockBegin cur = workList.remove(workList.size() - 1);

            if (!isVisited(cur)) {
                setVisited(cur);
                Util.traceLinearScan(4, "Computing loop depth for block B%d", cur.blockID());

                // compute loop-depth and loop-index for the block
                assert cur.loopDepth() == 0 : "cannot set loop-depth twice";
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

    BlockBegin commonDominator(BlockBegin a, BlockBegin b) {
        assert a != null && b != null : "must have input blocks";

        dominatorBlocks.clearAll();
        while (a != null) {
            dominatorBlocks.set(a.blockID());
            assert a.dominator() != null || a == linearScanOrder.get(0) : "dominator must be initialized";
            a = a.dominator();
        }
        while (b != null && !dominatorBlocks.get(b.blockID())) {
            assert b.dominator() != null || b == linearScanOrder.get(0) : "dominator must be initialized";
            b = b.dominator();
        }

        assert b != null : "could not find dominator";
        return b;
    }

    void computeDominator(BlockBegin cur, BlockBegin parent) {
        if (cur.dominator() == null) {
            Util.traceLinearScan(4, "DOM: initializing dominator of B%d to B%d", cur.blockID(), parent.blockID());
            cur.setDominator(parent);

        } else if (!(cur.checkBlockFlag(BlockBegin.BlockFlag.LinearScanLoopHeader) && parent.checkBlockFlag(BlockBegin.BlockFlag.LinearScanLoopEnd))) {
            Util.traceLinearScan(4, "DOM: computing dominator of B%d: common dominator of B%d and B%d is B%d", cur.blockID(), parent.blockID(), cur.dominator().blockID(), commonDominator(
                            cur.dominator(), parent).blockID());
            assert cur.numberOfPreds() > 1 : "";
            cur.setDominator(commonDominator(cur.dominator(), parent));
        }
    }

    int computeWeight(BlockBegin cur) {
        BlockBegin singleSux = null;
        if (cur.numberOfSux() == 1) {
            singleSux = cur.suxAt(0);
        }

        // limit loop-depth to 15 bit (only for security reason, it will never be so big)
        int weight = (cur.loopDepth() & 0x7FFF) << 16;

        int curBit = 15;

        // this is necessery for the (very rare) case that two successing blocks have
        // the same loop depth, but a different loop index (can happen for endless loops
        // with exception handlers)
        if (!cur.checkBlockFlag(BlockBegin.BlockFlag.LinearScanLoopHeader)) {
            weight |= (1 << curBit);
        }
        curBit--;

        // loop end blocks (blocks that end with a backward branch) are added
        // after all other blocks of the loop.
        if (!cur.checkBlockFlag(BlockBegin.BlockFlag.LinearScanLoopEnd)) {
            weight |= (1 << curBit);
        }
        curBit--;

        // critical edge split blocks are prefered because than they have a bigger
        // probability to be completely empty
        if (cur.checkBlockFlag(BlockBegin.BlockFlag.CriticalEdgeSplit)) {
            weight |= (1 << curBit);
        }
        curBit--;

        // exceptions should not be thrown in normal control flow, so these blocks
        // are added as late as possible
        if (!(cur.end() instanceof Throw) && (singleSux == null || !(singleSux.end() instanceof Throw))) {
            weight |= (1 << curBit);
        }
        curBit--;
        if (!(cur.end() instanceof Return) && (singleSux == null || !(singleSux.end() instanceof Return))) {
            weight |= (1 << curBit);
        }
        curBit--;

        // exceptions handlers are added as late as possible
        if (!cur.checkBlockFlag(BlockBegin.BlockFlag.ExceptionEntry)) {
            weight |= (1 << curBit);
        }
        curBit--;

        // guarantee that weight is > 0
        weight |= 1;

        assert curBit >= 0 : "too many flags";
        assert weight > 0 : "weight cannot become negative";

        return weight;
    }

    boolean readyForProcessing(BlockBegin cur) {
        // Discount the edge just traveled.
        // When the number drops to zero, all forward branches were processed
        if (decForwardBranches(cur) != 0) {
            return false;
        }

        assert linearScanOrder.indexOf(cur) == -1 : "block already processed (block can be ready only once)";
        assert workList.indexOf(cur) == -1 : "block already in work-list (block can be ready only once)";
        return true;
    }

    void sortIntoWorkList(BlockBegin cur) {
        assert workList.indexOf(cur) == -1 : "block already in work list";

        int curWeight = computeWeight(cur);

        // the linearScanNumber is used to cache the weight of a block
        cur.setLinearScanNumber(curWeight);

        if (C1XOptions.StressLinearScan) {
            workList.add(0, cur);
            return;
        }

        workList.add(null); // provide space for new element

        int insertIdx = workList.size() - 1;
        while (insertIdx > 0 && workList.get(insertIdx - 1).linearScanNumber() > curWeight) {
            workList.set(insertIdx, workList.get(insertIdx - 1));
            insertIdx--;
        }
        workList.set(insertIdx, cur);

        Util.traceLinearScan(3, "Sorted B%d into worklist. new worklist:", cur.blockID());
        if (C1XOptions.TraceLinearScanLevel >= 3) {
            for (int i = 0; i < workList.size(); i++) {
                TTY.println(String.format("%8d B%2d  weight:%6x", i, workList.get(i).blockID(), workList.get(i).linearScanNumber()));
            }
        }

        for (int i = 0; i < workList.size(); i++) {
            assert workList.get(i).linearScanNumber() > 0 : "weight not set";
            assert i == 0 || workList.get(i - 1).linearScanNumber() <= workList.get(i).linearScanNumber() : "incorrect order in worklist";
        }
    }

    void appendBlock(BlockBegin cur) {
        Util.traceLinearScan(3, "appending block B%d (weight 0x%6x) to linear-scan order", cur.blockID(), cur.linearScanNumber());
        assert linearScanOrder.indexOf(cur) == -1 : "cannot add the same block twice";

        // currently, the linear scan order and code emit order are equal.
        // therefore the linearScanNumber and the weight of a block must also
        // be equal.
        cur.setLinearScanNumber(linearScanOrder.size());
        linearScanOrder.add(cur);
    }

    void computeOrder(BlockBegin startBlock) {
        Util.traceLinearScan(3, "----- computing final block order");

        // the start block is always the first block in the linear scan order
        linearScanOrder = new ArrayList<BlockBegin>(numBlocks);
        appendBlock(startBlock);

        assert startBlock.end() instanceof Base : "start block must end with Base-instruction";
        BlockBegin stdEntry = ((Base) startBlock.end()).standardEntry();
        BlockBegin osrEntry = ((Base) startBlock.end()).osrEntry();

        BlockBegin suxOfOsrEntry = null;
        if (osrEntry != null) {
            // special handling for osr entry:
            // ignore the edge between the osr entry and its successor for processing
            // the osr entry block is added manually below
            assert osrEntry.numberOfSux() == 1 : "osr entry must have exactly one successor";
            assert osrEntry.suxAt(0).numberOfPreds() >= 2 : "sucessor of osr entry must have two predecessors (otherwise it is not present in normal control flow)";

            suxOfOsrEntry = osrEntry.suxAt(0);
            decForwardBranches(suxOfOsrEntry);

            computeDominator(osrEntry, startBlock);
            iterativeDominators = true;
        }
        computeDominator(stdEntry, startBlock);

        // start processing with standard entry block
        assert workList.isEmpty() : "list must be empty before processing";

        if (readyForProcessing(stdEntry)) {
            sortIntoWorkList(stdEntry);
        } else {
            assert false : "the stdEntry must be ready for processing (otherwise, the method has no start block)";
        }

        do {
            BlockBegin cur = workList.remove(workList.size() - 1);

            if (cur == suxOfOsrEntry) {
                // the osr entry block is ignored in normal processing : it is never added to the
                // work list. Instead : it is added as late as possible manually here.
                appendBlock(osrEntry);
                computeDominator(cur, osrEntry);
            }
            appendBlock(cur);

            int i;
            int numSux = cur.numberOfSux();
            // changed loop order to get "intuitive" order of if- and else-blocks
            for (i = 0; i < numSux; i++) {
                BlockBegin sux = cur.suxAt(i);
                computeDominator(sux, cur);
                if (readyForProcessing(sux)) {
                    sortIntoWorkList(sux);
                }
            }
            numSux = cur.numberOfExceptionHandlers();
            for (i = 0; i < numSux; i++) {
                BlockBegin sux = cur.exceptionHandlerAt(i);
                computeDominator(sux, cur);
                if (readyForProcessing(sux)) {
                    sortIntoWorkList(sux);
                }
            }
        } while (workList.size() > 0);
    }

    boolean computeDominatorsIter() {
        boolean changed = false;
        int numBlocks = linearScanOrder.size();

        assert linearScanOrder.get(0).dominator() == null : "must not have dominator";
        assert linearScanOrder.get(0).numberOfPreds() == 0 : "must not have predecessors";
        for (int i = 1; i < numBlocks; i++) {
            BlockBegin block = linearScanOrder.get(i);

            BlockBegin dominator = block.predAt(0);
            int numPreds = block.numberOfPreds();
            for (int j = 1; j < numPreds; j++) {
                dominator = commonDominator(dominator, block.predAt(j));
            }

            if (dominator != block.dominator()) {
                Util.traceLinearScan(4, "DOM: updating dominator of B%d from B%d to B%d", block.blockID(), block.dominator().blockID(), dominator.blockID());
                block.setDominator(dominator);
                changed = true;
            }
        }
        return changed;
    }

    void computeDominators() {
        Util.traceLinearScan(3, "----- computing dominators (iterative computation reqired: %d)", iterativeDominators);

        // iterative computation of dominators is only required for methods with non-natural loops
        // and OSR-methods. For all other methods : the dominators computed when generating the
        // linear scan block order are correct.
        if (iterativeDominators) {
            do {
                Util.traceLinearScan(1, "DOM: next iteration of fix-point calculation");
            } while (computeDominatorsIter());
        }

        // check that dominators are correct
        assert !computeDominatorsIter() : "fix point not reached";
    }

    void printBlocks() {
        if (C1XOptions.TraceLinearScanLevel >= 2) {
            TTY.println("----- loop information:");
            for (int blockIdx = 0; blockIdx < linearScanOrder.size(); blockIdx++) {
                BlockBegin cur = linearScanOrder.get(blockIdx);

                TTY.println(String.format("%4d: B%2d: ", cur.linearScanNumber(), cur.blockID()));
                for (int loopIdx = 0; loopIdx < numLoops; loopIdx++) {
                    TTY.println(String.format("%d ", isBlockInLoop(loopIdx, cur)));
                }
                TTY.println(String.format(" . loopIndex: %2d, loopDepth: %2d", cur.loopIndex(), cur.loopDepth()));
            }
        }

        if (C1XOptions.TraceLinearScanLevel >= 1) {
            TTY.println("----- linear-scan block order:");
            for (int blockIdx = 0; blockIdx < linearScanOrder.size(); blockIdx++) {
                BlockBegin cur = linearScanOrder.get(blockIdx);
                TTY.print(String.format("%4d: B%2d    loop: %2d  depth: %2d", cur.linearScanNumber(), cur.blockID(), cur.loopIndex(), cur.loopDepth()));

                TTY.print(cur.checkBlockFlag(BlockBegin.BlockFlag.ExceptionEntry) ? " ex" : "   ");
                TTY.print(cur.checkBlockFlag(BlockBegin.BlockFlag.CriticalEdgeSplit) ? " ce" : "   ");
                TTY.print(cur.checkBlockFlag(BlockBegin.BlockFlag.LinearScanLoopHeader) ? " lh" : "   ");
                TTY.print(cur.checkBlockFlag(BlockBegin.BlockFlag.LinearScanLoopEnd) ? " le" : "   ");

                if (cur.dominator() != null) {
                    TTY.print("    dom: B%d ", cur.dominator().blockID());
                } else {
                    TTY.print("    dom: null ");
                }

                if (cur.numberOfPreds() > 0) {
                    TTY.print("    preds: ");
                    for (int j = 0; j < cur.numberOfPreds(); j++) {
                        BlockBegin pred = cur.predAt(j);
                        TTY.print("B%d ", pred.blockID());
                    }
                }
                if (cur.numberOfSux() > 0) {
                    TTY.print("    sux: ");
                    for (int j = 0; j < cur.numberOfSux(); j++) {
                        BlockBegin sux = cur.suxAt(j);
                        TTY.print("B%d ", sux.blockID());
                    }
                }
                if (cur.numberOfExceptionHandlers() > 0) {
                    TTY.print("    ex: ");
                    for (int j = 0; j < cur.numberOfExceptionHandlers(); j++) {
                        BlockBegin ex = cur.exceptionHandlerAt(j);
                        TTY.print("B%d ", ex.blockID());
                    }
                }
                TTY.cr();
            }
        }
    }

    boolean verify() {
        assert linearScanOrder.size() == numBlocks : "wrong number of blocks in list";

        if (C1XOptions.StressLinearScan) {
            // blocks are scrambled when StressLinearScan is used
            return true;
        }

        // check that all successors of a block have a higher linear-scan-number
        // and that all predecessors of a block have a lower linear-scan-number
        // (only backward branches of loops are ignored)
        int i;
        for (i = 0; i < linearScanOrder.size(); i++) {
            BlockBegin cur = linearScanOrder.get(i);

            assert cur.linearScanNumber() == i : "incorrect linearScanNumber";
            assert cur.linearScanNumber() >= 0 && cur.linearScanNumber() == linearScanOrder.indexOf(cur) : "incorrect linearScanNumber";

            int j;
            for (j = cur.numberOfSux() - 1; j >= 0; j--) {
                BlockBegin sux = cur.suxAt(j);

                assert sux.linearScanNumber() >= 0 && sux.linearScanNumber() == linearScanOrder.indexOf(sux) : "incorrect linearScanNumber";
                if (!cur.checkBlockFlag(BlockBegin.BlockFlag.LinearScanLoopEnd)) {
                    assert cur.linearScanNumber() < sux.linearScanNumber() : "invalid order";
                }
                if (cur.loopDepth() == sux.loopDepth()) {
                    assert cur.loopIndex() == sux.loopIndex() || sux.checkBlockFlag(BlockBegin.BlockFlag.LinearScanLoopHeader) : "successing blocks with same loop depth must have same loop index";
                }
            }

            for (j = cur.numberOfPreds() - 1; j >= 0; j--) {
                BlockBegin pred = cur.predAt(j);

                assert pred.linearScanNumber() >= 0 && pred.linearScanNumber() == linearScanOrder.indexOf(pred) : "incorrect linearScanNumber";
                if (!cur.checkBlockFlag(BlockBegin.BlockFlag.LinearScanLoopHeader)) {
                    assert cur.linearScanNumber() > pred.linearScanNumber() : "invalid order";
                }
                if (cur.loopDepth() == pred.loopDepth()) {
                    assert cur.loopIndex() == pred.loopIndex() || cur.checkBlockFlag(BlockBegin.BlockFlag.LinearScanLoopHeader) : "successing blocks with same loop depth must have same loop index";
                }

                assert cur.dominator().linearScanNumber() <= cur.predAt(j).linearScanNumber() : "dominator must be before predecessors";
            }

            // check dominator
            if (i == 0) {
                assert cur.dominator() == null : "first block has no dominator";
            } else {
                assert cur.dominator() != null : "all but first block must have dominator";
            }
            assert cur.numberOfPreds() != 1 || cur.dominator() == cur.predAt(0) : "Single predecessor must also be dominator";
        }

        // check that all loops are continuous
        for (int loopIdx = 0; loopIdx < numLoops; loopIdx++) {
            int blockIdx = 0;
            assert !isBlockInLoop(loopIdx, linearScanOrder.get(blockIdx)) : "the first block must not be present in any loop";

            // skip blocks before the loop
            while (blockIdx < numBlocks && !isBlockInLoop(loopIdx, linearScanOrder.get(blockIdx))) {
                blockIdx++;
            }
            // skip blocks of loop
            while (blockIdx < numBlocks && isBlockInLoop(loopIdx, linearScanOrder.get(blockIdx))) {
                blockIdx++;
            }
            // after the first non-loop block : there must not be another loop-block
            while (blockIdx < numBlocks) {
                assert !isBlockInLoop(loopIdx, linearScanOrder.get(blockIdx)) : "loop not continuous in linear-scan order";
                blockIdx++;
            }
        }

        return true;
    }
}
