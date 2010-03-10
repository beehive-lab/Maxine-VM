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

import static com.sun.c1x.bytecode.Bytecodes.*;

import com.sun.c1x.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.VMRegister.*;

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
    @INTRINSIC(READREG)
    public static native Pointer getIntegerRegister(VMRegister.Role r);

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
    @INTRINSIC(WRITEREG)
    public static native Pointer setIntegerRegister(VMRegister.Role r, Word value);

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
     * @param numberOfWords the signed adjustment amount specified in words
     */
    @BUILTIN(AdjustJitStack.class)
    public static native void adjustJitStack(int numberOfWords);

    /**
     * @see SpecialBuiltin#adjustJitStack(int)
     */
    public static class AdjustJitStack extends SpecialBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitAdjustJitStack(this, result, arguments);
        }

        public static final AdjustJitStack BUILTIN = new AdjustJitStack();
    }

    /**
     * Gets the current value of the instruction pointer on the current thread.
     *
     * @return the address of the first instruction generated for this builtin
     */
    @BUILTIN(GetInstructionPointer.class)
    public static native Pointer getInstructionPointer();

    /**
     * @see SpecialBuiltin#getInstructionPointer()
     */
    public static class GetInstructionPointer extends SpecialBuiltin {

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 0;
            visitor.visitGetInstructionPointer(this, result, arguments);
        }

        public static final GetInstructionPointer BUILTIN = new GetInstructionPointer();
    }

    /**
     * Return the index to the least significant bit set in a word value.
     * @param value the word scan for the least significant bit
     * @return the index to the least significant bit within the specified word
     */
    @INTRINSIC(LSB)
    @BUILTIN(LeastSignificantBit.class)
    public static native int leastSignificantBit(Word value);

    public static class LeastSignificantBit extends SpecialBuiltin {
        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 1;
            visitor.visitLeastSignificantBit(this, result, arguments);
        }
        public static final LeastSignificantBit BUILTIN = new LeastSignificantBit();
    }

    /**
     * Return the index to the most significant bit set in a word value.
     * @param value the word scan for the most significant bit
     * @return the index to the most significant bit within the specified word
     */
    @INTRINSIC(MSB)
    @BUILTIN(MostSignificantBit.class)
    public static native int mostSignificantBit(Word value);

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
    @INTRINSIC(CALL)
    public static native void call();

    @BUILTIN(Call.class)
    @INTRINSIC(CALL)
    public static native float callFloat();

    @BUILTIN(Call.class)
    @INTRINSIC(CALL)
    public static native long callLong();

    @BUILTIN(Call.class)
    @INTRINSIC(CALL)
    public static native double callDouble();

    @BUILTIN(Call.class)
    @INTRINSIC(CALL)
    public static native Word callWord();

    @BUILTIN(Call.class)
    @INTRINSIC(CALL)
    public static native void call(Word address);

    @BUILTIN(Call.class)
    @INTRINSIC(CALL)
    public static native float callFloat(Word address);

    @BUILTIN(Call.class)
    @INTRINSIC(CALL)
    public static native long callLong(Word address);

    @BUILTIN(Call.class)
    @INTRINSIC(CALL)
    public static native double callDouble(Word address);

    @BUILTIN(Call.class)
    @INTRINSIC(CALL)
    public static native Word callWord(Word address);

    @BUILTIN(Call.class)
    @INTRINSIC(CALL)
    public static native void call(Word address, Object receiver);

    @BUILTIN(Call.class)
    @INTRINSIC(CALL)
    public static native float callFloat(Word address, Object receiver);

    @BUILTIN(Call.class)
    @INTRINSIC(CALL)
    public static native long callLong(Word address, Object receiver);

    @BUILTIN(Call.class)
    @INTRINSIC(CALL)
    public static native double callDouble(Word address, Object receiver);

    @BUILTIN(Call.class)
    @INTRINSIC(CALL)
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

    @BUILTIN(UnsignedIntGreaterEqual.class)
    @INTRINSIC(UGE)
    public static boolean unsignedIntGreaterEqual(int value1, int value2) {
        final long unsignedInt1 = value1 & 0xFFFFFFFFL;
        final long unsignedInt2 = value2 & 0xFFFFFFFFL;
        return unsignedInt1 >= unsignedInt2;
    }

    public static class UnsignedIntGreaterEqual extends SpecialBuiltin {

        @Override
        public final boolean hasSideEffects() {
            return false;
        }

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 2;
            visitor.visitUnsignedIntGreaterEqual(this, result, arguments);
        }

        public static final UnsignedIntGreaterEqual BUILTIN = new UnsignedIntGreaterEqual();
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

    public static class BarMemory extends SpecialBuiltin {

        public BarMemory() {
            super(MemoryBarrier.class);
        }

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

    /**
     * Marks the current position in a {@link TargetMethod}.
     */
    @BUILTIN(Marker.class)
    public static native void mark();

    public static class Marker extends SpecialBuiltin {

        @Override
        public boolean hasSideEffects() {
            return false;
        }

        @Override
        public <IR_Type> void acceptVisitor(BuiltinVisitor<IR_Type> visitor, IR_Type result, IR_Type[] arguments) {
            assert arguments.length == 0;
            visitor.visitMarker(this, result, arguments);
        }

        public static final Marker BUILTIN = new Marker();
    }

}
