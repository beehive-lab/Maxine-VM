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
     * The register configuration for a direct call to native/C code.
     * This configuration must specify <b>all</b> allocatable registers as caller-saved
     * so that all object references are on the stack around the native call.
     * These values will also be spilled to the stack by a native compiler
     * adhering to the ABI callee-save semantics. However, the ABI does not
     * specify where they are spilled and so they are not available
     * to the GC roots scanner.
     */
    public final CiRegisterConfig j2n;

    /**
     * The register configuration for a direct call to a {@linkplain MethodActor#isTrampoline() trampoline}.
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
        if (method.isCFunction()) {
            return j2n;
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
                    CiRegisterConfig j2n,
                    CiRegisterConfig trampoline,
                    CiRegisterConfig template,
                    CiRegisterConfig globalStub,
                    CiRegisterConfig trapStub) {
        this.standard = standard;
        this.n2j = n2j;
        this.j2n = j2n;
        this.trampoline = trampoline;
        this.jitTemplate = template;
        this.globalStub = globalStub;
        this.trapStub = trapStub;

        assert Arrays.equals(standard.getAllocatableRegisters(), standard.getCallerSaveRegisters()) : "VM requires caller-save for VM to VM calls";
        assert Arrays.equals(j2n.getAllocatableRegisters(), j2n.getCallerSaveRegisters()) : "VM requires caller-save for J2N calls so that it can find all references";
    }

    @HOSTED_ONLY
    public static RegisterConfigs create() {
        if (platform().isa == ISA.AMD64) {
            OS os = platform().os;
            if (os == OS.LINUX || os == OS.SOLARIS || os == OS.DARWIN || os == OS.GUESTVM) {
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
                CiRegisterConfig j2n = standard;
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

                return new RegisterConfigs(standard, n2j, j2n, trampoline, template, globalStub, trapStub);
            }
        }
        throw FatalError.unimplemented();
    }
}
