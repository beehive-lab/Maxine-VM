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
/*VCSID=38898e4a-6a77-4e74-a09f-a9e8599d8ad6*/
package com.sun.max.vm.monitor.modal.sync;

import com.sun.max.vm.*;
import com.sun.max.vm.debug.Debug.*;
import com.sun.max.vm.monitor.modal.sync.nat.*;
import com.sun.max.vm.thread.*;

/**
 * A slow-but-correct implementation of Java monitors.
 *
 * Monitor state is maintained in the JavaMonitor object, and protected by the mutex.
 * The mutex is not used for 'monitorEnter' blocking, rather, blocking threads wait on the
 * _blockingVar condition variable. This allows monitor ownership to be a deterministic Java-side property.
 * (Hence the current thread can easily acquire a monitor for another thread, which is necessary
 * for biased locking etc.)
 *
 * Wait and notify are implemented via a per-monitor waiting list, and a per-thread condition variable,
 * on which a thread suspends itself. We need a per-thread condition to implement single thread notification.
 *
 * @author Simon Wilkinson
 */
final class SlowProxyAcquirableJavaMonitor extends AbstractJavaMonitor {

    private VmThread _waitingThreads;
    private final Mutex _mutex;
    private final ConditionVariable _blockingVar;

    public SlowProxyAcquirableJavaMonitor() {
        _mutex = new Mutex();
        _blockingVar = new ConditionVariable();
        if (!MaxineVM.isPrototyping()) {
            alloc();
        }
    }

    @Override
    public void monitorEnter() {
        final VmThread currentThread = VmThread.current();
        if (_ownerThread == currentThread) {
            _recursionCount++;
            return;
        }
        currentThread.setState(Thread.State.BLOCKED);
        _mutex.lock();
        while (_ownerThread != null) {
            _blockingVar.threadWait(_mutex, 0);
        }
        currentThread.setState(Thread.State.RUNNABLE);
        _bindingProtection = BindingProtection.PROTECTED;
        _ownerThread = currentThread;
        _recursionCount = 1;
        _mutex.unlock();
    }

    @Override
    public void monitorExit() {
        if (_ownerThread != VmThread.current()) {
            throw new IllegalMonitorStateException();
        }
        if (--_recursionCount == 0) {
            _mutex.lock();
            _bindingProtection = BindingProtection.UNPROTECTED;
            _ownerThread = null;
            _blockingVar.threadNotify(false);
            _mutex.unlock();
        }
    }

    @Override
    public void monitorWait(long timeoutMilliSeconds) {
        if (_ownerThread != VmThread.current()) {
            throw new IllegalMonitorStateException();
        }
        _mutex.lock();
        if (_ownerThread.waitingCondition() == null) {
            final ConditionVariable condVar = new ConditionVariable();
            condVar.alloc();
            _ownerThread.setWaitingCondition(condVar);
        }
        final int rcount = _recursionCount;
        final VmThread ownerThread = _ownerThread;
        if (timeoutMilliSeconds == 0L) {
            _ownerThread.setState(Thread.State.WAITING);
        } else {
            _ownerThread.setState(Thread.State.TIMED_WAITING);
        }
        _ownerThread.setNextWaitingThread(_waitingThreads);
        _waitingThreads = _ownerThread;
        _ownerThread = null;
        _blockingVar.threadNotify(false);
        ownerThread.waitingCondition().threadWait(_mutex, timeoutMilliSeconds);
        while (_ownerThread != null) {
            // We've been notified, but someone else has the lock.
            // Go on the blocking queue.
            _blockingVar.threadWait(_mutex, 0);
        }
        _ownerThread = ownerThread;
        _ownerThread.setState(Thread.State.RUNNABLE);
        _recursionCount = rcount;
        _mutex.unlock();
    }

    @Override
    public void monitorNotify(boolean all) {
        if (_ownerThread != VmThread.current()) {
            throw new IllegalMonitorStateException();
        }
        _mutex.lock();
        if (all) {
            VmThread waiter = _waitingThreads;
            while (waiter != null) {
                waiter.setState(Thread.State.BLOCKED);
                waiter.waitingCondition().threadNotify(false);
                final VmThread previous = waiter;
                waiter = waiter.nextWaitingThread();
                previous.setNextWaitingThread(null);
            }
            _waitingThreads = null;
        } else {
            final VmThread waiter = _waitingThreads;
            if (waiter != null) {
                waiter.setState(Thread.State.BLOCKED);
                waiter.waitingCondition().threadNotify(false);
                _waitingThreads = waiter.nextWaitingThread();
                waiter.setNextWaitingThread(null);
            }
        }
        _mutex.unlock();
    }

    @Override
    public void monitorPrivateAcquire(VmThread owner, int lockQty) {
        _ownerThread = owner;
        _recursionCount = lockQty;
        _bindingProtection = BindingProtection.PROTECTED;
    }

    @Override
    public void monitorPrivateRelease() {
        _ownerThread = null;
        _recursionCount = 0;
        _bindingProtection = BindingProtection.UNPROTECTED;
    }

    @Override
    public void alloc() {
        _mutex.alloc();
        _blockingVar.alloc();
    }

    @Override
    public void dump(DebugPrintStream out) {
        super.dump(out);
        out.print(" mutex=");
        out.print(_mutex.asPointer());
        out.print(" blockingCondVar=");
        out.print(_blockingVar.asPointer());
        out.print(" waiters={");
        VmThread waiter = _waitingThreads;
        while (waiter != null) {
            out.print(waiter.getName());
            out.print(" ");
            waiter = waiter.nextWaitingThread();
        }
        out.print("}");
    }
}
