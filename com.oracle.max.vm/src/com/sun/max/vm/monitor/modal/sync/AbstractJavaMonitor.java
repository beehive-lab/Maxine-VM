/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.annotate.*;
import com.sun.max.atomic.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.monitor.modal.modehandlers.inflated.*;
import com.sun.max.vm.monitor.modal.sync.JavaMonitorManager.ManagedMonitor;
import com.sun.max.vm.monitor.modal.sync.JavaMonitorManager.VmLock;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Base class for {@code JavaMonitor}'s managed by {@code JavaMonitorManager}.
 */
abstract class AbstractJavaMonitor implements ManagedMonitor {

    private Object boundObject;
    protected volatile VmThread ownerThread;
    protected int recursionCount;
    private final AtomicWord displacedMiscWord = new AtomicWord();

    protected BindingProtection bindingProtection;
    private Word preGCLockword;

    // Support for direct linked lists of JavaMonitors.
    private ManagedMonitor next;

    protected AbstractJavaMonitor() {
        bindingProtection = BindingProtection.PRE_ACQUIRE;
    }

    public abstract void monitorEnter();

    public abstract void monitorExit();

    public abstract void monitorWait(long timeoutMilliSeconds) throws InterruptedException;

    public abstract void monitorNotify(boolean all);

    public abstract void monitorPrivateAcquire(VmThread owner, int lockQty);

    public abstract void monitorPrivateRelease();

    public abstract void allocate();

    public final boolean isOwnedBy(VmThread thread) {
        return ownerThread == thread;
    }

    public final Word displacedMisc() {
        return displacedMiscWord.get();
    }

    public final void setDisplacedMisc(Word lockword) {
        displacedMiscWord.set(lockword);
    }

    public final Word compareAndSwapDisplacedMisc(Word expectedValue, Word newValue) {
        return displacedMiscWord.compareAndSwap(expectedValue, newValue);
    }

    public void reset() {
        boundObject = null;
        ownerThread = null;
        recursionCount = 0;
        displacedMiscWord.set(Word.zero());
        preGCLockword = Word.zero();
        bindingProtection = BindingProtection.PRE_ACQUIRE;
    }

    public final void setBoundObject(Object object) {
        boundObject = object;
    }

    public final Object boundObject() {
        return boundObject;
    }

    public final boolean isBound() {
        return boundObject != null;
    }

    public final boolean isHardBound() {
        return isBound() && ObjectAccess.readMisc(boundObject).equals(InflatedMonitorLockword64.boundFromMonitor(this));
    }

    public final void preGCPrepare() {
        preGCLockword = InflatedMonitorLockword64.boundFromMonitor(this);
    }

    public final boolean requiresPostGCRefresh() {
        return isBound() && ObjectAccess.readMisc(boundObject).equals(preGCLockword);
    }

    public final void refreshBoundObject() {
        ObjectAccess.writeMisc(boundObject, InflatedMonitorLockword64.boundFromMonitor(this));
    }

    public final BindingProtection bindingProtection() {
        checkProtection();
        return bindingProtection;
    }

    public final void setBindingProtection(BindingProtection bindingProtection) {
        this.bindingProtection = bindingProtection;
        checkProtection();
    }

    /**
     * Ensures that this monitor is protected if it is bound to a thread.
     */
    protected void checkProtection() {
        if (bindingProtection != BindingProtection.PROTECTED && ownerThread != null) {
            Log.lock();
            Log.print("Unprotected monitor with non-null owner thread: ");
            log();
            Log.println();
            FatalError.unexpected("Monitor cannot be unprotected if it is held by a thread");
        }
    }

    @INLINE
    public final ManagedMonitor next() {
        return next;
    }

    @INLINE
    public final void setNext(ManagedMonitor next) {
        this.next = next;
    }

    public void log() {
        Log.print(ObjectAccess.readClassActor(this).name.string);
        Log.print(" boundTo=");
        if (boundObject() == null) {
            Log.print("null");
        } else {
            ClassActor classActor = ObjectAccess.readClassActor(boundObject());
            Log.print(classActor.name.string);
            if (classActor == VmLock.ACTOR) {
                Log.print(" name=");
                Log.print(VmLock.asVmLock(boundObject()).name);
            }
        }
        Log.print(" owner=");
        Log.print(ownerThread == null ? "null" : ownerThread.getName());
        Log.print(" recursion=");
        Log.print(recursionCount);
        Log.print(" binding=");
        Log.print(bindingProtection.name());
    }

    protected void traceStartMonitorEnter(final VmThread currentThread) {
        if (Monitor.TraceMonitors) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Acquiring monitor for ");
            Log.print(currentThread.getName());
            Log.print(" [");
            log();
            Log.println("]");
            Log.unlock(lockDisabledSafepoints);
        }
    }

    protected void traceEndMonitorEnter(final VmThread currentThread) {
        if (Monitor.TraceMonitors) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Acquired monitor for ");
            Log.print(currentThread.getName());
            Log.print(" [");
            log();
            Log.println("]");
            Log.unlock(lockDisabledSafepoints);
        }
    }

    protected void traceStartMonitorExit(final VmThread currentThread) {
        if (Monitor.TraceMonitors) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Releasing monitor for ");
            Log.print(currentThread.getName());
            Log.print(" [");
            log();
            Log.println("]");
            Log.unlock(lockDisabledSafepoints);
        }
    }

    protected void traceEndMonitorExit(final VmThread currentThread) {
        if (Monitor.TraceMonitors) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Released monitor for ");
            Log.print(currentThread.getName());
            Log.print(" [");
            log();
            Log.println("]");
            Log.unlock(lockDisabledSafepoints);
        }
    }

    protected void traceStartMonitorWait(final VmThread currentThread) {
        if (Monitor.TraceMonitors) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Start wait on monitor for ");
            Log.print(currentThread.getName());
            Log.print(" [");
            log();
            Log.println("]");
            Log.unlock(lockDisabledSafepoints);
        }
    }

    protected void traceEndMonitorWait(final VmThread currentThread, final boolean interrupted, boolean timedOut) {
        if (Monitor.TraceMonitors) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("End wait on monitor for ");
            Log.print(currentThread.getName());
            Log.print(" [");
            log();
            if (interrupted) {
                Log.print(" *interrupted*");
            } else if (timedOut) {
                Log.print(" *timed-out*");
            }
            Log.println("]");
            Log.unlock(lockDisabledSafepoints);
        }
    }

    protected void traceEndMonitorNotify(final VmThread currentThread) {
        if (Monitor.TraceMonitors) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("End notify monitor for ");
            Log.print(currentThread.getName());
            Log.print(" [");
            log();
            Log.println("]");
            Log.unlock(lockDisabledSafepoints);
        }
    }

    protected void traceStartMonitorNotify(final VmThread currentThread) {
        if (Monitor.TraceMonitors) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Start notify monitor for ");
            Log.print(currentThread.getName());
            Log.print(" [");
            log();
            Log.println("]");
            Log.unlock(lockDisabledSafepoints);
        }
    }
}
