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
import com.sun.max.vm.compiler.target.*;

/**
 * @author Bernd Mathiske
 */
public final class Disassemble {

    @PROTOTYPE_ONLY
    private Disassemble() {
    }

    @PROTOTYPE_ONLY
    private static Disassembler createDisassembler(ProcessorKind processorKind, Address startAddress) {
        switch (processorKind.instructionSet()) {
            case ARM:
                return new ARMDisassembler(startAddress.toInt());
            case AMD64:
                return new AMD64Disassembler(startAddress.toLong());
            case IA32:
                return new IA32Disassembler(startAddress.toInt());
            case PPC:
                if (processorKind.dataModel().wordWidth() == WordWidth.BITS_64) {
                    return new PPC64Disassembler(startAddress.toLong());
                }
                return new PPC32Disassembler(startAddress.toInt());
            case SPARC:
                if (processorKind.dataModel().wordWidth() == WordWidth.BITS_64) {
                    return new SPARC64Disassembler(startAddress.toLong());
                }
                return new SPARC32Disassembler(startAddress.toInt());
        }
        ProgramError.unknownCase();
        return null;
    }

    @PROTOTYPE_ONLY
    public static void targetMethod(TargetMethod targetMethod) {
        final ProcessorKind processorKind = targetMethod.compilerScheme().vmConfiguration().platform().processorKind();
        final Disassembler disassembler = createDisassembler(processorKind, targetMethod.codeStart());
        final BufferedInputStream stream = new BufferedInputStream(new ByteArrayInputStream(targetMethod.code()));
        try {
            disassembler.scanAndPrint(stream, System.out);
        } catch (IOException ioException) {
            ProgramError.unexpected();
        } catch (AssemblyException assemblyException) {
            System.err.println(assemblyException);
        }
    }

}
