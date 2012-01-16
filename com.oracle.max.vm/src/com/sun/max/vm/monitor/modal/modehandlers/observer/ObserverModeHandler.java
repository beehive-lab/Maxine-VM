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
package com.sun.max.vm.monitor.modal.modehandlers.observer;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.monitor.modal.modehandlers.*;
import com.sun.max.vm.monitor.modal.modehandlers.AbstractModeHandler.ModeDelegate.DelegatedThreadHoldsMonitorResult;
import com.sun.max.vm.monitor.modal.modehandlers.AbstractModeHandler.MonitorSchemeEntry;
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.thread.*;

/**
 */
public final class ObserverModeHandler extends AbstractModeHandler implements MonitorSchemeEntry {

    public interface MonitorObserver {
        void notify(Event event, Object object);
    }

    enum Event {MONITOR_ENTER, MONITOR_EXIT, MAKE_HASHCODE, MONITOR_NOTIFY, MONITOR_NOTIFYALL, MONITOR_WAIT}

    private MonitorObserver observerList;
    private MonitorObserver[] observers = new MonitorObserver[0];

    private ObserverModeHandler(ModeDelegate delegate) {
        super(delegate);
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        if (MaxineVM.isHosted() && phase == Phase.BOOTSTRAPPING) {
            JavaMonitorManager.bindStickyMonitor(this);
        }
    }

    private void notifyObservers(Event event, Object object) {
        for (int i = 0; i < observers.length; i++) {
            observers[i].notify(event, object);
        }
    }

    private boolean observerListContains(MonitorObserver observer) {
        for (int i = 0; i < observers.length; i++) {
            if (observers[i] == observer) {
                return true;
            }
        }
        return false;
    }

    @HOSTED_ONLY
    public synchronized void attach(MonitorObserver observer) {
        if (!observerListContains(observer)) {
            final MonitorObserver[] newObservers = new MonitorObserver[observers.length + 1];
            System.arraycopy(observers, 0, newObservers, 0, observers.length);
            newObservers[observers.length] = observer;
            observers = newObservers;
        }
    }

    /**
     * Returns a TracingModeHandler with the required interface for fast-path entry from a MonitorScheme.
     */
    public static MonitorSchemeEntry asFastPath(ModeDelegate delegate) {
        return new ObserverModeHandler(delegate);
    }

    public void afterGarbageCollection() {
        delegate().delegateAfterGarbageCollection();
    }

    public void beforeGarbageCollection() {
        delegate().delegateBeforeGarbageCollection();
    }

    public Word createMisc(Object object) {
        return HashableLockword64.from(Address.zero()).setHashcode(monitorScheme().createHashCode(object));
    }

    public int makeHashCode(Object object) {
        nullCheck(object);
        if (MaxineVM.isHosted()) {
            return monitorScheme().createHashCode(object);
        }
        notifyObservers(Event.MAKE_HASHCODE, object);
        return delegate().delegateMakeHashcode(object, ModalLockword64.from(ObjectAccess.readMisc(object)));
    }

    public void monitorEnter(Object object) {
        nullCheck(object);
        if (MaxineVM.isHosted()) {
            HostMonitor.enter(object);
            return;
        }
        notifyObservers(Event.MONITOR_ENTER, object);
        delegate().delegateMonitorEnter(object, ModalLockword64.from(ObjectAccess.readMisc(object)), encodeCurrentThreadIDForLockword());
    }

    public void monitorExit(Object object) {
        nullCheck(object);
        if (MaxineVM.isHosted()) {
            HostMonitor.exit(object);
            return;
        }
        notifyObservers(Event.MONITOR_EXIT, object);
        delegate().delegateMonitorExit(object, ModalLockword64.from(ObjectAccess.readMisc(object)));
    }

    public void monitorNotify(Object object, boolean all) {
        nullCheck(object);
        if (MaxineVM.isHosted()) {
            HostMonitor.notify(object);
            return;
        }
        if (all) {
            notifyObservers(Event.MONITOR_NOTIFYALL, object);
        } else {
            notifyObservers(Event.MONITOR_NOTIFY, object);
        }
        delegate().delegateMonitorNotify(object, all, ModalLockword64.from(ObjectAccess.readMisc(object)));
    }

    public void monitorWait(Object object, long timeout) throws InterruptedException {
        nullCheck(object);
        if (MaxineVM.isHosted()) {
            HostMonitor.wait(object, timeout);
            return;
        }
        notifyObservers(Event.MONITOR_WAIT, object);
        delegate().delegateMonitorWait(object, timeout, ModalLockword64.from(ObjectAccess.readMisc(object)));
    }

    public boolean threadHoldsMonitor(Object object, VmThread thread) {
        nullCheck(object);
        final ModalLockword64 lockword = ModalLockword64.from(ObjectAccess.readMisc(object));
        final DelegatedThreadHoldsMonitorResult result = delegate().delegateThreadHoldsMonitor(object, lockword, thread, encodeCurrentThreadIDForLockword());
        return result == DelegatedThreadHoldsMonitorResult.TRUE;
    }

}

