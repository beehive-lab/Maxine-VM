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
 * @author Laurent Daynes
 * @author Christos Kotselidis
 */

public class BeltwaySSCollector extends BeltwayCollector implements Runnable {

    public BeltwaySSCollector() {
        super("BSS");
    }

    protected BeltwaySSCollector(String name) {
        super(name);
    }

    private void verifyHeap(String when,  BeltwayHeapSchemeBSS ssHeapScheme, Belt fromSpace) {
        if (Heap.verbose()) {
            Log.println("Verify Heap");
            Log.println(when);
        }

        ssHeapScheme.bssHeapBoundChecker.reset();
        verifyRoots();
        verifyBelt(fromSpace);
    }

    public void run() {
        final BeltwayHeapSchemeBSS ssHeapScheme = (BeltwayHeapSchemeBSS) heapScheme;
        final Belt fromSpace = ssHeapScheme.getFromSpace();
        final Belt toSpace = ssHeapScheme.getToSpace();
        prologue();

        if (ssHeapScheme.verifyBeforeGC()) {
            verifyHeap("Before GC", ssHeapScheme, fromSpace);
        }

        monitorScheme.beforeGarbageCollection();
        // Start scanning the reachable objects from roots.
        ssHeapScheme.scavengeRoot(fromSpace, toSpace);

        // Evacuate all remaining  "from" object reachable from the "to" space in the to space.
        evacuateFollowers(fromSpace, toSpace);

        monitorScheme.afterGarbageCollection();

        // Swap semi-spaces. From--> To and To-->From
        ssHeapScheme.getBeltManager().swapBelts(fromSpace, toSpace);
        ssHeapScheme.getToSpace().resetAllocationMark();

        if (ssHeapScheme.verifyAfterGC()) {
            if (MaxineVM.isDebug()) {
                heapScheme.zapRegion(toSpace);
            }
            verifyHeap("After GC", ssHeapScheme, fromSpace);
        }

        InspectableHeapInfo.afterGarbageCollection();

        if (Heap.verbose()) {
            Log.print("Finished Collection: ");
            Log.println(numCollections);
        }
    }
}
