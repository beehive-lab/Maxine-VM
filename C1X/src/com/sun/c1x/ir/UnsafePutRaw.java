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
package com.sun.c1x.ir;

import com.sun.cri.ci.*;

/**
 * The {@code UnsafePutRaw} instruction represents an unsafe store operation.
 *
 * @author Ben L. Titzer
 */
public final class UnsafePutRaw extends UnsafeRawOp {

    Value value;

    /**
     * Constructs a new UnsafeGetRaw instruction.
     * @param opKind the kind of the operation
     * @param addr the instruction generating the base address
     * @param value the instruction generating the value to store
     */
    public UnsafePutRaw(CiKind opKind, Value addr, Value value) {
        super(opKind, addr, false);
        this.value = value;
    }

    /**
     * Gets the instruction generating the value that will be stored.
     * @return the instruction generating the value
     */
    public Value value() {
        return value;
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply
     */
    @Override
    public void inputValuesDo(ValueClosure closure) {
        super.inputValuesDo(closure);
        value = closure.apply(value);
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(ValueVisitor v) {
        v.visitUnsafePutRaw(this);
    }

    public int log2scale() {
        return log2Scale;
    }
}
