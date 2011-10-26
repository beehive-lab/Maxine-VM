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

/**
 * LIR operations that are Maxine-specific, and should therefore be moved to a Maxine-specific project.
 */
public class AMD64MaxineOp {
    public static final BreakpointOpcode BREAKPOINT = new BreakpointOpcode();
    public static final SignificantBitOpcode MSB = new SignificantBitOpcode();
    public static final SignificantBitOpcode LSB = new SignificantBitOpcode();

    protected static class BreakpointOpcode implements LIROpcode<AMD64LIRAssembler, LIRInstruction> {
        public LIRInstruction create() {
            return new LIRInstruction(this, CiValue.IllegalValue, null);
        }

        @Override
        public void emitCode(AMD64LIRAssembler lasm, LIRInstruction op) {
            lasm.masm.int3();
        }
    }

    protected static class SignificantBitOpcode implements LIROpcode<AMD64LIRAssembler, LIRInstruction>, LIROpcode.AllOperandsCanBeMemory {
        public LIRInstruction create(CiVariable result, CiVariable input) {
            return new LIRInstruction(this, result, null, false, 1, 0, input);
        }

        @Override
        public void emitCode(AMD64LIRAssembler lasm, LIRInstruction op) {
            CiValue src = op.operand(0);
            CiRegister result = lasm.asLongReg(op.result());

            lasm.masm.xorq(result, result);
            lasm.masm.notq(result);
            if (src.isRegister()) {
                assert lasm.asLongReg(src) != result;
                if (this == MSB) {
                    lasm.masm.bsrq(result, lasm.asLongReg(src));
                } else if (this == LSB) {
                    lasm.masm.bsfq(result, lasm.asLongReg(src));
                } else {
                    throw Util.shouldNotReachHere();
                }

            } else {
                if (this == MSB) {
                    lasm.masm.bsrq(result, lasm.asAddress(src));
                } else if (this == LSB) {
                    lasm.masm.bsfq(result, lasm.asAddress(src));
                } else {
                    throw Util.shouldNotReachHere();
                }
            }
        }
    }
}
