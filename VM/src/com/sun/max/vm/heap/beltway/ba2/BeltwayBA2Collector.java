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

    public static long minorCollections = 0;
    public static long majorCollections = 0;

    public BeltwayBA2Collector() {
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
            final BeltwayHeapSchemeBA2 beltwayHeapSchemeBA2 = (BeltwayHeapSchemeBA2) getBeltwayHeapScheme();
            if (Heap.verbose()) {
                Log.println("Verify Mature Space: ");
            }
            final Belt matureSpace = beltwayHeapSchemeBA2.getMatureSpace();
            verifyBelt(matureSpace);
            InspectableHeapInfo.beforeGarbageCollection();

            VMConfiguration.hostOrTarget().monitorScheme().beforeGarbageCollection();

            final Pointer matureSpaceEnd = matureSpace.end().asPointer();
            final Belt matureSpaceBeforeAllocation = beltwayHeapSchemeBA2.getBeltManager().getBeltBeforeLastAllocation(matureSpace);
            final Belt matureSpaceReserve = beltwayHeapSchemeBA2.getBeltManager().getRemainingOverlappingBelt(matureSpace);
            matureSpaceReserve.setExpandable(true);
            matureSpaceReserve.setEnd(matureSpaceEnd);

            if (Heap.verbose()) {
                Log.print("matureSpaceBeforeAllocation start: ");
                Log.println(matureSpaceBeforeAllocation.start());
                Log.print("matureSpaceBeforeAllocation Mark: ");
                Log.println(matureSpaceBeforeAllocation.getAllocationMark());
                Log.print("matureSpaceBeforeAllocation Space End: ");
                Log.println(matureSpaceBeforeAllocation.end());
                Log.print("matureSpaceReserve Space Start: ");
                Log.println(matureSpaceReserve.start());
                Log.print("matureSpaceReserve Space Allocation Mark: ");
                Log.println(matureSpaceReserve.getAllocationMark());
                Log.print("matureSpaceReserve Space End: ");
                Log.println(matureSpaceReserve.end());
                Log.println("Scan roots ");
            }
            // Start scanning the reachable objects from my roots.
            beltwayHeapSchemeBA2.getRootScannerUpdater().setFromSpace(matureSpaceBeforeAllocation);
            beltwayHeapSchemeBA2.getRootScannerUpdater().setToSpace(matureSpaceReserve);
            beltwayHeapSchemeBA2.getRootScannerUpdater().run();

            if (Heap.verbose()) {
                Log.print("matureSpaceReserve Space Start: ");
                Log.println(matureSpaceReserve.start());
                Log.print("matureSpaceReserve Space Allocation Mark: ");
                Log.println(matureSpaceReserve.getAllocationMark());
                Log.print("matureSpaceReserve Space End: ");
                Log.println(matureSpaceReserve.end());
                Log.println("Scan Boot");
            }
            beltwayHeapSchemeBA2.scanBootHeap(matureSpaceBeforeAllocation, matureSpaceReserve);

            if (Heap.verbose()) {
                Log.print("matureSpaceReserve Space Start: ");
                Log.println(matureSpaceReserve.start());
                Log.print("matureSpaceReserve Space Allocation Mark: ");
                Log.println(matureSpaceReserve.getAllocationMark());
                Log.print("matureSpaceReserve Space End: ");
                Log.println(matureSpaceReserve.end());
                Log.println("Scan code");

            }
            beltwayHeapSchemeBA2.scanCode(matureSpaceBeforeAllocation, matureSpaceReserve);

            if (Heap.verbose()) {
                Log.print("matureSpaceReserve Space Start: ");
                Log.println(matureSpaceReserve.start());
                Log.print("matureSpaceReserve Space Allocation Mark: ");
                Log.println(matureSpaceReserve.getAllocationMark());
                Log.print("matureSpaceReserve Space End: ");
                Log.println(matureSpaceReserve.end());
                Log.println("Move Reachable");
            }

            if (BeltwayConfiguration.parallelScavenging) {
                beltwayHeapSchemeBA2.fillLastTLAB();
                beltwayHeapSchemeBA2.markSideTableLastTLAB();
                BeltwayHeapScheme.inScavenging = true;
                beltwayHeapSchemeBA2.initializeGCThreads(beltwayHeapSchemeBA2, matureSpaceBeforeAllocation, matureSpaceReserve);
                VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();
                if (Heap.verbose()) {
                    Log.println("Start Threads");
                }

                beltwayHeapSchemeBA2.startGCThreads();
                BeltwayHeapScheme.inScavenging = false;
                if (Heap.verbose()) {
                    Log.println("Join Threads");
                }
            } else {
                beltwayHeapSchemeBA2.linearScanRegionBelt(matureSpaceReserve, matureSpaceBeforeAllocation, matureSpaceReserve);
                beltwayHeapSchemeBA2.fillLastTLAB();
            }

            matureSpaceReserve.setEnd(matureSpaceReserve.getAllocationMark());

            if (Heap.verbose()) {
                Log.print("matureSpaceBeforeAllocation start: ");
                Log.println(matureSpaceBeforeAllocation.start());
                Log.print("matureSpaceBeforeAllocation Mark: ");
                Log.println(matureSpaceBeforeAllocation.getAllocationMark());
                Log.print("matureSpaceBeforeAllocation Space End: ");
                Log.println(matureSpaceBeforeAllocation.end());
                Log.print("matureSpaceReserve Space Start: ");
                Log.println(matureSpaceReserve.start());
                Log.print("matureSpaceReserve Space Allocation Mark: ");
                Log.println(matureSpaceReserve.getAllocationMark());
                Log.print("matureSpaceReserve Space End: ");
                Log.println(matureSpaceReserve.end());
            }

            matureSpace.resetAllocationMark();
            matureSpace.setEnd(matureSpaceEnd);
            if (Heap.verbose()) {
                Log.print("Mature Space Start: ");
                Log.println(matureSpace.start());
                Log.print("Mature Space Allocation Mark: ");
                Log.println(matureSpace.getAllocationMark());
                Log.print("Mature Space End: ");
                Log.println(matureSpace.end());

                Log.print("Mature Space Reserve Size: ");
                Log.println(matureSpaceReserve.size().toLong());
                Log.print("Configuration usable memory Size ");
                Log.println(BeltwayConfiguration.getUsableMemory().toLong());
            }
            if (matureSpaceReserve.size().lessEqual(BeltwayConfiguration.getUsableMemory())) {

                if (Heap.verbose()) {
                    Log.println("Compaction ");
                    Log.println("Scan roots ");
                }
                // Start scanning the reachable objects from my roots.
                beltwayHeapSchemeBA2.getRootScannerUpdater().setToSpace(matureSpace);
                beltwayHeapSchemeBA2.getRootScannerUpdater().setFromSpace(matureSpaceReserve);
                beltwayHeapSchemeBA2.getRootScannerUpdater().run();

                if (Heap.verbose()) {
                    Log.println("Scan Boot");
                }

                beltwayHeapSchemeBA2.scanBootHeap(matureSpaceReserve, matureSpace);

                if (Heap.verbose()) {
                    Log.println("Scan code");
                }

                beltwayHeapSchemeBA2.scanCode(matureSpaceReserve, beltwayHeapSchemeBA2.getMatureSpace());
                if (Heap.verbose()) {
                    Log.println("Move Reachable");
                }

                if (BeltwayConfiguration.parallelScavenging) {
                    beltwayHeapSchemeBA2.fillLastTLAB();
                    beltwayHeapSchemeBA2.markSideTableLastTLAB();
                    BeltwayHeapScheme.inScavenging = true;
                    beltwayHeapSchemeBA2.initializeGCThreads(beltwayHeapSchemeBA2, matureSpaceReserve, matureSpace);
                    VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();
                    if (Heap.verbose()) {
                        Log.println("Start Threads");
                    }

                    beltwayHeapSchemeBA2.startGCThreads();
                    BeltwayHeapScheme.inScavenging = false;
                    if (Heap.verbose()) {
                        Log.println("Join Threads");
                    }
                } else {
                    beltwayHeapSchemeBA2.linearScanRegionBelt(matureSpace, matureSpaceReserve, matureSpace);
                    beltwayHeapSchemeBA2.fillLastTLAB();
                }

                BeltwayHeapSchemeBA2.sideTable.restoreAllChunkSlots();


                if (Heap.verbose()) {
                    Log.println("Reset Nursery Space Allocation Mark");
                }
                InspectableHeapInfo.afterGarbageCollection();
                VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();


                if (Heap.verbose()) {
                    final Belt nurserySpace = beltwayHeapSchemeBA2.getNurserySpace();
                    Log.print("Nursery Space Start: ");
                    Log.println(nurserySpace.start());
                    Log.print("Nursery Space Allocation Mark: ");
                    Log.println(nurserySpace.getAllocationMark());
                    Log.print("Nursery Space End: ");
                    Log.println(nurserySpace.end());
                    Log.print("Mature Space Start: ");
                    Log.println(matureSpace.start());
                    Log.print("Mature Space Allocation Mark: ");
                    Log.println(matureSpace.getAllocationMark());
                    Log.print("Mature Space End: ");
                    Log.println(matureSpace.end());
                    Log.println("End Calibration ");
                }

            } else {
                BeltwayHeapScheme.outOfMemory = true;
            }
        }
    };

    public Runnable getMinorGC() {
        return minorGC;
    }

    public Runnable getMajorGC() {
        return majorGC;
    }

    private final Runnable minorGC = new Runnable() {

        public void run() {
            minorCollections++;
            if (Heap.verbose()) {
                Log.print("Minor Collection: ");
                Log.println(minorCollections);
            }
            final BeltwayHeapSchemeBA2 beltwayHeapSchemeBA2 = (BeltwayHeapSchemeBA2) getBeltwayHeapScheme();
            final Belt nurserySpace =  beltwayHeapSchemeBA2.getNurserySpace();
            final Belt matureSpace =  beltwayHeapSchemeBA2.getMatureSpace();
            matureSpace.setAllocationMarkSnapshot();

            if (Heap.verbose()) {
                Log.print("Nursery Space Start: ");
                Log.println(nurserySpace.start());
                Log.print("Nursery Space Allocation Mark: ");
                Log.println(nurserySpace.getAllocationMark());
                Log.print("Nursery Space End: ");
                Log.println(nurserySpace.end());
                Log.print("Mature Space Start: ");
                Log.println(matureSpace.start());
                Log.print("Mature Space Allocation Mark: ");
                Log.println(matureSpace.getAllocationMark());
                Log.print("Mature Space End: ");
                Log.println(matureSpace.end());
                Log.println("Verify Nursery:");
            }
            verifyBelt(nurserySpace);
            InspectableHeapInfo.beforeGarbageCollection();
            VMConfiguration.hostOrTarget().monitorScheme().beforeGarbageCollection();

            if (Heap.verbose()) {
                Log.println("Scan Roots ");
            }
            beltwayHeapSchemeBA2.getRootScannerUpdater().setFromSpace(nurserySpace);
            beltwayHeapSchemeBA2.getRootScannerUpdater().setToSpace(matureSpace);
            beltwayHeapSchemeBA2.getRootScannerUpdater().run();

            if (Heap.verbose()) {
                Log.println("Scan Boot Heap");
            }

            beltwayHeapSchemeBA2.scanBootHeap(nurserySpace, matureSpace);

            if (Heap.verbose()) {
                Log.println("Scan Code");
            }

            beltwayHeapSchemeBA2.scanCode(nurserySpace, matureSpace);

            if (Heap.verbose()) {
                Log.println("Scan Cards");
            }

            beltwayHeapSchemeBA2.scanCards(matureSpace, nurserySpace, matureSpace);

            if (Heap.verbose()) {
                Log.println("Scavenge");
            }
            if (BeltwayConfiguration.parallelScavenging) {
                beltwayHeapSchemeBA2.fillLastTLAB();
                beltwayHeapSchemeBA2.markSideTableLastTLAB();
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
            } else {
                beltwayHeapSchemeBA2.linearScanRegionBelt(matureSpace, nurserySpace, matureSpace);
                beltwayHeapSchemeBA2.fillLastTLAB();
            }

            BeltwayHeapSchemeBA2.sideTable.restoreAllChunkSlots();

            // TODO: Delete
            //Debug.println("Wipe Eden");
            //beltwayHeapSchemeBA2.wipeMemory(nurserySpace);

            if (Heap.verbose()) {
                Log.println("Reset Nursery Space Allocation Mark");
            }
            nurserySpace.resetAllocationMark();
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

            verifyBelt(matureSpace);
            InspectableHeapInfo.afterGarbageCollection();

            if (Heap.verbose()) {
                Log.print("Nursery Space Start: ");
                Log.println(nurserySpace.start());
                Log.print("Nursery Space Allocation Mark: ");
                Log.println(nurserySpace.getAllocationMark());
                Log.print("Nursery Space End: ");
                Log.println(nurserySpace.end());
                Log.print("Mature Space Start: ");
                Log.println(matureSpace.start());
                Log.print("Mature Space Allocation Mark: ");
                Log.println(matureSpace.getAllocationMark());
                Log.print("Mature Space End: ");
                Log.println(matureSpace.end());
            }

            if (Heap.verbose()) {
                Log.print("End of Minor Collection: ");
                Log.println(minorCollections);
            }
        }
    };

}
