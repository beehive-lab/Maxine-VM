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
package com.sun.max.lang;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.util.*;
import com.sun.max.program.ProgramError;

/**
 * Additional methods that one could expect in java.util.Arrays.
 *
 * @author Bernd Mathiske
 */
public final class Arrays {

    private Arrays() {
    }

    public static <Element_Type> Element_Type[] newInstance(Class<Element_Type> elementType, int length) {
        final Object array = Array.newInstance(elementType, length);
        final Class<Element_Type[]> arrayType = null;
        return StaticLoophole.cast(arrayType, array);
    }

    public static <Element_Type> Element_Type[] fromElements(Element_Type... elements) {
        return elements;
    }

    public static <Element_Type> Element_Type[] from(Class<Element_Type> elementType, Iterable<Element_Type> elements) {
        final int length;
        if (elements instanceof IterableWithLength) {
            length = ((IterableWithLength) elements).length();
        } else if (elements instanceof Collection) {
            length = ((Collection) elements).size();
        } else {
            int count = 0;
            for (final Iterator<Element_Type> iterator = elements.iterator(); iterator.hasNext(); iterator.next()) {
                ++count;
            }
            length = count;
        }
        final Element_Type[] result = Arrays.newInstance(elementType, length);
        final Iterator<Element_Type> iterator = elements.iterator();
        for (int i = 0; i != result.length; ++i) {
            result[i] = iterator.next();
        }
        return result;
    }

    public static <Element_Type> Element_Type[] from(Class<Element_Type> elementType, Element_Type[] elements) {
        final Element_Type[] result = Arrays.newInstance(elementType, elements.length);
        for (int i = 0; i != result.length; ++i) {
            result[i] = elements[i];
        }
        return result;
    }

    /**
     * Gets an iterator for a given array. The iterator returns the elements of the array in ascending order of their
     * indices.
     *
     * @param array the array for which an iterator is returned
     * @return an iterator over the elements in {@code array} that iterates over the elements in ascending order of
     *         their indices
     */
    public static <Element_Type> Iterator<Element_Type> iterator(Element_Type[] array) {
        return java.util.Arrays.asList(array).iterator();
    }

    /**
     * Gets an object that can produce an {@linkplain #iterator(Object[]) ascending iterator} for a given array.
     *
     * @param array the array for which an iterable is returned
     */
    public static <Element_Type> IterableWithLength<Element_Type> iterable(Element_Type[] array) {
        return new ArraySequence<Element_Type>(array);
    }

    /**
     * Gets an iterator for a given array. The iterator returns the elements of the array in descending order of their
     * indices.
     *
     * @param array the array for which an iterator is returned
     * @return an iterator over the elements in {@code array} that iterates over the elements in descending order of
     *         their indices
     */
    public static <Element_Type> Iterator<Element_Type> reverseIterator(final Element_Type[] array) {
        return new Iterator<Element_Type>() {
            int index = array.length - 1;
            public boolean hasNext() {
                return index >= 0;
            }
            public Element_Type next() {
                try {
                    return array[index--];
                } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
                    throw new NoSuchElementException();
                }
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Gets an object that can produce a {@linkplain #reverseIterator(Object[]) descending iterator} for a given array.
     *
     * @param array the array for which an iterable is returned
     */
    public static <Element_Type> Iterable<Element_Type> reverseIterable(final Element_Type[] array) {
        return new Iterable<Element_Type>() {
            public Iterator<Element_Type> iterator() {
                return reverseIterator(array);
            }
        };
    }

    public static <Element_Type> Collection<Element_Type> collection(Element_Type[] array) {
        return java.util.Arrays.asList(array);
    }

    public static <Element_Type> boolean equals(Element_Type[] a, Element_Type[] b) {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] == null) {
                if (b[i] != null) {
                    return false;
                }
            } else if (!a[i].equals(b[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a new array containing a subrange of the given array.
     *
     * @param array The array to copy from
     * @param index The index at which to start copying
     * @return A new array of the same type, containing a copy of the subrange starting at the indicated index
     */
    public static <Element_Type> Element_Type[] subArray(Element_Type[] array, int index) {
        if (index < 0 || index > array.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        final int resultLength = array.length - index;
        final Object resultArray = Array.newInstance(array.getClass().getComponentType(), resultLength);
        final Class<Element_Type[]> arrayType = null;
        final Element_Type[] result = StaticLoophole.cast(arrayType, resultArray);
        /* Buggy
        System.arraycopy(array, index, result, 0, resultLength);
        */
        for (int i = 0; i < resultLength; i++) {
            result[i] = array[index + i];
        }
        return result;
    }

    public static <Element_Type> Element_Type[] subArray(Element_Type[] array, int index, int length) {
        if (index < 0 || index > array.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        final Class<Element_Type[]> arrayType = null;
        final Element_Type[] result = StaticLoophole.cast(arrayType, Array.newInstance(array.getClass().getComponentType(), length));
        /* Buggy
       System.arraycopy(array, index, result, 0, length);
       */
        for (int i = 0; i < length; i++) {
            result[i] = array[index + i];
        }
        return result;
    }

    /**
     * Copies the (partial) contents of one array to another array and returns the value of the destination array. This
     * is just a type-safe convenience call for {@link System#arraycopy(Object, int, Object, int, int)} that returns the
     * destination array. Invoking this method is equivalent to:
     *
     * <blockquote><pre>
     *   Arrays.copy(source, 0, destination, 0, source.length)
     * </pre></blockquote>
     *
     * @param source the array to be copied
     * @param destination the array into which the elements of {@code source} should be copied
     * @return the value of the {@code destination} parameter
     *
     * @throws  IndexOutOfBoundsException  if copying would cause access of data outside array bounds.
     * @exception  ArrayStoreException  if an element in {@code source} could not be stored into{@code destination} because of a type mismatch.
     * @exception  NullPointerException if either {@code source} or{@code destination} is {@code null}
     */
    public static <ElementSuper_Type, ElementSub_Type extends ElementSuper_Type> ElementSuper_Type[] copy(ElementSub_Type[] source, ElementSuper_Type[] destination) {
        return copy(source, 0, destination, 0, Math.min(source.length, destination.length));
    }

    /**
     * Copies the (partial) contents of one array to another array and returns the value of the destination array. This
     * is just a type-safe convenience call for {@link System#arraycopy(Object, int, Object, int, int)} that returns the
     * destination array.
     *
     * @param source the array to be copied
     * @param sourceStart starting position in the source array
     * @param destination the array into which the elements of {@code source} should be copied
     * @param destinationStart starting position in the destination data
     * @param length the number of array elements to be copied.
     * @return the value of the {@code destination} parameter
     *
     * @throws IndexOutOfBoundsException if copying would cause access of data outside array bounds.
     * @exception ArrayStoreException if an element in {@code source} could not be stored into{@code destination}
     *                because of a type mismatch.
     * @exception NullPointerException if either {@code source} or{@code destination} is {@code null}
     */
    public static <ElementSuper_Type, ElementSub_Type extends ElementSuper_Type> ElementSuper_Type[] copy(ElementSub_Type[] source, int sourceStart, ElementSuper_Type[] destination, int destinationStart, int length) {
        System.arraycopy(source, sourceStart, destination, destinationStart, length);
        return destination;
    }

    public static <Element_Type> Element_Type[] extend(Element_Type[] array, int length) {
        if (length <= array.length) {
            return array;
        }
        final Element_Type[] result = StaticLoophole.cast(Array.newInstance(array.getClass().getComponentType(), length));
        System.arraycopy(array, 0, result, 0, array.length);
        return result;
    }

    public static <Element_Type> Element_Type[] prepend(Element_Type[] array, Element_Type... additionalElements) {
        if (array == null) {
            return additionalElements;
        }
        final int resultLength = additionalElements.length + array.length;
        final Class<Element_Type[]> arrayType = null;
        final Element_Type[] result = StaticLoophole.cast(arrayType, Array.newInstance(array.getClass().getComponentType(), resultLength));
        System.arraycopy(additionalElements, 0, result, 0, additionalElements.length);
        System.arraycopy(array, 0, result, additionalElements.length, array.length);
        return result;
    }

    public static <Element_Type> Element_Type[] append(Element_Type[] array, Element_Type... additionalElements) {
        if (array == null) {
            return additionalElements;
        }
        final int resultLength = array.length + additionalElements.length;
        final Element_Type[] result = extend(array, resultLength);
        System.arraycopy(additionalElements, 0, result, array.length, additionalElements.length);
        return result;
    }

    public static <Element_Type> Element_Type[] append(Class<Element_Type> resultElementType, Element_Type[] array, Element_Type... additionalElements) {
        if (array == null) {
            return additionalElements;
        }
        final Element_Type[] result = newInstance(resultElementType, array.length + additionalElements.length);
        System.arraycopy(array, 0, result, 0, array.length);
        System.arraycopy(additionalElements, 0, result, array.length, additionalElements.length);
        return result;
    }

    public static <Element_Type> Element_Type[] insert(Class<Element_Type> resultElementType, Element_Type[] array, int index, Element_Type element) {
        if (array == null) {
            final Element_Type[] result = newInstance(resultElementType, 1);
            result[index] = element;
            return result;
        }
        final Element_Type[] result = newInstance(resultElementType, array.length + 1);
        if (index > 0) {
            System.arraycopy(array, 0, result, 0, index);
        }
        result[index] = element;
        if (index < array.length) {
            System.arraycopy(array, index, result, index + 1, array.length - index);
        }
        return result;
    }

    public static  <Element_Type> Element_Type[] remove(Class<Element_Type> resultElementType, Element_Type[] array, int index) {
        final int newLength = array.length - 1;
        final Element_Type[] result = newInstance(resultElementType, newLength);
        System.arraycopy(array, 0, result, 0, index);
        if (index < newLength) {
            System.arraycopy(array, index + 1, result, index, newLength - index);
        }
        return result;
    }

    public static <Element_Type> boolean contains(Element_Type[] array, Element_Type value) {
        for (Element_Type element : array) {
            if (element == value) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true iff there is at least one element that is contained in both arrays.
     */
    public static <Element_Type> boolean containsAny(Element_Type[] array1, Element_Type[] array2) {
        for (Element_Type element1 : array1) {
            for (Element_Type element2 : array2) {
                if (element1 == element2) {
                    return true;
                }
            }
        }
        return false;
    }

    public static <Element_Type> int find(Element_Type[] array, Element_Type value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) {
                return i;
            }
        }
        return -1;
    }

    public static <Element_Type> int countElement(Element_Type[] array, Element_Type element) {
        int n = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i] == element) {
                n++;
            }
        }
        return n;
    }

    public static <Element_Type, Result_Type extends Element_Type> Result_Type[] filter(Element_Type[] array, Class<Result_Type> resultType, Result_Type[] none) {
        if (array.length == 0) {
            return none;
        }
        List<Element_Type> result = null;
        for (int i = 0; i < array.length; i++) {
            if (resultType.isInstance(array[i])) {
                if (result == null) {
                    result = new ArrayList<Element_Type>();
                }
                result.add(array[i]);
            }
        }
        if (result == null) {
            return none;
        }
        return result.toArray(newInstance(resultType, result.size()));
    }

    public static <Element_Type> Element_Type[] filter(Element_Type[] array, Predicate<? super Element_Type> predicate, Element_Type[] none) {
        if (array.length == 0) {
            return none;
        }
        List<Element_Type> result = null;
        for (int i = 0; i < array.length; i++) {
            if (predicate.evaluate(array[i])) {
                if (result == null) {
                    result = new ArrayList<Element_Type>();
                }
                result.add(array[i]);
            }
        }
        if (result == null) {
            return none;
        }
        final Class<Element_Type[]> arrayType = null;
        final Element_Type[] space = StaticLoophole.cast(arrayType, Array.newInstance(array.getClass().getComponentType(), result.size()));
        return result.toArray(space);
    }

    public static <Element_Type> boolean verify(Element_Type[] array, Predicate<? super Element_Type> predicate) {
        for (int i = 0; i < array.length; i++) {
            if (!predicate.evaluate(array[i])) {
                return false;
            }
        }
        return true;
    }

    public static <Element_Type, Sub_Type extends Element_Type> boolean verify(Element_Type[] array, Class<Sub_Type> subType) {
        for (int i = 0; i < array.length; i++) {
            if (!subType.isInstance(array[i])) {
                return false;
            }
        }
        return true;
    }

    public static <From_Type, To_Type> To_Type[] map(From_Type[] from, Class<To_Type> toType, MapFunction<From_Type, To_Type> mapFunction) {
        final To_Type[] to = newInstance(toType, from.length);
        for (int i = 0; i < from.length; i++) {
            to[i] = mapFunction.map(from[i]);
        }
        return to;
    }

    /**
     * Returns a string representation of the contents of the specified array.
     * Adjacent elements are separated by the specified separator. Elements are
     * converted to strings as by <tt>String.valueOf(int)</tt>.
     *
     * @param array     the array whose string representation to return
     * @param separator the separator to use
     * @return a string representation of <tt>array</tt>
     * @throws NullPointerException if {@code array} or {@code separator} is null
     */
    public static <Element_Type> String toString(Element_Type[] array, String separator) {
        if (array == null || separator == null) {
            throw new NullPointerException();
        }
        if (array.length == 0) {
            return "";
        }

        final StringBuilder builder = new StringBuilder();
        builder.append(array[0]);

        for (int i = 1; i < array.length; i++) {
            builder.append(separator);
            builder.append(array[i]);
        }

        return builder.toString();
    }

    public static <Element_Type> String toString(Element_Type[] array) {
        return toString(array, ", ");
    }

    public static <Element_Type> void mergeEqualElements(Element_Type[] array) {
        final Dictionary<Element_Type, Element_Type> lookup = new Hashtable<Element_Type, Element_Type>();
        for (int i = 0; i < array.length; i++) {
            final Element_Type element = array[i];
            if (element != null) {
                final Element_Type mergedElement = lookup.get(element);
                if (mergedElement != null) {
                    array[i] = mergedElement;
                } else {
                    lookup.put(element, element);
                    array[i] = element;
                }
            }
        }
    }

    public static <ElementSuper_Type, ElementSub_Type extends ElementSuper_Type> ElementSuper_Type[] join(Class<ElementSuper_Type> elementSuperType, ElementSub_Type[]... arrays) {
        int totalLength = 0;
        for (ElementSub_Type[] array : arrays) {
            totalLength += array.length;
        }
        final ElementSuper_Type[] result = newInstance(elementSuperType, totalLength);
        int index = 0;
        for (ElementSub_Type[] array : arrays) {
            System.arraycopy(array, 0, result, index, array.length);
            index += array.length;
        }
        return result;
    }

    public static Object[] flatten(Object[] array) {
        final AppendableSequence<Object> sequence = new LinkSequence<Object>();
        for (Object outer : array) {
            if (outer instanceof Object[]) {
                for (Object inner : flatten((Object[]) outer)) {
                    sequence.append(inner);
                }
            } else {
                sequence.append(outer);
            }
        }
        return from(Object.class, sequence);
    }

    public static int[] grow(int[] array, int length) {
        final int[] newArray = new int[length];
        for (int i = 0; i < array.length; i++) {
            newArray[i] = array[i];
        }
        return newArray;
    }

    /**
     * Gets an integer element of a {@code byte[]}, {@code char[]}, {@code short[]}, or {@code int[]}.
     * This is useful for saving space by having densely encoded arrays of integers when it
     * is known that all values in the array fit in a small range.
     * @param array the array object
     * @param index the index into the array
     * @return the integer value at the specified index
     */
    public int getIntElement(Object array, int index) {
        if (array instanceof byte[]) {
            return ((byte[]) array)[index];
        }
        if (array instanceof char[]) {
            return ((char[]) array)[index];
        }
        if (array instanceof short[]) {
            return ((short[]) array)[index];
        }
        if (array instanceof int[]) {
            return ((int[]) array)[index];
        }
        throw ProgramError.unexpected("unexpected dense int array type: " + array.getClass());
    }

    /**
     * Sets an integer element of a {@code byte[]}, {@code char[]}, {@code short[]}, or {@code int[]}.
     * This is useful for saving space by having densely encoded arrays of integers when it
     * is known that all values in the array fit in a small range.
     * @param array the array object
     * @param index the index into the array
     * @param value the value to put into the array
     * @throws ProgramError if the value will not fit into the specified array
     */
    public void setIntElement(Object array, int index, int value) {
        if (array instanceof byte[]) {
            byte v = (byte) value;
            if (v != value) {
                ProgramError.unexpected("integer value will not fit into byte");
            }
            byte[] a = (byte[]) array;
            a[index] = v;
        }
        if (array instanceof char[]) {
            char v = (char) value;
            if (v != value) {
                ProgramError.unexpected("integer value will not fit into char");
            }
            char[] a = (char[]) array;
            a[index] = v;
        }
        if (array instanceof short[]) {
            short v = (short) value;
            if (v != value) {
                ProgramError.unexpected("integer value will not fit into short");
            }
            short[] a = (short[]) array;
            a[index] = v;
        }
        if (array instanceof int[]) {
            int[] a = (int[]) array;
            a[index] = value;
        }
        throw ProgramError.unexpected("unexpected dense int array type: " + array.getClass());
    }
}
