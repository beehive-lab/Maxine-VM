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
/*VCSID=70a86965-28eb-422d-8614-ed42c1ef6184*/
package test.com.sun.max.collect;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.ide.*;

/**
 * Tests for {@link LinkedIdentityHashSet}.
 *
 * @author Michael Van De Vanter
 */
public class LinkedIdentityHashSetTest extends MaxTestCase {

    public LinkedIdentityHashSetTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(LinkedIdentityHashSetTest.class);
    }

    private Integer[] makeIntegerArray(int nElements) {
        final Integer[] array = new Integer[nElements];
        for (int i = 0; i < array.length; i++) {
            array[i] = i;
        }
        return array;
    }

    public void test_linkedIdentityHashSet() {
        final Integer theElement = 0;
        final Integer[] theArray = new Integer[1];
        theArray[0] = theElement;
        final LinkedIdentityHashSet<Integer> set1 = new LinkedIdentityHashSet<Integer>();
        set1.add(theElement);
        final LinkedIdentityHashSet<Integer> set2 = new LinkedIdentityHashSet<Integer>(theElement);
        final LinkedIdentityHashSet<Integer> set3 = new LinkedIdentityHashSet<Integer>(theArray);
        assertSame(set1.first(), set2.first());
        assertSame(set1.first(), set3.first());
    }

    public void test_add() {
        final LinkedIdentityHashSet<Integer> set = new LinkedIdentityHashSet<Integer>();
        final Integer theInt = 99;
        set.add(theInt);
        assertTrue(set.contains(theInt));
        set.add(theInt);
        assertTrue(set.contains(theInt));
        assertEquals(set.length(), 1);
        assertFalse(set.contains(new Integer(99)));

        assertFalse(set.contains(null));
        set.add(null);
        assertTrue(set.contains(null));
    }

    private void check_iterator(int nElements) {
        final Integer[] ints = makeIntegerArray(nElements);
        final LinkedIdentityHashSet<Integer> set = new LinkedIdentityHashSet<Integer>();
        for (Integer theInt : ints) {
            set.add(theInt);
        }
        int i = 0;
        for (Integer theInt : set) {
            assertSame(theInt, ints[i]);
            i++;
        }
        assertEquals(i, nElements);
    }

    public void test_iterator() {
        check_iterator(0);
        check_iterator(1);
        check_iterator(2);
        check_iterator(10);
        check_iterator(10000);

        final LinkedIdentityHashSet<Integer> set = new LinkedIdentityHashSet<Integer>();
        set.add(null);
        assertSame(set.iterator().next(), null);

    }

    public void test_remove() {
        final LinkedIdentityHashSet<Integer> set = new LinkedIdentityHashSet<Integer>();
        final Integer[] ints = makeIntegerArray(100);
        set.remove(ints[10]);
        assertTrue(set.isEmpty());
        set.addAll(Arrays.asList(ints));
        assertTrue(set.contains(ints[10]));
        set.remove(ints[10]);
        assertFalse(set.contains(ints[10]));
        set.remove(ints[10]);
        assertFalse(set.contains(ints[10]));
    }

    public void test_clear() {
        final LinkedIdentityHashSet<Integer> set = new LinkedIdentityHashSet<Integer>();
        final Integer[] ints = makeIntegerArray(100);
        set.addAll(Arrays.asList(ints));
        set.clear();
        assertTrue(set.isEmpty());
    }

    public void test_first() {
        final LinkedIdentityHashSet<Integer> set = new LinkedIdentityHashSet<Integer>();
        Integer theFirst;
        try {
            theFirst = set.first();
            fail("first() should throw NoSuchElementException when called on empty set");
        } catch (NoSuchElementException noSuchElementException) {
        }
        theFirst = 99;
        set.add(theFirst);
        assertSame(set.first(), theFirst);
        final Integer[] ints = makeIntegerArray(100);
        set.addAll(Arrays.asList(ints));
        assertSame(set.first(), theFirst);
        set.remove(theFirst);
        assertNotSame(set.first(), theFirst);
        assertSame(set.first(), ints[0]);
    }

    private void check_last(int nElements) {
        final Integer[] ints = makeIntegerArray(nElements);
        final LinkedIdentityHashSet<Integer> set = new LinkedIdentityHashSet<Integer>();
        for (Integer theInt : ints) {
            set.add(theInt);
        }
        Integer theLast;
        for (int i = nElements - 1; i >= 0; i--) {
            theLast = set.last();
            assertSame(theLast, ints[i]);
            set.remove(theLast);
        }
        try {
            theLast = set.last();
            fail("last() should throw NoSuchElementException on empty set");
        } catch (NoSuchElementException noSuchElementException) {
        }
    }

    public void test_last() {
        check_last(0);
        check_last(1);
        check_last(2);
        check_last(10);
        check_last(100);
    }

    public void test_equals() {
        assertTrue((new LinkedIdentityHashSet<Integer>()).equals(new LinkedIdentityHashSet<Integer>()));
        assertFalse((new LinkedIdentityHashSet<Integer>()).equals(null));

        final Integer[] ints = makeIntegerArray(10);
        final LinkedIdentityHashSet<Integer> set1 = new LinkedIdentityHashSet<Integer>(ints);
        final LinkedIdentityHashSet<Integer> set2 = new LinkedIdentityHashSet<Integer>(ints);
        assertTrue(set1.equals(set2));
        assertTrue(set2.equals(set1));

        set1.remove(set1.first());
        assertFalse(set1.equals(set2));
        assertFalse(set2.equals(set1));
        set2.remove(set2.first());
        assertTrue(set1.equals(set2));
        assertTrue(set2.equals(set1));

        set1.add(99);
        assertFalse(set1.equals(set2));
        assertFalse(set2.equals(set1));
    }

}
