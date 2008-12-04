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

import com.sun.max.vm.compiler.builtin.AddressBuiltin.*;
import com.sun.max.vm.compiler.builtin.IEEE754Builtin.*;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.*;
import com.sun.max.vm.compiler.builtin.PointerAtomicBuiltin.*;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.*;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.*;
import com.sun.max.vm.compiler.builtin.SafepointBuiltin.*;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.*;

public interface BuiltinVisitor<IR_Type> {

    void visitIntNegated(IntNegated builtin, IR_Type result, IR_Type[] arguments);
    void visitFloatNegated(FloatNegated builtin, IR_Type result, IR_Type[] arguments);
    void visitLongNegated(LongNegated builtin, IR_Type result, IR_Type[] arguments);
    void visitDoubleNegated(DoubleNegated builtin, IR_Type result, IR_Type[] arguments);

    void visitIntPlus(IntPlus builtin, IR_Type result, IR_Type[] arguments);
    void visitFloatPlus(FloatPlus builtin, IR_Type result, IR_Type[] arguments);
    void visitLongPlus(LongPlus builtin, IR_Type result, IR_Type[] arguments);
    void visitDoublePlus(DoublePlus builtin, IR_Type result, IR_Type[] arguments);

    void visitIntMinus(IntMinus builtin, IR_Type result, IR_Type[] arguments);
    void visitFloatMinus(FloatMinus builtin, IR_Type result, IR_Type[] arguments);
    void visitLongMinus(LongMinus builtin, IR_Type result, IR_Type[] arguments);
    void visitDoubleMinus(DoubleMinus builtin, IR_Type result, IR_Type[] arguments);

    void visitIntTimes(IntTimes builtin, IR_Type result, IR_Type[] arguments);
    void visitFloatTimes(FloatTimes builtin, IR_Type result, IR_Type[] arguments);
    void visitLongTimes(LongTimes builtin, IR_Type result, IR_Type[] arguments);
    void visitDoubleTimes(DoubleTimes builtin, IR_Type result, IR_Type[] arguments);

    void visitIntDivided(IntDivided builtin, IR_Type result, IR_Type[] arguments);
    void visitFloatDivided(FloatDivided builtin, IR_Type result, IR_Type[] arguments);
    void visitLongDivided(LongDivided builtin, IR_Type result, IR_Type[] arguments);
    void visitDoubleDivided(DoubleDivided builtin, IR_Type result, IR_Type[] arguments);

    void visitIntRemainder(IntRemainder builtin, IR_Type result, IR_Type[] arguments);
    void visitFloatRemainder(FloatRemainder builtin, IR_Type result, IR_Type[] arguments);
    void visitLongRemainder(LongRemainder builtin, IR_Type result, IR_Type[] arguments);
    void visitDoubleRemainder(DoubleRemainder builtin, IR_Type result, IR_Type[] arguments);

    void visitIntShiftedLeft(IntShiftedLeft builtin, IR_Type result, IR_Type[] arguments);
    void visitLongShiftedLeft(LongShiftedLeft builtin, IR_Type result, IR_Type[] arguments);

    void visitIntSignedShiftedRight(IntSignedShiftedRight builtin, IR_Type result, IR_Type[] arguments);
    void visitLongSignedShiftedRight(LongSignedShiftedRight builtin, IR_Type result, IR_Type[] arguments);

    void visitIntUnsignedShiftedRight(IntUnsignedShiftedRight builtin, IR_Type result, IR_Type[] arguments);
    void visitLongUnsignedShiftedRight(LongUnsignedShiftedRight builtin, IR_Type result, IR_Type[] arguments);

    void visitIntNot(IntNot builtin, IR_Type result, IR_Type[] arguments);
    void visitLongNot(LongNot builtin, IR_Type result, IR_Type[] arguments);
    void visitIntAnd(IntAnd builtin, IR_Type result, IR_Type[] arguments);
    void visitLongAnd(LongAnd builtin, IR_Type result, IR_Type[] arguments);
    void visitIntOr(IntOr builtin, IR_Type result, IR_Type[] arguments);
    void visitLongOr(LongOr builtin, IR_Type result, IR_Type[] arguments);
    void visitIntXor(IntXor builtin, IR_Type result, IR_Type[] arguments);
    void visitLongXor(LongXor builtin, IR_Type result, IR_Type[] arguments);

    void visitLongCompare(LongCompare builtin, IR_Type result, IR_Type[] arguments);
    void visitFloatCompareL(FloatCompareL builtin, IR_Type result, IR_Type[] arguments);
    void visitFloatCompareG(FloatCompareG builtin, IR_Type result, IR_Type[] arguments);
    void visitDoubleCompareL(DoubleCompareL builtin, IR_Type result, IR_Type[] arguments);
    void visitDoubleCompareG(DoubleCompareG builtin, IR_Type result, IR_Type[] arguments);

    void visitConvertByteToInt(ConvertByteToInt builtin, IR_Type result, IR_Type[] arguments);
    void visitConvertCharToInt(ConvertCharToInt builtin, IR_Type result, IR_Type[] arguments);
    void visitConvertShortToInt(ConvertShortToInt builtin, IR_Type result, IR_Type[] arguments);
    void visitConvertIntToByte(ConvertIntToByte builtin, IR_Type result, IR_Type[] arguments);
    void visitConvertIntToChar(ConvertIntToChar builtin, IR_Type result, IR_Type[] arguments);
    void visitConvertIntToShort(ConvertIntToShort builtin, IR_Type result, IR_Type[] arguments);
    void visitConvertIntToFloat(ConvertIntToFloat builtin, IR_Type result, IR_Type[] arguments);
    void visitConvertIntToLong(ConvertIntToLong builtin, IR_Type result, IR_Type[] arguments);
    void visitConvertIntToDouble(ConvertIntToDouble builtin, IR_Type result, IR_Type[] arguments);
    void visitConvertFloatToDouble(ConvertFloatToDouble builtin, IR_Type result, IR_Type[] arguments);
    void visitConvertLongToInt(ConvertLongToInt builtin, IR_Type result, IR_Type[] arguments);
    void visitConvertLongToFloat(ConvertLongToFloat builtin, IR_Type result, IR_Type[] arguments);
    void visitConvertLongToDouble(ConvertLongToDouble builtin, IR_Type result, IR_Type[] arguments);
    void visitConvertDoubleToFloat(ConvertDoubleToFloat builtin, IR_Type result, IR_Type[] arguments);

    void visitConvertFloatToInt(ConvertFloatToInt builtin, IR_Type result, IR_Type[] arguments);
    void visitConvertFloatToLong(ConvertFloatToLong builtin, IR_Type result, IR_Type[] arguments);
    void visitConvertDoubleToInt(ConvertDoubleToInt builtin, IR_Type result, IR_Type[] arguments);
    void visitConvertDoubleToLong(ConvertDoubleToLong builtin, IR_Type result, IR_Type[] arguments);

    void visitLessEqual(LessEqual builtin, IR_Type result, IR_Type[] arguments);
    void visitLessThan(LessThan builtin, IR_Type result, IR_Type[] arguments);
    void visitGreaterEqual(GreaterEqual builtin, IR_Type result, IR_Type[] arguments);
    void visitGreaterThan(GreaterThan builtin, IR_Type result, IR_Type[] arguments);
    void visitDividedByAddress(DividedByAddress builtin, IR_Type result, IR_Type[] arguments);
    void visitDividedByInt(DividedByInt builtin, IR_Type result, IR_Type[] arguments);
    void visitRemainderByAddress(RemainderByAddress builtin, IR_Type result, IR_Type[] arguments);
    void visitRemainderByInt(RemainderByInt builtin, IR_Type result, IR_Type[] arguments);

    void visitReadByteAtLongOffset(ReadByteAtLongOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitReadByteAtIntOffset(ReadByteAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitGetByte(GetByte builtin, IR_Type result, IR_Type[] arguments);
    void visitReadShortAtLongOffset(ReadShortAtLongOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitReadShortAtIntOffset(ReadShortAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitGetShort(GetShort builtin, IR_Type result, IR_Type[] arguments);
    void visitReadCharAtLongOffset(ReadCharAtLongOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitReadCharAtIntOffset(ReadCharAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitGetChar(GetChar builtin, IR_Type result, IR_Type[] arguments);
    void visitReadIntAtLongOffset(ReadIntAtLongOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitReadIntAtIntOffset(ReadIntAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitGetInt(GetInt builtin, IR_Type result, IR_Type[] arguments);
    void visitReadFloatAtLongOffset(ReadFloatAtLongOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitReadFloatAtIntOffset(ReadFloatAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitGetFloat(GetFloat builtin, IR_Type result, IR_Type[] arguments);
    void visitReadLongAtLongOffset(ReadLongAtLongOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitReadLongAtIntOffset(ReadLongAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitGetLong(GetLong builtin, IR_Type result, IR_Type[] arguments);
    void visitReadDoubleAtLongOffset(ReadDoubleAtLongOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitReadDoubleAtIntOffset(ReadDoubleAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitGetDouble(GetDouble builtin, IR_Type result, IR_Type[] arguments);
    void visitReadWordAtLongOffset(ReadWordAtLongOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitReadWordAtIntOffset(ReadWordAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitGetWord(GetWord builtin, IR_Type result, IR_Type[] arguments);
    void visitReadReferenceAtLongOffset(ReadReferenceAtLongOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitReadReferenceAtIntOffset(ReadReferenceAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitGetReference(GetReference builtin, IR_Type result, IR_Type[] arguments);

    void visitWriteByteAtLongOffset(WriteByteAtLongOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteByteAtIntOffset(WriteByteAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitSetByte(SetByte builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteShortAtLongOffset(WriteShortAtLongOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteShortAtIntOffset(WriteShortAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitSetShort(SetShort builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteIntAtLongOffset(WriteIntAtLongOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteIntAtIntOffset(WriteIntAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitSetInt(SetInt builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteFloatAtLongOffset(WriteFloatAtLongOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteFloatAtIntOffset(WriteFloatAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitSetFloat(SetFloat builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteLongAtLongOffset(WriteLongAtLongOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteLongAtIntOffset(WriteLongAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitSetLong(SetLong builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteDoubleAtLongOffset(WriteDoubleAtLongOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteDoubleAtIntOffset(WriteDoubleAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitSetDouble(SetDouble builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteWordAtLongOffset(WriteWordAtLongOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteWordAtIntOffset(WriteWordAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitSetWord(SetWord builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteReferenceAtLongOffset(WriteReferenceAtLongOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteReferenceAtIntOffset(WriteReferenceAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitSetReference(SetReference builtin, IR_Type result, IR_Type[] arguments);

    void visitCompareAndSwapIntAtIntOffset(CompareAndSwapIntAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitCompareAndSwapIntAtLongOffset(CompareAndSwapIntAtLongOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitCompareAndSwapWordAtIntOffset(CompareAndSwapWordAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitCompareAndSwapWordAtLongOffset(CompareAndSwapWordAtLongOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitCompareAndSwapReferenceAtIntOffset(CompareAndSwapReferenceAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitCompareAndSwapReferenceAtLongOffset(CompareAndSwapReferenceAtLongOffset builtin, IR_Type result, IR_Type[] arguments);

    void visitGetInstructionPointer(GetInstructionPointer builtin, IR_Type result, IR_Type[] arguments);
    void visitGetIntegerRegister(GetIntegerRegister builtin, IR_Type result, IR_Type[] arguments);
    void visitSetIntegerRegister(SetIntegerRegister builtin, IR_Type result, IR_Type[] arguments);
    void visitAddWordsToIntegerRegister(AddWordsToIntegerRegister builtin, IR_Type result, IR_Type[] arguments);
    void visitPush(Push builtin, IR_Type result, IR_Type[] arguments);
    void visitPop(Pop builtin, IR_Type result, IR_Type[] arguments);
    void visitPause(Pause builtin, IR_Type result, IR_Type[] arguments);

    void visitGetFloatingPointRegister(GetFloatingPointRegister builtin, IR_Type result, IR_Type[] arguments);

    void visitJump(Jump builtin, IR_Type result, IR_Type[] arguments);
    void visitCall(Call builtin, IR_Type result, IR_Type[] arguments);

    void visitCompareInts(CompareInts builtin, IR_Type result, IR_Type[] arguments);
    void visitUnsignedIntGreaterEqual(UnsignedIntGreaterEqual builtin, IR_Type result, IR_Type[] arguments);
    void visitCompareReferences(CompareReferences builtin, IR_Type result, IR_Type[] arguments);

    void visitBarMemory(BarMemory builtin, IR_Type result, IR_Type[] arguments);

    void visitMakeStackVariable(MakeStackVariable builtin, IR_Type result, IR_Type[] arguments);

    void visitSoftSafepoint(SoftSafepoint builtin, IR_Type result, IR_Type[] arguments);
    void visitHardSafepoint(HardSafepoint builtin, IR_Type result, IR_Type[] arguments);

    void visitBreakpoint(Breakpoint builtin, IR_Type result, IR_Type[] arguments);
    void visitFlushRegisterWindows(FlushRegisterWindows builtin, IR_Type result, IR_Type[] arguments);

    void visitIntToFloat(IntToFloat builtin, IR_Type result, IR_Type[] arguments);
    void visitFloatToInt(FloatToInt builtin, IR_Type result, IR_Type[] arguments);
    void visitLongToDouble(LongToDouble builtin, IR_Type result, IR_Type[] arguments);
    void visitDoubleToLong(DoubleToLong builtin, IR_Type result, IR_Type[] arguments);

    void visitMarker(Marker builtin, IR_Type result, IR_Type[] arguments);
}
