/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.thread.*;

/**
 * An extension of {@link StandardJavaMonitor} to allow proxy acquirability. That is,
 * it allows a thread to acquire a monitor on behalf of another thread.
 *
 * For example, if we consider a thin-locking scheme:
 *
 * If thread {@code t1} has thin locked object {@code obj}, and thread {@code t2} wants it too,
 * then the lock must be inflated. The desired state after inflation is:
 * <ul>
 * <li>{@code t1} owns {@code m} ({@code obj}'s monitor) and the native
 * mutex associated with {@code m} is owned by {@code t1}'s native thread.</li>
 * <li>{@code t2} is blocked on {@code m} and {@code t2}'s native thread
 * is blocked on {@code m}'s native mutex.</li>
 * </ul>
 *
 * If {@code t2} is the thread that performs the inflation, i.e. it removes a
 * monitor from the free list, and CAS's it into {@code obj}'s lockword, then
 * the native state needs to be modified to match the logical Java state.
 * <p>
 * {@link ProxyAcquirableJavaMonitor} allows this sort of thing to happen. That is, it
 * allows a monitor (both the Java and native components) to be acquired on
 * behalf another thread).
 * <p>
 * Taking the above example, {@code t2} takes a {@code ProxyAcquirableJavaMonitor}, {@code m},
 * from the free list, and sets {@code m.ownerThread} to {@code t1}, {@code m.recursionCount} to
 * current lock count in {@code obj}'s thin lock and {@code m.ownerAcquired} to false. The
 * Java-side state is now correct, but the native-side still requires fixing;
 * {@code m.ownerAcquired} only gets set to true when the native-side
 * is fixed. Never-the-less, {@code t2} can now CAS's {@code obj}'s lockword to point to {@code m}
 * and the lock is now inflated, so it just calls {@link #monitorEnter()} on {@code m}.
 *
 * Looking at {@code ProxyAcquirableJavaMonitor} monitor operations, they are
 * arranged thus:
 * <pre>
 *    public void monitorEnter() {
 *        if (!ownerAcquired) {
 *            ownerAcquire();
 *        }
 *        super.monitorEnter();
 *    }
 * </pre>
 * This ensures that until the native side is correct, all threads, including {@code t1}, {@code t2} or
 * anybody else that wants to do something with the monitor gets diverted to {@link #ownerAcquire()}.
 * This is guarded by {@link #proxyMutex}, a global native mutex.
 * When {@code t2} calls {@link #ownerAcquire()},
 * it grabs this lock, checks if the native state is setup; if not it
 * checks to see if it is the owner (Java-side) of the monitor, it is not,
 * so it blocks on {@link #proxyVar}, a global native condition variable,
 * and releases {@code proxyMutex}.
 * <p>
 * The first time {@code t1} touches the lock after the inflation it calls
 * {@code ownerAcquire()}. It finds it is the Java-side owner of the monitor
 * and acquires the native mutex of the monitor (not the shared one, but
 * the one associated with {@code m}. At this point the native-side is correct, so
 * {@code m.ownerAcquired} is set to true, and {@code proxyVar} is notified (waking up
 * {@code t2} as a result). Thread {@code t1} then releases {@code proxyMutex}.
 * At this point {@code t2} will compete for {@code proxyMutex}
 * and come off the associated wait queue. Note although threads from other
 * {@code ProxyAcquirableJavaMonitor}s will also be woken up, they all check the
 * {@code ownerAcquired} field to see if it's their {@code ProxyAcquirableJavaMonitor}
 * which has been setup natively.
 * <p>
 * The above protocol thus guarantees both the Java and native state of a proxy acquirable
 * are eventual correct.
 *
 * @author Simon Wilkinson
 */
class ProxyAcquirableJavaMonitor extends StandardJavaMonitor {

    private static final Mutex proxyMutex = MutexFactory.create();
    private static final ConditionVariable proxyVar = ConditionVariableFactory.create();

    private volatile boolean ownerAcquired;

    private void ownerAcquire() {
        final VmThread currentThread = VmThread.current();
        proxyMutex.lock();
        if (!ownerAcquired) {
            if (currentThread == ownerThread) {
                mutex.lock();
                ownerAcquired = true;
                proxyVar.threadNotify(true);
            } else {
                while (!ownerAcquired) {
                    currentThread.setState(Thread.State.BLOCKED);
                    proxyVar.threadWait(proxyMutex, 0);
                    currentThread.setState(Thread.State.RUNNABLE);
                }
            }
        }
        proxyMutex.unlock();
    }

    @Override
    public void monitorEnter() {
        if (!ownerAcquired) {
            ownerAcquire();
        }
        super.monitorEnter();
    }

    @Override
    public void monitorExit() {
        if (!ownerAcquired) {
            ownerAcquire();
        }
        super.monitorExit();
    }

    @Override
    public void monitorWait(long timeoutMilliSeconds) throws InterruptedException {
        if (!ownerAcquired) {
            ownerAcquire();
        }
        super.monitorWait(timeoutMilliSeconds);
    }

    @Override
    public void monitorNotify(boolean all) {
        if (!ownerAcquired) {
            ownerAcquire();
        }
        super.monitorNotify(all);
    }

    @Override
    public void monitorPrivateAcquire(VmThread owner, int lockQty) {
        ownerThread = owner;
        recursionCount = lockQty;
        ownerAcquired = false;
        setBindingProtection(BindingProtection.PROTECTED);
    }

    @Override
    public void monitorPrivateRelease() {
        ownerThread = null;
        recursionCount = 0;
        ownerAcquired = true;
        setBindingProtection(BindingProtection.UNPROTECTED);
    }

    @Override
    public void allocate() {
        super.allocate();
        proxyMutex.init();
        proxyVar.init();
    }

    @Override
    public final void reset() {
        super.reset();
        ownerAcquired = false;
    }

    @Override
    public void log() {
        super.log();
        Log.print(" ownerAcquired=");
        Log.print(ownerAcquired);
        Log.print(" proxyMutex=");
        Log.print(Address.fromLong(proxyMutex.logId()));
        Log.print(" proxyCondVar=");
        Log.print(Address.fromLong(proxyVar.logId()));
    }
}
