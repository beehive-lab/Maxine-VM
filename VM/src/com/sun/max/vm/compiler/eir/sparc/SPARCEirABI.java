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
/*VCSID=67e1c7ed-3e71-4131-a6a2-24968a7ceafc*/
package com.sun.max.vm.compiler.eir.sparc;

import static com.sun.max.vm.compiler.eir.sparc.SPARCEirRegister.GeneralPurpose.*;
import static com.sun.max.vm.compiler.eir.sparc.SPARCEirRegister.DoublePrecision.*;

import com.sun.max.asm.sparc.*;
import com.sun.max.asm.sparc.complete.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.sparc.*;
import com.sun.max.vm.type.*;

/**
 * SPARC EIR ABI.
 *
 * TODO: for now only 64-bit is supported. You'll have to refactor this (in particular, regarding STACK_BIAS usage)
 * to adapt to 32 bits.
 *
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public abstract class SPARCEirABI extends EirABI<SPARCEirRegister> {
    @Override
    public int stackSlotSize() {
        return Longs.SIZE;
    }

    @Override
    public int frameSize(int numLocalStackSlots) {
        return targetABI().alignFrameSize(numLocalStackSlots * stackSlotSize() + SPARCStackFrameLayout.SAVED_AREA + SPARCStackFrameLayout.ARGUMENT_SLOTS);
    }

    @Override
    public Pool<SPARCEirRegister> registerPool() {
        return SPARCEirRegister.pool();
    }

    @Override
    public SPARCEirRegister.GeneralPurpose integerRegisterActingAs(VMRegister.Role role) {
        final GPR r = _targetABI.registerRoleAssignment().integerRegisterActingAs(role);
        if (r == null) {
            return null;
        }
        return SPARCEirRegister.GeneralPurpose.from(r);
    }

    @Override
    public SPARCEirRegister.FloatingPoint floatingPointRegisterActingAs(VMRegister.Role role) {
        final FPR r = _targetABI.registerRoleAssignment().floatingPointRegisterActingAs(role);
        if (r == null) {
            return null;
        }
        // Only double precision floating-point register can be assigned a role.
        return SPARCEirRegister.FloatingPoint.doublePrecisionFrom(r);
    }

    /**
     * Register used as a location-independent base for a method's literals.
     */
    public SPARCEirRegister.GeneralPurpose literalBaseRegister() {
        return integerRegisterActingAs(VMRegister.Role.LITERAL_BASE_POINTER);
    }

    private TargetABI<GPR, FPR> _targetABI;

    @Override
    public TargetABI<GPR, FPR> targetABI() {
        return _targetABI;
    }

    protected void initTargetABI(TargetABI<GPR, FPR> targetABI) {
        _targetABI = targetABI;
    }

    /**
     * Local registers available for allocation. These do not need to be caller saved.
     * Note: L7 is reserved as literal base pointer.
     */
    protected static final IndexedSequence<SPARCEirRegister> _integerLocalRegisters = new ArraySequence<SPARCEirRegister>(L0, L1, L2, L3, L4, L5, L6);

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
    protected static final IndexedSequence<SPARCEirRegister> _applicationGlobalRegisters = new ArraySequence<SPARCEirRegister>(G3, G4, G5);

    /**
     * Global registers reserved for system software and must not be used by the application (here both the VM and the program it runs).
     * These must be made unallocatable, no matter what.
     */
    protected static final IndexedSequence<SPARCEirRegister> _integerSystemReservedGlobalRegisters = new ArraySequence<SPARCEirRegister>(G0, G6, G7);


    protected static final IndexedSequence<SPARCEirRegister> _integerOutRegisters = new ArraySequence<SPARCEirRegister>(O0, O1, O2, O3, O4, O5);
    protected static final IndexedSequence<SPARCEirRegister> _integerInRegisters = new ArraySequence<SPARCEirRegister>(I0, I1, I2, I3, I4, I5);

    // FIXME: this is different from the SPARC / Solaris ABI. To be able to model it, we'd have to split the notion of floating point register into single and double precision,
    // or into FLOAT and DOUBLE kinds (float would use F1, F3, F5 etc..., double would use D0 (i.e., F0), D2, D4, etc... The same change would be needed for TargetABI.
    protected static final IndexedSequence<SPARCEirRegister> _floatingPointOutRegisters = new ArraySequence<SPARCEirRegister>(F0, F2, F4, F6, F8, F10, F12, F14, F16, F18, F20, F22, F24, F26, F28);
    protected static final IndexedSequence<SPARCEirRegister> _floatingPointInRegisters = new ArraySequence<SPARCEirRegister>(F0, F2, F4, F6, F8, F10, F12, F14, F16, F18, F20, F22, F24, F26, F28);
    private static final IndexedSequence<SPARCEirRegister> _emptyRegisterSet = new ArraySequence<SPARCEirRegister>();

    @Override
    public EirLocation[] getParameterLocations(EirStackSlot.Purpose stackSlotPurpose, Kind... kinds) {
        final EirLocation[] result = new EirLocation[kinds.length];
        int iInteger = 0;
        int iFloatingPoint = 0;
        final IndexedSequence<SPARCEirRegister> integerParameterRegisters = stackSlotPurpose.equals(EirStackSlot.Purpose.PARAMETER) ? _integerInRegisters : _integerOutRegisters;


        for (int i = 0; i < kinds.length; i++) {
            switch (kinds[i].asEnum()) {
                case BYTE:
                case BOOLEAN:
                case SHORT:
                case CHAR:
                case INT:
                case LONG:
                case WORD:
                case REFERENCE: {
                    if (iInteger < integerParameterRegisters.length()) {
                        result[i] = integerParameterRegisters.get(iInteger);
                        iInteger++;
                    }
                    break;
                }
                case FLOAT:
                case DOUBLE: {
                    if (iFloatingPoint < _floatingPointOutRegisters.length()) {
                        result[i] = _floatingPointOutRegisters.get(iFloatingPoint);
                        iFloatingPoint++;
                    }
                    break;
                }
                default: {
                    ProgramError.unknownCase();
                    return null;
                }
            }
        }
        int stackOffset = 0;
        for (int i =  kinds.length - 1; i >= 0;  i--) {
            if (result[i] == null) {
                result[i] = new EirStackSlot(stackSlotPurpose, stackOffset);
                stackOffset += stackSlotSize();
            }
        }
        return result;
    }

    private PoolSet<SPARCEirRegister> createUnallocatableRegisterPoolSet() {
        final PoolSet<SPARCEirRegister> result = PoolSet.noneOf(SPARCEirRegister.pool());
        for (SPARCEirRegister reserved : _integerSystemReservedGlobalRegisters) {
            result.add(reserved);
        }
        result.add(stackPointer());
        result.add(framePointer());
        result.add(integerRegisterActingAs(VMRegister.Role.SAFEPOINT_LATCH));
        result.add(integerRegisterActingAs(VMRegister.Role.LITERAL_BASE_POINTER));
        result.add(SPARCEirRegister.GeneralPurpose.I7);   // return address register. TODO: add an optional Role for this ?
        result.add(SPARCEirRegister.GeneralPurpose.O7); // Used to saved the return address in the callee. May only be used as temporary scratch.
        for (Kind kind : Kind.PRIMITIVE_VALUES) {
            result.add(getScratchRegister(kind));
        }
        return result;
    }

    private final PoolSet<SPARCEirRegister> _unallocatableRegisters;

    @Override
    public PoolSet<SPARCEirRegister> unallocatableRegisters() {
        return _unallocatableRegisters;
    }

    private PoolSet<SPARCEirRegister> createAllocatableRegisterPoolSet() {
        final PoolSet<SPARCEirRegister> result = PoolSet.noneOf(SPARCEirRegister.pool());
        result.addAll();
        for (SPARCEirRegister register : _unallocatableRegisters) {
            result.remove(register);
        }
        return result;
    }

    private final PoolSet<SPARCEirRegister> _allocatableRegisters;

    @Override
    public PoolSet<SPARCEirRegister> allocatableRegisters() {
        return _allocatableRegisters;
    }


    private PoolSet<SPARCEirRegister> createCallerSavedRegisterPoolSet() {
        final PoolSet<SPARCEirRegister> result = PoolSet.noneOf(SPARCEirRegister.pool());
        result.or(allocatableRegisters());
        // Local register don't need to be saved.
        for (SPARCEirRegister register : _integerLocalRegisters) {
            result.remove(register);
        }
        // Same for "in" register.
        for (SPARCEirRegister register : _integerInRegisters) {
            result.remove(register);
        }
        return result;
    }
    private final PoolSet<SPARCEirRegister> _callerSavedRegisters;

    @Override
    public PoolSet<SPARCEirRegister> callerSavedRegisters() {
        return _callerSavedRegisters;
    }

    /**
     * No callee saved registers.
     */
    private final PoolSet<SPARCEirRegister> _calleeSavedRegisters = PoolSet.noneOf(SPARCEirRegister.GeneralPurpose.pool());

    @Override
    public PoolSet<SPARCEirRegister> calleeSavedRegisters() {
        return _calleeSavedRegisters;
    }

    private final PoolSet<SPARCEirRegister> _resultRegisters;

    @Override
    public PoolSet<SPARCEirRegister> resultRegisters() {
        return _resultRegisters;
    }

    private static GPR[] getTargetIntegerParameterRegisters() {
        final GPR[] result = new GPR[_integerOutRegisters.length()];
        for (int i = 0; i < _integerOutRegisters.length(); i++) {
            final SPARCEirRegister.GeneralPurpose r = (SPARCEirRegister.GeneralPurpose) _integerOutRegisters.get(i);
            result[i] = r.as();
        }
        return result;
    }

    private static FPR[] getTargetFloatingPointParameterRegisters() {
        final FPR[] result = new FPR[_floatingPointOutRegisters.length()];
        for (int i = 0; i < _floatingPointOutRegisters.length(); i++) {
            final SPARCEirRegister.FloatingPoint r = (SPARCEirRegister.FloatingPoint) _floatingPointOutRegisters.get(i);
            result[i] = r.as();
        }
        return result;
    }

    protected void makeUnallocatable(SPARCEirRegister register) {
        _unallocatableRegisters.add(register);
        _allocatableRegisters.remove(register);
    }

    private static TargetABI<GPR, FPR> targetABI(VMConfiguration vmConfiguration) {
        final Class<TargetABI<GPR, FPR>> type = null;
        return StaticLoophole.cast(type, vmConfiguration.targetABIsScheme().optimizedJavaABI());
    }

    @Override
    public Sequence<SPARCEirRegister> integerParameterRegisters() {
        // This method is only used for callee saving of register by trampolines.
        // On SPARC, the caller's integer parameter are protected via a register window, so there isn't any need
        // for savings.
        return _emptyRegisterSet;
    }

    @Override
    public Sequence<SPARCEirRegister> floatingPointParameterRegisters() {
        return _floatingPointOutRegisters;
    }

    public SPARCAssembler createAssembler() {
        return SPARCAssembler.createAssembler(vmConfiguration().platform().processorKind().dataModel().wordWidth());
    }

    protected SPARCEirABI(VMConfiguration vmConfiguration) {
        super(vmConfiguration, SPARCEirRegister.class);
        _targetABI = targetABI(vmConfiguration);
        _unallocatableRegisters = createUnallocatableRegisterPoolSet();
        _allocatableRegisters = createAllocatableRegisterPoolSet();
        _resultRegisters = PoolSet.noneOf(SPARCEirRegister.pool());
        _resultRegisters.add((SPARCEirRegister) getResultLocation(Kind.LONG));
        _resultRegisters.add((SPARCEirRegister) getResultLocation(Kind.DOUBLE));
        _callerSavedRegisters = createCallerSavedRegisterPoolSet();
    }

}
