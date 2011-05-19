/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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
