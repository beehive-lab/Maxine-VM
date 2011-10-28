/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.compiler.target.amd64;

import com.oracle.max.asm.*;
import com.oracle.max.asm.target.amd64.*;
import com.oracle.max.asm.target.amd64.AMD64Assembler.*;
import com.oracle.max.graal.compiler.asm.*;
import com.oracle.max.graal.compiler.lir.*;
import com.oracle.max.graal.compiler.stub.*;
import com.oracle.max.graal.compiler.util.*;
import com.sun.cri.ci.*;

public enum AMD64ConvertOpFI implements LIROpcode<AMD64MacroAssembler, LIRConvert> {
    F2I, D2I;

    public LIRInstruction create(CiVariable result, CompilerStub stub, CiVariable input) {
        return new LIRConvert(this, result, stub, new CiValue[] {input}, LIRInstruction.NO_OPERANDS);
    }

    @Override
    public void emitCode(TargetMethodAssembler<AMD64MacroAssembler> tasm, LIRConvert op) {
        switch (this) {
            case F2I: tasm.masm.cvttss2sil(tasm.asIntReg(op.result()), tasm.asFloatReg(op.input(0))); break;
            case D2I: tasm.masm.cvttsd2sil(tasm.asIntReg(op.result()), tasm.asDoubleReg(op.input(0))); break;
            default: throw Util.shouldNotReachHere();
        }

        Label endLabel = new Label();
        tasm.masm.cmp32(tasm.asIntReg(op.result()), Integer.MIN_VALUE);
        tasm.masm.jcc(ConditionFlag.notEqual, endLabel);
        AMD64CallOp.callStub(tasm, op.stub, op.stub.resultKind, null, op.result(), op.input(0));
        tasm.masm.bind(endLabel);
    }
}
