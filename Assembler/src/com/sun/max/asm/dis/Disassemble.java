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

import com.sun.max.asm.*;
import com.sun.max.asm.dis.amd64.*;
import com.sun.max.asm.dis.arm.*;
import com.sun.max.asm.dis.ia32.*;
import com.sun.max.asm.dis.ppc.*;
import com.sun.max.asm.dis.sparc.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * @author Bernd Mathiske
 */
public final class Disassemble {

    private Disassemble() {
    }

    public static Disassembler createDisassembler(InstructionSet instructionSet, WordWidth wordWidth, long startAddress, InlineDataDecoder inlineDataDecoder) {
        switch (instructionSet) {
            case ARM:
                return new ARMDisassembler((int)startAddress, inlineDataDecoder);
            case AMD64:
                return new AMD64Disassembler(startAddress, inlineDataDecoder);
            case IA32:
                return new IA32Disassembler((int)startAddress, inlineDataDecoder);
            case PPC:
                if (wordWidth == WordWidth.BITS_64) {
                    return new PPC64Disassembler(startAddress, inlineDataDecoder);
                }
                return new PPC32Disassembler((int)startAddress, inlineDataDecoder);
            case SPARC:
                if (wordWidth == WordWidth.BITS_64) {
                    return new SPARC64Disassembler(startAddress, inlineDataDecoder);
                }
                return new SPARC32Disassembler((int)startAddress, inlineDataDecoder);
        }
        ProgramError.unknownCase();
        return null;
    }

    /**
     * Prints a textual disassembly of some given machine code.
     *
     * @param out where to print the disassembly
     * @param code the machine code to be disassembled and printed
     * @param instructionSet the instruction set
     * @param wordWidth the word width
     * @param startAddress the address at which {@code code} is located
     * @param inlineDataDecoder used to decode any inline date in {@code code}
     * @param disassemblyPrinter the printer utility to use for the printing. If {@code null}, then a new instance of
     *            {@link DisassemblyPrinter} is created and used.
     */
    public static void disassemble(OutputStream out, byte[] code, InstructionSet instructionSet, WordWidth wordWidth, long startAddress, InlineDataDecoder inlineDataDecoder, DisassemblyPrinter disassemblyPrinter) {
        if (code.length == 0) {
            return;
        }
        final Disassembler disassembler = createDisassembler(instructionSet, wordWidth, startAddress, inlineDataDecoder);
        final BufferedInputStream stream = new BufferedInputStream(new ByteArrayInputStream(code));
        try {
            disassembler.scanAndPrint(stream, out, disassemblyPrinter);
        } catch (IOException ioException) {
            ProgramError.unexpected();
        } catch (AssemblyException assemblyException) {
            System.err.println(assemblyException);
        }
    }

}
