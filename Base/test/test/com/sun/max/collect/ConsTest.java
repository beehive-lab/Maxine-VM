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
 * Tests for {@link Cons}.
 *
 * @author Michael Van De Vanter
 */
public class ConsTest extends MaxTestCase {

    public ConsTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ConsTest.class);
    }

    public void test_head() {
        final Cons<Object> nil = null;
        final Object o = new Integer(0);
        assertSame((new Cons<Object>(o, nil)).head(), o);
    }

    public void test_tail() {
        final Cons<Object> nil = null;
        final Cons<Object> c = new Cons<Object>(new Integer(1), nil);
        assertSame((new Cons<Object>(new Integer(0), c)).tail(), c);
    }

    public void test_length() {
        final Cons<Object> nil = null;
        final Cons<Object> c = new Cons<Object>(new Integer(1), nil);
        //assertEquals(nil.length(), 0);  // oh, well!
        assertEquals(c.length(), 1);
        assertEquals((new Cons<Object>(new Integer(0), c)).length(), 2);
    }

    public void test_iterator() {
        final Cons<Object> nil = null;
        final Integer i0 = new Integer(0);
        final Integer i1 = new Integer(1);
        final Cons<Object> c1 = new Cons<Object>(i1, nil);
        final Cons<Object> c0 = new Cons<Object>(i0, c1);
        final Iterator<Object > iter = c0.iterator();
        assertSame(iter.next(), i0);
        assertSame(iter.next(), i1);
        assertFalse(iter.hasNext());
    }

    public void test_create() {
        final Object[] array = new Object[10];
        for (int i = 0; i < array.length; i++) {
            array[i] = new Integer(i);
        }
        final Cons<Object> cons = Cons.create(Arrays.asList(array));
        int i = 0;
        for (Object o : cons) {
            assertSame(o, array[i++]);
        }
    }

    public void test_createReverse() {
        final Object[] array = new Object[10];
        for (int i = 0; i < array.length; i++) {
            array[i] = new Integer(i);
        }
        final Cons<Object> cons = Cons.createReverse(Arrays.asList(array));
        int i = array.length - 1;
        for (Object o : cons) {
            assertSame(o, array[i--]);
        }
    }

    public void test_equals() {
        final Object[] array10 = new Object[10];
        for (int i = 0; i < array10.length; i++) {
            array10[i] = new Integer(i);
        }
        final Cons<Object> c1 = Cons.create(Arrays.asList(array10));
        final Cons<Object> c2 = Cons.create(Arrays.asList(array10));
        assertTrue(Cons.equals(c1, c2));

        final Object[] array9 = new Object[9];
        for (int i = 0; i < array9.length; i++) {
            array9[i] = new Integer(i);
        }
        final Cons<Object> c3 = Cons.create(Arrays.asList(array9));
        assertFalse(Cons.equals(c1, c3));
        assertFalse(Cons.equals(c3, c1));

        final Object[] arrayMod = new Object[10];
        for (int i = 0; i < arrayMod.length; i++) {
            arrayMod[i] = new Integer(i);
        }
        arrayMod[3] = new String("different");
        final Cons<Object> c4 = Cons.create(Arrays.asList(arrayMod));
        assertFalse(Cons.equals(c1, c4));
        assertFalse(Cons.equals(c4, c1));
    }

}
