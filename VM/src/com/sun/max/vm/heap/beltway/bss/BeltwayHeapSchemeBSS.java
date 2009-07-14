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
package com.sun.max.vm.heap.beltway.bss;

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
 * Heap scheme for a semi-space beltway collector. Use a single belt with two increments, each allocated half of the total heap space.
 * @author Christos Kotselidis
 */

public class BeltwayHeapSchemeBSS extends BeltwayHeapScheme {

    private static int[] percentages = new int[] {50, 50};
    protected static BeltwaySSCollector beltCollectorBSS = new BeltwaySSCollector();

    public BeltwayHeapSchemeBSS(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (phase == MaxineVM.Phase.PRISTINE) {
            final Size heapSize = calculateHeapSize();
            final Address address = allocateMemory(heapSize);
            beltwayConfiguration.initializeBeltWayConfiguration(address.roundedUpBy(BeltwayConfiguration.TLAB_SIZE.toInt()), heapSize.roundedUpBy(BeltwayConfiguration.TLAB_SIZE.toInt()).asSize(), 2,
                            percentages);
            beltManager.initializeBelts();
            if (Heap.verbose()) {
                beltManager.printBeltsInfo();
            }
            final Size coveredRegionSize = beltManager.getEnd().minus(Heap.bootHeapRegion().start()).asSize();
            cardRegion.initialize(Heap.bootHeapRegion().start(), coveredRegionSize, Heap.bootHeapRegion().start().plus(coveredRegionSize));
            sideTable.initialize(Heap.bootHeapRegion().start(), coveredRegionSize, Heap.bootHeapRegion().start().plus(coveredRegionSize).plus(cardRegion.cardTableSize()).roundedUpBy(
                            Platform.target().pageSize));
            BeltwayCardRegion.switchToRegularCardTable(cardRegion.cardTableBase().asPointer());
            InspectableHeapInfo.registerMemoryRegions(getToSpace(), getFromSpace());
        } else if (phase == MaxineVM.Phase.STARTING) {
            collectorThread = new BeltwayStopTheWorldDaemon("GC", beltCollector);
        } else if (phase == MaxineVM.Phase.RUNNING) {
            beltCollectorBSS.setBeltwayHeapScheme(this);
            beltCollector.setRunnable(beltCollectorBSS);
            heapVerifier.initialize(this);
            heapVerifier.getRootsVerifier().setFromSpace(BeltManager.getApplicationHeap());
            heapVerifier.getRootsVerifier().setToSpace(getToSpace());
            HeapTimer.initializeTimers(Clock.SYSTEM_MILLISECONDS, "TotalGC", "Clear", "RootScan", "BootHeapScan", "CodeScan", "Scavenge");
        }
    }

    public Belt getFromSpace() {
        return beltManager.getBelt(0);
    }

    public Belt getToSpace() {
        return beltManager.getBelt(1);
    }

    public synchronized boolean collectGarbage(Size requestedFreeSpace) {
        if (outOfMemory) {
            return false;
        }
        collectorThread.execute();
        if (immediateFreeSpace().greaterEqual(requestedFreeSpace)) {
            return true;
        }
        return false;
    }

    @INLINE(override = true)
    @NO_SAFEPOINTS("TODO")
    public Pointer allocate(Size size) {
        if (!MaxineVM.isRunning()) {
            return bumpAllocateSlowPath(getFromSpace(), size);
        }
        if (BeltwayConfiguration.useTLABS) {
            return tlabAllocate(getFromSpace(), size);
        }
        return heapAllocate(getFromSpace(), size);
    }

    private Size immediateFreeSpace() {
        return getFromSpace().end().minus(getFromSpace().getAllocationMark()).asSize();
    }

    @Override
    public boolean contains(Address address) {
        return address.greaterEqual(Heap.bootHeapRegion().start()) && address.lessEqual(BeltwayConfiguration.getApplicationHeapEndAddress());
    }

    @INLINE(override = true)
    public void writeBarrier(Reference from, Reference to) {
        // do nothing.
    }
}
