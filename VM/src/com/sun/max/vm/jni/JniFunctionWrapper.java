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
/*VCSID=eef87071-f163-441d-abbb-19bd7d82d8ae*/
package com.sun.max.vm.jni;

import static com.sun.max.vm.compiler.builtin.MakeStackVariable.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;
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
        return _savedLastJavaCallerStackPointer;
    }

    @INLINE
    public static StackVariable savedLastJavaCallerFramePointer() {
        return _savedLastJavaCallerFramePointer;
    }

    @INLINE
    public static StackVariable savedLastJavaCallerInstructionPointer() {
        return _savedLastJavaCallerInstructionPointer;
    }

    // These fields cannot be final as they are updated by the inspector via reflection
    @CONSTANT_WHEN_NOT_ZERO
    private static StackVariable _savedLastJavaCallerStackPointer = StackVariable.create("LastJavaCallerStackPointer");
    @CONSTANT_WHEN_NOT_ZERO
    private static StackVariable _savedLastJavaCallerFramePointer = StackVariable.create("LastJavaCallerFramePointer");
    @CONSTANT_WHEN_NOT_ZERO
    private static StackVariable _savedLastJavaCallerInstructionPointer = StackVariable.create("LastJavaCallerInstructionPointer");

    private JniFunctionWrapper() {
    }

    @INLINE
    public static void exitThreadInNative(Pointer vmThreadLocals) {
        LAST_JAVA_CALLER_INSTRUCTION_POINTER.setVariableWord(vmThreadLocals, Word.zero());
    }

    @INLINE
    public static void reenterThreadInNative(Pointer vmThreadLocals, Word ip) {
        LAST_JAVA_CALLER_INSTRUCTION_POINTER.setVariableWord(vmThreadLocals, ip);
    }

    @WRAPPER
    public static void void_wrapper(Pointer env) {
        // start-prologue
        final Pointer vmThreadLocals = fromJniEnv(env);
        Safepoint.setLatchRegister(vmThreadLocals);
        Safepoint.hard(); // must happen before call to exitThreadInNative()
        final Word sp = LAST_JAVA_CALLER_STACK_POINTER.getVariableWord(vmThreadLocals);
        final Word fp = LAST_JAVA_CALLER_FRAME_POINTER.getVariableWord(vmThreadLocals);
        final Word ip = LAST_JAVA_CALLER_INSTRUCTION_POINTER.getVariableWord(vmThreadLocals);
        makeStackVariable(sp, _savedLastJavaCallerStackPointer);
        makeStackVariable(fp, _savedLastJavaCallerFramePointer);
        makeStackVariable(ip, _savedLastJavaCallerInstructionPointer);
        exitThreadInNative(vmThreadLocals);
        // end-prologue

        try {
            void_wrapper(env);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
        }

        // start-epilogue
        MemoryBarrier.storeStore(); // must happen after call to exitThreadInNative()
        LAST_JAVA_CALLER_STACK_POINTER.setVariableWord(vmThreadLocals, sp);
        LAST_JAVA_CALLER_FRAME_POINTER.setVariableWord(vmThreadLocals, fp);
        MemoryBarrier.storeStore(); // The following assignment must be last:
        // Re-enters the 'thread in native' state of the thread
        reenterThreadInNative(vmThreadLocals, ip);
        // end-epilogue
    }

    @WRAPPER
    public static int int_wrapper(Pointer env) {
        // start-prologue
        final Pointer vmThreadLocals = fromJniEnv(env);
        Safepoint.setLatchRegister(vmThreadLocals);
        Safepoint.hard(); // must happen before call to exitThreadInNative()
        final Word sp = LAST_JAVA_CALLER_STACK_POINTER.getVariableWord(vmThreadLocals);
        final Word fp = LAST_JAVA_CALLER_FRAME_POINTER.getVariableWord(vmThreadLocals);
        final Word ip = LAST_JAVA_CALLER_INSTRUCTION_POINTER.getVariableWord(vmThreadLocals);
        makeStackVariable(sp, _savedLastJavaCallerStackPointer);
        makeStackVariable(fp, _savedLastJavaCallerFramePointer);
        makeStackVariable(ip, _savedLastJavaCallerInstructionPointer);
        exitThreadInNative(vmThreadLocals);
        // end-prologue

        int result;
        try {
            result = int_wrapper(env);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JniFunctions.JNI_ERR;
        }

        // start-epilogue
        MemoryBarrier.storeStore();
        LAST_JAVA_CALLER_STACK_POINTER.setVariableWord(vmThreadLocals, sp);
        LAST_JAVA_CALLER_FRAME_POINTER.setVariableWord(vmThreadLocals, fp);
        MemoryBarrier.storeStore();
        reenterThreadInNative(vmThreadLocals, ip);
        // end-epilogue

        return result;
    }

    @WRAPPER
    public static float float_wrapper(Pointer env) {
        // start-prologue
        final Pointer vmThreadLocals = fromJniEnv(env);
        Safepoint.setLatchRegister(vmThreadLocals);
        Safepoint.hard(); // must happen before call to exitThreadInNative()
        final Word sp = LAST_JAVA_CALLER_STACK_POINTER.getVariableWord(vmThreadLocals);
        final Word fp = LAST_JAVA_CALLER_FRAME_POINTER.getVariableWord(vmThreadLocals);
        final Word ip = LAST_JAVA_CALLER_INSTRUCTION_POINTER.getVariableWord(vmThreadLocals);
        makeStackVariable(sp, _savedLastJavaCallerStackPointer);
        makeStackVariable(fp, _savedLastJavaCallerFramePointer);
        makeStackVariable(ip, _savedLastJavaCallerInstructionPointer);
        exitThreadInNative(vmThreadLocals);
        // end-prologue

        float result;
        try {
            result = float_wrapper(env);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JniFunctions.JNI_ERR;
        }

        // start-epilogue
        MemoryBarrier.storeStore();
        LAST_JAVA_CALLER_STACK_POINTER.setVariableWord(vmThreadLocals, sp);
        LAST_JAVA_CALLER_FRAME_POINTER.setVariableWord(vmThreadLocals, fp);
        MemoryBarrier.storeStore();
        reenterThreadInNative(vmThreadLocals, ip);
        // end-epilogue

        return result;
    }

    @WRAPPER
    public static long long_wrapper(Pointer env) {
        // start-prologue
        final Pointer vmThreadLocals = fromJniEnv(env);
        Safepoint.setLatchRegister(vmThreadLocals);
        Safepoint.hard(); // must happen before call to exitThreadInNative()
        final Word sp = LAST_JAVA_CALLER_STACK_POINTER.getVariableWord(vmThreadLocals);
        final Word fp = LAST_JAVA_CALLER_FRAME_POINTER.getVariableWord(vmThreadLocals);
        final Word ip = LAST_JAVA_CALLER_INSTRUCTION_POINTER.getVariableWord(vmThreadLocals);
        makeStackVariable(sp, _savedLastJavaCallerStackPointer);
        makeStackVariable(fp, _savedLastJavaCallerFramePointer);
        makeStackVariable(ip, _savedLastJavaCallerInstructionPointer);
        exitThreadInNative(vmThreadLocals);
        // end-prologue

        long result;
        try {
            result = long_wrapper(env);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JniFunctions.JNI_ERR;
        }

        // start-epilogue
        MemoryBarrier.storeStore();
        LAST_JAVA_CALLER_STACK_POINTER.setVariableWord(vmThreadLocals, sp);
        LAST_JAVA_CALLER_FRAME_POINTER.setVariableWord(vmThreadLocals, fp);
        MemoryBarrier.storeStore();
        reenterThreadInNative(vmThreadLocals, ip);
        // end-epilogue

        return result;
    }

    @WRAPPER
    public static double double_wrapper(Pointer env) {
        // start-prologue
        final Pointer vmThreadLocals = fromJniEnv(env);
        Safepoint.setLatchRegister(vmThreadLocals);
        Safepoint.hard(); // must happen before call to exitThreadInNative()
        final Word sp = LAST_JAVA_CALLER_STACK_POINTER.getVariableWord(vmThreadLocals);
        final Word fp = LAST_JAVA_CALLER_FRAME_POINTER.getVariableWord(vmThreadLocals);
        final Word ip = LAST_JAVA_CALLER_INSTRUCTION_POINTER.getVariableWord(vmThreadLocals);
        makeStackVariable(sp, _savedLastJavaCallerStackPointer);
        makeStackVariable(fp, _savedLastJavaCallerFramePointer);
        makeStackVariable(ip, _savedLastJavaCallerInstructionPointer);
        exitThreadInNative(vmThreadLocals);
        // end-prologue

        double result;
        try {
            result = double_wrapper(env);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = JniFunctions.JNI_ERR;
        }

        // start-epilogue
        MemoryBarrier.storeStore();
        LAST_JAVA_CALLER_STACK_POINTER.setVariableWord(vmThreadLocals, sp);
        LAST_JAVA_CALLER_FRAME_POINTER.setVariableWord(vmThreadLocals, fp);
        MemoryBarrier.storeStore();
        reenterThreadInNative(vmThreadLocals, ip);
        // end-epilogue

        return result;
    }

    @WRAPPER
    public static Word word_wrapper(Pointer env) {
        // start-prologue
        final Pointer vmThreadLocals = fromJniEnv(env);
        Safepoint.setLatchRegister(vmThreadLocals);
        Safepoint.hard(); // must happen before call to exitThreadInNative()
        final Word sp = LAST_JAVA_CALLER_STACK_POINTER.getVariableWord(vmThreadLocals);
        final Word fp = LAST_JAVA_CALLER_FRAME_POINTER.getVariableWord(vmThreadLocals);
        final Word ip = LAST_JAVA_CALLER_INSTRUCTION_POINTER.getVariableWord(vmThreadLocals);
        makeStackVariable(sp, _savedLastJavaCallerStackPointer);
        makeStackVariable(fp, _savedLastJavaCallerFramePointer);
        makeStackVariable(ip, _savedLastJavaCallerInstructionPointer);
        exitThreadInNative(vmThreadLocals);
        // end-prologue

        Word result;
        try {
            result = word_wrapper(env);
        } catch (Throwable t) {
            VmThread.fromJniEnv(env).setPendingException(t);
            result = Word.zero();
        }

        // start-epilogue
        MemoryBarrier.storeStore();
        LAST_JAVA_CALLER_STACK_POINTER.setVariableWord(vmThreadLocals, sp);
        LAST_JAVA_CALLER_FRAME_POINTER.setVariableWord(vmThreadLocals, fp);
        MemoryBarrier.storeStore();
        reenterThreadInNative(vmThreadLocals, ip);
        // end-epilogue

        return result;
    }
}
