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
package com.sun.max.vm.monitor.modal.modehandlers.lightweight.biased;

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;

/**

 */
public final class BiasedLockEpoch extends Word {

    private static final BiasedLockEpoch UNUSED = BiasedLockEpoch.from(Word.zero());
    private static final BiasedLockEpoch BULK_REVOCATION = BiasedLockEpoch.from(Address.fromInt(1).shiftedLeft(BiasedLockword.EPOCH_SHIFT));
    static final BiasedLockEpoch REVOKED = BiasedLockEpoch.from(Address.fromInt(2).shiftedLeft(BiasedLockword.EPOCH_SHIFT));
    private static final BiasedLockEpoch MIN = BiasedLockEpoch.from(Address.fromInt(3).shiftedLeft(BiasedLockword.EPOCH_SHIFT));
    private static final BiasedLockEpoch MAX = BiasedLockEpoch.from(BiasedLockword.EPOCH_MASK);


    @HOSTED_ONLY
    public BiasedLockEpoch(long value) {
        super(value);
    }

    @INTRINSIC(UNSAFE_CAST)
    public static BiasedLockEpoch from(Word word) {
        return new BiasedLockEpoch(word.value);
    }

    @INLINE
    BiasedLockEpoch increment() {
        if (this.equals(MAX)) {
            return MIN;
        }
        int epoch = toIntInternal();
        return BiasedLockEpoch.from(Address.fromUnsignedInt(epoch++).shiftedLeft(BiasedLockword.EPOCH_SHIFT));
    }

    @INLINE
    boolean isBulkRevocation() {
        return equals(BULK_REVOCATION);
    }

    @INLINE
    static BiasedLockEpoch bulkRevocation() {
        return BULK_REVOCATION;
    }

    @INLINE
    private int toIntInternal() {
        return asAddress().unsignedShiftedRight(BiasedLockword.EPOCH_SHIFT).toInt();
    }

    @INLINE
    public int toInt() {
        return toIntInternal();
    }

    @INLINE
    public static BiasedLockEpoch init() {
        return MIN;
    }
}
