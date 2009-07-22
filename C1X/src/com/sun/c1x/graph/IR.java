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
 * @author Thomas Wuerthinger
 *
 */
public class IR {

    public BlockBegin startBlock;
    BlockBegin osrEntryBlock;
    private List<BlockBegin> orderedBlocks;
    private Map<Instruction, Integer> useCounts;
    public IRScope topScope;
    private final C1XCompilation compilation;

    public IR(C1XCompilation compilation) {
        this.compilation = compilation;
    }

    // ir manipulation
    void optimize() {

    }

    void computePredecessors() {

    }

    void splitCriticalEdges() {

    }

    void computeCode() {
        ComputeLinearScanOrder computeLinearScanOrder = new ComputeLinearScanOrder(compilation.numberOfBlocks(), startBlock);
        orderedBlocks = computeLinearScanOrder.linearScanOrder();
        computeLinearScanOrder.printBlocks();

        if (C1XOptions.DoGlobalValueNumbering) {
            new GlobalValueNumbering(this);
        }
    }

    void computeUseCounts() {
        UseCountComputer useCountComputer = new UseCountComputer();
        this.iterateLinearScanOrder(useCountComputer);
        this.useCounts = useCountComputer.result();
    }

    public List<BlockBegin> linearScanOrder() {
        return orderedBlocks;
    }


    public void iterateLinearScanOrder(BlockClosure closure) {
        assert orderedBlocks != null;
        for (BlockBegin begin : this.orderedBlocks) {
            closure.apply(begin);
        }
    }

    public int useCount(Instruction instr) {
        if (!this.useCounts.containsKey(instr)) {
            return 0;
        } else {
            return this.useCounts.get(instr);
        }
    }

    public boolean hasUses(Instruction instr) {
        return useCount(instr) > 0;
    }

    public void build() {

        CFGPrinter cfgPrinter = compilation.cfgPrinter();

        topScope = new IRScope(compilation, null, -1, compilation.method, compilation.osrBCI());

        // Graph builder must set the startBlock and the osrEntryBlock
        new GraphBuilder(compilation, topScope, this);
        assert startBlock != null;

        if (C1XOptions.PrintCFGToFile && cfgPrinter != null) {
            cfgPrinter.printCFG(startBlock, "After Generation of HIR", true, false);
        }

        if (C1XOptions.PrintCFG) {
            TTY.println("CFG after parsing");
            print(true);
        }

        if (C1XOptions.PrintIR) {
            TTY.println("IR after parsing");
            print(false);
        }

        assert verify();
        optimize();

        assert verify();

        splitCriticalEdges();

        if (C1XOptions.PrintCFG) {
            TTY.println("CFG after optimizations");
            print(true);
        }

        if (C1XOptions.PrintIR) {
            TTY.println("IR after optimizations");
            print(false);
        }

        assert verify();

        // compute block ordering for code generation
        // the control flow must not be changed from here on
        computeCode();

        // compute use counts after global value numbering
        computeUseCounts();

        if (C1XOptions.PrintCFG) {
            TTY.println("CFG before code generation");
            printBlocks(true, false);
        }

        if (C1XOptions.PrintIR) {
            TTY.println("IR before code generation");
            printBlocks(false, true);
        }

        assert verify();

    }

    private void printBlocks(boolean cfgOnly, boolean liveOnly) {
        InstructionPrinter ip = new InstructionPrinter(TTY.out, false);
        for (BlockBegin block : linearScanOrder()) {
          if (cfgOnly) {
            ip.printInstruction(block); TTY.println();
          } else {

              BlockPrinter blockPrinter = new BlockPrinter(this, ip, cfgOnly, liveOnly);
              blockPrinter.printBlock(block, liveOnly);
          }
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
