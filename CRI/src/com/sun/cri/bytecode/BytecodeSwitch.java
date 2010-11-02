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
 * An abstract class that provides the state and methods common to {@link Bytecodes#LOOKUPSWITCH}
 * and {@link Bytecodes#TABLESWITCH} instructions.
 *
 * @author Ben L. Titzer
 */
public abstract class BytecodeSwitch {
	/**
	 * The {@link BytecodeStream} containing bytecode array or {@code null} if {@link #code} is not {@code null}.
	 */
    private final BytecodeStream stream;
    /**
     * The bytecode array or {@code null} if {@link #stream} is not {@code null}.
     */
    private final byte[] code;
    /**
     * Index of start of switch instruction.
     */
    protected final int bci;
    /**
     * Index of the start of the additional data for the switch instruction, aligned to a multiple of four from the method start.
     */
    protected final int alignedBci;

    /**
     * Constructor for a {@link BytecodeStream}.
     * @param stream the {@code BytecodeStream} containing the switch instruction
     * @param bci the index in the stream of the switch instruction
     */
    public BytecodeSwitch(BytecodeStream stream, int bci) {
        this.alignedBci = (bci + 4) & 0xfffffffc;
        this.stream = stream;
        this.code = null;
        this.bci = bci;
    }

    /**
     * Constructor for a bytecode array.
     * @param code the bytecode array containing the switch instruction.
     * @param bci the index in the array of the switch instruction
     */
    public BytecodeSwitch(byte[] code, int bci) {
        this.alignedBci = (bci + 4) & 0xfffffffc;
        this.stream = null;
        this.code = code;
        this.bci = bci;
    }

    /**
     * Gets the current bytecode index.
     * @return the current bytecode index
     */
    public int bci() {
        return bci;
    }

    /**
     * Gets the index of the instruction denoted by the {@code i}'th switch target.
     * @param i index of the switch target
     * @return the index of the instruction denoted by the {@code i}'th switch target
     */
    public int targetAt(int i) {
        return bci + offsetAt(i);
    }

    /**
     * Gets the index of the instruction for the default switch target.
     * @return the index of the instruction for the default switch target
     */
    public int defaultTarget() {
        return bci + defaultOffset();
    }

    /**
     * Gets the offset from the start of the switch instruction to the default switch target.
     * @return the offset to the default switch target
     */
    public abstract int defaultOffset();

    /**
     * Gets the key at {@code i}'th switch target index.
     * @param i the switch target index
     * @return the key at {@code i}'th switch target index
     */
    public abstract int keyAt(int i);

    /**
     * Gets the offset from the start of the switch instruction for the {@code i}'th switch target.
     * @param i the switch target index
     * @return the offset to the {@code i}'th switch target
     */
    public abstract int offsetAt(int i);

    /**
     * Gets the number of switch targets.
     * @return the number of switch targets
     */
    public abstract int numberOfCases();

    /**
     * Gets the total size in bytes of the switch instruction.
     * @return the total size in bytes of the switch instruction
     */
    public abstract int size();

    /**
     * Reads the signed value at given bytecode index.
     * @param bci the start index of the value to retrieve
     * @return the signed, 4-byte value in the bytecode array starting at {@code bci}
     */
    protected int readWord(int bci) {
        if (code != null) {
            return Bytes.beS4(code, bci);
        }
        return stream.readInt(bci);
    }
}
