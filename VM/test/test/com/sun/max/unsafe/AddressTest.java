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
package test.com.sun.max.unsafe;

import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;

public class AddressTest extends WordTestCase {

    public AddressTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AddressTest.class);
    }

    public void test_toString() {
        String s = _addressLow.toString();
        assertEquals(s, "@" + Integer.toHexString(_low));

        s = _address0.toString();
        assertEquals(s, "@0");

        s = _addressMax.toString();
        switch (wordWidth()) {
            case BITS_64:
                assertEquals(s, "@ffffffffffffffff");
                break;
            case BITS_32:
                assertEquals(s, "@ffffffff");
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
    }

    public void test_compareTo() {
        assertTrue(_addressMedium.compareTo(_address0) > 0);
        assertTrue(_addressMedium.compareTo(_addressMedium) == 0);
        assertTrue(_addressMedium.compareTo(_addressHigh) < 0);
        assertTrue(_address0.compareTo(_addressHigh) < 0);
        assertTrue(_addressMax.compareTo(_address0) > 0);
        assertTrue(_addressMax.compareTo(_addressHigh) > 0);
        assertTrue(_addressMax.compareTo(_addressMax) == 0);
        switch (wordWidth()) {
            case BITS_64:
                assertTrue(_addressMax32.compareTo(_addressMax) < 0);
                break;
            case BITS_32:
                assertTrue(_addressMax32.compareTo(_addressMax) == 0);
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
    }

    public void testLessThan() {
        assertTrue(_addressLow.lessThan(Integer.MAX_VALUE));
        assertFalse(_addressMax.lessThan(Integer.MAX_VALUE));
        assertTrue(_address0.lessThan(1));
        assertFalse(_address0.lessThan(0));
        assertFalse(_addressLow.lessThan(_low));
        assertTrue(_addressLow.lessThan(_low + 1));
        assertTrue(_address0.lessThan(-1));
        assertTrue(_address0.lessThan(-_low));
        switch (wordWidth()) {
            case BITS_64:
                assertFalse(_addressHigh.lessThan(Integer.MAX_VALUE));
                assertTrue(_addressMax32.compareTo(_addressHigh) < 0);
                break;
            case BITS_32:
                assertTrue(_addressHigh.lessThan(Integer.MAX_VALUE));
                assertTrue(_addressMax32.compareTo(_addressHigh) > 0);
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
    }

    public void testEquals() {
        assertTrue(_address0.equals(0));
        assertTrue(_address1.equals(1));
        assertTrue(_addressLow.equals(_low));
        switch (wordWidth()) {
            case BITS_64:
                assertFalse(_addressHigh.equals((int) _high));
                break;
            case BITS_32:
                assertTrue(_addressHigh.equals((int) _high));
                break;
            default:
                ProgramError.unknownCase();
                break;
        }

        assertFalse(_addressLow.equals(-1));
        assertFalse(_addressLow.equals(-_low));
        _addressMax.toString();
        assertTrue(_addressMax.equals(Address.fromLong(-1L)));
    }

    public void test_plus_Address() {
        assertTrue(_addressMedium.plus(_addressLow).toInt() == _medium + _low);
        assertTrue(_address0.plus(_address0).equals(_address0));
        assertTrue(_addressMax.plus(_address1).toLong() == 0L);

        final long result = _addressHigh.plus(_addressLow).toLong();
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

    public void test_plus_Offset() {
        assertTrue(_address0.plus(_offset1).equals(_address1));
        assertTrue(_address1.plus(_offset1.negate()).equals(_address0));
        assertTrue(_addressMedium.plus(Offset.fromInt(_low)).toInt() == _medium + _low);
        assertTrue(_addressMedium.plus(Offset.fromInt(-_low)).toInt() == _medium - _low);
        assertTrue(_address0.plus(Offset.zero()).equals(_address0));
        assertTrue(_addressMax.plus(_offset1).toLong() == 0L);
        assertTrue(_address0.plus(_offset1.negate()).equals(_addressMax));

        long result = _addressHigh.plus(_offsetLow).toLong();
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
        assertTrue(_addressLow.plus(_offsetHigh).equals(Address.fromLong(result)));

        result = _addressLow.plus(_offsetHigh.negate()).toLong();
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

    public void test_plus_int() {
        assertTrue(_address0.plus(1).equals(_address1));
        assertTrue(_address1.plus(-1).equals(_address0));
        assertTrue(_addressMedium.plus(_low).toInt() == _medium + _low);
        assertTrue(_addressMedium.plus(-_low).toInt() == _medium - _low);
        assertTrue(_address0.plus(0).equals(_address0));

        assertTrue(_addressMax.plus(1).toLong() == 0L);
        assertTrue(_address0.plus(-1).equals(_addressMax));

        final long result = _addressHigh.plus(_low).toLong();
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
        assertTrue(_addressLow.plus((int) _high).equals(Address.fromInt(_low + (int) _high)));
    }

    public void test_minus_Address() {
        assertTrue(_address1.minus(_address1).equals(_address0));
        assertTrue(_address0.minus(_address1).equals(_addressMax));
        assertTrue(_addressMedium.minus(_addressLow).toInt() == _medium - _low);
    }

    public void test_minus_Offset() {
        assertTrue(_address1.minus(_offset1).equals(_address0));
        assertTrue(_addressMedium.minus(_offsetLow).toInt() == _medium - _low);
        assertTrue(_address0.minus(_offset1).equals(_addressMax));
        switch (wordWidth()) {
            case BITS_64: {
                assertTrue(_addressLow.minus(_offsetMedium).equals(_offsetLow.minus(_offsetMedium)));
                break;
            }
            case BITS_32: {
                final long v = ((long) _low - (long) _medium) & LOW_32_BITS_MASK;
                assertTrue(_addressLow.minus(_offsetMedium).toLong() == v);
                break;
            }
            default: {
                ProgramError.unknownCase();
                break;
            }
        }
    }

    public void test_minus_int() {
        assertTrue(_address1.minus(1).equals(_address0));
        assertTrue(_addressMedium.minus(_low).toInt() == _medium - _low);
        assertTrue(_addressMedium.minus(_low).equals(_offsetLow.negate().plus(_offsetMedium)));
        assertTrue(_address0.minus(1).equals(_addressMax));
    }

    public void test_dividedBy() {
        try {
            _addressLow.dividedBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            _addressLow.dividedBy(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        assertTrue(_addressLow.dividedBy(4).toInt() == _low / 4);
        assertTrue(_address0.dividedBy(42).toInt() == 0);
    }

    public void test_remainder() {
        try {
            _addressLow.remainder(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            _addressLow.remainder(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 10; i++) {
            assertTrue(Address.fromInt(i).remainder(4) == i % 4);
        }
        assertTrue(_address0.remainder(42) == 0);
    }

    public void test_isRoundedBy() {
        try {
            _addressLow.isRoundedBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            _addressLow.isRoundedBy(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 10; i++) {
            assertTrue(Address.fromInt(i).isRoundedBy(4) == (i % 4 == 0));
        }
        assertTrue(_address0.isRoundedBy(42));
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
            _addressLow.roundedUpBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            _addressLow.roundedUpBy(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 20; i++) {
            assertTrue(Address.fromInt(i).roundedUpBy(8).toInt() == roundedUpBy(8, i));
        }
        assertTrue(_address0.roundedUpBy(12).equals(_address0));
    }

    public void test_roundedDownBy() {
        try {
            _addressLow.roundedDownBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            _addressLow.roundedDownBy(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 20; i++) {
            assertTrue(Address.fromInt(i).roundedDownBy(8).toInt() == (i & ~7));
        }
        assertTrue(_address0.roundedDownBy(12).equals(_address0));
    }

    public void test_align() {
        final int n = Platform.hostOrTarget().processorKind().dataModel().alignment().numberOfBytes();
        assertTrue(Address.zero().aligned().toInt() == 0);
        assertTrue(Address.fromInt(1).aligned().toInt() == n);
        assertTrue(Address.fromInt(n).aligned().toInt() == n);
        assertTrue(Address.fromInt(n - 1).aligned().toInt() == n);
        assertTrue(Address.fromInt(n / 2).aligned().toInt() == n);
        assertTrue(Address.fromInt(n + 1).aligned().toInt() == n + n);
        assertTrue(Address.fromInt(n + (n / 2)).aligned().toInt() == n + n);
        assertTrue(Address.fromInt(n + n).aligned().toInt() == n + n);
        assertTrue(Address.fromInt(n + n - 1).aligned().toInt() == n + n);
        assertTrue(Address.fromInt(2003 * n).aligned().toInt() == 2003 * n);
        assertTrue(Address.fromInt(2003 * n - 1).aligned().toInt() == 2003 * n);
        assertTrue(Address.fromInt(2003 * n + 1).aligned().toInt() == 2003 * n + n);

    }

    public void test_aligned() {
        final int n = Platform.hostOrTarget().processorKind().dataModel().alignment().numberOfBytes();
        assertTrue(Address.zero().isAligned());
        assertFalse(Address.fromInt(1).isAligned());
        assertFalse(Address.fromInt(n - (n / 2)).isAligned());
        assertFalse(Address.fromInt(n - 1).isAligned());
        assertTrue(Address.fromInt(n).isAligned());
        assertFalse(Address.fromInt(n + 1).isAligned());
        assertFalse(Address.fromInt(n + (n / 2)).isAligned());
        assertFalse(Address.fromInt(n + n - 1).isAligned());
        assertTrue(Address.fromInt(n + n).isAligned());
        assertFalse(Address.fromInt(n + n + 1).isAligned());
        assertFalse(Address.fromInt(2003 * n - 1).isAligned());
        assertTrue(Address.fromInt(2003 * n).isAligned());
        assertFalse(Address.fromInt(2003 * n + 1).isAligned());
    }

    public void test_times_Address() {
        assertTrue(_address0.times(_addressHigh).equals(_address0));
        assertTrue(_address0.times(_addressLow).toInt() == 0);
        assertTrue(_address1.times(_address1).toInt() == 1);
        assertTrue(_addressTiny.times(_addressTiny).toLong() == (long) _tiny * (long) _tiny);
        assertTrue(_address1.times(_addressHigh).equals(_addressHigh));
        assertTrue(_address1.times(_addressLow).toInt() == _addressLow.toInt());
        switch (wordWidth()) {
            case BITS_64:
                assertTrue(_addressLow.times(_addressLow).toLong() == (long) _low * (long) _low);
                assertTrue(_addressMax.times(_addressMax).equals(_address1));
                _addressHigh.toLong();
                _addressHigh.times(_addressHigh).toLong();

                _addressMax.toLong();
                _addressMax.dividedBy(2).toLong();

                assertTrue(_addressHigh.times(_addressHigh).lessThan(_addressMax.dividedBy(2)));
                assertTrue(_addressHigh.times(_addressHigh).greaterThan(_addressHigh));
                break;
            case BITS_32:
                assertTrue(_addressLow.times(_addressLow).toLong() < (long) _low * (long) _low);
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
    }

}
