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
package com.sun.max.vm.cps.hotpath.compiler;

import java.util.*;

import com.sun.max.vm.bytecode.BytecodeAggregatingVisitor.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.cps.collect.*;
import com.sun.max.vm.type.*;

public class OpMap {
    private static Map<Pair<Operation, Kind>, Snippet> operationSnippets;
    private static Map<Pair<Operation, Kind>, Builtin> operationBuiltins;
    private static Map<Pair<Kind, Kind>, Snippet> conversionSnippets;
    private static Map<Pair<Kind, Kind>, Builtin> conversionBuiltins;

    private static Pair<Operation, Kind> from(Operation operation, Kind kind) {
        return new Pair<Operation, Kind>(operation, kind);
    }

    private static Pair<Kind, Kind> from(Kind kind1, Kind kind2) {
        return new Pair<Kind, Kind>(kind1, kind2);
    }

    public static Snippet operationSnippet(Operation operation, Kind kind) {
        return operationSnippets.get(from(operation, kind));
    }

    public static Builtin operationBuiltin(Operation operation, Kind kind) {
        return operationBuiltins.get(from(operation, kind));
    }

    public static Snippet conversionSnippet(Kind fromKind, Kind toKind) {
        return conversionSnippets.get(from(fromKind, toKind));
    }

    public static Builtin conversionBuiltin(Kind fromKind, Kind toKind) {
        return conversionBuiltins.get(from(fromKind, toKind));
    }

    static {
        // Operation Snippets
        operationSnippets = new HashMap<Pair<Operation, Kind>, Snippet>();
        operationSnippets.put(from(Operation.ALOAD, Kind.INT), ArrayGetSnippet.GetInt.SNIPPET);
        operationSnippets.put(from(Operation.ALOAD, Kind.LONG), ArrayGetSnippet.GetLong.SNIPPET);
        operationSnippets.put(from(Operation.ALOAD, Kind.FLOAT), ArrayGetSnippet.GetFloat.SNIPPET);
        operationSnippets.put(from(Operation.ALOAD, Kind.DOUBLE), ArrayGetSnippet.GetDouble.SNIPPET);
        operationSnippets.put(from(Operation.ALOAD, Kind.REFERENCE), ArrayGetSnippet.GetReference.SNIPPET);
        operationSnippets.put(from(Operation.ALOAD, Kind.BYTE), ArrayGetSnippet.GetByte.SNIPPET);
        operationSnippets.put(from(Operation.ALOAD, Kind.CHAR), ArrayGetSnippet.GetChar.SNIPPET);
        operationSnippets.put(from(Operation.ALOAD, Kind.SHORT), ArrayGetSnippet.GetShort.SNIPPET);

        operationSnippets.put(from(Operation.ASTORE, Kind.INT), ArraySetSnippet.SetInt.SNIPPET);
        operationSnippets.put(from(Operation.ASTORE, Kind.LONG), ArraySetSnippet.SetLong.SNIPPET);
        operationSnippets.put(from(Operation.ASTORE, Kind.FLOAT), ArraySetSnippet.SetFloat.SNIPPET);
        operationSnippets.put(from(Operation.ASTORE, Kind.DOUBLE), ArraySetSnippet.SetDouble.SNIPPET);
        operationSnippets.put(from(Operation.ASTORE, Kind.REFERENCE), ArraySetSnippet.SetReference.SNIPPET);
        operationSnippets.put(from(Operation.ASTORE, Kind.BYTE), ArraySetSnippet.SetByte.SNIPPET);
        operationSnippets.put(from(Operation.ASTORE, Kind.CHAR), ArraySetSnippet.SetChar.SNIPPET);
        operationSnippets.put(from(Operation.ASTORE, Kind.SHORT), ArraySetSnippet.SetShort.SNIPPET);

        operationSnippets.put(from(Operation.MUL, Kind.LONG), Snippet.LongTimes.SNIPPET);
        operationSnippets.put(from(Operation.DIV, Kind.LONG), Snippet.LongDivided.SNIPPET);

        operationSnippets.put(from(Operation.REM, Kind.LONG), Snippet.LongRemainder.SNIPPET);
        operationSnippets.put(from(Operation.REM, Kind.FLOAT), Snippet.FloatRemainder.SNIPPET);
        operationSnippets.put(from(Operation.REM, Kind.DOUBLE), Snippet.DoubleRemainder.SNIPPET);

        operationSnippets.put(from(Operation.SHR, Kind.LONG), Snippet.LongSignedShiftedRight.SNIPPET);

        operationSnippets.put(from(Operation.CMP, Kind.LONG), Snippet.LongCompare.SNIPPET);

        operationSnippets.put(from(Operation.GETFIELD, Kind.BYTE), FieldReadSnippet.ReadByte.SNIPPET);
        operationSnippets.put(from(Operation.GETFIELD, Kind.BOOLEAN), FieldReadSnippet.ReadBoolean.SNIPPET);
        operationSnippets.put(from(Operation.GETFIELD, Kind.SHORT), FieldReadSnippet.ReadShort.SNIPPET);
        operationSnippets.put(from(Operation.GETFIELD, Kind.CHAR), FieldReadSnippet.ReadChar.SNIPPET);
        operationSnippets.put(from(Operation.GETFIELD, Kind.INT), FieldReadSnippet.ReadInt.SNIPPET);
        operationSnippets.put(from(Operation.GETFIELD, Kind.FLOAT), FieldReadSnippet.ReadFloat.SNIPPET);
        operationSnippets.put(from(Operation.GETFIELD, Kind.LONG), FieldReadSnippet.ReadLong.SNIPPET);
        operationSnippets.put(from(Operation.GETFIELD, Kind.DOUBLE), FieldReadSnippet.ReadDouble.SNIPPET);
        operationSnippets.put(from(Operation.GETFIELD, Kind.REFERENCE), FieldReadSnippet.ReadReference.SNIPPET);

        operationSnippets.put(from(Operation.PUTFIELD, Kind.BYTE), FieldWriteSnippet.WriteByte.SNIPPET);
        operationSnippets.put(from(Operation.PUTFIELD, Kind.BOOLEAN), FieldWriteSnippet.WriteBoolean.SNIPPET);
        operationSnippets.put(from(Operation.PUTFIELD, Kind.SHORT), FieldWriteSnippet.WriteShort.SNIPPET);
        operationSnippets.put(from(Operation.PUTFIELD, Kind.CHAR), FieldWriteSnippet.WriteChar.SNIPPET);
        operationSnippets.put(from(Operation.PUTFIELD, Kind.INT), FieldWriteSnippet.WriteInt.SNIPPET);
        operationSnippets.put(from(Operation.PUTFIELD, Kind.FLOAT), FieldWriteSnippet.WriteFloat.SNIPPET);
        operationSnippets.put(from(Operation.PUTFIELD, Kind.LONG), FieldWriteSnippet.WriteLong.SNIPPET);
        operationSnippets.put(from(Operation.PUTFIELD, Kind.DOUBLE), FieldWriteSnippet.WriteDouble.SNIPPET);
        operationSnippets.put(from(Operation.PUTFIELD, Kind.REFERENCE), FieldWriteSnippet.WriteReference.SNIPPET);

        // Operation Builtins
        operationBuiltins = new HashMap<Pair<Operation, Kind>, Builtin>();
        operationBuiltins.put(from(Operation.ADD, Kind.INT), JavaBuiltin.IntPlus.BUILTIN);
        operationBuiltins.put(from(Operation.ADD, Kind.LONG), JavaBuiltin.LongPlus.BUILTIN);
        operationBuiltins.put(from(Operation.ADD, Kind.FLOAT), JavaBuiltin.FloatPlus.BUILTIN);
        operationBuiltins.put(from(Operation.ADD, Kind.DOUBLE), JavaBuiltin.DoublePlus.BUILTIN);

        operationBuiltins.put(from(Operation.SUB, Kind.INT), JavaBuiltin.IntMinus.BUILTIN);
        operationBuiltins.put(from(Operation.SUB, Kind.LONG), JavaBuiltin.LongMinus.BUILTIN);
        operationBuiltins.put(from(Operation.SUB, Kind.FLOAT), JavaBuiltin.FloatMinus.BUILTIN);
        operationBuiltins.put(from(Operation.SUB, Kind.DOUBLE), JavaBuiltin.DoubleMinus.BUILTIN);

        operationBuiltins.put(from(Operation.MUL, Kind.INT), JavaBuiltin.IntTimes.BUILTIN);
        operationBuiltins.put(from(Operation.MUL, Kind.LONG), JavaBuiltin.LongTimes.BUILTIN);
        operationBuiltins.put(from(Operation.MUL, Kind.FLOAT), JavaBuiltin.FloatTimes.BUILTIN);
        operationBuiltins.put(from(Operation.MUL, Kind.DOUBLE), JavaBuiltin.DoubleTimes.BUILTIN);

        operationBuiltins.put(from(Operation.DIV, Kind.INT), JavaBuiltin.IntDivided.BUILTIN);
        operationBuiltins.put(from(Operation.DIV, Kind.LONG), JavaBuiltin.LongDivided.BUILTIN);
        operationBuiltins.put(from(Operation.DIV, Kind.FLOAT), JavaBuiltin.FloatDivided.BUILTIN);
        operationBuiltins.put(from(Operation.DIV, Kind.DOUBLE), JavaBuiltin.DoubleDivided.BUILTIN);

        operationBuiltins.put(from(Operation.REM, Kind.INT), JavaBuiltin.IntRemainder.BUILTIN);
        operationBuiltins.put(from(Operation.REM, Kind.LONG), JavaBuiltin.LongRemainder.BUILTIN);
        operationBuiltins.put(from(Operation.REM, Kind.FLOAT), JavaBuiltin.FloatRemainder.BUILTIN);
        operationBuiltins.put(from(Operation.REM, Kind.DOUBLE), JavaBuiltin.DoubleRemainder.BUILTIN);

        operationBuiltins.put(from(Operation.NEG, Kind.INT), JavaBuiltin.IntNegated.BUILTIN);
        operationBuiltins.put(from(Operation.NEG, Kind.LONG), JavaBuiltin.LongNegated.BUILTIN);
        operationBuiltins.put(from(Operation.NEG, Kind.FLOAT), JavaBuiltin.FloatNegated.BUILTIN);
        operationBuiltins.put(from(Operation.NEG, Kind.DOUBLE), JavaBuiltin.DoubleNegated.BUILTIN);

        operationBuiltins.put(from(Operation.SHL, Kind.INT), JavaBuiltin.IntShiftedLeft.BUILTIN);
        operationBuiltins.put(from(Operation.SHL, Kind.LONG), JavaBuiltin.LongShiftedLeft.BUILTIN);

        operationBuiltins.put(from(Operation.SHR, Kind.INT), JavaBuiltin.IntSignedShiftedRight.BUILTIN);
        operationBuiltins.put(from(Operation.SHR, Kind.LONG), JavaBuiltin.LongSignedShiftedRight.BUILTIN);

        operationBuiltins.put(from(Operation.USHR, Kind.INT), JavaBuiltin.IntUnsignedShiftedRight.BUILTIN);
        operationBuiltins.put(from(Operation.USHR, Kind.LONG), JavaBuiltin.LongUnsignedShiftedRight.BUILTIN);

        operationBuiltins.put(from(Operation.AND, Kind.INT), JavaBuiltin.IntAnd.BUILTIN);
        operationBuiltins.put(from(Operation.AND, Kind.LONG), JavaBuiltin.LongAnd.BUILTIN);

        operationBuiltins.put(from(Operation.OR, Kind.INT), JavaBuiltin.IntOr.BUILTIN);
        operationBuiltins.put(from(Operation.OR, Kind.LONG), JavaBuiltin.LongOr.BUILTIN);

        operationBuiltins.put(from(Operation.XOR, Kind.INT), JavaBuiltin.IntXor.BUILTIN);
        operationBuiltins.put(from(Operation.XOR, Kind.LONG), JavaBuiltin.LongXor.BUILTIN);

        operationBuiltins.put(from(Operation.CMP, Kind.LONG), JavaBuiltin.LongCompare.BUILTIN);

        operationBuiltins.put(from(Operation.CMPL, Kind.FLOAT), JavaBuiltin.FloatCompareL.BUILTIN);
        operationBuiltins.put(from(Operation.CMPL, Kind.DOUBLE), JavaBuiltin.DoubleCompareL.BUILTIN);

        operationBuiltins.put(from(Operation.CMPG, Kind.FLOAT), JavaBuiltin.FloatCompareG.BUILTIN);
        operationBuiltins.put(from(Operation.CMPG, Kind.DOUBLE), JavaBuiltin.DoubleCompareG.BUILTIN);

        // Conversion Snippets
        conversionSnippets = new HashMap<Pair<Kind, Kind>, Snippet>();
        conversionSnippets.put(from(Kind.FLOAT, Kind.INT), Snippet.ConvertFloatToInt.SNIPPET);
        conversionSnippets.put(from(Kind.FLOAT, Kind.LONG), Snippet.ConvertFloatToLong.SNIPPET);

        conversionSnippets.put(from(Kind.DOUBLE, Kind.INT), Snippet.ConvertDoubleToInt.SNIPPET);
        conversionSnippets.put(from(Kind.DOUBLE, Kind.LONG), Snippet.ConvertDoubleToLong.SNIPPET);

        // Conversion Builtins
        conversionBuiltins = new HashMap<Pair<Kind, Kind>, Builtin>();
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
