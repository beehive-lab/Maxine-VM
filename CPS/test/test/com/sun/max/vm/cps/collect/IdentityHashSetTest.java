/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package test.com.sun.max.vm.cps.collect;

import java.util.*;

import junit.framework.*;

import com.sun.max.vm.cps.collect.*;

/**
 * Tests for {@link IdentityHashSet}.
 *
 * @author Michael Van De Vanter
 */
public class IdentityHashSetTest extends TestCase {

    public IdentityHashSetTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(IdentityHashSetTest.class);
    }

    private Integer[] makeIntegerArray(int nElements) {
        final Integer[] array = new Integer[nElements];
        for (int i = 0; i < array.length; i++) {
            array[i] = new Integer(i);
        }
        return array;
    }

    private IdentityHashSet<Integer> makeIntegerIdentityHashSet(int nElements) {
        final IdentityHashSet<Integer> result = new IdentityHashSet<Integer>();
        for (int i = 0; i < nElements; i++) {
            result.add(i);
        }
        return result;
    }

    public void test_identityHashSet() {
        final IdentityHashSet<Integer> set = new IdentityHashSet<Integer>();
        assertTrue(set.isEmpty());
    }

    public void test_add() {
        final IdentityHashSet<Integer> set = makeIntegerIdentityHashSet(0);
        final Integer theInt = 99;
        set.add(theInt);
        assertTrue(set.contains(theInt));
        assertFalse(set.contains(new Integer(99)));

        assertFalse(set.contains(null));
        set.add(null);
        assertTrue(set.contains(null));
    }

    public void test_addAll() {
        final IdentityHashSet<Integer> set = makeIntegerIdentityHashSet(0);
        final Integer[] ints = makeIntegerArray(100);
        set.addAll(Arrays.asList(ints));
        for (int i = 0; i < ints.length; i++) {
            assertTrue(set.contains(ints[i]));
        }
    }

    public void test_isEmpty() {
        final IdentityHashSet<Integer> set = new IdentityHashSet<Integer>();
        assertTrue(set.isEmpty());
        assertFalse(set.iterator().hasNext());
        set.add(0);
        assertFalse(set.isEmpty());
        assertTrue(set.iterator().hasNext());
    }

    public void test_clear() {
        final IdentityHashSet<Integer> set = makeIntegerIdentityHashSet(0);
        final Integer[] ints = makeIntegerArray(100);
        set.addAll(Arrays.asList(ints));
        set.clear();
        assertTrue(set.isEmpty());
    }

    public void test_remove() {
        final IdentityHashSet<Integer> set = makeIntegerIdentityHashSet(0);
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

    public void test_iterator() {
        IdentityHashSet<Integer> set = makeIntegerIdentityHashSet(100);
        int i = 0;
        for (Integer theInt : set) {
            assertTrue(set.contains(theInt));
            i++;
        }
        assertEquals(i, 100);

        set = makeIntegerIdentityHashSet(0);
        set.add(null);
        assertSame(set.iterator().next(), null);
    }

    public void test_clone() {
        final Integer[] ints = makeIntegerArray(100);
        final IdentityHashSet<Integer> set1 = makeIntegerIdentityHashSet(0);
        set1.addAll(Arrays.asList(ints));
        final IdentityHashSet<Integer> set2 = set1.clone();
        assertEquals(set1.size(), set2.size());
        for (Integer i : set1) {
            assertTrue(set2.contains(i));
        }
    }

    public void test_contains() {
        final IdentityHashSet<Integer> set = makeIntegerIdentityHashSet(0);
        final Integer theInt = new Integer(99);
        assertFalse(set.contains(theInt));
        set.add(theInt);
        assertTrue(set.contains(theInt));
        assertFalse(set.contains(new Integer(99)));
    }

    public void test_length() {
        final IdentityHashSet<Integer> set = makeIntegerIdentityHashSet(0);
        assertEquals(set.size(), 0);
        final Integer[] ints = makeIntegerArray(100);
        set.addAll(Arrays.asList(ints));
        assertEquals(set.size(), 100);
        set.remove(ints[10]);
        assertEquals(set.size(), 99);
    }

    public void test_union() {
        final Integer[] ints1 = makeIntegerArray(100);
        final IdentityHashSet<Integer> set1 = makeIntegerIdentityHashSet(0);
        set1.addAll(Arrays.asList(ints1));
        final Integer[] ints2 = makeIntegerArray(100);
        final IdentityHashSet<Integer> set2 = makeIntegerIdentityHashSet(0);
        set2.addAll(Arrays.asList(ints2));
        set1.union(set2);
        assertEquals(set1.size(), 200);
        for (int i = 0; i < 100; i++) {
            assertTrue(set1.contains(ints1[i]));
            assertTrue(set1.contains(ints2[i]));
        }
    }

    public void test_isSuperSetOf() {
        final Integer[] ints = makeIntegerArray(100);
        final IdentityHashSet<Integer> set1 = makeIntegerIdentityHashSet(0);
        set1.addAll(Arrays.asList(ints));
        final IdentityHashSet<Integer> set2 = makeIntegerIdentityHashSet(0);
        set2.addAll(Arrays.asList(ints));
        assertTrue(set1.isSuperSetOf(set2));
        assertTrue(set2.isSuperSetOf(set1));
        set2.remove(ints[0]);
        assertTrue(set1.isSuperSetOf(set2));
        assertFalse(set2.isSuperSetOf(set1));
    }

    public void test_isStrictSuperSetOf() {
        final Integer[] ints = makeIntegerArray(100);
        final IdentityHashSet<Integer> set1 = makeIntegerIdentityHashSet(0);
        set1.addAll(Arrays.asList(ints));
        final IdentityHashSet<Integer> set2 = makeIntegerIdentityHashSet(0);
        set2.addAll(Arrays.asList(ints));
        assertFalse(set1.isStrictSuperSetOf(set2));
        assertFalse(set2.isStrictSuperSetOf(set1));
        set2.remove(ints[0]);
        assertTrue(set1.isStrictSuperSetOf(set2));
        assertFalse(set2.isStrictSuperSetOf(set1));
    }

    public void test_isSubSetOf() {
        final Integer[] ints = makeIntegerArray(100);
        final IdentityHashSet<Integer> set1 = makeIntegerIdentityHashSet(0);
        set1.addAll(Arrays.asList(ints));
        final IdentityHashSet<Integer> set2 = makeIntegerIdentityHashSet(0);
        set2.addAll(Arrays.asList(ints));
        assertTrue(set1.isSubSetOf(set2));
        assertTrue(set2.isSubSetOf(set1));
        set2.remove(ints[0]);
        assertFalse(set1.isSubSetOf(set2));
        assertTrue(set2.isSubSetOf(set1));
    }

    public void test_isStrictSubSetOf() {
        final Integer[] ints = makeIntegerArray(100);
        final IdentityHashSet<Integer> set1 = makeIntegerIdentityHashSet(0);
        set1.addAll(Arrays.asList(ints));
        final IdentityHashSet<Integer> set2 = makeIntegerIdentityHashSet(0);
        set2.addAll(Arrays.asList(ints));
        assertFalse(set1.isStrictSubSetOf(set2));
        assertFalse(set2.isStrictSubSetOf(set1));
        set2.remove(ints[0]);
        assertFalse(set1.isStrictSubSetOf(set2));
        assertTrue(set2.isStrictSubSetOf(set1));
    }

    public void test_toArray() {
        final Integer[] ints = makeIntegerArray(100);
        final IdentityHashSet<Integer> set1 = makeIntegerIdentityHashSet(0);
        set1.addAll(Arrays.asList(ints));
        final Integer[] result = new Integer[100];
        set1.toArray(result);
        final IdentityHashSet<Integer> set2 = makeIntegerIdentityHashSet(0);
        set2.addAll(Arrays.asList(result));
        assertEquals(set2.size(), 100);
        for (int i = 0; i < 100; i++) {
            assertTrue(set2.contains(ints[i]));
        }
    }

}
