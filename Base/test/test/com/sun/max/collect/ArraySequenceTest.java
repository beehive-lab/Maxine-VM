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
/*VCSID=33ff3576-047e-48a4-8202-afb79c8d4c3c*/
package test.com.sun.max.collect;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.ide.*;

/**
 * Tests for {@link ArraySequence}.
 *
 * @author Hiroshi Yamauchi
 */
public class ArraySequenceTest extends MaxTestCase {

    public ArraySequenceTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ArraySequenceTest.class);
    }

    private AppendableIndexedSequence<Integer> makeIntegerArraySequence(int nElements) {
        final Integer[] array = new Integer[nElements];
        for (int i = 0; i < array.length; i++) {
            array[i] = new Integer(i);
        }
        return new ArrayListSequence<Integer>(array);
    }

    public void test_length() {
        final IndexedSequence<Integer> seq1 = makeIntegerArraySequence(99);
        final IndexedSequence<Integer> seq2 = makeIntegerArraySequence(99);
        final IndexedSequence<Integer> seq3 = makeIntegerArraySequence(100);
        final IndexedSequence<Integer> seq4 = makeIntegerArraySequence(0);
        assertTrue(seq1.length() == seq2.length());
        assertTrue(seq1.length() != seq3.length());
        assertTrue(seq2.length() + 1 == seq3.length());
        assertTrue(seq4.length() == 0);
    }

    public void test_isEmpty() {
        final IndexedSequence<Integer> seq1 = new ArraySequence<Integer>();
        final IndexedSequence<Integer> seq2 = new ArraySequence<Integer>(new Integer(3));
        final IndexedSequence<Integer> seq3 = makeIntegerArraySequence(100);
        assertTrue(seq1.isEmpty());
        assertTrue(!seq2.isEmpty());
        assertTrue(!seq3.isEmpty());
    }

    public void test_get() {
        final IndexedSequence<Integer> seq1 = makeIntegerArraySequence(99);
        for (int i = 0; i < 99; i++) {
            assertTrue(seq1.get(i) == i);
        }
    }

    public void test_first_and_last() {
        final IndexedSequence<Integer> seq1 = makeIntegerArraySequence(99);
        assertTrue(seq1.first() == 0);
        assertTrue(seq1.last() == 98);
    }

    public void test_contains() {
        final IndexedSequence<Integer> seq1 = makeIntegerArraySequence(99);
        for (int i = 0; i < 99; i++) {
            assertTrue(Sequence.Static.containsEqual(seq1, new Integer(i)));
        }
    }

    public void test_add() {
        final AppendableIndexedSequence<Integer> seq1 = makeIntegerArraySequence(0);
        final AppendableIndexedSequence<Integer> seq2 = makeIntegerArraySequence(0);
        final AppendableIndexedSequence<Integer> seq3 = makeIntegerArraySequence(99);
        assertTrue(seq1.equals(seq2));
        for (int i = 0; i < 95; i++) {
            seq1.append(new Integer(i));
            seq2.append(new Integer(i));
            assertTrue(seq1.equals(seq2));
        }
        AppendableSequence.Static.appendAll(seq1, new Integer(95), new Integer(96), new Integer(97), new Integer(98));
        assertTrue(seq1.equals(seq3));
    }

    public void test_equals() {
        final AppendableIndexedSequence<Integer> seq1 = makeIntegerArraySequence(99);
        final AppendableIndexedSequence<Integer> seq2 = makeIntegerArraySequence(99);
        final AppendableIndexedSequence<Integer> seq3 = makeIntegerArraySequence(100);
        assertTrue(seq1.equals(seq2));
        assertTrue(!seq2.equals(seq3));
        seq2.get(34);
        final AppendableSequence<Integer> seq4 = new ArrayListSequence<Integer>();
        final AppendableSequence<Integer> seq5 = new ArrayListSequence<Integer>();
        assertTrue(seq4.equals(seq5));
        seq4.append(new Integer(123));
        seq5.append(new Integer(123));
        assertTrue(seq4.equals(seq5));
        seq4.append(new Integer(456));
        assertTrue(!seq4.equals(seq5));
        seq5.append(new Integer(789));
        assertTrue(!seq4.equals(seq5));
    }

    public void test_null() {
        final Sequence<Object> seq1 = new ArraySequence<Object>(new Object[]{null});
        final Sequence<Object> seq2 = new ArraySequence<Object>(new Object[]{null});
        assertTrue(seq1.equals(seq2));
    }

    public void test_clone() {
        final Sequence<Integer> seq1 = makeIntegerArraySequence(128);
        final Sequence<Integer> seq2 = new ArrayListSequence<Integer>(seq1);
        assertTrue(seq1.equals(seq2));

    }

    public void test_iterator() {
        final AppendableSequence<Integer> seq1 = makeIntegerArraySequence(128);
        int sum = 0;
        for (final Iterator<Integer> it = seq1.iterator(); it.hasNext();) {
            final Integer elem = it.next();
            sum += elem;
        }
        assertTrue(sum == 127 * 128 / 2);
    }
}
