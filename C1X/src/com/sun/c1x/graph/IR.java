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
package com.sun.c1x.graph;

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.opt.*;
import com.sun.c1x.value.*;

/**
 * This class implements the overall container for the HIR (high-level IR) graph
 * and directs its construction, optimization, and finalization.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public class IR {

    /**
     * The compilation associated with this IR.
     */
    public final C1XCompilation compilation;

    /**
     * The start block of this IR.
     */
    public BlockBegin startBlock;

    /**
     * The entry block for an OSR compile.
     */
    public BlockBegin osrEntryBlock;

    /**
     * The top IRScope.
     */
    public IRScope topScope;

    /**
     * The linear-scan ordered list of blocks.
     */
    private List<BlockBegin> orderedBlocks;

    /**
     * Creates a new IR instance for the specified compilation.
     * @param compilation the compilation
     */
    public IR(C1XCompilation compilation) {
        this.compilation = compilation;
    }

    /**
     * Builds the graph, optimizes it, and computes the linear scan block order.
     */
    public void build() {
        if (C1XOptions.PrintTimers) {
            C1XTimers.HIR_CREATE.start();
        }

        buildGraph();

        if (C1XOptions.PrintTimers) {
            C1XTimers.HIR_CREATE.stop();
            C1XTimers.HIR_OPTIMIZE.start();
        }

        optimize1();
        computeLinearScanOrder();
        optimize2();

        if (C1XOptions.PrintTimers) {
            C1XTimers.HIR_OPTIMIZE.stop();
        }
    }

    private void buildGraph() {
        topScope = new IRScope(compilation, null, -1, compilation.method, compilation.osrBCI);

        // Graph builder must set the startBlock and the osrEntryBlock
        new GraphBuilder(compilation, topScope, this);
        assert startBlock != null;
        verifyAndPrint("After graph building");
    }

    private void optimize1() {
        // do basic optimizations
        if (C1XOptions.PhiSimplify) {
            new PhiSimplifier(this);
            verifyAndPrint("After phi simplification");
        }
        if (C1XOptions.OptLoopPeeling) {
            LoopPeeler.peelLoops(this);
            verifyAndPrint("After Loop peeling");
        }
        if (C1XOptions.OptNullCheckElimination) {
            new NullCheckEliminator(this);
            verifyAndPrint("After null check elimination");
        }
        if (C1XOptions.OptDeadCodeElimination1) {
            new LivenessMarker(this).removeDeadCode();
            verifyAndPrint("After dead code elimination 1");
        }
        if (C1XOptions.OptCEElimination) {
            new CEEliminator(this);
            verifyAndPrint("After CEE elimination");
        }
        if (C1XOptions.OptBlockMerging) {
            new BlockMerger(this);
            verifyAndPrint("After block merging");
        }
    }

    private void computeLinearScanOrder() {
        if (C1XOptions.GenLIR) {
            makeLinearScanOrder();
            verifyAndPrint("After linear scan order");
        }
    }

    private void makeLinearScanOrder() {
        if (orderedBlocks == null) {
            CriticalEdgeFinder finder = new CriticalEdgeFinder(this);
            startBlock.iteratePreOrder(finder);
            finder.splitCriticalEdges();
            ComputeLinearScanOrder computeLinearScanOrder = new ComputeLinearScanOrder(compilation.stats.blockCount, startBlock);
            orderedBlocks = computeLinearScanOrder.linearScanOrder();
            compilation.stats.loopCount = computeLinearScanOrder.numLoops();
            computeLinearScanOrder.printBlocks();
        }
    }

    private void optimize2() {
        // do more advanced, dominator-based optimizations
        if (C1XOptions.OptGlobalValueNumbering) {
            makeLinearScanOrder();
            new GlobalValueNumberer(this);
            verifyAndPrint("After global value numbering");
        }
        if (C1XOptions.OptDeadCodeElimination2) {
            new LivenessMarker(this).removeDeadCode();
            verifyAndPrint("After dead code elimination 2");
        }

    }

    /**
     * Gets the linear scan ordering of blocks as a list.
     * @return the blocks in linear scan order
     */
    public List<BlockBegin> linearScanOrder() {
        return orderedBlocks;
    }

    private void print(boolean cfgOnly) {
        TTY.println("IR for " + compilation.method);
        final InstructionPrinter ip = new InstructionPrinter(TTY.out, true, compilation.target);
        final BlockPrinter bp = new BlockPrinter(this, ip, cfgOnly, false);
        startBlock.iteratePreOrder(bp);
    }

    /**
     * Verifies the IR and prints it out if the relevant options are set.
     * @param phase the name of the phase for printing
     */
    public void verifyAndPrint(String phase) {
        if (C1XOptions.IRChecking) {
            new IRChecker(this, phase).check();
        }
        CFGPrinter cfgPrinter = compilation.cfgPrinter();
        if (C1XOptions.PrintCFGToFile && cfgPrinter != null) {
            cfgPrinter.printCFG(startBlock, phase, true, false);
        }
        if (C1XOptions.PrintHIR) {
            TTY.println(phase);
            print(false);
        }
    }

    /**
     * Creates and inserts a new block between this block and the specified successor,
     * altering the successor and predecessor lists of involved blocks appropriately.
     * @param source the source of the edge
     * @param target the successor before which to insert a block
     * @return the new block inserted
     */
    public BlockBegin splitEdge(BlockBegin source, BlockBegin target) {
        int bci;
        if (target.predecessors().size() == 1) {
            bci = target.bci();
        } else {
            bci = source.end().bci();
        }

        // create new successor and mark it for special block order treatment
        BlockBegin newSucc = new BlockBegin(bci, nextBlockNumber());

        newSucc.setCriticalEdgeSplit(true);

        // This goto is not a safepoint.
        Goto e = new Goto(target, null, false);
        newSucc.setNext(e, bci);
        newSucc.setEnd(e);
        // setup states
        ValueStack s = source.end().stateAfter();
        newSucc.setStateBefore(s.copy());
        e.setStateAfter(s.immutableCopy());
        assert newSucc.stateBefore().localsSize() == s.localsSize();
        assert newSucc.stateBefore().stackSize() == s.stackSize();
        assert newSucc.stateBefore().locksSize() == s.locksSize();
        // link predecessor to new block
        source.end().substituteSuccessor(target, newSucc);

        // The ordering needs to be the same, so remove the link that the
        // set_end call above added and substitute the new_sux for this
        // block.
        target.removePredecessor(newSucc);

        // the successor could be the target of a switch so it might have
        // multiple copies of this predecessor, so substitute the new_sux
        // for the first and delete the rest.
        List<BlockBegin> list = target.predecessors();
        int x = list.indexOf(source);
        assert x >= 0;
        list.set(x, newSucc);
        newSucc.addPredecessor(source);
        Iterator<BlockBegin> iterator = list.iterator();
        while (iterator.hasNext()) {
            if (iterator.next() == source) {
                iterator.remove();
                newSucc.addPredecessor(source);
            }
        }
        return newSucc;
    }

    public void replaceBlock(BlockBegin oldBlock, BlockBegin newBlock) {
        assert !oldBlock.isExceptionEntry() : "cannot replace exception handler blocks (yet)";
        for (BlockBegin succ : oldBlock.end().successors()) {
            succ.removePredecessor(oldBlock);
        }
        for (BlockBegin pred : oldBlock.predecessors()) {
            // substitute the new successor for this block in each predecessor
            pred.end().substituteSuccessor(oldBlock, newBlock);
            // and add each predecessor to the successor
            newBlock.addPredecessor(pred);
        }
        // this block is now disconnected; remove all its incoming and outgoing edges
        oldBlock.predecessors().clear();
        oldBlock.end().successors().clear();
    }

    /**
     * Disconnects the specified block from all other blocks.
     * @param block the block to remove from the graph
     */
    public void disconnectFromGraph(BlockBegin block) {
        for (BlockBegin p : block.predecessors()) {
            p.end().successors().remove(block);
        }
        for (BlockBegin s : block.end().successors()) {
            s.predecessors().remove(block);
        }
    }

    public int nextBlockNumber() {
        return compilation.stats.blockCount++;
    }

    public int numberOfBlocks() {
        return compilation.stats.blockCount;
    }

    public int numLoops() {
        return compilation.stats.loopCount;
    }
}
