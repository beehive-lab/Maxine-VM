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
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.beltway.*;
import com.sun.max.vm.tele.*;

/**
 * An Appel-style generational collector. The heap is divided in two generations, each implemented using a belt.
 * @see BeltwayHeapSchemeBA2
 *
 * @author Laurent Daynes
 * @author Christos Kotselidis
 */

public class BeltwayBA2Collector extends BeltwayCollector {
    @CONSTANT_WHEN_NOT_ZERO
    protected Belt nurserySpace;
    @CONSTANT_WHEN_NOT_ZERO
    protected Belt matureSpace;

    BeltwayBA2Collector(String collectorName) {
        super(collectorName);
    }

    @PROTOTYPE_ONLY
    @Override
    public void initialize(BeltwayHeapScheme heapScheme) {
        super.initialize(heapScheme);
        final BeltwayHeapSchemeBA2 ba2HeapScheme = (BeltwayHeapSchemeBA2) heapScheme;
        nurserySpace =  ba2HeapScheme.getNurserySpace();
        matureSpace =  ba2HeapScheme.getMatureSpace();
    }

    protected void verifyHeap(String when) {
        final BeltwayHeapSchemeBA2 ba2HeapScheme = (BeltwayHeapSchemeBA2) heapScheme;
        if (Heap.verbose()) {
            Log.print("Verify Heap");
            Log.println(when);
        }

        ba2HeapScheme.ba2HeapBoundChecker.reset();
        verifyBelt(nurserySpace);
        verifyBelt(matureSpace);
    }

    static class FullGCCollector extends BeltwayBA2Collector implements Runnable {
        /**
         * A temporary virtual belt used to represent the mature space's reserve.
         */
        private final Belt matureSpaceReserve;

        FullGCCollector() {
            super("Major");
            matureSpaceReserve = new Belt();
            matureSpaceReserve.resetAllocationMark();
        }

        public void run() {
            prologue();
            if (heapScheme.verifyBeforeGC()) {
                verifyHeap("Before full GC");
            }

            monitorScheme.beforeGarbageCollection();

            final Pointer matureSpaceEnd = matureSpace.end().asPointer();
            final Belt matureSpaceBeforeAllocation = matureSpace;
            matureSpaceBeforeAllocation.setEnd(matureSpaceBeforeAllocation.getAllocationMarkSnapshot().asAddress());

            // Prepare the space reserve.
            matureSpaceReserve.setStart(matureSpace.getPrevAllocationMark());
            matureSpaceReserve.setEnd(matureSpaceEnd);
            matureSpaceReserve.setAllocationMark(matureSpace.getAllocationMark());
            matureSpaceReserve.setExpandable(true);

            if (Heap.verbose()) {
                printBeltInfo("matureSpaceBeforeAllocation", matureSpaceBeforeAllocation);
                printBeltInfo("matureSpaceReserve", matureSpaceReserve);
            }

            // Start scanning the reachable objects from my roots.
            heapScheme.scavengeRoot(matureSpaceBeforeAllocation, matureSpaceReserve);

            if (Heap.verbose()) {
                printBeltInfo("matureSpaceReserve", matureSpaceReserve);
            }
            evacuateFollowers(matureSpaceBeforeAllocation, matureSpaceReserve);

            if (heapScheme.verifyAfterGC()) {
                verifyHeap("After full GC");
            }

            matureSpaceReserve.setEnd(matureSpaceReserve.getAllocationMark());

            if (Heap.verbose()) {
                printBeltInfo("matureSpaceBeforeAllocation", matureSpaceBeforeAllocation);
                printBeltInfo("matureSpaceReserve", matureSpaceReserve);
            }

            matureSpace.resetAllocationMark();
            matureSpace.setEnd(matureSpaceEnd);
            final Size usableMemory =  ((BeltwayHeapSchemeBA2) heapScheme).getUsableMemory();
            if (Heap.verbose()) {
                printBeltInfo("Mature Space", matureSpace);
                Log.print("Mature Space Reserve Size: ");
                Log.println(matureSpaceReserve.size().toLong());
                Log.print("Configuration usable memory Size ");
                Log.println(usableMemory);
            }

            if (matureSpaceReserve.size().lessEqual(usableMemory)) {
                if (Heap.verbose()) {
                    Log.println("Compaction ");
                }

                // Start scanning the reachable objects from my roots.
                heapScheme.scavengeRoot(matureSpaceReserve, matureSpace);

                if (Heap.verbose()) {
                    Log.println("Evacuate Followers");
                }

                evacuateFollowers(matureSpaceReserve, matureSpace);

                heapScheme.sideTable.restoreAllChunkSlots();

                if (heapScheme.verifyAfterGC()) {
                    verifyHeap("After Compaction");
                }

                if (Heap.verbose()) {
                    Log.println("Reset Nursery Space Allocation Mark");
                }
                InspectableHeapInfo.afterGarbageCollection();
                monitorScheme.afterGarbageCollection();

                if (Heap.verbose()) {
                    printBeltInfo("Nursery Space", nurserySpace);
                    printBeltInfo("Mature Space", matureSpace);
                    Log.println("End Calibration ");
                }

            } else {
                BeltwayHeapScheme.outOfMemory = true;
            }
        }
    }

    static class MinorGCCollector extends BeltwayBA2Collector implements Runnable {
        MinorGCCollector() {
            super("Minor");
        }

        protected void evacuateFollowers() {
            heapScheme.evacuate(nurserySpace, matureSpace);
            // beltwayHeapSchemeBA2.fillLastTLAB(); FIXME: do we need this ?
        }

        public void run() {
            prologue();
            matureSpace.setAllocationMarkSnapshot();

            if (Heap.verbose()) {
                printBeltInfo("Nursery Space", nurserySpace);
                printBeltInfo("Mature Space", matureSpace);
            }

            if (heapScheme.verifyBeforeGC()) {
                verifyHeap("Before minor GC");
            }

            monitorScheme.beforeGarbageCollection();
            heapScheme.scavengeRoot(nurserySpace, matureSpace);

            if (Heap.verbose()) {
                Log.println("Scan Cards");
            }

            heapScheme.scanCardAndEvacuate(matureSpace, nurserySpace);

            if (Heap.verbose()) {
                Log.println("Evacuate Followers");
            }
            evacuateFollowers();

            heapScheme.sideTable.restoreAllChunkSlots();

            if (Heap.verbose()) {
                Log.println("Reset Nursery Space Allocation Mark");
            }
            nurserySpace.resetAllocationMark();
            monitorScheme.afterGarbageCollection();

            if (heapScheme.verifyAfterGC()) {
                verifyHeap("After minor GC");
            }

            InspectableHeapInfo.afterGarbageCollection();

            if (Heap.verbose()) {
                printBeltInfo("Nursery Space", nurserySpace);
                printBeltInfo("Mature Space", matureSpace);
                Log.print("End of Minor Collection: ");
                Log.println(numCollections);
            }
        }
    }

    /**
     * FIXME: revisit.
     */
    static class ParFullGCCollector extends FullGCCollector {

        @Override
        protected void evacuateFollowers(Belt from, Belt to) {
            heapScheme.fillLastTLAB();
            //beltwayHeapSchemeBA2.markSideTableLastTLAB();
            BeltwayHeapScheme.inScavenging = true;
            heapScheme.initializeGCThreads(heapScheme, from, to);
            if (Heap.verbose()) {
                Log.println("Start Threads");
            }

            heapScheme.startGCThreads();
            BeltwayHeapScheme.inScavenging = false;
            if (Heap.verbose()) {
                Log.println("Join Threads");
            }
        }
    }

    /**
     * FIXME: revisit.
     */
    static class ParMinorGCCollector  extends MinorGCCollector {

        @Override
        protected void evacuateFollowers() {
            heapScheme.fillLastTLAB();
            // beltwayHeapSchemeBA2.markSideTableLastTLAB();
            BeltwayHeapScheme.inScavenging = true;
            heapScheme.initializeGCThreads(heapScheme, nurserySpace, matureSpace);
            if (Heap.verbose()) {
                Log.println("Start Threads");
            }

            heapScheme.startGCThreads();
            BeltwayHeapScheme.inScavenging = false;
            if (Heap.verbose()) {
                Log.println("Join Threads");
            }
            heapScheme.evacuate(nurserySpace, matureSpace);
        }
    }
}
