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

import com.sun.max.collect.*;
import com.sun.max.ide.*;

/**
 * Tests for {@link IdentitySet}.
 *
 * @author Michael Van De Vanter
 */
public class IdentitySetTest extends MaxTestCase {

    public IdentitySetTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(IdentitySetTest.class);
    }

    private Integer[] makeIntegerArray(int nElements) {
        final Integer[] array = new Integer[nElements];
        for (int i = 0; i < array.length; i++) {
            array[i] = i;
        }
        return array;
    }

    private IdentitySet<Integer> makeIntegerIdentitySet(int nElements) {
        final IdentitySet<Integer> result = new IdentitySet<Integer>(Integer.class);
        for (int i = 0; i < nElements; i++) {
            result.add(i);
        }
        return result;
    }

    public void test_numberOfElements() {
        final IdentitySet<Integer> set = new IdentitySet<Integer>(Integer.class);
        assertEquals(set.numberOfElements(), 0);
        set.add(null);  // should not be added
        for (int i = 0; i < 1000; i++) {
            assertEquals(set.numberOfElements(), i);
            set.add(i);
        }
    }

    private void check_add(IdentitySet<Integer> set) {
        assertEquals(set.numberOfElements(), 0);
        set.add(null);
        assertEquals(set.numberOfElements(), 0);
        set.add(0);
        assertEquals(set.numberOfElements(), 1);
        set.add(1);
        assertEquals(set.numberOfElements(), 2);
    }

    public void test_add() {
        check_add(new IdentitySet<Integer>(Integer.class));
        check_add(new IdentitySet<Integer>(Integer.class, 0));
        check_add(new IdentitySet<Integer>(Integer.class, 1));
        check_add(new IdentitySet<Integer>(Integer.class, 10000));
    }

    public void test_contains() {
        final IdentitySet<Integer> set = new IdentitySet<Integer>(Integer.class);
        final Integer[] ints = makeIntegerArray(1000);
        for (int i = 0; i < 1000; i++) {
            set.add(ints[i]);
        }
        assertEquals(set.numberOfElements(), 1000);
        for (int i = 0; i < 1000; i++) {
            assertTrue(set.contains(ints[i]));
        }
        assertFalse(set.contains(null));
        assertFalse(set.contains(new Integer(0)));
    }

    public void test_iterator() {
        final IdentitySet<Integer> set = new IdentitySet<Integer>(Integer.class);
        final Integer[] ints = makeIntegerArray(1000);
        for (int i = 0; i < 1000; i++) {
            set.add(ints[i]);
        }
        assertEquals(set.numberOfElements(), 1000);
        final IdentitySet<Integer> newSet = new IdentitySet<Integer>(Integer.class);
        assertEquals(newSet.numberOfElements(), 0);
        for (Integer theInt : set) {
            assertNotNull(theInt);
            assertTrue(set.contains(theInt));
            newSet.add(theInt);
        }
        assertEquals(newSet.numberOfElements(), 1000);
    }

}
