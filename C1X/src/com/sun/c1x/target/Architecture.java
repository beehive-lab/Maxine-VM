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

import com.sun.c1x.util.Util;

/**
 * The <code>Architecture</code> class represents a CPU architecture that is supported by
 * the backend of C1X.
 *
 * @author Ben L. Titzer
 */
public abstract class Architecture {

    public static enum BitOrdering{
        LittleEndian,
        BigEndian
    }
    /**
     * Represents the natural size of words (typically registers and pointers) of this architecture, in bytes.
     */
    public final int wordSize;
    public final int bitsPerWord;
    public final int logBytesPerInt;
    public final String backend;
    public final int loWordOffsetInBytes;
    public final int hiWordOffsetInBytes;
    public final int stackBias = 0;
    public final Register[] registers;
    public final String name;
    public final BitOrdering bitOrdering;
    public final int framePadding;

    public static final Architecture findArchitecture(String name) {
        // load and instantiate the backend via reflection
        String className = "com.sun.c1x.target." + name.toUpperCase();
        try {
            Class<?> javaClass = Class.forName(className);
            return (Architecture) javaClass.newInstance();
        } catch (InstantiationException e) {
            throw new Error("could not instantiate architecture class: " + className);
        } catch (IllegalAccessException e) {
            throw new Error("could not access architecture class: " + className);
        } catch (ClassNotFoundException e) {
            throw new Error("could not find architecture class: " + className);
        }
    }

    protected Architecture(String name, int wordSize, String backend, BitOrdering bitOrdering, Register[] registers, final int framePadding) {
        this.name = name;
        this.registers = registers;
        this.wordSize = wordSize;
        this.backend = backend;
        this.bitsPerWord = wordSize * 8;
        this.logBytesPerInt = (int) (java.lang.Math.log(wordSize));
        this.bitOrdering = bitOrdering;
        this.framePadding = framePadding;
        switch (bitOrdering) {
            case LittleEndian:
                loWordOffsetInBytes = 0;
                hiWordOffsetInBytes = wordSize;
                break;
            case BigEndian:
                loWordOffsetInBytes = wordSize;
                hiWordOffsetInBytes = 0;
                break;
            default:
                Util.shouldNotReachHere();
                loWordOffsetInBytes = 0;
                hiWordOffsetInBytes = wordSize;
        }
    }

    public abstract Backend getBackend(Target target);

    /**
     * Converts this architecture to a string.
     * @return the string representation of this architecture
     */
    @Override
    public String toString() {
        return name.toLowerCase();
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
