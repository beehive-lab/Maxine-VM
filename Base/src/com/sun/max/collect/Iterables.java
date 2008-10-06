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
/*VCSID=4094e727-0d7b-4709-8858-e5a7a856cc7e*/
package com.sun.max.collect;

import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.lang.Arrays;

/**
 * Utilities that operate on {@link Iterable}s such as {@linkplain #join(Iterable, Iterable) combining} multiple {@code
 * Iterable} objects into one, converting {@link IterableWithLength} objects
 * {@linkplain #toCollection(IterableWithLength) to} and {@linkplain #toIterableWithLength(Collection) from}
 * {@link Collection} objects.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class Iterables {

    private Iterables() {

    }

    public static <Element_Type> Iterable<Element_Type> from(Element_Type... elements) {
        return Arrays.iterable(elements);
    }

    public static <Element_Type> Iterable<Element_Type> from(final Iterator<Element_Type> iterator) {
        return new Iterable<Element_Type>() {
            public Iterator<Element_Type> iterator() {
                return iterator;
            }
        };
    }

    public static <Element_Type> Collection<Element_Type> toCollection(final IterableWithLength<Element_Type> iterableWithLength) {
        return new AbstractCollection<Element_Type>() {
            @Override
            public Iterator<Element_Type> iterator() {
                return iterableWithLength.iterator();
            }

            @Override
            public int size() {
                return iterableWithLength.length();
            }
        };
    }

    public static <Element_Type> IterableWithLength<Element_Type> toIterableWithLength(final Collection<Element_Type> collection) {
        return new IterableWithLength<Element_Type>() {
            public Iterator<Element_Type> iterator() {
                return collection.iterator();
            }
            public int length() {
                return collection.size();
            }
        };
    }

    public static int countIterations(Iterator iterator) {
        int count = 0;
        while (iterator.hasNext()) {
            ++count;
            iterator.next();
        }
        return count;
    }

    private static final class EnumerationIterable<Element_Type> implements Iterable<Element_Type> {

        private final Enumeration<Element_Type> _enumeration;

        private class EnumerationIterator implements Iterator<Element_Type> {

            EnumerationIterator() {
            }

            public boolean hasNext() {
                return _enumeration.hasMoreElements();
            }

            public Element_Type next() {
                return _enumeration.nextElement();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

        }

        EnumerationIterable(Enumeration<Element_Type> enumeration) {
            _enumeration = enumeration;
        }

        public Iterator<Element_Type> iterator() {
            return new EnumerationIterator();
        }

    }

    public static <Element_Type> Iterable<Element_Type> fromEnumeration(Enumeration<Element_Type> enumeration) {
        return new EnumerationIterable<Element_Type>(enumeration);
    }

    private static final IterableWithLength<Object> EMPTY_ITERABLE = new IterableWithLength<Object>() {
        public Iterator<Object> iterator() {
            return Iterators.empty();
        }
        public int length() {
            return 0;
        }
    };

    public static <Element_Type> IterableWithLength<Element_Type> empty() {
        final Class<IterableWithLength<Element_Type>> type = null;
        return StaticLoophole.cast(type, EMPTY_ITERABLE);
    }

    private static final class Flatten1Iterator<Element_Type> implements Iterator<Element_Type> {

        private Iterator<Iterable<Element_Type>> _outerIterator;
        private Iterator<Element_Type> _innerIterator;

        private Flatten1Iterator(Iterable<Iterable<Element_Type>> iterable) {
            _outerIterator = iterable.iterator();
        }

        public boolean hasNext() {
            while (_innerIterator == null || !_innerIterator.hasNext()) {
                if (!_outerIterator.hasNext()) {
                    return false;
                }
                _innerIterator = _outerIterator.next().iterator();
            }
            return true;
        }

        public Element_Type next() {
            if (!hasNext()) {
                return null;
            }
            return _innerIterator.next();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static <Element_Type> Iterable<Element_Type> flatten1(Iterable<Iterable<Element_Type>> iterable) {
        final Iterator<Element_Type> iterator = new Flatten1Iterator<Element_Type>(iterable);
        return new Iterable<Element_Type>() {
            public Iterator<Element_Type> iterator() {
                return iterator;
            }
        };
    }

    /**
     * Creates an {@link Iterable} that combines the elements of 2 given {@code Iterable} objects.
     *
     * @param <Element_Type> the type of the elements in the given iterables
     * @param iterable1 the first iterable
     * @param iterable2 the second iterable
     * @return an object that produces an iterator over of the objects represented by {@code iterable1} and {@code
     *         iterable2}
     */
    public static <Element_Type> Iterable<Element_Type> join(final Iterable<Element_Type> iterable1, final Iterable<Element_Type> iterable2) {
        return new Iterable<Element_Type>() {
            public Iterator<Element_Type> iterator() {
                return Iterators.join(iterable1.iterator(), iterable2.iterator());
            }
        };
    }

    /**
     * Creates an {@link Iterable} that combines the elements of 3 given {@code Iterable} objects.
     *
     * @param <Element_Type> the type of the elements in the given iterables
     * @param iterable1 the first iterable
     * @param iterable2 the second iterable
     * @param iterable3 the third iterable
     * @return an object that produces an iterator over of the objects represented by {@code iterable1}, {@code
     *         iterable2} and {@code iterable3}
     */
    public static <Element_Type> Iterable<Element_Type> join(
                    final Iterable<Element_Type> iterable1,
                    final Iterable<Element_Type> iterable2,
                    final Iterable<Element_Type> iterable3) {
        return new Iterable<Element_Type>() {
            public Iterator<Element_Type> iterator() {
                return Iterators.join(iterable1.iterator(), iterable2.iterator(), iterable3.iterator());
            }
        };
    }

    /**
     * Creates an {@link Iterable} that combines the elements of 4 given {@code Iterable} objects.
     *
     * @param <Element_Type> the type of the elements in the given iterables
     * @param iterable1 the first iterable
     * @param iterable2 the second iterable
     * @param iterable3 the third iterable
     * @param iterable4 the fourth iterable
     * @return an object that produces an iterator over of the objects represented by {@code iterable1}, {@code
     *         iterable2}, {@code iterable3} and {@code iterable4}
     */
    public static <Element_Type> Iterable<Element_Type> join(
                    final Iterable<Element_Type> iterable1,
                    final Iterable<Element_Type> iterable2,
                    final Iterable<Element_Type> iterable3,
                    final Iterable<Element_Type> iterable4) {
        return new Iterable<Element_Type>() {
            public Iterator<Element_Type> iterator() {
                return Iterators.join(iterable1.iterator(), iterable2.iterator(), iterable3.iterator(), iterable4.iterator());
            }
        };
    }

    /**
     * Creates an {@link IterableWithLength} that combines the elements of 2 given {@code IterableWithLength} objects.
     *
     * @param <Element_Type> the type of the elements in the given iterables
     * @param iterable1 the first iterable
     * @param iterable2 the second iterable
     * @return an object that produces an iterator over of the objects represented by {@code iterable1} and {@code
     *         iterable2}
     */
    public static <Element_Type> IterableWithLength<Element_Type> join(
                    final IterableWithLength<Element_Type> iterable1,
                    final IterableWithLength<Element_Type> iterable2) {
        return new IterableWithLength<Element_Type>() {
            public Iterator<Element_Type> iterator() {
                return Iterators.join(iterable1.iterator(), iterable2.iterator());
            }
            public int length() {
                return iterable1.length() + iterable2.length();
            }
        };
    }

    /**
     * Creates an {@link IterableWithLength} that combines the elements of 3 given {@code IterableWithLength} objects.
     *
     * @param <Element_Type> the type of the elements in the given iterables
     * @param iterable1 the first iterable
     * @param iterable2 the second iterable
     * @param iterable3 the third iterable
     * @return an object that produces an iterator over of the objects represented by {@code iterable1}, {@code
     *         iterable2} and {@code iterable3}
     */
    public static <Element_Type> IterableWithLength<Element_Type> join(
                    final IterableWithLength<Element_Type> iterable1,
                    final IterableWithLength<Element_Type> iterable2,
                    final IterableWithLength<Element_Type> iterable3) {
        return new IterableWithLength<Element_Type>() {
            public Iterator<Element_Type> iterator() {
                return Iterators.join(iterable1.iterator(), iterable2.iterator(), iterable3.iterator());
            }
            public int length() {
                return iterable1.length() + iterable2.length() + iterable3.length();
            }
        };
    }

    /**
     * Creates an {@link IterableWithLength} that combines the elements of 4 given {@code IterableWithLength} objects.
     *
     * @param <Element_Type> the type of the elements in the given iterables
     * @param iterable1 the first iterable
     * @param iterable2 the second iterable
     * @param iterable3 the third iterable
     * @param iterable4 the fourth iterable
     * @return an object that produces an iterator over of the objects represented by {@code iterable1}, {@code
     *         iterable2}, {@code iterable3} and {@code iterable4}
     */
    public static <Element_Type> IterableWithLength<Element_Type> join(
                    final IterableWithLength<Element_Type> iterable1,
                    final IterableWithLength<Element_Type> iterable2,
                    final IterableWithLength<Element_Type> iterable3,
                    final IterableWithLength<Element_Type> iterable4) {
        return new IterableWithLength<Element_Type>() {
            public Iterator<Element_Type> iterator() {
                return Iterators.join(iterable1.iterator(), iterable2.iterator(), iterable3.iterator(), iterable4.iterator());
            }
            @Override
            public int length() {
                return iterable1.length() + iterable2.length() + iterable3.length() + iterable4.length();
            }
        };
    }
}
