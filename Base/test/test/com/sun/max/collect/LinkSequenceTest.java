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
 * Tests for {@link LinkSequence}.
 * 
 * @author Bernd Mathiske
 */
public class LinkSequenceTest extends MaxTestCase {

    public LinkSequenceTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(LinkSequenceTest.class);
    }

    private LinkSequence<Integer> m(int n) {
        final LinkSequence<Integer> s = new LinkSequence<Integer>();
        for (int i = 1; i <= n; i++) {
            s.append(i);
        }
        return s;
    }

    public void test_length() {
        assertTrue(m(0).length() == 0);
        assertTrue(m(1).length() == 1);
        assertTrue(m(2).length() == 2);
        assertTrue(m(3).length() == 3);
        assertTrue(m(100).length() == 100);
    }

    public void test_isEmpty() {
        assertTrue(m(0).isEmpty());
        assertFalse(m(1).isEmpty());
        assertFalse(m(2).isEmpty());
        assertFalse(m(3).isEmpty());
        assertFalse(m(100).isEmpty());
    }

    public void test_first() {
        try {
            m(-1).first();
            fail();
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
        }
        try {
            m(0).first();
            fail();
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
        }
        assertTrue(m(1).first() == 1);
        assertTrue(m(2).first() == 1);
        assertTrue(m(3).first() == 1);
        assertTrue(m(100).first() == 1);
    }

    public void test_last() {
        try {
            m(-1).last();
            fail();
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
        }
        try {
            m(0).last();
            fail();
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
        }
        assertTrue(m(1).last() == 1);
        assertTrue(m(2).last() == 2);
        assertTrue(m(3).last() == 3);
        assertTrue(m(100).last() == 100);
    }

    public void test_equals() {
        assertTrue(m(0).equals(m(0)));
        assertTrue(m(1).equals(m(1)));
        assertTrue(m(2).equals(m(2)));
        assertTrue(m(100).equals(m(100)));
        assertFalse(m(3).equals(this));
        assertFalse(m(0).equals(m(1)));
        assertFalse(m(1).equals(m(0)));
        assertFalse(m(0).equals(m(2)));
        assertFalse(m(2).equals(m(0)));
        assertFalse(m(1).equals(m(2)));
        assertFalse(m(2).equals(m(1)));
        assertFalse(m(90).equals(m(100)));
        assertFalse(m(100).equals(m(99)));
        final LinkSequence s = m(50);
        assertTrue(s.equals(s));
    }

    private void prepend(int n) {
        final LinkSequence<Integer> s = m(n);
        for (int i = 1; i <= 100; i++) {
            final int element = i;
            s.prepend(element);
            assertTrue(s.length() == n + i);
            assertTrue(s.first() == element);
        }
        int i = 0;
        for (Integer element : s) {
            if (i < 100) {
                assertTrue(element == 100 - i);
            } else {
                assertTrue(element == i - 99);
            }
            i++;
        }
    }

    public void test_prepend() {
        prepend(0);
        prepend(1);
        prepend(2);
        prepend(100);
    }

    private void append(int n) {
        final LinkSequence<Integer> s = m(n);
        for (int i = 1; i <= 100; i++) {
            final int element = n + i;
            s.append(element);
            assertTrue(s.length() == element);
            assertTrue(s.last() == element);
        }
        int i = 1;
        for (Integer element : s) {
            assertTrue(element == i);
            i++;
        }
    }

    public void test_append() {
        append(0);
        append(1);
        append(2);
        append(100);
    }

    public void test_clone() {
        assertTrue(m(0).clone().equals(m(0)));
        assertTrue(m(1).clone().equals(m(1)));
        assertTrue(m(2).clone().equals(m(2)));
        assertTrue(m(100).clone().equals(m(100)));
        assertFalse(m(3).clone().equals(this));
        assertFalse(m(0).clone().equals(m(1)));
        assertFalse(m(1).clone().equals(m(0)));
        assertFalse(m(0).clone().equals(m(2)));
        assertFalse(m(2).clone().equals(m(0)));
        assertFalse(m(1).clone().equals(m(2)));
        assertFalse(m(2).clone().equals(m(1)));
        assertFalse(m(90).clone().equals(m(100)));
        assertFalse(m(100).clone().equals(m(99)));
        final LinkSequence s = m(50);
        assertTrue(s.clone().equals(s));
    }

    private void map(int n) {
        final LinkSequence<Integer> in = m(n);
        final LinkSequence<String> out = LinkSequence.map(in, String.class, new MapFunction<Integer, String>() {
            public String map(Integer i) {
                return Integer.toString(i * 2);
            }
        });
        int i = 1;
        for (String s : out) {
            assertTrue(Integer.parseInt(s) == i * 2);
            i++;
        }
    }

    public void test_map() {
        map(0);
        map(1);
        map(2);
        map(100);
    }

}
