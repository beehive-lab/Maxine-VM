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
import com.oracle.max.graal.compiler.lir.FrameMap.StackBlock;
import com.oracle.max.graal.compiler.util.*;
import com.sun.cri.ci.*;

/**
 * LIR operations that are Maxine-specific, and should therefore be moved to a Maxine-specific project.
 */
public class AMD64MaxineOp {
    public static final BreakpointOpcode BREAKPOINT = new BreakpointOpcode();
    public static final SignificantBitOpcode MSB = new SignificantBitOpcode();
    public static final SignificantBitOpcode LSB = new SignificantBitOpcode();
    public static final StackAllocateOpcode STACK_ALLOCATE = new StackAllocateOpcode();

    public static class BreakpointOpcode implements LIROpcode<AMD64MacroAssembler, LIRInstruction> {
        public LIRInstruction create() {
            return new LIRInstruction(this, CiValue.IllegalValue, null);
        }

        @Override
        public void emitCode(TargetMethodAssembler<AMD64MacroAssembler> tasm, LIRInstruction op) {
            tasm.masm.int3();
        }
    }

    public static class SignificantBitOpcode implements LIROpcode<AMD64MacroAssembler, LIRInstruction>, LIROpcode.AllOperandsCanBeMemory {
        public LIRInstruction create(CiVariable result, CiVariable input) {
            return new LIRInstruction(this, result, null, false, new CiValue[] {input}, new CiValue[] {input});
        }

        @Override
        public void emitCode(TargetMethodAssembler<AMD64MacroAssembler> tasm, LIRInstruction op) {
            CiValue src = op.input(0);
            CiRegister result = tasm.asLongReg(op.result());

            tasm.masm.xorq(result, result);
            tasm.masm.notq(result);
            if (src.isRegister()) {
                assert tasm.asLongReg(src) != result;
                if (this == MSB) {
                    tasm.masm.bsrq(result, tasm.asLongReg(src));
                } else if (this == LSB) {
                    tasm.masm.bsfq(result, tasm.asLongReg(src));
                } else {
                    throw Util.shouldNotReachHere();
                }

            } else {
                if (this == MSB) {
                    tasm.masm.bsrq(result, tasm.asAddress(src));
                } else if (this == LSB) {
                    tasm.masm.bsfq(result, tasm.asAddress(src));
                } else {
                    throw Util.shouldNotReachHere();
                }
            }
        }
    }

    public static class StackAllocateOpcode implements LIROpcode<AMD64MacroAssembler, LIRStackAllocate> {
        public LIRInstruction create(CiVariable result, StackBlock stackBlock) {
            return new LIRStackAllocate(this, result, stackBlock);
        }

        @Override
        public void emitCode(TargetMethodAssembler<AMD64MacroAssembler> tasm, LIRStackAllocate op) {
            tasm.masm.leaq(op.result().asRegister(), tasm.compilation.frameMap().toStackAddress(op.stackBlock));
        }
    }
}
