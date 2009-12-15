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
package com.sun.max.vm.compiler.b.c.d.e.amd64.target;

import static com.sun.max.vm.compiler.CallEntryPoint.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.asm.amd64.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.b.c.d.e.amd64.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.amd64.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.*;
import com.sun.max.vm.stack.amd64.*;
import com.sun.max.vm.thread.*;

/**
 * @author Bernd Mathiske
 */
public final class AMD64CPSCompiler extends BcdeAMD64Compiler implements TargetGeneratorScheme {

    private final AMD64EirToTargetTranslator eirToTargetTranslator;

    public AMD64CPSCompiler(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        eirToTargetTranslator = new AMD64EirToTargetTranslator(this);
    }

    public TargetGenerator targetGenerator() {
        return eirToTargetTranslator;
    }

    @Override
    public IrGenerator irGenerator() {
        return eirToTargetTranslator;
    }

    @Override
    public Sequence<IrGenerator> irGenerators() {
        return Sequence.Static.appended(super.irGenerators(), targetGenerator());
    }

    static final int RIP_CALL_INSTRUCTION_SIZE = 5;

    @INLINE
    private static void patchRipCallSite(Pointer callSite, Address calleeEntryPoint) {
        final int calleeOffset = calleeEntryPoint.minus(callSite.plus(RIP_CALL_INSTRUCTION_SIZE)).toInt();
        callSite.writeInt(1, calleeOffset);
    }

    /**
     * @see StaticTrampoline
     */
    @Override
    @NEVER_INLINE
    public void staticTrampoline() {
        final StaticTrampolineContext context = new StaticTrampolineContext();
        new VmStackFrameWalker(VmThread.current().vmThreadLocals()).inspect(VMRegister.getInstructionPointer(),
                                                      VMRegister.getCpuStackPointer(),
                                                      VMRegister.getCpuFramePointer(),
                                                      context);
        final Pointer callSite = context.ip.minus(RIP_CALL_INSTRUCTION_SIZE);
        final TargetMethod caller = Code.codePointerToTargetMethod(callSite);

        final ClassMethodActor callee = caller.callSiteToCallee(callSite);

        // Use the caller's abi to get the correct entry point.
        final Address calleeEntryPoint = CompilationScheme.Static.compile(callee, caller.abi().callEntryPoint());
        patchRipCallSite(callSite, calleeEntryPoint);

        // Make the trampoline's caller re-execute the now modified CALL instruction after we return from the trampoline:
        final Pointer stackPointer = context.sp.minus(Word.size());
        stackPointer.setWord(callSite); // patch return address
    }

    public void patchCallSite(TargetMethod targetMethod, int callOffset, Word callEntryPoint) {
        final Pointer callSite = targetMethod.codeStart().plus(callOffset).asPointer();
        final AMD64Assembler assembler = new AMD64Assembler(callSite.toLong());
        final Label label = new Label();
        assembler.fixLabel(label, callEntryPoint.asAddress().toLong());
        assembler.call(label);
        try {
            final byte[] code = assembler.toByteArray();
            Bytes.copy(code, 0, targetMethod.code(), callOffset, code.length);
        } catch (AssemblyException assemblyException) {
            ProgramError.unexpected("patching call site failed", assemblyException);
        }
    }

    private static final byte RET = (byte) 0xC3;
    private static final byte RET2 = (byte) 0xC2;

    private static boolean walkAdapterFrame(StackFrameWalker.Cursor current, StackFrameWalker stackFrameWalker, TargetMethod targetMethod, Purpose purpose, Object context, Pointer startOfAdapter, boolean isTopFrame) {
        final Pointer instructionPointer = current.ip();
        final Pointer stackPointer = current.sp();
        final Pointer jitEntryPoint = JIT_ENTRY_POINT.in(targetMethod);
        final int adapterFrameSize = AMD64AdapterFrameGenerator.jitToOptimizingAdapterFrameSize(stackFrameWalker, startOfAdapter);
        Pointer callerFramePointer = current.fp();

        Pointer ripPointer = stackPointer; // stack pointer at call entry point (where the RIP is).
        final byte firstInstructionByte = stackFrameWalker.readByte(instructionPointer, 0);
        if (!instructionPointer.equals(jitEntryPoint) && !instructionPointer.equals(startOfAdapter) && firstInstructionByte != RET2) {
            ripPointer = stackPointer.plus(adapterFrameSize);
            callerFramePointer = stackFrameWalker.readWord(ripPointer, -Word.size()).asPointer();
        }

        final Pointer callerInstructionPointer = stackFrameWalker.readWord(ripPointer, 0).asPointer();
        switch(purpose) {
            case EXCEPTION_HANDLING: {
                // cannot have an exception while in an adapter frame
                break;
            }
            case REFERENCE_MAP_PREPARING: {
                break;
            }
            case RAW_INSPECTING: {
                final RawStackFrameVisitor stackFrameVisitor = (RawStackFrameVisitor) context;
                final int flags = RawStackFrameVisitor.Util.makeFlags(isTopFrame, true);
                stackFrameVisitor.visitFrame(targetMethod, callerInstructionPointer, current.fp(), stackPointer, flags);
                break;
            }
            case INSPECTING: {
                final StackFrameVisitor stackFrameVisitor = (StackFrameVisitor) context;
                final StackFrame stackFrame = new AdapterStackFrame(stackFrameWalker.calleeStackFrame(), new AdapterStackFrameLayout(adapterFrameSize, true), targetMethod, instructionPointer, current.fp(), stackPointer);
                if (!stackFrameVisitor.visitFrame(stackFrame)) {
                    return false;
                }
                break;
            }
        }
        stackFrameWalker.advance(callerInstructionPointer, ripPointer.plus(Word.size() /* skip RIP */), callerFramePointer);
        return true;
    }

    /**
     * Determines if an execution point is in some adapter frame related code.
     *
     * @param inTopFrame specifies if the execution point is in the top frame on the stack
     * @param instructionPointer the address of the next instruction that will be executed
     * @param optimizedEntryPoint the address of the first instruction compiled by this compiler
     * @param startOfAdapter the address of the first instruction that sets up the adapter frame
     * @return true if the execution point denoted by the combination of {@code instructionPointer} and
     *         {@code inTopFrame} is in adapter frame related code
     */
    private static boolean inAdapterFrameCode(boolean inTopFrame, final Pointer instructionPointer, final Pointer optimizedEntryPoint, final Pointer startOfAdapter) {
        if (instructionPointer.lessThan(optimizedEntryPoint)) {
            return true;
        }
        if (inTopFrame) {
            return instructionPointer.greaterEqual(startOfAdapter);
        }
        // Since we are not in the top frame, instructionPointer is really the return instruction pointer of
        // a call. If it happens that the call is to a method that is never expected to return normally (e.g. a method that only exits by throwing an exception),
        // the call may well be the very last instruction in the method prior to the adapter frame code.
        // In this case, we're only in adapter frame code if the instructionPointer is greater than
        // the start of the adapter frame code.
        return instructionPointer.greaterThan(startOfAdapter);
    }

    @Override
    public boolean walkFrame(StackFrameWalker.Cursor current, StackFrameWalker.Cursor callee, TargetMethod calleeMethod, Purpose purpose, Object context) {
        return walkFrameHelper(current, current.stackFrameWalker(), current.isTopFrame(), current.targetMethod(), calleeMethod, purpose, context);
    }

    public static boolean walkFrameHelper(StackFrameWalker.Cursor current, StackFrameWalker stackFrameWalker, boolean isTopFrame, TargetMethod targetMethod, TargetMethod callee, Purpose purpose, Object context) {
        final Pointer instructionPointer = current.ip();
        final Pointer stackPointer = current.sp();
        final Pointer entryPoint;
        if (targetMethod.abi().callEntryPoint().equals(CallEntryPoint.C_ENTRY_POINT)) {
            // Simple case (no adapter)
            entryPoint = C_ENTRY_POINT.in(targetMethod);
        } else {
            // we may be in an adapter
            final Pointer jitEntryPoint = JIT_ENTRY_POINT.in(targetMethod);
            final Pointer optimizedEntryPoint = OPTIMIZED_ENTRY_POINT.in(targetMethod);
            final boolean hasAdapterFrame = !(jitEntryPoint.equals(optimizedEntryPoint));

            if (hasAdapterFrame) {
                final Pointer startOfAdapter = AMD64AdapterFrameGenerator.jitEntryPointJmpTarget(stackFrameWalker, targetMethod);
                if (inAdapterFrameCode(isTopFrame, instructionPointer, optimizedEntryPoint, startOfAdapter)) {
                    return walkAdapterFrame(current, stackFrameWalker, targetMethod, purpose, context, startOfAdapter, isTopFrame);
                }
            }
            entryPoint = optimizedEntryPoint;
        }

        final int frameSize;
        final Pointer ripPointer; // stack pointer at call entry point (where the RIP is).
        if (instructionPointer.equals(entryPoint) || stackFrameWalker.readByte(instructionPointer, 0) == RET) {
            // We are at the very first or the very last instruction to be executed.
            // In either case the stack pointer is unmodified wrt. the CALL that got us here.
            frameSize = 0;
            ripPointer = stackPointer;
        } else {
            // We are somewhere in the middle of this method.
            // The stack pointer has been bumped already to access locals and it has not been reset yet.
            frameSize = targetMethod.frameSize();
            ripPointer = stackPointer.plus(frameSize);
        }

        switch (purpose) {
            case REFERENCE_MAP_PREPARING: {

                // frame pointer == stack pointer
                final StackReferenceMapPreparer preparer = (StackReferenceMapPreparer) context;
                Pointer trapState = stackFrameWalker.trapState();
                if (!trapState.isZero()) {
                    FatalError.check(!targetMethod.classMethodActor().isTrapStub(), "Cannot have a trap in the trapStub");
                    final TrapStateAccess trapStateAccess = TrapStateAccess.instance();
                    if (Trap.Number.isImplicitException(trapStateAccess.getTrapNumber(trapState))) {
                        Class<? extends Throwable> throwableClass = Trap.Number.toImplicitExceptionClass(trapStateAccess.getTrapNumber(trapState));
                        final Address catchAddress = targetMethod.throwAddressToCatchAddress(isTopFrame, trapStateAccess.getInstructionPointer(trapState), throwableClass);
                        if (catchAddress.isZero()) {
                            // An implicit exception occurred but not in the scope of a local exception handler.
                            // Thus, execution will not resume in this frame and hence no GC roots need to be scanned.
                            break;
                        }
                        // TODO: Get address of safepoint instruction at exception dispatcher site and scan
                        // the frame references based on its Java frame descriptor.
                        FatalError.unexpected("Cannot reliably find safepoint at exception dispatcher site yet.");
                    }
                } else {
                    if (targetMethod.classMethodActor().isTrapStub()) {
                        final TrapStateAccess trapStateAccess = TrapStateAccess.instance();
                        trapState = AMD64TrapStateAccess.getTrapStateFromRipPointer(ripPointer);
                        stackFrameWalker.setTrapState(trapState);
                        if (Trap.Number.isImplicitException(trapStateAccess.getTrapNumber(trapState))) {
                            Class<? extends Throwable> throwableClass = Trap.Number.toImplicitExceptionClass(trapStateAccess.getTrapNumber(trapState));
                            final Address catchAddress = targetMethod.throwAddressToCatchAddress(isTopFrame, trapStateAccess.getInstructionPointer(trapState), throwableClass);
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
                                    callerTargetMethod.prepareRegisterReferenceMap(trapStateAccess.getRegisterState(trapState), callerCatchAddress, preparer);
                                }
                            }
                        } else {
                            // Only scan with references in registers for a caller that did not trap due to an implicit exception.
                            // Find the register state and pass it to the preparer so that it can be covered with the appropriate reference map
                            final Pointer callerInstructionPointer = stackFrameWalker.readWord(ripPointer, 0).asPointer();

                            final TargetMethod callerTargetMethod = Code.codePointerToTargetMethod(callerInstructionPointer);
                            if (callerTargetMethod != null) {
                                callerTargetMethod.prepareRegisterReferenceMap(trapStateAccess.getRegisterState(trapState), callerInstructionPointer, preparer);
                            }
                        }
                    }
                }
                final Pointer ignoredOperandStackPointer = Pointer.zero();

                if (preparer.checkIgnoreCurrentFrame()) {
                    break;
                }

                if (!preparer.prepareFrameReferenceMap(targetMethod, instructionPointer, stackPointer, ignoredOperandStackPointer, 0, callee)) {
                    return false;
                }
                break;
            }
            case EXCEPTION_HANDLING: {
                final Address throwAddress = instructionPointer;
                final StackUnwindingContext stackUnwindingContext = UnsafeCast.asStackUnwindingContext(context);
                final Address catchAddress = targetMethod.throwAddressToCatchAddress(isTopFrame, throwAddress, stackUnwindingContext.throwable.getClass());
                if (!catchAddress.isZero()) {
                    if (StackFrameWalker.TRACE_STACK_WALK.getValue()) {
                        Log.print("StackFrameWalk: Handler position for exception at position ");
                        Log.print(throwAddress.minus(targetMethod.codeStart()).toInt());
                        Log.print(" is ");
                        Log.println(catchAddress.minus(targetMethod.codeStart()).toInt());
                    }

                    final Throwable throwable = stackUnwindingContext.throwable;
                    // Reset the stack walker
                    stackFrameWalker.reset();
                    // Completes the exception handling protocol (with respect to the garbage collector) initiated in Throw.raise()
                    Safepoint.enable();

                    if (callee != null && callee.registerRestoreEpilogueOffset() != -1) {
                        unwindToCalleeEpilogue(throwable, catchAddress, stackPointer, callee);
                    } else {
                        unwind(throwable, catchAddress, stackPointer);
                    }
                    ProgramError.unexpected("Should not reach here, unwind must jump to the exception handler!");
                }
                break;
            }
            case RAW_INSPECTING: {
                final RawStackFrameVisitor stackFrameVisitor = (RawStackFrameVisitor) context;
                final int flags = RawStackFrameVisitor.Util.makeFlags(isTopFrame, false);
                if (!stackFrameVisitor.visitFrame(targetMethod, instructionPointer, stackPointer, stackPointer, flags)) {
                    return false;
                }
                break;
            }
            case INSPECTING: {
                final StackFrameVisitor stackFrameVisitor = (StackFrameVisitor) context;
                if (!stackFrameVisitor.visitFrame(new AMD64JavaStackFrame(stackFrameWalker.calleeStackFrame(), targetMethod, instructionPointer, stackPointer, stackPointer))) {
                    return false;
                }
                break;
            }
        }

        final Pointer callerInstructionPointer = stackFrameWalker.readWord(ripPointer, 0).asPointer();
        final Pointer callerStackPointer = ripPointer.plus(Word.size()); // Skip RIP word
        final Pointer callerFramePointer;
        if (targetMethod.classMethodActor() != null && targetMethod.classMethodActor().isTrapStub()) {
            // framePointer is whatever was in the frame pointer register at the time of the trap
            final Pointer trapState = AMD64TrapStateAccess.getTrapStateFromRipPointer(ripPointer);
            callerFramePointer = stackFrameWalker.readWord(trapState, AMD64GeneralRegister64.RBP.value() * Word.size()).asPointer();
        } else {
            // framePointer == stackPointer for this scheme.
            callerFramePointer = callerStackPointer;
        }
        stackFrameWalker.advance(callerInstructionPointer, callerStackPointer, callerFramePointer);
        return true;
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (MaxineVM.isHosted()) {
            unwindMethod = ClassActor.fromJava(AMD64CPSCompiler.class).findLocalClassMethodActor(SymbolTable.makeSymbol("unwind"), null);
            assert unwindMethod != null;
        }
    }

    private static ClassMethodActor unwindMethod;

    private static int unwindFrameSize = -1;

    private static int getUnwindFrameSize() {
        if (unwindFrameSize == -1) {
            unwindFrameSize = CompilationScheme.Static.getCurrentTargetMethod(unwindMethod).frameSize();
        }
        return unwindFrameSize;
    }

    @NEVER_INLINE
    private static void unwindToCalleeEpilogue(Throwable throwable, Address catchAddress, Pointer stackPointer, TargetMethod lastJavaCallee) {
        // Overwrite return address of callee with catch address
        final Pointer returnAddressPointer = stackPointer.minus(Word.size());
        returnAddressPointer.setWord(catchAddress);

        assert lastJavaCallee.registerRestoreEpilogueOffset() != -1;
        Address epilogueAddress = lastJavaCallee.codeStart().plus(lastJavaCallee.registerRestoreEpilogueOffset());

        final Pointer calleeStackPointer = stackPointer.minus(Word.size()).minus(lastJavaCallee.frameSize());
        unwind(throwable, epilogueAddress, calleeStackPointer);
    }

    /**
     * Unwinds a thread's stack to an exception handler.
     * <p>
     * The compiled version of this method must have it's own frame but the frame size must be known at image build
     * time. This is because this code manually adjusts the stack pointer.
     * <p>
     * The critical state of the registers before the RET instruction is:
     * <ul>
     * <li>RAX must hold the exception object</li>
     * <li>RSP must be one word less than the stack pointer of the handler frame that is the target of the unwinding</li>
     * <li>The value at [RSP] must be the address of the handler code</li>
     * </ul>
     *
     * @param throwable the exception object
     * @param catchAddress the address of the exception handler code
     * @param stackPointer the stack pointer denoting the frame of the handler to which the stack is unwound upon
     *            returning from this method
     */
    @NEVER_INLINE
    private static void unwind(Throwable throwable, Address catchAddress, Pointer stackPointer) {
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
}
