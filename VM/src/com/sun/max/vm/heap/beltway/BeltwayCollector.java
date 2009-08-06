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

import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.tele.*;

/**
 *
 * @author Laurent Daynes
 * @author Christos Kotselidis
 */

public class BeltwayCollector {

    protected final BeltwayHeapScheme heapScheme;

    protected final MonitorScheme monitorScheme = VMConfiguration.target().monitorScheme();

    protected long numCollections;

    protected final String collectorName;

    public BeltwayCollector(String name) {
        numCollections = 0;
        collectorName = name;
        final HeapScheme scheme = VMConfiguration.target().heapScheme();
        FatalError.check(scheme instanceof BeltwayHeapScheme, "Heap scheme must be a Beltway Heap Scheme");
        heapScheme = (BeltwayHeapScheme) scheme;
    }

    public BeltwayHeapScheme getBeltwayHeapScheme() {
        return heapScheme;
    }

    protected void verifyBelt(Belt belt) {
        heapScheme.heapVerifier.verifyHeap(belt.start(), belt.getAllocationMark(), heapScheme.getBeltManager().getApplicationHeap());
    }

    protected void prologue() {
        numCollections++;
        heapScheme.resetTLABs();
        InspectableHeapInfo.beforeGarbageCollection();

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
        InspectableHeapInfo.afterGarbageCollection();
    }

    /**
     * Evacuate remaining objects of the "from" belt reachable from the "to" belt.
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
