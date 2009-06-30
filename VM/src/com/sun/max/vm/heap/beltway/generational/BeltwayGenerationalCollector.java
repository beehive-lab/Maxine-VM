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

    // /Dependency injection of the corresponding heap scheme
    private static HeapScheme beltwayHeapScheme;

    public static long edenCollections = 0;
    public static long majorCollections = 0;
    public static long toCollections = 0;

    public void setBeltwayHeapScheme(HeapScheme beltwayHeapScheme) {
        BeltwayGenerationalCollector.beltwayHeapScheme = beltwayHeapScheme;
    }

    public HeapScheme getBeltwayHeapScheme() {
        return beltwayHeapScheme;
    }

    public BeltwayGenerationalCollector() {

    }

    @Override
    public void run() {
    }

    private final Runnable majorGC = new Runnable() {

        public void run() {
            majorCollections++;
            if (Heap.verbose()) {
                Log.print("Major Collection: ");
                Log.println(majorCollections);
            }
            final BeltwayHeapSchemeGenerational beltwayHeapSchemeGen = (BeltwayHeapSchemeGenerational) beltwayHeapScheme;
            final Belt matureSpace = beltwayHeapSchemeGen.getMatureSpace();
            final Belt toSpace = beltwayHeapSchemeGen.getToSpace();
            final Belt edenSpace = beltwayHeapSchemeGen.getEdenSpace();

            if (Heap.verbose()) {
                Log.print("Mature Space Start: ");
                Log.println(matureSpace.start());
                Log.print("Mature Space Allocation Mark: ");
                Log.println(matureSpace.getAllocationMark());
                Log.print("Mature Space End: ");
                Log.println(matureSpace.end());
                Log.println("Verify Mature Space ");
            }


            beltwayHeapSchemeGen.getVerifier().verifyHeap(matureSpace.start(), matureSpace.getAllocationMark(), BeltManager.getApplicationHeap());
            TeleHeapInfo.beforeGarbageCollection();
            VMConfiguration.hostOrTarget().monitorScheme().beforeGarbageCollection();

            if (Heap.verbose()) {
                Log.println("Set Eden Expandable ");
            }

            edenSpace.setExpandable(true);

            if (Heap.verbose()) {
                Log.println("Scan roots ");
            }
            beltwayHeapSchemeGen.getRootScannerUpdater().run(matureSpace, edenSpace);

            if (Heap.verbose()) {
                Log.println("Scan Boot");
            }

            beltwayHeapSchemeGen.scanBootHeap(matureSpace, edenSpace);

            if (Heap.verbose()) {
                Log.println("Scan code");
            }
            beltwayHeapSchemeGen.scanCode(matureSpace, edenSpace);

            if (Heap.verbose()) {
                Log.print("Scavenge");
            }

            if (BeltwayConfiguration.parallelScavenging) {
                beltwayHeapSchemeGen.fillLastTLAB();
                beltwayHeapSchemeGen.markSideTableLastTLAB();
                BeltwayHeapScheme.inScavenging = true;
                beltwayHeapSchemeGen.initializeGCThreads(beltwayHeapSchemeGen, matureSpace, toSpace);
                VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();
                if (Heap.verbose()) {
                    Log.println("Start Threads");
                }

                beltwayHeapSchemeGen.startGCThreads();
                BeltwayHeapScheme.inScavenging = false;
                if (Heap.verbose()) {
                    Log.println("Join Threads");
                }

            } else {
                beltwayHeapSchemeGen.linearScanRegionBelt(edenSpace, matureSpace, edenSpace);
                beltwayHeapSchemeGen.fillLastTLAB();
            }

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
            if (Heap.verbose()) {
                Log.println("Scan roots ");
            }
            // Start scanning the reachable objects from my roots.
            beltwayHeapSchemeGen.getRootScannerUpdater().run(edenSpace, matureSpace);
            VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();

            if (Heap.verbose()) {
                Log.println("Scan Boot");
            }

            beltwayHeapSchemeGen.scanBootHeap(edenSpace, matureSpace);

            if (Heap.verbose()) {
                Log.println("Scan code");
            }
            beltwayHeapSchemeGen.scanCode(edenSpace, matureSpace);

            if (Heap.verbose()) {
                Log.println("Move Reachable");
                Log.print("Eden Space Start: ");
                Log.println(edenSpace.start());
                Log.print("Eden Space Allocation Mark: ");
                Log.println(edenSpace.getAllocationMark());
                Log.print("Eden Space End: ");
                Log.println(edenSpace.end());
                Log.print("To Space Start: ");
                Log.println(toSpace.start());
                Log.print("To Space Allocation Mark: ");
                Log.println(toSpace.getAllocationMark());
                Log.print("To Space End: ");
                Log.println(toSpace.end());
                Log.print("Mature Space Start: ");
                Log.println(matureSpace.start());
                Log.print("Mature Space Allocation Mark: ");
                Log.println(matureSpace.getAllocationMark());
                Log.print("Mature Space End: ");
                Log.println(matureSpace.end());
            }
            if (BeltwayConfiguration.parallelScavenging) {
                beltwayHeapSchemeGen.fillLastTLAB();
                beltwayHeapSchemeGen.markSideTableLastTLAB();
                BeltwayHeapScheme.inScavenging = true;

                beltwayHeapSchemeGen.initializeGCThreads(beltwayHeapSchemeGen, edenSpace, matureSpace);
                VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();
                if (Heap.verbose()) {
                    Log.println("Start Threads");
                }

                beltwayHeapSchemeGen.startGCThreads();
                BeltwayHeapScheme.inScavenging = false;
                if (Heap.verbose()) {
                    Log.println("Join Threads");
                }
            } else {
                beltwayHeapSchemeGen.linearScanRegionBelt(matureSpace, edenSpace, matureSpace);
                beltwayHeapSchemeGen.fillLastTLAB();
            }

            if (Heap.verbose()) {
                Log.println("Reset Eden Space ");
            }
            edenSpace.resetAllocationMark();
            edenSpace.setEnd(toSpace.start());
            edenSpace.setExpandable(false);
            TeleHeapInfo.afterGarbageCollection();
            if (Heap.verbose()) {
                Log.print("Eden Space Start: ");
                Log.println(edenSpace.start());
                Log.print("Eden Space Allocation Mark: ");
                Log.println(edenSpace.getAllocationMark());
                Log.print("Eden Space End: ");
                Log.println(edenSpace.end());
                Log.print("To Space Start: ");
                Log.println(toSpace.start());
                Log.print("To Space Allocation Mark: ");
                Log.println(toSpace.getAllocationMark());
                Log.print("To Space End: ");
                Log.println(toSpace.end());
                Log.print("Mature Space Start: ");
                Log.println(matureSpace.start());
                Log.print("Mature Space Allocation Mark: ");
                Log.println(matureSpace.getAllocationMark());
                Log.print("Mature Space End: ");
                Log.println(matureSpace.end());
            }
            if (Heap.verbose()) {
                Log.print("Finished Eden Collection: ");
                Log.println(edenCollections);
            }
        }
    };

    public Runnable getMinorGC() {
        return edenGC;
    }

    public Runnable getMajorGC() {
        return majorGC;
    }

    public Runnable getToGC() {
        return toGC;
    }

    private final Runnable toGC = new Runnable() {

        public void run() {
            toCollections++;
            if (Heap.verbose()) {
                Log.print("To Collection: ");
                Log.println(toCollections);
            }
            final BeltwayHeapSchemeGenerational beltwayHeapSchemeGen = (BeltwayHeapSchemeGenerational) beltwayHeapScheme;
            final Belt matureSpace = beltwayHeapSchemeGen.getMatureSpace();
            final Belt toSpace = beltwayHeapSchemeGen.getToSpace();

            if (Heap.verbose()) {
                Log.print("To Space Start: ");
                Log.println(toSpace.start());
                Log.print("To Space Allocation Mark: ");
                Log.println(toSpace.getAllocationMark());
                Log.print("To Space End: ");
                Log.println(toSpace.end());
                Log.println("Verify To Space: ");
            }

            beltwayHeapSchemeGen.getVerifier().verifyHeap(toSpace.start(), toSpace.getAllocationMark(), BeltManager.getApplicationHeap());
            if (Heap.verbose()) {
                Log.println(" Mature Snapshot ");
            }

            matureSpace.setAllocationMarkSnapshot();
            VMConfiguration.hostOrTarget().monitorScheme().beforeGarbageCollection();

            if (Heap.verbose()) {
                Log.println("Scan Roots ");
            }

            beltwayHeapSchemeGen.getRootScannerUpdater().run(toSpace, matureSpace);

            if (Heap.verbose()) {
                Log.println("Scan Boot Heap");
            }

            beltwayHeapSchemeGen.scanBootHeap(toSpace, matureSpace);

            if (Heap.verbose()) {
                Log.println("Scan Code");
            }

            beltwayHeapSchemeGen.scanCode(toSpace, matureSpace);

            if (Heap.verbose()) {
                Log.println("Scan cards");
            }

            beltwayHeapSchemeGen.scanCards(matureSpace, toSpace, matureSpace);

            if (Heap.verbose()) {
                Log.println("Move Reachable Objects");
            }

            if (BeltwayConfiguration.parallelScavenging) {
                beltwayHeapSchemeGen.fillLastTLAB();
                beltwayHeapSchemeGen.markSideTableLastTLAB();
                BeltwayHeapScheme.inScavenging = true;
                beltwayHeapSchemeGen.initializeGCThreads(beltwayHeapSchemeGen, toSpace, matureSpace);
                VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();
                if (Heap.verbose()) {
                    Log.println("Start Threads");
                }

                beltwayHeapSchemeGen.startGCThreads();
                BeltwayHeapScheme.inScavenging = false;
                if (Heap.verbose()) {
                    Log.println("Join Threads");
                }
            } else {
                beltwayHeapSchemeGen.linearScanRegionBelt(matureSpace, toSpace, matureSpace);
                beltwayHeapSchemeGen.fillLastTLAB();
            }

            BeltwayHeapSchemeGenerational.sideTable.restoreAllChunkSlots();

            // TODO: Delete
            //Debug.println("Wipe To");
            //beltwayHeapSchemeGen.wipeMemory(toSpace);

            toSpace.resetAllocationMark();
            VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();

            if (Heap.verbose()) {
                Log.print("Mature Space Start: ");
                Log.println(matureSpace.start());
                Log.print("Mature Space Allocation Mark: ");
                Log.println(matureSpace.getAllocationMark());
                Log.print("Mature Space End: ");
                Log.println(matureSpace.end());
                Log.println("Verify Mature Space");
            }
            beltwayHeapSchemeGen.getVerifier().verifyHeap(matureSpace.start(), matureSpace.getAllocationMark(), BeltManager.getApplicationHeap());
            TeleHeapInfo.afterGarbageCollection();
            if (Heap.verbose()) {
                Log.print("Finished To Collection: ");
                Log.println(toCollections);
            }
        }
    };

    private final Runnable edenGC = new Runnable() {

        public void run() {
            edenCollections++;
            if (Heap.verbose()) {
                Log.print("Eden Collection: ");
                Log.println(edenCollections);
            }
            final BeltwayHeapSchemeGenerational beltwayHeapSchemeGen = (BeltwayHeapSchemeGenerational) beltwayHeapScheme;

            final Belt matureSpace = beltwayHeapSchemeGen.getMatureSpace();
            final Belt toSpace = beltwayHeapSchemeGen.getToSpace();
            final Belt edenSpace = beltwayHeapSchemeGen.getEdenSpace();

            if (Heap.verbose()) {
                Log.print("Eden Space Start: ");
                Log.println(edenSpace.start());
                Log.print("Eden Space Allocation Mark: ");
                Log.println(edenSpace.getAllocationMark());
                Log.print("Eden Space End: ");
                Log.println(edenSpace.end());
                Log.println("Verify Eden: ");
            }
            beltwayHeapSchemeGen.getVerifier().verifyHeap(edenSpace.start(), edenSpace.getAllocationMark(), BeltManager.getApplicationHeap());

            if (Heap.verbose()) {
                Log.println("To Space Snapshot");
            }
            toSpace.setAllocationMarkSnapshot();
            TeleHeapInfo.beforeGarbageCollection();

            VMConfiguration.hostOrTarget().monitorScheme().beforeGarbageCollection();
            if (Heap.verbose()) {
                Log.println("Scan Roots ");
            }

            beltwayHeapSchemeGen.getRootScannerUpdater().run(edenSpace, toSpace);

            if (Heap.verbose()) {
                Log.println("Scan Boot Heap");
            }

            beltwayHeapSchemeGen.scanBootHeap(edenSpace, toSpace);

            if (Heap.verbose()) {
                Log.println("Scan Code");
            }

            beltwayHeapSchemeGen.scanCode(edenSpace, toSpace);

            if (Heap.verbose()) {
                Log.println("Scan cards");
            }

            beltwayHeapSchemeGen.scanCards(toSpace, edenSpace, toSpace);
            beltwayHeapSchemeGen.scanCards(matureSpace, edenSpace, toSpace);

            if (Heap.verbose()) {
                Log.println("Move Reachable Objects");
            }

            if (BeltwayConfiguration.parallelScavenging) {
                beltwayHeapSchemeGen.fillLastTLAB();
                beltwayHeapSchemeGen.markSideTableLastTLAB();
                BeltwayHeapScheme.inScavenging = true;
                beltwayHeapSchemeGen.initializeGCThreads(beltwayHeapSchemeGen, edenSpace, toSpace);
                VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();
                if (Heap.verbose()) {
                    Log.println("Start Threads");
                }

                beltwayHeapSchemeGen.startGCThreads();
                BeltwayHeapScheme.inScavenging = false;
                if (Heap.verbose()) {
                    Log.println("Join Threads");
                }
            } else {
                beltwayHeapSchemeGen.linearScanRegionBelt(toSpace, edenSpace, toSpace);
                beltwayHeapSchemeGen.fillLastTLAB();
            }

            BeltwayHeapSchemeGenerational.sideTable.restoreAllChunkSlots();

            // TODO: Delete
            //Debug.println("Wipe Eden");
            //beltwayHeapSchemeGen.wipeMemory(edenSpace);

            if (Heap.verbose()) {
                Log.println("Reset Nursery Space Allocation Mark");
            }

            edenSpace.resetAllocationMark();
            VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();

            if (Heap.verbose()) {
                Log.print("To Space Start: ");
                Log.println(toSpace.start());
                Log.print("To Space Allocation Mark: ");
                Log.println(toSpace.getAllocationMark());
                Log.print("To Space End: ");
                Log.println(toSpace.end());
                Log.println("Verfiy To Space: ");
            }

            beltwayHeapSchemeGen.getVerifier().verifyHeap(toSpace.start(), toSpace.getAllocationMark(), BeltManager.getApplicationHeap());
            TeleHeapInfo.afterGarbageCollection();
            if (Heap.verbose()) {
                Log.print("Finished Eden Collection: ");
                Log.println(edenCollections);
            }
        }
    };

}
