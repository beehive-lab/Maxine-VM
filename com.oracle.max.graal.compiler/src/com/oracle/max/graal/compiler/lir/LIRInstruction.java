/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.compiler.lir;

import com.oracle.max.graal.compiler.util.*;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiValue.Formatter;

/**
 * The {@code LIRInstruction} class definition.
 */
public class LIRInstruction {

    public static final CiValue[] NO_OPERANDS = {};

    public static final OperandMode[] OPERAND_MODES = OperandMode.values();

    /**
     * Constants denoting how a LIR instruction uses an operand. Any combination of these modes
     * can be applied to an operand as long as every operand has at least one mode applied to it.
     */
    public enum OperandMode {
        /**
         * An operand that is defined by a LIR instruction and is live after the code emitted for a LIR instruction.
         */
        Output,

        /**
         * An operand that is used by a LIR instruction and is live before the code emitted for a LIR instruction.
         * Unless such an operand is also an output or temp operand, it must not be modified by a LIR instruction.
         */
        Input,

        /**
         * An operand that is both modified and used by a LIR instruction.
         */
        Temp
    }

    /**
     * The opcode of this instruction.
     */
    public final LIROpcode code;

    /**
     * The result operand for this instruction.
     */
    protected CiValue result;

    protected final CiValue[] inputs;

    protected final CiValue[] temps;

    /**
     * Used to emit debug information.
     */
    public final LIRDebugInfo info;

    /**
     * Value id for register allocation.
     */
    private int id;

    /**
     * Determines if all caller-saved registers are destroyed by this instruction.
     */
    public final boolean hasCall;

    /**
     * Constructs a new LIR instruction that has no input or temp operands.
     *
     * @param opcode the opcode of the new instruction
     * @param result the operand that holds the operation result of this instruction. This will be
     *            {@link CiValue#IllegalValue} for instructions that do not produce a result.
     * @param info the {@link LIRDebugInfo} info that is to be preserved for the instruction. This will be {@code null} when no debug info is required for the instruction.
     * @param hasCall specifies if all caller-saved registers are destroyed by this instruction
     */
    public LIRInstruction(LIROpcode opcode, CiValue result, LIRDebugInfo info) {
        this(opcode, result, info, false, NO_OPERANDS, NO_OPERANDS);
    }

    public LIRInstruction(LIROpcode opcode, CiValue result, LIRDebugInfo info, CiValue... operands) {
        this (opcode, result, info, false, operands, NO_OPERANDS);
    }

    /**
     * Constructs a new LIR instruction.
     *
     * @param opcode the opcode of the new instruction
     * @param result the operand that holds the operation result of this instruction. This will be
     *            {@link CiValue#IllegalValue} for instructions that do not produce a result.
     * @param info the {@link LIRDebugInfo} that is to be preserved for the instruction. This will be {@code null} when no debug info is required for the instruction.
     * @param hasCall specifies if all caller-saved registers are destroyed by this instruction
     * @param inputs the input operands for the instruction
     * @param temps the temp operands for the instruction
     */
    public LIRInstruction(LIROpcode opcode, CiValue result, LIRDebugInfo info, boolean hasCall, CiValue[] inputs, CiValue[] temps) {
        this.code = opcode;
        this.info = info;
        this.hasCall = hasCall;

        this.result = result;
        this.inputs = inputs;
        this.temps = temps;

        this.id = -1;
    }

    public final int id() {
        return id;
    }

    public final void setId(int id) {
        this.id = id;
    }

    /**
     * Gets an input operand of this instruction.
     *
     * @param index the index of the operand requested.
     * @return the {@code index}'th input operand.
     */
    public final CiValue input(int index) {
        return inputs[index];
    }

    /**
     * Gets a temp operand of this instruction.
     *
     * @param index the index of the operand requested.
     * @return the {@code index}'th temp operand.
     */
    public final CiValue temp(int index) {
        return temps[index];
    }

    /**
     * Gets the result operand for this instruction.
     *
     * @return return the result operand
     */
    public final CiValue result() {
        return result;
    }

    /**
     * Gets the instruction name.
     *
     * @return the name of the enum constant that represents the instruction opcode, exactly as declared in the enum
     *         LIROpcode declaration.
     */
    public String name() {
        return code.getClass().getSimpleName();
    }

    /**
     * Gets the operation performed by this instruction in terms of its operands as a string.
     */
    public String operationString(Formatter operandFmt) {
        StringBuilder buf = new StringBuilder();
        if (result.isLegal()) {
            buf.append(operandFmt.format(result)).append(" = ");
        }
        if (inputs.length > 1) {
            buf.append("(");
        }
        String sep = "";
        for (CiValue input : inputs) {
            buf.append(sep).append(operandFmt.format(input));
            sep = ", ";
        }
        if (inputs.length > 1) {
            buf.append(")");
        }

        if (temps.length > 1) {
            buf.append("{");
        }
        sep = "";
        for (CiValue temp : temps) {
            buf.append(sep).append(operandFmt.format(temp));
            sep = ", ";
        }
        if (temps.length > 1) {
            buf.append("}");
        }
        return buf.toString();
    }

    public boolean hasOperands() {
        if (info != null || hasCall) {
            return true;
        }
        return inputs.length > 0 || temps.length > 0;
    }

    public final int operandCount(OperandMode mode) {
        switch (mode) {
            case Output: return result.isLegal() ? 1 : 0;
            case Input:  return inputs.length;
            case Temp:   return temps.length;
            default:     throw Util.shouldNotReachHere();
        }
    }

    public final CiValue operandAt(OperandMode mode, int index) {
        assert index < operandCount(mode);
        switch (mode) {
            case Output: return result;
            case Input:  return inputs[index];
            case Temp:   return temps[index];
            default:     throw Util.shouldNotReachHere();
        }
    }

    public final void setOperandAt(OperandMode mode, int index, CiValue location) {
        assert index < operandCount(mode);
        assert location.kind != CiKind.Illegal;
        assert operandAt(mode, index).isVariable();
        switch (mode) {
            case Output: result = location; break;
            case Input:  inputs[index] = location; break;
            case Temp:   temps[index] = location; break;
            default:     throw Util.shouldNotReachHere();
        }
    }

    public final LIRBlock exceptionEdge() {
        return (info == null) ? null : info.exceptionEdge();
    }

    @Override
    public String toString() {
        return toString(Formatter.DEFAULT);
    }

    public final String toStringWithIdPrefix() {
        if (id != -1) {
            return String.format("%4d %s", id, toString());
        }
        return "     " + toString();
    }

    protected static String refMapToString(CiDebugInfo debugInfo, Formatter operandFmt) {
        StringBuilder buf = new StringBuilder();
        if (debugInfo.hasStackRefMap()) {
            CiBitMap bm = debugInfo.frameRefMap;
            for (int slot = bm.nextSetBit(0); slot >= 0; slot = bm.nextSetBit(slot + 1)) {
                if (buf.length() != 0) {
                    buf.append(", ");
                }
                buf.append(operandFmt.format(CiStackSlot.get(CiKind.Object, slot)));
            }
        }
        if (debugInfo.hasRegisterRefMap()) {
            CiBitMap bm = debugInfo.registerRefMap;
            for (int reg = bm.nextSetBit(0); reg >= 0; reg = bm.nextSetBit(reg + 1)) {
                if (buf.length() != 0) {
                    buf.append(", ");
                }
                buf.append("r").append(reg);
            }
        }
        return buf.toString();
    }

    protected void appendDebugInfo(StringBuilder buf, Formatter operandFmt, LIRDebugInfo info) {
        if (info != null) {
            buf.append(" [bci:").append(info.state.bci);
            if (info.hasDebugInfo()) {
                CiDebugInfo debugInfo = info.debugInfo();
                String refmap = refMapToString(debugInfo, operandFmt);
                if (refmap.length() != 0) {
                    buf.append(", refmap(").append(refmap.trim()).append(')');
                }
            }
            buf.append(']');
        }
    }

    public String toString(Formatter operandFmt) {
        StringBuilder buf = new StringBuilder(name()).append(' ').append(operationString(operandFmt));
        appendDebugInfo(buf, operandFmt, info);
        return buf.toString();
    }
}
