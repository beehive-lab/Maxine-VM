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
package com.sun.max.vm.heap.sequential.Beltway.BeltwayGenerational;

import com.sun.max.annotate.*;
import com.sun.max.platform.*;
import com.sun.max.profile.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.profile.*;
import com.sun.max.vm.heap.sequential.*;
import com.sun.max.vm.heap.sequential.Beltway.*;
import com.sun.max.vm.tele.*;
import com.sun.max.vm.thread.*;

/**
 * @author Christos Kotselidis
 */

public class BeltwayHeapSchemeGenerational extends BeltwayHeapScheme {

    private static int[] _percentages = new int[] {10, 40, 50};
    protected static BeltwayGenerationalCollector _beltCollectorGenerational = new BeltwayGenerationalCollector();

    public BeltwayHeapSchemeGenerational(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (phase == MaxineVM.Phase.PRISTINE) {
            final Size heapSize = calculateHeapSize();
            final Address address = allocateMemory(heapSize);
            _beltwayConfiguration.initializeBeltWayConfiguration(address.roundedUpBy(BeltwayConfiguration.TLAB_SIZE.toInt()), heapSize.roundedUpBy(BeltwayConfiguration.TLAB_SIZE.toInt()).asSize(), 3,
                            _percentages);
            _beltManager.initializeBelts();
            if (Heap.verbose()) {
                _beltManager.printBeltsInfo();
            }
            final Size coveredRegionSize = _beltManager.getEnd().minus(Heap.bootHeapRegion().start()).asSize();
            _cardRegion.initialize(Heap.bootHeapRegion().start(), coveredRegionSize, Heap.bootHeapRegion().start().plus(coveredRegionSize));
            _sideTable.initialize(Heap.bootHeapRegion().start(), coveredRegionSize, Heap.bootHeapRegion().start().plus(coveredRegionSize).plus(_cardRegion.cardTableSize()).roundedUpBy(
                            Platform.target().pageSize()));
            CardRegion.switchToRegularCardTable(_cardRegion.cardTableBase().asPointer());
            TeleHeap.registerMemoryRegions(getEdenSpace(), getToSpace(), getMatureSpace());
        } else if (phase == MaxineVM.Phase.STARTING) {
            _collectorThread = new StopTheWorldDaemon("GC", _beltCollector);
        } else if (phase == MaxineVM.Phase.RUNNING) {
            _beltCollectorGenerational.setBeltwayHeapScheme(this);
            _beltCollector.setRunnable(_beltCollectorGenerational);
            _heapVerifier.initialize(this);
            _heapVerifier.getRootsVerifier().setFromSpace(BeltManager.getApplicationHeap());
            _heapVerifier.getRootsVerifier().setToSpace(getMatureSpace());
            if (Heap.verbose()) {
                HeapTimer.initializeTimers(Clock.SYSTEM_MILLISECONDS, "TotalGC", "EdenGC", "ToSpaceGC", "MatureSpaceGC", "Clear", "RootScan", "BootHeapScan", "CodeScan", "CardScan", "Scavenge");
            }
        }
    }

    @INLINE
    public final Belt getEdenSpace() {
        return _beltManager.getBelt(0);
    }

    @INLINE
    public final Belt getToSpace() {
        return _beltManager.getBelt(1);
    }

    @INLINE
    public final Belt getMatureSpace() {
        return _beltManager.getBelt(2);
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
    @NO_SAFEPOINTS("TODO")
    @Override
    public Pointer allocate(Size size) {
        if (!(_phase == MaxineVM.Phase.RUNNING)) {
            return bumpAllocateSlowPath(getEdenSpace(), size);
        }
        if (BeltwayConfiguration._useTLABS) {
            return tlabAllocate(getEdenSpace(), size);
        }

        return heapAllocate(getEdenSpace(), size);
    }

    @Override
    @INLINE
    public synchronized boolean collect(Size requestedFreeSpace) {
        if (_outOfMemory) {
            return false;
        }
        _beltCollector.setRunnable(_beltCollectorGenerational.getMinorGC());
        _collectorThread.execute();

        if (Heap.verbose()) {
            HeapStatistics.incrementEdenCollections();
        }
        if (getToSpace().getRemainingMemorySize().lessEqual(getEdenSpace().size())) {
            _beltCollector.setRunnable(_beltCollectorGenerational.getToGC());
            _collectorThread.execute();

            if (Heap.verbose()) {
                HeapStatistics.incrementToSpaceCollections();
            }
            if (getMatureSpace().getRemainingMemorySize().lessEqual(getToSpace().size().dividedBy(2))) {
                _beltCollector.setRunnable(_beltCollectorGenerational.getMajorGC());
                _collectorThread.execute();

                if (Heap.verbose()) {
                    HeapStatistics.incrementMatureCollections();
                }
                if (getMatureSpace().getRemainingMemorySize().lessEqual(getToSpace().size().dividedBy(2))) {
                    throw _outOfMemoryError;
                }

            }
        }

        _cardRegion.clearAllCards();
        return true;
    }

    @Override
    public boolean majorCollect(Size requestedFreeSpace) {
        return false;
    }

    private Size immediateFreeSpace(Belt belt) {
        return belt.end().minus(belt.getAllocationMark()).asSize();
    }


    @Override
    public boolean contains(Address address) {
        if (address.greaterEqual(Heap.bootHeapRegion().start()) & address.lessEqual(getMatureSpace().end())) {
            return true;
        }
        return false;
    }

}
