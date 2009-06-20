/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.lang;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;

/**
 * TODO: clarify what exactly is being aligned.
 *
 * @author Bernd Mathiske
 */
public enum Alignment {

    BYTES_1(1),
    BYTES_2(2),
    BYTES_4(4),
    BYTES_8(8);

    public static final IndexedSequence<Alignment> VALUES = new ArraySequence<Alignment>(values());

    private final int nBytes;

    private Alignment(int nBytes) {
        this.nBytes = nBytes;
    }

    @Override
    public String toString() {
        return Integer.toString(nBytes);
    }

    @INLINE
    public final int numberOfBytes() {
        return nBytes;
    }

    public static Alignment fromInt(int nBytes) {
        for (Alignment value : Alignment.VALUES) {
            if (value.nBytes == nBytes) {
                return value;
            }
        }
        return null;
    }
}
