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
/*VCSID=59b102d2-1868-4b7f-b120-d794bc3ecf54*/
package com.sun.max.asm;

import com.sun.max.program.*;

/**
 * An instruction that addresses some data with an absolute address.
 *
 * @author Bernd Mathiske
 */
public abstract class InstructionWithAddress extends InstructionWithLabel {

    protected InstructionWithAddress(Assembler assembler, int startPosition, int endPosition, Label label) {
        super(assembler, startPosition, endPosition, label);
        assembler.addFixedSizeAssembledObject(this);
    }

    public int addressAsInt() throws AssemblyException {
        final Assembler32 assembler = (Assembler32) assembler();
        switch (label().state()) {
            case BOUND: {
                return assembler.startAddress() + label().position();
            }
            case FIXED_32: {
                return assembler.address(label());
            }
            case FIXED_64: {
                throw ProgramError.unexpected("64-bit address requested for 32-bit assembler");
            }
            default: {
                throw new AssemblyException("unassigned label");
            }
        }
    }

    public long addressAsLong() throws AssemblyException {
        final Assembler64 assembler = (Assembler64) assembler();
        switch (label().state()) {
            case BOUND: {
                return assembler.startAddress() + label().position();
            }
            case FIXED_64: {
                return assembler.address(label());
            }
            case FIXED_32: {
                throw ProgramError.unexpected("32-bit address requested for 64-bit assembler");
            }
            default: {
                throw new AssemblyException("unassigned label");
            }
        }
    }
}
