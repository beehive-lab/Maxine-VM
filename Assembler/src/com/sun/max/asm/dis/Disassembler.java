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
package com.sun.max.asm.dis;

import java.io.*;
import java.util.*;

import com.sun.max.asm.*;
import com.sun.max.asm.InlineDataDescriptor.*;
import com.sun.max.asm.gen.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * Disassemblers scan machine code, discern and decode individual instructions and inline data objects and represent
 * them in an abstract representation, which they can then render in human-readable form, an assembly listing.
 *
 * @author Bernd Mathiske
 * @author Greg Wright
 * @author Doug Simon
 */
public abstract class Disassembler<Template_Type extends Template, DisassembledInstruction_Type extends DisassembledInstruction<Template_Type>> {

    private final ImmediateArgument _startAddress;
    private final Assembly<Template_Type> _assembly;
    private final Endianness _endianness;

    protected Disassembler(ImmediateArgument startAddress, Assembly<Template_Type> assembly, Endianness endianness, InlineDataDecoder inlineDataDecoder) {
        _startAddress = startAddress;
        _assembly = assembly;
        _endianness = endianness;
        _inlineDataDecoder = inlineDataDecoder;
    }

    public Assembly<Template_Type> assembly() {
        return _assembly;
    }

    public WordWidth addressWidth() {
        return _startAddress.width();
    }

    public Endianness endianness() {
        return _endianness;
    }

    protected int _currentPosition;

    public void setCurrentPosition(int position) {
        _currentPosition = position;
    }

    private final AddressMapper _addressMapper = new AddressMapper();

    public AddressMapper addressMapper() {
        return _addressMapper;
    }

    private final InlineDataDecoder _inlineDataDecoder;

    public InlineDataDecoder inlineDataDecoder() {
        return _inlineDataDecoder;
    }

    /**
     * Creates a disassembled instruction based on a given sequence of bytes, a template and a set of arguments. The
     * caller has performed the necessary decoding of the bytes to derive the template and arguments.
     *
     * @param position the position an instruction stream from which the bytes were read
     * @param bytes the bytes of an instruction
     * @param template the template that corresponds to the instruction encoded in {@code bytes}
     * @param arguments the arguments of the instruction encoded in {@code bytes}
     * @return a disassembled instruction representing the result of decoding {@code bytes} into an instruction
     */
    protected abstract DisassembledInstruction_Type createDisassembledInstruction(int position, byte[] bytes, Template_Type template, IndexedSequence<Argument> arguments);

    /**
     * Creates one or more disassembled data objects representing some given inline data.
     *
     * @param inlineData some inline data decoded by this disassembler's {@linkplain #inlineDataDecoder() inline data decoder}
     * @return a sequence of disassembled data objects representing {@code inlineData}
     */
    protected IterableWithLength<DisassembledData> createDisassembledDataObjects(final InlineData inlineData) {
        final InlineDataDescriptor descriptor = inlineData.descriptor();
        final int startPosition = descriptor.startPosition();
        switch (descriptor.tag()) {
            case BYTE_DATA: {
                final int size = inlineData.size();
                final String mnemonic = size == 1 ? ".byte" : ".bytes" + size;
                final ImmediateArgument address = startAddress().plus(startPosition);
                final DisassembledData disassembledData = new DisassembledData(address, startPosition, mnemonic, inlineData.data(), null) {
                    @Override
                    public String operandsToString(AddressMapper addressMapper) {
                        final byte[] data = inlineData.data();
                        return Bytes.toHexString(data, " ");
                    }
                    @Override
                    public String toString() {
                        return toString(addressMapper());
                    }
                };
                return Iterables.toIterableWithLength(Collections.singleton(disassembledData));
            }
            case JUMP_TABLE32: {
                final JumpTable32 jumpTable32 = (JumpTable32) descriptor;
                final AppendableSequence<DisassembledData> result = new ArrayListSequence<DisassembledData>(jumpTable32.numberOfEntries());

                int caseValue = jumpTable32.low();
                final InputStream stream = new ByteArrayInputStream(inlineData.data());
                final int jumpTable = startPosition;
                int casePosition = jumpTable;
                for (int i = 0; i < jumpTable32.numberOfEntries(); i++) {
                    try {
                        final Endianness endianness = endianness();
                        final int caseOffset = endianness.readInt(stream);
                        final byte[] caseOffsetBytes = endianness.toBytes(caseOffset);

                        final int targetPosition = jumpTable + caseOffset;
                        final ImmediateArgument targetAddress = startAddress().plus(targetPosition);
                        final String caseValueOperand = String.valueOf(caseValue);

                        final ImmediateArgument caseAddress = startAddress().plus(casePosition);
                        final DisassembledData disassembledData = new DisassembledData(caseAddress, casePosition, ".case", caseOffsetBytes, targetAddress) {
                            @Override
                            public String operandsToString(AddressMapper addressMapper) {
                                final DisassembledLabel label = addressMapper.labelAt(targetAddress);
                                String s = caseValueOperand + ", ";
                                if (label != null) {
                                    s += label.name() + ": ";
                                }
                                if (caseOffset >= 0) {
                                    s += "+";
                                }
                                return s + caseOffset;
                            }
                            @Override
                            public String toString() {
                                return toString(addressMapper());
                            }
                        };
                        result.append(disassembledData);
                        casePosition += 4;
                        caseValue++;
                    } catch (IOException ioException) {
                        throw ProgramError.unexpected(ioException);
                    }
                }
                assert casePosition == descriptor.endPosition();
                return result;
            }
            case LOOKUP_TABLE32: {
                final LookupTable32 lookupTable32 = (LookupTable32) descriptor;
                final AppendableSequence<DisassembledData> result = new ArrayListSequence<DisassembledData>(lookupTable32.numberOfEntries());

                final InputStream stream = new ByteArrayInputStream(inlineData.data());
                final int lookupTable = startPosition;
                int casePosition = lookupTable;
                for (int i = 0; i < lookupTable32.numberOfEntries(); i++) {
                    try {
                        final Endianness endianness = endianness();
                        final int caseValue = endianness.readInt(stream);
                        final int caseOffset = endianness.readInt(stream);

                        final byte[] caseBytes = new byte[8];
                        endianness.toBytes(caseValue, caseBytes, 0);
                        endianness.toBytes(caseOffset, caseBytes, 4);

                        final int targetPosition = lookupTable + caseOffset;
                        final ImmediateArgument caseAddress = startAddress().plus(casePosition);
                        final ImmediateArgument targetAddress = startAddress().plus(targetPosition);

                        final DisassembledData disassembledData = new DisassembledData(caseAddress, casePosition, ".case", caseBytes, targetAddress) {
                            @Override
                            public String operandsToString(AddressMapper addressMapper) {
                                final DisassembledLabel label = addressMapper.labelAt(targetAddress);
                                String s = caseValue + ", ";
                                if (label != null) {
                                    s += label.name() + ": ";
                                }
                                if (caseOffset >= 0) {
                                    s += "+";
                                }
                                return s + caseOffset;
                            }
                            @Override
                            public String toString() {
                                return toString(addressMapper());
                            }
                        };
                        result.append(disassembledData);
                        casePosition += 8;
                    } catch (IOException ioException) {
                        throw ProgramError.unexpected(ioException);
                    }
                }
                assert casePosition == descriptor.endPosition();
                return result;
            }
        }
        throw ProgramError.unknownCase(descriptor.tag().toString());
    }

    /**
     * Creates an assembler that will start assembling at {@code this.startAddress() + position}.
     */
    protected abstract Assembler createAssembler(int position);

    /**
     * Scans an instruction stream and disassembles the first encoded instruction.
     * <p>
     * @return the disassembled forms that match the first encoded instruction in {@code stream}
     */
    public final Sequence<DisassembledObject> scanOne(BufferedInputStream stream) throws IOException, AssemblyException {
        final Sequence<DisassembledObject> disassembledObjects = scanOne0(stream);
        if (!disassembledObjects.isEmpty()) {
            _addressMapper.add(disassembledObjects.first());
        }
        return disassembledObjects;
    }

    /**
     * Does the actual scanning for {@link #scanOne(BufferedInputStream)}.
     */
    protected abstract Sequence<DisassembledObject> scanOne0(BufferedInputStream stream) throws IOException, AssemblyException;

    /**
     * Scans an instruction stream and disassembles the encoded objects. If an encoded instruction has
     * more than one matching disassembled form, an arbitrary choice of one of the disassembled forms is
     * appended to the returned sequence.
     * <p>
     * The {@link #scanOne} method can be used to obtain all the disassembled forms
     * for each instruction in an instruction stream.
     */
    public final IndexedSequence<DisassembledObject> scan(BufferedInputStream stream) throws IOException, AssemblyException {
        final IndexedSequence<DisassembledObject> disassembledObjects = scan0(stream);
        _addressMapper.add(disassembledObjects);
        return disassembledObjects;

    }

    /**
     * Does the actual scanning for {@link #scan(BufferedInputStream)}.
     */
    public abstract IndexedSequence<DisassembledObject> scan0(BufferedInputStream stream) throws IOException, AssemblyException;

    protected final void scanInlineData(BufferedInputStream stream, AppendableIndexedSequence<DisassembledObject> disassembledObjects) throws IOException {
        if (inlineDataDecoder() != null) {
            InlineData inlineData;
            while ((inlineData = inlineDataDecoder().decode(_currentPosition, stream)) != null) {
                _currentPosition += addDisassembledDataObjects(disassembledObjects, inlineData);
            }
        }
    }

    protected int addDisassembledDataObjects(AppendableIndexedSequence<DisassembledObject> disassembledObjects, InlineData inlineData) {
        final IterableWithLength<DisassembledData> dataObjects = createDisassembledDataObjects(inlineData);
        for (DisassembledData dataObject : dataObjects) {
            disassembledObjects.append(dataObject);
        }
        return inlineData.size();
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

    public String addressString(DisassembledObject disassembledObject) {
        final String format = "0x%0" + addressWidth().numberOfBytes() + "X";
        return String.format(format, disassembledObject.startAddress().asLong());
    }

    /**
     * The start address of the instruction stream decoded by this disassembler.
     */
    protected final ImmediateArgument startAddress() {
        return _startAddress;
    }

    /**
     * Prints a disassembly for a given sequence of disassembled objects.
     *
     * @param outputStream the stream to which the disassembly wil be printed
     * @param disassembledObjects the disassembled objects to be printed
     * @param addressMapper an object that can provide symbol names/label for (global) addresses
     */
    public void print(OutputStream outputStream, Sequence<DisassembledObject> disassembledObjects) throws IOException {
        final PrintStream stream = outputStream instanceof PrintStream ? (PrintStream) outputStream : new PrintStream(outputStream);
        final int nOffsetChars = Integer.toString(disassembledObjects.last().startPosition()).length();
        final int nLabelChars = _addressMapper.maximumLabelNameLength();
        if (_isHeadingEnabled) {
            printHeading(stream, nOffsetChars, nLabelChars);
        }
        for (DisassembledObject disassembledObject : disassembledObjects) {
            stream.print(addressString(disassembledObject));
            stream.print(SPACE);
            stream.printf("%0" + nOffsetChars + "d", disassembledObject.startPosition());
            stream.print(SPACE);
            final DisassembledLabel label = _addressMapper.labelAt(disassembledObject.startAddress());
            if (label != null) {
                stream.print(Strings.padLengthWithSpaces(label.name(), nLabelChars) + ":");
            } else {
                stream.print(Strings.spaces(nLabelChars) + " ");
            }
            stream.print(SPACE);
            stream.print(Strings.padLengthWithSpaces(disassembledObject.toString(_addressMapper), NUMBER_OF_INSTRUCTION_CHARS));
            stream.print(SPACE);
            stream.print(DisassembledInstruction.toHexString(disassembledObject.bytes()));
            stream.println();
        }
    }

    public void scanAndPrint(BufferedInputStream bufferedInputStream, OutputStream outputStream) throws IOException, AssemblyException {
        final IndexedSequence<DisassembledObject> disassembledObjects = scan(bufferedInputStream);
        print(outputStream, disassembledObjects);
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
}
