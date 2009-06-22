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
package com.sun.max.vm.compiler.tir.pipeline;

import com.sun.max.collect.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.LocalVariableTable.*;
import com.sun.max.vm.compiler.tir.*;
import com.sun.max.vm.compiler.tir.TirInstruction.*;
import com.sun.max.vm.compiler.tir.TirMessage.*;
import com.sun.max.vm.hotpath.*;
import com.sun.max.vm.hotpath.compiler.*;
import com.sun.max.vm.hotpath.compiler.Console.*;


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
        private GrowableMapping<TirMessage, String> labelMap = new IdentityHashMapping<TirMessage, String>();
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
            final String variableName = entry == null ? "?" : entry.name(classMethodActor.codeAttribute().constantPool()).toString();
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
            if (guard.thorwable() != null) {
                Console.print(", " + guard.thorwable().getSimpleName());
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
