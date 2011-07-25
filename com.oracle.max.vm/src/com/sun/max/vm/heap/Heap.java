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
package com.sun.max.vm.heap;

import static com.sun.max.vm.VMConfiguration.*;
import static com.sun.max.vm.VMOptions.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * The dynamic Java object heap.
 */
public final class Heap {

    @HOSTED_ONLY
    public static boolean useOutOfLineStubs;

    // TODO: clean up. Just for indicating that boot image should be generated with inline TLAB allocation if heap scheme supports TLAB.
    @HOSTED_ONLY
    public static boolean genInlinedTLAB;

    private Heap() {
    }

    /**
     * This field is set to a non-zero value by the native code iff the
     * heap scheme returns a non-zero value for {@linkplain HeapScheme#reservedVirtualSpaceSize()}.
     */
    private static Address reservedVirtualSpace = Address.zero();

    private static final Size MIN_HEAP_SIZE = Size.M.times(4); // To be adjusted

    /**
     * If initial size not specified, then it is maxSize / DEFAULT_INIT_HEAP_SIZE_RATIO.
     */
    private static final int DEFAULT_INIT_HEAP_SIZE_RATIO = 2;

    public static final VMSizeOption maxHeapSizeOption = register(new VMSizeOption("-Xmx", Size.G, "The maximum heap size."), MaxineVM.Phase.PRISTINE);

    public static final VMSizeOption initialHeapSizeOption = register(new InitialHeapSizeOption(), MaxineVM.Phase.PRISTINE);

    static class InitialHeapSizeOption extends VMSizeOption {
        String invalidHeapSizeReason;

        @HOSTED_ONLY
        public InitialHeapSizeOption() {
            super("-Xms", maxHeapSizeOption.getValue().dividedBy(DEFAULT_INIT_HEAP_SIZE_RATIO), "The initial heap size.");
        }
        @Override
        public boolean check() {
            invalidHeapSizeReason = validateHeapSizing();
            return invalidHeapSizeReason == null;
        }

        @Override
        public void printErrorMessage() {
            Log.print(invalidHeapSizeReason);
        }
    }

    /**
     * Conveys the initial/max size for the heap. The {@code getXXXSize} methods can be overridden by an environment that has more knowledge.
     *
     */
    public static class HeapSizeInfo {
        protected Size getInitialSize() {
            return initialHeapSizeOption.getValue();
        }

        protected Size getMaxSize() {
            return maxHeapSizeOption.getValue();
        }
    }

    private static HeapSizeInfo heapSizeInfo = new HeapSizeInfo();

    @HOSTED_ONLY
    public static void registerHeapSizeInfo(HeapSizeInfo theHeapSizeInfo) {
        heapSizeInfo = theHeapSizeInfo;
    }

    /**
     * Lock for synchronizing access to the heap.
     */
    public static final Object HEAP_LOCK = JavaMonitorManager.newVmLock("HEAP_LOCK");

    private static Size maxSize;
    private static Size initialSize;

    private static boolean heapSizingInputValidated = false;

    /**
     * Start of the virtual space requested by the heap scheme and reserved at boot-load time.
     *
     * @return Address of reserved virtual space (not yet backed by swap space).
     */
    public static Address startOfReservedVirtualSpace() {
        return reservedVirtualSpace;
    }

    /**
     * Validate heap sizing inputs. This is common to any GC and can be done early on.
     *
     * @return
     */
    private static String validateHeapSizing() {
        if (heapSizingInputValidated) {
            return null;
        }
        Size max = heapSizeInfo.getMaxSize();
        Size init = heapSizeInfo.getInitialSize();
        if (maxHeapSizeOption.isPresent()) {
            if (max.lessThan(MIN_HEAP_SIZE)) {
                return "Heap too small";
            }
            if (initialHeapSizeOption.isPresent()) {
                if (max.lessThan(init)) {
                    return "Incompatible minimum and maximum heap sizes specified";
                }
                if (init.lessThan(MIN_HEAP_SIZE)) {
                    return "Too small initial heap";
                }
            } else {
                init = max;
            }
        } else if (initialHeapSizeOption.isPresent()) {
            if (init.lessThan(MIN_HEAP_SIZE)) {
                return "Heap too small";
            }
            max = init;
        }

        maxSize = max;
        initialSize = init;
        heapSizingInputValidated = true;
        return null;
    }

    // Note: Called via reflection from jvm.c
    public static long maxSizeLong() {
        return maxSize().toLong();
    }

    public static Size maxSize() {
        if (maxSize.isZero()) {
            validateHeapSizing();
        }
        return maxSize;
    }

    public static Size initialSize() {
        if (initialSize.isZero()) {
            validateHeapSizing();
        }
        return initialSize;
    }

    /**
     * Determines if information should be displayed about each garbage collection event.
     */
    public static boolean verbose() {
        return verboseOption.verboseGC || TraceGC || TraceRootScanning || Heap.traceGCTime();
    }

    /**
     * Set the verboseGC option (java.lang.management support).
     */
    public static void setVerbose(boolean value) {
        verboseOption.verboseGC = value;
    }

    private static boolean TraceAllocation;

    /**
     * Determines if allocation should be traced.
     *
     * @returns {@code false} if the VM build level is not {@link BuildLevel#DEBUG}.
     */
    @INLINE
    public static boolean traceAllocation() {
        return MaxineVM.isDebug() && TraceAllocation;
    }

    /**
     * Modifies the value of the flag determining if allocation should be traced. This flag is ignored if the VM
     * {@linkplain VMConfiguration#buildLevel() build level} is not {@link BuildLevel#DEBUG}. This is typically provided
     * so that error situations can be reported without being confused by interleaving allocation traces.
     */
    public static void setTraceAllocation(boolean flag) {
        TraceAllocation = flag;
    }

    static {
        if (MaxineVM.isDebug()) {
            VMOptions.addFieldOption("-XX:", "TraceAllocation", Classes.getDeclaredField(Heap.class, "TraceAllocation"), "Trace heap allocation.", MaxineVM.Phase.STARTING);
        }
    }

    /**
     * Determines if all garbage collection activity should be traced.
     */
    @INLINE
    public static boolean traceGC() {
        return TraceGC && TraceGCSuppressionCount <= 0;
    }

    /**
     * Determines if the garbage collection phases should be traced.
     */
    @INLINE
    public static boolean traceGCPhases() {
        return (TraceGC || TraceGCPhases) && TraceGCSuppressionCount <= 0;
    }

    /**
     * Determines if garbage collection root scanning should be traced.
     */
    @INLINE
    public static boolean traceRootScanning() {
        return (TraceGC || TraceRootScanning) && TraceGCSuppressionCount <= 0;
    }

    public static void setTraceRootScanning(boolean flag) {
        TraceRootScanning = flag;
    }

    /**
     * Determines if garbage collection timings should be collected and printed.
     */
    @INLINE
    public static boolean traceGCTime() {
        return (TraceGC || TimeGC) && TraceGCSuppressionCount <= 0;
    }

    /**
     * Disables -XX:-TraceGC, -XX:-TraceRootScanning and -XX:-TraceGCPhases if greater than 0.
     */
    public static int TraceGCSuppressionCount;

    private static boolean TraceGC;
    private static boolean TraceGCPhases;
    private static boolean TraceRootScanning;
    private static boolean TimeGC;
    private static boolean GCDisabled;

    static {
        VMOption timeOption = VMOptions.addFieldOption("-XX:", "TimeGC", Heap.class,
            "Time and print garbage collection activity.");

        VMOption traceGCPhasesOption = VMOptions.addFieldOption("-XX:", "TraceGCPhases", Heap.class,
            "Trace garbage collection phases.");

        VMOption traceRootScanningOption = VMOptions.addFieldOption("-XX:", "TraceRootScanning", Heap.class,
            "Trace garbage collection root scanning.");

        VMOption traceGCOption = VMOptions.addFieldOption("-XX:", "TraceGC", Heap.class,
            "Trace all garbage collection activity. Enabling this option also enables the " +
            traceRootScanningOption + ", " + traceGCPhasesOption + " and " + timeOption + " options.");

        VMOptions.addFieldOption("-XX:", "TraceGCSuppressionCount", Heap.class,
                        "Disable " + traceGCOption + ", " + traceRootScanningOption + " and " +
                        traceGCPhasesOption + " until the n'th GC");

        VMOptions.addFieldOption("-XX:", "DisableGC", Classes.getDeclaredField(Heap.class, "GCDisabled"), "Disable garbage collection.", MaxineVM.Phase.STARTING);
    }

    /**
     * Returns whether the "-XX:+DisableGC" option was specified.
     *
     * @return {@code true} if the user specified "-XX:+DisableGC" on the command line option; {@code false} otherwise
     */
    public static boolean gcDisabled() {
        return GCDisabled;
    }

    /**
     * Used by the Inspector to uniquely identify the special boot heap region.
     */
    @INSPECTED
    private static final String HEAP_BOOT_NAME = "Heap-Boot";

    @INSPECTED
    public static final BootHeapRegion bootHeapRegion = new BootHeapRegion(Address.zero(), Size.fromInt(Integer.MAX_VALUE), HEAP_BOOT_NAME);

    @FOLD
    private static HeapScheme heapScheme() {
        return vmConfig().heapScheme();
    }

    @INLINE
    public static void disableAllocationForCurrentThread() {
        heapScheme().disableAllocationForCurrentThread();
    }

    @INLINE
    public static void enableAllocationForCurrentThread() {
        heapScheme().enableAllocationForCurrentThread();
    }

    @INLINE
    public static boolean isAllocationDisabledForCurrentThread() {
        return heapScheme().isAllocationDisabledForCurrentThread();
    }

    /**
     * @see HeapScheme#isGcThread(Thread)
     */
    public static boolean isGcThread(Thread thread) {
        return heapScheme().isGcThread(thread);
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
    public static void traceCreateArray(DynamicHub hub, int length, final Object array) {
        final boolean lockDisabledSafepoints = Log.lock();
        Log.printCurrentThread(false);
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
        if (Heap.traceAllocation()) {
            traceCreateTuple(hub, object);
        }
        return object;
    }

    @NEVER_INLINE
    public static void traceCreateTuple(Hub hub, final Object object) {
        final boolean lockDisabledSafepoints = Log.lock();
        Log.printCurrentThread(false);
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
        Log.printCurrentThread(false);
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
        Log.printCurrentThread(false);
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
        Log.printCurrentThread(false);
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

    /**
     * A VM option for triggering a GC at fixed intervals.
     */
    public static int ExcessiveGCFrequency;
    static {
        VMOptions.addFieldOption("-XX:", "ExcessiveGCFrequency", Heap.class,
            "Run a garbage collection every <n> milliseconds. A value of 0 disables this mechanism.");
    }

    public static boolean collectGarbage(Size requestedFreeSpace) {
        if (Heap.gcDisabled()) {
            Throw.stackDump("Out of memory and GC is disabled");
            MaxineVM.native_exit(1);
        }
        if (VmThread.isAttaching()) {
            Log.println("Cannot run GC on a thread still attaching to the VM");
            MaxineVM.native_exit(1);
        }
        final long k = Size.K.toLong();
        long beforeFree = 0L;
        long beforeUsed = 0L;
        if (verbose()) {
            beforeUsed = reportUsedSpace();
            beforeFree = reportFreeSpace();
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("--GC requested by thread ");
            Log.printCurrentThread(false);
            Log.print(" for ");
            Log.print(requestedFreeSpace.toLong());
            Log.println(" bytes --");
            Log.print("--Before GC   used: ");
            Log.print(beforeUsed / k);
            Log.print(" Kb, free: ");
            Log.print(beforeFree / k);
            Log.println(" Kb --");
            Log.unlock(lockDisabledSafepoints);
        }
        final boolean freedEnough;
        if (VmThread.current().isVmOperationThread()) {
            // Even if another thread holds the heap lock for the purpose of executing a GC,
            // the GC is not actually executing as this is the VM operation thread which is
            // executing another VM operation that triggers a GC. So, the GC is now executed
            // as a nested VM operation without acquiring the heap lock.
            freedEnough = heapScheme().collectGarbage(requestedFreeSpace);
        } else {
            // Calls to collect garbage need to synchronize on the heap lock. This ensures that
            // GC operations are submitted serially to the VM operation thread. It also means
            // that a collection only actually occurs if needed (i.e. concurrent call to this
            // method by another thread did not trigger a GC that freed up enough memory for
            // this request).
            synchronized (HEAP_LOCK) {
                freedEnough = heapScheme().collectGarbage(requestedFreeSpace);
            }
        }
        if (verbose()) {
            final long afterUsed = reportUsedSpace();
            final long afterFree = reportFreeSpace();
            final long reclaimed = beforeUsed - afterUsed;
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("--GC requested by thread ");
            Log.printCurrentThread(false);
            Log.println(" done--");
            Log.print("--After GC   used: ");
            Log.print(afterUsed / k);
            Log.print(" Kb, free: ");
            Log.print(afterFree / k);
            Log.print(" Kb, reclaimed: ");
            Log.print(reclaimed / k);
            Log.println(" Kb --");
            if (freedEnough) {
                Log.println("--GC freed enough--");
            } else {
                Log.println("--GC did not free enough--");
            }
            Log.unlock(lockDisabledSafepoints);
        }
        return freedEnough;
    }

    // Note: Called via reflection from jvm.c
    public static long reportFreeSpace() {
        return heapScheme().reportFreeSpace().toLong();
    }

    // Note: Called via reflection from jvm.c
    public static long reportUsedSpace() {
        return heapScheme().reportUsedSpace().toLong();
    }

    // Note: Called via reflection from jvm.c
    public static long maxObjectInspectionAge() {
        return heapScheme().maxObjectInspectionAge();
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

    /**
     * Determines if the  heap scheme is initialized to the point where
     * {@link #collectGarbage(Size)} can safely be called.
     */
    public static boolean isInitialized() {
        return VmOperationThread.instance() != null;
    }

    public static void enableImmortalMemoryAllocation() {
        heapScheme().enableCustomAllocation(Word.allOnes().asAddress());
        if (ImmortalHeap.TraceImmortal) {
            Log.printCurrentThread(false);
            Log.println(": immortal heap allocation enabled");
        }
    }

    public static void disableImmortalMemoryAllocation() {
        heapScheme().disableCustomAllocation();
        if (ImmortalHeap.TraceImmortal) {
            Log.printCurrentThread(false);
            Log.println(": immortal heap allocation disabled");
        }
    }

    /**
     * Determines if a given object is in the boot image.
     *
     * @param object and object to check
     * @return true if {@code object} is in the boot image
     */
    public static boolean isInBootImage(Object object) {
        Pointer origin = Reference.fromJava(object).toOrigin();
        return bootHeapRegion.contains(origin) || Code.contains(origin) || ImmortalHeap.contains(origin);
    }

    public static boolean isValidRef(Reference ref) {
        if (ref.isZero()) {
            return true;
        }
        Pointer origin = ref.toOrigin();
        if (!bootHeapRegion.contains(origin) && !heapScheme().contains(origin) && !Code.contains(origin) && !ImmortalHeap.contains(origin)) {
            return false;
        }
        if (DebugHeap.isTagging()) {
            return DebugHeap.isValidNonnullRef(ref);
        }
        return true;
    }

    public static void checkHeapSizeOptions() {
        Size initSize = initialSize();
        Size maxSize = maxSize();
        if (initSize.greaterThan(maxSize)) {
            Log.println("Incompatible minimum and maximum heap sizes specified");
            MaxineVM.native_exit(1);
        }
    }
}
