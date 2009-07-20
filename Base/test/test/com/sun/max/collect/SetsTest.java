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
import com.sun.max.util.*;

/**
 * Tests for {@link Sets}.
 *
 * @author Michael Van De Vanter
 */
public class SetsTest extends MaxTestCase {

    public SetsTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SetsTest.class);
    }

    public void test_empty() {
        assertEquals(Sets.empty(Object.class).size(), 0);
    }

    private Object[] makeObjectArray(int nElements) {
        final Object[] array = new Object[nElements];
        for (int i = 0; i < array.length; i++) {
            array[i] = i;
        }
        return array;
    }

    private Integer[] makeIntegerArray(int nElements) {
        final Integer[] array = new Integer[nElements];
        for (int i = 0; i < array.length; i++) {
            array[i] = i;
        }
        return array;
    }

    public void test_from() {
        final Object[] array = makeObjectArray(3);
        final java.util.Set<Object> set = Sets.from(array[0], array[1], array[2]);
        assertEquals(set.size(), 3);
        for (Object o : array) {
            assertTrue(set.contains(o));
        }
    }

    public void test_addAll() {
        final Integer[] ints10 = makeIntegerArray(10);
        java.util.Set<Integer> orig = Sets.from(ints10);
        java.util.Set<Integer> mod = Sets.from(ints10);

        java.util.Set<Integer> result = Sets.addAll(mod, ints10[0], ints10[1], ints10[8]);
        assertTrue(result.equals(orig));
        assertTrue(mod.equals(orig));
        result = Sets.addAll(mod, new Integer(99));
        assertFalse(result.equals(orig));
        assertFalse(mod.equals(orig));

        final Integer[] ints9 = makeIntegerArray(9);
        orig = Sets.from(ints9);
        mod = Sets.from(ints9[0]);
        result = Sets.addAll(mod, ints9);
        assertTrue(mod.equals(orig));
    }

    public void test_union() {
        final Integer[] ints = makeIntegerArray(5);
        final java.util.Set<Integer> set = Sets.from(ints);
        assertTrue(set.equals(Sets.union(set, set)));
        final java.util.Set<Integer> setEvens = Sets.from(ints[0], ints[2], ints[4]);
        final java.util.Set<Integer> setOdds = Sets.from(ints[1], ints[3]);
        assertFalse(set.equals(setEvens));
        assertFalse(set.equals(setOdds));
        assertTrue(set.equals(Sets.union(setEvens, setOdds)));
    }

    private final Predicate<Integer> nonNegPred = new Predicate<Integer>() {
        public boolean evaluate(Integer i) {
            return i >= 0;
        }
    };
    private final Predicate<Integer> posPred = new Predicate<Integer>() {
        public boolean evaluate(Integer i) {
            return i > 0;
        }
    };

    public void test_filter() {
        final Integer[] ints = makeIntegerArray(3);
        final java.util.Set<Integer> set1 = Sets.from(ints);
        final java.util.Set<Integer> set2 = Sets.from(ints);
        assertTrue(set1.equals(set2));
        final java.util.Set<Integer> filtered = Sets.filter(set1, posPred);
        assertFalse(filtered.equals(set2));
        assertTrue(set2.remove(ints[0]));
        assertTrue(filtered.equals(set2));
    }
}
