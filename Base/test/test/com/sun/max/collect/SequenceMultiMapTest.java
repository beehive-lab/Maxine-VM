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
 * Tests for {@link SequenceBag}.
 *
 * @author Michael Van De Vanter
 */
public class SequenceMultiMapTest extends MaxTestCase {

    public SequenceMultiMapTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SequenceMultiMapTest.class);
    }

    private void check_empty(Bag<String, Integer, Sequence<Integer>> map) {
        assertSame(Sequence.Static.empty(Integer.class), map.get("a string"));
        assertFalse(map.containsKey("a string"));
        assertEquals(map.keys().size(), 0);
        for (String key : map.keys()) {
            fail("key" + key + "should not be in empty MultiMap");
        }
        for (Integer val : map) {
            fail("value" + val + "should not be in empty MultiMap");
        }
        for (Sequence<Integer> collection : map.collections()) {
            fail("collection" + collection + "should not be in empty MultiMap");
        }
    }

    public void test_empty() {
        check_empty(new SequenceBag<String, Integer>(SequenceBag.MapType.HASHED));
        check_empty(new SequenceBag<String, Integer>(SequenceBag.MapType.SORTED));
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

    private void check_nonempty(Bag<String, Integer, Sequence<Integer>> map) {
        for (int i = 0; i < nKeys; i++) {
            final String key = keys[i];
            for (int j = 0; j < i; j++) {
                map.add(key, vals[j]);
            }
        }
        // should be no entry for key 0
        assertFalse(map.containsKey(keys[0]));
        assertSame(Sequence.Static.empty(Integer.class), map.get(keys[0]));
        final Set<String> keySet = map.keys();
        assertEquals(keySet.size(), nKeys - 1);
        assertFalse(keySet.contains(keys[0]));
        // check all the keys that were added for correct values
        for (int i = 1; i < nKeys; i++) {
            final String key = keys[i];
            assertTrue(map.containsKey(key));
            assertTrue(keySet.contains(key));
            final Sequence<Integer> values = map.get(key);
            for (int j = 0; j < i; j++) {
                assertTrue(Sequence.Static.containsIdentical(values, vals[j]));
            }
            for (int j = i; j < nKeys; j++) {
                assertFalse(Sequence.Static.containsIdentical(values, vals[j]));
            }
        }
        // iterate over all values in map (union of all collections)
        int valuesFound = 0;
        for (Integer val : map) {
            valuesFound++;
            assertTrue(com.sun.max.lang.Arrays.contains(vals, val));
        }
        assertEquals(valuesFound, nKeys * (nKeys - 1) / 2);
    }

    public void test_sorted() {
        initialize();
        final Bag<String, Integer, Sequence<Integer>> map = new SequenceBag<String, Integer>(SequenceBag.MapType.SORTED);
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
        for (Sequence<Integer> values : map.collections()) {
            for (int j = 0; j < i; j++) {
                assertTrue(Sequence.Static.containsIdentical(values, vals[j]));
            }
            for (int j = i; j < nKeys; j++) {
                assertFalse(Sequence.Static.containsIdentical(values, vals[j]));
            }
            i++;
        }
        assertEquals(i, nKeys);
    }

    public void test_hashed() {
        initialize();
        final Bag<String, Integer, Sequence<Integer>> map = new SequenceBag<String, Integer>(SequenceBag.MapType.HASHED);
        check_nonempty(map);
        // check by iterating (unordered) over collections in map
        int collectionsFound = 0;
        for (Sequence<Integer> collection : map.collections()) {
            collectionsFound++;
            for (Integer val : collection) {
                assertTrue(com.sun.max.lang.Arrays.contains(vals, val));
            }
        }
        assertEquals(collectionsFound, nKeys - 1);
    }

    public void test_identityHashed() {
        initialize();
        final Bag<String, Integer, Sequence<Integer>> map = new SequenceBag<String, Integer>(SequenceBag.MapType.IDENTITY);
        check_nonempty(map);
        // check by iterating (unordered) over collections in map
        int collectionsFound = 0;
        for (Sequence<Integer> collection : map.collections()) {
            collectionsFound++;
            for (Integer val : collection) {
                assertTrue(com.sun.max.lang.Arrays.contains(vals, val));
            }
        }
        assertEquals(collectionsFound, nKeys - 1);
    }

}
