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
 * itself. A per-thread condition variable is necessary in order to implement single thread notification. <br>
 * <br>
 * Note: This implementation does not conform completely with the Java language specification of wait(). The specification
 * (see the JavaDoc of {@link Object#wait(long)} states the following protocol for continuing execution after a timeout,
 * interruption, or notify: <br>
 * "The thread T is then removed from the wait set for this object and re-enabled for thread scheduling. It then
 * competes in the usual manner with other threads for the right to synchronize on the object" <br>
 * In the case of wait timeouts and interruptions, this class reverses the order: First, all the synchronizations are
 * restored (which can take a while because another thread might own the lock), and then T is removed from the wait set.
 * This is due to the underlying use of pthreads (on Linux) and the use of convenient functions such as
 * pthread_cond_timedwait(). This function does not return until the lock is re-acquired, so there is no place where we
 * can modify our waitingThreads list. <br>
 * Calls to notify() and notifyAll() fulfill the specification: Here, the waitingThreads list is changed by the thread
 * that issues the notify, i.e., the notified thread is removed from the list. The remaining part of wait() only has to
 * re-acquire the monitor. <br>
 * <br>
 * Example of a situation that does not fulfill the specification: Two threads are waiting for a lock. Thread A uses a
 * wait with a timeout of 1 second, thread B a wait without a timeout. Thread C is holding the lock for 5 seconds (i.e.
 * the wait of A expires in between) and then issues a notify(). According to the specification, the notify cannot hit
 * thread A because thread A was removed from the wait set when its timeout expired. So the notify wakes up thread B and
 * all threads can continue. With this implementation, the notify can hit thread A since it could not re-acquire the
 * lock between the timeout and the notify (remember that thread C holds the lock). So the notify does not wake up
 * thread B, and it sleeps forever - thread B remains blocked forever.
 */
public class StandardJavaMonitor extends AbstractJavaMonitor {

    protected final Mutex mutex;

    /**
     * The list of threads waiting on this monitor as a result of a call to {@link #monitorWait(long)}. A thread is
     * responsible for adding/removing itself to/from this list on either side of the call to
     * {@link ConditionVariable#threadWait(Mutex, long)}. A {@linkplain #monitorNotify(boolean) notifying} thread also
     * modifies this list, i.e., the waiting thread only has to remove itself from the list when no notify occurred. In
     * order to have keep the monitor protected in the short window between a notifying thread releasing it and one of
     * the waiting threads re-acquiring it upon wake up, the field {@link #notifiedThreads} is > 0 in this timeframe.
     */
    private VmThread waitingThreads;

    private int notifiedThreads;

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
            if (waitingThreads == null && notifiedThreads == 0) {
                setBindingProtection(BindingProtection.UNPROTECTED);
            } else {
                // If there are waiting threads that have not yet been woken
                // (or threads that have been notified but not yet resumed)
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

        if (ownerThread.isOnWaitersList()) {
            removeFromWaitingList(ownerThread, null);
        } else {
            assert notifiedThreads > 0;
            notifiedThreads--;
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
            while (waitingThreads != null) {
                notifyAndRemove(waitingThreads, null);
            }
        } else if (waitingThreads != null) {
            // Notify the last thread of the waiting threads list, i.e., the thread that has been waiting longest.
            // We cannot notify the first thread of the list because some poorly programmed concurrent benchmarks
            // are deadlocking in this case: Notify always wakes up a monitoring thread that calls wait immediately,
            // and the threads actually performing a workload are never notified and thus starved.
            VmThread prev = null;
            VmThread cur = waitingThreads;
            while (cur.nextWaitingThread != null) {
                prev = cur;
                cur = cur.nextWaitingThread;
            }
            notifyAndRemove(cur, prev);
        }
        traceEndMonitorNotify(currentThread);
    }

    private void notifyAndRemove(VmThread waiter, VmThread previous) {
        removeFromWaitingList(waiter, previous);
        notifiedThreads++;
        waiter.setState(Thread.State.BLOCKED);
        waiter.waitingCondition().threadNotify(false);
    }

    /**
     * Remove a thread from the waitingThreads list. If the previous thread in the list is specified, then
     * the removal is performed in constant time.  Otherwise, the whole list of waiting threads might have
     * to be searched.
     *
     * @param toRemove The thread to be removed from the list.
     * @param previous The previous thread, if available.
     */
    private void removeFromWaitingList(VmThread toRemove, VmThread previous) {
        if (toRemove == waitingThreads) {
            // Common case: remove the head of the list
            waitingThreads = toRemove.nextWaitingThread;
            toRemove.unlinkFromWaitersList();
        } else {
            if (previous == null) {
                // Must now search the list and remove thread
                previous = waitingThreads;
                VmThread waiter = previous.nextWaitingThread;
                while (waiter != toRemove) {
                    if (waiter == null) {
                        throw FatalError.unexpected("Thread woken from wait not in waiting threads list");
                    }
                    previous = waiter;
                    waiter = waiter.nextWaitingThread;
                }
            }
            assert previous.nextWaitingThread == toRemove;
            previous.nextWaitingThread = toRemove.nextWaitingThread;
            toRemove.unlinkFromWaitersList();
        }
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
