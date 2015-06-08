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

import com.oracle.max.asm.target.amd64.AMD64;
import com.oracle.max.asm.target.armv8.*;

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
        } else if (platform().isa == ISA.ARMv8) {
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
                    ARMv8.r0,  ARMv8.r1,  ARMv8.r2,  ARMv8.r3,  ARMv8.r4,  ARMv8.r5,  ARMv8.r6,  ARMv8.r7,
                    ARMv8.r8,  ARMv8.r9,  ARMv8.r10, ARMv8.r11, ARMv8.r12, ARMv8.r13, ARMv8.r14, ARMv8.r15,
                    /*ARMv8.r16, ARMv8.r17, ARMv8.r18,*/
                    ARMv8.r19, ARMv8.r20, ARMv8.r21, ARMv8.r22, ARMv8.r23,
                    ARMv8.r24, ARMv8.r25, ARMv8.r26,
                    //r27:heapBaseRegister, r28:threadRegister, r29:fp(framePointer), r30:linkRegister
                    /*ARMv8.r27, ARMv8.r28, ARMv8.r29, ARMv8.r30,*/
                    //r31:sp||zr
                    /*ARMv8.r31, ARMv8.sp,  ARMv8.zr,*/
                    ARMv8.d0,  ARMv8.d1,  ARMv8.d2,  ARMv8.d3,  ARMv8.d4,  ARMv8.d5,  ARMv8.d6,  ARMv8.d7,
                    ARMv8.d8,  ARMv8.d9,  ARMv8.d10, ARMv8.d11, ARMv8.d12, ARMv8.d13, ARMv8.d14, ARMv8.d15,
                    ARMv8.d16, ARMv8.d17, ARMv8.d18, ARMv8.d19, ARMv8.d20, ARMv8.d21, ARMv8.d22, ARMv8.d23,
                    ARMv8.d24, ARMv8.d25, ARMv8.d26, ARMv8.d27, ARMv8.d28, ARMv8.d29, ARMv8.d30, ARMv8.d31
                };
                CiRegister[] parameters = {
                    ARMv8.r0, ARMv8.r1, ARMv8.r2, ARMv8.r3,
                    ARMv8.r4, ARMv8.r5, ARMv8.r6, ARMv8.r7,
                    ARMv8.d0, ARMv8.d1, ARMv8.d2, ARMv8.d3,
                    ARMv8.d4, ARMv8.d5, ARMv8.d6, ARMv8.d7
                };
                CiRegister[] allRegistersExceptLatch = {
                    ARMv8.r0,  ARMv8.r1,  ARMv8.r2,  ARMv8.r3,  ARMv8.r4,  ARMv8.r5,  ARMv8.r6,  ARMv8.r7,
                    ARMv8.r8,  ARMv8.r9,  ARMv8.r10, ARMv8.r11, ARMv8.r12, ARMv8.r13, ARMv8.r14, ARMv8.r15,
                    ARMv8.r16, ARMv8.r17, ARMv8.r18,
                    ARMv8.r19, ARMv8.r20, ARMv8.r21, ARMv8.r22, ARMv8.r23,
                    ARMv8.r24, ARMv8.r25,
                    // r26 is personally defined as latch register
                    /*ARMv8.r26,*/
                    //r27:heapBaseRegister, r28:threadRegister, r29:fp(framePointer), r30:linkRegister
                    ARMv8.r27, ARMv8.r28, ARMv8.r29, ARMv8.r30,
                    //r31:sp||zr
                    ARMv8.r31, ARMv8.sp,  ARMv8.zr,
                    ARMv8.d0,  ARMv8.d1,  ARMv8.d2,  ARMv8.d3,  ARMv8.d4,  ARMv8.d5,  ARMv8.d6,  ARMv8.d7,
                    ARMv8.d8,  ARMv8.d9,  ARMv8.d10, ARMv8.d11, ARMv8.d12, ARMv8.d13, ARMv8.d14, ARMv8.d15,
                    ARMv8.d16, ARMv8.d17, ARMv8.d18, ARMv8.d19, ARMv8.d20, ARMv8.d21, ARMv8.d22, ARMv8.d23,
                    ARMv8.d24, ARMv8.d25, ARMv8.d26, ARMv8.d27, ARMv8.d28, ARMv8.d29, ARMv8.d30, ARMv8.d31
                };
                CiRegister[] allRegisters = {
                    ARMv8.r0,  ARMv8.r1,  ARMv8.r2,  ARMv8.r3,  ARMv8.r4,  ARMv8.r5,  ARMv8.r6,  ARMv8.r7,
                    ARMv8.r8,  ARMv8.r9,  ARMv8.r10, ARMv8.r11, ARMv8.r12, ARMv8.r13, ARMv8.r14, ARMv8.r15,
                    ARMv8.r16, ARMv8.r17, ARMv8.r18,
                    ARMv8.r19, ARMv8.r20, ARMv8.r21, ARMv8.r22, ARMv8.r23,
                    ARMv8.r24, ARMv8.r25, ARMv8.r26,
                    //r27:heapBaseRegister, r28:threadRegister, r29:fp(framePointer), r30:linkRegister
                    ARMv8.r27, ARMv8.r28, ARMv8.r29, ARMv8.r30,
                    //r31:sp||zr
                    ARMv8.r31, ARMv8.sp,  ARMv8.zr,
                    ARMv8.d0,  ARMv8.d1,  ARMv8.d2,  ARMv8.d3,  ARMv8.d4,  ARMv8.d5,  ARMv8.d6,  ARMv8.d7,
                    ARMv8.d8,  ARMv8.d9,  ARMv8.d10, ARMv8.d11, ARMv8.d12, ARMv8.d13, ARMv8.d14, ARMv8.d15,
                    ARMv8.d16, ARMv8.d17, ARMv8.d18, ARMv8.d19, ARMv8.d20, ARMv8.d21, ARMv8.d22, ARMv8.d23,
                    ARMv8.d24, ARMv8.d25, ARMv8.d26, ARMv8.d27, ARMv8.d28, ARMv8.d29, ARMv8.d30, ARMv8.d31
                };

                CiRegister[] calleeSavedRegisters= {
                    ARMv8.r19, ARMv8.r20, ARMv8.r21, ARMv8.r22, ARMv8.r23,
                    ARMv8.r24, ARMv8.r25, ARMv8.r26, ARMv8.r27, ARMv8.r28,
                };

                HashMap<Integer, CiRegister> roleMap = new HashMap<Integer, CiRegister>();
                roleMap.put(CPU_SP, ARMv8.sp);
                roleMap.put(CPU_FP, ARMv8.r29);
                roleMap.put(ABI_SP, ARMv8.sp);
                roleMap.put(ABI_FP, ARMv8.r29);
                roleMap.put(LATCH, ARMv8.r26);

                /**
                 * The register configuration for a normal Java method.
                 * This configuration specifies <b>all</b> allocatable registers as caller-saved
                 * as inlining is expected to reduce the call overhead sufficiently.
                 */
                CiRegisterConfig standard = new CiRegisterConfig(
                                ARMv8.sp,          // frame
                                ARMv8.r0,          // integral return value
                                ARMv8.d0,          // floating point return value
                                ARMv8.r16,         // scratch
                                allocatable,         // allocatable
                                allocatable,         // caller save
                                parameters,          // parameter registers
                                null,                // no callee save
                                ARMv8.allRegisters,// all AMD64 registers
                                roleMap);            // VM register role map


                // Account for the word at the bottom of the frame used
                // for saving an overwritten return address during deoptimization
                int javaStackArg0Offset = Deoptimization.DEOPT_RETURN_ADDRESS_OFFSET + Word.size();
                int nativeStackArg0Offset = 0;
                standard.stackArg0Offsets[JavaCall.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[JavaCallee.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[RuntimeCall.ordinal()] = javaStackArg0Offset;
                standard.stackArg0Offsets[NativeCall.ordinal()] = nativeStackArg0Offset;

                setNonZero(standard.getAttributesMap(), ARMv8.r26, ARMv8.sp, ARMv8.fp);

                CiRegisterConfig compilerStub = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 8, allRegistersExceptLatch));
                CiRegisterConfig uncommonTrapStub = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 8, ARMv8.allRegisters));
                CiRegisterConfig trapStub = new CiRegisterConfig(standard,  new CiCalleeSaveLayout(0, 34*8+32*16, 8, ARMv8.allRegisters));
                CiRegisterConfig trampoline = new CiRegisterConfig(standard, new CiCalleeSaveLayout(0, -1, 8,
                                ARMv8.r0, ARMv8.r1, ARMv8.r2, ARMv8.r3, ARMv8.r4, ARMv8.r5, ARMv8.r6, ARMv8.r7, // parameters
                                ARMv8.fp,   // must be preserved for baseline compiler ???frame pointer???
                    standard.getScratchRegister(),    // dynamic dispatch index is saved here for stack frame walker
                    ARMv8.d0, ARMv8.d1, ARMv8.d2, ARMv8.d3, ARMv8.d4, ARMv8.d5, ARMv8.d6, ARMv8.d7   // parameters
                ));

                CiRegisterConfig n2j = new CiRegisterConfig(standard, new CiCalleeSaveLayout(Integer.MAX_VALUE, -1, 8, calleeSavedRegisters));
                n2j.stackArg0Offsets[JavaCallee.ordinal()] = nativeStackArg0Offset;

                roleMap.put(ABI_FP, ARMv8.r29);
                CiRegisterConfig template = new CiRegisterConfig(
                                ARMv8.r29,          // frame???
                                ARMv8.r0,          // integral return value
                                ARMv8.d0,          // floating point return value
                                ARMv8.r16,         // scratch
                                allocatable,         // allocatable
                                allocatable,         // caller save
                                parameters,          // parameter registers
                                null,                // no callee save
                                ARMv8.allRegisters,        // all AMD64 registers
                                roleMap);            // VM register role map

                setNonZero(template.getAttributesMap(), ARMv8.r26, ARMv8.sp, ARMv8.r29);
                return new RegisterConfigs(standard, n2j, trampoline, template, compilerStub, uncommonTrapStub, trapStub);
            }
        }
        throw FatalError.unimplemented();
    }
}
