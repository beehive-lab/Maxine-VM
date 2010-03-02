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

import com.sun.max.annotate.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.amd64.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.trampoline.*;
import com.sun.max.vm.type.*;

/**
 * This class collects together stack-walking related functionality that is (somewhat) compiler-independent.
 * Mostly, this means stack walking functionality that is shared between methods produced by the
 * CPS and C1X compilers.
 *
 * @author Ben L. Titzer
 */
public class AMD64OptStackWalking {

    public static final int RIP_CALL_INSTRUCTION_SIZE = 5;
    public static final byte RET = (byte) 0xC3;

    private static int unwindFrameSize = -1;
    private static ClassMethodActor unwindMethod;

    private static int getUnwindFrameSize() {
        if (unwindFrameSize == -1) {
            unwindFrameSize = CompilationScheme.Static.getCurrentTargetMethod(unwindMethod).frameSize();
        }
        return unwindFrameSize;
    }

    @NEVER_INLINE
    public static void unwindToCalleeEpilogue(Throwable throwable, Address catchAddress, Pointer stackPointer, TargetMethod lastJavaCallee) {
        // Overwrite return address of callee with catch address
        final Pointer returnAddressPointer = stackPointer.minus(Word.size());
        returnAddressPointer.setWord(catchAddress);

        assert lastJavaCallee.registerRestoreEpilogueOffset() != -1;
        Address epilogueAddress = lastJavaCallee.codeStart().plus(lastJavaCallee.registerRestoreEpilogueOffset());

        final Pointer calleeStackPointer = stackPointer.minus(Word.size()).minus(lastJavaCallee.frameSize());
        unwindOptimized(throwable, epilogueAddress, calleeStackPointer, Pointer.zero());
    }

    /**
     * Unwinds a thread's stack to an exception handler.
     * <p/>
     * The compiled version of this method must have its own frame but the frame size must be known at image build
     * time. This is because this code manually adjusts the stack pointer.
     * <p/>
     * The critical state of the registers before the RET instruction is:
     * <ul>
     * <li>RAX must hold the exception object</li>
     * <li>RSP must be one word less than the stack pointer of the handler frame that is the target of the unwinding</li>
     * <li>The value at [RSP] must be the address of the handler code</li>
     * </ul>
     *
     * @param throwable    the exception object
     * @param catchAddress the address of the exception handler code
     * @param stackPointer the stack pointer denoting the frame of the handler to which the stack is unwound upon
     *                     returning from this method
     * @param framePointer TODO
     */
    @NEVER_INLINE
    public static void unwindOptimized(Throwable throwable, Address catchAddress, Pointer stackPointer, Pointer framePointer) {
        final int unwindFrameSize = getUnwindFrameSize();

        // Put the exception where the exception handler expects to find it
        VmThreadLocal.EXCEPTION_OBJECT.setVariableReference(Reference.fromJava(throwable));

        if (throwable instanceof StackOverflowError) {
            // This complete call-chain must be inlined down to the native call
            // so that no further stack banging instructions
            // are executed before execution jumps to the catch handler.
            VirtualMemory.protectPages(VmThread.current().stackYellowZone(), VmThread.STACK_YELLOW_ZONE_PAGES);
        }

        if (!framePointer.isZero()) {
            VMRegister.setCpuFramePointer(framePointer);
        }

        // Push 'catchAddress' to the handler's stack frame and update RSP to point to the pushed value.
        // When the RET instruction is executed, the pushed 'catchAddress' will be popped from the stack
        // and the stack will be in the correct state for the handler.
        final Pointer returnAddressPointer = stackPointer.minus(Word.size());
        returnAddressPointer.setWord(catchAddress);
        VMRegister.setCpuStackPointer(returnAddressPointer.minus(unwindFrameSize));
    }

    public static void initialize() {
        AMD64OptStackWalking.unwindMethod = ClassActor.fromJava(AMD64OptStackWalking.class).findLocalClassMethodActor(SymbolTable.makeSymbol("unwindOptimized"), null);
        assert AMD64OptStackWalking.unwindMethod != null;
    }

    public static void catchException(TargetMethod targetMethod, Cursor current, Cursor callee, Throwable throwable) {
        Pointer ip = current.ip();
        Pointer sp = current.sp();
        Pointer fp = current.fp();
        Address catchAddress = targetMethod.throwAddressToCatchAddress(current.isTopFrame(), ip, throwable.getClass());
        if (!catchAddress.isZero()) {
            if (StackFrameWalker.TRACE_STACK_WALK.getValue()) {
                Log.print("StackFrameWalk: Handler position for exception at position ");
                Log.print(ip.minus(targetMethod.codeStart()).toInt());
                Log.print(" is ");
                Log.println(catchAddress.minus(targetMethod.codeStart()).toInt());
            }

            // Reset the stack walker
            current.stackFrameWalker().reset();
            // Completes the exception handling protocol (with respect to the garbage collector) initiated in Throw.raise()
            Safepoint.enable();

            TargetMethod calleeMethod = callee.targetMethod();
            if (calleeMethod != null && calleeMethod.registerRestoreEpilogueOffset() != -1) {
                unwindToCalleeEpilogue(throwable, catchAddress, sp, calleeMethod);
            } else {
                unwindOptimized(throwable, catchAddress, sp, fp);
            }
            ProgramError.unexpected("Should not reach here, unwind must jump to the exception handler!");
        }
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
        if (Trap.isTrapStub(targetMethod.classMethodActor)) {
            // RBP is whatever was in the frame pointer register at the time of the trap
            Pointer trapState = AMD64TrapStateAccess.getTrapStateFromRipPointer(ripPointer);
            callerFP = stackFrameWalker.readWord(trapState, AMD64GeneralRegister64.RBP.value() * Word.size()).asPointer();
        } else {
            // Propagate RBP unchanged as OPT methods do not touch this register.
            callerFP = current.fp();
        }
        stackFrameWalker.advance(callerIP, callerSP, callerFP);
    }

    @HOSTED_ONLY
    private static boolean atFirstOrLastInstruction(Cursor current) {
        // check whether the current ip is at the first instruction or a return
        // which means the stack pointer has not been adjusted yet (or has already been adjusted back)
        TargetMethod targetMethod = current.targetMethod();
        Pointer entryPoint = targetMethod.abi().callEntryPoint.equals(CallEntryPoint.C_ENTRY_POINT) ?
                CallEntryPoint.C_ENTRY_POINT.in(targetMethod) :
                CallEntryPoint.OPTIMIZED_ENTRY_POINT.in(targetMethod);

        return entryPoint.equals(current.ip()) || current.stackFrameWalker().readByte(current.ip(), 0) == RET;
    }

    /**
     * Prepares the reference map for the frame of a call to a trampoline from an OPT compiled method.
     *
     * An opto-compiled caller may pass some arguments in registers.  The trampoline is polymorphic, i.e. it does not have any
     * helpful maps regarding the actual callee.  It does store all potential parameter registers on its stack, though,
     * and recovers them before returning.  We mark those that contain references.
     *
     * @param current
     * @param callee
     * @param preparer
     */
    public static void prepareTrampolineRefMap(Cursor current, Cursor callee, StackReferenceMapPreparer preparer) {
        TargetMethod trampolineTargetMethod = callee.targetMethod();
        ClassMethodActor trampolineMethodActor = trampolineTargetMethod.classMethodActor();
        ClassMethodActor callerMethod;
        TargetMethod targetMethod = current.targetMethod();
        Pointer trampolineRegisters = callee.sp();

        // figure out what method the caller is trying to call
        if (trampolineMethodActor.isStaticTrampoline()) {
            int stopIndex = targetMethod.findClosestStopIndex(current.ip());
            callerMethod = (ClassMethodActor) targetMethod.directCallees()[stopIndex];
        } else {
            // this is an virtual or interface call; figure out the receiver method based on the
            // virtual or interface index
            final Object receiver = trampolineRegisters.getReference().toJava(); // read receiver object on stack
            final ClassActor classActor = ObjectAccess.readClassActor(receiver);
            assert trampolineTargetMethod.referenceLiterals().length == 1;
            final DynamicTrampoline dynamicTrampoline = (DynamicTrampoline) trampolineTargetMethod.referenceLiterals()[0];
            if (trampolineMethodActor.isVirtualTrampoline()) {
                callerMethod = classActor.getVirtualMethodActorByVTableIndex(dynamicTrampoline.dispatchTableIndex);
            } else {
                callerMethod = classActor.getVirtualMethodActorByIIndex(dynamicTrampoline.dispatchTableIndex);
            }
        }

        // use the caller method to fill out the reference map for the saved parameters in the trampoline frame
        int parameterRegisterIndex = 0;
        if (!callerMethod.isStatic()) {
            // set a bit for the receiver object
            preparer.setReferenceMapBits(current, trampolineRegisters, 1, 1);
            parameterRegisterIndex = 1;
        }

        SignatureDescriptor descriptor = callerMethod.descriptor();
        TargetABI abi = trampolineTargetMethod.abi();
        for (int i = 0; i < descriptor.numberOfParameters(); ++i) {
            final TypeDescriptor parameter = descriptor.parameterDescriptorAt(i);
            final Kind parameterKind = parameter.toKind();
            if (parameterKind.isReference) {
                // set a bit for this parameter
                preparer.setReferenceMapBits(current, trampolineRegisters.plusWords(parameterRegisterIndex), 1, 1);
            }
            if (abi.putIntoIntegerRegister(parameterKind)) {
                parameterRegisterIndex++;
                if (parameterRegisterIndex >= abi.integerIncomingParameterRegisters.length()) {
                    // done since all subsequent parameters are known to be passed on the stack
                    // and covered by the reference map for the stack at this call point
                    return;
                }
            }
        }
    }
}
