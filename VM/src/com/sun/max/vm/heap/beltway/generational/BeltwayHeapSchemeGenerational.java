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
package com.sun.max.vm.heap.beltway.generational;

import com.sun.max.annotate.*;
import com.sun.max.platform.*;
import com.sun.max.profile.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.beltway.*;
import com.sun.max.vm.heap.beltway.profile.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.tele.*;

/**
 * @author Christos Kotselidis
 *
 * A Beltway collector configured as a generational heap.
 * Configured with three belts: one for an nursery (the eden space); one for the tenured generation (the mature space);
 */
public class BeltwayHeapSchemeGenerational extends BeltwayHeapScheme {

    /**
     * Default sizing of each belt, expressed as percentage of the total heap.
     * Each entry in the array correspond to the percentage of heap taken by the corresponding belt
     * (e.g., belt 0, i.e., the eden, is allocated _percentages[0] percent of the heap.
     */
    private static int[] percentages = new int[] {10, 40, 50};
    protected static BeltwayGenerationalCollector beltCollectorGenerational = new BeltwayGenerationalCollector();

    public BeltwayHeapSchemeGenerational(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (phase == MaxineVM.Phase.PRISTINE) {
            final Size heapSize = calculateHeapSize();
            final Address address = allocateMemory(heapSize);
            beltwayConfiguration.initializeBeltWayConfiguration(address.roundedUpBy(BeltwayConfiguration.TLAB_SIZE.toInt()),
                heapSize.roundedUpBy(BeltwayConfiguration.TLAB_SIZE.toInt()).asSize(), 3, percentages);
            beltManager.initializeBelts();
            if (Heap.verbose()) {
                beltManager.printBeltsInfo();
            }
            final Size coveredRegionSize = beltManager.getEnd().minus(Heap.bootHeapRegion().start()).asSize();
            cardRegion.initialize(Heap.bootHeapRegion().start(), coveredRegionSize, Heap.bootHeapRegion().start().plus(coveredRegionSize));
            sideTable.initialize(Heap.bootHeapRegion().start(), coveredRegionSize, Heap.bootHeapRegion().start().plus(coveredRegionSize).plus(cardRegion.cardTableSize()).roundedUpBy(
                            Platform.target().pageSize));
            BeltwayCardRegion.switchToRegularCardTable(cardRegion.cardTableBase().asPointer());
            TeleHeapInfo.registerMemoryRegions(getEdenSpace(), getToSpace(), getMatureSpace());
        } else if (phase == MaxineVM.Phase.STARTING) {
            collectorThread = new BeltwayStopTheWorldDaemon("GC", beltCollector);
        } else if (phase == MaxineVM.Phase.RUNNING) {
            beltCollectorGenerational.setBeltwayHeapScheme(this);
            beltCollector.setRunnable(beltCollectorGenerational);
            heapVerifier.initialize(this);
            heapVerifier.getRootsVerifier().setFromSpace(BeltManager.getApplicationHeap());
            heapVerifier.getRootsVerifier().setToSpace(getMatureSpace());
            if (Heap.verbose()) {
                HeapTimer.initializeTimers(Clock.SYSTEM_MILLISECONDS, "TotalGC", "EdenGC", "ToSpaceGC", "MatureSpaceGC", "Clear", "RootScan", "BootHeapScan", "CodeScan", "CardScan", "Scavenge");
            }
        }
    }

    @INLINE
    public final Belt getEdenSpace() {
        return beltManager.getBelt(0);
    }

    @INLINE
    public final Belt getToSpace() {
        return beltManager.getBelt(1);
    }

    @INLINE
    public final Belt getMatureSpace() {
        return beltManager.getBelt(2);
    }

    /**
     * This is the generic allocator which first attempt to allocate space on current thread's TLAB. If allocation
     * fails, it checks if a new TLAB can be allocated in the youngest belt. If the TLAB allocation is successful the
     * object is allocated in the new allocated TLAB. Otherwise a minor GC is triggered. TODO: Recalculate tlabs' sizes
     *
     * @param size The size of the allocation.
     * @return the pointer to the address in which we can allocate. If null, a GC should be triggered.
     */
    @INLINE
    public Pointer allocate(Size size) {
        if (!MaxineVM.isRunning()) {
            return bumpAllocateSlowPath(getEdenSpace(), size);
        }
        if (BeltwayConfiguration.useTLABS) {
            return tlabAllocate(getEdenSpace(), size);
        }
        return heapAllocate(getEdenSpace(), size);
    }

    public synchronized boolean collectGarbage(Size requestedFreeSpace) {
        if (outOfMemory) {
            return false;
        }
        beltCollector.setRunnable(beltCollectorGenerational.getMinorGC());
        collectorThread.execute();

        if (Heap.verbose()) {
            HeapStatistics.incrementEdenCollections();
        }
        if (getToSpace().getRemainingMemorySize().lessEqual(getEdenSpace().size())) {
            beltCollector.setRunnable(beltCollectorGenerational.getToGC());
            collectorThread.execute();

            if (Heap.verbose()) {
                HeapStatistics.incrementToSpaceCollections();
            }
            if (getMatureSpace().getRemainingMemorySize().lessEqual(getToSpace().size().dividedBy(2))) {
                beltCollector.setRunnable(beltCollectorGenerational.getMajorGC());
                collectorThread.execute();

                if (Heap.verbose()) {
                    HeapStatistics.incrementMatureCollections();
                }
                if (getMatureSpace().getRemainingMemorySize().lessEqual(getToSpace().size().dividedBy(2))) {
                    throw outOfMemoryError;
                }

            }
        }

        cardRegion.clearAllCards();
        return true;
    }

    private Size immediateFreeSpace(Belt belt) {
        return belt.end().minus(belt.getAllocationMark()).asSize();
    }


    @Override
    public boolean contains(Address address) {
        return address.greaterEqual(Heap.bootHeapRegion().start()) & address.lessEqual(getMatureSpace().end());
    }

    @INLINE
    public void writeBarrier(Reference from, Reference to) {
        // do nothing.
    }
}
