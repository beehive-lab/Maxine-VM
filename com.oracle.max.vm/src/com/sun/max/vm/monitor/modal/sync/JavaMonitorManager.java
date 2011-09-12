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
package com.sun.max.vm.monitor.modal.sync;

import static com.sun.cri.bytecode.Bytecodes.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.monitor.modal.sync.JavaMonitorManager.ManagedMonitor.BindingProtection;
import com.sun.max.vm.monitor.modal.sync.nat.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Manages a pool of {@linkplain JavaMonitor monitors} and their binding and unbinding to objects.
 * <p>
 * Binding can be performed at bootstrapping or runtime. If binding is performed while bootstrapping then either a default
 * or specialized monitor can be used. If binding is performed at runtime then an unbound monitor is taken from
 * a free list.
 * <p>
 * Unbinding is performed at global safepoints. All unowned, unbindable, bound monitors are unbound. Writing of unbound
 * lockwords is delegated to an {@link UnboundMiscWordWriter} object (most likely the inflated mode handler of the ModalMonitorScheme).
 * This allows unbinding to be a transition to any other locking mode.
 * <p>
 * GC considerations:
 * <p>
 * 1) As all monitors are GC reachable, so are their bound objects (as this is just a field in the monitor).
 * As such, {@link #beforeGarbageCollection()} must be called prior to GC, to unbind dead objects.
 * <p>
 * 2) If the GC moves a bound monitor, then the bound object's misc word must be updated to point to the moved
 * monitor. Therefore a post-GC call is required to {@link #afterGarbageCollection()}.
 * <p>
 * TODO: (Simon) The terminology of binding/unbinding/hard-binding needs tidying up. The naming conventions
 * are not consistent and in places, misleading.
 */
public class JavaMonitorManager {

    // Image build time properties
    public static final String UNBOUNDLIST_IMAGE_QTY_PROPERTY = "max.monitor.unboundpool.imagesize";
    public static final String UNBOUNDLIST_GROW_QTY_PROPERTY = "max.monitor.unboundpool.grow";

    /**
     * Minimum number of unbound monitors that are kept available to handle synchronization
     * code during GC or monitor allocation code paths.
     */
    private static final int UNBOUNDLIST_MIN_QTY = 25;

    /**
     * The number of unbound monitors created in the boot image.
     */
    private static final int UNBOUNDLIST_IMAGE_QTY = 50;

    /**
     * The amount by which the list of unbound monitors grows each time it is {@linkplain #expandUnboundList() expanded}.
     * This value can be configured via the {@link #UNBOUNDLIST_GROW_QTY_PROPERTY} property at boot image build time.
     */
    private static int unboundListGrowQty = 50;

    /**
     * The current number of unbound monitors available.
     */
    private static int numberOfUnboundMonitors = 0;

    /**
     * The unbound monitors high water mark.
     *
     * @see #bindMonitor(Object)
     */
    private static int unboundMonitorsHwm;

    /**
     * The head of the list of unbound monitors.
     */
    private static ManagedMonitor unboundList;

    /**
     * The pool of monitors that can be bound to objects.
     */
    private static ManagedMonitor[] bindableMonitors = {};

    /**
     * The monitors that were created at boot image build time and are permanently
     * bound to boot image objects.
     */
    private static ManagedMonitor[] stickyMonitors = {};

    private static int numberOfBindableMonitors = 0;

    private static boolean inGlobalSafePoint = false;

    /**
     * Lockword rewriting for objects in the process of being unbound is delegated to an UnboundMiscWordWriter.
     * This allows unbinding to transition a lock from 'inflated' to any other mode.
     *
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
        if (MaxineVM.isHosted()) {
            LOCK = newVmLock("MONITOR_MANAGER_LOCK");
            int unboundListImageQty = UNBOUNDLIST_IMAGE_QTY;
            final String  unBoundListImageQtyProperty = System.getProperty(UNBOUNDLIST_IMAGE_QTY_PROPERTY);
            if (unBoundListImageQtyProperty != null) {
                unboundListImageQty = Integer.parseInt(unBoundListImageQtyProperty);
            }
            final String  unboundListGrowQtyProperty = System.getProperty(UNBOUNDLIST_GROW_QTY_PROPERTY);
            if (unboundListGrowQtyProperty != null) {
                unboundListGrowQty = Integer.parseInt(unboundListGrowQtyProperty);
            }
            for (int i = 0; i < unboundListImageQty; i++) {
                final ManagedMonitor monitor = newManagedMonitor();
                addToUnboundList(monitor);
                addToBindableMonitors(monitor);
            }
            unboundMonitorsHwm = unboundListImageQty;
        } else if (phase == MaxineVM.Phase.PRIMORDIAL) {
            NativeMutexFactory.initialize();
            NativeConditionVariableFactory.initialize();
            for (int i = 0; i < numberOfBindableMonitors; i++) {
                final ManagedMonitor monitor = bindableMonitors[i];
                monitor.allocate();
            }
            for (ManagedMonitor monitor : stickyMonitors) {
                monitor.allocate();
                monitor.setDisplacedMisc(ObjectAccess.readMisc(monitor.boundObject()));
                monitor.refreshBoundObject();
            }
        } else if (phase == MaxineVM.Phase.STARTING) {
            if (Monitor.TraceMonitors && stickyMonitors.length > 0) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.println("Sticky monitors:");
                for (int i = 0; i < stickyMonitors.length; i++) {
                    final ManagedMonitor monitor = stickyMonitors[i];
                    Log.print("  ");
                    Log.print(i);
                    Log.print(": ");
                    monitor.log();
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
    @HOSTED_ONLY
    public static void setRequireProxyAcquirableMonitors(boolean requireProxyAcquirableMonitors) {
        JavaMonitorManager.requireProxyAcquirableMonitors = requireProxyAcquirableMonitors;
    }

    /**
     * Binds the given monitor to the given object at VM image build time.
     * The binding will never be unbound. This is useful for monitors that are known to be
     * contended at runtime and/or are used in situations where allocation is disabled.
     *
     * @param object the object to bind
     * @param monitor the monitor to bind
     */
    @HOSTED_ONLY
    public static Object bindStickyMonitor(Object object, ManagedMonitor monitor) {
        monitor.setBoundObject(object);
        addToStickyMonitors(monitor);
        return object;
    }

    /**
     * Binds a {@link StandardJavaMonitor StandardJavaMonitor} to the given object at VM image build time.
     * The binding will never be unbound. This is useful for monitors that are known to be
     * contended at runtime and/or are used in situations where allocation is disabled.
     *
     * @param object the object to bind
     */
    @HOSTED_ONLY
    public static Object bindStickyMonitor(Object object) {
        return bindStickyMonitor(object, new StandardJavaMonitor());
    }

    /**
     * Creates an object and binds a {@link StandardJavaMonitor StandardJavaMonitor} to it at VM image build time.
     * The binding will never be unbound. This is useful for VM locks that are known to be
     * contended at runtime and/or are used in situations where allocation is disabled.
     *
     * @return object a new object to which an allocated monitor is permanently bound
     */
    @HOSTED_ONLY
    public static VmLock newVmLock(String name) {
        VmLock lock = new VmLock(name);
        bindStickyMonitor(lock, new StandardJavaMonitor());
        return lock;
    }

    /**
     * Class that can be used to instantiate a named lock that is used by the VM.
     * The name is useful in combination with the {@link Monitor#TraceMonitors}
     * VM option.
     */
    public static class VmLock {

        public static final ClassActor ACTOR = ClassActor.fromJava(VmLock.class);

        @INTRINSIC(UNSAFE_CAST)
        public static native VmLock asVmLock(Object object);

        public VmLock(String name) {
            this.name = name;
        }
        public final String name;
    }

    @HOSTED_ONLY
    private static void addToStickyMonitors(ManagedMonitor monitor) {
        stickyMonitors = Arrays.copyOf(stickyMonitors, stickyMonitors.length + 1);
        stickyMonitors[stickyMonitors.length - 1] = monitor;
    }

    @HOSTED_ONLY
    private static void addToBindableMonitors(ManagedMonitor monitor) {
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
        if (!MaxineVM.isHosted()) {
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
     * Lock used to synchronize access to the unbound monitor list.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private static Object LOCK;

    /**
     * Binds a monitor to the given object.
     * <p>
     * Important: the binding is not two-way at this stage; the monitor
     * points to the object, but the object does not know anything about the monitor.
     * <p>
     * TODO: 'bindMonitor' is a missing leading name. Refactor.
     *
     * @param object the object to bind a monitor to.
     * @return the monitor that was bound
     */
    public static ManagedMonitor bindMonitor(Object object) {
        ManagedMonitor monitor;
        if (inGlobalSafePoint) {
            monitor = takeFromUnboundList();
        } else {
            synchronized (LOCK) {
                if (numberOfUnboundMonitors < UNBOUNDLIST_MIN_QTY) {
                    System.gc();
                }
                // If we didn't free up enough such that we are at least midway between min and hwm, expand
                if (numberOfUnboundMonitors < (unboundMonitorsHwm + UNBOUNDLIST_MIN_QTY) >> 1) {
                    expandUnboundList();
                }
                monitor = takeFromUnboundList();
            }
        }
        monitor.setBoundObject(object);
        if (Monitor.TraceMonitors) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Bound monitor: ");
            monitor.log();
            Log.println();
            Log.unlock(lockDisabledSafepoints);
        }
        return monitor;
    }

    /**
     * Places the given monitor back into the free list.
     * <p>
     * Important: This should only be called for monitors that have
     * failed to be two-way bound to an object.
     * <p>
     * TODO: 'unbindMonitor' is a missing leading name. Refactor.
     * @param monitor the monitor to unbind
     */
    public static void unbindMonitor(JavaMonitor monitor) {
        final ManagedMonitor bindableMonitor = (ManagedMonitor) monitor;
        bindableMonitor.reset();
        if (inGlobalSafePoint) {
            addToUnboundList(bindableMonitor);
        } else {
            synchronized (LOCK) {
                addToUnboundList(bindableMonitor);
            }
        }
    }

    /**
     * Expands the list of unbound monitors by allocating and adding {@link #unboundListGrowQty} new
     * monitors to the list.
     */
    private static void expandUnboundList() {
        ManagedMonitor newUnboundList = null;
        final ManagedMonitor[] newAllBindable = new ManagedMonitor[bindableMonitors.length + unboundListGrowQty];

        // Create the new monitors
        for (int i = 0; i < unboundListGrowQty; i++) {
            final ManagedMonitor monitor = newManagedMonitor();
            monitor.setNext(newUnboundList);
            newUnboundList = monitor;
        }

        // This is the only place where we need to synchronize monitor list access
        // between a mutator thread and a GC thread which is performing unbinding.
        SafepointPoll.disable();
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
        SafepointPoll.enable();
    }

    /**
     * Notifies this JavaMonitorManager that the current thread is in-flight to
     * perform an operation on the given monitor. The monitor will
     * not be unbound until the current thread calls this method with a different
     * monitor, or the thread terminates.
     *
     * @param monitor the monitor whose binding to protect
     */
    public static void protectBinding(JavaMonitor monitor) {
        VmThread current = VmThread.current();
        current.protectedMonitor = monitor;
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

    private static class ProtectedMonitorGatherer implements Pointer.Procedure {
        public void run(Pointer tla) {
            VmThread thread = VmThread.fromTLA(tla);
            final JavaMonitor monitor = thread.protectedMonitor;
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
        VmThreadMap.ACTIVE.forAllThreadLocals(null, protectedMonitorGatherer);
        // Deflate all non-protected and non-sticky monitors with no owner
        for (int i = 0; i < numberOfBindableMonitors; i++) {
            final ManagedMonitor monitor = bindableMonitors[i];
            if (monitor.isHardBound() && monitor.bindingProtection() == BindingProtection.PRE_ACQUIRE) {
                monitor.setBindingProtection(BindingProtection.UNPROTECTED);
            }
            if (monitor.bindingProtection() == BindingProtection.UNPROTECTED) {
                if (Monitor.TraceMonitors) {
                    final boolean lockDisabledSafepoints = Log.lock();
                    Log.print("Unbinding monitor: ");
                    monitor.log();
                    Log.println();
                    Log.unlock(lockDisabledSafepoints);
                }
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
        for (ManagedMonitor monitor : stickyMonitors) {
            monitor.refreshBoundObject();
        }
    }

    /**
     * Extends the JavaMonitor interface to allow a pool of monitors to be managed by JavaMonitorManager.
     * <p>
     * TODO: (Simon) The terminology of binding/unbinding/hard-binding needs tidying up. The naming conventions
     * are not consistent and in places, misleading.
     *
     */
    interface ManagedMonitor extends JavaMonitor {

        /**
         * Defines the binding status of a monitor.
         *
         * TODO: (Simon) The terminology of binding/unbinding/hard-binding needs tidying up. The naming conventions
         * are not consistent and in places, misleading.
         *
         */
        enum BindingProtection {
            /**
             * The monitor is in the free-list, or it is one-way bound to an object.
             */
            PRE_ACQUIRE,

            /**
             * The monitor is two-way bound to an object (hard-bound), but can be safely unbound as it is no longer owned by a thread.
             */
            UNPROTECTED,

            /**
             * The monitor is two-way bound to an object (hard-bound), but cannot be unbound as it is owned by a thread.
             */
            PROTECTED
        }

        /**
         * Performs any native allocations necessary for this monitor.
         */
        void allocate();

        /**
         * Returns this monitor's bound object.
         *
         * @return the bound object
         */
        Object boundObject();

        /**
         * Refreshes the two-way binding of this monitor.
         */
        void refreshBoundObject();

        /**
         * Sets this monitor's bound object. The binding is one-way, i.e.
         * the monitor points to the object, but the object does not know anything about the monitor.
         *
         * @param object the object to bind
         */
        void setBoundObject(Object object);

        /**
         * Tests if this monitor is one-way bound, i.e. the monitor points to the object.
         *
         * @return true if this monitor is one-way bound; false otherwise
         */
        boolean isBound();

        /**
         * Tests if this monitor is two-way bound, i.e. the monitor points to the object.
         *
         * @return true if this monitor is two-way bound; false otherwise
         */
        boolean isHardBound();

        /**
         * Tests if this monitor was two-way (hard) bound prior to GC.
         *
         * @return true if this monitor was two-way (hard) bound prior to GC
         */
        boolean requiresPostGCRefresh();

        /**
         * Returns the BindingProtection for this monitor.
         *
         * @return the BindingProtection
         */
        BindingProtection bindingProtection();

        /**
         * Sets the BindingProtection for this monitor.
         *
         * @param protection the BindingProtection to set
         */
        void setBindingProtection(BindingProtection protection);

        /**
         * Sets this monitor to its default state.
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

        void log();
    }

}
