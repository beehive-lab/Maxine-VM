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
/*VCSID=965daa4f-e3d7-4169-8512-ed72cd296924*/
package com.sun.max.lang;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;


/**
 * A word width value describes many bits there are in a machine word.
 * 
 * @author Bernd Mathiske
 */
public enum WordWidth {

    BITS_8(8, byte.class, Byte.MIN_VALUE, Byte.MAX_VALUE),
    BITS_16(16, short.class, Short.MIN_VALUE, Short.MAX_VALUE),
    BITS_32(32, int.class, Integer.MIN_VALUE, Integer.MAX_VALUE),
    BITS_64(64, long.class, Long.MIN_VALUE, Long.MAX_VALUE);

    public static final IndexedSequence<WordWidth> VALUES = new ArraySequence<WordWidth>(values());

    private final int _numberOfBits;
    private final int _numberOfBytes;
    private final Class _canonicalPrimitiveType;
    private final long _min;
    private final long _max;

    private WordWidth(int numberOfBits, Class canonicalPrimitiveType, long min, long max) {
        _numberOfBits = numberOfBits;
        _numberOfBytes = numberOfBits / 8;
        _canonicalPrimitiveType = canonicalPrimitiveType;
        _min = min;
        _max = max;
    }

    @INLINE
    public final int numberOfBits() {
        return _numberOfBits;
    }

    @INLINE
    public final int numberOfBytes() {
        return _numberOfBytes;
    }

    public Class canonicalPrimitiveType() {
        return _canonicalPrimitiveType;
    }

    @INLINE
    public final long min() {
        return _min;
    }

    @INLINE
    public final long max() {
        return _max;
    }

    public boolean lessThan(WordWidth other) {
        return numberOfBits() < other.numberOfBits();
    }

    public boolean lessEqual(WordWidth other) {
        return numberOfBits() <= other.numberOfBits();
    }

    public boolean greaterThan(WordWidth other) {
        return numberOfBits() > other.numberOfBits();
    }

    public boolean greaterEqual(WordWidth other) {
        return numberOfBits() >= other.numberOfBits();
    }

    @Override
    public String toString() {
        return Integer.toString(_numberOfBits);
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
