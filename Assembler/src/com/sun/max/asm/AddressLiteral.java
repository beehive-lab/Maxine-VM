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
/*VCSID=30a96158-8e0f-4fe2-83c9-a951e8b5bcda*/
package com.sun.max.asm;

import com.sun.max.lang.*;

/**
 * Represents a position assembled as an absolute address.
 * 
 * @author Doug Simon
 */
public class AddressLiteral extends MutableAssembledObject {

    protected AddressLiteral(Assembler assembler, int startPosition, int endPosition, Label label) {
        super(assembler, startPosition, endPosition);
        assembler.addFixedSizeAssembledObject(this);
        _label = label;
    }

    private final Label _label;

    @Override
    protected final void assemble() throws AssemblyException {
        final Assembler assembler = assembler();
        final WordWidth wordWidth = assembler.wordWidth();
        if (wordWidth == WordWidth.BITS_64) {
            assembler.emitLong(assembler.baseAddress() + _label.position());
        } else if  (wordWidth == WordWidth.BITS_32) {
            assembler.emitInt((int) (assembler.baseAddress() + _label.position()));
        } else if  (wordWidth == WordWidth.BITS_16) {
            assembler.emitShort((short) (assembler.baseAddress() + _label.position()));
        } else {
            assert wordWidth == WordWidth.BITS_8;
            assembler.emitByte((byte) (assembler.baseAddress() + _label.position()));
        }
    }

    public final Type type() {
        return Type.DATA;
    }
}
