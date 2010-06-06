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
 * A utility for processing {@link Bytecodes#LOOKUPSWITCH} bytecodes.
 *
 * @author Ben L. Titzer
 */
public class BytecodeLookupSwitch extends BytecodeSwitch {
	private static final int OFFSET_TO_NUMBER_PAIRS = 4;
	private static final int OFFSET_TO_FIRST_PAIR_MATCH = 8;
	private static final int OFFSET_TO_FIRST_PAIR_OFFSET = 12;
	private static final int PAIR_SIZE = 8;

    /**
     * Constructor for a {@link BytecodeStream}.
     * @param stream the {@code BytecodeStream} containing the switch instruction
     * @param bci the index in the stream of the switch instruction
     */
    public BytecodeLookupSwitch(BytecodeStream stream, int bci) {
        super(stream, bci);
    }

    /**
     * Constructor for a bytecode array.
     * @param code the bytecode array containing the switch instruction.
     * @param bci the index in the array of the switch instruction
     */
    public BytecodeLookupSwitch(byte[] code, int bci) {
        super(code, bci);
    }

    @Override
    public int defaultOffset() {
        return readWord(alignedBci);
    }

    @Override
    public int offsetAt(int i) {
        return readWord(alignedBci + OFFSET_TO_FIRST_PAIR_OFFSET + PAIR_SIZE * i);
    }

    @Override
    public int keyAt(int i) {
        return readWord(alignedBci + OFFSET_TO_FIRST_PAIR_MATCH + PAIR_SIZE * i);
    }

    @Override
    public int numberOfCases() {
        return readWord(alignedBci + OFFSET_TO_NUMBER_PAIRS);
    }

    @Override
    public int size() {
        return alignedBci + OFFSET_TO_FIRST_PAIR_MATCH + PAIR_SIZE * numberOfCases() - bci;
    }
}
