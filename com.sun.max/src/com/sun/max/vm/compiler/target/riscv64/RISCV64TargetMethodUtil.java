/*
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
import com.oracle.max.asm.target.riscv64.*;
import com.sun.cri.ci.CiCalleeSaveLayout;
import com.sun.max.annotate.HOSTED_ONLY;
import com.sun.max.platform.Platform;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.CallEntryPoint;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.StackFrameCursor;
import com.sun.max.vm.stack.StackFrameWalker;

import static com.oracle.max.asm.target.riscv64.RISCV64MacroAssembler.*;

public final class RISCV64TargetMethodUtil {

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
        throw new UnsupportedOperationException("Unimplemented");
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

        long disp64 = target.toLong() - patchSite.toLong();
        int disp32 = (int) disp64;
        FatalError.check(disp64 == disp32, "Code displacement out of 32-bit range");

        if (NumUtil.isSignedNbit(28, disp32)) {
            synchronized (PatchingLock) {
                throw new UnsupportedOperationException("Unimplemented");
//                patchSite.writeInt(0, RISCV64Assembler.unconditionalBranchImmInstructionHelper(disp32, false));
//                ARMTargetMethodUtil.maxine_cache_flush(patchSite, 4);
            }
        } else {
            // Since adr is invoked NUMBER_OF_NOPS instructions before the actual branch we need to adjust the displacement
            final boolean isNegative = disp32 < 0;
            if (isNegative) {
                disp32 = -disp32;
            }

            synchronized (PatchingLock) {
                throw new UnsupportedOperationException("Unimplemented");
//                patchSite.writeInt(0, RISCV64Assembler.adrHelper(RISCV64.r16, 0));
//                patchSite.writeInt(4, RISCV64Assembler.movzHelper(64, RISCV64.r17, disp32 & 0xFFFF, 0));
//                patchSite.writeInt(8, RISCV64Assembler.movkHelper(64, RISCV64.r17, (disp32 >> 16) & 0xFFFF, 16));
//                patchSite.writeInt(12, RISCV64Assembler.addSubInstructionHelper(RISCV64.r16, RISCV64.r16, RISCV64.r17, isNegative));
//                patchSite.writeInt(16, RISCV64Assembler.unconditionalBranchRegInstructionHelper(RISCV64.r16, false));
//                ARMTargetMethodUtil.maxine_cache_flush(patchSite, 20);
            }
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
        CodePointer callSite = tm.codeAt(pos);
        Pointer callSitePointer = callSite.toPointer();
        int instruction = callSitePointer.readInt(0);
        // first check for branch immediate (jump with 28bit displacement)
        throw new UnsupportedOperationException("Unimplemented");
//        if (RISCV64Assembler.isBimmInstruction(instruction)) {
//            return readCall32Target(callSite).equals(jumpTarget);
//        }
//        // Else check for jumps with 32bit displacement. Move the callSite pointer to the actual branch instruction.
//        callSite = callSite.plus(NUMBER_OF_NOPS * INSTRUCTION_SIZE);
//        callSitePointer = callSite.toPointer();
//        instruction = callSitePointer.readInt(0);
//        if (RISCV64Assembler.isBranchInstruction(instruction)) {
//            return readCall32Target(callSite).equals(jumpTarget);
//        }
//        return false;
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
     * Fixup the target displacement (19 bit) in a branch immediate instruction.
     * Returns the old displacement.
     *
     * @param code - array containing the instruction
     * @param callOffset - offset of the call in code
     * @param displacement - the new displacement.
     * @return the previous displacement
     */
    public static int fixupCall19Site(byte [] code, int callOffset, int displacement) {
        throw new UnsupportedOperationException("Unimplemented");
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
            final int oldDisplacement = fixupCall19Site(code, callOffset, disp32);
            return callSite.plus(oldDisplacement);
        } else {
            return fixupCall32Site(callSite, target);
        }
    }

    private static CodePointer fixupCall32Site(CodePointer callSite, CodePointer target) {
        throw new UnsupportedOperationException("Unimplemented");
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
        if (!csa.isZero() && csl.contains(RISCV64.fp.getEncoding())) {
            callerFP = sfw.readWord(csa, csl.offsetOf(RISCV64.fp.getEncoding())).asPointer();
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
        throw new UnsupportedOperationException("Unimplemented");
//        int instruction = callIP.readInt(0);
//        // If it is not a branch instruction then it is definitely not a RIP Call
//        if (!RISCV64MacroAssembler.isBranchInstruction(instruction)) {
//            return false;
//        }
//        // If it is not a branch immediate we need to check the previous instructions as well
//        if (!RISCV64MacroAssembler.isBimmInstruction(instruction)) {
//            final Pointer nopSite = callIP.minus(NUMBER_OF_NOPS * INSTRUCTION_SIZE);
//            int movzInstruction = nopSite.readInt(4);
//            if (!RISCV64MacroAssembler.isMovz(movzInstruction)) {
//                return false;
//            }
//            int movkInstruction = nopSite.readInt(8);
//            if (!RISCV64MacroAssembler.isMovk(movkInstruction)) {
//                return false;
//            }
//            int addSubInstruction = nopSite.readInt(12);
//            if (!RISCV64Assembler.isAddSubInstruction(addSubInstruction)) {
//                return false;
//            }
//        }
//        return true;
    }
}
