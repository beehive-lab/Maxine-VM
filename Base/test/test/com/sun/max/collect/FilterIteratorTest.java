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
package test.com.sun.max.collect;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.ide.*;
import com.sun.max.util.*;

/**
 * Tests for {@link FilterIterator}.
 *
 * @author Michael Van De Vanter
 */

public class FilterIteratorTest extends MaxTestCase {

    public FilterIteratorTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(FilterIteratorTest.class);
    }

    private final Predicate<Integer> _evenPred = new Predicate<Integer>() {
        public boolean evaluate(Integer i) {
            return i % 2 == 0;
        }
    };

    public void test_FilterIterator() {
        final Integer[] array = new Integer[10];
        for (int i = 0; i < array.length; i++) {
            array[i] = new Integer(i);
        }
        final LinkedList<Integer> list = new LinkedList<Integer>(java.util.Arrays.asList(array));
        FilterIterator<Integer> iter = new FilterIterator<Integer>(list.iterator(), _evenPred);
        int i = 0;
        Integer elem;
        while (iter.hasNext()) {
            elem = iter.next();
            assertEquals(elem.intValue(), i);
            i += 2;
        }
        assertEquals(i, 10);
        assertEquals(list.size(), 10);
        try {
            iter = new FilterIterator<Integer>(list.iterator(), _evenPred);
            while (iter.hasNext()) {
                iter.remove();
            }
            fail("FilterIterator.remove() should have thrown IllegalStateException");
        } catch (IllegalStateException illegalStateException) {
        }
    }
}
