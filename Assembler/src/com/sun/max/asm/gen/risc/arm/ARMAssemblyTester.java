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

package com.sun.max.asm.gen.risc.arm;

import java.io.*;
import java.util.*;

import com.sun.max.asm.*;
import com.sun.max.asm.arm.complete.*;
import com.sun.max.asm.dis.arm.*;
import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.risc.*;
import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;

/**
 *
 * @author Sumeet Panchal
 */

public class ARMAssemblyTester extends RiscAssemblyTester<RiscTemplate> {

    public ARMAssemblyTester(ARMAssembly assembly, WordWidth addressWidth, EnumSet<AssemblyTestComponent> components) {
        super(assembly, addressWidth, components);
    }

    @Override
    public ARMAssembly assembly() {
        return (ARMAssembly) super.assembly();
    }

    @Override
    protected String assemblerCommand() {
        return "as -EB";
    }

    @Override
    protected void assembleExternally(IndentWriter writer, RiscTemplate template, Sequence<Argument> argumentList, String label) {
        final RiscExternalInstruction instruction = new RiscExternalInstruction(template, argumentList);
        writer.println(instruction.toString());
    }

    @Override
    protected boolean readNop(InputStream stream) throws IOException {
        final int instruction = Endianness.BIG.readInt(stream);
        return instruction == 0xe1a00000;
    }

    @Override
    protected Assembler createTestAssembler() {
        return new ARMAssembler(0);
    }

    @Override
    protected ARMDisassembler createTestDisassembler() {
        return new ARMDisassembler(0, null);
    }

}
