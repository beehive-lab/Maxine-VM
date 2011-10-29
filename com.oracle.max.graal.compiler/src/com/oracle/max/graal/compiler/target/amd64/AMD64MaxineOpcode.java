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
import com.oracle.max.graal.compiler.lir.FrameMap.StackBlock;
import com.oracle.max.graal.compiler.lir.*;
import com.oracle.max.graal.compiler.util.*;
import com.sun.cri.ci.*;

/**
 * LIR operations that are Maxine-specific, and should therefore be moved to a Maxine-specific project.
 */
public class AMD64MaxineOpcode {

    public enum BreakpointOpcode implements LIROpcode {
        BREAKPOINT;

        public LIRInstruction create() {
            CiValue[] inputs = LIRInstruction.NO_OPERANDS;

            return new AMD64LIRInstruction(this, CiValue.IllegalValue, null, inputs) {
                @Override
                public void emitCode(TargetMethodAssembler tasm, AMD64MacroAssembler masm) {
                    masm.int3();
                }
            };
        }
    }


    public enum SignificantBitOpcode implements LIROpcode {
        MSB, LSB;

        public LIRInstruction create(CiVariable result, CiVariable input) {
            CiValue[] inputs = new CiValue[] {input};
            CiValue[] temps = new CiValue[] {input};

            return new AMD64LIRInstruction(this, result, null, inputs, temps) {
                @Override
                public void emitCode(TargetMethodAssembler tasm, AMD64MacroAssembler masm) {
                    emit(tasm, masm, result(), input(0));
                }

                @Override
                public boolean inputCanBeMemory(int index) {
                    return true;
                }
            };
        }

        private void emit(TargetMethodAssembler tasm, AMD64MacroAssembler masm, CiValue result, CiValue input) {
            CiRegister dst = tasm.asLongReg(result);

            masm.xorq(dst, dst);
            masm.notq(dst);
            if (input.isRegister()) {
                assert tasm.asLongReg(input) != dst;
                switch (this) {
                    case MSB: masm.bsrq(dst, tasm.asLongReg(input)); break;
                    case LSB: masm.bsfq(dst, tasm.asLongReg(input)); break;
                    default: throw Util.shouldNotReachHere();
                }

            } else {
                switch (this) {
                    case MSB: masm.bsrq(dst, tasm.asAddress(input)); break;
                    case LSB: masm.bsfq(dst, tasm.asAddress(input)); break;
                    default: throw Util.shouldNotReachHere();
                }
            }
        }
    }


    public enum StackAllocateOpcode implements LIROpcode {
        STACK_ALLOCATE;

        public LIRInstruction create(CiVariable result, final StackBlock stackBlock) {
            CiValue[] inputs = LIRInstruction.NO_OPERANDS;

            return new AMD64LIRInstruction(this, result, null, inputs) {
                @Override
                public void emitCode(TargetMethodAssembler tasm, AMD64MacroAssembler masm) {
                    masm.leaq(tasm.asRegister(result()), tasm.compilation.frameMap().toStackAddress(stackBlock));
                }
            };
        }
    }
}
