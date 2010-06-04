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
 * Heap Sweeper abstract class. Passed to HeapMarker instances to sweep the heap.
 * The interface allows both precise and imprecise sweeping. Precise sweeping inspects
 * every marked location to determine the end of the live data and identified every
 * dead space. Imprecise sweeping only inspects marked location separated by a minimum
 * distance, thus avoiding inspecting object when the size of a potential free chunk is
 * too small to be of interest to the free space manager.
 *
 * @author Laurent Daynes.
 */
public abstract class HeapSweeper {
    /**
     * Invoked when doing precise sweeping on the first black object following the pointer last returned by this method.
     * @param liveObject a pointer to a live cell in the heap
     * @return a pointer to the position in the heap where to resume sweeping.
     */
    public abstract Pointer processLiveObject(Pointer liveObject);

    /**
     * Invoked when doing imprecise sweeping to process an large interval between to marked locations.
     * Imprecise heap sweeping ignores any space before two live objects smaller than a specified amount of space.
     * When the distance between two live marks is large enough to indicate a potentially large chunk of free space,
     * the sweeper invoke this method.
     *
     * @param leftLiveObject
     * @param rightLiveObject
     * @return
     */
    public abstract Pointer processLargeGap(Pointer leftLiveObject, Pointer rightLiveObject);

    /**
     * Invoke to record a know chunk of free space.
     * Used both by precise and imprecise sweeper, typically to record the unmarked space
     * at both end of the traced space.
     * @param freeChunk
     * @param size
     */
    public abstract void processDeadSpace(Address freeChunk, Size size);
}
