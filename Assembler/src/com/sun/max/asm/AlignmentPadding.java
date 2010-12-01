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
package com.sun.max.asm;

/**
 * Byte stream padding for memory alignment in the assembler. This pseudo-instruction pads the byte stream to ensure that the next
 * memory location is aligned on the specified boundary.
 * Padding may also be performed conditionally to the amount of space left in the byte stream before the next aligned boundary.
 *
 * @author David Liu
 * @author Laurent Daynes
 */
public abstract class AlignmentPadding extends MutableAssembledObject {

    public AlignmentPadding(Assembler assembler, int startPosition, int endPosition, int alignment, int requiredSpace, byte padByte) {
        super(assembler, startPosition, endPosition);
        this.alignment = alignment;
        this.padByte = padByte;
        this.maxMisalignment = alignment - requiredSpace;
        assembler.addAlignmentPadding(this);
    }

    private final int alignment;

    public final int alignment() {
        return alignment;
    }

    /**
     * Max misalignment supported. If greater than that, pad to the next alignment.
     * By default, this is zero, meaning that padding is always performed to the next alignment.
     * When we want to pad only if there isn't enough space to fit the next object of size s, maxMisalignment is
     * set to alignment - s.
     */
    private final int maxMisalignment;

    private final byte padByte;

    public void updatePadding() {
        // We avoid sign problems with '%' below by masking off the sign bit:
        final long unsignedAddend = (assembler().baseAddress() + startPosition()) & 0x7fffffffffffffffL;
        final int misalignmentSize = (int) (unsignedAddend % alignment);
        variableSize =  (misalignmentSize <= maxMisalignment)  ? 0 :  alignment - misalignmentSize;
    }

    @Override
    protected void assemble() throws AssemblyException {
        for (int i = 0; i < size(); i++) {
            assembler().emitByte(padByte);
        }
    }
}
