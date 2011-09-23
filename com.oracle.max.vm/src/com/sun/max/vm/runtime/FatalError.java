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

import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.intrinsics.Infopoints.*;
import static com.sun.max.vm.runtime.VMRegister.*;
import static com.sun.max.vm.thread.VmThread.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.program.ProgramError.Handler;
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
 */
public final class FatalError extends Error {

    private static boolean CoreOnError;
    private static boolean TrapOnError;
    static {
        VMOptions.addFieldOption("-XX:", "CoreOnError", FatalError.class, "Generate core dump on fatal error.", MaxineVM.Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "TrapOnError", FatalError.class, "Issue breakpoint trap on fatal error.", MaxineVM.Phase.PRISTINE);
    }

    static {
        ProgramError.setHandler(new Handler() {
            public void handle(String message, Throwable throwable) {
                unexpected(message, false, throwable, Pointer.zero());
            }
        });
    }

    private static boolean DUMPING_STACKS_IN_FATAL_ERROR;
    private static FatalError FATAL_ERROR_WHILE_DUMPING_STACKS = new FatalError(null, null);
    static {
        FATAL_ERROR_WHILE_DUMPING_STACKS.setStackTrace(new StackTraceElement[0]);
    }

    /**
     * A breakpoint should be set on this method when debugging the VM so that
     * fatal errors can be investigated before the VM exits.
     */
    @NEVER_INLINE
    public static void breakpoint() {
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
     * @see #unexpected(String, boolean, Throwable, Pointer)
     * @return never
     */
    @NEVER_INLINE
    public static FatalError unexpected(String message) {
        throw unexpected(message, false, null, Pointer.zero());
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
     * @see #unexpected(String, boolean, Throwable, Pointer)
     * @return never
     */
    @NEVER_INLINE
    public static FatalError unexpected(String message, Throwable throwable) {
        throw unexpected(message, false, throwable, Pointer.zero());
    }

    /**
     * Determines if a fatal error is in flight. This can be used to try and prevent recursive fatal errors.
     */
    public static boolean inFatalError() {
        return recursionCount != 0;
    }

    /**
     * Reports the occurrence of some error condition. Before exiting the VM, this method attempts to print a stack
     * trace.
     *
     * This method never returns normally.
     *
     * If {@code throwable == null}, this method does not perform any synchronization or heap allocation.
     *
     * @param message a message describing the trap. This value may be {@code null}.
     * @param trappedInNative specifies if this is a fatal error due to a trap in native code. If so, then the native
     *            code instruction pointer at which the trap occurred can be extracted from {@code trapState}
     * @param throwable an exception given more detail on the cause of the error condition. This value may be {@code null}.
     * @param trapFrame if non-zero, then this is a pointer to the {@linkplain TrapFrameAccess trap frame} for the trap
     *            resulting in this fatal error
     * @return never
     */
    @NEVER_INLINE
    public static FatalError unexpected(String message, boolean trappedInNative, Throwable throwable, Pointer trapFrame) {
        if (MaxineVM.isHosted()) {
            throw new FatalError(message, throwable);
        }

        SafepointPoll.disable();

        Throw.TraceExceptions = 0;
        Throw.TraceExceptionsRaw = false;

        // Try to recover from a fatal error while dumping stacks of the other threads
        if (DUMPING_STACKS_IN_FATAL_ERROR && FATAL_ERROR_WHILE_DUMPING_STACKS != null) {
            FatalError error = FATAL_ERROR_WHILE_DUMPING_STACKS;
            VmThread.current().stackDumpStackFrameWalker().reset();
            // Nulling FATAL_ERROR_WHILE_DUMPING_STACKS makes this is a one shot attempt at recovery
            FATAL_ERROR_WHILE_DUMPING_STACKS = null;
            throw error;
        }

        if (recursionCount >= MAX_RECURSION_COUNT) {
            Log.println("FATAL VM ERROR: Error occurred while handling previous fatal VM error");
            exit(false, Address.zero());
        }
        recursionCount++;

        final VmThread vmThread = VmThread.current();
        final boolean lockDisabledSafepoints = Log.lock();
        if (vmThread != null) {
            vmThread.stackDumpStackFrameWalker().reset();
        }

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
        Log.printThread(vmThread, true);

        if (!trapFrame.isZero()) {
            Log.print("------ Trap State for thread ");
            Log.printThread(vmThread, false);
            Log.println(" ------");
            vm().trapFrameAccess.logTrapFrame(trapFrame);
        }

        if (vmThread != null) {
            dumpStackAndThreadLocals(currentTLA(), trappedInNative);

            if (throwable != null) {
                Log.print("------ Cause Exception ------");
                throwable.printStackTrace(Log.out);
            }

            DUMPING_STACKS_IN_FATAL_ERROR = true;
            VmThreadMap.ACTIVE.forAllThreadLocals(null, dumpStackOfNonCurrentThread);
            DUMPING_STACKS_IN_FATAL_ERROR = false;
        }

        if (vmThread == null || trappedInNative || Throw.ScanStackOnFatalError) {
            final Word highestStackAddress = VmThreadLocal.HIGHEST_STACK_SLOT_ADDRESS.load(currentTLA());
            Throw.stackScan("RAW STACK SCAN FOR CODE POINTERS:", VMRegister.getCpuStackPointer(), highestStackAddress.asPointer());
        }
        Log.unlock(lockDisabledSafepoints);
        Address ip = Address.zero();
        if (trappedInNative && !trapFrame.isZero())  {
            ip = vm().trapFrameAccess.getPC(trapFrame);
        }
        exit(trappedInNative, ip);

        throw null; // unreachable
    }

    @NEVER_INLINE
    private static void exit(boolean doTrapExit, Address instructionPointer) {
        if (CoreOnError) {
            MaxineVM.core_dump();
        }
        if (TrapOnError) {
            Intrinsics.breakpointTrap();
        }
        if (doTrapExit) {
            MaxineVM.native_trap_exit(11, instructionPointer);
        }
        MaxineVM.native_exit(11);
    }


    /**
     * Causes the VM to print an error message and exit immediately.
     *
     * @param message the error message to print
     */
    @NEVER_INLINE
    public static void crash(String message) {
        Log.println(message);
        exit(false, Address.zero());
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
            unexpected(message, false, null, Pointer.zero());
        }
    }

    /**
     * Reports that an unimplemented piece of VM functionality was encountered.
     *
     * This method never returns normally.
     *
     * This method does not perform any synchronization or heap allocation.
     *
     * @see #unexpected(String, boolean, Throwable, Pointer)
     * @return never
     */
    @NEVER_INLINE
    public static FatalError unimplemented() {
        throw unexpected("Unimplemented", false, null, Pointer.zero());
    }

    /**
     * Dumps the stack and thread locals of a given thread to the log stream.
     *
     * @param tla VM thread locals of a thread
     * @param trappedInNative specifies if this is for a thread that trapped in native code
     */
    static void dumpStackAndThreadLocals(Pointer tla, boolean trappedInNative) {
        final VmThread vmThread = VmThread.fromTLA(tla);
        Log.print("------ Stack dump for thread ");
        Log.printThread(vmThread, false);
        Log.println(" ------");
        if (!trappedInNative && tla == currentTLA()) {
            Throw.stackDump(null, Pointer.fromLong(here()), getCpuStackPointer(), getCpuFramePointer());
        } else {
            Throw.stackDump(null, tla);
        }

        Log.print("------ Thread locals for thread ");
        Log.printThread(vmThread, false);
        Log.println(" ------");
        Log.printThreadLocals(tla, true);
    }

    static final class DumpStackOfNonCurrentThread implements Pointer.Procedure {
        public void run(Pointer tla) {
            if (ETLA.load(tla) != ETLA.load(currentTLA())) {
                try {
                    dumpStackAndThreadLocals(tla, false);
                } catch (FatalError e) {
                    Log.println("--- STACK TRACE TERMINATED DUE TO RECURSIVE FATAL ERROR ---");
                }
            }
        }
    }

    private static final DumpStackOfNonCurrentThread dumpStackOfNonCurrentThread = new DumpStackOfNonCurrentThread();
    private static final int MAX_RECURSION_COUNT = 2;
    private static int recursionCount;
}
