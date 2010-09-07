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
package com.sun.max.vm.classfile.create;

/**
 * @author Bernd Mathiske
 */
public final class MillWord {
    private MillWord() {
    }

    /**
     * Get least significant byte number 3, counting from 0.
     * 
     * @param i
     *            The word from which to get the byte.
     * @return The desired byte.
     */
    public static byte byte3(int i) {
        return (byte) ((i >> 24) & 0xff);
    }

    /**
     * Get least significant byte number 2, counting from 0.
     * 
     * @param i
     *            The word from which to get the byte.
     * @return The desired byte.
     */
    public static byte byte2(int i) {
        return (byte) ((i >> 16) & 0xff);
    }

    /**
     * Get least significant byte number 1, counting from 0.
     * 
     * @param i
     *            The word from which to get the byte.
     * @return The desired byte.
     */
    public static byte byte1(int i) {
        return (byte) ((i >> 8) & 0xff);
    }

    /**
     * Get the least significant byte.
     * 
     * @param i
     *            The word from which to get the byte.
     * @return The desired byte.
     */
    public static byte byte0(int i) {
        return (byte) (i & 0xff);
    }

}
