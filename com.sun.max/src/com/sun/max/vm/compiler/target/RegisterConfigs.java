/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
package com.sun.max.vm.compiler.target;

import static com.oracle.max.asm.target.aarch64.Aarch64.calleeSavedRegisters;
import static com.oracle.max.asm.target.aarch64.Aarch64.csaRegisters;
import static com.oracle.max.asm.target.amd64.AMD64.*;
import static com.oracle.max.asm.target.armv7.ARMV7.*;
import static com.sun.cri.ci.CiCallingConvention.Type.*;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.runtime.VMRegister.*;

import java.util.*;

import com.oracle.max.asm.target.aarch64.*;
import com.oracle.max.asm.target.amd64.*;
import com.oracle.max.asm.target.armv7.*;
import com.oracle.max.asm.target.riscv64.RISCV64;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.deopt.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.aarch64.Aarch64TrapFrameAccess;
import com.sun.max.vm.runtime.amd64.*;
import com.sun.max.vm.runtime.arm.*;
import com.sun.max.vm.runtime.riscv64.RISCV64TrapFrameAccess;

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
        if (method.isVmEntryPoint() || vm().compilationBroker.isOffline()) {
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

        // In ARM the callee save are different to the allocatable since it includes R14 which we use for
        // return address, R10 for safepoint , and R8,R9 scratch registers.
        if (platform().isa != ISA.ARM) {
            assert Arrays.equals(standard.getAllocatableRegisters(), standard.getCallerSaveRegisters()) : "VM requires caller-save for VM to VM calls";
        }
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

        /**
         * Input parameters: (General) r0-r3 (Floating Point) d0-d7 Frame pointer: r11 Stack pointer: r13 Return
         * register: r14 Latch register: r10 Scratch registers: r8, r12, d15
         */
        if (platform().isa == ISA.ARM) {
            if (os == OS.LINUX || os == OS.DARWIN) {
                allocatable = new CiRegister[] {r0, r1, r2, r3, r4, r5, r6, r7, ARMV7.r9, s0, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, s16, s17, s18, s19, s20, s21, s22, s23,
                                                s24, s25, s26, s27};
                // In accordance to the ARMv7 AAPCS r0-r3:
                // The first four registers r0-r3 (a1-a4) are used to pass argument values into a subroutine and to
                // return a result value from a function.
                // registers s0-s15 (d0-d7, q0-q3) do not need to be preserved (and can be used for passing arguments or
                // returning results in standard procedure-call variants)
                parameters = new CiRegister[] {r0, r1, r2, r3, s0, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15};
                allRegistersExceptLatch = new CiRegister[] {r0, r1, r2, r3, r4, r5, r6, r7, ARMV7.r8, ARMV7.r9, ARMV7.r11, ARMV7.r12, ARMV7.r13, ARMV7.r14, s0, s1, s2, s3, s4, s5, s6, s7, s8, s9,
                                                            s10, s11, s12, s13, s14, s15, s16, s17, s18, s19, s20, s21, s22, s23, s24, s25, s26, s27, s28, s29, s30, s31};
                roleMap.put(CPU_SP, ARMV7.r13);
                roleMap.put(CPU_FP, ARMV7.r11);
                roleMap.put(ABI_SP, ARMV7.r13);
                roleMap.put(ABI_FP, ARMV7.r13);
                roleMap.put(LATCH, ARMV7.r10);

                standard = new CiRegisterConfig(ARMV7.r13, r0, s0, ARMV7.r12, ARMV7.r8, allocatable, allocatable, parameters, null, ARMV7.allRegisters, roleMap);

                // Account for the word at the bottom of the frame used
                // for saving an overwritten return address during deoptimization
                int javaStackArg0Offset = Deoptimization.DEOPT_RETURN_ADDRESS_OFFSET + Word.size();
                int nativeStackArg0Offset = 0;
                standard.stackArg0Offsets[JavaCall.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[JavaCallee.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[RuntimeCall.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[NativeCall.ordinal()] = nativeStackArg0Offset;
                setNonZero(standard.getAttributesMap(), ARMV7.r10, ARMV7.r13);
                CiRegisterConfig compilerStub = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 4, allRegistersExceptLatch));
                CiRegisterConfig uncommonTrapStub = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 4, ARMV7.cpuxmmRegisters));
                CiRegisterConfig trapStub = new CiRegisterConfig(standard, ARMTrapFrameAccess.CSL);

                CiRegisterConfig trampoline = new CiRegisterConfig(standard,
                                new CiCalleeSaveLayout(0, -1, 4, r0, r1, r2, r3, ARMV7.r8, ARMV7.s0, ARMV7.s1, ARMV7.s2, ARMV7.s3, ARMV7.s4, ARMV7.s5, ARMV7.s6, ARMV7.s7));
                // r12 is unecessary, but the idea is that we canuse this to save the return address from the
                // resolveVirtual/InterfaceCall in the slot for r12
                // that we then call
                CiRegisterConfig n2j = new CiRegisterConfig(standard,
                                new CiCalleeSaveLayout(Integer.MAX_VALUE, -1, 4, r4, r5, r6, r7, ARMV7.r8, ARMV7.s13, ARMV7.s14, ARMV7.s15, ARMV7.r9, ARMV7.r10, ARMV7.r11));

                n2j.stackArg0Offsets[JavaCallee.ordinal()] = nativeStackArg0Offset;
                roleMap.put(ABI_FP, ARMV7.r11);
                CiRegisterConfig template = new CiRegisterConfig(ARMV7.r11, r0, s0, ARMV7.r12, ARMV7.r8, allocatable, allocatable, parameters, null, ARMV7.allRegisters, roleMap);

                setNonZero(template.getAttributesMap(), ARMV7.r10, ARMV7.r13, ARMV7.r11);
                return new RegisterConfigs(standard, n2j, trampoline, template, compilerStub, uncommonTrapStub, trapStub);
            }
        } else if (platform().isa == ISA.AMD64) {
            if (os == OS.LINUX || os == OS.SOLARIS || os == OS.DARWIN || os == OS.MAXVE) {
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
                standard = new CiRegisterConfig(
                                AMD64.rsp,           // frame
                                rax,                 // integral return value
                                xmm0,                // floating point return value
                                AMD64.r11,           // scratch
                                null,                // scratch 1
                                allocatable,         // allocatable
                                allocatable,         // caller save
                                parameters,          // parameter registers
                                null,                // no callee save
                                AMD64.allRegisters,  // all AMD64 registers
                                roleMap);            // VM register role map

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
                CiRegisterConfig uncommonTrapStub = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 8, AMD64.cpuxmmRegisters));
                CiRegisterConfig trapStub = new CiRegisterConfig(standard, AMD64TrapFrameAccess.CSL);
                CiRegisterConfig trampoline = new CiRegisterConfig(standard,
                                new CiCalleeSaveLayout(0, -1, 8, rdi, rsi, rdx, rcx, com.oracle.max.asm.target.amd64.AMD64.r8, com.oracle.max.asm.target.amd64.AMD64.r9, // parameters
                                                rbp, // must be preserved for baseline compiler
                                                standard.getScratchRegister(), // dynamic dispatch index is saved here
                                                                               // for stack frame walker
                                                xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7)); // parameters

                CiRegisterConfig n2j = new CiRegisterConfig(standard, new CiCalleeSaveLayout(Integer.MAX_VALUE, -1, 8, rbx, rbp, com.oracle.max.asm.target.amd64.AMD64.r12,
                                com.oracle.max.asm.target.amd64.AMD64.r13, com.oracle.max.asm.target.amd64.AMD64.r14, com.oracle.max.asm.target.amd64.AMD64.r15));
                n2j.stackArg0Offsets[JavaCallee.ordinal()] = nativeStackArg0Offset;

                roleMap.put(ABI_FP, rbp);
                CiRegisterConfig template = new CiRegisterConfig(
                                rbp,                 // frame
                                rax,                 // integral return value
                                xmm0,                // floating point return value
                                AMD64.r11,           // scratch
                                null,                // scratch 1
                                allocatable,         // allocatable
                                allocatable,         // caller save
                                parameters,          // parameter registers
                                null,                // no callee save
                                AMD64.allRegisters,  // all AMD64 registers
                                roleMap);            // VM register role map
                setNonZero(template.getAttributesMap(), AMD64.r14, AMD64.rsp, rbp);

                return new RegisterConfigs(standard, n2j, trampoline, template, compilerStub, uncommonTrapStub, trapStub);
            }
        } else if (platform().isa == ISA.Aarch64) {
            if (os == OS.LINUX) {

                /**
                 * The set of allocatable registers shared by most register configurations.
                 * according to
                 * "voo-graal/graal/src/cpu/aarch64/vm/register_definitions_aarch64.cpp"
                 * and
                 * "voo-graal/graal/graal/com.oracle.graal.hotspot.armv8/src/com/oracle/graal/hotspot/armv8/ARMv8HotSpotRegisterConfig.java"
                 */
                allocatable = new CiRegister[] {
                    Aarch64.r0,  Aarch64.r1,  Aarch64.r2,  Aarch64.r3,  Aarch64.r4,  Aarch64.r5,  Aarch64.r6,  Aarch64.r7,
                    Aarch64.r8,  Aarch64.r9,  Aarch64.r10, Aarch64.r11, Aarch64.r12, Aarch64.r13, Aarch64.r14, Aarch64.r15,
                    /*Aarch64.r16 : scratch, Aarch64.r17 : scratch2,*/ Aarch64.r18,
                    Aarch64.r19, Aarch64.r20, Aarch64.r21, Aarch64.r22, Aarch64.r23,
                    Aarch64.r24, Aarch64.r25,
                    //r26:latch register, r27:heapBaseRegister, r28:threadRegister, r29:fp(framePointer), r30:linkRegister
                    /*Aarch64.r26, Aarch64.r27, Aarch64.r28, Aarch64.r29, Aarch64.r30,*/
                    //r31:sp||zr
                    /*Aarch64.r31, Aarch64.sp,  Aarch64.zr,*/
                    Aarch64.d0,  Aarch64.d1,  Aarch64.d2,  Aarch64.d3,  Aarch64.d4,  Aarch64.d5,  Aarch64.d6,  Aarch64.d7,
                    Aarch64.d8,  Aarch64.d9,  Aarch64.d10, Aarch64.d11, Aarch64.d12, Aarch64.d13, Aarch64.d14, Aarch64.d15,
                    Aarch64.d16, Aarch64.d17, Aarch64.d18, Aarch64.d19, Aarch64.d20, Aarch64.d21, Aarch64.d22, Aarch64.d23,
                    Aarch64.d24, Aarch64.d25, Aarch64.d26, Aarch64.d27, Aarch64.d28, Aarch64.d29, Aarch64.d30, Aarch64.d31
                };
                parameters = new CiRegister[] {
                    Aarch64.r0, Aarch64.r1, Aarch64.r2, Aarch64.r3,
                    Aarch64.r4, Aarch64.r5, Aarch64.r6, Aarch64.r7,
                    Aarch64.d0, Aarch64.d1, Aarch64.d2, Aarch64.d3,
                    Aarch64.d4, Aarch64.d5, Aarch64.d6, Aarch64.d7
                };
                // A call to the runtime may change the state of the safepoint latch
                // and so a compiler stub must leave the latch register alone
                allRegistersExceptLatch = new CiRegister[] {
                    Aarch64.r0,  Aarch64.r1,  Aarch64.r2,  Aarch64.r3,  Aarch64.r4,  Aarch64.r5,  Aarch64.r6,  Aarch64.r7,
                    Aarch64.r8,  Aarch64.r9,  Aarch64.r10, Aarch64.r11, Aarch64.r12, Aarch64.r13, Aarch64.r14, Aarch64.r15,
                    Aarch64.r16, Aarch64.r17, Aarch64.r18,
                    Aarch64.r19, Aarch64.r20, Aarch64.r21, Aarch64.r22, Aarch64.r23,
                    Aarch64.r24, Aarch64.r25,
                    // r26 is defined as latch register
                    /*Aarch64.r26,*/
                    //r27:heapBaseRegister, r28:threadRegister, r29:fp(framePointer), r30:linkRegister
                    Aarch64.r27, Aarch64.r28, Aarch64.r29, Aarch64.r30,
                    //r31:sp||zr
                    //Aarch64.r31, Aarch64.sp,  Aarch64.zr,
                    Aarch64.d0,  Aarch64.d1,  Aarch64.d2,  Aarch64.d3,  Aarch64.d4,  Aarch64.d5,  Aarch64.d6,  Aarch64.d7,
                    Aarch64.d8,  Aarch64.d9,  Aarch64.d10, Aarch64.d11, Aarch64.d12, Aarch64.d13, Aarch64.d14, Aarch64.d15,
                    Aarch64.d16, Aarch64.d17, Aarch64.d18, Aarch64.d19, Aarch64.d20, Aarch64.d21, Aarch64.d22, Aarch64.d23,
                    Aarch64.d24, Aarch64.d25, Aarch64.d26, Aarch64.d27, Aarch64.d28, Aarch64.d29, Aarch64.d30, Aarch64.d31
                };

                roleMap.put(CPU_SP, Aarch64.sp);
                roleMap.put(CPU_FP, Aarch64.fp);
                roleMap.put(ABI_SP, Aarch64.sp);
                roleMap.put(ABI_FP, Aarch64.fp);
                roleMap.put(LATCH, Aarch64.LATCH_REGISTER);

                /**
                 * The register configuration for a normal Java method.
                 * This configuration specifies <b>all</b> allocatable registers as caller-saved
                 * as inlining is expected to reduce the call overhead sufficiently.
                 */
                standard = new CiRegisterConfig(
                                Aarch64.sp,          // frame
                                Aarch64.r0,          // integral return value
                                Aarch64.d0,          // floating point return value
                                Aarch64.r16,         // scratch
                                Aarch64.r17,         // scratch 1
                                allocatable,         // allocatable
                                allocatable,         // caller save
                                parameters,          // parameter registers
                                null,                // no callee save
                                Aarch64.allRegisters, // all AMD64 registers
                                roleMap);            // VM register role map


                // Account for the word at the bottom of the frame used
                // for saving an overwritten return address during deoptimization
                int javaStackArg0Offset = Deoptimization.DEOPT_RETURN_ADDRESS_OFFSET + Word.size();
                int nativeStackArg0Offset = 0;
                standard.stackArg0Offsets[JavaCall.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[JavaCallee.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[RuntimeCall.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[NativeCall.ordinal()] = nativeStackArg0Offset;

                setNonZero(standard.getAttributesMap(), Aarch64.LATCH_REGISTER, Aarch64.sp, Aarch64.fp);

                CiRegisterConfig compilerStub = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 8, allRegistersExceptLatch));
                CiRegisterConfig uncommonTrapStub = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 8, csaRegisters));
                CiRegisterConfig trapStub = new CiRegisterConfig(standard, Aarch64TrapFrameAccess.CSL);
                CiRegisterConfig trampoline = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 8,
                                Aarch64.r0, Aarch64.r1, Aarch64.r2, Aarch64.r3, Aarch64.r4, Aarch64.r5, Aarch64.r6, Aarch64.r7, // parameters
                                Aarch64.fp,   // must be preserved for baseline compiler ???frame pointer???
                    standard.getScratchRegister(),    // dynamic dispatch index is saved here for stack frame walker
                    Aarch64.d0, Aarch64.d1, Aarch64.d2, Aarch64.d3, Aarch64.d4, Aarch64.d5, Aarch64.d6, Aarch64.d7   // parameters
                ));

                CiRegisterConfig n2j = new CiRegisterConfig(standard, new CiCalleeSaveLayout(Integer.MAX_VALUE, -1, 8, calleeSavedRegisters));
                n2j.stackArg0Offsets[JavaCallee.ordinal()] = nativeStackArg0Offset;

                roleMap.put(ABI_FP, Aarch64.r29);
                CiRegisterConfig template = new CiRegisterConfig(
                                Aarch64.fp,          // frame???
                                Aarch64.r0,          // integral return value
                                Aarch64.d0,          // floating point return value
                                Aarch64.r16,         // scratch
                                Aarch64.r17,         // scratch 1
                                allocatable,         // allocatable
                                allocatable,         // caller save
                                parameters,          // parameter registers
                                null,                // no callee save
                                Aarch64.allRegisters,        // all AMD64 registers
                                roleMap);            // VM register role map

                setNonZero(template.getAttributesMap(), Aarch64.LATCH_REGISTER, Aarch64.sp, Aarch64.fp);
                return new RegisterConfigs(standard, n2j, trampoline, template, compilerStub, uncommonTrapStub, trapStub);
            }
        } else if (platform().isa == ISA.RISCV64) {
            if (os == OS.LINUX) {
                allocatable = new CiRegister[] {
                    /* RISCV64.x1 returnAddress,  RISCV64.x2 stackPointer,  RISCV64.x3 globalPointer,
                    RISCV64.x4 threadPointer */
                    RISCV64.x5,  RISCV64.x6, RISCV64.x7,
                    /* RISCV64.x8 framePointer, */
                    RISCV64.x9, RISCV64.x10, RISCV64.x11, RISCV64.x12, RISCV64.x13, RISCV64.x14, RISCV64.x15,
                    RISCV64.x16, RISCV64.x17, RISCV64.x18, RISCV64.x19, RISCV64.x20, RISCV64.x21, RISCV64.x22, RISCV64.x23,
                    RISCV64.x24, RISCV64.x25, /* RISCV64.x26, latch */ RISCV64.x27, /*RISCV64.x28 scratch, RISCV64.x29 scratch, RISCV64.x30 reserved for 32 bit jumps , */

                    RISCV64.f0,  RISCV64.f1,  RISCV64.f2,  RISCV64.f3,  RISCV64.f4,  RISCV64.f5,  RISCV64.f6,  RISCV64.f7,
                    RISCV64.f8,  RISCV64.f9,  RISCV64.f10, RISCV64.f11, RISCV64.f12, RISCV64.f13, RISCV64.f14, RISCV64.f15,
                    RISCV64.f16, RISCV64.f17, RISCV64.f18, RISCV64.f19, RISCV64.f20, RISCV64.f21, RISCV64.f22, RISCV64.f23,
                    RISCV64.f24, RISCV64.f25, RISCV64.f26, RISCV64.f27
                };
                parameters = new CiRegister[] {
                    RISCV64.x10, RISCV64.x11, RISCV64.x12, RISCV64.x13,
                    RISCV64.x14, RISCV64.x15, RISCV64.x16, RISCV64.x17,

                    RISCV64.f10, RISCV64.f11, RISCV64.f12, RISCV64.f13,
                    RISCV64.f14, RISCV64.f15, RISCV64.f16, RISCV64.f17,
                };
                // A call to the runtime may change the state of the safepoint latch
                // and so a compiler stub must leave the latch register alone
                allRegistersExceptLatch = new CiRegister[] {
                    RISCV64.x1,  RISCV64.x2,  RISCV64.x3,  RISCV64.x4,  RISCV64.x5,  RISCV64.x6,  RISCV64.x7,
                    RISCV64.x8,  RISCV64.x9,  RISCV64.x10, RISCV64.x11, RISCV64.x12, RISCV64.x13, RISCV64.x14, RISCV64.x15,
                    RISCV64.x16, RISCV64.x17, RISCV64.x18, RISCV64.x19, RISCV64.x20, RISCV64.x21, RISCV64.x22, RISCV64.x23,
                    RISCV64.x24, RISCV64.x25, /*RISCV64.x26, */ RISCV64.x27, RISCV64.x28, RISCV64.x29, RISCV64.x30, RISCV64.x31,

                    RISCV64.f0,  RISCV64.f1,  RISCV64.f2,  RISCV64.f3,  RISCV64.f4,  RISCV64.f5,  RISCV64.f6,  RISCV64.f7,
                    RISCV64.f8,  RISCV64.f9,  RISCV64.f10, RISCV64.f11, RISCV64.f12, RISCV64.f13, RISCV64.f14, RISCV64.f15,
                    RISCV64.f16, RISCV64.f17, RISCV64.f18, RISCV64.f19, RISCV64.f20, RISCV64.f21, RISCV64.f22, RISCV64.f23,
                    RISCV64.f24, RISCV64.f25, RISCV64.f26, RISCV64.f27, RISCV64.f28, RISCV64.f29, RISCV64.f30, RISCV64.f31
                };

                roleMap.put(CPU_SP, RISCV64.sp);
                roleMap.put(CPU_FP, RISCV64.fp);
                roleMap.put(ABI_SP, RISCV64.sp);
                roleMap.put(ABI_FP, RISCV64.fp);
                roleMap.put(LATCH, RISCV64.LATCH_REGISTER);

                /**
                 * The register configuration for a normal Java method.
                 * This configuration specifies <b>all</b> allocatable registers as caller-saved
                 * as inlining is expected to reduce the call overhead sufficiently.
                 */
                standard = new CiRegisterConfig(
                        RISCV64.sp,          // frame
                        RISCV64.a0,          // integral return value
                        RISCV64.fa0,          // floating point return value
                        RISCV64.x28,         // scratch
                        RISCV64.x29,         // scratch 1
                        allocatable,         // allocatable
                        allocatable,         // caller save
                        parameters,          // parameter registers
                        null,                // no callee save
                        RISCV64.allRegisters, // all AMD64 registers
                        roleMap);            // VM register role map


                // Account for the word at the bottom of the frame used
                // for saving an overwritten return address during deoptimization
                int javaStackArg0Offset = Deoptimization.DEOPT_RETURN_ADDRESS_OFFSET + Word.size();
                int nativeStackArg0Offset = 0;
                standard.stackArg0Offsets[JavaCall.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[JavaCallee.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[RuntimeCall.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[NativeCall.ordinal()] = nativeStackArg0Offset;

                setNonZero(standard.getAttributesMap(), RISCV64.LATCH_REGISTER, RISCV64.sp, RISCV64.fp);

                CiRegisterConfig compilerStub = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 8, allRegistersExceptLatch));
                CiRegisterConfig uncommonTrapStub = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 8, RISCV64.csaRegisters));
                CiRegisterConfig trapStub = new CiRegisterConfig(standard, RISCV64TrapFrameAccess.CSL);
                CiRegisterConfig trampoline = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 8,
                        RISCV64.x10, RISCV64.x11, RISCV64.x12, RISCV64.x13,
                        RISCV64.x14, RISCV64.x15, RISCV64.x16, RISCV64.x17, // parameters
                        RISCV64.fp,   // must be preserved for baseline compiler ???frame pointer???
                        standard.getScratchRegister(),    // dynamic dispatch index is saved here for stack frame walker
                        RISCV64.f10, RISCV64.f11, RISCV64.f12, RISCV64.f13,
                        RISCV64.f14, RISCV64.f15, RISCV64.f16, RISCV64.f17  // parameters
                ));

                CiRegisterConfig n2j = new CiRegisterConfig(standard, new CiCalleeSaveLayout(Integer.MAX_VALUE, -1, 8,
                        RISCV64.sp, RISCV64.x8, RISCV64.x9, RISCV64.s2, RISCV64.s3, RISCV64.s4, RISCV64.s5, RISCV64.s6, RISCV64.s7, RISCV64.s8,
                        RISCV64.s9, RISCV64.s10, RISCV64.s11,
                        RISCV64.fs0, RISCV64.fs1, RISCV64.fs2, RISCV64.fs3, RISCV64.fs4, RISCV64.fs5, RISCV64.fs6,
                        RISCV64.fs7, RISCV64.fs8, RISCV64.fs9, RISCV64.fs10, RISCV64.fs11));
                n2j.stackArg0Offsets[JavaCallee.ordinal()] = nativeStackArg0Offset;

                roleMap.put(ABI_FP, RISCV64.fp);
                CiRegisterConfig template = new CiRegisterConfig(
                        RISCV64.fp,          // frame???
                        RISCV64.a0,          // integral return value
                        RISCV64.fa0,          // floating point return value
                        RISCV64.x28,         // scratch
                        RISCV64.x29,         // scratch 1
                        allocatable,         // allocatable
                        allocatable,         // caller save
                        parameters,          // parameter registers
                        null,                // no callee save
                        RISCV64.allRegisters,        // all RISCV64 registers
                        roleMap);            // VM register role map

                setNonZero(template.getAttributesMap(), RISCV64.LATCH_REGISTER, RISCV64.sp, RISCV64.fp);
                return new RegisterConfigs(standard, n2j, trampoline, template, compilerStub, uncommonTrapStub, trapStub);
            }
        } else {
            throw FatalError.unimplemented("com.sun.max.vm.compiler.target.RegisterConfigs.create");
        }
        return null;
    }
}
