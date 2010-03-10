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

import com.sun.max.memory.*;
import com.sun.max.unsafe.*;

/**
 * A marking algorithm that uses a three-color mark-bitmap with a fixed-size tiny marking stack, a rescan map, and a finger.
 * The three-color mark-bitmap consumes as much space overhead as a two-color mark bitmap (see {@link ThreeColorMarkBitmap}.
 * The algorithm works as follows:
 *
 * @author Laurent Daynes
 */
public class HeapMarker {

    private static final int WORDS_COVERED_PER_BIT = 1;

    /**
     * Three color mark bitmap for the covered area.
     */
    final ThreeColorMarkBitmap markBitmaps;

    private final MarkingStack markingStack;

    /**
     * Finger that points to the rightmost visited (black) object.
     */
    private Address finger;

    /**
     * Leftmost marked position.
     */
    private Address leftmost;

    public HeapMarker() {
        markBitmaps = new ThreeColorMarkBitmap(WORDS_COVERED_PER_BIT);
        markingStack = new MarkingStack();
    }

    /**
     * Returns max amount of memory needed for a max heap size.
     * Let the HeapScheme decide where to allocate.
     *
     * @param maxHeapSize
     * @return
     */
    public Size memoryRequirement(Size maxHeapSize) {
        return markBitmaps.bitmapSize(maxHeapSize);
    }


    public void initialize(RuntimeMemoryRegion coveredArea, Address memory, Size size) {
        markBitmaps.initialize(coveredArea, memory, size);
        markingStack.initialize();
    }

}
