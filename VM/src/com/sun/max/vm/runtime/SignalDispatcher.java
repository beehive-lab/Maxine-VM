/*
 * Copyright (c) 2010 Sun Microsystems, Inc.  All rights reserved.
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

import java.util.concurrent.atomic.*;

import sun.misc.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * The thread used to post and dispatch signals to user supplied {@link SignalHandler}s.
 * The {@linkplain VmThread#signalDispatcherThread single instance} of this thread is
 * built into the boot image so that signal delivery during GC is safe.
 *
 * Apart from the signal to terminate the signal dispatcher thread, only
 * the {@linkplain VmOperationThread VM operation thread} can post signals
 * to this thread. That is, all threads exception for the VM operation thread
 * have their signal mask set to block all signals except the synchronous
 * signal employed by the VM for safepointing and detecting runtime exceptions
 *
 * @author Doug Simon
 */
public final class SignalDispatcher extends Thread {

    /**
     * A set of counters, one per supported signal, used to post and consume signals.
     */
    private static final AtomicIntegerArray PendingSignals = new AtomicIntegerArray(nativeNumberOfSignals() + 1);

    /**
     * The special signal used to terminate the signal dispatcher thread.
     */
    private static final int ExitSignal = PendingSignals.length() - 1;

    /**
     * The lock used by the VM operation thread to notify the signal dispatcher thread when it
     * has posted a new {@linkplain #PendingSignals pending signal}.
     */
    private static final Object LOCK = JavaMonitorManager.newStickyLock();

    /**
     * Gets the number of signals supported by the platform that may be delivered to the VM.
     * The range of signal numbers that the VM expects to see is between 0 (inclusive) and
     * {@code nativeNumberOfSignals()} (exclusive).
     */
    @HOSTED_ONLY
    private static native int nativeNumberOfSignals();

    @HOSTED_ONLY
    public SignalDispatcher(ThreadGroup group) {
        super(group, "Signal Dispatcher");
        setDaemon(true);
    }

    /**
     * Blocks the current thread until a pending signal is available.
     *
     * @return the next available pending signal (which is removed from the set of pending signals)
     */
    private int waitForSignal() {
        while (true) {
            for (int signal = 0; signal < PendingSignals.length(); signal++) {
                int n = PendingSignals.get(signal);
                if (n > 0 && PendingSignals.compareAndSet(signal, n, n - 1)) {

                    if (TraceSignals) {
                        boolean lockDisabledSafepoints = Log.lock();
                        Log.print("Handling signal ");
                        Log.print(signal);
                        Log.println(" on the SignalDispatcher thread");
                        Log.unlock(lockDisabledSafepoints);
                    }
                    return signal;
                }
            }

            // There must never be a safepoint between the acquisition of LOCK and blocking on it (LOCK.wait()).
            // If this was to happen and a signal occurred during the VM operation for which safepoints were
            // triggered, there would be a deadlock between the signal dispatcher thread (which is holding
            // LOCK) and the VM operation thread (which wants to acquire LOCK to notify it).
            boolean wasDisabled = Safepoint.disable();
            FatalError.check(!wasDisabled, "Safepoints were disabled multiple times on SignalDispatcher thread");
            synchronized (LOCK) {
                try {
                    LOCK.wait();
                } catch (InterruptedException e) {
                }
            }
            Safepoint.enable();
        }
    }

    private static boolean TraceSignals;
    static {
        VMOptions.addFieldOption("-XX:", "TraceSignals", "Trace signals.");
    }

    /**
     * Adds a signal to the set of pending signals and notifies the dispatcher thread.
     */
    public static void postSignal(int signal) {
        FatalError.check(signal == ExitSignal || VmThread.current().isVmOperationThread(), "Asynchronous signal posted by non VM operation thread");
        if (TraceSignals) {
            boolean lockDisabledSafepoints = Log.lock();
            if (signal == ExitSignal) {
                Log.print("Posting ExitSignal");
            } else {
                Log.print("Posting signal ");
                Log.print(signal);
            }
            Log.println(" to the SignalDispatcher thread");
            Log.unlock(lockDisabledSafepoints);
        }
        PendingSignals.incrementAndGet(signal);
        synchronized (LOCK) {
            LOCK.notify();
        }
    }

    private static volatile boolean started;

    /**
     * Terminates the signal dispatcher thread.
     */
    public static void terminate() {
        postSignal(ExitSignal);
    }

    @Override
    public void run() {
        started = true;
        while (true) {
            int signal = waitForSignal();

            if (signal == ExitSignal) {
                return;
            }

            try {
                ClassRegistry.Signal_dispatch.invoke(IntValue.from(signal));
            } catch (Exception e) {
                Log.println("Exception occurred while dispatching signal " + signal + " to handler - VM may need to be forcibly terminated");
                Log.print(Utils.stackTraceAsString(e));
            }
        }
    }
}
