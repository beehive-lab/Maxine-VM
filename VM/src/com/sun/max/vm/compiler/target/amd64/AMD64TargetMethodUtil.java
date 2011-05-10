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

import static com.sun.max.vm.compiler.target.TargetMethod.Flavor.*;

import com.oracle.max.asm.target.amd64.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.amd64.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.Cursor;
import com.sun.max.vm.stack.amd64.*;

/**
 * A utility class factoring out code common to all AMD64 target method.
 *
 * @author Bernd Mathiske
 * @author Paul Caprioli
 */
public final class AMD64TargetMethodUtil {
    public static final int RCALL = 0xe8;
    public static final int RJMP = 0xe9;
    public static final int RET = 0xc3;
    public static final int RIP_CALL_INSTRUCTION_SIZE = 5;

    /**
     * Lock to avoid race on concurrent icache invalidation when patching target methods.
     */
    private static final Object PatchingLock = new Object();

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
     * Patches the offset operand of a 32-bit relative CALL instruction.
     *
     * @param targetMethod the method containing the CALL instruction
     * @param callOffset the offset within the code of {@code targetMethod} of the CALL to be patched
     * @param destination the absolute target address of the CALL
     */
    public static void fixupCall32Site(TargetMethod targetMethod, int callOffset, Address destination) {
        fixupCode(targetMethod, callOffset, destination.asAddress(), RCALL);
    }

    private static final long DIRECT_METHOD_CALL_INSTRUCTION_LENGTH = 5L;

    private static void fixupCode(TargetMethod targetMethod, int offset, Address target, int controlTransferOpcode) {
        final Pointer callSite = targetMethod.codeStart().plus(offset);
        long displacement = target.minus(callSite.plus(DIRECT_METHOD_CALL_INSTRUCTION_LENGTH)).toLong();
        FatalError.check((int) displacement == displacement, "Code displacement out of 32-bit range");
        displacement = displacement & 0xFFFFFFFFL;
        if (MaxineVM.isHosted()) {
            final byte[] code = targetMethod.code();
            code[offset] = (byte) controlTransferOpcode;
            code[offset + 1] = (byte) displacement;
            code[offset + 2] = (byte) (displacement >> 8);
            code[offset + 3] = (byte) (displacement >> 16);
            code[offset + 4] = (byte) (displacement >> 24);
        } else {
            // Don't care about any particular alignment here. Can fixup any control of transfer code as there isn't concurrency issues.
            callSite.writeByte(0, (byte) controlTransferOpcode);
            callSite.writeByte(1, (byte) displacement);
            callSite.writeByte(2, (byte) (displacement >> 8));
            callSite.writeByte(3, (byte) (displacement >> 16));
            callSite.writeByte(4, (byte) (displacement >> 24));
        }
    }

    // MT-safe replacement of the displacement of a direct call.
    public static void mtSafePatchCallDisplacement(TargetMethod targetMethod, Pointer callSite, Address target) {
        if (!isPatchableCallSite(callSite)) {
            FatalError.unexpected(" invalid patchable call site:  " + callSite.toHexString());
        }
        long displacement = target.minus(callSite.plus(DIRECT_METHOD_CALL_INSTRUCTION_LENGTH)).toLong();
        FatalError.check((int) displacement == displacement, "Code displacement out of 32-bit range");
        displacement = displacement & 0xFFFFFFFFL;
        synchronized (PatchingLock) {
            // Just to prevent concurrent writing and invalidation to the same instruction cache line
            // (although the lock excludes ALL concurrent patching)
            callSite.writeInt(1,  (int) displacement);
            // Don't need icache invalidation to be correct (see AMD64's Architecture Programmer Manual Vol.2, p173 on self-modifying code)
        }
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
            // be anywhere other than at a recorded stop (i.e. call or safepoint).
            if (atFirstOrLastInstruction(current) || (generator != null && generator.inPrologue(current.ip(), current.targetMethod()))) {
                sp = sp.minus(current.targetMethod().frameSize());
            }
        }
        StackFrameWalker stackFrameWalker = current.stackFrameWalker();
        StackFrame stackFrame = new AMD64JavaStackFrame(stackFrameWalker.calleeStackFrame(), current.targetMethod(), current.ip(), sp, sp);
        return visitor.visitFrame(stackFrame);
    }

    public static void advance(Cursor current) {
        TargetMethod targetMethod = current.targetMethod();
        Pointer sp = current.sp();
        Pointer ripPointer = sp.plus(targetMethod.frameSize());
        if (MaxineVM.isHosted()) {
            // Only during a stack walk in the context of the Inspector can execution
            // be anywhere other than at a recorded stop (i.e. call or safepoint).
            AdapterGenerator generator = AdapterGenerator.forCallee(current.targetMethod());
            if (generator != null && generator.advanceIfInPrologue(current)) {
                return;
            }
            if (atFirstOrLastInstruction(current)) {
                ripPointer = sp;
            }
        }

        StackFrameWalker stackFrameWalker = current.stackFrameWalker();
        Pointer callerIP = stackFrameWalker.readWord(ripPointer, 0).asPointer();
        Pointer callerSP = ripPointer.plus(Word.size()); // Skip return instruction pointer on stack
        Pointer callerFP;
        if (targetMethod.is(TrapStub)) {
            // RBP is whatever was in the frame pointer register at the time of the trap
            Pointer calleeSaveArea = sp;
            callerFP = stackFrameWalker.readWord(calleeSaveArea, AMD64TrapStateAccess.CSA.offsetOf(AMD64.rbp)).asPointer();
        } else {
            // Propagate RBP unchanged as OPT methods do not touch this register.
            callerFP = current.fp();
        }
        stackFrameWalker.advance(callerIP, callerSP, callerFP, !targetMethod.is(TrapStub));
    }

}
