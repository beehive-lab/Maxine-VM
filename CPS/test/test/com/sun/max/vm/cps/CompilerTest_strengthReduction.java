/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.max.vm.cps;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.value.*;

/**
 * Test whether strength reduction takes place and
 * whether the resulting expressions still compute as expected.
 *
 * @author Bernd Mathiske
 */
public abstract class CompilerTest_strengthReduction<Method_Type extends IrMethod> extends CompilerTestCase<Method_Type> {

    public CompilerTest_strengthReduction(String name) {
        super(name);
    }

    @CONSTANT
    private static int intZero = 0;

    @CONSTANT
    private static int intOne = 1;

    @CONSTANT
    private static int intMinusOne = -1;

    @CONSTANT
    private static int intEight = 8;

    private static int intMinus0(int a) {
        return a - intZero;
    }

    private static int int0Minus(int b) {
        return intZero - b;
    }

    public void test_intMinus() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intMinus0");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntMinus.BUILTIN, false));
        Value result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 99);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "int0Minus");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntMinus.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), -99);
    }

    static interface Int1 {
    }
    static interface Int2 {
    }

    static class ClassA implements Int1{
    }
    static class ClassB extends ClassA implements Int2 {
        public ClassB(int x) {
            this.x = x;
        }
        public int x;
    }

    private static int instanceofClass(ClassA a) {
        if (a instanceof ClassB) {
            final ClassB b = (ClassB) a;
            return b.x;
        }
        return 0;
    }

    public void test_instanceof() {
        //TODO: this unit test is not complete. ClassA/B is unsolved in when compiled.
        final ClassB b = new ClassB(99);
        final Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "instanceofClass");
        final Value result = execute(compiledMethod, ReferenceValue.from(b));
        assertEquals(result.asInt(), 99);
    }

    private static int intPlus0(int a) {
        return a + intZero;
    }

    private static int int0Plus(int b) {
        return intZero + b;
    }

    public void test_intPlus() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intPlus0");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntPlus.BUILTIN, false));
        Value result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 99);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "int0Plus");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntPlus.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 99);
    }

    private static int intTimes0(int a) {
        return a * intZero;
    }

    private static int intTimes1(int a) {
        return intOne * a;
    }

    private static int intTimesMinusOne(int a) {
        return a * intMinusOne;
    }

    private static int intTimesEight(int a) {
        return a * intEight;
    }

    public void test_intTimes() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intTimes0");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntTimes.BUILTIN, false));
        Value result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 0);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intTimes1");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntTimes.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 99);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intTimesMinusOne");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntTimes.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), -99);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intTimesEight");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntTimes.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(10));
        assertEquals(result.asInt(), 80);
    }

    @CONSTANT
    private static long longZero = 0;

    @CONSTANT
    private static long longOne = 1;

    @CONSTANT
    private static long longMinusOne = -1;

    @CONSTANT
    private static long longEight = 8;

    private static long longMinus0(long a) {
        return a - longZero;
    }

    private static long long0Minus(long b) {
        return longZero - b;
    }

    public void test_longMinus() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longMinus0");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongMinus.BUILTIN, false));
        Value result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), 99L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "long0Minus");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongMinus.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), -99L);
    }

    private static long longPlus0(long a) {
        return a + longZero;
    }

    private static long long0Plus(long b) {
        return longZero + b;
    }

    public void test_longPlus() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longPlus0");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongPlus.BUILTIN, false));
        Value result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), 99L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "long0Plus");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongPlus.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), 99L);
    }

    private static long longTimes0(long a) {
        return a * longZero;
    }

    private static long longTimes1(long a) {
        return longOne * a;
    }

    private static long longTimesMinusOne(long a) {
        return a * longMinusOne;
    }

    private static long longTimesEight(long a) {
        return a * longEight;
    }

    public void test_longTimes() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longTimes0");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongTimes.BUILTIN, false));
        Value result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), 0L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longTimes1");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongTimes.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), 99L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longTimesMinusOne");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongTimes.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), -99L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longTimesEight");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongTimes.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(10L));
        assertEquals(result.asLong(), 80L);
    }

    private static int intDividedBy0(int a) {
        return a / intZero;
    }

    private static int int0DividedBy(int b) {
        return intZero / b;
    }

    private static int intDividedBy1(int a) {
        return a / intOne;
    }

    private static int intDividedByMinusOne(int a) {
        return a / intMinusOne;
    }

    public void test_intDividedBy() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intDividedBy0");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntDivided.BUILTIN, false));
        executeWithExpectedException(compiledMethod, ArithmeticException.class, IntValue.from(99));

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "int0DividedBy");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntDivided.BUILTIN, false));
        Value result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 0);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intDividedBy1");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntDivided.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 99);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intDividedByMinusOne");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntDivided.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), -99);
    }

    private static long longDividedBy0(long a) {
        return a / longZero;
    }

    private static long long0DividedBy(long b) {
        return longZero / b;
    }

    private static long longDividedBy1(long a) {
        return a / longOne;
    }

    private static long longDividedByMinusOne(long a) {
        return a / longMinusOne;
    }

    public void test_longDividedBy() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longDividedBy0");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongDivided.BUILTIN, false));
        executeWithExpectedException(compiledMethod, ArithmeticException.class, LongValue.from(99L));

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "long0DividedBy");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongDivided.BUILTIN, false));
        Value result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), 0L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longDividedBy1");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongDivided.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), 99L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longDividedByMinusOne");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongDivided.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), -99L);
    }

    private static Address addressDividedByAddress0(Address a) {
        return a.dividedBy(Address.zero());
    }

    private static Address addressZeroDividedByAddress(Address b) {
        return Address.zero().dividedBy(b);
    }

    private static Address addressDividedByAddress1(Address a) {
        return a.dividedBy(Address.fromInt(1));
    }

    private static Address addressDividedByAddressEight(Address a) {
        return a.dividedBy(Address.fromInt(8));
    }

    public void test_addressDividedByAddress() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "addressDividedByAddress0");
        assertFalse(compiledMethod.contains(AddressBuiltin.DividedByAddress.BUILTIN, false));
        executeWithExpectedException(compiledMethod, ArithmeticException.class, new WordValue(Address.fromInt(99)));

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "addressZeroDividedByAddress");
        assertFalse(compiledMethod.contains(AddressBuiltin.DividedByAddress.BUILTIN, false));
        Value result = execute(compiledMethod, new WordValue(Address.fromInt(99)));
        assertEquals(result.asWord().asAddress().toInt(), 0);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "addressDividedByAddress1");
        assertFalse(compiledMethod.contains(AddressBuiltin.DividedByAddress.BUILTIN, false));
        result = execute(compiledMethod, new WordValue(Address.fromInt(99)));
        assertEquals(result.asWord().asAddress().toInt(), 99);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "addressDividedByAddressEight");
        assertFalse(compiledMethod.contains(AddressBuiltin.DividedByAddress.BUILTIN, false));
        result = execute(compiledMethod, new WordValue(Address.fromInt(80)));
        assertEquals(result.asWord().asAddress().toInt(), 10);
    }

    private static Address addressDividedByInt0(Address a) {
        return a.dividedBy(intZero);
    }

    private static Address addressZeroDividedByInt(int b) {
        return Address.zero().dividedBy(b);
    }

    private static Address addressDividedByInt1(Address a) {
        return a.dividedBy(intOne);
    }

    private static Address addressDividedByIntEight(Address a) {
        return a.dividedBy(intEight);
    }

    public void test_addressDividedByInt() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "addressDividedByInt0");
        assertFalse(compiledMethod.contains(AddressBuiltin.DividedByInt.BUILTIN, false));
        executeWithExpectedException(compiledMethod, ArithmeticException.class, new WordValue(Address.fromInt(99)));

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "addressZeroDividedByInt");
        assertFalse(compiledMethod.contains(AddressBuiltin.DividedByInt.BUILTIN, false));
        Value result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asWord().asAddress().toInt(), 0);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "addressDividedByInt1");
        assertFalse(compiledMethod.contains(AddressBuiltin.DividedByInt.BUILTIN, false));
        result = execute(compiledMethod, new WordValue(Address.fromInt(99)));
        assertEquals(result.asWord().asAddress().toInt(), 99);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "addressDividedByIntEight");
        assertFalse(compiledMethod.contains(AddressBuiltin.DividedByInt.BUILTIN, false));
        result = execute(compiledMethod, new WordValue(Address.fromInt(80)));
        assertEquals(result.asWord().asAddress().toInt(), 10);
    }

    private static int intRemainder0(int a) {
        return a % intZero;
    }

    private static int int0Remainder(int b) {
        return intZero % b;
    }

    private static int intRemainder1(int a) {
        return a % intOne;
    }

    private static int intRemainderMinusOne(int a) {
        return a % intMinusOne;
    }

    private static int intRemainderEight(int a) {
        return a % intEight;
    }

    public void test_intRemainder() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intRemainder0");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntRemainder.BUILTIN, false));
        executeWithExpectedException(compiledMethod, ArithmeticException.class, IntValue.from(99));

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "int0Remainder");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntRemainder.BUILTIN, false));
        Value result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 0);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intRemainder1");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntRemainder.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 0);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intRemainderMinusOne");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntRemainder.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 0);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intRemainderEight");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntRemainder.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(85));
        assertEquals(result.asInt(), 5);
    }

    private static long longRemainder0(long a) {
        return a % longZero;
    }

    private static long long0Remainder(long b) {
        return longZero % b;
    }

    private static long longRemainder1(long a) {
        return a % longOne;
    }

    private static long longRemainderMinusOne(long a) {
        return a % longMinusOne;
    }

    private static long longRemainderEight(long a) {
        return a % longEight;
    }

    public void test_longRemainder() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longRemainder0");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongRemainder.BUILTIN, false));
        executeWithExpectedException(compiledMethod, ArithmeticException.class, LongValue.from(99L));

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "long0Remainder");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongRemainder.BUILTIN, false));
        Value result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), 0L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longRemainder1");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongRemainder.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), 0L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longRemainderMinusOne");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongRemainder.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), 0L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longRemainderEight");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongRemainder.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(85L));
        assertEquals(result.asLong(), 5L);
    }

    private static Address addressRemainderByAddress0(Address a) {
        return a.remainder(Address.zero());
    }

    private static Address addressZeroRemainderByAddress(Address b) {
        return Address.zero().remainder(b);
    }

    private static Address addressRemainderByAddress1(Address a) {
        return a.remainder(Address.fromInt(1));
    }

    private static Address addressRemainderByAddressEight(Address a) {
        return a.remainder(Address.fromInt(8));
    }

    public void test_addressRemainderByAddress() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "addressRemainderByAddress0");
        assertFalse(compiledMethod.contains(AddressBuiltin.RemainderByAddress.BUILTIN, false));
        executeWithExpectedException(compiledMethod, ArithmeticException.class, new WordValue(Address.fromInt(99)));

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "addressZeroRemainderByAddress");
        assertFalse(compiledMethod.contains(AddressBuiltin.RemainderByAddress.BUILTIN, false));
        Value result = execute(compiledMethod, new WordValue(Address.fromInt(99)));
        assertTrue(result.asWord().isZero());

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "addressRemainderByAddress1");
        assertFalse(compiledMethod.contains(AddressBuiltin.RemainderByAddress.BUILTIN, false));
        result = execute(compiledMethod, new WordValue(Address.fromInt(99)));
        assertTrue(result.asWord().isZero());

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "addressRemainderByAddressEight");
        assertFalse(compiledMethod.contains(AddressBuiltin.RemainderByAddress.BUILTIN, false));
        result = execute(compiledMethod, new WordValue(Address.fromInt(85)));
        assertEquals(result.asWord().asAddress().toInt(), 5);
    }

    private static int addressRemainderByInt0(Address a) {
        return a.remainder(intZero);
    }

    private static int addressZeroRemainderByInt(int b) {
        return Address.zero().remainder(b);
    }

    private static int addressRemainderByInt1(Address a) {
        return a.remainder(intOne);
    }

    private static int addressRemainderByIntEight(Address a) {
        return a.remainder(intEight);
    }

    public void test_addressRemainderByInt() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "addressRemainderByInt0");
        assertFalse(compiledMethod.contains(AddressBuiltin.RemainderByInt.BUILTIN, false));
        executeWithExpectedException(compiledMethod, ArithmeticException.class, new WordValue(Address.fromInt(99)));

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "addressZeroRemainderByInt");
        assertFalse(compiledMethod.contains(AddressBuiltin.RemainderByInt.BUILTIN, false));
        Value result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.toInt(), 0);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "addressRemainderByInt1");
        assertFalse(compiledMethod.contains(AddressBuiltin.RemainderByInt.BUILTIN, false));
        result = execute(compiledMethod, new WordValue(Address.fromInt(99)));
        assertEquals(result.toInt(), 0);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "addressRemainderByIntEight");
        assertFalse(compiledMethod.contains(AddressBuiltin.RemainderByInt.BUILTIN, false));
        result = execute(compiledMethod, new WordValue(Address.fromInt(85)));
        assertEquals(result.toInt(), 5);
    }

    private static int intShiftedLeft0(int b) {
        return intZero << b;
    }

    private static int intShiftedLeftBy0(int a) {
        return a << intZero;
    }

    private static int intShiftedLeftBy32(int a) {
        return a << (intEight * 4);
    }

    private static int intShiftedLeftBy65(int a) {
        return a << (intEight * 8 + 1);
    }

    public void test_intShiftedLeft() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intShiftedLeft0");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntShiftedLeft.BUILTIN, false));
        Value result = execute(compiledMethod, IntValue.from(3));
        assertEquals(result.asInt(), 0);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intShiftedLeftBy0");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntShiftedLeft.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 99);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intShiftedLeftBy32");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntShiftedLeft.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 99);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intShiftedLeftBy65");
        assertTrue(compiledMethod.contains(JavaBuiltin.IntShiftedLeft.BUILTIN, true));
        result = execute(compiledMethod, IntValue.from(4));
        assertEquals(result.asInt(), 8);
    }

    private static long longShiftedLeft0(int b) {
        return longZero << b;
    }

    private static long longShiftedLeftBy0(long a) {
        return a << intZero;
    }

    private static long longShiftedLeftBy64(long a) {
        return a << (intEight * 8);
    }

    private static long longShiftedLeftBy129(long a) {
        return a << (intEight * 16L + 1L);
    }

    public void test_longShiftedLeft() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longShiftedLeft0");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongShiftedLeft.BUILTIN, false));
        Value result = execute(compiledMethod, IntValue.from(3));
        assertEquals(result.asLong(), 0L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longShiftedLeftBy0");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongShiftedLeft.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), 99L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longShiftedLeftBy64");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongShiftedLeft.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), 99L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longShiftedLeftBy129");
        assertTrue(compiledMethod.contains(JavaBuiltin.LongShiftedLeft.BUILTIN, true));
        result = execute(compiledMethod, LongValue.from(4L));
        assertEquals(result.asLong(), 8L);
    }

    private static int intUnsignedShiftedRight0(int b) {
        return intZero >>> b;
    }

    private static int intUnsignedShiftedRightBy0(int a) {
        return a >>> intZero;
    }

    private static int intUnsignedShiftedRightBy32(int a) {
        return a >>> (intEight * 4);
    }

    private static int intUnsignedShiftedRightBy65(int a) {
        return a >>> (intEight * 8 + 1);
    }

    public void test_intUnsignedShiftedRight() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intUnsignedShiftedRight0");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntUnsignedShiftedRight.BUILTIN, false));
        Value result = execute(compiledMethod, IntValue.from(3));
        assertEquals(result.asInt(), 0);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intUnsignedShiftedRightBy0");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntUnsignedShiftedRight.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 99);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intUnsignedShiftedRightBy32");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntUnsignedShiftedRight.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 99);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intUnsignedShiftedRightBy65");
        assertTrue(compiledMethod.contains(JavaBuiltin.IntUnsignedShiftedRight.BUILTIN, true));
        result = execute(compiledMethod, IntValue.from(4));
        assertEquals(result.asInt(), 2);
    }

    private static long longUnsignedShiftedRight0(int b) {
        return longZero >>> b;
    }

    private static long longUnsignedShiftedRightBy0(long a) {
        return a >>> intZero;
    }

    private static long longUnsignedShiftedRightBy64(long a) {
        return a >>> (intEight * 8);
    }

    private static long longUnsignedShiftedRightBy129(long a) {
        return a >>> (intEight * 16L + 1L);
    }

    public void test_longUnsignedShiftedRight() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longUnsignedShiftedRight0");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongUnsignedShiftedRight.BUILTIN, false));
        Value result = execute(compiledMethod, IntValue.from(3));
        assertEquals(result.asLong(), 0L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longUnsignedShiftedRightBy0");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongUnsignedShiftedRight.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), 99L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longUnsignedShiftedRightBy64");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongUnsignedShiftedRight.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), 99L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longUnsignedShiftedRightBy129");
        assertTrue(compiledMethod.contains(JavaBuiltin.LongUnsignedShiftedRight.BUILTIN, true));
        result = execute(compiledMethod, LongValue.from(4L));
        assertEquals(result.asLong(), 2L);
    }

    private static int intSignedShiftedRightMinusOne(int b) {
        return intMinusOne >> b;
    }

    private static int intSignedShiftedRight0(int b) {
        return intZero >> b;
    }

    private static int intSignedShiftedRightBy0(int a) {
        return a >> intZero;
    }

    private static int intSignedShiftedRightBy32(int a) {
        return a >> (intEight * 4);
    }

    private static int intSignedShiftedRightBy65(int a) {
        return a >> (intEight * 8 + 1);
    }

    public void test_intSignedShiftedRight() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intSignedShiftedRightMinusOne");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntSignedShiftedRight.BUILTIN, false));
        Value result = execute(compiledMethod, IntValue.from(3));
        assertEquals(result.asInt(), -1);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intSignedShiftedRight0");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntSignedShiftedRight.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(3));
        assertEquals(result.asInt(), 0);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intSignedShiftedRightBy0");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntSignedShiftedRight.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 99);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intSignedShiftedRightBy32");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntSignedShiftedRight.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 99);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intSignedShiftedRightBy65");
        assertTrue(compiledMethod.contains(JavaBuiltin.IntSignedShiftedRight.BUILTIN, true));
        result = execute(compiledMethod, IntValue.from(4));
        assertEquals(result.asInt(), 2);
    }

    private static long longSignedShiftedRightMinusOne(int b) {
        return longMinusOne >> b;
    }

    private static long longSignedShiftedRight0(int b) {
        return longZero >> b;
    }

    private static long longSignedShiftedRightBy0(long a) {
        return a >> intZero;
    }

    private static long longSignedShiftedRightBy64(long a) {
        return a >> (intEight * 8);
    }

    private static long longSignedShiftedRightBy129(long a) {
        return a >> (intEight * 16L + 1L);
    }

    public void test_longSignedShiftedRight() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longSignedShiftedRightMinusOne");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongSignedShiftedRight.BUILTIN, false));
        Value result = execute(compiledMethod, IntValue.from(3));
        assertEquals(result.asLong(), -1L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longSignedShiftedRight0");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongSignedShiftedRight.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(3));
        assertEquals(result.asLong(), 0L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longSignedShiftedRightBy0");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongSignedShiftedRight.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), 99L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longSignedShiftedRightBy64");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongSignedShiftedRight.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), 99L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longSignedShiftedRightBy129");
        assertTrue(compiledMethod.contains(JavaBuiltin.LongSignedShiftedRight.BUILTIN, true));
        result = execute(compiledMethod, LongValue.from(4L));
        assertEquals(result.asLong(), 2L);
    }

    private static int intAnd0(int a) {
        return a & intZero;
    }

    private static int int0And(int b) {
        return intZero & b;
    }

    private static int intAndMinusOne(int a) {
        return a & intMinusOne;
    }

    private static int intMinusOneAnd(int b) {
        return intMinusOne & b;
    }

    private static int intAndSelf(int a) {
        return a & a;
    }

    public void test_intAnd() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intAnd0");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntAnd.BUILTIN, false));
        Value result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 0);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "int0And");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntAnd.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 0);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intAndMinusOne");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntAnd.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 99);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intMinusOneAnd");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntAnd.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 99);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intAndSelf");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntAnd.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 99);
    }

    private static long longAnd0(long a) {
        return a & longZero;
    }

    private static long long0And(long b) {
        return longZero & b;
    }

    private static long longAndMinusOne(long a) {
        return a & longMinusOne;
    }

    private static long longMinusOneAnd(long b) {
        return longMinusOne & b;
    }

    private static long longAndSelf(long a) {
        return a & a;
    }

    public void test_longAnd() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longAnd0");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongAnd.BUILTIN, false));
        Value result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), 0L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "long0And");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongAnd.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), 0L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longAndMinusOne");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongAnd.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), 99L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longMinusOneAnd");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongAnd.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), 99L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longAndSelf");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongAnd.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), 99L);
    }

    private static int intOr0(int a) {
        return a | intZero;
    }

    private static int int0Or(int b) {
        return intZero | b;
    }

    private static int intOrMinusOne(int a) {
        return a | intMinusOne;
    }

    private static int intMinusOneOr(int b) {
        return intMinusOne | b;
    }

    private static int intOrSelf(int a) {
        return a | a;
    }

    public void test_intOr() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intOr0");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntOr.BUILTIN, false));
        Value result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 99);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "int0Or");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntOr.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 99);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intOrMinusOne");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntOr.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), -1);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intMinusOneOr");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntOr.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), -1);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intOrSelf");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntOr.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 99);
    }

    private static long longOr0(long a) {
        return a | longZero;
    }

    private static long long0Or(long b) {
        return longZero | b;
    }

    private static long longOrMinusOne(long a) {
        return a | longMinusOne;
    }

    private static long longMinusOneOr(long b) {
        return longMinusOne | b;
    }

    private static long longOrSelf(long a) {
        return a | a;
    }

    public void test_longOr() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longOr0");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongOr.BUILTIN, false));
        Value result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), 99L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "long0Or");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongOr.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), 99L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longOrMinusOne");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongOr.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), -1L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longMinusOneOr");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongOr.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), -1L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longOrSelf");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongOr.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), 99L);
    }

    private static int intXor0(int a) {
        return a ^ intZero;
    }

    private static int int0Xor(int b) {
        return intZero ^ b;
    }

    private static int intXorMinusOne(int a) {
        return a ^ intMinusOne;
    }

    private static int intMinusOneXor(int b) {
        return intMinusOne ^ b;
    }

    private static int intXorSelf(int a) {
        return a ^ a;
    }

    public void test_intXor() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intXor0");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntXor.BUILTIN, false));
        Value result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 99);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "int0Xor");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntXor.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 99);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intXorMinusOne");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntXor.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), ~99);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intMinusOneXor");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntXor.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), ~99);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intXorSelf");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntXor.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 0);
    }

    private static long longXor0(long a) {
        return a ^ longZero;
    }

    private static long long0Xor(long b) {
        return longZero ^ b;
    }

    private static long longXorMinusOne(long a) {
        return a ^ longMinusOne;
    }

    private static long longMinusOneXor(long b) {
        return longMinusOne ^ b;
    }

    private static long longXorSelf(long a) {
        return a ^ a;
    }

    public void test_longXor() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longXor0");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongXor.BUILTIN, false));
        Value result = execute(compiledMethod, LongValue.from(99));
        assertEquals(result.asLong(), 99L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "long0Xor");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongXor.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), 99L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longXorMinusOne");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongXor.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), ~99L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longMinusOneXor");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongXor.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), ~99L);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longXorSelf");
        assertFalse(compiledMethod.contains(JavaBuiltin.LongXor.BUILTIN, false));
        result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), 0L);
    }

    /**
     * There is no Java bytecode that performs '~' directly.
     * We test here whether whatever javac generates does get strength-reduced.
     */
    private static int intNot(int a) {
        return ~a;
    }

    private static int intNotNot(int a) {
        return ~~a;
    }

    public void test_intNot() {
        Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intNot");
        assertTrue(compiledMethod.contains(JavaBuiltin.IntNot.BUILTIN, true));
        Value result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), ~99);

        compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "intNotNot");
        assertFalse(compiledMethod.contains(JavaBuiltin.IntNot.BUILTIN, false));
        result = execute(compiledMethod, IntValue.from(99));
        assertEquals(result.asInt(), 99);
    }

    /**
     * There is no Java bytecode that performs '~' directly.
     * We test here whether whatever javac generates does get strength-reduced.
     */
    private static long longNot(long a) {
        return ~a;
    }

    public void test_longNot() {
        final Method_Type compiledMethod = compileMethod(CompilerTest_strengthReduction.class, "longNot");
        assertTrue(compiledMethod.contains(JavaBuiltin.LongNot.BUILTIN, true));
        final Value result = execute(compiledMethod, LongValue.from(99L));
        assertEquals(result.asLong(), ~99L);
    }
}
