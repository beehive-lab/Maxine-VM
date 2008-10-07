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
/*VCSID=7f4c511a-2785-420c-81aa-11a05254795a*/
package com.sun.max.vm.heap.sequential.Beltway.BeltwayBSS;

import com.sun.max.vm.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.sequential.Beltway.*;
import com.sun.max.vm.tele.*;

/**
 * @author Christos Kotselidis
 */

public class BeltwaySSCollector implements Runnable {

    // Dependency injection of the corresponding heap scheme
    private static BeltwayHeapScheme _beltwayHeapScheme;
    private static long _collections;

    public void setBeltwayHeapScheme(BeltwayHeapScheme beltwayHeapScheme) {
        _beltwayHeapScheme = beltwayHeapScheme;
    }

    public BeltwayHeapScheme getBeltwayHeapScheme() {
        return _beltwayHeapScheme;
    }

    public BeltwaySSCollector() {
    }

    @Override
    public void run() {
        _collections++;
        if (Heap.verbose()) {
            Debug.print("Collection: ");
            Debug.println(_collections);
        }
        final BeltwayHeapSchemeBSS beltwayHeapSchemeBSS = (BeltwayHeapSchemeBSS) _beltwayHeapScheme;
        if (Heap.verbose()) {
            Debug.println("Verify Heap");
            _beltwayHeapScheme.getVerifier().verifyHeap(beltwayHeapSchemeBSS.getFromSpace().start(), beltwayHeapSchemeBSS.getFromSpace().getAllocationMark(), BeltManager.getApplicationHeap());
        }

        TeleHeap.beforeGarbageCollection();
        VMConfiguration.hostOrTarget().monitorScheme().beforeGarbageCollection();

        if (Heap.verbose()) {
            Debug.println("Scanning roots...");
        }

        // Start scanning the reachable objects from my roots.
        beltwayHeapSchemeBSS.getRootScannerUpdater().setFromSpace(beltwayHeapSchemeBSS.getFromSpace());
        beltwayHeapSchemeBSS.getRootScannerUpdater().setToSpace(beltwayHeapSchemeBSS.getToSpace());
        beltwayHeapSchemeBSS.getRootScannerUpdater().run();

        if (Heap.verbose()) {
            Debug.println("Scanning boot heap...");
        }

        beltwayHeapSchemeBSS.scanBootHeap(beltwayHeapSchemeBSS.getFromSpace(), beltwayHeapSchemeBSS.getToSpace());

        if (Heap.verbose()) {
            Debug.println("Scanning code...");
        }

        beltwayHeapSchemeBSS.scanCode(beltwayHeapSchemeBSS.getFromSpace(), beltwayHeapSchemeBSS.getToSpace());

        if (Heap.verbose()) {
            Debug.println("Moving recheable...");
        }

        if (BeltwayConfiguration._parallelScavenging) {
            beltwayHeapSchemeBSS.fillLastTLAB();
            beltwayHeapSchemeBSS.initializeGCThreads(beltwayHeapSchemeBSS, beltwayHeapSchemeBSS.getToSpace(), beltwayHeapSchemeBSS.getFromSpace());
            VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();
            if (Heap.verbose()) {
                Debug.println("Start Threads");
            }

            beltwayHeapSchemeBSS.startGCThreads();

            if (Heap.verbose()) {
                Debug.println("Join Threads");
            }
        } else {
            beltwayHeapSchemeBSS.linearScanRegionBelt(beltwayHeapSchemeBSS.getToSpace(), beltwayHeapSchemeBSS.getFromSpace(), beltwayHeapSchemeBSS.getToSpace());
            beltwayHeapSchemeBSS.fillLastTLAB();
        }

        beltwayHeapSchemeBSS.wipeMemory(beltwayHeapSchemeBSS.getFromSpace());

        VMConfiguration.hostOrTarget().monitorScheme().afterGarbageCollection();
        beltwayHeapSchemeBSS.getVerifier().verifyHeap(beltwayHeapSchemeBSS.getToSpace().start(), beltwayHeapSchemeBSS.getToSpace().getAllocationMark(), BeltManager.getApplicationHeap());
        TeleHeap.afterGarbageCollection();

        // Swap semi-spaces. From--> To and To-->From
        beltwayHeapSchemeBSS.getBeltManager().swapBelts(beltwayHeapSchemeBSS.getFromSpace(), beltwayHeapSchemeBSS.getToSpace());
        beltwayHeapSchemeBSS.getToSpace().resetAllocationMark();
        if (Heap.verbose()) {
            Debug.print("Finished Collection: ");
            Debug.println(_collections);
        }
    }
}
