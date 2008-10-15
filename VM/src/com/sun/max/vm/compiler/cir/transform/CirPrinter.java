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
package com.sun.max.vm.compiler.cir.transform;

import java.io.*;
import java.util.*;

import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.variable.*;

/**
 * "Pretty" printing of CIR constructs to a print stream.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class CirPrinter extends CirVisitor {

    private final PrintStream _stream;
    private CirMethod _method;
    private boolean _atStartOfLine = true;
    private BytecodeLocation _location;
    private final boolean _showBir;
    private final boolean _printingIds;
    private final int _nesting;
    private int _indentation;

    /**
     * Prints a nested trace of a CIR graph.
     *
     * @param stream where to print
     * @param node the root of the CIR graph to print
     * @param showBir shows bytecode level mapping (if any) for each node in the graph
     * @param printIds include the {@linkplain CirNode#id() globally unique ID} associated with each node in the print
     *            out
     * @param nesting maximum depth of node in the graph to be traced. Deeper nodes are indicated with "...". Set this
     *            value to {@link Integer#MAX_VALUE} to ensures the complete graph is printed
     */
    public CirPrinter(PrintStream stream, CirNode node, boolean showBir, boolean printIds, int nesting) {
        _stream = stream;
        _showBir = showBir;
        _printingIds = printIds;
        _nesting = nesting;
        if (node instanceof CirMethod) {
            _method = (CirMethod) node;
        } else {
            _method = null;
        }
    }

    private static final String MAX_BIR_PREFIX_SPACES = "                        ";

    private void print(String string) {
        if (_atStartOfLine) {
            if (_showBir) {
                if (_location != null) {
                    final String disassembly = getBytecodeDisassembly(_location);
                    _stream.print(disassembly);
                    int padding = MAX_BIR_PREFIX_SPACES.length() - disassembly.length();
                    if (padding >= 0) {
                        while (padding > 0) {
                            _stream.print(' ');
                            --padding;
                        }
                    } else {
                        _stream.println();
                        _stream.print(MAX_BIR_PREFIX_SPACES);
                    }
                } else {
                    _stream.print(MAX_BIR_PREFIX_SPACES);
                }
            }
            if (_indentation <= MAX_PRINTABLE_INDENTATION) {
                for (int i = 0; i < _indentation; i++) {
                    _stream.print(INDENTATION);
                }
            } else {
                final int actualIndentation = _indentation % MAX_PRINTABLE_INDENTATION;
                final int elided = _indentation - actualIndentation;
                _stream.print("[" + elided + " more +] ");
                for (int i = 0; i < actualIndentation; i++) {
                    _stream.print(INDENTATION);
                }
            }
            _atStartOfLine = false;
            _location = null;
        }
        _stream.print(string);
        _stream.flush();
    }

    private static final String INDENTATION = "| ";
    private static final int MAX_PRINTABLE_INDENTATION = 80;

    private void println() {
        _stream.println();
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
        if (_printingIds) {
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

    private String getBytecodeDisassembly(BytecodeLocation location) {
        final BytecodeBlock bytecodeBlock = location.getBytecodeBlock();
        final ConstantPool constantPool = location.classMethodActor().codeAttribute().constantPool();
        return BytecodePrinter.toString(constantPool, bytecodeBlock);
    }

    @Override
    public void visitCall(CirCall call) {
        _location = call.bytecodeLocation();
        call.procedure().acceptVisitor(this);
        print("(");
        print(call.arguments());
        print(")");
        if (call.javaFrameDescriptor() != null) {
            print(call.javaFrameDescriptor().toString());
        }
    }

    private void printParameters(CirClosure closure) {
        print("[");
        print(closure.parameters());
        print("] . ");
    }

    private void printClosure(String label, CirClosure closure) {
        _location = closure.location();
        print(label);
        printParameters(closure);
        indent();
        if (_indentation >= _nesting) {
            print("...");
        } else {
            closure.body().acceptVisitor(this);
        }
        outdent();
        print("}");
    }

    @Override
    public void visitMethod(CirMethod method) {
        if (method == _method && method.isGenerated()) {
            _method = null;
            printClosure("{" + method.getQualifiedName(), method.closure());
        } else {
            print(method.getQualifiedName());
        }
        println();
    }

    private final Map<CirBlock, Integer> _localBlockIds = new HashMap<CirBlock, Integer>();

    @Override
    public void visitBlock(CirBlock block) {
        Integer blockId = _localBlockIds.get(block);
        final String blockLabel = _printingIds ? block + "-" + block.id() : block.toString();
        if (blockId != null) {
            print("{" + blockLabel + "#" + blockId);
            printParameters(block.closure());
            print(" ...}");
        } else {
            blockId = _localBlockIds.size();
            _localBlockIds.put(block, blockId);
            printClosure("{" + blockLabel + "#" + blockId, block.closure());
        }
    }

    @Override
    public void visitClosure(CirClosure closure) {
        if (_printingIds) {
            printClosure("{proc_" + closure.id(), closure);
        } else {
            printClosure("{proc", closure);
        }
    }

    @Override
    public void visitContinuation(CirContinuation continuation) {
        if (_printingIds) {
            printClosure("{cont_" + continuation.id(), continuation);
        } else {
            printClosure("{cont", continuation);
        }
    }

    private String name(CirSlotVariable variable, BytecodeLocation location) {
        if (_printingIds) {
            return variable.toString() + "_" + variable.id();
        }
        return variable.toString();
    }

    @Override
    public void visitLocalVariable(CirLocalVariable variable) {
        print(name(variable, variable.definedAt()));
    }

    @Override
    public void visitMethodParameter(CirMethodParameter parameter) {
        print(name(parameter, parameter.definedAt()));
    }
}
