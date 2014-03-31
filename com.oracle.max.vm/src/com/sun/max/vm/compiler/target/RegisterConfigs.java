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

import com.oracle.max.asm.target.armv7.ARMV7;
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
        if (method.isVmEntryPoint()) {
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
        OS os = platform().os;

        CiRegister[] allocatable = null;
        CiRegister[] parameters = null;
        CiRegister[] allRegistersExceptLatch = null;

        HashMap<Integer, CiRegister> roleMap = new HashMap<Integer, CiRegister>();
        CiRegisterConfig standard = null;

        if (platform().isa == ISA.ARM) {
            if (os == OS.LINUX || os == OS.DARWIN) {
                /*
                 * The set of allocatable registers etc .... APN I hope this is correct Yaman would you like to check
                 * this. APN ... ARM Assembly Language:Fundamentals & Techiques states r0..r3 used for argument value
                 * passing and to return a result value from a function r12 might be used by a linker as a register
                 * between a routine and any subroutine it calls. r4..r8, r10 and r11 are used to hold the values of a
                 * routine's local variables ONLY r4..r7 can be used uniformly by THUMB. subroutine must preserve r4.r8,
                 * r10 and r11 and SP (r9 as well sometimes).
                 *
                 * LATCH chosen to be r10 FP chosen to be r11
                 */
                allocatable = new CiRegister[] { r0, r1, r2, r3, r4, r5, r6, r7, com.oracle.max.asm.target.armv7.ARMV7.r8, com.oracle.max.asm.target.armv7.ARMV7.r9,
                                com.oracle.max.asm.target.armv7.ARMV7.r10, d0, d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15, s0, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12,
                                s13, s14, s15, s16, s17, s18, s19, s20, s21, s22, s23, s24, s25, s26, s27, s28, s29}; // no
                // scratch in allocatable
                parameters = new CiRegister[] { r0, r1, r2, r3, d0, d1, d2, d3, s0, s1, s2, s3};
                allRegistersExceptLatch = new CiRegister[] { r0, r1, r2, r3, r4, r5, r6, r7, com.oracle.max.asm.target.armv7.ARMV7.r8, com.oracle.max.asm.target.armv7.ARMV7.r9,
                                com.oracle.max.asm.target.armv7.ARMV7.r11, com.oracle.max.asm.target.armv7.ARMV7.r12, com.oracle.max.asm.target.armv7.ARMV7.r13,
                                com.oracle.max.asm.target.armv7.ARMV7.r14, com.oracle.max.asm.target.armv7.ARMV7.r15, d0, d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15, s0, s1, s2,
                                s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, s16, s17, s18, s19, s20, s21, s22, s23, s24, s25, s26, s27, s28, s29, s30, s31};
                roleMap.put(CPU_SP, com.oracle.max.asm.target.armv7.ARMV7.r13);
                roleMap.put(CPU_FP, com.oracle.max.asm.target.armv7.ARMV7.r11); // TODO CHECK
                roleMap.put(ABI_SP, com.oracle.max.asm.target.armv7.ARMV7.r13);
                roleMap.put(ABI_FP, com.oracle.max.asm.target.armv7.ARMV7.r13);
                //ARM PCS suggest that r11 is used as a frame pointer, not sure how this is set up for each new java frame


                roleMap.put(LATCH, com.oracle.max.asm.target.armv7.ARMV7.r10);
                /*
                 * the LATCH register is a callee saved register associated with
                 * com.sun.max.vm.runtime.armv7.ARMV7Safepoint APN -thinks!!!!
                 *
                 * * The register configuration for a normal Java method. This configuration specifies <b>all</b>
                 * allocatable registers as caller-saved
                 *
                 *
                 * as inlining is expected to reduce the call overhead sufficiently.
                 */
                standard = new CiRegisterConfig(com.oracle.max.asm.target.armv7.ARMV7.r13, // frame
                                r0, // integral return value
                                d0, // APN TODO floating point return value for simplicity ALWAYS return a double

                                // TODO this means we must do the conversion as necessary if it is really a float?
                                // TODO it might be better to handle this another way
                                com.oracle.max.asm.target.armv7.ARMV7.r12, // scratch
                                allocatable, // allocatable
                                allocatable, // caller save *** TODO APN-- not sure we might need a
                                             // different set for ARM HERE!!!!!
                                parameters, // parameter registers
                                null, // no callee save
                                com.oracle.max.asm.target.armv7.ARMV7.allRegisters, // all ARM registers
                                roleMap); // VM register role map

                // Account for the word at the bottom of the frame used
                // for saving an overwritten return address during deoptimization
                int javaStackArg0Offset = Deoptimization.DEOPT_RETURN_ADDRESS_OFFSET + Word.size();
                int nativeStackArg0Offset = 0;
                standard.stackArg0Offsets[JavaCall.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[JavaCallee.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[RuntimeCall.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[NativeCall.ordinal()] = nativeStackArg0Offset;

                setNonZero(standard.getAttributesMap(), com.oracle.max.asm.target.armv7.ARMV7.r11, com.oracle.max.asm.target.armv7.ARMV7.r13); /*
                                                                                                                                                * APN
                /*
                 * APN I don't get why we need in the x86 version cpuxmmRegisters and all registers perhaps this will
                 * become clearer when we delve into the stubs below, I substituted allRegisters for cpuxmmRegisters for
                 * uncommonTrapStub
                 */
                CiRegisterConfig compilerStub = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 4, allRegistersExceptLatch));
                CiRegisterConfig uncommonTrapStub = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 4, com.oracle.max.asm.target.armv7.ARMV7.allRegisters));
                CiRegisterConfig trapStub = new CiRegisterConfig(standard, ARMTrapFrameAccess.CSL);

                // CSL stands for CALLEE SAVE LAYOUT ... needs to be rewritten in com.sun.man.vm.runtime.arm.ARMTrapFrameAccess or similar
                /*
                 * APN not sure about the trampoline config ... ok its specifying a CiCalleeSaveLayout, where the -1 for
                 * SIZE ... 2nd arg indicates that the this.size of the constructed object is equal to an offset that is
                 * calculated inside the constructor, not unsurprising as it has a varags for the number of registers to
                 * be saved. We will be conservative and save everything we should based on the APC ARM Procedure Call
                 * Standard. Once we know what is actually required for trampoline then we can be more
                 * aggressive/specific about the registers to be saved?
                 */
                CiRegisterConfig trampoline = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 4, ARMV7.r0, ARMV7.r1, ARMV7.r2,
                                ARMV7.r3, // parameters
                                r4, r5, r6, r7, com.oracle.max.asm.target.armv7.ARMV7.r8, com.oracle.max.asm.target.armv7.ARMV7.r9, com.oracle.max.asm.target.armv7.ARMV7.r10,
                                com.oracle.max.asm.target.armv7.ARMV7.r11, // r4..r11? must be preserved for baseline compiler
                                standard.getScratchRegister())); // dynamic dispatch index is saved here for stack frame walker
                                                                // parameters APN lets not worry about floating point .... lets crack out the StollyBolly once we get HelloWorld working

                // the registers below are a guess in n2j ....
                // ....
                CiRegisterConfig n2j = new CiRegisterConfig(standard, new CiCalleeSaveLayout(Integer.MAX_VALUE, -1, 4, r0, r1, r2, r3, r4, r5, r6, r7, com.oracle.max.asm.target.armv7.ARMV7.r8,
                                com.oracle.max.asm.target.armv7.ARMV7.r9, com.oracle.max.asm.target.armv7.ARMV7.r10, com.oracle.max.asm.target.armv7.ARMV7.r11,
                                com.oracle.max.asm.target.armv7.ARMV7.r12, com.oracle.max.asm.target.armv7.ARMV7.r13));
                n2j.stackArg0Offsets[JavaCallee.ordinal()] = nativeStackArg0Offset;

                roleMap.put(ABI_FP, com.oracle.max.asm.target.armv7.ARMV7.r11);
                CiRegisterConfig template = new CiRegisterConfig(com.oracle.max.asm.target.armv7.ARMV7.r11, // frame
                                r0, // integral return value
                                d0, // floating point return value
                                com.oracle.max.asm.target.armv7.ARMV7.r12, // scratch
                                allocatable, // allocatable
                                allocatable, // caller save
                                parameters, // parameter registers
                                null, // no callee save
                                com.oracle.max.asm.target.armv7.ARMV7.allRegisters, // all ARM!!! registers
                                roleMap); // VM register role map
                setNonZero(template.getAttributesMap(), com.oracle.max.asm.target.armv7.ARMV7.r11, com.oracle.max.asm.target.armv7.ARMV7.r13, com.oracle.max.asm.target.armv7.ARMV7.r12);

                /*
                 * APN this is really all a bit of a hack/guess in this file --- need to know more about the
                 * interworkings in order to determine if what we're doing is sensible Key issues have we got the
                 * register roles right. X86 is more complex than ARM and it does stacks differently so we probably need
                 * to learn a little about X86 stacks and frame management in order to recast the code and the classes
                 * correctly for ARM.
                 */
                return new RegisterConfigs(standard, n2j, trampoline, template, compilerStub, uncommonTrapStub, trapStub);
            }
        } else if (platform().isa == ISA.AMD64) {
            if (os == OS.LINUX || os == OS.SOLARIS || os == OS.DARWIN || os == OS.MAXVE) {
                /**
                 * The set of allocatable registers shared by most register configurations.
                 */
                allocatable = new CiRegister[] { rax, rcx, rdx, rbx, rsi, rdi, com.oracle.max.asm.target.amd64.AMD64.r8, com.oracle.max.asm.target.amd64.AMD64.r9,
                                com.oracle.max.asm.target.amd64.AMD64.r10, com.oracle.max.asm.target.amd64.AMD64.r12, com.oracle.max.asm.target.amd64.AMD64.r13,
                                com.oracle.max.asm.target.amd64.AMD64.r15, xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7, xmm8, xmm9, xmm10, xmm11, xmm12, xmm13, xmm14, xmm15};

                parameters = new CiRegister[] { rdi, rsi, rdx, rcx, com.oracle.max.asm.target.amd64.AMD64.r8, com.oracle.max.asm.target.amd64.AMD64.r9, xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7};

                // A call to the runtime may change the state of the safepoint latch
                // and so a compiler stub must leave the latch register alone
                allRegistersExceptLatch = new CiRegister[] { rax, rcx, rdx, rbx, rsp, rbp, rsi, rdi, com.oracle.max.asm.target.amd64.AMD64.r8, com.oracle.max.asm.target.amd64.AMD64.r9,
                                com.oracle.max.asm.target.amd64.AMD64.r10, com.oracle.max.asm.target.amd64.AMD64.r11, com.oracle.max.asm.target.amd64.AMD64.r12,
                                com.oracle.max.asm.target.amd64.AMD64.r13, /* r14, */
                                com.oracle.max.asm.target.amd64.AMD64.r15, xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7, xmm8, xmm9, xmm10, xmm11, xmm12, xmm13, xmm14, xmm15};

                roleMap.put(CPU_SP, rsp);
                roleMap.put(CPU_FP, rbp);
                roleMap.put(ABI_SP, rsp);
                roleMap.put(ABI_FP, rsp);
                roleMap.put(LATCH, com.oracle.max.asm.target.amd64.AMD64.r14);

                /**
                 * The register configuration for a normal Java method. This configuration specifies <b>all</b>
                 * allocatable registers as caller-saved as inlining is expected to reduce the call overhead
                 * sufficiently.
                 */
                standard = new CiRegisterConfig(rsp, // frame
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

                setNonZero(standard.getAttributesMap(), com.oracle.max.asm.target.amd64.AMD64.r14, rsp);

                CiRegisterConfig compilerStub = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 8, allRegistersExceptLatch));
                CiRegisterConfig uncommonTrapStub = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 8, cpuxmmRegisters));
                CiRegisterConfig trapStub  = new CiRegisterConfig(standard, AMD64TrapFrameAccess.CSL);
                CiRegisterConfig trampoline = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 8, rdi, rsi, rdx, rcx, com.oracle.max.asm.target.amd64.AMD64.r8,
                                com.oracle.max.asm.target.amd64.AMD64.r9, // parameters
                                rbp, // must be preserved for baseline compiler
                                standard.getScratchRegister(), // dynamic dispatch index is saved here for stack frame walker
                                xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7 // parameters
                                ));

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
                setNonZero(template.getAttributesMap(), com.oracle.max.asm.target.amd64.AMD64.r14, rsp, rbp);

                return new RegisterConfigs(standard, n2j, trampoline, template, compilerStub, uncommonTrapStub, trapStub);
            }
        } else
            throw FatalError.unimplemented();
        return null;
    }
}
