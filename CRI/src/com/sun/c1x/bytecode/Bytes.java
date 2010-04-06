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
package com.sun.c1x.bytecode;

/**
 * This class implements a number of utilities for dealing with bytes,
 * particularly in byte arrays.
 *
 * @author Ben L. Titzer
 */
public class Bytes {
    public static int beS1(byte[] data, int bci) {
        // big-endian unsigned 1-byte quantity
        return data[bci];
    }

    public static int beS2(byte[] data, int bci) {
        // big-endian signed 2-byte quantity
        return (data[bci] << 8) | (data[bci + 1] & 0xff);
    }

    public static int beU1(byte[] data, int bci) {
        // big-endian unsigned 1-byte quantity
        return data[bci] & 0xff;
    }

    public static int beU2(byte[] data, int bci) {
        // big-endian unsigned 2-byte quantity
        return ((data[bci] & 0xff) << 8) | (data[bci + 1] & 0xff);
    }

    public static int beS4(byte[] data, int bci) {
        // big-endian signed 4-byte quantity
        return (data[bci] << 24) | ((data[bci + 1] & 0xff) << 16) | ((data[bci + 2] & 0xff) << 8) | (data[bci + 3] & 0xff);
    }
}
