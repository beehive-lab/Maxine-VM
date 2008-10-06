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
/*VCSID=3c823ea0-22b6-43fa-a330-cc842d3a8e3d*/
package com.sun.max.vm.monitor.modal.sync;

import com.sun.max.vm.debug.Debug.*;
import com.sun.max.vm.monitor.modal.sync.nat.*;
import com.sun.max.vm.thread.*;

/**
 *
 * @author Simon Wilkinson
 */
class ProxyAcquirableJavaMonitor extends StandardJavaMonitor {

    private static final Mutex _proxyMutex = new Mutex();
    private static final ConditionVariable _proxyVar = new ConditionVariable();

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
    }

    @Override
    public void monitorPrivateRelease() {
        _ownerThread = null;
        _recursionCount = 0;
        _ownerAcquired = true;
    }

    @Override
    public void alloc() {
        super.alloc();
        _proxyMutex.alloc();
        _proxyVar.alloc();
    }

    @Override
    public final void reset() {
        super.reset();
        _ownerAcquired = false;
    }

    @Override
    public void dump(DebugPrintStream out) {
        super.dump(out);
        out.print(" ownerAcquired=");
        out.print(_ownerAcquired);
        out.print(" proxyMutex=");
        out.print(_proxyMutex.asPointer());
        out.print(" proxyCondVar=");
        out.print(_proxyVar.asPointer());
    }
}
