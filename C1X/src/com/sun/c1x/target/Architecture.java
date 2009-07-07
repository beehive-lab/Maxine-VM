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
package com.sun.c1x.target;

/**
 * The <code>Architecture</code> class represents a CPU architecture that is supported by
 * the backend of C1X.
 *
 * @author Ben L. Titzer
 */
public enum Architecture {
    IA32(4, "x86"),
    AMD64(8, "x86"),
    SPARC(4, "sparc"),
    SPARCV9(8, "sparc");
    // PPC(4),
    // PPC64(8),
    // ARM(4),

    /**
     * Represents the natural size of words (typically registers and pointers) of this architecture, in bytes.
     */
    public final int wordSize;
    public final int bitsPerWord;
    public final String backend;

    Architecture(int wordSize, String backend) {
        this.wordSize = wordSize;
        this.backend = backend;
        this.bitsPerWord = (int) java.lang.Math.pow(2, wordSize);
    }

    /**
     * Converts this architecture to a string.
     * @return the string representation of this architecture
     */
    @Override
    public String toString() {
        return name().toLowerCase();
    }

    public int bitsPerWord() {
        return bitsPerWord;
    }

    /**
     * Checks whether this is a 32-bit architecture.
     * @return <code>true</code> if this architecture is 32-bit
     */
    public boolean is32bit() {
        return wordSize == 4;
    }

    /**
     * Checks whether this is a 64-bit architecture.
     * @return <code>true</code> if this architecture is 64-bit
     */
    public boolean is64bit() {
        return wordSize == 8;
    }

    /**
     * TODO: Get rid of this method. Platform specific code should be in subclasses.
     * Checks whether the backend is x86.
     * @return <code>true</code> if the backend of this architecture is x86
     */
    public boolean isX86() {
        return backend.equals("x86");
    }

    /**
     * TODO: Get rid of this method. Platform specific code should be in subclasses.
     * Checks whether the backend is SPARC.
     * @return <code>true</code> if the backend of this architecture is SPARC
     */
    public boolean isSPARC() {
        return backend.equals("SPARC");
    }
}
