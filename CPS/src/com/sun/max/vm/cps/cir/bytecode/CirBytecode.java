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

import java.util.*;

import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.builtin.*;
import com.sun.max.vm.cps.cir.snippet.*;
import com.sun.max.vm.cps.cir.variable.*;

/**
 * A {@code CirBytecode} instance represents a CIR graph as a sequence of bytecodes. A {@link CirBytecodeWriter} converts
 * a CIR graph into this representation and a {@link CirBytecodeReader} converts it back into a graph of CirNode
 * objects.
 *
 * @author Doug Simon
 */
public class CirBytecode {

    /**
     * The opcodes for the CIR bytecode instructions. In the CIR bytecode instruction stream, all immediate operands are
     * unsigned integers between 0 and 0x0FFFFFFF. These operands are encoded as follows (where each 'x' is a bit of the
     * value being encoded):
     * <p>
     * <blockquote>
     *
     * <pre>
     *       Value range              Encoded bytes
     *       0       .. 127           0xxxxxxx
     *       128     .. 16383         1xxxxxxx 0xxxxxxx
     *       16384   .. 2097151       1xxxxxxx 1xxxxxxx 0xxxxxxx
     *       2097152 .. 268435455     1xxxxxxx 1xxxxxxx 1xxxxxxx 0xxxxxxx
     * </pre>
     * <p>
     * An operand surrounded by parenthesis indicates an index into the {@linkplain CirBytecode#constantPool() contant pool}.
     * </blockquote>
     * </p>
     *
     *
     * @author Doug Simon
     */
    public static enum Opcode {

        /**
         * {@link CirCall} instruction.
         * <pre>
         * Format: CALL count numberOfJavaFrameDescriptors {numberOfLocals numberOfStackSlots}*
         * </pre>
         * OR
         * <pre>
         * Format: CALL_[count] numberOfJavaFrameDescriptors {numberOfLocals numberOfStackSlots}*  // 0 <= count <= 6
         * </pre>
         * Stack: ..., procedure:CirValue, arguments:CirValue*count => ..., CirCall
         */
        CALL {
            @Override
            public Opcode withImplicitOperand(int value) {
                switch (value) {
                    case 0:
                        return CALL_0;
                    case 1:
                        return CALL_1;
                    case 2:
                        return CALL_2;
                    case 3:
                        return CALL_3;
                    case 4:
                        return CALL_4;
                    case 5:
                        return CALL_5;
                    case 6:
                        return CALL_6;
                }
                return null;
            }
        },

        /**
         * @see #CALL
         */
        CALL_0 {
            @Override
            public int implicitOperand() {
                return 0;
            }
        },

        /**
         * @see #CALL
         */
        CALL_1 {
            @Override
            public int implicitOperand() {
                return 1;
            }
        },

        /**
         * @see #CALL
         */
        CALL_2 {
            @Override
            public int implicitOperand() {
                return 2;
            }
        },

        /**
         * @see #CALL
         */
        CALL_3 {
            @Override
            public int implicitOperand() {
                return 3;
            }
        },

        /**
         * @see #CALL
         */
        CALL_4 {
            @Override
            public int implicitOperand() {
                return 4;
            }
        },

        /**
         * @see #CALL
         */
        CALL_5 {
            @Override
            public int implicitOperand() {
                return 5;
            }
        },

        /**
         * @see #CALL
         */
        CALL_6 {
            @Override
            public int implicitOperand() {
                return 6;
            }
        },

        /**
         * {@link CirCall} instruction for a {@linkplain CirCall#isNative() native function call}.
         * <pre>
         * Format: NATIVE_CALL count numberOfJavaFrameDescriptors {numberOfLocals numberOfStackSlots}*
         * </pre>
         * OR
         * <pre>
         * Format: NATIVE_CALL[count] numberOfJavaFrameDescriptors {numberOfLocals numberOfStackSlots}*  // 0 <= count <= 6
         * </pre>
         * Stack: ..., procedure:CirValue, arguments:CirValue*count => ..., CirCall
         */
        NATIVE_CALL,

        /**
         * {@link CirConstant} instruction.
         * <pre>
         * Format: CONSTANT (constant)
         * </pre>
         * Stack: ... => ..., CirConstant
         */
        CONSTANT,

        /**
         * {@link CirBlock} instruction.
         * <pre>
         * Format: [role]_BLOCK id numberOfCalls  // where role is NORMAL, EXCEPTION_HANDLER or EXCEPTION_DISPATCHER
         * </pre>
         * Stack: ..., closure:CirClosure,  => ..., CirBlock
         */
        NORMAL_BLOCK,

        /**
         * @see #NORMAL_BLOCK
         */
        EXCEPTION_DISPATCHER_BLOCK,

        /**
         * {@link CirBlock} instruction.
         * <pre>
         * Format: [role]_BLOCK_REFERENCE id  // where role is NORMAL, EXCEPTION_HANDLER or EXCEPTION_DISPATCHER
         * </pre>
         * Stack: ...  => ..., CirBlock
         */
        NORMAL_BLOCK_REFERENCE,

        /**
         * @see #NORMAL_BLOCK_REFERENCE
         */
        EXCEPTION_DISPATCHER_BLOCK_REFERENCE,

        /**
         * {@link CirBuiltin} instruction.
         * <pre>
         * Format: [variant]BUILTIN serial  // where variant is empty, FOLDABLE_ or FOLDABLE_WHEN_NOT_ZERO_
         * </pre>
         * Stack: ...  => ..., CirReflectionBuiltin
         */
        BUILTIN,

        /**
         * @see #BUILTIN
         */
        FOLDABLE_BUILTIN,

        /**
         * @see #BUILTIN
         */
        FOLDABLE_WHEN_NOT_ZERO_BUILTIN,

        /**
         * {@link CirSwitch} instruction.
         * <pre>
         *  Format: SWITCH_[kind_and_comparator]  // where kind_and_comparator is INT_EQUAL, INT_NOT_EQUAL, INT_LESS_THAN, INT_LESS_EQUAL,
         *                                        // INT_GREATER_THAN, INT_GREATER_EQUAL, REFERENCE_EQUAL or REFERENCE_NOT_EQUAL
         * </pre>
         * Stack: ... => ..., CirSwitch
         */
        SWITCH_INT_EQUAL,

        /**
         * @see #SWITCH_INT_EQUAL
         */
        SWITCH_INT_NOT_EQUAL,

        /**
         * @see #SWITCH_INT_EQUAL
         */
        SWITCH_SIGNED_INT_LESS_THAN,
        /**
         * @see #SWITCH_INT_EQUAL
         */
        SWITCH_SIGNED_INT_LESS_EQUAL,
        /**
         * @see #SWITCH_INT_EQUAL
         */
        SWITCH_SIGNED_INT_GREATER_THAN,
        /**
         * @see #SWITCH_INT_EQUAL
         */
        SWITCH_SIGNED_INT_GREATER_EQUAL,
        /**
         * @see #SWITCH_INT_EQUAL
         */
        SWITCH_REFERENCE_EQUAL,

        /**
         * @see #SWITCH_INT_EQUAL
         */
        SWITCH_REFERENCE_NOT_EQUAL,

        /**
         * New bytecode instructions for unsigned comparisons.
         *
         */
        SWITCH_UNSIGNED_INT_LESS_THAN,
        SWITCH_UNSIGNED_INT_LESS_EQUAL,
        SWITCH_UNSIGNED_INT_GREATER_THAN,
        SWITCH_UNSIGNED_INT_GREATER_EQUAL,

        /**
         * {@link CirSwitch} instruction.
         * <pre>
         * Format: SWITCH kind comparator numberOfMatches
         * </pre>
         * Stack: ...  => ..., CirSwitch
         */
        SWITCH,

        /**
         * {@link CirClosure} instruction.
         * <pre>
         * Format: CLOSURE count
         * </pre>
         * OR
         * <pre>
         * Format: CLOSURE_[count]  // 0 <= count <= 6
         * </pre>
         * Stack: ..., body:CirCall, parameters:CirVariable*count => ..., CirClosure
         */
        CLOSURE {

            @Override
            public Opcode withImplicitOperand(int value) {
                switch (value) {
                    case 0:
                        return CLOSURE_0;
                    case 1:
                        return CLOSURE_1;
                    case 2:
                        return CLOSURE_2;
                    case 3:
                        return CLOSURE_3;
                    case 4:
                        return CLOSURE_4;
                    case 5:
                        return CLOSURE_5;
                    case 6:
                        return CLOSURE_6;
                }
                return null;
            }
        },

        /**
         * @see #CLOSURE
         */
        CLOSURE_0 {
            @Override
            public int implicitOperand() {
                return 0;
            }
        },

        /**
         * @see #CLOSURE
         */
        CLOSURE_1 {
            @Override
            public int implicitOperand() {
                return 1;
            }
        },

        /**
         * @see #CLOSURE
         */
        CLOSURE_2 {
            @Override
            public int implicitOperand() {
                return 2;
            }
        },

        /**
         * @see #CLOSURE
         */
        CLOSURE_3 {
            @Override
            public int implicitOperand() {
                return 3;
            }
        },

        /**
         * @see #CLOSURE
         */
        CLOSURE_4 {
            @Override
            public int implicitOperand() {
                return 4;
            }
        },

        /**
         * @see #CLOSURE
         */
        CLOSURE_5 {
            @Override
            public int implicitOperand() {
                return 5;
            }
        },

        /**
         * @see #CLOSURE
         */
        CLOSURE_6 {
            @Override
            public int implicitOperand() {
                return 6;
            }
        },

        /**
         * {@link CirContinuation} instruction.
         * <pre>
         * Format: CONTINUATION
         * </pre>
         * Stack: ..., body:CirCall, parameter:CirVariable => ..., CirContinuation
         */
        CONTINUATION,

        /**
         * {@link CirContinuation} instruction.
         * <pre>
         * Format: VOID_CONTINUATION
         * </pre>
         * Stack: ..., body:CirCall => ..., CirContinuation
         */
        VOID_CONTINUATION,

        /**
         * {@link CirSnippet} instruction.
         * <pre>
         * Format: SNIPPET serial
         * </pre>
         * Stack: ...  => ..., CirSnippet
         */
        SNIPPET,

        /**
         * {@link CirCompiledMethod} instruction.
         * <pre>
         * Format: COMPILED_METHOD (cirCompiledMethod)
         * </pre>
         * Stack: ...  => ..., CirCompiledMethod
         */
        METHOD,

        /**
         * {@link CirExceptionContinuationParameter} instruction.
         * <pre>
         * Format: EXCEPTION_CONTINUATION_VARIABLE serial
         * </pre>
         * Stack: ...  => ..., CirExceptionContinuationParameter
         */
        EXCEPTION_CONTINUATION_VARIABLE,

        /**
         * {@link CirNormalContinuationParameter} instruction.
         * <pre>
         * Format: NORMAL_CONTINUATION_VARIABLE serial
         * </pre>
         * Stack: ...  => ..., CirNormalContinuationParameter
         */
        NORMAL_CONTINUATION_VARIABLE,

        /**
         * {@link CirLocalVariable} instruction.
         * <pre>
         * Format: LOCAL_VARIABLE serial kind slot
         * </pre>
         * Stack: ...  => ..., CirLocalVariable
         */
        LOCAL_VARIABLE,

        /**
         * {@link CirMethodParameter} instruction.
         * <pre>
         * Format: METHOD_PARAMETER serial kind slot
         * </pre>
         * Stack: ...  => ..., CirMethodParameter
         */
        METHOD_PARAMETER,

        /**
         * {@link CirStackVariable} instruction.
         * <pre>
         * Format: STACK_VARIABLE serial kind slot
         * </pre>
         * Stack: ...  => ..., CirStackVariable
         */
        STACK_VARIABLE,

        /**
         * {@link CirTemporaryVariable} instruction.
         * <pre>
         * Format: TEMP_VARIABLE serial kind
         * </pre>
         * Stack: ...  => ..., CirTemporaryVariable
         */
        TEMP_VARIABLE,

        /**
         * {@link CirVariable} instruction.
         * <pre>
         * Format: VARIABLE_REFERENCE serial
         * </pre>
         * Stack: ...  => ..., CirVariable
         */
        VARIABLE_REFERENCE,

        /**
         * {@link CirValue.Undefined} instruction.
         * <pre>
         * Format: UNDEFINED
         * </pre>
         * Stack: ...  => ..., UNDEFINED
         */
        UNDEFINED;

        public static final List<Opcode> VALUES = Arrays.asList(values());

        /**
         * Gets the value of the operand implicit in the opcode.
         *
         * @throw IllegalArgumentException if this opcode does not encode an operand value.
         */
        public int implicitOperand() {
            throw new IllegalArgumentException(this + " has no immediate operand");
        }

        /**
         * Gets the variation of this opcode that encodes a given operand value.
         *
         * @return null if this opcode has no variation that encodes {@code value}
         */
        public Opcode withImplicitOperand(int value) {
            return null;
        }
    }

    private final byte[] code;
    private final Object[] constantPool;
    private final int numberOfBlocks;
    private final int maxReferencedVariableSerial;

    public CirBytecode(byte[] code, Object[] constantPool, int numberOfBlocks, int maxReferencedVariableSerial) {
        this.code = code;
        this.constantPool = constantPool;
        this.numberOfBlocks = numberOfBlocks;
        this.maxReferencedVariableSerial = maxReferencedVariableSerial;
    }

    public byte[] code() {
        return code;
    }

    public Object[] constantPool() {
        return constantPool;
    }

    /**
     * Gets the number of blocks in the instruction stream. Each instruction defining or referencing a block in the
     * instruction stream includes a block ID operand whose value will be in the range {@code [0 .. numberOfBlocks())}.
     */
    public int numberOfBlocks() {
        return numberOfBlocks;
    }

    /**
     * Gets the highest {@linkplain CirVariable#serial() serial number} of a variable referenced via a
     * {@link Opcode#VARIABLE_REFERENCE} instruction in this CIR bytecode. Such references always succeed the referenced
     * variable in the instruction stream.
     *
     * A returned value of -1 indicates that there are no variable references in the stream.
     */
    public int maxReferencedVariableSerial() {
        return maxReferencedVariableSerial;
    }
}
