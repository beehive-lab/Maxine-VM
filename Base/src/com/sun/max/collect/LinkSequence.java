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

/**
 * Unsynchronized sequence made by single-linked cells.
 *
 * @author Bernd Mathiske
 */
public class LinkSequence<Element_Type> implements PrependableSequence<Element_Type>, AppendableSequence<Element_Type> {

    private class Cell {
        private Element_Type _head;
        private Cell _tail;
    }

    private Cell _first;
    private Cell _last;
    private int _length;

    public LinkSequence() {
    }

    public LinkSequence(Element_Type element) {
        append(element);
    }

    public LinkSequence(Iterable<Element_Type> elements) {
        for (Element_Type element : elements) {
            append(element);
        }
    }

    public void clear() {
        _first = null;
        _last = null;
        _length = 0;
    }

    public int length() {
        return _length;
    }

    public boolean isEmpty() {
        return _length <= 0;
    }

    public Element_Type first() {
        final Cell result = _first;
        if (result == null) {
            throw new IndexOutOfBoundsException();
        }
        return result._head;
    }

    public Element_Type last() {
        final Cell result = _last;
        if (result == null) {
            throw new IndexOutOfBoundsException();
        }
        return result._head;
    }

    /**
     * @return an Iterable for this sequence that omits the first element (if any)
     */
    public Iterable<Element_Type> tail() {
        final LinkSequence<Element_Type> result = new LinkSequence<Element_Type>();
        if (_length > 1) {
            result._first = _first._tail;
            result._last = _last;
            result._length = _length - 1;
        }
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Sequence)) {
            return false;
        }
        final Sequence sequence = (Sequence) other;
        return Sequence.Static.equals(this, sequence);
    }

    @Override
    public int hashCode() {
        int result = 0;
        if (_first != null && _first._head != null) {
            result ^= _first._head.hashCode();
            if (_last != _first && _last._head != null) {
                result ^= _last._head.hashCode();
            }
        }
        return result;
    }

    public void prepend(Element_Type element) {
        final Cell cell = new Cell();
        cell._head = element;
        if (_first == null) {
            assert _last == null;
            _last = cell;
        } else {
            assert _last != null;
            cell._tail = _first;
        }
        _first = cell;
        _length++;
    }

    public void append(Element_Type element) {
        final Cell cell = new Cell();
        cell._head = element;
        if (_last == null) {
            assert _first == null;
            _first = cell;
        } else {
            assert _first != null;
            _last._tail = cell;
        }
        _last = cell;
        _length++;
    }

    @Override
    public Sequence<Element_Type> clone() {
        return new LinkSequence<Element_Type>(this);
    }

    public static <From_Type, To_Type> LinkSequence<To_Type> map(Sequence<From_Type> from, Class<To_Type> toType, MapFunction<From_Type, To_Type> mapFunction) {
        final LinkSequence<To_Type> to = new LinkSequence<To_Type>();
        for (From_Type element : from) {
            to.append(mapFunction.map(element));
        }
        return to;
    }

    public Collection<Element_Type> toCollection() {
        return Iterables.toCollection(this);
    }

    @Override
    public String toString() {
        return "<" + Sequence.Static.toString(this, null, ", ") + ">";
    }

    public Iterator<Element_Type> iterator() {
        return new Iterator<Element_Type>() {
            private Cell _cell = _first;

            public void remove() {
                throw new UnsupportedOperationException();
            }

            public boolean hasNext() {
                return _cell != null;
            }


            public Element_Type next() {
                if (_cell == null) {
                    throw new NoSuchElementException();
                }
                final Element_Type element = _cell._head;
                _cell = _cell._tail;
                return element;
            }
        };
    }

}
