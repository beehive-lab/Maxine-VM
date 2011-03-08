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
package com.sun.max.vm.compiler.builtin;

import static com.sun.cri.bytecode.Bytecodes.*;
import static com.sun.cri.bytecode.Bytecodes.UnsignedComparisons.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.VMRegister.Role;

/**
 * @author Bernd Mathiske
 */
public abstract class SpecialBuiltin extends Builtin {

    protected SpecialBuiltin() {
        super(SpecialBuiltin.class);
    }

    protected SpecialBuiltin(Class executableHolder) {
        super(executableHolder);
    }

    /**
     * Gets the value of a given integer register.
     *
     * @param r specifies the register to read
     * @return the value of the register specified by {@code r}
     */
    @BUILTIN(GetIntegerRegister.class)
    private static native Pointer getIntegerRegister(VMRegister.Role r);

    /**
     * @see SpecialBuiltin#getIntegerRegister(com.sun.max.vm.runtime.VMRegister.Role)
     */
    public static class GetIntegerRegister extends SpecialBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitGetIntegerRegister(this, result, arguments);
        }

        public static final GetIntegerRegister BUILTIN = new GetIntegerRegister();
    }

    /**
     * Writes a given value to a specified integer register.
     *
     * @param r specifies the register to update
     * @param the value to write to the register specified by {@code r}
     */
    @BUILTIN(SetIntegerRegister.class)
    private static native Pointer setIntegerRegister(VMRegister.Role r, Word value);

    /**
     * @see SpecialBuiltin#setIntegerRegister(Role, Word)
     */
    public static class SetIntegerRegister extends SpecialBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitSetIntegerRegister(this, result, arguments);
        }

        public static final SetIntegerRegister BUILTIN = new SetIntegerRegister();
    }

    /**
     * Adjusts the value register used as the JIT's stack pointer. The value of the register
     * adjusted by adding {@code numberOrWords * Word.size()} to its current value.
     *
     * @param delta the signed increment amount specified
     */
    @BUILTIN(IncrementIntegerRegister.class)
    public static native void incrementIntegerRegister(VMRegister.Role r, int delta);

    /**
     * @see SpecialBuiltin#incrementIntegerRegister(int)
     */
    public static class IncrementIntegerRegister extends SpecialBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitIncrementIntegerRegister(this, result, arguments);
        }

        public static final IncrementIntegerRegister BUILTIN = new IncrementIntegerRegister();
    }

    /**
     * Returns the index of the least significant bit set in a given value.
     *
     * @param value the value to scan for the least significant bit
     * @return the index of the least significant bit within {@code value} or {@code -1} if {@code value == 0}
     */
    @INTRINSIC(LSB)
    @BUILTIN(LeastSignificantBit.class)
    public static int leastSignificantBit(Word value) {
        long l = value.asAddress().toLong();
        if (l == 0) {
            return -1;
        }
        return Long.numberOfTrailingZeros(l);
    }

    public static class LeastSignificantBit extends SpecialBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitLeastSignificantBit(this, result, arguments);
        }
        public static final LeastSignificantBit BUILTIN = new LeastSignificantBit();
    }

    /**
     * Returns the index to the most significant bit set in a given value.
     *
     * @param value the value to scan for the most significant bit
     * @return the index to the most significant bit within {@code value} or {@code -1} if {@code value == 0}
     */
    @INTRINSIC(MSB)
    @BUILTIN(MostSignificantBit.class)
    public static int mostSignificantBit(Word value) {
        long l = value.asAddress().toLong();
        if (l == 0) {
            return -1;
        }
        return Long.numberOfTrailingZeros(l);
    }

    public static class MostSignificantBit extends SpecialBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitMostSignificantBit(this, result, arguments);
        }
        public static final MostSignificantBit BUILTIN = new MostSignificantBit();
    }

    /**
     * @see Pause
     */
    @BUILTIN(Pause.class)
    @INTRINSIC(PAUSE)
    public static native void pause();

    /**
     * If the CPU supports it, then this builtin issues an instruction that improves the performance of spin loops by
     * providing a hint to the processor that the current thread is in a spin loop. The processor may use this to
     * optimize power consumption while in the spin loop.
     *
     * If the CPU does not support such an instruction, then nothing is emitted for this builtin.
     */
    public static class Pause extends SpecialBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 0;
            visitor.visitPause(this, result, arguments);
        }

        public static final Pause BUILTIN = new Pause();
    }

    // The following native methods all map to the same builtin call. The purpose is to be able to
    // have different types of return values. The builtin itself is agnostic to the
    // return type, so we don't need to have one SpecialBuiltin class per returnType.

    @BUILTIN(Call.class)
    @INTRINSIC(TEMPLATE_CALL)
    public static native void call();

    @BUILTIN(Call.class)
    @INTRINSIC(TEMPLATE_CALL)
    public static native float callFloat();

    @BUILTIN(Call.class)
    @INTRINSIC(TEMPLATE_CALL)
    public static native long callLong();

    @BUILTIN(Call.class)
    @INTRINSIC(TEMPLATE_CALL)
    public static native double callDouble();

    @BUILTIN(Call.class)
    @INTRINSIC(TEMPLATE_CALL)
    public static native Word callWord();

    @BUILTIN(Call.class)
    @INTRINSIC(TEMPLATE_CALL)
    public static native void call(Word address);

    @BUILTIN(Call.class)
    @INTRINSIC(TEMPLATE_CALL)
    public static native float callFloat(Word address);

    @BUILTIN(Call.class)
    @INTRINSIC(TEMPLATE_CALL)
    public static native long callLong(Word address);

    @BUILTIN(Call.class)
    @INTRINSIC(TEMPLATE_CALL)
    public static native double callDouble(Word address);

    @BUILTIN(Call.class)
    @INTRINSIC(TEMPLATE_CALL)
    public static native Word callWord(Word address);

    @BUILTIN(Call.class)
    @INTRINSIC(TEMPLATE_CALL)
    public static native void call(Word address, Object receiver);

    @BUILTIN(Call.class)
    @INTRINSIC(TEMPLATE_CALL)
    public static native float callFloat(Word address, Object receiver);

    @BUILTIN(Call.class)
    @INTRINSIC(TEMPLATE_CALL)
    public static native long callLong(Word address, Object receiver);

    @BUILTIN(Call.class)
    @INTRINSIC(TEMPLATE_CALL)
    public static native double callDouble(Word address, Object receiver);

    @BUILTIN(Call.class)
    @INTRINSIC(TEMPLATE_CALL)
    public static native Word callWord(Word address, Object receiver);

    public static class Call extends SpecialBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 0 || arguments.length == 1 || arguments.length == 2;
            visitor.visitCall(this, result, arguments);
        }

        @Override
        public int reasonsMayStop() {
            return Stoppable.CALL_STOP;
        }
        public static final Call BUILTIN = new Call();
    }

    @BUILTIN(AboveEqual.class)
    @INTRINSIC(UCMP | (ABOVE_EQUAL << 8))
    public static boolean aboveEqual(int value1, int value2) {
        final long unsignedInt1 = value1 & 0xFFFFFFFFL;
        final long unsignedInt2 = value2 & 0xFFFFFFFFL;
        return unsignedInt1 >= unsignedInt2;
    }

    public static class AboveEqual extends SpecialBuiltin {

        @Override
        public final boolean hasSideEffects() {
            return false;
        }

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitAboveEqual(this, result, arguments);
        }

        public static final AboveEqual BUILTIN = new AboveEqual();
    }

    @BUILTIN(AboveThan.class)
    @INTRINSIC(UCMP | (ABOVE_THAN << 8))
    public static boolean aboveThan(int value1, int value2) {
        final long unsignedInt1 = value1 & 0xFFFFFFFFL;
        final long unsignedInt2 = value2 & 0xFFFFFFFFL;
        return unsignedInt1 > unsignedInt2;
    }

    public static class AboveThan extends SpecialBuiltin {

        @Override
        public final boolean hasSideEffects() {
            return false;
        }

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitAboveThan(this, result, arguments);
        }

        public static final AboveThan BUILTIN = new AboveThan();
    }

    @BUILTIN(BelowEqual.class)
    @INTRINSIC(UCMP | (BELOW_EQUAL << 8))
    public static boolean belowEqual(int value1, int value2) {
        final long unsignedInt1 = value1 & 0xFFFFFFFFL;
        final long unsignedInt2 = value2 & 0xFFFFFFFFL;
        return unsignedInt1 <= unsignedInt2;
    }

    public static class BelowEqual extends SpecialBuiltin {

        @Override
        public final boolean hasSideEffects() {
            return false;
        }

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitBelowEqual(this, result, arguments);
        }

        public static final BelowEqual BUILTIN = new BelowEqual();
    }

    @BUILTIN(BelowThan.class)
    @INTRINSIC(UCMP | (BELOW_THAN << 8))
    public static boolean belowThan(int value1, int value2) {
        final long unsignedInt1 = value1 & 0xFFFFFFFFL;
        final long unsignedInt2 = value2 & 0xFFFFFFFFL;
        return unsignedInt1 < unsignedInt2;
    }

    public static class BelowThan extends SpecialBuiltin {

        @Override
        public final boolean hasSideEffects() {
            return false;
        }

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitBelowThan(this, result, arguments);
        }

        public static final BelowThan BUILTIN = new BelowThan();
    }

    /**
     * A compare instruction modifying condition flags, without returning a value in a register or memory location.
     */
    @BUILTIN(CompareInts.class)
    @INTRINSIC(ICMP)
    public static native void compareInts(int value1, int value2);

    public static class CompareInts extends SpecialBuiltin {

        @Override
        public final boolean hasSideEffects() {
            return false;
        }

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitCompareInts(this, result, arguments);
        }

        public static final CompareInts BUILTIN = new CompareInts();
    }

    /**
     * A compare instruction modifying condition flags, without returning a value in a register or memory location.
     */
    @BUILTIN(CompareWords.class)
    @INTRINSIC(WCMP)
    public static native void compareWords(Word value1, Word value2);

    public static class CompareWords extends SpecialBuiltin {

        @Override
        public final boolean hasSideEffects() {
            return false;
        }

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitCompareWords(this, result, arguments);
        }

        public static final CompareWords BUILTIN = new CompareWords();
    }

    @BUILTIN(BarMemory.class)
    public static void barMemory(int barriers) {
    }

    public static class BarMemory extends SpecialBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitBarMemory(this, result, arguments);
        }

        public static final BarMemory BUILTIN = new BarMemory();
    }

    @BUILTIN(FlushRegisterWindows.class)
    @INTRINSIC(FLUSHW)
    public static native void flushRegisterWindows();

    public static class FlushRegisterWindows extends SpecialBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 0;
            visitor.visitFlushRegisterWindows(this, result, arguments);
        }

        public static final FlushRegisterWindows BUILTIN = new FlushRegisterWindows();
    }

    @BUILTIN(value = IntToFloat.class)
    @INTRINSIC(MOV_I2F)
    public static float intToFloat(int value) {
        return Float.intBitsToFloat(value);
    }

    public static class IntToFloat extends SpecialBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitIntToFloat(this, result, arguments);
        }

        public static final IntToFloat BUILTIN = new IntToFloat();
    }

    @BUILTIN(value = FloatToInt.class)
    @INTRINSIC(MOV_F2I)
    public static int floatToInt(float value) {
        return Float.floatToRawIntBits(value);
    }

    public static class FloatToInt extends SpecialBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitFloatToInt(this, result, arguments);
        }

        public static final FloatToInt BUILTIN = new FloatToInt();
    }

    @BUILTIN(value = LongToDouble.class)
    @INTRINSIC(MOV_L2D)
    public static double longToDouble(long value) {
        return Double.longBitsToDouble(value);
    }

    public static class LongToDouble extends SpecialBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitLongToDouble(this, result, arguments);
        }

        public static final LongToDouble BUILTIN = new LongToDouble();
    }

    @BUILTIN(value = DoubleToLong.class)
    @INTRINSIC(MOV_D2L)
    public static long doubleToLong(double value) {
        return Double.doubleToRawLongBits(value);
    }

    public static class DoubleToLong extends SpecialBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitDoubleToLong(this, result, arguments);
        }

        public static final DoubleToLong BUILTIN = new DoubleToLong();
    }
}
