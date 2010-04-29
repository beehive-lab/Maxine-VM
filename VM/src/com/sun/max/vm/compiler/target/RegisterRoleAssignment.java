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
package com.sun.max.vm.compiler.target;

import static com.sun.max.vm.runtime.VMRegister.Role.*;

import com.sun.max.annotate.*;

import com.sun.max.lang.*;
import com.sun.max.util.*;
import com.sun.max.vm.runtime.*;

/**
 * Assignment of compilation roles to platform specific registers.
 *
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public final class RegisterRoleAssignment<IntegerRegister_Type extends Symbol, FloatingPointRegister_Type extends Symbol> {

    private final IntegerRegister_Type[] integerRegisters;

    /**
     * Gets the integer register playing a given role.
     *
     * @param role a compilation role for a register
     * @return the register designated to {@code role} by this object
     */
    @FOLD
    public IntegerRegister_Type integerRegisterActingAs(VMRegister.Role role) {
        return integerRegisters[role.ordinal()];
    }

    private final FloatingPointRegister_Type[] floatingPointRegisters;

    @FOLD
    public FloatingPointRegister_Type floatingPointRegisterActingAs(VMRegister.Role role) {
        return floatingPointRegisters[role.ordinal()];
    }

    /**
     * Creates a set of role assignments.
     *
     * @param integerRegisterType
     * @param cpuStackPointer
     * @param cpuFramePointer
     * @param abiStackPointer
     * @param abiFramePointer
     * @param integerReturn
     * @param integerResult
     * @param integerScratch
     * @param safepointLatch
     * @param literalBasePointer
     * @param floatingPointRegisterType
     * @param floatingPointReturn
     * @param floatingPointScratch
     * @param linkAddress
     * @param framelessCallInstructionAddress
     */
    @HOSTED_ONLY
    public RegisterRoleAssignment(Class<IntegerRegister_Type> integerRegisterType,
                    IntegerRegister_Type cpuStackPointer,
                    IntegerRegister_Type cpuFramePointer,
                    IntegerRegister_Type abiStackPointer,
                    IntegerRegister_Type abiFramePointer,
                    IntegerRegister_Type integerReturn,
                    IntegerRegister_Type integerResult,
                    IntegerRegister_Type integerScratch,
                    IntegerRegister_Type safepointLatch,
                    IntegerRegister_Type literalBasePointer,
                    Class<FloatingPointRegister_Type> floatingPointRegisterType,
                    FloatingPointRegister_Type floatingPointReturn, FloatingPointRegister_Type floatingPointScratch,
                    IntegerRegister_Type linkAddress) {
        final int roleCount = VMRegister.Role.VALUES.length();
        integerRegisters = Arrays.newInstance(integerRegisterType, roleCount);
        integerRegisters[CPU_STACK_POINTER.ordinal()] = cpuStackPointer;
        integerRegisters[CPU_FRAME_POINTER.ordinal()] = cpuFramePointer;
        integerRegisters[ABI_STACK_POINTER.ordinal()] = abiStackPointer;
        integerRegisters[ABI_FRAME_POINTER.ordinal()] = abiFramePointer;
        integerRegisters[ABI_RETURN.ordinal()] = integerReturn;
        integerRegisters[ABI_RESULT.ordinal()] = integerResult;
        integerRegisters[ABI_SCRATCH.ordinal()] = integerScratch;
        integerRegisters[SAFEPOINT_LATCH.ordinal()] = safepointLatch;
        integerRegisters[LITERAL_BASE_POINTER.ordinal()] = literalBasePointer;
        integerRegisters[LINK_ADDRESS.ordinal()] = linkAddress;

        floatingPointRegisters = Arrays.newInstance(floatingPointRegisterType, roleCount);
        floatingPointRegisters[ABI_RETURN.ordinal()] = floatingPointReturn;
        floatingPointRegisters[ABI_RESULT.ordinal()] = floatingPointReturn;
        floatingPointRegisters[ABI_SCRATCH.ordinal()] = floatingPointScratch;
    }

    @HOSTED_ONLY
    public RegisterRoleAssignment(Class<IntegerRegister_Type> integerRegisterType,
                                  IntegerRegister_Type cpuStackPointer,
                                  IntegerRegister_Type cpuFramePointer,
                                  IntegerRegister_Type abiStackPointer,
                                  IntegerRegister_Type abiFramePointer,
                                  IntegerRegister_Type integerReturn,
                                  IntegerRegister_Type integerScratch,
                                  IntegerRegister_Type safepointLatch,
                                  Class<FloatingPointRegister_Type> floatingPointRegisterType,
                                  FloatingPointRegister_Type floatingPointReturn,
                                  FloatingPointRegister_Type floatingPointScratch) {
        this(integerRegisterType, cpuStackPointer, cpuFramePointer, abiStackPointer, abiFramePointer, integerReturn, integerReturn,
                        integerScratch, safepointLatch, null, floatingPointRegisterType, floatingPointReturn, floatingPointScratch, null);
    }

    /**
     * Derives a new set of register role assignments by modifying another set of role assignments.
     *
     * @param original the role assignments from which the new one will be derived
     * @param role the role to be modified in the derived role assignments
     * @param newIntegerRegister the register to be associated with {@code role} in the derived role assignments
     */
    @HOSTED_ONLY
    public RegisterRoleAssignment(RegisterRoleAssignment<IntegerRegister_Type, FloatingPointRegister_Type> original,
                                  VMRegister.Role role, IntegerRegister_Type newIntegerRegister) {
        integerRegisters = original.integerRegisters.clone();
        integerRegisters[role.ordinal()] = newIntegerRegister;
        floatingPointRegisters = original.floatingPointRegisters;
    }

}
