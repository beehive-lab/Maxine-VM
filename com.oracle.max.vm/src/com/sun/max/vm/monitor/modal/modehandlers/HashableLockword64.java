/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
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
package com.sun.max.vm.monitor.modal.modehandlers;

import com.sun.max.annotate.HOSTED_ONLY;
import com.sun.max.annotate.INLINE;
import com.sun.max.annotate.INTRINSIC;
import com.sun.max.platform.Platform;
import com.sun.max.unsafe.Address;
import com.sun.max.unsafe.Word;
import com.sun.max.vm.Log;

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.UNSAFE_CAST;

/**
 * Abstracts access to a lock word's hashcode bit field.
 *
 * @see ModalLockword64
 */
public class HashableLockword64 extends ModalLockword64 {

    /*
     * Field layout (64 Bit):
     *
     * bit [63............................... 1  0]
     *
     *     [     Undefined      ][ hashcode ][m][s]
     *
     * Field layout (32 Bit):
     *
     * bit [31...................................0]
     *     [                hashcode              ]
     */

    protected static final int HASH_FIELD_WIDTH = Platform.target().arch.is64bit() ? 32 : 0;
    protected static final int HASHCODE_SHIFT = Platform.target().arch.is32bit() ? 0 : NUMBER_OF_MODE_BITS;
    protected static final Address HASHCODE_SHIFTED_MASK = Platform.target().arch.is32bit() ? Word.allOnes().asAddress() : Word.allOnes().asAddress().unsignedShiftedRight(64 - HASH_FIELD_WIDTH);

    @HOSTED_ONLY
    public HashableLockword64(long value) {
        super(value);
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
        return new HashableLockword64(word.value);
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
     * <p/>
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
