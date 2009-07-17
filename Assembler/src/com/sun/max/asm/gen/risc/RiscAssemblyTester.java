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
package com.sun.max.asm.gen.risc;

import java.io.*;
import java.util.*;

import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.asm.gen.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;

/**
 * @author Bernd Mathiske
 */
public abstract class RiscAssemblyTester<Template_Type extends RiscTemplate, DisassembledInstruction_Type extends DisassembledInstruction<Template_Type>>
                          extends AssemblyTester<Template_Type, DisassembledInstruction_Type> {

    public RiscAssemblyTester(Assembly<Template_Type> assembly, WordWidth addressWidth, EnumSet<AssemblyTestComponent> components) {
        super(assembly, addressWidth, components);
    }

    @Override
    protected byte[] readExternalInstruction(PushbackInputStream externalInputStream, Template_Type template, byte[] internalBytes) throws IOException {
        final byte[] result = new byte[4];
        if (externalInputStream.read(result) != 4) {
            throw new IOException("end of file before end of instruction");
        }
        return result;
    }

    @Override
    protected String disassembleFields(Template_Type template, byte[] assembledInstruction) {
        return new RiscFieldDisassembler<Template_Type>(template, assembledInstruction).toString();
    }

    @Override
    protected boolean isLegalArgumentList(Template_Type template, IndexedSequence<Argument> arguments) {
        final Sequence<InstructionConstraint> constraints = template.instructionDescription().constraints();
        for (InstructionConstraint constraint : constraints) {
            if (!(constraint.check(template, arguments))) {
                return false;
            }
        }
        return true;
    }

}
