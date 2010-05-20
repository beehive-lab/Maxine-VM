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

import static com.sun.cri.bytecode.Bytecodes.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.monitor.modal.modehandlers.*;
import com.sun.max.vm.monitor.modal.modehandlers.lightweight.*;

/**
 * Abstracts access to a thin lock word's bit fields.
 *
 * @author Simon Wilkinson
 */
public abstract class ThinLockword64 extends LightweightLockword64 {

    /*
     * bit [63........................................ 1  0]     Shape         Lock-state
     *
     *     [     0    ][ util  ][     0      ][ hash ][m][0]     Lightweight   Unlocked
     *     [ r. count ][ util  ][  thread ID ][ hash ][m][0]     Lightweight   Locked (rcount >= 1)
     *     [                 Undefined               ][m][1]     Inflated
     *
     * Note:
     * A valid thread ID must be >= 1. This is enforced by VmThreadMap.
     * The per-shape mode bit, m, is not used and is always masked.
     * The 'util' field is not used and is always masked.
     */

    private static final Address UTIL_MASK = UTIL_SHIFTED_MASK.shiftedLeft(UTIL_SHIFT);
    private static final Address UNLOCKED_MASK = HASHCODE_SHIFTED_MASK.shiftedLeft(HASHCODE_SHIFT).bitSet(MISC_BIT_INDEX).or(UTIL_MASK);

    protected ThinLockword64() {
    }

    /**
     * Prints the monitor state encoded in a {@code ThinLockword64} to the {@linkplain Log log} stream.
     */
    public static void log(ThinLockword64 lockword) {
        Log.print("ThinLockword64: ");
        if (lockword.isInflated()) {
            Log.print("inflated=true");
        } else {
            Log.print("inflated=false");
            Log.print(" locked=");
            Log.print(!lockword.equals(lockword.asUnlocked()));
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
     * Boxing-safe cast of a {@code Word} to a {@code ThinLockword64}.
     *
     * @param word the word to cast
     * @return the cast word
     */
    @INTRINSIC(UNSAFE_CAST)
    public static ThinLockword64 from(Word word) {
        return new BoxedThinLockword64(word);
    }

    /**
     * Tests if the given lock word is a {@code ThinLockword64}.
     *
     * @param lockword the lock word to test
     * @return true if {@code lockword} is a {@code ThinLockword64}; false otherwise
     */
    @INLINE
    public static final boolean isThinLockword(ModalLockword64 lockword) {
        return ThinLockword64.from(lockword).isLightweight();
    }

    /**
     * Returns a copy of this lock word in an unlocked state.
     *
     * @return the copy lock word
     */
    @INLINE
    public final ThinLockword64 asUnlocked() {
        return ThinLockword64.from(asAddress().and(UNLOCKED_MASK));
    }

    /**
     * Returns a copy of this lock word in a locked state, where the owner is
     * installed as {@code threadID}, and the recursion count is 1.
     *
     * @param threadID the lock owner
     */
    @INLINE
    public final ThinLockword64 asLockedOnceBy(int threadID) {
        return ThinLockword64.from(asUnlocked().asAddress().or(Address.fromInt(threadID).shiftedLeft(THREADID_SHIFT)).or(RCOUNT_INC_WORD));
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
     * (Image build support) Returns a new, unlocked {@code ThinLockword64} with the given
     * hashcode installed into the hashcode field.
     *
     * @param hashcode the hashcode to install
     * @return the lock word
     */
    @INLINE
    public static final ThinLockword64 unlockedFromHashcode(int hashcode) {
        return ThinLockword64.from(HashableLockword64.from(Address.zero()).setHashcode(hashcode));
    }
}
