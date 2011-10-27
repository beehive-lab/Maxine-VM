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

public enum AMD64MathIntrinsicOp implements LIROpcode<AMD64LIRAssembler, LIRInstruction> {
    SQRT,
    SIN, COS, TAN,
    LOG, LOG10;

    public LIRInstruction create(CiVariable result, CiVariable input) {
        return new LIRInstruction(this, result, null, input);
    }

    @Override
    public void emitCode(AMD64LIRAssembler lasm, LIRInstruction op) {
        CiRegister input = lasm.asDoubleReg(op.operand(0));
        CiRegister result = lasm.asDoubleReg(op.result());
        switch (this) {
            case SQRT:  lasm.masm.sqrtsd(result, input); break;
            case LOG:   lasm.masm.flog(result, input, false); break;
            case LOG10: lasm.masm.flog(result, input, true); break;
            case SIN:   lasm.masm.fsin(result, input); break;
            case COS:   lasm.masm.fcos(result, input); break;
            case TAN:   lasm.masm.ftan(result, input); break;
            default:    throw Util.shouldNotReachHere();
        }
    }
}
