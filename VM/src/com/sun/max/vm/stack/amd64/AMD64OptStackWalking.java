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

import com.sun.max.vm.actor.member.ClassMethodActor;
import com.sun.max.vm.actor.holder.ClassActor;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.compiler.target.TargetMethod;
import com.sun.max.vm.compiler.target.TargetABI;
import com.sun.max.vm.compiler.CompilationScheme;
import com.sun.max.vm.compiler.CallEntryPoint;
import com.sun.max.vm.classfile.constant.SymbolTable;
import com.sun.max.vm.reference.Reference;
import com.sun.max.vm.thread.VmThreadLocal;
import com.sun.max.vm.thread.VmThread;
import com.sun.max.vm.code.Code;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.amd64.AMD64TrapStateAccess;
import com.sun.max.vm.Log;
import com.sun.max.vm.VMConfiguration;
import com.sun.max.vm.trampoline.DynamicTrampoline;
import com.sun.max.vm.object.ObjectAccess;
import com.sun.max.vm.type.SignatureDescriptor;
import com.sun.max.vm.type.TypeDescriptor;
import com.sun.max.vm.type.Kind;
import com.sun.max.unsafe.Pointer;
import com.sun.max.unsafe.Word;
import com.sun.max.unsafe.Address;
import com.sun.max.unsafe.UnsafeCast;
import com.sun.max.annotate.NEVER_INLINE;
import com.sun.max.memory.VirtualMemory;
import com.sun.max.program.ProgramError;
import com.sun.max.asm.amd64.AMD64GeneralRegister64;

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

    public static boolean walkOptimizedFrame(StackFrameWalker.Cursor current, StackFrameWalker.Cursor callee, StackFrameWalker.Purpose purpose, Object context) {
        StackFrameWalker stackFrameWalker = current.stackFrameWalker();
        TargetMethod targetMethod = current.targetMethod();
        TargetMethod calleeMethod = callee.targetMethod();
        Pointer ip = current.ip();
        Pointer sp = current.sp();
        Pointer entryPoint;
        if (targetMethod.abi().callEntryPoint().equals(CallEntryPoint.C_ENTRY_POINT)) {
            // Simple case (no adapter)
            entryPoint = com.sun.max.vm.compiler.CallEntryPoint.C_ENTRY_POINT.in(targetMethod);
        } else {
            // we may be in an adapter
            Pointer jitEntryPoint = com.sun.max.vm.compiler.CallEntryPoint.JIT_ENTRY_POINT.in(targetMethod);
            Pointer optimizedEntryPoint = com.sun.max.vm.compiler.CallEntryPoint.OPTIMIZED_ENTRY_POINT.in(targetMethod);
            boolean hasAdapterFrame = !(jitEntryPoint.equals(optimizedEntryPoint));

            if (hasAdapterFrame) {
                final Pointer startOfAdapter = AMD64AdapterStackWalking.jitOptAdapterCodeStart(stackFrameWalker, targetMethod);
                if (AMD64AdapterStackWalking.inJitOptAdapterFrameCode(current.isTopFrame(), ip, optimizedEntryPoint, startOfAdapter)) {
                    return AMD64AdapterStackWalking.walkJitOptAdapterFrame(current, stackFrameWalker, targetMethod, purpose, context, startOfAdapter, current.isTopFrame());
                }
            }
            entryPoint = optimizedEntryPoint;
        }

        Pointer ripPointer; // stack pointer at call entry point (where the RIP is).
        if (ip.equals(entryPoint) || stackFrameWalker.readByte(ip, 0) == RET) {
            // We are at the very first or the very last instruction to be executed.
            // In either case the stack pointer is unmodified wrt. the CALL that got us here.
            ripPointer = sp;
        } else {
            // We are somewhere in the middle of this method.
            // The stack pointer has been bumped already to access locals and it has not been reset yet.
            ripPointer = sp.plus(targetMethod.frameSize());
        }

        switch (purpose) {
            case REFERENCE_MAP_PREPARING: {
                if (!prepareReferenceMap(ripPointer, current, callee, (StackReferenceMapPreparer) context)) {
                    return false;
                }
                break;
            }
            case EXCEPTION_HANDLING: {
                StackUnwindingContext stackUnwindingContext = UnsafeCast.asStackUnwindingContext(context);
                Address catchAddress = targetMethod.throwAddressToCatchAddress(current.isTopFrame(), ip, stackUnwindingContext.throwable.getClass());
                if (!catchAddress.isZero()) {
                    if (StackFrameWalker.TRACE_STACK_WALK.getValue()) {
                        Log.print("StackFrameWalk: Handler position for exception at position ");
                        Log.print(ip.minus(targetMethod.codeStart()).toInt());
                        Log.print(" is ");
                        Log.println(catchAddress.minus(targetMethod.codeStart()).toInt());
                    }

                    final Throwable throwable = stackUnwindingContext.throwable;
                    // Reset the stack walker
                    stackFrameWalker.reset();
                    // Completes the exception handling protocol (with respect to the garbage collector) initiated in Throw.raise()
                    Safepoint.enable();

                    if (calleeMethod != null && calleeMethod.registerRestoreEpilogueOffset() != -1) {
                        unwindToCalleeEpilogue(throwable, catchAddress, sp, calleeMethod);
                    } else {
                        unwindOptimized(throwable, catchAddress, sp);
                    }
                    ProgramError.unexpected("Should not reach here, unwind must jump to the exception handler!");
                }
                break;
            }
            case RAW_INSPECTING: {
                final RawStackFrameVisitor stackFrameVisitor = (RawStackFrameVisitor) context;
                final int flags = RawStackFrameVisitor.Util.makeFlags(current.isTopFrame(), false);
                if (!stackFrameVisitor.visitFrame(targetMethod, ip, sp, sp, flags)) {
                    return false;
                }
                break;
            }
            case INSPECTING: {
                final StackFrameVisitor stackFrameVisitor = (StackFrameVisitor) context;
                if (!stackFrameVisitor.visitFrame(new AMD64JavaStackFrame(stackFrameWalker.calleeStackFrame(), targetMethod, ip, sp, sp))) {
                    return false;
                }
                break;
            }
        }

        Pointer callerIP = stackFrameWalker.readWord(ripPointer, 0).asPointer();
        Pointer callerSP = ripPointer.plus(Word.size()); // Skip RIP word
        Pointer callerFP;
        if (targetMethod.classMethodActor() != null && targetMethod.classMethodActor().isTrapStub()) {
            // framePointer is whatever was in the frame pointer register at the time of the trap
            Pointer trapState = AMD64TrapStateAccess.getTrapStateFromRipPointer(ripPointer);
            callerFP = stackFrameWalker.readWord(trapState, AMD64GeneralRegister64.RBP.value() * Word.size()).asPointer();
        } else {
            // framePointer == stackPointer for this scheme.
            callerFP = callerSP;
        }
        stackFrameWalker.advance(callerIP, callerSP, callerFP);
        return true;
    }


    private static boolean prepareReferenceMap(Pointer ripPointer, StackFrameWalker.Cursor current, StackFrameWalker.Cursor callee, StackReferenceMapPreparer preparer) {
        StackFrameWalker stackFrameWalker = current.stackFrameWalker();
        TargetMethod targetMethod = current.targetMethod();
        Pointer trapState = stackFrameWalker.trapState();
        if (!trapState.isZero()) {
            FatalError.check(!targetMethod.classMethodActor().isTrapStub(), "Cannot have a trap in the trapStub");
            final TrapStateAccess trapStateAccess = TrapStateAccess.instance();
            if (Trap.Number.isImplicitException(trapStateAccess.getTrapNumber(trapState))) {
                Class<? extends Throwable> throwableClass = Trap.Number.toImplicitExceptionClass(trapStateAccess.getTrapNumber(trapState));
                final Address catchAddress = targetMethod.throwAddressToCatchAddress(current.isTopFrame(), trapStateAccess.getInstructionPointer(trapState), throwableClass);
                if (catchAddress.isZero()) {
                    // An implicit exception occurred but not in the scope of a local exception handler.
                    // Thus, execution will not resume in this frame and hence no GC roots need to be scanned.
                    return true;
                }
                // TODO: Get address of safepoint instruction at exception dispatcher site and scan
                // the frame references based on its Java frame descriptor.
                FatalError.unexpected("Cannot reliably find safepoint at exception dispatcher site yet.");
            }
        } else {
            if (targetMethod.classMethodActor().isTrapStub()) {
                TrapStateAccess trapStateAccess = TrapStateAccess.instance();
                trapState = AMD64TrapStateAccess.getTrapStateFromRipPointer(ripPointer);
                stackFrameWalker.setTrapState(trapState);
                if (Trap.Number.isImplicitException(trapStateAccess.getTrapNumber(trapState))) {
                    Class<? extends Throwable> throwableClass = Trap.Number.toImplicitExceptionClass(trapStateAccess.getTrapNumber(trapState));
                    final Address catchAddress = targetMethod.throwAddressToCatchAddress(current.isTopFrame(), trapStateAccess.getInstructionPointer(trapState), throwableClass);
                    if (catchAddress.isZero()) {
                        // An implicit exception occurred but not in the scope of a local exception handler.
                        // Thus, execution will not resume in this frame and hence no GC roots need to be scanned.
                    } else {
                        // TODO: Get address of safepoint instruction at exception dispatcher site and scan
                        // the register references based on its Java frame descriptor.
                        FatalError.unexpected("Cannot reliably find safepoint at exception dispatcher site yet.");

                        Pointer callerCatchAddress = catchAddress.asPointer();
                        final TargetMethod callerTargetMethod = Code.codePointerToTargetMethod(callerCatchAddress);
                        if (callerTargetMethod != null) {
                            callerTargetMethod.prepareRegisterReferenceMap(preparer, callerCatchAddress, trapStateAccess.getRegisterState(trapState), StackFrameWalker.CalleeKind.TRAP_STUB);
                        }
                    }
                } else {
                    // Only scan with references in registers for a caller that did not trap due to an implicit exception.
                    // Find the register state and pass it to the preparer so that it can be covered with the appropriate reference map
                    final Pointer callerInstructionPointer = stackFrameWalker.readWord(ripPointer, 0).asPointer();

                    final TargetMethod callerTargetMethod = Code.codePointerToTargetMethod(callerInstructionPointer);
                    if (callerTargetMethod != null) {
                        callerTargetMethod.prepareRegisterReferenceMap(preparer, callerInstructionPointer, trapStateAccess.getRegisterState(trapState), StackFrameWalker.CalleeKind.TRAP_STUB);
                    }
                }
            }
        }
        Pointer ignoredOperandStackPointer = Pointer.zero();

        if (preparer.checkIgnoreCurrentFrame()) {
            return true;
        }

        if (!preparer.prepareFrameReferenceMap(targetMethod, current.ip(), current.sp(), ignoredOperandStackPointer, 0, callee.targetMethod())) {
            return false;
        }

        return true;
    }

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
        unwindOptimized(throwable, epilogueAddress, calleeStackPointer);
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
     */
    @NEVER_INLINE
    public static void unwindOptimized(Throwable throwable, Address catchAddress, Pointer stackPointer) {
        final int unwindFrameSize = getUnwindFrameSize();

        // Put the exception where the exception handler expects to find it
        VmThreadLocal.EXCEPTION_OBJECT.setVariableReference(Reference.fromJava(throwable));

        if (throwable instanceof StackOverflowError) {
            // This complete call-chain must be inlined down to the native call
            // so that no further stack banging instructions
            // are executed before execution jumps to the catch handler.
            VirtualMemory.protectPages(VmThread.current().stackYellowZone(), VmThread.STACK_YELLOW_ZONE_PAGES);
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

    /**
     * Size in bytes of parameters on a stack frame.
     *
     * @return size in bytes
     */
    public static int adapterFrameSize(ClassMethodActor classMethodActor) {
        int paramSize = JitStackFrameLayout.JIT_SLOT_SIZE * classMethodActor.numberOfParameterSlots();
        return VMConfiguration.target().targetABIsScheme().jitABI().alignFrameSize(paramSize);
    }

    public static void catchException(TargetMethod targetMethod, StackFrameWalker.Cursor current, StackFrameWalker.Cursor callee, Throwable throwable) {
        Pointer ip = current.ip();
        Pointer sp = current.sp();
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
                unwindOptimized(throwable, catchAddress, sp);
            }
            ProgramError.unexpected("Should not reach here, unwind must jump to the exception handler!");
        }
    }

    public static boolean acceptStackFrameVisitor(StackFrameWalker.Cursor current, StackFrameWalker.Cursor callee, StackFrameVisitor visitor) {
        StackFrame stackFrame;
        StackFrameWalker stackFrameWalker = current.stackFrameWalker();
        if (AMD64AdapterStackWalking.isJitOptAdapterFrameCode(current)) {
            final Pointer startOfAdapter = AMD64AdapterStackWalking.jitOptAdapterCodeStart(stackFrameWalker, current.targetMethod());
            final int adapterFrameSize = AMD64AdapterStackWalking.jitToOptimizingAdapterFrameSize(stackFrameWalker, startOfAdapter);
            stackFrame = new AdapterStackFrame(stackFrameWalker.calleeStackFrame(), new AdapterStackFrameLayout(adapterFrameSize, true), current.targetMethod(), current.ip(), current.fp(), current.sp());
        } else {
            stackFrame = new AMD64JavaStackFrame(stackFrameWalker.calleeStackFrame(), current.targetMethod(), current.ip(), current.sp(), current.sp());
        }
        return visitor.visitFrame(stackFrame);
    }

    public static void advance(StackFrameWalker.Cursor current) {
        if (AMD64AdapterStackWalking.isJitOptAdapterFrameCode(current)) {
            AMD64AdapterStackWalking.advanceJitOptAdapterFrame(current);
            return;
        }

        TargetMethod targetMethod = current.targetMethod();
        StackFrameWalker stackFrameWalker = current.stackFrameWalker();
        Pointer sp = current.sp();
        Pointer ripPointer = atFirstOrLastInstruction(current) ? sp : sp.plus(targetMethod.frameSize());
        Pointer callerIP = stackFrameWalker.readWord(ripPointer, 0).asPointer();
        Pointer callerSP = ripPointer.plus(Word.size()); // Skip return instruction pointer on stack
        Pointer callerFP;
        if (targetMethod.classMethodActor() != null && targetMethod.classMethodActor().isTrapStub()) {
            // framePointer is whatever was in the frame pointer register at the time of the trap
            Pointer trapState = AMD64TrapStateAccess.getTrapStateFromRipPointer(ripPointer);
            callerFP = stackFrameWalker.readWord(trapState, AMD64GeneralRegister64.RBP.value() * Word.size()).asPointer();
        } else {
            // framePointer == stackPointer for this scheme.
            callerFP = callerSP;
        }
        stackFrameWalker.advance(callerIP, callerSP, callerFP);
    }

    private static boolean atFirstOrLastInstruction(StackFrameWalker.Cursor current) {
        // check whether the current ip is at the first instruction or a return
        // which means the stack pointer has not been adjusted yet (or has already been adjusted back)
        TargetMethod targetMethod = current.targetMethod();
        Pointer entryPoint = targetMethod.abi().callEntryPoint().equals(CallEntryPoint.C_ENTRY_POINT) ?
                CallEntryPoint.C_ENTRY_POINT.in(targetMethod) :
                CallEntryPoint.OPTIMIZED_ENTRY_POINT.in(targetMethod);

        return entryPoint.equals(current.ip()) || current.stackFrameWalker().readByte(current.ip(), 0) == RET;
    }

    public static void prepareTrampolineRefMap(StackFrameWalker.Cursor current, StackFrameWalker.Cursor callee, StackReferenceMapPreparer preparer) {
        TargetMethod trampolineTargetMethod = callee.targetMethod();
        ClassMethodActor trampolineMethodActor = trampolineTargetMethod.classMethodActor();
        ClassMethodActor calleeMethod;
        TargetMethod targetMethod = current.targetMethod();
        Pointer trampolineRegisters = callee.sp();

        // figure out what method the caller is trying to call
        if (trampolineMethodActor.isStaticTrampoline()) {
            int stopIndex = targetMethod.findClosestStopIndex(current.ip());
            calleeMethod = (ClassMethodActor) targetMethod.directCallees()[stopIndex];
        } else {
            // this is an virtual or interface call; figure out the receiver method based on the
            // virtual or interface index
            final Object receiver = trampolineRegisters.getReference().toJava(); // read receiver object on stack
            final ClassActor classActor = ObjectAccess.readClassActor(receiver);
            assert trampolineTargetMethod.referenceLiterals().length == 1;
            final DynamicTrampoline dynamicTrampoline = (DynamicTrampoline) trampolineTargetMethod.referenceLiterals()[0];
            if (trampolineMethodActor.isVirtualTrampoline()) {
                calleeMethod = classActor.getVirtualMethodActorByVTableIndex(dynamicTrampoline.dispatchTableIndex());
            } else {
                calleeMethod = classActor.getVirtualMethodActorByIIndex(dynamicTrampoline.dispatchTableIndex());
            }
        }

        // use the callee method to fill out the reference map for the saved parameters in the trampoline frame
        int parameterRegisterIndex = 0;
        if (!calleeMethod.isStatic()) {
            // set a bit for the receiver object
            preparer.setReferenceMapBits(current, trampolineRegisters, 1, 1);
            parameterRegisterIndex = 1;
        }

        SignatureDescriptor descriptor = calleeMethod.descriptor();
        TargetABI abi = trampolineTargetMethod.abi();
        for (int i = 0; i < descriptor.numberOfParameters(); ++i) {
            final TypeDescriptor parameter = descriptor.parameterDescriptorAt(i);
            final Kind parameterKind = parameter.toKind();
            if (parameterKind == Kind.REFERENCE) {
                // set a bit for this parameter
                preparer.setReferenceMapBits(current, trampolineRegisters.plusWords(parameterRegisterIndex), 1, 1);
            }
            if (abi.putIntoIntegerRegister(parameterKind)) {
                parameterRegisterIndex++;
                if (parameterRegisterIndex >= abi.integerIncomingParameterRegisters().length()) {
                    // done since all subsequent parameters are known to be passed on the stack
                    // and covered by the reference map for the stack at this call point
                    return;
                }
            }
        }
    }
}
