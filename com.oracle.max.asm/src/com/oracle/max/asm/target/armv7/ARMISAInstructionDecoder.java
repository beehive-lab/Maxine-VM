/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
 */
package com.oracle.max.asm.target.armv7;

public final class ARMISAInstructionDecoder {

    private boolean targetIs64Bit;
    private byte[] code;
    private int currentEndOfInstruction;
    private int currentDisplacementPosition;

    private class Prefix {

    }

    private ARMISAInstructionDecoder(byte[] code, boolean targetIs64Bit) {
        this.code = code;
        this.targetIs64Bit = targetIs64Bit;
    }

    public int currentEndOfInstruction() {
        return currentEndOfInstruction;
    }

    public int currentDisplacementPosition() {
        return currentDisplacementPosition;
    }

    public void decodePosition(int inst) {
        assert inst >= 0 && inst < code.length;
        int ip = inst;
        currentEndOfInstruction = ip + 4; // 32 bit instructions at the moment
        currentDisplacementPosition = ip;
    }

    public static void patchRelativeInstruction(byte[] code, int codePos, int relative) {
        ARMISAInstructionDecoder decoder = new ARMISAInstructionDecoder(code, true);
        decoder.decodePosition(codePos);
        int patchPos = decoder.currentDisplacementPosition();
        int endOfInstruction = decoder.currentEndOfInstruction();
        int offset = relative - endOfInstruction + codePos - 12;
        patchDisp32(code, patchPos, offset);
    }

    private static void patchDisp32(byte[] code, int pos, int offset) {
        int instruction;
        instruction = ARMV7Assembler.movwHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, offset & 0xffff);
        code[pos] = (byte) (instruction & 0xFF);
        code[pos + 1] = (byte) ((instruction >> 8) & 0xFF);
        code[pos + 2] = (byte) ((instruction >> 16) & 0xFF);
        code[pos + 3] = (byte) ((instruction >> 24) & 0xFF);
        offset = offset >> 16;
        offset = offset & 0xffff;
        instruction = ARMV7Assembler.movtHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, offset);
        code[pos + 4] = (byte) (instruction & 0xFF);
        code[pos + 5] = (byte) ((instruction >> 8) & 0xFF);
        code[pos + 6] = (byte) ((instruction >> 16) & 0xFF);
        code[pos + 7] = (byte) ((instruction >> 24) & 0xFF);
    }

    private static int movt(final int imm16) {
        int instruction = 0xe3400000;
        instruction |= (imm16 >> 12) << 16;
        instruction |= (12 & 0xf) << 12;
        instruction |= imm16 & 0xfff;
        return instruction;
    }

    private static int movw(final int imm16) {
        int instruction = 0xe3000000;
        instruction |= (imm16 >> 12) << 16;
        instruction |= (12 & 0xf) << 12;
        instruction |= imm16 & 0xfff;
        return instruction;
    }
}
