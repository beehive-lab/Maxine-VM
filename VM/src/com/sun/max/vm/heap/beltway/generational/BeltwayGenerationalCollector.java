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

            if (Heap.verbose()) {
                Log.print("Mature Space Start: ");
                Log.println(beltwayHeapSchemeGen.getMatureSpace().start());
                Log.print("Mature Space Allocation Mark: ");
                Log.println(beltwayHeapSchemeGen.getMatureSpace().getAllocationMark());
                Log.print("Mature Space End: ");
                Log.println(beltwayHeapSchemeGen.getMatureSpace().end());
                Log.println("Verify Mature Space ");
            }

            beltwayHeapSchemeGen.getVerifier().verifyHeap(beltwayHeapSchemeGen.getMatureSpace().start(), beltwayHeapSchemeGen.getMatureSpace().getAllocationMark(), BeltManager.getApplicationHeap());
            TeleHeapInfo.beforeGarbageCollection();
            VMConfiguration.hostOrTarget().monitorScheme().beforeGarbageCollection();

            if (Heap.verbose()) {
                Log.println("Set Eden Expandable ");
            }

            beltwayHeapSchemeGen.getEdenSpace().setExpandable(true);

            if (Heap.verbose()) {
                Log.println("Scan roots ");
            }

            beltwayHeapSchemeGen.getRootScannerUpdater().setFromSpace(beltwayHeapSchemeGen.getMatureSpace());
            beltwayHeapSchemeGen.getRootScannerUpdater().setToSpace(beltwayHeapSchemeGen.getEdenSpace());
            beltwayHeapSchemeGen.getRootScannerUpdater().run();

            if (Heap.verbose()) {
                Log.println("Scan Boot");
            }

            beltwayHeapSchemeGen.scanBootHeap(beltwayHeapSchemeGen.getMatureSpace(), beltwayHeapSchemeGen.getEdenSpace());

            if (Heap.verbose()) {
                Log.println("Scan code");
            }
            beltwayHeapSchemeGen.scanCode(beltwayHeapSchemeGen.getMatureSpace(), beltwayHeapSchemeGen.getEdenSpace());

            if (Heap.verbose()) {
                Log.print("Scavenge");
            }

            if (BeltwayConfiguration.parallelScavenging) {
                beltwayHeapSchemeGen.fillLastTLAB();
                beltwayHeapSchemeGen.markSideTableLastTLAB();
                BeltwayHeapScheme.inScavening = true;
                beltwayHeapSchemeGen.initializeGCThreads(beltwayHeapSchemeGen, beltwayHeapSchemeGen.getMatureSpace(), beltwayHeapSchemeGen.getToSpace());
                VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();
                if (Heap.verbose()) {
                    Log.println("Start Threads");
                }

                beltwayHeapSchemeGen.startGCThreads();
                BeltwayHeapScheme.inScavening = false;
                if (Heap.verbose()) {
                    Log.println("Join Threads");
                }

            } else {
                beltwayHeapSchemeGen.linearScanRegionBelt(beltwayHeapSchemeGen.getEdenSpace(), beltwayHeapSchemeGen.getMatureSpace(), beltwayHeapSchemeGen.getEdenSpace());
                beltwayHeapSchemeGen.fillLastTLAB();
            }

            if (Heap.verbose()) {
                Log.println("Mature Reset Allocation Mark");
            }
            beltwayHeapSchemeGen.getMatureSpace().resetAllocationMark();

            if (Heap.verbose()) {
                Log.println("Compaction ");
                Log.println("Equalize edens end ");
            }

            if (beltwayHeapSchemeGen.getEdenSpace().getAllocationMark().greaterEqual(beltwayHeapSchemeGen.getToSpace().end())) {
                if (Heap.verbose()) {
                    Log.println("The living objects size is equal greater than the copy reserve");
                }
                throw BeltwayHeapScheme.outOfMemoryError;
            }
            beltwayHeapSchemeGen.getEdenSpace().setEnd(beltwayHeapSchemeGen.getEdenSpace().getAllocationMark());
            if (Heap.verbose()) {
                Log.println("Scan roots ");
            }
            // Start scanning the reachable objects from my roots.
            beltwayHeapSchemeGen.getRootScannerUpdater().setToSpace(beltwayHeapSchemeGen.getMatureSpace());
            beltwayHeapSchemeGen.getRootScannerUpdater().setFromSpace(beltwayHeapSchemeGen.getEdenSpace());
            beltwayHeapSchemeGen.getRootScannerUpdater().run();
            VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();

            if (Heap.verbose()) {
                Log.println("Scan Boot");
            }

            beltwayHeapSchemeGen.scanBootHeap(beltwayHeapSchemeGen.getEdenSpace(), beltwayHeapSchemeGen.getMatureSpace());

            if (Heap.verbose()) {
                Log.println("Scan code");
            }
            beltwayHeapSchemeGen.scanCode(beltwayHeapSchemeGen.getEdenSpace(), beltwayHeapSchemeGen.getMatureSpace());

            if (Heap.verbose()) {
                Log.println("Move Reachable");
                Log.print("Eden Space Start: ");
                Log.println(beltwayHeapSchemeGen.getEdenSpace().start());
                Log.print("Eden Space Allocation Mark: ");
                Log.println(beltwayHeapSchemeGen.getEdenSpace().getAllocationMark());
                Log.print("Eden Space End: ");
                Log.println(beltwayHeapSchemeGen.getEdenSpace().end());
                Log.print("To Space Start: ");
                Log.println(beltwayHeapSchemeGen.getToSpace().start());
                Log.print("To Space Allocation Mark: ");
                Log.println(beltwayHeapSchemeGen.getToSpace().getAllocationMark());
                Log.print("To Space End: ");
                Log.println(beltwayHeapSchemeGen.getToSpace().end());
                Log.print("Mature Space Start: ");
                Log.println(beltwayHeapSchemeGen.getMatureSpace().start());
                Log.print("Mature Space Allocation Mark: ");
                Log.println(beltwayHeapSchemeGen.getMatureSpace().getAllocationMark());
                Log.print("Mature Space End: ");
                Log.println(beltwayHeapSchemeGen.getMatureSpace().end());
            }
            if (BeltwayConfiguration.parallelScavenging) {
                beltwayHeapSchemeGen.fillLastTLAB();
                beltwayHeapSchemeGen.markSideTableLastTLAB();
                BeltwayHeapScheme.inScavening = true;
                beltwayHeapSchemeGen.initializeGCThreads(beltwayHeapSchemeGen, beltwayHeapSchemeGen.getEdenSpace(), beltwayHeapSchemeGen.getMatureSpace());
                VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();
                if (Heap.verbose()) {
                    Log.println("Start Threads");
                }

                beltwayHeapSchemeGen.startGCThreads();
                BeltwayHeapScheme.inScavening = false;
                if (Heap.verbose()) {
                    Log.println("Join Threads");
                }
            } else {
                beltwayHeapSchemeGen.linearScanRegionBelt(beltwayHeapSchemeGen.getMatureSpace(), beltwayHeapSchemeGen.getEdenSpace(), beltwayHeapSchemeGen.getMatureSpace());
                beltwayHeapSchemeGen.fillLastTLAB();
            }

            if (Heap.verbose()) {
                Log.println("Reset Eden Space ");
            }
            beltwayHeapSchemeGen.getEdenSpace().resetAllocationMark();
            beltwayHeapSchemeGen.getEdenSpace().setEnd(beltwayHeapSchemeGen.getToSpace().start());
            beltwayHeapSchemeGen.getEdenSpace().setExpandable(false);
            TeleHeapInfo.afterGarbageCollection();
            if (Heap.verbose()) {
                Log.print("Eden Space Start: ");
                Log.println(beltwayHeapSchemeGen.getEdenSpace().start());
                Log.print("Eden Space Allocation Mark: ");
                Log.println(beltwayHeapSchemeGen.getEdenSpace().getAllocationMark());
                Log.print("Eden Space End: ");
                Log.println(beltwayHeapSchemeGen.getEdenSpace().end());
                Log.print("To Space Start: ");
                Log.println(beltwayHeapSchemeGen.getToSpace().start());
                Log.print("To Space Allocation Mark: ");
                Log.println(beltwayHeapSchemeGen.getToSpace().getAllocationMark());
                Log.print("To Space End: ");
                Log.println(beltwayHeapSchemeGen.getToSpace().end());
                Log.print("Mature Space Start: ");
                Log.println(beltwayHeapSchemeGen.getMatureSpace().start());
                Log.print("Mature Space Allocation Mark: ");
                Log.println(beltwayHeapSchemeGen.getMatureSpace().getAllocationMark());
                Log.print("Mature Space End: ");
                Log.println(beltwayHeapSchemeGen.getMatureSpace().end());
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

            if (Heap.verbose()) {
                Log.print("To Space Start: ");
                Log.println(beltwayHeapSchemeGen.getToSpace().start());
                Log.print("To Space Allocation Mark: ");
                Log.println(beltwayHeapSchemeGen.getToSpace().getAllocationMark());
                Log.print("To Space End: ");
                Log.println(beltwayHeapSchemeGen.getToSpace().end());
                Log.println("Verify To Space: ");
            }

            beltwayHeapSchemeGen.getVerifier().verifyHeap(beltwayHeapSchemeGen.getToSpace().start(), beltwayHeapSchemeGen.getToSpace().getAllocationMark(), BeltManager.getApplicationHeap());
            if (Heap.verbose()) {
                Log.println(" Mature Snapshot ");
            }

            beltwayHeapSchemeGen.getMatureSpace().setAllocationMarkSnapshot();
            VMConfiguration.hostOrTarget().monitorScheme().beforeGarbageCollection();

            if (Heap.verbose()) {
                Log.println("Scan Roots ");
            }
            beltwayHeapSchemeGen.getRootScannerUpdater().setFromSpace(beltwayHeapSchemeGen.getToSpace());
            beltwayHeapSchemeGen.getRootScannerUpdater().setToSpace(beltwayHeapSchemeGen.getMatureSpace());
            beltwayHeapSchemeGen.getRootScannerUpdater().run();

            if (Heap.verbose()) {
                Log.println("Scan Boot Heap");
            }

            beltwayHeapSchemeGen.scanBootHeap(beltwayHeapSchemeGen.getToSpace(), beltwayHeapSchemeGen.getMatureSpace());

            if (Heap.verbose()) {
                Log.println("Scan Code");
            }

            beltwayHeapSchemeGen.scanCode(beltwayHeapSchemeGen.getToSpace(), beltwayHeapSchemeGen.getMatureSpace());

            if (Heap.verbose()) {
                Log.println("Scan cards");
            }

            beltwayHeapSchemeGen.scanCards(beltwayHeapSchemeGen.getMatureSpace(), beltwayHeapSchemeGen.getToSpace(), beltwayHeapSchemeGen.getMatureSpace());

            if (Heap.verbose()) {
                Log.println("Move Reachable Objects");
            }

            if (BeltwayConfiguration.parallelScavenging) {
                beltwayHeapSchemeGen.fillLastTLAB();
                beltwayHeapSchemeGen.markSideTableLastTLAB();
                BeltwayHeapScheme.inScavening = true;
                beltwayHeapSchemeGen.initializeGCThreads(beltwayHeapSchemeGen, beltwayHeapSchemeGen.getToSpace(), beltwayHeapSchemeGen.getMatureSpace());
                VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();
                if (Heap.verbose()) {
                    Log.println("Start Threads");
                }

                beltwayHeapSchemeGen.startGCThreads();
                BeltwayHeapScheme.inScavening = false;
                if (Heap.verbose()) {
                    Log.println("Join Threads");
                }
            } else {
                beltwayHeapSchemeGen.linearScanRegionBelt(beltwayHeapSchemeGen.getMatureSpace(), beltwayHeapSchemeGen.getToSpace(), beltwayHeapSchemeGen.getMatureSpace());
                beltwayHeapSchemeGen.fillLastTLAB();
            }

            BeltwayHeapSchemeGenerational.sideTable.restoreAllChunkSlots();

            // TODO: Delete
            //Debug.println("Wipe To");
            //beltwayHeapSchemeGen.wipeMemory(beltwayHeapSchemeGen.getToSpace());

            beltwayHeapSchemeGen.getToSpace().resetAllocationMark();
            VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();

            if (Heap.verbose()) {
                Log.print("Mature Space Start: ");
                Log.println(beltwayHeapSchemeGen.getMatureSpace().start());
                Log.print("Mature Space Allocation Mark: ");
                Log.println(beltwayHeapSchemeGen.getMatureSpace().getAllocationMark());
                Log.print("Mature Space End: ");
                Log.println(beltwayHeapSchemeGen.getMatureSpace().end());
                Log.println("Verify Mature Space");
            }
            beltwayHeapSchemeGen.getVerifier().verifyHeap(beltwayHeapSchemeGen.getMatureSpace().start(), beltwayHeapSchemeGen.getMatureSpace().getAllocationMark(), BeltManager.getApplicationHeap());
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

            if (Heap.verbose()) {
                Log.print("Eden Space Start: ");
                Log.println(beltwayHeapSchemeGen.getEdenSpace().start());
                Log.print("Eden Space Allocation Mark: ");
                Log.println(beltwayHeapSchemeGen.getEdenSpace().getAllocationMark());
                Log.print("Eden Space End: ");
                Log.println(beltwayHeapSchemeGen.getEdenSpace().end());
                Log.println("Verify Eden: ");
            }
            beltwayHeapSchemeGen.getVerifier().verifyHeap(beltwayHeapSchemeGen.getEdenSpace().start(), beltwayHeapSchemeGen.getEdenSpace().getAllocationMark(), BeltManager.getApplicationHeap());

            if (Heap.verbose()) {
                Log.println("To Space Snapshot");
            }
            beltwayHeapSchemeGen.getToSpace().setAllocationMarkSnapshot();
            TeleHeapInfo.beforeGarbageCollection();

            VMConfiguration.hostOrTarget().monitorScheme().beforeGarbageCollection();
            if (Heap.verbose()) {
                Log.println("Scan Roots ");
            }
            beltwayHeapSchemeGen.getRootScannerUpdater().setFromSpace(beltwayHeapSchemeGen.getEdenSpace());
            beltwayHeapSchemeGen.getRootScannerUpdater().setToSpace(beltwayHeapSchemeGen.getToSpace());
            beltwayHeapSchemeGen.getRootScannerUpdater().run();

            if (Heap.verbose()) {
                Log.println("Scan Boot Heap");
            }

            beltwayHeapSchemeGen.scanBootHeap(beltwayHeapSchemeGen.getEdenSpace(), beltwayHeapSchemeGen.getToSpace());

            if (Heap.verbose()) {
                Log.println("Scan Code");
            }

            beltwayHeapSchemeGen.scanCode(beltwayHeapSchemeGen.getEdenSpace(), beltwayHeapSchemeGen.getToSpace());

            if (Heap.verbose()) {
                Log.println("Scan cards");
            }

            beltwayHeapSchemeGen.scanCards(beltwayHeapSchemeGen.getToSpace(), beltwayHeapSchemeGen.getEdenSpace(), beltwayHeapSchemeGen.getToSpace());
            beltwayHeapSchemeGen.scanCards(beltwayHeapSchemeGen.getMatureSpace(), beltwayHeapSchemeGen.getEdenSpace(), beltwayHeapSchemeGen.getToSpace());

            if (Heap.verbose()) {
                Log.println("Move Reachable Objects");
            }

            if (BeltwayConfiguration.parallelScavenging) {
                beltwayHeapSchemeGen.fillLastTLAB();
                beltwayHeapSchemeGen.markSideTableLastTLAB();
                BeltwayHeapScheme.inScavening = true;
                beltwayHeapSchemeGen.initializeGCThreads(beltwayHeapSchemeGen, beltwayHeapSchemeGen.getEdenSpace(), beltwayHeapSchemeGen.getToSpace());
                VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();
                if (Heap.verbose()) {
                    Log.println("Start Threads");
                }

                beltwayHeapSchemeGen.startGCThreads();
                BeltwayHeapScheme.inScavening = false;
                if (Heap.verbose()) {
                    Log.println("Join Threads");
                }
            } else {
                beltwayHeapSchemeGen.linearScanRegionBelt(beltwayHeapSchemeGen.getToSpace(), beltwayHeapSchemeGen.getEdenSpace(), beltwayHeapSchemeGen.getToSpace());
                beltwayHeapSchemeGen.fillLastTLAB();
            }

            BeltwayHeapSchemeGenerational.sideTable.restoreAllChunkSlots();

            // TODO: Delete
            //Debug.println("Wipe Eden");
            //beltwayHeapSchemeGen.wipeMemory(beltwayHeapSchemeGen.getEdenSpace());

            if (Heap.verbose()) {
                Log.println("Reset Nursery Space Allocation Mark");
            }

            beltwayHeapSchemeGen.getEdenSpace().resetAllocationMark();
            VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();

            if (Heap.verbose()) {
                Log.print("To Space Start: ");
                Log.println(beltwayHeapSchemeGen.getToSpace().start());
                Log.print("To Space Allocation Mark: ");
                Log.println(beltwayHeapSchemeGen.getToSpace().getAllocationMark());
                Log.print("To Space End: ");
                Log.println(beltwayHeapSchemeGen.getToSpace().end());
                Log.println("Verfiy To Space: ");
            }

            beltwayHeapSchemeGen.getVerifier().verifyHeap(beltwayHeapSchemeGen.getToSpace().start(), beltwayHeapSchemeGen.getToSpace().getAllocationMark(), BeltManager.getApplicationHeap());
            TeleHeapInfo.afterGarbageCollection();
            if (Heap.verbose()) {
                Log.print("Finished Eden Collection: ");
                Log.println(edenCollections);
            }
        }
    };

}
