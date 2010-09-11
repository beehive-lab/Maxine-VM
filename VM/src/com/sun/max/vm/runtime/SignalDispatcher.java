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
import com.sun.max.platform.*;
import com.sun.max.vm.*;
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
    private static final AtomicIntegerArray PendingSignals = new AtomicIntegerArray(Platform.numberOfSignals() + 1);

    /**
     * The special signal used to terminate the signal dispatcher thread.
     */
    private static final int ExitSignal = PendingSignals.length() - 1;

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

            nativeSignalWait();
        }
    }

    private static boolean TraceSignals;
    static {
        VMOptions.addFieldOption("-XX:", "TraceSignals", "Trace signals.");
    }

    /**
     * An ordinary lock (mutex) cannot be used when notifying the signal
     * dispatcher of a signal as the platform specific functions for
     * notifying condition variables (e.g. pthread_cond_signal(3)) are
     * typically not safe to call within an OS-level signal handler.
     * Instead, a single OS-level semaphore (e.g. POSIX sem_init(3))
     * is used.
     */
    private static native void nativeSignalInit();
    private static native void nativeSignalFinalize();
    @C_FUNCTION
    private static native void nativeSignalNotify();
    private static native void nativeSignalWait();

    static {
        new CriticalNativeMethod(SignalDispatcher.class, "nativeSignalInit");
        new CriticalNativeMethod(SignalDispatcher.class, "nativeSignalFinalize");
        new CriticalNativeMethod(SignalDispatcher.class, "nativeSignalNotify");
        new CriticalNativeMethod(SignalDispatcher.class, "nativeSignalWait");
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
        nativeSignalNotify();
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
        nativeSignalInit();
        started = true;
        while (true) {
            int signal = waitForSignal();

            if (signal == ExitSignal) {
                nativeSignalFinalize();
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
