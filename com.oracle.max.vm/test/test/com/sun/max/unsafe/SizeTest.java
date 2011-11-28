/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package test.com.sun.max.unsafe;

import com.sun.max.program.*;
import com.sun.max.unsafe.*;

public class SizeTest extends WordTestCase {

    public SizeTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SizeTest.class);
    }

    public void test_toString() {
        String s = sizeLow.toString();
        assertEquals(s, "#" + Integer.toString(low));

        s = size0.toString();
        assertEquals(s, "#0");

        s = sizeMax.toString();
        switch (wordWidth()) {
            case BITS_64:
                assertEquals(s, "#18446744073709551615");
                break;
            case BITS_32:
                assertEquals(s, "#4294967295");
                break;
            default:
                throw ProgramError.unknownCase();
        }
    }

    public void test_add_Size() {
        assertTrue(sizeMedium.plus(sizeLow).toInt() == medium + low);
        assertTrue(size0.plus(size0).equals(size0));
        assertTrue(sizeMax.plus(size1).toLong() == 0L);

        final long result = sizeHigh.plus(sizeLow).toLong();
        switch (wordWidth()) {
            case BITS_64:
                assertTrue(result == high + low);
                assertFalse(result == ((int) high + low));
                break;
            case BITS_32:
                assertFalse(result == high + low);
                assertTrue(result == ((int) high + low));
                break;
            default:
                throw ProgramError.unknownCase();
        }
    }

    public void test_add_Offset() {
        assertTrue(size0.plus(offset1).equals(size1));
        assertTrue(size1.plus(offset1.negate()).equals(size0));
        assertTrue(sizeMedium.plus(Offset.fromInt(low)).toInt() == medium + low);
        assertTrue(sizeMedium.plus(Offset.fromInt(-low)).toInt() == medium - low);
        assertTrue(size0.plus(Offset.zero()).equals(size0));

        assertTrue(sizeMax.plus(offset1).toLong() == 0L);
        assertTrue(size0.plus(offset1.negate()).equals(sizeMax));

        long result = sizeHigh.plus(offsetLow).toLong();
        switch (wordWidth()) {
            case BITS_64:
                assertTrue(result == high + low);
                assertFalse(result == ((int) high + low));
                break;
            case BITS_32:
                assertFalse(result == high + low);
                assertTrue(result == ((int) high + low));
                break;
            default:
                throw ProgramError.unknownCase();
        }
        assertTrue(sizeLow.plus(offsetHigh).equals(Address.fromLong(result)));

        result = sizeLow.plus(offsetHigh.negate()).toLong();
        final long difference = low - high;
        final long differenceLowBits = difference & 0xffffffffL;
        switch (wordWidth()) {
            case BITS_64:
                assertTrue(result == low - high);
                assertFalse(result == differenceLowBits);
                break;
            case BITS_32:
                assertFalse(result == low - high);
                assertTrue(result == differenceLowBits);
                break;
            default:
                throw ProgramError.unknownCase();
        }
    }

    public void test_add_int() {
        assertTrue(size0.plus(1).equals(size1));
        assertTrue(size1.plus(-1).equals(size0));
        assertTrue(sizeMedium.plus(low).toInt() == medium + low);
        assertTrue(sizeMedium.plus(-low).toInt() == medium - low);
        assertTrue(size0.plus(0).equals(size0));

        assertTrue(sizeMax.plus(1).toLong() == 0L);
        assertTrue(size0.plus(-1).equals(sizeMax));

        final long result = sizeHigh.plus(low).toLong();
        switch (wordWidth()) {
            case BITS_64:
                assertTrue(result == high + low);
                assertFalse(result == ((int) high + low));
                break;
            case BITS_32:
                assertFalse(result == high + low);
                assertTrue(result == ((int) high + low));
                break;
            default:
                throw ProgramError.unknownCase();
        }
        assertTrue(sizeLow.plus((int) high).equals(Address.fromInt(low + (int) high)));
    }

    public void test_subtract_Size() {
        assertTrue(address1.minus(address1).equals(address0));
        assertTrue(address0.minus(address1).equals(addressMax));
        assertTrue(addressMedium.minus(addressLow).toInt() == medium - low);
    }

    public void test_subtract_Offset() {
        assertTrue(address1.minus(offset1).equals(address0));
        assertTrue(addressMedium.minus(offsetLow).toInt() == medium - low);
        assertTrue(address0.minus(offset1).equals(addressMax));
        switch (wordWidth()) {
            case BITS_64:
                assertTrue(addressLow.minus(offsetMedium).equals(offsetLow.minus(offsetMedium)));
                break;
            case BITS_32:
                final long v = ((long) low - (long) medium) & LOW_32_BITS_MASK;
                assertTrue(addressLow.minus(offsetMedium).toLong() == v);
                break;
            default:
                throw ProgramError.unknownCase();
        }
    }

    public void test_subtract_int() {
        assertTrue(address1.minus(1).equals(address0));
        assertTrue(addressMedium.minus(low).toInt() == medium - low);
        assertTrue(addressMedium.minus(low).equals(offsetLow.negate().plus(offsetMedium)));
        assertTrue(address0.minus(1).equals(addressMax));
    }

    public void test_divide() {
        try {
            sizeLow.dividedBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            sizeLow.dividedBy(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        assertTrue(sizeLow.dividedBy(4).toInt() == low / 4);
        assertTrue(size0.dividedBy(42).toInt() == 0);
    }

    public void test_remainder() {
        try {
            sizeLow.remainder(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            sizeLow.remainder(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 10; i++) {
            assertTrue(Size.fromInt(i).remainder(4) == i % 4);
        }
        assertTrue(size0.remainder(42) == 0);
    }

    public void test_isRoundedBy() {
        try {
            sizeLow.isRoundedBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            sizeLow.isRoundedBy(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 10; i++) {
            assertTrue(Size.fromInt(i).isRoundedBy(4) == (i % 4 == 0));
        }
        assertTrue(size0.isRoundedBy(42));
    }

    private int roundedUpBy(int base, int number) {
        final int rest = number % base;
        if (rest == 0) {
            return number;
        }
        return number + base - rest;
    }

    public void test_roundedUpBy() {
        try {
            sizeLow.roundedUpBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            sizeLow.roundedUpBy(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 20; i++) {
            assertTrue(Size.fromInt(i).roundedUpBy(8).toInt() == roundedUpBy(8, i));
        }
        assertTrue(size0.roundedUpBy(12).equals(size0));
    }

    public void test_roundedDownBy() {
        try {
            sizeLow.roundedDownBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            sizeLow.roundedDownBy(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 20; i++) {
            assertTrue(Size.fromInt(i).roundedDownBy(8).toInt() == (i & ~7));
        }
        assertTrue(size0.roundedDownBy(12).equals(size0));
    }

}
