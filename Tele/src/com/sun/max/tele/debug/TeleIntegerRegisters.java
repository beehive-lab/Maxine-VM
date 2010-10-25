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
package com.sun.max.tele.debug;

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.VMConfiguration.*;

import com.sun.max.asm.amd64.*;
import com.sun.max.asm.sparc.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.VMRegister.Role;

/**
 * Encapsulates the values of the integer (or general purpose) registers for a tele native thread.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public final class TeleIntegerRegisters extends TeleRegisters {

    private final Symbol indirectCallRegister;

    public TeleIntegerRegisters(TeleVM teleVM, TeleRegisterSet teleRegisterSet) {
        super(teleVM, teleRegisterSet, createSymbolizer());
        switch (platform().instructionSet()) {
            case AMD64: {
                indirectCallRegister = AMD64GeneralRegister64.RAX;
                break;
            }
            case SPARC: {
                indirectCallRegister = null;
                break;
            }
            default: {
                throw FatalError.unimplemented();
            }
        }
    }

    /**
     * Gets the symbols representing all the integer registers of the instruction set denoted by a given VM
     * configuration.
     */
    public static Symbolizer<? extends Symbol> createSymbolizer() {
        switch (platform().instructionSet()) {
            case AMD64:
                return AMD64GeneralRegister64.ENUMERATOR;
            case SPARC:
                return GPR.SYMBOLIZER;
            default:
                FatalError.unimplemented();
                return null;
        }
    }

    /**
     * Returns the value of the register that is used to make indirect calls.
     *
     * @return null if there is no fixed register used to for indirect calls on the target platform
     */
    Address getCallRegisterValue() {
        if (indirectCallRegister == null) {
            return null;
        }
        return getValue(indirectCallRegister);
    }

    /**
     * Returns the value of the register that is used as the stack pointer.
     *
     * @return the current stack pointer
     */
    Pointer stackPointer() {
        return get(Role.CPU_STACK_POINTER, null);
    }

    /**
     * Returns the value of the register that is used as the frame pointer.
     *
     * @return the current frame pointer
     */
    Pointer framePointer() {
        return get(Role.CPU_FRAME_POINTER, null);
    }

    /**
     * Gets the value of the register denoted by a given role in a given target ABI.
     *
     * @param role the role denoting the register of interest
     * @param targetABI the ABI used to convert {@code role} to a platform dependent register. If this value is null,
     *            then the {@linkplain TargetABIsScheme#nativeABI() native ABI} is used.
     * @return the value of the register denoted by {@code role} and {@code targetABI}
     */
    Pointer get(VMRegister.Role role, TargetABI targetABI) {
        final TargetABI abi = targetABI == null ? vmConfig().targetABIsScheme().nativeABI : targetABI;
        final Symbol register = abi.registerRoleAssignment.integerRegisterActingAs(role);
        return getValue(register).asPointer();
    }
}
