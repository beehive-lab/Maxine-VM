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
import com.sun.max.program.*;
import com.sun.max.program.ProgramError.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.thread.*;

/**
 * A collection of static methods for reporting errors indicating some fatal condition
 * that should cause a hard exit of the VM. All errors reported by use of {@link ProgramError}
 * are rerouted to use this class.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Paul Caprioli
 */
public final class FatalError extends Error {

    static {
        ProgramError.setHandler(new Handler() {
            public void handle(String message, Throwable throwable) {
                unexpected(message, Address.zero(), throwable);
            }
        });
    }

    /**
     * A breakpoint should be set on this method when debugging the VM so that
     * fatal errors can be investigated before the VM exits.
     */
    @NEVER_INLINE
    private static void breakpoint() {
    }

    private FatalError(String msg, Throwable throwable) {
        super(msg, throwable);
    }

    /**
     * Reports the occurrence of some error condition.
     *
     * This method never returns normally.
     *
     * @param message a message describing the error condition. This value may be {@code null}.
     * @see #unexpected(String, Address, Throwable)
     */
    public static FatalError unexpected(String message) {
        throw unexpected(message, Address.zero(), null);
    }

    /**
     * Reports the occurrence of some error condition.
     *
     * This method never returns normally.
     *
     * @param message a message describing the error condition. This value may be {@code null}.
     * @param throwable an exception given more detail on the cause of the error condition. This value may be {@code null}.
     * @see #unexpected(String, Address, Throwable)
     */
    public static FatalError unexpected(String message, Throwable throwable) {
        throw unexpected(message, Address.zero(), throwable);
    }

    /**
     * Reports the occurrence of a trap that occurred while executing native code.
     *
     * This method never returns normally.
     *
     * @param message a message describing the trap. This value may be {@code null}.
     * @param nativeTrapAddress the address reported by the OS at which the trap occurred
     * @see #unexpected(String, Address, Throwable)
     */
    public static FatalError unexpected(String message, Address nativeTrapAddress) {
        throw unexpected(message, nativeTrapAddress, null);
    }

    /**
     * Reports the occurrence of some error condition. Before exiting the VM, this method attempts to
     * print a stack trace.
     *
     * This method never returns normally.
     *
     * @param message a message describing the trap. This value may be {@code null}.
     * @param throwable an exception given more detail on the cause of the error condition. This value may be {@code null}.
     * @param nativeTrapAddress if this value is not equal to {@link Address#zero()}, then it is the address reported by
     *            the OS at which a trap occurred
     */
    public static FatalError unexpected(String message, Address nativeTrapAddress, Throwable throwable) {
        if (MaxineVM.isPrototyping()) {
            throw new FatalError(message, throwable);
        }

        breakpoint();

        if (ExitingGuard.guard) {
            Log.println("FATAL VM ERROR: Error occurred while handling previous fatal VM error");
            MaxineVM.native_exit(11);
        }
        ExitingGuard.guard = true;

        final boolean lockDisabledSafepoints = Log.lock();
        Log.print("FATAL VM ERROR: ");
        Heap.setTraceAllocation(false);
        Throw.stackDump(message, VMRegister.getInstructionPointer(), VMRegister.getCpuStackPointer(), VMRegister.getCpuFramePointer());
        if (throwable != null) {
            Log.print("Caused by: ");
            throwable.printStackTrace(Log.out);
        }
        if (Throw.scanStackOnFatalError.getValue()) {
            Throw.stackScan("stack scan", VMRegister.getCpuStackPointer(), VmThread.current().vmThreadLocals());
        }
        Log.unlock(lockDisabledSafepoints);
        if (nativeTrapAddress.isZero()) {
            MaxineVM.native_exit(11);
        }
        MaxineVM.native_trap_exit(11, nativeTrapAddress);
        final FatalError unreachable = UnsafeLoophole.cast(null);
        throw unreachable;
    }

    /**
     * Causes the VM to print an error message and exit immediately.
     *
     * @param message the error message to print
     */
    public static void crash(String message) {
        Log.println(message);
        MaxineVM.native_exit(11); // should be symbolic
    }

    /**
     * Checks a given condition and if it is {@code false}, a fatal error is raised.
     *
     * @param condition a condition to test
     * @param message a message describing the error condition being tested
     */
    @INLINE
    public static void check(boolean condition, String message) {
        if (!condition) {
            unexpected(message, Address.zero(), null);
        }
    }

    /**
     * Reports that an unimplemented piece of VM functionality was encountered.
     *
     * This method never returns normally.
     *
     * @see #unexpected(String, Address, Throwable)
     */

    public static FatalError unimplemented() {
        throw unexpected("Unimplemented", Address.zero(), null);
    }

    static class ExitingGuard {
        static boolean guard;
    }
}
