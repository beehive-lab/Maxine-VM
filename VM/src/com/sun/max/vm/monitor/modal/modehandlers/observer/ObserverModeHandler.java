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
package com.sun.max.vm.monitor.modal.modehandlers.observer;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.monitor.modal.modehandlers.*;
import com.sun.max.vm.monitor.modal.modehandlers.AbstractModeHandler.*;
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.thread.*;

/**
 *
 * @author Simon Wilkinson
 */
public final class ObserverModeHandler extends AbstractModeHandler implements MonitorSchemeEntry {

    public interface MonitorObserver {
        void notify(Event event, Object object);
    }

    enum Event {MONITOR_ENTER, MONITOR_EXIT, MAKE_HASHCODE, MONITOR_NOTIFY, MONITOR_NOTIFYALL, MONITOR_WAIT}

    private MonitorObserver _observerList;
    private MonitorObserver[] _observers = new MonitorObserver[0];

    private ObserverModeHandler(ModeDelegate delegate) {
        super(delegate);
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        if (phase == MaxineVM.Phase.PROTOTYPING) {
            JavaMonitorManager.prototypeBindStickyMonitor(this);
        }
    }

    private void notifyObservers(Event event, Object object) {
        for (int i = 0; i < _observers.length; i++) {
            _observers[i].notify(event, object);
        }
    }

    private boolean observerListContains(MonitorObserver observer) {
        for (int i = 0; i < _observers.length; i++) {
            if (_observers[i] == observer) {
                return true;
            }
        }
        return false;
    }

    @PROTOTYPE_ONLY
    public synchronized void attach(MonitorObserver observer) {
        if (!observerListContains(observer)) {
            final MonitorObserver[] newObservers = new MonitorObserver[_observers.length + 1];
            System.arraycopy(_observers, 0, newObservers, 0, _observers.length);
            newObservers[_observers.length] = observer;
            _observers = newObservers;
        }
    }

    /**
     * Returns a TracingModeHandler with the required interface for fast-path entry from a MonitorScheme.
     */
    public static MonitorSchemeEntry asFastPath(ModeDelegate delegate) {
        return new ObserverModeHandler(delegate);
    }

    @Override
    public void afterGarbageCollection() {
        getDelegate().delegateAfterGarbageCollection();
    }

    @Override
    public void beforeGarbageCollection() {
        getDelegate().delegateBeforeGarbageCollection();
    }

    @Override
    public Word createMisc(Object object) {
        return HashableLockWord64.as(Address.zero()).setHashcode(monitorScheme().createHashCode(object));
    }

    @Override
    public int makeHashCode(Object object) {
        nullCheck(object);
        if (MaxineVM.isPrototyping()) {
            return monitorScheme().createHashCode(object);
        }
        notifyObservers(Event.MAKE_HASHCODE, object);
        return getDelegate().delegateMakeHashcode(object, ModalLockWord64.as(ObjectAccess.readMisc(object)));
    }

    @Override
    public void monitorEnter(Object object) {
        nullCheck(object);
        if (MaxineVM.isPrototyping()) {
            HostMonitor.enter(object);
            return;
        }
        notifyObservers(Event.MONITOR_ENTER, object);
        getDelegate().delegateMonitorEnter(object, ModalLockWord64.as(ObjectAccess.readMisc(object)), encodeCurrentForLockwordThreadID());
    }

    @Override
    public void monitorExit(Object object) {
        nullCheck(object);
        if (MaxineVM.isPrototyping()) {
            HostMonitor.exit(object);
            return;
        }
        notifyObservers(Event.MONITOR_EXIT, object);
        getDelegate().delegateMonitorExit(object, ModalLockWord64.as(ObjectAccess.readMisc(object)));
    }

    @Override
    public void monitorNotify(Object object, boolean all) {
        nullCheck(object);
        if (MaxineVM.isPrototyping()) {
            HostMonitor.notify(object);
            return;
        }
        if (all) {
            notifyObservers(Event.MONITOR_NOTIFYALL, object);
        } else {
            notifyObservers(Event.MONITOR_NOTIFY, object);
        }
        getDelegate().delegateMonitorNotify(object, all, ModalLockWord64.as(ObjectAccess.readMisc(object)));
    }

    @Override
    public void monitorWait(Object object, long timeout) throws InterruptedException {
        nullCheck(object);
        if (MaxineVM.isPrototyping()) {
            HostMonitor.wait(object, timeout);
            return;
        }
        notifyObservers(Event.MONITOR_WAIT, object);
        getDelegate().delegateMonitorWait(object, timeout, ModalLockWord64.as(ObjectAccess.readMisc(object)));
    }

    private final boolean[] _threadHoldsMonitorResult = new boolean[1];

    @Override
    public boolean threadHoldsMonitor(Object object, VmThread thread) {
        nullCheck(object);
        final ModalLockWord64 lockWord = ModalLockWord64.as(ObjectAccess.readMisc(object));
        getDelegate().delegateThreadHoldsMonitor(object, lockWord, thread, encodeCurrentForLockwordThreadID(), _threadHoldsMonitorResult);
        return _threadHoldsMonitorResult[0];
    }


}


