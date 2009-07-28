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

import com.sun.c1x.bytecode.Bytecodes;
import com.sun.c1x.ci.CiMethod;
import com.sun.c1x.ci.CiType;
import com.sun.c1x.util.Util;
import com.sun.c1x.value.ValueStack;
import com.sun.c1x.value.ValueType;

/**
 * The <code>CheckCast</code> instruction represents a checkcast bytecode.
 *
 * @author Ben L. Titzer
 */
public class CheckCast extends TypeCheck {

    CiMethod profiledMethod;
    int profiledBCI;

    /**
     * Creates a new CheckCast instruction.
     * @param targetClass the class being casted to
     * @param object the instruction producing the object
     * @param stateBefore the state before the cast
     */
    public CheckCast(CiType targetClass, Instruction object, ValueStack stateBefore) {
        super(targetClass, object, ValueType.OBJECT_TYPE, stateBefore);
        initFlag(Flag.NonNull, object.isNonNull());
    }

    /**
     * Gets the profiled method for this instruction.
     * @return the profiled method
     */
    public CiMethod profiledMethod() {
        return profiledMethod;
    }

    /**
     * Gets the profiled bytecode index for this instruction.
     * @return the profiled bytecode index
     */
    public int profiledBCI() {
        return profiledBCI;
    }

    /**
     * Checks whether profiling should be added to this instruction.
     * @return <code>true</code> if profiling should be added to this instruction
     */
    public boolean shouldProfile() {
        return profiledMethod != null;
    }

    /**
     * Sets the profiled method and bytecode index for this instruction.
     * @param method the profiled method
     * @param bci the bytecode index
     */
    public void setProfile(CiMethod method, int bci) {
        profiledMethod = method;
        profiledBCI = bci;
    }

    /**
     * Gets the declared type of the result of this instruction.
     * @return the declared type of the result
     */
    @Override
    public CiType declaredType() {
        return targetClass;
    }

    /**
     * Gets the exact type of the result of this instruction.
     * @return the exact type of the result
     */
    @Override
    public CiType exactType() {
        return targetClass.exactType();
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(InstructionVisitor v) {
        v.visitCheckCast(this);
    }

    @Override
    public int valueNumber() {
        return targetClass.isLoaded() ? Util.hash1(Bytecodes.CHECKCAST, object) : 0;
    }

    @Override
    public boolean valueEqual(Instruction i) {
        if (i instanceof CheckCast) {
            CheckCast o = (CheckCast) i;
            return targetClass == o.targetClass && object == o.object;
        }
        return false;
    }

    public boolean isIncompatibleClassChangeCheck() {
        // TODO Auto-generated method stub
        return false;
    }

}
