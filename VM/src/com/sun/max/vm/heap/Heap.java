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
package com.sun.max.vm.heap;

import com.sun.max.annotate.*;
import com.sun.max.profile.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.thread.*;

/**
 * The dynamic Java object heap.
 *
 * @author Bernd Mathiske
 */
public final class Heap {

    private Heap() {
    }

    private static final VMSizeOption _maxHeapSizeOption = new VMSizeOption("-Xmx", Size.G, "The maximum heap size.", MaxineVM.Phase.PRISTINE);

    private static final VMSizeOption _initialHeapSizeOption = new InitialHeapSizeOption();

    static class InitialHeapSizeOption extends VMSizeOption {
        @PROTOTYPE_ONLY
        public InitialHeapSizeOption() {
            super("-Xms", Size.M.times(512), "The initial heap size.", MaxineVM.Phase.PRISTINE);
        }
        @Override
        public boolean check() {
            return !(isPresent() && _maxHeapSizeOption.isPresent() && getValue().greaterThan(_maxHeapSizeOption.getValue()));
        }
        @Override
        public void printErrorMessage() {
            Log.print("initial heap size must not be greater than max heap size");
        }
    }

    private static final VMOption _disableGCOption = new VMOption("-XX:DisableGC", "Disable garbage collection.", MaxineVM.Phase.PRISTINE);

    private static Size _maxSize;
    private static Size _initialSize;

    public static Size maxSize() {
        if (_maxSize.isZero()) {
            _maxSize = maxSizeOption();
        }
        return _maxSize;
    }

    public static void setMaxSize(Size size) {
        _maxSize = size;
    }

    /**
     * Return the maximum heap size specified by the "-Xmx" command line option.
     * @return the size of the maximum heap specified on the command line
     */
    private static Size maxSizeOption() {
        if (_maxHeapSizeOption.isPresent() || _maxHeapSizeOption.getValue().greaterThan(_initialHeapSizeOption.getValue())) {
            return _maxHeapSizeOption.getValue();
        }
        return _initialHeapSizeOption.getValue();
    }

    public static boolean maxSizeOptionIsPresent() {
        return _maxHeapSizeOption.isPresent();
    }

    public static Size initialSize() {
        if (_initialSize.isZero()) {
            _initialSize = initialSizeOption();
        }
        return _initialSize;
    }

    public static void setInitialSize(Size size) {
        _initialSize = size;
    }

    /**
     * Return the initial heap size specified by the "-Xms" command line option.
     * @return the size of the initial heap specified on the command line
     */
    private static Size initialSizeOption() {
        if (_initialHeapSizeOption.isPresent() || _initialHeapSizeOption.getValue().lessThan(_maxHeapSizeOption.getValue())) {
            return _initialHeapSizeOption.getValue();
        }
        return _maxHeapSizeOption.getValue();
    }

    public static boolean initialSizeOptionIsPresent() {
        return _initialHeapSizeOption.isPresent();
    }

    /**
     * Returns whether the "-verbose:gc" option was specified.
     * @return {@code true} if the user specified the "-verbose:gc" command line option; {@code false}
     * otherwise
     */
    public static boolean verbose() {
        return VerboseVMOption.verboseGC();
    }

    private static boolean _traceAllocation;

    /**
     * Determines if allocation should be traced.
     *
     * @returns {@code false} if VM build level is not {@link BuildLevel#DEBUG}.
     */
    @INLINE
    public static boolean traceAllocation() {
        if (!VMConfiguration.hostOrTarget().debugging()) {
            return false;
        }
        return _traceAllocation;
    }

    private static final VMOption _traceAllocationOption;
    static {
        if (!VMConfiguration.hostOrTarget().debugging()) {
            _traceAllocationOption = null;
        } else {
            _traceAllocationOption = new VMOption("-XX:TraceAllocation", "Trace heap allocation.", MaxineVM.Phase.STARTING) {
                @Override
                public boolean parseValue(Pointer optionValue) {
                    _traceAllocation = true;
                    return true;
                }
            };
        }
    }

    /**
     * Determines if all garbage collection activity should be traced.
     */
    @INLINE
    public static boolean traceGC() {
        return _traceGC;
    }

    /**
     * Determines if garbage collection root scanning should be traced.
     */
    @INLINE
    public static boolean traceGCRootScanning() {
        return _traceGCRootScanning;
    }

    /**
     * Determines if garbage collection timings should be printed.
     */
    @INLINE
    public static boolean traceGCTime() {
        return _traceGCTime;
    }

    /**
     * The clock that specifies the timing resolution for GC related timing.
     */
    public static final Clock GC_TIMING_CLOCK = Clock.SYSTEM_MILLISECONDS;

    private static boolean _traceGC;
    private static boolean _traceGCRootScanning;
    private static boolean _traceGCTime;

    private static final VMOption _traceGCOption = new VMOption("-XX:TraceGC", "Trace garbage collection activity.", MaxineVM.Phase.STARTING) {
        @Override
        public boolean parseValue(Pointer optionValue) {
            if (CString.equals(optionValue, "")) {
                _traceGC = true;
                _traceGCRootScanning = true;
                _traceGCTime = true;
            } else if (CString.equals(optionValue, ":RootScanning")) {
                _traceGCRootScanning = true;
            } else if (CString.equals(optionValue, ":Time")) {
                _traceGCTime = true;
            } else {
                return false;
            }
            return true;
        }
        @Override
        public void printHelp() {
            VMOptions.printHelpForOption("-XX:TraceGC[:RootScanning|:Time]", "", _help);
        }
    };

    /**
     * Returns whether the "-XX:DisableGC" option was specified.
     *
     * @return {@code true} if the user specified the "-XX:DisableGC" command line option; {@code false}
     * otherwise
     * @return
     */
    public static boolean gcDisabled() {
        return _disableGCOption.isPresent();
    }

    @INSPECTED
    private static final LinearAllocatorHeapRegion _bootHeapRegion = new LinearAllocatorHeapRegion(Address.zero(), Size.fromInt(Integer.MAX_VALUE), "Heap-Boot");

    @INLINE
    public static LinearAllocatorHeapRegion bootHeapRegion() {
        return _bootHeapRegion;
    }

    @UNSAFE
    @FOLD
    private static HeapScheme heapScheme() {
        return VMConfiguration.hostOrTarget().heapScheme();
    }

    /**
     * @see HeapScheme#isGcThread(Thread)
     */
    public static boolean isGcThread(Thread thread) {
        return heapScheme().isGcThread(thread);
    }

    public static void initializeAuxiliarySpace(Pointer primordialVmThreadLocals, Pointer auxiliarySpace) {
        heapScheme().initializeAuxiliarySpace(primordialVmThreadLocals, auxiliarySpace);
    }

    public static void initializeVmThread(Pointer vmThreadLocals) {
        heapScheme().initializeVmThread(vmThreadLocals);
    }

    @INLINE
    public static Object createArray(DynamicHub hub, int length) {
        final Object array = heapScheme().createArray(hub, length);
        if (Heap.traceAllocation()) {
            traceCreateArray(hub, length, array);
        }
        return array;
    }

    @NEVER_INLINE
    private static void traceCreateArray(DynamicHub hub, int length, final Object array) {
        final boolean lockDisabledSafepoints = Log.lock();
        Log.print("Allocated array ");
        Log.print(hub.classActor().name().string());
        Log.print(" of length ");
        Log.print(length);
        Log.print(" at ");
        Log.print(Layout.originToCell(ObjectAccess.toOrigin(array)));
        Log.print(" [");
        Log.print(Layout.size(Reference.fromJava(array)));
        Log.println(" bytes]");
        Log.unlock(lockDisabledSafepoints);
    }

    @INLINE
    public static Object createTuple(Hub hub) {
        final Object object = heapScheme().createTuple(hub);
        if (Heap.traceAllocation()) {
            traceCreateTuple(hub, object);
        }
        return object;
    }

    @NEVER_INLINE
    private static void traceCreateTuple(Hub hub, final Object object) {
        final boolean lockDisabledSafepoints = Log.lock();
        Log.print("Allocated tuple ");
        Log.print(hub.classActor().name().string());
        Log.print(" at ");
        Log.print(Layout.originToCell(ObjectAccess.toOrigin(object)));
        Log.print(" [");
        Log.print(hub.tupleSize().toInt());
        Log.println(" bytes]");
        Log.unlock(lockDisabledSafepoints);
    }

    @INLINE
    public static Object createHybrid(DynamicHub hub) {
        final Object hybrid = heapScheme().createHybrid(hub);
        if (Heap.traceAllocation()) {
            traceCreateHybrid(hub, hybrid);
        }
        return hybrid;
    }

    @NEVER_INLINE
    private static void traceCreateHybrid(DynamicHub hub, final Object hybrid) {
        final boolean lockDisabledSafepoints = Log.lock();
        Log.print("Allocated hybrid ");
        Log.print(hub.classActor().name().string());
        Log.print(" at ");
        Log.print(Layout.originToCell(ObjectAccess.toOrigin(hybrid)));
        Log.print(" [");
        Log.print(hub.tupleSize().toInt());
        Log.println(" bytes]");
        Log.unlock(lockDisabledSafepoints);
    }

    @INLINE
    public static Hybrid expandHybrid(Hybrid hybrid, int length) {
        final Hybrid expandedHybrid = heapScheme().expandHybrid(hybrid, length);
        if (Heap.traceAllocation()) {
            traceExpandHybrid(hybrid, expandedHybrid);
        }
        return expandedHybrid;
    }

    @NEVER_INLINE
    private static void traceExpandHybrid(Hybrid hybrid, final Hybrid expandedHybrid) {
        final boolean lockDisabledSafepoints = Log.lock();
        Log.print("Allocated expanded hybrid ");
        final Hub hub = ObjectAccess.readHub(hybrid);
        Log.print(hub.classActor().name().string());
        Log.print(" at ");
        Log.print(Layout.originToCell(ObjectAccess.toOrigin(expandedHybrid)));
        Log.print(" [");
        Log.print(hub.tupleSize().toInt());
        Log.println(" bytes]");
        Log.unlock(lockDisabledSafepoints);
    }

    @INLINE
    public static Object clone(Object object) {
        final Object clone = heapScheme().clone(object);
        if (Heap.traceAllocation()) {
            traceClone(object, clone);
        }
        return clone;
    }

    @NEVER_INLINE
    private static void traceClone(Object object, final Object clone) {
        final boolean lockDisabledSafepoints = Log.lock();
        Log.print("Allocated cloned ");
        final Hub hub = ObjectAccess.readHub(object);
        Log.print(hub.classActor().name().string());
        Log.print(" at ");
        Log.print(Layout.originToCell(ObjectAccess.toOrigin(clone)));
        Log.print(" [");
        Log.print(hub.tupleSize().toInt());
        Log.println(" bytes]");
        Log.unlock(lockDisabledSafepoints);
    }

    @INLINE
    public static boolean contains(Address address) {
        return heapScheme().contains(address);
    }

    private static boolean _collecting;

    public static boolean collectGarbage(Size requestedFreeSpace) {
        if (verbose()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("--GC requested by thread ");
            Log.printVmThread(VmThread.current(), false);
            Log.println("--");
            Log.print("--Before GC--   used: ");
            Log.print(reportUsedSpace().toLong());
            Log.print(", free: ");
            Log.print(reportFreeSpace().toLong());
            Log.println("--");
            Log.unlock(lockDisabledSafepoints);
        }
        final boolean freedEnough = heapScheme().collectGarbage(requestedFreeSpace);
        if (verbose()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("--After GC--   used: ");
            Log.print(reportUsedSpace().toLong());
            Log.print(", free: ");
            Log.print(reportFreeSpace().toLong());
            Log.println("--");
            Log.unlock(lockDisabledSafepoints);
        }
        return freedEnough;
    }

    public static Size reportFreeSpace() {
        return heapScheme().reportFreeSpace();
    }

    public static Size reportUsedSpace() {
        return heapScheme().reportUsedSpace();
    }

    public static void runFinalization() {
        heapScheme().runFinalization();
    }

    @INLINE
    public static boolean pin(Object object) {
        return heapScheme().pin(object);
    }

    @INLINE
    public static void unpin(Object object) {
        heapScheme().unpin(object);
    }

    @INLINE
    public static boolean isPinned(Object object) {
        return heapScheme().isPinned(object);
    }
}
