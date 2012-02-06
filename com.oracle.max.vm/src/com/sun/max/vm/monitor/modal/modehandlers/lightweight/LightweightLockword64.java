/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.monitor.modal.modehandlers.lightweight;

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.monitor.modal.modehandlers.*;

/**
 * Provides common bit-field definitions and method-level access for lightweight lock words.
 */
public class LightweightLockword64 extends HashableLockword64 {

    /*
     * Field layout:
     *
     * bit [63........................................ 1  0]     Shape
     *
     *     [ r. count ][ util  ][  thread ID ][ hash ][m][0]     Lightweight
     *     [                 Undefined               ][m][1]     Inflated
     *
     */

    protected static final int RCOUNT_FIELD_WIDTH = 5; // Must be <= 8 (see incrementCount())
    protected static final int UTIL_FIELD_WIDTH = 9;
    protected static final int THREADID_FIELD_WIDTH = 64 - (RCOUNT_FIELD_WIDTH + UTIL_FIELD_WIDTH + HASH_FIELD_WIDTH + NUMBER_OF_MODE_BITS);

    protected static final int THREADID_SHIFT = HASHCODE_SHIFT + HASH_FIELD_WIDTH;
    protected static final int UTIL_SHIFT = THREADID_SHIFT + THREADID_FIELD_WIDTH;
    protected static final int RCOUNT_SHIFT = UTIL_SHIFT + UTIL_FIELD_WIDTH;

    protected static final Address THREADID_SHIFTED_MASK = Word.allOnes().asAddress().unsignedShiftedRight(64 - THREADID_FIELD_WIDTH);
    protected static final Address UTIL_SHIFTED_MASK = Word.allOnes().asAddress().unsignedShiftedRight(64 - UTIL_FIELD_WIDTH);
    protected static final Address RCOUNT_SHIFTED_MASK = Word.allOnes().asAddress().unsignedShiftedRight(64 - RCOUNT_FIELD_WIDTH);

    protected static final Address RCOUNT_INC_WORD = Address.zero().bitSet(64 - RCOUNT_FIELD_WIDTH);

    @HOSTED_ONLY
    public LightweightLockword64(long value) {
        super(value);
    }

    /**
     * Prints the monitor state encoded in a {@code LightweightLockword64} to the {@linkplain Log log} stream.
     */
    public static void log(LightweightLockword64 lockword) {
        Log.print("LightweightLockword64: ");
        if (lockword.isInflated()) {
            Log.print("inflated=true");
        } else {
            Log.print("inflated=false");
            Log.print(" recursion=");
            Log.print(lockword.getRecursionCount());
            Log.print(" util=");
            Log.print(lockword.getUtil());
            Log.print(" threadID=");
            Log.print(lockword.getThreadID());
            Log.print(" hash=");
            Log.print(lockword.getHashcode());
        }
    }

    /**
     * Boxing-safe cast of a {@code Word} to a {@code LightweightLockword64}.
     *
     * @param word the word to cast
     * @return the cast word
     */
    @INTRINSIC(UNSAFE_CAST)
    public static LightweightLockword64 from(Word word) {
        return new LightweightLockword64(word.value);
    }

    /**
     * Gets the value of this lock word's thread ID field.
     *
     * @return the hashcode field value
     */
    @INLINE
    protected final int getThreadID() {
        return asAddress().unsignedShiftedRight(THREADID_SHIFT).and(THREADID_SHIFTED_MASK).toInt();
    }

    /**
     * Gets the value of this lock word's util field.
     *
     * @return the util field value
     */
    @INLINE
    protected final int getUtil() {
        return asAddress().unsignedShiftedRight(UTIL_SHIFT).and(UTIL_SHIFTED_MASK).toInt();
    }

    /**
     * Tests if this lock word's recursion count field is at its maximum possible value
     * for the field's bit width.
     *
     * @return true if the recursion count field is at its maximum value; false otherwise
     */
    @INLINE
    public final boolean countOverflow() {
        return asAddress().unsignedShiftedRight(RCOUNT_SHIFT).equals(RCOUNT_SHIFTED_MASK);
    }

    /**
     * Tests if this lock word's recursion count field is at its minimum possible value.
     *
     * @return true if the recursion count field is at its minimum value; false otherwise
     */
    @INLINE
    public final boolean countUnderflow() {
        return asAddress().unsignedShiftedRight(RCOUNT_SHIFT).isZero();
    }

    /**
     * Returns a copy of this lock word with the value of its recursion count field incremented.
     *
     * @return a copy lock word with incremented recursion count
     */
    @INLINE
    public final LightweightLockword64 incrementCount() {
        // So long as the rcount field is within a byte boundary, we can just use addition
        // without any endian issues.
        return LightweightLockword64.from(asAddress().plus(RCOUNT_INC_WORD));
    }

    /**
     * Returns a copy of this lock word with the value of its recursion count field decremented.
     *
     * @return a copy lock word with decremented recursion count
     */
    @INLINE
    public final LightweightLockword64 decrementCount() {
        return LightweightLockword64.from(asAddress().minus(RCOUNT_INC_WORD));
    }

    /**
     * Gets the value of this lock word's recursion count field.
     *
     * @return the recursion count
     */
    @INLINE
    public final int getRecursionCount() {
        return asAddress().unsignedShiftedRight(RCOUNT_SHIFT).toInt();
    }
}
