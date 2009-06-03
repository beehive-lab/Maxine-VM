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
package com.sun.max.asm.dis.ppc;

import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.asm.dis.risc.*;
import com.sun.max.asm.gen.risc.ppc.*;
import com.sun.max.collect.*;

/**
 * @author Bernd Mathiske
 */
public abstract class PPCDisassembledInstruction extends RiscDisassembledInstruction<PPCTemplate> {

    PPCDisassembledInstruction(Disassembler disassembler, int position, byte[] bytes, PPCTemplate template, IndexedSequence<Argument> arguments) {
        super(disassembler, position, bytes, template, arguments);
    }

    @Override
    public String mnemonic() {
        final PPCExternalInstruction instruction = new PPCExternalInstruction(template(), arguments(), startAddress(), null);
        return instruction.name();
    }

    @Override
    public String operandsToString(AddressMapper addressMapper) {
        final PPCExternalInstruction instruction = new PPCExternalInstruction(template(), arguments(), startAddress(), addressMapper);
        return instruction.operands();
    }

    @Override
    public String toString(AddressMapper addressMapper) {
        final PPCExternalInstruction instruction = new PPCExternalInstruction(template(), arguments(), startAddress(), addressMapper);
        return instruction.toString();
    }

}
