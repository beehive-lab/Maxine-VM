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

    public static void patchRelativeInstruction(byte[] code, int codePos, int relative) {
        patchDisp32(code, codePos, relative);
    }

    private static void patchDisp32(byte[] code, int pos, int offset) {
        assert pos + 4 <= code.length;

        int instruction = Aarch64Assembler.adrHelper(Aarch64.r16, offset);
        code[pos] = (byte) (instruction & 0xFF);
        code[pos + 1] = (byte) ((instruction >> 8) & 0xFF);
        code[pos + 2] = (byte) ((instruction >> 16) & 0xFF);
        code[pos + 3] = (byte) ((instruction >> 24) & 0xFF);
    }
}
