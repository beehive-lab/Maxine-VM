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
/*VCSID=1c8246b6-c3b1-42bc-b2fd-b27c86108b2b*/
package com.sun.max.asm;

/**
 * Represents an offset between two positions (represented by given labels) in the assembled code.
 * 
 * @author Doug Simon
 */
public class OffsetLiteral extends MutableAssembledObject {

    /**
     * Creates an offset between two positions (represented by given labels) in the assembled code.
     * 
     * @param base the label whose position marks the base of the offset
     * @param target the label whose position marks the target of the offset
     */
    protected OffsetLiteral(Assembler assembler, int startPosition, int endPosition, Label target, Label base) {
        super(assembler, startPosition, endPosition);
        assembler.addFixedSizeAssembledObject(this);
        _target = target;
        _base = base;
    }

    private final Label _target;
    private final Label _base;

    @Override
    protected final void assemble() throws AssemblyException {
        final Assembler assembler = assembler();
        final long offset = _target.position() - _base.position();
        // select the correct assembler function to emit the offset.
        // The assembler knows the endianness to use
        switch(size()) {
            case 1:
                assembler.emitByte((byte) (offset & 0xFF));
                break;
            case 2:
                assembler.emitShort((short) (offset & 0xFFFF));
                break;
            case 4:
                assembler.emitInt((int) (offset & 0xFFFFFFFF));
                break;
            case 8:
                assembler.emitLong(offset);
                break;
            default:
                throw new AssemblyException("Invalid size for offset literal");
        }
    }

    public final Type type() {
        return Type.DATA;
    }
}
