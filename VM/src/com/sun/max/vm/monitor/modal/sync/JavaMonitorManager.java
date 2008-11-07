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
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.monitor.modal.sync.JavaMonitorManager.ManagedMonitor.*;
import com.sun.max.vm.monitor.modal.sync.nat.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Manages a pool of JavaMonitors and their binding and unbinding to objects.
 *
 * Binding can be performed at prototyping or runtime. If binding is performed at prototyping time then either a default
 * or specialized JavaMonitor can be used. If binding is performed at runtime then an unbound JavaMonitor is taken from
 * a free list.
 *
 * Unbinding is performed at global safepoints. All unowned, unbindable, bound monitors are unbound. Writing of unbound
 * lockwords is delegated to an UnboundMiscWordWriter (most likely the inflated mode handler of the ModalMonitorScheme).
 * This allows unbinding to be a transition to any other locking mode.
 *
 * GC considerations:
 *
 * 1) As all JavaMonitors are GC reachable, so are their bound objects (as this is just a field in the JavaMonitor). So
 * it is preferable to call beforeGarbageCollection() prior to GC, to unbind dead objects.
 *
 * 2) If the GC moves a bound JavaMonitor, then the bound object's misc word must be updated to point to the moved
 * JavaMonitor. Therefore a post-GC call is required to afterGarbageCollection().
 *
 * @author Simon Wilkinson
 */
public class JavaMonitorManager {

    private static final int _UNBOUNDLIST_IMAGE_QTY = 2000; // Have a large amount to start with until we get gc right
    private static final int _UNBOUNDLIST_MIN_QTY = 50; // Always keep a minimum in case gc or monitor allocation needs

    // monitors
    private static final int _UNBOUNDLIST_GROW_QTY = 2000;
    private static int _numberOfUnboundMonitors = 0;
    private static ManagedMonitor _unboundList;
    private static ManagedMonitor[] _bindableMonitors = new ManagedMonitor[0];
    private static ManagedMonitor[] _stickyMonitors = new ManagedMonitor[0];
    private static int _numberOfBindableMonitors = 0;
    private static int _numberOfStickyMonitors = 0;
    private static boolean _inGlobalSafePoint = false;

    private static boolean _gcDeadlockDetection = true;

    public interface UnboundMiscWordWriter {
        void writeUnboundMiscWord(Object object, Word preBindingMiscWord);
    }

    @CONSTANT_WHEN_NOT_ZERO
    private static UnboundMiscWordWriter _unboundMiscWordWriter;

    @CONSTANT_WHEN_NOT_ZERO
    private static boolean _requireProxyAcquirableMonitors;

    public static void initialize(MaxineVM.Phase phase) {
        Mutex.initialize(phase);
        if (phase == MaxineVM.Phase.PROTOTYPING) {
            prototypeBindStickyMonitor(JavaMonitorManager.class, new StandardJavaMonitor());
            prototypeBindStickyMonitor(VmThreadMap.ACTIVE, new StandardJavaMonitor.VMThreadMapJavaMonitor());

            if (_gcDeadlockDetection) {
                prototypeBindStickyMonitor(MaxineVM.hostOrTarget().configuration().heapScheme(), new StandardJavaMonitor.HeapSchemeDeadlockDetectionJavaMonitor());
            }
            for (int i = 0; i < _UNBOUNDLIST_IMAGE_QTY; i++) {
                final ManagedMonitor monitor = newManagedMonitor();
                addToUnboundList(monitor);
                prototypeAddToBindableMonitors(monitor);
            }
        } else if (phase == MaxineVM.Phase.PRIMORDIAL) {
            for (int i = 0; i < _numberOfBindableMonitors; i++) {
                final ManagedMonitor monitor = _bindableMonitors[i];
                monitor.allocate();
            }
            for (int i = 0; i < _numberOfStickyMonitors; i++) {
                final ManagedMonitor monitor = _stickyMonitors[i];
                monitor.allocate();
                monitor.setDisplacedMisc(ObjectAccess.readMisc(monitor.boundObject()));
                monitor.refreshBoundObject();
            }
        } else if (phase == MaxineVM.Phase.STARTING) {
            if (Monitor.traceMonitors() && _numberOfStickyMonitors > 0) {
                final boolean lockDisabledSafepoints = Debug.lock();
                Debug.println("Sticky monitors:");
                for (int i = 0; i < _numberOfStickyMonitors; i++) {
                    final ManagedMonitor monitor = _stickyMonitors[i];
                    Debug.print("  ");
                    Debug.print(i);
                    Debug.print(": ");
                    monitor.dump();
                    Debug.println();
                }
                Debug.unlock(lockDisabledSafepoints);
            }
        }
    }

    @PROTOTYPE_ONLY
    public static void setRequireProxyAcquirableMonitors(boolean requireProxyAcquirableMonitors) {
        _requireProxyAcquirableMonitors = requireProxyAcquirableMonitors;
    }

    @PROTOTYPE_ONLY
    public static void prototypeBindStickyMonitor(Object object, ManagedMonitor monitor) {
        monitor.setBoundObject(object);
        prototypeAddToStickyMonitors(monitor);
    }

    @PROTOTYPE_ONLY
    public static void prototypeBindStickyMonitor(Object object) {
        prototypeBindStickyMonitor(object, new StandardJavaMonitor());
    }

    @PROTOTYPE_ONLY
    private static void prototypeAddToStickyMonitors(ManagedMonitor monitor) {
        if (_numberOfStickyMonitors == _stickyMonitors.length) {
            final ManagedMonitor[] newAllSticky = new ManagedMonitor[_stickyMonitors.length + 1];
            System.arraycopy(_stickyMonitors, 0, newAllSticky, 0, _stickyMonitors.length);
            _stickyMonitors = newAllSticky;
        }
        _stickyMonitors[_numberOfStickyMonitors++] = monitor;
    }

    @PROTOTYPE_ONLY
    private static void prototypeAddToBindableMonitors(ManagedMonitor monitor) {
        if (_numberOfBindableMonitors == _bindableMonitors.length) {
            final ManagedMonitor[] newAllBindable = new ManagedMonitor[_bindableMonitors.length + _UNBOUNDLIST_GROW_QTY];
            System.arraycopy(_bindableMonitors, 0, newAllBindable, 0, _bindableMonitors.length);
            _bindableMonitors = newAllBindable;
        }
        addToAllBindable(monitor);
    }

    private static ManagedMonitor newManagedMonitor() {
        ManagedMonitor managedMonitor;
        if (_requireProxyAcquirableMonitors) {
            managedMonitor = new ProxyAcquirableJavaMonitor();
        } else {
            managedMonitor = new StandardJavaMonitor();
        }
        if (!MaxineVM.isPrototyping()) {
            managedMonitor.allocate();
        }
        return managedMonitor;
    }

    private static void addToAllBindable(ManagedMonitor monitor) {
        _bindableMonitors[_numberOfBindableMonitors++] = monitor;
    }

    @INLINE
    private static ManagedMonitor takeFromUnboundList() {
        // No safe points in here, so safe to touch the free list.
        final ManagedMonitor monitor = _unboundList;
        _unboundList = _unboundList.next();
        monitor.setNext(null);
        _numberOfUnboundMonitors--;
        return monitor;
    }

    @INLINE
    private static void addToUnboundList(ManagedMonitor monitor) {
        // No safe points in here, so safe to touch the free list.
        monitor.setNext(_unboundList);
        _unboundList = monitor;
        _numberOfUnboundMonitors++;
    }

    public static JavaMonitor bindMonitor(Object object) {
        ManagedMonitor monitor;
        if (_inGlobalSafePoint) {
            monitor = takeFromUnboundList();
        } else {
            synchronized (JavaMonitorManager.class) {
                if (_numberOfUnboundMonitors < _UNBOUNDLIST_MIN_QTY) {
                    //System.gc();
                }
                if (_numberOfUnboundMonitors < _UNBOUNDLIST_MIN_QTY) {
                    expandUnboundList();
                }
                monitor = takeFromUnboundList();
            }
        }
        monitor.setBoundObject(object);
        if (Monitor.traceMonitors()) {
            final boolean lockDisabledSafepoints = Debug.lock();
            Debug.print("Bound monitor: ");
            monitor.dump();
            Debug.println();
            Debug.unlock(lockDisabledSafepoints);
        }
        return monitor;
    }

    public static void unbindMonitor(JavaMonitor monitor) {
        final ManagedMonitor bindableMonitor = (ManagedMonitor) monitor;
        bindableMonitor.reset();
        if (_inGlobalSafePoint) {
            addToUnboundList(bindableMonitor);
        } else {
            synchronized (JavaMonitorManager.class) {
                addToUnboundList(bindableMonitor);
            }
        }
    }


    private static void expandUnboundList() {
        ManagedMonitor newUnboundList = null;
        final ManagedMonitor[] newAllBindable = new ManagedMonitor[_bindableMonitors.length + _UNBOUNDLIST_GROW_QTY];

        // Create the new monitors
        for (int i = 0; i < _UNBOUNDLIST_GROW_QTY; i++) {
            final ManagedMonitor monitor = newManagedMonitor();
            monitor.setNext(newUnboundList);
            newUnboundList = monitor;
        }

        // This is the only place where we need to synchronise monitor list access
        // between a mutator thread and a gc thread which is performing unbinding.
        Safepoint.disable();
        for (int i = 0; i < _bindableMonitors.length; i++) {
            newAllBindable[i] = _bindableMonitors[i];
        }
        _bindableMonitors = newAllBindable;
        ManagedMonitor monitor = newUnboundList;
        while (monitor != null) {
            newUnboundList = monitor.next();
            addToUnboundList(monitor);
            addToAllBindable(monitor);
            monitor = newUnboundList;
        }
        Safepoint.enable();
    }

    public static void protectBinding(JavaMonitor monitor) {
        VmThread.current().setProtectedMonitor(monitor);
    }

    public static void registerMonitorUnbinder(UnboundMiscWordWriter unbinder) {
        _unboundMiscWordWriter = unbinder;
    }

    /**
     * Must only be called on a global safepoint.
     */
    public static void beforeGarbageCollection() {
        _inGlobalSafePoint = true;
        //unbindUnownedMonitors();
    }

    /**
     * Must only be called on a global safepoint.
     */
    public static void afterGarbageCollection() {
        refreshAllBindings();
        _inGlobalSafePoint = false;
    }

    private static class ProtectedMonitorGatherer implements Procedure<VmThread> {
        @Override
        public void run(VmThread thread) {
            final JavaMonitor monitor = thread.protectedMonitor();
            if (monitor != null) {
                final ManagedMonitor managedMonitor = (ManagedMonitor) monitor;
                if (managedMonitor.bindingProtection() == BindingProtection.UNPROTECTED) {
                    managedMonitor.setBindingProtection(BindingProtection.PROTECTED);
                }
            }
        }
    }

    private static final ProtectedMonitorGatherer _protectedMonitorGatherer = new ProtectedMonitorGatherer();

    /**
     * Must only be called on a global safepoint.
     */
    private static void unbindUnownedMonitors() {
        // Mark all protected monitors
        VmThreadMap.ACTIVE.forAllVmThreads(null, _protectedMonitorGatherer);
        // Deflate all non-protected and non-sticky monitors with no owner
        for (int i = 0; i < _numberOfBindableMonitors; i++) {
            final ManagedMonitor monitor = _bindableMonitors[i];
            if (monitor.isHardBound() && monitor.bindingProtection() == BindingProtection.PRE_ACQUIRE) {
                monitor.setBindingProtection(BindingProtection.UNPROTECTED);
            }
            if (monitor.bindingProtection() == BindingProtection.UNPROTECTED) {
                // Write the object's new misc word
                _unboundMiscWordWriter.writeUnboundMiscWord(monitor.boundObject(), monitor.displacedMisc());
                monitor.reset();
                // Put the monitor back on the unbound list.
                // This is thread-safe as mutator thread access to the free-list is
                // atomic with respect to safepointing.
                addToUnboundList(monitor);
            } else if (monitor.isBound()) {
                monitor.preGCPrepare();
            }
        }
    }

    /**
     * Must only be called on a global safepoint.
     */
    private static void refreshAllBindings() {
        for (int i = 0; i < _numberOfBindableMonitors; i++) {
            final ManagedMonitor monitor = _bindableMonitors[i];
            if (monitor.requiresPostGCRefresh()) {
                monitor.refreshBoundObject();
            }
        }
        for (int i = 0; i < _numberOfStickyMonitors; i++) {
            final ManagedMonitor monitor = _stickyMonitors[i];
            monitor.refreshBoundObject();
        }
    }

    interface ManagedMonitor extends JavaMonitor {

        enum BindingProtection {
            PRE_ACQUIRE, UNPROTECTED, PROTECTED
        }

        void allocate();

        Object boundObject();

        void refreshBoundObject();

        void setBoundObject(Object object);

        boolean isBound();

        boolean isHardBound();

        boolean requiresPostGCRefresh();

        BindingProtection bindingProtection();

        void setBindingProtection(BindingProtection protection);

        void reset();

        void preGCPrepare();

        ManagedMonitor next();

        void setNext(ManagedMonitor monitor);

        void dump();
    }

}
