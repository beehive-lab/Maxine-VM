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
package com.sun.max.vm.heap.gcx;

import com.sun.max.unsafe.*;

/**
 * A region-based space supporting the sweeping interface.
 * The allocator is number of (potentially) non-contiguous
 * fixed-size regions. Each region maintains a list of free chunks of minimum size, and occupancy statistics.
 *
 *
 * @author Laurent Daynes
 */
public class RegionBasedFirstFitSpace extends HeapSweeper implements ResizableSpace {
    /**
     * Pointer to the head of the freelist of the current region.
     */
    Pointer currentRegionFreelist;

    int currentRegionId;

    @Override
    public Size beginSweep(boolean precise) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Size endSweep() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void processDeadSpace(Address freeChunk, Size size) {
        // TODO Auto-generated method stub

    }

    @Override
    public Pointer processLargeGap(Pointer leftLiveObject, Pointer rightLiveObject) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Pointer processLiveObject(Pointer liveObject) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Size growAfterGC(Size delta) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Size shrinkAfterGC(Size delta) {
        // TODO Auto-generated method stub
        return null;
    }

}
