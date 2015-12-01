/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved. DO NOT ALTER OR REMOVE COPYRIGHT NOTICES
 * OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License version 2 for
 * more details (a copy is included in the LICENSE file that accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version 2 along with this work; if not, write to
 * the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA or visit www.oracle.com if you need
 * additional information or have any questions.
 */
package com.sun.max.vm.compiler.target;

import static com.oracle.max.asm.target.amd64.AMD64.*;
import static com.oracle.max.asm.target.armv7.ARMV7.*;
import static com.sun.cri.ci.CiCallingConvention.Type.*;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.runtime.VMRegister.*;

import java.util.*;

import com.oracle.max.asm.target.amd64.*;
import com.oracle.max.asm.target.armv7.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.deopt.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.amd64.*;
import com.sun.max.vm.runtime.arm.*;

/**
 * The set of register configurations applicable to compiled code in the VM.
 */
public class RegisterConfigs {

    /**
     * The register configuration for a normal Java method.
     */
    public final CiRegisterConfig standard;

    /**
     * The register configuration for a method called directly from native/C code. This configuration preserves all
     * native ABI specified callee saved registers.
     */
    public final CiRegisterConfig n2j;

    /**
     * The register configuration for a trampoline. This configuration lists all parameter registers as callee saved.
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
        if (method.isVmEntryPoint() || CompilationBroker.OFFLINE) {
            return n2j;
        }
        if (method.isTemplate()) {
            return bytecodeTemplate;
        }
        return standard;
    }

    @HOSTED_ONLY
    public RegisterConfigs(CiRegisterConfig standard, CiRegisterConfig n2j, CiRegisterConfig trampoline, CiRegisterConfig template, CiRegisterConfig compilerStub, CiRegisterConfig uncommonTrapStub,
                    CiRegisterConfig trapStub) {
        this.standard = standard;
        this.n2j = n2j;
        this.trampoline = trampoline;
        this.bytecodeTemplate = template;
        this.compilerStub = compilerStub;
        this.uncommonTrapStub = uncommonTrapStub;
        this.trapStub = trapStub;

        //assert Arrays.equals(standard.getAllocatableRegisters(), standard.getCallerSaveRegisters()) : "VM requires caller-save for VM to VM calls";
	// in ARM the callee save are different to the allocatable as it includes R14 and R10 R8,R9 which we use for
	// return address (must be for R14), safepoint R10, and scratch for longs R8,R9
    }

    @HOSTED_ONLY
    private static void setNonZero(RiRegisterAttributes[] attrMap, CiRegister... regs) {
        for (CiRegister reg : regs) {
            attrMap[reg.number].isNonZero = true;
        }
    }

    @HOSTED_ONLY
    public static RegisterConfigs create() {
        OS os = platform().os;

        CiRegister[] allocatable = null;
        CiRegister[] allocatableANDSpecialFloat = null;
        CiRegister[] parameters = null;
        CiRegister[] allRegistersExceptLatch = null;

        HashMap<Integer, CiRegister> roleMap = new HashMap<Integer, CiRegister>();
        CiRegisterConfig standard = null;

        /**
         * Input parameters: (General) r0-r3 (Floating Point) d0-d7
         * Frame pointer: r11 Stack pointer: r13
         * Return register: r14
         * Latch register: r10
         * Scratch registers: r8, r12, d15
         */
        if (platform().isa == ISA.ARM) {
            if (os == OS.LINUX || os == OS.DARWIN) {
		// floats are stored and restored as DOUBLEs ... so we use s0.s14 as allocatable  ... and s0..s15 as the full set.
                allocatableANDSpecialFloat = new CiRegister[] { r0, r1, r2, r3, r4, r5, r6, r7, ARMV7.r9, s0, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15};// comment out in case it causes stack problem
                allocatable = new CiRegister[] { r0, r1, r2, r3, r4, r5, r6, r7, ARMV7.r9, s0, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13  }; // we  use s28 s29 s30 s31 floats for scratch so we cannot allocate s14 asa double
                parameters = new CiRegister[] { r0, r1, r2, r3, s0, s1, s2, s3, s4, s5, s6, s7};
                allRegistersExceptLatch = new CiRegister[] { r0, r1, r2, r3, r4, r5, r6, r7, com.oracle.max.asm.target.armv7.ARMV7.r8, com.oracle.max.asm.target.armv7.ARMV7.r9,
                                com.oracle.max.asm.target.armv7.ARMV7.r11, com.oracle.max.asm.target.armv7.ARMV7.r12, com.oracle.max.asm.target.armv7.ARMV7.r13,
                                com.oracle.max.asm.target.armv7.ARMV7.r14, s0, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15};
                roleMap.put(CPU_SP, com.oracle.max.asm.target.armv7.ARMV7.r13);
                roleMap.put(CPU_FP, com.oracle.max.asm.target.armv7.ARMV7.r11);
                roleMap.put(ABI_SP, com.oracle.max.asm.target.armv7.ARMV7.r13);
                roleMap.put(ABI_FP, com.oracle.max.asm.target.armv7.ARMV7.r13);
                roleMap.put(LATCH, com.oracle.max.asm.target.armv7.ARMV7.r10);

                standard = new CiRegisterConfig(com.oracle.max.asm.target.armv7.ARMV7.r13, r0, s0, com.oracle.max.asm.target.armv7.ARMV7.r12, allocatable, allocatable/*ANDSpecialFloat*/, parameters, null,
                                com.oracle.max.asm.target.armv7.ARMV7.allRegisters, roleMap);

                // Account for the word at the bottom of the frame used
                // for saving an overwritten return address during deoptimization
                int javaStackArg0Offset = Deoptimization.DEOPT_RETURN_ADDRESS_OFFSET + Word.size();
                int nativeStackArg0Offset = 0;
                standard.stackArg0Offsets[JavaCall.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[JavaCallee.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[RuntimeCall.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[NativeCall.ordinal()] = nativeStackArg0Offset;
                setNonZero(standard.getAttributesMap(), com.oracle.max.asm.target.armv7.ARMV7.r10, com.oracle.max.asm.target.armv7.ARMV7.r13);
                CiRegisterConfig compilerStub = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 4, allRegistersExceptLatch));
                CiRegisterConfig uncommonTrapStub = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 4, com.oracle.max.asm.target.armv7.ARMV7.cpuxmmRegisters));
                CiRegisterConfig trapStub = new CiRegisterConfig(standard, ARMTrapFrameAccess.CSL);

		CiRegisterConfig trampoline = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 4, r0, r1, r2, r3, com.oracle.max.asm.target.armv7.ARMV7.r8,
			/*com.oracle.max.asm.target.armv7.ARMV7.r9,*/ com.oracle.max.asm.target.armv7.ARMV7.r11, /*standard.getScratchRegister(),
			ARMV7.r14,*/ ARMV7.r14, ARMV7.s0, ARMV7.s1, ARMV7.s2, ARMV7.s3, ARMV7.s4, ARMV7.s5, ARMV7.s6, ARMV7.s7, ARMV7.s8, ARMV7.s9, ARMV7.s10, 
                       ARMV7.s11, ARMV7.s12, ARMV7.s13, ARMV7.s14, ARMV7.s15, com.oracle.max.asm.target.armv7.ARMV7.r12 )); 
			// r12 is unecessary, but the idea is that we canuse this to save the return address from the resolveVirtual/InterfaceCall in the slot for r12
			// that we then  call
                CiRegisterConfig n2j = new CiRegisterConfig(standard, new CiCalleeSaveLayout(Integer.MAX_VALUE, -1, 4, r4, r5, r6, r7, com.oracle.max.asm.target.armv7.ARMV7.r8, ARMV7.s28, ARMV7.s29, ARMV7.s30, ARMV7.s31,
                                com.oracle.max.asm.target.armv7.ARMV7.r9, com.oracle.max.asm.target.armv7.ARMV7.r10, com.oracle.max.asm.target.armv7.ARMV7.r11));

                n2j.stackArg0Offsets[JavaCallee.ordinal()] = nativeStackArg0Offset;
                roleMap.put(ABI_FP, com.oracle.max.asm.target.armv7.ARMV7.r11);
                CiRegisterConfig template = new CiRegisterConfig(com.oracle.max.asm.target.armv7.ARMV7.r11, r0, s0, com.oracle.max.asm.target.armv7.ARMV7.r12, allocatable, allocatable/*ANDSpecialFloat*/, parameters,
                                null, com.oracle.max.asm.target.armv7.ARMV7.allRegisters, roleMap);

                setNonZero(template.getAttributesMap(), com.oracle.max.asm.target.armv7.ARMV7.r10, com.oracle.max.asm.target.armv7.ARMV7.r13, com.oracle.max.asm.target.armv7.ARMV7.r11);
                return new RegisterConfigs(standard, n2j, trampoline, template, compilerStub, uncommonTrapStub, trapStub);
            }
        } else if (platform().isa == ISA.AMD64) {
            if (os == OS.LINUX || os == OS.SOLARIS || os == OS.DARWIN || os == OS.MAXVE) {
                /**
                 * The set of allocatable registers shared by most register configurations.
                 */
                allocatable = new CiRegister[] {rax, rcx, rdx, rbx, rsi, rdi, com.oracle.max.asm.target.amd64.AMD64.r8, com.oracle.max.asm.target.amd64.AMD64.r9,
                    com.oracle.max.asm.target.amd64.AMD64.r10, com.oracle.max.asm.target.amd64.AMD64.r12, com.oracle.max.asm.target.amd64.AMD64.r13,
                    com.oracle.max.asm.target.amd64.AMD64.r15, xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7, xmm8, xmm9, xmm10, xmm11, xmm12, xmm13, xmm14, xmm15};

                parameters = new CiRegister[] {rdi, rsi, rdx, rcx, com.oracle.max.asm.target.amd64.AMD64.r8, com.oracle.max.asm.target.amd64.AMD64.r9, xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7};

                // A call to the runtime may change the state of the safepoint latch
                // and so a compiler stub must leave the latch register alone
                allRegistersExceptLatch = new CiRegister[] {rax, rcx, rdx, rbx, AMD64.rsp, rbp, rsi, rdi, com.oracle.max.asm.target.amd64.AMD64.r8, com.oracle.max.asm.target.amd64.AMD64.r9,
                    com.oracle.max.asm.target.amd64.AMD64.r10, com.oracle.max.asm.target.amd64.AMD64.r11, com.oracle.max.asm.target.amd64.AMD64.r12,
                    com.oracle.max.asm.target.amd64.AMD64.r13, /* r14, */
                    com.oracle.max.asm.target.amd64.AMD64.r15, xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7, xmm8, xmm9, xmm10, xmm11, xmm12, xmm13, xmm14, xmm15};

                roleMap.put(CPU_SP, AMD64.rsp);
                roleMap.put(CPU_FP, rbp);
                roleMap.put(ABI_SP, AMD64.rsp);
                roleMap.put(ABI_FP, AMD64.rsp);
                roleMap.put(LATCH, com.oracle.max.asm.target.amd64.AMD64.r14);

                /**
                 * The register configuration for a normal Java method. This configuration specifies <b>all</b>
                 * allocatable registers as caller-saved as inlining is expected to reduce the call overhead
                 * sufficiently.
                 */
                standard = new CiRegisterConfig(AMD64.rsp, // frame
                    rax, // integral return value
                    xmm0, // floating point return value
                    com.oracle.max.asm.target.amd64.AMD64.r11, // scratch
                    allocatable, // allocatable
                    allocatable, // caller save
                    parameters, // parameter registers
                    null, // no callee save
                    com.oracle.max.asm.target.amd64.AMD64.allRegisters, // all AMD64 registers
                    roleMap); // VM register role map

                // Account for the word at the bottom of the frame used
                // for saving an overwritten return address during deoptimization
                int javaStackArg0Offset = Deoptimization.DEOPT_RETURN_ADDRESS_OFFSET + Word.size();
                int nativeStackArg0Offset = 0;
                standard.stackArg0Offsets[JavaCall.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[JavaCallee.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[RuntimeCall.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[NativeCall.ordinal()] = nativeStackArg0Offset;

                setNonZero(standard.getAttributesMap(), com.oracle.max.asm.target.amd64.AMD64.r14, AMD64.rsp);

                CiRegisterConfig compilerStub = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 8, allRegistersExceptLatch));
                CiRegisterConfig uncommonTrapStub = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 8,
		com.oracle.max.asm.target.amd64.AMD64.cpuxmmRegisters));
                CiRegisterConfig trapStub  = new CiRegisterConfig(standard, AMD64TrapFrameAccess.CSL);
                CiRegisterConfig trampoline = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 8, rdi, rsi, rdx, rcx, com.oracle.max.asm.target.amd64.AMD64.r8,
                    com.oracle.max.asm.target.amd64.AMD64.r9, // parameters
                    rbp, // must be preserved for baseline compiler
                    standard.getScratchRegister(), // dynamic dispatch index is saved here for stack frame walker
                    xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7)); // parameters

                CiRegisterConfig n2j = new CiRegisterConfig(standard, new CiCalleeSaveLayout(Integer.MAX_VALUE, -1, 8, rbx, rbp, com.oracle.max.asm.target.amd64.AMD64.r12,
                    com.oracle.max.asm.target.amd64.AMD64.r13, com.oracle.max.asm.target.amd64.AMD64.r14, com.oracle.max.asm.target.amd64.AMD64.r15));
                n2j.stackArg0Offsets[JavaCallee.ordinal()] = nativeStackArg0Offset;

                roleMap.put(ABI_FP, rbp);
                CiRegisterConfig template = new CiRegisterConfig(rbp, // frame
                    rax, // integral return value
                    xmm0, // floating point return value
                    com.oracle.max.asm.target.amd64.AMD64.r11, // scratch
                    allocatable, // allocatable
                    allocatable, // caller save
                    parameters, // parameter registers
                    null, // no callee save
                    com.oracle.max.asm.target.amd64.AMD64.allRegisters, // all AMD64 registers
                    roleMap); // VM register role map
                setNonZero(template.getAttributesMap(), com.oracle.max.asm.target.amd64.AMD64.r14, AMD64.rsp, rbp);

                return new RegisterConfigs(standard, n2j, trampoline, template, compilerStub, uncommonTrapStub, trapStub);
            }
        } else {
            throw FatalError.unimplemented();
        }
        return null;
    }
}
