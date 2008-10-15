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

/**
 * Tests for {@linkplain DeterministicSet.Static.empty} and {@linkplain DeterministicSet.Singleton}.
 *
 * @author Michael Paleczny
 */
public class DeterministicSetTest extends MaxTestCase {

    public DeterministicSetTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(DeterministicSetTest.class);
    }

    public void test_Empty() {
        final DeterministicSet<Integer> empty = DeterministicSet.Static.empty(Integer.class);
        assertTrue(empty.isEmpty());
        assertTrue(empty.length() == 0);

        final Integer integer3 = new Integer(3);
        assertTrue(!empty.contains(integer3));

        int count = 0;
        for (Integer element : empty) {
            if (element == 0 || element != 0) {
                count++;
            }
            fail("Iterator should not return elements from an empty set");
        }
        assertTrue(count == 0);

        try {
            empty.getOne();
        } catch (NoSuchElementException noSuchElementException) {
            return;
        }
        fail("Should not be able to successfully getOne() element from an empty set");
    }

    public void test_Singleton() {
        // test that DeterministicSet can accept new elements and correctly reports containment
        final Integer integer5 = new Integer(5);
        final DeterministicSet<Integer> setOfFive = new DeterministicSet.Singleton<Integer>(integer5);
        assertTrue(setOfFive.contains(integer5));
        assertTrue(setOfFive.isEmpty() == false);
        assertTrue(setOfFive.length()  == 1);
        assertTrue(setOfFive.first() == setOfFive.last());
        assertTrue(setOfFive.first() == setOfFive.getOne());

        final Integer integer5b = new Integer(5);
        assertTrue(!setOfFive.contains(integer5b));

        int count = 0;
        for (Integer element : setOfFive) {
            count++;
            assertTrue(element == integer5);
        }
        assertTrue(count == 1);
    }
}
