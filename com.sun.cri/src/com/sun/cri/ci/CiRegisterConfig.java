/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
package com.sun.cri.ci;

import java.util.*;

import com.sun.cri.ci.CiCallingConvention.*;
import com.sun.cri.ci.CiRegister.*;
import com.sun.cri.ri.*;

/**
 * A default implementation of {@link RiRegisterConfig}.
 */
public class CiRegisterConfig implements RiRegisterConfig {

    /**
     * The object describing the callee save area of this register configuration.
     */
    public CiCalleeSaveLayout csl;

    /**
     * The minimum register role identifier.
     */
    public final int minRole;

    /**
     * The map from register role IDs to registers.
     */
    public final CiRegister[] registersRoleMap;

    /**
     * The set of registers that can be used by the register allocator.
     */
    public final CiRegister[] allocatable;

    /**
     * The set of registers that can be used by the register allocator, {@linkplain CiRegister#categorize(CiRegister[])
     * categorized} by register {@linkplain RegisterFlag flags}.
     */
    public final EnumMap<RegisterFlag, CiRegister[]> categorized;

    /**
     * The ordered set of registers used to pass integral arguments.
     */
    public final CiRegister[] cpuParameters;

    /**
     * The ordered set of registers used to pass floating point arguments.
     */
    public final CiRegister[] fpuParameters;

    /**
     * The caller saved registers.
     */
    public final CiRegister[] callerSave;

    /**
     * The register to which {@link CiRegister#Frame} and {@link CiRegister#CallerFrame} are bound.
     */
    public final CiRegister frame;

    /**
     * Register for returning an integral value.
     */
    public final CiRegister integralReturn;

    /**
     * Register for returning a floating point value.
     */
    public final CiRegister floatingPointReturn;

    /**
     * The map from register {@linkplain CiRegister#number numbers} to register {@linkplain RiRegisterAttributes
     * attributes} for this register configuration.
     */
    public final RiRegisterAttributes[] attributesMap;

    /**
     * The scratch register(s).
     */
    public final CiRegister scratch;
    public final CiRegister scratch1;

    /**
     * The frame offset of the first stack argument for each calling convention {@link CiCallingConvention.Type}.
     */
    public final int[] stackArg0Offsets = new int[CiCallingConvention.Type.VALUES.length];

    public CiRegisterConfig(CiRegister frame, CiRegister integralReturn, CiRegister floatingPointReturn, CiRegister scratch, CiRegister scratch1, CiRegister[] allocatable, CiRegister[] callerSave,
                            CiRegister[] parameters, CiCalleeSaveLayout csl, CiRegister[] allRegisters, Map<Integer, CiRegister> roles) {
        this.frame = frame;
        this.csl = csl;
        this.allocatable = allocatable;
        this.callerSave = callerSave;
        assert !Arrays.asList(allocatable).contains(scratch);
        this.scratch = scratch;
        this.scratch1 = scratch1;
        EnumMap<RegisterFlag, CiRegister[]> categorizedParameters = CiRegister.categorize(parameters);
        this.cpuParameters = categorizedParameters.get(RegisterFlag.CPU);
        this.fpuParameters = categorizedParameters.get(RegisterFlag.FPU);
        categorized = CiRegister.categorize(allocatable);
        attributesMap = RiRegisterAttributes.createMap(this, allRegisters);
        this.floatingPointReturn = floatingPointReturn;
        this.integralReturn = integralReturn;
        int minRoleId = Integer.MAX_VALUE;
        int maxRoleId = Integer.MIN_VALUE;
        for (Map.Entry<Integer, CiRegister> e : roles.entrySet()) {
            int id = e.getKey();
            assert id >= 0;
            if (minRoleId > id) {
                minRoleId = id;
            }
            if (maxRoleId < id) {
                maxRoleId = id;
            }
        }
        registersRoleMap = new CiRegister[(maxRoleId - minRoleId) + 1];
        for (Map.Entry<Integer, CiRegister> e : roles.entrySet()) {
            int id = e.getKey();
            registersRoleMap[id] = e.getValue();
        }
        minRole = minRoleId;
    }

    public CiRegisterConfig(CiRegisterConfig src, CiCalleeSaveLayout csl) {
        this.frame = src.frame;
        this.csl = csl;
        this.allocatable = src.allocatable;
        this.callerSave = src.callerSave;
        this.scratch = src.scratch;
        this.scratch1 = src.scratch1;
        this.cpuParameters = src.cpuParameters;
        this.fpuParameters = src.fpuParameters;
        this.categorized = src.categorized;
        this.attributesMap = src.attributesMap;
        this.floatingPointReturn = src.floatingPointReturn;
        this.integralReturn = src.integralReturn;
        this.registersRoleMap = src.registersRoleMap;
        this.minRole = src.minRole;
        System.arraycopy(src.stackArg0Offsets, 0, stackArg0Offsets, 0, stackArg0Offsets.length);
    }

    public CiRegister getReturnRegister(CiKind kind) {
        if (kind.isDouble() || kind.isFloat()) {
            return floatingPointReturn;
        }
        return integralReturn;
    }

    public CiRegister getFrameRegister() {
        return frame;
    }

    public CiRegister getScratchRegister() {
        return scratch;
    }

    public CiRegister getScratchRegister1() {
        return scratch1;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation assigns all available registers to parameters before assigning any stack slots to parameters.
     */
    public CiCallingConvention getCallingConvention(Type type, CiKind[] parameters, CiTarget target, boolean stackOnly) {
        CiValue[] locations = new CiValue[parameters.length];
        int currentGeneral = 0;
        int currentFloat = 0;
        int firstStackIndex = (stackArg0Offsets[type.ordinal()]) / target.spillSlotSize;
        int currentStackIndex = firstStackIndex;

        for (int i = 0; i < parameters.length; i++) {
            locations[i] = null;
            final CiKind kind = parameters[i];
            switch (kind) {
                case Byte:
                case Boolean:
                case Short:
                case Char:
                case Int:
                case Long:
                case Object:
                    if (!stackOnly && currentGeneral < cpuParameters.length) {
                        if (kind.isLong() && target.arch.is32bit()) {
                            if ((cpuParameters.length - currentGeneral) < 2) {
                                currentGeneral = cpuParameters.length;
                                break;
                            }
                            if (currentGeneral % 2 != 0) {
                                currentGeneral++;
                            }
                        }
                        CiRegister register = cpuParameters[currentGeneral];
                        locations[i] = register.asValue(kind);
                        if (kind.isLong() && target.arch.is32bit()) {
                            currentGeneral += 2;
                        } else {
                            currentGeneral++;
                        }
                    }
                    break;
                case Float:
                case Double: {
                    // ABI for RISCV specifies that in the case of running out of FPU parameter slots when passing
                    // FPU values, you must use the available CPU registers, and only after that the stack slots.
                    // https://github.com/riscv/riscv-elf-psabi-doc/blob/master/riscv-elf.md
                    // Be aware that any change made to this must be compatible with the RISCV64AdapterGenerator::adapt for Baseline2Opt and Opt2Baseline
                    if (target.arch.isRISCV64() && currentFloat == fpuParameters.length
                            && currentGeneral < cpuParameters.length) {
                        CiRegister register = cpuParameters[currentGeneral];
                        locations[i] = register.asValue(kind);
                        currentGeneral++;
                        break;
                    }

                    if (!stackOnly && currentFloat < fpuParameters.length) {
                        if (kind.isDouble() && target.arch.is32bit()) {
                            if ((fpuParameters.length - currentFloat) < 2) {
                                currentFloat = fpuParameters.length;
                                break;
                            }
                            if (currentFloat % 2 != 0) {
                                currentFloat++;
                            }
                        }
                        CiRegister register = fpuParameters[currentFloat];
                        locations[i] = register.asValue(kind);
                        if (kind.isDouble() && target.arch.is32bit()) {
                            currentFloat += 2;
                        } else {
                            currentFloat++;
                        }
                    }
                    break;
                }
                default:
                    throw new InternalError("Unexpected parameter kind: " + kind);
            }

            if (locations[i] == null) {
                if (target.arch.is32bit() && (kind.stackKind() == CiKind.Long || kind.stackKind() == CiKind.Double) && ((currentStackIndex * target.spillSlotSize) % 8 != 0)) {
                    currentStackIndex++;
                }

                locations[i] = CiStackSlot.get(kind.stackKind(), currentStackIndex, !type.out);
                currentStackIndex += target.spillSlots(kind);
            }
        }
        return new CiCallingConvention(locations, (currentStackIndex - firstStackIndex) * target.spillSlotSize);
    }

    public CiRegister[] getCallingConventionRegisters(Type type, RegisterFlag flag) {
        if (flag == RegisterFlag.CPU) {
            return cpuParameters;
        }
        assert flag == RegisterFlag.FPU;
        return fpuParameters;
    }

    public CiRegister[] getAllocatableRegisters() {
        return allocatable;
    }

    public EnumMap<RegisterFlag, CiRegister[]> getCategorizedAllocatableRegisters() {
        return categorized;
    }

    public CiRegister[] getCallerSaveRegisters() {
        return callerSave;
    }

    public CiCalleeSaveLayout getCalleeSaveLayout() {
        return csl;
    }

    public RiRegisterAttributes[] getAttributesMap() {
        return attributesMap;
    }

    public CiRegister getRegisterForRole(int id) {
        return registersRoleMap[id - minRole];
    }

    @Override
    public String toString() {
        StringBuilder roleMap = new StringBuilder();
        for (int i = 0; i < registersRoleMap.length; ++i) {
            CiRegister reg = registersRoleMap[i];
            if (reg != null) {
                if (roleMap.length() != 0) {
                    roleMap.append(", ");
                }
                roleMap.append(i + minRole).append(" -> ").append(reg);
            }
        }
        StringBuilder stackArg0OffsetsMap = new StringBuilder();
        for (Type t : Type.VALUES) {
            if (stackArg0OffsetsMap.length() != 0) {
                stackArg0OffsetsMap.append(", ");
            }
            stackArg0OffsetsMap.append(t).append(" -> ").append(stackArg0Offsets[t.ordinal()]);
        }
        String res = String.format("Allocatable: " + Arrays.toString(getAllocatableRegisters()) + "%n" + "CallerSave:  " + Arrays.toString(getCallerSaveRegisters()) + "%n" + "CalleeSave:  " +
                getCalleeSaveLayout() + "%n" + "CPU Params:  " + Arrays.toString(cpuParameters) + "%n" + "FPU Params:  " + Arrays.toString(fpuParameters) + "%n" + "VMRoles:     " + roleMap +
                "%n" + "stackArg0:   " + stackArg0OffsetsMap + "%n" + "Scratch:     " + getScratchRegister() + "%n");
        return res;
    }
}
