/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.max.graal.compiler.debug;

import java.io.*;
import java.util.*;

import com.oracle.max.criutils.*;
import com.oracle.max.graal.compiler.*;
import com.oracle.max.graal.compiler.alloc.*;
import com.oracle.max.graal.compiler.alloc.Interval.UsePosList;
import com.oracle.max.graal.compiler.graph.*;
import com.oracle.max.graal.compiler.lir.*;
import com.oracle.max.graal.compiler.schedule.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * Utility for printing Graal IR at various compilation phases.
 */
public class CFGPrinter extends CompilationPrinter {

    private final CiTarget target;

    /**
     * Creates a control flow graph printer.
     *
     * @param os where the output generated via this printer shown be written
     * @param target the target architecture description
     */
    public CFGPrinter(OutputStream os, CiTarget target) {
        super(os);
        this.target = target;
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
    void printBlock(Block block, List<Block> successors, Block handler, boolean printHIR, boolean printLIR) {
        begin("block");

        out.print("name \"B").print(block.blockID()).println('"');
        out.print("from_bci -1");
        out.print("to_bci -1");

        out.print("predecessors ");
        for (Block pred : block.getPredecessors()) {
            out.print("\"B").print(pred.blockID()).print("\" ");
        }
        out.println();

        out.print("successors ");
        for (Block succ : successors) {
            out.print("\"B").print(succ.blockID()).print("\" ");
        }
        out.println();

        out.print("xhandlers");
        if (handler != null) {
            out.print("\"B").print(handler.blockID()).print("\" ");
        }
        out.println();

        out.print("flags ");
        out.println();

        out.print("loop_index ").println(-1);
        out.print("loop_depth ").println(-1);

        if (printHIR) {
            printHIR(block);
        }

        // TODO(tw): Add possibility to print LIR.
        //if (printLIR) {
        //    printLIR(block.lirBlock());
        //}

        end("block");
    }

    /**
     * Prints the JVM frame state upon entry to a given block.
     *
     * @param block the block for which the frame state is to be printed
     */
    /*private void printState(Block block) {
        begin("states");

        FrameState state = block.stateBefore();
        if (state == null) {
            return;
        }
        int stackSize = state.stackSize();
        if (stackSize > 0) {
            begin("stack");
            out.print("size ").println(stackSize);
            out.print("method \"").print(CiUtil.toLocation(GraalCompilation.compilation().method, state.bci)).println('"');

            int i = 0;
            while (i < stackSize) {
                Value value = state.stackAt(i);
                out.disableIndentation();
                out.print(block.stateString(i, value));
                printOperand(value);
                out.println();
                out.enableIndentation();
                if (value == null) {
                    i++;
                } else {
                    i += value.kind.sizeInSlots();
                }
            }
            end("stack");
        }

        if (state.locksSize() > 0) {
            begin("locks");
            out.print("size ").println(state.locksSize());
            out.print("method \"").print(CiUtil.toLocation(GraalCompilation.compilation().method, state.bci)).println('"');

            for (int i = 0; i < state.locksSize(); ++i) {
                Value value = state.lockAt(i);
                out.disableIndentation();
                out.print(block.stateString(i, value));
                printOperand(value);
                out.println();
                out.enableIndentation();
            }
            end("locks");
        }

        begin("locals");
        out.print("size ").println(state.localsSize());
        out.print("method \"").print(CiUtil.toLocation(GraalCompilation.compilation().method, state.bci)).println('"');
        int i = 0;
        while (i < state.localsSize()) {
            Value value = state.localAt(i);
            if (value != null) {
                out.disableIndentation();
                out.print(block.stateString(i, value));
                printOperand(value);
                out.println();
                out.enableIndentation();
                // also ignore illegal HiWords
                i += value.isIllegal() ? 1 : value.kind.sizeInSlots();
            } else {
                i++;
            }
        }
        end("locals");
        end("states");
    }*/

    /**
     * Formats a given {@linkplain FrameState JVM frame state} as a multi line string.
     */
    private String stateToString(FrameState state, OperandFormatter operandFmt) {
        if (state == null) {
            return null;
        }

        StringBuilder buf = new StringBuilder();
        buf.append(CiUtil.toLocation(GraalCompilation.compilation().method, state.bci));
        buf.append('\n');
        if (state.stackSize() > 0) {
            int i = 0;
            buf.append("stack: ");
            while (i < state.stackSize()) {
                if (i == 0) {
                    buf.append(' ');
                }
                ValueNode value = state.stackAt(i);
                buf.append(stateValueToString(value, operandFmt)).append(' ');
                i++;
            }
            buf.append("\n");
        }

        if (state.locksSize() > 0) {
            buf.append("locks: ");
            for (int i = 0; i < state.locksSize(); ++i) {
                if (i == 0) {
                    buf.append(' ');
                }
                ValueNode value = state.lockAt(i);
                buf.append(stateValueToString(value, operandFmt)).append(' ');
            }
            buf.append("\n");
        }

        buf.append("locals: ");
        int i = 0;
        while (i < state.localsSize()) {
            if (i == 0) {
                buf.append(' ');
            }
            ValueNode value = state.localAt(i);
            buf.append(stateValueToString(value, operandFmt)).append(' ');
            i++;
        }
        buf.append("\n");
        return buf.toString();
    }

    private String stateValueToString(ValueNode value, OperandFormatter operandFmt) {
        if (operandFmt == null) {
            return ValueUtil.valueString(value);
        }
        if (value == null) {
            return "-";
        }
        return operandFmt.format(value.operand());
    }

    /**
     * Prints the HIR for each instruction in a given block.
     *
     * @param block
     */
    private void printHIR(Block block) {
        begin("IR");
        out.println("HIR");
        out.disableIndentation();
        for (Node i : block.getInstructions()) {
            if (i instanceof FixedWithNextNode) {
                printInstructionHIR((FixedWithNextNode) i);
            }
        }
        out.enableIndentation();
        end("IR");
    }

    /**
     * Prints the LIR for each instruction in a given block.
     *
     * @param block the block to print
     */
    private void printLIR(LIRBlock block) {
        LIRList lir = block.lir();
        if (lir != null) {
            begin("IR");
            out.println("LIR");
            for (int i = 0; i < lir.length(); i++) {
                LIRInstruction inst = lir.at(i);
                out.printf("nr %4d ", inst.id()).print(COLUMN_END);

                if (inst.info != null) {
                    int level = out.indentationLevel();
                    out.adjustIndentation(-level);
                    String state;
                    if (inst.info.debugInfo != null) {
                        // Use register-allocator output if available
                        state = debugInfoToString(inst.info.debugInfo, new OperandFormatter(false), target.arch);
                    } else {
                        state = stateToString(inst.info.state, new OperandFormatter(false));
                    }
                    if (state != null) {
                        out.print(" st ").print(HOVER_START).print("st").print(HOVER_SEP).print(state).print(HOVER_END).print(COLUMN_END);
                    }
                    out.adjustIndentation(level);
                }

                out.print(" instruction ").print(inst.toString(new OperandFormatter(false))).print(COLUMN_END);
                out.println(COLUMN_END);
            }
            end("IR");
        }
    }

    private void printOperand(ValueNode i) {
        if (i != null && i.operand().isLegal()) {
            out.print(new OperandFormatter(true).format(i.operand()));
        }
    }

    /**
     * Prints the HIR for a given instruction.
     *
     * @param i the instruction for which HIR will be printed
     */
    private void printInstructionHIR(FixedWithNextNode i) {
        out.print("bci ").print(-1).println(COLUMN_END);
        if (i.operand().isLegal()) {
            out.print("result ").print(new OperandFormatter(false).format(i.operand())).println(COLUMN_END);
        }
        out.print("tid ").print(ValueUtil.valueString(i)).println(COLUMN_END);

        if (i instanceof StateSplit) {
            StateSplit stateSplit = (StateSplit) i;
            String state = stateToString(stateSplit.stateAfter(), null);
            if (state != null) {
                out.print("st ").print(HOVER_START).print("st").print(HOVER_SEP).print(state).print(HOVER_END).println(COLUMN_END);
            }
        }

        out.print("instruction ");
        out.print(i.toString());
        out.print(COLUMN_END).print(' ').println(COLUMN_END);
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
        for (BlockMap.Block block : blockMap.blocks) {
            begin("block");
            blockMap.printBlock(block, out);
            end("block");
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
    public void printCFG(Block startBlock, String label, final boolean printHIR, final boolean printLIR) {
        begin("cfg");
        out.print("name \"").print(label).println('"');
        startBlock.iteratePreOrder(new BlockClosure() {
            public void apply(Block block) {
                List<Block> successors = block.getSuccessors();
                printBlock(block, successors, null, printHIR, printLIR);
            }
        });
        end("cfg");
    }

    public void printIntervals(LinearScan allocator, Interval[] intervals, String name) {
        begin("intervals");
        out.println(String.format("name \"%s\"", name));

        for (Interval interval : intervals) {
            if (interval != null) {
                printInterval(allocator, interval);
            }
        }

        end("intervals");
    }

    private void printInterval(LinearScan allocator, Interval interval) {
        out.printf("%d %s ", interval.operandNumber, (interval.operand.isRegister() ? "fixed" : interval.kind().name()));
        if (interval.operand.isRegister()) {
            out.printf("\"[%s|%c]\"", interval.operand.name(), interval.operand.kind.typeChar);
        } else {
            if (interval.location() != null) {
                out.printf("\"[%s|%c]\"", interval.location().name(), interval.location().kind.typeChar);
            }
        }

        Interval hint = interval.locationHint(false, allocator);
        out.printf("%d %d ", interval.splitParent().operandNumber, hint != null ? hint.operandNumber : -1);

        // print ranges
        Range cur = interval.first();
        while (cur != Range.EndMarker) {
            out.printf("[%d, %d[", cur.from, cur.to);
            cur = cur.next;
            assert cur != null : "range list not closed with range sentinel";
        }

        // print use positions
        int prev = 0;
        UsePosList usePosList = interval.usePosList();
        for (int i = usePosList.size() - 1; i >= 0; --i) {
            assert prev < usePosList.usePos(i) : "use positions not sorted";
            out.printf("%d %s ", usePosList.usePos(i), usePosList.registerPriority(i));
            prev = usePosList.usePos(i);
        }

        out.printf(" \"%s\"", interval.spillState());
        out.println();
    }
}
