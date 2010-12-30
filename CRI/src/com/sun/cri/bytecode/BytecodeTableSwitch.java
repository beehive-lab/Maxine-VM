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
 * A utility for processing {@link Bytecodes#TABLESWITCH} bytecodes.
 *
 * @author Ben L. Titzer
 */
public class BytecodeTableSwitch extends BytecodeSwitch {
	private static final int OFFSET_TO_LOW_KEY = 4;
	private static final int OFFSET_TO_HIGH_KEY = 8;
	private static final int OFFSET_TO_FIRST_JUMP_OFFSET = 12;
	private static final int JUMP_OFFSET_SIZE = 4;

    /**
     * Constructor for a {@link BytecodeStream}.
     * @param stream the {@code BytecodeStream} containing the switch instruction
     * @param bci the index in the stream of the switch instruction
     */
    public BytecodeTableSwitch(BytecodeStream stream, int bci) {
        super(stream, bci);
    }

    /**
     * Constructor for a bytecode array.
     * @param code the bytecode array containing the switch instruction.
     * @param bci the index in the array of the switch instruction
     */
    public BytecodeTableSwitch(byte[] code, int bci) {
        super(code, bci);
    }

    /**
     * Gets the low key of the table switch.
     * @return the low key
     */
    public int lowKey() {
        return readWord(alignedBci + OFFSET_TO_LOW_KEY);
    }

    /**
     * Gets the high key of the table switch.
     * @return the high key
     */
    public int highKey() {
        return readWord(alignedBci + OFFSET_TO_HIGH_KEY);
    }

    @Override
    public int keyAt(int i) {
        return lowKey() + i;
    }

    @Override
    public int defaultOffset() {
        return readWord(alignedBci);
    }

    @Override
    public int offsetAt(int i) {
        return readWord(alignedBci + OFFSET_TO_FIRST_JUMP_OFFSET + JUMP_OFFSET_SIZE * i);
    }

    @Override
    public int numberOfCases() {
        return highKey() - lowKey() + 1;
    }

    @Override
    public int size() {
        return alignedBci + OFFSET_TO_FIRST_JUMP_OFFSET + JUMP_OFFSET_SIZE * numberOfCases() - bci;
    }
}
