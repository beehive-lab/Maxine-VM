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

import com.sun.c1x.value.*;
import com.sun.cri.ri.*;

/**
 * Represents a call to a native function from within a native method stub.
 *
 * @author Doug Simon
 */
public final class NativeCall extends StateSplit {

    /**
     * The instruction that produces the native function address for this native call.
     */
    private Value address;

    /**
     * The native method for this native call.
     */
    public final RiMethod nativeMethod;

    /**
     * The signature of the call which is derived from {@link #nativeMethod} but is not
     * the same as its {@linkplain RiMethod#signatureType() signature}.
     */
    public final RiSignature signature;

    /**
     * The list of instructions that produce the arguments for this native call.
     */
    public final Value[] arguments;

    /**
     * Constructs a new NativeCall instruction.
     *
     * @param result the result type
     * @param args the list of instructions producing arguments to the invocation
     * @param stateBefore the state before executing the invocation
     */
    public NativeCall(RiMethod nativeMethod, RiSignature signature, Value address, Value[] args, FrameState stateBefore) {
        super(signature.returnKind().stackKind(), stateBefore);
        this.address = address;
        this.nativeMethod = nativeMethod;
        this.arguments = args;
        this.signature = signature;
        assert nativeMethod.jniSymbol() != null;
    }

    /**
     * Native functions can never trap. Or, more accurately, if they do, there
     * is no safe way to recover and the VM terminates.
     *
     * @return {@code false}
     */
    @Override
    public boolean canTrap() {
        return false;
    }

    /**
     * Gets the instruction that produces the native function address for this native call.
     * @return
     */
    public Value address() {
        return address;
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
        address = closure.apply(address);
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     *
     * @param v the visitor to accept
     */
    @Override
    public void accept(ValueVisitor v) {
        v.visitNativeCall(this);
    }
}
