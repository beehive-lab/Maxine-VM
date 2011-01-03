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

import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.builtin.AddressBuiltin.DividedByAddress;
import com.sun.max.vm.compiler.builtin.AddressBuiltin.DividedByInt;
import com.sun.max.vm.compiler.builtin.AddressBuiltin.GreaterEqual;
import com.sun.max.vm.compiler.builtin.AddressBuiltin.GreaterThan;
import com.sun.max.vm.compiler.builtin.AddressBuiltin.LessEqual;
import com.sun.max.vm.compiler.builtin.AddressBuiltin.LessThan;
import com.sun.max.vm.compiler.builtin.AddressBuiltin.RemainderByAddress;
import com.sun.max.vm.compiler.builtin.AddressBuiltin.RemainderByInt;
import com.sun.max.vm.compiler.builtin.IEEE754Builtin.ConvertDoubleToInt;
import com.sun.max.vm.compiler.builtin.IEEE754Builtin.ConvertDoubleToLong;
import com.sun.max.vm.compiler.builtin.IEEE754Builtin.ConvertFloatToInt;
import com.sun.max.vm.compiler.builtin.IEEE754Builtin.ConvertFloatToLong;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.ConvertByteToInt;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.ConvertCharToInt;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.ConvertDoubleToFloat;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.ConvertFloatToDouble;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.ConvertIntToByte;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.ConvertIntToChar;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.ConvertIntToDouble;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.ConvertIntToFloat;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.ConvertIntToLong;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.ConvertIntToShort;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.ConvertLongToDouble;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.ConvertLongToFloat;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.ConvertLongToInt;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.ConvertShortToInt;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.DoubleCompareG;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.DoubleCompareL;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.DoubleDivided;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.DoubleMinus;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.DoubleNegated;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.DoublePlus;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.DoubleRemainder;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.DoubleTimes;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.FloatCompareG;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.FloatCompareL;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.FloatDivided;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.FloatMinus;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.FloatNegated;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.FloatPlus;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.FloatRemainder;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.FloatTimes;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.IntAnd;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.IntDivided;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.IntMinus;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.IntNegated;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.IntNot;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.IntOr;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.IntPlus;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.IntRemainder;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.IntShiftedLeft;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.IntSignedShiftedRight;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.IntTimes;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.IntUnsignedShiftedRight;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.IntXor;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.LongAnd;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.LongCompare;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.LongDivided;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.LongMinus;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.LongNegated;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.LongNot;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.LongOr;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.LongPlus;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.LongRemainder;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.LongShiftedLeft;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.LongSignedShiftedRight;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.LongTimes;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.LongUnsignedShiftedRight;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.LongXor;
import com.sun.max.vm.compiler.builtin.PointerAtomicBuiltin.CompareAndSwapInt;
import com.sun.max.vm.compiler.builtin.PointerAtomicBuiltin.CompareAndSwapIntAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerAtomicBuiltin.CompareAndSwapReference;
import com.sun.max.vm.compiler.builtin.PointerAtomicBuiltin.CompareAndSwapReferenceAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerAtomicBuiltin.CompareAndSwapWord;
import com.sun.max.vm.compiler.builtin.PointerAtomicBuiltin.CompareAndSwapWordAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.GetByte;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.GetChar;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.GetDouble;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.GetFloat;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.GetInt;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.GetLong;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.GetReference;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.GetShort;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.GetWord;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadByte;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadByteAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadChar;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadCharAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadDouble;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadDoubleAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadFloat;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadFloatAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadInt;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadIntAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadLong;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadLongAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadReference;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadReferenceAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadShort;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadShortAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadWord;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.ReadWordAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.SetByte;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.SetDouble;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.SetFloat;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.SetInt;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.SetLong;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.SetReference;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.SetShort;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.SetWord;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteByte;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteByteAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteDouble;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteDoubleAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteFloat;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteFloatAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteInt;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteIntAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteLong;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteLongAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteReference;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteReferenceAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteShort;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteShortAtIntOffset;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteWord;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.WriteWordAtIntOffset;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.AboveEqual;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.AboveThan;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.AdjustJitStack;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.BarMemory;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.BelowEqual;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.BelowThan;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.Call;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.CompareInts;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.CompareWords;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.DoubleToLong;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.FloatToInt;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.FlushRegisterWindows;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.GetIntegerRegister;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.IntToFloat;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.LeastSignificantBit;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.LongToDouble;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.MostSignificantBit;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.Pause;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.SetIntegerRegister;

public class BuiltinAdapter<IR_Type> implements BuiltinVisitor<IR_Type> {

    public void visitBuiltin(Builtin builtin, IR_Type result, IR_Type[] arguments) {
        ProgramError.unexpected("This builtin should not occur here: " + builtin);
    }

    public void visitIEEE754Builtin(IEEE754Builtin builtin, IR_Type result, IR_Type[] arguments) {
        visitBuiltin(builtin, result, arguments);
    }

    // ////////////////////
    // JavaBuiltin

    public void visitJavaBuiltin(JavaBuiltin builtin, IR_Type result, IR_Type[] arguments) {
        visitBuiltin(builtin, result, arguments);
    }

    public void visitIntNegated(IntNegated builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitFloatNegated(FloatNegated builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitLongNegated(LongNegated builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitDoubleNegated(DoubleNegated builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitIntPlus(IntPlus builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitFloatPlus(FloatPlus builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitLongPlus(LongPlus builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitDoublePlus(DoublePlus builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitIntMinus(IntMinus builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitFloatMinus(FloatMinus builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitLongMinus(LongMinus builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitDoubleMinus(DoubleMinus builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitIntTimes(IntTimes builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitFloatTimes(FloatTimes builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitLongTimes(LongTimes builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitDoubleTimes(DoubleTimes builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitIntDivided(IntDivided builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitFloatDivided(FloatDivided builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitLongDivided(LongDivided builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitDoubleDivided(DoubleDivided builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitIntRemainder(IntRemainder builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitFloatRemainder(FloatRemainder builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitLongRemainder(LongRemainder builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitDoubleRemainder(DoubleRemainder builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitIntShiftedLeft(IntShiftedLeft builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitLongShiftedLeft(LongShiftedLeft builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitIntSignedShiftedRight(IntSignedShiftedRight builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitLongSignedShiftedRight(LongSignedShiftedRight builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitIntUnsignedShiftedRight(IntUnsignedShiftedRight builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitLongUnsignedShiftedRight(LongUnsignedShiftedRight builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitIntNot(IntNot builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitLongNot(LongNot builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitIntAnd(IntAnd builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitLongAnd(LongAnd builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitIntOr(IntOr builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitLongOr(LongOr builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitIntXor(IntXor builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitLongXor(LongXor builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitLongCompare(LongCompare builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitFloatCompareL(FloatCompareL builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitFloatCompareG(FloatCompareG builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitDoubleCompareL(DoubleCompareL builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitDoubleCompareG(DoubleCompareG builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitConvertByteToInt(ConvertByteToInt builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitConvertCharToInt(ConvertCharToInt builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitConvertShortToInt(ConvertShortToInt builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitConvertIntToByte(ConvertIntToByte builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitConvertIntToChar(ConvertIntToChar builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitConvertIntToShort(ConvertIntToShort builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitConvertIntToFloat(ConvertIntToFloat builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitConvertIntToLong(ConvertIntToLong builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitConvertIntToDouble(ConvertIntToDouble builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitConvertLongToInt(ConvertLongToInt builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitConvertLongToFloat(ConvertLongToFloat builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitConvertLongToDouble(ConvertLongToDouble builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    public void visitConvertDoubleToFloat(ConvertDoubleToFloat builtin, IR_Type result, IR_Type[] arguments) {
        visitJavaBuiltin(builtin, result, arguments);
    }

    // ////////////////////
    // AddressBuiltin

    public void visitAddressBuiltin(AddressBuiltin builtin, IR_Type result, IR_Type[] arguments) {
        visitBuiltin(builtin, result, arguments);
    }

    public void visitLessEqual(LessEqual builtin, IR_Type result, IR_Type[] arguments) {
        visitAddressBuiltin(builtin, result, arguments);
    }

    public void visitLessThan(LessThan builtin, IR_Type result, IR_Type[] arguments) {
        visitAddressBuiltin(builtin, result, arguments);
    }

    public void visitGreaterEqual(GreaterEqual builtin, IR_Type result, IR_Type[] arguments) {
        visitAddressBuiltin(builtin, result, arguments);
    }

    public void visitGreaterThan(GreaterThan builtin, IR_Type result, IR_Type[] arguments) {
        visitAddressBuiltin(builtin, result, arguments);
    }

    public void visitDividedByAddress(DividedByAddress builtin, IR_Type result, IR_Type[] arguments) {
        visitAddressBuiltin(builtin, result, arguments);
    }

    public void visitDividedByInt(DividedByInt builtin, IR_Type result, IR_Type[] arguments) {
        visitAddressBuiltin(builtin, result, arguments);
    }

    public void visitRemainderByAddress(RemainderByAddress builtin, IR_Type result, IR_Type[] arguments) {
        visitAddressBuiltin(builtin, result, arguments);
    }

    public void visitRemainderByInt(RemainderByInt builtin, IR_Type result, IR_Type[] arguments) {
        visitAddressBuiltin(builtin, result, arguments);
    }

    // ////////////////////
    // PointerLoadBuiltin

    public void visitPointerLoadBuiltin(PointerLoadBuiltin builtin, IR_Type result, IR_Type[] arguments) {
        visitBuiltin(builtin, result, arguments);
    }

    public void visitWordWidth64(PointerLoadBuiltin builtin, IR_Type result, IR_Type[] arguments) {
        assert Platform.platform().wordWidth() == WordWidth.BITS_64;
        visitPointerLoadBuiltin(builtin, result, arguments);
    }

    public void visitCiscInstructionSet(PointerLoadBuiltin builtin, IR_Type result, IR_Type[] arguments) {
        assert Platform.platform().isa.category == ISA.Category.CISC;
        visitPointerLoadBuiltin(builtin, result, arguments);
    }

    public void visitReadByte(ReadByte builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitReadByteAtIntOffset(ReadByteAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerLoadBuiltin(builtin, result, arguments);
    }

    public void visitGetByte(GetByte builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitReadShort(ReadShort builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitReadShortAtIntOffset(ReadShortAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerLoadBuiltin(builtin, result, arguments);
    }

    public void visitGetShort(GetShort builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitReadChar(ReadChar builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitReadCharAtIntOffset(ReadCharAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerLoadBuiltin(builtin, result, arguments);
    }

    public void visitGetChar(GetChar builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitReadInt(ReadInt builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitReadIntAtIntOffset(ReadIntAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerLoadBuiltin(builtin, result, arguments);
    }

    public void visitGetInt(GetInt builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitReadFloat(ReadFloat builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitReadFloatAtIntOffset(ReadFloatAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerLoadBuiltin(builtin, result, arguments);
    }

    public void visitGetFloat(GetFloat builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitReadLong(ReadLong builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitReadLongAtIntOffset(ReadLongAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerLoadBuiltin(builtin, result, arguments);
    }

    public void visitGetLong(GetLong builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitReadDouble(ReadDouble builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitReadDoubleAtIntOffset(ReadDoubleAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerLoadBuiltin(builtin, result, arguments);
    }

    public void visitGetDouble(GetDouble builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitReadWord(ReadWord builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitReadWordAtIntOffset(ReadWordAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerLoadBuiltin(builtin, result, arguments);
    }

    public void visitGetWord(GetWord builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitReadReference(ReadReference builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitReadReferenceAtIntOffset(ReadReferenceAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerLoadBuiltin(builtin, result, arguments);
    }

    public void visitGetReference(GetReference builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    // ////////////////////
    // PointerStoreBuiltin

    public void visitPointerStoreBuiltin(PointerStoreBuiltin builtin, IR_Type result, IR_Type[] arguments) {
        visitBuiltin(builtin, result, arguments);
    }

    public void visitWordWidth64(PointerStoreBuiltin builtin, IR_Type result, IR_Type[] arguments) {
        assert Platform.platform().wordWidth() == WordWidth.BITS_64;
        visitPointerStoreBuiltin(builtin, result, arguments);
    }

    public void visitCiscInstructionSet(PointerStoreBuiltin builtin, IR_Type result, IR_Type[] arguments) {
        assert Platform.platform().isa.category == ISA.Category.CISC;
        visitPointerStoreBuiltin(builtin, result, arguments);
    }

    public void visitWriteByte(WriteByte builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitWriteByteAtIntOffset(WriteByteAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerStoreBuiltin(builtin, result, arguments);
    }

    public void visitSetByte(SetByte builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitWriteShort(WriteShort builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitWriteShortAtIntOffset(WriteShortAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerStoreBuiltin(builtin, result, arguments);
    }

    public void visitSetShort(SetShort builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitWriteInt(WriteInt builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitWriteIntAtIntOffset(WriteIntAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerStoreBuiltin(builtin, result, arguments);
    }

    public void visitSetInt(SetInt builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitWriteFloat(WriteFloat builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitWriteFloatAtIntOffset(WriteFloatAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerStoreBuiltin(builtin, result, arguments);
    }

    public void visitSetFloat(SetFloat builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitWriteLong(WriteLong builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitWriteLongAtIntOffset(WriteLongAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerStoreBuiltin(builtin, result, arguments);
    }

    public void visitSetLong(SetLong builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitWriteDouble(WriteDouble builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitWriteDoubleAtIntOffset(WriteDoubleAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerStoreBuiltin(builtin, result, arguments);
    }

    public void visitSetDouble(SetDouble builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitWriteWord(WriteWord builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitWriteWordAtIntOffset(WriteWordAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerStoreBuiltin(builtin, result, arguments);
    }

    public void visitSetWord(SetWord builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitWriteReference(WriteReference builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitWriteReferenceAtIntOffset(WriteReferenceAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerStoreBuiltin(builtin, result, arguments);
    }

    public void visitSetReference(SetReference builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    // ////////////////////
    // PointerAtomicBuiltin

    public void visitPointerAtomicBuiltin(PointerAtomicBuiltin builtin, IR_Type result, IR_Type[] arguments) {
        visitBuiltin(builtin, result, arguments);
    }

    public void visitCompareAndSwapInt(CompareAndSwapInt builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerAtomicBuiltin(builtin, result, arguments);
    }

    public void visitCompareAndSwapIntAtIntOffset(CompareAndSwapIntAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerAtomicBuiltin(builtin, result, arguments);
    }

    public void visitCompareAndSwapWord(CompareAndSwapWord builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerAtomicBuiltin(builtin, result, arguments);
    }

    public void visitCompareAndSwapWordAtIntOffset(CompareAndSwapWordAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerAtomicBuiltin(builtin, result, arguments);
    }

    public void visitCompareAndSwapReference(CompareAndSwapReference builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerAtomicBuiltin(builtin, result, arguments);
    }

    public void visitCompareAndSwapReferenceAtIntOffset(CompareAndSwapReferenceAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerAtomicBuiltin(builtin, result, arguments);
    }

    // ////////////////////
    // SpecialBuiltin

    public void visitSpecialBuiltin(SpecialBuiltin builtin, IR_Type result, IR_Type[] arguments) {
        visitBuiltin(builtin, result, arguments);
    }

    public void visitMostSignificantBit(MostSignificantBit builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitLeastSignificantBit(LeastSignificantBit builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitGetIntegerRegister(GetIntegerRegister builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitSetIntegerRegister(SetIntegerRegister builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitAdjustJitStack(AdjustJitStack builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitPause(Pause builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitCall(Call builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitAboveEqual(AboveEqual builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitAboveThan(AboveThan builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitBelowEqual(BelowEqual builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitBelowThan(BelowThan builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitCompareInts(CompareInts builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitCompareWords(CompareWords builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitBarMemory(BarMemory builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitMakeStackVariable(MakeStackVariable builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitStackAllocate(StackAllocate builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitInfopoint(InfopointBuiltin builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitFlushRegisterWindows(FlushRegisterWindows builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitIntToFloat(IntToFloat builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }
    public void visitFloatToInt(FloatToInt builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }
    public void visitLongToDouble(LongToDouble builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }
    public void visitDoubleToLong(DoubleToLong builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitConvertFloatToInt(ConvertFloatToInt builtin, IR_Type result, IR_Type[] arguments) {
        visitIEEE754Builtin(builtin, result, arguments);
    }

    public void visitConvertFloatToLong(ConvertFloatToLong builtin, IR_Type result, IR_Type[] arguments) {
        visitIEEE754Builtin(builtin, result, arguments);
    }

    public void visitConvertDoubleToInt(ConvertDoubleToInt builtin, IR_Type result, IR_Type[] arguments) {
        visitIEEE754Builtin(builtin, result, arguments);
    }

    public void visitConvertDoubleToLong(ConvertDoubleToLong builtin, IR_Type result, IR_Type[] arguments) {
        visitIEEE754Builtin(builtin, result, arguments);
    }

    public void visitConvertFloatToDouble(ConvertFloatToDouble builtin, IR_Type result, IR_Type[] arguments) {
        visitBuiltin(builtin, result, arguments);
    }
}
