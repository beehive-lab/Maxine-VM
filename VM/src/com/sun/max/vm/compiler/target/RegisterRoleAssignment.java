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

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.util.*;
import com.sun.max.vm.runtime.*;

/**
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public final class RegisterRoleAssignment<IntegerRegister_Type extends Symbol, FloatingPointRegister_Type extends Symbol> {

    private final IntegerRegister_Type[] _integerRegisters;

    @FOLD
    public IntegerRegister_Type integerRegisterActingAs(VMRegister.Role role) {
        return _integerRegisters[role.ordinal()];
    }

    private final FloatingPointRegister_Type[] _floatingPointRegisters;

    @FOLD
    public FloatingPointRegister_Type floatingPointRegisterActingAs(VMRegister.Role role) {
        return _floatingPointRegisters[role.ordinal()];
    }

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
                    IntegerRegister_Type callInstructionAddress, IntegerRegister_Type framelessCallInstructionAddress) {
        _integerRegisters = Arrays.newInstance(integerRegisterType, VMRegister.Role.VALUES.length());
        _integerRegisters[VMRegister.Role.CPU_STACK_POINTER.ordinal()] = cpuStackPointer;
        _integerRegisters[VMRegister.Role.CPU_FRAME_POINTER.ordinal()] = cpuFramePointer;
        _integerRegisters[VMRegister.Role.ABI_STACK_POINTER.ordinal()] = abiStackPointer;
        _integerRegisters[VMRegister.Role.ABI_FRAME_POINTER.ordinal()] = abiFramePointer;
        _integerRegisters[VMRegister.Role.ABI_RETURN.ordinal()] = integerReturn;
        _integerRegisters[VMRegister.Role.ABI_RESULT.ordinal()] = integerResult;
        _integerRegisters[VMRegister.Role.ABI_SCRATCH.ordinal()] = integerScratch;
        _integerRegisters[VMRegister.Role.SAFEPOINT_LATCH.ordinal()] = safepointLatch;
        _integerRegisters[VMRegister.Role.LITERAL_BASE_POINTER.ordinal()] = literalBasePointer;
        _integerRegisters[VMRegister.Role.CALL_INSTRUCTION_ADDRESS.ordinal()] = callInstructionAddress;
        _integerRegisters[VMRegister.Role.FRAMELESS_CALL_INSTRUCTION_ADDRESS.ordinal()] = framelessCallInstructionAddress;

        _floatingPointRegisters = Arrays.newInstance(floatingPointRegisterType, VMRegister.Role.values().length);
        _floatingPointRegisters[VMRegister.Role.ABI_RETURN.ordinal()] = floatingPointReturn;
        _floatingPointRegisters[VMRegister.Role.ABI_RESULT.ordinal()] = floatingPointReturn;
        _floatingPointRegisters[VMRegister.Role.ABI_SCRATCH.ordinal()] = floatingPointScratch;
    }


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
                        integerScratch, safepointLatch, null, floatingPointRegisterType, floatingPointReturn, floatingPointScratch, null, null);
    }

    public RegisterRoleAssignment(RegisterRoleAssignment<IntegerRegister_Type, FloatingPointRegister_Type> original,
                                  VMRegister.Role role, IntegerRegister_Type newIntegerRegister) {
        _integerRegisters = original._integerRegisters.clone();
        _integerRegisters[role.ordinal()] = newIntegerRegister;
        _floatingPointRegisters = original._floatingPointRegisters;
    }

}
