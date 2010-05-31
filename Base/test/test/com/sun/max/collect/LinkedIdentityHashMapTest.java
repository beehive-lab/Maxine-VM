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
 * Tests for {@link IdentityHashMap}.
 *
 * @author Michael Van De Vanter
 */
public class LinkedIdentityHashMapTest extends MaxTestCase {

    public LinkedIdentityHashMapTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(LinkedIdentityHashMapTest.class);
    }

    private final int nKeys = 100;
    private String[] keys = new String[nKeys];
    private Integer[] vals = new Integer[nKeys];

    private void initialize() {
        for (int i = 0; i < nKeys; i++) {
            keys[i] = "key" + i;
            vals[i] = new Integer(i);
        }
    }

    private void check_serial(LinkedIdentityHashMap<String, Integer> table, int n) {
        int i = 0;
        for (String key : table) {
            assertSame(key, keys[i++]);
        }
        assertEquals(i, n + 1);
        assertEquals(i, table.size());
    }

    public void test_serial() {
        initialize();
        final LinkedIdentityHashMap<String, Integer> table = new LinkedIdentityHashMap<String, Integer>();
        for (int i = 0; i < nKeys; i++) {
            assertEquals(table.get(keys[i]), null);
            table.put(keys[i], vals[i]);
            check_serial(table, i);
        }
    }

    public void test_random() {
        initialize();
        final LinkedIdentityHashMap<String, Integer> table = new LinkedIdentityHashMap<String, Integer>();
        final Random random = new Random();
        final int[] keyOrder = new int[nKeys];
        for (int i = 0; i < nKeys; i++) {
            int k = 0;
            do {
                k = random.nextInt(nKeys);
            } while (table.get(keys[k]) != null);
            keyOrder[i] = k;
            table.put(keys[k], vals[k]);
        }
        int i = 0;
        for (String key : table) {
            assertSame(key, keys[keyOrder[i]]);
            assertSame(table.get(key), vals[keyOrder[i]]);
            i++;
        }
        assertEquals(i, nKeys);
    }

    public void test_equals() {
        initialize();
        final LinkedIdentityHashMap<String, Integer> table1 = new LinkedIdentityHashMap<String, Integer>();
        final LinkedIdentityHashMap<String, Integer> table2 = new LinkedIdentityHashMap<String, Integer>();
        assertTrue(table1.equals(table2));
        assertTrue(table2.equals(table1));
        for (int i = 0; i < nKeys; i++) {
            table1.put(keys[i], vals[i]);
        }
        for (int i = 0; i < nKeys; i++) {
            table2.put(keys[i], vals[i]);
        }
        assertTrue(table1.equals(table2));
        assertTrue(table2.equals(table1));
        table1.put(keys[0], new Integer(-1));
        assertFalse(table1.equals(table2));
        assertFalse(table2.equals(table1));
    }

}
