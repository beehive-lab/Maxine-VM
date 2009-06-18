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
 * Tests for {@link Mapping} implementations.
 *
 * @author Doug Simon
 */
public class MappingTest extends MaxTestCase {

    public MappingTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(MappingTest.class);
    }

    private static final int N = 1000;

    private final Integer[] integers = new Integer[N];

    private void initialize() {
        for (int i = 0; i < N; i++) {
            integers[i] = new Integer(i);
        }
    }

    private void check(Mapping<Integer, Object> table, int n) {
        for (int i = 0; i < n; i++) {
            final Object entry = table.get(i);
            assertSame(entry, integers[i]);
        }
    }

    private Sequence<GrowableMapping<Integer, Object>> mappings() {
        final AppendableSequence<GrowableMapping<Integer, Object>> mappings = new ArrayListSequence<GrowableMapping<Integer, Object>>();
        mappings.append(new OpenAddressingHashMapping<Integer, Object>());
        mappings.append(new ChainedHashMapping<Integer, Object>());
        return mappings;
    }

    public void test_serialPut() {
        initialize();
        for (GrowableMapping<Integer, Object> table : mappings()) {
            for (int i = 0; i < N; i++) {
                final Integer key = i;
                assertEquals(table.get(key), null);
                table.put(i, integers[i] + "");
                table.put(i, integers[i]);
                check(table, i);
            }
        }
    }

    public void test_randomPut() {
        initialize();
        for (GrowableMapping<Integer, Object> table : mappings()) {
            final Random random = new Random();
            final int[] keys = new int[N];
            for (int i = 0; i < N; i++) {
                int k = 0;
                do {
                    k = random.nextInt();
                } while (table.get(k) != null);
                keys[i] = k;
                table.put(k, integers[i] + "");
                table.put(k, integers[i]);
            }
            for (int i = 0; i < N; i++) {
                assertSame(table.get(keys[i]), integers[i]);
            }
        }
    }
}
