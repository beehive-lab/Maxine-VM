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
package com.sun.max.vm.debug;

import java.io.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.asm.dis.amd64.*;
import com.sun.max.asm.dis.arm.*;
import com.sun.max.asm.dis.ia32.*;
import com.sun.max.asm.dis.ppc.*;
import com.sun.max.asm.dis.sparc.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.target.*;

/**
 * @author Bernd Mathiske
 */
@HOSTED_ONLY
public final class Disassemble {

    private Disassemble() {
    }

    public static Disassembler createDisassembler(ProcessorKind processorKind, Address startAddress, InlineDataDecoder inlineDataDecoder) {
        switch (processorKind.instructionSet) {
            case ARM:
                return new ARMDisassembler(startAddress.toInt(), inlineDataDecoder);
            case AMD64:
                return new AMD64Disassembler(startAddress.toLong(), inlineDataDecoder);
            case IA32:
                return new IA32Disassembler(startAddress.toInt(), inlineDataDecoder);
            case PPC:
                if (processorKind.dataModel.wordWidth == WordWidth.BITS_64) {
                    return new PPC64Disassembler(startAddress.toLong(), inlineDataDecoder);
                }
                return new PPC32Disassembler(startAddress.toInt(), inlineDataDecoder);
            case SPARC:
                if (processorKind.dataModel.wordWidth == WordWidth.BITS_64) {
                    return new SPARC64Disassembler(startAddress.toLong(), inlineDataDecoder);
                }
                return new SPARC32Disassembler(startAddress.toInt(), inlineDataDecoder);
        }
        ProgramError.unknownCase();
        return null;
    }

    /**
     * Prints a textual disassembly of some given machine code.
     *
     * @param out where to print the disassembly
     * @param code the machine code to be disassembled and printed
     * @param processorKind the kind of the processor on which {@code code} executes
     * @param startAddress the address at which {@code code} is located
     * @param inlineDataDecoder used to decode any inline date in {@code code}
     * @param disassemblyPrinter the printer utility to use for the printing. If {@code null}, then a new instance of
     *            {@link DisassemblyPrinter} is created and used.
     */
    public static void disassemble(OutputStream out, byte[] code, ProcessorKind processorKind, Address startAddress, InlineDataDecoder inlineDataDecoder, DisassemblyPrinter disassemblyPrinter) {
        final Disassembler disassembler = createDisassembler(processorKind, startAddress, inlineDataDecoder);
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
     * Prints a textual disassembly the code in a target method.
     *
     * @param out where to print the disassembly
     * @param targetMethod the target method whose code is to be disassembled
     */
    public static void disassemble(OutputStream out, final TargetMethod targetMethod) {
        final ProcessorKind processorKind = Platform.target().processorKind;
        final InlineDataDecoder inlineDataDecoder = InlineDataDecoder.createFrom(targetMethod.encodedInlineDataDescriptors());
        final Pointer startAddress = targetMethod.codeStart();
        final DisassemblyPrinter disassemblyPrinter = new DisassemblyPrinter(false) {
            @Override
            protected String disassembledObjectString(Disassembler disassembler, DisassembledObject disassembledObject) {
                String string = super.disassembledObjectString(disassembler, disassembledObject);
                if (string.startsWith("call ")) {

                    final Pointer instructionPointer = startAddress.plus(disassembledObject.startPosition());
                    BytecodeLocation bytecodeLocation = targetMethod.getBytecodeLocationFor(instructionPointer, false);
                    if (bytecodeLocation != null) {
                        final MethodRefConstant methodRef = bytecodeLocation.getCalleeMethodRef();
                        if (methodRef != null) {
                            final ConstantPool pool = bytecodeLocation.classMethodActor.codeAttribute().constantPool;
                            string += " [" + methodRef.holder(pool).toJavaString(false) + "." + methodRef.name(pool) + methodRef.signature(pool).toJavaString(false, false) + "]";
                        }
                    }
                    if (StopPositions.isNativeFunctionCallPosition(targetMethod.stopPositions(),  disassembledObject.startPosition())) {
                        string += " <native function call>";
                    }
                }
                return string;
            }
        };
        disassemble(out, targetMethod.code(), processorKind, startAddress, inlineDataDecoder, disassemblyPrinter);
    }
}
