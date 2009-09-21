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
package com.sun.max.asm.gen.risc.bitRange;


/**
 * A range of bits that contributes to a field's value but does not occupy any bit
 * positions in an instruction. The implicit bits are 0. This type of bit range
 * is typically used to represent the low-order bits for a field value's that is
 * always modulo {@code n} where {@code n > 1}. That is, an aligned value.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Dave Ungar
 * @author Adam Spitz
 */
public class OmittedBitRange extends ContiguousBitRange {

    private int width;

    OmittedBitRange(int width) {
        this.width = width;
        assert width > 0;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int encodedWidth() {
        return 0;
    }

    @Override
    public BitRange move(boolean left, int bits) {
        return this;
    }

    /* Accessing */

    @Override
    public int instructionMask() {
        return 0;
    }

    @Override
    public int numberOfLessSignificantBits() {
        return 32;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof OmittedBitRange)) {
            throw new Error("Invalid argument type\n");
        }
        final OmittedBitRange omittedBitRange = (OmittedBitRange) other;
        return width == omittedBitRange.width;
    }

    @Override
    public int hashCode() {
        return width;
    }

    /* Extracting */
    @Override
    public int extractSignedInt(int syllable) {
        return 0;
    }

    @Override
    public int extractUnsignedInt(int syllable) {
        return 0;
    }

    /* Inserting */
    @Override
    public int assembleUncheckedSignedInt(int signedInt) {
        return 0;
    }

    @Override
    public int assembleUncheckedUnsignedInt(int unsignedInt) {
        return 0;
    }

    @Override
    public String encodingString(String value, boolean signed, boolean checked) {
        return "";
    }

    @Override
    public String toString() {
        return "omit" + width;
    }
}
