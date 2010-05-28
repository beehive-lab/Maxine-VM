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
package com.sun.max.vm.heap.beltway;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.StopTheWorldGCDaemon.*;
import com.sun.max.vm.monitor.*;

/**
 *
 * @author Laurent Daynes
 * @author Christos Kotselidis
 */

public abstract class BeltwayCollector extends Collector {

    @CONSTANT_WHEN_NOT_ZERO
    protected BeltwayHeapScheme heapScheme;

    @CONSTANT_WHEN_NOT_ZERO
    protected MonitorScheme monitorScheme;

    protected long numCollections;

    protected final String collectorName;

    public BeltwayCollector(String name) {
        numCollections = 0;
        collectorName = name;
    }

    /**
     * Prototyping time initialization.
     * @param heapScheme
     */
    @HOSTED_ONLY
    public void initialize(BeltwayHeapScheme heapScheme) {
        // Initialize useful short cuts.
        this.heapScheme = heapScheme;
        monitorScheme = VMConfiguration.target().monitorScheme();
    }

    public BeltwayHeapScheme getBeltwayHeapScheme() {
        return heapScheme;
    }

    /**
     * Verifies that all the references from a belt are valid, i.e., points to used area of the heap.
     * @param belt
     */
    protected void verifyBelt(Belt belt) {
        heapScheme.heapVerifier.verifyHeapRange(belt.start(), belt.getAllocationMark(), heapScheme.heapBoundChecker());
    }

    /**
     * Verify all the roots of the belts. This includes stacks. monitors, boot region, boot code region and dynamic code regions.
     */
    protected void verifyRoots() {
        final BeltwayHeapVerifier verifier = heapScheme.heapVerifier;
        final HeapBoundChecker heapBoundChecker = heapScheme.heapBoundChecker();
        verifier.verifyThreadAndStack(heapBoundChecker);
        verifier.verifyHeapRange(Heap.bootHeapRegion.start(), Heap.bootHeapRegion.getAllocationMark(), heapBoundChecker);
        verifier.verifyHeapRange(Code.bootCodeRegion.start(), Code.bootCodeRegion.getAllocationMark(), heapBoundChecker);
        verifier.verifyCodeRegions(heapBoundChecker);
    }

    protected void prologue() {
        numCollections++;
        heapScheme.resetTLABs();
        HeapScheme.Inspect.notifyGCStarted();

        if (Heap.verbose()) {
            Log.print(collectorName);
            Log.print(" Collection: ");
            Log.println(numCollections);
        }
    }

    protected void epilogue() {
        if (Heap.verbose()) {
            Log.print("Finished ");
            Log.print(collectorName);
            Log.print(" Collection: ");
            Log.println(numCollections);
        }
        HeapScheme.Inspect.notifyGCCompleted();
    }

    /**
     * Evacuate objects from the "from" belt directly referenced by roots into the "to" belt.
     * @param from
     * @param to
     */
    protected void scavengeRoots(Belt from, Belt to) {
        if (Heap.verbose()) {
            Log.println("Scavenge Roots");
        }
        heapScheme.scavengeRoot(from, to);
    }

    /**
     * Evacuate remaining objects of the "from" belt reachable from the "to" belt.
     * @param from the evacuated belt
     * @param to the belt where objects are evacuated to
     */
    protected void evacuateFollowers(Belt from, Belt to) {
        if (Heap.verbose()) {
            Log.println("Evacuate Followers");
        }
        heapScheme.evacuate(from, to);
    }

    protected void printBeltInfo(String beltName, Belt belt) {
        Log.print(beltName);
        Log.print(" Start: ");
        Log.println(belt.start());
        Log.print(beltName);
        Log.print(" Mark: ");
        Log.println(belt.getAllocationMark());
        Log.print(beltName);
        Log.print(" End: ");
        Log.println(belt.end());
    }

}
