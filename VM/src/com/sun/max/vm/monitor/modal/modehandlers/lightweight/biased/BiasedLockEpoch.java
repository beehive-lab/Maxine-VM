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
/*VCSID=3330eeac-7870-4b00-b657-f3bd17ff9db9*/
package com.sun.max.vm.monitor.modal.modehandlers.lightweight.biased;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.unsafe.box.*;
import com.sun.max.vm.*;

/**

 * @author Simon Wilkinson
 */
public abstract class BiasedLockEpoch extends Word {

    private static final BiasedLockEpoch UNUSED = BiasedLockEpoch.as(Word.zero());
    private static final BiasedLockEpoch BULK_REVOCATION = BiasedLockEpoch.as(Address.fromInt(1).shiftedLeft(BiasedLockWord64.EPOCH_SHIFT));
    static final BiasedLockEpoch REVOKED = BiasedLockEpoch.as(Address.fromInt(2).shiftedLeft(BiasedLockWord64.EPOCH_SHIFT));
    private static final BiasedLockEpoch MIN = BiasedLockEpoch.as(Address.fromInt(3).shiftedLeft(BiasedLockWord64.EPOCH_SHIFT));
    private static final BiasedLockEpoch MAX = BiasedLockEpoch.as(BiasedLockWord64.EPOCH_MASK);

    protected BiasedLockEpoch() {
    }

    @INLINE
    public static BiasedLockEpoch as(Word word) {
        if (MaxineVM.isPrototyping()) {
            return new BoxedBiasedLockEpoch64(word);
        }
        return UnsafeLoophole.castWord(BiasedLockEpoch.class, word);
    }

    @INLINE
    final BiasedLockEpoch increment() {
        if (this.equals(MAX)) {
            return MIN;
        }
        int epoch = toIntInternal();
        return BiasedLockEpoch.as(Address.fromInt(epoch++).shiftedLeft(BiasedLockWord64.EPOCH_SHIFT));
    }

    @INLINE
    final boolean isBulkRevocation() {
        return equals(BULK_REVOCATION);
    }

    @INLINE
    static final BiasedLockEpoch bulkRevocation() {
        return BULK_REVOCATION;
    }

    @INLINE
    private int toIntInternal() {
        return asAddress().unsignedShiftedRight(BiasedLockWord64.EPOCH_SHIFT).toInt();
    }

    public int toInt() {
        return toIntInternal();
    }

    public static BiasedLockEpoch init() {
        return MIN;
    }

}
