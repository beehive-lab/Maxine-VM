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
package com.sun.max.unsafe;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
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
    @PROTOTYPE_ONLY
    private static <Word_Type extends Word> void replaceNullWithZero(Word_Type[] array) {
        final Class<Class<Word_Type>> type = null;
        final Class<Word_Type> wordType = StaticLoophole.cast(type, array.getClass().getComponentType());
        final Word_Type zero = Word.zero().as(wordType);
        for (int i = 0; i != array.length; ++i) {
            if (array[i] == null) {
                array[i] = zero;
            }
        }
    }

    public static <Word_Type extends Word> void fill(Word_Type[] array, Word_Type value) {
        for (int i = 0; i < array.length; i++) {
            uncheckedSet(array, i, value);
        }
    }

    // Substituted by uncheckedGet_()
    public static <Word_Type extends Word> Word_Type uncheckedGet(Word_Type[] array, int index) {
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
    public static <Word_Type extends Word> Word_Type get(Word_Type[] array, int index) {
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
    public static <Word_Type extends Word> void uncheckedSet(Word_Type[] array, int index, Word_Type value) {
        array[index] = value;
    }

    @LOCAL_SUBSTITUTION
    @INLINE
    private static <Word_Type extends Word> void uncheckedSet_(Word_Type[] array, int index, Word_Type value) {
        ArrayAccess.setWord(array, index, value);
    }

    // Substituted by set_()
    public static <Word_Type extends Word> void set(Word_Type[] array, int index, Word_Type value) {
        array[index] = value;
    }

    @LOCAL_SUBSTITUTION
    @INLINE
    private static <Word_Type extends Word> void set_(Word_Type[] array, int index, Word_Type value) {
        ArrayAccess.checkIndex(array, index);
        ArrayAccess.setWord(array, index, value);
    }


    public static <Word_Type extends Word> void copyAll(Word_Type[] fromArray, Word_Type[] toArray) {
        if (fromArray.length > toArray.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        for (int i = 0; i < fromArray.length; i++) {
            uncheckedSet(toArray, i, uncheckedGet(fromArray, i));
        }
    }
}
