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

import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;

public class PointerTest extends WordTestCase {

    public PointerTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PointerTest.class);
    }

    private Pointer _base;
    private long[] _pointerValues;

    @Override
    public void setUp() {
        super.setUp();
        final Size size = Size.fromInt(4 * 8192);
        _base = Memory.mustAllocate(size);
        Memory.clear(_base, size);

        // We will test a bunch of differently aligned pointers:
        final int nPointers = 25;
        _pointerValues = new long[nPointers];

        // Cover all kinds of aligned and unaligned pointers around some
        // well-aligned one:
        _pointerValues[0] = _base.plus(8000).roundedUpBy(8192).minus(-10).toLong();
        for (int i = 1; i < nPointers; i++) {
            _pointerValues[i] = _pointerValues[i - 1] + 1;
        }
    }

    @Override
    public void tearDown() throws Exception {
        Memory.deallocate(_base);
        super.tearDown();
    }

    public void test_toString() {
        String s = _pointerLow.toString();
        assertEquals(s, "^" + Integer.toHexString(_low));

        s = _pointer0.toString();
        assertEquals(s, "^0");

        s = _pointerMax.toString();
        switch (wordWidth()) {
            case BITS_64:
                assertEquals(s, "^ffffffffffffffff");
                break;
            case BITS_32:
                assertEquals(s, "^ffffffff");
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
    }

    public void test_add_Address() {
        assertTrue(_pointerMedium.plus(_addressLow).toInt() == _medium + _low);
        assertTrue(_pointer0.plus(_address0).equals(_pointer0));
        assertTrue(_pointerMax.plus(_address1).toLong() == 0L);

        final long result = _pointerHigh.plus(_addressLow).toLong();
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
        assertTrue(_pointer0.plus(_offset1).equals(_pointer1));
        assertTrue(_pointer1.plus(_offset1.negate()).equals(_pointer0));
        assertTrue(_pointerMedium.plus(Offset.fromInt(_low)).toInt() == _medium + _low);
        assertTrue(_pointerMedium.plus(Offset.fromInt(-_low)).toInt() == _medium - _low);
        assertTrue(_pointer0.plus(Offset.zero()).equals(_pointer0));

        assertTrue(_pointerMax.plus(_offset1).toLong() == 0L);
        assertTrue(_pointer0.plus(_offset1.negate()).equals(_pointerMax));

        long result = _pointerHigh.plus(_offsetLow).toLong();
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
        assertTrue(_pointerLow.plus(_offsetHigh).equals(Address.fromLong(result)));

        result = _pointerLow.plus(_offsetHigh.negate()).toLong();
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
        assertTrue(_pointer0.plus(1).equals(_pointer1));
        assertTrue(_pointer1.plus(-1).equals(_pointer0));
        assertTrue(_pointerMedium.plus(_low).toInt() == _medium + _low);
        assertTrue(_pointerMedium.plus(-_low).toInt() == _medium - _low);
        assertTrue(_pointer0.plus(0).equals(_pointer0));

        assertTrue(_pointerMax.plus(1).toLong() == 0L);
        assertTrue(_pointer0.plus(-1).equals(_pointerMax));

        final long result = _pointerHigh.plus(_low).toLong();
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
        assertTrue(_pointerLow.plus((int) _high).equals(Address.fromInt(_low + (int) _high)));
    }

    public void test_subtract_Address() {
        assertTrue(_pointer1.minus(_address1).equals(_pointer0));
        assertTrue(_pointer0.minus(_address1).equals(_pointerMax));
        assertTrue(_pointerMedium.minus(_addressLow).toInt() == _medium - _low);
    }

    public void test_subtract_Offset() {
        assertTrue(_pointer1.minus(_offset1).equals(_pointer0));
        assertTrue(_pointerMedium.minus(_offsetLow).toInt() == _medium - _low);

        assertTrue(_pointer0.minus(_offset1).equals(_pointerMax));
        switch (wordWidth()) {
            case BITS_64: {
                assertTrue(_pointerLow.minus(_offsetMedium).equals(_offsetLow.minus(_offsetMedium)));
                break;
            }
            case BITS_32: {
                final long v = ((long) _low - (long) _medium) & LOW_32_BITS_MASK;
                assertTrue(_pointerLow.minus(_offsetMedium).toLong() == v);
                break;
            }
            default: {
                ProgramError.unknownCase();
                break;
            }
        }
    }

    public void test_subtract_int() {
        assertTrue(_pointer1.minus(1).equals(_pointer0));
        assertTrue(_pointerMedium.minus(_low).toInt() == _medium - _low);
        assertTrue(_pointerMedium.minus(_low).equals(_offsetLow.negate().plus(_offsetMedium)));
        assertTrue(_pointer0.minus(1).equals(_pointerMax));
    }

    public void test_divide() {
        try {
            _pointerLow.dividedBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            _pointerLow.dividedBy(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        assertTrue(_pointerLow.dividedBy(4).toInt() == _low / 4);
        assertTrue(_pointer0.dividedBy(42).toInt() == 0);
    }

    public void test_remainder() {
        try {
            _pointerLow.remainder(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            _pointerLow.remainder(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 10; i++) {
            assertTrue(Address.fromInt(i).remainder(4) == i % 4);
        }
        assertTrue(_pointer0.remainder(42) == 0);
    }

    public void test_isRoundedBy() {
        try {
            _pointerLow.isRoundedBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            _pointerLow.isRoundedBy(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 10; i++) {
            assertTrue(Address.fromInt(i).isRoundedBy(4) == (i % 4 == 0));
        }
        assertTrue(_pointer0.isRoundedBy(42));
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
            _pointerLow.roundedUpBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            _pointerLow.roundedUpBy(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 20; i++) {
            assertTrue(Address.fromInt(i).roundedUpBy(8).toInt() == roundedUpBy(8, i));
        }
        assertTrue(_pointer0.roundedUpBy(12).equals(_pointer0));
    }

    public void test_roundedDownBy() {
        try {
            _pointerLow.roundedDownBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            _pointerLow.roundedDownBy(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 20; i++) {
            assertTrue(Address.fromInt(i).roundedDownBy(8).toInt() == (i & ~7));
        }
        assertTrue(_pointer0.roundedDownBy(12).equals(_pointer0));
    }

    public void test_writeAndReadByte_Offset() {
        for (long pointerValue : _pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeByte(_offset0, (byte) 55);
            pointer.writeByte(_offset1, (byte) -44);
            pointer.writeByte(_offset2, (byte) 33);
            assertTrue(pointer.readByte(0) == 55);
            assertTrue(pointer.readByte(_offset1) == -44);
            assertTrue(pointer.readByte(_offset2) == 33);
        }
    }

    public void test_writeAndReadByte_int() {
        for (long pointerValue : _pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeByte(0, (byte) -55);
            pointer.writeByte(1, (byte) 44);
            pointer.writeByte(2, (byte) -33);
            pointer.writeByte(-20, (byte) 123);
            assertTrue(pointer.readByte(_offset0) == -55);
            assertTrue(pointer.readByte(1) == 44);
            assertTrue(pointer.readByte(2) == -33);
            assertTrue(pointer.readByte(-20) == 123);
        }
    }

    public void test_writeAndReadShort_Offset() {
        for (long pointerValue : _pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeShort(_offset0, (short) -145);
            pointer.writeShort(_offset2, (short) 83);
            pointer.writeShort(_offset4, (short) -1);
            assertTrue(pointer.readShort(0) == -145);
            assertTrue(pointer.readShort(_offset2) == 83);
            assertTrue(pointer.readShort(_offset4) == -1);
        }
    }

    public void test_writeAndReadShort_int() {
        for (long pointerValue : _pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeShort(0, (short) 0x1278);
            pointer.writeShort(1, (short) 0x3456);
            pointer.writeShort(-9, (short) 576);
            switch (Word.endianness()) {
                case BIG:
                    assertTrue(pointer.readByte(_offset0) == 0x12);
                    assertTrue(pointer.readByte(1) == 0x34);
                    assertTrue(pointer.readShort(0) == 0x1234);
                    assertTrue(pointer.readShort(1) == 0x3456);
                    assertTrue(pointer.readShort(-9) == 576);
                    break;
                case LITTLE:
                    assertTrue(pointer.readByte(_offset0) == 0x78);
                    assertTrue(pointer.readByte(1) == 0x56);
                    assertTrue(pointer.readShort(0) == 0x5678);
                    assertTrue(pointer.readShort(1) == 0x3456);
                    assertTrue(pointer.readShort(-9) == 576);
                    break;
                default:
                    ProgramError.unknownCase();
                    break;
            }
        }
    }

    public void test_writeAndReadInt_Offset() {
        for (long pointerValue : _pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeInt(_offset0, 0x12345678);
            pointer.writeInt(_offset4, 0xabcdef47);
            pointer.writeInt(_offset8, Integer.MIN_VALUE);
            assertTrue(pointer.readInt(0) == 0x12345678);
            assertTrue(pointer.readInt(_offset4) == 0xabcdef47);
            assertTrue(pointer.readInt(_offset8) == Integer.MIN_VALUE);
        }
    }

    public void test_writeAndReadInt_int() {
        for (long pointerValue : _pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeInt(0, 0x12abcdef);
            pointer.writeInt(1, 0x34bbccdd);
            pointer.writeInt(2, 0x56eef712);
            pointer.writeInt(3, 0x78f78365);
            pointer.writeInt(-5, 12345678);
            switch (Word.endianness()) {
                case BIG:
                    assertTrue(pointer.readInt(_offset0) == 0x12345678);
                    assertTrue(pointer.readInt(1) == 0x345678f7);
                    assertTrue(pointer.readInt(2) == 0x5678f783);
                    assertTrue(pointer.readInt(3) == 0x78f78365);
                    assertTrue(pointer.readInt(-5) == 12345678);
                    break;
                case LITTLE:
                    assertTrue(pointer.readInt(_offset0) == 0x6512ddef);
                    assertTrue(pointer.readInt(1) == 0x836512dd);
                    assertTrue(pointer.readInt(2) == 0xf7836512);
                    assertTrue(pointer.readInt(3) == 0x78f78365);
                    assertTrue(pointer.readInt(-5) == 12345678);
                    break;
                default:
                    ProgramError.unknownCase();
                    break;
            }
        }
    }

    public void test_writeAndReadLong_Offset() {
        for (long pointerValue : _pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeLong(_offset0, 0x12345678abcdef18L);
            pointer.writeLong(_offset8, 0x1a2b3c4d5e6f3a4bL);
            pointer.writeLong(_offset16, Long.MAX_VALUE);
            assertTrue(pointer.readLong(0) == 0x12345678abcdef18L);
            assertTrue(pointer.readLong(_offset8) == 0x1a2b3c4d5e6f3a4bL);
            assertTrue(pointer.readLong(_offset16) == Long.MAX_VALUE);
        }
    }

    public void test_writeAndReadLong_int() {
        for (long pointerValue : _pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeLong(0, 0x12f3e4b5a6d7d527L);
            pointer.writeLong(1, 0x34bbccddeeff0011L);
            pointer.writeLong(2, 0x5678334455667788L);
            pointer.writeLong(4, 0xabcdef01aa0f0e0cL);
            pointer.writeLong(-40, 1234567812345678L);
            switch (Word.endianness()) {
                case BIG:
                    assertTrue(pointer.readLong(_offset0) == 0x12345678abcdef01L);
                    assertTrue(pointer.readLong(4) == 0xabcdef01aa0f0e0cL);
                    assertTrue(pointer.readLong(-40) == 1234567812345678L);
                    break;
                case LITTLE:
                    assertTrue(pointer.readLong(_offset0) == 0xaa0f0e0c77881127L);
                    assertTrue(pointer.readLong(4) == 0xabcdef01aa0f0e0cL);
                    assertTrue(pointer.readLong(-40) == 1234567812345678L);
                    break;
                default:
                    ProgramError.unknownCase();
                    break;
            }
        }
    }

    public void test_writeAndReadChar_Offset() {
        for (long pointerValue : _pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeChar(_offset0, (char) 78);
            pointer.writeChar(_offset2, (char) 12583);
            pointer.writeChar(_offset4, (char) -7564);
            pointer.readChar(0);
            assertTrue(pointer.readChar(0) == (char) 78);
            assertTrue(pointer.readChar(_offset2) == (char) 12583);
            assertTrue(pointer.readChar(_offset4) == (char) -7564);
        }
    }

    public void test_writeAndReadChar_int() {
        for (long pointerValue : _pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeChar(0, (char) 22);
            pointer.writeChar(123, (char) 1577);
            pointer.writeChar(-14, (char) 305);
            assertTrue(pointer.readChar(_offset0) == (char) 22);
            assertTrue(pointer.readChar(123) == (char) 1577);
            assertTrue(pointer.readChar(-14) == (char) 305);
        }
    }

    public void test_writeAndReadFloat_Offset() {
        for (long pointerValue : _pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeFloat(_offset0, 21.021f);
            pointer.writeFloat(_offset4, 77123.1233f);
            pointer.writeFloat(_offset8, -234.234e10f);
            assertTrue(pointer.readFloat(0) == 21.021f);
            assertTrue(pointer.readFloat(_offset4) == 77123.1233f);
            assertTrue(pointer.readFloat(_offset8) == -234.234e10f);
        }
    }

    public void test_writeAndReadFloat_int() {
        for (long pointerValue : _pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeFloat(0, 2.3f);
            pointer.writeFloat(71, -7736.4324f);
            pointer.writeFloat(-24, Float.MIN_VALUE);
            assertTrue(pointer.readFloat(_offset0) == 2.3f);
            assertTrue(pointer.readFloat(71) == -7736.4324f);
            assertTrue(pointer.readFloat(-24) == Float.MIN_VALUE);
        }
    }

    public void test_writeAndReadDouble_Offset() {
        for (long pointerValue : _pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeDouble(_offset0, 725448.2342499354);
            pointer.writeDouble(_offset8, 7712323.1231233);
            pointer.writeDouble(16, -2323424.2364456456567e30);
            assertTrue(pointer.readDouble(0) == 725448.2342499354);
            assertTrue(pointer.readDouble(8) == 7712323.1231233);
            assertTrue(pointer.readDouble(_offset16) == -2323424.2364456456567e30);
        }
    }

    public void test_writeAndReadDouble_int() {
        for (long pointerValue : _pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeDouble(0, -324543.3434);
            pointer.writeDouble(71, -1.0);
            pointer.writeDouble(-24, Double.MAX_VALUE);
            assertTrue(pointer.readDouble(_offset0) == -324543.3434);
            assertTrue(pointer.readDouble(71) == -1.0);
            assertTrue(pointer.readDouble(-24) == Double.MAX_VALUE);
        }
    }

    public void test_writeAndReadWord_Offset() {
        for (long pointerValue : _pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeWord(_offset0, _addressLow);
            switch (wordWidth()) {
                case BITS_64:
                    pointer.writeWord(_offset8, _offsetHigh);
                    break;
                case BITS_32:
                    pointer.writeWord(_offset4, _offsetHigh);
                    break;
                default:
                    ProgramError.unknownCase();
                    break;
            }
            switch (wordWidth()) {
                case BITS_64:
                    pointer.writeWord(_offset16, _sizeMax);
                    break;
                case BITS_32:
                    pointer.writeWord(_offset8, _sizeMax);
                    break;
                default:
                    ProgramError.unknownCase();
                    break;
            }
            assertTrue(pointer.readWord(0).asAddress().equals(_addressLow));
            switch (wordWidth()) {
                case BITS_64:
                    assertTrue(pointer.readWord(8).asOffset().equals(_offsetHigh));
                    break;
                case BITS_32:
                    assertTrue(pointer.readWord(4).asOffset().equals(_offsetHigh));
                    break;
                default:
                    ProgramError.unknownCase();
                    break;
            }
            switch (wordWidth()) {
                case BITS_64:
                    assertTrue(pointer.readWord(_offset16).asSize().equals(_sizeMax));
                    break;
                case BITS_32:
                    assertTrue(pointer.readWord(_offset8).asSize().equals(_sizeMax));
                    break;
                default:
                    ProgramError.unknownCase();
                    break;
            }
        }
    }

    public void test_writeAndReadWord_int() {
        for (long pointerValue : _pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeWord(0, _sizeHigh);
            pointer.writeWord(56, pointer);
            pointer.writeWord(-100, _addressMedium);
            assertTrue(pointer.readWord(_offset0).asSize().equals(_sizeHigh));
            assertTrue(pointer.readWord(56).asPointer().equals(pointer));
            assertTrue(pointer.readWord(-100).asSize().equals(_sizeMedium));
        }
    }

    public void performSetAndGetByte(int displacement) {
        for (long pointerValue : _pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.setByte(displacement, 0, (byte) -55);
            pointer.setByte(displacement, 1, (byte) 44);
            pointer.setByte(displacement, 2, (byte) -33);
            pointer.setByte(displacement + 10, -20, (byte) 123);
            assertTrue(pointer.getByte(displacement, 0) == -55);
            assertTrue(pointer.getByte(displacement, 1) == 44);
            assertTrue(pointer.getByte(displacement, 2) == -33);
            assertTrue(pointer.getByte(displacement, -10) == 123);
        }
    }

    public void test_setAndGetByte() {
        for (int displacement = -9; displacement <= 9; displacement++) {
            performSetAndGetByte(displacement);
        }
    }

    public void performSetAndGetWord(int displacement) {
        for (long pointerValue : _pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.setWord(displacement, 0, _sizeHigh);
            pointer.setWord(displacement, 56, pointer);
            pointer.setWord(displacement, -100, _addressMedium);
            assertTrue(pointer.getWord(displacement, 0).asSize().equals(_sizeHigh));
            assertTrue(pointer.getWord(displacement + (6 * Word.size()), 50).asPointer().equals(pointer));
            assertTrue(pointer.getWord(displacement, -100).asSize().equals(_sizeMedium));
        }
    }

    public void test_setAndGetWord() {
        for (int displacement = -16; displacement <= 16; displacement += 8) {
            performSetAndGetWord(displacement);
        }
    }

}
