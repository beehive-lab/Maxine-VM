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
package com.sun.max.vm.hotpath.compiler;

import com.sun.max.collect.*;
import com.sun.max.vm.bytecode.BytecodeAggregatingVisitor.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.type.*;

public class OpMap {
    private static GrowableMapping<Pair<Operation, Kind>, Snippet> _operationSnippets;
    private static GrowableMapping<Pair<Operation, Kind>, Builtin> _operationBuiltins;
    private static GrowableMapping<Pair<Kind, Kind>, Snippet> conversionSnippets;
    private static GrowableMapping<Pair<Kind, Kind>, Builtin> conversionBuiltins;

    private static Pair<Operation, Kind> from(Operation operation, Kind kind) {
        return new Pair<Operation, Kind>(operation, kind);
    }

    private static Pair<Kind, Kind> from(Kind kind1, Kind kind2) {
        return new Pair<Kind, Kind>(kind1, kind2);
    }

    public static Snippet operationSnippet(Operation operation, Kind kind) {
        return _operationSnippets.get(from(operation, kind));
    }

    public static Builtin operationBuiltin(Operation operation, Kind kind) {
        return _operationBuiltins.get(from(operation, kind));
    }

    public static Snippet conversionSnippet(Kind fromKind, Kind toKind) {
        return conversionSnippets.get(from(fromKind, toKind));
    }

    public static Builtin conversionBuiltin(Kind fromKind, Kind toKind) {
        return conversionBuiltins.get(from(fromKind, toKind));
    }

    static {
        // Operation Snippets
        _operationSnippets = new OpenAddressingHashMapping<Pair<Operation, Kind>, Snippet>();
        _operationSnippets.put(from(Operation.ALOAD, Kind.INT), ArrayGetSnippet.GetInt.SNIPPET);
        _operationSnippets.put(from(Operation.ALOAD, Kind.LONG), ArrayGetSnippet.GetLong.SNIPPET);
        _operationSnippets.put(from(Operation.ALOAD, Kind.FLOAT), ArrayGetSnippet.GetFloat.SNIPPET);
        _operationSnippets.put(from(Operation.ALOAD, Kind.DOUBLE), ArrayGetSnippet.GetDouble.SNIPPET);
        _operationSnippets.put(from(Operation.ALOAD, Kind.REFERENCE), ArrayGetSnippet.GetReference.SNIPPET);
        _operationSnippets.put(from(Operation.ALOAD, Kind.BYTE), ArrayGetSnippet.GetByte.SNIPPET);
        _operationSnippets.put(from(Operation.ALOAD, Kind.CHAR), ArrayGetSnippet.GetChar.SNIPPET);
        _operationSnippets.put(from(Operation.ALOAD, Kind.SHORT), ArrayGetSnippet.GetShort.SNIPPET);

        _operationSnippets.put(from(Operation.ASTORE, Kind.INT), ArraySetSnippet.SetInt.SNIPPET);
        _operationSnippets.put(from(Operation.ASTORE, Kind.LONG), ArraySetSnippet.SetLong.SNIPPET);
        _operationSnippets.put(from(Operation.ASTORE, Kind.FLOAT), ArraySetSnippet.SetFloat.SNIPPET);
        _operationSnippets.put(from(Operation.ASTORE, Kind.DOUBLE), ArraySetSnippet.SetDouble.SNIPPET);
        _operationSnippets.put(from(Operation.ASTORE, Kind.REFERENCE), ArraySetSnippet.SetReference.SNIPPET);
        _operationSnippets.put(from(Operation.ASTORE, Kind.BYTE), ArraySetSnippet.SetByte.SNIPPET);
        _operationSnippets.put(from(Operation.ASTORE, Kind.CHAR), ArraySetSnippet.SetChar.SNIPPET);
        _operationSnippets.put(from(Operation.ASTORE, Kind.SHORT), ArraySetSnippet.SetShort.SNIPPET);

        _operationSnippets.put(from(Operation.MUL, Kind.LONG), Snippet.LongTimes.SNIPPET);
        _operationSnippets.put(from(Operation.DIV, Kind.LONG), Snippet.LongDivided.SNIPPET);

        _operationSnippets.put(from(Operation.REM, Kind.LONG), Snippet.LongRemainder.SNIPPET);
        _operationSnippets.put(from(Operation.REM, Kind.FLOAT), Snippet.FloatRemainder.SNIPPET);
        _operationSnippets.put(from(Operation.REM, Kind.DOUBLE), Snippet.DoubleRemainder.SNIPPET);

        _operationSnippets.put(from(Operation.SHR, Kind.LONG), Snippet.LongSignedShiftedRight.SNIPPET);

        _operationSnippets.put(from(Operation.CMP, Kind.LONG), Snippet.LongCompare.SNIPPET);

        _operationSnippets.put(from(Operation.GETFIELD, Kind.BYTE), FieldReadSnippet.ReadByte.SNIPPET);
        _operationSnippets.put(from(Operation.GETFIELD, Kind.BOOLEAN), FieldReadSnippet.ReadBoolean.SNIPPET);
        _operationSnippets.put(from(Operation.GETFIELD, Kind.SHORT), FieldReadSnippet.ReadShort.SNIPPET);
        _operationSnippets.put(from(Operation.GETFIELD, Kind.CHAR), FieldReadSnippet.ReadChar.SNIPPET);
        _operationSnippets.put(from(Operation.GETFIELD, Kind.INT), FieldReadSnippet.ReadInt.SNIPPET);
        _operationSnippets.put(from(Operation.GETFIELD, Kind.FLOAT), FieldReadSnippet.ReadFloat.SNIPPET);
        _operationSnippets.put(from(Operation.GETFIELD, Kind.LONG), FieldReadSnippet.ReadLong.SNIPPET);
        _operationSnippets.put(from(Operation.GETFIELD, Kind.DOUBLE), FieldReadSnippet.ReadDouble.SNIPPET);
        _operationSnippets.put(from(Operation.GETFIELD, Kind.REFERENCE), FieldReadSnippet.ReadReference.SNIPPET);

        _operationSnippets.put(from(Operation.PUTFIELD, Kind.BYTE), FieldWriteSnippet.WriteByte.SNIPPET);
        _operationSnippets.put(from(Operation.PUTFIELD, Kind.BOOLEAN), FieldWriteSnippet.WriteBoolean.SNIPPET);
        _operationSnippets.put(from(Operation.PUTFIELD, Kind.SHORT), FieldWriteSnippet.WriteShort.SNIPPET);
        _operationSnippets.put(from(Operation.PUTFIELD, Kind.CHAR), FieldWriteSnippet.WriteChar.SNIPPET);
        _operationSnippets.put(from(Operation.PUTFIELD, Kind.INT), FieldWriteSnippet.WriteInt.SNIPPET);
        _operationSnippets.put(from(Operation.PUTFIELD, Kind.FLOAT), FieldWriteSnippet.WriteFloat.SNIPPET);
        _operationSnippets.put(from(Operation.PUTFIELD, Kind.LONG), FieldWriteSnippet.WriteLong.SNIPPET);
        _operationSnippets.put(from(Operation.PUTFIELD, Kind.DOUBLE), FieldWriteSnippet.WriteDouble.SNIPPET);
        _operationSnippets.put(from(Operation.PUTFIELD, Kind.REFERENCE), FieldWriteSnippet.WriteReference.SNIPPET);


        // Operation Builtins
        _operationBuiltins = new OpenAddressingHashMapping<Pair<Operation, Kind>, Builtin>();
        _operationBuiltins.put(from(Operation.ADD, Kind.INT), JavaBuiltin.IntPlus.BUILTIN);
        _operationBuiltins.put(from(Operation.ADD, Kind.LONG), JavaBuiltin.LongPlus.BUILTIN);
        _operationBuiltins.put(from(Operation.ADD, Kind.FLOAT), JavaBuiltin.FloatPlus.BUILTIN);
        _operationBuiltins.put(from(Operation.ADD, Kind.DOUBLE), JavaBuiltin.DoublePlus.BUILTIN);

        _operationBuiltins.put(from(Operation.SUB, Kind.INT), JavaBuiltin.IntMinus.BUILTIN);
        _operationBuiltins.put(from(Operation.SUB, Kind.LONG), JavaBuiltin.LongMinus.BUILTIN);
        _operationBuiltins.put(from(Operation.SUB, Kind.FLOAT), JavaBuiltin.FloatMinus.BUILTIN);
        _operationBuiltins.put(from(Operation.SUB, Kind.DOUBLE), JavaBuiltin.DoubleMinus.BUILTIN);

        _operationBuiltins.put(from(Operation.MUL, Kind.INT), JavaBuiltin.IntTimes.BUILTIN);
        _operationBuiltins.put(from(Operation.MUL, Kind.LONG), JavaBuiltin.LongTimes.BUILTIN);
        _operationBuiltins.put(from(Operation.MUL, Kind.FLOAT), JavaBuiltin.FloatTimes.BUILTIN);
        _operationBuiltins.put(from(Operation.MUL, Kind.DOUBLE), JavaBuiltin.DoubleTimes.BUILTIN);

        _operationBuiltins.put(from(Operation.DIV, Kind.INT), JavaBuiltin.IntDivided.BUILTIN);
        _operationBuiltins.put(from(Operation.DIV, Kind.LONG), JavaBuiltin.LongDivided.BUILTIN);
        _operationBuiltins.put(from(Operation.DIV, Kind.FLOAT), JavaBuiltin.FloatDivided.BUILTIN);
        _operationBuiltins.put(from(Operation.DIV, Kind.DOUBLE), JavaBuiltin.DoubleDivided.BUILTIN);

        _operationBuiltins.put(from(Operation.REM, Kind.INT), JavaBuiltin.IntRemainder.BUILTIN);
        _operationBuiltins.put(from(Operation.REM, Kind.LONG), JavaBuiltin.LongRemainder.BUILTIN);
        _operationBuiltins.put(from(Operation.REM, Kind.FLOAT), JavaBuiltin.FloatRemainder.BUILTIN);
        _operationBuiltins.put(from(Operation.REM, Kind.DOUBLE), JavaBuiltin.DoubleRemainder.BUILTIN);

        _operationBuiltins.put(from(Operation.NEG, Kind.INT), JavaBuiltin.IntNegated.BUILTIN);
        _operationBuiltins.put(from(Operation.NEG, Kind.LONG), JavaBuiltin.LongNegated.BUILTIN);
        _operationBuiltins.put(from(Operation.NEG, Kind.FLOAT), JavaBuiltin.FloatNegated.BUILTIN);
        _operationBuiltins.put(from(Operation.NEG, Kind.DOUBLE), JavaBuiltin.DoubleNegated.BUILTIN);

        _operationBuiltins.put(from(Operation.SHL, Kind.INT), JavaBuiltin.IntShiftedLeft.BUILTIN);
        _operationBuiltins.put(from(Operation.SHL, Kind.LONG), JavaBuiltin.LongShiftedLeft.BUILTIN);

        _operationBuiltins.put(from(Operation.SHR, Kind.INT), JavaBuiltin.IntSignedShiftedRight.BUILTIN);
        _operationBuiltins.put(from(Operation.SHR, Kind.LONG), JavaBuiltin.LongSignedShiftedRight.BUILTIN);

        _operationBuiltins.put(from(Operation.USHR, Kind.INT), JavaBuiltin.IntUnsignedShiftedRight.BUILTIN);
        _operationBuiltins.put(from(Operation.USHR, Kind.LONG), JavaBuiltin.LongUnsignedShiftedRight.BUILTIN);

        _operationBuiltins.put(from(Operation.AND, Kind.INT), JavaBuiltin.IntAnd.BUILTIN);
        _operationBuiltins.put(from(Operation.AND, Kind.LONG), JavaBuiltin.LongAnd.BUILTIN);

        _operationBuiltins.put(from(Operation.OR, Kind.INT), JavaBuiltin.IntOr.BUILTIN);
        _operationBuiltins.put(from(Operation.OR, Kind.LONG), JavaBuiltin.LongOr.BUILTIN);

        _operationBuiltins.put(from(Operation.XOR, Kind.INT), JavaBuiltin.IntXor.BUILTIN);
        _operationBuiltins.put(from(Operation.XOR, Kind.LONG), JavaBuiltin.LongXor.BUILTIN);

        _operationBuiltins.put(from(Operation.CMP, Kind.LONG), JavaBuiltin.LongCompare.BUILTIN);

        _operationBuiltins.put(from(Operation.CMPL, Kind.FLOAT), JavaBuiltin.FloatCompareL.BUILTIN);
        _operationBuiltins.put(from(Operation.CMPL, Kind.DOUBLE), JavaBuiltin.DoubleCompareL.BUILTIN);

        _operationBuiltins.put(from(Operation.CMPG, Kind.FLOAT), JavaBuiltin.FloatCompareG.BUILTIN);
        _operationBuiltins.put(from(Operation.CMPG, Kind.DOUBLE), JavaBuiltin.DoubleCompareG.BUILTIN);

        // Conversion Snippets
        conversionSnippets = new OpenAddressingHashMapping<Pair<Kind, Kind>, Snippet>();
        conversionSnippets.put(from(Kind.FLOAT, Kind.INT), Snippet.ConvertFloatToInt.SNIPPET);
        conversionSnippets.put(from(Kind.FLOAT, Kind.LONG), Snippet.ConvertFloatToLong.SNIPPET);

        conversionSnippets.put(from(Kind.DOUBLE, Kind.INT), Snippet.ConvertDoubleToInt.SNIPPET);
        conversionSnippets.put(from(Kind.DOUBLE, Kind.LONG), Snippet.ConvertDoubleToLong.SNIPPET);

        // Conversion Builtins
        conversionBuiltins = new OpenAddressingHashMapping<Pair<Kind, Kind>, Builtin>();
        conversionBuiltins.put(from(Kind.INT, Kind.LONG), JavaBuiltin.ConvertIntToLong.BUILTIN);
        conversionBuiltins.put(from(Kind.INT, Kind.FLOAT), JavaBuiltin.ConvertIntToFloat.BUILTIN);
        conversionBuiltins.put(from(Kind.INT, Kind.DOUBLE), JavaBuiltin.ConvertIntToDouble.BUILTIN);

        conversionBuiltins.put(from(Kind.INT, Kind.BYTE), JavaBuiltin.ConvertIntToByte.BUILTIN);
        conversionBuiltins.put(from(Kind.INT, Kind.CHAR), JavaBuiltin.ConvertIntToChar.BUILTIN);
        conversionBuiltins.put(from(Kind.INT, Kind.SHORT), JavaBuiltin.ConvertIntToShort.BUILTIN);

        conversionBuiltins.put(from(Kind.LONG, Kind.INT), JavaBuiltin.ConvertLongToInt.BUILTIN);
        conversionBuiltins.put(from(Kind.LONG, Kind.FLOAT), JavaBuiltin.ConvertLongToFloat.BUILTIN);
        conversionBuiltins.put(from(Kind.LONG, Kind.DOUBLE), JavaBuiltin.ConvertLongToDouble.BUILTIN);

        conversionBuiltins.put(from(Kind.FLOAT, Kind.DOUBLE), JavaBuiltin.ConvertFloatToDouble.BUILTIN);

        conversionBuiltins.put(from(Kind.DOUBLE, Kind.FLOAT), JavaBuiltin.ConvertDoubleToFloat.BUILTIN);
    }
}
