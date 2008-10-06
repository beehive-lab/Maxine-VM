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
/*VCSID=48acc220-856e-4e88-b42a-57e6c3f0f6c4*/
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
        String s = _sizeLow.toString();
        assertEquals(s, "#" + Integer.toString(_low));

        s = _size0.toString();
        assertEquals(s, "#0");

        s = _sizeMax.toString();
        switch (wordWidth()) {
            case BITS_64:
                assertEquals(s, "#42949672954294967295");
                break;
            case BITS_32:
                assertEquals(s, "#4294967295");
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
    }

    public void test_add_Size() {
        assertTrue(_sizeMedium.plus(_sizeLow).toInt() == _medium + _low);
        assertTrue(_size0.plus(_size0).equals(_size0));
        assertTrue(_sizeMax.plus(_size1).toLong() == 0L);

        final long result = _sizeHigh.plus(_sizeLow).toLong();
        switch (wordWidth()) {
            case BITS_64:
                assertTrue(result == _high + _low);
                assertFalse(result == ((int) _high + _low));
                break;
            case BITS_32:
                assertFalse(result == _high + _low);
                assertTrue(result == ((int) _high + _low));
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
    }

    public void test_add_Offset() {
        assertTrue(_size0.plus(_offset1).equals(_size1));
        assertTrue(_size1.plus(_offset1.negate()).equals(_size0));
        assertTrue(_sizeMedium.plus(Offset.fromInt(_low)).toInt() == _medium + _low);
        assertTrue(_sizeMedium.plus(Offset.fromInt(-_low)).toInt() == _medium - _low);
        assertTrue(_size0.plus(Offset.zero()).equals(_size0));

        assertTrue(_sizeMax.plus(_offset1).toLong() == 0L);
        assertTrue(_size0.plus(_offset1.negate()).equals(_sizeMax));

        long result = _sizeHigh.plus(_offsetLow).toLong();
        switch (wordWidth()) {
            case BITS_64:
                assertTrue(result == _high + _low);
                assertFalse(result == ((int) _high + _low));
                break;
            case BITS_32:
                assertFalse(result == _high + _low);
                assertTrue(result == ((int) _high + _low));
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
        assertTrue(_sizeLow.plus(_offsetHigh).equals(Address.fromLong(result)));

        result = _sizeLow.plus(_offsetHigh.negate()).toLong();
        final long difference = _low - _high;
        final long differenceLowBits = difference & 0xffffffffL;
        switch (wordWidth()) {
            case BITS_64:
                assertTrue(result == _low - _high);
                assertFalse(result == differenceLowBits);
                break;
            case BITS_32:
                assertFalse(result == _low - _high);
                assertTrue(result == differenceLowBits);
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
    }

    public void test_add_int() {
        assertTrue(_size0.plus(1).equals(_size1));
        assertTrue(_size1.plus(-1).equals(_size0));
        assertTrue(_sizeMedium.plus(_low).toInt() == _medium + _low);
        assertTrue(_sizeMedium.plus(-_low).toInt() == _medium - _low);
        assertTrue(_size0.plus(0).equals(_size0));

        assertTrue(_sizeMax.plus(1).toLong() == 0L);
        assertTrue(_size0.plus(-1).equals(_sizeMax));

        final long result = _sizeHigh.plus(_low).toLong();
        switch (wordWidth()) {
            case BITS_64:
                assertTrue(result == _high + _low);
                assertFalse(result == ((int) _high + _low));
                break;
            case BITS_32:
                assertFalse(result == _high + _low);
                assertTrue(result == ((int) _high + _low));
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
        assertTrue(_sizeLow.plus((int) _high).equals(Address.fromInt(_low + (int) _high)));
    }

    public void test_subtract_Size() {
        assertTrue(_address1.minus(_address1).equals(_address0));
        assertTrue(_address0.minus(_address1).equals(_addressMax));
        assertTrue(_addressMedium.minus(_addressLow).toInt() == _medium - _low);
    }

    public void test_subtract_Offset() {
        assertTrue(_address1.minus(_offset1).equals(_address0));
        assertTrue(_addressMedium.minus(_offsetLow).toInt() == _medium - _low);
        assertTrue(_address0.minus(_offset1).equals(_addressMax));
        switch (wordWidth()) {
            case BITS_64:
                assertTrue(_addressLow.minus(_offsetMedium).equals(_offsetLow.minus(_offsetMedium)));
                break;
            case BITS_32:
                final long v = ((long) _low - (long) _medium) & LOW_32_BITS_MASK;
                assertTrue(_addressLow.minus(_offsetMedium).toLong() == v);
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
    }

    public void test_subtract_int() {
        assertTrue(_address1.minus(1).equals(_address0));
        assertTrue(_addressMedium.minus(_low).toInt() == _medium - _low);
        assertTrue(_addressMedium.minus(_low).equals(_offsetLow.negate().plus(_offsetMedium)));
        assertTrue(_address0.minus(1).equals(_addressMax));
    }

    public void test_divide() {
        try {
            _sizeLow.dividedBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            _sizeLow.dividedBy(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        assertTrue(_sizeLow.dividedBy(4).toInt() == _low / 4);
        assertTrue(_size0.dividedBy(42).toInt() == 0);
    }

    public void test_remainder() {
        try {
            _sizeLow.remainder(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            _sizeLow.remainder(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 10; i++) {
            assertTrue(Size.fromInt(i).remainder(4) == i % 4);
        }
        assertTrue(_size0.remainder(42) == 0);
    }

    public void test_isRoundedBy() {
        try {
            _sizeLow.isRoundedBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            _sizeLow.isRoundedBy(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 10; i++) {
            assertTrue(Size.fromInt(i).isRoundedBy(4) == (i % 4 == 0));
        }
        assertTrue(_size0.isRoundedBy(42));
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
            _sizeLow.roundedUpBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            _sizeLow.roundedUpBy(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 20; i++) {
            assertTrue(Size.fromInt(i).roundedUpBy(8).toInt() == roundedUpBy(8, i));
        }
        assertTrue(_size0.roundedUpBy(12).equals(_size0));
    }

    public void test_roundedDownBy() {
        try {
            _sizeLow.roundedDownBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            _sizeLow.roundedDownBy(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 20; i++) {
            assertTrue(Size.fromInt(i).roundedDownBy(8).toInt() == (i & ~7));
        }
        assertTrue(_size0.roundedDownBy(12).equals(_size0));
    }

}
