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
package com.sun.max.collect;

import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.util.*;

/**
 * A Sequence presents an immutable view of a linear collection.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public interface Sequence<Element_Type> extends IterableWithLength<Element_Type>, Cloneable {

    /**
     * Gets the number of elements in this sequence.
     */
    int length();

    /**
     * Determines if this sequence is empty.
     */
    boolean isEmpty();

    /**
     * Gets the first element from this sequence.
     *
     * @throws IndexOutOfBoundsException if this sequence is {@linkplain #isEmpty() empty}
     */
    Element_Type first();

    /**
     * Gets the last element from this sequence.
     *
     * @throws IndexOutOfBoundsException if this sequence is {@linkplain #isEmpty() empty}
     */
    Element_Type last();

    /**
     * Gets an iterator over the elements in this sequence.
     */
    Iterator<Element_Type> iterator();

    /**
     * Clones this sequence.
     */
    Sequence<Element_Type> clone();

    Collection<Element_Type> toCollection();

    public final class Static {

        private Static() {
        }

        private static final Sequence<Object> EMPTY = new ArraySequence<Object>(0);

        /**
         * Returns a canonical object representing the empty sequence of a given type.
         */
        public static <Element_Type> Sequence<Element_Type> empty(Class<Element_Type> elementType) {
            final Class<Sequence<Element_Type>> sequenceType = null;
            return StaticLoophole.cast(sequenceType, EMPTY);
        }

        /**
         * Returns true if {@code sequence} contains an element identical to {@code value}. More formally, returns true
         * if and only if {@code sequence} contains at least one element {@code e} such that
         * {@code (value == e)}.
         */
        public static boolean containsIdentical(Sequence sequence, Object value) {
            for (Object element : sequence) {
                if (element == value) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Returns true if {@code sequence} contains an element equal to {@code value}. More formally, returns true
         * if and only if {@code sequence} contains at least one element {@code e} such that
         * {@code (value == null ? e == null : value.equals(e))}.
         */
        public static boolean containsEqual(Sequence sequence, Object value) {
            for (Object element : sequence) {
                if (element.equals(value)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Returns the index in {@code sequence} of the first occurrence equal to {@code value}, or -1 if
         * {@code sequence} does not contain {@code value}. More formally, returns the lowest index
         * {@code i} such that {@code (value == null ? sequence.get(i) == null : value.equals(sequence.get(i)))},
         * or -1 if there is no such index.
         */
        public static int indexOfEqual(Sequence sequence, Object value) {
            int i = 0;
            for (Object element : sequence) {
                if (element.equals(value)) {
                    return i;
                }
                ++i;
            }
            return -1;
        }

        /**
         * Returns the index in {@code sequence} of the first occurrence identical to {@code value}, or -1 if
         * {@code sequence} does not contain {@code value}. More formally, returns the lowest index
         * {@code i} such that {@code (sequence.get(i) == value)}, or -1 if there is no such index.
         */
        public static int indexOfIdentical(Sequence sequence, Object value) {
            int i = 0;
            for (Object element : sequence) {
                if (element == value) {
                    return i;
                }
                ++i;
            }
            return -1;
        }

        public static boolean equals(Sequence sequence1, Sequence sequence2) {
            if (sequence1.length() != sequence2.length()) {
                return false;
            }
            final Iterator iterator2 = sequence2.iterator();
            for (Object element1 : sequence1) {
                final Object element2 = iterator2.next();
                if (element1 == null) {
                    if (element2 != null) {
                        return false;
                    }
                } else if (!element1.equals(element2)) {
                    return false;
                }
            }
            return true;
        }

        public static <Element_Type> Element_Type[] toArray(IterableWithLength<? extends Element_Type> sequence, Class<Element_Type> elementType) {
            final Element_Type[] array = com.sun.max.lang.Arrays.newInstance(elementType, sequence.length());
            int i = 0;
            for (Element_Type element : sequence) {
                array[i] = element;
                i++;
            }
            return array;
        }

        public static <Element_Type> int copyIntoArray(Sequence<? extends Element_Type> sequence, int start, Element_Type[] array) {
            int i = start;
            for (Element_Type element : sequence) {
                array[i] = element;
                i++;
            }
            return i;
        }

        /**
         * Extracts the elements from a given sequence that are {@linkplain Class#isInstance(Object) instances of} a given class
         * and returns them in a new sequence.
         */
        public static <Element_Type, Sub_Type extends Element_Type> Sequence<Sub_Type> filter(Iterable<Element_Type> sequence, Class<Sub_Type> subType) {
            final AppendableSequence<Sub_Type> result = new LinkSequence<Sub_Type>();
            for (Element_Type element : sequence) {
                if (subType.isInstance(element)) {
                    result.append(subType.cast(element));
                }
            }
            return result;
        }

        /**
         * Returns a string representation of the contents of the specified iterable.
         * Adjacent elements are separated by the specified separator. Elements are
         * converted to strings by {@link String#valueOf(Object)}.
         *
         * @param iterable   the iterable whose string representation to return
         * @param separator  the separator to use
         * @param toStringFunction function that converts {@code Element_Type} to {@code String}. If
         *                   this parameter is {@code null}, then the {@link Object#toString} method
         *                   will be used
         * @return a string representation of {@code sequence}
         * @throws NullPointerException if {@code sequence} or {@code separator} is null
         */
        public static <Element_Type> String toString(Iterable<? extends Element_Type> iterable, MapFunction<Element_Type, String> toStringFunction, String separator) {
            if (iterable == null || separator == null) {
                throw new NullPointerException();
            }

            final Iterator<? extends Element_Type> iterator = iterable.iterator();
            if (!iterator.hasNext()) {
                return "";
            }
            boolean hasNext = iterator.hasNext();
            final StringBuilder buf = new StringBuilder();
            while (hasNext) {
                final Element_Type element = iterator.next();
                final String string = toStringFunction == null ? String.valueOf(element) : toStringFunction.map(element);
                buf.append(element == iterable ? "(this Iterable)" : string);
                hasNext = iterator.hasNext();
                if (hasNext) {
                    buf.append(separator);
                }
            }

            return buf.toString();
        }

        /**
         * Filters an iterable with a given predicate and return a sequence with the elements that matched the predicate.
         * If the returned sequence will only be iterated over, consider using a {@link FilterIterator} instead.
         */
        public static <Element_Type> Sequence<Element_Type> filter(Iterable<Element_Type> sequence, Predicate<? super Element_Type> predicate) {
            final AppendableSequence<Element_Type> result = new LinkSequence<Element_Type>();
            for (Element_Type element : sequence) {
                if (predicate.evaluate(element)) {
                    result.append(element);
                }
            }
            return result;
        }

        public static <Element_Type> Sequence<Element_Type> filterNonNull(Sequence<Element_Type> sequence) {
            return filter(sequence, new Predicate<Element_Type>() {
                public boolean evaluate(Element_Type element) {
                    return element != null;
                }
            });
        }

        public static <Element_Type> Sequence<Element_Type> reverse(Sequence<Element_Type> sequence) {
            return IndexedSequence.Static.reverse(sequence);
        }

        public static <Element_Type> Sequence<Element_Type> sort(Sequence<Element_Type> sequence, Class<Element_Type> elementType) {
            return IndexedSequence.Static.sort(sequence, elementType);
        }

        public static <Element_Type> List<Element_Type> toList(Sequence<Element_Type> sequence) {
            return IndexedSequence.Static.toList(sequence);
        }

        public static <Element_Type> Sequence<Element_Type> prepended(Element_Type element, Sequence<Element_Type> sequence) {
            final PrependableSequence<Element_Type> result = new LinkSequence<Element_Type>(sequence);
            result.prepend(element);
            return result;
        }

        public static <Element_Type> Sequence<Element_Type> appended(Sequence<Element_Type> sequence, Element_Type element) {
            final AppendableSequence<Element_Type> result = new LinkSequence<Element_Type>(sequence);
            result.append(element);
            return result;
        }

        public static <Element_Type> Sequence<Element_Type> concatenated(Sequence<Element_Type> sequence1, Sequence<Element_Type> sequence2) {
            return ArraySequence.Static.concatenated(sequence1, sequence2);
        }

        public static <Element_Type> Sequence<Element_Type> concatenated(Sequence<Element_Type> sequence1, Sequence<Element_Type> sequence2, Sequence<Element_Type> sequence3) {
            return ArraySequence.Static.concatenated(sequence1, sequence2, sequence3);
        }
    }
}
