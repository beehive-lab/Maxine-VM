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
 * A bit range that has its least significant bit on the left and its most significant bit on the right.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Dave Ungar
 * @author Adam Spitz
 */
public class AscendingBitRange extends SimpleBitRange {

    public AscendingBitRange(int firstBitIndex, int lastBitIndex) {
        super(firstBitIndex, lastBitIndex);
        if (firstBitIndex > lastBitIndex) {
            throw new IllegalArgumentException("bit ranges are specified from left to right, and ascending notation starts at 0 and goes up to 31");
        }
    }

    @Override
    public AscendingBitRange move(boolean left, int bits) {
        if (left) {
            return new AscendingBitRange(_firstBitIndex - bits, _lastBitIndex - bits);
        }
        return new AscendingBitRange(_firstBitIndex + bits, _lastBitIndex + bits);
    }

    @Override
    public int numberOfLessSignificantBits() {
        return (32 - _lastBitIndex) - 1;
    }

    @Override
    public int width() {
        return (_lastBitIndex - _firstBitIndex) + 1;
    }
}
