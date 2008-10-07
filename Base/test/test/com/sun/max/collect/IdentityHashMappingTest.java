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
 * Tests for {@link IdentityHashMapping}.
 * 
 * @author Hiroshi Yamauchi
 */
public class IdentityHashMappingTest extends MaxTestCase {

    public IdentityHashMappingTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(IdentityHashMappingTest.class);
    }

    private static final class Key {

        private final int _id;

        private Key(int id) {
            _id = id;
        }

        public int id() {
            return _id;
        }

        @Override
        public int hashCode() {
            return _id;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Key)) {
                return false;
            }
            final Key key = (Key) other;
            return _id == key._id;
        }
    }

    private static final class Value {

        private final int _id;

        private Value(int id) {
            _id = id;
        }

        public int id() {
            return _id;
        }
    }

    public void test_basic() {
        final int num = 100000;
        final IdentityHashMapping<Key, Value> map = new IdentityHashMapping<Key, Value>();
        final Key[] keys = new Key[num];
        final Value[] values = new Value[num];
        final Value[] values2 = new Value[num];
        for (int i = 0; i < num; i++) {
            keys[i] = new Key(i);
            values[i] = new Value(i);
            values2[i] = new Value(i * 2);
            map.put(keys[i], values[i]);
            assertTrue(map.containsKey(keys[i]));
            assertEquals(i + 1, map.keys().length());
        }
        for (int i = 0; i < num; i++) {
            assertTrue(map.containsKey(keys[i]));
            assertSame(map.get(keys[i]), values[i]);
        }
        assertFalse(map.containsKey(new Key(-1)));
        for (int i = 0; i < num; i++) {
            map.put(keys[i], values[i]);
            assertSame(map.get(keys[i]), values[i]);
        }
        for (int i = 0; i < num; i++) {
            if ((i % 3) == 0) {
                map.put(keys[i], values2[i]);
            }
        }
        for (int i = 0; i < num; i++) {
            if ((i % 3) == 0) {
                assertSame(map.get(keys[i]), values2[i]);
            } else {
                assertSame(map.get(keys[i]), values[i]);
            }
        }
    }
}
