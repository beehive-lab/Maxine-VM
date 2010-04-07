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
package com.sun.c1x.alloc;

import java.util.*;

import com.sun.c1x.ci.*;
import com.sun.c1x.util.*;

/**
 * An ordered, 0-based indexable pool of instruction operands for a method being compiled.
 * The physical {@linkplain CiRegister registers} of the platform occupy the front of the
 * pool (starting at index 0) followed by {@linkplain CiVariable variable} operands.
 * The index of an operand in the pool is its {@linkplain #operandNumber(CiLocation) operand number}.
 *
 * In the original HotSpot C1 source code, this pool corresponds to the
 * "flat register file" mentioned in c1_LinearScan.cpp.
 *
 * @author Doug Simon
 */
public class OperandPool {

    private static final int INITIAL_VARIABLE_CAPACITY = 20;

    /**
     * The physical registers occupying the head of the operand pool.
     */
    private final CiRegister[] registers;

    /**
     * The variable operands allocated from this pool. The {@linkplain #operandNumber(CiLocation) number}
     * of a variable operand is greater than the number of any fixed regisyer operand.
     */
    private final ArrayList<CiVariable> variables;

    private final int lowestVariableNumber;
    private BitMap mustBeByteRegister;
    private BitMap mustStartInMemory;

    /**
     * Flags that can be set on {@linkplain CiLocation#isVariable() variables}.
     */
    public enum VariableFlag {
        /**
         * Denotes a variable that needs to be assigned a memory location
         * at the beginning, but may then be loaded in a register.
         */
        MustStartInMemory,

        /**
         * Denotes a variable that must be assigned to a byte-sized register.
         */
        MustBeByteRegister;

        public static final VariableFlag[] VALUES = values();
    }

    private static BitMap set(BitMap map, CiVariable variable) {
        if (map == null) {
            map = new BitMap();
        }
        map.set(variable.index);
        return map;
    }

    private static boolean get(BitMap map, CiVariable variable) {
        if (map == null) {
            return false;
        }
        return map.get(variable.index);
    }

    public OperandPool(CiTarget target) {
        int maxRegisterNumber = target.allocationSpec.nofRegs;
        CiRegister[] registers = target.arch.registers;
        this.lowestVariableNumber = maxRegisterNumber + 1;
        this.registers = registers;
        variables = new ArrayList<CiVariable>(INITIAL_VARIABLE_CAPACITY);
    }

    /**
     * Creates a new {@linkplain CiVariable variable} operand.
     *
     * @param kind the kind of the variable
     * @return a new variable
     */
    public CiVariable newVariable(CiKind kind) {
        return newVariable(kind, kind == CiKind.Boolean || kind == CiKind.Byte ? VariableFlag.MustBeByteRegister : null);
    }

    /**
     * Creates a new {@linkplain CiVariable variable}.
     *
     * @param kind the kind of the variable
     * @return a new variable
     */
    public CiVariable newVariable(CiKind kind, VariableFlag flag) {
        assert kind != CiKind.Void;
        int varIndex = variables.size();
        CiVariable var = CiVariable.get(kind, varIndex);
        if (flag == VariableFlag.MustBeByteRegister) {
            mustBeByteRegister = set(mustBeByteRegister, var);
        } else if (flag == VariableFlag.MustStartInMemory) {
            mustStartInMemory = set(mustStartInMemory, var);
        }
        variables.add(var);
        return var;
    }

    public int operandNumber(CiLocation operand) {
        if (operand.isRegister()) {
            int number = operand.asRegister().number;
            assert number < lowestVariableNumber;
            return number;
        }
        assert operand.isVariable();
        return lowestVariableNumber + ((CiVariable) operand).index;
    }

    public CiLocation operandFor(int operandNumber) {
        if (operandNumber < lowestVariableNumber) {
            assert operandNumber >= 0;
            return registers[operandNumber].asLocation();
        }
        return variables.get(operandNumber - lowestVariableNumber);
    }

    public boolean mustStartInMemory(CiLocation operand) {
        if (operand.isVariable()) {
            return get(mustStartInMemory, (CiVariable) operand);
        }
        return false;
    }

    public void setMustStartInMemory(CiLocation operand) {
        if (operand.isVariable()) {
            mustStartInMemory = set(mustStartInMemory, (CiVariable) operand);
        }
    }

    public boolean mustBeByteRegister(CiLocation operand) {
        if (operand.isVariable()) {
            return get(mustBeByteRegister, (CiVariable) operand);
        }
        return false;
    }

    public void setMustBeByteRegister(CiLocation operand) {
        if (operand.isVariable()) {
            mustBeByteRegister = set(mustBeByteRegister, (CiVariable) operand);
        }
    }

    public int maxOperandNumber() {
        return lowestVariableNumber + variables.size();
    }

    public int maxRegisterNumber() {
        return lowestVariableNumber - 1;
    }
}
