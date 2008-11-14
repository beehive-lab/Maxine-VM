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
package com.sun.max.vm.compiler.cir.gui;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.util.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.gui.CirAnnotatedTrace.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.compiler.cir.variable.*;

/**
 * Builds an {@linkplain CirAnnotatedTrace annotated CIR trace} of a CIR graph.
 *
 * @author Doug Simon
 */
final class CirAnnotatedTraceBuilder extends CirVisitor {

    private final StringBuilder _buffer;
    private final AppendableSequence<Element> _elements = new ArrayListSequence<Element>();

    private CirMethod _method;
    private boolean _atStartOfLine = true;
    private int _indentation;

    public CirAnnotatedTraceBuilder(CirNode node) {
        if (node instanceof CirMethod) {
            _method = (CirMethod) node;
        } else {
            _method = null;
        }
        _buffer = new StringBuilder();
        node.acceptVisitor(this);
    }

    private void append(final Element element) {
        _elements.append(element);
    }

    private void append(String text) {
        _buffer.append(text);
    }

    private Range print(String string) {
        if (_atStartOfLine) {
            if (_indentation <= MAX_PRINTABLE_INDENTATION) {
                for (int i = 0; i < _indentation; i++) {
                    append(INDENTATION);
                }
            } else {
                final int actualIndentation = _indentation % MAX_PRINTABLE_INDENTATION;
                final int elided = _indentation - actualIndentation;
                append("[" + elided + " more +] ");
                for (int i = 0; i < actualIndentation; i++) {
                    append(INDENTATION);
                }
            }
            _atStartOfLine = false;
        }
        final int start = _buffer.length();
        append(string);
        return new Range(start, start + string.length());
    }

    private static final String INDENTATION = "  ";
    private static final int MAX_PRINTABLE_INDENTATION = 80;

    private void println() {
        append("\n");
        _atStartOfLine = true;
    }

    private void println(String string) {
        print(string);
        println();
    }

    private void indent() {
        _indentation++;
        println();
    }

    private void outdent() {
        _indentation--;
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
        final int bodyStart = _buffer.length();
        closure.body().acceptVisitor(this);
        final int bodyEnd = _buffer.length();
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
    public void visitMethod(CirMethod method) {
        if (method == _method && method.isGenerated()) {
            _method = null;
            printClosure(method.name(), method, method.closure());
        } else {
            append(new SimpleElement(method, print(method.name())));
        }
    }

    private final Set<CirBlock> _blockIds = new HashSet<CirBlock>();

    /**
     * A solid raised dot.
     */
    private static final String CALL_OPERATOR = " \u2022 ";

    @Override
    public void visitBlock(CirBlock block) {
        final CirClosure closure = block.closure();
        if (_blockIds.contains(block)) {
            final String label = block + "#" + block.id();
            final Range open = print("{");
            final Range blockRange = print(label);
            final Range parametersRange = printParameters(closure);
            final int bodyStart = parametersRange.end();
            append(new SimpleElement(closure.body(), print(CALL_OPERATOR)));
            print(" ...");
            final Range bodyRange = new Range(bodyStart, _buffer.length());
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
            _blockIds.add(block);
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

    private String name(CirSlotVariable variable, BytecodeLocation location) {
//        if (location != null) {
//            final LocalVariableInfo localVariableInfo = LocalVariableTableAttribute.findLocalVariable(location.classMethodActor().codeAttribute().localVariableTableAttributes(), variable.slotIndex(), location.programCounter());
//            if (localVariableInfo != null) {
//                return '"' + localVariableInfo.name() + "\":" + variable.kind().character();
//            }
//        }
        return variable.toString();
    }

    @Override
    public void visitLocalVariable(CirLocalVariable variable) {
        append(new SimpleElement(variable, print(name(variable, variable.definedAt()))));
    }

    @Override
    public void visitMethodParameter(CirMethodParameter parameter) {
        append(new SimpleElement(parameter, print(name(parameter, parameter.definedAt()))));
    }

    public String trace() {
        return _buffer.toString();
    }

    public Sequence<Element> elements() {
        return _elements;
    }
}
