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
import com.sun.max.profile.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.beltway.*;
import com.sun.max.vm.heap.beltway.generational.BeltwayGenerationalCollector.*;
import com.sun.max.vm.heap.beltway.profile.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.tele.*;

/**
 * Heap scheme for a three-generations generational collector. Configured with three belts: one for a nursery (the eden
 * space); one for a survivor space (to space); and one for the tenured generation (the mature space). The scheme uses a
 * specific collector for each belt when they fills up. There is a single-threaded and parallel version for each of
 * these collectors. An instance of each of these collector is created and initialized at prototyping time. What
 * collectors to use (single-threaded vs parallel) is selected at runtime.
 *
 * The rationale for doing this is that the collector objects are allocated in the boot region and out of reach of the
 * copying mechanism. This allows the collector objects to use their instance fields (including references field, if
 * they point to boot regions objects) at any time (included GC time).
 *
 * @author Christos Kotselidis
 * @author Laurent Daynes
 */
public class BeltwayHeapSchemeGenerational extends BeltwayHeapScheme {
    /**
     * Default sizing of each belt, expressed as percentage of the total heap.
     * Each entry in the array correspond to the percentage of heap taken by the corresponding belt
     * (e.g., belt 0, i.e., the eden, is allocated 10 % percent of the heap.
     */
    private static final  int [] DEFAULT_BELT_HEAP_PERCENTAGE = new int[] {10, 40, 50};

    /**
     * Label for the three belts used by this heap scheme.
     */
    private static final String [] BELT_DESCRIPTIONS = new String[] {"Eden Belt", "To Belt", "Mature Belt" };

    /**
     * Single threaded version of the collectors.
     */
    private static final BeltwayGenerationalCollector [] singleThreadedCollectors = new BeltwayGenerationalCollector[] {new EdenCollector(), new ToSpaceCollector(), new MajorCollector() };

    /**
     * Parallel version of the collectors.
     */
    private static final BeltwayGenerationalCollector [] parallelCollectors = new BeltwayGenerationalCollector[] {new ParEdenCollector(), new ParToSpaceCollector(), new ParMajorCollector() };

    private Runnable edenGC;
    private Runnable toGC;
    private Runnable majorGC;

    public Runnable getMinorGC() {
        return edenGC;
    }

    public Runnable getMajorGC() {
        return majorGC;
    }

    public Runnable getToGC() {
        return toGC;
    }

    public BeltwayHeapSchemeGenerational(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
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
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (MaxineVM.isPrototyping()) {
            for (BeltwayGenerationalCollector collector : singleThreadedCollectors) {
                collector.initialize(this);
            }
            for (BeltwayGenerationalCollector collector : parallelCollectors) {
                collector.initialize(this);
            }
        }
        if (phase == MaxineVM.Phase.PRISTINE) {
            // The following line enables allocation to take place.
            tlabAllocationBelt = getEdenSpace();
            // Watch out: the following create a MemoryRegion array
            InspectableHeapInfo.init(getEdenSpace(), getToSpace(), getMatureSpace());

            final BeltwayGenerationalCollector [] collectors = parallelScavenging ? parallelCollectors : singleThreadedCollectors;

            edenGC = (Runnable) collectors[0];
            toGC =  (Runnable) collectors[1];
            majorGC = (Runnable) collectors[2];

        } else if (phase == MaxineVM.Phase.RUNNING) {
            heapVerifier.initialize(beltManager.getApplicationHeap(), getMatureSpace());
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

    public synchronized boolean collectGarbage(Size requestedFreeSpace) {
        if (outOfMemory) {
            return false;
        }
        collectorThread.execute(getMinorGC());

        if (Heap.verbose()) {
            HeapStatistics.incrementEdenCollections();
        }
        if (getToSpace().getRemainingMemorySize().lessEqual(getEdenSpace().size())) {
            collectorThread.execute(getToGC());

            if (Heap.verbose()) {
                HeapStatistics.incrementToSpaceCollections();
            }
            if (getMatureSpace().getRemainingMemorySize().lessEqual(getToSpace().size().dividedBy(2))) {
                collectorThread.execute(getMajorGC());

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
        return address.greaterEqual(Heap.bootHeapRegion.start()) & address.lessEqual(getMatureSpace().end());
    }

    @INLINE(override = true)
    public void writeBarrier(Reference from, Reference to) {
        // do nothing.
    }
}
