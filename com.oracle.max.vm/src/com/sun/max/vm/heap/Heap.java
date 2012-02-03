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
import com.sun.max.vm.debug.*;
import com.sun.max.vm.heap.HeapScheme.PIN_SUPPORT_FLAG;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.log.*;
import com.sun.max.vm.log.VMLog.Record;
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
        return verboseOption.verboseGC || gcLogger.enabled() || TraceRootScanning || Heap.traceGCTime();
    }

    /**
     * Set the verboseGC option (java.lang.management support).
     */
    public static void setVerbose(boolean value) {
        verboseOption.verboseGC = value;
    }

    /**
     * Determines if allocation should be traced.
     *
     * @returns Always {@code false} if the VM build level is not {@link BuildLevel#DEBUG}.
     */
    @INLINE
    public static boolean traceAllocation() {
        return MaxineVM.isDebug() && allocationLogger.enabled();
    }

    /**
     * Modifies the value of the flag determining if allocation should be traced. This flag is ignored if the VM
     * {@linkplain VMConfiguration#buildLevel() build level} is not {@link BuildLevel#DEBUG}. This is typically provided
     * so that error situations can be reported without being confused by interleaving allocation traces.
     */
    public static void setTraceAllocation(boolean flag) {
        allocationLogger.enableTrace(flag);
    }

    /**
     * Determines if all garbage collection activity should be traced.
     */
    @INLINE
    public static boolean traceGC() {
        return gcLogger.enabled() && TraceGCSuppressionCount <= 0;
    }

    /**
     * Determines if the garbage collection phases should be traced.
     */
    @INLINE
    public static boolean traceGCPhases() {
        return (gcLogger.enabled()  || phaseLogger.enabled()) && TraceGCSuppressionCount <= 0;
    }

    /**
     * Determines if garbage collection root scanning should be traced.
     */
    @INLINE
    public static boolean traceRootScanning() {
        return (gcLogger.enabled() || TraceRootScanning) && TraceGCSuppressionCount <= 0;
    }

    public static void setTraceRootScanning(boolean flag) {
        TraceRootScanning = flag;
    }

    /**
     * Determines if garbage collection timings should be collected and printed.
     */
    @INLINE
    public static boolean traceGCTime() {
        return (gcLogger.enabled() || timeLogger.enabled()) && TraceGCSuppressionCount <= 0;
    }

    /**
     * Disables -XX:-TraceGC, -XX:-TraceRootScanning and -XX:-TraceGCPhases if greater than 0.
     */
    public static int TraceGCSuppressionCount;

    /**
     * A logger for the phases of the GC - implementation provided by the heap scheme.
     */
    public static final VMLogger phaseLogger = heapScheme().phaseLogger();

    /**
     * A logger for timing the phases of the GC.
     */
    public static final TimeLogger timeLogger = heapScheme().timeLogger();

    /**
     * A logger for object allocation, only visible in a DEBUG image build.
     */
    public static final AllocationLogger allocationLogger = MaxineVM.isDebug() ? new AllocationLogger(true) : new AllocationLogger();

    /**
     * A pseudo-logger that exists solely to define the {@code TraceGC/LogGC} options,
     * which force all the separate options on.
     */
    public static final VMLogger gcLogger = new VMLogger("GC", 0,
                    "all garbage collection activity. Enabling this option also enables the " +
                    phaseLogger.traceOption +  " and " + timeLogger.traceOption + " options.") {
        @Override
        public void checkOptions() {
            super.checkOptions();
            // force the checking of our dependent loggers now
            phaseLogger.checkOptions();
            timeLogger.checkOptions();
            // Now enforce our state on them.
            if (enabled()) {
                phaseLogger.enable(enabled());
                timeLogger.enable(enabled());
            }
            if (traceEnabled()) {
                phaseLogger.enableTrace(traceEnabled());
                timeLogger.enableTrace(traceEnabled());
            }
        }
    };

    private static boolean TraceRootScanning;
    private static boolean GCDisabled;

    static {
        VMOption traceRootScanningOption = VMOptions.addFieldOption("-XX:", "TraceRootScanning", Heap.class,
            "Trace garbage collection root scanning.");

        VMOptions.addFieldOption("-XX:", "TraceGCSuppressionCount", Heap.class,
                        "Disable " + gcLogger.traceOption + ", " + traceRootScanningOption + " and " +
                        phaseLogger.traceOption + " until the n'th GC");

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
            allocationLogger.logCreateArray(hub, length, array);
        }
        return array;
    }

    @INLINE
    public static Object createTuple(Hub hub) {
        final Object object = heapScheme().createTuple(hub);
        if (Heap.traceAllocation()) {
            allocationLogger.logCreateTuple(hub, object);
        }
        return object;
    }

    @INLINE
    public static Object createHybrid(DynamicHub hub) {
        final Object hybrid = heapScheme().createHybrid(hub);
        if (Heap.traceAllocation()) {
            allocationLogger.logCreateHybrid(hub, hybrid);
        }
        return hybrid;
    }

    @INLINE
    public static Hybrid expandHybrid(Hybrid hybrid, int length) {
        final Hybrid expandedHybrid = heapScheme().expandHybrid(hybrid, length);
        if (Heap.traceAllocation()) {
            allocationLogger.logExpandHybrid(hybrid, expandedHybrid);
        }
        return expandedHybrid;
    }

    @INLINE
    public static Object clone(Object object) {
        final Object clone = heapScheme().clone(object);
        if (Heap.traceAllocation()) {
            allocationLogger.logClone(object, clone);
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

    public static boolean collectGarbage(Size requestedFreeSpace) {
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
            return heapLockedCollectGarbage(requestedFreeSpace);
        } else {
            // Calls to collect garbage need to synchronize on the heap lock. This ensures that
            // GC operations are submitted serially to the VM operation thread. It also means
            // that a collection only actually occurs if needed (i.e. concurrent call to this
            // method by another thread did not trigger a GC that freed up enough memory for
            // this request).
            synchronized (HEAP_LOCK) {
                waitForGCDisablingThreads();
                return heapLockedCollectGarbage(requestedFreeSpace);
            }
        }
    }

    private static boolean heapLockedCollectGarbage(Size requestedFreeSpace) {
        if (verbose()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("--GC requested by thread ");
            Log.printCurrentThread(false);
            Log.print(" for ");
            Log.print(requestedFreeSpace.toLong());
            Log.println(" bytes --");
            Log.unlock(lockDisabledSafepoints);
        }
        final boolean freedEnough = heapScheme().collectGarbage(requestedFreeSpace);
        if (verbose()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("--GC requested by thread ");
            Log.printCurrentThread(false);
            if (freedEnough) {
                Log.println(" freed enough--");
            } else {
                Log.println(" did not free enough--");
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

    public static class AllocationLogger extends VMLogger {
        public enum Operation {
            CLONE("clone"), CREATE_ARRAY(""), CREATE_HYBRID("hybrid"),
            CREATE_TUPLE("tuple"), EXPAND_HYBRID("expanded hybrid");

            public final String logName;
            public static Operation[] VALUES = values();
            Operation(String logName) {
                this.logName = logName;
            }
        }
        AllocationLogger(boolean active) {
            super("Allocation", Operation.values().length, "heap allocation.");
        }

        AllocationLogger() {
            super();
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @NEVER_INLINE
        void logClone(Object object, Object clone) {
            final Hub hub = ObjectAccess.readHub(object);
            logCreateVariant(Operation.CLONE, hub, object);
        }

        @NEVER_INLINE
        void logCreateArray(DynamicHub hub, int length, Object array) {
            log(Operation.CREATE_ARRAY.ordinal(), VMLogger.intArg(hub.classActor.id),
                            VMLogger.intArg(length), Layout.originToCell(ObjectAccess.toOrigin(array)),
                                            Layout.size(Reference.fromJava(array)));
        }

        @NEVER_INLINE
        void logCreateTuple(Hub hub, Object object) {
            logCreateVariant(Operation.CREATE_TUPLE, hub, object);
        }

        @NEVER_INLINE
        void logCreateHybrid(Hub hub, Object hybrid) {
            logCreateVariant(Operation.CREATE_HYBRID, hub, hybrid);
        }

        @NEVER_INLINE
        void logExpandHybrid(Hybrid hybrid, Hybrid expandedHybrid) {
            logCreateVariant(Operation.EXPAND_HYBRID, ObjectAccess.readHub(hybrid), hybrid);
        }

        private void logCreateVariant(Operation op, Hub hub, Object object) {
            log(op.ordinal(), VMLogger.intArg(hub.classActor.id),
                            Layout.originToCell(ObjectAccess.toOrigin(object)), hub.tupleSize);
        }


        @Override
        protected void trace(Record r) {
            Operation op = Operation.VALUES[r.getOperation()];
            // Assumes the trace takes place on the same thread as the log operation, true today.
            Log.printCurrentThread(false);
            switch (op) {
                case CLONE:
                case CREATE_TUPLE:
                case CREATE_HYBRID: {
                    Log.print(": Allocated ");
                    Log.print(op.logName);
                    Log.print(' ');
                    ClassActor classActor = ClassID.toClassActor(r.getIntArg(1));
                    Log.print(classActor.name.string);
                    Log.print(" at ");
                    Log.print(r.getArg(2));
                    Log.print(" [");
                    Log.print(r.getIntArg(3));
                    Log.println(" bytes]");
                    break;
                }

                case CREATE_ARRAY: {
                    Log.print(": Allocated array ");
                    ClassActor classActor = ClassID.toClassActor(r.getIntArg(1));
                    Log.print(classActor.name.string);
                    Log.print(" of length ");
                    Log.print(r.getIntArg(2));
                    Log.print(" at ");
                    Log.print(r.getArg(3));
                    Log.print(" [");
                    Log.print(r.getIntArg(4));
                    Log.println(" bytes]");
                    break;
                }
            }
        }
    }
}
