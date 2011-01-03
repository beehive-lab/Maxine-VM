/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.unsafe;

import com.sun.max.annotate.*;
import com.sun.max.vm.object.*;

/**
 * Why we never directly use any arrays of any subtype of 'Word':
 *
 * The byte codes aaload and aastore have no clue whether the array reference on the stack refers to a Word array or an
 * Object array. As long as we are unable to or unsure whether we can discern the difference STATICALLY, we must not
 * allow aaload and aastore to deal with either case dynamically.
 *
 * Compare this to the situation between byte arrays and boolean arrays addressed by baload and bastore! In those cases,
 * we CAN differentiate between the respective array types DYNAMICALLY and cause no harm to the GC, because both types
 * are primitive types.
 *
 * In case of Object and Word, one calls for a stack map entry and the other does not. Therefore, a dynamic distinction
 * would require support for dynamic stack map changes. So far we are not willing to afford the price for that
 * (runtime, implementation effort, complexity).
 *
 * Instead, always use WordArray.set() and WordArray.get() instead of [] when addressing word array elements.
 *
 * @author Bernd Mathiske
 */
public final class WordArray {

    private WordArray() {
    }

    /**
     * Replaces all {@code null} entries in a given word array with the appropriately typed boxed zero value.
     */
    @HOSTED_ONLY
    private static void replaceNullWithZero(Word[] array) {
        final Word zero = Word.zero();
        for (int i = 0; i != array.length; ++i) {
            if (array[i] == null) {
                array[i] = zero;
            }
        }
    }

    public static void fill(Word[] array, Word value) {
        for (int i = 0; i < array.length; i++) {
            uncheckedSet(array, i, value);
        }
    }

    // Substituted by uncheckedGet_()
    public static Word uncheckedGet(Word[] array, int index) {
        if (array[index] == null) {
            replaceNullWithZero(array);
        }
        return array[index];
    }

    @LOCAL_SUBSTITUTION
    @INLINE
    private static Word uncheckedGet_(Word[] array, int index) {
        return ArrayAccess.getWord(array, index);
    }

    // Substituted by get_()
    public static Word get(Word[] array, int index) {
        if (array[index] == null) {
            replaceNullWithZero(array);
        }
        return array[index];
    }

    @LOCAL_SUBSTITUTION
    @INLINE
    private static Word get_(Word[] array, int index) {
        ArrayAccess.checkIndex(array, index);
        return ArrayAccess.getWord(array, index);
    }

    // Substituted by uncheckedSet_()
    public static void uncheckedSet(Word[] array, int index, Word value) {
        array[index] = value;
    }

    @LOCAL_SUBSTITUTION
    @INLINE
    private static void uncheckedSet_(Word[] array, int index, Word value) {
        ArrayAccess.setWord(array, index, value);
    }

    // Substituted by set_()
    public static void set(Word[] array, int index, Word value) {
        array[index] = value;
    }

    @LOCAL_SUBSTITUTION
    @INLINE
    private static void set_(Word[] array, int index, Word value) {
        ArrayAccess.checkIndex(array, index);
        ArrayAccess.setWord(array, index, value);
    }

    public static void copyAll(Word[] fromArray, Word[] toArray) {
        if (fromArray.length > toArray.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        for (int i = 0; i < fromArray.length; i++) {
            uncheckedSet(toArray, i, uncheckedGet(fromArray, i));
        }
    }
}
