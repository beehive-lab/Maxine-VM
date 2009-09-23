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
 * Thhis class represents a CPU architecture, including information such as its endianness, CPU
 * registers, word width, etc.
 *
 * @author Ben L. Titzer
 * @author Thomas Wuerthinger
 */
public abstract class CiArchitecture {

    public static enum BitOrdering {
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
    public final BitOrdering bitOrdering;

    /**
     * Additional padding that is added to the frame size of each method.
     */
    public final int framePadding;

    /**
     * Offset in bytes from the beginning of a call instruction to the displacement.
     */
    public final int machineCodeCallDisplacementOffset;

    /**
     * Size in bytes of a move instruction.
     */
    public final int machineCodeMoveConstInstructionSize;

    private final String name;

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

    protected CiArchitecture(String name, int wordSize, String backend, BitOrdering bitOrdering, CiRegister[] registers, final int framePadding, final int nativeCallDisplacementOffset, final int nativeMoveConstInstructionSize) {
        this.name = name;
        this.registers = registers;
        this.wordSize = wordSize;
        this.platform = backend;
        this.bitOrdering = bitOrdering;
        this.framePadding = framePadding;
        this.machineCodeCallDisplacementOffset = nativeCallDisplacementOffset;
        this.machineCodeMoveConstInstructionSize = nativeMoveConstInstructionSize;
        switch (bitOrdering) {
            case LittleEndian:
                lowWordOffset = 0;
                highWordOffset = wordSize;
                break;
            case BigEndian:
                lowWordOffset = wordSize;
                highWordOffset = 0;
                break;
            default:
			throw new Error("Invalid bitordering!");
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
        return "x86".equals(platform);
    }

    /**
     * TODO: Get rid of this method. Platform specific code should be in subclasses.
     * Checks whether the backend is SPARC.
     * @return <code>true</code> if the backend of this architecture is SPARC
     */
    public boolean isSPARC() {
        return "SPARC".equals(platform);
    }
}
