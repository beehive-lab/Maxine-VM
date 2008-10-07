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
/*VCSID=b8b93ee7-af0b-4101-af95-32669eeb0ff3*/
package com.sun.max.vm.bytecode;

/**
 * Denotes a range of bytecode positions that is inclusive in both its {@linkplain #start() start} <b>and</b>
 * {@linkplain #end() end} positions.
 * 
 * @author Bernd Mathiske
 */
public class BytecodePositionRange {

    /**
     * The lowest position regarded as within this range.
     */
    private final int _start;

    /**
     * The highest position regarded as within this range.
     */
    private final int _end;

    /**
     * Creates an object denoting a range of bytecode positions.
     * 
     * @param start
     *                the lowest position regarded as within the range
     * @param end
     *                the highest position regarded as within the range
     */
    public BytecodePositionRange(int start, int end) {
        _start = start;
        _end = end;
    }

    /**
     * Gets the lowest position regarded as within this range.
     */
    public int start() {
        return _start;
    }

    /**
     * Gets the highest position regarded as within this range.
     */
    public int end() {
        return _end;
    }

    @Override  public String toString() {
        return _start + "-" + _end;
    }
}
