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

import static com.sun.max.vm.compiler.target.TargetMethod.Flavor.*;
import static com.sun.max.vm.stack.StackFrameWalker.Purpose.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.snippet.NativeStubSnippet.NativeCallPrologue;
import com.sun.max.vm.compiler.snippet.NativeStubSnippet.NativeCallPrologueForC;
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

    public enum CalleeKind {
        NONE,
        NATIVE,
        JAVA,
        TRAP_STUB,
        TRAMPOLINE,
        CALLEE_SAVED
    }
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
        boolean isTopFrame = false;
        boolean ipIsReturnAddress;

        private Cursor() {
        }

        /**
         * Updates the cursor to point to the next stack frame.
         * This method implicitly sets {@link Cursor#isTopFrame()} of this cursor to {@code false} and
         * {@link Cursor#targetMethod()} of this cursor to {@code null}.
         * @param ip the new instruction pointer
         * @param sp the new stack pointer
         * @param fp the new frame pointer
         * @param ipIsReturnAddress
         */
        public Cursor advance(Pointer ip, Pointer sp, Pointer fp, boolean ipIsReturnAddress) {
            return setFields(null, ip, sp, fp, false, ipIsReturnAddress);
        }

        void reset() {
            setFields(null, Pointer.zero(), Pointer.zero(), Pointer.zero(), false, false);
        }

        private void copyFrom(Cursor other) {
            setFields(other.targetMethod, other.ip, other.sp, other.fp, other.isTopFrame, other.ipIsReturnAddress);
        }

        private Cursor setFields(TargetMethod targetMethod, Pointer ip, Pointer sp, Pointer fp, boolean isTopFrame, boolean ipIsReturnAddress) {
            this.targetMethod = targetMethod;
            this.ip = ip;
            this.sp = sp;
            this.fp = fp;
            this.isTopFrame = isTopFrame;
            this.ipIsReturnAddress = ipIsReturnAddress;
            return this;
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
         * Determines if {@link #ip()} denotes an instruction address that may
         * not be precisely correlated with a position for which debug info is available.
         * This is the case if the address is the return address read from a callee
         * frame except for the case where the callee is the {@linkplain Stubs#trapStub() trap stub}
         * or a native/C function. In the case of the former, the address is that
         * of the instruction causing the trap. In the case of the latter,
         * the address is that of the call to the native/C function.
         *
         * @return {@code true} if {@link #ip()} is the return address derived from
         * a {@linkplain Stubs#trapStub() non-trap-stub}, non-native-function callee
         */
        public boolean ipIsReturnAddress() {
            return ipIsReturnAddress;
        }

        /**
         * Get the callee kind of this method, which determines if there is register state in this frame
         * and where it is stored.
         * @return the callee kind of this method
         */
        public CalleeKind calleeKind() {
            if (targetMethod == null) {
                if (sp.isZero()) {
                    assert callee == this;
                    assert current.isTopFrame;
                    return CalleeKind.NONE;
                }
                return CalleeKind.NATIVE;
            }
            if (targetMethod.is(TrapStub)) {
                return CalleeKind.TRAP_STUB;
            }
            if (targetMethod.isTrampoline()) {
                return CalleeKind.TRAMPOLINE;
            }
            if (targetMethod.isCalleeSaved()) {
                return CalleeKind.CALLEE_SAVED;
            }
            return CalleeKind.JAVA;
        }
    }

    protected StackFrameWalker() {
    }

    /**
     * Constants denoting the finite set of reasons for which a stack walk can be performed.
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
                            // This can only occur in the inspector and implies that execution is in the platform specific
                            // prologue of Trap.trapStub() before the point where the trap frame has been completed. In
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
            advance(getNativeFunctionCallInstructionPointerInNativeStub(lastJavaCallerInstructionPointer.asPointer(), true),
                    lastJavaCallerStackPointer,
                    lastJavaCallerFramePointer, false);
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
        Pointer lastJavaCallerInstructionPointer = readWord(anchor, JavaFrameAnchor.PC.offset).asPointer();
        if (lastJavaCallerInstructionPointer.isZero()) {
            FatalError.check(!lastJavaCallerInstructionPointer.isZero(), "Thread cannot be 'in native' without having recorded the last Java caller in thread locals");
        }
        Pointer ip = getNativeFunctionCallInstructionPointerInNativeStub(lastJavaCallerInstructionPointer, purpose != INSPECTING && purpose != RAW_INSPECTING);
        if (ip.isZero()) {
            ip = lastJavaCallerInstructionPointer;
        }
        advance(ip, readWord(anchor, JavaFrameAnchor.SP.offset), readWord(anchor, JavaFrameAnchor.FP.offset), false);
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
     * Gets the address corresponding to a native function call in a {@linkplain NativeStubGenerator native stub}.
     *
     * @param ip the instruction pointer saved by {@link NativeCallPrologue} or {@link NativeCallPrologueForC}
     * @param fatalIfNotFound specifies whether a fatal error should be raised if a native call cannot be found
     * @return the address of the native function call derived from {@code ip} or zero if no such call exists
     */
    private Pointer getNativeFunctionCallInstructionPointerInNativeStub(Pointer ip, boolean fatalIfNotFound) {
        final TargetMethod nativeStubTargetMethod = targetMethodFor(ip);
        if (nativeStubTargetMethod != null) {
            final int pos = nativeStubTargetMethod.posFor(ip);
            final int nativeFunctionCallPos = nativeStubTargetMethod.findNextCall(pos, true);
            final Pointer nativeFunctionCall = nativeFunctionCallPos < 0 ? Pointer.zero() : nativeStubTargetMethod.codeStart().plus(nativeFunctionCallPos);
            if (!nativeFunctionCall.isZero()) {
                // The returned instruction pointer must be one past the actual address of the
                // native function call. This makes it match the pattern expected by the
                // StackReferenceMapPreparer where the instruction pointer in all but the
                // top frame is past the address of the call.
                if (TraceStackWalk && purpose == Purpose.REFERENCE_MAP_PREPARING) {
                    Log.print("IP for stack frame preparation of stub for native method ");
                    Log.print(nativeStubTargetMethod.name());
                    Log.print(" [");
                    Log.print(nativeStubTargetMethod.codeStart());
                    Log.print("+");
                    Log.print(nativeFunctionCallPos + 1);
                    Log.println(']');
                }
                return nativeFunctionCall;
            }
        }
        if (fatalIfNotFound) {
            if (nativeStubTargetMethod == null) {
                Log.print("Could not find native stub for instruction pointer ");
                Log.println(ip);
            } else {
                Log.print("Could not find native function call after ");
                Log.print(nativeStubTargetMethod.codeStart());
                Log.print("+");
                Log.print(ip.minus(nativeStubTargetMethod.codeStart()).toLong());
                Log.print(" in ");
                Log.printMethod(nativeStubTargetMethod, true);
            }
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
        reset();
    }

    /**
     * Walks a thread's stack for the purpose of inspecting one or more frames on the stack. This method takes care of
     * {@linkplain #reset() resetting} this walker before returning.
     */
    public final void inspect(Pointer ip, Pointer sp, Pointer fp, final RawStackFrameVisitor visitor) {
        walk(ip, sp, fp, RAW_INSPECTING, visitor);
        calleeStackFrame = null;
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

    public final void advance(Word ip, Word sp, Word fp, boolean ipIsReturnAddress) {
        if (!(current.targetMethod instanceof Adapter)) {
            // Adapter frames are never of interest when visiting the frame of a caller
            callee.copyFrom(current);
        }
        current.advance(ip.asPointer(), sp.asPointer(), fp.asPointer(), ipIsReturnAddress);
    }

    /**
     * Collects a sequence of stack frames, beginning a stack walk at the specified instruction pointer, stack pointer,
     * and frame pointer. This method will return all stack frames, including native frames, adapter frames, and
     * non-application visible stack frames. This method accepts an appendable sequence of stack frames in
     * which to store the result.
     *
     * @param stackFrames an appendable sequence of stack frames to collect the results; if {@code null}, this method
     * will create a new appendable sequence for collecting the result
     * @param ip the instruction pointer from which to begin the stack walk
     * @param sp the stack pointer from which to begin the stack walk
     * @param fp the frame pointer from which to begin the stack walk
     * @return a sequence of all the stack frames, including native, adapter, and non-application visible stack frames,
     *         with the top frame as the first frame
     */
    public List<StackFrame> frames(List<StackFrame> stackFrames, Pointer ip, Pointer sp, Pointer fp) {
        final List<StackFrame> frames = stackFrames == null ? new LinkedList<StackFrame>() : stackFrames;
        final StackFrameVisitor visitor = new StackFrameVisitor() {
            public boolean visitFrame(StackFrame stackFrame) {
                frames.add(stackFrame);
                return true;
            }
        };
        inspect(ip, sp, fp, visitor);
        return frames;
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
