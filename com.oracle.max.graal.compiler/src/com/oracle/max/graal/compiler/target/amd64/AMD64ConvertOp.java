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

import com.oracle.max.asm.target.amd64.*;
import com.oracle.max.graal.compiler.asm.*;
import com.oracle.max.graal.compiler.lir.*;
import com.oracle.max.graal.compiler.util.*;
import com.sun.cri.ci.*;

public enum AMD64ConvertOp implements LIROpcode<AMD64MacroAssembler, LIRInstruction>, LIROpcode.FirstOperandRegisterHint {
    I2L, L2I, I2B, I2C, I2S,
    F2D, D2F,
    I2F, I2D,
    L2F, L2D,
    MOV_I2F, MOV_L2D, MOV_F2I, MOV_D2L;

    public LIRInstruction create(CiVariable result, CiVariable input) {
        return new LIRInstruction(this, result, null, input);
    }

    @Override
    public void emitCode(TargetMethodAssembler<AMD64MacroAssembler> tasm, LIRInstruction op) {
        switch (this) {
            case L2I:
                AMD64MoveOp.move(tasm, op.result(), op.input(0));
                tasm.masm.andl(tasm.asIntReg(op.result()), 0xFFFFFFFF);
                break;
            case I2B:
                AMD64MoveOp.move(tasm, op.result(), op.input(0));
                tasm.masm.signExtendByte(tasm.asIntReg(op.result()));
                break;
            case I2C:
                AMD64MoveOp.move(tasm, op.result(), op.input(0));
                tasm.masm.andl(tasm.asIntReg(op.result()), 0xFFFF);
                break;
            case I2S:
                AMD64MoveOp.move(tasm, op.result(), op.input(0));
                tasm.masm.signExtendShort(tasm.asIntReg(op.result()));
                break;
            case I2L: tasm.masm.movslq(tasm.asLongReg(op.result()), tasm.asIntReg(op.input(0))); break;
            case F2D: tasm.masm.cvtss2sd(tasm.asDoubleReg(op.result()), tasm.asFloatReg(op.input(0))); break;
            case D2F: tasm.masm.cvtsd2ss(tasm.asFloatReg(op.result()), tasm.asDoubleReg(op.input(0))); break;
            case I2F: tasm.masm.cvtsi2ssl(tasm.asFloatReg(op.result()), tasm.asIntReg(op.input(0))); break;
            case I2D: tasm.masm.cvtsi2sdl(tasm.asDoubleReg(op.result()), tasm.asIntReg(op.input(0))); break;
            case L2F: tasm.masm.cvtsi2ssq(tasm.asFloatReg(op.result()), tasm.asLongReg(op.input(0))); break;
            case L2D: tasm.masm.cvtsi2sdq(tasm.asDoubleReg(op.result()), tasm.asLongReg(op.input(0))); break;
            case MOV_I2F: tasm.masm.movdl(tasm.asFloatReg(op.result()), tasm.asIntReg(op.input(0))); break;
            case MOV_L2D: tasm.masm.movdq(tasm.asDoubleReg(op.result()), tasm.asLongReg(op.input(0))); break;
            case MOV_F2I: tasm.masm.movdl(tasm.asIntReg(op.result()), tasm.asFloatReg(op.input(0))); break;
            case MOV_D2L: tasm.masm.movdq(tasm.asLongReg(op.result()), tasm.asDoubleReg(op.input(0))); break;
            default: throw Util.shouldNotReachHere();
        }
    }
}
