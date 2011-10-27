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

public enum AMD64ConvertOpFL implements LIROpcode<AMD64MacroAssembler, LIRConvert> {
    F2L, D2L;

    public LIRInstruction create(CiVariable result, CompilerStub stub, CiVariable input, CiVariable tmp) {
        return new LIRConvert(this, result, stub, input, tmp);
    }

    @Override
    public void emitCode(TargetMethodAssembler<AMD64MacroAssembler> tasm, LIRConvert op) {
        CiRegister dst = tasm.asLongReg(op.result());
        CiRegister tmp = tasm.asLongReg(op.operand(1));
        switch (this) {
            case F2L: tasm.masm.cvttss2siq(dst, tasm.asFloatReg(op.operand(0))); break;
            case D2L: tasm.masm.cvttsd2siq(dst, tasm.asDoubleReg(op.operand(0))); break;
            default: throw Util.shouldNotReachHere();
        }

        Label endLabel = new Label();
        tasm.masm.movq(tmp, java.lang.Long.MIN_VALUE);
        tasm.masm.cmpq(dst, tmp);
        tasm.masm.jcc(ConditionFlag.notEqual, endLabel);
        AMD64CallOp.callStub(tasm, op.stub, op.stub.resultKind, null, op.result(), op.operand(0));
        tasm.masm.bind(endLabel);
    }
}
