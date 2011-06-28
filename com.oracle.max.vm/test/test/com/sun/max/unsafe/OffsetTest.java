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

public class OffsetTest extends WordTestCase {

    public OffsetTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(OffsetTest.class);
    }

    public void test_toString() {
        String s = offsetLow.toString();
        assertEquals(s, "&" + Integer.toHexString(low));

        s = offset0.toString();
        assertEquals(s, "&0");

        s = offsetMinus1.toString();
        switch (wordWidth()) {
            case BITS_64:
                assertEquals(s, "&ffffffffffffffff");
                break;
            case BITS_32:
                assertEquals(s, "&ffffffff");
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
    }

    public void testCompareTo() {
        assertTrue(offsetMedium.compareTo(offset0) > 0);
        assertTrue(offsetMedium.compareTo(offsetMedium) == 0);
        assertTrue(offsetMedium.compareTo(offsetHigh) < 0);
        assertTrue(offset0.compareTo(offsetHigh) < 0);
        assertTrue(offsetMax.compareTo(offset0) > 0);
        assertTrue(offsetMax.compareTo(offsetHigh) > 0);
        assertTrue(offsetMax.compareTo(offsetMax) == 0);
        assertTrue(offsetMin.compareTo(offsetMax) < 0);
        assertTrue(offsetMin.compareTo(offsetMinus1) < 0);
        assertTrue(offsetMin.compareTo(offsetMinus1) < 0);
        assertTrue(offsetMinus1.compareTo(offsetMin) > 0);
        assertTrue(offsetMinus1.compareTo(offset0) < 0);
    }

    public void test_negate() {
        assertTrue(offset0.equals(offset0.negate().negate()));
        assertTrue(offset1.equals(offset1.negate().negate()));
        assertTrue(offset1.plus(offset1.negate()).equals(offset0));
        assertTrue(offsetMedium.equals(offsetMedium.negate().negate()));
        assertTrue(offsetMax.equals(offsetMax.negate().negate()));
        assertTrue(offsetMin.equals(offsetMin.negate().negate()));
    }

    public void test_add_Offset() {
        assertTrue(offset0.plus(Offset.zero()).equals(offset0));
        assertTrue(offsetMinus1.plus(offset1).equals(offset0));
        assertTrue(offset0.plus(offsetMinus1).equals(offsetMinus1));
        assertTrue(offset0.plus(offset1).equals(offset1));
        assertTrue(offsetMedium.plus(Offset.fromInt(low)).toInt() == medium + low);
        assertTrue(offsetMedium.plus(Offset.fromInt(-low)).toInt() == medium - low);

        switch (wordWidth()) {
            case BITS_64: {
                long result = offsetHigh.plus(offsetMedium).toLong();
                assertTrue(result == high + medium);
                assertFalse(result == ((int) high + medium));
                assertTrue(offsetMedium.plus(offsetHigh).equals(Offset.fromLong(result)));

                result = offsetMedium.plus(offsetHigh.negate()).toLong();
                assertTrue(result == medium - high);
                assertFalse(result == (medium - (int) high));
                break;
            }
            case BITS_32: {
                long result = offsetMedium.plus(offsetHigh.negate()).toLong();
                assertFalse(result == medium - high);
                assertTrue(result == (medium - (int) high));
                result = offsetHigh.plus(offsetMedium).toLong();
                assertFalse(result == high + medium);
                assertTrue(result == ((int) high + medium));
                assertTrue(offsetMedium.plus(offsetHigh).equals(Offset.fromLong(result)));
                break;
            }
            default: {
                ProgramError.unknownCase();
                break;
            }
        }
    }

    public void test_add_Size() {
        assertTrue(offsetMedium.plus(sizeLow).toInt() == medium + low);
        assertTrue(offset0.plus(size0).equals(offset0));

        switch (wordWidth()) {
            case BITS_64: {
                final long result = offsetHigh.plus(sizeMedium).toLong();
                assertTrue(result == high + medium);
                assertFalse(result == ((int) high + medium));
                break;
            }
            case BITS_32: {
                final long result = offsetHigh.plus(sizeMedium).toLong();
                assertFalse(result == high + medium);
                assertTrue(result == ((int) high + medium));
                break;
            }
            default: {
                ProgramError.unknownCase();
                break;
            }
        }
    }

    public void test_add_int() {
        assertTrue(offset0.plus(1).equals(offset1));
        assertTrue(offset1.plus(-1).equals(offset0));
        assertTrue(offsetMedium.plus(low).toInt() == medium + low);
        assertTrue(offsetMedium.plus(-low).toInt() == medium - low);
        assertTrue(offset0.plus(-1).equals(offsetMinus1));
        assertTrue(offset0.plus(0).equals(offset0));

        switch (wordWidth()) {
            case BITS_64: {
                final long result = offsetHigh.plus(medium).toLong();
                assertTrue(result == high + medium);
                assertFalse(result == ((int) high + medium));
                assertFalse(offsetMedium.plus((int) high).equals(Offset.fromInt(medium + (int) high)));
                break;
            }
            case BITS_32: {
                final long result = offsetHigh.plus(medium).toLong();
                assertFalse(result == high + medium);
                assertTrue(result == ((int) high + medium));
                assertTrue(offsetMedium.plus((int) high).equals(Offset.fromInt(medium + (int) high)));
                break;
            }
            default: {
                ProgramError.unknownCase();
                break;
            }
        }
    }

    public void test_subtract_Offset() {
        assertTrue(offset1.minus(offset1).equals(offset0));
        assertTrue(offset0.minus(offset1).equals(offsetMinus1));
        assertTrue(offsetMedium.minus(offsetLow).toInt() == medium - low);
        assertTrue(offsetMedium.minus(offsetHigh).equals(offsetHigh.negate().plus(offsetMedium)));
    }

    public void test_subtract_Size() {
        assertTrue(offset1.minus(size1).equals(offset0));
        assertTrue(offset0.minus(size1).equals(offsetMinus1));
        assertTrue(offsetMedium.minus(offsetLow).toInt() == medium - low);
    }

    public void test_subtract_int() {
        assertTrue(offset1.minus(1).equals(offset0));
        assertTrue(offset0.minus(1).equals(offsetMinus1));
        assertTrue(offsetMedium.minus(low).toInt() == medium - low);
        assertTrue(offsetHigh.minus(medium).equals(offsetMedium.negate().plus(offsetHigh)));
    }

    public void test_divide() {
        try {
            offsetLow.dividedBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        assertTrue(offset1.dividedBy(-1).equals(offsetMinus1));
        assertTrue(offsetLow.dividedBy(-8).equals(Offset.fromInt(low / -8)));
        assertTrue(offsetMedium.negate().dividedBy(low).toInt() == (-medium / low));
        assertTrue(offsetLow.dividedBy(4).toInt() == low / 4);
        assertTrue(offset0.dividedBy(42).toInt() == 0);
    }

    public void test_remainder() {
        try {
            offsetLow.remainder(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 10; i++) {
            assertTrue(Offset.fromInt(i).remainder(4) == i % 4);
        }
        assertTrue(offset0.remainder(42) == 0);

        // TODO: implement negative cases
    }

    public void test_isRoundedBy() {
        try {
            offsetLow.isRoundedBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 10; i++) {
            assertTrue(Offset.fromInt(i).isRoundedBy(4) == (i % 4 == 0));
        }
        assertTrue(offset0.isRoundedBy(42));

        // TODO: implement negative cases
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
            offsetLow.roundedUpBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 20; i++) {
            assertTrue(Offset.fromInt(i).roundedUpBy(8).toInt() == roundedUpBy(8, i));
        }
        assertTrue(offset0.roundedUpBy(12).equals(offset0));

        // TODO: implement negative cases
    }

    public void test_roundedDownBy() {
        try {
            offsetLow.roundedDownBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 20; i++) {
            assertTrue(Offset.fromInt(i).roundedDownBy(8).toInt() == (i & ~7));
        }
        assertTrue(offset0.roundedDownBy(12).equals(offset0));

        // TODO: implement negative cases
    }

    public void test_align() {
        final int n = Word.size();
        assertTrue(Offset.zero().aligned().toInt() == 0);
        assertTrue(Offset.fromInt(1).aligned().toInt() == n);
        assertTrue(Offset.fromInt(n).aligned().toInt() == n);
        assertTrue(Offset.fromInt(n - 1).aligned().toInt() == n);
        assertTrue(Offset.fromInt(n / 2).aligned().toInt() == n);
        assertTrue(Offset.fromInt(n + 1).aligned().toInt() == n + n);
        assertTrue(Offset.fromInt(n + (n / 2)).aligned().toInt() == n + n);
        assertTrue(Offset.fromInt(n + n).aligned().toInt() == n + n);
        assertTrue(Offset.fromInt(n + n - 1).aligned().toInt() == n + n);
        assertTrue(Offset.fromInt(2003 * n).aligned().toInt() == 2003 * n);
        assertTrue(Offset.fromInt(2003 * n - 1).aligned().toInt() == 2003 * n);
        assertTrue(Offset.fromInt(2003 * n + 1).aligned().toInt() == 2003 * n + n);
    }

    public void test_aligned() {
        final int n = Word.size();
        assertTrue(Offset.zero().isAligned());
        assertFalse(Offset.fromInt(1).isAligned());
        assertFalse(Offset.fromInt(n - (n / 2)).isAligned());
        assertFalse(Offset.fromInt(n - 1).isAligned());
        assertTrue(Offset.fromInt(n).isAligned());
        assertFalse(Offset.fromInt(n + 1).isAligned());
        assertFalse(Offset.fromInt(n + (n / 2)).isAligned());
        assertFalse(Offset.fromInt(n + n - 1).isAligned());
        assertTrue(Offset.fromInt(n + n).isAligned());
        assertFalse(Offset.fromInt(n + n + 1).isAligned());
        assertFalse(Offset.fromInt(2003 * n - 1).isAligned());
        assertTrue(Offset.fromInt(2003 * n).isAligned());
        assertFalse(Offset.fromInt(2003 * n + 1).isAligned());
    }

}
