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
/*VCSID=b753c312-5353-4766-99d4-5738fc0de0a4*/
package com.sun.max.collect;

/**
 * A {@link Sequence} from which elements can be removed.
 * 
 * @author Doug Simon
 */
public interface ShrinkableSequence<Element_Type> extends IndexedSequence<Element_Type> {

    /**
     * Removes all elements.
     */
    void clear();

    /**
     * Removes the element at a given index from this sequence and returns it.
     * 
     * @throws IndexOutOfBoundsException if {@code 0 < index || index >= length()}
     */
    Element_Type remove(int index);

    /**
     * Removes the first element from this sequence and returns it.
     * 
     * @throws IndexOutOfBoundsException if this sequence is {@linkplain Sequence#isEmpty() empty}
     */
    Element_Type removeFirst();

    /**
     * Removes the last element from this sequence and returns it.
     * 
     * @throws IndexOutOfBoundsException if this sequence is {@linkplain Sequence#isEmpty() empty}
     */
    Element_Type removeLast();

    public static final class Static {
        private Static() {
        }

        /**
         * Removes a single specified element from this sequence, if it is present.
         *
         * @return the removed element or null
         */
        public static <Element_Type> Element_Type removeIdentical(ShrinkableSequence<Element_Type> sequence, Element_Type element) {
            for (int i = 0; i < sequence.length(); i++) {
                if (sequence.get(i) == element) {
                    return sequence.remove(i);
                }
            }
            return null;
        }

        /**
         * Removes a single instance equal to a specified element from this sequence, if it is present.
         *
         * @return the removed element or null
         */
        public static <Element_Type> Element_Type removeEqual(ShrinkableSequence<Element_Type> sequence, Element_Type element) {
            for (int i = 0; i < sequence.length(); i++) {
                if (sequence.get(i).equals(element)) {
                    return sequence.remove(i);
                }
            }
            return null;
        }

        /**
         * Removes all instances equal to a specified element from this sequence.
         *
         * @return the remaining length of the sequence
         */
        public static <Element_Type> int removeAllEqual(ShrinkableSequence<Element_Type> sequence, Element_Type element) {
            int i = 0;
            while (i < sequence.length()) {
                if (sequence.get(i).equals(element)) {
                    sequence.remove(i);
                } else {
                    i++;
                }
            }
            return i;
        }

    }
}
