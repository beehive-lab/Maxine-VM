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
package com.sun.max.vm.stack;

import static com.sun.max.vm.jni.JniFunctionWrapper.*;
import static com.sun.max.vm.stack.StackFrameWalker.Purpose.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.snippet.NativeStubSnippet.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * The mechanism for iterating over the frames in a thread's stack.
 *
 * @author Doug Simon
 * @author Laurent Daynes
 */
public abstract class StackFrameWalker {

    private final CompilerScheme compilerScheme;

    protected StackFrameWalker(CompilerScheme compilerScheme) {
        this.compilerScheme = compilerScheme;
    }

    /**
     * Constants denoting the finite set of reasons for which a stack walk can be performed.
     * Every implementation of {@link DynamicCompilerScheme#walkFrame(StackFrameWalker, boolean, TargetMethod, Purpose, Object)}
     * must deal with each type of stack walk.
     *
     * @author Doug Simon
     */
    public enum Purpose {
        /**
         * Raising an exception.
         * This type of stack walk is allocation free.
         */
        EXCEPTION_HANDLING(StackUnwindingContext.class),

        /**
         * Preparing {@linkplain StackReferenceMapPreparer stack reference map} for a thread's stack.
         * This type of stack walk is allocation free.
         */
        REFERENCE_MAP_PREPARING(StackReferenceMapPreparer.class),

        /**
         * Reflecting on the frames of a thread's stack.
         * This type of stack walk is allocation free.
         */
        RAW_INSPECTING(RawStackFrameVisitor.class),

        /**
         * Reflecting on the frames of a thread's stack.
         * This type of stack walk is not allocation free.
         */
        INSPECTING(StackFrameVisitor.class);

        private final Class contextType;

        private Purpose(Class contextType) {
            this.contextType = contextType;
        }

        /**
         * Determines if a given context object is of the type expected by this purpose.
         */
        public final boolean isValidContext(Object context) {
            return contextType.isInstance(context);
        }
    }

    private Purpose purpose = null;
    private Pointer stackPointer = Pointer.zero();
    private Pointer framePointer;
    private Pointer instructionPointer;
    private StackFrame calleeStackFrame;
    private Pointer trapState;

    private static final CriticalMethod MaxineVM_run = new CriticalMethod(MaxineVM.class, "run", MaxineVM.RUN_METHOD_SIGNATURE);
    private static final CriticalMethod VmThread_run = new CriticalMethod(VmThread.class, "run", VmThread.RUN_METHOD_SIGNATURE);

    /**
     * Walks a thread's stack.
     * <p>
     * Note that this method does not explicitly {@linkplain #reset() reset} this stack walker. If this walk is for the
     * purpose of raising an exception, then the code that unwinds the stack to the exception handler frame is expected
     * to reset this walker. For all other purposes, the caller of this method must reset this walker.
     *
     * @param instructionPointer the instruction pointer of the code executing in the top frame
     * @param stackPointer a pointer denoting an ISA defined location in the top frame
     * @param framePointer a pointer denoting an ISA defined location in the top frame
     * @param purpose the reason this walk is being performed
     * @param context a purpose-specific object of a type {@linkplain Purpose#isValidContext(Object) compatible} with
     *            {@code purpose}
     */
    private void walk(Pointer instructionPointer, Pointer stackPointer, Pointer framePointer, Purpose purpose, Object context) {

        checkPurpose(purpose, context);

        this.trapState = Pointer.zero();
        this.purpose = purpose;
        this.instructionPointer = instructionPointer;
        this.framePointer = framePointer;
        this.stackPointer = stackPointer;
        boolean isTopFrame = true;
        boolean inNative = isThreadInNative();

        TargetMethod lastJavaCallee = null;
        Pointer lastJavaCalleeStackPointer = Pointer.zero();
        Pointer lastJavaCalleeFramePointer = Pointer.zero();
        while (!this.stackPointer.isZero()) {
            final TargetMethod targetMethod = targetMethodFor(this.instructionPointer);
            if (targetMethod != null && (!inNative || purpose == INSPECTING || purpose == RAW_INSPECTING)) {
                // Java frame
                checkVmEntrypointCaller(lastJavaCallee, targetMethod);

                final DynamicCompilerScheme compilerScheme = targetMethod.compilerScheme();
                // Record the last Java callee to be the current frame *before* the compiler scheme
                // updates the current frame during the call to walkJavaFrame()
                lastJavaCalleeStackPointer = this.stackPointer;
                lastJavaCalleeFramePointer = this.framePointer;
                lastJavaCallee = targetMethod;

                if (!compilerScheme.walkFrame(this, isTopFrame, targetMethod, purpose, context)) {
                    return;
                }
                if (!targetMethod.classMethodActor().isTrapStub()) {
                    trapState = Pointer.zero();
                }
            } else {
                final RuntimeStub stub = runtimeStubFor(this.instructionPointer);
                if (stub != null && (!inNative || purpose == INSPECTING || purpose == RAW_INSPECTING)) {
                    if (!stub.walkFrame(this, isTopFrame, purpose, context)) {
                        return;
                    }
                } else {
                    if (purpose == INSPECTING) {
                        final StackFrameVisitor stackFrameVisitor = (StackFrameVisitor) context;
                        if (!stackFrameVisitor.visitFrame(new NativeStackFrame(calleeStackFrame, this.instructionPointer, this.framePointer, this.stackPointer))) {
                            return;
                        }
                    } else if (purpose == RAW_INSPECTING) {
                        final RawStackFrameVisitor stackFrameVisitor = (RawStackFrameVisitor) context;
                        final int flags = RawStackFrameVisitor.Util.makeFlags(isTopFrame, false);
                        if (!stackFrameVisitor.visitFrame(null, this.instructionPointer, this.framePointer, this.stackPointer, flags)) {
                            return;
                        }
                    }

                    if (inNative) {
                        inNative = false;
                        advanceFrameInNative(purpose);
                    } else {
                        if (stub != null) {
                            if (!stub.walkFrame(this, isTopFrame, purpose, context)) {
                                return;
                            }
                        } else {
                            if (lastJavaCallee == null) {
                                // This is the native thread start routine (i.e. VmThread.run())
                                return;
                            }

                            final ClassMethodActor lastJavaCalleeMethodActor = lastJavaCallee.classMethodActor();
                            if (lastJavaCalleeMethodActor.isCFunction()) {
                                if (lastJavaCalleeMethodActor.isTrapStub()) {
                                    // This can only occur in the inspector and implies that execution is in the platform specific
                                    // prologue of Trap.trapStub() before the point where the trap frame has been completed. In
                                    // particular, the return instruction pointer slot has not been updated with the instruction
                                    // pointer at which the fault occurred.
                                    return;
                                }
                                if (!advanceCFunctionFrame(purpose, lastJavaCallee, lastJavaCalleeStackPointer, lastJavaCalleeFramePointer, context)) {
                                    return;
                                }
                            } else {
                                Log.print("Native code called/entered a Java method not annotated with @C_FUNCTION: ");
                                Log.print(lastJavaCalleeMethodActor.name.string);
                                Log.print(lastJavaCalleeMethodActor.descriptor().string);
                                Log.print(" in ");
                                Log.println(lastJavaCalleeMethodActor.holder().name.string);
                                FatalError.unexpected("Native code called/entered a Java method that is not a JNI function, a Java trap stub");
                            }
                        }
                    }
                }
                lastJavaCallee = null;
            }
            isTopFrame = false;
        }
    }

    private void checkVmEntrypointCaller(TargetMethod lastJavaCallee, final TargetMethod targetMethod) {
        if (lastJavaCallee != null) {
            final ClassMethodActor classMethodActor = lastJavaCallee.classMethodActor();
            if (classMethodActor.isCFunction() && !classMethodActor.isTrapStub()) {
                Log.print("Caller of VM entry point (@C_FUNCTION method) \"");
                Log.print(lastJavaCallee.name());
                Log.print("\" is not native code: ");
                Log.print(targetMethod.name());
                Log.print(targetMethod.classMethodActor().descriptor().string);
                Log.print(" in ");
                Log.println(targetMethod.classMethodActor().holder().name.string);
                FatalError.unexpected("Caller of a VM entry point (@C_FUNCTION method) must be native code");
            }
        }
    }

    private boolean isRunMethod(final ClassMethodActor lastJavaCalleeMethodActor) {
        return lastJavaCalleeMethodActor.equals(MaxineVM_run.classMethodActor) || lastJavaCalleeMethodActor.equals(VmThread_run.classMethodActor);
    }

    /**
     * Advances this stack walker through the frame of a method annotated with {@link C_FUNCTION}.
     *
     * @param purpose the reason this walk is being performed
     * @param lastJavaCallee
     * @param lastJavaCalleeStackPointer
     * @param lastJavaCalleeFramePointer
     * @return true if the stack walker was advanced to the caller of the method annotated with {@link C_FUNCTION}, false otherwise
     */
    private boolean advanceCFunctionFrame(Purpose purpose, TargetMethod lastJavaCallee, Pointer lastJavaCalleeStackPointer, Pointer lastJavaCalleeFramePointer, Object context) {
        final ClassMethodActor lastJavaCalleeMethodActor = lastJavaCallee.classMethodActor();
        if (lastJavaCalleeMethodActor.isJniFunction()) {
            final Pointer namedVariablesBasePointer = compilerScheme.namedVariablesBasePointer(lastJavaCalleeStackPointer, lastJavaCalleeFramePointer);
            final Word lastJavaCallerInstructionPointer = readWord(savedLastJavaCallerInstructionPointer().address(lastJavaCallee, namedVariablesBasePointer), 0);
            final Word lastJavaCallerStackPointer = readWord(savedLastJavaCallerStackPointer().address(lastJavaCallee, namedVariablesBasePointer), 0);
            final Word lastJavaCallerFramePointer = readWord(savedLastJavaCallerFramePointer().address(lastJavaCallee, namedVariablesBasePointer), 0);
            advance(getNativeFunctionCallInstructionPointerInNativeStub(lastJavaCallerInstructionPointer.asPointer(), true),
                    lastJavaCallerStackPointer,
                    lastJavaCallerFramePointer);
            return true;
        }
        if (!isRunMethod(lastJavaCalleeMethodActor)) {
            FatalError.check(purpose == INSPECTING || purpose == RAW_INSPECTING, "Could not unwind stack past Java method annotated with @C_FUNCTION");
        }

        return false;
    }

    /**
     * Advances this walker past the first frame encountered when walking the stack of a thread that is executing
     * {@linkplain VmThread#isInNative() in native} code.
     */
    private void advanceFrameInNative(Purpose purpose) {
        Pointer lastJavaCallerInstructionPointer = readPointer(LAST_JAVA_CALLER_INSTRUCTION_POINTER);
        final Word lastJavaCallerStackPointer;
        final Word lastJavaCallerFramePointer;
        if (!lastJavaCallerInstructionPointer.isZero()) {
            lastJavaCallerStackPointer = readPointer(LAST_JAVA_CALLER_STACK_POINTER);
            lastJavaCallerFramePointer = readPointer(LAST_JAVA_CALLER_FRAME_POINTER);
        } else {
            FatalError.check(purpose == INSPECTING || purpose == RAW_INSPECTING, "Thread cannot be 'in native' without having recorded the last Java caller in thread locals");
            // This code is currently only used by the inspector. The inspector might pause a thread when it is
            // in a C function. We use the LAST_JAVA_CALLER_INSTRUCTION_POINTER_FOR_C in a VM thread's locals
            // to display the Java stack
            lastJavaCallerInstructionPointer = readPointer(LAST_JAVA_CALLER_INSTRUCTION_POINTER_FOR_C);
            lastJavaCallerStackPointer = readPointer(LAST_JAVA_CALLER_STACK_POINTER_FOR_C);
            lastJavaCallerFramePointer = readPointer(LAST_JAVA_CALLER_FRAME_POINTER_FOR_C);

            FatalError.check(!lastJavaCallerInstructionPointer.isZero(), "Thread cannot be 'in native' without having recorded the last Java caller in thread locals");
        }
        advance(getNativeFunctionCallInstructionPointerInNativeStub(lastJavaCallerInstructionPointer, purpose != INSPECTING && purpose != RAW_INSPECTING),
                lastJavaCallerStackPointer,
                lastJavaCallerFramePointer);
    }

    private void checkPurpose(Purpose purpose, Object context) {
        if (!purpose.isValidContext(context)) {
            FatalError.unexpected("Invalid stack walk context");
        }

        if (!stackPointer.isZero()) {
            Log.print("Stack walker already in use for ");
            Log.println(this.purpose.name());
            stackPointer = Pointer.zero();
            this.purpose = null;
            FatalError.unexpected("Stack walker already in use");
        }
    }

    /**
     * Gets the address of the call to the native function in a {@linkplain NativeStubGenerator native stub}. That is,
     * this method gets the address of the instruction to which the native code will return.
     *
     * Finding this address relies on the invariant that the next {@linkplain StopType stop} after the instruction
     * pointer recorded by the {@linkplain NativeCallPrologue native call prologue} is for the call to the native
     * function.
     *
     * @param instructionPointer the instruction pointer in a native stub as saved by {@link NativeCallPrologue} or
     *            {@link NativeCallPrologueForC}
     * @param fatalIfNotFound specifies whether a {@linkplain FatalError fatal error} should be raised if no call
     *            instruction can be found after {@code instructionPointer}. If this value is false and no call
     *            instruction is found, then {@code instructionPointer} is returned.
     * @return the address of the first call instruction after {@code instructionPointer}
     */
    private Pointer getNativeFunctionCallInstructionPointerInNativeStub(Pointer instructionPointer, boolean fatalIfNotFound) {
        final TargetMethod nativeStubTargetMethod = targetMethodFor(instructionPointer);
        //FatalError.check(nativeStubTargetMethod.classMethodActor().isNative(), "Instruction pointer not within a native stub");
        if (nativeStubTargetMethod != null) {
            final int targetCodePosition = nativeStubTargetMethod.targetCodePositionFor(instructionPointer);
            final int nativeFunctionCallPosition = nativeStubTargetMethod.findNextCall(targetCodePosition);
            final Pointer nativeFunctionCall = nativeFunctionCallPosition < 0 ? Pointer.zero() : nativeStubTargetMethod.codeStart().plus(nativeFunctionCallPosition);
            if (!nativeFunctionCall.isZero()) {
                // The returned instruction pointer must be one past the actual address of the
                // native function call. This makes it match the pattern expected by the
                // StackReferenceMapPreparer when the instruction pointer in all but the
                // top frame is past the address of the call.
                return nativeFunctionCall.plus(1);
            }
        }
        if (fatalIfNotFound) {
            if (nativeStubTargetMethod == null) {
                Log.print("Could not find native stub for instruction pointer ");
                Log.println(instructionPointer);
            } else {
                Log.print("Could not find native function call after ");
                Log.print(nativeStubTargetMethod.codeStart());
                Log.print("+");
                Log.print(instructionPointer.minus(nativeStubTargetMethod.codeStart()).toLong());
                Log.print(" in ");
                Log.printMethodActor(nativeStubTargetMethod.classMethodActor(), true);
            }
            throw FatalError.unexpected("Could not find native function call in native stub");
        }
        return instructionPointer;
    }

    /**
     * Walks a thread's stack for the purpose of inspecting one or more frames on the stack. This method takes care of
     * {@linkplain #reset() resetting} this walker before returning.
     */
    public final void inspect(Pointer instructionPointer, Pointer stackPointer, Pointer framePointer, final StackFrameVisitor visitor) {
        // Wraps the visit operation to record the visited frame as the parent of the next frame to be visited.
        final StackFrameVisitor wrapper = new StackFrameVisitor() {
            public boolean visitFrame(StackFrame stackFrame) {
                if (calleeStackFrame == null || !stackFrame.isSameFrame(calleeStackFrame)) {
                    calleeStackFrame = stackFrame;
                } else {
                    Log.println("Same frame being visited twice: " + stackFrame);
                }
                return visitor.visitFrame(stackFrame);
            }
        };
        walk(instructionPointer, stackPointer, framePointer, INSPECTING, wrapper);
        calleeStackFrame = null;
        reset();
    }

    /**
     * Walks a thread's stack for the purpose of inspecting one or more frames on the stack. This method takes care of
     * {@linkplain #reset() resetting} this walker before returning.
     */
    public final void inspect(Pointer instructionPointer, Pointer stackPointer, Pointer framePointer, final RawStackFrameVisitor visitor) {
        walk(instructionPointer, stackPointer, framePointer, RAW_INSPECTING, visitor);
        calleeStackFrame = null;
        reset();
    }

    /**
     * Walks a thread's stack for the purpose of raising an exception.
     */
    public final void unwind(Pointer instructionPointer, Pointer stackPointer, Pointer framePointer, Throwable throwable) {
        walk(instructionPointer, stackPointer, framePointer, EXCEPTION_HANDLING, compilerScheme.makeStackUnwindingContext(stackPointer, framePointer, throwable));
    }

    /**
     * Walks a thread's stack for the purpose of preparing the reference map of a thread's stack. This method takes care of
     * {@linkplain #reset() resetting} this walker before returning.
     */
    public final void prepareReferenceMap(Pointer instructionPointer, Pointer stackPointer, Pointer framePointer, StackReferenceMapPreparer preparer) {
        walk(instructionPointer, stackPointer, framePointer, REFERENCE_MAP_PREPARING, preparer);
        reset();
    }

    /**
     * Terminates the current stack walk.
     */
    @INLINE
    public final void reset() {
        trapState = Pointer.zero();
        stackPointer = Pointer.zero();
        purpose = null;
    }

    /**
     * Gets the last stack frame {@linkplain StackFrameVisitor#visitFrame(StackFrame) visited} by this stack walker
     * while {@linkplain #inspect(Pointer, Pointer, Pointer, StackFrameVisitor) inspecting}. The returned frame is
     * the callee frame of the next frame to be visited.
     */
    public final StackFrame calleeStackFrame() {
        return calleeStackFrame;
    }

    /**
     * Determines if the thread is executing in native code based on the
     * value of {@link VmThreadLocal#LAST_JAVA_CALLER_INSTRUCTION_POINTER}.
     */
    public abstract boolean isThreadInNative();

    public abstract TargetMethod targetMethodFor(Pointer instructionPointer);

    protected abstract RuntimeStub runtimeStubFor(Pointer instructionPointer);

    /**
     * Determines if this stack walker is currently in use. This is useful for detecting if an exception is being thrown as part of exception handling.
     */
    public final boolean isInUse() {
        return !stackPointer.isZero();
    }

    @INLINE
    public final Pointer stackPointer() {
        return stackPointer;
    }

    @INLINE
    public final void advance(Word instructionPointer, Word stackPointer, Word framePointer) {
        this.instructionPointer = instructionPointer.asPointer();
        this.stackPointer = stackPointer.asPointer();
        this.framePointer = framePointer.asPointer();
    }

    @INLINE
    public final Pointer framePointer() {
        return framePointer;
    }

    @INLINE
    public final Pointer instructionPointer() {
        return instructionPointer;
    }

    /**
     * Records the trap state when walking a trap frame. This information can be subsequently {@linkplain #trapState()
     * accessed} when walking the frame in which the trap occurred.
     *
     * @param trapState the state pertinent to a trap
     */
    public void setTrapState(Pointer trapState) {
        this.trapState = trapState;
    }

    /**
     * Gets the state stored in the trap frame just below the frame currently being walked (i.e. the frame in which the
     * trap occurred).
     *
     * @return {@link Pointer#zero()} if the current frame is not a frame in which a trap occurred
     */
    public Pointer trapState() {
        return trapState;
    }

    /**
     * Collects a sequence of stack frames, beginning a stack walk at the specified instruction pointer, stack pointer,
     * and frame pointer. This method will return all stack frames, including native frames, adapter frames, and
     * non-application visible stack frames. This method accepts an appendable sequence of stack frames
     *
     * @param stackFrames an appendable sequence of stack frames to collect the results; if {@code null}, this method
     * will create a new appendable sequence for collecting the result
     * @param instructionPointer the instruction pointer from which to begin the stack walk
     * @param stackPointer the stack pointer from which to begin the stack walk
     * @param framePointer the frame pointer from which to begin the stack walk
     * @return a sequence of all the stack frames, including native, adapter, and non-application visible stack frames,
     *         with the top frame as the first frame
     */
    public Sequence<StackFrame> frames(AppendableSequence<StackFrame> stackFrames, Pointer instructionPointer, Pointer stackPointer, Pointer framePointer) {
        final AppendableSequence<StackFrame> frames = stackFrames == null ? new LinkSequence<StackFrame>() : stackFrames;
        final StackFrameVisitor visitor = new StackFrameVisitor() {
            public boolean visitFrame(StackFrame stackFrame) {
                frames.append(stackFrame);
                return true;
            }
        };
        inspect(instructionPointer, stackPointer, framePointer, visitor);
        return frames;
    }

    /**
     * Extracts a sequence of class method actors from a sequence of stack frames. It accepts a number of options that
     * indicate whether to include the top frame, adapter frames, native frames, and other frames that should not be
     * application visible.
     *
     * @param stackFrames an iterable list of stack frames
     * @param topFrame true if this method should include the ClassMethodActor of the top frame
     * @param adapterFrames true if adapter frames should be reported
     * @param invisibleFrames true if invisible frames should be reported
     * @return
     */
    public static Sequence<ClassMethodActor> extractClassMethodActors(Iterable<StackFrame> stackFrames, boolean topFrame, boolean adapterFrames, boolean invisibleFrames) {
        final AppendableSequence<ClassMethodActor> result = new LinkSequence<ClassMethodActor>();
        boolean top = true;
        for (StackFrame stackFrame : stackFrames) {
            if (top) {
                top = false;
                if (!topFrame) {
                    continue;
                }
            }
            if (stackFrame.isAdapter() && !adapterFrames) {
                continue;
            }
            final TargetMethod targetMethod = Code.codePointerToTargetMethod(stackFrame.instructionPointer);
            if (targetMethod == null) {
                // native frame
                continue;
            }
            final Iterator<? extends BytecodeLocation> bytecodeLocations = targetMethod.getBytecodeLocationsFor(stackFrame.instructionPointer);
            if (bytecodeLocations == null) {
                appendClassMethodActor(result, targetMethod.classMethodActor(), invisibleFrames);
            } else {
                appendCallers(result, bytecodeLocations, invisibleFrames);
            }
        }
        return result;
    }

    private static void appendCallers(AppendableSequence<ClassMethodActor> result, Iterator<? extends BytecodeLocation> bytecodeLocations, boolean invisibleFrames) {
        // this recursive method appends inlined bytecode locations to the frame list (i.e. parent first)
        if (bytecodeLocations.hasNext()) {
            final BytecodeLocation bytecodeLocation = bytecodeLocations.next();
            appendCallers(result, bytecodeLocations, invisibleFrames);
            appendClassMethodActor(result, bytecodeLocation.classMethodActor(), invisibleFrames);
        }
    }

    private static void appendClassMethodActor(final AppendableSequence<ClassMethodActor> result, final ClassMethodActor classMethodActor, boolean invisibleFrames) {
        if (classMethodActor.isApplicationVisible() || invisibleFrames) {
            result.append(classMethodActor);
        }
    }

    public abstract Word readWord(Address address, int offset);
    public abstract byte readByte(Address address, int offset);
    public abstract int readInt(Address address, int offset);

    public abstract Word readFramelessCallAddressRegister(TargetABI targetABI);

    /**
     * Reads the value of a given VM thread local from the safepoint-enabled thread locals.
     *
     * @param local the VM thread local to read
     * @return the value (as a word) of {@code local} in the safepoint-enabled thread locals
     */
    public abstract Word readWord(VmThreadLocal local);

    /**
     * Reads the value of a given VM thread local from the safepoint-enabled thread locals.
     *
     * @param local the VM thread local to read
     * @return the value (as a pointer) of {@code local} in the safepoint-enabled thread locals
     */
    public Pointer readPointer(VmThreadLocal local) {
        return readWord(local).asPointer();
    }

    /**
     * Updates the stack walker's frame and stack pointers with those specified by the target ABI (use the ABI stack and frame pointers).
     * This may be necessary when initiating stack walking: by default the stack frame walker uses the stack and frame pointers defined by the CPU.
     * This is incorrect when the ABI pointers differs from the CPU pointers (like it is the case with some JIT implementation, currently).
     * @param targetABI
     */
    public abstract void useABI(TargetABI targetABI);
}
