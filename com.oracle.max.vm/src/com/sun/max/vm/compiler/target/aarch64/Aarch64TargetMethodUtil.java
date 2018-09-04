/*
 * Copyright (c) 2018, APT Group, School of Computer Science,
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
package com.sun.max.vm.compiler.target.aarch64;

import com.oracle.max.asm.NumUtil;
import com.sun.cri.ci.CiCalleeSaveLayout;
import com.sun.max.annotate.HOSTED_ONLY;
import com.sun.max.platform.Platform;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.CallEntryPoint;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.arm.ARMTargetMethodUtil;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.StackFrameCursor;
import com.sun.max.vm.stack.StackFrameWalker;

import static com.oracle.max.asm.target.aarch64.Aarch64.*;
import static com.oracle.max.asm.target.aarch64.Aarch64Assembler.*;
import static com.oracle.max.asm.target.aarch64.Aarch64MacroAssembler.*;

public final class Aarch64TargetMethodUtil {

    /**
     * Lock to avoid race on concurrent icache invalidation when patching target methods.
     */
    private static final Object PatchingLock = new Object();

    public static final int RET = 0xD65F_0000;

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
     * Gets the target of a 32-bit relative CALL instruction.
     *
     * @param tm the method containing the CALL instruction
     * @param callPos the offset within the code of {@code targetMethod} of the CALL
     * @return the absolute target address of the CALL
     */
    public static CodePointer readCall32Target(TargetMethod tm, int callPos) {
        final CodePointer callSite = tm.codeAt(callPos);
        return readCall32Target(callSite);
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
        assert isBimmInstruction(instruction) : instruction;
        final int offset = bImmExtractDisplacement(instruction);
        assert offset == CALL_TRAMPOLINE1_OFFSET || offset == CALL_TRAMPOLINE2_OFFSET : offset;
        callSitePointer = callSitePointer.plus(offset);
        int displacement = getDisplacementFromTrampoline(callSitePointer);
        final CodePointer branchSite = callSite.plus(CALL_BRANCH_OFFSET);
        return branchSite.plus(displacement);
    }

    private static int getDisplacementFromTrampoline(Pointer callSitePointer) {
        int displacement;
        int movzInstruction   = callSitePointer.readInt(4);
        int movkInstruction   = callSitePointer.readInt(8);
        int addSubInstruction = callSitePointer.readInt(12);
        short low  = movExtractImmediate(movzInstruction);
        short high = movExtractImmediate(movkInstruction);
        displacement = high << 16 | low & 0xFFFF;
        if (!isAddInstruction(addSubInstruction)) {
            displacement = -displacement;
        }
        return displacement;
    }

    private static void patchCallTrampoline(Pointer patchSite, int displacement, boolean isLinked) {
        int instruction = patchSite.readInt(0);
        int offset = bImmExtractDisplacement(instruction);
        // The bimm offset must either point to one of the two trampolines or outside of them
        assert (offset == CALL_TRAMPOLINE1_OFFSET) || (offset == CALL_TRAMPOLINE2_OFFSET) : offset;
        // Get the offset of the unused trampoline
        offset = offset == CALL_TRAMPOLINE1_OFFSET ? CALL_TRAMPOLINE2_OFFSET : CALL_TRAMPOLINE1_OFFSET;
        // Create the new trampoline
        patchBranchRegister(patchSite, displacement, isLinked, offset);
    }

    private static void patchBranchRegister(Pointer patchSite, int displacement, boolean isLinked, int offset) {
        final boolean isNegative = displacement < 0;
        if (isNegative) {
            displacement = -displacement;
        }
        patchSite.writeInt(offset + 4, movzHelper(64, r16, displacement & 0xFFFF, 0));
        patchSite.writeInt(offset + 8, movkHelper(64, r16, (displacement >> 16) & 0xFFFF, 16));
        patchSite.writeInt(offset + 12, addSubInstructionHelper(r16, r17, r16, isNegative));
        patchSite.writeInt(CALL_BRANCH_OFFSET, unconditionalBranchRegInstructionHelper(r16, isLinked));
        // Patch the branch immediate to jump to the new trampoline
        patchSite.writeInt(0, unconditionalBranchImmInstructionHelper(offset, false));
        ARMTargetMethodUtil.maxine_cache_flush(patchSite, RIP_CALL_INSTRUCTION_SIZE);
    }

    private static void writeJump(Pointer patchSite, CodePointer target) {
        long disp64 = target.toLong() - patchSite.plus(CALL_BRANCH_OFFSET).toLong();
        int displacement = (int) disp64;
        assert displacement == disp64;
        int branchOffset = CALL_BRANCH_OFFSET - CALL_TRAMPOLINE1_OFFSET;
        patchSite.writeInt(CALL_TRAMPOLINE1_OFFSET, adrHelper(r17, branchOffset));
        branchOffset -= (CALL_TRAMPOLINE_INSTRUCTIONS - 1) * INSTRUCTION_SIZE;
        patchSite.writeInt(CALL_TRAMPOLINE1_OFFSET + 16, unconditionalBranchImmInstructionHelper(branchOffset, false));
        // Don't move this call higher since it flushes the cache
        patchBranchRegister(patchSite, displacement, false, CALL_TRAMPOLINE1_OFFSET);
    }

    /**
     * Patches a position in a target method with a direct jump to a given target address.
     *
     * @param tm the target method to be patched
     * @param pos the position in {@code tm} at which to apply the patch
     * @param target the target of the jump instruction being patched in
     */
    public static void patchWithJump(TargetMethod tm, int pos, CodePointer target) {
        // We must be at a global safepoint to safely patch TargetMethods
        FatalError.check(VmOperation.atSafepoint(), "should only be patching entry points when at a safepoint");

        final Pointer patchSite = tm.codeAt(pos).toPointer();

        synchronized (PatchingLock) {
            writeJump(patchSite, target);
        }
    }

    /**
     * Indicate with the instruction in a target method at a given position is a jump to a specified destination. Used
     * in particular for testing if the entry points of a target method were patched to jump to a trampoline.
     *
     * @param tm a target method
     * @param pos byte index relative to the start of the method to a call site
     * @param jumpTarget target to compare with the target of the assumed jump instruction
     * @return {@code true} if the instruction is a jump to the target, false otherwise
     */
    public static boolean isJumpTo(TargetMethod tm, int pos, CodePointer jumpTarget) {
        if (!isBranchInstruction(tm.codeAt(pos).toPointer().readInt(0))) {
            return false;
        }
        return readCall32Target(tm, pos).equals(jumpTarget);
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
                fixupCall32Site(callSite, target);
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
    public static int fixupCall28Site(byte [] code, int callOffset, int displacement) {
        final boolean isNegative = displacement < 0;
        if (isNegative) {
            displacement = -displacement;
        }
        int instruction = extractInstruction(code, callOffset);
        int offset = bImmExtractDisplacement(instruction);
        // The bimm offset must either point to one of the two trampolines or outside of them
        assert (offset == CALL_TRAMPOLINE1_OFFSET) || (offset == CALL_TRAMPOLINE2_OFFSET) : offset;
        // Get the offset of the unused trampoline
        offset = offset == CALL_TRAMPOLINE1_OFFSET ? CALL_TRAMPOLINE2_OFFSET : CALL_TRAMPOLINE1_OFFSET;
        final int trampolineOffset = callOffset + offset;
        // Create the new trampoline
        instruction = movzHelper(64, r16, displacement & 0xFFFF, 0);
        writeInstruction(code, trampolineOffset + 4, instruction);
        instruction = movkHelper(64, r16, (displacement >> 16) & 0xFFFF, 16);
        writeInstruction(code, trampolineOffset + 8, instruction);
        instruction = addSubInstructionHelper(r16, r17, r16, isNegative);
        writeInstruction(code, trampolineOffset + 12, instruction);
        instruction = extractInstruction(code, callOffset + CALL_BRANCH_OFFSET);
        final boolean isLinked = isBranchInstructionLinked(instruction);
        instruction = unconditionalBranchRegInstructionHelper(r16, isLinked);
        writeInstruction(code, callOffset + CALL_BRANCH_OFFSET, instruction);
        // Patch the branch immediate to jump to the new trampoline
        instruction = unconditionalBranchImmInstructionHelper(offset, false);
        writeInstruction(code, callOffset, instruction);
        return 0;
    }

    private static void writeInstruction(byte[] code, int offset, int instruction) {
        code[offset + 0] = (byte) (instruction       & 0xFF);
        code[offset + 1] = (byte) (instruction >> 8  & 0xFF);
        code[offset + 2] = (byte) (instruction >> 16 & 0xFF);
        code[offset + 3] = (byte) (instruction >> 24 & 0xFF);
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
            long disp64 = target.toLong() - callSite.plus(CALL_BRANCH_OFFSET).toLong();
            int disp32 = (int) disp64;
            FatalError.check(disp64 == disp32, "Code displacement out of 32-bit range");
            assert NumUtil.isSignedNbit(28, disp32);
            byte[] code = tm.code();
            final int oldDisplacement = fixupCall28Site(code, callOffset, disp32);
            return callSite.plus(oldDisplacement);
        } else {
            return fixupCall32Site(callSite, target);
        }
    }

    private static CodePointer fixupCall32Site(CodePointer callSite, CodePointer target) {
        final Pointer callSitePointer = callSite.toPointer();
        CodePointer oldTarget = readCall32Target(callSite);
        if (oldTarget.equals(target)) {
            return oldTarget;
        }
        assert isBimmInstruction(callSitePointer.readInt(0)) : callSitePointer.readInt(0);
        Pointer branchSitePointer = callSitePointer.plus(CALL_BRANCH_OFFSET);
        int instruction = branchSitePointer.readInt(0);
        final boolean isLinked = isBranchInstructionLinked(instruction);

        long disp64 = target.toLong() - branchSitePointer.toLong();
        int disp32 = (int) disp64;
        FatalError.check(disp64 == disp32, "Code displacement out of 32-bit range");
        patchCallTrampoline(callSitePointer, disp32, isLinked);

        return oldTarget;
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
        if (!csa.isZero() && csl.contains(fp.getEncoding())) {
            callerFP = sfw.readWord(csa, csl.offsetOf(fp.getEncoding())).asPointer();
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
        return isBimmInstruction(instruction)
            && (bImmExtractDisplacement(instruction) == CALL_TRAMPOLINE1_OFFSET
            || bImmExtractDisplacement(instruction) == CALL_TRAMPOLINE2_OFFSET);
    }

    public static Pointer returnAddressPointer(StackFrameCursor frame) {
        TargetMethod tm = frame.targetMethod();
        Pointer sp = frame.sp();
        return sp.plus(tm.frameSize());
    }
}
