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
import com.sun.c1x.util.Util;
import com.sun.c1x.debug.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.stub.*;

/**
 * The <code>LIRInstruction</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 */
public abstract class LIRInstruction {

    private static final OperandSlot ILLEGAL_SLOT = new OperandSlot(LIROperand.IllegalLocation);

    // the opcode of this instruction
    public final LIROpcode code;

    // the result operand for this instruction
    private OperandSlot result;

    // used to emit debug information
    public final LIRDebugInfo info;

    // value id for register allocation
    public int id;

    // backlink to the HIR instruction for debugging purposes
    public Value source;

    public final boolean hasCall;

    public LocalStub stub;
    public static final OperandMode[] OPERAND_MODES = OperandMode.values();

    public enum OperandMode {
        OutputMode,
        InputMode,
        TempMode
    }

    protected OperandSlot[] operandSlots;
    private int outputCount;
    private int allocatorInputCount;
    private int allocatorTempCount;
    private int allocatorTempInputCount;
    private List<LIRLocation> operands = new ArrayList<LIRLocation>(3);

    public static final class OperandSlot {
        private int base;
        private LIROperand direct;
        private boolean resolved;

        private OperandSlot(int base, LIRAddress address) {
            this.base = base;
            this.direct = address;
            assert !address.isStack();
        }

        private OperandSlot(int base) {
            this.base = base;
        }

        private OperandSlot(LIROperand direct) {
            resolved = true;
            this.direct = direct;
        }

        public LIROperand get(LIRInstruction inst) {
            if (!resolved) {
                LIROperand result = null;
                if (direct != null && LIROperand.isAddress(direct)) {
                    LIRAddress address = (LIRAddress) direct;
                    LIRLocation baseOperand = inst.operands.get(base);
                    LIRLocation indexOperand = LIROperand.IllegalLocation;
                    if (LIROperand.isLegal(address.index)) {
                        indexOperand = inst.operands.get(base + 1);
                        assert indexOperand.isVariableOrRegister();
                    }
                    assert baseOperand.isVariableOrRegister();
                    result = address.createCopy(baseOperand, indexOperand);
                } else if (base != -1) {
                    result = inst.operands.get(base);
                }

                assert result != null;

                direct = result;
                if (result.isVariableOrRegister() && !result.isVariable()) {
                    resolved = true;
                }

                if (LIROperand.isAddress(result) && !((LIRAddress) result).base.isVariable()) {
                    resolved = true;
                }

                if (resolved) {
                    direct = result;
                } else {
                    return result;
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
    public LIRInstruction(LIROpcode opcode, LIROperand result, LIRDebugInfo info, boolean hasCall, LocalStub stub, int tempInput, int temp, LIROperand... inputAndTempOperands) {
        this.code = opcode;
        this.info = info;
        this.hasCall = hasCall;
        this.stub = stub;

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

    private OperandSlot addOutput(LIROperand output) {
        assert output != null;
        if (output != LIROperand.IllegalLocation) {
            if (output instanceof LIRAddress) {
                return addAddress((LIRAddress) output);
            }

            assert operands.size() == outputCount;
            operands.add((LIRLocation) output);
            outputCount++;
            return new OperandSlot(operands.size() - 1);
        } else {
            return ILLEGAL_SLOT;
        }
    }

    private OperandSlot addAddress(LIRAddress address) {
        assert address.base.isVariableOrRegister();

        int baseIndex = operands.size();
        allocatorInputCount++;
        operands.add(address.base);

        if (LIROperand.isLegal(address.index)) {
            allocatorInputCount++;
            operands.add(address.index);
        }

        if (address.base.isVariableOrRegister() && !address.base.isVariable()) {
            assert LIROperand.isIllegal(address.index) || !address.index.isVariable();
            return new OperandSlot(address);
        }

        return new OperandSlot(baseIndex, address);
    }

    private OperandSlot addOperand(LIROperand input, boolean isInput, boolean isTemp) {
        assert input != null;
        if (input != LIROperand.IllegalLocation) {
            assert !(input instanceof LIRAddress);
            if (input.isStack()) {
                // no variables to add
                return new OperandSlot(input);
            } else if (LIROperand.isConstant(input)) {
                // no variables to add
                return new OperandSlot(input);
            } else {
                assert operands.size() == outputCount + allocatorInputCount + allocatorTempInputCount + allocatorTempCount;
                operands.add((LIRLocation) input);

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

    protected final LIROperand operand(int index) {
        if (index >= operandSlots.length) {
            return LIROperand.IllegalLocation;
        }

        return operandSlots[index].get(this);
    }

    public final LIROperand stubOperand(int index) {
        return operandSlots[index + (operandSlots.length - stub.operands.length)].get(this);
    }

    private void initInputsAndTemps(int tempInputCount, int tempCount, LIROperand[] operands, LocalStub stub) {

        LIROperand[] stubOperands = stub == null ? null : stub.operands;
        this.operandSlots = new OperandSlot[operands.length + (stubOperands == null ? 0 : stubOperands.length)];
        // Addresses in instruction
        for (int i = 0; i < operands.length; i++) {
            LIROperand op = operands[i];
            if (LIROperand.isAddress(op)) {
                operandSlots[i] = addAddress((LIRAddress) op);
            }
        }

        // Addresses in stub
        if (stubOperands != null) {
            for (int i = 0; i < stubOperands.length; i++) {
                LIROperand op = stubOperands[i];
                if (LIROperand.isAddress(op)) {
                    operandSlots[i + operands.length] = addAddress((LIRAddress) op);
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

        for (LIRLocation operand : this.operands) {
            assert operand != null;
        }
        return true;
    }

    /**
     * Gets the lock stack for this instruction.
     *
     * @return return the result operand
     */
    public LIROperand result() {
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
     * Abstract method to be print information specific to each instruction.
     *
     * @param out the LogStream to print into.
     */
    public void printInstruction(LogStream out) {

        out.printf("%s = (", result.get(this), this.code.name());
        for (OperandSlot operandSlot : operandSlots) {
            out.printf("%s ", operandSlot.get(this));
        }
        out.print(")");
    }

    /**
     * Prints information common to all LIR instruction.
     *
     * @param st the LogStream to print into.
     */
    public void printOn(LogStream st) {
        if (id != -1 || C1XOptions.PrintCFGToFile) {
            st.printf("%4d ", id);
        } else {
            st.print("     ");
        }
        st.print(name());
        st.print(" ");
        printInstruction(st);
        if (info != null) {
            st.printf(" [bci:%d]", info.bci);
        }
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

    protected static void printCondition(LogStream out, LIRCondition cond) {
        switch (cond) {
            case Equal:
                out.print("[EQ]");
                break;
            case NotEqual:
                out.print("[NE]");
                break;
            case Less:
                out.print("[LT]");
                break;
            case LessEqual:
                out.print("[LE]");
                break;
            case GreaterEqual:
                out.print("[GT]");
                break;
            case BelowEqual:
                out.print("[BE]");
                break;
            case AboveEqual:
                out.print("[AE]");
                break;
            case Always:
                out.print("[AL]");
                break;
            default:
                out.printf("[%d]", cond.ordinal());
                break;
        }
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

    public int oprCount(OperandMode mode) {
        if (mode == OperandMode.OutputMode) {
            return outputCount;
        } else if (mode == OperandMode.InputMode) {
            return allocatorInputCount + allocatorTempInputCount;
        } else {
            assert mode == OperandMode.TempMode;
            return allocatorTempInputCount + allocatorTempCount;
        }
    }

    public LIRLocation oprAt(OperandMode mode, int index) {
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

    public void setOprAt(OperandMode mode, int index, LIRLocation colorLirOpr) {
        assert index < oprCount(mode);
        if (mode == OperandMode.OutputMode) {
            assert index < outputCount;
            operands.set(index, colorLirOpr);
        } else if (mode == OperandMode.InputMode) {
            assert index < allocatorInputCount + allocatorTempInputCount;
            operands.set(index + outputCount, colorLirOpr);
        } else {
            assert mode == OperandMode.TempMode;
            assert index < allocatorTempInputCount + allocatorTempCount;
            operands.set(index + outputCount + allocatorInputCount, colorLirOpr);
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
}
