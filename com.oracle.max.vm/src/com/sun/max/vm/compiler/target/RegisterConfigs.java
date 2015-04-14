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

import com.oracle.max.asm.target.aarch64.*;
import com.oracle.max.asm.target.amd64.AMD64;

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
                    AMD64.rax,   AMD64.rcx,   AMD64.rdx,   AMD64.rbx,   AMD64.rsi,   AMD64.rdi,
                    AMD64.r8,    AMD64.r9,    AMD64.r10,   AMD64.r12,   AMD64.r13,   AMD64.r15,
                    AMD64.xmm0,  AMD64.xmm1,  AMD64.xmm2,  AMD64.xmm3,  AMD64.xmm4,
                    AMD64.xmm5,  AMD64.xmm6,  AMD64.xmm7,  AMD64.xmm8,  AMD64.xmm9,
                    AMD64.xmm10, AMD64.xmm11, AMD64.xmm12, AMD64.xmm13, AMD64.xmm14, AMD64.xmm15};

                CiRegister[] parameters = {
                    AMD64.rdi, AMD64.rsi, AMD64.rdx, AMD64.rcx, AMD64.r8, AMD64.r9,
                    AMD64.xmm0, AMD64.xmm1, AMD64.xmm2, AMD64.xmm3, AMD64.xmm4, AMD64.xmm5, AMD64.xmm6, AMD64.xmm7
                };

                // A call to the runtime may change the state of the safepoint latch
                // and so a compiler stub must leave the latch register alone
                CiRegister[] allRegistersExceptLatch = {
                    AMD64.rax,  AMD64.rcx,  AMD64.rdx,   AMD64.rbx,   AMD64.rsp,   AMD64.rbp,   AMD64.rsi,   AMD64.rdi,
                    AMD64.r8,   AMD64.r9,   AMD64.r10,   AMD64.r11,   AMD64.r12,   AMD64.r13,
                    /*r14,*/
                    AMD64.r15,
                    AMD64.xmm0, AMD64.xmm1, AMD64.xmm2,  AMD64.xmm3,  AMD64.xmm4,  AMD64.xmm5,  AMD64.xmm6,  AMD64.xmm7,
                    AMD64.xmm8, AMD64.xmm9, AMD64.xmm10, AMD64.xmm11, AMD64.xmm12, AMD64.xmm13, AMD64.xmm14, AMD64.xmm15
                };


                HashMap<Integer, CiRegister> roleMap = new HashMap<Integer, CiRegister>();
                roleMap.put(CPU_SP, AMD64.rsp);
                roleMap.put(CPU_FP, AMD64.rbp);
                roleMap.put(ABI_SP, AMD64.rsp);
                roleMap.put(ABI_FP, AMD64.rsp);
                roleMap.put(LATCH, AMD64.r14);

                /**
                 * The register configuration for a normal Java method.
                 * This configuration specifies <b>all</b> allocatable registers as caller-saved
                 * as inlining is expected to reduce the call overhead sufficiently.
                 */
                CiRegisterConfig standard = new CiRegisterConfig(
                                AMD64.rsp,                 // frame
                                AMD64.rax,                 // integral return value
                                AMD64.xmm0,                // floating point return value
                                AMD64.r11,                 // scratch
                                allocatable,         // allocatable
                                allocatable,         // caller save
                                parameters,          // parameter registers
                                null,                // no callee save
                                AMD64.allRegisters,        // all AMD64 registers
                                roleMap);            // VM register role map

                // Account for the word at the bottom of the frame used
                // for saving an overwritten return address during deoptimization
                int javaStackArg0Offset = Deoptimization.DEOPT_RETURN_ADDRESS_OFFSET + Word.size();
                int nativeStackArg0Offset = 0;
                standard.stackArg0Offsets[JavaCall.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[JavaCallee.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[RuntimeCall.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[NativeCall.ordinal()] = nativeStackArg0Offset;

                setNonZero(standard.getAttributesMap(), AMD64.r14, AMD64.rsp);

                CiRegisterConfig compilerStub = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 8, allRegistersExceptLatch));
                CiRegisterConfig uncommonTrapStub = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 8, AMD64.cpuxmmRegisters));
                CiRegisterConfig trapStub = new CiRegisterConfig(standard, AMD64TrapFrameAccess.CSL);
                CiRegisterConfig trampoline = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 8,
                                AMD64.rdi, AMD64.rsi, AMD64.rdx, AMD64.rcx, AMD64.r8, AMD64.r9,                       // parameters
                                AMD64.rbp,                                              // must be preserved for baseline compiler
                    standard.getScratchRegister(),                    // dynamic dispatch index is saved here for stack frame walker
                    AMD64.xmm0, AMD64.xmm1, AMD64.xmm2, AMD64.xmm3, AMD64.xmm4, AMD64.xmm5, AMD64.xmm6, AMD64.xmm7    // parameters
                ));

                CiRegisterConfig n2j = new CiRegisterConfig(standard, new CiCalleeSaveLayout(Integer.MAX_VALUE, -1, 8, AMD64.rbx, AMD64.rbp, AMD64.r12, AMD64.r13, AMD64.r14, AMD64.r15));
                n2j.stackArg0Offsets[JavaCallee.ordinal()] = nativeStackArg0Offset;

                roleMap.put(ABI_FP, AMD64.rbp);
                CiRegisterConfig template = new CiRegisterConfig(
                                AMD64.rbp,                 // frame
                                AMD64.rax,                 // integral return value
                                AMD64.xmm0,                // floating point return value
                                AMD64.r11,                 // scratch
                                allocatable,         // allocatable
                                allocatable,         // caller save
                                parameters,          // parameter registers
                                null,                // no callee save
                                AMD64.allRegisters,        // all AMD64 registers
                                roleMap);            // VM register role map
                setNonZero(template.getAttributesMap(), AMD64.r14, AMD64.rsp, AMD64.rbp);

                return new RegisterConfigs(standard, n2j, trampoline, template, compilerStub, uncommonTrapStub, trapStub);
            }
        } else if (platform().isa == ISA.AARCH64) {
            OS os = platform().os;
            if (os == OS.LINUX) {
                /**
                 * The set of allocatable registers shared by most register configurations.
                 * according to
                 * "voo-graal/graal/src/cpu/aarch64/vm/register_definitions_aarch64.cpp"
                 * and
                 * "voo-graal/graal/graal/com.oracle.graal.hotspot.armv8/src/com/oracle/graal/hotspot/armv8/ARMv8HotSpotRegisterConfig.java"
                 */
                CiRegister[] allocatable = {
                    AARCH64.r0,  AARCH64.r1,  AARCH64.r2,  AARCH64.r3,  AARCH64.r4,  AARCH64.r5,  AARCH64.r6,  AARCH64.r7,
                    AARCH64.r8,  AARCH64.r9,  AARCH64.r10, AARCH64.r11, AARCH64.r12, AARCH64.r13, AARCH64.r14, AARCH64.r15,
                    /*AARCH64.r16, AARCH64.r17, AARCH64.r18,*/
                    AARCH64.r19, AARCH64.r20, AARCH64.r21, AARCH64.r22, AARCH64.r23,
                    AARCH64.r24, AARCH64.r25, AARCH64.r26,
                    //r27:heapBaseRegister, r28:threadRegister, r29:fp(framePointer), r30:linkRegister
                    /*AARCH64.r27, AARCH64.r28, AARCH64.r29, AARCH64.r30,*/
                    //r31:sp||zr
                    /*AARCH64.r31, AARCH64.sp,  AARCH64.zr,*/
                    AARCH64.v0,  AARCH64.v1,  AARCH64.v2,  AARCH64.v3,  AARCH64.v4,  AARCH64.v5,  AARCH64.v6,  AARCH64.v7,
                    AARCH64.v8,  AARCH64.v9,  AARCH64.v10, AARCH64.v11, AARCH64.v12, AARCH64.v13, AARCH64.v14, AARCH64.v15,
                    AARCH64.v16, AARCH64.v17, AARCH64.v18, AARCH64.v19, AARCH64.v20, AARCH64.v21, AARCH64.v22, AARCH64.v23,
                    AARCH64.v24, AARCH64.v25, AARCH64.v26, AARCH64.v27, AARCH64.v28, AARCH64.v29, AARCH64.v30, AARCH64.v31
                };
                CiRegister[] parameters = {
                    AARCH64.r0, AARCH64.r1, AARCH64.r2, AARCH64.r3,
                    AARCH64.r4, AARCH64.r5, AARCH64.r6, AARCH64.r7,
                    AARCH64.v0, AARCH64.v1, AARCH64.v2, AARCH64.v3,
                    AARCH64.v4, AARCH64.v5, AARCH64.v6, AARCH64.v7
                };
                CiRegister[] allRegistersExceptLatch = {
                    AARCH64.r0,  AARCH64.r1,  AARCH64.r2,  AARCH64.r3,  AARCH64.r4,  AARCH64.r5,  AARCH64.r6,  AARCH64.r7,
                    AARCH64.r8,  AARCH64.r9,  AARCH64.r10, AARCH64.r11, AARCH64.r12, AARCH64.r13, AARCH64.r14, AARCH64.r15,
                    AARCH64.r16, AARCH64.r17, AARCH64.r18,
                    AARCH64.r19, AARCH64.r20, AARCH64.r21, AARCH64.r22, AARCH64.r23,
                    AARCH64.r24, AARCH64.r25,
                    // r26 is personally defined as latch register
                    /*AARCH64.r26,*/
                    //r27:heapBaseRegister, r28:threadRegister, r29:fp(framePointer), r30:linkRegister
                    AARCH64.r27, AARCH64.r28, AARCH64.r29, AARCH64.r30,
                    //r31:sp||zr
                    AARCH64.r31, AARCH64.sp,  AARCH64.zr,
                    AARCH64.v0,  AARCH64.v1,  AARCH64.v2,  AARCH64.v3,  AARCH64.v4,  AARCH64.v5,  AARCH64.v6,  AARCH64.v7,
                    AARCH64.v8,  AARCH64.v9,  AARCH64.v10, AARCH64.v11, AARCH64.v12, AARCH64.v13, AARCH64.v14, AARCH64.v15,
                    AARCH64.v16, AARCH64.v17, AARCH64.v18, AARCH64.v19, AARCH64.v20, AARCH64.v21, AARCH64.v22, AARCH64.v23,
                    AARCH64.v24, AARCH64.v25, AARCH64.v26, AARCH64.v27, AARCH64.v28, AARCH64.v29, AARCH64.v30, AARCH64.v31
                };
                CiRegister[] allRegisters = {
                    AARCH64.r0,  AARCH64.r1,  AARCH64.r2,  AARCH64.r3,  AARCH64.r4,  AARCH64.r5,  AARCH64.r6,  AARCH64.r7,
                    AARCH64.r8,  AARCH64.r9,  AARCH64.r10, AARCH64.r11, AARCH64.r12, AARCH64.r13, AARCH64.r14, AARCH64.r15,
                    AARCH64.r16, AARCH64.r17, AARCH64.r18,
                    AARCH64.r19, AARCH64.r20, AARCH64.r21, AARCH64.r22, AARCH64.r23,
                    AARCH64.r24, AARCH64.r25, AARCH64.r26,
                    //r27:heapBaseRegister, r28:threadRegister, r29:fp(framePointer), r30:linkRegister
                    AARCH64.r27, AARCH64.r28, AARCH64.r29, AARCH64.r30,
                    //r31:sp||zr
                    AARCH64.r31, AARCH64.sp,  AARCH64.zr,
                    AARCH64.v0,  AARCH64.v1,  AARCH64.v2,  AARCH64.v3,  AARCH64.v4,  AARCH64.v5,  AARCH64.v6,  AARCH64.v7,
                    AARCH64.v8,  AARCH64.v9,  AARCH64.v10, AARCH64.v11, AARCH64.v12, AARCH64.v13, AARCH64.v14, AARCH64.v15,
                    AARCH64.v16, AARCH64.v17, AARCH64.v18, AARCH64.v19, AARCH64.v20, AARCH64.v21, AARCH64.v22, AARCH64.v23,
                    AARCH64.v24, AARCH64.v25, AARCH64.v26, AARCH64.v27, AARCH64.v28, AARCH64.v29, AARCH64.v30, AARCH64.v31
                };

                HashMap<Integer, CiRegister> roleMap = new HashMap<Integer, CiRegister>();
                roleMap.put(CPU_SP, AARCH64.sp);
                roleMap.put(CPU_FP, AARCH64.r29);
                roleMap.put(ABI_SP, AARCH64.sp);
                roleMap.put(ABI_FP, AARCH64.r29);
                roleMap.put(LATCH, AARCH64.r26);

                /**
                 * The register configuration for a normal Java method.
                 * This configuration specifies <b>all</b> allocatable registers as caller-saved
                 * as inlining is expected to reduce the call overhead sufficiently.
                 */
                CiRegisterConfig standard = new CiRegisterConfig(
                                AARCH64.sp,          // frame
                                AARCH64.r0,          // integral return value
                                AARCH64.v0,          // floating point return value
                                AARCH64.r16,         // scratch
                                allocatable,         // allocatable
                                allocatable,         // caller save
                                parameters,          // parameter registers
                                null,                // no callee save
                                AARCH64.allRegisters,// all AMD64 registers
                                roleMap);            // VM register role map


                // Account for the word at the bottom of the frame used
                // for saving an overwritten return address during deoptimization
                int javaStackArg0Offset = Deoptimization.DEOPT_RETURN_ADDRESS_OFFSET + Word.size();
                int nativeStackArg0Offset = 0;
                standard.stackArg0Offsets[JavaCall.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[JavaCallee.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[RuntimeCall.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[NativeCall.ordinal()] = nativeStackArg0Offset;

                setNonZero(standard.getAttributesMap(), AARCH64.r26, AARCH64.sp, AARCH64.fp);

                CiRegisterConfig compilerStub = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 8, allRegistersExceptLatch));
                CiRegisterConfig uncommonTrapStub = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 8, AARCH64.allRegisters));
                CiRegisterConfig trapStub = new CiRegisterConfig(standard,  new CiCalleeSaveLayout(0, 32*8+32*16, 8, AARCH64.allRegisters));
                CiRegisterConfig trampoline = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 8,
                                AARCH64.r0, AARCH64.r1, AARCH64.r2, AARCH64.r3, AARCH64.r4, AARCH64.r5, AARCH64.r6, AARCH64.r7, // parameters
                                AARCH64.fp,   // must be preserved for baseline compiler ???frame pointer???
                    standard.getScratchRegister(),    // dynamic dispatch index is saved here for stack frame walker
                    AARCH64.v0, AARCH64.v1, AARCH64.v2, AARCH64.v3, AARCH64.v4, AARCH64.v5, AARCH64.v6, AARCH64.v7   // parameters
                ));

            }


            //return new RegisterConfigs(standard, n2j, trampoline, template, compilerStub, uncommonTrapStub, trapStub);
        }
        throw FatalError.unimplemented();
    }
}
