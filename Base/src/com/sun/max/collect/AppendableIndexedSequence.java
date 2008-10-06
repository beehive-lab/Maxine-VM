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
/*VCSID=9de48a9f-6bdb-4b09-a18f-fde759151dd8*/
package com.sun.max.collect;

import com.sun.max.util.*;

/**
 * @author Bernd Mathiske
 */
public interface AppendableIndexedSequence<Element_Type> extends AppendableSequence<Element_Type>, IndexedSequence<Element_Type> {

    public final class Static {

        private Static() {
        }

        /**
         * Filters an iterable with a given predicate and return a sequence with the elments that matched the predicate.
         * If the returned sequence will only be iterated over, consider using a {@link FilterIterator} instead.
         */
        public static <Element_Type> AppendableIndexedSequence<Element_Type> filter(Iterable<Element_Type> sequence, Predicate<? super Element_Type> predicate) {
            final AppendableIndexedSequence<Element_Type> result = new ArrayListSequence<Element_Type>();
            for (Element_Type element : sequence) {
                if (predicate.evaluate(element)) {
                    result.append(element);
                }
            }
            return result;
        }
    }
}
