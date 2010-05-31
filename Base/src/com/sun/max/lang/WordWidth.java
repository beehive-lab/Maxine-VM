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
package com.sun.max.lang;

import java.util.*;

/**
 * A word width value describes how many bits there are in a machine word.
 *
 * @author Bernd Mathiske
 */
public enum WordWidth {

    BITS_8(8, byte.class, Byte.MIN_VALUE, Byte.MAX_VALUE, 3),
    BITS_16(16, short.class, Short.MIN_VALUE, Short.MAX_VALUE, 4),
    BITS_32(32, int.class, Integer.MIN_VALUE, Integer.MAX_VALUE, 5),
    BITS_64(64, long.class, Long.MIN_VALUE, Long.MAX_VALUE, 6);

    public static final List<WordWidth> VALUES = java.util.Arrays.asList(values());

    /**
     * Number of bits in a Word.
     * This must be a positive power of two.
     */
    public final int numberOfBits;

    /**
     * Log2 of the number of bits.
     */
    public final int log2numberOfBits;

    /**
     * Log2 of the number of bytes.
     */
    public final int log2numberOfBytes;

    /**
     * Number of bytes in a Word.
     * This must be a positive power of two.
     */
    public final int numberOfBytes;

    public final Class canonicalPrimitiveType;
    public final long min;
    public final long max;

    private WordWidth(int numberOfBits, Class canonicalPrimitiveType, long min, long max, int log2numberOfBits) {
        this.numberOfBits = numberOfBits;
        this.numberOfBytes = numberOfBits / 8;
        this.canonicalPrimitiveType = canonicalPrimitiveType;
        this.min = min;
        this.max = max;
        this.log2numberOfBits = log2numberOfBits;
        this.log2numberOfBytes = log2numberOfBits - 3;
    }

    public boolean lessThan(WordWidth other) {
        return numberOfBits < other.numberOfBits;
    }

    public boolean lessEqual(WordWidth other) {
        return numberOfBits <= other.numberOfBits;
    }

    public boolean greaterThan(WordWidth other) {
        return numberOfBits > other.numberOfBits;
    }

    public boolean greaterEqual(WordWidth other) {
        return numberOfBits >= other.numberOfBits;
    }

    @Override
    public String toString() {
        return Integer.toString(numberOfBits);
    }

    public static WordWidth fromInt(int wordWidth) {
        if (wordWidth <= 8) {
            return WordWidth.BITS_8;
        }
        if (wordWidth <= 16) {
            return WordWidth.BITS_16;
        }
        if (wordWidth <= 32) {
            return WordWidth.BITS_32;
        }
        return WordWidth.BITS_64;
    }

    /**
     * @return which word width is minimally required to represent all the non-one bits in the signed argument, and a sign bit
     */
    public static WordWidth signedEffective(int signed) {
        return fromInt(Ints.numberOfEffectiveSignedBits(signed));
    }

    /**
     * @return which word width is minimally required to represent all the non-zero bits in the unsigned argument
     */
    public static WordWidth unsignedEffective(int unsigned) {
        return fromInt(Ints.numberOfEffectiveUnsignedBits(unsigned));
    }

    /**
     * @return which word width is minimally required to represent all the non-one bits in the signed argument, and a sign bit
     */
    public static WordWidth signedEffective(long signed) {
        return fromInt(Longs.numberOfEffectiveSignedBits(signed));
    }

    /**
     * @return which word width is minimally required to represent all the non-zero bits in the unsigned argument
     */
    public static WordWidth unsignedEffective(long unsigned) {
        return fromInt(Longs.numberOfEffectiveUnsignedBits(unsigned));
    }
}
