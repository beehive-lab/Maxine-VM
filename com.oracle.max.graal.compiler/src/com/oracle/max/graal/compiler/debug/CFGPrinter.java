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
import com.oracle.max.graal.compiler.graphbuilder.*;
import com.oracle.max.graal.compiler.lir.*;
import com.oracle.max.graal.compiler.schedule.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.graph.Node.*;
import com.oracle.max.graal.graph.NodeClass.*;
import com.oracle.max.graal.nodes.*;
import com.oracle.max.graal.nodes.calc.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * Utility for printing Graal IR at various compilation phases.
 */
public class CFGPrinter extends CompilationPrinter {

    public final ByteArrayOutputStream buffer;
    public final GraalCompilation compilation;
    public final CiTarget target;
    public final RiRuntime runtime;

    /**
     * Creates a control flow graph printer.
     *
     * @param buffer where the output generated via this printer shown be written
     * @param target the target architecture description
     */
    public CFGPrinter(ByteArrayOutputStream buffer, GraalCompilation compilation) {
        super(buffer);
        this.buffer = buffer;
        this.compilation = compilation;
        this.target = compilation.compiler.target;
        this.runtime = compilation.compiler.runtime;
    }


    /**
     * Prints the control flow graph denoted by a given block map.
     *
     * @param label A label describing the compilation phase that produced the control flow graph.
     * @param blockMap A data structure describing the blocks in a method and how they are connected.
     */
    public void printCFG(String label, BlockMap blockMap) {
        begin("cfg");
        out.print("name \"").print(label).println('"');
        for (BlockMap.Block block : blockMap.blocks) {
            begin("block");
            printBlock(block);
            end("block");
        }
        end("cfg");
    }


    private void printBlock(BlockMap.Block block) {
        out.print("name \"B").print(block.startBci).println('"');
        out.print("from_bci ").println(block.startBci);
        out.print("to_bci ").println(block.endBci);

        out.println("predecessors ");

        out.print("successors ");
        for (BlockMap.Block succ : block.successors) {
            if (!succ.isExceptionEntry) {
                out.print("\"B").print(succ.startBci).print("\" ");
            }
        }
        out.println();

        out.print("xhandlers");
        for (BlockMap.Block succ : block.successors) {
            if (succ.isExceptionEntry) {
                out.print("\"B").print(succ.startBci).print("\" ");
            }
        }
        out.println();

        out.print("flags ");
        if (block.isExceptionEntry) {
            out.print("\"ex\" ");
        }
        if (block.isLoopHeader) {
            out.print("\"plh\" ");
        }
        out.println();

        out.print("loop_depth ").println(Long.bitCount(block.loops));
    }

    private void printBlock(Block block, boolean printHIR) {
        begin("block");

        out.print("name \"B").print(block.blockID()).println('"');
        out.println("from_bci -1");
        out.println("to_bci -1");

        out.print("predecessors ");
        for (Block pred : block.getPredecessors()) {
            out.print("\"B").print(pred.blockID()).print("\" ");
        }
        out.println();

        out.print("successors ");
        for (Block succ : block.getSuccessors()) {
            if (!succ.isExceptionBlock()) {
                out.print("\"B").print(succ.blockID()).print("\" ");
            }
        }
        out.println();

        out.print("xhandlers");
        for (Block succ : block.getSuccessors()) {
            if (succ.isExceptionBlock()) {
                out.print("\"B").print(succ.blockID()).print("\" ");
            }
        }
        out.println();

        out.print("flags ");
        if (block.isLoopHeader()) {
            out.print("\"llh\" ");
        }
        if (block.isLoopEnd()) {
            out.print("\"llh\" ");
        }
        if (block.isExceptionBlock()) {
            out.print("\"ex\" ");
        }
        out.println();

        out.print("loop_index ").println(block.loopIndex());
        out.print("loop_depth ").println(block.loopDepth());

        if (printHIR) {
            printHIR(block);
        }

        if (block instanceof LIRBlock) {
            printLIR((LIRBlock) block);
        }

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
     * @param method
     */
    private String stateToString(FrameState state, OperandFormatter operandFmt, RiResolvedMethod method) {
        if (state == null) {
            return null;
        }

        StringBuilder buf = new StringBuilder();
        buf.append(CiUtil.toLocation(method, state.bci));
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
     * @param method
     */
    private void printHIR(Block block) {
        begin("IR");
        out.println("HIR");
        out.disableIndentation();
        for (Node node : block.getInstructions()) {
            printInstructionHIR((ValueNode) node);
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
        List<LIRInstruction> lir = block.lir();
        if (lir != null) {
            begin("IR");
            out.println("LIR");
            for (int i = 0; i < lir.size(); i++) {
                LIRInstruction inst = lir.get(i);
                out.printf("nr %4d ", inst.id()).print(COLUMN_END);

                if (inst.info != null) {
                    int level = out.indentationLevel();
                    out.adjustIndentation(-level);
                    String state;
                    if (inst.info.debugInfo != null) {
                        // Use register-allocator output if available
                        state = debugInfoToString(inst.info.debugInfo, new OperandFormatter(false), target.arch);
                    } else {
                        state = stateToString(inst.info.state, new OperandFormatter(false), compilation.method);
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
     * @param method
     */
    private void printInstructionHIR(ValueNode i) {
        if (i instanceof FixedWithNextNode) {
            out.print("f ").print(HOVER_START).print("#").print(HOVER_SEP).print("fixed with next").print(HOVER_END).println(COLUMN_END);
        } else if (i instanceof FixedNode) {
            out.print("f ").print(HOVER_START).print("*").print(HOVER_SEP).print("fixed").print(HOVER_END).println(COLUMN_END);
        } else if (i instanceof FloatingNode) {
            out.print("f ").print(HOVER_START).print("~").print(HOVER_SEP).print("floating").print(HOVER_END).println(COLUMN_END);
        }
        if (i.operand().isLegal()) {
            out.print("result ").print(new OperandFormatter(false).format(i.operand())).println(COLUMN_END);
        }
        out.print("tid ").print(node(i)).println(COLUMN_END);

        if (i instanceof StateSplit) {
            StateSplit stateSplit = (StateSplit) i;
            String state = stateToString(stateSplit.stateAfter(), null, compilation.method);
            if (state != null) {
                out.print("st ").print(HOVER_START).print("st").print(HOVER_SEP).print(state).print(HOVER_END).println(COLUMN_END);
            }
        }

        Map<Object, Object> props = i.getDebugProperties();
        out.print("d ").print(HOVER_START).print("d").print(HOVER_SEP);
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            out.print(entry.getKey().toString()).print(": ").print(entry.getValue().toString()).println();
        }
        out.print(HOVER_END).println(COLUMN_END);

        out.print("instruction ");
        out.print(HOVER_START).print(i.getNodeClass().shortName()).print(HOVER_SEP).print(i.getClass().getName()).print(HOVER_END).print(" ");
        int lastIndex = -1;
        for (NodeClassIterator iter = i.inputs().iterator(); iter.hasNext();) {
            Position pos = iter.nextPosition();
            if (pos.index != lastIndex) {
                if (pos.input) {
                    out.print(i.getNodeClass().getName(pos)).print(": ");
                } else {
                    out.print("#").print(i.getNodeClass().getName(pos)).print(": ");
                }
            }
            lastIndex = pos.index;
            out.print(node(i.getNodeClass().get(i, pos))).print(" ");
        }

        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = entry.getKey().toString();
            if (key.startsWith("data.") && !key.equals("data.kind")) {
                out.print(key.substring("data.".length())).print(": ").print(entry.getValue().toString()).print(" ");
            }
        }

        out.print(COLUMN_END).print(' ').println(COLUMN_END);
    }

    public static String node(Node node) {
        if (node == null) {
            return "-";
        } else if (!(node instanceof ValueNode)) {
            return "?";
        }
        ValueNode value = (ValueNode) node;
        if (value.kind() == CiKind.Illegal) {
            return "v" + value.toString(Verbosity.Id);
        } else {
            return String.valueOf(value.kind().typeChar) + value.toString(Verbosity.Id);
        }
    }

    /**
     * Prints the control flow graph rooted at a given block.
     *
     * @param startBlock the entry block of the control flow graph to be printed
     * @param label a label describing the compilation phase that produced the control flow graph
     * @param printHIR if {@code true} the HIR for each instruction in the block will be printed
     * @param printLIR if {@code true} the LIR for each instruction in the block will be printed
     */
    public void printCFG(String label, List<? extends Block> blocks, boolean printHIR) {
        begin("cfg");
        out.print("name \"").print(label).println('"');
        for (Block block : blocks) {
            printBlock(block, printHIR);
        }
        end("cfg");
    }


    public void printIntervals(String label, LinearScan allocator, Interval[] intervals) {
        begin("intervals");
        out.println(String.format("name \"%s\"", label));

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
