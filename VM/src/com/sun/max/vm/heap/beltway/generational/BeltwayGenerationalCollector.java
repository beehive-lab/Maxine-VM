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

/**
 *  Implementation of a three generations collector using beltway.
 *
 * @author Laurent Daynes
 * @author Christos Kotselidis
 */

public class BeltwayGenerationalCollector extends BeltwayCollector {

    public BeltwayGenerationalCollector(String name) {
        super(name);
    }

    protected void parEvacuateFollowers(Belt from, Belt to) {
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

    static class MajorCollector extends BeltwayGenerationalCollector implements Runnable {
        MajorCollector() {
            super("Major");
        }

        private void printSpacesInfo(Belt edenSpace, Belt toSpace, Belt matureSpace) {
            printBeltInfo("Eden Space", edenSpace);
            printBeltInfo("To Space", toSpace);
            printBeltInfo("Mature Space", matureSpace);
        }

        public void run() {
            final BeltwayHeapSchemeGenerational genHeapScheme = (BeltwayHeapSchemeGenerational) heapScheme;
            final Belt matureSpace = genHeapScheme.getMatureSpace();
            final Belt toSpace = genHeapScheme.getToSpace();
            final Belt edenSpace = genHeapScheme.getEdenSpace();
            prologue();

            if (Heap.verbose()) {
                printBeltInfo("Mature Space", matureSpace);
                Log.println("Verify Mature Space ");
            }
            verifyBelt(matureSpace);
            monitorScheme.beforeGarbageCollection();

            if (Heap.verbose()) {
                Log.println("Set Eden Expandable ");
            }
            edenSpace.setExpandable(true);

            genHeapScheme.scavengeRoot(matureSpace, edenSpace);

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
            genHeapScheme.scavengeRoot(edenSpace, matureSpace);

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

    static class ToSpaceCollector  extends BeltwayGenerationalCollector implements Runnable {
        ToSpaceCollector() {
            super("To");
        }
        public void run() {
            final BeltwayHeapSchemeGenerational genHeapScheme = (BeltwayHeapSchemeGenerational) heapScheme;
            final Belt matureSpace = genHeapScheme.getMatureSpace();
            final Belt toSpace = genHeapScheme.getToSpace();
            prologue();

            if (Heap.verbose()) {
                printBeltInfo("To Space", toSpace);
                Log.println("Verify To Space: ");
            }
            verifyBelt(toSpace);
            if (Heap.verbose()) {
                Log.println(" Mature Snapshot ");
            }

            matureSpace.setAllocationMarkSnapshot();
            monitorScheme.beforeGarbageCollection();
            genHeapScheme.scavengeRoot(toSpace, matureSpace);

            if (Heap.verbose()) {
                Log.println("Scan cards");
            }

            genHeapScheme.scanCardAndEvacuate(matureSpace, toSpace);

            evacuateFollowers(toSpace, matureSpace);

            genHeapScheme.sideTable.restoreAllChunkSlots();

            toSpace.resetAllocationMark();
            monitorScheme.afterGarbageCollection();

            if (Heap.verbose()) {
                printBeltInfo("Mature Space", matureSpace);
                Log.println("Verify Mature Space");
            }

            verifyBelt(matureSpace);
            epilogue();
        }
    }

    static class EdenCollector extends BeltwayGenerationalCollector implements Runnable {
        EdenCollector() {
            super("Eden");
        }

        public void run() {
            final BeltwayHeapSchemeGenerational genHeapScheme = (BeltwayHeapSchemeGenerational) heapScheme;
            final Belt matureSpace = genHeapScheme.getMatureSpace();
            final Belt toSpace = genHeapScheme.getToSpace();
            final Belt edenSpace = genHeapScheme.getEdenSpace();
            prologue();

            if (Heap.verbose()) {
                printBeltInfo("Eden Space", edenSpace);
                Log.println("Verify Eden: ");
            }
            verifyBelt(edenSpace);

            if (Heap.verbose()) {
                Log.println("To Space Snapshot");
            }
            toSpace.setAllocationMarkSnapshot();

            monitorScheme.beforeGarbageCollection();
            genHeapScheme.scavengeRoot(edenSpace, toSpace);

            if (Heap.verbose()) {
                Log.println("Scan cards");
            }

            genHeapScheme.scanCardAndEvacuate(toSpace, edenSpace);
            genHeapScheme.scanCardAndEvacuate(matureSpace, edenSpace);

            evacuateFollowers(edenSpace, toSpace);

            genHeapScheme.sideTable.restoreAllChunkSlots();

            if (Heap.verbose()) {
                Log.println("Reset Nursery Space Allocation Mark");
            }

            edenSpace.resetAllocationMark();
            monitorScheme.afterGarbageCollection();

            if (Heap.verbose()) {
                printBeltInfo("To Space", toSpace);
                Log.println("Verfiy To Space: ");
            }

            verifyBelt(toSpace);
            epilogue();
        }
    }

    static class ParEdenCollector extends EdenCollector {
        ParEdenCollector() {
            super();
        }

        @Override
        protected void evacuateFollowers(Belt from, Belt to) {
            parEvacuateFollowers(from, to);
        }
    }

    static class ParToSpaceCollector extends ToSpaceCollector {
        ParToSpaceCollector() {
            super();
        }
        @Override
        protected void evacuateFollowers(Belt from, Belt to) {
            parEvacuateFollowers(from, to);
        }
    }

    static class ParMajorCollector extends MajorCollector {
        ParMajorCollector() {
            super();
        }
        @Override
        protected void evacuateFollowers(Belt from, Belt to) {
            parEvacuateFollowers(from, to);
        }
    }

}
