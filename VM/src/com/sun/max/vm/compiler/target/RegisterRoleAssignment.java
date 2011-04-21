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
package com.sun.max.vm.compiler.target;

import static com.sun.max.vm.runtime.VMRegister.Role.*;

import java.lang.reflect.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
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
        final int roleCount = VMRegister.Role.VALUES.size();
        integerRegisters = Utils.cast(Array.newInstance(integerRegisterType, roleCount));
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

        floatingPointRegisters = Utils.cast(Array.newInstance(floatingPointRegisterType, roleCount));
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
