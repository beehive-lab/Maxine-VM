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

import com.sun.max.*;
import com.sun.max.ide.*;
import com.sun.max.vm.cps.collect.*;

/**
 * Tests for {@link ListBag}.
 *
 * @author Michael Van De Vanter
 */
public class ListBagTest extends MaxTestCase {

    public ListBagTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ListBagTest.class);
    }

    private final int nKeys = 10;
    private String[] keys = new String[nKeys];
    private Integer[] vals = new Integer[nKeys];

    private void initialize() {
        for (int i = 0; i < nKeys; i++) {
            keys[i] = "key" + i;
            vals[i] = new Integer(i);
        }
    }

    private void check_nonempty(ListBag<String, Integer> map) {
        for (int i = 0; i < nKeys; i++) {
            final String key = keys[i];
            for (int j = 0; j < i; j++) {
                map.add(key, vals[j]);
            }
        }
        // should be no entry for key 0
        assertFalse(map.containsKey(keys[0]));
        assertSame(Collections.emptyList(), map.get(keys[0]));
        final Set<String> keySet = map.keys();
        assertEquals(keySet.size(), nKeys - 1);
        assertFalse(keySet.contains(keys[0]));
        // check all the keys that were added for correct values
        for (int i = 1; i < nKeys; i++) {
            final String key = keys[i];
            assertTrue(map.containsKey(key));
            assertTrue(keySet.contains(key));
            final List<Integer> values = map.get(key);
            for (int j = 0; j < i; j++) {
                assertTrue(Utils.indexOfIdentical(values, vals[j]) != -1);
            }
            for (int j = i; j < nKeys; j++) {
                assertFalse(Utils.indexOfIdentical(values, vals[j]) != -1);
            }
        }
        // iterate over all values in map (union of all collections)
        int valuesFound = 0;
        for (List<Integer> list : map.collections()) {
            for (Integer val : list) {
                valuesFound++;
                assertTrue(Utils.indexOfIdentical(vals, val) >= 0);
            }
        }
        assertEquals(valuesFound, nKeys * (nKeys - 1) / 2);
    }

    public void test_sorted() {
        initialize();
        final ListBag<String, Integer> map = new ListBag<String, Integer>(ListBag.MapType.SORTED);
        check_nonempty(map);
        // Check order of keys
        int i = 1;
        for (String key : map.keys()) {
            assertSame(key, keys[i]);
            i++;
        }
        assertEquals(i, nKeys);
        // Check order and content of collections
        i = 1;
        for (List<Integer> values : map.collections()) {
            for (int j = 0; j < i; j++) {
                assertTrue(Utils.indexOfIdentical(values, vals[j]) != -1);
            }
            for (int j = i; j < nKeys; j++) {
                assertFalse(Utils.indexOfIdentical(values, vals[j]) != -1);
            }
            i++;
        }
        assertEquals(i, nKeys);
    }

    public void test_hashed() {
        initialize();
        final ListBag<String, Integer> map = new ListBag<String, Integer>(ListBag.MapType.HASHED);
        check_nonempty(map);
        // check by iterating (unordered) over collections in map
        int collectionsFound = 0;
        for (List<Integer> collection : map.collections()) {
            collectionsFound++;
            for (Integer val : collection) {
                assertTrue(Utils.indexOfIdentical(vals, val) >= 0);
            }
        }
        assertEquals(collectionsFound, nKeys - 1);
    }

    public void test_identityHashed() {
        initialize();
        final ListBag<String, Integer> map = new ListBag<String, Integer>(ListBag.MapType.IDENTITY);
        check_nonempty(map);
        // check by iterating (unordered) over collections in map
        int collectionsFound = 0;
        for (List<Integer> collection : map.collections()) {
            collectionsFound++;
            for (Integer val : collection) {
                assertTrue(Utils.indexOfIdentical(vals, val) >= 0);
            }
        }
        assertEquals(collectionsFound, nKeys - 1);
    }

}
