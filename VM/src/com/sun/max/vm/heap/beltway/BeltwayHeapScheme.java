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
package com.sun.max.vm.heap.beltway;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.beltway.profile.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * @author Christos Kotselidis
 */
public abstract class BeltwayHeapScheme extends HeapSchemeAdaptor implements HeapScheme, Allocator {

    protected static Action copyAction;
    protected static Action verifyAction = new VerifyActionImpl();
    protected final BeltWayCellVisitor beltwayCellVisitor = new BeltwayCellVisitorImpl();

    static {
        if (BeltwayConfiguration.parallelScavenging) {
            copyAction = new ParallelCopyActionImpl();
        } else {
            copyAction = new CopyActionImpl();
        }
    }

    private final BeltWayPointerOffsetVisitor pointerOffsetGripVerifier = new PointerOffsetVisitorImpl(verifyAction);
    private final BeltWayPointerIndexVisitor pointerIndexGripVerifier = new PointerIndexVisitorImpl(verifyAction);

    private final BeltWayPointerOffsetVisitor pointerOffsetGripUpdater = new PointerOffsetVisitorImpl(copyAction);
    private final BeltWayPointerIndexVisitor pointerIndexGripUpdater = new PointerIndexVisitorImpl(copyAction);

    public BeltWayCellVisitor beltwayCellVisitor() {
        return beltwayCellVisitor;
    }

    public Action getAction() {
        return copyAction;
    }

    public BeltWayPointerOffsetVisitor pointerOffsetGripVerifier() {
        return pointerOffsetGripVerifier;
    }

    public BeltWayPointerIndexVisitor pointerIndexGripVerifier() {
        return pointerIndexGripVerifier;
    }

    public BeltWayPointerIndexVisitor pointerIndexGripUpdater() {
        return pointerIndexGripUpdater;
    }

    public BeltWayPointerOffsetVisitor pointerOffsetGripUpdater() {
        return pointerOffsetGripUpdater;
    }

    protected final BeltwayCardRegion cardRegion = new BeltwayCardRegion();

    @INLINE
    public final int adjustedCardTableShift() {
        return BeltwayCardRegion.CARD_SHIFT;
    }

    public static final SideTable sideTable = new SideTable();
    protected Address adjustedCardTableAddress = Address.zero();

    public static final BeltwayHeapVerifier heapVerifier = new BeltwayHeapVerifier();

    private static final BeltwaySequentialHeapRootsScanner heapRootsScanner = new BeltwaySequentialHeapRootsScanner();
    public static final OutOfMemoryError outOfMemoryError = new OutOfMemoryError();

    protected BeltwayConfiguration beltwayConfiguration;
    protected BeltManager beltManager;
    protected BeltwayCollector beltCollector;
    protected static BeltwayStopTheWorldDaemon collectorThread;

    public static boolean outOfMemory = false;

    public static BeltwayCollectorThread[] gcThreads = new BeltwayCollectorThread[BeltwayConfiguration.numberOfGCThreads];
    public static int lastThreadAllocated;

    public static volatile long allocatedTLABS = 0;
    public static Object tlabCounterMutex = new Object();
    public static volatile long retrievedTLABS = 0;
    public static Object tlabRetrieveMutex = new Object();
    public static boolean inGC = false;
    public static boolean inScavenging = false;

    public static TLAB[] scavengerTLABs = new TLAB[BeltwayConfiguration.numberOfGCThreads + 1];

    public BeltwayHeapScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    protected abstract int [] defaultBeltHeapPercentage();

    @Override
    public void initialize(MaxineVM.Phase phase) {
        if (MaxineVM.isPrototyping()) {
            beltwayConfiguration = new BeltwayConfiguration();
            beltCollector = new BeltwayCollector();
            beltManager = new BeltManager();
            beltManager.createBelts();

            for (int i = 0; i < BeltwayConfiguration.numberOfGCThreads; i++) {
                JavaMonitorManager.bindStickyMonitor(BeltwayCollectorThread.tokens[i], new StandardJavaMonitor());
            }
            JavaMonitorManager.bindStickyMonitor(BeltwayCollectorThread.callerToken, new StandardJavaMonitor());
        } else if (phase == MaxineVM.Phase.PRISTINE) {
            final Size heapSize = calculateHeapSize();
            final Address address = allocateMemory(heapSize);
            final int [] defaultBeltHeapPercentage = defaultBeltHeapPercentage();
            beltwayConfiguration.initializeBeltWayConfiguration(address, heapSize,  defaultBeltHeapPercentage.length, defaultBeltHeapPercentage);
            beltManager.initializeBelts();
            if (Heap.verbose()) {
                beltManager.printBeltsInfo();
            }
            final Size coveredRegionSize = beltManager.getEnd().minus(Heap.bootHeapRegion.start()).asSize();
            cardRegion.initialize(Heap.bootHeapRegion.start(), coveredRegionSize, Heap.bootHeapRegion.start().plus(coveredRegionSize));
            sideTable.initialize(Heap.bootHeapRegion.start(), coveredRegionSize, Heap.bootHeapRegion.start().plus(coveredRegionSize).plus(cardRegion.cardTableSize()).roundedUpBy(
                            Platform.target().pageSize));
            BeltwayCardRegion.switchToRegularCardTable(cardRegion.cardTableBase().asPointer());
        } else if (phase == MaxineVM.Phase.STARTING) {
            collectorThread = new BeltwayStopTheWorldDaemon("GC", beltCollector);
            collectorThread.start();
        } else if (phase == MaxineVM.Phase.RUNNING) {
            if (BeltwayConfiguration.parallelScavenging) {
                createGCThreads();
            }
        }
    }

    @INLINE
    protected final Address allocateMemory(Size size) {
        final Address endOfCodeRegion = Code.bootCodeRegion.end();
        final Address tlabAlignedEndOfCodeRegion = endOfCodeRegion.roundedUpBy(BeltwayConfiguration.TLAB_SIZE.toInt());
        assert tlabAlignedEndOfCodeRegion.isAligned(Platform.target().pageSize);
        if (VirtualMemory.allocatePageAlignedAtFixedAddress(tlabAlignedEndOfCodeRegion, size, VirtualMemory.Type.HEAP)) {
            return tlabAlignedEndOfCodeRegion;
        }
        FatalError.unexpected("Error! Could not map fix the requested memory size");
        return Address.zero();
    }

    @INLINE
    public final BeltManager getBeltManager() {
        return beltManager;
    }

    @INLINE
    public final BeltwaySequentialHeapRootsScanner getRootScannerVerifier() {
        heapRootsScanner.setBeltwayPointerIndexVisitor(pointerIndexGripVerifier());
        return heapRootsScanner;
    }

    public final BeltwaySequentialHeapRootsScanner getRootScannerUpdater() {
        heapRootsScanner.setBeltwayPointerIndexVisitor(pointerIndexGripUpdater());
        return heapRootsScanner;
    }

    public final BeltwayHeapVerifier getVerifier() {
        return heapVerifier;
    }

    protected Size calculateHeapSize() {
        Size size = Heap.initialSize();
        if (Heap.maxSize().greaterThan(size)) {
            size = Heap.maxSize();
        }
        return size.roundedUpBy(BeltwayHeapSchemeConfiguration.TLAB_SIZE.toInt()).asSize();
    }

    @INLINE
    protected final void createGCThreads() {
        for (int i = 0; i < BeltwayConfiguration.numberOfGCThreads; i++) {
            gcThreads[i] = new BeltwayCollectorThread(i);
        }

    }

    public final void startGCThreads() {
        for (int i = 0; i < gcThreads.length; i++) {
            gcThreads[i].trigger();
        }

    }

    public void initializeGCThreads(BeltwayHeapScheme beltwayHeapScheme, RuntimeMemoryRegion from, RuntimeMemoryRegion to) {
        for (int i = 0; i < gcThreads.length; i++) {
            gcThreads[i].initialize(beltwayHeapScheme, from, to);
        }
    }

    private class BootHeapCellVisitor implements CellVisitor {
        RuntimeMemoryRegion from;
        RuntimeMemoryRegion to;

        public Pointer visitCell(Pointer cell) {
            return beltwayCellVisitor().visitCell(cell, copyAction, from, to);
        }
    }

    private final BootHeapCellVisitor cellVisitor = new BootHeapCellVisitor();

    /**
     * Holds the biased card table address.
     */
    public static final VmThreadLocal ADJUSTED_CARDTABLE_BASE = new VmThreadLocal("ADJUSTED_CARDTABLE_BASE", Kind.WORD, "Beltway: ->biased card table") {
        @Override
        public void initialize(com.sun.max.vm.MaxineVM.Phase phase) {
            final Pointer vmThreadLocals = VmThread.currentVmThreadLocals();
            // enable write barriers by setting the adjusted card table address
            if (phase.equals(MaxineVM.Phase.RUNNING) || phase.equals(MaxineVM.Phase.STARTING)) {
                // use the normal card table
                ADJUSTED_CARDTABLE_BASE.setConstantWord(vmThreadLocals, BeltwayCardRegion.getAdjustedCardTable());
            } else {
                // use the primordial card table
                ADJUSTED_CARDTABLE_BASE.setConstantWord(vmThreadLocals, ADJUSTED_CARDTABLE_BASE.getConstantWord(MaxineVM.primordialVmThreadLocals()));
            }
        }
    };

    public void scanBootHeap(RuntimeMemoryRegion from, RuntimeMemoryRegion to) {
        cellVisitor.from = from;
        cellVisitor.to = to;
        Heap.bootHeapRegion.visitCells(cellVisitor);
    }

    public void printCardTable() {
        final int startCardIndex = cardRegion.getCardIndexFromHeapAddress(Heap.bootHeapRegion.start());
        final int endCardIndex = cardRegion.getCardIndexFromHeapAddress(beltManager.getEnd());
        for (int i = startCardIndex; i < endCardIndex; i++) {
            if (cardRegion.isCardMarked(i)) {
                Log.print("0");
            } else {
                Log.print("1");
            }
            Log.print(" -- ");
            Log.println(cardRegion.getHeapAddressFromCardIndex(i));
        }
    }

    public void testCardAllignment(Pointer address) {
        final int boundaryIndex = cardRegion.getCardIndexFromHeapAddress(address);
        final int prevIndex = cardRegion.getCardIndexFromHeapAddress(address.minusWords(1));
        if (boundaryIndex == prevIndex) {
            Log.println("Error in TLAB allignment");
            Log.println("Erroneous Address");
            Log.print(address);
            Log.println("boundaryIndex");
            Log.println(boundaryIndex);
            Log.println("prevIndex");
            Log.println(prevIndex);
            FatalError.unexpected("ERROR in CARD ALLIGNMENT");

        }
    }

    public final void scanCards(RuntimeMemoryRegion origin, RuntimeMemoryRegion from, RuntimeMemoryRegion to) {
        final int startCardIndex = cardRegion.getCardIndexFromHeapAddress(origin.start());
        final int endCardIndex = cardRegion.getCardIndexFromHeapAddress(origin.end());
        for (int i = startCardIndex; i < endCardIndex; i++) {
            if (cardRegion.isCardMarked(i)) {
                final Address heapStartAddress = cardRegion.getHeapAddressFromCardIndex(i);
                final Pointer gcTLABStart = getGCTLABStartFromAddress(heapStartAddress);
                if (!gcTLABStart.isZero()) {
                    final Pointer gcTLABEnd = getGCTLABEndFromStart(gcTLABStart);
                    BeltwayCellVisitorImpl.linearVisitAllCellsTLAB(beltwayCellVisitor(), copyAction, gcTLABStart, gcTLABEnd, from, to);
                    SideTable.markScavengeSideTable(gcTLABStart);
                }
            }

        }
    }

    public void testCards(RuntimeMemoryRegion origin, RuntimeMemoryRegion from, RuntimeMemoryRegion to) {
        final int startCardIndex = cardRegion.getCardIndexFromHeapAddress(origin.start());
        final int endCardIndex = cardRegion.getCardIndexFromHeapAddress(origin.end());

        for (int i = startCardIndex; i < endCardIndex; i++) {
            if (cardRegion.isCardMarked(i)) {
                Log.print("Card: ");
                Log.print(i);
                Log.println("  is Dirty ");

                final Address heapStartAddress = cardRegion.getHeapAddressFromCardIndex(i);

                Log.print("Correspoding heap Address: ");
                Log.println(heapStartAddress);
            }
        }
    }

    public final void linearScanRegion(RuntimeMemoryRegion origin, RuntimeMemoryRegion from, RuntimeMemoryRegion to) {
        BeltwayCellVisitorImpl.linearVisitAllCells(beltwayCellVisitor(), copyAction, origin, from, to);
    }

    public final void linearScanRegionBelt(Belt origin, RuntimeMemoryRegion from, RuntimeMemoryRegion to) {
        BeltwayCellVisitorImpl.linearVisitAllCellsBelt(beltwayCellVisitor(), copyAction, origin, from, to);
    }

    public void scanCode(RuntimeMemoryRegion from, RuntimeMemoryRegion to) {
        cellVisitor.from = from;
        cellVisitor.to = to;
        Code.visitCells(cellVisitor, true);
    }

    @INLINE
    public final Pointer gcBumpAllocate(RuntimeMemoryRegion belt, Size size) {
        return beltManager.gcBumpAllocate((Belt) belt, size);
    }

    @INLINE
    public final Pointer gcSynchAllocate(RuntimeMemoryRegion belt, Size size) {
        return beltManager.gcAllocate((Belt) belt, size);
    }

    @INLINE
    public final Pointer gcAllocate(RuntimeMemoryRegion belt, Size size) {
        if (BeltwayConfiguration.useGCTlabs) {
            return gcTlabAllocate(belt, size);
        }
        return gcBumpAllocate(belt, size);

    }

    public boolean isGcThread(Thread thread) {
        return thread instanceof BeltwayStopTheWorldDaemon;
    }

    /**
     * Allocation from heap (SlowPath). This method delegates the allocation to the belt denoted by the Belt Manager.
     * Currently we are synchronizing to avoid race conditions. TODO: Recalculate tlabs' sizes
     *
     * @param size The size of the allocation.
     * @return the pointer to the address in which we can allocate. If null, a GC should be triggered.
     */
    private Pointer allocateSlowPath(Belt belt, Size size) {
        return beltManager.allocate(belt, size);
    }

    protected Pointer bumpAllocateSlowPath(Belt belt, Size size) {
        final Pointer pointer = beltManager.bumpAllocate(belt, size);
        if (pointer.isZero()) {
            throw outOfMemoryError;
        }
        return pointer;
    }

    @INLINE
    @NO_SAFEPOINTS("TODO")
    public final Pointer heapAllocate(Belt belt, Size size) {
        final Pointer pointer = allocateSlowPath(belt, size);
        if (pointer.equals(Pointer.zero())) {
            if (belt.getIndex() == (BeltwayConfiguration.getNumberOfBelts() - 1)) {
                throw outOfMemoryError;
            }
            if (!Heap.collectGarbage(size)) {
                throw outOfMemoryError;
            }
            return allocateSlowPath(belt, size);
        }
        return pointer;
    }

    @INLINE
    @NO_SAFEPOINTS("TODO")
    public final Pointer tlabAllocate(Belt belt, Size size) {
        final VmThread thread = VmThread.current();
        final TLAB tlab = thread.getTLAB();

        if (tlab.isSet()) {
            final Pointer pointer = tlab.allocate(size);
            if (pointer.equals(Pointer.zero())) { // TLAB is full
                //Debug.println("TLAB is full, try to allocate a new TLAB");

                final Size newSize = calculateTLABSize(size);
                final Size allocSize = newSize.asPointer().minusWords(1).asSize();
                final Pointer newTLABAddress = allocateTLAB(belt, allocSize);

                //Debug.print("New Tlab Address: ");
                //Debug.println(newTLABAddress);

                if (newTLABAddress.isZero()) { // TLAB allocation failed, nursery is full, Trigger GC
                    //Debug.println("Nursery is full, trigger GC");
                    if (!Heap.collectGarbage(size)) {
                        throw outOfMemoryError;

                    }
                    initializeFirstTLAB(belt, tlab, size);
                    return tlab.allocate(size);
                }
                // new TLAB has been successfully allocated, Rest thread's TLAB to the new one and do
                initializeTLAB(tlab, newTLABAddress, newSize);
                return tlab.allocate(size);
            }

            return pointer;
        }
        // Allocate first TLAB
        initializeFirstTLAB(belt, tlab, size);
        return tlab.allocate(size);

    }

    public Pointer gcTlabAllocate(RuntimeMemoryRegion gcRegion, Size size) {
        final VmThread thread = VmThread.current();
        final TLAB tlab = thread.getTLAB();
        lastThreadAllocated = thread.id();
        if (tlab.isSet()) { // If the TLABS has been set
            final Pointer pointer = tlab.allocate(size);

            // If the return address is zero, it means that the TLAB is full
            if (pointer.equals(Pointer.zero())) {
                final Size newSize = calculateTLABSize(size);
                final Size allocSize = newSize.asPointer().minusWords(1).asSize();
                final Pointer newTLABAddress = gcAllocateTLAB(gcRegion, allocSize);
                sideTable.markCreatingSideTable(newTLABAddress);
                if (!SideTable.isScavenged(tlab.start())) {
                    SideTable.markStartSideTable(tlab.start());
                }
                if (newTLABAddress.isZero()) { // TLAB allocation failed, nursery is full, Trigger GC
                    if (!Heap.collectGarbage(size) || BeltwayHeapScheme.outOfMemory) {
                        throw outOfMemoryError;

                    }
                    initializeFirstGCTLAB(gcRegion, tlab, size);
                    return tlab.allocate(size);
                }

                //_allocatedTLABS++;
                initializeTLAB(tlab, newTLABAddress, newSize);
                return tlab.allocate(size);
            }

            // Successful allocation in the existing TLAB, return the address
            return pointer;
        }
        // Allocate first GC TLAB
        initializeFirstGCTLAB(gcRegion, tlab, size);
        // Allocate in the first tlab
        return tlab.allocate(size);

    }

    public final Size calculateTLABSize(Size size) {
        Size defaultSize;
        defaultSize = BeltwayConfiguration.TLAB_SIZE;
        if (inGC) {
            defaultSize = BeltwayConfiguration.GC_TLAB_SIZE;
        }
        Size newSize = defaultSize;
        while (size.greaterThan(newSize.minus(100))) {
            newSize = newSize.plus(200);
            newSize = newSize.roundedUpBy(defaultSize).asSize();
        }
        return newSize;
    }

    public final void initializeFirstTLAB(Belt belt, TLAB tlab, Size size) {
        //Debug.println("Try to set initial Tlabs");
        final Size newSize = calculateTLABSize(size);
        final Size allocSize = newSize.asPointer().minusWords(1).asSize();
        final Pointer newTLABAddress = allocateTLAB(belt, allocSize);

        if (newTLABAddress.isZero()) {
            FatalError.unexpected("Nursery is full, trigger GC in the First Allocation(?) Smth is wrong....");
        } else {
            initializeTLAB(tlab, newTLABAddress, newSize);
        }
    }

    public final void initializeFirstGCTLAB(RuntimeMemoryRegion gcRegion, TLAB tlab, Size size) {
        //Debug.println("Try to set initial GC TLAB");
        final Size newSize = calculateTLABSize(size);
        final Size allocSize = newSize.asPointer().minusWords(1).asSize();
        final Pointer newTLABAddress = gcAllocateTLAB(gcRegion, allocSize);
        sideTable.markCreatingSideTable(newTLABAddress);
        if (newTLABAddress.isZero()) {
            FatalError.unexpected("Nursery is full, trigger GC in the First Allocation(?) Smth is wrong!");
        } else {
            initializeTLAB(tlab, newTLABAddress, newSize);

        }
    }

    @INLINE
    public final void initializeTLAB(TLAB tlab, Pointer newTLABAddress, Size size) {
        tlab.initializeTLAB(newTLABAddress.asAddress(), newTLABAddress.asAddress(), size);
    }

    @INLINE
    public final Pointer allocateTLAB(Belt belt, Size size) {
        Pointer pointer = heapAllocate(belt, size);
        if (pointer != Pointer.zero()) {
            if (MaxineVM.isDebug()) {
                // Subtract one word as it will be overwritten by the debug word of the TLAB descriptor
                pointer = pointer.minusWords(1);
            }
            if (Heap.verbose()) {
                HeapStatistics.incrementMutatorTlabAllocations();
            }
        }
        return pointer;
    }

    @INLINE
    public final Pointer gcAllocateTLAB(RuntimeMemoryRegion gcRegion, Size size) {
        Pointer pointer = gcSynchAllocate(gcRegion, size);
        if (!pointer.isZero()) {
            if (MaxineVM.isDebug()) {
                // Subtract one word as it will be overwritten by the debug word of the TLAB descriptor
                pointer = pointer.minusWords(1);
            }
        } else {
            throw BeltwayHeapScheme.outOfMemoryError;
        }
        return pointer;
    }

    @INLINE
    public final Pointer allocate(RuntimeMemoryRegion to, Size size) {
        return null;
    }

    /**
     * Allocate a new array object and fill in its header and initial data.
     */
    @INLINE
    @NO_SAFEPOINTS("TODO")
    public final Object createArray(DynamicHub dynamicHub, int length) {
        final Size size = Layout.getArraySize(dynamicHub.classActor.componentClassActor().kind, length);
        final Pointer cell = allocate(size);
        return Cell.plantArray(cell, size, dynamicHub, length);
    }

    /**
     * Allocate a new tuple and fill in its header and initial data. Obtain the cell size from the given tuple class
     * actor.
     */
    @INLINE
    @NO_SAFEPOINTS("TODO")
    public final Object createTuple(Hub hub) {
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
        return false;
    }

    public Size reportFreeSpace() {
        return beltManager.reportFreeSpace();
    }

    public Size reportUsedSpace() {
        FatalError.unimplemented();
        return null;
    }

    public void runFinalization() {

    }

    public boolean pin(Object object) {
        return false;
    }

    public void unpin(Object object) {

    }

    public boolean isPinned(Object object) {
        return false;
    }

    public void initializeAuxiliarySpace(Pointer primordialVmThreadLocals, Pointer auxiliarySpace) {
        ADJUSTED_CARDTABLE_BASE.setConstantWord(primordialVmThreadLocals, BeltwayCardRegion.adjustedCardTableBase(auxiliarySpace));
    }

    private Size cardTableSize(Size coveredRegionSize) {
        return coveredRegionSize.unsignedShiftedRight(BeltwayCardRegion.CARD_SHIFT);
    }

    public int auxiliarySpaceSize(int bootImageSize) {
        return cardTableSize(Size.fromInt(bootImageSize)).toInt();
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // TLAB code////
    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void fillLastTLAB() {
        final TLAB tlab = VmThreadMap.ACTIVE.getVmThreadForID(lastThreadAllocated).getTLAB();
        tlab.fillTLAB();
    }

    public void markSideTableLastTLAB() {
        final TLAB tlab = VmThreadMap.ACTIVE.getVmThreadForID(lastThreadAllocated).getTLAB();
        SideTable.markStartSideTable(tlab.start());
    }

    public void resetCurrentTLAB() {
        final BeltwayCollectorThread thread = (BeltwayCollectorThread) Thread.currentThread();
        final TLAB tlab = thread.getScavengeTLAB();
        tlab.unSet();
    }

    private static class StopTheWorldTLABReset implements Procedure<VmThread> {
        public void run(VmThread thread) {
            thread.getTLAB().unSet();
        }
    }

    private static final StopTheWorldTLABReset stopTheWorldReset = new StopTheWorldTLABReset();

    public Pointer getGCTLABStartFromAddress(Address address) {
        Address tlabAddress = address;
        if (!address.isAligned(BeltwayHeapSchemeConfiguration.GC_TLAB_SIZE.toInt())) {
            //Debug.println("Is not alligned");
            tlabAddress = address.roundedDownBy(BeltwayHeapSchemeConfiguration.GC_TLAB_SIZE.toInt());
        }
        while (!SideTable.isStart(SideTable.getChunkIndexFromHeapAddress(tlabAddress))) {
            if (SideTable.isScavenged(SideTable.getChunkIndexFromHeapAddress(tlabAddress))) {
                return Pointer.zero();
            }
            tlabAddress = tlabAddress.asPointer().minusWords(1);
            tlabAddress = tlabAddress.roundedDownBy(BeltwayHeapSchemeConfiguration.GC_TLAB_SIZE.toInt());
        }
        return tlabAddress.asPointer();
    }

    public Pointer getGCTLABEndFromStart(Address address) {
        int index = SideTable.getChunkIndexFromHeapAddress(address) + 1;
        while (sideTable.isMiddle(index)) {
            index++;
        }
        return sideTable.getHeapAddressFromChunkIndex(index).asPointer();
    }

    public Pointer getNextAvailableGCTask(int searchIndex, int stopSearchIndex) {
        int startSearchIndex = searchIndex;
        while (startSearchIndex < stopSearchIndex) {
            if (SideTable.isStart(startSearchIndex)) {
                if (sideTable.compareAndSwapStart(startSearchIndex) == SideTable.START) {
                    return sideTable.getHeapAddressFromChunkIndex(startSearchIndex).asPointer();
                }
            }
            startSearchIndex++;
        }
        if (SideTable.isStart(startSearchIndex)) {
            if (sideTable.compareAndSwapStart(startSearchIndex) == SideTable.START) {
                return sideTable.getHeapAddressFromChunkIndex(startSearchIndex).asPointer();
            }
        }
        return Pointer.zero();
    }

    private void createScavengerTLABs() {
        for (int i = 0; i < BeltwayConfiguration.numberOfGCThreads + 1; i++) {
            scavengerTLABs[i] = new TLAB();
        }
    }
}
