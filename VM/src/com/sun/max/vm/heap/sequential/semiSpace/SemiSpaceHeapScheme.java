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

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.profile.*;
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
public final class SemiSpaceHeapScheme extends AbstractVMScheme implements HeapScheme, CellVisitor {

    private static final VMOption _virtualAllocOption = new VMOption("-XX:SemiSpaceGC:Virtual", "Use VirtualMemory.allocate", MaxineVM.Phase.PRISTINE);

    private final PointerIndexVisitor _pointerIndexGripVerifier = new PointerIndexVisitor() {
        public void visitPointerIndex(Pointer pointer, int wordIndex) {
            verifyGripAtIndex(pointer, wordIndex * Kind.REFERENCE.size(), pointer.getGrip(wordIndex));
        }
    };

    private final PointerOffsetVisitor _pointerOffsetGripVerifier = new PointerOffsetVisitor() {
        public void visitPointerOffset(Pointer pointer, int offset) {
            verifyGripAtIndex(pointer, offset, pointer.readGrip(offset));
        }
    };

    private final PointerIndexVisitor _pointerIndexGripUpdater = new PointerIndexVisitor() {
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
    private Address _top;
    private volatile Address _allocationMark;

    @CONSTANT_WHEN_NOT_ZERO
    private Pointer _allocationMarkPointer;

    // Create timing facilities.
    private final Timer _clearTimer = new SingleUseTimer(Clock.SYSTEM_MILLISECONDS);
    private final Timer _gcTimer = new SingleUseTimer(Clock.SYSTEM_MILLISECONDS);
    private final Timer _rootScanTimer = new SingleUseTimer(Clock.SYSTEM_MILLISECONDS);
    private final Timer _bootHeapScanTimer = new SingleUseTimer(Clock.SYSTEM_MILLISECONDS);
    private final Timer _codeScanTimer = new SingleUseTimer(Clock.SYSTEM_MILLISECONDS);
    private final Timer _copyTimer = new SingleUseTimer(Clock.SYSTEM_MILLISECONDS);
    private final Timer _weakRefTimer = new SingleUseTimer(Clock.SYSTEM_MILLISECONDS);

    private int _numberOfGarbageCollectionInvocations = 0;

    // The heart of the collector.
    // Performs the actual Garbage Collection
    private final Runnable _collect = new Runnable() {
        public void run() {
            try {
                if (Heap.verbose()) {
                    Log.print("--Before GC--   size: ");
                    Log.println(_allocationMark.minus(_toSpace.start()).toInt());
                }

                // Calls the beforeGarbageCollection() method of the plugged monitor Scheme.
                // Pre-verification of the heap.
                verifyHeap();

                ++_numberOfGarbageCollectionInvocations;
                TeleHeapInfo.beforeGarbageCollection();

                VMConfiguration.hostOrTarget().monitorScheme().beforeGarbageCollection();

                _gcTimer.start();

                _clearTimer.start();
                swapSemiSpaces(); // Swap semi-spaces. From--> To and To-->From
                _clearTimer.stop();

                if (Heap.traceGCRootScanning()) {
                    Log.println("Scanning roots...");
                }
                _rootScanTimer.start();
                _heapRootsScanner.run(); // Start scanning the reachable objects from my roots.
                _rootScanTimer.stop();

                if (Heap.traceGC()) {
                    Log.println("Scanning boot heap...");
                }
                _bootHeapScanTimer.start();
                scanBootHeap();
                _bootHeapScanTimer.stop();

                _codeScanTimer.start();
                scanCode();
                _codeScanTimer.stop();

                if (Heap.traceGC()) {
                    Log.println("Moving reachable...");
                }

                _copyTimer.start();
                moveReachableObjects();
                _copyTimer.stop();

                _weakRefTimer.start();
                SpecialReferenceManager.processDiscoveredSpecialReferences(_gripForwarder);
                _weakRefTimer.stop();
                _gcTimer.stop();

                // Bring the inspectable mark up to date, since it is not updated during the move.
                _toSpace.setAllocationMark(_allocationMark); // for debugging

                VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();

                verifyHeap();

                TeleHeapInfo.afterGarbageCollection();

                if (Heap.traceGC()) {
                    final boolean lockDisabledSafepoints = Log.lock();
                    Log.print("clear & initialize: ");
                    Log.print(TimerUtil.getLastElapsedMilliSeconds(_clearTimer));
                    Log.print("   root scan: ");
                    Log.print(TimerUtil.getLastElapsedMilliSeconds(_rootScanTimer));
                    Log.print("   boot heap scan: ");
                    Log.print(TimerUtil.getLastElapsedMilliSeconds(_bootHeapScanTimer));
                    Log.print("   code scan: ");
                    Log.print(TimerUtil.getLastElapsedMilliSeconds(_codeScanTimer));
                    Log.print("   copy: ");
                    Log.print(TimerUtil.getLastElapsedMilliSeconds(_copyTimer));
                    Log.print("   weak refs: ");
                    Log.print(TimerUtil.getLastElapsedMilliSeconds(_weakRefTimer));
                    Log.println();
                    Log.print("GC <");
                    Log.print(_numberOfGarbageCollectionInvocations);
                    Log.print("> ");
                    Log.print(TimerUtil.getLastElapsedMilliSeconds(_gcTimer));
                    Log.println(" (ms)");

                    Log.println();
                    Log.unlock(lockDisabledSafepoints);
                }
                if (Heap.verbose()) {
                    Log.print("--After  GC--   size: ");
                    Log.println(_allocationMark.minus(_toSpace.start()).toInt());
                }
            } catch (Throwable throwable) {
                FatalError.unexpected(throwable.toString() + " during GC");
            }
        }
    };

    @Override
    public void initialize(MaxineVM.Phase phase) {
        if (phase == MaxineVM.Phase.PRISTINE) {
            final Size size = Heap.initialSize();

            _fromSpace.setSize(size);
            _toSpace.setSize(size);

            final boolean useVirtual = _virtualAllocOption.isPresent();
            final Pointer fromStart = useVirtual ? VirtualMemory.allocate(size, VirtualMemory.Type.HEAP) : Memory.allocate(size);
            final Pointer toStart = useVirtual ? VirtualMemory.allocate(size, VirtualMemory.Type.HEAP) : Memory.allocate(size);
            _fromSpace.setStart(fromStart);
            _toSpace.setStart(toStart);

            if (_fromSpace.start().isZero() || _toSpace.start().isZero()) {
                Log.print("Could not allocate object heap of size ");
                Log.print(size.toLong());
                Log.println();
                Log.println("This is only a very simple GC implementation that uses twice the specified amount");
            }

            _allocationMark = _toSpace.start();
            _top = _toSpace.end();

            _allocationMarkPointer = ClassActor.fromJava(SemiSpaceHeapScheme.class).findLocalInstanceFieldActor("_allocationMark").pointer(this);

            // From now on we can allocate

            TeleHeapInfo.registerMemoryRegions(_toSpace, _fromSpace);
        } else if (phase == MaxineVM.Phase.STARTING) {
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

    private void checkCellTag(Pointer cell) {
        if (VMConfiguration.hostOrTarget().debugging()) {
            if (!DebugHeap.isValidCellTag(cell.getWord(-1))) {
                Log.print("cell: ");
                Log.print(cell);
                Log.print("  origin: ");
                Log.print(Layout.cellToOrigin(cell));
                Log.println();
                FatalError.unexpected("missing object tag");
            }
        }
    }

    private void checkGripTag(Grip grip) {
        if (VMConfiguration.hostOrTarget().buildLevel() == BuildLevel.DEBUG) {
            if (!grip.isZero()) {
                checkCellTag(Layout.originToCell(grip.toOrigin()));
            }
        }
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
            checkGripTag(grip);
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
            if (VMConfiguration.hostOrTarget().debugging()) {
                cell = cell.plusWords(1);
                checkCellTag(cell);
            }
            cell = visitCell(cell);
        }
    }

    private void scanBootHeap() {
        Heap.bootHeapRegion().visitCells(this);
    }

    private void scanCode() {
        Code.visitCells(this);
    }

    private boolean _outOfMemory;

    private boolean reallocateAlternateSpace() {
        if (_fromSpace.size().isZero() || _fromSpace.size().greaterEqual(Heap.maxSize())) {
            _outOfMemory = true;
            return false;
        }
        final boolean useVirtual = _virtualAllocOption.isPresent();
        if (useVirtual) {
            VirtualMemory.deallocate(_fromSpace.start(), _fromSpace.size(), VirtualMemory.Type.HEAP);
        } else {
            Memory.deallocate(_fromSpace.start());
        }
        final Size size = Size.min(_fromSpace.size().times(2), Heap.maxSize());
        if (Heap.verbose()) {
            Log.print("New heap size: ");
            Log.println(size.toLong());
        }
        _fromSpace.setSize(size);
        _fromSpace.setStart(useVirtual ? VirtualMemory.allocate(size, VirtualMemory.Type.HEAP) : Memory.allocate(size));
        if (_fromSpace.size().isZero()) {
            _outOfMemory = true;
            return false;
        }
        return true;
    }

    @INLINE
    private void executeCollectorThread() {
        if (!Heap.gcDisabled()) {
            _collectorThread.execute();
        }
    }
    private boolean grow() {
        if (Heap.verbose()) {
            Log.println("Trying to grow the heap...");
        }
        if (!reallocateAlternateSpace()) {
            return false;
        }
        executeCollectorThread();
        return reallocateAlternateSpace();
    }

    public synchronized boolean collectGarbage(Size requestedFreeSpace) {
        if (_outOfMemory) {
            return false;
        }
        executeCollectorThread();
        if (immediateFreeSpace().greaterEqual(requestedFreeSpace)) {
            return true;
        }
        while (grow()) {
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
        executeCollectorThread();
        return immediateFreeSpace();
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

    private static final OutOfMemoryError _outOfMemoryError = new OutOfMemoryError(); // TODO: create a new one each

    @NEVER_INLINE
    private Pointer retryAllocate(Size size) {
        Pointer oldAllocationMark;
        Pointer cell;
        Address end;
        do {
            oldAllocationMark = _allocationMark.asPointer();
            if (VMConfiguration.hostOrTarget().debugging()) {
                cell = oldAllocationMark.plusWords(1);
            } else {
                cell = oldAllocationMark;
            }
            end = cell.plus(size);
            if (end.greaterThan(_top)) {
                if (!Heap.collectGarbage(size)) {
                    throw _outOfMemoryError;
                }
                oldAllocationMark = _allocationMark.asPointer();
                if (VMConfiguration.hostOrTarget().debugging()) {
                    cell = oldAllocationMark.plusWords(1);
                } else {
                    cell = oldAllocationMark;
                }
                end = cell.plus(size);
            }
        } while (_allocationMarkPointer.compareAndSwapWord(oldAllocationMark, end) != oldAllocationMark);
        return cell;
    }

    @INLINE
    public Pointer allocate(Size size) {
        Pointer cell;
        final Pointer oldAllocationMark = _allocationMark.asPointer();
        if (VMConfiguration.hostOrTarget().debugging()) {
            cell = oldAllocationMark.plusWords(1);
        } else {
            cell = oldAllocationMark;
        }
        final Pointer end = cell.plus(size);
        if (end.greaterThan(_top) || _allocationMarkPointer.compareAndSwapWord(oldAllocationMark, end) != oldAllocationMark) {
            cell = retryAllocate(size);
        }
        _toSpace.setAllocationMark(_allocationMark);
        return cell;
    }

    @INLINE
    @NO_SAFEPOINTS("TODO")
    public Object createArray(DynamicHub dynamicHub, int length) {
        final Size size = Layout.getArraySize(dynamicHub.classActor().componentClassActor().kind(), length);
        final Pointer cell = allocate(size);
        return Cell.plantArray(cell, size, dynamicHub, length);
    }

    @INLINE
    @NO_SAFEPOINTS("TODO")
    public Object createTuple(Hub hub) {
        final Pointer cell = allocate(hub.tupleSize());
        return Cell.plantTuple(cell, hub);
    }

    @NO_SAFEPOINTS("TODO")
    public <Hybrid_Type extends Hybrid> Hybrid_Type createHybrid(DynamicHub hub) {
        final Size size = hub.tupleSize();
        final Pointer cell = allocate(size);

        @JavacSyntax("type checker not able to infer type here")
        final Class<Hybrid_Type> type = null;
        return UnsafeLoophole.cast(type, Cell.plantHybrid(cell, size, hub));
    }

    @NO_SAFEPOINTS("TODO")
    public <Hybrid_Type extends Hybrid> Hybrid_Type expandHybrid(Hybrid_Type hybrid, int length) {
        final Size newSize = Layout.hybridLayout().getArraySize(length);
        final Pointer newCell = allocate(newSize);
        return Cell.plantExpandedHybrid(newCell, newSize, hybrid, length);
    }

    @NO_SAFEPOINTS("TODO")
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

    private void verifyGripAtIndex(Address address, int index, Grip grip) {
        if (grip.isZero()) {
            return;
        }
        checkGripTag(grip);
        final Pointer origin = grip.toOrigin();
        if (!(_toSpace.contains(origin) || Heap.bootHeapRegion().contains(origin) || Code.contains(origin))) {
            Log.print("invalid grip: ");
            Log.print(origin.asAddress());
            Log.print(" @ ");
            Log.print(address);
            Log.print(" + ");
            Log.print(index);
            Log.println();
            FatalError.unexpected("invalid grip");
        }
    }

    private void checkClassActor(ClassActor classActor) {
    }

    private Hub checkHub(Pointer origin) {
        final Grip hubGrip = Layout.readHubGrip(origin);
        FatalError.check(!hubGrip.isZero(), "null hub");
        verifyGripAtIndex(origin, 0, hubGrip); // zero is not strictly correctly here
        final Hub hub = UnsafeLoophole.cast(hubGrip.toJava());

        Hub h = hub;
        if (h instanceof StaticHub) {
            final ClassActor classActor = hub.classActor();
            checkClassActor(h.classActor());
            FatalError.check(classActor.staticHub() == h, "lost static hub");
            h = ObjectAccess.readHub(h);
        }

        for (int i = 0; i < 2; i++) {
            h = ObjectAccess.readHub(h);
        }
        FatalError.check(ObjectAccess.readHub(h) == h, "lost hub hub");
        return hub;
    }

    private void verifyHeap() {
        if (Heap.traceGC()) {
            Log.println("Verifying heap...");
        }
        _heapRootsVerifier.run();
        Pointer cell = _toSpace.start().asPointer();
        while (cell.lessThan(_allocationMark)) {
            if (VMConfiguration.hostOrTarget().debugging()) {
                cell = cell.plusWords(1);
                checkCellTag(cell);
            }

            final Pointer origin = Layout.cellToOrigin(cell);
            final Hub hub = checkHub(origin);

            if (Heap.traceGC()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("Verifying ");
                Log.print(hub.classActor().name().string());
                Log.print(" at ");
                Log.print(cell);
                Log.print(" [");
                Log.print(Layout.size(origin).toInt());
                Log.println(" bytes]");
                Log.unlock(lockDisabledSafepoints);
            }

            final SpecificLayout specificLayout = hub.specificLayout();
            if (specificLayout.isTupleLayout()) {
                TupleReferenceMap.visitOriginOffsets(hub, origin, _pointerOffsetGripVerifier);
                cell = cell.plus(hub.tupleSize());
            } else {
                if (specificLayout.isHybridLayout()) {
                    TupleReferenceMap.visitOriginOffsets(hub, origin, _pointerOffsetGripVerifier);
                } else if (specificLayout.isReferenceArrayLayout()) {
                    final int length = Layout.readArrayLength(origin);
                    for (int index = 0; index < length; index++) {
                        verifyGripAtIndex(origin, index * Kind.REFERENCE.size(), Layout.getGrip(origin, index));
                    }
                }
                cell = cell.plus(Layout.size(origin));
            }
        }
        if (Heap.traceGC()) {
            Log.println("done verifying heap");
        }
    }

    @Override
    public void finalize(MaxineVM.Phase phase) {
    }

    @Override
    @INLINE
    public void writeBarrier(Reference from, Reference to) {
        // do nothing.
    }
}
