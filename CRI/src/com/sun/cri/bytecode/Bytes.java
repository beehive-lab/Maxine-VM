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
package com.sun.cri.bytecode;

/**
 * A collection of utility methods for dealing with bytes, particularly in byte arrays.
 *
 * @author Ben L. Titzer
 */
public class Bytes {
	/**
	 * Gets a signed 1-byte value.
	 * @param data the array containing the data
	 * @param bci the start index of the value to retrieve
	 * @return the signed 1-byte value at index {@code bci} in array {@code data}
	 */
    public static int beS1(byte[] data, int bci) {
        return data[bci];
    }

	/**
	 * Gets a signed 2-byte big-endian value.
	 * @param data the array containing the data
	 * @param bci the start index of the value to retrieve
	 * @return the signed 2-byte, big-endian, value at index {@code bci} in array {@code data}
	 */
    public static int beS2(byte[] data, int bci) {
        return (data[bci] << 8) | (data[bci + 1] & 0xff);
    }

	/**
	 * Gets an unsigned 1-byte value.
	 * @param data the array containing the data
	 * @param bci the start index of the value to retrieve
	 * @return the unsigned 1-byte value at index {@code bci} in array {@code data}
	 */
    public static int beU1(byte[] data, int bci) {
        return data[bci] & 0xff;
    }

	/**
	 * Gets an unsigned 2-byte big-endian value.
	 * @param data the array containing the data
	 * @param bci the start index of the value to retrieve
	 * @return the unsigned 2-byte, big-endian, value at index {@code bci} in array {@code data}
	 */
    public static int beU2(byte[] data, int bci) {
        return ((data[bci] & 0xff) << 8) | (data[bci + 1] & 0xff);
    }

	/**
	 * Gets a signed 4-byte big-endian value.
	 * @param data the array containing the data
	 * @param bci the start index of the value to retrieve
	 * @return the signed 4-byte, big-endian, value at index {@code bci} in array {@code data}
	 */
    public static int beS4(byte[] data, int bci) {
        return (data[bci] << 24) | ((data[bci + 1] & 0xff) << 16) | ((data[bci + 2] & 0xff) << 8) | (data[bci + 3] & 0xff);
    }
}
