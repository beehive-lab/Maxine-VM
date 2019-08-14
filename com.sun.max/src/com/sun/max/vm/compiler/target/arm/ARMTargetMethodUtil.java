/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
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
package com.sun.max.vm.compiler.target.arm;

import com.oracle.max.asm.target.armv7.*;
import com.oracle.max.cri.intrinsics.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.armv7.*;

public final class ARMTargetMethodUtil {

    public static final int RIP_CALL = ((ARMV7Assembler.ConditionFlag.Always.value() & 0xf) << 28) | (0x8 << 20) | (ARMV7.r15.getEncoding() << 12) | (ARMV7.r12.getEncoding() << 16);
    public static final int REG_CALL = ((ARMV7Assembler.ConditionFlag.Always.value() & 0xf) << 28) | (0xd << 21) | (ARMV7.r15.getEncoding() << 12) | ARMV7.r12.getEncoding();
    public static final int RIP_JMP = ((ARMV7Assembler.ConditionFlag.Always.value() & 0xf) << 28) | (0x8 << 20) | (ARMV7.r15.getEncoding() << 12) | (ARMV7.r12.getEncoding() << 16);
    public static final int RET = ((ARMV7Assembler.ConditionFlag.Always.value() & 0xf) << 28) | (0x8 << 24) | (0xb << 20) | (0xd << 16) | (1 << 15);
    public static final int RIP_CALL_INSTRUCTION_SIZE = 16;

    /**
     * Lock to avoid race on concurrent icache invalidation when patching target methods.
     */
    private static final Object PatchingLock = new Object();
    private static final boolean JUMP_WITH_LINK = true;

    public static int registerReferenceMapSize() {
        return UnsignedMath.divide(ARMV7.cpuRegisters.length, Bytes.WIDTH);
    }

    public static boolean isPatchableCallSite(CodePointer callSite) {
        final Address callSiteAddress = callSite.toAddress();
        return callSiteAddress.isWordAligned();
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
        int disp32;
        if (MaxineVM.isHosted()) {
            final byte[] code = tm.code();
            assert code[0] == (byte) RIP_CALL;
            disp32 = (code[callPos + 4] & 0xff) << 24 | (code[callPos + 3] & 0xff) << 16 | (code[callPos + 2] & 0xff) << 8 | (code[callPos + 1] & 0xff) << 0;
        } else {
            final Pointer callSitePointer = callSite.toPointer();
            disp32 = 0;
            if (((callSitePointer.readByte(3) & 0xff) == 0xe3) && ((callSitePointer.readByte(4 + 3) & 0xff) == 0xe3)) {
                // just enough checking to make sure it has been patched before ...
                // and does not contain nops
                disp32 = (callSitePointer.readByte(4 + 0) & 0xff) | ((callSitePointer.readByte(4 + 1) & 0xf) << 8) | ((callSitePointer.readByte(4 + 2) & 0xf) << 12);
                disp32 = disp32 << 16;
                disp32 += (callSitePointer.readByte(0) & 0xff) | ((callSitePointer.readByte(1) & 0xf) << 8) | ((callSitePointer.readByte(2) & 0xf) << 12);
            }
        }
        return callSite.plus(RIP_CALL_INSTRUCTION_LENGTH).plus(disp32);
    }

    private static void manipulateBuffer(byte[] code, int callOffset, int instruction) {
        code[callOffset + 3] = (byte) (instruction & 0xff);
        code[callOffset + 2] = (byte) ((instruction >> 8) & 0xff);
        code[callOffset + 1] = (byte) ((instruction >> 16) & 0xff);
        code[callOffset] = (byte) ((instruction >> 24) & 0xff);
    }

    /**
     * Patches the offset operand of a 32-bit relative CALL instruction.
     *
     * @param tm the method containing the CALL instruction
     * @param callOffset the offset within the code of {@code targetMethod} of the CALL to be patched
     * @param target the absolute target address of the CALL
     * @return the target of the call prior to patching
     */
    public static CodePointer fixupCall32Site(TargetMethod tm, int callOffset, CodePointer target) {
        CodePointer callSite = tm.codeAt(callOffset);
        if (!isPatchableCallSite(callSite)) {
            // Every call site that is fixed up here might also be patched later. To avoid failed patching,
            // check for alignment of call site also here.
            // TODO(cwi): This is a check that I would like to have, however, T1X does not ensure proper alignment yet
            // when it stitches together templates that contain calls.
            FatalError.unexpected(" invalid patchable call site:  " + tm + "+" + callOffset + " " + callSite.toHexString());
        }

        long disp64 = target.toLong() - callSite.plus(RIP_CALL_INSTRUCTION_LENGTH).toLong();
        int disp32 = (int) disp64;
        FatalError.check(disp64 == disp32, "Code displacement out of 32-bit range");
        int oldDisp32 = 0;

        if (MaxineVM.isHosted()) {
            final byte[] code = tm.code();
            if (true) {
                if (JUMP_WITH_LINK) {
                    int instruction = ARMV7Assembler.movwHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, disp32 & 0xffff);
                    code[callOffset + 0] = (byte) (instruction & 0xff);
                    code[callOffset + 1] = (byte) ((instruction >> 8) & 0xff);
                    code[callOffset + 2] = (byte) ((instruction >> 16) & 0xff);
                    code[callOffset + 3] = (byte) ((instruction >> 24) & 0xff);
                    int tmp32 = disp32 >> 16;
                    instruction = ARMV7Assembler.movtHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, tmp32 & 0xffff);
                    code[callOffset + 4] = (byte) (instruction & 0xff);
                    code[callOffset + 5] = (byte) ((instruction >> 8) & 0xff);
                    code[callOffset + 6] = (byte) ((instruction >> 16) & 0xff);
                    code[callOffset + 7] = (byte) ((instruction >> 24) & 0xff);
                    instruction = ARMV7Assembler.addRegistersHelper(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r12, ARMV7.r15, ARMV7.r12, 0, 0);
                    code[callOffset + 8] = (byte) (instruction & 0xff);
                    code[callOffset + 9] = (byte) ((instruction >> 8) & 0xff);
                    code[callOffset + 10] = (byte) ((instruction >> 16) & 0xff);
                    code[callOffset + 11] = (byte) ((instruction >> 24) & 0xff);
                    instruction = ARMV7Assembler.blxHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12);
                    code[callOffset + 12] = (byte) (instruction & 0xff);
                    code[callOffset + 13] = (byte) ((instruction >> 8) & 0xff);
                    code[callOffset + 14] = (byte) ((instruction >> 16) & 0xff);
                    code[callOffset + 15] = (byte) ((instruction >> 24) & 0xff);
                } else {
                    int instruction = ARMV7Assembler.movwHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, disp32 & 0xffff);
                    code[callOffset + 0] = (byte) (instruction & 0xff);
                    code[callOffset + 1] = (byte) ((instruction >> 8) & 0xff);
                    code[callOffset + 2] = (byte) ((instruction >> 16) & 0xff);
                    code[callOffset + 3] = (byte) ((instruction >> 24) & 0xff);
                    int tmp32 = disp32 >> 16;
                    tmp32 = tmp32 & 0xffff;
                    instruction = ARMV7Assembler.movtHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, tmp32 & 0xffff);
                    code[callOffset + 4] = (byte) (instruction & 0xff);
                    code[callOffset + 5] = (byte) ((instruction >> 8) & 0xff);
                    code[callOffset + 6] = (byte) ((instruction >> 16) & 0xff);
                    code[callOffset + 7] = (byte) ((instruction >> 24) & 0xff);
                    instruction = ARMV7Assembler.addRegistersHelper(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r15, ARMV7.r15, ARMV7.r12, 0, 0);
                    code[callOffset + 8] = (byte) (instruction & 0xff);
                    code[callOffset + 9] = (byte) ((instruction >> 8) & 0xff);
                    code[callOffset + 10] = (byte) ((instruction >> 16) & 0xff);
                    code[callOffset + 11] = (byte) ((instruction >> 24) & 0xff);
                }
            }
        } else {
            final Pointer callSitePointer = callSite.toPointer();
            oldDisp32 = 0;
            if (((callSitePointer.readByte(3) & 0xff) == 0xe3) && ((callSitePointer.readByte(4 + 3) & 0xff) == 0xe3)) {
                // just enough checking to make sure it has been patched before ...
                // and does not contain nops
                oldDisp32 = (callSitePointer.readByte(4 + 0) & 0xff) | ((callSitePointer.readByte(4 + 1) & 0xf) << 8) | ((callSitePointer.readByte(4 + 2) & 0xf) << 12);
                oldDisp32 = oldDisp32 << 16;
                oldDisp32 += (callSitePointer.readByte(0) & 0xff) | ((callSitePointer.readByte(1) & 0xf) << 8) | ((callSitePointer.readByte(2) & 0xf) << 12);

            }
            int instruction = ARMV7Assembler.movwHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, disp32 & 0xffff);
            callSitePointer.writeByte(0, (byte) (instruction & 0xff));
            callSitePointer.writeByte(1, (byte) ((instruction >> 8) & 0xff));
            callSitePointer.writeByte(2, (byte) ((instruction >> 16) & 0xff));
            callSitePointer.writeByte(3, (byte) ((instruction >> 24) & 0xff));
            int tmp32 = disp32 >> 16;
            instruction = ARMV7Assembler.movtHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, tmp32 & 0xffff);
            callSitePointer.writeByte(4, (byte) (instruction & 0xff));
            callSitePointer.writeByte(5, (byte) ((instruction >> 8) & 0xff));
            callSitePointer.writeByte(6, (byte) ((instruction >> 16) & 0xff));
            callSitePointer.writeByte(7, (byte) ((instruction >> 24) & 0xff));
            instruction = ARMV7Assembler.addRegistersHelper(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r12, ARMV7.r15, ARMV7.r12, 0, 0);
            callSitePointer.writeByte(8, (byte) (instruction & 0xff));
            callSitePointer.writeByte(9, (byte) ((instruction >> 8) & 0xff));
            callSitePointer.writeByte(10, (byte) ((instruction >> 16) & 0xff));
            callSitePointer.writeByte(11, (byte) ((instruction >> 24) & 0xff));
            instruction = ARMV7Assembler.blxHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12);
            callSitePointer.writeByte(12, (byte) (instruction & 0xff));
            callSitePointer.writeByte(13, (byte) ((instruction >> 8) & 0xff));
            callSitePointer.writeByte(14, (byte) ((instruction >> 16) & 0xff));
            callSitePointer.writeByte(15, (byte) ((instruction >> 24) & 0xff));
            int checkDISP = 0;
            if (((callSitePointer.readByte(3) & 0xff) == 0xe3) && ((callSitePointer.readByte(4 + 3) & 0xff) == 0xe3)) {
                // just enough checking to make sure it has been patched before ...
                // and does not contain nops
                checkDISP = (callSitePointer.readByte(4 + 0) & 0xff) | ((callSitePointer.readByte(4 + 1) & 0xf) << 8) | ((callSitePointer.readByte(4 + 2) & 0xf) << 12);
                checkDISP = checkDISP << 16;
                checkDISP += (callSitePointer.readByte(0) & 0xff) | ((callSitePointer.readByte(1) & 0xf) << 8) | ((callSitePointer.readByte(2) & 0xf) << 12);
            }
        }
        return callSite.plus(RIP_CALL_INSTRUCTION_LENGTH).plus(oldDisp32);
    }

    private static final int RIP_CALL_INSTRUCTION_LENGTH = 16;
    private static final int RIP_JMP_INSTRUCTION_LENGTH = 4;

    public static int ripCallOffset(TargetMethod tm, Pointer callSitePointer) {
        // changed to use Pointers rather than CodePointers as we got an exception due to the
        // masking off of the MSB giving -ve values which then lead to a read of an illegal memory location
        int movw = callSitePointer.readInt(0);
        int movt = callSitePointer.readInt(4);
        int low = (movw & 0xfff) | ((movw & 0xf0000) >> 4);
        int high = (movt & 0xfff) | ((movt & 0xf0000) >> 4);
        high = (high << 16) | low;
        assert (0xe3000000 == (movw & 0xfff00000)) && (0xe3400000 == (movt & 0xfff00000)) : "Instruction sequence is wrong!";
        return high + 8 + RIP_CALL_INSTRUCTION_LENGTH;
    }

    public static boolean isARMV7RIPCall(TargetMethod tm, Pointer callSitePointer) {
        // changed to use Pointers rather than CodePointers as we got an exception due to the
        // masking off of the MSB giving -ve values which then lead to a read of an illegal memory location

        int addInstrn = ARMV7Assembler.addRegistersHelper(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r12, ARMV7.r15, ARMV7.r12, 0, 0);
        int blxInstrn = ARMV7Assembler.blxHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12);
        if ((callSitePointer.readInt(8) == addInstrn) && callSitePointer.readInt(12) == blxInstrn) {
            return true;
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
        final Pointer callSitePointer = callSite.toPointer();

        long disp64 = target.toLong() - callSite.plus(RIP_CALL_INSTRUCTION_LENGTH).toLong();
        int disp32 = (int) disp64;
        FatalError.check(disp64 == disp32, "Code displacement out of 32-bit range");
        int oldDisp32 = 0;
        if (((callSitePointer.readByte(3) & 0xff) == 0xe3) && ((callSitePointer.readByte(4 + 3) & 0xff) == 0xe3)) {
            // just enough checking to make sure it has been patched before ...
            // and does not contain nops
            oldDisp32 = (callSitePointer.readByte(4 + 0) & 0xff) | ((callSitePointer.readByte(4 + 1) & 0xf) << 8) | ((callSitePointer.readByte(4 + 2) & 0xf) << 12);
            oldDisp32 = oldDisp32 << 16;
            oldDisp32 += (callSitePointer.readByte(0) & 0xff) | ((callSitePointer.readByte(1) & 0xf) << 8) | ((callSitePointer.readByte(2) & 0xf) << 12);
        }

        if (oldDisp32 != disp64) {
            synchronized (PatchingLock) {
                // Just to prevent concurrent writing and invalidation to the same instruction cache line
                // callSitePointer.writeInt(1, disp32);
                int instruction = ARMV7Assembler.movwHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, disp32 & 0xffff);
                callSitePointer.writeByte(0, (byte) (instruction & 0xff));
                callSitePointer.writeByte(1, (byte) ((instruction >> 8) & 0xff));
                callSitePointer.writeByte(2, (byte) ((instruction >> 16) & 0xff));
                callSitePointer.writeByte(3, (byte) ((instruction >> 24) & 0xff));
                int tmp32 = disp32 >> 16;
                instruction = ARMV7Assembler.movtHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, tmp32 & 0xffff);
                callSitePointer.writeByte(4, (byte) (instruction & 0xff));
                callSitePointer.writeByte(5, (byte) ((instruction >> 8) & 0xff));
                callSitePointer.writeByte(6, (byte) ((instruction >> 16) & 0xff));
                callSitePointer.writeByte(7, (byte) ((instruction >> 24) & 0xff));
                instruction = ARMV7Assembler.addRegistersHelper(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r12, ARMV7.r15, ARMV7.r12, 0, 0);
                callSitePointer.writeByte(8, (byte) (instruction & 0xff));
                callSitePointer.writeByte(9, (byte) ((instruction >> 8) & 0xff));
                callSitePointer.writeByte(10, (byte) ((instruction >> 16) & 0xff));
                callSitePointer.writeByte(11, (byte) ((instruction >> 24) & 0xff));
                instruction = ARMV7Assembler.blxHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12);
                callSitePointer.writeByte(12, (byte) (instruction & 0xff));
                callSitePointer.writeByte(13, (byte) ((instruction >> 8) & 0xff));
                callSitePointer.writeByte(14, (byte) ((instruction >> 16) & 0xff));
                callSitePointer.writeByte(15, (byte) ((instruction >> 24) & 0xff));
                MaxineVM.maxine_cache_flush(callSitePointer, 24);
            }
        }
        return callSite.plus(RIP_CALL_INSTRUCTION_LENGTH).plus(oldDisp32);
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
        CodePointer callSite = tm.codeAt(pos);
        if (!isPatchableCallSite(callSite)) {
            throw FatalError.unexpected(" invalid patchable call site:  " + callSite.toHexString());
        }
        final Pointer callSitePointer = callSite.toPointer();

        long disp64 = target.toLong() - callSite.plus(RIP_CALL_INSTRUCTION_LENGTH).toLong();
        int disp32 = (int) disp64;
        FatalError.check(disp64 == disp32, "Code displacement out of 32-bit range");
        int oldDisp32 = 0;
        if (((callSitePointer.readByte(3) & 0xff) == 0xe3) && ((callSitePointer.readByte(4 + 3) & 0xff) == 0xe3)) {
            // just enough checking to make sure it has been patched before ...
            // and does not contain nops
            oldDisp32 = (callSitePointer.readByte(4 + 0) & 0xff) | ((callSitePointer.readByte(4 + 1) & 0xf) << 8) | ((callSitePointer.readByte(4 + 2) & 0xf) << 12);
            oldDisp32 = oldDisp32 << 16;
            oldDisp32 += (callSitePointer.readByte(0) & 0xff) | ((callSitePointer.readByte(1) & 0xf) << 8) | ((callSitePointer.readByte(2) & 0xf) << 12);
        }

        if (oldDisp32 != disp64) {
            synchronized (PatchingLock) {
                // Just to prevent concurrent writing and invalidation to the same instruction cache line
                // callSitePointer.writeInt(1, disp32);
                int instruction = ARMV7Assembler.movwHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, disp32 & 0xffff);
                callSitePointer.writeByte(0, (byte) (instruction & 0xff));
                callSitePointer.writeByte(1, (byte) ((instruction >> 8) & 0xff));
                callSitePointer.writeByte(2, (byte) ((instruction >> 16) & 0xff));
                callSitePointer.writeByte(3, (byte) ((instruction >> 24) & 0xff));
                int tmp32 = disp32 >> 16;
                instruction = ARMV7Assembler.movtHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, tmp32 & 0xffff);
                callSitePointer.writeByte(4, (byte) (instruction & 0xff));
                callSitePointer.writeByte(5, (byte) ((instruction >> 8) & 0xff));
                callSitePointer.writeByte(6, (byte) ((instruction >> 16) & 0xff));
                callSitePointer.writeByte(7, (byte) ((instruction >> 24) & 0xff));
                instruction = ARMV7Assembler.addRegistersHelper(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r12, ARMV7.r15, ARMV7.r12, 0, 0);
                callSitePointer.writeByte(8, (byte) (instruction & 0xff));
                callSitePointer.writeByte(9, (byte) ((instruction >> 8) & 0xff));
                callSitePointer.writeByte(10, (byte) ((instruction >> 16) & 0xff));
                callSitePointer.writeByte(11, (byte) ((instruction >> 24) & 0xff));
                instruction = ARMV7Assembler.movHelper(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r15, ARMV7.r12);
                callSitePointer.writeByte(12, (byte) (instruction & 0xff));
                callSitePointer.writeByte(13, (byte) ((instruction >> 8) & 0xff));
                callSitePointer.writeByte(14, (byte) ((instruction >> 16) & 0xff));
                callSitePointer.writeByte(15, (byte) ((instruction >> 24) & 0xff));
                MaxineVM.maxine_cache_flush(callSitePointer, 24);
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
        return readCall32Target(tm, pos).equals(jumpTarget);
    }

    private ARMTargetMethodUtil() {
    }

    @HOSTED_ONLY
    public static boolean atFirstOrLastInstruction(StackFrameCursor current) {
        // check whether the current ip is at the first instruction or a return
        // which means the stack pointer has not been adjusted yet (or has already been adjusted back)
        TargetMethod tm = current.targetMethod();
        CodePointer entryPoint = tm.callEntryPoint.equals(CallEntryPoint.C_ENTRY_POINT) ? CallEntryPoint.C_ENTRY_POINT.in(tm) : CallEntryPoint.OPTIMIZED_ENTRY_POINT.in(tm);
        return entryPoint.equals(current.vmIP()) || current.stackFrameWalker().readInt(current.vmIP().toAddress(), 0) == RET;

    }

    @HOSTED_ONLY
    public static boolean acceptStackFrameVisitor(StackFrameCursor current, StackFrameVisitor visitor) {
        AdapterGenerator generator = AdapterGenerator.forCallee(current.targetMethod());
        Pointer sp = current.sp();
        // Only during a stack walk in the context of the Inspector can execution
        // be anywhere other than at a safepoint.
        if (atFirstOrLastInstruction(current) || (generator != null && generator.inPrologue(current.vmIP(), current.targetMethod()))) {
            sp = sp.minus(current.targetMethod().frameSize());
        }
        StackFrameWalker stackFrameWalker = current.stackFrameWalker();
        StackFrame stackFrame = new ARMV7JavaStackFrame(stackFrameWalker.calleeStackFrame(), current.targetMethod(), current.vmIP().toPointer(), sp, sp);
        return visitor.visitFrame(stackFrame);
    }

    public static VMFrameLayout frameLayout(TargetMethod tm) {
        return new OptoStackFrameLayout(tm.frameSize(), true, ARMV7.r13);
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
        Pointer callerSP = ripPointer.plus(Word.size()); // Skip return instruction pointer on stack
        Pointer callerFP;
        if (!csa.isZero() && csl.contains(ARMV7.r11.getEncoding())) {
            callerFP = sfw.readWord(csa, csl.offsetOf(ARMV7.r11.getEncoding())).asPointer();
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

    public static Pointer returnAddressPointer(StackFrameCursor frame) {
        TargetMethod tm = frame.targetMethod();
        Pointer sp = frame.sp();
        return sp.plus(tm.frameSize());
    }
}
