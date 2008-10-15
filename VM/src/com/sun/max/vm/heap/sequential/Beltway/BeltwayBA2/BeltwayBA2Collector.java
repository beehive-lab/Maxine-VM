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
package com.sun.max.vm.heap.sequential.Beltway.BeltwayBA2;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.sequential.Beltway.*;
import com.sun.max.vm.tele.*;

/**
 * @author Christos Kotselidis
 */

public class BeltwayBA2Collector extends BeltwayCollector {

    // /Dependency injection of the corresponding heap scheme
    private static BeltwayHeapScheme _beltwayHeapScheme;

    public static long _minorCollections = 0;
    public static long _majorCollections = 0;

    public void setBeltwayHeapScheme(BeltwayHeapScheme beltwayHeapScheme) {
        _beltwayHeapScheme = beltwayHeapScheme;
    }

    public BeltwayHeapScheme getBeltwayHeapScheme() {
        return _beltwayHeapScheme;
    }

    public BeltwayBA2Collector() {

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
            final BeltwayHeapSchemeBA2 beltwayHeapSchemeBA2 = (BeltwayHeapSchemeBA2) _beltwayHeapScheme;
            if (Heap.verbose()) {
                Debug.println("Verify Mature Space: ");
            }
            beltwayHeapSchemeBA2.getVerifier().verifyHeap(beltwayHeapSchemeBA2.getMatureSpace().start(), beltwayHeapSchemeBA2.getMatureSpace().getAllocationMark(), BeltManager.getApplicationHeap());
            TeleHeapInfo.beforeGarbageCollection();

            VMConfiguration.hostOrTarget().monitorScheme().beforeGarbageCollection();

            final Pointer matureSpaceEnd = beltwayHeapSchemeBA2.getMatureSpace().end().asPointer();
            final Belt matureSpaceBeforeAllocation = beltwayHeapSchemeBA2.getBeltManager().getBeltBeforeLastAllocation(beltwayHeapSchemeBA2.getMatureSpace());
            final Belt matureSpaceReserve = beltwayHeapSchemeBA2.getBeltManager().getRemainingOverlappingBelt(beltwayHeapSchemeBA2.getMatureSpace());
            matureSpaceReserve.setExpandable(true);
            matureSpaceReserve.setStopAddress(matureSpaceEnd);

            if (Heap.verbose()) {
                Debug.print("matureSpaceBeforeAllocation start: ");
                Debug.println(matureSpaceBeforeAllocation.start());
                Debug.print("matureSpaceBeforeAllocation Mark: ");
                Debug.println(matureSpaceBeforeAllocation.getAllocationMark());
                Debug.print("matureSpaceBeforeAllocation Space End: ");
                Debug.println(matureSpaceBeforeAllocation.end());
                Debug.print("matureSpaceReserve Space Start: ");
                Debug.println(matureSpaceReserve.start());
                Debug.print("matureSpaceReserve Space Allocation Mark: ");
                Debug.println(matureSpaceReserve.getAllocationMark());
                Debug.print("matureSpaceReserve Space End: ");
                Debug.println(matureSpaceReserve.end());
                Debug.println("Scan roots ");
            }
            // Start scanning the reachable objects from my roots.
            beltwayHeapSchemeBA2.getRootScannerUpdater().setFromSpace(matureSpaceBeforeAllocation);
            beltwayHeapSchemeBA2.getRootScannerUpdater().setToSpace(matureSpaceReserve);
            beltwayHeapSchemeBA2.getRootScannerUpdater().run();

            if (Heap.verbose()) {
                Debug.print("matureSpaceReserve Space Start: ");
                Debug.println(matureSpaceReserve.start());
                Debug.print("matureSpaceReserve Space Allocation Mark: ");
                Debug.println(matureSpaceReserve.getAllocationMark());
                Debug.print("matureSpaceReserve Space End: ");
                Debug.println(matureSpaceReserve.end());
                Debug.println("Scan Boot");
            }
            beltwayHeapSchemeBA2.scanBootHeap(matureSpaceBeforeAllocation, matureSpaceReserve);

            if (Heap.verbose()) {
                Debug.print("matureSpaceReserve Space Start: ");
                Debug.println(matureSpaceReserve.start());
                Debug.print("matureSpaceReserve Space Allocation Mark: ");
                Debug.println(matureSpaceReserve.getAllocationMark());
                Debug.print("matureSpaceReserve Space End: ");
                Debug.println(matureSpaceReserve.end());
                Debug.println("Scan code");

            }
            beltwayHeapSchemeBA2.scanCode(matureSpaceBeforeAllocation, matureSpaceReserve);

            if (Heap.verbose()) {
                Debug.print("matureSpaceReserve Space Start: ");
                Debug.println(matureSpaceReserve.start());
                Debug.print("matureSpaceReserve Space Allocation Mark: ");
                Debug.println(matureSpaceReserve.getAllocationMark());
                Debug.print("matureSpaceReserve Space End: ");
                Debug.println(matureSpaceReserve.end());
                Debug.println("Move Reachable");
            }

            if (BeltwayConfiguration._parallelScavenging) {
                beltwayHeapSchemeBA2.fillLastTLAB();
                beltwayHeapSchemeBA2.markSideTableLastTLAB();
                BeltwayHeapScheme._inScavening = true;
                beltwayHeapSchemeBA2.initializeGCThreads(beltwayHeapSchemeBA2, matureSpaceBeforeAllocation, matureSpaceReserve);
                VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();
                if (Heap.verbose()) {
                    Debug.println("Start Threads");
                }

                beltwayHeapSchemeBA2.startGCThreads();
                BeltwayHeapScheme._inScavening = false;
                if (Heap.verbose()) {
                    Debug.println("Join Threads");
                }
            } else {
                beltwayHeapSchemeBA2.linearScanRegionBelt(matureSpaceReserve, matureSpaceBeforeAllocation, matureSpaceReserve);
                beltwayHeapSchemeBA2.fillLastTLAB();
            }

            matureSpaceReserve.setStopAddress(matureSpaceReserve.getAllocationMark());

            if (Heap.verbose()) {
                Debug.print("matureSpaceBeforeAllocation start: ");
                Debug.println(matureSpaceBeforeAllocation.start());
                Debug.print("matureSpaceBeforeAllocation Mark: ");
                Debug.println(matureSpaceBeforeAllocation.getAllocationMark());
                Debug.print("matureSpaceBeforeAllocation Space End: ");
                Debug.println(matureSpaceBeforeAllocation.end());
                Debug.print("matureSpaceReserve Space Start: ");
                Debug.println(matureSpaceReserve.start());
                Debug.print("matureSpaceReserve Space Allocation Mark: ");
                Debug.println(matureSpaceReserve.getAllocationMark());
                Debug.print("matureSpaceReserve Space End: ");
                Debug.println(matureSpaceReserve.end());
            }

            beltwayHeapSchemeBA2.getMatureSpace().resetAllocationMark();
            beltwayHeapSchemeBA2.getMatureSpace().setStopAddress(matureSpaceEnd);
            if (Heap.verbose()) {
                Debug.print("Mature Space Start: ");
                Debug.println(beltwayHeapSchemeBA2.getMatureSpace().start());
                Debug.print("Mature Space Allocation Mark: ");
                Debug.println(beltwayHeapSchemeBA2.getMatureSpace().getAllocationMark());
                Debug.print("Mature Space End: ");
                Debug.println(beltwayHeapSchemeBA2.getMatureSpace().end());

                Debug.print("Mature Space Reserve Size: ");
                Debug.println(matureSpaceReserve.size().toLong());
                Debug.print("Configuration usable memory Size ");
                Debug.println(BeltwayConfiguration.getUsableMemory().toLong());
            }
            if (matureSpaceReserve.size().lessEqual(BeltwayConfiguration.getUsableMemory())) {

                if (Heap.verbose()) {
                    Debug.println("Compaction ");
                    Debug.println("Scan roots ");
                }
                // Start scanning the reachable objects from my roots.
                beltwayHeapSchemeBA2.getRootScannerUpdater().setToSpace(beltwayHeapSchemeBA2.getMatureSpace());
                beltwayHeapSchemeBA2.getRootScannerUpdater().setFromSpace(matureSpaceReserve);
                beltwayHeapSchemeBA2.getRootScannerUpdater().run();

                if (Heap.verbose()) {
                    Debug.println("Scan Boot");
                }

                beltwayHeapSchemeBA2.scanBootHeap(matureSpaceReserve, beltwayHeapSchemeBA2.getMatureSpace());

                if (Heap.verbose()) {
                    Debug.println("Scan code");
                }

                beltwayHeapSchemeBA2.scanCode(matureSpaceReserve, beltwayHeapSchemeBA2.getMatureSpace());
                if (Heap.verbose()) {
                    Debug.println("Move Reachable");
                }

                if (BeltwayConfiguration._parallelScavenging) {
                    beltwayHeapSchemeBA2.fillLastTLAB();
                    beltwayHeapSchemeBA2.markSideTableLastTLAB();
                    BeltwayHeapScheme._inScavening = true;
                    beltwayHeapSchemeBA2.initializeGCThreads(beltwayHeapSchemeBA2, matureSpaceReserve, beltwayHeapSchemeBA2.getMatureSpace());
                    VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();
                    if (Heap.verbose()) {
                        Debug.println("Start Threads");
                    }

                    beltwayHeapSchemeBA2.startGCThreads();
                    BeltwayHeapScheme._inScavening = false;
                    if (Heap.verbose()) {
                        Debug.println("Join Threads");
                    }
                } else {
                    beltwayHeapSchemeBA2.linearScanRegionBelt(beltwayHeapSchemeBA2.getMatureSpace(), matureSpaceReserve, beltwayHeapSchemeBA2.getMatureSpace());
                    beltwayHeapSchemeBA2.fillLastTLAB();
                }

                BeltwayHeapSchemeBA2._sideTable.restoreAllChunkSlots();


                if (Heap.verbose()) {
                    Debug.println("Reset Nursery Space Allocation Mark");
                }
                TeleHeapInfo.afterGarbageCollection();
                VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();


                if (Heap.verbose()) {
                    Debug.print("Nursery Space Start: ");
                    Debug.println(beltwayHeapSchemeBA2.getNurserySpace().start());
                    Debug.print("Nursery Space Allocation Mark: ");
                    Debug.println(beltwayHeapSchemeBA2.getNurserySpace().getAllocationMark());
                    Debug.print("Nursery Space End: ");
                    Debug.println(beltwayHeapSchemeBA2.getNurserySpace().end());
                    Debug.print("Mature Space Start: ");
                    Debug.println(beltwayHeapSchemeBA2.getMatureSpace().start());
                    Debug.print("Mature Space Allocation Mark: ");
                    Debug.println(beltwayHeapSchemeBA2.getMatureSpace().getAllocationMark());
                    Debug.print("Mature Space End: ");
                    Debug.println(beltwayHeapSchemeBA2.getMatureSpace().end());
                    Debug.println("End Calibration ");
                }

            } else {
                BeltwayHeapScheme._outOfMemory = true;
            }
        }
    };

    public Runnable getMinorGC() {
        return _minorGC;
    }

    public Runnable getMajorGC() {
        return _majorGC;
    }

    private final Runnable _minorGC = new Runnable() {

        public void run() {
            _minorCollections++;
            if (Heap.verbose()) {
                Debug.print("Minor Collection: ");
                Debug.println(_minorCollections);
            }
            final BeltwayHeapSchemeBA2 beltwayHeapSchemeBA2 = (BeltwayHeapSchemeBA2) _beltwayHeapScheme;

            beltwayHeapSchemeBA2.getMatureSpace().setAllocationMarkSnapshot();

            if (Heap.verbose()) {
                Debug.print("Nursery Space Start: ");
                Debug.println(beltwayHeapSchemeBA2.getNurserySpace().start());
                Debug.print("Nursery Space Allocation Mark: ");
                Debug.println(beltwayHeapSchemeBA2.getNurserySpace().getAllocationMark());
                Debug.print("Nursery Space End: ");
                Debug.println(beltwayHeapSchemeBA2.getNurserySpace().end());
                Debug.print("Mature Space Start: ");
                Debug.println(beltwayHeapSchemeBA2.getMatureSpace().start());
                Debug.print("Mature Space Allocation Mark: ");
                Debug.println(beltwayHeapSchemeBA2.getMatureSpace().getAllocationMark());
                Debug.print("Mature Space End: ");
                Debug.println(beltwayHeapSchemeBA2.getMatureSpace().end());
                Debug.println("Verify Nursery:");
            }

            beltwayHeapSchemeBA2.getVerifier().verifyHeap(beltwayHeapSchemeBA2.getNurserySpace().start(), beltwayHeapSchemeBA2.getNurserySpace().getAllocationMark(), BeltManager.getApplicationHeap());
            TeleHeapInfo.beforeGarbageCollection();
            VMConfiguration.hostOrTarget().monitorScheme().beforeGarbageCollection();

            if (Heap.verbose()) {
                Debug.println("Scan Roots ");
            }
            beltwayHeapSchemeBA2.getRootScannerUpdater().setFromSpace(beltwayHeapSchemeBA2.getNurserySpace());
            beltwayHeapSchemeBA2.getRootScannerUpdater().setToSpace(beltwayHeapSchemeBA2.getMatureSpace());
            beltwayHeapSchemeBA2.getRootScannerUpdater().run();

            if (Heap.verbose()) {
                Debug.println("Scan Boot Heap");
            }

            beltwayHeapSchemeBA2.scanBootHeap(beltwayHeapSchemeBA2.getNurserySpace(), beltwayHeapSchemeBA2.getMatureSpace());

            if (Heap.verbose()) {
                Debug.println("Scan Code");
            }

            beltwayHeapSchemeBA2.scanCode(beltwayHeapSchemeBA2.getNurserySpace(), beltwayHeapSchemeBA2.getMatureSpace());

            if (Heap.verbose()) {
                Debug.println("Scan Cards");
            }

            beltwayHeapSchemeBA2.scanCards(beltwayHeapSchemeBA2.getMatureSpace(), beltwayHeapSchemeBA2.getNurserySpace(), beltwayHeapSchemeBA2.getMatureSpace());

            if (Heap.verbose()) {
                Debug.println("Scavenge");
            }
            if (BeltwayConfiguration._parallelScavenging) {
                beltwayHeapSchemeBA2.fillLastTLAB();
                beltwayHeapSchemeBA2.markSideTableLastTLAB();
                BeltwayHeapScheme._inScavening = true;
                beltwayHeapSchemeBA2.initializeGCThreads(beltwayHeapSchemeBA2, beltwayHeapSchemeBA2.getNurserySpace(), beltwayHeapSchemeBA2.getMatureSpace());
                VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();
                if (Heap.verbose()) {
                    Debug.println("Start Threads");
                }

                beltwayHeapSchemeBA2.startGCThreads();
                BeltwayHeapScheme._inScavening = false;
                if (Heap.verbose()) {
                    Debug.println("Join Threads");
                }
            } else {
                beltwayHeapSchemeBA2.linearScanRegionBelt(beltwayHeapSchemeBA2.getMatureSpace(), beltwayHeapSchemeBA2.getNurserySpace(), beltwayHeapSchemeBA2.getMatureSpace());
                beltwayHeapSchemeBA2.fillLastTLAB();
            }

            BeltwayHeapSchemeBA2._sideTable.restoreAllChunkSlots();

            // TODO: Delete
            //Debug.println("Wipe Eden");
            //beltwayHeapSchemeBA2.wipeMemory(beltwayHeapSchemeBA2.getNurserySpace());

            if (Heap.verbose()) {
                Debug.println("Reset Nursery Space Allocation Mark");
            }
            beltwayHeapSchemeBA2.getNurserySpace().resetAllocationMark();
            VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();

            if (Heap.verbose()) {
                Debug.print("Mature Space Start: ");
                Debug.println(beltwayHeapSchemeBA2.getMatureSpace().start());
                Debug.print("Mature Space Allocation Mark: ");
                Debug.println(beltwayHeapSchemeBA2.getMatureSpace().getAllocationMark());
                Debug.print("Mature Space End: ");
                Debug.println(beltwayHeapSchemeBA2.getMatureSpace().end());
                Debug.println("Verify Mature Space");
            }

            beltwayHeapSchemeBA2.getVerifier().verifyHeap(beltwayHeapSchemeBA2.getMatureSpace().start(), beltwayHeapSchemeBA2.getMatureSpace().getAllocationMark(), BeltManager.getApplicationHeap());
            TeleHeapInfo.afterGarbageCollection();

            if (Heap.verbose()) {
                Debug.print("Nursery Space Start: ");
                Debug.println(beltwayHeapSchemeBA2.getNurserySpace().start());
                Debug.print("Nursery Space Allocation Mark: ");
                Debug.println(beltwayHeapSchemeBA2.getNurserySpace().getAllocationMark());
                Debug.print("Nursery Space End: ");
                Debug.println(beltwayHeapSchemeBA2.getNurserySpace().end());
                Debug.print("Mature Space Start: ");
                Debug.println(beltwayHeapSchemeBA2.getMatureSpace().start());
                Debug.print("Mature Space Allocation Mark: ");
                Debug.println(beltwayHeapSchemeBA2.getMatureSpace().getAllocationMark());
                Debug.print("Mature Space End: ");
                Debug.println(beltwayHeapSchemeBA2.getMatureSpace().end());
            }

            if (Heap.verbose()) {
                Debug.print("End of Minor Collection: ");
                Debug.println(_minorCollections);
            }
        }
    };

}
