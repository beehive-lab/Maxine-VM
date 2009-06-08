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

import com.sun.max.annotate.*;
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
import com.sun.max.vm.type.*;

/**
 * A simple semi-space scavenger heap, mainly for testing.
 *
 * No, we do NOT share code with other implementations here,
 * even if this means duplication of effort.
 * This code base is supposed to remain stable,
 * as a reliable fallback position.
 * Refactoring of whatever other fancy memory management library
 * must not damage the functionality here.
 *
 * @author Bernd Mathiske
 * @author Sunil Soman
 */
public final class SemiSpaceHeapScheme extends HeapSchemeAdaptor implements HeapScheme, CellVisitor {

    private static final VMBooleanXXOption _virtualAllocOption =
        register(new VMBooleanXXOption("-XX:-SemiSpaceUseVirtualMemory", "Allocate memory for GC using mmap instead of malloc."), MaxineVM.Phase.PRISTINE);
    private static final int DEFAULT_SAFETY_ZONE_SIZE = 6144;  // empirically determined to be sufficient for simple VM termination after OutOfMemory condition
    private static final VMIntOption _safetyZoneSizeOption =
        register(new VMIntOption("-XX:SemiSpaceGCSafetyZoneSize", DEFAULT_SAFETY_ZONE_SIZE, "Safety zone size in bytes."), MaxineVM.Phase.PRISTINE);
    private static final VMStringOption _growPolicyOption =
        register(new VMStringOption("-XX:SemiSpaceGCGrowPolicy=", false, "Double", "Grow policy for heap (Linear|Double)."), MaxineVM.Phase.STARTING);

    private final PointerIndexVisitor _pointerIndexGripVerifier = new PointerIndexVisitor() {
        @Override
        public void visitPointerIndex(Pointer pointer, int wordIndex) {
            DebugHeap.verifyGripAtIndex(pointer, wordIndex * Kind.REFERENCE.size(), pointer.getGrip(wordIndex), _toSpace);
        }
    };

    private final PointerOffsetVisitor _pointerOffsetGripVerifier = new PointerOffsetVisitor() {
        public void visitPointerOffset(Pointer pointer, int offset) {
            DebugHeap.verifyGripAtIndex(pointer, offset, pointer.readGrip(offset), _toSpace);
        }
    };

    private final PointerIndexVisitor _pointerIndexGripUpdater = new PointerIndexVisitor() {
        @Override
        public void visitPointerIndex(Pointer pointer, int wordIndex) {
            final Grip oldGrip = pointer.getGrip(wordIndex);
            final Grip newGrip = mapGrip(oldGrip);
            if (newGrip != oldGrip) {
                pointer.setGrip(wordIndex, newGrip);
            }
        }
    };

    private final PointerOffsetVisitor _pointerOffsetGripUpdater = new PointerOffsetVisitor() {
        public void visitPointerOffset(Pointer pointer, int offset) {
            final Grip oldGrip = pointer.readGrip(offset);
            final Grip newGrip = mapGrip(oldGrip);
            if (newGrip != oldGrip) {
                pointer.writeGrip(offset, newGrip);
            }
        }
    };

    private final SpecialReferenceManager.GripForwarder _gripForwarder = new SpecialReferenceManager.GripForwarder() {
        public boolean isReachable(Grip grip) {
            final Pointer origin = grip.toOrigin();
            if (_fromSpace.contains(origin)) {
                final Grip forwardGrip = Layout.readForwardGrip(origin);
                if (forwardGrip.isZero()) {
                    return false;
                }
            }
            return true;
        }
        public Grip getForwardGrip(Grip grip) {
            final Pointer origin = grip.toOrigin();
            if (_fromSpace.contains(origin)) {
                return Layout.readForwardGrip(origin);
            }
            return grip;
        }
    };

    // The Sequential Heap Root Scanner is actually the "thread crawler" which will identify the
    // roots out of the threads' stacks. Here we create one for the actual scanning (_heapRootsScanner)
    // and one for the verification (_heapRootsVerifier)
    private final SequentialHeapRootsScanner _heapRootsScanner = new SequentialHeapRootsScanner(this, _pointerIndexGripUpdater);
    private final SequentialHeapRootsScanner _heapRootsVerifier = new SequentialHeapRootsScanner(this, _pointerIndexGripVerifier);

    private StopTheWorldDaemon _collectorThread;

    private SemiSpaceMemoryRegion _fromSpace = new SemiSpaceMemoryRegion("Heap-From");
    private SemiSpaceMemoryRegion _toSpace = new SemiSpaceMemoryRegion("Heap-To");
    private SemiSpaceMemoryRegion _growFromSpace = new SemiSpaceMemoryRegion("Heap-From-Grow");  // used while growing the heap
    private SemiSpaceMemoryRegion _growToSpace = new SemiSpaceMemoryRegion("Heap-To-Grow");          // used while growing the heap
    private static int _safetyZoneSize = DEFAULT_SAFETY_ZONE_SIZE;  // space reserved to allow throw OutOfMemory to complete
    private GrowPolicy _growPolicy;
    private LinearGrowPolicy _increaseGrowPolicy;
    private Address _top;                                         // top of allocatable space (less safety zone)
    private volatile Address _allocationMark;                     // current allocation point

    @CONSTANT_WHEN_NOT_ZERO
    private Pointer _allocationMarkPointer;

    // Create timing facilities.
    private final TimerMetric _clearTimer = new TimerMetric(new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK));
    private final TimerMetric _gcTimer = new TimerMetric(new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK));
    private final TimerMetric _rootScanTimer = new TimerMetric(new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK));
    private final TimerMetric _bootHeapScanTimer = new TimerMetric(new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK));
    private final TimerMetric _codeScanTimer = new TimerMetric(new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK));
    private final TimerMetric _copyTimer = new TimerMetric(new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK));
    private final TimerMetric _weakRefTimer = new TimerMetric(new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK));

    private int _numberOfGarbageCollectionInvocations;

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

    // The heart of the collector.
    // Performs the actual Garbage Collection
    private final Runnable _collect = new Runnable() {
        public void run() {
            try {
                if (vmConfiguration().debugging()) {
                    // Pre-verification of the heap.
                    verifyHeap("before GC");
                }

                ++_numberOfGarbageCollectionInvocations;
                TeleHeapInfo.beforeGarbageCollection();

                VMConfiguration.hostOrTarget().monitorScheme().beforeGarbageCollection();

                startTimer(_gcTimer);

                startTimer(_clearTimer);
                swapSemiSpaces(); // Swap semi-spaces. From--> To and To-->From
                stopTimer(_clearTimer);

                if (Heap.traceGCRootScanning()) {
                    Log.println("Scanning roots...");
                }
                startTimer(_rootScanTimer);
                _heapRootsScanner.run(); // Start scanning the reachable objects from my roots.
                stopTimer(_rootScanTimer);

                if (Heap.traceGC()) {
                    Log.println("Scanning boot heap...");
                }
                startTimer(_bootHeapScanTimer);
                scanBootHeap();
                stopTimer(_bootHeapScanTimer);

                if (Heap.traceGC()) {
                    Log.println("Scanning code...");
                }
                startTimer(_codeScanTimer);
                scanCode();
                stopTimer(_codeScanTimer);

                if (Heap.traceGC()) {
                    Log.println("Moving reachable...");
                }

                startTimer(_copyTimer);
                moveReachableObjects();
                stopTimer(_copyTimer);

                if (Heap.traceGC()) {
                    Log.println("Processing weak references...");
                }

                startTimer(_weakRefTimer);
                SpecialReferenceManager.processDiscoveredSpecialReferences(_gripForwarder);
                stopTimer(_weakRefTimer);
                stopTimer(_gcTimer);

                // Bring the inspectable mark up to date, since it is not updated during the move.
                _toSpace.setAllocationMark(_allocationMark); // for debugging

                VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();

                if (vmConfiguration().debugging()) {
                    verifyHeap("after GC");
                }

                TeleHeapInfo.afterGarbageCollection();

                if (Heap.traceGCTime()) {
                    final boolean lockDisabledSafepoints = Log.lock();
                    Log.print("Timings (");
                    Log.print(TimerUtil.getHzSuffix(HeapScheme.GC_TIMING_CLOCK));
                    Log.print(") for GC ");
                    Log.print(_numberOfGarbageCollectionInvocations);
                    Log.print(": clear & initialize=");
                    Log.print(_clearTimer.getLastElapsedTime());
                    Log.print(", root scan=");
                    Log.print(_rootScanTimer.getLastElapsedTime());
                    Log.print(", boot heap scan=");
                    Log.print(_bootHeapScanTimer.getLastElapsedTime());
                    Log.print(", code scan=");
                    Log.print(_codeScanTimer.getLastElapsedTime());
                    Log.print(", copy=");
                    Log.print(_copyTimer.getLastElapsedTime());
                    Log.print(", weak refs=");
                    Log.print(_weakRefTimer.getLastElapsedTime());
                    Log.print(", total=");
                    Log.println(_gcTimer.getLastElapsedTime());
                    Log.unlock(lockDisabledSafepoints);
                }
            } catch (Throwable throwable) {
                FatalError.unexpected("Exception during GC", throwable);
            }
        }
    };

    @INLINE
    /**
     * Attempts to allocate memory of given size for given space.
     * If successful sets region start and size.
     */
    private static Address allocateSpace(SemiSpaceMemoryRegion space, Size size) {
        final Address base = _virtualAllocOption.isPresent() ? VirtualMemory.allocate(size, VirtualMemory.Type.HEAP) : Memory.allocate(size);
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
        if (_virtualAllocOption.isPresent()) {
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


    @Override
    public void initialize(MaxineVM.Phase phase) {
        if (phase == MaxineVM.Phase.PRISTINE) {
            final Size size = Heap.initialSize().dividedBy(2);

            _safetyZoneSize = _safetyZoneSizeOption.getValue();
            if (allocateSpace(_fromSpace, size).isZero() || allocateSpace(_toSpace, size).isZero()) {
                Log.print("Could not allocate object heap of size ");
                Log.print(size.toLong());
                Log.println();
                FatalError.crash("Insufficient memory to initialize SemiSpaceHeapScheme");
            }

            _allocationMark = _toSpace.start();
            _top = _toSpace.end().minus(_safetyZoneSize);

            _allocationMarkPointer = ClassActor.fromJava(SemiSpaceHeapScheme.class).findLocalInstanceFieldActor("_allocationMark").pointer(this);

            // From now on we can allocate

            TeleHeapInfo.registerMemoryRegions(_toSpace, _fromSpace);
        } else if (phase == MaxineVM.Phase.STARTING) {
            final String growPolicy = _growPolicyOption.getValue();
            if (growPolicy.equals("Double")) {
                _growPolicy = new DoubleGrowPolicy();
            } else if (growPolicy.startsWith("Linear")) {
                _growPolicy = new LinearGrowPolicy(growPolicy);
            } else {
                Log.print("Unknown heap growth policy, using default policy");
                _growPolicy = new DoubleGrowPolicy();
            }
            _increaseGrowPolicy = new LinearGrowPolicy();
            _collectorThread = new StopTheWorldDaemon("GC", _collect);
        }
    }

    public SemiSpaceHeapScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    private void swapSemiSpaces() {
        final Address oldFromSpaceStart = _fromSpace.start();
        final Size oldFromSpaceSize = _fromSpace.size();

        _fromSpace.setStart(_toSpace.start());
        _fromSpace.setSize(_toSpace.size());
        _fromSpace.setAllocationMark(_toSpace.getAllocationMark()); // for debugging

        _toSpace.setStart(oldFromSpaceStart);
        _toSpace.setSize(oldFromSpaceSize);
        _toSpace.setAllocationMark(_toSpace.start());  // for debugging

        _allocationMark = _toSpace.start();
        _top = _toSpace.end();
        // If we are currently using the safety zone, we must not install it in the swapped space
        // as that could cause gcAllocate to fail trying to copying too much live data.
        if (!_inSafetyZone) {
            _top = _top.minus(_safetyZoneSize);
        }
    }

    public boolean isGcThread(Thread thread) {
        return thread instanceof StopTheWorldDaemon;
    }

    public int adjustedCardTableShift() {
        return -1;
    }

    public int auxiliarySpaceSize(int bootImageSize) {
        return 0;
    }

    public void initializeAuxiliarySpace(Pointer primordialVmThreadLocals, Pointer auxiliarySpace) {
    }

    public void initializeVmThread(Pointer vmThreadLocals) {
    }


    private Size immediateFreeSpace() {
        return _top.minus(_allocationMark).asSize();
    }

    private Grip mapGrip(Grip grip) {
        final Pointer fromOrigin = grip.toOrigin();
        if (VMConfiguration.hostOrTarget().debugging()) {
            if (!(grip.isZero() || _fromSpace.contains(fromOrigin) || _toSpace.contains(fromOrigin) || Heap.bootHeapRegion().contains(fromOrigin) || Code.contains(fromOrigin))) {
                Log.print("invalid grip: ");
                Log.print(grip.toOrigin().asAddress());
                Log.println();
                FatalError.unexpected("invalid grip");
            }
            DebugHeap.checkGripTag(grip);
        }
        if (_fromSpace.contains(fromOrigin)) {
            final Grip forwardGrip = Layout.readForwardGrip(fromOrigin);
            if (!forwardGrip.isZero()) {
                return forwardGrip;
            }
            final Pointer fromCell = Layout.originToCell(fromOrigin);
            final Size size = Layout.size(fromOrigin);
            final Pointer toCell = gcAllocate(size);
            if (VMConfiguration.hostOrTarget().debugging()) {
                DebugHeap.writeCellTag(toCell);
            }

            if (Heap.traceGC()) {
                final boolean lockDisabledSafepoints = Log.lock();
                final Hub hub = UnsafeLoophole.cast(Layout.readHubReference(grip).toJava());
                Log.print("Forwarding ");
                Log.print(hub.classActor().name().string());
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
        final SpecificLayout specificLayout = hub.specificLayout();
        if (specificLayout.isTupleLayout()) {
            TupleReferenceMap.visitOriginOffsets(hub, origin, _pointerOffsetGripUpdater);
            if (hub.isSpecialReference()) {
                SpecialReferenceManager.discoverSpecialReference(Grip.fromOrigin(origin));
            }
            return cell.plus(hub.tupleSize());
        }
        if (specificLayout.isHybridLayout()) {
            TupleReferenceMap.visitOriginOffsets(hub, origin, _pointerOffsetGripUpdater);
        } else if (specificLayout.isReferenceArrayLayout()) {
            scanReferenceArray(origin);
        }
        return cell.plus(Layout.size(origin));
    }

    private void moveReachableObjects() {
        Pointer cell = _toSpace.start().asPointer();
        while (cell.lessThan(_allocationMark)) {
            cell = DebugHeap.checkDebugCellTag(cell);
            cell = visitCell(cell);
        }
    }

    /**
     * This option exists only to measure the performance effect of using a reference map for the boot heap.
     */
    private static final VMBooleanXXOption _useBootHeapRefmap = register(new VMBooleanXXOption("-XX:-UseBootHeapRefmap", "Do not use the boot heap reference map when scanning the boot heap."), MaxineVM.Phase.STARTING);

    private void scanBootHeap() {
        if (!_useBootHeapRefmap.getValue()) {
            Heap.bootHeapRegion().visitPointers(_pointerIndexGripUpdater);
        } else {
            Heap.bootHeapRegion().visitCells(this);
        }
    }

    private void scanCode() {
        // All objects in the boot code region are immutable
        final boolean includeBootCode = false;
        Code.visitCells(this, includeBootCode);
    }

    private boolean cannotGrow() {
        return _fromSpace.size().isZero() || _fromSpace.size().greaterEqual(Heap.maxSize());
    }

    /**
     * Grow the semispaces to be of larger size.
     * @param preGc true if prior to executing collector thread to copy _toSpace  to (grown) _fromSpace
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
            final Size size = Size.min(growPolicy.growth(_fromSpace.size()), Heap.maxSize());
            if (preGc && Heap.verbose()) {
                Log.print("...new heap size: ");
                Log.println(size.toLong());
            }
            final Address fromBase = allocateSpace(_growFromSpace, size);
            final Address tempBase = allocateSpace(_growToSpace, size);
            if (fromBase.isZero() || tempBase.isZero()) {
                if (!fromBase.isZero()) {
                    deallocateSpace(_growFromSpace);
                }
                if (Heap.verbose()) {
                    Log.println("...grow failed, can't allocate spaces");
                }
                return false;
            }
            // return memory in _fromSpace
            deallocateSpace(_fromSpace);
            copySpaceState(_growFromSpace, _fromSpace);
        } else {
            // executing the collector thread swapped the spaces
            // so we are again updating _fromSpace but with _growToSpace.
            deallocateSpace(_fromSpace);
            copySpaceState(_growToSpace, _fromSpace);
        }
        if (preGc && Heap.verbose()) {
            Log.println("...grow ok");
        }
        return true;
    }

    @INLINE
    private void executeCollectorThread() {
        if (!Heap.gcDisabled()) {
            _collectorThread.execute();
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
        executeCollectorThread();
        if (immediateFreeSpace().greaterEqual(requestedFreeSpace)) {
            // check to see if we can reset safety zone
            if (_inSafetyZone) {
                if (_top.minus(_allocationMark).greaterThan(_safetyZoneSize)) {
                    _top = _top.minus(_safetyZoneSize);
                    _inSafetyZone = false;
                }
            }
            return true;
        }
        while (grow(_growPolicy)) {
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
        return _allocationMark.minus(_toSpace.start()).asSize();
    }

    private Pointer gcAllocate(Size size) {
        Pointer cell = _allocationMark.asPointer();
        if (VMConfiguration.hostOrTarget().debugging()) {
            cell = cell.plusWords(1);
        }
        _allocationMark = cell.plus(size);
        FatalError.check(_allocationMark.lessThan(_top), "GC allocation overflow");
        return cell;
    }

    private boolean _inSafetyZone; // set after we have thrown OutOfMemoryError and are using the safety zone

    /*
     * The OutOfMemoryError condition happens when we cannot satisfy a request after running a garbage collection and we
     * cannot grow the heap any further (i.e. we are at the limit set by -Xmx). In that case we raise _top to the actual end of
     * the active space and set _inSafetyZone  to true. If this happens recursively we fast fail the VM via MaxineVM.native_exit.
     * On the other hand if a subsequent GC manages to find enough space to allow the safety zone to be re-established
     * we set _inSafetyZone = false.
     */

    @NEVER_INLINE
    private Pointer retryAllocate(Size size) {
        Pointer oldAllocationMark;
        Pointer cell;
        Address end;
        do {
            oldAllocationMark = _allocationMark.asPointer();
            cell = allocateWithDebugTag(oldAllocationMark);
            end = cell.plus(size);
            if (end.greaterThan(_top)) {
                if (!Heap.collectGarbage(size)) {
                    if (_inSafetyZone) {
                        FatalError.crash("out of memory again after throwing OutOfMemoryError");
                    } else {
                        // Use the safety region to do the throw
                        _top = _top.plus(_safetyZoneSize);
                        _inSafetyZone = true;
                        // This new will now be ok
                        throw new OutOfMemoryError();
                    }
                }
                oldAllocationMark = _allocationMark.asPointer();
                cell = allocateWithDebugTag(oldAllocationMark);
                end = cell.plus(size);
            }
        } while (_allocationMarkPointer.compareAndSwapWord(oldAllocationMark, end) != oldAllocationMark);
        return cell;
    }

    @INLINE
    public Pointer allocate(Size size) {
        final Pointer oldAllocationMark = _allocationMark.asPointer();
        Pointer cell = allocateWithDebugTag(oldAllocationMark);
        final Pointer end = cell.plus(size);
        if (end.greaterThan(_top) || _allocationMarkPointer.compareAndSwapWord(oldAllocationMark, end) != oldAllocationMark) {
            cell = retryAllocate(size);
        }
        _toSpace.setAllocationMark(_allocationMark);
        return cell;
    }

    @INLINE
    private Pointer allocateWithDebugTag(Pointer mark) {
        if (VMConfiguration.hostOrTarget().debugging()) {
            return mark.plusWords(1);
        }
        return mark;
    }

    @INLINE
    @NO_SAFEPOINTS("initialization must be atomic")
    public Object createArray(DynamicHub dynamicHub, int length) {
        final Size size = Layout.getArraySize(dynamicHub.classActor().componentClassActor().kind(), length);
        final Pointer cell = allocate(size);
        return Cell.plantArray(cell, size, dynamicHub, length);
    }

    @INLINE
    @NO_SAFEPOINTS("initialization must be atomic")
    public Object createTuple(Hub hub) {
        final Pointer cell = allocate(hub.tupleSize());
        return Cell.plantTuple(cell, hub);
    }

    @NO_SAFEPOINTS("initialization must be atomic")
    public Object createHybrid(DynamicHub hub) {
        final Size size = hub.tupleSize();
        final Pointer cell = allocate(size);
        return Cell.plantHybrid(cell, size, hub);
    }

    @NO_SAFEPOINTS("initialization must be atomic")
    public Hybrid expandHybrid(Hybrid hybrid, int length) {
        final Size newSize = Layout.hybridLayout().getArraySize(length);
        final Pointer newCell = allocate(newSize);
        return Cell.plantExpandedHybrid(newCell, newSize, hybrid, length);
    }

    @NO_SAFEPOINTS("initialization must be atomic")
    public Object clone(Object object) {
        final Size size = Layout.size(Reference.fromJava(object));
        final Pointer cell = allocate(size);
        return Cell.plantClone(cell, size, object);
    }

    public boolean contains(Address address) {
        return _fromSpace.contains(address);
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

    private void verifyHeap(String when) {
        if (Heap.traceGC()) {
            Log.print("Verifying heap ");
            Log.println(when);
        }
        _heapRootsVerifier.run();
        DebugHeap.verifyRegion(_toSpace.start().asPointer(), _allocationMark, _toSpace, _pointerOffsetGripVerifier);
        if (Heap.traceGC()) {
            Log.print("Verifying heap");
            Log.print(when);
            Log.println(": DONE");
        }
    }

    private void logSpaces() {
        if (Heap.verbose()) {
            logSpace(_fromSpace);
            logSpace(_toSpace);
            Log.print("top "); Log.print(_top);
            Log.print(", allocation mark "); Log.println(_allocationMark);
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
            _fromSpace.setSize(_fromSpace.size().minus(amountAsInt));
            _toSpace.setSize(_toSpace.size().minus(amountAsInt));
            _top = _top.minus(amountAsInt);
            VirtualMemory.deallocate(_fromSpace.end(), pageAlignedAmount, VirtualMemory.Type.HEAP);
            VirtualMemory.deallocate(_toSpace.end(), pageAlignedAmount, VirtualMemory.Type.HEAP);
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
        if (pageAlignedAmount.greaterThan(_fromSpace.size())) {
            // grow adds the current space size to the amount in the grow policy
            _increaseGrowPolicy.setAmount(pageAlignedAmount.minus(_fromSpace.size()));
            return grow(_increaseGrowPolicy);
        }
        return false;
    }

    @Override
    public void finalize(MaxineVM.Phase phase) {
        if (MaxineVM.isPrototyping()) {
            StopTheWorldDaemon.checkInvariants();
        }
        if (MaxineVM.Phase.RUNNING == phase) {
            if (Heap.traceGCTime()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("Timings (");
                Log.print(TimerUtil.getHzSuffix(HeapScheme.GC_TIMING_CLOCK));
                Log.print(") for all GC: clear & initialize=");
                Log.print(_clearTimer.getElapsedTime());
                Log.print(", root scan=");
                Log.print(_rootScanTimer.getElapsedTime());
                Log.print(", boot heap scan=");
                Log.print(_bootHeapScanTimer.getElapsedTime());
                Log.print(", code scan=");
                Log.print(_codeScanTimer.getElapsedTime());
                Log.print(", copy=");
                Log.print(_copyTimer.getElapsedTime());
                Log.print(", weak refs=");
                Log.print(_weakRefTimer.getElapsedTime());
                Log.print(", total=");
                Log.println(_gcTimer.getElapsedTime());
                Log.unlock(lockDisabledSafepoints);
            }
        }
    }

    @INLINE
    public void writeBarrier(Reference from, Reference to) {
        // do nothing.
    }

    /*
     * This class encapsulates the policy for how to grow the heap.
     *
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
        int _amount;

        LinearGrowPolicy(String s) {
            _amount = Heap.initialSize().toInt();
        }

        LinearGrowPolicy() {
        }

        @Override
        Size growth(Size current) {
            return current.plus(_amount);
        }

        void setAmount(int amount) {
            _amount = amount;
        }

        void setAmount(Size amount) {
            _amount = amount.toInt();
        }
    }
}
