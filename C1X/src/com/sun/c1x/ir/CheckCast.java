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
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * The {@code CheckCast} instruction represents a {@link Bytecodes#CHECKCAST}.
 *
 * @author Ben L. Titzer
 */
public final class CheckCast extends TypeCheck {

    /**
     * Creates a new CheckCast instruction.
     * @param targetClass the class being cast to
     * @param object the instruction producing the object
     * @param stateBefore the state before the cast
     */
    public CheckCast(RiType targetClass, Value targetClassInstruction, Value object, FrameState stateBefore) {
        super(targetClass, targetClassInstruction, object, CiKind.Object, stateBefore);
        initFlag(Flag.NonNull, object.isNonNull());
    }

    /**
     * Gets the declared type of the result of this instruction.
     * @return the declared type of the result
     */
    @Override
    public RiType declaredType() {
        return targetClass;
    }

    /**
     * Gets the exact type of the result of this instruction.
     * @return the exact type of the result
     */
    @Override
    public RiType exactType() {
        return targetClass.exactType();
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(ValueVisitor v) {
        v.visitCheckCast(this);
    }

    @Override
    public int valueNumber() {
        return targetClass.isResolved() ? Util.hash1(Bytecodes.CHECKCAST, object) : 0;
    }

    @Override
    public boolean valueEqual(Instruction i) {
        if (i instanceof CheckCast) {
            CheckCast o = (CheckCast) i;
            return targetClass == o.targetClass && object == o.object;
        }
        return false;
    }

}
