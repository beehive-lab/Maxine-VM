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
import static com.sun.max.vm.compiler.CallEntryPoint.JIT_ENTRY_POINT;
import static com.sun.max.vm.compiler.CallEntryPoint.OPTIMIZED_ENTRY_POINT;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.jit.*;
import com.sun.max.vm.cps.target.amd64.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.amd64.AMD64JavaStackFrame;
import com.sun.max.vm.stack.amd64.AMD64OptStackWalking;
import com.sun.max.vm.stack.amd64.AMD64OptimizedToJitAdapterFrame;
import com.sun.max.vm.Log;
import com.sun.max.vm.runtime.Safepoint;
import com.sun.max.lang.Bytes;
import com.sun.max.program.ProgramError;

/**
 * @author Bernd Mathiske
 * @author Laurent Daynes
 * @author Doug Simon
 * @author Paul Caprioli
 */
public class AMD64JitTargetMethod extends JitTargetMethod {

    private static final byte ENTER = (byte) 0xC8;
    private static final byte LEAVE = (byte) 0xC9;
    private static final byte POP_RBP = (byte) 0x5D;

    private static final byte RET = (byte) 0xC3;
    private static final byte RET2 = (byte) 0xC2;


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
            Pointer returnIP(StackFrameWalker.Cursor current) {
                TargetMethod targetMethod = current.targetMethod();
                int dispToRip = targetMethod.frameSize() - sizeOfNonParameterLocals(targetMethod);
                return current.fp().plus(dispToRip);
            }

            @Override
            Pointer callerFP(StackFrameWalker.Cursor current) {
                return current.stackFrameWalker().readWord(returnIP(current), -Word.size()).asPointer();
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
            Pointer returnIP(StackFrameWalker.Cursor current) {
                return current.sp();
            }

            @Override
            Pointer callerFP(StackFrameWalker.Cursor current) {
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
            Pointer returnIP(StackFrameWalker.Cursor current) {
                return current.fp().plus(Word.size());
            }

            @Override
            Pointer callerFP(StackFrameWalker.Cursor current) {
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
            Pointer returnIP(StackFrameWalker.Cursor current) {
                TargetMethod targetMethod = current.targetMethod();
                int dispToRip = targetMethod.frameSize() - sizeOfNonParameterLocals(targetMethod);
                return localVariablesBase(current).plus(dispToRip);
            }

            @Override
            Pointer callerFP(StackFrameWalker.Cursor current) {
                return current.stackFrameWalker().readWord(returnIP(current), -Word.size()).asPointer();
            }
        };

        abstract Pointer localVariablesBase(StackFrameWalker.Cursor current);

        abstract Pointer returnIP(StackFrameWalker.Cursor current);

        abstract Pointer callerFP(StackFrameWalker.Cursor current);

        int sizeOfNonParameterLocals(TargetMethod targetMethod) {
            return JitStackFrameLayout.JIT_SLOT_SIZE * (targetMethod.classMethodActor().codeAttribute().maxLocals - targetMethod.classMethodActor().numberOfParameterSlots());
        }
    }

    @Override
    public void prepareReferenceMap(StackFrameWalker.Cursor current, StackFrameWalker.Cursor callee, StackReferenceMapPreparer preparer) {
        if (inAdapterCode(current)) {
            // no references in adapter frame, because the parameters become locals in the JIT frame
            return;
        }
        finalizeReferenceMaps();
        Pointer startOfPrologue = JIT_ENTRY_POINT.in(this);
        Pointer lastPrologueInstruction = startOfPrologue.plus(AMD64JitCompiler.OFFSET_TO_LAST_PROLOGUE_INSTRUCTION);
        FramePointerState framePointerState = computeFramePointerState(current, current.stackFrameWalker(), lastPrologueInstruction);
        Pointer localVariablesBase = framePointerState.localVariablesBase(current);

        if (callee.calleeKind() == StackFrameWalker.CalleeKind.TRAMPOLINE) {
            // TODO: handle call of trampoline!
            ProgramError.unexpected();
        }

        int stopIndex = findClosestStopIndex(current.ip());
        int frameReferenceMapSize = frameReferenceMapSize();

        // prepare the map for this stack frame
        Pointer slotPointer = localVariablesBase.plus(frameReferenceMapOffset);
        preparer.tracePrepareReferenceMap(this, stopIndex, slotPointer, "JIT frame");
        int byteIndex = stopIndex * frameReferenceMapSize;
        for (int i = 0; i < frameReferenceMapSize; i++) {
            preparer.setReferenceMapBits(current, slotPointer, referenceMaps[byteIndex] & 0xff, Bytes.WIDTH);
            slotPointer = slotPointer.plusWords(Bytes.WIDTH);
            byteIndex++;
        }
    }

    @Override
    public void catchException(StackFrameWalker.Cursor current, StackFrameWalker.Cursor callee, Throwable throwable) {
        StackFrameWalker stackFrameWalker = current.stackFrameWalker();
        Address throwAddress = current.ip();
        if (inAdapterCode(current)) {
            // adapter frames cannot catch exceptions
            return;
        }

        Address catchAddress = throwAddressToCatchAddress(current.isTopFrame(), throwAddress, throwable.getClass());

        if (!catchAddress.isZero()) {
            if (StackFrameWalker.TRACE_STACK_WALK.getValue()) {
                Log.print("StackFrameWalk: Handler position for exception at position ");
                Log.print(throwAddress.minus(codeStart()).toInt());
                Log.print(" is ");
                Log.println(catchAddress.minus(codeStart()).toInt());
            }

            Pointer startOfPrologue = JIT_ENTRY_POINT.in(this);
            Pointer lastPrologueInstruction = startOfPrologue.plus(AMD64JitCompiler.OFFSET_TO_LAST_PROLOGUE_INSTRUCTION);
            FramePointerState framePointerState = computeFramePointerState(current, stackFrameWalker, lastPrologueInstruction);
            Pointer localVariablesBase = framePointerState.localVariablesBase(current);
            // The Java operand stack of the method that handles the exception is always cleared.
            // A null object is then pushed to ensure the depth of the stack is as expected upon
            // entry to an exception handler. However, the handler must have a prologue that loads
            // the exception from VmThreadLocal.EXCEPTION_OBJECT which is indeed guaranteed by
            // ExceptionDispatcher.
            // Compute the offset to the first stack slot of the Java Stack: frame size - (space for locals + saved RBP
            // + space of the first slot itself).
            Pointer catcherStackPointer = localVariablesBase.minus(framePointerState.sizeOfNonParameterLocals(this) + JitStackFrameLayout.JIT_SLOT_SIZE);
            // Push the null object on top of the stack first
            catcherStackPointer.writeReference(0, null);

            // found an exception handler, and thus we are done with the stack walker
            stackFrameWalker.reset();

            // Completes the exception handling protocol (with respect to the garbage collector) initiated in
            // Throwing.raise()
            Safepoint.enable();

            AMD64JitCompiler.unwind(throwable, catchAddress, catcherStackPointer, localVariablesBase);
            // We should never reach here
        }
    }

    @Override
    public boolean acceptStackFrameVisitor(StackFrameWalker.Cursor current, StackFrameWalker.Cursor callee, StackFrameVisitor visitor) {
        StackFrameWalker stackFrameWalker = current.stackFrameWalker();
        StackFrame stackFrame;
        if (inAdapterCode(current)) {
            stackFrame = new AMD64OptimizedToJitAdapterFrame(stackFrameWalker.calleeStackFrame(), current.targetMethod(), current.ip(), current.fp(), current.sp());
        } else {
            stackFrame = new AMD64JavaStackFrame(stackFrameWalker.calleeStackFrame(), current.targetMethod(), current.ip(), current.fp(), current.sp());
        }
        return visitor.visitFrame(stackFrame);
    }

    @Override
    public void advance(StackFrameWalker.Cursor current) {
        StackFrameWalker stackFrameWalker = current.stackFrameWalker();
        // FIXME: need to encapsulate adapter frame related code in an
        // adapter frame scheme so that this code does not comprise any adapter related code.
        Pointer jitEntryPoint = JIT_ENTRY_POINT.in(this);
        Pointer optimizedEntryPoint = OPTIMIZED_ENTRY_POINT.in(this);
        boolean hasAdapterFrame = !jitEntryPoint.equals(optimizedEntryPoint);

        // points to the first instruction following the prologue of the JIT-ed method, whether there is an adapter
        // embedded in the code or not.
        Pointer startOfPrologue;
        if (hasAdapterFrame) {
            Pointer endOfAdapter = optimizedEntryPoint.plus(this.optimizedCallerAdapterFrameCodeSize());
            if (current.ip().greaterEqual(optimizedEntryPoint) && current.ip().lessThan(endOfAdapter)) {
                advanceAdapterFrame(current);
                return;
            }
            startOfPrologue = endOfAdapter;
        } else {
            startOfPrologue = jitEntryPoint;
        }
        Pointer lastPrologueInstruction = startOfPrologue.plus(AMD64JitCompiler.OFFSET_TO_LAST_PROLOGUE_INSTRUCTION);
        FramePointerState framePointerState = computeFramePointerState(current, stackFrameWalker, lastPrologueInstruction);

        Pointer returnIP = framePointerState.returnIP(current);
        Pointer callerIP = stackFrameWalker.readWord(returnIP, 0).asPointer();
        Pointer callerSP = returnIP.plus(Word.size()); // Skip the rip
        stackFrameWalker.advance(callerIP, callerSP, framePointerState.callerFP(current));
    }

    private void advanceAdapterFrame(StackFrameWalker.Cursor current) {
        StackFrameWalker stackFrameWalker = current.stackFrameWalker();
        Pointer instructionPointer = current.ip();
        Pointer stackPointer = current.sp();
        Pointer entryPoint = OPTIMIZED_ENTRY_POINT.in(this);
        Pointer ripPointer = adapterReturnInstructionPointer(instructionPointer, stackPointer, entryPoint);
        Pointer callerInstructionPointer = stackFrameWalker.readWord(ripPointer, 0).asPointer();

        Pointer callerSP = ripPointer.plus(Word.size()); // skip RIP word
        stackFrameWalker.advance(callerInstructionPointer, callerSP, callerSP);
    }

    private Pointer adapterReturnInstructionPointer(Pointer ip, Pointer sp, Pointer entryPoint) {
        TargetMethod targetMethod = this;
        ClassMethodActor classMethodActor = targetMethod.classMethodActor();
        // Currently, the opto-jit adapter frame always increases the stack by at least one slot, to make it looks like
        // a call from a

        boolean hasNoFrame = ip.equals(entryPoint) || classMethodActor.isStatic() && (classMethodActor.descriptor().numberOfParameters() == 0);

        Pointer ripPointer; // stack pointer at call entry point (where the RIP is).
        if (hasNoFrame) {
            ripPointer = sp;
        } else {
            // The adapter frame was constructed. Add space taken by the parameters for the placeholder of RBP.
            ripPointer = sp.plus(AMD64OptStackWalking.adapterFrameSize(classMethodActor));
        }
        return ripPointer;
    }

    private FramePointerState computeFramePointerState(StackFrameWalker.Cursor current, StackFrameWalker stackFrameWalker, Pointer lastPrologueInstr) {
        Pointer instructionPointer = current.ip();
        byte byteAtInstructionPointer = stackFrameWalker.readByte(instructionPointer, 0);
        if (instructionPointer.lessThan(lastPrologueInstr) || byteAtInstructionPointer == ENTER || byteAtInstructionPointer == RET || byteAtInstructionPointer == RET2) {
            return FramePointerState.CALLER_FRAME_IN_RBP;
        }
        if (instructionPointer.equals(lastPrologueInstr) || byteAtInstructionPointer == LEAVE) {
            return FramePointerState.CALLER_FRAME_AT_RBP;
        }
        if (byteAtInstructionPointer == POP_RBP) {
            return FramePointerState.RETURNING_FROM_RUNTIME;
        }
        return FramePointerState.IN_RBP;
    }

    private boolean inAdapterCode(StackFrameWalker.Cursor current) {
        Pointer jitEntryPoint = JIT_ENTRY_POINT.in(this);
        Pointer optimizedEntryPoint = OPTIMIZED_ENTRY_POINT.in(this);

        // points to the first instruction following the prologue of the JIT-ed method, whether there is an adapter
        // embedded in the code or not.
        if (!jitEntryPoint.equals(optimizedEntryPoint)) {
            Pointer endOfAdapter = optimizedEntryPoint.plus(this.optimizedCallerAdapterFrameCodeSize());
            if (current.ip().greaterEqual(optimizedEntryPoint) && current.ip().lessThan(endOfAdapter)) {
                return true;
            }
        }
        return false;
    }
}
