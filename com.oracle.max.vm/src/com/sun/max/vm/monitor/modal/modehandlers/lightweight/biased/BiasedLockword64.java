/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.monitor.modal.modehandlers.lightweight.biased;

import static com.sun.cri.bytecode.Bytecodes.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.monitor.modal.modehandlers.*;
import com.sun.max.vm.monitor.modal.modehandlers.lightweight.*;

/**
 * Abstracts access to a biased lock word's bit fields.
 */
public abstract class BiasedLockword64 extends LightweightLockword64 {

    /*
     * bit [63............................................. 1  0]     Shape         Mode            Lock-state
     *
     *     [     0    ][ UNUSED_EPOCH][     0     ][ hash ][m][0]     Lightweight   Biasable        No bias owner. Unlocked
     *     [     0    ][    epoch    ][ thread ID ][ hash ][m][0]     Lightweight   Biasable        Bias owned. Unlocked
     *     [ r. count ][    epoch    ][ thread ID ][ hash ][m][0]     Lightweight   Biasable        Bias owned. Locked (rcount >= 1)
     *
     *     [ r. count ][REVOKED_EPOCH][ thread ID ][ hash ][m][0]     Lightweight   Delegate lightweight lock mode
     *     [                     Undefined                ][m][1]     Inflated
     *
     *
     * Note: a valid thread ID must be >= 1
     * The per-shape mode bit, m, is not used and is always masked.
     *
     * For REVOKED_EPOCH see BiasedLockEpoch.REVOKED.
     */

    private static final Address HASHCODE_MASK = HASHCODE_SHIFTED_MASK.shiftedLeft(HASHCODE_SHIFT);
    static final Address EPOCH_MASK = UTIL_SHIFTED_MASK.shiftedLeft(UTIL_SHIFT);
    private static final Address NON_EPOCH_MASK = EPOCH_MASK.not();
    private static final Address BIASED_OWNED_MASK = HASHCODE_MASK.or(EPOCH_MASK.or(THREADID_SHIFTED_MASK.shiftedLeft(THREADID_SHIFT).bitSet(SHAPE_BIT_INDEX)));

    static final int EPOCH_FIELD_WIDTH = UTIL_FIELD_WIDTH;
    static final int EPOCH_SHIFT = UTIL_SHIFT;

    protected BiasedLockword64() {
    }

    /**
     * Boxing-safe cast of a {@code Word} to a {@code BiasedLockword64}.
     *
     * @param word the word to cast
     * @return the cast word
     */
    @INTRINSIC(UNSAFE_CAST)
    public static BiasedLockword64 from(Word word) {
        return new BoxedBiasedLockword64(word);
    }

    /**
     * Returns a copy of this lock word in a biasable, unlocked state with no bias owner.
     *
     * @return the copy lock word
     */
    @INLINE
    public final BiasedLockword64 asAnonBiased() {
        return BiasedLockword64.from(asAddress().and(HASHCODE_MASK));
    }

    /**
     * Returns a copy of this lock word in a locked state, where the lock / bias owner is
     * installed as {@code lockwordThreadID}, and the recursion count is 1.
     *
     * @param lockwordThreadID the lock and bias owner
     * @return the copy lock word
     */
    @INLINE
    public final BiasedLockword64 asBiasedAndLockedOnceBy(int lockwordThreadID) {
        return BiasedLockword64.from(asBiasedTo(lockwordThreadID).asAddress().or(RCOUNT_INC_WORD));
    }

    /**
     * Returns a copy of this lock word in a locked state, where the lock / bias owner is
     * installed as {@code lockwordThreadID}, the bias epoch is set to {@code epoch}, and the recursion count is 1.
     *
     * @param lockwordThreadID the lock and bias owner
     * @param epoch the bias epoch
     * @return the copy lock word
     */
    @INLINE
    public final BiasedLockword64 asBiasedAndLockedOnceBy(int lockwordThreadID, BiasedLockEpoch64 epoch) {
        return BiasedLockword64.from(asBiasedTo(lockwordThreadID, epoch).asAddress().or(RCOUNT_INC_WORD));
    }

    /**
     * Returns a copy of this lock word in a biased but unlocked state, where the bias owner is
     * installed as {@code lockwordThreadID}.
     *
     * @param lockwordThreadID the bias owner
     * @return the copy lock word
     */
    @INLINE
    public final BiasedLockword64 asBiasedTo(int lockwordThreadID) {
        return BiasedLockword64.from(asAnonBiased().asAddress().or(Address.fromUnsignedInt(lockwordThreadID).shiftedLeft(THREADID_SHIFT)));
    }

    /**
     * Returns a copy of this lock word in a biased but unlocked state, where the bias owner is
     * installed as {@code lockwordThreadID}, and the bias epoch is set to {@code epoch}.
     *
     * @param lockwordThreadID the bias owner
     * @param epoch the bias epoch
     * @return the copy lock word
     */
    @INLINE
    public final BiasedLockword64 asBiasedTo(int lockwordThreadID, BiasedLockEpoch64 epoch) {
        return BiasedLockword64.from(asAnonBiased().asAddress().or(epoch.asAddress()).or(Address.fromUnsignedInt(lockwordThreadID).shiftedLeft(THREADID_SHIFT)));
    }

    /**
     * Tests if the given lock word is a {@code BiasedLockword64}.
     *
     * @param lockword the lock word to test
     * @return true if {@code lockword} is a {@code BiasedLockword64}; false otherwise
     */
    @INLINE
    public static final boolean isBiasedLockword(ModalLockword64 lockword) {
        return !lockword.asAddress().and(EPOCH_MASK).equals(BiasedLockEpoch64.REVOKED) && lockword.isLightweight();
    }

    /**
     * Tests if the given lock word is a {@code BiasedLockword64}, and if so, if the value of the lock word's
     * bias owner field equals the given thread ID.
     *
     * @param lockword the lock word to test
     * @param lockwordThreadID the thread ID to test against the lock word's bias owner
     * @return true if {@code lockword} is a {@code BiasedLockword64} and
     *         {@code lockwordThreadID} is the bias owner; false otherwise
     */
    @INLINE
    public static final boolean isBiasedLockAndBiasedTo(ModalLockword64 lockword, int lockwordThreadID) { // Quicker to use individual tests
        return BiasedLockword64.from(lockword).asBiasedTo(lockwordThreadID).equals(lockword.asAddress().and(BIASED_OWNED_MASK));
    }

    /**
     * Tests if the given lock word is a {@code BiasedLockword64}, and if so, if the value of the lock word's
     * bias owner field equals the given thread ID and the lock word's bias epoch equals the given epoch.
     *
     * @param lockword the lock word to test
     * @param epoch the epoch to test against the lock word's bias epoch
     * @param lockwordThreadID the thread ID to test against the lock word's bias owner
     * @return true if {@code lockword} is a {@code BiasedLockword64} and
     *         {@code lockwordThreadID} is the bias owner; false otherwise
     */
    @INLINE
    public static final boolean isBiasedLockAndBiasedTo(ModalLockword64 lockword, BiasedLockEpoch64 epoch, int lockwordThreadID) {
        return BiasedLockword64.from(lockword).asBiasedTo(lockwordThreadID, epoch).equals(lockword.asAddress().and(BIASED_OWNED_MASK));
    }

    /**
     * Returns an unbiasable copy of this lock word.
     *
     * @return the copy lock word
     */
    @INLINE
    public final BiasedLockword64 asUnbiasable() {
        return asWithEpoch(BiasedLockEpoch64.REVOKED);
    }

    /**
     * Gets this lock word's bias epoch.
     *
     * @return the bias epoch
     */
    @INLINE
    public final BiasedLockEpoch64 getEpoch() {
        return BiasedLockEpoch64.from(asAddress().and(EPOCH_MASK));
    }

    /**
     * Returns a copy of this lock word with the bias epoch set to {@code epoch}.
     *
     * @param epoch the bias epoch
     * @return the copy lock word
     */
    @INLINE
    public final BiasedLockword64 asWithEpoch(BiasedLockEpoch64 epoch) {
        return BiasedLockword64.from(asAddress().and(NON_EPOCH_MASK).or(epoch.asAddress()));
    }

    /**
     * Gets the value of this lock word's bias owner field.
     *
     * @return the hashcode
     */
    @INLINE
    public final int getBiasOwnerID() {
        return getThreadID();
    }

    /**
     * (Image build support) Returns a new, unlocked, unbiased {@code BiasedLockword64} with the given
     * hashcode installed into the hashcode field.
     *
     * @param hashcode the hashcode to install
     * @return the lock word
     */
    @INLINE
    public static final BiasedLockword64 anonBiasedFromHashcode(int hashcode) {
        return BiasedLockword64.from(HashableLockword64.from(Address.zero()).setHashcode(hashcode));
    }
}
