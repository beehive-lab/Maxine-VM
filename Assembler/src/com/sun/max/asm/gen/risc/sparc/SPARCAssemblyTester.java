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

import java.io.*;
import java.util.*;

import com.sun.max.asm.*;
import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.risc.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * @author Bernd Mathiske
 */
public abstract class SPARCAssemblyTester extends RiscAssemblyTester<RiscTemplate> {

    public SPARCAssemblyTester(SPARCAssembly assembly, WordWidth addressWidth, EnumSet<AssemblyTestComponent> components) {
        super(assembly, addressWidth, components);
    }

    @Override
    public SPARCAssembly assembly() {
        return (SPARCAssembly) super.assembly();
    }

    @Override
    protected String assemblerCommand() {
        return "as -xarch=v9a";
    }

    private RiscTemplate lastTemplate;

    @Override
    protected void assembleExternally(IndentWriter writer, RiscTemplate template, List<Argument> argumentList, String label) {

        // This is a workaround for SPARC V9 ABI compliance checks: http://developers.sun.com/solaris/articles/sparcv9abi.html
        if (lastTemplate == null || template != lastTemplate) {
            writer.println(".register %g2,#scratch");
            writer.println(".register %g3,#scratch");
            writer.println(".register %g6,#scratch");
            writer.println(".register %g7,#scratch");
            lastTemplate = template;
        }
        final RiscExternalInstruction instruction = new RiscExternalInstruction(template, argumentList);
        writer.println(instruction.toString());
        writer.println("nop"); // fill potential DCTI slot with something - see below
    }

    @Override
    protected boolean readNop(InputStream stream) throws IOException {
        final int instruction = Endianness.BIG.readInt(stream);
        return instruction == 0x01000000;
    }

    @Override
    protected byte[] readExternalInstruction(PushbackInputStream externalInputStream, RiscTemplate template, byte[] internalBytes) throws IOException {
        final byte[] result = super.readExternalInstruction(externalInputStream, template, internalBytes);
        if (!readNop(externalInputStream)) { // read potential DCTI slot place holder contents - see above
            ProgramError.unexpected("nop missing after external instruction");
        }
        return result;
    }
}
