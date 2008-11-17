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
package com.sun.max.vm.bytecode;

/**
 * Extends the notion of a {@linkplain BytecodePositionRange bytecode position range} to refer to a specific underlying
 * {@linkplain #code() code array}.
 * 
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class BytecodeBlock extends BytecodePositionRange {

    private final byte[] _code;

    public BytecodeBlock(byte[] bytecode, int startPosition, int endPosition) {
        super(startPosition, endPosition);
        _code = bytecode;
        assert check();
    }

    public BytecodeBlock(byte[] bytecode) {
        super(0, bytecode.length - 1);
        _code = bytecode;
        assert check();
    }

    private boolean check() {
        assert _code != null;
        assert _code.length > 0;
        assert start() >= 0;
        assert end() >= start();
        assert end() < _code.length;
        return true;
    }

    /**
     * Gets the code to which the range of positions in this object refer.
     */
    public byte[] code() {
        return _code;
    }

    /**
     * Gets the number of bytecode positions covered by this block.
     */
    public int size() {
        return end() - start() + 1;
    }
}
