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
 * Uses two belts (to and from spaces) with one increment each.
 *
 * @author Christos Kotselidis
 */

public class BeltwaySSCollector extends BeltwayCollector {

    protected long collections;

    public BeltwaySSCollector() {
        collections = 0;
    }

    protected void evacuateFollowers(Belt fromSpace, Belt toSpace) {
        if (Heap.verbose()) {
            Log.println("Evacuate reachable...");
        }
        getBeltwayHeapScheme().evacuate(fromSpace, toSpace);
    }

    @Override
    public void run() {
        collections++;
        if (Heap.verbose()) {
            Log.print("Collection: ");
            Log.println(collections);
        }
        final BeltwayHeapSchemeBSS heapScheme = (BeltwayHeapSchemeBSS) getBeltwayHeapScheme();
        final Belt fromSpace = heapScheme.getFromSpace();
        final Belt toSpace = heapScheme.getToSpace();
        if (Heap.verbose()) {
            Log.println("Verify Heap");
            verifyBelt(fromSpace);
        }

        InspectableHeapInfo.beforeGarbageCollection();
        monitorScheme.beforeGarbageCollection();

        // Start scanning the reachable objects from roots.
        heapScheme.getRootScannerUpdater().setFromSpace(fromSpace);
        heapScheme.getRootScannerUpdater().setToSpace(toSpace);
        scavengeBeltRoot(fromSpace, toSpace);

        // Evacuate all remaining objects reachable
        evacuateFollowers(fromSpace, toSpace);
        heapScheme.evacuate(fromSpace, toSpace);
        // beltwayHeapSchemeBSS.fillLastTLAB(); FIXME: do we need this ?

        monitorScheme.afterGarbageCollection();

        verifyBelt(toSpace);
        InspectableHeapInfo.afterGarbageCollection();

        // Swap semi-spaces. From--> To and To-->From
        heapScheme.getBeltManager().swapBelts(fromSpace, toSpace);
        heapScheme.getToSpace().resetAllocationMark();
        if (Heap.verbose()) {
            Log.print("Finished Collection: ");
            Log.println(collections);
        }
    }
}
