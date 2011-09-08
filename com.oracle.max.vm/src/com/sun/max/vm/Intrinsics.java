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

import static com.sun.cri.bytecode.Bytecodes.*;
import static com.sun.cri.bytecode.Bytecodes.UnsignedComparisons.*;

import com.sun.cri.bytecode.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * A set of methods {@linkplain INTRINSIC intrinsified} via extended bytecodes.
 * Note that this is not the complete set of VM method annotated with {@link INTRINSIC}
 * as this annotation may be more naturally applied elsewhere (e.g.
 * methods in the {@link Pointer}, {@link Address} and {@link UnsafeCast} classes).
 */
public class Intrinsics {

    @INTRINSIC(MOV_F2I)
    public static int floatToInt(float value) {
        return Float.floatToRawIntBits(value);
    }

    @INTRINSIC(MOV_D2L)
    public static long doubleToLong(double value) {
        return Double.doubleToRawLongBits(value);
    }

    @INTRINSIC(MOV_I2F)
    public static float intToFloat(int value) {
        return Float.intBitsToFloat(value);
    }

    @INTRINSIC(MOV_L2D)
    public static double longToDouble(long value) {
        return Double.longBitsToDouble(value);
    }

    /**
     * Returns the index of the least significant bit set in a given value.
     *
     * @param value the value to scan for the least significant bit
     * @return the index of the least significant bit within {@code value} or {@code -1} if {@code value == 0}
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
     * Returns the index to the most significant bit set in a given value.
     *
     * @param value the value to scan for the most significant bit
     * @return the index to the most significant bit within {@code value} or {@code -1} if {@code value == 0}
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
     * If the CPU supports it, then this builtin issues an instruction that improves the performance of spin loops by
     * providing a hint to the processor that the current thread is in a spin loop. The processor may use this to
     * optimize power consumption while in the spin loop.
     *
     * If the CPU does not support such an instruction, then nothing is emitted for this builtin.
     */
    @INTRINSIC(PAUSE)
    public static native void pause();

    @INTRINSIC(ALLOCA)
    public static native Pointer stackAllocate(int size);

    @INTRINSIC(UCMP | (ABOVE_EQUAL << 8))
    public static boolean aboveEqual(int value1, int value2) {
        final long unsignedInt1 = value1 & 0xFFFFFFFFL;
        final long unsignedInt2 = value2 & 0xFFFFFFFFL;
        return unsignedInt1 >= unsignedInt2;
    }

    @INTRINSIC(UCMP | (ABOVE_THAN << 8))
    public static boolean aboveThan(int value1, int value2) {
        final long unsignedInt1 = value1 & 0xFFFFFFFFL;
        final long unsignedInt2 = value2 & 0xFFFFFFFFL;
        return unsignedInt1 > unsignedInt2;

    }

    @INTRINSIC(UCMP | (BELOW_EQUAL << 8))
    public static boolean belowEqual(int value1, int value2) {
        final long unsignedInt1 = value1 & 0xFFFFFFFFL;
        final long unsignedInt2 = value2 & 0xFFFFFFFFL;
        return unsignedInt1 <= unsignedInt2;
    }

    @INTRINSIC(UCMP | (BELOW_THAN << 8))
    public static boolean belowThan(int value1, int value2) {
        final long unsignedInt1 = value1 & 0xFFFFFFFFL;
        final long unsignedInt2 = value2 & 0xFFFFFFFFL;
        return unsignedInt1 < unsignedInt2;
    }

    @INTRINSIC(STACKHANDLE)
    public static native Pointer stackHandle(int i);

    @INTRINSIC(STACKHANDLE)
    public static native Pointer stackHandle(Reference ref);

    @INTRINSIC(READBIT | VMRegister.LATCH << 8)
    public static native boolean readLatchBit(int offset, int bit);

}
