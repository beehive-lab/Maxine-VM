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
package com.sun.max.vm.monitor.modal.modehandlers.lightweight.biased;

import static com.sun.cri.bytecode.Bytecodes.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;

/**

 * @author Simon Wilkinson
 */
public abstract class BiasedLockEpoch64 extends Word {

    private static final BiasedLockEpoch64 UNUSED = BiasedLockEpoch64.from(Word.zero());
    private static final BiasedLockEpoch64 BULK_REVOCATION = BiasedLockEpoch64.from(Address.fromInt(1).shiftedLeft(BiasedLockword64.EPOCH_SHIFT));
    static final BiasedLockEpoch64 REVOKED = BiasedLockEpoch64.from(Address.fromInt(2).shiftedLeft(BiasedLockword64.EPOCH_SHIFT));
    private static final BiasedLockEpoch64 MIN = BiasedLockEpoch64.from(Address.fromInt(3).shiftedLeft(BiasedLockword64.EPOCH_SHIFT));
    private static final BiasedLockEpoch64 MAX = BiasedLockEpoch64.from(BiasedLockword64.EPOCH_MASK);

    protected BiasedLockEpoch64() {
    }

    @INTRINSIC(UNSAFE_CAST)
    public static BiasedLockEpoch64 from(Word word) {
        return new BoxedBiasedLockEpoch64(word);
    }

    @INLINE
    final BiasedLockEpoch64 increment() {
        if (this.equals(MAX)) {
            return MIN;
        }
        int epoch = toIntInternal();
        return BiasedLockEpoch64.from(Address.fromUnsignedInt(epoch++).shiftedLeft(BiasedLockword64.EPOCH_SHIFT));
    }

    @INLINE
    final boolean isBulkRevocation() {
        return equals(BULK_REVOCATION);
    }

    @INLINE
    static final BiasedLockEpoch64 bulkRevocation() {
        return BULK_REVOCATION;
    }

    @INLINE
    private int toIntInternal() {
        return asAddress().unsignedShiftedRight(BiasedLockword64.EPOCH_SHIFT).toInt();
    }

    public int toInt() {
        return toIntInternal();
    }

    public static BiasedLockEpoch64 init() {
        return MIN;
    }

}
