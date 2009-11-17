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

import com.sun.c1x.util.*;

/**
 * The <code>BytecodeSwitch</code> class definition.
 *
 * @author Ben L. Titzer
 */
public abstract class BytecodeSwitch {
    final BytecodeStream stream;
    final byte[] code;
    final int bci;
    final int aligned;

    public BytecodeSwitch(BytecodeStream stream, int bci) {
        this.aligned = (bci + 4) & 0xfffffffc;
        this.stream = stream;
        this.code = null;
        this.bci = bci;
    }

    public BytecodeSwitch(byte[] code, int bci) {
        this.aligned = (bci + 4) & 0xfffffffc;
        this.stream = null;
        this.code = code;
        this.bci = bci;
    }

    public int bci() {
        return bci;
    }

    public int targetAt(int i) {
        return bci + offsetAt(i);
    }

    public int defaultTarget() {
        return bci + defaultOffset();
    }

    public abstract int defaultOffset();

    public abstract int keyAt(int i);

    public abstract int offsetAt(int i);

    public abstract int numberOfCases();

    public abstract int size();

    protected int readWord(int bci) {
        if (code != null) {
            return Bytes.beS4(code, bci);
        }
        return stream.readInt(bci);
    }
}
