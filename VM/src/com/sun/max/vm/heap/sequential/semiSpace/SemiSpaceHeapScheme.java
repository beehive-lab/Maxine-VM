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
/*VCSID=5bea8e4e-c77f-49f0-82c1-8993424030ce*/
package com.sun.max.vm.heap.sequential.semiSpace;


/**
 * A simple semi-space scavenger heap, mainly for testing.
 *
 * @author Bernd Mathiske
 * @author Sunil Soman
 */

public final class SemiSpaceHeapScheme{// extends GripUpdatingHeapScheme implements HeapScheme, CellVisitor {
/*
    public boolean isGcThread(VmThread vmThread) {
        return vmThread.javaThread() instanceof StopTheWorldDaemon;
    }

    public int auxiliarySpaceSize(int bootImageSize) {
        return 0;
    }

    public void initializePrimordialCardTable(Pointer primordialVmThreadLocals, Pointer auxiliarySpace) {
    }

    public void initializeVmThread(VmThread vmThread) {
    }

    private int _numberOfGarbageCollectionInvocations = 0;
    private int _numberOfGarbageCollectionTurnoverCycles = 0;

    // Defines the _pointerIndexGripVerifier which will retrieves the Grips of pointer
    // and the verify them.
    private final PointerIndexVisitor _pointerIndexGripVerifier = new PointerIndexVisitor() {
        public void visitPointerIndex(Pointer pointer, int wordIndex) {
            verifyGripAtIndex(pointer, wordIndex * Kind.REFERENCE.size(), pointer.getGrip(wordIndex));
        }
    };

    // The Sequential Heap Root Scanner is actually the "thread crawlers" which will identify the
    // roots out of the threads' stacks. Here we create one of the actual scanning (_heapRootsScanner)
    // and one for the verification (_heapRootsVerifier)
    private final SequentialHeapRootsScanner _heapRootsVerifier = new SequentialHeapRootsScanner(this, _pointerIndexGripVerifier);
    private final SequentialHeapRootsScanner _heapRootsScanner = new SequentialHeapRootsScanner(this, pointerIndexGripUpdater());

    // Create timing facilities.
    private final Timer _clearTimer = GlobalMetrics.newTimer("Clear", Clock.SYSTEM_MILLISECONDS);
    private final Timer _gcTimer = GlobalMetrics.newTimer("GC", Clock.SYSTEM_MILLISECONDS);
    private final Timer _rootScanTimer = GlobalMetrics.newTimer("Roots scan", Clock.SYSTEM_MILLISECONDS);
    private final Timer _bootHeapScanTimer = GlobalMetrics.newTimer("Boot heap scan", Clock.SYSTEM_MILLISECONDS);
    private final Timer _codeScanTimer = GlobalMetrics.newTimer("Code scan", Clock.SYSTEM_MILLISECONDS);
    private final Timer _copyTimer = GlobalMetrics.newTimer("Copy", Clock.SYSTEM_MILLISECONDS);

    // The heart of the collector.
    // Performs the actual Garbage Collection
    private final Runnable _collect = new Runnable() {

        public void run() {
            // Calls the beforeGarbageCollection() method of the plugged monitor Scheme.
            // Pre-verification of the heap.
            verifyHeap();

            ++_numberOfGarbageCollectionInvocations;
            TeleHeap.beforeGarbageCollection();

            if (Heap.verbose()) {
                Debug.out.print("--Before GC--   size: ");
                Debug.out.println(_allocationMark.minus(_toSpace.start()).toInt());
            }

            VMConfiguration.hostOrTarget().monitorScheme().beforeGarbageCollection();

            _gcTimer.restart();

            _clearTimer.restart();
            swapSemiSpaces(); // Swap semi-spaces. From--> To and To-->From
            _clearTimer.stop();

            if (Heap.traceGC()) {
                Debug.out.println("Scanning roots...");
            }
            _rootScanTimer.restart();
            _heapRootsScanner.run(); // Start scanning the reachable objects from my roots.
            _rootScanTimer.stop();

            if (Heap.traceGC()) {
                Debug.out.println("Scanning boot heap...");
            }
            _bootHeapScanTimer.restart();
            scanBootHeap();
            _bootHeapScanTimer.stop();

            _codeScanTimer.restart();
            scanCode();
            _codeScanTimer.stop();

            if (Heap.traceGC()) {
                Debug.out.println("Moving reachable...");
            }

            _copyTimer.restart();
            moveReachableObjects();
            _copyTimer.stop();
            _gcTimer.stop();
            if (Heap.traceGC()) {
                final DebugPrintStream out = Debug.out;
                final boolean lockDisabledSafepoints = Debug.lock();
                out.print("clear & initialize: ");
                out.print(_clearTimer.getMilliSeconds());
                out.print("   root scan: ");
                out.print(_rootScanTimer.getMilliSeconds());
                out.print("   boot heap scan: ");
                out.print(_bootHeapScanTimer.getMilliSeconds());
                out.print("   code scan: ");
                out.print(_codeScanTimer.getMilliSeconds());
                out.print("   copy: ");
                out.print(_copyTimer.getMilliSeconds());
                out.println();
                out.print("GC <");
                out.print(_numberOfGarbageCollectionInvocations);
                out.print("> ");
                out.print(_gcTimer.getMilliSeconds());
                out.println(" (ms)");

                out.print("--After GC--   bytes copied: ");
                out.print(_allocationMark.minus(_toSpace.start()).toInt());
                out.println();
                Debug.unlock();
            }
            VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();

            verifyHeap();

            TeleHeap.afterGarbageCollection();

            if (Heap.verbose()) {
                Debug.out.println("GC done");
            }
            _numberOfGarbageCollectionTurnoverCycles++;
        }
    };

    private StopTheWorldDaemon _collectorThread;

    private RuntimeMemoryRegion _fromSpace = new RuntimeMemoryRegion(Size.zero(), Size.zero());
    private RuntimeMemoryRegion _toSpace = new RuntimeMemoryRegion(Size.zero(), Size.zero());
    private Address _top;
    private volatile Address _allocationMark;

    @CONSTANT_WHEN_NOT_ZERO
    private Pointer _allocationMarkPointer;

    @Override
    public void initialize(MaxineVM.Phase phase) {
        if (phase == MaxineVM.Phase.PRISTINE) {
            final Size size = Heap.initialSize();

            _fromSpace.setSize(size);
            _toSpace.setSize(size);
            _fromSpace.setStart(Memory.allocate(size));
            _toSpace.setStart(Memory.allocate(size));

            if (_fromSpace.start().isZero() || _toSpace.start().isZero()) {
                final DebugPrintStream err = Debug.err;
                err.print("Could not allocate object heap of size ");
                err.print(size.toLong());
                err.println();
                err.println("This is only a very simple GC implementation that uses twice the specified amount");
            }

            _allocationMark = _toSpace.start();
            _top = _toSpace.end();

            _allocationMarkPointer = ClassActor.fromJava(SemiSpaceHeapScheme.class).findLocalInstanceFieldActor("_allocationMark").pointer(this);

            // From now on we can allocate

            TeleHeap.registerMemoryRegions(_toSpace, _fromSpace);
        } else if (phase == MaxineVM.Phase.STARTING) {
            _collectorThread = new StopTheWorldDaemon("GC", _collect);
            // _heapVerifier = new HeapVerifier(this);
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

        _toSpace.setStart(oldFromSpaceStart);
        _toSpace.setSize(oldFromSpaceSize);

        _allocationMark = _toSpace.start();
        _top = _toSpace.end();
    }

    private Size immediateFreeSpace() {
        return _top.minus(_allocationMark).asSize();
    }

    private void checkCellTag(Pointer cell) {
        if (VMConfiguration.hostOrTarget().debugging()) {
            if (!DebugHeap.isValidCellTag(cell.getWord(-1))) {
                final DebugPrintStream err = Debug.err;
                err.print("cell: ");
                err.print(cell);
                err.print("  origin: ");
                err.print(Layout.cellToOrigin(cell));
                err.println();
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

    @Override
    public Grip mapGrip(Grip grip) {
        final Pointer fromOrigin = grip.toOrigin();
        if (VMConfiguration.hostOrTarget().debugging()) {
            if (!(grip.isZero() || _fromSpace.contains(fromOrigin) || _toSpace.contains(fromOrigin) || Heap.bootHeapRegion().contains(fromOrigin) || Code.contains(fromOrigin))) {
                final DebugPrintStream err = Debug.err;
                err.print("invalid grip: ");
                err.print(grip.toOrigin().asAddress());
                err.println();
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
                DebugHeap.writeCellTag(toCell, Word.width());
            }

            if (Heap.traceGC()) {
                final DebugPrintStream out = Debug.out;
                final boolean lockDisabledSafepoints = Debug.lock();
                final Hub hub = UnsafeLoophole.cast(Layout.readHubReference(grip).toJava());
                out.print("Forwarding ");
                out.print(hub.classActor().name().string());
                out.print(" from ");
                out.print(fromCell);
                out.print(" to ");
                out.print(toCell);
                out.print(" [");
                out.print(size.toInt());
                out.println(" bytes]");
                Debug.unlock();
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
        // Doesn't matter what kind of object it is. Where does this
        // pointer points now? Concerning the nature of the object?
        final Grip oldHubGrip = Layout.readHubGrip(origin); // Reads the hub-Grip of the previously retrieved object.
        // Grips are used for GC purpose.
        final Grip newHubGrip = mapGrip(oldHubGrip);
        if (newHubGrip != oldHubGrip) {
            Layout.writeHubGrip(origin, newHubGrip);
        }
        final Hub hub = UnsafeLoophole.cast(newHubGrip.toJava());
        final SpecificLayout specificLayout = hub.specificLayout();
        if (specificLayout.isTupleLayout()) {
            TupleReferenceMap.visitOriginOffsets(hub, origin, pointerOffsetGripUpdater());
            return cell.plus(hub.tupleSize());
        }
        if (specificLayout.isHybridLayout()) {
            TupleReferenceMap.visitOriginOffsets(hub, origin, pointerOffsetGripUpdater());
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
        Memory.deallocate(_fromSpace.start());
        final Size size = Size.min(_fromSpace.size().times(2), Heap.maxSize());
        if (Heap.verbose()) {
            Debug.out.print("New heap size: ");
            Debug.out.println(size.toLong());
        }
        _fromSpace.setSize(size);
        _fromSpace.setStart(Memory.allocate(size));
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
            Debug.out.println("Trying to grow the heap...");
        }
        if (!reallocateAlternateSpace()) {
            return false;
        }
        executeCollectorThread();
        return reallocateAlternateSpace();
    }

    public synchronized boolean collect(Size requestedFreeSpace) {
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
            Debug.out.println("Out of memory and GC is disabled, exiting");
            MaxineVM.native_exit(1);
        }
        return false;
    }

    public boolean collectGarbage(Size requestedFreeSpace) {
        return collect(requestedFreeSpace);
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

    // time

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
                if (!collect(size)) {
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
    private Pointer allocate(Size size) {
        Pointer cell;
        final Pointer oldAllocationMark = _allocationMark.asPointer();
        if (VMConfiguration.hostOrTarget().debugging()) {
            cell = oldAllocationMark.plusWords(1);
        } else {
            cell = oldAllocationMark;
        }
        final Pointer end = cell.plus(size);
        if (end.greaterThan(_top) || _allocationMarkPointer.compareAndSwapWord(oldAllocationMark, end) != oldAllocationMark) {
            return retryAllocate(size);
        }
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
        // TODO
    }

    public long numberOfGarbageCollectionInvocations() {
        return _numberOfGarbageCollectionInvocations;
    }

    public long numberOfGarbageCollectionTurnoverCycles() {
        return _numberOfGarbageCollectionTurnoverCycles;
    }

    public <Object_Type> boolean flash(Object_Type object, com.sun.max.lang.Procedure<Object_Type> procedure) {
        try {
            Safepoint.disable();
            procedure.run(object);
        } finally {
            Safepoint.enable();
        }
        return true;
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
            Debug.err.print("invalid grip: ");
            Debug.err.print(origin.asAddress());
            Debug.err.print(" @ ");
            Debug.err.print(address);
            Debug.err.print(" + ");
            Debug.err.print(index);
            Debug.err.println();
            FatalError.unexpected("invalid grip");
        }
    }

    private final PointerOffsetVisitor _verifyField = new PointerOffsetVisitor() {

        public void visitPointerOffset(Pointer pointer, int offset) {
            verifyGripAtIndex(pointer, offset, pointer.readGrip(offset));
        }
    };

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
            Debug.out.println("Verifying heap...");
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
            final SpecificLayout specificLayout = hub.specificLayout();
            if (specificLayout.isTupleLayout()) {
                TupleReferenceMap.visitOriginOffsets(hub, origin, _verifyField);
                cell = cell.plus(hub.tupleSize());
            } else {
                if (specificLayout.isHybridLayout()) {
                    TupleReferenceMap.visitOriginOffsets(hub, origin, _verifyField);
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
            Debug.out.println("done verifying heap");
        }
    }

    @Override
    public void scanRegion(Address start, Address end) {
    }

    @Override
    public void writeBarrier(Grip grip) {
    }


    @Override
    public Address adjustedCardTableAddress() {
        // TODO Auto-generated method stub
        return null;
    }
  */
}
