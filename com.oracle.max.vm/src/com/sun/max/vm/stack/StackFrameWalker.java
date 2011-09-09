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

import static com.sun.max.vm.compiler.target.Stub.Type.*;
import static com.sun.max.vm.stack.StackFrameWalker.Purpose.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * The mechanism for iterating over the frames in a thread's stack.
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
     * The cursor class encapsulates all the state associated with a single stack frame,
     * including the target method, the instruction pointer, stack pointer, frame pointer, register state,
     * and whether the frame is the top frame. The stack frame walker manages two cursors internally: the current
     * frame (caller frame) and the last frame (callee frame) and destructively updates their contents
     * when walking the stack, rather than allocating new ones, since allocation is disallowed when
     * walking the stack for reference map preparation.
     */
    public final class Cursor {

        TargetMethod targetMethod;
        Pointer ip = Pointer.zero();
        Pointer sp = Pointer.zero();
        Pointer fp = Pointer.zero();
        CiCalleeSaveLayout csl;
        Pointer csa;
        boolean isTopFrame = false;

        private Cursor() {
        }

        /**
         * Updates the cursor to point to the next stack frame.
         * This method implicitly sets {@link Cursor#isTopFrame()} of this cursor to {@code false} and
         * {@link Cursor#targetMethod()} of this cursor to {@code null}.
         * @param ip the new instruction pointer
         * @param sp the new stack pointer
         * @param fp the new frame pointer
         */
        Cursor advance(Pointer ip, Pointer sp, Pointer fp) {
            return setFields(null, ip, sp, fp, false);
        }

        void reset() {
            setFields(null, Pointer.zero(), Pointer.zero(), Pointer.zero(), false);
        }

        private void copyFrom(Cursor other) {
            setFields(other.targetMethod, other.ip, other.sp, other.fp, other.isTopFrame);
            setCalleeSaveArea(other.csl, other.csa);
        }

        private Cursor setFields(TargetMethod targetMethod, Pointer ip, Pointer sp, Pointer fp, boolean isTopFrame) {
            this.targetMethod = targetMethod;
            this.ip = ip;
            this.sp = sp;
            this.fp = fp;
            this.isTopFrame = isTopFrame;
            this.csl = null;
            this.csa = Pointer.zero();
            return this;
        }

        /**
         * Sets the callee save details for this frame cursor. This must be called
         * while this cursor denotes the "current" frame just before {@link StackFrameWalker#advance(Word, Word, Word)
         * is called (after which this cursor will be the "callee" frame).
         *
         * @param csl the layout of the callee save area in the frame denoted by this cursor
         * @param csa the address of the callee save area in the frame denoted by this cursor
         */
        public void setCalleeSaveArea(CiCalleeSaveLayout csl, Pointer csa) {
            FatalError.check((csl == null) == csa.isZero(), "inconsistent callee save area info");
            this.csl = csl;
            this.csa = csa;
        }

        /**
         * @return the stack frame walker for this cursor
         */
        public StackFrameWalker stackFrameWalker() {
            return StackFrameWalker.this;
        }

        /**
         * @return the target method corresponding to the instruction pointer.
         */
        public TargetMethod targetMethod() {
            return targetMethod;
        }

        /**
         * Gets the address of the next instruction that will be executed in this frame.
         * If this is not the {@linkplain #isTopFrame() top frame}, then this is the
         * return address saved by a call instruction. The exact interpretation of this
         * return address depends on the {@linkplain ISA#offsetToReturnPC platform}.
         *
         * @return the current instruction pointer.
         */
        public Pointer ip() {
            return ip;
        }

        /**
         * @return the current stack pointer.
         */
        public Pointer sp() {
            return sp;
        }

        /**
         * @return the current frame pointer.
         */
        public Pointer fp() {
            return fp;
        }

        /**
         * @return {@code true} if this frame is the top frame
         */
        public boolean isTopFrame() {
            return isTopFrame;
        }

        /**
         * Gets the layout of the callee save area in this frame.
         *
         * @return {@code null} if there is no callee save area in this frame
         */
        public CiCalleeSaveLayout csl() {
            return csl;
        }

        /**
         * Gets the address of the callee save area in this frame.
         *
         * @return {@link Pointer#zero()} if there is no callee save area in this frame
         */
        public Pointer csa() {
            return csa;
        }
    }

    protected StackFrameWalker() {
    }

    /**
     * Constants denoting the finite set of reasons for which a stack walk can be performed.
     *
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

    private final Cursor current = new Cursor();
    private final Cursor callee = new Cursor();

    private Purpose purpose = null;
    private Pointer currentAnchor;
    private StackFrame calleeStackFrame;

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

        current.ip = ip;
        current.sp = sp;
        current.fp = fp;
        current.isTopFrame = true;

        this.purpose = purpose;
        this.currentAnchor = readPointer(LAST_JAVA_FRAME_ANCHOR);
        boolean isTopFrame = true;
        boolean inNative = !currentAnchor.isZero() && !readWord(currentAnchor, JavaFrameAnchor.PC.offset).isZero();

        while (!current.sp.isZero()) {
            Pointer adjustedIP;
            if (!isTopFrame && Platform.platform().isa.offsetToReturnPC == 0) {
                // Adjust the current IP to ensure it is within the call instruction of the current frame.
                // This ensures we will always get the correct method, even if the call instruction was the
                // last instruction in a method and calls a method never expected to return (such as a call
                // emitted by the compiler to implement a 'throw' statement at the end of a method).
                adjustedIP = current.ip().minus(1);
            } else {
                adjustedIP = current.ip();
            }


            TargetMethod targetMethod = targetMethodFor(adjustedIP);
            TargetMethod calleeMethod = callee.targetMethod;
            current.targetMethod = targetMethod;
            traceCursor(current);

            if (targetMethod != null && (!inNative || (purpose == INSPECTING || purpose == RAW_INSPECTING))) {

                // found target method
                inNative = false;

                checkVmEntrypointCaller(calleeMethod, targetMethod);

                // walk the frame
                if (!walkFrame(current, callee, targetMethod, purpose, context)) {
                    break;
                }
            } else {
                // did not find target method => in native code
                if (purpose == INSPECTING) {
                    final StackFrameVisitor stackFrameVisitor = (StackFrameVisitor) context;
                    if (!stackFrameVisitor.visitFrame(new NativeStackFrame(calleeStackFrame, current.ip, current.fp, current.sp))) {
                        break;
                    }
                } else if (purpose == RAW_INSPECTING) {
                    final RawStackFrameVisitor stackFrameVisitor = (RawStackFrameVisitor) context;
                    current.targetMethod = null;
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
                    if (calleeMethod == null) {
                        // This is a native function that called a VM entry point such as VmThread.run(),
                        // MaxineVM.run() or a JNI function.
                        break;
                    }

                    ClassMethodActor lastJavaCalleeMethodActor = calleeMethod.classMethodActor();
                    if (calleeMethod.is(TrapStub)) {
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
                        if (!advanceVmEntryPointFrame(calleeMethod)) {
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
                        FatalError.unexpected("Native code called/entered a Java method that is not a JNI function, a Java trap stub or a VM/thread entry point");
                    }
                }
            }
            isTopFrame = false;
        }
    }

    private boolean walkFrame(Cursor current, Cursor callee, TargetMethod targetMethod, Purpose purpose, Object context) {
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

    private void traceCursor(Cursor cursor) {
        if (TraceStackWalk) {
            if (cursor.targetMethod != null) {
                Log.print("StackFrameWalk: Frame for ");
                if (cursor.targetMethod.classMethodActor() == null) {
                    Log.print(cursor.targetMethod.regionName());
                } else {
                    Log.printMethod(cursor.targetMethod, false);
                }
                Log.print(", pc=");
                Log.print(cursor.ip);
                Log.print("[");
                Log.print(cursor.targetMethod.codeStart());
                Log.print("+");
                Log.print(cursor.ip.minus(cursor.targetMethod.codeStart()).toInt());
                Log.print("], isTopFrame=");
                Log.print(cursor.isTopFrame);
                Log.print(", sp=");
                Log.print(cursor.sp);
                Log.print(", fp=");
                Log.print(cursor.fp);
                Log.println("");
            } else {
                Log.print("StackFrameWalk: Frame for native function [IP=");
                Log.print(cursor.ip);
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
            advance(checkNativeFunctionCall(lastJavaCallerInstructionPointer.asPointer(), true),
                    lastJavaCallerStackPointer,
                    lastJavaCallerFramePointer);
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
        Pointer nativeFunctionCall = readWord(anchor, JavaFrameAnchor.PC.offset).asPointer();
        if (nativeFunctionCall.isZero()) {
            FatalError.unexpected("Thread cannot be 'in native' without having recorded the last Java caller in thread locals");
        }
        Pointer ip = checkNativeFunctionCall(nativeFunctionCall, purpose != INSPECTING && purpose != RAW_INSPECTING);
        if (ip.isZero()) {
            ip = nativeFunctionCall;
        }
        advance(ip, readWord(anchor, JavaFrameAnchor.SP.offset), readWord(anchor, JavaFrameAnchor.FP.offset));
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
    private Pointer checkNativeFunctionCall(Pointer pc, boolean fatalIfNotFound) {
        final TargetMethod nativeStub = targetMethodFor(pc);
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
        return Pointer.zero();
    }

    /**
     * Walks a thread's stack for the purpose of inspecting one or more frames on the stack. This method takes care of
     * {@linkplain #reset() resetting} this walker before returning.
     */
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
        calleeStackFrame = null;
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
    public final StackFrame calleeStackFrame() {
        return calleeStackFrame;
    }

    public abstract TargetMethod targetMethodFor(Pointer instructionPointer);

    /**
     * Determines if this stack walker is currently in use. This is useful for detecting if an exception is being thrown as part of exception handling.
     */
    public final boolean isInUse() {
        return !current.sp.isZero();
    }

    /**
     * Advances this stack walker such that {@link #current} becomes {@link #callee}.
     *
     * @param ip the instruction pointer of the new current frame
     * @param sp the stack pointer of the new current frame
     * @param fp the frame pointer of the new current frame
     */
    public final void advance(Word ip, Word sp, Word fp) {
        callee.copyFrom(current);
        current.advance(ip.asPointer(), sp.asPointer(), fp.asPointer());
    }

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
