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
    protected LIROperand result;

    // used to emit debug information
    public final CodeEmitInfo info;

    // value id for register allocation
    private int id;

    // backlink to the HIR instruction for debugging purposes
    private Instruction source;

    public LIROperand[] inputOperands;

    public LIROperand[] tempOperands;

    public final boolean hasCall;

    public CodeStub stub;

    public LIRInstruction(LIROpcode opcode, LIROperand result, CodeEmitInfo info) {
        this(opcode, result, info, false);
    }

    /**
     * Constructs a new Instruction.
     *
     * @param opcode the opcode of the new instruction
     * @param result the operand that holds the operation result of this instruction
     * @param info the object holding information needed to perform deoptimization
     */
    public LIRInstruction(LIROpcode opcode, LIROperand result, CodeEmitInfo info, boolean hasCall) {
        this.result = result;
        this.code = opcode;
        this.info = info;
        this.hasCall = hasCall;
        id = -1;
    }

    public void setStub(CodeStub stub) {
        assert this.stub == null;
        this.stub = stub;
    }

    protected void setInputOperands(LIROperand... operands) {
        assert inputOperands == null;
        assert nonNullOperands(operands);
        this.inputOperands = operands;
    }

    protected void setTempOperands(LIROperand... operands) {
        assert tempOperands == null;
        assert nonNullOperands(operands);
        this.tempOperands = operands;
    }

    private boolean nonNullOperands(LIROperand... operands) {
        for (int i = 0; i < operands.length; i++) {
            assert operands[i] != null;
        }
        return true;
    }

    /**
     * Gets the lock stack for this instruction.
     *
     * @return return the result operand
     */
    public LIROperand result() {
        return result;
    }

    /**
     * Sets the result operand for this instruction.
     *
     * @param result the result operand
     */
    public void setResult(LIROperand result) {
        this.result = result;
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
    public Instruction source() {
        return source;
    }

    /**
     * Sets the HIR correspondent for this instruction.
     *
     * @param source the HIR source instruction.
     */
    public void setSource(Instruction source) {
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
    public abstract void printInstruction(LogStream out);

    /**
     * Prints information common to all LIR instruction.
     *
     * @param stream the LogStream to print into.
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
            st.printf(" [bci:%d]", info.bci());
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
}
