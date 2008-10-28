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
package com.sun.max.asm.gen.risc.sparc;

import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.asm.dis.sparc.*;
import com.sun.max.asm.gen.risc.*;
import com.sun.max.collect.*;

/**
 *
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class SPARCAssemblerGenerator extends RiscAssemblerGenerator<SPARCTemplate> {

    private SPARCAssemblerGenerator() {
        super(SPARCAssembly.ASSEMBLY);
    }

    @Override
    protected String getJavadocManualReference(SPARCTemplate template) {
        return "\"<a href=\"http://developers.sun.com/solaris/articles/sparcv9.pdf\">The SPARC Architecture Manual, Version 9</a> - Section " +
            template.instructionDescription().architectureManualSection() + "\"";
    }

    public static void main(String[] programArguments) {
        final SPARCAssemblerGenerator generator = new SPARCAssemblerGenerator();
        generator._options.parseArguments(programArguments);
        generator.generate();
    }

    @Override
    protected String generateExampleInstruction(SPARCTemplate template, IndexedSequence<Argument> arguments, AddressMapper addressMapper) {
        final byte[] bytes = {};
        final SPARCDisassembledInstruction dis = new SPARC64DisassembledInstruction(new SPARC64Disassembler(0, null), 0, bytes, template, arguments);
        return dis.toString(addressMapper);
    }
}
