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

import java.io.*;
import java.util.*;

import com.sun.max.asm.*;
import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.risc.*;
import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;

/**
 * @author Bernd Mathiske
 */
public abstract class PPCAssemblyTester extends RiscAssemblyTester<RiscTemplate> {

    public PPCAssemblyTester(PPCAssembly assembly, WordWidth addressWidth, EnumSet<AssemblyTestComponent> components) {
        super(assembly, addressWidth, components);
    }

    @Override
    public PPCAssembly assembly() {
        return (PPCAssembly) super.assembly();
    }

    @Override
    protected String assemblerCommand() {
        return "as -force_cpusubtype_ALL";
    }

    @Override
    protected void assembleExternally(IndentWriter writer, RiscTemplate template, Sequence<Argument> argumentList, String label) {
        final RiscExternalInstruction instruction = new RiscExternalInstruction(template, argumentList);
        writer.println(instruction.toString());
    }

    @Override
    protected boolean readNop(InputStream stream) throws IOException {
        final int instruction = Endianness.BIG.readInt(stream);
        return instruction == 0x60000000;
    }

    @Override
    protected byte[] readExternalInstruction(PushbackInputStream externalInputStream, RiscTemplate template, byte[] internalBytes) throws IOException {
        final byte[] result = super.readExternalInstruction(externalInputStream, template, internalBytes);

        // Work-around for bug in Apple's version of the GNU 'as' assembler (see javadoc for 'isExternalMTCRFEncodingBroken' for more details)
        if (assembly().isExternalMTCRFEncodingBroken()) {
            if (template.externalName().equals("mtcrf")) {
                // Force 11th bit to 0
                final int bit11Mask = Integer.parseInt("11101111", 2);
                result[1] &= bit11Mask;
            } else if (template.externalName().equals("mtocrf")) {
                // Force 11th bit to 1
                final int bit11Mask = Integer.parseInt("00010000", 2);
                result[1] |= bit11Mask;
            }
        }
        return result;
    }
}
