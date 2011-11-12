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

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;

/**

 */
public abstract class BiasedLockEpoch64 extends Word {

    private static final BiasedLockEpoch64 UNUSED = BiasedLockEpoch64.from(Word.zero());
    private static final BiasedLockEpoch64 BULK_REVOCATION = BiasedLockEpoch64.from(Address.fromInt(1).shiftedLeft(BiasedLockword64.EPOCH_SHIFT));
    static final BiasedLockEpoch64 REVOKED = BiasedLockEpoch64.from(Address.fromInt(2).shiftedLeft(BiasedLockword64.EPOCH_SHIFT));
    private static final BiasedLockEpoch64 MIN = BiasedLockEpoch64.from(Address.fromInt(3).shiftedLeft(BiasedLockword64.EPOCH_SHIFT));
    private static final BiasedLockEpoch64 MAX = BiasedLockEpoch64.from(BiasedLockword64.EPOCH_MASK);

    @HOSTED_ONLY
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

    @INLINE
    public int toInt() {
        return toIntInternal();
    }

    @INLINE
    public static BiasedLockEpoch64 init() {
        return MIN;
    }

}
