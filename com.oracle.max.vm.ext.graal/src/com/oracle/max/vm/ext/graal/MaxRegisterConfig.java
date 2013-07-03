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

import static com.oracle.graal.amd64.AMD64.*;

import java.util.*;
import java.util.concurrent.*;

import com.oracle.graal.amd64.*;
import com.oracle.graal.api.code.*;
import com.oracle.graal.api.code.CallingConvention.Type;
import com.oracle.graal.api.code.Register.RegisterCategory;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.graph.*;
import com.oracle.max.vm.ext.graal.phases.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.vm.compiler.deopt.*;


public class MaxRegisterConfig implements RegisterConfig {

    private static Architecture architecture;

    private static ConcurrentHashMap<RiRegisterConfig, RegisterConfig> map = new ConcurrentHashMap<RiRegisterConfig, RegisterConfig>();

    private RiRegisterConfig riRegisterConfig;
    private final Register[] javaGeneralParameterRegisters;
    private final Register[] nativeGeneralParameterRegisters;
    private Register[] xmmParameterRegisters = {xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7};

    static void initialize(Architecture architecture) {
        MaxRegisterConfig.architecture = architecture;
    }

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
        javaGeneralParameterRegisters = initParameterRegisters(CiCallingConvention.Type.JavaCall, riRegisterConfig);
        nativeGeneralParameterRegisters = initParameterRegisters(CiCallingConvention.Type.NativeCall, riRegisterConfig);
    }

    private static Register[] initParameterRegisters(CiCallingConvention.Type type,  RiRegisterConfig riRegisterConfig) {
        CiRegister[] jRegs = riRegisterConfig.getCallingConventionRegisters(type, CiRegister.RegisterFlag.CPU);
        Register[] result = new Register[jRegs.length];
        for (int i = 0; i < jRegs.length; i++) {
            CiRegister ciReg = jRegs[i];
            result[i] = RegisterMap.toGraal(ciReg);
        }
        return result;
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
    public CallingConvention getCallingConvention(Type type, JavaType returnType, JavaType[] parameterTypes, TargetDescription target, boolean stackOnly) {
        assert type != Type.NativeCall;
        return callingConvention(javaGeneralParameterRegisters, returnType, parameterTypes, type, target, stackOnly);
    }

    public CallingConvention nativeCallingConvention(Kind returnKind, Kind[] parameterKinds) {
        return callingConvention(nativeGeneralParameterRegisters, returnKind, parameterKinds, Type.NativeCall, MaxGraal.runtime().getTarget(), false);
    }

    private CallingConvention callingConvention(Register[] generalParameterRegisters, JavaType returnType, JavaType[] parameterTypes, Type type, TargetDescription target, boolean stackOnly) {
        Kind[] parameterKinds = new Kind[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            parameterKinds[i] = MaxWordTypeRewriterPhase.checkWord(parameterTypes[i]);
        }
        Kind returnKind = returnType == null ? Kind.Void : MaxWordTypeRewriterPhase.checkWord(returnType);
        return callingConvention(generalParameterRegisters, returnKind, parameterKinds, type, target, stackOnly);

    }

    private CallingConvention callingConvention(Register[] generalParameterRegisters, Kind returnKind, Kind[] parameterKinds, Type type, TargetDescription target, boolean stackOnly) {
        AllocatableValue[] locations = new AllocatableValue[parameterKinds.length];

        int currentGeneral = 0;
        int currentXMM = 0;
        // Account for the word at the bottom of the frame used for saving an overwritten return address during deoptimization
        int currentStackOffset = type == Type.NativeCall ? 0 : Deoptimization.DEOPT_RETURN_ADDRESS_OFFSET + target.wordSize;

        for (int i = 0; i < parameterKinds.length; i++) {
            Kind kind = parameterKinds[i];

            switch (kind) {
                case Byte:
                case Boolean:
                case Short:
                case Char:
                case Int:
                case Long:
                case Object:
                    if (!stackOnly && currentGeneral < generalParameterRegisters.length) {
                        Register register = generalParameterRegisters[currentGeneral++];
                        locations[i] = register.asValue(kind);
                    }
                    break;
                case Float:
                case Double:
                    if (!stackOnly && currentXMM < xmmParameterRegisters.length) {
                        Register register = xmmParameterRegisters[currentXMM++];
                        locations[i] = register.asValue(kind);
                    }
                    break;
                default:
                    throw GraalInternalError.shouldNotReachHere();
            }

            if (locations[i] == null) {
                locations[i] = StackSlot.get(kind.getStackKind(), currentStackOffset, !type.out);
                currentStackOffset += Math.max(target.arch.getSizeInBytes(kind), target.wordSize);
            }
        }

        AllocatableValue returnLocation = returnKind == Kind.Void ? Value.ILLEGAL : getReturnRegister(returnKind).asValue(returnKind);
        return new CallingConvention(currentStackOffset, returnLocation, locations);
    }

    @Override
    public Register[] getCallingConventionRegisters(Type type, Kind kind) {
        if (architecture.canStoreValue(XMM, kind)) {
            return xmmParameterRegisters;
        }
        assert architecture.canStoreValue(CPU, kind);
        return type == Type.NativeCall ? nativeGeneralParameterRegisters : javaGeneralParameterRegisters;
    }

    private static CiCallingConvention.Type toCiType(Type type) {
        // Checkstyle: stop
        switch (type) {
            case JavaCall: return CiCallingConvention.Type.JavaCall;
            case JavaCallee: return CiCallingConvention.Type.JavaCallee;
            case NativeCall: return CiCallingConvention.Type.NativeCall;
            default: return null;
        }
        // Checkstyle: resume
    }

    private static RegisterCategory toGraalFlag(CiRegister.RegisterFlag flag) {
        // can't use a switch because can't qualify
        if (flag == CiRegister.RegisterFlag.CPU) {
            return AMD64.CPU;
        } else if (flag == CiRegister.RegisterFlag.FPU) {
            return AMD64.XMM;
        } else if (flag == CiRegister.RegisterFlag.Byte) {
            assert false;
            return null;
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

    private final HashMap<PlatformKind, Register[]> categorized = new HashMap<>();

    @Override
    public Register[] getAllocatableRegisters(PlatformKind kind) {
        if (categorized.containsKey(kind)) {
            return categorized.get(kind);
        }

        ArrayList<Register> list = new ArrayList<>();
        for (Register reg : getAllocatableRegisters()) {
            if (architecture.canStoreValue(reg.getRegisterCategory(), kind)) {
                list.add(reg);
            }
        }

        Register[] ret = list.toArray(new Register[0]);
        categorized.put(kind, ret);
        return ret;
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
        return new CalleeSaveLayout(architecture, ciSaveLayout.frameOffsetToCSA, ciSaveLayout.size, ciSaveLayout.slotSize, regs);
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
