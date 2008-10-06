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
/*VCSID=c5ae3dc5-b2ec-4c80-86b8-0ee248848d05*/
package test.com.sun.max.lang;

import com.sun.max.ide.*;
import com.sun.max.lang.*;

/**
 * Tests for {@link Integers}.
 *
 * @author Hiroshi Yamauchi
 */
public class IntegersTest extends MaxTestCase {

    public IntegersTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(IntegersTest.class);
    }

    public void test_roundUp() {
        assertTrue(Ints.roundUp(11, 4) == 12);
        assertTrue(Ints.roundUp(0, 4) == 0);
        assertTrue(Ints.roundUp(3, 1) == 3);
        assertTrue(Ints.roundUp(-1, 3) == 0);
        try {
            assertTrue(Ints.roundUp(1, 0) == 0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
    }

    public void test_contains() {
        final int[] array = new int[10000];
        for (int i = 0; i < array.length; i++) {
            array[i] = i;
        }
        assertTrue(!Ints.contains(array, -1));
        assertTrue(Ints.contains(array, 0));
        assertTrue(Ints.contains(array, 9999));
        assertTrue(!Ints.contains(array, 10000));
        assertTrue(!Ints.contains(array, Integer.MAX_VALUE));
    }

    public void test_append() {
        int[] array = new int[0];
        for (int i = 0; i < array.length; i++) {
            array = Ints.append(array, i);
        }
        for (int i = 0; i < array.length; i++) {
            assertTrue(array[i] == i);
        }
    }

    public void test_createRange() {
        final int[] array = Ints.createRange(0, 1000);
        assertTrue(array.length == 1001);
        assertTrue(array[0] == 0);
        assertTrue(array[array.length - 1] == 1000);
        for (int i = 0; i < array.length; i++) {
            assertTrue(array[i] == i);
        }

        final int[] array2 = Ints.createRange(-1000, 1000);
        assertTrue(array2.length == 2001);
        assertTrue(array2[0] == -1000);
        assertTrue(array2[array2.length - 1] == 1000);
        for (int i = 0; i < array2.length; i++) {
            assertTrue(array2[i] == i - 1000);
        }

        final int[] array3 = Ints.createRange(100, 1000);
        assertTrue(array3.length == 901);
        assertTrue(array3[0] == 100);
        assertTrue(array3[array3.length - 1] == 1000);
        for (int i = 0; i < array3.length; i++) {
            assertTrue(array3[i] == i + 100);
        }

        final int[] array4 = Ints.createRange(-1000, -100);
        assertTrue(array4.length == 901);
        assertTrue(array4[0] == -1000);
        assertTrue(array4[array4.length - 1] == -100);
        for (int i = 0; i < array4.length; i++) {
            assertTrue(array4[i] == i - 1000);
        }

        try {
            Ints.createRange(100, 0);
            assertTrue(false);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        final int[] array6 = Ints.createRange(1, 1);
        assertTrue(array6.length == 1 && array6[0] == 1);
    }
}
