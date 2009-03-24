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
import com.sun.max.vm.monitor.modal.sync.nat.*;
import com.sun.max.vm.thread.*;

/**
 * An extension of {@link StandardJavaMonitor} to allow proxy acquirability. That is,
 * it allows a thread to acquire a StandardJavaMonitor on behalf of another thread.
 *
 * For example, if we consider a thin-locking scheme:
 *
 * If Thread t1 has thin locked Object obj, and Thread t2 wants it too,
 * then we must inflate the lock.
 *
 * So the state we want after the inflation is:
 *
 * - t1 owns obj.JavaMonitor (which implies that obj.JavaMonitor's native
 * mutex is owned by t1's nativeThread.)
 *
 * - t2 is blocked on obj.JavaMonitor (which implies that t2's nativeThread
 * is blocked on obj.JavaMonitor's native mutex).
 *
 * If t2 is the thread that performs the inflation, i.e. it removes a
 * JavaMonitor from the free list, and CAS's it into obj's lockword, then
 * how do we fix-up the native state to match the logical Java state?
 *
 * ProxyAcquirableMonitor allows this sort of thing to happen. I.e. it
 * allows a monitor (both the java and native components) to be acquired on
 * behalf another thread).
 *
 * So taking the above example, t2 takes a ProxyAcquirableJavaMonitor, m,
 * from the free list, and sets m._ownerThread to t1, m._rcount to
 * obj.ThinLock's current lock count, and m._ownerAcquired to false. The
 * Java-side state is correct, but the native-side looks nothing like what
 * we want; so m._ownerAcquired only gets set to true when the native-side
 * is setup. Never-the-less, t2 can now CAS's obj's lockword to point to m,
 * and the lock is now inflated, so it just calls m.monitorEnter() as normal.
 *
 * Looking at ProxyAcquirableJavaMonitor monitor operations, they are
 * arranged thus:
 * <pre>
 *    public void monitorEnter() {
 *        if (!_ownerAcquired) {
 *            ownerAcquire();
 *        }
 *        super.monitorEnter();
 *    }
 * </pre>
 * So until the native side is correct, all threads, including t1, t2 or
 * anybody else that wants to do something with the monitor gets diverted to
 * ownerAcquire().
 * This is guarded by _proxyMutex, which is a single native mutex
 * shared by all ProxyAcquirableJavaMonitors. When t2 calls ownerAcquire(),
 * it grabs this lock, checks if the native state is setup; if not it
 * checks to see if it is the owner (java-side) of the monitor, it is not,
 * so it blocks on native condition variable (again, shared by all
 * ProxyAcquirableJavaMonitors), and releases _proxyMutex.
 *
 * The first time t1 touches the lock after the inflation it calls
 * ownerAcquire(). It is the Java-side owner of the monitor, so it acquires
 * the real native _mutex of the monitor (not the shared one, but
 * obj.JavaMonitor's); at this point the native-side is correct, so it sets
 * _ownerAcquired to true, and notifies the native condition variable (the
 * shared one) which t2 is waiting on. It then releases the shared native
 * _proxyMutex. At this point t2 will compete to get the shared _proxyMutex
 * and come off the shared waiting queue. Note although threads from other
 * ProxyAcquirableJavaMonitors will be woken up, they all check the local
 * _ownerAcquired field to see if it's their ProxyAcquirableJavaMonitor
 * which has been setup natively.
 *
 * So we are guaranteed to eventually get to both the Java and native state
 * that we require.
 * @author Simon Wilkinson
 */
class ProxyAcquirableJavaMonitor extends StandardJavaMonitor {

    private static final Mutex _proxyMutex = MutexFactory.create();
    private static final ConditionVariable _proxyVar = ConditionVariableFactory.create();

    private volatile boolean _ownerAcquired;

    private void ownerAcquire() {
        final VmThread currentThread = VmThread.current();
        _proxyMutex.lock();
        if (!_ownerAcquired) {
            if (currentThread == _ownerThread) {
                _mutex.lock();
                _ownerAcquired = true;
                _proxyVar.threadNotify(true);
            } else {
                while (!_ownerAcquired) {
                    currentThread.setState(Thread.State.BLOCKED);
                    _proxyVar.threadWait(_proxyMutex, 0);
                    currentThread.setState(Thread.State.RUNNABLE);
                }
            }
        }
        _proxyMutex.unlock();
    }

    @Override
    public void monitorEnter() {
        if (!_ownerAcquired) {
            ownerAcquire();
        }
        super.monitorEnter();
    }

    @Override
    public void monitorExit() {
        if (!_ownerAcquired) {
            ownerAcquire();
        }
        super.monitorExit();
    }

    @Override
    public void monitorWait(long timeoutMilliSeconds) throws InterruptedException {
        if (!_ownerAcquired) {
            ownerAcquire();
        }
        super.monitorWait(timeoutMilliSeconds);
    }

    @Override
    public void monitorNotify(boolean all) {
        if (!_ownerAcquired) {
            ownerAcquire();
        }
        super.monitorNotify(all);
    }

    @Override
    public void monitorPrivateAcquire(VmThread owner, int lockQty) {
        _ownerThread = owner;
        _recursionCount = lockQty;
        _ownerAcquired = false;
        _bindingProtection = BindingProtection.PROTECTED;
    }

    @Override
    public void monitorPrivateRelease() {
        _ownerThread = null;
        _recursionCount = 0;
        _ownerAcquired = true;
        _bindingProtection = BindingProtection.UNPROTECTED;
    }

    @Override
    public void allocate() {
        super.allocate();
        _proxyMutex.init();
        _proxyVar.init();
    }

    @Override
    public final void reset() {
        super.reset();
        _ownerAcquired = false;
    }

    @Override
    public void dump() {
        super.dump();
        Log.print(" ownerAcquired=");
        Log.print(_ownerAcquired);
        Log.print(" proxyMutex=");
        Log.print(_proxyMutex.logId());
        Log.print(" proxyCondVar=");
        Log.print(_proxyVar.logId());
    }
}
