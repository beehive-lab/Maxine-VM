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

import com.sun.c1x.ci.*;
import com.sun.c1x.value.*;


/**
 * An instruction that represents the runtime resolution of a Java class object. For example, an ldc of a class constant that is unresolved.
 *
 * @author Ben L. Titzer
 * @author Thomas Wuerthinger
 *
 */
public class ResolveClass extends Instruction {

    public final RiType ciType;
    private final ValueStack state;

    public ResolveClass(RiType type, ValueStack stack) {
        super(BasicType.Object);
        this.ciType = type;
        assert stack != null;
        this.state = stack;
        setFlag(Flag.NonNull);
    }

    @Override
    public void accept(InstructionVisitor v) {
        v.visitResolveClass(this);
    }

    public ValueStack state() {
        return state;
    }

    @Override
    public boolean canTrap() {
        return true;
    }

    /**
     * Iterates over the "other" values in this instruction. In the case of constants,
     * this method iterates over any values in the state if this constant may need patching.
     * @param closure the closure to apply to each value
     */
    @Override
    public void otherValuesDo(InstructionClosure closure) {
        state.valuesDo(closure);
    }

    @Override
    public int valueNumber() {
        return ciType.hashCode() | 0x50000000;
    }

    @Override
    public boolean valueEqual(Instruction i) {
        if (i instanceof ResolveClass) {
            final ResolveClass other = (ResolveClass) i;
            return other.ciType.equals(ciType);
        }
        return false;
    }
}
