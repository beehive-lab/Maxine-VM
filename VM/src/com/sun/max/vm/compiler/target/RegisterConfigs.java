/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.compiler.target;

import static com.sun.c1x.target.amd64.AMD64.*;
import static com.sun.cri.ci.CiCalleeSaveArea.*;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.runtime.VMRegister.Role.*;

import java.util.*;

import com.sun.c1x.globalstub.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.amd64.*;

/**
 * The set of register configurations applicable to compiled code in the VM.
 *
 * @author Doug Simon
 */
public class RegisterConfigs {
    /**
     * The register configuration for a normal Java method.
     */
    public final CiRegisterConfig standard;

    /**
     * The register configuration for a method called directly from native/C code.
     * This configuration preserves all native ABI specified callee saved registers.
     */
    public final CiRegisterConfig n2j;

    /**
     * The register configuration for a {@linkplain MethodActor#isTrampoline() trampoline}.
     * This configuration lists all parameter registers as callee saved.
     */
    public final CiRegisterConfig trampoline;

    /**
     * The register configuration for compiling the templates for the template-based JIT compiler.
     */
    public final CiRegisterConfig jitTemplate;

    /**
     * The register configuration for compiling a {@linkplain GlobalStub global stub}.
     */
    public final CiRegisterConfig globalStub;

    /**
     * The register configuration for compiling the {@linkplain Stubs#trapStub trap stub}.
     */
    public final CiRegisterConfig trapStub;

    public CiRegisterConfig getRegisterConfig(ClassMethodActor method) {
        if (method.isVmEntryPoint()) {
            return n2j;
        }
        if (method.isTemplate()) {
            return jitTemplate;
        }
        return standard;
    }

    @HOSTED_ONLY
    public RegisterConfigs(
                    CiRegisterConfig standard,
                    CiRegisterConfig n2j,
                    CiRegisterConfig trampoline,
                    CiRegisterConfig template,
                    CiRegisterConfig globalStub,
                    CiRegisterConfig trapStub) {
        this.standard = standard;
        this.n2j = n2j;
        this.trampoline = trampoline;
        this.jitTemplate = template;
        this.globalStub = globalStub;
        this.trapStub = trapStub;

        assert Arrays.equals(standard.getAllocatableRegisters(), standard.getCallerSaveRegisters()) : "VM requires caller-save for VM to VM calls";
    }

    @HOSTED_ONLY
    public static RegisterConfigs create() {
        if (platform().isa == ISA.AMD64) {
            OS os = platform().os;
            if (os == OS.LINUX || os == OS.SOLARIS || os == OS.DARWIN || os == OS.MAXVE) {
                /**
                 * The set of allocatable registers shared by most register configurations.
                 */
                CiRegister[] allocatable = {
                    rax, rcx, rdx, rbx, rsi, rdi, r8, r9, r10, r12, r13, r15,
                    xmm0, xmm1, xmm2,  xmm3,  xmm4,  xmm5,  xmm6,  xmm7,
                    xmm8, xmm9, xmm10, xmm11, xmm12, xmm13, xmm14, xmm15};

                CiRegister[] parameters = {
                    rdi, rsi, rdx, rcx, r8, r9,
                    xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7
                };

                HashMap<Integer, CiRegister> roleMap = new HashMap<Integer, CiRegister>();
                roleMap.put(CPU_STACK_POINTER.ordinal(), rsp);
                roleMap.put(CPU_FRAME_POINTER.ordinal(), rbp);
                roleMap.put(ABI_STACK_POINTER.ordinal(), rsp);
                roleMap.put(ABI_FRAME_POINTER.ordinal(), rsp);
                roleMap.put(ABI_RETURN.ordinal(), rax);
                roleMap.put(ABI_RESULT.ordinal(), rax);
                roleMap.put(ABI_SCRATCH.ordinal(), r11);
                roleMap.put(SAFEPOINT_LATCH.ordinal(), r14);

                /**
                 * The register configuration for a normal Java method.
                 * This configuration specifies <b>all</b> allocatable registers as caller-saved
                 * as inlining is expected to reduce the call overhead sufficiently.
                 */
                CiRegisterConfig standard = new CiRegisterConfig(
                                rsp,                 // frame
                                rax,                 // integral return value
                                xmm0,                // floating point return value
                                r11,                 // scratch
                                allocatable,         // allocatable
                                allocatable,         // caller save
                                parameters,          // parameter registers
                                EMPTY,               // no callee save
                                allRegisters,        // all AMD64 registers
                                roleMap);            // VM register role map

                CiRegisterConfig n2j = new CiRegisterConfig(standard, new CiCalleeSaveArea(-1, 8, rbx, rbp, r12, r13, r14, r15));
                CiRegisterConfig globalStub = new CiRegisterConfig(standard, new CiCalleeSaveArea(-1, 8, allRegisters));
                CiRegisterConfig trapStub = new CiRegisterConfig(standard, AMD64TrapStateAccess.CSA);
                CiRegisterConfig trampoline = new CiRegisterConfig(standard, new CiCalleeSaveArea(-1, 8,
                    rdi, rsi, rdx, rcx, r8, r9,                       // parameters
                    rbp,                                              // must be preserved for template JIT
                    standard.getScratchRegister(),                    // dynamic dispatch index is saved here for stack frame walker
                    xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7    // parameters
                ));

                roleMap.put(ABI_FRAME_POINTER.ordinal(), rbp);
                CiRegisterConfig template = new CiRegisterConfig(
                                rbp,                 // frame
                                rax,                 // integral return value
                                xmm0,                // floating point return value
                                r11,                 // scratch
                                allocatable,         // allocatable
                                allocatable,         // caller save
                                parameters,          // parameter registers
                                EMPTY,               // no callee save
                                allRegisters,        // all AMD64 registers
                                roleMap);            // VM register role map

                return new RegisterConfigs(standard, n2j, trampoline, template, globalStub, trapStub);
            }
        }
        throw FatalError.unimplemented();
    }
}
