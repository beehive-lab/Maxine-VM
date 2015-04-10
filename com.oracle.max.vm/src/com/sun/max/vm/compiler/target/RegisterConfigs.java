/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.max.asm.target.amd64.AMD64.*;
import static com.sun.cri.ci.CiCallingConvention.Type.*;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.runtime.VMRegister.*;

import java.util.*;

import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.deopt.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.amd64.*;

/**
 * The set of register configurations applicable to compiled code in the VM.
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
     * The register configuration for a trampoline.
     * This configuration lists all parameter registers as callee saved.
     */
    public final CiRegisterConfig trampoline;

    /**
     * The register configuration for compiling the templates used by a template-based baseline compiler (e.g. T1X).
     */
    public final CiRegisterConfig bytecodeTemplate;

    /**
     * The register configuration for a {@link Stub.Type#CompilerStub compiler stub}.
     */
    public final CiRegisterConfig compilerStub;

    /**
     * The register configuration for the {@linkplain Stubs#trapStub trap stub}.
     */
    public final CiRegisterConfig trapStub;

    /**
     * The register configuration for the {@linkplain Stubs#genUncommonTrapStub() uncommon trap stub}.
     */
    public final CiRegisterConfig uncommonTrapStub;

    public CiRegisterConfig getRegisterConfig(ClassMethodActor method) {
        if (method.isVmEntryPoint()) {
            return n2j;
        }
        if (method.isTemplate()) {
            return bytecodeTemplate;
        }
        return standard;
    }

    @HOSTED_ONLY
    public RegisterConfigs(
                    CiRegisterConfig standard,
                    CiRegisterConfig n2j,
                    CiRegisterConfig trampoline,
                    CiRegisterConfig template,
                    CiRegisterConfig compilerStub,
                    CiRegisterConfig uncommonTrapStub,
                    CiRegisterConfig trapStub) {
        this.standard = standard;
        this.n2j = n2j;
        this.trampoline = trampoline;
        this.bytecodeTemplate = template;
        this.compilerStub = compilerStub;
        this.uncommonTrapStub = uncommonTrapStub;
        this.trapStub = trapStub;

        assert Arrays.equals(standard.getAllocatableRegisters(), standard.getCallerSaveRegisters()) : "VM requires caller-save for VM to VM calls";
    }

    @HOSTED_ONLY
    private static void setNonZero(RiRegisterAttributes[] attrMap, CiRegister... regs) {
        for (CiRegister reg : regs) {
            attrMap[reg.number].isNonZero = true;
        }
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

                // A call to the runtime may change the state of the safepoint latch
                // and so a compiler stub must leave the latch register alone
                CiRegister[] allRegistersExceptLatch = {
                    rax,  rcx,  rdx,   rbx,   rsp,   rbp,   rsi,   rdi,
                    r8,   r9,   r10,   r11,   r12,   r13, /*r14,*/ r15,
                    xmm0, xmm1, xmm2,  xmm3,  xmm4,  xmm5,  xmm6,  xmm7,
                    xmm8, xmm9, xmm10, xmm11, xmm12, xmm13, xmm14, xmm15
                };


                HashMap<Integer, CiRegister> roleMap = new HashMap<Integer, CiRegister>();
                roleMap.put(CPU_SP, rsp);
                roleMap.put(CPU_FP, rbp);
                roleMap.put(ABI_SP, rsp);
                roleMap.put(ABI_FP, rsp);
                roleMap.put(LATCH, r14);

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
                                null,                // no callee save
                                allRegisters,        // all AMD64 registers
                                roleMap);            // VM register role map

                // Account for the word at the bottom of the frame used
                // for saving an overwritten return address during deoptimization
                int javaStackArg0Offset = Deoptimization.DEOPT_RETURN_ADDRESS_OFFSET + Word.size();
                int nativeStackArg0Offset = 0;
                standard.stackArg0Offsets[JavaCall.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[JavaCallee.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[RuntimeCall.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[NativeCall.ordinal()] = nativeStackArg0Offset;

                setNonZero(standard.getAttributesMap(), r14, rsp);

                CiRegisterConfig compilerStub = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 8, allRegistersExceptLatch));
                CiRegisterConfig uncommonTrapStub = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 8, cpuxmmRegisters));
                CiRegisterConfig trapStub = new CiRegisterConfig(standard, AMD64TrapFrameAccess.CSL);
                CiRegisterConfig trampoline = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 8,
                    rdi, rsi, rdx, rcx, r8, r9,                       // parameters
                    rbp,                                              // must be preserved for baseline compiler
                    standard.getScratchRegister(),                    // dynamic dispatch index is saved here for stack frame walker
                    xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7    // parameters
                ));

                CiRegisterConfig n2j = new CiRegisterConfig(standard, new CiCalleeSaveLayout(Integer.MAX_VALUE, -1, 8, rbx, rbp, r12, r13, r14, r15));
                n2j.stackArg0Offsets[JavaCallee.ordinal()] = nativeStackArg0Offset;

                roleMap.put(ABI_FP, rbp);
                CiRegisterConfig template = new CiRegisterConfig(
                                rbp,                 // frame
                                rax,                 // integral return value
                                xmm0,                // floating point return value
                                r11,                 // scratch
                                allocatable,         // allocatable
                                allocatable,         // caller save
                                parameters,          // parameter registers
                                null,                // no callee save
                                allRegisters,        // all AMD64 registers
                                roleMap);            // VM register role map
                setNonZero(template.getAttributesMap(), r14, rsp, rbp);

                return new RegisterConfigs(standard, n2j, trampoline, template, compilerStub, uncommonTrapStub, trapStub);
            }
        }
        throw FatalError.unimplemented();
    }
}
