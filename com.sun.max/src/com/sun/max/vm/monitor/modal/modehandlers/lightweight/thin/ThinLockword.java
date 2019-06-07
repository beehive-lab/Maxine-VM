/*
 * Copyright (c) 2017, 2019, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
package com.sun.max.vm.monitor.modal.modehandlers.lightweight.thin;

import com.sun.max.annotate.HOSTED_ONLY;
import com.sun.max.annotate.INLINE;
import com.sun.max.annotate.INTRINSIC;
import com.sun.max.platform.Platform;
import com.sun.max.unsafe.Address;
import com.sun.max.unsafe.Word;
import com.sun.max.vm.Log;
import com.sun.max.vm.monitor.modal.modehandlers.HashableLockword;
import com.sun.max.vm.monitor.modal.modehandlers.ModalLockword;
import com.sun.max.vm.monitor.modal.modehandlers.lightweight.LightweightLockword;

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.UNSAFE_CAST;


/**
 * Abstracts access to a thin lock word's bit fields.
 */
public class ThinLockword extends LightweightLockword {

    /*
     * For 64 bit:
     * bit [63........................................ 1  0]     Shape         Lock-state
     *
     *     [     0    ][ util  ][     0      ][ hash ][m][0]     Lightweight   Unlocked
     *     [ r. count ][ util  ][  thread ID ][ hash ][m][0]     Lightweight   Locked (rcount >= 1)
     *     [                 Undefined               ][m][1]     Inflated
     *
     * For 32 bit:
     * bit [32........................................ 1  0]     Shape         Lock-state
     *
     *     [     0     ][  util  ][         0        ][m][0]     Lightweight   Unlocked
     *     [ r. count 5][ util 1 ][    thread ID 4   ][m][0]     Lightweight   Locked (rcount >= 1)
     *     [                Undefined                ][m][1]     Inflated
     *     [                  hash                         ]
     *
     * Note:
     * A valid thread ID must be >= 1. This is enforced by VmThreadMap.
     * The per-shape mode bit, m, is not used and is always masked.
     * The 'util' field is not used and is always masked.
     */


    private static final Address UTIL_MASK = UTIL_SHIFTED_MASK.shiftedLeft(UTIL_SHIFT);
    private static final Address UNLOCKED_MASK = Platform.target().arch.is32bit() ? Word.zero().asAddress().bitSet(MISC_BIT_INDEX).or(UTIL_MASK)
                    : HASHCODE_SHIFTED_MASK.shiftedLeft(HASHCODE_SHIFT).bitSet(MISC_BIT_INDEX).or(UTIL_MASK);

    @HOSTED_ONLY
    public ThinLockword(long value) {
        super(value);
    }

    /**
     * Prints the monitor state encoded in a {@code ThinLockword} to the {@linkplain Log log} stream.
     */
    public static void log(ThinLockword lockword) {
        Log.print("ThinLockword: ");
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
     * Boxing-safe cast of a {@code Word} to a {@code ThinLockword}.
     *
     * @param word the word to cast
     * @return the cast word
     */
    @INTRINSIC(UNSAFE_CAST)
    public static ThinLockword from(Word word) {
        return new ThinLockword(word.value);
    }

    /**
     * Tests if the given lock word is a {@code ThinLockword}.
     *
     * @param lockword the lock word to test
     * @return true if {@code lockword} is a {@code ThinLockword}; false otherwise
     */
    @INLINE
    public static final boolean isThinLockword(ModalLockword lockword) {
        return ThinLockword.from(lockword).isLightweight();
    }

    /**
     * Returns a copy of this lock word in an unlocked state.
     *
     * @return the copy lock word
     */
    @INLINE
    public final ThinLockword asUnlocked() {
        return ThinLockword.from(asAddress().and(UNLOCKED_MASK));
    }

    /**
     * Returns a copy of this lock word in a locked state, where the owner is
     * installed as {@code threadID}, and the recursion count is 1.
     *
     * @param threadID the lock owner
     */
    @INLINE
    public final ThinLockword asLockedOnceBy(int threadID) {
        return ThinLockword.from(asUnlocked().asAddress().or(Address.fromInt(threadID).shiftedLeft(THREADID_SHIFT)).or(RCOUNT_INC_WORD));
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
     * (Image build support) Returns a new, unlocked {@code ThinLockword} with the given
     * hashcode installed into the hashcode field.
     *
     * @param hashcode the hashcode to install
     * @return the lock word
     */
    @INLINE
    public static final ThinLockword unlockedFromHashcode(int hashcode) {
        if (Platform.target().arch.is64bit()) {
            return ThinLockword.from(HashableLockword.from(Address.zero()).setHashcode(hashcode));
        } else {
            return ThinLockword.from(HashableLockword.from(Address.zero()));
        }
    }

    @INLINE
    public static final ThinLockword fromHashcode(int hashcode) {
        assert Platform.target().arch.is32bit() : "This function must be called only on 32 bit machines!";
        return ThinLockword.from(HashableLockword.from(Address.zero()).setHashcode(hashcode));
    }

}
