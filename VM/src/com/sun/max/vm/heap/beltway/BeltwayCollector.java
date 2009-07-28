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

/**
 * This class defines a specialized over the generic Collector Runnable which performs "an action" over a "a space".
 *
 * @author Christos Kotselidis
 */

public class BeltwayCollector implements Runnable {

    protected Runnable gcImpl;

    private BeltwayHeapScheme beltwayHeapScheme;

    protected final MonitorScheme monitorScheme = VMConfiguration.target().monitorScheme();

    public BeltwayCollector() {
    }

    public void setRunnable(Runnable gcImpl) {
        this.gcImpl = gcImpl;
    }

    public void run() {
        gcImpl.run();
    }

    public void setBeltwayHeapScheme(BeltwayHeapScheme beltwayHeapScheme) {
        this.beltwayHeapScheme = beltwayHeapScheme;
    }

    public BeltwayHeapScheme getBeltwayHeapScheme() {
        return beltwayHeapScheme;
    }

    protected void verifyBelt(Belt belt) {
        beltwayHeapScheme.getVerifier().verifyHeap(belt.start(), belt.getAllocationMark(), beltwayHeapScheme.getBeltManager().getApplicationHeap());
    }

    /**
     * Scavenge the stacks, boot, code root for the "scanned" belt and evacuate objects to the "evacuation" belt.
     *
     * @param scanned
     * @param evacuation
     */
    protected void scavengeBeltRoot(Belt scanned, Belt evacuation) {
        final BeltwayHeapScheme beltwayHeapSchemeGen = getBeltwayHeapScheme();
        if (Heap.verbose()) {
            Log.println("Scan Roots ");
        }

        beltwayHeapSchemeGen.getRootScannerUpdater().run(scanned, evacuation);

        if (Heap.verbose()) {
            Log.println("Scan Boot Heap");
        }

        beltwayHeapSchemeGen.scanBootHeap(scanned, evacuation);
        if (Heap.verbose()) {
            Log.println("Scan Code");
        }
        beltwayHeapSchemeGen.scanCode(scanned, evacuation);
    }


}
