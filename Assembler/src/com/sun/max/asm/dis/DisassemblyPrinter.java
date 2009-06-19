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

import com.sun.max.collect.*;
import com.sun.max.lang.*;

/**
 * Utility for printing a textual listing from a set of {@link DisassembledObject}s.
 *
 * @author Doug Simon
 */
public class DisassemblyPrinter {

    protected final boolean includeHeader;

    public DisassemblyPrinter(boolean includeHeader) {
        this.includeHeader = includeHeader;
    }

    public static final String SPACE = "   ";
    public static final int NUMBER_OF_INSTRUCTION_CHARS = 48;

    /**
     * Prints a disassembly for a given sequence of disassembled objects.
     *
     * @param outputStream the stream to which the disassembly wil be printed
     * @param disassembledObjects the disassembled objects to be printed
     */
    public void print(Disassembler disassembler, OutputStream outputStream, Sequence<DisassembledObject> disassembledObjects) throws IOException {
        final PrintStream stream = outputStream instanceof PrintStream ? (PrintStream) outputStream : new PrintStream(outputStream);
        final int nOffsetChars = Integer.toString(disassembledObjects.last().startPosition()).length();
        final int nLabelChars = disassembler.addressMapper().maximumLabelNameLength();
        if (includeHeader) {
            printHeading(disassembler, stream, nOffsetChars, nLabelChars);
        }
        for (DisassembledObject disassembledObject : disassembledObjects) {
            stream.print(addressString(disassembler, disassembledObject));
            stream.print(SPACE);
            stream.printf("%0" + nOffsetChars + "d", disassembledObject.startPosition());
            stream.print(SPACE);
            final DisassembledLabel label = disassembler.addressMapper().labelAt(disassembledObject.startAddress());
            if (label != null) {
                stream.print(Strings.padLengthWithSpaces(label.name(), nLabelChars) + ":");
            } else {
                stream.print(Strings.spaces(nLabelChars) + " ");
            }
            stream.print(SPACE);
            stream.print(Strings.padLengthWithSpaces(disassembledObjectString(disassembler, disassembledObject), NUMBER_OF_INSTRUCTION_CHARS));
            stream.print(SPACE);
            stream.print(DisassembledInstruction.toHexString(disassembledObject.bytes()));
            stream.println();
        }
    }

    protected void printHeading(Disassembler disassembler, PrintStream stream, int nOffsetChars, int nLabelChars)  {
        String s = Strings.padLengthWithSpaces("Address", (disassembler.addressWidth().numberOfBytes() * 2) + 2) + SPACE;
        s += Strings.padLengthWithSpaces("+", nOffsetChars) + SPACE;
        s += Strings.padLengthWithSpaces(":", nLabelChars + 1) + SPACE;
        s += Strings.padLengthWithSpaces("Instruction", NUMBER_OF_INSTRUCTION_CHARS) + SPACE;
        s += "Bytes";
        stream.println(s);
        stream.println(Strings.times('-', s.length()));
    }

    protected String addressString(Disassembler disassembler, DisassembledObject disassembledObject) {
        final String format = "0x%0" + disassembler.addressWidth().numberOfBytes() + "X";
        return String.format(format, disassembledObject.startAddress().asLong());
    }

    protected String disassembledObjectString(Disassembler disassembler, DisassembledObject disassembledObject) {
        return disassembledObject.toString(disassembler.addressMapper());
    }
}
