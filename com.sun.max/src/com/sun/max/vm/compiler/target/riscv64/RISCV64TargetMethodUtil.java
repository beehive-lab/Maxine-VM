/*
 * Copyright (c) 2020, APT Group, Department of Computer Science,
 * School of Engineering, The University of Manchester. All rights reserved.
 * Copyright (c) 2018-2019, APT Group, School of Computer Science,
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
package com.sun.max.vm.compiler.target.riscv64;

import com.oracle.max.asm.NumUtil;
import com.oracle.max.cri.intrinsics.MemoryBarriers;
import com.oracle.max.asm.target.riscv64.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.HOSTED_ONLY;
import com.sun.max.platform.Platform;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.CallEntryPoint;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.StackFrameCursor;
import com.sun.max.vm.stack.StackFrameWalker;

import static com.oracle.max.asm.target.aarch64.Aarch64Assembler.nopHelper;
import static com.oracle.max.asm.target.riscv64.RISCV64MacroAssembler.*;
import static com.sun.max.vm.compiler.CallEntryPoint.BASELINE_ENTRY_POINT;
import static com.sun.max.vm.compiler.CallEntryPoint.OPTIMIZED_ENTRY_POINT;
import static com.sun.max.vm.compiler.target.TargetMethod.useSystemMembarrier;
import static com.sun.max.vm.compiler.target.TargetMethod.useNonMandatedSystemMembarrier;

public final class RISCV64TargetMethodUtil {

    /**
     * Lock to avoid race on concurrent icache invalidation when patching target methods.
     */
    private static final Object PatchingLock = new Object();

    public static final int RET = 0x8067;

    /**
     * Instruction encodings for {@code auipc x28, #0}.
     */
    private static final int AUIPC_X28 = addUpperImmediatePCHelper(RISCV64.x28, 0);

    /**
     * Instruction encodings for {@code lw x29, 12(x28)}.
     */
    private static final int LW_X29_X28_16 =
            lwHelper(RISCV64.x29, RISCV64.x28, TRAMPOLINE_INSTRUCTIONS * INSTRUCTION_SIZE);

    /**
     * Instruction encodings for {@code add x28, x28, x29}.
     */
    private static final int ADD_X28_X28_X29 = addSubInstructionHelper(RISCV64.x28, RISCV64.x28, RISCV64.x29, false);

    /**
     * Instruction encodings for {@code jr x28}.
     */
    private static final int JR_X28 = jumpAndLinkHelper(RISCV64.zero, RISCV64.x28, 0);

    /**
     * Extract an instruction from the code array which starts at index=idx.
     * @param code
     * @param idx
     * @return
     */
    private static int extractInstruction(byte [] code, int idx) {
        assert code.length >= idx + 4 : "Insufficient space in code buffer";
        return ((code[idx + 3] & 0xFF) << 24) |
               ((code[idx + 2] & 0xFF) << 16) |
               ((code[idx + 1] & 0xFF) << 8) |
               (code[idx + 0] & 0xFF);
    }

    /**
     * Test whether the memory location contains the trampoline instruction sequence.
     * @param p
     * @return
     */
    private static boolean isTrampolineSite(Pointer p) {
        if (AUIPC_X28 == p.readInt(0) &&
            LW_X29_X28_16 == p.readInt(INSTRUCTION_SIZE) &&
            ADD_X28_X28_X29 == p.readInt(2 * INSTRUCTION_SIZE) &&
            JR_X28 == p.readInt(3 * INSTRUCTION_SIZE)) {
            return true;
        }
        return false;
    }

    /**
     * Gets the target of a 32-bit relative CALL instruction.
     *
     * @param tm the method containing the CALL instruction
     * @param callPos the offset within the code of {@code targetMethod} of the CALL
     * @return the absolute target address of the CALL
     */
    public static CodePointer readCall32Target(TargetMethod tm, int callPos) {
        return readCall32Target(tm.codeAt(callPos));
    }

    /**
     * Gets the target of a 32-bit relative CALL instruction.
     *
     * @param callSite the code pointer to the CALL
     * @return the absolute target address of the CALL
     */
    public static CodePointer readCall32Target(CodePointer callSite) {
        Pointer callSitePointer = callSite.toPointer();
        int instruction = callSitePointer.readInt(0);
        assert isJumpInstruction(instruction) : instruction;
        final int immOffset = jumpAndLinkExtractDisplacement(instruction);
        final Pointer trampolineSite = callSitePointer.plus(immOffset);
        if (isTrampolineSite(trampolineSite)) {
            long offset = trampolineSite.readInt(TRAMPOLINE_OFFSET_OFFSET);
            return CodePointer.from(trampolineSite.plus(offset));
        }
        return callSite.plus(immOffset);
    }

    /**
     * Patch a callsite: if the target is within range of a single branch instruction then
     * that is patched at the callsite; otherwise the trampoline is patched. The target prior
     * to patching is returned.
     * fixingUp identifies when a call-site is being fixed-up (see {@linkplain TargetMethod#fixupCallSite}).
     * A call site being fixed-up cannot be executed by another thread and so no specific consideration to
     * concurrent modification and execution is required during patching as long as the relevant cache maintenance is
     * carried out on the affected addresses after fixing up.
     * 
     * @param tm
     * @param callSite
     * @param target
     * @param fixingUp identifies when fixing up as opposed to patching a concurrently executable call-site.
     * @return
     */
    private static long patchCallSite(TargetMethod tm, CodePointer callSite, Pointer target, boolean fixingUp) {
        long disp = target.toLong() - callSite.toLong();
        int disp20 = (int) disp;
        if (NumUtil.isSignedNbit(21, (long) disp20)) {
            return maybePatchBranchImmediate(callSite, disp20, fixingUp);
        }
        return maybePatchTrampolineCall(tm, callSite, target, fixingUp);
    }

    /**
     * Patch the address operand of a trampoline call if the current target differs from the new target. Optionally
     * patch the address of the call site branch to steer execution to the trampoline. Returns the address of
     * the old target prior to any patching.
     *
     * @param tm
     * @param callSite
     * @param target
     * @param fixingUp identifies when fixing up as opposed to patching a concurrently executable call-site.
     * @return
     */
    private static long maybePatchTrampolineCall(TargetMethod tm, CodePointer callSite, Pointer target, boolean fixingUp) {
        int callOffset = (int) (callSite.toLong() - tm.codeStart().toLong());
        // locate the trampoline site that corresponds to the call site.
        int pos = Safepoints.safepointPosForCall(callOffset, INSTRUCTION_SIZE);
        int spIndex = tm.safepoints().indexOfCallAt(pos);
        Pointer trampolineSite = tm.trampolineStart().plus(spIndex * TRAMPOLINE_SIZE).toPointer();
        assert isTrampolineSite(trampolineSite);
        final long oldOffset = trampolineSite.readInt(TRAMPOLINE_OFFSET_OFFSET);
        final int newOffset = target.minus(trampolineSite).toInt();

        if (newOffset != oldOffset) {
            trampolineSite.writeInt(TRAMPOLINE_OFFSET_OFFSET, newOffset);
            /*
             * For concurrent modification and execution a memory barrier here prevents the possibility
             * of the previous store of the target address being ordered after the call site store (if it
             * is updated).
             */
            if (!fixingUp) {
                MemoryBarriers.barrier(MemoryBarriers.STORE_LOAD);
            }
        }

        long callTarget = maybePatchBranchImmediate(callSite, trampolineSite.minus(callSite.toPointer()).toInt(), fixingUp);

        if (callTarget != trampolineSite.toLong()) {
            return callTarget;
        }
        final long oldTarget = trampolineSite.plus(oldOffset).toLong();
        return oldTarget;
    }

    /**
     * Patch an unconditional branch immediate call site if the displacement of the current branch
     * is not equal to the displacement parameter. 
     * Returns the address of the target prior to patching.
     * 
     * @param callSite
     * @param disp20
     * @param fixingUp identifies when fixing up as opposed to patching a concurrently executable call-site.
     * @return
     */
    private static long maybePatchBranchImmediate(CodePointer callSite, int disp20, boolean fixingUp) {
        assert NumUtil.isSignedNbit(21, (long) disp20);
        int instruction = callSite.toPointer().readInt(0);
        assert isJumpInstruction(instruction) : instruction;
        int oldDisp = jumpAndLinkExtractDisplacement(instruction);
        boolean isLinked = isJumpLinked(instruction);
        if (oldDisp != disp20) {
            patchBranchImmediate(callSite.toPointer(), disp20, isLinked, fixingUp);
        }
        return callSite.plus(oldDisp).toLong();
    }

    /**
     * Pre conditions:
     *   CallSite has already been validated such that:
     *     a). it is the site of an unconditional branch immediate
     *     b). the present target != new target
     * @param callSite
     * @param displacement
     * @param isLinked
     * @param fixingUp identifies when fixing up as opposed to patching a concurrently executable call-site.
     * @return
     */
    private static void patchBranchImmediate(Pointer callSite, int displacement, boolean isLinked, boolean fixingUp) {
        CiRegister linkRegister = isLinked ? RISCV64.ra : RISCV64.zero;
        int instruction = jumpAndLinkImmediateHelper(linkRegister, displacement);
        callSite.writeInt(0, instruction);
        /*
         * Although no explicit synchronisation is mandated by the architecture when patching b -> b, doing
         * so here makes the modified instruction observable.
         */
        if (!fixingUp) {
            MaxineVM.maxine_cache_flush(callSite, INSTRUCTION_SIZE);
            /* The following memory barrier is not mandated by the architecture, however it ensures that the
             * modified branch is globally visible at the expense of the barrier.
             */
            if (useSystemMembarrier() && useNonMandatedSystemMembarrier()) {
                MaxineVM.syscall_membarrier();
            }
        }
    }

    /**
     * Patches all entry points of a {@linkplain TargetMethod} to the target parameter.
     * Only called during deoptimization and also at a safepoint
     * to direct execution from an invalidated method (the one being patched here) to a stub. The stub will
     * complete deoptimization and the invalidated method will eventually be discarded. We can therefore
     * use the simplest patching scheme and since we are on the slow path an optimised version is not necessary.
     * 
     * This function pairs with {@linkplain #isJumpTo} called to validate the target patched here. The prologue is
     * patched with <code>nop</instructions> from the baseline entry point to the optimised entry point (2 or 3
     * instructions depending on the prologue), and the optimised entry point is patched with the jump. 
     * 
     * Patching this way avoids overlapping two long range calls patches (4 * 4 bytes each) and is simpler than patching
     * the trampolines.
     *
     * @param tm the target method to be patched
     * @param target the target of the jump instruction being patched in
     */
    public static void patchWithJump(TargetMethod tm, CodePointer target) {
        // We must be at a global safepoint to safely patch TargetMethods
        FatalError.check(VmOperation.atSafepoint(), "should only be patching entry points when at a safepoint");
        Pointer code = tm.codeStart().toPointer();
        int patchOffset = BASELINE_ENTRY_POINT.offset();

        synchronized (PatchingLock) {
            do {
                code.writeInt(patchOffset, nopHelper());
                patchOffset += INSTRUCTION_SIZE;
            } while (patchOffset < OPTIMIZED_ENTRY_POINT.offset());

            final int auipcOffset = patchOffset;
            code.writeInt(patchOffset, AUIPC_X28);
            patchOffset += INSTRUCTION_SIZE;
            code.writeInt(patchOffset, LW_X29_X28_16);
            patchOffset += INSTRUCTION_SIZE;
            code.writeInt(patchOffset, ADD_X28_X28_X29);
            patchOffset += INSTRUCTION_SIZE;
            code.writeInt(patchOffset, JR_X28);
            patchOffset += INSTRUCTION_SIZE;
            final long callOffset = target.minus(code.plus(auipcOffset)).toLong();
            code.writeLong(patchOffset, callOffset);

            /*
             * After modifying instructions outside the permissible set the following cache maintenance is required
             * by the architecture. See B2.2.5 ARM ARM (issue E.a).
             */
            MaxineVM.maxine_cache_flush(code.plus(BASELINE_ENTRY_POINT.offset()), patchOffset);
            if (useSystemMembarrier()) {
                MaxineVM.syscall_membarrier();
            }
        }
    }

    /**
     * Indicate with the instruction in a target method at a given position is a jump to a specified destination. Used
     * in particular for testing if the entry points of a target method were patched to jump to a trampoline, and also
     * to validate itable/vtable entries.
     *
     * @param tm a target method
     * @param pos byte index relative to the start of the method to a call site
     * @param jumpTarget target to compare with the target of the assumed jump instruction
     * @return {@code true} if the instruction is a jump to the target, false otherwise
     */
    public static boolean isJumpTo(TargetMethod tm, int pos, CodePointer jumpTarget) {
        Pointer code = tm.codeAt(pos).toPointer();
        if (isRIPCall(code)) {
            CodePointer target = readCall32Target(tm.codeAt(pos));
            return jumpTarget.equals(target);
        }
        return false;
    }

    /**
     * Thread safe patching of the displacement field in a direct call.
     *
     * @return the target of the call prior to patching
     */
    public static CodePointer mtSafePatchCallDisplacement(TargetMethod tm, CodePointer callSite, CodePointer target) {
        if (!isPatchableCallSite(callSite)) {
            throw FatalError.unexpected(" invalid patchable call site:  " + callSite.toHexString());
        }
        CodePointer oldTarget = readCall32Target(callSite);
        if (!oldTarget.equals(target)) {
            synchronized (PatchingLock) {
                // Just to prevent concurrent writing and invalidation to the same instruction cache line
                // (although the lock excludes ALL concurrent patching)
                patchCallSite(tm, callSite, target.toPointer(), false);
            }
        }
        return oldTarget;
    }

    /**
     * Fixup the target displacement (28bit) in a branch immediate instruction.
     * Returns the old displacement.
     *
     * @param code - array containing the instruction
     * @param callOffset - offset of the call in code
     * @param displacement - the new displacement.
     * @return the previous displacement
     */
    @HOSTED_ONLY
    public static int fixupCall19Site(byte [] code, int callOffset, int displacement) {
        int instruction = extractInstruction(code, callOffset);
        assert isJumpInstruction(instruction) : "Not jump";
        CiRegister retRegister = isJumpLinked(instruction) ? RISCV64.ra : RISCV64.zero;
        int newBranch = jumpAndLinkImmediateHelper(retRegister, displacement);
        writeInstruction(code, callOffset, newBranch);
        return 0;
    }

    private static void writeInstruction(byte[] code, int offset, int instruction) {
        code[offset + 0] = (byte) (instruction         & 0xFF);
        code[offset + 1] = (byte) ((instruction >> 8)  & 0xFF);
        code[offset + 2] = (byte) ((instruction >> 16) & 0xFF);
        code[offset + 3] = (byte) ((instruction >> 24) & 0xFF);
    }

    /**
     * Fix up the target displacement in a branch immediate instruction.
     * Returns the old displacement.
     *
     * @param tm - the method containing the call
     * @param callOffset - the offset of the call in the methods code
     * @param target - the new target
     * @return the previous displacement
     */
    public static CodePointer fixupCall32Site(TargetMethod tm, int callOffset, CodePointer target) {
        CodePointer callSite = tm.codeAt(callOffset);
        if (MaxineVM.isHosted()) {
            long disp64 = target.toLong() - callSite.toLong();
            int disp32 = (int) disp64;
            assert disp64 == disp32 : "Code displacement out of 32-bit range";
            byte[] code = tm.code();
            if (NumUtil.isSignedNbit(20, disp32)) {
                final int oldDisplacement = fixupCall19Site(code, callOffset, disp32);
                return callSite.plus(oldDisplacement);
            } else {
                // locate the trampoline site that corresponds to the call site.
                final int pos = Safepoints.safepointPosForCall(callOffset, INSTRUCTION_SIZE);
                final int spIndex = tm.safepoints().indexOfCallAt(pos);
                final int trampolinesIndex = spIndex * TRAMPOLINE_SIZE;
                byte[] trampolines = tm.trampolines();
                assert trampolines != null : tm.classMethodActor() + " -- " + tm.name();
                assert readInt(trampolines, trampolinesIndex) == AUIPC_X28
                        && readInt(trampolines, trampolinesIndex + INSTRUCTION_SIZE) == LW_X29_X28_16
                        && readInt(trampolines, trampolinesIndex + 2 * INSTRUCTION_SIZE) == ADD_X28_X28_X29
                        && readInt(trampolines, trampolinesIndex + 3 * INSTRUCTION_SIZE) == JR_X28;
                final long oldOffset = readInt(trampolines, trampolinesIndex + TRAMPOLINE_OFFSET_OFFSET);

                CodePointer trampolineSite = tm.trampolineStart().plus(spIndex * TRAMPOLINE_SIZE);
                final int newOffset = target.minus(trampolineSite).toInt();
                writeInt(newOffset, trampolines, trampolinesIndex + TRAMPOLINE_OFFSET_OFFSET);

                final int trampolineOffset = trampolineSite.minus(callSite.toPointer()).toInt();
                final int oldDisplacement  = fixupCall19Site(code, callOffset, trampolineOffset);

                CodePointer callTarget = callSite.plus(oldDisplacement);

                if (oldDisplacement != trampolineOffset) {
                    return callTarget;
                }
                return trampolineSite.plus(oldOffset);

            }
        } else {
            return CodePointer.from(patchCallSite(tm, callSite, target.toPointer(), true));
        }
    }

    public static boolean isPatchableCallSite(CodePointer callSite) {
        final Address callSiteAddress = callSite.toAddress();
        return callSiteAddress.isAligned(4);
    }

    @HOSTED_ONLY
    private static boolean atFirstOrLastInstruction(StackFrameCursor current) {
        // check whether the current ip is at the first instruction or a return
        // which means the stack pointer has not been adjusted yet (or has already been adjusted back)
        TargetMethod tm = current.targetMethod();
        CodePointer entryPoint = tm.callEntryPoint.equals(CallEntryPoint.C_ENTRY_POINT) ? CallEntryPoint.C_ENTRY_POINT.in(tm) : CallEntryPoint.OPTIMIZED_ENTRY_POINT.in(tm);
        return entryPoint.equals(current.vmIP()) || current.stackFrameWalker().readInt(current.vmIP().toAddress(), 0) == RET;
    }

    /**
     * Advances the stack walker such that {@code current} becomes the callee.
     *
     * @param current the frame just visited by the current stack walk
     * @param csl the layout of the callee save area in {@code current}
     * @param csa the address of the callee save area in {@code current}
     */
    public static void advance(StackFrameCursor current, CiCalleeSaveLayout csl, Pointer csa) {
        assert csa.isZero() == (csl == null);
        TargetMethod tm = current.targetMethod();
        Pointer sp = current.sp();
        Pointer ripPointer = sp.plus(tm.frameSize());
        if (MaxineVM.isHosted()) {
            // Only during a stack walk in the context of the Inspector can execution
            // be anywhere other than at a safepoint.
            AdapterGenerator generator = AdapterGenerator.forCallee(current.targetMethod());
            if (generator != null && generator.advanceIfInPrologue(current)) {
                return;
            }
            if (atFirstOrLastInstruction(current)) {
                ripPointer = sp;
            }
        }

        StackFrameWalker sfw = current.stackFrameWalker();
        Pointer callerIP = sfw.readWord(ripPointer, 0).asPointer();
        // Skip the saved link register. Skip stackAlignment bytes since the push is 16-byte aligned as well
        Pointer callerSP = ripPointer.plus(Platform.target().stackAlignment);
        Pointer callerFP;
        if (!csa.isZero() && csl.contains(RISCV64.fp.number)) {
            callerFP = sfw.readWord(csa, csl.offsetOf(RISCV64.fp.number)).asPointer();
        } else {
            callerFP = current.fp();
        }
        current.setCalleeSaveArea(csl, csa);
        boolean wasDisabled = SafepointPoll.disable();
        sfw.advance(callerIP, callerSP, callerFP);
        if (!wasDisabled) {
            SafepointPoll.enable();
        }
    }

    public static boolean isRIPCall(Pointer callIP) {
        int instruction = callIP.readInt(0);
        return isJumpImmediateInstruction(instruction);
    }

    public static Pointer returnAddressPointer(StackFrameCursor frame) {
        TargetMethod tm = frame.targetMethod();
        Pointer sp = frame.sp();
        return sp.plus(tm.frameSize());
    }
}
