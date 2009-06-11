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

import com.sun.c1x.util.InstructionVisitor;
import com.sun.c1x.util.InstructionClosure;
import com.sun.c1x.ci.CiMethod;
import com.sun.c1x.ci.CiType;
import com.sun.c1x.value.ValueType;

/**
 * The <code>ProfileCall</code> instruction representings profiling instruction inserted in the
 * program to collect information about a particular call site.
 *
 * @author Ben L. Titzer
 */
public class ProfileCall extends Instruction {

    CiMethod _method;
    int _bciOfInvoke;
    Instruction _object;
    CiType _knownHolder;

    /**
     * Constructs a new ProfileCall instruction.
     * @param method the method being profiled
     * @param bci the bytecode index of the invocation bytecode
     * @param object the instruction generating the receiver object; <code>null</code> if there is no receiver
     * @param knownHolder the static type known at the call site
     */
    public ProfileCall(CiMethod method, int bci, Instruction object, CiType knownHolder) {
        super(ValueType.VOID_TYPE);
        _method = method;
        _bciOfInvoke = bci;
        _object = object;
        _knownHolder = knownHolder;
        pin();
    }

    /**
     * Gets the method being profiled.
     * @return the method
     */
    public CiMethod method() {
        return _method;
    }

    /**
     * Gets the bytecode index of the invocation bytecode.
     * @return the bytecode index
     */
    public int bciOfInvoke() {
        return _bciOfInvoke;
    }

    /**
     * Gets the instruction that generates the object that is input to this instruction.
     * @return the instruction generating the object
     */
    public Instruction object() {
        return _object;
    }

    /**
     * Gets the known information about the class of the object.
     * @return the known holder
     */
    public CiType knownHolder() {
        return _knownHolder;
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply
     */
    @Override
    public void inputValuesDo(InstructionClosure closure) {
        if (_object != null) {
            _object = closure.apply(_object);
        }
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(InstructionVisitor v) {
        v.visitProfileCall(this);
    }
}
