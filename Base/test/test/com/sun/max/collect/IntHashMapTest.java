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
/*VCSID=9b95977e-9e0a-4d53-95f1-9c2286da3ee0*/
package test.com.sun.max.collect;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.ide.*;

/**
 * Tests for {@link IntHashMap}.
 *
 * @author Bernd Mathiske
 */
public class IntHashMapTest extends MaxTestCase {

    public IntHashMapTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(IntHashMapTest.class);
    }

    private static final int N = 1000;

    private final Integer[] _integers = new Integer[N];

    private void initialize() {
        for (int i = 0; i < N; i++) {
            _integers[i] = new Integer(i);
        }
    }

    private void check(IntHashMap<Object> table, int n) {
        for (int i = 0; i < n; i++) {
            final Object entry = table.get(i);
            assertSame(entry, _integers[i]);
        }
    }

    public void test_serialPut() {
        initialize();
        final IntHashMap<Object> table = new IntHashMap<Object>();
        for (int i = 0; i < N; i++) {
            assertEquals(table.get(i), null);
            table.put(i, _integers[i] + "");
            table.put(i, _integers[i]);
            check(table, i);
        }
    }

    public void test_randomPut() {
        initialize();
        final IntHashMap<Object> table = new IntHashMap<Object>();
        final Random random = new Random();
        final int[] keys = new int[N];
        for (int i = 0; i < N; i++) {
            int k = 0;
            do {
                k = random.nextInt();
            } while (table.get(k) != null);
            keys[i] = k;
            table.put(k, _integers[i] + "");
            table.put(k, _integers[i]);
        }
        for (int i = 0; i < N; i++) {
            assertSame(table.get(keys[i]), _integers[i]);
        }
    }

}
