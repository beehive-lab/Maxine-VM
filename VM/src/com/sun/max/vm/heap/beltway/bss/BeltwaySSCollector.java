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

    private void verifyHeap(String when) {
        if (Heap.verbose()) {
            Log.println("Verify Heap");
            Log.println(when);
        }

        ((BeltwayHeapSchemeBSS) heapScheme).bssHeapBoundChecker.reset();
        verifyRoots();
        verifyBelt(getFromSpace());
    }

    private Belt getFromSpace() {
        return ((BeltwayHeapSchemeBSS) heapScheme).getFromSpace();
    }
    private Belt getToSpace() {
        return ((BeltwayHeapSchemeBSS) heapScheme).getToSpace();
    }

    public void run() {
        final Belt fromSpace = getFromSpace();
        final Belt toSpace = getToSpace();
        prologue();

        if (heapScheme.verifyBeforeGC()) {
            verifyHeap("Before GC");
        }

        monitorScheme.beforeGarbageCollection();
        // Start scanning the reachable objects from roots.
        scavengeRoots(fromSpace, toSpace);

        // Evacuate all remaining  "from" objects reachable from the "to" space in the to space.
        evacuateFollowers(fromSpace, toSpace);

        // Process special references (weak, soft, finalizer, etc.)
        heapScheme.processDiscoveredSpecialReferences(fromSpace);

        monitorScheme.afterGarbageCollection();

        // Swap semi-spaces. From--> To and To-->From
        heapScheme.getBeltManager().swapBelts(fromSpace, toSpace);
        getToSpace().resetAllocationMark();

        if (heapScheme.verifyAfterGC()) {
            if (MaxineVM.isDebug()) {
                heapScheme.zapRegion(toSpace);
            }
            verifyHeap("After GC");
        }

        InspectableHeapInfo.afterGarbageCollection();

        if (Heap.verbose()) {
            Log.print("Finished Collection: ");
            Log.println(numCollections);
        }
    }
}
