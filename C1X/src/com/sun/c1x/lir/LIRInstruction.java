/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.lir;

import static com.sun.c1x.C1XCompilation.*;

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.globalstub.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.lir.LIROperand.LIRAddressOperand;
import com.sun.c1x.lir.LIROperand.LIRVariableOperand;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiRegister.*;

/**
 * The {@code LIRInstruction} class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 */
public abstract class LIRInstruction {

    private static final LIROperand ILLEGAL_SLOT = new LIROperand(CiValue.IllegalValue);

    private static final CiValue[] NO_OPERANDS = {};

    /**
     * The opcode of this instruction.
     */
    public final LIROpcode code;

    /**
     * The result operand for this instruction.
     */
    private final LIROperand result;

    /**
     * The input and temporary operands of this instruction.
     */
    protected final LIROperand[] inputAndTempOperands;

    /**
     * Used to emit debug information.
     */
    public final LIRDebugInfo info;

    /**
     * Value id for register allocation.
     */
    public int id;

    /**
     * Link to the HIR instruction for debugging purposes.
     */
    public Value source;

    public final boolean hasCall;

    public GlobalStub globalStub;

    public static final OperandMode[] OPERAND_MODES = OperandMode.values();

    public enum OperandMode {
        Output,
        Input,
        Temp
    }

    private int outputCount;
    private int allocatorInputCount;
    private int allocatorTempCount;
    private int allocatorTempInputCount;

    /**
     * The set of operands that must be known to the register allocator either to bind a register
     * or stack slot to a {@linkplain CiVariable variable} or to inform the allocator about operands
     * that are already fixed to a specific register.
     * This set excludes all constant operands as well as operands that are bound to
     * a stack slot in the {@linkplain CiStackSlot#inCallerFrame() caller's frame}.
     */
    final List<CiValue> allocatorOperands = new ArrayList<CiValue>(3);

    /**
     * Constructs a new LIR instruction.
     *
     * @param opcode the opcode of the new instruction
     * @param result the operand that holds the operation result of this instruction. This will be
     *            {@link CiValue#IllegalValue} for instructions that do not produce a result.
     * @param info the {@link LIRDebugInfo} info that is to be preserved for the instruction. This will be {@code null} when no debug info is required for the instruction.
     * @param hasCall
     */
    public LIRInstruction(LIROpcode opcode, CiValue result, LIRDebugInfo info, boolean hasCall) {
        this(opcode, result, info, hasCall, 0, 0, NO_OPERANDS);
    }

    /**
     * Constructs a new LIR instruction.
     *
     * @param opcode the opcode of the new instruction
     * @param result the operand that holds the operation result of this instruction. This will be
     *            {@link CiValue#IllegalValue} for instructions that do not produce a result.
     * @param info the {@link LIRDebugInfo} that is to be preserved for the instruction. This will be {@code null} when no debug info is required for the instruction.
     * @param hasCall
     */
    public LIRInstruction(LIROpcode opcode, CiValue result, LIRDebugInfo info, boolean hasCall, int tempInput, int temp, CiValue... operands) {
        this.code = opcode;
        this.info = info;
        this.hasCall = hasCall;

        assert opcode != LIROpcode.Move || result != CiValue.IllegalValue;
        this.result = initOutput(result);

        C1XMetrics.LIRInstructions++;

        if (opcode == LIROpcode.Move) {
            C1XMetrics.LIRMoveInstructions++;
        }

        id = -1;
        this.inputAndTempOperands = new LIROperand[operands.length];
        initInputsAndTemps(tempInput, temp, operands);

        assert verifyOperands();
    }

    private LIROperand initOutput(CiValue output) {
        assert output != null;
        if (output != CiValue.IllegalValue) {
            if (output.isAddress()) {
                return addAddress((CiAddress) output);
            }
            if (output.isStackSlot()) {
                return new LIROperand(output);
            }

            assert allocatorOperands.size() == outputCount;
            allocatorOperands.add(output);
            outputCount++;
            return new LIRVariableOperand(allocatorOperands.size() - 1);
        } else {
            return ILLEGAL_SLOT;
        }
    }

    /**
     * Adds a {@linkplain CiValue#isLegal() legal} value that is part of an address to
     * the list of {@linkplain #allocatorOperands register allocator operands}. If
     * the value is {@linkplain CiVariable variable}, then its index into the list
     * of register allocator operands is returned. Otherwise, {@code -1} is returned.
     */
    private int addAddressPart(CiValue part) {
        if (part.isRegister()) {
            allocatorInputCount++;
            allocatorOperands.add(part);
            return -1;
        }
        if (part.isVariable()) {
            allocatorInputCount++;
            allocatorOperands.add(part);
            return allocatorOperands.size() - 1;
        }
        assert part.isIllegal();
        return -1;
    }

    private LIROperand addAddress(CiAddress address) {
        assert address.base.isVariableOrRegister();

        int base = addAddressPart(address.base);
        int index = addAddressPart(address.index);

        if (base != -1 || index != -1) {
            return new LIRAddressOperand(base, index, address);
        }

        assert address.base.isRegister() && (address.index.isIllegal() || address.index.isRegister());
        return new LIROperand(address);
    }

    private LIROperand addOperand(CiValue operand, boolean isInput, boolean isTemp) {
        assert operand != null;
        if (operand != CiValue.IllegalValue) {
            assert !(operand.isAddress());
            if (operand.isStackSlot()) {
                // no variables to add
                return new LIROperand(operand);
            } else if (operand.isConstant()) {
                // no variables to add
                return new LIROperand(operand);
            } else {
                assert allocatorOperands.size() == outputCount + allocatorInputCount + allocatorTempInputCount + allocatorTempCount;
                allocatorOperands.add(operand);

                if (isInput && isTemp) {
                    allocatorTempInputCount++;
                } else if (isInput) {
                    allocatorInputCount++;
                } else {
                    assert isTemp;
                    allocatorTempCount++;
                }

                return new LIRVariableOperand(allocatorOperands.size() - 1);
            }
        } else {
            return ILLEGAL_SLOT;
        }
    }

    /**
     * Gets an input or temp operand of this instruction.
     *
     * @param index the index of the operand requested
     * @return the {@code index}'th operand
     */
    public final CiValue operand(int index) {
        if (index >= inputAndTempOperands.length) {
            return CiValue.IllegalValue;
        }

        return inputAndTempOperands[index].value(this);
    }

    private void initInputsAndTemps(int tempInputCount, int tempCount, CiValue[] operands) {

        // Addresses in instruction
        for (int i = 0; i < operands.length; i++) {
            CiValue op = operands[i];
            if (op.isAddress()) {
                this.inputAndTempOperands[i] = addAddress((CiAddress) op);
            }
        }

        // Input-only operands
        for (int i = 0; i < operands.length - tempInputCount - tempCount; i++) {
            if (this.inputAndTempOperands[i] == null) {
                this.inputAndTempOperands[i] = addOperand(operands[i], true, false);
            }
        }

        // Operands that are both inputs and temps
        for (int i = operands.length - tempInputCount - tempCount; i < operands.length - tempCount; i++) {
            if (this.inputAndTempOperands[i] == null) {
                this.inputAndTempOperands[i] = addOperand(operands[i], true, true);
            }
        }

        // Temp-only operands
        for (int i = operands.length - tempCount; i < operands.length; i++) {
            if (this.inputAndTempOperands[i] == null) {
                this.inputAndTempOperands[i] = addOperand(operands[i], false, true);
            }
        }
    }

    private boolean verifyOperands() {
        for (LIROperand operandSlot : inputAndTempOperands) {
            assert operandSlot != null;
        }

        for (CiValue operand : this.allocatorOperands) {
            assert operand != null;
            assert operand.isVariableOrRegister() : "LIR operands can only be variables and registers initially, not " + operand.getClass().getSimpleName();
        }
        return true;
    }

    /**
     * Gets the result operand for this instruction.
     *
     * @return return the result operand
     */
    public CiValue result() {
        return result.value(this);
    }

    /**
     * Gets the instruction name.
     *
     * @return the name of the enum constant that represents the instruction opcode, exactly as declared in the enum
     *         LIROpcode declaration.
     */
    public String name() {
        return code.name();
    }

    /**
     * Abstract method to be used to emit target code for this instruction.
     *
     * @param masm the target assembler.
     */
    public abstract void emitCode(LIRAssembler masm);

    /**
     * Utility for specializing how a {@linkplain CiValue LIR operand} is formatted to a string.
     * The {@linkplain OperandFormatter#DEFAULT default formatter} returns the value of
     * {@link CiValue#toString()}.
     */
    public static class OperandFormatter {
        public static final OperandFormatter DEFAULT = new OperandFormatter();

        /**
         * Formats a given operand as a string.
         *
         * @param operand the operand to format
         * @return {@code operand} as a string
         */
        public String format(CiValue operand) {
            return operand.toString();
        }
    }

    /**
     * Gets the operation performed by this instruction in terms of its operands as a string.
     */
    public String operationString(OperandFormatter operandFmt) {
        StringBuilder buf = new StringBuilder();
        if (result != ILLEGAL_SLOT) {
            buf.append(operandFmt.format(result.value(this))).append(" = ");
        }
        if (inputAndTempOperands.length > 1) {
            buf.append("(");
        }
        boolean first = true;
        for (LIROperand operandSlot : inputAndTempOperands) {
            String operand = operandFmt.format(operandSlot.value(this));
            if (!operand.isEmpty()) {
                if (!first) {
                    buf.append(", ");
                } else {
                    first = false;
                }
                buf.append(operand);
            }
        }
        if (inputAndTempOperands.length > 1) {
            buf.append(")");
        }
        return buf.toString();
    }

    /**
     * Prints this instruction to a log stream.
     *
     * @param st the LogStream to print into.
     */
    public final void printOn(LogStream st) {
        if (id != -1) {
            st.printf("%4d ", id);
        } else {
            st.print("     ");
        }

        st.print(toString());
    }

    public boolean verify() {
        return true;
    }

    /**
     * Determines if a given opcode is in a given range of valid opcodes.
     *
     * @param opcode the opcode to be tested.
     * @param start the lower bound range limit of valid opcodes
     * @param end the upper bound range limit of valid opcodes
     */
    protected static boolean isInRange(LIROpcode opcode, LIROpcode start, LIROpcode end) {
        return start.ordinal() < opcode.ordinal() && opcode.ordinal() < end.ordinal();
    }

    public boolean hasOperands() {
        if (info != null || hasCall) {
            return true;
        }
        return allocatorOperands.size() > 0;
    }

    public boolean hasCall() {
        return hasCall;
    }

    public int operandCount(OperandMode mode) {
        if (mode == OperandMode.Output) {
            return outputCount;
        } else if (mode == OperandMode.Input) {
            return allocatorInputCount + allocatorTempInputCount;
        } else {
            assert mode == OperandMode.Temp;
            return allocatorTempInputCount + allocatorTempCount;
        }
    }

    public CiValue operandAt(OperandMode mode, int index) {
        if (mode == OperandMode.Output) {
            assert index < outputCount;
            return allocatorOperands.get(index);
        } else if (mode == OperandMode.Input) {
            assert index < allocatorInputCount + allocatorTempInputCount;
            return allocatorOperands.get(index + outputCount);
        } else {
            assert mode == OperandMode.Temp;
            assert index < allocatorTempInputCount + allocatorTempCount;
            return allocatorOperands.get(index + outputCount + allocatorInputCount);
        }
    }

    public void setOperandAt(OperandMode mode, int index, CiValue location) {
        assert index < operandCount(mode);
        assert location.kind != CiKind.Illegal;
        if (mode == OperandMode.Output) {
            assert index < outputCount;
            allocatorOperands.set(index, location);
        } else if (mode == OperandMode.Input) {
            assert index < allocatorInputCount + allocatorTempInputCount;
            allocatorOperands.set(index + outputCount, location);
        } else {
            assert mode == OperandMode.Temp;
            assert index < allocatorTempInputCount + allocatorTempCount;
            allocatorOperands.set(index + outputCount + allocatorInputCount, location);
        }
    }

    public List<ExceptionHandler> exceptionEdges() {
        if (info != null && info.exceptionHandlers != null) {
            return info.exceptionHandlers;
        }

        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return toString(OperandFormatter.DEFAULT);
    }

    protected static void appendRefMap(StringBuilder buf, OperandFormatter operandFmt, byte[] map, boolean frameRefMap) {
        for (int i = 0; i < map.length; i++) {
            int b = map[i] & 0xff;
            if (b != 0) {
                int index = (i * 8);
                while (b != 0) {
                    if ((b & 1) != 0) {
                        if (buf.length() != 0) {
                            buf.append(", ");
                        }
                        if (frameRefMap) {
                            buf.append(operandFmt.format(CiStackSlot.get(CiKind.Object, index)));
                        } else {
                            CiRegisterValue register = compilation().target.arch.registerFor(index, RegisterFlag.CPU).asValue(CiKind.Object);
                            buf.append(operandFmt.format(register));
                        }
                    }
                    b >>>= 1;
                    index++;
                }
            }
        }
    }

    protected void appendDebugInfo(StringBuilder buf, OperandFormatter operandFmt, LIRDebugInfo info) {
        if (info != null) {
            buf.append(" [bci:").append(info.state.bci);
            if (info.hasDebugInfo()) {
                CiDebugInfo debugInfo = info.debugInfo();
                StringBuilder refmap = new StringBuilder();
                if (debugInfo.hasStackRefMap()) {
                    appendRefMap(refmap, operandFmt, debugInfo.frameRefMap, true);
                }
                if (debugInfo.hasRegisterRefMap()) {
                    appendRefMap(refmap, operandFmt, debugInfo.registerRefMap, false);
                }
                if (refmap.length() != 0) {
                    buf.append(", refmap(").append(refmap.toString().trim()).append(')');
                }
            }
            buf.append(']');
        }
    }

    public String toString(OperandFormatter operandFmt) {
        StringBuilder buf = new StringBuilder(name()).append(' ').append(operationString(operandFmt));
        appendDebugInfo(buf, operandFmt, info);
        return buf.toString();
    }
}
