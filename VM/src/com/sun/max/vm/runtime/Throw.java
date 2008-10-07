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
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.debug.*;
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
            // N.B. use "->" to make dumped stacks look slightly different than exception stacktraces.
            Debug.err.print("        -> ");
            final TargetMethod targetMethod = stackFrame.targetMethod();
            if (targetMethod != null) {
                if (!stackFrame.isAdapter()) {
                    Debug.err.print(targetMethod.classMethodActor().format("%H.%n(%p)"));
                } else {
                    Debug.err.print("<adapter>");
                }
            } else {
                Debug.err.print("unknown:");
                Debug.err.print(stackFrame.instructionPointer());
            }
            Debug.err.println();
            if (_maximum > 0 && _count-- < 0) {
                return false;
            }
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
            Debug.err.println("exception thrown while raising another exception");
            if (!stackFrameWalker.isDumpingFatalStackTrace()) {
                stackFrameWalker.reset();
                stackFrameWalker.setIsDumpingFatalStackTrace(true);
                stackDumpWithException(throwable);
                stackFrameWalker.setIsDumpingFatalStackTrace(false);
            }
            FatalError.unexpected("exception thrown while raising another exception");
        }

        if (_dumpStackOnThrowOption.isPresent()) {
            throwable.printStackTrace(Debug.log);
            Debug.err.println("Complete unfiltered stack trace:");
            stackDumpWithException(throwable);
        }
        Safepoint.disable();
        raise(throwable, VMRegister.getCpuStackPointer(), VMRegister.getCpuFramePointer(), VMRegister.getInstructionPointer());
    }
    // Checkstyle: resume parameter assignment check

    public static void stackDumpWithException(Object throwable) {
        stackDump("Throwing " + throwable + ";", VMRegister.getInstructionPointer(), VMRegister.getCpuStackPointer(), VMRegister.getCpuFramePointer());
    }

    /**
     * Dumps the stack of the current thread.
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
        Debug.err.println(message);
        new VmStackFrameWalker(VmThread.current().vmThreadLocals()).inspect(instructionPointer, cpuStackPointer, cpuFramePointer, _stackFrameDumper);
    }

    public static void stackDump(String message, final Pointer instructionPointer, final Pointer cpuStackPointer, final Pointer cpuFramePointer, int depth) {
        Debug.err.println(message);
        new VmStackFrameWalker(VmThread.current().vmThreadLocals()).inspect(instructionPointer, cpuStackPointer, cpuFramePointer, new StackFrameDumper(depth));
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
        Debug.err.println(message);
        Pointer pointer = stackPointer.aligned();
        while (pointer.lessThan(endPointer)) {
            final Address potentialCodePointer = pointer.getWord().asAddress();
            final TargetMethod targetMethod = Code.codePointerToTargetMethod(potentialCodePointer);
            if (targetMethod != null) {
                Debug.err.print("        -> ");
                Debug.err.print(targetMethod.classMethodActor().format("%H.%n(%p)"));
                Debug.err.println();
            }
            pointer = pointer.plus(Word.size());
        }
    }

    @NEVER_INLINE
    public static void arrayIndexOutOfBoundsException() {
        throw new ArrayIndexOutOfBoundsException();
    }

    @NEVER_INLINE
    public static void arrayStoreException() {
        throw new ArrayStoreException();
    }

    @NEVER_INLINE
    public static void classCastException() {
        throw new ClassCastException();
    }

    @NEVER_INLINE
    public static void nullPointerException() {
        throw new NullPointerException();
    }

}
