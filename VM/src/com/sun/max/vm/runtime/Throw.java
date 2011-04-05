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
package com.sun.max.vm.runtime;

import static com.sun.cri.bytecode.Bytecodes.Infopoints.*;
import static com.sun.max.vm.object.ArrayAccess.*;
import static com.sun.max.vm.runtime.VMRegister.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.Cursor;
import com.sun.max.vm.thread.*;

/**
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class Throw {

    private Throw() {
    }

    public static int TraceExceptions;
    private static boolean TraceExceptionsRaw;
    private static int TraceExceptionsMaxFrames = 200;
    private static int TraceExceptionsRawMaxFrames = 200;
    private static String TraceExceptionsFilter;
    public static boolean ScanStackOnFatalError;
    static {
        VMOptions.addFieldOption("-XX:", "TraceExceptions", Throw.class,
            "Trace exception throwing: 0 = none, 1 = toString(), 2 = printStackTrace().", Phase.STARTING);
        VMOptions.addFieldOption("-XX:", "TraceExceptionsMaxFrames", Throw.class,
            "The max frames to dump for -XX:TraceExceptions=2.", Phase.STARTING);
        VMOptions.addFieldOption("-XX:", "TraceExceptionsRaw", Throw.class,
            "Report a stack frame dump for every exception thrown.", Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "TraceExceptionsRawMaxFrames", Throw.class,
            "The max frames to dump for -XX:+TraceExceptionsRaw.", Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "ScanStackOnFatalError", Throw.class,
            "Perform a raw stack scan when a fatal VM occurs.", Phase.PRISTINE);
    }

    static class StackFrameDumper extends RawStackFrameVisitor {
        int frames;
        @Override
        public boolean visitFrame(Cursor current, Cursor callee) {
            if (frames++ < TraceExceptionsRawMaxFrames) {
                // N.B. use "->" to make dumped stacks look slightly different than exception stack traces.
                Throw.logFrame("        -> ", current.targetMethod(), current.ip());
            }
            return true;
        }

        @Override
        public void done() {
            if (frames > TraceExceptionsRawMaxFrames) {
                Log.print("        [");
                Log.print(frames);
                Log.println(" frames elided]");
            }
            frames = 0;
        }
    }

    /**
     * Shared global object for dumping stack traces without incurring any allocation.
     */
    private static final StackFrameDumper stackFrameDumper = new StackFrameDumper();

    /**
     * Unwinds the current thread's stack to the frame containing an exception handler (there is guaranteed to be one).
     *
     * This method disables safepoints so that GC request (or any other VM operation) cannot interrupt
     * the unwinding process. Why? A {@linkplain VmThread#unwindingStackFrameWalker(Throwable) shared}
     * stack walker object is used for unwinding and stack reference map preparation.
     * Safepoints are re-enabled when the exception object is {@linkplain VmThread#loadExceptionForHandler() loaded}
     * by the exception handler.
     *
     * @param throwable the object to be passed to the exception handler - must not be null
     * @param stackPointer the stack pointer to be used when determining the point at which exception was raised
     * @param framePointer the frame pointer to be used when determining the point at which exception was raised
     * @param instructionPointer the instruction pointer to be used when determining the point at which exception was raised
     */
    public static void raise(Throwable throwable, Pointer stackPointer, Pointer framePointer, Pointer instructionPointer) {
        FatalError.check(throwable != null, "Trying to raise an exception with a null Throwable object");
        final VmStackFrameWalker sfw = VmThread.current().unwindingStackFrameWalker(throwable);

        VmThread.current().checkYellowZoneForRaisingException();
        Safepoint.disable();
        sfw.unwind(instructionPointer, stackPointer, framePointer, throwable);
        FatalError.unexpected("could not find top-level exception handler");
    }

    /**
     * Unwinds the current thread's stack to the frame containing an exception handler (there is guaranteed to be one).
     */
    public static void raise(Object throwable) {
        if (throwable == null || throwable instanceof Throwable) {
            raise((Throwable) throwable);
        } else {
            raise(FatalError.unexpected("Object thrown not a throwable: " + throwable));
        }
    }

    /**
     * Unwinds the current thread's stack to the frame containing an exception handler (there is guaranteed to be one).
     *
     * @param throwable throwable the object to be passed to the exception handler. If this value is null, then a
     *            {@link NullPointerException} is instantiated and raised instead.
     */
    // Checkstyle: stop (parameter assignment)
    public static void raise(Throwable throwable) {
        if (throwable == null) {
            throwable = new NullPointerException();
        }

        traceThrow(throwable);
        raise(throwable, getCpuStackPointer(), getCpuFramePointer(), Pointer.fromLong(here()));
    }

    public static void traceThrow(Throwable throwable) {
        if (TraceExceptions == 1) {
            Log.printThread(VmThread.current(), false);
            Log.println(": Throwing " + throwable);
        } else if (TraceExceptions == 2) {
            StackTraceElement[] trace = throwable.getStackTrace();
            boolean lockDisabledSafepoints = Log.lock();
            try {
                Log.printThread(VmThread.current(), false);
                Log.print(": Throwing ");
                Log.println(throwable);
                for (int i = 0; i < trace.length && i < TraceExceptionsMaxFrames; i++) {
                    Log.println("\tat " + trace[i]);
                }
                int elided = trace.length - TraceExceptionsMaxFrames;
                if (elided > 0) {
                    Log.print("\t[");
                    Log.print(elided);
                    Log.println(" frames elided]");
                }
            } finally {
                Log.unlock(lockDisabledSafepoints);
            }
        }
        if (TraceExceptionsRaw) {
            stackDumpWithException(throwable);
        }
    }

    public static void stackDumpWithException(Object throwable) {
        stackDump("Throwing " + throwable + ";", Pointer.fromLong(here()), getCpuStackPointer(), getCpuFramePointer());
    }

    /**
     * Dumps the physical frames of a given stack to the {@link Log} stream.
     *
     * @param message if not {@code null}, this message is printed on a separate line prior to the stack trace
     * @param instructionPointer the instruction pointer at which to begin the stack trace
     * @param cpuStackPointer the stack pointer at which to begin the stack trace
     * @param cpuFramePointer the frame pointer at which to begin the stack trace
     */
    @NEVER_INLINE
    public static void stackDump(String message, final Pointer instructionPointer, final Pointer cpuStackPointer, final Pointer cpuFramePointer) {
        if (message != null) {
            Log.println(message);
        }
        VmThread.current().stackDumpStackFrameWalker().inspect(instructionPointer, cpuStackPointer, cpuFramePointer, stackFrameDumper);
    }

    /**
     * Dumps the stack of the thread denoted by a given VM thread locals pointer.
     *
     * @param message if not {@code null}, this message is printed on a separate line prior to the stack trace
     * @param tla
     *
     * @see #stackDump(String, Pointer, Pointer, Pointer)
     */
    @NEVER_INLINE
    public static void stackDump(String message, final Pointer tla) {
        if (message != null) {
            Log.println(message);
        }

        Pointer anchor = JavaFrameAnchor.from(tla);
        final Pointer ip = anchor.isZero() ? Pointer.zero() : JavaFrameAnchor.PC.get(anchor);
        final VmThread vmThread = VmThread.fromTLA(tla);
        if (ip.isZero()) {
            Log.print("Cannot dump stack for non-stopped thread ");
            Log.printThread(vmThread, true);
        } else {
            final Pointer sp = JavaFrameAnchor.SP.get(anchor);
            final Pointer fp = JavaFrameAnchor.FP.get(anchor);
            stackDump(null, ip, sp, fp);
        }
    }

    /**
     * Dumps the stack of the current thread.
     *
     * @param message if not {@code null}, this message is printed on a separate line prior to the stack trace
     *
     * @see #stackDump(String, Pointer, Pointer, Pointer)
     */
    @NEVER_INLINE
    public static void stackDump(String message) {
        stackDump(message, Pointer.fromLong(here()), getCpuStackPointer(), getCpuFramePointer());
    }

    /**
     * Scans the stack between the specified addresses in search of potential code pointers.
     * <p>
     * This stack scanning is even more primitive than a stack dump, since it does not rely on
     * the stack frame walker to walk individual frames, but instead scans the memory sequentially,
     * inspecting each word in the memory range to see if it is a potential code pointer.
     *
     * @param message the message to print for this stack scan
     * @param stackPointer the pointer to top of the stack
     * @param endPointer the pointer to the end of the stack
     */
    public static void stackScan(String message, final Pointer stackPointer, final Pointer endPointer) {
        Log.println(message);
        Pointer pointer = stackPointer.wordAligned();
        while (pointer.lessThan(endPointer)) {
            final Address potentialCodePointer = pointer.getWord().asAddress();
            final TargetMethod targetMethod = Code.codePointerToTargetMethod(potentialCodePointer);
            if (targetMethod != null) {
                Log.print("        -> ");
                Log.printMethod(targetMethod, false);
                final Pointer codeStart = targetMethod.codeStart();
                Log.print(" [");
                Log.print(codeStart);
                Log.print("+");
                Log.print(potentialCodePointer.minus(codeStart).toInt());
                Log.println("]");

            }
            pointer = pointer.plus(Word.size());
        }
    }

    /**
     * Raises an {@code ArrayIndexOutOfBoundsException}. This is out-of-line to reduce the amount
     * of code inlined on the fast path for an array bounds check.
     *
     * @param array the array being accessed
     * @param index the index that is out of the bounds of {@code array}
     */
    @NEVER_INLINE
    public static void arrayIndexOutOfBoundsException(Object array, int index) {
        FatalError.check(array != null, "Arguments for raising an ArrayIndexOutOfBoundsException cannot be null");
        throw new ArrayIndexOutOfBoundsException("Index: " + index + ", Array length: " + readArrayLength(array));
    }

    /**
     * Raises an {@code ArrayStoreException}. This is out-of-line to reduce the amount
     * of code inlined on the fast path for an array store check.
     *
     * @param array the array being accessed
     * @param value the value whose type is not assignable to the component type of {@code array}
     */
    @NEVER_INLINE
    public static void arrayStoreException(Object array, Object value) {
        FatalError.check(array != null && value != null, "Arguments for raising an ArrayStoreException cannot be null");
        final ClassActor arrayClassActor = MaxineVM.isHosted() ? ClassActor.fromJava(array.getClass()) : ObjectAccess.readClassActor(array);
        final ClassActor componentClassActor = arrayClassActor.componentClassActor();
        throw new ArrayStoreException(value.getClass().getName() + " is not assignable to " + componentClassActor.name);
    }

    /**
     * Raises an {@code ClassCastException}. This is out-of-line to reduce the amount
     * of code inlined on the fast path for a type check.
     *
     * @param classActor the type being checked against
     * @param object the object whose type is not assignable to {@code classActor}
     */
    @NEVER_INLINE
    public static void classCastException(ClassActor classActor, Object object) {
        FatalError.check(object != null && classActor != null, "Arguments for raising a ClassCastException cannot be null");
        throw new ClassCastException(object.getClass().getName() + " is not assignable to " + classActor.name);
    }

    @NEVER_INLINE
    public static void nullPointerException() {
        throw new NullPointerException();
    }

    /**
     * Raises an {@code NegativeArraySizeException}. This is out-of-line to reduce the amount
     * of code inlined on the fast path for an array allocation.
     *
     * @param length the negative array length
     */
    @NEVER_INLINE
    public static void negativeArraySizeException(int length) {
        throw new NegativeArraySizeException(String.valueOf(length));
    }

    /**
     * Prints a line to the log stream for a given frame.
     */
    public static void logFrame(String prefix, TargetMethod targetMethod, Pointer ip) {
        if (prefix != null) {
            Log.print(prefix);
        }
        if (targetMethod != null) {
            Log.printMethod(targetMethod, false);
            final Pointer codeStart = targetMethod.codeStart();
            Log.print(" [");
            Log.print(codeStart);
            Log.print("+");
            Log.print(ip.minus(codeStart).toInt());
            Log.print("]");
        } else {
            Log.print("native{");
            Log.printSymbol(ip);
            Log.print("}");
        }
        Log.println();
    }
}
