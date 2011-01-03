/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.asm.amd64;

import java.io.*;

import com.sun.max.asm.*;
import com.sun.max.lang.*;

/**
 * Facility to patch AMD64 assembly instructions.
 *
 * @author Laurent Daynes
 */
public class AMD64InstructionEditor implements AssemblyInstructionEditor {
    /**
     * Target code containing the instruction to edit.
     */
    public final byte[] code;

    /**
     * Position of the instruction to edit within {@link #code}.
     */
    public final int startPosition;

    /**
     * The size of the instruction to edit.
     */
    public final int size;

    /**
     * For editing a single instruction stored in a byte array.
     * The instruction is edited in place.
     */
    public AMD64InstructionEditor(byte[] code) {
        this.startPosition = 0;
        this.size = code.length;
        this.code = code;
    }

    /**
     * For editing an instruction at a particular offset in code.
     * The instruction is edited in place.
     */
    public AMD64InstructionEditor(byte[] code, int startPosition, int size) {
        this.startPosition = startPosition;
        this.size = size;
        this.code = code;
    }

    public  int getIntDisplacement(WordWidth displacementWidth) throws AssemblyException {
        // Displacement always appended in the end. Same as immediate
        final int displacementOffset = startPosition + size - displacementWidth.numberOfBytes;
        switch(displacementWidth) {
            case BITS_8:
                return code[displacementOffset];
            case BITS_16:
                return getImm16(displacementOffset);
            case BITS_32:
                return getImm32(displacementOffset);
            default:
                throw new AssemblyException("invalid width for a displacement");
        }
    }

    public int getIntImmediate(WordWidth immediateWidth) throws AssemblyException {
        final int immediateOffset = startPosition + size - immediateWidth.numberOfBytes;
        switch(immediateWidth) {
            case BITS_8:
                return code[immediateOffset];
            case BITS_16:
                return getImm16(immediateOffset);
            case BITS_32:
                return getImm32(immediateOffset);
            default:
                throw new AssemblyException("invalid width for an integer value");
        }
    }

    public void fixDisplacement(WordWidth displacementWidth, boolean withIndex, byte disp8) {
        // TODO: various invariant control here to make sure that the template of the assembly
        // instruction here has a displacement of the specified width

        // Displacement always appended in the end.
        final int displacementOffset = startPosition + size - displacementWidth.numberOfBytes;

        // low order byte come first, so we write the offset value first, regardless of the width of the original offset.
        code[displacementOffset] = disp8;
        if (displacementWidth == WordWidth.BITS_32) {
            code[displacementOffset + 1] = 0;
            code[displacementOffset + 2] = 0;
            code[displacementOffset + 3] = 0;
        }
    }

    private void fixImm8(int position, int imm8) {
        code[position] = (byte) (imm8 & 0xff);
    }

    private int getImm8(int position) {
        return code[position] & 0xff;
    }

    private void fixImm16(int position, int imm16) {
        int imm = imm16;
        code[position] = (byte) (imm & 0xff);
        imm >>= 8;
        code[position + 1] = (byte) (imm & 0xff);
    }

    private int getImm16(int position) {
        int imm16 = 0;
        imm16 |= getImm8(position + 1) << 8;
        imm16 |= getImm8(position);
        return imm16;
    }

    private void fixImm32(int position, int imm32) {
        int imm = imm32;
        code[position] = (byte) (imm & 0xff);
        imm >>= 8;
        code[position + 1] = (byte) (imm & 0xff);
        imm >>= 8;
        code[position + 2] = (byte) (imm & 0xff);
        imm >>= 8;
        code[position + 3] = (byte) (imm & 0xff);
    }

    private int getImm32(int position) {
        int imm32 = 0;
        imm32 |= getImm16(position + 2) << 16;
        imm32 |= getImm16(position);
        return imm32;
    }

    private void fixImm64(int position, long imm64) {
        long imm = imm64;
        code[position] = (byte) (imm & 0xff);
        imm >>= 8;
        code[position + 1] = (byte) (imm & 0xff);
        imm >>= 8;
        code[position + 2] = (byte) (imm & 0xff);
        imm >>= 8;
        code[position + 3] = (byte) (imm & 0xff);
        imm >>= 8;
        code[position + 4] = (byte) (imm & 0xff);
        imm >>= 8;
        code[position + 5] = (byte) (imm & 0xff);
        imm >>= 8;
        code[position + 6] = (byte) (imm & 0xff);
        imm >>= 8;
        code[position + 7] = (byte) (imm & 0xff);
    }

    public void fixDisplacement(WordWidth displacementWidth, boolean withIndex, int disp32) throws AssemblyException {
        if (displacementWidth != WordWidth.BITS_32) {
            throw new AssemblyException("Invalid offset width. Can't");
        }
        // index to the first byte displacement parameter (it's always appended in the end).
        final int displacementStart = startPosition + size - displacementWidth.numberOfBytes;
        fixImm32(displacementStart, disp32);
    }

    public void fixBranchRelativeDisplacement(WordWidth displacementWidth, int disp32) throws AssemblyException {
        final WordWidth effectiveDispWidth = WordWidth.signedEffective(disp32);
        if  (effectiveDispWidth.numberOfBits > displacementWidth.numberOfBits) {
            throw new AssemblyException("Width of displacement too long for instruction.");
        }
        final int displacementStart = startPosition + size - displacementWidth.numberOfBytes;
        if (displacementWidth == WordWidth.BITS_8) {
            fixImm8(displacementStart, disp32);
        } else {
            fixImm32(displacementStart, disp32);
        }
    }

    private void zeroFillFrom(int start) {
        int i = start;
        while (i < size) {
            code[i++] = 0;
        }
    }

    public void fixImmediateOperand(WordWidth operandWidth, byte imm8) {
        final int numBytes = operandWidth.numberOfBytes;
        final int immediateStart = startPosition + size - numBytes;
        code[immediateStart] = imm8;
        zeroFillFrom(immediateStart + 1);
    }

    public void fixImmediateOperand(WordWidth operandWidth, short imm16) {
        // The Eir to AMD64 code generation uses only two width for immediate operands: 8 bits, or 32 bits.
        final WordWidth effectiveOperandWidth =  (operandWidth == WordWidth.BITS_8) ? WordWidth.BITS_8 : WordWidth.BITS_32;
        // index to the first byte of the immediate value
        final int immediateStart = startPosition + size - effectiveOperandWidth.numberOfBytes;
        fixImm16(immediateStart, imm16);
        zeroFillFrom(immediateStart + operandWidth.numberOfBytes);
    }

    /*
     * Special AMD64 specific interface to fix the operand of an instruction with a single short operand (e.g., enter, ret).
     */
    public void fixSingleShortOperand(short imm16) {
        // index to the first byte of the immediate value
        final int immediateStart = startPosition + size - WordWidth.BITS_16.numberOfBytes;
        fixImm16(immediateStart, imm16);
    }

    /**
     * Replace the operand of the assembly instruction with a new value. The width of the immediate operand the
     *  instruction was originally generated with is specified in parameter.
     * @param operandWidth width of the operand of the assembly instruction.
     * @param imm32 the new value of the instruction's operand.
     */
    public void fixImmediateOperand(WordWidth operandWidth, int imm32) {
        // The Eir to AMD64 code generation uses only two width for immediate operands: 8 bits, or 32 bits.
        if (operandWidth == WordWidth.BITS_8) {
            // index to the first byte of the immediate value
            final int immediateStart = startPosition + size - WordWidth.BITS_8.numberOfBytes;
            fixImm8(immediateStart, imm32);
        } else {
            // index to the first byte of the immediate value
            final int immediateStart = startPosition + size - WordWidth.BITS_32.numberOfBytes;
            fixImm32(immediateStart, imm32);
        }
    }

    /**
     * Replace the operand of the assembly instruction with a new value. The width of the immediate operand the
     *  instruction was originally generated with must be 32 bits.
     * @param operandWidth width of the operand of the assembly instruction.
     * @param imm32 the new value of the instruction's operand.
     */
    public void fixImmediateOperand(int imm32) {
        // index to the first byte of the immediate value
        final int immediateStart = startPosition + size - WordWidth.BITS_32.numberOfBytes;
        fixImm32(immediateStart, imm32);
    }

    public void fixImmediateOperand(long imm64) {
        // index to the first byte of the immediate value
        final int immediateStart = startPosition + size - WordWidth.BITS_64.numberOfBytes;
        fixImm64(immediateStart, imm64);
    }

    public void writeTo(OutputStream outputStream) throws IOException {
        outputStream.write(code, startPosition, size);
    }
}
