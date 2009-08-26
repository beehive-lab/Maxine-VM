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
 * Tests for {@link PoolBitSet}.
 *
 * @author Michael Van De Vanter
 * @author Doug Simon
 */
public class PoolSetTest extends MaxTestCase {

    public PoolSetTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PoolSetTest.class);
    }

    private static class TestElement implements PoolObject {

        private int serial;

        public TestElement(int n) {
            serial = n;
        }

        public int serial() {
            return serial;
        }

        @Override
        public String toString() {
            return String.valueOf(serial);
        }
    }

    public void test_emptyPoolSet() {
        final Pool<TestElement> emptyPool = new IndexedSequencePool<TestElement>(IndexedSequence.Static.empty(TestElement.class));
        final PoolSet<TestElement> poolSet = PoolSet.noneOf(emptyPool);
        assertSame(poolSet.pool(), emptyPool);
        assertEquals(poolSet.length(), 0);
        assertTrue(poolSet.isEmpty());
        poolSet.clear();
        poolSet.addAll();
        assertTrue(poolSet.isEmpty());
        final PoolSet<TestElement> clone = poolSet.clone();
        assertSame(clone.pool(), emptyPool);
        assertEquals(clone.length(), 0);
        assertTrue(clone.isEmpty());
    }

    private int nElems;
    private TestElement[] elems;
    private Pool<TestElement> pool;

    private void foreachPool(Runnable runnable) {
        for (int numElems : new int[] {0, 1, 63, 64, 65, 127, 128, 129, 1000}) {
            this.nElems = numElems;
            elems = new TestElement[numElems];
            for (int i = 0; i < numElems; i++) {
                elems[i] = new TestElement(i);
            }
            pool = new IndexedSequencePool<TestElement>(new ArraySequence<TestElement>(elems));
            runnable.run();
            pool = new ArrayPool<TestElement>(elems);
            runnable.run();
        }
    }

    private void check_poolSet(PoolSet<TestElement> poolSet, int n) {
        assertEquals(poolSet.length(), n);
        for (int i = 0; i < n; i++) {
            assertTrue(poolSet.contains(elems[i]));
        }
        for (int i = n; i < nElems; i++) {
            assertFalse(poolSet.contains(elems[i]));
        }
    }

    public void test_poolBitSet() {
        foreachPool(new Runnable() {
            public void run() {
                final PoolSet<TestElement> poolSet = PoolSet.noneOf(pool);
                for (int i = 0; i < nElems; i++) {
                    check_poolSet(poolSet, i);
                    poolSet.add(elems[i]);
                }
                check_poolSet(poolSet, nElems);
            }
        });
    }

    public void test_remove() {
        foreachPool(new Runnable() {
            public void run() {
                final PoolSet<TestElement> poolSet = PoolSet.noneOf(pool);
                poolSet.addAll();
                assertEquals(poolSet.length(), nElems);
                for (int i = 0; i < nElems; i++) {
                    assertTrue(poolSet.contains(elems[i]));
                    poolSet.remove(elems[i]);
                    assertFalse(poolSet.contains(elems[i]));
                    assertEquals(poolSet.length(), nElems - i - 1);
                }
            }
        });
    }

    public void test_removeOne() {
        foreachPool(new Runnable() {
            public void run() {
                final PoolSet<TestElement> poolSet = PoolSet.noneOf(pool);
                poolSet.addAll();
                assertEquals(poolSet.length(), nElems);
                if (nElems > 0) {
                    final TestElement elem = poolSet.removeOne();
                    assertFalse(poolSet.contains(elem));
                    assertEquals(poolSet.length(), nElems - 1);
                } else {
                    try {
                        poolSet.removeOne();
                        fail();
                    } catch (NoSuchElementException noSuchElementException) {
                    }
                }
            }
        });
    }

    public void test_pool() {
        foreachPool(new Runnable() {
            public void run() {
                final PoolSet<TestElement> poolSet = PoolSet.noneOf(pool);
                assertSame(poolSet.pool(), pool);
            }
        });
    }

    public void test_clear() {
        foreachPool(new Runnable() {
            public void run() {
                final PoolSet<TestElement> poolSet = PoolSet.noneOf(pool);
                poolSet.addAll();
                poolSet.clear();
                check_poolSet(poolSet, 0);
                assertEquals(poolSet.length(), 0);
                for (int i = 0; i < nElems; i++) {
                    assertFalse(poolSet.contains(elems[i]));
                }
            }
        });
    }

    public void test_addAll() {
        foreachPool(new Runnable() {
            public void run() {
                final PoolSet<TestElement> poolSet1 = PoolSet.noneOf(pool);
                poolSet1.addAll();
                check_poolSet(poolSet1, nElems);

                final PoolSet<TestElement> evenSet = PoolSet.noneOf(pool);
                for (int i = 0; i < nElems; i += 2) {
                    evenSet.add(elems[i]);
                }
                final PoolSet<TestElement> oddSet = PoolSet.noneOf(pool);
                for (int i = 1; i < nElems; i += 2) {
                    oddSet.add(elems[i]);
                }
                final PoolSet<TestElement> poolSet2 = PoolSet.noneOf(pool);
                poolSet2.or(oddSet);
                assertEquals(poolSet2.length(), nElems / 2);
                poolSet2.or(evenSet);
                assertEquals(poolSet2.length(), nElems);
                check_poolSet(poolSet2, nElems);
            }
        });
    }

    public void test_and() {
        foreachPool(new Runnable() {
            public void run() {
                final PoolSet<TestElement> poolSet = PoolSet.noneOf(pool);
                poolSet.addAll();
                final PoolSet<TestElement> oddSet = PoolSet.noneOf(pool);
                for (int i = 1; i < nElems; i += 2) {
                    oddSet.add(elems[i]);
                }
                poolSet.and(oddSet);
                assertEquals(poolSet.length(), oddSet.length());
                for (TestElement elem : poolSet) {
                    assertTrue(oddSet.contains(elem));
                }
            }
        });
    }

    public void test_containsAll() {
        foreachPool(new Runnable() {
            public void run() {
                final PoolSet<TestElement> emptyPoolSet = PoolSet.noneOf(pool);
                final PoolSet<TestElement> fullPoolSet = PoolSet.allOf(pool);
                if (nElems == 0) {
                    assertTrue(emptyPoolSet.containsAll(fullPoolSet));
                    assertTrue(fullPoolSet.containsAll(emptyPoolSet));
                } else {
                    assertFalse(emptyPoolSet.containsAll(fullPoolSet));
                    assertTrue(fullPoolSet.containsAll(emptyPoolSet));
                }


                final PoolSet<TestElement> poolSet = PoolSet.noneOf(pool);
                for (int i = 0; i < nElems; i++) {
                    poolSet.add(elems[i]);
                    assertTrue(poolSet.containsAll(emptyPoolSet));
                    assertTrue(fullPoolSet.containsAll(poolSet));
                    if (i == nElems - 1) {
                        assertTrue(poolSet.containsAll(fullPoolSet));
                    } else {
                        assertFalse(poolSet.containsAll(fullPoolSet));
                    }

                }
                poolSet.clear();
                for (int i = nElems - 1; i >= 0; i--) {
                    poolSet.add(elems[i]);
                    assertTrue(poolSet.containsAll(emptyPoolSet));
                    assertTrue(fullPoolSet.containsAll(poolSet));
                    if (i == 0) {
                        assertTrue(poolSet.containsAll(fullPoolSet));
                    } else {
                        assertFalse(poolSet.containsAll(fullPoolSet));
                    }

                }
            }
        });
    }

    public void test_clone() {
        foreachPool(new Runnable() {
            public void run() {
                final PoolSet<TestElement> evenSet = PoolSet.noneOf(pool);
                for (int i = 0; i < nElems; i += 2) {
                    evenSet.add(elems[i]);
                }
                final PoolSet<TestElement> clone = evenSet.clone();
                assertSame(clone.pool(), pool);
                assertEquals(evenSet.length(), clone.length());
                for (TestElement elem : evenSet) {
                    assertTrue(clone.contains(elem));
                }
            }
        });
    }

    public void test_isEmpty() {
        foreachPool(new Runnable() {
            public void run() {
                final PoolSet<TestElement> poolSet = PoolSet.noneOf(pool);
                assertTrue(poolSet.isEmpty());
                if (nElems > 0) {
                    poolSet.add(elems[0]);
                    assertFalse(poolSet.isEmpty());
                    poolSet.addAll();
                    assertFalse(poolSet.isEmpty());
                    poolSet.clear();
                    assertTrue(poolSet.isEmpty());
                }
            }
        });
    }

    public void test_iterator() {
        foreachPool(new Runnable() {
            public void run() {
                final PoolSet<TestElement> poolSet = PoolSet.noneOf(pool);
                for (TestElement elem : poolSet) {
                    assertTrue(poolSet.contains(elem));
                }
                for (int i = 0; i < nElems; i++) {
                    poolSet.add(elems[i]);
                    for (TestElement elem : poolSet) {
                        assertTrue(poolSet.contains(elem));
                    }
                }
            }
        });
    }

    public void test_staticAddAll() {
        foreachPool(new Runnable() {
            public void run() {
                final PoolSet<TestElement> poolSet = PoolSet.noneOf(pool);
                PoolSet.addAll(poolSet, java.util.Arrays.asList(elems));
                check_poolSet(poolSet, nElems);
            }
        });
    }

    public void test_staticToArray() {
        foreachPool(new Runnable() {
            public void run() {
                final PoolSet<TestElement> poolSet = PoolSet.noneOf(pool);
                poolSet.addAll();
                final TestElement[] array = PoolSet.toArray(poolSet, TestElement.class);
                assertTrue(java.util.Arrays.equals(array, elems));
            }
        });
    }

    public void test_allOf() {
        foreachPool(new Runnable() {
            public void run() {
                final PoolSet<TestElement> poolBitSet = PoolSet.allOf(pool);
                check_poolSet(poolBitSet, nElems);
            }
        });
    }

}
