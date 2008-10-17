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
package com.sun.max.asm;

import com.sun.max.lang.*;

/**
 * An instruction that addresses some data as an offset from itself.
 * 
 * @author Bernd Mathiske
 * @author Greg Wright
 * @author Doug Simon
 */
public abstract class InstructionWithOffset extends InstructionWithLabel {

    /**
     * The mask of all the valid offset sizes supported by the union of all instructions that can address some data via an offset.
     */
    public static final int ALL_VALID_OFFSET_SIZES_MASK = WordWidth.BITS_8.numberOfBytes() | WordWidth.BITS_16.numberOfBytes() | WordWidth.BITS_32.numberOfBytes() | WordWidth.BITS_64.numberOfBytes();

    private final int _validOffsetSizesMask;
    private int _offsetSize;

    /**
     * Creates an object representing an instruction that addresses some data as an offset from itself.
     * 
     * @param startPosition
     *                the current position in the instruction stream of the instruction's first byte
     * @param endPosition
     *                the current position in the instruction stream one byte past the instruction's last byte
     * @param label
     *                a label representing the position referred to by this instruction
     * @param validOffsetSizesMask
     *                a mask of the offset sizes supported by a concrete instruction. This value must not be 0 and its
     *                set of non-zero bits must be a subset of the non-zero bits of {@link #ALL_VALID_OFFSET_SIZES_MASK}.
     *                The one-bit integer values (i.e. the powers of two) corresponding with each set bit in the mask
     *                are the offset sizes for which there is an available concrete instruction. The concrete
     *                instruction emitted once the label has been bound is the one with smallest offset size
     *                that can represent the distance between the label's position and this instruction.
     */
    protected InstructionWithOffset(Assembler assembler, int startPosition, int endPosition, Label label, int validOffsetSizesMask) {
        super(assembler, startPosition, endPosition, label);
        _validOffsetSizesMask = validOffsetSizesMask;
        assert validOffsetSizesMask != 0;
        assert (validOffsetSizesMask & ~ALL_VALID_OFFSET_SIZES_MASK) == 0;
        if (Ints.isPowerOfTwo(validOffsetSizesMask)) {
            assembler.addFixedSizeAssembledObject(this);
            _offsetSize = validOffsetSizesMask;
        } else {
            assembler.addSpanDependentInstruction(this);
            _offsetSize = Integer.lowestOneBit(validOffsetSizesMask);
        }
    }

    protected InstructionWithOffset(Assembler assembler, int startPosition, int endPosition, Label label) {
        super(assembler, startPosition, endPosition, label);
        _validOffsetSizesMask = 0;
        assembler.addFixedSizeAssembledObject(this);
    }

    void setSize(int nBytes) {
        _variableSize = nBytes;
    }

    protected final int labelSize() {
        return _offsetSize;
    }

    /**
     * Updates the size of this instruction's label based on the value bound to the label.
     * 
     * @return true if the size of this instruction's label was changed
     */
    boolean updateLabelSize() throws AssemblyException {
        int offsetSize = WordWidth.signedEffective(offset()).numberOfBytes();
        if (offsetSize > _offsetSize) {
            final int maxLabelSize = Integer.highestOneBit(_validOffsetSizesMask);
            if (offsetSize > maxLabelSize) {
                throw new AssemblyException("instruction cannot accomodate number of bits required for offset");
            }
            while ((offsetSize & _validOffsetSizesMask) == 0) {
                offsetSize = offsetSize << 1;
            }
            _offsetSize = offsetSize;
            return true;
        }
        return false;
    }

    private int offset() throws AssemblyException {
        return assembler().offsetInstructionRelative(label(), this);
    }

    protected byte offsetAsByte() throws AssemblyException {
        if (assembler().selectingLabelInstructions()) {
            return (byte) 0;
        }
        final int result = offset();
        if (Ints.numberOfEffectiveSignedBits(result) > 8) {
            throw new AssemblyException("label out of 8-bit range");
        }
        return (byte) result;
    }

    protected short offsetAsShort() throws AssemblyException {
        if (assembler().selectingLabelInstructions()) {
            return (short) 0;
        }
        final int result = offset();
        if (Ints.numberOfEffectiveSignedBits(result) > 16) {
            throw new AssemblyException("label out of 16-bit range");
        }
        return (short) result;
    }

    protected int offsetAsInt() throws AssemblyException {
        if (assembler().selectingLabelInstructions()) {
            return 0;
        }
        return offset();
    }

}
