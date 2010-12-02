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
import static com.sun.max.vm.VMOptions.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * Descriptor of heap region.
 *
 * @author Laurent Daynes
 */
public class HeapRegionInfo {
    public static final VMSizeOption regionSizeOption = register(new VMSizeOption("-XX:HeapRegionSize", Size.K.times(256), "Heap Region Size"), MaxineVM.Phase.PRISTINE);

    @CONSTANT_WHEN_NOT_ZERO
    static int regionSizeInBytes;
    @CONSTANT_WHEN_NOT_ZERO
    static int regionSizeInWords;
    @CONSTANT_WHEN_NOT_ZERO
    static int log2RegionSizeInBytes;
    @CONSTANT_WHEN_NOT_ZERO
    static int log2RegionSizeInWords;

    @CONSTANT_WHEN_NOT_ZERO
    static int regionAlignmentMask;

    static void initializeConstants() {
        // TODO: this is where it would be interesting to use annotation to ask the boot image
        // generator to keep track of methods that depends on the values below and force a
        // re-compilation of these methods at startup (or opportunistically).

        regionSizeInBytes = regionSizeOption.getValue().toInt();
        log2RegionSizeInBytes = Integer.numberOfTrailingZeros(regionSizeInBytes);
        FatalError.check(regionSizeInBytes == (1 << log2RegionSizeInBytes), "Heap region size must be a power of 2");
        regionSizeInWords = regionSizeInBytes >> Word.widthValue().log2numberOfBytes;
        log2RegionSizeInWords = log2RegionSizeInBytes + Word.widthValue().log2numberOfBytes;
        regionAlignmentMask = regionSizeInBytes - 1;
    }

    static boolean isAligned(Address address) {
        return address.isAligned(regionSizeInBytes);
    }

    static Address regionStart(Address address) {
        return address.and(regionAlignmentMask);
    }

    HeapRegionInfo() {
        // Not a class one can allocate. Allocation is the responsibility of the region table.
    }

    /**
     * Offset to the next HeapRegionInfo. This takes into account heap padding and alignment issue.
     * HeapRegionInfo objects are allocated in a single contiguous area so they can be accessed as a single
     */
    private static final Size OFFSET_TO_NEXT = ClassActor.fromJava(HeapFreeChunk.class).dynamicHub().tupleSize;

    /**
     * Index, in number of minimum object size relative to the beginning of a region to the first free chunk of the region.
     */
    short firstFreeChunkIndex;
    /**
     * Number of fragments not holding live data (i.e., dark-matter + free space).
     * TODO: better define this base on actual use of this field!
     */
    short numFragments;
    /**
     * Space available for allocation. This excludes dark matter than cannot be used
     * for allocation.
     */
    short freeSpace;
    /**
     * Amount of live data.
     */
    short liveData;

    public int liveInWords() {
        return liveData;
    }
    public int darkMatterInWords() {
        return regionSizeInWords - (liveData + freeSpace);
    }
    public int freeSpaceInWords() {
        return freeSpace;
    }

    @INTRINSIC(UNSAFE_CAST)
    private static native HeapRegionInfo asHeapRegionInfo(Object regionInfo);

    @INLINE
    static HeapRegionInfo toHeapRegionInfo(Pointer regionInfoPointer) {
        return asHeapRegionInfo(Reference.fromOrigin(Layout.cellToOrigin(regionInfoPointer)).toJava());
    }
}
