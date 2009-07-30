/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.lir;


/**
 * The <code>CompressedStream</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class CompressedStream {

    protected byte[] buffer;
    int position;


    protected enum Encoding {
        // Constants for UNSIGNED5 coding of Pack200
        LgH(6), H(1 << Encoding.LgH.value), // number of high codes (64)
        L((1 << Integer.SIZE) - H.value), // number of low codes (192) TODO: using target specific
                                                            // information
        MAXI(4); // bytes are numbered in (0..4), max 5 bytes

        public final int value;

        private Encoding(int value) {
            this.value = value;
        }
    }

    // these inlines are defined only in compressedStream.cpp
    // for Pack200 SIGNED5
    static int encodeSign(int value) {
        return  (value << 1) ^ (value >> 31);
    }

    // for Pack200 SIGNED5
    static int decodeSign(int value) {
        return (value >> 1) ^ -(value & 1);
    }

    // to trim trailing float 0's
    static int reverseInt(int bits) {
        // Hacker's Delight, Figure 7-1
        bits = (bits & 0x55555555) << 1 | (bits >> 1) & 0x55555555;
        bits = (bits & 0x33333333) << 2 | (bits >> 2) & 0x33333333;
        bits = (bits & 0x0f0f0f0f) << 4 | (bits >> 4) & 0x0f0f0f0f;
        bits = (bits << 24) | ((bits & 0xff00) << 8) | ((bits >> 8) & 0xff00) | (bits >> 24);
        return bits;
    }

    public CompressedStream(byte[] buffer) {
        this(buffer, 0);
    }

    public CompressedStream(byte[] buffer, int position) {
        this.buffer = buffer;
        this.position = position;
    }

    public byte[] buffer() {
        return buffer;
    }

    // Positioning
    public int position() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
