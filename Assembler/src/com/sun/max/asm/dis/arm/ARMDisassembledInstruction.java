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
/*VCSID=396f0742-80ba-4373-8f60-3a54e87de797*/

package com.sun.max.asm.dis.arm;

import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.asm.dis.risc.*;
import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.risc.arm.*;
import com.sun.max.collect.*;


/**
 *
 * @author Sumeet Panchal
 */

public class ARMDisassembledInstruction extends RiscDisassembledInstruction<ARMTemplate> implements Address32Instruction {

    private final Address32Instruction.Mixin _addressInstruction;

    public ARMDisassembledInstruction(int startAddress, int position, byte[] bytes, ARMTemplate template, IndexedSequence<Argument> arguments) {
        super(position, bytes, template, arguments);
        _addressInstruction = new Address32Instruction.Mixin(this, startAddress);
    }

    @Override
    public String externalName() {
        final ARMExternalInstruction instruction = new ARMExternalInstruction(template(), arguments(), startPosition(), null, null);
        return instruction.name();
    }

    @Override
    public String operandsToString(Sequence<DisassembledLabel> labels, GlobalLabelMapper globalLabelMapper) {
        final ARMExternalInstruction instruction = new ARMExternalInstruction(template(), arguments(), startPosition(), labels, globalLabelMapper);
        return instruction.operands();
    }

    @Override
    public String toString(Sequence<DisassembledLabel> labels, GlobalLabelMapper globalLabelMapper) {
        final ARMExternalInstruction instruction = new ARMExternalInstruction(template(), arguments(), startPosition(), labels, globalLabelMapper);
        return instruction.toString();
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
