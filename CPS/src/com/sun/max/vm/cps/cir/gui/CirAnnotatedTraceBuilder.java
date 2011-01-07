/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.cir.gui;

import java.util.*;

import com.sun.max.util.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.gui.CirAnnotatedTrace.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.cps.cir.variable.*;

/**
 * Builds an {@linkplain CirAnnotatedTrace annotated CIR trace} of a CIR graph.
 *
 * @author Doug Simon
 */
final class CirAnnotatedTraceBuilder extends CirVisitor {

    private final StringBuilder buffer;
    private final List<Element> elements = new ArrayList<Element>();

    private CirMethod method;
    private boolean atStartOfLine = true;
    private int indentation;

    public CirAnnotatedTraceBuilder(CirNode node) {
        if (node instanceof CirMethod) {
            method = (CirMethod) node;
        } else {
            method = null;
        }
        buffer = new StringBuilder();
        node.acceptVisitor(this);
    }

    private void append(final Element element) {
        elements.add(element);
    }

    private void append(String text) {
        buffer.append(text);
    }

    private Range print(String string) {
        if (atStartOfLine) {
            if (indentation <= MAX_PRINTABLE_INDENTATION) {
                for (int i = 0; i < indentation; i++) {
                    append(INDENTATION);
                }
            } else {
                final int actualIndentation = indentation % MAX_PRINTABLE_INDENTATION;
                final int elided = indentation - actualIndentation;
                append("[" + elided + " more +] ");
                for (int i = 0; i < actualIndentation; i++) {
                    append(INDENTATION);
                }
            }
            atStartOfLine = false;
        }
        final int start = buffer.length();
        append(string);
        return new Range(start, start + string.length());
    }

    private static final String INDENTATION = "  ";
    private static final int MAX_PRINTABLE_INDENTATION = 80;

    private void println() {
        append("\n");
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
        final Range range = print(node.toString());
        append(new SimpleElement(node, range));
    }

    private void print(CirValue[] values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                print(" ");
            }
            values[i].acceptVisitor(this);
        }
    }

    @Override
    public void visitCall(CirCall call) {
        call.procedure().acceptVisitor(this);
        final Range open = print("(");
        print(call.arguments());
        final Range close = print(")");
        append(new ParenthesisElement(open, close));
    }

    private Range printParameters(CirClosure closure) {
        final Range open = print("[");
        print(closure.parameters());
        final Range close = print("]");
        append(new ParenthesisElement(open, close));
        return new Range(open.end(), close.start());
    }

    private void printClosure(String label, CirNode parent, CirClosure closure) {
        final Range open = print("{");
        final Range closureRange = print(label);
        final Range parametersRange = printParameters(closure);
        append(new SimpleElement(closure.body(), print(CALL_OPERATOR)));
        indent();
        print(""); // Prints the indentation
        final int bodyStart = buffer.length();
        closure.body().acceptVisitor(this);
        final int bodyEnd = buffer.length();
        outdent();
        final Range bodyRange = new Range(bodyStart, bodyEnd);
        final Range close = print("}");

        append(new SimpleElement(parent, closureRange) {
            @Override
            public void visitAssociatedRanges(RangeVisitor visitor) {
                visitor.visitRange(parametersRange);
                visitor.visitRange(bodyRange);
            }
        });
        append(new ParenthesisElement(open, close));
    }

    @Override
    public void visitMethod(CirMethod m) {
        if (method == m && method.isGenerated()) {
            method = null;
            printClosure(m.name(), m, m.closure());
        } else {
            append(new SimpleElement(method, print(method.name())));
        }
    }

    private final Set<CirBlock> blockIds = new HashSet<CirBlock>();

    /**
     * A solid raised dot.
     */
    private static final String CALL_OPERATOR = " \u2022 ";

    @Override
    public void visitBlock(CirBlock block) {
        final CirClosure closure = block.closure();
        if (blockIds.contains(block)) {
            final String label = block + "#" + block.id();
            final Range open = print("{");
            final Range blockRange = print(label);
            final Range parametersRange = printParameters(closure);
            final int bodyStart = parametersRange.end();
            append(new SimpleElement(closure.body(), print(CALL_OPERATOR)));
            print(" ...");
            final Range bodyRange = new Range(bodyStart, buffer.length());
            final Range close = print("}");
            append(new SimpleElement(block, blockRange) {
                @Override
                public void visitAssociatedRanges(RangeVisitor visitor) {
                    visitor.visitRange(parametersRange);
                    visitor.visitRange(bodyRange);
                }
            });
            append(new ParenthesisElement(open, close));
        } else {
            blockIds.add(block);
            final String label = block.role() + "#" + block.id();
            printClosure(label, block, closure);
        }
    }

    @Override
    public void visitClosure(CirClosure closure) {
        printClosure("proc", closure, closure);
    }

    @Override
    public void visitContinuation(CirContinuation continuation) {
        printClosure("cont", continuation, continuation);
    }

    @Override
    public void visitLocalVariable(CirLocalVariable variable) {
        append(new SimpleElement(variable, print(variable.toString())));
    }

    @Override
    public void visitMethodParameter(CirMethodParameter parameter) {
        append(new SimpleElement(parameter, print(parameter.toString())));
    }

    public String trace() {
        return buffer.toString();
    }

    public List<Element> elements() {
        return elements;
    }
}
