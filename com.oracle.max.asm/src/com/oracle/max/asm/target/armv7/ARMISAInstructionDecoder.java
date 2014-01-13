/*
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
package com.oracle.max.asm.target.armv7;


public final class ARMISAInstructionDecoder {

    private boolean targetIs64Bit;
    private byte[] code;
    private int currentEndOfInstruction;
    private int currentDisplacementPosition;

    private class Prefix {

        // APN not relevant for ARM so deleted
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
	// APN mostly not relevant so ARM so largely deleted
        int ip = inst;
        boolean is64bit = false;

        boolean hasDisp32 = false;
        int tailSize = 0; // other random bytes (#32, #16, etc.) at end of insn


        currentEndOfInstruction = ip + 4; // 32 bit instructions at the moment 
	// APN this will break above if we go to ARMV8 which is 64bit
        currentDisplacementPosition = ip;
    }

    public static void patchRelativeInstruction(byte[] code, int codePos, int relative) {
        ARMISAInstructionDecoder decoder = new ARMISAInstructionDecoder(code, true);
        decoder.decodePosition(codePos);
        int patchPos = decoder.currentDisplacementPosition();
        int endOfInstruction = decoder.currentEndOfInstruction();
        int offset = relative - endOfInstruction + codePos;
        patchDisp32(code, patchPos, offset);
    }

    private static void patchDisp32(byte[] code, int pos, int offset) {
        assert pos + 4 <= code.length;

        assert code[pos] == 0;
        assert code[pos + 1] == 0;
        assert code[pos + 2] == 0;
        assert code[pos + 3] == 0;

        code[pos++] = (byte) (offset & 0xFF);
        code[pos++] = (byte) ((offset >> 8) & 0xFF);
        code[pos++] = (byte) ((offset >> 16) & 0xFF);
        code[pos++] = (byte) ((offset >> 24) & 0xFF);
    }
}
