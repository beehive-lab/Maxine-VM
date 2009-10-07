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
import com.sun.max.profile.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.beltway.*;
import com.sun.max.vm.heap.beltway.profile.*;
import com.sun.max.vm.reference.*;

/**
 * Heap scheme for a semi-space beltway collector. Use two belts, each allocated half of the total heap space. The heap
 * scheme can use two collectors, a single-threaded one and a parallel one. An instance of each of these collectors is
 * created at prototyping time and their reference stored in static variable. Which of the two collector to use is
 * decided at runtime. The rationale for doing this is that the collector objects are allocated in the boot region and
 * out of reach of the copying mechanism. This allows the collector objects to use their instance fields (including
 * references field, if they point to boot regions objects) at any time (included GC time).
 *
 * @author Christos Kotselidis
 * @author Laurent Daynes
 */

public class BeltwayHeapSchemeBSS extends BeltwayHeapScheme {

    private static int[] DEFAULT_BELT_HEAP_PERCENTAGE = new int[] {50, 50};
    private final String [] BELT_DESCRIPTIONS = new String[] {"From Belt", "To Belt"};

    /**
     * Single threaded version of the beltway collector.
     */
    private static final BeltwaySSCollector singleThreadedCollector = new BeltwaySSCollector();

    /**
     * Single threaded version of the beltway collector.
     */
    private static final SemiSpaceParCollector parallelCollector = new SemiSpaceParCollector();

    /**
     * Bound Checker for heap verification only.
     */
    final class BSSHeapBoundChecker extends HeapBoundChecker {
        private Address start;
        private Address end;

        void reset() {
            start = getFromSpace().start();
            end = getFromSpace().getAllocationMark();
        }

        @INLINE
        private boolean inFromSpace(Pointer origin) {
            return origin.greaterEqual(start) && origin.lessThan(end);
        }

        @INLINE
        @Override
        public boolean contains(Pointer origin) {
            return inFromSpace(origin) ||  Heap.bootHeapRegion.contains(origin) || Code.contains(origin) || ImmortalHeap.getImmortalHeap().contains(origin);
        }
    }

    final BSSHeapBoundChecker bssHeapBoundChecker;
    private BeltwaySSCollector bssCollector;

    public BeltwayHeapSchemeBSS(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        bssHeapBoundChecker = new BSSHeapBoundChecker();
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
        return bssHeapBoundChecker;
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (MaxineVM.isPrototyping()) {
            singleThreadedCollector.initialize(this);
            parallelCollector.initialize(this);
        }
        if (phase == MaxineVM.Phase.PRISTINE) {
            bssCollector = parallelScavenging ? parallelCollector : singleThreadedCollector;
        } else if (phase == MaxineVM.Phase.RUNNING) {
            if (Heap.verbose()) {
                HeapTimer.initializeTimers(Clock.SYSTEM_MILLISECONDS, "TotalGC", "Clear", "RootScan", "BootHeapScan", "CodeScan", "Scavenge");
            }
        }
    }

    @Override
    protected void initializeTlabAllocationBelt() {
        tlabAllocationBelt = getFromSpace();
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
        collectorThread.execute(bssCollector);
        if (immediateFreeSpace().greaterEqual(requestedFreeSpace)) {
            return true;
        }
        return false;
    }

    private Size immediateFreeSpace() {
        return getFromSpace().end().minus(getFromSpace().getAllocationMark()).asSize();
    }

    @Override
    public boolean contains(Address address) {
        return address.greaterEqual(Heap.bootHeapRegion.start()) && address.lessEqual(getHeapEnd());
    }

    @INLINE(override = true)
    public void writeBarrier(Reference from, Reference to) {
        // do nothing.
    }
}
