/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.vm.intrinsics.Infopoints.*;
import static com.sun.max.vm.jdk.JDK_java_lang_Throwable.*;
import static com.sun.max.vm.object.ArrayAccess.*;
import static com.sun.max.vm.runtime.VMRegister.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.jdk.JDK_java_lang_Throwable.Backtrace;
import com.sun.max.vm.object.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.ti.*;
import com.sun.max.vm.type.*;

/**
 */
public final class Throw {

    private Throw() {
    }

    public static int TraceExceptions;
    public static boolean TraceExceptionsRaw;
    public static boolean FatalVMAssertions = true;
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
        VMOptions.addFieldOption("-XX:", "FatalVMAssertions", Throw.class,
            "Convert assertions thrown in the VM code to fatal errors.", Phase.PRISTINE);
    }

    static class StackFrameDumper extends RawStackFrameVisitor {
        int frames;
        @Override
        public boolean visitFrame(StackFrameCursor current, StackFrameCursor callee) {
            if (frames++ < TraceExceptionsRawMaxFrames) {
                // N.B. use "->" to make dumped stacks look slightly different than exception stack traces.
                final Pointer ip = current.ipAsPointer();
                Throw.logFrame("        -> ", current.targetMethod(), ip);
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
     * This method disables safepoints so that a GC request (or any other VM operation) cannot interrupt
     * the unwinding process. Why? A {@linkplain VmThread#unwindingStackFrameWalker(Throwable) shared}
     * stack walker object is used for unwinding and stack reference map preparation.
     * Safepoints are re-enabled when the exception object is {@linkplain VmThread#loadExceptionForHandler() loaded}
     * by the exception handler.
     *
     * @param throwable the object to be passed to the exception handler - must not be null
     * @param sp the stack pointer to be used when determining the point at which exception was raised
     * @param fp the frame pointer to be used when determining the point at which exception was raised
     * @param ip the instruction pointer to be used when determining the point at which exception was raised
     */
    public static void raise(Throwable throwable, Pointer sp, Pointer fp, CodePointer ip) {
        VMTI.handler().raise(throwable, sp, fp, ip);

        convertAssertionToFatalError(throwable);

        FatalError.check(throwable != null, "Trying to raise an exception with a null Throwable object");
        final VmStackFrameWalker sfw = VmThread.current().unwindingStackFrameWalker(throwable);

        VmThread.current().checkYellowZoneForRaisingException();
        SafepointPoll.disable();

        sfw.unwind(ip.toPointer(), sp, fp, throwable);
        FatalError.unexpected("could not find top-level exception handler");
    }

    /**
     * Converts an {@link AssertionError} to a {@link FatalError} if {@value #FatalVMAssertions} is {@code true}
     * and {@code throwable} is an AssertionError.
     */
    public static void convertAssertionToFatalError(Throwable throwable) {
        if (FatalVMAssertions && StackTraceInThrowable && throwable instanceof AssertionError) {
            Backtrace bt = JDK_java_lang_Throwable.getBacktrace(throwable);
            if (bt != null) {
                for (int i = 0; i < bt.count; i++) {
                    ClassMethodActor cma = bt.methods[i];
                    if (cma.isInitializer() && AssertionError.class.isAssignableFrom(cma.holder().toJava())) {
                        // still in exception constructor chain
                    } else {
                        // this is the method causing the assertion error
                        if (cma.holder().classLoader == BootClassLoader.BOOT_CLASS_LOADER) {
                            FatalError.unexpected("Assertion thrown in the VM", throwable);
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * Unwinds the current thread's stack to the frame containing an exception handler (there is guaranteed to be one).
     */
    @INLINE
    public static void raise(Object throwable) {
        if (throwable == null || throwable instanceof Throwable) {
            raise(UnsafeCast.asThrowable(throwable));
        } else {
            FatalError.unexpected("Object thrown not a throwable");
        }
    }

    /**
     * Unwinds the current thread's stack to the frame containing an exception handler (there is guaranteed to be one).
     *
     * @param throwable the object to be passed to the exception handler. If this value is null, then a
     *            {@link NullPointerException} is instantiated and raised instead.
     */
    @INLINE
    public static void raise(Throwable throwable) {
        convertAndRaise(throwable, getCpuStackPointer(), getCpuFramePointer(), CodePointer.from(here()));
    }

    @NEVER_INLINE
    public static void convertAndRaise(Throwable throwable, Pointer sp, Pointer fp, CodePointer ip) {
        if (throwable == null) {
            throwable = new NullPointerException();
        }

        convertAssertionToFatalError(throwable);
        traceThrow(throwable);
        raise(throwable, sp, fp, ip);
    }

    public static void traceThrow(Throwable throwable) {
        if (TraceExceptions == 1) {
            Log.printThread(VmThread.current(), false);
            Log.println(": Throwing " + throwable);
        } else if (TraceExceptions >= 2) {
            StackTraceElement[] trace = throwable.getStackTrace();
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
     * @param ip the instruction pointer at which to begin the stack trace
     * @param sp the stack pointer at which to begin the stack trace
     * @param fp the frame pointer at which to begin the stack trace
     */
    @NEVER_INLINE
    public static void stackDump(String message, Pointer ip, final Pointer sp, final Pointer fp) {
        if (message != null) {
            Log.println(message);
        }
        VmThread.current().stackDumpStackFrameWalker().inspect(ip, sp, fp, stackFrameDumper);
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
        Pointer ip = anchor.isZero() ? Pointer.zero() : JavaFrameAnchor.PC.get(anchor);
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
     * @param sp the pointer to top of the stack
     * @param end the pointer to the end of the stack
     */
    public static void stackScan(String message, final Pointer sp, final Pointer end) {
        Log.println(message);
        Pointer pointer = sp.wordAligned();
        while (pointer.lessThan(end)) {
            final CodePointer potentialCodePointer = CodePointer.from(pointer.getWord());
            final TargetMethod targetMethod = potentialCodePointer.toTargetMethod();
            if (targetMethod != null) {
                logFrame(null, targetMethod, potentialCodePointer.toPointer());
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
    public static ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException(Object array, int index) {
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
    public static ArrayStoreException arrayStoreException(Object array, Object value) {
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
    public static ClassCastException classCastException(ClassActor classActor, Object object) {
        FatalError.check(object != null && classActor != null, "Arguments for raising a ClassCastException cannot be null");
        throw new ClassCastException(object.getClass().getName() + " is not assignable to " + classActor.name);
    }

    @NEVER_INLINE
    public static NullPointerException nullPointerException() {
        throw new NullPointerException();
    }

    /**
     * Raises an {@code NegativeArraySizeException}. This is out-of-line to reduce the amount
     * of code inlined on the fast path for an array allocation.
     *
     * @param length the negative array length
     */
    @NEVER_INLINE
    public static NegativeArraySizeException negativeArraySizeException(int length) {
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
            CodePointer codeStart = targetMethod.codeStart();
            Log.print(" [");
            Log.print(codeStart);
            Log.print("+");
            Log.print(ip.minus(codeStart.toAddress()).toInt());
            Log.print("]");
        } else {
            Log.print("native{");
            Log.printSymbol(ip);
            Log.print("}");
        }
        Log.println();
    }
}
