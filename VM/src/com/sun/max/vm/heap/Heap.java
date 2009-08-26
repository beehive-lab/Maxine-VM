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

import static com.sun.max.vm.VMOptions.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.annotate.*;
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

    private static final VMSizeOption maxHeapSizeOption = register(new VMSizeOption("-Xmx", Size.G, "The maximum heap size."), MaxineVM.Phase.PRISTINE);

    private static final VMSizeOption initialHeapSizeOption = register(new InitialHeapSizeOption(), MaxineVM.Phase.PRISTINE);

    static class InitialHeapSizeOption extends VMSizeOption {
        @PROTOTYPE_ONLY
        public InitialHeapSizeOption() {
            super("-Xms", Size.M.times(512), "The initial heap size.");
        }
        @Override
        public boolean check() {
            return !(isPresent() && maxHeapSizeOption.isPresent() && getValue().greaterThan(maxHeapSizeOption.getValue()));
        }
        @Override
        public void printErrorMessage() {
            Log.print("initial heap size must not be greater than max heap size");
        }
    }

    private static final VMBooleanXXOption disableGCOption = register(new VMBooleanXXOption("-XX:-DisableGC", "Disable garbage collection."), MaxineVM.Phase.PRISTINE);

    private static Size maxSize;
    private static Size initialSize;

    public static Size maxSize() {
        if (maxSize.isZero()) {
            maxSize = maxSizeOption();
        }
        return maxSize;
    }

    public static void setMaxSize(Size size) {
        maxSize = size;
    }

    /**
     * Return the maximum heap size specified by the "-Xmx" command line option.
     * @return the size of the maximum heap specified on the command line
     */
    private static Size maxSizeOption() {
        if (maxHeapSizeOption.isPresent() || maxHeapSizeOption.getValue().greaterThan(initialHeapSizeOption.getValue())) {
            return maxHeapSizeOption.getValue();
        }
        return initialHeapSizeOption.getValue();
    }

    public static boolean maxSizeOptionIsPresent() {
        return maxHeapSizeOption.isPresent();
    }

    public static Size initialSize() {
        if (initialSize.isZero()) {
            initialSize = initialSizeOption();
        }
        return initialSize;
    }

    public static void setInitialSize(Size size) {
        initialSize = size;
    }

    /**
     * Return the initial heap size specified by the "-Xms" command line option.
     * @return the size of the initial heap specified on the command line
     */
    private static Size initialSizeOption() {
        if (initialHeapSizeOption.isPresent() || initialHeapSizeOption.getValue().lessThan(maxHeapSizeOption.getValue())) {
            return initialHeapSizeOption.getValue();
        }
        return maxHeapSizeOption.getValue();
    }

    public static boolean initialSizeOptionIsPresent() {
        return initialHeapSizeOption.isPresent();
    }

    /**
     * Determines if information should be displayed about each garbage collection event.
     */
    public static boolean verbose() {
        return verboseOption.verboseGC || traceGC || traceRootScanningOption.getValue() || Heap.traceGCTime() || traceGC;
    }

    private static boolean traceAllocation;

    /**
     * Determines if allocation should be traced.
     *
     * @returns {@code false} if the VM build level is not {@link BuildLevel#DEBUG}.
     */
    @INLINE
    public static boolean traceAllocation() {
        if (!MaxineVM.isDebug()) {
            return false;
        }
        return traceAllocation;
    }

    /**
     * Modifies the value of the flag determining if allocation should be traced. This flag is ignored if the VM
     * {@linkplain VMConfiguration#buildLevel() build level} is not {@link BuildLevel#DEBUG}. This is typically provided
     * so that error situations can be reported without being confused by interleaving allocation traces.
     */
    public static void setTraceAllocation(boolean flag) {
        traceAllocation = flag;
    }

    static {
        if (MaxineVM.isDebug()) {
            register(new VMBooleanXXOption("-XX:-TraceAllocation", "Trace heap allocation.") {
                @Override
                public boolean parseValue(Pointer optionValue) {
                    traceAllocation = getValue();
                    return true;
                }
            }, MaxineVM.Phase.STARTING);
        }
    }

    /**
     * Determines if all garbage collection activity should be traced.
     */
    @INLINE
    public static boolean traceGC() {
        return traceGC;
    }

    /**
     * Determines if the garbage collection phases should be traced.
     */
    @INLINE
    public static boolean traceGCPhases() {
        return traceGC || traceGCPhasesOption.getValue();
    }

    /**
     * Determines if garbage collection root scanning should be traced.
     */
    @INLINE
    public static boolean traceRootScanning() {
        return traceGC || traceRootScanningOption.getValue();
    }

    /**
     * Determines if garbage collection timings should be collected and printed.
     */
    @INLINE
    public static boolean traceGCTime() {
        return traceGC || timeOption.getValue();
    }

    private static boolean traceGC;

    private static final VMBooleanXXOption traceGCPhasesOption = register(new VMBooleanXXOption("-XX:-TraceGCPhases",
        "Trace garbage collection phases."), MaxineVM.Phase.STARTING);

    private static final VMBooleanXXOption traceRootScanningOption = register(new VMBooleanXXOption("-XX:-TraceRootScanning",
        "Trace garbage collection root scanning."), MaxineVM.Phase.STARTING);

    private static final VMBooleanXXOption timeOption = register(new VMBooleanXXOption("-XX:-TimeGC",
        "Time and print garbage collection activity."), MaxineVM.Phase.STARTING);

    private static final class TraceGCOption extends VMBooleanXXOption {
        TraceGCOption() {
            super("-XX:-TraceGC", "Trace all garbage collection activity. Enabling this option also enables the " +
                traceRootScanningOption + ", " + traceGCPhasesOption + " and " + timeOption + " options.");
        }

        @Override
        public boolean parseValue(Pointer optionValue) {
            if (getValue()) {
                traceGC = true;
            }
            return true;
        }
    }

    private static final VMBooleanXXOption traceGCOption = register(new TraceGCOption(), MaxineVM.Phase.STARTING);

    /**
     * Returns whether the "-XX:+DisableGC" option was specified.
     *
     * @return {@code true} if the user specified the "-XX:+DisableGC" on the command line option; {@code false} otherwise
     */
    public static boolean gcDisabled() {
        return disableGCOption.getValue();
    }

    @INSPECTED
    public static final BootHeapRegion bootHeapRegion = new BootHeapRegion(Address.zero(), Size.fromInt(Integer.MAX_VALUE), "Heap-Boot");

    @UNSAFE
    @FOLD
    private static HeapScheme heapScheme() {
        return VMConfiguration.hostOrTarget().heapScheme();
    }

    @INLINE
    public static void disableAllocationForCurrentThread() {
        heapScheme().disableAllocationForCurrentThread();
    }

    @INLINE
    public static void enableAllocationForCurrentThread() {
        heapScheme().enableAllocationForCurrentThread();
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

    @INLINE
    public static Object createArray(DynamicHub hub, int length) {
        final Object array = heapScheme().createArray(hub, length);
        if (ImmortalHeap.traceAllocation()) {
            final Pointer enabledVmThreadLocals = VmThread.currentVmThreadLocals().getWord(VmThreadLocal.SAFEPOINTS_ENABLED_THREAD_LOCALS.index).asPointer();
            final Pointer immortalAllocation = enabledVmThreadLocals.getWord(IMMORTAL_ALLOCATION.index).asPointer();
            if (immortalAllocation.equals(Word.allOnes())) {
                Log.print("Immortal heap allocation: ");
                traceCreateArray(hub, length, array);
                return array;
            }
        }
        if (Heap.traceAllocation()) {
            traceCreateArray(hub, length, array);
        }
        return array;
    }

    @NEVER_INLINE
    public static void traceCreateArray(DynamicHub hub, int length, final Object array) {
        final boolean lockDisabledSafepoints = Log.lock();
        Log.printVmThread(VmThread.current(), false);
        Log.print(": Allocated array ");
        Log.print(hub.classActor.name.string);
        Log.print(" of length ");
        Log.print(length);
        Log.print(" at ");
        Log.print(Layout.originToCell(ObjectAccess.toOrigin(array)));
        Log.print(" [");
        Log.print(Layout.size(Reference.fromJava(array)).toInt());
        Log.println(" bytes]");
        Log.unlock(lockDisabledSafepoints);
    }

    @INLINE
    public static Object createTuple(Hub hub) {
        final Object object = heapScheme().createTuple(hub);
        if (ImmortalHeap.traceAllocation()) {
            final Pointer enabledVmThreadLocals = VmThread.currentVmThreadLocals().getWord(VmThreadLocal.SAFEPOINTS_ENABLED_THREAD_LOCALS.index).asPointer();
            final Pointer immortalAllocation = enabledVmThreadLocals.getWord(IMMORTAL_ALLOCATION.index).asPointer();
            if (immortalAllocation.equals(Word.allOnes())) {
                Log.print("Immortal heap allocation: ");
                traceCreateTuple(hub, object);
                return object;
            }
        }
        if (Heap.traceAllocation()) {
            traceCreateTuple(hub, object);
        }
        return object;
    }

    @NEVER_INLINE
    public static void traceCreateTuple(Hub hub, final Object object) {
        final boolean lockDisabledSafepoints = Log.lock();
        Log.printVmThread(VmThread.current(), false);
        Log.print(": Allocated tuple ");
        Log.print(hub.classActor.name.string);
        Log.print(" at ");
        Log.print(Layout.originToCell(ObjectAccess.toOrigin(object)));
        Log.print(" [");
        Log.print(hub.tupleSize.toInt());
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
        Log.printVmThread(VmThread.current(), false);
        Log.print(": Allocated hybrid ");
        Log.print(hub.classActor.name.string);
        Log.print(" at ");
        Log.print(Layout.originToCell(ObjectAccess.toOrigin(hybrid)));
        Log.print(" [");
        Log.print(hub.tupleSize.toInt());
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
        Log.printVmThread(VmThread.current(), false);
        Log.print(": Allocated expanded hybrid ");
        final Hub hub = ObjectAccess.readHub(hybrid);
        Log.print(hub.classActor.name.string);
        Log.print(" at ");
        Log.print(Layout.originToCell(ObjectAccess.toOrigin(expandedHybrid)));
        Log.print(" [");
        Log.print(hub.tupleSize.toInt());
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
        Log.printVmThread(VmThread.current(), false);
        Log.print(": Allocated cloned ");
        final Hub hub = ObjectAccess.readHub(object);
        Log.print(hub.classActor.name.string);
        Log.print(" at ");
        Log.print(Layout.originToCell(ObjectAccess.toOrigin(clone)));
        Log.print(" [");
        Log.print(hub.tupleSize.toInt());
        Log.println(" bytes]");
        Log.unlock(lockDisabledSafepoints);
    }

    @INLINE
    public static boolean contains(Address address) {
        return heapScheme().contains(address);
    }

    public static boolean collectGarbage(Size requestedFreeSpace) {
        long beforeFree = 0L;
        long beforeUsed = 0L;
        if (verbose()) {
            beforeUsed = reportUsedSpace().toLong();
            beforeFree = reportFreeSpace().toLong();
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("--GC requested by thread ");
            Log.printVmThread(VmThread.current(), false);
            Log.println("--");
            Log.print("--Before GC   used: ");
            Log.print(beforeUsed);
            Log.print(", free: ");
            Log.print(beforeFree);
            Log.println("--");
            Log.unlock(lockDisabledSafepoints);
        }
        final boolean freedEnough = heapScheme().collectGarbage(requestedFreeSpace);
        if (verbose()) {
            final long afterUsed = reportUsedSpace().toLong();
            final long afterFree = reportFreeSpace().toLong();
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("--GC requested by thread ");
            Log.printVmThread(VmThread.current(), false);
            Log.println(" done--");
            Log.print("--After GC   used: ");
            Log.print(afterUsed);
            Log.print(", free: ");
            Log.print(afterFree);
            Log.print(", reclaimed: ");
            Log.print(beforeUsed - afterUsed);
            Log.println("--");
            if (freedEnough) {
                Log.println("--GC freed enough--");
            } else {
                Log.println("--GC did not free enough--");
            }
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

    public static void enableImmortalMemoryAllocation() {
        heapScheme().enableImmortalMemoryAllocation();
        if (ImmortalHeap.traceAllocation()) {
            Log.printVmThread(VmThread.current(), false);
            Log.println(": immortal heap allocation enabled");
        }
    }

    public static void disableImmortalMemoryAllocation() {
        heapScheme().disableImmortalMemoryAllocation();
        if (ImmortalHeap.traceAllocation()) {
            Log.printVmThread(VmThread.current(), false);
            Log.println(": immortal heap allocation disabled");
        }
    }
}
