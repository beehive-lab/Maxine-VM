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
 * @see ModalLockword
 */
public class HashableLockword extends ModalLockword {

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

    protected static final int HASH_FIELD_WIDTH = 32;
    protected static final int HASHCODE_SHIFT = Platform.target().arch.is64bit() ? NUMBER_OF_MODE_BITS : 0;
    protected static final Address HASHCODE_SHIFTED_MASK = Word.allOnes().asAddress().unsignedShiftedRight(Word.width() - HASH_FIELD_WIDTH);

    @HOSTED_ONLY
    public HashableLockword(long value) {
        super(value);
    }

    /**
     * Prints the monitor state encoded in a {@code HashableLockword} to the {@linkplain Log log} stream.
     */
    public static void log(HashableLockword lockword) {
        Log.print("HashableLockword: ");
        if (lockword.isInflated()) {
            Log.print("inflated=true");
        } else {
            Log.print("inflated=false");
            Log.print(" hash=");
            Log.print(lockword.getHashcode());
        }
    }

    /**
     * Boxing-safe cast of a {@code Word} to a {@code HashableLockword}.
     *
     * @param word the word to cast
     * @return the cast word
     */
    @INTRINSIC(UNSAFE_CAST)
    public static HashableLockword from(Word word) {
        return new HashableLockword(word.value);
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
     * Installs the given hashcode into a <i>copy</i> of this {@code HashableLockword}. The copied
     * lock word is returned.
     * <p/>
     * Note: It is assumed that this lock word does not contain an existing hashcode.
     *
     * @param hashcode the hashcode to install
     * @return a copy of this {@code HashableLockword} with the installed hashcode
     */
    @INLINE
    public final HashableLockword setHashcode(int hashcode) {
        return HashableLockword.from(asAddress().or(Address.fromUnsignedInt(hashcode).shiftedLeft(HASHCODE_SHIFT)));
    }
}
