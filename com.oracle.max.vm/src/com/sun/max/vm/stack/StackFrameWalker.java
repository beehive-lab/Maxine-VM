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
package com.sun.max.vm.stack;

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.compiler.deopt.Deoptimization.*;
import static com.sun.max.vm.compiler.target.Stub.*;
import static com.sun.max.vm.compiler.target.Stub.Type.*;
import static com.sun.max.vm.stack.StackFrameWalker.Purpose.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * A walker that iterates over the frames in a thread's stack.
 * <p>
 * The walker manages two {@linkplain StackFrameCursor cursors} internally, each of which encapsulates
 * the state needed about a stack frame: one for the <em>current</em>
 * frame (caller frame) and one for the <em>previous</em> frame (callee frame).
 * <p>
 * The walker destructively updates the cursors' contents, rather than allocating new ones,
 * since allocation is disallowed when walking the stack for reference map preparation.
 *
 * @see StackFrameCursor
 */
public abstract class StackFrameWalker {

    /**
     * A VM option for enabling stack frame walk tracing.
     */
    public static boolean TraceStackWalk;
    static {
        VMOptions.addFieldOption("-XX:", "TraceStackWalk", "Trace every stack walk");
    }

    /**
     * Constants denoting the finite set of reasons for which a stack walk can be performed.
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
        INSPECTING(null);

        private final Class contextType;

        private Purpose(Class contextType) {
            this.contextType = contextType;
        }

        /**
         * Determines if a given context object is of the type expected by this purpose.
         */
        public final boolean isValidContext(Object context) {
            if (this == INSPECTING) {
                return true;
            }
            return contextType.isInstance(context);
        }
    }

    protected StackFrameCursor current;
    protected StackFrameCursor callee;

    private Purpose purpose = null;
    private Pointer currentAnchor;

    @HOSTED_ONLY
    private StackFrame calleeStackFrame;

    protected StackFrameWalker() {
        // These initializations are overridden by the Inspector with variant cursor implementations.
        // This is an awkward and fragile way to handle the override, but it avoids object initialization problems
        // during construction of the Inspector's specialization of the walker.
        this.current = new StackFrameCursor(this);
        this.callee = new StackFrameCursor(this);
    }

    /**
     * Walks a thread's stack.
     * <p>
     * Note that this method does not explicitly {@linkplain #reset() reset} this stack walker. If this walk is for the
     * purpose of raising an exception, then the code that unwinds the stack to the exception handler frame is expected
     * to reset this walker. For all other purposes, the caller of this method must reset this walker.
     *
     * @param ip the instruction pointer of the code executing in the top frame
     * @param sp a pointer denoting an ISA defined location in the top frame
     * @param fp a pointer denoting an ISA defined location in the top frame
     * @param purpose the reason this walk is being performed
     * @param context a purpose-specific object of a type {@linkplain Purpose#isValidContext(Object) compatible} with
     *            {@code purpose}
     */
    private void walk(Pointer ip, Pointer sp, Pointer fp, Purpose purpose, Object context) {
        checkPurpose(purpose, context);

        current.reset();
        callee.reset();

        current.ip.derive(ip);
        current.sp = sp;
        current.fp = fp;
        current.isTopFrame = true;

        this.purpose = purpose;
        this.currentAnchor = readPointer(LAST_JAVA_FRAME_ANCHOR);
        boolean isTopFrame = true;
        boolean inNative = !currentAnchor.isZero() && !readWord(currentAnchor, JavaFrameAnchor.PC.offset).isZero();


        while (!current.sp.isZero()) {
            TargetMethod tm = current.targetMethod();
            TargetMethod calleeTM = callee.targetMethod();
            traceCursor(current);

            if (tm != null && (!inNative || (purpose == INSPECTING || purpose == RAW_INSPECTING))) {

                // found target method
                inNative = false;

                checkVmEntrypointCaller(calleeTM, tm);

                // walk the frame
                if (!walkFrame(current, callee, tm, purpose, context)) {
                    break;
                }
            } else {
                // did not find target method => in native code
                if (purpose == INSPECTING) {
                    final StackFrameVisitor stackFrameVisitor = (StackFrameVisitor) context;
                    if (!stackFrameVisitor.visitFrame(new NativeStackFrame(calleeStackFrame, current.nativeIP(), current.fp, current.sp))) {
                        break;
                    }
                } else if (purpose == RAW_INSPECTING) {
                    final RawStackFrameVisitor stackFrameVisitor = (RawStackFrameVisitor) context;
                    current.isTopFrame = isTopFrame;
                    if (!stackFrameVisitor.visitFrame(current, callee)) {
                        break;
                    }
                }

                if (inNative) {
                    inNative = false;
                    Pointer anchor = nextNativeStubAnchor();
                    advanceFrameInNative(anchor, purpose);
                } else {
                    if (calleeTM == null) {
                        // This is a native function that called a VM entry point such as VmThread.run(),
                        // MaxineVM.run() or a JNI function.
                        break;
                    }

                    ClassMethodActor lastJavaCalleeMethodActor = calleeTM.classMethodActor();
                    if (calleeTM.is(TrapStub)) {
                        Pointer anchor = nextNativeStubAnchor();
                        if (!anchor.isZero()) {
                            // This can only occur in the Inspector and implies that execution is in
                            // the trap stub as a result a trap or signal while execution was in
                            // native code.
                            advanceFrameInNative(anchor, purpose);
                        } else {
                            // This can only occur in the Inspector and implies that execution is in the trab stub
                            // before the point where the trap frame has been completed. In
                            // particular, the return instruction pointer slot has not been updated with the instruction
                            // pointer at which the fault occurred.
                            break;
                        }
                    } else if (lastJavaCalleeMethodActor != null && lastJavaCalleeMethodActor.isVmEntryPoint()) {
                        if (!advanceVmEntryPointFrame(calleeTM)) {
                            break;
                        }
                    } else if (lastJavaCalleeMethodActor == null) {
                        FatalError.unexpected("Unrecognized target method without a class method actor!");
                    } else {
                        Log.print("Native code called/entered a Java method that is not a JNI function, a Java trap stub or a VM/thread entry point: ");
                        Log.print(lastJavaCalleeMethodActor.name.string);
                        Log.print(lastJavaCalleeMethodActor.descriptor().string);
                        Log.print(" in ");
                        Log.println(lastJavaCalleeMethodActor.holder().name.string);
                        if (!FatalError.inFatalError()) {
                            FatalError.unexpected("Native code called/entered a Java method that is not a JNI function, a Java trap stub or a VM/thread entry point");
                        } else {
                            reset();
                            break;
                        }
                    }
                }
            }
            isTopFrame = false;
        }
    }

    private boolean walkFrame(StackFrameCursor current, StackFrameCursor callee, TargetMethod targetMethod, Purpose purpose, Object context) {
        boolean proceed = true;
        if (purpose == Purpose.REFERENCE_MAP_PREPARING) {
            // walk the frame for reference map preparation
            StackReferenceMapPreparer preparer = (StackReferenceMapPreparer) context;
            if (preparer.checkIgnoreCurrentFrame()) {
                proceed = true;
            } else {
                targetMethod.prepareReferenceMap(current, callee, preparer);
                Pointer limit = preparer.completingReferenceMapLimit();
                if (!limit.isZero() && current.sp().greaterEqual(limit)) {
                    proceed = false;
                }
            }
        } else if (purpose == Purpose.EXCEPTION_HANDLING) {
            // walk the frame for exception handling
            Throwable throwable = ((StackUnwindingContext) context).throwable;
            targetMethod.catchException(current, callee, throwable);
        } else if (purpose == Purpose.INSPECTING) {
            // walk the frame for inspecting (Java frames)
            StackFrameVisitor visitor = (StackFrameVisitor) context;
            proceed = targetMethod.acceptStackFrameVisitor(current, visitor);
        } else if (purpose == Purpose.RAW_INSPECTING) {
            // walk the frame for inspect (compiled frames)
            RawStackFrameVisitor visitor = (RawStackFrameVisitor) context;
            proceed = visitor.visitFrame(current, callee);
        }
        // in any case, advance to the next frame
        if (proceed) {
            targetMethod.advance(current);
            return true;
        }
        return false;
    }

    private void traceWalkPurpose(Purpose purpose) {
        if (TraceStackWalk) {
            Log.print("StackFrameWalk: Start stack frame walk for purpose ");
            Log.println(purpose);
        }
    }

    private void traceCursor(StackFrameCursor cursor) {
        if (TraceStackWalk) {
            if (cursor.targetMethod() != null) {
                Log.print("StackFrameWalk: Frame at ");
                Log.printLocation(cursor.targetMethod(), cursor.vmIP(), false);
                Log.print(", isTopFrame=");
                Log.print(cursor.isTopFrame);
                Log.print(", sp=");
                Log.print(cursor.sp);
                Log.print(", fp=");
                Log.print(cursor.fp);
                Log.println("");
            } else {
                Log.print("StackFrameWalk: Frame for native function [IP=");
                Log.print(cursor.nativeIP());
                Log.println(']');
            }
        }
    }

    private void checkVmEntrypointCaller(TargetMethod lastJavaCallee, final TargetMethod targetMethod) {
        if (lastJavaCallee != null && lastJavaCallee.classMethodActor() != null) {
            final ClassMethodActor classMethodActor = lastJavaCallee.classMethodActor();
            if (classMethodActor.isVmEntryPoint()) {
                Log.print("Caller of VM entry point (@VM_ENTRY_POINT annotated method) \"");
                Log.print(lastJavaCallee.regionName());
                Log.print("\" is not native code: ");
                Log.print(targetMethod.regionName());
                Log.print(targetMethod.classMethodActor().descriptor().string);
                Log.print(" in ");
                Log.println(targetMethod.classMethodActor().holder().name.string);
                FatalError.unexpected("Caller of a VM entry point (@VM_ENTRY_POINT method) must be native code");
            }
        }
    }

    /**
     * Advances this stack walker through the frame of a method annotated with {@link VM_ENTRY_POINT}.
     *
     * @param lastJavaCallee the last java method called
     * @return true if the stack walker was advanced
     */
    private boolean advanceVmEntryPointFrame(TargetMethod lastJavaCallee) {
        final ClassMethodActor lastJavaCalleeMethodActor = lastJavaCallee.classMethodActor();
        if (lastJavaCalleeMethodActor != null && lastJavaCalleeMethodActor.isVmEntryPoint()) {
            Pointer anchor = nextNativeStubAnchor();
            if (anchor.isZero()) {
                return false;
            }
            final Word lastJavaCallerInstructionPointer = readWord(anchor, JavaFrameAnchor.PC.offset);
            final Word lastJavaCallerStackPointer = readWord(anchor, JavaFrameAnchor.SP.offset);
            final Word lastJavaCallerFramePointer = readWord(anchor, JavaFrameAnchor.FP.offset);

            boolean wasDisabled = SafepointPoll.disable();
            advance(checkNativeFunctionCall(CodePointer.from(lastJavaCallerInstructionPointer), true).toPointer(),
                    lastJavaCallerStackPointer,
                    lastJavaCallerFramePointer);
            if (!wasDisabled) {
                SafepointPoll.enable();
            }

            return true;
        }
        return false;
    }

    /**
     * Advances this walker past the first frame encountered when walking the stack of a thread that is executing
     * in native code.
     */
    private void advanceFrameInNative(Pointer anchor, Purpose purpose) {
        FatalError.check(!anchor.isZero(), "No native stub frame anchor found when executing 'in native'");
        CodePointer nativeFunctionCall = CodePointer.from(readWord(anchor, JavaFrameAnchor.PC.offset));
        if (nativeFunctionCall.isZero()) {
            FatalError.unexpected("Thread cannot be 'in native' without having recorded the last Java caller in thread locals");
        }
        CodePointer ip = checkNativeFunctionCall(nativeFunctionCall, purpose != INSPECTING && purpose != RAW_INSPECTING);
        if (ip.isZero()) {
            ip = nativeFunctionCall;
        }

        boolean wasDisabled = SafepointPoll.disable();
        advance(ip.toPointer(), readWord(anchor, JavaFrameAnchor.SP.offset), readWord(anchor, JavaFrameAnchor.FP.offset));
        if (!wasDisabled) {
            SafepointPoll.enable();
        }
    }

    /**
     * Gets the next anchor in a VM exit frame. That is, get the frame in the next {@linkplain NativeStubGenerator native stub} on the stack
     * above the current stack pointer.
     *
     * @return {@link Pointer#zero()} if there are no more native stub frame anchors
     */
    private Pointer nextNativeStubAnchor() {
        if (currentAnchor.isZero()) {
            // We're at a VM entry point that has no Java frames above it.
            return Pointer.zero();
        }

        // Skip over an anchor in a VM entry point frame. The test is the same as JavaFrameAnchor.inJava()
        // except we can't use the latter here if in a separate address space (e.g. the Inspector) than the VM.
        Pointer pc = readWord(currentAnchor, JavaFrameAnchor.PC.offset).asPointer();
        if (pc.isZero()) {
            currentAnchor = readWord(currentAnchor, JavaFrameAnchor.PREVIOUS.offset).asPointer();
            if (currentAnchor.isZero()) {
                // We're at a VM entry point that has no Java frames above it.
                return Pointer.zero();
            }
        }

        pc = readWord(currentAnchor, JavaFrameAnchor.PC.offset).asPointer();
        if (pc.isZero()) {
            // Java frame anchors should always alternate between VM entry and exit frames.
            FatalError.unexpected("Found two adjacent VM entry point frame anchors");
        }
        Pointer anchor = this.currentAnchor;
        this.currentAnchor = readWord(anchor, JavaFrameAnchor.PREVIOUS.offset).asPointer();

        if (!anchor.isZero()) {
            // Recurse if the anchor is below the current frame in the stack walk.
            // This can occur when a stack walk is initiated further up the stack
            // than the currently executing frame.
            if (readWord(anchor, JavaFrameAnchor.SP.offset).asPointer().lessThan(current.sp)) {
                return nextNativeStubAnchor();
            }
        }

        return anchor;
    }

    private void checkPurpose(Purpose purpose, Object context) {
        traceWalkPurpose(purpose);
        if (!purpose.isValidContext(context)) {
            FatalError.unexpected("Invalid stack walk context");
        }

        if (!current.sp.isZero()) {
            Log.print("Stack walker already in use for ");
            Log.println(this.purpose.name());
            current.reset();
            callee.reset();
            this.purpose = null;
            FatalError.unexpected("Stack walker already in use");
        }
    }

    /**
     * Checks the address of the native function call in a native method stub.
     *
     * @param pc the address of a native function call saved by {@link Snippets#nativeCallPrologue(NativeFunction)} or
     *            {@link Snippets#nativeCallPrologueForC(NativeFunction)}
     * @param fatalIfNotFound specifies whether a fatal error should be raised if {@code pc} is not a native call site
     * @return the value of {@code pc} if it is valid or zero if not
     */
    private CodePointer checkNativeFunctionCall(CodePointer pc, boolean fatalIfNotFound) {
        final TargetMethod nativeStub = targetMethodFor(pc.toPointer());
        if (nativeStub != null) {
            if (TraceStackWalk && purpose == Purpose.REFERENCE_MAP_PREPARING) {
                final int nativeFunctionCallPos = nativeStub.posFor(pc);
                Log.print("IP for stack frame preparation of stub for native method ");
                Log.print(nativeStub.name());
                Log.print(" [");
                Log.print(nativeStub.codeStart());
                Log.print("+");
                Log.print(nativeFunctionCallPos);
                Log.println(']');
            }
            return pc;
        }
        if (fatalIfNotFound) {
            Log.print("Could not find native stub for instruction pointer ");
            Log.println(pc);
            throw FatalError.unexpected("Could not find native function call in native stub");
        }
        return CodePointer.zero();
    }

    /**
     * Looks up a method based on a return address read from a frame. Depending on the architecture,
     * the return address must be adjusted before searching the code cache to ensure the
     * address within the method containing the call instruction.
     */
    private TargetMethod targetMethodForReturnAddress(Word retAddr) {
        if (platform().isa.offsetToReturnPC == 0) {
            // Adjust 'retAddr' to ensure it is within the call instruction.
            // This ensures we will always get the correct method, even if
            // the call instruction was the last instruction in a method.
            return targetMethodFor(retAddr.asPointer().minus(1));
        } else {
            return targetMethodFor(retAddr.asPointer());
        }
    }

    /**
     * Walks a thread's stack for the purpose of inspecting one or more frames on the stack. This method takes care of
     * {@linkplain #reset() resetting} this walker before returning.
     *
     * This method is only ever used in Inspector contexts, hence the annotation with {@link HOSTED_ONLY}.
     */
    @HOSTED_ONLY
    public final void inspect(Pointer ip, Pointer sp, Pointer fp, final StackFrameVisitor visitor) {
        // Wraps the visit operation to record the visited frame as the parent of the next frame to be visited.
        final StackFrameVisitor wrapper = new StackFrameVisitor() {
            @Override
            public boolean visitFrame(StackFrame stackFrame) {
                if (calleeStackFrame == null || !stackFrame.isSameFrame(calleeStackFrame)) {
                    calleeStackFrame = stackFrame;
                } else {
                    Log.println("Same frame being visited twice: " + stackFrame);
                }
                return visitor.visitFrame(stackFrame);
            }
        };
        walk(ip, sp, fp, INSPECTING, wrapper);
        calleeStackFrame = null;
        visitor.done();
        reset();
    }

    /**
     * Walks a thread's stack for the purpose of inspecting one or more frames on the stack. This method takes care of
     * {@linkplain #reset() resetting} this walker before returning.
     */
    public final void inspect(Pointer ip, Pointer sp, Pointer fp, final RawStackFrameVisitor visitor) {
        walk(ip, sp, fp, RAW_INSPECTING, visitor);
        visitor.done();
        reset();
    }

    private final StackUnwindingContext defaultStackUnwindingContext = new StackUnwindingContext();

    /**
     * Walks a thread's stack for the purpose of raising an exception.
     */
    public final void unwind(Pointer ip, Pointer sp, Pointer fp, Throwable throwable) {
        defaultStackUnwindingContext.throwable = throwable;
        defaultStackUnwindingContext.stackPointer = sp;
        walk(ip, sp, fp, EXCEPTION_HANDLING, defaultStackUnwindingContext);
    }

    /**
     * Walks a thread's stack for the purpose of preparing the reference map of a thread's stack. This method takes care of
     * {@linkplain #reset() resetting} this walker before returning.
     */
    public final void prepareReferenceMap(Pointer ip, Pointer sp, Pointer fp, StackReferenceMapPreparer preparer) {
        walk(ip, sp, fp, REFERENCE_MAP_PREPARING, preparer);
        reset();
    }

    /**
     * Walks a thread's stack for the purpose of preparing the reference map of a thread's stack. This method takes care of
     * {@linkplain #reset() resetting} this walker before returning.
     */
    public final void verifyReferenceMap(Pointer ip, Pointer sp, Pointer fp, StackReferenceMapPreparer preparer) {
        walk(ip, sp, fp, REFERENCE_MAP_PREPARING, preparer);
        reset();
    }

    /**
     * Terminates the current stack walk.
     */
    public final void reset() {
        if (TraceStackWalk) {
            Log.print("StackFrameWalk: Finish stack frame walk for purpose ");
            Log.println(purpose);
        }

        current.reset();
        callee.reset();
        purpose = null;
        defaultStackUnwindingContext.stackPointer = Pointer.zero();
        defaultStackUnwindingContext.throwable = null;
    }

    /**
     * Gets the last stack frame {@linkplain StackFrameVisitor#visitFrame(StackFrame) visited} by this stack walker
     * while {@linkplain #inspect(Pointer, Pointer, Pointer, StackFrameVisitor) inspecting}. The returned frame is
     * the callee frame of the next frame to be visited.
     */
    @HOSTED_ONLY
    public final StackFrame calleeStackFrame() {
        return calleeStackFrame;
    }

    /**
     * Determines if this stack walker is currently in use. This is useful for detecting if an exception is being thrown as part of exception handling.
     */
    public final boolean isInUse() {
        return !current.sp.isZero();
    }

    /**
     * Advances this stack walker such that {@link #current} becomes {@link #callee}.
     *
     * @param ip the instruction pointer of the new current frame (return address read from the current frame)
     * @param sp the stack pointer of the new current frame (stack pointer in the caller frame)
     * @param fp the frame pointer of the new current frame
     */
    public final void advance(Word retAddr, Word sp, Word fp) {
        callee.copyFrom(current);

        Pointer ip = retAddr.asPointer();

        TargetMethod tm = targetMethodForReturnAddress(retAddr);

        // Rescue a return address that has been patched for deoptimization
        if (isDeoptStubEntry(ip, tm)) {
            // Since 'ip' denotes the start of a deopt stub, then we're dealing with a patched return address
            // and the real caller is found in the rescue slot
            Pointer originalReturnAddress = readWord(sp.asAddress(), DEOPT_RETURN_ADDRESS_OFFSET).asPointer();
            tm = targetMethodForReturnAddress(originalReturnAddress);
            ip = originalReturnAddress;
        }

        // distinguish between a native function and a target method
        int pos = 0;
        if (tm != null) {
            // target method
            pos = tm.posFor(CodePointer.from(ip));
            ip = Pointer.zero();
        } else {
            // native function
        }

        current.advance(tm, pos, ip, sp.asPointer(), fp.asPointer());
    }

    public abstract TargetMethod targetMethodFor(Pointer instructionPointer);

    public abstract Word readWord(Address address, int offset);
    public abstract byte readByte(Address address, int offset);
    public abstract int readInt(Address address, int offset);

    /**
     * Reads the value of a given VM thread local from the safepoint-enabled thread locals.
     *
     * @param tl the VM thread local to read
     * @return the value (as a pointer) of {@code local} in the safepoint-enabled thread locals
     */
    public abstract Pointer readPointer(VmThreadLocal tl);
}
