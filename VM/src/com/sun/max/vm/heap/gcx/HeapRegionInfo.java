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

import static com.sun.cri.bytecode.Bytecodes.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;


public class HeapRegionInfo {
    /**
     * Offset to the next HeapRegionInfo. This takes into account heap padding and alignment issue.
     * HeapRegionInfo objects are allocated in a single contiguous area so they can be accessed as a single
     */
    private static final Size OFFSET_TO_NEXT = ClassActor.fromJava(HeapFreeChunk.class).dynamicHub().tupleSize;

    @CONSTANT_WHEN_NOT_ZERO
    private static Pointer regionInfoTable;

    @CONSTANT_WHEN_NOT_ZERO
    private static int regionInfoTableSize;

    /**
     * Index of the next region in a linked list of regions.
     */
    int next;
    /**
     * Index of the prev region in a linked list of regions.
     */
    int prev;
    /**
     * Index, in number of minimum object size relative to the beginning of a region to the first free chunk of the region.
     */
    short firstFreeChunkIndex;
    short numHoles;
    short darkMatter;
    short liveData;

    @INTRINSIC(UNSAFE_CAST)
    private static native HeapRegionInfo asHeapRegionInfo(Object regionInfo);

    static HeapRegionInfo toHeapRegionInfo(Pointer heapRegionPointer) {
        return asHeapRegionInfo(Reference.fromOrigin(Layout.cellToOrigin(heapRegionPointer)).toJava());
    }

    static Pointer toHeapRegionPointer(int regionID) {
        return regionInfoTable.plus(OFFSET_TO_NEXT.times(regionID));
    }

    static HeapRegionInfo toHeaRegionInfo(int regionID) {
        return toHeapRegionInfo(toHeapRegionPointer(regionID));
    }

    static interface HeapRegionInfoIterator {
        boolean doRegionInfo(HeapRegionInfo heapRegionInfo);
    }

    int prevRegionID() {
        return prev;
    }

    int nextRegionID() {
        return next;
    }

    HeapRegionInfo prev() {
        return toHeaRegionInfo(prev);
    }

    HeapRegionInfo next() {
        return toHeaRegionInfo(next);
    }
}
