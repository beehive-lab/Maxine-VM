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

import com.sun.c1x.util.*;
import com.sun.c1x.value.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * The {@code Invoke} instruction represents all kinds of method calls.
 *
 * @author Ben L. Titzer
 */
public final class Invoke extends StateSplit {

    final int opcode;
    final Value[] arguments;
    final RiMethod target;
    public final int cpi;
    public final RiConstantPool constantPool;

    /**
     * Constructs a new Invoke instruction.
     *
     * @param opcode the opcode of the invoke
     * @param result the result type
     * @param args the list of instructions producing arguments to the invocation, including the receiver object
     * @param isStatic {@code true} if this call is static (no receiver object)
     * @param target the target method being called
     * @param stateBefore the state before executing the invocation
     */
    public Invoke(int opcode, CiKind result, Value[] args, boolean isStatic, RiMethod target, int cpi, RiConstantPool constantPool, FrameState stateBefore) {
        super(result, stateBefore);
        this.opcode = opcode;
        this.arguments = args;
        this.target = target;
        if (isStatic) {
            setFlag(Flag.IsStatic);
            redundantNullCheck();
        } else if (args[0].isNonNull()) {
            redundantNullCheck();
        }

        this.cpi = cpi;
        this.constantPool = constantPool;
    }

    /**
     * Gets the opcode of this invoke instruction.
     * @return the opcode
     */
    public int opcode() {
        return opcode;
    }

    /**
     * Checks whether this is an invocation of a static method.
     * @return {@code true} if the invocation is a static invocation
     */
    public boolean isStatic() {
        return checkFlag(Flag.IsStatic);
    }

    /**
     * Gets the instruction that produces the receiver object for this invocation, if any.
     * @return the instruction that produces the receiver object for this invocation if any, {@code null} if this
     *         invocation does not take a receiver object
     */
    public Value receiver() {
        assert !isStatic();
        return arguments[0];
    }

    /**
     * Gets the target method for this invocation instruction.
     * @return the target method
     */
    public RiMethod target() {
        return target;
    }

    /**
     * Gets the list of instructions that produce input for this instruction.
     * @return the list of instructions that produce input
     */
    public Value[] arguments() {
        return arguments;
    }

    /**
     * Checks whether this instruction can trap.
     * @return {@code true}, conservatively assuming the called method may throw an exception
     */
    @Override
    public boolean canTrap() {
        return true;
    }

    @Override
    public boolean internalClearNullCheck() {
        return true;
    }

    /**
     * Checks whether this invocation has a receiver object.
     * @return {@code true} if this invocation has a receiver object; {@code false} otherwise, if this is a
     *         static call
     */
    public boolean hasReceiver() {
        return !isStatic();
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply to each instruction
     */
    @Override
    public void inputValuesDo(ValueClosure closure) {
        for (int i = 0; i < arguments.length; i++) {
            Value arg = arguments[i];
            if (arg != null) {
                arguments[i] = closure.apply(arg);
                assert arguments[i] != null;
            }
        }
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     *
     * @param v the visitor to accept
     */
    @Override
    public void accept(ValueVisitor v) {
        v.visitInvoke(this);
    }

    public CiKind[] signature() {
        CiKind receiver = isStatic() ? null : target.holder().kind();
        return Util.signatureToKinds(target.signature(), receiver);
    }
}
