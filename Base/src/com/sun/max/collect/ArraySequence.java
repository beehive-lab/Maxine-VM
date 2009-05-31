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
import com.sun.max.lang.Arrays;

/**
 * A sequence that wraps a generic array.
 * Thus instantiation does not require any type cast quirks.
 *
 * @author Bernd Mathiske
 */
public class ArraySequence<Element_Type> implements MutableSequence<Element_Type> {

    private final Element_Type[] _array;

    public ArraySequence(Element_Type... array) {
        _array = array;
    }

    public static <T> Sequence<T> of(T... elements) {
        return new ArraySequence<T>(elements.clone());
    }

    public ArraySequence(int length) {
        final Class<Element_Type[]> arrayType = null;
        _array = StaticLoophole.cast(arrayType, new Object[length]);
    }

    public ArraySequence(Sequence<Element_Type> elements) {
        final Class<Element_Type[]> arrayType = null;
        _array = StaticLoophole.cast(arrayType, new Object[elements.length()]);
        int i = 0;
        for (Element_Type element : elements) {
            _array[i++] = element;
        }
    }

    public ArraySequence(Collection<Element_Type> collection) {
        final Class<Element_Type[]> arrayType = null;
        _array = StaticLoophole.cast(arrayType, collection.toArray());
    }

    public boolean isEmpty() {
        return _array.length == 0;
    }

    public int length() {
        return _array.length;
    }

    public Element_Type first() {
        return _array[0];
    }

    public Element_Type last() {
        return _array[_array.length - 1];
    }

    public Element_Type get(int index) {
        return _array[index];
    }

    public Element_Type set(int index, Element_Type value) {
        final Element_Type previousValue = _array[index];
        _array[index] = value;
        return previousValue;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Sequence)) {
            return false;
        }
        if (other instanceof ArraySequence) {
            return Arrays.equals(_array, ((ArraySequence) other)._array);
        }

        final Sequence sequence = (Sequence) other;
        return Sequence.Static.equals(this, sequence);
    }

    @Override
    public int hashCode() {
        int result = 0;
        final int delta = (_array.length >> 2) + 1;
        for (int i = 0; i < _array.length; i += delta) {
            final Element_Type element = _array[i];
            result += element == null ? i : element.hashCode();
        }
        return result;
    }

    public Iterator<Element_Type> iterator() {
        return Arrays.iterator(_array);
    }

    @Override
    public Sequence<Element_Type> clone() {
        return new ArraySequence<Element_Type>(_array.clone());
    }

    public static <From_Type, To_Type> Sequence<To_Type> map(Sequence<From_Type> from, Class<To_Type> toType, MapFunction<From_Type, To_Type> mapFunction) {
        final MutableSequence<To_Type> to = new ArraySequence<To_Type>(from.length());
        int i = 0;
        for (From_Type fromElement : from) {
            to.set(i, mapFunction.map(fromElement));
            i++;
        }
        return to;
    }

    @Override
    public Collection<Element_Type> toCollection() {
        return java.util.Arrays.asList(_array);
    }

    @Override
    public String toString() {
        return Arrays.toString(_array, ", ");
    }

    public static final class Static {

        private Static() {
        }

        public static <Element_Type> MutableSequence<Element_Type> concatenated(Sequence<Element_Type> sequence1, Sequence<Element_Type> sequence2) {
            final int length = sequence1.length() + sequence2.length();
            final MutableSequence<Element_Type> result = new ArraySequence<Element_Type>(length);
            final int i = MutableSequence.Static.copy(sequence1, result, 0);
            MutableSequence.Static.copy(sequence2, result, i);
            return result;
        }

        public static <Element_Type> MutableSequence<Element_Type> concatenated(Sequence<Element_Type> sequence1, Sequence<Element_Type> sequence2, Sequence<Element_Type> sequence3) {
            final int length = sequence1.length() + sequence2.length() + sequence3.length();
            final MutableSequence<Element_Type> result = new ArraySequence<Element_Type>(length);
            int i = MutableSequence.Static.copy(sequence1, result, 0);
            i = MutableSequence.Static.copy(sequence2, result, i);
            MutableSequence.Static.copy(sequence3, result, i);
            return result;
        }
    }
}
