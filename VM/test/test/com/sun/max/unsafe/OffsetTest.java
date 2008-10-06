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
/*VCSID=13f5e00e-e7db-4f45-b300-4fbf78933904*/
package test.com.sun.max.unsafe;

import com.sun.max.platform.*;
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
        String s = _offsetLow.toString();
        assertEquals(s, "&" + Integer.toHexString(_low));

        s = _offset0.toString();
        assertEquals(s, "&0");

        s = _offsetMinus1.toString();
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
        assertTrue(_offsetMedium.compareTo(_offset0) > 0);
        assertTrue(_offsetMedium.compareTo(_offsetMedium) == 0);
        assertTrue(_offsetMedium.compareTo(_offsetHigh) < 0);
        assertTrue(_offset0.compareTo(_offsetHigh) < 0);
        assertTrue(_offsetMax.compareTo(_offset0) > 0);
        assertTrue(_offsetMax.compareTo(_offsetHigh) > 0);
        assertTrue(_offsetMax.compareTo(_offsetMax) == 0);
        assertTrue(_offsetMin.compareTo(_offsetMax) < 0);
        assertTrue(_offsetMin.compareTo(_offsetMinus1) < 0);
        assertTrue(_offsetMin.compareTo(_offsetMinus1) < 0);
        assertTrue(_offsetMinus1.compareTo(_offsetMin) > 0);
        assertTrue(_offsetMinus1.compareTo(_offset0) < 0);
    }

    public void test_negate() {
        assertTrue(_offset0.equals(_offset0.negate().negate()));
        assertTrue(_offset1.equals(_offset1.negate().negate()));
        assertTrue(_offset1.plus(_offset1.negate()).equals(_offset0));
        assertTrue(_offsetMedium.equals(_offsetMedium.negate().negate()));
        assertTrue(_offsetMax.equals(_offsetMax.negate().negate()));
        assertTrue(_offsetMin.equals(_offsetMin.negate().negate()));
    }

    public void test_add_Offset() {
        assertTrue(_offset0.plus(Offset.zero()).equals(_offset0));
        assertTrue(_offsetMinus1.plus(_offset1).equals(_offset0));
        assertTrue(_offset0.plus(_offsetMinus1).equals(_offsetMinus1));
        assertTrue(_offset0.plus(_offset1).equals(_offset1));
        assertTrue(_offsetMedium.plus(Offset.fromInt(_low)).toInt() == _medium + _low);
        assertTrue(_offsetMedium.plus(Offset.fromInt(-_low)).toInt() == _medium - _low);

        switch (wordWidth()) {
            case BITS_64: {
                long result = _offsetHigh.plus(_offsetMedium).toLong();
                assertTrue(result == _high + _medium);
                assertFalse(result == ((int) _high + _medium));
                assertTrue(_offsetMedium.plus(_offsetHigh).equals(Offset.fromLong(result)));

                result = _offsetMedium.plus(_offsetHigh.negate()).toLong();
                assertTrue(result == _medium - _high);
                assertFalse(result == (_medium - (int) _high));
                break;
            }
            case BITS_32: {
                long result = _offsetMedium.plus(_offsetHigh.negate()).toLong();
                assertFalse(result == _medium - _high);
                assertTrue(result == (_medium - (int) _high));
                result = _offsetHigh.plus(_offsetMedium).toLong();
                assertFalse(result == _high + _medium);
                assertTrue(result == ((int) _high + _medium));
                assertTrue(_offsetMedium.plus(_offsetHigh).equals(Offset.fromLong(result)));
                break;
            }
            default: {
                ProgramError.unknownCase();
                break;
            }
        }
    }

    public void test_add_Size() {
        assertTrue(_offsetMedium.plus(_sizeLow).toInt() == _medium + _low);
        assertTrue(_offset0.plus(_size0).equals(_offset0));

        switch (wordWidth()) {
            case BITS_64: {
                final long result = _offsetHigh.plus(_sizeMedium).toLong();
                assertTrue(result == _high + _medium);
                assertFalse(result == ((int) _high + _medium));
                break;
            }
            case BITS_32: {
                final long result = _offsetHigh.plus(_sizeMedium).toLong();
                assertFalse(result == _high + _medium);
                assertTrue(result == ((int) _high + _medium));
                break;
            }
            default: {
                ProgramError.unknownCase();
                break;
            }
        }
    }

    public void test_add_int() {
        assertTrue(_offset0.plus(1).equals(_offset1));
        assertTrue(_offset1.plus(-1).equals(_offset0));
        assertTrue(_offsetMedium.plus(_low).toInt() == _medium + _low);
        assertTrue(_offsetMedium.plus(-_low).toInt() == _medium - _low);
        assertTrue(_offset0.plus(-1).equals(_offsetMinus1));
        assertTrue(_offset0.plus(0).equals(_offset0));

        switch (wordWidth()) {
            case BITS_64: {
                final long result = _offsetHigh.plus(_medium).toLong();
                assertTrue(result == _high + _medium);
                assertFalse(result == ((int) _high + _medium));
                assertFalse(_offsetMedium.plus((int) _high).equals(Offset.fromInt(_medium + (int) _high)));
                break;
            }
            case BITS_32: {
                final long result = _offsetHigh.plus(_medium).toLong();
                assertFalse(result == _high + _medium);
                assertTrue(result == ((int) _high + _medium));
                assertTrue(_offsetMedium.plus((int) _high).equals(Offset.fromInt(_medium + (int) _high)));
                break;
            }
            default: {
                ProgramError.unknownCase();
                break;
            }
        }
    }

    public void test_subtract_Offset() {
        assertTrue(_offset1.minus(_offset1).equals(_offset0));
        assertTrue(_offset0.minus(_offset1).equals(_offsetMinus1));
        assertTrue(_offsetMedium.minus(_offsetLow).toInt() == _medium - _low);
        assertTrue(_offsetMedium.minus(_offsetHigh).equals(_offsetHigh.negate().plus(_offsetMedium)));
    }

    public void test_subtract_Size() {
        assertTrue(_offset1.minus(_size1).equals(_offset0));
        assertTrue(_offset0.minus(_size1).equals(_offsetMinus1));
        assertTrue(_offsetMedium.minus(_offsetLow).toInt() == _medium - _low);
    }

    public void test_subtract_int() {
        assertTrue(_offset1.minus(1).equals(_offset0));
        assertTrue(_offset0.minus(1).equals(_offsetMinus1));
        assertTrue(_offsetMedium.minus(_low).toInt() == _medium - _low);
        assertTrue(_offsetHigh.minus(_medium).equals(_offsetMedium.negate().plus(_offsetHigh)));
    }

    public void test_divide() {
        try {
            _offsetLow.dividedBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        assertTrue(_offset1.dividedBy(-1).equals(_offsetMinus1));
        assertTrue(_offsetLow.dividedBy(-8).equals(Offset.fromInt(_low / -8)));
        assertTrue(_offsetMedium.negate().dividedBy(_low).toInt() == (-_medium / _low));
        assertTrue(_offsetLow.dividedBy(4).toInt() == _low / 4);
        assertTrue(_offset0.dividedBy(42).toInt() == 0);
    }

    public void test_remainder() {
        try {
            _offsetLow.remainder(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 10; i++) {
            assertTrue(Offset.fromInt(i).remainder(4) == i % 4);
        }
        assertTrue(_offset0.remainder(42) == 0);

        Problem.todo("implement negative cases");
    }

    public void test_isRoundedBy() {
        try {
            _offsetLow.isRoundedBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 10; i++) {
            assertTrue(Offset.fromInt(i).isRoundedBy(4) == (i % 4 == 0));
        }
        assertTrue(_offset0.isRoundedBy(42));

        Problem.todo("implement negative cases");
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
            _offsetLow.roundedUpBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 20; i++) {
            assertTrue(Offset.fromInt(i).roundedUpBy(8).toInt() == roundedUpBy(8, i));
        }
        assertTrue(_offset0.roundedUpBy(12).equals(_offset0));

        Problem.todo("implement negative cases");
    }

    public void test_roundedDownBy() {
        try {
            _offsetLow.roundedDownBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 20; i++) {
            assertTrue(Offset.fromInt(i).roundedDownBy(8).toInt() == (i & ~7));
        }
        assertTrue(_offset0.roundedDownBy(12).equals(_offset0));

        Problem.todo("implement negative cases");
    }

    public void test_align() {
        final int n = Platform.hostOrTarget().processorKind().dataModel().alignment().numberOfBytes();
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
        final int n = Platform.hostOrTarget().processorKind().dataModel().alignment().numberOfBytes();
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
