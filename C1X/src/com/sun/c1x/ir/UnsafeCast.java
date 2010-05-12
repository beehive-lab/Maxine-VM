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

import com.sun.cri.bytecode.*;
import com.sun.cri.ri.*;

/**
 * The {@code UnsafeCast} instruction represents a {@link Bytecodes#UNSAFE_CAST}.
 *
 * @author Doug Simon
 */
public final class UnsafeCast extends Instruction {

    public final RiType toType;

    /**
     * The instruction that produced the value being unsafe cast.
     */
    private Value value;

    /**
     * Creates a new UnsafeCast instruction.
     *
     * @param toKind the the being cast to
     * @param value the value being cast
     */
    public UnsafeCast(RiType toType, Value value) {
        super(toType.kind().stackKind());
        this.toType = toType;
        this.value = value;
    }

    /**
     * Gets the instruction that produced the value being unsafe cast.
     */
    public Value value() {
        return value;
    }

    @Override
    public RiType declaredType() {
        return toType;
    }

    @Override
    public RiType exactType() {
        return declaredType().exactType();
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(ValueVisitor v) {
        v.visitUnsafeCast(this);
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply
     */
    @Override
    public void inputValuesDo(ValueClosure closure) {
        value = closure.apply(value);
    }
}
