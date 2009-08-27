/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.debug;

import static com.sun.c1x.ir.Instruction.*;

import java.io.*;
import java.util.*;

import com.sun.c1x.alloc.*;
import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;

/**
 * Utility for printing the control flow graph of a method being compiled by C1X at various compilation phases.
 * The output format matches that produced by HotSpot so that it can then be fed to the
 * <a href="https://c1visualizer.dev.java.net/">C1 Visualizer</a>.
 *
 * @author Doug Simon
 */
public class CFGPrinter {

    private static OutputStream cfgFileStream;

    /**
     * Gets the output stream  on the file "output.cfg" in the current working directory.
     * This stream is first opened if necessary.
     *
     * @return the output stream to "output.cfg" or {@code null} if there was an error opening this file for writing
     */
    public static synchronized OutputStream cfgFileStream() {
        if (cfgFileStream == null) {
            File cfgFile = new File("output.cfg");
            try {
                cfgFileStream = new FileOutputStream(cfgFile);
            } catch (FileNotFoundException e) {
                TTY.println("WARNING: Cound not open " + cfgFile.getAbsolutePath());
            }
        }
        return cfgFileStream;
    }

    private final LogStream out;

    /**
     * Creates a control flow graph printer.
     *
     * @param os where the output generated via this printer shown be written
     */
    public CFGPrinter(OutputStream os) {
        out = new LogStream(os);
    }

    private void begin(String string) {
        out.println("begin_" + string);
        out.adjustIndentation(2);
    }

    private void end(String string) {
        out.adjustIndentation(-2);
        out.println("end_" + string);
    }

    /**
     * Prints a compilation timestamp for a given method.
     *
     * @param method the method for which a timestamp will be printed
     */
    public void printCompilation(RiMethod method) {
        begin("compilation");
        out.print("name \" ").print(Util.format("%H::%n", method, true)).println('"');
        out.print("method \"").print(Util.format("%f %r %H.%n(%p)", method, true)).println('"');
        out.print("date ").println(System.currentTimeMillis());
        end("compilation");
    }

    /**
     * Print the details of a given control flow graph block.
     *
     * @param block the block to print
     * @param successors the successor blocks of {@code block}
     * @param handlers the exception handler blocks of {@code block}
     * @param printHIR if {@code true} the HIR for each instruction in the block will be printed
     * @param printLIR if {@code true} the LIR for each instruction in the block will be printed
     */
    void printBlock(BlockBegin block, List<BlockBegin> successors, Iterable<BlockBegin> handlers, boolean printHIR, boolean printLIR) {
        begin("block");

        out.print("name \"B").print(block.blockID).println('"');
        out.print("from_bci ").println(block.bci());
        out.print("to_bci ").println(block.end() == null ? -1 : block.end().bci());

        out.print("predecessors ");
        for (BlockBegin pred : block.predecessors()) {
            out.print("\"B").print(pred.blockID).print("\" ");
        }
        out.println();

        out.print("successors ");
        for (BlockBegin succ : successors) {
            out.print("\"B").print(succ.blockID).print("\" ");
        }
        out.println();

        out.print("xhandlers");
        for (BlockBegin handler : handlers) {
            out.print("\"B").print(handler.blockID).print("\" ");
        }
        out.println();

        out.print("flags ");
        if (block.isStandardEntry()) {
            out.print("\"std\" ");
        }
        if (block.isOsrEntry()) {
            out.print("\"osr\" ");
        }
        if (block.isExceptionEntry()) {
            out.print("\"ex\" ");
        }
        if (block.isSubroutineEntry()) {
            out.print("\"sr\" ");
        }
        if (block.isBackwardBranchTarget()) {
            out.print("\"bb\" ");
        }
        if (block.isParserLoopHeader()) {
            out.print("\"plh\" ");
        }
        if (block.isCriticalEdgeSplit()) {
            out.print("\"ces\" ");
        }
        if (block.isLinearScanLoopHeader()) {
            out.print("\"llh\" ");
        }
        if (block.isLinearScanLoopEnd()) {
            out.print("\"lle\" ");
        }
        out.println();

        if (block.dominator() != null) {
            out.print("dominator \"B").print(block.dominator().blockID).println('"');
        }
        if (block.loopIndex() != -1) {
            out.print("loop_index ").println(block.loopIndex());
            out.print("loop_depth ").println(block.loopDepth());
        }

        /* TODO: Uncomment (and fix) once LIR is implemented
        if (block.firstLirInstructionId() != -1) {
            _out.print("first_lir_id ").println(block.firstLirInstructionId());
            _out.print("last_lir_id ").println(block.lastLirInstructionId());
        }
        */

        if (printHIR) {
            printState(block);
            printHIR(block);
        }

        if (printLIR) {
            printLIR(block);
        }

        end("block");
    }

    /**
     * Prints the JVM frame states pertaining to a given block. There will be more than one
     * frame state if this block is in a method that has been inlined.
     *
     * @param block the block for which the frame state is to be printed
     */
    private void printState(BlockBegin block) {
        begin("states");

        ValueStack state = block.stateBefore();

        if (state.stackSize() > 0) {
          begin("stack");
          out.print("size ").println(state.stackSize());

          int i = 0;
          while (i < state.stackSize()) {
              Instruction value = state.stackAt(i);
              out.disableIndentation();
              out.print(InstructionPrinter.stateString(i, value, block));
              printLirOperand(value);
              out.println();
              out.enableIndentation();
              i += value.type().sizeInSlots();
          }
          end("stack");
        }

        if (state.locksSize() > 0) {
            begin("locks");
            out.print("size ").println(state.locksSize());

            for (int i = 0; i < state.locksSize(); ++i) {
                Instruction value = state.lockAt(i);
                out.disableIndentation();
                out.print(InstructionPrinter.stateString(i, value, block));
                printLirOperand(value);
                out.println();
                out.enableIndentation();
            }
            end("locks");
        }

        do {
            begin("locals");
            out.print("size ").println(state.localsSize());
            out.print("method \"").print(Util.format("%f %r %H.%n(%p)", state.scope().method, true)).println('"');
            int i = 0;
            while (i < state.localsSize()) {
                Instruction value = state.localAt(i);
                if (value != null) {
                    out.disableIndentation();
                    out.print(InstructionPrinter.stateString(i, value, block));
                    printLirOperand(value);
                    out.println();
                    out.enableIndentation();
                    // also ignore illegal HiWords
                    i += value.isIllegal() ? 1 : value.type().sizeInSlots();
                } else {
                    i++;
                }
            }
            state = state.scope().callerState();
            end("locals");
        } while (state != null);

        end("states");
    }

    /**
     * Prints the HIR for each instruction in a given block.
     *
     * @param block
     */
    private void printHIR(BlockBegin block) {
        begin("HIR");
        out.disableIndentation();
        for (Instruction i = block.next(); i != null; i = i.next()) {
            assert i.next() == null || !Instruction.valueString(i).equals(valueString(i.next()));
            printInstructionHIR(i);
        }
        out.enableIndentation();
        end("HIR");
    }

    /**
     * Prints the LIR for each instruction in a given block.
     *
     * @param block the block to print
     */
    private void printLIR(BlockBegin block) {
        LIRList lir = block.lir();
        if (lir != null) {
            begin("LIR");
            for (int i = 0; i < lir.length(); i++) {
                lir.at(i).printOn(out);
                out.println(" <|@ ");
            }
            end("LIR");
        }
    }

    private void printLirOperand(Instruction i) {
        if (i != null && i.operand().isVirtual()) {
            out.print(" \"").print(i.operand().toString()).print("\" ");
        }
    }

    /**
     * Prints the HIR for a given instruction.
     *
     * @param i the instruction for which HIR will be printed
     */
    private void printInstructionHIR(Instruction i) {
        if (i.isLive()) {
            out.print('.');
        }
        int useCount = 0;
        out.print(i.bci()).print(' ').print(useCount).print(' ');
        printLirOperand(i);
        out.print(i).print(' ');
        new InstructionPrinter(out, true).printInstruction(i);

        out.println(" <|@");
    }

    /**
     * Prints the control flow graph denoted by a given block map.
     *
     * @param blockMap a data structure describing the blocks in a method and how they are connected
     * @param codeSize the bytecode size of the method from which {@code blockMap} was produced
     * @param label a label describing the compilation phase that produced the control flow graph
     * @param printHIR if {@code true} the HIR for each instruction in the block will be printed
     * @param printLIR if {@code true} the LIR for each instruction in the block will be printed
     */
    public void printCFG(RiMethod method, BlockMap blockMap, int codeSize, String label, boolean printHIR, boolean printLIR) {
        begin("cfg");
        out.print("name \"").print(label).println('"');
        for (int bci = 0; bci < codeSize; ++bci) {
            BlockBegin block = blockMap.get(bci);
            if (block != null) {
                printBlock(block, Arrays.asList(blockMap.getSuccessors(block)), blockMap.getHandlers(block), printHIR, printLIR);
            }
        }
        end("cfg");
    }

    /**
     * Prints the control flow graph rooted at a given block.
     *
     * @param startBlock the entry block of the control flow graph to be printed
     * @param label a label describing the compilation phase that produced the control flow graph
     * @param printHIR if {@code true} the HIR for each instruction in the block will be printed
     * @param printLIR if {@code true} the LIR for each instruction in the block will be printed
     */
    public void printCFG(BlockBegin startBlock, String label, final boolean printHIR, final boolean printLIR) {
        begin("cfg");
        out.print("name \"").print(label).println('"');
        startBlock.iteratePreOrder(new BlockClosure() {
            public void apply(BlockBegin block) {
                printBlock(block, block.end().successors(), block.exceptionHandlerBlocks(), printHIR, printLIR);
            }
        });
        end("cfg");
    }

    public void printIntervals(LinearScan allocator, List<Interval> intervals, String name) {
        begin("intervals");
        out.print(String.format("name \"%s\"", name));

        for (int i = 0; i < intervals.size(); i++) {
          if (intervals.get(i) != null) {
            intervals.get(i).print(out, allocator);
          }
        }

        end("intervals");
    }
}
