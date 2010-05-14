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
import com.sun.max.vm.*;


public class HeapResizingPolicy {
    /**
     * Percentage of free space below which heap should be expanded.
     */
    final int minFreeSpaceRatioForExpansion = 40;
    /**
     * Percentage of free space above which heap should be shrunk.
     */
    final int maxFreeSpaceRatioForShrinking = 70;

    /**
     * Resize the heap according to policy.
     *
     * @param totalSpace
     * @param spaceLeftAfterGC
     * @param heapSpace
     * @return true if the heap was resized
     */
    public boolean resizeAfterCollection(Size totalSpace, Size spaceLeftAfterGC, ResizableSpace heapSpace) {
        Size min = Size.fromLong((totalSpace.toLong() * minFreeSpaceRatioForExpansion) / 100);
        Size spaceUsedAfterGC = totalSpace.minus(spaceLeftAfterGC);

        if (spaceLeftAfterGC.lessThan(min)) {
            // Use current occupancy to compute heap growth.
            Size minDesiredCapacity =  Size.fromLong((spaceUsedAfterGC.toLong() * 100) / (100 - minFreeSpaceRatioForExpansion));
            Size growth = minDesiredCapacity.minus(totalSpace);
            // Resize take care of rounding up to alignment constraints.
            Size actualGrowth = heapSpace.growAfterGC(growth);
            if (MaxineVM.isDebug()) {
                Log.print("Request to grow the heap: requested ");
                Log.print(growth.toLong());
                Log.print("bytes, obtained ");
                Log.print(actualGrowth.toLong());
                Log.println(" bytes");
            }
            return !actualGrowth.isZero();
        }
        Size max = Size.fromLong((totalSpace.toLong() * maxFreeSpaceRatioForShrinking) / 100);
        if (spaceLeftAfterGC.greaterThan(max)) {
            Size maxDesiredCapacity =  Size.fromLong((spaceUsedAfterGC.toLong() * 100) / (100 - maxFreeSpaceRatioForShrinking));
            Size shrinkage = totalSpace.minus(maxDesiredCapacity);
            return !heapSpace.shrinkAfterGC(shrinkage).isZero();
        }
        return false;
    }
}
