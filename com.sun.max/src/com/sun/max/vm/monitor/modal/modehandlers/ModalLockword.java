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

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;

/**
 * Abstracts bit field access to a 64-bit modal lock word.
 *
 * This base class defines only the mode fields; the minimum
 * necessary to allow eventual decoding of the lock word.
 */
public class ModalLockword extends Word {

    /*
     * Field layout:
     *
     * bit [63............................... 1  0]     Shape
     *
     *     [           Undefined            ][m][0]     Lightweight
     *     [           Undefined            ][m][1]     Inflated
     *
     * 2 mode bits are used, allowing 2 lock shapes and 2 modes per shape.
     * Sub classes should define the use of and access to the per-shape mode bit (m).
     *
     */

    protected static final int NUMBER_OF_MODE_BITS = 2;
    protected static final int SHAPE_BIT_INDEX = 0;
    protected static final int MISC_BIT_INDEX = 1;

    @HOSTED_ONLY
    public ModalLockword(long value) {
        super(value);
    }

    /**
     * Prints the monitor state encoded in a {@code ModalLockword} to the {@linkplain Log log} stream.
     */
    public static void log(ModalLockword lockword) {
        Log.print("ModalLockword: ");
        if (lockword.isInflated()) {
            Log.print("inflated=true");
        } else {
            Log.print("inflated=false");
        }
    }

    /**
     * Boxing-safe cast of a {@code Word} to a {@code ModalLockword}.
     *
     * @param word the word to cast
     * @return the cast word
     */
    @INTRINSIC(UNSAFE_CAST)
    public static ModalLockword from(Word word) {
        return new ModalLockword(word.value);
    }

    /**
     * Tests if this lock word is in an inflated mode.
     *
     * @return true if inflated; false otherwise
     */
    @INLINE
    public final boolean isInflated() {
        return asAddress().isBitSet(SHAPE_BIT_INDEX);
    }

    /**
     * Tests if this lock word is in a lightweight mode.
     *
     * @return true if lightweight; false otherwise
     */
    @INLINE
    public final boolean isLightweight() {
        return !isInflated();
    }
}
