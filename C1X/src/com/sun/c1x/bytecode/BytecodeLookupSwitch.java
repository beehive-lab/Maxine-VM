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
 * The <code>BytecodeLookupSwitch</code> class is a utility for processing lookupswitch bytecodes.
 *
 * @author Ben L. Titzer
 */
public class BytecodeLookupSwitch extends BytecodeSwitch {

    public BytecodeLookupSwitch(BytecodeStream stream, int bci) {
        super(stream, bci);
    }

    public BytecodeLookupSwitch(byte[] code, int bci) {
        super(code, bci);
    }

    @Override
    public int defaultOffset() {
        return readWord(aligned);
    }

    @Override
    public int offsetAt(int i) {
        return readWord(aligned + 12 + 8 * i);
    }

    public int keyAt(int i) {
        return readWord(aligned + 8 + 8 * i);
    }

    @Override
    public int numberOfCases() {
        return readWord(aligned + 4);
    }

    @Override
    public int size() {
        return aligned + 8 + 8 * numberOfCases() - bci;
    }
}
