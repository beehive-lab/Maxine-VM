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
import static com.sun.max.vm.MaxineVM.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;

/**
 * A word encoding a fixed size region range. The low-order bits holds the
 *
 * @author Laurent Daynes
 */
public abstract class RegionRange extends Word {
    protected static final int REGION_ID_WIDTH = Word.width() == 64 ? 32 : 16;
    protected static final Word REGION_ID_MASK = Word.allOnes().asAddress().unsignedShiftedRight(REGION_ID_WIDTH);

    public static final RegionRange INVALID_RANGE = asRegionRange(-1L << REGION_ID_WIDTH);

    @INLINE
    protected static RegionRange fromLong(long encodedRange) {
        if (isHosted()) {
            return BoxedRegionRange.fromLong(encodedRange);
        }
        return asRegionRange(encodedRange);
    }

    protected static RegionRange fromInt(int encodedRange) {
        if (isHosted()) {
            return BoxedRegionRange.fromInt(encodedRange);
        }
        FatalError.check(Word.width() == 32, "");
        return asRegionRange(encodedRange);
    }


    @INLINE
    public static RegionRange from(int regionID, int numRegions) {
        if (isHosted()) {
            return BoxedRegionRange.from(regionID, numRegions);
        }
        long range = regionID;
        range = (range << REGION_ID_WIDTH) | numRegions;
        if (Word.width() == 64) {
            return asRegionRange(range);
        }
        return asRegionRange((int) range);
    }

    @INLINE
    public final int firstRegion() {
        return asAddress().unsignedShiftedRight(REGION_ID_WIDTH).toInt();
    }

    @INLINE
    public final int numRegions() {
        return asAddress().and(REGION_ID_MASK.asAddress()).toInt();
    }

    @INTRINSIC(UNSAFE_CAST) private static RegionRange asRegionRange(long value) { return RegionRange.fromLong(value); }
    @INTRINSIC(UNSAFE_CAST) private static RegionRange asRegionRange(int value) { return RegionRange.fromInt(value); }

}
