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
package com.sun.c1x.ci;

/**
 * This class represents a CPU architecture, including information such as its endianness, CPU
 * registers, word width, etc.
 *
 * @author Ben L. Titzer
 * @author Thomas Wuerthinger
 */
public abstract class CiArchitecture {

    public static enum ByteOrder {
        LittleEndian,
        BigEndian
    }

    /**
     * Represents the natural size of words (typically registers and pointers) of this architecture, in bytes.
     */
    public final int wordSize;

    /**
     * The name of the platform associated with this architecture. May either be "x86" or "SPARC".
     */
    public final String platform;

    /**
     * The offset of the lower half of a word in bytes.
     */
    public final int lowWordOffset;

    /**
     * The offset of the upper half of a word in bytes.
     */
    public final int highWordOffset;

    /**
     * Array of all available registers on this architecture.
     */
    public final CiRegister[] registers;

    /**
     * The bit ordering can be either little or big endian.
     */
    public final ByteOrder byteOrder;

    /**
     * Offset in bytes from the beginning of a call instruction to the displacement.
     */
    public final int machineCodeCallDisplacementOffset;

    public final String name;

    public final int returnAddressSize;

    /**
     * Reflectively instantiates an architecture given its name.
     *
     * @param name
     *            the name of the wanted architecture
     * @return the newly created architecture object
     */
    public static CiArchitecture findArchitecture(String name) {
        // load and instantiate the backend via reflection
        String className = "com.sun.c1x.target." + name.toUpperCase();
        try {
            Class<?> javaClass = Class.forName(className);
            return (CiArchitecture) javaClass.newInstance();
        } catch (InstantiationException e) {
            throw new Error("could not instantiate architecture class: " + className);
        } catch (IllegalAccessException e) {
            throw new Error("could not access architecture class: " + className);
        } catch (ClassNotFoundException e) {
            throw new Error("could not find architecture class: " + className);
        }
    }

    protected CiArchitecture(String name, int wordSize, String backend, ByteOrder byteOrder, CiRegister[] registers, int nativeCallDisplacementOffset, int returnAddressSize) {
        this.name = name;
        this.registers = registers;
        this.wordSize = wordSize;
        this.platform = backend;
        this.byteOrder = byteOrder;
        this.machineCodeCallDisplacementOffset = nativeCallDisplacementOffset;
        this.returnAddressSize = returnAddressSize;

        if (byteOrder == ByteOrder.LittleEndian) {
            this.lowWordOffset = 0;
            this.highWordOffset = wordSize;
        } else {
            this.lowWordOffset = wordSize;
            this.highWordOffset = 0;
        }
    }

    /**
     * Converts this architecture to a string.
     * @return the string representation of this architecture
     */
    @Override
    public String toString() {
        return name.toLowerCase();
    }

    /**
     * Checks whether this is a 32-bit architecture.
     * @return {@code true} if this architecture is 32-bit
     */
    public boolean is32bit() {
        return wordSize == 4;
    }

    /**
     * Checks whether this is a 64-bit architecture.
     * @return {@code true} if this architecture is 64-bit
     */
    public boolean is64bit() {
        return wordSize == 8;
    }

    /**
     * Checks whether the backend is x86.
     * @return {@code true} if the backend of this architecture is x86
     */
    public boolean isX86() {
        return false;
    }

    /**
     * Checks whether the backend is SPARC.
     * @return {@code true} if the backend of this architecture is SPARC
     */
    public boolean isSPARC() {
        return false;
    }

    /**
     * Checks whether this architecture's normal arithmetic instructions use a two-operand form
     * (e.g. x86 which overwrites one operand register with the result when adding).
     * @return {@code true} if this architecture uses two-operand mode
     */
    public boolean twoOperandMode() {
        return false;
    }
}
