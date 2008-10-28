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
package com.sun.max.asm.gen.risc.ppc;


import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.asm.dis.ppc.*;
import com.sun.max.asm.gen.risc.*;
import com.sun.max.collect.*;

/**
 * The program entry point for the PowerPC assembler generator.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class PPCAssemblerGenerator extends RiscAssemblerGenerator<PPCTemplate> {

    private PPCAssemblerGenerator() {
        super(PPCAssembly.ASSEMBLY);
    }

    @Override
    protected String getJavadocManualReference(PPCTemplate template) {
        String section = template.instructionDescription().architectureManualSection();
        if (section.indexOf("[Book ") == -1) {
            section += " [Book 1]";
        }
        return "\"<a href=\"http://www.ibm.com/developerworks/eserver/library/es-archguide-v2.html\">PowerPC Architecture Book, Version 2.02</a> - Section " + section + "\"";
    }

    public static void main(String[] programArguments) {
        final PPCAssemblerGenerator generator = new PPCAssemblerGenerator();
        generator._options.parseArguments(programArguments);
        generator.generate();
    }

    @Override
    protected String generateExampleInstruction(PPCTemplate template, IndexedSequence<Argument> arguments, AddressMapper addressMapper) {
        final byte[] bytes = {};
        final PPCDisassembledInstruction dis = new PPC64DisassembledInstruction(new PPC64Disassembler(0, null), 0, bytes, template, arguments);
        return dis.toString(addressMapper);
    }
}
