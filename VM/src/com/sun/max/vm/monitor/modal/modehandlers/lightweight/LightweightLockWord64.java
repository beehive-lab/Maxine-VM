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
package com.sun.max.vm.monitor.modal.modehandlers.lightweight;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.unsafe.box.*;
import com.sun.max.vm.monitor.modal.modehandlers.*;

/**
 * Provides common bit-field definitions and method-level access for lightweight lock words.
 *
 * @author Simon Wilkinson
 */
public abstract class LightweightLockWord64 extends HashableLockWord64 {

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

    protected LightweightLockWord64() {
    }

    /**
     * Boxing-safe cast of a <code>Word</code> to a <code>LightweightLockWord64</code>.
     *
     * @param word the word to cast
     * @return the cast word
     */
    @UNCHECKED_CAST
    public static LightweightLockWord64 from(Word word) {
        return new BoxedLightweightLockWord64(word);
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
    public final LightweightLockWord64 incrementCount() {
        // So long as the rcount field is within a byte boundary, we can just use addition
        // without any endian issues.
        return LightweightLockWord64.from(asAddress().plus(RCOUNT_INC_WORD));
    }

    /**
     * Returns a copy of this lock word with the value of its recursion count field decremented.
     *
     * @return a copy lock word with decremented recursion count
     */
    @INLINE
    public final LightweightLockWord64 decrementCount() {
        return LightweightLockWord64.from(asAddress().minus(RCOUNT_INC_WORD));
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
