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
package test.com.sun.max.lang;

import com.sun.max.ide.*;
import com.sun.max.lang.*;

/**
 * Tests for {@link Longs}.
 *
 * @author Bernd Mathiske
 */
public class LongsTest extends MaxTestCase {

    public LongsTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(LongsTest.class);
    }

    public void test_numberOfEffectiveUnsignedBits() {
        assertTrue(Longs.numberOfEffectiveUnsignedBits(0L) == 0);
        assertTrue(Longs.numberOfEffectiveUnsignedBits(1L) == 1);
        assertTrue(Longs.numberOfEffectiveUnsignedBits(2L) == 2);
        assertTrue(Longs.numberOfEffectiveUnsignedBits(3L) == 2);
        assertTrue(Longs.numberOfEffectiveUnsignedBits(4L) == 3);
        assertTrue(Longs.numberOfEffectiveUnsignedBits(126L) == 7);
        assertTrue(Longs.numberOfEffectiveUnsignedBits(127L) == 7);
        assertTrue(Longs.numberOfEffectiveUnsignedBits(129L) == 8);
        assertTrue(Longs.numberOfEffectiveUnsignedBits(254L) == 8);
        assertTrue(Longs.numberOfEffectiveUnsignedBits(255L) == 8);
        assertTrue(Longs.numberOfEffectiveUnsignedBits(256L) == 9);
        assertTrue(Longs.numberOfEffectiveUnsignedBits(257L) == 9);
    }

    public void test_numberOfEffectiveSignedBits() {
        for (long i = 0; i < 257L; i++) {
            assertTrue(Longs.numberOfEffectiveSignedBits(i) == Longs.numberOfEffectiveUnsignedBits(i) + 1L);
        }
        assertTrue(Longs.numberOfEffectiveSignedBits(0L) == 1);
        assertTrue(Longs.numberOfEffectiveSignedBits(-1L) == 1);
        assertTrue(Longs.numberOfEffectiveSignedBits(-2L) == 2);
        assertTrue(Longs.numberOfEffectiveSignedBits(-3L) == 3);
        assertTrue(Longs.numberOfEffectiveSignedBits(-4L) == 3);
        assertTrue(Longs.numberOfEffectiveSignedBits(-5L) == 4);
        assertTrue(Longs.numberOfEffectiveSignedBits(-6L) == 4);
        assertTrue(Longs.numberOfEffectiveSignedBits(-7L) == 4);
        assertTrue(Longs.numberOfEffectiveSignedBits(-8L) == 4);
        assertTrue(Longs.numberOfEffectiveSignedBits(-9L) == 5);
        assertTrue(Longs.numberOfEffectiveSignedBits(-126L) == 8);
        assertTrue(Longs.numberOfEffectiveSignedBits(-127L) == 8);
        assertTrue(Longs.numberOfEffectiveSignedBits(-128L) == 8);
        assertTrue(Longs.numberOfEffectiveSignedBits(-129L) == 9);
        assertTrue(Longs.numberOfEffectiveSignedBits(-254L) == 9);
        assertTrue(Longs.numberOfEffectiveSignedBits(-255L) == 9);
        assertTrue(Longs.numberOfEffectiveSignedBits(-256L) == 9);
        assertTrue(Longs.numberOfEffectiveSignedBits(-257L) == 10);
    }

    public void test_insert() {
        long[] a = Longs.insert(null, 0, 11L);
        assertTrue(a.length == 1);
        assertTrue(a[0] == 11);

        a = Longs.insert(a, 0, 12);
        assertTrue(a.length == 2);
        assertTrue(a[0] == 12);
        assertTrue(a[1] == 11);

        a = Longs.insert(a, 1, 13);
        assertTrue(a.length == 3);
        assertTrue(a[0] == 12);
        assertTrue(a[1] == 13);
        assertTrue(a[2] == 11);

        a = Longs.insert(a, 3, 14);
        assertTrue(a.length == 4);
        assertTrue(a[0] == 12);
        assertTrue(a[1] == 13);
        assertTrue(a[2] == 11);
        assertTrue(a[3] == 14);

        try {
            Longs.insert(a, -1, 10);
            fail();
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
        }

        try {
            Longs.insert(a, 5, 10);
            fail();
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
        }

        try {
            Longs.insert(a, 100, 10);
            fail();
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
        }

        try {
            Longs.insert(null, 1, 10);
            fail();
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
        }

        try {
            Longs.insert(null, -1, 10);
            fail();
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
        }
    }

    public void test_remove() {
        long[] a = new long[]{0, 1, 2, 3, 4};

        try {
            Longs.remove(a, -1);
            fail();
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
        }

        try {
            Longs.remove(a, 5);
            fail();
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
        }

        try {
            Longs.remove(a, 100);
            fail();
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
        }

        a = Longs.remove(a, 4);
        assertTrue(a.length == 4);
        assertTrue(a[0] == 0);
        assertTrue(a[1] == 1);
        assertTrue(a[2] == 2);
        assertTrue(a[3] == 3);

        a = Longs.remove(a, 2);
        assertTrue(a.length == 3);
        assertTrue(a[0] == 0);
        assertTrue(a[1] == 1);
        assertTrue(a[2] == 3);

        a = Longs.remove(a, 0);
        assertTrue(a.length == 2);
        assertTrue(a[0] == 1);
        assertTrue(a[1] == 3);

        a = Longs.remove(a, 1);
        assertTrue(a.length == 1);
        assertTrue(a[0] == 1);

        a = Longs.remove(a, 0);
        assertTrue(a.length == 0);

        try {
            Longs.remove(a, 0);
            fail();
        } catch (NegativeArraySizeException negativeArraySizeException) {
        }

        try {
            Longs.remove(a, 1);
            fail();
        } catch (NegativeArraySizeException negativeArraySizeException) {
        }
    }
}
