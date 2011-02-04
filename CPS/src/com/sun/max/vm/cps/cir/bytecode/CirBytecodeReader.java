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
package com.sun.max.vm.cps.cir.bytecode;

import java.io.*;
import java.util.*;

import com.sun.max.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.builtin.*;
import com.sun.max.vm.cps.cir.bytecode.CirBytecode.*;
import com.sun.max.vm.cps.cir.snippet.*;
import com.sun.max.vm.cps.cir.variable.*;
import com.sun.max.vm.cps.ir.IrBlock.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Converts a {@link CirBytecode} instance back into a graph of CirNode objects.
 *
 * @author Doug Simon
 */
public final class CirBytecodeReader {

    private final ByteArrayInputStream stream;
    private final Object[] constantPool;
    private final Stack<Object> stack = new Stack<Object>();
    private final CirBlock[] blocks;
    private final CirVariable[] referencedVariables;
    private final CirNode node;
    private final int codeLength;
    private final PrintStream traceStream;
    private String detailedInstructionTrace;
    private final boolean traceStack;

    public static CirNode read(CirBytecode bytecode) {
        return new CirBytecodeReader(bytecode, null, false).node();
    }

    public static void trace(CirNode node, PrintStream traceStream, boolean traceStack) {
        final CirBytecode bytecode = new CirBytecodeWriter(node, "").bytecode();
        trace(bytecode, traceStream, traceStack);
    }

    public static void trace(CirBytecode bytecode, PrintStream traceStream, boolean traceStack) {
        new CirBytecodeReader(bytecode, traceStream, traceStack);
    }

    private CirBytecodeReader(CirBytecode bytecode, PrintStream traceStream, boolean traceStack) {
        this.codeLength = bytecode.code().length;
        this.stream = new ByteArrayInputStream(bytecode.code());
        this.traceStream = traceStream;
        this.traceStack = traceStack;
        this.constantPool = bytecode.constantPool();
        this.blocks = new CirBlock[bytecode.numberOfBlocks()];

        final int maxReferencedVariableSerial = bytecode.maxReferencedVariableSerial();
        if (maxReferencedVariableSerial == -1) {
            this.referencedVariables = null;
        } else {
            this.referencedVariables = new CirVariable[maxReferencedVariableSerial + 1];
        }
        if (traceStream != null) {
            traceStream.println("NumberOfBlocks: " + bytecode.numberOfBlocks());
            traceStream.println("MaxVariableSerial: " + bytecode.maxReferencedVariableSerial());
            traceStream.println("ConstantPool[" + constantPool.length + "]:");
            for (int i = 0; i != constantPool.length; ++i) {
                final Object constant = constantPool[i];
                final String className = constant == null ? "null" : constant.getClass().getName();
                traceStream.println("    " + i + ": " + className + " // " + constant);
            }
        }
        this.node = read();
    }

    public CirNode node() {
        return node;
    }

    /**
     * Decodes an unsigned integer from the current decoding position.
     *
     * @return the decoded value
     * @see    CirBytecodeWriter#writeUnsignedInt(int)
     */
    private int readUnsignedInt0() {
        int lo = stream.read() & 0xFF;
        if (lo < 128) {
            /* 0xxxxxxx */
            return lo;
        }
        lo &= 0x7f;
        int mid = stream.read() & 0xFF;
        if (mid < 128) {
            /* 1xxxxxxx 0xxxxxxx */
            return (mid << 7) + lo;
        }
        mid &= 0x7f;
        int hi = stream.read() & 0xFF;
        if (hi < 128) {
            /* 1xxxxxxx 1xxxxxxx 0xxxxxxx */
            return (hi << 14) + (mid << 7) + lo;
        }
        hi &= 0x7f;
        final int last = stream.read() & 0xFF;
        if (last < 128) {
            /* 1xxxxxxx 1xxxxxxx 1xxxxxxx 0xxxxxxx */
            return (last << 21) + (hi << 14) + (mid << 7) + lo;
        }
        throw ProgramError.unexpected();
    }

    private int readUnsignedInt() {
        final int value = readUnsignedInt0();
        if (traceStream != null) {
            traceStream.print(" " + value);
        }
        return value;
    }

    private <T> T readConstant() {
        final Class<T> type = null;
        final int index = readUnsignedInt();
        final T constant = Utils.cast(type, constantPool[index]);
        if (traceStream != null) {
            detailedInstructionTrace += " " + constant;
        }
        return constant;
    }

    private CirSnippet readSnippet() {
        final int serial = readUnsignedInt();
        final CirSnippet snippet = CirSnippet.get(Snippet.snippets().get(serial));
        if (traceStream != null) {
            detailedInstructionTrace += " " + snippet.name();
        }
        return snippet;
    }

    private Opcode readOpcode() {
        return Opcode.VALUES.get(stream.read());
    }

    private Kind readKind() {
        final char c = (char) readUnsignedInt();
        final Kind kind = Kind.fromCharacter(c);
        assert kind != null;
        if (traceStream != null) {
            detailedInstructionTrace += " " + kind;
        }
        return kind;
    }

    private static final CirVariable[] NO_PARAMETERS = CirClosure.NO_PARAMETERS;

    private <Node_Type extends CirNode> Node_Type[] pop(Node_Type[] values) {
        for (int i = values.length - 1; i >= 0; --i) {
            final Class<Node_Type> type = null;
            final Node_Type value = Utils.cast(type, pop());
            values[i] = value;
        }
        return values;
    }

    private <Node_Type extends CirNode> Node_Type pop() {
        final Class<Node_Type> type = null;
        return Utils.cast(type, stack.pop());
    }

    private void push(Object object) {
        stack.push(object);
    }

    private void pushVariable(CirVariable variable) {
        final int serial = variable.serial();
        if (serial < referencedVariables.length) {
            referencedVariables[serial] = variable;
        }
        push(variable);
    }

    private CirBlock readBlock(Role role, boolean isReference) {
        final int blockId = readUnsignedInt();
        CirBlock block = blocks[blockId];

        if (isReference) {
            if (block == null) {
                block = new CirBlock(role);
                blocks[blockId] = block;
            }
        } else {
            final CirClosure closure = (CirClosure) pop();

            if (block == null) {
                block = new CirBlock(role);
                blocks[blockId] = block;
            }

            block.setClosure(closure);
        }
        return block;
    }

    private CirClosure readClosure(CirVariable[] parameters, CirCall body) {
        final CirClosure closure = new CirClosure();
        closure.setBody(body);
        closure.setParameters(parameters);
        assert closure.verifyParameters();
        return closure;
    }

    private CirJavaFrameDescriptor popJavaFrameDescriptor(int numberOfJavaFrameDescriptors) {
        if (numberOfJavaFrameDescriptors == 0) {
            return null;
        }

        final ClassMethodActor classMethodActor = readConstant();
        final int bytecodePosition = readUnsignedInt();
        if (traceStream != null) {
            detailedInstructionTrace += "@" + bytecodePosition;
        }
        final CirValue[] locals = pop(CirCall.newArguments(readUnsignedInt()));
        final CirValue[] stackSlots = pop(CirCall.newArguments(readUnsignedInt()));

        return new CirJavaFrameDescriptor(popJavaFrameDescriptor(numberOfJavaFrameDescriptors - 1), classMethodActor, bytecodePosition, locals, stackSlots);
    }

    private CirCall readCall(int count, boolean isNative) {
        final CirCall call = new CirCall();
        call.setArguments(pop(CirCall.newArguments(count)));
        call.setJavaFrameDescriptor(popJavaFrameDescriptor(readUnsignedInt()));
        call.setProcedure((CirValue) pop());
        if (isNative) {
            call.setIsNative();
        }
        return call;
    }

    private <Node_Type extends CirNode> Node_Type read() {
        while (stream.available() != 0) {

            if (traceStream != null && traceStack) {
                if (!stack.isEmpty()) {
                    int index = 0;
                    for (Object object : stack) {
                        traceStream.println("    " + (index++) + ": " + object);
                    }
                }
            }

            final int address = codeLength - stream.available();
            final Opcode opcode = readOpcode();
            if (traceStream != null) {
                traceStream.print(address + ": " + opcode);
                detailedInstructionTrace = "";
            }

            switch (opcode) {
                case NATIVE_CALL: {
                    push(readCall(readUnsignedInt(), true));
                    break;
                }
                case CALL: {
                    push(readCall(readUnsignedInt(), false));
                    break;
                }
                case CALL_0:
                case CALL_1:
                case CALL_2:
                case CALL_3:
                case CALL_4:
                case CALL_5:
                case CALL_6: {
                    push(readCall(opcode.implicitOperand(), false));
                    break;
                }
                case CONSTANT: {
                    final Value value = readConstant();
                    push(new CirConstant(value));
                    break;
                }
                case NORMAL_BLOCK: {
                    push(readBlock(Role.NORMAL, false));
                    break;
                }
                case EXCEPTION_DISPATCHER_BLOCK: {
                    push(readBlock(Role.EXCEPTION_DISPATCHER, false));
                    break;
                }
                case NORMAL_BLOCK_REFERENCE: {
                    push(readBlock(Role.NORMAL, true));
                    break;
                }
                case EXCEPTION_DISPATCHER_BLOCK_REFERENCE: {
                    push(readBlock(Role.EXCEPTION_DISPATCHER, true));
                    break;
                }
                case BUILTIN: {
                    final int serial = readUnsignedInt();
                    push(CirBuiltin.get(Builtin.builtins().get(serial)));
                    break;
                }
                case FOLDABLE_BUILTIN: {
                    final int serial = readUnsignedInt();
                    push(CirBuiltin.get(Builtin.builtins().get(serial)).foldableVariant());
                    break;
                }
                case FOLDABLE_WHEN_NOT_ZERO_BUILTIN: {
                    final int serial = readUnsignedInt();
                    push(CirBuiltin.get(Builtin.builtins().get(serial)).foldableWhenNotZeroVariant());
                    break;
                }
                case SWITCH_INT_EQUAL: {
                    push(CirSwitch.INT_EQUAL);
                    break;
                }
                case SWITCH_INT_NOT_EQUAL: {
                    push(CirSwitch.INT_NOT_EQUAL);
                    break;
                }
                case SWITCH_SIGNED_INT_LESS_THAN: {
                    push(CirSwitch.SIGNED_INT_LESS_THAN);
                    break;
                }
                case SWITCH_SIGNED_INT_LESS_EQUAL: {
                    push(CirSwitch.SIGNED_INT_LESS_EQUAL);
                    break;
                }
                case SWITCH_SIGNED_INT_GREATER_THAN: {
                    push(CirSwitch.SIGNED_INT_GREATER_THAN);
                    break;
                }
                case SWITCH_SIGNED_INT_GREATER_EQUAL: {
                    push(CirSwitch.SIGNED_INT_GREATER_EQUAL);
                    break;
                }
                case SWITCH_REFERENCE_EQUAL: {
                    push(CirSwitch.REFERENCE_EQUAL);
                    break;
                }
                case SWITCH_REFERENCE_NOT_EQUAL: {
                    push(CirSwitch.REFERENCE_NOT_EQUAL);
                    break;
                }

                // Bytecodes instructions for unsigned comparisons.

                case SWITCH_UNSIGNED_INT_LESS_THAN: {
                    push(CirSwitch.UNSIGNED_INT_LESS_THAN);
                    break;
                }
                case SWITCH_UNSIGNED_INT_LESS_EQUAL: {
                    push(CirSwitch.UNSIGNED_INT_LESS_EQUAL);
                    break;
                }
                case SWITCH_UNSIGNED_INT_GREATER_THAN: {
                    push(CirSwitch.UNSIGNED_INT_GREATER_THAN);
                    break;
                }
                case SWITCH_UNSIGNED_INT_GREATER_EQUAL: {
                    push(CirSwitch.UNSIGNED_INT_GREATER_EQUAL);
                    break;
                }

                case SWITCH: {
                    final Kind kind = readKind();
                    final ValueComparator valueComparator = ValueComparator.VALUES.get(readUnsignedInt());
                    final int numberOfMatches = readUnsignedInt();
                    push(new CirSwitch(kind, valueComparator, numberOfMatches));
                    break;
                }
                case CONTINUATION: {
                    final CirVariable parameter = pop();
                    final CirCall body = pop();
                    final CirContinuation continuation = new CirContinuation(parameter);
                    continuation.setBody(body);
                    push(continuation);
                    break;
                }
                case VOID_CONTINUATION: {
                    final CirCall body = pop();
                    final CirContinuation continuation = new CirContinuation();
                    continuation.setBody(body);
                    push(continuation);
                    break;
                }
                case CLOSURE: {
                    final int count = readUnsignedInt();
                    push(readClosure(count == 0 ? NO_PARAMETERS : pop(new CirVariable[count]), (CirCall) pop()));
                    break;
                }
                case CLOSURE_0: {
                    push(readClosure(NO_PARAMETERS, (CirCall) pop()));
                    break;
                }
                case CLOSURE_1:
                case CLOSURE_2:
                case CLOSURE_3:
                case CLOSURE_4:
                case CLOSURE_5:
                case CLOSURE_6: {
                    final int count = opcode.implicitOperand();
                    push(readClosure(pop(new CirVariable[count]), (CirCall) pop()));
                    break;
                }
                case SNIPPET: {
                    push(readSnippet());
                    break;
                }
                case METHOD: {
                    final CirMethod cirMethod = readConstant();
                    push(cirMethod);
                    break;
                }
                case EXCEPTION_CONTINUATION_VARIABLE: {
                    pushVariable(new CirExceptionContinuationParameter(readUnsignedInt()));
                    break;
                }
                case NORMAL_CONTINUATION_VARIABLE: {
                    pushVariable(new CirNormalContinuationParameter(readUnsignedInt()));
                    break;
                }
                case LOCAL_VARIABLE: {
                    final int serial = readUnsignedInt();
                    final Kind kind = readKind();
                    final int slotIndex = readUnsignedInt();
                    pushVariable(new CirLocalVariable(serial, kind, slotIndex));
                    break;
                }
                case METHOD_PARAMETER: {
                    final int serial = readUnsignedInt();
                    final Kind kind = readKind();
                    final int slotIndex = readUnsignedInt();
                    pushVariable(new CirMethodParameter(serial, kind, slotIndex));
                    break;
                }
                case STACK_VARIABLE: {
                    final int serial = readUnsignedInt();
                    final Kind kind = readKind();
                    final int slotIndex = readUnsignedInt();
                    pushVariable(new CirStackVariable(serial, kind, slotIndex));
                    break;
                }
                case TEMP_VARIABLE: {
                    final int serial = readUnsignedInt();
                    final Kind kind = readKind();
                    pushVariable(new CirTemporaryVariable(serial, kind));
                    break;
                }
                case UNDEFINED:
                    push(CirValue.UNDEFINED);
                    break;
                case VARIABLE_REFERENCE: {
                    final int serial = readUnsignedInt();
                    final CirVariable variable = referencedVariables[serial];
                    assert variable != null;
                    push(variable);
                    break;
                }
            }

            if (traceStream != null) {
                if (detailedInstructionTrace.length() != 0) {
                    traceStream.println("  // " + detailedInstructionTrace);
                } else {
                    traceStream.println();
                }
            }
        }
        final Class<Node_Type> type = null;
        return Utils.cast(type, pop());
    }
}
