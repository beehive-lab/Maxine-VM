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
/*VCSID=1a121dc1-7e8d-42a2-a632-1787c75b9f16*/
package com.sun.max.vm.heap.sequential.Beltway.BeltwayBA2;

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

public class BeltwayHeapSchemeBA2 extends BeltwayHeapScheme {

    private static int[] _percentages = new int[] {70, 30};
    protected static BeltwayBA2Collector _beltCollectorBA2 = new BeltwayBA2Collector();

    public BeltwayHeapSchemeBA2(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (phase == MaxineVM.Phase.PRISTINE) {
            final Size heapSize = calculateHeapSize();
            final Address address = allocateMemory(heapSize);
            _beltwayConfiguration.initializeBeltWayConfiguration(address.roundedUpBy(BeltwayConfiguration.TLAB_SIZE.toInt()), heapSize.roundedUpBy(BeltwayConfiguration.TLAB_SIZE.toInt()).asSize(), 2,
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
            _adjustedCardTableAddress = CardRegion.adjustedCardTableBase(_cardRegion.cardTableBase().asPointer());
            _beltManager.swapBelts(getMatureSpace(), getNurserySpace());
            getMatureSpace().setExpandable(true);
            TeleHeap.registerMemoryRegions(getNurserySpace(), getMatureSpace());
        } else if (phase == MaxineVM.Phase.STARTING) {
            _collectorThread = new StopTheWorldDaemon("GC", _beltCollector);
        } else if (phase == MaxineVM.Phase.RUNNING) {
            _beltCollectorBA2.setBeltwayHeapScheme(this);
            _beltCollector.setRunnable(_beltCollectorBA2);
            _heapVerifier.initialize(this);
            _heapVerifier.getRootsVerifier().setFromSpace(BeltManager.getApplicationHeap());
            _heapVerifier.getRootsVerifier().setToSpace(getMatureSpace());
            if (Heap.verbose()) {
                HeapTimer.initializeTimers(Clock.SYSTEM_MILLISECONDS, "TotalGC", "NurserySpaceGC", "MatureSpaceGC", "Clear", "RootScan", "BootHeapScan", "CodeScan", "CardScan", "Scavenge");
            }
        }
    }

    @INLINE
    public Belt getNurserySpace() {
        return _beltManager.getBelt(0);
    }

    @INLINE
    public Belt getMatureSpace() {
        return _beltManager.getBelt(1);
    }

    @INLINE
    @NO_SAFEPOINTS("TODO")
    @Override
    public Pointer allocate(Size size) {
        if (!(_phase == MaxineVM.Phase.RUNNING)) {
            return bumpAllocateSlowPath(getNurserySpace(), size);
        }
        if (BeltwayConfiguration._useTLABS) {
            return tlabAllocate(getNurserySpace(), size);
        }
        return heapAllocate(getNurserySpace(), size);
    }

    @Override
    @INLINE
    public synchronized boolean collect(Size requestedFreeSpace) {
        boolean result = false;
        if (minorCollect(requestedFreeSpace)) {
            result = true;
            if (getMatureSpace().getUsedMemorySize().greaterEqual(HeapSchemeConfiguration.getMaxHeapSize().dividedBy(2))) {
                if (majorCollect(requestedFreeSpace)) {
                    result = true;
                } else {
                    result = false;
                }
            }
        }
        _cardRegion.clearAllCards();
        return result;

    }

    @Override
    @INLINE
    public boolean minorCollect(Size requestedFreeSpace) {
        if (_outOfMemory) {
            return false;
        }
        _beltCollector.setRunnable(_beltCollectorBA2.getMinorGC());
        _collectorThread.execute();
        if (immediateFreeSpace(getNurserySpace()).greaterEqual(requestedFreeSpace)) {
            return true;
        }

        return false;
    }

    @Override
    @INLINE
    public boolean majorCollect(Size requestedFreeSpace) {
        if (_outOfMemory) {
            return false;
        }
        _beltCollector.setRunnable(_beltCollectorBA2.getMajorGC());
        _collectorThread.execute();
        if (!BeltwayHeapScheme._outOfMemory == true) {
            if (immediateFreeSpace(getMatureSpace()).greaterEqual(requestedFreeSpace)) {
                return true;
            }
        }
        return false;
    }

    @INLINE
    private Size immediateFreeSpace(Belt belt) {
        return belt.end().minus(belt.getAllocationMark()).asSize();
    }

    @Override
    public boolean contains(Address address) {
        if (address.greaterEqual(Heap.bootHeapRegion().start()) & address.lessEqual(getNurserySpace().end())) {
            return true;
        }
        return false;
    }

    @INLINE
    public boolean checkOverlappingBelts(Belt from, Belt to) {
        return (from.getAllocationMark().greaterThan(to.start()) || from.end().greaterThan(to.start())) ? true : false;
    }

}
