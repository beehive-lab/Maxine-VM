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
import com.oracle.max.asm.target.aarch64.*;
import com.sun.cri.ci.CiCalleeSaveLayout;
import com.sun.max.annotate.C_FUNCTION;
import com.sun.max.annotate.HOSTED_ONLY;
import com.sun.max.platform.Platform;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.CallEntryPoint;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.StackFrameCursor;
import com.sun.max.vm.stack.StackFrameWalker;

public final class Aarch64TargetMethodUtil {

    /**
     * Lock to avoid race on concurrent icache invalidation when patching target methods.
     */
    private static final Object PatchingLock = new Object();

    public static final int INSTRUCTION_SIZE = 4;
    public static final int NUMBER_OF_NOPS = 4;
    public static final int RIP_CALL_INSTRUCTION_SIZE = 5 * INSTRUCTION_SIZE;
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

    @C_FUNCTION
    public static native void maxine_cache_flush(Pointer start, int length);

    private static int getOldDisplacement(Pointer callSitePointer) {
        final int instruction = callSitePointer.readInt(0);
        if (Aarch64Assembler.isBimmInstruction(instruction)) {
            return Aarch64Assembler.bImmExtractDisplacement(instruction);
        } else {
            final Pointer nopSite = callSitePointer.minus(NUMBER_OF_NOPS * INSTRUCTION_SIZE);
            int movzInstruction = nopSite.readInt(4);
            int movkInstruction = nopSite.readInt(8);
            short low = Aarch64Assembler.movExtractImmediate(movzInstruction);
            short high = Aarch64Assembler.movExtractImmediate(movkInstruction);
            return high << 16 | low;
        }
    }

    /**
     * Thread safe patching of the displacement field in a direct call.
     *
     * @return the target of the call prior to patching
     */
    public static void mtSafePatchCallDisplacement(TargetMethod tm, CodePointer callSite, CodePointer target) {
        if (!isPatchableCallSite(callSite)) {
            throw FatalError.unexpected(" invalid patchable call site:  " + callSite.toHexString());
        }
        final long disp64 = target.toLong() - callSite.toLong();
        final Pointer callSitePointer = callSite.toPointer();
        final int oldDisp32 = getOldDisplacement(callSitePointer);
        if (oldDisp32 != disp64) {
            synchronized (PatchingLock) {
                // Just to prevent concurrent writing and invalidation to the same instruction cache line
                // (although the lock excludes ALL concurrent patching)
                fixupCall32Site(tm, callSite, target);
                // TODO (fz): invalidate icache?
            }
        }
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
    public static int fixupCall28Site(byte [] code, int callOffset, int displacement) {
        int instruction = extractInstruction(code, callOffset);
        int oldDisplacement = Aarch64Assembler.bImmExtractDisplacement(instruction);
        instruction = Aarch64Assembler.bImmPatch(instruction, displacement);
        code[callOffset + 0] = (byte) (instruction       & 0xFF);
        code[callOffset + 1] = (byte) (instruction >> 8  & 0xFF);
        code[callOffset + 2] = (byte) (instruction >> 16 & 0xFF);
        code[callOffset + 3] = (byte) (instruction >> 24 & 0xFF);
        return oldDisplacement;
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
            FatalError.check(disp64 == disp32, "Code displacement out of 32-bit range");
            assert NumUtil.isSignedNbit(28, disp32);
            byte[] code = tm.code();
            final int oldDisplacement = fixupCall28Site(code, callOffset, disp32);
            return callSite.plus(oldDisplacement);
        } else {
            return fixupCall32Site(tm, callSite, target);
        }
    }

    private static CodePointer fixupCall32Site(TargetMethod tm, CodePointer callSite, CodePointer target) {
        long disp64 = target.toLong() - callSite.toLong();
        int disp32 = (int) disp64;
        FatalError.check(disp64 == disp32, "Code displacement out of 32-bit range");
        final Pointer callSitePointer = callSite.toPointer();
        int oldDisplacement = getOldDisplacement(callSitePointer);
        final int instruction = callSitePointer.readInt(0);
        final boolean isLinked = Aarch64Assembler.isBranchInstructionLinked(instruction);

        if (NumUtil.isSignedNbit(28, disp32)) {
            // overwrite the four nops from com.oracle.max.asm.target.aarch64.Aarch64MacroAssembler.call()
            final Pointer nopSite = callSitePointer.minus(NUMBER_OF_NOPS * INSTRUCTION_SIZE);
            nopSite.writeInt(0, Aarch64Assembler.nopHelper());
            nopSite.writeInt(4, Aarch64Assembler.nopHelper());
            nopSite.writeInt(8, Aarch64Assembler.nopHelper());
            nopSite.writeInt(12, Aarch64Assembler.nopHelper());
            nopSite.writeInt(16, Aarch64Assembler.unconditionalBranchImmInstructionHelper(disp32, isLinked));
        } else {
            // Since adr is invoked NUMBER_OF_NOPS instructions before the actual branch we need to adjust the displacement
            FatalError.check(disp32 <= Integer.MAX_VALUE - (NUMBER_OF_NOPS * INSTRUCTION_SIZE), "Code displacement out of 32-bit range");
            disp32 += NUMBER_OF_NOPS * INSTRUCTION_SIZE;
            final boolean isNegative = disp32 < 0;
            if (isNegative) {
                disp32 = -disp32;
            }

            // overwrite the four nops from com.oracle.max.asm.target.aarch64.Aarch64MacroAssembler.call()
            final Pointer nopSite = callSitePointer.minus(NUMBER_OF_NOPS * INSTRUCTION_SIZE);
            nopSite.writeInt(0, Aarch64Assembler.adrHelper(Aarch64.r16, 0));
            nopSite.writeInt(4, Aarch64Assembler.movzHelper(64, Aarch64.r17, disp32 & 0xFFFF, 0));
            nopSite.writeInt(8, Aarch64Assembler.movkHelper(64, Aarch64.r17, (disp32 >> 16) & 0xFFFF, 16));
            nopSite.writeInt(12, Aarch64Assembler.addSubInstructionHelper(Aarch64.r16, Aarch64.r16, Aarch64.r17,
                                                                          isNegative));
            // overwrote the immediate branch with a register branch
            nopSite.writeInt(16, Aarch64Assembler.unconditionalBranchRegInstructionHelper(Aarch64.r16, isLinked));
        }

        return callSite.plus(oldDisplacement);
    }

    public static boolean isPatchableCallSite(CodePointer callSite) {
        final Address callSiteAddress = callSite.toAddress();
        return callSiteAddress.isWordAligned();
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
        if (!csa.isZero() && csl.contains(Aarch64.fp.getEncoding())) {
            callerFP = sfw.readWord(csa, csl.offsetOf(Aarch64.fp.getEncoding())).asPointer();
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

}
