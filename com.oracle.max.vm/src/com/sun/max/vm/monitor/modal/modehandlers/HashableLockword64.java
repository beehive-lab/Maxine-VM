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
package com.sun.max.vm.monitor.modal.modehandlers;

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;

/**
 * Abstracts access to a lock word's hashcode bit field.
 *
 * @see ModalLockword64
 */
public abstract class HashableLockword64 extends ModalLockword64 {

    /*
     * Field layout:
     *
     * bit [63............................... 1  0]
     *
     *     [     Undefined      ][ hashcode ][m][s]
     *
     */

    protected static final int HASH_FIELD_WIDTH = 32;
    protected static final int HASHCODE_SHIFT = NUMBER_OF_MODE_BITS;
    protected static final Address HASHCODE_SHIFTED_MASK = Word.allOnes().asAddress().unsignedShiftedRight(64 - HASH_FIELD_WIDTH);

    protected HashableLockword64() {
    }

    /**
     * Prints the monitor state encoded in a {@code HashableLockword64} to the {@linkplain Log log} stream.
     */
    public static void log(HashableLockword64 lockword) {
        Log.print("HashableLockword64: ");
        if (lockword.isInflated()) {
            Log.print("inflated=true");
        } else {
            Log.print("inflated=false");
            Log.print(" hash=");
            Log.print(lockword.getHashcode());
        }
    }

    /**
     * Boxing-safe cast of a {@code Word} to a {@code HashableLockword64}.
     *
     * @param word the word to cast
     * @return the cast word
     */
    @INTRINSIC(UNSAFE_CAST)
    public static HashableLockword64 from(Word word) {
        return new BoxedHashableLockword64(word);
    }

    /**
     * Gets the value of this lock word's hashcode field.
     *
     * @return the hashcode
     */
    @INLINE
    public final int getHashcode() {
        return asAddress().unsignedShiftedRight(HASHCODE_SHIFT).and(HASHCODE_SHIFTED_MASK).toInt();
    }

    /**
     * Installs the given hashcode into a <i>copy</i> of this {@code HashableLockword64}. The copied
     * lock word is returned.
     *
     * Note: It is assumed that this lock word does not contain an existing hashcode.
     *
     * @param hashcode the hashcode to install
     * @return a copy of this {@code HashableLockword64} with the installed hashcode
     */
    @INLINE
    public final HashableLockword64 setHashcode(int hashcode) {
        return HashableLockword64.from(asAddress().or(Address.fromUnsignedInt(hashcode).shiftedLeft(HASHCODE_SHIFT)));
    }
}
