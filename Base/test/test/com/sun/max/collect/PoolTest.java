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
 * Tests for {@link IndexedSequencePool}.
 *
 * @author Michael Van De Vanter
 */
public class PoolTest extends MaxTestCase {

    public PoolTest(String name) {
        super(name);
        for (int i = 0; i < nElems; i++) {
            elems[i] = new TestElement(i);
        }
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PoolTest.class);
    }

    private static class TestElement implements PoolObject {
        private int serial;
        public TestElement(int n) {
            serial = n;
        }
        public int serial() {
            return serial;
        }
    }

    public void test_empty() {
        Pool<TestElement> pool = new IndexedSequencePool<TestElement>(IndexedSequence.Static.empty(TestElement.class));
        assertEquals(pool.length(), 0);
        try {
            final TestElement elem = pool.get(0);
            fail(elem + " should not be in empty collection");
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
        }

        pool = new ArrayPool<TestElement>(new TestElement[0]);
        assertEquals(pool.length(), 0);
        try {
            final TestElement elem = pool.get(0);
            fail(elem + " should not be in empty collection");
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
        }
    }

    private final int nElems = 100;
    TestElement[] elems = new TestElement[nElems];

    private void check_pool(Pool<TestElement> pool, int n) {
        assertEquals(pool.length(), n);
        final Iterator<TestElement> iterator = pool.iterator();
        for (int i = 0; i < n; i++) {
            assertTrue(iterator.hasNext());
            final TestElement element = pool.get(i);
            assertEquals(element.serial(), i);
            assertSame(element, elems[i]);
            assertSame(element, iterator.next());
        }
        assertFalse(iterator.hasNext());
    }

    public void test_pool() {
        Pool<TestElement> pool = new IndexedSequencePool<TestElement>(new ArraySequence<TestElement>(elems));
        check_pool(pool, nElems);
        pool = new ArrayPool<TestElement>(elems);
        check_pool(pool, nElems);
    }
}
