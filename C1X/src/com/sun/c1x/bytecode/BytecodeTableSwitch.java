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
 * The <code>BytecodeTableSwitch</code> class is a utility for processing tableswitch bytecodes.
 *
 * @author Ben L. Titzer
 */
public class BytecodeTableSwitch extends BytecodeSwitch {

    public BytecodeTableSwitch(BytecodeStream stream, int bci) {
        super(stream, bci);
    }

    public BytecodeTableSwitch(byte[] code, int bci) {
        super(code, bci);
    }

    public int lowKey() {
        return readWord(_aligned + 4);
    }

    public int highKey() {
        return readWord(_aligned + 8);
    }

    public int defaultOffset() {
        return readWord(_aligned);
    }

    public int offsetAt(int i) {
        return readWord(_aligned + 12 + 4 * i);
    }

    public int numberOfCases() {
        return highKey() - lowKey() + 1;
    }

    public int size() {
        return _aligned + 12 + 4 * numberOfCases() - _bci;
    }
}
