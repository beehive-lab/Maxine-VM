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
package com.sun.max.vm.compiler.builtin;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.type.*;

public abstract class JavaBuiltin extends Builtin {

    protected JavaBuiltin() {
        super(null);
    }

    @Override
    public final boolean hasSideEffects() {
        return false;
    }

    @Override
    public boolean isFoldable(IrValue[] arguments) {
        if (!super.isFoldable(arguments)) {
            return false;
        }
        for (IrValue argument : arguments) {
            if (argument.kind() == Kind.REFERENCE) {
                return false;
            }
        }
        return true;
    }

    public static class IntNegated extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitIntNegated(this, result, arguments);
        }

        @BUILTIN(builtinClass = IntNegated.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = FloatNegated.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = LongNegated.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = DoubleNegated.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = IntPlus.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = FloatPlus.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = LongPlus.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = DoublePlus.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = IntMinus.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = FloatMinus.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = LongMinus.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = DoubleMinus.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = IntTimes.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = FloatTimes.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = LongTimes.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = DoubleTimes.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = IntDivided.class)
        public static int intDivided(int dividend, int divisor) {
            return dividend / divisor;
        }

        public static final IntDivided BUILTIN = new IntDivided();
    }


    public static class FloatDivided extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitFloatDivided(this, result, arguments);
        }

        @BUILTIN(builtinClass = FloatDivided.class)
        public static float floatDivided(float dividend, float divisor) {
            return dividend / divisor;
        }

        public static final FloatDivided BUILTIN = new FloatDivided();
    }


    public static class LongDivided extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitLongDivided(this, result, arguments);
        }

        @BUILTIN(builtinClass = LongDivided.class)
        public static long longDivided(long dividend, long divisor) {
            return dividend / divisor;
        }

        public static final LongDivided BUILTIN = new LongDivided();
    }


    public static class DoubleDivided extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitDoubleDivided(this, result, arguments);
        }

        @BUILTIN(builtinClass = DoubleDivided.class)
        public static double doubleDivided(double dividend, double divisor) {
            return dividend / divisor;
        }

        public static final DoubleDivided BUILTIN = new DoubleDivided();
    }


    public static class IntRemainder extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitIntRemainder(this, result, arguments);
        }

        @BUILTIN(builtinClass = IntRemainder.class)
        public static int intRemainder(int dividend, int divisor) {
            return dividend % divisor;
        }

        public static final IntRemainder BUILTIN = new IntRemainder();
    }


    public static class FloatRemainder extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitFloatRemainder(this, result, arguments);
        }

        @BUILTIN(builtinClass = FloatRemainder.class)
        public static float floatRemainder(float dividend, float divisor) {
            return dividend % divisor;
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

        @BUILTIN(builtinClass = LongRemainder.class)
        public static long longRemainder(long dividend, long divisor) {
            return dividend % divisor;
        }

        public static final LongRemainder BUILTIN = new LongRemainder();
    }


    public static class DoubleRemainder extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitDoubleRemainder(this, result, arguments);
        }

        @BUILTIN(builtinClass = DoubleRemainder.class)
        public static double doubleRemainder(double dividend, double divisor) {
            return dividend % divisor;
        }

        public static final DoubleRemainder BUILTIN = new DoubleRemainder();
        public static final Snippet SNIPPET = Snippet.DoubleRemainder.SNIPPET;
    }


    public static class IntShiftedLeft extends JavaBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitIntShiftedLeft(this, result, arguments);
        }

        @BUILTIN(builtinClass = IntShiftedLeft.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = LongShiftedLeft.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = IntSignedShiftedRight.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = LongSignedShiftedRight.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = IntUnsignedShiftedRight.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = LongUnsignedShiftedRight.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = IntNot.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = LongNot.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = IntAnd.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = LongAnd.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = IntOr.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = LongOr.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = IntXor.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = LongXor.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = LongCompare.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = FloatCompareL.class, thrownExceptions = 0)
        public static int floatCompareL(float greater, float less) {
            Problem.todo("NaN treatment");
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

        @BUILTIN(builtinClass = FloatCompareG.class, thrownExceptions = 0)
        public static int floatCompareG(float greater, float less) {
            Problem.todo("NaN treatment");
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

        @BUILTIN(builtinClass = DoubleCompareL.class, thrownExceptions = 0)
        public static int doubleCompareL(double greater, double less) {
            Problem.todo("NaN treatment");
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

        @BUILTIN(builtinClass = DoubleCompareG.class, thrownExceptions = 0)
        public static int doubleCompareG(double greater, double less) {
            Problem.todo("NaN treatment");
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

        @BUILTIN(builtinClass = ConvertByteToInt.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = ConvertCharToInt.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = ConvertShortToInt.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = ConvertIntToByte.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = ConvertIntToChar.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = ConvertIntToShort.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = ConvertIntToFloat.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = ConvertIntToLong.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = ConvertIntToDouble.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = ConvertFloatToDouble.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = ConvertLongToInt.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = ConvertLongToFloat.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = ConvertLongToDouble.class, thrownExceptions = 0)
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

        @BUILTIN(builtinClass = ConvertDoubleToFloat.class, thrownExceptions = 0)
        public static float convertDoubleToFloat(double doubleValue) {
            return (float) doubleValue;
        }

        public static final ConvertDoubleToFloat BUILTIN = new ConvertDoubleToFloat();
    }

}
