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
package com.sun.max.asm;

/**
 * An assembled object whose position and size may change.
 * Examples include span-dependent jump instructions to labels and padding bytes for memory alignment.
 *
 * @author David Liu
 * @author Doug Simon
 */
public abstract class MutableAssembledObject extends AssembledObject {

    protected abstract void assemble() throws AssemblyException;

    private final Assembler assembler;
    private int variablePosition;
    protected int variableSize;

    protected MutableAssembledObject(Assembler assembler, int startPosition, int endPosition) {
        super(startPosition, endPosition);
        this.assembler = assembler;
        this.variablePosition = startPosition;
        this.variableSize = endPosition - startPosition;
    }

    protected Assembler assembler() {
        return assembler;
    }

    public final int initialStartPosition() {
        return super.startPosition();
    }

    public final int initialEndPosition() {
        return super.endPosition();
    }

    public final int initialSize() {
        return super.size();
    }

    public void adjust(int delta) {
        variablePosition += delta;
    }

    @Override
    public int startPosition() {
        return variablePosition;
    }

    @Override
    public int endPosition() {
        return variablePosition + variableSize;
    }

    @Override
    public int size() {
        return variableSize;
    }
}
