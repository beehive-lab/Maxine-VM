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

import java.io.*;

import com.sun.max.asm.AssemblyObject.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * Assembler base class.
 *
 * @author Bernd Mathiske
 * @author Greg Wright
 * @author Doug Simon
 */
public abstract class Assembler {

    protected Assembler() {
    }

    public abstract InstructionSet instructionSet();

    public abstract Directives directives();

    /**
     * Gets the width of a word on machine for which the code is being assembled.
     */
    public abstract WordWidth wordWidth();

    /**
     * Resets any internal state in this assembler to the equivalent state it had when first constructed.
     * <p>
     * This method is overridden as needed to ensure the state in subclasses is reset as well. Any
     * overriding implementation must call this method in its superclass.
     */
    public Assembler reset() {
        _boundLabels.clear();
        ((LinkSequence) _assembledObjects).clear();
        ((LinkSequence) _mutableAssembledObjects).clear();
        _padOutput = false;
        _potentialExpansionSize = 0;
        _selectingLabelInstructions = true;
        _stream.reset();
        if (this instanceof Assembler32) {
            final Assembler32 assembler32 = (Assembler32) this;
            assembler32.setStartAddress(0);
        } else if (this instanceof Assembler64) {
            final Assembler64 assembler64 = (Assembler64) this;
            assembler64.setStartAddress(0);
        }
        return this;
    }

    /**
     * A facility for including output during assembly that may not necessarily be decoded interpreted as
     * {@linkplain Type#CODE code}.
     *
     * @author David Liu
     * @author Doug Simon
     */
    public abstract class Directives {
        /**
         * Inserts as many {@linkplain #padByte() pad byte} as necessary to ensure that the next assembled object starts
         * at an address aligned by a given number.
         *
         * @param alignment
         *                the next assembled object is guaranteed to start at the next highest address starting at the
         *                current address that is divisible by this value. Note that this computed address will be the
         *                current address if the current address is already aligned by {@code alignment}
         */
        public void align(int alignment) {
            final int startPosition = currentPosition();

            // We avoid sign problems with '%' below by masking off the sign bit:
            final long unsignedAddend = (baseAddress() + startPosition) & Long.MAX_VALUE;

            final int misalignmentSize = (int) (unsignedAddend % alignment);
            final int padSize = misalignmentSize > 0 ? (alignment - misalignmentSize) : 0;
            for (int i = 0; i < padSize; i++) {
                emitByte(padByte());
            }
            new AlignmentPadding(Assembler.this, startPosition, currentPosition(), alignment, padByte()) {
                public Type type() {
                    return padByteType();
                }
            };
        }

        /**
         * Gets the padding byte used when {@linkplain #align(int) aligning}. Typically, the ISA representation of a
         * single byte NOP instruction is used as the pad byte if possible.
         */
        protected abstract byte padByte();

        /**
         * Gets an object denoting whether the value of the {@linkplain pad byte} can be decoded as valid code or only
         * as data.
         */
        protected abstract Type padByteType();

        public final void inlineByte(byte byteValue) {
            addInlineData(currentPosition(), Bytes.SIZE);
            emitByte(byteValue);
        }

        public final void inlineByteArray(byte[] byteArrayValue) {
            addInlineData(currentPosition(), byteArrayValue.length);
            emitByteArray(byteArrayValue, 0, byteArrayValue.length);
        }

        public final void inlineByteArray(byte[] byteArrayValue, int offset, int length) {
            addInlineData(currentPosition(), length);
            emitByteArray(byteArrayValue, offset, length);
        }

        public final void inlineShort(short shortValue) {
            addInlineData(currentPosition(), Shorts.SIZE);
            emitShort(shortValue);
        }

        public final void inlineInt(int intValue) {
            addInlineData(currentPosition(), Ints.SIZE);
            emitInt(intValue);
        }

        public final void inlineLong(long longValue) {
            addInlineData(currentPosition(), Longs.SIZE);
            emitLong(longValue);
        }

        /**
         * Inlines the absolute address of a position (represented by a given label) in the assembled code.
         * The absolute address is calculated as {@code baseAddress() + label.position()}. The size
         * of the inlined address is determined by {@link Assembler#wordWidth()}.
         *
         * @param label the label whose absolute address is to be inlined
         */
        public AddressLiteral inlineAddress(Label label) {
            final int startPosition = currentPosition();
            // Emit placeholder bytes
            final WordWidth width = wordWidth();
            for (int i = 0; i < width.numberOfBytes(); i++) {
                emitByte((byte) 0);
            }
            final AddressLiteral addressLiteral = new AddressLiteral(Assembler.this, startPosition, currentPosition(), label);
            assert addressLiteral.size() == width.numberOfBytes();
            return addressLiteral;
        }

        /**
         * Inlines the offset between two positions (represented by given labels) in the assembled code.
         *
         * @param base the label whose position marks the base of the offset
         * @param target the label whose position marks the target of the offset
         * @param width the fixed size to be used for the offset
         */
        public OffsetLiteral inlineOffset(Label target, Label base, WordWidth width) {
            final int startPosition = currentPosition();
            for (int i = 0; i < width.numberOfBytes(); i++) {
                emitByte((byte) 0);
            }
            final OffsetLiteral offsetLiteral = new OffsetLiteral(Assembler.this, startPosition, currentPosition(), target, base);
            assert offsetLiteral.size() == width.numberOfBytes();
            return offsetLiteral;
        }
    }

    /**
     * Gets the number of bytes that have been written to the underlying output stream.
     */
    public int currentPosition() {
        return _stream.size();
    }

    /**
     * Gets the start address of the code assembled by this assembler.
     */
    public abstract long baseAddress();

    private ByteArrayOutputStream _stream = new ByteArrayOutputStream();

    protected void emitByte(int byteValue) {
        _stream.write(byteValue);
    }

    protected void emitZeroes(int count) {
        for (int i = 0; i < count; ++i) {
            _stream.write(0);
        }
    }

    protected abstract void emitShort(short shortValue);

    protected abstract void emitInt(int intValue);

    protected abstract void emitLong(long longValue);

    protected void emitByteArray(byte[] byteArrayValue, int off, int len) {
        _stream.write(byteArrayValue, off, len);
    }

    private boolean _selectingLabelInstructions = true;

    boolean selectingLabelInstructions() {
        return _selectingLabelInstructions;
    }

    private final IdentityHashSet<Label> _boundLabels = new IdentityHashSet<Label>();

    public IdentityHashSet<Label> boundLabels() {
        return _boundLabels;
    }

    /**
     * Binds a given label to the current position in the assembler's instruction stream. The assembler may update the
     * label's position if any emitted instructions change lengths, so that this label keeps addressing the same logical
     * position.
     *
     * @param label
     *                the label that is to be bound to the current position
     *
     * @see Label#fix32
     */
    public final void bindLabel(Label label) {
        label.bind(currentPosition());
        _boundLabels.add(label);
    }

    private final AppendableSequence<AssembledObject> _assembledObjects = new LinkSequence<AssembledObject>();
    private final AppendableSequence<MutableAssembledObject> _mutableAssembledObjects = new LinkSequence<MutableAssembledObject>();

    private int _potentialExpansionSize;

    void addFixedSizeAssembledObject(AssembledObject fixedSizeAssembledObject) {
        _assembledObjects.append(fixedSizeAssembledObject);
    }

    void addSpanDependentInstruction(InstructionWithOffset spanDependentInstruction) {
        _assembledObjects.append(spanDependentInstruction);
        _mutableAssembledObjects.append(spanDependentInstruction);
        // A span-dependent instruction's offset operand can potentially grow from 8 bits to 32 bits.
        // Also, some instructions need an extra byte for encoding when not using an 8-bit operand.
        // Together, this might enlarge every span-dependent label instruction by maximally 4 bytes.
        _potentialExpansionSize += 4;
    }

    void addAlignmentPadding(AlignmentPadding alignmentPadding) {
        _assembledObjects.append(alignmentPadding);
        _mutableAssembledObjects.append(alignmentPadding);
        _potentialExpansionSize += alignmentPadding.alignment() - alignmentPadding.size();
    }

    void addInlineData(int startPosition, int size) {
        _assembledObjects.append(new AssembledObject(startPosition, startPosition + size) {
            public Type type() {
                return Type.DATA;
            }
        });
    }

    private void gatherLabels() throws AssemblyException {
        for (AssembledObject assembledObject : _assembledObjects) {
            if (assembledObject instanceof InstructionWithLabel) {
                final InstructionWithLabel labelInstruction = (InstructionWithLabel) assembledObject;
                switch (labelInstruction.label().state()) {
                    case UNASSIGNED:
                        throw new AssemblyException("unassigned label");
                    case BOUND:
                        _boundLabels.add(labelInstruction.label());
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private boolean updateSpanDependentInstruction(InstructionWithOffset instruction) throws AssemblyException {
        if (!instruction.updateLabelSize()) {
            return false;
        }
        final int oldSize = instruction.size();
        _stream.reset();
        instruction.assemble();
        final int newSize = _stream.toByteArray().length;
        instruction.setSize(newSize);
        final int delta = newSize - oldSize;
        adjustMutableAssembledObjects(delta, instruction.startPosition());
        return true;
    }

    private boolean updateAlignmentPadding(AlignmentPadding alignmentPadding) throws AssemblyException {
        final int oldSize = alignmentPadding.size();
        alignmentPadding.updatePadding();
        final int newSize = alignmentPadding.size();
        if (oldSize != newSize) {
            final int delta = newSize - oldSize;
            // It is possible that the old size was zero because of a previous adjustment.
            // This can cause a problem because there could be a label on the next instruction
            // (which would then happen to be at the same offset) that needs to be adjusted, but
            // ordinarily adjustVariableLengthInstructions only adjusts labels beyond startPosition. So we indicate
            // this special case by the zeroLengthAdjust argument being -1. The classic case where this happens
            // is the alignment before a table switch jump target sequence and the label at the start of that sequence..
            adjustMutableAssembledObjects(delta, alignmentPadding.startPosition(), oldSize == 0 ? -1 : 0);
            return true;
        }
        return false;
    }

    private void adjustMutableAssembledObjects(int delta, int startPosition) throws AssemblyException {
        adjustMutableAssembledObjects(delta, startPosition, 0);
    }

    private void adjustMutableAssembledObjects(int delta, int startPosition, int zeroLengthAdjust) throws AssemblyException {
        for (Label label : _boundLabels) {
            // Normally we only adjust labels that are beyond startPosition. However,
            // if zeroLengthAdjust == -1 we will adjust a label at startPosition;  this
            // copes with an AlignmentPaddingInstruction that changes from zero length.
            if (label.position() > startPosition + zeroLengthAdjust) {
                label.adjust(delta);
            }
        }

        for (AssembledObject assembledObject : _assembledObjects) {
            if (assembledObject instanceof MutableAssembledObject) {
                final MutableAssembledObject mutableAssembledObject = (MutableAssembledObject) assembledObject;
                if (mutableAssembledObject.startPosition() > startPosition) {
                    mutableAssembledObject.adjust(delta);
                }
            }
        }
    }

    private void updateSpanDependentVariableInstructions() throws AssemblyException {
        boolean changed;
        do {
            changed = false;
            for (MutableAssembledObject mutableAssembledObject : _mutableAssembledObjects) {
                if (mutableAssembledObject instanceof InstructionWithOffset) {
                    changed |= updateSpanDependentInstruction((InstructionWithOffset) mutableAssembledObject);
                } else if (mutableAssembledObject instanceof AlignmentPadding) {
                    changed |= updateAlignmentPadding((AlignmentPadding) mutableAssembledObject);
                }
            }
        } while (changed);
    }

    private int writeOutput(OutputStream outputStream, byte[] initialBytes, InlineDataRecorder inlineDataRecorder) throws IOException, AssemblyException {
        _selectingLabelInstructions = false;
        int bytesWritten = 0;
        try {
            int initialOffset = 0;
            for (AssembledObject assembledObject : _assembledObjects) {
                if (inlineDataRecorder != null && assembledObject.type() == Type.DATA) {
                    inlineDataRecorder.add(new InlineDataDescriptor.ByteData(assembledObject.startPosition(), assembledObject.size()));
                }

                if (assembledObject instanceof MutableAssembledObject) {
                    final MutableAssembledObject mutableAssembledObject = (MutableAssembledObject) assembledObject;

                    // Copy the original assembler output between the end of the last mutable assembled object and the start of the current one
                    final int length = mutableAssembledObject.initialStartPosition() - initialOffset;
                    outputStream.write(initialBytes, initialOffset, length);
                    bytesWritten += length;

                    // Now (re)assemble the mutable assembled object
                    _stream.reset();
                    mutableAssembledObject.assemble();
                    _stream.writeTo(outputStream);
                    bytesWritten += _stream.size();
                    initialOffset = mutableAssembledObject.initialEndPosition();
                } else {
                    // Copy the original assembler output between the end of the last assembled object and the end of current one
                    final int length = assembledObject.endPosition() - initialOffset;
                    outputStream.write(initialBytes, initialOffset, length);
                    bytesWritten += length;
                    initialOffset = assembledObject.endPosition();
                }
            }

            // Copy the original assembler output (if any) after the last mutable assembled object
            outputStream.write(initialBytes, initialOffset, initialBytes.length - initialOffset);
            bytesWritten += initialBytes.length - initialOffset;

            if (_padOutput) {
                final int padding = (initialBytes.length + _potentialExpansionSize) - bytesWritten;
                assert padding >= 0;
                if (padding > 0) {
                    _stream.reset();
                    emitPadding(padding);
                    _stream.writeTo(outputStream);
                    bytesWritten += padding;
                }
            }
            return bytesWritten;
        } finally {
            _selectingLabelInstructions = true;
        }
    }

    /**
     * Emits padding to the instruction stream in the form of NOP instructions.
     *
     * @param numberOfBytes
     * @throws AssemblyException if exactly {@code numberOfBytes} cannot be emitted as a sequence of one or more valid NOP instructions
     */
    protected abstract void emitPadding(int numberOfBytes) throws AssemblyException;

    /**
     * Writes the object code assembled so far to a given output stream.
     *
     * @return the number of bytes written {@code outputStream}
     * @throws AssemblyException
     *             if there any problem with binding labels to addresses
     */
    public int output(OutputStream outputStream, InlineDataRecorder inlineDataRecorder) throws IOException, AssemblyException {
        final int upperLimitForCurrentOutputSize = upperLimitForCurrentOutputSize();
        final byte[] initialBytes = _stream.toByteArray();
        gatherLabels();
        updateSpanDependentVariableInstructions();
        final int bytesWritten = writeOutput(outputStream, initialBytes, inlineDataRecorder);
        assert !_padOutput || upperLimitForCurrentOutputSize == bytesWritten;
        return bytesWritten;
    }

    /**
     * Gets the maximum size of the code array that would be assembled by a call to {@link #output(OutputStream)} or
     * {@link #toByteArray()}. For a variable sized instruction set (e.g. x86), the exact size may be known until the
     * code is assembled as the size of certain instructions depends on their position in the instruction and/or the
     * {@linkplain #baseAddress() base address} at which the code is being assembled.
     * <p>
     * <b>Note that any subsequent call that adds a new instruction to the instruction stream invalidates the value
     * returned by this method.</b>
     */
    public int upperLimitForCurrentOutputSize() {
        return currentPosition() + _potentialExpansionSize;
    }


    private boolean _padOutput;

    /**
     * Sets or unsets the flag determining if the code assembled by a call to {@link #output(OutputStream)} or
     * {@link #toByteArray()} should be padded with NOPs at the end to ensure that the code size equals the value
     * returned by {@link #upperLimitForCurrentOutputSize()}. This default value of the flag is {@code false}.
     */
    public void setPadOutput(boolean flag) {
        _padOutput = flag;
    }

    /**
     * Returns the object code assembled so far in a byte array.
     *
     * @throws AssemblyException
     *             if there any problem with binding labels to addresses
     */
    public byte[] toByteArray(InlineDataRecorder inlineDataRecorder) throws AssemblyException {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream(upperLimitForCurrentOutputSize());
        try {
            output(stream, inlineDataRecorder);
            stream.close();
            final byte[] result = stream.toByteArray();
            return result;
        } catch (IOException ioException) {
            throw ProgramError.unexpected("IOException during output to byte array", ioException);
        }
    }

    public byte[] toByteArray() throws AssemblyException {
        return toByteArray(null);
    }

    /**
     * @see Label#fix32(int)
     */
    protected void fixLabel32(Label label, int address32) {
        label.fix32(address32);
    }

    /**
     * @see Label#fix64(long)
     */
    protected void fixLabel64(Label label, long address64) {
        label.fix64(address64);
    }

    protected int address32(Label label) throws AssemblyException {
        return label.address32();
    }

    protected long address64(Label label) throws AssemblyException {
        return label.address64();
    }

    protected void checkConstraint(boolean passed, String expression) {
        if (!passed) {
            throw new IllegalArgumentException(expression);
        }
    }

    /**
     * Calculate the difference between two Labels. This works whether the labels
     * are fixed or bound.
     * @throws AssemblyException
     */
    public int labelOffsetRelative(Label label, Label relativeTo) throws AssemblyException {
        return labelOffsetRelative(label, 0) - labelOffsetRelative(relativeTo, 0);
    }

    /**
     * Calculate the difference between a Label and a position within the assembled code.
     * @throws AssemblyException
     */
    public int labelOffsetRelative(Label label, int position) throws AssemblyException {
        switch (label.state()) {
            case BOUND: {
                return label.position() - position;
            }
            case FIXED_32: {
                final Assembler32 assembler32 = (Assembler32) this;
                return assembler32.address(label) - (assembler32.startAddress() + position);
            }
            case FIXED_64: {
                final Assembler64 assembler64 = (Assembler64) this;
                final long offset64 = assembler64.address(label) - (assembler64.startAddress() + position);
                if (Longs.numberOfEffectiveSignedBits(offset64) > 32) {
                    throw new AssemblyException("fixed 64-bit label out of 32-bit range");
                }
                return (int) offset64;
            }
            default: {
                throw new AssemblyException("unassigned label");
            }
        }
    }

    /**
     * Calculate the difference between a Label and an assembled object.
     * Different CPUs have different conventions for which end of an
     * instruction to measure from.
     * @throws AssemblyException
     */
    public final int offsetInstructionRelative(Label label, AssemblyObject assembledObject) throws AssemblyException {
        switch (instructionSet().relativeAddressing()) {
            case FROM_INSTRUCTION_START:
                return labelOffsetRelative(label, assembledObject.startPosition());
            case FROM_INSTRUCTION_END:
                return labelOffsetRelative(label, assembledObject.endPosition());
        }
        throw ProgramError.unknownCase();
    }
}
