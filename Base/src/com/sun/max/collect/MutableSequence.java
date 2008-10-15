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


/**
 * A {@link Sequence} whose existing elements can be modified.
 * 
 * @author Bernd Mathiske
 */
public interface MutableSequence<Element_Type> extends IndexedSequence<Element_Type> {

    /**
     * Sets the value of the element at a given index.
     * 
     * @return the previous value at {@code index}
     */
    Element_Type set(int index, Element_Type value);

    public static final class Static {
        private Static() {
        }

        public static <Element_Type> void copy(IndexedSequence<Element_Type> fromSequence, int fromStartIndex, MutableSequence<Element_Type> toSequence, int toStartIndex, int numberOfElements) {
            for (int i = 0; i < numberOfElements; i++) {
                toSequence.set(toStartIndex + i, fromSequence.get(fromStartIndex + i));
            }
        }

        public static <Element_Type> int copy(Sequence<Element_Type> fromSequence, MutableSequence<Element_Type> toSequence, int toStartIndex) {
            int i = toStartIndex;
            for (Element_Type element : fromSequence) {
                toSequence.set(i, element);
                i++;
            }
            return i;
        }

        public static <Element_Type> void copy(Sequence<Element_Type> fromSequence, MutableSequence<Element_Type> toSequence) {
            copy(fromSequence, toSequence, 0);
        }

    }
}
