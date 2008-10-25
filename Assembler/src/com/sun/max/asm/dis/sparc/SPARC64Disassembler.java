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
package com.sun.max.asm.dis.sparc;

import com.sun.max.asm.*;
import com.sun.max.asm.gen.risc.sparc.*;
import com.sun.max.asm.sparc.complete.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;

/**
 *
 *
 * @author Bernd Mathiske
 */
public class SPARC64Disassembler extends SPARCDisassembler<SPARC64DisassembledInstruction> {

    private final long _startAddress;

    public SPARC64Disassembler(long startAddress, InlineDataDecoder inlineDataDecoder) {
        super(SPARCAssembly.ASSEMBLY, WordWidth.BITS_64, inlineDataDecoder);
        _startAddress = startAddress;
    }

    @Override
    public Class<SPARC64DisassembledInstruction> disassembledInstructionType() {
        return SPARC64DisassembledInstruction.class;
    }

    @Override
    protected SPARC64DisassembledInstruction createDisassembledInstruction(int position, byte[] bytes, SPARCTemplate template, IndexedSequence<Argument> arguments) {
        return new SPARC64DisassembledInstruction(this, position, bytes, template, arguments);
    }

    @Override
    protected Assembler createAssembler(int position) {
        return new SPARC64Assembler(_startAddress + position);
    }

    @Override
    protected long startAddress() {
        return _startAddress;
    }
}
