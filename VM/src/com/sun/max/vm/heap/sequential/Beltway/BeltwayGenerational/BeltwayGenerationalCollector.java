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
package com.sun.max.vm.heap.sequential.Beltway.BeltwayGenerational;

import com.sun.max.vm.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.sequential.Beltway.*;
import com.sun.max.vm.tele.*;

/**
 * @author Christos Kotselidis
 */

public class BeltwayGenerationalCollector extends BeltwayCollector {

    // /Dependency injection of the corresponding heap scheme
    private static BeltwayHeapScheme _beltwayHeapScheme;

    public static long _edenCollections = 0;
    public static long _majorCollections = 0;
    public static long _toCollections = 0;

    public void setBeltwayHeapScheme(BeltwayHeapScheme beltwayHeapScheme) {
        _beltwayHeapScheme = beltwayHeapScheme;
    }

    public BeltwayHeapScheme getBeltwayHeapScheme() {
        return _beltwayHeapScheme;
    }

    public BeltwayGenerationalCollector() {

    }

    @Override
    public void run() {
    }

    private final Runnable _majorGC = new Runnable() {

        public void run() {
            _majorCollections++;
            if (Heap.verbose()) {
                Debug.print("Major Collection: ");
                Debug.println(_majorCollections);
            }
            final BeltwayHeapSchemeGenerational beltwayHeapSchemeGen = (BeltwayHeapSchemeGenerational) _beltwayHeapScheme;

            if (Heap.verbose()) {
                Debug.print("Mature Space Start: ");
                Debug.println(beltwayHeapSchemeGen.getMatureSpace().start());
                Debug.print("Mature Space Allocation Mark: ");
                Debug.println(beltwayHeapSchemeGen.getMatureSpace().getAllocationMark());
                Debug.print("Mature Space End: ");
                Debug.println(beltwayHeapSchemeGen.getMatureSpace().end());
                Debug.println("Verify Mature Space ");
            }

            beltwayHeapSchemeGen.getVerifier().verifyHeap(beltwayHeapSchemeGen.getMatureSpace().start(), beltwayHeapSchemeGen.getMatureSpace().getAllocationMark(), BeltManager.getApplicationHeap());
            TeleHeap.beforeGarbageCollection();
            VMConfiguration.hostOrTarget().monitorScheme().beforeGarbageCollection();

            if (Heap.verbose()) {
                Debug.println("Set Eden Expandable ");
            }

            beltwayHeapSchemeGen.getEdenSpace().setExpandable(true);

            if (Heap.verbose()) {
                Debug.println("Scan roots ");
            }

            beltwayHeapSchemeGen.getRootScannerUpdater().setFromSpace(beltwayHeapSchemeGen.getMatureSpace());
            beltwayHeapSchemeGen.getRootScannerUpdater().setToSpace(beltwayHeapSchemeGen.getEdenSpace());
            beltwayHeapSchemeGen.getRootScannerUpdater().run();

            if (Heap.verbose()) {
                Debug.println("Scan Boot");
            }

            beltwayHeapSchemeGen.scanBootHeap(beltwayHeapSchemeGen.getMatureSpace(), beltwayHeapSchemeGen.getEdenSpace());

            if (Heap.verbose()) {
                Debug.println("Scan code");
            }
            beltwayHeapSchemeGen.scanCode(beltwayHeapSchemeGen.getMatureSpace(), beltwayHeapSchemeGen.getEdenSpace());

            if (Heap.verbose()) {
                Debug.print("Scavenge");
            }

            if (BeltwayConfiguration._parallelScavenging) {
                beltwayHeapSchemeGen.fillLastTLAB();
                beltwayHeapSchemeGen.markSideTableLastTLAB();
                BeltwayHeapScheme._inScavening = true;
                beltwayHeapSchemeGen.initializeGCThreads(beltwayHeapSchemeGen, beltwayHeapSchemeGen.getMatureSpace(), beltwayHeapSchemeGen.getToSpace());
                VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();
                if (Heap.verbose()) {
                    Debug.println("Start Threads");
                }

                beltwayHeapSchemeGen.startGCThreads();
                BeltwayHeapScheme._inScavening = false;
                if (Heap.verbose()) {
                    Debug.println("Join Threads");
                }

            } else {
                beltwayHeapSchemeGen.linearScanRegionBelt(beltwayHeapSchemeGen.getEdenSpace(), beltwayHeapSchemeGen.getMatureSpace(), beltwayHeapSchemeGen.getEdenSpace());
                beltwayHeapSchemeGen.fillLastTLAB();
            }

            if (Heap.verbose()) {
                Debug.println("Mature Reset Allocation Mark");
            }
            beltwayHeapSchemeGen.getMatureSpace().resetAllocationMark();

            if (Heap.verbose()) {
                Debug.println("Compaction ");
                Debug.println("Equalize edens end ");
            }

            if (beltwayHeapSchemeGen.getEdenSpace().getAllocationMark().greaterEqual(beltwayHeapSchemeGen.getToSpace().end())) {
                if (Heap.verbose()) {
                    Debug.println("The living objects size is equal greater than the copy reserve");
                }
                throw BeltwayHeapScheme._outOfMemoryError;
            }
            beltwayHeapSchemeGen.getEdenSpace().setStopAddress(beltwayHeapSchemeGen.getEdenSpace().getAllocationMark());
            if (Heap.verbose()) {
                Debug.println("Scan roots ");
            }
            // Start scanning the reachable objects from my roots.
            beltwayHeapSchemeGen.getRootScannerUpdater().setToSpace(beltwayHeapSchemeGen.getMatureSpace());
            beltwayHeapSchemeGen.getRootScannerUpdater().setFromSpace(beltwayHeapSchemeGen.getEdenSpace());
            beltwayHeapSchemeGen.getRootScannerUpdater().run();
            VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();

            if (Heap.verbose()) {
                Debug.println("Scan Boot");
            }

            beltwayHeapSchemeGen.scanBootHeap(beltwayHeapSchemeGen.getEdenSpace(), beltwayHeapSchemeGen.getMatureSpace());

            if (Heap.verbose()) {
                Debug.println("Scan code");
            }
            beltwayHeapSchemeGen.scanCode(beltwayHeapSchemeGen.getEdenSpace(), beltwayHeapSchemeGen.getMatureSpace());

            if (Heap.verbose()) {
                Debug.println("Move Reachable");
                Debug.print("Eden Space Start: ");
                Debug.println(beltwayHeapSchemeGen.getEdenSpace().start());
                Debug.print("Eden Space Allocation Mark: ");
                Debug.println(beltwayHeapSchemeGen.getEdenSpace().getAllocationMark());
                Debug.print("Eden Space End: ");
                Debug.println(beltwayHeapSchemeGen.getEdenSpace().end());
                Debug.print("To Space Start: ");
                Debug.println(beltwayHeapSchemeGen.getToSpace().start());
                Debug.print("To Space Allocation Mark: ");
                Debug.println(beltwayHeapSchemeGen.getToSpace().getAllocationMark());
                Debug.print("To Space End: ");
                Debug.println(beltwayHeapSchemeGen.getToSpace().end());
                Debug.print("Mature Space Start: ");
                Debug.println(beltwayHeapSchemeGen.getMatureSpace().start());
                Debug.print("Mature Space Allocation Mark: ");
                Debug.println(beltwayHeapSchemeGen.getMatureSpace().getAllocationMark());
                Debug.print("Mature Space End: ");
                Debug.println(beltwayHeapSchemeGen.getMatureSpace().end());
            }
            if (BeltwayConfiguration._parallelScavenging) {
                beltwayHeapSchemeGen.fillLastTLAB();
                beltwayHeapSchemeGen.markSideTableLastTLAB();
                BeltwayHeapScheme._inScavening = true;
                beltwayHeapSchemeGen.initializeGCThreads(beltwayHeapSchemeGen, beltwayHeapSchemeGen.getEdenSpace(), beltwayHeapSchemeGen.getMatureSpace());
                VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();
                if (Heap.verbose()) {
                    Debug.println("Start Threads");
                }

                beltwayHeapSchemeGen.startGCThreads();
                BeltwayHeapScheme._inScavening = false;
                if (Heap.verbose()) {
                    Debug.println("Join Threads");
                }
            } else {
                beltwayHeapSchemeGen.linearScanRegionBelt(beltwayHeapSchemeGen.getMatureSpace(), beltwayHeapSchemeGen.getEdenSpace(), beltwayHeapSchemeGen.getMatureSpace());
                beltwayHeapSchemeGen.fillLastTLAB();
            }

            if (Heap.verbose()) {
                Debug.println("Reset Eden Space ");
            }
            beltwayHeapSchemeGen.getEdenSpace().resetAllocationMark();
            beltwayHeapSchemeGen.getEdenSpace().setStopAddress(beltwayHeapSchemeGen.getToSpace().start());
            beltwayHeapSchemeGen.getEdenSpace().setExpandable(false);
            TeleHeap.afterGarbageCollection();
            if (Heap.verbose()) {
                Debug.print("Eden Space Start: ");
                Debug.println(beltwayHeapSchemeGen.getEdenSpace().start());
                Debug.print("Eden Space Allocation Mark: ");
                Debug.println(beltwayHeapSchemeGen.getEdenSpace().getAllocationMark());
                Debug.print("Eden Space End: ");
                Debug.println(beltwayHeapSchemeGen.getEdenSpace().end());
                Debug.print("To Space Start: ");
                Debug.println(beltwayHeapSchemeGen.getToSpace().start());
                Debug.print("To Space Allocation Mark: ");
                Debug.println(beltwayHeapSchemeGen.getToSpace().getAllocationMark());
                Debug.print("To Space End: ");
                Debug.println(beltwayHeapSchemeGen.getToSpace().end());
                Debug.print("Mature Space Start: ");
                Debug.println(beltwayHeapSchemeGen.getMatureSpace().start());
                Debug.print("Mature Space Allocation Mark: ");
                Debug.println(beltwayHeapSchemeGen.getMatureSpace().getAllocationMark());
                Debug.print("Mature Space End: ");
                Debug.println(beltwayHeapSchemeGen.getMatureSpace().end());
            }
            if (Heap.verbose()) {
                Debug.print("Finished Eden Collection: ");
                Debug.println(_edenCollections);
            }
        }
    };

    public Runnable getMinorGC() {
        return _edenGC;
    }

    public Runnable getMajorGC() {
        return _majorGC;
    }

    public Runnable getToGC() {
        return _toGC;
    }

    private final Runnable _toGC = new Runnable() {

        public void run() {
            _toCollections++;
            if (Heap.verbose()) {
                Debug.print("To Collection: ");
                Debug.println(_toCollections);
            }
            final BeltwayHeapSchemeGenerational beltwayHeapSchemeGen = (BeltwayHeapSchemeGenerational) _beltwayHeapScheme;

            if (Heap.verbose()) {
                Debug.print("To Space Start: ");
                Debug.println(beltwayHeapSchemeGen.getToSpace().start());
                Debug.print("To Space Allocation Mark: ");
                Debug.println(beltwayHeapSchemeGen.getToSpace().getAllocationMark());
                Debug.print("To Space End: ");
                Debug.println(beltwayHeapSchemeGen.getToSpace().end());
                Debug.println("Verify To Space: ");
            }

            beltwayHeapSchemeGen.getVerifier().verifyHeap(beltwayHeapSchemeGen.getToSpace().start(), beltwayHeapSchemeGen.getToSpace().getAllocationMark(), BeltManager.getApplicationHeap());
            if (Heap.verbose()) {
                Debug.println(" Mature Snapshot ");
            }

            beltwayHeapSchemeGen.getMatureSpace().setAllocationMarkSnapshot();
            VMConfiguration.hostOrTarget().monitorScheme().beforeGarbageCollection();

            if (Heap.verbose()) {
                Debug.println("Scan Roots ");
            }
            beltwayHeapSchemeGen.getRootScannerUpdater().setFromSpace(beltwayHeapSchemeGen.getToSpace());
            beltwayHeapSchemeGen.getRootScannerUpdater().setToSpace(beltwayHeapSchemeGen.getMatureSpace());
            beltwayHeapSchemeGen.getRootScannerUpdater().run();

            if (Heap.verbose()) {
                Debug.println("Scan Boot Heap");
            }

            beltwayHeapSchemeGen.scanBootHeap(beltwayHeapSchemeGen.getToSpace(), beltwayHeapSchemeGen.getMatureSpace());

            if (Heap.verbose()) {
                Debug.println("Scan Code");
            }

            beltwayHeapSchemeGen.scanCode(beltwayHeapSchemeGen.getToSpace(), beltwayHeapSchemeGen.getMatureSpace());

            if (Heap.verbose()) {
                Debug.println("Scan cards");
            }

            beltwayHeapSchemeGen.scanCards(beltwayHeapSchemeGen.getMatureSpace(), beltwayHeapSchemeGen.getToSpace(), beltwayHeapSchemeGen.getMatureSpace());

            if (Heap.verbose()) {
                Debug.println("Move Reachable Objects");
            }

            if (BeltwayConfiguration._parallelScavenging) {
                beltwayHeapSchemeGen.fillLastTLAB();
                beltwayHeapSchemeGen.markSideTableLastTLAB();
                BeltwayHeapScheme._inScavening = true;
                beltwayHeapSchemeGen.initializeGCThreads(beltwayHeapSchemeGen, beltwayHeapSchemeGen.getToSpace(), beltwayHeapSchemeGen.getMatureSpace());
                VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();
                if (Heap.verbose()) {
                    Debug.println("Start Threads");
                }

                beltwayHeapSchemeGen.startGCThreads();
                BeltwayHeapScheme._inScavening = false;
                if (Heap.verbose()) {
                    Debug.println("Join Threads");
                }
            } else {
                beltwayHeapSchemeGen.linearScanRegionBelt(beltwayHeapSchemeGen.getMatureSpace(), beltwayHeapSchemeGen.getToSpace(), beltwayHeapSchemeGen.getMatureSpace());
                beltwayHeapSchemeGen.fillLastTLAB();
            }

            BeltwayHeapSchemeGenerational._sideTable.restoreAllChunkSlots();

            // TODO: Delete
            //Debug.println("Wipe To");
            //beltwayHeapSchemeGen.wipeMemory(beltwayHeapSchemeGen.getToSpace());

            beltwayHeapSchemeGen.getToSpace().resetAllocationMark();
            VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();

            if (Heap.verbose()) {
                Debug.print("Mature Space Start: ");
                Debug.println(beltwayHeapSchemeGen.getMatureSpace().start());
                Debug.print("Mature Space Allocation Mark: ");
                Debug.println(beltwayHeapSchemeGen.getMatureSpace().getAllocationMark());
                Debug.print("Mature Space End: ");
                Debug.println(beltwayHeapSchemeGen.getMatureSpace().end());
                Debug.println("Verify Mature Space");
            }
            beltwayHeapSchemeGen.getVerifier().verifyHeap(beltwayHeapSchemeGen.getMatureSpace().start(), beltwayHeapSchemeGen.getMatureSpace().getAllocationMark(), BeltManager.getApplicationHeap());
            TeleHeap.afterGarbageCollection();
            if (Heap.verbose()) {
                Debug.print("Finished To Collection: ");
                Debug.println(_toCollections);
            }
        }
    };

    private final Runnable _edenGC = new Runnable() {

        public void run() {
            _edenCollections++;
            if (Heap.verbose()) {
                Debug.print("Eden Collection: ");
                Debug.println(_edenCollections);
            }
            final BeltwayHeapSchemeGenerational beltwayHeapSchemeGen = (BeltwayHeapSchemeGenerational) _beltwayHeapScheme;

            if (Heap.verbose()) {
                Debug.print("Eden Space Start: ");
                Debug.println(beltwayHeapSchemeGen.getEdenSpace().start());
                Debug.print("Eden Space Allocation Mark: ");
                Debug.println(beltwayHeapSchemeGen.getEdenSpace().getAllocationMark());
                Debug.print("Eden Space End: ");
                Debug.println(beltwayHeapSchemeGen.getEdenSpace().end());
                Debug.println("Verify Eden: ");
            }
            beltwayHeapSchemeGen.getVerifier().verifyHeap(beltwayHeapSchemeGen.getEdenSpace().start(), beltwayHeapSchemeGen.getEdenSpace().getAllocationMark(), BeltManager.getApplicationHeap());

            if (Heap.verbose()) {
                Debug.println("To Space Snapshot");
            }
            beltwayHeapSchemeGen.getToSpace().setAllocationMarkSnapshot();
            TeleHeap.beforeGarbageCollection();

            VMConfiguration.hostOrTarget().monitorScheme().beforeGarbageCollection();
            if (Heap.verbose()) {
                Debug.println("Scan Roots ");
            }
            beltwayHeapSchemeGen.getRootScannerUpdater().setFromSpace(beltwayHeapSchemeGen.getEdenSpace());
            beltwayHeapSchemeGen.getRootScannerUpdater().setToSpace(beltwayHeapSchemeGen.getToSpace());
            beltwayHeapSchemeGen.getRootScannerUpdater().run();

            if (Heap.verbose()) {
                Debug.println("Scan Boot Heap");
            }

            beltwayHeapSchemeGen.scanBootHeap(beltwayHeapSchemeGen.getEdenSpace(), beltwayHeapSchemeGen.getToSpace());

            if (Heap.verbose()) {
                Debug.println("Scan Code");
            }

            beltwayHeapSchemeGen.scanCode(beltwayHeapSchemeGen.getEdenSpace(), beltwayHeapSchemeGen.getToSpace());

            if (Heap.verbose()) {
                Debug.println("Scan cards");
            }

            beltwayHeapSchemeGen.scanCards(beltwayHeapSchemeGen.getToSpace(), beltwayHeapSchemeGen.getEdenSpace(), beltwayHeapSchemeGen.getToSpace());
            beltwayHeapSchemeGen.scanCards(beltwayHeapSchemeGen.getMatureSpace(), beltwayHeapSchemeGen.getEdenSpace(), beltwayHeapSchemeGen.getToSpace());

            if (Heap.verbose()) {
                Debug.println("Move Reachable Objects");
            }

            if (BeltwayConfiguration._parallelScavenging) {
                beltwayHeapSchemeGen.fillLastTLAB();
                beltwayHeapSchemeGen.markSideTableLastTLAB();
                BeltwayHeapScheme._inScavening = true;
                beltwayHeapSchemeGen.initializeGCThreads(beltwayHeapSchemeGen, beltwayHeapSchemeGen.getEdenSpace(), beltwayHeapSchemeGen.getToSpace());
                VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();
                if (Heap.verbose()) {
                    Debug.println("Start Threads");
                }

                beltwayHeapSchemeGen.startGCThreads();
                BeltwayHeapScheme._inScavening = false;
                if (Heap.verbose()) {
                    Debug.println("Join Threads");
                }
            } else {
                beltwayHeapSchemeGen.linearScanRegionBelt(beltwayHeapSchemeGen.getToSpace(), beltwayHeapSchemeGen.getEdenSpace(), beltwayHeapSchemeGen.getToSpace());
                beltwayHeapSchemeGen.fillLastTLAB();
            }

            BeltwayHeapSchemeGenerational._sideTable.restoreAllChunkSlots();

            // TODO: Delete
            //Debug.println("Wipe Eden");
            //beltwayHeapSchemeGen.wipeMemory(beltwayHeapSchemeGen.getEdenSpace());

            if (Heap.verbose()) {
                Debug.println("Reset Nursery Space Allocation Mark");
            }

            beltwayHeapSchemeGen.getEdenSpace().resetAllocationMark();
            VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();

            if (Heap.verbose()) {
                Debug.print("To Space Start: ");
                Debug.println(beltwayHeapSchemeGen.getToSpace().start());
                Debug.print("To Space Allocation Mark: ");
                Debug.println(beltwayHeapSchemeGen.getToSpace().getAllocationMark());
                Debug.print("To Space End: ");
                Debug.println(beltwayHeapSchemeGen.getToSpace().end());
                Debug.println("Verfiy To Space: ");
            }

            beltwayHeapSchemeGen.getVerifier().verifyHeap(beltwayHeapSchemeGen.getToSpace().start(), beltwayHeapSchemeGen.getToSpace().getAllocationMark(), BeltManager.getApplicationHeap());
            TeleHeap.afterGarbageCollection();
            if (Heap.verbose()) {
                Debug.print("Finished Eden Collection: ");
                Debug.println(_edenCollections);
            }
        }
    };

}
