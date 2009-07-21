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
import com.sun.max.atomic.*;
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
 * A heap that lets you allocate, but never reclaims any memory, i.e. never performs a garbage collection.
 * With this setup, you can call System.gc() to test the safepoint mechanism
 * without potential object reference corruption by a presumably broken GC.
 *
 * @author Bernd Mathiske
 */
public final class NoGCHeapScheme extends HeapSchemeAdaptor implements HeapScheme {

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
            mark = address;
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

    public void initializeVmThread(Pointer vmThreadLocals) {
    }

    private final Timer clearTimer = GlobalMetrics.newTimer("Clear", Clock.SYSTEM_MILLISECONDS);
    private final Timer gcTimer = GlobalMetrics.newTimer("GC", Clock.SYSTEM_MILLISECONDS);
    private final Timer rootScanTimer = GlobalMetrics.newTimer("Roots scan", Clock.SYSTEM_MILLISECONDS);
    private final Timer bootHeapScanTimer = GlobalMetrics.newTimer("Boot heap scan", Clock.SYSTEM_MILLISECONDS);
    private final Timer codeScanTimer = GlobalMetrics.newTimer("Code scan", Clock.SYSTEM_MILLISECONDS);
    private final Timer copyTimer = GlobalMetrics.newTimer("Copy", Clock.SYSTEM_MILLISECONDS);

    private int numberOfGarbageCollectionInvocations = 0;

    private final Runnable collect = new Runnable() {
        public void run() {
            if (Heap.verbose()) {
                Log.print("-- no GC--   size: ");
                Log.println(allocationMark().minus(space.start()).toInt());
            }

            verifyHeap();

            ++numberOfGarbageCollectionInvocations;
            InspectableHeapInfo.beforeGarbageCollection();
            VMConfiguration.hostOrTarget().monitorScheme().beforeGarbageCollection();

            gcTimer.start();

            clearTimer.start();
            clearTimer.stop();

            if (Heap.traceRootScanning()) {
                Log.println("Scanning roots...");
            }
            rootScanTimer.start();
            rootScanTimer.stop();

            if (Heap.traceGC()) {
                Log.println("Scanning boot heap...");
            }
            bootHeapScanTimer.start();
            bootHeapScanTimer.stop();

            codeScanTimer.start();
            codeScanTimer.stop();

            if (Heap.traceGC()) {
                Log.println("Moving reachable...");
            }

            copyTimer.start();
            copyTimer.stop();
            gcTimer.stop();

            VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();

            verifyHeap();

            InspectableHeapInfo.afterGarbageCollection();

            if (Heap.traceGC()) {
                final boolean lockDisabledSafepoints = Log.lock();
                Log.print("clear & initialize: ");
                Log.print(TimerUtil.getLastElapsedMilliSeconds(clearTimer));
                Log.print("   root scan: ");
                Log.print(TimerUtil.getLastElapsedMilliSeconds(rootScanTimer));
                Log.print("   boot heap scan: ");
                Log.print(TimerUtil.getLastElapsedMilliSeconds(bootHeapScanTimer));
                Log.print("   code scan: ");
                Log.print(TimerUtil.getLastElapsedMilliSeconds(codeScanTimer));
                Log.print("   copy: ");
                Log.print(TimerUtil.getLastElapsedMilliSeconds(copyTimer));
                Log.println();
                Log.print("GC <");
                Log.print(numberOfGarbageCollectionInvocations);
                Log.print("> ");
                Log.print(TimerUtil.getLastElapsedMilliSeconds(gcTimer));
                Log.println(" (ms)");

                Log.print("--After GC--   bytes copied: ");
                Log.print(allocationMark().minus(space.start()).toInt());
                Log.println();
                Log.unlock(lockDisabledSafepoints);
            }

            if (Heap.verbose()) {
                Log.println("-- no GC--   done.");
            }
        }
    };

    private StopTheWorldGCDaemon collectorThread;

    private NoGCHeapMemoryRegion space = new NoGCHeapMemoryRegion("Heap-NoGC");
    private Address top;
    private AtomicWord allocationMark;

    @INLINE
    private Address allocationMark() {
        return allocationMark.get().asAddress();
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        if (MaxineVM.isPrototyping()) {
            allocationMark = new AtomicWord();
        } else if (phase == MaxineVM.Phase.PRISTINE) {
            final Size size = Heap.initialSize();

            space.setSize(size);
            space.setStart(Memory.allocate(size));
            allocationMark.set(space.start());
            top = space.end();

            // From now on we can allocate

            // For debugging
            space.setAllocationMark(allocationMark());

            InspectableHeapInfo.registerMemoryRegions(space);
        } else if (phase == MaxineVM.Phase.STARTING) {
            collectorThread = new StopTheWorldGCDaemon("GC", collect);
            collectorThread.start();
        }
    }

    public NoGCHeapScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    private Size immediateFreeSpace() {
        return top.minus(allocationMark()).asSize();
    }

    private void checkCellTag(Pointer cell) {
        if (MaxineVM.isDebug()) {
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
        if (MaxineVM.isDebug()) {
            if (!grip.isZero()) {
                checkCellTag(Layout.originToCell(grip.toOrigin()));
            }
        }
    }

    private boolean outOfMemory;

    @INLINE
    private void executeCollectorThread() {
        if (!Heap.gcDisabled()) {
            collectorThread.execute();
        }
    }

    public synchronized boolean collectGarbage(Size requestedFreeSpace) {
        if (outOfMemory) {
            return false;
        }
        executeCollectorThread();
        return immediateFreeSpace().greaterEqual(requestedFreeSpace);
    }

    public Size reportFreeSpace() {
        return immediateFreeSpace();
    }

    public Size reportUsedSpace() {
        return allocationMark().minus(space.start()).asSize();
    }

    private static final OutOfMemoryError outOfMemoryError = new OutOfMemoryError(); // TODO: create a new one each time

    @NEVER_INLINE
    private Pointer fail() {
        throw outOfMemoryError;
    }

    @INLINE
    @NO_SAFEPOINTS("TODO")
    public Pointer allocate(Size size) {
        Pointer cell;
        final Pointer oldAllocationMark = allocationMark().asPointer();
        if (MaxineVM.isDebug()) {
            cell = oldAllocationMark.plusWords(1);
        } else {
            cell = oldAllocationMark;
        }
        final Pointer end = cell.plus(size);
        if (end.greaterThan(top) || allocationMark.compareAndSwap(oldAllocationMark, end) != oldAllocationMark) {
            return fail();
        }
        // For debugging
        space.setAllocationMark(end);
        return cell;
    }

    @INLINE
    @NO_SAFEPOINTS("TODO")
    public Object createArray(DynamicHub dynamicHub, int length) {
        final Size size = Layout.getArraySize(dynamicHub.classActor.componentClassActor().kind, length);
        final Pointer cell = allocate(size);
        return Cell.plantArray(cell, size, dynamicHub, length);
    }

    @INLINE
    @NO_SAFEPOINTS("TODO")
    public Object createTuple(Hub hub) {
        final Pointer cell = allocate(hub.tupleSize);
        return Cell.plantTuple(cell, hub);
    }

    @NO_SAFEPOINTS("TODO")
    public Object createHybrid(DynamicHub hub) {
        final Size size = hub.tupleSize;
        final Pointer cell = allocate(size);
        return Cell.plantHybrid(cell, size, hub);
    }

    @NO_SAFEPOINTS("TODO")
    public Hybrid expandHybrid(Hybrid hybrid, int length) {
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
        return space.contains(address);
    }

    public void runFinalization() {
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
        if (!(space.contains(origin) || Heap.bootHeapRegion.contains(origin) || Code.contains(origin))) {
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
            final ClassActor classActor = hub.classActor;
            checkClassActor(h.classActor);
            FatalError.check(classActor.staticHub() == h, "lost static hub");
            h = ObjectAccess.readHub(h);
        }

        for (int i = 0; i < 2; i++) {
            h = ObjectAccess.readHub(h);
        }
        FatalError.check(ObjectAccess.readHub(h) == h, "lost hub hub");
        return hub;
    }

    private final PointerIndexVisitor pointerIndexGripVerifier = new PointerIndexVisitor() {
        @Override
        public void visitPointerIndex(Pointer pointer, int wordIndex) {
            verifyGripAtIndex(pointer, wordIndex * Kind.REFERENCE.width.numberOfBytes, pointer.getGrip(wordIndex));
        }
    };

    private final PointerOffsetVisitor pointerOffsetGripVerifier = new PointerOffsetVisitor() {
        public void visitPointerOffset(Pointer pointer, int offset) {
            verifyGripAtIndex(pointer, offset, pointer.readGrip(offset));
        }
    };

    private final SequentialHeapRootsScanner heapRootsVerifier = new SequentialHeapRootsScanner(pointerIndexGripVerifier);

    private void verifyHeap() {
        if (Heap.traceGC()) {
            Log.println("Verifying heap...");
        }
        heapRootsVerifier.run();
        Pointer cell = space.start().asPointer();
        final Address end = allocationMark();
        while (cell.lessThan(end)) {
            if (MaxineVM.isDebug()) {
                cell = cell.plusWords(1);
                checkCellTag(cell);
            }
            final Pointer origin = Layout.cellToOrigin(cell);
            final Hub hub = checkHub(origin);
            final SpecificLayout specificLayout = hub.specificLayout;
            if (specificLayout.isTupleLayout()) {
                TupleReferenceMap.visitOriginOffsets(hub, origin, pointerOffsetGripVerifier);
                cell = cell.plus(hub.tupleSize);
            } else {
                if (specificLayout.isHybridLayout()) {
                    TupleReferenceMap.visitOriginOffsets(hub, origin, pointerOffsetGripVerifier);
                } else if (specificLayout.isReferenceArrayLayout()) {
                    final int length = Layout.readArrayLength(origin);
                    for (int index = 0; index < length; index++) {
                        verifyGripAtIndex(origin, index * Kind.REFERENCE.width.numberOfBytes, Layout.getGrip(origin, index));
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
    public void writeBarrier(Reference from, Reference to) {
        // do nothing.
    }
}
