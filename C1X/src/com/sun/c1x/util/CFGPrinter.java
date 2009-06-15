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
package com.sun.c1x.util;

import static com.sun.c1x.ir.Instruction.*;

import java.io.*;
import java.util.*;

import com.sun.c1x.ci.*;
import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.value.*;

/**
 * Utility for printing the control flow graph of a method being compiled by C1X at various compilation phases.
 * The output format matches that produced by HotSpot so that it can then be fed to the
 * <a href="https://c1visualizer.dev.java.net/">C1 Visualizer</a>.
 *
 * @author Doug Simon
 */
public class CFGPrinter {

    private static OutputStream _cfgFileStream;

    /**
     * Gets the output stream  on the file "output.cfg" in the current working directory.
     * This stream is first opened if necessary.
     *
     * @return the output stream to "output.cfg" or {@code null} if there was an error opening this file for writing
     */
    public static synchronized OutputStream cfgFileStream() {
        if (_cfgFileStream == null) {
            File cfgFile = new File("output.cfg");
            try {
                _cfgFileStream = new FileOutputStream(cfgFile);
            } catch (FileNotFoundException e) {
                TTY.println("WARNING: Cound not open " + cfgFile.getAbsolutePath());
            }
        }
        return _cfgFileStream;
    }

    private final LogStream _out;

    /**
     * Creates a control flow graph printer.
     *
     * @param os where the output generated via this printer shown be written
     */
    public CFGPrinter(OutputStream os) {
        _out = new LogStream(os);
    }

    private void begin(String string) {
        _out.println("begin_" + string);
        _out.adjustIndentation(2);
    }

    private void end(String string) {
        _out.adjustIndentation(-2);
        _out.println("end_" + string);
    }

    /**
     * Prints a compilation timestamp for a given method.
     *
     * @param method the method for which a timestamp will be printed
     */
    public void printCompilation(CiMethod method) {
        begin("compilation");
        _out.print("name \" ").print(Util.format("%H::%n", method, true)).println('"');
        _out.print("method \"").print(Util.format("%f %r %H.%n(%p)", method, true)).println('"');
        _out.print("date ").println(System.currentTimeMillis());
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

        _out.print("name \"B").print(block.blockID()).println('"');
        _out.print("from_bci ").println(block.bci());
        _out.print("to_bci ").println(block.end() == null ? -1 : block.end().bci());

        _out.print("predecessors ");
        for (BlockBegin pred : block.predecessors()) {
          _out.print("\"B").print(pred.blockID()).print("\" ");
        }
        _out.println();

        _out.print("successors ");
        for (BlockBegin succ : successors) {
            _out.print("\"B").print(succ.blockID()).print("\" ");
        }
        _out.println();

        _out.print("xhandlers");
        for (BlockBegin handler : handlers) {
            _out.print("\"B").print(handler.blockID()).print("\" ");
        }
        _out.println();

        _out.print("flags ");
        if (block.isStandardEntry()) {
            _out.print("\"std\" ");
        }
        if (block.isOsrEntry()) {
            _out.print("\"osr\" ");
        }
        if (block.isExceptionEntry()) {
            _out.print("\"ex\" ");
        }
        if (block.isSubroutineEntry()) {
            _out.print("\"sr\" ");
        }
        if (block.isBackwardBranchTarget()) {
            _out.print("\"bb\" ");
        }
        if (block.isParserLoopHeader()) {
            _out.print("\"plh\" ");
        }
        if (block.isCriticalEdgeSplit()) {
            _out.print("\"ces\" ");
        }
        if (block.isLinearScanLoopHeader()) {
            _out.print("\"llh\" ");
        }
        if (block.isLinearScanLoopEnd()) {
            _out.print("\"lle\" ");
        }
        _out.println();

        if (block.dominator() != null) {
            _out.print("dominator \"B").print(block.dominator().blockID()).println('"');
        }
        if (block.loopIndex() != -1) {
            _out.print("loop_index ").println(block.loopIndex());
            _out.print("loop_depth ").println(block.loopDepth());
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

        ValueStack state = block.state();

        if (state.stackSize() > 0) {
          begin("stack");
          _out.print("size ").println(state.stackSize());

          int i = 0;
          while (i < state.stackSize()) {
              Instruction value = state.stackAt(i);
              _out.disableIndentation();
              _out.print(stateString(i, value, block));
              printLirOperand(value);
              _out.println();
              _out.enableIndentation();
              i += value.type().size();
          }
          end("stack");
        }

        if (state.locksSize() > 0) {
            begin("locks");
            _out.print("size ").println(state.locksSize());

            for (int i = 0; i < state.locksSize(); ++i) {
                Instruction value = state.lockAt(i);
                _out.disableIndentation();
                _out.print(stateString(i, value, block));
                printLirOperand(value);
                _out.println();
                _out.enableIndentation();
            }
            end("locks");
        }

        do {
            begin("locals");
            _out.print("size ").println(state.localsSize());
            _out.print("method \"").print(Util.format("%f %r %H.%n(%p)", state.scope().method, true)).println('"');
            int i = 0;
            while (i < state.localsSize()) {
                Instruction value = state.localAt(i);
                if (value != null) {
                    _out.disableIndentation();
                    _out.print(stateString(i, value, block));
                    printLirOperand(value);
                    _out.println();
                    _out.enableIndentation();
                    // also ignore illegal HiWords
                    i += value.type().isIllegal() ? 1 : value.type().size();
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
        _out.disableIndentation();
        for (Instruction i = block.next(); i != null; i = i.next()) {
            assert i.next() == null || !Instruction.valueString(i).equals(valueString(i.next()));
            printInstructionHIR(i);
        }
        _out.enableIndentation();
        end("HIR");
    }

    /**
     * Prints the LIR for each instruction in a given block.
     *
     * @param block
     */
    private void printLIR(BlockBegin block) {
        begin("LIR");
        // TODO: Complete once LIR is implemented
        end("LIR");
    }

    private void printLirOperand(Instruction i) {
        /* TODO: Uncomment (and fix) once LIR is implemented
        if (i.operand().isVirtual()) {
            _out.print(" \"").print(i.lirOperand()).print("\" ");
        }
        */
    }

    /**
     * Prints the HIR for a given instruction.
     *
     * @param i the instruction for which HIR will be printed
     */
    private void printInstructionHIR(Instruction i) {
        if (i.isPinned()) {
            _out.print('.');
        }
        int useCount = 0;
        _out.print(i.bci()).print(' ').print(useCount).print(' ');
        printLirOperand(i);
        _out.print(i).print(' ');
        new InstructionPrinter(_out, true).printInstruction(i);

        _out.println(" <|@");
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
    public void printCFG(BlockMap blockMap, int codeSize, String label, boolean printHIR, boolean printLIR) {
        begin("cfg");
        _out.print("name \"").print(label).println('"');
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
        _out.print("name \"").print(label).println('"');
        startBlock.iteratePreOrder(new BlockClosure() {
            public void apply(BlockBegin block) {
                printBlock(block, block.end().successors(), block.exceptionHandlerBlocks(), printHIR, printLIR);
            }
        });
        end("cfg");
    }
}
