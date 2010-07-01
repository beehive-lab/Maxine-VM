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
