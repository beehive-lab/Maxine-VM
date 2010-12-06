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
import java.lang.reflect.*;
import java.util.*;

import com.sun.max.*;
import com.sun.max.asm.*;
import com.sun.max.asm.InlineDataDescriptor.*;
import com.sun.max.asm.gen.*;
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
public abstract class Disassembler {

    private static final Map<String, Constructor> disassemblerConstructors = new HashMap<String, Constructor>();

    /**
     * Gets a constructor for instantiating a disassembler for a given {@linkplain ISA ISA}
     * and {@linkplain WordWidth word size}. If a non-null constructor is returned, it's signature
     * will be {@code (long, InlineDataDecoder)} or {@code (int, InlineDataDecoder)} depending on whether
     * {@code wordWidth} is {@link WordWidth#BITS_64} or {@link WordWidth#BITS_32}.
     *
     * @param isa an instruction set
     * @param wordWidth a word size
     * @return a disassembler for {@code isa} and {@code wordWidth} or {@code null} if none exists
     */
    public static Constructor getDisassemblerConstructor(ISA isa, WordWidth wordWidth) {
        String key = isa + " " + wordWidth;
        Constructor con = disassemblerConstructors.get(key);
        if (con == null) {
            // Try word-width specific class first:
            String packageName = Disassembler.class.getPackage().getName() + "." + isa.name().toLowerCase();
            String className = packageName + "." + isa.name() + wordWidth.numberOfBits + "Disassembler";
            Class<?> disasmClass = null;
            try {
                disasmClass = Class.forName(className);
            } catch (ClassNotFoundException e1) {
                className = packageName + "." + isa.name() + "Disassembler";
                try {
                    disasmClass = Class.forName(className);
                } catch (ClassNotFoundException e2) {
                    return null;
                }
            }
            try {
                if (wordWidth == WordWidth.BITS_64) {
                    con = disasmClass.getConstructor(long.class, InlineDataDecoder.class);
                } else {
                    assert wordWidth == WordWidth.BITS_32 : wordWidth;
                    con = disasmClass.getConstructor(int.class, InlineDataDecoder.class);
                }
            } catch (NoSuchMethodException e) {
                return null;
            }
            disassemblerConstructors.put(key, con);
        }
        return con;

    }

    /**
     * Gets a disassembler for a given {@linkplain ISA ISA} and {@linkplain WordWidth word size}
     * that can be used to disassemble an instruction stream located at a given address.
     *
     * @param isa an instruction set
     * @param wordWidth a word size
     * @param startAddress the start address at which the instruction stream to be disassembled is located
     * @param inlineDataDecoder used to decode any inline data in {@code code}. This value can be {@code null}.
     * @return the created disassembler
     * @throws IllegalArgumentException if no disassembler exists for {@code isa} and {@code wordWidth}
     */
    public static Disassembler createDisassembler(ISA isa, WordWidth wordWidth, long startAddress, InlineDataDecoder inlineDataDecoder) {
        Constructor con = getDisassemblerConstructor(isa, wordWidth);
        if (con == null) {
            throw new IllegalArgumentException("No disassembler is available for " + isa + " with word size " + wordWidth.numberOfBits);
        }

        try {
            return (Disassembler) con.newInstance(startAddress, inlineDataDecoder);
        } catch (Exception e) {
            throw (InternalError) new InternalError("Error invoking constructor " + con).initCause(e);
        }
    }

    /**
     * Prints a textual disassembly of some given machine code.
     *
     * @param out where to print the disassembly
     * @param code the machine code to be disassembled and printed
     * @param isa the instruction set
     * @param wordWidth the word width
     * @param startAddress the address at which {@code code} is located
     * @param inlineDataDecoder used to decode any inline data in {@code code}
     * @param disassemblyPrinter the printer utility to use for the printing. If {@code null}, then a new instance of
     *            {@link DisassemblyPrinter} is created and used.
     */
    public static void disassemble(OutputStream out, byte[] code, ISA isa, WordWidth wordWidth, long startAddress, InlineDataDecoder inlineDataDecoder, DisassemblyPrinter disassemblyPrinter) {
        if (code.length == 0) {
            return;
        }
        final Disassembler disassembler = createDisassembler(isa, wordWidth, startAddress, inlineDataDecoder);
        final BufferedInputStream stream = new BufferedInputStream(new ByteArrayInputStream(code));
        try {
            disassembler.scanAndPrint(stream, out, disassemblyPrinter);
        } catch (IOException ioException) {
            ProgramError.unexpected();
        } catch (AssemblyException assemblyException) {
            System.err.println(assemblyException);
        }
    }

    /**
     * Turn on the following flag in order to get debugging output.
     */
    public static final boolean TRACE = false;

    private final ImmediateArgument startAddress;
    private final Endianness endianness;

    protected Disassembler(ImmediateArgument startAddress, Endianness endianness, InlineDataDecoder inlineDataDecoder) {
        this.startAddress = startAddress;
        this.endianness = endianness;
        this.inlineDataDecoder = inlineDataDecoder;
    }

    public WordWidth addressWidth() {
        return startAddress.width();
    }

    public Endianness endianness() {
        return endianness;
    }

    protected int currentPosition;

    public void setCurrentPosition(int position) {
        currentPosition = position;
    }

    private final AddressMapper addressMapper = new AddressMapper();

    public AddressMapper addressMapper() {
        return addressMapper;
    }

    private final InlineDataDecoder inlineDataDecoder;

    public InlineDataDecoder inlineDataDecoder() {
        return inlineDataDecoder;
    }

    /**
     * Creates one or more disassembled data objects representing some given inline data.
     *
     * @param inlineData some inline data decoded by this disassembler's {@linkplain #inlineDataDecoder() inline data decoder}
     * @return a sequence of disassembled data objects representing {@code inlineData}
     */
    protected List<DisassembledData> createDisassembledDataObjects(final InlineData inlineData) {
        final InlineDataDescriptor descriptor = inlineData.descriptor();
        final int startPosition = descriptor.startPosition();
        switch (descriptor.tag()) {
            case BYTE_DATA: {
                final int size = inlineData.size();
                final String mnemonic = size == 1 ? ".byte" : ".bytes" + size;
                final ImmediateArgument address = startAddress().plus(startPosition);
                final DisassembledData disassembledData = new DisassembledData(address, startPosition, mnemonic, inlineData.data(), null) {
                    @Override
                    public String operandsToString(AddressMapper addrMapper) {
                        final byte[] data = inlineData.data();
                        return Bytes.toHexString(data, " ");
                    }
                    @Override
                    public String toString() {
                        return toString(addressMapper());
                    }
                };
                return Collections.singletonList(disassembledData);
            }
            case ASCII: {
                final String mnemonic = ".ascii";
                final ImmediateArgument address = startAddress().plus(startPosition);
                final DisassembledData disassembledData = new DisassembledData(address, startPosition, mnemonic, inlineData.data(), null) {
                    @Override
                    public String operandsToString(AddressMapper addrMapper) {
                        final byte[] asciiBytes = inlineData.data();
                        return '"' + new String(asciiBytes) + '"';
                    }
                    @Override
                    public String toString() {
                        return toString(addressMapper());
                    }
                };
                return Collections.singletonList(disassembledData);
            }
            case JUMP_TABLE32: {
                final JumpTable32 jumpTable32 = (JumpTable32) descriptor;
                final List<DisassembledData> result = new ArrayList<DisassembledData>(jumpTable32.numberOfEntries());

                int caseValue = jumpTable32.low();
                final InputStream stream = new ByteArrayInputStream(inlineData.data());
                final int jumpTable = startPosition;
                int casePosition = jumpTable;
                for (int i = 0; i < jumpTable32.numberOfEntries(); i++) {
                    try {
                        final int caseOffset = endianness().readInt(stream);
                        final byte[] caseOffsetBytes = endianness().toBytes(caseOffset);

                        final int targetPosition = jumpTable + caseOffset;
                        final ImmediateArgument targetAddress = startAddress().plus(targetPosition);
                        final String caseValueOperand = String.valueOf(caseValue);

                        final ImmediateArgument caseAddress = startAddress().plus(casePosition);
                        final DisassembledData disassembledData = new DisassembledData(caseAddress, casePosition, ".case", caseOffsetBytes, targetAddress) {
                            @Override
                            public String operandsToString(AddressMapper addrMapper) {
                                final DisassembledLabel label = addrMapper.labelAt(targetAddress);
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
                        result.add(disassembledData);
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
                final List<DisassembledData> result = new ArrayList<DisassembledData>(lookupTable32.numberOfEntries());

                final InputStream stream = new ByteArrayInputStream(inlineData.data());
                final int lookupTable = startPosition;
                int casePosition = lookupTable;
                for (int i = 0; i < lookupTable32.numberOfEntries(); i++) {
                    try {
                        final int caseValue = endianness().readInt(stream);
                        final int caseOffset = endianness().readInt(stream);

                        final byte[] caseBytes = new byte[8];
                        endianness().toBytes(caseValue, caseBytes, 0);
                        endianness().toBytes(caseOffset, caseBytes, 4);

                        final int targetPosition = lookupTable + caseOffset;
                        final ImmediateArgument caseAddress = startAddress().plus(casePosition);
                        final ImmediateArgument targetAddress = startAddress().plus(targetPosition);

                        final DisassembledData disassembledData = new DisassembledData(caseAddress, casePosition, ".case", caseBytes, targetAddress) {
                            @Override
                            public String operandsToString(AddressMapper addrMapper) {
                                final DisassembledLabel label = addrMapper.labelAt(targetAddress);
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
                        result.add(disassembledData);
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
    public final List<DisassembledObject> scanOne(BufferedInputStream stream) throws IOException, AssemblyException {
        final List<DisassembledObject> disassembledObjects = scanOne0(stream);
        if (!disassembledObjects.isEmpty()) {
            addressMapper.add(Utils.first(disassembledObjects));
        }
        return disassembledObjects;
    }

    /**
     * Does the actual scanning for {@link #scanOne(BufferedInputStream)}.
     */
    protected abstract List<DisassembledObject> scanOne0(BufferedInputStream stream) throws IOException, AssemblyException;

    /**
     * Scans an instruction stream and disassembles the encoded objects. If an encoded instruction has
     * more than one matching disassembled form, an arbitrary choice of one of the disassembled forms is
     * appended to the returned sequence.
     * <p>
     * The {@link #scanOne} method can be used to obtain all the disassembled forms
     * for each instruction in an instruction stream.
     */
    public final List<DisassembledObject> scan(BufferedInputStream stream) throws IOException, AssemblyException {
        final List<DisassembledObject> disassembledObjects = scan0(stream);
        addressMapper.add(disassembledObjects);
        return disassembledObjects;

    }

    /**
     * Does the actual scanning for {@link #scan(BufferedInputStream)}.
     */
    protected abstract List<DisassembledObject> scan0(BufferedInputStream stream) throws IOException, AssemblyException;

    protected final void scanInlineData(BufferedInputStream stream, List<DisassembledObject> disassembledObjects) throws IOException {
        if (inlineDataDecoder() != null) {
            InlineData inlineData;
            while ((inlineData = inlineDataDecoder().decode(currentPosition, stream)) != null) {
                currentPosition += addDisassembledDataObjects(disassembledObjects, inlineData);
            }
        }
    }

    protected int addDisassembledDataObjects(List<DisassembledObject> disassembledObjects, InlineData inlineData) {
        final List<DisassembledData> dataObjects = createDisassembledDataObjects(inlineData);
        for (DisassembledData dataObject : dataObjects) {
            disassembledObjects.add(dataObject);
        }
        return inlineData.size();
    }

    /**
     * The start address of the instruction stream decoded by this disassembler.
     */
    protected final ImmediateArgument startAddress() {
        return startAddress;
    }

    public void scanAndPrint(BufferedInputStream bufferedInputStream, OutputStream outputStream) throws IOException, AssemblyException {
        scanAndPrint(bufferedInputStream, outputStream, new DisassemblyPrinter(false));
    }

    public void scanAndPrint(BufferedInputStream bufferedInputStream, OutputStream outputStream, DisassemblyPrinter printer) throws IOException, AssemblyException {
        if (printer == null) {
            scanAndPrint(bufferedInputStream, outputStream);
        } else {
            final List<DisassembledObject> disassembledObjects = scan(bufferedInputStream);
            printer.print(this, outputStream, disassembledObjects);
        }
    }

    public enum AbstractionPreference {
        RAW, SYNTHETIC;
    }

    private AbstractionPreference abstractionPreference = AbstractionPreference.SYNTHETIC;

    protected AbstractionPreference abstractionPreference() {
        return abstractionPreference;
    }

    public void setAbstractionPreference(AbstractionPreference abstractionPreference) {
        this.abstractionPreference = abstractionPreference;
    }

    private int expectedNumberOfArguments = -1;

    protected int expectedNumberOfArguments() {
        return expectedNumberOfArguments;
    }

    public void setExpectedNumberOfArguments(int expectedNumberOfArguments) {
        this.expectedNumberOfArguments = expectedNumberOfArguments;
    }

    public abstract ImmediateArgument addressForRelativeAddressing(DisassembledInstruction di);
    public abstract String mnemonic(DisassembledInstruction di);
    public abstract String operandsToString(DisassembledInstruction di, AddressMapper addressMapper);
    public abstract String toString(DisassembledInstruction di, AddressMapper addressMapper);
}
