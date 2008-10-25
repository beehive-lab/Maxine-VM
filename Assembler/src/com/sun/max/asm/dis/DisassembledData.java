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
package com.sun.max.asm.dis;

import com.sun.max.collect.*;

/**
 *
 *
 * @author Doug Simon
 */
public abstract class DisassembledData implements DisassembledObject {

    private final int _startPosition;
    private final byte[] _bytes;

    public DisassembledData(int startPosition, byte[] bytes) {
        _startPosition = startPosition;
        _bytes = bytes;
    }

    public int startPosition() {
        return _startPosition;
    }

    public int endPosition() {
        return _startPosition + _bytes.length;
    }

    public byte[] bytes() {
        return _bytes.clone();
    }

    public Type type() {
        return Type.DATA;
    }

    public abstract String prefix();

    public abstract String operandsToString(Sequence<DisassembledLabel> labels, GlobalLabelMapper globalLabelMapper);

    public String operandsToString(Sequence<DisassembledLabel> labels) {
        return toString(labels, null);
    }

    @Override
    public String toString(Sequence<DisassembledLabel> labels, GlobalLabelMapper globalLabelMapper) {
        return prefix() + " " + operandsToString(labels, globalLabelMapper);
    }
}
