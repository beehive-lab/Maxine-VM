/*
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
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.compiler.target.amd64;

import static com.sun.max.vm.compiler.deopt.Deoptimization.*;
import static com.sun.max.vm.compiler.target.Stub.Type.*;

import com.oracle.max.asm.target.amd64.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.Cursor;
import com.sun.max.vm.stack.amd64.*;

/**
 * A utility class factoring out code common to all AMD64 target method.
 */
public final class AMD64TargetMethodUtil {

    /**
     * Opcode of a RIP-relative call instruction.
     */
    public static final int RIP_CALL = 0xe8;

    /**
     * Opcode of a register-based call instruction.
     */
    public static final int REG_CALL = 0xff;

    /**
     * Opcode of a RIP-relative jump instruction.
     */
    public static final int RIP_JMP = 0xe9;

    /**
     * Opcode of a (near) return instruction.
     */
    public static final int RET = 0xc3;

    /**
     * Size (in bytes) of a RIP-relative call instruction.
     */
    public static final int RIP_CALL_INSTRUCTION_SIZE = 5;

    /**
     * Lock to avoid race on concurrent icache invalidation when patching target methods.
     */
    private static final Object PatchingLock = new Object(); // JavaMonitorManager.newVmLock("PATCHING_LOCK");

    public static int registerReferenceMapSize() {
        return Unsigned.idiv(AMD64.cpuRegisters.length, Bytes.WIDTH);
    }

    public static boolean isPatchableCallSite(Address callSite) {
        // We only update the disp of the call instruction.
        // C1X imposes that disp of the call be aligned to a word boundary.
        // This may cause up to 7 nops to be inserted before a call.
        final Address endOfCallSite = callSite.plus(DIRECT_METHOD_CALL_INSTRUCTION_LENGTH - 1);
        return callSite.plus(1).isWordAligned() ? true :
        // last byte of call site:
        callSite.roundedDownBy(8).equals(endOfCallSite.roundedDownBy(8));
    }

    /**
     * Gets the target of a 32-bit relative CALL instruction.
     *
     * @param targetMethod the method containing the CALL instruction
     * @param callOffset the offset within the code of {@code targetMethod} of the CALL
     * @return the absolute target address of the CALL
     */
    public static Address readCall32Target(TargetMethod targetMethod, int callOffset) {
        final Pointer callSite = targetMethod.codeStart().plus(callOffset);
        int disp32;
        if (MaxineVM.isHosted()) {
            final byte[] code = targetMethod.code();
            assert code[0] == (byte) RIP_CALL;
            disp32 =
                (code[callOffset + 4] & 0xff) << 24 |
                (code[callOffset + 3] & 0xff) << 16 |
                (code[callOffset + 2] & 0xff) << 8 |
                (code[callOffset + 1] & 0xff) << 0;
        } else {
            assert callSite.readByte(0) == (byte) RIP_CALL;
            disp32 =
                (callSite.readByte(4) & 0xff) << 24 |
                (callSite.readByte(3) & 0xff) << 16 |
                (callSite.readByte(2) & 0xff) << 8 |
                (callSite.readByte(1) & 0xff) << 0;
        }
        return callSite.plus(DIRECT_METHOD_CALL_INSTRUCTION_LENGTH).plus(disp32);
    }

    /**
     * Patches the offset operand of a 32-bit relative CALL instruction.
     *
     * @param targetMethod the method containing the CALL instruction
     * @param callOffset the offset within the code of {@code targetMethod} of the CALL to be patched
     * @param target the absolute target address of the CALL
     * @return the target of the call prior to patching
     */
    public static Address fixupCall32Site(TargetMethod targetMethod, int callOffset, Address target) {
        final Pointer callSite = targetMethod.codeStart().plus(callOffset);
        if (!isPatchableCallSite(callSite)) {
            // Every call site that is fixed up here might also be patched later.  To avoid failed patching,
            // check for alignment of call site also here.
            // TODO(cwi): This is a check that I would like to have, however, T1X does not ensure proper alignment yet when it stitches together templates that contain calls.
            // FatalError.unexpected(" invalid patchable call site:  " + targetMethod + "+" + offset + " " + callSite.toHexString());
        }

        long disp64 = target.asAddress().minus(callSite.plus(DIRECT_METHOD_CALL_INSTRUCTION_LENGTH)).toLong();
        int disp32 = (int) disp64;
        int oldDisp32;
        FatalError.check(disp64 == disp32, "Code displacement out of 32-bit range");
        if (MaxineVM.isHosted()) {
            final byte[] code = targetMethod.code();
            oldDisp32 =
                (code[callOffset + 4] & 0xff) << 24 |
                (code[callOffset + 3] & 0xff) << 16 |
                (code[callOffset + 2] & 0xff) << 8 |
                (code[callOffset + 1] & 0xff) << 0;
            if (oldDisp32 != disp32) {
                code[callOffset] = (byte) RIP_CALL;
                code[callOffset + 1] = (byte) disp32;
                code[callOffset + 2] = (byte) (disp32 >> 8);
                code[callOffset + 3] = (byte) (disp32 >> 16);
                code[callOffset + 4] = (byte) (disp32 >> 24);
            }
        } else {
            oldDisp32 =
                (callSite.readByte(4) & 0xff) << 24 |
                (callSite.readByte(3) & 0xff) << 16 |
                (callSite.readByte(2) & 0xff) << 8 |
                (callSite.readByte(1) & 0xff) << 0;
            if (oldDisp32 != disp32) {
                callSite.writeByte(0, (byte) RIP_CALL);
                callSite.writeByte(1, (byte) disp32);
                callSite.writeByte(2, (byte) (disp32 >> 8));
                callSite.writeByte(3, (byte) (disp32 >> 16));
                callSite.writeByte(4, (byte) (disp32 >> 24));
            }
        }
        return callSite.plus(DIRECT_METHOD_CALL_INSTRUCTION_LENGTH).plus(oldDisp32);
    }

    private static final long DIRECT_METHOD_CALL_INSTRUCTION_LENGTH = 5L;

    /**
     * Thread safe patching of the displacement field in a direct call.
     *
     * @return the target of the call prior to patching
     */
    public static Address mtSafePatchCallDisplacement(TargetMethod targetMethod, Pointer callSite, Address target) {
        if (!isPatchableCallSite(callSite)) {
            throw FatalError.unexpected(" invalid patchable call site:  " + callSite.toHexString());
        }
        long disp64 = target.minus(callSite.plus(DIRECT_METHOD_CALL_INSTRUCTION_LENGTH)).toLong();
        int disp32 = (int) disp64;
        FatalError.check(disp64 == disp32, "Code displacement out of 32-bit range");
        int oldDisp32 = callSite.readInt(1);
        if (oldDisp32 != disp64) {
            synchronized (PatchingLock) {
                // Just to prevent concurrent writing and invalidation to the same instruction cache line
                // (although the lock excludes ALL concurrent patching)
                callSite.writeInt(1,  disp32);
                // Don't need icache invalidation to be correct (see AMD64's Architecture Programmer Manual Vol.2, p173 on self-modifying code)
            }
        }
        return callSite.plus(DIRECT_METHOD_CALL_INSTRUCTION_LENGTH).plus(oldDisp32);
    }

    // Disable instance creation.
    private AMD64TargetMethodUtil() {
    }

    @HOSTED_ONLY
    public static boolean atFirstOrLastInstruction(Cursor current) {
        // check whether the current ip is at the first instruction or a return
        // which means the stack pointer has not been adjusted yet (or has already been adjusted back)
        TargetMethod targetMethod = current.targetMethod();
        Pointer entryPoint = targetMethod.callEntryPoint.equals(CallEntryPoint.C_ENTRY_POINT) ?
            CallEntryPoint.C_ENTRY_POINT.in(targetMethod) :
            CallEntryPoint.OPTIMIZED_ENTRY_POINT.in(targetMethod);

        return entryPoint.equals(current.ip()) || current.stackFrameWalker().readByte(current.ip(), 0) == RET;
    }

    public static boolean acceptStackFrameVisitor(Cursor current, StackFrameVisitor visitor) {
        AdapterGenerator generator = AdapterGenerator.forCallee(current.targetMethod());
        Pointer sp = current.sp();
        if (MaxineVM.isHosted()) {
            // Only during a stack walk in the context of the Inspector can execution
            // be anywhere other than at a safepoint.
            if (atFirstOrLastInstruction(current) || (generator != null && generator.inPrologue(current.ip(), current.targetMethod()))) {
                sp = sp.minus(current.targetMethod().frameSize());
            }
        }
        StackFrameWalker stackFrameWalker = current.stackFrameWalker();
        StackFrame stackFrame = new AMD64JavaStackFrame(stackFrameWalker.calleeStackFrame(), current.targetMethod(), current.ip(), sp, sp);
        return visitor.visitFrame(stackFrame);
    }

    public static VMFrameLayout frameLayout(TargetMethod targetMethod) {
        return new OptoStackFrameLayout(targetMethod.frameSize(), true, AMD64.rsp);
    }

    /**
     * Advances the stack walker such that {@code current} becomes the callee.
     *
     * @param current the frame just visited by the current stack walk
     * @param csl the layout of the callee save area in {@code current}
     * @param csa the address of the callee save area in {@code current}
     */
    public static void advance(Cursor current, CiCalleeSaveLayout csl, Pointer csa) {
        assert csa.isZero() == (csl == null);
        TargetMethod targetMethod = current.targetMethod();
        Pointer sp = current.sp();
        Pointer ripPointer = sp.plus(targetMethod.frameSize());
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
        if (!csa.isZero() && csl.contains(AMD64.rbp.number)) {
            // Read RBP from the callee save area
            callerFP = sfw.readWord(csa, csl.offsetOf(AMD64.rbp)).asPointer();
        } else {
            // Propagate RBP unchanged
            callerFP = current.fp();
        }

        // Rescue a return address that has been patched for deoptimization
        callerIP = rescuePatchedReturnAddress(sfw, callerIP, callerSP);

        current.setCalleeSaveArea(csl, csa);
        sfw.advance(callerIP, callerSP, callerFP, !targetMethod.is(TrapStub));
    }

    /**
     * Gets the original return address for a method on the stack that may be deoptimized when it is returned to.
     *
     * @param callerIP the return address read from the "current" frame
     * @param callerSP the stack pointer in the caller's frame
     * @return the value of {@code callerIP} altered if necessary to "undo" the patching done for deoptimization
     */
    public static Pointer rescuePatchedReturnAddress(StackFrameWalker sfw, Pointer callerIP, Pointer callerSP) {
        TargetMethod caller = sfw.targetMethodFor(callerIP);
        if (caller != null && (caller.is(DeoptStub) || caller.is(DeoptStubFromCompilerStub) || caller.is(DeoptStubFromSafepoint))) {
            if (callerIP.equals(caller.codeStart())) {
                // Since callerIP denotes the start of a deopt stub, then we're dealing with a patched return address
                // and the real caller is found in the 'rescue' slot
                Pointer originalReturnAddress = sfw.readWord(callerSP, DEOPT_RETURN_ADDRESS_OFFSET).asPointer();
                callerIP = originalReturnAddress;
            } else {
                // Since callerIP denotes an address within a deopt stub, then the caller really is the deopt stub
            }
        }
        return callerIP;
    }

    public static Pointer returnAddressPointer(Cursor frame) {
        TargetMethod targetMethod = frame.targetMethod();
        Pointer sp = frame.sp();
        return sp.plus(targetMethod.frameSize());
    }

    public static int callInstructionSize(byte[] code, int pos) {
        if ((code[pos] & 0xFF) == RIP_CALL) {
            return RIP_CALL_INSTRUCTION_SIZE;
        }
        if ((code[pos] & 0xff) == REG_CALL) {
            return 2;
        }
        if ((code[pos + 1] & 0xff) == REG_CALL) {
            return 3;
        }
        return -1;
    }
}
