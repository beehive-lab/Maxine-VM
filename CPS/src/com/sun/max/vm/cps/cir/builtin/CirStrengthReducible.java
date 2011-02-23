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
package com.sun.max.vm.cps.cir.builtin;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.optimize.*;
import com.sun.max.vm.cps.cir.snippet.*;
import com.sun.max.vm.cps.cir.variable.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Local strength reduction for various builtins.
 *
 * @author Bernd Mathiske
 */
public abstract class CirStrengthReducible extends CirSpecialBuiltin {

    @HOSTED_ONLY
    protected CirStrengthReducible(Builtin builtin) {
        super(builtin);
    }

    /**
     * Is reducible if at least one argument is pathological.
     *
     * @author Bernd Mathiske
     */
    private abstract static class Alternative extends CirStrengthReducible {
        @HOSTED_ONLY
        private Alternative(Builtin builtin) {
            super(builtin);
        }

        /**
         * @return whether the argument is pathological, permitting reduction
         */
        protected abstract boolean isReducible(CirValue argument);

        @Override
        public boolean isReducible(CirOptimizer cirOptimizer, CirValue[] arguments) {
            return isReducible(arguments[0]) || isReducible(arguments[1]);
        }

    }

    public static final class IntMinus extends Alternative {
        @HOSTED_ONLY
        public IntMinus() {
            super(JavaBuiltin.IntMinus.BUILTIN);
        }

        @Override
        protected boolean isReducible(CirValue argument) {
            if (argument.isScalarConstant()) {
                return argument.value().toInt() == 0;
            }
            return false;
        }

        @Override
        public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
            final CirValue minuend = arguments[0];
            final CirValue subtrahend = arguments[1];
            final CirValue normalContinuation = arguments[2];
            final CirValue exceptionContinuation = arguments[3];
            if (isReducible(minuend)) {
                return new CirCall(CirBuiltin.get(JavaBuiltin.IntNegated.BUILTIN), subtrahend, normalContinuation, exceptionContinuation);
            }
            assert isReducible(subtrahend);
            return new CirCall(normalContinuation, minuend);
        }
    }

    public static final class LongMinus extends Alternative {
        @HOSTED_ONLY
        public LongMinus() {
            super(JavaBuiltin.LongMinus.BUILTIN);
        }

        @Override
        protected boolean isReducible(CirValue argument) {
            if (argument.isScalarConstant()) {
                if (argument.value().kind() != Kind.REFERENCE) {
                    return argument.value().toLong() == 0L;
                }
            }
            return false;
        }

        @Override
        public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
            final CirValue minuend = arguments[0];
            final CirValue subtrahend = arguments[1];
            final CirValue normalContinuation = arguments[2];
            final CirValue exceptionContinuation = arguments[3];
            if (isReducible(arguments[0])) {
                return new CirCall(CirBuiltin.get(JavaBuiltin.LongNegated.BUILTIN), subtrahend, normalContinuation, exceptionContinuation);
            }
            assert isReducible(arguments[1]);
            return new CirCall(normalContinuation, minuend);
        }
    }

    /**
     * Commutative operations such as plus, multiply.
     *
     * @author Bernd Mathiske
     */
    private abstract static class Commutative extends Alternative {
        @HOSTED_ONLY
        protected Commutative(Builtin builtin) {
            super(builtin);
        }

        protected abstract CirCall reduce(CirValue numberValue, CirValue factorValue, CirValue normalContinuation, CirValue exceptionContinuation);

        @Override
        public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
            if (isReducible(arguments[0])) {
                return reduce(arguments[1], arguments[0], arguments[2], arguments[3]);
            }
            assert isReducible(arguments[1]);
            return reduce(arguments[0], arguments[1], arguments[2], arguments[3]);
        }
    }

    public static final class IntPlus extends Commutative {
        @HOSTED_ONLY
        public IntPlus() {
            super(JavaBuiltin.IntPlus.BUILTIN);
        }

        @Override
        protected boolean isReducible(CirValue argument) {
            if (argument.isScalarConstant()) {
                return argument.value().toInt() == 0;
            }
            return false;
        }

        @Override
        protected CirCall reduce(CirValue numberValue, CirValue addendValue, CirValue normalContinuation, CirValue exceptionContinuation) {
            return new CirCall(normalContinuation, numberValue);
        }
    }

    public static final class LongPlus extends Commutative {
        @HOSTED_ONLY
        public LongPlus() {
            super(JavaBuiltin.LongPlus.BUILTIN);
        }

        @Override
        protected boolean isReducible(CirValue argument) {
            if (argument.isScalarConstant() && argument.kind().isExtendedPrimitiveValue()) {
                return argument.value().toLong() == 0L;
            }
            return false;
        }

        @Override
        protected CirCall reduce(CirValue numberValue, CirValue addendValue, CirValue normalContinuation, CirValue exceptionContinuation) {
            return new CirCall(normalContinuation, numberValue);
        }
    }

    public static final class IntTimes extends Commutative {
        @HOSTED_ONLY
        public IntTimes() {
            super(JavaBuiltin.IntTimes.BUILTIN);
        }

        @Override
        protected boolean isReducible(CirValue argument) {
            if (argument.isScalarConstant()) {
                final int a = argument.value().toInt();
                return a == -1 || Ints.isPowerOfTwoOrZero(a);
            }
            return false;
        }

        @Override
        protected CirCall reduce(CirValue numberValue, CirValue factorValue, CirValue normalContinuation, CirValue exceptionContinuation) {
            final int factor = factorValue.value().toInt();
            switch (factor) {
                case -1: {
                    return new CirCall(CirBuiltin.get(JavaBuiltin.IntNegated.BUILTIN), numberValue, normalContinuation, exceptionContinuation);
                }
                case 0: {
                    return new CirCall(normalContinuation, new CirConstant(IntValue.ZERO));
                }
                case 1: {
                    return new CirCall(normalContinuation, numberValue);
                }
                default: {
                    final int shift = Integer.numberOfTrailingZeros(factor);
                    return new CirCall(CirBuiltin.get(JavaBuiltin.IntShiftedLeft.BUILTIN), numberValue, new CirConstant(IntValue.from(shift)), normalContinuation, exceptionContinuation);
                }
            }
        }
    }

    public static final class LongTimes extends Commutative {
        @HOSTED_ONLY
        public LongTimes() {
            super(JavaBuiltin.LongTimes.BUILTIN);
        }

        @Override
        protected boolean isReducible(CirValue argument) {
            if (argument.isScalarConstant()) {
                final long a = argument.value().toLong();
                return a == -1L || Longs.isPowerOfTwoOrZero(a);
            }
            return false;
        }

        @Override
        protected CirCall reduce(CirValue numberValue, CirValue factorValue, CirValue normalContinuation, CirValue exceptionContinuation) {
            final long factor = factorValue.value().toLong();
            if (factor == 0L) {
                return new CirCall(normalContinuation, new CirConstant(LongValue.ZERO));
            }
            if (factor == -1L) {
                return new CirCall(CirBuiltin.get(JavaBuiltin.LongNegated.BUILTIN), numberValue, normalContinuation, exceptionContinuation);
            }
            if (factor == 1) {
                return new CirCall(normalContinuation, numberValue);
            }
            final int shift = Long.numberOfTrailingZeros(factor);
            return new CirCall(CirBuiltin.get(JavaBuiltin.LongShiftedLeft.BUILTIN), numberValue, new CirConstant(IntValue.from(shift)), normalContinuation, exceptionContinuation);
        }
    }

    public static final class IntDivided extends CirStrengthReducible {
        @HOSTED_ONLY
        public IntDivided() {
            super(JavaBuiltin.IntDivided.BUILTIN);
        }

        @Override
        public boolean isReducible(CirOptimizer cirOptimizer, CirValue[] arguments) {
            if (arguments[1].isScalarConstant()) {
                final int divisor = arguments[1].value().toInt();
                // TODO: strength reduction appears (to the untrained eye) to be possible for power of 2 divisors,
                // but due to rounding of negative values this is NOT exactly equivalent to an arithmetic shift right.
                // revisit in the future.
                if (divisor == 0 || divisor == -1 || divisor == 1) {
                    return true;
                }
//                if (MaxineVM.isHosted() && Ints.isPowerOfTwoOrZero(divisor) && MaxineVM.isMaxineClass(cirOptimizer.classMethodActor().holder())) {
//                    ProgramWarning.message(cirOptimizer.classMethodActor().format("%H.%n(%p): Consider replacing division by " + divisor +
//                        " with use of " + Unsigned.class + " or a shift operation"));
//                }
            }
            if (arguments[0].isScalarConstant()) {
                final Value dividendValue = arguments[0].value();
                if (dividendValue.isZero()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
            final CirValue dividendValue = arguments[0];
            final CirValue divisorValue = arguments[1];
            final CirValue normalContinuation = arguments[2];
            final CirValue exceptionContinuation = arguments[3];
            if (divisorValue.isScalarConstant()) {
                final int divisor = divisorValue.value().toInt();
                switch (divisor) {
                    case -1: {
                        return new CirCall(CirBuiltin.get(JavaBuiltin.IntNegated.BUILTIN), dividendValue, normalContinuation, exceptionContinuation);
                    }
                    case 0: {
                        return new CirCall(CirSnippet.get(Snippet.CreateArithmeticException.SNIPPET), exceptionContinuation, exceptionContinuation);
                    }
                    case 1: {
                        return new CirCall(normalContinuation, dividendValue);
                    }
                    default: {
                        break;
                    }
                }
            }
            assert dividendValue.value().isZero();
            return new CirCall(normalContinuation, dividendValue);
        }
    }

    public static final class LongDivided extends CirStrengthReducible {
        @HOSTED_ONLY
        public LongDivided() {
            super(JavaBuiltin.LongDivided.BUILTIN);
        }

        @Override
        public boolean isReducible(CirOptimizer cirOptimizer, CirValue[] arguments) {
            if (arguments[1].isScalarConstant()) {
                final long divisor = arguments[1].value().toInt();
                // TODO: strength reduction appears (to the untrained eye) to be possible for power of 2 divisors,
                // but due to rounding of negative values this is NOT exactly equivalent to an arithmetic shift right.
                // revisit in the future.
                if (divisor == 0L || divisor == -1L || divisor == 1L) {
                    return true;
                }
//                if (MaxineVM.isHosted() && Longs.isPowerOfTwoOrZero(divisor) && MaxineVM.isMaxineClass(cirOptimizer.classMethodActor().holder())) {
//                    ProgramWarning.message(cirOptimizer.classMethodActor().format("%H.%n(%p): Consider replacing division by " + divisor +
//                        " with use of " + Unsigned.class + " or a shift operation"));
//                }
            }
            if (arguments[0].isScalarConstant()) {
                final Value dividendValue = arguments[0].value();
                if (dividendValue.isZero()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
            final CirValue dividendValue = arguments[0];
            final CirValue divisorValue = arguments[1];
            final CirValue normalContinuation = arguments[2];
            final CirValue exceptionContinuation = arguments[3];
            if (divisorValue.isScalarConstant()) {
                final long d = divisorValue.value().toInt();
                if (d == -1L) {
                    return new CirCall(CirBuiltin.get(JavaBuiltin.LongNegated.BUILTIN), dividendValue, normalContinuation, exceptionContinuation);
                }
                if (d == 0L) {
                    return new CirCall(CirSnippet.get(Snippet.CreateArithmeticException.SNIPPET), exceptionContinuation, exceptionContinuation);
                }
                if (d == 1L) {
                    return new CirCall(normalContinuation, dividendValue);
                }
            }
            assert dividendValue.value().isZero();
            return new CirCall(normalContinuation, dividendValue);
        }
    }

    public static final class AddressDividedByAddress extends CirStrengthReducible {
        @HOSTED_ONLY
        public AddressDividedByAddress() {
            super(AddressBuiltin.DividedByAddress.BUILTIN);
        }

        @Override
        public boolean isReducible(CirOptimizer cirOptimizer, CirValue[] arguments) {
            if (arguments[1].isScalarConstant()) {
                final Address divisor = arguments[1].value().toWord().asAddress();
                return Longs.isPowerOfTwoOrZero(divisor.toLong());
            }
            if (arguments[0].isScalarConstant()) {
                final Value dividendValue = arguments[0].value();
                if (dividendValue.isZero()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
            final CirValue dividendValue = arguments[0];
            final CirValue divisorValue = arguments[1];
            final CirValue normalContinuation = arguments[2];
            final CirValue exceptionContinuation = arguments[3];
            if (divisorValue.isScalarConstant()) {
                final Address divisor = divisorValue.value().toWord().asAddress();
                if (divisor.equals(Address.zero())) {
                    return new CirCall(CirSnippet.get(Snippet.CreateArithmeticException.SNIPPET), exceptionContinuation, exceptionContinuation);
                }
                final int shift = Long.numberOfTrailingZeros(divisor.toLong());
                if (shift == 0) {
                    return new CirCall(normalContinuation, dividendValue);
                }
                final Builtin bltin = (Word.width() == 64) ? JavaBuiltin.LongUnsignedShiftedRight.BUILTIN : JavaBuiltin.IntUnsignedShiftedRight.BUILTIN;
                return new CirCall(CirBuiltin.get(bltin), dividendValue, new CirConstant(IntValue.from(shift)), normalContinuation, exceptionContinuation);
            }
            assert dividendValue.value().isZero();
            return new CirCall(normalContinuation, dividendValue);
        }
    }

    public static final class AddressDividedByInt extends CirStrengthReducible {
        @HOSTED_ONLY
        public AddressDividedByInt() {
            super(AddressBuiltin.DividedByInt.BUILTIN);
        }

        @Override
        public boolean isReducible(CirOptimizer cirOptimizer, CirValue[] arguments) {
            if (arguments[1].isScalarConstant()) {
                final int divisor = arguments[1].value().toInt();
                return Ints.isPowerOfTwoOrZero(divisor);
            }
            if (arguments[0].isScalarConstant()) {
                final Value dividendValue = arguments[0].value();
                if (dividendValue.isZero()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
            final CirValue dividendValue = arguments[0];
            final CirValue divisorValue = arguments[1];
            final CirValue normalContinuation = arguments[2];
            final CirValue exceptionContinuation = arguments[3];
            if (divisorValue.isScalarConstant()) {
                final int divisor = divisorValue.value().toInt();
                if (divisor == 0) {
                    return new CirCall(CirSnippet.get(Snippet.CreateArithmeticException.SNIPPET), exceptionContinuation, exceptionContinuation);
                }
                if (divisor == 1) {
                    return new CirCall(normalContinuation, dividendValue);
                }
                final int shift = Integer.numberOfTrailingZeros(divisor);
                final Builtin bltin = (Word.width() == 64) ? JavaBuiltin.LongUnsignedShiftedRight.BUILTIN : JavaBuiltin.IntUnsignedShiftedRight.BUILTIN;
                return new CirCall(CirBuiltin.get(bltin), dividendValue, new CirConstant(IntValue.from(shift)), normalContinuation, exceptionContinuation);
            }
            assert dividendValue.value().isZero();
            return new CirCall(normalContinuation, dividendValue);
        }
    }

    public static final class IntRemainder extends CirStrengthReducible {
        @HOSTED_ONLY
        public IntRemainder() {
            super(JavaBuiltin.IntRemainder.BUILTIN);
        }

        @Override
        public boolean isReducible(CirOptimizer cirOptimizer, CirValue[] arguments) {
            if (arguments[1].isScalarConstant()) {
                final int divisor = arguments[1].value().toInt();
                if (divisor == -1 || Ints.isPowerOfTwoOrZero(divisor)) {
                    return true;
                }
            }
            if (arguments[0].isScalarConstant()) {
                final Value dividendValue = arguments[0].value();
                if (dividendValue.isZero()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
            final CirValue dividendValue = arguments[0];
            final CirValue divisorValue = arguments[1];
            final CirValue normalContinuation = arguments[2];
            final CirValue exceptionContinuation = arguments[3];
            if (divisorValue.isScalarConstant()) {
                final int divisor = divisorValue.value().toInt();
                switch (divisor) {
                    case -1:
                    case 1 : {
                        return new CirCall(normalContinuation, new CirConstant(IntValue.ZERO));
                    }
                    case 0: {
                        return new CirCall(CirSnippet.get(Snippet.CreateArithmeticException.SNIPPET), exceptionContinuation, exceptionContinuation);
                    }
                    default: {
                        final int mask = divisor - 1;
                        return new CirCall(CirBuiltin.get(JavaBuiltin.IntAnd.BUILTIN), dividendValue, new CirConstant(IntValue.from(mask)), normalContinuation, exceptionContinuation);
                    }
                }
            }
            assert dividendValue.value().isZero();
            return new CirCall(normalContinuation, dividendValue);
        }
    }

    public static final class LongRemainder extends CirStrengthReducible {
        @HOSTED_ONLY
        public LongRemainder() {
            super(JavaBuiltin.LongRemainder.BUILTIN);
        }

        @Override
        public boolean isReducible(CirOptimizer cirOptimizer, CirValue[] arguments) {
            if (arguments[1].isScalarConstant()) {
                final long divisor = arguments[1].value().toLong();
                return divisor == -1L || Longs.isPowerOfTwoOrZero(divisor);
            }
            if (arguments[0].isScalarConstant()) {
                final Value dividendValue = arguments[0].value();
                if (dividendValue.isZero()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
            final CirValue dividendValue = arguments[0];
            final CirValue divisorValue = arguments[1];
            final CirValue normalContinuation = arguments[2];
            final CirValue exceptionContinuation = arguments[3];
            if (divisorValue.isScalarConstant()) {
                final long d = divisorValue.value().toLong();
                if (d == -1L || d == 1L) {
                    return new CirCall(normalContinuation, new CirConstant(LongValue.ZERO));
                }
                if (d == 0L) {
                    return new CirCall(CirSnippet.get(Snippet.CreateArithmeticException.SNIPPET), exceptionContinuation, exceptionContinuation);
                }
                final long mask = d - 1L;
                return new CirCall(CirBuiltin.get(JavaBuiltin.LongAnd.BUILTIN), dividendValue, new CirConstant(LongValue.from(mask)), normalContinuation, exceptionContinuation);
            }
            assert dividendValue.value().isZero();
            return new CirCall(normalContinuation, dividendValue);
        }
    }

    public static final class AddressRemainderByAddress extends CirStrengthReducible {
        @HOSTED_ONLY
        public AddressRemainderByAddress() {
            super(AddressBuiltin.RemainderByAddress.BUILTIN);
        }

        @Override
        public boolean isReducible(CirOptimizer cirOptimizer, CirValue[] arguments) {
            if (arguments[1].isScalarConstant()) {
                final Address divisor = arguments[1].value().toWord().asAddress();
                return Longs.isPowerOfTwoOrZero(divisor.toLong());
            }
            if (arguments[0].isScalarConstant()) {
                final Value dividendValue = arguments[0].value();
                if (dividendValue.isZero()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
            final CirValue dividendValue = arguments[0];
            final CirValue divisorValue = arguments[1];
            final CirValue normalContinuation = arguments[2];
            final CirValue exceptionContinuation = arguments[3];
            if (divisorValue.isScalarConstant()) {
                final Address divisor = divisorValue.value().toWord().asAddress();
                if (divisor.equals(Address.zero())) {
                    return new CirCall(CirSnippet.get(Snippet.CreateArithmeticException.SNIPPET), exceptionContinuation, exceptionContinuation);
                }
                if (divisor.equals(Address.fromInt(1))) {
                    return new CirCall(normalContinuation, new CirConstant(WordValue.ZERO));
                }
                if (Word.width() == 64) {
                    final long mask = divisor.toLong() - 1L;
                    return new CirCall(CirBuiltin.get(JavaBuiltin.LongAnd.BUILTIN), dividendValue, new CirConstant(LongValue.from(mask)), normalContinuation, exceptionContinuation);
                }
                final int mask = divisor.toInt() - 1;
                return new CirCall(CirBuiltin.get(JavaBuiltin.IntAnd.BUILTIN), dividendValue, new CirConstant(IntValue.from(mask)), normalContinuation, exceptionContinuation);
            }
            assert dividendValue.value().isZero();
            return new CirCall(normalContinuation, dividendValue);
        }
    }

    public static final class AddressRemainderByInt extends CirStrengthReducible {
        @HOSTED_ONLY
        public AddressRemainderByInt() {
            super(AddressBuiltin.RemainderByInt.BUILTIN);
        }

        @Override
        public boolean isReducible(CirOptimizer cirOptimizer, CirValue[] arguments) {
            if (arguments[1].isScalarConstant()) {
                final int divisor = arguments[1].value().toInt();
                return Ints.isPowerOfTwoOrZero(divisor);
            }
            if (arguments[0].isScalarConstant()) {
                final Value dividendValue = arguments[0].value();
                if (dividendValue.isZero()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
            final CirValue dividendValue = arguments[0];
            final CirValue divisorValue = arguments[1];
            final CirValue normalContinuation = arguments[2];
            final CirValue exceptionContinuation = arguments[3];
            if (divisorValue.isScalarConstant()) {
                final int divisor = divisorValue.value().toInt();
                if (divisor == 0) {
                    return new CirCall(CirSnippet.get(Snippet.CreateArithmeticException.SNIPPET), exceptionContinuation, exceptionContinuation);
                }
                if (divisor == 1) {
                    return new CirCall(normalContinuation, new CirConstant(IntValue.ZERO));
                }
                final CirConstant mask = new CirConstant(IntValue.from(divisor - 1));
                if (Word.width() == 64) {
                    final CirVariable intDividend = cirOptimizer.cirGenerator().postTranslationVariableFactory().createTemporary(Kind.INT);
                    final CirContinuation continuation = new CirContinuation(intDividend);
                    continuation.setBody(new CirCall(CirBuiltin.get(JavaBuiltin.IntAnd.BUILTIN), intDividend, mask, normalContinuation, exceptionContinuation));
                    return new CirCall(CirBuiltin.get(JavaBuiltin.ConvertLongToInt.BUILTIN), dividendValue, continuation, exceptionContinuation);
                }
                return new CirCall(CirBuiltin.get(JavaBuiltin.IntAnd.BUILTIN), dividendValue, mask, normalContinuation, exceptionContinuation);
            }
            assert dividendValue.value().isZero();
            return new CirCall(normalContinuation, dividendValue);
        }
    }

    private abstract static class BinaryLogic extends Commutative {
        @HOSTED_ONLY
        protected BinaryLogic(Builtin builtin) {
            super(builtin);
        }

        @Override
        protected boolean isReducible(CirValue argument) {
            if (argument.isScalarConstant()) {
                final Value a = argument.value();
                return a.isZero() || a.isAllOnes();
            }
            return false;
        }

        @Override
        public boolean isReducible(CirOptimizer cirOptimizer, CirValue[] arguments) {
            if (arguments[0] == arguments[1]) {
                return true;
            }
            return super.isReducible(cirOptimizer, arguments);
        }
    }

    private abstract static class SimpleBinaryLogic extends BinaryLogic {
        @HOSTED_ONLY
        protected SimpleBinaryLogic(Builtin builtin) {
            super(builtin);
        }

        @Override
        public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
            if (arguments[0] == arguments[1]) {
                final CirValue normalContinuation = arguments[2];
                return new CirCall(normalContinuation, arguments[0]);
            }
            return super.reduce(cirOptimizer, arguments);
        }
    }

    public static final class IntAnd extends SimpleBinaryLogic {
        @HOSTED_ONLY
        public IntAnd() {
            super(JavaBuiltin.IntAnd.BUILTIN);
        }

        @Override
        protected CirCall reduce(CirValue variable, CirValue constant, CirValue normalContinuation, CirValue exceptionContinuation) {
            final int a = constant.value().toInt();
            if (a == 0) {
                return new CirCall(normalContinuation, new CirConstant(IntValue.ZERO));
            }
            assert a == -1;
            return new CirCall(normalContinuation, variable);
        }
    }

    public static final class LongAnd extends SimpleBinaryLogic  {
        @HOSTED_ONLY
        public LongAnd() {
            super(JavaBuiltin.LongAnd.BUILTIN);
        }

        @Override
        protected CirCall reduce(CirValue variable, CirValue constant, CirValue normalContinuation, CirValue exceptionContinuation) {
            final long a = constant.value().toLong();
            if (a == 0L) {
                return new CirCall(normalContinuation, new CirConstant(LongValue.ZERO));
            }
            assert a == -1L;
            return new CirCall(normalContinuation, variable);
        }

        @Override
        public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
            if (arguments[0] == arguments[1]) {
                final CirValue normalContinuation = arguments[2];
                return new CirCall(normalContinuation, arguments[0]);
            }
            return super.reduce(cirOptimizer, arguments);
        }
    }

    public static final class IntOr extends SimpleBinaryLogic {
        @HOSTED_ONLY
        public IntOr() {
            super(JavaBuiltin.IntOr.BUILTIN);
        }

        @Override
        protected CirCall reduce(CirValue variable, CirValue constant, CirValue normalContinuation, CirValue exceptionContinuation) {
            final int a = constant.value().toInt();
            if (a == 0) {
                return new CirCall(normalContinuation, variable);
            }
            assert a == -1;
            return new CirCall(normalContinuation, new CirConstant(IntValue.MINUS_ONE));
        }
    }

    public static final class LongOr extends SimpleBinaryLogic {
        @HOSTED_ONLY
        public LongOr() {
            super(JavaBuiltin.LongOr.BUILTIN);
        }

        @Override
        protected CirCall reduce(CirValue variable, CirValue constant, CirValue normalContinuation, CirValue exceptionContinuation) {
            final long a = constant.value().toLong();
            if (a == 0L) {
                return new CirCall(normalContinuation, variable);
            }
            assert a == -1L;
            return new CirCall(normalContinuation, new CirConstant(LongValue.MINUS_ONE));
        }
    }

    public static final class IntXor extends BinaryLogic {
        @HOSTED_ONLY
        public IntXor() {
            super(JavaBuiltin.IntXor.BUILTIN);
        }

        @Override
        protected CirCall reduce(CirValue variable, CirValue constant, CirValue normalContinuation, CirValue exceptionContinuation) {
            final int a = constant.value().toInt();
            if (a == 0) {
                return new CirCall(normalContinuation, variable);
            }
            assert a == -1;
            return new CirCall(CirBuiltin.get(JavaBuiltin.IntNot.BUILTIN), variable, normalContinuation, exceptionContinuation);
        }

        @Override
        public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
            if (arguments[0] == arguments[1]) {
                final CirValue normalContinuation = arguments[2];
                return new CirCall(normalContinuation, new CirConstant(IntValue.ZERO));
            }
            return super.reduce(cirOptimizer, arguments);
        }
    }

    public static final class LongXor extends BinaryLogic {
        @HOSTED_ONLY
        public LongXor() {
            super(JavaBuiltin.LongXor.BUILTIN);
        }

        @Override
        protected CirCall reduce(CirValue variable, CirValue constant, CirValue normalContinuation, CirValue exceptionContinuation) {
            final long a = constant.value().toLong();
            if (a == 0L) {
                return new CirCall(normalContinuation, variable);
            }
            assert a == -1L;
            return new CirCall(CirBuiltin.get(JavaBuiltin.LongNot.BUILTIN), variable, normalContinuation, exceptionContinuation);
        }

        @Override
        public CirCall reduce(CirOptimizer cirOptimizer, CirValue... arguments) {
            if (arguments[0] == arguments[1]) {
                final CirValue normalContinuation = arguments[2];
                return new CirCall(normalContinuation, new CirConstant(LongValue.ZERO));
            }
            return super.reduce(cirOptimizer, arguments);
        }
    }
}
