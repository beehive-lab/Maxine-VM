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
package com.sun.max.vm;

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.intrinsics.*;
import com.sun.max.vm.reference.*;

/**
 * A set of methods {@linkplain INTRINSIC intrinsified} via extended bytecodes.
 * Note that this is not the complete set of VM method annotated with {@link INTRINSIC}
 * as this annotation may be more naturally applied elsewhere (e.g.
 * methods in the {@link Pointer}, {@link Address} and {@link UnsafeCast} classes).
 */
public class Intrinsics {

    /**
     * @see MaxineIntrinsicIDs#LSB
     */
    @INTRINSIC(LSB)
    public static int leastSignificantBit(Word value) {
        long l = value.asAddress().toLong();
        if (l == 0) {
            return -1;
        }
        return Long.numberOfTrailingZeros(l);
    }

    /**
     * @see MaxineIntrinsicIDs#MSB
     */
    @INTRINSIC(MSB)
    public static int mostSignificantBit(Word value) {
        long l = value.asAddress().toLong();
        if (l == 0) {
            return -1;
        }
        return Long.numberOfTrailingZeros(l);
    }

    /**
     * @see MaxineIntrinsicIDs#ALLOCA
     */
    @INTRINSIC(ALLOCA)
    public static native Pointer stackAllocate(@INTRINSIC.Constant int size);

    /**
     * @see MaxineIntrinsicIDs#STACKHANDLE
     */
    @INTRINSIC(STACKHANDLE)
    public static native Pointer stackHandle(int value);

    /**
     * @see MaxineIntrinsicIDs#STACKHANDLE
     */
    @INTRINSIC(STACKHANDLE)
    public static native Pointer stackHandle(Reference value);

    /**
     * @see MaxineIntrinsicIDs#PAUSE
     */
    @INTRINSIC(PAUSE)
    public static void pause() {
    }

    /**
     * @see MaxineIntrinsicIDs#BREAKPOINT_TRAP
     */
    @INTRINSIC(BREAKPOINT_TRAP)
    public static native void breakpointTrap();

    /**
     * @see MaxineIntrinsicIDs#IFLATCHBITREAD
     */
    @INTRINSIC(IFLATCHBITREAD)
    public static native boolean readLatchBit(@INTRINSIC.Constant int offset, @INTRINSIC.Constant int bit);

}
