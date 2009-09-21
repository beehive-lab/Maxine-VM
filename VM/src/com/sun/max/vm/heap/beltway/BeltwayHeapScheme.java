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

import static com.sun.max.vm.VMOptions.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.sync.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.tele.*;
import com.sun.max.vm.thread.*;

/**
 * A heap scheme for beltway collectors.
 * This scheme loosely follows the beltway infrastructure for building copying collectors.
 * The main difference with what is described in the original paper is that a belt is made of a single increment (instead of potentially many).
 *
 * The scheme gathers a number of statically allocated objects so as to avoid dynamic allocation during GC
 * (although it is possible for the GC to allocate objects directly in the evacuation belts as beltway collectors are primarily copying GC).
 *
 * @author Christos Kotselidis
 * @author Laurent Daynes
 */
public abstract class BeltwayHeapScheme extends HeapSchemeWithTLAB {
    /**
     * Alignment requirement for belts. Must be aligned on a card for now.
     * Since the heap is made of belts, it must also enforce belt alignment.
     */
    public static final int BELT_ALIGNMENT = BeltwayCardRegion.CARD_SIZE.toInt();

    /**
     * Size to reserve at the end of a TLABs to guarantee that a dead object can always be
     * appended to a TLAB to fill unused space before a TLAB refill.
     * The headroom is used to compute a soft limit that'll be used as the tlab's top.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private static Size TLAB_HEADROOM;

    /**
     *  Cell visitor for evacuating object from a belt to another belt in a single-threaded GC.
     * The source and destination belt should be properly initialized before using the cell visitor.
     */
    private static final BeltwayCellVisitorImpl singleThreadedCellVisitor = new BeltwayCellVisitorImpl(new GripUpdaterPointerVisitor(new CopyActionImpl()));

    /**
     * Cell visitor for evacuating object from a belt to another belt in a parallel GC (i.e., with potentially multiple thread evacuating objects).
     * Although the visitor can be used by multiple thread simultaneously, all the threads must operate on the same source and destination belts.
     */
    private static final BeltwayCellVisitorImpl parallelCellVisitor = new BeltwayCellVisitorImpl(new GripUpdaterPointerVisitor(new ParallelCopyActionImpl()));

    /**
     * A VM option for specifying if GC should used multiple-threads to scavenge Belts.
     */
    private static final VMBooleanXXOption useParallelGCOption = register(new VMBooleanXXOption("-XX:-UseParallelGC", "Use parallel GC."), MaxineVM.Phase.PRISTINE);
    private static final VMBooleanXXOption verifyBeforeGCOption = register(new VMBooleanXXOption("-XX:-VerifyBeforeGC", "Verify Heap before GC."), MaxineVM.Phase.PRISTINE);
    private static final VMBooleanXXOption verifyAfterGCOption = register(new VMBooleanXXOption("-XX:-VerifyAfterGC", "Verify Heap after GC."), MaxineVM.Phase.PRISTINE);

    /**
     * The cell visitor used by the heap scheme. It's either one of singleThreadedCellVisitor parallelCellVisitor depending on
     * the Beltway configuration. It should be possible to pick up the right configuration based on a VM option.
     */
    @CONSTANT_WHEN_NOT_ZERO
    protected BeltwayCellVisitorImpl cellVisitor;

    public final BeltwayHeapVerifier heapVerifier = new BeltwayHeapVerifier();

    protected final BeltwayCardRegion cardRegion = new BeltwayCardRegion();

    private ResetTLAB resetTLAB = new ResetTLAB() {
        @Override
        protected void doBeforeReset(Pointer enabledVmThreadLocals, Pointer tlabMark, Pointer tlabTop) {
            doBeforeTLABRefill(tlabMark, tlabTop);
        }
    };

    private final SequentialHeapRootsScanner stackAndMonitorGripUpdater = new SequentialHeapRootsScanner(null);

    /**
     * The address of the first byte of the dynamic heap. This is set at VM startup time, once the heap size is known.
     */
    private Address dynamicHeapStart;
    /**
     * Size of the dynamic heap (i.e., excluding boot heap and code regions). This is set at VM startup time, once the heap
     * size is computed based on heap sizing arguments provided to the VM.
     */
    private Size dynamicHeapMaxSize;

    /**
     * Side table for the heap. Keeps track of the address to the first object in a card to enable
     * walking an arbitrarily chosen card.
     */
    public final SideTable sideTable = new SideTable();

    protected Address adjustedCardTableAddress = Address.zero();

    protected BeltManager beltManager;

    protected boolean verifyBeforeGC = false;
    protected boolean verifyAfterGC = false;

    /**
     * The thread running the collector, and coordinating the GC threads when the GC support parallel collection.
     */
    protected BlockingServerDaemon collectorThread;

    public static final OutOfMemoryError outOfMemoryError = new OutOfMemoryError();
    public static boolean outOfMemory = false;

    // Support for parallel collections. FIXME: this needs to be completely revisited.

    public static final int numberOfGCThreads = 0;

    public static boolean useGCTlabs = false;
    public static boolean parallelScavenging = false;

    public static BeltwayCollectorThread[] gcThreads = new BeltwayCollectorThread[numberOfGCThreads];
    public static int lastThreadAllocated;
    public static volatile long allocatedTLABS = 0;
    public static Object tlabCounterMutex = new Object();
    public static volatile long retrievedTLABS = 0;
    public static Object tlabRetrieveMutex = new Object();
    public static boolean inGC = false;
    public static boolean inScavenging = false;
    public static BeltTLAB[] scavengerTLABs = new BeltTLAB[numberOfGCThreads + 1];


    public BeltCellVisitor cellVisitor() {
        return cellVisitor;
    }

    @INLINE
    public final int adjustedCardTableShift() {
        return BeltwayCardRegion.CARD_SHIFT;
    }

    public BeltwayHeapScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    protected abstract int [] beltHeapPercentage();
    protected abstract String [] beltDescriptions();
    protected abstract HeapBoundChecker heapBoundChecker();
    protected abstract void initializeTlabAllocationBelt();

    public Size getMaxHeapSize() {
        return dynamicHeapMaxSize;
    }

    public Address getHeapStart() {
        return dynamicHeapStart;
    }

    public Address getHeapEnd() {
        return dynamicHeapStart.plus(dynamicHeapMaxSize);
    }

    @INLINE
    public final boolean verifyBeforeGC() {
        return verifyBeforeGC;
    }

    @INLINE
    public final boolean verifyAfterGC() {
        return verifyAfterGC;
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (MaxineVM.isPrototyping()) {
            TLAB_HEADROOM = MIN_OBJECT_SIZE.plus(MaxineVM.isDebug() ? Word.size() : 0);
            beltManager = new BeltManager(beltDescriptions());

            // Parallel GC support. FIXME: Should this be here at all ?
            // the number of GC threads to use should be a VM startup decision, not  a prototyping one.
            for (int i = 0; i < numberOfGCThreads; i++) {
                JavaMonitorManager.bindStickyMonitor(BeltwayCollectorThread.tokens[i], new StandardJavaMonitor());
            }
            JavaMonitorManager.bindStickyMonitor(BeltwayCollectorThread.callerToken, new StandardJavaMonitor());
        } else if (phase == MaxineVM.Phase.PRISTINE) {
            parallelScavenging = useParallelGCOption.getValue();
            verifyBeforeGC = verifyBeforeGCOption.getValue();
            verifyAfterGC = verifyAfterGCOption.getValue();
            if (MaxineVM.isDebug()) {
                // For now, always override
                verifyBeforeGC = true;
                verifyAfterGC = true;
            }
            cellVisitor = parallelScavenging ? parallelCellVisitor : singleThreadedCellVisitor;
            ((CopyActionImpl) cellVisitor.pointerVisitorGripUpdater.action).initialize(this);
            stackAndMonitorGripUpdater.setPointerIndexVisitor(cellVisitor.pointerVisitorGripUpdater);
            dynamicHeapMaxSize = calculateHeapSize();
            dynamicHeapStart = allocateHeapStorage(dynamicHeapMaxSize);
            beltManager.initializeBelts(this);
            initializeTlabAllocationBelt(); // enable tlab allocation -- needed for InspectableHeapInfo.

            InspectableHeapInfo.init(beltManager.belts());

            if (Heap.verbose()) {
                beltManager.printBeltsInfo();
            }

            // FIXME: the following should be conditional to using a card table. Factor out in a method used by subclasses ?
            final Size coveredRegionSize = beltManager.getEnd().minus(Heap.bootHeapRegion.start()).asSize();
            cardRegion.initialize(Heap.bootHeapRegion.start(), coveredRegionSize, Heap.bootHeapRegion.start().plus(coveredRegionSize));
            sideTable.initialize(Heap.bootHeapRegion.start(), coveredRegionSize, Heap.bootHeapRegion.start().plus(coveredRegionSize).plus(cardRegion.cardTableSize()).roundedUpBy(
                Platform.target().pageSize));
            BeltwayCardRegion.switchToRegularCardTable(cardRegion.cardTableBase().asPointer());
        } else if (phase == MaxineVM.Phase.STARTING) {
            collectorThread =  parallelScavenging ? new BeltwayStopTheWorldDaemon("GC") : new StopTheWorldGCDaemon("GC");
            collectorThread.start();
        } else if (phase == MaxineVM.Phase.RUNNING) {
            if (parallelScavenging) {
                createGCThreads();
            }
        }
    }

    /**
     * Allocate the backing storage for the entire heap.
     * For now, the beltway heap schemes require that the heap be contiguous with the boot heap region so as to
     * simply implement card-tables (the write barrier currently uses a card table that covers both the boot and the heap).
     * This gets complicated because other parts  of the VM may require that too. At the moment, the CodeManager, if a FixedAddressCodeManager,
     * requires storage immediately after the code region. We need to take this into account when allocating storage for the heap.
     *
     * @param size
     * @return
     */
    private Address allocateHeapStorage(Size size) {
        Address endOfCodeRegion = Code.bootCodeRegion.end().roundedUpBy(Platform.target().pageSize);
        CodeManager codeManager = Code.getCodeManager();
        if (codeManager instanceof FixedAddressCodeManager && codeManager.getRuntimeCodeRegion().start().equals(endOfCodeRegion)) {
            endOfCodeRegion = codeManager.getRuntimeCodeRegion().end();
        }
        final Address tlabAlignedEndOfCodeRegion = endOfCodeRegion.roundedUpBy(BeltwayHeapSchemeConfiguration.TLAB_SIZE.toInt());
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

    /**
     * Single-threaded scan the heap roots, looking for references to objects of the "from" belt and evacuating them to the "to" belt.
     *
     * @param from the belt whose objects are being evacuated
     * @param to the belt where the objects are evacuated to
     */
    public void scavengeRoot(Belt from, Belt to) {
        cellVisitor.init(from, to);

        if (Heap.verbose()) {
            Log.println("Scan Roots ");
        }
        stackAndMonitorGripUpdater.run();

        if (Heap.verbose()) {
            Log.println("Scan Boot Heap");
        }
        Heap.bootHeapRegion.visitReferences(cellVisitor.pointerVisitorGripUpdater);
        if (Heap.verbose()) {
            Log.println("Scan Code");
        }
        Code.visitCells(cellVisitor, false);

        if (Heap.verbose()) {
            Log.println("Scan Immortal");
        }
        ImmortalHeap.visitCells(cellVisitor);
    }

    protected Size calculateHeapSize() {
        Size size = Heap.initialSize();
        if (Heap.maxSize().greaterThan(size)) {
            size = Heap.maxSize();
        }
        return size.roundedUpBy(BELT_ALIGNMENT).asSize();
    }

    protected final void createGCThreads() {
        for (int i = 0; i < numberOfGCThreads; i++) {
            gcThreads[i] = new BeltwayCollectorThread(i);
        }
    }

    public final void startGCThreads() {
        for (int i = 0; i < gcThreads.length; i++) {
            gcThreads[i].trigger();
        }
    }

    public void initializeGCThreads(BeltwayHeapScheme beltwayHeapScheme, Belt from, Belt to) {
        for (int i = 0; i < gcThreads.length; i++) {
            gcThreads[i].initialize(beltwayHeapScheme, from, to);
        }
    }

    /**
     * Holds the biased card table address.
     */
    public static final VmThreadLocal ADJUSTED_CARDTABLE_BASE = new VmThreadLocal("ADJUSTED_CARDTABLE_BASE", false, "Beltway: ->biased card table") {
        @Override
        public void initialize() {
            final Pointer vmThreadLocals = VmThread.currentVmThreadLocals();
            // enable write barriers by setting the adjusted card table address
            // use the normal card table
            ADJUSTED_CARDTABLE_BASE.setConstantWord(vmThreadLocals, BeltwayCardRegion.getAdjustedCardTable());
        }
    };

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

    /**
     * Scan cards of the from belt for references to the "to" belt, and evacuate the referenced object into the from card.
     * @param from the belt whose cards are scanned
     * @param to the belt references are looked up in cards.
     */
    public final void scanCardAndEvacuate(Belt from, Belt to) {
        final int startCardIndex = cardRegion.getCardIndexFromHeapAddress(from.start());
        final int endCardIndex = cardRegion.getCardIndexFromHeapAddress(from.end());
        for (int i = startCardIndex; i < endCardIndex; i++) {
            if (cardRegion.isCardMarked(i)) {
                final Address heapStartAddress = cardRegion.getHeapAddressFromCardIndex(i);
                final Pointer gcTLABStart = getGCTLABStartFromAddress(heapStartAddress);
                if (!gcTLABStart.isZero()) {
                    final Pointer gcTLABEnd = getGCTLABEndFromStart(gcTLABStart);
                    BeltwayCellVisitorImpl.linearVisitAllCellsTLAB(cellVisitor, gcTLABStart, gcTLABEnd, from, to);
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

    public final void evacuate(Belt from, Belt to) {
        to.evacuate(cellVisitor, from);
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
        if (useGCTlabs) {
            return gcTlabAllocate(belt, size);
        }
        return gcBumpAllocate(belt, size);

    }

    public boolean isGcThread(Thread thread) {
        return thread instanceof StopTheWorldGCDaemon || (parallelScavenging && thread instanceof BeltwayCollectorThread);
    }

    /**
     * Belt where mutator TLABs are allocated from.This belt is used both to refill TLABs and to
     *  allocate objects with a size larger than the TLAB size.
     */
    protected Belt tlabAllocationBelt;

    @Override
    protected  void doBeforeTLABRefill(Pointer tlabAllocationMark, Pointer tlabEnd) {
        Pointer hardLimit = tlabEnd.plus(TLAB_HEADROOM);
        if (tlabAllocationMark.greaterThan(tlabEnd)) {
            FatalError.check(hardLimit.equals(tlabAllocationMark), "TLAB allocation mark cannot be greater than TLAB End");
            return;
        }
        fillWithDeadObject(tlabAllocationMark, hardLimit);
    }

    protected void resetTLABs() {
        VmThreadMap.ACTIVE.forAllVmThreadLocals(null, resetTLAB);
    }

    /**
     * Allocate a chunk of memory of the specified size and refill a thread's TLAB with it.
     * @param enabledVmThreadLocals the thread whose TLAB will be refilled
     * @param tlabSize the size of the chunk of memory used to refill the TLAB
     */
    private void allocateAndRefillTLAB(Pointer enabledVmThreadLocals, Size tlabSize) {
        Pointer tlab = tlabAllocationBelt.allocateTLAB(tlabSize);
        if (MaxineVM.isDebug()) {
            DebugHeap.writeCellPadding(tlab, tlab.plus(tlabSize));
        }
        if (Heap.traceAllocation()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.printVmThread(VmThread.current(), false);
            Log.print(": Allocated TLAB at ");
            Log.print(tlab);
            Log.print(" [TOP=");
            Log.print(tlab.plus(tlab.plus(tlabSize.minus(TLAB_HEADROOM)).asAddress()));
            Log.print(", end=");
            Log.print(tlab.plus(tlabSize));
            Log.print(", size=");
            Log.print(tlabSize.toInt());
            Log.println("]");
            Log.unlock(lockDisabledSafepoints);
        }
        refillTLAB(enabledVmThreadLocals, tlab, tlabSize.minus(TLAB_HEADROOM));
    }

    /**
     * Handling of TLAB Overflow.
     */
    @Override
    protected Pointer handleTLABOverflow(Size size, Pointer enabledVmThreadLocals, Pointer tlabMark, Pointer tlabEnd) {
        // Should we refill the TLAB ?
        final TLABRefillPolicy refillPolicy = TLABRefillPolicy.getForCurrentThread(enabledVmThreadLocals);
        if (refillPolicy == null) {
            // No policy yet for the current thread. This must be the first time this thread uses a TLAB (it does not have one yet).
            ProgramError.check(tlabMark.isZero(), "thread must not have a TLAB yet");
            if (!usesTLAB()) {
                // We're not using TLAB. So let's assign the never refill tlab policy.
                TLABRefillPolicy.setForCurrentThread(enabledVmThreadLocals, NEVER_REFILL_TLAB);
                return tlabAllocationBelt.allocate(size);
            }
            // Allocate an initial TLAB and a refill policy. For simplicity, this one is allocated from the TLAB (see comment below).
            final Size tlabSize = initialTlabSize();
            allocateAndRefillTLAB(enabledVmThreadLocals, tlabSize);
            // Let's do a bit of dirty meta-circularity. The TLAB is refilled, and no-one except the current thread can use it.
            // So the tlab allocation is going to succeed here
            TLABRefillPolicy.setForCurrentThread(enabledVmThreadLocals, new SimpleTLABRefillPolicy(tlabSize));
            // Now, address the initial request. Note that we may recursed down to handleTLABOverflow again here if the
            // request is larger than the TLAB size. However, this second call will succeed and allocate outside of the tlab.
            return tlabAllocate(size);
        }
        final Size nextTLABSize = refillPolicy.nextTlabSize();
        if (size.greaterThan(nextTLABSize)) {
            // This couldn't be allocated in a TLAB, so go directly to direct allocation routine.
            return tlabAllocationBelt.allocate(size);
        }
        final Pointer hardLimit = tlabEnd.plus(TLAB_HEADROOM);
        final Pointer cell = DebugHeap.adjustForDebugTag(tlabMark);
        if (cell.plus(size).equals(hardLimit)) {
            // Can actually fit the object in the TLAB.
            setTlabAllocationMark(enabledVmThreadLocals, hardLimit);
            // allocateAndRefillTLAB(enabledVmThreadLocals, nextTLABSize);
            return cell;
        }

        if (!refillPolicy.shouldRefill(size, tlabMark)) {
            // Size would fit in a new tlab, but the policy says we shouldn't refill the tlab yet, so allocate directly in the heap.
            return tlabAllocationBelt.allocate(size);
        }
        // Refill TLAB and allocate (we know the request can be satisfied with a fresh TLAB and will therefore succeed).
        allocateAndRefillTLAB(enabledVmThreadLocals, nextTLABSize);
        return tlabAllocate(size);
    }

    /**
     * Called by belts when they fail to satisfy an allocation request.
     *
     * @param size
     * @param belt
     * @param adjustForDebugTag
     */
    @NEVER_INLINE
    protected void handleBeltAllocationFailure(Size size, Belt belt, boolean adjustForDebugTag) {
        Size neededSize = size;
        if (adjustForDebugTag) {
            neededSize = DebugHeap.adjustForDebugTag(neededSize.asPointer()).asSize();
        }
        if (!Heap.collectGarbage(neededSize)) {
            throw outOfMemoryError;
        }
    }

    public boolean contains(Address address) {
        return false;
    }

    public Size reportFreeSpace() {
        return beltManager.reportFreeSpace();
    }

    public Size reportUsedSpace() {
        return beltManager.reportUsedSpace();
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
    // Old TLAB code////
    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Pointer gcTlabAllocate(RuntimeMemoryRegion gcRegion, Size size) {
        // FIXME: REVISIT this code
        /*
        final VmThread thread = VmThread.current();
        final BeltTLAB tlab = thread.getTLAB();
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
         */
        return Pointer.zero();

    }

    private Size calculateGCTLABSize(Size size) {
        Size newSize = BeltwayHeapSchemeConfiguration.GC_TLAB_SIZE;
        while (size.greaterThan(newSize.minus(100))) {
            newSize = newSize.plus(200).roundedUpBy(BeltwayHeapSchemeConfiguration.GC_TLAB_SIZE).asSize();
        }
        return newSize;
    }

    public final void initializeFirstGCTLAB(RuntimeMemoryRegion gcRegion, BeltTLAB tlab, Size size) {
        //Debug.println("Try to set initial GC TLAB");
        final Size newSize = calculateGCTLABSize(size);
        final Size allocSize = newSize.asPointer().minusWords(1).asSize();
        final Pointer newTLABAddress = gcAllocateTLAB(gcRegion, allocSize);
        sideTable.markCreatingSideTable(newTLABAddress);
        if (newTLABAddress.isZero()) {
            FatalError.unexpected("Nursery is full, trigger GC in the First Allocation(?) Smth is wrong!");
        } else {
            tlab.initializeTLAB(newTLABAddress.asAddress(), newTLABAddress.asAddress(), size);
        }
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

    public void fillLastTLAB() {
        /*
        final BeltTLAB tlab = VmThreadMap.ACTIVE.getVmThreadForID(lastThreadAllocated).getTLAB();
        tlab.fillTLAB();*/
    }
    /*

    public void markSideTableLastTLAB() {
        final BeltTLAB tlab = VmThreadMap.ACTIVE.getVmThreadForID(lastThreadAllocated).getTLAB();
        SideTable.markStartSideTable(tlab.start());
    }

    public void resetCurrentTLAB() {
        final BeltwayCollectorThread thread = (BeltwayCollectorThread) Thread.currentThread();
        final BeltTLAB tlab = thread.getScavengeTLAB();
        tlab.unSet();
    }

    private static class StopTheWorldTLABReset implements Procedure<VmThread> {
        public void run(VmThread thread) {
            thread.getTLAB().unSet();
        }
    }

    private static final StopTheWorldTLABReset stopTheWorldReset = new StopTheWorldTLABReset();
     */
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
}
