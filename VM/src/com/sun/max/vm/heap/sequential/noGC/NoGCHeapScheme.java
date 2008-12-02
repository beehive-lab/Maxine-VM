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
package com.sun.max.vm.heap.sequential.noGC;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.profile.*;
import com.sun.max.profile.Metrics.*;
import com.sun.max.unsafe.*;
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
 * A heap that lets you allocate, but never reclaims any memory, i.e. never performs a garbage collection.
 * With this setup, you can call System.gc() to test the safepoint mechanism
 * without potential object reference corruption by a presumably broken GC.
 *
 * @author Bernd Mathiske
 */
public final class NoGCHeapScheme extends AbstractVMScheme implements HeapScheme {

    private static final class NoGCHeapMemoryRegion extends RuntimeMemoryRegion {

        /**
         * @param title how the region should identify itself for debugging purposes
         */
        public NoGCHeapMemoryRegion(String title) {
            super(Size.zero(), Size.zero());
            setDescription(title);
        }

        /**
         * @param address sets an inspected field that can be used for debugging.
         */
        void setAllocationMark(Address address) {
            _mark = address;
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

    private final Timer _clearTimer = GlobalMetrics.newTimer("Clear", Clock.SYSTEM_MILLISECONDS);
    private final Timer _gcTimer = GlobalMetrics.newTimer("GC", Clock.SYSTEM_MILLISECONDS);
    private final Timer _rootScanTimer = GlobalMetrics.newTimer("Roots scan", Clock.SYSTEM_MILLISECONDS);
    private final Timer _bootHeapScanTimer = GlobalMetrics.newTimer("Boot heap scan", Clock.SYSTEM_MILLISECONDS);
    private final Timer _codeScanTimer = GlobalMetrics.newTimer("Code scan", Clock.SYSTEM_MILLISECONDS);
    private final Timer _copyTimer = GlobalMetrics.newTimer("Copy", Clock.SYSTEM_MILLISECONDS);

    private int _numberOfGarbageCollectionInvocations = 0;

    private final Runnable _collect = new Runnable() {
        public void run() {
            if (Heap.verbose()) {
                Log.print("-- no GC--   size: ");
                Log.println(_allocationMark.minus(_space.start()).toInt());
            }

            verifyHeap();

            ++_numberOfGarbageCollectionInvocations;
            TeleHeapInfo.beforeGarbageCollection();
            VMConfiguration.hostOrTarget().monitorScheme().beforeGarbageCollection();

            _gcTimer.restart();

            _clearTimer.restart();
            _clearTimer.stop();

            if (Heap.traceGCRootScanning()) {
                Log.println("Scanning roots...");
            }
            _rootScanTimer.restart();
            _rootScanTimer.stop();

            if (Heap.traceGC()) {
                Log.println("Scanning boot heap...");
            }
            _bootHeapScanTimer.restart();
            _bootHeapScanTimer.stop();

            _codeScanTimer.restart();
            _codeScanTimer.stop();

            if (Heap.traceGC()) {
                Log.println("Moving reachable...");
            }

            _copyTimer.restart();
            _copyTimer.stop();
            _gcTimer.stop();

            VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();

            verifyHeap();

            TeleHeapInfo.afterGarbageCollection();

            if (Heap.traceGC()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("clear & initialize: ");
                Log.print(_clearTimer.getMilliSeconds());
                Log.print("   root scan: ");
                Log.print(_rootScanTimer.getMilliSeconds());
                Log.print("   boot heap scan: ");
                Log.print(_bootHeapScanTimer.getMilliSeconds());
                Log.print("   code scan: ");
                Log.print(_codeScanTimer.getMilliSeconds());
                Log.print("   copy: ");
                Log.print(_copyTimer.getMilliSeconds());
                Log.println();
                Log.print("GC <");
                Log.print(_numberOfGarbageCollectionInvocations);
                Log.print("> ");
                Log.print(_gcTimer.getMilliSeconds());
                Log.println(" (ms)");

                Log.print("--After GC--   bytes copied: ");
                Log.print(_allocationMark.minus(_space.start()).toInt());
                Log.println();
                Log.unlock(lockDisabledSafepoints);
            }

            if (Heap.verbose()) {
                Log.println("-- no GC--   done.");
            }
        }
    };

    private StopTheWorldDaemon _collectorThread;

    private NoGCHeapMemoryRegion _space = new NoGCHeapMemoryRegion("Heap-NoGC");
    private Address _top;
    private volatile Address _allocationMark;

    @CONSTANT_WHEN_NOT_ZERO
    private Pointer _allocationMarkPointer;

    @Override
    public void initialize(MaxineVM.Phase phase) {
        if (phase == MaxineVM.Phase.PRISTINE) {
            final Size size = Heap.initialSize();

            _space.setSize(size);
            _space.setStart(Memory.allocate(size));
            _allocationMark = _space.start();
            _top = _space.end();

            _allocationMarkPointer = ClassActor.fromJava(NoGCHeapScheme.class).findLocalInstanceFieldActor("_allocationMark").pointer(this);

            // From now on we can allocate

            // For debugging
            _space.setAllocationMark(_allocationMark);

            TeleHeapInfo.registerMemoryRegions(_space);
        } else if (phase == MaxineVM.Phase.STARTING) {
            _collectorThread = new StopTheWorldDaemon("GC", _collect);
        }
    }

    public NoGCHeapScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
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

    private boolean _outOfMemory;

    @INLINE
    private void executeCollectorThread() {
        if (!Heap.gcDisabled()) {
            _collectorThread.execute();
        }
    }

    public synchronized boolean collectGarbage(Size requestedFreeSpace) {
        if (_outOfMemory) {
            return false;
        }
        executeCollectorThread();
        return immediateFreeSpace().greaterEqual(requestedFreeSpace);
    }

    public Size reportFreeSpace() {
        return immediateFreeSpace();
    }

    private static final OutOfMemoryError _outOfMemoryError = new OutOfMemoryError(); // TODO: create a new one each time

    @NEVER_INLINE
    private Pointer fail() {
        throw _outOfMemoryError;
    }

    @INLINE
    @NO_SAFEPOINTS("TODO")
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
            return fail();
        }
        // For debugging
        _space.setAllocationMark(_allocationMark);
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
        return _space.contains(address);
    }

    public void runFinalization() {
    }

    public long numberOfGarbageCollectionInvocations() {
        return _numberOfGarbageCollectionInvocations;
    }

    public long numberOfGarbageTurnovers() {
        return 0;
    }

    public <Object_Type> boolean flash(Object_Type object, com.sun.max.lang.Procedure<Object_Type> procedure) {
        procedure.run(object);
        return true;
    }

    @INLINE
    public boolean pin(Object object) {
        return true;
    }

    @INLINE
    public void unpin(Object object) {
    }

    @INLINE
    public boolean isPinned(Object object) {
        return true;
    }

    private void verifyGripAtIndex(Address address, int index, Grip grip) {
        if (grip.isZero()) {
            return;
        }
        checkGripTag(grip);
        final Pointer origin = grip.toOrigin();
        if (!(_space.contains(origin) || Heap.bootHeapRegion().contains(origin) || Code.contains(origin))) {
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

    private final SequentialHeapRootsScanner _heapRootsVerifier = new SequentialHeapRootsScanner(this, _pointerIndexGripVerifier);

    private void verifyHeap() {
        if (Heap.traceGC()) {
            Log.println("Verifying heap...");
        }
        _heapRootsVerifier.run();
        Pointer cell = _space.start().asPointer();
        while (cell.lessThan(_allocationMark)) {
            if (VMConfiguration.hostOrTarget().debugging()) {
                cell = cell.plusWords(1);
                checkCellTag(cell);
            }
            final Pointer origin = Layout.cellToOrigin(cell);
            final Hub hub = checkHub(origin);
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

    @INLINE
    @Override
    public void writeBarrier(Reference reference) {
    }

}
