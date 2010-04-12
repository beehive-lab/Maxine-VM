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

    private static final OperandSlot ILLEGAL_SLOT = new OperandSlot(CiValue.IllegalLocation);

    /**
     * The opcode of this instruction.
     */
    public final LIROpcode code;

    /**
     * Slot for the result operand for this instruction.
     */
    private final OperandSlot result;

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

    /**
     * The slots for the input and temp operands of this instruction.
     */
    protected OperandSlot[] operandSlots;

    private int outputCount;
    private int allocatorInputCount;
    private int allocatorTempCount;
    private int allocatorTempInputCount;
    private final List<CiLocation> operands = new ArrayList<CiLocation>(3);

    /**
     * An indirection to an instruction operand that is constant in the context of register allocation.
     * That is, while the register allocator may modify the underlying operand (e.g. to replace a
     * variable with a register), an {@link OperandSlot} value can be used to address the operand
     * for the lifetime of the instruction.
     */
    public static final class OperandSlot {

        /**
         * Index in instruction's {@linkplain LIRInstruction#operands operands} corresponding to this slot.
         * This will be -1 for operands that are bound to a location prior to register allocation (e.g.
         * method parameters in registers and stack slots).
         */
        private int index;

        /**
         * The current value of the operand. After register allocation, this is guaranteed
         * to be a {@linkplain CiRegisterValue register},
         * a {@linkplain CiConstant constant} or an {@linkplain CiAddress address}.
         */
        private CiValue direct;

        /**
         * Specifies if {@link direct} is a value that will no longer change.
         */
        private boolean resolved;

        private OperandSlot(int base, CiAddress address) {
            this.index = base;
            this.direct = address;
            assert !address.isStackSlot();
        }

        private OperandSlot(int index) {
            this.index = index;
        }

        private OperandSlot(CiValue direct) {
            index = -1;
            resolved = true;
            this.direct = direct;
        }

        /**
         * Gets the current value of the operand denoted by this object. This may still be a {@linkplain CiVariable}
         * if the register allocator has not yet assigned a register or stack address to the operand.
         *
         * @param inst the instruction containing the operand denoted by this object
         */
        public CiValue get(LIRInstruction inst) {
            if (!resolved) {
                CiValue result = null;
                if (direct != null && direct.isAddress()) {
                    CiAddress address = (CiAddress) direct;
                    CiLocation baseOperand = inst.operands.get(index);
                    CiLocation indexOperand = CiValue.IllegalLocation;
                    if (address.index.isLegal()) {
                        indexOperand = inst.operands.get(index + 1);
                        assert indexOperand.isVariableOrRegister();
                    }
                    assert baseOperand.isVariableOrRegister();
                    result = new CiAddress(address.kind, baseOperand, indexOperand, address.scale, address.displacement);
                } else if (index != -1) {
                    result = inst.operands.get(index);
                }

                assert result != null;

                direct = result;
                if (result.isRegister()) {
                    resolved = true;
                } else if (result.isAddress() && !((CiAddress) result).base.isVariable()) {
                    resolved = true;
                }
            }
            return direct;
        }
    }

    /**
     * Constructs a new Instruction.
     *
     * @param opcode the opcode of the new instruction
     * @param result the operand that holds the operation result of this instruction
     * @param info the object holding information needed to perform deoptimization
     */
    public LIRInstruction(LIROpcode opcode, CiValue result, LIRDebugInfo info, boolean hasCall, LocalStub stub, int tempInput, int temp, CiValue... inputAndTempOperands) {
        this.code = opcode;
        this.info = info;
        this.hasCall = hasCall;
        this.stub = stub;

        assert opcode != LIROpcode.Move || result != CiValue.IllegalLocation;
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
    }

    private OperandSlot addOutput(CiValue output) {
        assert output != null;
        if (output != CiValue.IllegalLocation) {
            if (output instanceof CiAddress) {
                return addAddress((CiAddress) output);
            }

            assert operands.size() == outputCount;
            operands.add((CiLocation) output);
            outputCount++;
            return new OperandSlot(operands.size() - 1);
        } else {
            return ILLEGAL_SLOT;
        }
    }

    private OperandSlot addAddress(CiAddress address) {
        assert address.base.isVariableOrRegister();

        int index = operands.size();
        allocatorInputCount++;
        operands.add(address.base);

        if (address.index.isLegal()) {
            allocatorInputCount++;
            operands.add(address.index);
        }

        if (address.base.isRegister()) {
            assert address.index.isIllegal();
            return new OperandSlot(address);
        }

        return new OperandSlot(index, address);
    }

    private OperandSlot addOperand(CiValue input, boolean isInput, boolean isTemp) {
        assert input != null;
        if (input != CiValue.IllegalLocation) {
            assert !(input instanceof CiAddress);
            if (input.isStackSlot()) {
                // no variables to add
                return new OperandSlot(input);
            } else if (input.isConstant()) {
                // no variables to add
                return new OperandSlot(input);
            } else {
                assert operands.size() == outputCount + allocatorInputCount + allocatorTempInputCount + allocatorTempCount;
                operands.add((CiLocation) input);

                if (isInput && isTemp) {
                    allocatorTempInputCount++;
                } else if (isInput) {
                    allocatorInputCount++;
                } else {
                    assert isTemp;
                    allocatorTempCount++;
                }

                return new OperandSlot(operands.size() - 1);
            }
        } else {
            return ILLEGAL_SLOT;
        }
    }

    protected final CiValue operand(int index) {
        if (index >= operandSlots.length) {
            return CiValue.IllegalLocation;
        }

        return operandSlots[index].get(this);
    }

    public final CiValue stubOperand(int index) {
        return operandSlots[index + (operandSlots.length - stub.operands.length)].get(this);
    }

    private void initInputsAndTemps(int tempInputCount, int tempCount, CiValue[] operands, LocalStub stub) {

        CiValue[] stubOperands = stub == null ? null : stub.operands;
        this.operandSlots = new OperandSlot[operands.length + (stubOperands == null ? 0 : stubOperands.length)];
        // Addresses in instruction
        for (int i = 0; i < operands.length; i++) {
            CiValue op = operands[i];
            if (op.isAddress()) {
                operandSlots[i] = addAddress((CiAddress) op);
            }
        }

        // Addresses in stub
        if (stubOperands != null) {
            for (int i = 0; i < stubOperands.length; i++) {
                CiValue op = stubOperands[i];
                if (op.isAddress()) {
                    operandSlots[i + operands.length] = addAddress((CiAddress) op);
                }
            }
        }

        // Input operands in instruction
        for (int i = 0; i < operands.length - tempInputCount - tempCount; i++) {
            if (operandSlots[i] == null) {
                operandSlots[i] = addOperand(operands[i], true, false);
            }
        }

        // Input operands in stub
        if (stubOperands != null) {
            for (int i = 0; i < stubOperands.length - stub.tempCount - stub.tempInputCount; i++) {
                if (operandSlots[i + operands.length] == null) {
                    operandSlots[i + operands.length] = addOperand(stubOperands[i], true, false);
                }
            }
        }

        // Input Temp operands in instruction
        for (int i = operands.length - tempInputCount - tempCount; i < operands.length - tempCount; i++) {
            if (operandSlots[i] == null) {
                operandSlots[i] = addOperand(operands[i], true, true);
            }
        }

        // Input Temp operands in stub
        if (stubOperands != null) {
            for (int i = stubOperands.length - stub.tempCount - stub.tempInputCount; i < stubOperands.length - stub.tempCount; i++) {
                if (operandSlots[i + operands.length] == null) {
                    operandSlots[i + operands.length] = addOperand(stubOperands[i], true, true);
                }
            }
        }

        // Temp operands in instruction
        for (int i = operands.length - tempCount; i < operands.length; i++) {
            if (operandSlots[i] == null) {
                operandSlots[i] = addOperand(operands[i], false, true);
            }
        }

        // Temp operands in stub
        if (stubOperands != null) {
            for (int i = stubOperands.length - stub.tempCount; i < stubOperands.length; i++) {
                if (operandSlots[i + operands.length] == null) {
                    operandSlots[i + operands.length] = addOperand(stubOperands[i], false, true);
                }
            }
        }

        assert verifyOperands();
    }

    private boolean verifyOperands() {
        for (OperandSlot operandSlot : operandSlots) {
            assert operandSlot != null;
        }

        for (CiLocation operand : this.operands) {
            assert operand != null;
        }
        return true;
    }

    /**
     * Gets the result operand for this instruction.
     *
     * @return return the result operand
     */
    public CiValue result() {
        return result.get(this);
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
     * Gets the operation performed by this instruction in terms of its operands as a string.
     */
    public String operationString() {
        StringBuilder buf = new StringBuilder();
        if (result != ILLEGAL_SLOT) {
            buf.append(result.get(this)).append(" = ");
        }
        if (operandSlots.length > 1) {
            buf.append("(");
        }
        boolean first = true;
        for (OperandSlot operandSlot : operandSlots) {
            if (!first) {
                buf.append(' ');
            } else {
                first = false;
            }
            buf.append(operandSlot.get(this));
        }
        if (operandSlots.length > 1) {
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
        if (id != -1 || C1XOptions.PrintCFGToFile) {
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

        return this.operands.size() > 0;
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

    public CiLocation operandAt(OperandMode mode, int index) {
        if (mode == OperandMode.OutputMode) {
            assert index < outputCount;
            return operands.get(index);
        } else if (mode == OperandMode.InputMode) {
            assert index < allocatorInputCount + allocatorTempInputCount;
            return operands.get(index + outputCount);
        } else {
            assert mode == OperandMode.TempMode;
            assert index < allocatorTempInputCount + allocatorTempCount;
            return operands.get(index + outputCount + allocatorInputCount);
        }
    }

    public void setOperandAt(OperandMode mode, int index, CiLocation location) {
        assert index < operandCount(mode);
        assert location.kind != CiKind.Illegal;
        if (mode == OperandMode.OutputMode) {
            assert index < outputCount;
            operands.set(index, location);
        } else if (mode == OperandMode.InputMode) {
            assert index < allocatorInputCount + allocatorTempInputCount;
            operands.set(index + outputCount, location);
        } else {
            assert mode == OperandMode.TempMode;
            assert index < allocatorTempInputCount + allocatorTempCount;
            operands.set(index + outputCount + allocatorInputCount, location);
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
        StringBuilder buf = new StringBuilder(name()).append(' ').append(operationString());
        if (info != null) {
            buf.append(" [bci:").append(info.bci).append("]");
        }
        return buf.toString();
    }
}
