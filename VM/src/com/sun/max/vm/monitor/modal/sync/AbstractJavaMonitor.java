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

import com.sun.max.annotate.*;
import com.sun.max.atomic.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.monitor.modal.modehandlers.inflated.*;
import com.sun.max.vm.monitor.modal.sync.JavaMonitorManager.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Base class for {@code JavaMonitor}'s managed by {@code JavaMonitorManager}.
 *
 * @author Simon Wilkinson
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

    public final Word compareAndSwapDisplacedMisc(Word suspectedValue, Word newValue) {
        return displacedMiscWord.compareAndSwap(suspectedValue, newValue);
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

    private void checkProtection() {
        if (bindingProtection != BindingProtection.PROTECTED && ownerThread != null) {
            Log.print("Unprotected monitor with non-null owner thread: ");
            dump();
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

    public void dump() {
        Log.print(ObjectAccess.readClassActor(this).name.string);
        Log.print(" boundTo=");
        Log.print(boundObject() == null ? "null" : ObjectAccess.readClassActor(boundObject()).name.string);
        Log.print(" owner=");
        Log.print(ownerThread == null ? "null" : ownerThread.getName());
        Log.print(" recursion=");
        Log.print(recursionCount);
        Log.print(" binding=");
        Log.print(bindingProtection.name());
    }

    protected void traceStartMonitorEnter(final VmThread currentThread) {
        if (Monitor.traceMonitors()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Acquiring monitor for ");
            Log.print(currentThread.getName());
            Log.print(" [");
            dump();
            Log.println("]");
            Log.unlock(lockDisabledSafepoints);
        }
    }

    protected void traceEndMonitorEnter(final VmThread currentThread) {
        if (Monitor.traceMonitors()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Acquired monitor for ");
            Log.print(currentThread.getName());
            Log.print(" [");
            dump();
            Log.println("]");
            Log.unlock(lockDisabledSafepoints);
        }
    }

    protected void traceStartMonitorExit(final VmThread currentThread) {
        if (Monitor.traceMonitors()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Releasing monitor for ");
            Log.print(currentThread.getName());
            Log.print(" [");
            dump();
            Log.println("]");
            Log.unlock(lockDisabledSafepoints);
        }
    }

    protected void traceEndMonitorExit(final VmThread currentThread) {
        if (Monitor.traceMonitors()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Released monitor for ");
            Log.print(currentThread.getName());
            Log.print(" [");
            dump();
            Log.println("]");
            Log.unlock(lockDisabledSafepoints);
        }
    }

    protected void traceStartMonitorWait(final VmThread currentThread) {
        if (Monitor.traceMonitors()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Start wait on monitor for ");
            Log.print(currentThread.getName());
            Log.print(" [");
            dump();
            Log.println("]");
            Log.unlock(lockDisabledSafepoints);
        }
    }

    protected void traceEndMonitorWait(final VmThread currentThread, final boolean interrupted, boolean timedOut) {
        if (Monitor.traceMonitors()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("End wait on monitor for ");
            Log.print(currentThread.getName());
            Log.print(" [");
            dump();
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
        if (Monitor.traceMonitors()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("End notify monitor for ");
            Log.print(currentThread.getName());
            Log.print(" [");
            dump();
            Log.println("]");
            Log.unlock(lockDisabledSafepoints);
        }
    }

    protected void traceStartMonitorNotify(final VmThread currentThread) {
        if (Monitor.traceMonitors()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Start notify monitor for ");
            Log.print(currentThread.getName());
            Log.print(" [");
            dump();
            Log.println("]");
            Log.unlock(lockDisabledSafepoints);
        }
    }
}
