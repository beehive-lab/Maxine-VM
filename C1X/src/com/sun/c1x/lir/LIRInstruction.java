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

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.globalstub.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.lir.LIROperand.*;
import com.sun.c1x.stub.*;
import com.sun.c1x.util.*;
import com.sun.cri.ci.*;

/**
 * The {@code LIRInstruction} class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 */
public abstract class LIRInstruction {

    private static final LIROperand ILLEGAL_SLOT = new LIROperand(CiValue.IllegalValue);

    /**
     * The opcode of this instruction.
     */
    public final LIROpcode code;

    /**
     * The result operand for this instruction.
     */
    private final LIROperand result;

    /**
     * The input and temp operands of this instruction.
     */
    protected LIROperand[] inputAndTempOperands;

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

    public final LocalStub stub;
    public GlobalStub globalStub;

    public static final OperandMode[] OPERAND_MODES = OperandMode.values();

    public enum OperandMode {
        OutputMode,
        InputMode,
        TempMode
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
     * Constructs a new Instruction.
     *
     * @param opcode the opcode of the new instruction
     * @param result the operand that holds the operation result of this instruction. This will be
     *            {@link CiValue#IllegalValue} for instructions that do not produce a result.
     * @param info the debug info that is to be preserved for the instruction. This will be {@code null} when no debug info is required for the instruction.
     * @param hasCall
     * @param stub
     * @param tempInput
     * @param info the object holding information needed to perform deoptimization
     */
    public LIRInstruction(LIROpcode opcode, CiValue result, LIRDebugInfo info, boolean hasCall, LocalStub stub, int tempInput, int temp, CiValue... inputAndTempOperands) {
        this.code = opcode;
        this.info = info;
        this.hasCall = hasCall;
        this.stub = stub;

        assert opcode != LIROpcode.Move || result != CiValue.IllegalValue;
        this.result = addOutput(result);
        if (stub != null) {
            stub.setInstruction(this);
            stub.setResultSlot(addOutput(stub.originalResult()));
        }

        C1XMetrics.LIRInstructions++;

        if (opcode == LIROpcode.Move) {
            C1XMetrics.LIRMoveInstructions++;
        }

        id = -1;
        initInputsAndTemps(tempInput, temp, inputAndTempOperands, stub);

        assert verifyOperands();
    }

    private LIROperand addOutput(CiValue output) {
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

    private LIROperand addAddress(CiAddress address) {
        assert address.base.isVariableOrRegister();

        int base = allocatorOperands.size();
        allocatorInputCount++;
        allocatorOperands.add(address.base);

        if (address.index.isLegal()) {
            allocatorInputCount++;
            allocatorOperands.add(address.index);
        }

        if (address.base.isRegister()) {
            assert address.index.isIllegal();
            return new LIROperand(address);
        }

        return new LIRAddressOperand(base, address);
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

    protected final CiValue operand(int index) {
        if (index >= inputAndTempOperands.length) {
            return CiValue.IllegalValue;
        }

        return inputAndTempOperands[index].value(this);
    }

    public final CiValue stubOperand(int index) {
        return inputAndTempOperands[index + (inputAndTempOperands.length - stub.operands.length)].value(this);
    }

    private void initInputsAndTemps(int tempInputCount, int tempCount, CiValue[] operands, LocalStub stub) {

        CiValue[] stubOperands = stub == null ? null : stub.operands;
        this.inputAndTempOperands = new LIROperand[operands.length + (stubOperands == null ? 0 : stubOperands.length)];
        // Addresses in instruction
        for (int i = 0; i < operands.length; i++) {
            CiValue op = operands[i];
            if (op.isAddress()) {
                inputAndTempOperands[i] = addAddress((CiAddress) op);
            }
        }

        // Addresses in stub
        if (stubOperands != null) {
            for (int i = 0; i < stubOperands.length; i++) {
                CiValue op = stubOperands[i];
                if (op.isAddress()) {
                    inputAndTempOperands[i + operands.length] = addAddress((CiAddress) op);
                }
            }
        }

        // Input operands in instruction
        for (int i = 0; i < operands.length - tempInputCount - tempCount; i++) {
            if (inputAndTempOperands[i] == null) {
                inputAndTempOperands[i] = addOperand(operands[i], true, false);
            }
        }

        // Input operands in stub
        if (stubOperands != null) {
            for (int i = 0; i < stubOperands.length - stub.tempCount - stub.tempInputCount; i++) {
                if (inputAndTempOperands[i + operands.length] == null) {
                    inputAndTempOperands[i + operands.length] = addOperand(stubOperands[i], true, false);
                }
            }
        }

        // Input Temp operands in instruction
        for (int i = operands.length - tempInputCount - tempCount; i < operands.length - tempCount; i++) {
            if (inputAndTempOperands[i] == null) {
                inputAndTempOperands[i] = addOperand(operands[i], true, true);
            }
        }

        // Input Temp operands in stub
        if (stubOperands != null) {
            for (int i = stubOperands.length - stub.tempCount - stub.tempInputCount; i < stubOperands.length - stub.tempCount; i++) {
                if (inputAndTempOperands[i + operands.length] == null) {
                    inputAndTempOperands[i + operands.length] = addOperand(stubOperands[i], true, true);
                }
            }
        }

        // Temp operands in instruction
        for (int i = operands.length - tempCount; i < operands.length; i++) {
            if (inputAndTempOperands[i] == null) {
                inputAndTempOperands[i] = addOperand(operands[i], false, true);
            }
        }

        // Temp operands in stub
        if (stubOperands != null) {
            for (int i = stubOperands.length - stub.tempCount; i < stubOperands.length; i++) {
                if (inputAndTempOperands[i + operands.length] == null) {
                    inputAndTempOperands[i + operands.length] = addOperand(stubOperands[i], false, true);
                }
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
        if (info != null || hasCall || stub != null) {
            return true;
        }

        return this.allocatorOperands.size() > 0;
    }

    public boolean hasCall() {
        return hasCall;
    }

    public int operandCount(OperandMode mode) {
        if (mode == OperandMode.OutputMode) {
            return outputCount;
        } else if (mode == OperandMode.InputMode) {
            return allocatorInputCount + allocatorTempInputCount;
        } else {
            assert mode == OperandMode.TempMode;
            return allocatorTempInputCount + allocatorTempCount;
        }
    }

    public CiValue operandAt(OperandMode mode, int index) {
        if (mode == OperandMode.OutputMode) {
            assert index < outputCount;
            return allocatorOperands.get(index);
        } else if (mode == OperandMode.InputMode) {
            assert index < allocatorInputCount + allocatorTempInputCount;
            return allocatorOperands.get(index + outputCount);
        } else {
            assert mode == OperandMode.TempMode;
            assert index < allocatorTempInputCount + allocatorTempCount;
            return allocatorOperands.get(index + outputCount + allocatorInputCount);
        }
    }

    public void setOperandAt(OperandMode mode, int index, CiValue location) {
        assert index < operandCount(mode);
        assert location.kind != CiKind.Illegal;
        if (mode == OperandMode.OutputMode) {
            assert index < outputCount;
            allocatorOperands.set(index, location);
        } else if (mode == OperandMode.InputMode) {
            assert index < allocatorInputCount + allocatorTempInputCount;
            allocatorOperands.set(index + outputCount, location);
        } else {
            assert mode == OperandMode.TempMode;
            assert index < allocatorTempInputCount + allocatorTempCount;
            allocatorOperands.set(index + outputCount + allocatorInputCount, location);
        }
    }

    public boolean hasInfo() {
        return info != null || (stub != null && stub.info != null);
    }

    public int infoCount() {
        int result = 0;
        if (info != null) {
            result++;
        }

        if (stub != null && stub.info != null) {
            result++;
        }

        return result;
    }

    public List<ExceptionHandler> exceptionEdges() {
        int count = infoCount();
        List<ExceptionHandler> result = null;
        for (int i = 0; i < count; i++) {
            List<ExceptionHandler> handlers = infoAt(i).exceptionHandlers;
            if (handlers != null) {
                assert result == null : "only one xhandler list allowed per LIR-operation";
                result = handlers;
            }
        }

        if (result == null) {
            result = Util.uncheckedCast(Collections.EMPTY_LIST);
        }
        return result;
    }

    public LIRDebugInfo infoAt(int k) {
        if (k == 1) {
            return stub.info;
        } else {
            assert k == 0;
            if (info == null) {
                return stub.info;
            } else {
                return info;
            }
        }
    }

    @Override
    public String toString() {
        return toString(OperandFormatter.DEFAULT);
    }

    protected static void appendRefMap(StringBuilder buf, OperandFormatter operandFmt, byte[] map, boolean frameRefMap) {
        CiRegister[] registerReferenceMapOrder = null;
        if (!frameRefMap) {
            C1XCompilation current = C1XCompilation.current();
            registerReferenceMapOrder = current.target.registerConfig.getRegisterReferenceMapOrder();
        }
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
                            CiRegisterValue register = registerReferenceMapOrder[index].asValue(CiKind.Object);
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
            buf.append(" [bci:").append(info.bci);
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
