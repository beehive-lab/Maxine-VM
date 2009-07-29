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
package com.sun.max.vm.heap.sequential.semiSpace;

import static com.sun.max.vm.VMOptions.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.annotate.*;
import com.sun.max.atomic.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.timer.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.sequential.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.tele.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * A simple semi-space scavenger heap, mainly for testing.
 *
 * @author Bernd Mathiske
 * @author Sunil Soman
 * @author Doug Simon
 * @author Hannes Payer
 */
public final class SemiSpaceHeapScheme extends HeapSchemeAdaptor implements HeapScheme, CellVisitor {

    /**
     * A VM option for specifying how heap memory is to be allocated.
     */
    private static final VMBooleanXXOption virtualAllocOption =
        register(new VMBooleanXXOption("-XX:+SemiSpaceUseVirtualMemory", "Allocate memory for GC using mmap instead of malloc."), MaxineVM.Phase.PRISTINE);

    /**
     * A VM option for specifying amount of memory to be reserved for allocating and raising an
     * OutOfMemoryError when insufficient memory is available to satisfy an {@linkplain #allocate(Size)
     * allocation} request.
     *
     * @see #safetyZoneSize
     */
    private static final VMIntOption safetyZoneSizeOption =
        register(new VMIntOption("-XX:SemiSpaceGCSafetyZoneSize=", 6144,
            "Memory reserved to throw OutOfMemoryError. If using TLABs, then the actual memory reserved " +
            "is the maximum of this option's value and the size of a TLAB."), MaxineVM.Phase.PRISTINE);

    private static final VMStringOption growPolicyOption =
        register(new VMStringOption("-XX:SemiSpaceGCGrowPolicy=", false, "Double", "Grow policy for heap (Linear|Double)."), MaxineVM.Phase.STARTING);

    private final PointerIndexGripUpdater pointerIndexGripUpdater = new PointerIndexGripUpdater();
    private final PointerOffsetGripUpdater pointerOffsetGripUpdater = new PointerOffsetGripUpdater();
    private final GripForwarder gripForwarder = new GripForwarder();

    /**
     * The procedure that will identify all the GC roots except those in the boot heap and code regions.
     */
    private final SequentialHeapRootsScanner heapRootsScanner = new SequentialHeapRootsScanner(pointerIndexGripUpdater);

    /**
     * Procedure used to verify a grip.
     * This field is null in a {@linkplain VMConfiguration#debugging() non-debug} VM.
     */
    private GripVerifier gripVerifier;

    /**
     * Procedure used to verify heap well-formedness.
     * This field is null in a {@linkplain VMConfiguration#debugging() non-debug} VM.
     */
    private SequentialHeapRootsScanner heapRootsVerifier;

    private final Collect collect = new Collect();

    private StopTheWorldGCDaemon collectorThread;

    private final SemiSpaceMemoryRegion fromSpace = new SemiSpaceMemoryRegion("Heap-From");
    private final SemiSpaceMemoryRegion toSpace = new SemiSpaceMemoryRegion("Heap-To");

    /**
     * Used when {@linkplain #grow(GrowPolicy) growing} the heap.
     */
    private final SemiSpaceMemoryRegion growFromSpace = new SemiSpaceMemoryRegion("Heap-From-Grow");

    /**
     * Used when {@linkplain #grow(GrowPolicy) growing} the heap.
     */
    private final SemiSpaceMemoryRegion growToSpace = new SemiSpaceMemoryRegion("Heap-To-Grow");

    /**
     * The amount of memory reserved for allocating and raising an OutOfMemoryError when insufficient
     * memory is available to satisfy an {@linkplain #allocate(Size) allocation} request.
     *
     * @see #safetyZoneSizeOption
     */
    private int safetyZoneSize;

    private GrowPolicy growPolicy;
    private LinearGrowPolicy increaseGrowPolicy;

    /**
     * The global allocation limit (minus the {@linkplain #safetyZoneSize safety zone}).
     */
    private Address top;

    /**
     * The global allocation mark.
     */
    private final AtomicWord allocationMark = new AtomicWord();

    /**
     * Flags if TLABs are being used for allocation.
     */
    private boolean useTLAB;

    private final ResetTLAB resetTLAB = new ResetTLAB();

    /**
     * The size of a TLAB.
     */
    private Size tlabSize;

    // Create timing facilities.
    private final TimerMetric clearTimer = new TimerMetric(new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK));
    private final TimerMetric gcTimer = new TimerMetric(new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK));
    private final TimerMetric rootScanTimer = new TimerMetric(new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK));
    private final TimerMetric bootHeapScanTimer = new TimerMetric(new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK));
    private final TimerMetric codeScanTimer = new TimerMetric(new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK));
    private final TimerMetric copyTimer = new TimerMetric(new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK));
    private final TimerMetric weakRefTimer = new TimerMetric(new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK));

    private int numberOfGarbageCollectionInvocations;

    /**
     * A VM option for disabling use of TLABs.
     */
    private static final VMBooleanXXOption useTLABOption = register(new VMBooleanXXOption("-XX:+UseTLAB",
        "Use thread-local object allocation."), MaxineVM.Phase.PRISTINE);

    /**
     * A VM option for specifying the size of a TLAB.
     */
    private static final VMSizeOption tlabSizeOption = register(new VMSizeOption("-XX:TLABSize=", Size.K.times(64),
        "The size of thread-local allocation buffers."), MaxineVM.Phase.PRISTINE);

    /**
     * A VM option for forcing a GC before allocation. Ignored if useTLABOption is on.
     */
    private static final VMBooleanXXOption excessiveGCOption = register(new VMBooleanXXOption("-XX:-ExcessiveGC",
        "Perform a garbage collection before every allocation. This is ignored if " + useTLABOption + " is specified."), MaxineVM.Phase.PRISTINE);

    public SemiSpaceHeapScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        if (MaxineVM.isPrototyping()) {
            if (MaxineVM.isDebug()) {
                gripVerifier = new GripVerifier();
                heapRootsVerifier = new SequentialHeapRootsScanner(gripVerifier);
            }

        } else if (phase == MaxineVM.Phase.PRISTINE) {
            final Size size = Heap.initialSize().dividedBy(2);

            if (allocateSpace(fromSpace, size).isZero() || allocateSpace(toSpace, size).isZero()) {
                Log.print("Could not allocate object heap of size ");
                Log.print(size.toLong());
                Log.println();
                FatalError.unexpected("Insufficient memory to initialize SemiSpaceHeapScheme");
            }

            useTLAB = useTLABOption.getValue();
            tlabSize = tlabSizeOption.getValue();
            if (tlabSize.lessThan(0)) {
                FatalError.unexpected("Specified TLAB size is too small");
            }
            safetyZoneSize = Math.max(safetyZoneSizeOption.getValue(), tlabSize.toInt());

            allocationMark.set(toSpace.start());
            top = toSpace.end().minus(safetyZoneSize);

            // From now on we can allocate
            InspectableHeapInfo.init(toSpace, fromSpace);
        } else if (phase == MaxineVM.Phase.STARTING) {
            final String growPolicy = growPolicyOption.getValue();
            if (growPolicy.equals("Double")) {
                this.growPolicy = new DoubleGrowPolicy();
            } else if (growPolicy.equals("Linear")) {
                this.growPolicy = new LinearGrowPolicy(growPolicy);
            } else {
                Log.print("Unknown heap growth policy, using default policy");
                this.growPolicy = new DoubleGrowPolicy();
            }
            increaseGrowPolicy = new LinearGrowPolicy();
            collectorThread = new StopTheWorldGCDaemon("GC", collect);
            collectorThread.start();
        }
    }

    @INLINE
    private Address allocationMark() {
        return allocationMark.get().asAddress();
    }

    private static void startTimer(Timer timer) {
        if (Heap.traceGCTime()) {
            timer.start();
        }
    }

    private static void stopTimer(Timer timer) {
        if (Heap.traceGCTime()) {
            timer.stop();
        }
    }

    private final class GripForwarder implements SpecialReferenceManager.GripForwarder {

        public boolean isReachable(Grip grip) {
            final Pointer origin = grip.toOrigin();
            if (fromSpace.contains(origin)) {
                final Grip forwardGrip = Layout.readForwardGrip(origin);
                if (forwardGrip.isZero()) {
                    return false;
                }
            }
            return true;
        }

        public Grip getForwardGrip(Grip grip) {
            final Pointer origin = grip.toOrigin();
            if (fromSpace.contains(origin)) {
                return Layout.readForwardGrip(origin);
            }
            return grip;
        }
    }

    private final class PointerOffsetGripUpdater implements PointerOffsetVisitor {

        public void visitPointerOffset(Pointer pointer, int offset) {
            final Grip oldGrip = pointer.readGrip(offset);
            final Grip newGrip = mapGrip(oldGrip);
            if (newGrip != oldGrip) {
                pointer.writeGrip(offset, newGrip);
            }
        }
    }

    private final class PointerIndexGripUpdater implements PointerIndexVisitor {
        public void visitPointerIndex(Pointer pointer, int wordIndex) {
            final Grip oldGrip = pointer.getGrip(wordIndex);
            final Grip newGrip = mapGrip(oldGrip);
            if (newGrip != oldGrip) {
                pointer.setGrip(wordIndex, newGrip);
            }
        }
    }

    private final class GripVerifier   implements PointerIndexVisitor, PointerOffsetVisitor {
        public void visitPointerIndex(Pointer pointer, int wordIndex) {
            visitPointerOffset(pointer, wordIndex * Word.size());
        }
        public void visitPointerOffset(Pointer pointer, int offset) {
            DebugHeap.verifyGripAtIndex(pointer, offset, pointer.readGrip(offset), toSpace, null);
        }
    }

    /**
     * Routine that performs the actual garbage collection.
     */
    private final class Collect implements Runnable {
        public void run() {
            try {
                VmThreadMap.ACTIVE.forAllVmThreadLocals(null, resetTLAB);
                if (MaxineVM.isDebug()) {
                    // Pre-verification of the heap.
                    verifyObjectSpaces("before GC");
                }

                ++numberOfGarbageCollectionInvocations;
                InspectableHeapInfo.beforeGarbageCollection();

                VMConfiguration.hostOrTarget().monitorScheme().beforeGarbageCollection();

                startTimer(gcTimer);

                startTimer(clearTimer);
                swapSemiSpaces(); // Swap semi-spaces. From--> To and To-->From
                stopTimer(clearTimer);

                if (Heap.traceGCPhases()) {
                    Log.println("Scanning roots...");
                }
                startTimer(rootScanTimer);
                heapRootsScanner.run(); // Start scanning the reachable objects from my roots.
                stopTimer(rootScanTimer);

                if (Heap.traceGCPhases()) {
                    Log.println("Scanning boot heap...");
                }
                startTimer(bootHeapScanTimer);
                scanBootHeap();
                stopTimer(bootHeapScanTimer);

                if (Heap.traceGCPhases()) {
                    Log.println("Scanning code...");
                }
                startTimer(codeScanTimer);
                scanCode();
                stopTimer(codeScanTimer);

                if (Heap.traceGCPhases()) {
                    Log.println("Moving reachable...");
                }

                startTimer(copyTimer);
                moveReachableObjects();
                stopTimer(copyTimer);

                if (Heap.traceGCPhases()) {
                    Log.println("Processing weak references...");
                }

                startTimer(weakRefTimer);
                SpecialReferenceManager.processDiscoveredSpecialReferences(gripForwarder);
                stopTimer(weakRefTimer);
                stopTimer(gcTimer);

                // Bring the inspectable mark up to date, since it is not updated during the move.
                toSpace.setAllocationMark(allocationMark()); // for debugging

                VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();

                if (MaxineVM.isDebug()) {
                    verifyObjectSpaces("after GC");
                }

                InspectableHeapInfo.afterGarbageCollection();

                if (Heap.traceGCTime()) {
                    final boolean lockDisabledSafepoints = Log.lock();
                    Log.print("Timings (");
                    Log.print(TimerUtil.getHzSuffix(HeapScheme.GC_TIMING_CLOCK));
                    Log.print(") for GC ");
                    Log.print(numberOfGarbageCollectionInvocations);
                    Log.print(": clear & initialize=");
                    Log.print(clearTimer.getLastElapsedTime());
                    Log.print(", root scan=");
                    Log.print(rootScanTimer.getLastElapsedTime());
                    Log.print(", boot heap scan=");
                    Log.print(bootHeapScanTimer.getLastElapsedTime());
                    Log.print(", code scan=");
                    Log.print(codeScanTimer.getLastElapsedTime());
                    Log.print(", copy=");
                    Log.print(copyTimer.getLastElapsedTime());
                    Log.print(", weak refs=");
                    Log.print(weakRefTimer.getLastElapsedTime());
                    Log.print(", total=");
                    Log.println(gcTimer.getLastElapsedTime());
                    Log.unlock(lockDisabledSafepoints);
                }
            } catch (Throwable throwable) {
                FatalError.unexpected("Exception during GC", throwable);
            }
        }
    }

    @INLINE
    /**
     * Attempts to allocate memory of given size for given space.
     * If successful sets region start and size.
     */
    private static Address allocateSpace(SemiSpaceMemoryRegion space, Size size) {
        final Address base = virtualAllocOption.getValue() ? VirtualMemory.allocate(size, VirtualMemory.Type.HEAP) : Memory.allocate(size);
        if (!base.isZero()) {
            space.setStart(base);
            space.setAllocationMark(base); // debugging
            space.setSize(size);
        }
        return base;
    }

    @INLINE
    /**
     * Deallocates the memory associated with the given region.
     * Sets the region start to zero but does not change the size.
     */
    private static void deallocateSpace(SemiSpaceMemoryRegion space) {
        final Address base = space.start();
        if (virtualAllocOption.getValue()) {
            VirtualMemory.deallocate(base, space.size(), VirtualMemory.Type.HEAP);
        } else {
            Memory.deallocate(base);
        }
        space.setStart(Address.zero());
    }

    @INLINE
    /**
     * Copies the state of one space into another.
     * Used when growing the semispaces.
     */
    private static void copySpaceState(SemiSpaceMemoryRegion from, SemiSpaceMemoryRegion to) {
        to.setStart(from.start());
        to.setAllocationMark(from.start());
        to.setSize(from.size());
    }

    private void swapSemiSpaces() {
        final Address oldFromSpaceStart = fromSpace.start();
        final Size oldFromSpaceSize = fromSpace.size();

        fromSpace.setStart(toSpace.start());
        fromSpace.setSize(toSpace.size());
        fromSpace.setAllocationMark(toSpace.getAllocationMark()); // for debugging

        toSpace.setStart(oldFromSpaceStart);
        toSpace.setSize(oldFromSpaceSize);
        toSpace.setAllocationMark(toSpace.start());  // for debugging

        allocationMark.set(toSpace.start());
        top = toSpace.end();
        // If we are currently using the safety zone, we must not install it in the swapped space
        // as that could cause gcAllocate to fail trying to copying too much live data.
        if (!inSafetyZone) {
            top = top.minus(safetyZoneSize);
        }
    }

    public boolean isGcThread(Thread thread) {
        return thread instanceof StopTheWorldGCDaemon;
    }

    public int adjustedCardTableShift() {
        return -1;
    }

    public int auxiliarySpaceSize(int bootImageSize) {
        return 0;
    }

    public void initializeAuxiliarySpace(Pointer primordialVmThreadLocals, Pointer auxiliarySpace) {
    }

    private Size immediateFreeSpace() {
        return top.minus(allocationMark()).asSize();
    }

    private Grip mapGrip(Grip grip) {
        final Pointer fromOrigin = grip.toOrigin();
        if (MaxineVM.isDebug()) {
            DebugHeap.verifyGripAtIndex(Address.zero(), 0, grip, toSpace, fromSpace);
        }
        if (fromSpace.contains(fromOrigin)) {
            final Grip forwardGrip = Layout.readForwardGrip(fromOrigin);
            if (!forwardGrip.isZero()) {
                return forwardGrip;
            }
            final Pointer fromCell = Layout.originToCell(fromOrigin);
            final Size size = Layout.size(fromOrigin);
            final Pointer toCell = gcAllocate(size);
            if (MaxineVM.isDebug()) {
                DebugHeap.writeCellTag(toCell);
            }

            if (Heap.traceGC()) {
                final boolean lockDisabledSafepoints = Log.lock();
                final Hub hub = UnsafeLoophole.cast(Layout.readHubReference(grip).toJava());
                Log.print("Forwarding ");
                Log.print(hub.classActor.name.string);
                Log.print(" from ");
                Log.print(fromCell);
                Log.print(" to ");
                Log.print(toCell);
                Log.print(" [");
                Log.print(size.toInt());
                Log.println(" bytes]");
                Log.unlock(lockDisabledSafepoints);
            }

            Memory.copyBytes(fromCell, toCell, size);
            final Pointer toOrigin = Layout.cellToOrigin(toCell);
            final Grip toGrip = Grip.fromOrigin(toOrigin);
            Layout.writeForwardGrip(fromOrigin, toGrip);

            return toGrip;
        }
        return grip;
    }

    private void scanReferenceArray(Pointer origin) {
        final int length = Layout.readArrayLength(origin);
        for (int index = 0; index < length; index++) {
            final Grip oldGrip = Layout.getGrip(origin, index);
            final Grip newGrip = mapGrip(oldGrip);
            if (newGrip != oldGrip) {
                Layout.setGrip(origin, index, newGrip);
            }
        }
    }


    public Pointer visitCell(Pointer cell) {
        final Pointer origin = Layout.cellToOrigin(cell); // Returns the pointer of the first object of the semispace.
        final Grip oldHubGrip = Layout.readHubGrip(origin); // Reads the hub-Grip of the previously retrieved object.
        // Grips are used for GC purpose.
        final Grip newHubGrip = mapGrip(oldHubGrip);
        if (newHubGrip != oldHubGrip) {
            Layout.writeHubGrip(origin, newHubGrip);
        }
        final Hub hub = UnsafeLoophole.cast(newHubGrip.toJava());
        final SpecificLayout specificLayout = hub.specificLayout;
        if (specificLayout.isTupleLayout()) {
            TupleReferenceMap.visitOriginOffsets(hub, origin, pointerOffsetGripUpdater);
            if (hub.isSpecialReference) {
                SpecialReferenceManager.discoverSpecialReference(Grip.fromOrigin(origin));
            }
            return cell.plus(hub.tupleSize);
        }
        if (specificLayout.isHybridLayout()) {
            TupleReferenceMap.visitOriginOffsets(hub, origin, pointerOffsetGripUpdater);
        } else if (specificLayout.isReferenceArrayLayout()) {
            scanReferenceArray(origin);
        }
        return cell.plus(Layout.size(origin));
    }

    private void moveReachableObjects() {
        Pointer cell = toSpace.start().asPointer();
        while (cell.lessThan(allocationMark())) {
            cell = DebugHeap.checkDebugCellTag(toSpace.start(), cell);
            if (Heap.traceGC()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("Visiting cell in to space ");
                Log.println(cell);
                Log.unlock(lockDisabledSafepoints);
            }
            cell = visitCell(cell);
        }
    }

    private void scanBootHeap() {
        Heap.bootHeapRegion.visitReferences(pointerIndexGripUpdater);
    }

    private void scanCode() {
        Code.visitReferences(pointerIndexGripUpdater);
    }

    private boolean cannotGrow() {
        return fromSpace.size().isZero() || fromSpace.size().greaterEqual(Heap.maxSize());
    }

    /**
     * Grow the semispaces to be of larger size.
     *
     * @param preGc true if prior to executing collector thread to copy {@link #toSpace} to (grown) {@link #fromSpace}
     * @return true iff both spaces can be grown
     */
    private boolean growSpaces(boolean preGc, GrowPolicy growPolicy) {
        if (preGc && Heap.verbose()) {
            Log.println("Trying to grow the heap...");
        }
        if (cannotGrow()) {
            if (preGc && Heap.verbose()) {
                Log.println("...failed, max heap size reached");
            }
            return false;
        }
        if (preGc) {
            // It is important to know now that we can allocate both spaces of the new size
            // and, if we cannot, to leave things as they are, so that the VM can continue
            // using the safety zone and perhaps then free enough space to continue.
            final Size size = Size.min(growPolicy.growth(fromSpace.size()), Heap.maxSize());
            if (preGc && Heap.verbose()) {
                Log.print("...new heap size: ");
                Log.println(size.toLong());
            }
            final Address fromBase = allocateSpace(growFromSpace, size);
            final Address tempBase = allocateSpace(growToSpace, size);
            if (fromBase.isZero() || tempBase.isZero()) {
                if (!fromBase.isZero()) {
                    deallocateSpace(growFromSpace);
                }
                if (Heap.verbose()) {
                    Log.println("...grow failed, can't allocate spaces");
                }
                return false;
            }
            // return memory in 'fromSpace'
            deallocateSpace(fromSpace);
            copySpaceState(growFromSpace, fromSpace);
        } else {
            // executing the collector thread swapped the spaces
            // so we are again updating _fromSpace but with _growToSpace.
            deallocateSpace(fromSpace);
            copySpaceState(growToSpace, fromSpace);
        }
        if (preGc && Heap.verbose()) {
            Log.println("...grow ok");
        }
        return true;
    }

    @INLINE
    private void executeCollectorThread() {
        if (!Heap.gcDisabled()) {
            collectorThread.execute();
        }
    }

    private boolean grow(GrowPolicy growPolicy) {
        if (cannotGrow()) {
            return false;
        }
        boolean result = true;
        if (!growSpaces(true, growPolicy)) {
            result = false;
        } else {
            executeCollectorThread();
            result = growSpaces(false, growPolicy);
        }
        if (Heap.verbose()) {
            logSpaces();
        }
        return result;
    }

    public synchronized boolean collectGarbage(Size requestedFreeSpace) {
        if (requestedFreeSpace.toInt() == 0 || immediateFreeSpace().lessThan(requestedFreeSpace)) {
            executeCollectorThread();
        }
        if (immediateFreeSpace().greaterEqual(requestedFreeSpace)) {
            // check to see if we can reset safety zone
            if (inSafetyZone) {
                if (top.minus(allocationMark()).greaterThan(safetyZoneSize)) {
                    top = top.minus(safetyZoneSize);
                    inSafetyZone = false;
                }
            }
            return true;
        }
        while (grow(growPolicy)) {
            if (immediateFreeSpace().greaterEqual(requestedFreeSpace)) {
                return true;
            }
        }
        if (Heap.gcDisabled()) {
            Log.println("Out of memory and GC is disabled, exiting");
            MaxineVM.native_exit(1);
        }
        return false;
    }

    public Size reportFreeSpace() {
        return immediateFreeSpace();
    }

    public Size reportUsedSpace() {
        return allocationMark().minus(toSpace.start()).asSize();
    }

    /**
     * Allocates space for a cell being copied to 'to space' during GC.
     * Note that this allocation is only ever performed by the GC thread and so there's no
     * need to use compare-and-swap when updating the allocation mark.
     *
     * @param size the size of the cell being copied
     * @return the start of the allocated cell in 'to space'
     */
    private Pointer gcAllocate(Size size) {
        Pointer cell = allocationMark().asPointer();
        if (MaxineVM.isDebug()) {
            cell = cell.plusWords(1);
        }
        allocationMark.set(cell.plus(size));
        FatalError.check(allocationMark().lessThan(top), "GC allocation overflow");
        return cell;
    }

    private boolean inSafetyZone; // set after we have thrown OutOfMemoryError and are using the safety zone

    /**
     * The top of the current thread-local allocation buffer. This will remain zero if TLABs are not
     * {@linkplain #useTLABOption enabled}.
     */
    private static final VmThreadLocal TLAB_TOP
        = new VmThreadLocal("TLAB_TOP", Kind.WORD, "SemiSpace: top of current TLAB, zero if not used");

    /**
     * The allocation mark of the current thread-local allocation buffer. This will remain zero if TLABs
     * are not {@linkplain #useTLABOption enabled}.
     */
    private static final VmThreadLocal TLAB_MARK
        = new VmThreadLocal("TLAB_MARK", Kind.WORD, "SemiSpace: allocation mark of current TLAB, zero if not used");

    /**
     * Thread-local used to disable allocation per thread.
     */
    private static final VmThreadLocal ALLOCATION_DISABLED
        = new VmThreadLocal("TLAB_DISABLED", Kind.WORD, "SemiSpace: disables per thread allocation if non-zero");

    /**
     * The fast, inline path for allocation.
     *
     * @param size the size of memory chunk to be allocated
     * @return an allocated chunk of memory {@code size} bytes in size
     * @throws OutOfMemoryError if the allocation request cannot be satisfied.
     */
    @INLINE
    private Pointer allocate(Size size) {
        final Pointer enabledVmThreadLocals = VmThread.currentVmThreadLocals().getWord(VmThreadLocal.SAFEPOINTS_ENABLED_THREAD_LOCALS.index).asPointer();
        final Pointer oldAllocationMark = enabledVmThreadLocals.getWord(TLAB_MARK.index).asPointer();
        final Pointer cell = DebugHeap.adjustForDebugTag(oldAllocationMark);
        final Pointer end = cell.plus(size);
        if (end.greaterThan(enabledVmThreadLocals.getWord(TLAB_TOP.index).asAddress())) {
            // This path will always be taken if TLAB allocation is not enabled
            return allocateSlowPath(size);
        }
        enabledVmThreadLocals.setWord(TLAB_MARK.index, end);
        return cell;
    }

    /**
     * The slow path for allocation. This will always be taken when not using TLABs which is fine as the cost of the
     * compare-and-swap on {@link #allocationMark} will dominate.
     *
     * @param size the requested allocation size
     * @return the address of the allocated cell. Space for the {@linkplain DebugHeap#writeCellTag(Pointer) debug tag}
     *         will have been reserved immediately before the allocated cell.
     * @throws OutOfMemoryError if the allocation request cannot be satisfied.
     */
    @NEVER_INLINE
    private Pointer allocateSlowPath(Size size) {
        if (!ALLOCATION_DISABLED.getConstantWord().isZero()) {
            Log.print("Trying to allocate ");
            Log.print(size.toLong());
            Log.print(" bytes on thread ");
            Log.printVmThread(VmThread.current(), false);
            Log.println(" while allocation is disabled");
            FatalError.unexpected("Trying to allocate while allocation is disabled");
        }
        if (useTLAB) {
            if (size.greaterEqual(tlabSize)) {
                // Allocate large objects (i.e. those that won't fit in a TLAB) directly on the heap.
                // This means the next allocation will also take the slow path.
                return retryAllocate(size, true);
            }

            final Size tlabSize = this.tlabSize;
            final Pointer tlab = retryAllocate(tlabSize, false);
            final Pointer tlabTop;
            final Pointer enabledVmThreadLocals = VmThread.currentVmThreadLocals().getWord(SAFEPOINTS_ENABLED_THREAD_LOCALS.index).asPointer();
            if (MaxineVM.isDebug()) {
                padTLAB(enabledVmThreadLocals);
            }
            tlabTop = tlab.plus(tlabSize);

            final Pointer cell = DebugHeap.adjustForDebugTag(tlab);
            enabledVmThreadLocals.setWord(TLAB_TOP.index, tlabTop);
            enabledVmThreadLocals.setWord(TLAB_MARK.index, cell.plus(size));
            if (Heap.traceAllocation()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.printVmThread(VmThread.current(), false);
                Log.print(": Allocated TLAB at ");
                Log.print(tlab);
                Log.print(" [TOP=");
                Log.print(tlabTop);
                Log.print(", end=");
                Log.print(tlab.plus(tlabSize));
                Log.print(", size=");
                Log.print(tlabSize.toInt());
                Log.println("]");
                Log.unlock(lockDisabledSafepoints);
            }
            return cell;
        }
        return retryAllocate(size, true);
    }

    /**
     * Allocates a cell from the global heap, using compare-and-swap to resolve any thread race condition.
     *
     * If allocation fails in this routine, then a garbage collection is performed. If a collection
     * does not free up enough space to satisfy the allocation request, then the heap is expanded.
     * If there is still not enough space after heap expansion, a {@link OutOfMemoryError} is thrown.
     *
     * @param size the requested cell size to be allocated
     * @param adjustForDebugTag specifies if an extra word is to be reserved before the cell for the debug tag word
     * @throws OutOfMemoryError if the allocation request cannot be satisfied.
     */
    @NEVER_INLINE
    private Pointer retryAllocate(Size size, boolean adjustForDebugTag) {
        Pointer oldAllocationMark;
        Pointer cell;
        Address end;
        do {
            if (excessiveGCOption.getValue()) {
                Heap.collectGarbage(size);
            }
            oldAllocationMark = allocationMark().asPointer();
            cell = adjustForDebugTag ? DebugHeap.adjustForDebugTag(oldAllocationMark) : oldAllocationMark;
            end = cell.plus(size);
            while (end.greaterThan(top)) {
                if (!Heap.collectGarbage(size)) {
                    /*
                     * The OutOfMemoryError condition happens when we cannot satisfy a request after running a garbage collection and we
                     * cannot grow the heap any further (i.e. we are at the limit set by -Xmx). In that case we raise 'top' to the actual end of
                     * the active space and set 'inSafetyZone' to true. If this happens recursively we fast fail the VM via MaxineVM.native_exit.
                     * On the other hand if a subsequent GC manages to find enough space to allow the safety zone to be re-established
                     * we set 'inSafetyZone' to false.
                     */
                    if (inSafetyZone) {
                        FatalError.unexpected("Out of memory again after throwing OutOfMemoryError");
                    } else {
                        // Use the safety region to do the throw
                        top = top.plus(safetyZoneSize);
                        inSafetyZone = true;
                        // This new will now be ok
                        if (Heap.verbose()) {
                            Log.println("Throwing OutOfMemoryError");
                        }
                        throw new OutOfMemoryError();
                    }
                }
                oldAllocationMark = allocationMark().asPointer();
                cell = adjustForDebugTag ? DebugHeap.adjustForDebugTag(oldAllocationMark) : oldAllocationMark;
                end = cell.plus(size);
            }
        } while (allocationMark.compareAndSwap(oldAllocationMark, end) != oldAllocationMark);
        return cell;
    }

    /**
     * Inserts {@linkplain DebugHeap#writeCellPadding(Pointer, int) padding} into the unused portion of a thread's TLAB.
     * This is required if {@linkplain DebugHeap#verifyRegion(String, Pointer, Address, MemoryRegion, PointerOffsetVisitor) verification}
     * of the heap will be performed.
     *
     * @param enabledVmThreadLocals the pointer to the safepoint-enabled VM thread locals for the thread whose TLAB is
     *            to be padded
     */
    static void padTLAB(Pointer enabledVmThreadLocals) {
        final Pointer tlabTop = enabledVmThreadLocals.getWord(TLAB_TOP.index).asPointer();
        if (!tlabTop.isZero()) {
            final Pointer tlabMark = enabledVmThreadLocals.getWord(TLAB_MARK.index).asPointer();
            final int padWords = DebugHeap.writeCellPadding(tlabMark, tlabTop);
            if (Heap.traceAllocation()) {
                final boolean lockDisabledSafepoints = Log.lock();
                final VmThread vmThread = UnsafeLoophole.cast(enabledVmThreadLocals.getReference(VM_THREAD.index).toJava());
                Log.printVmThread(vmThread, false);
                Log.print(": Placed TLAB padding at ");
                Log.print(tlabMark);
                Log.print(" [words=");
                Log.print(padWords);
                Log.println("]");
                Log.unlock(lockDisabledSafepoints);
            }
        }
    }

    /**
     * A procedure for resetting the TLAB of a thread.
     */
    static class ResetTLAB implements Pointer.Procedure {

        public void run(Pointer vmThreadLocals) {
            final Pointer enabledVmThreadLocals = vmThreadLocals.getWord(VmThreadLocal.SAFEPOINTS_ENABLED_THREAD_LOCALS.index).asPointer();
            if (Heap.traceAllocation()) {
                final Pointer tlabTop = enabledVmThreadLocals.getWord(TLAB_TOP.index).asPointer();
                final Pointer tlabMark = enabledVmThreadLocals.getWord(TLAB_MARK.index).asPointer();
                final VmThread vmThread = UnsafeLoophole.cast(enabledVmThreadLocals.getReference(VM_THREAD.index).toJava());
                final boolean lockDisabledSafepoints = Log.lock();
                Log.printVmThread(vmThread, false);
                Log.print(": Resetting TLAB [TOP=");
                Log.print(tlabTop);
                Log.print(", MARK=");
                Log.print(tlabMark);
                Log.println("]");
                Log.unlock(lockDisabledSafepoints);
            }
            if (MaxineVM.isDebug()) {
                padTLAB(enabledVmThreadLocals);
            }
            enabledVmThreadLocals.setWord(TLAB_TOP.index, Address.zero());
            enabledVmThreadLocals.setWord(TLAB_MARK.index, Address.zero());
        }
    }

    @Override
    public void disableAllocationForCurrentThread() {
        final Pointer vmThreadLocals = VmThread.currentVmThreadLocals();
        final Address value = ALLOCATION_DISABLED.getConstantWord(vmThreadLocals).asAddress();
        ALLOCATION_DISABLED.setConstantWord(vmThreadLocals, value.plus(1));
        if (MaxineVM.isDebug()) {
            // Need to pad the current TLAB in case a GC occurs while allocation is disabled on the current thread.
            final Pointer enabledVmThreadLocals = vmThreadLocals.getWord(VmThreadLocal.SAFEPOINTS_ENABLED_THREAD_LOCALS.index).asPointer();
            padTLAB(enabledVmThreadLocals);
        }
        TLAB_TOP.setVariableWord(vmThreadLocals, Address.zero());
    }

    @Override
    public void enableAllocationForCurrentThread() {
        final Pointer vmThreadLocals = VmThread.currentVmThreadLocals();
        final Address value = ALLOCATION_DISABLED.getConstantWord(vmThreadLocals).asAddress();
        if (value.isZero()) {
            FatalError.unexpected("Unbalanced calls to disable/enable allocation for current thread");
        }
        ALLOCATION_DISABLED.setConstantWord(vmThreadLocals, value.minus(1));
    }


    @INLINE
    @NO_SAFEPOINTS("object allocation and initialization must be atomic")
    public Object createArray(DynamicHub dynamicHub, int length) {
        final Size size = Layout.getArraySize(dynamicHub.classActor.componentClassActor().kind, length);
        final Pointer cell = allocate(size);
        return Cell.plantArray(cell, size, dynamicHub, length);
    }

    @INLINE
    @NO_SAFEPOINTS("object allocation and initialization must be atomic")
    public Object createTuple(Hub hub) {
        final Pointer cell = allocate(hub.tupleSize);
        return Cell.plantTuple(cell, hub);
    }

    @NO_SAFEPOINTS("object allocation and initialization must be atomic")
    public Object createHybrid(DynamicHub hub) {
        final Size size = hub.tupleSize;
        final Pointer cell = allocate(size);
        return Cell.plantHybrid(cell, size, hub);
    }

    @NO_SAFEPOINTS("object allocation and initialization must be atomic")
    public Hybrid expandHybrid(Hybrid hybrid, int length) {
        final Size newSize = Layout.hybridLayout().getArraySize(length);
        final Pointer newCell = allocate(newSize);
        return Cell.plantExpandedHybrid(newCell, newSize, hybrid, length);
    }

    @NO_SAFEPOINTS("object allocation and initialization must be atomic")
    public Object clone(Object object) {
        final Size size = Layout.size(Reference.fromJava(object));
        final Pointer cell = allocate(size);
        return Cell.plantClone(cell, size, object);
    }

    public boolean contains(Address address) {
        return fromSpace.contains(address);
    }

    public void runFinalization() {
    }

    @INLINE
    public boolean pin(Object object) {
        return false;
    }

    @INLINE
    public void unpin(Object object) {
    }

    @INLINE
    public boolean isPinned(Object object) {
        return false;
    }

    /**
     * Verifies invariants for memory spaces (i.e. heap, code caches, thread stacks) that contain
     * objects and/or object references.
     *
     * @param when a description of the current GC phase
     */
    private void verifyObjectSpaces(String when) {
        if (Heap.traceGCPhases()) {
            Log.print("Verifying object spaces ");
            Log.println(when);
        }
        heapRootsVerifier.run();

        DebugHeap.verifyRegion(toSpace.description(), toSpace.start().asPointer(), allocationMark(), toSpace, gripVerifier);
        Code.visitReferences(gripVerifier);

        if (Heap.traceGCPhases()) {
            Log.print("Verifying object spaces ");
            Log.print(when);
            Log.println(": DONE");
        }
    }

    private void logSpaces() {
        if (Heap.verbose()) {
            logSpace(fromSpace);
            logSpace(toSpace);
            Log.print("top "); Log.print(top);
            Log.print(", allocation mark ");
            Log.println(allocationMark());
        }
    }

    private void logSpace(SemiSpaceMemoryRegion space) {
        Log.print(space.description());
        Log.print(" start "); Log.print(space.start());
        Log.print(", end "); Log.print(space.end());
        Log.print(", size "); Log.print(space.size());
        Log.println("");
    }

    private synchronized boolean shrink(Size amount) {
        final Size pageAlignedAmount = VirtualMemory.pageAlign(amount.asAddress()).asSize().dividedBy(2);
        logSpaces();
        executeCollectorThread();
        if (immediateFreeSpace().greaterEqual(pageAlignedAmount)) {
            // give back part of the existing spaces
            if (Heap.verbose()) {
                logSpaces();
            }
            final int amountAsInt = pageAlignedAmount.toInt();
            fromSpace.setSize(fromSpace.size().minus(amountAsInt));
            toSpace.setSize(toSpace.size().minus(amountAsInt));
            top = top.minus(amountAsInt);
            VirtualMemory.deallocate(fromSpace.end(), pageAlignedAmount, VirtualMemory.Type.HEAP);
            VirtualMemory.deallocate(toSpace.end(), pageAlignedAmount, VirtualMemory.Type.HEAP);
            logSpaces();
            return true;
        }
        return false;
    }

    @Override
    public boolean decreaseMemory(Size amount) {
        return shrink(amount);
    }

    @Override
    public synchronized boolean increaseMemory(Size amount) {
        /* The conservative assumption is that "amount" is the total amount that we could
         * allocate. Since we can't deallocate our existing spaces until we know we can allocate
         * the new ones, our new spaces cannot be greater than amount/2 in size.
         * This could be smaller than the existing spaces so we need to check.
         * It's unfortunate but that's the nature of the semispace scheme.
         */
        final Size pageAlignedAmount = VirtualMemory.pageAlign(amount.asAddress()).asSize().dividedBy(2);
        if (pageAlignedAmount.greaterThan(fromSpace.size())) {
            // grow adds the current space size to the amount in the grow policy
            increaseGrowPolicy.setAmount(pageAlignedAmount.minus(fromSpace.size()));
            return grow(increaseGrowPolicy);
        }
        return false;
    }

    @Override
    public void finalize(MaxineVM.Phase phase) {
        if (MaxineVM.isPrototyping()) {
            StopTheWorldGCDaemon.checkInvariants();
        }
        if (MaxineVM.Phase.RUNNING == phase) {
            if (Heap.traceGCTime()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("Timings (");
                Log.print(TimerUtil.getHzSuffix(HeapScheme.GC_TIMING_CLOCK));
                Log.print(") for all GC: clear & initialize=");
                Log.print(clearTimer.getElapsedTime());
                Log.print(", root scan=");
                Log.print(rootScanTimer.getElapsedTime());
                Log.print(", boot heap scan=");
                Log.print(bootHeapScanTimer.getElapsedTime());
                Log.print(", code scan=");
                Log.print(codeScanTimer.getElapsedTime());
                Log.print(", copy=");
                Log.print(copyTimer.getElapsedTime());
                Log.print(", weak refs=");
                Log.print(weakRefTimer.getElapsedTime());
                Log.print(", total=");
                Log.println(gcTimer.getElapsedTime());
                Log.unlock(lockDisabledSafepoints);
            }
        }
    }

    @INLINE
    public void writeBarrier(Reference from, Reference to) {
        // do nothing.
    }

    /**
     * The policy for how to grow the heap.
     */
    private abstract class GrowPolicy {
        /**
         * Returns the new size given the old.
         * @param current the current size
         * @return the new size
         */
        abstract Size growth(Size current);
    }

    private class DoubleGrowPolicy extends GrowPolicy {
        @Override
        Size growth(Size current) {
            return current.times(2);
        }
    }

    private class LinearGrowPolicy extends GrowPolicy {
        int amount;

        LinearGrowPolicy(String s) {
            amount = Heap.initialSize().toInt();
        }

        LinearGrowPolicy() {
        }

        @Override
        Size growth(Size current) {
            return current.plus(amount);
        }

        void setAmount(int amount) {
            this.amount = amount;
        }

        void setAmount(Size amount) {
            this.amount = amount.toInt();
        }
    }
}
