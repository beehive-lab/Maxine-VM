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
package com.sun.max.vm.runtime;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.snippet.ArrayGetSnippet.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.*;
import com.sun.max.vm.thread.*;

/**
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class Throw {

    private Throw() {
    }

    public static VMOption _dumpStackOnThrowOption = new VMOption("-XX:DumpStackOnThrow",
                    "Reports a stack trace for every throw operation, regardless of whether the exception is " +
                    "caught or uncaught.", MaxineVM.Phase.PRISTINE);
    public static VMOption _scanStackOnFatalError = new VMOption("-XX:ScanStackOnFatalError",
                    "Reports a stack trace scan when a fatal VM occurs.", MaxineVM.Phase.PRISTINE);

    private static class StackFrameDumper implements StackFrameVisitor {
        private final int _maximum;
        private int _count;
        StackFrameDumper(int max) {
            _maximum = max;
        }
        @Override
        public boolean visitFrame(StackFrame stackFrame) {
            final boolean lockDisabledSafepoints = Log.lock();
            // N.B. use "->" to make dumped stacks look slightly different than exception stacktraces.
            Log.print("        -> ");
            final TargetMethod targetMethod = stackFrame.targetMethod();
            if (targetMethod != null) {
                if (!stackFrame.isAdapter()) {
                    final ClassMethodActor classMethodActor = targetMethod.classMethodActor();
                    Log.print(classMethodActor.holder().name());
                    Log.print(".");
                    Log.print(classMethodActor.name());
                    Log.print(classMethodActor.descriptor());
                } else {
                    Log.print("<adapter>");
                }
                Log.print(" [");
                Log.print(stackFrame.instructionPointer());
                Log.print("+");
                Log.print(stackFrame.instructionPointer().minus(targetMethod.codeStart()).toInt());
                Log.print("]");
            } else {
                Log.print("unknown:");
                Log.print(stackFrame.instructionPointer());
            }
            Log.println();
            if (_maximum > 0 && _count-- < 0) {
                Log.unlock(lockDisabledSafepoints);
                return false;
            }
            Log.unlock(lockDisabledSafepoints);
            return true;
        }
    }

    private static final StackFrameDumper _stackFrameDumper = new StackFrameDumper(0);

    /**
     * Unwinds the current thread's stack to the frame containing an exception handler (there is guaranteed to be one).
     *
     * <b>Safepoints must be {@linkplain Safepoint#disable() disabled} before calling this method.</b> They are
     * re-enabled just prior jumping to the exception handler.
     *
     * @param throwable the object to be passed to the exception handler - must not be null
     * @param stackPointer the stack pointer to be used when determining the point at which exception was raised
     * @param framePointer the frame pointer to be used when determining the point at which exception was raised
     * @param instructionPointer the instruction pointer to be used when determining the point at which exception was raised
     */
    public static void raise(Throwable throwable, Pointer stackPointer, Pointer framePointer, Pointer instructionPointer) {
        FatalError.check(throwable != null, "Trying to raise an exception with a null Throwable object");
        VmThread.current().stackFrameWalker().unwind(instructionPointer, stackPointer, framePointer, throwable);
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
     * The complete sequence from the call to this method until the transfer of control to the exception handler must be
     * atomic from the perspective of the garbage collector. To achieve this, safepoints are
     * {@linkplain Safepoint#disable() disabled} here and must be {@linkplain Safepoint#enable() re-enabled} just prior
     * jumping to the exception handler. The latter is the responsibility of every implementation of
     * {@link DynamicCompilerScheme#walkFrame(com.sun.max.vm.stack.StackFrameWalker, boolean, com.sun.max.vm.compiler.target.TargetMethod, Purpose, Object)}
     * .
     *
     * @param throwable throwable the object to be passed to the exception handler. If this value is null, then a
     *            {@link NullPointerException} is instantiated and raised instead.
     */
    // Checkstyle: stop parameter assignment check
    public static void raise(Throwable throwable) {
        if (throwable == null) {
            throwable = new NullPointerException();
        }
        final VmStackFrameWalker stackFrameWalker = VmThread.current().stackFrameWalker();
        if (stackFrameWalker.isInUse()) {
            Log.println("exception thrown while raising another exception");
            if (!stackFrameWalker.isDumpingFatalStackTrace()) {
                stackFrameWalker.reset();
                stackFrameWalker.setIsDumpingFatalStackTrace(true);
                stackDumpWithException(throwable);
                stackFrameWalker.setIsDumpingFatalStackTrace(false);
            }
            FatalError.unexpected("exception thrown while raising another exception");
        }

        if (_dumpStackOnThrowOption.isPresent()) {
            throwable.printStackTrace(Log.out);
        }
        Safepoint.disable();
        raise(throwable, VMRegister.getCpuStackPointer(), VMRegister.getCpuFramePointer(), VMRegister.getInstructionPointer());
    }
    // Checkstyle: resume parameter assignment check

    public static void stackDumpWithException(Object throwable) {
        stackDump("Throwing " + throwable + ";", VMRegister.getInstructionPointer(), VMRegister.getCpuStackPointer(), VMRegister.getCpuFramePointer());
    }

    /**
     * Dumps the entire stack of the current thread.
     * <p>
     * This stack dump is meant to be more primitive that the dump obtained by {@link Thread#dumpStack()} in that all
     * the actual frames are dumped, not only the frames of application visible methods. In addition, there is no
     * attempt to decode Java frame descriptors to show virtual frames (i.e. frames that would have existed if there was
     * no inlining performed).
     *
     * @param message the message to print accompanying the stack trace
     * @param instructionPointer the instruction pointer at which to begin the stack trace
     * @param cpuStackPointer the stack pointer at which to begin the stack trace
     * @param cpuFramePointer the frame pointer at which to begin the stack trace
     */
    @NEVER_INLINE
    public static void stackDump(String message, final Pointer instructionPointer, final Pointer cpuStackPointer, final Pointer cpuFramePointer) {
        Log.println(message);
        new VmStackFrameWalker(VmThread.currentVmThreadLocals()).inspect(instructionPointer, cpuStackPointer, cpuFramePointer, _stackFrameDumper);
    }

    /**
     * Dumps the stack of the current thread. This method is equivalent to
     * {@link #stackDump(String, Pointer, Pointer, Pointer)} except that it takes an extra parameter ({@code depth})
     * that specifies the maximum number of stack frames to be walked.
     *
     * @param message the message to print accompanying the stack trace
     * @param instructionPointer the instruction pointer at which to begin the stack trace
     * @param cpuStackPointer the stack pointer at which to begin the stack trace
     * @param cpuFramePointer the frame pointer at which to begin the stack trace
     * @param depth the maximum number of stack frames to be walked
     */
    public static void stackDump(String message, final Pointer instructionPointer, final Pointer cpuStackPointer, final Pointer cpuFramePointer, int depth) {
        Log.println(message);
        new VmStackFrameWalker(VmThread.currentVmThreadLocals()).inspect(instructionPointer, cpuStackPointer, cpuFramePointer, new StackFrameDumper(depth));
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
        Pointer pointer = stackPointer.aligned();
        while (pointer.lessThan(endPointer)) {
            final Address potentialCodePointer = pointer.getWord().asAddress();
            final TargetMethod targetMethod = Code.codePointerToTargetMethod(potentialCodePointer);
            if (targetMethod != null) {
                Log.print("        -> ");
                Log.print(targetMethod.classMethodActor().format("%H.%n(%p)"));
                Log.println();
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
        throw new ArrayIndexOutOfBoundsException("Index: " + index + ", Array length: " + ReadLength.readLength(array));
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
        final ClassActor arrayClassActor = MaxineVM.isPrototyping() ? ClassActor.fromJava(array.getClass()) : ObjectAccess.readClassActor(array);
        final ClassActor componentClassActor = arrayClassActor.componentClassActor();
        throw new ArrayStoreException(value.getClass().getName() + " is not assignable to " + componentClassActor.name());
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
        throw new ClassCastException(object.getClass().getName() + " is not assignable to " + classActor.name());
    }

    @NEVER_INLINE
    public static void nullPointerException() {
        throw new NullPointerException();
    }

}
