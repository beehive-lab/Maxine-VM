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
package com.sun.max.vm.monitor.modal.sync;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Provides Java monitor services on behalf of a {@linkplain #boundObject() bound} object.
 *
 * The {@link Bytecode#MONITORENTER} and {@link Bytecode#MONITOREXIT} instructions are implemented via a per-monitor
 * mutex. {@linkplain Object#wait() Wait} and {@linkplain Object#notify() notify} are implemented via a per-monitor
 * waiting list and a per-thread {@linkplain VmThread#waitingCondition() condition variable} on which a thread suspends
 * itself. A per-thread condition variable is necessary in order to implement single thread notification.
 *
 * @author Simon Wilkinson
 */
public class StandardJavaMonitor extends AbstractJavaMonitor {

    protected final Mutex mutex;
    private VmThread waitingThreads;

    public StandardJavaMonitor() {
        mutex = MutexFactory.create();
    }

    private static void raiseIllegalMonitorStateException(VmThread owner) {
        if (owner == null) {
            throw new IllegalMonitorStateException();
        }
        throw new IllegalMonitorStateException("Monitor owned by thread \"" + owner.getName() + "\" [id=" + owner.id() + "]");
    }

    @Override
    public void monitorEnter() {
        final VmThread currentThread = VmThread.current();
        traceStartMonitorEnter(currentThread);
        if (ownerThread == currentThread) {
            recursionCount++;
            traceEndMonitorEnter(currentThread);
            return;
        }
        currentThread.setState(Thread.State.BLOCKED);
        mutex.lock();
        currentThread.setState(Thread.State.RUNNABLE);
        ownerThread = currentThread;
        setBindingProtection(BindingProtection.PROTECTED);
        recursionCount = 1;
        traceEndMonitorEnter(currentThread);
    }

    @Override
    public void monitorExit() {
        final VmThread currentThread = VmThread.current();
        traceStartMonitorExit(currentThread);
        if (ownerThread != currentThread) {
            raiseIllegalMonitorStateException(ownerThread);
        }
        if (--recursionCount == 0) {
            ownerThread = null;
            if (waitingThreads == null) {
                setBindingProtection(BindingProtection.UNPROTECTED);
            }
            traceEndMonitorExit(currentThread);
            mutex.unlock();
        }
    }

    @Override
    public void monitorWait(long timeoutMilliSeconds) throws InterruptedException {
        final VmThread currentThread = VmThread.current();
        traceStartMonitorWait(currentThread);
        if (ownerThread != currentThread) {
            raiseIllegalMonitorStateException(ownerThread);
        }
        final int recursionCount = this.recursionCount;
        final VmThread ownerThread = this.ownerThread;
        if (timeoutMilliSeconds == 0L) {
            ownerThread.setState(Thread.State.WAITING);
        } else {
            ownerThread.setState(Thread.State.TIMED_WAITING);
        }

        final ConditionVariable waitingCondition = ownerThread.waitingCondition().init();
        ownerThread.setNextWaitingThread(waitingThreads);
        waitingThreads = ownerThread;
        this.ownerThread = null;
        final boolean interrupted = !waitingCondition.threadWait(mutex, timeoutMilliSeconds);
        this.ownerThread = ownerThread;
        ownerThread.setState(Thread.State.RUNNABLE);
        this.recursionCount = recursionCount;

        boolean timedOut = false;
        if (!interrupted) {
            if (ownerThread.nextWaitingThread() != ownerThread) {
                // The thread is still on the waitingThreads list: remove it
                timedOut = true;

                if (ownerThread == waitingThreads) {
                    // Common case: owner is at the head of the list
                    waitingThreads = ownerThread.nextWaitingThread();
                    ownerThread.setNextWaitingThread(ownerThread);
                } else {
                    if (waitingThreads == null) {
                        FatalError.unexpected("Thread woken from wait by timeout not in waiting threads list");
                    }
                    // Must now search the list and remove ownerThread
                    VmThread previous = waitingThreads;
                    VmThread waiter = previous.nextWaitingThread();
                    while (waiter != ownerThread) {
                        if (waiter == null) {
                            FatalError.unexpected("Thread woken from wait by timeout not in waiting threads list");
                        }
                        previous = waiter;
                        waiter = waiter.nextWaitingThread();
                    }
                    // ownerThread
                    previous.setNextWaitingThread(ownerThread.nextWaitingThread());
                    ownerThread.setNextWaitingThread(ownerThread);
                }
            }
        }

        traceEndMonitorWait(currentThread, interrupted, timedOut);

        if (interrupted || this.ownerThread.isInterrupted(true)) {
            throw new InterruptedException();
        }
    }

    @Override
    public void monitorNotify(boolean all) {
        final VmThread currentThread = VmThread.current();
        traceStartMonitorNotify(currentThread);
        if (ownerThread != currentThread) {
            raiseIllegalMonitorStateException(ownerThread);
        }
        if (all) {
            VmThread waiter = waitingThreads;
            while (waiter != null) {
                waiter.setState(Thread.State.BLOCKED);
                waiter.waitingCondition().threadNotify(false);
                final VmThread previous = waiter;
                waiter = waiter.nextWaitingThread();

                // This is the idiom for indicating that the thread is no longer on a waiters list.
                // which in turn makes it easy to determine in 'monitorWait' if the thread was
                // notified or woke up because the timeout expired.
                previous.setNextWaitingThread(previous);
            }
            waitingThreads = null;
        } else {
            final VmThread waiter = waitingThreads;
            if (waiter != null) {
                waiter.setState(Thread.State.BLOCKED);
                waiter.waitingCondition().threadNotify(false);
                waitingThreads = waiter.nextWaitingThread();

                // See comment above.
                waiter.setNextWaitingThread(waiter);
            }
        }
        traceEndMonitorNotify(currentThread);
    }

    @Override
    public void monitorPrivateAcquire(VmThread owner, int lockQty) {
        FatalError.unexpected("Cannot perform a private monitor acquire from a " + this.getClass().getName());
    }

    @Override
    public void monitorPrivateRelease() {
        FatalError.unexpected("Cannot perform a private monitor release from a " + this.getClass().getName());
    }

    @Override
    public void allocate() {
        mutex.init();
    }

    @Override
    public void log() {
        super.log();
        Log.print(" mutex=");
        Log.print(Address.fromLong(mutex.logId()));
        Log.print(" waiters={");
        VmThread waiter = waitingThreads;
        while (waiter != null) {
            Log.print(waiter.getName());
            Log.print(" ");
            waiter = waiter.nextWaitingThread();
        }
        Log.print("}");
    }
}
