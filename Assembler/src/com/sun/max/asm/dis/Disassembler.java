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
/*VCSID=2a1b0ea8-e4a9-49e9-aad8-13bad365e9b1*/
package com.sun.max.asm.dis;

import java.io.*;

import com.sun.max.asm.*;
import com.sun.max.asm.gen.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * Disassemblers scan machine code, discern and decode individual instructions
 * and represent them in an abstract representation (DisassembledInstruction),
 * which they can then render in human-readable form, an assembly listing.
 *
 * @see DisassembledInstruction
 *
 * @author Bernd Mathiske
 * @author Greg Wright
 */
public abstract class Disassembler<Template_Type extends Template, DisassembledInstruction_Type extends DisassembledInstruction<Template_Type>> {

    private final Assembly<Template_Type> _assembly;
    private final WordWidth _addressWidth;
    private final Endianness _endianness;

    protected Disassembler(Assembly<Template_Type> assembly, WordWidth addressWidth, Endianness endianness) {
        _assembly = assembly;
        _addressWidth = addressWidth;
        _endianness = endianness;
    }

    public Assembly<Template_Type> assembly() {
        return _assembly;
    }

    public WordWidth addressWidth() {
        return _addressWidth;
    }

    public Endianness endianness() {
        return _endianness;
    }

    protected int _currentPosition;

    public void setCurrentPosition(int position) {
        _currentPosition = position;
    }

    private InlineDataDecoder _inlineDataDecoder;

    public void setInlineDataDecoder(InlineDataDecoder inlineDataDecoder) {
        _inlineDataDecoder = inlineDataDecoder;
    }

    public InlineDataDecoder inlineDataDecoder() {
        return _inlineDataDecoder;
    }

    public abstract Class<DisassembledInstruction_Type> disassembledInstructionType();

    protected abstract DisassembledInstruction_Type createDisassembledInstruction(int position, byte[] bytes, Template_Type template, IndexedSequence<Argument> arguments);

    protected abstract  DisassembledInstruction_Type createDisassembledInlineBytesInstruction(int currentPosition, byte[] bytes);

    protected abstract Assembler createAssembler(int position);

    /**
     * Scans an instruction stream and disassembles the first encoded instruction.
     * <p>
     * @return the disassembled forms that match the first encoded instruction in {@code stream}
     */
    public abstract Sequence<DisassembledInstruction_Type> scanOneInstruction(BufferedInputStream stream) throws IOException, AssemblyException;

    /**
     * Scans an instruction stream and disassembles the encoded instructions. If an encoded instruction has
     * more than one matching disassembled form, an arbitrary choice of one of the disassembled forms is
     * appended to the returned sequence.
     * <p>
     * The {@link #scanOneInstruction} method can be used to obtain all the disassembled forms
     * for each instruction in an instruction stream.
     */
    public abstract IndexedSequence<DisassembledInstruction_Type> scan(BufferedInputStream stream) throws IOException, AssemblyException;

    private int findTargetInstructionIndex(int position, IndexedSequence<DisassembledInstruction_Type> disassembledInstructions) {
        if (position >= 0 && position <= disassembledInstructions.last().startPosition()) {
            for (int i = 0; i < disassembledInstructions.length(); i++) {
                if (disassembledInstructions.get(i).startPosition() == position) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * A label map is a sequence of labels that matches a sequence of disassembled instructions,
     * containing either a label or null at the index of each instruction.
     */
    public IndexedSequence<DisassembledLabel> createLabelMap(IndexedSequence<DisassembledInstruction_Type> disassembledInstructions) {
        final MutableSequence<DisassembledLabel> labels = new ArraySequence<DisassembledLabel>(disassembledInstructions.length());
        for (DisassembledInstruction_Type disassembledInstruction : disassembledInstructions) {
            final Template_Type template = disassembledInstruction.template();
            final int parameterIndex = template.labelParameterIndex();
            if (parameterIndex >= 0) {
                final ImmediateArgument immediateArgument = (ImmediateArgument) disassembledInstruction.arguments().get(parameterIndex);
                final Parameter parameter = template.parameters().get(parameterIndex);
                final int position = (parameter instanceof OffsetParameter) ?
                                (int) immediateArgument.asLong() + disassembledInstruction.positionForRelativeAddressing() :
                                disassembledInstruction.addressToPosition(immediateArgument);
                final int targetInstructionIndex = findTargetInstructionIndex(position, disassembledInstructions);
                if (targetInstructionIndex >= 0 && labels.get(targetInstructionIndex) == null) {
                    labels.set(targetInstructionIndex, new DisassembledLabel(targetInstructionIndex));
                }
            }
        }
        return labels;
    }

    /**
     * Assigns serial numbers to these labels.
     * 
     * @return the length (i.e. number of characters) of the longest {@linkplain DisassembledLabel#name() label name}
     */
    public int updateLabels(Sequence<DisassembledLabel> labels, IndexedSequence<DisassembledInstruction_Type> disassembledInstructions) {
        int result = 0;
        int serial = 1;
        for (DisassembledLabel label : labels) {
            label.setSerial(serial);
            serial++;
            label.bind(disassembledInstructions.get(label.instructionIndex()).startPosition());
            if (label.name().length() > result) {
                result = label.name().length();
            }
        }
        return result;
    }

    private static final String SPACE = "   ";
    private static final int NUMBER_OF_INSTRUCTION_CHARS = 48;

    private void printHeading(PrintStream stream, int nOffsetChars, int nLabelChars)  {
        String s = Strings.padLengthWithSpaces("Address", (addressWidth().numberOfBytes() * 2) + 2) + SPACE;
        s += Strings.padLengthWithSpaces("+", nOffsetChars) + SPACE;
        s += Strings.padLengthWithSpaces(":", nLabelChars + 1) + SPACE;
        s += Strings.padLengthWithSpaces("Instruction", NUMBER_OF_INSTRUCTION_CHARS) + SPACE;
        s += "Bytes";
        stream.println(s);
        stream.println(Strings.times('-', s.length()));
    }

    private boolean _isHeadingEnabled;

    public void enableHeading() {
        _isHeadingEnabled = true;
    }

    public void disableHeading() {
        _isHeadingEnabled = false;
    }

    public void print(OutputStream outputStream, IndexedSequence<DisassembledInstruction_Type> disassembledInstructions, GlobalLabelMapper globalLabelMapper) throws IOException {
        final PrintStream stream = outputStream instanceof PrintStream ? (PrintStream) outputStream : new PrintStream(outputStream);
        final int nOffsetChars = Integer.toString(disassembledInstructions.last().startPosition()).length();
        final IndexedSequence<DisassembledLabel> labelMap = createLabelMap(disassembledInstructions);
        final Sequence<DisassembledLabel> labels = Sequence.Static.filterNonNull(labelMap);
        final int nLabelChars = updateLabels(labels, disassembledInstructions);
        if (_isHeadingEnabled) {
            printHeading(stream, nOffsetChars, nLabelChars);
        }
        for (int i = 0; i < disassembledInstructions.length(); i++) {
            final DisassembledInstruction_Type disassembledInstruction = disassembledInstructions.get(i);
            stream.print(disassembledInstruction.addressString());
            stream.print(SPACE);
            stream.printf("%0" + nOffsetChars + "d", disassembledInstruction.startPosition());
            stream.print(SPACE);
            if (labelMap.get(i) != null) {
                stream.print(Strings.padLengthWithSpaces(labelMap.get(i).name(), nLabelChars) + ":");
            } else {
                stream.print(Strings.spaces(nLabelChars) + " ");
            }
            stream.print(SPACE);
            stream.print(Strings.padLengthWithSpaces(disassembledInstruction.toString(labels, globalLabelMapper), NUMBER_OF_INSTRUCTION_CHARS));
            stream.print(SPACE);
            stream.print(DisassembledInstruction.toHexString(disassembledInstruction.bytes()));
            stream.println();
        }
    }

    public void print(OutputStream outputStream, IndexedSequence<DisassembledInstruction_Type> disassembledInstructions) throws IOException {
        print(outputStream, disassembledInstructions, null);
    }

    public void scanAndPrint(BufferedInputStream bufferedInputStream, OutputStream outputStream, GlobalLabelMapper globalLabelMapper) throws IOException, AssemblyException {
        final IndexedSequence<DisassembledInstruction_Type> disassembledInstructions = scan(bufferedInputStream);
        print(outputStream, disassembledInstructions, globalLabelMapper);
    }

    public void scanAndPrint(BufferedInputStream bufferedInputStream, OutputStream outputStream) throws IOException, AssemblyException {
        scanAndPrint(bufferedInputStream, outputStream, null);
    }

    public enum AbstractionPreference {
        RAW, SYNTHETIC;
    }

    private AbstractionPreference _abstractionPreference = AbstractionPreference.SYNTHETIC;

    protected AbstractionPreference abstractionPreference() {
        return _abstractionPreference;
    }

    public void setAbstractionPreference(AbstractionPreference abstractionPreference) {
        _abstractionPreference = abstractionPreference;
    }

    private int _expectedNumberOfArguments = -1;

    protected int expectedNumberOfArguments() {
        return _expectedNumberOfArguments;
    }

    public void setExpectedNumberOfArguments(int expectedNumberOfArguments) {
        _expectedNumberOfArguments = expectedNumberOfArguments;
    }

    /**
     * Gets the target code position denoted by an offset and a given instruction. The position is obtained by adding
     * {@code offset} to either the {@linkplain DisassembledInstruction#startPosition() start} or
     * {@linkplain DisassembledInstruction#endPosition() end} position of the given instruction depending on the
     * {@linkplain InstructionSet#relativeAddressing() relative addressing mode} of the current ISA.
     * 
     * @param disassembledInstruction
     *                a disassembled instruction
     * @param offset
     *                an offset denoting a target code position relative to {@code disassembledInstruction}
     * @return the target code position given by adding {@code offset} to {@code disassembledInstruction} position
     */
    public long getPositionFromInstructionRelativeOffset(DisassembledInstruction_Type disassembledInstruction, long offset) {
        switch (assembly().instructionSet().relativeAddressing()) {
            case FROM_INSTRUCTION_START:
                return disassembledInstruction.startPosition() + offset;
            case FROM_INSTRUCTION_END:
                return disassembledInstruction.endPosition() + offset;
        }
        ProgramError.unknownCase();
        return 0;
    }
}
