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

import com.sun.max.annotate.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.*;
import com.sun.max.vm.runtime.*;

/**
 * Centralize constants and convenience related to fix-size heap region.
 * By default, the heap region manager tries to maintain the total number of regions below a specified threshold.
 * Given a maximum heap size, it picks a region size that keeps the region table under the maximum length, starting with the default region size.
 * Region sizes are always a power of 2, and the heap size is rounded up to a region size boundary.
 */
public final class HeapRegionConstants {
    public static final int INVALID_REGION_ID = -1;

    private static Size DefaultHeapRegionSize = Size.K.times(256);
    private static Size MaxHeapRegionSize = Size.M.times(4);
    private static int MaxNumberOfRegions = 4096;
    static {
        VMOptions.addFieldOption("-XX:", "MaxNumberOfRegions", HeapRegionConstants.class, "Maximum number of heap regions", Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "DefaultHeapRegionSize", HeapRegionConstants.class, "Default heap region size", Phase.PRISTINE);
        VMOptions.addFieldOption("-XX:", "MaxHeapRegionSize", HeapRegionConstants.class, "Maximum number of regions", Phase.PRISTINE);
    }

    @INSPECTED
    @CONSTANT_WHEN_NOT_ZERO
    static int regionSizeInBytes;
    @CONSTANT_WHEN_NOT_ZERO
    static int regionSizeInWords;
    @CONSTANT_WHEN_NOT_ZERO
    static int log2RegionSizeInBytes;
    @CONSTANT_WHEN_NOT_ZERO
    static int log2RegionSizeInWords;
    @CONSTANT_WHEN_NOT_ZERO
    static Address regionAlignmentMask;

    /*
     * Support for inspection.
     */
    @HOSTED_ONLY
    public static void initializeWithConstants(int defaultRegionSizeInBytes) {
        initializeConstants(defaultRegionSizeInBytes);
    }

    private static void initializeConstants(int defaultRegionSizeInBytes) {
        regionSizeInBytes = defaultRegionSizeInBytes;
        log2RegionSizeInBytes = Integer.numberOfTrailingZeros(regionSizeInBytes);
        FatalError.check((regionSizeInBytes == (1 << log2RegionSizeInBytes)) &&
                        ((regionSizeInBytes % Platform.platform().pageSize) == 0),
                        "Region size must be a power of 2 and an integral number of platform pages");
        regionSizeInWords = regionSizeInBytes >> Word.widthValue().log2numberOfBytes;
        log2RegionSizeInWords = log2RegionSizeInBytes - Word.widthValue().log2numberOfBytes;
        regionAlignmentMask = Address.fromInt(regionSizeInBytes).minus(1);
    }

    static void initializeConstants() {
        // TODO: this is where it would be interesting to use annotation to ask the boot image
        // generator to keep track of methods that depends on the values below and force a
        // re-compilation of these methods at startup (or opportunistically).
        initializeConstants(DefaultHeapRegionSize.toInt());
    }

    static boolean isAligned(Address address) {
        return address.isAligned(regionSizeInBytes);
    }

    static Address regionStart(Address address) {
        return address.and(regionAlignmentMask.not());
    }

    /**
     * Compute the number of regions needed to hold the number of bytes.
     * @param sizeInBytes size in bytes
     * @return a number of regions.
     */
    public static int numberOfRegions(Size sizeInBytes) {
        return sizeInBytes.alignUp(regionSizeInBytes).unsignedShiftedRight(log2RegionSizeInBytes).toInt();
    }

    /**
     *  Initialize heap region constants (size and log 2) based on the maximum heap size specified at VM startup.
     * The size of heap regions is computed from the specified maximum heap size so as to keep the region table bounded and to better adapt
     * the region size to the heap size (in particular, very large heap command larger region size).
     * The minimum region size, and the one used by default, is specified by the {@link #DefaultHeapRegionSize} option.
     * The maximum region size is specified by the {@link #MaxHeapRegionSize} option.
     * The maximum length of the {@link RegionTable}  is specified by {@value #MaxNumberOfRegions} option.
     *
     * @param maxHeapSize the maximum heap size specified at VM startup
     */
    static void initializeConstants(Size maxHeapSize) {
        Size regionSize = DefaultHeapRegionSize;
        if (maxHeapSize.dividedBy(regionSize).greaterThan(MaxNumberOfRegions)) {
            long smallestRegionSize = maxHeapSize.dividedBy(MaxNumberOfRegions).toLong();
            // Get power of 2 nearest to the smallest region size
            long roundedRegionSize = Long.highestOneBit(smallestRegionSize);
            if (roundedRegionSize < smallestRegionSize) {
                // Get next power of two
                roundedRegionSize <<= 1;
            }
            regionSize = Size.fromLong(roundedRegionSize);
            if (regionSize.greaterThan(MaxHeapRegionSize)) {
                regionSize = MaxHeapRegionSize;
            }
        }
        initializeConstants(regionSize.toInt());
    }
}
