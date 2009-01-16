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
package com.sun.max.vm.compiler.cir.bytecode;

import java.io.*;
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.builtin.*;
import com.sun.max.vm.compiler.cir.bytecode.CirBytecode.*;
import com.sun.max.vm.compiler.cir.snippet.*;
import com.sun.max.vm.compiler.cir.variable.*;
import com.sun.max.vm.compiler.ir.IrBlock.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Converts a {@link CirBytecode} instance back into a graph of CirNode objects.
 *
 * @author Doug Simon
 */
public final class CirBytecodeReader {

    private final ByteArrayInputStream _stream;
    private final Object[] _constantPool;
    private final Stack<Object> _stack = new Stack<Object>();
    private final CirBlock[] _blocks;
    private final CirVariable[] _referencedVariables;
    private final CirNode _node;
    private final int _codeLength;
    private final PrintStream _traceStream;
    private String _detailedInstructionTrace;
    private final boolean _traceStack;

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
        _codeLength = bytecode.code().length;
        _stream = new ByteArrayInputStream(bytecode.code());
        _traceStream = traceStream;
        _traceStack = traceStack;
        _constantPool = bytecode.constantPool();
        _blocks = new CirBlock[bytecode.numberOfBlocks()];

        final int maxReferencedVariableSerial = bytecode.maxReferencedVariableSerial();
        if (maxReferencedVariableSerial == -1) {
            _referencedVariables = null;
        } else {
            _referencedVariables = new CirVariable[maxReferencedVariableSerial + 1];
        }
        if (_traceStream != null) {
            _traceStream.println("NumberOfBlocks: " + bytecode.numberOfBlocks());
            _traceStream.println("MaxVariableSerial: " + bytecode.maxReferencedVariableSerial());
            _traceStream.println("ConstantPool[" + _constantPool.length + "]:");
            for (int i = 0; i != _constantPool.length; ++i) {
                final Object constant = _constantPool[i];
                final String className = constant == null ? "null" : constant.getClass().getName();
                _traceStream.println("    " + i + ": " + className + " // " + constant);
            }
        }
        _node = read();
    }

    public CirNode node() {
        return _node;
    }

    /**
     * Decodes an unsigned integer from the current decoding position.
     *
     * @return the decoded value
     * @see    CirBytecodeWriter#writeUnsignedInt(int)
     */
    private int readUnsignedInt0() {
        int lo = _stream.read() & 0xFF;
        if (lo < 128) {
            /* 0xxxxxxx */
            return lo;
        }
        lo &= 0x7f;
        int mid = _stream.read() & 0xFF;
        if (mid < 128) {
            /* 1xxxxxxx 0xxxxxxx */
            return (mid << 7) + lo;
        }
        mid &= 0x7f;
        int hi = _stream.read() & 0xFF;
        if (hi < 128) {
            /* 1xxxxxxx 1xxxxxxx 0xxxxxxx */
            return (hi << 14) + (mid << 7) + lo;
        }
        hi &= 0x7f;
        final int last = _stream.read() & 0xFF;
        if (last < 128) {
            /* 1xxxxxxx 1xxxxxxx 1xxxxxxx 0xxxxxxx */
            return (last << 21) + (hi << 14) + (mid << 7) + lo;
        }
        throw ProgramError.unexpected();
    }

    private int readUnsignedInt() {
        final int value = readUnsignedInt0();
        if (_traceStream != null) {
            _traceStream.print(" " + value);
        }
        return value;
    }

    private <T> T readConstant() {
        final Class<T> type = null;
        final int index = readUnsignedInt();
        final T constant = StaticLoophole.cast(type, _constantPool[index]);
        if (_traceStream != null) {
            _detailedInstructionTrace += " " + constant;
        }
        return constant;
    }

    private CirSnippet readSnippet() {
        final int serial = readUnsignedInt();
        final CirSnippet snippet = CirSnippet.get(Snippet.snippets().get(serial));
        if (_traceStream != null) {
            _detailedInstructionTrace += " " + snippet.name();
        }
        return snippet;
    }

    private Opcode readOpcode() {
        return Opcode.VALUES.get(_stream.read());
    }

    private Kind readKind() {
        final char c = (char) readUnsignedInt();
        final Kind kind = Kind.fromCharacter(c);
        assert kind != null;
        if (_traceStream != null) {
            _detailedInstructionTrace += " " + kind;
        }
        return kind;
    }

    private BytecodeLocation readBytecodeLocation() {
        final ClassMethodActor classMethodActor = readConstant();
        if (classMethodActor == null) {
            return null;
        }
        final int pc = readUnsignedInt();
        if (_traceStream != null) {
            _detailedInstructionTrace += "@" + pc;
        }
        return new BytecodeLocation(classMethodActor, pc);
    }

    private static final CirVariable[] NO_PARAMETERS = CirClosure.NO_PARAMETERS;

    private <Node_Type extends CirNode> Node_Type[] pop(Node_Type[] values) {
        for (int i = values.length - 1; i >= 0; --i) {
            final Class<Node_Type> type = null;
            final Node_Type value = StaticLoophole.cast(type, pop());
            values[i] = value;
        }
        return values;
    }

    private <Node_Type extends CirNode> Node_Type pop() {
        final Class<Node_Type> type = null;
        return StaticLoophole.cast(type, _stack.pop());
    }

    private void push(Object object) {
        _stack.push(object);
    }

    private void pushVariable(CirVariable variable) {
        final int serial = variable.serial();
        if (serial < _referencedVariables.length) {
            _referencedVariables[serial] = variable;
        }
        push(variable);
    }

    private CirBlock readBlock(Role role, boolean isReference) {
        final int blockId = readUnsignedInt();
        CirBlock block = _blocks[blockId];

        if (isReference) {
            if (block == null) {
                block = new CirBlock(role);
                _blocks[blockId] = block;
            }
        } else {
            final CirClosure closure = (CirClosure) pop();

            if (block == null) {
                block = new CirBlock(role);
                _blocks[blockId] = block;
            }

            block.setClosure(closure);
        }
        return block;
    }

    private CirClosure readClosure(CirVariable[] parameters, CirCall body) {
        final BytecodeLocation bytecodeLocation = readBytecodeLocation();
        final CirClosure closure = new CirClosure(bytecodeLocation);
        closure.setBody(body);
        closure.setParameters(parameters);
        assert closure.verifyParameters();
        return closure;
    }

    private CirJavaFrameDescriptor popJavaFrameDescriptor(int numberOfJavaFrameDescriptors) {
        if (numberOfJavaFrameDescriptors == 0) {
            return null;
        }

        final BytecodeLocation bytecodeLocation = readBytecodeLocation();
        final CirValue[] locals = pop(CirCall.newArguments(readUnsignedInt()));
        final CirValue[] stackSlots = pop(CirCall.newArguments(readUnsignedInt()));

        return new CirJavaFrameDescriptor(popJavaFrameDescriptor(numberOfJavaFrameDescriptors - 1), bytecodeLocation, locals, stackSlots);
    }

    private CirCall readCall(int count) {
        final CirCall call = new CirCall();
        call.setArguments(pop(CirCall.newArguments(count)));
        call.setJavaFrameDescriptor(popJavaFrameDescriptor(readUnsignedInt()));
        call.setProcedure((CirValue) pop(), readBytecodeLocation());
        return call;
    }

    private <Node_Type extends CirNode> Node_Type read() {
        while (_stream.available() != 0) {

            if (_traceStream != null && _traceStack) {
                if (!_stack.isEmpty()) {
                    int index = 0;
                    for (Object object : _stack) {
                        _traceStream.println("    " + (index++) + ": " + object);
                    }
                }
            }

            final int address = _codeLength - _stream.available();
            final Opcode opcode = readOpcode();
            if (_traceStream != null) {
                _traceStream.print(address + ": " + opcode);
                _detailedInstructionTrace = "";
            }

            switch (opcode) {
                case CALL: {
                    push(readCall(readUnsignedInt()));
                    break;
                }
                case CALL_0:
                case CALL_1:
                case CALL_2:
                case CALL_3:
                case CALL_4:
                case CALL_5:
                case CALL_6: {
                    push(readCall(opcode.implicitOperand()));
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

                // Bytecode instructions for unsigned comparisons.

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
                    final BytecodeLocation bytecodeLocation = readBytecodeLocation();
                    pushVariable(new CirLocalVariable(serial, kind, slotIndex, bytecodeLocation));
                    break;
                }
                case METHOD_PARAMETER: {
                    final int serial = readUnsignedInt();
                    final Kind kind = readKind();
                    final int slotIndex = readUnsignedInt();
                    final BytecodeLocation bytecodeLocation = readBytecodeLocation();
                    pushVariable(new CirMethodParameter(serial, kind, slotIndex, bytecodeLocation));
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
                    final CirVariable variable = _referencedVariables[serial];
                    assert variable != null;
                    push(variable);
                    break;
                }
            }

            if (_traceStream != null) {
                if (_detailedInstructionTrace.length() != 0) {
                    _traceStream.println("  // " + _detailedInstructionTrace);
                } else {
                    _traceStream.println();
                }
            }
        }
        final Class<Node_Type> type = null;
        return StaticLoophole.cast(type, pop());
    }
}
