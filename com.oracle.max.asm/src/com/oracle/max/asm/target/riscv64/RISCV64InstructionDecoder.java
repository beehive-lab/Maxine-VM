/*
 * Copyright (c) 2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.asm.target.riscv64;

public final class RISCV64InstructionDecoder {

    public static void patchRelativeInstruction(byte[] code, int codePos, int relative) {
        patchDisp32(code, codePos, relative);
    }

    private static void patchDisp32(byte[] code, int pos, int offset) {
        assert pos + RISCV64MacroAssembler.INSTRUCTION_SIZE *
                RISCV64MacroAssembler.PLACEHOLDER_INSTRUCTIONS_FOR_LONG_OFFSETS <= code.length;

        pos += RISCV64MacroAssembler.INSTRUCTION_SIZE; //skip asm.auipc(scratch, 0);

        //Nop everything except the asm.auipc(scratch, 0);
        int instruction = RISCV64MacroAssembler.addImmediateHelper(RISCV64.x0, RISCV64.x0, 0);
        for (int i = 0; i < RISCV64MacroAssembler.PLACEHOLDER_INSTRUCTIONS_FOR_LONG_OFFSETS; i++) {
            writeInstruction(code, pos + i * RISCV64MacroAssembler.INSTRUCTION_SIZE, instruction);
        }

        if (RISCV64MacroAssembler.isArithmeticImmediate(offset)) {
            instruction = RISCV64MacroAssembler.addImmediateHelper(RISCV64.x28, RISCV64.x28,  offset);
            writeInstruction(code, pos, instruction);
        } else {
            int[] mov32BitConstantInstructions = RISCV64MacroAssembler.mov32BitConstantHelper(RISCV64.x29, offset);
            for (int i = 0; i < mov32BitConstantInstructions.length; i++) {
                instruction = mov32BitConstantInstructions[i];
                if (instruction != 0) { // fill in with asm.nop() if mov32BitConstant did not need those instructions
                    writeInstruction(code, pos + i * RISCV64MacroAssembler.INSTRUCTION_SIZE, instruction);
                }
            }
            assert mov32BitConstantInstructions.length <=
                    RISCV64MacroAssembler.PLACEHOLDER_INSTRUCTIONS_FOR_LONG_OFFSETS - 1;
            instruction = RISCV64MacroAssembler.addSubInstructionHelper(RISCV64.x28, RISCV64.x28, RISCV64.x29, false);
            writeInstruction(code, pos + mov32BitConstantInstructions.length * RISCV64MacroAssembler.INSTRUCTION_SIZE, instruction);
        }
    }

    private static void writeInstruction(byte[] code, int offset, int instruction) {
        code[offset + 0] = (byte) (instruction       & 0xFF);
        code[offset + 1] = (byte) (instruction >> 8  & 0xFF);
        code[offset + 2] = (byte) (instruction >> 16 & 0xFF);
        code[offset + 3] = (byte) (instruction >> 24 & 0xFF);
    }
}

