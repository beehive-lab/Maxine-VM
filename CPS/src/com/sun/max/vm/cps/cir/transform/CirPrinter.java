/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.cir.transform;

import java.io.*;
import java.util.*;

import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.snippet.*;
import com.sun.max.vm.cps.cir.variable.*;

/**
 * "Pretty" printing of CIR constructs to a print stream.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class CirPrinter extends CirVisitor {

    private final PrintStream stream;
    private CirMethod method;
    private boolean atStartOfLine = true;
    private final boolean printingIds;
    private final int nesting;
    private int indentation;

    /**
     * Prints a nested trace of a CIR graph.
     *
     * @param stream where to print
     * @param node the root of the CIR graph to print
     * @param printIds include the {@linkplain CirNode#id() globally unique ID} associated with each node in the print
     *            out
     * @param nesting maximum depth of node in the graph to be traced. Deeper nodes are indicated with "...". Set this
     *            value to {@link Integer#MAX_VALUE} to ensures the complete graph is printed
     */
    public CirPrinter(PrintStream stream, CirNode node, boolean printIds, int nesting) {
        this.stream = stream;
        this.printingIds = printIds;
        this.nesting = nesting;
        if (node instanceof CirMethod) {
            this.method = (CirMethod) node;
        } else {
            this.method = null;
        }
    }

    private static final String MAX_BIR_PREFIX_SPACES = "                        ";

    private void print(String string) {
        if (atStartOfLine) {
            if (indentation <= MAX_PRINTABLE_INDENTATION) {
                for (int i = 0; i < indentation; i++) {
                    stream.print(INDENTATION);
                }
            } else {
                final int actualIndentation = indentation % MAX_PRINTABLE_INDENTATION;
                final int elided = indentation - actualIndentation;
                stream.print("[" + elided + " more +] ");
                for (int i = 0; i < actualIndentation; i++) {
                    stream.print(INDENTATION);
                }
            }
            atStartOfLine = false;
        }
        stream.print(string);
        stream.flush();
    }

    private static final String INDENTATION = "| ";
    private static final int MAX_PRINTABLE_INDENTATION = 80;

    private void println() {
        stream.println();
        atStartOfLine = true;
    }

    private void println(String string) {
        print(string);
        println();
    }

    private void indent() {
        indentation++;
        println();
    }

    private void outdent() {
        indentation--;
        println();
    }

    @Override
    public void visitNode(CirNode node) {
        if (printingIds) {
            print(node.toString() + "_" + node.id());
        } else {
            print(node.toString());
        }
    }

    private void print(CirValue[] values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                print(" ");
            }
            if (values[i] == null) {
                print("null");
            } else {
                values[i].acceptVisitor(this);
            }
        }
    }

    @Override
    public void visitCall(CirCall call) {
        call.procedure().acceptVisitor(this);
        print("(");
        print(call.arguments());
        print(")");
        if (call.javaFrameDescriptor() != null) {
            print(call.javaFrameDescriptor().toString());
        }
        if (call.isNative()) {
            print(" <native function call>");
        }
    }

    private void printParameters(CirClosure closure) {
        print("[");
        print(closure.parameters());
        print("] . ");
    }

    private void printClosure(String label, CirClosure closure) {
        print(label);
        printParameters(closure);
        if (indentation >= nesting) {
            indent();
            print("...");
        } else {
            if (printingIds) {
                print("@" + closure.body().id() + "@");
            }
            indent();
            closure.body().acceptVisitor(this);
        }
        outdent();
        print("}");
    }

    @Override
    public void visitMethod(CirMethod method) {
        if (method == this.method && method.isGenerated()) {
            this.method = null;
            printClosure("{" + method.getQualifiedName(), method.closure());
            println();
        } else {
            if (method instanceof CirSnippet) {
                print(method.name());
            } else {
                print(method.getQualifiedName());
            }
        }
    }

    private final Map<CirBlock, Integer> localBlockIds = new HashMap<CirBlock, Integer>();

    @Override
    public void visitBlock(CirBlock block) {
        Integer blockId = localBlockIds.get(block);
        final String blockLabel = printingIds ? block + "-" + block.id() : block.toString();
        if (blockId != null) {
            print("{" + blockLabel + "#" + blockId);
            printParameters(block.closure());
            print(" ...}");
        } else {
            blockId = localBlockIds.size();
            localBlockIds.put(block, blockId);
            printClosure("{" + blockLabel + "#" + blockId, block.closure());
        }
    }

    @Override
    public void visitClosure(CirClosure closure) {
        if (printingIds) {
            printClosure("{proc_" + closure.id(), closure);
        } else {
            printClosure("{proc", closure);
        }
    }

    @Override
    public void visitContinuation(CirContinuation continuation) {
        if (printingIds) {
            printClosure("{cont_" + continuation.id(), continuation);
        } else {
            printClosure("{cont", continuation);
        }
    }

    @Override
    public void visitLocalVariable(CirLocalVariable variable) {
        print(variable.toString());
    }

    @Override
    public void visitMethodParameter(CirMethodParameter parameter) {
        print(parameter.toString());
    }
}
