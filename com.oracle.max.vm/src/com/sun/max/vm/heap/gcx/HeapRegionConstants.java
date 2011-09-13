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

import static com.sun.max.vm.VMOptions.*;

import com.sun.max.annotate.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.runtime.*;

/**
 * Centralize constants and convenience related to fix-size heap region.
 */
public final class HeapRegionConstants {
    public static final int INVALID_REGION_ID = -1;
    public static final VMSizeOption regionSizeOption = register(new VMSizeOption("-XX:HeapRegionSize=", Size.K.times(256), "Heap Region Size"), MaxineVM.Phase.PRISTINE);

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

    static void initializeConstants() {
        // TODO: this is where it would be interesting to use annotation to ask the boot image
        // generator to keep track of methods that depends on the values below and force a
        // re-compilation of these methods at startup (or opportunistically).

        regionSizeInBytes = regionSizeOption.getValue().toInt();
        log2RegionSizeInBytes = Integer.numberOfTrailingZeros(regionSizeInBytes);
        FatalError.check(regionSizeInBytes == (1 << log2RegionSizeInBytes), "Heap region size must be a power of 2");
        regionSizeInWords = regionSizeInBytes >> Word.widthValue().log2numberOfBytes;
        log2RegionSizeInWords = log2RegionSizeInBytes - Word.widthValue().log2numberOfBytes;
        regionAlignmentMask = Address.fromInt(regionSizeInBytes).minus(1);
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
    static int numberOfRegions(Size sizeInBytes) {
        return sizeInBytes.alignUp(regionSizeInBytes).unsignedShiftedRight(log2RegionSizeInBytes).toInt();
    }

    static void validate() {
        FatalError.check((regionSizeInBytes == (1 << log2RegionSizeInBytes)) &&
                        ((regionSizeInBytes % Platform.platform().pageSize) == 0),
                        "Region size must be a power of 2 and an integral number of platform pages");
    }
}
