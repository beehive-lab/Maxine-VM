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
/*VCSID=2849b2d5-8074-4fd7-81a0-d493f32b26ec*/
package com.sun.max.asm.gen.risc.bitRange;


/**
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Dave Ungar
 * @author Adam Spitz
 */
public abstract class SimpleBitRange extends ContiguousBitRange {

    protected final int _firstBitIndex;
    protected final int _lastBitIndex;

    protected SimpleBitRange(int firstBitIndex, int lastBitIndex) {
        super();
        if (!(firstBitIndex >= 0 && lastBitIndex >= 0 && firstBitIndex < 32 && lastBitIndex < 32)) {
            throw new IllegalArgumentException("bit indexes must be between 0 and 31");
        }
        _firstBitIndex = firstBitIndex;
        _lastBitIndex = lastBitIndex;
    }

    @Override
    public int instructionMask() {
        return valueMask() << numberOfLessSignificantBits();
    }

    @Override
    public int encodedWidth() {
        return width();
    }

    // comparing

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SimpleBitRange)) {
            return false;
        }
        final SimpleBitRange simpleBitRange = (SimpleBitRange) other;
        return _firstBitIndex == simpleBitRange._firstBitIndex && _lastBitIndex == simpleBitRange._lastBitIndex;
    }

    @Override
    public int hashCode() {
        return _firstBitIndex ^ _lastBitIndex;
    }

    // extracting

    @Override
    public int extractSignedInt(int syllable) {
        final int unsignedInt = extractUnsignedInt(syllable);
        final int max = 1 << (width() - 1);
        if (unsignedInt < max) {
            return unsignedInt;
        }
        return (unsignedInt - valueMask()) - 1;
    }

    @Override
    public int extractUnsignedInt(int syllable) {
        return (syllable >>> numberOfLessSignificantBits()) & valueMask();
    }

    // inserting

    @Override
    public int assembleUncheckedSignedInt(int signedInt) {
        return (signedInt & valueMask()) << numberOfLessSignificantBits();
    }

    @Override
    public int assembleUncheckedUnsignedInt(int unsignedInt) {
        return (unsignedInt & valueMask()) << numberOfLessSignificantBits();
    }

    @Override
    public String encodingString(String value, boolean signed, boolean omitMask) {
        final StringBuilder sb = new StringBuilder();
        if (valueMask() == 0 || omitMask) {
            sb.append(value);
        } else {
            sb.append('(').append(value).append(" & 0x").append(Integer.toHexString(valueMask())).append(')');
        }
        if (numberOfLessSignificantBits() != 0) {
            return "(" + sb + " << " + numberOfLessSignificantBits() + ")";
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return _firstBitIndex + ":" + _lastBitIndex;
    }
}
