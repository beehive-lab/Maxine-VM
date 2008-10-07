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
/*VCSID=6d58b1f2-0190-4290-b932-01ad25b65692*/
package test.com.sun.max.lang;

import com.sun.max.ide.*;
import com.sun.max.lang.*;

/**
 * Tests for com.sun.max.util.Bytes.
 *
 * @author Hiroshi Yamauchi
 */
public class BytesTest extends MaxTestCase {

    public BytesTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(BytesTest.class);
    }

    public static byte[] makeByteArray(int length) {
        final byte[] bytes = new byte[length];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (i % 127);
        }
        return bytes;
    }

    public static final int TEST_LENGTH = 98;

    public void test_numberOfTrailingZeros() {
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; ++i) {
            final byte b = (byte) i;
            final int expected = b == 0 ? 8 : Integer.numberOfTrailingZeros(b);
            final int actual = Bytes.numberOfTrailingZeros(b);
            assertEquals(expected, actual);
        }
    }

    public void test_equals() {
        final byte[] bytes1 = makeByteArray(TEST_LENGTH);
        final byte[] bytes2 = makeByteArray(TEST_LENGTH);
        assertTrue(Bytes.equals(bytes1, bytes2));
        assertTrue(Bytes.equals(bytes1, bytes2, 0));
        assertTrue(Bytes.equals(bytes1, bytes2, 59));
        assertTrue(Bytes.equals(bytes1, bytes2, TEST_LENGTH));
        assertTrue(Bytes.equals(bytes1, 0, bytes2));
        bytes2[2] = 99;
        assertTrue(Bytes.equals(bytes1, bytes2, 0));
        assertTrue(Bytes.equals(bytes1, bytes2, 1));
        assertTrue(Bytes.equals(bytes1, bytes2, 2));
        assertFalse(Bytes.equals(bytes1, bytes2, 3));
        assertFalse(Bytes.equals(bytes1, bytes2, 4));
        final byte[] bytes3 = new byte[TEST_LENGTH - 10];
        for (int i = 0; i < bytes3.length; i++) {
            bytes3[i] = (byte) (i + 10);
        }
        assertFalse(Bytes.equals(bytes1, bytes3));
        assertFalse(Bytes.equals(bytes1, 0, bytes3));
        assertFalse(Bytes.equals(bytes1, 9, bytes3));
        assertTrue(Bytes.equals(bytes1, 10, bytes3));
        assertFalse(Bytes.equals(bytes1, 11, bytes3));
    }

    public void test_copy() {
        final byte[] bytes1 = makeByteArray(TEST_LENGTH);
        final byte[] bytes2 = new byte[TEST_LENGTH];
        final byte[] bytes3 = new byte[TEST_LENGTH];
        final byte[] bytes4 = new byte[TEST_LENGTH];
        Bytes.copy(bytes1, 0, bytes2, 0, TEST_LENGTH);
        for (int i = 0; i < TEST_LENGTH; i++) {
            assertTrue(bytes1[i] == bytes2[i]);
        }
        Bytes.copy(bytes1, TEST_LENGTH / 2, bytes3, TEST_LENGTH / 2, TEST_LENGTH / 4);
        for (int i = TEST_LENGTH / 2; i < TEST_LENGTH / 4; i++) {
            assertTrue(bytes1[i] == bytes3[i]);
        }
        Bytes.copy(bytes1, bytes4, TEST_LENGTH);
        for (int i = 0; i < TEST_LENGTH; i++) {
            assertTrue(bytes1[i] == bytes4[i]);
        }
    }

    public void test_copyAll() {
        final byte[] bytes1 = makeByteArray(TEST_LENGTH);
        final byte[] bytes2 = new byte[TEST_LENGTH];
        final byte[] bytes3 = new byte[TEST_LENGTH + 100];
        Bytes.copyAll(bytes1, bytes2);
        for (int i = 0; i < TEST_LENGTH; i++) {
            assertTrue(bytes1[i] == bytes2[i]);
        }
        Bytes.copyAll(bytes1, bytes3, 100);
        for (int i = 0; i < TEST_LENGTH; i++) {
            assertTrue(bytes1[i] == bytes3[i + 100]);
        }
    }

    public void test_getSection() {
        final byte[] bytes1 = makeByteArray(TEST_LENGTH);
        final byte[] bytes2 = Bytes.getSection(bytes1, 0, TEST_LENGTH);
        for (int i = 0; i < TEST_LENGTH; i++) {
            assertTrue(bytes1[i] == bytes2[i]);
        }
        final byte[] bytes3 = Bytes.getSection(bytes1, TEST_LENGTH / 8, 2 * TEST_LENGTH / 3);
        for (int i = 0; i < (13 * TEST_LENGTH) / 24; i++) {
            assertTrue(bytes1[i + TEST_LENGTH / 8] == bytes3[i]);
        }
    }

    public void test_toHexLiteral() {
        assertEquals(Bytes.toHexLiteral((byte) 0), "0x00");
        assertEquals(Bytes.toHexLiteral((byte) 15), "0x0F");
        assertEquals(Bytes.toHexLiteral(Byte.MAX_VALUE), "0x7F");
        assertEquals(Bytes.toHexLiteral(Byte.MIN_VALUE), "0x80");
        assertEquals(Bytes.toHexLiteral(makeByteArray(3)), "0x000102");
    }

}
