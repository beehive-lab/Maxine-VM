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
 * Tests for {@link ByteVector}.
 *
 * @author Hiroshi Yamauchi
 */
public class ByteVectorTest extends MaxTestCase {

    public ByteVectorTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ByteVectorTest.class);
    }

    public static ByteVector makeByteVector(int length) {
        final byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) (i % 128);
        }
        return new ByteVector(bytes);
    }

    public void test_isEmpty() {
        final ByteVector bv1 = new ByteVector();
        final ByteVector bv2 = makeByteVector(100);
        assertTrue(bv1.isEmpty());
        assertFalse(bv2.isEmpty());
        bv1.add((byte) 0);
        assertFalse(bv1.isEmpty());
    }

    public void test_length() {
        final ByteVector bv1 = new ByteVector();
        final ByteVector bv2 = makeByteVector(100);
        assertTrue(bv1.length() == 0);
        assertTrue(bv2.length() == 100);
        bv2.add((byte) 0);
        assertTrue(bv2.length() == 101);
    }

    public void test_add() {
        final ByteVector bv1 = new ByteVector();
        for (int i = 0; i < 100000; i++) {
            final int len = bv1.length();
            bv1.add((byte) (i % 128));
            assertTrue(bv1.length() == len + 1);
        }
        final ByteVector bv2 = makeByteVector(100000);
        for (int i = 0; i < 100000; i++) {
            assertTrue(bv1.get(i) == bv2.get(i));
        }
    }

    public void test_get() {
        final ByteVector bv2 = makeByteVector(100000);
        for (int i = 0; i < 100000; i++) {
            assertTrue((byte) (i % 128) == bv2.get(i));
        }
        try {
            bv2.get(-1);
            fail("ByteVector.get() failed");
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
        }
    }

    public void test_set() {
        final ByteVector bv2 = makeByteVector(100000);
        for (int i = 0; i < 100000; i++) {
            bv2.set(i, (byte) ((i % 128) - 1));
        }
        for (int i = 0; i < 100000; i++) {
            assertTrue(bv2.get(i) == (byte) ((i % 128) - 1));
        }
        try {
            bv2.set(-1, (byte) 1);
            fail("ByteVector.set() failed");
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
        }
    }

    public void test_put() {
        final ByteVector bv2 = makeByteVector(0);
        for (int i = 0; i < 100000; i++) {
            try {
                bv2.put(i, (byte) ((i % 128) - 1));
            } catch (Exception exception) {
                fail("ByteVector.put() failed");
            }
        }
        for (int i = 0; i < 100000; i++) {
            assertTrue(bv2.get(i) == (byte) ((i % 128) - 1));
        }
        try {
            bv2.put(-1, (byte) 1);
            fail("ByteVector.put() failed");
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
        }
    }
}
