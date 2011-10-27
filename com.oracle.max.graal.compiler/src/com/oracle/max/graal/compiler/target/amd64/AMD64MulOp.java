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

public enum AMD64MulOp implements LIROpcode<AMD64MacroAssembler, LIRInstruction> {
    IMUL, LMUL;

    public LIRInstruction create(CiVariable leftAndResult, CiValue right) {
        return new LIRInstruction(this, leftAndResult, null, leftAndResult, right);
    }

    @Override
    public void emitCode(TargetMethodAssembler<AMD64MacroAssembler> tasm, LIRInstruction op) {
        if (op.operand(1).isRegister()) {
            switch (this) {
                case IMUL: tasm.masm.imull(tasm.asRegister(op.result()), tasm.asRegister(op.operand(1))); break;
                case LMUL: tasm.masm.imulq(tasm.asRegister(op.result()), tasm.asRegister(op.operand(1))); break;
                default:   throw Util.shouldNotReachHere();
            }
        } else {
            switch (this) {
                case IMUL: tasm.masm.imull(tasm.asRegister(op.result()), tasm.asRegister(op.operand(0)), tasm.asIntConst(op.operand(1))); break;
                case LMUL: tasm.masm.imulq(tasm.asRegister(op.result()), tasm.asRegister(op.operand(0)), tasm.asIntConst(op.operand(1))); break;
                default:   throw Util.shouldNotReachHere();
            }
        }
    }
}
