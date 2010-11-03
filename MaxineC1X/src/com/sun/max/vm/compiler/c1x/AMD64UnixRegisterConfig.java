/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.compiler.c1x;

import static com.sun.c1x.target.amd64.AMD64.*;
import static com.sun.cri.ci.CiKind.*;
import static com.sun.max.vm.runtime.VMRegister.Role.*;

import java.util.*;

import com.sun.c1x.globalstub.*;
import com.sun.c1x.target.amd64.*;
import com.sun.c1x.util.*;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiCallingConvention.*;
import com.sun.cri.ci.CiRegister.RegisterFlag;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.VMRegister.Role;

/**
 * The set of register configurations used by Mainxe on Unix-AMD64. Where applicable,
 * these configurations comply with <a href="http://www.x86-64.org/documentation/abi-0.96.pdf">
 * System V Application Binary Interface</a>.
 *
 * @author Doug Simon
 */
public class AMD64UnixRegisterConfig implements RiRegisterConfig, Cloneable {

    /**
     * The set of allocatable registers shared by most register configurations.
     */
    public static final CiRegister[] StandardAllocatable = {
        rax, rcx, rdx, rbx, rsi, rdi, r8, r9, r10, r12, r13, r15,
        xmm0, xmm1, xmm2,  xmm3,  xmm4,  xmm5,  xmm6,  xmm7,
        xmm8, xmm9, xmm10, xmm11, xmm12, xmm13, xmm14, xmm15};

    /**
     * The register configuration for a normal Java method.
     * This configuration specifies <b>all</b> allocatable registers as caller-saved
     * as inlining is expected to reduce the call overhead sufficiently.
     */
    public static final AMD64UnixRegisterConfig STANDARD = new AMD64UnixRegisterConfig(
        def("Allocatable", StandardAllocatable),
        def("CallerSave",  StandardAllocatable),
        def("CalleeSave"   /* none */),
        def("Parameters",  rdi, rsi, rdx, rcx, r8, r9,
                           xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7),
        def("Scratch",     r11)).

        def(CPU_STACK_POINTER, rsp).
        def(CPU_FRAME_POINTER, rbp).
        def(ABI_STACK_POINTER, rsp).
        def(ABI_FRAME_POINTER, rsp).
        def(ABI_RETURN,        rax).
        def(ABI_RESULT,        rax).
        def(ABI_SCRATCH,       r11).
        def(SAFEPOINT_LATCH,   r14).

        def("ReturnIntegral", rax,  Boolean, Byte, Char, Short, Int, Long, Word, Object).
        def("ReturnFloat",    xmm0, Float, Double);

    /**
     * The register configuration for a method called directly from native/C code.
     * This configuration preserves all native ABI specified callee saved registers.
     */
    public static final AMD64UnixRegisterConfig N2J = STANDARD.withCalleeSave(rbx, rbp, r12, r13, r14, r15);

    /**
     * The register configuration for a direct call to native/C code.
     * This configuration specifies <b>all</b> allocatable registers as caller-saved
     * so that all object references are on the stack around the native call.
     * These values will also be spilled to the stack by a native compiler
     * adhering to the ABI callee-save semantics. However, the ABI does not
     * specify where they are spilled and so they are not available
     * to the GC roots scanner.
     */
    public static final AMD64UnixRegisterConfig J2N = STANDARD;

    /**
     * The register configuration for a direct call to a {@linkplain MethodActor#isTrampoline() trampoline}.
     * This configuration lists all parameter registers as callee saved.
     */
    public static final AMD64UnixRegisterConfig TRAMPOLINE = STANDARD.
        withCalleeSave(rdi, rsi, rdx, rcx, r8, r9, rbx, rbp,
                       xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7);

    /**
     * The register configuration for compiling a {@linkplain GlobalStub stub}.
     */
    public static final AMD64UnixRegisterConfig STUB = STANDARD.withCalleeSave(allRegisters);

    /**
     * The register configuration for compiling the {@linkplain Trap#isTrapStub(MethodActor) trap stub}.
     */
    public static final AMD64UnixRegisterConfig TRAP_STUB = STANDARD.withCalleeSave(allRegisters);

    private final EnumMap<Role, CiRegister> registersByRole = new EnumMap<VMRegister.Role, CiRegister>(Role.class);
    private final CiRegister[] allocatable;
    private final EnumMap<RegisterFlag, CiRegister[]> categorized;
    private final CiRegister[] parameters;
    private final CiRegister[] cpuParameters;
    private final CiRegister[] fpuParameters;
    private final CiRegister[] callerSave;
    private CiRegister[] calleeSave;
    private final CiRegister[] ret;
    private final RiRegisterAttributes[] attributesMap;
    private final CiRegister scratch;

    @HOSTED_ONLY
    public AMD64UnixRegisterConfig(CiRegister[] allocatable, CiRegister[] callerSave, CiRegister[] calleeSave, CiRegister[] parameters, CiRegister scratch) {
        this.allocatable = allocatable;
        this.callerSave = callerSave;
        this.calleeSave = calleeSave;
        assert !Arrays.asList(allocatable).contains(scratch);
        this.scratch = scratch;
        this.parameters = parameters;
        EnumMap<RegisterFlag, CiRegister[]> categorizedParameters = CiRegister.categorize(parameters);
        this.cpuParameters = categorizedParameters.get(RegisterFlag.CPU);
        this.fpuParameters = categorizedParameters.get(RegisterFlag.FPU);
        categorized = CiRegister.categorize(allocatable);
        attributesMap = RiRegisterAttributes.createMap(this, AMD64.allRegisters);
        ret = new CiRegister[CiKind.VALUES.length];
    }

    @HOSTED_ONLY
    public AMD64UnixRegisterConfig withCalleeSave(CiRegister... calleeSave) {
        AMD64UnixRegisterConfig copy = clone();
        copy.calleeSave = calleeSave;
        return copy;
    }

    @Override
    protected AMD64UnixRegisterConfig clone() {
        try {
            return (AMD64UnixRegisterConfig) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError("Could not clone " + getClass().getName());
        }
    }

    @Override
    public String toString() {
        String res = String.format(
             "Allocatable: " + Arrays.toString(getAllocatableRegisters()) + "%n" +
             "CallerSave:  " + Arrays.toString(getCallerSaveRegisters()) + "%n" +
             "CalleeSave:  " + Arrays.toString(getCalleeSaveRegisters()) + "%n" +
             "CPU Params:  " + Arrays.toString(cpuParameters) + "%n" +
             "FPU Params:  " + Arrays.toString(fpuParameters) + "%n" +
             "VMRoles:     " + registersByRole + "%n" +
             "Scratch:     " + getScratchRegister() + "%n");
        return res;
    }

    public CiRegister getReturnRegister(CiKind kind) {
        return ret[kind.ordinal()];
    }

    public CiRegister getFrameRegister() {
        return rsp;
    }

    public CiRegister getScratchRegister() {
        return scratch;
    }

    public CiCallingConvention getCallingConvention(Type type, CiKind[] parameters, boolean outgoing, CiTarget target) {
        return callingConvention(parameters, type == Type.Runtime ? true : outgoing, target);
    }

    public CiRegister[] getCallingConventionRegisters(Type type) {
        return parameters;
    }

    public CiRegister[] getAllocatableRegisters() {
        return allocatable;
    }

    public EnumMap<RegisterFlag, CiRegister[]> getCategorizedAllocatableRegisters() {
        return categorized;
    }

    public CiRegister[] getCallerSaveRegisters() {
        return allocatable;
    }

    public CiRegister[] getCalleeSaveRegisters() {
        return calleeSave;
    }

    public RiRegisterAttributes[] getAttributesMap() {
        return attributesMap;
    }

    public CiRegister getRegister(int id) {
        if (id < 0 || id >= Role.VALUES.size()) {
            return null;
        }
        return registersByRole.get(Role.VALUES.get(id));
    }

    private CiCallingConvention callingConvention(CiKind[] types, boolean outgoing, CiTarget target) {
        CiValue[] locations = new CiValue[types.length];

        int currentGeneral = 0;
        int currentXMM = 0;
        int currentStackIndex = 0;

        for (int i = 0; i < types.length; i++) {
            final CiKind kind = types[i];

            switch (kind) {
                case Byte:
                case Boolean:
                case Short:
                case Char:
                case Int:
                case Long:
                case Word:
                case Object:
                    if (currentGeneral < cpuParameters.length) {
                        CiRegister register = cpuParameters[currentGeneral++];
                        locations[i] = register.asValue(kind);
                    }
                    break;

                case Float:
                case Double:
                    if (currentXMM < fpuParameters.length) {
                        CiRegister register = fpuParameters[currentXMM++];
                        locations[i] = register.asValue(kind);
                    }
                    break;

                default:
                    throw Util.shouldNotReachHere();
            }

            if (locations[i] == null) {
                locations[i] = CiStackSlot.get(kind.stackKind(), currentStackIndex, !outgoing);
                currentStackIndex += target.spillSlots(kind);
            }
        }

        return new CiCallingConvention(locations, currentStackIndex * target.spillSlotSize);
    }

    @HOSTED_ONLY
    private static CiRegister[] def(String comment, CiRegister head, CiRegister... tail) {
        CiRegister[] regs = new CiRegister[tail.length + 1];
        regs[0] = head;
        System.arraycopy(tail, 0, regs, 1, tail.length);
        return regs;
    }

    @HOSTED_ONLY
    private static CiRegister[] def(String comment) {
        return new CiRegister[0];
    }

    @HOSTED_ONLY
    private static CiRegister[] def(String comment, CiRegister[] regs) {
        return regs;
    }

    @HOSTED_ONLY
    private static CiRegister def(String comment, CiRegister reg) {
        return reg;
    }

    @HOSTED_ONLY
    private AMD64UnixRegisterConfig def(Role role, CiRegister reg) {
        registersByRole.put(role, reg);
        return this;
    }

    @HOSTED_ONLY
    private AMD64UnixRegisterConfig def(String comment, CiRegister reg, CiKind... kinds) {
        for (CiKind kind : kinds) {
            ret[kind.ordinal()] = reg;
        }
        return this;
    }
}
