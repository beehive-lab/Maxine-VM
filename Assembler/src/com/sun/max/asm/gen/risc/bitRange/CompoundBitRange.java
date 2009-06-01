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

import java.util.*;

import com.sun.max.collect.*;

/**
 * A range of bits that is composed of several disjoint subranges.
 * 
 * @author Bernd Mathiske
 * @author Dave Ungar
 * @author Adam Spitz
 */

public class CompoundBitRange extends BitRange {

    public CompoundBitRange() {
        super();
    }

    private AppendableIndexedSequence<ContiguousBitRange> _contiguousBitRanges = new ArrayListSequence<ContiguousBitRange>();

    public Sequence<ContiguousBitRange> contiguousBitRanges() {
        return _contiguousBitRanges;
    }

    public void add(ContiguousBitRange contiguousBitRange) {
        _contiguousBitRanges.append(contiguousBitRange);
    }

    @Override
    public CompoundBitRange move(boolean left, int bits) {
        final CompoundBitRange movedRange = new CompoundBitRange();
        for (ContiguousBitRange contiguousBitRange : _contiguousBitRanges) {
            movedRange.add((ContiguousBitRange) contiguousBitRange.move(left, bits));
        }
        return movedRange;
    }

    @Override
    public int width() {
        int result = 0;
        for (ContiguousBitRange contiguousBitRange : _contiguousBitRanges) {
            result += contiguousBitRange.width();
        }
        return result;
    }

    @Override
    public int encodedWidth() {
        int result = 0;
        for (ContiguousBitRange contiguousBitRange : _contiguousBitRanges) {
            result += contiguousBitRange.encodedWidth();
        }
        return result;
    }

    @Override
    public int instructionMask() {
        int mask = 0;
        for (ContiguousBitRange contiguousBitRange : _contiguousBitRanges) {
            mask |= contiguousBitRange.instructionMask();
        }
        return mask;
    }

    @Override
    public int numberOfLessSignificantBits() {
        int result = 32;
        for (ContiguousBitRange contiguousBitRange : _contiguousBitRanges) {
            final int n = contiguousBitRange.numberOfLessSignificantBits();
            if (n < result) {
                result = n;
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CompoundBitRange)) {
            return false;
        }
        final CompoundBitRange compoundBitRange = (CompoundBitRange) other;
        return _contiguousBitRanges.equals(compoundBitRange._contiguousBitRanges);
    }

    @Override
    public int hashCode() {
        return _contiguousBitRanges.hashCode();
    }

    @Override
    public String toString() {
        String result = null;
        for (ContiguousBitRange contiguousBitRange : _contiguousBitRanges) {
            if (result != null) {
                result += "," + contiguousBitRange;
            } else {
                result = contiguousBitRange.toString();
            }
        }
        return result;
    }

    /* Extracting */

    @Override
    public int extractSignedInt(int instruction) {
        final Iterator<ContiguousBitRange> iterator = _contiguousBitRanges.iterator();
        final ContiguousBitRange firstBitRange = iterator.next();
        int signedInt = firstBitRange.extractSignedInt(instruction);
        while (iterator.hasNext()) {
            final ContiguousBitRange contiguousBitRange = iterator.next();
            signedInt <<= contiguousBitRange.width();
            signedInt |= contiguousBitRange.extractUnsignedInt(instruction);
        }
        return signedInt;
    }

    @Override
    public int extractUnsignedInt(int instruction) {
        int unsignedInt = 0;
        for (ContiguousBitRange contiguousBitRange : _contiguousBitRanges) {
            unsignedInt <<= contiguousBitRange.width();
            unsignedInt |= contiguousBitRange.extractUnsignedInt(instruction);
        }
        return unsignedInt;
    }

    /* Inserting */

    @Override
    public int assembleUncheckedSignedInt(int signedInt) throws IndexOutOfBoundsException {
        int value = signedInt;
        int result = 0;
        for (int i = _contiguousBitRanges.length() - 1; i >= 0; i--) {
            final ContiguousBitRange contiguousBitRange = _contiguousBitRanges.get(i);
            result |= contiguousBitRange.assembleUncheckedSignedInt(value);
            value >>= contiguousBitRange.width();
        }
        return result;
    }

    @Override
    public int assembleUncheckedUnsignedInt(int unsignedInt) throws IndexOutOfBoundsException {
        int value = unsignedInt;
        int result = 0;
        for (int i = _contiguousBitRanges.length() - 1; i >= 0; i--) {
            final ContiguousBitRange contiguousBitRange = _contiguousBitRanges.get(i);
            result |= contiguousBitRange.assembleUncheckedUnsignedInt(value);
            value >>>= contiguousBitRange.width();
        }
        return result;
    }

    @Override
    public String encodingString(String value, boolean signed, boolean checked) {
        final StringBuilder sb = new StringBuilder();
        int shift = 0;
        for (int i = _contiguousBitRanges.length() - 1; i >= 0; i--) {
            final ContiguousBitRange contiguousBitRange = _contiguousBitRanges.get(i);
            final String bits = (shift == 0) ? value : "(" + value + (signed ? " >> " : " >>> ") + shift + ")";
            final String encoding = contiguousBitRange.encodingString(bits, signed, false);
            if (encoding.length() != 0) {
                if (sb.length() != 0) {
                    sb.append(" | ");
                }
                sb.append(encoding);
            }
            shift += contiguousBitRange.width();
        }
        return sb.toString();
    }
}
