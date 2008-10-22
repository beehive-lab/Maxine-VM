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
import com.sun.max.vm.debug.Debug.*;
import com.sun.max.vm.heap.sequential.Beltway.*;
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
    private static int _unboundListQty = 0;
    private static ManagedMonitor _unboundList;
    private static ManagedMonitor[] _allBindable = new ManagedMonitor[0];
    private static ManagedMonitor[] _allSticky = new ManagedMonitor[0];
    private static int _allBindableQty = 0;
    private static int _allStickyQty = 0;
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

            for (int i = 0; i < BeltwayConfiguration._numberOfGCThreads; i++) {
                prototypeBindStickyMonitor(BeltwayCollectorThread._tokens[i], new StandardJavaMonitor());
            }
            prototypeBindStickyMonitor(BeltwayCollectorThread._callerToken, new StandardJavaMonitor());

            if (_gcDeadlockDetection) {
                prototypeBindStickyMonitor(MaxineVM.hostOrTarget().configuration().heapScheme(), new StandardJavaMonitor.HeapSchemeDeadlockDetectionJavaMonitor());
            }
            for (int i = 0; i < _UNBOUNDLIST_IMAGE_QTY; i++) {
                final ManagedMonitor monitor = newManagedMonitor();
                addToUnboundList(monitor);
                prototypeAddToAllBindable(monitor);
            }
        } else if (phase == MaxineVM.Phase.PRIMORDIAL) {
            for (int i = 0; i < _allBindableQty; i++) {
                final ManagedMonitor monitor = _allBindable[i];
                monitor.alloc();
            }
            for (int i = 0; i < _allStickyQty; i++) {
                final ManagedMonitor monitor = _allSticky[i];
                monitor.alloc();
                monitor.setDisplacedMisc(ObjectAccess.readMisc(monitor.boundObject()));
                monitor.refreshBoundObject();
            }
        } else if (phase == MaxineVM.Phase.STARTING) {
            if (Monitor.traceMonitors() && _allStickyQty > 0) {
                final DebugPrintStream out = Debug.out;
                final boolean lockDisabledSafepoints = Debug.lock();
                out.println("Sticky monitors:");
                for (int i = 0; i < _allStickyQty; i++) {
                    final ManagedMonitor monitor = _allSticky[i];
                    out.print("  ");
                    out.print(i);
                    out.print(": ");
                    monitor.dump(out);
                    out.println();
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
    static void prototypeBindStickyMonitor(Object object, ManagedMonitor monitor) {
        monitor.setBoundObject(object);
        prototypeAddToAllSticky(monitor);
    }

    @PROTOTYPE_ONLY
    public static void prototypeBindStickyMonitor(Object object) {
        prototypeBindStickyMonitor(object, new StandardJavaMonitor());
    }

    @PROTOTYPE_ONLY
    private static void prototypeAddToAllSticky(ManagedMonitor monitor) {
        if (_allStickyQty == _allSticky.length) {
            final ManagedMonitor[] newAllSticky = new ManagedMonitor[_allSticky.length + 1];
            System.arraycopy(_allSticky, 0, newAllSticky, 0, _allSticky.length);
            _allSticky = newAllSticky;
        }
        _allSticky[_allStickyQty++] = monitor;
    }

    @PROTOTYPE_ONLY
    private static void prototypeAddToAllBindable(ManagedMonitor monitor) {
        if (_allBindableQty == _allBindable.length) {
            final ManagedMonitor[] newAllBindable = new ManagedMonitor[_allBindable.length + _UNBOUNDLIST_GROW_QTY];
            System.arraycopy(_allBindable, 0, newAllBindable, 0, _allBindable.length);
            _allBindable = newAllBindable;
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
            managedMonitor.alloc();
        }
        return managedMonitor;
    }

    private static void addToAllBindable(ManagedMonitor monitor) {
        _allBindable[_allBindableQty++] = monitor;
    }

    @INLINE
    private static ManagedMonitor takeFromUnboundList() {
        // No safe points in here, so safe to touch the free list.
        final ManagedMonitor monitor = _unboundList;
        _unboundList = _unboundList.next();
        monitor.setNext(null);
        _unboundListQty--;
        return monitor;
    }

    @INLINE
    private static void addToUnboundList(ManagedMonitor monitor) {
        // No safe points in here, so safe to touch the free list.
        monitor.setNext(_unboundList);
        _unboundList = monitor;
        _unboundListQty++;
    }

    public static JavaMonitor bindMonitor(Object object) {
        ManagedMonitor monitor;
        if (_inGlobalSafePoint) {
            monitor = takeFromUnboundList();
        } else {
            synchronized (JavaMonitorManager.class) {
                if (_unboundListQty < _UNBOUNDLIST_MIN_QTY) {
                    System.gc();
                }
                if (_unboundListQty < _UNBOUNDLIST_MIN_QTY) {
                    expandUnboundList();
                }
                monitor = takeFromUnboundList();
            }
        }
        monitor.setBoundObject(object);
        if (Monitor.traceMonitors()) {
            final DebugPrintStream out = Debug.out;
            final boolean lockDisabledSafepoints = Debug.lock();
            out.print("Bound monitor: ");
            monitor.dump(out);
            out.println();
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
        final ManagedMonitor[] newAllBindable = new ManagedMonitor[_allBindable.length + _UNBOUNDLIST_GROW_QTY];

        // Create the new monitors
        for (int i = 0; i < _UNBOUNDLIST_GROW_QTY; i++) {
            final ManagedMonitor monitor = newManagedMonitor();
            monitor.setNext(newUnboundList);
            newUnboundList = monitor;
        }

        // This is the only place where we need to synchronise monitor list access
        // between a mutator thread and a gc thread which is performing unbinding.
        Safepoint.disable();
        for (int i = 0; i < _allBindable.length; i++) {
            newAllBindable[i] = _allBindable[i];
        }
        _allBindable = newAllBindable;
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
        unbindUnownedMonitors();
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
        for (int i = 0; i < _allBindableQty; i++) {
            final ManagedMonitor monitor = _allBindable[i];
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
        for (int i = 0; i < _allBindableQty; i++) {
            final ManagedMonitor monitor = _allBindable[i];
            if (monitor.requiresPostGCRefresh()) {
                monitor.refreshBoundObject();
            }
        }
        for (int i = 0; i < _allStickyQty; i++) {
            final ManagedMonitor monitor = _allSticky[i];
            monitor.refreshBoundObject();
        }
    }

    interface ManagedMonitor extends JavaMonitor {

        enum BindingProtection {
            PRE_ACQUIRE, UNPROTECTED, PROTECTED
        }

        void alloc();

        Object boundObject();

        void refreshBoundObject();

        void setBoundObject(Object object);

        boolean isBound();

        boolean isHardBound();

        boolean requiresPostGCRefresh();

        BindingProtection bindingProtection();

        void setBindingProtection(BindingProtection protection);

        void setDisplacedMisc(Word lockWord);

        void reset();

        void preGCPrepare();

        ManagedMonitor next();

        void setNext(ManagedMonitor monitor);

        void dump(DebugPrintStream out);
    }

}
