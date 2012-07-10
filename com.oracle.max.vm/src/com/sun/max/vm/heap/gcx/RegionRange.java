/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;

/**
 * A word encoding a fixed size region range. The low-order bits hold the number of regions in the range. The high-order bits
 * hold the identifier of the first region in the range.
 */
public final class RegionRange extends Word {
    protected static final int REGION_ID_SHIFT = Word.width() == 64 ? 32 : 16;
    protected static final Word REGION_ID_MASK = Word.allOnes().asAddress().unsignedShiftedRight(REGION_ID_SHIFT);

    public static final RegionRange INVALID_RANGE = asRegionRange(-1L << REGION_ID_SHIFT);
    @HOSTED_ONLY
    public RegionRange(long value) {
        super(value);
    }

    @INLINE
    protected static RegionRange fromLong(long encodedRange) {
        if (isHosted()) {
            return new RegionRange(encodedRange);
        }
        return asRegionRange(encodedRange);
    }

    @INLINE
    protected static RegionRange fromInt(int encodedRange) {
        if (isHosted()) {
            return new RegionRange(encodedRange);
        }
        FatalError.check(Word.width() == 32, "");
        return asRegionRange(encodedRange);
    }


    @INLINE
    public static RegionRange from(int regionID, int numRegions) {
        if (isHosted()) {
            long encodedRange = regionID;
            encodedRange = (encodedRange << REGION_ID_SHIFT) | numRegions;
            return new RegionRange(encodedRange);
        }
        long range = regionID;
        range = (range << REGION_ID_SHIFT) | numRegions;
        if (Word.width() == 64) {
            return asRegionRange(range);
        }
        return asRegionRange((int) range);
    }

    @INLINE
    public int firstRegion() {
        return asAddress().unsignedShiftedRight(REGION_ID_SHIFT).toInt();
    }

    @INLINE
    public int numRegions() {
        return asAddress().and(REGION_ID_MASK.asAddress()).toInt();
    }

    @INTRINSIC(UNSAFE_CAST) private static RegionRange asRegionRange(long value) { return RegionRange.fromLong(value); }
    @INTRINSIC(UNSAFE_CAST) private static RegionRange asRegionRange(int value) { return RegionRange.fromInt(value); }

}
