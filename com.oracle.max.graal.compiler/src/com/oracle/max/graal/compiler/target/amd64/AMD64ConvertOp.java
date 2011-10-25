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

import com.oracle.max.graal.compiler.lir.*;
import com.oracle.max.graal.compiler.util.*;
import com.sun.cri.ci.*;

public enum AMD64ConvertOp implements LIROpcode<AMD64LIRAssembler, LIRInstruction>, LIROpcode.FirstOperandRegisterHint {
    I2L, L2I, I2B, I2C, I2S,
    F2D, D2F,
    I2F, I2D,
    L2F, L2D,
    MOV_I2F, MOV_L2D, MOV_F2I, MOV_D2L;

    public LIRInstruction create(CiVariable result, CiVariable input) {
        return new LIRInstruction(this, result, null, input);
    }

    @Override
    public void emitCode(AMD64LIRAssembler lasm, LIRInstruction op) {
        switch (this) {
            case L2I:
                AMD64MoveOp.move(lasm, op.result(), op.operand(0));
                lasm.masm.andl(lasm.asIntReg(op.result()), 0xFFFFFFFF);
                break;
            case I2B:
                AMD64MoveOp.move(lasm, op.result(), op.operand(0));
                lasm.masm.signExtendByte(lasm.asIntReg(op.result()));
                break;
            case I2C:
                AMD64MoveOp.move(lasm, op.result(), op.operand(0));
                lasm.masm.andl(lasm.asIntReg(op.result()), 0xFFFF);
                break;
            case I2S:
                AMD64MoveOp.move(lasm, op.result(), op.operand(0));
                lasm.masm.signExtendShort(lasm.asIntReg(op.result()));
                break;
            case I2L: lasm.masm.movslq(lasm.asLongReg(op.result()), lasm.asIntReg(op.operand(0))); break;
            case F2D: lasm.masm.cvtss2sd(lasm.asDoubleReg(op.result()), lasm.asFloatReg(op.operand(0))); break;
            case D2F: lasm.masm.cvtsd2ss(lasm.asFloatReg(op.result()), lasm.asDoubleReg(op.operand(0))); break;
            case I2F: lasm.masm.cvtsi2ssl(lasm.asFloatReg(op.result()), lasm.asIntReg(op.operand(0))); break;
            case I2D: lasm.masm.cvtsi2sdl(lasm.asDoubleReg(op.result()), lasm.asIntReg(op.operand(0))); break;
            case L2F: lasm.masm.cvtsi2ssq(lasm.asFloatReg(op.result()), lasm.asLongReg(op.operand(0))); break;
            case L2D: lasm.masm.cvtsi2sdq(lasm.asDoubleReg(op.result()), lasm.asLongReg(op.operand(0))); break;
            case MOV_I2F: lasm.masm.movdl(lasm.asFloatReg(op.result()), lasm.asIntReg(op.operand(0))); break;
            case MOV_L2D: lasm.masm.movdq(lasm.asDoubleReg(op.result()), lasm.asLongReg(op.operand(0))); break;
            case MOV_F2I: lasm.masm.movdl(lasm.asFloatReg(op.result()), lasm.asIntReg(op.operand(0))); break;
            case MOV_D2L: lasm.masm.movdq(lasm.asLongReg(op.result()), lasm.asIntReg(op.operand(0))); break;
            default: throw Util.shouldNotReachHere();
        }
    }
}
