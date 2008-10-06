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
/*VCSID=594bd447-06e2-41bc-9ee2-6c81bc8d681f*/
package com.sun.max.vm.monitor.modal.modehandlers.lightweight.biased;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.unsafe.box.*;
import com.sun.max.vm.*;
import com.sun.max.vm.monitor.modal.modehandlers.*;
import com.sun.max.vm.monitor.modal.modehandlers.lightweight.*;

/**
 * Abstracts bit field access to a 64-bit biased-lock word.
 * @see LightweightLockWord64
 *
 * @author Simon Wilkinson
 */
public abstract class BiasedLockWord64 extends LightweightLockWord64 {

    /*
     * bit [63............................................. 1  0]     Shape         Mode              State
     *
     *     [     0    ][ UNUSED_EPOCH][     0     ][ hash ][m][0]     Lightweight   Biased          No bias owner. Unlocked
     *     [     0    ][    epoch    ][ thread ID ][ hash ][m][0]     Lightweight   Biased          Bias owned. Unlocked
     *     [ r. count ][    epoch    ][ thread ID ][ hash ][m][0]     Lightweight   Biased          Bias owned. Locked (rcount >= 1)
     *
     *     [ r. count ][REVOKED_EPOCH][ thread ID ][ hash ][m][0]     Lightweight   Next lightweight lock mode
     *     [        Def. by inflated monitor scheme       ][m][1]     Inflated
     *
     *
     * Note: a valid thread ID must be >= 1
     * The per-shape mode bit, m, is not used and is always masked.
     */

    private static final Address HASHCODE_MASK = HASHCODE_SHIFTED_MASK.shiftedLeft(HASHCODE_SHIFT);
    static final Address EPOCH_MASK = UTIL_SHIFTED_MASK.shiftedLeft(UTIL_SHIFT);
    private static final Address NON_EPOCH_MASK = EPOCH_MASK.not();
    private static final Address BIASED_OWNED_MASK = HASHCODE_MASK.or(EPOCH_MASK.or(THREADID_SHIFTED_MASK.shiftedLeft(THREADID_SHIFT).bitSet(SHAPE_BIT)));

    static final int EPOCH_FIELD_WIDTH = UTIL_FIELD_WIDTH;
    static final int EPOCH_SHIFT = UTIL_SHIFT;

    protected BiasedLockWord64() {
    }

    @INLINE
    public static BiasedLockWord64 as(Word word) {
        if (MaxineVM.isPrototyping()) {
            return new BoxedBiasedLockWord64(word);
        }
        return UnsafeLoophole.castWord(BiasedLockWord64.class, word);
    }

    @INLINE
    public final BiasedLockWord64 asAnonBiased() {
        return BiasedLockWord64.as(asAddress().and(HASHCODE_MASK));
    }

    @INLINE
    public final BiasedLockWord64 asBiasedAndLockedOnceBy(int lockwordThreadID) {
        return BiasedLockWord64.as(asBiasedTo(lockwordThreadID).asAddress().or(RCOUNT_INC_WORD));
    }

    @INLINE
    public final BiasedLockWord64 asBiasedAndLockedOnceBy(int lockwordThreadID, BiasedLockEpoch epoch) {
        return BiasedLockWord64.as(asBiasedTo(lockwordThreadID, epoch).asAddress().or(RCOUNT_INC_WORD));
    }

    @INLINE
    public final BiasedLockWord64 asBiasedTo(int lockwordThreadID) {
        return BiasedLockWord64.as(asAnonBiased().asAddress().or(Address.fromInt(lockwordThreadID).shiftedLeft(THREADID_SHIFT)));
    }

    @INLINE
    public final BiasedLockWord64 asBiasedTo(int lockwordThreadID, BiasedLockEpoch epoch) {
        return BiasedLockWord64.as(asAnonBiased().asAddress().or(epoch.asAddress()).or(Address.fromInt(lockwordThreadID).shiftedLeft(THREADID_SHIFT)));
    }

    @INLINE
    public static final boolean isBiasedLockWord(ModalLockWord64 lockword) {
        return !lockword.asAddress().and(EPOCH_MASK).equals(BiasedLockEpoch.REVOKED) && lockword.isLightweight();
    }

    @INLINE
    public static final boolean isBiasedLockAndBiasedTo(ModalLockWord64 lockword, int lockwordThreadID) { // Quicker to use individual tests
        return BiasedLockWord64.as(lockword).asBiasedTo(lockwordThreadID).equals(lockword.asAddress().and(BIASED_OWNED_MASK));
    }

    @INLINE
    public static final boolean isBiasedLockAndBiasedTo(ModalLockWord64 lockword, BiasedLockEpoch epoch, int lockwordThreadID) {
        return BiasedLockWord64.as(lockword).asBiasedTo(lockwordThreadID, epoch).equals(lockword.asAddress().and(BIASED_OWNED_MASK));
    }

    @INLINE
    public final BiasedLockWord64 asUnbiasable() {
        return asWithEpoch(BiasedLockEpoch.REVOKED);
    }

    @INLINE
    public final BiasedLockEpoch getEpoch() {
        return BiasedLockEpoch.as(asAddress().and(EPOCH_MASK));
    }

    @INLINE
    public final BiasedLockWord64 asWithEpoch(BiasedLockEpoch epoch) {
        return BiasedLockWord64.as(asAddress().and(NON_EPOCH_MASK).or(epoch.asAddress()));
    }

    @INLINE
    public final int getBiasOwnerID() {
        return getThreadID();
    }

    @INLINE
    public static final BiasedLockWord64 anonBiasedFromHashcode(int hashcode) {
        return BiasedLockWord64.as(HashableLockWord64.as(Address.zero()).setHashcode(hashcode));
    }
}
