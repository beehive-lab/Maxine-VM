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
package com.sun.max.vm.cps.eir;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.eir.EirStackSlot.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * "Application Binary Interface" (ABI) - platform specific conventions concerning:
 * - parameter passing
 * - return location
 * - callee/caller saved registers
 * - ...
 *
 * @author Bernd Mathiske
 */
public abstract class EirABI<EirRegister_Type extends EirRegister> {

    private final VMConfiguration vmConfiguration;

    public VMConfiguration vmConfiguration() {
        return vmConfiguration;
    }

    private final Class<EirRegister_Type> registerType;

    protected EirABI(VMConfiguration vmConfiguration, Class<EirRegister_Type> registerType) {
        this.vmConfiguration = vmConfiguration;
        this.registerType = registerType;
    }

    public abstract int stackSlotSize();

    public int stackSlotIndex(EirStackSlot stackSlot) {
        return stackSlot.offset / stackSlotSize();
    }

    public int frameSize(int numLocalStackSlots, int extraBytes) {
        return targetABI().alignFrameSize((numLocalStackSlots * stackSlotSize()) + extraBytes);
    }

    /**
     * Location where callers retrieve a returned value.
     */
    public EirLocation getResultLocation(Kind kind) {
        return getResultLocation(kind, VMRegister.Role.ABI_RESULT);
    }

    /**
     * Location callees use to return a value.
     */
    public EirLocation getReturnLocation(Kind kind) {
        return getResultLocation(kind, VMRegister.Role.ABI_RETURN);
    }

    private EirLocation getResultLocation(Kind kind, VMRegister.Role role) {
        if (kind != null) {
            switch (kind.asEnum) {
                case VOID:
                    return null;
                case BYTE:
                case BOOLEAN:
                case SHORT:
                case CHAR:
                case INT:
                case LONG:
                case WORD:
                case REFERENCE:
                    return integerRegisterActingAs(role);
                case FLOAT:
                case DOUBLE:
                    return floatingPointRegisterActingAs(role);
                default:
                    ProgramError.unknownCase();
                    return null;
            }
        }
        return null;
    }

    /**
     * Returns an array filled with the appropriate locations for an ordered list of parameters types.
     *
     * @param stackSlotPurpose specifies if the locations returned are for the outgoing parameters at a call site (stackSlotPurpose = LOCAL)
     *                 or are for incoming parameters upon entry to a method (stackSlotPurpose = PARAMETER)
     */
    public abstract EirLocation[] getParameterLocations(EirStackSlot.Purpose stackSlotPurpose, Kind... kinds);

    /**
     * Returns an array filled with the appropriate locations for an ordered list of parameters types.
     */
    public EirLocation[] getParameterLocations(ClassMethodActor classMethodActor, Purpose parameter, Kind[] parameterKinds) {
        return getParameterLocations(parameter, parameterKinds);
    }

    protected EirLocation[] createStackParameterLocations(Purpose stackSlotPurpose, Kind... kinds) {
        final EirLocation[] result = new EirLocation[kinds.length];
        int stackOffset = 0;
        for (int i = 0; i < kinds.length; i++) {
            if (result[i] == null) {
                result[i] = new EirStackSlot(stackSlotPurpose, stackOffset);
                stackOffset += stackSlotSize();
            }
        }
        return result;
    }

    public EirRegister_Type getScratchRegister(Kind kind) {
        switch (kind.asEnum) {
            case BYTE:
            case BOOLEAN:
            case SHORT:
            case CHAR:
            case INT:
            case LONG:
            case WORD:
            case REFERENCE:
                return integerRegisterActingAs(VMRegister.Role.ABI_SCRATCH);
            case FLOAT:
            case DOUBLE:
                return floatingPointRegisterActingAs(VMRegister.Role.ABI_SCRATCH);
            default:
                ProgramError.unknownCase();
                return null;
        }
    }

    public abstract EirRegister_Type integerRegisterActingAs(VMRegister.Role role);

    public abstract EirRegister_Type floatingPointRegisterActingAs(VMRegister.Role role);

    /**
     * Gets the register holding the address that a {@linkplain EirStackSlot#offset() stack variable's offset}
     * is relative to. The logical offset must be adjusted for a stack variable holding a
     * {@linkplain EirStackSlot#isParameter() parameter} to account for the return address pushed to the
     * stack as well as the amount by which the stack pointer may have been adjusted to allocate a
     * frame on the stack for non-parameter local variables.
     */
    public EirRegister_Type stackPointer() {
        return integerRegisterActingAs(VMRegister.Role.ABI_STACK_POINTER);
    }

    /**
     * The frame pointer is used as a base for incoming arguments,
     * local variables and register spills.
     */
    public EirRegister_Type framePointer() {
        return integerRegisterActingAs(VMRegister.Role.ABI_FRAME_POINTER);
    }

    public final EirRegister_Type safepointLatchRegister() {
        return integerRegisterActingAs(VMRegister.Role.SAFEPOINT_LATCH);
    }

    public abstract Pool<EirRegister_Type> registerPool();

    /**
     * Gets all the registers that can be used by the register allocator for general purpose values when compiling a
     * method. The returned set excludes (at least) the {@linkplain #stackPointer() stack pointer},
     * {@linkplain #framePointer() frame pointer}, {@linkplain #safepointLatchRegister() safepoint latch register}
     * and {@linkplain #getScratchRegister(Kind) scratch register(s)}.
     */
    public abstract PoolSet<EirRegister_Type> allocatableRegisters();

    /**
     * Gets all the registers that cannot be used by the register allocator for general purpose values when compiling a
     * method. The returned set includes (at least) the {@linkplain #stackPointer() stack pointer},
     * {@linkplain #framePointer() frame pointer}, {@linkplain #safepointLatchRegister() safepoint latch register}
     * and {@linkplain #getScratchRegister(Kind) scratch register(s)}.
     */
    public abstract PoolSet<EirRegister_Type> unallocatableRegisters();

    /**
     * @return all registers that may carry a return value
     */
    public abstract PoolSet<EirRegister_Type> resultRegisters();

    /**
     * @return the integer parameter registers in parameter order
     */
    public abstract Sequence<EirRegister_Type> integerParameterRegisters();

    /**
     * @return the floating point parameter registers in parameter order
     */
    public abstract Sequence<EirRegister_Type> floatingPointParameterRegisters();

    /**
     * @return all registers that must be saved before a call
     */
    public abstract PoolSet<EirRegister_Type> callerSavedRegisters();

    private EirRegister_Type[] callerSavedRegisterArray;

    public EirRegister_Type[] callerSavedRegisterArray() {
        if (callerSavedRegisterArray == null) {
            callerSavedRegisterArray = Arrays.from(registerType, callerSavedRegisters());
        }
        return callerSavedRegisterArray;
    }

    /**
     * Gets the registers that must be restored before returning from a call. This is an ordered
     * sequence that determines the order of the stack frame slots used to save the registers.
     */
    public abstract Sequence<EirRegister_Type> calleeSavedRegisters();

    public abstract TargetABI targetABI();

    /**
     * Computes the space occupied on the stack by arguments that aren't passed by registers.
     * @param parameterLocations description of parameters' location.
     * @return the size, in bytes, of the stack arguments.
     */
    public int overflowArgumentsSize(EirLocation[] parameterLocations) {
        int overflowSize = 0;
        for (int i = parameterLocations.length - 1; i >= 0; i--) {
            if (parameterLocations[i] instanceof EirStackSlot) {
                overflowSize += stackSlotSize();
            }
        }
        return overflowSize;
    }
}
