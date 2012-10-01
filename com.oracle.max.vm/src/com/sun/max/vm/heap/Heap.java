/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
import static com.sun.max.vm.heap.HeapSchemeAdaptor.*;
import static com.sun.max.vm.thread.VmThread.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.heap.HeapScheme.PIN_SUPPORT_FLAG;
import com.sun.max.vm.heap.debug.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.log.*;
import com.sun.max.vm.log.VMLog.Record;
import com.sun.max.vm.log.hosted.*;
import com.sun.max.vm.monitor.*;
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

    @HOSTED_ONLY
    private static Map<Object, Address> objectToCell;

    @HOSTED_ONLY
    public static Address objectToCell(Object object) {
        return objectToCell.get(object);
    }

    @HOSTED_ONLY
    public static void initObjectToCell(Map<Object, Address> map) {
        objectToCell = map;
    }

    private Heap() {
    }

    /**
     * This field is set to a non-zero value by the native code iff the
     * heap scheme returns a non-zero value for {@linkplain HeapScheme#reservedVirtualSpaceKB()}.
     */
    private static Address reservedVirtualSpace = Address.zero();

    private static final Size MIN_HEAP_SIZE = Size.M.times(4); // To be adjusted

    /**
     * If initial size not specified, then it is maxSize / DEFAULT_INIT_HEAP_SIZE_RATIO.
     */
    private static final int DEFAULT_INIT_HEAP_SIZE_RATIO = 2;

    public static final VMSizeOption maxHeapSizeOption = register(new VMSizeOption("-Xmx", Size.G, "The maximum heap size."), MaxineVM.Phase.PRISTINE);

    public static final VMSizeOption initialHeapSizeOption = register(new InitialHeapSizeOption(), MaxineVM.Phase.PRISTINE);

    /**
     * Avoid using commit / uncommit operations that relies on anonymous memory operations.
     * These have been shown to be very expensive (on Solaris) although the cause isn't clear yet.
     * We keep this until we understand better the performance issues.
     */
    public static boolean AvoidsAnonOperations = true;

    public static boolean OptimizeJNICritical = true;

    static {
        VMOptions.addFieldOption("-XX:", "OptimizeJNICritical", Heap.class, "Use GC disabling to optimize JNI 'critical' functions when heap scheme doesn't support object pinning.", MaxineVM.Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "AvoidsAnonOperations", Heap.class, "Avoids using Anonymous Memory operations as much as possible.", MaxineVM.Phase.PRISTINE);
    }

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

    public static boolean holdsHeapLock() {
        return Monitor.threadHoldsMonitor(HEAP_LOCK, VmThread.current());
    }

    private static Size maxSize;
    private static Size initialSize;

    private static boolean heapSizingInputValidated = false;

    /**
     * Start of the virtual space requested by the heap scheme and reserved at boot-load time.
     * The reserved space isn't backed yet by swap space (i.e., the memory is uncommitted).
     * The start or end of the reserved virtual space may comprise the boot image.
     *
     * @return Address of reserved virtual space.
     */
    public static Address startOfReservedVirtualSpace() {
        return reservedVirtualSpace;
    }

    /**
     * Validate heap sizing inputs. This is common to any GC and can be done early on.
     *
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
     * Beyond the {@link VMOptions#verboseOption}, this must depend on tracing being enabled, not logging,
     * as it will generate output.
     */
    public static boolean verbose() {
        return verboseOption.verboseGC || gcAllLogger.traceEnabled() ||
               rootScanLogger.traceEnabled() || (timeLogger.traceEnabled() && LogGCSuppressionCount <= 0);
    }

    /**
     * Set the verboseGC option (java.lang.management support).
     */
    public static void setVerbose(boolean value) {
        verboseOption.verboseGC = value;
    }

    private static boolean GCDisabled;

    static {
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
        if (Heap.logAllocation()) {
            allocationLogger.logCreateArray(hub, length, array);
        }
        return array;
    }

    @INLINE
    public static Object createTuple(Hub hub) {
        final Object object = heapScheme().createTuple(hub);
        if (Heap.logAllocation()) {
            allocationLogger.logCreateTuple(hub, object);
        }
        return object;
    }

    @INLINE
    public static Object createHybrid(DynamicHub hub) {
        final Object hybrid = heapScheme().createHybrid(hub);
        if (Heap.logAllocation()) {
            allocationLogger.logCreateHybrid(hub, hybrid);
        }
        return hybrid;
    }

    @INLINE
    public static Hybrid expandHybrid(Hybrid hybrid, int length) {
        final Hybrid expandedHybrid = heapScheme().expandHybrid(hybrid, length);
        if (Heap.logAllocation()) {
            allocationLogger.logExpandHybrid(ObjectAccess.readHub(hybrid), expandedHybrid);
        }
        return expandedHybrid;
    }

    @INLINE
    public static Object clone(Object object) {
        final Object clone = heapScheme().clone(object);
        if (Heap.logAllocation()) {
            allocationLogger.logClone(ObjectAccess.readHub(object), clone);
        }
        return clone;
    }

    /**
     * A VM option for triggering a GC at fixed intervals.
     */
    public static int ExcessiveGCFrequency;
    static {
        VMOptions.addFieldOption("-XX:", "ExcessiveGCFrequency", Heap.class,
            "Run a garbage collection every <n> milliseconds. A value of 0 disables this mechanism.");
    }

    public static boolean collectGarbage() {
        if (Heap.gcDisabled()) {
            Throw.stackDump("Out of memory and GC is disabled");
            MaxineVM.native_exit(1);
        }
        if (VmThread.isAttaching()) {
            Log.println("Cannot run GC on a thread still attaching to the VM");
            MaxineVM.native_exit(1);
        }
        if (VmThread.current().isVmOperationThread()) {
            // Even if another thread holds the heap lock for the purpose of executing a GC,
            // the GC is not actually executing as this is the VM operation thread which is
            // executing another VM operation that triggers a GC. So, the GC is now executed
            // as a nested VM operation without acquiring the heap lock.
            VmOperationThread.instance().promoteToGlobalSafepoint();

            // We're at a global safepoint, no one can concurrently update disableGCThreadCount.
            // It is possible that a VM operation (that isn't a GC operation) was running while mutator threads
            // disabled the GC. If that is the case, the current GC operation is illegal.
            FatalError.check(disableGCThreadCount == 0, "GC must be enabled");
            return heapLockedCollectGarbage();
        } else {
            // Calls to collect garbage need to synchronize on the heap lock. This ensures that
            // GC operations are submitted serially to the VM operation thread. It also means
            // that a collection only actually occurs if needed (i.e. concurrent call to this
            // method by another thread did not trigger a GC that freed up enough memory for
            // this request).
            synchronized (HEAP_LOCK) {
                waitForGCDisablingThreads();
                return heapLockedCollectGarbage();
            }
        }
    }

    private static boolean heapLockedCollectGarbage() {
        if (verbose()) {
            VmThread.current().gcRequest.printBeforeGC();
        }
        final boolean result = heapScheme().collectGarbage();
        if (verbose()) {
            VmThread.current().gcRequest.printAfterGC(result);
        }
        return result;
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

    public static void lock() {
        Monitor.enter(HEAP_LOCK);
    }

    public static void unlock() {
        Monitor.exit(HEAP_LOCK);
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
        if (ImmortalHeap.immortalHeapLogger.enabled()) {
            ImmortalHeap.immortalHeapLogger.logEnable();
        }
    }

    public static void disableImmortalMemoryAllocation() {
        heapScheme().disableCustomAllocation();
        if (ImmortalHeap.immortalHeapLogger.enabled()) {
            ImmortalHeap.immortalHeapLogger.logDisable();
        }
    }

    /**
     * Counter of threads that are disabling GC.
     * The counter is increased / decreased only when the thread local count changes from zero to one (and vice-versa).
     *
     * @see Heap#disableGC()
     * @see Heap#enableGC()
     */
    private static int disableGCThreadCount = 0;

    /**
     * Flag indicating that the GC is waiting for GC-disabling threads.
     * This is currently used to implement an optimistic form of object pinning for heap schemes that don't
     * support it, wherein threads disable GC while holding direct pointers to arrays.
     *
     * @see Heap#disableGC()
     * @see Heap#enableGC()
     */
    private static boolean gcWaitForDisablingThreads = false;

    /**
     * Disable GC. Must be paired with a subsequent call to {@link Heap#enableGC()}
     */
    @INLINE
    private static void disableGC() {
        final Pointer etla = ETLA.load(currentTLA());
        Pointer count = GC_DISABLING_COUNT.load(etla);
        if (count.isZero()) {
            synchronized (HEAP_LOCK) {
                disableGCThreadCount++;
            }
        }
        GC_DISABLING_COUNT.store(etla, count.plus(1));
    }

    /**
     * Enable GC. Must be paired with a previous call to {@link Heap#disableGC()}
     */
    @INLINE
    private static void enableGC() {
        final Pointer etla = ETLA.load(currentTLA());
        Pointer count = GC_DISABLING_COUNT.load(etla);
        assert count.greaterThan(Pointer.zero()) :  "thread has not issued a GC disabling request";
        if (count.equals(1)) {
            synchronized (HEAP_LOCK) {
                assert disableGCThreadCount > 0 : "some thread have not issued a GC disabling request";
                disableGCThreadCount--;
                if (disableGCThreadCount == 0 && gcWaitForDisablingThreads) {
                    // Wake up GC if waiting on the HEAP lock.
                    HEAP_LOCK.notifyAll();
                }
            }
        }
        GC_DISABLING_COUNT.store(etla, count.minus(1));
    }

    private static void waitForGCDisablingThreads() {
        while (disableGCThreadCount > 0) {
            gcWaitForDisablingThreads = true;
            final Pointer etla = ETLA.load(currentTLA());
            FatalError.check(GC_DISABLING_COUNT.load(etla).equals(0), "GC requester must not pin any objects");
            try {
                HEAP_LOCK.wait();
            } catch (InterruptedException e) {
            }
        }
        gcWaitForDisablingThreads = false;
    }

    @INLINE
    public static boolean useDirectPointer(Object object) {
        HeapScheme heapScheme = heapScheme();
        if (heapScheme.supportsPinning(PIN_SUPPORT_FLAG.CAN_NEST)) {
            heapScheme.pin(object);
            return true;
        }
        if (OptimizeJNICritical) {
            disableGC();
            return true;
        }
        return false;
    }

    @INLINE
    public static boolean releasedDirectPointer(Object object) {
        HeapScheme heapScheme = VMConfiguration.vmConfig().heapScheme();
        if (heapScheme.supportsPinning(PIN_SUPPORT_FLAG.CAN_NEST)) {
            heapScheme.unpin(object);
            return true;
        }
        if (OptimizeJNICritical) {
            enableGC();
            return true;
        }
        return false;
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
        if (CodePointer.isCodePointer(ref)) {
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

    /*
     * Support for callbacks on GC start and end
     */

    /**
     * Identifies the callback phase, and can also be used within
     * heap implementations for logging purposes.
     */
    public static enum GCCallbackPhase {
        INIT("GC initialization"), BEFORE("before GC"), AFTER("after GC");

        public final String description;
        public static final GCCallbackPhase[] VALUES = values();

        @HOSTED_ONLY
        public static String inspectedValue(Word argValue) {
            return values()[argValue.asAddress().toInt()].description;
        }

        GCCallbackPhase(String traceString) {
            this.description = traceString;
        }
    }

    /**
     * A class that wants to register for callbacks must implement this interface
     * and register itself with {@link Heap#registerGCCallback(GCCallback)}.
     */
    public interface GCCallback {
        void gcCallback(GCCallbackPhase gcCallbackPhase);
    }

    @HOSTED_ONLY
    private static ArrayList<GCCallback> gcCallbackList = new ArrayList<GCCallback>();

    @HOSTED_ONLY
    public static void registerGCCallback(GCCallback callback) {
        FatalError.check(gcCallbacks == null, "too late to register a GC callback");
        gcCallbackList.add(callback);
    }

    @HOSTED_ONLY
    private static class InitializationCompleteCallback implements JavaPrototype.InitializationCompleteCallback {

        @Override
        public void initializationComplete() {
            gcCallbacks = new GCCallback[gcCallbackList.size()];
            gcCallbackList.toArray(gcCallbacks);
        }

    }

    static {
        JavaPrototype.registerInitializationCompleteCallback(new InitializationCompleteCallback());
    }

    @CONSTANT
    private static GCCallback[] gcCallbacks;

    public static void invokeGCCallbacks(GCCallbackPhase callbackPhase) {
        for (int i = 0; i < gcCallbacks.length; i++) {
            gcCallbacks[i].gcCallback(callbackPhase);
        }
    }

    /*
     * Everything related to logging/tracing the heap behavior is defined below.
     */

    /**
     * A logger for the phases of the GC - implementation provided by the heap scheme.
     */
    public static final PhaseLogger phaseLogger = heapScheme().phaseLogger();

    /**
     * A logger for timing the phases of the GC - implementation provided by the heap scheme.
     */
    public static final TimeLogger timeLogger = heapScheme().timeLogger();

    /**
     * Logger for root scanning.
     */
    public static final RootScanLogger rootScanLogger = new RootScanLogger();

    /**
     * A logger for object allocation, only visible in a DEBUG image build.
     */
    public static final AllocationLogger allocationLogger = MaxineVM.isDebug() ? new AllocationLogger(true) : new AllocationLogger();

    /**
     * A pseudo-logger that exists solely to define the {@code LogGC and TraceGC} options,
     * which, if enabled, force all the dependent loggers to enabled. It has no operations.
     */
    public static final VMLogger gcAllLogger = new VMLogger("GC", 0,
                    "all garbage collection activity. Enabling this option also enables the " +
                    rootScanLogger.logOption + ", " +
                    phaseLogger.traceOption +  " and " + timeLogger.traceOption + " options.", null) {
        @Override
        public void checkOptions() {
            super.checkOptions();
            forceDependentLoggerState(phaseLogger, rootScanLogger, timeLogger);
        }
    };

    static {
        VMOptions.addFieldOption("-XX:", "LogGCSuppressionCount", Heap.class,
                        "Disable " + gcAllLogger.logOption + ", " + rootScanLogger.logOption + " and " +
                        phaseLogger.logOption + " until the n'th GC");
    }

    /*
     * Functions that act as guards for logging, and add additional conjunctive constraints
     * beyond the setting of the log options.
     */

    /**
     * Determines if object allocation should be logged.
     *
     * @return {@code false} if the VM build level is not {@link BuildLevel#DEBUG}.
     */
    @INLINE
    public static boolean logAllocation() {
        return MaxineVM.isDebug() && allocationLogger.enabled();
    }

    /**
     * Determines if all garbage collection activity should be logged.
     */
    @INLINE
    public static boolean logAllGC() {
        return gcAllLogger.enabled() && LogGCSuppressionCount <= 0;
    }

    /**
     * Determines if the garbage collection phases should be logged.
     */
    @INLINE
    public static boolean logGCPhases() {
        return (gcAllLogger.enabled()  || phaseLogger.enabled()) && LogGCSuppressionCount <= 0;
    }

    /**
     * Determines if garbage collection root scanning should be logged.
     */
    @INLINE
    public static boolean logRootScanning() {
        return (gcAllLogger.enabled() || rootScanLogger.enabled()) && LogGCSuppressionCount <= 0;
    }

    /**
     * Determines if garbage collection timings should be collected and logged.
     */
    @INLINE
    public static boolean logGCTime() {
        return (gcAllLogger.enabled() || timeLogger.enabled()) && LogGCSuppressionCount <= 0;
    }

    /**
     * Disables phase, time and roots logging if greater than 0.
     */
    public static int LogGCSuppressionCount;


    /*
     * Logging of root scanning.
     */
    @HOSTED_ONLY
    @VMLoggerInterface
    private interface RootScanLoggerInterface {
        void scanningBootHeap(
            @VMLogParam(name = "start") Address start,
            @VMLogParam(name = "end") Address end,
            @VMLogParam(name = "mutableReferencesEnd") Address mutableReferencesEnd);

        void visitReferenceMapSlot(
            @VMLogParam(name = "regionWordIndex") int regionWordIndex,
            @VMLogParam(name = "address") Pointer address,
            @VMLogParam(name = "value") Word value);
    }

    public static class RootScanLogger extends RootScanLoggerAuto {
        RootScanLogger() {
            super("RootScanning", "garbage collection root scanning.");
        }

        public void logScanningBootHeap(BootHeapRegion region, Address mutableReferencesEnd) {
            logScanningBootHeap(region.start(), region.end(), mutableReferencesEnd);
        }

        @Override
        protected  void traceScanningBootHeap(Address start, Address end, Address mutableReferencesEnd) {
            Log.print("Scanning boot heap: start=");
            Log.print(start);
            Log.print(", end=");
            Log.print(end);
            Log.print(", mutable references end=");
            Log.println(mutableReferencesEnd);
        }

        @Override
        protected void traceVisitReferenceMapSlot(int regionWordIndex, Pointer address, Word value) {
            Log.print("    Slot: ");
            Log.print("index=");
            Log.print(regionWordIndex);
            Log.print(", address=");
            Log.print(address);
            Log.print(", value=");
            Log.println(value);
        }
    }

    /*
     * Logging of object allocation.
     */

    /**
     * Allocation logging interface.
     * These methods are very similar in form, but we choose to distinguish them
     * by the operation (method) name. Most of the implementation will be shared.
     */
    @HOSTED_ONLY
    @VMLoggerInterface(defaultConstructor = true)
    private interface AllocationLoggerInterface {
        void clone(
            @VMLogParam(name = "classActor") ClassActor classActor,
            @VMLogParam(name = "cell") Pointer cell,
            @VMLogParam(name = "size") Size size);

        void createArray(
            @VMLogParam(name = "classActor") ClassActor classActor,
            @VMLogParam(name = "length") int length,
            @VMLogParam(name = "cell") Pointer cell,
            @VMLogParam(name = "size") Size size);

        void createTuple(
            @VMLogParam(name = "classActor") ClassActor classActor,
            @VMLogParam(name = "cell") Pointer cell,
            @VMLogParam(name = "size") Size size);

        void createHybrid(
            @VMLogParam(name = "classActor") ClassActor classActor,
            @VMLogParam(name = "cell") Pointer cell,
            @VMLogParam(name = "size") Size size);

        void expandHybrid(
            @VMLogParam(name = "classActor") ClassActor classActor,
            @VMLogParam(name = "cell") Pointer cell,
            @VMLogParam(name = "size") Size size);
    }

    /**
     * Implementation of allocation logging.
     * The actual methods traffic in {@link Hub} types, which we convert to
     * {@link ClassActor}, since the tracing simply prints the class name,
     * and we can log a {@link ClassActor} by its id.
     */
    public final static class AllocationLogger extends AllocationLoggerAuto {
        AllocationLogger(boolean active) {
            super("Allocation", "heap allocation.");
        }

        AllocationLogger() {
            super();
        }

        @NEVER_INLINE
        void logClone(Hub hub, Object clone) {
            logClone(hub.classActor, Layout.originToCell(ObjectAccess.toOrigin(clone)), hub.tupleSize);
        }

        @NEVER_INLINE
        void logCreateArray(Hub hub, int length, Object array) {
            logCreateArray(hub.classActor, length, Layout.originToCell(ObjectAccess.toOrigin(array)), Layout.size(Reference.fromJava(array)));
        }

        @NEVER_INLINE
        void logCreateTuple(Hub hub, Object object) {
            logCreateTuple(hub.classActor, Layout.originToCell(ObjectAccess.toOrigin(object)), hub.tupleSize);
        }

        @NEVER_INLINE
        void logCreateHybrid(Hub hub, Object hybrid) {
            logCreateHybrid(hub.classActor, Layout.originToCell(ObjectAccess.toOrigin(hybrid)), hub.tupleSize);
        }

        @NEVER_INLINE
        void logExpandHybrid(Hub hub, Hybrid expandedHybrid) {
            logExpandHybrid(hub.classActor, Layout.originToCell(ObjectAccess.toOrigin(expandedHybrid)), hub.tupleSize);
        }

        @Override
        protected void traceClone(ClassActor classActor, Pointer cell, Size size) {
            traceAllocation(classActor, cell, size, -1, "clone");
        }

        @Override
        protected void traceCreateTuple(ClassActor classActor, Pointer cell, Size size) {
            traceAllocation(classActor, cell, size, -1, "tuple");
        }

        @Override
        protected void traceCreateHybrid(ClassActor classActor, Pointer cell, Size size) {
            traceAllocation(classActor, cell, size, -1, "hybrid");
        }

        @Override
        protected void traceExpandHybrid(ClassActor classActor, Pointer cell, Size size) {
            traceAllocation(classActor, cell, size, -1, "expanded hybrid");
        }

        @Override
        protected void traceCreateArray(ClassActor classActor, int length, Pointer cell, Size size) {
            traceAllocation(classActor, cell, size, length, "array");
        }

        private static void traceAllocation(ClassActor classActor, Pointer cell, Size size, int length, String variant) {
            Log.print(": Allocated ");
            Log.print(variant);
            Log.print(' ');
            Log.print(classActor.name.string);
            if (length >= 0) {
                Log.print(" of length ");
                Log.print(length);
            }
            Log.print(" at ");
            Log.print(cell);
            Log.print(" [");
            Log.print(size.toInt());
            Log.println(" bytes]");
        }

    }

// START GENERATED CODE
    private static abstract class AllocationLoggerAuto extends com.sun.max.vm.log.VMLogger {
        public enum Operation {
            Clone, CreateArray, CreateHybrid,
            CreateTuple, ExpandHybrid;

            @SuppressWarnings("hiding")
            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = null;

        protected AllocationLoggerAuto(String name, String optionDescription) {
            super(name, Operation.VALUES.length, optionDescription, REFMAPS);
        }

        protected AllocationLoggerAuto() {
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @INLINE
        public final void logClone(ClassActor classActor, Pointer cell, Size size) {
            log(Operation.Clone.ordinal(), classActorArg(classActor), cell, size);
        }
        protected abstract void traceClone(ClassActor classActor, Pointer cell, Size size);

        @INLINE
        public final void logCreateArray(ClassActor classActor, int length, Pointer cell, Size size) {
            log(Operation.CreateArray.ordinal(), classActorArg(classActor), intArg(length), cell, size);
        }
        protected abstract void traceCreateArray(ClassActor classActor, int length, Pointer cell, Size size);

        @INLINE
        public final void logCreateHybrid(ClassActor classActor, Pointer cell, Size size) {
            log(Operation.CreateHybrid.ordinal(), classActorArg(classActor), cell, size);
        }
        protected abstract void traceCreateHybrid(ClassActor classActor, Pointer cell, Size size);

        @INLINE
        public final void logCreateTuple(ClassActor classActor, Pointer cell, Size size) {
            log(Operation.CreateTuple.ordinal(), classActorArg(classActor), cell, size);
        }
        protected abstract void traceCreateTuple(ClassActor classActor, Pointer cell, Size size);

        @INLINE
        public final void logExpandHybrid(ClassActor classActor, Pointer cell, Size size) {
            log(Operation.ExpandHybrid.ordinal(), classActorArg(classActor), cell, size);
        }
        protected abstract void traceExpandHybrid(ClassActor classActor, Pointer cell, Size size);

        @Override
        protected void trace(Record r) {
            switch (r.getOperation()) {
                case 0: { //Clone
                    traceClone(toClassActor(r, 1), toPointer(r, 2), toSize(r, 3));
                    break;
                }
                case 1: { //CreateArray
                    traceCreateArray(toClassActor(r, 1), toInt(r, 2), toPointer(r, 3), toSize(r, 4));
                    break;
                }
                case 2: { //CreateHybrid
                    traceCreateHybrid(toClassActor(r, 1), toPointer(r, 2), toSize(r, 3));
                    break;
                }
                case 3: { //CreateTuple
                    traceCreateTuple(toClassActor(r, 1), toPointer(r, 2), toSize(r, 3));
                    break;
                }
                case 4: { //ExpandHybrid
                    traceExpandHybrid(toClassActor(r, 1), toPointer(r, 2), toSize(r, 3));
                    break;
                }
            }
        }
    }

    private static abstract class RootScanLoggerAuto extends com.sun.max.vm.log.VMLogger {
        public enum Operation {
            ScanningBootHeap, VisitReferenceMapSlot;

            @SuppressWarnings("hiding")
            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = null;

        protected RootScanLoggerAuto(String name, String optionDescription) {
            super(name, Operation.VALUES.length, optionDescription, REFMAPS);
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @INLINE
        public final void logScanningBootHeap(Address start, Address end, Address mutableReferencesEnd) {
            log(Operation.ScanningBootHeap.ordinal(), start, end, mutableReferencesEnd);
        }
        protected abstract void traceScanningBootHeap(Address start, Address end, Address mutableReferencesEnd);

        @INLINE
        public final void logVisitReferenceMapSlot(int regionWordIndex, Pointer address, Word value) {
            log(Operation.VisitReferenceMapSlot.ordinal(), intArg(regionWordIndex), address, value);
        }
        protected abstract void traceVisitReferenceMapSlot(int regionWordIndex, Pointer address, Word value);

        @Override
        protected void trace(Record r) {
            switch (r.getOperation()) {
                case 0: { //ScanningBootHeap
                    traceScanningBootHeap(toAddress(r, 1), toAddress(r, 2), toAddress(r, 3));
                    break;
                }
                case 1: { //VisitReferenceMapSlot
                    traceVisitReferenceMapSlot(toInt(r, 1), toPointer(r, 2), toWord(r, 3));
                    break;
                }
            }
        }
    }

// END GENERATED CODE

}
