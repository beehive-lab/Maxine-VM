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
package com.sun.max.vm.cps.cir.bytecode;

import static com.sun.max.vm.cps.cir.bytecode.CirBytecode.Opcode.*;

import java.io.*;
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.builtin.*;
import com.sun.max.vm.cps.cir.bytecode.CirBytecode.*;
import com.sun.max.vm.cps.cir.snippet.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.cps.cir.transform.CirDepthFirstTraversal.*;
import com.sun.max.vm.cps.cir.variable.*;
import com.sun.max.vm.type.*;

/**
 * Converts a CIR graph to a {@linkplain CirBytecode compact bytecode form}. A CIR graph can only be converted if all
 * of it's unique variables have unique {@linkplain CirVariable#serial() serial numbers}. That is, it has been
 * {@linkplain CirAlphaConversion alpha converted}.
 *
 * @author Doug Simon
 */
public class CirBytecodeWriter extends CirVisitor {

    /**
     * This class serves a dual purpose:
     * 1. Acts as the "visited block set" in a {@linkplain CirDepthFirstTraversal#run(CirNode, CirVisitor) CIR depth first traversal}.
     * 2. Allocates unique identifiers for blocks.
     */
    static class BlockIdMap extends AbstractBlockSetMap<Integer> {
        public void recordBlock(CirBlock block) {
            final Integer blockId = new Integer(length());
            put(block, blockId);
        }
    }

    private static final int TRACE_LEVEL = 6;

    private final ByteArrayOutputStream stream;
    private final HashMap<Object, Integer> constantPoolMap;
    private final BlockIdMap blocks = new BlockIdMap();
    private final Map<CirVariable, Integer> variables = new HashMap<CirVariable, Integer>();
    private int maxReferencedVariableSerial;

    /**
     * Very slow operation to ensure that the translation from CIR to CIR bytecode was correct.
     * This should only be called as the condition of an assertion.
     */
    private boolean compareBeforeAndAfter(CirNode beforeNode, Object context) {
        // The string form of the before and after CIR graphs must not include the unique
        // CirNode ids otherwise the string comparison will never succeed.
        final String before = beforeNode.traceToString(true, false, 0);
        final String after = CirBytecodeReader.read(bytecode()).traceToString(true, false, 0);
        if (!before.equals(after)) {
            final String nl = System.getProperty("line.separator", "\n");
            ProgramError.unexpected(
                            "CIR to CirBytecode translation for " + context + " failed:" + nl +
                            "---- Before ----" + nl +
                            before + nl +
                            "---- After ----" + nl +
                            after);
        }
        return true;
    }

    private CirDepthFirstTraversal depthFirstTraversal;

    public CirBytecodeWriter(CirNode node, Object context) {
        stream = new ByteArrayOutputStream();
        constantPoolMap = new HashMap<Object, Integer>();

        depthFirstTraversal = new CirDepthFirstTraversal(blocks);
        depthFirstTraversal.run(node, this);
        depthFirstTraversal = null;

        // This is a somewhat slow assertion as it reconstructs the CIR from bytecode and then traces
        // the CIR twice. This was primarily used to debug the CirBytecodeReader and CirBytecodeWriter
        // and so is disabled until they appear to have a problem.
//      assert compareBeforeAndAfter(node, context);
    }

    public CirBytecode bytecode() {
        final byte[] code = stream.toByteArray();
        final Object[] constantPool = new Object[constantPoolMap.size() + 1];
        final Iterator<Object> keys = constantPoolMap.keySet().iterator();
        final Iterator<Integer> values = constantPoolMap.values().iterator();
        while (keys.hasNext()) {
            final int index = values.next();
            assert index != 0;
            constantPool[index] = keys.next();
        }
        return new CirBytecode(code, constantPool, blocks.length(), maxReferencedVariableSerial);
    }

    /**
     * A wrapper for handling variable aliasing within the CIR bytecode stream.
     */
    abstract class VariableWriter<Variable_Type extends CirVariable> {

        /**
         * Called when this is the first instance of the given variable to be written to
         * the stream.
         */
        abstract void write(Variable_Type variable);

        protected void writeSerial(CirVariable variable) {
            final int serial = variable.serial();
            assert !variables.containsKey(variable);
            assert !variables.containsValue(serial) : "2 or more variables have the same serial number";
            variables.put(variable, serial);
            writeUnsignedInt(serial);
        }

        /**
         * Writes a variable or variable alias to the stream depending on whether or not the variable has previously
         * been written to the stream.
         *
         * @param variable
         */
        VariableWriter(Variable_Type variable) {
            final int serial = variable.serial();
            if (variables.containsKey(variable)) {
                writeOpcode(VARIABLE_REFERENCE);
                writeUnsignedInt(serial);
                if (maxReferencedVariableSerial < serial) {
                    maxReferencedVariableSerial = serial;
                }
            } else {
                write(variable);
            }
            finish(variable);
        }
    }

    /**
     * Adds an unsigned integer whose value must be between 0 and 0x0FFFFFFF.
     * The number of bytes added to the buffer is given below:
     * <p><blockquote><pre>
     *     Value range               Bytes used for encoding
     *     0       .. 127                 1
     *     128     .. 16383               2
     *     16384   .. 2097151             3
     *     2097152 .. 268435455           4
     * </pre></blockquote></p>
     *
     * @param value  the unsigned integer value to add (must be between
     *               0 and 0x0FFFFFFF)
     */
    private void writeUnsignedInt(int value) {
        assert value >= 0 && value < 0x0FFFFFFF;
        if (value < 128) {
            /* 0xxxxxxx */
            stream.write(value);
        } else if (value < 16384) {
            /* 1xxxxxxx 0xxxxxxx */
            stream.write(((value >> 0) & 0x7F) | 0x80);
            stream.write(value >> 7);
        } else if (value < 2097152) {
            /* 1xxxxxxx 1xxxxxxx 0xxxxxxx */
            stream.write(((value >> 0) & 0x7F) | 0x80);
            stream.write(((value >> 7) & 0x7F) | 0x80);
            stream.write(value >> 14);
        } else {
            /* 1xxxxxxx 1xxxxxxx 1xxxxxxx 0xxxxxxx */
            stream.write(((value >> 0) & 0x7F) | 0x80);
            stream.write(((value >> 7) & 0x7F) | 0x80);
            stream.write(((value >> 14) & 0x7F) | 0x80);
            stream.write(value >> 21);
        }
        if (Trace.hasLevel(TRACE_LEVEL)) {
            traceLines.push(traceLines.pop() + " " + value);
        }
    }

    private void writeKind(Kind kind) {
        writeUnsignedInt(kind.character);
    }

    private void writeEnum(Enum value) {
        writeUnsignedInt(value.ordinal());
    }

    private void writeConstant(Object object) {
        if (object == null) {
            writeUnsignedInt(0);
        } else {
            Integer index = constantPoolMap.get(object);
            if (index == null) {
                index = constantPoolMap.size() + 1;
                constantPoolMap.put(object, index);
            }
            writeUnsignedInt(index);
        }
    }

    private void writeJavaFrameDescriptor(CirJavaFrameDescriptor javaFrameDescriptor) {
        CirJavaFrameDescriptor jfd = javaFrameDescriptor;
        if (jfd == null) {
            writeUnsignedInt(0);
        } else {
            writeUnsignedInt(jfd.depth());
            do {
                writeConstant(jfd.classMethodActor);
                writeUnsignedInt(jfd.bci);
                writeUnsignedInt(jfd.locals.length);
                writeUnsignedInt(jfd.stackSlots.length);
                jfd = jfd.parent();
            } while (jfd != null);
        }
    }

    private final Stack<String> traceLines = new Stack<String>();

    private void finish(CirNode node) {
        if (Trace.hasLevel(TRACE_LEVEL)) {
            Trace.line(0, traceLines.pop() + "   // " + node);
        }
    }

    private void writeOpcode(Opcode opcode) {
        if (Trace.hasLevel(TRACE_LEVEL)) {
            final int nodeLevel = depthFirstTraversal.currentNode().depth();
            traceLines.push(Strings.padLengthWithSpaces(stream.size() + "[" + nodeLevel + "] ", 8 + (2 * nodeLevel)) + opcode);
        }
        final int b = opcode.ordinal();
        assert (b & 0xff) == b;
        stream.write(b);
    }

    @Override
    public void visitNode(CirNode node) {
        ProgramError.unexpected("unknown concrete CIR node type: " + node.getClass().getName());
    }

    @Override
    public void visitCall(CirCall call) {
        final CirValue[] arguments = call.arguments();
        final Opcode implicitOperandOpcode = call.isNative() ? null : CALL.withImplicitOperand(arguments.length);
        if (implicitOperandOpcode != null) {
            writeOpcode(implicitOperandOpcode);
        } else {
            writeOpcode(call.isNative() ? NATIVE_CALL : CALL);
            writeUnsignedInt(arguments.length);
        }
        writeJavaFrameDescriptor(call.javaFrameDescriptor());
        finish(call);
    }

    @Override
    public void visitMethod(CirMethod method) {
        writeOpcode(METHOD);
        writeConstant(method);
        finish(method);
    }

    @Override
    public void visitSnippet(CirSnippet cirSnippet) {
        writeOpcode(SNIPPET);
        writeUnsignedInt(cirSnippet.snippet.serial());
        finish(cirSnippet);
    }

    @Override
    public void visitBlock(CirBlock block) {
        final int blockId = blocks.get(block);
        final boolean isReference = !depthFirstTraversal.currentNode().isFirstTraversal();
        switch (block.role()) {
            case NORMAL: {
                writeOpcode(isReference ? NORMAL_BLOCK_REFERENCE : NORMAL_BLOCK);
                break;
            }
            case EXCEPTION_DISPATCHER: {
                writeOpcode(isReference ? EXCEPTION_DISPATCHER_BLOCK_REFERENCE : EXCEPTION_DISPATCHER_BLOCK);
                break;
            }
        }

        writeUnsignedInt(blockId);
        finish(block);
    }

    @Override
    public void visitClosure(CirClosure closure) {
        final CirVariable[] parameters = closure.parameters();
        final Opcode implicitOperandOpcode = CLOSURE.withImplicitOperand(parameters.length);
        if (implicitOperandOpcode != null) {
            writeOpcode(implicitOperandOpcode);
        } else {
            writeOpcode(CLOSURE);
            writeUnsignedInt(parameters.length);
        }
        finish(closure);
    }

    @Override
    public void visitContinuation(CirContinuation continuation) {
        final CirVariable[] parameters = continuation.parameters();
        if (parameters.length == 1) {
            writeOpcode(CONTINUATION);
        } else {
            assert parameters.length == 0;
            writeOpcode(VOID_CONTINUATION);
        }
        finish(continuation);
    }

    @Override
    public void visitBuiltin(CirBuiltin builtin) {
        if (builtin.foldableWhenNotZeroVariant() == builtin) {
            writeOpcode(FOLDABLE_WHEN_NOT_ZERO_BUILTIN);
        } else if (builtin.foldableVariant() == builtin) {
            writeOpcode(FOLDABLE_BUILTIN);
        } else {
            writeOpcode(BUILTIN);
        }
        writeUnsignedInt(builtin.builtin.serial());
    }

    @Override
    public void visitSwitch(CirSwitch builtin) {
        if (builtin == CirSwitch.INT_EQUAL) {
            writeOpcode(SWITCH_INT_EQUAL);
        } else if (builtin == CirSwitch.INT_NOT_EQUAL) {
            writeOpcode(SWITCH_INT_NOT_EQUAL);
        } else if (builtin == CirSwitch.SIGNED_INT_LESS_THAN) {
            writeOpcode(SWITCH_SIGNED_INT_LESS_THAN);
        } else if (builtin == CirSwitch.SIGNED_INT_LESS_EQUAL) {
            writeOpcode(SWITCH_SIGNED_INT_LESS_EQUAL);
        } else if (builtin == CirSwitch.SIGNED_INT_GREATER_THAN) {
            writeOpcode(SWITCH_SIGNED_INT_GREATER_THAN);
        } else if (builtin == CirSwitch.SIGNED_INT_GREATER_EQUAL) {
            writeOpcode(SWITCH_SIGNED_INT_GREATER_EQUAL);
        } else if (builtin == CirSwitch.REFERENCE_EQUAL) {
            writeOpcode(SWITCH_REFERENCE_EQUAL);
        } else if (builtin == CirSwitch.REFERENCE_NOT_EQUAL) {
            writeOpcode(SWITCH_REFERENCE_NOT_EQUAL);
        }  else if (builtin == CirSwitch.SIGNED_INT_LESS_THAN) {
            writeOpcode(SWITCH_UNSIGNED_INT_LESS_THAN);
        } else if (builtin == CirSwitch.UNSIGNED_INT_LESS_EQUAL) {
            writeOpcode(SWITCH_UNSIGNED_INT_LESS_EQUAL);
        } else if (builtin == CirSwitch.UNSIGNED_INT_GREATER_THAN) {
            writeOpcode(SWITCH_UNSIGNED_INT_GREATER_THAN);
        } else if (builtin == CirSwitch.SIGNED_INT_GREATER_EQUAL) {
            writeOpcode(SWITCH_UNSIGNED_INT_GREATER_EQUAL);
        } else if (builtin == CirSwitch.REFERENCE_EQUAL) {
            writeOpcode(SWITCH_REFERENCE_EQUAL);
        } else {
            writeOpcode(SWITCH);
            writeKind(builtin.comparisonKind());
            writeEnum(builtin.valueComparator());
            writeUnsignedInt(builtin.numberOfMatches());
        }
        finish(builtin);
    }

    @Override
    public void visitConstant(CirConstant constant) {
        writeOpcode(CONSTANT);
        writeConstant(constant.value());
        finish(constant);
    }

    @Override
    public void visitExceptionContinuationParameter(CirExceptionContinuationParameter parameter) {
        new VariableWriter<CirExceptionContinuationParameter>(parameter) {
            @Override
            void write(CirExceptionContinuationParameter variable) {
                writeOpcode(EXCEPTION_CONTINUATION_VARIABLE);
                writeSerial(variable);
            }
        };
    }

    @Override
    public void visitNormalContinuationParameter(CirNormalContinuationParameter parameter) {
        new VariableWriter<CirNormalContinuationParameter>(parameter) {
            @Override
            void write(CirNormalContinuationParameter variable) {
                writeOpcode(NORMAL_CONTINUATION_VARIABLE);
                writeSerial(variable);
            }
        };
    }

    @Override
    public void visitLocalVariable(CirLocalVariable localVariable) {
        new VariableWriter<CirLocalVariable>(localVariable) {
            @Override
            void write(CirLocalVariable variable) {
                writeOpcode(LOCAL_VARIABLE);
                writeSerial(variable);
                writeKind(variable.kind());
                writeUnsignedInt(variable.slotIndex());
            }
        };
    }

    @Override
    public void visitMethodParameter(CirMethodParameter parameter) {
        new VariableWriter<CirMethodParameter>(parameter) {
            @Override
            void write(CirMethodParameter variable) {
                writeOpcode(METHOD_PARAMETER);
                writeSerial(variable);
                writeKind(variable.kind());
                writeUnsignedInt(variable.slotIndex());
            }
        };
    }

    @Override
    public void visitStackVariable(CirStackVariable stackVariable) {
        new VariableWriter<CirStackVariable>(stackVariable) {
            @Override
            void write(CirStackVariable variable) {
                writeOpcode(STACK_VARIABLE);
                writeSerial(variable);
                writeKind(variable.kind());
                writeUnsignedInt(variable.slotIndex());
            }
        };
    }

    @Override
    public void visitTemporaryVariable(CirTemporaryVariable stackVariable) {
        new VariableWriter<CirTemporaryVariable>(stackVariable) {
            @Override
            void write(CirTemporaryVariable variable) {
                writeOpcode(TEMP_VARIABLE);
                writeSerial(variable);
                writeKind(variable.kind());
            }
        };
    }

    @Override
    public void visitUndefined(CirValue.Undefined undefined) {
        writeOpcode(UNDEFINED);
        finish(undefined);
    }
}
