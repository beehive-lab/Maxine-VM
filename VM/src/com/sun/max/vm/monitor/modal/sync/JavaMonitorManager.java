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
import com.sun.max.vm.heap.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.monitor.modal.sync.JavaMonitorManager.ManagedMonitor.*;
import com.sun.max.vm.monitor.modal.sync.nat.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Manages a pool of {@linkplain JavaMonitor JavaMonitors} and their binding and unbinding to objects.
 * <p>
 * Binding can be performed at prototyping or runtime. If binding is performed at prototyping time then either a default
 * or specialized JavaMonitor can be used. If binding is performed at runtime then an unbound JavaMonitor is taken from
 * a free list.
 * <p>
 * Unbinding is performed at global safepoints. All unowned, unbindable, bound monitors are unbound. Writing of unbound
 * lockwords is delegated to an {@linkplain UnboundMiscWordWriter UnboundMiscWordWriter} object (most likely the inflated mode handler of the ModalMonitorScheme).
 * This allows unbinding to be a transition to any other locking mode.
 * <p>
 * GC considerations:
 * <p>
 * 1) As all JavaMonitors are GC reachable, so are their bound objects (as this is just a field in the JavaMonitor). So
 * it is preferable to call beforeGarbageCollection() prior to GC, to unbind dead objects.
 * <p>
 * 2) If the GC moves a bound JavaMonitor, then the bound object's misc word must be updated to point to the moved
 * JavaMonitor. Therefore a post-GC call is required to afterGarbageCollection().
 * <p>
 * TODO: (Simon) The terminology of binding/unbinding/hard-binding needs tidying up. The naming conventions
 * are not consistent and in places, misleading.
 *
 * @author Simon Wilkinson
 * @author Mick Jordan
 */
public class JavaMonitorManager {

    // Image build time properties
    public static final String UNBOUNDLIST_IMAGE_QTY_PROPERTY = "max.monitor.unboundpool.imagesize";
    public static final String UNBOUNDLIST_GROW_QTY_PROPERTY = "max.monitor.unboundpool.grow";

    private static final int UNBOUNDLIST_MIN_QTY = 25;      // minimum to keep in case gc or monitor allocation needs
    private static final int UNBOUNDLIST_IMAGE_QTY = 50; // Initial allocation in image

    // monitors
    private static final int UNBOUNDLIST_GROW_QTY_DEFAULT = 50;  // default
    private static int unboundListGrowQty = UNBOUNDLIST_GROW_QTY_DEFAULT;
    private static int numberOfUnboundMonitors = 0;
    private static int unboundMonitorsHwm;
    private static ManagedMonitor unboundList;
    private static ManagedMonitor[] bindableMonitors = new ManagedMonitor[0];
    private static ManagedMonitor[] stickyMonitors = new ManagedMonitor[0];
    private static int numberOfBindableMonitors = 0;
    private static int numberOfStickyMonitors = 0;
    private static boolean inGlobalSafePoint = false;

    private static boolean gcDeadlockDetection = true;

    /**
     * Lockword rewriting for objects in the process of being unbound is delegated to an UnboundMiscWordWriter.
     * This allows unbinding to transition a lock from 'inflated' to any other mode.
     *
     * @author Simon Wilkinson
     */
    public interface UnboundMiscWordWriter {
        void writeUnboundMiscWord(Object object, Word preBindingMiscWord);
    }

    @CONSTANT_WHEN_NOT_ZERO
    private static UnboundMiscWordWriter unboundMiscWordWriter;

    @CONSTANT_WHEN_NOT_ZERO
    private static boolean requireProxyAcquirableMonitors;

    /**
     * Performs any initialization necessary for the given phase.
     *
     * @param phase the current VM phase
     */
    public static void initialize(MaxineVM.Phase phase) {
        if (MaxineVM.isPrototyping()) {
            prototypeBindStickyMonitor(JavaMonitorManager.class, new StandardJavaMonitor());
            prototypeBindStickyMonitor(VmThreadMap.ACTIVE, new StandardJavaMonitor.VMThreadMapJavaMonitor());
            prototypeBindStickyMonitor(SpecialReferenceManager.LOCK, new StandardJavaMonitor());

            if (gcDeadlockDetection) {
                prototypeBindStickyMonitor(MaxineVM.hostOrTarget().configuration().heapScheme(), new StandardJavaMonitor.HeapSchemeDeadlockDetectionJavaMonitor());
            }
            int unBoundListImageQty = UNBOUNDLIST_IMAGE_QTY;
            final String  unBoundListImageQtyProperty = System.getProperty(UNBOUNDLIST_IMAGE_QTY_PROPERTY);
            if (unBoundListImageQtyProperty != null) {
                unBoundListImageQty = Integer.parseInt(unBoundListImageQtyProperty);
            }
            final String  unBoundListGrowQtyProperty = System.getProperty(UNBOUNDLIST_GROW_QTY_PROPERTY);
            if (unBoundListGrowQtyProperty != null) {
                unboundListGrowQty = Integer.parseInt(unBoundListGrowQtyProperty);
            }
            for (int i = 0; i < unBoundListImageQty; i++) {
                final ManagedMonitor monitor = newManagedMonitor();
                addToUnboundList(monitor);
                prototypeAddToBindableMonitors(monitor);
            }
            unboundMonitorsHwm = unBoundListImageQty;
        } else if (phase == MaxineVM.Phase.PRIMORDIAL) {
            NativeMutexFactory.initialize();
            NativeConditionVariableFactory.initialize();
            for (int i = 0; i < numberOfBindableMonitors; i++) {
                final ManagedMonitor monitor = bindableMonitors[i];
                monitor.allocate();
            }
            for (int i = 0; i < numberOfStickyMonitors; i++) {
                final ManagedMonitor monitor = stickyMonitors[i];
                monitor.allocate();
                monitor.setDisplacedMisc(ObjectAccess.readMisc(monitor.boundObject()));
                monitor.refreshBoundObject();
            }
        } else if (phase == MaxineVM.Phase.STARTING) {
            if (Monitor.traceMonitors() && numberOfStickyMonitors > 0) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.println("Sticky monitors:");
                for (int i = 0; i < numberOfStickyMonitors; i++) {
                    final ManagedMonitor monitor = stickyMonitors[i];
                    Log.print("  ");
                    Log.print(i);
                    Log.print(": ");
                    monitor.dump();
                    Log.println();
                }
                Log.unlock(lockDisabledSafepoints);
            }
        }
    }

    /**
     * Notifies the JavaMonitorManager that the current MonitorScheme requires
     * all JavaMonitors to be proxy acquirable (i.e. they may be acquired by
     * one thread on behalf of another).
     *
     * @param requireProxyAcquirableMonitors true if proxy acquirable monitors are required; false otherwise
     * @see ProxyAcquirableJavaMonitor
     */
    @PROTOTYPE_ONLY
    public static void setRequireProxyAcquirableMonitors(boolean requireProxyAcquirableMonitors) {
        JavaMonitorManager.requireProxyAcquirableMonitors = requireProxyAcquirableMonitors;
    }

    /**
     * Binds the given monitor to the given object at VM image build time.
     * The binding will never be unbound.
     *
     * @param object the object to bind
     * @param monitor the monitor to bind
     */
    @PROTOTYPE_ONLY
    public static void prototypeBindStickyMonitor(Object object, ManagedMonitor monitor) {
        monitor.setBoundObject(object);
        prototypeAddToStickyMonitors(monitor);
    }

    /**
     * Binds a {@link StandardJavaMonitor StandardJavaMonitor} to the given object at VM image build time.
     * The binding will never be unbound.
     *
     * @param object the object to bind
     */
    @PROTOTYPE_ONLY
    public static void prototypeBindStickyMonitor(Object object) {
        prototypeBindStickyMonitor(object, new StandardJavaMonitor());
    }

    @PROTOTYPE_ONLY
    private static void prototypeAddToStickyMonitors(ManagedMonitor monitor) {
        if (numberOfStickyMonitors == stickyMonitors.length) {
            final ManagedMonitor[] newAllSticky = new ManagedMonitor[stickyMonitors.length + 1];
            System.arraycopy(stickyMonitors, 0, newAllSticky, 0, stickyMonitors.length);
            stickyMonitors = newAllSticky;
        }
        stickyMonitors[numberOfStickyMonitors++] = monitor;
    }

    @PROTOTYPE_ONLY
    private static void prototypeAddToBindableMonitors(ManagedMonitor monitor) {
        if (numberOfBindableMonitors == bindableMonitors.length) {
            final ManagedMonitor[] newAllBindable = new ManagedMonitor[bindableMonitors.length + unboundListGrowQty];
            System.arraycopy(bindableMonitors, 0, newAllBindable, 0, bindableMonitors.length);
            bindableMonitors = newAllBindable;
        }
        addToAllBindable(monitor);
    }

    private static ManagedMonitor newManagedMonitor() {
        ManagedMonitor managedMonitor;
        if (requireProxyAcquirableMonitors) {
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
        bindableMonitors[numberOfBindableMonitors++] = monitor;
    }

    private static ManagedMonitor takeFromUnboundList() {
        // No safe points in here, so safe to touch the free list.
        final ManagedMonitor monitor = unboundList;
        unboundList = unboundList.next();
        monitor.setNext(null);
        numberOfUnboundMonitors--;
        return monitor;
    }

    private static void addToUnboundList(ManagedMonitor monitor) {
        // No safe points in here, so safe to touch the free list.
        monitor.setNext(unboundList);
        unboundList = monitor;
        numberOfUnboundMonitors++;
    }

    /**
     * Binds a JavaMonitor to the given object.
     * <p>
     * Important: the binding is not two-way at this stage; the JavaMonitor
     * points to the object, but the object does not know anything about the JavaMonitor.
     * <p>
     * TODO: 'bindMonitor' is a missing leading name. Refactor.
     *
     * @param object the object to bind a monitor to.
     * @return the monitor that was bound
     */
    public static JavaMonitor bindMonitor(Object object) {
        ManagedMonitor monitor;
        if (inGlobalSafePoint) {
            monitor = takeFromUnboundList();
        } else {
            synchronized (JavaMonitorManager.class) {
                if (numberOfUnboundMonitors < UNBOUNDLIST_MIN_QTY) {
                    System.gc();
                }
                // If we didn't free up enough such that we are at least midway between min and hwm, expand
                if (numberOfUnboundMonitors < (unboundMonitorsHwm - UNBOUNDLIST_MIN_QTY) >> 1) {
                    expandUnboundList();
                }
                monitor = takeFromUnboundList();
            }
        }
        monitor.setBoundObject(object);
        if (Monitor.traceMonitors()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Bound monitor: ");
            monitor.dump();
            Log.println();
            Log.unlock(lockDisabledSafepoints);
        }
        return monitor;
    }

    /**
     * Places the given JavaMonitor back into the free list.
     * <p>
     * Important: This should only be called for monitors that have
     * failed to be two-way bound to an object.
     * <p>
     * TODO: 'unbindMonitor' is a missing leading name. Refactor.
     * @param monitor the monitor to unbind.
     */
    public static void unbindMonitor(JavaMonitor monitor) {
        final ManagedMonitor bindableMonitor = (ManagedMonitor) monitor;
        bindableMonitor.reset();
        if (inGlobalSafePoint) {
            addToUnboundList(bindableMonitor);
        } else {
            synchronized (JavaMonitorManager.class) {
                addToUnboundList(bindableMonitor);
            }
        }
    }


    private static void expandUnboundList() {
        ManagedMonitor newUnboundList = null;
        final ManagedMonitor[] newAllBindable = new ManagedMonitor[bindableMonitors.length + unboundListGrowQty];

        // Create the new monitors
        for (int i = 0; i < unboundListGrowQty; i++) {
            final ManagedMonitor monitor = newManagedMonitor();
            monitor.setNext(newUnboundList);
            newUnboundList = monitor;
        }

        // This is the only place where we need to synchronise monitor list access
        // between a mutator thread and a gc thread which is performing unbinding.
        Safepoint.disable();
        for (int i = 0; i < bindableMonitors.length; i++) {
            newAllBindable[i] = bindableMonitors[i];
        }
        bindableMonitors = newAllBindable;
        unboundMonitorsHwm = newAllBindable.length;
        ManagedMonitor monitor = newUnboundList;
        while (monitor != null) {
            newUnboundList = monitor.next();
            addToUnboundList(monitor);
            addToAllBindable(monitor);
            monitor = newUnboundList;
        }
        Safepoint.enable();
    }

    /**
     * Notifies the JavaMonitorManager that the current thread is in-flight to
     * perform an operation on the given JavaMonitor. The JavaMonitor will
     * not be unbound until the current thread calls this method with a different
     * JavaMonitor, or the the thread terminates.
     *
     * @param monitor the monitor whose binding to protect
     */
    public static void protectBinding(JavaMonitor monitor) {
        VmThread.current().setProtectedMonitor(monitor);
    }

    /**
     * Registers the given UnboundMiscWordWriter as the lock word rewriter
     * for when unbinding objects from monitors.
     *
     * @param unbinder the lockword rewriter
     */
    public static void registerMonitorUnbinder(UnboundMiscWordWriter unbinder) {
        unboundMiscWordWriter = unbinder;
    }

    /**
     * Notifies the JavaMonitorManager that the VM is at a global safepoint, before
     * garbage collection has started.
     */
    public static void beforeGarbageCollection() {
        inGlobalSafePoint = true;
        unbindUnownedMonitors();
    }

    /**
     * Notifies the JavaMonitorManager that the VM is at a global safepoint, after
     * garbage collection has completed.
     */
    public static void afterGarbageCollection() {
        refreshAllBindings();
        inGlobalSafePoint = false;
    }

    private static class ProtectedMonitorGatherer implements Procedure<VmThread> {
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

    private static final ProtectedMonitorGatherer protectedMonitorGatherer = new ProtectedMonitorGatherer();

    /**
     * Must only be called on a global safepoint.
     */
    private static void unbindUnownedMonitors() {
        // Mark all protected monitors
        VmThreadMap.ACTIVE.forAllVmThreads(null, protectedMonitorGatherer);
        // Deflate all non-protected and non-sticky monitors with no owner
        for (int i = 0; i < numberOfBindableMonitors; i++) {
            final ManagedMonitor monitor = bindableMonitors[i];
            if (monitor.isHardBound() && monitor.bindingProtection() == BindingProtection.PRE_ACQUIRE) {
                monitor.setBindingProtection(BindingProtection.UNPROTECTED);
            }
            if (monitor.bindingProtection() == BindingProtection.UNPROTECTED) {
                // Write the object's new misc word
                unboundMiscWordWriter.writeUnboundMiscWord(monitor.boundObject(), monitor.displacedMisc());
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
        for (int i = 0; i < numberOfBindableMonitors; i++) {
            final ManagedMonitor monitor = bindableMonitors[i];
            if (monitor.requiresPostGCRefresh()) {
                monitor.refreshBoundObject();
            }
        }
        for (int i = 0; i < numberOfStickyMonitors; i++) {
            final ManagedMonitor monitor = stickyMonitors[i];
            monitor.refreshBoundObject();
        }
    }

    /**
     * Extends the JavaMonitor interface to allow a pool of JavaMonitors to be managed by JavaMonitorManager.
     * <p>
     * TODO: (Simon) The terminology of binding/unbinding/hard-binding needs tidying up. The naming conventions
     * are not consistent and in places, misleading.
     *
     * @author Simon Wilkinson
     */
    interface ManagedMonitor extends JavaMonitor {

        /**
         * Defines the binding status of a JavaMonitor.
         *
         * TODO: (Simon) The terminology of binding/unbinding/hard-binding needs tidying up. The naming conventions
         * are not consistent and in places, misleading.
         *
         * @author Simon Wilkinson
         */
        enum BindingProtection {
            /**
             * The JavaMonitor is in the free-list, or it is one-way bound to an object.
             */
            PRE_ACQUIRE,
            /**
             * The JavaMonitor is two-way bound to an object (hard-bound), but can be safely unbound.
             */
            UNPROTECTED,
            /**
             * The JavaMonitor is two-way bound to an object (hard-bound), but cannot be unbound.
             */
            PROTECTED
        }

        /**
         * Performs any native allocations necessary for this JavaMonitor.
         */
        void allocate();

        /**
         * Returns this JavaMonitor's bound object.
         *
         * @return the bound object
         */
        Object boundObject();

        /**
         * Refreshes the two-way binding of this JavaMonitor.
         */
        void refreshBoundObject();

        /**
         * Sets this JavaMonitor's bound object. The binding is one-way, i.e.
         * the JavaMonitor points to the object, but the object does not know anything about the JavaMonitor.
         *
         * @param object the object to bind
         */
        void setBoundObject(Object object);

        /**
         * Tests if this JavaMonitor is one-way bound, i.e. the JavaMonitor points to the object.
         *
         * @return true if this JavaMonitor is one-way bound; false otherwise
         */
        boolean isBound();

        /**
         * Tests if this JavaMonitor is two-way bound, i.e. the JavaMonitor points to the object.
         *
         * @return true if this JavaMonitor is two-way bound; false otherwise
         */
        boolean isHardBound();

        /**
         * Tests if this JavaMonitor was two-way (hard) bound prior to GC.
         *
         * @return true if this JavaMonitor was two-way (hard) bound prior to GC
         */
        boolean requiresPostGCRefresh();

        /**
         * Returns the BindingProtection for this JavaMonitor.
         *
         * @return the BindingProtection
         */
        BindingProtection bindingProtection();

        /**
         * Sets the BindingProtection for this JavaMonitor.
         *
         * @param protection the BindingProtection to set
         */
        void setBindingProtection(BindingProtection protection);

        /**
         * Sets this JavaMonitor to its default state.
         */
        void reset();

        /**
         * Perform any pre-GC actions.
         */
        void preGCPrepare();

        /**
         * Direct linked-list support. Returns the next monitor in the list.
         *
         * @return the next in the list
         */
        ManagedMonitor next();

        /**
         * Direct linked-list support. Sets the next monitor in the list.
         *
         * @param monitor the next monitor
         */
        void setNext(ManagedMonitor monitor);

        void dump();
    }

}
