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
import com.sun.c1x.opt.GlobalValueNumbering;
import com.sun.c1x.ir.*;
import com.sun.c1x.util.*;

/**
 * This class implements the overall container for the HIR (high-level IR) graph
 * and directs its construction, optimization, and finalization.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public class IR {

    public final C1XCompilation compilation;
    public BlockBegin startBlock;
    public BlockBegin osrEntryBlock;
    public IRScope topScope;
    private List<BlockBegin> orderedBlocks;

    public IR(C1XCompilation compilation) {
        this.compilation = compilation;
    }

    /**
     * Builds the graph and optimizes it.
     */
    public void build() {
        topScope = new IRScope(compilation, null, -1, compilation.method, compilation.osrBCI());

        // Graph builder must set the startBlock and the osrEntryBlock
        new GraphBuilder(compilation, topScope, this);
        assert startBlock != null;

        print("After graph building");

        assert verify();

        optimize();

        assert verify();

        splitCriticalEdges();

        print("After optimizations");

        assert verify();

        // compute block ordering for code generation
        // the control flow must not be changed from here on
        computeLinearScanOrder();

        print("Before code generation");

        assert verify();
    }

    void optimize() {

    }

    void splitCriticalEdges() {
        // TODO: split critical edges
    }

    void computeLinearScanOrder() {
        ComputeLinearScanOrder computeLinearScanOrder = new ComputeLinearScanOrder(compilation.numberOfBlocks(), startBlock);
        orderedBlocks = computeLinearScanOrder.linearScanOrder();
        computeLinearScanOrder.printBlocks();

        if (C1XOptions.DoGlobalValueNumbering) {
            new GlobalValueNumbering(this);
        }
    }

    /**
     * Gets the linear scan ordering of blocks.
     * @return the blocks in linear scan order
     */
    public List<BlockBegin> linearScanOrder() {
        return orderedBlocks;
    }

    private void print(String phase) {
        CFGPrinter cfgPrinter = compilation.cfgPrinter();
        if (C1XOptions.PrintCFGToFile && cfgPrinter != null) {
            cfgPrinter.printCFG(startBlock, phase, true, false);
        }

        if (C1XOptions.PrintCFG) {
            TTY.println(phase);
            print(true);
        }

        if (C1XOptions.PrintIR) {
            TTY.println(phase);
            print(false);
        }
    }

    private void print(boolean cfgOnly) {
        TTY.println("IR for " + compilation.method);
        final InstructionPrinter ip = new InstructionPrinter(TTY.out, true);
        final BlockPrinter bp = new BlockPrinter(this, ip, cfgOnly, false);
        startBlock.iteratePreOrder(bp);
    }

    public boolean verify() {
        return true;
    }

    public int numLoops() {
        // TODO Auto-generated method stub
        return 0;
    }

    public BlockBegin start() {
        return startBlock;
    }
}
