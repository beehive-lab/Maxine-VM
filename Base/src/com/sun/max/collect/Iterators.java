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


public final class Iterators {

    private Iterators() {
    }

    private static final Iterator<Object> EMPTY_ITERATOR = new Iterator<Object>() {
        public boolean hasNext() {
            return false;
        }

        public Object next() {
            throw new NoSuchElementException();
        }
        public void remove() {
            throw new UnsupportedOperationException();
        }
    };

    public static <Element_Type> Iterator<Element_Type> empty() {
        final Class<Iterator<Element_Type>> type = null;
        return StaticLoophole.cast(type, EMPTY_ITERATOR);
    }

    public static <Element_Type> Iterable<Element_Type> iterable(Element_Type[] array) {
        return Arrays.iterable(array);
    }

    public static <Element_Type> Iterator<Element_Type> iterator(Element_Type[] array) {
        return Arrays.iterator(array);
    }

    @SuppressWarnings("unchecked")
    public static <Element_Type> Iterator<Element_Type> join(Iterator<Element_Type> iterator1, Iterator<Element_Type> iterator2) {
        return join0(iterator1, iterator2);
    }

    @SuppressWarnings("unchecked")
    public static <Element_Type> Iterator<Element_Type> join(Iterator<Element_Type> iterator1, Iterator<Element_Type> iterator2, Iterator<Element_Type> iterator3) {
        return join0(iterator1, iterator2, iterator3);
    }

    @SuppressWarnings("unchecked")
    public static <Element_Type> Iterator<Element_Type> join(Iterator<Element_Type> iterator1, Iterator<Element_Type> iterator2, Iterator<Element_Type> iterator3, Iterator<Element_Type> iterator4) {
        return join0(iterator1, iterator2, iterator3, iterator4);
    }

    /**
     * Creates an iterator that traverses all the elements represented by a given list of iterators.
     *
     * @param iterators the individual iterators that are to be composed into a single iterator
     * @return the composite iterator
     */
    private static <Element_Type> Iterator<Element_Type> join0(final Iterator<Element_Type>... iterators) {
        if (iterators.length == 0) {
            return empty();
        }
        return new Iterator<Element_Type>() {
            int currentIteratorIndex;
            public boolean hasNext() {
                if (currentIteratorIndex == iterators.length) {
                    return false;
                }
                if (iterators[currentIteratorIndex].hasNext()) {
                    return true;
                }
                currentIteratorIndex++;
                return hasNext();
            }

            public Element_Type next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return iterators[currentIteratorIndex].next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

}
