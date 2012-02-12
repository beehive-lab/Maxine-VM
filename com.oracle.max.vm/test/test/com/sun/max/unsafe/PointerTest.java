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

    private Pointer base;
    private long[] pointerValues;

    @Override
    public void setUp() {
        super.setUp();
        final Size size = Size.fromInt(4 * 8192);
        base = Memory.mustAllocate(size);
        Memory.clearWords(base, size.dividedBy(Word.size()).toInt());

        // We will test a bunch of differently aligned pointers:
        final int nPointers = 25;
        pointerValues = new long[nPointers];

        // Cover all kinds of aligned and unaligned pointers around some
        // well-aligned one:
        pointerValues[0] = base.plus(8000).roundedUpBy(8192).minus(-10).toLong();
        for (int i = 1; i < nPointers; i++) {
            pointerValues[i] = pointerValues[i - 1] + 1;
        }
    }

    @Override
    public void tearDown() throws Exception {
        Memory.deallocate(base);
        super.tearDown();
    }

    public void test_toString() {
        String s = pointerLow.toString();
        assertEquals(s, "^" + Integer.toHexString(low));

        s = pointer0.toString();
        assertEquals(s, "^0");

        s = pointerMax.toString();
        switch (wordWidth()) {
            case BITS_64:
                assertEquals(s, "^ffffffffffffffff");
                break;
            case BITS_32:
                assertEquals(s, "^ffffffff");
                break;
            default:
                throw ProgramError.unknownCase();
        }
    }

    public void test_add_Address() {
        assertTrue(pointerMedium.plus(addressLow).toInt() == medium + low);
        assertTrue(pointer0.plus(address0).equals(pointer0));
        assertTrue(pointerMax.plus(address1).toLong() == 0L);

        final long result = pointerHigh.plus(addressLow).toLong();
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
        assertTrue(pointer0.plus(offset1).equals(pointer1));
        assertTrue(pointer1.plus(offset1.negate()).equals(pointer0));
        assertTrue(pointerMedium.plus(Offset.fromInt(low)).toInt() == medium + low);
        assertTrue(pointerMedium.plus(Offset.fromInt(-low)).toInt() == medium - low);
        assertTrue(pointer0.plus(Offset.zero()).equals(pointer0));

        assertTrue(pointerMax.plus(offset1).toLong() == 0L);
        assertTrue(pointer0.plus(offset1.negate()).equals(pointerMax));

        long result = pointerHigh.plus(offsetLow).toLong();
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
        assertTrue(pointerLow.plus(offsetHigh).equals(Address.fromLong(result)));

        result = pointerLow.plus(offsetHigh.negate()).toLong();
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
        assertTrue(pointer0.plus(1).equals(pointer1));
        assertTrue(pointer1.plus(-1).equals(pointer0));
        assertTrue(pointerMedium.plus(low).toInt() == medium + low);
        assertTrue(pointerMedium.plus(-low).toInt() == medium - low);
        assertTrue(pointer0.plus(0).equals(pointer0));

        assertTrue(pointerMax.plus(1).toLong() == 0L);
        assertTrue(pointer0.plus(-1).equals(pointerMax));

        final long result = pointerHigh.plus(low).toLong();
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
        assertTrue(pointerLow.plus((int) high).equals(Address.fromInt(low + (int) high)));
    }

    public void test_subtract_Address() {
        assertTrue(pointer1.minus(address1).equals(pointer0));
        assertTrue(pointer0.minus(address1).equals(pointerMax));
        assertTrue(pointerMedium.minus(addressLow).toInt() == medium - low);
    }

    public void test_subtract_Offset() {
        assertTrue(pointer1.minus(offset1).equals(pointer0));
        assertTrue(pointerMedium.minus(offsetLow).toInt() == medium - low);

        assertTrue(pointer0.minus(offset1).equals(pointerMax));
        switch (wordWidth()) {
            case BITS_64: {
                assertTrue(pointerLow.minus(offsetMedium).equals(offsetLow.minus(offsetMedium)));
                break;
            }
            case BITS_32: {
                final long v = ((long) low - (long) medium) & LOW_32_BITS_MASK;
                assertTrue(pointerLow.minus(offsetMedium).toLong() == v);
                break;
            }
            default: {
                throw ProgramError.unknownCase();
            }
        }
    }

    public void test_subtract_int() {
        assertTrue(pointer1.minus(1).equals(pointer0));
        assertTrue(pointerMedium.minus(low).toInt() == medium - low);
        assertTrue(pointerMedium.minus(low).equals(offsetLow.negate().plus(offsetMedium)));
        assertTrue(pointer0.minus(1).equals(pointerMax));
    }

    public void test_divide() {
        try {
            pointerLow.dividedBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            pointerLow.dividedBy(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        assertTrue(pointerLow.dividedBy(4).toInt() == low / 4);
        assertTrue(pointer0.dividedBy(42).toInt() == 0);
    }

    public void test_remainder() {
        try {
            pointerLow.remainder(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            pointerLow.remainder(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 10; i++) {
            assertTrue(Address.fromInt(i).remainder(4) == i % 4);
        }
        assertTrue(pointer0.remainder(42) == 0);
    }

    public void test_isRoundedBy() {
        try {
            pointerLow.isRoundedBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            pointerLow.isRoundedBy(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 10; i++) {
            assertTrue(Address.fromInt(i).isRoundedBy(4) == (i % 4 == 0));
        }
        assertTrue(pointer0.isRoundedBy(42));
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
            pointerLow.roundedUpBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            pointerLow.roundedUpBy(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 20; i++) {
            assertTrue(Address.fromInt(i).roundedUpBy(8).toInt() == roundedUpBy(8, i));
        }
        assertTrue(pointer0.roundedUpBy(12).equals(pointer0));
    }

    public void test_roundedDownBy() {
        try {
            pointerLow.roundedDownBy(0);
            fail();
        } catch (ArithmeticException arithmeticException) {
        }
        try {
            pointerLow.roundedDownBy(-1);
        } catch (ArithmeticException arithmeticException) {
        }
        for (int i = 0; i < 20; i++) {
            assertTrue(Address.fromInt(i).roundedDownBy(8).toInt() == (i & ~7));
        }
        assertTrue(pointer0.roundedDownBy(12).equals(pointer0));
    }

    public void test_writeAndReadByte_Offset() {
        for (long pointerValue : pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeByte(offset0, (byte) 55);
            pointer.writeByte(offset1, (byte) -44);
            pointer.writeByte(offset2, (byte) 33);
            assertTrue(pointer.readByte(0) == 55);
            assertTrue(pointer.readByte(offset1) == -44);
            assertTrue(pointer.readByte(offset2) == 33);
        }
    }

    public void test_writeAndReadByte_int() {
        for (long pointerValue : pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeByte(0, (byte) -55);
            pointer.writeByte(1, (byte) 44);
            pointer.writeByte(2, (byte) -33);
            pointer.writeByte(-20, (byte) 123);
            assertTrue(pointer.readByte(offset0) == -55);
            assertTrue(pointer.readByte(1) == 44);
            assertTrue(pointer.readByte(2) == -33);
            assertTrue(pointer.readByte(-20) == 123);
        }
    }

    public void test_writeAndReadShort_Offset() {
        for (long pointerValue : pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeShort(offset0, (short) -145);
            pointer.writeShort(offset2, (short) 83);
            pointer.writeShort(offset4, (short) -1);
            assertTrue(pointer.readShort(0) == -145);
            assertTrue(pointer.readShort(offset2) == 83);
            assertTrue(pointer.readShort(offset4) == -1);
        }
    }

    public void test_writeAndReadShort_int() {
        for (long pointerValue : pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeShort(0, (short) 0x1278);
            pointer.writeShort(1, (short) 0x3456);
            pointer.writeShort(-9, (short) 576);
            switch (Word.endianness()) {
                case BIG:
                    assertTrue(pointer.readByte(offset0) == 0x12);
                    assertTrue(pointer.readByte(1) == 0x34);
                    assertTrue(pointer.readShort(0) == 0x1234);
                    assertTrue(pointer.readShort(1) == 0x3456);
                    assertTrue(pointer.readShort(-9) == 576);
                    break;
                case LITTLE:
                    assertTrue(pointer.readByte(offset0) == 0x78);
                    assertTrue(pointer.readByte(1) == 0x56);
                    assertTrue(pointer.readShort(0) == 0x5678);
                    assertTrue(pointer.readShort(1) == 0x3456);
                    assertTrue(pointer.readShort(-9) == 576);
                    break;
                default:
                    throw ProgramError.unknownCase();
            }
        }
    }

    public void test_writeAndReadInt_Offset() {
        for (long pointerValue : pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeInt(offset0, 0x12345678);
            pointer.writeInt(offset4, 0xabcdef47);
            pointer.writeInt(offset8, Integer.MIN_VALUE);
            assertTrue(pointer.readInt(0) == 0x12345678);
            assertTrue(pointer.readInt(offset4) == 0xabcdef47);
            assertTrue(pointer.readInt(offset8) == Integer.MIN_VALUE);
        }
    }

    public void test_writeAndReadInt_int() {
        for (long pointerValue : pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeInt(0, 0x12abcdef);
            pointer.writeInt(1, 0x34bbccdd);
            pointer.writeInt(2, 0x56eef712);
            pointer.writeInt(3, 0x78f78365);
            pointer.writeInt(-5, 12345678);
            switch (Word.endianness()) {
                case BIG:
                    assertTrue(pointer.readInt(offset0) == 0x12345678);
                    assertTrue(pointer.readInt(1) == 0x345678f7);
                    assertTrue(pointer.readInt(2) == 0x5678f783);
                    assertTrue(pointer.readInt(3) == 0x78f78365);
                    assertTrue(pointer.readInt(-5) == 12345678);
                    break;
                case LITTLE:
                    assertTrue(pointer.readInt(offset0) == 0x6512ddef);
                    assertTrue(pointer.readInt(1) == 0x836512dd);
                    assertTrue(pointer.readInt(2) == 0xf7836512);
                    assertTrue(pointer.readInt(3) == 0x78f78365);
                    assertTrue(pointer.readInt(-5) == 12345678);
                    break;
                default:
                    throw ProgramError.unknownCase();
            }
        }
    }

    public void test_writeAndReadLong_Offset() {
        for (long pointerValue : pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeLong(offset0, 0x12345678abcdef18L);
            pointer.writeLong(offset8, 0x1a2b3c4d5e6f3a4bL);
            pointer.writeLong(offset16, Long.MAX_VALUE);
            assertTrue(pointer.readLong(0) == 0x12345678abcdef18L);
            assertTrue(pointer.readLong(offset8) == 0x1a2b3c4d5e6f3a4bL);
            assertTrue(pointer.readLong(offset16) == Long.MAX_VALUE);
        }
    }

    public void test_writeAndReadLong_int() {
        for (long pointerValue : pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeLong(0, 0x12f3e4b5a6d7d527L);
            pointer.writeLong(1, 0x34bbccddeeff0011L);
            pointer.writeLong(2, 0x5678334455667788L);
            pointer.writeLong(4, 0xabcdef01aa0f0e0cL);
            pointer.writeLong(-40, 1234567812345678L);
            switch (Word.endianness()) {
                case BIG:
                    assertTrue(pointer.readLong(offset0) == 0x12345678abcdef01L);
                    assertTrue(pointer.readLong(4) == 0xabcdef01aa0f0e0cL);
                    assertTrue(pointer.readLong(-40) == 1234567812345678L);
                    break;
                case LITTLE:
                    assertTrue(pointer.readLong(offset0) == 0xaa0f0e0c77881127L);
                    assertTrue(pointer.readLong(4) == 0xabcdef01aa0f0e0cL);
                    assertTrue(pointer.readLong(-40) == 1234567812345678L);
                    break;
                default:
                    throw ProgramError.unknownCase();
            }
        }
    }

    public void test_writeAndReadChar_Offset() {
        for (long pointerValue : pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeChar(offset0, (char) 78);
            pointer.writeChar(offset2, (char) 12583);
            pointer.writeChar(offset4, (char) -7564);
            pointer.readChar(0);
            assertTrue(pointer.readChar(0) == (char) 78);
            assertTrue(pointer.readChar(offset2) == (char) 12583);
            assertTrue(pointer.readChar(offset4) == (char) -7564);
        }
    }

    public void test_writeAndReadChar_int() {
        for (long pointerValue : pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeChar(0, (char) 22);
            pointer.writeChar(123, (char) 1577);
            pointer.writeChar(-14, (char) 305);
            assertTrue(pointer.readChar(offset0) == (char) 22);
            assertTrue(pointer.readChar(123) == (char) 1577);
            assertTrue(pointer.readChar(-14) == (char) 305);
        }
    }

    public void test_writeAndReadFloat_Offset() {
        for (long pointerValue : pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeFloat(offset0, 21.021f);
            pointer.writeFloat(offset4, 77123.1233f);
            pointer.writeFloat(offset8, -234.234e10f);
            assertTrue(pointer.readFloat(0) == 21.021f);
            assertTrue(pointer.readFloat(offset4) == 77123.1233f);
            assertTrue(pointer.readFloat(offset8) == -234.234e10f);
        }
    }

    public void test_writeAndReadFloat_int() {
        for (long pointerValue : pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeFloat(0, 2.3f);
            pointer.writeFloat(71, -7736.4324f);
            pointer.writeFloat(-24, Float.MIN_VALUE);
            assertTrue(pointer.readFloat(offset0) == 2.3f);
            assertTrue(pointer.readFloat(71) == -7736.4324f);
            assertTrue(pointer.readFloat(-24) == Float.MIN_VALUE);
        }
    }

    public void test_writeAndReadDouble_Offset() {
        for (long pointerValue : pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeDouble(offset0, 725448.2342499354);
            pointer.writeDouble(offset8, 7712323.1231233);
            pointer.writeDouble(16, -2323424.2364456456567e30);
            assertTrue(pointer.readDouble(0) == 725448.2342499354);
            assertTrue(pointer.readDouble(8) == 7712323.1231233);
            assertTrue(pointer.readDouble(offset16) == -2323424.2364456456567e30);
        }
    }

    public void test_writeAndReadDouble_int() {
        for (long pointerValue : pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeDouble(0, -324543.3434);
            pointer.writeDouble(71, -1.0);
            pointer.writeDouble(-24, Double.MAX_VALUE);
            assertTrue(pointer.readDouble(offset0) == -324543.3434);
            assertTrue(pointer.readDouble(71) == -1.0);
            assertTrue(pointer.readDouble(-24) == Double.MAX_VALUE);
        }
    }

    public void test_writeAndReadWord_Offset() {
        for (long pointerValue : pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeWord(offset0, addressLow);
            switch (wordWidth()) {
                case BITS_64:
                    pointer.writeWord(offset8, offsetHigh);
                    break;
                case BITS_32:
                    pointer.writeWord(offset4, offsetHigh);
                    break;
                default:
                    throw ProgramError.unknownCase();
            }
            switch (wordWidth()) {
                case BITS_64:
                    pointer.writeWord(offset16, sizeMax);
                    break;
                case BITS_32:
                    pointer.writeWord(offset8, sizeMax);
                    break;
                default:
                    throw ProgramError.unknownCase();
            }
            assertTrue(pointer.readWord(0).asAddress().equals(addressLow));
            switch (wordWidth()) {
                case BITS_64:
                    assertTrue(pointer.readWord(8).asOffset().equals(offsetHigh));
                    break;
                case BITS_32:
                    assertTrue(pointer.readWord(4).asOffset().equals(offsetHigh));
                    break;
                default:
                    throw ProgramError.unknownCase();
            }
            switch (wordWidth()) {
                case BITS_64:
                    assertTrue(pointer.readWord(offset16).asSize().equals(sizeMax));
                    break;
                case BITS_32:
                    assertTrue(pointer.readWord(offset8).asSize().equals(sizeMax));
                    break;
                default:
                    throw ProgramError.unknownCase();
            }
        }
    }

    public void test_writeAndReadWord_int() {
        for (long pointerValue : pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.writeWord(0, sizeHigh);
            pointer.writeWord(56, pointer);
            pointer.writeWord(-100, addressMedium);
            assertTrue(pointer.readWord(offset0).asSize().equals(sizeHigh));
            assertTrue(pointer.readWord(56).asPointer().equals(pointer));
            assertTrue(pointer.readWord(-100).asSize().equals(sizeMedium));
        }
    }

    public void performSetAndGetByte(int displacement) {
        for (long pointerValue : pointerValues) {
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
        for (long pointerValue : pointerValues) {
            final Pointer pointer = Pointer.fromLong(pointerValue);
            pointer.setWord(displacement, 0, sizeHigh);
            pointer.setWord(displacement, 56, pointer);
            pointer.setWord(displacement, -100, addressMedium);
            assertTrue(pointer.getWord(displacement, 0).asSize().equals(sizeHigh));
            assertTrue(pointer.getWord(displacement + (6 * Word.size()), 50).asPointer().equals(pointer));
            assertTrue(pointer.getWord(displacement, -100).asSize().equals(sizeMedium));
        }
    }

    public void test_setAndGetWord() {
        for (int displacement = -16; displacement <= 16; displacement += 8) {
            performSetAndGetWord(displacement);
        }
    }

}
