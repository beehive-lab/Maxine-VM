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
package com.sun.max.vm.monitor.modal.modehandlers.lightweight.thin;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.unsafe.box.*;
import com.sun.max.vm.monitor.modal.modehandlers.*;
import com.sun.max.vm.monitor.modal.modehandlers.lightweight.*;

/**
 * Abstracts access to a thin lock word's bit fields.
 *
 * @author Simon Wilkinson
 */
public abstract class ThinLockWord64 extends LightweightLockWord64 {

    /*
     * bit [63........................................ 1  0]     Shape         Lock-state
     *
     *     [     0    ][ util  ][     0      ][ hash ][m][0]     Lightweight   Unlocked
     *     [ r. count ][ util  ][  thread ID ][ hash ][m][0]     Lightweight   Locked (rcount >= 1)
     *     [                 Undefined               ][m][1]     Inflated
     *
     * Note:
     * A valid thread ID must be >= 1.
     * The per-shape mode bit, m, is not used and is always masked.
     * The 'util' field is not used and is always masked.
     */

    private static final Address UTIL_MASK = UTIL_SHIFTED_MASK.shiftedLeft(UTIL_SHIFT);
    private static final Address UNLOCKED_MASK = HASHCODE_SHIFTED_MASK.shiftedLeft(HASHCODE_SHIFT).bitSet(MISC_BIT_INDEX).or(UTIL_MASK);


    protected ThinLockWord64() {
    }

    /**
     * Boxing-safe cast of a <code>Word</code> to a <code>ThinLockWord64</code>.
     *
     * @param word the word to cast
     * @return the cast word
     */
    @UNCHECKED_CAST
    public static ThinLockWord64 from(Word word) {
        return new BoxedThinLockWord64(word);
    }

    /**
     * Tests if the given lock word is a <code>ThinLockWord64</code>.
     *
     * @param lockWord the lock word to test
     * @return true if <code>lockWord</code> is a <code>ThinLockWord64</code>; false otherwise
     */
    @INLINE
    public static final boolean isThinLockWord(ModalLockWord64 lockWord) {
        return ThinLockWord64.from(lockWord).isLightweight();
    }

    /**
     * Returns a copy of this lock word in an unlocked state.
     *
     * @return the copy lock word
     */
    @INLINE
    public final ThinLockWord64 asUnlocked() {
        return ThinLockWord64.from(asAddress().and(UNLOCKED_MASK));
    }

    /**
     * Returns a copy of this lock word in a locked state, where the owner is
     * installed as <code>threadID</code>, and the recursion count is 1.
     *
     * @param threadID the lock owner
     * @return the copy lock word
     */
    @INLINE
    public final ThinLockWord64 asLockedOnceBy(int threadID) {
        return ThinLockWord64.from(asUnlocked().asAddress().or(Address.fromInt(threadID).shiftedLeft(THREADID_SHIFT)).or(RCOUNT_INC_WORD));
    }

    /**
     * Gets the value of this lock word's lock owner field.
     *
     * @return the hashcode
     */
    @INLINE
    public final int getLockOwnerID() {
        return getThreadID();
    }


    /**
     * (Image build support) Returns a new, unlocked <code>ThinLockWord64</code> with the given
     * hashcode installed into the hashcode field.
     *
     * @param hashcode the hashcode to install
     * @return the lock word
     */
    @INLINE
    public static final ThinLockWord64 unlockedFromHashcode(int hashcode) {
        return ThinLockWord64.from(HashableLockWord64.from(Address.zero()).setHashcode(hashcode));
    }
}
