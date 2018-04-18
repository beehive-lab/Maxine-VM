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
package com.oracle.max.asm.target.aarch64;

public final class Aarch64InstructionDecoder {

    private byte[] code;
    private int currentEndOfInstruction;
    private int currentDisplacementPosition;

    private Aarch64InstructionDecoder(byte[] code) {
        this.code = code;
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
        currentEndOfInstruction = ip + 4; // Aarch64 has 32 bit instructions
        currentDisplacementPosition = ip;
    }

    public static void patchRelativeInstruction(byte[] code, int codePos, int relative) {
        Aarch64InstructionDecoder decoder = new Aarch64InstructionDecoder(code);
        decoder.decodePosition(codePos);
        int patchPos = decoder.currentDisplacementPosition();
        int endOfInstruction = decoder.currentEndOfInstruction();
        int offset = relative - endOfInstruction + codePos;
        patchDisp32(code, patchPos, offset);
    }

    private static void patchDisp32(byte[] code, int pos, int offset) {
        assert pos + 4 <= code.length;

        int instruction;
        instruction = Aarch64Assembler.movzHelper(64, Aarch64.r16, offset & 0xFFFF, 0);
        code[pos] = (byte) (instruction & 0xFF);
        code[pos + 1] = (byte) ((instruction >> 8) & 0xFF);
        code[pos + 2] = (byte) ((instruction >> 16) & 0xFF);
        code[pos + 3] = (byte) ((instruction >> 24) & 0xFF);

        offset = offset >> 16;
        offset = offset & 0xffff;

        instruction = Aarch64Assembler.movkHelper(64, Aarch64.r16, offset, 16);
        code[pos + 4] = (byte) (instruction & 0xFF);
        code[pos + 5] = (byte) ((instruction >> 8) & 0xFF);
        code[pos + 6] = (byte) ((instruction >> 16) & 0xFF);
        code[pos + 7] = (byte) ((instruction >> 24) & 0xFF);
    }
}
