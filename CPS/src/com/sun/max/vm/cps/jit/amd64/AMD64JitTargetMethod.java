/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.cps.jit.amd64;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.jit.*;
import com.sun.max.vm.cps.target.amd64.*;
import com.sun.max.vm.stack.StackFrameWalker;
import com.sun.max.vm.stack.JitStackFrameLayout;

/**
 * @author Bernd Mathiske
 * @author Laurent Daynes
 * @author Doug Simon
 * @author Paul Caprioli
 */
public class AMD64JitTargetMethod extends JitTargetMethod {

    public AMD64JitTargetMethod(ClassMethodActor classMethodActor, RuntimeCompilerScheme compilerScheme) {
        super(classMethodActor, compilerScheme);
    }

    @Override
    public int callerInstructionPointerAdjustment() {
        return -1;
    }

    @Override
    public int bytecodePositionForCallSite(Pointer returnInstructionPointer) {
        // The instruction pointer is now just beyond the call machine instruction.
        // In case the call happens to be the last machine instruction for the invoke bytecode we are interested in, we subtract one byte.
        // Thus we always look up what bytecode we were in during the call.
        return bytecodePositionFor(returnInstructionPointer.minus(1));
    }

    @Override
    public final int registerReferenceMapSize() {
        return AMD64TargetMethod.registerReferenceMapSize();
    }

    @Override
    public final void patchCallSite(int callOffset, Word callEntryPoint) {
        AMD64TargetMethod.patchCall32Site(this, callOffset, callEntryPoint);
    }

    @Override
    public void forwardTo(TargetMethod newTargetMethod) {
        AMD64TargetMethod.forwardTo(this, newTargetMethod);
    }

    enum FramePointerState {
        /**
         * RBP holds the frame pointer of the current method activation. caller's RIP is at [RBP + FrameSize], caller's
         * frame pointer is at [RBP + FrameSize -1]
         */
        IN_RBP {

            @Override
            Pointer localVariablesBase(StackFrameWalker.Cursor current) {
                return current.fp();
            }

            @Override
            Pointer returnInstructionPointer(StackFrameWalker.Cursor current) {
                TargetMethod targetMethod = current.targetMethod();
                int dispToRip = targetMethod.frameSize() - sizeOfNonParameterLocals(targetMethod);
                return current.fp().plus(dispToRip);
            }

            @Override
            Pointer callerFramePointer(StackFrameWalker.Cursor current) {
                return current.stackFrameWalker().readWord(returnInstructionPointer(current), -Word.size()).asPointer();
            }
        },

        /**
         * RBP holds the frame pointer of the caller, caller's RIP is at [RSP] This state occurs when entering the
         * method or exiting it.
         */
        CALLER_FRAME_IN_RBP {

            @Override
            Pointer localVariablesBase(StackFrameWalker.Cursor current) {
                int offsetToSaveArea = current.targetMethod().frameSize();
                return current.sp().minus(offsetToSaveArea);
            }

            @Override
            Pointer returnInstructionPointer(StackFrameWalker.Cursor current) {
                return current.sp();
            }

            @Override
            Pointer callerFramePointer(StackFrameWalker.Cursor current) {
                return current.fp();
            }
        },

        /**
         * RBP points at the bottom of the "saving area". Caller's frame pointer is at [RBP], caller's RIP is at [RBP +
         * WordSize].
         */
        CALLER_FRAME_AT_RBP {

            @Override
            Pointer localVariablesBase(StackFrameWalker.Cursor current) {
                TargetMethod targetMethod = current.targetMethod();
                int dispToFrameStart = targetMethod.frameSize() - (sizeOfNonParameterLocals(targetMethod) + Word.size());
                return current.fp().minus(dispToFrameStart);
            }

            @Override
            Pointer returnInstructionPointer(StackFrameWalker.Cursor current) {
                return current.fp().plus(Word.size());
            }

            @Override
            Pointer callerFramePointer(StackFrameWalker.Cursor current) {
                return current.stackFrameWalker().readWord(current.fp(), 0).asPointer();
            }
        },

        /**
         * Returning from a runtime call (or actually in a runtime call). RBP may have been clobbered by the runtime.
         * The frame pointer for the current activation record is 'RSP + stack slot size'.
         */
        RETURNING_FROM_RUNTIME {

            @Override
            Pointer localVariablesBase(StackFrameWalker.Cursor current) {
                return current.stackFrameWalker().readWord(current.sp(), 0).asPointer();
            }

            @Override
            Pointer returnInstructionPointer(StackFrameWalker.Cursor current) {
                TargetMethod targetMethod = current.targetMethod();
                int dispToRip = targetMethod.frameSize() - sizeOfNonParameterLocals(targetMethod);
                return localVariablesBase(current).plus(dispToRip);
            }

            @Override
            Pointer callerFramePointer(StackFrameWalker.Cursor current) {
                return current.stackFrameWalker().readWord(returnInstructionPointer(current), -Word.size()).asPointer();
            }
        };

        abstract Pointer localVariablesBase(StackFrameWalker.Cursor current);

        abstract Pointer returnInstructionPointer(StackFrameWalker.Cursor current);

        abstract Pointer callerFramePointer(StackFrameWalker.Cursor current);

        int sizeOfNonParameterLocals(TargetMethod targetMethod) {
            return JitStackFrameLayout.JIT_SLOT_SIZE * (targetMethod.classMethodActor().codeAttribute().maxLocals - targetMethod.classMethodActor().numberOfParameterSlots());
        }
    }
}
