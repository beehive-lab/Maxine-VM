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

package com.sun.max.asm.dis.arm;

import com.sun.max.asm.*;
import com.sun.max.asm.arm.complete.*;
import com.sun.max.asm.dis.risc.*;
import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.risc.arm.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;

/**
 * @author Sumeet Panchal
 */
public class ARMDisassembler extends RiscDisassembler<ARMTemplate, ARMDisassembledInstruction>{

    private final int _startAddress;

    public ARMDisassembler(int startAddress) {
        super(ARMAssembly.ASSEMBLY, WordWidth.BITS_32, Endianness.BIG);
        _startAddress = startAddress;
    }

    @Override
    protected Assembler createAssembler(int position) {
        return new ARMAssembler(_startAddress + position);
    }

    @Override
    public Class<ARMDisassembledInstruction> disassembledInstructionType() {
        return ARMDisassembledInstruction.class;
    }

    @Override
    protected ARMDisassembledInstruction createDisassembledInstruction(int position, byte[] bytes, ARMTemplate template, IndexedSequence<Argument> arguments) {
        return new ARMDisassembledInstruction(_startAddress, position, bytes, template, arguments);
    }

    @Override
    protected ARMTemplate createInlineDataTemplate(InstructionDescription instructionDescription) {
        return new ARMTemplate(instructionDescription);
    }
}
