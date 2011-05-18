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
package com.sun.max.vm.monitor.modal.sync;

import com.sun.cri.bytecode.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Provides Java monitor services on behalf of a {@linkplain #boundObject() bound} object.
 *
 * The {@link Bytecodes#MONITORENTER} and {@link Bytecodes#MONITOREXIT} instructions are implemented via a per-monitor
 * mutex. {@linkplain Object#wait() Wait} and {@linkplain Object#notify() notify} are implemented via a per-monitor
 * waiting list and a per-thread {@linkplain VmThread#waitingCondition() condition variable} on which a thread suspends
 * itself. A per-thread condition variable is necessary in order to implement single thread notification.
 */
public class StandardJavaMonitor extends AbstractJavaMonitor {

    protected final Mutex mutex;

    /**
     * The list of threads waiting on this monitor as a result of a call to
     * {@link #monitorWait(long)}. A thread is responsible
     * for adding/removing itself to/from this list on either side of the call
     * to {@link ConditionVariable#threadWait(Mutex, long)}. A
     * {@linkplain #monitorNotify(boolean) notifying}
     * thread traverses the list but does not modify it. This means a monitor
     * will stay protected in the short window between a notifying thread
     * releasing it and one of the waiting threads reacquiring it upon wake up.
     */
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
            } else {
                // If there are waiting threads that have not yet been woken
                // then this monitor must stay protected.
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
        ownerThread.nextWaitingThread = waitingThreads;
        waitingThreads = ownerThread;
        this.ownerThread = null;
        final boolean interrupted;
        if (ownerThread.isInterrupted(true)) {
            // The wait is prematurely interrupted and never calls native code
            interrupted = true;
        } else {
            waitingCondition.threadWait(mutex, timeoutMilliSeconds);
            interrupted = ownerThread.isInterrupted(true);
        }

        this.ownerThread = ownerThread;
        checkProtection();
        final boolean timedOut = ownerThread.state() == Thread.State.TIMED_WAITING && !interrupted;
        ownerThread.setState(Thread.State.RUNNABLE);
        this.recursionCount = recursionCount;

        FatalError.check(waitingThreads != null && ownerThread.isOnWaitersList(), "Thread woken from wait not in waiting threads list");

        // Remove the thread from the waitingThreads list
        if (ownerThread == waitingThreads) {
            // Common case: owner is at the head of the list
            waitingThreads = ownerThread.nextWaitingThread;
            ownerThread.unlinkFromWaitersList();
        } else {
            // Must now search the list and remove ownerThread
            VmThread previous = waitingThreads;
            VmThread waiter = previous.nextWaitingThread;
            while (waiter != ownerThread) {
                if (waiter == null) {
                    FatalError.unexpected("Thread woken from wait not in waiting threads list");
                }
                previous = waiter;
                waiter = waiter.nextWaitingThread;
            }
            // ownerThread
            previous.nextWaitingThread = ownerThread.nextWaitingThread;
            ownerThread.unlinkFromWaitersList();
        }

        traceEndMonitorWait(currentThread, interrupted, timedOut);

        if (interrupted) {
            // turn off interrupted status
            this.ownerThread.isInterrupted(true);
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
                waiter = waiter.nextWaitingThread;
            }
        } else {
            final VmThread waiter = waitingThreads;
            if (waiter != null) {
                waiter.setState(Thread.State.BLOCKED);
                waiter.waitingCondition().threadNotify(false);
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
            waiter = waiter.nextWaitingThread;
        }
        Log.print("}");
    }
}
