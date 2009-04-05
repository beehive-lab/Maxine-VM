/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
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
    public final byte[] _code;

    /**
     * Position of the instruction to edit within {@link #_code}.
     */
    public final int _startPosition;

    /**
     * The size of the instruction to edit.
     */
    public final int _size;

    /**
     * For editing a single instruction stored in a byte array.
     * The instruction is edited in place.
     */
    public AMD64InstructionEditor(byte[] code) {
        _startPosition = 0;
        _size = code.length;
        _code = code;
    }

    /**
     * For editing an instruction at a particular offset in code.
     * The instruction is edited in place.
     */
    public AMD64InstructionEditor(byte[] code, int startPosition, int size) {
        _startPosition = startPosition;
        _size = size;
        _code = code;
    }

    public  int getIntDisplacement(WordWidth displacementWidth) throws AssemblyException {
        // Displacement always appended in the end. Same as immediate
        final int displacementOffset = _startPosition + _size - displacementWidth.numberOfBytes();
        switch(displacementWidth) {
            case BITS_8:
                return _code[displacementOffset];
            case BITS_16:
                return getImm16(displacementOffset);
            case BITS_32:
                return getImm32(displacementOffset);
            default:
                throw new AssemblyException("invalid width for a displacement");
        }
    }

    public int getIntImmediate(WordWidth immediateWidth) throws AssemblyException {
        final int immediateOffset = _startPosition + _size - immediateWidth.numberOfBytes();
        switch(immediateWidth) {
            case BITS_8:
                return _code[immediateOffset];
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
        final int displacementOffset = _startPosition + _size - displacementWidth.numberOfBytes();

        // low order byte come first, so we write the offset value first, regardless of the width of the original offset.
        _code[displacementOffset] = disp8;
        if (displacementWidth == WordWidth.BITS_32) {
            _code[displacementOffset + 1] = 0;
            _code[displacementOffset + 2] = 0;
            _code[displacementOffset + 3] = 0;
        }
    }

    private void fixImm8(int startPosition, int imm8) {
        _code[startPosition] = (byte) (imm8 & 0xff);
    }

    private int getImm8(int startPosition) {
        return _code[startPosition] & 0xff;
    }

    private void fixImm16(int startPosition, int imm16) {
        int imm = imm16;
        _code[startPosition] = (byte) (imm & 0xff);
        imm >>= 8;
        _code[startPosition + 1] = (byte) (imm & 0xff);
    }

    private int getImm16(int startPosition) {
        int imm16 = 0;
        imm16 |= getImm8(startPosition + 1) << 8;
        imm16 |= getImm8(startPosition);
        return imm16;
    }

    private void fixImm32(int startPosition, int imm32) {
        int imm = imm32;
        _code[startPosition] = (byte) (imm & 0xff);
        imm >>= 8;
        _code[startPosition + 1] = (byte) (imm & 0xff);
        imm >>= 8;
        _code[startPosition + 2] = (byte) (imm & 0xff);
        imm >>= 8;
        _code[startPosition + 3] = (byte) (imm & 0xff);
    }

    private int getImm32(int startPosition) {
        int imm32 = 0;
        imm32 |= getImm16(startPosition + 2) << 16;
        imm32 |= getImm16(startPosition);
        return imm32;
    }

    private void fixImm64(int startPosition, long imm64) {
        long imm = imm64;
        _code[startPosition] = (byte) (imm & 0xff);
        imm >>= 8;
        _code[startPosition + 1] = (byte) (imm & 0xff);
        imm >>= 8;
        _code[startPosition + 2] = (byte) (imm & 0xff);
        imm >>= 8;
        _code[startPosition + 3] = (byte) (imm & 0xff);
        imm >>= 8;
        _code[startPosition + 4] = (byte) (imm & 0xff);
        imm >>= 8;
        _code[startPosition + 5] = (byte) (imm & 0xff);
        imm >>= 8;
        _code[startPosition + 6] = (byte) (imm & 0xff);
        imm >>= 8;
        _code[startPosition + 7] = (byte) (imm & 0xff);
    }

    public void fixDisplacement(WordWidth displacementWidth, boolean withIndex, int disp32) throws AssemblyException {
        if (displacementWidth != WordWidth.BITS_32) {
            throw new AssemblyException("Invalid offset width. Can't");
        }
        // index to the first byte displacement parameter (it's always appended in the end).
        final int displacementStart = _startPosition + _size - displacementWidth.numberOfBytes();
        fixImm32(displacementStart, disp32);
    }

    public void fixBranchRelativeDisplacement(WordWidth displacementWidth, int disp32) throws AssemblyException {
        final WordWidth effectiveDispWidth = WordWidth.signedEffective(disp32);
        if  (effectiveDispWidth.numberOfBits() > displacementWidth.numberOfBits()) {
            throw new AssemblyException("Width of displacement too long for instruction.");
        }
        final int displacementStart = _startPosition + _size - displacementWidth.numberOfBytes();
        if (displacementWidth == WordWidth.BITS_8) {
            fixImm8(displacementStart, disp32);
        } else {
            fixImm32(displacementStart, disp32);
        }
    }

    private void zeroFillFrom(int start) {
        int i = start;
        while (i < _size) {
            _code[i++] = 0;
        }
    }

    public void fixImmediateOperand(WordWidth operandWidth, byte imm8) {
        final int numBytes = operandWidth.numberOfBytes();
        final int immediateStart = _startPosition + _size - numBytes;
        _code[immediateStart] = imm8;
        zeroFillFrom(immediateStart + 1);
    }

    public void fixImmediateOperand(WordWidth operandWidth, short imm16) {
        // The Eir to AMD64 code generation uses only two width for immediate operands: 8 bits, or 32 bits.
        final WordWidth effectiveOperandWidth =  (operandWidth == WordWidth.BITS_8) ? WordWidth.BITS_8 : WordWidth.BITS_32;
        // index to the first byte of the immediate value
        final int immediateStart = _startPosition + _size - effectiveOperandWidth.numberOfBytes();
        fixImm16(immediateStart, imm16);
        zeroFillFrom(immediateStart + operandWidth.numberOfBytes());
    }

    /*
     * Special AMD64 specific interface to fix the operand of an instruction with a single short operand (e.g., enter, ret).
     */
    public void fixSingleShortOperand(short imm16) {
        // index to the first byte of the immediate value
        final int immediateStart = _startPosition + _size - WordWidth.BITS_16.numberOfBytes();
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
            final int immediateStart = _startPosition + _size - WordWidth.BITS_8.numberOfBytes();
            fixImm8(immediateStart, imm32);
        } else {
            // index to the first byte of the immediate value
            final int immediateStart = _startPosition + _size - WordWidth.BITS_32.numberOfBytes();
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
        final int immediateStart = _startPosition + _size - WordWidth.BITS_32.numberOfBytes();
        fixImm32(immediateStart, imm32);
    }

    public void fixImmediateOperand(long imm64) {
        // index to the first byte of the immediate value
        final int immediateStart = _startPosition + _size - WordWidth.BITS_64.numberOfBytes();
        fixImm64(immediateStart, imm64);
    }

    public void writeTo(OutputStream outputStream) throws IOException {
        outputStream.write(_code, _startPosition, _size);
    }
}
