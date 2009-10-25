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
import com.sun.max.vm.code.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.StopTheWorldGCDaemon.*;
import com.sun.max.vm.heap.beltway.*;
import com.sun.max.vm.heap.beltway.ba2.BeltwayBA2Collector.*;
import com.sun.max.vm.heap.beltway.profile.*;
import com.sun.max.vm.reference.*;

/**
 * An Heap Scheme for a Appel-style collector implemented with Beltway. Uses two belts: one for the nursery and one for
 * the mature space.
 *
 * The scheme uses two collectors, one for minor collection of the young generation only, one for full collection.
 * There is a single-threaded and parallel version for each of these collectors. An instance of each of these collector
 * is created and initialized while bootstrapping. What collectors to use (single-threaded vs parallel) is selected at
 * runtime.
 *
 * The rationale for doing this is that the collector objects are allocated in the boot region and out of reach of the
 * copying mechanism. This allows the collector objects to use their instance fields (including references field, if
 * they point to boot regions objects) at any time (included GC time).
 *
 * @author Christos Kotselidis
 * @author Laurent Daynes
 */

public class BeltwayHeapSchemeBA2 extends BeltwayHeapScheme {

    private static int[] DEFAULT_BELT_HEAP_PERCENTAGE = new int[] {70, 30};
    private static final String [] BELT_DESCRIPTIONS = new String[] {"Nursery Belt", "Mature Belt" };


    /**
     * Single threaded version of the collectors.
     */
    private static final BeltwayBA2Collector [] singleThreadedCollectors = new BeltwayBA2Collector[] {new MinorGCCollector(), new FullGCCollector() };

    /**
     * Parallel version of the collectors.
     */
    private static final BeltwayBA2Collector [] parallelCollectors = new BeltwayBA2Collector[] {new ParMinorGCCollector(), new ParFullGCCollector() };

    final class BA2HeapBoundChecker extends HeapBoundChecker {
        private Address nurseryStart;
        private Address nurseryEnd;
        private Address matureStart;
        private Address matureEnd;

        void reset() {
            nurseryStart = getNurserySpace().start();
            nurseryEnd = getNurserySpace().getAllocationMark();
            matureStart = getMatureSpace().start();
            matureEnd = getMatureSpace().getAllocationMark();
        }

        @INLINE
        private boolean inNurserySpace(Pointer origin) {
            return origin.greaterEqual(nurseryStart) && origin.lessThan(nurseryEnd);
        }

        @INLINE
        private boolean inMatureSpace(Pointer origin) {
            return origin.greaterEqual(matureStart) && origin.lessThan(matureEnd);
        }

        @INLINE(override = true)
        @Override
        public boolean contains(Pointer origin) {
            return inMatureSpace(origin) ||  Heap.bootHeapRegion.contains(origin) || Code.contains(origin)  ||
            inNurserySpace(origin) || ImmortalHeap.getImmortalHeap().contains(origin);
        }
    }

    final BA2HeapBoundChecker ba2HeapBoundChecker;

    Collector minorGCCollector;
    Collector fullGCCollector;

    public Collector getMinorGC() {
        return minorGCCollector;
    }

    public Collector getMajorGC() {
        return fullGCCollector;
    }

    public BeltwayHeapSchemeBA2(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        ba2HeapBoundChecker = new BA2HeapBoundChecker();
    }

    @Override
    protected int [] beltHeapPercentage() {
        return DEFAULT_BELT_HEAP_PERCENTAGE;
    }

    @Override
    protected String [] beltDescriptions() {
        return BELT_DESCRIPTIONS;
    }

    @Override
    protected HeapBoundChecker heapBoundChecker() {
        return ba2HeapBoundChecker;
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);

        if (MaxineVM.isHosted()) {
            for (BeltwayBA2Collector collector : singleThreadedCollectors) {
                collector.initialize(this);
            }
            for (BeltwayBA2Collector collector : parallelCollectors) {
                collector.initialize(this);
            }
        }
        if (phase == MaxineVM.Phase.PRISTINE) {
            adjustedCardTableAddress = BeltwayCardRegion.adjustedCardTableBase(cardRegion.cardTableBase().asPointer());
            beltManager.swapBelts(getMatureSpace(), getNurserySpace());
            getMatureSpace().setExpandable(true);

            final BeltwayBA2Collector [] collectors = parallelScavenging ? parallelCollectors : singleThreadedCollectors;
            minorGCCollector = collectors[0];
            fullGCCollector = collectors[1];

        } else if (phase == MaxineVM.Phase.RUNNING) {
            if (Heap.verbose()) {
                HeapTimer.initializeTimers(Clock.SYSTEM_MILLISECONDS, "TotalGC", "NurserySpaceGC", "MatureSpaceGC", "Clear", "RootScan", "BootHeapScan", "CodeScan", "CardScan", "Scavenge");
            }
        }
    }

    @Override
    protected void initializeTlabAllocationBelt() {
        tlabAllocationBelt = getNurserySpace();
    }

    @INLINE(override = true)
    public Belt getNurserySpace() {
        return beltManager.getBelt(0);
    }

    @INLINE(override = true)
    public Belt getMatureSpace() {
        return beltManager.getBelt(1);
    }

    @INLINE(override = true)
    public  Size getUsableMemory() {
        return getMaxHeapSize().dividedBy(2);
    }

    @INLINE(override = true)
    public  Size getCopyReserveMemory() {
        return getMaxHeapSize().minus(getUsableMemory());
    }

    public synchronized boolean collectGarbage(Size requestedFreeSpace) {
        boolean result = false;
        if (minorCollect(requestedFreeSpace)) {
            result = true;
            if (getMatureSpace().getUsedMemorySize().greaterEqual(getUsableMemory())) {
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
        collectorThread.execute(getMinorGC());
        if (immediateFreeSpace(getNurserySpace()).greaterEqual(requestedFreeSpace)) {
            return true;
        }

        return false;
    }

    public boolean majorCollect(Size requestedFreeSpace) {
        if (outOfMemory) {
            return false;
        }
        collectorThread.execute(getMajorGC());
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
        // FIXME: this look suspicious. Shouldn't it be to the end of the heap instead of the end of the nursery ?
        return address.greaterEqual(Heap.bootHeapRegion.start()) && address.lessEqual(getNurserySpace().end());
    }

    public boolean checkOverlappingBelts(Belt from, Belt to) {
        return from.getAllocationMark().greaterThan(to.start()) || from.end().greaterThan(to.start());
    }

    @INLINE(override = true)
    public void writeBarrier(Reference from, Reference to) {
        // do nothing.
    }

    public boolean isForwardingPointer(Pointer pointer) {
        // TODO Auto-generated method stub
        return false;
    }

    public Pointer getTrueLocationFromPointer(Pointer value) {
        // TODO Auto-generated method stub
        return null;
    }

    public Pointer getForwardedObject(Pointer objectPointer, DataAccess dataAccess) {
        // TODO Auto-generated method stub
        return null;
    }

}
