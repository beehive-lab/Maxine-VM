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

import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.beltway.*;
import com.sun.max.vm.tele.*;

/**
 * Semi-space Collector based on Beltways.
 * Used a single belt with two increment (to and from spaces).
 *
 * @author Christos Kotselidis
 */

public class BeltwaySSCollector extends BeltwayCollector {

    private static long collections;

    public BeltwaySSCollector() {
    }

    @Override
    public void run() {
        collections++;
        if (Heap.verbose()) {
            Log.print("Collection: ");
            Log.println(collections);
        }
        final BeltwayHeapSchemeBSS beltwayHeapSchemeBSS = (BeltwayHeapSchemeBSS) getBeltwayHeapScheme();
        if (Heap.verbose()) {
            Log.println("Verify Heap");
            verifyBelt(beltwayHeapSchemeBSS.getFromSpace());
        }

        InspectableHeapInfo.beforeGarbageCollection();
        VMConfiguration.hostOrTarget().monitorScheme().beforeGarbageCollection();

        if (Heap.verbose()) {
            Log.println("Scanning roots...");
        }

        // Start scanning the reachable objects from my roots.
        beltwayHeapSchemeBSS.getRootScannerUpdater().setFromSpace(beltwayHeapSchemeBSS.getFromSpace());
        beltwayHeapSchemeBSS.getRootScannerUpdater().setToSpace(beltwayHeapSchemeBSS.getToSpace());
        beltwayHeapSchemeBSS.getRootScannerUpdater().run();

        if (Heap.verbose()) {
            Log.println("Scanning boot heap...");
        }

        beltwayHeapSchemeBSS.scanBootHeap(beltwayHeapSchemeBSS.getFromSpace(), beltwayHeapSchemeBSS.getToSpace());

        if (Heap.verbose()) {
            Log.println("Scanning code...");
        }

        beltwayHeapSchemeBSS.scanCode(beltwayHeapSchemeBSS.getFromSpace(), beltwayHeapSchemeBSS.getToSpace());

        if (Heap.verbose()) {
            Log.println("Moving recheable...");
        }

        if (BeltwayConfiguration.parallelScavenging) {
            beltwayHeapSchemeBSS.fillLastTLAB();
            beltwayHeapSchemeBSS.initializeGCThreads(beltwayHeapSchemeBSS, beltwayHeapSchemeBSS.getToSpace(), beltwayHeapSchemeBSS.getFromSpace());
            VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();
            if (Heap.verbose()) {
                Log.println("Start Threads");
            }

            beltwayHeapSchemeBSS.startGCThreads();

            if (Heap.verbose()) {
                Log.println("Join Threads");
            }
        } else {
            beltwayHeapSchemeBSS.linearScanRegionBelt(beltwayHeapSchemeBSS.getToSpace(), beltwayHeapSchemeBSS.getFromSpace(), beltwayHeapSchemeBSS.getToSpace());
            beltwayHeapSchemeBSS.fillLastTLAB();
        }

        VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();

        verifyBelt(beltwayHeapSchemeBSS.getToSpace());
        InspectableHeapInfo.afterGarbageCollection();

        // Swap semi-spaces. From--> To and To-->From
        beltwayHeapSchemeBSS.getBeltManager().swapBelts(beltwayHeapSchemeBSS.getFromSpace(), beltwayHeapSchemeBSS.getToSpace());
        beltwayHeapSchemeBSS.getToSpace().resetAllocationMark();
        if (Heap.verbose()) {
            Log.print("Finished Collection: ");
            Log.println(collections);
        }
    }
}
