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
package com.sun.max.asm.dis;

import com.sun.max.asm.*;
import com.sun.max.asm.gen.*;

/**
 * Extends the abstraction of an object in an assembled instruction stream with extra
 * properties that are only relevant for an object decoded from an instruction stream.
 *
 * @author Doug Simon
 */
public interface DisassembledObject extends AssemblyObject {

    /**
     * Gets a name for this object. If this is a disassembled instruction, it will be an assembler mnemonic. For
     * disassembled inline data, it will resemble an assembler directive.
     */
    String mnemonic();

    /**
     * Gets the address of the instruction or inline data addressed (either relatively or absolutely) by this object.
     *
     * @return null if this object does not address an instruction or inline data
     */
    ImmediateArgument targetAddress();

    /**
     * Gets the absolute address of this disassembled object's first byte.
     */
    ImmediateArgument startAddress();

    /**
     * Gets the absolute address of this disassembled object's first byte.
     */
    ImmediateArgument endAddress();

    /**
     * Gets a string representation of this object. The recommended format is one resembling that of a native
     * disassembler for the platform corresponding to the ISA.
     *
     * @param addressMapper object used to map addresses to {@linkplain DisassembledLabel labels}. This value may be null.
     */
    String toString(AddressMapper addressMapper);

    /**
     * Gets the raw bytes of this object.
     */
    byte[] bytes();
}
