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
/*VCSID=25553186-4871-497a-836c-691a593576c9*/
package com.sun.max.collect;

import java.util.*;
import java.util.Arrays;

import com.sun.max.lang.*;
import com.sun.max.util.*;

/**
 * @author Bernd Mathiske
 */
public interface IndexedSequence<Element_Type> extends Sequence<Element_Type> {

    /**
     * Gets the element from this sequence located at a given index.
     * 
     * @throws IndexOutOfBoundsException if {@code 0 < index || index >= length()}
     */
    Element_Type get(int index);

    public final class Static {

        private Static() {
        }

        private static final IndexedSequence<Object> EMPTY = new ArraySequence<Object>(0);

        /**
         * Returns a canonical object representing the empty sequence of a given type.
         */
        public static <Element_Type> IndexedSequence<Element_Type> empty(Class<Element_Type> elementType) {
            final Class<IndexedSequence<Element_Type>> sequenceType = null;
            return StaticLoophole.cast(sequenceType, EMPTY);
        }

        /**
         * Extracts the elements from a given sequence that are {@linkplain Class#isInstance(Object) instances of} a given class
         * and returns them in a new sequence.
         */
        public static <Element_Type, Sub_Type extends Element_Type> IndexedSequence<Sub_Type> filter(Iterable<Element_Type> sequence, Class<Sub_Type> subType) {
            final AppendableIndexedSequence<Sub_Type> result = new ArrayListSequence<Sub_Type>();
            for (Element_Type element : sequence) {
                if (subType.isInstance(element)) {
                    result.append(subType.cast(element));
                }
            }
            return result;
        }

        /**
         * Filters an iterable with a given predicate and return a sequence with the elments that matched the predicate.
         * If the returned sequence will only be iterated over, consider using a {@link FilterIterator} instead.
         */
        public static <Element_Type> IndexedSequence<Element_Type> filter(Iterable<Element_Type> sequence, Predicate<? super Element_Type> predicate) {
            return AppendableIndexedSequence.Static.filter(sequence, predicate);
        }

        public static <Element_Type> IndexedSequence<Element_Type> filterNonNull(Sequence<Element_Type> sequence) {
            return filter(sequence, new Predicate<Element_Type>() {
                public boolean evaluate(Element_Type element) {
                    return element != null;
                }
            });
        }

        public static <Element_Type> IndexedSequence<Element_Type> reverse(Sequence<Element_Type> sequence) {
            int i = sequence.length();
            final MutableSequence<Element_Type> result = new ArraySequence<Element_Type>(i);
            for (Element_Type element : sequence) {
                i--;
                result.set(i, element);
            }
            return result;
        }

        public static <Element_Type> IndexedSequence<Element_Type> sort(Sequence<Element_Type> sequence, Class<Element_Type> elementType) {
            final Element_Type[] array = Sequence.Static.toArray(sequence, elementType);
            Arrays.sort(array);
            return new ArraySequence<Element_Type>(array);
        }

        public static <Element_Type> List<Element_Type> toList(Sequence<Element_Type> sequence) {
            if (sequence instanceof List) {
                final Class<List<Element_Type>> type = null;
                return StaticLoophole.cast(type, sequence);
            }
            final List<Element_Type> arrayList = new ArrayList<Element_Type>(sequence.length());
            for (Element_Type element : sequence) {
                arrayList.add(element);
            }
            return arrayList;
        }

        public static <Element_Type> Sequence<Element_Type> prepended(Element_Type element, Sequence<Element_Type> sequence) {
            final MutableSequence<Element_Type> result = new ArraySequence<Element_Type>(1 + sequence.length());
            MutableSequence.Static.copy(sequence, result, 1);
            result.set(0, element);
            return result;
        }

        public static <Element_Type> Sequence<Element_Type> appended(Sequence<Element_Type> sequence, Element_Type element) {
            final MutableSequence<Element_Type> result = new ArraySequence<Element_Type>(sequence.length() + 1);
            MutableSequence.Static.copy(sequence, result);
            result.set(sequence.length(), element);
            return result;
        }

    }

}
