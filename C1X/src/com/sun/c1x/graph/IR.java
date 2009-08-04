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

import com.sun.c1x.C1XCompilation;
import com.sun.c1x.C1XOptions;
import com.sun.c1x.util.Util;
import com.sun.c1x.debug.*;
import com.sun.c1x.ir.BlockBegin;
import com.sun.c1x.ir.ComputeLinearScanOrder;
import com.sun.c1x.ir.Goto;
import com.sun.c1x.ir.IRScope;
import com.sun.c1x.opt.CEEliminator;
import com.sun.c1x.opt.GlobalValueNumberer;
import com.sun.c1x.opt.NullCheckEliminator;
import com.sun.c1x.opt.BlockMerger;
import com.sun.c1x.value.ValueStack;
import java.util.Iterator;
import java.util.List;

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

    int totalBlocks = 1;
    int totalInstructions;

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
        buildGraph();
        verifyAndPrint("After graph building");
        optimize1();
        verifyAndPrint("After optimizations");
        computeLinearScanOrder();
        verifyAndPrint("After linear scan order");
        optimize2();
        verifyAndPrint("After global optimizations");
    }

    private void buildGraph() {
        topScope = new IRScope(compilation, null, -1, compilation.method, compilation.osrBCI);

        // Graph builder must set the startBlock and the osrEntryBlock
        GraphBuilder g = new GraphBuilder(compilation, topScope, this);
        totalInstructions += g.totalInstructions();
        assert startBlock != null;

    }

    private void optimize1() {
        // do basic optimizations
        if (C1XOptions.DoCEElimination) {
            new CEEliminator(this);
        }
        if (C1XOptions.DoBlockMerging) {
            new BlockMerger(this);
        }
        if (C1XOptions.DoNullCheckElimination) {
            new NullCheckEliminator(this);
        }
    }

    private void computeLinearScanOrder() {
        CriticalEdgeFinder finder = new CriticalEdgeFinder(this);
        startBlock.iteratePreOrder(finder);
        finder.splitCriticalEdges();

        if (C1XOptions.GenerateLIR && C1XOptions.GenerateAssembly) {
            makeLinearScanOrder();
        }
    }

    private void makeLinearScanOrder() {
        if (orderedBlocks == null) {
            ComputeLinearScanOrder computeLinearScanOrder = new ComputeLinearScanOrder(totalBlocks, startBlock);
            orderedBlocks = computeLinearScanOrder.linearScanOrder();
            computeLinearScanOrder.printBlocks();
        }
    }

    private void optimize2() {
        // do more advanced, dominator-based optimizations
        if (C1XOptions.DoGlobalValueNumbering) {
            makeLinearScanOrder();
            new GlobalValueNumberer(this);
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
        final InstructionPrinter ip = new InstructionPrinter(TTY.out, true);
        final BlockPrinter bp = new BlockPrinter(this, ip, cfgOnly, false);
        startBlock.iteratePreOrder(bp);
    }

    /**
     * Verifies the IR and prints it out if the relevant options are set.
     * @param phase the name of the phase for printing
     */
    public void verifyAndPrint(String phase) {
        if (C1XOptions.TypeChecking) {
            startBlock.iteratePreOrder(new IRChecker(this));
        }
        CFGPrinter cfgPrinter = compilation.cfgPrinter();
        if (C1XOptions.PrintCFGToFile && cfgPrinter != null) {
            cfgPrinter.printCFG(startBlock, phase, true, false);
        }

         if (C1XOptions.PrintIR) {
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
        newSucc.setBlockFlag(BlockBegin.BlockFlag.CriticalEdgeSplit);

        // This goto is not a safepoint.
        Goto e = new Goto(target, null, false);
        newSucc.setNext(e, bci);
        newSucc.setEnd(e);
        // setup states
        ValueStack s = source.end().state();
        newSucc.setState(s.copy());
        e.setState(s.copy());
        assert newSucc.state().localsSize() == s.localsSize();
        assert newSucc.state().stackSize() == s.stackSize();
        assert newSucc.state().locksSize() == s.locksSize();
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
        return totalBlocks++;
    }

    public int numberOfBlocks() {
        return totalBlocks;
    }

    public int totalInstructions() {
        return totalInstructions;
    }

    public void incrementNumberOfBlocks(int i) {
        totalBlocks += i;
    }

    public int numLoops() {
        return Util.nonFatalUnimplemented(0);
    }
}
