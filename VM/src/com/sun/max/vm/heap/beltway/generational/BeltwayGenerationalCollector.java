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

import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.beltway.*;
import com.sun.max.vm.tele.*;

/**
 * @author Christos Kotselidis
 */

public class BeltwayGenerationalCollector extends BeltwayCollector {

    public static long edenCollections = 0;
    public static long majorCollections = 0;
    public static long toCollections = 0;

    @Override
    public void run() {
    }

    class GenCollector {
        long numCollections = 0;
        final String collectorName;
        GenCollector(String name) {
            collectorName = name;
        }

        protected void printSpaceDebugInfo(String name, Belt space) {
            Log.print(name + " Space Start: ");
            Log.println(space.start());
            Log.print(name + " Space Allocation Mark: ");
            Log.println(space.getAllocationMark());
            Log.print(name + " Space End: ");
            Log.println(space.end());
        }

        protected void evacuateFollowers(Belt from, Belt to) {
            getBeltwayHeapScheme().evacuate(from, to);
            // beltwayHeapSchemeGen.fillLastTLAB();
        }

        protected void parEvacuateFollowers(Belt from, Belt to) {
            BeltwayHeapScheme heapScheme = getBeltwayHeapScheme();
            /*               heapScheme.fillLastTLAB();
                heapScheme.markSideTableLastTLAB();
             */
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

        protected void prologue() {
            numCollections++;
            InspectableHeapInfo.beforeGarbageCollection();

            if (Heap.verbose()) {
                Log.print(collectorName + " Collection: ");
                Log.println(numCollections);
            }
        }

        protected void epilogue() {
            if (Heap.verbose()) {
                Log.print("Finished " + collectorName + " Collection: ");
                Log.println(numCollections);
            }
            InspectableHeapInfo.afterGarbageCollection();
        }
    }

    class MajorCollector extends GenCollector implements Runnable {
        MajorCollector() {
            super("Major");
        }

        private void printSpacesInfo(Belt edenSpace, Belt toSpace, Belt matureSpace) {
            printSpaceDebugInfo("Eden", edenSpace);
            printSpaceDebugInfo("To", toSpace);
            printSpaceDebugInfo("Mature", matureSpace);
        }

        public void run() {
            final BeltwayHeapSchemeGenerational heapScheme = (BeltwayHeapSchemeGenerational) getBeltwayHeapScheme();
            final Belt matureSpace = heapScheme.getMatureSpace();
            final Belt toSpace = heapScheme.getToSpace();
            final Belt edenSpace = heapScheme.getEdenSpace();
            prologue();

            if (Heap.verbose()) {
                printSpaceDebugInfo("Mature", matureSpace);
                Log.println("Verify Mature Space ");
            }
            verifyBelt(matureSpace);
            monitorScheme.beforeGarbageCollection();

            if (Heap.verbose()) {
                Log.println("Set Eden Expandable ");
            }
            edenSpace.setExpandable(true);

            heapScheme.scavengeRoot(matureSpace, edenSpace);

            if (Heap.verbose()) {
                Log.print("Evacuate Followers");
            }

            evacuateFollowers(matureSpace, edenSpace);

            if (Heap.verbose()) {
                Log.println("Mature Reset Allocation Mark");
            }
            matureSpace.resetAllocationMark();

            if (Heap.verbose()) {
                Log.println("Compaction ");
                Log.println("Equalize edens end ");
            }

            if (edenSpace.getAllocationMark().greaterEqual(toSpace.end())) {
                if (Heap.verbose()) {
                    Log.println("The living objects size is equal greater than the copy reserve");
                }
                throw BeltwayHeapScheme.outOfMemoryError;
            }
            edenSpace.setEnd(edenSpace.getAllocationMark());
            heapScheme.scavengeRoot(edenSpace, matureSpace);

            if (Heap.verbose()) {
                Log.println("Move Reachable");
                printSpacesInfo(edenSpace, toSpace, matureSpace);
            }
            evacuateFollowers(edenSpace, matureSpace);

            if (Heap.verbose()) {
                Log.println("Reset Eden Space ");
            }
            edenSpace.resetAllocationMark();
            edenSpace.setEnd(toSpace.start());
            edenSpace.setExpandable(false);
            monitorScheme.afterGarbageCollection();

            if (Heap.verbose()) {
                printSpacesInfo(edenSpace, toSpace, matureSpace);
            }
            epilogue();
        }
    }

    class ToSpaceCollector  extends GenCollector implements Runnable {
        ToSpaceCollector() {
            super("To");
        }
        public void run() {
            final BeltwayHeapSchemeGenerational heapScheme = (BeltwayHeapSchemeGenerational) getBeltwayHeapScheme();
            final Belt matureSpace = heapScheme.getMatureSpace();
            final Belt toSpace = heapScheme.getToSpace();
            prologue();

            if (Heap.verbose()) {
                printSpaceDebugInfo("To", toSpace);
                Log.println("Verify To Space: ");
            }
            verifyBelt(toSpace);
            if (Heap.verbose()) {
                Log.println(" Mature Snapshot ");
            }

            matureSpace.setAllocationMarkSnapshot();
            monitorScheme.beforeGarbageCollection();
            heapScheme.scavengeRoot(toSpace, matureSpace);

            if (Heap.verbose()) {
                Log.println("Scan cards");
            }

            heapScheme.scanCardAndEvacuate(matureSpace, toSpace);

            if (Heap.verbose()) {
                Log.println("Promote Reachable Objects");
            }

            evacuateFollowers(toSpace, matureSpace);

            heapScheme.sideTable.restoreAllChunkSlots();

            toSpace.resetAllocationMark();
            monitorScheme.afterGarbageCollection();

            if (Heap.verbose()) {
                printSpaceDebugInfo("Mature", matureSpace);
                Log.println("Verify Mature Space");
            }

            verifyBelt(matureSpace);
            epilogue();
        }
    }

    class EdenCollector extends GenCollector implements Runnable {
        EdenCollector() {
            super("Eden");
        }

        public void run() {
            final BeltwayHeapSchemeGenerational heapScheme = (BeltwayHeapSchemeGenerational) getBeltwayHeapScheme();
            final Belt matureSpace = heapScheme.getMatureSpace();
            final Belt toSpace = heapScheme.getToSpace();
            final Belt edenSpace = heapScheme.getEdenSpace();
            prologue();

            if (Heap.verbose()) {
                printSpaceDebugInfo("Eden", edenSpace);
                Log.println("Verify Eden: ");
            }
            verifyBelt(edenSpace);

            if (Heap.verbose()) {
                Log.println("To Space Snapshot");
            }
            toSpace.setAllocationMarkSnapshot();

            monitorScheme.beforeGarbageCollection();
            heapScheme.scavengeRoot(edenSpace, toSpace);

            if (Heap.verbose()) {
                Log.println("Scan cards");
            }

            heapScheme.scanCardAndEvacuate(toSpace, edenSpace);
            heapScheme.scanCardAndEvacuate(matureSpace, edenSpace);

            if (Heap.verbose()) {
                Log.println("Evacuate Followers");
            }
            evacuateFollowers(edenSpace, toSpace);

            heapScheme.sideTable.restoreAllChunkSlots();

            if (Heap.verbose()) {
                Log.println("Reset Nursery Space Allocation Mark");
            }

            edenSpace.resetAllocationMark();
            monitorScheme.afterGarbageCollection();

            if (Heap.verbose()) {
                printSpaceDebugInfo("To", toSpace);
                Log.println("Verfiy To Space: ");
            }

            verifyBelt(toSpace);
            epilogue();
        }
    }

    class ParEdenCollector extends EdenCollector {
        @Override
        protected void evacuateFollowers(Belt from, Belt to) {
            parEvacuateFollowers(from, to);
        }
    }

    class ParToSpaceCollector extends ToSpaceCollector {
        @Override
        protected void evacuateFollowers(Belt from, Belt to) {
            parEvacuateFollowers(from, to);
        }
    }

    class ParMajorCollector extends MajorCollector {
        @Override
        protected void evacuateFollowers(Belt from, Belt to) {
            parEvacuateFollowers(from, to);
        }
    }

    private final Runnable edenGC;
    private final Runnable toGC;
    private final Runnable majorGC;

    public Runnable getMinorGC() {
        return edenGC;
    }

    public Runnable getMajorGC() {
        return majorGC;
    }

    public Runnable getToGC() {
        return toGC;
    }

    public BeltwayGenerationalCollector() {
        if (BeltwayConfiguration.parallelScavenging) {
            edenGC = new ParEdenCollector();
            toGC = new ParToSpaceCollector();
            majorGC = new ParMajorCollector();
        } else {
            edenGC = new EdenCollector();
            toGC = new ToSpaceCollector();
            majorGC = new MajorCollector();
        }
    }
}
