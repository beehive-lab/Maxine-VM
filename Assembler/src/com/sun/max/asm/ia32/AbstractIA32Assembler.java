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
package com.sun.max.asm.ia32;

import com.sun.max.asm.*;
import com.sun.max.lang.*;

/**
 * Base class for an IA32 assembler.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class AbstractIA32Assembler extends LittleEndianAssembler implements Assembler32 {

    @Override
    public final ISA isa() {
        return ISA.IA32;
    }

    @Override
    public WordWidth wordWidth() {
        return WordWidth.BITS_32;
    }

    private int startAddress; // address of first instruction

    public int startAddress() {
        return startAddress;
    }

    public void setStartAddress(int address) {
        startAddress = address;
    }

    @Override
    public long baseAddress() {
        return startAddress;
    }

    public AbstractIA32Assembler() {
    }

    public AbstractIA32Assembler(int startAddress) {
        this.startAddress = startAddress;
    }

    public void fixLabel(Label label, int address) {
        fixLabel32(label, address);
    }

    public int address(Label label) throws AssemblyException {
        return address32(label);
    }
}
