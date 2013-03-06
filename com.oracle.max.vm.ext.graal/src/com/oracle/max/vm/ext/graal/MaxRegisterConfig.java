/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.graal;

import java.util.*;
import java.util.concurrent.*;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.code.CallingConvention.Type;
import com.oracle.graal.api.code.Register.RegisterFlag;
import com.oracle.graal.api.meta.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.platform.*;


public class MaxRegisterConfig implements RegisterConfig {

    private static ConcurrentHashMap<RiRegisterConfig, RegisterConfig> map = new ConcurrentHashMap<RiRegisterConfig, RegisterConfig>();

    private RiRegisterConfig riRegisterConfig;

    static RegisterConfig get(RiRegisterConfig riRegisterConfig) {
        RegisterConfig result = map.get(riRegisterConfig);
        if (result == null) {
            result = new MaxRegisterConfig(riRegisterConfig);
            map.put(riRegisterConfig, result);
        }
        return result;
    }

    private MaxRegisterConfig(RiRegisterConfig riRegisterConfig) {
        this.riRegisterConfig = riRegisterConfig;
    }

    @Override
    public Register getReturnRegister(Kind kind) {
        return RegisterMap.toGraal(riRegisterConfig.getReturnRegister(KindMap.toCiKind(kind)));
    }

    @Override
    public Register getFrameRegister() {
        return RegisterMap.toGraal(riRegisterConfig.getFrameRegister());
    }

    @Override
    public Register getScratchRegister() {
        return RegisterMap.toGraal(riRegisterConfig.getScratchRegister());
    }

    @Override
    public CallingConvention getCallingConvention(Type type, JavaType returnType, JavaType[] parameters, TargetDescription target, boolean stackOnly) {
        CiKind[] ciParameters = new CiKind[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            ciParameters[i] = KindMap.toCiKind(parameters[i].getKind());
        }
        // assert: this will be functionally equivalent to "target"
        CiTarget ciTarget = Platform.platform().target;
        CiCallingConvention ciConv = riRegisterConfig.getCallingConvention(toCiType(type), ciParameters, ciTarget, stackOnly);
        Kind returnKind = MaxWordTypeRewriterPhase.checkWord(returnType);
        Value returnLocation = returnType.getKind() == Kind.Void ? Value.ILLEGAL : getReturnRegister(returnKind).asValue(returnKind);
        CiValue[] ciValues = ciConv.locations;
        Value[] values = new Value[ciValues.length];
        for (int i = 0; i < ciValues.length; i++) {
            values[i] = ValueMap.toGraal(ciValues[i]);
        }
        return new CallingConvention(ciConv.stackSize, returnLocation, values);
    }

    private static ConcurrentHashMap<Type, Register[]> callingConventionRegistersMap =
                    new ConcurrentHashMap<Type, Register[]>();

    @Override
    public Register[] getCallingConventionRegisters(Type type, RegisterFlag flag) {
        Register[] regs = callingConventionRegistersMap.get(type);
        if (regs == null) {
            CiRegister[] ciRegs = riRegisterConfig.getCallingConventionRegisters(toCiType(type), toCiFlag(flag));
            regs = new Register[ciRegs.length];
            for (int i = 0; i < ciRegs.length; i++) {
                regs[i] = RegisterMap.toGraal(ciRegs[i]);
            }
            callingConventionRegistersMap.put(type, regs);
        }
        return regs;
    }

    private static CiCallingConvention.Type toCiType(Type type) {
        // Checkstyle: stop
        switch (type) {
            case JavaCall: return CiCallingConvention.Type.JavaCall;
            case JavaCallee: return CiCallingConvention.Type.JavaCallee;
            case RuntimeCall: return CiCallingConvention.Type.RuntimeCall;
            case NativeCall: return CiCallingConvention.Type.NativeCall;
            default: return null;
        }
        // Checkstyle: resume
    }

    private static CiRegister.RegisterFlag toCiFlag(RegisterFlag flag) {
        // Checkstyle: stop
        switch (flag) {
            case CPU: return CiRegister.RegisterFlag.CPU;
            case Byte: return CiRegister.RegisterFlag.Byte;
            case FPU: return CiRegister.RegisterFlag.FPU;
            default: return null;
        }
        // Checkstyle: resume
    }

    private static RegisterFlag toGraalFlag(CiRegister.RegisterFlag flag) {
        // can't use a switch because can't qualify
        if (flag == CiRegister.RegisterFlag.CPU) {
            return RegisterFlag.CPU;
        } else if (flag == CiRegister.RegisterFlag.FPU) {
            return RegisterFlag.FPU;
        } else if (flag == CiRegister.RegisterFlag.Byte) {
            return RegisterFlag.Byte;
        } else {
            return null;
        }
    }

    private static Register[] allocatableRegisters;

    @Override
    public Register[] getAllocatableRegisters() {
        if (allocatableRegisters == null) {
            CiRegister[] ciRegs = riRegisterConfig.getAllocatableRegisters();
            allocatableRegisters = new Register[ciRegs.length];
            for (int i = 0; i < ciRegs.length; i++) {
                allocatableRegisters[i] = RegisterMap.toGraal(ciRegs[i]);
            }
        }
        return allocatableRegisters;
    }

    private static EnumMap<RegisterFlag, Register[]> categorizedAllocatableRegisters;
    @Override
    public EnumMap<RegisterFlag, Register[]> getCategorizedAllocatableRegisters() {
        if (categorizedAllocatableRegisters == null) {
            EnumMap<CiRegister.RegisterFlag, CiRegister[]> ciRegMap = riRegisterConfig.getCategorizedAllocatableRegisters();
            categorizedAllocatableRegisters = new EnumMap<RegisterFlag, Register[]>(RegisterFlag.class);
            for (Map.Entry<CiRegister.RegisterFlag, CiRegister[]> entry : ciRegMap.entrySet()) {
                CiRegister[] ciRegs = entry.getValue();
                Register[] regs = new Register[ciRegs.length];
                for (int i = 0; i < ciRegs.length; i++) {
                    regs[i] = RegisterMap.toGraal(ciRegs[i]);
                }
                categorizedAllocatableRegisters.put(toGraalFlag(entry.getKey()), regs);
            }
        }
        return categorizedAllocatableRegisters;
    }

    private static Register[] callerSaveRegisters;

    @Override
    public Register[] getCallerSaveRegisters() {
        if (callerSaveRegisters == null) {
            CiRegister[] ciRegs = riRegisterConfig.getCallerSaveRegisters();
            callerSaveRegisters = new Register[ciRegs.length];
            for (int i = 0; i < ciRegs.length; i++) {
                callerSaveRegisters[i] = RegisterMap.toGraal(ciRegs[i]);
            }
        }
        return callerSaveRegisters;
    }

    @Override
    public CalleeSaveLayout getCalleeSaveLayout() {
        CiCalleeSaveLayout ciSaveLayout = riRegisterConfig.getCalleeSaveLayout();
        if (ciSaveLayout == null) {
            return null;
        }

        CiRegister[] ciRegs = ciSaveLayout.registers;
        Register[] regs = new Register[ciRegs.length];
        for (int i = 0; i < ciRegs.length; i++) {
            regs[i] = RegisterMap.toGraal(ciRegs[i]);
        }
        return new CalleeSaveLayout(ciSaveLayout.frameOffsetToCSA, ciSaveLayout.size, ciSaveLayout.slotSize, regs);
    }

    private static RegisterAttributes[] registerAttributes;

    @Override
    public RegisterAttributes[] getAttributesMap() {
        if (registerAttributes == null) {
            RiRegisterAttributes[] riAttrs = riRegisterConfig.getAttributesMap();
            registerAttributes = new RegisterAttributes[riAttrs.length];
            for (int i = 0; i < riAttrs.length; i++) {
                RiRegisterAttributes riAttr = riAttrs[i];
                registerAttributes[i] = new RegisterAttributes(riAttr.isCallerSave, riAttr.isCalleeSave, riAttr.isAllocatable);
            }
        }
        return registerAttributes;
    }

    @Override
    public Register getRegisterForRole(int id) {
        return RegisterMap.toGraal(riRegisterConfig.getRegisterForRole(id));
    }

}
