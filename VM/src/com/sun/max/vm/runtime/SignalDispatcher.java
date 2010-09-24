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
import com.sun.max.vm.type.*;

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

    @Override
    public void run() {
        Hashtable handlers = (Hashtable) ClassRegistry.Signal_handlers.get(null);
        Hashtable signals = (Hashtable) ClassRegistry.Signal_signals.get(null);
        nativeSignalInit(tryPostSignal.address());
        started = true;

        while (true) {
            int signal = waitForSignal();

            if (signal == ExitSignal) {
                nativeSignalFinalize();
                return;
            }

            final Signal sig = (Signal) signals.get(Integer.valueOf(signal));
            final SignalHandler handler = (SignalHandler) handlers.get(sig);
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
