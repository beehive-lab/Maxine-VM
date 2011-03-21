/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.stack.amd64;

import static com.sun.cri.ci.CiRegister.RegisterFlag.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.compiler.target.TargetMethod.Flavor.*;

import com.sun.c1x.target.amd64.*;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiCallingConvention.Type;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.amd64.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.Cursor;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * This class collects together stack-walking related functionality that is (somewhat) compiler-independent.
 *
 * @author Ben L. Titzer
 */
public class AMD64OptStackWalking {

    public static final int RIP_CALL_INSTRUCTION_SIZE = 5;
    public static final byte RET = (byte) 0xC3;

    private static CriticalMethod unwindOptimized = new CriticalMethod(AMD64OptStackWalking.class, "unwindOptimized", null);

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
        final int unwindFrameSize = unwindOptimized.targetMethod().frameSize();

        // Put the exception where the exception handler expects to find it
        VmThreadLocal.EXCEPTION_OBJECT.store3(Reference.fromJava(throwable));

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

    public static void catchException(TargetMethod targetMethod, Cursor current, Cursor callee, Throwable throwable) {
        Pointer ip = current.ip();
        Pointer sp = current.sp();
        Pointer fp = current.fp();
        Address catchAddress = targetMethod.throwAddressToCatchAddress(current.isTopFrame(), ip, throwable.getClass());
        if (!catchAddress.isZero()) {
            if (StackFrameWalker.TraceStackWalk) {
                Log.print("StackFrameWalk: Handler position for exception at position ");
                Log.print(ip.minus(targetMethod.codeStart()).toInt());
                Log.print(" is ");
                Log.println(catchAddress.minus(targetMethod.codeStart()).toInt());
            }

            TargetMethod calleeMethod = callee.targetMethod();
            // Reset the stack walker
            current.stackFrameWalker().reset();
            // Completes the exception handling protocol (with respect to the garbage collector) initiated in Throw.raise()
            Safepoint.enable();

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

    @HOSTED_ONLY
    private static boolean atFirstOrLastInstruction(Cursor current) {
        // check whether the current ip is at the first instruction or a return
        // which means the stack pointer has not been adjusted yet (or has already been adjusted back)
        TargetMethod targetMethod = current.targetMethod();
        Pointer entryPoint = targetMethod.callEntryPoint.equals(CallEntryPoint.C_ENTRY_POINT) ?
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
     * @param registerConfig TODO
     */
    public static void prepareTrampolineRefMap(Cursor current, Cursor callee, StackReferenceMapPreparer preparer) {
        RiRegisterConfig registerConfig = vm().registerConfigs.trampoline;
        TargetMethod trampoline = callee.targetMethod();
        ClassMethodActor calledMethod;
        TargetMethod targetMethod = current.targetMethod();

        Pointer calleeSaveStart = callee.sp();
        CiCalleeSaveArea csa = registerConfig.getCalleeSaveArea();
        CiRegister[] regs = registerConfig.getCallingConventionRegisters(Type.JavaCall, CPU);

        // figure out what method the caller is trying to call
        if (trampoline.is(StaticTrampoline)) {
            int stopIndex = targetMethod.findClosestStopIndex(current.ip().minus(1));
            calledMethod = (ClassMethodActor) targetMethod.directCallees()[stopIndex];
        } else {
            // this is a virtual or interface call; figure out the receiver method based on the
            // virtual or interface index
            Object receiver = calleeSaveStart.plus(csa.offsetOf(regs[0])).getReference().toJava();
            ClassActor classActor = ObjectAccess.readClassActor(receiver);
            // The virtual dispatch trampoline stubs put the virtual dispatch index into the
            // scratch register and then save it to the stack.
            int index = vm().stubs.readVirtualDispatchIndexFromTrampolineFrame(calleeSaveStart);
            if (trampoline.is(VirtualTrampoline)) {
                calledMethod = classActor.getVirtualMethodActorByVTableIndex(index);
            } else {
                assert trampoline.is(InterfaceTrampoline);
                calledMethod = classActor.getVirtualMethodActorByIIndex(index);
            }
        }

        int regIndex = 0;
        if (!calledMethod.isStatic()) {
            // set a bit for the receiver object
            int offset = csa.offsetOf(regs[regIndex++]);
            preparer.setReferenceMapBits(current, calleeSaveStart.plus(offset), 1, 1);
        }

        SignatureDescriptor sig = calledMethod.descriptor();
        for (int i = 0; i < sig.numberOfParameters() && regIndex < regs.length; ++i) {
            TypeDescriptor arg = sig.parameterDescriptorAt(i);
            CiRegister reg = regs[regIndex];
            Kind kind = arg.toKind();
            if (kind.isReference) {
                // set a bit for this parameter
                int offset = csa.offsetOf(reg);
                preparer.setReferenceMapBits(current, calleeSaveStart.plus(offset), 1, 1);
            }
            if (kind != Kind.FLOAT && kind != Kind.DOUBLE) {
                // Only iterating over the integral arg registers
                regIndex++;
            }
        }
    }
}
