/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.tir.pipeline;

import com.sun.max.collect.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.LocalVariableTable.*;
import com.sun.max.vm.cps.hotpath.*;
import com.sun.max.vm.cps.hotpath.compiler.*;
import com.sun.max.vm.cps.hotpath.compiler.Console.*;
import com.sun.max.vm.cps.tir.*;
import com.sun.max.vm.cps.tir.TirInstruction.*;
import com.sun.max.vm.cps.tir.TirMessage.*;

public class TirPrintSink extends TirBufferedSink {

    public static void print(TirTree tree) {
        tree.send(new TirPrintSink());
    }

    @Override
    public void visit(TirTreeEnd message) {
        super.visit(message);
        replay(new TirReverse(new PrintSink()));
    }

    private class PrintSink extends TirInstructionFilter {
        private Mapping<TirMessage, String> labelMap = new IdentityHashMapping<TirMessage, String>();
        private OperandVisitor operandVisitor = new OperandVisitor();
        private TirTreeBegin treeBegin;
        private TirTraceBegin traceBegin;

        private MapFunction<TirInstruction, String> defLabelMap = new MapFunction<TirInstruction, String>() {
            public String map(TirInstruction instruction) {
                assert labelMap.containsKey(instruction) == false;
                final String name = "(" + labelMap.length() + ")";
                labelMap.put(instruction, name);
                return name;
            }
        };

        private MapFunction<TirInstruction, String> useLabelMap = new MapFunction<TirInstruction, String>() {
            public String map(TirInstruction instruction) {
                if (instruction == Placeholder.FILLER) {
                    return "%";
                } else if (instruction == Placeholder.UNDEFINED) {
                    return "_";
                } else if (instruction instanceof TirConstant) {
                    return Color.color(NameMap.CONSTANT_COLOR, instruction.toString());
                } else if (instruction instanceof TirLocal) {
                    final String variableName = localVar((TirLocal) instruction, treeBegin.tree());
                    return variableName;
                }
                String name = labelMap.get(instruction);
                if (name == null) {
                    name = "(" + labelMap.length() + ")";
                    labelMap.put(instruction, name);
                }
                return name;
            }
        };

        public PrintSink() {
            super(TirVoidSink.SINK);
        }

        @Override
        public void visit(TirTreeBegin treeBegin) {
            NameMap.updateLabelMap(treeBegin.tree(), useLabelMap);
            assert treeBegin.order() == TirPipelineOrder.FORWARD;
            this.treeBegin = treeBegin;
            Console.printDivider("TREE " + NameMap.nameOf(treeBegin.tree()), '=', 6);
            Console.print("entry state: ");
            treeBegin.tree().entryState().println(useLabelMap);
            Console.println("anchor: " + treeBegin.tree().anchor().toString());
            Console.printDivider('-');
        }

        @Override
        public void visit(TirTraceBegin traceBegin) {
            this.traceBegin = traceBegin;
            final String title;
            if (this.traceBegin.trace().anchor() == null) {
                title = "TRUNK";
            } else {
                title = "BRANCH AT " + useLabelMap.map(this.traceBegin.trace().anchor().guard());
            }
            Console.printDivider(title, '-', 6);
        }

        @Override
        public void visit(TirTraceEnd traceEnd) {
            Console.printDivider("TRACE END", '-', 6);
            Console.print("tail state: ");
            traceBegin.trace().tailState().println(useLabelMap);
        }

        @Override
        public void visit(TirTreeEnd message) {
            Console.printDivider("TREE END", '=', 6);
        }

        @Override
        public void visit(TirLocal instruction) {
            printPrefix(instruction);
            final TirTree tree = treeBegin.tree();
            final String variableName = localVar(instruction, tree);
            Console.print(", var: " + variableName);
            Console.print(", " + instruction.flags());
            Console.println();
        }

        private String localVar(TirLocal instruction, final TirTree tree) {
            final ClassMethodActor classMethodActor = tree.classMethodActor();
            final LocalVariableTable localVariableTable = classMethodActor.codeAttribute().localVariableTable();
            final Entry entry = localVariableTable.findLocalVariable(instruction.slot(), tree.anchor().position());
            final String variableName = entry == null ? "?" : entry.name(classMethodActor.codeAttribute().cp).toString();
            return Color.color(NameMap.LOCAL_COLOR, "(" + variableName + ")");
        }

        @Override
        public void visit(TirTreeCall call) {
            printPrefix(call);
            Console.print(" " + NameMap.nameOf(call.tree()) + " state: ");
            call.state().println(useLabelMap);
        }

        @Override
        public void visit(TirMethodCall call) {
            printPrefix(call);
            printOperands(call);
            Console.print(" state: ");
            call.state().println(useLabelMap);
        }

        @Override
        public void visit(TirGuard guard) {
            printPrefix(guard);
            Console.print(" #" + treeBegin.tree().getNumber(guard));
            Console.print(", " + useLabelMap.map(guard.operand0()) + " " + guard.valueComparator().symbol() + " " + useLabelMap.map(guard.operand1()));
            if (guard.throwable() != null) {
                Console.print(", " + guard.throwable().getSimpleName());
            }
            Console.print(", state: ");
            guard.state().println(useLabelMap);
        }

        @Override
        public void visit(TirInstruction instruction) {
            if (instruction instanceof Placeholder) {
                Console.printf("     : " + Color.color(Color.LIGHTYELLOW, instruction.toString()));
            } else {
                printPrefix(instruction);
                printOperands(instruction);
            }
            Console.println();
        }

        private void printPrefix(TirInstruction instruction) {
            Console.printf("%5s: ", defLabelMap.map(instruction));
            Console.print(instruction.toString());
        }

        private void printOperands(TirInstruction instruction) {
            instruction.visitOperands(operandVisitor);
        }

        @Override
        public void visit(TirMessage message) {
            Console.err().println(Color.LIGHTRED, message.toString());
        }

        public class OperandVisitor extends TirInstructionAdapter {
            @Override
            public void visit(TirInstruction instruction) {
                Console.print(" " + useLabelMap.map(instruction));
            }
        }
    }
}
