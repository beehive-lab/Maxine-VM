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

import com.sun.max.asm.gen.*;

/**
 * Boxes inline data decoded from an instruction stream.
 *
 * @author Doug Simon
 */
public abstract class DisassembledData implements DisassembledObject {

    private final ImmediateArgument startAddress;
    private final int startPosition;
    private final byte[] bytes;
    private final String mnemonic;
    private final ImmediateArgument targetAddress;

    /**
     * Creates an object encapsulating some inline data that starts at a given position.
     *
     * @param startAddress the absolute address at which the inline data starts
     * @param startPosition the instruction stream relative position at which the inline data starts
     * @param mnemonic an assembler directive like name for the data
     * @param bytes the raw bytes of the inline data
     */
    public DisassembledData(ImmediateArgument startAddress, int startPosition, String mnemonic, byte[] bytes, ImmediateArgument targetAddress) {
        this.startAddress = startAddress;
        this.startPosition = startPosition;
        this.mnemonic = mnemonic;
        this.bytes = bytes;
        this.targetAddress = targetAddress;
    }

    public ImmediateArgument startAddress() {
        return startAddress;
    }

    public ImmediateArgument endAddress() {
        return startAddress().plus(startPosition());
    }

    public int startPosition() {
        return startPosition;
    }

    public int endPosition() {
        return startPosition + bytes.length;
    }

    public byte[] bytes() {
        return bytes.clone();
    }

    public Type type() {
        return Type.DATA;
    }

    public ImmediateArgument targetAddress() {
        return targetAddress;
    }

    public String mnemonic() {
        return mnemonic;
    }

    public abstract String operandsToString(AddressMapper addressMapper);

    public String toString(AddressMapper addressMapper) {
        return mnemonic() + " " + operandsToString(addressMapper);
    }

    @Override
    public abstract String toString();
}
