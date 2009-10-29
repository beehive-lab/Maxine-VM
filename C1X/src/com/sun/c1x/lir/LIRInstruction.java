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
import com.sun.c1x.ir.*;
import com.sun.c1x.stub.*;

/**
 * The <code>LIRInstruction</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 */
public abstract class LIRInstruction {

    // the opcode of this instruction
    public final LIROpcode code;

    // the result operand for this instruction
    private OperandSlot result;

    // used to emit debug information
    public final LIRDebugInfo info;

    // value id for register allocation
    private int id;

    // backlink to the HIR instruction for debugging purposes
    private Value source;

    public final boolean hasCall;

    public CodeStub stub;

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
    private List<LIRLocation> operands = new ArrayList<LIRLocation>(6);

    private static final OperandSlot ILLEGAL_SLOT = new OperandSlot(LIROperandFactory.IllegalLocation);

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
                if (direct != null && direct.isAddress()) {
                    LIRAddress address = (LIRAddress) direct;
                    LIRLocation baseOperand = inst.operands.get(base);
                    LIRLocation indexOperand = LIROperandFactory.IllegalLocation;
                    if (!address.index.isIllegal()) {
                        indexOperand = inst.operands.get(base + 1);
                        assert indexOperand.isRegister();
                    }
                    assert baseOperand.isRegister();
                    result = address.createCopy(baseOperand, indexOperand);
                } else if (base != -1) {
                    result = inst.operands.get(base);
                }

                assert result != null;

                direct = result;
                if (result.isRegister() && !result.isVirtual()) {
                    resolved = true;
                }

                if (result.isAddress() && !((LIRAddress) result).base.isVirtual()) {
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
    public LIRInstruction(LIROpcode opcode, LIROperand result, LIRDebugInfo info, boolean hasCall, CodeStub stub, int tempInput, int temp, LIROperand... inputAndTempOperands) {

        this.code = opcode;
        this.info = info;
        this.hasCall = hasCall;
        this.stub = stub;

        this.result = addOutput(result);
        if (stub != null) {
            stub.setInstruction(this);
            stub.setResultSlot(addOutput(stub.originalResult()));
        }

        id = -1;
        initInputsAndTemps(tempInput, temp, inputAndTempOperands, stub);
    }

    private OperandSlot addOutput(LIROperand output) {
        assert output != null;
        if (output != LIROperandFactory.IllegalLocation) {
            if (output instanceof LIRAddress) {
                return addAddress((LIRAddress) output);
            }

            assert output instanceof LIRLocation;
            assert operands.size() == outputCount;
            operands.add((LIRLocation) output);
            outputCount++;
            return new OperandSlot(operands.size() - 1);
        } else {
            return ILLEGAL_SLOT;
        }
    }

    private OperandSlot addAddress(LIRAddress address) {
        assert address.base.isRegister();

        int baseIndex = operands.size();
        allocatorInputCount++;
        operands.add(address.base);

        if (!address.index.isIllegal()) {
            allocatorInputCount++;
            operands.add(address.index);
        }

        if (address.base.isRegister() && !address.base.isVirtual()) {
            assert address.index.isIllegal() || !address.index.isVirtual();
            return new OperandSlot(address);
        }

        return new OperandSlot(baseIndex, address);
    }

    private OperandSlot addStackSlot(LIROperand operand) {
        assert operand.isStack();
        return new OperandSlot(operand);
    }

    private OperandSlot addConstant(LIRConstant constant) {
        return new OperandSlot(constant);
    }

    private OperandSlot addOperand(LIROperand input, boolean isInput, boolean isTemp) {
        assert input != null;
        if (input != LIROperandFactory.IllegalLocation) {
            assert !(input instanceof LIRAddress);
            if (input.isStack()) {
                return addStackSlot(input);
            } else if (input.isConstant()) {
                return addConstant((LIRConstant) input);
            } else {
                assert operands.size() == outputCount + allocatorInputCount + allocatorTempInputCount + allocatorTempCount;

                assert input instanceof LIRLocation;
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
            return LIROperandFactory.IllegalLocation;
        }

        return operandSlots[index].get(this);
    }

    public final LIROperand stubOperand(int index) {
        return operandSlots[index + (operandSlots.length - stub.operands().length)].get(this);
    }

    private void initInputsAndTemps(int tempInputCount, int tempCount, LIROperand[] operands, CodeStub stub) {

        this.operandSlots = new OperandSlot[operands.length + (stub == null || stub.operands() == null ? 0 : stub.operands().length)];

        // Addresses in instruction
        for (int i = 0; i < operands.length; i++) {
            LIROperand op = operands[i];
            if (op.isAddress()) {
                operandSlots[i] = addAddress((LIRAddress) op);
            }
        }

        // Addresses in stub
        if (stub != null && stub.operands() != null) {
            for (int i = 0; i < stub.operands().length; i++) {
                LIROperand op = stub.operands()[i];
                if (op.isAddress()) {
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
        if (stub != null && stub.operands() != null) {
            for (int i = 0; i < stub.operands().length - stub.tempCount() - stub.tempInputCount(); i++) {
                if (operandSlots[i + operands.length] == null) {
                    operandSlots[i + operands.length] = addOperand(stub.operands()[i], true, false);
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
        if (stub != null && stub.operands() != null) {
            for (int i = stub.operands().length - stub.tempCount() - stub.tempInputCount(); i < stub.operands().length - stub.tempCount(); i++) {
                if (operandSlots[i + operands.length] == null) {
                    operandSlots[i + operands.length] = addOperand(stub.operands()[i], true, true);
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
        if (stub != null && stub.operands() != null) {
            for (int i = stub.operands().length - stub.tempCount(); i < stub.operands().length; i++) {
                if (operandSlots[i + operands.length] == null) {
                    operandSlots[i + operands.length] = addOperand(stub.operands()[i], false, true);
                }
            }
        }

        for (int i = 0; i < operandSlots.length; i++) {
            assert operandSlots[i] != null;
        }

        for (int i = 0; i < this.operands.size(); i++) {
            assert this.operands.get(i) != null;
        }
    }

    /**
     * Gets the lock stack for this instruction.
     *
     * @return return the result operand
     */
    public LIROperand result() {
        return result.get(this);
    }

    public CodeStub stub() {
        return stub;
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
     * Gets the value id of this instruction.
     *
     * @return id the value id.
     */
    public int id() {
        return id;
    }

    /**
     * Sets the value id of this instruction.
     *
     * @param id the value
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Gets the HIR correspondent for this instruction.
     *
     * @return source the HIR source instruction.
     */
    public Value source() {
        return source;
    }

    /**
     * Sets the HIR correspondent for this instruction.
     *
     * @param source the HIR source instruction.
     */
    public void setSource(Value source) {
        this.source = source;
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
        if (id() != -1 || C1XOptions.PrintCFGToFile) {
            st.printf("%4d ", id());
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
        List<ExceptionHandler> result = null;

        int i;
        for (i = 0; i < infoCount(); i++) {
            if (infoAt(i).exceptionHandlers != null) {
                result = infoAt(i).exceptionHandlers;
                break;
            }
        }

        for (i = 0; i < infoCount(); i++) {
            assert infoAt(i).exceptionHandlers == null || infoAt(i).exceptionHandlers == result : "only one xhandler list allowed per LIR-operation";
        }

        if (result != null) {
            return result;
        } else {
            return new ArrayList<ExceptionHandler>();
        }
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
