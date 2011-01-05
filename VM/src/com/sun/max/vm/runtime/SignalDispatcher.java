/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.vm.VMOptions.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import sun.misc.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.*;

/**
 * The thread used to post and dispatch signals to user supplied {@link SignalHandler}s.
 * <p>
 * The special C signal handler mentioned in {@link Signal} is 'userSignalHandler' in trap.c.
 * This C signal handler atomically adds a signal to a queue in this class by up-calling
 * {@link #tryPostSignal(int)} and then notifies the native semaphore
 * on which the signal dispatching thread is {@linkplain #waitForSignal() waiting}.
 * <p>
 * The native layer of the VM also uses thread signal masks so that only one thread
 * (the {@linkplain VmOperationThread VM operation thread}) handles the
 * signals that are dispatched by this class.
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

    private static VMBooleanXXOption TraceSignalsOption = register(new VMBooleanXXOption("-XX:-TraceSignals", "Trace traps.") {
        @Override
        public boolean parseValue(Pointer optionValue) {
            TraceSignals = TraceSignalsOption.getValue();
            nativeSetSignalTracing(TraceSignals);
            return true;
        }
    }, MaxineVM.Phase.PRISTINE);
    private static boolean TraceSignals = TraceSignalsOption.getValue();

    /*
     * An ordinary lock (mutex) cannot be used when notifying the signal
     * dispatcher of a signal as the platform specific functions for
     * notifying condition variables (e.g. pthread_cond_signal(3)) are
     * typically not safe to call within an OS-level signal handler.
     * Instead, a single OS-level semaphore (e.g. POSIX sem_init(3))
     * is used.
     */
    private static native void nativeSignalInit(Address tryPostSignalAddress);
    private static native void nativeSignalFinalize();
    private static native void nativeSignalNotify();
    private static native void nativeSignalWait();

    @C_FUNCTION // called on the primordial thrad
    private static native void nativeSetSignalTracing(boolean flag);

    /**
     * The handle by which the address of {@link #tryPostSignal} can be communicated to the native substrate.
     */
    private static CriticalMethod tryPostSignal = new CriticalMethod(SignalDispatcher.class, "tryPostSignal", null, CallEntryPoint.C_ENTRY_POINT);

    static {
        new CriticalNativeMethod(SignalDispatcher.class, "nativeSignalInit");
        new CriticalNativeMethod(SignalDispatcher.class, "nativeSignalFinalize");
        new CriticalNativeMethod(SignalDispatcher.class, "nativeSignalNotify");
        new CriticalNativeMethod(SignalDispatcher.class, "nativeSignalWait");
    }

    /**
     * Attempts to atomically increment an element of {@link #PendingSignals}.
     * This is provided so that the native substrate does not have to encode
     * architecture specific mechanisms for trying to perform an atomic
     * update on a given value in memory.
     *
     * This code is called from within a native signal handler and so
     * must not block, cause any exception or assume that the thread
     * pointer/safepoint latch (i.e. R14 on x64) is set up correctly.
     *
     * @param signal the index of the element in {@link #PendingSignals} on which an atomic increment attempt is performed
     * @return {@code true} if the update succeeded, {@code false} otherwise
     */
    @VM_ENTRY_POINT
    @NO_SAFEPOINTS("executes inside a native signal handler")
    private static boolean tryPostSignal(int signal) {
        int n = PendingSignals.get(signal);
        return PendingSignals.compareAndSet(signal, n, n + 1);
    }

    private static volatile boolean started;

    /**
     * Terminates the signal dispatcher thread.
     */
    public static void terminate() {
        PendingSignals.incrementAndGet(ExitSignal);
        nativeSignalNotify();
    }

    /**
     * This flag can be used to work-around the fact that the rapid creation of a lot of
     * threads has a performance issue. Of course, it means that there's a risk that a
     * signal handler that blocks will cause handling of all subsequent signals to be
     * blocked. On the other hand, signal handling throughput is increased as there's
     * no need to create a new thread for each signal.
     */
    static boolean SerializeSignals = true;
    static {
        VMOptions.addFieldOption("-XX:", "SerializeSignals",
            "Run Java signal handlers on a single thread.");
    }

    @ALIAS(declaringClass = Signal.class)
    static Hashtable<Signal, SignalHandler> handlers;

    @ALIAS(declaringClass = Signal.class)
    static Hashtable<Integer, Signal> signals;

    @Override
    public void run() {
        nativeSignalInit(tryPostSignal.address());
        started = true;

        while (true) {
            int signal = waitForSignal();

            if (signal == ExitSignal) {
                nativeSignalFinalize();
                return;
            }

            final Signal sig = signals.get(signal);
            final SignalHandler handler = handlers.get(sig);
            if (handler != null) {
                if (SerializeSignals) {
                    try {
                        handler.handle(sig);
                    } catch (Throwable e) {
                        Log.println("Exception occurred while dispatching signal " + signal + " to handler - VM may need to be forcibly terminated");
                        Log.print(Utils.stackTraceAsString(e));
                    }
                } else {
                    Runnable runnable = new Runnable() {
                        public void run() {
                            // Don't bother to reset the priority. Signal handler will
                            // run at maximum priority inherited from the VM signal
                            // dispatch thread.
                            // Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
                            handler.handle(sig);
                        }
                    };
                    new Thread(runnable, sig + " handler").start();
                }
            }
        }
    }
}
