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
package com.sun.max.asm.dis.ia32;

import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.asm.dis.x86.*;
import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.cisc.ia32.*;
import com.sun.max.collect.*;

/**
 * @author Bernd Mathiske
 */
public class IA32DisassembledInstruction extends X86DisassembledInstruction<IA32Template> implements Address32Instruction {

    private final Address32Instruction.Mixin _addressInstruction;

    public IA32DisassembledInstruction(int startAddress, int position, byte[] bytes, IA32Template template, IndexedSequence<Argument> arguments) {
        super(position, bytes, template, arguments);
        _addressInstruction = new Address32Instruction.Mixin(this, startAddress);
    }

    public static String exampleToString(IA32Template template, IndexedSequence<Argument> arguments) {
        final byte[] bytes = {};
        final AppendableIndexedSequence<DisassembledLabel> labels = new ArrayListSequence<DisassembledLabel>();
        final IA32DisassembledInstruction dis = new IA32DisassembledInstruction(0, 0, bytes, template, arguments);
        return dis.toString(labels);
    }

    public int address() {
        return _addressInstruction.address();
    }

    public String addressString() {
        return _addressInstruction.addressString();
    }

    public int addressToPosition(ImmediateArgument argument) {
        return _addressInstruction.addressToPosition(argument);
    }

}
