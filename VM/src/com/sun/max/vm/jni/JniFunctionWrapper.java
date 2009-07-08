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


import static com.sun.max.vm.compiler.builtin.MakeStackVariable.*;
import static com.sun.max.vm.compiler.snippet.NativeStubSnippet.NativeCallPrologue.*;
import static com.sun.max.vm.runtime.Safepoint.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.VMRegister.*;
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

    @INLINE
    public static StackVariable savedLastJavaCallerStackPointer() {
        return savedLastJavaCallerStackPointer;
    }

    @INLINE
    public static StackVariable savedLastJavaCallerFramePointer() {
        return savedLastJavaCallerFramePointer;
    }

    @INLINE
    public static StackVariable savedLastJavaCallerInstructionPointer() {
        return savedLastJavaCallerInstructionPointer;
    }

    // These fields cannot be final as they are updated by the inspector via reflection
    @CONSTANT_WHEN_NOT_ZERO
    private static StackVariable savedLastJavaCallerStackPointer = StackVariable.create("LastJavaCallerStackPointer");
    @CONSTANT_WHEN_NOT_ZERO
    private static StackVariable savedLastJavaCallerFramePointer = StackVariable.create("LastJavaCallerFramePointer");
    @CONSTANT_WHEN_NOT_ZERO
    private static StackVariable savedLastJavaCallerInstructionPointer = StackVariable.create("LastJavaCallerInstructionPointer");

    private JniFunctionWrapper() {
    }

    /**
     * This method implements part of the prologue for entering a JNI upcall from native code. Because
     * safepoints may already be triggered, this method cannot trap.
     *
     * @param vmThreadLocals
     */
    @INLINE
    public static void reenterJavaFromNative(Pointer vmThreadLocals) {
        // a JNI upcall is similar to a native method returning; reuse the native call epilogue sequence
        NativeCallEpilogue.nativeCallEpilogue(vmThreadLocals);
    }

    /**
     * This method implements the epilogue for leaving an JNI upcall. The steps performed are to
     * reset the thread-local information which stores the last Java caller SP, FP, and IP, and
     * print a trace if necessary.
     *
     * @param vmThreadLocals the pointer to the VMThreadLocals to update
     * @param sp the stack pointer to which the last java SP should be reset
     * @param fp the frame pointer to which the last java FP should be reset
     * @param ip the instruction pointer to which the last java IP should be reset
     * @param jniTargetMethod the method which was called (for tracing only)
     */
    @INLINE
    private static void jniWrapperEpilogue(final Pointer vmThreadLocals, final Word sp, final Word fp, final Word ip, final TargetMethod jniTargetMethod) {
        traceExit(jniTargetMethod);
        MemoryBarrier.storeStore();
        LAST_JAVA_CALLER_STACK_POINTER.setVariableWord(vmThreadLocals, sp);
        LAST_JAVA_CALLER_FRAME_POINTER.setVariableWord(vmThreadLocals, fp);
        MemoryBarrier.storeStore();
        LAST_JAVA_CALLER_INSTRUCTION_POINTER.setVariableWord(vmThreadLocals, ip);

        if (Safepoint.UseThreadStateWordForGCMutatorSynchronization) {
            final Pointer enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
            final Pointer statePointer = STATE.pointer(enabledVmThreadLocals);
            final int oldValue = Safepoint.cas(statePointer, THREAD_IN_JAVA, THREAD_IN_NATIVE);
            if (oldValue != THREAD_IN_JAVA) {
                Safepoint.reportIllegalThreadState("JNI function call epilogue", oldValue);
            }
        }
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
     * @return the target method for the JNI function denoted by {@code instructionPointer} or null if JNI tracing is
     *         not enabled
     */
    private static TargetMethod traceEntry(Pointer instructionPointer, Pointer stackPointer, Pointer framePointer) {
        if (ClassMethodActor.traceJNI()) {
            final TargetMethod jniTargetMethod = JniNativeInterface.jniTargetMethod(instructionPointer);
            Log.print("[Thread \"");
            Log.print(VmThread.current().getName());
            Log.print("\" --> JNI upcall: ");
            if (jniTargetMethod != null) {
                printUpCall(jniTargetMethod);

                final Pointer namedVariablesBasePointer = VMConfiguration.target().compilerScheme().namedVariablesBasePointer(stackPointer, framePointer);
                final Word nativeMethodIP = savedLastJavaCallerInstructionPointer().address(jniTargetMethod, namedVariablesBasePointer).asPointer().readWord(0);
                final TargetMethod nativeMethod = Code.codePointerToTargetMethod(nativeMethodIP.asAddress());
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
        final Word sp = LAST_JAVA_CALLER_STACK_POINTER.getVariableWord(vmThreadLocals);
        final Word fp = LAST_JAVA_CALLER_FRAME_POINTER.getVariableWord(vmThreadLocals);
        final Word ip = LAST_JAVA_CALLER_INSTRUCTION_POINTER.getVariableWord(vmThreadLocals);
        makeStackVariable(sp, savedLastJavaCallerStackPointer);
        makeStackVariable(fp, savedLastJavaCallerFramePointer);
        makeStackVariable(ip, savedLastJavaCallerInstructionPointer);
        reenterJavaFromNative(vmThreadLocals);
        // end-prologue

        final TargetMethod jniTargetMethod = traceEntry(ip(), sp(), fp());
        try {
            void_wrapper(env);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }
        jniWrapperEpilogue(vmThreadLocals, sp, fp, ip, jniTargetMethod);
    }

    @WRAPPER
    public static int int_wrapper(Pointer env) {
        // start-prologue
        final Pointer vmThreadLocals = fromJniEnv(env);
        Safepoint.setLatchRegister(vmThreadLocals);
        final Word sp = LAST_JAVA_CALLER_STACK_POINTER.getVariableWord(vmThreadLocals);
        final Word fp = LAST_JAVA_CALLER_FRAME_POINTER.getVariableWord(vmThreadLocals);
        final Word ip = LAST_JAVA_CALLER_INSTRUCTION_POINTER.getVariableWord(vmThreadLocals);
        makeStackVariable(sp, savedLastJavaCallerStackPointer);
        makeStackVariable(fp, savedLastJavaCallerFramePointer);
        makeStackVariable(ip, savedLastJavaCallerInstructionPointer);
        reenterJavaFromNative(vmThreadLocals);
        // end-prologue

        final TargetMethod jniTargetMethod = traceEntry(ip(), sp(), fp());
        int result;
        try {
            result = int_wrapper(env);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JniFunctions.JNI_ERR;
        }
        jniWrapperEpilogue(vmThreadLocals, sp, fp, ip, jniTargetMethod);

        return result;
    }

    @WRAPPER
    public static float float_wrapper(Pointer env) {
        // start-prologue
        final Pointer vmThreadLocals = fromJniEnv(env);
        Safepoint.setLatchRegister(vmThreadLocals);
        final Word sp = LAST_JAVA_CALLER_STACK_POINTER.getVariableWord(vmThreadLocals);
        final Word fp = LAST_JAVA_CALLER_FRAME_POINTER.getVariableWord(vmThreadLocals);
        final Word ip = LAST_JAVA_CALLER_INSTRUCTION_POINTER.getVariableWord(vmThreadLocals);
        makeStackVariable(sp, savedLastJavaCallerStackPointer);
        makeStackVariable(fp, savedLastJavaCallerFramePointer);
        makeStackVariable(ip, savedLastJavaCallerInstructionPointer);
        reenterJavaFromNative(vmThreadLocals);
        // end-prologue

        final TargetMethod jniTargetMethod = traceEntry(ip(), sp(), fp());
        float result;
        try {
            result = float_wrapper(env);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JniFunctions.JNI_ERR;
        }
        jniWrapperEpilogue(vmThreadLocals, sp, fp, ip, jniTargetMethod);

        return result;
    }

    @WRAPPER
    public static long long_wrapper(Pointer env) {
        // start-prologue
        final Pointer vmThreadLocals = fromJniEnv(env);
        Safepoint.setLatchRegister(vmThreadLocals);
        final Word sp = LAST_JAVA_CALLER_STACK_POINTER.getVariableWord(vmThreadLocals);
        final Word fp = LAST_JAVA_CALLER_FRAME_POINTER.getVariableWord(vmThreadLocals);
        final Word ip = LAST_JAVA_CALLER_INSTRUCTION_POINTER.getVariableWord(vmThreadLocals);
        makeStackVariable(sp, savedLastJavaCallerStackPointer);
        makeStackVariable(fp, savedLastJavaCallerFramePointer);
        makeStackVariable(ip, savedLastJavaCallerInstructionPointer);
        reenterJavaFromNative(vmThreadLocals);
        // end-prologue

        final TargetMethod jniTargetMethod = traceEntry(ip(), sp(), fp());
        long result;
        try {
            result = long_wrapper(env);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JniFunctions.JNI_ERR;
        }
        jniWrapperEpilogue(vmThreadLocals, sp, fp, ip, jniTargetMethod);

        return result;
    }

    @WRAPPER
    public static double double_wrapper(Pointer env) {
        // start-prologue
        final Pointer vmThreadLocals = fromJniEnv(env);
        Safepoint.setLatchRegister(vmThreadLocals);
        final Word sp = LAST_JAVA_CALLER_STACK_POINTER.getVariableWord(vmThreadLocals);
        final Word fp = LAST_JAVA_CALLER_FRAME_POINTER.getVariableWord(vmThreadLocals);
        final Word ip = LAST_JAVA_CALLER_INSTRUCTION_POINTER.getVariableWord(vmThreadLocals);
        makeStackVariable(sp, savedLastJavaCallerStackPointer);
        makeStackVariable(fp, savedLastJavaCallerFramePointer);
        makeStackVariable(ip, savedLastJavaCallerInstructionPointer);
        reenterJavaFromNative(vmThreadLocals);
        // end-prologue

        final TargetMethod jniTargetMethod = traceEntry(ip(), sp(), fp());
        double result;
        try {
            result = double_wrapper(env);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JniFunctions.JNI_ERR;
        }

        // finally exit back to the native method that called this upcall
        jniWrapperEpilogue(vmThreadLocals, sp, fp, ip, jniTargetMethod);
        return result;
    }

    @WRAPPER
    public static Word word_wrapper(Pointer env) {
        // start-prologue
        final Pointer vmThreadLocals = fromJniEnv(env);
        Safepoint.setLatchRegister(vmThreadLocals);
        final Word sp = LAST_JAVA_CALLER_STACK_POINTER.getVariableWord(vmThreadLocals);
        final Word fp = LAST_JAVA_CALLER_FRAME_POINTER.getVariableWord(vmThreadLocals);
        final Word ip = LAST_JAVA_CALLER_INSTRUCTION_POINTER.getVariableWord(vmThreadLocals);
        makeStackVariable(sp, savedLastJavaCallerStackPointer);
        makeStackVariable(fp, savedLastJavaCallerFramePointer);
        makeStackVariable(ip, savedLastJavaCallerInstructionPointer);
        reenterJavaFromNative(vmThreadLocals);
        // end-prologue

        final TargetMethod jniTargetMethod = traceEntry(ip(), sp(), fp());
        Word result;
        try {
            result = word_wrapper(env);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = Word.zero();
        }
        jniWrapperEpilogue(vmThreadLocals, sp, fp, ip, jniTargetMethod);

        return result;
    }
}
