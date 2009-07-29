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

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.beltway.*;
import com.sun.max.vm.tele.*;

/**
 * An Appel-style generational collector. The heap is divided in two generations, each implemented using a belt.
 * @see BeltwayHeapSchemeBA2
 *
 * @author Christos Kotselidis
 */

public class BeltwayBA2Collector extends BeltwayCollector {

    @Override
    public void run() {
    }

    class BA2Collector {
        long  numCollections = 0;
        final Belt nurserySpace;
        final Belt matureSpace;
        final BeltwayHeapSchemeBA2 heapScheme;
        final String collectorName;

        BA2Collector(String collectorName) {
            heapScheme = (BeltwayHeapSchemeBA2) getBeltwayHeapScheme();
            nurserySpace =  heapScheme.getNurserySpace();
            matureSpace =  heapScheme.getMatureSpace();
            this.collectorName = collectorName;
        }

        void prologue() {
            numCollections++;
            if (Heap.verbose()) {
                Log.print(collectorName);
                Log.print(" Collection: ");
                Log.println(numCollections);
            }
        }
        /**
         * Evacuate remaining objects of the ¨from " belt reachable from the "to" belt.
         */
        protected void evacuateFollowers(Belt from, Belt to) {
            getBeltwayHeapScheme().evacuate(from, to);
            // beltwayHeapSchemeBA2.fillLastTLAB(); FIXME: do we need this ?
        }

        void printBeltInfo(String beltName, Belt belt) {
            Log.print(beltName);
            Log.print(" start: ");
            Log.println(belt.start());
            Log.print(beltName);
            Log.print(" Mark: ");
            Log.println(belt.getAllocationMark());
            Log.print(beltName);
            Log.print(" Space End: ");
            Log.println(belt.end());
        }
    }

    class FullGCCollector extends BA2Collector implements Runnable {
        FullGCCollector() {
            super("Major");
        }

        public void run() {
            prologue();
            if (Heap.verbose()) {
                Log.println("Verify Mature Space: ");
            }
            verifyBelt(matureSpace);
            InspectableHeapInfo.beforeGarbageCollection();

            monitorScheme.beforeGarbageCollection();

            final Pointer matureSpaceEnd = matureSpace.end().asPointer();
            final Belt matureSpaceBeforeAllocation = heapScheme.getBeltManager().getBeltBeforeLastAllocation(matureSpace);
            final Belt matureSpaceReserve = heapScheme.getBeltManager().getRemainingOverlappingBelt(matureSpace);
            matureSpaceReserve.setExpandable(true);
            matureSpaceReserve.setEnd(matureSpaceEnd);

            if (Heap.verbose()) {
                printBeltInfo("matureSpaceBeforeAllocation", matureSpaceBeforeAllocation);
                printBeltInfo("matureSpaceReserve", matureSpaceReserve);
            }

            // Start scanning the reachable objects from my roots.
            heapScheme.scavengeRoot(matureSpaceBeforeAllocation, matureSpaceReserve);

            if (Heap.verbose()) {
                printBeltInfo("matureSpaceReserve", matureSpaceReserve);
                Log.println("Evacuate Followers");
            }
            evacuateFollowers(matureSpaceBeforeAllocation, matureSpaceReserve);

            matureSpaceReserve.setEnd(matureSpaceReserve.getAllocationMark());

            if (Heap.verbose()) {
                printBeltInfo("matureSpaceBeforeAllocation", matureSpaceBeforeAllocation);
                printBeltInfo("matureSpaceReserve", matureSpaceReserve);
            }

            matureSpace.resetAllocationMark();
            matureSpace.setEnd(matureSpaceEnd);
            if (Heap.verbose()) {
                printBeltInfo("Mature Space", matureSpace);
                Log.print("Mature Space Reserve Size: ");
                Log.println(matureSpaceReserve.size().toLong());
                Log.print("Configuration usable memory Size ");
                Log.println(BeltwayConfiguration.getUsableMemory().toLong());
            }

            if (matureSpaceReserve.size().lessEqual(BeltwayConfiguration.getUsableMemory())) {
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

    class MinorGCCollector extends BA2Collector implements Runnable {
        MinorGCCollector() {
            super("Minor");
        }

        protected void evacuateFollowers() {
            getBeltwayHeapScheme().evacuate(nurserySpace, matureSpace);
            // beltwayHeapSchemeBA2.fillLastTLAB(); FIXME: do we need this ?
        }

        public void run() {
            prologue();
            matureSpace.setAllocationMarkSnapshot();

            if (Heap.verbose()) {
                printBeltInfo("Nursery Space", nurserySpace);
                printBeltInfo("Mature Space", matureSpace);
                Log.println("Verify Nursery:");
            }
            verifyBelt(nurserySpace);
            InspectableHeapInfo.beforeGarbageCollection();
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

            if (Heap.verbose()) {
                printBeltInfo("Mature Space", matureSpace);
                Log.println("Verify Mature Space");
            }

            verifyBelt(matureSpace);
            InspectableHeapInfo.afterGarbageCollection();

            if (Heap.verbose()) {
                printBeltInfo("Nursery Space", nurserySpace);
                printBeltInfo("Mature Space", matureSpace);
                Log.print("End of Minor Collection: ");
                Log.println(numCollections);
            }
        }
    }

    class ParFullGCCollector extends FullGCCollector {

        @Override
        protected void evacuateFollowers(Belt from, Belt to) {
            final BeltwayHeapSchemeBA2 beltwayHeapSchemeBA2 = (BeltwayHeapSchemeBA2) getBeltwayHeapScheme();
            beltwayHeapSchemeBA2.fillLastTLAB();
            //beltwayHeapSchemeBA2.markSideTableLastTLAB();
            BeltwayHeapScheme.inScavenging = true;
            beltwayHeapSchemeBA2.initializeGCThreads(beltwayHeapSchemeBA2, from, to);
            VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();
            if (Heap.verbose()) {
                Log.println("Start Threads");
            }

            beltwayHeapSchemeBA2.startGCThreads();
            BeltwayHeapScheme.inScavenging = false;
            if (Heap.verbose()) {
                Log.println("Join Threads");
            }
        }
    }

    class ParMinorGCCollector  extends MinorGCCollector {
        final BeltwayHeapSchemeBA2 beltwayHeapSchemeBA2 = (BeltwayHeapSchemeBA2) getBeltwayHeapScheme();

        @Override
        protected void evacuateFollowers() {
            beltwayHeapSchemeBA2.fillLastTLAB();
            // beltwayHeapSchemeBA2.markSideTableLastTLAB();
            BeltwayHeapScheme.inScavenging = true;
            beltwayHeapSchemeBA2.initializeGCThreads(beltwayHeapSchemeBA2, nurserySpace, matureSpace);
            VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();
            if (Heap.verbose()) {
                Log.println("Start Threads");
            }

            beltwayHeapSchemeBA2.startGCThreads();
            BeltwayHeapScheme.inScavenging = false;
            if (Heap.verbose()) {
                Log.println("Join Threads");
            }
            getBeltwayHeapScheme().evacuate(nurserySpace, matureSpace);
        }
    }

    final Runnable minorGCCollector;
    final Runnable fullGCCollector;

    public Runnable getMinorGC() {
        return minorGCCollector;
    }

    public Runnable getMajorGC() {
        return fullGCCollector;
    }

    public BeltwayBA2Collector() {
        minorGCCollector =  BeltwayConfiguration.parallelScavenging ? new ParMinorGCCollector() : new MinorGCCollector();
        fullGCCollector =  BeltwayConfiguration.parallelScavenging ? new ParFullGCCollector() : new FullGCCollector();
    }
}
