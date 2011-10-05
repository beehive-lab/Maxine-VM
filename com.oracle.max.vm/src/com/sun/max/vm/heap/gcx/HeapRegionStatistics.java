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
import com.sun.max.vm.runtime.*;

/**
 * Statistics on heap regions free space and fragmentation.
 */
public final class HeapRegionStatistics {
    /**
     * Log2 of the smallest fragment size (smallest space reclaimable by the GC).
     */
    final int log2MinFragmentSize;
    /**
     * Log2 of the largest free chunk size (i.e., heap region size).
     */
    final int log2LargestChunkSize;
    /**
     * Histogram of fragment sizes. Each entries correspond to a power of 2 of fragment size, starting at the minimum size for a fragment.
     * Entry at index i records the number of fragments of size between 2^i and 2^(i+1) -1.
     */
    final int [] fragmentSizes;

    /**
     * Histogram of free space within regions. Entry at index i records the number of regions with free space amount comprises between 2^i and 2^(i+1) -1.
     */
    final int [] freeSpaceSizes;

    /**
     * Histogram of number of free chunks per region.
     */
    final int [] regionsFragmentation;

    /**
     * Private region info iterator.
     */
    private final HeapRegionInfoIterable regionInfoIterable;

    private int sizeBin(int size) {
        // bin index of a give size is the log 2 of its highest bit.
        return 31 - Integer.numberOfLeadingZeros(size);
    }

    public HeapRegionStatistics(Size minFragmentSize) {
        log2MinFragmentSize =  sizeBin(minFragmentSize.toInt());
        log2LargestChunkSize = HeapRegionConstants.log2RegionSizeInBytes;
        // Maximum fragmentation for a region of size R when the smallest fragment size is F is (R / F) / 2.
        final int maxFragmentation = 1 << (log2LargestChunkSize - (log2MinFragmentSize + 1));
        regionInfoIterable = new HeapRegionInfoIterable();
        // We over allocate to allow fast indexing.
        fragmentSizes = new int[log2LargestChunkSize + 1];
        freeSpaceSizes = new int[log2LargestChunkSize + 1];
        regionsFragmentation = new int[maxFragmentation + 1];
    }

    public void clear() {
        for (int i = log2MinFragmentSize; i <= log2MinFragmentSize; i++) {
            fragmentSizes[i] = 0;
            freeSpaceSizes[i] = 0;
        }
        for (int i = 0; i < regionsFragmentation.length; i++) {
            regionsFragmentation[i] = 0;
        }
    }

    /**
     * Add statistics for the specified region.
     * @param rinfo a heap region info
     */
    public void add(HeapRegionInfo rinfo) {
        if (MaxineVM.isDebug()) {
            FatalError.check(rinfo.hasFreeChunks() || (rinfo.isEmpty() && rinfo.freeSpace == 0) || (rinfo.isFull() && rinfo.freeSpace == 0), "Invalid RegionInfo");
        }
        regionsFragmentation[rinfo.numFreeChunks]++;
        if (rinfo.hasFreeChunks()) {
            freeSpaceSizes[sizeBin(rinfo.freeBytesInChunks())]++;
        } else if (rinfo.isEmpty()) {
            freeSpaceSizes[log2LargestChunkSize]++;
        } else {
            freeSpaceSizes[0]++;
        }
    }

    public void addFull(HeapRegionInfo rinfo) {
        add(rinfo);
        if (rinfo.hasFreeChunks()) {
            HeapFreeChunk c = HeapFreeChunk.toHeapFreeChunk(rinfo.firstFreeBytes());
            while (c != null) {
                fragmentSizes[sizeBin(c.size.toInt())]++;
                c = c.next;
            }
        }
    }

    public void doStats(HeapRegionList regionList) {
        regionInfoIterable.initialize(regionList);
        regionInfoIterable.reset();
        while (regionInfoIterable.hasNext()) {
            add(regionInfoIterable.next());
        }
    }

    public void doStats(HeapAccount<? extends HeapAccountOwner>heapAccount) {
        doStats(heapAccount.allocatedRegions().regionList);
    }

    public void doFullStats(HeapRegionList regionList) {
        regionInfoIterable.initialize(regionList);
        regionInfoIterable.reset();
        while (regionInfoIterable.hasNext()) {
            addFull(regionInfoIterable.next());
        }
    }

    public void doFullStats(HeapAccount<? extends HeapAccountOwner>heapAccount) {
        doFullStats(heapAccount.allocatedRegions().regionList);
    }

    public void dump() {
        Log.println("[ min, max ]           :  # holes         # regions");
        for (int i = log2MinFragmentSize; i < log2LargestChunkSize; i++) {
            Log.print(" ["); Log.print(1 << i); Log.print(", "); Log.print((1 << i + 1) - 1); Log.print(" ]  : ");
            Log.print(fragmentSizes[i]);
            Log.print("            ");
            Log.println(freeSpaceSizes[i]);
        }
        Log.print("empty regions ("); Log.print(1 << log2LargestChunkSize); Log.print(") : ");  Log.println(freeSpaceSizes[log2LargestChunkSize]);
        Log.print("full regions : ");  Log.println(freeSpaceSizes[0]);
        Log.println(" # fragments             : # regions");
        for (int i = 0; i < regionsFragmentation.length; i++) {
            int numRegions = regionsFragmentation[i];
            if (numRegions > 0) {
                Log.print(i); Log.print(" : "); Log.println(numRegions);
            }
        }
    }

    public void reportStats(HeapAccount<? extends HeapAccountOwner>heapAccount) {
        clear();
        doFullStats(heapAccount);
        dump();
    }
}
