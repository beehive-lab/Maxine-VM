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

import com.sun.max.vm.compiler.builtin.AddressBuiltin.*;
import com.sun.max.vm.compiler.builtin.IEEE754Builtin.*;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.*;
import com.sun.max.vm.compiler.builtin.PointerAtomicBuiltin.*;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.*;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.*;
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

    void visitReadByte(ReadByte builtin, IR_Type result, IR_Type[] arguments);
    void visitReadByteAtIntOffset(ReadByteAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitGetByte(GetByte builtin, IR_Type result, IR_Type[] arguments);
    void visitReadShort(ReadShort builtin, IR_Type result, IR_Type[] arguments);
    void visitReadShortAtIntOffset(ReadShortAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitGetShort(GetShort builtin, IR_Type result, IR_Type[] arguments);
    void visitReadChar(ReadChar builtin, IR_Type result, IR_Type[] arguments);
    void visitReadCharAtIntOffset(ReadCharAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitGetChar(GetChar builtin, IR_Type result, IR_Type[] arguments);
    void visitReadInt(ReadInt builtin, IR_Type result, IR_Type[] arguments);
    void visitReadIntAtIntOffset(ReadIntAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitGetInt(GetInt builtin, IR_Type result, IR_Type[] arguments);
    void visitReadFloat(ReadFloat builtin, IR_Type result, IR_Type[] arguments);
    void visitReadFloatAtIntOffset(ReadFloatAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitGetFloat(GetFloat builtin, IR_Type result, IR_Type[] arguments);
    void visitReadLong(ReadLong builtin, IR_Type result, IR_Type[] arguments);
    void visitReadLongAtIntOffset(ReadLongAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitGetLong(GetLong builtin, IR_Type result, IR_Type[] arguments);
    void visitReadDouble(ReadDouble builtin, IR_Type result, IR_Type[] arguments);
    void visitReadDoubleAtIntOffset(ReadDoubleAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitGetDouble(GetDouble builtin, IR_Type result, IR_Type[] arguments);
    void visitReadWord(ReadWord builtin, IR_Type result, IR_Type[] arguments);
    void visitReadWordAtIntOffset(ReadWordAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitGetWord(GetWord builtin, IR_Type result, IR_Type[] arguments);
    void visitReadReference(ReadReference builtin, IR_Type result, IR_Type[] arguments);
    void visitReadReferenceAtIntOffset(ReadReferenceAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitGetReference(GetReference builtin, IR_Type result, IR_Type[] arguments);

    void visitWriteByte(WriteByte builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteByteAtIntOffset(WriteByteAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitSetByte(SetByte builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteShort(WriteShort builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteShortAtIntOffset(WriteShortAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitSetShort(SetShort builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteInt(WriteInt builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteIntAtIntOffset(WriteIntAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitSetInt(SetInt builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteFloat(WriteFloat builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteFloatAtIntOffset(WriteFloatAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitSetFloat(SetFloat builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteLong(WriteLong builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteLongAtIntOffset(WriteLongAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitSetLong(SetLong builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteDouble(WriteDouble builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteDoubleAtIntOffset(WriteDoubleAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitSetDouble(SetDouble builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteWord(WriteWord builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteWordAtIntOffset(WriteWordAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitSetWord(SetWord builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteReference(WriteReference builtin, IR_Type result, IR_Type[] arguments);
    void visitWriteReferenceAtIntOffset(WriteReferenceAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitSetReference(SetReference builtin, IR_Type result, IR_Type[] arguments);

    void visitMostSignificantBit(MostSignificantBit builtin, IR_Type result, IR_Type[] arguments);
    void visitLeastSignificantBit(LeastSignificantBit builtin, IR_Type result, IR_Type[] arguments);


    void visitCompareAndSwapIntAtIntOffset(CompareAndSwapIntAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitCompareAndSwapInt(CompareAndSwapInt builtin, IR_Type result, IR_Type[] arguments);
    void visitCompareAndSwapWordAtIntOffset(CompareAndSwapWordAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitCompareAndSwapWord(CompareAndSwapWord builtin, IR_Type result, IR_Type[] arguments);
    void visitCompareAndSwapReferenceAtIntOffset(CompareAndSwapReferenceAtIntOffset builtin, IR_Type result, IR_Type[] arguments);
    void visitCompareAndSwapReference(CompareAndSwapReference builtin, IR_Type result, IR_Type[] arguments);

    void visitGetIntegerRegister(GetIntegerRegister builtin, IR_Type result, IR_Type[] arguments);
    void visitSetIntegerRegister(SetIntegerRegister builtin, IR_Type result, IR_Type[] arguments);
    void visitIncrementIntegerRegister(IncrementIntegerRegister builtin, IR_Type result, IR_Type[] arguments);
    void visitPause(Pause builtin, IR_Type result, IR_Type[] arguments);

    void visitCall(Call builtin, IR_Type result, IR_Type[] arguments);

    void visitCompareInts(CompareInts builtin, IR_Type result, IR_Type[] arguments);
    void visitAboveEqual(AboveEqual builtin, IR_Type result, IR_Type[] arguments);
    void visitAboveThan(AboveThan builtin, IR_Type result, IR_Type[] arguments);
    void visitBelowEqual(BelowEqual builtin, IR_Type result, IR_Type[] arguments);
    void visitBelowThan(BelowThan builtin, IR_Type result, IR_Type[] arguments);
    void visitCompareWords(CompareWords builtin, IR_Type result, IR_Type[] arguments);

    void visitBarMemory(BarMemory builtin, IR_Type result, IR_Type[] arguments);

    void visitMakeStackVariable(MakeStackVariable builtin, IR_Type result, IR_Type[] arguments);
    void visitStackAllocate(StackAllocate builtin, IR_Type result, IR_Type[] arguments);

    void visitInfopoint(InfopointBuiltin builtin, IR_Type result, IR_Type[] arguments);

    void visitFlushRegisterWindows(FlushRegisterWindows builtin, IR_Type result, IR_Type[] arguments);

    void visitIntToFloat(IntToFloat builtin, IR_Type result, IR_Type[] arguments);
    void visitFloatToInt(FloatToInt builtin, IR_Type result, IR_Type[] arguments);
    void visitLongToDouble(LongToDouble builtin, IR_Type result, IR_Type[] arguments);
    void visitDoubleToLong(DoubleToLong builtin, IR_Type result, IR_Type[] arguments);
}
