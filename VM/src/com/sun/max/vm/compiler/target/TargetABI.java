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
import com.sun.max.collect.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.VMRegister.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 * @author Laurent Daynes
 * @author Ben L. Titzer
 */
public final class TargetABI<IntegerRegister_Type extends Symbol, FloatingPointRegister_Type extends Symbol> {
    private final boolean _useRegisterWindows;

    public boolean usesRegisterWindows() {
        return _useRegisterWindows;
    }

    private final boolean _callPushesReturnAddress;

    public boolean callPushesReturnAddress() {
        return _callPushesReturnAddress;
    }

    private final int _stackBias;

    public int stackBias() {
        return _stackBias;
    }

    private final int _stackFrameAlignment;

    public int stackFrameAlignment() {
        return _stackFrameAlignment;
    }

    public int alignFrameSize(int frameSize) {
        final int n = _stackFrameAlignment - 1;
        if (callPushesReturnAddress()) {
            return ((frameSize + Word.size() + n) & ~n) - Word.size();
        }
        return (frameSize + n) & ~n;
    }

    private final RegisterRoleAssignment<IntegerRegister_Type, FloatingPointRegister_Type> _registerRoleAssignment;

    public RegisterRoleAssignment<IntegerRegister_Type, FloatingPointRegister_Type> registerRoleAssignment() {
        return _registerRoleAssignment;
    }

    @FOLD
    public IntegerRegister_Type stackPointer() {
        return _registerRoleAssignment.integerRegisterActingAs(VMRegister.Role.ABI_STACK_POINTER);
    }

    @FOLD
    public IntegerRegister_Type framePointer() {
        return _registerRoleAssignment.integerRegisterActingAs(VMRegister.Role.ABI_FRAME_POINTER);
    }

    @FOLD
    public IntegerRegister_Type scratchRegister() {
        return _registerRoleAssignment.integerRegisterActingAs(VMRegister.Role.ABI_SCRATCH);
    }

    @FOLD
    public IntegerRegister_Type integerReturn() {
        return _registerRoleAssignment.integerRegisterActingAs(VMRegister.Role.ABI_RETURN);
    }

    @FOLD
    public IntegerRegister_Type literalBaseRegister() {
        return _registerRoleAssignment.integerRegisterActingAs(VMRegister.Role.LITERAL_BASE_POINTER);
    }

    private final IndexedSequence<IntegerRegister_Type> _integerIncomingParameterRegisters;

    public IndexedSequence<IntegerRegister_Type> integerIncomingParameterRegisters() {
        return _integerIncomingParameterRegisters;
    }

    private final IndexedSequence<IntegerRegister_Type> _integerOutgoingParameterRegisters;

    public IndexedSequence<IntegerRegister_Type> integerOutgoingParameterRegisters() {
        return _integerOutgoingParameterRegisters;
    }

    @FOLD
    public FloatingPointRegister_Type floatingPointReturn() {
        return _registerRoleAssignment.floatingPointRegisterActingAs(VMRegister.Role.ABI_RETURN);
    }

    private final IndexedSequence<FloatingPointRegister_Type> _floatingPointParameterRegisters;

    public IndexedSequence<FloatingPointRegister_Type> floatingPointParameterRegisters() {
        return _floatingPointParameterRegisters;
    }

    /**
     * The call entry point dedicated to the compiler that compiled methods associated with this TargetABI object.
     */
    @INSPECTED
    private final CallEntryPoint _callEntryPoint;

    @INLINE
    public CallEntryPoint callEntryPoint() {
        return _callEntryPoint;
    }

    /**
     * A target ABI specifies a number of register roles assignment used by the compiler that produces the target code, as well as
     * the entry point {@linkplain CallEntryPoint call entry point} associated with its compiler. The latter can be used
     * to compute the offset to entry point of callees of the target methods associated with the TargetABI.
     * @param callPushesReturnAddress indicates whether call instructions push a callee's return address on to the stack.
     * @param stackFrameAlignment alignment requirement for stack frame.
     * @see Role
     */
    public TargetABI(RegisterRoleAssignment<IntegerRegister_Type, FloatingPointRegister_Type> registerRoleAssignment, CallEntryPoint callEntryPoint,
                    IndexedSequence<IntegerRegister_Type> integerIncomingParameterRegisters,
                    IndexedSequence<IntegerRegister_Type> integerOutgoingParameterRegisters,
                    IndexedSequence<FloatingPointRegister_Type> floatingPointParameterRegisters, boolean useRegisterWindows, boolean callPushesReturnAddress, int stackFrameAlignment, int stackBias) {
        _registerRoleAssignment = registerRoleAssignment;
        _callEntryPoint = callEntryPoint;
        _integerIncomingParameterRegisters = integerIncomingParameterRegisters;
        _integerOutgoingParameterRegisters = integerOutgoingParameterRegisters;
        _floatingPointParameterRegisters = floatingPointParameterRegisters;
        _useRegisterWindows = useRegisterWindows;
        _callPushesReturnAddress = callPushesReturnAddress;
        _stackFrameAlignment = stackFrameAlignment;
        _stackBias = stackBias;
    }

    public TargetABI(TargetABI<IntegerRegister_Type, FloatingPointRegister_Type> original,
                    RegisterRoleAssignment<IntegerRegister_Type, FloatingPointRegister_Type> registerRoleAssignment,
                    CallEntryPoint callEntryPoint) {
        _registerRoleAssignment = registerRoleAssignment;
        _callEntryPoint = callEntryPoint;
        _integerIncomingParameterRegisters = original._integerIncomingParameterRegisters;
        _integerOutgoingParameterRegisters = original._integerOutgoingParameterRegisters;
        _floatingPointParameterRegisters = original._floatingPointParameterRegisters;
        _useRegisterWindows = original._useRegisterWindows;
        _callPushesReturnAddress = original._callPushesReturnAddress;
        _stackFrameAlignment = original._stackFrameAlignment;
        _stackBias = original._stackBias;
    }

    /**
     * Decides whether a certain parameter is stored in an integer or a floating point register.
     * @param parameterKind the kind of the parameter
     * @return true, if an integer parameter is used to store this parameter kind
     */
    public boolean putIntoIntegerRegister(Kind parameterKind) {
        return parameterKind != Kind.FLOAT && parameterKind != Kind.DOUBLE;
    }

    // FIXME: this is architecture AND compiler dependent.
    // Should be delegated to some other class since TargetABI is final.
    // It is also redundant with the EirABI which already to this parameter to platform register mapping.
    // see EirABI.getParameterLocations.
    public TargetLocation[] getParameterTargetLocations(Kind[] parameterKinds) {
        final TargetLocation[] result = new TargetLocation[parameterKinds.length];
        int integerIndex = 0;
        int floatIndex = 0;
        int stackIndex = 0;
        for (int i = 0; i < parameterKinds.length; i++) {
            IndexedSequence<? extends Symbol> sequence = null;
            int index = 0;
            if (putIntoIntegerRegister(parameterKinds[i])) {
                sequence = this.integerIncomingParameterRegisters();
                index = integerIndex;
                integerIndex++;
            } else {
                sequence = this.floatingPointParameterRegisters();
                index = floatIndex;
                floatIndex++;
            }

            if (index >= sequence.length()) {
                // Get from stack slot
                result[i] = new TargetLocation.ParameterStackSlot(stackIndex);
                stackIndex++;
            } else {
                if (sequence == this.integerIncomingParameterRegisters()) {
                    result[i] = new TargetLocation.IntegerRegister(sequence.get(i).value());
                } else {
                    result[i] = new TargetLocation.FloatingPointRegister(sequence.get(i).value());
                }
            }
        }
        System.out.println("target locations for parameter kinds (" + parameterKinds + ": " + result);
        return result;
    }
}
