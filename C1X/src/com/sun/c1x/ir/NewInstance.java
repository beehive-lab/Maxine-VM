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
import com.sun.c1x.ri.*;
import com.sun.c1x.value.*;

/**
 * The <code>NewInstance</code> instruction represents the allocation of an instance class object.
 *
 * @author Ben L. Titzer
 */
public class NewInstance extends StateSplit {

    final RiType instanceClass;
    public final char cpi;
    public final RiConstantPool constantPool;

    /**
     * Constructs a NewInstance instruction.
     * @param type the class being allocated
     * @param cpi the constant pool index
     * @param stateBefore the state before executing this instruction
     */
    public NewInstance(RiType type, char cpi, RiConstantPool constantPool, ValueStack stateBefore) {
        super(CiKind.Object, stateBefore);
        this.instanceClass = type;
        this.cpi = cpi;
        this.constantPool = constantPool;
        setFlag(Flag.NonNull);
    }

    /**
     * Gets the instance class being allocated by this instruction.
     * @return the instance class allocated
     */
    public RiType instanceClass() {
        return instanceClass;
    }

    /**
     * Checks whether this instruction can trap.
     * @return <code>true</code>, assuming that allocation can cause OutOfMemory or other exceptions
     */
    @Override
    public boolean canTrap() {
        return true;
    }

    /**
     * Gets the exact type produced by this instruction. For allocations of instance classes, this is
     * always the class allocated.
     * @return the exact type produced by this instruction
     */
    @Override
    public RiType exactType() {
        return instanceClass;
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(ValueVisitor v) {
        v.visitNewInstance(this);
    }
}
