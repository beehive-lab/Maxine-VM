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
package com.sun.max.vm.jni;


import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.NativeStubSnippet.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.VMRegister.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * This method holds the wrappers for all the {@linkplain JniFunctions JNI functions}.
 * <p>
 * Given the limitations of the {@linkplain WRAPPED wrapping mechanism}, a separate wrapper
 * method is required for each JNI function return kind even though the prologue and
 * epilogue of each wrapper is identical. This means that any modification to JNI wrapping
 * semantics must be made to each wrapper in this class.
 *
 * @author Doug Simon
 * @author Bernd Mathiske
 */
public final class JniFunctionWrapper {

    private JniFunctionWrapper() {
    }

    /**
     * This method implements part of the prologue for entering a JNI upcall from native code.
     *
     * @param enabledVmThreadLocals
     */
    @INLINE
    public static Pointer reenterJavaFromNative(Pointer enabledVmThreadLocals) {
        Word previousAnchor = LAST_JAVA_FRAME_ANCHOR.getVariableWord();
        if (previousAnchor.isZero()) {
            FatalError.unexpected("LAST_JAVA_FRAME_ANCHOR is zero");
        }
        Pointer anchor = JavaFrameAnchor.create(Word.zero(), Word.zero(), Word.zero(), previousAnchor);
        // a JNI upcall is similar to a native method returning; reuse the native call epilogue sequence
        NativeCallEpilogue.nativeCallEpilogue0(enabledVmThreadLocals, anchor);
        return anchor;
    }

    /**
     * This method implements the epilogue for leaving an JNI upcall. The steps performed are to
     * reset the thread-local information which stores the last Java caller SP, FP, and IP, and
     * print a trace if necessary.
     *
     * @param jniTargetMethod the method which was called (for tracing only)
     */
    @INLINE
    private static void jniWrapperEpilogue(Pointer enabledVmThreadLocals, Pointer anchor, TargetMethod jniTargetMethod) {
        traceExit(jniTargetMethod);

        // returning from a JNI upcall is similar to a entering a native method returning; reuse the native call prologue sequence
        NativeCallPrologue.nativeCallPrologue0(enabledVmThreadLocals, JavaFrameAnchor.PREVIOUS.get(anchor));
    }

    /**
     * Gets the current value of the instruction pointer at the call site.
     */
    @INLINE
    private static Pointer ip() {
        return SpecialBuiltin.getInstructionPointer();
    }

    /**
     * Gets the current value of the stack pointer at the call site.
     */
    @INLINE
    private static Pointer sp() {
        return SpecialBuiltin.getIntegerRegister(Role.CPU_STACK_POINTER).asPointer();
    }

    /**
     * Gets the current value of the frame pointer at the call site.
     */
    @INLINE
    private static Pointer fp() {
        return SpecialBuiltin.getIntegerRegister(Role.CPU_FRAME_POINTER).asPointer();
    }

    /**
     * Traces the entry to an upcall if the {@linkplain ClassMethodActor#traceJNI() JNI tracing flag} has been set.
     *
     * @param instructionPointer the instruction pointer denoting a code location anywhere in the JNI function being traced
     * @param stackPointer the stack pointer of the JNI function frame. The value is used to read variables saved to in
     *            the frame via the {@link MakeStackVariable} builtin.
     * @param framePointer the stack pointer of the JNI function frame. The value is used to read variables saved to in
     *            the frame via the {@link MakeStackVariable} builtin.
     * @param anchor TODO
     * @return the target method for the JNI function denoted by {@code instructionPointer} or null if JNI tracing is
     *         not enabled
     */
    private static TargetMethod traceEntry(Pointer instructionPointer, Pointer stackPointer, Pointer framePointer, Pointer anchor) {
        if (ClassMethodActor.traceJNI()) {
            final TargetMethod jniTargetMethod = JniNativeInterface.jniTargetMethod(instructionPointer);
            Log.print("[Thread \"");
            Log.print(VmThread.current().getName());
            Log.print("\" --> JNI upcall: ");
            if (jniTargetMethod != null) {
                printUpCall(jniTargetMethod);
                Pointer jniStubAnchor = JavaFrameAnchor.PREVIOUS.get(anchor);
                final Address jniStubPC = JavaFrameAnchor.PC.get(jniStubAnchor).asAddress();
                final TargetMethod nativeMethod = Code.codePointerToTargetMethod(jniStubPC);
                Log.print(", last down call: ");
                FatalError.check(nativeMethod != null, "Could not find Java down call when entering JNI upcall");
                printUpCall(nativeMethod);
            } else {
                FatalError.unexpected("Could not find TargetMethod for a JNI function");
            }
            Log.println("]");
            return jniTargetMethod;
        }
        return null;
    }

    private static void printUpCall(final TargetMethod jniTargetMethod) {
        boolean lockDisabledSafepoints = false;
        lockDisabledSafepoints = Log.lock();
        Log.print(jniTargetMethod.classMethodActor().name.string);
        Log.unlock(lockDisabledSafepoints);
    }

    /**
     * Traces the exit from an upcall if the {@linkplain ClassMethodActor#traceJNI() JNI tracing flag} has been set.
     *
     * @param jniTargetMethod the target method for the JNI function denoted by {@code instructionPointer}
     */
    private static void traceExit(TargetMethod jniTargetMethod) {
        if (ClassMethodActor.traceJNI()) {
            Log.print("[Thread \"");
            Log.print(VmThread.current().getName());
            Log.print("\" <-- JNI upcall: ");
            FatalError.check(jniTargetMethod != null, "Cannot trace method from unknown JNI function");
            printUpCall(jniTargetMethod);
            Log.println("]");
        }
    }

    @WRAPPER
    public static void void_wrapper(Pointer env) {
        // start-prologue
        final Pointer vmThreadLocals = fromJniEnv(env);
        Safepoint.setLatchRegister(vmThreadLocals);
        Pointer enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord().asPointer();
        Pointer anchor = reenterJavaFromNative(enabledVmThreadLocals);
        // end-prologue

        final TargetMethod jniTargetMethod = traceEntry(ip(), sp(), fp(), anchor);
        try {
            void_wrapper(env);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        jniWrapperEpilogue(enabledVmThreadLocals, anchor, jniTargetMethod);
    }

    @WRAPPER
    public static int int_wrapper(Pointer env) {
        // start-prologue
        final Pointer vmThreadLocals = fromJniEnv(env);
        Safepoint.setLatchRegister(vmThreadLocals);
        Pointer enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord().asPointer();
        Pointer anchor = reenterJavaFromNative(enabledVmThreadLocals);
        // end-prologue

        final TargetMethod jniTargetMethod = traceEntry(ip(), sp(), fp(), anchor);
        int result;
        try {
            result = int_wrapper(env);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JniFunctions.JNI_ERR;
        }
        jniWrapperEpilogue(enabledVmThreadLocals, anchor, jniTargetMethod);

        return result;
    }

    @WRAPPER
    public static float float_wrapper(Pointer env) {
        // start-prologue
        final Pointer vmThreadLocals = fromJniEnv(env);
        Safepoint.setLatchRegister(vmThreadLocals);
        Pointer enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord().asPointer();
        Pointer anchor = reenterJavaFromNative(enabledVmThreadLocals);
        // end-prologue

        final TargetMethod jniTargetMethod = traceEntry(ip(), sp(), fp(), anchor);
        float result;
        try {
            result = float_wrapper(env);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JniFunctions.JNI_ERR;
        }
        jniWrapperEpilogue(enabledVmThreadLocals, anchor, jniTargetMethod);

        return result;
    }

    @WRAPPER
    public static long long_wrapper(Pointer env) {
        // start-prologue
        final Pointer vmThreadLocals = fromJniEnv(env);
        Safepoint.setLatchRegister(vmThreadLocals);
        Pointer enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord().asPointer();
        Pointer anchor = reenterJavaFromNative(enabledVmThreadLocals);
        // end-prologue

        final TargetMethod jniTargetMethod = traceEntry(ip(), sp(), fp(), anchor);
        long result;
        try {
            result = long_wrapper(env);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JniFunctions.JNI_ERR;
        }
        jniWrapperEpilogue(enabledVmThreadLocals, anchor, jniTargetMethod);

        return result;
    }

    @WRAPPER
    public static double double_wrapper(Pointer env) {
        // start-prologue
        final Pointer vmThreadLocals = fromJniEnv(env);
        Safepoint.setLatchRegister(vmThreadLocals);
        Pointer enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord().asPointer();
        Pointer anchor = reenterJavaFromNative(enabledVmThreadLocals);
        // end-prologue

        final TargetMethod jniTargetMethod = traceEntry(ip(), sp(), fp(), anchor);
        double result;
        try {
            result = double_wrapper(env);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JniFunctions.JNI_ERR;
        }

        // finally exit back to the native method that called this upcall
        jniWrapperEpilogue(enabledVmThreadLocals, anchor, jniTargetMethod);
        return result;
    }

    @WRAPPER
    public static Word word_wrapper(Pointer env) {
        // start-prologue
        final Pointer vmThreadLocals = fromJniEnv(env);
        Safepoint.setLatchRegister(vmThreadLocals);
        Pointer enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord().asPointer();
        Pointer anchor = reenterJavaFromNative(enabledVmThreadLocals);
        // end-prologue

        final TargetMethod jniTargetMethod = traceEntry(ip(), sp(), fp(), anchor);
        Word result;
        try {
            result = word_wrapper(env);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = Word.zero();
        }
        jniWrapperEpilogue(enabledVmThreadLocals, anchor, jniTargetMethod);
        return result;
    }
}
