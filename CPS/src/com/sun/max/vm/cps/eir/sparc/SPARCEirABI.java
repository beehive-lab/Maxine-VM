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
package com.sun.max.vm.cps.eir.sparc;

import static com.sun.max.vm.cps.eir.sparc.SPARCEirRegisters.*;
import static com.sun.max.vm.cps.eir.sparc.SPARCEirRegisters.GeneralPurpose.*;
import static com.sun.max.vm.cps.eir.sparc.SPARCEirRegisters.SinglePrecision.*;

import java.util.*;
import java.util.Arrays;

import com.sun.max.*;
import com.sun.max.asm.sparc.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.sparc.*;
import com.sun.max.vm.type.*;

/**
 * SPARC EIR ABI.
 *
 * TODO: for now only 64-bit is supported. You'll have to refactor this (in particular, regarding STACK_BIAS usage)
 * to adapt to 32 bits.
 *
 * @author Laurent Daynes
 */
public abstract class SPARCEirABI extends EirABI<SPARCEirRegister> {
    @Override
    public int stackSlotSize() {
        return Longs.SIZE;
    }

    @Override
    public int frameSize(int numLocalStackSlots, int extraBytes) {
        return targetABI().alignFrameSize((numLocalStackSlots * stackSlotSize()) + extraBytes
            + SPARCStackFrameLayout.SAVE_AREA_SIZE + SPARCStackFrameLayout.ARGUMENT_SLOTS_SIZE);
    }

    @Override
    public Pool<SPARCEirRegister> registerPool() {
        return pool();
    }

    @Override
    public SPARCEirRegisters.GeneralPurpose integerRegisterActingAs(VMRegister.Role role) {
        final GPR r = targetABI.registerRoleAssignment.integerRegisterActingAs(role);
        if (r == null) {
            return null;
        }
        return GeneralPurpose.from(r);
    }

    @Override
    public SPARCEirRegisters.SinglePrecision floatingPointRegisterActingAs(VMRegister.Role role) {
        final FPR r = targetABI.registerRoleAssignment.floatingPointRegisterActingAs(role);
        if (r == null) {
            return null;
        }
        // Only double precision floating-point register can be assigned a role.
        return doublePrecisionFrom(r);
    }

    /**
     * Register used as a location-independent base for a method's literals.
     */
    public SPARCEirRegisters.GeneralPurpose literalBaseRegister() {
        return integerRegisterActingAs(VMRegister.Role.LITERAL_BASE_POINTER);
    }

    private TargetABI<GPR, FPR> targetABI;

    @Override
    public TargetABI<GPR, FPR> targetABI() {
        return targetABI;
    }

    protected void initTargetABI(TargetABI<GPR, FPR> abi) {
        this.targetABI = abi;
    }

    /**
     * Local registers available for allocation. These do not need to be caller saved.
     * Note: L7 is reserved as literal base pointer.
     */
    protected static final List<SPARCEirRegisters.GeneralPurpose> integerLocalRegisters =
        Arrays.asList(L0, L1, L2, L3, L4, L5, L6);

    /**
     * Global registers available for allocation.
     * G1 and G5 are volatile and not preserved by library call. They can be used as scratch register, or they must be caller saved.
     * G2, G3 and G4 are reserved for application (here the VM and the program it runs), and are callee saved by system software.
     *
     * G1 is used as the integer scratch register.
     * G2 is used as the safepoint latch (so we don't have to save it except when going native).
     * The remaining must be consider register-saved.
     *
     */
    protected static final List<SPARCEirRegisters.GeneralPurpose> applicationGlobalRegisters =
        Arrays.asList(G3, G4, G5);

    /**
     * Global registers reserved for system software and must not be used by the application (here both the VM and the program it runs).
     * Register %g0 is included here, though it can be used anywhere.
     * These must be made unallocatable, no matter what.
     */
    protected static final List<SPARCEirRegisters.GeneralPurpose> integerSystemReservedGlobalRegisters =
        Arrays.asList(G0, G6, G7);

    /**
     * Global registers not reserved for system software and may be used by the application (either the VM or the program it runs).
     * Register %g0 is not included here, though it can be used anywhere.
     */
    public static final List<SPARCEirRegisters.GeneralPurpose> integerNonSystemReservedGlobalRegisters =
        Arrays.asList(G1, G2, G3, G4, G5);

    protected static final List<SPARCEirRegisters.GeneralPurpose> integerOutRegisters =
        Arrays.asList(O0, O1, O2, O3, O4, O5);

    protected static final List<SPARCEirRegisters.GeneralPurpose> integerInRegisters =
        Arrays.asList(I0, I1, I2, I3, I4, I5);

    // The SPARC / Solaris ABI distinguishes 3 categories of floating point registers
    // that overlaps over the entire set of floating point registers: single, double and quad precisions.
    // We currently do not use operations that requires quad-precisions registers so we ignore these.
    // The FLOAT and DOUBLE kinds map directly to single and double precision register type.
    // SPARC /Solaris ABI makes single precision values passed in odd-numbered registers (F1, F3, F5, etc...), whereas double
    // precisions are passed in even-numbered registers (D0, D2, D4, etc...). Each double precision register actually maps physically
    // to two consecutive single-precision registers (e.g., D0 == F0 + F1, D2 = F2 + F3, etc...) for the first 32 registers. Subsequent
    // registers (D32 and up) are actual double precision registers.
    private static final SPARCEirRegister[] fprs = {
        F0,  F1,  F2,  F3,  F4,  F5,  F6,  F7,  F8,  F9,  F10, F11, F12, F13, F14, F15,
        F16, F17, F18, F19, F20, F21, F22, F23, F24, F25, F26, F27, F28, F29, F30, F31
    };
    protected static final List<SPARCEirRegister> floatingPointOutRegisters = Arrays.asList(fprs);

    protected static final List<SPARCEirRegister> floatingPointInRegisters = floatingPointOutRegisters;

    private static final SPARCEirRegister[] spFprs = {F1, F3, F5, F7, F9, F11, F13, F15, F17, F19, F21, F23, F25, F27, F29, F31};
    protected static final List<SPARCEirRegister> singlePrecisionParameterRegisters = Arrays.asList(spFprs);

    private static final SPARCEirRegister[] dpFprs = {F0, F2, F4, F6, F8, F10, F12, F14, F16, F18, F20, F22, F24, F26, F28, F30};
    protected static final List<SPARCEirRegister> doublePrecisionParameterRegisters = Arrays.asList(dpFprs);

    private static final List<SPARCEirRegister> emptyRegisterSet = Arrays.asList();

    @Override
    public EirLocation[] getParameterLocations(EirStackSlot.Purpose stackSlotPurpose, Kind... kinds) {
        final EirLocation[] result = new EirLocation[kinds.length];
        final List<? extends SPARCEirRegister> integerParameterRegisters = stackSlotPurpose.equals(EirStackSlot.Purpose.PARAMETER) ? integerInRegisters : integerOutRegisters;
        // This strictly follows the Solaris / SPARC 64-bits ABI.
        // Each argument matches a position on the stack, and each stack position corresponds to a specific register.
        // So it may be the case that a register is not used. For instance, consider the following call:
        //                          SP-relative offset to reserved stack slot
        // f( char,         %o0     BIAS + RW_SAVING_AREA + 0 * wordSize
        //    float,        %f3     BIAS + RW_SAVING_AREA + 1 * wordSize
        //    short,        %o2     BIAS + RW_SAVING_AREA + 2 * wordSize
        //    double,       %f6     BIAS + RW_SAVING_AREA + 3 * wordSize
        //    int,          %o4     BIAS + RW_SAVING_AREA + 5 * wordSize
        //    int           %o5     BIAS + RW_SAVING_AREA + 6 * wordSize
        //
        // In this case, %o1 and %o3 aren't used. This means that when compiling the callee,
        // we may add the corresponding %i registers to the pool of available registers
        // (especially since these are already caller-saved by the caller).

        int stackOffset = 0;
        for (int i = 0; i < kinds.length; i++) {
            final List<? extends SPARCEirRegister> parameterRegisters;
            switch (kinds[i].asEnum) {
                case BYTE:
                case BOOLEAN:
                case SHORT:
                case CHAR:
                case INT:
                case LONG:
                case WORD:
                case REFERENCE:
                    parameterRegisters = integerParameterRegisters;
                    break;
                case FLOAT:
                    parameterRegisters = singlePrecisionParameterRegisters;
                    break;
                case DOUBLE:
                    parameterRegisters = doublePrecisionParameterRegisters;
                    break;
                default: {
                    ProgramError.unknownCase();
                    return null;
                }
            }
            if (i < parameterRegisters.size()) {
                result[i] = parameterRegisters.get(i);
            } else {
                result[i] = new EirStackSlot(stackSlotPurpose, stackOffset);
                stackOffset += stackSlotSize();
            }
        }
        return result;
    }

    private PoolSet<SPARCEirRegister> createUnallocatableRegisterPoolSet() {
        final PoolSet<SPARCEirRegister> result = PoolSet.noneOf(pool());
        for (SPARCEirRegister reserved : integerSystemReservedGlobalRegisters) {
            result.add(reserved);
        }
        result.add(stackPointer());
        result.add(framePointer());
        result.add(integerRegisterActingAs(VMRegister.Role.SAFEPOINT_LATCH));
        result.add(integerRegisterActingAs(VMRegister.Role.LITERAL_BASE_POINTER));
        result.add(GeneralPurpose.I7);   // return address register. TODO: add an optional Role for this ?
        result.add(GeneralPurpose.O7); // Used to saved the return address in the callee. May only be used as temporary scratch.
        for (Kind kind : Kind.PRIMITIVE_VALUES) {
            result.add(getScratchRegister(kind));
        }
        return result;
    }

    private final PoolSet<SPARCEirRegister> unallocatableRegisters;

    @Override
    public PoolSet<SPARCEirRegister> unallocatableRegisters() {
        return unallocatableRegisters;
    }

    private PoolSet<SPARCEirRegister> createAllocatableRegisterPoolSet() {
        final PoolSet<SPARCEirRegister> result = PoolSet.noneOf(pool());
        result.addAll();
        for (SPARCEirRegister register : unallocatableRegisters) {
            result.remove(register);
        }
        return result;
    }

    private final PoolSet<SPARCEirRegister> allocatableRegisters;

    @Override
    public PoolSet<SPARCEirRegister> allocatableRegisters() {
        return allocatableRegisters;
    }

    private PoolSet<SPARCEirRegister> createCallerSavedRegisterPoolSet() {
        final PoolSet<SPARCEirRegister> result = PoolSet.noneOf(pool());
        result.or(allocatableRegisters());
        // Local register don't need to be saved.
        for (SPARCEirRegister register : integerLocalRegisters) {
            result.remove(register);
        }
        // Same for "in" register.
        for (SPARCEirRegister register : integerInRegisters) {
            result.remove(register);
        }
        return result;
    }

    PoolSet<SPARCEirRegister> callerSavedRegisters;

    @Override
    public PoolSet<SPARCEirRegister> callerSavedRegisters() {
        return callerSavedRegisters;
    }

    /**
     * No callee saved registers.
     */
    List<SPARCEirRegister> calleeSavedRegisters = Collections.emptyList();

    @Override
    public List<SPARCEirRegister> calleeSavedRegisters() {
        return calleeSavedRegisters;
    }

    private final PoolSet<SPARCEirRegister> resultRegisters;

    @Override
    public PoolSet<SPARCEirRegister> resultRegisters() {
        return resultRegisters;
    }

    protected void makeUnallocatable(SPARCEirRegister register) {
        unallocatableRegisters.add(register);
        allocatableRegisters.remove(register);
    }

    private static TargetABI<GPR, FPR> targetABI(VMConfiguration vmConfiguration) {
        final Class<TargetABI<GPR, FPR>> type = null;
        return Utils.cast(type, vmConfiguration.targetABIsScheme().optimizedJavaABI);
    }

    @Override
    public List<SPARCEirRegister> integerParameterRegisters() {
        // This method is only used for callee saving of register by trampolines.
        // On SPARC, the caller's integer parameter are protected via a register window, so there isn't any need
        // for savings.
        return emptyRegisterSet;
    }

    @Override
    public List<SPARCEirRegister> floatingPointParameterRegisters() {
        return floatingPointOutRegisters;
    }

    protected SPARCEirABI(VMConfiguration vmConfiguration) {
        super(vmConfiguration, SPARCEirRegister.class);
        targetABI = targetABI(vmConfiguration);
        unallocatableRegisters = createUnallocatableRegisterPoolSet();
        allocatableRegisters = createAllocatableRegisterPoolSet();
        resultRegisters = PoolSet.noneOf(pool());
        resultRegisters.add((SPARCEirRegister) getResultLocation(Kind.LONG));
        resultRegisters.add((SPARCEirRegister) getResultLocation(Kind.DOUBLE));
        callerSavedRegisters = createCallerSavedRegisterPoolSet();
    }

}
