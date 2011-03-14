/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.target;

import static com.sun.max.platform.Platform.*;

import java.util.*;

import com.sun.cri.ci.*;
import com.sun.cri.ci.CiCallingConvention.Type;
import com.sun.cri.ci.CiRegister.RegisterFlag;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.VMRegister.Role;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 * @author Laurent Daynes
 * @author Ben L. Titzer
 */
public final class TargetABI<IntegerRegister_Type extends Symbol, FloatingPointRegister_Type extends Symbol> {

    public final boolean useRegisterWindows;
    public final boolean callPushesReturnAddress;
    public final int stackBias;
    public final int stackFrameAlignment;
    public final RegisterRoleAssignment<IntegerRegister_Type, FloatingPointRegister_Type> registerRoleAssignment;
    public final List<IntegerRegister_Type> integerIncomingParameterRegisters;
    public final List<IntegerRegister_Type> integerOutgoingParameterRegisters;
    public final List<FloatingPointRegister_Type> floatingPointParameterRegisters;

    /**
     * The call entry point of methods {@linkplain TargetMethod#abi() compiled with} with this ABI.
     */
    public final CallEntryPoint callEntryPoint;

    /**
     * Returns a frame size aligned according to the platform's stack frame alignment requirement. For example, on
     * Darwin x86, the stack pointer (i.e. %rsp or %sp) must be a 16-byte aligned value. The returned value will account an extra
     * word in the frame for platforms where the caller's return address is pushed to the stack by a call instruction.
     *
     * @param frameSize the size of a stack frame which is not necessarily aligned. This value does not include the
     *            extra word (if any) occupied by the caller's return address pushed by a call instruction
     */
    public int alignFrameSize(int frameSize) {
        final int n = stackFrameAlignment - 1;
        if (callPushesReturnAddress) {
            return ((frameSize + Word.size() + n) & ~n) - Word.size();
        }
        return (frameSize + n) & ~n;
    }


    @FOLD
    public IntegerRegister_Type stackPointer() {
        return registerRoleAssignment.integerRegisterActingAs(VMRegister.Role.ABI_STACK_POINTER);
    }

    @FOLD
    public IntegerRegister_Type framePointer() {
        return registerRoleAssignment.integerRegisterActingAs(VMRegister.Role.ABI_FRAME_POINTER);
    }

    @FOLD
    public IntegerRegister_Type scratchRegister() {
        return registerRoleAssignment.integerRegisterActingAs(VMRegister.Role.ABI_SCRATCH);
    }

    @FOLD
    public IntegerRegister_Type integerReturn() {
        return registerRoleAssignment.integerRegisterActingAs(VMRegister.Role.ABI_RETURN);
    }

    @FOLD
    public IntegerRegister_Type literalBaseRegister() {
        return registerRoleAssignment.integerRegisterActingAs(VMRegister.Role.LITERAL_BASE_POINTER);
    }

    @FOLD
    public FloatingPointRegister_Type floatingPointReturn() {
        return registerRoleAssignment.floatingPointRegisterActingAs(VMRegister.Role.ABI_RETURN);
    }

    public final RegisterConfig registerConfig;

    /**
     * A target ABI specifies a number of register roles assignment used by the compiler that produces the target code, as well as
     * the entry point {@linkplain CallEntryPoint call entry point} associated with its compiler. The latter can be used
     * to compute the offset to entry point of callees of the target methods associated with the TargetABI.
     *
     * @param callPushesReturnAddress indicates whether call instructions push a callee's return address on to the stack.
     * @param stackFrameAlignment alignment requirement for stack frame.
     * @see Role
     */
    public TargetABI(RegisterRoleAssignment<IntegerRegister_Type, FloatingPointRegister_Type> registerRoleAssignment, CallEntryPoint callEntryPoint,
                    List<IntegerRegister_Type> integerIncomingParameterRegisters,
                    List<IntegerRegister_Type> integerOutgoingParameterRegisters,
                    List<FloatingPointRegister_Type> floatingPointParameterRegisters, boolean useRegisterWindows, boolean callPushesReturnAddress, int stackFrameAlignment, int stackBias) {
        this.registerRoleAssignment = registerRoleAssignment;
        this.callEntryPoint = callEntryPoint;
        this.integerIncomingParameterRegisters = integerIncomingParameterRegisters;
        this.integerOutgoingParameterRegisters = integerOutgoingParameterRegisters;
        this.floatingPointParameterRegisters = floatingPointParameterRegisters;
        this.useRegisterWindows = useRegisterWindows;
        this.callPushesReturnAddress = callPushesReturnAddress;
        this.stackFrameAlignment = stackFrameAlignment;
        this.stackBias = stackBias;
        this.registerConfig = new RegisterConfig(this);
    }

    public TargetABI(TargetABI<IntegerRegister_Type, FloatingPointRegister_Type> original,
                    RegisterRoleAssignment<IntegerRegister_Type, FloatingPointRegister_Type> registerRoleAssignment,
                    CallEntryPoint callEntryPoint) {
        this.registerRoleAssignment = registerRoleAssignment;
        this.callEntryPoint = callEntryPoint;
        this.integerIncomingParameterRegisters = original.integerIncomingParameterRegisters;
        this.integerOutgoingParameterRegisters = original.integerOutgoingParameterRegisters;
        this.floatingPointParameterRegisters = original.floatingPointParameterRegisters;
        this.useRegisterWindows = original.useRegisterWindows;
        this.callPushesReturnAddress = original.callPushesReturnAddress;
        this.stackFrameAlignment = original.stackFrameAlignment;
        this.stackBias = original.stackBias;
        this.registerConfig = new RegisterConfig(this);

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
            List<? extends Symbol> sequence = null;
            int index = 0;
            if (putIntoIntegerRegister(parameterKinds[i])) {
                sequence = this.integerIncomingParameterRegisters;
                index = integerIndex;
                integerIndex++;
            } else {
                sequence = this.floatingPointParameterRegisters;
                index = floatIndex;
                floatIndex++;
            }

            if (index >= sequence.size()) {
                // Get from stack slot
                result[i] = new TargetLocation.ParameterStackSlot(stackIndex);
                stackIndex++;
            } else {
                if (sequence == this.integerIncomingParameterRegisters) {
                    result[i] = new TargetLocation.IntegerRegister(sequence.get(index).value());
                } else {
                    result[i] = new TargetLocation.FloatingPointRegister(sequence.get(index).value());
                }
            }
        }
        return result;
    }

    /**
     * A partial implementation of {@link RiRegisterConfig} to satisfy uses of {@link TargetMethod#getRegisterConfig()}
     * on CPS target methods. This will no longer be necessary once CPS goes away.
     */
    public static class RegisterConfig implements RiRegisterConfig {
        final CiRegister[] inParameters;
        final CiRegister[] outParameters;
        final CiRegister cpuReturn;
        final CiRegister fpuReturn;
        final CiRegister frameReg;
        final CiRegister scratchReg;

        private static CiRegister[] toArray(List cpuRegs, List fpuRegs) {
            CiRegister[] res = new CiRegister[cpuRegs.size() + fpuRegs.size()];
            int i = 0;
            for (Object reg : cpuRegs) {
                res[i++] = cpuReg((Symbol) reg);
            }
            for (Object reg : fpuRegs) {
                res[i++] = fpuReg((Symbol) reg);
            }
            return res;
        }

        public RegisterConfig(TargetABI abi) {
            inParameters = toArray(abi.integerIncomingParameterRegisters, abi.floatingPointParameterRegisters);
            outParameters = toArray(abi.integerOutgoingParameterRegisters, abi.floatingPointParameterRegisters);
            cpuReturn = cpuReg(abi.integerReturn());
            fpuReturn = fpuReg(abi.floatingPointReturn());
            frameReg = cpuReg(abi.framePointer());
            scratchReg = cpuReg(abi.scratchRegister());
        }

        private static CiRegister cpuReg(Symbol reg) {
            if (reg == null) {
                return null;
            }
            return target().arch.registerFor(reg.value(), RegisterFlag.CPU);
        }

        private static CiRegister fpuReg(Symbol reg) {
            if (reg == null) {
                return null;
            }
            return target().arch.registerFor(reg.value(), RegisterFlag.FPU);
        }
        public CiRegister getReturnRegister(CiKind kind) {
            if (kind.isDouble() || kind.isWord()) {
                return fpuReturn;
            }
            assert !kind.isVoid();
            return cpuReturn;
        }
        public CiRegister getFrameRegister() {
            return frameReg;
        }
        public CiRegister getScratchRegister() {
            return scratchReg;
        }
        public CiCallingConvention getCallingConvention(Type type, CiKind[] parameters, CiTarget target) {
            throw FatalError.unimplemented();
        }
        public CiRegister[] getCallingConventionRegisters(Type type, RegisterFlag flag) {
            switch (type) {
                case JavaCall:
                    return outParameters;
                case JavaCallee:
                    return inParameters;
                case NativeCall:
                    break;
                case RuntimeCall:
                    break;
            }
            throw FatalError.unimplemented();
        }
        public CiRegister[] getAllocatableRegisters() {
            throw FatalError.unimplemented();
        }
        public EnumMap<RegisterFlag, CiRegister[]> getCategorizedAllocatableRegisters() {
            throw FatalError.unimplemented();
        }
        public CiRegister[] getCallerSaveRegisters() {
            throw FatalError.unimplemented();
        }
        public CiCalleeSaveArea getCalleeSaveArea() {
            return null;
        }
        public RiRegisterAttributes[] getAttributesMap() {
            throw FatalError.unimplemented();
        }
        public CiRegister getRegisterForRole(int id) {
            throw FatalError.unimplemented();
        }
    }
}
