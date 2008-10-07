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
/*VCSID=9911b34e-8bb0-4342-9330-32c4097c19c1*/
package com.sun.max.vm.monitor.modal.modehandlers.lightweight;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.unsafe.box.*;
import com.sun.max.vm.*;
import com.sun.max.vm.monitor.modal.modehandlers.*;

/**
 * Base class for lightweight lock words.
 *
 * The bit field manipulation is constructed to be endian-agnostic.
 * This could be optimised quite a bit for specific architectures.
 *
 * @author Simon Wilkinson
 */
public abstract class LightweightLockWord64 extends HashableLockWord64 {

    /*
     * bit [63........................................ 1  0]     Shape
     *
     *     [ r. count ][ util  ][  thread ID ][ hash ][m][0]     Lightweight
     *     [    Def. by inflated monitor scheme      ][m][1]     Inflated
     *
     */

    protected static final int RCOUNT_FIELD_WIDTH = 5; // Must be <= 8 (see incrementCount())
    protected static final int UTIL_FIELD_WIDTH = 9;
    protected static final int THREADID_FIELD_WIDTH = 64 - (RCOUNT_FIELD_WIDTH + UTIL_FIELD_WIDTH + HASH_FIELD_WIDTH + MODE_BIT_QTY);

    protected static final int THREADID_SHIFT = HASHCODE_SHIFT + HASH_FIELD_WIDTH;
    protected static final int UTIL_SHIFT = THREADID_SHIFT + THREADID_FIELD_WIDTH;
    protected static final int RCOUNT_SHIFT = UTIL_SHIFT + UTIL_FIELD_WIDTH;

    protected static final Address THREADID_SHIFTED_MASK = Word.allOnes().asAddress().unsignedShiftedRight(64 - THREADID_FIELD_WIDTH);
    protected static final Address UTIL_SHIFTED_MASK = Word.allOnes().asAddress().unsignedShiftedRight(64 - UTIL_FIELD_WIDTH);
    protected static final Address RCOUNT_SHIFTED_MASK = Word.allOnes().asAddress().unsignedShiftedRight(64 - RCOUNT_FIELD_WIDTH);

    protected static final Address RCOUNT_INC_WORD = Address.zero().bitSet(64 - RCOUNT_FIELD_WIDTH);

    protected LightweightLockWord64() {
    }

    @INLINE
    public static LightweightLockWord64 as(Word word) {
        if (MaxineVM.isPrototyping()) {
            return new BoxedLightweightLockWord64(word);
        }
        return UnsafeLoophole.castWord(LightweightLockWord64.class, word);
    }

    @INLINE
    protected final int getThreadID() {
        return asAddress().unsignedShiftedRight(THREADID_SHIFT).and(THREADID_SHIFTED_MASK).toInt();
    }

    @INLINE
    protected final int getUtil() {
        return asAddress().unsignedShiftedRight(UTIL_SHIFT).and(UTIL_SHIFTED_MASK).toInt();
    }

    @INLINE
    public final boolean countOverflow() {
        return asAddress().unsignedShiftedRight(RCOUNT_SHIFT).equals(RCOUNT_SHIFTED_MASK);
    }

    @INLINE
    public final boolean countUnderflow() {
        return asAddress().unsignedShiftedRight(RCOUNT_SHIFT).isZero();
    }

    @INLINE
    public final LightweightLockWord64 incrementCount() {
        // So long as the rcount field is within a byte boundary, we can just use addition
        // without any endian issues.
        return LightweightLockWord64.as(asAddress().plus(RCOUNT_INC_WORD));
    }

    @INLINE
    public final LightweightLockWord64 decrementCount() {
        return LightweightLockWord64.as(asAddress().minus(RCOUNT_INC_WORD));
    }

    @INLINE
    public final int getRecursionCount() {
        return asAddress().unsignedShiftedRight(RCOUNT_SHIFT).toInt();
    }
}
