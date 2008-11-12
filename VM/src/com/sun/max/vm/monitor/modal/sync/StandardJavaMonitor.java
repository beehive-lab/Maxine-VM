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

import com.sun.max.vm.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.monitor.modal.sync.nat.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * @author Simon Wilkinson
 */
public class StandardJavaMonitor extends AbstractJavaMonitor {

    protected final Mutex _mutex;
    private VmThread _waitingThreads;

    public StandardJavaMonitor() {
        _mutex = new Mutex();
    }

    private static void raiseIllegalMonitorStateException(VmThread owner) {
        if (owner == null) {
            throw new IllegalMonitorStateException();
        }
        throw new IllegalMonitorStateException("Monitor owned by thread \"" + owner.getName() + "\" [serial=" + owner.serial() + "]");
    }

    @Override
    public void monitorEnter() {
        final VmThread currentThread = VmThread.current();
        if (Monitor.traceMonitors()) {
            final boolean lockDisabledSafepoints = Debug.lock();
            Debug.print("Acquiring monitor for ");
            Debug.print(currentThread.getName());
            Debug.print(" [");
            dump();
            Debug.println("]");
            Debug.unlock(lockDisabledSafepoints);
        }
        if (_ownerThread == currentThread) {
            _recursionCount++;
            if (Monitor.traceMonitors()) {
                final boolean lockDisabledSafepoints = Debug.lock();
                Debug.print("Acquired monitor for ");
                Debug.print(currentThread.getName());
                Debug.print(" [");
                dump();
                Debug.println("]");
                Debug.unlock(lockDisabledSafepoints);
            }
            return;
        }
        currentThread.setState(Thread.State.BLOCKED);
        _mutex.lock();
        currentThread.setState(Thread.State.RUNNABLE);
        _bindingProtection = BindingProtection.PROTECTED;
        _ownerThread = currentThread;
        _recursionCount = 1;
        if (Monitor.traceMonitors()) {
            final boolean lockDisabledSafepoints = Debug.lock();
            Debug.print("Acquired monitor for ");
            Debug.print(currentThread.getName());
            Debug.print(" [");
            dump();
            Debug.println("]");
            Debug.unlock(lockDisabledSafepoints);
        }
    }

    @Override
    public void monitorExit() {
        final VmThread currentThread = VmThread.current();
        if (Monitor.traceMonitors()) {
            final boolean lockDisabledSafepoints = Debug.lock();
            Debug.print("Releasing monitor for ");
            Debug.print(currentThread.getName());
            Debug.print(" [");
            dump();
            Debug.println("]");
            Debug.unlock(lockDisabledSafepoints);
        }
        if (_ownerThread != currentThread) {
            raiseIllegalMonitorStateException(_ownerThread);
        }
        if (--_recursionCount == 0) {
            _bindingProtection = BindingProtection.UNPROTECTED;
            _ownerThread = null;
            _mutex.unlock();
            if (Monitor.traceMonitors()) {
                final boolean lockDisabledSafepoints = Debug.lock();
                Debug.print("Released monitor for ");
                Debug.print(currentThread.getName());
                Debug.print(" [");
                dump();
                Debug.println("]");
                Debug.unlock(lockDisabledSafepoints);
            }
        }
    }

    @Override
    public void monitorWait(long timeoutMilliSeconds) throws InterruptedException {
        if (_ownerThread != VmThread.current()) {
            raiseIllegalMonitorStateException(_ownerThread);
        }
        final int rcount = _recursionCount;
        final VmThread ownerThread = _ownerThread;
        if (timeoutMilliSeconds == 0L) {
            _ownerThread.setState(Thread.State.WAITING);
        } else {
            _ownerThread.setState(Thread.State.TIMED_WAITING);
        }

        final ConditionVariable waitingCondition  = _ownerThread.waitingCondition();
        if (waitingCondition.requiresAllocation()) {
            waitingCondition.allocate();
        }
        _ownerThread.setNextWaitingThread(_waitingThreads);
        _waitingThreads = _ownerThread;
        _ownerThread = null;
        final boolean interrupted = ownerThread.waitingCondition().threadWait(_mutex, timeoutMilliSeconds);
        _ownerThread = ownerThread;
        _ownerThread.setState(Thread.State.RUNNABLE);
        _recursionCount = rcount;

        if (interrupted) {
            // Clear thread's interrupted flag
            _ownerThread.setInterrupted();
            throw new InterruptedException();
        }
    }

    @Override
    public void monitorNotify(boolean all) {
        if (_ownerThread != VmThread.current()) {
            raiseIllegalMonitorStateException(_ownerThread);
        }
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
        _mutex.alloc();
    }

    @Override
    public void dump() {
        super.dump();
        Debug.print(" mutex=");
        Debug.print(_mutex.asPointer());
        Debug.print(" waiters={");
        VmThread waiter = _waitingThreads;
        while (waiter != null) {
            Debug.print(waiter.getName());
            Debug.print(" ");
            waiter = waiter.nextWaitingThread();
        }
        Debug.print("}");
    }

    static class VMThreadMapJavaMonitor extends StandardJavaMonitor {

        @Override
        public void monitorEnter() {
            final VmThread currentThread = VmThread.current();
            if (currentThread.state() == Thread.State.TERMINATED) {
                assert _ownerThread != currentThread;
                _mutex.lock();
                _ownerThread = currentThread;
                _recursionCount = 1;
            } else {
                super.monitorEnter();
            }
        }
    }

    static class HeapSchemeDeadlockDetectionJavaMonitor extends StandardJavaMonitor {

        private boolean _elideForDeadlockStackTrace = false;

        @Override
        public void monitorEnter() {
            final VmThread currentThread = VmThread.current();
            if (MaxineVM.hostOrTarget().configuration().heapScheme().isGcThread(currentThread)) {
                if (currentThread.waitingCondition() == null) {
                    // This is the GC thread creating its private waiting condition variable.
                    // This done at vm boot so no deadlock risk.
                } else if (_elideForDeadlockStackTrace) {
                    // Pretend the GC thread has acquired the lock so that we can allocate if necessary while dumping its stack.
                    return;
                } else {
                    heapSchemeDeadlock();
                }
            }
            super.monitorEnter();
        }

        private void heapSchemeDeadlock() throws FatalError {
            Debug.println("WARNING : GC thread is going for the HeapScheme lock. Trying to allocate?");
            Debug.println("WARNING : Eliding HeapScheme lock for GC thread and attempting stack trace...");
            DebugBreak.here();
            _elideForDeadlockStackTrace = true;
            throw FatalError.unexpected("GC thread is attempting to allocate. Attempting stack trace.");
        }

        @Override
        public void monitorExit() {
            if (_elideForDeadlockStackTrace) {
                // Pretend the GC thread has released the lock so that we can allocate if necessary while dumping its stack.
                return;
            }
            super.monitorExit();
        }
    }
}
