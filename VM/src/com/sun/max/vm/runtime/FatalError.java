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
import com.sun.max.vm.heap.*;
import com.sun.max.vm.thread.*;

/**
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class FatalError extends Error {

    /**
     * A breakpoint should be set on this method when debugging the VM so that
     * fatal errors can be investigated before the VM exits.
     */
    @NEVER_INLINE
    private static void breakpoint() {
    }

    private FatalError(String msg) {
        super(msg);
    }

    public static FatalError unexpected(String message) {
        throw unexpected(message, Address.zero());
    }

    public static FatalError unexpected(String message, Throwable throwable) {
        throw unexpected(message, Address.zero(), throwable);
    }

    public static FatalError unexpected(String message, Address nativeTrapAddress) {
        throw unexpected(message, nativeTrapAddress, null);
    }

    public static FatalError unexpected(String message, Address nativeTrapAddress, Throwable throwable) {
        if (MaxineVM.isPrototyping()) {
            throw new FatalError(message);
        }

        breakpoint();

        if (ExitingGuard._guard) {
            Log.println("FATAL VM ERROR: Error occurred while handling previous fatal VM error");
            MaxineVM.native_exit(11);
        }
        ExitingGuard._guard = true;

        final boolean lockDisabledSafepoints = Log.lock();
        Log.print("FATAL VM ERROR: ");
        Heap.setTraceAllocation(false);
        Throw.stackDump(message, VMRegister.getInstructionPointer(), VMRegister.getCpuStackPointer(), VMRegister.getCpuFramePointer());
        if (throwable != null) {
            Log.print("Caused by: ");
            throwable.printStackTrace(Log.out);
        }
        if (Throw._scanStackOnFatalError.isPresent()) {
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

    public static void crash(String message) {
        Log.println(message);
        MaxineVM.native_exit(11); // should be symbolic
    }

    @INLINE
    public static void check(boolean condition, String message) {
        if (!condition) {
            unexpected(message);
        }
    }

    static class ExitingGuard {
        static boolean _guard;
    }
}
