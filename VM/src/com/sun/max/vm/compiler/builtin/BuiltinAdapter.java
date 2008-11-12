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

import com.sun.max.asm.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.builtin.AddressBuiltin.*;
import com.sun.max.vm.compiler.builtin.IEEE754Builtin.*;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.*;
import com.sun.max.vm.compiler.builtin.PointerAtomicBuiltin.*;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.*;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.*;
import com.sun.max.vm.compiler.builtin.SafepointBuiltin.*;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.*;

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
        assert Platform.target().processorKind().dataModel().wordWidth() == WordWidth.BITS_64;
        visitPointerLoadBuiltin(builtin, result, arguments);
    }

    public void visitCiscInstructionSet(PointerLoadBuiltin builtin, IR_Type result, IR_Type[] arguments) {
        assert Platform.target().processorKind().instructionSet().category() == InstructionSet.Category.CISC;
        visitPointerLoadBuiltin(builtin, result, arguments);
    }

    public void visitReadByteAtLongOffset(ReadByteAtLongOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitReadByteAtIntOffset(ReadByteAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerLoadBuiltin(builtin, result, arguments);
    }

    public void visitGetByte(GetByte builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitReadShortAtLongOffset(ReadShortAtLongOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitReadShortAtIntOffset(ReadShortAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerLoadBuiltin(builtin, result, arguments);
    }

    public void visitGetShort(GetShort builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitReadCharAtLongOffset(ReadCharAtLongOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitReadCharAtIntOffset(ReadCharAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerLoadBuiltin(builtin, result, arguments);
    }

    public void visitGetChar(GetChar builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitReadIntAtLongOffset(ReadIntAtLongOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitReadIntAtIntOffset(ReadIntAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerLoadBuiltin(builtin, result, arguments);
    }

    public void visitGetInt(GetInt builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitReadFloatAtLongOffset(ReadFloatAtLongOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitReadFloatAtIntOffset(ReadFloatAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerLoadBuiltin(builtin, result, arguments);
    }

    public void visitGetFloat(GetFloat builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitReadLongAtLongOffset(ReadLongAtLongOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitReadLongAtIntOffset(ReadLongAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerLoadBuiltin(builtin, result, arguments);
    }

    public void visitGetLong(GetLong builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitReadDoubleAtLongOffset(ReadDoubleAtLongOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitReadDoubleAtIntOffset(ReadDoubleAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerLoadBuiltin(builtin, result, arguments);
    }

    public void visitGetDouble(GetDouble builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitReadWordAtLongOffset(ReadWordAtLongOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitReadWordAtIntOffset(ReadWordAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerLoadBuiltin(builtin, result, arguments);
    }

    public void visitGetWord(GetWord builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitReadReferenceAtLongOffset(ReadReferenceAtLongOffset builtin, IR_Type result, IR_Type[] arguments) {
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
        assert Platform.target().processorKind().dataModel().wordWidth() == WordWidth.BITS_64;
        visitPointerStoreBuiltin(builtin, result, arguments);
    }

    public void visitCiscInstructionSet(PointerStoreBuiltin builtin, IR_Type result, IR_Type[] arguments) {
        assert Platform.target().processorKind().instructionSet().category() == InstructionSet.Category.CISC;
        visitPointerStoreBuiltin(builtin, result, arguments);
    }

    public void visitWriteByteAtLongOffset(WriteByteAtLongOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitWriteByteAtIntOffset(WriteByteAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerStoreBuiltin(builtin, result, arguments);
    }

    public void visitSetByte(SetByte builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitWriteShortAtLongOffset(WriteShortAtLongOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitWriteShortAtIntOffset(WriteShortAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerStoreBuiltin(builtin, result, arguments);
    }

    public void visitSetShort(SetShort builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitWriteIntAtLongOffset(WriteIntAtLongOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitWriteIntAtIntOffset(WriteIntAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerStoreBuiltin(builtin, result, arguments);
    }

    public void visitSetInt(SetInt builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitWriteFloatAtLongOffset(WriteFloatAtLongOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitWriteFloatAtIntOffset(WriteFloatAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerStoreBuiltin(builtin, result, arguments);
    }

    public void visitSetFloat(SetFloat builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitWriteLongAtLongOffset(WriteLongAtLongOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitWriteLongAtIntOffset(WriteLongAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerStoreBuiltin(builtin, result, arguments);
    }

    public void visitSetLong(SetLong builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitWriteDoubleAtLongOffset(WriteDoubleAtLongOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitWriteDoubleAtIntOffset(WriteDoubleAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerStoreBuiltin(builtin, result, arguments);
    }

    public void visitSetDouble(SetDouble builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitWriteWordAtLongOffset(WriteWordAtLongOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitWordWidth64(builtin, result, arguments);
    }

    public void visitWriteWordAtIntOffset(WriteWordAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerStoreBuiltin(builtin, result, arguments);
    }

    public void visitSetWord(SetWord builtin, IR_Type result, IR_Type[] arguments) {
        visitCiscInstructionSet(builtin, result, arguments);
    }

    public void visitWriteReferenceAtLongOffset(WriteReferenceAtLongOffset builtin, IR_Type result, IR_Type[] arguments) {
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

    public void visitCompareAndSwapIntAtLongOffset(CompareAndSwapIntAtLongOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerAtomicBuiltin(builtin, result, arguments);
    }

    public void visitCompareAndSwapIntAtIntOffset(CompareAndSwapIntAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerAtomicBuiltin(builtin, result, arguments);
    }

    public void visitCompareAndSwapWordAtLongOffset(CompareAndSwapWordAtLongOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerAtomicBuiltin(builtin, result, arguments);
    }

    public void visitCompareAndSwapWordAtIntOffset(CompareAndSwapWordAtIntOffset builtin, IR_Type result, IR_Type[] arguments) {
        visitPointerAtomicBuiltin(builtin, result, arguments);
    }

    public void visitCompareAndSwapReferenceAtLongOffset(CompareAndSwapReferenceAtLongOffset builtin, IR_Type result, IR_Type[] arguments) {
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

    public void visitGetIntegerRegister(GetIntegerRegister builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitSetIntegerRegister(SetIntegerRegister builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitAddWordsToIntegerRegister(AddWordsToIntegerRegister builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitPush(Push builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitPop(Pop builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitGetFloatingPointRegister(GetFloatingPointRegister builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitGetInstructionPointer(GetInstructionPointer builtin, IR_Type result, IR_Type[] arguments) {
        visitGetInstructionPointer(builtin, result, arguments);
    }

    public void visitJump(Jump builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitCall(Call builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitUnsignedIntGreaterEqual(UnsignedIntGreaterEqual builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitCompareInts(CompareInts builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitCompareReferences(CompareReferences builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitBarMemory(BarMemory builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitMakeStackVariable(MakeStackVariable builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitSoftSafepoint(SoftSafepoint builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitHardSafepoint(HardSafepoint builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitFlushRegisterWindows(FlushRegisterWindows builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }

    public void visitBreakpoint(Breakpoint builtin, IR_Type result, IR_Type[] arguments) {
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

    @Override
    public void visitConvertFloatToInt(ConvertFloatToInt builtin, IR_Type result, IR_Type[] arguments) {
        visitIEEE754Builtin(builtin, result, arguments);
    }

    @Override
    public void visitConvertFloatToLong(ConvertFloatToLong builtin, IR_Type result, IR_Type[] arguments) {
        visitIEEE754Builtin(builtin, result, arguments);
    }

    @Override
    public void visitConvertDoubleToInt(ConvertDoubleToInt builtin, IR_Type result, IR_Type[] arguments) {
        visitIEEE754Builtin(builtin, result, arguments);
    }

    @Override
    public void visitConvertDoubleToLong(ConvertDoubleToLong builtin, IR_Type result, IR_Type[] arguments) {
        visitIEEE754Builtin(builtin, result, arguments);
    }

    @Override
    public void visitConvertFloatToDouble(ConvertFloatToDouble builtin, IR_Type result, IR_Type[] arguments) {
        visitBuiltin(builtin, result, arguments);
    }



    public void visitMarker(Marker builtin, IR_Type result, IR_Type[] arguments) {
        visitSpecialBuiltin(builtin, result, arguments);
    }
}
