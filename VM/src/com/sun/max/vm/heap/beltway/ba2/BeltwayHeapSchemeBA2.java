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
package com.sun.max.vm.heap.beltway.ba2;

import com.sun.max.annotate.*;
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
 */

public class BeltwayHeapSchemeBA2 extends BeltwayHeapScheme {

    private static int[] DEFAULT_BELT_HEAP_PERCENTAGE = new int[] {70, 30};
    protected static BeltwayBA2Collector beltCollectorBA2 = new BeltwayBA2Collector();

    public BeltwayHeapSchemeBA2(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    @Override
    protected int [] defaultBeltHeapPercentage() {
        return DEFAULT_BELT_HEAP_PERCENTAGE;
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (phase == MaxineVM.Phase.PRISTINE) {
            adjustedCardTableAddress = BeltwayCardRegion.adjustedCardTableBase(cardRegion.cardTableBase().asPointer());
            beltManager.swapBelts(getMatureSpace(), getNurserySpace());
            getMatureSpace().setExpandable(true);
            tlabAllocationBelt = getNurserySpace();
            InspectableHeapInfo.registerMemoryRegions(getNurserySpace(), getMatureSpace());
            collectorThread.start();
        } else if (phase == MaxineVM.Phase.RUNNING) {
            beltCollectorBA2.setBeltwayHeapScheme(this);
            beltCollector.setRunnable(beltCollectorBA2);
            heapVerifier.initialize(this);
            heapVerifier.getRootsVerifier().setFromSpace(beltManager.getApplicationHeap());
            heapVerifier.getRootsVerifier().setToSpace(getMatureSpace());
            if (Heap.verbose()) {
                HeapTimer.initializeTimers(Clock.SYSTEM_MILLISECONDS, "TotalGC", "NurserySpaceGC", "MatureSpaceGC", "Clear", "RootScan", "BootHeapScan", "CodeScan", "CardScan", "Scavenge");
            }
        }
    }

    @INLINE
    public Belt getNurserySpace() {
        return beltManager.getBelt(0);
    }

    @INLINE
    public Belt getMatureSpace() {
        return beltManager.getBelt(1);
    }


    public synchronized boolean collectGarbage(Size requestedFreeSpace) {
        boolean result = false;
        if (minorCollect(requestedFreeSpace)) {
            result = true;
            if (getMatureSpace().getUsedMemorySize().greaterEqual(BeltwayHeapSchemeConfiguration.getMaxHeapSize().dividedBy(2))) {
                result = majorCollect(requestedFreeSpace);
            }
        }
        cardRegion.clearAllCards();
        return result;

    }


    public boolean minorCollect(Size requestedFreeSpace) {
        if (outOfMemory) {
            return false;
        }
        beltCollector.setRunnable(beltCollectorBA2.getMinorGC());
        collectorThread.execute();
        if (immediateFreeSpace(getNurserySpace()).greaterEqual(requestedFreeSpace)) {
            return true;
        }

        return false;
    }

    public boolean majorCollect(Size requestedFreeSpace) {
        if (outOfMemory) {
            return false;
        }
        beltCollector.setRunnable(beltCollectorBA2.getMajorGC());
        collectorThread.execute();
        if (!BeltwayHeapScheme.outOfMemory == true) {
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
        return address.greaterEqual(Heap.bootHeapRegion.start()) && address.lessEqual(getNurserySpace().end());
    }

    public boolean checkOverlappingBelts(Belt from, Belt to) {
        return from.getAllocationMark().greaterThan(to.start()) || from.end().greaterThan(to.start());
    }

    @INLINE(override = true)
    public void writeBarrier(Reference from, Reference to) {
        // do nothing.
    }

}
