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
package com.sun.max.vm.stack.amd64;

import com.sun.max.vm.stack.*;
import com.sun.max.vm.compiler.target.TargetMethod;
import com.sun.max.unsafe.Pointer;
import com.sun.max.unsafe.Word;

/**
 * The <code>AMD64AdapterStackWalking</code> class definition.
 * @author Ben L. Titzer
 */
public class AMD64AdapterStackWalking {
    public static final byte RET2 = (byte) 0xC2;
    static final byte PUSH_RBP = (byte) 0x55;
    static final byte ENTER = (byte) 0xC8;
    static final byte SHORT_JMP = (byte) 0xEB;
    static final byte NEAR_JMP = (byte) 0xE9;
    /**
     * Length in bytes of a short jump instruction on AMD64 (1 byte instruction encoding + 8 bits displacement).
     */
    static final int SHORT_JMP_SIZE = 2;
    /**
     * Length in bytes of a near jump instruction on AMD64 (1 byte instruction encoding + 32 bits displacement).
     */
    static final int NEAR_JMP_SIZE = 5;

    static boolean walkJitOptAdapterFrame(StackFrameWalker.Cursor current, StackFrameWalker stackFrameWalker, TargetMethod targetMethod, StackFrameWalker.Purpose purpose, Object context, Pointer startOfAdapter, boolean isTopFrame) {
        final Pointer jitEntryPoint = com.sun.max.vm.compiler.CallEntryPoint.JIT_ENTRY_POINT.in(targetMethod);
        final int adapterFrameSize = jitToOptimizingAdapterFrameSize(stackFrameWalker, startOfAdapter);
        Pointer callerFramePointer = current.fp();

        Pointer ripPointer = current.sp(); // stack pointer at call entry point (where the RIP is).
        final byte firstInstructionByte = stackFrameWalker.readByte(current.ip(), 0);
        if (!current.ip().equals(jitEntryPoint) && !current.ip().equals(startOfAdapter) && firstInstructionByte != RET2) {
            ripPointer = current.sp().plus(adapterFrameSize);
            callerFramePointer = stackFrameWalker.readWord(ripPointer, -Word.size()).asPointer();
        }

        final Pointer callerInstructionPointer = stackFrameWalker.readWord(ripPointer, 0).asPointer();
        switch (purpose) {
            case EXCEPTION_HANDLING: {
                // adapter frames do not have exception handlers
                break;
            }
            case REFERENCE_MAP_PREPARING: {
                break;
            }
            case RAW_INSPECTING: {
                final RawStackFrameVisitor stackFrameVisitor = (RawStackFrameVisitor) context;
                final int flags = RawStackFrameVisitor.Util.makeFlags(isTopFrame, true);
                stackFrameVisitor.visitFrame(targetMethod, callerInstructionPointer, current.fp(), current.sp(), flags);
                break;
            }
            case INSPECTING: {
                final StackFrameVisitor stackFrameVisitor = (StackFrameVisitor) context;
                final StackFrame stackFrame = new AdapterStackFrame(stackFrameWalker.calleeStackFrame(), new AdapterStackFrameLayout(adapterFrameSize, true), targetMethod, current.ip(), current.fp(), current.sp());
                if (!stackFrameVisitor.visitFrame(stackFrame)) {
                    return false;
                }
                break;
            }
        }
        stackFrameWalker.advance(callerInstructionPointer, ripPointer.plus(Word.size() /* skip RIP */), callerFramePointer);
        return true;
    }

    static boolean inJitOptAdapterFrameCode(boolean inTopFrame, final Pointer ip, final Pointer optimizedEntryPoint, final Pointer startOfAdapter) {
        if (ip.lessThan(optimizedEntryPoint)) {
            return true;
        }
        if (inTopFrame) {
            return ip.greaterEqual(startOfAdapter);
        }
        // Since we are not in the top frame, instructionPointer is really the return instruction pointer of
        // a call. If it happens that the call is to a method that is never expected to return normally (e.g. a method that only exits by throwing an exception),
        // the call may well be the very last instruction in the method prior to the adapter frame code.
        // In this case, we're only in adapter frame code if the instructionPointer is greater than
        // the start of the adapter frame code.
        return ip.greaterThan(startOfAdapter);
    }

    /**
     * Returns the adapter frame size. The size is deduced from the first instruction of the adapter, which decreases the
     * stack pointer if the adapter has a frame of size greater than 0. This is the only sub instruction in an adapter
     * frame, so if the first instruction isn't a sub instruction, the size of the frame is 0.
     *
     * @param stackFrameWalker the stack frame walker
     * @param adapterFirstInstruction the first instruction of the adapter code
     * @return the size of the adapter frame in bytes
     */
    public static int jitToOptimizingAdapterFrameSize(StackFrameWalker stackFrameWalker, Pointer adapterFirstInstruction) {
        final byte instruction = stackFrameWalker.readByte(adapterFirstInstruction, 0);
        if (instruction == ENTER) {
            final int lo = stackFrameWalker.readByte(adapterFirstInstruction, 1) & 0xff;
            final int hi = stackFrameWalker.readByte(adapterFirstInstruction, 2) & 0xff;
            final int frameSize = hi << 8 | lo;
            return frameSize + Word.size();
        }
        if (instruction == PUSH_RBP) {
            // Frame size == a single slot for saving RBP
            return Word.size();
        }
        return 0;
    }

    static boolean advanceJitOptAdapterFrame(StackFrameWalker.Cursor current) {
        StackFrameWalker stackFrameWalker = current.stackFrameWalker();
        TargetMethod targetMethod = current.targetMethod();
        Pointer startOfAdapter = jitOptAdapterCodeStart(stackFrameWalker, targetMethod);
        final Pointer jitEntryPoint = com.sun.max.vm.compiler.CallEntryPoint.JIT_ENTRY_POINT.in(targetMethod);
        final int adapterFrameSize = jitToOptimizingAdapterFrameSize(stackFrameWalker, startOfAdapter);
        Pointer callerFramePointer = current.fp();

        Pointer ripPointer = current.sp(); // stack pointer at call entry point (where the RIP is).
        final byte firstInstructionByte = stackFrameWalker.readByte(current.ip(), 0);
        if (!current.ip().equals(jitEntryPoint) && !current.ip().equals(startOfAdapter) && firstInstructionByte != RET2) {
            ripPointer = current.sp().plus(adapterFrameSize);
            callerFramePointer = stackFrameWalker.readWord(ripPointer, -Word.size()).asPointer();
        }

        final Pointer callerInstructionPointer = stackFrameWalker.readWord(ripPointer, 0).asPointer();
        stackFrameWalker.advance(callerInstructionPointer, ripPointer.plus(Word.size() /* skip RIP */), callerFramePointer);
        return true;
    }

    /**
     * Returns the target of the jump instruction at the JIT entry point.
     * <p/>
     * The target is the first instruction in a target method compiled by the JIT compiler, the frame adapter in target
     * method compiled by the optimizing compiler.
     * <p/>
     * FIXME: This method may be invoked by the inspector with an incomplete targetMethod object,
     * (i.e., one without the _targetABI field correctly set. Because of this, we pass an extra parameter to ease
     * figuring out what offset the jump instruction is (normally, we could figure this out with
     * targetABI().callEntryPoint()). Fix the inspector so that TargetMethod are always provided with a TargetABI
     * object.
     *
     * @param stackFrameWalker the stack frame walker
     * @param targetMethod the target method
     * @return a pointer to the start of the code of the adapter frame
     */
    public static Pointer jitOptAdapterCodeStart(StackFrameWalker stackFrameWalker, TargetMethod targetMethod) {
        final Pointer jitEntryPoint = com.sun.max.vm.compiler.CallEntryPoint.JIT_ENTRY_POINT.in(targetMethod);
        final byte jumpInstruction = stackFrameWalker.readByte(jitEntryPoint, 0);
        int distance;
        if (jumpInstruction == SHORT_JMP) {
            distance = SHORT_JMP_SIZE + stackFrameWalker.readByte(jitEntryPoint, 1);
        } else if (jumpInstruction == NEAR_JMP) {
            distance = NEAR_JMP_SIZE + stackFrameWalker.readInt(jitEntryPoint, 1);
        } else {
            // (tw) Did not find a jump here => return max
            return Pointer.zero();
        }
        return jitEntryPoint.plus(distance);
    }
}
