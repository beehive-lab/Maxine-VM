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
 * Tests for {@link Ints}.
 *
 * @author Bernd Mathiske
 * @author Athul Acharya
 */
public class IntsTest extends MaxTestCase {

    public IntsTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(IntsTest.class);
    }

    static String toBinaryString32(int i) {
        String s = Integer.toBinaryString(i);
        while (s.length() < 32) {
            s = '0' + s;
        }
        return s;
    }

    /**
     * An naive, inefficient but correct implementation of {@link Ints#lowBitsSet(int)}.
     */
    static int lowBitsSet(int highestBitIndex) {
        int result = 0;
        for (int i = highestBitIndex & 0x1f; i >= 0; --i) {
            result |= 1 << i;
        }
        return result;
    }

    /**
     * An naive, inefficient but correct implementation of {@link Ints#highBitsSet(int)}.
     */
    static int highBitsSet(int lowestBitIndex) {
        int result = 0;
        for (int i = lowestBitIndex & 0x1f; i < 32; ++i) {
            result |= 1 << i;
        }
        return result;
    }

    static void assertBitsEquals(int expected, int actual) {
        if (expected != actual) {
            fail("expected: " + toBinaryString32(expected) + " but was: " + toBinaryString32(actual));
        }
    }

    public void test_highBitsSet() {
        for (int lowestBitIndex = -100; lowestBitIndex < 100; ++lowestBitIndex) {
            assertBitsEquals(highBitsSet(lowestBitIndex), Ints.highBitsSet(lowestBitIndex));
        }
        assertBitsEquals(highBitsSet(Integer.MAX_VALUE), Ints.highBitsSet(Integer.MAX_VALUE));
        assertBitsEquals(highBitsSet(Integer.MIN_VALUE), Ints.highBitsSet(Integer.MIN_VALUE));
    }

    public void test_lowBitsSet() {
        for (int highestBitIndex = -100; highestBitIndex < 100; ++highestBitIndex) {
            assertBitsEquals(lowBitsSet(highestBitIndex), Ints.lowBitsSet(highestBitIndex));
        }
        assertBitsEquals(lowBitsSet(Integer.MAX_VALUE), Ints.lowBitsSet(Integer.MAX_VALUE));
        assertBitsEquals(lowBitsSet(Integer.MIN_VALUE), Ints.lowBitsSet(Integer.MIN_VALUE));
    }

    public void test_sizeOfBase10String() {
        assertEquals(Integer.toString(0).length(), Ints.sizeOfBase10String(0));
        assertEquals(Integer.toString(1).length(), Ints.sizeOfBase10String(1));
        assertEquals(Integer.toString(-1).length(), Ints.sizeOfBase10String(-1));
        assertEquals(Integer.toString(Integer.MAX_VALUE).length(), Ints.sizeOfBase10String(Integer.MAX_VALUE));
        assertEquals(Integer.toString(Integer.MIN_VALUE).length(), Ints.sizeOfBase10String(Integer.MIN_VALUE));
        for (int shift = 0; shift < 32; shift++) {
            final int val1 = 1 << shift;
            assertEquals(Integer.toString(val1).length(), Ints.sizeOfBase10String(val1));
            assertEquals(Integer.toString(-val1).length(), Ints.sizeOfBase10String(-val1));
            assertEquals(Integer.toString(val1 + 1).length(), Ints.sizeOfBase10String(val1 + 1));
            assertEquals(Integer.toString(val1 - 1).length(), Ints.sizeOfBase10String(val1 - 1));
        }
    }

    public void test_numberOfEffectiveUnsignedBits() {
        assertTrue(Ints.numberOfEffectiveUnsignedBits(0) == 0);
        assertTrue(Ints.numberOfEffectiveUnsignedBits(1) == 1);
        assertTrue(Ints.numberOfEffectiveUnsignedBits(2) == 2);
        assertTrue(Ints.numberOfEffectiveUnsignedBits(3) == 2);
        assertTrue(Ints.numberOfEffectiveUnsignedBits(4) == 3);
        assertTrue(Ints.numberOfEffectiveUnsignedBits(126) == 7);
        assertTrue(Ints.numberOfEffectiveUnsignedBits(127) == 7);
        assertTrue(Ints.numberOfEffectiveUnsignedBits(128) == 8);
        assertTrue(Ints.numberOfEffectiveUnsignedBits(129) == 8);
        assertTrue(Ints.numberOfEffectiveUnsignedBits(254) == 8);
        assertTrue(Ints.numberOfEffectiveUnsignedBits(255) == 8);
        assertTrue(Ints.numberOfEffectiveUnsignedBits(256) == 9);
        assertTrue(Ints.numberOfEffectiveUnsignedBits(257) == 9);
    }

    public void test_numberOfEffectiveSignedBits() {
        for (int i = 0; i < 257; i++) {
            assertTrue(Ints.numberOfEffectiveSignedBits(i) == Ints.numberOfEffectiveUnsignedBits(i) + 1);
        }
        assertTrue(Ints.numberOfEffectiveSignedBits(0) == 1);
        assertTrue(Ints.numberOfEffectiveSignedBits(-1) == 1);
        assertTrue(Ints.numberOfEffectiveSignedBits(-2) == 2);
        assertTrue(Ints.numberOfEffectiveSignedBits(-3) == 3);
        assertTrue(Ints.numberOfEffectiveSignedBits(-4) == 3);
        assertTrue(Ints.numberOfEffectiveSignedBits(-5) == 4);
        assertTrue(Ints.numberOfEffectiveSignedBits(-6) == 4);
        assertTrue(Ints.numberOfEffectiveSignedBits(-7) == 4);
        assertTrue(Ints.numberOfEffectiveSignedBits(-8) == 4);
        assertTrue(Ints.numberOfEffectiveSignedBits(-9) == 5);
        assertTrue(Ints.numberOfEffectiveSignedBits(-126) == 8);
        assertTrue(Ints.numberOfEffectiveSignedBits(-127) == 8);
        assertTrue(Ints.numberOfEffectiveSignedBits(-128) == 8);
        assertTrue(Ints.numberOfEffectiveSignedBits(-129) == 9);
        assertTrue(Ints.numberOfEffectiveSignedBits(-254) == 9);
        assertTrue(Ints.numberOfEffectiveSignedBits(-255) == 9);
        assertTrue(Ints.numberOfEffectiveSignedBits(-256) == 9);
        assertTrue(Ints.numberOfEffectiveSignedBits(-257) == 10);
    }

    public void test_log2() {
        assertTrue(Ints.log2(1) == 0);
        assertTrue(Ints.log2(2) == 1);
        assertTrue(Ints.log2(3) == 1);
        assertTrue(Ints.log2(4) == 2);
        assertTrue(Ints.log2(8) == 3);
        assertTrue(Ints.log2(1073741824) == 30);
    }

    public void test_hex() {
        assertEquals("0x00001000", Ints.toHexLiteral(16 * 16 * 16));
        assertEquals("0x00000001", Ints.toHexLiteral(1));
        assertEquals("0xFFFFFFFF", Ints.toHexLiteral(-1));
        assertEquals("XXXX1000", Ints.toPaddedHexString(16 * 16 * 16, 'X'));
        assertEquals("FFFFFFFF", Ints.toPaddedHexString(-1, ' '));
    }
}
