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

    private static class Cell<Element_Type> {
        private Element_Type head;
        private Cell<Element_Type> tail;
    }

    private Cell<Element_Type> first;
    private Cell<Element_Type> last;
    private int length;

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
        first = null;
        last = null;
        length = 0;
    }

    public int length() {
        return length;
    }

    public boolean isEmpty() {
        return length <= 0;
    }

    public Element_Type first() {
        final Cell<Element_Type> result = first;
        if (result == null) {
            throw new IndexOutOfBoundsException();
        }
        return result.head;
    }

    public Element_Type last() {
        final Cell<Element_Type> result = last;
        if (result == null) {
            throw new IndexOutOfBoundsException();
        }
        return result.head;
    }

    /**
     * @return an Iterable for this sequence that omits the first element (if any)
     */
    public Iterable<Element_Type> tail() {
        final LinkSequence<Element_Type> result = new LinkSequence<Element_Type>();
        if (length > 1) {
            result.first = first.tail;
            result.last = last;
            result.length = length - 1;
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
        if (first != null && first.head != null) {
            result ^= first.head.hashCode();
            if (last != first && last.head != null) {
                result ^= last.head.hashCode();
            }
        }
        return result;
    }

    public void prepend(Element_Type element) {
        final Cell<Element_Type> cell = new Cell<Element_Type>();
        cell.head = element;
        if (first == null) {
            assert last == null;
            last = cell;
        } else {
            assert last != null;
            cell.tail = first;
        }
        first = cell;
        length++;
    }

    public void append(Element_Type element) {
        final Cell<Element_Type> cell = new Cell<Element_Type>();
        cell.head = element;
        if (last == null) {
            assert first == null;
            first = cell;
        } else {
            assert first != null;
            last.tail = cell;
        }
        last = cell;
        length++;
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
            private Cell<Element_Type> cell = first;

            public void remove() {
                throw new UnsupportedOperationException();
            }

            public boolean hasNext() {
                return cell != null;
            }


            public Element_Type next() {
                if (cell == null) {
                    throw new NoSuchElementException();
                }
                final Element_Type element = cell.head;
                cell = cell.tail;
                return element;
            }
        };
    }

}
