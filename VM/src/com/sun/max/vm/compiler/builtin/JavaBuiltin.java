/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.compiler.builtin;

import com.sun.max.annotate.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.snippet.*;

public abstract class JavaBuiltin extends Builtin {

    protected JavaBuiltin() {
        super(null);
    }

    @Override
    public final boolean hasSideEffects() {
        return false;
    }

    public static class IntNegated extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitIntNegated(this, result, arguments);
        }

        @BUILTIN(value = IntNegated.class)
        public static int intNegated(int value) {
            return -value;
        }

        public static final IntNegated BUILTIN = new IntNegated();
    }

    public static class FloatNegated extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitFloatNegated(this, result, arguments);
        }

        @BUILTIN(value = FloatNegated.class)
        public static float floatNegated(float value) {
            return -value;
        }

        public static final FloatNegated BUILTIN = new FloatNegated();
    }

    public static class LongNegated extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitLongNegated(this, result, arguments);
        }

        @BUILTIN(value = LongNegated.class)
        public static long longNegated(long value) {
            return -value;
        }

        public static final LongNegated BUILTIN = new LongNegated();
    }

    public static class DoubleNegated extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitDoubleNegated(this, result, arguments);
        }

        @BUILTIN(value = DoubleNegated.class)
        public static double doubleNegated(double value) {
            return -value;
        }

        public static final DoubleNegated BUILTIN = new DoubleNegated();
    }

    public static class IntPlus extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitIntPlus(this, result, arguments);
        }

        @BUILTIN(value = IntPlus.class)
        public static int intPlus(int a, int b) {
            return a + b;
        }

        public static final IntPlus BUILTIN = new IntPlus();
    }

    public static class FloatPlus extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitFloatPlus(this, result, arguments);
        }

        @BUILTIN(value = FloatPlus.class)
        public static float floatPlus(float a, float b) {
            return a + b;
        }

        public static final FloatPlus BUILTIN = new FloatPlus();
    }

    public static class LongPlus extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitLongPlus(this, result, arguments);
        }

        @BUILTIN(value = LongPlus.class)
        public static long longPlus(long a, long b) {
            return a + b;
        }

        public static final LongPlus BUILTIN = new LongPlus();
    }

    public static class DoublePlus extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitDoublePlus(this, result, arguments);
        }

        @BUILTIN(value = DoublePlus.class)
        public static double doublePlus(double a, double b) {
            return a + b;
        }

        public static final DoublePlus BUILTIN = new DoublePlus();
    }

    public static class IntMinus extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitIntMinus(this, result, arguments);
        }

        @BUILTIN(value = IntMinus.class)
        public static int intMinus(int minuend, int subtrahend) {
            return minuend - subtrahend;
        }

        public static final IntMinus BUILTIN = new IntMinus();
    }

    public static class FloatMinus extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitFloatMinus(this, result, arguments);
        }

        @BUILTIN(value = FloatMinus.class)
        public static float floatMinus(float minuend, float subtrahend) {
            return minuend - subtrahend;
        }

        public static final FloatMinus BUILTIN = new FloatMinus();
    }

    public static class LongMinus extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitLongMinus(this, result, arguments);
        }

        @BUILTIN(value = LongMinus.class)
        public static long longMinus(long minuend, long subtrahend) {
            return minuend - subtrahend;
        }

        public static final LongMinus BUILTIN = new LongMinus();
    }

    public static class DoubleMinus extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitDoubleMinus(this, result, arguments);
        }

        @BUILTIN(value = DoubleMinus.class)
        public static double doubleMinus(double minuend, double subtrahend) {
            return minuend - subtrahend;
        }

        public static final DoubleMinus BUILTIN = new DoubleMinus();
    }

    public static class IntTimes extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitIntTimes(this, result, arguments);
        }

        @BUILTIN(value = IntTimes.class)
        public static int intTimes(int a, int b) {
            return a * b;
        }

        public static final IntTimes BUILTIN = new IntTimes();
    }

    public static class FloatTimes extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitFloatTimes(this, result, arguments);
        }

        @BUILTIN(value = FloatTimes.class)
        public static float floatTimes(float a, float b) {
            return a * b;
        }

        public static final FloatTimes BUILTIN = new FloatTimes();
    }

    public static class LongTimes extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitLongTimes(this, result, arguments);
        }

        @BUILTIN(value = LongTimes.class)
        public static long longTimes(long a, long b) {
            return a * b;
        }

        public static final LongTimes BUILTIN = new LongTimes();
    }

    public static class DoubleTimes extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitDoubleTimes(this, result, arguments);
        }

        @BUILTIN(value = DoubleTimes.class)
        public static double doubleTimes(double a, double b) {
            return a * b;
        }

        public static final DoubleTimes BUILTIN = new DoubleTimes();
    }

    public static class IntDivided extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitIntDivided(this, result, arguments);
        }

        @BUILTIN(value = IntDivided.class)
        public static int intDivided(int dividend, int divisor) {
            return dividend / divisor;
        }

        public static final IntDivided BUILTIN = new IntDivided();

        @Override
        public int reasonsMayStop() {
            return Stoppable.DIVIDE_BY_ZERO_CHECK;
        }
    }

    public static class FloatDivided extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitFloatDivided(this, result, arguments);
        }

        @BUILTIN(value = FloatDivided.class)
        public static float floatDivided(float dividend, float divisor) {
            return dividend / divisor;
        }

        public static final FloatDivided BUILTIN = new FloatDivided();

        @Override
        public int reasonsMayStop() {
            return Stoppable.DIVIDE_BY_ZERO_CHECK;
        }
    }

    public static class LongDivided extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitLongDivided(this, result, arguments);
        }

        @BUILTIN(value = LongDivided.class)
        public static long longDivided(long dividend, long divisor) {
            return dividend / divisor;
        }

        public static final LongDivided BUILTIN = new LongDivided();

        @Override
        public int reasonsMayStop() {
            return Stoppable.DIVIDE_BY_ZERO_CHECK;
        }
    }

    public static class DoubleDivided extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitDoubleDivided(this, result, arguments);
        }

        @BUILTIN(value = DoubleDivided.class)
        public static double doubleDivided(double dividend, double divisor) {
            return dividend / divisor;
        }

        public static final DoubleDivided BUILTIN = new DoubleDivided();

        @Override
        public int reasonsMayStop() {
            return Stoppable.DIVIDE_BY_ZERO_CHECK;
        }
    }

    public static class IntRemainder extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitIntRemainder(this, result, arguments);
        }

        @BUILTIN(value = IntRemainder.class)
        public static int intRemainder(int dividend, int divisor) {
            return dividend % divisor;
        }

        public static final IntRemainder BUILTIN = new IntRemainder();

        @Override
        public int reasonsMayStop() {
            return Stoppable.DIVIDE_BY_ZERO_CHECK;
        }
    }

    public static class FloatRemainder extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitFloatRemainder(this, result, arguments);
        }

        @BUILTIN(value = FloatRemainder.class)
        public static float floatRemainder(float dividend, float divisor) {
            return dividend % divisor;
        }

        @Override
        public int reasonsMayStop() {
            return Stoppable.DIVIDE_BY_ZERO_CHECK;
        }

        public static final FloatRemainder BUILTIN = new FloatRemainder();
        public static final Snippet SNIPPET = Snippet.FloatRemainder.SNIPPET;
    }

    public static class LongRemainder extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitLongRemainder(this, result, arguments);
        }

        @BUILTIN(value = LongRemainder.class)
        public static long longRemainder(long dividend, long divisor) {
            return dividend % divisor;
        }

        public static final LongRemainder BUILTIN = new LongRemainder();

        @Override
        public int reasonsMayStop() {
            return Stoppable.DIVIDE_BY_ZERO_CHECK;
        }
    }

    public static class DoubleRemainder extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitDoubleRemainder(this, result, arguments);
        }

        @BUILTIN(value = DoubleRemainder.class)
        public static double doubleRemainder(double dividend, double divisor) {
            return dividend % divisor;
        }

        public static final DoubleRemainder BUILTIN = new DoubleRemainder();
        public static final Snippet SNIPPET = Snippet.DoubleRemainder.SNIPPET;

        @Override
        public int reasonsMayStop() {
            return Stoppable.DIVIDE_BY_ZERO_CHECK;
        }
    }

    public static class IntShiftedLeft extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitIntShiftedLeft(this, result, arguments);
        }

        @BUILTIN(value = IntShiftedLeft.class)
        public static int intShiftedLeft(int number, int shift) {
            return number << shift;
        }

        public static final IntShiftedLeft BUILTIN = new IntShiftedLeft();
    }

    public static class LongShiftedLeft extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitLongShiftedLeft(this, result, arguments);
        }

        @BUILTIN(value = LongShiftedLeft.class)
        public static long longShiftedLeft(long number, int shift) {
            return number << shift;
        }

        public static final LongShiftedLeft BUILTIN = new LongShiftedLeft();
    }

    public static class IntSignedShiftedRight extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitIntSignedShiftedRight(this, result, arguments);
        }

        @BUILTIN(value = IntSignedShiftedRight.class)
        public static int intSignedShiftedRight(int number, int shift) {
            return number >> shift;
        }

        public static final IntSignedShiftedRight BUILTIN = new IntSignedShiftedRight();
    }

    public static class LongSignedShiftedRight extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitLongSignedShiftedRight(this, result, arguments);
        }

        @BUILTIN(value = LongSignedShiftedRight.class)
        public static long longSignedShiftedRight(long number, int shift) {
            return number >> shift;
        }

        public static final LongSignedShiftedRight BUILTIN = new LongSignedShiftedRight();
    }

    public static class IntUnsignedShiftedRight extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitIntUnsignedShiftedRight(this, result, arguments);
        }

        @BUILTIN(value = IntUnsignedShiftedRight.class)
        public static int intUnsignedShiftedRight(int number, int shift) {
            return number >>> shift;
        }

        public static final IntUnsignedShiftedRight BUILTIN = new IntUnsignedShiftedRight();
    }

    public static class LongUnsignedShiftedRight extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitLongUnsignedShiftedRight(this, result, arguments);
        }

        @BUILTIN(value = LongUnsignedShiftedRight.class)
        public static long longUnsignedShiftedRight(long number, int shift) {
            return number >>> shift;
        }

        public static final LongUnsignedShiftedRight BUILTIN = new LongUnsignedShiftedRight();
    }

    public static class IntNot extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitIntNot(this, result, arguments);
        }

        @BUILTIN(value = IntNot.class)
        public static int intNot(int value) {
            return ~value;
        }

        public static final IntNot BUILTIN = new IntNot();
    }

    public static class LongNot extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitLongNot(this, result, arguments);
        }

        @BUILTIN(value = LongNot.class)
        public static long longNot(long value) {
            return ~value;
        }

        public static final LongNot BUILTIN = new LongNot();
    }

    public static class IntAnd extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitIntAnd(this, result, arguments);
        }

        @BUILTIN(value = IntAnd.class)
        public static int intAnd(int addend1, int addend2) {
            return addend1 & addend2;
        }

        public static final IntAnd BUILTIN = new IntAnd();
    }

    public static class LongAnd extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitLongAnd(this, result, arguments);
        }

        @BUILTIN(value = LongAnd.class)
        public static long longAnd(long addend1, long addend2) {
            return addend1 & addend2;
        }

        public static final LongAnd BUILTIN = new LongAnd();
    }

    public static class IntOr extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitIntOr(this, result, arguments);
        }

        @BUILTIN(value = IntOr.class)
        public static int intOr(int addend1, int addend2) {
            return addend1 | addend2;
        }

        public static final IntOr BUILTIN = new IntOr();
    }

    public static class LongOr extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitLongOr(this, result, arguments);
        }

        @BUILTIN(value = LongOr.class)
        public static long longOr(long addend1, long addend2) {
            return addend1 | addend2;
        }

        public static final LongOr BUILTIN = new LongOr();
    }

    public static class IntXor extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitIntXor(this, result, arguments);
        }

        @BUILTIN(value = IntXor.class)
        public static int intXor(int addend1, int addend2) {
            return addend1 ^ addend2;
        }

        public static final IntXor BUILTIN = new IntXor();
    }

    public static class LongXor extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitLongXor(this, result, arguments);
        }

        @BUILTIN(value = LongXor.class)
        public static long longXor(long addend1, long addend2) {
            return addend1 ^ addend2;
        }

        public static final LongXor BUILTIN = new LongXor();
    }

    public static class LongCompare extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitLongCompare(this, result, arguments);
        }

        @BUILTIN(value = LongCompare.class)
        public static int longCompare(long greater, long less) {
            final Long n = greater;
            return n.compareTo(less);
        }

        public static final LongCompare BUILTIN = new LongCompare();
    }

    public static class FloatCompareL extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitFloatCompareL(this, result, arguments);
        }

        @BUILTIN(value = FloatCompareL.class)
        public static int floatCompareL(float greater, float less) {
            // TODO: NaN treatment
            final Float f = greater;
            return f.compareTo(less);
        }

        public static final FloatCompareL BUILTIN = new FloatCompareL();
    }

    public static class FloatCompareG extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitFloatCompareG(this, result, arguments);
        }

        @BUILTIN(value = FloatCompareG.class)
        public static int floatCompareG(float greater, float less) {
            // TODO: NaN treatment
            final Float f = greater;
            return f.compareTo(less);
        }

        public static final FloatCompareG BUILTIN = new FloatCompareG();
    }

    public static class DoubleCompareL extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitDoubleCompareL(this, result, arguments);
        }

        @BUILTIN(value = DoubleCompareL.class)
        public static int doubleCompareL(double greater, double less) {
            // TODO: NaN treatment
            final Double d = greater;
            return d.compareTo(less);
        }

        public static final DoubleCompareL BUILTIN = new DoubleCompareL();
    }

    public static class DoubleCompareG extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitDoubleCompareG(this, result, arguments);
        }

        @BUILTIN(value = DoubleCompareG.class)
        public static int doubleCompareG(double greater, double less) {
            // TODO: NaN treatment
            final Double d = greater;
            return d.compareTo(less);
        }

        public static final DoubleCompareG BUILTIN = new DoubleCompareG();
    }

    public static class ConvertByteToInt extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitConvertByteToInt(this, result, arguments);
        }

        @BUILTIN(value = ConvertByteToInt.class)
        public static int convertByteToInt(byte byteValue) {
            return byteValue;
        }

        public static final ConvertByteToInt BUILTIN = new ConvertByteToInt();
    }

    public static class ConvertCharToInt extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitConvertCharToInt(this, result, arguments);
        }

        @BUILTIN(value = ConvertCharToInt.class)
        public static int convertCharToInt(char charValue) {
            return charValue;
        }

        public static final ConvertCharToInt BUILTIN = new ConvertCharToInt();
    }

    public static class ConvertShortToInt extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitConvertShortToInt(this, result, arguments);
        }

        @BUILTIN(value = ConvertShortToInt.class)
        public static int convertShortToInt(short shortValue) {
            return shortValue;
        }

        public static final ConvertShortToInt BUILTIN = new ConvertShortToInt();
    }

    public static class ConvertIntToByte extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitConvertIntToByte(this, result, arguments);
        }

        @BUILTIN(value = ConvertIntToByte.class)
        public static byte convertIntToByte(int intValue) {
            return (byte) intValue;
        }

        public static final ConvertIntToByte BUILTIN = new ConvertIntToByte();
    }

    public static class ConvertIntToChar extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitConvertIntToChar(this, result, arguments);
        }

        @BUILTIN(value = ConvertIntToChar.class)
        public static char convertIntToChar(int intValue) {
            return (char) intValue;
        }

        public static final ConvertIntToChar BUILTIN = new ConvertIntToChar();
    }

    public static class ConvertIntToShort extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitConvertIntToShort(this, result, arguments);
        }

        @BUILTIN(value = ConvertIntToShort.class)
        public static short convertIntToShort(int intValue) {
            return (short) intValue;
        }

        public static final ConvertIntToShort BUILTIN = new ConvertIntToShort();
    }

    public static class ConvertIntToFloat extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitConvertIntToFloat(this, result, arguments);
        }

        @BUILTIN(value = ConvertIntToFloat.class)
        public static float convertIntToFloat(int intValue) {
            return intValue;
        }

        public static final ConvertIntToFloat BUILTIN = new ConvertIntToFloat();
    }

    public static class ConvertIntToLong extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitConvertIntToLong(this, result, arguments);
        }

        @BUILTIN(value = ConvertIntToLong.class)
        public static long convertIntToLong(int intValue) {
            return intValue;
        }

        public static final ConvertIntToLong BUILTIN = new ConvertIntToLong();
    }

    public static class ConvertIntToDouble extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitConvertIntToDouble(this, result, arguments);
        }

        @BUILTIN(value = ConvertIntToDouble.class)
        public static double convertIntToDouble(int intValue) {
            return intValue;
        }

        public static final ConvertIntToDouble BUILTIN = new ConvertIntToDouble();
    }

    public static class ConvertFloatToDouble extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitConvertFloatToDouble(this, result, arguments);
        }

        @BUILTIN(value = ConvertFloatToDouble.class)
        public static double convertFloatToDouble(float floatValue) {
            return floatValue;
        }

        public static final ConvertFloatToDouble BUILTIN = new ConvertFloatToDouble();
    }

    public static class ConvertLongToInt extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitConvertLongToInt(this, result, arguments);
        }

        @BUILTIN(value = ConvertLongToInt.class)
        public static int convertLongToInt(long longValue) {
            return (int) longValue;
        }

        public static final ConvertLongToInt BUILTIN = new ConvertLongToInt();
    }

    public static class ConvertLongToFloat extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitConvertLongToFloat(this, result, arguments);
        }

        @BUILTIN(value = ConvertLongToFloat.class)
        public static float convertLongToFloat(long longValue) {
            return longValue;
        }

        public static final ConvertLongToFloat BUILTIN = new ConvertLongToFloat();
    }

    public static class ConvertLongToDouble extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitConvertLongToDouble(this, result, arguments);
        }

        @BUILTIN(value = ConvertLongToDouble.class)
        public static double convertLongToDouble(long longValue) {
            return longValue;
        }

        public static final ConvertLongToDouble BUILTIN = new ConvertLongToDouble();
    }

    public static class ConvertDoubleToFloat extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitConvertDoubleToFloat(this, result, arguments);
        }

        @BUILTIN(value = ConvertDoubleToFloat.class)
        public static float convertDoubleToFloat(double doubleValue) {
            return (float) doubleValue;
        }

        public static final ConvertDoubleToFloat BUILTIN = new ConvertDoubleToFloat();
    }

}
