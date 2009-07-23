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

import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.program.ProgramError.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.thread.*;

/**
 * A collection of static methods for reporting errors indicating some fatal condition
 * that should cause a hard exit of the VM. All errors reported by use of {@link ProgramError}
 * are rerouted to use this class.
 *
 * None of the methods in this class perform any synchronization or heap allocation
 * and they should never cause recursive error reporting.
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
     * This method does not perform any synchronization or heap allocation.
     *
     * @param message a message describing the error condition. This value may be {@code null}.
     * @see #unexpected(String, Address, Throwable)
     * @return never
     */
    public static FatalError unexpected(String message) {
        throw unexpected(message, Address.zero(), null);
    }

    /**
     * Reports the occurrence of some error condition.
     *
     * This method never returns normally.
     *
     * If {@code throwable == null}, this method does not perform any synchronization or heap allocation.
     *
     * @param message a message describing the error condition. This value may be {@code null}.
     * @param throwable an exception given more detail on the cause of the error condition. This value may be {@code null}.
     * @see #unexpected(String, Address, Throwable)
     * @return never
     */
    public static FatalError unexpected(String message, Throwable throwable) {
        throw unexpected(message, Address.zero(), throwable);
    }

    /**
     * Reports the occurrence of a trap that occurred while executing native code.
     *
     * This method never returns normally.
     *
     * This method does not perform any synchronization or heap allocation.
     *
     * @param message a message describing the trap. This value may be {@code null}.
     * @param nativeTrapAddress the address reported by the OS at which the trap occurred
     * @see #unexpected(String, Address, Throwable)
     * @return never
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
     * If {@code throwable == null}, this method does not perform any synchronization or heap allocation.
     *
     * @param message a message describing the trap. This value may be {@code null}.
     * @param throwable an exception given more detail on the cause of the error condition. This value may be {@code null}.
     * @param nativeTrapAddress if this value is not equal to {@link Address#zero()}, then it is the address reported by
     *            the OS at which a trap occurred in native code
     * @return never
     */
    public static FatalError unexpected(String message, Address nativeTrapAddress, Throwable throwable) {
        if (MaxineVM.isPrototyping()) {
            throw new FatalError(message, throwable);
        }

        Safepoint.disable();
        breakpoint();

        if (recursionCount >= MAX_RECURSION_COUNT) {
            Log.println("FATAL VM ERROR: Error occurred while handling previous fatal VM error");
            MaxineVM.native_exit(11);
        }
        recursionCount++;

        final boolean lockDisabledSafepoints = Log.lock();
        final VmThread vmThread = VmThread.current();
        vmThread.stackDumpStackFrameWalker().reset();

        Log.println();
        Log.print("FATAL VM ERROR[");
        Log.print(recursionCount);
        Log.print("]: ");
        if (message != null) {
            Log.println(message);
        } else {
            Log.println();
        }
        Log.print("Faulting thread: ");
        Log.printVmThread(vmThread, true);

        dumpStackAndThreadLocals(VmThread.currentVmThreadLocals());

        if (throwable != null) {
            Log.print("------ Cause Exception ------");
            throwable.printStackTrace(Log.out);
        }
        VmThreadMap.ACTIVE.forAllVmThreadLocals(null, dumpStackOfNonCurrentThread);

        if (!nativeTrapAddress.isZero() || Throw.scanStackOnFatalError.getValue()) {
            final Word highestStackAddress = VmThreadLocal.HIGHEST_STACK_SLOT_ADDRESS.getConstantWord();
            Throw.stackScan("RAW STACK SCAN FOR CODE POINTERS:", VMRegister.getCpuStackPointer(), highestStackAddress.asPointer());
        }
        Log.unlock(lockDisabledSafepoints);
        if (nativeTrapAddress.isZero()) {
            MaxineVM.native_exit(11);
        }
        MaxineVM.native_trap_exit(11, nativeTrapAddress);
        throw null; // unreachable
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
     * This method does not perform any synchronization or heap allocation.
     *
     * @see #unexpected(String, Address, Throwable)
     * @return never
     */
    public static FatalError unimplemented() {
        throw unexpected("Unimplemented", Address.zero(), null);
    }

    /**
     * Dumps the stack and thread locals of a given thread to the log stream.
     *
     * @param vmThreadLocals VM thread locals of a thread
     */
    static void dumpStackAndThreadLocals(Pointer vmThreadLocals) {
        final VmThread vmThread = VmThread.fromVmThreadLocals(vmThreadLocals);
        Log.print("------ Stack dump for thread ");
        Log.printVmThread(vmThread, false);
        Log.println(" ------");
        if (vmThreadLocals == VmThread.currentVmThreadLocals()) {
            Throw.stackDump(null, VMRegister.getInstructionPointer(), VMRegister.getCpuStackPointer(), VMRegister.getCpuFramePointer());
        } else {
            Throw.stackDump(null, vmThreadLocals);
        }

        Log.print("------ Thread locals for thread ");
        Log.printVmThread(vmThread, false);
        Log.println(" ------");
        Log.printVmThreadLocals(vmThreadLocals, true);
    }

    static final class DumpStackOfNonCurrentThread implements Pointer.Procedure {
        public void run(Pointer vmThreadLocals) {
            if (SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord(vmThreadLocals) != SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord()) {
                dumpStackAndThreadLocals(vmThreadLocals);
            }
        }
    }

    private static final DumpStackOfNonCurrentThread dumpStackOfNonCurrentThread = new DumpStackOfNonCurrentThread();
    private static final int MAX_RECURSION_COUNT = 2;
    private static int recursionCount;
}
